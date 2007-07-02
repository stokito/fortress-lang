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

package com.sun.fortress.interpreter.nodes_util;

import java.util.List;
import com.sun.fortress.interpreter.nodes.*;
import com.sun.fortress.interpreter.useful.Fn;

public class NodeUtil {

    public static String getName(StaticParam p) {
        if (p instanceof BoolParam) {
            return ((BoolParam)p).getId().getName();
        } else if (p instanceof DimensionParam) {
            return ((DimensionParam)p).getId().getName();
        } else if (p instanceof IntParam) {
            return ((IntParam)p).getId().getName();
        } else if (p instanceof NatParam) {
            return ((NatParam)p).getId().getName();
        } else if (p instanceof OperatorParam) {
            return ((OperatorParam)p).getOp().getName();
        } else if (p instanceof SimpleTypeParam) {
            return ((SimpleTypeParam)p).getId().getName();
        } else { throw new Error("Uncovered StaticParam.");
        }
    }

    public static final Fn<Id, String> IdtoStringFn = new Fn<Id, String>() {
        public String apply(Id x) {
            return x.getName();
        }
    };
}
