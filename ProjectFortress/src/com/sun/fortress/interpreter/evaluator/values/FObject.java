/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.values;

import com.sun.fortress.interpreter.evaluator.Environment;


public abstract class FObject extends FValue implements Selectable {
    /**
     * The environment that you get from "self."
     */
    public abstract Environment getSelfEnv();

    public abstract Environment getLexicalEnv();

    public FValue select(String s) {
        return getSelfEnv().getLeafValue(s);
    }

    public FValue selectField(String s) {
        return getSelfEnv().getLeafValue("$" + s);
    }

    public String getString() {
        return type().toString();
    }

    public String toString() {
        return getString();
    }
}
