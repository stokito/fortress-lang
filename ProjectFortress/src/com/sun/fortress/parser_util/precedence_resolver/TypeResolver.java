/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

/*
 * Resolving type operator precedence during parsing.
 */
package com.sun.fortress.parser_util.precedence_resolver;

import com.sun.fortress.exceptions.ProgramError;
import static com.sun.fortress.exceptions.ProgramError.error;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeFactory;
import static com.sun.fortress.nodes_util.NodeFactory.makeInParentheses;
import com.sun.fortress.nodes_util.NodeUtil;
import static com.sun.fortress.nodes_util.NodeUtil.spanTwo;
import static com.sun.fortress.nodes_util.OprUtil.noColonText;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.parser_util.precedence_opexpr.*;
import com.sun.fortress.useful.Cons;
import com.sun.fortress.useful.PureList;
import edu.rice.cs.plt.tuple.Option;

import java.util.Collections;
import java.util.List;

/*
 * This class implements the functionality for resolving operator precedence during parsing.
 * Resolution is performed via pattern matching along with an auxillary stack.
 */
public class TypeResolver {

    private static boolean isVerbose = false;

    private static Precedence precedence(Op op1, Op op2) {
        String op1name = noColonText(op1);
        String op2name = noColonText(op2);
        return PrecedenceMap.ONLY.get(op1name, op2name);
    }

    private static boolean isTypeOp(Op op) {
        return op.getText().equals("->");
    }

    private static boolean isDOT(Op op) {
        return op.getText().equals("DOT");
    }

    private static Op product(Span span) {
        return NodeFactory.makeOpInfix(span, " ");
    }

    private static Op quotient(Span span) {
        return NodeFactory.makeOpInfix(span, "/");
    }

    private static Type makeProductDim(Span span, TaggedDimType expr0, DimExpr expr2) throws TypeConvertFailure {
        DimExpr dim = NodeFactory.makeDimBinaryOp(span, true, dimToDim(expr0.getDimExpr()), dimToDim(expr2), product(
                span));
        return NodeFactory.makeTaggedDimType(span, true, typeToType(expr0.getElemType()), dim, expr0.getUnitExpr());
    }

    private static Type makeQuotientDim(Span span, TaggedDimType expr0, DimExpr expr2) throws TypeConvertFailure {
        DimExpr dim = NodeFactory.makeDimBinaryOp(span, true, dimToDim(expr0.getDimExpr()), dimToDim(expr2), quotient(
                span));
        return NodeFactory.makeTaggedDimType(span, true, typeToType(expr0.getElemType()), dim, expr0.getUnitExpr());
    }

    private static Type makeJuxt(Type first, Type second) throws ReadError {
        Span span = spanTwo(first, second);
        try {
            DimExpr dim = makeInParentheses(typeToDim(second));
            if (first instanceof TaggedDimType) {
                return makeProductDim(span, (TaggedDimType) first, dim);
            } else {
                return NodeFactory.makeTaggedDimType(span, true, typeToType(first), dim, Option.<Expr>none());
            }
        }
        catch (TypeConvertFailure x) {
            throw new ReadError(span, "Misuse of type juxtaposition.");
        }
    }

    public static Type makeMatrixType(Span span, Type type, IntExpr power) {
        StaticArg arg = NodeFactory.makeIntArg(NodeUtil.getSpan(power), power);
        ExtentRange er = NodeFactory.makeExtentRange(NodeUtil.getSpan(power),
                                                     Option.<StaticArg>none(),
                                                     Option.some(arg),
                                                     Option.<Op>none());
        return NodeFactory.makeMatrixType(span, typeToType(type), er);
    }

