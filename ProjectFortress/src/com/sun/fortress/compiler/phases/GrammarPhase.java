package com.sun.fortress.compiler.phases;

import com.sun.fortress.compiler.AnalyzeResult;
import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.IndexBuilder;
import com.sun.fortress.exceptions.MultipleStaticError;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.syntax_abstractions.phases.GrammarRewriter;
import com.sun.fortress.useful.Debug;

import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.iter.IterUtil;

public class GrammarPhase extends Phase {

	public GrammarPhase(Phase parentPhase) {
		super(parentPhase);
	}
	
	@Override
	public AnalyzeResult execute() throws StaticError {
        Debug.debug( Debug.Type.FORTRESS, 1, "Start phase GrammarPhase" );		
		AnalyzeResult previous = parentPhase.getResult();    	    	    	

		GlobalEnvironment apiEnv =
			new GlobalEnvironment.FromMap(CollectUtil.union(repository.apis(),
					previous.apis()));

		GrammarRewriter.ApiResult apiID = GrammarRewriter.rewriteApis(previous.apis(), apiEnv);
		if (!apiID.isSuccessful()) {
			throw new MultipleStaticError(apiID.errors());
		}

		IndexBuilder.ApiResult apiDone = IndexBuilder.buildApis(apiID.apis(), lastModified);
		if (!apiDone.isSuccessful()) {
			throw new MultipleStaticError(apiDone.errors());
		}

		return new AnalyzeResult(apiDone.apis(), previous.components(), 
				IterUtil.<StaticError>empty(), previous.typeEnvAtNode());        
	}

}
