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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import com.sun.fortress.compiler.IndexBuilder;
import com.sun.fortress.compiler.NamingCzar;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.nativeInterface.*;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.AliasedAPIName;
import com.sun.fortress.nodes.AliasedSimpleName;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.CompilationUnit;
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
import com.sun.fortress.nodes.WhereClause;
import com.sun.fortress.nodes_util.Modifiers;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.repository.graph.ApiGraphNode;
import com.sun.fortress.repository.graph.GraphNode;
import com.sun.fortress.useful.BASet;
import com.sun.fortress.useful.Bijection;
import com.sun.fortress.useful.CheapSerializer;
import com.sun.fortress.useful.F;
import com.sun.fortress.useful.HashBijection;
import com.sun.fortress.useful.IMultiMap;
import com.sun.fortress.useful.MagicNumbers;
import com.sun.fortress.useful.MapOfMap;
import com.sun.fortress.useful.MapOfMapOfSet;
import com.sun.fortress.useful.MultiMap;
import com.sun.fortress.useful.Useful;
import com.sun.fortress.useful.VersionMismatch;

import edu.rice.cs.plt.tuple.Option;

public class ForeignJava {

    public static final ForeignJava only = new ForeignJava();

    private ForeignJava() {

    }

   /** given an API Name, what Java classes does it use (include)?  (Existing
     * Java class, not its compiled Fortress wrapper class.)
     * This is a subset of all the classes available in the package
     * corresponding to the API Name.
     */
    MultiMap<APIName, org.objectweb.asm.Type> classesIncludedInForeignAPI = new MultiMap<APIName, org.objectweb.asm.Type>();

    /**
     * given an API Name, what Java classes is it actually importing?
     * This is (potentially) a subset of the API-importing-foreign-API composed
     * with javaImplementedAPIs, because two imports (from different APIs) from
     * the same foreign API might reference different classes.
     */
    MapOfMap<APIName, org.objectweb.asm.Type, Long> classesImportedByAPI = new MapOfMap<APIName, org.objectweb.asm.Type, Long>();
    MapOfMap<APIName, org.objectweb.asm.Type, Long> classesImportedByComp = new MapOfMap<APIName, org.objectweb.asm.Type, Long>();

    /**
     * This is an in-memory cache of information read from disk (if any).
     */
    Map<APIName, Map<org.objectweb.asm.Type, Long>> classesImportedByAPIOld = new MapOfMap<APIName, org.objectweb.asm.Type, Long>();
    Map<APIName, Map<org.objectweb.asm.Type, Long>> classesImportedByCompOld = new MapOfMap<APIName, org.objectweb.asm.Type, Long>();

    /**
     * Given a foreign API Name, what other (foreign) APIs does it import?
     */
    MapOfMapOfSet<APIName, APIName, Id> generatedImports = new MapOfMapOfSet<APIName, APIName, Id>();

    /**
     * Given an API Name, what are the decls in it?
     * This includes both trait (class) decls and function (static method) decls.
     */
    public MultiMap<APIName, Decl> apiToStaticDecls = new MultiMap<APIName, Decl>();

    /**
     * Given a Java class, what items from it were requested?
     * Possibilities are:
     * 1) Non-empty strings; those particular things.
     * 2) Empty string; the entire class.
     * 3) Nothing at all; an opaque import.
     */
    MultiMap<org.objectweb.asm.Type, String> itemsFromClasses = new MultiMap<org.objectweb.asm.Type, String>();

    Map<APIName, ApiIndex> cachedFakeApis = new HashMap<APIName, ApiIndex>();

    Set<APIName> foreignApisNeedingCompilation = new HashSet<APIName>();

    /**
     * Stores the FnDecl for a particular method -- also acts as an is-visited
     * marker.
     */
    public Bijection<String, FnDecl> methodToDecl = new HashBijection<String, FnDecl>();

    /**
     * Stores the TraitDecl for a trait.
     */
    public Bijection<Type, TraitDecl> classToTraitDecl = new HashBijection<Type, TraitDecl>();

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

    static ClassNode findClass(String s) throws IOException {
        ClassReader cr = new ClassReader(s);
        ClassNode cn = new ClassNode();
        cr.accept(cn, ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES|ClassReader.SKIP_CODE);
        return cn;
    }

