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

package com.sun.fortress.interpreter.evaluator.values;
import com.sun.fortress.interpreter.evaluator.types.FBuiltinType;
import com.sun.fortress.interpreter.evaluator.types.FTypeChar;

public class FChar extends FBuiltinValue {
    // For now Fortress chars are equivalent to Java chars.
    // We will eventually need to expand the definition to include the unicode
    // characters which require more than 16 bits.
  private final char val;
  public FBuiltinType type() {return FTypeChar.ONLY;}
  public String getString() {return Character.toString(val);}
  public char getChar() {return val;}

  static public FChar make(int x) {
      return new FChar((char) x);
  }

  FChar(char x) {
    val = x;
  }
}
