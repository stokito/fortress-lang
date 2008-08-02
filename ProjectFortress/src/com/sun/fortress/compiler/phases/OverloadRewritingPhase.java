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

package com.sun.fortress.compiler.phases;

import com.sun.fortress.compiler.AnalyzeResult;
import com.sun.fortress.compiler.OverloadRewriter;
import com.sun.fortress.exceptions.MultipleStaticError;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.useful.Debug;

import edu.rice.cs.plt.iter.IterUtil;

public class OverloadRewritingPhase extends Phase {

    public OverloadRewritingPhase(Phase parentPhase) {
        super(parentPhase);
    }

    @Override
    public AnalyzeResult execute() throws StaticError {
        Debug.debug(Debug.Type.FORTRESS, 1, "Start phase Overload Rewriting");
        AnalyzeResult previous = parentPhase.getResult();

        OverloadRewriter.ComponentResult results =
            OverloadRewriter.rewriteComponents(previous.components(), env);

        if (!results.isSuccessful()) {
            throw new MultipleStaticError(results.errors());
        }

        return new AnalyzeResult(previous.apis(), results.components(),
                IterUtil.<StaticError> empty(), previous.typeEnvAtNode());
    }

}
