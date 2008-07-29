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

import com.sun.fortress.compiler.*;
import com.sun.fortress.compiler.index.*;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.useful.NI;
import edu.rice.cs.plt.tuple.Option;
import java.util.*;

import static com.sun.fortress.nodes_util.NodeFactory.*;
import static edu.rice.cs.plt.tuple.Option.*;

/**
 * A type environment whose outermost lexical scope consists of a map from IDs
 * to Variables.
 */
class LocalVarTypeEnv extends TypeEnv {
    private LocalVarDecl decl;
    private TypeEnv parent;

    LocalVarTypeEnv(LocalVarDecl _entries, TypeEnv _parent) {
        decl = _entries;
        parent = _parent;
    }

    private Option<LValueBind> findLVal(IdOrOpOrAnonymousName var) {
    	IdOrOpOrAnonymousName no_api_var = removeApi(var);

    	for (LValue lval : decl.getLhs()) {
    		if (lval instanceof LValueBind) {
    			LValueBind _lval = (LValueBind) lval;
    			if (_lval.getName().equals(var) || _lval.getName().equals(no_api_var)) {
    				return some(_lval);
    			}
    		} else {
    			return NI.nyi();
    		}
    	}
    	return none();
    }
    
    /**
     * Return a BindingLookup that binds the given IdOrOpOrAnonymousName to a type
     * (if the given IdOrOpOrAnonymousName is in this type environment).
     */
    public Option<BindingLookup> binding(IdOrOpOrAnonymousName var) {
    	Option<LValueBind> lval = findLVal(var);
    	
    	if(lval.isSome())
    		return some(new BindingLookup(lval.unwrap()));
    	else
    		return parent.binding(var);
    }

    @Override
    public List<BindingLookup> contents() {
        List<BindingLookup> result = new ArrayList<BindingLookup>();
        for (LValue lval : decl.getLhs()) {
            if (lval instanceof LValueBind) {
                result.add(new BindingLookup((LValueBind) lval));
            } else {
                return NI.nyi();
            }
        }
        result.addAll(parent.contents());
        return result;
    }

	@Override
	public Option<Node> declarationSite(IdOrOpOrAnonymousName var) {
		Option<LValueBind> lval = findLVal(var);
		
		if(lval.isSome())
			return Option.<Node>some(decl);
		else
			return parent.declarationSite(var);
	}
}
