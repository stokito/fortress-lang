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

package com.sun.fortress.compiler;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.fortress.compiler.environments.TopLevelEnvGen;
import com.sun.fortress.compiler.index.Function;
import com.sun.fortress.compiler.optimization.Unbox.Contains;
import com.sun.fortress.compiler.typechecker.TypeAnalyzer;
import com.sun.fortress.exceptions.CompilerError;
import com.sun.fortress.nodes.BoolArg;
import com.sun.fortress.nodes.BoolBase;
import com.sun.fortress.nodes.BoolBinaryOp;
import com.sun.fortress.nodes.BoolExpr;
import com.sun.fortress.nodes.BoolRef;
import com.sun.fortress.nodes.BoolUnaryOp;
import com.sun.fortress.nodes.DimArg;
import com.sun.fortress.nodes.DimExpr;
import com.sun.fortress.nodes.Fixity;
import com.sun.fortress.nodes.FunctionalRef;
import com.sun.fortress.nodes.IntArg;
import com.sun.fortress.nodes.IntBase;
import com.sun.fortress.nodes.IntBinaryOp;
import com.sun.fortress.nodes.IntExpr;
import com.sun.fortress.nodes.IntRef;
import com.sun.fortress.nodes.KindBool;
import com.sun.fortress.nodes.KindDim;
import com.sun.fortress.nodes.KindInt;
import com.sun.fortress.nodes.KindNat;
import com.sun.fortress.nodes.KindOp;
import com.sun.fortress.nodes.KindType;
import com.sun.fortress.nodes.KindUnit;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.AnyType;
import com.sun.fortress.nodes.ArrowType;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.BottomType;
import com.sun.fortress.nodes.FnDecl;
import com.sun.fortress.nodes.FnHeader;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdOrOp;
import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.nodes.NamedType;
import com.sun.fortress.nodes.Op;
import com.sun.fortress.nodes.OpArg;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.PostFixity;
import com.sun.fortress.nodes.PreFixity;
import com.sun.fortress.nodes.SelfType;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.StaticParamKind;
import com.sun.fortress.nodes.TraitSelfType;
import com.sun.fortress.nodes.TraitType;
import com.sun.fortress.nodes.TraitTypeWhere;
import com.sun.fortress.nodes.TupleType;
import com.sun.fortress.nodes.TypeArg;
import com.sun.fortress.nodes.UnitArg;
import com.sun.fortress.nodes.UnitExpr;
import com.sun.fortress.nodes.VarType;
import com.sun.fortress.nodes.NodeAbstractVisitor;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.repository.ForeignJava;
import com.sun.fortress.repository.GraphRepository;
import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.runtimeSystem.Naming;

import com.sun.fortress.scala_src.useful.STypesUtil;

import com.sun.fortress.useful.BATree;
import com.sun.fortress.useful.Debug;
import com.sun.fortress.useful.F;
import com.sun.fortress.useful.Useful;

import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.Pair;

import org.objectweb.asm.Type;

import static com.sun.fortress.exceptions.InterpreterBug.bug;
import static com.sun.fortress.exceptions.ProgramError.error;
import static com.sun.fortress.exceptions.ProgramError.errorMsg;

public class NamingCzar {
    private NamingCzar() { super(); }

    private final static boolean logLoads = ProjectProperties.getBoolean("fortress.log.classloads", false);

    public static final String COERCION_NAME = "coerce";
    public static final String LIFTED_COERCION_PREFIX = "coerce_";

    public static final Id SELF_NAME = NodeFactory.makeId(NodeFactory.internalSpan, "self");

    /** Name for sole field of top-level singleton class representing a
     *  top-level binding.  Should not need to be particularly
     *  unambiguous, as other fields can't occur in the class, right?
     */
    public static final String SINGLETON_FIELD_NAME = "ONLY";

    public static final String springBoard = "$DefaultTraitMethods";
    public static final String make = "make";

