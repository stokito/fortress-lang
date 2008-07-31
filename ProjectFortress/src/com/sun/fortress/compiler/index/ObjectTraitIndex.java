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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.ObjectAbsDeclOrDecl;
import com.sun.fortress.nodes.VarDecl;

import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.collect.Relation;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.Pair;

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
                            Relation<IdOrOpOrAnonymousName, Method> dottedMethods,
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
		Option<Constructor> new_constr = this.constructor().isNone() ?
				                         this.constructor() :
				                         Option.some((Constructor)this.constructor().unwrap().acceptNodeUpdateVisitor(v));

		Map<Id,Variable> new_fields = new HashMap<Id,Variable>();
		for( Map.Entry<Id, Variable> entry : this.fields().entrySet() ) {
			Variable var = entry.getValue();
			new_fields.put(entry.getKey(), var.acceptNodeUpdateVisitor(v));
		}
				                    
		Set<VarDecl> new_field_ini = new HashSet<VarDecl>();
		for( VarDecl vd : this.fieldInitializers() ) {
			new_field_ini.add((VarDecl)vd.accept(v));
		}
		
		Map<Id,Method> new_getters = new HashMap<Id,Method>();
		for( Map.Entry<Id, Method> entry : this.getters().entrySet() ) {
			Method var = entry.getValue();
			new_getters.put(entry.getKey(), (Method)var.acceptNodeUpdateVisitor(v));
		}
		
		Map<Id,Method> new_setters = new HashMap<Id,Method>();
		for( Map.Entry<Id, Method> entry : this.setters().entrySet() ) {
			Method var = entry.getValue();
			new_setters.put(entry.getKey(), (Method)var.acceptNodeUpdateVisitor(v));
		}
		
		Set<Function> new_coercions = new HashSet<Function>();
		for( Function vd : this.coercions() ) {
			new_coercions.add((Function)vd.acceptNodeUpdateVisitor(v));
		}
		
		Iterator<Pair<IdOrOpOrAnonymousName, Method>> iter_1 = this.dottedMethods().iterator();
		Set<Pair<IdOrOpOrAnonymousName, Method>> new_dm = new HashSet<Pair<IdOrOpOrAnonymousName, Method>>();
		while(iter_1.hasNext()) {
			Pair<IdOrOpOrAnonymousName, Method> p = iter_1.next();
			new_dm.add(Pair.make(p.first(), (Method)p.second().acceptNodeUpdateVisitor(v)));
		}
		Relation<IdOrOpOrAnonymousName, Method> new_dotted = CollectUtil.makeRelation(new_dm);
		
		Iterator<Pair<IdOrOpOrAnonymousName, FunctionalMethod>> iter_2 = this.functionalMethods().iterator();
		Set<Pair<IdOrOpOrAnonymousName, FunctionalMethod>> new_fm = new HashSet<Pair<IdOrOpOrAnonymousName, FunctionalMethod>>();
		while(iter_1.hasNext()) {
			Pair<IdOrOpOrAnonymousName, FunctionalMethod> p = iter_2.next();
			new_fm.add(Pair.make(p.first(), (FunctionalMethod)p.second().acceptNodeUpdateVisitor(v)));
		}
		Relation<IdOrOpOrAnonymousName, FunctionalMethod> new_functional = CollectUtil.makeRelation(new_fm);
		
		return new ObjectTraitIndex((ObjectAbsDeclOrDecl)this.ast().accept(v),
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