   static CheapSerializer<Type> typeSerializer = new CheapSerializer<Type>() {



    @Override
    public Type read(InputStream i) throws IOException {
        return Type.getType(CheapSerializer.STRING.read(i));
    }

    @Override
    public void write(OutputStream o, Type data) throws IOException {
        CheapSerializer.STRING.write(o, data.getDescriptor());
    }

    byte[] V = {'A', 'S', 'M', '.', 'T', 'Y', 'P', 'E', '_', '1', '.', '0', ' '};

    @Override
    public void version(OutputStream o) throws IOException {
        o.write(V);
    }

    @Override
    public void version(InputStream o) throws IOException, VersionMismatch {
        check(o,V);
    }

    };

    static CheapSerializer<Map<Type, Long>> dependenceSerializer =
        new CheapSerializer.MAP<Type, Long>(typeSerializer, CheapSerializer.LONG);

    private static Span span() {
        return NodeFactory.internalSpan;
    }
    private static Span span(MethodNode m) {
        return NodeFactory.makeSpan(methodName(m));
    }
    private static Span span(APIName a) {
        return NodeFactory.makeSpan(a.getText());
    }
    private static Span span(String s) {
        return NodeFactory.makeSpan(s);
    }
    private static Span span(Type t) {
        return NodeFactory.makeSpan(t.getDescriptor());
    }

    static org.objectweb.asm.Type type(ClassNode cl) {
        return org.objectweb.asm.Type.getObjectType(cl.name);
    }

    static org.objectweb.asm.Type returnType(MethodNode meth) {
        return org.objectweb.asm.Type.getReturnType(meth.desc);
   }

    static org.objectweb.asm.Type[] argumentTypes(MethodNode meth) {
        return org.objectweb.asm.Type.getArgumentTypes(meth.desc);
   }

    static String getName(MethodNode m) {
        return m.name;
    }

    static String getSimpleName(String internal_name) {
        int last_slash = internal_name.lastIndexOf("/");
        String simple_name = internal_name.substring(last_slash+1);
        return simple_name;
    }
    static String getSimpleName(ClassNode cl) {
        return getSimpleName(cl.name);
    }

    static String getPackageName(String internal_name) {
        int last_slash = internal_name.lastIndexOf("/");
        String package_name = internal_name.substring(0, last_slash);
        package_name = Useful.replace(package_name, "/", ".");
        return package_name;
    }
    static String getPackageName(ClassNode cl) {
        return getPackageName(cl.name);
    }

    static APIName packageToAPIName(String s) {
        return NodeFactory.makeAPIName(span(s), s);
    }

