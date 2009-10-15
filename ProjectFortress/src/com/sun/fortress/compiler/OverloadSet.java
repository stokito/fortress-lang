/*******************************************************************************
 Copyright 2009 Sun Microsystems, Inc.,
 4150 Network Circle, Santa Clara, California 95054, U.S.A.
 All rights reserved.

 U.S. Government Rights - Commercial software.
 Government users are subject to the Sun Microsystems, Inc. standard
 license agreement and applicable provisions of the FAR and its supplements.

 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 Sun, Sun Microsystems, the Sun logo and Java are trademarks or registered
 trademarks of Sun Microsystems, Inc. in the U.S. and other countries.
 ******************************************************************************/

package com.sun.fortress.compiler;

import com.sun.fortress.compiler.NamingCzar;
import com.sun.fortress.compiler.index.Function;
import com.sun.fortress.compiler.index.Functional;
import com.sun.fortress.compiler.typechecker.TypeAnalyzer;
import com.sun.fortress.compiler.phases.CodeGenerationPhase;
import com.sun.fortress.exceptions.InterpreterBug;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.runtimeSystem.Naming;
import com.sun.fortress.useful.*;
import edu.rice.cs.plt.tuple.Option;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.*;

abstract public class OverloadSet implements Comparable<OverloadSet> {

    static class POType extends TopSortItemImpl<Type> {
        public POType(Type x) {
            super(x);
        }
    }

    public static class TaggedFunctionName implements Comparable<TaggedFunctionName> {
        final private APIName tagA;
        final private Function tagF;

        public TaggedFunctionName(APIName a, Function f) {
            this.tagF = f;
            this.tagA = a;
        }

        public List<Param> tagParameters() {
            return tagF.parameters();
        }

        public List<Param> callParameters() {
            return tagF.parameters();
        }

        public Type getReturnType() {
            return tagF.getReturnType().unwrap();
        }

        public int hashCode() {
            return tagF.hashCode() + MagicNumbers.a * tagA.hashCode();
        }

        public boolean equals(Object o) {
            if (o instanceof TaggedFunctionName) {
                TaggedFunctionName tfn = (TaggedFunctionName) o;
                return tagF.equals(tfn.tagF) && tagA.equals(tfn.tagA);
            }
            return false;
        }

        public List<BaseType> thrownTypes() {
            return tagF.thrownTypes();
        }

        public String toString() {
            return tagA.toString() + ".." + tagF.toString();
        }

        @Override
        public int compareTo(TaggedFunctionName o) {
            int i = tagF.toUndecoratedName().toString().compareTo(o.tagF.toUndecoratedName().toString());
            if (i != 0) return i;
            i = tagA.toString().compareTo(o.tagA.toString());
            List<Param> p = tagParameters();
            List<Param> q = o.tagParameters();
            if (p.size() < q.size()) return -1;
            if (p.size() > q.size()) return 1;
            for (int j = 0; j < p.size(); j++) {
                Param pp = p.get(i);
                Param qq = q.get(i);
                Type tp = pp.getIdType().unwrap();
                Type tq = qq.getIdType().unwrap();
                Class cp = tp.getClass();
                Class cq = tq.getClass();
                if (cp.equals(cq)) {
                    i = tp.toString().compareTo(tq.toString());
                } else {
                    i = cp.getName().compareTo(cq.getName());
                }
                if (i != 0)
                    return i;

            }
            return 0;
        }
    }

    /**
     * The set of functions that are less-specific-than-or-equal to the
     * parameters seen so far, so the parameter seen so far would be legal
     * for any of them.  The goal is to thin this set as more parameters
     * are found, and ultimately to choose the most specific one that
     * remains.
     */
    final Set<TaggedFunctionName> lessSpecificThanSoFar;
    final IdOrOpOrAnonymousName name;

    /**
     * This overloaded function may have a member whose signature matches
     * the overloaded function's signature.  If so, the principalMember is not
     * null.
     */
    TaggedFunctionName principalMember;

