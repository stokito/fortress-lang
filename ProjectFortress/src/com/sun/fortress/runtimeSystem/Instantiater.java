/*******************************************************************************
    Copyright 2009 Sun Microsystems, Inc.,
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
package com.sun.fortress.runtimeSystem;

import java.util.Map;
import java.util.regex.Pattern;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

import com.sun.fortress.useful.StringMap;
import com.sun.fortress.useful.Useful;

public class Instantiater extends ClassAdapter {

    public static class InstantiationMap extends StringMap.FromMap {

        public final static Pattern envVar = Pattern.compile("["+ 
                Naming.GENERIC_TAGS +
                "][0-9]+");

        final static int INTRO_LEN = 0;

        final static int OUTRO_LEN = 0;

        InstantiationMap(Map<String, String> p) {
            super(p);
            // TODO Auto-generated constructor stub
        }
        
        
        public String getCompletely(String s) {
            // TODO will need to rewrite into type, desc, and method variants.
            if (s == null) return s;
            
            s = Naming.demangleFortressIdentifier(s);
            
            s =  getCompletely(s, 1000);
            
            // Don't remangle; the writer will do that for us.
            // s = Naming.mangleFortressIdentifier(s);
            
            return s;
        }
        public String getDesc(String s) {
            // TODO will need to rewrite into type, desc, and method variants.
            if (s == null) return s;
            
            s = Naming.demangleFortressDescriptor(s);
            
            s =  getCompletely(s, 1000);
            
            // Don't remangle; the writer will do that for us.
            // s = Naming.mangleFortressIdentifier(s);
            
            return s;
        }
        public String getCompletely(String s, int limit) {
            
            return Useful.substituteVarsCompletely(s, this, limit, envVar, INTRO_LEN, OUTRO_LEN);
        }
        
        public String repairMangle(String s, int start, int end) {
            StringBuffer b = new StringBuffer();
            b.append(s.substring(0,start));
            int olevel = 0;
            for (int i = start; i < end; i++) {
                char c = s.charAt(i);
                
                if (c == ';') {
                    mangle(b, olevel, '?', ';');
                } else if (c == '$') {
                    mangle(b, olevel, '%', '$');
                } else if (c == '.') {
                    mangle(b, olevel, ',', '.');
                } else {
                    if (c == Naming.LEFT_OXFORD_CHAR) {
                        olevel++;
                    } else if (c == Naming.RIGHT_OXFORD_CHAR) {
                        olevel--;
                    }
                    b.append(c);
                }
            }
            b.append(s.substring(end));
            return b.toString();
        }

        private void mangle(StringBuffer b, int olevel, char c, char c_if_zero) {
            if (olevel == 0)
                b.append(c_if_zero);
            else {
                b.append('\\');
                while (olevel-- > 1)
                    b.append('-');
                b.append(c);
            }
        }
        
    }
    
    InstantiationMap xlation;
    String instanceName;
    
    public Instantiater(ClassVisitor cv, Map xlation, String instanceName) {
        super(cv);
        this.xlation = new InstantiationMap(xlation);
        this.instanceName = instanceName;
    }

    
    @Override
    public void visit(int version, int access, String name, String signature,
            String superName, String[] interfaces) {
        // TODO Auto-generated method stub
        String[] new_interfaces = new String[interfaces.length];
        for (int i = 0; i < interfaces.length; i++) {
            new_interfaces[i] = xlation.getCompletely(interfaces[i]);
        }
        super.visit(version, access, instanceName, signature,
                xlation.getCompletely(superName), new_interfaces);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        // TODO Auto-generated method stub
        return super.visitAnnotation(desc, visible);
    }

    @Override
    public void visitAttribute(Attribute attr) {
        // TODO Auto-generated method stub
        super.visitAttribute(attr);
    }

    @Override
    public void visitEnd() {
        // TODO Auto-generated method stub
        super.visitEnd();
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc,
            String signature, Object value) {
        // TODO Auto-generated method stub
        desc = xlation.getDesc(desc);
        return super.visitField(access, name, desc, signature, value);
    }

    @Override
    public void visitInnerClass(String name, String outerName,
            String innerName, int access) {
        // TODO Auto-generated method stub
        super.visitInnerClass(name, outerName, innerName, access);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc,
            String signature, String[] exceptions) {
        MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);

        return new MethodInstantiater(mv, xlation);
    }

    @Override
    public void visitOuterClass(String owner, String name, String desc) {
        // TODO Auto-generated method stub
        super.visitOuterClass(owner, name, desc);
    }

    @Override
    public void visitSource(String source, String debug) {
        // TODO Auto-generated method stub
        super.visitSource(source, debug);
    }


}