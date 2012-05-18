/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.nodes_util;

import static com.sun.fortress.exceptions.InterpreterBug.bug;

import java.io.BufferedWriter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.sun.fortress.compiler.Types;
import com.sun.fortress.compiler.WellKnownNames;
import com.sun.fortress.nodes.*;
import com.sun.fortress.useful.BATree;
import com.sun.fortress.useful.Cons;
import com.sun.fortress.useful.Pair;
import com.sun.fortress.useful.PureList;
import com.sun.fortress.useful.Useful;

import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.lambda.Lambda;
import edu.rice.cs.plt.tuple.Option;

public class ExprFactory {

    public static final int defaultLexicalDepth = -2147483648;

    public static AmbiguousMultifixOpExpr makeAmbiguousMultifixOpExpr(AmbiguousMultifixOpExpr that,
                                                                      boolean parenthesized) {
        ExprInfo info = NodeFactory.makeExprInfo(NodeUtil.getSpan(that), parenthesized, NodeUtil.getExprType(that));
        return makeAmbiguousMultifixOpExpr(info, that.getInfix_op(), that.getMultifix_op(),
                                           that.getArgs());
    }

    public static AmbiguousMultifixOpExpr makeAmbiguousMultifixOpExpr(Span span,
                                                                      FunctionalRef infix_op,
                                                                      FunctionalRef multifix_op,
                                                                      List<Expr> args) {
        ExprInfo info = NodeFactory.makeExprInfo(span, false, Option.<Type>none());
        return makeAmbiguousMultifixOpExpr(info, infix_op, multifix_op,
                                           args);
    }

    public static AmbiguousMultifixOpExpr makeAmbiguousMultifixOpExpr(ExprInfo info,
                                                                      FunctionalRef infix_op,
                                                                      FunctionalRef multifix_op,
                                                                      List<Expr> args) {
        return new AmbiguousMultifixOpExpr(info, infix_op, multifix_op, args);
    }

    public static ArrayElement makeArrayElement(Expr elem) {
        return makeArrayElement(NodeUtil.getSpan(elem), false, Option.<Type>none(),
                                Collections.<StaticArg>emptyList(), elem);
    }

    public static ArrayElement makeArrayElement(Span span, boolean parenthesized,
                                                Option<Type> ty,
                                                List<StaticArg> staticArgs,
                                                Expr elem) {
        ExprInfo info = NodeFactory.makeExprInfo(span, parenthesized, ty);
        return new ArrayElement(info, staticArgs, elem);
    }

    public static ArrayElements makeArrayElements(Span span, int dim,
                                                  List<ArrayExpr> elems) {
        return makeArrayElements(span, false, Option.<Type>none(),
                                 Collections.<StaticArg>emptyList(), dim, elems,
                                 false);
    }

    public static ArrayElements makeArrayElements(ArrayElements a,
                                                  boolean outermost) {
        return makeArrayElements(NodeUtil.getSpan(a), NodeUtil.isParenthesized(a), NodeUtil.getExprType(a),
                                 a.getStaticArgs(), a.getDimension(),
                                 a.getElements(), outermost);
    }

    public static ArrayElements makeArrayElements(ArrayElements a,
                                                  List<StaticArg> staticArgs,
                                                  boolean outermost) {
        return makeArrayElements(NodeUtil.getSpan(a), NodeUtil.isParenthesized(a), NodeUtil.getExprType(a),
                                 staticArgs, a.getDimension(), a.getElements(),
                                 outermost);
    }

    public static ArrayElements makeArrayElements(Span span, boolean parenthesized,
                                                  Option<Type> ty,
                                                  List<StaticArg> staticArgs,
                                                  int dim,
                                                  List<ArrayExpr> elems,
                                                  boolean outermost) {
        ExprInfo info = NodeFactory.makeExprInfo(span, parenthesized, ty);
        return new ArrayElements(info, staticArgs, dim, elems,
                                 outermost);
    }

    public static ArrayElements finalizeArrayExpr(ArrayElements a) {
        return makeArrayElements(a, true);
    }

    public static ArrayElements addStaticArgsToArrayExpr(List<StaticArg> sargs,
                                                         ArrayElements a) {
        return makeArrayElements(a, sargs, true);
    }

    public static MathPrimary makeMathPrimary(Span span, Expr front,
                                              List<MathItem> rest) {
        return makeMathPrimary(span, false, Option.<Type>none(),
                               makeMultiJuxt(span), makeInfixJuxt(span), front, rest);
    }

    public static MathPrimary makeMathPrimary(Span span, boolean parenthesized,
                                              Option<Type> ty, FunctionalRef multi,
                                              FunctionalRef infix, Expr front,
                                              List<MathItem> rest) {
        ExprInfo info = NodeFactory.makeExprInfo(span, parenthesized, ty);
        return new MathPrimary(info, multi, infix, front, rest);
    }

    public static MethodInvocation makeMethodInvocation(FieldRef that, Expr obj,
                                                        IdOrOp field, Expr expr) {
        return makeMethodInvocation(NodeUtil.getSpan(that), NodeUtil.isParenthesized(that),
                                    NodeUtil.getExprType(that), obj, field, expr);
    }

    public static MethodInvocation makeMethodInvocation(SubscriptExpr that, Expr obj,
                                                        IdOrOp method, List<StaticArg> staticArgs, Expr arg) {
        return makeMethodInvocation(NodeUtil.getSpan(that), true,
                                    NodeUtil.getExprType(that), obj, method, staticArgs, arg);
    }

    public static MethodInvocation makeMethodInvocation(Assignment that, Expr obj,
                                                        IdOrOp method, List<StaticArg> staticArgs, Expr arg) {
        return makeMethodInvocation(NodeUtil.getSpan(that), NodeUtil.isParenthesized(that),
                                    NodeUtil.getExprType(that), obj, method, staticArgs, arg);
    }

    public static MethodInvocation makeMethodInvocation(Span span,
                                                        Expr receiver,
                                                        IdOrOp method,
                                                        Expr arg) {
        return makeMethodInvocation(span, false, Option.<Type>none(),
                                    receiver, method, arg);
    }
    public static MethodInvocation makeMethodInvocation(Span span,
                                                        Expr receiver,
                                                        IdOrOp method,
                                                        List<StaticArg> staticArgs,
                                                        Expr arg) {
        return makeMethodInvocation(span, false, Option.<Type>none(),
                                    receiver, method, staticArgs, arg);
    }

    public static MethodInvocation makeMethodInvocation(Span span,
                                                        boolean isParenthesized,
                                                        Option<Type> type,
                                                        Expr obj, IdOrOp field,
                                                        Expr expr) {
        return makeMethodInvocation(span, isParenthesized, type, obj, field,
                                    Collections.<StaticArg>emptyList(), expr);
    }

    public static MethodInvocation makeMethodInvocation(Span span,
                                                        boolean parenthesized,
                                                        Option<Type> ty,
                                                        Expr obj, IdOrOp field,
                                                        List<StaticArg> staticArgs,
                                                        Expr expr) {
        ExprInfo info = NodeFactory.makeExprInfo(span, parenthesized, ty);
        return new MethodInvocation(info, obj, field, staticArgs, expr);
    }

    public static OpExpr makeOpExpr(Span span, Op op) {
        return makeOpExpr(span, makeOpRef(op));
    }

    public static OpExpr makeOpExpr(Span span, Op op, Expr arg) {
        return makeOpExpr(span, false, Option.<Type>none(), makeOpRef(op),
                          Collections.singletonList(arg));
    }

    public static OpExpr makeOpExpr(Span span, Op op, Expr first,
                                    Expr second) {
        return makeOpExpr(span, false, Option.<Type>none(), makeOpRef(op),
                          Arrays.asList(first, second));
    }

    public static OpExpr makeOpExpr(Span span, Op op, List<StaticArg> staticArgs) {
        return makeOpExpr(span, makeOpRef(op, staticArgs));
    }

    public static OpExpr makeOpExpr(Span span, FunctionalRef op) {
        return makeOpExpr(span, false, Option.<Type>none(), op,
                          Collections.<Expr>emptyList());
    }

    public static OpExpr makeOpExpr(Span span, FunctionalRef op,
                                    List<Expr> args) {
        return makeOpExpr(span, false, Option.<Type>none(), op, args);
    }

    public static OpExpr makeOpExpr(Span span, FunctionalRef op,
                                    Expr first, Expr second) {
        return makeOpExpr(span, false, Option.<Type>none(),
                          op, Arrays.asList(first, second));
    }

    public static OpExpr makeOpExpr(Span span, Option<Type> ty, FunctionalRef op,
                                    Expr first, Expr second) {
        return makeOpExpr(span, false, ty, op, Arrays.asList(first, second));
    }

    public static OpExpr makeOpExpr(Span span, Op op, Expr arg,
                                    List<StaticArg> staticArgs) {
        return makeOpExpr(span, false, Option.<Type>none(),
                          makeOpRef(op, staticArgs),
                          Collections.singletonList(arg));
    }

    public static OpExpr makeOpExpr(Expr e,FunctionalRef op) {
        return makeOpExpr(NodeUtil.spanTwo(e, op), false,
                          Option.<Type>none(),
                          op, Useful.list(e));
    }

    public static OpExpr makeOpExpr(FunctionalRef op, Expr e_1, Expr e_2) {
        return makeOpExpr(NodeUtil.spanTwo(e_1, e_2), op, e_1, e_2);
    }

    public static OpExpr makeOpExpr(FunctionalRef op, Expr e_1) {
        return makeOpExpr(NodeUtil.spanTwo(e_1, op), false, Option.<Type>none(),
                          op, Collections.singletonList(e_1));
    }

    public static OpExpr makeOpExpr(FunctionalRef op, Expr e_1, Option<Expr> e_2) {
        if ( e_2.isSome() )
            return makeOpExpr(NodeUtil.spanTwo(e_1, e_2.unwrap()), op, e_1, e_2.unwrap());
        else
            return makeOpExpr(op, e_1);
    }

    public static OpExpr makeOpExpr(Span span, boolean parenthesized,
                                    Option<Type> ty, FunctionalRef op,
                                    List<Expr> exprs) {
        ExprInfo info = NodeFactory.makeExprInfo(span, parenthesized, ty);
        return new OpExpr(info, op, exprs);
    }

    public static Juxt makeTightJuxt(Juxt that, List<Expr> exprs) {
     return makeJuxt(NodeUtil.getSpan(that), NodeUtil.isParenthesized(that),
                     NodeUtil.getExprType(that),
                     that.getMultiJuxt(), that.getInfixJuxt(),
                     Useful.immutableTrimmedList(exprs),
                     that.isFnApp(), true);
    }

    public static Juxt makeTightJuxt(Span span, Expr first, Expr second) {
        return makeJuxt(span, false, Useful.list(first, second), false, true);
    }

    public static Juxt makeTightJuxt(Span span, boolean isParenthesized,
                                     List<Expr> exprs) {
        return makeJuxt(span, isParenthesized, Useful.immutableTrimmedList(exprs),
                        false, true);
    }

    public static Juxt makeTightJuxt(Span span, List<Expr> exprs) {
        return makeJuxt(span, false, Useful.immutableTrimmedList(exprs),
                        false, true);
    }

    public static Juxt makeTightJuxt(Span span, boolean isParenthesized,
                                     List<Expr> exprs, boolean isFnApp) {
        return makeJuxt(span, isParenthesized, Useful.immutableTrimmedList(exprs),
                        isFnApp, true);
    }

    public static Juxt makeLooseJuxt(Span span, List<Expr> exprs) {
        return makeJuxt(span, false, Useful.immutableTrimmedList(exprs),
                        false, false);
    }

    public static Juxt makeJuxt(Span span, boolean isParenthesized,
                                List<Expr> exprs, boolean isFnApp,
                                boolean tight) {
        return makeJuxt(span, isParenthesized,
                        Option.<Type>none(), makeMultiJuxt(span), makeInfixJuxt(span),
                        exprs, isFnApp, tight);
    }

    public static Juxt makeJuxt(Span span, boolean parenthesized,
                                Option<Type> ty,
                                FunctionalRef multi, FunctionalRef infix,
                                List<Expr> exprs, boolean isFnApp,
                                boolean tight) {
        ExprInfo info = NodeFactory.makeExprInfo(span, parenthesized, ty);
        return new Juxt(info, multi, infix, exprs, isFnApp, tight);
    }

