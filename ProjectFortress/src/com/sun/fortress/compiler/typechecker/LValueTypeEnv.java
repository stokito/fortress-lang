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

import static edu.rice.cs.plt.tuple.Option.none;
import static edu.rice.cs.plt.tuple.Option.some;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.nodes.LValue;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes._InferenceVarType;

import edu.rice.cs.plt.tuple.Option;

class LValueTypeEnv extends TypeEnv {
    private final LValue[] entries;
    private final TypeEnv parent;

    LValueTypeEnv(LValue[] _entries, TypeEnv _parent) {
        entries = _entries;
        parent = _parent;
    }

    LValueTypeEnv(List<LValue> _entries, TypeEnv _parent) {
        entries = _entries.toArray(new LValue[_entries.size()]);
        parent = _parent;
    }

    private Option<LValue> findLVal(IdOrOpOrAnonymousName var) {
    	IdOrOpOrAnonymousName no_api_var = removeApi(var);

    	for (LValue entry : entries) {
            if (var.equals(entry.getName()) || no_api_var.equals(entry.getName())) {
                return some(entry);
            }
        }
    	return none();
    }

    /**
     * Return a BindingLookup that binds the given IdOrOpOrAnonymousName to a type
     * (if the given IdOrOpOrAnonymousName is in this type environment).
     */
    public Option<BindingLookup> binding(IdOrOpOrAnonymousName var) {
    	Option<LValue> lval = findLVal(var);

    	if( lval.isSome() )
    		return some(new BindingLookup(lval.unwrap()));
    	else
    		return parent.binding(var);
    }

    @Override
    public List<BindingLookup> contents() {
        List<BindingLookup> result = new ArrayList<BindingLookup>();
        for (LValue entry : entries) {
            result.add(new BindingLookup(entry));
        }
        result.addAll(parent.contents());
        return result;
    }

	@Override
	public Option<Node> declarationSite(IdOrOpOrAnonymousName var) {
		Option<LValue> lval = findLVal(var);

		if( lval.isSome() )
			return Option.<Node>some(lval.unwrap());
		else
			return parent.declarationSite(var);
	}

	@Override
	public TypeEnv replaceAllIVars(Map<_InferenceVarType, Type> ivars) {
		LValue[] new_entries = new LValue[entries.length];

		InferenceVarReplacer rep = new InferenceVarReplacer(ivars);

		for( int i = 0; i<entries.length; i++ ) {
			new_entries[i] = (LValue)entries[i].accept(rep);
		}

		return new LValueTypeEnv(new_entries, parent.replaceAllIVars(ivars));
	}

	@Override
	public Option<StaticParam> staticParam(IdOrOpOrAnonymousName id) {
		return parent.staticParam(id);
	}
}
