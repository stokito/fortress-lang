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

import com.sun.fortress.compiler.index.*;
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
    Map<APIName, ApiIndex> apis = new HashMap<APIName, ApiIndex>();
    // Hopefully there is only one of these, but why not be general.
    Map<APIName, ComponentIndex> components = new HashMap<APIName, ComponentIndex>();

    public void addApi(APIName apiName, ApiIndex apiIndex) {
        //System.out.println("XXXXX add api " + apiName + " component " + apiIndex);
        apis.put(apiName, apiIndex);
    }

    public void addComponent(APIName apiName, ComponentIndex componentIndex) {
        //System.out.println("XXXXX add component " + apiName + " component " + componentIndex);
        components.put(apiName, componentIndex);
    }

    private <T> T sayWhat(ASTNode x) {
        throw new CompilerError(NodeUtil.getSpan(x), "Can't compile " + x + " of class " + x.getClass());
    }

    private <T> T sayWhat(ASTNode x, String message) {
        throw new CompilerError(NodeUtil.getSpan(x), message + " node = " + x);
    }



    public String getTypeSignatureForIdOrOp(IdOrOp op, Component c) {
        Function f = getFunction(op, c);
        String desc = "";
        if (f instanceof FunctionalMethod) {
            FunctionalMethod fm = (FunctionalMethod) f;
            List<Param> params = f.parameters();
            Type returnType = f.getReturnType();
            desc = Naming.openParen;
            for (Param p : params) {
                Id paramName = p.getName();
                Option<com.sun.fortress.nodes.Type> optionType = p.getIdType();
                if (optionType.isNone())
                    sayWhat(op);
                else {
                    com.sun.fortress.nodes.Type t = optionType.unwrap();
                    desc = desc + Naming.emitDesc(t);
                }
            }
            desc = desc + ")" + Naming.emitDesc(returnType);
        } else if (f instanceof Constructor) {
            throw new CompilerError("We can't generate code for constructors yet");
        } else if (f instanceof DeclaredFunction) {
            List<Param> params = f.parameters();
            Type returnType = f.getReturnType();
            desc = Naming.openParen;
            for (Param p : params) {
                Id paramName = p.getName();
                Option<com.sun.fortress.nodes.Type> optionType = p.getIdType();
                if (optionType.isNone())
                    sayWhat(op);
                else {
                    com.sun.fortress.nodes.Type t = optionType.unwrap();
                    desc = desc + Naming.emitDesc(t);
                }
            }
            desc = desc + ")" + Naming.emitDesc(returnType);
        } else sayWhat(op);
        return desc;
    }

    public String getJavaClassForSymbol(IdOrOp fnName, Component component) {
        Debug.debug(Debug.Type.CODEGEN, 1,
                    "getJavaClassForSymbol:" + fnName);
        Function f = getFunction(fnName, component);

        if (f instanceof FunctionalMethod) {
            FunctionalMethod fm = (FunctionalMethod) f;
            Id i = fm.declaringTrait();
            return i.getText();
        }

        throw new CompilerError(NodeUtil.getSpan(fnName), "Get Java Class For Symbol Not yet implemented");
    }

    // This works around the issue with IdOrOps not matching the table provided by the type checker.
    public IdOrOp lookupFunctionInPredicateSet(IdOrOp fnName, PredicateSet<IdOrOpOrAnonymousName> predSet) {
        for (IdOrOpOrAnonymousName name : predSet) {
                if (name instanceof IdOrOp) {
                    IdOrOp foo = (IdOrOp) name;
                    Debug.debug(Debug.Type.CODEGEN, 1,
                                "lookupFunctionInPredicateSet:name = ", name,
                                " fnName = ", fnName);
                    if (foo.getText() == fnName.getText()) {
                        return foo;
                    }
                }
        }
        throw new CompilerError(NodeUtil.getSpan(fnName), "Cannot find function " + fnName + " in predicate set");
    }

    public Function lookupFunctionInApi(IdOrOp fnName, APIName api) {
        if (apis.containsKey(api)) {
            ApiIndex ind = apis.get(api);
            PredicateSet<IdOrOpOrAnonymousName> first = ind.functions().firstSet();
            IdOrOp n = lookupFunctionInPredicateSet(fnName, first);
            PredicateSet<Function> functions = ind.functions().matchFirst(n);
            // Someday we will do overloading here
            for (Function f : functions) {
                return f;
            }
            //System.out.println("Got to here with ind = " + ind + " first = " + first + " n = " + n + " functions = " + functions);
        }

        //System.out.println("apis = " + apis);

        throw new CompilerError(NodeUtil.getSpan(fnName), "Cannot find function " + fnName + " in Api " + api);
    }

    public Function lookupFunctionInComponent(IdOrOp fnName, ComponentIndex ind) {
        Debug.debug(Debug.Type.CODEGEN, 1, "lookupFunctionInComponent:name = " + fnName + " component = " + ind);
        PredicateSet<IdOrOpOrAnonymousName> first = ind.functions().firstSet();
        IdOrOp n = lookupFunctionInPredicateSet(fnName, first);
        PredicateSet<Function> functions = ind.functions().matchFirst(n);
        // Someday we will do overloading here
        for (Function f : functions) {
            return f;
        }
        throw new CompilerError(NodeUtil.getSpan(fnName), "Cannot find function " + fnName + " in component");
    }

    public Function getFunction(IdOrOp fnName, Component component) {
        Option<APIName> maybe_api = fnName.getApiName();
        IdOrOp id = fnName;
        if (maybe_api.isSome()) {
            return lookupFunctionInApi(fnName, maybe_api.unwrap());
        } else {
            ComponentIndex ind = components.get(component.getName());
            return lookupFunctionInComponent(fnName, ind);
        }
    }

    public boolean isFunctionalMethod(IdOrOp fnName, Component component) {
        Option<APIName> maybe_api = fnName.getApiName();
        Function f = getFunction(fnName, component);
        if (f instanceof FunctionalMethod)
            return true;
        else return false;
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
