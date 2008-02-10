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

import java.util.Collections;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.math.BigInteger;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.lambda.Lambda;

import com.sun.fortress.nodes.*;
import com.sun.fortress.useful.*;
import com.sun.fortress.interpreter.glue.WellKnownNames;
import com.sun.fortress.parser_util.precedence_resolver.PrecedenceMap;
import com.sun.fortress.parser_util.precedence_resolver.ASTUtil;
import com.sun.fortress.parser_util.FortressUtil;

import static com.sun.fortress.interpreter.evaluator.InterpreterBug.bug;

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

    public static TupleExpr makeTuple(Expr... exprs) {
        return makeTuple(Arrays.asList(exprs));
    }

    private static OpRef makeOpRef(QualifiedOpName op) {
        return new OpRef(op.getSpan(), Collections.singletonList(op));
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
                return new Accumulator(e.getSpan(), true, e.getOpr(), e.getGens(),
                                       e.getBody());
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

}
