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

package com.sun.fortress.interpreter.evaluator.values;
import com.sun.fortress.interpreter.evaluator.types.FTypeInt;

public class FInt extends FValue implements HasIntValue {
  int val;
  public int getInt() {return val;}
  public long getLong() {return (long)val;}
  public double getFloat() { return (double) val;}
  public String getString() {return Integer.toString(val);}
  public String toString() {
      return "ZZ32 " + val;
  }
  private FInt(int x) {
    val = x;
    setFtype(FTypeInt.T);
  }

  static FInt[] cached = {
      new FInt(0),
      new FInt(1),
      new FInt(2),
      new FInt(3),
      new FInt(4),
      new FInt(5),
      new FInt(6),
      new FInt(7),
      new FInt(8),
      new FInt(9),
      new FInt(10),
      new FInt(11),
      new FInt(12),
      new FInt(13),
      new FInt(14),
      new FInt(15),
      new FInt(16)
  };

  static FInt[] neg_cached = {
      new FInt(-1),
      new FInt(-2),
      new FInt(-3),
      new FInt(-4),
      new FInt(-5),
      new FInt(-6),
      new FInt(-7),
      new FInt(-8),
      new FInt(-9),
      new FInt(-10),
      new FInt(-11),
      new FInt(-12),
      new FInt(-13),
      new FInt(-14),
      new FInt(-15),
      new FInt(-16)
  };

  public static FInt make(int i) {
      if (i >= 0) {
          if (i < cached.length) {
              return cached[i];
          }
      } else if (~i < neg_cached.length) {
          return neg_cached[~i];
      }
      return new FInt(i);
  }

}
