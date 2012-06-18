/*******************************************************************************
 Copyright 2008,2010, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.values;

import static com.sun.fortress.exceptions.InterpreterBug.bug;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.EvalVarsEnvironment;
import com.sun.fortress.interpreter.evaluator.types.FTypeObject;
import com.sun.fortress.nodes.ObjectConstructor;

import java.util.List;
import java.util.Vector;

public abstract class NativeConstructor extends Constructor {

    private Environment selfEnv;

    private static List<NativeConstructor> nativeConstructors = new Vector<NativeConstructor>();

    public NativeConstructor(Environment env, FTypeObject selfType, ObjectConstructor def) {
        super(env, selfType, def);
        nativeConstructors.add(this);
    }

    public static void unregisterAllConstructors() {
        for (NativeConstructor n : nativeConstructors) {
            n.unregister();
        }
        nativeConstructors.clear();
    }

    abstract protected void unregister();

    public Environment getSelfEnv() {
        return selfEnv;
    }

    public Environment getLexicalEnv() {
        return getWithin();
    }

    protected FValue check(FValue x) {
        return x;
    }

    /**
     * Apply a constructor.  This method allows separate specification
     * of the lexical environment; this is done to simplify implementation
     * of object expressions.
     */
    @Override
    public FValue applyConstructor(List<FValue> args) {
        return makeNativeObject(args, this);
    }

    @Override
    public void finishInitializing() {
        super.finishInitializing();
        initializeSelfEnv();
    }

    private void initializeSelfEnv() {
        // Problem -- we need to detach self-env from other env.
        if (methodsEnv == null) bug("Null methods env for " + this);

        Environment self_env = methodsEnv.extendAt(getAt());

        // TODO this is WRONG.  The vars need to be inserted into
        // self, but get evaluated against the larger (lexical)
        // environment.  Arrrrrrrggggggh.

        self_env.bless(); // HACK we add to this later.

        // This should go wrong if one of the vars has closure value
        // or objectExpr value.
        if (defs.size() > 0) {
            Environment lex_env = getWithin();
            EvalVarsEnvironment eve = new EvalVarsEnvironment(lex_env.extend(self_env), self_env);
            visitDefs(eve); // HACK here's where we add to self_env.
        }

        oneTimeInit(self_env);
        selfEnv = self_env;
    }

    /**
     * Code to run once, just before self_env is initialized.
     * At that point we have locked this.
     */
    protected void oneTimeInit(Environment self_env) {
    }

    protected abstract FNativeObject makeNativeObject(List<FValue> args, NativeConstructor con);

}
