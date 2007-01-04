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
import com.sun.fortress.interpreter.evaluator.scopes.SComponent;
import com.sun.fortress.interpreter.evaluator.types.FType;
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
import com.sun.fortress.interpreter.evaluator.values.GenericConstructor;
import com.sun.fortress.interpreter.evaluator.values.GenericFunctionSet;
import com.sun.fortress.interpreter.evaluator.values.GenericMethod;
import com.sun.fortress.interpreter.evaluator.values.GenericMethodSet;
import com.sun.fortress.interpreter.evaluator.values.OverloadedFunction;
import com.sun.fortress.interpreter.evaluator.values.Parameter;
import com.sun.fortress.interpreter.evaluator.values.Simple_fcn;
import com.sun.fortress.interpreter.nodes.AbsDecl;
import com.sun.fortress.interpreter.nodes.AbsFnDecl;
import com.sun.fortress.interpreter.nodes.AbsObjectDecl;
import com.sun.fortress.interpreter.nodes.AbsTraitDecl;
import com.sun.fortress.interpreter.nodes.AbsVarDecl;
import com.sun.fortress.interpreter.nodes.AnonymousFnName;
import com.sun.fortress.interpreter.nodes.Api;
import com.sun.fortress.interpreter.nodes.Applicable;
import com.sun.fortress.interpreter.nodes.BaseNodeVisitor;
import com.sun.fortress.interpreter.nodes.Component;
import com.sun.fortress.interpreter.nodes.Decl;
import com.sun.fortress.interpreter.nodes.DefOrDecl;
import com.sun.fortress.interpreter.nodes.Dimension;
import com.sun.fortress.interpreter.nodes.DottedId;
import com.sun.fortress.interpreter.nodes.Enclosing;
import com.sun.fortress.interpreter.nodes.Expr;
import com.sun.fortress.interpreter.nodes.FnDecl;
import com.sun.fortress.interpreter.nodes.FnDefOrDecl;
import com.sun.fortress.interpreter.nodes.FnName;
import com.sun.fortress.interpreter.nodes.HasWhere;
import com.sun.fortress.interpreter.nodes.Id;
import com.sun.fortress.interpreter.nodes.ImportApi;
import com.sun.fortress.interpreter.nodes.ImportIds;
import com.sun.fortress.interpreter.nodes.ImportNames;
import com.sun.fortress.interpreter.nodes.ImportStar;
import com.sun.fortress.interpreter.nodes.LValue;
import com.sun.fortress.interpreter.nodes.LValueBind;
import com.sun.fortress.interpreter.nodes.Modifier;
import com.sun.fortress.interpreter.nodes.ObjectDecl;
import com.sun.fortress.interpreter.nodes.ObjectExpr;
import com.sun.fortress.interpreter.nodes.Option;
import com.sun.fortress.interpreter.nodes.Param;
import com.sun.fortress.interpreter.nodes.StaticParam;
import com.sun.fortress.interpreter.nodes.TraitDecl;
import com.sun.fortress.interpreter.nodes.TypeAlias;
import com.sun.fortress.interpreter.nodes.TypeRef;
import com.sun.fortress.interpreter.nodes.UnitDim;
import com.sun.fortress.interpreter.nodes.UnitVar;
import com.sun.fortress.interpreter.nodes.VarDecl;
import com.sun.fortress.interpreter.nodes.WhereClause;
import com.sun.fortress.interpreter.nodes.WhereExtends;
import com.sun.fortress.interpreter.useful.HasAt;
import com.sun.fortress.interpreter.useful.NI;
import com.sun.fortress.interpreter.useful.Voidoid;


public class BuildEnvironments extends BaseNodeVisitor<Voidoid> {

    private boolean firstPass = true;

    public void secondPass() {
        firstPass = false;
        bindInto.bless();
    }

    public void resetPass() {
        firstPass = true;
    }

    BetterEnv containing;
    BetterEnv bindInto;

    /**
     * Creates an environment builder that will inject bindings into 'within'.
     * The visit is suspended at generics (com.sun.fortress.interpreter.nodes with type parameters) until
     * they can be instantiated.
     */
    public BuildEnvironments(BetterEnv within) {
        this.containing = within;
        this.bindInto = within;
    }
    public BuildEnvironments(BetterEnv within, BetterEnv bind_into) {
        this.containing = within;
        this.bindInto = bind_into;
    }

