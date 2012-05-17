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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.jar.JarOutputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;
import org.objectweb.asm.tree.*;

import com.sun.fortress.compiler.NamingCzar;
import com.sun.fortress.compiler.codegen.ManglingClassWriter;
import com.sun.fortress.compiler.codegen.ManglingMethodVisitor;
import com.sun.fortress.compiler.nativeInterface.SignatureParser;
import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.useful.BATree;
import com.sun.fortress.useful.DefaultComparator;
import com.sun.fortress.useful.F;
import com.sun.fortress.useful.FnVoid;
import com.sun.fortress.useful.FnVoidVoid;
import com.sun.fortress.useful.InfiniteList;
import com.sun.fortress.useful.Pair;
import com.sun.fortress.useful.Triple;
import com.sun.fortress.useful.Useful;
import com.sun.fortress.useful.VersionMismatch;

/**
 * This code steals willy-nilly from the NextGen class loader.
 *
 * @author dr2chase
 */
public class InstantiatingClassloader extends ClassLoader implements Opcodes {

     public static final String IS_A = "isA";

    private static final String CAST_TO = "castTo";
    public static final String TUPLE_TYPED_ELT_PFX = "e";
    public static final String TUPLE_OBJECT_ELT_PFX = "o";
    public static final String TUPLE_FIELD_PFX = "f";
    public static final String ABSTRACT_ = "Abstract";
    public static final String CONCRETE_ = "Concrete";
    public static final String ABSTRACT_ARROW = ABSTRACT_ + Naming.ARROW_TAG;
    public static final String WRAPPED_ARROW = "Wrapped" + Naming.ARROW_TAG;
    
    public static final String CONCRETE_TUPLE = CONCRETE_ + Naming.TUPLE_TAG;
    public static final String ANY_CONCRETE_TUPLE = "Any" + CONCRETE_ + Naming.TUPLE_TAG;
    public static final String ANY_TUPLE = "Any" + Naming.TUPLE_TAG;
    
    public static final int JVM_BYTECODE_VERSION = Opcodes.V1_6;
    // TODO make this depends on properties/env w/o dragging in all of the world.
    private static final boolean LOG_LOAD_CHOICES = false;
    static final boolean LOG_LOADS = false;
    private static final boolean LOG_FUNCTION_EXPANSION = false;
    public final static String SAVE_EXPANDED_DIR = ProjectProperties.getDirectory("fortress.bytecodes.expanded.directory", null);
    public static JarOutputStream SAVE_EXPANDED_JAR = null;
    static {
        try {
            if (SAVE_EXPANDED_DIR != null)
                SAVE_EXPANDED_JAR = new JarOutputStream(new BufferedOutputStream( new FileOutputStream(SAVE_EXPANDED_DIR + "/" + "expanded.jar")));
        } catch (IOException ex) {
            System.err.println("Failed to open jar file in " + SAVE_EXPANDED_DIR + " for expanded bytecodes");
        }
    }
    
    public static void exitProgram() {
        if (SAVE_EXPANDED_JAR != null) {
            try {
                SAVE_EXPANDED_JAR.close();
            } catch (IOException e) {
                System.err.println("Failed to close jar file in " + SAVE_EXPANDED_DIR + " for expanded bytecodes");
            }
        }
    }
   
    
    public final static InstantiatingClassloader ONLY =
        new InstantiatingClassloader(Thread.currentThread().getContextClassLoader());

    private final static ClassLoadChecker _classLoadChecker = new ClassLoadChecker();

    private final Vector<String> history = new Vector<String>();

    private final Hashtable<String,  Naming.XlationData>
       stemToXlation = new Hashtable<String, Naming.XlationData>();
    
    private InstantiatingClassloader() {
        throw new Error(); // Really do not call this.
    }

