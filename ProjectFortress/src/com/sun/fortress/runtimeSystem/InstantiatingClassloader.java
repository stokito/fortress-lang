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
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.TraceClassVisitor;

import com.sun.fortress.compiler.codegen.ManglingClassWriter;
import com.sun.fortress.repository.ProjectProperties;

/**
 * This code steals willy-nilly from the NextGen class loader.
 *
 * @author dr2chase
 */
public class InstantiatingClassloader extends ClassLoader implements Opcodes {

    // TODO make this depends on properties/env w/o dragging in all of the world.
    private static final boolean LOG_LOADS = false;
    private static final boolean LOG_FUNCTION_EXPANSION = false;

    public final static InstantiatingClassloader ONLY =
        new InstantiatingClassloader(Thread.currentThread().getContextClassLoader());

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
     * Gets the bytes for a "resource". Resources can include classfiles on the
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
        // generally used to load resources (like images) from the same location as
        // class files. However for our purposes of loading the bytes of a class
        // file, this works perfectly. It will find the class in any place in
        // the classpath, and it doesn't force us to search the classpath
        // ourselves.
        String fileName = dotToSlash(className) + ".class";

        InputStream origStream = getResourceAsStream(fileName);
        if (origStream == null) {
            throw new IOException("Resource not found: " + fileName);
        }