    private static PureList<TypeInfixFrame> looseInfixStack(Type e,
                                                            Op op,
                                                            Effect effect,
                                                            PureList<TypeInfixFrame> stack) throws ReadError {
        if (stack.isEmpty()) {
            return PureList.<TypeInfixFrame>make(new TypeLoose(op, effect, e));
        } else { // !stack.isEmpty()
            Cons<TypeInfixFrame> _stack = (Cons<TypeInfixFrame>) stack;
            TypeInfixFrame frame = _stack.getFirst();
            PureList<TypeInfixFrame> rest = _stack.getRest();

            if (frame instanceof TypeTight) {
                Op _op = ((TypeTight) frame).getOp();
                if (precedence(_op, op) instanceof Higher) {
                    return looseInfixStack(finishInfixFrame(e, frame), op, effect, rest);
                } else {
                    throw new ReadError(spanTwo(_op, op),
                                        "Tight operator " + _op.getText() + " near loose operator " + op.getText() +
                                        " of incompatible precedence.");
                }
            } else { // (frame instanceof TypeLoose)
                Op _op = ((TypeLoose) frame).getOp();
                Type first = ((TypeLoose) frame).getArg();
                Span span = spanTwo(first, e);
                if (op.getText().equals(_op.getText())) {
                    if (isDOT(op)) {
                        try {
                            if (first instanceof TaggedDimType) {
                                Type _new = makeProductDim(span, (TaggedDimType) first, typeToDim(e));
                                return rest.cons(new TypeLoose(_op, effect, _new));
                            } else {
                                throw new ReadError(NodeUtil.getSpan(first), "Dimensions are expected.");
                            }
                        }
                        catch (TypeConvertFailure x) {
                            throw new ReadError(NodeUtil.getSpan(first), "Dimensions are expected.");
                        }
                    } else // op.getText().equals("->") ||
                        // op.getText().equals("/") ||
                        // op.getText().equals("per")
                        throw new ReadError(spanTwo(_op, op), "Loose infix " + op.getText() + " does not associate.");
                } else {
                    Precedence prec = precedence(op, _op);
                    if (prec instanceof Higher) {
                        return stack.cons(new TypeLoose(op, effect, e));
                    } else if (prec instanceof Lower || prec instanceof Equal) {
                        return looseInfixStack(finishInfixFrame(e, frame), op, effect, rest);
                    } else { // prec instanceof None
                        throw new ReadError(spanTwo(_op, op),
                                            "Loose operators " + _op.getText() + " and " + op.getText() +
                                            " have incomparable precedence.");
                    }
                }
            }
        }
    }

    private static PureList<TypeInfixFrame> tightInfixStack(Type e,
                                                            Op op,
                                                            Effect effect,
                                                            PureList<TypeInfixFrame> stack) throws ReadError {
        if (stack.isEmpty()) {
            return PureList.<TypeInfixFrame>make(new TypeTight(op, effect, e));
        } else { // !stack.isEmpty()
            Cons<TypeInfixFrame> _stack = (Cons<TypeInfixFrame>) stack;
            TypeInfixFrame frame = _stack.getFirst();
            PureList<TypeInfixFrame> rest = _stack.getRest();

            if (frame instanceof TypeLoose) {
                Op _op = ((TypeLoose) frame).getOp();
                if (precedence(op, _op) instanceof Higher) {
                    return stack.cons(new TypeTight(op, effect, e));
                } else {
                    throw new ReadError(spanTwo(_op, op),
                                        "Loose operator " + _op.getText() + " near tight operator " + op.getText() +
                                        " of incompatible precedence.");
                }
            } else { // (frame instanceof TypeTight)
                Op _op = ((TypeTight) frame).getOp();
                Type first = ((TypeTight) frame).getArg();
                Span span = spanTwo(first, e);
                if (op.getText().equals(_op.getText())) {
                    if (isDOT(op)) {
                        try {
                            if (first instanceof TaggedDimType) {
                                Type _new = makeProductDim(span, (TaggedDimType) first, typeToDim(e));
                                return rest.cons(new TypeTight(_op, effect, _new));
                            } else {
                                throw new ReadError(NodeUtil.getSpan(first), "Dimensions are expected.");
                            }
                        }
                        catch (TypeConvertFailure x) {
                            throw new ReadError(NodeUtil.getSpan(first), "Dimensions are expected.");
                        }
                    } else // op.getText().equals("->") ||
                        // op.getText().equals("/") ||
                        // op.getText().equals("per")
                        throw new ReadError(spanTwo(_op, op), "Tight infix " + op.getText() + " does not associate.");
                } else {
                    Precedence prec = precedence(op, _op);
                    if (prec instanceof Higher) {
                        return stack.cons(new TypeTight(op, effect, e));
                    } else if (prec instanceof Lower || prec instanceof Equal) {
                        return tightInfixStack(finishInfixFrame(e, frame), op, effect, rest);
                    } else { // prec instanceof None
                        throw new ReadError(spanTwo(_op, op),
                                            "Tight operators " + _op.getText() + " and " + op.getText() +
                                            " have incomparable precedence.");
                    }
                }
            }
        }
    }

