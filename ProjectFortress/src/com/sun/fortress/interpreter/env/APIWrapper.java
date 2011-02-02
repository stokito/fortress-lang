/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/
package com.sun.fortress.interpreter.env;

import static com.sun.fortress.exceptions.InterpreterBug.bug;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.useful.BASet;

import java.util.HashMap;
import java.util.Set;

public class APIWrapper extends CUWrapper {

    public APIWrapper(Api api, HashMap<String, NonApiWrapper> linker, String[] implicitLibs) {
        super(api, linker, implicitLibs);
        // TODO Auto-generated constructor stub
    }

    public CompilationUnit populateOne(NonApiWrapper exporter) {
        if (visitState != IMPORTED) return bug("Component wrapper in wrong visit state: " + visitState);

        visitState = POPULATED;

        getEnvBuilder().setExporterAndApi(exporter, this);

        CompilationUnit cu = comp_unit;

        // Caches information in dis!
        getEnvBuilder().visit(cu);
        // Reset the non-function names from the disambiguator.
        excludedImportNames = new BASet<String>(com.sun.fortress.useful.StringHashComparer.V);
        getEnvBuilder().getEnvironment().visit(nameCollector);
        comp_unit = cu;

        return cu;
    }

    public Set<String> getTopLevelRewriteNames() {
        return desugarer.getTopLevelRewriteNames();
    }

}
