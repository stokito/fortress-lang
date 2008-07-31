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
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes._InferenceVarType;

import edu.rice.cs.plt.tuple.Option;

/** 
 * A type environment that will conceal bindings from its parent type
 * environments. All bindings contained in this type environment will have no
 * type.
 */
class ConcealingTypeEnv extends TypeEnv {
    private final Set<? extends IdOrOpOrAnonymousName> entries;
    private final TypeEnv parent;
    private final Node declSite;
    
    ConcealingTypeEnv(Node _declSite, Set<? extends IdOrOpOrAnonymousName> _entries, TypeEnv _parent) {
        entries = _entries;
        parent = _parent;
        declSite = _declSite;
    }
    
    /**
     * Return a BindingLookup that binds the given IdOrOpOrAnonymousName to no
     * type (if the given IdOrOpOrAnonymousName is in this type environment).
     */
    public Option<BindingLookup> binding(IdOrOpOrAnonymousName var) {
    	IdOrOpOrAnonymousName no_api_var = removeApi(var);
    	
    	if (!entries.contains(no_api_var)) { return parent.binding(var); }
        return some(new BindingLookup(var, Option.<Type>none()));
    }

    @Override
    public List<BindingLookup> contents() {
        List<BindingLookup> result = new ArrayList<BindingLookup>();
        for (IdOrOpOrAnonymousName name : entries) {
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
    	IdOrOpOrAnonymousName no_api_var = removeApi(var);
    	
    	if (!entries.contains(no_api_var)) { return parent.declarationSite(var); }
        return some(declSite);
	}

	@Override
	public TypeEnv replaceAllIVars(Map<_InferenceVarType, Type> ivars) {
		InferenceVarReplacer rep = new InferenceVarReplacer(ivars);
		
		return new ConcealingTypeEnv((Node)this.declSite.accept(rep),
				entries, 
				parent.replaceAllIVars(ivars));
	}

	@Override
	public Option<StaticParam> staticParam(IdOrOpOrAnonymousName id) {
		return this.parent.staticParam(id);
	}
}