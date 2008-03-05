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

import static com.sun.fortress.interpreter.evaluator.InterpreterBug.bug;
import static com.sun.fortress.interpreter.evaluator.ProgramError.error;
import static com.sun.fortress.interpreter.evaluator.ProgramError.errorMsg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import edu.rice.cs.plt.tuple.Option;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.BuildEnvironments;
import com.sun.fortress.interpreter.evaluator.EvalType;
import com.sun.fortress.interpreter.evaluator.InstantiationLock;
import com.sun.fortress.interpreter.rewrite.OprInstantiater;
import com.sun.fortress.nodes.AbsDeclOrDecl;
import com.sun.fortress.nodes.AbstractNode;
import com.sun.fortress.nodes.BoolParam;
import com.sun.fortress.nodes.DimensionParam;
import com.sun.fortress.nodes.Generic;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdType;
import com.sun.fortress.nodes.InstantiatedType;
import com.sun.fortress.nodes.IntParam;
import com.sun.fortress.nodes.NatParam;
import com.sun.fortress.nodes.NodeAbstractVisitor;
import com.sun.fortress.nodes.ObjectDecl;
import com.sun.fortress.nodes.OperatorParam;
import com.sun.fortress.nodes.OprArg;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.nodes.SimpleTypeParam;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.TraitAbsDeclOrDecl;
import com.sun.fortress.nodes.TraitObjectAbsDeclOrDecl;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.TypeArg;
import com.sun.fortress.nodes.UnitParam;
import com.sun.fortress.nodes._RewriteObjectExpr;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.Factory1P;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.LazyFactory1P;
import com.sun.fortress.useful.LazyMemo1PCL;
import com.sun.fortress.useful.Useful;

public class FTypeGeneric extends FTraitOrObjectOrGeneric implements Factory1P<List<FType>, FTraitOrObject, HasAt> {

    /**
     * This is a HACK, to let other people make progress.
     * Traits should not be scanned for functional methods until the last possible moment,
     * and that is not known until, say, a generic constructor is returned.
     *
     */
    static ThreadLocal<List<FTraitOrObjectOrGeneric>> pendingFunctionalMethodFinishes = new ThreadLocal<List<FTraitOrObjectOrGeneric>>() {
        protected synchronized List<FTraitOrObjectOrGeneric> initialValue() {
            return new ArrayList<FTraitOrObjectOrGeneric>();
        }
    };

    static public void flushPendingTraitFMs() {
        for (int i = 0; i < pendingFunctionalMethodFinishes.get().size(); i++) {
            FTraitOrObjectOrGeneric tt = pendingFunctionalMethodFinishes.get().get(i);
            tt.finishFunctionalMethods();
        }
        pendingFunctionalMethodFinishes.get().clear();
    }

    public FTypeGeneric(BetterEnv e, Generic d, List<? extends AbsDeclOrDecl> members, AbstractNode decl) {
        super(NodeUtil.stringName(d), e, decl);
        def = d;
        params = d.getStaticParams();
        genericAt = d;
        this.members = members;
    }

    public FTypeGeneric(FTypeGeneric orig, TraitObjectAbsDeclOrDecl new_def) {
        super(orig.getName(), orig.getEnv(), orig.getDecl());
        genericAt = orig.getDef();
        def = new_def;
        params = new_def.getStaticParams();
        members = new_def.getDecls();

    }

    FTypeGeneric substituteOprs(List<FType> args) {
        return this;
    }


     Generic def;

    public Generic getDef() {
        return def;
    }

    public List<StaticParam> getParams() {
        return params;
    }

