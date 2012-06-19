/*******************************************************************************
    Copyright 2009,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/

package com.sun.fortress.compiler;

import static com.sun.fortress.exceptions.ProgramError.errorMsg;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Type;

import com.sun.fortress.compiler.codegen.FnNameInfo;
import com.sun.fortress.compiler.environments.TopLevelEnvGen;
import com.sun.fortress.compiler.index.Functional;
import com.sun.fortress.compiler.runtimeValues.FortressBufferedReader;
import com.sun.fortress.compiler.runtimeValues.FortressBufferedWriter;
import com.sun.fortress.compiler.runtimeValues.FZZ;
import com.sun.fortress.compiler.runtimeValues.FZZ32Vector;
import com.sun.fortress.compiler.runtimeValues.FStringVector;
import com.sun.fortress.exceptions.CompilerError;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.AnyType;
import com.sun.fortress.nodes.ArrowType;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.BoolArg;
import com.sun.fortress.nodes.BoolBase;
import com.sun.fortress.nodes.BoolBinaryOp;
import com.sun.fortress.nodes.BoolExpr;
import com.sun.fortress.nodes.BoolRef;
import com.sun.fortress.nodes.BoolUnaryOp;
import com.sun.fortress.nodes.BottomType;
import com.sun.fortress.nodes.DimArg;
import com.sun.fortress.nodes.Fixity;
import com.sun.fortress.nodes.FnDecl;
import com.sun.fortress.nodes.FnHeader;
import com.sun.fortress.nodes.FunctionalRef;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdOrOp;
import com.sun.fortress.nodes.IntArg;
import com.sun.fortress.nodes.IntBase;
import com.sun.fortress.nodes.IntBinaryOp;
import com.sun.fortress.nodes.IntExpr;
import com.sun.fortress.nodes.IntRef;
import com.sun.fortress.nodes.IntersectionType;
import com.sun.fortress.nodes.KindBool;
import com.sun.fortress.nodes.KindDim;
import com.sun.fortress.nodes.KindInt;
import com.sun.fortress.nodes.KindNat;
import com.sun.fortress.nodes.KindOp;
import com.sun.fortress.nodes.KindType;
import com.sun.fortress.nodes.KindUnit;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeAbstractVisitor;
import com.sun.fortress.nodes.Op;
import com.sun.fortress.nodes.OpArg;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.PostFixity;
import com.sun.fortress.nodes.PreFixity;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.StaticParamKind;
import com.sun.fortress.nodes.TraitSelfType;
import com.sun.fortress.nodes.TraitType;
import com.sun.fortress.nodes.TraitTypeWhere;
import com.sun.fortress.nodes.TupleType;
import com.sun.fortress.nodes.TypeArg;
import com.sun.fortress.nodes.UnionType;
import com.sun.fortress.nodes.UnitArg;
import com.sun.fortress.nodes.VarType;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.OprUtil;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.repository.ForeignJava;
import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.runtimeSystem.Naming;
import com.sun.fortress.useful.BATree;
import com.sun.fortress.useful.Debug;
import com.sun.fortress.useful.Pair;
import com.sun.fortress.useful.Triple;
import com.sun.fortress.useful.Useful;

import edu.rice.cs.plt.tuple.Option;

public class NamingCzar {
    private NamingCzar() { super(); }

    private final static boolean logLoads = ProjectProperties.getBoolean("fortress.log.classloads", false);

    public static final String COERCION_NAME = "coerce";
    public static final String LIFTED_COERCION_PREFIX = "coerce_";

    /**
     * For use in equality testing only; do not incorporate this node
     * into any generated tress.  For that purpose, use selfName(Span).
     */
    public static final Id SELF_NAME = NodeFactory.makeId(NodeFactory.internalSpan, "self");
    /**
     * Generates a "self" at a particular location.
     * 
     * @param sp
     * @return
     */
    public static final Id selfName(Span sp) {
        return NodeFactory.makeId(sp, "self");
    }

    /** Name for sole field of top-level singleton class representing a
     *  top-level binding.  Should not need to be particularly
     *  unambiguous, as other fields can't occur in the class, right?
     */
    public static final String SINGLETON_FIELD_NAME = "ONLY";

    public static final String springBoard = "$DefaultTraitMethods";
    public static final String make = "make";

    public static final String cache = ProjectProperties.BYTECODE_CACHE_DIR + "/";
    public static final String optimizedcache = ProjectProperties.OPTIMIZED_BYTECODE_CACHE_DIR + "/";
    public static final String nativecache = ProjectProperties.NATIVE_WRAPPER_CACHE_DIR + "/";

    // fortress types
    public static final String fortressPackage = "fortress";
    public static final String fortressAny = fortressPackage + "/" +
                                              WellKnownNames.anyTypeLibrary() +
                                             "$" + WellKnownNames.anyTypeName;

    // Base class for all executable Fortress Components
    public static final String fortressExecutable = "com/sun/fortress/runtimeSystem/FortressExecutable";
    public static final String fortressExecutableRun = "runExecutable";
    public static final String fortressExecutableRunType = "([Ljava/lang/String;)V";

    // Base class for tasks
    public static final String fortressBaseTask = "com/sun/fortress/runtimeSystem/BaseTask";

    // Base class for misc RTS routines
    public static final String miscCodegen = "com/sun/fortress/runtimeSystem/MiscCodegenFunctions";
    public static final String matchFailure = "overloadMatchFailure";
    public static final String errorReturn = "()Ljava/lang/Error;";

    // Base class for non-executable Fortress Components
    public static final String fortressComponent = Naming.javaObject;

    public static final String primordialTask    = "com/sun/fortress/runtimeSystem/PrimordialTask";

    // Classes: internal names
    // (Section 2.1.2 in ASM 3.0: A Java bytecode engineering library)
    public static final String internalFloat      = org.objectweb.asm.Type.getInternalName(float.class);
    public static final String internalInt        = org.objectweb.asm.Type.getInternalName(int.class);
    public static final String internalDouble     = org.objectweb.asm.Type.getInternalName(double.class);
    public static final String internalLong       = org.objectweb.asm.Type.getInternalName(long.class);
    public static final String internalBoolean    = org.objectweb.asm.Type.getInternalName(boolean.class);
    public static final String internalCharacter  = org.objectweb.asm.Type.getInternalName(int.class);
    public static final String internalJavaBufferedReader = org.objectweb.asm.Type.getInternalName(FortressBufferedReader.class);
    public static final String internalJavaBufferedWriter = org.objectweb.asm.Type.getInternalName(FortressBufferedWriter.class);
    public static final String internalObject     = org.objectweb.asm.Type.getInternalName(Object.class);
    public static final String internalString     = org.objectweb.asm.Type.getInternalName(String.class);
    public static final String internalSingleton  = internalObject;
    public static final String internalClass      = org.objectweb.asm.Type.getInternalName(Class.class);

    // If you change these, you may also need to change items in file nativeInterface/FortressMethodAdapter.java
    public static final String descFloat         = org.objectweb.asm.Type.getDescriptor(float.class);
    public static final String descInt           = org.objectweb.asm.Type.getDescriptor(int.class);
    public static final String descDouble        = org.objectweb.asm.Type.getDescriptor(double.class);
    public static final String descLong          = org.objectweb.asm.Type.getDescriptor(long.class);
    public static final String descBoolean       = org.objectweb.asm.Type.getDescriptor(boolean.class);
    public static final String descCharacter          = org.objectweb.asm.Type.getDescriptor(int.class);
    public static final String descJavaBufferedReader = Naming.internalToDesc(internalJavaBufferedReader);
    public static final String descJavaBufferedWriter = Naming.internalToDesc(internalJavaBufferedWriter);
    public static final String descString        = Naming.internalToDesc(internalString);
    public static final String descObject        = Naming.internalToDesc(internalObject);
    public static final String descClass        = Naming.internalToDesc(internalClass);
    public static final String stringArrayToVoid = Naming.makeMethodDesc(makeArrayDesc(descString), Naming.descVoid);
    public static final String internalFortressIntLiteral  = makeFortressInternal("IntLiteral");
    public static final String internalFortressFloatLiteral = makeFortressInternal("FloatLiteral");
    public static final String internalFortressZZ  = makeFortressInternal("ZZ");
    public static final String internalFortressZZ32  = makeFortressInternal("ZZ32");
    public static final String internalFortressZZ64  = makeFortressInternal("ZZ64");
    public static final String internalFortressNN32  = makeFortressInternal("NN32");
    public static final String internalFortressNN64  = makeFortressInternal("NN64");
    public static final String internalFortressRR32  = makeFortressInternal("RR32");
    public static final String internalFortressRR64  = makeFortressInternal("RR64");
    public static final String internalFortressBoolean  = makeFortressInternal("Boolean");
    public static final String internalFortressCharacter  = makeFortressInternal("Character");
    public static final String internalFortressJavaBufferedReader = makeFortressInternal("JavaBufferedReader");
    public static final String internalFortressJavaBufferedWriter = makeFortressInternal("JavaBufferedWriter");
    // public static final String internalFortressString = makeFortressInternal("String");
    public static final String internalFortressJavaString = makeFortressInternal("JavaString");
    public static final String internalFortressZZ32Vector   = makeFortressInternal("ZZ32Vector");
    public static final String internalFortressStringVector   = makeFortressInternal("StringVector");
    public static final String internalFortressVoid   = makeFortressInternal("Void");
    public static final String internalFortressBottom   = "java/lang/Object"; // internalFortressVoid; // Naming.BOTTOM;

    // fortress interpreter types: type descriptors
    public static final String descFortressIntLiteral  = Naming.internalToDesc(internalFortressIntLiteral);
    public static final String descFortressFloatLiteral  = Naming.internalToDesc(internalFortressFloatLiteral);
    public static final String descFortressZZ  = Naming.internalToDesc(internalFortressZZ);
    public static final String descFortressZZ32  = Naming.internalToDesc(internalFortressZZ32);
    public static final String descFortressZZ64  = Naming.internalToDesc(internalFortressZZ64);
    public static final String descFortressNN32  = Naming.internalToDesc(internalFortressNN32);
    public static final String descFortressNN64  = Naming.internalToDesc(internalFortressNN64);
    public static final String descFortressRR32  = Naming.internalToDesc(internalFortressRR32);
    public static final String descFortressRR64  = Naming.internalToDesc(internalFortressRR64);
    public static final String descFortressBoolean  = Naming.internalToDesc(internalFortressBoolean);
    public static final String descFortressCharacter  = Naming.internalToDesc(internalFortressCharacter);
    public static final String descFortressJavaBufferedReader  = Naming.internalToDesc(internalFortressJavaBufferedReader);
    public static final String descFortressJavaBufferedWriter  = Naming.internalToDesc(internalFortressJavaBufferedWriter);
    // public static final String descFortressString = Naming.internalToDesc(internalFortressString);
    public static final String descFortressJavaString = Naming.internalToDesc(internalFortressJavaString);
    public static final String descFortressVoid   = Naming.internalToDesc(internalFortressVoid);
    public static final String descFortressAny        = Naming.internalToDesc(fortressAny);
    public static final String descFortressBottom        = Naming.internalToDesc(internalFortressBottom);

    public static final String voidToFortressVoid = Naming.makeMethodDesc("", descFortressVoid);

    public static final String descFortressMutableFValueInternal = 
        "Lcom/sun/fortress/compiler/runtimeValues/MutableFValue;" ;

    private static final List<String> extendsObject =
        Collections.singletonList(internalObject);

    public static String deCase(String s) {
        return "_" + Integer.toString(s.hashCode()&0x7fffffff,16);
    }

    public static String deCaseName(PathTaggedApiName ptan) {
        return deCaseName(ptan.name, ptan.source_path);
    }

    public static String deCaseName(APIName s, String sourcePath) {
        return s + "-" + Integer.toString(sourcePath.hashCode()&0x7fffffff,16);
    }

    public static String cachedPathNameForApiAst(String passedPwd, String sourcePath, APIName name) {
        return ProjectProperties.apiFileName(passedPwd,  deCaseName(name, sourcePath));
    }

    public static String cachedPathNameForCompAst(String passedPwd, String sourcePath, APIName name) {
        return ProjectProperties.compFileName(passedPwd,  deCaseName(name, sourcePath));
    }

    public static String dependenceFileNameForCompAst(APIName name, String sourcePath) {
        return ProjectProperties.compFileName(ProjectProperties.ANALYZED_CACHE_DEPENDS_DIR, deCaseName(name, sourcePath));
    }

    public static String dependenceFileNameForApiAst(APIName name, String sourcePath) {
        return ProjectProperties.apiFileName(ProjectProperties.ANALYZED_CACHE_DEPENDS_DIR, deCaseName(name, sourcePath));
    }

    /* Converting names of Fortress entities into Java entities.
     *
     1. FortressLibrary.ZZ32
     what we call it in Fortress

     2. L/com/sun/fortress/compiler/runtimeValues/FZZ32
     the signature encoding of the Java type that we use to represent that.

     3. I
     the signature encoding of Java int.

     4. int
     Java int.

     5. org.objectweb.asm.Type.INT_TYPE
     The asm representation of the type of Java int.

     6. java.lang.Integer.TYPE
     The name of that "class" (int) in a running Java program.

     *
     * For foreign interfaces, there's two translations.  From the foreign
     * type, to the Fortress type we choose for the Fortress interfaces,
     * and then (same as other types) from the Fortress type to the type
     * of its bytecode encoding.
     *
     */

    static private Span span = NodeFactory.makeSpan("Internally generated library name, in source code NamingCzar.");

    static APIName fortLib =
        NodeFactory.makeAPIName(span, WellKnownNames.fortressBuiltin());
    
    static public APIName fortressLibrary() {
        return fortLib;
    }

    static APIName anyLib =
        NodeFactory.makeAPIName(span, WellKnownNames.anyTypeLibrary());

    public static boolean jvmTypeExtendsAny(String jvmArgType ) {
        if (jvmArgType.startsWith("L")) {
            String class_desc = Useful.substring(jvmArgType, 1, -1);
            class_desc = Useful.replace(class_desc, "/", "."); // Leave $ alone for inner classes.
            try {
                Class cl = Class.forName(class_desc);
                if (fortress.AnyType.Any.class.isAssignableFrom(cl)) {
                    // If so, this is a fortress implementation type, allowed to flow through unmolested.
                    return true;
                }
            } catch (ClassNotFoundException e) {
                // Ignore error, report by other route below.
            }
        }
        return false;
    }
    
    /**
     * Given an ASM Type t from foreign Java, what is the corresponding type
     * in Fortress (expressed as an AST Type node)?
     *
     * If it is not defined in the current foreign interface implementation,
     * null is returned.
     */
    // One reference, from ForeignJava.recurOnOpaqueClass
    public static com.sun.fortress.nodes.Type fortressTypeForForeignJavaType(Type t, String method_name, boolean isResultType) {
        String s = t.getDescriptor();
        com.sun.fortress.nodes.Type y = fortressTypeForForeignJavaType(t.getDescriptor(), method_name, isResultType);
        if (y == null) {
            if (jvmTypeExtendsAny(s)) {
                // Special case for now, will generalize later.
                if (s.equals("Lfortress/AnyType$Any;")) {
                    return NodeFactory.makeTraitType(span, false, NodeFactory.makeId(span, anyLib, "Any"));
                } else if (s.equals("Lcom/sun/fortress/compiler/runtimeValues/FZZ32Vector;")) {
                    return NodeFactory.makeTraitType(span, false, NodeFactory.makeId(span, fortLib, "ZZ32Vector"));
                } else if (s.equals("Lcom/sun/fortress/compiler/runtimeValues/FStringVector;")) {
                    return NodeFactory.makeTraitType(span, false, NodeFactory.makeId(span, fortLib, "StringVector"));
                } else if (s.equals("Lcom/sun/fortress/compiler/runtimeValues/FZZ;")) {
                    return NodeFactory.makeTraitType(span, false, NodeFactory.makeId(span, fortLib, "ZZ"));
                } else throw new Error("Unhandled case in import of native method dealing in implementation types, native type is " + s);
            }
        }
        return y;
    }

    /**
     * Given a Java type String descriptor ("Ljava/lang/Object;", V, [J, etc),
     * what is the corresponding type in Fortress
     * (expressed as an AST Type node)?
     *
     * If it is not defined in the current foreign interface implementation,
     * null is returned.
     */
    // Used in this class, in test, in FortressMethodAdapter.toImplFFFF,
    // and in ForeignJava.recurOnOpaqueClass
        public static com.sun.fortress.nodes.Type fortressTypeForForeignJavaType(String s) {
            return specialForeignJavaTranslations.get(s);
        }


    // The following version is needed to hack the methods "jMakeCharacter" and "jCodePoint".
    // If other methods also need to be hacked, then this mechanism should be generalized.
    public static com.sun.fortress.nodes.Type fortressTypeForForeignJavaType(String s, String method_name, boolean isResultType) {
	if (isResultType && method_name.equals("charMakeCharacterWithSpecialCompilerHackForCharacterResultType") && s.equals("I")) {
	    return fortressCharacterType;
	} else if ((!isResultType) && method_name.equals("charCodePointWithSpecialCompilerHackForCharacterArgumentType") && s.equals("I")) {
	    return fortressCharacterType;
	} else if (isResultType && method_name.equals("makeNN32FromZZ32WithSpecialCompilerHackForNN32ResultType") && s.equals("I")) {
	    return fortressNN32Type;
	} else if ((!isResultType) && method_name.equals("makeZZ32FromNN32WithSpecialCompilerHackForNN32ArgumentType") && s.equals("I")) {
	    return fortressNN32Type;
	} else if (isResultType && method_name.equals("makeNN64FromZZ64WithSpecialCompilerHackForNN64ResultType") && s.equals("J")) {
	    return fortressNN64Type;
	} else if ((!isResultType) && method_name.equals("makeZZ64FromNN64WithSpecialCompilerHackForNN64ArgumentType") && s.equals("J")) {
	    return fortressNN64Type;
	} else if (s.equals("[I")) {
	    return fortressZZ32VectorType;
	} else if (s.equals("[String")) {
	    return fortressStringVectorType;
	} else return specialForeignJavaTranslations.get(s);
    }

    // Translate among Java type names
    // (Section 2.1.3 in ASM 3.0: A Java bytecode engineering library)

    /**
     * Strips L and ; off of type; they are assumed to exist.
     * @param desc
     * @return
     */
    public static String descToInternal(String desc) {
        if (desc.startsWith("L") && desc.endsWith(";"))
            return Useful.substring(desc, 1, -1);
        throw new IllegalArgumentException(desc + " did not begin with 'L' and end with ';'");
    }
    // Used once.
    private static String makeArrayDesc(String element) {
        return "[" + element;
    }

    // fortress runtime types: internal names
    private static String makeFortressInternal(String type) {
        return "com/sun/fortress/compiler/runtimeValues/F" + type;
        // return "fortress/CompilerBuiltin/" + type;
    }


    private static Map<String, com.sun.fortress.nodes.Type> specialForeignJavaTranslations =
        new HashMap<String, com.sun.fortress.nodes.Type>();

    /* Minor hackery here -- because we know these types are already loaded
     * and not eligible for ASM-wrapping, we just go ahead and refer to the
     * loaded class.
     */
    private static void s(Class cl, APIName api, String str) {
        s(Type.getType(cl), api, str);
    }

    private static void s(Type cl, APIName api, String str) {
        s(cl.getDescriptor(), api, str);
    }

    private static void s(String cl, APIName api, String str) {
        specialForeignJavaTranslations.put(cl, ss(api, str));
    }

    private static TraitType ss(APIName api, String str) {
	return NodeFactory.makeTraitType(span, false, NodeFactory.makeId(span, api, str));
    }

    static {
        s(Boolean.class, fortLib, "Boolean");
        s(Type.BOOLEAN_TYPE, fortLib, "Boolean");
        s(Integer.class, fortLib, "ZZ32");
        s(Type.INT_TYPE, fortLib, "ZZ32");
        s(Long.class, fortLib, "ZZ64");
        s(Type.LONG_TYPE, fortLib, "ZZ64");
        s(Float.class, fortLib, "RR32");
        s(Type.FLOAT_TYPE, fortLib, "RR32");
        s(Double.class, fortLib, "RR64");
        s(Type.DOUBLE_TYPE, fortLib, "RR64");
        s(Object.class, anyLib, "Any");
        s(String.class, fortLib, "JavaString");
        s(FortressBufferedReader.class, fortLib, "JavaBufferedReader");
        s(FortressBufferedWriter.class, fortLib, "JavaBufferedWriter");
        s(BigInteger.class, fortLib, "ZZ");
        specialForeignJavaTranslations.put("V", NodeFactory.makeVoidType(span));
	// You would think the following would be correct, but it's not, because
	// we actually use Java int values to represent Fortress Character values.
	//        s(Type.CHAR_TYPE,fortLib, "Character");
	//        s(Character.class,fortLib, "Character");
	// Instead, we need a specially coded hack in method fortressTypeForForeignJavaType,
	// and it relies on the following static field definition.
     }
    private static TraitType fortressCharacterType = ss(fortLib, "Character");
    private static TraitType fortressZZ32VectorType = ss(fortLib, "ZZ32Vector");    
    private static TraitType fortressStringVectorType = ss(fortLib, "StringVector");
    private static TraitType fortressNN32Type = ss(fortLib, "NN32");
    private static TraitType fortressNN64Type = ss(fortLib, "NN64");
    private static TraitType fortressZZType = ss(fortLib, "ZZ");   
    
    /**
     * Package prefix for runtime values
     */
    private static final String runtimeValues = com.sun.fortress.runtimeSystem.Naming.runtimeValues;

    public static final String FValueType = runtimeValues + "FValue";
    // static final String FValueDesc = internalToDesc(FValueType);

    static class PartialTypeComparator implements Comparator<com.sun.fortress.nodes.Type>, Serializable {

        @Override
        public int compare(com.sun.fortress.nodes.Type o1,
                com.sun.fortress.nodes.Type o2) {
            if (o1 instanceof TraitSelfType)
                o1 = ((TraitSelfType)o1).getNamed();
            if (o2 instanceof TraitSelfType)
                o2 = ((TraitSelfType)o2).getNamed();

            if (o1 instanceof TraitType && o2 instanceof TraitType) {
                TraitType t1 = (TraitType) o1;
                TraitType t2 = (TraitType) o2;
                Id i1 = t1.getName();
                Id i2 = t2.getName();
                return i1.toString().compareTo(i2.toString());
            } else {
                Class c1 = o1.getClass();
                Class c2 = o2.getClass();
                if (c1 != c2) {
                    return c1.getName().compareTo(c2.getName());
                }
                // TODO We may wish to elaborate this, but traittypes is good enough.
                return 0;
            }
        }

    }


    /**
     * Java descriptors for (boxed) Fortress types, INCLUDING leading L and trailing ;
     */
    private static Map<com.sun.fortress.nodes.Type, String> specialFortressDescriptors =
        new BATree<com.sun.fortress.nodes.Type, String>(new PartialTypeComparator());
    //new HashMap<com.sun.fortress.nodes.Type, String>();
    /**
     * Java descriptors for (boxed) Fortress types, WITHOUT leading L and trailing ;
     */
    private static Map<com.sun.fortress.nodes.Type, String> specialFortressTypes =
        new BATree<com.sun.fortress.nodes.Type, String>(new PartialTypeComparator());
        //new HashMap<com.sun.fortress.nodes.Type, String>();

    private static void bl(APIName api, String str, String cl) {
        b(api,str, runtimeValues+cl);
    }

    private static void bl(com.sun.fortress.nodes.Type t, String cl) {
        b(t, runtimeValues+cl);
    }

    private static void b(APIName api, String str, String cl) {
        b(NodeFactory.makeTraitType(span, false, NodeFactory.makeId(span, api, str)), cl);
        b(NodeFactory.makeTraitType(span, false, NodeFactory.makeId(span, /* api, */ str)), cl);
    }

    private static void b(com.sun.fortress.nodes.Type t, String cl) {
        specialFortressDescriptors.put(t, Naming.internalToDesc(cl));
        specialFortressTypes.put(t, cl );
    }

    static {
        /*
         * This code is duplicated, mostly, in runtime Naming.java,
         * except that it deals only in strings.
         */
        bl(fortLib, "Boolean", "FBoolean");
        bl(fortLib, "Character", "FCharacter");
        bl(fortLib, "JavaBufferedReader", "FJavaBufferedReader");
        bl(fortLib, "JavaBufferedWriter", "FJavaBufferedWriter");
        bl(fortLib, "ZZ", "FZZ");
        bl(fortLib, "ZZ64", "FZZ64");
        bl(fortLib, "ZZ32", "FZZ32");
        bl(fortLib, "NN32", "FNN32");
        bl(fortLib, "NN64", "FNN64");
        bl(fortLib, "RR32", "FRR32");
        bl(fortLib, "RR64", "FRR64");
        bl(fortLib, "JavaString", "FJavaString");
        bl(fortLib, "ZZ32Vector", "FZZ32Vector");
        bl(fortLib, "StringVector", "FStringVector");
        bl(NodeFactory.makeVoidType(span), "FVoid");
    }

    /**
     * Determine whether given TraitType is special, meaning, implemented
     * by hand, with a non-standard naming convention.
     *
     * Note that this is used only once, to determine whether a method
     * invocation should be invoke interface or invoke virtual, and
     * arguably that is a bug in how these are defined.
     */
    public static boolean fortressTypeIsSpecial(com.sun.fortress.nodes.Type t) {
        return specialFortressDescriptors.containsKey(t);
    }

    public static boolean typeIsFortressSpecialType(String t) {
        return specialFortressTypes.containsValue(t);
    }

