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

package com.sun.fortress.interpreter.nodes_util;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.math.BigInteger;
import com.sun.fortress.interpreter.nodes.*;
import com.sun.fortress.interpreter.useful.*;
import com.sun.fortress.interpreter.evaluator.InterpreterError;
import com.sun.fortress.interpreter.glue.WellKnownNames;
import com.sun.fortress.interpreter.parser.precedence.resolver.PrecedenceMap;

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
            } else {
                digits = s.substring(0, underLoc);
                // Base other, no ".", parse as BigInteger and convert.
                String base_digits = s.substring(underLoc + 1);

                if (!Unicode.charactersOverlap(base_digits, "0123456789")) {
                    base = Unicode.numberToValue(base_digits);
                } else {
                    base = Integer.parseInt(base_digits);
                }
            }
            digits = dozenalHack(digits, base);
            intPart = new BigInteger(digits, base);

        } else {
            // There is a fraction part.

            int base;

            if (underLoc == -1) {
                base = 10;
                underLoc = s.length();
            } else {
                String base_digits = s.substring(underLoc + 1);
                if (!Unicode.charactersOverlap(base_digits, "0123456789")) {
                    base = Unicode.numberToValue(base_digits);
                } else {
                    base = Integer.parseInt(base_digits);
                }
            }
            {
                String digits = s.substring(0, dotLoc);
                if (digits.length() > 0) {
                    digits = dozenalHack(digits, base);
                    intPart = new BigInteger(digits, base);
                } else {
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

                } else {
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
        } else {
            String digits = s.substring(0, underLoc);
            String base_digits = s.substring(underLoc + 1);
            int base;
            if (!Unicode.charactersOverlap(base_digits, "0123456789")) {
                base = Unicode.numberToValue(base_digits);
            } else {
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


    public static LetExpr makeLetExpr(LetExpr expr, List<Expr> body) {
        if (expr instanceof GeneratedExpr) {
            GeneratedExpr exp = (GeneratedExpr) expr;
            return new GeneratedExpr(exp.getSpan(), false, body, exp.getExpr(),
                                     exp.getGens());
        } else if (expr instanceof LetFn) {
            return new LetFn(expr.getSpan(),false, body, ((LetFn)expr).getFns());
        } else if (expr instanceof LocalVarDecl) {
            LocalVarDecl exp = (LocalVarDecl) expr;
            return new LocalVarDecl(exp.getSpan(), false, body, exp.getLhs(),
                                    exp.getRhs());
        } else {
            throw new Error(expr.getClass() + " is not a subtype of LetExpr.");
        }
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

    public static VarRefExpr makeVarRefExpr(Span span, String s) {
        return new VarRefExpr(span, false, new Id(span, s));
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
        } else {
            List<StaticParam> tparams =
                new ArrayList<StaticParam>(implicit_type_parameters.values());
            stParams = Some.makeSomeList(tparams);
            for (String s : implicit_type_parameters.keySet()) {
                staticArgs.add(NodeFactory.makeTypeArg(expr.getSpan(), s));
            }
        }
        return new _RewriteObjectExpr(expr.getSpan(), false, expr.getTraits(),
                                      expr.getDefOrDecls(),
                                      implicit_type_parameters, expr.toString(),
                                      stParams, staticArgs,
                    new Some<List<Param>>(Collections.<Param>emptyList()));
    }

    public static Expr makeInParentheses(Expr expr) {
        if (expr instanceof AsExpr) {
            AsExpr e = (AsExpr)expr;
            return new AsExpr(e.getSpan(), true, e.getExpr(), e.getType());
        } else if (expr instanceof AsIfExpr) {
            AsIfExpr e = (AsIfExpr)expr;
            return new AsIfExpr(e.getSpan(), true, e.getExpr(), e.getType());
        } else if (expr instanceof Assignment) {
            Assignment e = (Assignment)expr;
            return new Assignment(e.getSpan(), true, e.getLhs(), e.getOp(),
                                  e.getRhs());
        } else if (expr instanceof Block) {
            Block e = (Block)expr;
            return new Block(e.getSpan(), true, e.getExprs());
        } else if (expr instanceof CaseExpr) {
            CaseExpr e = (CaseExpr)expr;
            return new CaseExpr(e.getSpan(), true, e.getParam(), e.getCompare(),
                                e.getClauses(), e.getElseClause());
        } else if (expr instanceof Do) {
            Do e = (Do)expr;
            return new Do(e.getSpan(), true, e.getFronts());
        } else if (expr instanceof For) {
            For e = (For)expr;
            return new For(e.getSpan(), true, e.getGens(), e.getBody());
        } else if (expr instanceof If) {
            If e = (If)expr;
            return new If(e.getSpan(), true, e.getClauses(), e.getElseClause());
        } else if (expr instanceof Label) {
            Label e = (Label)expr;
            return new Label(e.getSpan(), true, e.getName(), e.getBody());
        } else if (expr instanceof ObjectExpr) {
            ObjectExpr e = (ObjectExpr)expr;
            return new ObjectExpr(e.getSpan(), true, e.getTraits(),
                                  e.getDefOrDecls());
        } else if (expr instanceof _RewriteObjectExpr) {
            _RewriteObjectExpr e = (_RewriteObjectExpr)expr;
            return new _RewriteObjectExpr(e.getSpan(), true, e.getTraits(),
                                          e.getDefOrDecls(),
                                          e.getImplicitTypeParameters(),
                                          e.getGenSymName(), e.getStaticParams(),
                                          e.getStaticArgs(), e.getParams());
        } else if (expr instanceof Try) {
            Try e = (Try)expr;
            return new Try(e.getSpan(), true, e.getBody(), e.getCatchClause(),
                           e.getForbid(), e.getFinallyClause());
        } else if (expr instanceof TupleExpr) {
            TupleExpr e = (TupleExpr)expr;
            return new TupleExpr(e.getSpan(), true, e.getExprs());
        } else if (expr instanceof KeywordsExpr) {
            KeywordsExpr e = (KeywordsExpr)expr;
            return new KeywordsExpr(e.getSpan(), true, e.getExprs(),
                                    e.getKeywords());
        } else if (expr instanceof TypeCase) {
            TypeCase e = (TypeCase)expr;
            return new TypeCase(e.getSpan(), true, e.getBind(), e.getClauses(),
                                e.getElseClause());
        } else if (expr instanceof VarargsExpr) {
            VarargsExpr e = (VarargsExpr)expr;
            return new VarargsExpr(e.getSpan(), true, e.getVarargs());
        } else if (expr instanceof While) {
            While e = (While)expr;
            return new While(e.getSpan(), true, e.getTest(), e.getBody());
        } else if (expr instanceof _WrappedFValue) {
            _WrappedFValue e = (_WrappedFValue)expr;
            return new _WrappedFValue(e.getSpan(), true, e.getFValue());
        } else if (expr instanceof Accumulator) {
            Accumulator e = (Accumulator)expr;
            return new Accumulator(e.getSpan(), true, e.getOp(), e.getGens(),
                                   e.getBody());
        } else if (expr instanceof AtomicExpr) {
            AtomicExpr e = (AtomicExpr)expr;
            return new AtomicExpr(e.getSpan(), true, e.getExpr());
        } else if (expr instanceof Exit) {
            Exit e = (Exit)expr;
            return new Exit(e.getSpan(), true, e.getName(), e.getReturnExpr());
        } else if (expr instanceof Spawn) {
            Spawn e = (Spawn)expr;
            return new Spawn(e.getSpan(), true, e.getBody());
        } else if (expr instanceof Throw) {
            Throw e = (Throw)expr;
            return new Throw(e.getSpan(), true, e.getExpr());
        } else if (expr instanceof TryAtomicExpr) {
            TryAtomicExpr e = (TryAtomicExpr)expr;
            return new TryAtomicExpr(e.getSpan(), true, e.getExpr());
        } else if (expr instanceof FnExpr) {
            FnExpr e = (FnExpr)expr;
            return new FnExpr(e.getSpan(), true, e.getFnName(),
                              e.getStaticParams(), e.getParams(),
                              e.getReturnType(), e.getWhere(),
                              e.getThrowsClause(), e.getBody());
        } else if (expr instanceof GeneratedExpr) {
            GeneratedExpr e = (GeneratedExpr)expr;
            return new GeneratedExpr(e.getSpan(), true, e.getBody(), e.getExpr(),
                                     e.getGens());
        } else if (expr instanceof LetFn) {
            LetFn e = (LetFn)expr;
            return new LetFn(e.getSpan(), true, e.getBody(), e.getFns());
        } else if (expr instanceof LocalVarDecl) {
            LocalVarDecl e = (LocalVarDecl)expr;
            return new LocalVarDecl(e.getSpan(), true, e.getBody(), e.getLhs(),
                                    e.getRhs());
        } else if (expr instanceof OprExpr) {
            OprExpr e = (OprExpr)expr;
            return new OprExpr(e.getSpan(), true, e.getOp(), e.getArgs());
        } else if (expr instanceof Apply) {
            Apply e = (Apply)expr;
            return new Apply(e.getSpan(), true, e.getFn(), e.getArgs());
        } else if (expr instanceof MapExpr) {
            MapExpr e = (MapExpr)expr;
            return new MapExpr(e.getSpan(), true, e.getElements());
        } else if (expr instanceof ArrayElement) {
            ArrayElement e = (ArrayElement)expr;
            return new ArrayElement(e.getSpan(), true, e.getElement());
        } else if (expr instanceof ArrayElements) {
            ArrayElements e = (ArrayElements)expr;
            return new ArrayElements(e.getSpan(), true, e.getDimension(),
                                     e.getElements());
        } else if (expr instanceof FloatLiteral) {
            FloatLiteral e = (FloatLiteral)expr;
            return new FloatLiteral(e.getSpan(), true, e.getText(),
                                    e.getIntPart(), e.getNumerator(),
                                    e.getDenomBase(), e.getDenomPower());
        } else if (expr instanceof IntLiteral) {
            IntLiteral e = (IntLiteral)expr;
            return new IntLiteral(e.getSpan(), true, e.getText(), e.getVal());
        } else if (expr instanceof CharLiteral) {
            CharLiteral e = (CharLiteral)expr;
            return new CharLiteral(e.getSpan(), true, e.getText(), e.getVal());
        } else if (expr instanceof StringLiteral) {
            StringLiteral e = (StringLiteral)expr;
            return new StringLiteral(e.getSpan(), true, e.getText());
        } else if (expr instanceof VoidLiteral) {
            VoidLiteral e = (VoidLiteral)expr;
            return new VoidLiteral(e.getSpan(), true, e.getText());
        } else if (expr instanceof VarRefExpr) {
            VarRefExpr e = (VarRefExpr)expr;
            return new VarRefExpr(e.getSpan(), true, e.getVar());
        } else if (expr instanceof ArrayComprehension) {
            ArrayComprehension e = (ArrayComprehension)expr;
            return new ArrayComprehension(e.getSpan(), true, e.getClauses());
        } else if (expr instanceof SetComprehension) {
            SetComprehension e = (SetComprehension)expr;
            return new SetComprehension(e.getSpan(), true, e.getGens(),
                                        e.getElement());
        } else if (expr instanceof MapComprehension) {
            MapComprehension e = (MapComprehension)expr;
            return new MapComprehension(e.getSpan(), true, e.getGens(),
                                        e.getKey(), e.getValue());
        } else if (expr instanceof ListComprehension) {
            ListComprehension e = (ListComprehension)expr;
            return new ListComprehension(e.getSpan(), true, e.getGens(),
                                         e.getElement());
        } else if (expr instanceof ChainExpr) {
            ChainExpr e = (ChainExpr)expr;
            return new ChainExpr(e.getSpan(), true, e.getFirst(), e.getLinks());
        } else if (expr instanceof FieldSelection) {
            FieldSelection e = (FieldSelection)expr;
            return new FieldSelection(e.getSpan(), true, e.getObj(), e.getId());
        } else if (expr instanceof LooseJuxt) {
            LooseJuxt e = (LooseJuxt)expr;
            return new LooseJuxt(e.getSpan(), true, e.getExprs());
        } else if (expr instanceof TightJuxt) {
            TightJuxt e = (TightJuxt)expr;
            return new TightJuxt(e.getSpan(), true, e.getExprs());
        } else if (expr instanceof TypeApply) {
            TypeApply e = (TypeApply)expr;
            return new TypeApply(e.getSpan(), true, e.getExpr(), e.getArgs());
        } else if (expr instanceof SubscriptExpr) {
            SubscriptExpr e = (SubscriptExpr)expr;
            return new SubscriptExpr(e.getSpan(), true, e.getObj(), e.getSubs(),
                                     e.getOp());
        } else if (expr instanceof UnitRef) {
            UnitRef e = (UnitRef)expr;
            return new UnitRef(e.getSpan(), true, e.getVal());
        } else {
            throw new InterpreterError("makeInParentheses: " + expr.getClass() +
                                       " is not a subtype of Expr.");
        }
    }
}
