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

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.EvalType;
import com.sun.fortress.interpreter.evaluator.InterpreterError;
import com.sun.fortress.interpreter.nodes.ParamType;
import com.sun.fortress.interpreter.nodes.StaticArg;
import com.sun.fortress.interpreter.nodes.StaticParam;
import com.sun.fortress.interpreter.nodes.TypeArg;
import com.sun.fortress.interpreter.nodes.TypeRef;
import com.sun.fortress.interpreter.useful.ABoundingMap;


public class FTypeObjectInstance extends FTypeObject implements
        GenericTypeInstance {

    public FTypeObjectInstance(String name, BetterEnv interior,
            FTypeGeneric generic, List<FType> args) {
        super(name, interior, interior.getAt());
        this.generic = generic;
        this.args = args;
    }

    final private FTypeGeneric generic;

    final private List<FType> args;

    public FTypeGeneric getGeneric() {
        return generic;
    }

    public List<FType> getTypeParams() {
        return args;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.evaluator.types.FType#unify(java.util.Set, com.sun.fortress.interpreter.useful.ABoundingMap,
     *      com.sun.fortress.interpreter.nodes.TypeRef)
     */
    @Override
    public void unify(BetterEnv env, Set<StaticParam> tp_set,
            ABoundingMap<String, FType, TypeLatticeOps> abm, TypeRef val) {
        if (val instanceof ParamType) {
            ParamType pt = (ParamType) val;
            TypeRef val_generic = pt.getGeneric();
            List<StaticArg> val_args = pt.getArgs();
            // TODO Auto-generated method stub
            List<FType> ets = this.getTransitiveExtends();
            EvalType eval_type = new EvalType(env);
            FType eval_val_generic = val_generic.accept(eval_type);
            for (FType t : ets) {
                if (t instanceof GenericTypeInstance) {
                    GenericTypeInstance gti = (GenericTypeInstance) t;
                    FTypeGeneric g = gti.getGeneric();
                    List<FType> gp = gti.getTypeParams();
                    if (g == eval_val_generic) {
                        Iterator<StaticArg> val_args_iterator = val_args.iterator();
                        for (FType param_ftype: gp) {
                            StaticArg targ = val_args_iterator.next();
                            if (targ instanceof TypeArg) {
                                param_ftype.unify(env, tp_set, abm, ((TypeArg) targ).getType());
                            } else if (param_ftype instanceof FTypeNat) {
                                param_ftype.unify(env, tp_set, abm, targ);
                            } else {
                                throw new InterpreterError(val,env,"Can't yet unify non-type parameters to object expressions "+this+" and "+val);
                            }
                        }
                    }
                }
            }
        } else {
            super.unify(env, tp_set, abm, val);
        }
    }

}
