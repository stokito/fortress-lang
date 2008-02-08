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

package com.sun.fortress.compiler.index;

import java.util.Collections;
import java.util.Map;
import edu.rice.cs.plt.collect.Relation;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.nodes.SimpleName;

public class ApiIndex extends CompilationUnitIndex {
    
    private final Map<QualifiedIdName, GrammarIndex> _grammars;
    
    public ApiIndex(Api ast,
                    Map<Id, Variable> variables,
                    Relation<SimpleName, Function> functions,
                    Map<Id, TypeConsIndex> typeConses,
                    Map<Id, Dimension> dimensions,
                    Map<Id, Unit> units,
                    Map<QualifiedIdName, GrammarIndex> grammars,
                    long modifiedDate) {
        super(ast, variables, functions, typeConses, dimensions, units, modifiedDate);
        _grammars = Collections.unmodifiableMap(grammars);
    }

    public Map<QualifiedIdName, GrammarIndex> grammars() { return _grammars; }
    
}
