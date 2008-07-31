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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.sun.fortress.compiler.Types;
import com.sun.fortress.nodes.BoolParam;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdOrOpName;
import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.nodes.IdStaticParam;
import com.sun.fortress.nodes.IntParam;
import com.sun.fortress.nodes.NatParam;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeAbstractVisitor;
import com.sun.fortress.nodes.OpParam;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes._InferenceVarType;

import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.Pair;


/**
 * An environment for static parameters that are allowed to be used as expressions.
 * Nat params, int params, bool params are the best examples, but op params may be
 * useable too. (Note that op params have not yet been implemented.)
 */
public class StaticParamTypeEnv extends TypeEnv {
	
	final static Type STATIC_INT_TYPE = Types.INT_LITERAL;
	final static Type STATIC_NAT_TYPE = Types.INT_LITERAL;
	final static Type STATIC_BOOL_TYPE = Types.BOOLEAN;
	
	final List<StaticParam> entries;
	final private TypeEnv parent;
	
	public StaticParamTypeEnv(List<StaticParam> _static, TypeEnv _parent) {
		entries=Collections.unmodifiableList(new ArrayList<StaticParam>(_static));
		parent=_parent;
	}

	private Option<Pair<StaticParam,Type>> findParam(Id _var) {
        Id no_api_var = removeApi(_var);
        for (StaticParam param : entries) {        	
        	IdOrOpName name = nameFromStaticParam(param);
        	if(name.equals(no_api_var) || name.equals(_var) ){
	        	Option<Type> type_ = typeOfStaticParam(param);
	        	
	        	if( type_.isSome() )
	        		return Option.some(Pair.make(param, type_.unwrap()));
        	}
        }
        return Option.none();
    }
	
	@Override
	public Option<BindingLookup> binding(IdOrOpOrAnonymousName var) {
        if (!(var instanceof Id)) { return parent.binding(var); }
        Id _var = (Id)var;
       
        Option<Pair<StaticParam,Type>> p = findParam(_var);
        
        if( p.isSome() ) {
        	Type type_ = p.unwrap().second();
        	return Option.some(new BindingLookup(var, type_));
        }
        else
        	return parent.binding(_var);
	}

	private static IdOrOpName nameFromStaticParam(StaticParam param) {
		NodeAbstractVisitor<IdOrOpName> get_name = new NodeAbstractVisitor<IdOrOpName>(){
			@Override public IdOrOpName forIdStaticParam(IdStaticParam that) { return that.getName(); }
			@Override public IdOrOpName forOpParam(OpParam that) { return that.getName(); }
		};
		
		IdOrOpName name = param.accept(get_name);
		return name;
	}

	private static Option<Type> typeOfStaticParam(StaticParam param) {
		NodeAbstractVisitor<Option<Type>> get_type = new NodeAbstractVisitor<Option<Type>>(){

			@Override 
			public Option<Type> defaultCase(Node node) {
				return Option.none();
			}
			
			@Override
			public Option<Type> forBoolParam(BoolParam that) {
				return Option.some(STATIC_BOOL_TYPE);
			}

			@Override
			public Option<Type> forIntParam(IntParam that) {
				return Option.some(STATIC_INT_TYPE);
			}

			@Override
			public Option<Type> forNatParam(NatParam that) {
				return Option.some(STATIC_NAT_TYPE);
			}

		};
		Option<Type> type = param.accept(get_type);
		return type;
	}

	@Override
	public List<BindingLookup> contents() {
		List<BindingLookup> result = new ArrayList<BindingLookup>();
		
		for( StaticParam param : entries ) {
			Option<Type> type_ = typeOfStaticParam(param);
			if( type_.isSome() )
				result.add(new BindingLookup(nameFromStaticParam(param), type_.unwrap()));
		}
		return result;
	}

	@Override
	public Option<Node> declarationSite(IdOrOpOrAnonymousName var) {
        if (!(var instanceof Id)) { return parent.declarationSite(var); }
        Id _var = (Id)var;
       
        Option<Pair<StaticParam,Type>> p = findParam(_var);
        if( p.isSome() )
        	return Option.<Node>some(p.unwrap().first());
        else
        	return parent.declarationSite(var);
	}

	@Override
	public TypeEnv replaceAllIVars(Map<_InferenceVarType, Type> ivars) {
		List<StaticParam> new_entries = new ArrayList<StaticParam>(entries.size());
		InferenceVarReplacer rep = new InferenceVarReplacer(ivars);
		
		for( StaticParam entry : entries ) {
			new_entries.add((StaticParam)entry.accept(rep));
		}
		
		return new StaticParamTypeEnv(new_entries, parent.replaceAllIVars(ivars));
	}

}
