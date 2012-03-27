/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.typechecker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.sun.fortress.scala_src.typechecker.STypeChecker;

import com.sun.fortress.compiler.StaticPhaseResult;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.nodes.ASTNode;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.VarType;
import com.sun.fortress.nodes._InferenceVarType;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.scala_src.types.TypeAnalyzer;
import com.sun.fortress.useful.Useful;

import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.lambda.Lambda;
import edu.rice.cs.plt.lambda.Lambda2;
import edu.rice.cs.plt.lambda.Predicate;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.Pair;

import static com.sun.fortress.exceptions.InterpreterBug.bug;

public class TypeCheckerResult extends StaticPhaseResult {
    private Node ast;
    private final Option<Type> type;
    private STypeChecker typeChecker;
    
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

         return new TypeCheckerResult(result.ast, result.type(), errs);
     }

    public static Option<? extends Node> astFromResult(Option<TypeCheckerResult> result) {
        if( result.isSome() )
            return Option.some(result.unwrap().ast());
        else
            return Option.<Node>none();
    }

    public static Option<? extends List<? extends Node>> astFromResults(Option<List<TypeCheckerResult>> results) {
        if( results.isSome() )
            return Option.some(astFromResults(results.unwrap()));
        else
            return Option.none();
    }

    public static List<? extends Node> astFromResults(List<TypeCheckerResult> results) {
        return Useful.immutableTrimmedList(
                CollectUtil.makeList(IterUtil.map(results,
                        new Lambda<TypeCheckerResult, Node>(){
                          public Node value(TypeCheckerResult arg0) {
                    return arg0.ast();
                        }}
                )));
    }

    public TypeCheckerResult(Node _ast, Type _type,
            Iterable<? extends StaticError> _errors) {
        super(_errors);
        ast = _ast;
        type = Option.wrap(_type);
    }

    public TypeCheckerResult(Node _ast, Option<Type> _type,
                             Iterable<? extends StaticError> _errors) {
        super(_errors);
        ast = _ast;
        type = _type;
    }

    public TypeCheckerResult(Node _ast,
            Iterable<? extends StaticError> _errors) {
        super(_errors);
        ast = _ast;
        type = Option.none();
    }

    public TypeCheckerResult(Node _ast,
            Iterable<? extends StaticError> _errors, STypeChecker _typeChecker) {
        super(_errors);
        ast = _ast;
        type = Option.none();
        typeChecker = _typeChecker;
    }
    
    public TypeCheckerResult(Node _ast) {
        super();
        ast = _ast;
        type = Option.none();
    }

    public TypeCheckerResult(Node _ast, Type _type) {
        super();
        ast = _ast;
        type = Option.wrap(_type);
    }

    public TypeCheckerResult(Node _ast, Option<Type> _type) {
        super();
        ast = _ast;
        type = _type;
    }

    public TypeCheckerResult(Node _ast, StaticError _error) {
        super(IterUtil.make(_error));
        ast = _ast;
        type = Option.none();
    }

    public TypeCheckerResult(Node _ast, Type _type, StaticError _error) {
        super(IterUtil.make(_error));
        ast = _ast;
        type = Option.wrap(_type);
    }

    public Node ast() { return ast; }
    public Option<Type> type() { return type; }
    public STypeChecker typeChecker() { return typeChecker; }
    
    //Provide a setter so that we can Normalize the AST and keep all of the other state in TypeCheckerResult
    public void setAst(Node _ast){
        this.ast=_ast;
    }
    
    public TypeCheckerResult removeStaticParamsFromScope(List<StaticParam> staticParams) {
        List<VarType> var_types = new LinkedList<VarType>();
        for( StaticParam static_param : staticParams ) {
            if( NodeUtil.isTypeParam(static_param) ) {
                var_types.add(NodeFactory.makeVarType(NodeUtil.getSpan(static_param), (Id)static_param.getName()));
            }
        }
        return new TypeCheckerResult(this.ast,
                                     this.type,
                                     this.errors());
    }
}
