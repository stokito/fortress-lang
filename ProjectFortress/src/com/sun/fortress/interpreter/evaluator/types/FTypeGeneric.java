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

package com.sun.fortress.interpreter.evaluator.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.BuildEnvironments;
import com.sun.fortress.interpreter.evaluator.EvalType;
import com.sun.fortress.interpreter.evaluator.InterpreterBug;
import com.sun.fortress.interpreter.evaluator.ProgramError;
import com.sun.fortress.interpreter.rewrite.OprInstantiater;
import com.sun.fortress.nodes.AbsDeclOrDecl;
import com.sun.fortress.nodes.Generic;
import com.sun.fortress.nodes.ObjectDecl;
import com.sun.fortress.nodes.OperatorParam;
import com.sun.fortress.nodes.TraitObjectAbsDeclOrDecl;
import com.sun.fortress.nodes._RewriteObjectExpr;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.TraitAbsDeclOrDecl;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.Factory1P;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.LazyFactory1P;
import com.sun.fortress.useful.LazyMemo1P;
import com.sun.fortress.useful.Useful;

import static com.sun.fortress.interpreter.evaluator.ProgramError.errorMsg;

public class FTypeGeneric extends FType implements Factory1P<List<FType>, FTraitOrObject, HasAt> {
    public FTypeGeneric(BetterEnv e, Generic d, List<? extends AbsDeclOrDecl> members) {
        super(NodeUtil.stringName(d));
        env = e;
        def = d;
        params = d.getStaticParams();
        genericAt = d;
        this.members = members;
    }

    public FTypeGeneric(FTypeGeneric orig, TraitObjectAbsDeclOrDecl new_def) {
        super(orig.getName());
        env = orig.getEnv();
        genericAt = orig.getDef();
        def = new_def;
        params = new_def.getStaticParams();
        members = new_def.getDecls();

    }

    FTypeGeneric substituteOprs(List<FType> args) {
        return this;
    }


    BetterEnv env;

    Generic def;

    public Generic getDef() {
        return def;
    }

    @Override
    public BetterEnv getEnv() {
        return env;
    }

    List<StaticParam> params;

    HasAt genericAt;

    String genericName;

    List<? extends AbsDeclOrDecl> members;

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
                        FTypeOpr fto = (FTypeOpr) arg;
                        substitutions.put( ((OperatorParam)param).getOp().getName(),
                                fto.getName());
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
                FTypeTrait ftt = new FTypeTraitInstance(td.getId()
                        .getName(), clenv, gen, bind_args, key_args, gen.members);
                FTraitOrObject old = map.put(key_args, ftt); // Must put
                                                            // early to
                                                            // expose for
                                                            // second pass.

                // Perhaps make this conditional on nothing being symbolic here?
                be.scanForFunctionalMethodNames(ftt, td.getDecls(), true);
                be.secondPass();
                be.finishTrait(td, ftt, clenv);
                be.thirdPass();
                rval = ftt;
            } else if (dod instanceof ObjectDecl) {
                ObjectDecl td = (ObjectDecl) dod;
                FTypeObject fto = new FTypeObjectInstance(td.getId()
                        .getName(), clenv, gen, bind_args, key_args, gen.members);
                map.put(key_args, fto); // Must put early to expose for second
                                    // pass.

                be.scanForFunctionalMethodNames(fto, td.getDecls(), true);
                be.secondPass();
                be.finishObjectTrait(td, fto);
                be.thirdPass();
                be.scanForFunctionalMethodNames(fto, td.getDecls(), true);
                rval = fto;
            } else if (dod instanceof _RewriteObjectExpr) {
                _RewriteObjectExpr td = (_RewriteObjectExpr) dod;
                FTypeObject fto = new FTypeObjectInstance(NodeUtil.stringName(td),
                        clenv, gen, bind_args, key_args, gen.members);
                map.put(key_args, fto); // Must put early to expose for second
                                    // pass.

                be.scanForFunctionalMethodNames(fto, td.getDecls(), true);
                be.secondPass();
                be.finishObjectTrait(td, fto);
                be.thirdPass();
                be.scanForFunctionalMethodNames(fto, td.getDecls(), true);

                rval = fto;
            } else {
                throw new InterpreterBug(within, errorMsg(
                        "Generic def-or-declaration surprise ", dod));
            }

            return rval;

        } else {
            throw new InterpreterBug(within, errorMsg("Generic surprise ",
                                                      gen.def));
        }
    }


    LazyMemo1P<List<FType>, FTraitOrObject, HasAt> memo =
        new LazyMemo1P<List<FType>, FTraitOrObject, HasAt>(
            new Factory());

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
            throw new ProgramError(x, e,
                     errorMsg("Generic instantiation (size) mismatch, expected ",
                              Useful.listInOxfords(static_params),
                              " got ", Useful.listInOxfords(args)));
        }
        EvalType et = new EvalType(e);
        ArrayList<FType> argValues = et.forStaticArgList(args);
        return make(argValues, x);
    }

}
