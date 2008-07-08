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
    	ConstraintFormula t1 = upperBound(i1,i2,t.new SubtypeHistory()).and(
    			lowerBound(i1,type("Int"),t.new SubtypeHistory()),t.new SubtypeHistory()).and(
    					upperBound(i2,type("Int"),t.new SubtypeHistory()), t.new SubtypeHistory()).and(
    							upperBound(i2,i1,t.new SubtypeHistory()),t.new SubtypeHistory());
    	assertTrue(t1.isSatisfiable());
    	} finally { debug.logEnd(); }
    }
}
