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

import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.repository.DerivedFiles;
import com.sun.fortress.repository.IOAst;
import com.sun.fortress.useful.BASet;
import com.sun.fortress.useful.Fn;

public class ComponentWrapper extends CUWrapper {

    /* 
     * Next three lines are for the "cache" of rewritten ASTs
     */
    private static Fn<APIName, String> toCompFileName = new Fn<APIName, String>() {
        @Override
        public String apply(APIName x) {
            // TODO Auto-generated method stub
            return null;
        } 
    };
    private static IOAst componentReaderWriter = new IOAst(toCompFileName);
    private static DerivedFiles<CompilationUnit> componentCache = 
        new DerivedFiles<CompilationUnit>(componentReaderWriter);
    
    public ComponentWrapper(ComponentIndex comp, HashMap<String, ComponentWrapper> linker,
            String[] implicitLibs) {
        super((Component) comp.ast(), linker, implicitLibs);
        // TODO Auto-generated constructor stub
    }

    public ComponentWrapper(ComponentIndex comp, APIWrapper api,
            HashMap<String, ComponentWrapper> linker, String[] implicitLibs) {
        super((Component) comp.ast(), api, linker, implicitLibs);
        // TODO Auto-generated constructor stub
    }

    public ComponentWrapper(ComponentIndex comp, List<APIWrapper> api_list,
            HashMap<String, ComponentWrapper> linker, String[] implicitLibs) {
        super((Component) comp.ast(), api_list, linker, implicitLibs);
        // TODO Auto-generated constructor stub
    }

    public CompilationUnit populateOne() {
        if (visitState != IMPORTED)
            return bug("Component wrapper " + name() + " in wrong visit state: " + visitState);

        visitState = POPULATED;

        CompilationUnit cu = comp_unit;

        cu = (CompilationUnit) desugarer.visit(cu); // Rewrites cu!
                                                  // Caches information in dis!
        be.visit(cu);
        // Reset the non-function names from the disambiguator.
        excludedImportNames = new BASet<String>(com.sun.fortress.useful.StringHashComparer.V);
        be.getEnvironment().visit(nameCollector);
        comp_unit = cu;
         
        for (String implicitLibraryName : implicitLibs) {
            be.importAPIName(implicitLibraryName);
        }
        
        for (CUWrapper api: exports.values()) {
            be.importAPIName(api.name());
        }
        
        for (APIWrapper api: exports.values()) {
            api.populateOne(this);
        }

        return cu;
    }
    
}
