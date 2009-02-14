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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.fortress.compiler.Types;
import com.sun.fortress.compiler.typechecker.SubtypeHistory;
import com.sun.fortress.exceptions.InterpreterBug;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeDepthFirstVisitor;
import com.sun.fortress.nodes.NodeDepthFirstVisitor_void;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.TypeDepthFirstVisitor;
import com.sun.fortress.nodes.VarType;
import com.sun.fortress.nodes._InferenceVarType;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.SourceLoc;
import com.sun.fortress.useful.IMultiMap;
import com.sun.fortress.useful.MultiMap;
import com.sun.fortress.useful.NI;

import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.collect.ConsList;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.lambda.Lambda;
import edu.rice.cs.plt.lambda.Runnable1;
import edu.rice.cs.plt.tuple.Option;

/**
 * An immutable representation of constraints on a set of inference variables.  Certain typing
 * assertions (such as subtyping) can be made under the assumption that a certain ConstraintFormula
 * is satisfied.  Combining a number of these formulas, it is possible to determine whether a
 * certain portion of a program is well-formed (and, if so, how the inference variables that appear
 * within the program should be instantiated).
 * Constraint formulas are always in Disjunctive normal form
 */
public abstract class ConstraintFormula{

	/** Merge this and another formula by asserting that they must both be true. */
	public abstract ConstraintFormula and(ConstraintFormula c, SubtypeHistory history);

	/**
	 * Apply a type substitution to the contents of a formula.  Callers assume responsibility
	 * for guaranteeing that the substitution will not change the satisfiability (or truth) of the
	 * formula.  Substitutions of bounded inference variables must map to other (or the same)
	 * inference variables, not arbitrary types.
	 */
	public abstract ConstraintFormula applySubstitution(Lambda<Type, Type> sigma);

	/** Get the map of inference variable types to upper bounds **/
	public Map<_InferenceVarType, Type> getMap(){
		return Collections.emptyMap();
	}


	/** Determine whether the formula is false for all inference variable instantiations. */
	public abstract boolean isFalse();

	/** Determine whether there exists some choice for inference variables that makes the formula true. */
	public boolean isSatisfiable() { return !isFalse(); }

	/** Determine whether the formula is true for all inference variable instantiations. */
	public abstract boolean isTrue();

	/** Indicates that the given types have just gone out of scope, so in the resulting
	 *  ConstraintFormula they must not be mentioned. */
	public abstract ConstraintFormula removeTypesFromScope(List<VarType> types);

	/** Merge this and another formula by asserting that one of the two must be true. */
	public abstract ConstraintFormula or(ConstraintFormula c, SubtypeHistory history);

	public ConstraintFormula solve() {
		return this;
	}

}