//    public static String boxedImplDesc(com.sun.fortress.nodes.Type t, APIName ifNone) {
//        return jvmTypeDesc(t, ifNone);
//    }


//    public static String boxedImplType( com.sun.fortress.nodes.Type t, APIName ifNone ) {
//        return jvmTypeDesc(t, ifNone, false);
//    }

    /**
     * @deprecated
     * Converts Fortress API name to Java package name.
     * However, this is probably obsolete, since it appears to be
     * only used in the interpreter code (csf.interpreter.env.ClosureMaker)
     */
    public static String apiNameToPackageName(APIName name) {
        if (ForeignJava.only.definesApi(name)) {
            return Naming.NATIVE_PREFIX_DOT + name.getText();
        } else {
            return javaPackageClassForApi(name, ".");
        }
    }

    /**
     * Given an APIName and function (static method), return the name of
     * the package and class containing the function (static method) in the
     * generated code.
     */
    // Only called from OverloadSet.AmongApis
    public static String apiAndMethodToMethodOwner(APIName name, Functional method) {
        String m = method.toUndecoratedName().toString();
        return apiAndMethodToMethodOwner(name, m);
    }

    /**
     * Given an APIName and function (static method) name, return the name of
     * the package and class containing the function (static method) in the
     * generated code.
     */
    // Only called by preceding method...
    public static String apiAndMethodToMethodOwner(APIName name, String m) {
        String p;
        if (ForeignJava.only.definesApi(name)) {
             p = Naming.NATIVE_PREFIX_DOT + name.getText();
             int idot = m.lastIndexOf(".");
             if (idot != -1) {
                 p = p + "/" + m.substring(0,idot);
             }

        } else {
             p = javaPackageClassForApi(name, ".");
        }
        p = Useful.replace(p, ".", "/") ;
        return p;
    }

    /**
     * Returns name of method, in generated code, for a given API and function.
     *
     * @param name
     * @param method
     * @return
     */
    // Only called from OverloadSet.AmongApis
    public static String apiAndMethodToMethod(APIName name, Functional method) {
        String m = NodeUtil.nameSuffixString(method.toUndecoratedName());// .toString();
        return apiAndMethodToMethod(name, m);
    }

    /**
     * Returns name of method, in generated code, for a given API and function.
     *
     * @param name
     * @param m
     * @return
     */
    // Only called above, and in CodeGen.generateTopLevelOverloads
    public static String apiAndMethodToMethod(APIName name, String m) {
        if (ForeignJava.only.definesApi(name)) {
            int idot = m.lastIndexOf(".");
            if (idot != -1) {
                m = m.substring(idot+1);
            }
        }
        return m;
    }

    /**
     * @deprecated
     * @param componentName
     * @return the name of the class implementing the compiled top-level
     *         environment for component componentName.
     */
    // I think this is old interpreter-centric code.
    public static String classNameForComponentEnvironment(APIName componentName) {
        return classNameForComponentEnvironment(NodeUtil.nameString(componentName));
    }

    /**
     * @deprecated
     * @param componentName
     * @return the name of the class implementing the compiled top-level
     *         environment for component componentName.
     */
    // I think this is old interpreter-centric code.
    public static String classNameForComponentEnvironment(String componentName) {
        componentName = componentName + TopLevelEnvGen.COMPONENT_ENV_SUFFIX;
        componentName = mangleClassIdentifier(componentName);  // Need to mangle the name if it contains "."
        return componentName;
    }

    /**
     * @deprecated
     *
     * @param apiName
     * @return the name of the class implementing the compiled top-level
     *         environment for api apiName
     */
    // I think this is old interpreter-centric code.
    public static String classNameForApiEnvironment(APIName apiName) {
        return classNameForApiEnvironment(NodeUtil.nameString(apiName));
    }

    /**
     * @deprecated
     *
     * @param apiName
     * @return the name of the class implementing the compiled top-level
     *         environment for apiName
     */
    // I think this is old interpreter-centric code.
    public static String classNameForApiEnvironment(String apiName) {
        apiName = apiName + TopLevelEnvGen.API_ENV_SUFFIX;
        apiName = mangleClassIdentifier(apiName);  // Need to mangle the name if it contains "."
        return apiName;
    }

    /**
     * @deprecated
     * @param identifier
     * @return
     */
    private static String mangleClassIdentifier(String identifier) {
        // Is this adequate, given naming freedom?
        String mangledString = identifier.replace(".", "$");
        return mangledString+deCase(mangledString);
    }

    /**
     * @param extendsC
     * @return The names of the Java interfaces providing the mentioned types;
     *         if the extends clause is empty, fills in Object as required.
     */
    public static String [] extendsClauseToInterfaces(List<TraitTypeWhere> extendsC, APIName ifMissing, String erasedName) {
        String [] result = new String[extendsC.size()];
        int i = -1;
        for (TraitTypeWhere ttw : extendsC) {
            i++;
            BaseType parentType = ttw.getBaseType();
            if ( !(parentType instanceof TraitType) ) {
                if ( parentType instanceof AnyType ) {
                    result[i] = fortressAny;
                    continue;
                }
                throw new CompilerError(parentType,
                              errorMsg("Invalid type ",parentType," in extends clause."));
            }
            Id name = ((TraitType)parentType).getName();
            Option<APIName> apiName = name.getApiName();
            APIName api = apiName.unwrap(ifMissing);
            String s1 =  makeInnerClassName(api,name);
            String s2 = jvmTypeDesc(parentType, ifMissing, false);
            if (! s1.equals(s2)) {
                // System.err.println(s1 + " MISMATCH " + s2);
            }
            result[i] = s2;
        }
        return result;
    }

    /**
     * Returns slash-separated concatenation of parts of api name.
     * @param api
     * @return
     */
    public static String javaPackageClassForApi(APIName api) {
        return javaPackageClassForApi(api, "/");
    }

    public static String javaPackageClassForApi(APIName api, String sep) {
        return javaPackageClassForApi(api.getText(), sep);
    }

    /**
     * @param api
     * @return
     */
    private static String javaPackageClassForApi(String api, String sep) {
        boolean defaultLib = WellKnownNames.exportsDefaultLibrary( api );
        if (!(sep.equals("."))) {
            api = Useful.replace(api, ".", sep);
        }
        if (!defaultLib) return api;
        return fortressPackage + sep + api;
    }


    public static String mangleAwayFromOverload(String mname) {
        mname += Naming.NON_OVERLOADED_TAG;
        return mname;
    }
    
    /**
     * functions for generating the name of a generic method used in codegeneration
     */
    /**
     * @param name
     * @param sparams
     */
    // many call sites
    static public String genericMethodName(FnNameInfo x, int selfIndex, APIName ifNone) {
        ArrowType at = x.methodArrowType(selfIndex); // This looks wrong, too.
        String possiblyDottedName = Naming.fmDottedName(idOrOpToString(x.getName()), selfIndex);
        
        String generic_arrow_type = NamingCzar.jvmTypeDesc(at, ifNone,
                false);
        return genericMethodName(possiblyDottedName, generic_arrow_type);    
    }
    
    // DRC-WIP
    // forMethodInvocation
    static public String genericMethodName(IdOrOp name, ArrowType at, APIName ifNone) {
        String generic_arrow_type = NamingCzar.jvmTypeDesc(at, ifNone,
                false);
        
        return genericMethodName(name.getText(), generic_arrow_type);
    }

    /**
     * @param name
     * @param generic_arrow_type
     * @return
     */
    static public String genericMethodName(String name, String generic_arrow_type) {
        /* Just append the schema.
         * TEMP FIX -- do sep w/HEAVY_X.
         * Need to stop substitution on static parameters from the method itself.
         * 
           Do not separate with HEAVY_X, because schema may depend on 
           parameters from parent trait/object.
           (HEAVY_X stops substitution in instantiator).
           */
        return name + Naming.UP_INDEX + Naming.HEAVY_X + generic_arrow_type ;
    }
    

    private static int taskCount = 0;
    public static String gensymTaskName(String packageAndClassName) {
        return packageAndClassName + "$" + "task" + taskCount++;
    }

    private static int implementationCount = 0;
    public static String gensymArrowClassName(String desc) {
        return desc + "$" + "implementation" + implementationCount++;
    }

    public static String gensymArrowClassName(String desc, Span sp) {
        return desc + "$fn@" + sp.toStringWithoutFiles();
    }

    public static String makeInnerClassName(APIName api, Id id) {
        return makeInnerClassName(javaPackageClassForApi(api), id.getText());
    }

    public static String makeInnerClassName(APIName api, Id id, String sparams_part) {
        return makeInnerClassName(javaPackageClassForApi(api), id.getText() + sparams_part);
    }

    // forSubscriptExpr
    public static String makeInnerClassName(Id id) {
        return makeInnerClassName(jvmClassForSymbol(id), id.getText());
    }

    // Naming Czar and CodeGen
    public static String makeInnerClassName(String packageAndClassName, String t) {
        return packageAndClassName + "$" + t;
    }

    // CodeGen.generateHigherOrderCall
    public static String jvmSignatureFor(ArrowType arrow, APIName ifNone) {
        return jvmSignatureFor(arrow.getDomain(), arrow.getRange(), ifNone);
    }

    // Codegen, several methods
    public static String jvmSignatureFor(com.sun.fortress.nodes.Type domain,
                                         com.sun.fortress.nodes.Type range,
                                         APIName ifNone) {
        return jvmSignatureFor(domain, jvmTypeDesc(range, ifNone, true, true), ifNone);
    } // jvmTypeDesc(type, ifNone, true, false)

    //
    private static String jvmSignatureFor(com.sun.fortress.nodes.Type domain,
            String rangeDesc,
            APIName ifNone) {
        // Changing this to do the tuple thing, seems to break "run".  Why?
        return Naming.makeMethodDesc(
                NodeUtil.isVoidType(domain) ? "" : jvmTypeDesc(domain, ifNone),
                        rangeDesc);
    }

    // CodeGen, several methods
    public static String jvmSignatureFor(List<com.sun.fortress.nodes.Param> domain,
            String rangeDesc, APIName ifNone) {
        // This special case handles single void argument type properly.
        if (domain.size() == 1)
            return jvmSignatureFor(NodeUtil.optTypeOrPatternToType(domain.get(0).getIdType()).unwrap(), rangeDesc, ifNone);
        String args = "";
        StringBuilder buf = new StringBuilder();
        for (Param p : domain) {
            buf.append(jvmBoxedTypeDesc((com.sun.fortress.nodes.Type) p.getIdType().unwrap(), ifNone));
        }
        args = buf.toString();
        return Naming.makeMethodDesc(args, rangeDesc);
    }
    
    /**
     * Ultimately used for overloaded methods with self parameters in their list.
     * @param domain
     * @param self_index_to_skip
     * @param rangeDesc
     * @param ifNone
     * @return
     */
    public static String jvmSignatureFor(List<com.sun.fortress.nodes.Param> domain,
            int self_index_to_skip, 
            String rangeDesc, APIName ifNone) {
        // This special case handles single void argument type properly.
        if (domain.size() == 1)
            return jvmSignatureFor(NodeUtil.optTypeOrPatternToType(domain.get(0).getIdType()).unwrap(), rangeDesc, ifNone);
        String args = "";
        StringBuilder buf = new StringBuilder();
        int i = 0;
        for (Param p : domain) {
            if (i != self_index_to_skip)
                buf.append(jvmBoxedTypeDesc((com.sun.fortress.nodes.Type) p.getIdType().unwrap(), ifNone));
            i++;
        }
        args = buf.toString();
        return Naming.makeMethodDesc(args, rangeDesc);
    }
    
    public static String jvmSignatureForNObjects(int n,
            String rangeDesc) {
        // This special case handles single void argument type properly.
        String args = "";
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < n; i++) {
            buf.append("Ljava/lang/Object;");
        }
        args = buf.toString();
        return Naming.makeMethodDesc(args, rangeDesc);
    }

    // CodeGen.forFnDecl
    public static String jvmSignatureFor(com.sun.fortress.nodes.Type domain,
            String rangeDesc, int spliceAt, com.sun.fortress.nodes.Type spliceType, APIName ifNone) {
        String args = "";
        if (spliceAt == 0)
            args += jvmTypeDesc(spliceType, ifNone);

        if (domain instanceof com.sun.fortress.nodes.TupleType) {
            TupleType tt = (TupleType) domain;
            int i = 0;
            StringBuilder buf = new StringBuilder();
            buf.append(args);
            for (com.sun.fortress.nodes.Type p : tt.getElements()) {
                buf.append(jvmTypeDesc(p, ifNone, true, true));
                i++;
                if (spliceAt == i)
                    buf.append(jvmTypeDesc(spliceType, ifNone));
            }
            args = buf.toString();
        } else {
            if (spliceAt < 0)
                return jvmSignatureFor(domain, rangeDesc, ifNone);
            else {
                args += jvmTypeDesc(domain, ifNone, true, true);
                if (spliceAt == 1)
                    args += jvmTypeDesc(spliceType, ifNone);
            }
        }

        return Naming.makeMethodDesc(args, rangeDesc);
    }

    private static String jvmSignatureFor(List<com.sun.fortress.nodes.Param> domain,
            com.sun.fortress.nodes.Type range,
            APIName ifNone) {
        return jvmSignatureFor(domain, jvmBoxedTypeDesc(range, ifNone), ifNone);
    }

    // OverloadSet.jvmSignatureFor
    public static String jvmSignatureFor(Functional f, APIName ifNone) {
        com.sun.fortress.nodes.Type range = f.getReturnType().unwrap();
        return jvmSignatureFor(f.parameters(), range, ifNone);
    }

    // OverloadSet.jvmSignatureFor (method case, no AP
    public static String jvmSignatureFor(Functional f, int self_index_to_skip, APIName ifNone) {
        com.sun.fortress.nodes.Type range = f.getReturnType().unwrap();
        return jvmSignatureFor(f.parameters(), self_index_to_skip, jvmBoxedTypeDesc(range, ifNone), ifNone);
    }

    // Codegen.dumpSigs
    public static String jvmSignatureFor(FnDecl f, APIName ifNone) {
        FnHeader h = f.getHeader();
        // needs to box tuple results
        return jvmSignatureFor(h.getParams(), h.getReturnType().unwrap(), ifNone);
    }

    /**
     * It looks like a bad idea to call this; how can the API possible be right?
     *
     * @deprecated
     * @param fnName
     * @return
     */
    // Seems like a bad idea to call this.
    public static String jvmClassForSymbol(IdOrOp fnName) {
        return jvmClassForSymbol(fnName, "");
    }

    private static String jvmClassForSymbol(IdOrOp fnName, String ifNone) {
        Option<APIName> maybe_api = fnName.getApiName();
        String result = ifNone;
        if (maybe_api.isSome()) {
            APIName apiName = maybe_api.unwrap();
            result = javaPackageClassForApi(apiName);
        }

        Debug.debug(Debug.Type.CODEGEN, 1,
                    "jvmClassForSymbol(", fnName, ")=", result);
        return result;
    }

    /**
     * If a type occurs in a parameter list or return type, it
     * is necessary to determine its name for purpose of generating
     * the signature portion of a Java method name.
     *
     * The Java type that is generated will be an interface type for all
     * trait types, and a final class for all object types.
     *
     * Generic object types yield non-final classes; they are extended by their
     * instantiations (which are final classes).
     */
    // Widely used
    public static String jvmTypeDesc(com.sun.fortress.nodes.Type type,
            final APIName ifNone) {
        return jvmTypeDesc(type, ifNone, true, false);
    }

    /**
     * This deals with the boxed case of a type descriptor, meaning for
     * typle types, Tuple[\ stuff \].  These are for return values,
     * and for local variables.
     * 
     * @param type
     * @param ifNone
     * @return
     */
    public static String jvmBoxedTypeDesc(com.sun.fortress.nodes.Type type,
            final APIName ifNone) {
        return jvmTypeDesc(type, ifNone, true, true);
    }

    public static String jvmBoxedTypeName(com.sun.fortress.nodes.Type type,
            final APIName ifNone) {
        return jvmTypeDesc(type, ifNone, false, true);
    }

    // Local, and Codegen.generateHigherOrderCall
    public static String makeArrowDescriptor(ArrowType t, final APIName ifNone) {

        return makeArrowDescriptor(ifNone, t.getDomain(), t.getRange());

  }

    public static String makeTupleDescriptor(TupleType t, final APIName ifNone,
            boolean specialFortressAsInterfaces) {
        List<com.sun.fortress.nodes.Type> types = t.getElements();
        
        String res =
            "Tuple"+ Naming.LEFT_OXFORD +
            makeUnboxedTupleDescriptor(t, ifNone, specialFortressAsInterfaces) +
            Naming.RIGHT_OXFORD;

        return res;

  }

    public static String[] makeTupleElementDescriptors(TupleType t, final APIName ifNone) {

        List<com.sun.fortress.nodes.Type> types = t.getElements();
        int n = types.size();
        String[] res = new String[n];
        for (int i = 0; i<n; i ++) {
            res[i] = makeBoxedTypeName(types.get(i), ifNone);
        }
          
        return res;

  }

    