    public static _RewriteFnRef make_RewriteFnRef(Span span, boolean parenthesized,
                                                  Option<Type> ty, Expr expr,
                                                  List<StaticArg> sargs) {
        ExprInfo info = NodeFactory.makeExprInfo(span, parenthesized, ty);
        return make_RewriteFnRef(info, expr, sargs);
    }

    private static _RewriteFnRef make_RewriteFnRef(ExprInfo info,
                                                  Expr expr,
                                                  List<StaticArg> sargs) {
        return new _RewriteFnRef(info, expr, sargs);
    }

    public static FnRef make_RewriteFnRefOverloading(Span span, FnRef original, Type type) {
        return makeFnRef(span, NodeUtil.isParenthesized(original), NodeUtil.getExprType(original),
                         original.getStaticArgs(), original.getLexicalDepth(),
                         original.getOriginalName(), original.getNames(),
                         original.getInterpOverloadings(), original.getNewOverloadings(),
                         Option.<Type>some(type));
    }

    public static FnRef makeFnRef(Id name) {
        return makeFnRef(NodeUtil.getSpan(name), name);
    }

    public static FnRef makeFnRef(Span span, Id name) {
        //List<IdOrOp> names = Collections.<IdOrOp>singletonList(name);
        return makeFnRef(span, name, Collections.<StaticArg>emptyList());
    }

    public static FnRef makeFnRef(Span span, Id name, List<StaticArg> sargs) {
        //List<IdOrOp> names = Collections.<IdOrOp>singletonList(name);
        return makeFnRef(span, false, Option.<Type>none(), sargs,
                         defaultLexicalDepth, name,
                         Collections.<IdOrOp>singletonList(name),
                         Collections.<Overloading>emptyList(),
                         Collections.<Overloading> emptyList(),
                         Option.<Type>none());
    }

    public static FnRef makeFnRef(Span span, boolean paren,
                                  Id original_fn, List<IdOrOp> fns,
                                  List<StaticArg> sargs) {
        return makeFnRef(span, paren, Option.<Type>none(), sargs,
                         defaultLexicalDepth, original_fn, fns,
                         Collections.<Overloading>emptyList(),
                         Collections.<Overloading> emptyList(),
                         Option.<Type>none());
    }

    public static FnRef makeFnRef(Id name, Id orig){
        return makeFnRef(NodeUtil.getSpan(name), orig);
    }

    public static FnRef makeFnRef(Id orig, List<IdOrOp> names){
        return makeFnRef(NodeUtil.getSpan(orig), false, Option.<Type>none(),
                         Collections.<StaticArg>emptyList(),
                         defaultLexicalDepth, orig, names,
                         Collections.<Overloading>emptyList(),
                         CollectUtil.<Overloading>emptyList(),
                         Option.<Type>none());
    }

    public static FnRef makeFnRef(Iterable<Id> apiIds, Id name) {
        Id qName = NodeFactory.makeId(apiIds, name);
        return makeFnRef(qName, Collections.<IdOrOp>singletonList(qName));
    }

    public static FnRef makeFnRef(APIName api, Id name) {
        Id qName = NodeFactory.makeId(api, name);
        return makeFnRef(qName, Collections.<IdOrOp>singletonList(qName));
    }

    public static FnRef makeFnRef(FnRef original, int lexicalNestedness) {
        return makeFnRef(NodeUtil.getSpan(original), NodeUtil.isParenthesized(original),
                         NodeUtil.getExprType(original), original.getStaticArgs(),
                         lexicalNestedness,
                         original.getOriginalName(), original.getNames(),
                         original.getInterpOverloadings(), original.getNewOverloadings(),
                         original.getOverloadingType());
    }

    public static FnRef makeFnRef(FnRef that, Option<Type> ty, Id name,
            List<IdOrOp> ids, List<StaticArg> sargs,
            List<Overloading> interp_overloadings,
            List<Overloading> newOverloadings) {
        return makeFnRef(NodeUtil.getSpan(that),
                NodeUtil.isParenthesized(that), ty, sargs, defaultLexicalDepth,
                name, ids, interp_overloadings, newOverloadings, Option
                        .<Type> none());
    }
    
    public static FnRef makeFnRef(FnRef that, Option<Type> ty, Id name,
            List<IdOrOp> ids, List<StaticArg> sargs,
            List<Overloading> interp_overloadings,
            List<Overloading> newOverloadings,
            Option<Type> type,
            Option<Type> schema) {
        return makeFnRef(NodeUtil.getSpan(that),
                NodeUtil.isParenthesized(that), ty, sargs, defaultLexicalDepth,
                name, ids, interp_overloadings, newOverloadings, type, schema);
    }

    public static FnRef makeFnRef(Span span, boolean isParenthesized,  Id name,
            List<StaticArg> sargs,
            List<Overloading> interp_overloadings,
            List<Overloading> newOverloadings) {
        return makeFnRef(span,
                isParenthesized, Option
                .<Type> none(), sargs, defaultLexicalDepth,
                name, Collections.<IdOrOp>emptyList(), interp_overloadings, newOverloadings, Option
                        .<Type> none());
    }

    public static FnRef makeFnRefHelpScala(Span span, boolean isParenthesized,  Id name,
            List<StaticArg> sargs,
            List<Overloading> interp_overloadings,
            List<Overloading> newOverloadings,
            Option<ArrowType> arrow_type,
            Option<ArrowType> arrow_schema) {
        Option<Type> schema = arrow_schema.isSome() ?
                Option.<Type>some(arrow_schema.unwrap()) :
            Option.<Type>none();
        Option<Type> type = arrow_type.isSome() ?
                Option.<Type>some(arrow_type.unwrap()) :
                    Option.<Type>none();
        return makeFnRef(span,
                isParenthesized, Option
                .<Type> none(), sargs, defaultLexicalDepth,
                name, Collections.<IdOrOp>emptyList(), interp_overloadings, newOverloadings, type, schema);
    }

    public static FnRef makeFnRef(Span span, boolean parenthesized,
            Option<Type> ty,
            List<StaticArg> staticArgs,
            int lexicalDepth,
            IdOrOp name, List<IdOrOp> names,
            List<Overloading> interp_overloadings,
            List<Overloading> newOverloadings,
            Option<Type> overloadingType) {
        return makeFnRef(span, parenthesized, ty, staticArgs, lexicalDepth, name, names, interp_overloadings, newOverloadings, overloadingType, Option.<Type>none());
    }

   public static FnRef makeFnRef(Span span, boolean parenthesized,
                                  Option<Type> ty,
                                  List<StaticArg> staticArgs,
                                  int lexicalDepth,
                                  IdOrOp name, List<IdOrOp> names,
                                  List<Overloading> interp_overloadings,
                                  List<Overloading> newOverloadings,
                                  Option<Type> overloadingType,
                                  Option<Type> overloadingSchema) {
        ExprInfo info = NodeFactory.makeExprInfo(span, parenthesized, ty);
        return new FnRef(info, staticArgs, lexicalDepth, name, names,
                interp_overloadings, newOverloadings, overloadingType, overloadingSchema);
    }

    public static FunctionalRef make_RewriteOpRefOverloading(Span span,
                                                             FunctionalRef original,
                                                             Type type) {
        return makeOpRef(span, NodeUtil.isParenthesized(original), NodeUtil.getExprType(original),
                         original.getStaticArgs(), original.getLexicalDepth(),
                         original.getOriginalName(), original.getNames(),
                         original.getInterpOverloadings(), original.getNewOverloadings(),
                         Option.<Type>some(type));
    }

    public static FunctionalRef makeOpRef(Span span, String name) {
        return makeOpRef(NodeFactory.makeOpInfix(span, name));
    }

    public static FunctionalRef makeOpRef(Op op) {
        return makeOpRef(op, Collections.<StaticArg>emptyList());
    }

    public static FunctionalRef makeOpRef(Op op, List<StaticArg> staticArgs) {
        return makeOpRef(NodeUtil.getSpan(op), false, Option.<Type>none(), staticArgs,
                         defaultLexicalDepth, op, Collections.<IdOrOp>singletonList(op),
                         Collections.<Overloading>emptyList(),
                         Collections.<Overloading>emptyList(),
                         Option.<Type>none());
    }

    public static Expr makeOpRef(FunctionalRef original, int lexicalNestedness) {
        return makeOpRef(NodeUtil.getSpan(original), NodeUtil.isParenthesized(original),
                         NodeUtil.getExprType(original), original.getStaticArgs(),
                         lexicalNestedness, original.getOriginalName(),
                         original.getNames(), original.getInterpOverloadings(),
                         original.getNewOverloadings(), original.getOverloadingType());
    }

    public static FunctionalRef makeOpRef(Span span, boolean parenthesized,
                                          Option<Type> ty,
                                          List<StaticArg> staticArgs,
                                          int lexicalDepth,
                                          IdOrOp name, List<IdOrOp> names,
                                          List<Overloading> interp_overloadings,
                                          List<Overloading> newOverloadings,
                                          Option<Type> overloadingType) {
        ExprInfo info = NodeFactory.makeExprInfo(span, parenthesized, ty);
        return new OpRef(info, staticArgs, lexicalDepth, name, names,
                         interp_overloadings, newOverloadings,
                         overloadingType);
    }

    public static _RewriteObjectExprRef make_RewriteObjectExprRef(_RewriteObjectExpr rwoe) {
        return new _RewriteObjectExprRef(rwoe.getInfo(),
                                         rwoe.getGenSymName(), rwoe.getStaticArgs());
    }

    public static Expr make_RewriteObjectRef(boolean parenthesized, Id in_obj,
                                             List<StaticArg> static_args) {
    	return makeVarRef(NodeUtil.getSpan(in_obj), parenthesized, Option.<Type>none(),
                          in_obj, static_args, defaultLexicalDepth);
    }

    public static VarRef makeVarRef(Span span, String s) {
        return makeVarRef(span, NodeFactory.makeId(span, s));
    }

    public static VarRef makeVarRef(Span span, Option<Type> type, Id name) {
        return makeVarRef(span, false, type, name);
    }

    public static VarRef makeVarRef(Span span, String s, int lexical_depth) {
        return makeVarRef(span, false, Option.<Type>none(),
                          NodeFactory.makeId(span, s),
                          Collections.<StaticArg>emptyList(), lexical_depth);
    }

    public static VarRef makeVarRef(Span span, Id id) {
        return makeVarRef(span, id, Collections.<StaticArg>emptyList());
    }

    public static VarRef makeVarRef(Span span, Id id, List<StaticArg> sargs) {
        return makeVarRef(span, false, Option.<Type>none(), id, sargs,
                          defaultLexicalDepth);
    }

    public static VarRef makeVarRef(Span span, Iterable<Id> apiIds, Id name) {
        return makeVarRef(span, NodeFactory.makeId(span, apiIds, name));
    }

    public static VarRef makeVarRef(Span span, String api_s, String local_s) {
        return makeVarRef(span, NodeFactory.makeId(span, api_s, local_s));
    }

    public static VarRef makeVarRef(Id id) {
        return makeVarRef(NodeUtil.getSpan(id), id);
    }

    public static VarRef makeVarRef(Iterable<Id> apiIds, Id name) {
        return makeVarRef(NodeFactory.makeId(apiIds, name));
    }

    /** Assumes {@code ids} is nonempty. */
    public static VarRef makeVarRef(Iterable<Id> ids) {
        return makeVarRef(NodeFactory.makeId(ids));
    }

    public static VarRef makeVarRef(APIName api, Id name) {
        return makeVarRef(NodeFactory.makeId(api, name));
    }

    public static VarRef makeVarRef(VarRef old, int depth) {
        return makeVarRef(NodeUtil.getSpan(old), NodeUtil.isParenthesized(old),
                          NodeUtil.getExprType(old),
                          old.getVarId(), old.getStaticArgs(), depth);
    }

    public static VarRef makeVarRef(VarRef var, Option<Type> type, Id name) {
        return makeVarRef(NodeUtil.getSpan(var), NodeUtil.isParenthesized(var), type, name);
    }

    public static VarRef makeVarRef(Span span, boolean isParenthesized,
                                    Option<Type> exprType, Id varId) {
        return makeVarRef(span, isParenthesized, exprType, varId,
                          Collections.<StaticArg>emptyList(), defaultLexicalDepth);
    }

