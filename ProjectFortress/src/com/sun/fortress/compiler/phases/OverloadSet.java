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

package com.sun.fortress.compiler.phases;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.sun.fortress.compiler.NamingCzar;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.Function;
import com.sun.fortress.compiler.typechecker.TypeAnalyzer;
import com.sun.fortress.exceptions.InterpreterBug;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.AnyType;
import com.sun.fortress.nodes.ArrowType;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.nodes.IntersectionType;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.TupleType;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.useful.BASet;
import com.sun.fortress.useful.DefaultComparator;
import com.sun.fortress.useful.F;
import com.sun.fortress.useful.GMultiMap;
import com.sun.fortress.useful.Hasher;
import com.sun.fortress.useful.MagicNumbers;
import com.sun.fortress.useful.MultiMap;
import com.sun.fortress.useful.TopSort;
import com.sun.fortress.useful.TopSortItemImpl;
import com.sun.fortress.useful.Useful;

import edu.rice.cs.plt.tuple.Option;

abstract public class OverloadSet implements Comparable<OverloadSet> {

    static class POType extends TopSortItemImpl<Type> {
        public POType(Type x) {
            super(x);
        }
    }

    static class TaggedFunctionName {
        final private APIName tagA;
        final private Function tagF;
        TaggedFunctionName(APIName a, Function f) {
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
            return tagF.getReturnType();
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

    /**
     * Which parameter is used to split this set into subsets?
     */
    int dispatchParameterIndex;
    OverloadSet[] children;
    boolean splitDone;

    protected OverloadSet(IdOrOpOrAnonymousName name, TypeAnalyzer ta,
                Set<TaggedFunctionName> lessSpecificThanSoFar,
                BASet<Integer> testedIndices, OverloadSet parent, Type selectedParameterType, int paramCount) {
        this.name = name;
        this.ta = ta;
        this.lessSpecificThanSoFar = lessSpecificThanSoFar;
        this.testedIndices = testedIndices;
        this.parent = parent;
        this.selectedParameterType = selectedParameterType;
        this.paramCount = paramCount;
    }

    protected OverloadSet(final APIName apiname, IdOrOpOrAnonymousName name, TypeAnalyzer ta, Set<Function> defs, int n) {

        this(name, ta, Useful.applyToAll(defs, new F<Function, TaggedFunctionName>(){

            @Override
            public TaggedFunctionName apply(Function f) {
                return new TaggedFunctionName(apiname, f);
            }} ),
            new BASet<Integer>(DefaultComparator.<Integer>normal()),
            null, null, n);

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

    abstract protected  void invokeParticularMethod(MethodVisitor mv, TaggedFunctionName f,
            String sig);

    abstract protected OverloadSet makeChild(Set<TaggedFunctionName> childLSTSF, BASet<Integer> childTestedIndices, Type t);


    public void split() {
        if (splitDone)
            return;

        if (lessSpecificThanSoFar.size() == 1) {
                splitDone = true;
                return;
            // If there are no other alternatives, then we are done.
        }

        {
            // Accumulate sets of parameter types.
            int nargs = paramCount;;

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
            int besti = -1; int best = 0;
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
                for (int j = i+1; j < potypes.length; j++) {
                    Type ti = potypes[i].x;
                    Type tj = potypes[j].x;
                    if (ta.subtype(ti, tj).isTrue()) {
                        potypes[i].edgeTo(potypes[j]);
                    } else if (ta.subtype(tj, ti).isTrue()) {
                        potypes[j].edgeTo(potypes[i]);
                    }
                }
            }

            List<TopSortItemImpl<Type>> specificFirst = TopSort.depthFirst(potypes);
            children = new OverloadSet[specificFirst.size()];
            Set<TaggedFunctionName> alreadySelected = new HashSet<TaggedFunctionName>();

            // fill in children.
            for (i = 0; i < specificFirst.size(); i++) {
                Type t = specificFirst.get(i).x;
                Set<TaggedFunctionName> childLSTSF = new HashSet<TaggedFunctionName>();

                    for (TaggedFunctionName f : lessSpecificThanSoFar) {
//                        if (alreadySelected.contains(f))
//                            continue;
                        List<Param> parameters = f.tagParameters();
                        Param p = parameters.get(dispatchParameterIndex);
                        Type pt = p.getIdType().unwrap();
                        if (ta.subtype(t, pt).isTrue()) {
                            childLSTSF.add(f);
                            alreadySelected.add(f);

                        }
                    }

               childLSTSF = thin(childLSTSF, childTestedIndices);

               // ought to not be necessary
               if (paramCount == childTestedIndices.size()) {
                        // Choose most specific member of lessSpecificThanSoFar
                   childLSTSF = mostSpecificMemberOf(childLSTSF);

                }

                children[i] = makeChild(childLSTSF, childTestedIndices, t);

            }
            for (OverloadSet child: children) {
                child.split();
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
                    Type tx = px.get(i).getIdType().unwrap();
                    Type ty = px.get(i).getIdType().unwrap();
                    if (! tx.equals(ty))
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
                List<Param> msf_parameters = msf.tagParameters();
                List<Param> cand_parameters = candidate.tagParameters();
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
                    if (! ta.subtype(cand_t, msf_t).isTrue()) {
                        cand_better = false;
                        break;
                    }
                }
                if (cand_better)
                    msf = candidate;

            }
        }
        if (msf == null)
            return Collections.<TaggedFunctionName>emptySet();
        else
            return Collections.singleton(msf);
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

        Type r = null;

        for (TaggedFunctionName f : lessSpecificThanSoFar) {
            r = join(r, f.getReturnType(), ta);
        }
        s += NamingCzar.only.boxedImplDesc(r);

        return s;
    }

