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

import static com.sun.fortress.compiler.Types.ANY;
import static com.sun.fortress.compiler.Types.BOTTOM;
import static edu.rice.cs.plt.debug.DebugUtil.debug;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sun.fortress.compiler.typechecker.SubtypeHistory;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes._InferenceVarType;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.useful.UsefulPLT;

public class ConstraintUtil {

	static protected class TypeExpander extends NodeUpdateVisitor{
		Map<_InferenceVarType,Type> bounds;
		Set<_InferenceVarType> context;
		TypeExpander(Map<_InferenceVarType,Type> _bounds){
			bounds=_bounds;
			context= new HashSet<_InferenceVarType>();
		}
		TypeExpander extend(_InferenceVarType t){
			TypeExpander temp = new TypeExpander(bounds);
			temp.context.addAll(context);
			temp.context.add(t);
			return temp;
		}

		@Override
		public Type for_InferenceVarType(_InferenceVarType that) {
			if(context.contains(that)){
				return that;
			}
			else{
				Type bound=bounds.get(that);
				TypeExpander v = this.extend(that);
				//return NodeFactory.makeFixedPointType(that,(Type)bound.accept(v));
				return (Type)bound.accept(v);
			}
		}

	}

	/**
	 * AND together all of the given constraints.
	 */
	public static ConstraintFormula bigAnd(Iterable<? extends ConstraintFormula> constraints,
			SubtypeHistory hist) {
		ConstraintFormula result = trueFormula();
		for( ConstraintFormula constraint : constraints ) {
			result = result.and(constraint, hist);
		}
		return result;
	}

	public static ConstraintFormula falseFormula(){return FalseConstraint.FALSE;}

	public static ConstraintFormula trueFormula(){return TrueConstraint.TRUE;}

	public static ConstraintFormula fromBoolean(boolean b) {
		return b ? falseFormula() : trueFormula();
	}

	public static ConstraintFormula lowerBound(_InferenceVarType var, Type bound, SubtypeHistory history) {
		debug.logStart(new String[]{"var","lowerBound"}, var, bound);
		if (history.subtypeNormal(bound, BOTTOM)) {
			debug.logEnd("result", trueFormula());
			return trueFormula();
		}
		else {
			//            IMultiMap<_InferenceVarType,Type> lowers = new MultiMap<_InferenceVarType,Type>();
			//            lowers.putItem(var, bound);
			ConstraintFormula result =
				new ConjunctiveFormula(UsefulPLT.<_InferenceVarType,Type>emptyMultiMap(),
						UsefulPLT.singletonMultiMap(var, bound), history);
			debug.logEnd("result", result);
			return result;
		}
	}

	public static ConstraintFormula upperBound(_InferenceVarType var, Type bound, SubtypeHistory history) {
		debug.logStart(new String[]{"var","upperBound"}, var, bound);
		if (history.subtypeNormal(ANY, bound)) {
			debug.logEnd("result", trueFormula());
			return trueFormula();
		}
		else{
			ConstraintFormula result =
				new ConjunctiveFormula(UsefulPLT.singletonMultiMap(var, bound),
						UsefulPLT.<_InferenceVarType,Type>emptyMultiMap(), history);
			debug.logEnd("result", result);
			return result;
		}
	}

}