    public static VarRef makeVarRef(Span span, boolean parenthesized,
                                    Option<Type> ty, Id varId,
                                    List<StaticArg> staticArgs, int lexicalDepth) {
        ExprInfo info = NodeFactory.makeExprInfo(span, parenthesized, ty);
        return new VarRef(info, varId, staticArgs,
                          lexicalDepth);
    }

    public static IntLiteralExpr makeIntLiteralExpr(Span span, String s) {
        BigInteger val;

        // Delete every apostrophe and U+202F NARROW NO-BREAK SPACE
        s = s.replace("'", "").replace("\u202F", "");

        int underLoc = s.indexOf('_');
        if (underLoc == -1) {
            val = new BigInteger(s);
        }
        else {
            String digits = s.substring(0, underLoc);
            String base_digits = s.substring(underLoc + 1);
            int base;
            if (!Unicode.charactersOverlap(base_digits, "0123456789")) {
                base = Unicode.numberToValue(base_digits);
            }
            else {
                base = Integer.parseInt(base_digits);
            }
            digits = dozenalHack(digits, base);
            val = new BigInteger(digits, base);
        }
        return makeIntLiteralExpr(span, s, val);
    }

    static String dozenalHack(String digits, int base) {
        if (base == 12 && Unicode.charactersOverlap(digits, "xXeE")) {
            digits = digits.replace('x', 'A');
            digits = digits.replace('X', 'A');
            digits = digits.replace('e', 'B');
            digits = digits.replace('E', 'B');
        }
        return digits;
    }

 /*
  *  There is a subtlety for declaration of the following makeFooExpr functions.
  *  The precise type information must not be provided. 
  *  Instead, the type information may be Option.<Type>none()
  *  This is because the disambiguator comes before the type checker and is not
  *  able to process the type information for literals encountered in the file
  *  CompilerBuiltIn.fss, because the types needed are defined by that very file.
  *  Letting the type checker fill in the types seems to work fine.
  */
    
    public static IntLiteralExpr makeIntLiteralExpr(Span span,
                                                    BigInteger intVal) {
        return makeIntLiteralExpr(span, intVal.toString(), intVal);
    }

    public static IntLiteralExpr makeIntLiteralExpr(Span span,
                                                    String text,
                                                    BigInteger intVal) {
        return makeIntLiteralExpr(span, false, Option.<Type>none(),
                                  text, intVal);
    }

    public static IntLiteralExpr makeIntLiteralExpr(Span span,
						    boolean parenthesized,
                                                    BigInteger intVal) {
        return makeIntLiteralExpr(span, parenthesized, Option.<Type>none(),
                                  intVal.toString(), intVal);
    }

    public static IntLiteralExpr makeIntLiteralExpr(Span span,
                                                    boolean parenthesized,
                                                    Option<Type> ty,
                                                    String text,
                                                    BigInteger intVal) {
        ExprInfo info = NodeFactory.makeExprInfo(span, parenthesized, ty);
        return new IntLiteralExpr(info, text, intVal);
    }

    public static IntLiteralExpr makeIntLiteralExpr(ExprInfo info, BigInteger intVal) {
        return new IntLiteralExpr(info, intVal.toString(), intVal);
}
    
    public static CharLiteralExpr makeCharLiteralExpr(Span span, String s) {
        int n = s.length();
        if (n == 0) {
	    // If parser gave us no character (because of a syntax error),
	    // use U+FFFD REPLACEMENT CHARACTER.
	    return makeCharLiteralExpr(span, false, Option.<Type>none(),
				       s, 0xFFFD);
	} else if (n == 1) {
	    return makeCharLiteralExpr(span, false, Option.<Type>none(),
				       s, s.charAt(0));
	} else {
	    // If not a single character, then it should be a hex string
	    // or a character, a space, and a hex string (which have already
	    // been checked by the parser to see that they match).
	    if (s.charAt(1) == ' ') {
		return makeCharLiteralExpr(span, false, Option.<Type>none(),
					   s, s.charAt(0));
	    } else {
		return makeCharLiteralExpr(span, false, Option.<Type>none(),
					   s, Integer.parseInt(s, 16));
	    }
	}
    }

    public static CharLiteralExpr makeCharLiteralExpr(Span span,
                                                      boolean parenthesized,
                                                      Option<Type> ty,
                                                      String text,
                                                      int charVal) {
        ExprInfo info = NodeFactory.makeExprInfo(span, parenthesized, ty);
        return new CharLiteralExpr(info, text, charVal);
    }

    public static VoidLiteralExpr makeVoidLiteralExpr(Span span) {
        return makeVoidLiteralExpr(span, false, Option.<Type>some(Types.VOID), "");
    }

    public static VoidLiteralExpr makeVoidLiteralExpr(Span span,
                                                      boolean parenthesized,
                                                      Option<Type> ty,
                                                      String text) {
        ExprInfo info = NodeFactory.makeExprInfo(span, parenthesized, ty);
        return new VoidLiteralExpr(info, text);
    }

    public static BooleanLiteralExpr makeBooleanLiteralExpr(Span span,int bVal) {
        return makeBooleanLiteralExpr(span, false, Option.<Type>none(), "",bVal); 
    }

    public static BooleanLiteralExpr makeBooleanLiteralExpr(Span span,
                                                      boolean parenthesized,
                                                      Option<Type> ty,
                                                      String text, int bVal) {
        ExprInfo info = NodeFactory.makeExprInfo(span, parenthesized, ty);
        return new BooleanLiteralExpr(info, text,bVal);
    }    
    
    public static BooleanLiteralExpr makeBooleanLiteralExpr(ExprInfo info, boolean bVal) {
    	Integer iVal = bVal? 1 : 0;
        return new BooleanLiteralExpr(info, iVal.toString(), iVal);
}
    
    public static Expr makeSubscripting(Span span,
                                        String left, String right,
                                        Expr base, List<Expr> args,
                                        List<StaticArg> sargs) {
        return makeSubscripting(span, base, left, right, args, sargs);
    }

    public static Expr makeSubscripting(Span span, Expr base, String open,
                                        String close, List<Expr> args,
                                        List<StaticArg> sargs) {
        Op op = NodeFactory.makeEnclosing(span, open, close, true, false);
        List<Expr> es;
        if (args == null) es = Collections.<Expr>emptyList();
        else              es = args;
        return makeSubscriptExpr(span, base, es, Option.<Op>some(op), sargs);
    }

    public static SubscriptExpr makeSubscriptExpr(Span span, Expr obj,
                                                  List<Expr> subs) {
        return makeSubscriptExpr(span, obj, subs, Option.<Op>none());
    }

    public static SubscriptExpr makeSubscriptExpr(Span span, Expr obj,
                                                  List<Expr> subs,
                                                  Option<Op> op) {
        return makeSubscriptExpr(span, obj, subs, op,
                                 Collections.<StaticArg>emptyList());
    }

    public static SubscriptExpr makeSubscriptExpr(Span span, Expr obj,
                                                  List<Expr> subs,
                                                  Option<Op> op,
                                                  List<StaticArg> sargs) {
        return makeSubscriptExpr(span, false, Option.<Type>none(), obj,
                                 Useful.immutableTrimmedList(subs), op, sargs);
    }

    public static SubscriptExpr makeSubscriptExpr(Span span,
                                                  boolean parenthesized,
                                                  Option<Type> ty,
                                                  Expr obj, List<Expr> subs,
                                                  Option<Op> op,
                                                  List<StaticArg> staticArgs) {
        ExprInfo info = NodeFactory.makeExprInfo(span, parenthesized, ty);
        return new SubscriptExpr(info, obj, subs, op, staticArgs);
    }

    public static LocalVarDecl makeLocalVarDecl(Id p, Expr _r, Expr _body_expr) {
        List<Expr> _body = new ArrayList<Expr>(1);
        List<LValue> _lhs = new ArrayList<LValue>(1);
        Option<Expr> _rhs = Option.some(_r);
        _body.add(_body_expr);
        _lhs.add(NodeFactory.makeLValue(NodeUtil.getSpan(p), p));
        return makeLocalVarDecl(NodeUtil.spanTwo(p, _r), _body, _lhs, _rhs);
    }

    public static LocalVarDecl makeLocalVarDecl(Span sp, Id p, Expr _r, Expr _body_expr) {
        List<Expr> _body = new ArrayList<Expr>(1);
        List<LValue> _lhs = new ArrayList<LValue>(1);
        Option<Expr> _rhs = Option.some(_r);
        _body.add(_body_expr);
        _lhs.add(NodeFactory.makeLValue(sp, p));
        return makeLocalVarDecl(sp, _body, _lhs, _rhs);
    }

    public static LocalVarDecl makeLocalVarDecl(Span sp, List<LValue> lhs, Expr _r, Expr _body_expr) {
        List<Expr> _body = new ArrayList<Expr>(1);
        Option<Expr> _rhs = Option.some(_r);
        _body.add(_body_expr);
        return makeLocalVarDecl(sp, _body, lhs, _rhs);
    }

    public static LocalVarDecl makeLocalVarDecl(Span span, List<LValue> lvs,
                                                Expr expr) {
        return makeLocalVarDecl(span, lvs, Option.<Expr>some(expr));
    }

    public static LocalVarDecl makeLocalVarDecl(Span span, List<LValue> lvs) {
        return makeLocalVarDecl(span, lvs, Option.<Expr>none());
    }

    public static LocalVarDecl makeLocalVarDecl(Span span, List<LValue> lvs,
                                                Option<Expr> expr) {
        return makeLocalVarDecl(span, Collections.<Expr>emptyList(), lvs, expr);
    }

    public static LocalVarDecl makeLocalVarDecl(Span sp, List<LValue> lhs,
                                                Expr _r, List<Expr> _body) {
        Option<Expr> _rhs = Option.some(_r);
        return makeLocalVarDecl(sp, _body, lhs, _rhs);
    }

    public static LocalVarDecl makeLocalVarDecl(Span span,
                                                List<Expr> body,
                                                List<LValue> lhs,
                                                Option<Expr> rhs) {
        return makeLocalVarDecl(span, false, Option.<Type>none(), body, lhs, rhs);
    }

    public static LocalVarDecl makeLocalVarDecl(Span span,
                                                boolean parenthesized,
                                                Option<Type> ty,
                                                List<Expr> body,
                                                List<LValue> lhs,
                                                Option<Expr> rhs) {
        return makeLocalVarDecl(span, parenthesized, ty, makeBlock(span, body), lhs, rhs);
    }

    public static LocalVarDecl makeLocalVarDecl(Span span,
                                                boolean parenthesized,
                                                Option<Type> ty,
                                                Block body,
                                                List<LValue> lhs,
                                                Option<Expr> rhs) {
        ExprInfo info = NodeFactory.makeExprInfo(span, parenthesized, ty);
        return new LocalVarDecl(info, body, lhs, rhs);
    }

    public static FnExpr makeFnExpr(Span span,
                                    List<Param> params,
                                    Expr body) {
        return makeFnExpr(span, params, Option.<Type>none(),
                          Option.<List<Type>>none(), body);
    }

    public static FnExpr makeFnExpr(Span span, List<Param> params,
                                    Option<Type> returnType, Expr body) {
        return makeFnExpr(span, params, returnType,
                          Option.<List<Type>>none(), body);
    }

    public static FnExpr makeFnExpr(Span span, List<Param> params,
                                    Option<Type> returnType,
                                    Option<List<Type>> throwsClause,
                                    Expr body) {
        return makeFnExpr(span, false, Option.<Type>none(),
                          makeAnonymousFnName(span, Option.<APIName>none()),
                          Collections.<StaticParam>emptyList(), params,
                          returnType, Option.<WhereClause>none(),
                          throwsClause, body);
    }

    public static FnExpr makeFnExpr(Span span,
                                    boolean parenthesized,
                                    Option<Type> ty,
                                    IdOrOpOrAnonymousName name,
                                    List<StaticParam> staticParams,
                                    List<Param> params,
                                    Option<Type> returnType,
                                    Option<WhereClause> whereClause,
                                    Option<List<Type>> throwsClause,
                                    Expr body) {
        FnHeader header = NodeFactory.makeFnHeader(Modifiers.None, name, staticParams,
                                                   whereClause, throwsClause,
                                                   Option.<Contract>none(), params,
                                                   returnType);
        ExprInfo info = NodeFactory.makeExprInfo(span, parenthesized, ty);
        return new FnExpr(info, header, body);
    }

