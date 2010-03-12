
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