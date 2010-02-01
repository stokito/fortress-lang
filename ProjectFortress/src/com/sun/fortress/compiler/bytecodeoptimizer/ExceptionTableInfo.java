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

class ExceptionTableInfo extends AttributeInfo {
  int start_pc;
  int end_pc;
  int handler_pc;
  int catch_type;

  ExceptionTableInfo(ClassFileReader  reader) {
    start_pc = reader.read2Bytes();
    end_pc =   reader.read2Bytes();
    handler_pc = reader.read2Bytes();
    catch_type = reader.read2Bytes();
  }

  public void Print() {
    System.out.println("     " + start_pc +
		       "     " + end_pc +
		       "     " + handler_pc);
  }

  String asString() {
    return new String(" startPC = " + start_pc +
		      " endPC = " + end_pc +
		      " handlerPC = " + handler_pc);
  }


}
