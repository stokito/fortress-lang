/*******************************************************************************
    Copyright 2008,2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler;

import edu.rice.cs.plt.iter.IterUtil;
import com.sun.fortress.compiler.typechecker.TypeCheckerResult;
import com.sun.fortress.exceptions.StaticError;

import java.util.ArrayList;
import java.util.List;

public class StaticPhaseResult {

    private Iterable<? extends StaticError> _errors;

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

    public void setErrors(List<StaticError> errors) { _errors = errors; }

    protected static Iterable<? extends StaticError> collectErrors(Iterable<? extends TypeCheckerResult> results) {
        Iterable<? extends StaticError> allErrors = new ArrayList<StaticError>();

        for (TypeCheckerResult result: results) {
            allErrors = IterUtil.compose(allErrors, result.errors());
        }
        return allErrors;
    }
}