    /**
     * @param r
     * @param r0
     * @return
     */
    private static Type join(Type r, Type r0, TypeAnalyzer ta) {
        // Eventually we want to use the join from TypeAnalyzer, once we
        // figure out how to deal with union types.
        if(r == null) {
            r = r0;
        } else if (r.equals(r0)) {
            // ok
        } else if (ta.subtype(r, r0).isTrue()) {
            r = r0;
        } else if (ta.subtype(r0, r).isTrue()) {
            // ok
        } else if (!(r instanceof AnyType)) {
            // Locate the any at the place we realized it was necessary.
            r = NodeFactory.makeAnyType(r0.getInfo().getSpan());
        }
        return r;
    }

    /**
     * Given an intersection of arrow types (what we get for at least some
     * overloaded functions) and an indication of how many parameters actually
     * appeared, create the signature for the overloaded function that will be
     * called.
     *
     * There's some fiddliness with varargs that has to be sorted out.
     *
     * @param t
     * @param paramCount
     * @return
     */
    public static String getSignature(IntersectionType t, int paramCount, TypeAnalyzer ta) {

        String s = overloadedDomainSig(t, paramCount, ta);

        Type r = getRangeSignature(t, paramCount, ta);

        s += NamingCzar.only.boxedImplDesc(r);

        return s;
    }

    private static Type getRangeSignature(IntersectionType t, int paramCount, TypeAnalyzer ta) {
        List<Type> types = t.getElements();

        Type r = null;

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

            r = join(r, r0, ta);

            if (r instanceof AnyType)
                break;
        }
        return r;
    }

    private  static Type getParamType(IntersectionType t, int i, int paramCount, TypeAnalyzer ta) {
        List<Type> types = t.getElements();

        Type r = null;

        for (Type type : types) {
            Type r0;

            if (type instanceof ArrowType) {
                ArrowType at = (ArrowType) type;
                r0 = at.getDomain();
                
                // Ensure that the domain count matches the paramCount
                if (r0 instanceof TupleType) {
                    TupleType tt = (TupleType) r0;
                    if (paramCount != tt.getElements().size())
                        continue;
                    r0 = tt.getElements().get(i);
                } else if (paramCount != 1) {
                    continue;
                }
            } else if (type instanceof IntersectionType) {
                r0 = getParamType((IntersectionType) type, i, paramCount, ta);
            } else {
                InterpreterBug.bug("Non arrowtype " + type + " in (function) intersection type");
                return null; // not reached
            }

            r = join(r, r0, ta);

            if (r instanceof AnyType)
                break;
        }
        return r;
    }

