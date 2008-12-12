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

package com.sun.fortress.nodes_util;

import static com.sun.fortress.exceptions.InterpreterBug.bug;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.sun.fortress.interpreter.glue.WellKnownNames;
import com.sun.fortress.nodes.*;
import com.sun.fortress.parser_util.FortressUtil;
import com.sun.fortress.useful.BATree;
import com.sun.fortress.useful.Useful;

import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.lambda.Lambda;
import edu.rice.cs.plt.tuple.Option;

public class ExprFactory {

    private static FunctionalRef multiJuxt = makeMultiJuxt();
    private static FunctionalRef infixJuxt = makeInfixJuxt();
    public static int lexicalDepth = -2147483648;

    public static ArrayElement makeArrayElement(Expr elem) {
        return makeArrayElement(elem.getSpan(), false, Option.<Type>none(),
                                Collections.<StaticArg>emptyList(), elem);
    }

    public static ArrayElement makeArrayElement(Span span, boolean parenthesized,
                                                Option<Type> ty,
                                                List<StaticArg> staticArgs,
                                                Expr elem) {
        return new ArrayElement(span, parenthesized, ty, staticArgs, elem);
    }

    public static ArrayElements makeArrayElements(Span span, int dim,
                                                  List<ArrayExpr> elems) {
        return makeArrayElements(span, false, Option.<Type>none(),
                                 Collections.<StaticArg>emptyList(), dim, elems,
                                 false);
    }

    public static ArrayElements makeArrayElements(ArrayElements a,
                                                  boolean outermost) {
        return makeArrayElements(a.getSpan(), a.isParenthesized(), a.getExprType(),
                                 a.getStaticArgs(), a.getDimension(),
                                 a.getElements(), outermost);
    }

    public static ArrayElements makeArrayElements(ArrayElements a,
                                                  List<StaticArg> staticArgs,
                                                  boolean outermost) {
        return makeArrayElements(a.getSpan(), a.isParenthesized(), a.getExprType(),
                                 staticArgs, a.getDimension(), a.getElements(),
                                 outermost);
    }

    public static ArrayElements makeArrayElements(Span span, boolean parenthesized,
                                                  Option<Type> ty,
                                                  List<StaticArg> staticArgs,
                                                  int dim,
                                                  List<ArrayExpr> elems,
                                                  boolean outermost) {
        return new ArrayElements(span, parenthesized, ty, staticArgs, dim, elems,
                                 outermost);
    }

    public static MathPrimary makeMathPrimary(Span span, Expr front,
                                              List<MathItem> rest) {
        return makeMathPrimary(span, false, Option.<Type>none(),
                               multiJuxt, infixJuxt, front, rest);
    }

    public static MathPrimary makeMathPrimary(Span span, boolean parenthesized,
                                              Option<Type> ty, FunctionalRef multi,
                                              FunctionalRef infix, Expr front,
                                              List<MathItem> rest) {
        return new MathPrimary(span, parenthesized, ty, multi, infix, front, rest);
    }

    public static MethodInvocation makeMethodInvocation(FieldRef that, Expr obj,
                                                        Id field, Expr expr) {
        return makeMethodInvocation(that.getSpan(), that.isParenthesized(),
                                    that.getExprType(), obj, field, expr);
    }

    public static MethodInvocation makeMethodInvocation(Span span,
                                                        Expr receiver,
                                                        Id method,
                                                        Expr arg) {
        return makeMethodInvocation(span, false, Option.<Type>none(),
                                    receiver, method, arg);
    }
    public static MethodInvocation makeMethodInvocation(Span span,
                                                        Expr receiver,
                                                        Id method,
                                                        List<StaticArg> staticArgs,
                                                        Expr arg) {
        return makeMethodInvocation(span, false, Option.<Type>none(),
                                    receiver, method, staticArgs, arg);
    }

    public static MethodInvocation makeMethodInvocation(Span span,
                                                        boolean isParenthesized,
                                                        Option<Type> type,
                                                        Expr obj, Id field,
                                                        Expr expr) {
        return makeMethodInvocation(span, isParenthesized, type, obj, field,
                                    Collections.<StaticArg>emptyList(), expr);
    }

    public static MethodInvocation makeMethodInvocation(Span span,
                                                        boolean isParenthesized,
                                                        Option<Type> type,
                                                        Expr obj, Id field,
                                                        List<StaticArg> staticArgs,
                                                        Expr expr) {
        return new MethodInvocation(span, isParenthesized, type, obj, field,
                                    staticArgs, expr);
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
        return makeOpExpr(FortressUtil.spanTwo(e, op), false,
                          Option.<Type>none(),
                          op, Useful.list(e));
    }

    public static OpExpr makeOpExpr(FunctionalRef op, Expr e_1, Expr e_2) {
        return makeOpExpr(FortressUtil.spanTwo(e_1, e_2), op, e_1, e_2);
    }

    public static OpExpr makeOpExpr(Span span, boolean isParenthesized,
                                    Option<Type> ty, FunctionalRef op,
                                    List<Expr> exprs) {
        return new OpExpr(span, isParenthesized, ty, op, exprs);
    }

    public static Juxt makeTightJuxt(Juxt that, List<Expr> exprs) {
     return makeJuxt(that.getSpan(), that.isParenthesized(),
                     that.getExprType(),
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
                        Option.<Type>none(), multiJuxt, infixJuxt,
                        exprs, isFnApp, tight);
    }

    public static Juxt makeJuxt(Span span, boolean isParenthesized,
                                Option<Type> type,
                                FunctionalRef multi, FunctionalRef infix,
                                List<Expr> exprs, boolean isFnApp,
                                boolean tight) {
        return new Juxt(span, isParenthesized, type, multi, infix,
                        exprs, isFnApp, tight);
    }

    public static FnRef make_RewriteFnRefOverloading(Span span, FnRef original, Type type) {
        return makeFnRef(span, original.isParenthesized(), original.getExprType(),
                         original.getStaticArgs(), original.getLexicalDepth(),
                         original.getOriginalName(), original.getNames(),
                         original.getOverloadings(), Option.<Type>some(type));
    }

