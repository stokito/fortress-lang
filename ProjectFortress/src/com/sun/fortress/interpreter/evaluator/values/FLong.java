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
import com.sun.fortress.interpreter.evaluator.types.FTypeLong;

public class FLong extends FValue implements HasIntValue {
  long val;
  public int getInt() {
      // TODO Throw an error on out-of-range conversion?
      return (int) val;
  }
  public long getLong() {return (long)val;}
  public double getFloat() { return (double) val;}
  public String getString() {return Long.toString(val);}
  public String toString() {
      return "ZZ64 " + val;
  }
  private FLong(long x) {
    val = x;
    setFtype(FTypeLong.T);
  }

  static final FLong[] cached = {
      new FLong(0),
      new FLong(1),
      new FLong(2),
      new FLong(3),
      new FLong(4),
      new FLong(5),
      new FLong(6),
      new FLong(7),
      new FLong(8),
      new FLong(9),
      new FLong(10),
      new FLong(11),
      new FLong(12),
      new FLong(13),
      new FLong(14),
      new FLong(15),
      new FLong(16)
  };

  static final FLong[] neg_cached = {
      new FLong(-1),
      new FLong(-2),
      new FLong(-3),
      new FLong(-4),
      new FLong(-5),
      new FLong(-6),
      new FLong(-7),
      new FLong(-8),
      new FLong(-9),
      new FLong(-10),
      new FLong(-11),
      new FLong(-12),
      new FLong(-13),
      new FLong(-14),
      new FLong(-15),
      new FLong(-16)
  };

  public static FLong make(long i) {
      if (i >= 0) {
          if (i < cached.length) {
              return cached[(int)i];
          }
      } else if (~i < neg_cached.length) {
          return neg_cached[(int)~i];
      }
      return new FLong(i);
  }

}
