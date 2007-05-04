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

package com.sun.fortress.interpreter.evaluator;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.env.FortressTests;
import com.sun.fortress.interpreter.env.LazilyEvaluatedCell;
import com.sun.fortress.interpreter.evaluator.scopes.SComponent;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeDynamic;
import com.sun.fortress.interpreter.evaluator.types.FTypeGeneric;
import com.sun.fortress.interpreter.evaluator.types.FTypeObject;
import com.sun.fortress.interpreter.evaluator.types.FTypeTrait;
import com.sun.fortress.interpreter.evaluator.types.SymbolicType;
import com.sun.fortress.interpreter.evaluator.types.SymbolicWhereType;
import com.sun.fortress.interpreter.evaluator.types.TypeGeneric;
import com.sun.fortress.interpreter.evaluator.values.Closure;
import com.sun.fortress.interpreter.evaluator.values.Constructor;
import com.sun.fortress.interpreter.evaluator.values.FGenericFunction;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.Fcn;
import com.sun.fortress.interpreter.evaluator.values.FunctionalMethod;
import com.sun.fortress.interpreter.evaluator.values.GenericConstructor;
import com.sun.fortress.interpreter.evaluator.values.GenericFunctionSet;
import com.sun.fortress.interpreter.evaluator.values.GenericMethod;
import com.sun.fortress.interpreter.evaluator.values.GenericMethodSet;
import com.sun.fortress.interpreter.evaluator.values.OverloadedFunction;
import com.sun.fortress.interpreter.evaluator.values.Parameter;
import com.sun.fortress.interpreter.evaluator.values.Simple_fcn;
import com.sun.fortress.interpreter.nodes.AbsFnDecl;
import com.sun.fortress.interpreter.nodes.AbsObjectDecl;
import com.sun.fortress.interpreter.nodes.AbsTraitDecl;
import com.sun.fortress.interpreter.nodes.AbsVarDecl;
import com.sun.fortress.interpreter.nodes.Api;
import com.sun.fortress.interpreter.nodes.Applicable;
import com.sun.fortress.interpreter.nodes.CompilationUnit;
import com.sun.fortress.interpreter.nodes.Generic;
import com.sun.fortress.interpreter.nodes.Node;
import com.sun.fortress.interpreter.nodes.NodeVisitor;
import com.sun.fortress.interpreter.nodes.Component;
import com.sun.fortress.interpreter.nodes.DefOrDecl;
import com.sun.fortress.interpreter.nodes.Dimension;
import com.sun.fortress.interpreter.nodes.DottedId;
import com.sun.fortress.interpreter.nodes.Expr;
import com.sun.fortress.interpreter.nodes.FnDecl;
import com.sun.fortress.interpreter.nodes.FnDefOrDecl;
import com.sun.fortress.interpreter.nodes.FnName;
import com.sun.fortress.interpreter.nodes.Id;
import com.sun.fortress.interpreter.nodes.ImportApi;
import com.sun.fortress.interpreter.nodes.ImportNames;
import com.sun.fortress.interpreter.nodes.ImportStar;
import com.sun.fortress.interpreter.nodes.LValue;
import com.sun.fortress.interpreter.nodes.LValueBind;
import com.sun.fortress.interpreter.nodes.Modifier;
import com.sun.fortress.interpreter.nodes.ObjectDefOrDecl;
import com.sun.fortress.interpreter.nodes.ObjectDecl;
import com.sun.fortress.interpreter.nodes.ObjectExpr;
import com.sun.fortress.interpreter.nodes.Option;
import com.sun.fortress.interpreter.nodes.Param;
import com.sun.fortress.interpreter.nodes.StaticParam;
import com.sun.fortress.interpreter.nodes.TightJuxt;
import com.sun.fortress.interpreter.nodes.TraitDefOrDecl;
import com.sun.fortress.interpreter.nodes.TraitDecl;
import com.sun.fortress.interpreter.nodes.TupleExpr;
import com.sun.fortress.interpreter.nodes.TypeAlias;
import com.sun.fortress.interpreter.nodes.TypeRef;
import com.sun.fortress.interpreter.nodes.UnitDim;
import com.sun.fortress.interpreter.nodes.UnitVar;
import com.sun.fortress.interpreter.nodes.VarDecl;
import com.sun.fortress.interpreter.nodes.VarRefExpr;
import com.sun.fortress.interpreter.nodes.VoidLiteral;
import com.sun.fortress.interpreter.nodes.WhereClause;
import com.sun.fortress.interpreter.nodes.WhereExtends;
import com.sun.fortress.interpreter.useful.HasAt;
import com.sun.fortress.interpreter.useful.NI;
import com.sun.fortress.interpreter.useful.Voidoid;

/**
 * This comment is not yet true; it is a goal.
 *
 * BuildEnvironments is a multiple-pass visitor pattern.
 *
 * The first pass, applied to a node that contains things (for example, a
 * component contains top-level declarations, a trait contains method
 * declarations) it creates entries for those things in the bindInto
 * environment.  In the top-level environment, traits and objects export
 * the names and definitions for the functional methods that they contain.
 *
 * The bindings created are not complete after the first pass.
 *
 * The second pass completes the type initialization. For contained things that
 * have internal structure (e.g., a trait within a top level list) this may
 * require a recursive visit, but with a newly allocated environment running its
 * first and second passes. This includes singleton object types.
 *
 * The third pass initializes functions and methods; these may depend on types.
 * The third pass must extract functional methods from traits and objects.
 *
 * The fourth pass performs value initialization. These may depend on functions.
 * This includes singleton object values.
 *
 * The evaluation order is slightly relaxed to make the interpreter tractable;
 * value cells (and variable cells?) are initialized with thunks. (How do we
 * thunk a singleton object?)
 *
 * It may be necessary to thunk the types as well; this is not yet entirely
 * clear because the type system is so complex. Because types already contain
 * references to their defining environment, this may proceed in an ad-hoc
 * fashion with lazy memoization.
 *
 * Note that not all passes are required in all contexts; only the top level has
 * the combination of types, functions, variables, and unordered access.
 * Different initializations are assigned to different (numbered) passes so that
 * environment building in some contexts can skip passes (for example, skip the
 * type pass in any non-top-level environment).
 *
 */
