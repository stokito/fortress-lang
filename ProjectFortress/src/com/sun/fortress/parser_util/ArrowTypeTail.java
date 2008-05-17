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

/*
 * Fortress arrow type tail part
 * Fortress AST node local to the Rats! com.sun.fortress.interpreter.parser.
 */
package com.sun.fortress.parser_util;

import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.Effect;

public class ArrowTypeTail {

    Type range;
    Effect effect;

    public ArrowTypeTail(Type in_range,
                         Effect in_effect) {
        this.range = in_range;
        this.effect = in_effect;
    }

    public Type getRange() { return range; }
    public Effect getEffect() { return effect; }
}