    public static FnRef makeFnRef(Id name) {
        return makeFnRef(name.getSpan(), name);
    }

    public static FnRef makeFnRef(Span span, Id name) {
        List<IdOrOp> names = Collections.<IdOrOp>singletonList(name);
        return makeFnRef(span, name, Collections.<StaticArg>emptyList());
    }

    public static FnRef makeFnRef(Span span, Id name, List<StaticArg> sargs) {
        List<IdOrOp> names = Collections.<IdOrOp>singletonList(name);
        return makeFnRef(span, false, Option.<Type>none(), sargs,
                         lexicalDepth, name,
                         Collections.<IdOrOp>singletonList(name),
                         Option.<List<FunctionalRef>>none(),
                         Option.<Type>none());
    }

    public static FnRef makeFnRef(Span span, boolean paren,
                                  Id original_fn, List<IdOrOp> fns,
                                  List<StaticArg> sargs) {
        return makeFnRef(span, paren, Option.<Type>none(), sargs,
                         lexicalDepth, original_fn, fns,
                         Option.<List<FunctionalRef>>none(),
                         Option.<Type>none());
    }

    public static FnRef makeFnRef(Id name, Id orig){
        return makeFnRef(name.getSpan(), orig);
    }

    public static FnRef makeFnRef(Id orig, List<IdOrOp> names){
        return makeFnRef(orig.getSpan(), false, Option.<Type>none(),
                         Collections.<StaticArg>emptyList(),
                         lexicalDepth, orig, names,
                         Option.<List<FunctionalRef>>none(),
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
        return makeFnRef(original.getSpan(), original.isParenthesized(),
                         original.getExprType(), original.getStaticArgs(),
                         lexicalNestedness,
                         original.getOriginalName(), original.getNames(),
                         original.getOverloadings(), original.getOverloadingType());
    }

    public static FnRef makeFnRef(FnRef that, Option<Type> ty, Id name,
                                  List<IdOrOp> ids, List<StaticArg> sargs,
                                  Option<List<FunctionalRef>> overloadings) {
        return makeFnRef(that.getSpan(), that.isParenthesized(), ty,
                         sargs, lexicalDepth, name, ids,
                         overloadings, Option.<Type>none());
    }

    public static FnRef makeFnRef(FnRef that, Option<Type> ty, Id name,
                                  List<IdOrOp> ids) {
        return makeFnRef(that, ty, name, ids,
                         Collections.<StaticArg>emptyList(),
                         Option.<List<FunctionalRef>>none());
    }

    public static FnRef makeFnRef(Span span, boolean isParenthesized,
                                  Option<Type> type,
                                  List<StaticArg> staticArgs,
                                  int lexicalDepth,
                                  IdOrOp name, List<IdOrOp> names,
                                  Option<List<FunctionalRef>> overloadings,
                                  Option<Type> overloadingType) {
        return new FnRef(span, isParenthesized, type, staticArgs,
                         lexicalDepth, name, names, overloadings, overloadingType);
    }

    public static FunctionalRef make_RewriteOpRefOverloading(Span span,
                                                             FunctionalRef original,
                                                             Type type) {
        return makeOpRef(span, original.isParenthesized(), original.getExprType(),
                         original.getStaticArgs(), original.getLexicalDepth(),
                         original.getOriginalName(), original.getNames(),
                         original.getOverloadings(), Option.<Type>some(type));
    }

    public static FunctionalRef makeOpRef(Span span, String name) {
        return makeOpRef(NodeFactory.makeOpInfix(span, name));
    }

    public static FunctionalRef makeOpRef(Op op) {
        return makeOpRef(op, Collections.<StaticArg>emptyList());
    }

    public static FunctionalRef makeOpRef(Op op, List<StaticArg> staticArgs) {
        return makeOpRef(op.getSpan(), false, Option.<Type>none(), staticArgs,
                         lexicalDepth, op, Collections.<IdOrOp>singletonList(op),
                         Option.<List<FunctionalRef>>none(),
                         Option.<Type>none());
    }

    public static Expr makeOpRef(FunctionalRef original, int lexicalNestedness) {
        return makeOpRef(original.getSpan(), original.isParenthesized(),
                         original.getExprType(), original.getStaticArgs(),
                         lexicalNestedness, original.getOriginalName(),
                         original.getNames(), original.getOverloadings(),
                         original.getOverloadingType());
    }

    public static FunctionalRef makeOpRef(Span span, boolean isParenthesized,
                                          Option<Type> type,
                                          List<StaticArg> staticArgs,
                                          int lexicalDepth,
                                          IdOrOp name, List<IdOrOp> names,
                                          Option<List<FunctionalRef>> overloadings,
                                          Option<Type> overloadingType) {
        return new OpRef(span, isParenthesized, type, staticArgs,
                         lexicalDepth, name, names, overloadings, overloadingType);
    }

    public static Expr make_RewriteObjectRef(boolean parenthesized, Id in_obj,
                                             List<StaticArg> static_args) {
    	return makeVarRef(in_obj.getSpan(), parenthesized, Option.<Type>none(),
                          in_obj, static_args, lexicalDepth);
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
                          lexicalDepth);
    }

    public static VarRef makeVarRef(Span span, Iterable<Id> apiIds, Id name) {
        return makeVarRef(span, NodeFactory.makeId(span, apiIds, name));
    }

    public static VarRef makeVarRef(String s) {
        return makeVarRef(NodeFactory.makeId(s));
    }

    public static VarRef makeVarRef(String api_s, String local_s) {
        return makeVarRef(NodeFactory.makeId(api_s, local_s));
    }

    public static VarRef makeVarRef(Id id) {
        return makeVarRef(id.getSpan(), id);
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
        return makeVarRef(old.getSpan(), old.isParenthesized(), old.getExprType(),
                          old.getVarId(), old.getStaticArgs(), depth);
    }