    /**
     * If there are subsets of the overload set that have principal
     * members, those can be referenced by name (the signature of the principal
     * member).  Overloaded functions need to be compiled for those subsets.
     * Note that if the entire set has a principal member, then it also appears
     * in this subset, because that indicates that the principal member
     * (a single function that may be dispatched to) needs a local-mangled name.
     * Thus, when iterating over the values in this set to generate code,
     * it is necessary to guard against generating code for the outermost
     * overloaded set.
     * <p/>
     * Uses a comparator that takes parameter lists into account.
     */
    private BATree<String, OverloadSet> overloadSubsets = new BATree<String, OverloadSet>(DefaultComparator.<String>normal());

    /**
     * Used to answer subtype questions.
     */
    final TypeAnalyzer ta;
    /**
     * All the indices that have been tested already.
     * Dispatch begins at the "most profitable" index, which
     * is defined to be the one with the greatest variation.
     * (Alternative plan might be to order them by the one
     * with the smallest subsets, thus guaranteeing log depth).
     */
    final BASet<Integer> testedIndices;

    /**
     * Assuming we have a parent (are not the root of a tree of OverloadSets),
     * what is that parent.
     */
    final OverloadSet parent;
    /**
     * Assuming we have a parent that dispatched on a parameter, how did we
     * get here?
     */
    final Type selectedParameterType;

    final int paramCount;

    final APIName ifNone;

    /**
     * Which parameter is used to split this set into subsets?
     */
    int dispatchParameterIndex;
    OverloadSet[] children;
    boolean splitDone;

    protected OverloadSet(APIName ifNone, IdOrOpOrAnonymousName name, TypeAnalyzer ta,
                          Set<TaggedFunctionName> lessSpecificThanSoFar,
                          BASet<Integer> testedIndices, OverloadSet parent, Type selectedParameterType, int paramCount) {
        this.ifNone = ifNone;
        this.name = name;
        this.ta = ta;
        this.lessSpecificThanSoFar = lessSpecificThanSoFar;
        this.testedIndices = testedIndices;
        this.parent = parent;
        this.selectedParameterType = selectedParameterType;
        this.paramCount = paramCount;
    }

    protected OverloadSet(IdOrOpOrAnonymousName name, TypeAnalyzer ta,
                          Set<TaggedFunctionName> lessSpecificThanSoFar,
                          int paramCount, APIName ifNone) {
        this(ifNone, name, ta, lessSpecificThanSoFar, new BASet<Integer>(DefaultComparator.<Integer>normal()),
                null, null, paramCount);
    }

    protected OverloadSet(final APIName apiname, IdOrOpOrAnonymousName name, TypeAnalyzer ta, Set<Function> defs, int n) {

        this(name, ta, Useful.applyToAll(defs, new F<Function, TaggedFunctionName>() {

            @Override
            public TaggedFunctionName apply(Function f) {
                return new TaggedFunctionName(apiname, f);
            }
        }), n, apiname);

        // Ensure that they are all the same size.
        for (TaggedFunctionName f : lessSpecificThanSoFar) {
            if (CodeGenerationPhase.debugOverloading)
                System.err.println("Overload: " + f);
            List<Param> parameters = f.tagParameters();
            int this_size = parameters.size();
            if (this_size != paramCount)
                InterpreterBug.bug("Need to handle variable arg dispatch elsewhere " + name);

        }
    }

    abstract protected void invokeParticularMethod(MethodVisitor mv, TaggedFunctionName f,
                                                   String sig);

    /**
     * Creates a dispatch-child overload set; one in which the dispatch choices have been reduced.
     *
     * @param childLSTSF
     * @param childTestedIndices
     * @param t
     * @return
     */
    abstract protected OverloadSet makeChild(Set<TaggedFunctionName> childLSTSF, BASet<Integer> childTestedIndices, Type t);

    /**
     * Creates a subset overload set; one that can be named independently as an overloaded function.
     *
     * @param childLSTSF
     * @param principalMember
     * @return
     */
    abstract protected OverloadSet makeSubset(Set<TaggedFunctionName> childLSTSF, TaggedFunctionName principalMember);

