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

import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes._InferenceVarType;

import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;

/**
 * A type environment whose outermost lexical scope consists of a map from IDs
 * to Variables.
 */
class ParamTypeEnv extends TypeEnv {
    private List<Param> entries;
    private TypeEnv parent;

    ParamTypeEnv(List<Param> _entries, TypeEnv _parent) {
        entries = _entries;
        parent = _parent;
    }

    private Option<Param> findParam(Id var) {
        Id no_api_var = removeApi(var);
        
        for (Param param : entries) {
            if (param.getName().getText().equals(no_api_var.getText())) {
                return some(param);
            }
        }
        return none();
    }
    
    /**
     * Return a BindingLookup that binds the given IdOrOpOrAnonymousName to a type
     * (if the given IdOrOpOrAnonymousName is in this type environment).
     */
    public Option<BindingLookup> binding(IdOrOpOrAnonymousName var) {
    	if (!(var instanceof Id)) { return parent.binding(var); }
    	Id _var = (Id)var;
    	
    	Option<Param> _p = findParam(_var); 
    	
    	if( _p.isSome() ) {
    		Param p = _p.unwrap();
    		return some(new BindingLookup(_var, typeFromParam(p), p.getMods()));
    	}
    	else
    		return parent.binding(_var);
    }

    @Override
    public List<BindingLookup> contents() {
        List<BindingLookup> result = new ArrayList<BindingLookup>();
        for (Param param : entries) {
            result.add(new BindingLookup(param.getName(), typeFromParam(param), param.getMods()));
        }
        result.addAll(parent.contents());
        return result;
    }

	@Override
	public Option<Node> declarationSite(IdOrOpOrAnonymousName var) {
    	if (!(var instanceof Id)) { return parent.declarationSite(var); }
    	Id _var = (Id)var;
    	
    	Option<Param> _p = findParam(_var); 
    	if( _p.isSome() )
    		return Option.<Node>some(_p.unwrap());
    	else
    		return parent.declarationSite(var);
	}

	@Override
	public TypeEnv replaceAllIVars(Map<_InferenceVarType, Type> ivars) {
		
		List<Param> new_entries = new ArrayList<Param>(entries.size());
		InferenceVarReplacer rep = new InferenceVarReplacer(ivars);
		
		for( Param p : entries ) {
			new_entries.add((Param)p.accept(rep));
		}
		
		return new ParamTypeEnv(new_entries, parent.replaceAllIVars(ivars));
	}

	@Override
	public Option<StaticParam> staticParam(IdOrOpOrAnonymousName id) {
		return this.parent.staticParam(id);
	}
}