public class BuildEnvironments extends NodeVisitor<Voidoid> {

    private int pass = 1;

    public void resetPass() {
        pass = 1;
    }

    public void assertPass(int p) {
        if (pass != p)
            throw new InterpreterError("Expected pass " + p + " got pass "
                    + pass);
    }

    public void secondPass() {
        assertPass(1);
        pass = 2;
        // An environment must be blessed before it can be cloned.
        bindInto.bless();
    }

    public void thirdPass() {
        assertPass(2);
        pass = 3;
    }

    public void fourthPass() {
        assertPass(3);
        pass = 4;
    }

    public void visit(CompilationUnit n) {
        acceptNode(n);
    }

    BetterEnv containing;

    BetterEnv bindInto;

    /**
     * Creates an environment builder that will inject bindings into 'within'.
     * The visit is suspended at generics (com.sun.fortress.interpreter.nodes
     * with type parameters) until they can be instantiated.
     */
    public BuildEnvironments(BetterEnv within) {
        this.containing = within;
        this.bindInto = within;
    }

    public BuildEnvironments(BetterEnv within, BetterEnv bind_into) {
        this.containing = within;
        this.bindInto = bind_into;
    }

    private BuildEnvironments(BetterEnv within, int pass) {
        this.containing = within;
        this.bindInto = within;
        this.pass = pass;
    }

    public BetterEnv getEnvironment() {
        return containing;
    }

    static Closure instantiate(FGenericFunction x) {
        return null;
    }

    static Constructor instantiate(GenericConstructor x) {
        return null;
    }

