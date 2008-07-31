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

package com.sun.fortress.compiler.typechecker;

import static edu.rice.cs.plt.tuple.Option.some;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.fortress.nodes.FnDef;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.nodes.IntersectionType;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes._InferenceVarType;

import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.collect.Relation;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.Pair;

/**
 * A type environment whose outermost scope binds local function definitions.
 */
class FnDefTypeEnv extends TypeEnv {
    private Relation<IdOrOpOrAnonymousName, ? extends FnDef> entries;
    private TypeEnv parent;

    FnDefTypeEnv(Relation<IdOrOpOrAnonymousName, ? extends FnDef> _entries, TypeEnv _parent) {
        entries = _entries;
        parent = _parent;
    }

    /**
     * Return a BindingLookup that binds the given IdOrOpOrAnonymousName to a type
     * (if the given IdOrOpOrAnonymousName is in this type environment).
     */
    public Option<BindingLookup> binding(IdOrOpOrAnonymousName var) {
    	IdOrOpOrAnonymousName no_api_name = removeApi(var);
    	
        Set<? extends FnDef> fns = entries.matchFirst(no_api_name);
        if (fns.isEmpty()) {
            if (var instanceof Id) {
                Id _var = (Id)var;
                if (_var.getApi().isSome())
                    return binding(new Id(_var.getSpan(), _var.getText()));
            }
            return parent.binding(var);
        }

        LinkedList<Type> overloadedTypes = new LinkedList<Type>();
        for (FnDef fn : fns) {
            overloadedTypes.add(genericArrowFromDecl(fn));
        }
        return some(new BindingLookup(var, new IntersectionType(overloadedTypes)));
    }

    @Override
    public List<BindingLookup> contents() {
        List<BindingLookup> result = new ArrayList<BindingLookup>();
        for (IdOrOpOrAnonymousName name : entries.firstSet()) {
            Option<BindingLookup> element = binding(name);
            if (element.isSome()) {
                result.add(element.unwrap());
            }
        }
        result.addAll(parent.contents());
        return result;
    }

	@Override
	public Option<Node> declarationSite(IdOrOpOrAnonymousName var) {
    	IdOrOpOrAnonymousName no_api_name = removeApi(var);
    	
        Set<? extends FnDef> fns = entries.matchFirst(no_api_name);
        if (fns.isEmpty()) {
            if (var instanceof Id) {
                Id _var = (Id)var;
                if (_var.getApi().isSome())
                    return declarationSite(new Id(_var.getSpan(), _var.getText()));
            }
            return parent.declarationSite(var);
        }
		
		throw new IllegalArgumentException("The declarationSite method should not be called on functions.");
	}

	@Override
	public TypeEnv replaceAllIVars(Map<_InferenceVarType, Type> ivars) {
		Iterator<? extends Pair<IdOrOpOrAnonymousName, ? extends FnDef>> iter = this.entries.iterator();
		Set<Pair<IdOrOpOrAnonymousName, FnDef>> new_entries_ = new HashSet<Pair<IdOrOpOrAnonymousName, FnDef>>();
		
		InferenceVarReplacer rep = new InferenceVarReplacer(ivars);
		
		while( iter.hasNext() ) {
			Pair<IdOrOpOrAnonymousName, ? extends FnDef> p = iter.next();
			FnDef f = p.second();
			
			f = (FnDef)f.accept(rep);
			new_entries_.add(Pair.make(p.first(), f));
		}
		
		return new FnDefTypeEnv(CollectUtil.makeRelation(new_entries_),
				                parent.replaceAllIVars(ivars));
	}

	@Override
	public Option<StaticParam> staticParam(IdOrOpOrAnonymousName id) {
		return this.parent.staticParam(id);
	}
}