    private InstantiatingClassloader(ClassLoader parent) {
        super(parent);
        // System.err.println("I am the one true class loader!");
        String p = System.getProperty("I_can_haz_classloader");
        if (p != null)
            throw new Error("Second classloader detected!!");
        System.setProperty("I_can_haz_classloader", "initialized");
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
        String fileName = Naming.dotToSep(className) + "." + suffix;

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
   
        if (history.contains(name)) { 
            if (LOG_LOAD_CHOICES)
                System.err.println("Cached load for " + name);
            Class c = this.findLoadedClass(name);
            return c;
        }

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
            if (LOG_LOAD_CHOICES)
                System.err.println("System loader for " + name);
            clazz = findSystemClass(name);
        } else {
            history.add(name);
            if (LOG_LOAD_CHOICES)
                System.err.println("Custom load for " + name);
            if (name.equals("com.sun.fortress.runtimeSystem.InstantiatingClassloader"))
                 new Error(); // why are we here?
            
            byte[] classData = null;
            try {
                boolean isClosure = name.contains(Naming.ENVELOPE);
                boolean isGenericFunction = name.contains(Naming.GEAR);
                boolean isGenericOxford = isExpandedOx(name);
                boolean isGenericAngle = isExpandedAngle(name);
                boolean isGeneric = isGenericOxford || isGenericAngle;
                boolean isRTTIc = name.endsWith(Naming.RTTI_CLASS_SUFFIX);
                
                char left_char = isGenericOxford ?
                        Naming.LEFT_OXFORD_CHAR : Naming.LEFT_HEAVY_ANGLE_CHAR;
                char right_char = isGenericOxford ?
                        Naming.RIGHT_OXFORD_CHAR : Naming.RIGHT_HEAVY_ANGLE_CHAR;

                boolean expanded = (isGeneric || isGenericFunction || isClosure) ;
                if (name.startsWith(Naming.TUPLE_RTTI_TAG)) {
                    classData = instantiateTupleRTTI(name);
                    expanded = true;
                } else if (name.startsWith(Naming.ARROW_RTTI_TAG)) {
                	classData = instantiateArrowRTTI(name);
                	expanded = true;
                } else if (isGenericFunction) {
                    // also a closure
                    String dename = Naming.dotToSep(name);
                    dename = Naming.demangleFortressIdentifier(dename);
                    ArrayList<String> sargs = new ArrayList<String>();
                    String template_name = functionTemplateName(dename, sargs);
                    Naming.XlationData xldata = xlationForFunction(dename);
                    classData = readAndExpandGenericThing(dename, sargs, xldata,
                            template_name);
                    
                } else if (isClosure) {
                    classData = instantiateClosure(Naming.demangleFortressIdentifier(name));
                } else if (isGeneric) {
                    String dename = Naming.dotToSep(name);
                    dename = Naming.demangleFortressIdentifier(dename);
                    int left = dename.indexOf(left_char);
                    int right = dename.lastIndexOf(right_char);
                    String stem = dename.substring(0,left);
                    List<String> parameters = RTHelpers.extractStringParameters(dename, left, right);
                    if (stem.equals(Naming.ARROW_TAG)) {
                        // Arrow interface
                        classData = instantiateArrow(dename, parameters);
                        resolve = true;
                    } else if (stem.equals(ABSTRACT_ARROW)) {
                        // Arrow boilerplate
                        classData = instantiateAbstractArrow(dename, parameters);
                    } else if (stem.equals(WRAPPED_ARROW)) {
                        classData = instantiateWrappedArrow(dename, parameters);
                    } else if (stem.equals(Naming.TUPLE_TAG)) {
                        classData = instantiateTuple(dename, parameters);
                    } else if (stem.equals(CONCRETE_TUPLE)) {
                        classData = instantiateConcreteTuple(dename, parameters);
                    } else if (stem.equals(ANY_TUPLE)) {
                        classData = instantiateAnyTuple(dename, parameters);
                    } else if (stem.equals(ANY_CONCRETE_TUPLE)) {
                        classData = instantiateAnyConcreteTuple(dename, parameters);
                    } else if (stem.equals(Naming.UNION)) {
                        classData = instantiateUnion(dename, parameters);
                    } else {
                        Naming.XlationData xldata =
                                xlationForGeneric(dename, left_char, right_char);
                        ArrayList<String> sargs = new ArrayList<String>();
                        String template_name = genericTemplateName(dename, left_char, right_char, sargs); // empty sargs
                        classData = readAndExpandGenericThing(dename, sargs, xldata,
                                template_name);
                        
                        // throw new ClassNotFoundException("Don't know how to instantiate generic " + stem + " of " + parameters);
                    }
                } else if (isRTTIc) {
                    // Even if not generic, translate these to flush out symbolic references.
                    String dename = Naming.dotToSep(name);
                    dename = Naming.demangleFortressIdentifier(dename);
                    Naming.XlationData xldata = new Naming.XlationData(Naming.RTTI_GENERIC_TAG);
                    ArrayList<String> sargs = new ArrayList<String>();
                    
                    classData = getClass(name);
                    classData = readAndExpandGenericThing(dename, sargs,
                            xldata, classData);
                    
                } else {
                
                	classData = getClass(name);
                	/*//System.out.println("Getting class: " + name);
                    classData = getClass(name);
                    
                    ClassReader cr = new ClassReader(classData);
                    ClassNode classADT = new ClassNode();
                    cr.accept(classADT, 0);
                    
                    if (!(name.replace('.','/')).equals(classADT.name)) {
                    	System.out.println("Renaming on the fly :-)");
                    	classADT.name = name;
                    }
                    
                    ClassWriter cw = new ClassWriter(0);
                    classADT.accept(cw);
                    classData = cw.toByteArray();*/
                }
                
                if (expanded && SAVE_EXPANDED_JAR != null) {

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
     * @param dename
     * @param sargs
     * @param template_name
     * @param xldata
     * @return
     * @throws IOException
     */
    private byte[] readAndExpandGenericThing(String dename,
            ArrayList<String> sargs,
            Naming.XlationData xldata,
            String template_name) throws IOException {
        byte[] templateClassData = readResource(template_name);
            // Naming.XlationData.fromBytes(readResource(template_name, "xlation"));
        
        byte[] classData = readAndExpandGenericThing(dename, sargs, xldata,
                templateClassData);

        return classData;
    }

    /**
     * @param dename
     * @param sargs
     * @param xldata
     * @param templateClassData
     * @return
     */
    private byte[] readAndExpandGenericThing(String dename,
            ArrayList<String> sargs, Naming.XlationData xldata,
            byte[] templateClassData) {
        List<String> xl = xldata.staticParameterNames();
               
        Map<String, String> xlation  = Useful.map(xl, sargs);
        Map<String, String> opr_xlation  = Useful.map(xl, sargs, xldata.isOprKind());
        
        ManglingClassWriter cw = new ManglingClassWriter(ClassWriter.COMPUTE_MAXS|ClassWriter.COMPUTE_FRAMES);
        ClassReader cr = new ClassReader(templateClassData);
        ClassVisitor cvcw = LOG_FUNCTION_EXPANSION ?
            new TraceClassVisitor((ClassVisitor) cw, new PrintWriter(System.err)) :
                cw;
        Instantiater instantiater = new Instantiater(cvcw, xlation, opr_xlation, dename, this);
        cr.accept(instantiater, 0);
        byte[] classData = cw.toByteArray();
        if (ProjectProperties.getBoolean("fortress.bytecode.verify", false)) {
            try {
                PrintWriter pw = new PrintWriter(System.out);
                CheckClassAdapter.verify(new ClassReader(classData), true, pw);
            } catch (Throwable th) {
                th.printStackTrace();
            }
        }
        return classData;
    }

    private String functionTemplateName(String name, ArrayList<String> sargs) {
        int left_oxford = name.indexOf(Naming.LEFT_OXFORD);
        int right_oxford = name.indexOf(Naming.ENVELOPE) - 1; // right oxford

        String s = InstantiationMap.canonicalizeStaticParameters(name, left_oxford,
                right_oxford, sargs);

        return Naming.mangleFortressIdentifier(s);
    }

    /**
     * Parses a method/function name (which can in certain cases also be a 
     * class name,for instance, closures and generics) into the "name" of the 
     * method, and 
     * @param name
     * @param tag
     * @param sargs
     * @return
     */
    private String functionTemplateNameDetailed(String name, StringBuffer tag, ArrayList<String> sargs) {
        int leftBracket = name.indexOf(Naming.LEFT_OXFORD);
        int rightBracket = name.indexOf(Naming.ENVELOPE) - 1; // right oxford

        int depth = 0;
        
        {
            int i = 0;
            while (true) {
                char ch = name.charAt(i);
                if (ch == Naming.RIGHT_OXFORD_CHAR) {
                    
                } else if (ch == Naming.LEFT_OXFORD_CHAR) {
                    
                } else if (ch == Naming.RIGHT_PAREN_ORNAMENT_CHAR) {
                    
                } else if (ch == Naming.LEFT_PAREN_ORNAMENT_CHAR) {
                    
                } else {
                    break;
                }
            }
        }
        
        
        int pbegin = leftBracket+1;
        for (int i = leftBracket+1; i <= rightBracket; i++) {
            char ch = name.charAt(i);
    
            if ((ch == Naming.GENERIC_SEPARATOR_CHAR || ch == Naming.RIGHT_OXFORD_CHAR) && depth == 1) {
                String parameter = name.substring(pbegin,i);
                if (sargs != null)
                    sargs.add(parameter);
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
        
        
        String s = InstantiationMap.canonicalizeStaticParameters(name, leftBracket,
                rightBracket, sargs);

        return Naming.mangleFortressIdentifier(s);
    }

    private String genericTemplateName(String name, ArrayList<String> sargs) {
        return genericTemplateName(name, Naming.LEFT_OXFORD_CHAR,
                Naming.RIGHT_OXFORD_CHAR, sargs);
    }

    private String genericTemplateName(String name, char left_char, char right_char, ArrayList<String> sargs) {
        int left_oxford = name.indexOf(left_char);
        int right_oxford = name.lastIndexOf(right_char);

        String s = InstantiationMap.canonicalizeStaticParameters(name, left_oxford,
                right_oxford, sargs);

        return Naming.mangleFortressIdentifier(s);
    }

    private static byte[] instantiateClosure(String name) {
        ManglingClassWriter cw = new ManglingClassWriter(ClassWriter.COMPUTE_MAXS|ClassWriter.COMPUTE_FRAMES);

        ArrayList<InitializedStaticField> isf_list = new ArrayList<InitializedStaticField>();
        
        closureClassPrefix(name, cw, null, null, true, null, isf_list);
        
//        //RTTI getter
//        {
//        	MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, Naming.RTTI_GETTER,
//                    "()" + Naming.RTTI_CONTAINER_DESC,
//                    null, null);
//        	mv.visitCode();
//        	mv.visitFieldInsn(GETSTATIC, name, Naming.RTTI_FIELD, Naming.RTTI_CONTAINER_DESC);
//        	mv.visitInsn(ARETURN);
//        	mv.visitMaxs(1, 1);
//        	mv.visitEnd();
//        }
        
        optionalStaticsAndClassInitForTO(isf_list, cw);
        
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
            String forceCastParam0,
            List<InitializedStaticField> statics) {
        return closureClassPrefix(name, cw, staticClass, sig, false, forceCastParam0, statics);
        
    }
        public static String closureClassPrefix(String name,
                                          ManglingClassWriter cw,
                                          String staticClass,
                                          String sig,
                                          boolean is_forwarding_closure,
                                          String forceCastParam0,
                                          List<InitializedStaticField> statics) {
        int env_loc = name.indexOf(Naming.ENVELOPE);
        int last_dot = name.substring(0,env_loc).lastIndexOf('$');

        String api = name.substring(0,last_dot);
        String suffix = name.substring(last_dot+1);
        env_loc = suffix.indexOf(Naming.ENVELOPE); // followed by $
        String fn = is_forwarding_closure ? suffix.substring(0,env_loc): Naming.APPLIED_METHOD; 
        String ft = suffix.substring(env_loc+2); // skip $ following ENVELOPE

        // Normalize out leading HEAVY_CROSS, if there is one.
        {
            if (ft.charAt(0) == Naming.HEAVY_CROSS_CHAR)
                ft = ft.substring(1);
            int left = ft.indexOf(Naming.LEFT_OXFORD);
            int right = ft.lastIndexOf(Naming.RIGHT_OXFORD);
            List<String> parameters = RTHelpers.extractStringParameters(ft, left, right);

            Triple<List<String>, List<String>, String> stuff =
                normalizeArrowParameters(parameters);
            List<String> flat_params_and_ret = stuff.getA();

            if (flat_params_and_ret.size() == 2 && flat_params_and_ret.get(0).equals(Naming.INTERNAL_SNOWMAN))
                flat_params_and_ret = flat_params_and_ret.subList(1,2);

            if (sig == null)
                sig = arrowParamsToJVMsig(flat_params_and_ret);
        }
        
        SignatureParser sp = new SignatureParser(sig);


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
        String superClass = ABSTRACT_+ft; // ft is assumed to begin with "Arrow"
        name = api.replace(".", "/") + '$' + suffix;
        final String final_name = name;
        
        //String desc = Naming.internalToDesc(name);
        final String field_desc = Naming.internalToDesc(ft);
        // Begin with a class
        cw.visit(JVM_BYTECODE_VERSION, ACC_PUBLIC + ACC_SUPER, name, null, superClass, null);

        statics.add(new InitializedStaticField.StaticForClosureField(field_desc, final_name));
        
        //RTTI field
//        statics.add(new InitializedStaticField() {
//
//            @Override
//            public void forClinit(MethodVisitor init_mv) {
//            	MethodInstantiater mi = new MethodInstantiater(init_mv, null, null);
//            	mi.rttiReference(final_name + Naming.RTTI_CLASS_SUFFIX);
//            	init_mv.visitFieldInsn(PUTSTATIC, final_name, Naming.RTTI_FIELD, Naming.RTTI_CONTAINER_DESC);
//            }
//
//            @Override
//            public String asmName() {
//                return Naming.RTTI_FIELD;
//            }
//
//            @Override
//            public String asmSignature() {
//                return Naming.RTTI_CONTAINER_DESC;
//            }});
 
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

        if (LOG_LOADS)
            System.err.println(name + ".apply" + sig + " concrete\nparams = " + sp);

        // KBN 06/2011 handled above now
        // Monkey business to deal with case of "void" args.
        //int sz = parameters.size();
        // Last parameter is actually result type!
        // But we need to include an extra arg in sz to represent the closure itself (this).
       // if (sz==2 && Naming.INTERNAL_SNOWMAN.equals(parameters.get(0))) {
            // Arity 1 (sz 2) with void parameter should actually be arity 0 (sz 1).
        //    sz = 1;
        //}

        // Emit a method with well-known name ("apply", most likely)
        // to forward calls from the instance to the static, which our
        // caller will supply.  Note that the static class can be a
        // different class.
        forwardingMethod(cw, Naming.APPLY_METHOD, ACC_PUBLIC, 0,
                staticClass, fn, INVOKESTATIC,
                sig, sig, sp.paramCount()+1, false, forceCastParam0);
        
        return fn;

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
     * @param maximalSig     the signature of the generated (forwarding) method
     * @param selfCastSig    a full signature containing self at selfIndex
     * @param nparamsIncludingSelf number of parameters, including self (if any)
     * @param pushSelf       if true, push self first, using selfIndex to find it
     * @param forceCastParam0 cast param 0, even if it is not self.  This is for
     *                        implementation of generic methods.  It may need
     *                        to be generalized to all params, not entirely sure.
     * 
     * Create forwarding method that re-pushes its arguments and
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
    
    public static void forwardingMethod(ClassWriter cw,
            String thisName, int thisModifiers, int selfIndex,
            String fwdClass, String fwdName, int fwdOp,
            String thisSig, String fwdSig, String selfCastSig,
            int nparamsIncludingSelf, boolean pushSelf, String forceCastParam0) {
        forwardingMethod(cw, thisName, thisModifiers, selfIndex,
                fwdClass, fwdName, fwdOp, thisSig, fwdSig, selfCastSig,
                nparamsIncludingSelf, pushSelf, forceCastParam0, false
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
     * @param castReturn      cast the return type to what it "ought" to be.
     *                        deals with BottomType case.  Makes verifier happy.
     */
    public static void forwardingMethod(ClassWriter cw,
                                        String thisName, int thisModifiers, int selfIndex,
                                        String fwdClass, String fwdName, int fwdOp,
                                        String thisSig, String fwdSig, String selfCastSig,
                                        int nparamsIncludingSelf, boolean pushSelf,
                                        String forceCastParam0, boolean castReturn) {
        String selfSig = null;
        if (pushSelf) {
            if (selfCastSig != null) {
                selfSig = Naming.nthSigParameter(selfCastSig, selfIndex);
                selfSig = selfSig.substring(1, selfSig.length()-1);
            }
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
            fwdSig = Naming.replaceNthSigParameter(fwdSig, 0, Naming.internalToDesc(forceCastParam0));
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
            if (selfSig != null)
                mv.visitTypeInsn(CHECKCAST, selfSig);
            if (fwdOp == INVOKESTATIC)
                parsed_arg_cursor++;
        }
        
        pushParamsNotSelf(selfIndex, nparamsIncludingSelf, forceCastParam0, mv,
                sp, parsed_arg_cursor);
        
        mv.visitMethodInsn(fwdOp, fwdClass, fwdName, fwdSig);
        // optional CAST here for tuple and arrow
        String tyName = Naming.sigRet(thisSig);
        if (tyName.startsWith(Naming.TUPLE_OX) ||
                tyName.startsWith(Naming.ARROW_OX) ||
                castReturn) {
            String tyNameFrom = Naming.sigRet(fwdSig);
            InstantiatingClassloader.generalizedCastTo(mv, tyName);
        }

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
        List<String> parsed_args = sp.getJVMArguments();
        int i_bump = 0;
        for (int i = 0; i < nparamsIncludingSelf; i++) {
            if (i==selfIndex) continue;
            String one_param = parsed_args.get(parsed_arg_cursor++);
            int load_op = SignatureParser.asm_loadop(one_param);
            mv.visitVarInsn(load_op, i + i_bump);
            // TODO Need to get counting right here.  P0 is "really" P1
            if (i == 1 && forceCastParam0 != null) {
                mv.visitTypeInsn(CHECKCAST, forceCastParam0);
            }
            // if one_param is long or double, increment i_bump to account for the extra slot.
            i_bump += SignatureParser.width(one_param) - 1;
        }
    }

    public static void eep(MethodVisitor mv, String s) {
        mv.visitLdcInsn(s);
        mv.visitMethodInsn(INVOKESTATIC, "com/sun/fortress/runtimeSystem/InstantiatingClassloader", "eep", "(Ljava/lang/String;)V");
    }
    
    public static void eepI(MethodVisitor mv, String s) {
        mv.visitLdcInsn(s);
        mv.visitMethodInsn(INVOKESTATIC, "com/sun/fortress/runtimeSystem/InstantiatingClassloader", "eep", "(Ljava/lang/String;I)I");
    }     
    
    public static void eep(String s) {
        System.err.println(s);
    }
    
    public static int eep(String s, int i) {
        System.err.println(s + i);
        return i;
    }
    
     public static void eep(MethodVisitor mv) {
         mv.visitMethodInsn(INVOKESTATIC, "com/sun/fortress/runtimeSystem/InstantiatingClassloader", "eep", "(Ljava/lang/Throwable;)V");
     } 

     public static void fail(MethodVisitor mv, String s) {
         System.err.println("Warning, emitting fail case for '" + s + "'");
         mv.visitLdcInsn(s);
         mv.visitMethodInsn(INVOKESTATIC, "com/sun/fortress/runtimeSystem/InstantiatingClassloader", "fail", "(Ljava/lang/String;)Ljava/lang/Error;");
         mv.visitInsn(ATHROW);
     }     

     public static void eep(Throwable t) {
         t.printStackTrace();

     }
     
     static Error error(String s) {
         return new Error(s);
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
    
    /**
     * Creates the interface for an Arrow type.  An Arrow interface includes
     * 2 or 3 methods.  One is the simple domain-to-range "apply", where domain
     * is the determined by the parameters of the generic, taken as is.
     * The next "apply" method replaces all types in the domain and range
     * with java/lang/Object, for use in certain contexts (coerced arrows
     * for casts, also for dynamically instantiated generic functions).  The
     * third apply method is generated if there is more than parameter to the
     * function, in which case the parameters are wrapped in a tuple, or if the
     * first parameter is a Tuple, in which case they will be unwrapped.
     * 
     * For example, Arrow[\T;U;V\] will have the apply methods (ignore
     * both Fortress and JVM dangerous characters mangling issues for now):
     * 
     * apply(LT;LU;)LV;
     * 
     * apply(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
     * 
     * apply(LTuple[\T;U\];)LV
     * 
     * @param name
     * @param parameters
     * @return
     */
    
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
        } else {
        	super_interfaces = new String[] { "fortress/AnyType$Any" };
        }
        
        cw.visit(JVM_BYTECODE_VERSION, ACC_PUBLIC + ACC_ABSTRACT + ACC_INTERFACE,
                 name, null, "java/lang/Object", super_interfaces);

        /* If more than one domain parameter, then also include the tupled apply method. */
        int l = parameters.size();
        if (tupled_parameters != null) {
            String sig = arrowParamsToJVMsig(tupled_parameters);
            if (LOG_LOADS) System.err.println(name+".apply"+sig+" abstract");
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC + ACC_ABSTRACT, Naming.APPLY_METHOD,
                                sig,
                                null, null);
            mv.visitEnd();
        }
        
        {      
            String sig;
            if (parameters.size() == 2 && parameters.get(0).equals(Naming.INTERNAL_SNOWMAN))
            	sig = arrowParamsToJVMsig(parameters.subList(1,2));
            else
            	sig = arrowParamsToJVMsig(unwrapped_parameters);
            if (LOG_LOADS) System.err.println(name+".apply"+sig+" abstract");
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC + ACC_ABSTRACT, Naming.APPLY_METHOD,
                                sig,
                                null, null);
            mv.visitEnd();
        }
        {      
            String sig = "()"+Naming.internalToDesc(obj_sig);
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
        
        List<String> flat_params_and_ret = stuff.getA();
        List<String> tupled_params_and_ret = stuff.getB();
        String tupleType = stuff.getC();

        List<String> flat_obj_params_and_ret = Useful.applyToAll(flat_params_and_ret, toJLO);
        List<String> norm_obj_params_and_ret = normalizeArrowParametersAndReturn(flat_obj_params_and_ret);
        List<String> norm_params_and_ret = normalizeArrowParametersAndReturn(flat_params_and_ret);

        String extendsClass = stringListToGeneric(ABSTRACT_ARROW, norm_params_and_ret);
        
        // List<String> objectified_parameters = Useful.applyToAll(flat_params_and_ret, toJLO);
        //String obj_sig = stringListToGeneric("AbstractArrow", objectified_parameters);
        String obj_intf_sig = stringListToGeneric(Naming.ARROW_TAG, norm_obj_params_and_ret);
        String wrappee_name = "wrappee";
        
        //extends AbstractArrow[\parameters\]
        cw.visit(JVM_BYTECODE_VERSION, ACC_PUBLIC + ACC_SUPER, name, null,
                extendsClass, null);

        // private final Arrow[\Object...Object\] wrappee
        cw.visitField(Opcodes.ACC_PRIVATE + Opcodes.ACC_FINAL, wrappee_name,
                Naming.internalToDesc(obj_intf_sig), null /* for non-generic */, null /* instance has no value */);

        // WrappedArrow[\parameters\](Arrow[\Object...Object\] _wrappee)
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "(" + Naming.internalToDesc(obj_intf_sig) + ")V", null, null);
        mv.visitCode();
        // super()
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, extendsClass, "<init>", "()V");
        // this.wrappee = wrappee
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitFieldInsn(PUTFIELD, name, wrappee_name, Naming.internalToDesc(obj_intf_sig));
        // done
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        // getWrappee
        
        mv = cw.visitMethod(ACC_PUBLIC, getWrappee,
                "()"+Naming.internalToDesc(obj_intf_sig),
                null, null);
        
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, name, wrappee_name, Naming.internalToDesc(obj_intf_sig));
        mv.visitInsn(ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        //  public range_parameter apply( domain_parameters ) = 
        //    (range_parameter) wrappee.apply( domain_parameters )
        
        String flattened_apply_sig;
        if (parameters.size() == 2 && parameters.get(0).equals(Naming.INTERNAL_SNOWMAN))
        	flattened_apply_sig = arrowParamsToJVMsig(parameters.subList(1,2));
        else
        	flattened_apply_sig= arrowParamsToJVMsig(flat_params_and_ret);
       
        String obj_apply_sig = arrowParamsToJVMsig(flat_obj_params_and_ret);
  
        mv = cw.visitMethod(ACC_PUBLIC, Naming.APPLY_METHOD,
                flattened_apply_sig,
                null, null);
        mv.visitCode();

        // load wrappee for delegation
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, name, wrappee_name, Naming.internalToDesc(obj_intf_sig));
        
        // Push parameters.
        // i is indexed so that it corresponds to parameters pushed, even though
        // the types are ignored here (for now).
        for (int i = 0; i < flat_params_and_ret.size()-1; i++) {
            String t = flat_params_and_ret.get(i);
            if (!t.equals(Naming.INTERNAL_SNOWMAN)) {
                mv.visitVarInsn(ALOAD, i+1);
            } else {
                /* we are calling the object-interface version of this,
                 * we need something on the stack, or else it will fail.
                 * 
                 * This is also a naming/refactoring FAIL; this information
                 * needs to come from somewhere else.
                */
                mv.visitInsn(Opcodes.ACONST_NULL);
//                mv.visitMethodInsn(Opcodes.INVOKESTATIC,
//                        Naming.runtimeValues + "FVoid", "make",
//                        "()" + Naming.internalToDesc(Naming.runtimeValues + "FVoid"));
            }
        }

        mv.visitMethodInsn(INVOKEINTERFACE, obj_intf_sig, Naming.APPLY_METHOD, obj_apply_sig);
        
        // mv.visitTypeInsn(Opcodes.CHECKCAST, parameters.get(parameters.size()-1));
        generalizedCastTo(mv, flat_params_and_ret.get(flat_params_and_ret.size()-1));
        
        // done
        mv.visitInsn(ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        //getRTTI - forwards to wrapped arrow
        {
        	mv = cw.visitMethod(ACC_PUBLIC, Naming.RTTI_GETTER,
                    "()" + Naming.RTTI_CONTAINER_DESC,
                    null, null);
        	mv.visitCode();
        	// load wrappee for delegation
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, name, wrappee_name, Naming.internalToDesc(obj_intf_sig));
            
            //invoke interface getRTTI method
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, Naming.ANY_TYPE_CLASS, Naming.RTTI_GETTER, Naming.STATIC_PARAMETER_GETTER_SIG);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        
        }
        cw.visitEnd();

        return cw.toByteArray();

    }
    private byte[] instantiateAbstractArrow(String name, List<String> parameters) {
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
        
        List<String> flat_params_and_ret = stuff.getA();
        List<String> tupled_params_and_ret = stuff.getB();
        String tupleType = stuff.getC();
        
        List<String> flat_obj_params_and_ret = Useful.applyToAll(flat_params_and_ret, toJLO);
        List<String> norm_obj_params_and_ret = normalizeArrowParametersAndReturn(flat_obj_params_and_ret);
        List<String> norm_params_and_ret = normalizeArrowParametersAndReturn(flat_params_and_ret);
        
        String obj_sig = stringListToGeneric(ABSTRACT_ARROW, norm_obj_params_and_ret);
        String obj_intf_sig = stringListToGeneric(Naming.ARROW_TAG, norm_obj_params_and_ret);
        String wrapped_sig = stringListToGeneric(WRAPPED_ARROW, norm_params_and_ret);
        String typed_intf_sig = stringListToGeneric(Naming.ARROW_TAG, norm_params_and_ret);
        String unwrapped_apply_sig;

        if (parameters.size() == 2 && parameters.get(0).equals(Naming.INTERNAL_SNOWMAN))
        	unwrapped_apply_sig = arrowParamsToJVMsig(parameters.subList(1,2));
        else
        	unwrapped_apply_sig= arrowParamsToJVMsig(flat_params_and_ret);
        
        String obj_apply_sig = arrowParamsToJVMsig(flat_obj_params_and_ret);
    
        String[] interfaces = 
                  new String[] { stringListToArrow(norm_params_and_ret) }
                ;
        /*
         * Note that in the case of foo -> bar,
         * normalized = flattened, and tupled does not exist (is null).
         */
        String typed_tupled_intf_sig = tupled_params_and_ret == null ? null :
            stringListToGeneric(Naming.ARROW_TAG, tupled_params_and_ret);
        String objectified_tupled_intf_sig =
            tupled_params_and_ret == null ? null :
            stringListToGeneric(Naming.ARROW_TAG,
                        Useful.applyToAll(tupled_params_and_ret, toJLO));

        boolean is_all_objects = norm_obj_params_and_ret.equals(norm_params_and_ret);
                  
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
            
            int unwrapped_l = flat_params_and_ret.size();

            for (int i = 0; i < unwrapped_l-1; i++) {
                String t = flat_params_and_ret.get(i);
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
        
        // is instance method -- takes an Object
        {
            String sig = "(Ljava/lang/Object;)Z";
        	MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, IS_A, sig, null, null);
            
            Label fail = new Label();
            
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitTypeInsn(Opcodes.INSTANCEOF, Naming.ANY_TYPE_CLASS);
            mv.visitJumpInsn(Opcodes.IFEQ, fail);
            
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitTypeInsn(Opcodes.CHECKCAST,Naming.ANY_TYPE_CLASS);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, name,  IS_A, "("+Naming.internalToDesc(Naming.ANY_TYPE_CLASS)+")Z");
            mv.visitInsn(Opcodes.IRETURN);
            
            mv.visitLabel(fail);
            mv.visitIntInsn(BIPUSH, 0);
            mv.visitInsn(Opcodes.IRETURN);
            
            mv.visitMaxs(Naming.ignoredMaxsParameter, Naming.ignoredMaxsParameter);
            mv.visitEnd();
        }
        
        // is instance method -- takes an Any
        {
            String sig = "(" + Naming.internalToDesc( Naming.ANY_TYPE_CLASS) + ")Z";
            MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, IS_A, sig, null, null);
            Label fail = new Label();
                        
            //get RTTI to compare to
            mv.visitFieldInsn(GETSTATIC, name, Naming.RTTI_FIELD, Naming.RTTI_CONTAINER_DESC);
            //get RTTI of object
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(INVOKEINTERFACE, Naming.ANY_TYPE_CLASS, Naming.RTTI_GETTER, "()" + Naming.RTTI_CONTAINER_DESC );
           // mv.visitJumpInsn(IFNONNULL, fail);
            mv.visitMethodInsn(INVOKEVIRTUAL,Naming.RTTI_CONTAINER_TYPE , Naming.RTTI_SUBTYPE_METHOD_NAME, Naming.RTTI_SUBTYPE_METHOD_SIG);
            
            //mv.visitIntInsn(BIPUSH, 0);
            mv.visitJumpInsn(Opcodes.IFEQ, fail);

            mv.visitIntInsn(BIPUSH, 1);
            mv.visitInsn(Opcodes.IRETURN);
            
            mv.visitLabel(fail);
            mv.visitIntInsn(BIPUSH, 0);
            mv.visitInsn(Opcodes.IRETURN);
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
                    "(" + Naming.internalToDesc(obj_intf_sig) + ")" + Naming.internalToDesc(typed_intf_sig),
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
            mv.visitMethodInsn(INVOKEINTERFACE, obj_intf_sig, getWrappee, "()"+ Naming.internalToDesc(obj_intf_sig));
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
            mv.visitMethodInsn(INVOKESPECIAL, wrapped_sig, "<init>", "(" + Naming.internalToDesc(obj_intf_sig) +")V");
            
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(Naming.ignoredMaxsParameter, Naming.ignoredMaxsParameter);
       
            mv.visitEnd();            
        } 

        if (typed_tupled_intf_sig != null )
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
                    "(" + Naming.internalToDesc(objectified_tupled_intf_sig) + ")" + Naming.internalToDesc(typed_intf_sig),
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
            mv.visitMethodInsn(INVOKEINTERFACE, objectified_tupled_intf_sig, getWrappee, "()"+ Naming.internalToDesc(objectified_tupled_intf_sig));
            mv.visitVarInsn(Opcodes.ASTORE, 0);

            // try instanceof on unwrapped
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitTypeInsn(Opcodes.INSTANCEOF, typed_intf_sig);
            mv.visitJumpInsn(Opcodes.IFEQ, not_instance2);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitInsn(Opcodes.ARETURN);
            
            // wrap and return - untupled should be okay here, since it subtypes
            mv.visitLabel(not_instance2);
            mv.visitTypeInsn(NEW, wrapped_sig);
            mv.visitInsn(DUP);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, wrapped_sig, "<init>", "(" + Naming.internalToDesc(obj_intf_sig) +")V");
            
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(Naming.ignoredMaxsParameter, Naming.ignoredMaxsParameter);
       