    public static VarRef makeVarRef(VarRef var, Option<Type> type, Id name) {
        return makeVarRef(var.getSpan(), var.isParenthesized(), type, name);
    }

    public static VarRef makeVarRef(Span span, boolean isParenthesized,
                                    Option<Type> exprType, Id varId) {
        return makeVarRef(span, isParenthesized, exprType, varId,
                          Collections.<StaticArg>emptyList(), lexicalDepth);
    }

    public static VarRef makeVarRef(Span span, boolean isParenthesized,
                                    Option<Type> exprType, Id varId,
                                    List<StaticArg> staticArgs, int lexicalDepth) {
        return new VarRef(span, isParenthesized, exprType, varId, staticArgs,
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
                                                    Option<Type> exprType,
                                                    String text,
                                                    BigInteger intVal) {
        return new IntLiteralExpr(span, parenthesized, exprType, text, intVal);
    }

    public static CharLiteralExpr makeCharLiteralExpr(Span span, String s) {
        return makeCharLiteralExpr(span, false, Option.<Type>none(),
                                   s, s.charAt(0));
    }

    public static CharLiteralExpr makeCharLiteralExpr(Span span,
                                                      boolean parenthesized,
                                                      Option<Type> exprType,
                                                      String text,
                                                      int charVal) {
        return new CharLiteralExpr(span, parenthesized, exprType, text, charVal);
    }

    public static VoidLiteralExpr makeVoidLiteralExpr(Span span) {
        return makeVoidLiteralExpr(span, false, Option.<Type>none(), "");
    }

    public static VoidLiteralExpr makeVoidLiteralExpr(Span span,
                                                      boolean parenthesized,
                                                      Option<Type> exprType,
                                                      String text) {
        return new VoidLiteralExpr(span, parenthesized, exprType, text);
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
        Op op = NodeFactory.makeEnclosing(span, open, close);
        List<Expr> es;
        if (args == null) es = FortressUtil.emptyExprs();
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
                                                  Option<Type> exprType,
                                                  Expr obj, List<Expr> subs,
                                                  Option<Op> op,
                                                  List<StaticArg> staticArgs) {
        return new SubscriptExpr(span, parenthesized, exprType, obj, subs, op,
                                 staticArgs);
    }

    public static LocalVarDecl makeLocalVarDecl(Id p, Expr _r, Expr _body_expr) {
        List<Expr> _body = new ArrayList<Expr>(1);
        List<LValue> _lhs = new ArrayList<LValue>(1);
        Option<Expr> _rhs = Option.some(_r);
        _body.add(_body_expr);
        _lhs.add(NodeFactory.makeLValue(p.getSpan(), p));
        return makeLocalVarDecl(FortressUtil.spanTwo(p, _r), _body, _lhs, _rhs);
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
                                                Option<Type> exprType,
                                                List<Expr> body,
                                                List<LValue> lhs,
                                                Option<Expr> rhs) {
        return new LocalVarDecl(span, parenthesized, exprType, body, lhs, rhs);
    }

    public static FnExpr makeFnExpr(Span span,
                                    List<Param> params,
                                    Expr body) {
        return makeFnExpr(span, params, Option.<Type>none(),
                          Option.<List<BaseType>>none(), body);
    }

    public static FnExpr makeFnExpr(Span span, List<Param> params,
                                    Option<Type> returnType, Expr body) {
        return makeFnExpr(span, params, returnType,
                          Option.<List<BaseType>>none(), body);
    }

    public static FnExpr makeFnExpr(Span span, List<Param> params,
                                    Option<Type> returnType,
                                    Option<List<BaseType>> throwsClause,
                                    Expr body) {
        return makeFnExpr(span, false, Option.<Type>none(),
                          new AnonymousFnName(span),
                          Collections.<StaticParam>emptyList(), params,
                          returnType, Option.<WhereClause>none(),
                          throwsClause, body);
    }

    public static FnExpr makeFnExpr(Span span,
                                    boolean parenthesized,
                                    Option<Type> exprType,
                                    IdOrOpOrAnonymousName name,
                                    List<StaticParam> staticParams,
                                    List<Param> params,
                                    Option<Type> returnType,
                                    Option<WhereClause> whereClause,
                                    Option<List<BaseType>> throwsClause,
                                    Expr body) {
        return new FnExpr(span, parenthesized, exprType, name, staticParams,
                          params, returnType, whereClause, throwsClause, body);
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
                                Option<Type> typeOp,
                                Option<Id> targetOp,
                                Option<Expr> retExpr) {
        return new Exit(span, parenthesized, typeOp, targetOp, retExpr);
    }

    public static ArrayComprehension makeArrayComprehension(Span span,
                                                            List<StaticArg> staticArgs,
                                                            List<ArrayComprehensionClause> clauses) {
        return makeArrayComprehension(span, false, Option.<Type>none(),
                                      staticArgs, clauses);
    }

    public static ArrayComprehension makeArrayComprehension(Span span,
                                                            boolean parenthesized,
                                                            Option<Type> exprType,
                                                            List<StaticArg> staticArgs,
                                                            List<ArrayComprehensionClause> clauses) {
        return new ArrayComprehension(span, parenthesized, exprType, staticArgs, clauses);
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
                                              Option<Type> exprType,
                                              List<StaticArg> staticArgs,
                                              Op accOp,
                                              List<GeneratorClause> gens,
                                              Expr body) {
        return new Accumulator(span, parenthesized, exprType, staticArgs,
                               accOp, gens, body);
    }

    /**
     * For rewriting the id list, expr, of an existing typecase.
     * @param tc
     * @param lid
     * @param expr
     * @return
     */
    public static Typecase makeTypecase(Typecase tc, List<Id> lid, Expr expr) {
        return makeTypecase(tc.getSpan(), tc.isParenthesized(), tc.getExprType(),
                            lid, Option.wrap(expr), tc.getClauses(),
                            tc.getElseClause());
    }

    public static Typecase makeTypecase(Span span,
                                        boolean parenthesized,
                                        Option<Type> exprType,
                                        List<Id> bindIds,
                                        Option<Expr> bindExpr,
                                        List<TypecaseClause> clauses,
                                        Option<Block> elseClause) {
        return new Typecase(span, parenthesized, exprType, bindIds, bindExpr,
                            clauses, elseClause);
    }

    public static TupleExpr makeTupleExpr(List<Expr> exprs) {
        return makeTupleExpr(FortressUtil.spanAll(exprs), exprs);
    }

    public static TupleExpr makeTupleExpr(Span span, List<Expr> exprs) {
        return makeTupleExpr(span, false, Option.<Type>none(), exprs,
                             Option.<Expr>none(),
                             Collections.<KeywordExpr>emptyList(), false);
    }

    public static TupleExpr makeTupleExpr(Expr... exprs) {
        return makeTupleExpr(Arrays.asList(exprs));
    }

    public static TupleExpr makeTupleExpr(Span span,
                                          boolean parenthesized,
                                          Option<Type> exprType,
                                          List<Expr> exprs,
                                          Option<Expr> varargs,
                                          List<KeywordExpr> keywords,
                                          boolean inApp) {
        return new TupleExpr(span, parenthesized, exprType, exprs, varargs,
                             keywords, inApp);
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
                              Option<Type> exprType,
                              Block body,
                              Option<Catch> catchClause,
                              List<BaseType> forbidClause,
                              Option<Block> finallyClause) {
        return new Try(span, parenthesized, exprType, body, catchClause,
                       forbidClause, finallyClause);
    }

    public static ObjectExpr makeObjectExpr(Span span,
                                            List<TraitTypeWhere> extendsC,
                                            List<Decl> decls) {
        return makeObjectExpr(span, false, Option.<Type>none(),
                              extendsC, decls);
    }

    public static ObjectExpr makeObjectExpr(Span span,
                                            boolean parenthesized,
                                            Option<Type> exprType,
                                            List<TraitTypeWhere> extendsC,
                                            List<Decl> decls) {
        return new ObjectExpr(span, parenthesized, exprType, extendsC, decls);
    }

    public static _RewriteObjectExpr make_RewriteObjectExpr(ObjectExpr expr,
                                                            BATree<String, StaticParam> implicit_type_parameters) {
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
                staticArgs.add(NodeFactory.makeTypeArg(expr.getSpan(), s));
            }
        }
        return make_RewriteObjectExpr(expr.getSpan(), false, Option.<Type>none(),
                                      expr.getExtendsClause(), expr.getDecls(),
                                      implicit_type_parameters, WellKnownNames.objectExprName(expr),
                                      stParams, staticArgs,
                                      Option.some(Collections.<Param>emptyList()));
    }

    public static _RewriteObjectExpr make_RewriteObjectExpr(Span span,
                                                            boolean parenthesized,
                                                            Option<Type> exprType,
                                                            List<TraitTypeWhere> extendsC,
                                                            List<Decl> decls,
                                                            Map<String, StaticParam> implicitTypeParameters,
                                                            String genSymName,
                                                            List<StaticParam> staticParams,
                                                            List<StaticArg> staticArgs,
                                                            Option<List<Param>> params) {
        return new _RewriteObjectExpr(span, parenthesized, exprType, extendsC,
                                      decls, implicitTypeParameters, genSymName,
                                      staticParams, staticArgs, params);
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
                              Option.<List<FunctionalRef>>none());
    }

    public static Assignment makeAssignment(Span span,
                                            List<Lhs> lhs,
                                            Option<FunctionalRef> assignOp,
                                            Expr rhs,
                                            Option<List<FunctionalRef>> opsForLhs) {
        return makeAssignment(span, false, Option.<Type>none(), lhs, assignOp,
                              rhs, opsForLhs);
    }

    public static Assignment makeAssignment(Span span,
                                            boolean parenthesized,
                                            Option<Type> exprType,
                                            List<Lhs> lhs,
                                            Option<FunctionalRef> assignOp,
                                            Expr rhs,
                                            Option<List<FunctionalRef>> opsForLhs) {
        return new Assignment(span, parenthesized, exprType, lhs, assignOp,
                              rhs, opsForLhs);
    }

    public static Block makeBlock(Expr e) {
        List<Expr> b = Collections.singletonList(e);
        return makeBlock(e.getSpan(), e.getExprType(), b);
    }

    public static Block makeBlock(Span sp, Expr e) {
        List<Expr> b = Collections.singletonList(e);
        return makeBlock(sp, e.getExprType(), b);
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
                                  Option<Type> exprType,
                                  Option<Expr> loc,
                                  boolean atomicBlock,
                                  boolean withinDo,
                                  List<Expr> exprs) {
        return new Block(span, parenthesized, exprType, loc, atomicBlock,
                         withinDo, exprs);
    }

    public static If makeIf(Span span,
                            List<IfClause> clauses,
                            Option<Block> elseClause) {
        return makeIf(span, false, Option.<Type>none(), clauses, elseClause);
    }

    public static If makeIf(IfClause _if, Expr _else) {
        List<IfClause> ifclauses = Collections.singletonList(_if);
        List<Expr> elseBlock = Collections.singletonList(_else);
        Block _elseClause = makeBlock(_else.getSpan(), elseBlock);
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
        return makeIf(_if.getSpan(), ifclauses, Option.<Block>none());
    }

    public static If makeIf(Span sp, IfClause _if) {
        List<IfClause> ifclauses = Collections.singletonList(_if);
        return makeIf(sp, ifclauses, Option.<Block>none());
    }

    public static If makeIf(Span sp, Expr cond, Block _then, Block _else) {
        return makeIf(sp,
                      new IfClause(NodeFactory.makeSpan(cond, _else),
                                   makeGeneratorClause(cond.getSpan(), cond),
                                   _then),
                      _else);
    }

    public static If makeIf(Span sp, Expr cond, Block _then) {
        return makeIf(sp,
                      new IfClause(NodeFactory.makeSpan(cond, _then),
                                   makeGeneratorClause(cond.getSpan(), cond),
                                   _then));
    }

    public static If makeIf(Span span,
                            boolean parenthesized,
                            Option<Type> exprType,
                            List<IfClause> clauses,
                            Option<Block> elseClause) {
        return new If(span, parenthesized, exprType,
                      clauses, elseClause);
    }

    public static CaseExpr makeCaseExpr(Span span,
                                        Option<Expr> param,
                                        Option<FunctionalRef> compare,
                                        List<CaseClause> clauses,
                                        Option<Block> elseClause) {
        return makeCaseExpr(span, false, Option.<Type>none(), param, compare,
                            ExprFactory.makeInfixEq(), ExprFactory.makeInfixIn(),
                            clauses, elseClause);
    }

    public static CaseExpr makeCaseExpr(Span span,
                                        boolean parenthesized,
                                        Option<Type> exprType,
                                        Option<Expr> param,
                                        Option<FunctionalRef> compare,
                                        FunctionalRef equalsOp,
                                        FunctionalRef inOp,
                                        List<CaseClause> clauses,
                                        Option<Block> elseClause) {
        return new CaseExpr(span, parenthesized, exprType, param, compare,
                            equalsOp, inOp, clauses, elseClause);
    }

    public static ChainExpr makeChainExpr(Expr e, Op _op, Expr _expr) {
        List<Link> links = new ArrayList<Link>(1);
        Link link = new Link(NodeFactory.makeSpan(_op, _expr), makeOpRef(NodeFactory.makeOpInfix(_op)), _expr);
        links.add(link);
        return makeChainExpr(NodeFactory.makeSpan(e, _expr), e, links);
    }

    public static ChainExpr makeChainExpr(Span sp, Expr e, Op _op, Expr _expr) {
        List<Link> links = new ArrayList<Link>(1);
        Link link = new Link(NodeFactory.makeSpan(_op, _expr),
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
                                          Option<Type> exprType,
                                          Expr first, List<Link> links) {
        return new ChainExpr(span, parenthesized, exprType, first, links);
    }

    public static _RewriteFnApp make_RewriteFnApp(Expr e_1, Expr e_2) {
        return make_RewriteFnApp(FortressUtil.spanTwo(e_1, e_2), false,
                                 Option.<Type>none(), e_1, e_2);
    }

    public static _RewriteFnApp make_RewriteFnApp(Span span,
                                                  boolean parenthesized,
                                                  Option<Type> exprType,
                                                  Expr function,
                                                  Expr argument) {
        return new _RewriteFnApp(span, parenthesized, exprType, function,
                                 argument);
    }

    public static FieldRef makeFieldRef(FieldRef expr, Span span) {
        return makeFieldRef(span, expr.isParenthesized(),
                            expr.getExprType(), expr.getObj(), expr.getField());
    }

    public static FieldRef makeFieldRef(FieldRef expr, Expr receiver, Id field) {
        return makeFieldRef(expr.getSpan(), expr.isParenthesized(),
                            expr.getExprType(), receiver, field);
    }

    public static FieldRef make_RewriteFieldRef(FieldRef expr,
                                                Expr receiver, Id field) {
        return makeFieldRef(expr.getSpan(), expr.isParenthesized(),
                            expr.getExprType(), receiver, field);
    }

    public static FieldRef makeFieldRef(Span span, Expr receiver, Id field) {
        return makeFieldRef(span, false, Option.<Type>none(), receiver, field);
    }

    public static FieldRef makeFieldRef(Expr receiver, Id field) {
        return makeFieldRef(FortressUtil.spanTwo(receiver, field), receiver, field);
    }

    public static FieldRef makeFieldRef(Span span,
                                        boolean parenthesized,
                                        Option<Type> exprType,
                                        Expr obj, Id field) {
        return new FieldRef(span, parenthesized, exprType, obj, field);
    }

    public static AtomicExpr makeAtomicExpr(Span span, Expr expr) {
        return makeAtomicExpr(span, false, Option.<Type>none(), expr);
    }

    public static AtomicExpr makeAtomicExpr(Span span,
                                            boolean parenthesized,
                                            Option<Type> exprType,
                                            Expr expr) {
        return new AtomicExpr(span, parenthesized, exprType, expr);
    }

    public static For makeFor(Span span,
                              List<GeneratorClause> gens,
                              Block body) {
        return makeFor(span, false, Option.<Type>none(), gens, body);
    }

    public static For makeFor(Span span,
                              boolean parenthesized,
                              Option<Type> exprType,
                              List<GeneratorClause> gens,
                              Block body) {
        return new For(span, parenthesized, exprType, gens, body);
    }

    public static Spawn makeSpawn(Span span,
                                  Expr body) {
        return makeSpawn(span, false, Option.<Type>none(), body);
    }

    public static Spawn makeSpawn(Span span,
                                  boolean parenthesized,
                                  Option<Type> exprType,
                                  Expr body) {
        return new Spawn(span, parenthesized, exprType, body);
    }


    public static TryAtomicExpr makeTryAtomicExpr(Span span,
                                                  Expr expr) {
        return makeTryAtomicExpr(span, false, Option.<Type>none(), expr);
    }

    public static TryAtomicExpr makeTryAtomicExpr(Span span,
                                                  boolean parenthesized,
                                                  Option<Type> exprType,
                                                  Expr expr) {
        return new TryAtomicExpr(span, parenthesized, exprType, expr);
    }

    public static LetFn makeLetFn(Span span,
                                  List<Expr> body,
                                  List<FnDecl> fns) {
        return makeLetFn(span, false, Option.<Type>none(),
                         body, fns);
    }

    public static LetFn makeLetFn(Span span,
                                  boolean parenthesized,
                                  Option<Type> exprType,
                                  List<Expr> body,
                                  List<FnDecl> fns) {
        return new LetFn(span, parenthesized, exprType, body, fns);
    }

    public static Throw makeThrow(Span sp, String st) {
        Id id = NodeFactory.makeId(sp, WellKnownNames.fortressLibrary, st);
        return makeThrow(sp, makeVarRef(sp, id));
    }

    public static Throw makeThrow(Span span,
                                  Expr expr) {
        return makeThrow(span, false, Option.<Type>none(), expr);
    }

    public static Throw makeThrow(Span span,
                                  boolean parenthesized,
                                  Option<Type> exprType,
                                  Expr expr) {
        return new Throw(span, parenthesized, exprType, expr);
    }

    public static StringLiteralExpr makeStringLiteralExpr(Span span, String s) {
        return makeStringLiteralExpr(span, false, Option.<Type>none(), s);
    }

    public static StringLiteralExpr makeStringLiteralExpr(Span span,
                                                          boolean parenthesized,
                                                          Option<Type> exprType,
                                                          String text) {
        return new StringLiteralExpr(span, parenthesized, exprType, text);
    }

    public static FloatLiteralExpr makeFloatLiteralExpr(Span span,
                                                        boolean parenthesized,
                                                        Option<Type> exprType,
                                                        String text,
                                                        BigInteger intPart,
                                                        BigInteger numerator,
                                                        int denomBase,
                                                        int denomPower) {
        return new FloatLiteralExpr(span, parenthesized, exprType, text,
                                    intPart, numerator, denomBase, denomPower);
    }

    public static AsExpr makeAsExpr(Span span,
                                    Expr expr, Type annType) {
        return makeAsExpr(span, false, Option.<Type>none(), expr, annType);
    }

    public static AsExpr makeAsExpr(Span span,
                                    boolean parenthesized,
                                    Option<Type> exprType,
                                    Expr expr, Type annType) {
        return new AsExpr(span, parenthesized, exprType, expr, annType);
    }

    public static AsIfExpr makeAsIfExpr(Span span,
                                        Expr expr, Type annType) {
        return makeAsIfExpr(span, false, Option.<Type>none(), expr, annType);
    }

    public static AsIfExpr makeAsIfExpr(Span span,
                                        boolean parenthesized,
                                        Option<Type> exprType,
                                        Expr expr, Type annType) {
        return new AsIfExpr(span, parenthesized, exprType, expr, annType);
    }

    public static While makeWhile(Span sp, Expr cond) {
        // Might not work; empty Do may be naughty.
        return makeWhile(sp, makeGeneratorClause(cond.getSpan(), cond),
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
                                  Option<Type> exprType,
                                  GeneratorClause testExpr,
                                  Do body) {
        return new While(span, parenthesized, exprType, testExpr, body);
    }

    public static Label makeLabel(Span span,
                                  Id name, Block body) {
        return makeLabel(span, false, Option.<Type>none(), name, body);
    }

    public static Label makeLabel(Span span,
                                  boolean parenthesized,
                                  Option<Type> exprType,
                                  Id name, Block body) {
        return new Label(span, parenthesized, exprType, name, body);
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
                            Option<Type> exprType,
                            List<Block> fronts) {
        return new Do(span, parenthesized, exprType, fronts);
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
                return ExprFactory.makeLetFn(expr.getSpan(), body, expr.getFns());
            }
            public LetExpr forLocalVarDecl(LocalVarDecl expr) {
                return makeLocalVarDecl(expr.getSpan(), false, Option.<Type>none(),
                                        body, expr.getLhs(), expr.getRhs());
            }
        });
    }

    public static GeneratorClause makeGeneratorClause(Span span,
            Iterable<Id> ids,
            Expr expr) {
        return new GeneratorClause(span, CollectUtil.makeList(ids), expr);
    }

    public static GeneratorClause makeGeneratorClause(Span span, Expr cond) {
        return new GeneratorClause(span, Collections.<Id>emptyList(), cond);
    }

    public static FunctionalRef makeMultiJuxt() {
        return makeOpRef(NodeFactory.makeOpMultifix(NodeFactory.makeOp("juxtaposition")));
    }

    public static FunctionalRef makeInfixJuxt() {
        return makeOpRef(NodeFactory.makeOpInfix(NodeFactory.makeOp("juxtaposition")));
    }

    public static FunctionalRef makeInfixEq(){
     return makeOpRef(NodeFactory.makeOpInfix(NodeFactory.makeOp("=")));
    }

    public static FunctionalRef makeInfixIn(){
     return makeOpRef(NodeFactory.makeOpInfix(NodeFactory.makeOp("IN")));
    }

    public static Expr makeReceiver(Iterable<Id> ids) {
        Expr expr = makeVarRef(IterUtil.first(ids));
        for (Id id : IterUtil.skipFirst(ids)) {
            expr = makeFieldRef(expr, id);
        }
        return expr;
    }

    public static Expr makeInParentheses(Expr expr) {
        return expr.accept(new NodeAbstractVisitor<Expr>() {
            @Override
            public Expr for_RewriteFnApp(_RewriteFnApp that) {
                return make_RewriteFnApp(that.getSpan(), true, that.getExprType(),
                                         that.getFunction(), that.getArgument());
            }
        public Expr forAsExpr(AsExpr e) {
            return makeAsExpr(e.getSpan(), true, e.getExprType(),
                              e.getExpr(), e.getAnnType());
        }
        public Expr forAsIfExpr(AsIfExpr e) {
            return makeAsIfExpr(e.getSpan(), true, e.getExprType(),
                                e.getExpr(), e.getAnnType());
        }

        public Expr forAssignment(Assignment e) {
            return makeAssignment(e.getSpan(), true, e.getExprType(),
                                  e.getLhs(), e.getAssignOp(), e.getRhs(),
                                  e.getOpsForLhs());
        }
        public Expr forBlock(Block e) {
            return makeBlock(e.getSpan(), true, e.getExprType(), e.getLoc(),
                             e.isAtomicBlock(), e.isWithinDo(), e.getExprs());
        }
        public Expr forCaseExpr(CaseExpr e) {
            return makeCaseExpr(e.getSpan(), true, e.getExprType() , e.getParam(),
                                e.getCompare(), e.getEqualsOp(), e.getInOp(),
                                e.getClauses(), e.getElseClause());
        }
        public Expr forDo(Do e) {
            return makeDo(e.getSpan(), true, e.getExprType(), e.getFronts());
        }
        public Expr forFor(For e) {
            return makeFor(e.getSpan(), true, e.getExprType(), e.getGens(),
                           e.getBody());
        }
        public Expr forIf(If e) {
            return makeIf(e.getSpan(), true, e.getExprType(),
                          e.getClauses(), e.getElseClause());
        }
        public Expr forLabel(Label e) {
            return makeLabel(e.getSpan(), true, e.getExprType(),
                             e.getName(), e.getBody());
        }
        public Expr forMathPrimary(MathPrimary e) {
            return makeMathPrimary(e.getSpan(), true, e.getExprType(),
                                   e.getMultiJuxt(), e.getInfixJuxt(),
                                   e.getFront(), e.getRest());
        }
        public Expr forObjectExpr(ObjectExpr e) {
            return makeObjectExpr(e.getSpan(), true, e.getExprType(),
                                  e.getExtendsClause(), e.getDecls());
        }
        public Expr for_RewriteObjectExpr(_RewriteObjectExpr e) {
            return new _RewriteObjectExpr(e.getSpan(), true,
                                          e.getExprType(),
                    e.getExtendsClause(), e.getDecls(),
                    e.getImplicitTypeParameters(),
                    e.getGenSymName(),
                    e.getStaticParams(),
                    e.getStaticArgs(), e.getParams());
        }
        public Expr forTry(Try e) {
            return makeTry(e.getSpan(), true, e.getExprType(), e.getBody(),
                           e.getCatchClause(), e.getForbidClause(),
                           e.getFinallyClause());
        }
        public Expr forTupleExpr(TupleExpr e) {
            return makeTupleExpr(e.getSpan(), true, e.getExprType(),
                                 e.getExprs(), e.getVarargs(), e.getKeywords(),
                                 e.isInApp());
        }
        public Expr forTypecase(Typecase e) {
            return makeTypecase(e.getSpan(), true, e.getExprType(), e.getBindIds(),
                                e.getBindExpr(), e.getClauses(),
                                e.getElseClause());
        }
        public Expr forWhile(While e) {
            return makeWhile(e.getSpan(), true, e.getExprType(),
                             e.getTestExpr(), e.getBody());
        }
        public Expr forAccumulator(Accumulator e) {
            return makeAccumulator(e.getSpan(), true, e.getExprType(),
                                   e.getStaticArgs(),
                                   e.getAccOp(), e.getGens(), e.getBody());
        }
        public Expr forAtomicExpr(AtomicExpr e) {
            return makeAtomicExpr(e.getSpan(), true, e.getExprType(), e.getExpr());
        }
        public Expr forExit(Exit e) {
            return makeExit(e.getSpan(), true, e.getExprType(),
                            e.getTarget(), e.getReturnExpr());
        }

        public Expr forSpawn(Spawn e) {
            return makeSpawn(e.getSpan(), true, e.getExprType(), e.getBody());
        }
        public Expr forThrow(Throw e) {
            return makeThrow(e.getSpan(), true, e.getExprType(), e.getExpr());
        }
        public Expr forTryAtomicExpr(TryAtomicExpr e) {
            return makeTryAtomicExpr(e.getSpan(), true, e.getExprType(),
                                     e.getExpr());
        }
        public Expr forFnExpr(FnExpr e) {
            return makeFnExpr(e.getSpan(), true, e.getExprType(), e.getName(),
                              e.getStaticParams(), e.getParams(),
                              e.getReturnType(), e.getWhereClause(),
                              e.getThrowsClause(), e.getBody());
        }
        public Expr forLetFn(LetFn e) {
            return makeLetFn(e.getSpan(), true, e.getExprType(), e.getBody(), e.getFns());
        }
        public Expr forLocalVarDecl(LocalVarDecl e) {
            return makeLocalVarDecl(e.getSpan(), true, e.getExprType(),
                                    e.getBody(), e.getLhs(), e.getRhs());
        }
        public Expr forOpExpr(OpExpr e) {
            return makeOpExpr(e.getSpan(), true, e.getExprType(), e.getOp(), e.getArgs());
        }
        @Override
            public Expr forAmbiguousMultifixOpExpr(AmbiguousMultifixOpExpr that) {
            return new AmbiguousMultifixOpExpr(that.getSpan(), true, that.getExprType(),
                                               that.getInfix_op(), that.getMultifix_op(),
                                               that.getArgs());
        }
        public Expr forArrayElement(ArrayElement e) {
            return makeArrayElement(e.getSpan(), true, e.getExprType(),
                                    e.getStaticArgs(), e.getElement());
        }
        public Expr forArrayElements(ArrayElements e) {
            return makeArrayElements(e.getSpan(), true, e.getExprType(),
                                     e.getStaticArgs(), e.getDimension(),
                                     e.getElements(), e.isOutermost());
        }
        public Expr forFloatLiteralExpr(FloatLiteralExpr e) {
            return makeFloatLiteralExpr(e.getSpan(), true, e.getExprType(),
                                        e.getText(),
                    e.getIntPart(), e.getNumerator(),
                    e.getDenomBase(), e.getDenomPower());
        }
        public Expr forIntLiteralExpr(IntLiteralExpr e) {
            return makeIntLiteralExpr(e.getSpan(), true, e.getExprType(),
                                      e.getText(), e.getIntVal());
        }
        public Expr forCharLiteralExpr(CharLiteralExpr e) {
            return makeCharLiteralExpr(e.getSpan(), true, e.getExprType(),
                                       e.getText(), e.getCharVal());
        }
        public Expr forStringLiteralExpr(StringLiteralExpr e) {
            return makeStringLiteralExpr(e.getSpan(), true, e.getExprType(),
                                         e.getText());
        }
        public Expr forVoidLiteralExpr(VoidLiteralExpr e) {
            return makeVoidLiteralExpr(e.getSpan(), true, e.getExprType(),
                                       e.getText());
        }
        public Expr forVarRef(VarRef e) {
            return makeVarRef(e.getSpan(), true, e.getExprType(), e.getVarId(),
                              e.getStaticArgs(), e.getLexicalDepth());
        }
        public Expr forArrayComprehension(ArrayComprehension e) {
            return makeArrayComprehension(e.getSpan(), true, e.getExprType(),
                                          e.getStaticArgs(), e.getClauses());
        }
        public Expr forChainExpr(ChainExpr e) {
            return makeChainExpr(e.getSpan(), true, e.getExprType(), e.getFirst(),
                                 e.getLinks());
        }
        public Expr forFieldRef(FieldRef e) {
            return makeFieldRef(e.getSpan(), true, e.getExprType(),
                                e.getObj(), e.getField());
        }
        public Expr forMethodInvocation(MethodInvocation e) {
            return makeMethodInvocation(e.getSpan(), true, e.getExprType(),
                                        e.getObj(), e.getMethod(),
                                        e.getStaticArgs(), e.getArg());
        }
        public Expr forJuxt(Juxt e) {
            return makeJuxt(e.getSpan(), true, e.getExprType(),
                            e.getMultiJuxt(), e.getInfixJuxt(),
                            e.getExprs(), e.isFnApp(), e.isTight());
        }
        public Expr forFnRef(FnRef e) {
            return makeFnRef(e.getSpan(), true, e.getExprType(), e.getStaticArgs(),
                             e.getLexicalDepth(), e.getOriginalName(), e.getNames(),
                             e.getOverloadings(), e.getOverloadingType());
        }
        public Expr forOpRef(OpRef e) {
            return makeOpRef(e.getSpan(), true, e.getExprType(),
                                         e.getStaticArgs(), e.getLexicalDepth(),
                                         e.getOriginalName(), e.getNames(),
                                         e.getOverloadings(), e.getOverloadingType());
        }
        public Expr forSubscriptExpr(SubscriptExpr e) {
            return makeSubscriptExpr(e.getSpan(), true, e.getExprType(),
                                     e.getObj(), e.getSubs(), e.getOp(),
                                     e.getStaticArgs());
        }
        public Expr forTemplateGapExpr(TemplateGapExpr e) {
            return new TemplateGapExpr(e.getSpan(), e.getGapId(), e.getTemplateParams());
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

     FunctionalRef new_op = makeOpRef(op.getSpan(), op.isParenthesized(),
                                      op.getExprType(), op.getStaticArgs(),
                                      op.getLexicalDepth(), new_original_name,
                                      new_ops, op.getOverloadings(),
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

     FunctionalRef new_op = makeOpRef(op.getSpan(), op.isParenthesized(),
                                      op.getExprType(), op.getStaticArgs(),
                                      op.getLexicalDepth(), new_original_name,
                                      new_ops, op.getOverloadings(),
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
        return new TemplateGapExpr(s, id, params);
    }

    public static TemplateGapSimpleExpr makeTemplateGapSimpleExpr(Span s, Id id, List<Id> params) {
        return new TemplateGapSimpleExpr(s, id, params);
    }

    public static TemplateGapPrimary makeTemplateGapPrimary(Span s, Id id, List<Id> params) {
        return new TemplateGapPrimary(s, id, params);
    }

    public static TemplateGapFnExpr makeTemplateGapFnExpr(Span s, Id id, List<Id> params) {
        Expr body = makeVarRef(id);
        return new TemplateGapFnExpr(s, id, params);
    }

    public static TemplateGapJuxt makeTemplateGapJuxt(Span s, Id id, List<Id> params) {
        return new TemplateGapJuxt(s, id, params);
    }

    public static TemplateGapName makeTemplateGapName(Span s, Id id, List<Id> params) {
        return new TemplateGapName(s, id, params);
    }

    public static TemplateGapId makeTemplateGapId(Span s, Id id, List<Id> params) {
        return new TemplateGapId(s, id, params);
    }

    public static TemplateGapLiteralExpr makeTemplateGapLiteralExpr(Span s, Id id, List<Id> params) {
        return new TemplateGapLiteralExpr(s, id, params);
    }

    public static TemplateGapNumberLiteralExpr makeTemplateGapNumberLiteralExpr(Span s, Id id, List<Id> params) {
        return new TemplateGapNumberLiteralExpr(s, id, params);
    }

    public static TemplateGapFloatLiteralExpr makeTemplateGapFloatLiteralExpr(Span s, Id id, List<Id> params) {
        return new TemplateGapFloatLiteralExpr(s, id, params);
    }

    public static TemplateGapIntLiteralExpr makeTemplateGapIntLiteralExpr(Span s, Id id, List<Id> params) {
        return new TemplateGapIntLiteralExpr(s, id, params);
    }

    public static TemplateGapCharLiteralExpr makeTemplateGapCharLiteralExpr(Span s, Id id, List<Id> params) {
        return new TemplateGapCharLiteralExpr(s, id, params);
    }

    public static TemplateGapStringLiteralExpr makeTemplateGapStringLiteralExpr(Span s, Id id, List<Id> params) {
        return new TemplateGapStringLiteralExpr(s, id, params);
    }

    public static TemplateGapVoidLiteralExpr makeTemplateGapVoidLiteralExpr(Span s, Id id, List<Id> params) {
        return new TemplateGapVoidLiteralExpr(s, id, params);
    }

    //Span in_span, boolean in_parenthesized, Id in_obj, List<StaticArg> in_staticArgs
	public static Expr make_RewriteObjectRef(boolean parenthesized, Id in_obj) {
		return make_RewriteObjectRef(parenthesized, in_obj, Collections.<StaticArg>emptyList());
	}
}
