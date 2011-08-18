/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.values;

import com.sun.fortress.compiler.WellKnownNames;
import static com.sun.fortress.exceptions.InterpreterBug.bug;
import static com.sun.fortress.exceptions.ProgramError.error;
import static com.sun.fortress.exceptions.ProgramError.errorMsg;
import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.BuildEnvironments;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.EvalType;
import com.sun.fortress.interpreter.evaluator.EvalVarsEnvironment;
import com.sun.fortress.interpreter.evaluator.types.*;
import com.sun.fortress.interpreter.glue.NativeApp;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.*;
import edu.rice.cs.plt.tuple.Option;

import java.util.*;

public class Constructor extends NonPrimitive {

    // TODO need to be more organized about all the names
    // that get rewritten.
    //   public HashSet<String> parameterNames = new HashSet<String>();

    private HasAt at;
    protected FTypeObject selfType;

    @Override
    public HasAt getAt() {
        return at;
    }

    public String stringName() {
        return "Constructor for " + selfType;
    }

    public boolean seqv(FValue v) {
        return false;
    }

    boolean finished = false;

    IdOrOpOrAnonymousName cfn;
    List<Decl> defs;
    Option<List<Param>> params;

    MultiMap<FTraitOrObject, SingleFcn> traitsToMethodSets = new MultiMap<FTraitOrObject, SingleFcn>();

    MultiMap<String, MethodClosure> namesToSignatureSets = new MultiMap<String, MethodClosure>();

    MultiMap<FTraitOrObject, String> traitsToNamesReferenced = new MultiMap<FTraitOrObject, String>();

    // sets of strings (can't be explicit due to erasure)
    Set<?>[] traitNameReferenceArray;

    FTraitOrObject[] traitArray;
    MethodClosure[] methodsArray;
    MethodClosure[] closuresArray;
    int[] traitIndexForMethod; // 0 = object
    int[] overloadMembership;  // 0 = no overload
    int overloadCount = 0; // first overload is indexed at 1.

    Environment methodsEnv;

    public Constructor(Environment env, FTypeObject selfType, ObjectConstructor def) {
        this(env,
             selfType,
             (HasAt) def,
             NodeFactory.makeConstructorFnName(def),
             NodeUtil.getDecls(def),
             NodeUtil.getParams(def));
        //       addParamsToCollection(def, parameterNames);
    }

    public Constructor(Environment env, FTypeObject selfType, ObjectConstructor def, Option<List<Param>> params) {
        this(env, selfType, (HasAt) def, NodeFactory.makeConstructorFnName(def), NodeUtil.getDecls(def), params);
        //       addParamsToCollection(def, parameterNames);
    }


    /**
     * @param def
     */
    //    static public void addParamsToCollection(
    //          HasParams def, Collection<String> parameterNames) {
    //        addParamsToCollection(def.getParams(), parameterNames);
    //
    //    }
    //    static public void addParamsToCollection(
    //          Option<List<Param>> opt_params, Collection<String> parameterNames) {
    //        if (opt_params.isSome()) {
    //            addParamsToCollection(opt_params.unwrap(), parameterNames);
    //        }
    //    }
    //    static public void addParamsToCollection(
    //          List<Param> params, Collection<String> parameterNames) {
    //        for (Param p : params) {
    //                if (!NodeUtil.isTransient(p))
    //                    parameterNames.add(p.getName().getId().getText());
    //       }
    //    }
    //    static public void removeParamsFromCollection(
    //          ObjectDecl def, Collection<String> parameterNames) {
    //        removeParamsFromCollection(def.getParams(), parameterNames);
    //
    //    }
    //    static public void removeParamsFromCollection(
    //          Option<List<Param>> opt_params, Collection<String> parameterNames) {
    //        if (opt_params.isSome()) {
    //            removeParamsFromCollection(opt_params.unwrap(), parameterNames);
    //        }
    //    }
    //    static public void removeParamsFromCollection(
    //          List<Param> params,Collection<String> parameterNames) {
    //        for (Param p : params) {
    //            if (!NodeUtil.isTransient(p))
    //                parameterNames.remove(p.getName().getId().getText());
    //   }
    //}

