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

import static com.sun.fortress.exceptions.InterpreterBug.bug;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.fortress.compiler.typechecker.SubtypeHistory;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.VarType;
import com.sun.fortress.nodes._InferenceVarType;

import edu.rice.cs.plt.lambda.Lambda;

/**
 * A formula that was unsatisfiable, and yet was able to solve for some inference variables.
 * We hope this class will help us give more localized error messages.
 */
public class FailedSolvedFormula extends ConstraintFormula {
	private final Map<_InferenceVarType, Type> inferredTypes;
	final private SubtypeHistory history;

	public FailedSolvedFormula(Map<_InferenceVarType, Type> inferred_types, SubtypeHistory history) {
		this.inferredTypes = inferred_types;
		this.history = history;
	}

	@Override
	public ConstraintFormula and(ConstraintFormula c,
			SubtypeHistory history) {
		return bug("Once constraint has been solved, this should not be called");
	}

	@Override
	public ConstraintFormula applySubstitution(Lambda<Type, Type> sigma) {
		return bug("Once constraint has been solved, this should not be called");
	}

	// For this implementation, getMap() actually does hard work! It
	// implements section 20.3 of the specification.
	@Override
	public Map<_InferenceVarType, Type> getMap() {
		Map<_InferenceVarType, Type> result = new HashMap<_InferenceVarType, Type>();
		// for each inferred type
		for( Map.Entry<_InferenceVarType, Type> entry : inferredTypes.entrySet() ) {
			result.put(entry.getKey(), closestExpressibleType(entry.getValue()));
		}

		return result;
	}

	private Type closestExpressibleType(Type value) {
		// TODO: As of 8/12/08 discussion w/ EA, we won't actually do
		// closest expressible types. Just normalize, then simplify.
		return history.normalize(value);
	}

	@Override public boolean isFalse() { return true; }
	@Override public boolean isTrue() { return false; }

	@Override
	public ConstraintFormula or(ConstraintFormula c,
			SubtypeHistory history) {
		return bug("Once constraint has been solved, this should not be called");
	}

	@Override
	public ConstraintFormula removeTypesFromScope(List<VarType> types) {
		return bug("Once constraint has been solved, this should not be called");
	}
	
	@Override
	public ConstraintFormula solve(){return this;}
	
}