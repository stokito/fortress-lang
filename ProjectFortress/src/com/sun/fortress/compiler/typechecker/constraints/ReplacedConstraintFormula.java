/*******************************************************************************
    Copyright 2009 Sun Microsystems, Inc.,
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

package com.sun.fortress.compiler.typechecker.constraints;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.fortress.compiler.typechecker.SubtypeHistory;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.VarType;
import com.sun.fortress.nodes._InferenceVarType;

import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.lambda.Lambda;

/**
 * When inference variables have been removed because of cycles, we still need to preserve the
 * inference variables that were removed, so that later when getMap is called, we will still know
 * what those removed inference variables were discovered to be.
 */
public class ReplacedConstraintFormula extends ConstraintFormula {
	private final ConstraintFormula delegate;
	private final _InferenceVarType newIVar;
	private  final List<_InferenceVarType> removedIVars;

	public ReplacedConstraintFormula(ConstraintFormula delegate, _InferenceVarType newIVar, List<_InferenceVarType> removedIVars) {
		this.delegate = delegate;
		this.newIVar = newIVar;
		this.removedIVars = removedIVars;
	}

	@Override
	public Map<_InferenceVarType, Type> getMap() {
		Map<_InferenceVarType, Type> old_map = delegate.getMap();
		if( !old_map.containsKey(newIVar) )
			return old_map;

		Map<_InferenceVarType, Type> new_map = new HashMap<_InferenceVarType, Type>();
		for( _InferenceVarType removedIVar : removedIVars ) {
			new_map.put(removedIVar, old_map.get(newIVar));
		}
		return CollectUtil.union(old_map, new_map);
	}

	@Override
	public ConstraintFormula solve() {
		return new ReplacedConstraintFormula(delegate.solve(), newIVar, removedIVars);
	}

	@Override public ConstraintFormula and(ConstraintFormula c, SubtypeHistory history) { return delegate.and(c, history); }
	@Override public ConstraintFormula applySubstitution(Lambda<Type, Type> sigma) { return delegate.applySubstitution(sigma); }
	@Override public boolean isFalse() { return delegate.isFalse(); }
	@Override public boolean isTrue() { return delegate.isTrue(); }
	@Override public ConstraintFormula or(ConstraintFormula c, SubtypeHistory history) { return delegate.or(c, history); }

	@Override
	public ConstraintFormula removeTypesFromScope(List<VarType> types) {
		return new ReplacedConstraintFormula(delegate.removeTypesFromScope(types), newIVar, removedIVars);
	}
}