    public void split(boolean computeSubsets) {
        /* First determine if there are any overload subsets.
           This matters because it may affect naming of the leaf (single) functions.
        */
        if (computeSubsets) {
            for (TaggedFunctionName f : lessSpecificThanSoFar) {
                int i = 1; // for self
                for (TaggedFunctionName g : lessSpecificThanSoFar) {
                    if (!(f == g)) {
                        if (fSuperTypeOfG(f, g)) {
                            i++;
                        }
                    }
                }
                if (i > 1) {
                    if (i == lessSpecificThanSoFar.size()) {
                        principalMember = f;
                    } else {
                        // TODO work in progress
                        HashSet<TaggedFunctionName> subLSTSF = new HashSet<TaggedFunctionName>();
                        subLSTSF.add(f);
                        for (TaggedFunctionName g : lessSpecificThanSoFar) {
                            if (!(f == g)) {
                                if (fSuperTypeOfG(f, g)) {
                                    subLSTSF.add(g);
                                }
                            }
                        }
                        OverloadSet subset = makeSubset(subLSTSF, f);
                        subset.overloadSubsets = overloadSubsets;

                        overloadSubsets.put(jvmSignatureFor(f), subset);
                    }
                }
            }

            for (OverloadSet subset : overloadSubsets.values()) {
                subset.splitInternal();
            }
        }

        if (principalMember != null)
            overloadSubsets.put(jvmSignatureFor(principalMember), this);

        /* Split set into dispatch tree. */
        splitInternal();

    }


    public void splitInternal() {
        if (splitDone)
            return;

        if (lessSpecificThanSoFar.size() == 1) {
            splitDone = true;
            return;
            // If there are no other alternatives, then we are done.
        }

        // Accumulate sets of parameter types.
        int nargs = paramCount;

        MultiMap<Type, TaggedFunctionName>[] typeSets = new MultiMap[nargs];
        for (int i = 0; i < nargs; i++) {
            typeSets[i] = new MultiMap<Type, TaggedFunctionName>();
        }

        for (TaggedFunctionName f : lessSpecificThanSoFar) {
            List<Param> parameters = f.tagParameters();
            int i = 0;
            for (Param p : parameters) {
                if (testedIndices.contains(i)) {
                    i++;
                    continue;
                }
                Option<Type> ot = p.getIdType();
                Option<Type> ovt = p.getVarargsType();
                if (ovt.isSome()) {
                    InterpreterBug.bug("Not ready to handle compilation of overloaded varargs yet, function is " + f);
                }
                if (ot.isNone()) {
                    InterpreterBug.bug("Missing type for parameter " + i + " of " + f);
                }
                Type t = ot.unwrap();
                typeSets[i++].putItem(t, f);
            }
        }

        // Choose parameter index with greatest variation.
        // Choose parameter index with the smallest largest subset.
        int besti = -1;
        int best = 0;
        boolean greatest_variation = false;
        for (int i = 0; i < nargs; i++) {
            if (testedIndices.contains(i))
                continue;
            if (greatest_variation) {
                if (typeSets[i].size() > best) {
                    best = typeSets[i].size();
                    besti = i;
                }
            } else {
                MultiMap<Type, TaggedFunctionName> mm = typeSets[i];
                int largest = 0;
                for (Set<TaggedFunctionName> sf : mm.values()) {
                    if (sf.size() > largest)
                        largest = sf.size();
                }
                if (besti == -1 || largest < best) {
                    besti = i;
                    best = largest;
                }
            }
        }

        // dispatch on maxi'th parameter.
        dispatchParameterIndex = besti;
        Set<Type> dispatchTypes = typeSets[dispatchParameterIndex].keySet();


        children = new OverloadSet[best];
        BASet<Integer> childTestedIndices = testedIndices.putNew(besti);

        int i = 0;
        TopSortItemImpl<Type>[] potypes =
                new OverloadSet.POType[dispatchTypes.size()];
        /* Convert set of dispatch types into something that can be
           (topologically) sorted. */
        for (Type t : dispatchTypes) {
            potypes[i] = new POType(t);
            i++;
        }

        /*
         * Figure out ordering relationship for top sort.  O(N^2) work,
         * hope N is not too large.
         */
        for (i = 0; i < potypes.length; i++) {
            for (int j = i + 1; j < potypes.length; j++) {
                Type ti = potypes[i].x;
                Type tj = potypes[j].x;
                if (ta.subtypeNormal(ti, tj).isTrue()) {
                    potypes[i].edgeTo(potypes[j]);
                } else if (ta.subtypeNormal(tj, ti).isTrue()) {
                    potypes[j].edgeTo(potypes[i]);
                }
            }
        }

        List<TopSortItemImpl<Type>> specificFirst = TopSort.depthFirst(potypes);
        children = new OverloadSet[specificFirst.size()];

        // fill in children.
        for (i = 0; i < specificFirst.size(); i++) {
            Type t = specificFirst.get(i).x;
            Set<TaggedFunctionName> childLSTSF = new HashSet<TaggedFunctionName>();

            for (TaggedFunctionName f : lessSpecificThanSoFar) {
                List<Param> parameters = f.tagParameters();
                Param p = parameters.get(dispatchParameterIndex);
                Type pt = p.getIdType().unwrap();
                if (ta.subtypeNormal(t, pt).isTrue()) {
                    childLSTSF.add(f);
                }
            }

            childLSTSF = thin(childLSTSF, childTestedIndices);

            // ought to not be necessary
            if (paramCount == childTestedIndices.size()) {
                // Choose most specific member of lessSpecificThanSoFar
                childLSTSF = mostSpecificMemberOf(childLSTSF);

            }

            OverloadSet ch = makeChild(childLSTSF, childTestedIndices, t);
            ch.overloadSubsets = overloadSubsets;
            children[i] = ch;

        }
        for (OverloadSet child : children) {
            child.splitInternal();
        }

        splitDone = true;
    }