    private static Type finishInfixFrame(Type last, TypeInfixFrame frame) throws ReadError {
        Op op = frame.getOp();
        Type first = frame.getArg();
        if (isTypeOp(op)) {
            return NodeFactory.makeArrowType(spanTwo(first, last), false, first, typeToType(last),
					     frame.getEffect(),
					     Collections.<StaticParam>emptyList(),
                                             Option.<WhereClause>none());
        } else { // !(isTypeOp(op))
            try {
                DimExpr _second = typeToDim(last);
                Span span = spanTwo(first, _second);
                if (first instanceof TaggedDimType) {
                    if (isDOT(op)) return makeProductDim(span, (TaggedDimType) first, _second);
                    else // op.getText().equals("/") ||
                        // op.getText().equals("per")
                        return makeQuotientDim(span, (TaggedDimType) first, _second);
                } else {
                    DimExpr _first = typeToDim(first);
                    if (isDOT(op)) return NodeFactory.makeDimBinaryOp(span, true, _first, _second, product(span));
                    else // op.getText().equals("/") ||
                        // op.getText().equals("per")
                        return NodeFactory.makeDimBinaryOp(span, true, _first, _second, quotient(span));
                    //                    throw new ReadError(NodeUtil.getSpan(op), "DimExpr is expected.");
                }
            }
            catch (TypeConvertFailure x) {
                throw new ReadError(NodeUtil.getSpan(op), "DimExpr is expected.");
            }

        }
    }

    private static Type finishInfixStack(Type last, PureList<TypeInfixFrame> stack) throws ReadError {
        if (stack.isEmpty()) {
            return last;
        } else { // !stack.isEmpty()
            TypeInfixFrame frame = ((Cons<TypeInfixFrame>) stack).getFirst();
            PureList<TypeInfixFrame> rest = ((Cons<TypeInfixFrame>) stack).getRest();
            return finishInfixStack(finishInfixFrame(last, frame), rest);
        }
    }

    private static Type resolveInfixStack(PureList<InfixOpExpr> opTypes, PureList<TypeInfixFrame> stack) throws
                                                                                                         ReadError {
        if (opTypes.isEmpty()) {
            throw new ReadError(NodeFactory.parserSpan, "Empty juxtaposition/operation expression.");
        } else { // !opTypes.isEmpty()
            InfixOpExpr first = ((Cons<InfixOpExpr>) opTypes).getFirst();
            if (!(first instanceof RealType)) {
                throw new ReadError(NodeFactory.parserSpan, "A type or dimension is expected.");
            }
            Type type = ((RealType) first).getType();
            if (opTypes.size() == 1) {
                return finishInfixStack(type, stack);
            }
            if (opTypes.size() >= 3) {
                Cons<InfixOpExpr> rest = (Cons<InfixOpExpr>) ((Cons<InfixOpExpr>) opTypes).getRest();
                InfixOpExpr second = ((Cons<InfixOpExpr>) rest).getFirst();
                PureList<InfixOpExpr> _rest = ((Cons<InfixOpExpr>) rest).getRest();
                // Regular operators
                if (second instanceof LooseInfix) return resolveInfixStack(_rest, looseInfixStack(type,
                                                                                                  ((LooseInfix) second).getOp(),
                                                                                                  ((LooseInfix) second).getEffect(),
                                                                                                  stack));
                else if (second instanceof TightInfix) return resolveInfixStack(_rest, tightInfixStack(type,
                                                                                                       ((TightInfix) second).getOp(),
                                                                                                       ((TightInfix) second).getEffect(),
                                                                                                       stack));
            }
            // Errors
            // first instanceof RealType
            PureList<InfixOpExpr> rest = ((Cons<InfixOpExpr>) opTypes).getRest();
            if (rest.isEmpty()) {
                throw new ReadError(NodeUtil.getSpan(((RealType) first).getType()),
                                    "Nonexhaustive pattern matching.");
            } else { // !rest.isEmpty()
                Cons<InfixOpExpr> _rest = (Cons<InfixOpExpr>) rest;
                InfixOpExpr second = _rest.getFirst();

                if (second instanceof RealType) {
                    Span span = spanTwo(((RealType) first).getType(), ((RealType) second).getType());
                    throw new ReadError(span, "Failed to process juxtaposition.");
                } else { // second instanceof JuxtInfix
                    Op op = ((JuxtInfix) second).getOp();
                    throw new ReadError(NodeUtil.getSpan(op),
                                        "Interpreted " + op.getText() + " with no right operand as infix.");
                }
            }
        }
    }

