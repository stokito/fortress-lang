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

import com.sun.fortress.compiler.IndexBuilder;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.AliasedAPIName;
import com.sun.fortress.nodes.AliasedSimpleName;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.Decl;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.FnDecl;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.nodes.Import;
import com.sun.fortress.nodes.ImportApi;
import com.sun.fortress.nodes.ImportNames;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.TraitDecl;
import com.sun.fortress.nodes.TraitObjectDecl;
import com.sun.fortress.nodes.TraitTypeWhere;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.WhereClause;
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

    /** given an API Name, what Java classes does it import?  (Name of existing
     * Java class, not its wrapper.)
     */
    MultiMap<APIName, Class> javaImplementedAPIs = new MultiMap<APIName, Class>();
    
    /**
     * Given a foreign API Name, what other (foreign) APIs does it import?
     */
    MultiMap<APIName, APIName> generatedImports = new MultiMap<APIName, APIName>();
    
    /**
     * Given an API Name, what are the decls in it?
     * This includes both trait (class) decls and function (static method) decls.
     */
    MultiMap<APIName, Decl> apiToStaticDecls = new MultiMap<APIName, Decl>();

    /**
     * Given a Java class, what items from it were requested?
     * Possibilities are:
     * 1) Non-empty strings; those particular things.
     * 2) Empty string; the entire class.
     * 3) Nothing at all; an opaque import.
     */
    MultiMap<Class, String> itemsFromClasses = new MultiMap<Class, String>();
    
    Map<APIName, ApiIndex> cachedFakeApis = new HashMap<APIName, ApiIndex>();
    
    /**
     * Stores the FnDecl for a particular method -- also acts as an is-visited
     * marker.
     */
    Bijection<Method, FnDecl> methodToDecl = new HashBijection<Method, FnDecl>();
    
    /**
     * Stores the TraitDecl for a trait.
     */
    Bijection<Class, TraitDecl> classToTraitDecl = new HashBijection<Class, TraitDecl>();

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
            recurOnClass(pkg_name, imported_class, imported_item);
            
        } /* for name : names */
        System.err.println("javaImplementedAPIs="+javaImplementedAPIs);
        System.err.println("itemsFromClasses="+itemsFromClasses);
    }

    /**
     * @param imported_class
     */
    private Type recurOnOpaqueClass(APIName importing_package, Class imported_class) {
        // Need to special-case for primitives, String, Object, void.
        // Also, not a bijection.
        Type  t = specialCases.get(imported_class);
        if (t != null)
            return t;
        
        Package p = imported_class.getPackage();
        APIName api_name = packageToAPIName(p);
        
        /* Though the class may have been previously referenced, the import
         * may still need to be recorded.  Re-recording an existing import
         * is harmless.
         */
        generatedImports.putItem(importing_package, api_name);
        
        Id name = NodeFactory.makeId(span, imported_class.getSimpleName());
        
        /*
         *  Has there (not) been any reference to this class previously?
         *  If no reference, then record that it is needed (opaquely) and
         *  create a trivial declaration for it, and enter the declaration
         *  into the appropriate places.
         */
        if (!itemsFromClasses.containsKey(imported_class)) {
            itemsFromClasses.putKey(imported_class);
            // Construct a minimal trait declaration for this class.
            classToTraitType(imported_class, api_name, name, Collections.<Decl>emptyList());
        };
        
        name = NodeFactory.makeId(api_name, name);
        // Note: a TraitType is a reference to a trait.
        return NodeFactory.makeTraitType(span, false, name);
    }

    private void classToTraitType(Class imported_class, APIName api_name,
            Id name, List<Decl> decls) {
        List<StaticParam> sparams = Collections.emptyList();
        List<TraitTypeWhere> extendsC = Collections.emptyList();
        Modifiers mods = Modifiers.None;
        Option<WhereClause> whereC = Option.none();
        List<BaseType> excludesC = Collections.emptyList();
        Option<List<BaseType>> comprisesC = Option.none();
        TraitDecl td = NodeFactory.makeTraitDecl (span, mods, name, sparams, extendsC, whereC, decls, excludesC, comprisesC);
        // Need to fake up an API for this class, too.
        classToTraitDecl.put(imported_class, td);
        apiToStaticDecls.putItem(api_name, td);
        javaImplementedAPIs.putItem(api_name, imported_class);
        
        /* Invalidate any old entry.
         * Let's hope this does not cause problems down the line.
         */
        cachedFakeApis.remove(api_name);
    }
    
    private void recurOnClass(APIName pkg_name, Class imported_class, String imported_item) {
        Set<String> old = itemsFromClasses.get(imported_class);
        
        /*
         * Import of a class, non-opaquely.
         * Keep track of which items, specifically, are imported.
         * Note that the empty string is a valid item, it means "the class itself".
         */
        if (old != null && old.size() > 0) { 
            itemsFromClasses.putItem(imported_class, imported_item);
            return;
        }
        
        /*
         * Class not seen before (except perhaps as an opaque import)
         * Recursively visit all of its parts.
         */
        itemsFromClasses.putItem(imported_class, imported_item);

        Method[] methods = imported_class.getDeclaredMethods();
        Field[] fields = imported_class.getDeclaredFields();
        Class[] interfaces = imported_class.getInterfaces();
        Class super_class = imported_class.getSuperclass();
        
        ArrayList<Decl> trait_decls = new ArrayList<Decl>();
        
        for (Method m : methods) {
            if (isPublic(m.getModifiers())) {
                if (isStatic(m.getModifiers())) {
                 // static goes to api-indexed set
                    FnDecl decl = recurOnMethod(pkg_name, imported_class, m, true);
                    apiToStaticDecls.putItem(pkg_name, decl);
                } else {
                    // non static goes to declaration pile for class
                    FnDecl decl = recurOnMethod(pkg_name, imported_class, m, false);
                    trait_decls.add(decl);
                }
            }
        }
        
        Id name = NodeFactory.makeId(span, imported_class.getSimpleName());
        classToTraitType(imported_class, pkg_name, name, trait_decls);
        
    }

    private FnDecl recurOnMethod(APIName importing_package, Class cl, Method m, boolean is_static) {
        if (methodToDecl.containsKey(m))
            return methodToDecl.get(m);
        
        Class rt = m.getReturnType();
        Class[] pts = m.getParameterTypes();
        // To construct a FnDecl, need to get names for all the other types
        Type return_type = recurOnOpaqueClass(importing_package, rt);
        
        List<Param> params = new ArrayList<Param>(pts.length);
        int i = 0;
        for (Class pt : pts) {
            Type type = recurOnOpaqueClass(importing_package, pt);
            Id id = NodeFactory.makeId(span, "p"+(i++));
            Param p = NodeFactory.makeParam(id, type);
            params.add(p);
        }
        Id id = is_static ?
                NodeFactory.makeId(span, cl.getSimpleName()+"."+ m.getName()) :
            NodeFactory.makeId(span, m.getName());
        FnDecl fndecl = NodeFactory.makeFnDecl(span, Modifiers.None,
                id, params,Option.some(return_type), Option.<Expr>none());
        methodToDecl.put(m, fndecl);
        return fndecl;
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

    public ApiIndex fakeApi(APIName name) {
        ApiIndex result = cachedFakeApis.get(name);
        if (result == null) {

            Set<Class> classes = javaImplementedAPIs.get(name);
            // Need to generate wrappers for all these classes,
            // if they do not already exist.

            List<Import> imports = new ArrayList<Import>();
            Set<APIName> gi = generatedImports.get(name);
            if (gi != null)
            for (APIName a : gi) {
                AliasedAPIName aan = NodeFactory.makeAliasedAPIName(a);
                /*
                 * Hoping to lie, slightly, to static analysis. This is
                 * technically speaking a "foreign" import, but the import is
                 * already known to the ForeignJava data structures, and this
                 * allows use of fully qualified (hence unambiguous) references
                 * to classes from other packages in the generated API.
                 * 
                 * So, one lie -- no foreign annotation.
                 */
                ImportApi iapi = NodeFactory.makeImportApi(span, Option
                        .<String> none(), Useful.list(aan));
                imports.add(iapi);
            }

            List<Decl> decls = new ArrayList<Decl>();
            for (Decl d : apiToStaticDecls.get(name)) {
                decls.add(d);
            }
            Api a = NodeFactory.makeApi(span, name, imports, decls);

            result = IndexBuilder.builder.buildApiIndex(a, Long.MIN_VALUE + 2);
            cachedFakeApis.put(name, result);
        }
        return result;
        
    }
    
    public Map<APIName, ApiIndex> augmentApiMap(Map<APIName, ApiIndex> map) {
        for (APIName a : javaImplementedAPIs.keySet()) {
            map.put(a, fakeApi(a));
        }
        return map;
    }

}
