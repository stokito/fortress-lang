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
import edu.rice.cs.plt.tuple.Pair;

import java.util.*;

/**
 * Wraps a (non-object) trait declaration.
 */
public class ProperTraitIndex extends TraitIndex {

    Set<TraitType> _excludes = new HashSet<TraitType>();
    Set<NamedType> _comprises = new HashSet<NamedType>();

    public ProperTraitIndex(TraitDecl ast, Map<Id, FieldGetterMethod> getters, Map<Id, FieldSetterMethod> setters, Set<Coercion> coercions,
            Relation<IdOrOpOrAnonymousName, DeclaredMethod> dottedMethods,
            Relation<IdOrOpOrAnonymousName, FunctionalMethod> functionalMethods) {
        super(ast, getters, setters, coercions, dottedMethods, functionalMethods);
    }

    public Set<TraitType> excludesTypes() {
        return _excludes;
    }

    public Set<NamedType> comprisesTypes() {
        return _comprises;
    }

    public void addExcludesType(TraitType t) {
        _excludes.add(t);
    }

    public void addComprisesType(NamedType t) {
        _comprises.add(t);
    }

}
