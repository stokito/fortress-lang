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
import java.util.List;
import java.util.Map;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.BuildEnvironments;
import com.sun.fortress.interpreter.evaluator.EvalType;
import com.sun.fortress.interpreter.evaluator.InterpreterError;
import com.sun.fortress.interpreter.evaluator.ProgramError;
import com.sun.fortress.nodes.DefOrDecl;
import com.sun.fortress.nodes.Generic;
import com.sun.fortress.nodes.GenericDefOrDecl;
import com.sun.fortress.nodes.ObjectDecl;
import com.sun.fortress.nodes._RewriteObjectExpr;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.TraitDefOrDecl;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.Factory1P;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.LazyFactory1P;
import com.sun.fortress.useful.LazyMemo1P;
import com.sun.fortress.useful.Useful;

import static com.sun.fortress.interpreter.evaluator.ProgramError.errorMsg;

public class FTypeGeneric extends FType implements Factory1P<List<FType>, FTraitOrObject, HasAt> {
    public FTypeGeneric(BetterEnv e, GenericDefOrDecl d, List<? extends DefOrDecl> members) {
        super(NodeUtil.stringName(d));
        env = e;
        def = d;
        params = d.getStaticParams().getVal();
        genericAt = d;
        this.members = members;
    }

    BetterEnv env;

    Generic def;

    public Generic getDef() {
        return def;
    }

    List<StaticParam> params;

    HasAt genericAt;

    String genericName;

    List<? extends DefOrDecl> members;

    private class Factory implements
            LazyFactory1P<List<FType>, FTraitOrObject, HasAt> {

        public FTraitOrObject make(List<FType> args, HasAt within,
                Map<List<FType>, FTraitOrObject> map) {
            BetterEnv clenv = new BetterEnv(env, within);
            // List<StaticParam> params = def.getTypeParams().getVal();
            EvalType.bindGenericParameters(params, args, clenv, within,
                    genericAt);
            BuildEnvironments be = new BuildEnvironments(clenv);

            FTraitOrObject rval;

            if (def instanceof DefOrDecl) {
                DefOrDecl dod = (DefOrDecl) def;
                if (dod instanceof TraitDefOrDecl) {
                    TraitDefOrDecl td = (TraitDefOrDecl) dod;
                    FTypeTrait ftt = new FTypeTraitInstance(td.getName()
                            .getName(), clenv, FTypeGeneric.this, args, members);
                    FTraitOrObject old = map.put(args, ftt); // Must put
                                                                // early to
                                                                // expose for
                                                                // second pass.

                    // Perhaps make this conditional on nothing being symbolic here?
                    be.scanForFunctionalMethodNames(ftt, td.getFns(), true);
                    be.secondPass();
                    be.finishTrait(td, ftt, clenv);
                    be.thirdPass();
                    rval = ftt;
                } else if (dod instanceof ObjectDecl) {
                    ObjectDecl td = (ObjectDecl) dod;
                    FTypeObject fto = new FTypeObjectInstance(td.getName()
                            .getName(), clenv, FTypeGeneric.this, args, members);
                    map.put(args, fto); // Must put early to expose for second
                                        // pass.

                    be.scanForFunctionalMethodNames(fto, td.getDefOrDecls(), true);
                    be.secondPass();
                    be.finishObjectTrait(td, fto);
                    be.thirdPass();
                    be.scanForFunctionalMethodNames(fto, td.getDefOrDecls(), true);
                    rval = fto;
                } else if (dod instanceof _RewriteObjectExpr) {
                    _RewriteObjectExpr td = (_RewriteObjectExpr) dod;
                    FTypeObject fto = new FTypeObjectInstance(NodeUtil.stringName(td),
                            clenv, FTypeGeneric.this, args, members);
                    map.put(args, fto); // Must put early to expose for second
                                        // pass.

                    be.scanForFunctionalMethodNames(fto, td.getDefOrDecls(), true);
                    be.secondPass();
                    be.finishObjectTrait(td, fto);
                    be.thirdPass();
                    be.scanForFunctionalMethodNames(fto, td.getDefOrDecls(), true);

                    rval = fto;
                } else {
                    throw new InterpreterError(within,
                            "Generic def-or-declaration surprise " + dod);
                }



                return rval;

            } else {
                throw new InterpreterError(within, "Generic surprise " + def);
            }
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
        List<StaticParam> static_params = def.getStaticParams().getVal();

        // Evaluate each of the args in e, inject into clenv.
        if (args.size() != static_params.size()) {
            throw new ProgramError(x, e,
                                   errorMsg("Generic instantiation (size) mismatch, expected ", Useful.listInOxfords(static_params),
                                            " got ", Useful.listInOxfords(args)));
        }
        EvalType et = new EvalType(e);
        ArrayList<FType> argValues = et.forStaticArgList(args);
        return make(argValues, x);
    }

}
