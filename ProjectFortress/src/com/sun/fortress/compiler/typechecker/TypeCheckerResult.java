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

import java.util.*;
import com.sun.fortress.compiler.*;
import com.sun.fortress.compiler.typechecker.TypeAnalyzer.SubtypeHistory;
import com.sun.fortress.nodes.*;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.lambda.Lambda;
import edu.rice.cs.plt.tuple.Option;


public class TypeCheckerResult extends StaticPhaseResult {
    private final Node ast;
    private final Option<Type> type;
    private final ConstraintFormula nodeConstraints;

    private static ConstraintFormula 
    collectConstraints(Iterable<? extends TypeCheckerResult> results,
    		           TypeAnalyzer checker) {
    	SubtypeHistory empty_history = checker.new SubtypeHistory();

    	Iterable<ConstraintFormula> constraints =
    		IterUtil.map(results, new Lambda<TypeCheckerResult, ConstraintFormula>(){
    			public ConstraintFormula value(TypeCheckerResult arg0) {
    				return arg0.nodeConstraints;
    			}});    	

    	return ConstraintFormula.bigAnd(constraints, empty_history);
    }
    
    public static TypeCheckerResult compose(Node _ast, Option<Type> _type,
                                            TypeAnalyzer type_analyzer, 
                                            TypeCheckerResult... results) {
    	Arrays.asList();
        return new TypeCheckerResult(_ast, _type,
                                     collectErrors(Arrays.asList(results)),
                                     collectConstraints(Arrays.asList(results), type_analyzer));
    }
    
    public static TypeCheckerResult compose(Node _ast, Type _type,
                                            TypeAnalyzer type_analyzer, 
                                            TypeCheckerResult... results) {
        return new TypeCheckerResult(_ast, _type,
                                     collectErrors(Arrays.asList(results)),
                                     collectConstraints(Arrays.asList(results), type_analyzer));
    }
    
    public static TypeCheckerResult compose(Node _ast,
    		                                TypeAnalyzer type_analyzer,
    		                                TypeCheckerResult... results) {
        return new TypeCheckerResult(_ast,
                                     Option.<Type>none(),
                                     collectErrors(Arrays.asList(results)),
                                     collectConstraints(Arrays.asList(results), type_analyzer));
    }
    
    public static TypeCheckerResult compose(Node _ast, 
    		                                TypeAnalyzer type_analyzer, 
    		                                List<TypeCheckerResult> results) {
        return new TypeCheckerResult(_ast,
                                     Option.<Type>none(),
                                     collectErrors(results),
                                     collectConstraints(results, type_analyzer));
    }
    
    public static TypeCheckerResult compose(Node _ast, 
    		                                Type _type, 
    		                                TypeAnalyzer type_analyzer, 
    		                                List<TypeCheckerResult> results) {
        return new TypeCheckerResult(_ast,
                                     Option.wrap(_type),
                                     collectErrors(results),
                                     collectConstraints(results, type_analyzer));
    }
    
    public static TypeCheckerResult compose(Node _ast, 
    		                                Option<Type> _type, 
    		                                TypeAnalyzer type_analyzer,
    		                                List<TypeCheckerResult> results) {
        return new TypeCheckerResult(_ast,
                                     _type,
                                     collectErrors(results),
                                     collectConstraints(results, type_analyzer));
    }
    
    public static TypeCheckerResult compose(Node _ast, 
    		                                TypeAnalyzer type_analyzer,
    		                                Option<List<TypeCheckerResult>> results) {
        if (results.isSome()) {
            return new TypeCheckerResult(_ast,
                                         Option.<Type>none(),
                                         collectErrors(results.unwrap()),
                                         collectConstraints(results.unwrap(), type_analyzer));
        } else {
            return new TypeCheckerResult(_ast);
        }
    }
    
    /**
     * Generally compose should be called instead of this method. This method is
     * only to be called if you no longer care about propagating constraints upwards.
     */
    public static TypeCheckerResult addError(TypeCheckerResult result,
    		                                 StaticError s_err) {
    	List<StaticError> errs = new ArrayList<StaticError>();
    	for( StaticError err : result.errors() ) {
    		errs.add(err);
    	}
    	errs.add(s_err);
    	    	
    	return new TypeCheckerResult(result.ast, result.type(), errs, result.nodeConstraints);
    }
    
    public TypeCheckerResult(Node _ast, Type _type,
                             Iterable<? extends StaticError> _errors) {
        super(_errors);
        ast = _ast;
        type = Option.wrap(_type);
        nodeConstraints = ConstraintFormula.TRUE;
    }
    
    public TypeCheckerResult(Node _ast, Type _type,
    		                 Iterable<? extends StaticError> _errors,
    		                 ConstraintFormula c) {
    	super(_errors);
    	ast = _ast;
    	type = Option.wrap(_type);
    	nodeConstraints = c;
    }
    
    public TypeCheckerResult(Node _ast, 
                             Iterable<? extends StaticError> _errors) {
        super(_errors);
        ast = _ast;
        type = Option.none();
        nodeConstraints = ConstraintFormula.TRUE;
    }
                           
    public TypeCheckerResult(Node _ast) {
        super();
        ast = _ast;
        type = Option.none();
        nodeConstraints = ConstraintFormula.TRUE;
    }
    
    public TypeCheckerResult(Node _ast, Type _type) {
        super();
        ast = _ast;
        type = Option.wrap(_type);
        nodeConstraints = ConstraintFormula.TRUE;
    }
    
    public TypeCheckerResult(Node _ast, Option<Type> _type) {
        super();
        ast = _ast;
        type = _type;
        nodeConstraints = ConstraintFormula.TRUE;
    }
    
    public TypeCheckerResult(Node _ast, Option<Type> _type, Iterable<? extends StaticError> _errors) {
        super(_errors);
        ast = _ast;
        type = _type;
        nodeConstraints = ConstraintFormula.TRUE;
    }
    
    public TypeCheckerResult(Node _ast,
    		                 Option<Type> _type, 
    		                 Iterable<? extends StaticError> _errors,
    		                 ConstraintFormula c) {
        super(_errors);
        ast = _ast;
        type = _type;
        nodeConstraints = c;
    }    
    
    public TypeCheckerResult(Node _ast, StaticError _error) {
        super(IterUtil.make(_error));
        ast = _ast;
        type = Option.none();
        nodeConstraints = ConstraintFormula.TRUE;
    }
    
    public TypeCheckerResult(Node _ast, Type _type, StaticError _error) {
        super(IterUtil.make(_error));
        ast = _ast;
        type = Option.wrap(_type);
        nodeConstraints = ConstraintFormula.TRUE;
    }
    
    public TypeCheckerResult(Node _ast, Option<Type> _type, ConstraintFormula _constraints) {
        super();
        ast = _ast;
        type = _type;
        nodeConstraints = _constraints;
        // TODO: insert source locations for constraints
    }
    
    public TypeCheckerResult(Node _ast, Type _type, ConstraintFormula _constraints) {
        super();
        ast = _ast;
        type = Option.wrap(_type);
        nodeConstraints = _constraints;
        // TODO: insert source locations for constraints
    }
    
    public TypeCheckerResult(Node _ast, ConstraintFormula _constraints) {
        super();
        ast = _ast;
        type = Option.none();
        nodeConstraints = _constraints;
        // TODO: insert source locations for constraints
    }
    
    public Node ast() { return ast; }
    public Option<Type> type() { return type; }

	public ConstraintFormula getNodeConstraints() {
		return nodeConstraints;
	}
}
