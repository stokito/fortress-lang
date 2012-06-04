/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

/*
 * Fortress arrow type tail part
 * Fortress AST node local to the Rats! com.sun.fortress.interpreter.parser.
 */
package com.sun.fortress.parser_util;

import com.sun.fortress.nodes.Effect;
import com.sun.fortress.nodes.Type;

public class ArrowTypeTail {

    Type range;
    Effect effect;

    public ArrowTypeTail(Type in_range, Effect in_effect) {
        this.range = in_range;
        this.effect = in_effect;
    }

    public Type getRange() {
        return range;
    }

    public Effect getEffect() {
        return effect;
    }
}
