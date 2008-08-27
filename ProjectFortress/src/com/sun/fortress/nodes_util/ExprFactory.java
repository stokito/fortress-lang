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
    /** Alternatively, you can invoke the CharLiteralExpr constructor without parenthesized or val */
    public static CharLiteralExpr makeCharLiteralExpr(Span span, String s) {
        return new CharLiteralExpr(span, false, s, s.charAt(0));
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
        return new FloatLiteralExpr(span, false, s,
                intPart, numerator, denomBase, denomPower);
    }

    /** Alternatively, you can invoke the FnExpr constructor with only these parameters */
    public static FnExpr makeFnExpr(Span span, List<Param> params, Expr body) {
        return makeFnExpr(span, params, Option.<Type>none(),
                Option.<List<BaseType>>none(), body);
    }

    public static FnExpr makeFnExpr(Span span, List<Param> params,
            Option<Type> returnType,
            Option<List<BaseType>> throwsClause,
            Expr body) {
        return new FnExpr(span, false, new AnonymousFnName(span),
                          Collections.<StaticParam>emptyList(), params,
                          returnType, Option.<WhereClause>none(),
                          throwsClause, body);
    }

    /** Alternatively, you can invoke the IntLiteralExpr constructor without parenthesized or text */
    public static IntLiteralExpr makeIntLiteralExpr(Span span, BigInteger val) {
        return new IntLiteralExpr(span, false, val.toString(), val);
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
        return new IntLiteralExpr(span, false, s, val);
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


    public static LetExpr makeLetExpr(final LetExpr let_expr, final List<Expr> body) {
        return let_expr.accept(new NodeAbstractVisitor<LetExpr>() {
            public LetExpr forLetFn(LetFn expr) {
                return new LetFn(expr.getSpan(),false, body, expr.getFns());
            }
            public LetExpr forLocalVarDecl(LocalVarDecl expr) {
                return new LocalVarDecl(expr.getSpan(), false, body,
                        expr.getLhs(), expr.getRhs());
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

    public static TupleExpr makeTuple(List<Expr> exprs) {
        return new TupleExpr(FortressUtil.spanAll(exprs), false, exprs);
    }

    public static TupleExpr makeTuple(Span span, List<Expr> exprs) {
        return new TupleExpr(span, false, exprs);
    }

    public static TupleExpr makeTuple(Expr... exprs) {
        return makeTuple(Arrays.asList(exprs));
    }

    public static OpRef makeMultiJuxt() {
        return makeOpRef(NodeFactory.makeOpMultifix(NodeFactory.makeOp("juxtaposition")));
    }

    public static OpRef makeInfixJuxt() {
        return makeOpRef(NodeFactory.makeOpInfix(NodeFactory.makeOp("juxtaposition")));
    }

    public static OpRef makeInfixEq(){
     return makeOpRef(NodeFactory.makeOpInfix(NodeFactory.makeOp("=")));
    }

    public static OpRef makeInfixIn(){
     return makeOpRef(NodeFactory.makeOpInfix(NodeFactory.makeOp("IN")));
    }

    public static OpRef makeOpRef(OpName op) {
        return new OpRef(op.getSpan(), op, Collections.singletonList(op));
    }

    public static OpRef makeOpRef(OpName op, List<StaticArg> staticArgs) {
        return new OpRef(op.getSpan(), op, Collections.singletonList(op), staticArgs);
    }

    public static OpExpr makeOpExpr(Span span, Option<Type> ty, OpRef op,
                                    Expr first, Expr second) {
        return new OpExpr(span, false, ty, op, Arrays.asList(first, second));
    }

    public static OpExpr makeOpExpr(Span span, OpName op) {
        return new OpExpr(span, false, makeOpRef(op));
    }

    public static OpExpr makeOpExpr(Span span, OpName op, Expr arg) {
        return new OpExpr(span, false, makeOpRef(op),
                Collections.singletonList(arg));
    }

    public static OpExpr makeOpExpr(Span span, OpName op, Expr first,
                                    Expr second) {
        return new OpExpr(span, false, makeOpRef(op),
                          Arrays.asList(first, second));
    }

    public static OpExpr makeOpExpr(Span span, OpName op, List<StaticArg> staticArgs) {
        return new OpExpr(span, false, makeOpRef(op, staticArgs));
    }

    public static OpExpr makeOpExpr(Span span, OpName op, Expr arg,
            List<StaticArg> staticArgs) {
        return new OpExpr(span, false, makeOpRef(op, staticArgs),
                Collections.singletonList(arg));
    }

    /**
     * Creates an OpExpr using the Spans from e_1 and e_2.
     */
    public static OpExpr makeOpExpr(OpRef op, Expr e_1, Expr e_2) {
     return new OpExpr(new Span(e_1.getSpan(), e_2.getSpan()), false, op, Useful.list(e_1, e_2));
    }

    public static OpExpr makeOpExpr(Expr e,OpRef op) {
     return new OpExpr(new Span(e.getSpan(),op.getSpan()),false,op,Useful.list(e));
    }

    public static FnRef makeFnRef(FnRef that, Option<Type> ty, Id name,
                                  List<Id> ids, List<StaticArg> sargs) {
        return new FnRef(that.getSpan(), that.isParenthesized(), ty, name, ids,
                         sargs);
    }

    public static FnRef makeFnRef(Span span, Id name, List<StaticArg> sargs) {
        List<Id> names = Collections.singletonList(name);
        return new FnRef(span, false, name, names, sargs);
    }

    public static FnRef makeFnRef(Id name) {
        List<Id> names =
            Collections.singletonList(name);
        return new FnRef(name.getSpan(), false, name, names, Collections.<StaticArg>emptyList());
    }

    public static FnRef makeFnRef(Id name, Id orig){
     return new FnRef(name.getSpan(),false, orig, Collections.singletonList(name),Collections.<StaticArg>emptyList());
    }

    public static FnRef makeFnRef(Id orig, List<Id> names){
     return new FnRef(orig.getSpan(),false, orig, names,Collections.<StaticArg>emptyList());
    }

    public static FnRef makeFnRef(Iterable<Id> apiIds, Id name) {
        Id qName = NodeFactory.makeId(apiIds, name);
        List<Id> qNames = Collections.singletonList(qName);
        return new FnRef(qName.getSpan(), false, qName, qNames,
                Collections.<StaticArg>emptyList());
    }

    public static FnRef makeFnRef(APIName api, Id name) {
        Id qName = NodeFactory.makeId(api, name);
        List<Id> qNames = Collections.singletonList(qName);
        return new FnRef(qName.getSpan(), false, qName, qNames,
                Collections.<StaticArg>emptyList());
    }

    public static FnRef makeFnRef(Span span, boolean paren, Id original_fn, List<Id> fns, List<StaticArg> sargs) {
     return new FnRef(span, paren, original_fn, fns, sargs);
    }

    /** Alternatively, you can invoke the SubscriptExpr constructor without parenthesized or op */
    public static SubscriptExpr makeSubscriptExpr(Span span, Expr obj,
            List<Expr> subs) {
        return new SubscriptExpr(span, false, obj, subs,
                Option.<Enclosing>none(),
                Collections.<StaticArg>emptyList());
    }

    public static SubscriptExpr makeSubscriptExpr(Span span, Expr obj,
            List<Expr> subs,
            Option<Enclosing> op,
            List<StaticArg> sargs) {
        return new SubscriptExpr(span, false, obj, subs, op, sargs);
    }

    public static SubscriptExpr makeSubscriptExpr(Span span, Expr obj,
            List<Expr> subs,
            Option<Enclosing> op) {
        return new SubscriptExpr(span, false, obj, subs, op,
                Collections.<StaticArg>emptyList());
    }

    public static TightJuxt makeTightJuxt(Span span, Expr first, Expr second) {
        return new TightJuxt(span, false, Useful.list(first, second));
    }

    public static TightJuxt makeTightJuxt(Span span, boolean isParenthesized, List<Expr> exprs) {
        return new TightJuxt(span, isParenthesized, exprs);
    }

    public static TightJuxt makeTightJuxt(Span span, List<Expr> exprs, Boolean isParenthesized, OpRef infixJuxt, OpRef multiJuxt){
     return new TightJuxt(span, isParenthesized, multiJuxt, infixJuxt ,exprs);
    }

    /**
     * Make a TightJuxt that is a copy of the given one in every way except
     * with new exprs.
     */
    public static TightJuxt makeTightJuxt(TightJuxt that, List<Expr> exprs) {
     return new TightJuxt(that.getSpan(), that.isParenthesized(),
                          that.getMultiJuxt(), that.getInfixJuxt(), exprs);
    }

    public static MethodInvocation makeMethodInvocation(Span span,
                                                        boolean isParenthesized,
                                                        Option<Type> type,
                                                        Expr obj,
                                                        Id field, Expr expr) {
        return new MethodInvocation(span, isParenthesized, type, obj,
                                    field, expr);
    }

    public static MethodInvocation makeMethodInvocation(FieldRef that, Expr obj,
                                                        Id field, Expr expr) {
        return new MethodInvocation(that.getSpan(), that.isParenthesized(), obj,
                                    field, expr);
    }

    public static MethodInvocation makeMethodInvocation(_RewriteFieldRef that,
                                                        Expr obj, Id field,
                                                        Expr expr) {
        return new MethodInvocation(that.getSpan(), that.isParenthesized(), obj,
                                    field, expr);
    }

    public static VarRef makeVarRef(VarRef var, Option<Type> type, Id name) {
        return new VarRef(var.getSpan(), var.isParenthesized(), type, name);
    }

    public static VarRef makeVarRef(Span span, Option<Type> type, Id name) {
        return new VarRef(span, false, type, name);
    }

    public static VarRef makeVarRef(Span span, String s) {
        return new VarRef(span, false, NodeFactory.makeId(span, s));
    }

    public static VarRef makeVarRef(Span span, String s, int lexical_depth) {
        return new VarRef(span, false, NodeFactory.makeId(span, s), lexical_depth);
    }

    public static VarRef makeVarRef(String s) {
        return makeVarRef(NodeFactory.makeId(s));
    }

    public static VarRef makeVarRef(String api_s, String local_s) {
        return makeVarRef(NodeFactory.makeId(api_s, local_s));
    }

    public static VarRef makeVarRef(Span sp, Id id) {
        return new VarRef(sp, false, id);
    }

    public static VarRef makeVarRef(Span sp, Id id, Option<Type> type) {
        return new VarRef(sp, false, type, id);
    }

    public static VarRef makeVarRef(Id id) {
        return new VarRef(id.getSpan(), false, id);
    }

    public static VarRef makeVarRef(Iterable<Id> apiIds, Id name) {
        Id qName = NodeFactory.makeId(apiIds, name);
        return new VarRef(qName.getSpan(), false, qName);
    }

    public static VarRef makeVarRef(Span span, Iterable<Id> apiIds, Id name) {
        Id qName = NodeFactory.makeId(span, apiIds, name);
        return new VarRef(span, false, qName);
    }

    /** Assumes {@code ids} is nonempty. */
    public static VarRef makeVarRef(Iterable<Id> ids) {
        Id qName = NodeFactory.makeId(ids);
        return new VarRef(qName.getSpan(), false, qName);
    }

    public static VarRef makeVarRef(APIName api, Id name) {
        Id qName = NodeFactory.makeId(api, name);
        return new VarRef(qName.getSpan(), false, qName);
    }

    public static FieldRef makeFieldRef(FieldRef expr, Span span) {
        return new FieldRef(span, expr.isParenthesized(),
                            expr.getExprType(), expr.getObj(), expr.getField());
    }

    public static FieldRef makeFieldRef(FieldRef expr, Expr receiver, Id field) {
        return new FieldRef(expr.getSpan(), expr.isParenthesized(),
                            expr.getExprType(), receiver, field);
    }

    public static _RewriteFieldRef make_RewriteFieldRef(_RewriteFieldRef expr,
                                                        Expr receiver, Name field) {
        return new _RewriteFieldRef(expr.getSpan(), expr.isParenthesized(),
                                    expr.getExprType(), receiver, field);
    }

    public static FieldRef makeFieldRef(Span span, Expr receiver, Id field) {
        return new FieldRef(span, receiver, field);
    }

    public static FieldRef makeFieldRef(Expr receiver, Id field) {
        return new FieldRef(FortressUtil.spanTwo(receiver, field), false,
                            receiver, field);
    }

    public static Expr makeReceiver(Iterable<Id> ids) {
        Expr expr = makeVarRef(IterUtil.first(ids));
        for (Id id : IterUtil.skipFirst(ids)) {
            expr = makeFieldRef(expr, id);
        }
        return expr;
    }

    /** Alternatively, you can invoke the VoidLiteralExpr constructor without parenthesized or text */
    public static VoidLiteralExpr makeVoidLiteralExpr(Span span) {
        return new VoidLiteralExpr(span, false, "");
    }

    public static If makeIf(IfClause _if, Expr _else) {
        List<IfClause> ifclauses = new ArrayList<IfClause>();
        ifclauses.add(_if);
        List<Expr> elseBlock = new ArrayList<Expr>();
        elseBlock.add(_else);
        Block _elseClause = new Block( elseBlock);
        return new If(ifclauses, Option.some(_elseClause));
    }

    public static If makeIf(Span sp, IfClause _if, Expr _else) {
        List<IfClause> ifclauses = new ArrayList<IfClause>();
        ifclauses.add(_if);
        List<Expr> elseBlock = new ArrayList<Expr>();
        elseBlock.add(_else);
        Block _elseClause = new Block(sp, elseBlock);
        return new If(sp, ifclauses, Option.some(_elseClause));
    }

    public static If makeIf(IfClause _if) {
        List<IfClause> ifclauses = new ArrayList<IfClause>();
        ifclauses.add(_if);
        return new If(ifclauses, Option.<Block>none());
    }

    public static If makeIf(Span sp, IfClause _if) {
        List<IfClause> ifclauses = new ArrayList<IfClause>();
        ifclauses.add(_if);
        return new If(sp, ifclauses, Option.<Block>none());
    }

    public static If makeIf(Span sp, Expr cond, Block _then, Block _else) {
        return
        makeIf(sp,
                new IfClause(
                        makeGeneratorClause(cond.getSpan(), cond),
                        _then), _else);
    }

    public static If makeIf(Span sp, Expr cond, Block _then) {
        return
        makeIf(sp,
                new IfClause(
                        makeGeneratorClause(cond.getSpan(), cond),
                        _then));
    }

    public static While makeWhile(Span sp, Expr cond) {
        // Might not work; empty Do may be naughty.
        return new While(sp, makeGeneratorClause(cond.getSpan(), cond),
                new Do(sp, Collections.<DoFront>emptyList()));
    }

    public static Block makeBlock(Expr e) {
        List<Expr> b = new ArrayList<Expr>(1);
        b.add(e);
        return new Block(e.getSpan(), e.getExprType(), b);
    }

    public static Block makeBlock(Span sp, Expr e) {
        List<Expr> b = new ArrayList<Expr>(1);
        b.add(e);
        return new Block(sp, e.getExprType(), b);
    }

    public static Do makeDo(Span sp, Option<Type> t, Expr e) {
        List<Expr> b = new ArrayList<Expr>(1);
        b.add(e);
        List<DoFront> body = new ArrayList<DoFront>();
        body.add(new DoFront(sp, new Block(sp, t, b)));
        return new Do(sp, t, body);
    }

    public static Do makeDo(Span sp, Option<Type> t, List<Expr> exprs) {
        List<DoFront> body = new ArrayList<DoFront>();
        body.add(new DoFront(sp, new Block(sp, t, exprs)));
        return new Do(sp, t, body);
    }

    public static LocalVarDecl makeLocalVarDecl(Id p, Expr _r, Expr _body_expr) {
        List<Expr> _body = new ArrayList<Expr>();
        List<LValue> _lhs = new ArrayList<LValue>();
        Option<Expr> _rhs = Option.some(_r);
        _body.add(_body_expr);
        _lhs.add(new LValueBind(p,false));
        return new LocalVarDecl(FortressUtil.spanTwo(p, _r), _body, _lhs, _rhs);
    }

    public static LocalVarDecl makeLocalVarDecl(Span sp, Id p, Expr _r, Expr _body_expr) {
        List<Expr> _body = new ArrayList<Expr>();
        List<LValue> _lhs = new ArrayList<LValue>();
        Option<Expr> _rhs = Option.some(_r);
        _body.add(_body_expr);
        _lhs.add(new LValueBind(sp, p,false));
        return new LocalVarDecl(sp, _body, _lhs, _rhs);
    }

    public static LocalVarDecl makeLocalVarDecl(Span sp, List<LValue> lhs, Expr _r, Expr _body_expr) {
        List<Expr> _body = new ArrayList<Expr>();
        Option<Expr> _rhs = Option.some(_r);
        _body.add(_body_expr);
        return new LocalVarDecl(sp, _body, lhs, _rhs);
    }

    public static LocalVarDecl makeLocalVarDecl(Span sp, List<LValue> lhs,
                                                Expr _r, List<Expr> _body) {
        Option<Expr> _rhs = Option.some(_r);
        return new LocalVarDecl(sp, _body, lhs, _rhs);
    }

    public static ChainExpr makeChainExpr(Expr e, Op _op, Expr _expr) {
        List<Link> links = new ArrayList<Link>();
        Link link = new Link(new Span(_op.getSpan(), _expr.getSpan()), makeOpRef(NodeFactory.makeOpInfix(_op)), _expr);
        links.add(link);
        return new ChainExpr(e, links);
    }

    public static ChainExpr makeChainExpr(Span sp, Expr e, Op _op, Expr _expr) {
     List<Link> links = new ArrayList<Link>();
        Link link = new Link(new Span(_op.getSpan(), _expr.getSpan()), makeOpRef(NodeFactory.makeOpInfix(_op)), _expr);
        links.add(link);
        return new ChainExpr(sp, e, links);
    }

    public static Throw makeThrow(String st) {
        return new Throw(makeVarRef(st));
    }

    /**
     * Throws a standard (FortressLibrary) exception
     * @param sp
     * @param st
     * @return
     */
    public static Throw makeThrow(Span sp, String st) {
        Id id = NodeFactory.makeId(sp, WellKnownNames.fortressLibrary, st);
        return new Throw(sp, makeVarRef(sp, id));
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
        return new _RewriteObjectExpr(expr.getSpan(), false,
                expr.getExtendsClause(), expr.getDecls(),
                implicit_type_parameters, WellKnownNames.objectExprName(expr),
                stParams, staticArgs,
                Option.some(Collections.<Param>emptyList()));
    }

    public static Assignment makeAssignment(Span span, Option<Type> type,
                                            List<Lhs> lhs, Option<OpRef> op,
                                            Expr rhs) {
        return new Assignment(span, false, type, lhs, op, rhs);
    }

    public static Assignment makeAssignment(Span span, Option<Type> type,
                                            List<Lhs> lhs, Expr rhs) {
        return new Assignment(span, false, type, lhs, Option.<OpRef>none(), rhs);
    }

    /**
     * Uses the Spans from e_1 and e_2.
     */
    public static _RewriteFnApp make_RewriteFnApp(Expr e_1, Expr e_2) {
     return new _RewriteFnApp(new Span(e_1.getSpan(), e_2.getSpan()), e_1, e_2);
    }

    public static Expr makeInParentheses(Expr expr) {
        return expr.accept(new NodeAbstractVisitor<Expr>() {
            @Override
            public Expr for_RewriteFnApp(_RewriteFnApp that) {
                return new _RewriteFnApp(that.getSpan(),true,that.getFunction(),that.getArgument());
            }
        @Override
            public Expr for_RewriteObjectRef(_RewriteObjectRef that) {
                return new _RewriteObjectRef(that.getSpan(),true,that.getObj(),that.getStaticArgs());
            }
        public Expr forAsExpr(AsExpr e) {
            return new AsExpr(e.getSpan(), true, e.getExpr(), e.getType());
        }
        public Expr forAsIfExpr(AsIfExpr e) {
            return new AsIfExpr(e.getSpan(), true, e.getExpr(), e.getType());
        }

        public Expr forAssignment(Assignment e) {
            return new Assignment(e.getSpan(), true, e.getLhs(), e.getOpr(),
                                  e.getRhs());
        }
        public Expr forBlock(Block e) {
            return new Block(e.getSpan(), true, e.getExprs());
        }
        public Expr forCaseExpr(CaseExpr e) {
            return new CaseExpr(e.getSpan(), true, e.getExprType() , e.getParam(),
                    e.getCompare(),e.getInOp() ,e.getClauses(),
                    e.getElseClause());
        }
        public Expr forDo(Do e) {
            return new Do(e.getSpan(), true, e.getFronts());
        }
        public Expr forFor(For e) {
            return new For(e.getSpan(), true, e.getGens(), e.getBody());
        }
        public Expr forIf(If e) {
            return new If(e.getSpan(), true, e.getClauses(),
                    e.getElseClause());
        }
        public Expr forLabel(Label e) {
            return new Label(e.getSpan(), true, e.getName(), e.getBody());
        }
        public Expr forMathPrimary(MathPrimary e) {
            return new MathPrimary(e.getSpan(), true, e.getFront(),
                    e.getRest());
        }
        public Expr forObjectExpr(ObjectExpr e) {
            return new ObjectExpr(e.getSpan(), true, e.getExtendsClause(),
                    e.getDecls());
        }
        public Expr for_RewriteObjectExpr(_RewriteObjectExpr e) {
            return new _RewriteObjectExpr(e.getSpan(), true,
                    e.getExtendsClause(), e.getDecls(),
                    e.getImplicitTypeParameters(),
                    e.getGenSymName(),
                    e.getStaticParams(),
                    e.getStaticArgs(), e.getParams());
        }
        public Expr forTry(Try e) {
            return new Try(e.getSpan(), true, e.getBody(),
                    e.getCatchClause(), e.getForbid(),
                    e.getFinallyClause());
        }
        public Expr forArgExpr(ArgExpr e) {
            return new ArgExpr(e.getSpan(), true, e.getExprs(), e.getVarargs());
        }
        public Expr forTupleExpr(TupleExpr e) {
            return new TupleExpr(e.getSpan(), true, e.getExprs());
        }
        public Expr forTypecase(Typecase e) {
            return new Typecase(e.getSpan(), true, e.getBindIds(),
                    e.getBindExpr(), e.getClauses(),
                    e.getElseClause());
        }
        public Expr forWhile(While e) {
            return new While(e.getSpan(), true, e.getTest(), e.getBody());
        }
        public Expr forAccumulator(Accumulator e) {
            return new Accumulator(e.getSpan(), true, e.getStaticArgs(),
                    e.getOpr(), e.getGens(), e.getBody());
        }
        public Expr forAtomicExpr(AtomicExpr e) {
            return new AtomicExpr(e.getSpan(), true, e.getExpr());
        }
        public Expr forExit(Exit e) {
            return new Exit(e.getSpan(), true, e.getExprType() ,e.getTarget(), e.getReturnExpr());
        }

        public Expr forSpawn(Spawn e) {
            return new Spawn(e.getSpan(), true, e.getBody());
        }
        public Expr forThrow(Throw e) {
            return new Throw(e.getSpan(), true, e.getExpr());
        }
        public Expr forTryAtomicExpr(TryAtomicExpr e) {
            return new TryAtomicExpr(e.getSpan(), true, e.getExpr());
        }
        public Expr forFnExpr(FnExpr e) {
            return new FnExpr(e.getSpan(), true, e.getName(),
                              e.getStaticParams(), e.getParams(),
                              e.getReturnType(), e.getWhere(),
                              e.getThrowsClause(), e.getBody());
        }
        public Expr forGeneratedExpr(GeneratedExpr e) {
            return new GeneratedExpr(e.getSpan(), true,
                    e.getExpr(), e.getGens());
        }
        public Expr forLetFn(LetFn e) {
            return new LetFn(e.getSpan(), true, e.getBody(), e.getFns());
        }
        public Expr forLocalVarDecl(LocalVarDecl e) {
            return new LocalVarDecl(e.getSpan(), true, e.getBody(),
                    e.getLhs(), e.getRhs());
        }
        public Expr forOpExpr(OpExpr e) {
            return new OpExpr(e.getSpan(), true, e.getOp(), e.getArgs());
        }
        @Override
		public Expr forAmbiguousMultifixOpExpr(AmbiguousMultifixOpExpr that) {
        	return new AmbiguousMultifixOpExpr(that.getSpan(), true,
        			                           that.getInfix_op(), that.getMultifix_op(),
        			                           that.getArgs());
		}
		public Expr forArrayElement(ArrayElement e) {
            return new ArrayElement(e.getSpan(), true, e.getElement());
        }
        public Expr forArrayElements(ArrayElements e) {
            return new ArrayElements(e.getSpan(), true, e.getDimension(),
                    e.getElements());
        }
        public Expr forFloatLiteralExpr(FloatLiteralExpr e) {
            return new FloatLiteralExpr(e.getSpan(), true, e.getText(),
                    e.getIntPart(), e.getNumerator(),
                    e.getDenomBase(), e.getDenomPower());
        }
        public Expr forIntLiteralExpr(IntLiteralExpr e) {
            return new IntLiteralExpr(e.getSpan(), true, e.getText(),
                    e.getVal());
        }
        public Expr forCharLiteralExpr(CharLiteralExpr e) {
            return new CharLiteralExpr(e.getSpan(), true, e.getText(),
                    e.getVal());
        }
        public Expr forStringLiteralExpr(StringLiteralExpr e) {
            return new StringLiteralExpr(e.getSpan(), true, e.getText());
        }
        public Expr forVoidLiteralExpr(VoidLiteralExpr e) {
            return new VoidLiteralExpr(e.getSpan(), true, e.getText());
        }
        public Expr forVarRef(VarRef e) {
            return new VarRef(e.getSpan(), true, e.getVar());
        }
        public Expr forArrayComprehension(ArrayComprehension e) {
            return new ArrayComprehension(e.getSpan(), true, e.getClauses());
        }
        public Expr forChainExpr(ChainExpr e) {
            return new ChainExpr(e.getSpan(), true, e.getFirst(),
                    e.getLinks());
        }
        public Expr forFieldRef(FieldRef e) {
            return new FieldRef(e.getSpan(), true, e.getObj(),
                    e.getField());
        }
        public Expr forMethodInvocation(MethodInvocation e) {
            return new MethodInvocation(e.getSpan(), true, e.getObj(),
                    e.getMethod(), e.getStaticArgs(),
                    e.getArg());
        }
        public Expr forLooseJuxt(LooseJuxt e) {
            return new LooseJuxt(e.getSpan(), true, e.getExprs());
        }
        public Expr forTightJuxt(TightJuxt e) {
            return new TightJuxt(e.getSpan(), true, e.getExprs());
        }
        public Expr forFnRef(FnRef e) {
            return new FnRef(e.getSpan(), true, e.getOriginalName(), e.getFns(),
                    e.getStaticArgs());
        }
        public Expr forOpRef(OpRef e) {
            return new OpRef(e.getSpan(), true, e.getOriginalName(), e.getOps(),
                    e.getStaticArgs());
        }
        public Expr forSubscriptExpr(SubscriptExpr e) {
            return new SubscriptExpr(e.getSpan(), true, e.getObj(),
                    e.getSubs(), e.getOp(),
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
    private static Expr makeInfixOpExpr(Span s, Expr lhs, OpRef op, Expr rhs) {
     List<OpName> new_ops = CollectUtil.makeList(IterUtil.map(op.getOps(), new Lambda<OpName,OpName>(){
   public Op value(OpName arg0) {
    if( arg0 instanceof Enclosing )
     return bug("Can't make an infix operator out of an enclosing operator.");
    else
     return NodeFactory.makeOpInfix((Op)arg0);
   }}));
     // We are remaking this because we think that the Interpreter expects originalName to be infix
     Op new_original_name;
     if( op.getOriginalName() instanceof Enclosing )
      return bug("Can't make an infix operator out of an enclosing operator.");
     else
      new_original_name = NodeFactory.makeOpInfix((Op)op.getOriginalName());

     OpRef new_op = new OpRef(op.getSpan(),op.isParenthesized(),op.getLexicalDepth(),new_original_name,new_ops,op.getStaticArgs());
     return ExprFactory.makeOpExpr(new_op, lhs, rhs);
    }

    private static Expr makePostfixOpExpr(Span s, Expr e, OpRef op) {
     List<OpName> new_ops = CollectUtil.makeList(IterUtil.map(op.getOps(), new Lambda<OpName,OpName>(){
   public Op value(OpName arg0) {
    if( arg0 instanceof Enclosing )
     return bug("Can't make an postfix operator out of an enclosing operator.");
    else
     return NodeFactory.makeOpPostfix((Op)arg0);
   }}));
     // We are remaking this because we think that the Interpreter expects originalName to be postfix
     Op new_original_name;
     if( op.getOriginalName() instanceof Enclosing )
      return bug("Can't make an postfix operator out of an enclosing operator.");
     else
      new_original_name = NodeFactory.makeOpPostfix((Op)op.getOriginalName());

     OpRef new_op = new OpRef(op.getSpan(),op.isParenthesized(),op.getLexicalDepth(),new_original_name,new_ops,op.getStaticArgs());
     return ExprFactory.makeOpExpr(e, new_op);
    }

    public static Expr simplifyMathPrimary(Span span, Expr front, MathItem mi) {
        if (mi instanceof ExprMI) {
            Expr expr = ((ExprMI)mi).getExpr();
            return new TightJuxt(span, Useful.list(front, expr));
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

    /**
     * For rewriting the id list, expr, of an existing typecase.
     * @param tc
     * @param lid
     * @param expr
     * @return
     */
    public static Typecase makeTypecase(Typecase tc, List<Id> lid, Expr expr) {
        /* Span in_span,
         * boolean in_parenthesized,
         * Pair<List<Id>, Option<Expr>> in_bind,
         * List<TypecaseClause> in_clauses,
         * Option<Block> in_elseClause
         */
        return new Typecase(tc.getSpan(), tc.isParenthesized(), lid,
                            Option.wrap(expr), tc.getClauses(),
                            tc.getElseClause());
    }

    public static TemplateGapExpr makeTemplateGapExpr(Span s, Id id, List<Id> params) {
        return new TemplateGapExpr(s, id, params);
    }

    public static TemplateGapDelimitedExpr makeTemplateGapDelimitedExpr(Span s, Id id, List<Id> params) {
        return new TemplateGapDelimitedExpr(s, id, params);
    }

    public static TemplateGapSimpleExpr makeTemplateGapSimpleExpr(Span s, Id id, List<Id> params) {
        return new TemplateGapSimpleExpr(s, id, params);
    }

    public static TemplateGapPrimary makeTemplateGapPrimary(Span s, Id id, List<Id> params) {
        return new TemplateGapPrimary(s, id, params);
    }

    public static TemplateGapFnExpr makeTemplateGapFnExpr(Span s, Id id, List<Id> params) {
        Expr body = new VarRef(id);
        return new TemplateGapFnExpr(s, id, params);
    }

    public static TemplateGapLooseJuxt makeTemplateGapLooseJuxt(Span s, Id id, List<Id> params) {
        return new TemplateGapLooseJuxt(s, id, params);
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

    public static Expr make_RewriteObjectRef(boolean parenthesized, Id in_obj, List<StaticArg> static_args) {
    	return new _RewriteObjectRef(in_obj.getSpan(), parenthesized, in_obj, static_args);
    }

    //Span in_span, boolean in_parenthesized, Id in_obj, List<StaticArg> in_staticArgs
	public static Expr make_RewriteObjectRef(boolean parenthesized, Id in_obj) {
		return make_RewriteObjectRef(parenthesized, in_obj, Collections.<StaticArg>emptyList());
	}
}
