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

package com.sun.fortress.syntax_abstractions.util;

import java.util.LinkedList;
import java.util.List;

import com.sun.fortress.interpreter.evaluator.values.FObject;
import com.sun.fortress.interpreter.evaluator.values.FString;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.glue.prim.PrimImmutableArray.PrimImmutableArrayObject;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Contract;
import com.sun.fortress.nodes.Enclosing;
import com.sun.fortress.nodes.EnclosingFixity;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.Fixity;
import com.sun.fortress.nodes.FnDef;
import com.sun.fortress.nodes.FnRef;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.TraitType;
import com.sun.fortress.nodes.LooseJuxt;
import com.sun.fortress.nodes.Modifier;
import com.sun.fortress.nodes.Op;
import com.sun.fortress.nodes.OpName;
import com.sun.fortress.nodes.OpRef;
import com.sun.fortress.nodes.OpExpr;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.VoidLiteralExpr;
import com.sun.fortress.nodes.VoidType;
import com.sun.fortress.nodes.WhereClause;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.parser_util.FortressUtil;
import com.sun.fortress.useful.Debug;

import edu.rice.cs.plt.tuple.Option;

/*
 * Translate from a Fortress interpreter representation of Fortress AST to
 * Java representation of Fortress AST.
 * TODO: Implement cases for all the AST nodes. Do so by matching on their name.
 */
@SuppressWarnings("unchecked")
public class FortressObjectASTVisitor<T> {

    private static final String VALUE_FIELD_NAME = "in_text";
    private Span span;

    public FortressObjectASTVisitor(Span span) {
        this.span = span;
    }

    public T dispatch(FValue value) {
        if (null == value) {
            throw new RuntimeException("Unexpected value was null");
        }
        Debug.debug( Debug.Type.SYNTAX, 2, "Val: ", value.getClass());
        if (value instanceof FString) {
            return (T) ((FString) value).getString();
        }
        if (value instanceof FObject) {
            return dispatch((FObject) value);
        }
        
        throw new RuntimeException("Unexpected type of value: "+value.getClass());
    }

    public <V> List<V> dispatchList(FObject fObject) {
        if (fObject.type().getName().startsWith("ArrayList")) {
            List<V> ls = new LinkedList<V>();
            FValue firstUnused = getField(fObject, "firstUnused");
            FValue firstUsed = getField(fObject, "firstUsed");
            int size = firstUnused.getInt() - firstUsed.getInt();
            PrimImmutableArrayObject a = (PrimImmutableArrayObject) getField(fObject, "underlying");
            for (int inx=0;inx<size;inx++) {
                FValue elm = a.get(inx);
                ls.add(new FortressObjectASTVisitor<V>(this.span).dispatch(elm));
            }
            return ls;
        }
        throw new RuntimeException("Unexpected list type: "+fObject.type().getName());
    }

    private <V> Option<V> dispatchMaybe(FObject fObject) {
        if (fObject.type().getName().equals("Just")) {
            FValue v = getField(fObject, "x");
            V value = new FortressObjectASTVisitor<V>(this.span).dispatch(v);
            return Option.<V>some(value);
        }
        if (fObject.type().getName().equals("Nothing")) {
            return Option.<V>none();
        }
        throw new RuntimeException("Unexpected Maybe type: "+fObject.type().getName());
    }

