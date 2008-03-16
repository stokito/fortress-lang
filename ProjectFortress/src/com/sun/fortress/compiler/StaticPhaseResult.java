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

package com.sun.fortress.compiler;

import edu.rice.cs.plt.iter.IterUtil;
import com.sun.fortress.compiler.typechecker.TypeCheckerResult;

import java.util.ArrayList;

public class StaticPhaseResult {
    
    private final Iterable<? extends StaticError> _errors;
    
    public StaticPhaseResult() {
        this(IterUtil.<StaticError>empty());
    }
    
    public StaticPhaseResult(Iterable<? extends StaticError> errors) {
        _errors = errors;
    }
    
    public StaticPhaseResult(StaticPhaseResult r1, StaticPhaseResult r2) {
        _errors = IterUtil.compose(r1._errors, r2._errors);
    }
    
    public boolean isSuccessful() { return IterUtil.isEmpty(_errors); }
    
    public Iterable<? extends StaticError> errors() { return _errors; }

    protected static Iterable<? extends StaticError> collectErrors(Iterable<? extends TypeCheckerResult> results) {
        Iterable<? extends StaticError> allErrors = new ArrayList<StaticError>();

        for (TypeCheckerResult result: results) {
            allErrors = IterUtil.compose(allErrors, result.errors());
        }
        return allErrors;
    }
}
