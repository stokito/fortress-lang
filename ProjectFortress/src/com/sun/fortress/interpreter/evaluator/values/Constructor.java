/*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
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

package com.sun.fortress.interpreter.evaluator.values;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.BuildEnvironments;
import com.sun.fortress.interpreter.evaluator.BuildObjectEnvironment;
import com.sun.fortress.interpreter.evaluator.EvalVarsEnvironment;
import com.sun.fortress.interpreter.evaluator.ProgramError;
import com.sun.fortress.interpreter.evaluator.types.FTraitOrObject;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeArrow;
import com.sun.fortress.interpreter.evaluator.types.FTypeObject;
import com.sun.fortress.interpreter.evaluator.types.FTypeTrait;
import com.sun.fortress.interpreter.evaluator.types.FTypeTuple;
import com.sun.fortress.interpreter.glue.WellKnownNames;
import com.sun.fortress.interpreter.nodes.Applicable;
import com.sun.fortress.interpreter.nodes.ConstructorFnName;
import com.sun.fortress.interpreter.nodes.DefOrDecl;
import com.sun.fortress.interpreter.nodes.FnDecl;
import com.sun.fortress.interpreter.nodes.FnName;
import com.sun.fortress.interpreter.nodes.GenericDefOrDeclWithParams;
import com.sun.fortress.interpreter.nodes.HasParams;
import com.sun.fortress.interpreter.nodes.ObjectDecl;
import com.sun.fortress.interpreter.useful.Option;
import com.sun.fortress.interpreter.nodes.Param;
import com.sun.fortress.interpreter.nodes_util.NodeUtil;
import com.sun.fortress.interpreter.useful.GHashMap;
import com.sun.fortress.interpreter.useful.HasAt;
import com.sun.fortress.interpreter.useful.MultiMap;
import com.sun.fortress.interpreter.useful.NI;
import com.sun.fortress.interpreter.useful.ReversedList;


public class Constructor extends AnonymousConstructor {

    // TODO need to be more organized about all the names
    // that get rewritten.
    public HashSet<String> parameterNames = new HashSet<String>();

    public Constructor(BetterEnv env,
            FTypeObject selfType,
            GenericDefOrDeclWithParams def) {
        this(env, selfType, (HasAt) def, new ConstructorFnName(def),
                def.getDefOrDecls());
        addParamsToCollection(def, parameterNames);
    }

    /**
     * @param def
     */
    static public void addParamsToCollection(
          HasParams def, Collection<String> parameterNames) {
        addParamsToCollection(def.getParams(), parameterNames);

    }
    static public void addParamsToCollection(
          Option<List<Param>> opt_params, Collection<String> parameterNames) {
        if (opt_params.isPresent()) {
            addParamsToCollection(opt_params.getVal(), parameterNames);
        }
    }
    static public void addParamsToCollection(
          List<Param> params, Collection<String> parameterNames) {
        for (Param p : params) {
                if (!NodeUtil.isTransient(p))
                    parameterNames.add(p.getName().getName());
       }
    }
    static public void removeParamsFromCollection(
          ObjectDecl def, Collection<String> parameterNames) {
        removeParamsFromCollection(def.getParams(), parameterNames);

    }
    static public void removeParamsFromCollection(
          Option<List<Param>> opt_params, Collection<String> parameterNames) {
        if (opt_params.isPresent()) {
            removeParamsFromCollection(opt_params.getVal(), parameterNames);
        }
    }
    static public void removeParamsFromCollection(
          List<Param> params,Collection<String> parameterNames) {
        for (Param p : params) {
            if (!NodeUtil.isTransient(p))
                parameterNames.remove(p.getName().getName());
   }
}

    // TODO need to copy the field names

    public Constructor(BetterEnv env, FTypeObject selfType, HasAt def,
                FnName name, List<? extends DefOrDecl> defs) {
        super(env, selfType, def); // TODO verify that this is the proper env.
        this.cfn = name;
        this.defs = defs;
    }

    /**
      * Figure out the methods inherited from the super-traits.
      */
    public void finishInitializing() {
        // Next build a bogus environment to help us figure out
        // overloading, shadowing, etc.  First puts win.
        BetterEnv bte = new BetterEnv(getWithin(), getAt());
        finishInitializing(bte);
    }

    public void finishInitializing(BetterEnv bte) {

        GHashMap<SingleFcn, FTraitOrObject>
            signaturesToTraitsContainingMethods =
                new GHashMap<SingleFcn, FTraitOrObject>(
                         SingleFcn.signatureEquivalence);

        MultiMap<String, GenericMethod> generics =
            new MultiMap<String, GenericMethod>();

        HashSet<String> fields = new HashSet<String>();
         // HashMap<String, String> com.sun.fortress.interpreter.rewrite =
        //  new HashMap<String, String>();

        BuildObjectEnvironment bt =
            new BuildObjectEnvironment(bte, selfType.getEnv(), fields);

        // Inject methods into this environment
        // This should create new MethodClosures
        visitDefs(bt);

        // TODO deal with ORDER and ambiguity.  TransitiveExtends returns
        // a topological sort, which is close, but not perfect.
        //
        List<FType> extendedTraits = selfType.getProperTransitiveExtends();

        // First process all the generics, so we can form generic functions
        // TODO this is approximate; add generics in reversed order.
        for (FType t : new ReversedList<FType>(extendedTraits)) {
            FTypeTrait ft = (FTypeTrait) t;
            BetterEnv e = ft.getMembers(); // This is correct, it enumerates the methods.
            accumulateGenericMethods(generics, ft, e);
        }

        // This also enumerates the methods.
        accumulateGenericMethods(generics, selfType, bte);

        // For each set of generic methods (with same name and signature)
        // create a list of symbolic values to substitute for the type
        // parameters so that the methods may be pseudo-instantiated
        // and plugged into the trait/object and overloading definition
        // machinery.
        Map<String, List<FType>> genericArgs =
            new HashMap<String, List<FType>>();
        for (String s : generics.keySet() ) {
            // All the methods are similarly parameterized, so the
            // first generic in the set (generics is a multimap)
            // is as good as any other for this purpose.
            GenericMethod g = generics.get(s).iterator().next();

            Applicable ap = g.getDef();
            List<FType> instantiationTypes =
                SingleFcn.createSymbolicInstantiation(bte, ap, getAt());
            genericArgs.put(s, instantiationTypes);
        }

        //  Find all the methods in selfType, using the containing environment
        // to give them meaning.
        accumulateEnvMethods(
          signaturesToTraitsContainingMethods,
          generics, genericArgs, selfType, bte);

        // Accumulate all the trait methods, evaluated against their
        // trait environments.
        // TODO The signature map uses EQUALITY, and that might be wrong,
        // if the object can implement with a more general signature.
        for (FType t : extendedTraits) {
            FTypeTrait ft = (FTypeTrait) t;
            BetterEnv e = ft.getMembers();
            accumulateEnvMethods(
             signaturesToTraitsContainingMethods, generics, genericArgs, ft, e);
        }

        // Check that all methods are defined, also check to see
        // if the object defines any of them.
        boolean objectDefinesAny = false;
        for (Map.Entry<SingleFcn, FTraitOrObject> ent :
                 (Set<Map.Entry<SingleFcn, FTraitOrObject>>)
                     signaturesToTraitsContainingMethods.entrySet()) {
            SingleFcn sf = ent.getKey();
            FTraitOrObject too = ent.getValue();

            if (too instanceof FTypeObject)
                objectDefinesAny = true;
            if (isNotADef(sf, too))
                throw new ProgramError(cfn,
                        "Object " + NodeUtil.stringName(cfn) +
                        " does not define method " + sf.getString() +
                        " declared in " + too.getName());
        }

        // Plan to iterate over traits at instantiation, and form closures
        // for methods defined there based on the trait environment and
        // a filter of the object environment into that trait.
        // Duplication of symbols into the per-method environements is
        // ok because the symbols duplicated are immutably bound (are
        // methods).
        traitsToMethodSets.addInverse(signaturesToTraitsContainingMethods);

        // Count traits and create an array of trait types, so that
        // the constructor can build things without thinking.
        // int traitCount = traitsToMethodSets.size() + (objectDefinesAny ? 0 : 1);
        int traitCount =
            traitsToNamesReferenced.size() + (objectDefinesAny ? 0 : 1);

        HashMap<FTraitOrObject, Integer> traitToIndex =
            new HashMap<FTraitOrObject, Integer> ();

        traitArray = new FTraitOrObject[traitCount];
        // The cast is really checking the erased type.
        traitNameReferenceArray = (Set<String>[]) new Set[traitCount];

        traitArray[0] = selfType;
        traitToIndex.put(selfType, Integer.valueOf(0));
        int trait_i = 1;
        for (FTraitOrObject too : traitsToNamesReferenced.keySet() ) { // was traitsToMethodSets.keySet()
            if (too != selfType) {
                traitArray[trait_i] = too;
                traitToIndex.put(too, Integer.valueOf(trait_i));
                traitNameReferenceArray[trait_i] =
                    traitsToNamesReferenced.get(too);
                trait_i ++;
            }
        }

        /* At construction time,
          1) create an array of environments (one per trait)
          2) iterate over the methods, and assign each of them the appropiate
             environment to form closures, setting the results aside.
          3) then form any overloads necessary
          4) then iterate over the traits, binding names to method values.
         */
        int signatureCount = signaturesToTraitsContainingMethods.size()+1;
        methodsArray = new MethodClosure[signatureCount];
        closuresArray = new MethodClosure[signatureCount];
        HashMap<MethodClosure, Integer> methodsIndex =
            new HashMap<MethodClosure, Integer> ();

        traitIndexForMethod = new int[signatureCount];
        overloadMembership = new int[signatureCount];

        // Iterate over the signatures to create a map from names to sets
        // of functions, for use by overloading.  Also create indexing
        // for methods, and initialize arrays from method (signature)
        // index to partially defined method (carries base environment),
        // and trait index.
        int sig_i = 1;

        for (Map.Entry<SingleFcn, FTraitOrObject> ent :
            (Set<Map.Entry<SingleFcn, FTraitOrObject>>)
            signaturesToTraitsContainingMethods.entrySet()) {

            SingleFcn sf = ent.getKey();
            /*
             * This is a moderate hack.  Because the methods for any
             * particular trait must obey the overloading rules individually,
             * overloaded things also get bound "as methods".  However, the
             * the rules for binding methods to actual objects behave as-if the
             * overloaded method is dismantled and separately overridden.  To
             * take advantage of the SingleFcn-specific machinery in
             * OverloadedFunctions, the type signature of the "early" data
             * structures in this process allow SingleFcn's to appear, but
             * now (right here) they had better be gone.
             *
             */
            if (! (sf instanceof MethodClosure)) {
                throw new
                ProgramError("Internal error, non-method " +sf);

            }
            MethodClosure mc = (MethodClosure) sf;
            FTraitOrObject too = ent.getValue();

            Set<MethodClosure> s =
                namesToSignatureSets.putItem(sf.asMethodName(), mc);
            if (s.size() == 2)
                overloadCount++;
            methodsArray[sig_i] = mc;
            methodsIndex.put(mc, Integer.valueOf(sig_i));
            traitIndexForMethod[sig_i] = traitToIndex.get(too).intValue();

            sig_i++;
        }

        // If the same method name is defined with more than one signature,
        // then it is overloaded.  If a method is part of an overload,
        // record its membership in the overloadIndex array.
        int overloadIndex = 1;
        for (Map.Entry<String, Set<MethodClosure>> ent:
            namesToSignatureSets.entrySet()) {
            Set<MethodClosure> s = ent.getValue();
            if (s.size() > 1) {
                // This will pop an error if the set of defined functions is
                // a bad overload.  Discard the value.
                new OverloadedMethod(ent.getKey(), s, getWithin());

                for (MethodClosure m : s) {
                    overloadMembership[methodsIndex.get(m).intValue() ] =
                        overloadIndex;
                }
                overloadIndex++;
            }
        }

        // Experimental early computation of this information.
        for (int i = 1; i < methodsArray.length; i++) {
            // Closure cl = methodsArray[i].completeClosure(trait_envs[traitIndexForMethod[i]]);
            MethodClosure cl = methodsArray[i];
            // .completeClosure(traitArray[traitIndexForMethod[i]].getEnv());
            closuresArray[i] = cl;
        }

            finished = true;
    }

    /**
      * Returns true iff the function sf defined in trait-or-object
      * too is NOT a definition.
      * @param sf
      * @param too
      * @return
      */
    private boolean isNotADef(SingleFcn sf, FTraitOrObject too) {
        if (too instanceof FTypeObject) return false;
        if (sf instanceof MethodClosure) {
            MethodClosure pdm = (MethodClosure) sf;
            Applicable a = pdm.getDef();
            if (a instanceof FnDecl)
                return false;
            return true;
        }
        throw new ProgramError("Unexpected symbolic method binding " + sf);
    }

    /**
     * Iterates over the youngest (innermost) scope of e to determine
     * what things are defined there, and record the relationship
     * from signature to containing trait, and from trait to names
     * referenced.
     *
     * @param signaturesToTraitsContainingMethods
     * @param genericArgs
     * @param ft
     * @param e
     */
    private void accumulateEnvMethods(
            GHashMap<SingleFcn, FTraitOrObject> signaturesToTraitsContainingMethods,
            MultiMap<String, GenericMethod> generics,
            Map<String, List<FType>> genericArgs, FTraitOrObject ft, BetterEnv e) {
        for (String s : e) {
            FValue fv = e.getValue(s);
            // This has got to be wrong...
            if (fv instanceof OverloadedFunction) {
                // Treat the overloaded function as a bag of separate
                // definitions.
                List<Overload> overloads = ((OverloadedFunction) fv)
                        .getOverloads();
                for (Overload ov : overloads) {
                    SingleFcn sfcn = ov.getFn();
                    // extract below as method, call it here.
                    signaturesToTraitsContainingMethods.putIfAbsent(sfcn, ft);
                }
            } else
                if (fv instanceof GenericMethod) {
                GenericMethod gfv = (GenericMethod) fv;
                MethodClosure sfcn = gfv.make(genericArgs.get(s), gfv.getAt());
                signaturesToTraitsContainingMethods.putIfAbsent(sfcn, ft);

            } else if (fv instanceof MethodClosure) {
                signaturesToTraitsContainingMethods.putIfAbsent
                    ((MethodClosure) fv, ft);
            } else {
                NI.nyi("Don't handle " + fv + " yet");
            }
            // Record the name to ensure that it is defined somewhere.
            traitsToNamesReferenced.putItem(ft, ((Fcn)fv).asMethodName());
        }
    }

    private void accumulateGenericMethods(
            MultiMap<String, GenericMethod> generics,
            FTraitOrObject ft, BetterEnv e) {
        for (String s : e) {
            FValue fv = e.getValue(s);
            if (fv instanceof GenericMethod) {
                // TODO This is VERY approximate
                generics.putItem(s, (GenericMethod) fv);
            } else {
                // do nothing
            }
        }
    }

    public String getString() {
        return NodeUtil.stringName(cfn);
    }

    boolean finished = false;

    FnName cfn;
    List<? extends DefOrDecl> defs;

    MultiMap<FTraitOrObject, SingleFcn> traitsToMethodSets =
        new MultiMap<FTraitOrObject, SingleFcn>();

    MultiMap<String, MethodClosure> namesToSignatureSets =
        new MultiMap<String, MethodClosure>();

    MultiMap<FTraitOrObject, String> traitsToNamesReferenced =
        new MultiMap<FTraitOrObject, String>();

    Set<String>[] traitNameReferenceArray;
    FTraitOrObject[] traitArray;
    MethodClosure[] methodsArray;
    MethodClosure[] closuresArray;
    int[] traitIndexForMethod; // 0 = object
    int[] overloadMembership;  // 0 = no overload
    int overloadCount = 0; // first overload is indexed at 1.

    public FnName getFnName() {
        return cfn;
    }

    @Override
    public FValue applyInner(
            List<FValue> args, HasAt loc, BetterEnv envForInference) {
        BetterEnv lex_env = getWithin();
        return applyConstructor(args, loc, lex_env);
    }
    /**
     *
     * Apply a constructor.  This method allows separate specification
     * of the lexical environment; this is done to simplify implementation
     * of object expressions.
     *
     */
    public FValue applyConstructor(
            List<FValue> args, HasAt loc, BetterEnv lex_env) {
        // Problem -- we need to detach self-env from other env.
        BetterEnv self_env = buildEnvFromParams(args, loc);


        // BuildEnvironments be = new BuildObjectEnvironment(env);

        /* For each trait that supplies at least one method,
         * construct an environment based on the trait.  To that
         * environment, add the definitions for each NAME (after
         * we resolve overloading) that is defined in that environment,
         * based on the definition that is injected into the object
         * environment.
         */

        /*
         * TODO: methods with 'self' in their parameter list
         * create (additional) overloadings in the surrounding
         * environment!
         */

        // visitDefs(be);

        BetterEnv[] trait_envs = new BetterEnv[traitArray.length];
        trait_envs[0] = self_env;
        OverloadedMethod[] overloads = new OverloadedMethod[overloadCount+1];

        // First initialize an array of environments.
//        for (int i = 1; i < trait_envs.length; i++) {
//            trait_envs[i] = new ImmutableSpineEnv(traitArray[i].getEnv(), loc);
//        }

        // For each method, attach the appropriate environment from the array
        for (int i = 1; i < methodsArray.length; i++) {
            // Closure cl = methodsArray[i].completeClosure(trait_envs[traitIndexForMethod[i]]);
            Closure cl = closuresArray[i]; // methodsArray[i].completeClosure(traitArray[traitIndexForMethod[i]].getEnv());
            int j = overloadMembership[i];
            // Check to see if the new closure should be bound now or
            // placed in an overload.
            if (j != 0) {
                OverloadedMethod of = overloads[j];
                if (of == null) {
                    // Overload in self_env
                    of = new OverloadedMethod(cl.asMethodName(), self_env);
                    self_env.putValue(cl.asMethodName(), of);
                    overloads[j] = of;
                }
                of.addOverload(cl);
            } else {
                // Assertion -- by construction, illegal shadowing won't occur,
                // because it is processed/detected in the overloading code.
                self_env.putValueUnconditionally(cl.asMethodName(), cl);
            }
        }

        // By construction, all the overloads are already ok.
        // Bless them to avoid the overhad of checking what we
        // "know" to be true.
        for (int i = 1; i < overloads.length; i++) {
            overloads[i].bless();
        }

        // Evaluate any vars defined inline within the environment.

        // TODO, crap, we need to build an environment for those, too.
        // This means we probably need to rewrite their field references
        // to "self", and be sure it is defined in there.
        // BUT! we have not "made an object yet".

        // The above remarks are relatively wrong -- field initializers
        // may not self-refer.

        FValue surroundSelf = lex_env.getValueNull(WellKnownNames.secretSelfName);
        if (surroundSelf != null)
            self_env.putValueUnconditionally(WellKnownNames.secretParentName, surroundSelf);

        FObject theObject = makeAnObject(lex_env, self_env);

        self_env.putValueUnconditionally(WellKnownNames.secretSelfName, theObject);

        // TODO this is WRONG.  The vars need to be inserted into self, but
        // get evaluated against the larger (lexical) environment.  Arrrrrrrggggggh.

        self_env.bless(); // HACK we add to this later.

        EvalVarsEnvironment eve = new EvalVarsEnvironment(new BetterEnv(lex_env, self_env), self_env);
        visitDefs(eve); // HACK here's where we add to self_env.

        return theObject;
    }

    protected FObject makeAnObject(BetterEnv lex_env, BetterEnv self_env) {
        return new FObject(selfType, lex_env, self_env);
    }

    /**
     * @param be
     */
    private void visitDefs(BuildEnvironments be) {
        be.doDefs1234(defs);
//        be.secondPass();
//
//        be.doDefs(defs);
//        be.getBindingEnv().bless();
//
//        be.thirdPass();
//        be.doDefs(defs);
//
//        be.fourthPass();
//        be.doDefs(defs);

    }

    @Override
    protected void setValueType() {
        // TODO Constructors aren't exposed, do they have a type?
        // yes, they are exposed, and they do.
        setFtype(FTypeArrow.make(getDomain(), selfType));
    }

    @Override
    boolean getFinished() {
        // TODO Auto-generated method stub
        return finished;
    }

}