    /**
     * @param paramCount
     * @return
     */
    private static String overloadedDomainSig(IntersectionType t, int paramCount, TypeAnalyzer ta) {
        String s = "(";

        for (int i = 0; i < paramCount; i++) {
            s += NamingCzar.only.boxedImplDesc(getParamType(t,i,paramCount, ta));
        }
        s += ")";
        return s;
    }

    private String overloadedDomainSig() {
        String s = "(";

        for (int i = 0; i < paramCount; i++) {
            s += NamingCzar.only.boxedImplDesc(overloadedParamType(i));
        }
        s += ")";
        return s;
    }

    private Type overloadedParamType(int param) {
        Type r = null;
        for (TaggedFunctionName f : lessSpecificThanSoFar) {
            List<Param> params = f.tagParameters();
            Param p = params.get(param);
            r = join(r, p.getIdType().unwrap(), ta);
        }
        return r;
    }

    public String[] getExceptions() {
        HashSet<Type> exceptions = new HashSet<Type>();
        for (TaggedFunctionName f : lessSpecificThanSoFar) {
            List<BaseType> f_exceptions =  f.thrownTypes();
            exceptions.addAll(f_exceptions);
        }
        String[] string_exceptions = new String[exceptions.size()];
        int i = 0;
        for (Type e : exceptions) {
            string_exceptions[i++] = NamingCzar.only.boxedImplDesc(e);
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
            TaggedFunctionName f =  lessSpecificThanSoFar.iterator().next();

            List<Param> params = f.callParameters();
            int i = firstArgIndex;
            String sig = "(";
            for (Param p : params ) {
                mv.visitVarInsn(Opcodes.ALOAD, i);
                String toType = NamingCzar.only.boxedImplDesc(p.getIdType().unwrap());
                sig += toType;
                mv.visitTypeInsn(Opcodes.CHECKCAST, toType);
                i++;
            }
            sig += ")";
            sig += NamingCzar.only.boxedImplDesc(f.getReturnType());
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
                mv.visitTypeInsn(Opcodes.INSTANCEOF, NamingCzar.only.boxedImplDesc(os.selectedParameterType));
                mv.visitJumpInsn(Opcodes.IFEQ, lookahead);
                os.generateCall(mv, firstArgIndex, failLabel);
                mv.visitLabel(lookahead);
                lookahead = new Label();
            }
            mv.visitJumpInsn(Opcodes.GOTO, failLabel);
        }
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
            String s =  indent + "#" + dispatchParameterIndex + "\n";
            for (int i = 0; i < children.length; i++) {
                OverloadSet os = children[i];
                s += indent + os.selectedParameterType + "->" + os.toStringR(indent + "   ") + "\n";
            }
            return s;
        }
    }

    public void generateAnOverloadDefinition(String name, ClassVisitor cv) {

        // "(" anOverloadedArg^N ")" returnType
        // Not sure what to do with return type.
        String signature = getSignature();
        String[] exceptions = getExceptions();
        if (CodeGenerationPhase.debugOverloading)
            System.err.println("Emitting overload " + name + signature);

        MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC
                + Opcodes.ACC_STATIC, // access,
                name, // name,
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



    static public class Foreign extends OverloadSet {
        /**
         * Emit the invocation for a particular type of overloaded functions.
         *
         * @param mv
         * @param f
         * @param sig
         */
        protected void invokeParticularMethod(MethodVisitor mv, TaggedFunctionName f,
                String sig) {
            String pname = NamingCzar.only.apiNameToPackageName(f.tagA);
            String cnameDOTmname = name.toString();

            int idot = cnameDOTmname.lastIndexOf(".");
            String ownerName;
            String mname;
            if (idot == -1) {
                ownerName = Useful.replace(pname, ".", "/") ;
                mname = cnameDOTmname;
            } else {
                ownerName = Useful.replace(pname, ".", "/") + "/" + cnameDOTmname.substring(0,idot);
                mname = cnameDOTmname.substring(idot+1);
            }

            mv.visitMethodInsn(Opcodes.INVOKESTATIC, ownerName, mname, sig);
        }

        /* Boilerplate follows, because this is a subtype. */

        protected Foreign(IdOrOpOrAnonymousName name, TypeAnalyzer ta,
                Set<TaggedFunctionName> lessSpecificThanSoFar,
                BASet<Integer> testedIndices, OverloadSet parent, Type selectedParameterType, int paramCount) {
            super(name, ta, lessSpecificThanSoFar, testedIndices, parent, selectedParameterType, paramCount);
        }

        public Foreign(final APIName apiname, IdOrOpOrAnonymousName name, TypeAnalyzer ta, Set<Function> defs, int n) {
            super(apiname, name, ta, defs, n);
        }

        protected OverloadSet makeChild(Set<TaggedFunctionName> childLSTSF, BASet<Integer> childTestedIndices, Type t) {
            return new Foreign(name, ta, childLSTSF,
                    childTestedIndices, this, t, paramCount);
        }


    }

    static public class Local extends OverloadSet {
        String packageAndClassName;
        
        /**
         * Emit the invocation for a particular type of overloaded functions.
         *
         * @param mv
         * @param f
         * @param sig
         */
        protected void invokeParticularMethod(MethodVisitor mv, TaggedFunctionName f,
                String sig) {
            String mname = name.toString();

            String ownerName= packageAndClassName;
                
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, ownerName, mname, sig);
        }

        /* Boilerplate follows, because this is a subtype. */

        protected Local(String packageAndClassName, IdOrOpOrAnonymousName name, TypeAnalyzer ta,
                Set<TaggedFunctionName> lessSpecificThanSoFar,
                BASet<Integer> testedIndices, OverloadSet parent, Type selectedParameterType, int paramCount) {
            super(name, ta, lessSpecificThanSoFar, testedIndices, parent, selectedParameterType, paramCount);
            this.packageAndClassName = packageAndClassName;
        }

        public Local(String packageAndClassName, final APIName apiname, IdOrOpOrAnonymousName name, TypeAnalyzer ta, Set<Function> defs, int n) {
            super(apiname, name, ta, defs, n);
            this.packageAndClassName = packageAndClassName;
        }

        protected OverloadSet makeChild(Set<TaggedFunctionName> childLSTSF, BASet<Integer> childTestedIndices, Type t) {
            return new Local(packageAndClassName, name, ta, childLSTSF,
                    childTestedIndices, this, t, paramCount);
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
            String pname = NamingCzar.only.apiNameToPackageName(f.tagA);
            String cnameDOTmname = name.toString();

            String ownerName;
            String mname;
            ownerName = Useful.replace(pname, ".", "/") ;
            mname = cnameDOTmname;

            mv.visitMethodInsn(Opcodes.INVOKESTATIC, ownerName, mname, sig);
        }

        /* Boilerplate follows, because this is a subtype. */

        protected AmongApis(IdOrOpOrAnonymousName name, TypeAnalyzer ta,
                Set<TaggedFunctionName> lessSpecificThanSoFar,
                BASet<Integer> testedIndices, OverloadSet parent, Type selectedParameterType, int paramCount) {
            super(name, ta, lessSpecificThanSoFar, testedIndices, parent, selectedParameterType, paramCount);
        }

        public AmongApis(final APIName apiname, IdOrOpOrAnonymousName name, TypeAnalyzer ta, Set<Function> defs, int n) {
            super(apiname, name, ta, defs, n);
        }

        protected OverloadSet makeChild(Set<TaggedFunctionName> childLSTSF, BASet<Integer> childTestedIndices, Type t) {
            return new AmongApis(name, ta, childLSTSF,
                    childTestedIndices, this, t, paramCount);
        }

    }

}