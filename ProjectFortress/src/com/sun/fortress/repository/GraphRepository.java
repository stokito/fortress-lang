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

import static com.sun.fortress.interpreter.glue.WellKnownNames.defaultLibrary;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.fortress.Shell;
import com.sun.fortress.compiler.AnalyzeResult;
import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.Parser;
import com.sun.fortress.compiler.Parser.Result;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.exceptions.MultipleStaticError;
import com.sun.fortress.exceptions.ProgramError;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.exceptions.WrappedException;
import com.sun.fortress.interpreter.glue.WellKnownNames;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.AliasedAPIName;
import com.sun.fortress.nodes.AliasedSimpleName;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes.FnDecl;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdOrOp;
import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.nodes.Import;
import com.sun.fortress.nodes.ImportApi;
import com.sun.fortress.nodes.ImportNames;
import com.sun.fortress.nodes.ImportedNames;
import com.sun.fortress.nodes.NodeDepthFirstVisitor_void;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.TraitObjectDecl;
import com.sun.fortress.nodes.TraitTypeWhere;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.repository.graph.ApiGraphNode;
import com.sun.fortress.repository.graph.ComponentGraphNode;
import com.sun.fortress.repository.graph.Graph;
import com.sun.fortress.repository.graph.GraphNode;
import com.sun.fortress.repository.graph.GraphVisitor;
import com.sun.fortress.syntax_abstractions.parser.FortressParser;
import com.sun.fortress.useful.Bijection;
import com.sun.fortress.useful.Debug;
import com.sun.fortress.useful.Fn;
import com.sun.fortress.useful.HashBijection;
import com.sun.fortress.useful.MultiMap;
import com.sun.fortress.useful.Path;
import com.sun.fortress.useful.Useful;
import com.sun.fortress.useful.Debug.Type;

import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.OptionUnwrapException;

