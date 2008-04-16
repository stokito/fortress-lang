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

import static com.sun.fortress.interpreter.evaluator.InterpreterBug.bug;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Accumulator;
import com.sun.fortress.nodes.AnonymousFnName;
import com.sun.fortress.nodes.ArgExpr;
import com.sun.fortress.nodes.ArrayComprehension;
import com.sun.fortress.nodes.ArrayElement;
import com.sun.fortress.nodes.ArrayElements;
import com.sun.fortress.nodes.AsExpr;
import com.sun.fortress.nodes.AsIfExpr;
import com.sun.fortress.nodes.Assignment;
import com.sun.fortress.nodes.AtomicExpr;
import com.sun.fortress.nodes.Block;
import com.sun.fortress.nodes.CaseExpr;
import com.sun.fortress.nodes.ChainExpr;
import com.sun.fortress.nodes.CharLiteralExpr;
import com.sun.fortress.nodes.Do;
import com.sun.fortress.nodes.Enclosing;
import com.sun.fortress.nodes.Exit;
import com.sun.fortress.nodes.ExponentiationMI;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.ExprMI;
import com.sun.fortress.nodes.FieldRef;
import com.sun.fortress.nodes.FloatLiteralExpr;
import com.sun.fortress.nodes.FnExpr;
import com.sun.fortress.nodes.FnRef;
import com.sun.fortress.nodes.For;
import com.sun.fortress.nodes.GeneratedExpr;
import com.sun.fortress.nodes.GeneratorClause;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.If;
import com.sun.fortress.nodes.IfClause;
import com.sun.fortress.nodes.IntLiteralExpr;
import com.sun.fortress.nodes.LValue;
import com.sun.fortress.nodes.LValueBind;
import com.sun.fortress.nodes.Label;
import com.sun.fortress.nodes.LetExpr;
import com.sun.fortress.nodes.LetFn;
import com.sun.fortress.nodes.LocalVarDecl;
import com.sun.fortress.nodes.LooseJuxt;
import com.sun.fortress.nodes.MathItem;
import com.sun.fortress.nodes.MathPrimary;
import com.sun.fortress.nodes.MethodInvocation;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeAbstractVisitor;
import com.sun.fortress.nodes.ObjectExpr;
import com.sun.fortress.nodes.Op;
import com.sun.fortress.nodes.OpName;
import com.sun.fortress.nodes.OpRef;
import com.sun.fortress.nodes.OprExpr;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.nodes.QualifiedOpName;
import com.sun.fortress.nodes.Spawn;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.StringLiteralExpr;
import com.sun.fortress.nodes.SubscriptExpr;
import com.sun.fortress.nodes.SubscriptingMI;
import com.sun.fortress.nodes.Throw;
import com.sun.fortress.nodes.TightJuxt;
import com.sun.fortress.nodes.TraitType;
import com.sun.fortress.nodes.Try;
import com.sun.fortress.nodes.TryAtomicExpr;
import com.sun.fortress.nodes.TupleExpr;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.Typecase;
import com.sun.fortress.nodes.VarRef;
import com.sun.fortress.nodes.VoidLiteralExpr;
import com.sun.fortress.nodes.While;
import com.sun.fortress.nodes._RewriteObjectExpr;
import com.sun.fortress.parser_util.FortressUtil;
import com.sun.fortress.parser_util.precedence_resolver.ASTUtil;

import com.sun.fortress.useful.BATree;
import com.sun.fortress.useful.Pair;
import com.sun.fortress.useful.Useful;

