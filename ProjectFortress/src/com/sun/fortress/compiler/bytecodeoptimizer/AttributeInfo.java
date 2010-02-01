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

class AttributeInfo {
  String attributeName;
  int attributeLength;

  public static AttributeInfo[] readAttributes(ClassToBeOptimized cls) {
    int count = cls.reader.read2Bytes();
    String name;
    AttributeInfo[] result = new AttributeInfo[count];    

    for (int i = 0; i < count; i++) {
      int nameIndex = cls.reader.read2Bytes();
      int length = cls.reader.read4Bytes();
      name = cls.cp[nameIndex].getUtf8String();
      if (name.equals("SourceFile"))
	result[i] = new SourceFileAttributeInfo(cls, name, length);
      else if (name.equals("Code")) 
	result[i] = new CodeAttributeInfo(cls, name, length);
      else if (name.equals("ConstantValue"))
	result[i] = new ConstantValueAttributeInfo(cls, name, length);
      else if (name.equals("Exceptions"))
	result[i] = new ExceptionAttributeInfo(cls, name, length);
      else if (name.equals("LineNumberTable"))
        result[i] = new LineNumberTableAttributeInfo(cls, name, length);
      else if (name.equals("LocalVariableTable"))
      	result[i] = new LocalVariableTableAttributeInfo(cls, name, length);
      else result[i] = new GenericAttributeInfo(cls, name, length);
    }
    return result;
  }

  public void Print() {
     System.out.println(attributeName+" Attribute (Length = "
     +attributeLength+")");
  }
  
  public static void printAttributes(AttributeInfo attributes[]) {
    for (int i = 0; i < attributes.length; i++) 
      attributes[i].Print();
  }
}