            mv.visitEnd();            
        }
        
        // getWrappee
        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, getWrappee,
                "()"+Naming.internalToDesc(obj_intf_sig),
                null, null);
        
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd(); // return this
        }
        
        if (tupled_params_and_ret == null) {
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
            String tupled_apply_sig = arrowParamsToJVMsig(tupled_params_and_ret);

            {
                /* Given tupled args, extract, and invoke apply. */
                
                if (LOG_LOADS) System.err.println(name + ".apply" + tupled_apply_sig+" abstract for abstract");
                MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, Naming.APPLY_METHOD,
                        tupled_apply_sig,
                        null, null);
                
                mv.visitVarInsn(Opcodes.ALOAD, 0); // closure
                
                int unwrapped_l = flat_params_and_ret.size();
                
                for (int i = 0; i < unwrapped_l-1; i++) {
                    String param = flat_params_and_ret.get(i);
                    mv.visitVarInsn(Opcodes.ALOAD, 1); // tuple
                    mv.visitMethodInsn(INVOKEINTERFACE, tupleType, TUPLE_TYPED_ELT_PFX + (Naming.TUPLE_ORIGIN + i), "()" + Naming.internalToDesc(param));
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
                
                int unwrapped_l = flat_params_and_ret.size();

                for (int i = 0; i < unwrapped_l-1; i++) {
                    mv.visitVarInsn(Opcodes.ALOAD, i+1); // element
                }

                List<String> tuple_elements = flat_params_and_ret.subList(0,unwrapped_l-1);
                
                String make_sig = toJvmSig(tuple_elements,
                                  Naming.javaDescForTaggedFortressType(tupleType));
                mv.visitMethodInsn(INVOKESTATIC, 
                        stringListToGeneric(CONCRETE_TUPLE, tuple_elements), "make", make_sig);

                mv.visitMethodInsn(INVOKEVIRTUAL, name, Naming.APPLY_METHOD, tupled_apply_sig);
                mv.visitInsn(Opcodes.ARETURN);
                mv.visitMaxs(Naming.ignoredMaxsParameter, Naming.ignoredMaxsParameter);
           
                mv.visitEnd();
            }
            

        }
        
        //RTTI comparison field
       
		final String final_name = name;
		ArrayList<InitializedStaticField> isf_list = new ArrayList<InitializedStaticField>();
		if (!parameters.contains("java/lang/Object")) {
		    
		    isf_list.add(new InitializedStaticField.StaticForUsualRttiField(final_name, this));
        } else {
		    isf_list.add(new InitializedStaticField.StaticForJLOParameterizedRttiField(final_name));
		}
		cw.visitEnd();
