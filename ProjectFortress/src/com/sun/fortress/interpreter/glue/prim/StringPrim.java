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


/**
 * Functions from RR64.
 */
public class StringPrim {

public static final class App extends Util.SS2S {
    protected String f(String x, String y) { return x + y; }
}
public static final class Eq extends Util.SS2B {
    protected boolean f(String x, String y) { return x.equals(y); }
}
public static final class Less extends Util.SS2B {
    protected boolean f(String x, String y) { return x.compareTo(y)<0; }
}
public static final class LessEq extends Util.SS2B {
    protected boolean f(String x, String y) { return x.compareTo(y)<=0; }
}
public static final class Greater extends Util.SS2B {
    protected boolean f(String x, String y) { return x.compareTo(y)>0; }
}
public static final class GreaterEq extends Util.SS2B {
    protected boolean f(String x, String y) { return x.compareTo(y)>=0; }
}

public static final class Print extends Util.S2V {
    protected void f(String x) { System.out.print(x); }
}
public static final class Println extends Util.S2V {
    protected void f(String x) { System.out.println(x); }
}

}
