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

import static com.sun.fortress.nodes_util.NodeFactory.makeLValue;
import static edu.rice.cs.plt.tuple.Option.some;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.sun.fortress.compiler.index.DeclaredVariable;
import com.sun.fortress.compiler.index.ObjectTraitIndex;
import com.sun.fortress.compiler.index.ParamVariable;
import com.sun.fortress.compiler.index.SingletonVariable;
import com.sun.fortress.compiler.index.TypeConsIndex;
import com.sun.fortress.compiler.index.Variable;
import com.sun.fortress.nodes.ArrowType;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.ObjectAbsDeclOrDecl;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes._RewriteGenericArrowType;
import com.sun.fortress.nodes_util.NodeFactory;

import edu.rice.cs.plt.tuple.Option;

/**
 * This environment represents bindings of top-level variables in a component.
 * It consists of a map from Ids to Variables.
 */
class VarTypeEnv extends TypeEnv {
    private Map<Id, Variable> entries;
    private TypeEnv parent;
    
    VarTypeEnv(Map<Id, Variable> _entries, TypeEnv _parent) {
        entries = _entries;
        parent = _parent;
    }

    /**
     * Return a BindingLookup that binds the given IdOrOpOrAnonymousName to a type
     * (if the given IdOrOpOrAnonymousName is in this type environment).
     */
    public Option<BindingLookup> binding(IdOrOpOrAnonymousName var) {
     if (!(var instanceof Id)) { return parent.binding(var); }
     Id _var = (Id)var;
     
     Id no_api_var = removeApi(_var);
     
        if (entries.containsKey(no_api_var)) {
            Variable result = entries.get(no_api_var);
            if (result instanceof DeclaredVariable) {
                return some(new BindingLookup(((DeclaredVariable)result).ast()));
            } else if (result instanceof SingletonVariable) {
                SingletonVariable _result = (SingletonVariable)result;
                Id declaringTrait = _result.declaringTrait();
                return some(new BindingLookup(makeLValue(_var, declaringTrait)));

            } else { // result instanceof ParamVariable
                ParamVariable _result = (ParamVariable)result;
                Param param = _result.ast();
                Option<Type> type = typeFromParam(param);

                return some(new BindingLookup(makeLValue(
                  makeLValue(param.getName(), type), param.getMods())));
            }
        } else {
            return parent.binding(var);
        }
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
	public Option<Node> declarationSite(IdOrOpOrAnonymousName id) {
	     if (!(id instanceof Id)) { return parent.declarationSite(id); }
	     Id _var = (Id)id;
	     
	     Id no_api_var = removeApi(_var);
	     
	     if (entries.containsKey(no_api_var)) {
	    	 Variable var = entries.get(no_api_var);
	    	 if( var instanceof DeclaredVariable ) {
	    		 return Option.<Node>some(((DeclaredVariable)var).ast());
	    	 }
	     }
	     return parent.declarationSite(id);
	}
}
