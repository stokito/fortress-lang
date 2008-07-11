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

import static edu.rice.cs.plt.debug.DebugUtil.debug;

import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.compiler.index.TraitIndex;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes._InferenceVarType;
import com.sun.fortress.nodes_util.NodeFactory;

import static com.sun.fortress.compiler.typechecker.TypeAnalyzerJUTest.*;
import static com.sun.fortress.compiler.typechecker.ConstraintFormula.*;

import junit.framework.TestCase;

public class TypeInferenceJUTest extends TestCase {
    public static void main(String... args) {
        junit.textui.TestRunner.run(TypeAnalyzerJUTest.class);
      }
    public void testConstraints(){
    	debug.logStart();
    	try {
    	TypeAnalyzer t = makeAnalyzer(trait("Int"));
    	_InferenceVarType i1=NodeFactory.make_InferenceVarType();
    	_InferenceVarType i2=NodeFactory.make_InferenceVarType();
    	_InferenceVarType i3=NodeFactory.make_InferenceVarType();
    	ConstraintFormula t1 = upperBound(i1,i2,t.new SubtypeHistory()).and(
    			lowerBound(i1,type("Int"),t.new SubtypeHistory()),t.new SubtypeHistory()).and(
    					upperBound(i2,type("Int"),t.new SubtypeHistory()), t.new SubtypeHistory()).and(
    							upperBound(i2,i1,t.new SubtypeHistory()),t.new SubtypeHistory());
    	assertTrue(t1.isSatisfiable());
    	t = makeAnalyzer(trait("A"),trait("B"),trait("C","B"));
    	ConstraintFormula t2 =  upperBound(i1,type("A"),t.new SubtypeHistory()).and(lowerBound(i1,type("A"),t.new SubtypeHistory()),t.new SubtypeHistory());
    	ConstraintFormula t3 =  t2.and(upperBound(i2,type("B"),t.new SubtypeHistory()).and(lowerBound(i2,type("B"),t.new SubtypeHistory()),t.new SubtypeHistory()),t.new SubtypeHistory());
    	ConstraintFormula t4 =  t3.and(upperBound(i3,type("C"),t.new SubtypeHistory()).and(lowerBound(i3,type("C"),t.new SubtypeHistory()),t.new SubtypeHistory()),t.new SubtypeHistory());
    	ConstraintFormula t5 = t4.and(lowerBound(i2,NodeFactory.makeIntersectionType(i1, i3),t.new SubtypeHistory()),t.new SubtypeHistory());
    	assertTrue(t5.isSatisfiable());
    	} finally { debug.logEnd(); }
    }
}
