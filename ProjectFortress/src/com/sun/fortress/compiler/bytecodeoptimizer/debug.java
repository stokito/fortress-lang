/*******************************************************************************
    Copyright 2010 Sun Microsystems, Inc.,
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
package com.sun.fortress.compiler.bytecodeoptimizer;

class debug {
  
  static int printLevel = 3;

  debug(int i) {
    printLevel = i;
  }

  static void setPrintLevel(int i) {
    printLevel = i;
  }

  static void println(int level, String str) {
    if (printLevel >= level) {
      System.out.println(str);
    }
  }

  static void print(int level, String str) {
    if (printLevel >= level) {
      System.out.print(str);
    }
  }
}
