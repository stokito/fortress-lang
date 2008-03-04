/*******************************************************************************
    Copyright 2008 Sun Microsystems, Inc.,
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
import edu.rice.cs.plt.tuple.Option;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.env.FortressTests;
import com.sun.fortress.interpreter.env.LazilyEvaluatedCell;
import com.sun.fortress.interpreter.evaluator.scopes.SComponent;
import com.sun.fortress.interpreter.evaluator.types.FTraitOrObject;
import com.sun.fortress.interpreter.evaluator.types.FTraitOrObjectOrGeneric;
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
import com.sun.fortress.interpreter.evaluator.values.FVoid;
import com.sun.fortress.interpreter.evaluator.values.Fcn;
import com.sun.fortress.interpreter.evaluator.values.FunctionalMethod;
import com.sun.fortress.interpreter.evaluator.values.GenericConstructor;
import com.sun.fortress.interpreter.evaluator.values.GenericMethod;
import com.sun.fortress.interpreter.evaluator.values.GenericSingleton;
import com.sun.fortress.interpreter.evaluator.values.HasFinishInitializing;
import com.sun.fortress.interpreter.evaluator.values.OverloadedFunction;
import com.sun.fortress.interpreter.evaluator.values.Parameter;
import com.sun.fortress.interpreter.evaluator.values.Simple_fcn;
import com.sun.fortress.nodes.AbsFnDecl;
import com.sun.fortress.nodes.AbsObjectDecl;
import com.sun.fortress.nodes.AbsTraitDecl;
import com.sun.fortress.nodes.AbsVarDecl;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.Applicable;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes.Generic;
import com.sun.fortress.nodes.GenericDeclWithParams;
import com.sun.fortress.nodes.AbstractNode;
import com.sun.fortress.nodes.GrammarDecl;
import com.sun.fortress.nodes.GrammarDef;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeAbstractVisitor;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes.AbsDeclOrDecl;
import com.sun.fortress.nodes.Decl;
import com.sun.fortress.nodes.DimUnitDecl;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.FnDef;
import com.sun.fortress.nodes.FnAbsDeclOrDecl;
import com.sun.fortress.nodes.SimpleName;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.ImportApi;
import com.sun.fortress.nodes.ImportNames;
import com.sun.fortress.nodes.ImportStar;
import com.sun.fortress.nodes.LValueBind;
import com.sun.fortress.nodes.Modifier;
import com.sun.fortress.nodes.ModifierTest;
import com.sun.fortress.nodes.ObjectAbsDeclOrDecl;
import com.sun.fortress.nodes.ObjectDecl;
import com.sun.fortress.nodes._RewriteObjectExpr;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.TightJuxt;
import com.sun.fortress.nodes.TraitAbsDeclOrDecl;
import com.sun.fortress.nodes.TraitDecl;
import com.sun.fortress.nodes.ArgExpr;
import com.sun.fortress.nodes.TupleExpr;
import com.sun.fortress.nodes.TypeAlias;
import com.sun.fortress.nodes.TraitType;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.DimArg;
import com.sun.fortress.nodes.VarDecl;
import com.sun.fortress.nodes.VarRef;
import com.sun.fortress.nodes.VoidLiteralExpr;
import com.sun.fortress.nodes.WhereClause;
import com.sun.fortress.nodes.WhereConstraint;
import com.sun.fortress.nodes.WhereExtends;
import com.sun.fortress.nodes_util.ExprFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.BATree;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.NI;
import com.sun.fortress.useful.StringComparer;
import com.sun.fortress.useful.Voidoid;

import static com.sun.fortress.interpreter.evaluator.ProgramError.errorMsg;
import static com.sun.fortress.interpreter.evaluator.ProgramError.error;
import static com.sun.fortress.interpreter.evaluator.InterpreterBug.bug;

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
public class BuildEnvironments extends NodeAbstractVisitor<Voidoid> {

     private int pass = 1;

    public void resetPass() {
        pass = 1;
    }

    public void assertPass(int p) {
        if (pass != p)
            bug("Expected pass " + p + " got pass " + pass);
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
        n.accept(this);
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

    protected BuildEnvironments(BetterEnv within, BetterEnv bind_into) {
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
        List<? extends AbsDeclOrDecl> decls = x.getDecls();

        switch (pass) {
        case 1:
        case 2:
        case 3:
        case 4: doDefs(this, decls);break;
        }
        return null;

    }

    class ForceTraitFinish extends NodeAbstractVisitor<Voidoid> {

        /**
         * Make the default behavior return null, no throw an exception.
         */
        public Voidoid defaultCase(Node x) {
            return null;
        }

        /* (non-Javadoc)
         * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forAbsTraitDecl(com.sun.fortress.interpreter.nodes.AbsTraitDecl)
         */
        @Override
        public Voidoid forAbsTraitDecl(AbsTraitDecl x) {
            List<StaticParam> staticParams = x.getStaticParams();
            Id name = x.getName();

            if (staticParams.isEmpty()) {
                    FTypeTrait ftt =
                        (FTypeTrait) containing.getType(NodeUtil.nameString(name));
                    BetterEnv interior = ftt.getEnv();
                    ftt.getMembers();
            }
            return null;
        }

        /* (non-Javadoc)
         * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forTraitDecl(com.sun.fortress.interpreter.nodes.TraitDecl)
         */
        @Override
        public Voidoid forTraitDecl(TraitDecl x) {
            List<StaticParam> staticParams = x.getStaticParams();
            Id name = x.getName();

            if (staticParams.isEmpty()) {
                    FTypeTrait ftt = (FTypeTrait) containing
                            .getType(NodeUtil.nameString(name));
                    BetterEnv interior = ftt.getEnv();
                    ftt.getMembers();
            }
            return null;
        }

        void visit(AbsDeclOrDecl def) {
            def.accept(this);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forComponent(com.sun.fortress.interpreter.nodes.Component)
     */
    @Override
    public Voidoid forComponent(Component x) {
        List<? extends AbsDeclOrDecl> defs = x.getDecls();
        switch (pass) {
        case 1: forComponent1(x); break;

        case 2: doDefs(this, defs); {
            ForceTraitFinish v = new ForceTraitFinish() ;
            for (AbsDeclOrDecl def : defs) {
                v.visit(def);
            }
        }
        break;
        case 3: doDefs(this, defs);break;
        case 4: doDefs(this, defs);break;
        }
        return null;
    }

    public Voidoid forComponentDefs(Component x) {
        List<? extends AbsDeclOrDecl> defs = x.getDecls();
        doDefs(this, defs);
        return null;
    }

    public Voidoid forComponent1(Component x) {
        APIName name = x.getName();
        // List<Import> imports = x.getImports();
        // List<Export> exports = x.getExports();
        List<? extends AbsDeclOrDecl> defs = x.getDecls();

        SComponent comp = new SComponent(BetterEnv.primitive(x), x);
        containing.putComponent(name, comp);

        forComponentDefs(x);

        return null;
    }

    private static void doDefs(BuildEnvironments inner, List<? extends AbsDeclOrDecl> defs) {
        for (AbsDeclOrDecl def : defs) {
            def.accept(inner);
        }
    }

    protected void doDefs(List<? extends AbsDeclOrDecl> defs) {
        for (AbsDeclOrDecl def : defs) {
            def.accept(this);
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
    private void doTraitMethodDefs(FTypeTrait ftt, Set<String> fields) {
        BetterEnv into = ftt.getMembers();
        BetterEnv forTraitMethods = ftt.getMethodExecutionEnv();
        List<? extends AbsDeclOrDecl> defs = ftt.getASTmembers();

        BuildTraitEnvironment inner = new BuildTraitEnvironment(into,
                forTraitMethods, fields);

        inner.doDefs1234(defs);

    }

    public void doDefs1234(List<? extends AbsDeclOrDecl> defs) {
        doDefs(defs);
        doDefs234(defs);
    }

    public void doDefs234(List<? extends AbsDeclOrDecl> defs) {
        secondPass();
        doDefs(defs);
        thirdPass();
        doDefs(defs);
        fourthPass();
        doDefs(defs);
    }

    protected void guardedPutValue(BetterEnv e, String name, FValue value,
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
     */
    protected void putValue(BetterEnv e, String name, FValue value) {
        e.putValue(name, value);
    }

    protected void guardedPutValue(BetterEnv e, String name, FValue value,
            FType ft, HasAt where) {
        try {
            if (ft != null) {
                if (!ft.typeMatch(value)) {
                    error(where, e,
                            errorMsg("Type mismatch binding ", value, " (type ",
                                     value.type(), ") to ", name, " (type ",
                                     ft, ")"));
                }
                putValue(e, name, value, ft);
            } else {
                putValue(e, name, value);
            }
        } catch (FortressError pe) {
            throw pe.setContext(where,e);
        }
    }

    protected void guardedPutType(String name, FType type, HasAt where) {
        EvalType.guardedPutType(name, type, where, containing);
    }

    protected FValue newGenericClosure(BetterEnv e, FnAbsDeclOrDecl x) {
        return new FGenericFunction(e, x);
    }



    private void forFnDef1(FnDef x) {
        List<StaticParam> optStaticParams = x.getStaticParams();
        String fname = NodeUtil.nameAsMethod(x);

        FValue cl;

        if (!optStaticParams.isEmpty()) {
             cl = newGenericClosure(containing, x);
        } else {
            // NOT GENERIC
            cl = newClosure(containing, x);

            // Search for test modifier -- can't we have a generic test modifier?
            List<Modifier> mods = x.getMods();
            if (!mods.isEmpty()) {
                for (Iterator<Modifier> i = mods.iterator(); i.hasNext();) {
                    Modifier m = i.next();
                    if (m instanceof ModifierTest) {
                        FortressTests.add((Closure) cl);
                        break;
                    }
                }
            }
        }
        // TODO this isn't right if it was a test function.
        // it belongs in a different namespace if it is.
        bindInto.putValueShadowFn(fname, cl);
        //LINKER putOrOverloadOrShadowGeneric(x, containing, name, cl);

    }

   private void forFnDef2(FnDef x) {

   }
   // Overridden in BuildTraitEnvironment
   protected void forFnDef3(FnDef x) {
       List<StaticParam> optStaticParams = x.getStaticParams();
       String fname = NodeUtil.nameAsMethod(x);

       if (!optStaticParams.isEmpty()) {
           // GENERIC
           {
               // Why isn't this the right thing to do?
               // FGenericFunction is (currently) excluded from this treatment.
               FValue fcn = containing.getValue(fname);

               if (fcn instanceof OverloadedFunction) {
                   OverloadedFunction og = (OverloadedFunction) fcn;
                   og.finishInitializing();

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
   private void forFnDef4(FnDef x) {
   }

 /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forFnDef(com.sun.fortress.interpreter.nodes.FnDef)
     */
    @Override
    public Voidoid forFnDef(FnDef x) {
        switch (pass) {
        case 1: forFnDef1(x); break;
        case 2: forFnDef2(x); break;
        case 3: forFnDef3(x); break;
        case 4: forFnDef4(x); break;
        }
       return null;
    }

//    public void putOrOverloadOrShadow(HasAt x, BetterEnv e, SimpleName name,
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
//                error(x, e,
//                        "Cannot combine generic method and nongeneric method "
//                                + name.name() + " in an overloading");
//            } else if (g instanceof GenericFunctionSet
//                    || g instanceof FGenericFunction) {
//                error(x, e,
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
//            SimpleName name, FValue cl) {
//        FValue fv = e.getValueNull(name.name());
//        if (fv != null && !(fv instanceof Fcn)) {
//            error(x, e, "Generic not generic? " + name.name());
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
//                    error(x, e, "Overload of generic method "
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
//                    error(x, e, "Overload of function method "
//                            + cl + " with non-generic/method " + g);
//                }
//            } else {
//                error(x, e,
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

    private void putFunction(BetterEnv e, SimpleName name, FValue f, HasAt x) {
        String s = NodeUtil.nameString(name);
        guardedPutValue(e, s, f, x);
        e.noteName(s);
    }

    private static void assignFunction(BetterEnv e, SimpleName name, FValue f) {
        e.putValueUnconditionally(NodeUtil.nameString(name), f);
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
    protected void forObjectDecl1(ObjectDecl x) {
        // List<Modifier> mods;

        BetterEnv e = containing;
        Id name = x.getName();

        List<StaticParam> staticParams = x.getStaticParams();
        Option<List<Param>> params = x.getParams();

        // List<Type> throws_;
        // List<WhereClause> where;
        // Contract contract;
        // List<Decl> defs = x.getDecls();
        String fname = NodeUtil.nameString(name);
        FTraitOrObjectOrGeneric ft;
        ft = staticParams.isEmpty() ?
                  new FTypeObject(fname, e, x, params, x.getDecls(), x)
                : new FTypeGeneric(e, x, x.getDecls(), x);

        // Need to check for overloaded constructor.

        guardedPutType(fname, ft, x);

        if (params.isSome()) {
            if (!staticParams.isEmpty()) {
                // A generic, not yet a constructor
                GenericConstructor gen = new GenericConstructor(e, x, name);
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
            if (!staticParams.isEmpty()) {
                // A parameterized singleton is a sort of generic value.
                // bug(x,"Generic singleton objects not yet implemented");
                makeGenericSingleton(x, e, name, fname, ft);

            } else {
                // It is a singleton; do not expose the constructor, do
                // visit the interior environment.
                // BetterEnv interior = new SpineEnv(e, x);

                // TODO - binding into "containing", or "bindInto"?

                Constructor cl = new Constructor(containing, (FTypeObject) ft,
                        x);
                guardedPutValue(containing, obfuscatedSingletonConstructorName(fname, x), cl, x);

                // Create a little expression to run the constructor.
                Expr init = ExprFactory.makeTightJuxt(x.getSpan(),
                      ExprFactory.makeVarRef(x.getSpan(), obfuscatedSingletonConstructorName(fname, x)),
                      ExprFactory.makeVoidLiteralExpr(x.getSpan()));
                FValue init_value = new LazilyEvaluatedCell(init, containing);
                putValue(bindInto, fname, init_value);

                // doDefs(interior, defs);
            }
        }

        scanForFunctionalMethodNames(ft, x.getDecls());

    }

    private void makeGenericSingleton(ObjectAbsDeclOrDecl x, BetterEnv e, Id name,
            String fname, FTraitOrObjectOrGeneric ft) {
        GenericConstructor gen = new GenericConstructor(e, x, name);
        guardedPutValue(containing, obfuscatedConstructorName(fname), gen, x);
        guardedPutValue(containing, fname, new GenericSingleton(x,ft, gen), x);
    }

    public void scanForFunctionalMethodNames(
            FTraitOrObjectOrGeneric x,
            List<? extends AbsDeclOrDecl> defs) {
        scanForFunctionalMethodNames(x, defs, false);
    }

    public void scanForFunctionalMethodNames(FTraitOrObjectOrGeneric x,
            List<? extends AbsDeclOrDecl> defs, boolean bogus) {
        // This is probably going away.
        BetterEnv topLevel = containing;
        if (pass == 1) {
            x.initializeFunctionalMethods();
        } else if (pass == 3) {
            x.finishFunctionalMethods();
        }

    }


     private void forObjectDecl2(ObjectDecl x) {

        BetterEnv e = containing;
        Id name = x.getName();

        List<StaticParam> staticParams = x.getStaticParams();
        Option<List<Param>> params = x.getParams();

        String fname = NodeUtil.nameString(name);
        FType ft;

        if (params.isSome()) {
            if (!staticParams.isEmpty()) {
                // Do nothing.
            } else {
                FTypeObject fto = (FTypeObject) containing.getType(fname);
                FValue xxx = containing.getValue(fname);
                //Constructor cl = (Constructor) containing.getValue(fname);
                finishObjectTrait(x, fto);
            }
        } else {
            // If there are no parameters, it is a singleton.
            // Not clear we can evaluate it yet.
            if (!staticParams.isEmpty()) {
                // Do nothing.
            } else {
                FTypeObject fto = (FTypeObject) containing.getType(fname);

                finishObjectTrait(x, fto);
            }

        }

    }
    private void forObjectDecl3(ObjectDecl x) {
        BetterEnv e = containing;
        Id name = x.getName();

        List<StaticParam> staticParams = x.getStaticParams();
        Option<List<Param>> params = x.getParams();

        String fname = NodeUtil.nameString(name);
        FTraitOrObjectOrGeneric ft = (FTraitOrObjectOrGeneric) containing.getType(fname);

        if (params.isSome()) {

            if (!staticParams.isEmpty()) {
                // Do nothing.
            } else {
                FTypeObject fto = (FTypeObject) ft;
                HasFinishInitializing cl = (HasFinishInitializing) containing.getValue(fname);
//                List<Parameter> fparams = EvalType.paramsToParameters(
//                        containing, Option.unwrap(params));
//                cl.setParams(fparams);
                cl.finishInitializing();
            }

        } else {
            // If there are no parameters, it is a singleton.

            if (!staticParams.isEmpty()) {
                // do nothing for generic singleton or its constructor
            } else {
            Constructor cl = (Constructor) containing
                    .getValue(obfuscatedSingletonConstructorName(fname, x));
            //  cl.setParams(Collections.<Parameter> emptyList());
                cl.finishInitializing();
            }
         }
        scanForFunctionalMethodNames(ft, x.getDecls());
    }
    private void forObjectDecl4(ObjectDecl x) {

        BetterEnv e = containing;
        Id name = x.getName();

        Option<List<Param>> params = x.getParams();

        String fname = NodeUtil.nameString(name);

        if (params.isSome()) {

        } else {
            // TODO - Blindly assuming a non-generic singleton.
            // TODO - Need to insert the name much, much, earlier; this is too late.

            FValue value = bindInto.getValue(fname);

//            Constructor cl = (Constructor) containing
//                    .getValue(obfuscated(fname));
//
//            guardedPutValue(containing, fname, cl.apply(java.util.Collections
//                    .<FValue> emptyList(), x, e), x);

        }
    }


    protected String obfuscatedSingletonConstructorName(String fname, HasAt x) {
        // TODO Auto-generated method stub
        return "*1_" + fname;
    }

    private String obfuscatedConstructorName(String fname) {
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
        List<LValueBind> lhs = x.getLhs();

        // List<Modifier> mods;
        // Id name = x.getName();
        // Option<Type> type = x.getType();
        Expr init = x.getInit();
        LValueBind lvb = lhs.get(0);

          Option<Type> type = lvb.getType();
          Id name = lvb.getName();
          String sname = NodeUtil.nameString(name);

          try {
              /* Ignore the type, until later */
              if (lvb.isMutable()) {
                  bindInto.putVariablePlaceholder(sname);
              } else {
                  FValue init_val = new LazilyEvaluatedCell(init, containing);
                  putValue(bindInto, sname, init_val);
              }
          } catch (FortressError pe) {
              throw pe.setContext(x,bindInto);
          }

//        int index = 0;

//        for (LValue lv : lhs) {
//            if (lv instanceof LValueBind) {
//                LValueBind lvb = (LValueBind) lv;
//                Option<Type> type = lvb.getType();
//                Id name = lvb.getName();
//                String sname = name.getName();
//
//                try {
//                    /* Ignore the type, until later */
//                    if (lvb.isMutable()) {
//                        bindInto.putVariablePlaceholder(sname);
//                    } else {
//                        FValue init_val;
//                        if (init instanceof ArgExpr) {
//                            init_val = new LazilyEvaluatedCell(
//                                      ((ArgExpr)init).getExprs().get(index++),
//                                      containing);
//                        } else {
//                            init_val = new LazilyEvaluatedCell(init, containing);
//                        }
//                        putValue(bindInto, sname, init_val);
//                    }
//                } catch (FortressError pe) {
//                    throw pe.setContext(x,bindInto);
//                }
//
//            } else {
//                bug(x, "Don't support arbitary LHS in Var decl yet");
//            }
//        }
    }

    private void forVarDecl2(VarDecl x) {

    }

    private void forVarDecl3(VarDecl x) {


    }

    private void forVarDecl4(VarDecl x) {

        List<LValueBind> lhs = x.getLhs();

        // List<Modifier> mods;
        // Id name = x.getName();
        // Option<Type> type = x.getType();
        Expr init = x.getInit();
        // int index = 0;
        LValueBind lvb = lhs.get(0);


         {
                Option<Type> type = lvb.getType();
                Id name = lvb.getName();
                String sname = NodeUtil.nameString(name);

                FType ft = type.isSome() ?
                        (new EvalType(containing)).evalType(Option.unwrap(type))
                                : null;

                if (lvb.isMutable()) {
                    Expr rhs = init;

                    FValue value = (new Evaluator(containing)).eval(rhs);

                    // TODO When new environment are created, need to insert
                    // into containing AND bindInto

                    if (ft != null) {
                        if (!ft.typeMatch(value)) {
                            ft = error(x, bindInto,
                                    errorMsg("Type mismatch binding ", value, " (type ",
                                             value.type(), ") to ", name, " (type ",
                                             ft, ")"));
                        }
                    } else {
                        ft = FTypeDynamic.ONLY;
                    }
                    /* Finally, can finish this initialiation. */
                    bindInto.storeType(x, sname, ft);
                    bindInto.assignValue(x, sname, value);
                } else {
                    // Force evaluation, snap the link, check the type!
                    FValue value = bindInto.getValue(sname);
                    if (ft != null) {
                        if (!ft.typeMatch(value)) {
                            error(x, bindInto,
                                  errorMsg("Type mismatch binding ", value, " (type ",
                                  value.type(), ") to ", name, " (type ",
                                  ft, ")"));
                        }
                    }
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
        List<StaticParam> staticParams = x.getStaticParams();
        // List<Modifier> mods;
        Id name = x.getName();
        // List<Type> excludes;
        // Option<List<Type>> bounds;
        // List<WhereClause> where;
        FTraitOrObjectOrGeneric ft;

        String fname = NodeUtil.nameString(name);

        if (!staticParams.isEmpty()) {

                FTypeGeneric ftg = new FTypeGeneric(containing, x, x.getDecls(), x);
                guardedPutType(fname, ftg, x);
                // scanForFunctionalMethodNames(ftg, x.getDecls(), ftg);
           ft = ftg;
        } else {

                BetterEnv interior = containing; // new BetterEnv(containing, x);
                FTypeTrait ftt = new FTypeTrait(fname, interior, x, x.getDecls(), x);
                guardedPutType(fname, ftt, x);
                // scanForFunctionalMethodNames(ftt, x.getDecls(), ftt);
           ft = ftt;
        }

        scanForFunctionalMethodNames(ft, x.getDecls());
    }
    private void forAbsTraitDecl2(AbsTraitDecl x) {
        // TODO Auto-generated method stub
        List<StaticParam> staticParams = x.getStaticParams();
        // List<Modifier> mods;
        Id name = x.getName();
        // List<Type> excludes;
        // Option<List<Type>> bounds;
        // List<WhereClause> where;

        if (!staticParams.isEmpty()) {

        } else {
           {
                FTypeTrait ftt = (FTypeTrait) containing
                        .getType(NodeUtil.nameString(name));
                BetterEnv interior = ftt.getEnv();
                finishTrait(x, ftt, interior);

            }
        }
    }
    private void forAbsTraitDecl3(AbsTraitDecl x) {
        Id name = x.getName();
        FTraitOrObjectOrGeneric ft =  (FTraitOrObjectOrGeneric) containing.getType(NodeUtil.nameString(name));
        scanForFunctionalMethodNames(ft, x.getDecls());
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
        List<StaticParam> staticParams = x.getStaticParams();
        // List<Modifier> mods;
        Id name = x.getName();
        // List<Type> excludes;
        // Option<List<Type>> bounds;
        // List<WhereClause> where;
        FTraitOrObjectOrGeneric ft;

        String fname = NodeUtil.nameString(name);

        if (!staticParams.isEmpty()) {

                FTypeGeneric ftg = new FTypeGeneric(containing, x, x.getDecls(), x);
            guardedPutType(fname, ftg, x);
                //scanForFunctionalMethodNames(ftg, x.getDecls(), ftg);
           ft = ftg;
        } else {

                BetterEnv interior = containing; // new BetterEnv(containing, x);
            FTypeTrait ftt = new FTypeTrait(fname, interior, x, x.getDecls(), x);
            guardedPutType(fname, ftt, x);
                //scanForFunctionalMethodNames(ftt, x.getDecls(), ftt);
           ft = ftt;
        }

        scanForFunctionalMethodNames(ft, x.getDecls());
    }
    private void forTraitDecl2(TraitDecl x) {
        // TODO Auto-generated method stub
        List<StaticParam> staticParams = x.getStaticParams();
        // List<Modifier> mods;
        Id name = x.getName();
        // List<Type> excludes;
        // Option<List<Type>> bounds;
        // List<WhereClause> where;

        if (!staticParams.isEmpty()) {

        } else {
           {
                FTypeTrait ftt = (FTypeTrait) containing
                        .getType(NodeUtil.nameString(name));
                BetterEnv interior = ftt.getEnv();
                finishTrait(x, ftt, interior);

            }
        }
    }
    private void forTraitDecl3(TraitDecl x) {
        Id name = x.getName();
        String fname = NodeUtil.nameString(name);
        FTraitOrObjectOrGeneric ft =  (FTraitOrObjectOrGeneric) containing.getType(fname);
        scanForFunctionalMethodNames(ft, x.getDecls());
    }

    private void forTraitDecl4(TraitDecl x) {
    }

    /**
     * @param x
     * @param ftt
     * @param interior
     */
    public void finishTrait(TraitAbsDeclOrDecl x, FTypeTrait ftt, BetterEnv interior) {
        List<TraitType> extends_ = NodeUtil.getTypes(x.getExtendsClause());
        interior = new BetterEnv(interior, x);

        EvalType et = processWhereClauses(x.getWhere(), interior);

        List<FType> extl = et.getFTypeListFromList(extends_);
        List<FType> excl = et.getFTypeListFromList(x.getExcludes());
        ftt.setExtendsAndExcludes(extl, excl, interior);
        List<? extends AbsDeclOrDecl> fns = x.getDecls();

        // doTraitMethodDefs(ftt, null); /* NOTICE THE DIFFERENT ENVIRONMENT! */

    }


    /**
     * Processes a where clause,
     * both using and augmenting the environment
     * "interior" passed in as a parameter.
     *
     * @param wheres
     * @param interior
     * @return
     */
    private static EvalType processWhereClauses(WhereClause wheres,
            BetterEnv interior) {

        if (wheres != null) {
            for (WhereConstraint w : wheres.getConstraints()) {
                if (w instanceof WhereExtends) {
                    WhereExtends we = (WhereExtends) w;
                    Id name = we.getName();
                    String string_name = NodeUtil.nameString(name);
                    // List<Type> types = we.getSupers();
                    FType ft = interior.getTypeNull(string_name);
                    if (ft == null) {
                        ft = new SymbolicWhereType(string_name, interior, we);
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
                    bug(w, errorMsg("Where clause ", w));
                }
            }
        }

        EvalType et = new EvalType(interior);

        if (wheres != null) {
            for (WhereConstraint w : wheres.getConstraints()) {
                if (w instanceof WhereExtends) {
                    WhereExtends we = (WhereExtends) w;
                    Id name = we.getName();
                    String string_name = NodeUtil.nameString(name);
                    List<TraitType> types = we.getSupers();
                    FType ft = interior.getTypeNull(string_name);
                    for (Type t : types) {
                        FType st = et.evalType(t); // t.visit(et);
                        if (ft instanceof SymbolicType) {
                            // Treat as "extends".
                            ((SymbolicType) ft).addExtend(st);
                        } else if (st instanceof SymbolicWhereType) {
                            // Record subtype ft of st.
                            SymbolicWhereType swt = (SymbolicWhereType) st;
                            swt.addSubtype(ft);
                        } else {
                            ft.mustExtend(st, w);
                            // Check that constraint holds.
                            // NI.nyi("need to verify constraint stated in where clause");
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
                    Type type = ta.getType();
                    interior.putType(NodeUtil.nameString(name), et.evalType(type));
                } else {
                    bug(w, errorMsg("Where clause ", w));
                }
            }
        }
        return et;
    }

    public void finishObjectTrait(ObjectAbsDeclOrDecl x, FTypeObject ftt) {
        List<TraitType> extends_ = NodeUtil.getTypes(x.getExtendsClause());
        finishObjectTrait(extends_, null, x.getWhere(), ftt, containing, x);
    }

    public void finishObjectTrait(_RewriteObjectExpr x, FTypeObject ftt) {
        List<TraitType> extends_ = NodeUtil.getTypes(x.getExtendsClause());
        // _RewriteObjectExpr has no excludes clause.
        finishObjectTrait(extends_, null, null, ftt, containing, x);
    }

    static public void finishObjectTrait(List<TraitType> extends_,
            List<? extends Type> excludes, WhereClause wheres, FTypeObject ftt,
            BetterEnv interior, HasAt x) {
        interior = new BetterEnv(interior, x);
        EvalType et = processWhereClauses(wheres, interior);
        ftt.setExtendsAndExcludes(et.getFTypeListFromList(extends_), et
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
        // Type type;
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forDimUnitDecl(com.sun.fortress.interpreter.nodes.DimUnitDecl)
     */
    @Override
    public Voidoid forDimUnitDecl(DimUnitDecl x) {
        // TODO Auto-generated method stub

        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forDimArg(com.sun.fortress.interpreter.nodes.DimArg)
     */
    @Override
    public Voidoid forDimArg(DimArg x) {
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
        switch (pass) {
        case 1: doAbsVarDecl(x); break;
        case 2:
        case 3:
        case 4: break;
        }
        return null;
    }

    private void doAbsVarDecl(AbsVarDecl x) {
        List<LValueBind> lhs = x.getLhs();

        // List<Modifier> mods;
        // Id name = x.getName();
        // Option<Type> type = x.getType();
        LValueBind lvb = lhs.get(0);

        Id name = lvb.getName();
        String sname = NodeUtil.nameString(name);

        try {
            /* Assumption: we only care for APIs, for which this
             * is a placeholder. */
            FValue init_val = new LazilyEvaluatedCell(null, null);
            putValue(bindInto, sname, init_val);
        } catch (FortressError pe) {
            throw pe.setContext(x,bindInto);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forFnDef(com.sun.fortress.interpreter.nodes.AbsFnDecl)
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


        List<StaticParam> optStaticParams = x.getStaticParams();
        String fname = NodeUtil.nameAsMethod(x);

        if (!optStaticParams.isEmpty()) {
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


        List<StaticParam> optStaticParams = x.getStaticParams();
        String fname = NodeUtil.nameAsMethod(x);

        if (!optStaticParams.isEmpty()) {
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

        List<StaticParam> staticParams = x.getStaticParams();
        Option<List<Param>> params = x.getParams();

        // List<Type> throws_;
        // WhereClause where;
        // Contract contract;
        // List<Decl> defs = x.getDecls();
        String fname = NodeUtil.nameString(name);
        FTraitOrObjectOrGeneric ft;
        ft = staticParams.isEmpty()
                ? new FTypeObject(fname, e, x, params, x.getDecls(), x)
                : new FTypeGeneric(e, x, x.getDecls(), x);

        // Need to check for overloaded constructor.

        guardedPutType(fname, ft, x);

        if (params.isSome()) {
            if (!staticParams.isEmpty()) {
                // A generic, not yet a constructor
                GenericConstructor gen = new GenericConstructor(e, x, name);
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
            if (!staticParams.isEmpty()) {
                // A parameterized singleton is a sort of generic value.
                makeGenericSingleton(x, e, name, fname, ft);

            } else {
                // Simply need to create a named value so that imports will work.
                FValue init_value = FVoid.V;
                putValue(bindInto, fname, init_value);
            }
        }

        scanForFunctionalMethodNames(ft, x.getDecls());

    }

    private void forAbsObjectDecl2(AbsObjectDecl x) {

        BetterEnv e = containing;
        Id name = x.getName();

        List<StaticParam> staticParams = x.getStaticParams();
        Option<List<Param>> params = x.getParams();

        String fname = NodeUtil.nameString(name);
        FType ft;

        if (params.isSome()) {
            if (!staticParams.isEmpty()) {
                // Do nothing.
            } else {
                FTypeObject fto = (FTypeObject) containing.getType(fname);
                finishObjectTrait(x, fto);
            }
        } else {
            if (!staticParams.isEmpty()) {
                // Do nothing.
            } else {
                FTypeObject fto = (FTypeObject) containing.getType(fname);

                finishObjectTrait(x, fto);
            }

        }

    }
    private void forAbsObjectDecl3(AbsObjectDecl x) {
        Id name = x.getName();
        String fname = NodeUtil.nameString(name);
        FTraitOrObjectOrGeneric ft = (FTraitOrObjectOrGeneric) containing.getType(fname);
        scanForFunctionalMethodNames(ft, x.getDecls());
    }
    private void forAbsObjectDecl4(AbsObjectDecl x) {
        BetterEnv e = containing;
        Id name = x.getName();

        List<StaticParam> staticParams = x.getStaticParams();
        Option<List<Param>> params = x.getParams();

        String fname = NodeUtil.nameString(name);
        FType ft;

        if (params.isSome()) {

        } else {

        }
        }

    public BetterEnv getBindingEnv() {
        return bindInto;
    }

    @Override
    public Voidoid forGrammarDecl(GrammarDecl that) {
        return null; // Do nothing
    }

    @Override
    public Voidoid forGrammarDef(GrammarDef that) {
        return null; // Do nothing
    }



}