    public static AnonymousFnName makeAnonymousFnName(Span span, Option<APIName> api) {
        ExprInfo info = NodeFactory.makeExprInfo(span);
        return new AnonymousFnName(info, api);
    }

    public static Exit makeExit(Span span,
                                Option<Type> typeOp,
                                Option<Id> targetOp,
                                Expr retExpr) {
        return makeExit(span, false, typeOp, targetOp, Option.<Expr>some(retExpr));
    }

    public static Exit makeExit(Span span,
                                Option<Type> typeOp,
                                Option<Id> targetOp,
                                Option<Expr> retExpr) {
        return makeExit(span, false, typeOp, targetOp, retExpr);
    }

    public static Exit makeExit(Span span,
                                boolean parenthesized,
                                Option<Type> ty,
                                Option<Id> targetOp,
                                Option<Expr> retExpr) {
        ExprInfo info = NodeFactory.makeExprInfo(span, parenthesized, ty);
        return new Exit(info, targetOp, retExpr);
    }

    public static ArrayComprehension makeArrayComprehension(Span span,
                                                            List<StaticArg> staticArgs,
                                                            List<ArrayComprehensionClause> clauses) {
        return makeArrayComprehension(span, false, Option.<Type>none(),
                                      staticArgs, clauses);
    }

    public static ArrayComprehension makeArrayComprehension(Span span,
                                                            boolean parenthesized,
                                                            Option<Type> ty,
                                                            List<StaticArg> staticArgs,
                                                            List<ArrayComprehensionClause> clauses) {
        ExprInfo info = NodeFactory.makeExprInfo(span, parenthesized, ty);
        return new ArrayComprehension(info, staticArgs, clauses);
    }

    public static Accumulator makeAccumulator(Span span,
                                              List<StaticArg> staticArgs,
                                              Op accOp,
                                              List<GeneratorClause> gens,
                                              Expr body) {
        return makeAccumulator(span, false, Option.<Type>none(), staticArgs,
                               accOp, gens, body);
    }

    public static Accumulator makeAccumulator(Span span,
                                              boolean parenthesized,
                                              Option<Type> ty,
                                              List<StaticArg> staticArgs,
                                              Op accOp,
                                              List<GeneratorClause> gens,
                                              Expr body) {
        ExprInfo info = NodeFactory.makeExprInfo(span, parenthesized, ty);
        return new Accumulator(info, staticArgs, accOp, gens, body);
    }

    /**
     * For rewriting the id list, expr, of an existing typecase.
     * @param tc
     * @param lid
     * @param expr
     * @return
     */
    public static Typecase makeTypecase(Typecase tc, Expr expr) {
        return makeTypecase(NodeUtil.getSpan(tc), NodeUtil.isParenthesized(tc),
                            NodeUtil.getExprType(tc),
                            expr, tc.getClauses(),
                            tc.getElseClause());
    }

    public static Typecase makeTypecase(Span span,
                                        boolean parenthesized,
                                        Option<Type> ty,
                                        Expr bindExpr,
                                        List<TypecaseClause> clauses,
                                        Option<Block> elseClause) {
        ExprInfo info = NodeFactory.makeExprInfo(span, parenthesized, ty);
        return new Typecase(info, bindExpr, clauses, elseClause);
    }

    public static TupleExpr makeTupleExpr(Span span, List<Expr> exprs) {
        return makeTupleExpr(span, false, Option.<Type>none(), exprs,
                             Option.<Expr>none(),
                             Collections.<KeywordExpr>emptyList(), false);
    }

    public static TupleExpr makeTupleExpr(Span span,
                                          boolean parenthesized,
                                          Option<Type> ty,
                                          List<Expr> exprs) {
        return makeTupleExpr(span, parenthesized, ty, exprs,
                             Option.<Expr>none(),
                             Collections.<KeywordExpr>emptyList(), false);
    }

    public static Expr makeMaybeTupleExpr(Span span, List<Expr> elements) {
        if ( elements.size() > 1 )
            return makeTupleExpr(span, elements);
        else if ( elements.size() == 1 )
            return elements.get(0);
        else
            return makeVoidLiteralExpr(span);
    }

    public static TupleExpr makeTupleExpr(Span span, Expr... exprs) {
        return makeTupleExpr(span, Arrays.asList(exprs));
    }

    public static TupleExpr makeTupleExpr(Span span,
                                          boolean parenthesized,
                                          Option<Type> ty,
                                          List<Expr> exprs,
                                          Option<Expr> varargs,
                                          List<KeywordExpr> keywords,
                                          boolean inApp) {
        ExprInfo info = NodeFactory.makeExprInfo(span, parenthesized, ty);
        return new TupleExpr(info, exprs, varargs, keywords, inApp);
    }

    public static Try makeTry(Span span,
                              Block body,
                              Option<Catch> catchClause,
                              List<BaseType> forbidClause,
                              Option<Block> finallyClause) {
        return makeTry(span, false, Option.<Type>none(), body, catchClause,
                       forbidClause, finallyClause);
    }

    public static Try makeTry(Span span,
                              boolean parenthesized,
                              Option<Type> ty,
                              Block body,
                              Option<Catch> catchClause,
                              List<BaseType> forbidClause,
                              Option<Block> finallyClause) {
        ExprInfo info = NodeFactory.makeExprInfo(span, parenthesized, ty);
        return new Try(info, body, catchClause, forbidClause, finallyClause);
    }

    public static ObjectExpr makeObjectExpr(Span span,
                                            List<TraitTypeWhere> extendsC,
                                            List<Decl> decls,
                                            Option<SelfType> selfType) {
        return makeObjectExpr(span, false, Option.<Type>none(),
                              extendsC, decls, selfType);
    }

    public static ObjectExpr makeObjectExpr(Span span,
                                            boolean parenthesized,
                                            Option<Type> exprType,
                                            List<TraitTypeWhere> extendsC,
                                            List<Decl> decls,
                                            Option<SelfType> selfType) {
        TraitTypeHeader header = NodeFactory.makeTraitTypeHeader(NodeFactory.makeId(span, "_"),
                                                                 extendsC, decls);
        return makeObjectExpr(span, parenthesized, exprType, header,selfType);
    }

    public static ObjectExpr makeObjectExpr(Span span,
                                            boolean parenthesized,
                                            Option<Type> ty,
                                            TraitTypeHeader header,
                                            Option<SelfType> selfType) {
        ExprInfo info = NodeFactory.makeExprInfo(span, parenthesized, ty);
        return new ObjectExpr(info, header, selfType);
    }

    public static _RewriteObjectExpr make_RewriteObjectExpr(ObjectExpr expr,
                                                            Map<String, StaticParam> implicit_type_parameters) {
        List<StaticArg> staticArgs =
            new ArrayList<StaticArg>(implicit_type_parameters.size());
        List<StaticParam> stParams;
        if (implicit_type_parameters.size() == 0) {
            stParams = Collections.<StaticParam>emptyList();
        }
        else {
            stParams =
                new ArrayList<StaticParam>(implicit_type_parameters.values());
            for (String s : implicit_type_parameters.keySet()) {
                staticArgs.add(NodeFactory.makeTypeArg(NodeUtil.getSpan(expr), s));
            }
        }
        return make_RewriteObjectExpr(NodeUtil.getSpan(expr), false, Option.<Type>none(),
                                      NodeUtil.getExtendsClause(expr), NodeUtil.getDecls(expr),
                                      implicit_type_parameters, WellKnownNames.objectExprName(expr),
                                      stParams, staticArgs,
                                      Option.some(Collections.<Param>emptyList()));
    }

    public static _RewriteObjectExpr make_RewriteObjectExpr(Span span,
                                                            boolean parenthesized,
                                                            Option<Type> ty,
                                                            List<TraitTypeWhere> extendsC,
                                                            List<Decl> decls,
                                                            Map<String, StaticParam> implicitTypeParameters,
                                                            String genSymName,
                                                            List<StaticParam> staticParams,
                                                            List<StaticArg> staticArgs,
                                                            Option<List<Param>> params) {
        TraitTypeHeader header = NodeFactory.makeTraitTypeHeader(Modifiers.None,
                                                                 NodeFactory.makeId(span,genSymName),
                                                                 staticParams,
                                                                 Option.<WhereClause>none(),
                                                                 Option.<List<Type>>none(),
                                                                 Option.<Contract>none(),
                                                                 extendsC, params, decls);
        return make_RewriteObjectExpr(span, parenthesized, ty, header,
                                      implicitTypeParameters, genSymName,
                                      staticArgs);
    }

    public static _RewriteObjectExpr make_RewriteObjectExpr(Span span,
                                                            boolean parenthesized,
                                                            Option<Type> ty,
                                                            TraitTypeHeader header,
                                                            Map<String, StaticParam> implicitTypeParameters,
                                                            String genSymName,
                                                            List<StaticArg> staticArgs) {
        ExprInfo info = NodeFactory.makeExprInfo(span, parenthesized, ty);
        return new _RewriteObjectExpr(info, header,
                                      implicitTypeParameters, genSymName,
                                      staticArgs);
    }

    public static Assignment makeAssignment(Span span, Lhs lhs, Expr rhs) {
        return makeAssignment(span, Option.<Type>none(), Useful.list(lhs), Option.<FunctionalRef>none(), rhs);
    }

    public static Assignment makeAssignment(Span span, List<Lhs> lhs, Expr rhs) {
        return makeAssignment(span, Option.<Type>none(), lhs, Option.<FunctionalRef>none(), rhs);
    }

    public static Assignment makeAssignment(Span span, List<Lhs> lhs,
                                            Option<FunctionalRef> op, Expr rhs) {
        return makeAssignment(span, Option.<Type>none(), lhs, op, rhs);
    }

    public static Assignment makeAssignment(Span span, Option<Type> type,
                                            List<Lhs> lhs, Expr rhs) {
        return makeAssignment(span, type, lhs, Option.<FunctionalRef>none(), rhs);
    }

    public static Assignment makeAssignment(Span span, Option<Type> type,
                                            List<Lhs> lhs, Option<FunctionalRef> op,
                                            Expr rhs) {
        return makeAssignment(span, false, type, lhs, op, rhs,
                              Collections.<CompoundAssignmentInfo>emptyList());
    }

    public static Assignment makeAssignment(Span span,
                                            List<Lhs> lhs,
                                            Option<FunctionalRef> assignOp,
                                            Expr rhs,
                                            List<CompoundAssignmentInfo> assignmentInfos) {
        return makeAssignment(span, false, Option.<Type>none(), lhs, assignOp,
                              rhs, assignmentInfos);
    }

    public static Assignment makeAssignment(Span span,
                                            boolean parenthesized,
                                            Option<Type> ty,
                                            List<Lhs> lhs,
                                            Option<FunctionalRef> assignOp,
                                            Expr rhs,
                                            List<CompoundAssignmentInfo> assignmentInfos) {
        ExprInfo info = NodeFactory.makeExprInfo(span, parenthesized, ty);
        return new Assignment(info, lhs, assignOp, rhs, assignmentInfos);
    }

    public static Assignment makeAssignmentOld(Span span,
                                            boolean parenthesized,
                                            Option<Type> ty,
                                            List<Lhs> lhs,
                                            Option<FunctionalRef> assignOp,
                                            Expr rhs,
                                            List<FunctionalRef> opsForLhs) {
        ExprInfo info = NodeFactory.makeExprInfo(span, parenthesized, ty);

        // Make an assignment info from a functional ref.
        Lambda<FunctionalRef, CompoundAssignmentInfo> makeInfo =
                new Lambda<FunctionalRef, CompoundAssignmentInfo>() {
            public CompoundAssignmentInfo value(FunctionalRef arg) {
                return new CompoundAssignmentInfo(arg,
                                                  Option.<CoercionInvocation>none(),
                                                  Option.<CoercionInvocation>none());
            }
        };

        List<CompoundAssignmentInfo> assignmentInfos =
                CollectUtil.makeList(IterUtil.map(opsForLhs, makeInfo));

        return new Assignment(info, lhs, assignOp, rhs, assignmentInfos);
    }

    public static Block makeBlock(Expr e) {
        List<Expr> b = Collections.singletonList(e);
        return makeBlock(NodeUtil.getSpan(e), NodeUtil.getExprType(e), b);
    }

    public static Block makeBlock(Span sp, Expr e) {
        List<Expr> b = Collections.singletonList(e);
        return makeBlock(sp, NodeUtil.getExprType(e), b);
    }