    private Set<TaggedFunctionName> thin(Set<TaggedFunctionName> childLSTSF, final Set<Integer> childTestedIndices) {
        /*
         * Hashes together functions that are equal in their unexamined parameter lists.
         */
        Hasher<TaggedFunctionName> hasher = new Hasher<TaggedFunctionName>() {

            @Override
            public boolean equiv(TaggedFunctionName x, TaggedFunctionName y) {
                List<Param> px = x.tagParameters();
                List<Param> py = y.tagParameters();
                for (int i = 0; i < px.size(); i++) {
                    if (childTestedIndices.contains(i))
                        continue;
                    Type tx = px.get(i).getIdType().unwrap();
                    Type ty = px.get(i).getIdType().unwrap();
                    if (!tx.equals(ty))
                        return false;
                }
                return true;
            }

            @Override
            public long hash(TaggedFunctionName x) {
                int h = MagicNumbers.T;

                List<Param> px = x.tagParameters();
                for (int i = 0; i < px.size(); i++) {
                    if (childTestedIndices.contains(i))
                        continue;
                    Type tx = px.get(i).getIdType().unwrap();
                    h = h * MagicNumbers.t + tx.hashCode();
                }
                return h;
            }

        };

        /*
         * Creates map from (some) functions to the
         * equivalence sets to which they are members.
         */
        GMultiMap<TaggedFunctionName, TaggedFunctionName> eqSetMap = new GMultiMap<TaggedFunctionName, TaggedFunctionName>(hasher);
        for (TaggedFunctionName f : childLSTSF)
            eqSetMap.putItem(f, f);

        Set<TaggedFunctionName> tmp = new HashSet<TaggedFunctionName>();

        /*
         * Take the most specific member of each equivalence set, and union
         * those together.
         */
        for (Set<TaggedFunctionName> sf : eqSetMap.values())
            tmp.addAll(mostSpecificMemberOf(sf));

        return tmp;
    }