    public Type getInstantiationForFunctionalMethodInference() {
        List<StaticArg> statics = paramsToArgs();
        QualifiedIdName qin = NodeFactory.makeQualifiedIdName(def.getSpan(), name);
        InstantiatedType inst_type = new InstantiatedType(qin, statics);
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
                     new IdType(idn.getSpan(),
                             NodeFactory.makeQualifiedIdName(idn)));
         }

         @Override
        public StaticArg forBoolParam(BoolParam that) {
            return idNameToTypeArg(that.getName());
        }


        @Override
        public StaticArg forDimensionParam(DimensionParam that) {
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
        public StaticArg forOperatorParam(OperatorParam that) {
            return new OprArg(that.getName());
       }

        @Override
        public StaticArg forSimpleTypeParam(SimpleTypeParam that) {
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
                if (param instanceof OperatorParam)
                    oprParamCount++;
            }

            if (oprParamCount > 0) {
                HashMap<String, String> substitutions = new HashMap<String, String>();
                List<FType> thinned_args = new ArrayList<FType>(args.size() - oprParamCount);
                int i = 0;
                for (StaticParam param : params) {
                    FType arg = args.get(i);
                    if (param instanceof OperatorParam) {
                        if (arg instanceof FTypeOpr) {
                            FTypeOpr fto = (FTypeOpr) arg;
                            String s = NodeUtil.nameString(((OperatorParam)param).getName());
                            substitutions.put(s, fto.getName());
                        } else if (arg instanceof SymbolicOprType) {
                            SymbolicOprType fto = (SymbolicOprType) arg;
                            String s = NodeUtil.nameString(((OperatorParam)param).getName());
                            substitutions.put(s, fto.getName());
                        }
                    } else {
                        thinned_args.add(arg);
                    }
                    i++;
                }

                OprInstantiater oi = new OprInstantiater(substitutions);

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
        BetterEnv clenv = new BetterEnv(gen.env, within);
        // List<StaticParam> params = def.getTypeParams().getVal();
        EvalType.bindGenericParameters(gen.params, bind_args, clenv, within,
                gen.genericAt);
        BuildEnvironments be = new BuildEnvironments(clenv);

        FTraitOrObject rval;

        if (gen.def instanceof AbsDeclOrDecl) {
            AbsDeclOrDecl dod = (AbsDeclOrDecl) gen.def;
            if (dod instanceof TraitAbsDeclOrDecl) {
                TraitAbsDeclOrDecl td = (TraitAbsDeclOrDecl) dod;
                FTypeTrait ftt = new FTypeTraitInstance(td.getName().getText(),
                                                        clenv, gen, bind_args, key_args, gen.members);
                FTraitOrObject old = map.put(key_args, ftt); // Must put
                                                            // early to
                                                            // expose for
                                                            // second pass.

                // Perhaps make this conditional on nothing being symbolic here?
                ftt.initializeFunctionalMethods();
                //be.scanForFunctionalMethodNames(ftt, td.getDecls(), true);
                be.secondPass();
                be.finishTrait(td, ftt, clenv);
                be.thirdPass();
                // Perhaps this is ok now that we have self-param double-overload fix in.
                // be.scanForFunctionalMethodNames(ftt, td.getDecls(), true);

                pendingFunctionalMethodFinishes.get().add(ftt);

                rval = ftt;
            } else if (dod instanceof ObjectDecl) {
                ObjectDecl td = (ObjectDecl) dod;
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
                fto.finishFunctionalMethods();
                for (FType fte : fto.getExtends()) {
                    ((FTraitOrObjectOrGeneric) fte).finishFunctionalMethods();
                }
                rval = fto;
            } else if (dod instanceof _RewriteObjectExpr) {
                _RewriteObjectExpr td = (_RewriteObjectExpr) dod;
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
                fto.finishFunctionalMethods();
                for (FType fte : fto.getExtends()) {
                    ((FTraitOrObjectOrGeneric) fte).finishFunctionalMethods();
                }
                rval = fto;
            } else {
                rval = bug(within, errorMsg("Generic def-or-declaration surprise ", dod));
            }

            return rval;

        } else {
            return bug(within, errorMsg("Generic surprise ", gen.def));
        }
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

    public FType typeApply(List<StaticArg> args, BetterEnv e, HasAt x) {
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

}
