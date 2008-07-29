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
import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.repository.FortressRepository;

public abstract class Phase {

    Phase parentPhase;
    FortressRepository repository;
    GlobalEnvironment env;
    long lastModified;

    private AnalyzeResult result;

    public Phase(Phase parentPhase) {
        this.parentPhase = parentPhase;
        if (parentPhase != null) {
            repository = parentPhase.getRepository();
            env = parentPhase.getEnv();
            lastModified = parentPhase.getLastModified();
        }
    }

    public final AnalyzeResult getResult() {
        return result;
    }

    public final FortressRepository getRepository() {
        return repository;
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