    private static Type resolveInfix(PureList<InfixOpExpr> opTypes) throws ReadError {
        if (isVerbose) System.out.println("resolveInfix...");
        return resolveInfixStack(opTypes, PureList.<TypeInfixFrame>make());
    }

    private static PureList<InfixOpExpr> buildJuxt(PureList<InfixOpExpr> opTypes, PureList<Type> revTypes) throws
                                                                                                           ReadError {
        if (revTypes.size() < 2) {
            throw new ReadError(NodeFactory.parserSpan, "Misuse of type/dimension juxtaposition.");
        }
        Object[] prefix = revTypes.toArray(2);
        Type first = (Type) prefix[1];
        Type second = (Type) prefix[0];
        Span span = spanTwo(first, second);
        if (revTypes.size() > 2) {
            throw new ReadError(span, "Too much types/dimensions juxtaposed.");
        }
        if (!opTypes.isEmpty() && ((Cons<InfixOpExpr>) opTypes).getFirst() instanceof TightInfix) {
            TightInfix _first = (TightInfix) ((Cons<InfixOpExpr>) opTypes).getFirst();
            throw new ReadError(span, "Precedence mismatch: juxtaposition and " + _first.getOp().toString() + ".");
        } else if (!opTypes.isEmpty() && ((Cons<InfixOpExpr>) opTypes).getFirst() instanceof RealType) {
            Type _first = ((RealType) ((Cons<InfixOpExpr>) opTypes).getFirst()).getType();
            PureList<InfixOpExpr> rest = ((Cons<InfixOpExpr>) opTypes).getRest();
            return buildJuxt(rest, PureList.make(_first, makeJuxt(first, second)));
        } else {
            return (resolveJuxt(opTypes)).cons(new RealType(makeJuxt(first, second)));
        }
    }

    private static PureList<InfixOpExpr> resolveJuxt(PureList<InfixOpExpr> opTypes) throws ReadError {
        if (isVerbose) System.out.println("resolveJuxt...");
        if (opTypes.isEmpty()) {
            return PureList.<InfixOpExpr>make();
        } else { // opTypes instanceof Cons
            Cons<InfixOpExpr> _opTypes = (Cons<InfixOpExpr>) opTypes;
            InfixOpExpr first = _opTypes.getFirst();
            PureList<InfixOpExpr> rest = _opTypes.getRest();

            if (opTypes.size() >= 3) {
                Object[] prefix = opTypes.toArray(3);

                if (prefix[0] instanceof TightInfix && prefix[1] instanceof RealType && prefix[2] instanceof RealType) {
                    Span span = spanTwo(((RealType) prefix[1]).getType(), ((RealType) prefix[2]).getType());
                    throw new ReadError(span,
                                        "Precedence mismatch: " + ((TightInfix) prefix[0]).getOp().toString() +
                                        " and juxtaposition.");
                }
            }
            if (opTypes.size() >= 2) {
                InfixOpExpr second = ((Cons<InfixOpExpr>) rest).getFirst();
                PureList<InfixOpExpr> _rest = ((Cons<InfixOpExpr>) rest).getRest();
                if (first instanceof RealType && second instanceof RealType) {
                    return buildJuxt(_rest, PureList.make(((RealType) second).getType(), ((RealType) first).getType()));
                }
            }
            return (resolveJuxt(rest)).cons(first);
        }
    }

