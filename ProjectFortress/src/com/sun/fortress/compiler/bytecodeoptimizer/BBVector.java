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
import java.util.*;

class BBVector {
  Vector BBs;

  BasicBlock getFirst() {
    return (BasicBlock) BBs.elementAt(0);
  }

  // Walk the opCodes marking any opCode which must be the
  // beginning of a basic block.

  BitSet getLeaders(CodeAttributeInfo c) {
    BitSet leaders = new BitSet(c.codeLength);
    int i = 0;

    while (i < c.codeLength) {
      // Mark the succeeding instructions
      int succs[] = getBBStarts(i,c);
      for (int j = 0; j < succs.length; j++) 
	leaders.set(succs[j]);

      // Mark the exception handlers which are active 
      ExceptionTableInfo excs[] = c.getExceptionHandlers(i);
      for (int j = 0; j < excs.length; j++) 
	leaders.set(excs[j].handler_pc);
	
      // If there were any exceptions then the next opCode
      // must be a start of a basic block.

      if (excs.length != 0) 
	leaders.set(i + util.getOpCodeLength(c.code, i));

      i = i + util.getOpCodeLength(c.code, i);
    }
    return leaders;
  }

  void getBBs(CodeAttributeInfo c) {
    BitSet leaders = getLeaders(c);
    BBs = new Vector();
    int start = 0;
    int end = 0;
    for (int i = 1; i < c.codeLength; i++) {
      if (leaders.get(i)) {
	end = i - 1;
	BBs.addElement(new BasicBlock(c, start, end));
	start = i;
      }
    }
    BBs.addElement(new BasicBlock(c, start, c.codeLength-1));
  }

  BBVector(CodeAttributeInfo c) {
    getBBs(c);

    for (int i = 0; i < BBs.size(); i++) {
      BasicBlock BB = (BasicBlock) BBs.elementAt(i);

      int pc = BB.startPC;
      int last = pc;
      while (pc <= BB.endPC) {
	last = pc;
	pc = pc + util.getOpCodeLength(c.code, pc);
      }
      
      int succs[] = getSuccessors(last, c);
      ExceptionTableInfo excs[] = c.getExceptionHandlers(last);

      if ((succs == null) && (excs == null))
	BB.addSucc(findElement(BB.endPC+1));

      if (succs != null) 
	for (int j = 0;  j < succs.length; j++) 
	  BB.addSucc(findElement(succs[j]));

      if (excs != null)
	for (int j = 0; j < excs.length; j++) 
	  BB.addException(findElement(excs[j].handler_pc));
    }
    printBasicBlocks();
  }

  BasicBlock findElement(int i) {
    for (int j = 0; j < BBs.size(); j++) {
      BasicBlock bb = (BasicBlock) BBs.elementAt(j);
      if (bb.startPC == i)
	return bb;
    }
    return null;
  }

  void printBasicBlocks() {
    Enumeration e = BBs.elements();
    while (e.hasMoreElements()) {
      BasicBlock b = (BasicBlock) e.nextElement();
      debug.println(2,b.asString());
    }
  }

  static int[] getBBStarts(int pc, CodeAttributeInfo c) {
    int result[] = new int[0];
    switch(c.code[pc]) {
    case RTC.opc_ifeq: 
    case RTC.opc_ifne: 
    case RTC.opc_iflt: 
    case RTC.opc_ifge: 
    case RTC.opc_ifgt: 
    case RTC.opc_ifle:  
    case RTC.opc_if_icmpeq: 
    case RTC.opc_if_icmpne: 
    case RTC.opc_if_icmplt:
    case RTC.opc_if_icmpge:
    case RTC.opc_if_icmpgt: 
    case RTC.opc_if_icmple: 
    case RTC.opc_if_acmpeq: 
    case RTC.opc_if_acmpne: 
    case RTC.opc_ifnull: 
    case RTC.opc_ifnonnull:
    case RTC.opc_goto:
    case RTC.opc_jsr: {
      int next = pc + util.getOpCodeLength(c.code, pc);
      if (next < c.codeLength) {
	result = new int[2];
	result[0] = next;
	result[1] = pc + util.getSigned16BitValue(c.code, pc+1);
      } else {
	result = new int[1];
	result[0] = pc + util.getSigned16BitValue(c.code, pc+1);
      }
      break;
    }
    case RTC.opc_athrow:
    case RTC.opc_ret: 
    case RTC.opc_ireturn:
    case RTC.opc_lreturn:
    case RTC.opc_freturn:
    case RTC.opc_dreturn:
    case RTC.opc_areturn:
    case RTC.opc_return: {
      int next = pc + util.getOpCodeLength(c.code, pc);
      if (next < c.codeLength) {
	result = new int[1];
	result[0] = next;
      }
      break;
    }
    case RTC.opc_goto_w: 
    case RTC.opc_jsr_w: {
      int next = pc + util.getOpCodeLength(c.code, pc);
      if (next < c.codeLength) {
	result = new int[2];
	result[0] = next;
	result[1] = pc + util.getSigned16BitValue(c.code, pc+1);
      } else {
	result = new int[1];
	result[0] = pc + util.getSigned16BitValue(c.code, pc+1);
      }
      break;
    }
    case RTC.opc_tableswitch: {
      int start = 4 * ((int) Math.ceil((pc + 1) / 4.0));
      int defaultValue = util.get32BitValue(c.code, start);
      int low =     util.get32BitValue(c.code, start + 4);
      int high =     util.get32BitValue(c.code, start + 8);
      start = start + 12;
      int stmts = high - low + 1;
      result = new int[stmts + 1];
      for (int i = 0; i < stmts; i++) 
	result[i] = pc + util.get32BitValue(c.code, start + i * 4);
      result[stmts] = pc + defaultValue;
      break;
    }
    case RTC.opc_lookupswitch: {
      int start = 4 * ((int) Math.ceil((pc + 1) / 4.0));
      int defaultValue = util.get32BitValue(c.code, start);
      int npairs = util.get32BitValue(c.code, start + 4);
      start = start + 8;
      result = new int[npairs + 1];
      for (int i = 0; i < npairs; i++) 
	result[i] = pc + util.get32BitValue(c.code, 8*i + start + 4);
      result[npairs] = pc + defaultValue;
      break;
    }
    }
    return result;
  }

