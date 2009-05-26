/*******************************************************************************
    Copyright 2009 Sun Microsystems, Inc.,
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

package com.sun.fortress.compiler.runtimeValues;

public class FString  extends FValue {
    String val;

    FString(String x) { val = x; }
    public String getValue() { return val;}
    public String toString() { return val;}
    public static FString make(String s) { return new FString(s);}
    public static FString concatenate(FString s1, FString s2) { return new FString(s1.toString() + s2.toString());}
}
