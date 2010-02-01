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

class FieldInfo {
  int accessFlags;
  int nameIndex;
  int descriptorIndex;
  AttributeInfo attributes[];

  FieldInfo(ClassToBeOptimized cls) {
    accessFlags =     cls.reader.read2Bytes();
    nameIndex =       cls.reader.read2Bytes();
    descriptorIndex = cls.reader.read2Bytes();
    attributes = AttributeInfo.readAttributes(cls);
  }
  
  public void Print() {
    System.out.println("FieldInfo: ");
    System.out.println("accessFlags = " + accessFlags);
    System.out.println("nameIndex = " + nameIndex);
    System.out.println("descriptorIndex = " + descriptorIndex);
    AttributeInfo.printAttributes(attributes);
    System.out.println("End FieldInfo ");
  }

  public static void printFields(FieldInfo fields[]) {
    for (int i = 0; i<fields.length; i++)
      fields[i].Print();
  }

  public static FieldInfo[] readFields(ClassToBeOptimized cls) {
    int count = cls.reader.read2Bytes();
    debug.println(2,"Field count = " + count);
    FieldInfo[] result = new FieldInfo[count];
    for (int i = 0; i < count; i++)
      result[i] = new FieldInfo(cls);
    return result;
  }
}
