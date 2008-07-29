package com.sun.fortress.compiler.phases;

import com.sun.fortress.compiler.AnalyzeResult;
import com.sun.fortress.compiler.environments.TopLevelEnvGen;
import com.sun.fortress.exceptions.MultipleStaticError;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.useful.Debug;

import edu.rice.cs.plt.iter.IterUtil;

public class CodeGenerationPhase extends Phase {
	
	public CodeGenerationPhase(Phase parentPhase) {
		super(parentPhase);
	}

	@Override
	public AnalyzeResult execute() throws StaticError {		
        Debug.debug( Debug.Type.FORTRESS, 1, "Start phase CodeGeneration" );
		AnalyzeResult previous = parentPhase.getResult();
		
		TopLevelEnvGen.CompilationUnitResult apiGR = TopLevelEnvGen
				.generateApiEnvs(previous.apis());

		if (!apiGR.isSuccessful()) {
			throw new MultipleStaticError(apiGR.errors());
		}

		// Generate top-level byte code environments for components
		TopLevelEnvGen.CompilationUnitResult componentGR = TopLevelEnvGen
				.generateComponentEnvs(previous.components());

		if (!componentGR.isSuccessful()) {
			throw new MultipleStaticError(componentGR.errors());
		}

		return new AnalyzeResult(previous.apis(), previous.components(), 
				IterUtil.<StaticError> empty(), previous.typeEnvAtNode());        
        
	}

}
