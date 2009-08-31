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

package com.sun.fortress.compiler;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.fortress.compiler.environments.TopLevelEnvGen;
import com.sun.fortress.compiler.index.Function;
import com.sun.fortress.compiler.optimization.Unbox.Contains;
import com.sun.fortress.compiler.phases.OverloadSet;
import com.sun.fortress.exceptions.CompilerError;
import com.sun.fortress.nodes.ASTNode;
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
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.TraitType;
import com.sun.fortress.nodes.TraitTypeWhere;
import com.sun.fortress.nodes.TupleType;
import com.sun.fortress.nodes.VarType;
import com.sun.fortress.nodes.NodeAbstractVisitor;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.repository.ForeignJava;
import com.sun.fortress.repository.GraphRepository;
import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.runtimeSystem.Naming;
import com.sun.fortress.useful.BATree;
import com.sun.fortress.useful.Debug;
import com.sun.fortress.useful.Useful;

import edu.rice.cs.plt.tuple.Option;

import org.objectweb.asm.Type;

import static com.sun.fortress.exceptions.InterpreterBug.bug;
import static com.sun.fortress.exceptions.ProgramError.error;
import static com.sun.fortress.exceptions.ProgramError.errorMsg;

public class NamingCzar {
    public static final NamingCzar only = new NamingCzar(ForeignJava.only);

    private final ForeignJava fj;

    private NamingCzar(ForeignJava fj) {
        this.fj = fj;
    }
    
    private final static boolean logLoads = ProjectProperties.getBoolean("fortress.log.classloads", false);

    private final static boolean transitionArrowNaming = false;
    
    public static final String COERCION_NAME = "coerce";
    public static final Id SELF_NAME = NodeFactory.makeId(NodeFactory.internalSpan, "self");

    public static final String springBoard = "$DefaultTraitMethods";
    public static final String make = "make";

    public static final String cache = ProjectProperties.BYTECODE_CACHE_DIR + "/";

    //Asm requires you to call visitMaxs for every method
    // but ignores the arguments.
    public static final int ignore = 1;

    // fortress types
    public static final String fortressPackage = "fortress";
    public static final String fortressAny = fortressPackage + "/" +
                                              WellKnownNames.anyTypeLibrary() +
                                             "$" + WellKnownNames.anyTypeName;

    // java.lang.Object correctly formatted for asm generation
    public static final String javaObject = "java/lang/Object";

    // Base class for all executable Fortress Components
    public static final String fortressExecutable = "com/sun/fortress/runtimeSystem/FortressExecutable";
    public static final String fortressExecutableRun = "runExecutable";
    public static final String fortressExecutableRunType = "([Ljava/lang/String;)V";

    // Base class for tasks
    public static final String fortressBaseTask = "com/sun/fortress/runtimeSystem/BaseTask";

    // Base class for non-executable Fortress Components
    public static final String fortressComponent = javaObject;

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

    public static final String internalFortressZZ32  = makeFortressInternal("ZZ32");
    public static final String internalFortressZZ64  = makeFortressInternal("ZZ64");
    public static final String internalFortressRR32  = makeFortressInternal("RR32");
    public static final String internalFortressRR64  = makeFortressInternal("RR64");
    public static final String internalFortressBoolean  = makeFortressInternal("Boolean");
    public static final String internalFortressChar  = makeFortressInternal("Char");
    public static final String internalFortressString = makeFortressInternal("String");
    public static final String internalFortressVoid   = makeFortressInternal("Void");

    // fortress interpreter types: type descriptors
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
    public static com.sun.fortress.nodes.Type fortressTypeForForeignJavaType(String s) {
        return specialForeignJavaTranslations.get(s);
    }

    // Translate among Java type names
    // (Section 2.1.3 in ASM 3.0: A Java bytecode engineering library)

    public static String internalToDesc(String type) {
        return "L" + type + ";";
    }
    public static String makeMethodDesc(String param, String result) {
        return "(" + param + ")" + result;
    }
    public static String makeMethodDesc(List<String> params, String result) {
        String desc ="(";
        for (String param : params) {
            desc += param;
        }
        desc += ")" + result;
        return desc;
    }

    public static String makeArrayDesc(String element) {
        return "[" + element;
    }

    // fortress runtime types: internal names
    public static String makeFortressInternal(String type) {
        return "com/sun/fortress/compiler/runtimeValues/F" + type;
        // return "fortress/CompilerBuiltin/" + type;
    }


    static Map<String, com.sun.fortress.nodes.Type> specialForeignJavaTranslations = new HashMap<String, com.sun.fortress.nodes.Type>();

