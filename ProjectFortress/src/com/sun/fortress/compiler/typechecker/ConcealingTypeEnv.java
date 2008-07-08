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

import com.sun.fortress.nodes.*;

import edu.rice.cs.plt.collect.Relation;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.lambda.Lambda2;
import edu.rice.cs.plt.tuple.Option;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static edu.rice.cs.plt.tuple.Option.*;

/** 
 * A type environment that will conceal bindings from its parent type
 * environments. All bindings contained in this type environment will have no
 * type.
 */
class ConcealingTypeEnv extends TypeEnv {
    private Set<? extends IdOrOpOrAnonymousName> entries;
    private TypeEnv parent;
    
    ConcealingTypeEnv(Set<? extends IdOrOpOrAnonymousName> _entries, TypeEnv _parent) {
        entries = _entries;
        parent = _parent;
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
}