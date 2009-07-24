/*******************************************************************************
 Copyright 2009 Sun Microsystems, Inc.,
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
import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.PreTypeCheckDesugarer;
import com.sun.fortress.exceptions.MultipleStaticError;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.useful.Debug;
import edu.rice.cs.plt.iter.IterUtil;

public class PreTypeCheckDesugarPhase extends Phase {

    public PreTypeCheckDesugarPhase(Phase parentPhase) {
        super(parentPhase);
    }

    @Override
    public AnalyzeResult execute() throws StaticError {
        Debug.debug(Debug.Type.FORTRESS, 1, "Start phase PreTypeCheckDesugar");
        AnalyzeResult previous = parentPhase.getResult();

        GlobalEnvironment apiEnv = new GlobalEnvironment.FromMap(previous.apis());

        // Desugar APIs.
        PreTypeCheckDesugarer.ApiResult apis = PreTypeCheckDesugarer.desugarApis(previous.apis(), apiEnv);
        if (!apis.isSuccessful()) {
            throw new MultipleStaticError(apis.errors());
        }

        // Desugar components.
        PreTypeCheckDesugarer.ComponentResult components =
                PreTypeCheckDesugarer.desugarComponents(previous.components(), apiEnv);

        if (!components.isSuccessful()) {
            throw new MultipleStaticError(components.errors());
        }

        return new AnalyzeResult(apis.apis(),
                                 components.components(),
                                 IterUtil.<StaticError>empty(),
                                 previous.typeCheckerOutput());
    }

}