    /* Minor hackery here -- because we know these types are already loaded
     * and not eligible for ASM-wrapping, we just go ahead and refer to the
     * loaded class.
     */
    static void s(Class cl, APIName api, String str) {
        s(Type.getType(cl), api, str);
    }

    static void s(Type cl, APIName api, String str) {
        s(cl.getDescriptor(), api, str);
    }

    static void s(String cl, APIName api, String str) {
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

    static final String runtimeValues = com.sun.fortress.runtimeSystem.Naming.runtimeValues;

    public static final String FValueType = runtimeValues + "FValue";
    static final String FValueDesc = "L" + FValueType + ";";

    /**
     * Java descriptors for (boxed) Fortress types, INCLUDING leading L and trailing ;
     */
    static Map<com.sun.fortress.nodes.Type, String> specialFortressDescriptors = new HashMap<com.sun.fortress.nodes.Type, String>();
    /**
     * Java descriptors for (boxed) Fortress types, WITHOUT leading L and trailing ;
     */
    static Map<com.sun.fortress.nodes.Type, String> specialFortressTypes = new HashMap<com.sun.fortress.nodes.Type, String>();

    static void bl(APIName api, String str, String cl) {
        b(api,str, runtimeValues+cl);
    }

    static void bl(com.sun.fortress.nodes.Type t, String cl) {
        b(t, runtimeValues+cl);
    }

    static void b(APIName api, String str, String cl) {
        b(NodeFactory.makeTraitType(span, false, NodeFactory.makeId(span, api, str)), cl);
        b(NodeFactory.makeTraitType(span, false, NodeFactory.makeId(span, /* api, */ str)), cl);
    }

    static void b(com.sun.fortress.nodes.Type t, String cl) {
        specialFortressDescriptors.put(t, "L" + cl + ";");
        specialFortressTypes.put(t, cl );
    }

    static {
        /*
         * This code is duplicated, mostly, in runtime Naming.java,
         * except that it deals only in strings.
         */
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
    public static String boxedImplDesc(com.sun.fortress.nodes.Type t, APIName ifNone) {
        return jvmTypeDesc(t, ifNone);
    }


    public static String boxedImplType( com.sun.fortress.nodes.Type t, APIName ifNone ) {
        return jvmTypeDesc(t, ifNone, false);
    }

    public String apiNameToPackageName(APIName name) {
        if (fj.definesApi(name)) {
            return Naming.NATIVE_PREFIX_DOT + name.getText();
        } else {
            return javaPackageClassForApi(name.getText(), ".").toString();
        }
    }

    public String apiAndMethodToMethodOwner(APIName name, Function method) {
        String p;
        String m = method.toUndecoratedName().toString();
        if (fj.definesApi(name)) {
             p = Naming.NATIVE_PREFIX_DOT + name.getText();
             int idot = m.lastIndexOf(".");
             if (idot != -1) {
                 p = p + "/" + m.substring(0,idot);
             }

        } else {
             p = javaPackageClassForApi(name.getText(), ".").toString();
        }
        p = Useful.replace(p, ".", "/") ;
        return p;
    }

    public String apiAndMethodToMethod(APIName name, Function method) {
        String m = method.toUndecoratedName().toString();
        return apiAndMethodToMethod(name, m);
    }

    /**
     * @param name
     * @param m
     * @return
     */
    public String apiAndMethodToMethod(APIName name, String m) {
        if (fj.definesApi(name)) {
            int idot = m.lastIndexOf(".");
            if (idot != -1) {
                m = m.substring(idot+1);
            }
        }
        return m;
    }

    /**
     * @param componentName
     * @return the name of the class implementing the compiled top-level
     *         environment for component componentName.
     */
    public static String classNameForComponentEnvironment(APIName componentName) {
        return classNameForComponentEnvironment(NodeUtil.nameString(componentName));
    }

    /**
     * @param componentName
     * @return the name of the class implementing the compiled top-level
     *         environment for component componentName.
     */
    public static String classNameForComponentEnvironment(String componentName) {
        componentName = componentName + TopLevelEnvGen.COMPONENT_ENV_SUFFIX;
        componentName = mangleClassIdentifier(componentName);  // Need to mangle the name if it contains "."
        return componentName;
    }

    /**
     *
     * @param apiName
     * @return the name of the class implementing the compiled top-level
     *         environment for api apiName
     */
    public static String classNameForApiEnvironment(APIName apiName) {
        return classNameForApiEnvironment(NodeUtil.nameString(apiName));
    }

    /**
     *
     * @param apiName
     * @return the name of the class implementing the compiled top-level
     *         environment for apiName
     */
    public static String classNameForApiEnvironment(String apiName) {
        apiName = apiName + TopLevelEnvGen.API_ENV_SUFFIX;
        apiName = mangleClassIdentifier(apiName);  // Need to mangle the name if it contains "."
        return apiName;
    }

    public static String mangleClassIdentifier(String identifier) {
        // Is this adequate, given naming freedom?
        String mangledString = identifier.replaceAll("\\.", "\\$");
        return mangledString+deCase(mangledString);
    }

    /**
     * @param extendsC
     * @return The names of the Java interfaces providing the mentioned types;
     *         if the extends clause is empty, fills in Object as required.
     */
    public static String [] extendsClauseToInterfaces(List<TraitTypeWhere> extendsC, APIName ifMissing) {
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
                throw new CompilerError(NodeUtil.getSpan(parentType),
                              errorMsg("Invalid type ",parentType," in extends clause."));
            }
            Id name = ((TraitType)parentType).getName();
            Option<APIName> apiName = name.getApiName();
            String api = apiName.unwrap(ifMissing).getText();

            StringBuilder parent = javaPackageClassForApi(api, "/");  parent.append("$");  parent.append(name.getText());
            result[i] = parent.toString();
        }
        return result;
    }