    // TODO need to copy the field names
    public Constructor(Environment env,
                       FTypeObject selfType,
                       HasAt def,
                       IdOrOpOrAnonymousName name,
                       List<Decl> defs,
                       Option<List<Param>> params) {
        super(env); // TODO verify that this is the proper env.
        this.selfType = selfType;
        this.at = def;
        this.cfn = name;
        this.defs = defs;
        this.params = params;
    }

    /**
     * Figure out the methods inherited from the super-traits.
     */
    public void finishInitializing() {
        // Next build a bogus environment to help us figure out
        // overloading, shadowing, etc.  First puts win.
        if (params.isSome()) {
            List<Parameter> fparams = EvalType.paramsToParameters(getWithin(), params.unwrap());
            setParams(fparams);
        } else {
            setParams(Collections.<Parameter>emptyList());
        }

        Environment bte = selfType.getMethodExecutionEnv(); // new BetterEnv(getWithin(), getAt());
        selfType.getMembers(); // has initializing side-effect.
        finishInitializing(bte);
    }

    private void finishInitializing(Environment bte) {

        // HashSet<String> fields = new HashSet<String>();
        // HashMap<String, String> com.sun.fortress.interpreter.rewrite =
        //  new HashMap<String, String>();

        //  BuildObjectEnvironment bt =
        //      new BuildObjectEnvironment(bte, selfType.getWithin(), selfType, fields);

        // Inject methods into this environment
        // This should create new MethodClosures
        // visitDefs(bt);

        GHashMap<SingleFcn, FTraitOrObject> signaturesToTraitsContainingMethods =
                new GHashMap<SingleFcn, FTraitOrObject>(SingleFcn.signatureEquivalence);

        MultiMap<String, GenericMethod> generics = new MultiMap<String, GenericMethod>();

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
        Map<String, List<FType>> genericArgs = new HashMap<String, List<FType>>();
        for (Map.Entry<String, Set<GenericMethod>> s : generics.entrySet()) {
            // All the methods are similarly parameterized, so the
            // first generic in the set (generics is a multimap)
            // is as good as any other for this purpose.
            GenericMethod g = s.getValue().iterator().next();

            Applicable ap = g.getDef();
            List<FType> instantiationTypes = SingleFcn.createSymbolicInstantiation(bte, ap, getAt());
            genericArgs.put(s.getKey(), instantiationTypes);
        }

        final Set<String> overridden = new HashSet<String>();
        //        final NodeVisitor_void overrideFinder = new NodeAbstractVisitor_void() {
        //
        //            FnDecl current;
        //
        //            public void forModifierOverride(ModifierOverride mo) {
        //                overridden.add(current.stringName());
        //                System.err.println("Override of " + current.stringName());
        //            }
        //
        //            @Override
        //            public void forFnDecl(FnDecl that) {
        //                current = that;
        //                for (Modifier m : that.getMods()) {
        //                    m.accept(this);
        //                }
        //            }
        //
        //        };


        //  Find all the methods in selfType, using the containing environment
        // to give them meaning.
        accumulateEnvMethods(null,
                             overridden,
                             signaturesToTraitsContainingMethods,
                             generics,
                             genericArgs,
                             selfType,
                             bte);

        //        for (AbsDeclOrDecl d : defs ) {
        //            d.accept(overrideFinder);
        //        }
        // Accumulate all the trait methods, evaluated against their
        // trait environments.
        // TODO The signature map uses EQUALITY, and that might be wrong,
        // if the object can implement with a more general signature.
        for (FType t : extendedTraits) {
            FTypeTrait ft = (FTypeTrait) t;
            BetterEnv e = ft.getMembers();
            accumulateEnvMethods(overridden, null, signaturesToTraitsContainingMethods, generics, genericArgs, ft, e);
        }

        // Check that all methods are defined, also check to see
        // if the object defines any of them.
        boolean objectDefinesAny = false;
        for (Map.Entry<SingleFcn, FTraitOrObject> ent : (Set<Map.Entry<SingleFcn, FTraitOrObject>>) signaturesToTraitsContainingMethods
                .entrySet()) {
            SingleFcn sf = ent.getKey();
            FTraitOrObject too = ent.getValue();

            if (too instanceof FTypeObject) objectDefinesAny = true;
            checkForDef(sf, too);
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
        int traitCount = traitsToNamesReferenced.size() + (objectDefinesAny ? 0 : 1);

        HashMap<FTraitOrObject, Integer> traitToIndex = new HashMap<FTraitOrObject, Integer>();

        traitArray = new FTraitOrObject[traitCount];
        traitNameReferenceArray = new Set<?>[traitCount];

        traitArray[0] = selfType;
        traitToIndex.put(selfType, Integer.valueOf(0));
        int trait_i = 1;
        for (FTraitOrObject too : traitsToNamesReferenced.keySet()) { // was traitsToMethodSets.keySet()
            if (too != selfType) {
                traitArray[trait_i] = too;
                traitToIndex.put(too, Integer.valueOf(trait_i));
                traitNameReferenceArray[trait_i] = traitsToNamesReferenced.get(too);
                trait_i++;
            }
        }

        /* At construction time,
          1) create an array of environments (one per trait)
          2) iterate over the methods, and assign each of them the appropriate
             environment to form closures, setting the results aside.
          3) then form any overloads necessary
          4) then iterate over the traits, binding names to method values.
         */
        int signatureCount = signaturesToTraitsContainingMethods.size() + 1;
        methodsArray = new MethodClosure[signatureCount];
        closuresArray = new MethodClosure[signatureCount];
        HashMap<MethodClosure, Integer> methodsIndex = new HashMap<MethodClosure, Integer>();

        traitIndexForMethod = new int[signatureCount];
        overloadMembership = new int[signatureCount];

        // Iterate over the signatures to create a map from names to sets
        // of functions, for use by overloading.  Also create indexing
        // for methods, and initialize arrays from method (signature)
        // index to partially defined method (carries base environment),
        // and trait index.
        int sig_i = 1;

        for (Map.Entry<SingleFcn, FTraitOrObject> ent : (Set<Map.Entry<SingleFcn, FTraitOrObject>>) signaturesToTraitsContainingMethods
                .entrySet()) {

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
            if (!(sf instanceof MethodClosure)) {
                bug(errorMsg("Internal error, non-method ", sf));
            }
            MethodClosure mc = (MethodClosure) sf;
            FTraitOrObject too = ent.getValue();

            Set<MethodClosure> s = namesToSignatureSets.putItem(sf.asMethodName(), mc);
            if (s.size() == 2) overloadCount++;
            methodsArray[sig_i] = mc;
            methodsIndex.put(mc, Integer.valueOf(sig_i));
            traitIndexForMethod[sig_i] = traitToIndex.get(too).intValue();

            sig_i++;
        }

        // If the same method name is defined with more than one signature,
        // then it is overloaded.  If a method is part of an overload,
        // record its membership in the overloadIndex array.
        int overloadIndex = 1;
        for (Map.Entry<String, Set<MethodClosure>> ent : namesToSignatureSets.entrySet()) {
            Set<MethodClosure> s = ent.getValue();
            if (s.size() > 1) {
                // This will pop an error if the set of defined functions is
                // a bad overload.  Discard the value.
                new OverloadedMethod(ent.getKey(), s, getWithin());

                for (MethodClosure m : s) {
                    overloadMembership[methodsIndex.get(m).intValue()] = overloadIndex;
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


        methodsEnv = within.extend();
        addMethodsToEnv(methodsEnv);
        methodsEnv.bless();

        finished = true;
    }

    /**
     * Checks for definition of sf from supertrait too, fails if absent.
     *
     * @param sf
     * @param too
     * @return
     */
    private void checkForDef(SingleFcn sf, FTraitOrObject too) {
        if (too instanceof FTypeObject) return;
        if (sf instanceof MethodClosure) {
            MethodClosure pdm = (MethodClosure) sf;
            Applicable a = pdm.getDef();
            if (a instanceof FnDecl || a instanceof NativeApp) return;
            if (a.at().equals(sf.at())) {
                error(cfn, errorMsg("Object ",
                                    cfn.stringName(),
                                    " does not define an abstract method declared in type ",
                                    too.getName(),
                                    ":\n",
                                    sf.getString()));
            } else {
                error(cfn, errorMsg("Object ",
                                    cfn.stringName(),
                                    " does not define an abstract method declared in type ",
                                    too.getName(),
                                    ":\n",
                                    sf.getString(),
                                    "\n Instead found: \n",
                                    a.at(),
                                    ": ",
                                    a));
            }
        }
        bug(errorMsg("Unexpected symbolic method binding ", sf));
        return;
    }

    /**
     * Iterates over the youngest (innermost) scope of e to determine
     * what things are defined there, and record the relationship
     * from signature to containing trait, and from trait to names
     * referenced.
     *
     * @param signaturesToTraitsContainingMethods
     *
     * @param genericArgs
     * @param ft
     * @param e
     */
    private void accumulateEnvMethods(Set<String> alreadyOverridden,
                                      Set<String> newOverrides,
                                      GHashMap<SingleFcn, FTraitOrObject> signaturesToTraitsContainingMethods,
                                      MultiMap<String, GenericMethod> generics,
                                      Map<String, List<FType>> genericArgs,
                                      FTraitOrObject ft,
                                      Environment e) {
        for (String s : e.youngestFrame()) {
            FValue fv = e.getLeafValue(s);
            if (alreadyOverridden == null || !alreadyOverridden.contains(s)) {

                // This has got to be wrong...
                if (fv instanceof OverloadedFunction) {
                    // Treat the overloaded function as a bag of separate
                    // definitions.
                    List<Overload> overloads = ((OverloadedFunction) fv).getOverloads();
                    for (Overload ov : overloads) {
                        SingleFcn sfcn = ov.getFn();
                        if (newOverrides != null && sfcn.isOverride()) newOverrides.add(s);
                        // extract below as method, call it here.
                        signaturesToTraitsContainingMethods.putIfAbsent(sfcn, ft);
                    }
                } else if (fv instanceof GenericMethod) {
                    GenericMethod gfv = (GenericMethod) fv;
                    if (newOverrides != null && gfv.isOverride()) newOverrides.add(s);

                    MethodClosure sfcn = gfv.make(genericArgs.get(s), gfv.getAt());

                    signaturesToTraitsContainingMethods.putIfAbsent(sfcn, ft);

                } else if (fv instanceof MethodClosure) {
                    MethodClosure mc = (MethodClosure) fv;
                    signaturesToTraitsContainingMethods.putIfAbsent(mc, ft);
                    if (newOverrides != null && mc.isOverride()) newOverrides.add(s);
                } else {
                    bug(errorMsg("Don't handle ", fv, " yet"));
                }
            }
            // Record the name to ensure that it is defined somewhere.
            // The name of the overloaded function goes wrong, if it is a functional method.
            traitsToNamesReferenced.putItem(ft, s);// ((Fcn)fv).asMethodName());
        }
    }

    private void accumulateGenericMethods(MultiMap<String, GenericMethod> generics, FTraitOrObject ft, Environment e) {
        for (String s : e.youngestFrame()) {
            FValue fv = e.getLeafValue(s);
            if (fv instanceof GenericMethod) {
                // TODO This is VERY approximate
                generics.putItem(s, (GenericMethod) fv);
            } else {
                // do nothing
            }
        }
    }

    public String getString() {
        return cfn.stringName();
    }

    public String toString() {
        //String name = getFnName().toString();
        List<FType> l = null;
        try {
            l = getDomain();
        }
        catch (Throwable th) {
            ; /* do nothing */
        }
        return (s(selfType)) + (l == null ? "(DOMAIN_ERROR null)" : Useful.listInParens(l)) + cfn.at();
    }

    public IdOrOpOrAnonymousName getFnName() {
        return cfn;
    }

    @Override
    public FValue applyInnerPossiblyGeneric(List<FValue> args) {
        return applyConstructor(args);
    }

    /**
     * Apply a constructor.  This method allows separate specification
     * of the lexical environment; this is done to simplify implementation
     * of object expressions.
     */
    public FValue applyConstructor(List<FValue> args) {
        // Problem -- we need to detach self-env from other env.
        if (methodsEnv == null) bug("Null methods env for " + this);

        Environment self_env = buildEnvFromEnvAndParams(methodsEnv, args);

        Environment lex_env = getWithin();
        FObject theObject = makeAnObject(lex_env, self_env);

        self_env.putValueRaw(WellKnownNames.secretSelfName, theObject);

        // TODO this is WRONG.  The vars need to be inserted into self, but
        // get evaluated against the larger (lexical) environment.  Arrrrrrrggggggh.

        self_env.bless(); // HACK we add to this later.
        // This should go wrong if one of the vars has closure value
        // or objectExpr value.

        if (defs.size() > 0) {
            // Minor optimization, avoid this if no defs to eval.
            EvalVarsEnvironment eve = new EvalVarsEnvironment(lex_env.extend(self_env), self_env);
            visitDefs(eve); // HACK here's where we add to self_env.
        }

        return stripTransient(theObject);
    }

    private FObject stripTransient(FObject original) {
        Environment selfEnv = original.getSelfEnv();
        return makeAnObject(original.getLexicalEnv(), selfEnv);
    }

    public FValue applyOEConstructor(HasAt loc, Environment lex_env) {
        // Problem -- we need to detach self-env from other env.
        Environment self_env = methodsEnv.extendAt(loc);

        // TODO This is a problem -- the level is not determined
        FValue surroundSelf = lex_env.getLeafValueNull(WellKnownNames.secretSelfName);
        if (surroundSelf != null) self_env.putValueRaw(WellKnownNames.secretParentName, surroundSelf);

        FObject theObject = makeAnObject(lex_env, self_env);

        self_env.putValueRaw(WellKnownNames.secretSelfName, theObject);

        // TODO this is WRONG.  The vars need to be inserted into self, but
        // get evaluated against the larger (lexical) environment.  Arrrrrrrggggggh.

        self_env.bless(); // HACK we add to this later.
        // This should go wrong if one of the vars has closure value
        // or objectExpr value.

        if (defs.size() > 0) {
            // Minor optimization, avoid this if no defs to eval.
            EvalVarsEnvironment eve = new EvalVarsEnvironment(lex_env.extend(self_env), self_env);
            visitDefs(eve); // HACK here's where we add to self_env.
        }

        return theObject;
    }

    private void addMethodsToEnv(Environment self_env) {
        OverloadedMethod[] overloads = new OverloadedMethod[overloadCount + 1];

        // First initialize an array of environments.
        //        for (int i = 1; i < trait_envs.length; i++) {
        //            trait_envs[i] = new ImmutableSpineEnv(traitArray[i].getEnv(), loc);
        //        }

        // For each method, attach the appropriate environment from the array
        for (int i = 1; i < methodsArray.length; i++) {
            // Closure cl = methodsArray[i].completeClosure(trait_envs[traitIndexForMethod[i]]);
            FunctionClosure cl =
                    closuresArray[i]; // methodsArray[i].completeClosure(traitArray[traitIndexForMethod[i]].getEnv());
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
                self_env.putValueRaw(cl.asMethodName(), cl);
            }
        }

        // By construction, all the overloads are already ok.
        // Bless them to avoid the overhad of checking what we
        // "know" to be true.
        for (int i = 1; i < overloads.length; i++) {
            overloads[i].bless();
        }
    }

    protected FObject makeAnObject(Environment lex_env, Environment self_env) {
        return new FOrdinaryObject(selfType, lex_env, self_env);
    }

    /**
     * @param be
     */
    protected void visitDefs(BuildEnvironments be) {
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