/* A graph-based repository. This repository determines the dependency structure
 * before any components/APIs are compiled so that they can be compiled in an
 * efficient and deterministic order.
 *
 * When only compiling a .fss file,
 * the .fss depends on its importing APIs and those APIs depend
 * on the APIs they import.
 * When linking a .fss file, the .fss depends on its importing APIs,
 * and components that implement those APIs are added to the graph.
 *
 * Elements in the graph are stored as Node depends on List<Node>
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

    ForeignJava foreignJava = new ForeignJava();

    public GraphRepository(Path p, CacheBasedRepository cache) throws FileNotFoundException {
        this.path = p;
        this.cache = cache;
        graph = new Graph<GraphNode>();
        addRoots();
    }

    private static String[] roots() {
        /* files that are dependencies of everything */
        return defaultLibrary();
    }

    /* by default all the root APIs should be added to the graph
     * and set as dependencies for everything else.
     */
    private void addRoots() throws FileNotFoundException {
        for ( String root : roots() ){
            APIName name = NodeFactory.makeAPIName(NodeFactory.shellSpan,root);
            ApiGraphNode api = new ApiGraphNode(name, getApiFileDate(name));
            try{
                long cache_date = cache.getModifiedDateForApi(api.getName());
                api.setApi( cache.getApi( api.getName() ), cache_date);
            } catch ( FileNotFoundException e ){
            } catch ( IOException e ){
            }
            graph.addNode( api );
        }

        for ( String root : roots() ) {
            ApiGraphNode node =
                (ApiGraphNode) graph.find(ApiGraphNode.key(NodeFactory.makeAPIName(NodeFactory.shellSpan,root)));
            try{
                for ( APIName api : dependencies(node) ){
                    Debug.debug( Debug.Type.REPOSITORY, 2, "Add edge ", api );
                    graph.addEdge(node, addApiGraph(api));
                }
            } catch ( FileNotFoundException e ){
            } catch ( IOException e ){
            }
        }
    }

    @Override
    public Map<APIName, ApiIndex> apis() {
        return cache.apis();
    }

    @Override
    public Map<APIName, ComponentIndex> components() {
        return cache.components();
    }

    /* getApi and getComponent add an API/component to the graph, find all the
     * dependencies( via addApiGraph/addComponentGraph ) and recompile everything.
     */
    @Override
    public ApiIndex getApi(APIName name) throws FileNotFoundException, IOException, StaticError {
       Debug.debug( Debug.Type.REPOSITORY, 2, "Get API for ", name);
       
        ApiGraphNode node = addApiGraph(name);
        refreshGraph();
        try{
            return node.getApi().unwrap();
        } catch ( OptionUnwrapException o ){
            throw StaticError.make( "Cannot find api " + name + " in the repository. This should not happen, please contact a developer.", "" );
        }
    }

    @Override
    public ComponentIndex getComponent(APIName name) throws FileNotFoundException, IOException, StaticError {
        Debug.debug( Debug.Type.REPOSITORY, 2, "Get component for ", name );
        ComponentGraphNode node = addComponentGraph(name);
        refreshGraph();
        try{
            return node.getComponent().unwrap();
        } catch ( OptionUnwrapException o ){
            throw StaticError.make( "Cannot find component " + name + " in the repository. " +
                                    "This should not happen, please contact a developer.", "" );
        }
    }

    /* add an API node to the graph and return the node. if the API exists in the
     * cache it is loaded, otherwise it will remain empty until it gets
     * recompiled( probably via refreshGraph )
     */
    private ApiGraphNode addApiGraph( APIName name ) throws FileNotFoundException, IOException {
        Debug.debug( Debug.Type.REPOSITORY, 2, "Add API graph ", name );
        ApiGraphNode node = (ApiGraphNode) graph.find(ApiGraphNode.key(name));
        if ( node == null ){
            
            if (foreignJava.definesApi(name)) {
                // TODO not smart about age of native API yet
                // Make the native API be very old, so nothing is out of date;
                needUpdate = true;
                node = new ApiGraphNode(name, Long.MIN_VALUE);
                graph.addNode( node );
                return node;
            }
            
            /* a new node was added, a recompile is needed */
            needUpdate = true;
            node = new ApiGraphNode(name, getApiFileDate(name));
            graph.addNode( node );
            try{
                /* try to load the API from the cache.
                 * if it fails then it will be reloaded later on
                 * in refreshGraph
                 */
                long cache_date = getCacheDate(node);
                if ( cache_date > getApiFileDate(node) ){
                    Debug.debug( Debug.Type.REPOSITORY, 2 , "Found cached version of ", node );
                    node.setApi( cache.getApi(name), cache_date);
                }
            } catch ( FileNotFoundException f ){
                /* oh well */
            } catch ( IOException e ){
            }
            /* make this API depend on the APIs it imports */
            for ( APIName api : dependencies(node) ){
                Debug.debug( Debug.Type.REPOSITORY, 2, "Add edge ", api );
                graph.addEdge(node, addApiGraph(api));
            }
            /* and depend on all the root APIs */
            for ( String root : roots() ){
                graph.addEdge(node, addApiGraph(NodeFactory.makeAPIName(NodeFactory.shellSpan,root)));
            }
        }

        return node;
    }

    /* same thing, but add a component */
    private ComponentGraphNode addComponentGraph( APIName name ) throws FileNotFoundException, IOException, StaticError {
        Debug.debug( Debug.Type.REPOSITORY, 2, "Add component graph ", name );
        ComponentGraphNode node = (ComponentGraphNode) graph.find(ComponentGraphNode.key(name));
        if ( node == null ){
            /* a new node was added, a recompile is needed */
            needUpdate = true;
            node = new ComponentGraphNode(name, getComponentFileDate(name));
            graph.addNode( node );
            try{
                /* try to load the component from the cache.
                 * if it fails then it will be reloaded later on
                 * in refreshGraph
                 */
                long cache_date = getCacheDate(node);
                if ( cache_date > getComponentFileDate(node) ){
                    Debug.debug( Debug.Type.REPOSITORY, 2 , "Found cached version of ", node );
                    node.setComponent( cache.getComponent(name), cache_date);
                }
            } catch ( FileNotFoundException f ){
                /* oh well */
            } catch ( IOException e ){
            }

            /* make this component depend on the APIs it imports */
            for ( APIName api : dependencies(node) ){
                nodeDependsOnApi(node, api);
            }
            /* and depend on all the root APIs */
            for ( String root : roots() ){
                nodeDependsOnApi(node, NodeFactory.makeAPIName(NodeFactory.shellSpan,root));
            }
        }

        return node;
    }

    private void nodeDependsOnApi(ComponentGraphNode node, APIName api)
            throws FileNotFoundException, IOException {
        Debug.debug( Debug.Type.REPOSITORY, 2, "Add edge ", api );
        graph.addEdge(node, addApiGraph(api));
        boolean b = foreignJava.definesApi(api);
        // System.err.println("b="+b);
        if ( link && ! b){
            Debug.debug( Debug.Type.REPOSITORY, 1, "Component ", node.getName(), " depends on API ", api );
            // Add element, but no API
            addComponentGraph(api);
        }
    }

    private long getCacheDate( ApiGraphNode node ){
        try{
            return cache.getModifiedDateForApi(node.getName());
        } catch ( FileNotFoundException e ){
            return Long.MIN_VALUE;
        }
    }

    private long getCacheDate( ComponentGraphNode node ){
        try{
            return cache.getModifiedDateForComponent(node.getName());
        } catch ( FileNotFoundException e ){
            return Long.MIN_VALUE;
        }
    }

    private long getCacheDate( GraphNode node ){
        try{
            return node.accept( new CacheVisitor() );
        } catch ( FileNotFoundException e ){
            return Long.MIN_VALUE;
        }
    }

    private long getComponentFileDate( ComponentGraphNode node ) throws FileNotFoundException {
        return node.getSourceDate();
    }

    private long getComponentFileDate( APIName name ) throws FileNotFoundException {
        return findFile( name, ProjectProperties.COMP_SOURCE_SUFFIX ).lastModified();
    }

    private long getApiFileDate( ApiGraphNode node ) throws FileNotFoundException {
        return node.getSourceDate();
    }

    private long getApiFileDate( APIName name ) throws FileNotFoundException {
        return findFile( name, ProjectProperties.API_SOURCE_SUFFIX ).lastModified();
    }

    public File findFile(APIName name, String suffix) throws FileNotFoundException {
        String dotted = name.toString();
        String slashed = dotted.replaceAll("[.]", "/");
        slashed = slashed + "." + suffix;
        File fdot;

        Debug.debug( Debug.Type.REPOSITORY, 3, "Finding file ", name);
        try {
            fdot = path.findFile(slashed);
        } catch (FileNotFoundException ex2) {
            throw new FileNotFoundException(NodeUtil.getSpan(name) +
                                            "\n    Could not find API " + dotted +
                                            " in file named " + slashed +
                                            " on path\n    " + path);
        }
        return fdot;
    }

    private class CacheVisitor implements GraphVisitor<Long, FileNotFoundException>{
        public Long visit(ApiGraphNode node) throws FileNotFoundException {
            return getCacheDate(node);
        }

        public Long visit(ComponentGraphNode node) throws FileNotFoundException {
            return getCacheDate(node);
        }
    }

    /* what if the file has been edited to include import statements that the cached
     * version doesn't have? that's ok because the cached version won't be loaded unless it
     * is newer than the file on disk.
     */
    private List<APIName> dependencies(ApiGraphNode node) throws FileNotFoundException, StaticError {
        CompilationUnit cu = node.getApi().isSome() ?
                node.getApi().unwrap().ast() :
                    readCUFor(node, ProjectProperties.API_SOURCE_SUFFIX);

        return collectApiImports((Api)cu);

    }

    private List<APIName> dependencies(ComponentGraphNode node) throws FileNotFoundException, StaticError {
        CompilationUnit cu = node.getComponent().isSome() ?
                node.getComponent().unwrap().ast() :
                    readCUFor(node, ProjectProperties.COMP_SOURCE_SUFFIX);

        return collectComponentImports((Component)cu);
    }

    private boolean inApiList( APIName name, List<ApiGraphNode> nodes ){
        for ( ApiGraphNode node : nodes ){
            if ( node.getName().equals( name ) ){
                return true;
            }
        }
        return false;
    }

    private boolean inComponentList( APIName name, List<ComponentGraphNode> nodes ){
        for ( ComponentGraphNode node : nodes ){
            if ( node.getName().equals( name ) ){
                return true;
            }
        }
        return false;
    }

    /* reparse anything that is out of date */
    private AnalyzeResult refreshGraph() throws FileNotFoundException, IOException, StaticError {
        AnalyzeResult result =
            new AnalyzeResult(IterUtil.<StaticError>empty());
        if ( needUpdate ){
            needUpdate = false;
            OutOfDateVisitor date = new OutOfDateVisitor();
            for ( GraphNode node : graph.nodes() ){
                node.accept( date );
            }
            List<ApiGraphNode> reparseApis = date.apis();
            List<ComponentGraphNode> reparseComponents = sortComponents(date.components());
            Debug.debug( Debug.Type.REPOSITORY, 1, "Out of date APIs ", reparseApis );
            Debug.debug( Debug.Type.REPOSITORY, 1, "Out of date components ", reparseComponents );
            /* these can be parsed all at once */
            result = parseApis(date.outOfDateApi());
            for ( Map.Entry<APIName, ApiIndex> entry : result.apis().entrySet() ){
                if ( inApiList(entry.getKey(), reparseApis) ){
                    addApi( entry.getKey(), entry.getValue() );
                }
            }

            /* but these have to be done in a specific order due to
             * syntax expansion requiring some components, like
             * FortressBuiltin, being parsed.
             * If syntax expansion was unable to execute code then all
             * the APIs and components could be parsed at the same time.
             */
            for ( ComponentGraphNode node : reparseComponents ){
                /* parseComponent will call Fortress.analyze which
                 * will call repository.add(component). That add() will
                 * call setComponent() on this node with the same component
                 * that parseComponent is going to return.
                 */
                result = parseComponent(syntaxExpand(node));
                for ( Map.Entry<APIName, ComponentIndex> entry : result.components().entrySet() ){
                    if ( inComponentList( entry.getKey(), reparseComponents ) ){
                        addComponent( entry.getKey(), entry.getValue() );
                    }
                }
            }
        }
        return result;
    }

    private class OutOfDateVisitor implements GraphVisitor<Boolean,FileNotFoundException>{
        private Map<GraphNode, Long> youngestSourceDependedOn;
        private Map<GraphNode, Boolean> staleOrDependsOnStale;

        public OutOfDateVisitor(){
            youngestSourceDependedOn = new HashMap<GraphNode,Long>();
            staleOrDependsOnStale = new HashMap<GraphNode,Boolean>();
        }

        Fn<GraphNode,Boolean> outOfDateApi() {
            return new Fn<GraphNode,Boolean>(){
                @Override
                public Boolean apply(GraphNode g){
                    return g instanceof ApiGraphNode && staleOrDependsOnStale.get(g);
                }
            };
        }

        Fn<GraphNode,Boolean> outOfDateComponent() {
            return new Fn<GraphNode,Boolean>(){
                @Override
                public Boolean apply(GraphNode g){
                    return g instanceof ComponentGraphNode && staleOrDependsOnStale.get(g);
                }
            };
        }

        public List<ApiGraphNode> apis(){
            return Useful.convertList(Useful.filter(youngestSourceDependedOn.keySet(), outOfDateApi()));
        }
        /* returns out of date components */
        public List<ComponentGraphNode> components(){
            return Useful.convertList(Useful.filter(youngestSourceDependedOn.keySet(), outOfDateComponent()));
        }

        private Long handle( GraphNode node ) throws FileNotFoundException {
            if ( youngestSourceDependedOn.containsKey(node) ){
                return youngestSourceDependedOn.get(node);
            }
            long youngest = node.getSourceDate();
            youngestSourceDependedOn.put(node, youngest);

            List<GraphNode> depends = graph.depends(node);
            Debug.debug( Debug.Type.REPOSITORY, 2, node + " depends on " + depends );
            for ( GraphNode next : depends ){
                long dependent_youngest = handle(next);
                if (dependent_youngest > youngest) {

                    Debug.debug( Debug.Type.REPOSITORY, 1, next + " has younger source than " + next );
                    youngest = dependent_youngest;
                }
            }

            youngestSourceDependedOn.put(node, youngest);

            return youngest;
        }

        private Boolean isStale( GraphNode node ) throws FileNotFoundException {
            if ( staleOrDependsOnStale.containsKey(node) ){
                return staleOrDependsOnStale.get(node);
            }

            boolean stale = youngestSourceDependedOn.get(node) > getCacheDate(node);

            // If anything depended on has source that is younger than our compiled code,
            // then this is stale.
            if ( stale ){
                Debug.debug( Debug.Type.REPOSITORY, 1, node + "or dependent is newer " + youngestSourceDependedOn.get(node) + " than the cache " + getCacheDate(node) );
            }

            staleOrDependsOnStale.put(node, stale);

            List<GraphNode> depends = graph.depends(node);
            Debug.debug( Debug.Type.REPOSITORY, 2, node + " depends on " + depends );
            for ( GraphNode next : depends ){
                boolean dependent_stale = isStale(next);

                if ( dependent_stale  ){
                    stale = true;
                    Debug.debug( Debug.Type.REPOSITORY, 1, node + " is stale " + next + " is stale" );
                    staleOrDependsOnStale.put(node, stale);
                }
            }
            return stale;
        }

        public Boolean visit( ApiGraphNode node ) throws FileNotFoundException {
            handle(node);
            return isStale( node );
        }

        public Boolean visit( ComponentGraphNode node ) throws FileNotFoundException {
            handle(node);
            return isStale( node );
        }
    }

    private List<ComponentGraphNode> sortComponents(List<ComponentGraphNode> nodes) throws FileNotFoundException {
        Graph<GraphNode> componentGraph = new Graph<GraphNode>( graph, new Fn<GraphNode, Boolean>(){
                @Override
                public Boolean apply(GraphNode g){
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

        if ( Debug.isOnFor(2, Debug.Type.REPOSITORY) ){
            Debug.debug( Debug.Type.REPOSITORY, 2, componentGraph.getDebugString() );
        }

        List<GraphNode> sorted = componentGraph.sorted();
        List<ComponentGraphNode> rest = new ArrayList<ComponentGraphNode>();

        for ( GraphNode node : sorted ){
            ComponentGraphNode comp = (ComponentGraphNode) node;
            if ( nodes.contains( comp ) ){
                /* force root components to come in front of other things */
                if ( Arrays.asList(roots()).contains(comp.getName().toString()) ){
                    rest.add( 0, comp );
                } else {
                    rest.add( comp );
                }
            }
        }

        return rest;
    }

    private AnalyzeResult parseApis(final Fn<GraphNode, Boolean> these_apis){

        List<Api> unparsed = Useful.applyToAll(graph.filter(these_apis),
            new Fn<GraphNode, Api>(){
                @Override
                public Api apply(GraphNode g){
                    return parseApi((ApiGraphNode) g);
                }
            });

        if (unparsed.size() == 0)
            return new AnalyzeResult(IterUtil.<StaticError>empty());

        GlobalEnvironment knownApis = new GlobalEnvironment.FromMap(parsedApis());

        // Can we exclude non-imported pieces of the api here?


        List<Component> components = new ArrayList<Component>();
        Shell shell = new Shell(this);
        AnalyzeResult result =
            Shell.analyze(shell.getRepository(),
                          knownApis, unparsed, components, System.currentTimeMillis() );
        if ( !result.isSuccessful() ){
            throw new MultipleStaticError(result.errors());
        }
        return result;
    }

    /* return a parsed API */
    private Api parseApi( ApiGraphNode node ){
        try{
            APIName api_name = node.getName();
            File fdot = findFile(api_name, ProjectProperties.API_SOURCE_SUFFIX);
            CompilationUnit api = Parser.parseFileConvertExn(fdot);
            if (api instanceof Api) {
                return (Api) api;
            } else {
                throw StaticError.make("Unexpected parse of API " + api_name, "");
            }
        } catch ( FileNotFoundException e ){
            throw new WrappedException(e);
        } catch ( IOException e ){
            throw new WrappedException(e);
        }
    }


    /* find all parsed APIs */
    public Map<APIName, ApiIndex> parsedApis(){
        
        Map<APIName, ApiIndex> apis = new HashMap<APIName, ApiIndex>();
        
        for ( GraphNode g :  graph.nodes()){
            if (g instanceof ApiGraphNode) {
                ApiGraphNode node = (ApiGraphNode) g;
                if (node.getApi().isSome()) {
                    apis.put( node.getName(), node.getApi().unwrap() );
                } else if (foreignJava.definesApi(node.getName())) {
                    apis.put(node.getName(), foreignJava.fakeApi(node.getName()));
                }
            }
        }
        return apis;
    }

    /* parse a single component. */
    private AnalyzeResult parseComponent( Component component ) throws StaticError {
        GlobalEnvironment knownApis = new GlobalEnvironment.FromMap(parsedApis());
        List<Component> components = new ArrayList<Component>();
        components.add(component);
        long now = System.currentTimeMillis();
        Debug.debug( Debug.Type.REPOSITORY, 1, "Parsing ", component, " at ", now );

        Shell shell = new Shell(this);
        AnalyzeResult result =
            Shell.analyze(shell.getRepository(),
                          knownApis, new ArrayList<Api>(), components, now );
        if ( !result.isSuccessful() ){
            throw new MultipleStaticError(result.errors());
        }
        return result;
    }

    /* parse a component and run it through syntax expansion, may
     * invoke code( transformer expressions )
     */
    private Component syntaxExpand( ComponentGraphNode node ) throws FileNotFoundException, IOException {
        Debug.debug( Debug.Type.REPOSITORY, 1, "Expand component ", node );

        APIName api_name = node.getName();
        File file = findFile(api_name, ProjectProperties.COMP_SOURCE_SUFFIX);
        GraphRepository g1 = new GraphRepository( this.path, this.cache );
        /* FIXME: hack to prevent infinite recursion */
        Shell.setCurrentInterpreterRepository( g1 );
        Result result = FortressParser.parse(api_name, file, new GlobalEnvironment.FromRepository( g1 ), verbose());
        // Result result = FortressParser.parse(file, new GlobalEnvironment.FromRepository(this), verbose());
        /* FIXME: hack to prevent infinite recursion */
        Shell.setCurrentInterpreterRepository( this );
        if (result.isSuccessful()) {
            Debug.debug( Debug.Type.REPOSITORY, 1, "Expanded component ", node );
            Iterator<Component> components = result.components().iterator();
            if (components.hasNext()) return components.next();
            throw new ProgramError("Successful parse result was nonetheless empty, file " + file.getCanonicalPath());
        }
        throw new ProgramError(result.errors());
    }

    /* add an API to the repository and cache it */
    @Override
    public void addApi(APIName name, ApiIndex definition) {
        ApiGraphNode node = (ApiGraphNode) graph.find(ApiGraphNode.key(name));
        if ( node == null ){
            throw new RuntimeException("No such API '" + name + "'");
        } else {
            node.setApi(definition, definition.modifiedDate());
            cache.addApi(name, definition);
        }
    }

    /* add a component to the repository and cache it */
    @Override
    public void addComponent(APIName name, ComponentIndex definition){
        ComponentGraphNode node = (ComponentGraphNode) graph.find(ComponentGraphNode.key(name));
        if (node == null ){
            throw new RuntimeException("No such component " + name);
        } else {
            node.setComponent(definition, definition.modifiedDate());
            cache.addComponent(name, definition);
        }
    }

    @Override
    public void deleteComponent(APIName name) {
        ComponentGraphNode node = (ComponentGraphNode) graph.find(ComponentGraphNode.key(name));
        if ( node != null ){
            cache.deleteComponent(name);
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

    private void addRootComponents() throws FileNotFoundException, IOException{
        boolean added = false;
        for ( String root : roots() ){
            APIName name = NodeFactory.makeAPIName(NodeFactory.shellSpan,root);
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
    public long getModifiedDateForApi(APIName name)
        throws FileNotFoundException {
        return 0;
    }

    @Override
    public long getModifiedDateForComponent(APIName name)
        throws FileNotFoundException {
        return 0;
    }

    @Override
    public void clear() {
        cache.clear();
    }

    private List<APIName> collectExplicitImports(CompilationUnit comp) {
        List<APIName> all = new ArrayList<APIName>();
        
        for (Import i : comp.getImports()){
            Option<String> opt_fl = i.getForeignLanguage();
            boolean isNative = opt_fl.isSome();
            if (isNative && (i instanceof ImportNames)) {
                String fl = opt_fl.unwrap();
                // Conditional overlap with later clause.
                // Handle import of foreign names here.
                // Ought to handle this case by case.
                ImportNames ins = (ImportNames) i;
                if("java".equalsIgnoreCase(fl)) {
                    /*
                     *  Don't create the API yet; its contents depend on all
                     *  the imports.
                     */
                    foreignJava.processJavaImport(i, ins);
                    
                    // depend on the API name; 
                    // "compilation"/"reading" will get the API
                    all.add( ins.getApiName() );
                    continue;
                } else if ("fortress".equalsIgnoreCase(fl)) {
                    // do nothing, fall into normal case
                } else {
                    throw StaticError.make("Foreign language "+ fl + " not yet handled ", i);
                }
            }

            if (i instanceof ImportedNames) {
                ImportedNames names = (ImportedNames) i;
                all.add( names.getApiName() );
            } else { // i instanceof ImportApi
                ImportApi apis = (ImportApi) i;
                for (AliasedAPIName a : apis.getApis()) {
                    all.add(a.getApiName());
                }
            }
        }
        return all;
    }

    private  List<APIName> collectComponentImports(Component comp) {
         List<APIName> all =  collectExplicitImports(comp);

         for (APIName api : comp.getExports()) {
             all.add(api);
         }
         return removeExecutableApi(all);
     }

    private  List<APIName> collectApiImports(Api api) {
        List<APIName> all =  collectExplicitImports(api);

        return removeExecutableApi(all);
    }

    private CompilationUnit readCUFor(GraphNode node, String sourceSuffix) throws FileNotFoundException {
        APIName name = node.getName();
        File fdot = findFile(name, sourceSuffix);
        return Parser.preparseFileConvertExn(fdot);
    }

    private static List<APIName> removeExecutableApi(List<APIName> all){
        List<APIName> fixed = new ArrayList<APIName>();
        for (APIName name : all){
            if (! WellKnownNames.exportsMain(name.getText())) {
                fixed.add(name);
            }
        }
        return fixed;
    }

}
