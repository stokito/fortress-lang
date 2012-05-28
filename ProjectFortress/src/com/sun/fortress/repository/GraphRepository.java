 /*******************************************************************************
    Copyright 2008,2012, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.repository;

import com.sun.fortress.Shell;
import com.sun.fortress.compiler.*;
import com.sun.fortress.compiler.Parser.Result;
import static com.sun.fortress.compiler.WellKnownNames.defaultLibrary;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.exceptions.MultipleStaticError;
import com.sun.fortress.exceptions.ProgramError;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.exceptions.WrappedException;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.ASTIO;
import com.sun.fortress.nodes_util.NodeComparator;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.repository.graph.*;
import com.sun.fortress.runtimeSystem.ByteCodeWriter;
import com.sun.fortress.scala_src.typechecker.IndexBuilder;
import com.sun.fortress.useful.BATree;
import com.sun.fortress.useful.Debug;
import com.sun.fortress.useful.DefaultComparator;
import com.sun.fortress.useful.Fn;
import com.sun.fortress.useful.Path;
import com.sun.fortress.useful.Useful;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.OptionUnwrapException;
import com.sun.fortress.useful.Pair;
import com.sun.fortress.linker.Linker;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

/* A graph-based repository. This repository determines the dependency structure
 * before any components/APIs are compiled so that they can be compiled in an
 * efficient and deterministic order.
 *
 * When compiling a .fsi file, the .fsi depends on the transitive
 * closure of dependencies of the APIs it imports and comprises.
 *
 * When only compiling a .fss file, the .fss depends on its imported
 * and exported APIs, and the transitive closure of the APIs they
 * depend on.
 *
 * When linking a .fss file, the .fss depends on its imported and
 * exported APIs. Components that implement those APIs are added to
 * the graph.
 */
public class GraphRepository extends StubRepository implements FortressRepository {

    /* stores the nodes and their relationships */
    private Graph<GraphNode> graph;
    /* current source path */
    private Path path;
    /* underlying cache of compiled files */
    private CacheBasedRepository cache;
    /* true if a recompile is needed */
    private boolean needUpdate = true;
    /* If link is true then pull in a component for an API */
    private boolean link = false;
    
    static private Map<String, DerivedFiles<CompilationUnit>> otherCaches =
            new BATree<String, DerivedFiles<CompilationUnit>>(DefaultComparator.<String>normal());

    ForeignJava foreignJava = ForeignJava.only;

    public GraphRepository(Path p, CacheBasedRepository cache) throws IOException {
        this.path = p;
        this.cache = cache;
        graph = new Graph<GraphNode>();
        addRoots();
    }

    private GraphRepository(Path p, String cacheDir) throws IOException {
        this(p, new CacheBasedRepository(cacheDir));
    }

    public boolean isForeign(APIName name) {
        return foreignJava.definesApi(name);
    }

    public DerivedFiles<CompilationUnit> getDerivedComponentCache(final String cache_path) {
        String key = path + "//" + cache_path;
        DerivedFiles<CompilationUnit> derived_cache = otherCaches.get(key);
        if (derived_cache == null) {

            Fn<PathTaggedApiName, String> toCompFileName = new Fn<PathTaggedApiName, String>() {
                @Override
                public String apply(PathTaggedApiName x) {
                    return ProjectProperties.compFileName(cache_path, NamingCzar.deCaseName(x));
                }
            };

            IOAst componentReaderWriter = new IOAst(toCompFileName);

            derived_cache = new DerivedFiles<CompilationUnit>(componentReaderWriter);

            otherCaches.put(key, derived_cache);
        }
        return derived_cache;
    }

    private static String[] roots() {
        /* files that are dependencies of everything */
        return defaultLibrary();
    }

    /* by default all the root APIs should be added to the graph
     * and set as dependencies for everything else.
     */
    private void addRoots() throws IOException {
        for (String root : roots()) {
            APIName name = NodeFactory.makeAPIName(NodeFactory.shellSpan, root);
            File api_file = getApiFile(name);
            ApiGraphNode api = new ApiGraphNode(name, api_file);
            try {
                long cache_date = cache.getModifiedDateForApi(api);
                api.setApi(cache.getApi(api.getName(), api.getSourcePath()), cache_date);
            }
            catch (FileNotFoundException e) {
            }
            catch (IOException e) {
            }
            graph.addNode(api);
        }

        for (String root : roots()) {
            ApiGraphNode node =
                    (ApiGraphNode) graph.find(ApiGraphNode.key(NodeFactory.makeAPIName(NodeFactory.shellSpan, root)));
            try {
                for (APIName api : dependencies(node)) {
                    Debug.debug(Debug.Type.REPOSITORY, 2, "Add edge ", api);
                    //                    graph.addEdge(node, addApiGraph(api));
                    addApiGraph(api);

                }
            }
            catch (FileNotFoundException e) {
            }
            catch (IOException e) {
            }
        }
    }

