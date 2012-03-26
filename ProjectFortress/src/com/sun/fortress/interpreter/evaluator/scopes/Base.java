/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.scopes;

import com.sun.fortress.interpreter.evaluator.Environment;

public class Base implements Scope {
    private Environment env;

    public Base(Environment environment) {
        env = environment;
    }

    /**
     * @return Returns the com.sun.fortress.interpreter.env.
     */
    public Environment getEnv() {
        return env;
    }

}
