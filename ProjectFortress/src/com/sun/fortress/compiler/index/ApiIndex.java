/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.index;

import com.sun.fortress.nodes.*;
import edu.rice.cs.plt.collect.Relation;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class ApiIndex extends CompilationUnitIndex {

    private final Map<String, GrammarIndex> _grammars;

    public ApiIndex(Api ast, Map<Id, Variable> variables, Relation<IdOrOpOrAnonymousName, Function> functions, Set<ParametricOperator> parametricOperators, Map<Id, TypeConsIndex> typeConses, Map<Id, Dimension> dimensions, Map<Id, Unit> units, Map<String, GrammarIndex> grammars, long modifiedDate) {
        super(ast, variables, functions, parametricOperators, typeConses, dimensions, units, modifiedDate);
        _grammars = Collections.unmodifiableMap(grammars);
    }

    public Map<String, GrammarIndex> grammars() {
        return _grammars;
    }

    public String toString() {
        return "API Index" + "\nVariables: " + variables() + "\nFunctions: " + functions() + "\nTypeConses: " + typeConses() + "\nDimensions: " + dimensions() + "\nUnits: " + units() + "\nGrammars: " + grammars();
    }

    @Override
    public Set<APIName> exports() {
        return Collections.emptySet();
    }

    @Override
    public boolean declared(IdOrOpOrAnonymousName name) {
        if (super.declared(name)) return true;
        else if (name instanceof IdOrOp) return _grammars.keySet().contains(((IdOrOp) name).getText());
        else return false;
    }

}
