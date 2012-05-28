/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.index;

import com.sun.fortress.nodes.*;
import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.collect.Relation;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Comprises {@link ApiIndex} and {@link CompilationUnit}.
 */
public abstract class CompilationUnitIndex {

    private final CompilationUnit _ast;
    private final Map<Id, Variable> _variables;
    private final Relation<IdOrOpOrAnonymousName, Function> _functions;
    private final Set<ParametricOperator> _parametricOperators;
    private final Map<Id, TypeConsIndex> _typeConses;
    private final Map<Id, Dimension> _dimensions;
    private final Map<Id, Unit> _units;
    private final long _modifiedDate;

    public CompilationUnitIndex(CompilationUnit ast, Map<Id, Variable> variables, Relation<IdOrOpOrAnonymousName, Function> functions, Set<ParametricOperator> parametricOperators, Map<Id, TypeConsIndex> typeConses, Map<Id, Dimension> dimensions, Map<Id, Unit> units, long modifiedDate) {
        _ast = ast;
        _variables = CollectUtil.immutable(variables);
        _functions = CollectUtil.immutable(functions);
        _parametricOperators = CollectUtil.immutable(parametricOperators);
        _typeConses = CollectUtil.immutable(CollectUtil.union(typeConses, CollectUtil.union(dimensions, units)));
        _dimensions = CollectUtil.immutable(dimensions);
        _units = CollectUtil.immutable(units);
        _modifiedDate = modifiedDate;
    }

    public CompilationUnit ast() {
        return _ast;
    }

    public APIName name() {
        return ast().getName();
    }

    public abstract Set<APIName> exports();

    public Set<APIName> imports() {
        final Set<APIName> result = new HashSet<APIName>();
        for (Import _import : ast().getImports()) {
            _import.accept(new NodeAbstractVisitor_void() {
                public void forImportedNames(ImportedNames that) {
                    result.add(that.getApiName());
                }
            });
        }
        return result;
    }

    public Set<APIName> comprises() {
        final Set<APIName> result = new HashSet<APIName>();
        for (APIName _apiName : ast().getComprises()) {
            result.add(_apiName);
        }
        return result;
    }

    public Map<Id, Variable> variables() {
        return _variables;
    }

    public Relation<IdOrOpOrAnonymousName, Function> functions() {
        return _functions;
    }

    public Set<ParametricOperator> parametricOperators() {
        return _parametricOperators;
    }

    public Map<Id, TypeConsIndex> typeConses() {
        return _typeConses;
    }

    public Map<Id, Dimension> dimensions() {
        return _dimensions;
    }

    public Map<Id, Unit> units() {
        return _units;
    }

    public long modifiedDate() {
        return _modifiedDate;
    }

    public boolean declared(IdOrOpOrAnonymousName name) {
        if (name instanceof Id) {
            Id id = (Id) name;
            return (_variables.keySet().contains(id) ||
                    _functions.firstSet().contains(id) ||
                    _typeConses.keySet().contains(id) ||
                    _dimensions.keySet().contains(id) ||
                    _units.keySet().contains(id));
        } else if (name instanceof Op) {
            String op = ((Op)name).getText();
            for (IdOrOpOrAnonymousName f : _functions.firstSet()) {
                if (f instanceof Op && ((Op)f).getText().equals(op))
                    return true;
            }
            for (ParametricOperator opr : _parametricOperators) {
                if (opr.name().getText().equals(op)) return true;
            }
            return false;
        } else {
            if (_functions.firstSet().contains(name)) return true;
            else return false;
        }
    }
}
