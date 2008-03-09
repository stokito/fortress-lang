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

package com.sun.fortress.compiler.typechecker;

import java.util.*;
import edu.rice.cs.plt.tuple.Option;

import com.sun.fortress.nodes.*;
import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.index.CompilationUnitIndex;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.compiler.index.TypeConsIndex;


public class TraitTable {
    private ComponentIndex currentComponent;
    private GlobalEnvironment globalEnv;
    
    public TraitTable(ComponentIndex _currentComponent, GlobalEnvironment _globalEnv) {
        currentComponent = _currentComponent;
        globalEnv = _globalEnv;
    }
    
    public TypeConsIndex typeCons(Id name) { 
        return currentComponent.typeConses().get(name);
    }
    
    public TypeConsIndex typeCons(QualifiedIdName name) {
        Id rawName = name.getName();
        Option<APIName> api = name.getApi();
        if (api.isNone() || currentComponent.ast().getName().equals(Option.unwrap(api))) {
            return currentComponent.typeConses().get(rawName);
        } else {
            return globalEnv.api(Option.unwrap(api)).typeConses().get(rawName);
        }
    }
    
    public CompilationUnitIndex compilationUnit(APIName name) {
        if (currentComponent.ast().getName().equals(name)) {
            return currentComponent;
        } else {
            return globalEnv.api(name);
        }
    }
}
