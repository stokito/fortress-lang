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
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.nodes.*;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;

public class TypeCheckerResult extends StaticPhaseResult {
    private final Node ast;
    private final Option<Type> type;
    private final ConstraintFormula nodeConstraints;

    public static TypeCheckerResult compose(Node _ast, Option<Type> _type,
                                            TypeCheckerResult... results) {
        return new TypeCheckerResult(_ast, _type,
                                     collectErrors(Arrays.asList(results)));
    }
    
    public static TypeCheckerResult compose(Node _ast, Type _type,
                                            TypeCheckerResult... results) {
        return new TypeCheckerResult(_ast, _type,
                                     collectErrors(Arrays.asList(results)));
    }
    
    public static TypeCheckerResult compose(Node _ast,
                                            TypeCheckerResult... results) {
        return new TypeCheckerResult(_ast,
                                     Option.<Type>none(),
                                     collectErrors(Arrays.asList(results)));
    }
    
    public static TypeCheckerResult compose(Node _ast, List<TypeCheckerResult> results) {
        return new TypeCheckerResult(_ast,
                                     Option.<Type>none(),
                                     collectErrors(results));
    }       
    
    public static TypeCheckerResult compose(Node _ast, Option<List<TypeCheckerResult>> results) {
        if (results.isSome()) {
            Iterable<? extends StaticError> allErrors = collectErrors(Option.unwrap(results));
            
            return new TypeCheckerResult(_ast,
                                         Option.<Type>none(),
                                         allErrors);
        } else {
            return new TypeCheckerResult(_ast);
        }
    }
    
    public TypeCheckerResult(Node _ast, Type _type,
                             Iterable<? extends StaticError> _errors) {
        super(_errors);
        ast = _ast;
        type = Option.wrap(_type);
        nodeConstraints = ConstraintFormula.TRUE;
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
    
    public TypeCheckerResult(Node _ast, StaticError _error) {
        super(IterUtil.make(_error));
        ast = _ast;
        type = Option.none();
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
}