    /**
     * The intention is that each AST node appears in the same order as in Fortress.ast
     * @param value
     * @return
     */
    public T dispatch(FObject value) {
        if (value.type().toString().startsWith("ArrayList")) {
            return (T) dispatchList(value);
        } else if (value.type().toString().equals("FnDef")) {
            return dispatchFnDef(value);
        } else if (value.type().toString().equals("FnRef")) {
            return dispatchFnRef(value);
        } else if (value.type().toString().equals("OpRef")) {
            return dispatchOpRef(value);
        } else if (value.type().toString().equals("LooseJuxt")) {
            return dispatchLooseJuxt(value);
        } else if (value.type().toString().equals("TightJuxt")) {
            return dispatchTightJuxt(value);
        } else if (value.type().toString().equals("OpExpr")) {
            return dispatchOpExpr(value);
        } else if (value.type().toString().equals("IntLiteralExpr")) {
            return dispatchInteger(value);
        } else if (value.type().toString().equals("CharLiteralExpr")) {
            return dispatchChar(value);
        } else if (value.type().toString().equals("StringLiteralExpr")) {
            return dispatchStringLiteralExpr(value);
        } else if (value.type().toString().equals("VoidLiteralExpr")) {
            return dispatchVoid(value);
        } else if (value.type().toString().equals("Type")) {
            return dispatchType(value);
        } else if (value.type().toString().equals("TraitType")) {
            return dispatchTraitType(value);
        } else if (value.type().toString().equals("VoidType")) {
            return dispatchVoidType(value);
        } else if (value.type().toString().equals("WhereClause")) {
            return dispatchWhereClause(value);
        } else if (value.type().toString().equals("Contract")) {
            return dispatchContract(value);
        } else if (value.type().toString().equals("APIName")) {
            return dispatchAPIName(value);
        } else if (value.type().toString().equals("Id")) {
            return dispatchId(value);
        } else if (value.type().toString().equals("Op")) {
            return dispatchOp(value);
        } else if (value.type().toString().equals("Enclosing")) {
            return dispatchEnclosing(value);
        } else if (value.type().toString().equals("EnclosingFixity")) {
            return dispatchEnclosingFixity(value);
        } else {
            throw new RuntimeException("NYI: "+value.type());
        }
    }

    /*
     * mods:List[\Modifier\],
      name:IdOrOpOrAnonymousName,
      staticParams:List[\StaticParam\],
      params:List[\Param\],
      returnType:Maybe[\Type\],
      throwsClause:Maybe[\List[\TraitType\]\],
      whereClauses:List[\WhereClause\],
      acontract:Contract,
      selfName:String,
      body:Expr
     */
    private T dispatchFnDef(FObject value) {
        FValue v1 = getField(value, "in_mods");
        List<Modifier> mods = dispatchList((FObject)v1);
        FValue v2 = getField(value, "in_name");
        IdOrOpOrAnonymousName name = new FortressObjectASTVisitor<IdOrOpOrAnonymousName>(this.span).dispatch((FObject)v2);
        FValue v3 = getField(value, "in_staticParams");
        List<StaticParam> staticParams = dispatchList((FObject)v3);
        FValue v4 = getField(value, "in_params");
        List<Param> params = dispatchList((FObject)v4);
        FValue v5 = getField(value, "in_returnType");
        Option<Type> returnType = dispatchMaybe((FObject)v5);
        FValue v6 = getField(value, "in_throwsClause");
        Option<List<BaseType>> throwsClause = dispatchMaybe((FObject)v6);
        FValue v7 = getField(value, "in_where");
        WhereClause whereClause = new FortressObjectASTVisitor<WhereClause>(this.span).dispatch((FObject)v7);
        FValue v8 = getField(value, "in_contract");
        Contract acontract = new FortressObjectASTVisitor<Contract>(this.span).dispatch((FObject)v8);
        FValue v9 = getField(value, "in_selfName");
        String selfName = new FortressObjectASTVisitor<String>(this.span).dispatch((FValue)v9);
        FValue v10 = getField(value, "in_body");
        Expr body = new FortressObjectASTVisitor<Expr>(this.span).dispatch((FObject)v10);
        return (T) new FnDef(this.span, mods, name, staticParams, params, returnType,
                              throwsClause, whereClause, acontract, selfName, body);
    }

    private Id mkQFortressASTName(String name) {
        return NodeFactory.makeId(SyntaxAbstractionUtil.FORTRESSAST, name);
    }

    private T dispatchFnRef(FObject value) {
        FValue v1 = getField(value, "in_fns");
        List<Id> fns = dispatchList((FObject)v1);
        Id originalName = fns.get(0); // HACK should be: (Id) dispatch((FObject)getField(value, "originalName")); //???
        FValue v2 = getField(value, "in_staticArgs");
        List<StaticArg> staticArgs = dispatchList((FObject)v2);
        return (T) new FnRef(this.span, originalName, fns, staticArgs);
    }

    private T dispatchOpRef(FObject value) {
        FValue v1 = getField(value, "in_ops");
        List<OpName> ops = dispatchList((FObject)v1);
        OpName originalName = ops.get(0); // HACK should be: (OpName) dispatch(getField(value, "originalName")); //???
        FValue v2 = getField(value, "in_staticArgs");
        List<StaticArg> staticArgs = dispatchList((FObject)v2);
        return (T) new OpRef(this.span, originalName, ops, staticArgs);
    }

