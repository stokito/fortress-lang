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

import java.io.File;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Collection;

import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.useful.Path;
import com.sun.fortress.useful.Fn;
import com.sun.fortress.useful.Useful;
import com.sun.fortress.compiler.index.CompilationUnitIndex;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.Parser.Result;
import com.sun.fortress.compiler.Parser;
import com.sun.fortress.compiler.Fortress;
import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.Shell;
import com.sun.fortress.exceptions.ParserError;
import com.sun.fortress.exceptions.ProgramError;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.exceptions.WrappedException;
import com.sun.fortress.exceptions.MultipleStaticError;
import com.sun.fortress.syntax_abstractions.parser.FortressParser;
import com.sun.fortress.syntax_abstractions.parser.PreParser;
import com.sun.fortress.useful.Debug;

import xtc.parser.SemanticValue;
import xtc.parser.ParseError;

import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;

import com.sun.fortress.repository.graph.Graph;
import com.sun.fortress.repository.graph.ComponentGraphNode;
import com.sun.fortress.repository.graph.ApiGraphNode;
import com.sun.fortress.repository.graph.GraphNode;
import com.sun.fortress.repository.graph.GraphVisitor;

import static com.sun.fortress.interpreter.glue.WellKnownNames.*;

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

    /* files that are dependancies of everything */
    private static final String[] roots = defaultLibrary;

    /* stores the nodes and their relationships */
    private Graph<GraphNode> graph;
    /* current source path */
    private Path path;
    /* underlying cache of compiled files */
    private FortressRepository cache;
    private GlobalEnvironment env;
    /* true if a recompile is needed */
    private boolean needUpdate = true;
    /* If link is true then pull in a component for an API */
    private boolean link = false;
    public GraphRepository(Path p, FortressRepository cache) {
        this.path = p;
        this.cache = cache;
        graph = new Graph<GraphNode>();
        env = new GlobalEnvironment.FromRepository(this);

        addRoots();
    }

    /* by default all the root APIs should be added to the graph
     * and set as dependancies for everything else.
     */
    private void addRoots(){
        for ( String root : roots ){
            ApiGraphNode api = new ApiGraphNode(NodeFactory.makeAPIName(root));
            try{
                api.setApi( cache.getApi( api.getName() ) );
            } catch ( FileNotFoundException e ){
            } catch ( IOException e ){
            }
            graph.addNode( api );
        }
    }

    public Map<APIName, ApiIndex> apis() {
        return cache.apis();
    }

    /* getApi and getComponent add an API/component to the graph, find all the
     * dependancies( via addApiGraph/addComponentGraph ) and recompile everything.
     */
    public ApiIndex getApi(APIName name) throws FileNotFoundException, IOException, StaticError {
        ApiGraphNode node = addApiGraph(name);
        Debug.debug( 2, "Get API for " + name);
        refreshGraph();
        return node.getApi().unwrap();
    }

    public ComponentIndex getComponent(APIName name) throws FileNotFoundException, IOException, StaticError {
        ComponentGraphNode node = addComponentGraph(name);
        Debug.debug( 2, "Get component for " + name );
        refreshGraph();
        return node.getComponent().unwrap();
    }

    /* add an API node to the graph and return the node. if the API exists in the
     * cache it is loaded, otherwise it will remain empty until it gets
     * recompiled( probably via refreshGraph )
     */
    private ApiGraphNode addApiGraph( APIName name ) throws FileNotFoundException, IOException {
        Debug.debug( 2, "Add API graph " + name );
        ApiGraphNode node = new ApiGraphNode( name );
        if ( ! graph.contains( node ) ){
            /* a new node was added, a recompile is needed */
            needUpdate = true;
            graph.addNode( node );
            try{
                /* try to load the API from the cache.
                 * if it fails then it will be reloaded later on
                 * in refreshGraph
                 */
                if ( getCacheDate(node) > getFileDate(node) ){
                    Debug.debug( 2 , "Found cached version of " + node );
                    node.setApi( cache.getApi(name) );
                }
            } catch ( FileNotFoundException f ){
                /* oh well */
            } catch ( IOException e ){
            }
            /* make this API depend on the APIs it imports */
            for ( APIName api : dependencies(node) ){
                Debug.debug( 2, "Add edge " + api );
                graph.addEdge(node, addApiGraph(api));
            }
            /* and depend on all the root APIs */
            for ( String root : roots ){
                graph.addEdge(node, addApiGraph(NodeFactory.makeAPIName(root)));
            }
        } else {
            node = (ApiGraphNode) graph.find( node );
        }

        if ( link ){
            Debug.debug( 1, "Add component for API " + name );
            graph.addEdge(addComponentGraph(name), node);
        }

        return node;
    }

    /* same thing, but add a component */
    private ComponentGraphNode addComponentGraph( APIName name ) throws FileNotFoundException, IOException, StaticError {
        Debug.debug( 2, "Add component graph " + name );
        ComponentGraphNode node = new ComponentGraphNode( name );
        if ( ! graph.contains( node ) ){
            /* a new node was added, a recompile is needed */
            needUpdate = true;
            graph.addNode( node );
            try{
                /* try to load the component from the cache.
                 * if it fails then it will be reloaded later on
                 * in refreshGraph
                 */
                if ( getCacheDate(node) > getFileDate(node) ){
                    Debug.debug( 2 , "Found cached version of " + node );
                    node.setComponent( cache.getComponent(name) );
                }
            } catch ( FileNotFoundException f ){
                /* oh well */
            } catch ( IOException e ){
            }
            /* make this component depend on the APIs it imports */
            for ( APIName api : dependencies(node) ){
                Debug.debug( 2, "Add edge " + api );
                graph.addEdge(node, addApiGraph(api));
            }
            /* and depend on all the root APIs */
            for ( String root : roots ){
                graph.addEdge(node, addApiGraph(NodeFactory.makeAPIName(root)));
            }
        } else {
            node = (ComponentGraphNode) graph.find( node );
        }

        return node;
    }

    private long getCacheDate( ApiGraphNode node ){
        try{
            return cache.getModifiedDateForApi(node.getName());
        } catch ( FileNotFoundException e ){
            return 0;
        }
    }

    private long getCacheDate( ComponentGraphNode node ){
        try{
            return cache.getModifiedDateForComponent(node.getName());
        } catch ( FileNotFoundException e ){
            return 0;
        }
    }

    private long getCacheDate( GraphNode node ){
        try{
            return node.accept( new CacheVisitor() );
        } catch ( FileNotFoundException e ){
            return 0;
        }
    }

    private long getFileDate( ComponentGraphNode node ) throws FileNotFoundException {
        return findFile( node.getName(), ProjectProperties.COMP_SOURCE_SUFFIX ).lastModified();
    }

    private long getFileDate( ApiGraphNode node ) throws FileNotFoundException {
        return findFile( node.getName(), ProjectProperties.API_SOURCE_SUFFIX ).lastModified();
    }

    private File findFile(APIName name, String suffix) throws FileNotFoundException {
        String dotted = name.toString();
        String slashed = dotted.replaceAll("[.]", "/");
        dotted = dotted + "." + suffix;
        slashed = slashed + "." + suffix;
        File fdot;

        Debug.debug(3, "Finding file " + name );
        try {
            fdot = path.findFile(slashed);
        } catch (FileNotFoundException ex2) {
            throw new FileNotFoundException("Could not find " + dotted + " on path " + path);
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

    private List<APIName> dependencies(ApiGraphNode node) throws FileNotFoundException, StaticError {
        if ( node.getApi().isSome() ){
            return PreParser.collectApiImports((Api)node.getApi().unwrap().ast());
        } else {
            File fdot = findFile(node.getName(), ProjectProperties.API_SOURCE_SUFFIX);
            return PreParser.getImportedApis(node.getName(), fdot);
        }
    }

    private List<APIName> dependencies(ComponentGraphNode node) throws FileNotFoundException, StaticError {
        if ( node.getComponent().isSome() ){
            return PreParser.collectComponentImports((Component)node.getComponent().unwrap().ast());
        } else {
            File fdot = findFile(node.getName(), ProjectProperties.COMP_SOURCE_SUFFIX);
            return PreParser.getImportedApis(node.getName(), fdot);
        }
    }

    /**************************************************************************************************************/
    /* reparse anything that is out of date */
    private void refreshGraph() throws FileNotFoundException, IOException, StaticError {
        Iterable<? extends StaticError> errors = IterUtil.empty();
        if ( needUpdate ){
            needUpdate = false;
            OutOfDateVisitor date = new OutOfDateVisitor();
            for ( GraphNode node : graph.nodes() ){
                node.accept( date );
            }
            List<ApiGraphNode> reparseApis = date.apis();
            List<ComponentGraphNode> reparseComponents = sortComponents(date.components());
            Debug.debug( 1, "Out of date APIs " + reparseApis );
            Debug.debug( 1, "Out of date components " + reparseComponents );
            /* these can be parsed all at once */
            parseApis(reparseApis);

            /* but these have to be done in a specific order due to
             * syntax expansion requiring some components, like
             * FortressBuiltin, being parsed.
             * If syntax expansion was unable to execute code then all
             * the APIs and components could be parsed at the same time.
             */
            for ( ComponentGraphNode node : reparseComponents ){
                /* setComponent here is actually redundant because
                 * parseComponent will call Fortress.analyze which
                 * will call repository.add(component). That add() will
                 * call setComponent() on this node with the same component
                 * that parseComponent is going to return.
                 */ // ????????????????????????????????????????
                node.setComponent(parseComponent(syntaxExpand(node)));
            }
        }
    }

    private class OutOfDateVisitor implements GraphVisitor<Boolean,FileNotFoundException>{
        private Map<GraphNode,Boolean> seen;
        public OutOfDateVisitor(){
            seen = new HashMap<GraphNode,Boolean>();
        }

        /* returns out of date APIs */
        public List<ApiGraphNode> apis(){
            return Useful.convertList(Useful.filter(seen.keySet(), new Fn<GraphNode,Boolean>(){
			public Boolean apply(GraphNode g){
                            return g instanceof ApiGraphNode && seen.get(g);
			}
                    }));
        }

        /* returns out of date components */
        public List<ComponentGraphNode> components(){
            return Useful.convertList(Useful.filter(seen.keySet(), new Fn<GraphNode,Boolean>(){
			public Boolean apply(GraphNode g){
                            return g instanceof ComponentGraphNode && seen.get(g);
			}
                    }));
        }

        private Boolean handle( GraphNode node, File source ) throws FileNotFoundException {
            if ( seen.containsKey(node) ){
                return seen.get(node);
            }
            seen.put(node,true);
            if ( source.lastModified() > getCacheDate(node) ){
                Debug.debug( 2, node + " is newer " + source.lastModified() + " than the cache " + getCacheDate(node) );
                seen.put(node,true);
                return true;
            }
            List<GraphNode> depends = graph.depends(node);
            Debug.debug( 2, node + " depends on " + depends );
            for ( GraphNode next : depends ){
                if ( next.accept( this ) || getCacheDate(next) > getCacheDate(node) ){
                    Debug.debug( 2, node + " is out of date because " + next + " is out of date" );
                    seen.put(node,true);
                    return true;
                }
            }
            seen.put(node,false);
            return false;
        }

        public Boolean visit( ApiGraphNode node ) throws FileNotFoundException {
            return handle( node, findFile(node.getName(), ProjectProperties.API_SOURCE_SUFFIX) );
        }

        public Boolean visit( ComponentGraphNode node ) throws FileNotFoundException {
            return handle( node, findFile(node.getName(), ProjectProperties.COMP_SOURCE_SUFFIX) );
        }
    }

    private List<ComponentGraphNode> sortComponents(List<ComponentGraphNode> nodes) throws FileNotFoundException {
        Graph<GraphNode> componentGraph = new Graph<GraphNode>( graph, new Fn<GraphNode, Boolean>(){
                public Boolean apply(GraphNode g){
                    return g instanceof ComponentGraphNode;
                }
            });

        /* force components that import things to depend on the component
         * that implements that import. This is for syntax abstraction
         * update 6/19/2008: this would prevent seperate compilation, so
         * don't set things up this way.
         */
        /*
        for ( GraphNode node : componentGraph.nodes() ){
            ComponentGraphNode comp = (ComponentGraphNode) node;
            for ( GraphNode dependancy : graph.dependancies(comp) ){
                if ( dependancy instanceof ApiGraphNode ){
                    ComponentGraphNode next = (ComponentGraphNode) componentGraph.find( new ComponentGraphNode( ((ApiGraphNode) dependancy).getName() ) );
                    // ComponentGraphNode next = new ComponentGraphNode( ((ApiGraphNode) dependancy).getName() );
                    if ( ! next.equals( comp ) ){
                        componentGraph.addEdge( comp, next );
                    }
                }
            }
        }
        */

        if ( Debug.getDebug() >= 2 ){
            componentGraph.dump();
        }

        List<GraphNode> sorted = componentGraph.sorted();
        List<ComponentGraphNode> rest = new ArrayList<ComponentGraphNode>();

        for ( GraphNode node : sorted ){
            ComponentGraphNode comp = (ComponentGraphNode) node;
            if ( nodes.contains( comp ) ){
                /* force root components to come in front of other things */
                if ( Arrays.asList(roots).contains(comp.getName().toString()) ){
                    rest.add( 0, comp );
                } else {
                    rest.add( comp );
                }
            }
        }

        return rest;
    }

    private void parseApis( List<ApiGraphNode> apis ){
        for ( ApiGraphNode node : apis ){
            /* yes, set the API to nothing so that it gets
             * reparsed no matter what
             */
            node.setApi(null);
        }
        if ( apis.size() > 0 ){
            parseApis();
        }
    }

    private void parseApis(){
        List<Api> unparsed = Useful.applyToAll(graph.filter(new Fn<GraphNode,Boolean>(){
                    @Override
                    public Boolean apply(GraphNode g){
                        if ( g instanceof ApiGraphNode ){
                            ApiGraphNode a = (ApiGraphNode) g;
                            return ! a.getApi().isSome();
                        }
                        return false;
                    }
		}),
            new Fn<GraphNode, Api>(){
                @Override
                public Api apply(GraphNode g){
                    return parseApi((ApiGraphNode) g);
                }
            });
        /***/
		GlobalEnvironment knownApis = new GlobalEnvironment.FromMap(parsedApis());
		List<Component> components = new ArrayList<Component>();
		Shell shell = new Shell(this);
		Iterable<? extends StaticError> errors = Fortress.analyze(shell.getRepository(),
                                                                          knownApis, unparsed, components, System.currentTimeMillis() );
                if ( errors.iterator().hasNext() ){
                    throw new MultipleStaticError(errors);
                }
                /***/
    }

    /* return a parsed API */
    private Api parseApi( ApiGraphNode node ){
        try{
            APIName api_name = node.getName();
            File fdot = findFile(api_name, ProjectProperties.API_SOURCE_SUFFIX);
            CompilationUnit api = Parser.parseFile(api_name, fdot);
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
    private Map<APIName, ApiIndex> parsedApis(){
        List<GraphNode> all = graph.filter(new Fn<GraphNode, Boolean>(){
                @Override
                public Boolean apply(GraphNode g){
                    if ( g instanceof ApiGraphNode ){
                        ApiGraphNode a = (ApiGraphNode) g;
                        return a.getApi().isSome();
                    }
                    return false;
                }
            });
        Map<APIName, ApiIndex> apis = new HashMap<APIName, ApiIndex>();
        for ( GraphNode g : all ){
            ApiGraphNode node = (ApiGraphNode) g;
            apis.put( node.getName(), node.getApi().unwrap() );
        }
        return apis;
    }

    /* parse a single component. */
    private ComponentIndex parseComponent( Component component ) throws StaticError {
        GlobalEnvironment knownApis = new GlobalEnvironment.FromMap(parsedApis());
        List<Component> components = new ArrayList<Component>();
        components.add(component);
        long now = System.currentTimeMillis();
        Debug.debug( 1, "Parsing " + component + " at " + now );

            Shell shell = new Shell(this);
            /* fort.analyze will call repository.add() with the parsed component,
                 * where that repository is 'this'. After that add the component node
                 * in the graph will contain something and can be unwrap()'d at the end.
                 */
        /***/
            Iterable<? extends StaticError> errors = Fortress.analyze(shell.getRepository(),
                                                                      knownApis, new ArrayList<Api>(), components, now );
        if ( errors.iterator().hasNext() ){
                    throw new MultipleStaticError(errors);
        }
        Debug.debug( 1, "No errors for " + component );
        /***/

        ComponentGraphNode node = (ComponentGraphNode) graph.find( new ComponentGraphNode(component.getName()) );
        return node.getComponent().unwrap();
    }

    /* parse a component and run it through syntax expansion, may
     * invoke code( transformer expressions )
     */
    private Component syntaxExpand( ComponentGraphNode node ) throws FileNotFoundException, IOException {
        Debug.debug( 1, "Expand component " + node );

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
            Debug.debug( 1, "Expanded component " + node );
            Iterator<Component> components = result.components().iterator();
            if (components.hasNext()) return components.next();
            throw new ProgramError("Successful parse result was nonetheless empty, file " + file.getCanonicalPath());
        }
        throw new ProgramError(result.errors());
    }

    /* add a parsed API to the repository */
    public void addApi(APIName name, ApiIndex definition) {
        ApiGraphNode graphNode = new ApiGraphNode(name);
        if ( ! graph.contains(graphNode) ){
            throw new RuntimeException("No such API '" + name + "'");
        } else {
            ApiGraphNode node = (ApiGraphNode) graph.find(graphNode);
            node.setApi(definition);
            cache.addApi(name, definition);
        }
    }

    /* add a parsed component to the repository */
    public void addComponent(APIName name, ComponentIndex definition){
        ComponentGraphNode graphNode = new ComponentGraphNode(name);
        if ( ! graph.contains(graphNode) ){
            throw new RuntimeException("No such component " + name);
        } else {
            ComponentGraphNode node = (ComponentGraphNode) graph.find(graphNode);
            node.setComponent(definition);
            cache.addComponent(name, definition);
        }
    }

    public ComponentIndex getLinkedComponent(APIName name) throws FileNotFoundException, IOException {
        link = true;
        addRootComponents();
        ComponentIndex node = getComponent(name);
        link = false;
        return node;
    }

    private void addRootComponents(){
        for ( String root : roots ){
            APIName name = NodeFactory.makeAPIName(root);
            ApiGraphNode node = (ApiGraphNode) graph.find(new ApiGraphNode(name));
            ComponentGraphNode comp = new ComponentGraphNode(name);
            try{
                comp.setComponent( cache.getComponent( comp.getName() ) );
            } catch ( FileNotFoundException e ){
            } catch ( IOException e ){
            }
            graph.addNode( comp );
            graph.addEdge( comp, node );
            needUpdate = true;
        }
    }

    public long getModifiedDateForApi(APIName name)
        throws FileNotFoundException {
        return 0;
    }

    public long getModifiedDateForComponent(APIName name)
        throws FileNotFoundException {
        return 0;
    }
}