    public static Block makeBlock(Span span,
                                  List<Expr> exprs) {
        return makeBlock(span, Option.<Type>none(), exprs);
    }

    public static Block makeBlock(Span span, Option<Type> exprType,
                                  boolean atomicBlock,
                                  boolean withinDo,
                                  List<Expr> exprs) {
        return makeBlock(span, false, exprType, Option.<Expr>none(),
                         atomicBlock, withinDo, exprs);
    }

    public static Block makeBlock(Span span, Option<Type> exprType,
                                  List<Expr> exprs) {
        return makeBlock(span, false, exprType, Option.<Expr>none(),
                         false, false, exprs);
    }

    public static Block makeBlock(Span span,
                                  boolean parenthesized,
                                  Option<Type> ty,
                                  Option<Expr> loc,
                                  boolean atomicBlock,
                                  boolean withinDo,
                                  List<Expr> exprs) {
        ExprInfo info = NodeFactory.makeExprInfo(span, parenthesized, ty);
        return new Block(info, loc, atomicBlock, withinDo, exprs);
    }

    public static If makeIf(Span span,
                            List<IfClause> clauses,
                            Option<Block> elseClause) {
        return makeIf(span, false, Option.<Type>none(), clauses, elseClause);
    }

    public static If makeIf(IfClause _if, Expr _else) {
        List<IfClause> ifclauses = Collections.singletonList(_if);
        List<Expr> elseBlock = Collections.singletonList(_else);
        Block _elseClause = makeBlock(NodeUtil.getSpan(_else), elseBlock);
        return makeIf(NodeFactory.makeSpan(_if, _else), ifclauses, Option.some(_elseClause));
    }

    public static If makeIf(Span sp, IfClause _if, Expr _else) {
        List<IfClause> ifclauses = Collections.singletonList(_if);
        List<Expr> elseBlock = Collections.singletonList(_else);
        Block _elseClause = makeBlock(sp, elseBlock);
        return makeIf(sp, ifclauses, Option.some(_elseClause));
    }

    public static If makeIf(IfClause _if) {
        List<IfClause> ifclauses = Collections.singletonList(_if);
        return makeIf(NodeUtil.getSpan(_if), ifclauses, Option.<Block>none());
    }

    public static If makeIf(Span sp, IfClause _if) {
        List<IfClause> ifclauses = Collections.singletonList(_if);
        return makeIf(sp, ifclauses, Option.<Block>none());
    }

    public static If makeIf(Span sp, Expr cond, Block _then, Block _else) {
        return makeIf(sp,
                      new IfClause(NodeFactory.makeSpanInfo(NodeFactory.makeSpan(cond, _else)),
                                   makeGeneratorClause(NodeUtil.getSpan(cond), cond),
                                   _then),
                      _else);
    }

    public static If makeIf(Span sp, Expr cond, Block _then) {
        return makeIf(sp,
                      new IfClause(NodeFactory.makeSpanInfo(NodeFactory.makeSpan(cond, _then)),
                                   makeGeneratorClause(NodeUtil.getSpan(cond), cond),
                                   _then));
    }

    public static If makeIf(Span span,
                            boolean parenthesized,
                            Option<Type> ty,
                            List<IfClause> clauses,
                            Option<Block> elseClause) {
        ExprInfo info = NodeFactory.makeExprInfo(span, parenthesized, ty);
        return new If(info, clauses, elseClause);
    }

    public static CaseExpr makeCaseExpr(Span span,
                                        Option<Expr> param,
                                        Option<FunctionalRef> compare,
                                        List<CaseClause> clauses,
                                        Option<Block> elseClause) {
        return makeCaseExpr(span, false, Option.<Type>none(), param, compare,
                            makeInfixEq(span), makeInfixIn(span),
                            clauses, elseClause);
    }

    public static CaseExpr makeCaseExpr(Span span,
                                        boolean parenthesized,
                                        Option<Type> ty,
                                        Option<Expr> param,
                                        Option<FunctionalRef> compare,
                                        FunctionalRef equalsOp,
                                        FunctionalRef inOp,
                                        List<CaseClause> clauses,
                                        Option<Block> elseClause) {
        ExprInfo info = NodeFactory.makeExprInfo(span, parenthesized, ty);
        return new CaseExpr(info, param, compare,
                            equalsOp, inOp, clauses, elseClause);
    }

    public static ChainExpr makeChainExpr(Expr e, Op _op, Expr _expr) {
        List<Link> links = new ArrayList<Link>(1);
        Link link = NodeFactory.makeLink(NodeFactory.makeSpan(_op, _expr),
                                         makeOpRef(NodeFactory.makeOpInfix(_op)), _expr);
        links.add(link);
        return makeChainExpr(NodeFactory.makeSpan(e, _expr), e, links);
    }

    public static ChainExpr makeChainExpr(Span sp, Expr e, Op _op, Expr _expr) {
        List<Link> links = new ArrayList<Link>(1);
        Link link = NodeFactory.makeLink(NodeFactory.makeSpan(_op, _expr),
                                         makeOpRef(NodeFactory.makeOpInfix(_op)), _expr);
        links.add(link);
        return makeChainExpr(sp, e, links);
    }

    public static ChainExpr makeChainExpr(Span span,
                                          Expr first, List<Link> links) {
        return makeChainExpr(span, false, Option.<Type>none(), first, links);
    }

    public static ChainExpr makeChainExpr(Span span,
                                          boolean parenthesized,
                                          Option<Type> ty,
                                          Expr first, List<Link> links) {
        ExprInfo info = NodeFactory.makeExprInfo(span, parenthesized, ty);
        return new ChainExpr(info, first, links);
    }

    public static _RewriteFnApp make_RewriteFnApp(Expr e_1, Expr e_2) {
        return make_RewriteFnApp(NodeUtil.spanTwo(e_1, e_2), false,
                                 Option.<Type>none(), e_1, e_2);
    }

    // For desugaring, marked as parenthesized.
    public static _RewriteFnApp make_RewriteFnApp(Span span, Expr e_1, Expr e_2) {
        return make_RewriteFnApp(span, true,
                                 Option.<Type>none(), e_1, e_2);
    }

    public static _RewriteFnApp make_RewriteFnApp(Span span,
                                                  boolean parenthesized,
                                                  Option<Type> ty,
                                                  Expr function,
                                                  Expr argument) {
        ExprInfo info = NodeFactory.makeExprInfo(span, parenthesized, ty);
        return new _RewriteFnApp(info, function, argument);
    }

    public static FieldRef makeFieldRef(FieldRef expr, Span span) {
        return makeFieldRef(span, NodeUtil.isParenthesized(expr),
                            NodeUtil.getExprType(expr), expr.getObj(), expr.getField());
    }

    public static FieldRef makeFieldRef(FieldRef expr, Expr receiver, Id field) {
        return makeFieldRef(NodeUtil.getSpan(expr), NodeUtil.isParenthesized(expr),
                            NodeUtil.getExprType(expr), receiver, field);
    }

    public static FieldRef make_RewriteFieldRef(FieldRef expr,
                                                Expr receiver, Id field) {
        return makeFieldRef(NodeUtil.getSpan(expr), NodeUtil.isParenthesized(expr),
                            NodeUtil.getExprType(expr), receiver, field);
    }

    public static FieldRef makeFieldRef(Span span, Expr receiver, Id field) {
        return makeFieldRef(span, false, Option.<Type>none(), receiver, field);
    }

    public static FieldRef makeFieldRef(Expr receiver, Id field) {
        return makeFieldRef(NodeUtil.spanTwo(receiver, field), receiver, field);
    }

    public static FieldRef makeFieldRef(Span span,
                                        boolean parenthesized,
                                        Option<Type> ty,
                                        Expr obj, Id field) {
        ExprInfo info = NodeFactory.makeExprInfo(span, parenthesized, ty);
        return new FieldRef(info, obj, field);
    }

    public static AtomicExpr makeAtomicExpr(Span span, Expr expr) {
        return makeAtomicExpr(span, false, Option.<Type>none(), expr);
    }

    public static AtomicExpr makeAtomicExpr(Span span,
                                            boolean parenthesized,
                                            Option<Type> ty,
                                            Expr expr) {
        ExprInfo info = NodeFactory.makeExprInfo(span, parenthesized, ty);
        return new AtomicExpr(info, expr);
    }

    public static For makeFor(Span span,
                              List<GeneratorClause> gens,
                              Block body) {
        return makeFor(span, false, Option.<Type>none(), gens, body);
    }

    public static For makeFor(Span span,
                              boolean parenthesized,
                              Option<Type> ty,
                              List<GeneratorClause> gens,
                              Block body) {
        ExprInfo info = NodeFactory.makeExprInfo(span, parenthesized, ty);
        return new For(info, gens, body);
    }

    public static Spawn makeSpawn(Span span,
                                  Expr body) {
        return makeSpawn(span, false, Option.<Type>none(), body);
    }

    public static Spawn makeSpawn(Span span,
                                  boolean parenthesized,
                                  Option<Type> ty,
                                  Expr body) {
        ExprInfo info = NodeFactory.makeExprInfo(span, parenthesized, ty);
        return new Spawn(info, body);
    }


    public static TryAtomicExpr makeTryAtomicExpr(Span span,
                                                  Expr expr) {
        return makeTryAtomicExpr(span, false, Option.<Type>none(), expr);
    }

    public static TryAtomicExpr makeTryAtomicExpr(Span span,
                                                  boolean parenthesized,
                                                  Option<Type> ty,
                                                  Expr expr) {
        ExprInfo info = NodeFactory.makeExprInfo(span, parenthesized, ty);
        return new TryAtomicExpr(info, expr);
    }

    public static LetFn makeLetFn(Span span,
                                  List<Expr> body,
                                  List<FnDecl> fns) {
        return makeLetFn(span, false, Option.<Type>none(),
                         body, fns);
    }

    public static LetFn makeLetFn(Span span,
                                  boolean parenthesized,
                                  Option<Type> ty,
                                  List<Expr> body,
                                  List<FnDecl> fns) {
        return makeLetFn(span, parenthesized, ty, makeBlock(span, body), fns);
    }

    public static LetFn makeLetFn(Span span,
                                  boolean parenthesized,
                                  Option<Type> ty,
                                  Block body,
                                  List<FnDecl> fns) {
        ExprInfo info = NodeFactory.makeExprInfo(span, parenthesized, ty);
        return new LetFn(info, body, fns);
    }

    public static Throw makeThrow(Span sp, String st) {
        Id id = NodeFactory.makeId(sp, WellKnownNames.fortressLibrary(), st);
        return makeThrow(sp, makeVarRef(sp, id));
    }

    public static Throw makeThrow(Span span,
                                  Expr expr) {
        return makeThrow(span, false, Option.<Type>none(), expr);
    }

    public static Throw makeThrow(Span span,
                                  boolean parenthesized,
                                  Option<Type> ty,
                                  Expr expr) {
        ExprInfo info = NodeFactory.makeExprInfo(span, parenthesized, ty);
        return new Throw(info, expr);
    }

  public static Throw makeThrow(ExprInfo info, String st) {
      Id id = NodeFactory.makeId(info.getSpan(),WellKnownNames.compilerBuiltin(), st);
	  return new Throw(info,makeVarRef(info.getSpan(), info.getExprType() ,id));
  }
    
    public static StringLiteralExpr makeStringLiteralExpr(Span span, String s) {
        return makeStringLiteralExpr(span, false, Option.<Type>none(), s);
    }

    public static StringLiteralExpr makeStringLiteralExpr(Span span,
                                                          boolean parenthesized,
                                                          Option<Type> ty,
                                                          String text) {
        ExprInfo info = NodeFactory.makeExprInfo(span, parenthesized, ty);
        return new StringLiteralExpr(info, text);
    }

    public static FloatLiteralExpr makeFloatLiteralExpr(Span span,
                                                        boolean parenthesized,
                                                        Option<Type> ty,
                                                        String text,
                                                        BigInteger intPart,
                                                        BigInteger numerator,
                                                        int denomBase,
                                                        int denomPower) {
        ExprInfo info = NodeFactory.makeExprInfo(span, parenthesized, ty);
        return new FloatLiteralExpr(info, text,
                                    intPart, numerator, denomBase, denomPower);
    }

    public static AsExpr makeAsExpr(Span span,
                                    Expr expr, Type annType) {
        return makeAsExpr(span, false, Option.<Type>none(), expr, annType);
    }

