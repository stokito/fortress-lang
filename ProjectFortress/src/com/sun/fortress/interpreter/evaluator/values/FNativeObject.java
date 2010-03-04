/*******************************************************************************
 Copyright 2010 Sun Microsystems, Inc.,
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