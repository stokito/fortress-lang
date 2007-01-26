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
import com.sun.fortress.interpreter.nodes.DefOrDecl;
import com.sun.fortress.interpreter.nodes.Generic;
import com.sun.fortress.interpreter.nodes.GenericDef;
import com.sun.fortress.interpreter.nodes.ObjectDecl;
import com.sun.fortress.interpreter.nodes.ObjectExpr;
import com.sun.fortress.interpreter.nodes.StaticArg;
import com.sun.fortress.interpreter.nodes.StaticParam;
import com.sun.fortress.interpreter.nodes.TraitDecl;
import com.sun.fortress.interpreter.useful.Factory1P;
import com.sun.fortress.interpreter.useful.HasAt;
import com.sun.fortress.interpreter.useful.LazyFactory1P;
import com.sun.fortress.interpreter.useful.LazyMemo1P;
import com.sun.fortress.interpreter.useful.Useful;


public class FTypeGeneric extends FType implements Factory1P<List<FType>, FTraitOrObject, HasAt> {
    public FTypeGeneric(BetterEnv e, GenericDef d) {
        super(d.stringName());
        env = e;
        def = d;
        params = d.getStaticParams().getVal();
        genericAt = d;
    }

    BetterEnv env;

    Generic def;

    public Generic getDef() {
        return def;
    }

    List<StaticParam> params;

    HasAt genericAt;

    String genericName;

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
                if (dod instanceof TraitDecl) {
                    TraitDecl td = (TraitDecl) dod;
                    FTypeTrait ftt = new FTypeTraitInstance(td.getName()
                            .getName(), clenv, FTypeGeneric.this, args);
                    FTraitOrObject old = map.put(args, ftt); // Must put
                                                                // early to
                                                                // expose for
                                                                // second pass.

                    be.secondPass();
                    be.finishTrait(td, ftt, clenv);
                    rval = ftt;
                } else if (dod instanceof ObjectDecl) {
                    ObjectDecl td = (ObjectDecl) dod;
                    FTypeObject fto = new FTypeObjectInstance(td.getName()
                            .getName(), clenv, FTypeGeneric.this, args);
                    map.put(args, fto); // Must put early to expose for second
                                        // pass.

                    be.secondPass();
                    be.finishObjectTrait(td, fto);
                    rval = fto;
                } else if (dod instanceof ObjectExpr) {
                    ObjectExpr td = (ObjectExpr) dod;
                    FTypeObject fto = new FTypeObjectInstance(td.stringName(),
                            clenv, FTypeGeneric.this, args);
                    map.put(args, fto); // Must put early to expose for second
                                        // pass.

                    be.secondPass();
                    be.finishObjectTrait(td, fto);
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
        return memo.make(l, within);
    }

    public FType typeApply(List<StaticArg> args, BetterEnv e, HasAt x) {
        List<StaticParam> params = def.getStaticParams().getVal();

        // Evaluate each of the args in e, inject into clenv.
        if (args.size() != params.size()) {
            throw new ProgramError(x, e,
                                   "Generic instantiation (size) mismatch, expected " + Useful.listInOxfords(params)
                                   + " got " + Useful.listInOxfords(args));
        }
        EvalType et = new EvalType(e);
        ArrayList<FType> argValues = et.forStaticArgList(args);
        return make(argValues, x);
    }

}
