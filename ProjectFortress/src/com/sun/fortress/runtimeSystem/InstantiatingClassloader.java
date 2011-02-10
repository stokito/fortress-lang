/*******************************************************************************
    Copyright 2009,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/
package com.sun.fortress.runtimeSystem;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.jar.JarOutputStream;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.TraceClassVisitor;

import com.sun.fortress.compiler.NamingCzar;
import com.sun.fortress.compiler.codegen.CodeGenMethodVisitor;
import com.sun.fortress.compiler.codegen.ManglingClassWriter;
import com.sun.fortress.compiler.codegen.ManglingMethodVisitor;
import com.sun.fortress.compiler.nativeInterface.SignatureParser;
import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.useful.F;
import com.sun.fortress.useful.FnVoid;
import com.sun.fortress.useful.FnVoidVoid;
import com.sun.fortress.useful.Pair;
import com.sun.fortress.useful.ProjectedList;
import com.sun.fortress.useful.Triple;
import com.sun.fortress.useful.Useful;
import com.sun.fortress.useful.VersionMismatch;

/**
 * This code steals willy-nilly from the NextGen class loader.
 *
 * @author dr2chase
 */
public class InstantiatingClassloader extends ClassLoader implements Opcodes {

    private static final String CAST_TO = "castTo";
    public static final String TUPLE_TYPED_ELT_PFX = "e";
    public static final String TUPLE_OBJECT_ELT_PFX = "o";
    public static final String TUPLE_FIELD_PFX = "f";
    public static final String ARROW_OX = "Arrow\u27e6";
    public static final String TUPLE_OX = "Tuple\u27e6";
    public static final int JVM_BYTECODE_VERSION = Opcodes.V1_6;

    
    // TODO make this depends on properties/env w/o dragging in all of the world.
    private static final boolean LOG_LOADS = false;
    private static final boolean LOG_FUNCTION_EXPANSION = false;
    public final static String SAVE_EXPANDED_DIR = ProjectProperties.getDirectory("fortress.bytecodes.expanded.directory", null);
    public static JarOutputStream SAVE_EXPANDED_JAR = null;
    static {
        try {
            SAVE_EXPANDED_JAR = new JarOutputStream(new BufferedOutputStream( new FileOutputStream(SAVE_EXPANDED_DIR + "/" + "expanded.jar")));
        } catch (IOException ex) {
            
        }
    }
    
    public static void exitProgram() {
        if (SAVE_EXPANDED_JAR != null) {
            try {
                SAVE_EXPANDED_JAR.close();
            } catch (IOException e) {
                System.err.println("Failed to close jar file for expanded bytecodes");
            }
        }
    }
   
    
    public final static InstantiatingClassloader ONLY =
        new InstantiatingClassloader(Thread.currentThread().getContextClassLoader());

    private final static ClassLoadChecker _classLoadChecker = new ClassLoadChecker();

    private final Vector<String> history = new Vector<String>();

    private final Hashtable<String, Pair<String, List<Pair<String, String>>>>
       stemToXlation = new Hashtable<String, Pair<String, List<Pair<String, String>>>>();
    
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
                    String dename = Naming.dotToSep(name);
                    dename = Naming.demangleFortressIdentifier(dename);
                    ArrayList<String> sargs = new ArrayList<String>();
                    String template_name = functionTemplateName(dename, sargs);
                    byte[] templateClassData = readResource(template_name);
                    Pair<String, List<Pair<String, String>>> pslpss =
                        Naming.xlationSerializer.fromBytes(readResource(template_name, "xlation"));
                    
                    List<String> xl = extractStaticParameterNames(pslpss);
                    