    private BuildEnvironments(BetterEnv within, boolean firstPass) {
        this.containing = within;
        this.bindInto = within;
        this.firstPass = firstPass;
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
     * @see com.sun.fortress.interpreter.nodes.BaseNodeVisitor#forApi(com.sun.fortress.interpreter.nodes.Api)
     */
    @Override
    public Voidoid forApi(Api x) {
        List<? extends DefOrDecl> decls = x.getDecls();

        // List<Import> imports = x.getImports();
        //DottedId name = x.getName();

        //SApi api = new SApi(containing, x);
        //containing.putApi(name, api);

        // TODO Run over the imports,
        // and inject names appropriately into the environment.

        for (DefOrDecl decl : decls) {
            decl.accept(this);
        }

        secondPass();

        for (DefOrDecl decl : decls) {
            decl.accept(this);
        }

        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.BaseNodeVisitor#forComponent(com.sun.fortress.interpreter.nodes.Component)
     */
    @Override
    public Voidoid forComponent(Component x) {
        forComponent1(x);
        secondPass();
        forComponent2(x);
        return null;
    }

    public Voidoid forComponent2(Component x) {
        List<? extends DefOrDecl> defs = x.getDefs();
        doDefs(containing, defs);
        return null;
    }

    public Voidoid forComponent1(Component x) {
        DottedId name = x.getName();
        // List<Import> imports = x.getImports();
        // List<Export> exports = x.getExports();
        List<? extends DefOrDecl> defs = x.getDefs();

        SComponent comp = new SComponent(BetterEnv.primitive(x), x);
        containing.putComponent(name, comp);

        // TODO Run over the imports,
        // and inject names appropriately into the environment.

        doDefs(containing, defs);

        return null;
    }

    public void doDefs(BetterEnv into, List<? extends DefOrDecl> defs) {
        BuildEnvironments inner = new BuildEnvironments(into, firstPass);
        for (DefOrDecl def : defs) {
            def.accept(inner);
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
    public void doTraitMethodDefs(BetterEnv into, BetterEnv forTraitMethods, List<? extends DefOrDecl> defs,
            Set<String> fields) {
        BuildTraitEnvironment inner = new BuildTraitEnvironment(into, forTraitMethods, fields);
        for (DefOrDecl def : defs) {
            def.accept(inner);
        }
        inner.secondPass();
        for (DefOrDecl def : defs) {
            def.accept(inner);
        }
    }

    private void guardedPutValue(BetterEnv e, String name, FValue value, HasAt where) {
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
     */protected void putValue(BetterEnv e, String name, FValue value) {
        e.putValue(name, value);
    }

    private void guardedPutValue(BetterEnv e, String name, FValue value, FType ft,
            HasAt where) {
        try {
            if (ft != null) {
                if (!ft.typeMatch(value)) {
                    throw new ProgramError(where,e,
                              "TypeRef mismatch binding " + value
                            + " (type " + value.type() + ") to " + name
                            + " (type " + ft + ")");
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
        return new FGenericFunction(e,x);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.BaseNodeVisitor#forFnDef(com.sun.fortress.interpreter.nodes.FnDecl)
     */
    @Override
    public Voidoid forFnDecl(FnDecl x) {
        //BetterEnv e = containing;

        /*
         * If a function definition is generic, suspend evaluation, including
         * the types of the parameter list, until the generic parameters have
         * been supplied.
         */

        // List<Modifier> mods;
        // List<TypeRef> throwss;
        // List<WhereClause> where;
        // Contract contract;
        // TODO Auto-generated method stub
        Option<List<StaticParam>> optStaticParams = x.getStaticParams();
        FnName name = x.getFnName();
        String fname = name.name();

        if (optStaticParams.isPresent()) {
            // GENERIC
            if (firstPass) {
                FValue cl = newGenericClosure(containing, x);
                putOrOverloadOrShadowGeneric(x, containing, name, cl);
//                guardedPutValue(containing, fname, cl, x);
//                if (name instanceof Enclosing) {
//                    Enclosing ename = (Enclosing) name;
//                    String efname = ename.getClose().getName();
//                    if (!efname.equals(name)) {
//                        guardedPutValue(containing, efname, cl, x);
//                    }
//                }
            } else {
                // Why isn't this the right thing to do?
                // FGenericFunction is (currently) excluded from this treatment.
                FValue fcn = containing.getValue(fname);
                if (fcn instanceof GenericFunctionSet) {
                    GenericFunctionSet gfs = (GenericFunctionSet) fcn;
                    gfs.finishInitializing();
                }
//
//                if (fcn instanceof Closure) {
//                    // This is only loosely paired with the
//                    // first pass; dealing with overloading tends to
//                    // break up the 1-1 relationship between the two.
//                    // However, because of the way that scopes nest,
//                    // it is possible (I think) that f could be overloaded
//                    // in an inner scope but not overloaded in an outer
//                    // scope.
//                    Closure cl = (Closure) fcn;
//                    cl.finishInitializing();
//                } else if (fcn instanceof OverloadedFunction) {
//                    OverloadedFunction og = (OverloadedFunction) fcn;
//                    og.finishInitializing();
//
//                }
            }

        } else {
            // NOT GENERIC
            if (firstPass) {
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
                putOrOverloadOrShadow(x, containing, name, cl);

            } else {
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

        return null;

        // Do not visit the body of the function; that BetterEnv is
        // created dynamically.
    }

    /**
     * @param x
     * @param e
     * @param name
     * @param cl
     */
    public void putOrOverloadOrShadow(
            HasAt x,
            BetterEnv e,
            FnName name,
            Simple_fcn cl) {
        Fcn g = (Fcn) e.getValueNull(name.name());
        if (g == null) {
            putFunction(e, name, cl, x);

            // This is delicate temporary code (below), and breaks the
            // property that adding another layer of environment is an OK
            // thing to do.
        } else if (g.getWithin().equals(e)) {
            // OVERLOADING
            OverloadedFunction og;
            if (g instanceof OverloadedFunction) {
                og = (OverloadedFunction) g;
                og.addOverload(cl);
            } else if (g instanceof GenericMethodSet || g instanceof GenericMethod) {
                throw new ProgramError(x,e, "Cannot combine generic method and nongeneric method " + name.name() + " in an overloading");
            } else if (g instanceof GenericFunctionSet || g instanceof FGenericFunction) {
                throw new ProgramError(x,e, "Cannot combine generic function and nongeneric function " + name.name() + " in an overloading");
            } else {
                og = new OverloadedFunction(name, e);
                og.addOverload(cl);
                og.addOverload((Simple_fcn) g);

                assignFunction(e, name, og);
            }
        } else {
            // SHADOWING
            putFunction(e, name, cl, x);
        }
    }

    /**
     * @param x
     * @param e
     * @param name
     * @param cl
     */
    public void putOrOverloadOrShadowGeneric(
            HasAt x,
            BetterEnv e,
            FnName name,
            FValue cl) {
        FValue fv = e.getValueNull(name.name());
        if (fv != null && ! (fv instanceof Fcn)) {
            throw new ProgramError(x, e, "Generic not generic? " + name.name());
        }
        Fcn g = (Fcn) fv;
        // Actually need to test for diff types of g.
        if (g == null) {
            putFunction(e, name, cl, x);
        } else if (g.getWithin().equals(e)) {
            // OVERLOADING
            if (cl instanceof GenericMethod) {
                GenericMethod clg = (GenericMethod) cl;
                GenericMethodSet og;
                if (g instanceof GenericMethodSet) {
                    og = (GenericMethodSet) g;
                    og.addOverload(clg);
                } else if (g instanceof GenericMethod) {
                    og = new GenericMethodSet(name, e);
                    og.addOverload(clg);
                    og.addOverload((GenericMethod) g);

                    assignFunction(e, name, og);
                } else {
                    throw new ProgramError(x, e, "Overload of generic method "
                            + cl + " with non-generic/method " + g);
                }
            } else if (cl instanceof FGenericFunction) {
                FGenericFunction clg = (FGenericFunction) cl;
                GenericFunctionSet og;
                if (g instanceof GenericFunctionSet) {
                    og = (GenericFunctionSet) g;
                    og.addOverload(clg);
                } else if (g instanceof FGenericFunction) {
                    og = new GenericFunctionSet(name, e);
                    og.addOverload(clg);
                    og.addOverload((FGenericFunction) g);

                    assignFunction(e, name, og);
                } else {
                    throw new ProgramError(x, e, "Overload of function method "
                            + cl + " with non-generic/method " + g);
                }
            } else {
                throw new ProgramError(x, e,
                        "Overload of generic, but not a method/function" + cl
                                + " with generic/method " + g);

            }
        } else {
            // SHADOWING
            putFunction(e, name, cl, x);
        }
    }

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
//        if (name instanceof Enclosing) {
//            Enclosing ename = (Enclosing) name;
//            String efname = ename.getClose().getName();
//            // TODO seems unnecessary.
//            if (!efname.equals(s))
//                guardedPutValue(e, efname, f, x);
//        }
    }

    private static void assignFunction(BetterEnv e, FnName name, FValue f) {
        e.putValueUnconditionally(name.name(), f);
//        if (name instanceof Enclosing) {
//            Enclosing ename = (Enclosing) name;
//            String efname = ename.getClose().getName();
//            if (!efname.equals(name.name()))
//                e.putValueUnconditionally(efname, f);
//        }
    }

//    public static FValue anObject(FTypeObject ft, BetterEnv e, Option<List<TypeRef>> traits,
//            List<Decl> defs, HasAt x) {
//
//        Constructor cl = new Constructor(e, (FTypeObject) ft, x,
//                new AnonymousFnName(x), defs);
//        cl.setParams(Collections.<Parameter> emptyList());
//        finishObjectTrait(traits, null, null, ft, e, x);
//        cl.finishInitializing();
//
//        return cl.apply(java.util.Collections.<FValue> emptyList(), x, e);
//    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.BaseNodeVisitor#forObjectDef(com.sun.fortress.interpreter.nodes.ObjectDecl)
     */
    @Override
    public Voidoid forObjectDecl(ObjectDecl x) {
        // List<Modifier> mods;

        BetterEnv e = containing;
        Id name = x.getName();

        Option<List<StaticParam>> staticParams = x.getStaticParams();
        Option<List<Param>> params = x.getParams();

        // List<TypeRef> throws_;
        // List<WhereClause> where;
        // Contract contract;
        // List<Decl> defs = x.getDefs();
        String fname = name.getName();
        FType ft;
        if (firstPass) {
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
                    Constructor cl = new Constructor(containing,
                            (FTypeObject) ft, x);
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
                    Constructor cl = new Constructor(containing,
                            (FTypeObject) ft, x);
                    guardedPutValue(containing, obfuscated(fname), cl, x);

                    // doDefs(interior, defs);
                }
            }
        } else {

            if (params.isPresent()) {
                if (staticParams.isPresent()) {
                    // Do nothing.
                } else {
                    FTypeObject fto = (FTypeObject) containing.getType(fname);
                    Constructor cl = (Constructor) containing.getValue(fname);
                    List<Parameter> fparams = EvalType.paramsToParameters(
                            containing, params.getVal());
                    cl.setParams(fparams);
                    finishObjectTrait(x, fto);
                    cl.finishInitializing();
                }
            } else {
                // If there are no parameters, it is a singleton.
                // Not clear we can evaluate it yet.
                FTypeObject fto = (FTypeObject) containing.getType(fname);
                Constructor cl = (Constructor) containing
                        .getValue(obfuscated(fname));
                cl.setParams(Collections.<Parameter> emptyList());
                finishObjectTrait(x, fto);
                cl.finishInitializing();
                ;
                guardedPutValue(containing, fname, cl.apply(java.util.Collections
                        .<FValue> emptyList(), x, e), x);

            }
        }

        return null;
    }

    private String obfuscated(String fname) {
        // TODO Auto-generated method stub
        return "*1_" + fname;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.BaseNodeVisitor#forVarDef(com.sun.fortress.interpreter.nodes.VarDecl)
     */
    @Override
    public Voidoid forVarDecl(VarDecl x) {

        List<LValue> lhs = x.getLhs();

        // List<Modifier> mods;
        //Id name = x.getName();
        //Option<TypeRef> type = x.getType();
        Expr init = x.getInit();

        if (firstPass) {
        } else {
            FValue init_value = init.accept(new Evaluator(
                    containing));
            for (LValue lv : lhs) {
                if (lv instanceof LValueBind) {
                    LValueBind lvb = (LValueBind) lv;

                Option<TypeRef> type = lvb.getType();
                Id name = lvb.getName();

            FType ft = type.isPresent() ? type.getVal().accept(
                    new EvalType(containing)) : null;
            // TODO We're not careful enough about forward references here.
            // TODO When new environment are created, need to insert into containing AND bindInto
            guardedPutValue(bindInto, name.getName(), init_value, ft, x);
                } else {
                    throw new InterpreterError(x, "Don't support arbitary LHS in Var decl yet");
                }
            }
        }

        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.BaseNodeVisitor#forTraitDecl(com.sun.fortress.interpreter.nodes.AbsTraitDecl)
     */
    @Override
    public Voidoid forAbsTraitDecl(AbsTraitDecl x) {
        // TODO Auto-generated method stub
        // List<Modifier> mods;
        // Id name;
        // Option<List<StaticParam>> staticParams;
        // Option<List<TypeRef>> extends_;
        // List<TypeRef> excludes;
        // Option<List<TypeRef>> bounds;
        // List<WhereClause> wheres;
        // List<AbsFnDecl> fns;

        return super.forAbsTraitDecl(x);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.BaseNodeVisitor#forTraitDef(com.sun.fortress.interpreter.nodes.TraitDecl)
     */
    @Override
    public Voidoid forTraitDecl(TraitDecl x) {
        // TODO Auto-generated method stub
        Option<List<StaticParam>> staticParams = x.getStaticParams();
        // List<Modifier> mods;
        Id name = x.getName();
        // List<TypeRef> excludes;
        // Option<List<TypeRef>> bounds;
        // List<WhereClause> where;

        if (staticParams.isPresent()) {
            if (firstPass) {
                FTypeGeneric ftg = new FTypeGeneric(containing, x);
                guardedPutType(name.getName(), ftg, x);
            }
        } else {
            if (firstPass) {
                BetterEnv interior = containing; // new BetterEnv(containing, x);
                FTypeTrait ftt = new FTypeTrait(name.getName(), interior, x);
                guardedPutType(name.getName(), ftt, x);

            } else {
                FTypeTrait ftt = (FTypeTrait) containing
                        .getType(name.getName());
                BetterEnv interior = ftt.getEnv();
                finishTrait(x, ftt, interior);

            }
        }
        return null;
    }

    /**
     * @param x
     * @param ftt
     * @param interior
     */
    public void finishTrait(TraitDecl x, FTypeTrait ftt, BetterEnv interior) {
        Option<List<TypeRef>> extends_ = x.getExtends_();
        interior = new BetterEnv(interior, x);

        EvalType et = processWhereClauses(x.getWhere(), interior);

        List<FType> extl = et.getFTypeListFromOptionList(extends_);
        List<FType> excl = et.getFTypeListFromList(x.getExcludes());
        ftt.setExtendsAndExcludes(extl, excl, interior);
        List<? extends DefOrDecl> fns = x.getFns();

        doTraitMethodDefs(ftt.getMembers(), ftt.getMethodExecutionEnv(), fns, null); // NOTICE THE
                                                            // DIFFERENT
                                                            // ENVIRONMENT!

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
                        FType st = t.accept(et);
                        if (ft instanceof SymbolicType) {
                            // Treat as "extends".
                            ((SymbolicType) ft).addExtend(st);
                        } else if (st instanceof SymbolicWhereType) {
                            // Record subtype ft of st.
                            SymbolicWhereType swt = (SymbolicWhereType) st;
                            swt.addSubtype(ft);
                        } else {
                            // Check that constraint holds.
                            NI
                                    .nyi("need to verify constraint stated in where clause");
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
                    interior.putType(name.getName(), type.accept(et));
                } else {
                    NI.nyi("Where clause " + w);
                }
            }
        }
        return et;
    }

    public void finishObjectTrait(ObjectDecl x, FTypeObject ftt) {
        Option<List<TypeRef>> extends_ = x.getTraits();
        finishObjectTrait(extends_, null, x.getWhere(), ftt, containing, x);
    }

    public void finishObjectTrait(ObjectExpr x, FTypeObject ftt) {
        Option<List<TypeRef>> extends_ = x.getTraits();
        // ObjectExpr has no excludes clause.
        finishObjectTrait(extends_, null, null, ftt, containing, x);
    }

    static public void finishObjectTrait(Option<List<TypeRef>> extends_, List<TypeRef> excludes, List<WhereClause> wheres,
            FTypeObject ftt, BetterEnv interior, HasAt x) {
        interior = new BetterEnv(interior, x);
        EvalType et = processWhereClauses(wheres, interior);
        ftt.setExtendsAndExcludes(et.getFTypeListFromOptionList(extends_), et.getFTypeListFromList(excludes), interior);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.BaseNodeVisitor#forTypeAlias(com.sun.fortress.interpreter.nodes.TypeAlias)
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
     * @see com.sun.fortress.interpreter.nodes.BaseNodeVisitor#forDimension(com.sun.fortress.interpreter.nodes.Dimension)
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
     * @see com.sun.fortress.interpreter.nodes.BaseNodeVisitor#forUnitDim(com.sun.fortress.interpreter.nodes.UnitDim)
     */
    @Override
    public Voidoid forUnitDim(UnitDim x) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.BaseNodeVisitor#forUnitVar(com.sun.fortress.interpreter.nodes.UnitVar)
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
     * @see com.sun.fortress.interpreter.nodes.BaseNodeVisitor#forImportApi(com.sun.fortress.interpreter.nodes.ImportApi)
     */
    @Override
    public Voidoid forImportApi(ImportApi x) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.BaseNodeVisitor#forImportIds(com.sun.fortress.interpreter.nodes.ImportIds)
     */
    @Override
    public Voidoid forImportIds(ImportIds x) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.BaseNodeVisitor#forImportNames(com.sun.fortress.interpreter.nodes.ImportNames)
     */
    @Override
    public Voidoid forImportNames(ImportNames x) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.BaseNodeVisitor#forImportStar(com.sun.fortress.interpreter.nodes.ImportStar)
     */
    @Override
    public Voidoid forImportStar(ImportStar x) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.BaseNodeVisitor#forVarDecl(com.sun.fortress.interpreter.nodes.AbsVarDecl)
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
     * @see com.sun.fortress.interpreter.nodes.BaseNodeVisitor#forFnDecl(com.sun.fortress.interpreter.nodes.AbsFnDecl)
     */
    @Override
    public Voidoid forAbsFnDecl(AbsFnDecl x) {

        /*
         * EXPERIMENTAL CUT/PASTE from forFnDef to simplify trait
         * implementation.
         */

        // I think Decls are a no-op in the interpreter.
        // List<Modifier> mods;
        // FnName name;
        // Option<List<StaticParam>> staticParams;
        // List<Param> params;
        // Option<TypeRef> returnType;
        // List<TypeRef> throwss;
        // List<WhereClause> where;
        // Contract contract;
        // Environment e = containing;

        /*
         * If a function definition is generic, suspend evaluation, including
         * the types of the parameter list, until the generic parameters have
         * been supplied.
         */

        // List<Modifier> mods;
        // List<TypeRef> throwss;
        // List<WhereClause> where;
        // Contract contract;
        // TODO Auto-generated method stub
        Option<List<StaticParam>> optStaticParams = x.getStaticParams();
        FnName name = x.getFnName();
        String fname = name.name();

        if (optStaticParams.isPresent()) {
            // GENERIC
            if (firstPass) {
                // TODO same treatment as regular functions.
                FValue cl = newGenericClosure(containing, x);
                putOrOverloadOrShadowGeneric(x, containing, name, cl);
            }

        } else {
            // NOT GENERIC
            if (firstPass) {
                Simple_fcn cl = newClosure(containing, x);
                putOrOverloadOrShadow(x, containing, name, cl);
            } else {
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

        return null;

    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.BaseNodeVisitor#forObjectDecl(com.sun.fortress.interpreter.nodes.AbsObjectDecl)
     */
    @Override
    public Voidoid forAbsObjectDecl(AbsObjectDecl x) {
        // I think Decls are a no-op in the interpreter.
        // List<Modifier> mods;
        //
        // Id name;
        //
        // Option<List<StaticParam>> staticParams;
        // Option<List<Param>> params;
        //        Option<List<TypeRef>> traits;
        //        List<TypeRef> throws_;
        //        List<WhereClause> where;
        //        Contract contract;
        //        List<AbsDecl> decls;
        // TODO Auto-generated method stub
        return null;
    }

    public BetterEnv getBindingEnv() {
        return bindInto;
    }

}
