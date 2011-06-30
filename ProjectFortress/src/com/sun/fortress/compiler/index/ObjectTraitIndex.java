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
                            Map<Id, Method> getters,
                            Map<Id, Method> setters,
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

    @Override
    public TypeConsIndex acceptNodeUpdateVisitor(NodeUpdateVisitor v) {
        Option<Constructor> new_constr;
        if (this.constructor().isNone()) new_constr = this.constructor();
        else new_constr = Option.some((Constructor) this.constructor().unwrap().acceptNodeUpdateVisitor(v));

        Map<Id, Variable> new_fields = new HashMap<Id, Variable>();
        for (Map.Entry<Id, Variable> entry : this.fields().entrySet()) {
            Variable var = entry.getValue();
            new_fields.put(entry.getKey(), var.acceptNodeUpdateVisitor(v));
        }

        Set<VarDecl> new_field_ini = new HashSet<VarDecl>();
        for (VarDecl vd : this.fieldInitializers()) {
            new_field_ini.add((VarDecl) vd.accept(v));
        }

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

        Set<Pair<IdOrOpOrAnonymousName, DeclaredMethod>> new_dm =
                new HashSet<Pair<IdOrOpOrAnonymousName, DeclaredMethod>>();
        for (Pair<IdOrOpOrAnonymousName, DeclaredMethod> p : this.dottedMethods()) {
            new_dm.add(Pair.make(p.first(), (DeclaredMethod) p.second().acceptNodeUpdateVisitor(v)));
        }
        Relation<IdOrOpOrAnonymousName, DeclaredMethod> new_dotted = CollectUtil.makeRelation(new_dm);

        Set<Pair<IdOrOpOrAnonymousName, FunctionalMethod>> new_fm =
                new HashSet<Pair<IdOrOpOrAnonymousName, FunctionalMethod>>();
        for (Pair<IdOrOpOrAnonymousName, FunctionalMethod> p : this.functionalMethods()) {
            new_fm.add(Pair.make(p.first(), (FunctionalMethod) p.second().acceptNodeUpdateVisitor(v)));
        }
        Relation<IdOrOpOrAnonymousName, FunctionalMethod> new_functional = CollectUtil.makeRelation(new_fm);

        return new ObjectTraitIndex((ObjectDecl) this.ast().accept(v),
                                    new_constr,
                                    new_fields,
                                    new_field_ini,
                                    new_getters,
                                    new_setters,
                                    new_coercions,
                                    new_dotted,
                                    new_functional);
    }

}