    public static final String cache = ProjectProperties.BYTECODE_CACHE_DIR + "/";
    public static final String optimizedcache = ProjectProperties.OPTIMIZED_BYTECODE_CACHE_DIR + "/";

    //Asm requires you to call visitMaxs for every method
    // but ignores the arguments.
    public static final int ignore = 1;

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
    public static final String internalChar       = org.objectweb.asm.Type.getInternalName(char.class);
    public static final String internalObject     = org.objectweb.asm.Type.getInternalName(Object.class);
    public static final String internalString     = org.objectweb.asm.Type.getInternalName(String.class);
    public static final String internalSingleton  = internalObject;

    public static final String descFloat         = org.objectweb.asm.Type.getDescriptor(float.class);
    public static final String descInt           = org.objectweb.asm.Type.getDescriptor(int.class);
    public static final String descDouble        = org.objectweb.asm.Type.getDescriptor(double.class);
    public static final String descLong          = org.objectweb.asm.Type.getDescriptor(long.class);
    public static final String descBoolean       = org.objectweb.asm.Type.getDescriptor(boolean.class);
    public static final String descChar          = org.objectweb.asm.Type.getDescriptor(char.class);
    public static final String descString        = internalToDesc(internalString);
    public static final String descVoid          = org.objectweb.asm.Type.getDescriptor(void.class);
    public static final String stringArrayToVoid = makeMethodDesc(makeArrayDesc(descString), descVoid);
    public static final String voidToVoid        = makeMethodDesc("", descVoid);

    public static final String internalFortressIntLiteral  = makeFortressInternal("IntLiteral");
    public static final String internalFortressFloatLiteral = makeFortressInternal("FloatLiteral");
    public static final String internalFortressZZ32  = makeFortressInternal("ZZ32");
    public static final String internalFortressZZ64  = makeFortressInternal("ZZ64");
    public static final String internalFortressRR32  = makeFortressInternal("RR32");
    public static final String internalFortressRR64  = makeFortressInternal("RR64");
    public static final String internalFortressBoolean  = makeFortressInternal("Boolean");
    public static final String internalFortressChar  = makeFortressInternal("Char");
    public static final String internalFortressString = makeFortressInternal("String");
    public static final String internalFortressVoid   = makeFortressInternal("Void");

    // fortress interpreter types: type descriptors
    public static final String descFortressIntLiteral  = internalToDesc(internalFortressIntLiteral);
    public static final String descFortressFloatLiteral  = internalToDesc(internalFortressFloatLiteral);
    public static final String descFortressZZ32  = internalToDesc(internalFortressZZ32);
    public static final String descFortressZZ64  = internalToDesc(internalFortressZZ64);
    public static final String descFortressRR32  = internalToDesc(internalFortressRR32);
    public static final String descFortressRR64  = internalToDesc(internalFortressRR64);
    public static final String descFortressBoolean  = internalToDesc(internalFortressBoolean);
    public static final String descFortressChar  = internalToDesc(internalFortressChar);
    public static final String descFortressString = internalToDesc(internalFortressString);
    public static final String descFortressVoid   = internalToDesc(internalFortressVoid);
    public static final String descFortressAny        = internalToDesc(fortressAny);

    public static final String voidToFortressVoid = makeMethodDesc("", descFortressVoid);

    public static final String closureFieldName = "closure";

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

    static Span span = NodeFactory.internalSpan;

    static APIName fortLib =
        NodeFactory.makeAPIName(span, WellKnownNames.fortressBuiltin());

    /**
     * Given an ASM Type t from foreign Java, what is the corresponding type
     * in Fortress (expressed as an AST Type node)?
     *
     * If it is not defined in the current foreign interface implementation,
     * null is returned.
     */
    // One reference, from ForeignJava.recurOnOpaqueClass
    public static com.sun.fortress.nodes.Type fortressTypeForForeignJavaType(Type t) {
        return fortressTypeForForeignJavaType(t.getDescriptor());
    }

