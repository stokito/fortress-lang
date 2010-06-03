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
package com.sun.fortress.runtimeSystem;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

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
import com.sun.fortress.useful.Useful;
import com.sun.fortress.useful.VersionMismatch;

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

    private final Vector<String> history = new Vector<String>();

    private InstantiatingClassloader() {
        throw new Error(); // Really do not call this.
    }

    private InstantiatingClassloader(ClassLoader parent) {
        super(parent);
        // System.err.println("I am the one true class loader!");
        if (ONLY != null)
            throw new Error();
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
        return readResource(className, "class");
    }
    private byte[] readResource(String className, String suffix) throws IOException {
        // getResourceAsStream finds a file that's in the classpath. It's
        // generally used to load resources (like images) from the same location as
        // class files. However for our purposes of loading the bytes of a class
        // file, this works perfectly. It will find the class in any place in
        // the classpath, and it doesn't force us to search the classpath
        // ourselves.
        String fileName = dotToSlash(className) + "." + suffix;

        InputStream origStream = getResourceAsStream(fileName);
        if (origStream == null) {
            throw new IOException("Resource not found : " + fileName);
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
            history.add(name);
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
                    try {
                    Map<String, String> xlation = new HashMap<String, String>();
                    String dename = Naming.dotToSep(name);
                    dename = Naming.demangleFortressIdentifier(dename);
                    ArrayList<String> sargs = new ArrayList<String>();
                    String template_name = functionTemplateName(dename, xlation, sargs);
                    byte[] templateClassData = readResource(template_name);
                    List<String> xl = Naming.xlationSerializer.fromBytes(readResource(template_name, "xlation"));
                    xlation = Useful.map(xl, sargs);
                    ManglingClassWriter cw = new ManglingClassWriter(ClassWriter.COMPUTE_MAXS|ClassWriter.COMPUTE_FRAMES);
                    ClassReader cr = new ClassReader(templateClassData);
                    ClassVisitor cvcw = LOG_FUNCTION_EXPANSION ?
                        new TraceClassVisitor((ClassVisitor) cw, new PrintWriter(System.err)) :
                            cw;
                    Instantiater instantiater = new Instantiater(cvcw, xlation, dename);
                    cr.accept(instantiater, 0);
                    classData = cw.toByteArray();
                    } catch (VersionMismatch ex) {
                        throw new ClassNotFoundException("Failed to decode xlation info", ex);
                    }
                } else if (isClosure) {
                    classData = instantiateClosure(Naming.demangleFortressIdentifier(name));
                } else if (isGeneric) {
                    String dename = Naming.dotToSep(name);
                    dename = Naming.demangleFortressIdentifier(dename);
                    int left = dename.indexOf(Naming.LEFT_OXFORD);
                    int right = dename.lastIndexOf(Naming.RIGHT_OXFORD);
                    String stem = dename.substring(0,left);
                    ArrayList<String> parameters = extractStringParameters(
                                                                           dename, left, right);
                    if (stem.equals("Arrow")) {
                        classData = instantiateArrow(dename, parameters);
                        resolve = true;
                    } else if (stem.equals("AbstractArrow")) {
                        classData = instantiateAbstractArrow(dename, parameters);
                    } else {
                        try {
                        Map<String, String> xlation = new HashMap<String, String>();
                        ArrayList<String> sargs = new ArrayList<String>();
                        String template_name = genericTemplateName(dename, xlation, sargs);
                        byte[] templateClassData = readResource(template_name);
                        List<String> xl = Naming.xlationSerializer.fromBytes(readResource(template_name, "xlation"));
                        xlation = Useful.map(xl, sargs);
                        ManglingClassWriter cw = new ManglingClassWriter(ClassWriter.COMPUTE_MAXS|ClassWriter.COMPUTE_FRAMES);
                        ClassReader cr = new ClassReader(templateClassData);
                        ClassVisitor cvcw = LOG_FUNCTION_EXPANSION ?
                            new TraceClassVisitor((ClassVisitor) cw, new PrintWriter(System.err)) :
                                cw;
                        Instantiater instantiater = new Instantiater(cvcw, xlation, dename);
                        cr.accept(instantiater, 0);
                        classData = cw.toByteArray();
                        } catch (VersionMismatch ex) {
                            throw new ClassNotFoundException("Failed to decode xlation info", ex);
                        }
                        // throw new ClassNotFoundException("Don't know how to instantiate generic " + stem + " of " + parameters);
                    }
                } else {
                    classData = getClass(name);
                }
                clazz = defineClass(name, classData, 0, classData.length);
                if (LOG_LOADS)
                    System.err.println("Loaded " + clazz.getName() + " (" + name+ ")");
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

    private String functionTemplateName(String name, Map<String, String> xlation, ArrayList<String> sargs) {
        int left_oxford = name.indexOf(Naming.LEFT_OXFORD);
        int right_oxford = name.indexOf(Naming.ENVELOPE) - 1; // right oxford

        String s = InstantiationMap.canonicalizeStaticParameters(name, left_oxford,
                right_oxford, xlation, sargs);

        return Naming.mangleFortressIdentifier(s);
    }

    private String genericTemplateName(String name, Map<String, String> xlation, ArrayList<String> sargs) {
        int left_oxford = name.indexOf(Naming.LEFT_OXFORD);
        int right_oxford = name.lastIndexOf(Naming.RIGHT_OXFORD);

        String s = InstantiationMap.canonicalizeStaticParameters(name, left_oxford,
                right_oxford, xlation, sargs);

        return Naming.mangleFortressIdentifier(s);
    }

    static public Object findGenericMethodClosure(long l, BAlongTree t, String tcn, String sig) {
        if (LOG_LOADS)
            System.err.println("findGenericMethodClosure("+l+", t, " + tcn +", " + sig +")");
        

        int up_index = tcn.indexOf(Naming.UP_INDEX);
        int envelope = tcn.indexOf(Naming.ENVELOPE); // Preceding char is RIGHT_OXFORD;
        int begin_static_params = tcn.indexOf(Naming.LEFT_OXFORD, up_index);
        int gear_index = tcn.indexOf(Naming.GEAR);
        String self_class = tcn.substring(0,gear_index) + tcn.substring(gear_index+1,up_index);
        
        String class_we_want = tcn.substring(0,begin_static_params+1) + // self_class + ";" +
            sig.substring(1) + tcn.substring(envelope);
        class_we_want = Naming.mangleFortressIdentifier(class_we_want);
        Class cl;
        try {
            cl = Class.forName(class_we_want);
            synchronized (t) {
                Object o = t.get(l);
                if (o == null) {
                    o = cl.newInstance();
                    t.put(l,o);
                }
                return o;
            }
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InstantiationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        throw new Error("Not supposed to happen; some template class must be missing.");
    }

    private static byte[] instantiateClosure(String name) {
        ManglingClassWriter cw = new ManglingClassWriter(ClassWriter.COMPUTE_MAXS|ClassWriter.COMPUTE_FRAMES);

        closureClassPrefix(name, cw, null, null, true);
        cw.visitEnd();

        return cw.toByteArray();

    }
    
    /**
     * Emits code for the common prefix of a closure class.
     * 
     * Note that names may contain "illegal" characters; these are on the
     * dangerous side of the "dangerous characters" transformation.
     * 
     * A closure class has a name of the form
     * 
     * apiComponent DOLLAR functionName ENVELOPE DOLLAR functionType
     * 
     * functionType may contain a HEAVY_X_CHAR; if it does, the characters
     * following it are part of the function's schema (declared type syntax),
     * not actual type, used only to locate the appropriate generic function
     * to instantiate.
     * 
     * 
     * 
     * @param name
     * @param cw
     * @param staticClass
     * @param sig
     */
    public static String closureClassPrefix(String name,
            ManglingClassWriter cw,
            String staticClass,
            String sig) {
        return closureClassPrefix(name, cw, staticClass, sig, false);
        
    }
        public static String closureClassPrefix(String name,
                                          ManglingClassWriter cw,
                                          String staticClass,
                                          String sig,
                                          boolean is_forwarding_closure) {
        int env_loc = name.indexOf(Naming.ENVELOPE);
        int last_dot = name.substring(0,env_loc).lastIndexOf('$');

        String api = name.substring(0,last_dot);
        String suffix = name.substring(last_dot+1);
        env_loc = suffix.indexOf(Naming.ENVELOPE); // followed by $
        String fn = is_forwarding_closure ? suffix.substring(0,env_loc): Naming.APPLIED_METHOD; 
        String ft = suffix.substring(env_loc+2); // skip $ following ENVELOPE

        // Normalize out leading HEAVY_X, if there is one.
        if (ft.charAt(0) == Naming.HEAVY_X_CHAR)
            ft = ft.substring(1);
        int left = ft.indexOf(Naming.LEFT_OXFORD);
        int right = ft.lastIndexOf(Naming.RIGHT_OXFORD);
        ArrayList<String> parameters = extractStringParameters(ft, left, right);

        if (sig == null)
            sig = arrowParamsToJVMsig(parameters);

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
        String superClass = "Abstract"+ft;
        name = api.replace(".", "/") + '$' + suffix;
        //String desc = "L" + name + ";";
        String field_desc = "L" +(ft) + ";";
        // Begin with a class
        cw.visit(V1_6, ACC_PUBLIC + ACC_SUPER, name, null, superClass, null);

        // Static field closure of appropriate arrow type.
        fv = cw.visitField(ACC_PUBLIC + ACC_FINAL + ACC_STATIC, "closure", field_desc, null, null);
        fv.visitEnd();

        // Class init allocates a singleton and initializes previous field
        mv = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
        mv.visitCode();
        mv.visitTypeInsn(NEW, name);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, name, "<init>", "()V");
        mv.visitFieldInsn(PUTSTATIC, name, "closure", field_desc);
        mv.visitInsn(RETURN);
        mv.visitMaxs(2, 0);
        mv.visitEnd();

        // Instance init does nothing special
        mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        // Supertype is mangle("Abstract"+ft)
        mv.visitMethodInsn(INVOKESPECIAL, superClass, "<init>", "()V");
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        // What if staticClass is compiler builtin?  How do we know?
        if (staticClass == null)
            staticClass = api;
        staticClass = staticClass.replace(".","/");

        if (LOG_LOADS) System.err.println(name + ".apply" + sig + " concrete\nparams = " + parameters);

        // Monkey business to deal with case of "void" args.
        int sz = parameters.size();
        // Last parameter is actually result type!
        // But we need to include an extra arg in sz to represent the closure itself (this).
        if (sz==2 && Naming.INTERNAL_SNOWMAN.equals(parameters.get(0))) {
            // Arity 1 (sz 2) with void parameter should actually be arity 0 (sz 1).
            sz = 1;
        }

        // Emit a method with well-known name ("apply", most likely)
        // to forward calls from the instance to the static, which our
        // caller will supply.  Note that the static class can be a
        // different class.
        forwardingMethod(cw, Naming.APPLY_METHOD, ACC_PUBLIC, 0,
                staticClass, fn, INVOKESTATIC,
                sig, sig, sz, false);
        
        return fn;

    }

    /** Create forwarding method that re-pushes its arguments and
     * chains to another method in another class.
     * When selfIndex == -1, all arguments are pushed exactly in the order given,
     * and the input and output signatures are assumed to be the same (so this can
     * also be used to pass along a self parameter without mucking about).
     * Otherwise selfIndex indicates the index of a self parameter; when pushSelf
     * is true this index is pushed first.
     *
     * How do we determine incoming and outgoing signatures?
     */
    public static void forwardingMethod(ClassWriter cw,
            String thisName, int thisModifiers, int selfIndex,
            String fwdClass, String fwdName, int fwdOp,
            String maximalSig, String selfCastSig,
            int nparamsIncludingSelf, boolean pushSelf) {
        forwardingMethod(cw, thisName, thisModifiers, selfIndex,
                fwdClass, fwdName, fwdOp, maximalSig, maximalSig, selfCastSig,
                nparamsIncludingSelf, pushSelf
                );
    }
    
    
    /**
     * Emits a forwarding method.
     * 
     * Cases:
     * apply static, target static
     * apply instance, target static
     * apply instance, target instance
     * 
     * @param cw Classwriter that will write the forwarding method
     * @param thisName       name of the generated (forwarding) method
     * @param thisModifiers  modifiers for the generated (forwarding) method
     * @param selfIndex      index of the self parameter, if any
     * @param fwdClass       class for the target method
     * @param fwdName        name of the target method
     * @param fwdOp          the appropriate INVOKE opcode for the forward
     * @param thisSig        the signature of the generated (forwarding) method
     * @param fwdSig         the signature of the target (called) method
     * @param selfCastSig    a full signature containing self at selfIndex
     * @param nparamsIncludingSelf number of parameters, including self (if any)
     * @param pushSelf       if true, push self first, using selfIndex to find it
     */
    public static void forwardingMethod(ClassWriter cw,
                                        String thisName, int thisModifiers, int selfIndex,
                                        String fwdClass, String fwdName, int fwdOp,
                                        String thisSig, String fwdSig, String selfCastSig,
                                        int nparamsIncludingSelf, boolean pushSelf) {
        String selfSig = null;
        if (pushSelf) {
            selfSig = Naming.nthSigParameter(selfCastSig, selfIndex);
            selfSig = selfSig.substring(1, selfSig.length()-1);
            if ((thisModifiers & ACC_STATIC) != 0) {
                if (fwdOp != INVOKESTATIC) {
                    // receiver has explicit self, fwd is dotted.
                    fwdSig = Naming.removeNthSigParameter(fwdSig, selfIndex);
                }
            } else if (fwdOp == INVOKESTATIC) {
                thisSig = Naming.removeNthSigParameter(thisSig, selfIndex);
            }
        } else if (selfIndex >= 0 && (thisModifiers & ACC_STATIC) != 0) {
            // Dropping explicit self parameter, so remove from signature.
            fwdSig = Naming.removeNthSigParameter(fwdSig, selfIndex);
        }
        // System.err.println("Forwarding "+thisName+":"+thisSig+
        //                    " arity "+nparamsIncludingSelf+"\n"+
        //                    "  to       "+fwdClass+"."+fwdName+":"+fwdSig);
        MethodVisitor mv = cw.visitMethod(thisModifiers, thisName, thisSig, null, null);
        mv.visitCode();
        if (pushSelf) {
            mv.visitVarInsn(ALOAD, selfIndex);
            mv.visitTypeInsn(CHECKCAST, selfSig);
        }
        for (int i = 0; i < nparamsIncludingSelf; i++) {
            if (i==selfIndex) continue;
            mv.visitVarInsn(ALOAD, i);
        }
        mv.visitMethodInsn(fwdOp, fwdClass, fwdName, fwdSig);
        mv.visitInsn(ARETURN);

        mv.visitMaxs(2, 3);
        mv.visitEnd();
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

        StringBuilder buf = new StringBuilder();
        buf.append(sig);
        for (int i = 0; i < l-1; i++) {
            String s = parameters.get(i);
            if (! s.equals(Naming.INTERNAL_SNOWMAN))
                buf.append(Naming.javaDescForTaggedFortressType(parameters.get(i)));
        }
        sig = buf.toString();
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