                    Map<String, String> xlation  = Useful.map(xl, sargs);
                    ManglingClassWriter cw = new ManglingClassWriter(ClassWriter.COMPUTE_MAXS|ClassWriter.COMPUTE_FRAMES);
                    ClassReader cr = new ClassReader(templateClassData);
                    ClassVisitor cvcw = LOG_FUNCTION_EXPANSION ?
                        new TraceClassVisitor((ClassVisitor) cw, new PrintWriter(System.err)) :
                            cw;
                    Instantiater instantiater = new Instantiater(cvcw, xlation, dename, this);
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
                    List<String> parameters = extractStringParameters(
                                                                           dename, left, right);
                    if (stem.equals("Arrow")) {
                        // Arrow interface
                        classData = instantiateArrow(dename, parameters);
                        resolve = true;
                    } else if (stem.equals("AbstractArrow")) {
                        // Arrow boilerplate
                        classData = instantiateAbstractArrow(dename, parameters);
                    } else if (stem.equals("Tuple")) {
                        classData = instantiateTuple(dename, parameters);
                    } else if (stem.equals("ConcreteTuple")) {
                        classData = instantiateConcreteTuple(dename, parameters);
                    } else if (stem.equals("AnyTuple")) {
                        classData = instantiateAnyTuple(dename, parameters);
                    } else if (stem.equals("AnyConcreteTuple")) {
                        classData = instantiateAnyConcreteTuple(dename, parameters);
                    } else {
                        try {
                        ArrayList<String> sargs = new ArrayList<String>();
                        String template_name = genericTemplateName(dename, sargs);
                        byte[] templateClassData = readResource(template_name);
                        Pair<String, List<Pair<String, String>>> pslpss =
                            Naming.xlationSerializer.fromBytes(readResource(template_name, "xlation"));
                        
                        List<String> xl = extractStaticParameterNames(pslpss);
                        Map<String, String> xlation = Useful.map(xl, sargs);
                        ManglingClassWriter cw = new ManglingClassWriter(ClassWriter.COMPUTE_MAXS|ClassWriter.COMPUTE_FRAMES);
                        ClassReader cr = new ClassReader(templateClassData);
                        ClassVisitor cvcw = LOG_FUNCTION_EXPANSION ?
                            new TraceClassVisitor((ClassVisitor) cw, new PrintWriter(System.err)) :
                                cw;
                        Instantiater instantiater = new Instantiater(cvcw, xlation, dename, this);
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
                
                if ((isGeneric || isGenericFunction || isClosure) && SAVE_EXPANDED_JAR != null) {
                    ByteCodeWriter.writeJarredClass(SAVE_EXPANDED_JAR, name , classData);
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

    /**
     * @param pslpss
     * @return
     */
    public static List<String> extractStaticParameterNames(
            Pair<String, List<Pair<String, String>>> pslpss) {
        List<String> xl =
            new ProjectedList<Pair<String, String>, String>(
                    pslpss.getB(),
                    new Pair.GetB<String, String>());
        return xl;
    }

    private String functionTemplateName(String name, ArrayList<String> sargs) {
        int left_oxford = name.indexOf(Naming.LEFT_OXFORD);
        int right_oxford = name.indexOf(Naming.ENVELOPE) - 1; // right oxford

        String s = InstantiationMap.canonicalizeStaticParameters(name, left_oxford,
                right_oxford, sargs);

        return Naming.mangleFortressIdentifier(s);
    }

    private String genericTemplateName(String name, ArrayList<String> sargs) {
        int left_oxford = name.indexOf(Naming.LEFT_OXFORD);
        int right_oxford = name.lastIndexOf(Naming.RIGHT_OXFORD);

        String s = InstantiationMap.canonicalizeStaticParameters(name, left_oxford,
                right_oxford, sargs);

        return Naming.mangleFortressIdentifier(s);
    }

    static public Object findGenericMethodClosure(long l, BAlongTree t, String tcn, String sig) {
        if (LOG_LOADS)
            System.err.println("findGenericMethodClosure("+l+", t, " + tcn +", " + sig +")");

        int up_index = tcn.indexOf(Naming.UP_INDEX);
        int envelope = tcn.indexOf(Naming.ENVELOPE); // Preceding char is RIGHT_OXFORD;
        int begin_static_params = tcn.indexOf(Naming.LEFT_OXFORD, up_index);
        // int gear_index = tcn.indexOf(Naming.GEAR);
        // String self_class = tcn.substring(0,gear_index) + tcn.substring(gear_index+1,up_index);
        
        String class_we_want = tcn.substring(0,begin_static_params+1) + // self_class + ";" +
            sig.substring(1) + tcn.substring(envelope);
        class_we_want = Naming.mangleFortressIdentifier(class_we_want);
        return loadClosureClass(l, t, class_we_want);
    }

    static public Object findGenericMethodClosure(long l, BAlongTree t,
            String tcn, String sig, String trait_sig) {
        if (LOG_LOADS)
            System.err.println("findGenericMethodClosure("+l+", t, " + tcn +
                    ", " + sig +", " + trait_sig + ")");

        int up_index = tcn.indexOf(Naming.UP_INDEX);
        int envelope = tcn.indexOf(Naming.ENVELOPE); // Preceding char is RIGHT_OXFORD;
        int begin_static_params = tcn.indexOf(Naming.LEFT_OXFORD, up_index);
        // int gear_index = tcn.indexOf(Naming.GEAR);
        // String self_class = tcn.substring(0,gear_index) + tcn.substring(gear_index+1,up_index);
        
        String class_we_want = tcn.substring(0,begin_static_params+1) + // self_class + ";" +
            Useful.substring(sig,1,-1) + ";" + trait_sig.substring(1) + tcn.substring(envelope);
        class_we_want = Naming.mangleFortressIdentifier(class_we_want);
        return loadClosureClass(l, t, class_we_want);
    } 

    /**
     * @param l
     * @param t
     * @param class_we_want
     * @throws Error
     */
    private static Object loadClosureClass(long l, BAlongTree t,
            String class_we_want) throws Error {
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

        closureClassPrefix(name, cw, null, null, true, null);
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
            String sig,
            String forceCastParam0) {
        return closureClassPrefix(name, cw, staticClass, sig, false, forceCastParam0);
        
    }
        public static String closureClassPrefix(String name,
                                          ManglingClassWriter cw,
                                          String staticClass,
                                          String sig,
                                          boolean is_forwarding_closure,
                                          String forceCastParam0) {
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
        List<String> parameters = extractStringParameters(ft, left, right);

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
        String superClass = "Abstract"+ft;
        name = api.replace(".", "/") + '$' + suffix;
        //String desc = "L" + name + ";";
        String field_desc = "L" +(ft) + ";";
        // Begin with a class
        cw.visit(JVM_BYTECODE_VERSION, ACC_PUBLIC + ACC_SUPER, name, null, superClass, null);

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
                sig, sig, sz, false, forceCastParam0);
        
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
            int nparamsIncludingSelf, boolean pushSelf, String forceCastParam0) {
        forwardingMethod(cw, thisName, thisModifiers, selfIndex,
                fwdClass, fwdName, fwdOp, maximalSig, maximalSig, selfCastSig,
                nparamsIncludingSelf, pushSelf, forceCastParam0
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
     * @param forceCastParam0 cast param 0, even if it is not self.  This is for
     *                        implementation of generic methods.  It may need
     *                        to be generalized to all params, not entirely sure.
     */
    public static void forwardingMethod(ClassWriter cw,
                                        String thisName, int thisModifiers, int selfIndex,
                                        String fwdClass, String fwdName, int fwdOp,
                                        String thisSig, String fwdSig, String selfCastSig,
                                        int nparamsIncludingSelf, boolean pushSelf, String forceCastParam0) {
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
        
        if (forceCastParam0 != null) {
            fwdSig = Naming.replaceNthSigParameter(fwdSig, 0, "L" + forceCastParam0 + ";");
        }
        
        // System.err.println("Forwarding "+thisName+":"+thisSig+
        //                    " arity "+nparamsIncludingSelf+"\n"+
        //                    "  to       "+fwdClass+"."+fwdName+":"+fwdSig);
        MethodVisitor mv = cw.visitMethod(thisModifiers, thisName, thisSig, null, null);
        mv.visitCode();
        
        SignatureParser sp = new SignatureParser(fwdSig);
        
        int parsed_arg_cursor = 0;
        
        if (pushSelf) {
            mv.visitVarInsn(ALOAD, selfIndex);
            mv.visitTypeInsn(CHECKCAST, selfSig);
            if (fwdOp == INVOKESTATIC)
                parsed_arg_cursor++;
        }
        
        pushParamsNotSelf(selfIndex, nparamsIncludingSelf, forceCastParam0, mv,
                sp, parsed_arg_cursor);
        
        mv.visitMethodInsn(fwdOp, fwdClass, fwdName, fwdSig);
        mv.visitInsn(ARETURN);

        mv.visitMaxs(2, 3);
        mv.visitEnd();
    }

    /**
     * @param selfIndex
     * @param nparamsIncludingSelf
     * @param forceCastParam0
     * @param mv
     * @param sp
     * @param parsed_arg_cursor
     */
    public static void pushParamsNotSelf(int selfIndex,
            int nparamsIncludingSelf, String forceCastParam0, MethodVisitor mv,
            SignatureParser sp, int parsed_arg_cursor) {
        int i_bump = 0;
        List<String> parsed_args = sp.getJVMArguments();
        for (int i = 0; i < nparamsIncludingSelf; i++) {
            if (i==selfIndex) continue;
            String one_param = parsed_args.get(parsed_arg_cursor++);
            int load_op = sp.asm_loadop(one_param);
            mv.visitVarInsn(load_op, i + i_bump);
            // TODO Need to get counting right here.  P0 is "really" P1
            if (i == 1 && forceCastParam0 != null) {
                mv.visitTypeInsn(CHECKCAST, forceCastParam0);
            }
            // if one_param is long or double, increment i_bump to account for the extra slot.
            i_bump += sp.width(one_param) - 1;
        }
    }


    /**
     * @param s
     * @param leftBracket
     * @param rightBracket
     * @return
     */
    private static List<String> extractStringParameters(String s,
                                                             int leftBracket, int rightBracket) {
        
        ArrayList<String> parameters = new ArrayList<String>();
        return InstantiationMap.extractStringParameters(s, leftBracket, rightBracket, parameters);
    }
    private static List<String> extractStringParameters(String s) {
        int leftBracket = s.indexOf(Naming.LEFT_OXFORD);
        int rightBracket = InstantiationMap.templateClosingRightOxford(s);
        return extractStringParameters(s, leftBracket, rightBracket);
    }

    public static void eep(MethodVisitor mv, String s) {
         mv.visitLdcInsn(s);
         mv.visitMethodInsn(INVOKESTATIC, "com/sun/fortress/runtimeSystem/InstantiatingClassloader", "eep", "(Ljava/lang/String;)V");
     }     
     
     public static void eep(String s) {
         System.err.println(s);
     }
     
    /**
     * Generates an interface method (public, abstract) with specified name
     * and signature.
     * 
     * @param cw
     * @param m
     * @param sig
     */
    private static void interfaceMethod(ManglingClassWriter cw, String m,
            String sig) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_ABSTRACT, m, sig, null, null);
        mv.visitMaxs(Naming.ignoredMaxsParameter, Naming.ignoredMaxsParameter);
        mv.visitEnd();
    }
    
    /**
     * Generate a trivial init method.
     * 
     * @param cw
     * @param _super
     */
    private static void simpleInitMethod(ManglingClassWriter cw, String _super) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, _super, "<init>", "()V");
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }
    
