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

package com.sun.fortress.interpreter.evaluator.types;

import static com.sun.fortress.exceptions.InterpreterBug.bug;
import static com.sun.fortress.exceptions.ProgramError.error;
import static com.sun.fortress.exceptions.ProgramError.errorMsg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.fortress.interpreter.evaluator.BuildEnvironments;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.EvalType;
import com.sun.fortress.interpreter.evaluator.InstantiationLock;
import com.sun.fortress.interpreter.rewrite.OprInstantiaterVisitor;
import com.sun.fortress.nodes.AbsDeclOrDecl;
import com.sun.fortress.nodes.AbstractNode;
import com.sun.fortress.nodes.BoolParam;
import com.sun.fortress.nodes.DimParam;
import com.sun.fortress.nodes.Generic;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IntParam;
import com.sun.fortress.nodes.NatParam;
import com.sun.fortress.nodes.NodeAbstractVisitor;
import com.sun.fortress.nodes.ObjectDecl;
import com.sun.fortress.nodes.OpArg;
import com.sun.fortress.nodes.OpParam;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.TraitAbsDeclOrDecl;
import com.sun.fortress.nodes.TraitObjectAbsDeclOrDecl;
import com.sun.fortress.nodes.TraitType;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.TypeArg;
import com.sun.fortress.nodes.TypeParam;
import com.sun.fortress.nodes.UnitParam;
import com.sun.fortress.nodes.VarType;
import com.sun.fortress.nodes._RewriteObjectExpr;
import com.sun.fortress.nodes_util.ExprFactory;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.Factory1P;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.LazyFactory1P;
import com.sun.fortress.useful.LazyMemo1PCL;
import com.sun.fortress.useful.Useful;

import edu.rice.cs.plt.tuple.Option;

public class FTypeGeneric extends FTraitOrObjectOrGeneric implements Factory1P<List<FType>, FTraitOrObject, HasAt> {

    /**
     * This is a HACK, to let other people make progress.
     * Do not scan for functional methods until the last possible moment;
     * we kick off generic instantiation work by instantiating a generic
     * trait or object type, and we need to keep track of this outermost
     * point and process the functional methods once we've completed type
     * instantiation for the entire type hierarchy reachable from that point.
     *
     * This used to store all the data in a pair of ThreadLocals.
     * However, we single-thread instantiation work using the
     * InstantiationLock, so it is safe to store this as static data.
     * This prevents anyone else from observing a
     * partially-instantiated type.  The type and constructor caches
     * are clever enough to ensure that we can continue to look at
     * fully-constructed stuff while another thread is instantiating.
     */
    static int instantiationDepth = 0;
    static List<FTraitOrObjectOrGeneric> pendingFunctionalMethodFinishes =
        new ArrayList<FTraitOrObjectOrGeneric>();

    public static void reset() {
    	pendingFunctionalMethodFinishes = new ArrayList<FTraitOrObjectOrGeneric>();
    }
    
    private final Generic def;
    private final FTypeGeneric original;

    public FTypeGeneric(Environment e, Generic d, List<? extends AbsDeclOrDecl> members, AbstractNode decl) {
        super(NodeUtil.stringName(d), e, decl);
        def = d;
        params = d.getStaticParams();
        genericAt = d;
        this.members = members;
        original = this;
    }

    public FTypeGeneric(FTypeGeneric orig, TraitObjectAbsDeclOrDecl new_def) {
        super(orig.getName(), orig.getWithin(), orig.getDecl());
        genericAt = orig.getDef();
        def = new_def;
        params = new_def.getStaticParams();
        members = new_def.getDecls();
        original = orig;
    }

    FTypeGeneric substituteOprs(List<FType> args) {
        return this;
    }

    public Generic getDef() {
        return def;
    }

    public FTypeGeneric getOriginal() {
        return original;
    }

    static public void startPendingTraitFMs() {
        if (!InstantiationLock.L.isHeldByCurrentThread()) {
            bug("startPendingTraitFMs without holding instantiation lock.");
        }
        instantiationDepth++;
    }

    static public void flushPendingTraitFMs() {
        if (--instantiationDepth > 0) return;
        for (int i = 0; i < pendingFunctionalMethodFinishes.size(); i++) {
            FTraitOrObjectOrGeneric tt = pendingFunctionalMethodFinishes.get(i);
            pendingFunctionalMethodFinishes.set(i, null);
            tt.finishFunctionalMethods();

        }
        pendingFunctionalMethodFinishes.clear();
    }