    private T dispatchLooseJuxt(FObject value) {
        FValue v = getField(value, "in_exprs");
        List<Expr> exprs = dispatchList((FObject)v);
        return (T) new LooseJuxt(this.span, exprs);
    }

    private T dispatchTightJuxt(FObject value) {
        FValue v = getField(value, "in_exprs");
        List<Expr> exprs = dispatchList((FObject)v);
        return (T) NodeFactory.makeTightJuxt(this.span, exprs);
    }

    private T dispatchOpExpr(FObject value) {
        FValue v1 = getField(value, "in_op");
        OpRef opRef = new FortressObjectASTVisitor<OpRef>(this.span).dispatch((FObject)v1);
        FValue v2 = getField(value, "in_args");
        List<Expr> exprs = dispatchList((FObject) v2);
        return (T) new OpExpr(this.span, opRef, exprs);
    }

    public T dispatchChar(FObject value) {
        return (T) NodeFactory.makeIntLiteralExpr(getVal(value).getInt());
    }

    public T dispatchInteger(FObject value) {
        return (T) NodeFactory.makeCharLiteralExpr(getVal(value).getChar());
    }

    public T dispatchStringLiteralExpr(FObject value) {
        return (T) NodeFactory.makeStringLiteralExpr(getVal(value).getString());
    }

    public T dispatchVoid(FObject value) {
        return (T) new VoidLiteralExpr(this.span);
    }

    private T dispatchType(FObject value) {
        if (value.type().toString().equals("TraitType")) {
            return dispatchTraitType(value);
        } else if (value.type().toString().equals("VoidType")) {
            return dispatchVoidType(value);
        } else {
            throw new RuntimeException("NYI: "+value.type());
        }
    }

    private T dispatchTraitType(FObject value) {
        FValue v1 = getField(value, "in_name");
        Id name = new FortressObjectASTVisitor<Id>(this.span).dispatch((FObject)v1);
        FValue v2 = getField(value, "in_args");
        List<StaticArg> staticArgs = dispatchList((FObject) v2);
        return (T) new TraitType(name, staticArgs);
    }

    private T dispatchVoidType(FObject value) {
        return (T) new VoidType();
    }

    private T dispatchWhereClause(FObject value) {
        return (T) FortressUtil.emptyWhereClause();
    }

    private T dispatchContract(FObject value) {
        return (T) new Contract();
    }
    private T dispatchAPIName(FObject value) {
        FValue v1 = getField(value, "in_ids");
        List<Id> ids = dispatchList((FObject)v1);
        return (T) new APIName(this.span, ids);
    }

    private T dispatchId(FObject value) {
        FValue v1 = getField(value, "in_api");
        Option<APIName> apiName = dispatchMaybe((FObject)v1);
        FValue v2 = getField(value, "in_text");
        String id = ((FString) v2).getString();
        return (T) new Id(this.span, apiName, id);
    }

    private T dispatchOp(FObject value) {
        FValue v1 = getField(value, "in_text");
        FValue v2 = getField(value, "in_fixity");
        Option<Fixity> fixity = dispatchMaybe((FObject) v2);
        return (T) new Op(this.span, v1.getString(), fixity);
    }

    private T dispatchEnclosing(FObject value) {
        FValue v1 = getField(value, "in_open");
        Op op1 = new FortressObjectASTVisitor<Op>(this.span).dispatch((FObject)v1);
        FValue v2 = getField(value, "in_close");
        Op op2 = new FortressObjectASTVisitor<Op>(this.span).dispatch((FObject)v2);
        return (T) new Enclosing(this.span, op1, op2);
    }

    private T dispatchEnclosingFixity(FObject value) {
        return (T) new EnclosingFixity(this.span);
    }

    private FValue getVal(FObject value) {
        return value.getSelfEnv().getValueNull(VALUE_FIELD_NAME);
    }

    private FValue getField(FObject fObject, String field) {
        FValue val = fObject.getSelfEnv().getValueNull(field);
        if (val == null) {
            throw new Error("Object " + fObject + " lacks field " + field);
        }
        return val;
    }

}
