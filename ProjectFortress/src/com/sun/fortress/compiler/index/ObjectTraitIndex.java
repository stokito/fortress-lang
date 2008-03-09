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

import java.util.Map;
import java.util.Set;
import edu.rice.cs.plt.collect.Relation;
import edu.rice.cs.plt.tuple.Option;
import com.sun.fortress.nodes.ObjectAbsDeclOrDecl;
import com.sun.fortress.nodes.VarDecl;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.SimpleName;

import com.sun.fortress.useful.NI;

/** Wraps an object declaration. */
public class ObjectTraitIndex extends TraitIndex {
    
    private final Option<Constructor> _constructor;
    private final Map<Id, Variable> _fields;
    private final Set<VarDecl> _fieldInitializers;
    
    public ObjectTraitIndex(ObjectAbsDeclOrDecl ast,
                            Option<Constructor> constructor,
                            Map<Id, Variable> fields,
                            Set<VarDecl> fieldInitializers,
                            Map<Id, Method> getters,
                            Map<Id, Method> setters,
                            Set<Function>coercions,
                            Relation<SimpleName, Method> dottedMethods,
                            Relation<SimpleName, FunctionalMethod> functionalMethods) {
        super(ast, getters, setters, coercions, dottedMethods, functionalMethods);
        _constructor = constructor;
        _fields = fields;
        _fieldInitializers = fieldInitializers;
    }
    
    public Option<Constructor> constructor() {
        return NI.nyi();
    }
    
    public Map<Id, Variable> fields() {
        return NI.nyi();
    }
    
    public Set<VarDecl> fieldInitializers() {
        return NI.nyi();
    }
    
}