    private static PureList<InfixOpExpr> resolvePrefix(PureList<PrefixOpExpr> opTypes) throws ReadError {
        if (isVerbose) System.out.println("resolvePrefix...");
        if (opTypes.isEmpty()) {
            return PureList.<InfixOpExpr>make();
        } else { // !opTypes.isEmpty()
            PrefixOpExpr first = ((Cons<PrefixOpExpr>) opTypes).getFirst();
            PureList<PrefixOpExpr> rest = ((Cons<PrefixOpExpr>) opTypes).getRest();

            if (first instanceof Prefix) {
                PureList<InfixOpExpr> _opTypes = resolvePrefix(rest);

                if (!_opTypes.isEmpty() && ((Cons<InfixOpExpr>) _opTypes).getFirst() instanceof RealType) {
                    Cons<InfixOpExpr> __opTypes = (Cons<InfixOpExpr>) _opTypes;
                    Op op = ((Prefix) first).getOp();
                    PureList<InfixOpExpr> _rest = __opTypes.getRest();
                    try {
                        DimExpr e = typeToDim(((RealType) __opTypes.getFirst()).getType());
                        return _rest.cons(new RealType(NodeFactory.makeDimUnaryOp(NodeUtil.getSpan(e), true, e, op)));
                    }
                    catch (TypeConvertFailure x) {
                        throw new ReadError(NodeUtil.getSpan(op),
                                            "Prefix operator " + op.toString() + " without argument.");
                    }
                } else {
                    Op op = ((Prefix) first).getOp();
                    throw new ReadError(NodeUtil.getSpan(op),
                                        "Prefix operator " + op.toString() + " without argument.");
                }
            } else { // first isinstanceof InfixOpExpr
                return (resolvePrefix(rest)).cons((InfixOpExpr) first);
            }
        }
    }

    private static boolean isDiv(Op op) {
        return (op.getText().equals("/") || op.getText().equals("per"));
    }

    private static PureList<PrefixOpExpr> resolveTightDiv(PureList<PrefixOpExpr> opTypes) throws ReadError {
        if (isVerbose) System.out.println("resolveTightDiv...");
        if (opTypes.isEmpty()) {
            return PureList.<PrefixOpExpr>make();
        } else { // !opTypes.isEmpty()
            Cons<PrefixOpExpr> _opTypes = (Cons<PrefixOpExpr>) opTypes;
            PrefixOpExpr first = _opTypes.getFirst();
            PureList<PrefixOpExpr> rest = _opTypes.getRest();

            if (opTypes.size() >= 4) {
                Object[] prefix = opTypes.toArray(4);
                PureList<PrefixOpExpr> _rest = ((Cons<PrefixOpExpr>) rest).getRest();
                // PureList<PrefixOpExpr> __rest = ((Cons<PrefixOpExpr>) _rest).getRest();

                if (prefix[0] instanceof RealType && prefix[1] instanceof JuxtInfix && prefix[2] instanceof RealType &&
                    prefix[3] instanceof JuxtInfix) {
                    Op op1 = ((JuxtInfix) prefix[1]).getOp();
                    Op op3 = ((JuxtInfix) prefix[3]).getOp();

                    if (isDiv(op1) && isDiv(op3)) {
                        throw new ReadError(spanTwo(op1, op3), "The operator " + "/ and per do not associate.");
                    }
                }
            }
            if (opTypes.size() >= 3) {
                Object[] prefix = opTypes.toArray(3);
                PureList<PrefixOpExpr> _rest = ((Cons<PrefixOpExpr>) rest).getRest();
                PureList<PrefixOpExpr> __rest = ((Cons<PrefixOpExpr>) _rest).getRest();

                if (prefix[0] instanceof RealType && prefix[1] instanceof TightInfix && prefix[2] instanceof RealType) {
                    Op op1 = ((TightInfix) prefix[1]).getOp();
                    if (isDiv(op1)) {
                        try {
                            DimExpr expr2 = typeToDim(((RealType) prefix[2]).getType());
                            Type expr0 = ((RealType) prefix[0]).getType();
                            Span span = spanTwo(expr0, expr2);
                            Type e;
                            try {
                                DimExpr _expr0 = typeToDim(expr0);
                                e = NodeFactory.makeDimBinaryOp(span, true, _expr0, expr2, quotient(span));
                            }
                            catch (TypeConvertFailure x) {
                                if (expr0 instanceof TaggedDimType) {
                                    e = makeQuotientDim(span, (TaggedDimType) expr0, expr2);
                                } else {
                                    throw new ReadError(span, "Misuse of tight" + "division.");
                                }
                            }
                            return resolveTightDiv(__rest.cons(new RealType(e)));
                        }
                        catch (TypeConvertFailure x) {
                        }
                    }
                }
            }
            if (first instanceof TightInfix && isDiv(((TightInfix) first).getOp())) {
                throw new ReadError(NodeUtil.getSpan(((TightInfix) first).getOp()), "Misuse of tight division.");
            } else {
                return (resolveTightDiv(rest)).cons(first);
            }
        }
    }