    public List<StaticParam> getParams() {
        return params;
    }

    public Type getInstantiationForFunctionalMethodInference() {
        List<StaticArg> statics = paramsToArgs();
        Id in = NodeFactory.makeId(def.getSpan(), name);
        TraitType inst_type = new TraitType(NodeFactory.makeSpan(in, statics), in, statics);
        return inst_type;
    }

    private List<StaticArg> paramsToArgs() {
        List<StaticArg> args = new ArrayList<StaticArg>(params.size());
        for (StaticParam p : params) {
            args.add(paramToArg(p));
        }
        // TODO Auto-generated method stub
        return args;
    }

    static class ParamToArg extends NodeAbstractVisitor<StaticArg> {

        private TypeArg idNameToTypeArg(Id idn) {
            return new TypeArg(idn.getSpan(),
                               new VarType(idn.getSpan(), idn));
        }

        @Override
        public StaticArg forBoolParam(BoolParam that) {
            return idNameToTypeArg(that.getName());
        }


        @Override
        public StaticArg forDimParam(DimParam that) {
            return idNameToTypeArg(that.getName());
        }

        @Override
        public StaticArg forIntParam(IntParam that) {
            return idNameToTypeArg(that.getName());
        }

        @Override
        public StaticArg forNatParam(NatParam that) {
            return idNameToTypeArg(that.getName());
        }

        @Override
        public StaticArg forOpParam(OpParam that) {
        	return new OpArg(that.getSpan(), ExprFactory.makeOpRef(that.getName()));
        }

        @Override
        public StaticArg forTypeParam(TypeParam that) {
            return idNameToTypeArg(that.getName());
        }

        @Override
        public StaticArg forUnitParam(UnitParam that) {
            return idNameToTypeArg(that.getName());
        }
    }

    private final static ParamToArg paramToArg = new ParamToArg();

    private StaticArg paramToArg(StaticParam p) {
        return p.accept(paramToArg);
    }

    List<StaticParam> params;

    HasAt genericAt;

    String genericName;

    private class Factory implements
            LazyFactory1P<List<FType>, FTraitOrObject, HasAt> {

        public FTraitOrObject make(List<FType> args, HasAt within,
                Map<List<FType>, FTraitOrObject> map) {
            int oprParamCount = 0;

            for (StaticParam param : params) {
                if (param instanceof OpParam)
                    oprParamCount++;
            }

            if (oprParamCount > 0) {
                HashMap<String, String> substitutions = new HashMap<String, String>();
                List<FType> thinned_args = new ArrayList<FType>(args.size() - oprParamCount);
                int i = 0;
                for (StaticParam param : params) {
                    FType arg = args.get(i);
                    if (param instanceof OpParam) {
                        if (arg instanceof FTypeOpr) {
                            FTypeOpr fto = (FTypeOpr) arg;
                            String s = NodeUtil.nameString(((OpParam)param).getName());
                            substitutions.put(s, fto.getName());
                        } else if (arg instanceof SymbolicOprType) {
                            SymbolicOprType fto = (SymbolicOprType) arg;
                            String s = NodeUtil.nameString(((OpParam)param).getName());
                            substitutions.put(s, fto.getName());
                        }
                    } else {
                        thinned_args.add(arg);
                    }
                    i++;
                }

                OprInstantiaterVisitor oi = new OprInstantiaterVisitor(substitutions);
                //OprInstantiater oi = new OprInstantiater(substitutions);

                TraitObjectAbsDeclOrDecl new_def =
                    (TraitObjectAbsDeclOrDecl)oi.visit((TraitObjectAbsDeclOrDecl)FTypeGeneric.this.getDef());

                FTypeGeneric replacement =
                    new FTypeGeneric(FTypeGeneric.this, new_def);

                return FTypeGeneric.make(thinned_args, args, within, map, replacement);
            } else {
                return FTypeGeneric.make(args, args, within, map, FTypeGeneric.this);
            }
        }
    }

