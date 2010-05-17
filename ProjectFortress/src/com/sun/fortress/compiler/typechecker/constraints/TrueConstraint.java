/*******************************************************************************
    Copyright 2010 Sun Microsystems, Inc.,
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

import com.sun.fortress.compiler.typechecker.SubtypeHistory;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.VarType;

import edu.rice.cs.plt.lambda.Lambda;

public class TrueConstraint extends ConstraintFormula{
	public static final TrueConstraint TRUE = new TrueConstraint();
	private TrueConstraint(){}
	@Override public ConstraintFormula and(ConstraintFormula f, SubtypeHistory history) { return f; }
	@Override public ConstraintFormula applySubstitution(Lambda<Type, Type> sigma) { return this; }
	@Override public boolean isFalse() { return false; }
	@Override public boolean isTrue() { return true; }
	@Override public ConstraintFormula or(ConstraintFormula f, SubtypeHistory history) { return this; }
	@Override public String toString() { return "(true)"; }
	@Override public ConstraintFormula solve() {return this;}
	@Override public ConstraintFormula removeTypesFromScope(List<VarType> types) { return this; }
}