    private static PureList<PrefixOpExpr> resolvePostfix(PureList<PostfixOpExpr> opTypes) throws ReadError {
        if (isVerbose) System.out.println("resolvePostfix...");
        if (opTypes.isEmpty()) {
            return PureList.<PrefixOpExpr>make();
        } else {
            Cons<PostfixOpExpr> _opTypes = (Cons<PostfixOpExpr>) opTypes;
            PostfixOpExpr first = _opTypes.getFirst();
            PureList<PostfixOpExpr> rest = _opTypes.getRest();

            if (first instanceof RealType && !(rest.isEmpty()) &&
                ((Cons<PostfixOpExpr>) rest).getFirst() instanceof Postfix) {
                try {
                    DimExpr _first = typeToDim(((RealType) first).getType());
                    Cons<PostfixOpExpr> _rest = (Cons<PostfixOpExpr>) rest;
                    Op op = ((Postfix) (_rest.getFirst())).getOp();
                    PureList<PostfixOpExpr> restRest = _rest.getRest();
                    DimExpr dim = NodeFactory.makeDimUnaryOp(NodeUtil.getSpan(_first),
                                                             NodeUtil.isParenthesized(_first),
                                                             _first,
                                                             op);
                    return resolvePostfix(restRest.cons(new RealType(dim)));
                }
                catch (TypeConvertFailure x) {
                    throw new ReadError(NodeUtil.getSpan(((RealType) first).getType()),
                                        "Type conversion failed.");
                }
            } else if (first instanceof Postfix) {
                throw new ReadError(NodeUtil.getSpan(((Postfix) first).getOp()),
                                    "Postfix operator %s without argument.");
            } else { // first instanceof PrefixOpExpr
                return (resolvePostfix(rest)).cons((PrefixOpExpr) first);
            }
        }
    }

    private static Type buildLayer(PureList<PostfixOpExpr> opTypes) throws ReadError {
        return resolveInfix(resolveJuxt(resolvePrefix(resolveTightDiv(resolvePostfix(opTypes)))));
    }

    private static Type typeToType(Type type) {
        return (Type) type.accept(new NodeUpdateVisitor() {
            public Type forDimExponent(DimExponent t) {
                return makeMatrixType(NodeUtil.getSpan(t), typeToType(t.getBase()), t.getPower());
            }

            public Type forDimExpr(DimExpr t) {
                try {
                    return dimToType(t);
                }
                catch (TypeConvertFailure x) {
                    return (Type) t;
                }
            }

            public Type forTaggedDimType(TaggedDimType t) {
                return NodeFactory.makeTaggedDimType(NodeUtil.getSpan(t),
                                                     true,
                                                     typeToType(t.getElemType()),
                                                     makeInParentheses(dimToDim(t.getDimExpr())),
                                                     t.getUnitExpr());
            }
        });
    }