    @Override
    public Map<APIName, ApiIndex> apis() {
        return foreignJava.augmentApiMap(cache.apis.copy());
    }

    @Override
    public Map<APIName, ComponentIndex> components() {
        return cache.components();
    }

    /* getApi and getComponent add an API/component to the graph, find all the
     * dependencies (via addApiGraph/addComponentGraph) and recompile everything.
     */
    @Override
    public ApiIndex getApi(APIName name) throws FileNotFoundException, IOException, StaticError {
        Debug.debug(Debug.Type.REPOSITORY, 2, "Get API for ", name);

        ApiGraphNode node = addApiGraph(name);
        refreshGraph();
        try {
            return node.getApi().unwrap();
        }
        catch (OptionUnwrapException o) {
            throw StaticError.make("Cannot find API " + name +
                                   " in the repository. This should not happen, please contact a developer.");
        }
    }

    @Override
    public ComponentIndex getComponent(APIName name) throws FileNotFoundException, IOException, StaticError {
        Debug.debug(Debug.Type.REPOSITORY, 2, "Get component for ", name);

        // Generate jar files for new aliases
        
        ComponentGraphNode node = addComponentGraph(name);
        refreshGraph();
        
        Linker.linkAll();
        
        try {
            return node.getComponent().unwrap();
        }
        catch (OptionUnwrapException o) {
            throw StaticError.make("Cannot find component " + name + " in the repository. " +
                                   "This should not happen, please contact a developer.");
        }
    }

    /* Add an API node to the graph and return the node. If the API exists in the
     * cache it is loaded, otherwise it will remain empty until it gets
     * recompiled (probably via refreshGraph).
     */
    private ApiGraphNode addApiGraph(APIName name) throws FileNotFoundException, IOException {
        Debug.debug(Debug.Type.REPOSITORY, 2, "Add API graph ", name);
        ApiGraphNode node = (ApiGraphNode) graph.find(ApiGraphNode.key(name));
        if (node == null) {

            if (foreignJava.definesApi(name)) {
                // TODO not smart about age of native API yet
                // Make the native API be very old, so nothing is out of date;
                needUpdate = true;
                node = new ApiGraphNode(name, "ForeignJava", Long.MIN_VALUE);
                graph.addNode(node);
                return node;
            }

            /* a new node was added, a recompile is needed */
            needUpdate = true;
            File api_file = getApiFile(name);
            node = new ApiGraphNode(name, api_file);
            graph.addNode(node);
            try {
                /* try to load the API from the cache.
                 * if it fails then it will be reloaded later on
                 * in refreshGraph
                 */
                long cache_date = getCacheDate(node);
                if (cache_date >= getApiFileDate(node)) {
                    Debug.debug(Debug.Type.REPOSITORY, 2, "Found cached version of ", node);
                    node.setApi(cache.getApi(name, node.getSourcePath()), cache_date);
                }
            }
            catch (FileNotFoundException f) {
                /* oh well */
            }
            catch (IOException e) {
            }
            /* make this API depend on the APIs it imports and comprises */
            for (APIName api : dependencies(node)) {
                Debug.debug(Debug.Type.REPOSITORY, 2, "Add edge ", api);
                graph.addEdge(node, addApiGraph(api));
            }
            /* and depend on all the root APIs */
            for (String root : roots()) {
                graph.addEdge(node, addApiGraph(NodeFactory.makeAPIName(NodeFactory.shellSpan, root)));
            }
        }
        return node;
    }

