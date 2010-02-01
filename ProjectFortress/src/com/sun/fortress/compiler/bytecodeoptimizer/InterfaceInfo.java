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

class InterfaceInfo {
  int info;
  
  InterfaceInfo(ClassFileReader reader) {
    info = reader.read2Bytes();
  }

  static InterfaceInfo[] readInterfaces(ClassToBeOptimized cls) {
    int interfaceCount = cls.reader.read2Bytes();
    debug.println(2, "Interface count = " + interfaceCount);
    InterfaceInfo result[]  = new InterfaceInfo[interfaceCount];
    for (int i = 0; i < interfaceCount; i++)
      result[i] = new InterfaceInfo(cls.reader);
    return result;
  }

  static void printInterfaces(InterfaceInfo[] interfaces) {
    for (int i = 0; i < interfaces.length; i++)
      System.out.println("Interface(" + i + ") = " + interfaces[i].info);
  }
}