    static  FTraitOrObject make(List<FType> bind_args, List<FType> key_args, HasAt within,
            Map<List<FType>, FTraitOrObject> map, FTypeGeneric gen) {
        Environment clenv = gen.env.extendAt(within);
        // List<StaticParam> params = def.getTypeParams().getVal();
        EvalType.bindGenericParameters(gen.params, bind_args, clenv, within,
                gen.genericAt);
        BuildEnvironments be = new BuildEnvironments(clenv);

        FTraitOrObject rval;

        if (gen.def instanceof TraitAbsDeclOrDecl) {
            TraitAbsDeclOrDecl td = (TraitAbsDeclOrDecl) gen.def;
            FTypeTraitInstance ftt = new FTypeTraitInstance(td.getName().getText(),
                                                    clenv, gen, bind_args, key_args, gen.members);
            FTraitOrObject old = map.put(key_args, ftt); // Must put
                                                         // early to
                                                         // expose for
                                                         // second pass.

            // Perhaps make this conditional on nothing being symbolic here?
            // Specify a top-level environment here.
            ftt.initializeFunctionalMethods(gen.env);
            //be.scanForFunctionalMethodNames(ftt, td.getDecls(), true);
            be.secondPass();
            be.finishTrait(td, ftt, clenv);
            be.thirdPass();
            // Perhaps this is ok now that we have self-param double-overload fix in.
            // be.scanForFunctionalMethodNames(ftt, td.getDecls(), true);

            pendingFunctionalMethodFinishes.add(ftt);
            rval = ftt;
        } else if (gen.def instanceof ObjectDecl) {
            ObjectDecl td = (ObjectDecl) gen.def;
            FTypeObject fto = new FTypeObjectInstance(td.getName().getText(),
                                       clenv, gen, bind_args, key_args,
                                                      td.getParams(),
                                                      gen.members);
            map.put(key_args, fto); // Must put early to expose for second
                                    // pass.

            fto.initializeFunctionalMethods();
            //be.scanForFunctionalMethodNames(fto, td.getDecls(), true);
            be.secondPass();
            be.finishObjectTrait(td, fto);
            be.thirdPass();
            //be.scanForFunctionalMethodNames(fto, td.getDecls(), true);
            pendingFunctionalMethodFinishes.add(fto);
            // fto.finishFunctionalMethods();
            // for (FType fte : fto.getExtends()) {
            //     ((FTraitOrObjectOrGeneric) fte).finishFunctionalMethods();
            // }
            rval = fto;
        } else if (gen.def instanceof _RewriteObjectExpr) {
            _RewriteObjectExpr td = (_RewriteObjectExpr) gen.def;
            FTypeObject fto = new FTypeObjectInstance(NodeUtil.stringName(td),
                                                      clenv, gen, bind_args,
                                                      key_args,
                                                      Option.<List<Param>>none(), gen.members);
            map.put(key_args, fto); // Must put early to expose for second
                                    // pass.

            fto.initializeFunctionalMethods();
            // be.scanForFunctionalMethodNames(fto, td.getDecls(), true);
            be.secondPass();
            be.finishObjectTrait(td, fto);
            be.thirdPass();
            //be.scanForFunctionalMethodNames(fto, td.getDecls(), true);
            pendingFunctionalMethodFinishes.add(fto);
            // fto.finishFunctionalMethods();
            // for (FType fte : fto.getExtends()) {
            //     ((FTraitOrObjectOrGeneric) fte).finishFunctionalMethods();
            // }
            rval = fto;
        } else {
            rval = bug( within, errorMsg("The use of generic type is " +
                        "found at unexpected place; it needs to be either " +
                        "within a trait, an object, or an object " + 
                        "expression type, but found: " + gen.def) );
       }

        return rval;
    }


    LazyMemo1PCL<List<FType>, FTraitOrObject, HasAt> memo =
        new LazyMemo1PCL<List<FType>, FTraitOrObject, HasAt>(
            new Factory(), FType.listComparer, InstantiationLock.L);

    public FTraitOrObject make(List<FType> l, HasAt within) {
        // System.out.println(""+within.at()+": "+
        //                    this.name+Useful.listInOxfords(l));
        FTraitOrObject r = memo.make(l, within);
        // System.out.println(""+within.at()+": "+
        //                    this.name+Useful.listInOxfords(l)+" R");
        return r;
    }

    public FType typeApply(List<StaticArg> args, Environment e, HasAt x) {
        List<StaticParam> static_params = def.getStaticParams();

        // Evaluate each of the args in e, inject into clenv.
        if (args.size() != static_params.size()) {
            error(x, e,
                  errorMsg("Generic instantiation (size) mismatch, expected ",
                           Useful.listInOxfords(static_params),
                           " got ", Useful.listInOxfords(args)));
        }
        EvalType et = new EvalType(e);
        ArrayList<FType> argValues = et.forStaticArgList(args);
        return make(argValues, x);
    }

    public String toString() {
        return getName()+Useful.listInOxfords(getParams())+"(uninstantiated)";
    }

}
