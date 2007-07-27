/*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
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
import java.util.ArrayList;
import java.util.List;
import java.math.BigInteger;
import com.sun.fortress.nodes.*;
import com.sun.fortress.useful.*;
import com.sun.fortress.interpreter.evaluator.InterpreterError;
import com.sun.fortress.interpreter.glue.WellKnownNames;
import com.sun.fortress.parser.precedence.resolver.PrecedenceMap;

public class ExprFactory {
    public static CharLiteral makeCharLiteral(Span span, String s) {
        return new CharLiteral(span, false, s, s.charAt(0));
    }

    public static FloatLiteral makeFloatLiteral(Span span, String s) {
        BigInteger intPart;
        BigInteger numerator;
        int denomBase;
        int denomPower;

        // Trim leading zeroes
        while (s.length() > 1 && s.charAt(0) == '0') {
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
        return new FloatLiteral(span, false, s,
                                intPart, numerator, denomBase, denomPower);
    }

    public static FnExpr makeFnExpr(Span span, List<Param> params, Expr body) {
        return makeFnExpr(span, params, new None<TypeRef>(),
                          Collections.<TypeRef>emptyList(), body);
    }

    public static FnExpr makeFnExpr(Span span, List<Param> params,
                                    Option<TypeRef> returnType,
                                    List<TypeRef> throwsClause, Expr body) {
        return new FnExpr(span, false, new AnonymousFnName(span),
                          new None<List<StaticParam>>(), params, returnType,
                          Collections.<WhereClause>emptyList(), throwsClause,
                          body);
    }

    public static IntLiteral makeIntLiteral(Span span, BigInteger val) {
        return new IntLiteral(span, false, val.toString(), val);
    }

    public static IntLiteral makeIntLiteral(Span span, String s) {
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
        return new IntLiteral(span, false, s, val);
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


    public static LetExpr makeLetExpr(final LetExpr expr, final List<Expr> body) {
        return expr.accept(new NodeAbstractVisitor<LetExpr>() {
            public LetExpr forGeneratedExpr(GeneratedExpr expr) {
                return new GeneratedExpr(expr.getSpan(), false, body, expr.getExpr(),
                                         expr.getGens());
            }
            public LetExpr forLetFn(LetFn expr) {
                return new LetFn(expr.getSpan(),false, body, expr.getFns());
            }
            public LetExpr forLocalVarDecl(LocalVarDecl expr) {
                return new LocalVarDecl(expr.getSpan(), false, body, expr.getLhs(),
                                        expr.getRhs());
            }
        });
    }


    public static OprExpr makeOprExpr(Span span, OprName op) {
        return new OprExpr(span, false, op, new ArrayList<Expr>());
    }

    public static OprExpr makeOprExpr(Span span, OprName op, Expr arg) {
        List<Expr> es = new ArrayList<Expr>();
        es.add(arg);
        return new OprExpr(span, false, op, es);
    }

    public static OprExpr makeOprExpr(Span span, OprName op, Expr first,
                                      Expr second) {
        List<Expr> es = new ArrayList<Expr>();
        es.add(first);
        es.add(second);
        return new OprExpr(span, false, op, es);
    }

    public static SubscriptExpr makeSubscriptExpr(Span span, Expr obj,
                                                  List<Expr> subs) {
        return new SubscriptExpr(span, false, obj, subs, None.<Enclosing>make());
    }

    public static TightJuxt makeTightJuxt(Span span, Expr first, Expr second) {
        return new TightJuxt(span, false, Useful.list(first, second));
    }

    public static VarRef makeVarRef(Span span, String s) {
        return new VarRef(span, false, new Id(span, s));
    }

    public static VoidLiteral makeVoidLiteral(Span span) {
        return new VoidLiteral(span, false, "");
    }

    public static _RewriteObjectExpr make_RewriteObjectExpr(ObjectExpr expr,
                         BATree<String, StaticParam> implicit_type_parameters) {
        List<StaticArg> staticArgs =
            new ArrayList<StaticArg>(implicit_type_parameters.size());
        Option<List<StaticParam>> stParams;
        if (implicit_type_parameters.size() == 0) {
            stParams = new None<List<StaticParam>>();
        }
        else {
            List<StaticParam> tparams =
                new ArrayList<StaticParam>(implicit_type_parameters.values());
            stParams = Some.makeSomeList(tparams);
            for (String s : implicit_type_parameters.keySet()) {
                staticArgs.add(NodeFactory.makeTypeArg(expr.getSpan(), s));
            }
        }
        return new _RewriteObjectExpr(expr.getSpan(), false, expr.getExtendsClause(),
                                      expr.getAbsDeclOrDecls(),
                                      implicit_type_parameters, expr.toString(),
                                      stParams, staticArgs,
                    new Some<List<Param>>(Collections.<Param>emptyList()));
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
                return new Assignment(e.getSpan(), true, e.getLhs(), e.getOp(),
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
                return new Label(e.getSpan(), true, e.getId(), e.getBody());
            }
            public Expr forObjectExpr(ObjectExpr e) {
                return new ObjectExpr(e.getSpan(), true, e.getExtendsClause(),
                                      e.getAbsDeclOrDecls());
            }
            public Expr for_RewriteObjectExpr(_RewriteObjectExpr e) {
                return new _RewriteObjectExpr(e.getSpan(), true, e.getExtendsClause(),
                                              e.getAbsDeclOrDecls(),
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
            public Expr forTupleExpr(TupleExpr e) {
                return new TupleExpr(e.getSpan(), true, e.getExprs());
            }
            public Expr forKeywordsExpr(KeywordsExpr e) {
                return new KeywordsExpr(e.getSpan(), true, e.getExprs(),
                                        e.getKeywords());
            }
            public Expr forTypeCase(Typecase e) {
                return new Typecase(e.getSpan(), true, e.getBind(),
                                    e.getClauses(), e.getElseClause());
            }
            public Expr forVarargsExpr(VarargsExpr e) {
                return new VarargsExpr(e.getSpan(), true, e.getVarargs());
            }
            public Expr forWhile(While e) {
                return new While(e.getSpan(), true, e.getTest(), e.getBody());
            }
            public Expr forAccumulator(Accumulator e) {
                return new Accumulator(e.getSpan(), true, e.getOp(), e.getGens(),
                                       e.getBody());
            }
            public Expr forAtomicExpr(AtomicExpr e) {
                return new AtomicExpr(e.getSpan(), true, e.getExpr());
            }
            public Expr forExit(Exit e) {
                return new Exit(e.getSpan(), true, e.getOptId(),
                                e.getReturnExpr());
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
                return new FnExpr(e.getSpan(), true, e.getFnName(),
                                  e.getStaticParams(), e.getParams(),
                                  e.getReturnType(), e.getWhere(),
                                  e.getThrowsClause(), e.getBody());
            }
            public Expr forGeneratedExpr(GeneratedExpr e) {
                return new GeneratedExpr(e.getSpan(), true, e.getBody(),
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
            public Expr forApply(Apply e) {
                return new Apply(e.getSpan(), true, e.getFn(), e.getArgs());
            }
            public Expr forMapExpr(MapExpr e) {
                return new MapExpr(e.getSpan(), true, e.getElements());
            }
            public Expr forArrayElement(ArrayElement e) {
                return new ArrayElement(e.getSpan(), true, e.getElement());
            }
            public Expr forArrayElements(ArrayElements e) {
                return new ArrayElements(e.getSpan(), true, e.getDimension(),
                                         e.getElements());
            }
            public Expr forFloatLiteral(FloatLiteral e) {
                return new FloatLiteral(e.getSpan(), true, e.getText(),
                                        e.getIntPart(), e.getNumerator(),
                                        e.getDenomBase(), e.getDenomPower());
            }
            public Expr forIntLiteral(IntLiteral e) {
                return new IntLiteral(e.getSpan(), true, e.getText(),
                                      e.getVal());
            }
            public Expr forCharLiteral(CharLiteral e) {
                return new CharLiteral(e.getSpan(), true, e.getText(),
                                       e.getVal());
            }
            public Expr forStringLiteral(StringLiteral e) {
                return new StringLiteral(e.getSpan(), true, e.getText());
            }
            public Expr forVoidLiteral(VoidLiteral e) {
                return new VoidLiteral(e.getSpan(), true, e.getText());
            }
            public Expr forVarRef(VarRef e) {
                return new VarRef(e.getSpan(), true, e.getVar());
            }
            public Expr forArrayComprehension(ArrayComprehension e) {
                return new ArrayComprehension(e.getSpan(), true, e.getClauses());
            }
            public Expr forSetComprehension(SetComprehension e) {
                return new SetComprehension(e.getSpan(), true, e.getGens(),
                                            e.getElement());
            }
            public Expr forMapComprehension(MapComprehension e) {
                return new MapComprehension(e.getSpan(), true, e.getGens(),
                                            e.getKey(), e.getValue());
            }
            public Expr forListComprehension(ListComprehension e) {
                return new ListComprehension(e.getSpan(), true, e.getGens(),
                                             e.getElement());
            }
            public Expr forChainExpr(ChainExpr e) {
                return new ChainExpr(e.getSpan(), true, e.getFirst(),
                                     e.getLinks());
            }
            public Expr forFieldSelection(FieldSelection e) {
                return new FieldSelection(e.getSpan(), true, e.getObj(),
                                          e.getId());
            }
            public Expr forLooseJuxt(LooseJuxt e) {
                return new LooseJuxt(e.getSpan(), true, e.getExprs());
            }
            public Expr forTightJuxt(TightJuxt e) {
                return new TightJuxt(e.getSpan(), true, e.getExprs());
            }
            public Expr forFnRef(FnRef e) {
                return new FnRef(e.getSpan(), true, e.getExpr(), e.getArgs());
            }
            public Expr forSubscriptExpr(SubscriptExpr e) {
                return new SubscriptExpr(e.getSpan(), true, e.getObj(),
                                         e.getSubs(), e.getOp());
            }
            public Expr forUnitRef(UnitRef e) {
                return new UnitRef(e.getSpan(), true, e.getVal());
            }
            public Expr defaultCase(Node x) {
                throw new InterpreterError("makeInParentheses: " + x.getClass() +
                                           " is not a subtype of Expr.");
            }
        });
    }
}
