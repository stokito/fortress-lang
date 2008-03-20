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

package com.sun.fortress.interpreter.evaluator.values;

import java.util.List;

import com.sun.fortress.useful.HasAt;
import com.sun.fortress.nodes.GenericWithParams;
import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.EvalVarsEnvironment;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeObject;

import static com.sun.fortress.interpreter.evaluator.InterpreterBug.bug;

public abstract class NativeConstructor extends Constructor {

    private volatile BetterEnv selfEnv;

    public NativeConstructor(BetterEnv env,
                             FTypeObject selfType,
                             GenericWithParams def) {
        super(env,selfType,def);
    }

    public static abstract class FNativeObject extends FObject {
        public FNativeObject(NativeConstructor con) {
            setConstructor(con);
        }

        /**
         * Every native object must defined setConstructor, which
         * ought to stash the provided constructor in a static field
         * (if the type is non-generic) or otherwise cache it in the
         * constructed object itself.
         */
        public abstract void setConstructor(NativeConstructor con);

        /**
         * getConstructor retrieves the constructor stored away by
         * setConstructor.
         */
        public abstract NativeConstructor getConstructor();

        /**
         * All the getters operate in terms of getConstructor.
         */
        public BetterEnv getSelfEnv() {
            BetterEnv se = getConstructor().selfEnv;
            return se;
        }

        public BetterEnv getLexicalEnv() {
            return BetterEnv.blessedEmpty();
        }

        public FType type() {
            return getConstructor().selfType;
        }
    }

    /**
     *
     * Apply a constructor.  This method allows separate specification
     * of the lexical environment; this is done to simplify implementation
     * of object expressions.
     *
     */
    public FValue applyConstructor(
            List<FValue> args, HasAt loc, BetterEnv lex_env) {
        if (!finished) {
            bug(loc, "applyConstructor before finished!");
        }

        if (selfEnv == null) {
            initializeSelfEnv(args, loc);
        }

        return makeNativeObject(args, this);
    }

    private void initializeSelfEnv(List<FValue> args, HasAt loc) {
        // Problem -- we need to detach self-env from other env.
        if (methodsEnv == null)
            bug("Null methods env for " + this);

        BetterEnv self_env =
            buildEnvFromEnvAndParams(methodsEnv, args, loc);

        // TODO this is WRONG.  The vars need to be inserted into
        // self, but get evaluated against the larger (lexical)
        // environment.  Arrrrrrrggggggh.

        self_env.bless(); // HACK we add to this later.
        // This should go wrong if one of the vars has closure value
        // or objectExpr value.
        if (defs.size() > 0) {
            EvalVarsEnvironment eve =
                new EvalVarsEnvironment(self_env,self_env);
            visitDefs(eve); // HACK here's where we add to self_env.
        }

        synchronized (this) {
            if (selfEnv != null) return;

            selfEnv = self_env;
        }
    }

    protected abstract FNativeObject makeNativeObject(List<FValue> args,
                                                      NativeConstructor con);

}
