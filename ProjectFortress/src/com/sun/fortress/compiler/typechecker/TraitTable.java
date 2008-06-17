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

    public Option<TypeConsIndex> typeCons(Id name) {
        TypeConsIndex result;
        Id simpleName = new Id(name.getText());
        // TODO: Shouldn't qualified names only point to APIs? -- Dan
        if (name.getApi().isNone() ||
            currentComponent.ast().getName().equals(name.getApi().unwrap())) {
            result = currentComponent.typeConses().get(simpleName);
        }
        else {
            ApiIndex api = globalEnv.api(name.getApi().unwrap());
            if (api == null) {
            	return Option.none();
            }
            result = api.typeConses().get(simpleName);
        }
        if (result == null) {
        	return Option.none();
        }
        return Option.some(result);
    }

    public CompilationUnitIndex compilationUnit(APIName name) {
        if (currentComponent.ast().getName().equals(name)) {
            return currentComponent;
        } else {
            return globalEnv.api(name);
        }
    }
}
