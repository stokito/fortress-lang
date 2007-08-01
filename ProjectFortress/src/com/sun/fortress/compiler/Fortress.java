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

public abstract class Fortress {

  protected abstract Map<String, ApiIndex> apis();

  protected abstract void addApi(String name, ApiIndex definition);
  
  protected abstract void addComponent(String name, ComponentIndex definition);
  
  /**
   * Compile all definitions in the given files, and any additional sources that they depend on,
   * and add them to the fortress.
   */
  public Iterable<? extends StaticError> compile(Iterable<File> files) {
    GlobalEnvironment env = new GlobalEnvironment(apis());
    
    Parser.Result pr = Parser.parse(files, env);
    if (!pr.isSuccessful()) { return pr.errors(); }
    
    IndexBuilder.Result ir = IndexBuilder.build(pr.asts());
    if (!ir.isSuccessful()) { return ir.errors(); }
    
    // Handle APIs first
    
    Disambiguator.ApiResult drForApis =
      Disambiguator.disambiguateApis(ir.apis(),
                                     new GlobalEnvironment(CollectUtil.compose(apis(), ir.apis())));
    if (!drForApis.isSuccessful()) { return drForApis.errors(); }
    
    StaticChecker.ApiResult crForApis =
      StaticChecker.checkApis(drForApis.apis(),
                              new GlobalEnvironment(CollectUtil.compose(apis(), drForApis.apis())));
    if (!crForApis.isSuccessful()) { return crForApis.errors(); }
    
    for (Map.Entry<String, ApiIndex> newApi : drForApis.apis().entrySet()) {
      addApi(newApi.getKey(), newApi.getValue());
    }
    
    // Handle components
    
    Disambiguator.ComponentResult dr = Disambiguator.disambiguateComponents(ir.components(), env);
    if (!dr.isSuccessful()) { return dr.errors(); }
        
    StaticChecker.ComponentResult cr = StaticChecker.checkComponents(dr.components(), env);
    if (!cr.isSuccessful()) { return cr.errors(); }
    
    // Additional optimization phases can be inserted here
    
    for (Map.Entry<String, ComponentIndex> newComponent : cr.components().entrySet()) {
      addComponent(newComponent.getKey(), newComponent.getValue());
    }
    
    return IterUtil.empty();
  }
      
  public void run(String componentName) {
    throw new RuntimeException("Not implemented");
  }

  
}
