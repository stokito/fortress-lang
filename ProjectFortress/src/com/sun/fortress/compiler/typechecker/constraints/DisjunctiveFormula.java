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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.fortress.compiler.typechecker.SubtypeHistory;
import com.sun.fortress.exceptions.InterpreterBug;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.VarType;
import com.sun.fortress.nodes._InferenceVarType;
import com.sun.fortress.useful.NI;

import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.lambda.Lambda;
import static com.sun.fortress.compiler.typechecker.constraints.ConstraintUtil.*;


public class DisjunctiveFormula extends ConstraintFormula {

	private Set<ConjunctiveFormula> conjuncts;

	DisjunctiveFormula(Set<ConjunctiveFormula> _conjuncts){
		if(_conjuncts.isEmpty())
			InterpreterBug.bug("Empty conjunct");
		conjuncts=Collections.unmodifiableSet(_conjuncts);
	}

	/*
	 * Returns the first constraint formula that is solvable.
	 */
	@Override
	public ConstraintFormula solve() {
		for(ConjunctiveFormula cf: conjuncts){
			ConstraintFormula sf= cf.solve();
			if(sf.isSatisfiable())
				return sf;
		}
		return falseFormula();
	}

	@Override
	public ConstraintFormula and(ConstraintFormula c, final SubtypeHistory history) {
		if(c.isFalse()){
			return c;
		}
		if(c.isTrue()){
			return this;
		}
		if(c instanceof ConjunctiveFormula){
			final ConjunctiveFormula cf = (ConjunctiveFormula)c;

			Set<ConjunctiveFormula> temp = new HashSet<ConjunctiveFormula>();
			for( ConjunctiveFormula cur_cf : conjuncts ) {
				ConjunctiveFormula new_cf = cur_cf.merge(cf, history);

				// Memory optimization
				if( new_cf.isSatisfiable() ) {
					temp.add(new_cf);
				}
			}
			if(temp.isEmpty())
				return falseFormula();
			return new DisjunctiveFormula(temp);
		}
		if(c instanceof DisjunctiveFormula){
			final DisjunctiveFormula df = (DisjunctiveFormula) c;

			Set<ConjunctiveFormula> temp = new HashSet<ConjunctiveFormula>();
			for( ConjunctiveFormula cur_cf : conjuncts ) {
				ConstraintFormula new_cf = df.and(cur_cf, history);

				if( new_cf instanceof DisjunctiveFormula ) {
					temp.addAll(((DisjunctiveFormula)new_cf).conjuncts);
				}
			}
			if(temp.isEmpty())
				return falseFormula();
			return new DisjunctiveFormula(temp);
		}
		return InterpreterBug.bug("Can't and with a Solved Constraint");
	}


	@Override
	public ConstraintFormula applySubstitution(final Lambda<Type, Type> sigma) {
		return new DisjunctiveFormula(CollectUtil.makeSet(IterUtil.map(conjuncts, new Lambda<ConjunctiveFormula,ConjunctiveFormula>(){
			public ConjunctiveFormula value(ConjunctiveFormula arg0) {
				return arg0.applySubstitution(sigma);
			}

		})));
	}

	@Override
	public boolean isFalse() {
		return false;
	}

	@Override
	public boolean isTrue() {
		return false;
	}

	@Override
	public ConstraintFormula or(ConstraintFormula c, SubtypeHistory history) {
		if(c.isFalse()){
			return this;
		}
		if(c.isTrue()){
			return c;
		}
		if(c instanceof ConjunctiveFormula){
			final ConjunctiveFormula cf = (ConjunctiveFormula)c;
			Set<ConjunctiveFormula> temp = new HashSet<ConjunctiveFormula>(conjuncts);
			temp.add(cf);
			return new DisjunctiveFormula(temp);
		}
		if(c instanceof DisjunctiveFormula){
			final DisjunctiveFormula df = (DisjunctiveFormula) c;
			Set<ConjunctiveFormula> temp = new HashSet<ConjunctiveFormula>(conjuncts);
			temp.addAll(df.conjuncts);
			return new DisjunctiveFormula(temp);
		}
		return InterpreterBug.bug("Can't and with a Solved Constraint");

	}

	@Override
	public boolean isSatisfiable() {
		return this.solve().isSatisfiable();
	}

	@Override
	public Map<_InferenceVarType, Type> getMap() {
		return this.solve().getMap();
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		int i=0;
		for(ConjunctiveFormula form : this.conjuncts){
			result.append(form.toString());
			if(i<this.conjuncts.size()-1){
				result.append(" OR ");
			}
			i++;
		}
		return result.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
		+ ((conjuncts == null) ? 0 : conjuncts.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final DisjunctiveFormula other = (DisjunctiveFormula) obj;
		if (conjuncts == null) {
			if (other.conjuncts != null)
				return false;
		} else if (!conjuncts.equals(other.conjuncts))
			return false;
		return true;
	}

	@Override
	public ConstraintFormula removeTypesFromScope(List<VarType> types) {
		return NI.nyi();
	}


}
