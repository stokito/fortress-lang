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

class BasicBlock {
  boolean visited;
  BasicBlock succ[];
  BasicBlock excs[];
  aiContext context;
  int startPC;
  int endPC;

  BasicBlock(CodeAttributeInfo c, int start, int end) {
    visited = false;
    context = new aiContext(c);
    succ = new BasicBlock[0];
    excs = new BasicBlock[0];
    startPC = start;
    endPC = end;
  }

  void addSucc(BasicBlock bb) {
    int len = succ.length;
    BasicBlock newsucc[] = new BasicBlock[len+1];
    for (int i = 0; i < len; i++)
      newsucc[i] = succ[i];
    newsucc[len] = bb;
    succ = newsucc;
  }

  void addException(BasicBlock bb) {
    int len = excs.length;
    BasicBlock newexcs[] = new BasicBlock[len+1];
    for (int i = 0; i < len; i++)
      newexcs[i] = excs[i];
    newexcs[len] = bb;
    excs = newexcs;
  }

  String asString() {
    String pstr = new String("start = " + startPC + " end = " + endPC); 
    if (succ.length != 0) {
      pstr = pstr + " successors = ";
      for (int i = 0; i < succ.length; i++) 
	pstr = pstr + succ[i].startPC + " ";
    }
    if (excs.length != 0) {
      pstr = pstr + " exceptions = ";
      for (int i = 0; i < excs.length; i++)
	pstr = pstr + excs[i].startPC + " ";
    }
    return pstr;
  }
}


