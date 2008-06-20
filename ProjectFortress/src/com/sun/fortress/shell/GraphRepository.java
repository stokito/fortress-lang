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

package com.sun.fortress.shell;

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
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.FortressRepository;
import com.sun.fortress.compiler.IndexBuilder;
import com.sun.fortress.compiler.Parser.Result;
import com.sun.fortress.compiler.Parser;
import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.Fortress;
import com.sun.fortress.exceptions.ParserError;
import com.sun.fortress.exceptions.ProgramError;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.interpreter.drivers.ProjectProperties;
import com.sun.fortress.interpreter.drivers.Driver;
import com.sun.fortress.syntax_abstractions.parser.FortressParser;
import com.sun.fortress.useful.Debug;

import xtc.parser.SemanticValue;
import xtc.parser.ParseError;

import edu.rice.cs.plt.tuple.Option;

import com.sun.fortress.shell.graph.Graph;
import com.sun.fortress.shell.graph.ComponentGraphNode;
import com.sun.fortress.shell.graph.ApiGraphNode;
import com.sun.fortress.shell.graph.GraphNode;

/* A graph-based repository. This repository determines the dependency structure
 * before any components/apis are compiled so that they can be compiled in an
 * efficient and deterministic order.
 *
 * When just compiling a .fss file the .fss depends on its apis and those apis depend
 * on the apis they import.
 * When linking a .fss file, the .fss depends on its imports and components that
 * implement that import are added to the graph.
 *
 * Elements in the graph are stored as Node depends on List<Node>
 */
public class GraphRepository extends StubRepository implements FortressRepository {

	private static final String[] roots = {"FortressLibrary", "AnyType", "FortressBuiltin", "NatReflect", "NativeArray" };

	private Graph<GraphNode> graph;
	private Path path;
	private FortressRepository cache;
	private IndexBuilder builder;
	private GlobalEnvironment env;
	private boolean needUpdate = true;
        /* If link is true then pull in a component for an api */
        private boolean link = false;
	public GraphRepository(Path p, FortressRepository cache) {
		this.path = p;
		this.cache = cache;
		graph = new Graph<GraphNode>();
		builder = new IndexBuilder();
		env = new GlobalEnvironment.FromRepository(this);

                addRoots();
	}

