/*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
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

import java.io.File;
import java.util.Map;
import java.util.Collections;
import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.iter.IterUtil;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.ComponentIndex;

import com.sun.fortress.useful.NI;

public class Fortress {
    
    private final FortressRepository _repository;
    
    public Fortress(FortressRepository repository) { _repository = repository; }
    
    /**
     * Compile all definitions in the given files, and any additional sources that
     * they depend on, and add them to the fortress.
     */
    public Iterable<? extends StaticError> compile(File... files) {
        return compile(IterUtil.asIterable(files));
    }
    
    /**
     * Compile all definitions in the given files, and any additional sources that
     * they depend on, and add them to the fortress.
     */
    public Iterable<? extends StaticError> compile(Iterable<File> files) {
        GlobalEnvironment env = new GlobalEnvironment(_repository.apis());
        
        Parser.Result pr = Parser.parse(files, env);
        if (!pr.isSuccessful()) { return pr.errors(); }
        
        // Handle APIs first
        
        // Build ApiIndices before disambiguating to allow circular references.
        // An IndexBuilder.ApiResult contains a map of strings (names) to
        // ApiIndices.
        IndexBuilder.ApiResult rawApiIR = IndexBuilder.buildApis(pr.apis());
        if (!rawApiIR.isSuccessful()) { return rawApiIR.errors(); }
        
        // Build a new GlobalEnvironment consisting of all APIs in a global
        // repository combined with all APIs that have been processed in the previous
        // step. For now, we are implementing pure static linking, so there is
        // no global repository.
        GlobalEnvironment rawApiEnv =
            new GlobalEnvironment(CollectUtil.compose(_repository.apis(),
                                                      rawApiIR.apis()));
        
        // Rewrite all API ASTs so they include only fully qualified names, relying
        // on the rawApiEnv constructed in the previous step. Note that, after this
        // step, the rawApiEnv is stale and needs to be rebuilt with the new API ASTs.
        Disambiguator.ApiResult apiDR =
            Disambiguator.disambiguateApis(pr.apis(), rawApiEnv);
        if (!apiDR.isSuccessful()) { return apiDR.errors(); }
        
        // Rebuild ApiIndices.
        IndexBuilder.ApiResult apiIR = IndexBuilder.buildApis(apiDR.apis());
        if (!apiIR.isSuccessful()) { return apiIR.errors(); }
        
        // Rebuild GlobalEnvironment.
        GlobalEnvironment apiEnv =
            new GlobalEnvironment(CollectUtil.compose(_repository.apis(),
                                                      apiIR.apis()));
        
        // Do all type checking and other static checks on APIs.
        StaticChecker.ApiResult apiSR =
            StaticChecker.checkApis(apiIR.apis(), apiEnv);
        if (!apiSR.isSuccessful()) { return apiSR.errors(); }
        
        // Generate code. Code is stored in the _repository object. In an implementation
        // with pure static linking, we would have to write this code back out to a file.
        // In an implementation with fortresses, we would write this code into the resident
        // fortress.
        for (Map.Entry<String, ApiIndex> newApi : apiIR.apis().entrySet()) {
            _repository.addApi(newApi.getKey(), newApi.getValue());
        }
        
        // Handle components
        
        // Build ApiIndices before disambiguating to allow circular references.
        // An IndexBuilder.ApiResult contains a map of strings (names) to
        // ApiIndices.
        IndexBuilder.ComponentResult rawComponentIR =
            IndexBuilder.buildComponents(pr.components());
        if (!rawComponentIR.isSuccessful()) { return rawComponentIR.errors(); }
        
        Disambiguator.ComponentResult componentDR =
            Disambiguator.disambiguateComponents(pr.components(), env,
                                                 rawComponentIR.components());
        if (!componentDR.isSuccessful()) { return componentDR.errors(); }
        
        IndexBuilder.ComponentResult componentIR =
            IndexBuilder.buildComponents(componentDR.components());
        if (!componentIR.isSuccessful()) { return componentIR.errors(); }
        
        StaticChecker.ComponentResult componentSR =
            StaticChecker.checkComponents(componentIR.components(), env);
        if (!componentSR.isSuccessful()) { return componentSR.errors(); }
        
        // Additional optimization phases can be inserted here
        
        for (Map.Entry<String, ComponentIndex> newComponent :
                 componentSR.components().entrySet()) {
            _repository.addComponent(newComponent.getKey(), newComponent.getValue());
        }
        
        return IterUtil.empty();
    }
    
    
    public void run(String componentName) {
        NI.nyi();
    }
    
}