    void processJavaImport(CompilationUnit comp, Import i, ImportNames ins) {
        //
        // Need to record dependence of importer on the class implied by i.
        //

        APIName pkg_name = ins.getApiName();
        String pkg_name_string = pkg_name.getText();
        List<AliasedSimpleName> names = ins.getAliasedNames();
        for (AliasedSimpleName name : names) {
            Option<IdOrOpOrAnonymousName> opt_alias = name.getAlias();
            if (opt_alias.isSome()) {
//                throw StaticError.make(
//                "Import aliasing not yet implemented for foreign imports ", i);
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
            ClassNode imported_class = null;
            while (last_dot > 0) {
                String candidate_class = suffix.substring(0,last_dot);
                candidate_class = pkg_name_string + "." + Useful.replace(candidate_class, ".", "$");
                    try {
                        imported_class = findClass(candidate_class);
                        break;
                        // exit with imported_class and last_dot
                    } catch (IOException e) {
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
            recurOnClass(comp, pkg_name, imported_class, imported_item);

        } /* for name : names */

//        System.err.println("classesIncludedInForeignAPI="+classesIncludedInForeignAPI);
//        System.err.println("classesImportedByAPI="+classesImportedByAPI);
//        System.err.println("itemsFromClasses="+itemsFromClasses);

    }

    /**
     * @param imported_type
     */
    private com.sun.fortress.nodes.Type recurOnOpaqueClass(APIName importing_package, org.objectweb.asm.Type imported_type) {
        // Need to special-case for primitives, String, Object, void.
        // Also, not a bijection.
        com.sun.fortress.nodes.Type t = NamingCzar.only.fortressTypeForForeignJavaType(imported_type);
        if (t != null)
            return t;

        String internal_name = imported_type.getInternalName();
        String package_name = getPackageName(internal_name);
        String simple_name = getSimpleName(internal_name);

        APIName api_name = packageToAPIName(package_name);
        Id name = NodeFactory.makeId(span(package_name), simple_name);

        /* Though the class may have been previously referenced, the import
         * may still need to be recorded.  Re-recording an existing import
         * is harmless.
         */
        generatedImports.putItem(importing_package, api_name, name);

        /*
         * Ensure that the API and class appear.
         */
        classesIncludedInForeignAPI.putItem(api_name,imported_type);


        /*
         *  Has there (not) been any reference to this class previously?
         *  If no reference, then record that it is needed (opaquely) and
         *  create a trivial declaration for it, and enter the declaration
         *  into the appropriate places.
         */
        if (!itemsFromClasses.containsKey(imported_type)) {
            itemsFromClasses.putKey(imported_type);
            // Construct a minimal trait declaration for this class.
            classToTraitType(imported_type, api_name, name, Collections.<Decl>emptyList());
        };

        // LOSE THE DOTTED REFERENCE, at least for now.
        // name = NodeFactory.makeId(api_name, name);
        // Note: a TraitType is a reference to a trait.
        return NodeFactory.makeTraitType(span(imported_type), false, name);
    }

    private void classToTraitType(Type imported_class, APIName api_name,
            Id name, List<Decl> decls) {
        List<StaticParam> sparams = Collections.emptyList();
        List<TraitTypeWhere> extendsC = Collections.emptyList();
        Modifiers mods = Modifiers.None;
        Option<WhereClause> whereC = Option.none();
        List<BaseType> excludesC = Collections.emptyList();
        Option<List<BaseType>> comprisesC = Option.none();
        TraitDecl td = NodeFactory.makeTraitDecl (span(imported_class), mods, name, sparams, extendsC, whereC, decls, excludesC, comprisesC);
        // Need to fake up an API for this class, too.
        classToTraitDecl.put(imported_class, td);
        apiToStaticDecls.putItem(api_name, td);
        classesIncludedInForeignAPI.putItem(api_name, imported_class);

        /* Invalidate any old entry.
         * Let's hope this does not cause problems down the line.
         */
        cachedFakeApis.remove(api_name);
    }

    static Comparator<MethodNode> methodNodeComparator = new Comparator<MethodNode> () {

        public int compare(MethodNode o1, MethodNode o2) {
            return methodName(o1).compareTo(methodName(o2));
        }

    };

    private long subsetHash(org.objectweb.asm.Type t, ClassNode cn) {
        long h = t.hashCode();
        BASet<MethodNode> methods =
            new BASet<MethodNode> (methodNodeComparator, cn.methods);
        for (MethodNode m : methods) {
            int access = m.access;
            if (isPublic(access)) {
                if (isStatic(access)) {
                    h = h * MagicNumbers.s;
                }
                Type rt = returnType(m);
                h = MagicNumbers.hashStepLong(h, MagicNumbers.M, rt.hashCode());
                Type[] pts = argumentTypes(m);
                h = MagicNumbers.hashArrayLong(pts, h);
            }
        }
        return h;
    }

    private void recurOnClass(CompilationUnit comp, APIName pkg_name, ClassNode imported_class, String imported_item) {
        org.objectweb.asm.Type t = type(imported_class);
        APIName importer = comp.getName();

        if (comp instanceof Api)
            classesImportedByAPI.putItem(importer, t, subsetHash(t, imported_class));
        else
            classesImportedByComp.putItem(importer, t, subsetHash(t, imported_class));

        Set<String> old = itemsFromClasses.get(t);

        /*
         * Import of a class, non-opaquely.
         * Keep track of which items, specifically, are imported.
         * Note that the empty string is a valid item, it means "the class itself".
         */
        if (old != null && old.size() > 0) {
            itemsFromClasses.putItem(t, imported_item);
            return;
        }

        /*
         * Class not seen before (except perhaps as an opaque import)
         * Recursively visit all of its parts.
         */
        itemsFromClasses.putItem(t, imported_item);

        List<MethodNode> methods = imported_class.methods;
//        Field[] fields = imported_class.getDeclaredFields();
//        Class[] interfaces = imported_class.getInterfaces();
//        Class super_class = imported_class.getSuperclass();

        ArrayList<Decl> trait_decls = new ArrayList<Decl>();

        for (MethodNode m : methods) {
            int access = m.access;
            if (isPublic(access)) {
                if (isStatic(access)) {
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

        Id name = NodeFactory.makeId(span(), getSimpleName(imported_class));
        classToTraitType(t, pkg_name, name, trait_decls);

    }

    private FnDecl recurOnMethod(APIName importing_package, ClassNode cl, MethodNode m, boolean is_static) {
        String methodKey = methodName(m);
        if (methodToDecl.containsKey(methodKey))
            return methodToDecl.get(methodKey);

        Type rt = returnType(m);
        Type[] pts = argumentTypes(m);
        // To construct a FnDecl, need to get names for all the other types
        com.sun.fortress.nodes.Type return_type = recurOnOpaqueClass(importing_package, rt);

        List<Param> params = new ArrayList<Param>(pts.length);
        int i = 0;
        for (Type pt : pts) {
            com.sun.fortress.nodes.Type type = recurOnOpaqueClass(importing_package, pt);
            Span param_span = NodeFactory.makeSpan(span(m) + " p#" + i);
            Id id = NodeFactory.makeId(param_span, "p"+(i++));
            Param p = NodeFactory.makeParam(id, type);
            params.add(p);
        }

        Span fn_span = span(m);
        Id id = is_static ?
                NodeFactory.makeId(fn_span, getSimpleName(cl)+"."+ getName(m)) :
            NodeFactory.makeId(fn_span, getName(m));
        FnDecl fndecl = NodeFactory.makeFnDecl(fn_span, Modifiers.None,
                id, params,Option.some(return_type), Option.<Expr>none());
        methodToDecl.put(methodKey, fndecl);
        return fndecl;
    }
    /**
     * @param m
     * @return
     */
    private static String methodName(MethodNode m) {
        return m.name+m.desc;
    }

    private boolean isPublic(int modifiers) {
        return 0 != (modifiers & Opcodes.ACC_PUBLIC);
    }

    private boolean isStatic(int modifiers) {
        return 0 != (modifiers & Opcodes.ACC_STATIC);
    }

    public boolean definesApi(APIName name) {
        return classesIncludedInForeignAPI.containsKey(name);
    }

    public boolean definesApi(GraphNode node) {
        return definesApi(node.getName());
    }

    public ApiIndex fakeApi(APIName name) {
        ApiIndex result = cachedFakeApis.get(name);
        if (result == null) {
            foreignApisNeedingCompilation.add(name);
            //generateWrappersForApi(name);

            List<Import> imports = new ArrayList<Import>();
            IMultiMap<APIName, Id> gi = generatedImports.get(name);
            if (gi != null)
                for (APIName a : gi.keySet()) {
                    importAnApi(imports, a, gi.get(a));
                }

            List<Decl> decls = new ArrayList<Decl>();
            for (Decl d : apiToStaticDecls.get(name)) {
                decls.add(d);
            }
            Api a = NodeFactory.makeApi(span(name), name, imports, decls);

            result = IndexBuilder.builder.buildApiIndex(a, Long.MIN_VALUE + 2);
            cachedFakeApis.put(name, result);
        }
        return result;

    }
    private void importAnApi(List<Import> imports, APIName a, Set<Id> items) {

        List<AliasedSimpleName> lasn = new ArrayList<AliasedSimpleName>();
        lasn = Useful.applyToAllAppending(items, new F<Id, AliasedSimpleName>() {
            @Override
            public AliasedSimpleName apply(Id x) {
                return NodeFactory.makeAliasedSimpleName(x);
            } }, lasn);

        ImportNames imp_names = NodeFactory.makeImportNames(span(), Option.some("java"), a, lasn);

        imports.add(imp_names);
    }

    public Map<APIName, ApiIndex> augmentApiMap(Map<APIName, ApiIndex> map) {
        for (APIName a : classesIncludedInForeignAPI.keySet()) {
            map.put(a, fakeApi(a));
        }

        // dumpGenerated();

        return map;
    }

    public void dumpGenerated() {
        for (APIName a : classesIncludedInForeignAPI.keySet()) {
            System.out.println(a);
            CompilationUnit cu = fakeApi(a).ast();
            System.out.println(cu.toStringVerbose());
        }
    }

    /**
     * Node depends on next.  Compares the on-disk record of the dependence
     * with the current version of the dependence, and return true iff there
     * is any change or missing data.
     *
     * @param node A thing (component or api) importing a Java package.
     * @param next The api generated for the Java package.
     * @return
     */
    public boolean dependenceChanged(GraphNode node, GraphNode next) {
        APIName node_name = node.getName();
        APIName next_name = next.getName();

        Map<APIName, Map<org.objectweb.asm.Type, Long>> classesImportedByOld =
            (node instanceof ApiGraphNode) ? classesImportedByAPIOld : classesImportedByCompOld;

        MapOfMap<APIName, org.objectweb.asm.Type, Long> classesImportedByNew = classImportedByCurrent(node);

        if (! classesImportedByOld.containsKey(node_name)) {
            // read the data from disk.
            readDependenceDataForAST(classesImportedByOld, node);
        }

        Map<Type, Long> oldmap = classesImportedByOld.get(node_name);
        Map<Type, Long> newmap = classesImportedByNew.get(node_name);

        if (oldmap == null || newmap == null)
            return true;

        // need to implement proper test.

        return ! oldmap.equals(newmap);
    }

     public void writeDependenceDataForAST(GraphNode node) {
        try {
            if (!hasForeignDependence(node))
                return;
            String s = dependsFileName(node);
            OutputStream o =
                    new FileOutputStream(s);
            MapOfMap<APIName, org.objectweb.asm.Type, Long> classesImportedByNew = classImportedByCurrent(node);

            dependenceSerializer.version(o);
            dependenceSerializer.write(o, classesImportedByNew.get(node.getName()));

            o.close();
        } catch (IOException ex) {

        }
    }

    static private void readDependenceDataForAST(Map<APIName, Map<org.objectweb.asm.Type, Long>> classesImportedByOld, GraphNode node) throws Error {
        InputStream i;
        String s = dependsFileName(node);
        boolean found = false;
        try {
            i = new FileInputStream(s);
            try {
            dependenceSerializer.version(i);
            Map<Type, Long> object = dependenceSerializer.read(i);

            classesImportedByOld.put(node.getName(),  object);
            } finally {
                i.close();
            }
            found = true;
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
        } catch (IOException e) {
            e.printStackTrace();
        } catch (VersionMismatch e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (! found)
            classesImportedByOld.put(node.getName(),  Collections.<Type, Long>emptyMap());

    }

    boolean hasForeignDependence(GraphNode node) {
        MapOfMap<APIName, org.objectweb.asm.Type, Long> classesImportedByNew = classImportedByCurrent(node);

        return classesImportedByNew.containsKey(node.getName());
    }

    private MapOfMap<APIName, org.objectweb.asm.Type, Long> classImportedByCurrent(
            GraphNode node) {
        MapOfMap<APIName, org.objectweb.asm.Type, Long> classesImportedByNew =
            (node instanceof ApiGraphNode) ? classesImportedByAPI : classesImportedByComp;
        return classesImportedByNew;
    }

    static private String dependsFileName(GraphNode node) {
        if (node instanceof ApiGraphNode) {
            return NamingCzar.dependenceFileNameForApiAst(node.getName(), node.getSourcePath());
        } else {
            return NamingCzar.dependenceFileNameForCompAst(node.getName(), node.getSourcePath());
        }
    }
    
    public boolean foreignApiNeedingCompilation(APIName name) {
        return foreignApisNeedingCompilation.contains(name);
    }
    
    /**
     * @param name
     */
    public void generateWrappersForApi(APIName name) {
        foreignApisNeedingCompilation.remove(name);
        
        Set<Type> classes = classesIncludedInForeignAPI.get(name);

        // Need to generate wrappers for all these classes,
        // if they do not already exist.
        for (Type t : classes) {
            FortressTransformer.transform(t.getClassName());
        }
    }



}