    public static DimExpr typeToDim(Type type) throws TypeConvertFailure {
        try {
            return type.accept(new NodeAbstractVisitor<DimExpr>() {
                public DimExpr forDimExpr(DimExpr t) {
                    return t;
                }

                public DimExpr forDimExponent(DimExponent t) {
                    try {
                        return NodeFactory.makeDimExponent(NodeUtil.getSpan(t),
                                                           NodeUtil.isParenthesized(t),
                                                           typeToDim(t.getBase()),
                                                           t.getPower());
                    }
                    catch (TypeConvertFailure e) {
                        return error(t, "A dimension is expected but " + "a type is found.");
                    }
                }

                public DimExpr forMatrixType(MatrixType t) {
                    try {
                        List<ExtentRange> dimensions = t.getDimensions();
                        if (dimensions.size() != 1) return error(t,
                                                                 "A dimension is expected but " + "a type is found.");
                        ExtentRange dimension = dimensions.get(0);
                        IntArg power = (IntArg) dimension.getSize().unwrap();
                        return NodeFactory.makeDimExponent(NodeUtil.getSpan(t),
                                                           NodeUtil.isParenthesized(t),
                                                           typeToDim(t.getElemType()),
                                                           power.getIntVal());
                    }
                    catch (Throwable e) {
                        return error(t, "A dimension is expected but " + "a type is found.");
                    }
                }

                public DimExpr forTaggedDimType(TaggedDimType t) {
                    try {
                        if (t.getUnitExpr().isNone()) {
                            return NodeFactory.makeDimBinaryOp(NodeUtil.getSpan(t),
                                                               NodeUtil.isParenthesized(t),
                                                               typeToDim(t.getElemType()),
                                                               t.getDimExpr(),
                                                               product(NodeUtil.getSpan(t)));
                        } else return error(t, "A dimension is expected " + "but a type is found.");
                    }
                    catch (TypeConvertFailure e) {
                        return error(t, "A dimension is expected but " + "a type is found.");
                    }
                }

                public DimExpr forVarType(VarType t) {
                    return NodeFactory.makeDimRef(NodeUtil.getSpan(t), NodeUtil.isParenthesized(t), t.getName());
                }

                public DimExpr defaultCase(Node x) {
                    return error(x, "A dimension is expected but a " + "type is found.");
                }
            });
        }
        catch (ProgramError e) {
            throw new TypeConvertFailure(e.getMessage());
        }
    }

    private static DimExpr dimToDim(DimExpr dim) {
        return dim.accept(new NodeAbstractVisitor<DimExpr>() {
            public DimExpr forDimExponent(DimExponent d) {
                try {
                    return NodeFactory.makeDimExponent(NodeUtil.getSpan(d),
                                                       NodeUtil.isParenthesized(d),
                                                       typeToDim(d.getBase()),
                                                       d.getPower());
                }
                catch (TypeConvertFailure x) {
                    return (DimExpr) d;
                }
            }

            public DimExpr forDimBinaryOp(DimBinaryOp d) {
                return NodeFactory.makeDimBinaryOp(NodeUtil.getSpan(d),
                                                   NodeUtil.isParenthesized(d),
                                                   dimToDim(d.getLeft()),
                                                   dimToDim(d.getRight()),
                                                   d.getOp());
            }

            public DimExpr forDimUnaryOp(DimUnaryOp d) {
                return NodeFactory.makeDimUnaryOp(NodeUtil.getSpan(d),
                                                  NodeUtil.isParenthesized(d),
                                                  dimToDim(d.getDimVal()),
                                                  d.getOp());
            }

            public DimExpr defaultCase(Node x) {
                return (DimExpr) x;
            }
        });
    }

    private static Type dimToType(DimExpr dim) throws TypeConvertFailure {
        try {
            return dim.accept(new NodeAbstractVisitor<Type>() {
                public Type forDimRef(DimRef d) {
                    return NodeFactory.makeVarType(NodeUtil.getSpan(d),
                                                   NodeUtil.isParenthesized(d),
                                                   d.getName(),
                                                   NodeFactory.lexicalDepth);
                }

                public Type forDimBinaryOp(DimBinaryOp d) {
                    try {
                        return NodeFactory.makeTaggedDimType(NodeUtil.getSpan(d),
                                                             NodeUtil.isParenthesized(d),
                                                             makeInParentheses(dimToType(d.getLeft())),
                                                             dimToDim(d.getRight()),
                                                             Option.<Expr>none());
                    }
                    catch (TypeConvertFailure e) {
                        return error(e.getMessage());
                    }
                }

                public Type forDimExponent(DimExponent d) {
                    return makeMatrixType(NodeUtil.getSpan(d), typeToType(d.getBase()), d.getPower());
                }

                public Type defaultCase(Node x) {
                    return error(x, "A type is expected but a " + "dimension is found:\n  " + x);
                }
            });
        }
        catch (ProgramError e) {
            throw new TypeConvertFailure(e.getMessage());
        }
    }

    private static Type canonicalizeType(Type ty) {
        if (ty instanceof TaggedDimType) {
            TaggedDimType _ty = (TaggedDimType) ty;
            return NodeFactory.makeTaggedDimType(NodeUtil.getSpan(_ty), NodeUtil.isParenthesized(_ty), canonicalizeType(
                    _ty.getElemType()), canonicalizeDim(_ty.getDimExpr()), _ty.getUnitExpr());
        } else return ty;
    }

