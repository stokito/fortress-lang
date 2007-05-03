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

package com.sun.fortress.interpreter.evaluator.values;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.ProgramError;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeRest;
import com.sun.fortress.interpreter.glue.Glue;
import com.sun.fortress.interpreter.glue.IndexedArrayWrapper;
import com.sun.fortress.interpreter.glue.WellKnownNames;
import com.sun.fortress.interpreter.useful.HasAt;
import com.sun.fortress.interpreter.useful.NI;


public abstract class NonPrimitive extends Simple_fcn {

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.evaluator.values.Simple_fcn#at()
     */
    @Override
    String at() {
        return getAt().at();
    }

    protected abstract HasAt getAt();

    NonPrimitive(BetterEnv within) {
        super(within);
    }

    private List<Parameter> params;

    private boolean lastParamIsRest;

    boolean hasRest() {
        return lastParamIsRest;
    }

    /**
     * @param params
     *            The params to set.
     */
    public final void setParams(List<Parameter> params) {
        if (this.params != null) {
            throw new IllegalStateException(
                    "Attempted second set of constructor/function/method params");
        }
        
        params = adjustParameterList(params);
        
        this.params = params;
        lastParamIsRest = params.size() > 0
                && (params.get(params.size() - 1).getType() instanceof FTypeRest);
        setValueType();
    }

    protected List<Parameter> adjustParameterList(List<Parameter> params2) {
        return params2;
    }

    abstract protected void setValueType();

    public boolean isMethod() {
        return false;
    }

    /**
     * @return Returns the params.
     */
    public List<Parameter> getParams() {
        return NI.nnf(params);
    }

    @Override
    public List<FType> getDomain() {
        return typeListFromParameters(getParams());
    }

    /**
     * Take passed-in parameters, type check them.
     * Intended to be called from NativeApp.
     * Do not bother calling this if you also call buildEnvFromParams.
     */
    public void typecheckParams(List<FValue> args, HasAt loc) {
        if (argCountIsWrong(args))
            throw new ProgramError(loc, within,
                    "Incorrect number of arguments, expected " + params.size()
                            + ", got " + args.size());
        Iterator<FValue> argsIter = args.iterator();
        Iterator<Parameter> paramsIter = params.iterator();
        for (int i = 1; paramsIter.hasNext(); i++) {
            Parameter param = paramsIter.next();
            FType paramType = param.getType();
            if (paramType instanceof FTypeRest) {
                FType restType = ((FTypeRest)paramType).getType();
                for (; argsIter.hasNext(); i++) {
                    FValue arg = argsIter.next();
                    if (!restType.typeMatch(arg)) {
                        throw new ProgramError(loc, within,
                                               "Closure/Constructor for "
                                               + getAt().stringName()
                                               + " rest parameter " + i + " ("
                                               + param.getName()
                                               + ":" + restType
                                               + "...) got type " + arg.type());
                    }
                }
            } else {
                // Usual case for the loop.
                FValue arg = argsIter.next();
                if (!paramType.typeMatch(arg)) {
                    throw new ProgramError(loc, within,
                            "Closure/Constructor for " + getAt().stringName()
                                    + " parameter " + i + " ("
                                    + param.getName() + ":"
                                    + param.getType() + ") got type "
                                    + arg.type());
                }
            }
        }
    }

    /**
     * Build environment for evaluation of closure.
     * Intended to be called from Closure.
     * @param args
     * @return
     * @throws Error
     */
    public BetterEnv buildEnvFromParams(List<FValue> args, HasAt loc)
            throws Error {
        BetterEnv env = new BetterEnv(within, getAt());
        return buildEnvFromParams(args, env, loc);
    }

    public BetterEnv buildEnvFromEnvAndParams(BetterEnv env, List<FValue> args, HasAt loc)
            throws Error {
        env = new BetterEnv(env, getAt());
        return buildEnvFromParams(args, env, loc);
    }

    public BetterEnv buildEnvFromParams(List<FValue> args, BetterEnv env,
            HasAt loc) throws Error {
        // TODO Here is where we deal with rest parameters.
        if (argCountIsWrong(args))
            throw new ProgramError(loc, env,
                    "Incorrect number of arguments, expected " + params.size()
                            + ", got " + args.size());

        Iterator<FValue> argsIter = args.iterator();
        FValue arg = null;
        int i = 0;
        for (Parameter param : params) {
            FType paramType = param.getType();
            if (paramType instanceof FTypeRest) {
                // Finish processing args in here (we just saw the last
                // parameter)

                // Find and invoke array1[\ T, size1 \] ()
                String genericName = WellKnownNames.arrayMaker(1);
                int[] natParams = new int[1];
                natParams[0] = args.size() - i;

                Simple_fcn f = Glue.instantiateGenericConstructor(env,
                        genericName, ((FTypeRest) paramType).getType(),
                        natParams, loc);

                FValue theArray = f
                        .apply(Collections.<FValue> emptyList(), loc, env);
                // Use a wrapper to simplify our life
                IndexedArrayWrapper iaw = new IndexedArrayWrapper(theArray, loc);
                int j = 0;
                while (argsIter.hasNext()) {
                    arg = argsIter.next();
                    iaw.put(arg, j);
                    j++;
                }
                // Do the copy.
                env.putValue(param.getName(), theArray);
            } else {
                // Usual case for the loop.
                arg = argsIter.next();
                i++;
                if (!paramType.typeMatch(arg)) {
                    throw new ProgramError(loc, env,
                            "Closure/Constructor for " + getAt().stringName()
                                    + " parameter " + i + " ("
                                    + param.getName() + ":"
                                    + paramType + ") got type "
                                    + arg.type());
                }
                try {
                    env.putValueUnconditionally(param.getName(), arg);
                } catch (ProgramError ex) {
                    throw ex.setWhere(loc).setWithin(env);
                }
            }

        }
        return env;
    }

    /**
     * @param args
     * @return
     */
    public boolean argCountIsWrong(List<FValue> args) {
        if (this.params==null) {
            throw new ProgramError(this.getAt(),
                                   "Calling argCountIsWrong on "+getAt().stringName()+" with null params");
        }
        return hasRest() ? args.size() + 1 < params.size()
                : args.size() != params.size();
    }
}
