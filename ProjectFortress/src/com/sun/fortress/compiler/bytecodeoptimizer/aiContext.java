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

class aiContext {
  stack s;
  locals l;
  int Code[];
  ClassToBeOptimized cls;
  int pc;
  CodeAttributeInfo codeAttribute;

  boolean Merge(aiContext aic, int stpc) {
    return s.Merge(aic.s, stpc) | l.Merge(aic.l, stpc);
  }

  void incpc() {pc = pc + util.getOpCodeLength(Code, pc);}

  aiContext(CodeAttributeInfo c) {
    codeAttribute = c;
    s = new stack(c.maxStack);
    l = new locals(c.maxLocals);
    cls = c.cls;
    Code = c.code;
    pc = 0;
  }

  aiContext(aiContext aic) {
    s = new stack(aic.s);
    l = new locals(aic.l);
    Code = aic.Code;
    cls = aic.cls;
    pc = 0;
    codeAttribute = aic.codeAttribute;
  }

  void print() {
    s.print();
    l.print();
  }
}
