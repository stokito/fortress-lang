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
import java.util.Vector;

class CodeAttributeInfo extends AttributeInfo {
    int maxStack;
    int maxLocals;
    int codeLength;
    int code[];
    int exceptionTableLength;
    String stackTypes[];
    String localTypes[];
    ExceptionTableInfo[] exceptions;
    int attributesCount;
    AttributeInfo attributes[];
    ClassToBeOptimized cls;
    CodeAttributeInfo(ClassToBeOptimized c, String name, int length) {
        attributeName = name;
        attributeLength = length;
        cls = c;
        maxStack =   cls.reader.read2Bytes();
        maxLocals =  cls.reader.read2Bytes();

        codeLength = cls.reader.read4Bytes();
        code = new int[codeLength];

        for (int i = 0; i < codeLength; i++) 
            code[i] = cls.reader.read1Byte();

        exceptionTableLength = cls.reader.read2Bytes();
        exceptions = new ExceptionTableInfo[exceptionTableLength];
        for (int i = 0; i < exceptionTableLength; i++)
            exceptions[i] = new ExceptionTableInfo(cls.reader);
        attributes = readAttributes(cls);
    }

    ExceptionTableInfo[] getExceptionHandlers(int pc) {
        Vector e = new Vector();
        for (int i = 0; i < exceptionTableLength; i++) 
            if ((exceptions[i].start_pc <= pc) &&
                (exceptions[i].end_pc >= pc))
                e.addElement(exceptions[i]);
        ExceptionTableInfo[] result = new ExceptionTableInfo[e.size()];
        e.copyInto(result);
        return result;
    }
  
    public void Print() {
        System.out.println("Code Attribute:");
        int pc = 0;
        while (pc < codeLength) {
            int opcode = code[pc];
            int len = util.getOpCodeLength(code, pc);
            System.out.print("pc = " + pc + ": opcode = " + RTC.opcNames[opcode]);
            pc++;
            for (int j = 1; j < len; j++) {
                System.out.print(" " + code[pc++]);
            }
            System.out.println("");
        }
        System.out.println("number start  end    handler");
        for (int exs = 0; exs < exceptionTableLength; exs++) {
            System.out.print(exs + "    ");
            exceptions[exs].Print();
        }
    }

    public void optimizedCode() {
        System.out.println("Code Attribute:");
        int pc = 0;
        while (pc < codeLength) {
            int opcode = code[pc];
            int len = util.getOpCodeLength(code, pc);
            System.out.print("pc = " + pc + ": opcode = " + RTC.opcNames[opcode]);
            pc++;
            for (int j = 1; j < len; j++) {
                System.out.print(" " + code[pc++]);
            }
            System.out.println("");
        }
        System.out.println("number start  end    handler");
        for (int exs = 0; exs < exceptionTableLength; exs++) {
            System.out.print(exs + "    ");
            exceptions[exs].Print();
        }

    }
}
