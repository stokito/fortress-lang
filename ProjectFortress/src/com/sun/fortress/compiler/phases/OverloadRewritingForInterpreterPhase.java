/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.phases;

import com.sun.fortress.compiler.AnalyzeResult;
import com.sun.fortress.compiler.OverloadRewriter;
import com.sun.fortress.exceptions.MultipleStaticError;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.useful.Debug;
import edu.rice.cs.plt.iter.IterUtil;

public class OverloadRewritingForInterpreterPhase extends Phase {

    public OverloadRewritingForInterpreterPhase(Phase parentPhase) {
        super(parentPhase);
    }

    @Override
    public AnalyzeResult execute() throws StaticError {
        Debug.debug(Debug.Type.FORTRESS, 1, "Start phase Overload Rewriting");
        AnalyzeResult previous = parentPhase.getResult();

        OverloadRewriter.ComponentResult results =
                OverloadRewriter.rewriteComponents(previous.components(), env, true);

        if (!results.isSuccessful()) {
            throw new MultipleStaticError(results.errors());
        }

        return new AnalyzeResult(previous.apis(), results.components(),
                                 IterUtil.<StaticError>empty());
    }

}