  static int[] getSuccessors(int pc, CodeAttributeInfo c) {
    int result[] = new int[0];
    switch(c.code[pc]) {
    case RTC.opc_ifeq: 
    case RTC.opc_ifne: 
    case RTC.opc_iflt: 
    case RTC.opc_ifge: 
    case RTC.opc_ifgt: 
    case RTC.opc_ifle:  
    case RTC.opc_if_icmpeq: 
    case RTC.opc_if_icmpne: 
    case RTC.opc_if_icmplt:
    case RTC.opc_if_icmpge:
    case RTC.opc_if_icmpgt: 
    case RTC.opc_if_icmple: 
    case RTC.opc_if_acmpeq: 
    case RTC.opc_if_acmpne: 
    case RTC.opc_ifnull: 
    case RTC.opc_ifnonnull: {
      int next = pc + util.getOpCodeLength(c.code, pc);
      if (next < c.codeLength) {
	result = new int[2];
	result[0] = next;
	result[1] = pc + util.getSigned16BitValue(c.code, pc+1);
      } else {
	result = new int[1];
	result[0] = pc + util.getSigned16BitValue(c.code, pc+1);
      }
      break;
    }
    case RTC.opc_goto:
    case RTC.opc_jsr: {
      result = new int[1];
      result[0] = pc + util.getSigned16BitValue(c.code, pc+1);
      break;
    }
    case RTC.opc_athrow:
    case RTC.opc_ret: 
    case RTC.opc_ireturn:
    case RTC.opc_lreturn:
    case RTC.opc_freturn:
    case RTC.opc_dreturn:
    case RTC.opc_areturn:
    case RTC.opc_return: break;
    case RTC.opc_goto_w: 
    case RTC.opc_jsr_w: {
      result = new int[1];
      result[0] = util.get32BitValue(c.code, pc+1);
      break;
    }

    case RTC.opc_tableswitch: {
      int start = 4 * ((int) Math.ceil((pc + 1) / 4.0));
      int defaultValue = util.get32BitValue(c.code, start);
      int low =     util.get32BitValue(c.code, start + 4);
      int high =     util.get32BitValue(c.code, start + 8);
      start = start + 12;
      int stmts = high - low + 1;
      result = new int[stmts + 1];
      for (int i = 0; i < stmts; i++) {
	result[i] = pc + util.get32BitValue(c.code, start + i * 4);
      }
      result[stmts] = pc + defaultValue;
      break;
    }
      
    case RTC.opc_lookupswitch: {
      int start = 4 * ((int) Math.ceil((pc + 1) / 4.0));
      int defaultValue = util.get32BitValue(c.code, start);
      int npairs = util.get32BitValue(c.code, start + 4);
      start = start + 8;
     result = new int[npairs + 1];
      for (int i = 0; i < npairs; i++) 
	result[i] = pc + util.get32BitValue(c.code, 8*i + start + 4);
      result[npairs] = pc + defaultValue;
      break;
    }
    
    default: { 
      int next = pc + util.getOpCodeLength(c.code,pc);
      if (next < c.codeLength) {
	result = new int[1];
	result[0] = next;
      }
      else result = new int[0];
    }
    }
    return result;
  }
}
  