    /**
     *
     */
    private Set<TaggedFunctionName> mostSpecificMemberOf(Set<TaggedFunctionName> set) {
        TaggedFunctionName msf = null;
        for (TaggedFunctionName candidate : set) {
            if (msf == null)
                msf = candidate;
            else {
                boolean cand_better = fSuperTypeOfG(msf, candidate);
                if (cand_better)
                    msf = candidate;

            }
        }
        if (msf == null)
            return Collections.<TaggedFunctionName>emptySet();
        else
            return Collections.singleton(msf);
    }

    /**
     * @param f
     * @param g
     * @return
     */
    private boolean fSuperTypeOfG(TaggedFunctionName f, TaggedFunctionName g) {
        List<Param> msf_parameters = f.tagParameters();
        List<Param> cand_parameters = g.tagParameters();
        if (msf_parameters.size() != cand_parameters.size()) {
            InterpreterBug.bug("Diff length parameter lists, should not be possible");
        }
        boolean cand_better = true;
        for (int i = 0; i < msf_parameters.size(); i++) {
            // Not handling varargs yet!
            Type msf_t = msf_parameters.get(i).getIdType().unwrap();
            Type cand_t = cand_parameters.get(i).getIdType().unwrap();
            // if any type of the candidate is not a subtype(or eq)
            // of the corresponding type of the msf, then the candidate
            // is NOT better.
            if (!ta.subtypeNormal(cand_t, msf_t).isTrue()) {
                cand_better = false;
                break;
            }
        }
        return cand_better;
    }

    @Override
    public int compareTo(OverloadSet o) {
        // TODO Auto-generated method stub
        return name.stringName().compareTo(o.name.stringName());
    }

    public IdOrOpOrAnonymousName getName() {
        return name;
    }

    public int getParamCount() {
        return paramCount;
    }

    /**
     * returns the code generation signature for the overloaded function.
     * (separation of concerns problem here -- taking joins of types,
     * and returning a target-platform (i.e., Java) signature).
     *
     * @return
     */
    public String getSignature() {
        String s = overloadedDomainSig();
        List<Type> typesToJoin = new ArrayList(lessSpecificThanSoFar.size());

        for (TaggedFunctionName f : lessSpecificThanSoFar) {
            typesToJoin.add(f.getReturnType());
        }

        s += NamingCzar.jvmTypeDesc(join(ta,typesToJoin), ifNone);

        return s;
    }

    /**
     * Type-level join that erases to Any if it obtains a Union type.
     *
     * TODO: should really erase to concrete least upper bound in type hierarchy,
     * but that requires extending TypeAnalyzer.
     */
    private static Type join(TypeAnalyzer ta, Iterable<Type> tys) {
        Type r = ta.joinNormal(tys);
        if (r instanceof UnionType) {
            r = NodeFactory.makeAnyType(r.getInfo().getSpan());
        }
        return r;
    }

    /**
     * Given an intersection of arrow types (what we get for at least some
     * overloaded functions) and an indication of how many parameters actually
     * appeared, create the signature for the overloaded function that will be
     * called.
     * <p/>
     * There's some fiddliness with varargs that has to be sorted out.
     *
     * @param t
     * @param paramCount
     * @return
     */
    public static String getSignature(IntersectionType t, int paramCount, TypeAnalyzer ta) {

        String s = overloadedDomainSig(t, paramCount, ta);

        Type r = getRangeSignature(t, paramCount, ta);

        s += NamingCzar.jvmTypeDesc(r, null);

        return s;
    }

    /**
     * Checks the parameter count against the intersection type to see if
     * this is a real overloading, or one that can trivially be disambiguated
     * by counting parameters.
     *
     * @param t
     * @param paramCount
     * @return
     */
    public static boolean actuallyOverloaded(IntersectionType t, int paramCount) {
        return matchingCount(t, paramCount) > 1;
    }