    private static byte[] instantiateArrow(String name, List<String> parameters) {
        ManglingClassWriter cw = new ManglingClassWriter(ClassWriter.COMPUTE_MAXS|ClassWriter.COMPUTE_FRAMES);
        
        // newer boilerplate copied from abstract arrow and wrapped arrow
        Triple<List<String>, List<String>, String> stuff =
            normalizeArrowParameters(parameters);
        
        List<String> unwrapped_parameters = stuff.getA();
        List<String> tupled_parameters = stuff.getB();
        String tuple_type = stuff.getC();
        List<String> objectified_parameters = Useful.applyToAll(unwrapped_parameters, toJLO);
        String obj_sig = stringListToGeneric("Arrow", objectified_parameters);

        boolean is_all_objects = objectified_parameters.equals(unwrapped_parameters);
        
        String[] super_interfaces = null;
        if (!is_all_objects) {
            super_interfaces = new String[] { obj_sig };
        }
        
        cw.visit(JVM_BYTECODE_VERSION, ACC_PUBLIC + ACC_ABSTRACT + ACC_INTERFACE,
                 name, null, "java/lang/Object", super_interfaces);

        /* If more than one domain parameter, then also include the tupled apply method. */
        int l = parameters.size();
        if (l > 2) {
//            String tupleType = stringListToTuple(parameters.subList(0, l-1));
//            List<String> tupled_parameters = Useful.<String>list(tupleType,
//                        parameters.get(l-1)  );
           
            String sig = arrowParamsToJVMsig(tupled_parameters);
            if (LOG_LOADS) System.err.println(name+".apply"+sig+" abstract");
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC + ACC_ABSTRACT, Naming.APPLY_METHOD,
                                sig,
                                null, null);
            mv.visitEnd();
        }
        
