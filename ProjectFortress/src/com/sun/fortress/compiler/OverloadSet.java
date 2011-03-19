/*******************************************************************************
    Copyright 2009,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler;

import java.util.*;

import org.objectweb.asm.*;
import org.objectweb.asm.util.*;

import com.sun.fortress.compiler.codegen.CodeGen;
import com.sun.fortress.compiler.codegen.CodeGenClassWriter;
import com.sun.fortress.compiler.index.Constructor;
import com.sun.fortress.compiler.index.DeclaredFunction;
import com.sun.fortress.compiler.index.Function;
import com.sun.fortress.compiler.index.Functional;
import com.sun.fortress.compiler.index.FunctionalMethod;
import com.sun.fortress.scala_src.overloading.OverloadingOracle;
import com.sun.fortress.scala_src.typechecker.Formula;
import com.sun.fortress.scala_src.types.TypeAnalyzer;
import com.sun.fortress.scala_src.types.TypeSchemaAnalyzer;
import com.sun.fortress.scala_src.useful.STypesUtil;
import com.sun.fortress.compiler.phases.CodeGenerationPhase;
import com.sun.fortress.exceptions.CompilerError;
import com.sun.fortress.exceptions.InterpreterBug;
import static com.sun.fortress.exceptions.InterpreterBug.bug;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.runtimeSystem.InstantiatingClassloader;
import com.sun.fortress.runtimeSystem.Naming;
import com.sun.fortress.useful.*;

import edu.rice.cs.plt.tuple.Option;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import scala.collection.JavaConversions;

abstract public class OverloadSet implements Comparable<OverloadSet> {
    private static final boolean OVERLOADS_WITH_GENERICS = true;

    static class POType extends TopSortItemImpl<Type> {
        public POType(Type x) {
            super(x);
        }
    }

    static class POTFN extends TopSortItemImpl<TaggedFunctionName> {
        public POTFN(TaggedFunctionName x) {
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

        public Function getF() {
            return tagF;
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

        public List<Type> thrownTypes() {
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
                if (! (pp.getIdType().unwrap() instanceof Type &&
                       qq.getIdType().unwrap() instanceof Type) )
                    InterpreterBug.bug("Types are expected.");
                Type tp = (Type)pp.getIdType().unwrap();
                Type tq = (Type)qq.getIdType().unwrap();
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
     * The functions, ordered from most to least specific order.
     * This is used to obtain a dispatch order in the presence
     * of generics (it also works when there are no generics).
     */
    TaggedFunctionName[] specificDispatchOrder;
    
    /**
     * This overloaded function may have a member whose signature matches
     * the overloaded function's signature.  If so, the principalMember is not
     * null.
     */
    TaggedFunctionName principalMember;
    public String genericSchema = ""; /* Need to stash this for generic-tagged overloads. */

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
    final OverloadingOracle oa;

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

    protected OverloadSet(APIName ifNone, IdOrOpOrAnonymousName name, TypeAnalyzer ta, OverloadingOracle oa,
                          Set<OverloadSet.TaggedFunctionName> lessSpecificThanSoFar,
                          BASet<Integer> testedIndices, OverloadSet parent, Type selectedParameterType, int paramCount) {
        this.ifNone = ifNone;
        this.name = name;
        this.ta = ta;
        this.oa = oa;
        this.lessSpecificThanSoFar = lessSpecificThanSoFar;
        this.testedIndices = testedIndices;
        this.parent = parent;
        this.selectedParameterType = selectedParameterType;
        this.paramCount = paramCount;
    }

    protected OverloadSet(IdOrOpOrAnonymousName name, TypeAnalyzer ta, OverloadingOracle oa,
                          Set<OverloadSet.TaggedFunctionName> lessSpecificThanSoFar,
                          int paramCount, APIName ifNone) {
        this(ifNone, name, ta, oa, lessSpecificThanSoFar, new BASet<Integer>(DefaultComparator.<Integer>normal()),
                null, null, paramCount);
    }

    protected OverloadSet(final APIName apiname, IdOrOpOrAnonymousName name, TypeAnalyzer ta, OverloadingOracle oa, Set<Function> defs, int n) {

        this(name, ta, oa, Useful.applyToAll(defs, new F<Function, TaggedFunctionName>() {

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
    abstract protected OverloadSet makeSubset(Set<TaggedFunctionName> childLSTSF, TaggedFunctionName _principalMember);

    public void split(boolean computeSubsets) {
        /* First determine if there are any overload subsets.
           This matters because it may affect naming
           of the leaf (single) functions.
         */

        TopSortItemImpl<TaggedFunctionName>[] pofuns =
            new OverloadSet.POTFN[lessSpecificThanSoFar.size()];
        /* Convert set of dispatch types into something that can be
       (topologically) sorted. */
        {
            int i = 0;   

            for (TaggedFunctionName f : lessSpecificThanSoFar ) {
                pofuns[i] = new POTFN(f);
                i++;
            }
        }

        for (int fi = 0; fi < pofuns.length; fi++) {
            TaggedFunctionName f = pofuns[fi].x;
            int i = 1; // for self
            /* Count the number of functions in LSTSF that are more
             * specific than or equal to f.
             * Also record any more-specific-than relationship encountered.
             */
            for (int gi = 0; gi < pofuns.length; gi++) {
                TaggedFunctionName g = pofuns[gi].x;
                if (!(f == g)) {
                    if (fSuperTypeOfG(f, g)) {
                        i++;
                        pofuns[gi].edgeTo(pofuns[fi]);
                    }
                }
            }
            if (computeSubsets) {

                if (i > 1) {
                    /* There's at least one function more specific than f. */
                    if (i == lessSpecificThanSoFar.size()) {
                        /*
                         * If f is more specific than or equal to every member
                         * of LSTSF, then it is the principal member.
                         */
                        principalMember = f;
                    } else {
                        /* TODO work in progress
                         * There are SOME members of the subset that are more
                         * specific than f; identify those, and create that
                         * subset.  f will be the principal member of the
                         * overloaded function that results.  External (through
                         * API) references to f, must invoke the overloaded
                         * function.
                         */
                        HashSet<TaggedFunctionName> subLSTSF =
                            new HashSet<TaggedFunctionName>();
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

                        overloadSubsets.put(
                                name.stringName()+jvmSignatureFor(f), subset);
                    }
                }
            }
        }
        
        List<TopSortItemImpl<TaggedFunctionName>> specificFirst = TopSort.depthFirstArray(pofuns);
        
        if (computeSubsets) {
            /*
             * After identifying all the subsets (which have principal
             * members), generate their structure.
             */
            for (OverloadSet subset : overloadSubsets.values()) {
                subset.splitInternal(specificFirst);
            }
        }

        if (principalMember != null)
            overloadSubsets.put(
                    name.stringName()+jvmSignatureFor(principalMember), this);

        /* Split set into dispatch tree. */
        splitInternal(specificFirst);

    }


    public void splitInternal(List<TopSortItemImpl<TaggedFunctionName>> funsInSpecificOrder) {
        if (splitDone)
            return;

        int l = lessSpecificThanSoFar.size();
        
        specificDispatchOrder = new TaggedFunctionName[l];
        {
            int i = 0;

            for (TopSortItemImpl<TaggedFunctionName> tsii_f : funsInSpecificOrder) {
                TaggedFunctionName f = tsii_f.x;
                if (lessSpecificThanSoFar.contains(f))
                    specificDispatchOrder[i++] = f;
            }
        }
        
        if (l == 1) {
            splitDone = true;
            return;
            // If there are no other alternatives, then we are done.
        }

        
        if (OVERLOADS_WITH_GENERICS) {
        } else {


            // Accumulate sets of parameter types.
            int nargs = paramCount;

            MultiMap<Type, TaggedFunctionName>[] typeSets = new MultiMap[nargs];
            for (int i = 0; i < nargs; i++) {
                typeSets[i] = new MultiMap<Type, TaggedFunctionName>();
            }

            for (TaggedFunctionName f : lessSpecificThanSoFar) {
                List<Param> parameters = f.tagParameters();

                for (int i = 0; i < parameters.size(); i++) {
                    if (testedIndices.contains(i)) {
                        continue;
                    }
                    Function eff = f.getF();
                    Type t = oa.getParamType(eff,i);
                    typeSets[i].putItem(t, f);

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
                    if (tweakedSubtypeTest(ta, ti, tj)) {
                        potypes[i].edgeTo(potypes[j]);
                    } else if (tweakedSubtypeTest(ta, tj, ti)) {
                        potypes[j].edgeTo(potypes[i]);
                    }
                }
            }

            List<TopSortItemImpl<Type>> specificFirst = TopSort.depthFirstArray(potypes);
            children = new OverloadSet[specificFirst.size()];

            // fill in children.
            for (i = 0; i < specificFirst.size(); i++) {
                Type t = specificFirst.get(i).x;
                Set<TaggedFunctionName> childLSTSF =
                    new HashSet<TaggedFunctionName>();

                for (TaggedFunctionName f : lessSpecificThanSoFar) {
                    Type pt = oa.getParamType(f.getF(),dispatchParameterIndex);
                    if (tweakedSubtypeTest(ta, t, pt)) {
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
                child.splitInternal(funsInSpecificOrder);
            }

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
                    TypeOrPattern tx = px.get(i).getIdType().unwrap();
                    TypeOrPattern ty = py.get(i).getIdType().unwrap();
                    if (! (tx instanceof Type && ty instanceof Type))
                        bug("Types are expected.");
                    if (!((Type)tx).equals((Type)ty))
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
                    h = h * MagicNumbers.t + px.get(i).getIdType().unwrap().hashCode();
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
     * Returns the most specific member (least generally applicable) 
     * member of a set of tagged function names, if any exists.
     * (So, return a singleton or empty.)
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
     * Returns true if the parameter list of g is more specific than or equal
     * to the parameter list of f.  If any of g's parameters fails to be more
     * specific than the corresponding parameter of f (using the
     * "tweakedSubtypeTest"), then returns false.
     * 
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
                
        return oa.lteq(g.tagF, f.tagF);
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
        return overloadedDomainSig() + NamingCzar.jvmTypeDesc(getRange(), ifNone);
    }

    public Type getRange() {
        List<Type> typesToJoin = new ArrayList(lessSpecificThanSoFar.size());
        for (TaggedFunctionName f : lessSpecificThanSoFar) {
            List<StaticParam> sparams = f.tagF.staticParameters();
            typesToJoin.add(STypesUtil.insertStaticParams(normalizeSelfType(f.getReturnType()), sparams));
        }
        return join(ta,typesToJoin);
    }

    /**
     * Type-level join that erases to Any if it obtains a Union type.
     *
     * TODO: should really erase to concrete least upper bound in type hierarchy,
     * but that requires extending TypeAnalyzer.
     */
    private static Type join(TypeAnalyzer ta, List<Type> tys) {
        TypeSchemaAnalyzer tsa = new TypeSchemaAnalyzer(ta);

        int l = tys.size();
        Type r = tys.get(0);
        if (l > 1) 
         for (Type rr : tys.subList(1, l)) {
             r = tsa.joinED(r, rr);
         }
            
        //    ta.join(JavaConversions.asIterable(tys));
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

        Type r = getRange(t, paramCount, ta);

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


    private static Type getRange(IntersectionType t, int paramCount, TypeAnalyzer ta) {
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
                r0 = getRange((IntersectionType) type, paramCount, ta);
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

            typesToJoin.add(normalizeSelfType(r0));
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
            Type ti = getParamType(ty, i, l, ta);
            TypeOrPattern tp = p.getIdType().unwrap();
            if (! (tp instanceof Type))
                bug("Type is expected: " + tp);
            if (ti == null)
                return false;
            /*
             * TraitSelfTypes yielded the wrong result here.
             */
            ti = normalizeSelfType(ti);
            tp = normalizeSelfType((Type)tp);
            if (!tweakedSubtypeTest(ta, (Type)tp, ti))
                return false;
            i++;
        }
        return true;
    }

    /**
     * @param ti
     * @return
     */
    private static com.sun.fortress.nodes.Type normalizeSelfType(
            com.sun.fortress.nodes.Type ti) {
        if (ti instanceof TraitSelfType)
            ti = ((TraitSelfType)ti).getNamed();
        return ti;
    }

    private static boolean tweakedSubtypeTest(TypeAnalyzer ta, com.sun.fortress.nodes.Type sub_type, Type super_type ) {
        sub_type = normalizeSelfType(sub_type);
        super_type = normalizeSelfType(super_type);
        TypeSchemaAnalyzer tsa = new TypeSchemaAnalyzer(ta);
        return tsa.subtypeED(sub_type, super_type);
        // return Formula.isTrue(ta.subtype(sub_type, super_type), ta);
    }

    /**
     * @param paramCount
     * @return
     */
    private static String overloadedDomainSig(IntersectionType t, int paramCount, TypeAnalyzer ta) {
        String s = "(";
        StringBuilder buf = new StringBuilder();
        buf.append(s);
        for (int i = 0; i < paramCount; i++) {
            buf.append(NamingCzar.jvmTypeDesc(getParamType(t, i, paramCount, ta), null));
        }
        s = buf.toString();
        s += ")";
        return s;
    }

    private String overloadedDomainSig() {
        String s = "(";
        StringBuilder buf = new StringBuilder();
        buf.append(s);
        for (Type t : overloadedDomain()) {
            buf.append(NamingCzar.jvmTypeDesc(t, ifNone));
        }
        s = buf.toString();
        s += ")";
        return s;
    }

    protected List<Type> overloadedDomain() {
        List<Type> res = new ArrayList(paramCount);
        for (int i = 0; i < paramCount; i++) {
            res.add(overloadedParamType(i));
        }
        return res;
    }

    private Type overloadedParamType(int param) {
        List<Type> typesToJoin = new ArrayList(lessSpecificThanSoFar.size());
        for (TaggedFunctionName f : lessSpecificThanSoFar) {
            List<Param> params = f.tagParameters();
            List<StaticParam> sparams = f.tagF.staticParameters();
            Param p = params.get(param);
            if (! (p.getIdType().unwrap() instanceof Type))
                bug("Type is expected: " + p.getIdType().unwrap());
            typesToJoin.add(STypesUtil.insertStaticParams(normalizeSelfType((Type)p.getIdType().unwrap()),sparams));
        }
        return join(ta,typesToJoin);
    }

    public String[] getExceptions() {
        HashSet<Type> exceptions = new HashSet<Type>();
        for (TaggedFunctionName f : lessSpecificThanSoFar) {
            List<Type> f_exceptions = f.thrownTypes();
            exceptions.addAll(f_exceptions);
        }
        String[] string_exceptions = new String[exceptions.size()];
        int i = 0;
        for (Type e : exceptions) {
            string_exceptions[i++] = NamingCzar.jvmTypeDesc(e, ifNone);
        }
        return string_exceptions;
    }

    
    
    /**
     * 
     * @author dr2chase
     */
    static class TS {
        String fullname;
        String stem;
        TS[] parameters;
        int localIndex; /* index of local to store corresponding value */
        int successorIndex;
        int variance; /* 1 = co, 0 = in, -1 = contra */
        boolean hasGeneric;
        
        public TS(String fullname, String stem, TS[] parameters,
                int localIndex, int successorIndex, int variance,
                boolean hasGeneric) {
            super();
            this.fullname = fullname;
            this.stem = stem;
            this.parameters = parameters;
            this.localIndex = localIndex;
            this.successorIndex = successorIndex;
            this.variance = variance;
            this.hasGeneric = hasGeneric;
        }
    }
    
 
    TS makeTypeStructure(Type t, Map<String, String> spmap, int variance, int storeAtIndex) {
        String fullname = NamingCzar.jvmBoxedTypeName(t, ifNone);
        String stem = null;
        TS[] parameters = null;
        int[] variances = null;
        List<Type> type_elements = null;
        boolean hasGeneric = false;
        
        if (t instanceof TupleType) {
            stem = Naming.TUPLE_TAG;
            type_elements = ((TupleType) t).getElements();
            variances = arrayOfInt(type_elements.size(), variance);
            
        } else if (t instanceof ArrowType) {
            stem = Naming.ARROW_TAG;
            ArrowType at = ((ArrowType) t);
            Type range = at.getRange();
            Type domain = at.getDomain();
            if (domain instanceof TupleType) {
                type_elements = Useful.<Type,Type>list(((TupleType) domain).getElements(), range);
            } else {
                type_elements = Useful.<Type>list(domain, range);
            }
            int l = type_elements.size();
            variances = arrayOfInt(l, -variance);
            variances[l-1] = variance;
            
        } else if (t instanceof TraitSelfType) {
            return makeTypeStructure(((TraitSelfType)t).getNamed(), spmap, variance, storeAtIndex);
            
        } else if (t instanceof TraitType) {
            TraitType tt = (TraitType) t;
            List<StaticArg> tt_sa = tt.getArgs();
            Id tt_id = tt.getName();
            stem = NamingCzar.jvmClassForToplevelTypeDecl(tt_id,"",ifNone);
            if (tt_sa.size() > 0) {
                // process args into types. Non-type args will be somewhat problematic at first.
                type_elements = new ArrayList<Type>();
                // need to figure out how to normalize array length if non-type args.
                variances = arrayOfInt(tt_sa.size(), 0);
                int variance_index = 0;
                for (StaticArg sta : tt_sa) {
                    if (sta instanceof TypeArg) {
                        TypeArg sta_ta = (TypeArg) sta;
                        type_elements.add( sta_ta.getTypeArg() );
                        // if interesting variance, change it here.
                    } else {
                        // unhandled case.
                        throw new CompilerError("Only handling some static args of generic types");
                    }
                    variance_index++;
                }
            }

        } else if (t instanceof VarType) {
            VarType tt = (VarType) t;
            Id tt_id = tt.getName();
            hasGeneric = true;

        } else if (t instanceof BottomType) {
            throw new CompilerError("Not handling Bottom type yet in generic overload dispatch");

        } else if (t instanceof AnyType) {
            throw new CompilerError("Not handling Any type yet in generic overload dispatch");

        } else if (t instanceof AbbreviatedType) {
            throw new CompilerError("Not handling Abbreviated type yet in generic overload dispatch");

        } else {
            throw new CompilerError("Not handling " + t.getClass() + " type yet in generic overload dispatch");
        }
        
        if (type_elements != null) {
            parameters = new TS[type_elements.size()];
            int i = 0;
            int next_index = storeAtIndex+1;
            for (Type tt : type_elements) {
                TS ts = makeTypeStructure(tt, spmap, variances[i], next_index);
                if (ts.hasGeneric)
                    hasGeneric = true;
                parameters[i++] = ts;
                next_index = ts.successorIndex;
            }
            // if no generics, then no sub-evaluation
            if (hasGeneric)
                next_index = storeAtIndex+1;
            return new TS(fullname, stem, parameters, storeAtIndex, next_index, variance, hasGeneric);
        } else {
            return new TS(fullname, stem, parameters, storeAtIndex, storeAtIndex+1, variance, hasGeneric);
        }        
    }

    /**
     * @param size
     * @param initial
     */
    private int[] arrayOfInt(int size, int initial) {
        int[] variances = new int[size];
        Arrays.fill(variances, initial);
        return variances;
    }
    
    public void generateCall(MethodVisitor mv, int firstArgIndex, Label failLabel) {
        if (!splitDone) {
            InterpreterBug.bug("Must split overload set before generating call(s)");
            return;
        }

        if (OVERLOADS_WITH_GENERICS) {
            int l = specificDispatchOrder.length;
            for (int i = 0; i < l; i++) {
                TaggedFunctionName f = specificDispatchOrder[i];
                Function eff = f.getF();
                
                Label lookahead = null;

                if (i < l-1) {
                    /* Trust the static checker; no need to verify
                     * applicability of the last one.
                     */
                    // Will need lookahead for the next one.
                    lookahead = new Label();

                    List<Param> parameters = f.tagParameters();
                    for (int j = 0; j < parameters.size(); j++) {
                        Type t = oa.getParamType(eff,j);
                        
                        // Load actual parameter
                        mv.visitVarInsn(Opcodes.ALOAD, j + firstArgIndex);
                        // Check type
                        InstantiatingClassloader.generalizedInstanceOf(mv,
                                NamingCzar.jvmBoxedTypeName(t, ifNone));
                        // Branch ahead if failure
                        mv.visitJumpInsn(Opcodes.IFEQ, lookahead);
                    }
                }
                // Come here if we have successfully passed the test.
                generateLeafCall(mv, firstArgIndex, f);

                if (lookahead != null)
                    mv.visitLabel(lookahead);
            }
            
            
        } else {

            if (lessSpecificThanSoFar.size() == 1) {
                // Emit casts and call of f.
                TaggedFunctionName f = lessSpecificThanSoFar.iterator().next();
                generateLeafCall(mv, firstArgIndex, f);

            } else {
                // Perform instanceof checks on specified parameter to dispatch to children.
                for (int i = 0; i < children.length; i++) {
                    OverloadSet os = children[i];
                    Label lookahead = new Label();
                    mv.visitVarInsn(Opcodes.ALOAD, dispatchParameterIndex + firstArgIndex);
                    InstantiatingClassloader.generalizedInstanceOf(mv,
                            NamingCzar.jvmBoxedTypeName(os.selectedParameterType, ifNone));
                    mv.visitJumpInsn(Opcodes.IFEQ, lookahead);
                    os.generateCall(mv, firstArgIndex, failLabel);
                    mv.visitLabel(lookahead);
                }
                mv.visitJumpInsn(Opcodes.GOTO, failLabel);
            }
        }
    }

    /**
     * Invoke f (it's a forwarding call) with casts inserted as necessary.
     * 
     * @param mv
     * @param firstArgIndex
     * @param f
     */
    private void generateLeafCall(MethodVisitor mv, int firstArgIndex,
            TaggedFunctionName f) {
        String sig = jvmSignatureFor(f);

        int i = firstArgIndex;
        List<Param> params = f.callParameters();

        for (Param p : params) {
            mv.visitVarInsn(Opcodes.ALOAD, i);

            TypeOrPattern ty = p.getIdType().unwrap();
            if (! (ty instanceof Type))
                bug("Type is expected: " + ty);
            InstantiatingClassloader.generalizedCastTo(mv,  NamingCzar.jvmBoxedTypeName((Type)ty, ifNone));
            // mv.visitTypeInsn(Opcodes.CHECKCAST, NamingCzar.jvmBoxedTypeDesc((Type)ty, ifNone));
            i++;
        }
        if (CodeGenerationPhase.debugOverloading)
            System.err.println("Emitting call " + f.tagF + sig);

        invokeParticularMethod(mv, f, sig);
        mv.visitInsn(Opcodes.ARETURN);
    }

    /**
     * @param f
     * @return
     */
    String jvmSignatureFor(TaggedFunctionName f) {
        /*
        Function fu = f.tagF;
        List<StaticParam> params = staticParametersOf(fu);
        TypeAnalyzer eta = ta;
        if (params != null) {
            eta = ta.extend(params, Option.<WhereClause>none());
        }
        */

        return NamingCzar.jvmSignatureFor(f.tagF, f.tagA); // eta
    }

    /**
     * @param fu
     * @return
     */
    private static List<StaticParam> staticParametersOf(Function fu) {
        List<StaticParam> params = null;
        if (fu instanceof FunctionalMethod) {
            List<StaticParam> lsp = ((FunctionalMethod) fu).traitStaticParameters();
            if (lsp.size() > 0)
                params = lsp;
        } else if (fu instanceof DeclaredFunction) {
            DeclaredFunction df = (DeclaredFunction) fu;
            List<StaticParam> lsp = df.staticParameters();
            if (lsp.size() > 0)
                params = lsp;
        } else if (fu instanceof Constructor) {
            InterpreterBug.bug("Unimplemented arm of jvm signature " + fu);
        } else {
            InterpreterBug.bug("Unexpected subtype of FunctionalMethod " + fu);
        }
        return params;
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
        if (OVERLOADS_WITH_GENERICS) {
            String s = indent;
            int l = specificDispatchOrder.length;
            for (int i = 0; i < l; i++) {
                TaggedFunctionName f = specificDispatchOrder[i];
                s += f.toString() + "\n";
            }
            return s;

        } else {
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

    public void generateAnOverloadDefinition(String _name, CodeGenClassWriter cv) {
        // System.err.println("Generating "+ name + "\n" +
        //                    "    principalMember "+ principalMember + "\n" + this.toStringR("   "));
        generateAnOverloadDefinitionInner(_name, cv);

        for (Map.Entry<String, OverloadSet> o_entry : getOverloadSubsets().entrySet()) {
            String ss = o_entry.getKey();
            OverloadSet sos = o_entry.getValue();
            if (sos != this) {
                sos.generateAnOverloadDefinitionInner(_name, cv);
            }
        }
    }

    private void generateAnOverloadDefinitionInner(String _name, CodeGenClassWriter cv) {

        // "(" anOverloadedArg^N ")" returnType
        // Not sure what to do with return type.
        String signature = getSignature();
        String[] exceptions = getExceptions();

        // Right now we extract any necessary static arguments from
        // the principalMember.  This may turn out to be wrong, but it
        // ought to be the case that principalMember is always set to
        // the statically most applicable method for the given set of
        // overloadings under consideration.  [At present this does
        // not always appear to be the case in practice.]
        List<StaticParam> sargs = null;
        /* Some overloads have static args, but even if they are supplied,
         * runtime inference is still necessary (so I think) -- drc 2010-02-24
         */
        if (principalMember != null) {
            sargs = staticParametersOf(principalMember.tagF);
        }

        //String tstr = (exceptions.length==0) ? "" : (" throws " + Useful.list(exceptions));
        //String astr = (sargs==null)? "" : Useful.listInOxfords(sargs);
        // System.err.println(astr + signature + tstr);
        if (CodeGenerationPhase.debugOverloading)
            System.err.println("Emitting overload " + _name + signature);

        String PCNOuter = null;
        Pair<String, List<Pair<String, String>>> pslpss = null; 
        String overloaded_name = oMangle(_name);
        
        if (sargs != null) {
            // Map<String, String> xlation = new HashMap<String, String>();
            pslpss = CodeGen.xlationData(Naming.FUNCTION_GENERIC_TAG);
                        
            String sparamsType = NamingCzar.genericDecoration(sargs, pslpss, ifNone);
            // TODO: which signature is which?  One needs to not have generics info in it.
            String genericArrowType =
                NamingCzar.makeArrowDescriptor(ifNone, overloadedDomain(), getRange());
            
            /* Save this for later, to forestall collisions with
             * single functions that hit the generic type.
             * 
             * Question: is this a problem with references to single
             * types within the overload itself?  I think it might be, if we
             * do not use exactly the same handshake.
             */
            genericSchema = genericArrowType;
            
            String packageAndClassName = NamingCzar.javaPackageClassForApi(ifNone);
            // If we have static arguments, then our caller must be
            // invoking us by instantiating a closure class and then
            // calling its apply method.  Thus we need to make sure
            // that we generate the expected closure class rather than
            // a top-level method.
            String PCN =
                Naming.genericFunctionPkgClass(packageAndClassName, _name,
                                                   sparamsType, genericArrowType);
            PCNOuter =
                Naming.genericFunctionPkgClass(packageAndClassName, _name,
                                                   Naming.LEFT_OXFORD + Naming.RIGHT_OXFORD,
                                                   genericArrowType);
            // System.err.println("Looks generic.\n    signature " + signature +
            //                    "\n    gArrType " + genericArrowType +
            //                    "\n    sparamsType " + sparamsType +
            //                    "\n    PCN " + PCN +
            //                    "\n    PCNOuter " + PCNOuter);
            cv = new CodeGenClassWriter(ClassWriter.COMPUTE_FRAMES, cv);
            overloaded_name = InstantiatingClassloader.closureClassPrefix(PCN, cv, PCN, signature, null);
        }
        MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC
                    + Opcodes.ACC_STATIC, // access,
                    overloaded_name, // name,
                    signature, // sp.getFortressifiedSignature(),
                    null, // signature, // depends on generics, I think
                    exceptions); // exceptions);
        
        generateBody(mv);
        
        if (PCNOuter != null) {
            cv.dumpClass(PCNOuter, pslpss);
        }
    }

    private void generateBody(MethodVisitor mv) {
        mv.visitCode();
        Label fail = new Label();

        generateCall(mv, 0, fail); // Guts of overloaded method

        // Emit failure case
        mv.visitLabel(fail);
        // Boilerplate for throwing an error.
        // mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                           NamingCzar.miscCodegen, NamingCzar.matchFailure, NamingCzar.errorReturn);
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

            List<StaticParam> sargs = staticParametersOf(f.tagF);
            String sparamsType = "";
            String genericArrowType = "";
            
            String ownerName = NamingCzar.apiAndMethodToMethodOwner(f.tagA, f.tagF);
            String mname = NamingCzar.apiAndMethodToMethod(f.tagA, f.tagF);

            // this ought to work better here.
            if (getOverloadSubsets().containsKey(name.stringName()+sig)) {
                mname = NamingCzar.mangleAwayFromOverload(mname);
            }
            
            if (sargs != null) {
                 genericArrowType =
                    NamingCzar.makeArrowDescriptor(ifNone, oa.getDomainType(f.tagF), oa.getRangeType(f.tagF));
                sparamsType = NamingCzar.genericDecoration(sargs, null, ifNone);
                ownerName =
                    Naming.genericFunctionPkgClass(ownerName, mname,
                                                       sparamsType, genericArrowType);
                mname = Naming.APPLIED_METHOD;
            }
            
//            if (getOverloadSubsets().containsKey(name.stringName()+sig)) {
//                mname = NamingCzar.mangleAwayFromOverload(mname);
//            }

            mv.visitMethodInsn(Opcodes.INVOKESTATIC, ownerName, mname, sig);
        }

        /* Boilerplate follows, because this is a subtype. */

        protected AmongApis(APIName ifNone, IdOrOpOrAnonymousName name, TypeAnalyzer ta, OverloadingOracle oa, 
                            Set<TaggedFunctionName> lessSpecificThanSoFar,
                            BASet<Integer> testedIndices, OverloadSet parent, Type selectedParameterType, int paramCount) {
            super(ifNone, name, ta, oa, lessSpecificThanSoFar, testedIndices, parent, selectedParameterType, paramCount);
        }

        public AmongApis(APIName ifNone, IdOrOpOrAnonymousName name, TypeAnalyzer ta, Set<TaggedFunctionName> defs, int n) {
            super(name, ta, new OverloadingOracle(ta), defs, n, ifNone);
        }

        protected OverloadSet makeChild(Set<TaggedFunctionName> childLSTSF, BASet<Integer> childTestedIndices, Type t) {
            return new AmongApis(ifNone, name, ta, oa, childLSTSF,
                    childTestedIndices, this, t, paramCount);
        }

        protected OverloadSet makeSubset(Set<TaggedFunctionName> childLSTSF, TaggedFunctionName _principalMember) {
            OverloadSet subset = new AmongApis(ifNone, name, ta, childLSTSF, paramCount);
            subset.principalMember = _principalMember;
            return subset;
        }

        public int hashCode() {
            return super.hashCode();
        }

        public boolean equals(Object o) {
            if (o == null) return false;
            if ((o.getClass() != this.getClass()) || (o.hashCode() != this.hashCode())) {
                return false;
            }
            else {
                return super.equals(o);
            }
        }

   }

    public boolean notGeneric() {
        // TODO Auto-generated method stub
        for (TaggedFunctionName tfn: lessSpecificThanSoFar) {
            Function f = tfn.getF();
            List<StaticParam> lsp = f.staticParameters();
            if (lsp.size() > 0)
                return false;
            
        }
        return true;
    }

}
