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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * This code steals willy-nilly from the Nextgen class loader.
 * 
 * @author dr2chase
 */
public class InstantiatingClassloader extends ClassLoader  implements Opcodes {

    public final static InstantiatingClassloader ONLY = new InstantiatingClassloader(
            Thread.currentThread().getContextClassLoader());

    private final static ClassLoadChecker _classLoadChecker = new ClassLoadChecker();

    private InstantiatingClassloader() {
        // TODO Auto-generated constructor stub
    }

    private InstantiatingClassloader(ClassLoader parent) {
        super(parent);
        // TODO Auto-generated constructor stub
    }

    public static String dotToSlash(String s) {
        return s.replace('.', '/');
    }

    /**
     * Gets the bytes for a "resource". Resources can includes classfiles on the
     * classpath, which is handy.
     * 
     * (adapted from Nextgen)
     * 
     * @param className
     * @return
     * @throws IOException
     */
    private byte[] readResource(String className) throws IOException {
        // getResourceAsStream finds a file that's in the classpath. It's
        // generally
        // used to load resources (like images) from the same location as
        // class files. However for our purposes of loading the bytes of a class
        // file, this works perfectly. It will find the class in any place in
        // the classpath, and it doesn't force us to search the classpath
        // ourselves.
        String fileName = dotToSlash(className) + ".class";

        InputStream stream = new java.io.BufferedInputStream(
                getResourceAsStream(fileName));

        if (stream == null) {
            throw new IOException("Resource not found: " + fileName);
        }

        byte[] data = new byte[stream.available()];
        stream.read(data);

        return data;
    }

    protected byte[] getClass(String name) throws ClassNotFoundException,
            IOException {
        // We can load it ourselves. Let's get the bytes.
        byte[] classData;

        if (false) {
            // Here will go all the magic expando-stuff.
        } else {
            classData = readResource(name);
        }
        // delegates to superclass to define the class
        // System.out.println("Loading class:" + name);
        return classData;
    }

    protected Class loadClass(String name, boolean resolve)
            throws ClassNotFoundException {
        Class clazz;
        // First, we need to replace all occurrences of the System path
        // separator
        // with '.', to handle differences in package specifications. eallen
        // 10/1/2002
        //name = MainWrapper.sepToDot(name);
        
        /*
         * We want to actually load the class ourselves, if security allows us
         * to. This is so that the classloader associated with the class is
         * ours, not the system loader. But some classes (java.*, etc) must be
         * loaded with the system loader.
         */
        if (_classLoadChecker.mustUseSystemLoader(name)) {
            clazz = findSystemClass(name);
        } else {
            byte[] classData = null;
            try {
                if (name.contains(Naming.ENVELOPE)) {
                   
                    classData = instantiateClosure(name);
                } else if (isExpanded(name)) {
                    String dename = Naming.deMangle(name);
                    int left = dename.indexOf(Naming.LEFT_OXFORD);
                    int right = dename.lastIndexOf(Naming.RIGHT_OXFORD);
                    String stem = dename.substring(0,left);
                    ArrayList<String> parameters = extractStringParameters(
                            dename, left, right);
                    if (stem.equals("Arrow")) {
                        classData = instantiateArrow(name, parameters);
                    } else if (stem.equals("AbstractArrow")) {
                        classData = instantiateAbstractArrow(dename, parameters);
                    } else {
                        throw new ClassNotFoundException("Don't know how to instantiate generic " + stem + " of " + parameters);
                    }
                } else {
                    classData = getClass(name);
                }
                // System.err.println("trying to getClass("+name+")");
                clazz = defineClass(name, classData, 0, classData.length);
                System.err.println(clazz.getName());
            } catch (java.io.EOFException ioe) {
                // output error msg if this is a real problem
                ioe.printStackTrace();
                throw new ClassNotFoundException(
                        "IO Exception in reading class : " + name + " ", ioe);
            } catch (ClassFormatError ioe) {
                // output error msg if this is a real problem
                ioe.printStackTrace();
                throw new ClassNotFoundException(
                        "ClassFormatError in reading class file: ", ioe);
            } catch (IOException ioe) {
                // this incl FileNotFoundException which is used by resource
                // loader
                // dont print stack trace here
                // System.err.println("Got IO Exception reading class file");
                // ioe.printStackTrace();
                // throw new
                // ClassNotFoundException("IO Exception in reading class file: "
                // + ioe);
                throw new ClassNotFoundException(
                        "IO Exception in reading class file: ", ioe);
            }
        }

        if (clazz == null) {
            System.err.println(">>>>>>>>>>>>>> clazz null ");
            throw new ClassNotFoundException(name);
        }

        if (resolve) {
            resolveClass(clazz);
        }

        return clazz;
    }

