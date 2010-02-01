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

class GenericAttributeInfo extends AttributeInfo {
  int info[];

  GenericAttributeInfo(ClassToBeOptimized cls, String name, int length) {
    attributeName = name;
    attributeLength = length;
    info = new int[length];
    for (int i = 0; i < length; i++)
      info[i] = cls.reader.read1Byte();
  }

  public void Print() {
    System.out.println("Generic Attribute Info = ");
  }
}
    

