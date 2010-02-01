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

class LocalVariableTableInfo extends AttributeInfo {
  int start_pc;
  int length;
  int nameIndex;
  int descriptorIndex;
  int index;

  LocalVariableTableInfo(ClassFileReader  reader) {
    start_pc = reader.read2Bytes();
    length = reader.read2Bytes();
    nameIndex = reader.read2Bytes();
    descriptorIndex = reader.read2Bytes();
    index = reader.read2Bytes();    
  }

  public void Print() {
    System.out.println(" startPC = " + start_pc +
		      " length = " + length +
		      " nameIndex = " + nameIndex +
		      " descriptorIndex = " + descriptorIndex +
		      " index = " + index);
  }

  String asString() {
    return new String(" startPC = " + start_pc +
		      " length = " + length +
		      " nameIndex = " + nameIndex +
		      " descriptorIndex = " + descriptorIndex +
		      " index = " + index);
  }


}
