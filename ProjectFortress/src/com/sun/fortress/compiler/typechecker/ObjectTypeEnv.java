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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.fortress.compiler.index.ObjectTraitIndex;
import com.sun.fortress.compiler.index.TypeConsIndex;
import com.sun.fortress.nodes.ArrowType;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.ObjectAbsDeclOrDecl;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes._InferenceVarType;
import com.sun.fortress.nodes._RewriteGenericArrowType;
import com.sun.fortress.nodes_util.NodeFactory;

import edu.rice.cs.plt.tuple.Option;

/**
 * A type environment whose outermost lexical scope consists of a map from IDs
 * to ObjectTraitIndex.
 */
class ObjectTypeEnv extends TypeEnv {
    private Map<Id, TypeConsIndex> entries;
    private TypeEnv parent;

    ObjectTypeEnv(Map<Id, TypeConsIndex> _entries, TypeEnv _parent) {
        entries = _entries;
        parent = _parent;
    }

    /**
     * Return a BindingLookup that binds the given Id to a type
     * (if the given Id is in this type environment).
     */
    public Option<BindingLookup> binding(IdOrOpOrAnonymousName var) {
        if (!(var instanceof Id)) { return parent.binding(var); }
        Id _var = (Id)var;

        // Api-less name used for look-up only.
        Id no_api_var = removeApi(_var);
        
        if (!entries.containsKey(no_api_var)) { return parent.binding(var); }
        TypeConsIndex typeCons = entries.get(no_api_var);
        
        // TODO: This seems wrong... If they were looking for an Object, but found some
        // other kind of type, isn't there some way we could return a better error message?
        if (!(typeCons instanceof ObjectTraitIndex)) { return parent.binding(var); }
        ObjectTraitIndex objIndex = (ObjectTraitIndex)typeCons;
        
        Type type;
        ObjectAbsDeclOrDecl decl = (ObjectAbsDeclOrDecl)objIndex.ast();
        if (decl.getStaticParams().isEmpty()) {
            if (decl.getParams().isNone()) {
                // No static params, no normal params
                type = NodeFactory.makeTraitType(_var);
            } else {
                // No static params, some normal params
                type = new ArrowType(var.getSpan(),
                                     domainFromParams(decl.getParams().unwrap()),
                                     NodeFactory.makeTraitType(_var));
            }
        } else {
            if (decl.getParams().isNone()) {
                // Some static params, no normal params
                type = NodeFactory.makeGenericSingletonType(_var, decl.getStaticParams());
            } else {
                // Some static params, some normal params
                // TODO: handle type variables bound in where clause
                type = 
                	new _RewriteGenericArrowType(decl.getSpan(), decl.getStaticParams(),
                                                 domainFromParams(decl.getParams().unwrap()),
                                                 NodeFactory.makeTraitType(_var, TypeEnv.staticParamsToArgs(decl.getStaticParams())),
                                                 decl.getWhere());
            }
        }

        return Option.some(new BindingLookup(var, type, decl.getMods()));   
    }

    @Override
    public List<BindingLookup> contents() {
        List<BindingLookup> result = new ArrayList<BindingLookup>();
        for (IdOrOpOrAnonymousName name : entries.keySet()) {
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
        if (!(var instanceof Id)) { return parent.declarationSite(var); }
        Id _var = (Id)var;

        // Api-less name used for look-up only.
        Id no_api_var = removeApi(_var);
        
        if (!entries.containsKey(no_api_var)) { return parent.declarationSite(var); }
        TypeConsIndex typeCons = entries.get(no_api_var);
        
        // TODO: This seems wrong... If they were looking for an Object, but found some
        // other kind of type, isn't there some way we could return a better error message?
        if (!(typeCons instanceof ObjectTraitIndex)) { return parent.declarationSite(var); }
        ObjectTraitIndex objIndex = (ObjectTraitIndex)typeCons;
        
        return Option.<Node>some(objIndex.ast());
	}

	@Override
	public TypeEnv replaceAllIVars(Map<_InferenceVarType, Type> ivars) {
		
		Map<Id, TypeConsIndex> new_entries = new HashMap<Id, TypeConsIndex>();
		InferenceVarReplacer rep = new InferenceVarReplacer(ivars);
		
		for( Map.Entry<Id, TypeConsIndex> entry : entries.entrySet() ) {
			TypeConsIndex tc = entry.getValue();
			tc = tc.acceptNodeUpdateVisitor(rep);
			new_entries.put(entry.getKey(), tc);
		}
		return new ObjectTypeEnv(new_entries, parent.replaceAllIVars(ivars));
	}

	@Override
	public Option<StaticParam> staticParam(IdOrOpOrAnonymousName id) {
		return this.parent.staticParam(id);
	}
}