    static FTypeTrait instantiate(TypeGeneric x) {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forApi(com.sun.fortress.interpreter.nodes.Api)
     */
    @Override
    public Voidoid forApi(Api x) {
        List<? extends DefOrDecl> decls = x.getDecls();

        switch (pass) {
        case 1:
        case 2:
        case 3:
        case 4: doDefs(this, decls);break;
        }
       return null;

    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forComponent(com.sun.fortress.interpreter.nodes.Component)
     */
    @Override
    public Voidoid forComponent(Component x) {
        List<? extends DefOrDecl> defs = x.getDefs();
        switch (pass) {
        case 1: forComponent1(x); break;

        case 2:
        case 3:
        case 4: doDefs(this, defs);break;
        }
       return null;
    }

    public Voidoid forComponentDefs(Component x) {
        List<? extends DefOrDecl> defs = x.getDefs();
        doDefs(this, defs);
        return null;
    }

    public Voidoid forComponent1(Component x) {
        DottedId name = x.getName();
        // List<Import> imports = x.getImports();
        // List<Export> exports = x.getExports();
        List<? extends DefOrDecl> defs = x.getDefs();

        SComponent comp = new SComponent(BetterEnv.primitive(x), x);
        containing.putComponent(name, comp);

        forComponentDefs(x);

        return null;
    }

    private static void doDefs(BuildEnvironments inner, List<? extends DefOrDecl> defs) {
        for (DefOrDecl def : defs) {
            inner.acceptNode(def);
        }
    }

    protected void doDefs(List<? extends DefOrDecl> defs) {
        for (DefOrDecl def : defs) {
            acceptNode(def);
        }
    }

    /**
     * Put the mappings into "into", but create closures against forTraitMethods.
     *
     * @param into
     * @param forTraitMethods
     * @param defs
     * @param fields
     */
    public void doTraitMethodDefs(BetterEnv into, BetterEnv forTraitMethods,
            List<? extends DefOrDecl> defs, Set<String> fields) {
        BuildTraitEnvironment inner = new BuildTraitEnvironment(into,
                forTraitMethods, fields);

        inner.doDefs1234(defs);

    }

    public void doDefs1234(List<? extends DefOrDecl> defs) {
        doDefs(defs);
        doDefs234(defs);
    }

    public void doDefs234(List<? extends DefOrDecl> defs) {
        secondPass();
        doDefs(defs);
        thirdPass();
        doDefs(defs);
        fourthPass();
        doDefs(defs);
    }

    private void guardedPutValue(BetterEnv e, String name, FValue value,
            HasAt where) {
        guardedPutValue(e, name, value, null, where);

    }

    /**
     * Put a value, perhaps unconditionally depending on subtype's choice
     * @param e
     * @param name
     * @param value
     * @param ft
     */
    protected void putValue(BetterEnv e, String name, FValue value, FType ft) {
        e.putVariable(name, value, ft);
    }

    /**
     * Put a value, perhaps unconditionally depending on subtype's choice
     * @param e
     * @param name
     * @param value
     * @param ft
     */
    protected void putValue(BetterEnv e, String name, FValue value) {
        e.putValue(name, value);
    }

    private void guardedPutValue(BetterEnv e, String name, FValue value,
            FType ft, HasAt where) {
        try {
            if (ft != null) {
                if (!ft.typeMatch(value)) {
                    throw new ProgramError(where, e,
                            "TypeRef mismatch binding " + value + " (type "
                                    + value.type() + ") to " + name + " (type "
                                    + ft + ")");
                }
                putValue(e, name, value, ft);
            } else {
                putValue(e, name, value);
            }
        } catch (ProgramError pe) {
            pe.setWithin(e);
            pe.setWhere(where);
            throw pe;
        }
    }

    private void guardedPutType(String name, FType type, HasAt where) {
        EvalType.guardedPutType(name, type, where, containing);
    }

    protected FValue newGenericClosure(BetterEnv e, FnDefOrDecl x) {
        return new FGenericFunction(e, x);
    }



    private void forFnDecl1(FnDecl x) {
        Option<List<StaticParam>> optStaticParams = x.getStaticParams();
        String fname = x.nameAsMethod();

        if (optStaticParams.isPresent()) {
            FValue cl = newGenericClosure(containing, x);
            bindInto.putValueShadowFn(fname, cl);
            //LINKER putOrOverloadOrShadowGeneric(x, containing, name, cl);

        } else {
            // NOT GENERIC

            Simple_fcn cl = newClosure(containing, x);
            // Search for test modifier
            List<Modifier> mods = x.getMods();
            if (!mods.isEmpty()) {
                for (Iterator<Modifier> i = mods.iterator(); i.hasNext();) {
                    Modifier m = i.next();
                    if (m instanceof Modifier.Test) {
                        FortressTests.add((Closure) cl);
                        break;
                    }
                }
            }
            // TODO this isn't right if it was a test function.
            // it belongs in a different namespace if it is.
            bindInto.putValueShadowFn(fname, cl);
            //LINKER putOrOverloadOrShadow(x, containing, name, cl);

        }

    }

   private void forFnDecl2(FnDecl x) {

   }
   private void forFnDecl3(FnDecl x) {
       Option<List<StaticParam>> optStaticParams = x.getStaticParams();
       String fname = x.nameAsMethod();

       if (optStaticParams.isPresent()) {
           // GENERIC
           {
               // Why isn't this the right thing to do?
               // FGenericFunction is (currently) excluded from this treatment.
               FValue fcn = containing.getValue(fname);
               if (fcn instanceof GenericFunctionSet) {
                   GenericFunctionSet gfs = (GenericFunctionSet) fcn;
                   gfs.finishInitializing();
               }

           }

       } else {
           // NOT GENERIC
           {
               Fcn fcn = (Fcn) containing.getValue(fname);

               if (fcn instanceof Closure) {
                   // This is only loosely paired with the
                   // first pass; dealing with overloading tends to
                   // break up the 1-1 relationship between the two.
                   // However, because of the way that scopes nest,
                   // it is possible (I think) that f could be overloaded
                   // in an inner scope but not overloaded in an outer
                   // scope.
                   Closure cl = (Closure) fcn;
                   cl.finishInitializing();
               } else if (fcn instanceof OverloadedFunction) {
                   OverloadedFunction og = (OverloadedFunction) fcn;
                   og.finishInitializing();

               }
           }
       }
  }
   private void forFnDecl4(FnDecl x) {
   }

 /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forFnDef(com.sun.fortress.interpreter.nodes.FnDecl)
     */
    @Override
    public Voidoid forFnDecl(FnDecl x) {
        switch (pass) {
        case 1: forFnDecl1(x); break;
        case 2: forFnDecl2(x); break;
        case 3: forFnDecl3(x); break;
        case 4: forFnDecl4(x); break;
        }
       return null;
    }

    /**
     * @param x
     * @param e
     * @param name
     * @param cl
     */
//    public void putOrOverloadOrShadow(HasAt x, BetterEnv e, FnName name,
//            Simple_fcn cl) {
//        Fcn g = (Fcn) e.getValueNull(name.name());
//        if (g == null) {
//            putFunction(e, name, cl, x);
//
//            // This is delicate temporary code (below), and breaks the
//            // property that adding another layer of environment is an OK
//            // thing to do.
//        } else if (g.getWithin().equals(e)) {
//            // OVERLOADING
//            OverloadedFunction og;
//            if (g instanceof OverloadedFunction) {
//                og = (OverloadedFunction) g;
//                og.addOverload(cl);
//            } else if (g instanceof GenericMethodSet
//                    || g instanceof GenericMethod) {
//                throw new ProgramError(x, e,
//                        "Cannot combine generic method and nongeneric method "
//                                + name.name() + " in an overloading");
//            } else if (g instanceof GenericFunctionSet
//                    || g instanceof FGenericFunction) {
//                throw new ProgramError(x, e,
//                        "Cannot combine generic function and nongeneric function "
//                                + name.name() + " in an overloading");
//            } else {
//                og = new OverloadedFunction(name, e);
//                og.addOverload(cl);
//                og.addOverload((Simple_fcn) g);
//
//                assignFunction(e, name, og);
//            }
//        } else {
//            // SHADOWING
//            putFunction(e, name, cl, x);
//        }
//    }

//    /**
//     * @param x
//     * @param e
//     * @param name
//     * @param cl
//     */
//    private void putOrOverloadOrShadowGeneric(HasAt x, BetterEnv e,
//            FnName name, FValue cl) {
//        FValue fv = e.getValueNull(name.name());
//        if (fv != null && !(fv instanceof Fcn)) {
//            throw new ProgramError(x, e, "Generic not generic? " + name.name());
//        }
//        Fcn g = (Fcn) fv;
//        // Actually need to test for diff types of g.
//        if (g == null) {
//            putFunction(e, name, cl, x);
//        } else if (g.getWithin().equals(e)) {
//            // OVERLOADING
//            if (cl instanceof GenericMethod) {
//                GenericMethod clg = (GenericMethod) cl;
//                GenericMethodSet og;
//                if (g instanceof GenericMethodSet) {
//                    og = (GenericMethodSet) g;
//                    og.addOverload(clg);
//                } else if (g instanceof GenericMethod) {
//                    og = new GenericMethodSet(name, e);
//                    og.addOverload(clg);
//                    og.addOverload((GenericMethod) g);
//
//                    assignFunction(e, name, og);
//                } else {
//                    throw new ProgramError(x, e, "Overload of generic method "
//                            + cl + " with non-generic/method " + g);
//                }
//            } else if (cl instanceof FGenericFunction) {
//                FGenericFunction clg = (FGenericFunction) cl;
//                GenericFunctionSet og;
//                if (g instanceof GenericFunctionSet) {
//                    og = (GenericFunctionSet) g;
//                    og.addOverload(clg);
//                } else if (g instanceof FGenericFunction) {
//                    og = new GenericFunctionSet(name, e);
//                    og.addOverload(clg);
//                    og.addOverload((FGenericFunction) g);
//
//                    assignFunction(e, name, og);
//                } else {
//                    throw new ProgramError(x, e, "Overload of function method "
//                            + cl + " with non-generic/method " + g);
//                }
//            } else {
//                throw new ProgramError(x, e,
//                        "Overload of generic, but not a method/function" + cl
//                                + " with generic/method " + g);
//
//            }
//        } else {
//            // SHADOWING
//            putFunction(e, name, cl, x);
//        }
//    }

    protected Simple_fcn newClosure(BetterEnv e, Applicable x) {
        return new Closure(e, x);
    }

    /**
     * @param name
     * @param fname
     * @param f
     */
    private void putFunction(BetterEnv e, FnName name, FValue f, HasAt x) {
        String s = name.name();
        guardedPutValue(e, s, f, x);
        e.noteName(s);
    }

    private static void assignFunction(BetterEnv e, FnName name, FValue f) {
        e.putValueUnconditionally(name.name(), f);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forObjectDef(com.sun.fortress.interpreter.nodes.ObjectDecl)
     */
    @Override
    public Voidoid forObjectDecl(ObjectDecl x) {
        switch (pass) {
        case 1: forObjectDecl1(x); break;
        case 2: forObjectDecl2(x); break;
        case 3: forObjectDecl3(x); break;
        case 4: forObjectDecl4(x); break;
        }
       return null;
    }
    private void forObjectDecl1(ObjectDecl x) {
        // List<Modifier> mods;

        BetterEnv e = containing;
        Id name = x.getName();

        Option<List<StaticParam>> staticParams = x.getStaticParams();
        Option<List<Param>> params = x.getParams();

        // List<TypeRef> throws_;
        // List<WhereClause> where;
        // Contract contract;
        // List<Decl> defs = x.getDefOrDecls();
        String fname = name.getName();
        FType ft;
        ft = staticParams.isPresent() ? new FTypeGeneric(e, x)
                : new FTypeObject(fname, e, x);

        // Need to check for overloaded constructor.

        guardedPutType(fname, ft, x);

        if (params.isPresent()) {
            if (staticParams.isPresent()) {
                // A generic, not yet a constructor
                GenericConstructor gen = new GenericConstructor(e, x);
                guardedPutValue(containing, fname, gen, x);
            } else {
                // TODO need to deal with constructor overloading.

                // If parameters are present, it is really a constructor
                // BetterEnv interior = new SpineEnv(e, x);
                Constructor cl = new Constructor(containing, (FTypeObject) ft,
                        x);
                guardedPutValue(containing, fname, cl, x);
                // doDefs(interior, defs);
            }

        } else {
            if (staticParams.isPresent()) {
                // A parameterized singleton is a sort of generic value.
                NI.nyi("Generic singleton objects");
                GenericConstructor gen = new GenericConstructor(e, x);
                guardedPutValue(containing, obfuscated(fname), gen, x);

            } else {
                // It is a singleton; do not expose the constructor, do
                // visit
                // the interior environment.
                // BetterEnv interior = new SpineEnv(e, x);

                // TODO - binding into "containing", or "bindInto"?

                Constructor cl = new Constructor(containing, (FTypeObject) ft,
                        x);
                guardedPutValue(containing, obfuscated(fname), cl, x);

                String sname = name.getName();
                // Create a little expression to run the constructor.
                Expr init = new TightJuxt(x.getSpan(),
                        new VarRefExpr(x.getSpan(), obfuscated(fname)),
                        new VoidLiteral(x.getSpan()));
                FValue init_value = new LazilyEvaluatedCell(init, containing);
                putValue(bindInto, sname, init_value);

                // doDefs(interior, defs);
            }
        }

        scanForFunctionalMethodNames(x, x.getDefOrDecls(), ft, fname);

    }

    private void scanForFunctionalMethodNames(
            Node x,
            List<? extends DefOrDecl> defs,
            FType ft,
            String fname) {
        for (DefOrDecl dod : defs) {
            int spi = dod.selfParameterIndex();
            if (spi >= 0)  {
                // If it is a functional method, it is definitely a FnDefOrDecl
                FnDefOrDecl fndod = (FnDefOrDecl) dod;
                // System.err.println("Functional method " + dod);
                String fndodname = fndod.nameAsFunction();
                if (pass == 1) {
                    Simple_fcn cl = new FunctionalMethod(containing, fndod, spi);
                    // TODO test and other modifiers
                    bindInto.putValueShadowFn(fndodname, cl);
                } else if (pass == 3) {
                    Fcn fcn = (Fcn) containing.getValue(fndodname);

                    if (fcn instanceof Closure) {
                        Closure cl = (Closure) fcn;
                        cl.finishInitializing();
                    } else if (fcn instanceof OverloadedFunction) {
                        // TODO it is correct to do this here, though it won't work yet.
                        OverloadedFunction og = (OverloadedFunction) fcn;
                        og.finishInitializing();

                    }
                }
            }
        }
    }

    private void forObjectDecl2(ObjectDecl x) {

        BetterEnv e = containing;
        Id name = x.getName();

        Option<List<StaticParam>> staticParams = x.getStaticParams();
        Option<List<Param>> params = x.getParams();

        String fname = name.getName();
        FType ft;

        if (params.isPresent()) {
            if (staticParams.isPresent()) {
                // Do nothing.
            } else {
                FTypeObject fto = (FTypeObject) containing.getType(fname);
                FValue xxx = containing.getValue(fname);
                Constructor cl = (Constructor) containing.getValue(fname);
                finishObjectTrait(x, fto);
            }
        } else {
            // If there are no parameters, it is a singleton.
            // Not clear we can evaluate it yet.
            FTypeObject fto = (FTypeObject) containing.getType(fname);

            finishObjectTrait(x, fto);

        }

    }
    private void forObjectDecl3(ObjectDecl x) {

        BetterEnv e = containing;
        Id name = x.getName();

        Option<List<StaticParam>> staticParams = x.getStaticParams();
        Option<List<Param>> params = x.getParams();

        String fname = name.getName();
        FType ft = containing.getType(fname);

        if (params.isPresent()) {

            if (staticParams.isPresent()) {
                // Do nothing.
            } else {
                FTypeObject fto = (FTypeObject) ft;
                Constructor cl = (Constructor) containing.getValue(fname);
                List<Parameter> fparams = EvalType.paramsToParameters(
                        containing, params.getVal());
                cl.setParams(fparams);
                cl.finishInitializing();
            }

        } else {
            // If there are no parameters, it is a singleton.
            // TODO -  Blindly assuming a non-generic singleton.

            Constructor cl = (Constructor) containing
                    .getValue(obfuscated(fname));
            cl.setParams(Collections.<Parameter> emptyList());
            cl.finishInitializing();
         }
        scanForFunctionalMethodNames(x, x.getDefOrDecls(), ft, fname);
    }
    private void forObjectDecl4(ObjectDecl x) {

        BetterEnv e = containing;
        Id name = x.getName();

        Option<List<Param>> params = x.getParams();

        String fname = name.getName();

        if (params.isPresent()) {

        } else {
            // TODO - Blindly assuming a non-generic singleton.
            // TODO - Need to insert the name much, much, earlier; this is too late.

            String sname = name.getName();
            FValue value = bindInto.getValue(sname);

//            Constructor cl = (Constructor) containing
//                    .getValue(obfuscated(fname));
//
//            guardedPutValue(containing, fname, cl.apply(java.util.Collections
//                    .<FValue> emptyList(), x, e), x);

        }
    }


    private String obfuscated(String fname) {
        // TODO Auto-generated method stub
        return "*1_" + fname;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forVarDef(com.sun.fortress.interpreter.nodes.VarDecl)
     */
    @Override
    public Voidoid forVarDecl(VarDecl x) {
        switch (pass) {
        case 1:
            forVarDecl1(x);
            break;
        case 2:
            forVarDecl2(x);
            break;
        case 3:
            forVarDecl3(x);
            break;
        case 4:
            forVarDecl4(x);
            break;
        }
        return null;
    }

    private void forVarDecl1(VarDecl x) {
        List<LValue> lhs = x.getLhs();

        // List<Modifier> mods;
        // Id name = x.getName();
        // Option<TypeRef> type = x.getType();
        Expr init = x.getInit();
	int index = 0;

        for (LValue lv : lhs) {
            if (lv instanceof LValueBind) {
                LValueBind lvb = (LValueBind) lv;
                Option<TypeRef> type = lvb.getType();
                Id name = lvb.getName();
                String sname = name.getName();

                try {
                    /* Ignore the type, until later */
                    if (lvb.getMutable()) {
                        bindInto.putVariablePlaceholder(sname);
                    } else {
                        FValue init_val;
                        if (init instanceof TupleExpr) {
                            init_val = new LazilyEvaluatedCell(
                                      ((TupleExpr)init).getExprs().get(index++),
                                      containing);
                        } else {
                            init_val = new LazilyEvaluatedCell(init, containing);
                        }
                        putValue(bindInto, sname, init_val);
                    }
                 } catch (ProgramError pe) {
                    pe.setWithin(bindInto);
                    pe.setWhere(x);
                    throw pe;
                }

            } else {
                throw new InterpreterError(x,
                        "Don't support arbitary LHS in Var decl yet");
            }
        }
    }

    private void forVarDecl2(VarDecl x) {

    }

    private void forVarDecl3(VarDecl x) {


    }

    private void forVarDecl4(VarDecl x) {

        List<LValue> lhs = x.getLhs();

        // List<Modifier> mods;
        // Id name = x.getName();
        // Option<TypeRef> type = x.getType();
        Expr init = x.getInit();
	int index = 0;

        for (LValue lv : lhs) {
            if (lv instanceof LValueBind) {
                LValueBind lvb = (LValueBind) lv;

                Option<TypeRef> type = lvb.getType();
                Id name = lvb.getName();
                String sname = name.getName();

                FType ft = type.isPresent() ?
                        (new EvalType(containing)).evalType(type.getVal())
                                : null;

                if (lvb.getMutable()) {
                    Expr rhs;
                    if (init instanceof TupleExpr) {
                        rhs = ((TupleExpr)init).getExprs().get(index++);
                    } else {
                        rhs = init;
                    }
                    FValue value = (new Evaluator(containing)).eval(rhs);

                    // TODO When new environment are created, need to insert
                    // into containing AND bindInto

                    if (ft != null) {
                        if (!ft.typeMatch(value)) {
                            throw new ProgramError(x, bindInto,
                                    "TypeRef mismatch binding " + value + " (type "
                                            + value.type() + ") to " + name + " (type "
                                            + ft + ")");
                        }
                    } else {
                        ft = FTypeDynamic.T;
                    }
                    /* Finally, can finish this initialiation. */
                    bindInto.storeType(x, sname, ft);
                    bindInto.assignValue(x, sname, value);
                } else {
                    // Force evaluation, snap the link, check the type!
                    FValue value = bindInto.getValue(sname);
                    if (ft != null) {
                        if (!ft.typeMatch(value)) {
                            throw new ProgramError(x, bindInto,
                                    "TypeRef mismatch binding " + value + " (type "
                                            + value.type() + ") to " + name + " (type "
                                            + ft + ")");
                        }
                    }
                }
            } else {
                throw new InterpreterError(x,
                        "Don't support arbitary LHS in Var decl yet");
            }
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forTraitDecl(com.sun.fortress.interpreter.nodes.AbsTraitDecl)
     */
    @Override
    public Voidoid forAbsTraitDecl(AbsTraitDecl x) {
        switch (pass) {
        case 1: forAbsTraitDecl1(x); break;
        case 2: forAbsTraitDecl2(x); break;
        case 3: forAbsTraitDecl3(x); break;
        case 4: forAbsTraitDecl4(x); break;
        }
       return null;
    }
    private void forAbsTraitDecl1(AbsTraitDecl x) {
        // TODO Auto-generated method stub
        Option<List<StaticParam>> staticParams = x.getStaticParams();
        // List<Modifier> mods;
        Id name = x.getName();
        // List<TypeRef> excludes;
        // Option<List<TypeRef>> bounds;
        // List<WhereClause> where;
        FType ft;

        String fname = name.getName();

        if (staticParams.isPresent()) {

                FTypeGeneric ftg = new FTypeGeneric(containing, x);
                guardedPutType(name.getName(), ftg, x);
                scanForFunctionalMethodNames(x, x.getFns(), ftg, fname);
           ft = ftg;
        } else {

                BetterEnv interior = containing; // new BetterEnv(containing, x);
                FTypeTrait ftt = new FTypeTrait(name.getName(), interior, x);
                guardedPutType(name.getName(), ftt, x);
                scanForFunctionalMethodNames(x, x.getFns(), ftt, fname);
           ft = ftt;
        }

        scanForFunctionalMethodNames(x, x.getFns(), ft, fname);
    }
    private void forAbsTraitDecl2(AbsTraitDecl x) {
        // TODO Auto-generated method stub
        Option<List<StaticParam>> staticParams = x.getStaticParams();
        // List<Modifier> mods;
        Id name = x.getName();
        // List<TypeRef> excludes;
        // Option<List<TypeRef>> bounds;
        // List<WhereClause> where;

        if (staticParams.isPresent()) {

        } else {
           {
                FTypeTrait ftt = (FTypeTrait) containing
                        .getType(name.getName());
                BetterEnv interior = ftt.getEnv();
                finishTrait(x, ftt, interior);

            }
        }
    }
    private void forAbsTraitDecl3(AbsTraitDecl x) {
        Id name = x.getName();
        FType ft =  containing.getType(name.getName());
        String fname = name.getName();
        scanForFunctionalMethodNames(x, x.getFns(), ft, fname);
    }

    private void forAbsTraitDecl4(AbsTraitDecl x) {
    }


    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forTraitDef(com.sun.fortress.interpreter.nodes.TraitDecl)
     */
    @Override
    public Voidoid forTraitDecl(TraitDecl x) {
        switch (pass) {
        case 1: forTraitDecl1(x); break;
        case 2: forTraitDecl2(x); break;
        case 3: forTraitDecl3(x); break;
        case 4: forTraitDecl4(x); break;
        }
       return null;
    }
    private void forTraitDecl1(TraitDecl x) {
        // TODO Auto-generated method stub
        Option<List<StaticParam>> staticParams = x.getStaticParams();
        // List<Modifier> mods;
        Id name = x.getName();
        // List<TypeRef> excludes;
        // Option<List<TypeRef>> bounds;
        // List<WhereClause> where;
        FType ft;

        String fname = name.getName();

        if (staticParams.isPresent()) {

                FTypeGeneric ftg = new FTypeGeneric(containing, x);
                guardedPutType(name.getName(), ftg, x);
                scanForFunctionalMethodNames(x, x.getFns(), ftg, fname);
           ft = ftg;
        } else {

                BetterEnv interior = containing; // new BetterEnv(containing, x);
                FTypeTrait ftt = new FTypeTrait(name.getName(), interior, x);
                guardedPutType(name.getName(), ftt, x);
                scanForFunctionalMethodNames(x, x.getFns(), ftt, fname);
           ft = ftt;
        }

        scanForFunctionalMethodNames(x, x.getFns(), ft, fname);
    }
    private void forTraitDecl2(TraitDecl x) {
        // TODO Auto-generated method stub
        Option<List<StaticParam>> staticParams = x.getStaticParams();
        // List<Modifier> mods;
        Id name = x.getName();
        // List<TypeRef> excludes;
        // Option<List<TypeRef>> bounds;
        // List<WhereClause> where;

        if (staticParams.isPresent()) {

        } else {
           {
                FTypeTrait ftt = (FTypeTrait) containing
                        .getType(name.getName());
                BetterEnv interior = ftt.getEnv();
                finishTrait(x, ftt, interior);

            }
        }
    }
    private void forTraitDecl3(TraitDecl x) {
        Id name = x.getName();
        FType ft =  containing.getType(name.getName());
        String fname = name.getName();
        scanForFunctionalMethodNames(x, x.getFns(), ft, fname);
    }

    private void forTraitDecl4(TraitDecl x) {
    }

    /**
     * @param x
     * @param ftt
     * @param interior
     */
    public void finishTrait(TraitDefOrDecl x, FTypeTrait ftt, BetterEnv interior) {
        Option<List<TypeRef>> extends_ = x.getExtends_();
        interior = new BetterEnv(interior, x);

        EvalType et = processWhereClauses(x.getWhere(), interior);

        List<FType> extl = et.getFTypeListFromOptionList(extends_);
        List<FType> excl = et.getFTypeListFromList(x.getExcludes());
        ftt.setExtendsAndExcludes(extl, excl, interior);
        List<? extends DefOrDecl> fns = x.getFns();

        doTraitMethodDefs(ftt.getMembers(), ftt.getMethodExecutionEnv(), fns,
                null); /* NOTICE THE DIFFERENT ENVIRONMENT! */

    }


    /**
     * Processes a list of where clauses,
     * both using and augmenting the environment
     * "interior" passed in as a parameter.
     *
     * @param wheres
     * @param interior
     * @return
     */
    private static EvalType processWhereClauses(List<WhereClause> wheres,
            BetterEnv interior) {

        if (wheres != null) {
            for (WhereClause w : wheres) {
                if (w instanceof WhereExtends) {
                    WhereExtends we = (WhereExtends) w;
                    Id name = we.getName();
                    String string_name = name.getName();
                    // List<TypeRef> types = we.getSupers();
                    FType ft = interior.getTypeNull(string_name);
                    if (ft == null) {
                        ft = new SymbolicWhereType(string_name, interior);
                        interior.putType(string_name, ft);
                    }
                } else if (w instanceof TypeAlias) {
                    /*
                     * This is problematic; the type bound to the alias cannot
                     * quite be evaluated yet, but it might be necessary for
                     * some of the other clauses. What we need is a little
                     * topological order.
                     */
                } else {
                    NI.nyi("Where clause " + w);
                }
            }
        }

        EvalType et = new EvalType(interior);

        if (wheres != null) {
            for (WhereClause w : wheres) {
                if (w instanceof WhereExtends) {
                    WhereExtends we = (WhereExtends) w;
                    Id name = we.getName();
                    String string_name = name.getName();
                    List<TypeRef> types = we.getSupers();
                    FType ft = interior.getTypeNull(string_name);
                    for (TypeRef t : types) {
                        FType st = et.evalType(t); // t.accept(et);
                        if (ft instanceof SymbolicType) {
                            // Treat as "extends".
                            ((SymbolicType) ft).addExtend(st);
                        } else if (st instanceof SymbolicWhereType) {
                            // Record subtype ft of st.
                            SymbolicWhereType swt = (SymbolicWhereType) st;
                            swt.addSubtype(ft);
                        } else {
                            // Check that constraint holds.
                            NI.nyi("need to verify constraint stated in where clause");
                        }
                    }
                } else if (w instanceof TypeAlias) {
                    /*
                     * This is problematic; the type bound to the alias cannot
                     * quite be evaluated yet, but it might be necessary for
                     * some of the other clauses. What we need is a little
                     * topological order.
                     */
                    // For now, assume that the order in the where clause is
                    // topological.
                    TypeAlias ta = (TypeAlias) w;
                    Id name = ta.getName();
                    TypeRef type = ta.getType();
                    interior.putType(name.getName(), et.evalType(type));
                } else {
                    NI.nyi("Where clause " + w);
                }
            }
        }
        return et;
    }

    public void finishObjectTrait(ObjectDefOrDecl x, FTypeObject ftt) {
        Option<List<TypeRef>> extends_ = x.getTraits();
        finishObjectTrait(extends_, null, x.getWhere(), ftt, containing, x);
    }

    public void finishObjectTrait(ObjectExpr x, FTypeObject ftt) {
        Option<List<TypeRef>> extends_ = x.getTraits();
        // ObjectExpr has no excludes clause.
        finishObjectTrait(extends_, null, null, ftt, containing, x);
    }

    static public void finishObjectTrait(Option<List<TypeRef>> extends_,
            List<TypeRef> excludes, List<WhereClause> wheres, FTypeObject ftt,
            BetterEnv interior, HasAt x) {
        interior = new BetterEnv(interior, x);
        EvalType et = processWhereClauses(wheres, interior);
        ftt.setExtendsAndExcludes(et.getFTypeListFromOptionList(extends_), et
                .getFTypeListFromList(excludes), interior);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forTypeAlias(com.sun.fortress.interpreter.nodes.TypeAlias)
     */
    @Override
    public Voidoid forTypeAlias(TypeAlias x) {
        // Id name;
        // List<Id> params;
        // TypeRef type;
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forDimension(com.sun.fortress.interpreter.nodes.Dimension)
     */
    @Override
    public Voidoid forDimension(Dimension x) {
        // TODO Auto-generated method stub
        // Id id = x.getId();
        // Option<TypeRef> derived;
        // Option<TypeRef> default_;

        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forUnitDim(com.sun.fortress.interpreter.nodes.UnitDim)
     */
    @Override
    public Voidoid forUnitDim(UnitDim x) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forUnitVar(com.sun.fortress.interpreter.nodes.UnitVar)
     */
    @Override
    public Voidoid forUnitVar(UnitVar x) {
        // List<Id> names;
        // Option<TypeRef> type;
        // Option<Expr> def;
        // boolean si;

        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forImportApi(com.sun.fortress.interpreter.nodes.ImportApi)
     */
    @Override
    public Voidoid forImportApi(ImportApi x) {
        // TODO Auto-generated method stub
        return null;
    }



    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forImportNames(com.sun.fortress.interpreter.nodes.ImportNames)
     */
    @Override
    public Voidoid forImportNames(ImportNames x) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forImportStar(com.sun.fortress.interpreter.nodes.ImportStar)
     */
    @Override
    public Voidoid forImportStar(ImportStar x) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forVarDecl(com.sun.fortress.interpreter.nodes.AbsVarDecl)
     */
    @Override
    public Voidoid forAbsVarDecl(AbsVarDecl x) {
        // List<Modifier> mods;
        // Id name;
        // TypeRef type;
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forFnDecl(com.sun.fortress.interpreter.nodes.AbsFnDecl)
     */
    @Override
    public Voidoid forAbsFnDecl(AbsFnDecl x) {
        switch (pass) {
        case 1: forAbsFnDecl1(x); break;
        case 2: forAbsFnDecl2(x); break;
        case 3: forAbsFnDecl3(x); break;
        case 4: forAbsFnDecl4(x); break;
        }
       return null;
    }
    private void forAbsFnDecl1(AbsFnDecl x) {


        Option<List<StaticParam>> optStaticParams = x.getStaticParams();
        String fname = x.nameAsMethod();

        if (optStaticParams.isPresent()) {
            // GENERIC

                // TODO same treatment as regular functions.
                FValue cl = newGenericClosure(containing, x);
                // LINKER putOrOverloadOrShadowGeneric(x, containing, name, cl);
                bindInto.putValueShadowFn(fname, cl);


        } else {
            // NOT GENERIC

                Simple_fcn cl = newClosure(containing, x);
                // LINKER putOrOverloadOrShadow(x, containing, name, cl);
                bindInto.putValueShadowFn(fname, cl);

        }

    }
    private void forAbsFnDecl2(AbsFnDecl x) {
    }
    private void forAbsFnDecl3(AbsFnDecl x) {


        Option<List<StaticParam>> optStaticParams = x.getStaticParams();
        String fname = x.nameAsMethod();

        if (optStaticParams.isPresent()) {
            // GENERIC


        } else {
            // NOT GENERIC

                Fcn fcn = (Fcn) containing.getValue(fname);

                if (fcn instanceof Closure) {
                    // This is only loosely paired with the
                    // first pass; dealing with overloading tends to
                    // break up the 1-1 relationship between the two.
                    // However, because of the way that scopes nest,
                    // it is possible (I think) that f could be overloaded
                    // in an inner scope but not overloaded in an outer
                    // scope.
                    Closure cl = (Closure) fcn;
                    cl.finishInitializing();
                } else if (fcn instanceof OverloadedFunction) {
                    OverloadedFunction og = (OverloadedFunction) fcn;
                    og.finishInitializing();

                }

        }

    }
    private void forAbsFnDecl4(AbsFnDecl x) {
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forAbsObjectDecl(com.sun.fortress.interpreter.nodes.AbsObjectDecl)
     */
    @Override
    public Voidoid forAbsObjectDecl(AbsObjectDecl x) {
        switch (pass) {
        case 1: forAbsObjectDecl1(x); break;
        case 2: forAbsObjectDecl2(x); break;
        case 3: forAbsObjectDecl3(x); break;
        case 4: forAbsObjectDecl4(x); break;
        }
       return null;
    }
    private void forAbsObjectDecl1(AbsObjectDecl x) {
        // List<Modifier> mods;

        BetterEnv e = containing;
        Id name = x.getName();

        Option<List<StaticParam>> staticParams = x.getStaticParams();
        Option<List<Param>> params = x.getParams();

        // List<TypeRef> throws_;
        // List<WhereClause> where;
        // Contract contract;
        // List<Decl> defs = x.getDefOrDecls();
        String fname = name.getName();
        FType ft;
        ft = staticParams.isPresent() ? new FTypeGeneric(e, x)
                : new FTypeObject(fname, e, x);

        // Need to check for overloaded constructor.

        guardedPutType(fname, ft, x);

        if (params.isPresent()) {
            if (staticParams.isPresent()) {
                // A generic, not yet a constructor
                GenericConstructor gen = new GenericConstructor(e, x);
                guardedPutValue(containing, fname, gen, x);
            } else {
                // TODO need to deal with constructor overloading.

                // If parameters are present, it is really a constructor
                // BetterEnv interior = new SpineEnv(e, x);
                Constructor cl = new Constructor(containing, (FTypeObject) ft,
                        x);
                guardedPutValue(containing, fname, cl, x);
                // doDefs(interior, defs);
            }

        } else {
            if (staticParams.isPresent()) {
                // A parameterized singleton is a sort of generic value.
                NI.nyi("Generic singleton objects");
                GenericConstructor gen = new GenericConstructor(e, x);
                guardedPutValue(containing, obfuscated(fname), gen, x);

            } else {
                // It is a singleton; do not expose the constructor, do
                // visit
                // the interior environment.
                // BetterEnv interior = new SpineEnv(e, x);

                // TODO - binding into "containing", or "bindInto"?

                Constructor cl = new Constructor(containing, (FTypeObject) ft,
                        x);
                guardedPutValue(containing, obfuscated(fname), cl, x);

                // doDefs(interior, defs);
            }
        }

        scanForFunctionalMethodNames(x, x.getDefOrDecls(), ft, fname);

    }

    private void forAbsObjectDecl2(AbsObjectDecl x) {

        BetterEnv e = containing;
        Id name = x.getName();

        Option<List<StaticParam>> staticParams = x.getStaticParams();
        Option<List<Param>> params = x.getParams();

        String fname = name.getName();
        FType ft;

        if (params.isPresent()) {
            if (staticParams.isPresent()) {
                // Do nothing.
            } else {
                FTypeObject fto = (FTypeObject) containing.getType(fname);
                finishObjectTrait(x, fto);
            }
        } else {
            // If there are no parameters, it is a singleton.
            // Not clear we can evaluate it yet.
            FTypeObject fto = (FTypeObject) containing.getType(fname);

            finishObjectTrait(x, fto);

        }

    }
    private void forAbsObjectDecl3(AbsObjectDecl x) {
        Id name = x.getName();
        String fname = name.getName();
        FType ft = containing.getType(fname);
        scanForFunctionalMethodNames(x, x.getDefOrDecls(), ft, fname);
    }
    private void forAbsObjectDecl4(AbsObjectDecl x) {
    }

    public BetterEnv getBindingEnv() {
        return bindInto;
    }

}
