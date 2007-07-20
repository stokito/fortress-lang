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

package com.sun.fortress.interpreter.evaluator.types;

import java.util.List;

import com.sun.fortress.useful.Factory1;
import com.sun.fortress.useful.Memo1;
import com.sun.fortress.useful.Useful;


public class FTypeOverloadedArrow extends FType {

    private static class Factory implements Factory1<List<FType>, FType> {

        public FType make(List<FType> part1) {
            return new FTypeOverloadedArrow(part1);
        }

    }

    static Memo1<List<FType>, FType> memo = null;

    static public void reset() {
        memo = new Memo1<List<FType>, FType>(new Factory());
    }

    static public FType make(List<FType> l) {
        return memo.make(l);
    }

  List<FType> l;

  private FTypeOverloadedArrow(List<FType> l) {
      super("Overloaded"); // TODO should construct this lazily, better
      this.l = l;
  }

  public String toString() {
      return Useful.listInParens(l);
  }

  public boolean subtypeOf(FType other) {
      if (commonSubtypeOf(other)) return true;
      if (other instanceof FType) {
          FType fta = (FType)(other);
          for (FType  i : l) {
              if (i.subtypeOf(fta)) return true;
          }
      }
      return false;
  }

}