        /* by default all the root apis should be added to the graph
         * and set as dependancies for everything else.
         */
        private void addRoots(){
            for ( String root : roots ){
                ApiGraphNode api = new ApiGraphNode(NodeFactory.makeAPIName(root));
                // ComponentGraphNode comp = new ComponentGraphNode(NodeFactory.makeAPIName(root));
                try{
                    api.setApi( cache.getApi( api.getName() ) );
                } catch ( FileNotFoundException e ){
                } catch ( IOException e ){
                }
                graph.addNode( api );

                /*
                try{
                    comp.setComponent( cache.getComponent( comp.getName() ) );
                } catch ( FileNotFoundException e ){
                } catch ( IOException e ){
                }
                graph.addNode( comp );
                graph.addEdge( comp, api );
                */
            }
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

        private boolean outOfDate( ApiGraphNode node, Map<GraphNode,Boolean> seen ) throws FileNotFoundException {
            if ( seen.containsKey(node) ){
                return seen.get(node);
            }
            File source = findFile(node.getName(), ProjectProperties.API_SOURCE_SUFFIX);
            if ( source.lastModified() > getCacheDate(node) ){
                Debug.debug( 2, node + " is newer " + source.lastModified() + " than the cache " + getCacheDate(node) );
                seen.put(node,true);
                return true;
            }
            List<GraphNode> depends = graph.dependancies(node);
            Debug.debug( 2, node + " depends on " + depends );
            for ( GraphNode next : depends ){
                if ( outOfDate( (ApiGraphNode) next, seen ) || getCacheDate((ApiGraphNode) next) > getCacheDate(node) ){
                    Debug.debug( 2, node + " is out of date because " + next + " is out of date" );
                    seen.put(node,true);
                    return true;
                }
            }
            seen.put(node,false);
            return false;
        }

	private boolean outOfDate( ComponentGraphNode node, Map<GraphNode,Boolean> seen ) throws FileNotFoundException {
		if ( seen.containsKey(node) ){
			return seen.get(node);
		}

		File source = findFile(node.getName(), ProjectProperties.COMP_SOURCE_SUFFIX);
		if ( source.lastModified() > getCacheDate(node) ){
                    Debug.debug( 2, node + " is newer " + source.lastModified() + " than the cache " + getCacheDate(node) );
			seen.put(node,true);
			return true;
		}
		List<GraphNode> depends = graph.dependancies(node);
                Debug.debug( 2, node + " depends on " + depends );
		for ( GraphNode next : depends ){
			if ( next instanceof ApiGraphNode ){
				if ( outOfDate( (ApiGraphNode) next, seen ) || getCacheDate((ApiGraphNode) next) > getCacheDate(node) ){
                                    Debug.debug( 2, node + " is out of date because " + next + " is out of date. Cached: " + getCacheDate((ApiGraphNode)next) + " Source: " + getCacheDate(node) );
					seen.put(node,true);
					return true;
				}
			} else if ( next instanceof ComponentGraphNode ){
				if ( outOfDate( (ComponentGraphNode) next, seen ) || getCacheDate( (ComponentGraphNode) next ) > getCacheDate(node) ){
                                    Debug.debug( 2, node + " is out of date because " + next + " is out of date" );
					seen.put(node,true);
					return true;
				}
			}
		}
		seen.put(node,false);
		return false;
	}

        private boolean newer( ApiGraphNode node, long now ) throws FileNotFoundException {
            File source = findFile(node.getName(), ProjectProperties.API_SOURCE_SUFFIX);
            return source.lastModified() > now;
        }
        
        private boolean newer( ComponentGraphNode node, long now ) throws FileNotFoundException {
            File source = findFile(node.getName(), ProjectProperties.COMP_SOURCE_SUFFIX);
            return source.lastModified() > now;
        }

	private static <TNode> List<TNode> onlyNodes( Graph graph, final Class type ){
		return Useful.applyToAll(graph.filter( new Fn<GraphNode, Boolean>(){
			public Boolean apply(GraphNode g){
				return type.isInstance(g);
			}
		}), new Fn<GraphNode, TNode>(){
			public TNode apply(GraphNode g){
				return (TNode) g;
			}
		});
	}

	private List<ApiGraphNode> onlyApis(){
		return onlyNodes( graph, ApiGraphNode.class );
	}

	private List<ComponentGraphNode> onlyComponents(){
		return onlyNodes( graph, ComponentGraphNode.class );
	}

	private List<ApiGraphNode> findOutOfDateApis() throws FileNotFoundException {
		List<ApiGraphNode> nodes = new ArrayList<ApiGraphNode>();

		for ( ApiGraphNode node : onlyApis() ){
			if ( outOfDate( node, new HashMap<GraphNode,Boolean>() ) ){
				nodes.add( node );
			}
		}

		return nodes;
	}

	private List<ComponentGraphNode> findOutOfDateComponents() throws FileNotFoundException {
		List<ComponentGraphNode> nodes = new ArrayList<ComponentGraphNode>();
		for ( ComponentGraphNode node : onlyComponents() ){
			if ( outOfDate( node, new HashMap<GraphNode,Boolean>() ) ){
				nodes.add( node );
			}
		}
		Graph<GraphNode> componentGraph = new Graph<GraphNode>( graph, new Fn<GraphNode, Boolean>(){
			public Boolean apply(GraphNode g){
				return g instanceof ComponentGraphNode;
			}
		});

		/* force components that import things to depend on the component
		 * that implements that import. This is for syntax abstraction
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

                if ( Debug.getDebug() >= 1 ){
                    componentGraph.dump();
                }

		List<GraphNode> sorted = componentGraph.sorted();
		List<ComponentGraphNode> rest = new ArrayList<ComponentGraphNode>();

		for ( GraphNode node : sorted ){
			ComponentGraphNode comp = (ComponentGraphNode) node;
			if ( nodes.contains( comp ) ){
                            if ( Arrays.asList(roots).contains(comp.getName().toString()) ){
                                rest.add( 0, comp );
                            } else {
                                rest.add( comp );
                            }
			}
		}

		return rest;
	}

	/* recompile anything that is out of date */
	private void refreshGraph() throws FileNotFoundException, IOException, StaticError {

		if ( needUpdate ){
			needUpdate = false;
			List<ApiGraphNode> recompileApis = findOutOfDateApis();
			List<ComponentGraphNode> recompileComponents = findOutOfDateComponents();
                        Debug.debug( 1, "Out of date components " + recompileComponents );
			/* these can be compiled all at once */
			compileApis(recompileApis);

			/* but these have to be done in a specific order due to
			 * syntax expansion requiring some components, like
			 * FortressBuiltin, being compiled
			 */
			for ( ComponentGraphNode node : recompileComponents ){
				node.setComponent(compileComponent(syntaxExpand(node)));
			}
		}

		/*
		if ( needUpdate ){
			System.out.println( "Refresh graph" );
			needUpdate = false;
			List<ApiGraphNode> recompileApis = new ArrayList<ApiGraphNode>();
			List<ComponentGraphNode> recompileComponents = new ArrayList<ComponentGraphNode>();
			for ( GraphNode node : graph.nodes() ){
				if ( node instanceof ApiGraphNode ){
					ApiGraphNode api = (ApiGraphNode) node;
					System.out.print( "Check out of date for " + api );
					if ( outOfDate(api, new HashMap<GraphNode,Boolean>()) ){
						recompileApis.add(api);
						System.out.println( " out of date" );
					} else {
						api.setApi( cache.getApi(api.getName()) );
						System.out.println( " cached" );
					}
				} else if ( node instanceof ComponentGraphNode ){
					ComponentGraphNode comp = (ComponentGraphNode) node;
					System.out.print( "Check out of date for " + comp );
					if ( outOfDate(comp, new HashMap<GraphNode,Boolean>()) ){
						recompileComponents.add(comp);
						System.out.println( " out of date" );
					} else {
						comp.setComponent( cache.getComponent(comp.getName()) );
						System.out.println( " cached" );
					}
				}
			}

			for ( ApiGraphNode node : recompileApis ){
				node.setApi(null);
			}

			compileApis();

			for ( ComponentGraphNode node : recompileComponents ){
				node.setComponent(compileComponent(syntaxExpand(node)));
			}
			System.out.println( "Graph was refreshed" );
		}
		*/
	}

	public ApiIndex getApi(APIName name) throws FileNotFoundException, IOException, StaticError {
		ApiGraphNode node = addApiGraph(name);
		Debug.debug( 2, "Get api for " + name);
		refreshGraph();
		return node.getApi().unwrap();
	}

	public ComponentIndex getComponent(APIName name) throws FileNotFoundException, IOException, StaticError {
		ComponentGraphNode node = addComponentGraph(name);
		Debug.debug( 2, "Get component for " + name );
		refreshGraph();
		return node.getComponent().unwrap();
	}

	private Component syntaxExpand( ComponentGraphNode node ) throws FileNotFoundException, IOException {
		Debug.debug( 1, "Expand component " + node );
		File file = findFile(node.getName(), ProjectProperties.COMP_SOURCE_SUFFIX);
                GraphRepository g1 = new GraphRepository( this.path, this.cache );
                /* FIXME: hack to prevent infinite recursion */
                Driver.setCurrentInterpreterRepository( g1 );
		Result result = FortressParser.parse(file, new GlobalEnvironment.FromRepository( g1 ), verbose());
		// Result result = FortressParser.parse(file, new GlobalEnvironment.FromRepository(this), verbose());
                /* FIXME: hack to prevent infinite recursion */
                Driver.setCurrentInterpreterRepository( this );
		if (result.isSuccessful()) {
                    Debug.debug( 1, "Expanded component " + node );
			Iterator<Component> components = result.components().iterator();
			if (components.hasNext()) return components.next();
			throw new ProgramError("Successful parse result was nonetheless empty, file " + file.getCanonicalPath());
		}
		throw new ProgramError(result.errors());
	}

	private ComponentIndex compileComponent( Component component ) throws StaticError {
		GlobalEnvironment knownApis = new GlobalEnvironment.FromMap(compiledApis());
		List<Component> components = new ArrayList<Component>();
		components.add(component);
		Fortress fort = new Fortress(this);
                long now = System.currentTimeMillis();
		Debug.debug( 1, "Compiling " + component + " at " + now );
		Iterable<? extends StaticError> errors = fort.analyze( knownApis, new ArrayList<Api>(), components, now );
                for ( StaticError e : errors ){
                    throw e;
                }
                Debug.debug( 1, "No errors for " + component );
                /*
		for ( StaticError e : errors ){
			System.err.println("Error while compiling component: " + e);
		}
                */
		ComponentGraphNode node = (ComponentGraphNode) graph.find( new ComponentGraphNode(component.getName()) );
		return node.getComponent().unwrap();
	}

	private Map<APIName, ApiIndex> compiledApis(){
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

	private CompilationUnit doParse( File file ) throws FileNotFoundException, IOException {
		BufferedReader in = Useful.utf8BufferedFileReader(file);
		try{
			com.sun.fortress.parser.Fortress parser = new com.sun.fortress.parser.Fortress(in, file.toString());
			xtc.parser.Result parseResult = parser.pFile(0);
			if (parseResult.hasValue()) {
				Object cu = ((SemanticValue) parseResult).value;
				if (cu instanceof CompilationUnit) {
					return (CompilationUnit) cu;
				}
			}
			throw new RuntimeException("Unexpected parse result: " + new ParserError((ParseError) parseResult, parser));
		} finally {
			in.close();
		}
	}

	private Component parseComponent( ComponentGraphNode node ){
		try{
			File fdot = findFile(node.getName(), ProjectProperties.COMP_SOURCE_SUFFIX);
			Debug.debug( 1, "Parsing " + fdot);
			CompilationUnit comp = doParse(fdot);
			if (comp instanceof Component) {
				return (Component) comp;
			} else {
				throw new RuntimeException("Unexpected parse of component");
			}
		} catch ( FileNotFoundException e ){
			throw new RuntimeException(e);
		} catch ( IOException e ){
			throw new RuntimeException(e);
		}
	}

	private Api parseApi( ApiGraphNode node ){ 
		try{
			File fdot = findFile(node.getName(), ProjectProperties.API_SOURCE_SUFFIX);
			CompilationUnit api = doParse(fdot);
			if (api instanceof Api) {
				return (Api) api;
			} else {
				throw new RuntimeException("Unexpected parse of api");
			}
		} catch ( FileNotFoundException e ){
			throw new RuntimeException(e);
		} catch ( IOException e ){
			throw new RuntimeException(e);
		}
	}
    
	public Map<APIName, ApiIndex> apis() {
		return cache.apis();
	}

	private void compileApis( List<ApiGraphNode> apis ){
		for ( ApiGraphNode node : apis ){
			/* yes, set the api to nothing so that it gets
			 * recompiled no matter what
			 */
			node.setApi(null);
		}
		if ( apis.size() > 0 ){
			compileApis();
		}
	}

	private void compileApis(){
		List<Api> uncompiled = Useful.applyToAll(graph.filter(new Fn<GraphNode,Boolean>(){
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
		GlobalEnvironment knownApis = new GlobalEnvironment.FromMap(compiledApis());
		List<Component> components = new ArrayList<Component>();
		Fortress fort = new Fortress(this);
		Debug.debug( 1, "Compiling " + uncompiled );
		Iterable<? extends StaticError> errors = fort.analyze( knownApis, uncompiled, components, System.currentTimeMillis() );
		for ( StaticError e : errors ){
			System.err.println("Error while compiling apis: " + e);
		}
	}

	private List<APIName> componentImports(APIName name) throws FileNotFoundException, StaticError {
		File fdot = findFile(name, ProjectProperties.COMP_SOURCE_SUFFIX);
		return com.sun.fortress.syntax_abstractions.parser.PreParser.getImportedApis(fdot);
	}

	private List<APIName> apiImports(APIName name) throws FileNotFoundException, StaticError {
		File fdot = findFile(name, ProjectProperties.API_SOURCE_SUFFIX);
		return com.sun.fortress.syntax_abstractions.parser.PreParser.getImportedApis(fdot);
	}
     
	public void addApi(APIName name, ApiIndex definition) {
		if ( ! graph.contains(new ApiGraphNode(name)) ){
			throw new RuntimeException("No such api '" + name + "'");
		} else {
			ApiGraphNode node = (ApiGraphNode) graph.find(new ApiGraphNode(name));
			node.setApi(definition);
			cache.addApi(name, definition);
		}
	}

	public void addComponent(APIName name, ComponentIndex definition){
		if ( ! graph.contains(new ComponentGraphNode(name)) ){
			throw new RuntimeException("No such component " + name);
		} else {
			ComponentGraphNode node = (ComponentGraphNode) graph.find(new ComponentGraphNode(name));
			node.setComponent(definition);
			cache.addComponent(name, definition);
		}
	}

	private ApiGraphNode addApiGraph( APIName name ) throws FileNotFoundException, IOException {
		// System.out.println( "Add api graph " + name );
		ApiGraphNode node = new ApiGraphNode( name );
		if ( ! graph.contains( node ) ){
			needUpdate = true;
			graph.addNode( node );
			try{
                                /* try to load the api from the cache.
                                 * if it fails then it will be reloaded later on
                                 * in refreshGraph
                                 */
				node.setApi( cache.getApi(name) );
			} catch ( FileNotFoundException f ){
				/* oh well */
			} catch ( IOException e ){
			}
			// System.out.println( "Find imports" );
			for ( APIName api : apiImports(name) ){
				Debug.debug( 1, "Add edge " + api );
				graph.addEdge(node, addApiGraph(api));
			}
			for ( String root : roots ){
				graph.addEdge(node, addApiGraph(NodeFactory.makeAPIName(root)));
			}
		} else {
			node = (ApiGraphNode) graph.find( node );
		}

                if ( link ){
                    graph.addEdge(addComponentGraph(name),node);
                }

		return node;

	}

	private ComponentGraphNode addComponentGraph( APIName name ) throws FileNotFoundException, IOException, StaticError {
		ComponentGraphNode node = new ComponentGraphNode( name );
		if ( ! graph.contains( node ) ){
			needUpdate = true;
			graph.addNode( node );
			try{
                                /* try to load the component from the cache.
                                 * if it fails then it will be reloaded later on
                                 * in refreshGraph
                                 */
				node.setComponent( cache.getComponent(name) );
			} catch ( FileNotFoundException f ){
				/* oh well */
			} catch ( IOException e ){
			}
			for ( APIName api : componentImports(name) ){
                                /* the component depends on the imported api */
				graph.addEdge(node, addApiGraph(api));
                                /* the component depends on the imported api's component */
				// graph.addEdge(node, addComponentGraph(api));
			}
                        /*
			for ( String root : roots ){
				graph.addEdge(node, addComponentGraph(NodeFactory.makeAPIName(root)));
			}
                        */
		} else {
			node = (ComponentGraphNode) graph.find( node );
		}

		return node;
	}

	protected CompilationUnit getCompilationUnit(File f) throws IOException {
		Result result = FortressParser.parse(f, this.env, verbose());
		if (result.isSuccessful()) {
			Iterator<Api> apis = result.apis().iterator();
			Iterator<Component> components = result.components().iterator();
			if (apis.hasNext()) return apis.next();
			if (components.hasNext()) return components.next();
			throw new ProgramError("Successful parse result was nonetheless empty, file " + f.getCanonicalPath());
		}
		throw new ProgramError(result.errors());
	}

	/*
	private ComponentIndex indexOf(ComponentGraphNode node) throws FileNotFoundException, IOException {
		File fdot = findFile(node.getName(), ProjectProperties.API_SOURCE_SUFFIX);
		Component component = (Component) getCompilationUnit(fdot);
		node.setComponent(component);
		return builder.buildComponentIndex( component, fdot.lastModified() );
	}
	
	public ComponentIndex getLinkedComponent(APIName name) throws FileNotFoundException, IOException {
		ComponentGraphNode node = addComponentGraph(name);
		return indexOf(node);
	}
	*/
	public long getModifiedDateForApi(APIName name)
		throws FileNotFoundException {
		return 0;
	}

	public long getModifiedDateForComponent(APIName name)
		throws FileNotFoundException {
		return 0;
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

	public ComponentIndex getLinkedComponent(APIName name) throws FileNotFoundException, IOException {
		/*
		ComponentGraphNode node = getComponentNode(name);
		// (ComponentGraphNode) graph.find( new ComponentGraphNode(name) );
		// return cache.getComponent(name);
		return node.getComponent().unwrap();
		*/
                link = true;
                ComponentIndex node = getComponent(name);
                link = false;
                return node;
	}
}
