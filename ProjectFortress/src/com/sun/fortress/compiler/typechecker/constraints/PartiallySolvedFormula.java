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

import java.util.List;
import java.util.Map;

import com.sun.fortress.compiler.typechecker.SubtypeHistory;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.VarType;
import com.sun.fortress.nodes._InferenceVarType;
import com.sun.fortress.useful.NI;

import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.lambda.Lambda;

/**
 * A constraint that is open, and yet contains solved inference variables.
 * Trying to add additional constraints to these inference variables will result in
 * FALSE, but otherwise this formula functions as a {@code ConjunctiveFormula}.
 */
public class PartiallySolvedFormula extends ConstraintFormula {

	private final Map<_InferenceVarType,Type> results;
	private final ConstraintFormula unsolved_formula;
	private final SubtypeHistory history;

	public PartiallySolvedFormula(Map<_InferenceVarType,Type> results, ConstraintFormula unsolved_formula, SubtypeHistory history) {
		this.results = results;
		this.unsolved_formula = unsolved_formula;
		this.history = history;
	}

	@Override
	public ConstraintFormula and(ConstraintFormula c, SubtypeHistory history) {
		if(c.isTrue()) { return this; }
		if(c.isFalse()) { return c; }
		if(c instanceof ConjunctiveFormula) {
			ConjunctiveFormula c_ = (ConjunctiveFormula)c;

			// TODO: This is really slow!!
			if( CollectUtil.containsAny(c_.getMap().keySet(), this.results.keySet()) )
				return new FailedSolvedFormula(results, history);

			return new PartiallySolvedFormula(results, unsolved_formula.and(c_, history), history);
		}
		if(c instanceof DisjunctiveFormula) { return NI.nyi(); };
		if(c instanceof PartiallySolvedFormula) {
			PartiallySolvedFormula c_ = (PartiallySolvedFormula)c;

			// For now, we should make sure our set of inference vars are disjoint.
			if( CollectUtil.containsAny(c_.results.keySet(), this.results.keySet()) )
				return NI.nyi();

			return new PartiallySolvedFormula(CollectUtil.union(this.results, c_.results),
					this.unsolved_formula.and(c_, history), history);
		}
		return NI.nyi();
	}

	@Override
	public ConstraintFormula applySubstitution(Lambda<Type, Type> sigma) {
		return NI.nyi();
	}

	@Override
	public Map<_InferenceVarType, Type> getMap() {
		return CollectUtil.union(unsolved_formula.getMap(), results);
	}

	@Override public boolean isFalse() { return false; }
	@Override public boolean isSatisfiable() { return this.solve().isSatisfiable(); }
	@Override public boolean isTrue() { return false; }

	@Override
	public ConstraintFormula or(ConstraintFormula c, SubtypeHistory history) {
		if( c.isTrue() )
			return ConstraintFormula.trueFormula();
		else if( c.isFalse() )
			return this;
		else {
			// TODO: This is really slow!!
			if( CollectUtil.containsAny(c.getMap().keySet(), this.results.keySet()) )
				return new FailedSolvedFormula(results, history);

			return new PartiallySolvedFormula(results, unsolved_formula.and(c, history), history);
		}
	}

	@Override
	public ConstraintFormula removeTypesFromScope(List<VarType> types) {
		return new PartiallySolvedFormula(results, unsolved_formula.removeTypesFromScope(types), history);
	}

	@Override
	public ConstraintFormula solve() {
		ConstraintFormula solved_constraint = unsolved_formula.solve();
		if( solved_constraint.isTrue() )
			return new SolvedFormula(CollectUtil.union(solved_constraint.getMap(), results), history);
		else
			return new FailedSolvedFormula(CollectUtil.union(solved_constraint.getMap(), results), history);
	}
}