    /* same thing, but add a component */
    private ComponentGraphNode addComponentGraph(APIName name) throws FileNotFoundException, IOException, StaticError {
        Debug.debug(Debug.Type.REPOSITORY, 2, "Add component graph ", name);
        ComponentGraphNode node = (ComponentGraphNode) graph.find(ComponentGraphNode.key(name));
        if (node == null) {
            /* a new node was added, a recompile is needed */
            needUpdate = true;
            File component_file = getComponentFile(name);
            node = new ComponentGraphNode(name, component_file);
            graph.addNode(node);
            try {
                /* try to load the component from the cache.
                 * if it fails then it will be reloaded later on
                 * in refreshGraph
                 */
                long cache_date = getCacheDate(node);
                if (cache_date >= getComponentFileDate(node)) {
                    Debug.debug(Debug.Type.REPOSITORY, 2, "Found cached version of ", node);
                    node.setComponent(cache.getComponent(name, node.getSourcePath()), cache_date);
              
                    
                }
            }
            catch (FileNotFoundException f) {
                /* oh well */
            }
            catch (IOException e) {
            }

            /* Make this component depend on the APIs it imports.
             * For now, calling nodeDependsOnApi when linking will
             * also make this component depend on a component with the same name as
             * the API it imports.
             */
            for (APIName api : collectComponentImports(nodeToComponent(node))) {
                Debug.debug(Debug.Type.REPOSITORY,
                            2,
                            "Recording that component " + node + " depends on API/component ",
                            api);
                nodeDependsOnApi(node, api);
            }

            /* Make this component depend on the APIs it exports.
             * Do not attempt to add components with the same names as
             * these exports.
             */
            for (APIName api : collectComponentExports(nodeToComponent(node))) {
                Debug.debug(Debug.Type.REPOSITORY, 2, "Recording that component " + node + " depends on API ", api);
                // nodeDependsOnApiNotComponent(node, api);
                graph.addEdge(node, addApiGraph(api));
            }

            /* Make this component depend on all the root APIs */
            for (String root : roots()) {
                nodeDependsOnApi(node, NodeFactory.makeAPIName(NodeFactory.shellSpan, root));
            }
        }
        
        return node;
    }

    private void nodeDependsOnApiNotComponent(ComponentGraphNode node, APIName api) throws FileNotFoundException,
                                                                                           IOException {
        Debug.debug(Debug.Type.REPOSITORY, 2, "Add edge from component " + node.getName() + " to API ", api);
        graph.addEdge(node, addApiGraph(api));
    }

    private void nodeDependsOnApi(ComponentGraphNode node, APIName api) throws FileNotFoundException, IOException {
        Debug.debug(Debug.Type.REPOSITORY, 2, "Add edge from component " + node.getName() + " to API ", api);
        graph.addEdge(node, addApiGraph(api));
        boolean b = foreignJava.definesApi(api);
        // System.err.println("b="+b);
        if (link && !b) {
            Debug.debug(Debug.Type.REPOSITORY, 1, "Linking component ", node.getName(), " to component ", api);
            // Add element, but no API
            
            // At this point, the question is: which component do we want to add? 
            // We need to query the repository to know which component is exporting this API
            // this has to depend on node as well as on api
            
            APIName implementer = Linker.whoIsImplementingMyAPI(node.getName(), api);
            addComponentGraph(implementer);
            
        }
    }

    private void addComponentComprisesDependencies(ComponentGraphNode node) throws FileNotFoundException, IOException {
        for (APIName name : node.getComponent().unwrap().comprises()) {
            addComponentGraph(name);
        }
    }

    private long getCacheDate(ApiGraphNode node) {
        try {
            return cache.getModifiedDateForApi(node);
        }
        catch (FileNotFoundException e) {
            return Long.MIN_VALUE;
        }
    }

    private long getCacheDate(ComponentGraphNode node) {
        try {
            return cache.getModifiedDateForComponent(node);
        }
        catch (FileNotFoundException e) {
            return Long.MIN_VALUE;
        }
    }

    private long getCacheDate(GraphNode node) {
        try {
            return node.accept(new CacheVisitor());
        }
        catch (FileNotFoundException e) {
            return Long.MIN_VALUE;
        }
    }

    private long getComponentFileDate(ComponentGraphNode node) throws FileNotFoundException {
        return node.getSourceDate();
    }

    private File getComponentFile(APIName name) throws FileNotFoundException {
        return findFile(name, ProjectProperties.COMP_SOURCE_SUFFIX);
    }

    private long getApiFileDate(ApiGraphNode node) throws FileNotFoundException {
        return node.getSourceDate();
    }

    private File getApiFile(APIName name) throws FileNotFoundException {
        return findFile(name, ProjectProperties.API_SOURCE_SUFFIX);
    }

