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

import java.util.Stack;

class MethodInfo {
    int accessFlags;
    int nameIndex;
    int descriptorIndex;
    AttributeInfo attributes[];
    ClassToBeOptimized cls;

    MethodInfo(ClassToBeOptimized cl) {
        cls = cl;
        accessFlags =     cls.reader.read2Bytes();
        nameIndex =       cls.reader.read2Bytes();
        descriptorIndex = cls.reader.read2Bytes();
        attributes = AttributeInfo.readAttributes(cls);
    }

    public void Print() {
        System.out.println("MethodInfo: ");
        System.out.println("  accessFlags = " + accessFlags);
        System.out.println("  nameIndex = " + nameIndex);
        System.out.println("  descriptorIndex = " + descriptorIndex);
        AttributeInfo.printAttributes(attributes);
    }

    public void SetUpStaticMethod(BasicBlock bb) throws Exception {
        String methodName = cls.cp[nameIndex].getUtf8String();
        String descriptor = cls.cp[descriptorIndex].getUtf8String();
        TypeSig sig = new TypeSig(descriptor);
        debug.println(2,"");
        debug.println(2,"Verifying static method " + 
                      methodName + " " + 
                      sig.asString());

        for (int j = 0; j < sig.argTypes.length; j++)
            bb.context.l.store(sig.argTypes[j], j);
    }

    public void SetUpNonStaticMethod(BasicBlock bb) 
        throws Exception {
        String methodName = cls.cp[nameIndex].getUtf8String();
        String descriptor = cls.cp[descriptorIndex].getUtf8String();
        TypeSig sig = new TypeSig(descriptor);
        debug.println(2,"");
        debug.println(2,"Verifying nonstatic method " + 
                      methodName + " " + 
                      sig.asString());

        TypeState ts = TypeState.typeRef(cls.name, true);
        bb.context.l.store(ts,0);
        for (int j = 0; j < sig.argTypes.length; j++)
            bb.context.l.store(sig.argTypes[j], j+1);
    }    

    public void SetupLocals(BasicBlock bb) throws Exception {
        if ((accessFlags & RTC.ACC_STATIC) != 0)
            SetUpStaticMethod(bb);
        else SetUpNonStaticMethod(bb);
    }
  
    public void VerifyCodeAttribute(CodeAttributeInfo info) throws Exception {
        Stack bbs = new Stack();
        BBVector BBs = new BBVector(info);
        BasicBlock firstBB = BBs.getFirst();
        SetupLocals(firstBB);
        bbs.push(firstBB);
        while (bbs.empty() == false) {
            BasicBlock currentBB = (BasicBlock) bbs.pop();
            aiContext c = new aiContext(currentBB.context);
            debug.println(2,"");       debug.println(2,"");
            debug.println(2,"Interpreting BB " + currentBB.startPC);
            c.print();
            c.pc = currentBB.startPC;
            currentBB.visited = true;
            while (c.pc <= currentBB.endPC) 
                ops.Interpret(c);
      
            debug.println(2,"");      
            for (int i = 0; i < currentBB.succ.length; i++) {
                BasicBlock succ = currentBB.succ[i];

                if (succ.visited == true) {
                    debug.println(2,"Merging context " + currentBB.startPC + 
                                  " with successor " + succ.startPC);

                    if (c.Merge(succ.context, succ.startPC)) {
                        succ.context.print();
                        bbs.push(succ);
                    }
                } else {
                    succ.context = new aiContext(c);
                    succ.visited = true;
                    bbs.push(succ);
                }
            }

            for (int i = 0; i < currentBB.excs.length; i++) {
                BasicBlock ex = currentBB.excs[i];
                c.s.clear();
                c.s.push(TypeState.typeRef("java/lang/Object", true));
                if (ex.visited == true) {
                    debug.println(2,"");
                    debug.println(2,"Merging context " + currentBB.startPC + 
                                  " with exception " + ex.startPC);
                    if (c.Merge(ex.context, ex.startPC)) {
                        ex.context.print();
                        bbs.push(ex);
                    }
                }
                else {
                    ex.context = new aiContext(c);
                    ex.visited = true;
                    bbs.push(ex);
                }
            }
        }
    }

    public void OptimizeCodeAttribute(CodeAttributeInfo info) {
       info.optimizedCode();         
    }

    public void Verify() throws Exception {
        for (int i = 0; i < attributes.length; i++) 
            if (attributes[i] instanceof CodeAttributeInfo) 
                VerifyCodeAttribute((CodeAttributeInfo) attributes[i]);
    }

    public void Optimize() {
        for (int i = 0; i < attributes.length; i++) 
            if (attributes[i] instanceof CodeAttributeInfo) 
                OptimizeCodeAttribute((CodeAttributeInfo) attributes[i]);
    }

    public static MethodInfo[] readMethods(ClassToBeOptimized cls) {
        int count = cls.reader.read2Bytes();
        debug.println(2,"Method count = " + count);
        MethodInfo[] result = new MethodInfo[count];
        for (int i = 0; i < count; i++)
            result[i] = new MethodInfo(cls);
        return result;
    }

    public static void printMethods(MethodInfo methods[]) {
        for (int i = 0; i<methods.length; i++)
            methods[i].Print();
    }
}