    private byte[] instantiateClosure(String name) {
        int last_dot = name.lastIndexOf('.');
        String api = name.substring(0,last_dot);
        String suffix = Naming.deMangle(name.substring(last_dot+1));
        int env_loc = suffix.indexOf(Naming.ENVELOPE);
        String fn = suffix.substring(0,env_loc);
        String ft = suffix.substring(env_loc+1);
        
        int left = ft.indexOf(Naming.LEFT_OXFORD);
        int right = ft.lastIndexOf(Naming.RIGHT_OXFORD);
        String stem = ft.substring(0,left);
        ArrayList<String> parameters = extractStringParameters(
                ft, left, right);
        
        /*
         * Recipe:
         * need to emit class "name".
         * It needs to extend AbstractArrow[\parameters\]
         * It needs to contain
         *   RT apply (params_except_last) {
         *     return api.fn(params_except_last);
         *   }
         */
        
        
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS|ClassWriter.COMPUTE_FRAMES);
        FieldVisitor fv;
        MethodVisitor mv;
        AnnotationVisitor av0;
        String superClass = Naming.mangleIdentifier("Abstract"+ft);
        name = name.replaceAll("[.]", "/");
        String desc = "L" + name + ";";
        String field_desc = "L" + Naming.mangleIdentifier(ft) + ";";
        cw.visit(V1_6, ACC_PUBLIC + ACC_SUPER, name, null, superClass, null);

        {
            fv = cw.visitField(ACC_PUBLIC + ACC_FINAL + ACC_STATIC, "closure", field_desc, null, null);
            fv.visitEnd();
            
        }