    /**
     * @param api
     * @return
     */
    public static StringBuilder javaPackageClassForApi(String api, String sep) {
        StringBuilder parent = new StringBuilder();
        if ( WellKnownNames.exportsDefaultLibrary( api ) ) {
            parent.append(fortressPackage);  parent.append(sep);
        }
        if (!(sep.equals("."))) {
            api = Useful.replace(api, ".", sep);
        }
        parent.append(api);
        return parent;
    }

 
    public String mangleAwayFromOverload(String mname) {
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

    public static String makeInnerClassName(Id id) {
        return makeInnerClassName(jvmClassForSymbol(id), id.getText());
    }

    public static String makeInnerClassName(String packageAndClassName, String t) {
        return packageAndClassName + "$" + t;
    }

    public static String jvmSignatureFor(com.sun.fortress.nodes.Type domain,
                                         com.sun.fortress.nodes.Type range,
                                         APIName ifNone) {
        return jvmSignatureFor(domain, jvmTypeDesc(range, ifNone), ifNone);
    }

    public static String jvmSignatureFor(com.sun.fortress.nodes.Type domain,
            String rangeDesc,
            APIName ifNone) {
        return makeMethodDesc(
                NodeUtil.isVoidType(domain) ? "" : jvmTypeDesc(domain, ifNone),
                        rangeDesc);
    }

    public static String jvmSignatureFor(List<com.sun.fortress.nodes.Param> domain,
            String rangeDesc, APIName ifNone) {
        String args = "";
        // This special case handles single void argument type properly.
        if (domain.size() == 1)
            return jvmSignatureFor(domain.get(0).getIdType().unwrap(), rangeDesc, ifNone);
        for (Param p : domain) {
            args += jvmTypeDesc(p.getIdType(), ifNone);
        }
        return makeMethodDesc(args, rangeDesc);
    }

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

    public static String jvmSignatureFor(List<com.sun.fortress.nodes.Param> domain,
            com.sun.fortress.nodes.Type range,
            APIName ifNone) {
        return jvmSignatureFor(domain, jvmTypeDesc(range, ifNone), ifNone);
    }

    public static String jvmSignatureFor(Function f, APIName ifNone) {
        return jvmSignatureFor(f.parameters(), f.getReturnType().unwrap(), ifNone);
    }

    public static String jvmSignatureFor(FnDecl f, APIName ifNone) {
        FnHeader h = f.getHeader();
        return jvmSignatureFor(h.getParams(), h.getReturnType().unwrap(), ifNone);
    }

    public static String jvmClassForSymbol(IdOrOp fnName) {
        Option<APIName> maybe_api = fnName.getApiName();
        String result = "";
        if (maybe_api.isSome()) {
            APIName apiName = maybe_api.unwrap();
            if (WellKnownNames.exportsDefaultLibrary(apiName.getText()))
                result = result + "fortress/";
            result = result + apiName.getText();
        }
        //        result = result + fnName.getText();

        Debug.debug(Debug.Type.CODEGEN, 1,
                    "jvmClassForSymbol(" + fnName +")=" + result);
        return result;
    }

    public static String jvmTypeDesc(com.sun.fortress.nodes.Type type,
            final APIName ifNone) {
        return jvmTypeDesc(type, ifNone, true);
    }

    public static String applyMethodName() { return "apply";}

    public static String makeArrowDescriptor(ArrowType t, final APIName ifNone) {
        if (transitionArrowNaming) {
            return "com/sun/fortress/compiler/runtimeValues/AbstractArrow_" + 
                makeArrowDescriptor(t.getDomain(), ifNone) + "_" + 
                makeArrowDescriptor(t.getRange(), ifNone);
        } else {
            String res = 
             "Arrow"+ Naming.LEFT_OXFORD + makeArrowDescriptor(t.getDomain(), ifNone) + ";" +
                makeArrowDescriptor(t.getRange(), ifNone) + Naming.RIGHT_OXFORD;
            res = Naming.mangleIdentifier(res);
            return res;
        }
    }

    public static String makeAbstractArrowDescriptor(List<com.sun.fortress.nodes.Param> params, 
                                             com.sun.fortress.nodes.Type rt,
                                             APIName ifNone) {
        String result = "AbstractArrow" + Naming.LEFT_OXFORD;
        for (Param p : params) {
            result = result + makeArrowDescriptor(p.getIdType().unwrap(), ifNone) + ";";
        }

        result = result + makeArrowDescriptor(rt, ifNone) + Naming.RIGHT_OXFORD;
        result = Naming.mangleIdentifier(result);
        return result;
    }

    public static String makeArrowDescriptor(AnyType t, final APIName ifNone) {
        return "Object_";
    }

    public static String makeArrowDescriptor(TraitType t, final APIName ifNone) {
        Id id = t.getName();
        APIName apiName = id.getApiName().unwrap(ifNone);
        String tag = "";
        if (transitionArrowNaming)
            return id.getText();
        else {
            if (WellKnownNames.exportsDefaultLibrary(apiName.getText())) {
                tag = Naming.INTERNAL_TAG; // warning sign -- internal use only
            } else if (only.fj.definesApi(apiName)) {
                tag = Naming.FOREIGN_TAG; // hot beverage == JAVA
            } else {
                tag = Naming.NORMAL_TAG; // smiley face == normal case.
            }
            
            return tag + apiName + "." + id.getText();
        }
    }

    public static String makeArrowDescriptor(TupleType t, final APIName ifNone) {
        if ( NodeUtil.isVoidType(t) )
            return Naming.INTERNAL_TAG + Naming.SNOWMAN;
        if (t.getVarargs().isSome())
            throw new CompilerError(NodeUtil.getSpan(t),
                                    "Can't compile VarArgs yet");
        if (!t.getKeywords().isEmpty())
            throw new CompilerError(NodeUtil.getSpan(t),
                                    "Can't compile Keyword args yet");
        String res = "";
        for (com.sun.fortress.nodes.Type ty : t.getElements()) {
            res += makeArrowDescriptor(ty, ifNone) + (transitionArrowNaming ? "_" : ';');
        }
        return res;
    }

    public static String makeArrowDescriptor(com.sun.fortress.nodes.Type t, final APIName ifNone) {
        if (t instanceof TupleType) return makeArrowDescriptor((TupleType) t, ifNone);
        else if (t instanceof TraitType) return makeArrowDescriptor((TraitType) t, ifNone);
        else if (t instanceof AnyType) return makeArrowDescriptor((AnyType) t, ifNone);
        else if (t instanceof ArrowType) return makeArrowDescriptor((ArrowType) t, ifNone);
        else throw new CompilerError(NodeUtil.getSpan(t), " How did we get here? type = " +
                                     t + " of class " + t.getClass());
    }

    public static String jvmTypeDescs(List<com.sun.fortress.nodes.Type> types,
                                      final APIName ifNone) {
        String r = "";
        for (com.sun.fortress.nodes.Type t : types) {
            r += jvmTypeDesc(t, ifNone);
        }
        return r;
    }

    public static String jvmTypeDesc(com.sun.fortress.nodes.Type type,
                                     final APIName ifNone,
                                     final boolean withLSemi) {
        return type.accept(new NodeAbstractVisitor<String>() {
            public void defaultCase(ASTNode x) {
                throw new CompilerError(NodeUtil.getSpan(x),
                                        "emitDesc of type failed");
            }
            public String forArrowType(ArrowType t) {
                String res = makeArrowDescriptor(t, ifNone);
                
                if (withLSemi) res = "L" + res + ";";
                return res;
            }

            public String forTupleType(TupleType t) {
                if ( NodeUtil.isVoidType(t) )
                    return descFortressVoid;
                if (t.getVarargs().isSome())
                    throw new CompilerError(NodeUtil.getSpan(t),
                                            "Can't compile VarArgs yet");
                if (!t.getKeywords().isEmpty())
                    throw new CompilerError(NodeUtil.getSpan(t),
                                            "Can't compile Keyword args yet");
                return jvmTypeDescs(t.getElements(), ifNone);
            }
            public String forAnyType (AnyType t) {
                return descFortressAny;
            }
            public String forTraitType(TraitType t) {
                // I think this is wrong!  What about API names?
                // What about foreign-implemented types?
                // - DRC 2009-08-10
                Id id = t.getName();
                String name = id.getText();
                String result = (withLSemi ? specialFortressDescriptors : specialFortressTypes).get(t);
                if (result != null) {
                    Debug.debug(Debug.Type.CODEGEN, 1, "forTrait Type ", t ,
                                " builtin ", result);
                    return result;
                }
                Option<APIName> maybeApi = id.getApiName();
                if (ifNone == null && !maybeApi.isSome()) {
                    throw new CompilerError(NodeUtil.getSpan(id),
                                            "no api name given for id");
                }
                APIName api = maybeApi.unwrap(ifNone);
                result = makeInnerClassName(api.getText(), name) ;
                if (withLSemi)
                    result = "L" + result + ";";
                Debug.debug(Debug.Type.CODEGEN, 1, "forTrait Type ", t, " = ", result);

                return result;
            }
            });
    }



    /* Clone of above, to clean things out, TYPE DESCRIPTORS != METHOD DESCRIPTORS */

    public static String jvmMethodDesc(com.sun.fortress.nodes.Type type,
            final APIName ifNone)  {
        return type.accept(new NodeAbstractVisitor<String>() {
            public void defaultCase(ASTNode x) {
                throw new CompilerError(NodeUtil.getSpan(x),
                                        "emitDesc of type failed");
            }

            public String forArrowType(ArrowType t) {
                if (NodeUtil.isVoidType(t.getDomain()))
                    return makeMethodDesc("", jvmTypeDesc(t.getRange(), ifNone));
                else return makeMethodDesc(jvmTypeDesc(t.getDomain(), ifNone),
                                           jvmTypeDesc(t.getRange(), ifNone));
            }

            // TODO CASES BELOW OUGHT TO JUST FAIL, WILL TEST SOON.
            public String forTupleType(TupleType t) {
                if ( NodeUtil.isVoidType(t) )
                    return descFortressVoid;
                if (t.getVarargs().isSome())
                    throw new CompilerError(NodeUtil.getSpan(t),
                                            "Can't compile VarArgs yet");
                if (!t.getKeywords().isEmpty())
                    throw new CompilerError(NodeUtil.getSpan(t),
                                            "Can't compile Keyword args yet");
                String res = "";
                for (com.sun.fortress.nodes.Type ty : t.getElements()) {
                    res += jvmTypeDesc(ty, ifNone);
                }
                return res;
            }
            public String forAnyType (AnyType t) {
                return descFortressAny;
            }
            public String forTraitType(TraitType t) {
                Id id = t.getName();
                String name = id.getText();
                String result = specialFortressDescriptors .get(t);
                if (result != null) {
                    Debug.debug(Debug.Type.CODEGEN, 1, "forTrait Type ", t ,
                                " builtin ", result);
                    return result;
                }
                Option<APIName> maybeApi = id.getApiName();
                if (ifNone == null && !maybeApi.isSome()) {
                    throw new CompilerError(NodeUtil.getSpan(id),
                                            "no api name given for id");
                }
                APIName api = maybeApi.unwrap(ifNone);
                result = makeInnerClassName(api.getText(), name) ;
                    result = "L" + result + ";";
                Debug.debug(Debug.Type.CODEGEN, 1, "forTrait Type ", t, " = ", result);

                return result;
            }
            });
    }

    public static String jvmTypeDesc(Option<com.sun.fortress.nodes.Type> otype, APIName ifNone) {
        if (!otype.isSome()) {
            throw new CompilerError("Expected type information was absent.");
        }
        return jvmTypeDesc(otype.unwrap(), ifNone);
    }

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

    public static String
            jvmTypeDescForGeneratedTaskInit(List<com.sun.fortress.nodes.Type> fvtypes,
                                            APIName ifNone) {
        return "(" + jvmTypeDescs(fvtypes, ifNone) + ")V";
    }


}