    public static int matchingCount(IntersectionType t, int paramCount) {
        int sum = 0;
        List<Type> types = t.getElements();

        Type r = null;

        for (Type type : types) {
            Type r0;

            if (type instanceof ArrowType) {
                ArrowType at = (ArrowType) type;
                r0 = at.getDomain();
                if (r0 instanceof TupleType) {
                    TupleType tt = (TupleType) r0;
                    if (paramCount != tt.getElements().size())
                        continue;
                } else if (paramCount != 1) {
                    continue;
                }

                sum++;

            } else if (type instanceof IntersectionType) {
                sum += matchingCount((IntersectionType) type, paramCount);
            } else {
                InterpreterBug.bug("Non arrowtype " + type + " in (function) intersection type");
            }
        }
        return sum;
    }


    private static Type getRangeSignature(IntersectionType t, int paramCount, TypeAnalyzer ta) {
        List<Type> types = t.getElements();
        List<Type> typesToJoin = new ArrayList(types.size());

        for (Type type : types) {
            Type r0;

            if (type instanceof ArrowType) {
                ArrowType at = (ArrowType) type;

                // Ensure that the domain count matches the paramCount
                r0 = at.getDomain();
                if (r0 instanceof TupleType) {
                    TupleType tt = (TupleType) r0;
                    if (paramCount != tt.getElements().size())
                        continue;
                } else if (paramCount != 1) {
                    continue;
                }

                r0 = at.getRange();
            } else if (type instanceof IntersectionType) {
                r0 = getRangeSignature((IntersectionType) type, paramCount, ta);
            } else {
                InterpreterBug.bug("Non arrowtype " + type + " in (function) intersection type");
                return null; // not reached
            }
            typesToJoin.add(r0);
        }
        return join(ta,typesToJoin);
    }

    private static Type getParamType(IntersectionType t, int i, int paramCount, TypeAnalyzer ta) {
        List<Type> types = t.getElements();
        List<Type> typesToJoin = new ArrayList(types.size());

        for (Type type : types) {
            Type r0;

            r0 = getParamType(type, i, paramCount, ta);
            if (r0==null) continue;

            typesToJoin.add(r0);
        }
        return join(ta,typesToJoin);
    }

    /**
     * @param type
     * @param i
     * @param paramCount
     * @param ta
     * @return
     */
    public static Type getParamType(Type type, int i, int paramCount,
                                    TypeAnalyzer ta) {
        Type r0;
        if (type instanceof ArrowType) {
            ArrowType at = (ArrowType) type;
            r0 = at.getDomain();

            // Ensure that the domain count matches the paramCount
            if (r0 instanceof TupleType) {
                TupleType tt = (TupleType) r0;
                if (paramCount != tt.getElements().size())
                    r0 = null;
                else
                    r0 = tt.getElements().get(i);
            } else if (paramCount != 1) {
                r0 = null;
            }
        } else if (type instanceof IntersectionType) {
            r0 = getParamType((IntersectionType) type, i, paramCount, ta);
        } else {
            r0 = InterpreterBug.bug("Non arrowtype " + type + " in (function) intersection type");
            // not reached
        }
        return r0;
    }

    public static boolean functionInstanceofType(Functional f, com.sun.fortress.nodes.Type ty, TypeAnalyzer ta) {
        List<Param> params = f.parameters();
        int l = params.size();
        int i = 0;
        // TODO this check is imperfect in a land of intersection types.
        // ought to iteratively ask the question, not reify the parameter type.
        for (Param p : params) {
            com.sun.fortress.nodes.Type ti = getParamType(ty, i, l, ta);
            com.sun.fortress.nodes.Type tp = p.getIdType().unwrap();
            if (ti == null)
                return false;
            if (!ta.subtypeNormal(tp, ti).isTrue())
                return false;
            i++;
        }
        return true;
    }

    /**
     * @param paramCount
     * @return
     */
    private static String overloadedDomainSig(IntersectionType t, int paramCount, TypeAnalyzer ta) {
        String s = "(";

        for (int i = 0; i < paramCount; i++) {
            s += NamingCzar.jvmTypeDesc(getParamType(t, i, paramCount, ta), null);
        }
        s += ")";
        return s;
    }

    private String overloadedDomainSig() {
        String s = "(";

        for (int i = 0; i < paramCount; i++) {
            s += NamingCzar.jvmTypeDesc(overloadedParamType(i), ifNone);
        }
        s += ")";
        return s;
    }

