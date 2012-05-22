/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.phases;

import com.sun.fortress.compiler.AnalyzeResult;
import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.PreDisambiguationDesugarer;
import com.sun.fortress.exceptions.MultipleStaticError;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.useful.Debug;
import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.iter.IterUtil;

/**
 * A phase for running desugarings that must occur before Disambiguation.
 */
public class PreDisambiguationDesugarPhase extends Phase {

    public PreDisambiguationDesugarPhase(Phase parentPhase) {
        super(parentPhase);
    }

    @Override
    public AnalyzeResult execute() throws StaticError {
        Debug.debug(Debug.Type.FORTRESS, 1, "Start phase Pre-Disambiguation Desugar");
        AnalyzeResult previous = parentPhase.getResult();

        GlobalEnvironment apiEnv = new GlobalEnvironment.FromMap(CollectUtil.union(env.apis(), previous.apis()));

        PreDisambiguationDesugarer.ApiResult apiDSR = PreDisambiguationDesugarer.desugarApis(previous.apis(), apiEnv);

        if (!apiDSR.isSuccessful()) {
            throw new MultipleStaticError(apiDSR.errors());
        }

        PreDisambiguationDesugarer.ComponentResult componentDSR =
                PreDisambiguationDesugarer.desugarComponents(previous.components(), apiEnv);

        if (!componentDSR.isSuccessful()) {
            throw new MultipleStaticError(componentDSR.errors());
        }

        return new AnalyzeResult(apiDSR.apis(),
                                 componentDSR.components(),
                                 IterUtil.<StaticError>empty());
    }

}
