/*******************************************************************************
 Copyright 2010, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.values;

import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.types.FType;

public abstract class FNativeObject extends FObject {
    public FNativeObject(NativeConstructor con) {
    }

    /**
     * getConstructor retrieves the constructor stored away by
     * setConstructor.
     */
    public abstract NativeConstructor getConstructor();

    /**
     * All the getters operate in terms of getConstructor.
     */
    public Environment getSelfEnv() {
        return getConstructor().getSelfEnv();
    }

    public Environment getLexicalEnv() {
        return getConstructor().getLexicalEnv();
    }

    public FType type() {
        return getConstructor().selfType;
    }
}
