/*******************************************************************************
    Copyright 2009 Sun Microsystems, Inc.,
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
package com.sun.fortress.compiler.codegen;

import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.compiler.index.Function;
import com.sun.fortress.compiler.index.FunctionalMethod;
import com.sun.fortress.exceptions.CompilerError;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.*;
import com.sun.fortress.repository.FortressRepository;
import com.sun.fortress.useful.Debug;
import edu.rice.cs.plt.collect.PredicateSet;
import edu.rice.cs.plt.collect.Relation;
import edu.rice.cs.plt.tuple.Option;

import java.util.*;

public class Symbols {
    ComponentIndex ci;
    Map<APIName, ApiIndex> apis = new HashMap<APIName, ApiIndex>();
    // Hopefully there is only one of these, but why not be general.
    Map<APIName, ComponentIndex> components = new HashMap<APIName, ComponentIndex>();

    public void addApi(APIName apiName, ApiIndex apiIndex) {
        apis.put(apiName, apiIndex);
    }

    public void addComponent(APIName apiName, ComponentIndex componentIndex) {
        components.put(apiName, componentIndex);
    }

    public Function getFunctionForSymbol(IdOrOp fnName) {
        Option<APIName> maybe_api = fnName.getApiName();
        
        if (maybe_api.isSome()) {
            APIName apiName = maybe_api.unwrap();
            Debug.debug(Debug.Type.CODEGEN, 1, 
                        "getFunctionForSymbol" + apiName + ":" + fnName);
            if (apis.containsKey(apiName)) {
                ApiIndex ind = apis.get(apiName);            
                Debug.debug(Debug.Type.CODEGEN, 1, 
                            "getFunctionForSymbol" + apiName + ":" + fnName + "::" + ind);
            
                PredicateSet<IdOrOpOrAnonymousName> first = ind.functions().firstSet();

                // The following code is meant to work a known issue.
                // The IdOrOp added when processing an api, is not the same IdOrOp
                // we are searching for when processing a component.  We need to
                // match on the strings, not on the objects. 
                IdOrOp matchingFnName = fnName;

                for (IdOrOpOrAnonymousName name : first) {
                    if (name instanceof IdOrOp) {
                        IdOrOp foo = (IdOrOp) name;
                        if (foo.getText() == fnName.getText()) {
                            matchingFnName = foo;
                        }
                    }
                }
                PredicateSet<Function> functions = ind.functions().matchFirst(matchingFnName);                
                for (Function f : functions) {
                    Debug.debug(Debug.Type.CODEGEN, 1, 
                                "getJavaClassForSymbol contains key" + apiName + " f = " + f);
                    return f;
                }
            }
            throw new CompilerError(NodeUtil.getSpan(fnName), "Cannot find function: " + fnName + 
                                    " in api " + apiName);
        }

        throw new CompilerError(NodeUtil.getSpan(fnName), "Cannot find function: " + fnName);
    }


    public String getJavaClassForSymbol(IdOrOp fnName) {
        Debug.debug(Debug.Type.CODEGEN, 1, 
                    "getJavaClassForSymbo:" + fnName);
        Function f = getFunctionForSymbol(fnName);

        if (f instanceof FunctionalMethod) {
            FunctionalMethod fm = (FunctionalMethod) f;
            Id i = fm.declaringTrait();
            return i.getText();
        } 
        throw new RuntimeException("WTF");
    }

    public String toString() {
        String result = "Symbols:";
        for (APIName api : apis.keySet()) {
            ApiIndex index = apis.get(api);
            result = result + "api::" + api.getText() + ":::" + index;
        }
        for (APIName api : components.keySet()) {
            ComponentIndex index = components.get(api);
            result = result + "component:" + api.getText() + ":::" + index;
        }
        return result;
    }
}        
        