    private Type overloadedParamType(int param) {
        List<Type> typesToJoin = new ArrayList(lessSpecificThanSoFar.size());
        for (TaggedFunctionName f : lessSpecificThanSoFar) {
            List<Param> params = f.tagParameters();
            Param p = params.get(param);
            typesToJoin.add(p.getIdType().unwrap());
        }
        return join(ta,typesToJoin);
    }

    public String[] getExceptions() {
        HashSet<Type> exceptions = new HashSet<Type>();
        for (TaggedFunctionName f : lessSpecificThanSoFar) {
            List<BaseType> f_exceptions = f.thrownTypes();
            exceptions.addAll(f_exceptions);
        }
        String[] string_exceptions = new String[exceptions.size()];
        int i = 0;
        for (Type e : exceptions) {
            string_exceptions[i++] = NamingCzar.jvmTypeDesc(e, ifNone);
        }
        return string_exceptions;
    }

    public void generateCall(MethodVisitor mv, int firstArgIndex, Label failLabel) {
        if (!splitDone) {
            InterpreterBug.bug("Must split overload set before generating call(s)");
            return;
        }

        if (lessSpecificThanSoFar.size() == 1) {
            // Emit casts and call of f.
            TaggedFunctionName f = lessSpecificThanSoFar.iterator().next();

            String sig = jvmSignatureFor(f);

            int i = firstArgIndex;
            List<Param> params = f.callParameters();

            for (Param p : params) {
                mv.visitVarInsn(Opcodes.ALOAD, i);

                Type ty = p.getIdType().unwrap();
                mv.visitTypeInsn(Opcodes.CHECKCAST, NamingCzar.jvmTypeDesc(ty, ifNone, false));
                i++;
            }
            if (CodeGenerationPhase.debugOverloading)
                System.err.println("Emitting call " + f.tagF + sig);


            invokeParticularMethod(mv, f, sig);
            mv.visitInsn(Opcodes.ARETURN);

        } else {
            // Perform instanceof checks on specified parameter to dispatch to children.
            Label lookahead = new Label();
            for (int i = 0; i < children.length; i++) {
                OverloadSet os = children[i];
                mv.visitVarInsn(Opcodes.ALOAD, dispatchParameterIndex + firstArgIndex);
                mv.visitTypeInsn(Opcodes.INSTANCEOF, NamingCzar.jvmTypeDesc(os.selectedParameterType, ifNone, false));
                mv.visitJumpInsn(Opcodes.IFEQ, lookahead);
                os.generateCall(mv, firstArgIndex, failLabel);
                mv.visitLabel(lookahead);
                if (i + 1 < children.length)
                    lookahead = new Label();
                else lookahead = null;
            }
            mv.visitJumpInsn(Opcodes.GOTO, failLabel);
        }
    }

    /**
     * @param f
     * @return
     */
    static String jvmSignatureFor(TaggedFunctionName f) {
        return NamingCzar.jvmSignatureFor(f.tagF, f.tagA);
    }


    public String toString() {
        if (lessSpecificThanSoFar.size() == 1) {
            return lessSpecificThanSoFar.iterator().next().toString();
        }
        if (splitDone) {
            return toStringR("");
        } else {
            return lessSpecificThanSoFar.toString();
        }
    }

    private String toStringR(String indent) {
        if (lessSpecificThanSoFar.size() == 1) {
            return indent + lessSpecificThanSoFar.iterator().next().toString();
        } else {
            String s = indent + "#" + dispatchParameterIndex + "\n";
            for (int i = 0; i < children.length; i++) {
                OverloadSet os = children[i];
                s += indent + os.selectedParameterType + "->" + os.toStringR(indent + "   ") + "\n";
            }
            return s;
        }
    }

    /**
     * Overloaded functions get a slightly mangled name.
     *
     * @param name
     * @return
     */
    public static String oMangle(String name) {
        return name; // no mangling after all.
    }

