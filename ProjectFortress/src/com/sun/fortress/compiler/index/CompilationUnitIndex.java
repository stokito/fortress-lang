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

import java.util.*;
import java.util.Collections;
import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.collect.Relation;
import com.sun.fortress.nodes.*;

import com.sun.fortress.useful.NI;

/** Comprises {@link ApiIndex} and {@link CompilationUnit}. */
public abstract class CompilationUnitIndex {
    
    private final CompilationUnit _ast;
    private final Map<IdName, Variable> _variables;
    private final Relation<SimpleName, Function> _functions;
    private final Map<IdName, TypeConsIndex> _typeConses;
    private final long _modifiedDate;
    
    public CompilationUnitIndex(CompilationUnit ast,
                                Map<IdName, Variable> variables,
                                Relation<SimpleName, Function> functions,
                                Map<IdName, TypeConsIndex> typeConses,
                                long modifiedDate) {
        _ast = ast;
        _variables = Collections.unmodifiableMap(variables);
        _functions = CollectUtil.unmodifiableRelation(functions);
        _typeConses = Collections.unmodifiableMap(typeConses);
        _modifiedDate = modifiedDate;
    }
    
    public CompilationUnit ast() { return _ast; }
    
    public Set<DottedName> exports() {
        return NI.nyi();
    }
    
    public Set<DottedName> imports() {
        final Set<DottedName> result = new HashSet<DottedName>();
        for (Import _import : ast().getImports()) {
            _import.accept(new NodeAbstractVisitor_void() {
                public void forImportFrom(ImportFrom that) {
                    result.add(that.getApi());
                }
            });
        }
        return result;
    }
    
    public Map<IdName, Variable> variables() { return _variables; }
    
    public Relation<SimpleName, Function> functions() { return _functions; }
    
    public Map<IdName, TypeConsIndex> typeConses() { return _typeConses; }
    
    public long modifiedDate() { return _modifiedDate; }
    
}
