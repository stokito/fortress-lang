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

package com.sun.fortress.compiler.index;

import java.util.Map;
import java.util.Set;
import java.util.Collections;
import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.collect.Relation;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes.IdName;
import com.sun.fortress.nodes.FnName;
import com.sun.fortress.nodes.DottedName;

import com.sun.fortress.useful.NI;

/** Comprises {@link ApiIndex} and {@link CompilationUnit}. */
public class CompilationUnitIndex {
    
    private final CompilationUnit _ast;
    private final Map<IdName, Variable> _variables;
    private final Relation<FnName, Function> _functions;
    private final Map<IdName, TypeConsIndex> _typeConses;
    
    public CompilationUnitIndex(CompilationUnit ast,
                                Map<IdName, Variable> variables,
                                Relation<FnName, Function> functions,
                                Map<IdName, TypeConsIndex> typeConses) {
        _ast = ast;
        _variables = Collections.unmodifiableMap(variables);
        _functions = CollectUtil.unmodifiableRelation(functions);
        _typeConses = Collections.unmodifiableMap(typeConses);
    }
    
    public CompilationUnit ast() { return _ast; }
    
    public Set<DottedName> exports() {
        return NI.nyi();
    }
    
    public Set<DottedName> imports() {
        return NI.nyi();
    }
    
    public Map<IdName, Variable> variables() { return _variables; }
    
    public Relation<FnName, Function> functions() { return _functions; }
    
    public Map<IdName, TypeConsIndex> typeConses() { return _typeConses; }
    
}