    public static AsExpr makeAsExpr(Span span,
                                    boolean parenthesized,
                                    Option<Type> ty,
                                    Expr expr, Type annType) {
        ExprInfo info = NodeFactory.makeExprInfo(span, parenthesized, ty);
        return new AsExpr(info, expr, annType);
    }

    public static AsIfExpr makeAsIfExpr(Span span,
                                        Expr expr, Type annType) {
        return makeAsIfExpr(span, false, Option.<Type>none(), expr, annType);
    }

    public static AsIfExpr makeAsIfExpr(Span span,
                                        boolean parenthesized,
                                        Option<Type> ty,
                                        Expr expr, Type annType) {
        ExprInfo info = NodeFactory.makeExprInfo(span, parenthesized, ty);
        return new AsIfExpr(info, expr, annType);
    }

    public static While makeWhile(Span sp, Expr cond) {
        // Might not work; empty Do may be naughty.
        return makeWhile(sp, makeGeneratorClause(NodeUtil.getSpan(cond), cond),
                         makeDo(sp, Collections.<Block>emptyList()));
    }

    public static While makeWhile(Span span,
                                  GeneratorClause testExpr,
                                  Do body) {
        return makeWhile(span, false, Option.<Type>none(),
                         testExpr, body);
    }

    public static While makeWhile(Span span,
                                  boolean parenthesized,
                                  Option<Type> ty,
                                  GeneratorClause testExpr,
                                  Do body) {
        ExprInfo info = NodeFactory.makeExprInfo(span, parenthesized, ty);
        return new While(info, testExpr, body);
    }

    public static Label makeLabel(Span span,
                                  Id name, Block body) {
        return makeLabel(span, false, Option.<Type>none(), name, body);
    }

    public static Label makeLabel(Span span,
                                  boolean parenthesized,
                                  Option<Type> ty,
                                  Id name, Block body) {
        ExprInfo info = NodeFactory.makeExprInfo(span, parenthesized, ty);
        return new Label(info, name, body);
    }

    public static Do makeDo(Span sp, Option<Type> t, Expr e) {
        List<Expr> b = Collections.singletonList(e);
        List<Block> body = new ArrayList<Block>(1);
        body.add(makeBlock(sp, t, false, true, b));
        return makeDo(sp, false, t, body);
    }

    public static Do makeDo(Span sp, Option<Type> t, List<Expr> exprs) {
        List<Block> body = new ArrayList<Block>(1);
        body.add(makeBlock(sp, t, false, true, exprs));
        return makeDo(sp, false, t, body);
    }

    public static Do makeDo(Span span,
                            List<Block> fronts) {
        return makeDo(span, false, Option.<Type>none(), fronts);
    }

    public static Do makeDo(Span span,
                            boolean parenthesized,
                            Option<Type> ty,
                            List<Block> fronts) {
        ExprInfo info = NodeFactory.makeExprInfo(span, parenthesized, ty);
        return new Do(info, fronts);
    }

    public static FloatLiteralExpr makeFloatLiteralExpr(Span span, String s) {
        BigInteger intPart;
        BigInteger numerator;
        int denomBase;
        int denomPower;

        // Delete every apostrophe and U+202F NARROW NO-BREAK SPACE
        s = s.replace("'", "").replace("\u202F", "");

        // Trim leading zeroes
        while (s.length() > 2 && s.charAt(0) == '0' && s.charAt(1) == '0') {
            s = s.substring(1);
        }

        int dotLoc = s.indexOf('.');
        int underLoc = s.indexOf('_');

        if (dotLoc == -1) {
            // No fraction part.
            numerator = BigInteger.ZERO;
            denomBase = 1;
            denomPower = 0;
            int base;
            String digits;

            if (underLoc == -1) {
                digits = s;
                base = 10;
            }
            else {
                digits = s.substring(0, underLoc);
                // Base other, no ".", parse as BigInteger and convert.
                String base_digits = s.substring(underLoc + 1);

                if (!Unicode.charactersOverlap(base_digits, "0123456789")) {
                    base = Unicode.numberToValue(base_digits);
                }
                else {
                    base = Integer.parseInt(base_digits);
                }
            }
            digits = dozenalHack(digits, base);
            intPart = new BigInteger(digits, base);
        }
        else {
            // There is a fraction part.

            int base;

            if (underLoc == -1) {
                base = 10;
                underLoc = s.length();
            }
            else {
                String base_digits = s.substring(underLoc + 1);
                if (!Unicode.charactersOverlap(base_digits, "0123456789")) {
                    base = Unicode.numberToValue(base_digits);
                }
                else {
                    base = Integer.parseInt(base_digits);
                }
            }
            {
                String digits = s.substring(0, dotLoc);
                if (digits.length() > 0) {
                    digits = dozenalHack(digits, base);
                    intPart = new BigInteger(digits, base);
                }
                else {
                    intPart = BigInteger.ZERO;
                }
                digits = s.substring(dotLoc + 1, underLoc);

                // TODO Getting the rounding and overflow dead right is hard.
                while (digits.length() > 1 && digits.endsWith("0")) {
                    digits = digits.substring(0, digits.length() - 1);
                }

                if (digits.length() == 0 || "0".equals(digits)) {
                    numerator = BigInteger.ZERO;
                    denomBase = 1;
                    denomPower = 0;

                }
                else {
                    digits = dozenalHack(digits, base);
                    numerator = new BigInteger(digits, base);
                    denomBase = base;
                    denomPower = digits.length();
                }
            }
        }
        return makeFloatLiteralExpr(span, false, Option.<Type>none(), s,
                                    intPart, numerator, denomBase, denomPower);
    }

    public static LetExpr makeLetExpr(final LetExpr let_expr, final List<Expr> body) {
        return let_expr.accept(new NodeAbstractVisitor<LetExpr>() {
            public LetExpr forLetFn(LetFn expr) {
                return makeLetFn(NodeUtil.getSpan(expr), body, expr.getFns());
            }
            public LetExpr forLocalVarDecl(LocalVarDecl expr) {
                return makeLocalVarDecl(NodeUtil.getSpan(expr), false, Option.<Type>none(),
                                        body, expr.getLhs(), expr.getRhs());
            }
        });
    }

    public static GeneratorClause makeGeneratorClause(Span span,
                                                      Iterable<Id> ids,
                                                      Expr expr) {
        return new GeneratorClause(NodeFactory.makeSpanInfo(span), CollectUtil.makeList(ids), expr);
    }

    public static GeneratorClause makeGeneratorClause(Span span, Expr cond) {
        return new GeneratorClause(NodeFactory.makeSpanInfo(span), Collections.<Id>emptyList(), cond);
    }

    public static FunctionalRef makeMultiJuxt(Span span) {
        return makeOpRef(NodeFactory.makeOpMultifix(NodeFactory.makeOp(span,WellKnownNames.operatorNameJuxtaposition)));
    }

    public static FunctionalRef makeInfixJuxt(Span span) {
        return makeOpRef(NodeFactory.makeOpInfix(NodeFactory.makeOp(span,WellKnownNames.operatorNameJuxtaposition)));
    }

    public static FunctionalRef makeInfixEq(Span span){
        return makeOpRef(NodeFactory.makeOpInfix(NodeFactory.makeOp(span, WellKnownNames.operatorNameEQUALS)));
    }

    public static FunctionalRef makeInfixIn(Span span){
        return makeOpRef(NodeFactory.makeOpInfix(NodeFactory.makeOp(span, WellKnownNames.operatorNameIN)));
    }

    public static FunctionalRef makeInfixAnd(Span span){
        return makeOpRef(NodeFactory.makeOpInfix(NodeFactory.makeOp(span, WellKnownNames.operatorNameAND)));
    }

    public static Expr makeReceiver(Iterable<Id> ids) {
        Iterator<Id> iter = ids.iterator();
        Expr expr = makeVarRef(iter.next());
        while (iter.hasNext()) {
            Id id = iter.next();
            expr = makeFieldRef(expr, id);
        }
        return expr;
    }

