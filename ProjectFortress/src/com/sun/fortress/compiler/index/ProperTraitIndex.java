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

    public ProperTraitIndex(TraitDecl ast, Map<Id, Method> getters, Map<Id, Method> setters, Set<Coercion> coercions, Relation<IdOrOpOrAnonymousName, DeclaredMethod> dottedMethods, Relation<IdOrOpOrAnonymousName, FunctionalMethod> functionalMethods) {
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

    @Override
    public TypeConsIndex acceptNodeUpdateVisitor(NodeUpdateVisitor v) {
        Map<Id, Method> new_getters = new HashMap<Id, Method>();
        for (Map.Entry<Id, Method> entry : this.getters().entrySet()) {
            Method var = entry.getValue();
            new_getters.put(entry.getKey(), (Method) var.acceptNodeUpdateVisitor(v));
        }
        Map<Id, Method> new_setters = new HashMap<Id, Method>();
        for (Map.Entry<Id, Method> entry : this.setters().entrySet()) {
            Method var = entry.getValue();
            new_setters.put(entry.getKey(), (Method) var.acceptNodeUpdateVisitor(v));
        }
        Set<Coercion> new_coercions = new HashSet<Coercion>();
        for (Coercion vd : this.coercions()) {
            new_coercions.add((Coercion) vd.acceptNodeUpdateVisitor(v));
        }
        Set<Pair<IdOrOpOrAnonymousName, DeclaredMethod>> new_dm = new HashSet<Pair<IdOrOpOrAnonymousName, DeclaredMethod>>();
        for (Pair<IdOrOpOrAnonymousName, DeclaredMethod> p : this.dottedMethods()) {
            new_dm.add(Pair.make(p.first(), (DeclaredMethod) p.second().acceptNodeUpdateVisitor(v)));
        }
        Relation<IdOrOpOrAnonymousName, DeclaredMethod> new_dotted = CollectUtil.makeRelation(new_dm);
        Set<Pair<IdOrOpOrAnonymousName, FunctionalMethod>> new_fm = new HashSet<Pair<IdOrOpOrAnonymousName, FunctionalMethod>>();
        for (Pair<IdOrOpOrAnonymousName, FunctionalMethod> p : this.functionalMethods()) {
            new_fm.add(Pair.make(p.first(), (FunctionalMethod) p.second().acceptNodeUpdateVisitor(v)));
        }
        Relation<IdOrOpOrAnonymousName, FunctionalMethod> new_functional = CollectUtil.makeRelation(new_fm);

        return new ProperTraitIndex((TraitDecl) this.ast().accept(v), new_getters, new_setters, new_coercions, new_dotted, new_functional);
    }
}