        {      
            String sig = arrowParamsToJVMsig(parameters);
            if (LOG_LOADS) System.err.println(name+".apply"+sig+" abstract");
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC + ACC_ABSTRACT, Naming.APPLY_METHOD,
                                sig,
                                null, null);
            mv.visitEnd();
        }
        {      
            String sig = "()L"+obj_sig+";";
            if (LOG_LOADS) System.err.println(name+".getWrappee"+sig+" abstract");
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC + ACC_ABSTRACT, getWrappee,
                                sig,
                                null, null);
            mv.visitEnd();
        }
        cw.visitEnd();

        return cw.toByteArray();
    }

    static F<String, String> toJLO = new F<String, String>() {

        @Override
        public String apply(String x) {
            // TODO Auto-generated method stub
            return "java/lang/Object";
        }
        
    };
    
    static final String getWrappee = "getWrappee";
    
    private static byte[] instantiateWrappedArrow(String name, List<String> parameters) {
        ManglingClassWriter cw = new ManglingClassWriter(ClassWriter.COMPUTE_MAXS|ClassWriter.COMPUTE_FRAMES);
        /*
         * extends AbstractArrow[\parameters\]
         * 
         * private final Arrow[\Object...Object\] wrappee
         * 
         * Arrow[\Object...Object\] getWrappee()
         * 
         * WrappedArrow[\parameters\](Arrow[\Object...Object\] _wrappee)
         * 
         * public range_parameter apply( domain_parameters ) = 
         *   (range_parameter) wrappee.apply( domain_parameters )
         */
        Triple<List<String>, List<String>, String> stuff =
            normalizeArrowParameters(parameters);
        
        List<String> unwrapped_parameters = stuff.getA();
        List<String> tupled_parameters = stuff.getB();
        String tupleType = stuff.getC();

        String extendsClass = stringListToGeneric("AbstractArrow", unwrapped_parameters);
        List<String> objectified_parameters = Useful.applyToAll(unwrapped_parameters, toJLO);
        //String obj_sig = stringListToGeneric("AbstractArrow", objectified_parameters);
        String obj_intf_sig = stringListToGeneric("Arrow", objectified_parameters);
        String wrappee_name = "wrappee";
        
        //extends AbstractArrow[\parameters\]
        cw.visit(JVM_BYTECODE_VERSION, ACC_PUBLIC + ACC_SUPER, name, null,
                extendsClass, null);

        // private final Arrow[\Object...Object\] wrappee
        cw.visitField(Opcodes.ACC_PRIVATE + Opcodes.ACC_FINAL, wrappee_name,
                obj_intf_sig, null /* for non-generic */, null /* instance has no value */);

        // WrappedArrow[\parameters\](Arrow[\Object...Object\] _wrappee)
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        // super()
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, extendsClass, "<init>", "()V");
        // this.wrappee = wrappee
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitFieldInsn(PUTFIELD, name, wrappee_name, "L" + obj_intf_sig + ";");
        // done
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        // getWrappee
        
        mv = cw.visitMethod(ACC_PUBLIC, getWrappee,
                "()L"+obj_intf_sig+";",
                null, null);
        
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, name, wrappee_name, "L" + obj_intf_sig + ";");
        mv.visitInsn(ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        //  public range_parameter apply( domain_parameters ) = 
        //    (range_parameter) wrappee.apply( domain_parameters )
        
        String unwrapped_apply_sig = arrowParamsToJVMsig(unwrapped_parameters);
        String obj_apply_sig = arrowParamsToJVMsig(objectified_parameters);
  
        mv = cw.visitMethod(ACC_PUBLIC, Naming.APPLY_METHOD,
                unwrapped_apply_sig,
                null, null);
        mv.visitCode();

        // load wrappee for delegation
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, name, wrappee_name, "L" + obj_intf_sig + ";");
        
        // Push parameters.
        // i is indexed so that it corresponds to parameters pushed, even though
        // the types are ignored here (for now).
        for (int i = 0; i < unwrapped_parameters.size()-1; i++) {
            String t = unwrapped_parameters.get(i);
            if (!t.equals(Naming.INTERNAL_SNOWMAN)) {
                mv.visitVarInsn(ALOAD, i+1);
            }
        }

        mv.visitMethodInsn(INVOKEVIRTUAL, obj_intf_sig, Naming.APPLY_METHOD, obj_apply_sig);
        
        // mv.visitTypeInsn(Opcodes.CHECKCAST, parameters.get(parameters.size()-1));
        generalizedCastTo(mv, parameters.get(parameters.size()-1));
        
        // done
        mv.visitInsn(ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        cw.visitEnd();

        return cw.toByteArray();

    }
    private static byte[] instantiateAbstractArrow(String name, List<String> parameters) {
        ManglingClassWriter cw = new ManglingClassWriter(ClassWriter.COMPUTE_MAXS|ClassWriter.COMPUTE_FRAMES);
        
        /*
         * Special case extensions to plumb tuples
         * correctly in the face of generics instantiated
         * with tuple types.
         * 
         * Except, recall that Arrow parameters are domain...;range
         * 
         * if > 1 param then
         *   unwrap = params
         *   wrap = tuple params
         * else 1 param
         *   if tuple
         *     wrap = param
         *     unwrap = untuple params
         *   else
         *     unwrap = param
         *     wrap = null
         *     
         *  Use unwrapped parameters to generate the all-Objects case
         *  for casting; check the generated signature against the input
         *  to see if we are them.
         *   
         */
       
        Triple<List<String>, List<String>, String> stuff =
            normalizeArrowParameters(parameters);
        
        List<String> unwrapped_parameters = stuff.getA();
        List<String> tupled_parameters = stuff.getB();
        String tupleType = stuff.getC();
        
        List<String> objectified_parameters = Useful.applyToAll(unwrapped_parameters, toJLO);
        String obj_sig = stringListToGeneric("AbstractArrow", objectified_parameters);
        String obj_intf_sig = stringListToGeneric("Arrow", objectified_parameters);
        String wrapped_sig = stringListToGeneric("WrappedArrow", unwrapped_parameters);
        String typed_intf_sig = stringListToGeneric("Arrow", unwrapped_parameters);
        String unwrapped_apply_sig = arrowParamsToJVMsig(unwrapped_parameters);
        String obj_apply_sig = arrowParamsToJVMsig(objectified_parameters);
    
        String[] interfaces = tupled_parameters == null ?
                  new String[] { stringListToArrow(unwrapped_parameters) }
                : new String[] { stringListToArrow(unwrapped_parameters),
                                 stringListToArrow(tupled_parameters) };

        boolean is_all_objects = objectified_parameters.equals(unwrapped_parameters);
                  
        String _super = is_all_objects ? "java/lang/Object" : obj_sig ;

        cw.visit(JVM_BYTECODE_VERSION, ACC_PUBLIC + ACC_SUPER + ACC_ABSTRACT, name, null,
                _super, interfaces);

        simpleInitMethod(cw, _super);
        
        /* */
        if (! is_all_objects ) {
            // implement method for the object version.
            // cast parameters, invoke this.apply on cast parameters, ARETURN
            
            // note cut and paste from apply below, work in progress.
            
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, Naming.APPLY_METHOD,
                    obj_apply_sig,
                    null, null);
            
            mv.visitVarInsn(Opcodes.ALOAD, 0); // this
            
            int unwrapped_l = unwrapped_parameters.size();

            for (int i = 0; i < unwrapped_l-1; i++) {
                String t = unwrapped_parameters.get(i);
                if (!t.equals(Naming.INTERNAL_SNOWMAN)) {
                    mv.visitVarInsn(Opcodes.ALOAD, i+1); // element
                    // mv.visitTypeInsn(CHECKCAST, t);
                    generalizedCastTo(mv, t);
                }
            }

            mv.visitMethodInsn(INVOKEVIRTUAL, name, Naming.APPLY_METHOD, unwrapped_apply_sig);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(Naming.ignoredMaxsParameter, Naming.ignoredMaxsParameter);
       
            mv.visitEnd();            
        } 
        
        // castTo
        {
            /*
             *  If arg0 instanceof typed_intf_sig
             *     return arg0
             *  arg0 = arg0.getWrappee()
             *  if arg0 instanceof typed_intf_sig
             *     return arg0
             *  new WrappedArrow
             *  dup
             *  push argo
             *  init
             *  return tos
             */         
            
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, CAST_TO,
                    "(L" + obj_intf_sig + ";)L" + typed_intf_sig + ";",
                    null, null);

            Label not_instance1 = new Label();
            Label not_instance2 = new Label();

            // try bare instanceof
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitTypeInsn(Opcodes.INSTANCEOF, typed_intf_sig);
            mv.visitJumpInsn(Opcodes.IFEQ, not_instance1);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitInsn(Opcodes.ARETURN);
            
            // unwrap
            mv.visitLabel(not_instance1);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(INVOKEVIRTUAL, obj_intf_sig, getWrappee, "()L"+ obj_intf_sig + ";");
            mv.visitVarInsn(Opcodes.ASTORE, 0);

            // try instanceof on unwrapped
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitTypeInsn(Opcodes.INSTANCEOF, typed_intf_sig);
            mv.visitJumpInsn(Opcodes.IFEQ, not_instance2);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitInsn(Opcodes.ARETURN);
            
            // wrap and return
            mv.visitLabel(not_instance2);
            mv.visitTypeInsn(NEW, wrapped_sig);
            mv.visitInsn(DUP);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, wrapped_sig, "<init>", "(L" + obj_intf_sig +";)V");
            
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(Naming.ignoredMaxsParameter, Naming.ignoredMaxsParameter);
       
            mv.visitEnd();            
        } 

        // getWrappee
        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, getWrappee,
                "()L"+obj_intf_sig+";",
                null, null);
        
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd(); // return this
        }
        
        if (tupled_parameters == null) {
            /* Single abstract method */
            if (LOG_LOADS) System.err.println(name + ".apply" + unwrapped_apply_sig+" abstract for abstract");
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC + ACC_ABSTRACT, Naming.APPLY_METHOD,
                    unwrapped_apply_sig,
                    null, null);
            mv.visitEnd();

        } else {
            /*
             * Establish two circular forwarding methods;
             * the eventual implementer will break the cycle.
             * 
             */
            String tupled_apply_sig = arrowParamsToJVMsig(tupled_parameters);

            {
                /* Given tupled args, extract, and invoke apply. */
                
                if (LOG_LOADS) System.err.println(name + ".apply" + tupled_apply_sig+" abstract for abstract");
                MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, Naming.APPLY_METHOD,
                        tupled_apply_sig,
                        null, null);
                
                mv.visitVarInsn(Opcodes.ALOAD, 0); // closure
                
                int unwrapped_l = unwrapped_parameters.size();
                
                for (int i = 0; i < unwrapped_l-1; i++) {
                    String param = unwrapped_parameters.get(i);
                    mv.visitVarInsn(Opcodes.ALOAD, 1); // tuple
                    mv.visitMethodInsn(INVOKEINTERFACE, tupleType, TUPLE_TYPED_ELT_PFX + (Naming.TUPLE_ORIGIN + i), "()L" + param + ";");
                }
                
                mv.visitMethodInsn(INVOKEVIRTUAL, name, Naming.APPLY_METHOD, unwrapped_apply_sig);
                mv.visitInsn(Opcodes.ARETURN);
                mv.visitMaxs(Naming.ignoredMaxsParameter, Naming.ignoredMaxsParameter);

                mv.visitEnd();
            }

            {   /* Given untupled args, load, make a tuple, invoke apply. */
                if (LOG_LOADS) System.err.println(name + ".apply" + unwrapped_apply_sig+" abstract for abstract");
                MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, Naming.APPLY_METHOD,
                        unwrapped_apply_sig,
                        null, null);
                
                mv.visitVarInsn(Opcodes.ALOAD, 0); // closure
                
                int unwrapped_l = unwrapped_parameters.size();

                for (int i = 0; i < unwrapped_l-1; i++) {
                    mv.visitVarInsn(Opcodes.ALOAD, i+1); // element
                }

                List<String> tuple_elements = unwrapped_parameters.subList(0,unwrapped_l-1);
                
                String make_sig = toJvmSig(tuple_elements,
                                  Naming.javaDescForTaggedFortressType(tupleType));
                mv.visitMethodInsn(INVOKESTATIC, 
                        stringListToGeneric("ConcreteTuple", tuple_elements), "make", make_sig);

                mv.visitMethodInsn(INVOKEVIRTUAL, name, Naming.APPLY_METHOD, tupled_apply_sig);
                mv.visitInsn(Opcodes.ARETURN);
                mv.visitMaxs(Naming.ignoredMaxsParameter, Naming.ignoredMaxsParameter);
           
                mv.visitEnd();
            }
            

        }
        cw.visitEnd();

        return cw.toByteArray();
    }

    /**
     * @param parameters
     * @return
     */
    private static Triple<List<String>, List<String>, String> normalizeArrowParameters(
            List<String> parameters) {
        Triple<List<String>, List<String>, String> stuff;
        {
            int l = parameters.size();
            List<String> unwrapped_parameters = null;
            List<String> tupled_parameters = null;
            String tupleType = null;

            if (l == 2) {
                String parameter = parameters.get(0);
                if (parameter.startsWith(TUPLE_OX)) {
                    /* Unwrap tuple, also. */
                    unwrapped_parameters = extractStringParameters(parameter);
                    unwrapped_parameters.add(parameters.get(1));
                    tupled_parameters = parameters;
                    tupleType = parameter;
                } else {
                    unwrapped_parameters = parameters;
                }

            } else {
                unwrapped_parameters = parameters;
                tupleType = stringListToTuple(parameters.subList(0, l - 1));
                tupled_parameters = Useful.<String> list(tupleType,
                        parameters.get(l - 1));
            }

            stuff = new Triple(unwrapped_parameters, tupled_parameters,
                    tupleType);
        }
        return stuff;
    }

    static final String UNTYPED_GETTER_SIG = "()Lfortress/AnyType$Any;";
    
    private static byte[] instantiateAnyTuple(String dename, List<String> parameters) {
        /*
         * Single parameter, N, which is the arity of the tuple.
         * 
         * implements Ljava/util/List;
         * implements Lfortress/AnyType$Any;
         * abstract methods o1 ... oN (or 0 ... N-1, depending on tuple origin)
         */
        int n = Integer.parseInt(parameters.get(0));

        ManglingClassWriter cw = new ManglingClassWriter(ClassWriter.COMPUTE_MAXS|ClassWriter.COMPUTE_FRAMES);
        String[] superInterfaces = {
        //        "java/util/List",
                "fortress/AnyType$Any"
        };
        cw.visit( JVM_BYTECODE_VERSION,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE,
                dename, null, "java/lang/Object", superInterfaces);

        for (int i = 0; i < n; i++) {
            String m = TUPLE_OBJECT_ELT_PFX + (i + Naming.TUPLE_ORIGIN);
            String sig = UNTYPED_GETTER_SIG;
            interfaceMethod(cw, m, sig);
        }

        cw.visitEnd(); 
        return cw.toByteArray();
    }
    
    private static byte[] instantiateAnyConcreteTuple(String dename, List<String> parameters) {
        /*
         * Single parameter, N, which is the arity of the tuple.
         * 
         * extends Ljava/util/AbstractList;
         * implements LAnyTuple[\N\];
         * int size() { return N; }
         * Object get(int n) {
         *    if (n >= N || n < 0) {
         *       throw new IndexOutOfBoundsException();
         *    } else {
         *      // binary search tree returning o1 ... oN
         *    }
         * }
         */
        
        ManglingClassWriter cw = new ManglingClassWriter(ClassWriter.COMPUTE_MAXS|ClassWriter.COMPUTE_FRAMES);
        final String super_type = "java/lang/Object";
        
        final int n = Integer.parseInt(parameters.get(0));
        final String any_tuple_n = "AnyTuple" + Naming.LEFT_OXFORD + n + Naming.RIGHT_OXFORD;        
        String[] superInterfaces = { any_tuple_n };
        cw.visit( JVM_BYTECODE_VERSION,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
                dename, null, super_type, superInterfaces);

        simpleInitMethod(cw, super_type);

        { // size method
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "size", "()I", null, null);
            mv.visitCode();
            mv.visitIntInsn(BIPUSH, n);
            mv.visitInsn(IRETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }

        { // get method  
            final MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "get", "(I)Ljava/lang/Object;", null, null);
            mv.visitCode();
            mv.visitVarInsn(ILOAD, 1);
            mv.visitIntInsn(BIPUSH, n);
            Label l1 = new Label();
            mv.visitJumpInsn(IF_ICMPGE, l1);
            mv.visitVarInsn(ILOAD, 1);
            Label l2 = new Label();
            mv.visitJumpInsn(IFGE, l2);
            mv.visitLabel(l1);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitTypeInsn(NEW, "java/lang/IndexOutOfBoundsException");
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/IndexOutOfBoundsException", "<init>", "()V");
            mv.visitInsn(ATHROW);

            FnVoidVoid geti = new FnVoidVoid() {

                @Override
                public void apply() {
                    mv.visitVarInsn(ILOAD, 1);
                }
                
            };
            
            FnVoid<Integer> leaf = new FnVoid<Integer>() {

                @Override
                public void apply(Integer x) {
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitMethodInsn(INVOKEINTERFACE, any_tuple_n, TUPLE_OBJECT_ELT_PFX + (Naming.TUPLE_ORIGIN + x), UNTYPED_GETTER_SIG);
                    mv.visitInsn(ARETURN);
                }
                
            };
            
            visitBinaryTree(mv, 0, n-1, l2, geti, leaf);
            
            mv.visitMaxs(2, 2);
            mv.visitEnd();

        }
        
        cw.visitEnd(); 
        return cw.toByteArray();
    }
    
    /**
     * Generates a binary search tree for integers in the range [lo,hi]
     * (INCLUSIVE!).  Target, if not null, is to be attached to the generated code.
     * geti pushes the integer in question onto the top of the stack.
     * leaf handles the leaf case where lo=hi.
     * 
     * Cases are generated into ascending order, just because.
     * 
     * @param mv
     * @param lo
     * @param hi
     * @param target
     * @param geti
     * @param leaf
     */
    
    static void visitBinaryTree(MethodVisitor mv, int lo, int hi, Label target, FnVoidVoid geti,  FnVoid<Integer> leaf) {
        if (target != null)
            mv.visitLabel(target);
        mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

        if (lo == hi) {
           leaf.apply(lo);
        } else {
            /*
             * 0,1 -> 0,0; 1,1
             * 0,2 -> 0,1; 2,2 
             */
            int mid = (lo + hi)/2;
            Label small = null;
            Label large = new Label();
            geti.apply();
            mv.visitIntInsn(BIPUSH, mid);
            mv.visitJumpInsn(IF_ICMPGT, large);
            visitBinaryTree(mv, lo, mid, small, geti, leaf);
            visitBinaryTree(mv, mid+1, hi, large, geti, leaf);
        }
    }
    
    
    private static byte[] instantiateTuple(String dename, List<String> parameters) {
        /*
         * interface implements AnyTuple[\ N \]
         * methods e1 ... eN returning typed results.
         */
        ManglingClassWriter cw = new ManglingClassWriter(ClassWriter.COMPUTE_MAXS|ClassWriter.COMPUTE_FRAMES);
 
        final int n = parameters.size();
        final String any_tuple_n = "AnyTuple" + Naming.LEFT_OXFORD + n + Naming.RIGHT_OXFORD;        
        String[] superInterfaces = { any_tuple_n };
        
        cw.visit(JVM_BYTECODE_VERSION, ACC_PUBLIC + ACC_ABSTRACT + ACC_INTERFACE, dename, null,
                 "java/lang/Object", superInterfaces);


        for (int i = 0; i < n; i++) {
            String m = TUPLE_TYPED_ELT_PFX + (i + Naming.TUPLE_ORIGIN);
            String sig = "()L" + parameters.get(i) + ";";
            interfaceMethod(cw, m, sig);
        }

        cw.visitEnd();

        return cw.toByteArray();
    }

    private static byte[] instantiateConcreteTuple(String dename, List<String> parameters) {
        /*
         * extends AnyConcreteTuple[\ N \]
         * 
         * implements Tuple[\ parameters \]
         * 
         * defines f1 ... fN
         * defines e1 ... eN
         * defines o1 ... oN
         */

        ManglingClassWriter cw = new ManglingClassWriter(ClassWriter.COMPUTE_MAXS|ClassWriter.COMPUTE_FRAMES);
        
        final int n = parameters.size();
        final String any_tuple_n = "AnyTuple" + Naming.LEFT_OXFORD + n + Naming.RIGHT_OXFORD;        
        final String any_concrete_tuple_n = "AnyConcreteTuple" + Naming.LEFT_OXFORD + n + Naming.RIGHT_OXFORD;        
        final String tuple_params = stringListToTuple(parameters);
        
        String[] superInterfaces = { tuple_params };
        
        cw.visit(JVM_BYTECODE_VERSION, ACC_PUBLIC + ACC_SUPER, dename, null,
                any_concrete_tuple_n, superInterfaces);
        
        
        /* Outline of what must be generated:
        
        // fields
        
        // init method
        
        // factory method
        
        // is instance method -- takes an Object

        // is instance method
          
        // cast method
        
        // typed getters
        
        // untyped getters
         
        */
        
        // fields
        {
            for (int i = 0; i < n; i++) {
                String f = TUPLE_FIELD_PFX + (i + Naming.TUPLE_ORIGIN);
                String sig = "L" + parameters.get(i) + ";";
                cw.visitField(Opcodes.ACC_PRIVATE + Opcodes.ACC_FINAL, f,
                        sig, null /* for non-generic */, null /* instance has no value */);
            }
        }
        // init method
        {
            String init_sig = tupleParamsToJvmInitSig(parameters);
            MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", init_sig, null, null);
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, any_concrete_tuple_n, "<init>", NamingCzar.voidToVoid);

            for (int i = 0; i < n; i++) {
                String f = TUPLE_FIELD_PFX + (i + Naming.TUPLE_ORIGIN);
                String sig = "L" + parameters.get(i) + ";";
                
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitVarInsn(Opcodes.ALOAD, i+1);
                mv.visitFieldInsn(Opcodes.PUTFIELD, dename, f, sig);
            }
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(Naming.ignoredMaxsParameter, Naming.ignoredMaxsParameter);
            mv.visitEnd();
        }
            
        // factory method -- same args as init, returns a new one.
        {
            String init_sig = tupleParamsToJvmInitSig(parameters);
            String make_sig = toJvmSig(parameters, Naming.javaDescForTaggedFortressType(tuple_params));
            MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, "make", make_sig, null, null);
            
            mv.visitCode();
            // eep(mv, "before new");
            mv.visitTypeInsn(NEW, dename);
            mv.visitInsn(DUP);
            // push params for init
            for (int i = 0; i < n; i++) {
                mv.visitVarInsn(Opcodes.ALOAD, i);
            }
            // eep(mv, "before init");
            mv.visitMethodInsn(INVOKESPECIAL, dename, "<init>", init_sig);
            
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(Naming.ignoredMaxsParameter, Naming.ignoredMaxsParameter);
            mv.visitEnd();
        }
        
        // is instance method -- takes an Object
        {
            String sig = "(Ljava/lang/Object;)Z";
            MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, "isA", sig, null, null);
            
            Label fail = new Label();
            
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitTypeInsn(Opcodes.INSTANCEOF, any_tuple_n);
            mv.visitJumpInsn(Opcodes.IFEQ, fail);
            
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitTypeInsn(Opcodes.CHECKCAST, any_tuple_n);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, dename,  "isA", "(L"+any_tuple_n+";)Z");
            mv.visitInsn(Opcodes.IRETURN);
            
            mv.visitLabel(fail);
            mv.visitIntInsn(BIPUSH, 0);
            mv.visitInsn(Opcodes.IRETURN);
            
            mv.visitMaxs(Naming.ignoredMaxsParameter, Naming.ignoredMaxsParameter);
            mv.visitEnd();
        }
        
        // is instance method -- takes an AnyTuple[\N\]
        {
            String sig = "(L" + any_tuple_n + ";)Z";
            MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, "isA", sig, null, null);
            
            Label fail = new Label();
            
            for (int i = 0; i < n; i++) {
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitMethodInsn(INVOKEINTERFACE, any_tuple_n, TUPLE_OBJECT_ELT_PFX + (Naming.TUPLE_ORIGIN + i), UNTYPED_GETTER_SIG);
                
                String cast_to = parameters.get(i);

                generalizedInstanceOf(mv, cast_to);
                
                mv.visitJumpInsn(Opcodes.IFEQ, fail);

            }
            
            mv.visitIntInsn(BIPUSH, 1);
            mv.visitInsn(Opcodes.IRETURN);
            
            mv.visitLabel(fail);
            mv.visitIntInsn(BIPUSH, 0);
            mv.visitInsn(Opcodes.IRETURN);
            mv.visitMaxs(Naming.ignoredMaxsParameter, Naming.ignoredMaxsParameter);
            mv.visitEnd();
        }
        
        // cast method
        {
            String sig = "(L" + any_tuple_n + ";)L"+tuple_params+";";
            String make_sig = toJvmSig(parameters, Naming.javaDescForTaggedFortressType(tuple_params));
            
            MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, CAST_TO, sig, null, null);
                        
            // Get the parameters to make, and cast them.
            for (int i = 0; i < n; i++) {
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitMethodInsn(INVOKEINTERFACE, any_tuple_n, TUPLE_OBJECT_ELT_PFX + (Naming.TUPLE_ORIGIN + i), UNTYPED_GETTER_SIG);
                String cast_to = parameters.get(i);
                generalizedCastTo(mv, cast_to);
            }
            
            mv.visitMethodInsn(INVOKESTATIC, dename, "make", make_sig);
            
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(Naming.ignoredMaxsParameter, Naming.ignoredMaxsParameter);
            mv.visitEnd();
        }
        
        // typed getters
        // untyped getters
        for (int i = 0; i < n; i++) {
            String untyped = TUPLE_OBJECT_ELT_PFX + (Naming.TUPLE_ORIGIN + i);
            String typed = TUPLE_TYPED_ELT_PFX + (Naming.TUPLE_ORIGIN + i);
            String field = TUPLE_FIELD_PFX + (Naming.TUPLE_ORIGIN + i);
            String param_type = parameters.get(i);
            String param_desc = "L" + param_type + ";";
            {
                MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC , untyped, UNTYPED_GETTER_SIG, null, null);
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, dename, field, param_desc);
                mv.visitInsn(Opcodes.ARETURN);
                mv.visitMaxs(Naming.ignoredMaxsParameter, Naming.ignoredMaxsParameter);
                mv.visitEnd();
            }
            {
                MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC , typed, "()" + param_desc, null, null);
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, dename, field, param_desc);
                mv.visitInsn(Opcodes.ARETURN);
                mv.visitMaxs(Naming.ignoredMaxsParameter, Naming.ignoredMaxsParameter);
                mv.visitEnd();
            }
        }

        
        cw.visitEnd();

        return cw.toByteArray();
}

    /**
     * @param parameters
     * @return
     */
    private static String stringListToTuple(List<String> parameters) {
        return stringListToGeneric("Tuple", parameters);
    }

    private static String stringListToArrow(List<String> parameters) {
        return stringListToGeneric("Arrow", parameters);
    }

    private static String stringListToGeneric(String what, List<String> parameters) {
        return what + Useful.listInDelimiters(Naming.LEFT_OXFORD, parameters, Naming.RIGHT_OXFORD, ";");
    }

    private static String stringListToGenericOfObjects(String what, List<String> parameters) {
        return what + Useful.listInDelimiters(Naming.LEFT_OXFORD, parameters, Naming.RIGHT_OXFORD, ";");
    }


    static Pair<Integer, Integer> make(Integer a, Integer b) {
        return new Pair<Integer, Integer>(a, b);
    }
    
    /**
     * @param mv
     * @param cast_to
     */
    public static void generalizedInstanceOf(MethodVisitor mv, String cast_to) {
        if (cast_to.startsWith("Tuple" + Naming.LEFT_OXFORD)) {
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "Concrete"+cast_to, "isA", "(Ljava/lang/Object;)Z");
        } else {
            mv.visitTypeInsn(Opcodes.INSTANCEOF, cast_to);
        }
    }

    /**
     * @param mv
     * @param cast_to
     */
    public static void generalizedCastTo(MethodVisitor mv, String cast_to) {
        if (cast_to.startsWith(TUPLE_OX)) {
            List<String> cast_to_parameters = extractStringParameters(cast_to);
            String any_tuple_n = "AnyTuple" + Naming.LEFT_OXFORD + cast_to_parameters.size() + Naming.RIGHT_OXFORD;
            String sig = "(L" + any_tuple_n + ";)L" + cast_to + ";";
            mv.visitTypeInsn(Opcodes.CHECKCAST, any_tuple_n);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "Concrete"+cast_to, CAST_TO, sig);
        } else if (cast_to.startsWith(TUPLE_OX)) {
            List<String> cast_to_parameters = extractStringParameters(cast_to);
            mv.visitTypeInsn(Opcodes.CHECKCAST, cast_to);
            
            String any_tuple_n = "Arrow" + Naming.LEFT_OXFORD + cast_to_parameters.size() + Naming.RIGHT_OXFORD;
            String sig = "(L" + any_tuple_n + ";)L" + cast_to + ";";
           // mv.visitTypeInsn(Opcodes.CHECKCAST, any_tuple_n);
           // mv.visitMethodInsn(Opcodes.INVOKESTATIC, "Concrete"+cast_to, "castTo", sig);

        } else {
            mv.visitTypeInsn(Opcodes.CHECKCAST, cast_to);
        }
    }

    /**
     * @param parameters
     * @return
     */
    private static String arrowParamsToJVMsig(List<String> parameters) {
        int l = parameters.size();
        return toJvmSig(parameters.subList(0, l-1),
                Naming.javaDescForTaggedFortressType(parameters.get(l-1)));
    }

    /**
     * @param parameters
     * @return
     */
    private static String tupleParamsToJvmInitSig(List<String> parameters) {
        return toJvmSig(parameters, "V");
    }

    /**
     * @param parameters
     * @return
     */
    private static String toJvmSig(List<String> parameters, String rt) {
        String sig = "(";

        int l = parameters.size();

        StringBuilder buf = new StringBuilder();
        buf.append(sig);
        for (int i = 0; i < l; i++) {
            String s = parameters.get(i);
            if (! s.equals(Naming.INTERNAL_SNOWMAN))
                buf.append(Naming.javaDescForTaggedFortressType(parameters.get(i)));
        }
        sig = buf.toString();
        sig += ")";
        // nothing special here, yet, but AbstractArrow will be different.
        sig += rt;
        return sig;
    }


    
    static boolean isExpanded(String className) {
        int left = className.indexOf(Naming.LEFT_OXFORD);
        int right = className.indexOf(Naming.RIGHT_OXFORD);
        return (left != -1 && right != -1 && left < right);
    }
    
    Pair<String, List<Pair<String, String>>> xlationForGeneric(String t) {
        String template_name = genericTemplateName(t, null);

        Pair<String, List<Pair<String, String>>> pslpss = stemToXlation.get(template_name);
        
        if (pslpss != null) return pslpss;
        
        try {
            pslpss =
                Naming.xlationSerializer.fromBytes(readResource(template_name, "xlation"));
        } catch (VersionMismatch e) {
            throw new Error("Read stale serialized data for " + template_name + ", recommend you delete the Fortress bytecode cache and relink", e);
        } catch (IOException e) {
            throw new Error("Unable to read serialized data for " + template_name + ", recommend you delete the Fortress bytecode cache and relink", e);
        }
        
        synchronized(stemToXlation) {
            if (stemToXlation.get(template_name) == null) {
                stemToXlation.put(template_name, pslpss);
            }
        }
        
        return pslpss;

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
