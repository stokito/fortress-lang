/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.index;

import com.sun.fortress.nodes.*;
import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.collect.Relation;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.Pair;

import java.util.*;

/**
 * Wraps an object declaration.
 */
public class ObjectTraitIndex extends TraitIndex {

    private final Option<Constructor> _constructor;
    private final Map<Id, Variable> _fields;
    private final Set<VarDecl> _fieldInitializers;

    public ObjectTraitIndex(ObjectDecl ast,
                            Option<Constructor> constructor,
                            Map<Id, Variable> fields,
                            Set<VarDecl> fieldInitializers,
                            Map<Id, FieldGetterMethod> getters,
                            Map<Id, FieldSetterMethod> setters,
                            Set<Coercion> coercions,
                            Relation<IdOrOpOrAnonymousName, DeclaredMethod> dottedMethods,
                            Relation<IdOrOpOrAnonymousName, FunctionalMethod> functionalMethods) {
        super(ast, getters, setters, coercions, dottedMethods, functionalMethods);
        _constructor = constructor;
        _fields = fields;
        _fieldInitializers = fieldInitializers;
    }

    public Option<Constructor> constructor() {
        return _constructor;
    }

    public Map<Id, Variable> fields() {
        return Collections.unmodifiableMap(this._fields);
    }

    public Set<VarDecl> fieldInitializers() {
        return Collections.unmodifiableSet(_fieldInitializers);
    }
    
}