    public File findFile(APIName name, String suffix) throws FileNotFoundException {
    	
    	APIName n = Linker.whatToSearchFor(name);
    	
        String dotted = n.toString();
        String slashed = dotted.replace(".", "/");
        slashed = slashed + "." + suffix;
        File fdot;

        Debug.debug(Debug.Type.REPOSITORY, 3, "Finding file ", name);
        try {
            fdot = path.findFile(slashed);
        }
        catch (FileNotFoundException ex2) {
            throw new FileNotFoundException(
                    NodeUtil.getSpan(name) + ":\n    Could not find an implementation for API " + dotted +
                    " on path\n    " + path);
        }
        return fdot;
    }

    private class CacheVisitor implements GraphVisitor<Long, FileNotFoundException> {
        public Long visit(ApiGraphNode node) throws FileNotFoundException {
            return getCacheDate(node);
        }

        public Long visit(ComponentGraphNode node) throws FileNotFoundException {
            return getCacheDate(node);
        }
    }

    /* What if the file has been edited to include import statements that the cached
     * version doesn't have? that's ok because the cached version won't be loaded unless it
     * is newer than the file on disk.
     */
    private List<APIName> dependencies(ApiGraphNode node) throws FileNotFoundException, StaticError {
        CompilationUnit cu = node.getApi().isSome() ? node.getApi().unwrap().ast() : readCUFor(node,
                                                                                               ProjectProperties.API_SOURCE_SUFFIX);

        Api a = (Api) cu;

        List<APIName> result = collectApiImports(a);
        result.addAll(collectApiComprises(a));
        return result;
    }

    private Component nodeToComponent(ComponentGraphNode node) throws FileNotFoundException, StaticError {
        CompilationUnit cu = node.getComponent().isSome() ? node.getComponent().unwrap().ast() : readCUFor(node,
                                                                                                           ProjectProperties.COMP_SOURCE_SUFFIX);

        return (Component) cu;

    }

    // private List<APIName> dependencies(ComponentGraphNode node) throws FileNotFoundException, StaticError {
    //     return collectComponentDependencies(nodeToComponent(node));
    // }

