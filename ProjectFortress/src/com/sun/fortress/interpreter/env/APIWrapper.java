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
package com.sun.fortress.interpreter.env;

import static com.sun.fortress.exceptions.InterpreterBug.bug;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.useful.BASet;

public class APIWrapper extends CUWrapper {

    public APIWrapper(Api api, HashMap<String, ComponentWrapper> linker,
            String[] implicitLibs) {
        super(api, linker, implicitLibs);
        // TODO Auto-generated constructor stub
    }
    
    public CompilationUnit populateOne(ComponentWrapper exporter) {
        if (visitState != IMPORTED)
            return bug("Component wrapper in wrong visit state: " + visitState);

        visitState = POPULATED;

        be.setExporterAndApi(exporter, this);
        
        CompilationUnit cu = comp_unit;

                                      // Caches information in dis!
        be.visit(cu);
        // Reset the non-function names from the disambiguator.
        excludedImportNames = new BASet<String>(com.sun.fortress.useful.StringHashComparer.V);
        be.getEnvironment().visit(nameCollector);
        comp_unit = cu;

        return cu;
    }
    
    public Set<String> getTopLevelRewriteNames() {
        return desugarer.getTopLevelRewriteNames();
    }
    
}