//      //RTTI getter
      {
      	MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, Naming.RTTI_GETTER,
                  "()" + Naming.RTTI_CONTAINER_DESC,
                  null, null);
      	mv.visitCode();
      	mv.visitFieldInsn(GETSTATIC, name, Naming.RTTI_FIELD, Naming.RTTI_CONTAINER_DESC);
      	mv.visitInsn(ARETURN);
      	mv.visitMaxs(1, 1);
      	mv.visitEnd();
      }
		
		optionalStaticsAndClassInitForTO(isf_list, cw);
        return cw.toByteArray();
    }
    
    private static byte[] instantiateArrowRTTI(String name) {
        ManglingClassWriter cw = new ManglingClassWriter(ClassWriter.COMPUTE_MAXS|ClassWriter.COMPUTE_FRAMES);
        // Tuple,N$RTTIc
        int dollar_at = name.indexOf('$');
        String stem_name = name.substring(0,dollar_at);
        String nstring = name.substring(Naming.ARROW_RTTI_TAG.length(), dollar_at);
        final int n = Integer.parseInt(nstring);
        String[] superInterfaces = null;
        cw.visit(JVM_BYTECODE_VERSION, ACC_PUBLIC, name, null,
                Naming.ARROW_RTTI_CONTAINER_TYPE, superInterfaces);
        // init
        {
        String init_sig =
            InstantiatingClassloader.jvmSignatureForOnePlusNTypes("java/lang/Class", n, Naming.RTTI_CONTAINER_TYPE, "V");
        MethodVisitor mv = cw.visitNoMangleMethod(ACC_PUBLIC, "<init>", init_sig, null, null);
        mv.visitCode();
        
        mv.visitVarInsn(ALOAD, 0); // this
        mv.visitVarInsn(ALOAD, 1); // class
                                   // allocate and init array for next parameter
        int first_element = 2;
        // new array
        mv.visitLdcInsn(new Integer(n));
        mv.visitTypeInsn(ANEWARRAY, Naming.RTTI_CONTAINER_TYPE);
        for (int i = 0; i < n; i++) {
            mv.visitInsn(DUP);
            mv.visitLdcInsn(new Integer(i));
            mv.visitVarInsn(ALOAD, first_element + i);
            mv.visitInsn(AASTORE);
        }
       
        // invoke super.<init>
        mv.visitMethodInsn(INVOKESPECIAL, Naming.ARROW_RTTI_CONTAINER_TYPE, "<init>", "(Ljava/lang/Class;["+Naming.RTTI_CONTAINER_DESC+")V");
        
        int pno = 2; // skip the java class parameter
        for (int i = Naming.STATIC_PARAMETER_ORIGIN;
                 i < n+Naming.STATIC_PARAMETER_ORIGIN; i++) {
            String spn = "T"+i;
            // not yet this;  sp.getKind();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, pno);
            mv.visitFieldInsn(PUTFIELD, name, spn,
                              Naming.RTTI_CONTAINER_DESC);
            pno++;
        }
        
        voidEpilogue(mv);
        }

        // fields and getters
        for (int i = Naming.STATIC_PARAMETER_ORIGIN;
             i < n+Naming.STATIC_PARAMETER_ORIGIN; i++) {
            fieldAndGetterForStaticParameter(cw, stem_name, "T"+i, i);   
        }
        
        // clinit -- part of the dictionary call
        // dictionary
        // factory
        // ought to create bogus xldata for tuples and arrows, instead we pass null
        emitDictionaryAndFactoryForGenericRTTIclass(cw, name, n, null);
        
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
                if (parameter.startsWith(Naming.TUPLE_OX)) {
                    /* Unwrap tuple, also. */
                    unwrapped_parameters = RTHelpers.extractStringParameters(parameter);
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
        final String any_tuple_n = ANY_TUPLE + Naming.LEFT_OXFORD + n + Naming.RIGHT_OXFORD;        
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
    
    private static byte[] instantiateTupleRTTI(String name) {
        ManglingClassWriter cw = new ManglingClassWriter(ClassWriter.COMPUTE_MAXS|ClassWriter.COMPUTE_FRAMES);
        // Tuple,N$RTTIc
        int dollar_at = name.indexOf('$');
        String stem_name = name.substring(0,dollar_at);
        String nstring = name.substring(Naming.TUPLE_RTTI_TAG.length(), dollar_at);
        final int n = Integer.parseInt(nstring);
        String[] superInterfaces = null;
        cw.visit(JVM_BYTECODE_VERSION, ACC_PUBLIC, name, null,
                Naming.TUPLE_RTTI_CONTAINER_TYPE, superInterfaces);
        // init
        {
        String init_sig =
            InstantiatingClassloader.jvmSignatureForOnePlusNTypes("java/lang/Class", n, Naming.RTTI_CONTAINER_TYPE, "V");
        MethodVisitor mv = cw.visitNoMangleMethod(ACC_PUBLIC, "<init>", init_sig, null, null);
        mv.visitCode();
        
        mv.visitVarInsn(ALOAD, 0); // this
        mv.visitVarInsn(ALOAD, 1); // class
                                   // allocate and init array for next parameter
        int first_element = 2;
        // new array
        mv.visitLdcInsn(new Integer(n));
        mv.visitTypeInsn(ANEWARRAY, Naming.RTTI_CONTAINER_TYPE);
        for (int i = 0; i < n; i++) {
            mv.visitInsn(DUP);
            mv.visitLdcInsn(new Integer(i));
            mv.visitVarInsn(ALOAD, first_element + i);
            mv.visitInsn(AASTORE);
        }
       
        // invoke super.<init>
        mv.visitMethodInsn(INVOKESPECIAL, Naming.TUPLE_RTTI_CONTAINER_TYPE, "<init>", "(Ljava/lang/Class;["+Naming.RTTI_CONTAINER_DESC+")V");
        
        int pno = 2; // skip the java class parameter
        for (int i = Naming.STATIC_PARAMETER_ORIGIN;
                 i < n+Naming.STATIC_PARAMETER_ORIGIN; i++) {
            String spn = "T"+i;
            // not yet this;  sp.getKind();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, pno);
            mv.visitFieldInsn(PUTFIELD, name, spn,
                              Naming.RTTI_CONTAINER_DESC);
            pno++;
        }
        
        voidEpilogue(mv);
        }

        // fields and getters
        for (int i = Naming.STATIC_PARAMETER_ORIGIN;
             i < n+Naming.STATIC_PARAMETER_ORIGIN; i++) {
            fieldAndGetterForStaticParameter(cw, stem_name, "T"+i, i);   
        }
        
        // clinit -- part of the dictionary call
        // dictionary
        // factory
        // ought to create bogus xldata for tuples and arrows, instead we pass null
        emitDictionaryAndFactoryForGenericRTTIclass(cw, name, n, null);
        
        cw.visitEnd();
        return cw.toByteArray();

    }
   
    /**
     * A union type.  Iterate over the members of the union
     * 
     * @param dename
     * @param parameters
     * @return
     */
    private static byte[] instantiateUnion(String dename, List<String> parameters) {
        /*
         */
        ManglingClassWriter cw = new ManglingClassWriter(ClassWriter.COMPUTE_MAXS|ClassWriter.COMPUTE_FRAMES);
 
        final int n = parameters.size();
        String[] superInterfaces = {  };
        
        cw.visit(JVM_BYTECODE_VERSION, ACC_PUBLIC, dename, null,
                 "java/lang/Object", superInterfaces);

        // Intersect all the interfaces to find those common to all members
        // of the union.
        HashSet<Class> intersected_tc_ifs = null;
        for (String member: parameters) {
            String for_loading = Naming.sepToDot(Naming.mangleFortressIdentifier(member));
            Class cl = null;
            try {
                cl = Class.forName(for_loading);
            } catch (ClassNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            HashSet<Class> tc_ifs = new HashSet<Class> ();
            addTransitiveImplements(cl, tc_ifs);
            if (intersected_tc_ifs == null)
                intersected_tc_ifs = tc_ifs;
            else
                intersected_tc_ifs.retainAll(tc_ifs);
        }

        // For each distinct method of the interfaces in the intersection,
        // ignoring self type, emit a static forwarding method, where
        // the first parameter is cast to an interface type (one that
        // has that method) and then it is invoke-interfaced.
        BATree<String, Method> forwarded = new BATree<String, Method>(DefaultComparator.V);
        for (Class an_if : intersected_tc_ifs) {
            // emit a forwarding method for each method in an_if
            if (an_if.isInterface()) {
                Method[] methods = an_if.getDeclaredMethods();
                for (Method m : methods) {
                    String nm = m.getName();
                    Class[] pts = m.getParameterTypes();
                    Class rt = m.getReturnType();
                    StringBuffer key = new StringBuffer();
                    key.append(nm);
                    key.append("(");
                    for (Class pt : pts) {
                        String s = pt.getName();
                        if (pt.isPrimitive()) {
                            key.append(s);
                        } else {
                            key.append("L");
                            key.append(Naming.dotToSep(s));
                            key.append(";");
                        }
                    }
                    key.append(")");
                    String s = Naming.dotToSep(rt.getName());
                    key.append("L" + s + ";");
                    forwarded.put(key.toString(), m);
                }
            }
        }
        
        // For each interface-qualified method defined in the intersection
        // of interfaces, emit a forwarding method.
        
        for (String key : forwarded.keySet()) {
            Method m = forwarded.get(key);
            int ploc = key.indexOf('(');
            String nm = Naming.demangleFortressIdentifier(key.substring(0, ploc));
            String callee_sig = Naming.demangleFortressDescriptor(key.substring(ploc));
            String sig = "(" + Naming.ERASED_UNION_DESC + callee_sig.substring(1);
            // Static, forwarding method
            MethodVisitor mv = cw.visitCGMethod(ACC_PUBLIC+ACC_STATIC, nm, sig, null, null);
            String an_interface = Naming.dotToSep(m.getDeclaringClass().getName());
            String the_interface = Naming.demangleFortressIdentifier(an_interface);
            int stack_index = 0;
            
            mv.visitVarInsn(ALOAD, stack_index++); // 'this'
            mv.visitTypeInsn(CHECKCAST, the_interface);
            
            Class[] pts = m.getParameterTypes();
            
            for (Class pt : pts) {
                String s = pt.getName();
                if (pt.isPrimitive()) {
                    switch (s.charAt(0)) {
                    case 'D':
                        mv.visitVarInsn(DLOAD, stack_index++); // param
                        stack_index++;
                        break;
                    case 'F':
                        mv.visitVarInsn(FLOAD, stack_index++); // param
                        break;
                        
                    case 'I':
                    case 'S':
                    case 'C':
                    case 'B':
                    case 'Z':
                        mv.visitVarInsn(ILOAD, stack_index++); // param
                        break;
                    case 'J':
                        mv.visitVarInsn(LLOAD, stack_index++); // param
                        stack_index++;
                        break;
                    }
                } else {
                    mv.visitVarInsn(ALOAD, stack_index++); // param
                }
            }
            
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE,
                    the_interface, 
                    nm,
                    callee_sig);
            
            char last = sig.charAt(sig.length()-1);
            if (last == 'V')
                voidEpilogue(mv);
            else
                areturnEpilogue(mv);
        }       

        cw.visitEnd();

        return cw.toByteArray();
    }
    
    static void addTransitiveImplements(Class cl, Set<Class> tc_ifs) {
        tc_ifs.add(cl);
        Class[] ifs = cl.getInterfaces();

        for (Class an_if : ifs) 
            if (! tc_ifs.contains(an_if)) 
                addTransitiveImplements(an_if, tc_ifs);
    }

    private static byte[] instantiateTuple(String dename, List<String> parameters) {
        /*
         * interface implements AnyTuple[\ N \]
         * methods e1 ... eN returning typed results.
         */
        ManglingClassWriter cw = new ManglingClassWriter(ClassWriter.COMPUTE_MAXS|ClassWriter.COMPUTE_FRAMES);
 
        final int n = parameters.size();
        final String any_tuple_n = ANY_TUPLE + Naming.LEFT_OXFORD + n + Naming.RIGHT_OXFORD;        
        String[] superInterfaces = { any_tuple_n };
        
        cw.visit(JVM_BYTECODE_VERSION, ACC_PUBLIC + ACC_ABSTRACT + ACC_INTERFACE, dename, null,
                 "java/lang/Object", superInterfaces);


        for (int i = 0; i < n; i++) {
            String m = TUPLE_TYPED_ELT_PFX + (i + Naming.TUPLE_ORIGIN);
            String sig = "()" + Naming.internalToDesc(parameters.get(i));
            interfaceMethod(cw, m, sig);
        }

        cw.visitEnd();

        return cw.toByteArray();
    }

    private byte[] instantiateConcreteTuple(String dename, List<String> parameters) {
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
        final String any_tuple_n = ANY_TUPLE + Naming.LEFT_OXFORD + n + Naming.RIGHT_OXFORD;        
        final String any_concrete_tuple_n = ANY_CONCRETE_TUPLE + Naming.LEFT_OXFORD + n + Naming.RIGHT_OXFORD;        
        final String tuple_params = stringListToTuple(parameters);
        
        String[] superInterfaces = { tuple_params };
        
        cw.visit(JVM_BYTECODE_VERSION, ACC_PUBLIC + ACC_SUPER, dename, null,
                any_concrete_tuple_n, superInterfaces);
        
        
        /* Outline of what must be generated:
        
        // fields
        
        // init method
        
        // factory method
          
 		// getRTTI method
        
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
                String sig = Naming.internalToDesc(parameters.get(i));
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
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, any_concrete_tuple_n, "<init>", Naming.voidToVoid);

            for (int i = 0; i < n; i++) {
                String f = TUPLE_FIELD_PFX + (i + Naming.TUPLE_ORIGIN);
                String sig = Naming.internalToDesc(parameters.get(i));
                
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
        
        // getRTTI method/field and static initialization
        {
        	final String classname = dename;
        	MethodVisitor mv = cw.visitNoMangleMethod(Opcodes.ACC_PUBLIC, // acccess
                     Naming.RTTI_GETTER, // name
                     Naming.STATIC_PARAMETER_GETTER_SIG, // sig
                     null, // generics sig?
                     null); // exceptions
        	mv.visitCode();
        	mv.visitFieldInsn(GETSTATIC, classname, Naming.RTTI_FIELD, Naming.RTTI_CONTAINER_DESC);

        	areturnEpilogue(mv);
        	
            MethodVisitor imv = cw.visitMethod(ACC_STATIC,
                    "<clinit>",
                    Naming.voidToVoid,
                    null,
                    null);
            //taken from codegen.emitRttiField	
            InitializedStaticField isf = new InitializedStaticField.StaticForRttiFieldOfTuple(classname, this);
            isf.forClinit(imv);
            cw.visitField(ACC_PUBLIC + ACC_STATIC + ACC_FINAL,
            		isf.asmName(), isf.asmSignature(),
            		null /* for non-generic */, null /* instance has no value */);

            imv.visitInsn(RETURN);
            imv.visitMaxs(Naming.ignoredMaxsParameter, Naming.ignoredMaxsParameter);
            imv.visitEnd();
        	
        }
        
        // is instance method -- takes an Object
        {
            String sig = "(Ljava/lang/Object;)Z";
        	MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, IS_A, sig, null, null);
            
            Label fail = new Label();
            
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitTypeInsn(Opcodes.INSTANCEOF, any_tuple_n);
            mv.visitJumpInsn(Opcodes.IFEQ, fail);
            
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitTypeInsn(Opcodes.CHECKCAST, any_tuple_n);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, dename,  IS_A, "("+Naming.internalToDesc(any_tuple_n)+")Z");
            mv.visitInsn(Opcodes.IRETURN);
            
            mv.visitLabel(fail);
            mv.visitIntInsn(BIPUSH, 0);
            mv.visitInsn(Opcodes.IRETURN);
            
            mv.visitMaxs(Naming.ignoredMaxsParameter, Naming.ignoredMaxsParameter);
            mv.visitEnd();
        }
        
        // is instance method -- takes an AnyTuple[\N\]
        {
            String sig = "(" + Naming.internalToDesc(any_tuple_n) + ")Z";
            MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, IS_A, sig, null, null);
            
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
            String sig = "(" + Naming.internalToDesc(any_tuple_n) + ")"+Naming.internalToDesc(tuple_params);
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
            String param_desc = Naming.internalToDesc(param_type);
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
    static String stringListToTuple(List<String> parameters) {
        return stringListToGeneric(Naming.TUPLE_TAG, parameters);
    }

    private static String stringListToArrow(List<String> parameters) {
        return stringListToGeneric(Naming.ARROW_TAG, parameters);
    }

    private static String stringListToGeneric(String what, List<String> parameters) {
        return what + Useful.listInDelimiters(Naming.LEFT_OXFORD, parameters, Naming.RIGHT_OXFORD, Naming.GENERIC_SEPARATOR);
    }

    private static String stringListToGenericOfObjects(String what, List<String> parameters) {
        return what + Useful.listInDelimiters(Naming.LEFT_OXFORD, parameters, Naming.RIGHT_OXFORD, Naming.GENERIC_SEPARATOR);
    }


    static Pair<Integer, Integer> make(Integer a, Integer b) {
        return new Pair<Integer, Integer>(a, b);
    }
    
    /**
     * @param mv
     * @param cast_to
     */
    public static void generalizedInstanceOf(MethodVisitor mv, String cast_to) {
        if (cast_to.startsWith(Naming.UNION_OX)) {
            List<String> cast_to_parameters = RTHelpers.extractStringParameters(cast_to);
            Label done = new Label();
            for (int i = 0; i < cast_to_parameters.size(); i++) {
                mv.visitInsn(DUP); // object to test
                generalizedInstanceOf(mv, cast_to_parameters.get(i)); // replaces obj w/ 1/0
                mv.visitInsn(DUP); // copy for branch test. leave one on TOS
                // eepI(mv,"union instanceof subtest " + cast_to_parameters.get(i));
                mv.visitJumpInsn(IFNE, done);
                mv.visitInsn(POP); // discard unnecessary zero.
            }
            mv.visitLdcInsn(0); // failure
            mv.visitLabel(done);
            mv.visitInsn(SWAP); // put tested obj on TOS
            mv.visitInsn(POP); // discard
        } else if (cast_to.startsWith(Naming.TUPLE_OX)) {
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, CONCRETE_+cast_to, IS_A, "(Ljava/lang/Object;)Z");
        } else if (cast_to.startsWith(Naming.ARROW_OX)) {
        	mv.visitMethodInsn(Opcodes.INVOKESTATIC, ABSTRACT_+cast_to, IS_A,"(Ljava/lang/Object;)Z");
        } else {
        	String type = cast_to.equals(Naming.INTERNAL_SNOWMAN) ? Naming.specialFortressTypes.get(Naming.INTERNAL_SNOWMAN) : cast_to;
            mv.visitTypeInsn(Opcodes.INSTANCEOF, type);
        }
    }

    /**
     * @param mv
     * @param cast_to
     */
    public static void generalizedCastTo(MethodVisitor mv, String cast_to) {
        if (cast_to.startsWith(Naming.UNION_OX)) {
            // do nothing, it will be erased!
        } else if (cast_to.startsWith(Naming.TUPLE_OX)) {
            List<String> cast_to_parameters = RTHelpers.extractStringParameters(cast_to);
            String any_tuple_n = ANY_TUPLE + Naming.LEFT_OXFORD + cast_to_parameters.size() + Naming.RIGHT_OXFORD;
            String sig = "(" + Naming.internalToDesc(any_tuple_n) + ")L" + cast_to + ";";
            mv.visitTypeInsn(Opcodes.CHECKCAST, any_tuple_n);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, CONCRETE_+cast_to, CAST_TO, sig);
        } else if (cast_to.startsWith(Naming.ARROW_OX)) {
            List<String> cast_to_parameters = RTHelpers.extractStringParameters(cast_to);
            // mv.visitTypeInsn(Opcodes.CHECKCAST, cast_to);
            
            Triple<List<String>, List<String>, String> stuff =
                normalizeArrowParameters(cast_to_parameters);
            
            List<String> unwrapped_parameters = stuff.getA();
            List<String> tupled_parameters = stuff.getB();
            String tupleType = stuff.getC();
            
            List<String> objectified_parameters = Useful.applyToAll(unwrapped_parameters, toJLO);
            objectified_parameters = normalizeArrowParametersAndReturn(objectified_parameters);
            String obj_sig = stringListToGeneric(Naming.ARROW_TAG, objectified_parameters);

           String sig = "(L" + obj_sig + ";)L" + cast_to + ";";
           mv.visitMethodInsn(Opcodes.INVOKESTATIC, ABSTRACT_+cast_to, CAST_TO, sig);

        } else {
            String type = cast_to.equals(Naming.INTERNAL_SNOWMAN) ? Naming.specialFortressTypes.get(Naming.INTERNAL_SNOWMAN) : cast_to;
        	mv.visitTypeInsn(Opcodes.CHECKCAST, type);
        }
    }

    /**
     * @param params_and_return_list
     * @return
     */
    private static List<String> normalizeArrowParametersAndReturn(
            List<String> params_and_return_list) {
        int l = params_and_return_list.size();
        if (l > 2) {
            String tuple_sig = stringListToGeneric(Naming.TUPLE_TAG, params_and_return_list.subList(0,l-1));
            params_and_return_list = Useful.<String>list(tuple_sig, params_and_return_list.get(l-1));
        }
        return params_and_return_list;
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
    static String toJvmSig(List<String> parameters, String rt) {
        String sig = "(";

        int l = parameters.size();

        StringBuilder buf = new StringBuilder();
        buf.append(sig);
        for (int i = 0; i < l; i++) {
            String s = parameters.get(i);
            if (! s.equals(Naming.INTERNAL_SNOWMAN))
                buf.append(Naming.javaDescForTaggedFortressType(parameters.get(i)));
            else
            	buf.append(Naming.javaDescForTaggedFortressType(Naming.specialFortressTypes.get(Naming.INTERNAL_SNOWMAN)));
        }
        sig = buf.toString();
        sig += ")";
        // nothing special here, yet, but AbstractArrow will be different.
        sig += rt;
        return sig;
    }


    
    static boolean isExpandedOx(String className) {
        return isExpanded(className, Naming.LEFT_OXFORD_CHAR, Naming.RIGHT_OXFORD_CHAR) ;
    }
    static boolean isExpandedAngle(String className) {
        return 
        isExpanded(className, Naming.LEFT_HEAVY_ANGLE_CHAR, Naming.RIGHT_HEAVY_ANGLE_CHAR);
    }

    /**
     * @param className
     * @param left_ch
     * @param right_ch
     * @return
     */
    private static boolean isExpanded(String className, char left_ch,
            char right_ch) {
        int left = className.indexOf(left_ch);
        int right = className.indexOf(right_ch);
        return (left != -1 && right != -1 && left < right);
    }
    
    Naming.XlationData xlationForGeneric(String t) {
        String template_name = genericTemplateName(t, null);
        return xlationForFunctionOrGeneric(template_name);

    }
    Naming.XlationData xlationForGeneric(String t, char left_char, char right_char) {
        String template_name = genericTemplateName(t, left_char, right_char, null);
        return xlationForFunctionOrGeneric(template_name);

    }

    Naming.XlationData xlationForFunction(String t) {
        String template_name = functionTemplateName(t, null);
        return xlationForFunctionOrGeneric(template_name);

    }

    /**
     * @param template_name
     * @return
     * @throws Error
     */
    private Naming.XlationData xlationForFunctionOrGeneric(String template_name)
            throws Error {
        Naming.XlationData xldata = stemToXlation.get(template_name);
        
        if (xldata != null) return xldata;
        
        try {
            xldata =
                Naming.XlationData.fromBytes(readResource(template_name, "xlation"));
        } catch (VersionMismatch e) {
            throw new Error("Read stale serialized data for " + template_name + ", recommend you delete the Fortress bytecode cache and relink", e);
        } catch (IOException e) {
            throw new Error("Unable to read serialized data for " + template_name + ", recommend you delete the Fortress bytecode cache and relink", e);
        }
        
        synchronized(stemToXlation) {
            if (stemToXlation.get(template_name) == null) {
                stemToXlation.put(template_name, xldata);
            }
        }
        
        return xldata;
    }
    public static void optionalStaticsAndClassInitForTO(
               List<InitializedStaticField> isf_list,
               ManglingClassWriter cw) {
           if (isf_list.size() ==  0)
               return;
    
           MethodVisitor imv = cw.visitMethod(ACC_STATIC,
                                              "<clinit>",
                                              Naming.voidToVoid,
                                              null,
                                              null);
    
                  
           for (InitializedStaticField isf : isf_list) {
               isf.forClinit(imv);
               cw.visitField(
                       ACC_PUBLIC + ACC_STATIC + ACC_FINAL,
                       isf.asmName(), isf.asmSignature(),
                       null /* for non-generic */, null /* instance has no value */);
               // DRC-WIP
           }
           
           imv.visitInsn(RETURN);
           imv.visitMaxs(Naming.ignoredMaxsParameter, Naming.ignoredMaxsParameter);
           imv.visitEnd();
       }

    public static String jvmSignatureForNTypes(int n, String type,
            String rangeDesc) {
        // This special case handles single void argument type properly.
        String args = "";
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < n; i++) {
            buf.append("L");
            buf.append(type);
            buf.append(";");
        }
        args = buf.toString();
        return Naming.makeMethodDesc(args, rangeDesc);
    }

    public static String jvmSignatureForOnePlusNTypes(String one, int n, String type,
            String rangeDesc) {
        // This special case handles single void argument type properly.
        String args = "";
        StringBuilder buf = new StringBuilder();
        buf.append("L");
        buf.append(one);
        buf.append(";");
        for (int i = 0; i < n; i++) {
            buf.append("L");
            buf.append(type);
            buf.append(";");
        }
        args = buf.toString();
        return Naming.makeMethodDesc(args, rangeDesc);
    }

    /**
     * @param stem_name
     * @param static_parameter_name
     * @param i
     */
    static public void fieldAndGetterForStaticParameter(ManglingClassWriter cw, String stem_name,
            String static_parameter_name, int i) {
        String method_name =
            Naming.staticParameterGetterName(stem_name, i);
        
        cw.visitField(ACC_PRIVATE + ACC_FINAL,
                static_parameter_name, Naming.RTTI_CONTAINER_DESC, null, null);
        
        MethodVisitor mv = cw.visitCGMethod(
                ACC_PUBLIC, method_name,
                Naming.STATIC_PARAMETER_GETTER_SIG, null, null);
        
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, Naming.stemClassToRTTIclass(stem_name), static_parameter_name, Naming.RTTI_CONTAINER_DESC);
    
        
        areturnEpilogue(mv);
    }

    /**
     * 
     */
    static public void voidEpilogue(MethodVisitor mv) {
        mv.visitInsn(RETURN);
        mv.visitMaxs(Naming.ignoredMaxsParameter, Naming.ignoredMaxsParameter);
        mv.visitEnd();
    }

    /**
     * 
     */
    static public void areturnEpilogue(MethodVisitor mv) {
        mv.visitInsn(ARETURN);
    
        mv.visitMaxs(Naming.ignoredMaxsParameter, Naming.ignoredMaxsParameter);
        mv.visitEnd();
    }

    /**
     * @param first_arg
     * @param n_args
     */
    public static void pushArgs(MethodVisitor mv, int first_arg, int n_args) {
        for (int arg = 0; arg < n_args; arg++) {
            mv.visitVarInsn(ALOAD, arg+first_arg);
        }
    }
    
    public static void pushArgs(MethodVisitor mv, int first_arg, int n_args, List<Boolean> nulls) {
        int nulls_pushed = 0;
        for (int arg = 0; arg < n_args; arg++) {
            if (nulls.get(arg)) {
                mv.visitInsn(Opcodes.ACONST_NULL);
                nulls_pushed++;
            } else {
                mv.visitVarInsn(Opcodes.ALOAD, arg+first_arg-nulls_pushed);
            }
        }
    }
    
    public static void pushArgsIntoArray(MethodVisitor mv, int first_arg, int n_args, int array_offset) {
        for (int arg = 0; arg < n_args; arg++) {
            mv.visitVarInsn(Opcodes.ALOAD, array_offset);
            mv.visitLdcInsn(arg); //index is the static param number
            mv.visitVarInsn(Opcodes.ALOAD, arg+first_arg);
            mv.visitInsn(Opcodes.AASTORE);
        }
        mv.visitVarInsn(ALOAD, array_offset);
    }

    public static void pushArgsIntoArray(MethodVisitor mv, int first_arg,
            int n_args, int array_offset, List<Boolean> nulls) {
        int nulls_pushed = 0;
        for (int arg = 0; arg < n_args; arg++) {
            mv.visitVarInsn(Opcodes.ALOAD, array_offset);
            mv.visitLdcInsn(arg); //index is the static param number
            if (nulls.get(arg)) {
                mv.visitInsn(Opcodes.ACONST_NULL);
                nulls_pushed++;
            } else {
                mv.visitVarInsn(Opcodes.ALOAD, arg+first_arg-nulls_pushed);
            }
            mv.visitInsn(Opcodes.AASTORE);
        }
        mv.visitVarInsn(ALOAD, array_offset);
    }

    /**
     * @param rttiClassName
     * @param sparams_size
     */
    static public void emitDictionaryAndFactoryForGenericRTTIclass(
            ManglingClassWriter cw,
            String rttiClassName,
            int sparams_size,
            final Naming.XlationData xldata) {
        
        // Push nulls for opr parameters in the factory call.
        List<Boolean> spks;
        int type_sparams_size = sparams_size;
        if (xldata != null) {
             spks = xldata.isOprKind();
             sparams_size = spks.size();
        } else {
            spks = new InfiniteList<Boolean>(false);
        }
        
        // FIELD
        // static, initialized to Map-like thing
        cw.visitField(ACC_PRIVATE + ACC_STATIC + ACC_FINAL,
                "DICTIONARY", Naming.RTTI_MAP_DESC, null, null);

        // CLINIT
        // factory, consulting map, optionally invoking constructor.
        MethodVisitor mv = cw.visitNoMangleMethod(ACC_STATIC, "<clinit>", "()V", null, null);
        mv.visitCode();
        // new
        mv.visitTypeInsn(NEW, Naming.RTTI_MAP_TYPE);
        // init
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, Naming.RTTI_MAP_TYPE, "<init>", "()V");
        // store
        mv.visitFieldInsn(PUTSTATIC, rttiClassName,
                "DICTIONARY", Naming.RTTI_MAP_DESC);                

        mv.visitInsn(RETURN);
        mv.visitMaxs(Naming.ignoredMaxsParameter, Naming.ignoredMaxsParameter);
        mv.visitEnd();

        // FACTORY

        boolean useSparamsArray = sparams_size > 6;
        int sparamsArrayIndex = sparams_size;
        
        String fact_sig = Naming.rttiFactorySig(type_sparams_size);
        String init_sig = InstantiatingClassloader.jvmSignatureForOnePlusNTypes("java/lang/Class",
                type_sparams_size, Naming.RTTI_CONTAINER_TYPE, "V");
        String get_sig;
        String put_sig;
        String getClass_sig;
        if (useSparamsArray) {
            get_sig = Naming.makeMethodDesc(Naming.RTTI_CONTAINER_ARRAY_DESC, 
                                            Naming.RTTI_CONTAINER_DESC);
            put_sig = Naming.makeMethodDesc(Naming.RTTI_CONTAINER_ARRAY_DESC + Naming.RTTI_CONTAINER_DESC, 
                                            Naming.RTTI_CONTAINER_DESC);
            getClass_sig = Naming.makeMethodDesc(NamingCzar.descString + Naming.RTTI_CONTAINER_ARRAY_DESC,
                                                 NamingCzar.descClass);
        } else {
            get_sig = InstantiatingClassloader.jvmSignatureForNTypes(
                sparams_size, Naming.RTTI_CONTAINER_TYPE, Naming.RTTI_CONTAINER_DESC);
            put_sig = InstantiatingClassloader.jvmSignatureForNTypes(
                    sparams_size+1, Naming.RTTI_CONTAINER_TYPE, Naming.RTTI_CONTAINER_DESC);
            getClass_sig = InstantiatingClassloader.jvmSignatureForOnePlusNTypes(NamingCzar.internalString,
                    sparams_size, Naming.RTTI_CONTAINER_TYPE, NamingCzar.descClass);
        }

        mv = cw.visitNoMangleMethod(ACC_PUBLIC + ACC_STATIC, Naming.RTTI_FACTORY, fact_sig, null, null);
        mv.visitCode();
        /* 
         * First arg is java class, necessary for creation of type.
         * 
         * rCN x = DICTIONARY.get(args)
         * if  x == null then
         *   x = new rCN(args)
         *   x = DICTIONARY.put(args, x)
         * end
         * return x
         */

        // object
        mv.visitFieldInsn(GETSTATIC, rttiClassName,
                "DICTIONARY", Naming.RTTI_MAP_DESC);                
        // push args
        int l = sparams_size;
        if (useSparamsArray) {
            mv.visitLdcInsn(sparams_size);
            mv.visitTypeInsn(Opcodes.ANEWARRAY, Naming.RTTI_CONTAINER_TYPE);
            mv.visitVarInsn(Opcodes.ASTORE, sparamsArrayIndex);
            InstantiatingClassloader.pushArgsIntoArray(mv, 0, l, sparamsArrayIndex, spks);
        } else {
            InstantiatingClassloader.pushArgs(mv, 0, l, spks);
        }
        // invoke Dictionary.get
        mv.visitMethodInsn(INVOKEVIRTUAL, Naming.RTTI_MAP_TYPE, "get", get_sig);
        Label not_null = new Label();
        mv.visitInsn(DUP);
        mv.visitJumpInsn(IFNONNULL, not_null);
        mv.visitInsn(POP); // discard dup'd null
        // doing it all on the stack -- (unless too many static params, then use an array for human coded stuff)
        // 1) first push the dictionary and args (array if used) 
        // 2) create new RTTI object
        // 3) push args again (array if used) and create the class for this object
        // 4) push the args again (never array) to init RTTI object
        // 5) add to dictionary
        
        //1)
        mv.visitFieldInsn(GETSTATIC, rttiClassName,
                "DICTIONARY", Naming.RTTI_MAP_DESC);                
        
        if (useSparamsArray) {
            mv.visitVarInsn(ALOAD, sparamsArrayIndex);
        } else {
            InstantiatingClassloader.pushArgs(mv, 0, l, spks);
        }

        // 2) invoke constructor
        mv.visitTypeInsn(NEW, rttiClassName);
        mv.visitInsn(DUP);
        
        // 3) create class for this object
        String stem = Naming.rttiClassToBaseClass(rttiClassName);
        if (xldata == null) {
            // NOT symbolic (and a problem if we pretend that it is)
            mv.visitLdcInsn(stem);
        } else {
            RTHelpers.symbolicLdc(mv, stem);
        }
        if (useSparamsArray) {
            mv.visitVarInsn(ALOAD, sparamsArrayIndex);
        } else {
            InstantiatingClassloader.pushArgs(mv, 0, l, spks);
        }
        
        //(mv, "before getRTTIclass");
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, Naming.RT_HELPERS, "getRTTIclass", getClass_sig);
        //eep(mv, "after getRTTIclass");

        // 4) init RTTI object (do not use array)
        // NOTE only pushing type_sparams here.
        InstantiatingClassloader.pushArgs(mv, 0, type_sparams_size);
        mv.visitMethodInsn(INVOKESPECIAL, rttiClassName,
                "<init>", init_sig);
        // 5) add to dictionary
        mv.visitMethodInsn(INVOKEVIRTUAL, Naming.RTTI_MAP_TYPE,
                "putIfNew", put_sig);

        mv.visitLabel(not_null);
        mv.visitInsn(ARETURN);

        mv.visitMaxs(Naming.ignoredMaxsParameter, Naming.ignoredMaxsParameter);
        mv.visitEnd();
    }
    