        InputStream stream = new java.io.BufferedInputStream(origStream);

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
        // Verify suspicious class isn't being loaded.
        // if (("com.sun.fortress.nativeHelpers.LocalRandom").equals(name)) {
        //     throw new Error("HEY!!! LOADING LocalRandom!!!!!");
        // }
        if (_classLoadChecker.mustUseSystemLoader(name)) {
            clazz = findSystemClass(name);
        } else {
            byte[] classData = null;
            try {
                boolean isClosure = name.contains(Naming.ENVELOPE);
                boolean isGeneric = isExpanded(name);
                boolean isGenericFunction = name.contains(Naming.GEAR);
                //                if (isClosure && isGeneric) {
                //                    // A generic function, or so we think.
                //                    throw new ClassNotFoundException("Not yet handling generic functions " + name);
                //                } else

                if (isGenericFunction) {
                    // also a closure
                    HashMap<String, String> xlation = new HashMap<String, String>();
                    String dename = Naming.demangleFortressIdentifier(name);

                    String template_name = functionTemplateName(dename, xlation);
                    byte[] templateClassData = readResource(template_name);
                    ManglingClassWriter cw = new ManglingClassWriter(ClassWriter.COMPUTE_MAXS|ClassWriter.COMPUTE_FRAMES);
                    ClassReader cr = new ClassReader(templateClassData);
                    ClassVisitor cvcw = LOG_FUNCTION_EXPANSION ?
                        new TraceClassVisitor((ClassVisitor) cw, new PrintWriter(System.err)) :
                            cw;
                    Instantiater instantiater = new Instantiater(cvcw, xlation, dename);
                    cr.accept(instantiater, 0);
                    classData = cw.toByteArray();
                } else if (isClosure) {
                    classData = instantiateClosure(Naming.demangleFortressIdentifier(name));
                } else if (isGeneric) {
                    String dename = Naming.deMangle(name);
                    int left = dename.indexOf(Naming.LEFT_OXFORD);
                    int right = dename.lastIndexOf(Naming.RIGHT_OXFORD);
                    String stem = dename.substring(0,left);
                    ArrayList<String> parameters = extractStringParameters(
                                                                           dename, left, right);
                    if (stem.equals("Arrow")) {
                        classData = instantiateArrow(dename, parameters);
                    } else if (stem.equals("AbstractArrow")) {
                        classData = instantiateAbstractArrow(dename, parameters);
                    } else {
                        throw new ClassNotFoundException("Don't know how to instantiate generic " + stem + " of " + parameters);
                    }
                } else {
                    classData = getClass(name);
                }
                clazz = defineClass(name, classData, 0, classData.length);
                if (LOG_LOADS)
                    System.err.println("Loaded " + clazz.getName());
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

    private String functionTemplateName(String dename, Map<String, String> xlation) {
        // We have a mangling problem here.
        int mangleBegin = dename.indexOf(Naming.GEAR)+1; // A mangled string follows
        String firstPart = dename.substring(0, mangleBegin);
        String name = dename.substring(mangleBegin);

        int genericBegin = name.indexOf(Naming.LEFT_OXFORD) + 1;
        int genericEnd = name.indexOf(Naming.ENVELOPE) - 1; // right oxford
        String template_start = name.substring(0,genericBegin);
        String template_end = name.substring(genericEnd);
        // Note include trailing oxford to simplify loop termination.
        String generics = name.substring(genericBegin, genericEnd);
        String template_middle = "";
        int i = 1;
        while (generics.length() > 0) {
            int end = generics.indexOf(';');
            if (end == -1)
                end = generics.length();
            String tok =
                generics.substring(0, end);
            char ch = tok.charAt(0);
            String tag;
            if (ch == Naming.FOREIGN_TAG_CHAR ||
                ch == Naming.NORMAL_TAG_CHAR ||
                ch == Naming.INTERNAL_TAG_CHAR) {
                tag = Naming.YINYANG;
            } else if (ch == Naming.MUSIC_SHARP_CHAR) {
                tag = Naming.MUSIC_SHARP;
            } else if (ch == Naming.HAMMER_AND_PICK_CHAR) {
                tag = Naming.MUSIC_SHARP;
            } else {
                throw new Error("Unimplemented generic kind " + ch + " seen in instantiating classloader");
            }
            template_middle += tag+i;
            xlation.put(tag+i, tok);


            if (end == generics.length())
                break;
            template_middle += ";";
            generics = generics.substring(end+1);
            i++;
        }
        return Naming.mangleFortressIdentifier(firstPart +  template_start + template_middle + template_end);
        // return firstPart +  template_start + template_middle + template_end;
    }

    private static byte[] instantiateClosure(String name) {
        ManglingClassWriter cw = new ManglingClassWriter(ClassWriter.COMPUTE_MAXS|ClassWriter.COMPUTE_FRAMES);

        closureClassPrefix(name, cw, null);
        cw.visitEnd();

        return cw.toByteArray();

    }

    /**
     * @param name
     * @param cw
     */
    public static void closureClassPrefix(String name, ManglingClassWriter cw, String staticClass) {
        int env_loc = name.indexOf(Naming.ENVELOPE);
        int last_dot = name.substring(0,env_loc).lastIndexOf('$');

        String api = name.substring(0,last_dot);
        String suffix = name.substring(last_dot+1);
        env_loc = suffix.indexOf(Naming.ENVELOPE); // followed by $
        String fn = suffix.substring(0,env_loc);
        String ft = suffix.substring(env_loc+2); // skip $

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


        FieldVisitor fv;
        MethodVisitor mv;
        AnnotationVisitor av0;
        //String superClass = Naming.mangleFortressIdentifier("Abstract"+ft);
        String superClass = "Abstract"+ft;
        name = api.replaceAll("[.]", "/") + '$' + suffix;
        // name = Naming.mangleFortressIdentifier(name);
        String desc = "L" + name + ";";
        // String field_desc = "L" + Naming.mangleFortressIdentifier(ft) + ";";
        String field_desc = "L" +(ft) + ";";
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
            if (staticClass == null)
                staticClass = api.replaceAll("[.]", "/");

            if (LOG_LOADS) System.err.println(name + ".apply" + sig + " concrete\nparams = " + parameters);
            mv = cw.visitMethod(ACC_PUBLIC, Naming.APPLY_METHOD, sig, null, null);
            mv.visitCode();
            // Is this right?  What about a 2-arg function that takes a void as its second arg?
            // This can happen with generic instantiations of various sorts (esp the default
            // seq implementation!).
            for (int i = 1; i < parameters.size();i++) {
                String param = parameters.get(i-1);
                if (! param.equals(Naming.INTERNAL_SNOWMAN))
                    mv.visitVarInsn(ALOAD, i);

            }
            mv.visitMethodInsn(INVOKESTATIC, staticClass, fn, sig);
            // ARETURN if not void...
            mv.visitInsn(ARETURN);

            mv.visitMaxs(2, 3);
            mv.visitEnd();
        }
    }

    /**
     * @param s
     * @param leftBracket
     * @param rightBracket
     * @return
     */
    private static ArrayList<String> extractStringParameters(String s,
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

    private static byte[] instantiateArrow(String name, ArrayList<String> parameters) {
        ManglingClassWriter cw = new ManglingClassWriter(ClassWriter.COMPUTE_MAXS|ClassWriter.COMPUTE_FRAMES);
        FieldVisitor fv;
        MethodVisitor mv;
        AnnotationVisitor av0;

        cw.visit(Opcodes.V1_6, ACC_PUBLIC + ACC_ABSTRACT + ACC_INTERFACE,
                 name, null, "java/lang/Object", null);

        String sig = arrowParamsToJVMsig(parameters);

        {
            if (LOG_LOADS) System.err.println(name+".apply"+sig+" abstract");
            mv = cw.visitMethod(ACC_PUBLIC + ACC_ABSTRACT, Naming.APPLY_METHOD,
                                sig,
                                null, null);
            mv.visitEnd();
        }
        cw.visitEnd();

        return cw.toByteArray();
    }

    private static byte[] instantiateAbstractArrow(String dename, ArrayList<String> parameters) {
        ManglingClassWriter cw = new ManglingClassWriter(ClassWriter.COMPUTE_MAXS|ClassWriter.COMPUTE_FRAMES);
        FieldVisitor fv;
        MethodVisitor mv;
        AnnotationVisitor av0;

        // String name = Naming.mangleIdentifier(dename);
        String name = (dename);
        String if_name =
//            // Naming.mangleIdentifier("Arrow" + dename.substring("AbstractArrow".length()));
//            Naming.mangleIdentifier(dename.substring("Abstract".length()));
        (dename.substring("Abstract".length()));

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
            if (LOG_LOADS) System.err.println(name + ".apply" + sig+" abstract for abstract");
            mv = cw.visitMethod(ACC_PUBLIC + ACC_ABSTRACT, Naming.APPLY_METHOD,
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
    private static String arrowParamsToJVMsig(ArrayList<String> parameters) {
        String sig = "(";

        int l = parameters.size();

        for (int i = 0; i < l-1; i++) {
            String s = parameters.get(i);
            if (! s.equals(Naming.INTERNAL_SNOWMAN))
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
            || name.startsWith("sun.") || (name.startsWith("com.sun.") && ! name.startsWith("com.sun.fortress."))) {
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
