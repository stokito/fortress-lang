package com.sun.fortress.compiler.phases;

import java.util.Map;

import com.sun.fortress.compiler.AnalyzeResult;
import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.IndexBuilder;
import com.sun.fortress.compiler.StaticChecker;
import com.sun.fortress.compiler.typechecker.TypeEnv;
import com.sun.fortress.exceptions.MultipleStaticError;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.useful.Debug;

import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.Pair;

public class TypeCheckPhase extends Phase {
	
    public TypeCheckPhase(Phase parentPhase) {
		super(parentPhase);
	}

	@Override
    public AnalyzeResult execute( ) throws StaticError {
        Debug.debug( Debug.Type.FORTRESS, 1, "Start phase TypeCheck" );
		AnalyzeResult previous = parentPhase.getResult();    	

    	IndexBuilder.ApiResult apiIndex = IndexBuilder.buildApis(previous.apiIterator(), lastModified);
    	IndexBuilder.ComponentResult componentIndex = IndexBuilder.buildComponents(previous.componentIterator(), lastModified);
    	GlobalEnvironment apiEnv = new GlobalEnvironment.FromMap(CollectUtil.union(repository.apis(),
                                                     apiIndex.apis()));

    	StaticChecker.ApiResult apiSR = StaticChecker.checkApis( apiIndex.apis(), apiEnv );

    	if ( !apiSR.isSuccessful() ){
    		throw new MultipleStaticError(apiSR.errors());
    	}

    	StaticChecker.ComponentResult componentSR =
    		StaticChecker.checkComponents( componentIndex.components(), env);

    	if ( !componentSR.isSuccessful() ){
    		throw new MultipleStaticError(componentSR.errors());
    	}

    	return new AnalyzeResult(apiSR.apis(), componentSR.components(), 
    			IterUtil.<StaticError> empty(), 
    			Option.<Map<Pair<Node,Span>, TypeEnv>>some(componentSR.typeEnvAtNode()));
    }

}