    /**
     * Given a Java type String descriptor ("Ljava/lang/Object;", V, [J, etc),
     * what is the corresponding type in Fortress
     * (expressed as an AST Type node)?
     *
     * If it is not defined in the current foreign interface implementation,
     * null is returned.
     */
    // Used in this class, in test, and in SignatureParser.toImplFFFF
    public static com.sun.fortress.nodes.Type fortressTypeForForeignJavaType(String s) {
        return specialForeignJavaTranslations.get(s);
    }

    // Translate among Java type names
    // (Section 2.1.3 in ASM 3.0: A Java bytecode engineering library)

    /**
     * Convert an ASM internal form to a Java descriptor form.
     * That is, surround a class type with L and ;
     */
    // Widely used
    public static String internalToDesc(String type) {
        return "L" + type + ";";
    }
    /**
     * Returns "(" + param + ")" + result ; converts
     * to JVM method descriptor form.
     *
     * @param param
     * @param result
     * @return
     */
    // Widely used internally, not much outside.
    public static String makeMethodDesc(String param, String result) {
        return "(" + param + ")" + result;
    }
//    public static String makeMethodDesc(List<String> params, String result) {
//        String desc ="(";
//        for (String param : params) {
//            desc += param;
//        }
//        desc += ")" + result;
//        return desc;
//    }

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
        specialForeignJavaTranslations.put(cl,
                NodeFactory.makeTraitType(span, false, NodeFactory.makeId(span, api, str))); /* api was commented out before... */
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
        s(Object.class, fortLib, "Any");
        s(String.class, fortLib, "String");
        s(BigInteger.class, fortLib, "ZZ");
        specialForeignJavaTranslations.put("V", NodeFactory.makeVoidType(span));
    }

    /**
     * Package prefix for runtime values
     */
    private static final String runtimeValues = com.sun.fortress.runtimeSystem.Naming.runtimeValues;

    public static final String FValueType = runtimeValues + "FValue";
    // static final String FValueDesc = internalToDesc(FValueType);

    static class PartialTypeComparator implements Comparator<com.sun.fortress.nodes.Type> {

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
        specialFortressDescriptors.put(t, internalToDesc(cl));
        specialFortressTypes.put(t, cl );
    }

    static {
        /*
         * This code is duplicated, mostly, in runtime Naming.java,
         * except that it deals only in strings.
         */
    //    bl(fortLib, "IntLiteral", "FIntLiteral");
        bl(fortLib, "Boolean", "FBoolean");
        bl(fortLib, "Char", "FChar");
        bl(fortLib, "RR32", "FRR32");
        bl(fortLib, "RR64", "FRR64");
        bl(fortLib, "ZZ32", "FZZ32");
        bl(fortLib, "ZZ64", "FZZ64");
        bl(fortLib, "String", "FString");
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
    public static String apiAndMethodToMethodOwner(APIName name, Function method) {
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
    public static String apiAndMethodToMethod(APIName name, Function method) {
        String m = method.toUndecoratedName().toString();
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
        mname += "$SINGLE";
        return mname;
    }

    private static int taskCount = 0;
    public static String gensymTaskName(String packageAndClassName) {
        return packageAndClassName + "$" + "task" + taskCount++;
    }
    private static int implementationCount = 0;
    public static String gensymArrowClassName(String desc) {
        return desc + "$" + "implementation" + implementationCount++;
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
        return jvmSignatureFor(domain, jvmTypeDesc(range, ifNone), ifNone);
    }

    //
    private static String jvmSignatureFor(com.sun.fortress.nodes.Type domain,
            String rangeDesc,
            APIName ifNone) {
        return makeMethodDesc(
                NodeUtil.isVoidType(domain) ? "" : jvmTypeDesc(domain, ifNone),
                        rangeDesc);
    }

    // CodeGen, several methods
    public static String jvmSignatureFor(List<com.sun.fortress.nodes.Param> domain,
            String rangeDesc, APIName ifNone) {
        String args = "";
        // This special case handles single void argument type properly.
        if (domain.size() == 1)
            return jvmSignatureFor(NodeUtil.optTypeOrPatternToType(domain.get(0).getIdType()).unwrap(), rangeDesc, ifNone);
        for (Param p : domain) {
            args += jvmTypeDesc(NodeUtil.optTypeOrPatternToType(p.getIdType()), ifNone);
        }
        return makeMethodDesc(args, rangeDesc);
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
            for (com.sun.fortress.nodes.Type p : tt.getElements()) {
                args += jvmTypeDesc(p, ifNone);
                i++;
                if (spliceAt == i)
                    args += jvmTypeDesc(spliceType, ifNone);
            }
        } else {
            if (spliceAt < 0)
                return jvmSignatureFor(domain, rangeDesc, ifNone);
            else {
                args += jvmTypeDesc(domain, ifNone);
                if (spliceAt == 1)
                    args += jvmTypeDesc(spliceType, ifNone);
            }
        }

        return makeMethodDesc(args, rangeDesc);
    }

    private static String jvmSignatureFor(List<com.sun.fortress.nodes.Param> domain,
            com.sun.fortress.nodes.Type range,
            APIName ifNone) {
        return jvmSignatureFor(domain, jvmTypeDesc(range, ifNone), ifNone);
    }

    // OverloadSet.jvmSignatureFor
    public static String jvmSignatureFor(Function f, APIName ifNone, TypeAnalyzer ta) {
        com.sun.fortress.nodes.Type range = f.getReturnType().unwrap();
        range = ta.groundBound(range);
        return jvmSignatureFor(f.parameters(), range, ifNone);
    }

    // OverloadSet.jvmSignatureFor
    public static String jvmSignatureFor(Function f, APIName ifNone) {
        com.sun.fortress.nodes.Type range = f.getReturnType().unwrap();
        return jvmSignatureFor(f.parameters(), range, ifNone);
    }

    // Codegen.dumpSigs
    public static String jvmSignatureFor(FnDecl f, APIName ifNone) {
        FnHeader h = f.getHeader();
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
        return jvmTypeDesc(type, ifNone, true);
    }

    // Local, and Codegen.generateHigherOrderCall
    public static String makeArrowDescriptor(ArrowType t, final APIName ifNone) {

        String res =
         "Arrow"+ Naming.LEFT_OXFORD + makeArrowDescriptor(t.getDomain(), ifNone) + ";" +
            makeArrowDescriptor(t.getRange(), ifNone) + Naming.RIGHT_OXFORD;
        return res;

    }

    private static String makeNestedArrowDescriptor(ArrowType t, final APIName ifNone) {
        return // Naming.NORMAL_TAG +
        makeArrowDescriptor(t,ifNone);
    }

    // forFnExpr
    public static String makeAbstractArrowDescriptor(
            List<com.sun.fortress.nodes.Param> params,
            com.sun.fortress.nodes.Type rt, APIName ifNone) {
        return makeAnArrowDescriptor(paramsToTypes(params), rt, ifNone, "AbstractArrow");
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
        if (params.size() > 0)
            for (com.sun.fortress.nodes.Type t : params) {
                result += makeArrowDescriptor(t, ifNone) + ";";
            }
        else {
            result = result + Naming.INTERNAL_SNOWMAN + ";";
        }

        result = result + makeArrowDescriptor(rt, ifNone) + Naming.RIGHT_OXFORD;
        return result;
    }

    private static List<com.sun.fortress.nodes.Type> paramsToTypes(
             List<com.sun.fortress.nodes.Param> params) {
        List<com.sun.fortress.nodes.Type> res = new ArrayList(params.size());
        for (com.sun.fortress.nodes.Param p : params) {
            res.add(NodeUtil.optTypeOrPatternToType(p.getIdType()).unwrap());
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

    // forFnExpr
    public static String makeArrowDescriptor(
            List<com.sun.fortress.nodes.Param> params,
            com.sun.fortress.nodes.Type rt, APIName ifNone) {
        return makeArrowDescriptor(ifNone, paramsToTypes(params), rt);
    }

    private static String makeArrowDescriptor(AnyType t, final APIName ifNone) {
        return "Object_";
    }

    private static String makeArrowDescriptor(TraitType t, final APIName ifNone) {
        Id id = t.getName();
        APIName apiName = id.getApiName().unwrap(ifNone);
        String tag = "";
        String sep = ".";
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

    private static String makeArrowDescriptor(VarType t, final APIName ifNone) {
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


    private static String makeArrowDescriptor(TupleType t, final APIName ifNone) {
        if ( NodeUtil.isVoidType(t) )
            return Naming.INTERNAL_TAG + Naming.SNOWMAN;
        if (t.getVarargs().isSome())
            throw new CompilerError(t,"Can't compile VarArgs yet");
        if (!t.getKeywords().isEmpty())
            throw new CompilerError(t,"Can't compile Keyword args yet");
        String res = "";
        for (com.sun.fortress.nodes.Type ty : t.getElements()) {
            res += makeArrowDescriptor(ty, ifNone) +  ';';
        }
        return Useful.substring(res, 0, -1);
    }

    public static String makeArrowDescriptor(com.sun.fortress.nodes.Type t, final APIName ifNone) {
        if (t instanceof TupleType) return makeArrowDescriptor((TupleType) t, ifNone);
        else if (t instanceof TraitSelfType) return makeArrowDescriptor(((TraitSelfType) t).getNamed(), ifNone);
        else if (t instanceof TraitType) return makeArrowDescriptor((TraitType) t, ifNone);
        else if (t instanceof AnyType) return makeArrowDescriptor((AnyType) t, ifNone);
        else if (t instanceof ArrowType) return makeNestedArrowDescriptor((ArrowType) t, ifNone);
        else if (t instanceof VarType) return makeArrowDescriptor((VarType) t, ifNone);
        else
            throw new CompilerError(t, " How did we get here? type = " +
                                     t + " of class " + t.getClass());
    }

    private static String jvmTypeDescs(List<com.sun.fortress.nodes.Type> types,
                                      final APIName ifNone) {
        String r = "";
        for (com.sun.fortress.nodes.Type t : types) {
            r += jvmTypeDesc(t, ifNone);
        }
        return r;
    }

    public static String jvmTypeDesc(final com.sun.fortress.nodes.Type type,
                                     final APIName ifNone,
                                     final boolean withLSemi) {
        return type.accept(new NodeAbstractVisitor<String>() {
            @Override
            public String defaultCase(Node x) {
                throw new CompilerError(x,"emitDesc of type "+x+" failed");
            }
            @Override
            public String forArrowType(ArrowType t) {
                String res = makeArrowDescriptor(t, ifNone);

                if (withLSemi) res = internalToDesc(res);
                return res;
            }
            @Override
            public String forTupleType(TupleType t) {
                if ( NodeUtil.isVoidType(t) )
                    return descFortressVoid;
                if (t.getVarargs().isSome())
                    throw new CompilerError(t,"Can't compile VarArgs yet");
                if (!t.getKeywords().isEmpty())
                    throw new CompilerError(t,"Can't compile Keyword args yet");
                return jvmTypeDescs(t.getElements(), ifNone);
            }
            @Override
            public String forAnyType (AnyType t) {
                return descFortressAny;
            }
            @Override
            public String forVarType (VarType t) {
                //
                String s = t.getName().getText();
                s = internalToDesc(s);
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
                String result = (withLSemi ? specialFortressDescriptors : specialFortressTypes).get(t);
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
                List<StaticParam> sparams = t.getStaticParams();
                List<StaticArg> sargs = t.getArgs();

                // TODO work in progress -- need to expand with StaticArg if those are available.
                if (sargs.size() > 0) {
                    result = makeInnerClassName(api,id, genericDecoration(sargs, ifNone));
                    if (sparams.size() > 0)
                        throw new CompilerError(id,"Static args and params both non-empty, what wins? " +  type);
                } else
                    result = makeInnerClassName(api,id, forStaticParams(sparams));

                if (withLSemi)
                    result = internalToDesc(result);
                Debug.debug(Debug.Type.CODEGEN, 1, "forTrait Type ", t, " = ", result);

                return result;
            }

            private String forStaticParams(List<StaticParam> sparams) {
                if (sparams.size() == 0)
                    return "";
                StringBuffer sparams_part = new StringBuffer();
                String pfx = Naming.LEFT_OXFORD;
                for (StaticParam p : sparams) {
                    sparams_part.append(pfx);
                    pfx = ";";
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
    // Codegen.reloveMethodAndSignature
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
                    return makeMethodDesc("", jvmTypeDesc(t.getRange(), ifNone));
                else return makeMethodDesc(jvmTypeDesc(t.getDomain(), ifNone),
                                           jvmTypeDesc(t.getRange(), ifNone));
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
                for (com.sun.fortress.nodes.Type ty : t.getElements()) {
                    res += jvmTypeDesc(ty, ifNone);
                }
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
                Option<APIName> maybeApi = id.getApiName();
                APIName api = id.getApiName().unwrap(ifNone);
                if (api == null) {
                    throw new CompilerError(id,"no api name given for id");
                }
                result = makeInnerClassName(api,id);
                result = internalToDesc(result);
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
            return new String(desc.substring(start, ++i));
        case 'L':
            start = i;
            while (ch != ';') {
                ch = desc.charAt(++i);
            }
            return new String(desc.substring(start, ++i));
        default: throw new CompilerError("Bad Type Descriptor:" + desc);
        }
    }

    // Codegen.taskConstructorDesc
    public static String
            jvmTypeDescForGeneratedTaskInit(List<com.sun.fortress.nodes.Type> fvtypes,
                                            APIName ifNone) {
        return "(" + jvmTypeDescs(fvtypes, ifNone) + ")V";
    }

    /** Type name for class containing singleton binding of toplevel
     * entity, or declaration of toplevel type.
     *
     * TODO: call this from code above when referencing types by name.
     */
    // Called from CodeGen
    public static String jvmClassForToplevelDecl(IdOrOp x, String api) {
        Option<APIName> actualApiOpt = x.getApiName();
        if (actualApiOpt.isSome()) {
            api = javaPackageClassForApi(actualApiOpt.unwrap());
        }
        return makeInnerClassName(api, x.getText());
    }

    public static String jvmClassForToplevelTypeDecl(IdOrOp x, String sparams_part, String api) {
        Option<APIName> actualApiOpt = x.getApiName();
        if (actualApiOpt.isSome()) {
            api = javaPackageClassForApi(actualApiOpt.unwrap());
        }
        return makeInnerClassName(api, x.getText()+sparams_part);
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

    public static NodeAbstractVisitor<String> spkTagger(final APIName ifMissing) { return new NodeAbstractVisitor<String> () {

        @Override
        public String forKindBool(KindBool that) {
            return Naming.BALLOT_BOX_WITH_CHECK;
        }

        @Override
        public String forKindDim(KindDim that) {
            return Naming.SCALES;
        }

        @Override
        public String forKindInt(KindInt that) {
            return Naming.MUSIC_SHARP;
        }

        @Override
        public String forKindNat(KindNat that) {
            // nats and ints go with same encoding; no distinction in args
            return Naming.MUSIC_SHARP;
        }

        @Override
        public String forKindOp(KindOp that) {
            return Naming.HAMMER_AND_PICK;
        }

        @Override
        public String forKindType(KindType that) {
            return Naming.YINYANG;
        }

        @Override
        public String forKindUnit(KindUnit that) {
            return Naming.ATOM;
        }

        @Override
        public String forBoolBase(BoolBase b) {
            return b.isBoolVal() ? "T" : "F";
        }

        @Override
        public String forBoolRef(BoolRef b) {
            return b.getName().getText();
        }

        @Override
        public String forBoolBinaryOp(BoolBinaryOp b) {
            BoolExpr l = b.getLeft();
            BoolExpr r = b.getRight();
            Op op = b.getOp();
            return l.accept(this) + Naming.ENTER + r.accept(this) + Naming.ENTER + op.getText();
        }

        @Override
        public String forBoolUnaryOp(BoolUnaryOp b) {
            BoolExpr v = b.getBoolVal();
            Op op = b.getOp();
            return v.accept(this) + Naming.ENTER + op.getText();
        }

        /* These need to return encodings of Fortress types. */
        @Override
        public String forBoolArg(BoolArg that) {
            BoolExpr arg = that.getBoolArg();

            return Naming.BALLOT_BOX_WITH_CHECK + arg.accept(this);
        }

        @Override
        public String forDimArg(DimArg that) {
            DimExpr arg = that.getDimArg();
            return Naming.SCALES;
        }

        @Override
        public String forIntBase(IntBase b) {
            return String.valueOf(b.getIntVal());
        }

        @Override
        public String forIntRef(IntRef b) {
            return b.getName().getText();
        }

        @Override
        public String forIntBinaryOp(IntBinaryOp b) {
            IntExpr l = b.getLeft();
            IntExpr r = b.getRight();
            Op op = b.getOp();
            return l.accept(this) + Naming.ENTER + r.accept(this) + Naming.ENTER + op.getText();
        }

       @Override
        public String forIntArg(IntArg that) {
            IntExpr arg = that.getIntVal();
            return Naming.MUSIC_SHARP + arg.accept(this);
        }

        @Override
        public String forOpArg(OpArg that) {
            FunctionalRef arg = that.getName();
            // TODO what about static args here?
            IdOrOp name = arg.getNames().get(0);
            return Naming.HAMMER_AND_PICK + name.getText();
        }

        @Override
        public String forTypeArg(TypeArg that) {
            com.sun.fortress.nodes.Type arg = that.getTypeArg();
            // Pretagged with type information
            String s =  makeArrowDescriptor(arg, ifMissing);
            return s;
        }

        @Override
        public String forUnitArg(UnitArg that) {
            UnitExpr arg = that.getUnitArg();
            return Naming.ATOM;
        }

    };
    }

    /**
     * @param xlation
     * @param sparams
     * @return
     */
    public static String genericDecoration(List<StaticParam> sparams,
            Map<String, String> xlation,
            List<String> splist,
            APIName ifMissing
            ) {
        if (sparams.size() == 0)
            return "";

        NodeAbstractVisitor<String> spkTagger = spkTagger(ifMissing);

        String frag = Naming.LEFT_OXFORD;
        int index = 1;
        for (StaticParam sp : sparams) {
            StaticParamKind spk = sp.getKind();

            IdOrOp spn = sp.getName();
            String tag = spk.accept(spkTagger) + index;
            if (xlation != null)
                xlation.put(spn.getText(), tag);
            if (splist != null)
                splist.add(spn.getText());
            frag += spn.getText() + ";";
            index++;
        }
        // TODO Auto-generated method stub
        return Useful.substring(frag, 0, -1) + Naming.RIGHT_OXFORD;
    }

    public static String genericDecoration(List<StaticArg> sargs,
            APIName ifMissing) {
        // TODO we need to make the conventions for Arrows and other static types converge.
        if (sargs.size() == 0)
            return "";

        NodeAbstractVisitor<String> spkTagger = spkTagger(ifMissing);

        String frag = Naming.LEFT_OXFORD;
        int index = 1;
        for (StaticArg sp : sargs) {
            String tag = sp.accept(spkTagger);
            frag += tag;
            frag += ";";
            index++;
        }
        return Useful.substring(frag,0,-1) + Naming.RIGHT_OXFORD;
    }
}
