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


import com.sun.fortress.compiler.*;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.useful.NI;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;
import java.util.*;

import static edu.rice.cs.plt.tuple.Option.*;

public class TypeChecker extends NodeDepthFirstVisitor<TypeCheckerResult> {
    private final GlobalEnvironment globals;
    private final StaticParamEnv staticParams;
    private final TypeEnv params; 
    private final TypeAnalyzer.StubEnv traits;
    private final TypeAnalyzer analyzer;
    
    public TypeChecker(GlobalEnvironment _globals, 
                       StaticParamEnv _staticParams,
                       TypeEnv _params) 
    {
        globals = _globals;
        staticParams = _staticParams;
        params = _params;
        traits = new TypeAnalyzer.StubEnv();
        analyzer = new TypeAnalyzer(traits);
    }
    
    private static Type typeFromLValueBinds(List<LValueBind> bindings) {
        List<Type> results = new ArrayList<Type>();
        
        for (LValueBind binding: bindings) {
            results.add(unwrap(binding.getType()));
        }
        return NodeFactory.makeTupleType(results);
    }
    
    /** Ignore unsupported nodes for now. */
    public TypeCheckerResult defaultCase(Node that) {
        return new TypeCheckerResult(that, NodeFactory.makeTupleType(new ArrayList<Type>()), IterUtil.<StaticError>empty());
    }
    
    public TypeCheckerResult forFnDef(FnDef that) {
        StaticParamEnv newStatics = staticParams.extend(that.getStaticParams());
        TypeEnv newParams = params.extend(that.getParams());
        TypeChecker newChecker = new TypeChecker(globals, staticParams, newParams);
        
        TypeCheckerResult contractResult = that.getContract().accept(newChecker);
        TypeCheckerResult bodyResult = that.getBody().accept(newChecker);
        
        return new TypeCheckerResult(new FnDef(that.getSpan(), 
                                               that.getMods(), 
                                               that.getName(), 
                                               that.getStaticParams(), 
                                               that.getParams(), 
                                               that.getReturnType(), 
                                               that.getThrowsClause(), 
                                               that.getWhere(), 
                                               (Contract)contractResult.ast(), 
                                               that.getSelfName(), 
                                               (Expr)bodyResult.ast()),
                                     IterUtil.compose(contractResult.errors(), bodyResult.errors()));
    }

     
    public TypeCheckerResult forVarDecl(VarDecl that) {
        List<LValueBind> lhs = that.getLhs();
        Expr init = that.getInit();
        
        TypeCheckerResult initResult = init.accept(this);
        
        if (lhs.size() == 1) {
            LValueBind var = lhs.get(0);
            Option<Type> varType = var.getType();
            if (varType.isSome()) {
                // The result of checking an Expr should always include a type.
                if (analyzer.subtype(unwrap(initResult.type()), unwrap(varType)).isTrue()) {
                    return new TypeCheckerResult(that);
                } else {
                    StaticError error = 
                        StaticError.make("Attempt to define variable " + var + " " +
                                         "with an expression of type " + initResult.type(),
                                         that);
                    return new TypeCheckerResult(that, error);
                }
            } else { // Eventually, this case will involve type inference
                return NI.nyi();
            }
        } else { // lhs.size() >= 2
            Type varType = typeFromLValueBinds(lhs); 
            if (analyzer.subtype(unwrap(initResult.type()), varType).isTrue()) {
                return new TypeCheckerResult(that);
            } else {
                StaticError error = 
                    StaticError.make("Attempt to define variables " + lhs + " " + 
                                     "with an expression of type " + varType,
                                     that);
                return new TypeCheckerResult(that, error);
            }
        }
    }
}