    public static Expr makeInParentheses(Expr expr) {
        return expr.accept(new NodeAbstractVisitor<Expr>() {
            @Override
            public Expr for_RewriteFnApp(_RewriteFnApp that) {
                return make_RewriteFnApp(NodeUtil.getSpan(that), true, NodeUtil.getExprType(that),
                                         that.getFunction(), that.getArgument());
            }
        public Expr forAsExpr(AsExpr e) {
            return makeAsExpr(NodeUtil.getSpan(e), true, NodeUtil.getExprType(e),
                              e.getExpr(), e.getAnnType());
        }
        public Expr forAsIfExpr(AsIfExpr e) {
            return makeAsIfExpr(NodeUtil.getSpan(e), true, NodeUtil.getExprType(e),
                                e.getExpr(), e.getAnnType());
        }

        public Expr forAssignment(Assignment e) {
            return makeAssignment(NodeUtil.getSpan(e), true, NodeUtil.getExprType(e),
                                  e.getLhs(), e.getAssignOp(), e.getRhs(),
                                  e.getAssignmentInfos());
        }
        public Expr forBlock(Block e) {
            return makeBlock(NodeUtil.getSpan(e), true, NodeUtil.getExprType(e), e.getLoc(),
                             e.isAtomicBlock(), e.isWithinDo(), e.getExprs());
        }
        public Expr forCaseExpr(CaseExpr e) {
            return makeCaseExpr(NodeUtil.getSpan(e), true, NodeUtil.getExprType(e) , e.getParam(),
                                e.getCompare(), e.getEqualsOp(), e.getInOp(),
                                e.getClauses(), e.getElseClause());
        }
        public Expr forDo(Do e) {
            return makeDo(NodeUtil.getSpan(e), true, NodeUtil.getExprType(e), e.getFronts());
        }
        public Expr forFor(For e) {
            return makeFor(NodeUtil.getSpan(e), true, NodeUtil.getExprType(e), e.getGens(),
                           e.getBody());
        }
        public Expr forIf(If e) {
            return makeIf(NodeUtil.getSpan(e), true, NodeUtil.getExprType(e),
                          e.getClauses(), e.getElseClause());
        }
        public Expr forLabel(Label e) {
            return makeLabel(NodeUtil.getSpan(e), true, NodeUtil.getExprType(e),
                             e.getName(), e.getBody());
        }
        public Expr forMathPrimary(MathPrimary e) {
            return makeMathPrimary(NodeUtil.getSpan(e), true, NodeUtil.getExprType(e),
                                   e.getMultiJuxt(), e.getInfixJuxt(),
                                   e.getFront(), e.getRest());
        }
        public Expr forObjectExpr(ObjectExpr e) {
            return makeObjectExpr(NodeUtil.getSpan(e), true, NodeUtil.getExprType(e),
                                  e.getHeader(), e.getSelfType());
        }
        public Expr for_RewriteObjectExpr(_RewriteObjectExpr e) {
            return make_RewriteObjectExpr(NodeUtil.getSpan(e), true,
                                          NodeUtil.getExprType(e),
                                          e.getHeader(),
                                          e.getImplicitTypeParameters(),
                                          e.getGenSymName(),
                                          e.getStaticArgs());
        }
        public Expr forTry(Try e) {
            return makeTry(NodeUtil.getSpan(e), true, NodeUtil.getExprType(e), e.getBody(),
                           e.getCatchClause(), e.getForbidClause(),
                           e.getFinallyClause());
        }
        public Expr forTupleExpr(TupleExpr e) {
            return makeTupleExpr(NodeUtil.getSpan(e), true, NodeUtil.getExprType(e),
                                 e.getExprs(), e.getVarargs(), e.getKeywords(),
                                 e.isInApp());
        }
        public Expr forTypecase(Typecase e) {
            return makeTypecase(NodeUtil.getSpan(e), true, NodeUtil.getExprType(e),
                                e.getBindExpr(), e.getClauses(),
                                e.getElseClause());
        }
        public Expr forWhile(While e) {
            return makeWhile(NodeUtil.getSpan(e), true, NodeUtil.getExprType(e),
                             e.getTestExpr(), e.getBody());
        }
        public Expr forAccumulator(Accumulator e) {
            return makeAccumulator(NodeUtil.getSpan(e), true, NodeUtil.getExprType(e),
                                   e.getStaticArgs(),
                                   e.getAccOp(), e.getGens(), e.getBody());
        }
        public Expr forAtomicExpr(AtomicExpr e) {
            return makeAtomicExpr(NodeUtil.getSpan(e), true, NodeUtil.getExprType(e), e.getExpr());
        }
        public Expr forExit(Exit e) {
            return makeExit(NodeUtil.getSpan(e), true, NodeUtil.getExprType(e),
                            e.getTarget(), e.getReturnExpr());
        }

        public Expr forSpawn(Spawn e) {
            return makeSpawn(NodeUtil.getSpan(e), true, NodeUtil.getExprType(e), e.getBody());
        }
        public Expr forThrow(Throw e) {
            return makeThrow(NodeUtil.getSpan(e), true, NodeUtil.getExprType(e), e.getExpr());
        }
        public Expr forTryAtomicExpr(TryAtomicExpr e) {
            return makeTryAtomicExpr(NodeUtil.getSpan(e), true, NodeUtil.getExprType(e),
                                     e.getExpr());
        }
        public Expr forFnExpr(FnExpr e) {
            return makeFnExpr(NodeUtil.getSpan(e), true, NodeUtil.getExprType(e), e.getHeader().getName(),
                              e.getHeader().getStaticParams(), e.getHeader().getParams(),
                              e.getHeader().getReturnType(), e.getHeader().getWhereClause(),
                              e.getHeader().getThrowsClause(), e.getBody());
        }
        public Expr forLetFn(LetFn e) {
            return makeLetFn(NodeUtil.getSpan(e), true, NodeUtil.getExprType(e), e.getBody(), e.getFns());
        }
        public Expr forLocalVarDecl(LocalVarDecl e) {
            return makeLocalVarDecl(NodeUtil.getSpan(e), true, NodeUtil.getExprType(e),
                                    e.getBody(), e.getLhs(), e.getRhs());
        }
        public Expr forOpExpr(OpExpr e) {
            return makeOpExpr(NodeUtil.getSpan(e), true, NodeUtil.getExprType(e), e.getOp(), e.getArgs());
        }
        @Override
            public Expr forAmbiguousMultifixOpExpr(AmbiguousMultifixOpExpr that) {
            return makeAmbiguousMultifixOpExpr(that, true);
        }
        public Expr forArrayElement(ArrayElement e) {
            return makeArrayElement(NodeUtil.getSpan(e), true, NodeUtil.getExprType(e),
                                    e.getStaticArgs(), e.getElement());
        }
        public Expr forArrayElements(ArrayElements e) {
            return makeArrayElements(NodeUtil.getSpan(e), true, NodeUtil.getExprType(e),
                                     e.getStaticArgs(), e.getDimension(),
                                     e.getElements(), e.isOutermost());
        }
        public Expr forFloatLiteralExpr(FloatLiteralExpr e) {
            return makeFloatLiteralExpr(NodeUtil.getSpan(e), true, NodeUtil.getExprType(e),
                                        e.getText(),
                    e.getIntPart(), e.getNumerator(),
                    e.getDenomBase(), e.getDenomPower());
        }
        public Expr forIntLiteralExpr(IntLiteralExpr e) {
            return makeIntLiteralExpr(NodeUtil.getSpan(e), true, NodeUtil.getExprType(e),
                                      e.getText(), e.getIntVal());
        }
        public Expr forBooleanLiteralExpr(BooleanLiteralExpr e) {
            return makeBooleanLiteralExpr(NodeUtil.getSpan(e), true, NodeUtil.getExprType(e),
                                      e.getText(), e.getBooleanVal());
        }
        public Expr forCharLiteralExpr(CharLiteralExpr e) {
            return makeCharLiteralExpr(NodeUtil.getSpan(e), true, NodeUtil.getExprType(e),
                                       e.getText(), e.getCharVal());
        }
        public Expr forStringLiteralExpr(StringLiteralExpr e) {
            return makeStringLiteralExpr(NodeUtil.getSpan(e), true, NodeUtil.getExprType(e),
                                         e.getText());
        }
        public Expr forVoidLiteralExpr(VoidLiteralExpr e) {
            return makeVoidLiteralExpr(NodeUtil.getSpan(e), true, NodeUtil.getExprType(e),
                                       e.getText());
        }
        public Expr forVarRef(VarRef e) {
            return makeVarRef(NodeUtil.getSpan(e), true, NodeUtil.getExprType(e), e.getVarId(),
                              e.getStaticArgs(), e.getLexicalDepth());
        }
        public Expr forArrayComprehension(ArrayComprehension e) {
            return makeArrayComprehension(NodeUtil.getSpan(e), true, NodeUtil.getExprType(e),
                                          e.getStaticArgs(), e.getClauses());
        }
        public Expr forChainExpr(ChainExpr e) {
            return makeChainExpr(NodeUtil.getSpan(e), true, NodeUtil.getExprType(e), e.getFirst(),
                                 e.getLinks());
        }
        public Expr forFieldRef(FieldRef e) {
            return makeFieldRef(NodeUtil.getSpan(e), true, NodeUtil.getExprType(e),
                                e.getObj(), e.getField());
        }
        public Expr forMethodInvocation(MethodInvocation e) {
            return makeMethodInvocation(NodeUtil.getSpan(e), true, NodeUtil.getExprType(e),
                                        e.getObj(), e.getMethod(),
                                        e.getStaticArgs(), e.getArg());
        }
        public Expr forJuxt(Juxt e) {
            return makeJuxt(NodeUtil.getSpan(e), true, NodeUtil.getExprType(e),
                            e.getMultiJuxt(), e.getInfixJuxt(),
                            e.getExprs(), e.isFnApp(), e.isTight());
        }
        public Expr forFnRef(FnRef e) {
            return makeFnRef(NodeUtil.getSpan(e), true, NodeUtil.getExprType(e), e.getStaticArgs(),
                             e.getLexicalDepth(), e.getOriginalName(), e.getNames(),
                             e.getInterpOverloadings(), e.getNewOverloadings(), e.getOverloadingType());
        }
        public Expr forOpRef(OpRef e) {
            return makeOpRef(NodeUtil.getSpan(e), true, NodeUtil.getExprType(e),
                             e.getStaticArgs(), e.getLexicalDepth(),
                             e.getOriginalName(), e.getNames(),
                             e.getInterpOverloadings(), e.getNewOverloadings(), e.getOverloadingType());
        }
        public Expr forSubscriptExpr(SubscriptExpr e) {
            return makeSubscriptExpr(NodeUtil.getSpan(e), true, NodeUtil.getExprType(e),
                                     e.getObj(), e.getSubs(), e.getOp(),
                                     e.getStaticArgs());
        }
        public Expr forTemplateGapExpr(TemplateGapExpr e) {
            return new TemplateGapExpr(NodeFactory.makeExprInfo(NodeUtil.getSpan(e)),
                                       e.getGapId(), e.getTemplateParams());
        }

        public Expr for_SyntaxTransformationExpr(_SyntaxTransformationExpr e){
            return e;
        }

        public Expr defaultCase(Node x) {
            return bug(x, "makeInParentheses: " + x.getClass() +
                    " is not a subtype of Expr.");
        }
        });
    }

    /**
     * Expected to be called on an OpRef that refers to an Op, not an Enclosing.
     * Creates an OpExpr where every Op in OpRef, including original name, is
     * made into an infix operator.
     */
    private static Expr makeInfixOpExpr(Span s, Expr lhs, FunctionalRef op, Expr rhs) {
        List<IdOrOp> new_ops = CollectUtil.makeList(IterUtil.map(op.getNames(), new Lambda<IdOrOp,IdOrOp>(){
                    public IdOrOp value(IdOrOp arg0) {
                        if( ! (arg0 instanceof Op) )
                            return bug("Can't make an postfix operator out of an identifier.");
                        else if( ((Op)arg0).isEnclosing() )
                            return bug("Can't make an infix operator out of an enclosing operator.");
                        else
                            return NodeFactory.makeOpInfix((Op)arg0);
                    }}));
     // We are remaking this because we think that the Interpreter expects originalName to be infix
     Op new_original_name;
     if( ((Op)op.getOriginalName()).isEnclosing() )
      return bug("Can't make an infix operator out of an enclosing operator.");
     else
      new_original_name = NodeFactory.makeOpInfix((Op)op.getOriginalName());

     FunctionalRef new_op = makeOpRef(NodeUtil.getSpan(op), NodeUtil.isParenthesized(op),
                                      NodeUtil.getExprType(op), op.getStaticArgs(),
                                      op.getLexicalDepth(), new_original_name,
                                      new_ops, op.getInterpOverloadings(),
                                      op.getNewOverloadings(),
                                      op.getOverloadingType());
     return makeOpExpr(new_op, lhs, rhs);
    }

    private static Expr makePostfixOpExpr(Span s, Expr e, FunctionalRef op) {
        List<IdOrOp> new_ops = CollectUtil.makeList(IterUtil.map(op.getNames(), new Lambda<IdOrOp,IdOrOp>(){
                    public IdOrOp value(IdOrOp arg0) {
                        if( ! (arg0 instanceof Op) )
                            return bug("Can't make an postfix operator out of an identifier.");
                        else if( ((Op)arg0).isEnclosing() )
                            return bug("Can't make an postfix operator out of an enclosing operator.");
                        else
                            return NodeFactory.makeOpPostfix((Op)arg0);
                    }}));
     // We are remaking this because we think that the Interpreter expects originalName to be postfix
     Op new_original_name;
     if( ((Op)op.getOriginalName()).isEnclosing() )
      return bug("Can't make an postfix operator out of an enclosing operator.");
     else
      new_original_name = NodeFactory.makeOpPostfix((Op)op.getOriginalName());

     FunctionalRef new_op = makeOpRef(NodeUtil.getSpan(op), NodeUtil.isParenthesized(op),
                                      NodeUtil.getExprType(op), op.getStaticArgs(),
                                      op.getLexicalDepth(), new_original_name,
                                      new_ops, op.getInterpOverloadings(),
                                      op.getNewOverloadings(),
                                      op.getOverloadingType());
     return makeOpExpr(e, new_op);
    }

    public static Expr simplifyMathPrimary(Span span, Expr front, MathItem mi) {
        if (mi instanceof ExprMI) {
            Expr expr = ((ExprMI)mi).getExpr();
            return makeTightJuxt(span, front, expr);
        } else if (mi instanceof ExponentiationMI) {
            ExponentiationMI expo = (ExponentiationMI)mi;
            Option<Expr> expr = expo.getExpr();
            if (expr.isSome()) // ^ Exponent
             return makeInfixOpExpr(span, front, expo.getOp(), expr.unwrap());
            else // ExponentOp
                return makePostfixOpExpr(span, front, expo.getOp());
        } else { // mi instanceof SubscriptingMI
            SubscriptingMI sub = (SubscriptingMI)mi;
            return makeSubscriptExpr(span, front, sub.getExprs(),
                    Option.wrap(sub.getOp()),
                    sub.getStaticArgs());
        }
    }

    public static TemplateGapExpr makeTemplateGapExpr(Span s, Id id, List<Id> params) {
        return new TemplateGapExpr(NodeFactory.makeExprInfo(s), id, params);
    }

    public static TemplateGapSimpleExpr makeTemplateGapSimpleExpr(Span s, Id id, List<Id> params) {
        return new TemplateGapSimpleExpr(NodeFactory.makeExprInfo(s), id, params);
    }

    public static TemplateGapPrimary makeTemplateGapPrimary(Span s, Id id, List<Id> params) {
        return new TemplateGapPrimary(NodeFactory.makeExprInfo(s), id, params);
    }

    public static TemplateGapFnExpr makeTemplateGapFnExpr(Span s, Id id, List<Id> params) {
        //Expr body = makeVarRef(id);
        return new TemplateGapFnExpr(NodeFactory.makeExprInfo(s), id, params);
    }

    public static TemplateGapJuxt makeTemplateGapJuxt(Span s, Id id, List<Id> params) {
        return new TemplateGapJuxt(NodeFactory.makeExprInfo(s), id, params);
    }

    public static TemplateGapName makeTemplateGapName(Span s, Id id, List<Id> params) {
        return new TemplateGapName(NodeFactory.makeSpanInfo(s), id, params);
    }

    public static TemplateGapId makeTemplateGapId(Span s, Id id, List<Id> params) {
        return new TemplateGapId(NodeFactory.makeSpanInfo(s), id, params);
    }

    public static TemplateGapLiteralExpr makeTemplateGapLiteralExpr(Span s, Id id, List<Id> params) {
        return new TemplateGapLiteralExpr(NodeFactory.makeExprInfo(s), id, params);
    }

