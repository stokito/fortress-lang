/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.phases;

import com.sun.fortress.compiler.AnalyzeResult;
import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.repository.FortressRepository;

public abstract class Phase {

    Phase parentPhase;
    GlobalEnvironment env;
    long lastModified;

    private AnalyzeResult result;

    public Phase(Phase parentPhase) {
        this.parentPhase = parentPhase;
        if (parentPhase != null) {
            env = parentPhase.getEnv();
            lastModified = parentPhase.getLastModified();
        }
    }

    public final AnalyzeResult getResult() {
        return result;
    }

    public final GlobalEnvironment getEnv() {
        return env;
    }

    public final long getLastModified() {
        return lastModified;
    }

    public abstract AnalyzeResult execute() throws StaticError;

    public final AnalyzeResult run() throws StaticError {
        if (parentPhase != null)
            parentPhase.run();
        result = execute();
        return result;
    }

}
