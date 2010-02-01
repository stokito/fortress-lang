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

// Added for PJVM_ClassLoader 
class Constant4ByteInfo extends ConstantPoolInfo {
  byte byte0;
  byte byte1;
  byte byte2;
  byte byte3;
  
  Constant4ByteInfo(ClassToBeOptimized cls) {
    byte3 = (byte)cls.reader.read1Byte();
    byte2 = (byte)cls.reader.read1Byte();
    byte1 = (byte)cls.reader.read1Byte();
    byte0 = (byte)cls.reader.read1Byte();
  }
}
// FY - 08/11/98

