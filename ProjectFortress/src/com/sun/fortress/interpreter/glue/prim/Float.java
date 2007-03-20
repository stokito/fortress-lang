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
public class Float {

public static final class Negate extends Util.R2R {
    protected double f(double x) { return -x; }
}
public static final class Add extends Util.RR2R {
    protected double f(double x, double y) { return x + y; }
}
public static final class Sub extends Util.RR2R {
    protected double f(double x, double y) { return x - y; }
}
public static final class Mul extends Util.RR2R {
    protected double f(double x, double y) { return x * y; }
}
public static final class Div extends Util.RR2R {
    protected double f(double x, double y) { return x / y; }
}
public static final class Eq extends Util.RR2B {
    protected boolean f(double x, double y) { return x==y; }
}
public static final class NEq extends Util.RR2B {
    protected boolean f(double x, double y) { return x!=y; }
}
public static final class Less extends Util.RR2B {
    protected boolean f(double x, double y) { return x<y; }
}
public static final class LessEq extends Util.RR2B {
    protected boolean f(double x, double y) { return x<=y; }
}
public static final class Greater extends Util.RR2B {
    protected boolean f(double x, double y) { return x>y; }
}
public static final class GreaterEq extends Util.RR2B {
    protected boolean f(double x, double y) { return x>=y; }
}
public static final class Min extends Util.RR2R {
    protected double f(double x, double y) { return Math.min(x,y); }
}
public static final class Max extends Util.RR2R {
    protected double f(double x, double y) { return Math.max(x,y); }
}
public static final class Pow extends Util.RR2R {
    protected double f(double x, double y) {
        return Math.pow(x,y);
    }
}
public static final class Sqrt extends Util.R2R {
    protected double f(double x) { return Math.sqrt(x); }
}
public static final class Sin extends Util.R2R {
    protected double f(double x) { return Math.sin(x); }
}
public static final class Cos extends Util.R2R {
    protected double f(double x) { return Math.cos(x); }
}
public static final class Tan extends Util.R2R {
    protected double f(double x) { return Math.tan(x); }
}
public static final class ASin extends Util.R2R {
    protected double f(double x) { return Math.asin(x); }
}
public static final class ACos extends Util.R2R {
    protected double f(double x) { return Math.acos(x); }
}
public static final class ATan extends Util.R2R {
    protected double f(double x) { return Math.atan(x); }
}
public static final class ATan2 extends Util.RR2R {
    protected double f(double y, double x) { return Math.atan2(y,x); }
}
public static final class Log extends Util.R2R {
    protected double f(double x) { return Math.log(x); }
}
public static final class Exp extends Util.R2R {
    protected double f(double x) { return Math.exp(x); }
}
public static final class Ceiling extends Util.R2R {
    protected double f(double x) { return Math.ceil(x); }
}
public static final class Floor extends Util.R2R {
    protected double f(double x) { return Math.floor(x); }
}
public static final class ICeiling extends Util.R2L {
    protected long f(double x) { return (long)Math.ceil(x); }
}
public static final class IFloor extends Util.R2L {
    protected long f(double x) { return (long)Math.floor(x); }
}
public static final class Truncate extends Util.R2L {
    protected long f(double x) { return (long)x; }
}
public static final class Abs extends Util.R2R {
    protected double f(double x) { return Math.abs(x); }
}
public static final class Random extends Util.R2R {
    protected double f(double scale) { return scale*Math.random(); }
}

}
