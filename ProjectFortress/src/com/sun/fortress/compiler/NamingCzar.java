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
import java.util.HashMap;
import java.util.Map;

import com.sun.fortress.compiler.environments.TopLevelEnvGen;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.AnyType;
import com.sun.fortress.nodes.ArrowType;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.BottomType;
import com.sun.fortress.nodes.NamedType;
import com.sun.fortress.nodes.TraitType;
import com.sun.fortress.nodes.VarType;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.repository.ForeignJava;
import com.sun.fortress.repository.GraphRepository;
import com.sun.fortress.repository.ProjectProperties;

import org.objectweb.asm.Type;

import static com.sun.fortress.exceptions.InterpreterBug.bug;
import static com.sun.fortress.exceptions.ProgramError.error;
import static com.sun.fortress.exceptions.ProgramError.errorMsg;

public class NamingCzar {
    public final static NamingCzar only = new NamingCzar(ForeignJava.only);

    private final ForeignJava fj;

    private NamingCzar(ForeignJava fj) {
        this.fj = fj;
    }

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

        2. L/com/sun/fortress/interpreter/evaluator/values/FInt
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
         NodeFactory.makeAPIName(span, "FortressLibrary");

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
         specialForeignJavaTranslations.put(cl, NodeFactory.makeTraitType(span, false, NodeFactory.makeId(span, str)));
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

     static final String interpreterValues = "Lcom/sun/fortress/interpreter/evaluator/values/";

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
         b(api,str, interpreterValues+cl+";");
     }

     static void bl(com.sun.fortress.nodes.Type t, String cl) {
         b(t, interpreterValues+cl+";");
     }

     static void b(APIName api, String str, String cl) {
         b(NodeFactory.makeTraitType(span, false, NodeFactory.makeId(span, str)), cl);
     }

     static void b(com.sun.fortress.nodes.Type t, String cl) {
         specialFortressTypes.put(t, cl);
     }

     static {
         bl(fortLib, "Boolean", "FBool");
         bl(fortLib, "Char", "FChar");
         bl(fortLib, "RR32", "FFloat");
         bl(fortLib, "ZZ32", "FInt");
         bl(fortLib, "ZZ64", "FLong");
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
                 return interpreterValues + "FValue";
             } else if (t instanceof BottomType) {
                 return bug("Not sure how bottom type translates into Java");
             } else if (t instanceof NamedType) {
                 if (t instanceof TraitType) {
                     return interpreterValues + "FValue";
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