import edu.rice.cs.plt.iter.IterUtil;
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
                          Option.<List<TraitType>>none(), body);
    }

    public static FnExpr makeFnExpr(Span span, List<Param> params,
                                    Option<Type> returnType,
                                    Option<List<TraitType>> throwsClause,
                                    Expr body) {
        return new FnExpr(span, false, new AnonymousFnName(span),
                          Collections.<StaticParam>emptyList(), params,
                          returnType, FortressUtil.emptyWhereClause(),
                          throwsClause, body);
    }

    /** Alternatively, you can invoke the IntLiteralExpr constructor without parenthesized or text */
    public static IntLiteralExpr makeIntLiteralExpr(Span span, BigInteger val) {
        return new IntLiteralExpr(span, false, val.toString(), val);
    }

    public static IntLiteralExpr makeIntLiteralExpr(Span span, String s) {
        BigInteger val;
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
        return new GeneratorClause(span, IterUtil.asList(ids), expr);
    }

    public static TupleExpr makeTuple(List<Expr> exprs) {
        return new TupleExpr(new Span(), false, exprs);
    }

    public static TupleExpr makeTuple(Span span, List<Expr> exprs) {
        return new TupleExpr(span, false, exprs);
    }

    public static TupleExpr makeTuple(Expr... exprs) {
        return makeTuple(Arrays.asList(exprs));
    }

    private static OpRef makeOpRef(QualifiedOpName op) {
        return new OpRef(op.getSpan(), Collections.singletonList(op));
    }

    private static OpRef makeOpRef(QualifiedOpName op, List<StaticArg> staticArgs) {
        return new OpRef(op.getSpan(), Collections.singletonList(op), staticArgs);
    }

    public static OprExpr makeOprExpr(Span span, QualifiedOpName op) {
        return new OprExpr(span, false, makeOpRef(op));
    }

    public static OprExpr makeOprExpr(Span span, QualifiedOpName op, Expr arg) {
        return new OprExpr(span, false, makeOpRef(op),
                           Collections.singletonList(arg));
    }

    public static OprExpr makeOprExpr(Span span, QualifiedOpName op, Expr first,
                                      Expr second) {
        return new OprExpr(span, false, makeOpRef(op),
                           Arrays.asList(first, second));
    }

    public static OprExpr makeOprExpr(Span span, OpName op) {
        QualifiedOpName name = new QualifiedOpName(span, Option.<APIName>none(), op);
        return new OprExpr(span, false, makeOpRef(name),
                           Collections.<Expr>emptyList());
    }

    public static OprExpr makeOprExpr(Span span, OpName op, Expr arg) {
        QualifiedOpName name = new QualifiedOpName(span, Option.<APIName>none(), op);
        return new OprExpr(span, false, makeOpRef(name),
                           Collections.singletonList(arg));
    }

    public static OprExpr makeOprExpr(Span span, OpName op, Expr first,
                                      Expr second) {
        QualifiedOpName name = new QualifiedOpName(span, Option.<APIName>none(), op);
        return new OprExpr(span, false, makeOpRef(name),
                           Arrays.asList(first, second));
    }

    public static OprExpr makeOprExpr(Span span, OpName op, Expr arg,
                                      List<StaticArg> staticArgs) {
        QualifiedOpName name = new QualifiedOpName(span, Option.<APIName>none(), op);
        return new OprExpr(span, false, makeOpRef(name, staticArgs),
                           Collections.singletonList(arg));
    }

    public static FnRef makeFnRef(Span span, QualifiedIdName name, List<StaticArg> sargs) {
        List<QualifiedIdName> names = Collections.singletonList(name);
        return new FnRef(span, false, names, sargs);
    }

    public static FnRef makeFnRef(Id name) {
        List<QualifiedIdName> names =
            Collections.singletonList(NodeFactory.makeQualifiedIdName(name));
        return new FnRef(name.getSpan(), false, names, Collections.<StaticArg>emptyList());
    }

    public static FnRef makeFnRef(Iterable<Id> apiIds, Id name) {
        QualifiedIdName qName = NodeFactory.makeQualifiedIdName(apiIds, name);
        List<QualifiedIdName> qNames = Collections.singletonList(qName);
        return new FnRef(qName.getSpan(), false, qNames,
                         Collections.<StaticArg>emptyList());
    }

    public static FnRef makeFnRef(APIName api, Id name) {
        QualifiedIdName qName = NodeFactory.makeQualifiedIdName(api, name);
        List<QualifiedIdName> qNames = Collections.singletonList(qName);
        return new FnRef(qName.getSpan(), false, qNames,
                         Collections.<StaticArg>emptyList());
    }

    public static FnRef makeFnRef(QualifiedIdName name) {
        return new FnRef(name.getSpan(), false, Collections.singletonList(name),
                         Collections.<StaticArg>emptyList());
    }

    /** Alternatively, you can invoke the SubscriptExpr constructor without parenthesized or op */
    public static SubscriptExpr makeSubscriptExpr(Span span, Expr obj,
                                                  List<Expr> subs) {
        return new SubscriptExpr(span, false, obj, subs,
                                 Option.<Enclosing>none());
    }

    public static SubscriptExpr makeSubscriptExpr(Span span, Expr obj,
                                                  List<Expr> subs,
                                                  Option<Enclosing> op) {
        return new SubscriptExpr(span, false, obj, subs, op);
    }

    public static TightJuxt makeTightJuxt(Span span, Expr first, Expr second) {
        return new TightJuxt(span, false, Useful.list(first, second));
    }

    public static VarRef makeVarRef(Span span, String s) {
        return new VarRef(span, false, NodeFactory.makeQualifiedIdName(span, s));
    }

    public static VarRef makeVarRef(String s) {
        return makeVarRef(NodeFactory.makeId(s));
    }

    public static VarRef makeVarRef(Span sp, Id id) {
        return new VarRef(sp, false,
                          NodeFactory.makeQualifiedIdName(id));
    }

    public static VarRef makeVarRef(Id id) {
        return new VarRef(id.getSpan(), false,
                          NodeFactory.makeQualifiedIdName(id));
    }

    public static VarRef makeVarRef(Iterable<Id> apiIds, Id name) {
        QualifiedIdName qName = NodeFactory.makeQualifiedIdName(apiIds, name);
        return new VarRef(qName.getSpan(), false, qName);
    }

    public static VarRef makeVarRef(Span span, Iterable<Id> apiIds, Id name) {
        QualifiedIdName qName = NodeFactory.makeQualifiedIdName(span, apiIds, name);
        return new VarRef(span, false, qName);
    }

    /** Assumes {@code ids} is nonempty. */
    public static VarRef makeVarRef(Iterable<Id> ids) {
        QualifiedIdName qName = NodeFactory.makeQualifiedIdName(ids);
        return new VarRef(qName.getSpan(), false, qName);
    }

    public static VarRef makeVarRef(APIName api, Id name) {
        QualifiedIdName qName = NodeFactory.makeQualifiedIdName(api, name);
        return new VarRef(qName.getSpan(), false, qName);
    }

    public static FieldRef makeFieldRef(Expr receiver, Id field) {
        return new FieldRef(FortressUtil.spanTwo(receiver, field), false,
                            receiver, field);
    }

    public static Expr makeReceiver(Iterable<Id> ids) {
        Expr expr = makeVarRef(IterUtil.first(ids));
        for (Id id : IterUtil.skipFirst(ids)) {
            expr = new FieldRef(FortressUtil.spanTwo(expr, id), expr, id);
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
                    new GeneratorClause(Collections.<Id>emptyList(), cond),
                    _then), _else);
    }

    public static If makeIf(Span sp, Expr cond, Block _then) {
        return
            makeIf(sp,
                new IfClause(
                    new GeneratorClause(Collections.<Id>emptyList(), cond),
                    _then));
    }

    public static Block makeBlock(Expr e) {
        List<Expr> b = new ArrayList<Expr>();
        b.add(e);
        return new Block(b);
    }

    public static Block makeBlock(Span sp, Expr e) {
        List<Expr> b = new ArrayList<Expr>();
        b.add(e);
        return new Block(sp, b);
    }

    public static LocalVarDecl makeLocalVarDecl(Id p, Expr _r, Expr _body_expr) {
        List<Expr> _body = new ArrayList<Expr>();
        List<LValue> _lhs = new ArrayList<LValue>();
        Option<Expr> _rhs = Option.some(_r);
        _body.add(_body_expr);
        _lhs.add(new LValueBind(p,false));
        return new LocalVarDecl(_body, _lhs, _rhs);
    }

    public static LocalVarDecl makeLocalVarDecl(Span sp, Id p, Expr _r, Expr _body_expr) {
        List<Expr> _body = new ArrayList<Expr>();
        List<LValue> _lhs = new ArrayList<LValue>();
        Option<Expr> _rhs = Option.some(_r);
        _body.add(_body_expr);
        _lhs.add(new LValueBind(sp, p,false));
        return new LocalVarDecl(sp, _body, _lhs, _rhs);
    }

    public static ChainExpr makeChainExpr(Expr e, Op _op, Expr _expr) {
        List<Pair<Op,Expr>> links = new ArrayList<Pair<Op,Expr>>();
        Pair<Op,Expr> link = new Pair<Op, Expr>(_op, _expr);
        links.add(link);
        return new ChainExpr(e, links);
    }

    public static ChainExpr makeChainExpr(Span sp, Expr e, Op _op, Expr _expr) {
        List<Pair<Op,Expr>> links = new ArrayList<Pair<Op,Expr>>();
        Pair<Op,Expr> link = new Pair<Op, Expr>(_op, _expr);
        links.add(link);
        return new ChainExpr(sp, e, links);
    }

    public static Throw makeThrow(String st) {
        return new Throw(makeVarRef(st));
    }

    public static Throw makeThrow(Span sp, String st) {
        return new Throw(sp, makeVarRef(sp, st));
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
                                      implicit_type_parameters, expr.toString(),
                                      stParams, staticArgs,
                                      Option.some(Collections.<Param>emptyList()));
    }

    public static Expr makeInParentheses(Expr expr) {
        return expr.accept(new NodeAbstractVisitor<Expr>() {
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
                return new CaseExpr(e.getSpan(), true, e.getParam(),
                                    e.getCompare(), e.getClauses(),
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
                return new ArgExpr(e.getSpan(), true, e.getExprs(), e.getVarargs(), e.getKeywords());
            }
            public Expr forTupleExpr(TupleExpr e) {
                return new TupleExpr(e.getSpan(), true, e.getExprs());
            }
            public Expr forTypeCase(Typecase e) {
                return new Typecase(e.getSpan(), true, e.getBind(),
                                    e.getClauses(), e.getElseClause());
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
                return new Exit(e.getSpan(), true, e.getTarget(), e.getReturnExpr());
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
            public Expr forOprExpr(OprExpr e) {
                return new OprExpr(e.getSpan(), true, e.getOp(), e.getArgs());
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
                return new FnRef(e.getSpan(), true, e.getFns(),
                                 e.getStaticArgs());
            }
            public Expr forOpRef(OpRef e) {
                return new OpRef(e.getSpan(), true, e.getOps(),
                                 e.getStaticArgs());
            }
            public Expr forSubscriptExpr(SubscriptExpr e) {
                return new SubscriptExpr(e.getSpan(), true, e.getObj(),
                                         e.getSubs(), e.getOp());
            }
            public Expr defaultCase(Node x) {
                return bug(x, "makeInParentheses: " + x.getClass() +
                              " is not a subtype of Expr.");
            }
        });
    }

    public static Expr simplifyMathPrimary(Span span, Expr front, MathItem mi) {
        if (mi instanceof ExprMI) {
            Expr expr = ((ExprMI)mi).getExpr();
            return new TightJuxt(span, Useful.list(front, expr));
        } else if (mi instanceof ExponentiationMI) {
            ExponentiationMI expo = (ExponentiationMI)mi;
            Option<Expr> expr = expo.getExpr();
            if (expr.isSome()) // ^ Exponent
                return ASTUtil.infix(span, front, expo.getOp(),
                                     Option.unwrap(expr));
            else // ExponentOp
                return ASTUtil.postfix(span, front, expo.getOp());
        } else { // mi instanceof SubscriptingMI
            SubscriptingMI sub = (SubscriptingMI)mi;
            return makeSubscriptExpr(span, front, sub.getExprs(),
                                     Option.wrap(sub.getOp()));
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
        return new Typecase(tc.getSpan(),
                tc.isParenthesized(),
                Pair.make(lid, Option.wrap(expr)),
                tc.getClauses(),
                tc.getElseClause());
    }

}