        {
            mv = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
            mv.visitCode();
            mv.visitTypeInsn(NEW, name);
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, name, "<init>", "()V");
            mv.visitFieldInsn(PUTSTATIC, name, "closure", field_desc);
            mv.visitInsn(RETURN);
            mv.visitMaxs(2, 0);
            mv.visitEnd();
            }
        
        
        {
            mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            // Supertype is mangle("Abstract"+ft)
            mv.visitMethodInsn(INVOKESPECIAL, superClass, "<init>", "()V");
            mv.visitInsn(RETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        {
            String sig = arrowParamsToJVMsig(parameters);
            // What if staticClass is compiler builtin?  How do we know?
            String staticClass = api.replaceAll("[.]", "/");
            
            mv = cw.visitMethod(ACC_PUBLIC, "apply", sig, null, null);
            mv.visitCode();
            for (int i = 1; i < parameters.size();i++) {
                String param = parameters.get(i-1);
                mv.visitVarInsn(ALOAD, i);
                
            }
            mv.visitMethodInsn(INVOKESTATIC, staticClass, fn, sig);
            // ARETURN if not void...
            mv.visitInsn(ARETURN);
            
            mv.visitMaxs(2, 3);
            mv.visitEnd();
        }
        cw.visitEnd();
        
        return cw.toByteArray();
        
    }

    /**
     * @param s
     * @param leftBracket
     * @param rightBracket
     * @return
     */
    private ArrayList<String> extractStringParameters(String s,
            int leftBracket, int rightBracket) {
        ArrayList<String> parameters = new ArrayList<String>();
        int depth = 1;
        int pbegin = leftBracket+1;
        for (int i = leftBracket+1; i <= rightBracket; i++) {
            char ch = s.charAt(i);
            
            if ((ch == ';' || ch == Naming.RIGHT_OXFORD_CHAR) && depth == 1) {
                String parameter = s.substring(pbegin,i);
                parameters.add(parameter);
                pbegin = i+1;
            } else {
                if (ch == Naming.LEFT_OXFORD_CHAR) {
                    depth++;
                } else if (ch == Naming.RIGHT_OXFORD_CHAR) {
                    depth--;
                } else {

                }
            }
        }
        return parameters;
    }
    
    private byte[] instantiateArrow(String name, ArrayList<String> parameters) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS|ClassWriter.COMPUTE_FRAMES);
        FieldVisitor fv;
        MethodVisitor mv;
        AnnotationVisitor av0;

        cw.visit(Opcodes.V1_6, ACC_PUBLIC + ACC_ABSTRACT + ACC_INTERFACE,
                name, null, "java/lang/Object", null);

        String sig = arrowParamsToJVMsig(parameters);
        
        {
        mv = cw.visitMethod(ACC_PUBLIC + ACC_ABSTRACT, "apply",
                sig,
                null, null);
        mv.visitEnd();
        }
        cw.visitEnd();

        return cw.toByteArray();
    }

    private byte[] instantiateAbstractArrow(String dename, ArrayList<String> parameters) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS|ClassWriter.COMPUTE_FRAMES);
        FieldVisitor fv;
        MethodVisitor mv;
        AnnotationVisitor av0;
        
        String name = Naming.mangleIdentifier(dename);
        String if_name =
            Naming.mangleIdentifier("Arrow" + dename.substring("AbstractArrow".length()));
        
        cw.visit(V1_6, ACC_PUBLIC + ACC_SUPER + ACC_ABSTRACT, name, null,
                "java/lang/Object", new String[] { if_name });

        
        {
            mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
            mv.visitInsn(RETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        
        String sig = arrowParamsToJVMsig(parameters);
        
        {
        mv = cw.visitMethod(ACC_PUBLIC + ACC_ABSTRACT, "apply",
                sig,
                null, null);
        mv.visitEnd();
        }
        cw.visitEnd();

        return cw.toByteArray();
    }

    /**
     * @param parameters
     * @return
     */
    private String arrowParamsToJVMsig(ArrayList<String> parameters) {
        String sig = "(";
        
        int l = parameters.size();
        
        for (int i = 0; i < l-1; i++) {
            sig += Naming.javaDescForTaggedFortressType(parameters.get(i));
        }
        sig += ")";
        // nothing special here, yet, but AbstractArrow will be different.
        String rt = parameters.get(l-1);
        sig += Naming.javaDescForTaggedFortressType(rt);
        return sig;
    }

    static boolean isExpanded(String className) {
        int left = className.indexOf(Naming.LEFT_OXFORD);
        int right = className.indexOf(Naming.RIGHT_OXFORD);
        return (left != -1 && right != -1 && left < right);
    }
}

/** Figures out whether a class can be loaded by a custom class loader or not. */
class ClassLoadChecker {
    private final SecurityManager _security = System.getSecurityManager();

    /**
     * Map of package name (string) to whether must use system loader (boolean).
     */
    private Hashtable<String, Boolean> _checkedPackages = new Hashtable<String, Boolean>();

    public boolean mustUseSystemLoader(String name) {
        // If name begins with java., must use System loader. This
        // is regardless of the security manager.
        // javax. too, though this is not documented
        if (name.startsWith("java.") || name.startsWith("javax.")
                || name.startsWith("sun.") || name.startsWith("com.sun.")) {
            return true;
        }

        // No security manager? We can do whatever we want!
        if (_security == null) {
            return false;
        }

        int lastDot = name.lastIndexOf('.');
        String packageName;
        if (lastDot == -1) {
            packageName = "";
        } else {
            packageName = name.substring(0, lastDot);
        }

        // Check the cache first
        Object cacheCheck = _checkedPackages.get(packageName);
        if (cacheCheck != null) {
            return ((Boolean) cacheCheck).booleanValue();
        }

        // Now try to get the package info. If it fails, it's a system class.
        try {
            _security.checkPackageDefinition(packageName);
            // Succeeded, so does not require system loader.
            _checkedPackages.put(packageName, Boolean.FALSE);
            return false;
        } catch (SecurityException se) {
            // Failed, so does require system loader.
            _checkedPackages.put(packageName, Boolean.TRUE);
            return true;
        }
    }
}
