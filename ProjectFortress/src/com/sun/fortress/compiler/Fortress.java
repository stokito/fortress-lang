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
        
        // Build APIs before disambiguating to allow circular references.
        IndexBuilder.ApiResult rawApiIR = IndexBuilder.buildApis(pr.apis());
        if (!rawApiIR.isSuccessful()) { return rawApiIR.errors(); }
        
        GlobalEnvironment rawApiEnv =
            new GlobalEnvironment(CollectUtil.compose(_repository.apis(),
                                                      rawApiIR.apis()));
        Disambiguator.ApiResult apiDR =
            Disambiguator.disambiguateApis(pr.apis(), rawApiEnv);
        if (!apiDR.isSuccessful()) { return apiDR.errors(); }
        
        IndexBuilder.ApiResult apiIR = IndexBuilder.buildApis(apiDR.apis());
        if (!apiIR.isSuccessful()) { return apiIR.errors(); }
        
        GlobalEnvironment apiEnv =
            new GlobalEnvironment(CollectUtil.compose(_repository.apis(),
                                                      apiIR.apis()));
        StaticChecker.ApiResult apiSR =
            StaticChecker.checkApis(apiIR.apis(), apiEnv);
        if (!apiSR.isSuccessful()) { return apiSR.errors(); }
        
        for (Map.Entry<String, ApiIndex> newApi : apiIR.apis().entrySet()) {
            _repository.addApi(newApi.getKey(), newApi.getValue());
        }
        
        // Handle components
        
        Disambiguator.ComponentResult componentDR =
            Disambiguator.disambiguateComponents(pr.components(), env);
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
