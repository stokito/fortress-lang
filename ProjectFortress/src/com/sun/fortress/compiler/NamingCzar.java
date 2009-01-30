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

import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.AnyType;
import com.sun.fortress.nodes.ArrowType;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.BottomType;
import com.sun.fortress.nodes.NamedType;
import com.sun.fortress.nodes.TraitType;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.VarType;
import com.sun.fortress.repository.ForeignJava;
import com.sun.fortress.repository.GraphRepository;
import com.sun.fortress.repository.ProjectProperties;

public class NamingCzar {
    public final static NamingCzar only = new NamingCzar(ForeignJava.only);
    
    private final ForeignJava fj;
    
    private NamingCzar(ForeignJava fj) {
        this.fj = fj;
    }
    
    public static String deCase(String s) {
        return "_" + Integer.toString(s.hashCode()&0x7fffffff,16);
    }

    public static String deCaseName(APIName s) {
        return s + "-" + Integer.toString(s.getText().hashCode()&0x7fffffff,16);
    }

    public static String deCase(APIName s) {
        return "-" + Integer.toString(s.getText().hashCode()&0x7fffffff,16);
    }


    /* File name stuff */
    /**
     * Returns the name, with no leading directory, of the class
     * used to implement the environment for an API.
     * 
     * @param a
     * @return
     */
    static public String cachedClassNameForApiEnv(APIName a, GraphRepository gr) {
        return "";
    }
    
    /**
     * Returns the name, with no leading directory, of the class
     * used to implement the environment for a component.
     * @param a
     * @return
     */
    static public String cachedClassNameForCompEnv(APIName a) {
        return "";
    }    
    
    /**
     * Returns the name, with no leading directory, of the file in which
     * in API's AST is stored.  Different phases that store AST, must store
     * in different directories.
     * 
     * @param a
     * @return
     */
    static public String cachedFileNameForApiAst(APIName a) {
        return "";
    }
    
    static public String cachedFileNameForCompAst(APIName a) {
        return "";
    }

     public static String cachedFileNameForCompAst(String passedPwd, APIName name) {
        return ProjectProperties.compFileName(passedPwd,  deCaseName(name));
    }
     
     public static String dependenceFileNameForCompAst(APIName name) {
         return ProjectProperties.compFileName(ProjectProperties.ANALYZED_CACHE_DEPENDS_DIR, deCaseName(name));
     }
     
     public static String dependenceFileNameForApiAst(APIName name) {
         return ProjectProperties.compFileName(ProjectProperties.ANALYZED_CACHE_DEPENDS_DIR, deCaseName(name));
     }
     
    /* Converting names of Fortress entities into Java entities. */
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
     public String unboxedTypeName(Type t) {
         if (t instanceof ArrowType) {
             
         } else if (t instanceof BaseType) {
             if (t instanceof AnyType) {
                 
             } else if (t instanceof BottomType) {
                 
             } else if (t instanceof NamedType) {
                 if (t instanceof TraitType) {
                     // has args, or not.
                 } else if (t instanceof VarType) {
                     // Cannot translate a VarType, need a binding.
                 }
             }
         }
         throw new Error("unhanded case");

     }

}
