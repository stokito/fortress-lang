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

class util {
  static int get32BitValue(int code[], int start) {
    byte b1 = (byte) code[start];
    byte b2 = (byte) code[start+1];
    byte b3 = (byte) code[start+2];
    byte b4 = (byte) code[start+3];
    return (b1<<24) | 
      ((b2<<16) & 0x00FF0000) | 
      ((b3<<8) & 0x0000FF00) |
      (b4 & 0x000000FF);
  }

  static int get16BitValue(int code[], int start) {
    byte b1 = (byte) code[start];
    byte b2 = (byte) code[start+1];
    int i = 0x0000FFFF & (b1 << 8) | (b2 & 0x000000FF);
    return i;
  }

  static int getSigned16BitValue(int code[], int start) {
    byte b1 = (byte) code[start];
    byte b2 = (byte) code[start+1];
    return ((b1 << 8) | (b2 & 0X000000FF));
  }

  static int get8BitValue(int code[], int start) {
    byte b = (byte) code[start];
    int i = 0x000000FF & b;
    return i;
  }

  static int tableSwitchLength(int code[], int pc) {
    int padding = 4 * ((int) Math.ceil((pc + 1) / 4.0)) - pc;
    int low =     get32BitValue(code, pc + padding + 4);
    int high =    get32BitValue(code, pc + padding + 8);
    return (high - low + 1) * 4 + 12 + padding;    
  }

  static int lookupSwitchLength(int code[], int pc) {
    int padding = 4 * ((int) Math.ceil((pc + 1) / 4.0)) - pc;      
    int npairs = get32BitValue(code, pc + padding + 4);
    return npairs * 8 + 9 + padding;
  }

  static int wideLength(int code[], int pc) {
    switch(code[pc+1]) {
    case RTC.opc_iload:
    case RTC.opc_fload:
    case RTC.opc_aload:
    case RTC.opc_lload:
    case RTC.opc_dload:
    case RTC.opc_istore:
    case RTC.opc_fstore:
    case RTC.opc_astore:
    case RTC.opc_lstore:
    case RTC.opc_dstore:
    case RTC.opc_ret: return 4;
    case RTC.opc_iinc: return 6;
    default: return 0;
    }
  }

  static int getOpCodeLength(int code[], int pc) {
    switch(code[pc]) {
    case RTC.opc_tableswitch: return tableSwitchLength(code, pc);
    case RTC.opc_lookupswitch: return lookupSwitchLength(code, pc);
    case RTC.opc_wide: return wideLength(code, pc);
    default: return RTC.opcLengths[code[pc]];
    }
  }
}