    public void generateAnOverloadDefinition(String name, ClassVisitor cv) {
        generateAnOverloadDefinitionInner(name, cv);

        for (Map.Entry<String, OverloadSet> o_entry : getOverloadSubsets().entrySet()) {
            String ss = o_entry.getKey();
            OverloadSet sos = o_entry.getValue();
            if (sos != this) {
                sos.generateAnOverloadDefinitionInner(name, cv);
            }
        }
    }

    private void generateAnOverloadDefinitionInner(String name, ClassVisitor cv) {

        // "(" anOverloadedArg^N ")" returnType
        // Not sure what to do with return type.
        String signature = getSignature();
        String[] exceptions = getExceptions();
        if (CodeGenerationPhase.debugOverloading)
            System.err.println("Emitting overload " + name + signature);

        MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC
                + Opcodes.ACC_STATIC, // access,
                oMangle(name), // name,
                signature, // sp.getFortressifiedSignature(),
                null, // signature, // depends on generics, I think
                exceptions); // exceptions);

        mv.visitCode();
        Label fail = new Label();

        generateCall(mv, 0, fail); // Guts of overloaded method

        // Emit failure case
        mv.visitLabel(fail);
        // Boilerplate for throwing an error.
        mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/Error");
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn("Should not happen");
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Error", "<init>",
                "(Ljava/lang/String;)V");
        mv.visitInsn(Opcodes.ATHROW);

        mv.visitMaxs(getParamCount(), getParamCount()); // autocomputed
        mv.visitEnd();

    }


    public BATree<String, OverloadSet> getOverloadSubsets() {
        return overloadSubsets;
    }

    static public class Local extends AmongApis {
        public Local(final APIName apiname, IdOrOpOrAnonymousName name, TypeAnalyzer ta, Set<Function> defs, int n) {
            super(apiname, name, ta, Useful.applyToAll(defs, new F<Function, TaggedFunctionName>() {

                @Override
                public TaggedFunctionName apply(Function f) {
                    return new TaggedFunctionName(apiname, f);
                }
            }), n);
        }

    }

    static public class AmongApis extends OverloadSet {
        /**
         * Emit the invocation for a particular type of overloaded functions.
         *
         * @param mv
         * @param f
         * @param sig
         */
        protected void invokeParticularMethod(MethodVisitor mv, TaggedFunctionName f,
                                              String sig) {


            String ownerName = NamingCzar.apiAndMethodToMethodOwner(f.tagA, f.tagF);
            String mname = NamingCzar.apiAndMethodToMethod(f.tagA, f.tagF);

            if (getOverloadSubsets().containsKey(sig)) {
                mname = NamingCzar.mangleAwayFromOverload(mname);
            }

            mv.visitMethodInsn(Opcodes.INVOKESTATIC, ownerName, mname, sig);
        }

        /* Boilerplate follows, because this is a subtype. */

        protected AmongApis(APIName ifNone, IdOrOpOrAnonymousName name, TypeAnalyzer ta,
                            Set<TaggedFunctionName> lessSpecificThanSoFar,
                            BASet<Integer> testedIndices, OverloadSet parent, Type selectedParameterType, int paramCount) {
            super(ifNone, name, ta, lessSpecificThanSoFar, testedIndices, parent, selectedParameterType, paramCount);
        }

        public AmongApis(APIName ifNone, IdOrOpOrAnonymousName name, TypeAnalyzer ta, Set<TaggedFunctionName> defs, int n) {
            super(name, ta, defs, n, ifNone);
        }

        protected OverloadSet makeChild(Set<TaggedFunctionName> childLSTSF, BASet<Integer> childTestedIndices, Type t) {
            return new AmongApis(ifNone, name, ta, childLSTSF,
                    childTestedIndices, this, t, paramCount);
        }

        protected OverloadSet makeSubset(Set<TaggedFunctionName> childLSTSF, TaggedFunctionName principalMember) {
            OverloadSet subset = new AmongApis(ifNone, name, ta, childLSTSF, paramCount);
            subset.principalMember = principalMember;
            return subset;
        }

    }

}