//    static public Class getRTTIclass(String stem, RTTI[] params) {
//  
//        StringBuilder classNameBuf = new StringBuilder(stem + Naming.LEFT_OXFORD);
//        for (int i = 0; i < params.length - 1; i++) {
//            classNameBuf.append(params[i].className() + ";");
//        }
//        classNameBuf.append(params[params.length-1].className() + Naming.RIGHT_OXFORD);
//        
//        String mangledClassName = Naming.mangleFortressIdentifier(classNameBuf.toString());
//        String mangledDots = Naming.sepToDot(mangledClassName);
//        
//        try {
//            return Class.forName(mangledDots); //ONLY.loadClass(Naming.sepToDot(mangledClassName), false);
//        } catch (ClassNotFoundException e) {
//            e.printStackTrace();
//            throw new RuntimeException("class " + mangledClassName.toString() + " failed to load");
//        }
//    }
//    
//    static public Class getRTTIclass(String stem, RTTI param1) {
//        RTTI[] params = { param1 };
//        return getRTTIclass(stem, params);
//    }
//    
//    static public Class getRTTIclass(String stem, RTTI param1, RTTI param2) {
//        RTTI[] params = { param1, param2 };
//        return getRTTIclass(stem, params);
//    }
//    
//    static public Class getRTTIclass(String stem, RTTI param1, RTTI param2, RTTI param3) {
//        RTTI[] params = { param1, param2, param3 };
//        return getRTTIclass(stem, params);
//    }
//    
//    static public Class getRTTIclass(String stem, RTTI param1, RTTI param2, RTTI param3, RTTI param4) {
//        RTTI[] params = { param1, param2, param3, param4 };
//        return getRTTIclass(stem, params);
//    }
//    
//    static public Class getRTTIclass(String stem, RTTI param1, RTTI param2, RTTI param3, RTTI param4, RTTI param5) {
//        RTTI[] params = { param1, param2, param3, param4, param5 };
//        return getRTTIclass(stem, params);
//    }
//
//    static public Class getRTTIclass(String stem, RTTI param1, RTTI param2, RTTI param3, RTTI param4, RTTI param5, RTTI param6) {
//        RTTI[] params = { param1, param2, param3, param4, param5, param6 };
//        return getRTTIclass(stem, params);
//    }
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
        if (name.startsWith("java.")
                || name.startsWith("javax.")
            || name.startsWith("jsr166y.")
            || name.startsWith("sun.")
            || name.startsWith("com.sun.fortress.runtimeSystem.InitializedStaticField")
            || name.startsWith("com.sun.fortress.runtimeSystem.BAlongTree")
            || name.startsWith("com.sun.fortress.compiler.codegen.ManglingClassWriter")
            || name.startsWith("com.sun.fortress.repository.ProjectProperties")
            || name.startsWith("com.sun.fortress.useful.")
            || name.startsWith("org.objectweb.asm.")
            || name.startsWith("com.sun.fortress.compiler.runtimeValues.RTTI") 
            || name.startsWith("com.sun.fortress.compiler.runtimeValues.ArrowRTTI") 
            || name.startsWith("com.sun.fortress.compiler.runtimeValues.JavaRTTI") 
            || name.startsWith("com.sun.fortress.runtimeSystem.RttiTupleMap") 
            || name.startsWith("com.sun.fortress.runtimeSystem.Naming") 
            || (name.startsWith("com.sun.") && ! name.startsWith("com.sun.fortress."))) {
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
