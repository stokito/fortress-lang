package com.sun.fortress.compiler.phases;

import java.util.Map;

import com.sun.fortress.compiler.AnalyzeResult;
import com.sun.fortress.compiler.Desugarer;
import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.IndexBuilder;
import com.sun.fortress.compiler.PreTypeCheckDesugarer;
import com.sun.fortress.compiler.StaticChecker;
import com.sun.fortress.compiler.typechecker.TypeEnv;
import com.sun.fortress.exceptions.DesugarerError;
import com.sun.fortress.exceptions.MultipleStaticError;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.syntax_abstractions.phases.GrammarRewriter;
import com.sun.fortress.useful.Debug;

import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.Pair;

public class PreTypeCheckDesugarPhase extends Phase {

    public PreTypeCheckDesugarPhase(Phase parentPhase) {
        super(parentPhase);
    }

    
	@Override
	public AnalyzeResult execute() throws StaticError {
        Debug.debug(Debug.Type.FORTRESS, 1, "Start phase PreTypeCheckDesugar");
        AnalyzeResult previous = parentPhase.getResult();

        GlobalEnvironment apiEnv = new GlobalEnvironment.FromMap(CollectUtil
                .union(repository.apis(), previous.apis()));

        // Desugar APIs.
        PreTypeCheckDesugarer.ApiResult apis = PreTypeCheckDesugarer.desugarApis(previous
                .apis(), apiEnv);
        if (!apis.isSuccessful()) {
            throw new MultipleStaticError(apis.errors());
        }

        // Desugar components.
        PreTypeCheckDesugarer.ComponentResult components = PreTypeCheckDesugarer
        .desugarComponents(previous.components(), apiEnv);

	if (!components.isSuccessful()) {
	    throw new MultipleStaticError(components.errors());
	}

        return new AnalyzeResult(apis.apis(), components.components(),
                IterUtil.<StaticError> empty(), previous.typeEnvAtNode());
    }

}
