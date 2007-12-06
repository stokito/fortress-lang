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

package com.sun.fortress.interpreter.glue.prim;

import static com.sun.fortress.interpreter.evaluator.ProgramError.error;

/**
 * Functions from String.
 */
public class Char {
public static final class Eq extends Util.CC2B {
    protected boolean f(char x, char y) {
        return (int) x == (int) y;
    }
}

public static final class Print extends Util.C2V {
    protected void f(char x) { System.out.print(x); }
}
public static final class Println extends Util.C2V {
    protected void f(char x) { System.out.println(x); }
}


}