//    public static String makeArrowDescriptor(com.sun.fortress.nodes.Type domain,
//            com.sun.fortress.nodes.Type range, final APIName ifNone) {
//
//        String res =
//         "Arrow"+ Naming.LEFT_OXFORD + makeGenericParameterDescriptor(domain, ifNone) + ";" +
//            makeGenericParameterDescriptor(range, ifNone) + Naming.RIGHT_OXFORD;
//        return res;
//
//    }

    private static String makeBoxedArrowName(ArrowType t, final APIName ifNone) {
        return // Naming.NORMAL_TAG +
        makeArrowDescriptor(t,ifNone);
    }

    // Refugee from Overloadset, DID NOT BELONG THERE 
    public static String objectAbstractArrowTypeForNParams(int numParams) {
        return objectFooTypeForNParams(Naming.ABSTRACT_ARROW, numParams);
    }
    public static String objectFooTypeForNParams(String foo, int numParams) {
        StringBuilder ret = new StringBuilder(foo + Naming.LEFT_OXFORD);
        if (numParams > 1) {
            ret.append(Naming.TUPLE_OX ); // return
            for (int i = 0; i < numParams; i++) {
                ret.append(NamingCzar.internalObject ); // params
                if (i < numParams-1)
                    ret.append(Naming.GENERIC_SEPARATOR); // sep among tuple
            }
            ret.append(Naming.RIGHT_OXFORD);
            ret.append(Naming.GENERIC_SEPARATOR); // sep after tuple
        } else
            for (int i = 0; i < numParams; i++) ret.append(NamingCzar.internalObject + Naming.GENERIC_SEPARATOR); // params
        ret.append(NamingCzar.internalObject + Naming.RIGHT_OXFORD); // return
        return ret.toString();
    }
    
    // Refugee from Overloadset, DID NOT BELONG THERE 
    public static  String objectArrowTypeForNParams(int numParams) {
        return objectFooTypeForNParams(Naming.ARROW_TAG, numParams);
    }
    
    // forFnExpr
    public static String makeAbstractArrowDescriptor(
            List<com.sun.fortress.nodes.Param> params,
            com.sun.fortress.nodes.Type rt, APIName ifNone) {
        return makeAnArrowDescriptor(paramsToTypes(params), rt, ifNone, Naming.ABSTRACT_ARROW);
    }

    /**
     * @param params
     * @param rt
     * @param ifNone
     * @param result
     * @return
     */
    private static String makeAnArrowDescriptor(
            List<com.sun.fortress.nodes.Type> params,
            com.sun.fortress.nodes.Type rt, APIName ifNone, String result) {
        result += Naming.LEFT_OXFORD;
        // Always normalize to tuple types.
        if (params.size() > 1)
            params = Useful.<com.sun.fortress.nodes.Type>list(NodeFactory.makeTupleType(params));
        if (params.size() > 0) {
            StringBuilder buf = new StringBuilder();
            buf.append(result);
            for (com.sun.fortress.nodes.Type t : params) {
                buf.append(makeBoxedTypeName(t, ifNone) + Naming.GENERIC_SEPARATOR);
            }
            result = buf.toString();
        } else {
            result = result + Naming.INTERNAL_SNOWMAN + Naming.GENERIC_SEPARATOR;
        }

        result = result + makeBoxedTypeName(rt, ifNone) + Naming.RIGHT_OXFORD;
        return result;
    }

    private static List<com.sun.fortress.nodes.Type> paramsToTypes(
             List<com.sun.fortress.nodes.Param> params) {
        List<com.sun.fortress.nodes.Type> res = new ArrayList(params.size());
        for (com.sun.fortress.nodes.Param p : params) {
            res.add(NodeUtil.optTypeOrPatternToType(p.getIdType()).unwrap());
        }
        // Always normalize to tuple types.
        if (res.size() > 1) {
            res = Useful.<com.sun.fortress.nodes.Type>list(NodeFactory.makeTupleType(res));
        }
        return res;
    }

    // Parameter order differs here (ifNone first) in order to avoid a clash
    // with the List<Param> overloading below.
    public static String makeArrowDescriptor(
            APIName ifNone,
            List<com.sun.fortress.nodes.Type> params,
            com.sun.fortress.nodes.Type rt) {
        return makeAnArrowDescriptor(params, rt, ifNone, "Arrow");
    }

    public static String makeArrowDescriptor(
            APIName ifNone,
            com.sun.fortress.nodes.Type params,
            com.sun.fortress.nodes.Type rt) {
        List<com.sun.fortress.nodes.Type> list_of_type = Useful.list(params);
        // Always normalize to tuple types.
          //  (params instanceof TupleType) ? ((TupleType) params).getElements() : Useful.list(params);
        return makeAnArrowDescriptor(list_of_type, rt, ifNone, "Arrow");
    }

    // forFnExpr
    public static String makeArrowDescriptor(
            List<com.sun.fortress.nodes.Param> params,
            com.sun.fortress.nodes.Type rt, APIName ifNone) {
        return makeArrowDescriptor(ifNone, paramsToTypes(params), rt);
    }

    private static String makeBoxedAnyName(AnyType t, final APIName ifNone) {
        return Naming.ANY_TYPE_CLASS;
    }

    private static String makeBoxedTraitName(TraitType t, final APIName ifNone) {
        //Id id = t.getName();
        //APIName apiName = id.getApiName().unwrap(ifNone);
        //String tag = "";
        //String sep = ".";
//        if (false)
//         {
//            if (WellKnownNames.exportsDefaultLibrary(apiName.getText())) {
//                tag = Naming.INTERNAL_TAG; // warning sign -- internal use only
//                sep = "$"; // for some reason this fails, why?
//            } else if (ForeignJava.only.definesApi(apiName)) {
//                tag = Naming.FOREIGN_TAG; // hot beverage == JAVA
//            } else {
//                tag = Naming.NORMAL_TAG; // smiley face == normal case.
//                sep = "$";
//            }
//
//                // this might be buggy.
//            return tag + apiName + sep + id.getText();
//        } else
            {
            return // Naming.NORMAL_TAG +
            jvmTypeDesc(t, ifNone, false);
        }
    }

    private static String makeBoxedVarName(VarType t, final APIName ifNone) {
        Id id = t.getName();
        String s = id.getText();

        return s;

//        // Don't tag variables.......
//        if (s.startsWith(Naming.YINYANG))
//            return s;
//
//        String tag = Naming.NORMAL_TAG; // has to be a NORMAL_TAG to work.
//        // this might be buggy.
//        return tag + s;
    }


    private static String makeBoxedTupleName(TupleType t, final APIName ifNone) {
        if ( NodeUtil.isVoidType(t) )
            return Naming.SNOWMAN;
        if (t.getVarargs().isSome())
            throw new CompilerError(t,"Can't compile VarArgs yet");
        if (!t.getKeywords().isEmpty())
            throw new CompilerError(t,"Can't compile Keyword args yet");
        return jvmTypeDesc(t, ifNone, false, true);
    }

    private static String makeUnboxedTupleDescriptor(TupleType t, final APIName ifNone, boolean specialFortressAsInterfaces) {
        if ( NodeUtil.isVoidType(t) )
            throw new Error("Unexpected case in unboxed tuple descriptor");
        if (t.getVarargs().isSome())
            throw new CompilerError(t,"Can't compile VarArgs yet");
        if (!t.getKeywords().isEmpty())
            throw new CompilerError(t,"Can't compile Keyword args yet");
        String res = "";
        StringBuilder buf = new StringBuilder();
        for (com.sun.fortress.nodes.Type ty : t.getElements()) {
            buf.append(makeBoxedTypeName(ty, ifNone) +  Naming.GENERIC_SEPARATOR_CHAR);
        }
        res = buf.toString();
        return Useful.substring(res, 0, -1);
    }

    public static String makeBoxedTypeName(com.sun.fortress.nodes.Type t, final APIName ifNone) {
        if (t instanceof TupleType) return makeBoxedTupleName((TupleType) t, ifNone);
        else if (t instanceof TraitSelfType) return makeBoxedTypeName(((TraitSelfType) t).getNamed(), ifNone);
        else if (t instanceof TraitType) return makeBoxedTraitName((TraitType) t, ifNone);
        else if (t instanceof AnyType) return makeBoxedAnyName((AnyType) t, ifNone);
        else if (t instanceof ArrowType) return makeBoxedArrowName((ArrowType) t, ifNone);
        else if (t instanceof VarType) return makeBoxedVarName((VarType) t, ifNone);
        else if (t instanceof UnionType) {
            UnionType ut = (UnionType) t;
            return makeBoxedUnionName(ut, ifNone);
        }
        else if (t instanceof IntersectionType) {
            IntersectionType ut = (IntersectionType) t;
            return makeBoxedIntersectionName(ut, ifNone);
        }
        else if (t instanceof BottomType) {
            // For code that can never be executed.
            // But it might fail to verify, because there is
            // a Java context that is probably more specific
            // than Object.  The tickling example is a FnExpr
            // with return type Bottom.
            
            return "java/lang/Object";
            
        } else
            throw new CompilerError(t, " How did we get here? type = " +
                                     t + " of class " + t.getClass());
    }

    /**
     * @param ut
     * @param ifNone
     * @return
     */
    private static String makeBoxedUnionName(UnionType ut,
            final APIName ifNone) {
        String s = makeBoxedThingName(Naming.UNION, ut.getElements(), ifNone);
        return s;
    }
    
    private static String makeBoxedIntersectionName(IntersectionType ut,
            final APIName ifNone) {
        String s = makeBoxedThingName(Naming.INTERSECTION, ut.getElements(), ifNone);
        return s;
    }
    
    private static String makeBoxedThingName(String thing,
            List<com.sun.fortress.nodes.Type> lt,
            final APIName ifNone) {
        StringBuilder buf = new StringBuilder();
        buf.append(thing);
        buf.append(Naming.LEFT_OXFORD);
        String prepend = "";
        for (com.sun.fortress.nodes.Type ty : lt) {
            buf.append(prepend);
            prepend = Naming.GENERIC_SEPARATOR;
            buf.append(makeBoxedTypeName(ty, ifNone));
        }
        buf.append(Naming.RIGHT_OXFORD);
        return buf.toString();
    }

    private static String jvmTypeDescs(List<com.sun.fortress.nodes.Type> types,
                                      final APIName ifNone, boolean withLSemi, boolean boxedTuples, boolean specialFortressAsInterfaces) {
        String r = "";
        StringBuilder buf = new StringBuilder();
        for (com.sun.fortress.nodes.Type t : types) {
            buf.append(jvmTypeDesc(t, ifNone, withLSemi, boxedTuples, specialFortressAsInterfaces));
        }
        r = buf.toString();
        return r;
    }

    public static String jvmTypeDesc(final com.sun.fortress.nodes.Type type,
            final APIName ifNone,
            final boolean withLSemi) {
        return jvmTypeDesc(type, ifNone, withLSemi, false, false);
    }

    public static String jvmTypeDescAsTrait(final com.sun.fortress.nodes.Type type,
            final APIName ifNone) {
        return jvmTypeDesc(type, ifNone, false, false, true);
    }

            
    public static String jvmTypeDesc(final com.sun.fortress.nodes.Type type,
            final APIName ifNone,
            final boolean withLSemi,
            final boolean boxed) {
        return jvmTypeDesc(type, ifNone, withLSemi, boxed, false);
    }
    
    private static String jvmTypeDesc(final com.sun.fortress.nodes.Type type,
                                     final APIName ifNone,
                                     final boolean withLSemi,
                                     final boolean boxed,
                                     final boolean specialFortressAsInterfaces) {
        return type.accept(new NodeAbstractVisitor<String>() {
            @Override
            public String defaultCase(Node x) {
                throw new CompilerError(x,"emitDesc of type "+x+" failed");
            }
            @Override
            public String forArrowType(ArrowType t) {
                String res = makeArrowDescriptor(t, ifNone);

                if (withLSemi) res = Naming.internalToDesc(res);
                return res;
            }
            @Override
            public String forBottomType(BottomType t) {
                    return withLSemi ? descFortressBottom : internalFortressBottom;
            }
            @Override
            public String forUnionType(UnionType t) {
                String s = makeBoxedUnionName(t, ifNone);
                return withLSemi ? "L" + s + ";" : s;
            }
            @Override
            public String forIntersectionType(IntersectionType t) {
                String s = makeBoxedIntersectionName(t, ifNone);
                return withLSemi ? "L" + s + ";" : s;
            }
            @Override
            public String forTupleType(TupleType t) {
                if ( NodeUtil.isVoidType(t) )
                    return withLSemi ? descFortressVoid : internalFortressVoid;
                if (t.getVarargs().isSome())
                    throw new CompilerError(t,"Can't compile VarArgs yet");
                if (!t.getKeywords().isEmpty())
                    throw new CompilerError(t,"Can't compile Keyword args yet");
                if (boxed) {
                    String res = makeTupleDescriptor(t, ifNone, specialFortressAsInterfaces);
                    if (withLSemi) res = Naming.internalToDesc(res);
                    return res;
                } else
                return jvmTypeDescs(t.getElements(), ifNone, true, true, specialFortressAsInterfaces);
            }
            @Override
            public String forAnyType (AnyType t) {
                return withLSemi ? descFortressAny : fortressAny;
            }
            @Override
            public String forVarType (VarType t) {
                //
                String s = t.getName().getText();
                if (withLSemi) s = Naming.internalToDesc(s);
                return s;
            }
            @Override
            public String forTraitSelfType(TraitSelfType t) {
                return t.getNamed().accept(this);
            }
            @Override
            public String forTraitType(TraitType t) {
                // I think this is wrong!  What about API names?
                // What about foreign-implemented types?
                // - DRC 2009-08-10
                String result = specialFortressAsInterfaces ? null :
                    (withLSemi ? specialFortressDescriptors :
                        specialFortressTypes).get(t);
                if (result != null) {
                    Debug.debug(Debug.Type.CODEGEN, 1, "forTrait Type ", t ,
                                " builtin ", result);
                    if (! withLSemi)
                        return result;
                    return result;
                }
                Id id = t.getName();
                APIName api = id.getApiName().unwrap(ifNone);
                if (api == null) {
                    throw new CompilerError(id,"no api name given for id");
                }
                List<StaticParam> sparams = t.getTraitStaticParams();
                List<StaticArg> sargs = t.getArgs();

                // TODO work in progress -- need to expand with StaticArg if those are available.
                if (sargs.size() > 0) {
                    result = makeInnerClassName(api,id, instantiatedGenericDecoration(sargs, ifNone));
                    if (sparams.size() > 0)
                        throw new CompilerError(id,"Static args and params both non-empty, what wins? " +  type);
                } else
                    result = makeInnerClassName(api,id, forStaticParams(sparams));

                if (withLSemi)
                    result = Naming.internalToDesc(result);
                Debug.debug(Debug.Type.CODEGEN, 1, "forTrait Type ", t, " = ", result);

                return result;
            }

            private String forStaticParams(List<StaticParam> sparams) {
                if (sparams.size() == 0)
                    return "";
                StringBuilder sparams_part = new StringBuilder();
                String pfx = Naming.LEFT_OXFORD;
                for (StaticParam p : sparams) {
                    sparams_part.append(pfx);
                    pfx = Naming.GENERIC_SEPARATOR;
                    sparams_part.append(p.getName().accept(this));
                }
                sparams_part.append(Naming.RIGHT_OXFORD);
                return sparams_part.toString();
            }
            @Override
            public String forIdOrOp(IdOrOp that) {
                // TODO This is not going to work, need to figure out what we do with oprefs.
                Pair<String, String> p = idToPackageClassAndName(that, ifNone);
                return makeInnerClassName(p.first(), p.second());
            }

            });
    }



    /* Clone of above, to clean things out, TYPE DESCRIPTORS != METHOD DESCRIPTORS */
    // Codegen.resolveMethodAndSignature
    public static String jvmMethodDesc(com.sun.fortress.nodes.Type type,
            final APIName ifNone)  {
        return type.accept(new NodeAbstractVisitor<String>() {
            @Override
            public String defaultCase(Node x) {
                throw new CompilerError(x,"methodDesc of type "+x+" failed");
            }
            @Override
            public String forArrowType(ArrowType t) {
                if (NodeUtil.isVoidType(t.getDomain()))
                    return Naming.makeMethodDesc("", jvmTypeDesc(t.getRange(), ifNone, true, true));
                else return Naming.makeMethodDesc(jvmTypeDesc(t.getDomain(), ifNone, true, false),
                                           jvmTypeDesc(t.getRange(), ifNone, true, true));
            }

            // TODO CASES BELOW OUGHT TO JUST FAIL, WILL TEST SOON.
            @Override
            public String forTupleType(TupleType t) {
                if ( NodeUtil.isVoidType(t) )
                    return descFortressVoid;
                if (t.getVarargs().isSome())
                    throw new CompilerError(t,"Can't compile VarArgs yet");
                if (!t.getKeywords().isEmpty())
                    throw new CompilerError(t,"Can't compile Keyword args yet");
                String res = "";
                StringBuilder buf = new StringBuilder();
                for (com.sun.fortress.nodes.Type ty : t.getElements()) {
                    buf.append(jvmTypeDesc(ty, ifNone));
                }
                res = buf.toString();
                return res;
            }
            @Override
            public String forAnyType (AnyType t) {
                return descFortressAny;
            }
            @Override
            public String forTraitSelfType(TraitSelfType t) {
                return t.getNamed().accept(this);
            }
            @Override
            public String forTraitType(TraitType t) {
                String result = specialFortressDescriptors.get(t);
                if (result != null) {
                    Debug.debug(Debug.Type.CODEGEN, 1, "forTrait Type ", t ,
                                " builtin ", result);
                    return result;
                }
                Id id = t.getName();
                APIName api = id.getApiName().unwrap(ifNone);
                if (api == null) {
                    throw new CompilerError(id,"no api name given for id");
                }
                result = makeInnerClassName(api,id);
                result = Naming.internalToDesc(result);
                Debug.debug(Debug.Type.CODEGEN, 1, "forTrait Type ", t, " = ", result);

                return result;
            }
            });
    }

    private static String jvmTypeDesc(Option<com.sun.fortress.nodes.Type> otype, APIName ifNone) {
        if (!otype.isSome()) {
            throw new CompilerError("Expected type information was absent.");
        }
        return jvmTypeDesc(otype.unwrap(), ifNone);
    }

    // CodegenMethodVisitor
    public static List<String> parseArgs(String desc) {
        List<String> args = new ArrayList<String>();
        if (desc.charAt(0) != '(') throw new CompilerError("Bad Type Descriptor:" + desc);
        int i = 1;
        int start = 0;
        int ch = desc.charAt(i);


        while (ch != ')') {
            switch(ch) {
            case 'B':
            case 'S':
            case 'F':
            case 'D':
            case 'C':
            case 'I':
            case 'J':
            case 'Z':
                args.add(Character.toString(desc.charAt(i))); ch = desc.charAt(++i); break;
            case '[':
            case 'L':
                start = i;
                while (ch != ';') {
                    ch = desc.charAt(++i);
                }
                args.add(desc.substring(start, ++i));
                ch = desc.charAt(i);
                break;
            default: throw new CompilerError("Bad Type Descriptor:" + desc);
            }
        }
        return args;
    }


    // CodeGenMethodVisitor
    public static String parseResult(String desc) {
        int i = desc.lastIndexOf(')') + 1;
        int ch = desc.charAt(i);
        int start;
        switch(ch) {
        case 'B':
        case 'S':
        case 'F':
        case 'D':
        case 'C':
        case 'I':
        case 'J':
        case 'Z':
        case 'V':
            return Character.toString(desc.charAt(i));
        case '[':
            start = i;
            while (ch != ']') {
                ch = desc.charAt(++i);
            }
            return desc.substring(start, ++i);
        case 'L':
            start = i;
            while (ch != ';') {
                ch = desc.charAt(++i);
            }
            return desc.substring(start, ++i);
        default: throw new CompilerError("Bad Type Descriptor:" + desc);
        }
    }

    // Codegen.taskConstructorDesc
    public static String
            jvmTypeDescForGeneratedTaskInit(List<com.sun.fortress.nodes.Type> fvtypes,
                                            APIName ifNone) {
        // May need to box any tuples, not really sure yet.
        return "(" + jvmTypeDescs(fvtypes, ifNone, true, false, false) + ")V";
    }

    /** Type name for class containing singleton binding of toplevel
     * entity, or declaration of toplevel type.
     *
     * TODO: call this from code above when referencing types by name.
     */
    // Called from CodeGen
    public static String jvmClassForToplevelDecl(IdOrOp x, String api) {
        api = repairedApiName(x, api);
        return makeInnerClassName(api, x.getText());
    }

    public static String jvmClassForToplevelTypeDecl(IdOrOp x, String sparams_part, String api) {
        api = repairedApiName(x, api);
        return makeInnerClassName(api, x.getText()+sparams_part);
    }

    public static String jvmClassForToplevelTypeDecl(IdOrOp x, List<String> opr_args, String api) {
        api = repairedApiName(x, api);
        return makeInnerClassName(api, Naming.oprArgAnnotatedRTTI(x.getText(), opr_args));
    }

    /**
     * @param id_or_op
     * @param default_api
     * @return
     */
    public static String repairedApiName(IdOrOp id_or_op, String default_api) {
        Option<APIName> actualApiOpt = id_or_op.getApiName();
        if (actualApiOpt.isSome()) {
            default_api = javaPackageClassForApi(actualApiOpt.unwrap());
        }
        return default_api;
    }
    
    /**
     * @param id_or_op
     * @param default_api
     * @return
     */
    public static String repairedApiName(IdOrOp id_or_op, APIName default_api) {
        Option<APIName> actualApiOpt = id_or_op.getApiName();
        if (actualApiOpt.isSome()) {
            default_api = actualApiOpt.unwrap();
        }
        String api_string = javaPackageClassForApi(default_api);
        return api_string;
    }
    
    public static String jvmClassForToplevelTypeDecl(String api, String local, String sparams_part) {
        return makeInnerClassName(api, local+sparams_part);
    }
    // Variant of above
    public static String jvmClassForToplevelTypeDecl(IdOrOp x, String sparams_part, APIName api) {
        String api_string = repairedApiName(x, api);
        return makeInnerClassName(api_string, x.getText()+sparams_part);
    }


    /**
     * The name of a renamed, lifted coercion declaration for the given trait.
     */
    public static String makeLiftedCoercionName(Id traitName) {
        return LIFTED_COERCION_PREFIX + traitName.getText();
    }

    /**
     * @param fnName
     * @return
     */
    public static String idOrOpToString(IdOrOp fnName) {
      
        if (fnName instanceof Op)
            return NamingCzar.opToString((Op) fnName);
        else if (fnName instanceof Id)
            return NamingCzar.idToString((Id) fnName);
        else
            return fnName.getText();

    }

    /**
     * @param op
     * @return
     */
    public static String opToString(Op op) {
        if (true) 
            return OprUtil.fixityDecorator(op.getFixity(), op.getText());
       // Conflicts with above!
        Fixity fixity = op.getFixity();
        if (fixity instanceof PreFixity) {
            return op.getText() + Naming.BOX;
        } else if (fixity instanceof PostFixity) {
            return Naming.BOX + op.getText();
        } else {
          return op.getText();
        }
    }

    /**
     * @param method
     * @return
     */
    public static String  idToString(Id id) {
        return id.getText();
    }

    // This might generalize beyond functions
    public static Pair<String, String> idToPackageClassAndName(IdOrOp fnName, APIName ifMissingApi) {
        Option<APIName> possibleApiName = fnName.getApiName();

        /* Note that after pre-processing in the overload rewriter,
         * there is only one name here; this is not an overload check.
         */
        String calleePackageAndClass = "";
        String method = idOrOpToString(fnName);

        if (!possibleApiName.isSome()) {
            // NOT Foreign, calls same component.
            // Nothing special to do.
            calleePackageAndClass = javaPackageClassForApi(ifMissingApi);
        } else {
            APIName apiName = possibleApiName.unwrap();
            if (!ForeignJava.only.definesApi(apiName)) {
                // NOT Foreign, calls other component.
                calleePackageAndClass =
                    javaPackageClassForApi(apiName);
            } else {
                // Foreign function call
                // TODO this prefix op belongs in naming czar.
                String n = Naming.NATIVE_PREFIX_DOT + fnName;
                // Cheating by assuming class is everything before the dot.
                int lastDot = n.lastIndexOf(".");
                calleePackageAndClass = n.substring(0, lastDot).replace(".", "/");
                method = n.substring(lastDot+2);
                int foreign_tag = method.indexOf(Naming.FOREIGN_TAG);
                calleePackageAndClass = calleePackageAndClass + "/" + method.substring(0, foreign_tag);
                method = method.substring(foreign_tag+1);
                int paren_tag = method.indexOf("(");
                if (paren_tag != -1)
                    method = method.substring(0, paren_tag);
            }
        }
        Pair<String, String> calleeInfo =
            new Pair<String, String>(calleePackageAndClass, method);
        return calleeInfo;
    }

    public static Triple<String,String,Integer> p(String s1) {
        return new Triple<String,String,Integer>(s1, null, 0);
    }
    
    public static Triple<String,String,Integer> p(String s1, String s2) {
        return new Triple<String,String,Integer>(s1, s2, 0);
    }
    
    public static Triple<String,String,Integer> p(String s1, Triple<String,String,Integer> ps2) {
        return new Triple<String,String,Integer>(s1, ps2.getB(), ps2.getC());
    }
    
    public static Triple<String,String,Integer> p(Triple<String,String,Integer> ps1, Triple<String,String,Integer> ps2) {
        return new Triple<String,String,Integer>(ps1.getA(), ps2.getB(), ps2.getC());
    }
    
    public static NodeAbstractVisitor<Triple<String,String,Integer>>
        spkTagger(final APIName ifMissing)
        { return new NodeAbstractVisitor<Triple<String,String,Integer>> () {

        @Override
        public Triple<String,String,Integer> forKindBool(KindBool that) {
            return p(Naming.XL_BOOL);
        }

        @Override
        public Triple<String,String,Integer> forKindDim(KindDim that) {
            return p(Naming.XL_DIM);
        }

        @Override
        public Triple<String,String,Integer> forKindInt(KindInt that) {
            return p(Naming.XL_INTNAT);
        }

        @Override
        public Triple<String,String,Integer> forKindNat(KindNat that) {
            // nats and ints go with same encoding; no distinction in args
            return p(Naming.XL_INTNAT);
        }

        @Override
        public Triple<String,String,Integer> forKindOp(KindOp that) {
            return p(Naming.XL_OPR);
        }

        @Override
        public Triple<String,String,Integer> forKindType(KindType that) {
            return p(Naming.XL_TYPE);
        }

        @Override
        public Triple<String,String,Integer> forKindUnit(KindUnit that) {
            return p(Naming.XL_UNIT); 
        }

        @Override
        public Triple<String,String,Integer> forBoolBase(BoolBase b) {
            // need these to be manifest constants for any evaluation
            return p(Naming.XL_BOOL, b.isBoolVal() ? "true" : "false");
        }

        @Override
        public Triple<String,String,Integer> forBoolRef(BoolRef b) {
            return p(Naming.XL_BOOL, b.getName().getText());
        }

        @Override
        public Triple<String,String,Integer> forBoolBinaryOp(BoolBinaryOp b) {
            BoolExpr l = b.getLeft();
            BoolExpr r = b.getRight();
            Op op = b.getOp();
            return p(Naming.XL_BOOL, l.accept(this).getB() + Naming.ENTER + r.accept(this).getB() + Naming.ENTER + op.getText());
        }

        @Override
        public Triple<String,String,Integer> forBoolUnaryOp(BoolUnaryOp b) {
            BoolExpr v = b.getBoolVal();
            Op op = b.getOp();
            return p(Naming.XL_BOOL, v.accept(this).getB() + Naming.ENTER + op.getText());
        }

        /* These need to return encodings of Fortress types. */
        @Override
        public Triple<String,String,Integer> forBoolArg(BoolArg that) {
            BoolExpr arg = that.getBoolArg();

            return  p(Naming.XL_BOOL, arg.accept(this));
        }

        @Override
        public Triple<String,String,Integer> forDimArg(DimArg that) {
            return p(Naming.XL_DIM, that.getDimArg().accept(this));
        }

        @Override
        public Triple<String,String,Integer> forIntBase(IntBase b) {
            return p(Naming.XL_INTNAT, String.valueOf(b.getIntVal()));
        }

        @Override
        public Triple<String,String,Integer> forIntRef(IntRef b) {
            return p(Naming.XL_INTNAT, b.getName().getText());
        }

        @Override
        public Triple<String,String,Integer> forIntBinaryOp(IntBinaryOp b) {
            IntExpr l = b.getLeft();
            IntExpr r = b.getRight();
            Op op = b.getOp();
            return p(Naming.XL_INTNAT,l.accept(this).getB() + Naming.ENTER + r.accept(this).getB() + Naming.ENTER + op.getText());
        }

       @Override
        public Triple<String,String,Integer> forIntArg(IntArg that) {
            IntExpr arg = that.getIntVal();
            return p(Naming.XL_INTNAT,  arg.accept(this));
        }

        @Override
        public Triple<String,String,Integer> forOpArg(OpArg that) {
            IdOrOp name = that.getId();
            return p(Naming.XL_OPR, name.getText());
        }

        @Override
        public Triple<String,String,Integer> forTypeArg(TypeArg that) {
            com.sun.fortress.nodes.Type arg = that.getTypeArg();
            // Pretagged with type information
            String s =  makeBoxedTypeName(arg, ifMissing);
            return p(Naming.XL_TYPE, s);
        }

        @Override
        public Triple<String,String,Integer> forUnitArg(UnitArg that) {
            //UnitExpr arg = that.getUnitArg();
            return p(Naming.XL_UNIT, that.getUnitArg().accept(this).getB());
        }

        @Override
        public Triple<String,String,Integer> forTraitSelfType(TraitSelfType that) {
            // TODO Auto-generated method stub
            return that.getNamed().accept(this);
        }

        @Override
        public Triple<String,String,Integer> forTraitType(TraitType that) {
            String s =  makeBoxedTraitName(that, ifMissing);
            return p(Naming.XL_TYPE, s);
        }

    };
    }

    /**
     * generates string representing the genericity based on the list of static type parameters
     * adds information to xldata if non-null
     *
     * @param sparams - static parameter list
     * @param xldata - translation data: information added when used to generate output, can be null
     * @param ifMissing - default API
     * @return
     */
    public static String genericDecoration(List<StaticParam> sparams,
            Naming.XlationData xldata,
            APIName ifMissing
            ) {
        return genericDecoration(null, sparams, xldata, ifMissing);
    }
    /**
     * generates string representing the genericity based on the list of static type parameters
     * adds information to xldata if non-null
     * 
     * @param receiverType - Type of the receiver added when converting methods to java static methods.  May be null.
     * @param sparams - static parameter list
     * @param xldata - translation data: information added when used to generate output, can be null
     * @param ifMissing - default API
     * @return
     */
    public static String genericDecoration(com.sun.fortress.nodes.Type receiverType, List<StaticParam> sparams,
            Naming.XlationData xldata,
            APIName ifMissing
            ) {
        if (sparams.size() == 0)
            return "";

        NodeAbstractVisitor<Triple<String,String,Integer>> spkTagger = spkTagger(ifMissing);
        
        String frag = Naming.LEFT_OXFORD;
        StringBuilder buf = new StringBuilder();
        buf.append(frag);
        if (receiverType != null) {
            Triple<String,String,Integer> s = receiverType.accept(spkTagger);
            if (xldata != null)
                xldata.addKindAndNameToStaticParams(s);
            buf.append(s.getB() + Naming.GENERIC_SEPARATOR);
        }
        for (StaticParam sp : sparams) {
            StaticParamKind spk = sp.getKind();
            String k = spk.accept(spkTagger).getA();
            
            IdOrOp spn = sp.getName();
            String s = spn.getText();
            if (xldata != null)
                xldata.addKindAndNameToStaticParams(k,s);
            buf.append(s + Naming.GENERIC_SEPARATOR);
        }
        frag = buf.toString();
        // TODO Auto-generated method stub
        return Useful.substring(frag, 0, -1) + Naming.RIGHT_OXFORD;
    }

    /**
     * NOTE: similar to genericDecoration, but intended for an instantiated generic
     * 
     * @param sargs - list of instantiated static type arguments
     * @param ifMissing - default API
     * @return string representing the generic instantiation
     */
    public static String instantiatedGenericDecoration(List<StaticArg> sargs,
            APIName ifMissing) {
        // TODO we need to make the conventions for Arrows and other static types converge.
        if (sargs.size() == 0)
            return "";

        NodeAbstractVisitor<Triple<String,String,Integer>> spkTagger = spkTagger(ifMissing);

        String frag = Naming.LEFT_OXFORD;
        StringBuilder buf = new StringBuilder();
        buf.append(frag);
        int index = 1;
        for (StaticArg sp : sargs) {
            Triple<String,String,Integer> tag = sp.accept(spkTagger);
            buf.append(tag.getB());
            buf.append(Naming.GENERIC_SEPARATOR);
            index++;
        }
        frag = buf.toString();
        return Useful.substring(frag,0,-1) + Naming.RIGHT_OXFORD;
    }
}
