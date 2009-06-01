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

package com.sun.fortress.compiler.disambiguator;

import java.util.Set;
import java.util.Collections;

import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.nodes.FnDecl;
import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdOrOp;
import com.sun.fortress.nodes.Op;
import com.sun.fortress.nodes_util.NodeFactory;

import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.collect.IndexedRelation;
import edu.rice.cs.plt.collect.Relation;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.lambda.Lambda;
import edu.rice.cs.plt.tuple.Option;

public class LocalFnEnv extends DelegatingNameEnv {
    private Relation<IdOrOpOrAnonymousName, FnDecl> _fns;

    public LocalFnEnv(NameEnv parent, Set<FnDecl> fns) {
        super(parent);
        _fns = new IndexedRelation<IdOrOpOrAnonymousName, FnDecl>();
        
        // Add mapping of each function's name to its decl, and its unambiguous
        // name to its decl.
        for (FnDecl fn : fns) {
        	_fns.add(fn.getHeader().getName(), fn);
        	_fns.add(fn.getUnambiguousName(), fn);
        }
    }

    @Override
    public Set<IdOrOp> explicitFunctionNames(IdOrOp name) {
        if (_fns.containsFirst(name)) {
            return Collections.singleton(name);
        }
        else { return super.explicitFunctionNames(name); }
    }
    
    @Override
    public Set<IdOrOp> unambiguousFunctionNames(IdOrOp name) {
    	
    	// Get all the FnDecls with this name.
    	Set<FnDecl> decls = _fns.matchFirst(name);
    	Lambda<FnDecl, IdOrOp> getUnambiguousName = new Lambda<FnDecl,IdOrOp>() {
			@Override
			public IdOrOp value(FnDecl arg0) {
				return arg0.getUnambiguousName();
			}
    	};
    	
    	// Get all the unambiguous names from the decls.
    	Set<IdOrOp> allNames = CollectUtil.asSet(
    			IterUtil.map(decls,getUnambiguousName));
    	
    	// Combine them with those of the parent environments.
    	return CollectUtil.union(allNames, super.unambiguousFunctionNames(name));
    }

}
