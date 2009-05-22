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

import java.lang.StringBuffer;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.fortress.compiler.environments.TopLevelEnvGen;
import com.sun.fortress.exceptions.CompilerError;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.AnyType;
import com.sun.fortress.nodes.ArrowType;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.BottomType;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.NamedType;
import com.sun.fortress.nodes.TraitType;
import com.sun.fortress.nodes.TraitTypeWhere;
import com.sun.fortress.nodes.VarType;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.repository.ForeignJava;
import com.sun.fortress.repository.GraphRepository;
import com.sun.fortress.repository.ProjectProperties;

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

    public static final String springBoard = "$SpringBoard";
    public static final String make = "make";

    public static final String cache = ProjectProperties.BYTECODE_CACHE_DIR + "/";

    //Asm requires you to call visitMaxs for every method
    // but ignores the arguments.
    public static final int ignore = 1;

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

    public static final String voidToFortressVoid = makeMethodDesc("", descFortressVoid);


    // fortress types
    public static final String fortressPackage = "fortress";
    public static final String fortressAny = fortressPackage + "/" + WellKnownNames.anyTypeLibrary() +
                                             "$" + WellKnownNames.anyTypeName;

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
            desc = desc + param;
        }
        desc = desc + "(" + result;
        return desc;
    }
    public static String makeArrayDesc(String element) {
        return "[" + element;
    }

    // fortress runtime types: internal names
    public static String makeFortressInternal(String type) {
        return "com/sun/fortress/compiler/runtimeValues/F" + type;
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

    static final String runtimeValues = "Lcom/sun/fortress/compiler/runtimeValues/";


    /**
     * Given a Fortress type (expressed as AST node for a Type),
     * what is the descriptor ("Ljava/lang/Object;", V, I, [J, etc)
     * of the type implementing it in boxed form?
     *
     * @param t
     * @return
     */
    static String javaDescriptorImplementingFortressType(com.sun.fortress.nodes.Type t) {
        return specialFortressTypes.get(t);
    }

    static Map<com.sun.fortress.nodes.Type, String> specialFortressTypes = new HashMap<com.sun.fortress.nodes.Type, String>();

    static void bl(APIName api, String str, String cl) {
        b(api,str, runtimeValues+cl+";");
    }

    static void bl(com.sun.fortress.nodes.Type t, String cl) {
        b(t, runtimeValues+cl+";");
    }

    static void b(APIName api, String str, String cl) {
        b(NodeFactory.makeTraitType(span, false, NodeFactory.makeId(span, api, str)), cl);
        b(NodeFactory.makeTraitType(span, false, NodeFactory.makeId(span, /* api, */ str)), cl);
    }

    static void b(com.sun.fortress.nodes.Type t, String cl) {
        specialFortressTypes.put(t, cl);
    }

    static {
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
    public String boxedImplDesc(com.sun.fortress.nodes.Type t) {
        String desc = javaDescriptorImplementingFortressType(t);

        if (desc != null)
            return desc;

        if (t instanceof ArrowType) {

        } else if (t instanceof BaseType) {
            if (t instanceof AnyType) {
                return runtimeValues + "FValue;";
            } else if (t instanceof BottomType) {
                return bug("Not sure how bottom type translates into Java");
            } else if (t instanceof NamedType) {
                if (t instanceof TraitType) {
                    return runtimeValues + "FValue;";
                } else if (t instanceof VarType) {
                    return bug("Need a binding to translate a VarType into Java");
                }
            }
        }
        return bug ("unhandled type translation, Fortress type " + t);

    }

    public String apiNameToPackageName(APIName name) {
        if (fj.definesApi(name)) {
            return name.getText();
        } else {
            return "fortress."+name.getText();
        }
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
        String mangledString = identifier.replaceAll("\\.", "\\$");
        return mangledString+deCase(mangledString);
    }

    /**
     * @param extendsC
     * @return The names of the Java interfaces providing the mentioned types;
     *         if the extends clause is empty, fills in Object as required.
     */
    public static String [] extendsClauseToInterfaces(List<TraitTypeWhere> extendsC) {
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
            if (apiName.isNone()) {
                result[i] = name.toString();
                continue;
            }
            StringBuilder parent = new StringBuilder();
            String api = apiName.unwrap().getText();
            if ( WellKnownNames.exportsDefaultLibrary( api ) ) {
                parent.append(fortressPackage);  parent.append("/");
            }
            parent.append(api);  parent.append("$");  parent.append(name.getText());
            result[i] = parent.toString();
        }
        return result;
    }

    /**
     * Convert a string identifier into something that will be legal in a
     * JVM.
     *
     * http://blogs.sun.com/jrose/entry/symbolic_freedom_in_the_vm
     * Dangerous characters are the union of all characters forbidden
     * or otherwise restricted by the JVM specification, plus their mates,
     * if they are brackets.

     * @param identifier
     * @return
     */
    public static String mangleIdentifier(String identifier) {

        // 1. In each accidental escape, replace the backslash with an escape sequence (\-)
        String mangledString = identifier.replaceAll("\\\\", "\\\\-");

        // 2. Replace each dangerous character with an escape sequence (\| for /, etc.)
        mangledString = mangledString.replaceAll("/", "\\\\|");
        mangledString = mangledString.replaceAll("\\.", "\\\\,");
        mangledString = mangledString.replaceAll(";", "\\\\?");
        mangledString = mangledString.replaceAll("\\$", "\\\\%");
        mangledString = mangledString.replaceAll("<", "\\\\^");
        mangledString = mangledString.replaceAll(">", "\\\\_");
        mangledString = mangledString.replaceAll("\\[", "\\\\{");
        mangledString = mangledString.replaceAll("\\]", "\\\\}");
        mangledString = mangledString.replaceAll(":", "\\\\!");

        // Non-standard name-mangling convention.  Michael Spiegel 6/16/2008
        mangledString = mangledString.replaceAll("\\ ", "\\\\~");

        // 3. If the first two steps introduced any change, <em>and</em> if the
        // string does not already begin with a backslash, prepend a null prefix (\=)
        if (!mangledString.equals(identifier) && !(mangledString.charAt(0) == '\\')) {
            mangledString = "\\=" + mangledString;
        }
        return mangledString;
    }

}
