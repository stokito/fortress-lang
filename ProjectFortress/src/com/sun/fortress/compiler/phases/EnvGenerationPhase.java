/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.phases;

import com.sun.fortress.compiler.AnalyzeResult;
import com.sun.fortress.compiler.environments.TopLevelEnvGen;
import com.sun.fortress.exceptions.MultipleStaticError;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.useful.Debug;
import edu.rice.cs.plt.iter.IterUtil;

public class EnvGenerationPhase extends Phase {

    public EnvGenerationPhase(Phase parentPhase) {
        super(parentPhase);
    }

    @Override
    public AnalyzeResult execute() throws StaticError {
        Debug.debug(Debug.Type.FORTRESS, 1, "Start phase EnvGeneration");
        AnalyzeResult previous = parentPhase.getResult();

        TopLevelEnvGen.CompilationUnitResult apiGR = TopLevelEnvGen.generateApiEnvs(previous.apis());

        if (!apiGR.isSuccessful()) {
            throw new MultipleStaticError(apiGR.errors());
        }

        // Generate top-level byte code environments for components
        TopLevelEnvGen.CompilationUnitResult componentGR = TopLevelEnvGen.generateComponentEnvs(previous.components());

        if (!componentGR.isSuccessful()) {
            throw new MultipleStaticError(componentGR.errors());
        }

        Debug.debug(Debug.Type.ENVGEN, 1, "Before invoking Compile: components=", previous.components());

        return new AnalyzeResult(previous.apis(),
                                 previous.components(),
                                 IterUtil.<StaticError>empty());

    }

}