    private static boolean isProductDim(DimBinaryOp dim) {
        return dim.getOp().getText().equals(" ");
    }

    private static DimExpr canonicalizeDim(DimExpr dim) {
        return dim.accept(new NodeAbstractVisitor<DimExpr>() {
            public DimExpr forDimBinaryOp(DimBinaryOp d) {
                if (isProductDim(d)) {
                    DimExpr left = d.getLeft();
                    DimExpr right = d.getRight();
                    if (right instanceof DimBinaryOp && isProductDim((DimBinaryOp) right)) {
                        DimBinaryOp _right = (DimBinaryOp) right;
                        DimExpr rleft = _right.getLeft();
                        Span span = spanTwo(left, rleft);
                        left = NodeFactory.makeDimBinaryOp(span, true, left, rleft, product(span));
                        left = canonicalizeDim(left);
                        right = canonicalizeDim(_right.getRight());
                    } else left = canonicalizeDim(left);
                    return NodeFactory.makeDimBinaryOp(NodeUtil.getSpan(d),
                                                       NodeUtil.isParenthesized(d),
                                                       left,
                                                       right,
                                                       product(NodeUtil.getSpan(d)));
                } else {
                    return NodeFactory.makeDimBinaryOp(NodeUtil.getSpan(d),
                                                       NodeUtil.isParenthesized(d),
                                                       canonicalizeDim(d.getLeft()),
                                                       canonicalizeDim(d.getRight()),
                                                       quotient(NodeUtil.getSpan(d)));

                }
            }

            public DimExpr forDimExponent(DimExponent d) {
                return NodeFactory.makeDimExponent(NodeUtil.getSpan(d),
                                                   NodeUtil.isParenthesized(d),
                                                   canonicalizeDim((DimExpr) d.getBase()),
                                                   d.getPower());
            }

            public DimExpr forDimUnaryOp(DimUnaryOp d) {
                return NodeFactory.makeDimUnaryOp(NodeUtil.getSpan(d),
                                                  NodeUtil.isParenthesized(d),
                                                  canonicalizeDim(d.getDimVal()),
                                                  d.getOp());
            }

            public DimExpr defaultCase(Node x) {
                return (DimExpr) x;
            }
        });
    }

    public static Type resolveOps(PureList<PostfixOpExpr> opTypes) {
        try {
            if (isVerbose) {
                System.out.println("resolveOps(");
                for (PostfixOpExpr exp : opTypes.toJavaList()) {
                    System.out.println("  " + exp);
                }
                System.out.println(")");
            }
            Type type = buildLayer(opTypes);
            if (isVerbose) System.out.println("after resolveOps: " + type);
            try {
                if (type instanceof DimExpr) return canonicalizeType(dimToType((DimExpr) type));
                else return canonicalizeType(typeToType(type));
            }
            catch (TypeConvertFailure x) {
                return canonicalizeType((Type) type);
            }
        }
        catch (Throwable e) {
            String msg = e.getMessage();
            StringBuilder buf = new StringBuilder();
            buf.append(msg);
            for (PrecedenceOpExpr type : opTypes.toJavaList()) {
                buf.append("\n  " + type.toString());
            }
            msg = buf.toString();
            return error("Resolution of operator property failed for:\n" + msg);
        }
    }

    public static DimExpr resolveOpsDim(PureList<PostfixOpExpr> opTypes) {
        try {
            Type type = buildLayer(opTypes);
            try {
                if (type instanceof DimExpr) return canonicalizeDim(typeToDim((DimExpr) type));
                else return canonicalizeDim(typeToDim(type));
            }
            catch (TypeConvertFailure x) {
                return canonicalizeDim((DimExpr) type);
            }
        }
        catch (Throwable e) {
            String msg = e.getMessage();
            StringBuilder buf = new StringBuilder();
            buf.append(msg);
            for (PrecedenceOpExpr type : opTypes.toJavaList()) {
                buf.append("\n  " + type.toString());
            }
            msg = buf.toString();
            return error("Resolution of operator property failed for:\n" + msg);
        }
    }
}
