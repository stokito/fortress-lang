
/*******************************************************************************
    Copyright 2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/
package com.sun.fortress.compiler.asmbytecodeoptimizer;

import java.io.*;
import java.util.ArrayList;
import java.util.jar.*;
import java.util.List;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;
import org.objectweb.asm.util.*;


public class ByteCodeAnnotationVisitor extends AbstractVisitor implements AnnotationVisitor {

    ByteCodeAnnotationVisitor(int foo) {
        super();
    }

    public void visit (String name, Object value) {
    }

    public void visitEnum(String name, String desc, String value) {
        
    }

    public void visitEnd() {

    }

    public AnnotationVisitor visitArray(String name) {
        return new EmptyVisitor();
    }

    public AnnotationVisitor visitAnnotation(String name, String desc) {
        return new EmptyVisitor();
    }
}
