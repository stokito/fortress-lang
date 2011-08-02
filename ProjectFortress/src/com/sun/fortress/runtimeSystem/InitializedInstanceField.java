/*
 * Created on Aug 1, 2011
 *
 */
package com.sun.fortress.runtimeSystem;

import org.objectweb.asm.MethodVisitor;

abstract public class InitializedInstanceField {
    abstract public void forInit(MethodVisitor mv);

    abstract public String asmName();

    abstract public String asmSignature();
}