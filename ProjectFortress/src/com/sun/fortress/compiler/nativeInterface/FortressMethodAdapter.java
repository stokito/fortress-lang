 /*******************************************************************************
    Copyright 2009,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.nativeInterface;

import java.util.*;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.EmptyVisitor;

import com.sun.fortress.compiler.codegen.CodeGen;
import com.sun.fortress.compiler.codegen.CodeGenClassWriter;
import com.sun.fortress.compiler.index.Function;
import com.sun.fortress.compiler.index.Functional;
import com.sun.fortress.compiler.NamingCzar;
import com.sun.fortress.compiler.OverloadSet;
import com.sun.fortress.scala_src.types.TypeAnalyzer;
import com.sun.fortress.nativeHelpers.*;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.useful.Debug;
import com.sun.fortress.useful.MultiMap;
import com.sun.fortress.useful.Useful;

class fortressConverter {
    final String shortName;
    final String fortressRuntimeType;
    final String toJavaTypeMethod;
    final String toJavaTypeMethodDesc;
    final String constructor;
    final String constructorType;

    private final static String prefix = "com/sun/fortress/compiler/runtimeValues/";

    fortressConverter(String _fortressRuntimeType,
                      String _toJavaTypeMethod,
                      String _toJavaTypeMethodDesc,
                      String _constructor,
                      String _constructorType) {

        shortName = _fortressRuntimeType;
        fortressRuntimeType = "L" + prefix + _fortressRuntimeType + ";";
        toJavaTypeMethod = _toJavaTypeMethod;
        toJavaTypeMethodDesc = _toJavaTypeMethodDesc;
        constructor = _constructor;
        constructorType = _constructorType;
    }
    
    int opcode() {
        return Opcodes.INVOKEVIRTUAL;
    }
    
    String wrapStrippedClass(String strippedClass) {
        return prefix + strippedClass;
    }
    
    void convertArg(MethodVisitor mv, String classDesc) {
        mv.visitMethodInsn(opcode(),FortressMethodAdapter.descToType(classDesc),
                toJavaTypeMethod, toJavaTypeMethodDesc);
    }
    
    void convertResult(MethodVisitor mv, String classDesc) {
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, FortressMethodAdapter.descToType(classDesc),
                constructor, constructorType);
       
    }

}

class emptyConverter extends fortressConverter {
    static final emptyConverter ONLY = new emptyConverter();
    private emptyConverter() {
        super(null, null, null, null, null);
    }
    void convertArg(MethodVisitor mv, String classDesc) {
        // do nothing
    }
    
    void convertResult(MethodVisitor mv, String classDesc) {
        // do nothing
    }
}

public class FortressMethodAdapter extends ClassAdapter {

    String inputClassName;
    String outputClassName;
    private final static String prefix = "com/sun/fortress/compiler/runtimeValues/";
    private final static String prefixDotted = "com.sun.fortress.compiler.runtimeValues";
    private HashMap conversionTable;

    private APIName apiName;
    private Map<IdOrOpOrAnonymousName,MultiMap<Integer, Functional>> sizePartitionedOverloads;
    private TypeAnalyzer ta;
    private CodeGenClassWriter cw;
    private Set<String> overloadedNamesAndSigs;

    private void initializeEntry(String fortressRuntimeType,
                                 String toJavaTypeMethod,
                                 String toJavaTypeMethodDesc,
                                 String constructor,
                                 String constructorType) {
        conversionTable.put(fortressRuntimeType,
                            new fortressConverter(fortressRuntimeType,
                                                  toJavaTypeMethod,
                                                  toJavaTypeMethodDesc,
                                                  constructor,
                                                  constructorType));
    }

    private void initializeTables() {
        conversionTable = new HashMap();
	// If you change these, you may also need to change items in file NamingCzar.java
        initializeEntry("FZZ32",    "getValue", "()I", "make", "(I)L" + prefix + "FZZ32;");
        initializeEntry("FZZ64",    "getValue", "()J", "make", "(J)L" + prefix + "FZZ64;");
        initializeEntry("FNN32",    "getValue", "()I", "make", "(I)L" + prefix + "FNN32;");
        initializeEntry("FNN64",    "getValue", "()J", "make", "(J)L" + prefix + "FNN64;");
        initializeEntry("FRR32",    "getValue", "()F", "make", "(F)L" + prefix + "FRR32;");
        initializeEntry("FRR64",    "getValue", "()D", "make", "(D)L" + prefix + "FRR64;");
        initializeEntry("FBoolean", "getValue", "()Z", "make", "(Z)L" + prefix + "FBoolean;");
        initializeEntry("FVoid",    "getValue", "()",  "make", "()L" + prefix + "FVoid;");
        initializeEntry("FJavaString",  "getValue", "()Ljava/lang/String;",
                                    "make", "(Ljava/lang/String;)L" + prefix + "FJavaString;");
        initializeEntry("FCharacter",    "getValue",	"()I", "make", "(I)L" + prefix + "FCharacter;");
        initializeEntry("FJavaBufferedReader", "getValue", "()Lcom/sun/fortress/compiler/runtimeValues/FortressBufferedReader;",
                                    "make", "(Lcom/sun/fortress/compiler/runtimeValues/FortressBufferedReader;)L" + prefix + "FJavaBufferedReader;");
        initializeEntry("FJavaBufferedWriter", "getValue", "()Lcom/sun/fortress/compiler/runtimeValues/FortressBufferedWriter;",
                                    "make", "(Lcom/sun/fortress/compiler/runtimeValues/FortressBufferedWriter;)L" + prefix + "FJavaBufferedWriter;");
        initializeEntry("FZZ", "getValue", "()Ljava/math/BigInteger;",
                "make", "(Ljava/math/BigInteger;)L" + prefix + "FZZ;");
        initializeEntry("FVector", "getValue", "()[I" , "make","([I)L"+ prefix + "FVector;");
    }

    // Strip off the leading L + prefix, and trailing ;"

    static String strip(String s) {
        // String header = "L" + prefix;
        String trailer = ";";
        int end = s.lastIndexOf(";");
        int start = s.lastIndexOf("/") + 1; // header.length();
        return s.substring(start,end);
    }

    static String descToType(String s) {
        if (! (s.startsWith("L") && s.endsWith(";")))
            throw new IllegalArgumentException("Input string " + s + " must begin with 'L' and end with ';'");
        
        return Useful.substring(s,1,-1);
    }

    public FortressMethodAdapter(CodeGenClassWriter cv,
            String inputClassName,
            String outputClassName,
            APIName api_name,
            Map<IdOrOpOrAnonymousName,MultiMap<Integer, Functional>> size_partitioned_overloads,
            TypeAnalyzer ta) {
        super(cv);
        this.cw = cv;
        this.inputClassName = inputClassName.replace('.','/');
        this.outputClassName = outputClassName.replace('.','/');
        this.apiName = api_name;
        this.sizePartitionedOverloads = size_partitioned_overloads;
        this.ta = ta;
        initializeTables();
    }

    public void visit(int version, int access, String name, String signature,
            String superName, String[] interfaces) {
        Debug.debug(Debug.Type.COMPILER, 1, "visit:" + name + " generate "
                + inputClassName);
        cv.visit(version, access, outputClassName, signature, superName,
                interfaces);

        /*
         * TODO there may be more work be done in the method called below.
         * cg=null allows a bunch of code reuse, but does not deal with the
         * possibility of unambiguous reference to one of these
         * adapter-generated overloaded (?) native methods.
         */

        overloadedNamesAndSigs = CodeGen.generateTopLevelOverloads(apiName,
                sizePartitionedOverloads, ta, cw, /* cg= */null,
                OverloadSet.localFactory);
    }

    private Set<String> getOverloadedNamesAndSigs() {
        return overloadedNamesAndSigs;
    }

    public MethodVisitor visitMethod(int access, String name, String desc,
            String signature, String[] exceptions) {
        // Don't know how to do these, or if we need them...
        if (name.equals("<init>") || name.equals("<clinit>"))
            Debug.debug(Debug.Type.COMPILER, 1, "Don't visit Method ", name);
        else if (SignatureParser.unsayable(desc))
            Debug.debug(Debug.Type.COMPILER, 1,
                    "Don't visit Method with unsayable desc", name);
        else {

            generateNewBody(access, desc, signature, exceptions, name, name);

        }

        return new EmptyVisitor();// super.visitMethod(access, name, desc, signature, exceptions);
    }

    static class SignatureAndConverter {
        final fortressConverter converter;
        final String signature;
        SignatureAndConverter(String signature, fortressConverter converter) {
            this.converter = converter;
            this.signature = signature;
        }
    }
    
    /**
     * converts an input, foreign, Java type descriptor into the Java type
     * descriptor for the implementation of the corresponding Fortress type.
     * 
     * The name is an abbreviation for toImplForFortressForForeign.
     * 
     * @param arg_desc
     * @return
     */
    private SignatureAndConverter toImplFFFF(String jvmArgType, String method_name, boolean isResultType) {
        com.sun.fortress.nodes.Type ftype = NamingCzar.fortressTypeForForeignJavaType(jvmArgType, method_name, isResultType);
        if (ftype == null) {
            // Perhaps this type is in the Fortress implementation hierarchy.
            if (NamingCzar.jvmTypeExtendsAny(jvmArgType))
                return new SignatureAndConverter(jvmArgType, emptyConverter.ONLY);;
            
            throw new Error("No Fortress type (yet) for foreign Java type descriptor '" + jvmArgType + "'");
        }
        String fortressArgType = NamingCzar.jvmTypeDesc(ftype, null);
        if (fortressArgType == null)
            throw new Error("No Java impl type (yet) for Fortress type " + ftype + " for foreign descriptor '" + jvmArgType + "'");
        String stripped = strip(fortressArgType);
        fortressConverter converter = (fortressConverter) conversionTable
                .get(stripped);
        
        if (converter == null)
            throw new RuntimeException("Can't generate header for method "
                    + method_name + " with jvm desc =" + jvmArgType + " stripped = " + stripped);
        
        return new SignatureAndConverter(fortressArgType, converter);
    }
    
    private MethodVisitor generateNewBody(int access, String desc, String signature,
            String[] exceptions, String name, String newName) {

        Debug.debug(Debug.Type.COMPILER, 1, "generateNewBody: ", name,
                    " with desc ", desc);

        SignatureParser sp = new SignatureParser(desc);
        
        List<String> desc_args = sp.getJVMArguments();
        String desc_result = sp.getJVMResult();
        List<String> fortress_args = new ArrayList<String>();
        List<fortressConverter> convert_args = new ArrayList<fortressConverter>();

        String fsig = "(";
        StringBuilder buf = new StringBuilder();
        buf.append(fsig);
        for (String s : desc_args) {
             
            SignatureAndConverter s_a_c = toImplFFFF(s, name, false);
            buf.append(s_a_c.signature);
            fortress_args.add(s_a_c.signature);
            
            convert_args.add(s_a_c.converter);
        }
        fsig = buf.toString();
        SignatureAndConverter s_a_c = toImplFFFF(desc_result, name, true);
        fsig = fsig + ")" + s_a_c.signature;
        
        fortressConverter convert_result = s_a_c.converter;
    
        // FORWARDING METHOD, only with type conversions on the way in/out!
        MethodVisitor mv = cv.visitMethod(access, name, fsig, signature,
                exceptions);
        mv.visitCode();
        Label l0 = new Label();
        mv.visitLabel(l0);
        int count = 0;
        for (String s : fortress_args) {
            fortressConverter converter = convert_args.get(count);
            mv.visitVarInsn(Opcodes.ALOAD, count++);
            converter.convertArg(mv, s);
        }

        Debug.debug(Debug.Type.COMPILER, 1, "className = ", inputClassName,
                    " name = ", name, " access = ", access);

        mv.visitMethodInsn(Opcodes.INVOKESTATIC, inputClassName, name, sp
                .getSignature());
        
        convert_result.convertResult(mv, s_a_c.signature);

        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(2, 1);
        mv.visitEnd();
        return mv;
    }

    public void visitEnd() {

        super.visitEnd();
    }


}
