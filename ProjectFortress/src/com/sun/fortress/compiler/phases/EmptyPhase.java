package com.sun.fortress.compiler.phases;

import java.util.Map;

import com.sun.fortress.compiler.AnalyzeResult;
import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.IndexBuilder;
import com.sun.fortress.compiler.typechecker.TypeEnv;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.repository.FortressRepository;
import com.sun.fortress.useful.Debug;

import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.Pair;

public class EmptyPhase extends Phase {

	private final Iterable<Api> apis;
	private final Iterable<Component> components;
	
	public EmptyPhase(FortressRepository repository, GlobalEnvironment env, 
			Iterable<Api> apis, Iterable<Component> components, long lastModified) {
		super(null);		
		this.repository = repository;		
		this.env = env;
		this.apis = apis;
		this.components = components;		
		this.lastModified = lastModified;
	}

	@Override	
    public AnalyzeResult execute( ) throws StaticError {
        Debug.debug( Debug.Type.FORTRESS, 1, "Start phase Empty" );
        IndexBuilder.ApiResult apiIndex = IndexBuilder.buildApis(apis, lastModified);
        IndexBuilder.ComponentResult componentIndex = IndexBuilder.buildComponents(components, lastModified);
        return new AnalyzeResult(apiIndex.apis(), componentIndex.components(),
        		IterUtil.<StaticError>empty(), Option.<Map<Pair<Node,Span>, TypeEnv>>none());
    }

}