    private boolean inApiList(APIName name, List<ApiGraphNode> nodes) {
        for (ApiGraphNode node : nodes) {
            if (node.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private boolean inComponentList(APIName name, List<ComponentGraphNode> nodes) {
        for (ComponentGraphNode node : nodes) {
            if (node.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    /* reparse anything that is out of date */
    private AnalyzeResult refreshGraph() throws FileNotFoundException, IOException, StaticError {
        AnalyzeResult result = new AnalyzeResult(IterUtil.<StaticError>empty());
        if (needUpdate) {
            needUpdate = false;
            OutOfDateVisitor date = new OutOfDateVisitor();
            for (GraphNode node : graph.nodes()) {
                node.accept(date);
            }
            List<ApiGraphNode> reparseApis = date.apis();
            List<ComponentGraphNode> reparseComponents = sortComponents(date.components());
            Debug.debug(Debug.Type.REPOSITORY, 1, "Out of date APIs ", reparseApis);
            Debug.debug(Debug.Type.REPOSITORY, 1, "Out of date components ", reparseComponents);
            /* these can be parsed all at once */
            result = parseApis(date.outOfDateApi());
            for (Map.Entry<APIName, ApiIndex> entry : result.apis().entrySet()) {
                if (inApiList(entry.getKey(), reparseApis)) {
                    addApi(entry.getKey(), entry.getValue());
                }
            }

            /* but these have to be done in a specific order due to
             * syntax expansion requiring some components, like
             * FortressBuiltin, being parsed.
             * If syntax expansion was unable to execute code then all
             * the APIs and components could be parsed at the same time.
             */
            for (ComponentGraphNode node : reparseComponents) {
                /* parseComponent will call Fortress.analyze which
                 * will call repository.add(component). That add() will
                 * call setComponent() on this node with the same component
                 * that parseComponent is going to return.
                 */
                result = parseComponent(syntaxExpand(node), result);
                
                // At this point, the jar file should have been generated
                Linker.generateAliases(node.getName());
                
                for (Map.Entry<APIName, ComponentIndex> entry : result.components().entrySet()) {
                    if (inComponentList(entry.getKey(), reparseComponents)) {
                        addComponent(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
        return result;
    }

    private class OutOfDateVisitor implements GraphVisitor<Boolean, FileNotFoundException> {
        // Rebuilds get triggered transitively across chains of API dependence.
        // "Youngest" is computed transitively.
        private Map<GraphNode, Long> youngestSourceDependedOn;

        // Dates are not available for foreign imports.  Therefore, keep track
        // of a hashcode of the last-seen build, and if it does not match, then
        // assume stale.
        private Set<GraphNode> foreignChange;

        // In a second pass, if anything is stale, or depends on stale, it is
        // marked for rebuild.
        private Map<GraphNode, Boolean> staleOrDependsOnStale;


        public OutOfDateVisitor() {
            youngestSourceDependedOn = new HashMap<GraphNode, Long>();
            staleOrDependsOnStale = new HashMap<GraphNode, Boolean>();
            foreignChange = new HashSet<GraphNode>();
        }

        Fn<GraphNode, Boolean> outOfDateApi() {
            return new Fn<GraphNode, Boolean>() {
                @Override
                public Boolean apply(GraphNode g) {
                    return g instanceof ApiGraphNode && (staleOrDependsOnStale.get(g) ||
                                                         // TODO is this strictly necessary?
                                                         foreignJava.definesApi(((ApiGraphNode) g).getName()));
                }
            };
        }

        Fn<GraphNode, Boolean> outOfDateComponent() {
            return new Fn<GraphNode, Boolean>() {
                @Override
                public Boolean apply(GraphNode g) {
                    return g instanceof ComponentGraphNode && staleOrDependsOnStale.get(g);
                }
            };
        }

        public List<ApiGraphNode> apis() {
            return Useful.convertList(Useful.filter(youngestSourceDependedOn.keySet(), outOfDateApi()));
        }

        /* returns out of date components */
        public List<ComponentGraphNode> components() {
            return Useful.convertList(Useful.filter(youngestSourceDependedOn.keySet(), outOfDateComponent()));
        }

        private Long handle(GraphNode node) throws FileNotFoundException {
            if (youngestSourceDependedOn.containsKey(node)) {
                return youngestSourceDependedOn.get(node);
            }
            long youngest = node.getSourceDate();
            youngestSourceDependedOn.put(node, youngest);

            List<GraphNode> depends = graph.depends(node);
            Debug.debug(Debug.Type.REPOSITORY, 2, node, " depends on ", depends);
            for (GraphNode next : depends) {
                if (foreignJava.definesApi(next)) {
                    // no date information here; need to look for the dependence
                    // info for this node.
                    if (foreignJava.dependenceChanged(node, next)) {
                        Debug.debug(Debug.Type.REPOSITORY, 1, "" + node + " added to foreign change by change in " + next);
                        foreignChange.add(node);
                    }
                }

                long dependent_youngest = handle(next);
                if (dependent_youngest > youngest) {

                    Debug.debug(Debug.Type.REPOSITORY, 3, next + " has younger source than " + node);
                    youngest = dependent_youngest;
                }

            }

            youngestSourceDependedOn.put(node, youngest);

            return youngest;
        }

        private Boolean isStale(GraphNode node) throws FileNotFoundException {
            if (staleOrDependsOnStale.containsKey(node)) {
                return staleOrDependsOnStale.get(node);
            }

            boolean stale1 = youngestSourceDependedOn.get(node) > getCacheDate(node);
            boolean stale2 = foreignChange.contains(node);
            boolean stale = stale1 || stale2;
            
            // If anything depended on has source that is younger than our compiled code,
            // then this is stale.
            if (stale) {
                if (stale1)
                    Debug.debug(Debug.Type.REPOSITORY,
                            1,
                            node,
                            " or dependent is newer ",
                            youngestSourceDependedOn.get(node),
                            " than the cache ",
                            getCacheDate(node));
                if (stale2)
                    Debug.debug(Debug.Type.REPOSITORY,
                            1,
                            node, " is in foreign change ", foreignChange);
                    
            }

            staleOrDependsOnStale.put(node, stale);

            List<GraphNode> depends = graph.depends(node);
            Debug.debug(Debug.Type.REPOSITORY, 2, node, " depends on ", depends);
            for (GraphNode next : depends) {
                boolean dependent_stale = isStale(next);

                if (dependent_stale) {
                    stale = true;
                    Debug.debug(Debug.Type.REPOSITORY, 1, node, " is stale ", next, " is stale");
                    staleOrDependsOnStale.put(node, stale);
                }
            }
            return stale;
        }

        public Boolean visit(ApiGraphNode node) throws FileNotFoundException {
            handle(node);
            return isStale(node);
        }

        public Boolean visit(ComponentGraphNode node) throws FileNotFoundException {
            handle(node);
            return isStale(node);
        }
    }

    private List<ComponentGraphNode> sortComponents(List<ComponentGraphNode> nodes) throws FileNotFoundException {
        Graph<GraphNode> componentGraph = new Graph<GraphNode>(graph, new Fn<GraphNode, Boolean>() {
            @Override
            public Boolean apply(GraphNode g) {
                return g instanceof ComponentGraphNode;
            }
        });

        /* force components that import things to depend on the component
         * that implements that import. This is for syntax abstraction
         * update 6/19/2008: this would prevent separate compilation, so
         * don't set things up this way.
         */
        /*
        for ( GraphNode node : componentGraph.nodes() ){
            ComponentGraphNode comp = (ComponentGraphNode) node;
            for ( GraphNode dependency : graph.dependencies(comp) ){
                if ( dependency instanceof ApiGraphNode ){
                    ComponentGraphNode next = (ComponentGraphNode) componentGraph.find( new ComponentGraphNode( ((ApiGraphNode) dependency).getName() ) );
                    // ComponentGraphNode next = new ComponentGraphNode( ((ApiGraphNode) dependency).getName() );
                    if ( ! next.equals( comp ) ){
                        componentGraph.addEdge( comp, next );
                    }
                }
            }
        }
        */

        if (Debug.isOnFor(2, Debug.Type.REPOSITORY)) {
            Debug.debug(Debug.Type.REPOSITORY, 2, componentGraph.getDebugString());
        }

        List<GraphNode> sorted = componentGraph.sorted();
        List<ComponentGraphNode> rest = new ArrayList<ComponentGraphNode>();

        for (GraphNode node : sorted) {
            ComponentGraphNode comp = (ComponentGraphNode) node;
            if (nodes.contains(comp)) {
                /* force root components to come in front of other things */
                if (Arrays.asList(roots()).contains(comp.getName().toString())) {
                    rest.add(0, comp);
                } else {
                    rest.add(comp);
                }
            }
        }

        return rest;
    }

    private AnalyzeResult parseApis(final Fn<GraphNode, Boolean> these_apis) {

        List<Api> unparsed = Useful.applyToAll(graph.filter(these_apis), new Fn<GraphNode, Api>() {
            @Override
            public Api apply(GraphNode g) {
                Debug.debug(Debug.Type.REPOSITORY, 1, "Parsing API ", g);
                return parseApi((ApiGraphNode) g);
            }
        });

        if (unparsed.size() == 0) return new AnalyzeResult(IterUtil.<StaticError>empty());

        GlobalEnvironment knownApis = new GlobalEnvironment.FromMap(parsedApis(unparsed));
        //         System.err.println("knownApis");
        //         knownApis.print();
        //         System.err.println("end knownApis");

        // Can we exclude non-imported pieces of the api here?


        List<Component> components = new ArrayList<Component>();
        Shell shell = new Shell(this);
        AnalyzeResult result = Shell.analyze(shell.getRepository(),
                                             knownApis,
                                             unparsed,
                                             components,
                                             System.currentTimeMillis());
        if (!result.isSuccessful()) {
            throw new MultipleStaticError(result.errors());
        }
        return result;
    }

    /* return a parsed API */
    private Api parseApi(ApiGraphNode node) {
        try {
            APIName api_name = node.getName();
            if (foreignJava.definesApi(api_name)) {
                return (Api) foreignJava.fakeApi(api_name).ast();
            } else {
                File fdot = findFile(api_name, ProjectProperties.API_SOURCE_SUFFIX);
                CompilationUnit api = Parser.parseFileConvertExn(fdot);
                if (api instanceof Api) {
                    // Is this a good side-effect?
                    node.setApi((ApiIndex)IndexBuilder.buildCompilationUnitIndex(api,
                                                                                 fdot.lastModified(), true),
                                fdot.lastModified());
                    return (Api) api;
                } else {
                    throw StaticError.make("Unexpected parse of API " + api_name);
                }
            }
        }
        catch (IOException e) {
            throw new WrappedException(e);
        }
    }


    /* find all parsed APIs */
    public Map<APIName, ApiIndex> parsedApis(List<Api> unparsed) {

        Map<APIName, ApiIndex> apis = new BATree<APIName, ApiIndex>(NodeComparator.apiNameComparer);

        for (GraphNode g : graph.nodes()) {
            if (g instanceof ApiGraphNode) {
                ApiGraphNode node = (ApiGraphNode) g;
                if (node.getApi().isSome()) {
                    apis.put(node.getName(), node.getApi().unwrap());
                } else if (foreignJava.definesApi(node.getName())) {
                    apis.put(node.getName(), foreignJava.fakeApi(node.getName()));
                }
            }
        }

        return apis;
    }

    /* parse a single component. */
    private AnalyzeResult parseComponent(Component component, AnalyzeResult apiResult) throws StaticError {
        // GlobalEnvironment knownApis = new GlobalEnvironment.FromMap(parsedApis(Collections.<Api>emptyList()));
        //GlobalEnvironment knownApis = new GlobalEnvironment.FromMap(apiResult.apis());
        GlobalEnvironment knownApis = new GlobalEnvironment.FromMap(parsedApis(new ArrayList<Api>()));
        List<Component> components = new ArrayList<Component>();
        components.add(component);
        long now = System.currentTimeMillis();
        Debug.debug(Debug.Type.REPOSITORY, 1, "Parsing ", component, " at ", now);
        //         System.err.println("knownApis");
        //         knownApis.print();
        //         System.err.println("end knownApis");

        Shell shell = new Shell(this);
        AnalyzeResult result = Shell.analyze(shell.getRepository(), knownApis, new ArrayList<Api>(), components, now);
        Debug.debug(Debug.Type.REPOSITORY, 1, "Shell.analyze for ", component, " done.");
        if (!result.isSuccessful()) {
            throw new MultipleStaticError(result.errors());
        }
        
        return result;
    }

    /* parse a component and run it through syntax expansion */
    private Component syntaxExpand(ComponentGraphNode node) throws FileNotFoundException, IOException {
        Debug.debug(Debug.Type.REPOSITORY, 1, "Expand component ", node);

        APIName api_name = node.getName();
        File file = findFile(api_name, ProjectProperties.COMP_SOURCE_SUFFIX);
        GraphRepository g1 = new GraphRepository(this.path, this.cache);
        /* FIXME: hack to prevent infinite recursion */
        Shell.setCurrentInterpreterRepository(g1);
        Result result = Parser.macroParse(file, new GlobalEnvironment.FromRepository(g1), verbose());
        // Result result = FortressParser.parse(file, new GlobalEnvironment.FromRepository(this), verbose());
        /* FIXME: hack to prevent infinite recursion */
        Shell.setCurrentInterpreterRepository(this);
        if (result.isSuccessful()) {
            Debug.debug(Debug.Type.REPOSITORY, 1, "Expanded component ", node);
            Iterator<Component> components = result.components().iterator();
            if (components.hasNext()) return components.next();
            throw new ProgramError("Successful parse result was nonetheless empty, file " + file.getCanonicalPath());
        } else throw new ProgramError(result.errors());
    }

    /* add an API to the repository and cache it */
    @Override
    public void addApi(APIName name, ApiIndex definition) {
        ApiGraphNode node = (ApiGraphNode) graph.find(ApiGraphNode.key(name));
        if (node == null) {
            throw new RuntimeException("No such API '" + name + "'");
        } else {
            // TODO if the name is native-implemented, be sure not to write
            // it to a file.
            node.setApi(definition, definition.modifiedDate());
            cache.addApi(name, definition, node.getSourcePath());
            foreignJava.writeDependenceDataForAST(node);

        }
    }

    /* add a component to the repository and cache it */
    @Override
    public void addComponent(APIName name, ComponentIndex definition) {
        ComponentGraphNode node = (ComponentGraphNode) graph.find(ComponentGraphNode.key(name));
        if (node == null) {
            throw new RuntimeException("No such component " + name);
        } else {
            node.setComponent(definition, definition.modifiedDate());
            cache.addComponent(name, definition, node.getSourcePath());
            foreignJava.writeDependenceDataForAST(node);
        }
    }

    @Override
    public void deleteComponent(APIName name, boolean andTheFileToo) {
        ComponentGraphNode node = (ComponentGraphNode) graph.find(ComponentGraphNode.key(name));
        if (node != null) {
            cache.deleteComponent(name);
            if (andTheFileToo) {
                try {
                    ASTIO.deleteJavaAst(NamingCzar.cachedPathNameForCompAst(ProjectProperties.ANALYZED_CACHE_DIR,
                                                                            node.getSourcePath(),
                                                                            name));
                }
                catch (IOException e) {
                    // We tried.  Maybe it was never written anyhow.
                }
            }
        }
    }

    @Override
    public ComponentIndex getLinkedComponent(APIName name) throws FileNotFoundException, IOException {
        link = true;
        addRootComponents();
        ComponentIndex node = getComponent(name);
        link = false;
        return node;
    }

    private void addRootComponents() throws FileNotFoundException, IOException {
        boolean added = false;
        for (String root : roots()) {
            APIName name = NodeFactory.makeAPIName(NodeFactory.shellSpan, root);
            if (null == graph.find(ApiGraphNode.key(name))) {
                addApiGraph(name);
            }
            // If the API is from import-native, treat it differently than this.
            if (link && null == graph.find(ComponentGraphNode.key(name))) {
                addComponentGraph(name);
            }
            //            ApiGraphNode node = (ApiGraphNode) graph.find(new ApiGraphNode(name));
            //            ComponentGraphNode comp = new ComponentGraphNode(name);
            //            try{
            //                comp.setComponent( cache.getComponent( comp.getName() ) );
            //            } catch ( FileNotFoundException e ){
            //            } catch ( IOException e ){
            //            }
            //            graph.addNode( comp );
            //            graph.addEdge( comp, node );
        }
    }

    @Override
    public long getModifiedDateForApi(APIName name) throws FileNotFoundException {
        return 0;
    }

    @Override
    public long getModifiedDateForComponent(APIName name) throws FileNotFoundException {
        return 0;
    }

    @Override
    public void clear() {
        cache.clear();
    }

    private List<APIName> collectApiComprises(Api api) {
        List<APIName> result = new ArrayList<APIName>();

        for (APIName cname : api.getComprises()) {
            result.add(cname);
        }
        return result;
    }

    private List<APIName> collectExplicitImports(CompilationUnit comp) {
        List<APIName> all = new ArrayList<APIName>();

        //APIName comp_name = comp.getName();

        for (Import i : comp.getImports()) {
            Option<String> opt_fl = i.getForeignLanguage();
            boolean isNative = opt_fl.isSome();
            if (isNative && (i instanceof ImportNames)) {
                String fl = opt_fl.unwrap();
                // Conditional overlap with later clause.
                // Handle import of foreign names here.
                // Ought to handle this case by case.
                ImportNames ins = (ImportNames) i;
                if ("java".equalsIgnoreCase(fl)) {
                    /*
                     *  Don't create the API yet; its contents depend on all
                     *  the imports.
                     */
                    foreignJava.processJavaImport(comp, i, ins);

                    // depend on the API name;
                    // "compilation"/"reading" will get the API
                    all.add(ins.getApiName());
                    continue;
                } else if ("fortress".equalsIgnoreCase(fl)) {
                    // do nothing, fall into normal case
                } else {
                    throw StaticError.make("Foreign language " + fl + " not yet handled ", i);
                }
            }

            if (i instanceof ImportedNames) {
                ImportedNames names = (ImportedNames) i;
                all.add(names.getApiName());
            } else { // i instanceof ImportApi
                ImportApi apis = (ImportApi) i;
                for (AliasedAPIName a : apis.getApis()) {
                    all.add(a.getApiName());
                }
            }
        }
        return all;
    }

    private List<APIName> collectComponentImports(Component comp) {
        List<APIName> all = collectExplicitImports(comp);
        return all;
    }

    private List<APIName> collectComponentExports(Component comp) {
        List<APIName> all = new ArrayList<APIName>();

        // System.err.println("collectComponentDependencies for " + comp);
        // System.err.println("with exports " + comp.getExports());
        for (APIName api : comp.getExports()) {
            // System.err.println("Collecting dependency " + api);
            all.add(api);
        }
        return all;
    }

    private List<APIName> collectComponentComprises(Component comp) {
        List<APIName> all = new ArrayList<APIName>();
        for (APIName constituent : comp.getComprises()) {
            all.add(constituent);
        }
        return all;
    }

    private List<APIName> collectComponentDependencies(Component comp) {
        List<APIName> all = collectComponentImports(comp);
        all.addAll(collectComponentExports(comp));
        all.addAll(collectComponentComprises(comp));

        return all;
    }

    private List<APIName> collectApiImports(Api api) {
        List<APIName> all = collectExplicitImports(api);

        return all;
    }

    private CompilationUnit readCUFor(GraphNode node, String sourceSuffix) throws FileNotFoundException {
        APIName name = node.getName();
        File fdot = findFile(name, sourceSuffix);
        return Parser.importCollector(fdot);
    }

    public String getComponentSourcePath(APIName name) {
        ComponentGraphNode node = (ComponentGraphNode) graph.find(ComponentGraphNode.key(name));

        return node.getSourcePath();
    }

    public PathTaggedApiName pathTaggedComponent(APIName name) {
        return new PathTaggedApiName(getComponentSourcePath(name), name);
    }

}
