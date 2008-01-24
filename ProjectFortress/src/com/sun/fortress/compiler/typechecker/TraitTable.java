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
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.compiler.index.TypeConsIndex;


public class TraitTable {
    private ComponentIndex currentComponent;
    private GlobalEnvironment globalEnv;
    
    public TraitTable(ComponentIndex _currentComponent, GlobalEnvironment _globalEnv) {
        currentComponent = _currentComponent;
        globalEnv = _globalEnv;
    }
    public TypeConsIndex typeCons(QualifiedIdName name) {
        Id rawName = name.getName();
        Option<APIName> api = name.getApi();
        
        if (api.isSome()) {
            APIName _api = Option.unwrap(api);
            return globalEnv.api(_api).typeConses().get(rawName);
        }
        else {
            return currentComponent.typeConses().get(rawName);
        }
    }
    
    public ApiIndex api(APIName name) { return globalEnv.api(name); }
}
