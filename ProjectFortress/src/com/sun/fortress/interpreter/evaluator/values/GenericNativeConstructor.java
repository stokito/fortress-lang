/*
 * Created on Aug 30, 2007
 *
 */
package com.sun.fortress.interpreter.evaluator.values;

import java.util.List;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.BuildNativeEnvironment;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.types.FTypeObject;
import com.sun.fortress.nodes.GenericWithParams;

public class GenericNativeConstructor extends GenericConstructor {

    private String name;
    
    public GenericNativeConstructor(Environment env,
            GenericWithParams odefOrDecl, String name) {
        super(env, odefOrDecl);
        this.name = name;
    }

    protected Constructor makeAConstructor(BetterEnv clenv, FTypeObject objectType, List<Parameter> objectParams) {
        Constructor cl = BuildNativeEnvironment.nativeConstructor(clenv, objectType, odefOrDecl, name);
        cl.setParams(objectParams);
        cl.finishInitializing();
        return cl;
    }

}
