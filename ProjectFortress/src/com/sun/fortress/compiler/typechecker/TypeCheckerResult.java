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
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.exceptions.TypeError;
import com.sun.fortress.nodes.*;
import com.sun.fortress.useful.Useful;

import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.lambda.Lambda;
import edu.rice.cs.plt.lambda.Lambda2;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.Pair;


public class TypeCheckerResult extends StaticPhaseResult {
    private final Node ast;
    private final Option<Type> type;
    private final ConstraintFormula nodeConstraints;

    private static Pair<ConstraintFormula, Option<StaticError>> 
    collectConstraints(Iterable<? extends TypeCheckerResult> results,
    		           TypeAnalyzer checker, Node ast) {
    	SubtypeHistory empty_history = checker.new SubtypeHistory();
    	
    	Iterable<ConstraintFormula> constraints =
    		IterUtil.map(results, new Lambda<TypeCheckerResult, ConstraintFormula>(){
    			public ConstraintFormula value(TypeCheckerResult arg0) {
    				return arg0.nodeConstraints;
    			}});    	

    	Boolean was_sat =
    		IterUtil.fold(constraints, true, new Lambda2<Boolean,ConstraintFormula,Boolean>(){
				public Boolean value(Boolean arg0, ConstraintFormula arg1) {
					return arg0 & (arg1.isSatisfiable());
				}});
    	
    	ConstraintFormula and = ConstraintFormula.bigAnd(constraints, empty_history);
    	
    	// we want to report if this particular addition of constraints made the
    	// whole thing unsatisfiable.
    	boolean became_unsat = was_sat & !(and.isSatisfiable());
    	Option<StaticError> error;
    	if( became_unsat ) {
    		String err_str = "Type inference constraints became unsatisfiable " +
    			"with the following set of constraints: " + constraints;
    		error = Option.<StaticError>some(TypeError.make(err_str,ast));
    	}
    	else {
    		error = Option.none();
    	}
    	
    	return Pair.make(and, error);
    }
    
    
    public Map<_InferenceVarType, Type> getMap() {
		return nodeConstraints.getMap();
	}



	public static TypeCheckerResult compose(Node _ast, Option<Type> _type,
                                            TypeAnalyzer type_analyzer, 
                                            TypeCheckerResult... results) {
		
		Pair<ConstraintFormula,Option<StaticError>> constraints_ =
			collectConstraints(Arrays.asList(results),type_analyzer,_ast);
			
		List<StaticError> errors = IterUtil.asList(IterUtil.relax(collectErrors(Arrays.asList(results))));
		
		if( constraints_.second().isSome() ) {
			errors = Useful.prepend(constraints_.second().unwrap(), errors);
		}
		
        return new TypeCheckerResult(_ast, 
        		                     _type, 
        		                     errors, 
        		                     constraints_.first());
    }
    
    public static TypeCheckerResult compose(Node _ast, Type _type,
                                            TypeAnalyzer type_analyzer, 
                                            TypeCheckerResult... results) {
		Pair<ConstraintFormula,Option<StaticError>> constraints_ =
			collectConstraints(Arrays.asList(results),type_analyzer,_ast);
			
		List<StaticError> errors = IterUtil.asList(IterUtil.relax(collectErrors(Arrays.asList(results))));
		
		if( constraints_.second().isSome() ) {
			errors = Useful.prepend(constraints_.second().unwrap(), errors);
		}
    	
        return new TypeCheckerResult(_ast, 
        		                     _type, 
        		                     errors, 
        		                     constraints_.first());
    }
    
    public static TypeCheckerResult compose(Node _ast,
    		                                TypeAnalyzer type_analyzer,
    		                                TypeCheckerResult... results) {
		Pair<ConstraintFormula,Option<StaticError>> constraints_ =
			collectConstraints(Arrays.asList(results),type_analyzer,_ast);
			
		List<StaticError> errors = IterUtil.asList(IterUtil.relax(collectErrors(Arrays.asList(results))));
		
		if( constraints_.second().isSome() ) {
			errors = Useful.prepend(constraints_.second().unwrap(), errors);
		}
    	
        return new TypeCheckerResult(_ast,
                                     Option.<Type>none(),
                                     errors,
                                     constraints_.first());
    }
    
    public static TypeCheckerResult compose(Node _ast, 
    		                                TypeAnalyzer type_analyzer, 
    		                                List<TypeCheckerResult> results) {
		Pair<ConstraintFormula,Option<StaticError>> constraints_ =
			collectConstraints(results,type_analyzer,_ast);
			
		List<StaticError> errors = IterUtil.asList(IterUtil.relax(collectErrors(results)));
		
		if( constraints_.second().isSome() ) {
			errors = Useful.prepend(constraints_.second().unwrap(), errors);
		}
        return new TypeCheckerResult(_ast,
                                     Option.<Type>none(),
                                     errors,
                                     constraints_.first());
    }
    
    public static TypeCheckerResult compose(Node _ast, 
    		                                Type _type, 
    		                                TypeAnalyzer type_analyzer, 
    		                                List<TypeCheckerResult> results) {
		Pair<ConstraintFormula,Option<StaticError>> constraints_ =
			collectConstraints(results,type_analyzer,_ast);
			
		List<StaticError> errors = IterUtil.asList(IterUtil.relax(collectErrors(results)));
		
		if( constraints_.second().isSome() ) {
			errors = Useful.prepend(constraints_.second().unwrap(), errors);
		}
        return new TypeCheckerResult(_ast,
                                     Option.wrap(_type),
                                     errors,
                                     constraints_.first());
    }
    
    public static TypeCheckerResult compose(Node _ast, 
    		                                Option<Type> _type, 
    		                                TypeAnalyzer type_analyzer,
    		                                List<TypeCheckerResult> results) {
    	Pair<ConstraintFormula,Option<StaticError>> constraints_ =
			collectConstraints(results,type_analyzer,_ast);
			
		List<StaticError> errors = IterUtil.asList(IterUtil.relax(collectErrors(results)));
		
		if( constraints_.second().isSome() ) {
			errors = Useful.prepend(constraints_.second().unwrap(), errors);
		}
        return new TypeCheckerResult(_ast,
                                     _type,
                                     errors,
                                     constraints_.first());
    }
    
    public static TypeCheckerResult compose(Node _ast, 
    		                                TypeAnalyzer type_analyzer,
    		                                Option<List<TypeCheckerResult>> results_) {
        if (results_.isSome()) {
            List<TypeCheckerResult> results = results_.unwrap();
            
            Pair<ConstraintFormula,Option<StaticError>> constraints_ =
    			collectConstraints(results,type_analyzer,_ast);
    			
    		List<StaticError> errors = IterUtil.asList(IterUtil.relax(collectErrors(results)));
    		
    		if( constraints_.second().isSome() ) {
    			errors = Useful.prepend(constraints_.second().unwrap(), errors);
    		}
            
			return new TypeCheckerResult(_ast,
                                         Option.<Type>none(),
                                         errors,
                                         constraints_.first());
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
    
    /** Takes a TypeCheckerResult and returns a copy with the new AST **/
    public static TypeCheckerResult replaceAST(TypeCheckerResult result, Node _ast){
    	return new TypeCheckerResult(_ast, result.type(),result.errors(),result.nodeConstraints);
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
