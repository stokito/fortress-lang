package com.sun.fortress.compiler.index;

import java.util.List;

import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.IdOrOp;
import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.Modifiers;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.nodes_util.NodeUtil;


import edu.rice.cs.plt.tuple.Option;

/* While checking overloadings we need to treat declared variables as functions. Do not use this
 * outside the overloading checker or overloading oracle (unless you know what you are doing).
 * 
 * SAFE METHODS
 * getArrowType
 * getId
 */

public class DummyVariableFunction extends Function {
    
    DeclaredVariable ast;
    
    public DummyVariableFunction(DeclaredVariable dv) {
        ast=dv;
    }

    @Override
    public List<StaticParam> staticParameters() {
        return null;
    }

    @Override
    public List<Param> parameters() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Type> thrownTypes() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Modifiers mods() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Option<Expr> body() {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public IdOrOp name() {
        // TODO Auto-generated method stub
        return ast._lvalue.getName();
    }

    @Override
    public IdOrOpOrAnonymousName toUndecoratedName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IdOrOp unambiguousName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Span getSpan() {
        // TODO Auto-generated method stub
        return null;
    }
    
    public ArrowType getArrowType() {
        return (ArrowType)ast._lvalue.getIdType().unwrap();
    }

}