    public static TemplateGapNumberLiteralExpr makeTemplateGapNumberLiteralExpr(Span s, Id id, List<Id> params) {
        return new TemplateGapNumberLiteralExpr(NodeFactory.makeExprInfo(s), id, params);
    }

    public static TemplateGapFloatLiteralExpr makeTemplateGapFloatLiteralExpr(Span s, Id id, List<Id> params) {
        return new TemplateGapFloatLiteralExpr(NodeFactory.makeExprInfo(s), id, params);
    }

    public static TemplateGapIntLiteralExpr makeTemplateGapIntLiteralExpr(Span s, Id id, List<Id> params) {
        return new TemplateGapIntLiteralExpr(NodeFactory.makeExprInfo(s), id, params);
    }

    public static TemplateGapCharLiteralExpr makeTemplateGapCharLiteralExpr(Span s, Id id, List<Id> params) {
        return new TemplateGapCharLiteralExpr(NodeFactory.makeExprInfo(s), id, params);
    }

    public static TemplateGapStringLiteralExpr makeTemplateGapStringLiteralExpr(Span s, Id id, List<Id> params) {
        return new TemplateGapStringLiteralExpr(NodeFactory.makeExprInfo(s), id, params);
    }

    public static TemplateGapVoidLiteralExpr makeTemplateGapVoidLiteralExpr(Span s, Id id, List<Id> params) {
        return new TemplateGapVoidLiteralExpr(NodeFactory.makeExprInfo(s), id, params);
    }

    //Span in_span, boolean in_parenthesized, Id in_obj, List<StaticArg> in_staticArgs
    public static Expr make_RewriteObjectRef(boolean parenthesized, Id in_obj) {
        return make_RewriteObjectRef(parenthesized, in_obj, Collections.<StaticArg>emptyList());
    }

    public static NonParenthesisDelimitedMI makeNonParenthesisDelimitedMI(Span span,
                                                                          Expr expr) {
        return new NonParenthesisDelimitedMI(NodeFactory.makeSpanInfo(span), expr);
    }

    public static ParenthesisDelimitedMI makeParenthesisDelimitedMI(Span span,
                                                                    Expr expr) {
        return new ParenthesisDelimitedMI(NodeFactory.makeSpanInfo(span), expr);
    }

    public static ExponentiationMI makeExponentiationMI(Span span,
                                                        FunctionalRef op,
                                                        Option<Expr> expr) {
        return new ExponentiationMI(NodeFactory.makeSpanInfo(span), op, expr);
    }

    public static SubscriptingMI makeSubscriptingMI(Span span,
                                                    Op op, List<Expr> exprs,
                                                    List<StaticArg> sargs) {
        return new SubscriptingMI(NodeFactory.makeSpanInfo(span), op, exprs, sargs);
    }

// (* Turn an expr list into a single TightJuxt *)
// let build_primary (p : expr list) : expr =
//   match p with
//     | [e] -> e
//     | _ ->
//         let es = List.rev p in
//           Node.node (span_all es) (`TightJuxt es)
    public static Expr buildPrimary(PureList<Expr> exprs) {
        if (exprs.size() == 1) return ((Cons<Expr>)exprs).getFirst();
        else {
            exprs = exprs.reverse();
            List<Expr> javaList = Useful.immutableTrimmedList(exprs);
            Span span;
            if ( javaList.size() == 0 )
                span = NodeFactory.parserSpan;
            else
                span = NodeUtil.spanAll(javaList.toArray(new AbstractNode[0]),
                                        javaList.size());
            return makeTightJuxt(span, javaList);
        }
    }

// let build_block (exprs : expr list) : expr list =
//   List_aux.foldr
//     (fun e es ->
//        match e.node_data with
//          | `LetExpr (le,[]) -> [{ e with node_data = `LetExpr (le, es) }]
//          | `LetExpr _ -> raise (Failure "misparsed variable introduction!")
//          | _ -> e :: es)
//     exprs
//     []
//
// let do_block (body : expr list) : expr =
//   let span = span_all body in
//     node span (`FlowExpr (node span (`BlockExpr (build_block body))))
    public static Block doBlock(Span span) {
        return makeBlock(span, Collections.<Expr>emptyList());
    }

    public static Block doBlock(BufferedWriter writer, List<Expr> exprs) {
        Span span;
        if ( exprs.size() == 0 )
            span = NodeFactory.parserSpan;
        else
            span = NodeUtil.spanAll(exprs.toArray(new AbstractNode[0]), exprs.size());
        List<Expr> es = new ArrayList<Expr>();
        Collections.reverse(exprs);
        for (Expr e : exprs) {
            if (e instanceof LetExpr) {
                LetExpr _e = (LetExpr)e;
                if (_e.getBody().getExprs().isEmpty()) {
                    if (_e instanceof LocalVarDecl) {
                        NodeUtil.validId(writer, ((LocalVarDecl)_e).getLhs());
                    }
                    _e = makeLetExpr(_e, es);
                    es = new ArrayList<Expr>();
                    es.add((Expr)_e);
                } else {
                    NodeUtil.log(writer, NodeUtil.getSpan(e), "Misparsed variable introduction!");
                }
            } else {
                if (isEquality(e) && !NodeUtil.isParenthesized(e))
                    NodeUtil.log(writer, NodeUtil.getSpan(e),
                                 "Equality testing expressions should be parenthesized.");
                else es.add(0, e);
            }
        }
        return makeBlock(span, es);
    }

    private static boolean isEquality(Expr expr) {
        if (expr instanceof ChainExpr) {
            ChainExpr e = (ChainExpr)expr;
            List<Link> links = e.getLinks();
            if (links.size() == 1) {
                IdOrOp op = links.get(0).getOp().getOriginalName();
                return (op instanceof Op && ((Op)op).getText().equals("="));
            } else return false;
        } else return false;
    }

// let rec multi_dim_cons (expr : expr)
//                        (dim : int)
//                        (multi : multi_dim_expr) : multi_dim_expr =
//   let elem = multi_dim_elem expr in
//   let span = span_two expr multi in
//     match multi.node_data with
//       | `ArrayElement _ ->
//           multi_dim_row span dim [ elem; multi ]
//       | `ArrayElements
//           { node_span = row_span;
//             node_data =
//               { multi_dim_row_dimension = row_dim;
//                 multi_dim_row_elements = elements; } } ->
//           if dim = row_dim
//           then multi_dim_row span dim (elem :: elements)
//           else if dim > row_dim
//           then multi_dim_row span dim [ elem; multi ]
//           else
//             (match elements with
//                | [] -> Errors.internal_error row_span
//                    "empty array/matrix literal"
//                | first::rest ->
//                    multi_dim_row span row_dim
//                      (multi_dim_cons expr dim first :: rest))
    private static ArrayExpr multiDimElement(Expr expr) {
        return makeArrayElement(expr);
    }
    private static ArrayElements addOneMultiDim(BufferedWriter writer,
                                                ArrayExpr multi, int dim,
                                                Expr expr){
        Span span = NodeUtil.spanTwo(multi, expr);
        ArrayExpr elem = multiDimElement(expr);
        if (multi instanceof ArrayElement) {
            List<ArrayExpr> elems = new ArrayList<ArrayExpr>();
            elems.add(multi);
            elems.add(elem);
            return makeArrayElements(span, dim, elems);
        } else if (multi instanceof ArrayElements) {
            ArrayElements m = (ArrayElements)multi;
            int _dim = m.getDimension();
            List<ArrayExpr> elements = m.getElements();
            if (dim == _dim) {
                elements.add(elem);
                return makeArrayElements(span, dim, elements);
            } else if (dim > _dim) {
                List<ArrayExpr> elems = new ArrayList<ArrayExpr>();
                elems.add(multi);
                elems.add(elem);
                return makeArrayElements(span, dim, elems);
            } else if (elements.size() == 0) {
                NodeUtil.log(writer, NodeUtil.getSpan(multi),
                             "Empty array/matrix literal.");
                return makeArrayElements(span, _dim, elements);
            } else { // if (dim < _dim)
                int index = elements.size()-1;
                ArrayExpr last = elements.get(index);
                elements.set(index, addOneMultiDim(writer, last, dim, expr));
                return makeArrayElements(span, _dim, elements);
            }
        } else {
            NodeUtil.log(writer, NodeUtil.getSpan(multi),
                         "ArrayElement or ArrayElements is expected.");
            return makeArrayElements(span, 0, Collections.<ArrayExpr>emptyList());
        }
    }
    public static ArrayElements multiDimCons(BufferedWriter writer, Expr init,
                                             List<Pair<Integer,Expr>> rest) {
        ArrayExpr _init = multiDimElement(init);
        if (rest.isEmpty()) {
            return bug(init, "multiDimCons: empty rest");
        } else {
            Pair<Integer,Expr> pair = rest.get(0);
            Expr expr = pair.getB();
            List<ArrayExpr> elems = new ArrayList<ArrayExpr>();
            elems.add(_init);
            elems.add(multiDimElement(expr));
            ArrayElements result = makeArrayElements(NodeUtil.spanTwo(_init,expr),
                                                     pair.getA(), elems);
            for (Pair<Integer,Expr> _pair : rest.subList(1, rest.size())) {
                int _dim   = _pair.getA();
                Expr _expr = _pair.getB();
                Span span = NodeUtil.spanTwo(result, _expr);
                result = addOneMultiDim(writer, result, _dim, _expr);
            }
            return result;
        }
    }

    public static Expr makeArgumentExpr(Span span, List<Expr> args) {
        if ( args.size() > 1 ) {

            // Construct the tuple type.
            Lambda<Expr, Type> getType = new Lambda<Expr, Type>() {
                public Type value(Expr expr) {
                    return expr.getInfo().getExprType().unwrap();
                }
            };
            List<Type> argTypes = CollectUtil.makeList(IterUtil.map(args, getType));
            Type type = NodeFactory.makeTupleType(span, argTypes);

            // Construct the tuple.
            ExprInfo info = new ExprInfo(span, true, Option.some(type));
            return new TupleExpr(info, args, Option.<Expr>none(), CollectUtil.<KeywordExpr>emptyList(), true);
        } else if ( args.size() == 1 )
            return args.get(0);
        else
            return makeVoidLiteralExpr(span);
    }

// let rec unpasting_cons (span : span)
//                        (one : unpasting)
//                        (sep : int)
//                        (two : unpasting) : unpasting =
//   match two.node_data with
//     | `UnpastingBind _ | `UnpastingNest _ -> unpasting_split span sep [one;two]
//     | `UnpastingSplit split ->
//         (match split.node_data with
//            | { unpasting_split_elems = (head :: tail) as elems;
//                unpasting_split_dim = dim; } ->
//                if sep > dim then unpasting_split span sep [one;two]
//                else if sep < dim then
//                  unpasting_split span dim
//                    (unpasting_cons (span_two one head) one sep head :: tail)
//                else (* sep = dim *)
//                  unpasting_split span dim (one :: elems)
//            | _ -> Errors.internal_error span "Empty unpasting.")
/*
    public static Unpasting unpastingCons(BufferedWriter writer,
                                          Span span, Unpasting one, int sep,
                                          Unpasting two) {
        List<Unpasting> onetwo = new ArrayList<Unpasting>();
        onetwo.add(one);
        onetwo.add(two);
        if (two instanceof UnpastingBind) {
            return new UnpastingSplit(span, onetwo, sep);
        } else if (two instanceof UnpastingSplit) {
            UnpastingSplit split = (UnpastingSplit)two;
            List<Unpasting> elems = split.getElems();
            if (elems.size() != 0) {
                int dim = split.getDim();
                if (sep > dim) {
                    return new UnpastingSplit(span, onetwo, sep);
                } else if (sep < dim) {
                    Unpasting head = elems.get(0);
                    elems.set(0, unpastingCons(spanTwo(one,head),one,sep,head));
                    return new UnpastingSplit(span, elems, dim);
                } else { // sep = dim
                    elems.add(0, one);
                    return new UnpastingSplit(span, elems, dim);
                }
            } else { // elems.size() == 0
                NodeUtil.log(writer, two.getSpan(), "Empty unpasting.");
                return new UnpastingSplit(span, elems, 0);
            }
        } else { //    !(two instanceof UnpastingBind)
                 // && !(two instanceof UnpastingSplit)
            return bug(two, "UnpastingBind or UnpastingSplit expected.");
        }
    }
*/

}
