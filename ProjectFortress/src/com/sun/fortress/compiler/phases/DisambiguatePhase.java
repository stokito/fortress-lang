package com.sun.fortress.compiler.phases;

import com.sun.fortress.compiler.AnalyzeResult;
import com.sun.fortress.compiler.Disambiguator;
import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.IndexBuilder;
import com.sun.fortress.exceptions.MultipleStaticError;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.useful.Debug;

import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.iter.IterUtil;

public class DisambiguatePhase extends Phase {		

	public DisambiguatePhase(Phase parentPhase) {
		super(parentPhase);
	}		
	
    @Override
    public AnalyzeResult execute( ) throws StaticError {
        Debug.debug( Debug.Type.FORTRESS, 1, "Start phase Disambiguate" );
		AnalyzeResult previous = parentPhase.getResult();    	    	

		// Build a new GlobalEnvironment consisting of all APIs in a global
		// repository combined with all APIs that have been processed in the previous
		// step.  For now, we are implementing pure static linking, so there is
		// no global repository.
		GlobalEnvironment rawApiEnv =
			new GlobalEnvironment.FromMap(CollectUtil.union(repository.apis(),
					previous.apis()));

		// Rewrite all API ASTs so they include only fully qualified names, relying
		// on the rawApiEnv constructed in the previous step. Note that, after this
		// step, the rawApiEnv is stale and needs to be rebuilt with the new API ASTs.
		Disambiguator.ApiResult apiDR =
			Disambiguator.disambiguateApis(previous.apiIterator(), rawApiEnv, repository.apis());
		if (!apiDR.isSuccessful()) {
			throw new MultipleStaticError(apiDR.errors());
		}

		// Rebuild ApiIndices.
		IndexBuilder.ApiResult apiIR =
			IndexBuilder.buildApis(apiDR.apis(), lastModified);
		if (!apiIR.isSuccessful()) {
			throw new MultipleStaticError(apiIR.errors());
		}

		// Rebuild GlobalEnvironment.
		GlobalEnvironment apiEnv =
			new GlobalEnvironment.FromMap(CollectUtil.union(repository.apis(),
					apiIR.apis()));

		Disambiguator.ComponentResult componentDR =
			Disambiguator.disambiguateComponents(previous.componentIterator(), apiEnv,
					previous.components());
		if (!componentDR.isSuccessful()) {
			throw new MultipleStaticError(componentDR.errors());
		}

		// Rebuild ComponentIndices.
		IndexBuilder.ComponentResult componentsDone =
			IndexBuilder.buildComponents(componentDR.components(), lastModified);
		if (!componentsDone.isSuccessful()) {
			throw new MultipleStaticError(componentsDone.errors());
		}

		return new AnalyzeResult(apiIR.apis(), componentsDone.components(), 
				IterUtil.<StaticError>empty(), previous.typeEnvAtNode());
        
    }

}
