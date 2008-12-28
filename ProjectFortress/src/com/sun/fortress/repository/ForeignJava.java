/*******************************************************************************
    Copyright 2008 Sun Microsystems, Inc.,
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

package com.sun.fortress.repository;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.AliasedSimpleName;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.FnDecl;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.nodes.Import;
import com.sun.fortress.nodes.ImportNames;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.TraitObjectDecl;
import com.sun.fortress.nodes.TraitTypeWhere;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes_util.Modifiers;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.useful.Bijection;
import com.sun.fortress.useful.HashBijection;
import com.sun.fortress.useful.MultiMap;
import com.sun.fortress.useful.Useful;

import edu.rice.cs.plt.tuple.Option;

public class ForeignJava {

    /** given an API Name, what Java class does it import?  (Name of existing
     * Java class, not its wrapper.)
     */
    MultiMap<APIName, Class> javaImplementedAPIs = new MultiMap<APIName, Class>();

    /**
     * Given a Java class, what items from it were requested?
     * Possibilities are:
     * 1) Non-empty strings; those particular things.
     * 2) Empty string; the entire class.
     * 3) Nothing at all; an opaque import.
     */
    MultiMap<Class, String> itemsFromClasses = new MultiMap<Class, String>();
    
    Bijection<Method, FnDecl> methodToDecl = new HashBijection<Method, FnDecl>();
    Bijection<Class, TraitObjectDecl> classToDecl = new HashBijection<Class, TraitObjectDecl>();

    /* Special cases
     * 
     * Boolean
     * Byte
     * Character
     * Short
     * Int
     * Long
     * Float
     * Double
     * 
     * all of the above, .TYPE (the primitives)
     * 
     * 
     * String
     */
    
    static Span span = NodeFactory.internalSpan;

    static Map<Class, Type> specialCases = new HashMap<Class, Type>();
    static APIName fortLib =
        NodeFactory.makeAPIName(span, "FortressLibrary");
    
    static void s(Class cl, APIName api, String str) {
        specialCases.put(cl, NodeFactory.makeTraitType(span, false, NodeFactory.makeId(span, api, str)));
    }
    
    static {
        s(Boolean.class, fortLib, "Boolean");
        s(Boolean.TYPE, fortLib, "Boolean");
        s(Integer.class, fortLib, "ZZ32");
        s(Integer.TYPE, fortLib, "ZZ32");
        s(Long.class, fortLib, "ZZ64");
        s(Long.TYPE, fortLib, "ZZ64");
        s(Float.class, fortLib, "RR32");
        s(Float.TYPE, fortLib, "RR32");
        s(Double.class, fortLib, "RR64");
        s(Double.TYPE, fortLib, "RR64");
        s(String.class, fortLib, "String");
        s(BigInteger.class, fortLib, "ZZ");
        specialCases.put(Void.TYPE, NodeFactory.makeVoidType(span));
    }
    
    APIName packageToAPIName(Package p) {
        return NodeFactory.makeAPIName(span, p.getName());
    }

    void processJavaImport(Import i, ImportNames ins) {
        APIName pkg_name = ins.getApiName();
        String pkg_name_string = pkg_name.getText();
        List<AliasedSimpleName> names = ins.getAliasedNames();
        for (AliasedSimpleName name : names) {
            Option<IdOrOpOrAnonymousName> opt_alias = name.getAlias();
            if (opt_alias.isSome()) {
                throw StaticError.make(
                "Import aliasing not yet implemented for foreign imports ", i);
            }

            IdOrOpOrAnonymousName imported = name.getName();
            Option<APIName> dotted_prefix = imported.getApiName();
            String suffix = NodeUtil.nameString(imported);
            /*
             * Okay, so this means that the "api" being imported
             * is pkg_name, and the class is specified either as
             * the first part of the prefix (which looks like an
             * APIName) or the suffix.
             *
             * Possible cases:
             *
             * Aclass.AnInnerClass (Map.Entry)
             * Aclass.AStaticFunction
             * Aclass.AField
             * Aclass.AnInner*.{AStaticFunction,AField}
             */
            
            /*
             * More hints.
             * The api part of the name, corresponds to a package name.
             * So, if it is an inner class, that dot has to come
             * in the suffix.
             * 
             * Inner classes expect a dollar sign where the dot
             * appears in the Java text.
             */
            
            /*
             * There can be static inner classes,
             * static functions,
             * and static fields,
             * all with the same dotted name.
             * 
             * For now, try the static inner class first,
             * then the function,
             * then the fields.
             */
            
            
            int last_dot = suffix.length();
            
            /* Try replacing fewer and fewer dots with $; prefer the
             * "classiest" answer.
             */
            Class imported_class = null;
            while (last_dot > 0) {
                String candidate_class = suffix.substring(0,last_dot);
                candidate_class = pkg_name_string + "." + Useful.replace(candidate_class, ".", "$");
                    try {
                        imported_class = Class.forName(candidate_class);
                        break;
                        // exit with imported_class and last_dot
                    } catch (ClassNotFoundException e) {
                        // failed to get this one
                    }
                last_dot = suffix.lastIndexOf('.', last_dot);
            }
            if (imported_class == null) {
                // Could not match a class to any prefix
                throw StaticError.make("Could not find any Java package.class prefix of  "+ pkg_name_string + "." + suffix, i);
            }
            /* imported_class specifies the class,
             * the item is the unused portion of the suffix.
             */
            String imported_item = suffix.substring(last_dot);
            if (imported_item.length() == 0) {
                
            } else if (imported_item.startsWith(".", 0)) {
                imported_item = imported_item.substring(1);
            } else {
                throw StaticError.make("Internal error processing imported item " + imported_item, i);
            }
            /* Record import of (part of) pkg_name_string.
             * Class imported_class
             * item imported_item -- entire class if "".
             */
            // TODO
            javaImplementedAPIs.putItem(pkg_name, imported_class);
            recurOnClass(imported_class, imported_item);
            
        } /* for name : names */
        System.err.println("javaImplementedAPIs="+javaImplementedAPIs);
        System.err.println("itemsFromClasses="+itemsFromClasses);
        System.err.println("    z   ");
    }

    /**
     * @param imported_class
     */
    private Type recurOnOpaqueClass(Class imported_class) {
        // Need to special-case for primitives, String, Object, void.
        // Also, not a bijection.
        Type  t = specialCases.get(imported_class);
        if (t != null)
            return t;
        
        TraitObjectDecl tod = classToDecl.get(imported_class);
        Package p = imported_class.getPackage();
        APIName api_name = packageToAPIName(p);
        
        Id name = NodeFactory.makeId(span, imported_class.getSimpleName());
        if (!itemsFromClasses.containsKey(imported_class)) {
            itemsFromClasses.putKey(imported_class);
            // Construct a minimal trait declaration for this class.
            List<StaticParam> sparams = Collections.emptyList();
            List<TraitTypeWhere> extendsC = Collections.emptyList();
            tod = NodeFactory.makeTraitDecl(span, name, sparams, extendsC);
            // Need to fake up an API for this class, too.
            classToDecl.put(imported_class, tod);
            javaImplementedAPIs.putItem(api_name, imported_class);
        };
        
        name = NodeFactory.makeId(api_name, name);
        return NodeFactory.makeTraitType(span, false, name);
    }
    
    private void recurOnClass(Class imported_class, String imported_item) {
        Set<String> old = itemsFromClasses.get(imported_class);
        if (old != null && old.contains(imported_item)) {
            return;
        }
        /* We're here because imported_item had not been seen before in this
         * class's set of items.
         * 
         * For now, just ignore the imported_item, and pull in all the class
         * members that we can see.  Recursion includes the parent class and
         * interfaces, too.
         */
        itemsFromClasses.putItem(imported_class, imported_item);

        Method[] methods = imported_class.getDeclaredMethods();
        Field[] fields = imported_class.getDeclaredFields();
        Class[] interfaces = imported_class.getInterfaces();
        Class super_class = imported_class.getSuperclass();
        
        for (Method m : methods) {
            if (isPublic(m.getModifiers())) {
                recurOnMethod(m);
            }
        }
        
    }

    private void recurOnMethod(Method m) {
        if (methodToDecl.containsKey(m))
            return;
        
        Class rt = m.getReturnType();
        Class[] pts = m.getParameterTypes();
        // To construct a FnDecl, need to get names for all the other types
        Type return_type = recurOnOpaqueClass(rt);
        
        List<Param> params = new ArrayList<Param>(pts.length);
        int i = 0;
        for (Class pt : pts) {
            Type type = recurOnOpaqueClass(pt);
            Id id = NodeFactory.makeId(span, "p"+(i++));
            Param p = NodeFactory.makeParam(id, type);
            params.add(p);
        }
        Id id = NodeFactory.makeId(span, m.getName());
        FnDecl fndecl = NodeFactory.makeFnDecl(span, Modifiers.None,
                id, params,Option.some(return_type), Option.<Expr>none());
        methodToDecl.put(m, fndecl);
    }

    private boolean isPublic(int modifiers) {
        return 0 != (modifiers & java.lang.reflect.Modifier.PUBLIC);
    }
    
    private boolean isStatic(int modifiers) {
        return 0 != (modifiers & java.lang.reflect.Modifier.STATIC);
    }

    public boolean definesApi(APIName name) {
        return javaImplementedAPIs.containsKey(name);
    }
    
    
    
    

}
