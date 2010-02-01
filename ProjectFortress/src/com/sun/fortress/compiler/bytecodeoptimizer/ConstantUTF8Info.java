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

class ConstantUTF8Info extends ConstantPoolInfo {
  String ConstantString;
  
  ConstantUTF8Info(ClassToBeOptimized cls) {
    int len = cls.reader.read2Bytes();
    char chars[] = new char[len];

    for (int i = 0; i < len; i++)
      chars[i] = (char) cls.reader.read1Byte();
      
    ConstantString = new String(chars);
  }
  void print(ClassToBeOptimized cls) {
    System.out.println("CONSTANT_UTF8 = " + ConstantString); 
  }
}

