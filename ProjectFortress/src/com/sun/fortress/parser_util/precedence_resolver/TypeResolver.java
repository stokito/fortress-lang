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

/*
 * Resolving type operator precedence during parsing.
 */
package com.sun.fortress.parser_util.precedence_resolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.parser_util.FortressUtil;
import com.sun.fortress.parser_util.precedence_opexpr.*;
import com.sun.fortress.useful.Cons;
import com.sun.fortress.useful.Fn;
import com.sun.fortress.useful.Pair;
import com.sun.fortress.useful.PureList;
import edu.rice.cs.plt.tuple.Option;

import com.sun.fortress.exceptions.ProgramError;

import static com.sun.fortress.exceptions.ProgramError.error;
import static com.sun.fortress.nodes_util.OprUtil.noColonText;
import static com.sun.fortress.nodes_util.NodeFactory.makeInParentheses;
import static com.sun.fortress.parser_util.FortressUtil.spanTwo;

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

    private static Type makeProductDim(Span span, TaggedDimType expr0,
                                       DimExpr expr2) throws TypeConvertFailure {
        DimExpr dim = new ProductDim(span, true, dimToDim(expr0.getDim()),
                                     dimToDim(expr2));
        return new TaggedDimType(span, true,
                                 typeToType(expr0.getType()), dim,
                                 expr0.getUnit());
    }

    private static Type makeQuotientDim(Span span, TaggedDimType expr0,
                                        DimExpr expr2)
        throws TypeConvertFailure {
        DimExpr dim = new QuotientDim(span, true, dimToDim(expr0.getDim()),
                                      dimToDim(expr2));
        return new TaggedDimType(span, true,
                                 typeToType(expr0.getType()), dim,
                                 expr0.getUnit());
    }

    private static TypeOrDomain makeJuxt(TypeOrDomain first,
                                         TypeOrDomain second) throws ReadError {
        Span span = spanTwo(first, second);
        try {
            DimExpr dim = makeInParentheses(typeToDim(second));
            if (first instanceof TaggedDimType) {
                return makeProductDim(span, (TaggedDimType)first, dim);
            } else {
                return new TaggedDimType(span, true, typeToType(first), dim);
            }
        } catch (TypeConvertFailure x) {
            throw new ReadError(span, "Misuse of type juxtaposition.");
        }
    }

    public static Type makeMatrixType(Span span, Type type, IntExpr power) {
        StaticArg arg = new IntArg(power.getSpan(), power);
        ExtentRange er = new ExtentRange(power.getSpan(),
                                         Option.<StaticArg>none(),
                                         Option.some(arg),
                                         Option.<Op>none());
        return NodeFactory.makeMatrixType(span, typeToType(type), er);
    }

    private static PureList<TypeInfixFrame>
        looseInfixStack(TypeOrDomain e, Op op, Effect effect,
                        PureList<TypeInfixFrame> stack) throws ReadError {
        if (stack.isEmpty()) {
            return PureList.<TypeInfixFrame>make(new TypeLoose(op, effect, e));
        } else { // !stack.isEmpty()
            Cons<TypeInfixFrame> _stack = (Cons<TypeInfixFrame>)stack;
            TypeInfixFrame frame = _stack.getFirst();
            PureList<TypeInfixFrame> rest = _stack.getRest();

            if (frame instanceof TypeTight) {
                Op _op = ((TypeTight)frame).getOp();
                if (precedence(_op, op) instanceof Higher) {
                    return looseInfixStack(finishInfixFrame(e,frame),op,
                                           effect, rest);
                } else {
                    throw new ReadError(spanTwo(_op,op),
                                        "Tight operator " + _op.getText() +
                                        " near loose operator " + op.getText()
                                        + " of incompatible precedence.");
                }
            } else { // (frame instanceof TypeLoose)
                Op _op = ((TypeLoose)frame).getOp();
                TypeOrDomain first = ((TypeLoose)frame).getArg();
                Span span = spanTwo(first, e);
                if (op.getText().equals(_op.getText())) {
                    if (isDOT(op)) {
                        try {
                            if (first instanceof TaggedDimType) {
                                Type _new = makeProductDim(span,
                                                           (TaggedDimType)first,
                                                           typeToDim(e));
                                return rest.cons(new TypeLoose(_op,effect,_new));
                            } else {
                                throw new ReadError(first.getSpan(),
                                                    "Dimensions are expected.");
                            }
                        } catch (TypeConvertFailure x) {
                            throw new ReadError(first.getSpan(),
                                                "Dimensions are expected.");
                        }
                    } else // op.getText().equals("->") ||
                           // op.getText().equals("/") ||
                           // op.getText().equals("per")
                        throw new ReadError(spanTwo(_op,op), "Loose infix " +
                                            op.getText() + " does not associate.");
                } else {
                    Precedence prec = precedence(op, _op);
                    if (prec instanceof Higher) {
                        return stack.cons(new TypeLoose(op, effect, e));
                    } else if (prec instanceof Lower ||
                               prec instanceof Equal) {
                        return looseInfixStack(finishInfixFrame(e,frame), op,
                                               effect, rest);
                    } else { // prec instanceof None
                        throw new ReadError(spanTwo(_op, op),
                                            "Loose operators " + _op.getText()
                                            + " and " + op.getText() +
                                            " have incomparable precedence.");
                    }
                }
            }
        }
    }

  private static PureList<TypeInfixFrame>
      tightInfixStack(TypeOrDomain e, Op op, Effect effect,
                      PureList<TypeInfixFrame> stack) throws ReadError {
      if (stack.isEmpty()) {
          return PureList.<TypeInfixFrame>make(new TypeTight(op, effect, e));
      } else { // !stack.isEmpty()
          Cons<TypeInfixFrame> _stack = (Cons<TypeInfixFrame>)stack;
          TypeInfixFrame frame = _stack.getFirst();
          PureList<TypeInfixFrame> rest = _stack.getRest();

          if (frame instanceof TypeLoose) {
              Op _op = ((TypeLoose)frame).getOp();
              if (precedence(op, _op) instanceof Higher) {
                  return stack.cons(new TypeTight(op, effect, e));
              } else {
                  throw new ReadError(spanTwo(_op,op),
                                      "Loose operator " + _op.getText() +
                                      " near tight operator " + op.getText()
                                      + " of incompatible precedence.");
              }
          } else { // (frame instanceof TypeTight)
              Op _op = ((TypeTight)frame).getOp();
              TypeOrDomain first = ((TypeTight)frame).getArg();
              Span span = spanTwo(first, e);
              if (op.getText().equals(_op.getText())) {
                  if (isDOT(op)) {
                        try {
                            if (first instanceof TaggedDimType) {
                                Type _new = makeProductDim(span,
                                                           (TaggedDimType)first,
                                                           typeToDim(e));
                                return rest.cons(new TypeTight(_op,effect,_new));
                            } else {
                                throw new ReadError(first.getSpan(),
                                                    "Dimensions are expected.");
                            }
                        } catch (TypeConvertFailure x) {
                            throw new ReadError(first.getSpan(),
                                                "Dimensions are expected.");
                        }
                  } else // op.getText().equals("->") ||
                         // op.getText().equals("/") ||
                         // op.getText().equals("per")
                      throw new ReadError(spanTwo(_op,op), "Tight infix " +
                                          op.getText() + " does not associate.");
              } else {
                  Precedence prec = precedence(op, _op);
                  if (prec instanceof Higher) {
                      return stack.cons(new TypeTight(op, effect, e));
                  }
                  else if (prec instanceof Lower ||
                           prec instanceof Equal) {
                      return tightInfixStack(finishInfixFrame(e,frame), op,
                                             effect, rest);
                  } else { // prec instanceof None
                      throw new ReadError(spanTwo(_op, op),
                                          "Tight operators " + _op.getText()
                                          + " and " + op.getText() +
                                          " have incomparable precedence.");
                  }
              }
          }
      }
  }

    private static TypeOrDomain finishInfixFrame(TypeOrDomain last,
                                                 TypeInfixFrame frame)
        throws ReadError {
        Op op = frame.getOp();
        TypeOrDomain first = frame.getArg();
        if (isTypeOp(op)) {
            return new ArrowType(spanTwo(first, last), true,
                                 typeToDomain(first),
                                 typeToType(last), frame.getEffect());
        } else { // !(isTypeOp(op))
            try {
                DimExpr _second = typeToDim(last);
                Span span = spanTwo(first,_second);
                if (first instanceof TaggedDimType) {
                    if (isDOT(op))
                        return makeProductDim(span, (TaggedDimType)first,
                                              _second);
                    else // op.getText().equals("/") ||
                         // op.getText().equals("per")
                        return makeQuotientDim(span, (TaggedDimType)first,
                                               _second);
                } else {
                    DimExpr _first = typeToDim(first);
                    if (isDOT(op))
                        return new ProductDim(span, true, _first, _second);
                    else // op.getText().equals("/") ||
                         // op.getText().equals("per")
                        return new QuotientDim(span, true, _first, _second);
                    //                    throw new ReadError(op.getSpan(), "DimExpr is expected.");
                }
            } catch (TypeConvertFailure x) {
                throw new ReadError(op.getSpan(), "DimExpr is expected.");
            }

        }
    }

    private static TypeOrDomain finishInfixStack(TypeOrDomain last,
                                                 PureList<TypeInfixFrame> stack)
        throws ReadError {
        if (stack.isEmpty()) {
            return last;
        } else { // !stack.isEmpty()
            TypeInfixFrame frame = ((Cons<TypeInfixFrame>)stack).getFirst();
            PureList<TypeInfixFrame> rest = ((Cons<TypeInfixFrame>)stack).getRest();
            return finishInfixStack(finishInfixFrame(last, frame), rest);
        }
    }

    private static TypeOrDomain resolveInfixStack(PureList<InfixOpExpr> opTypes,
                                                  PureList<TypeInfixFrame> stack)
        throws ReadError {
        if (opTypes.isEmpty()) {
            throw new ReadError(new Span(),
                                "Empty juxtaposition/operation expression.");
        } else { // !opTypes.isEmpty()
            InfixOpExpr first = ((Cons<InfixOpExpr>)opTypes).getFirst();
            if (!(first instanceof RealType)) {
                throw new ReadError(new Span(),
                                    "A type or dimension is expected.");
            }
            TypeOrDomain type = ((RealType)first).getType();
            if (opTypes.size() == 1) {
                return finishInfixStack(type, stack);
            }
            if (opTypes.size() >= 3) {
                Cons<InfixOpExpr> rest =
                    (Cons<InfixOpExpr>)((Cons<InfixOpExpr>)opTypes).getRest();
                InfixOpExpr second = ((Cons<InfixOpExpr>)rest).getFirst();
                PureList<InfixOpExpr> _rest = ((Cons<InfixOpExpr>)rest).getRest();
                // Regular operators
                if (second instanceof LooseInfix)
                    return resolveInfixStack
                        (_rest, looseInfixStack(type,
                                                ((LooseInfix)second).getOp(),
                                                ((LooseInfix)second).getEffect(),
                                                stack));
                else if (second instanceof TightInfix)
                    return resolveInfixStack
                        (_rest, tightInfixStack(type,
                                                ((TightInfix)second).getOp(),
                                                ((TightInfix)second).getEffect(),
                                                stack));
            }
            // Errors
            if (first instanceof JuxtInfix) {
                Op op = ((JuxtInfix)first).getOp();
                throw new ReadError(op.getSpan(),
                                    "Interpreted " + op.getText() +
                                    " with no left operand as infix.");
            } else { // first instanceof RealType
                PureList<InfixOpExpr> rest = ((Cons<InfixOpExpr>)opTypes).getRest();
                if (rest.isEmpty()) {
                    throw new ReadError(((RealType)first).getType().getSpan(),
                                        "Nonexhaustive pattern matching.");
                } else { // !rest.isEmpty()
                    Cons<InfixOpExpr> _rest = (Cons<InfixOpExpr>)rest;
                    InfixOpExpr second = _rest.getFirst();

                    if (second instanceof RealType) {
                        Span span = spanTwo(((RealType)first).getType(),
                                            ((RealType)second).getType());
                        throw new ReadError(span,
                                            "Failed to process juxtaposition.");
                    } else { // second instanceof JuxtInfix
                        Op op = ((JuxtInfix)second).getOp();
                        throw new ReadError(op.getSpan(),
                                            "Interpreted " + op.getText() +
                                            " with no right operand as infix.");
                    }
                }
            }
        }
    }

    private static TypeOrDomain resolveInfix(PureList<InfixOpExpr> opTypes)
        throws ReadError {
        if (isVerbose) System.out.println("resolveInfix...");
        return resolveInfixStack(opTypes, PureList.<TypeInfixFrame>make());
    }

    private static PureList<InfixOpExpr> buildJuxt(PureList<InfixOpExpr> opTypes,
                                                   PureList<TypeOrDomain> revTypes)
        throws ReadError {
        if (revTypes.size() < 2) {
            throw new ReadError(new Span(),
                                "Misuse of type/dimension juxtaposition.");
        }
        Object[] prefix = revTypes.toArray(2);
        TypeOrDomain first = (TypeOrDomain)prefix[1];
        TypeOrDomain second = (TypeOrDomain)prefix[0];
        Span span = spanTwo(first, second);
        if (revTypes.size() > 2) {
            throw new ReadError(span, "Too much types/dimensions juxtaposed.");
        }
        if (!opTypes.isEmpty() &&
                   ((Cons<InfixOpExpr>)opTypes).getFirst() instanceof TightInfix) {
            TightInfix _first =
                (TightInfix)((Cons<InfixOpExpr>)opTypes).getFirst();
            throw new ReadError(span,
                                "Precedence mismatch: juxtaposition and " +
                                _first.getOp().toString() + ".");
        } else if (!opTypes.isEmpty() &&
                   ((Cons<InfixOpExpr>)opTypes).getFirst() instanceof RealType) {
            TypeOrDomain _first =
                ((RealType)((Cons<InfixOpExpr>)opTypes).getFirst()).getType();
            PureList<InfixOpExpr> rest = ((Cons<InfixOpExpr>)opTypes).getRest();
            return buildJuxt(rest, PureList.make(_first,makeJuxt(first,second)));
        } else {
            return (resolveJuxt(opTypes)).cons(new RealType(makeJuxt(first,
                                                                     second)));
        }
    }

    private static PureList<InfixOpExpr>
        resolveJuxt(PureList<InfixOpExpr> opTypes) throws ReadError {
        if (isVerbose) System.out.println("resolveJuxt...");
        if (opTypes.isEmpty()) { return PureList.<InfixOpExpr>make(); }

        else { // opTypes instanceof Cons
            Cons<InfixOpExpr> _opTypes = (Cons<InfixOpExpr>) opTypes;
            InfixOpExpr first = _opTypes.getFirst();
            PureList<InfixOpExpr> rest = _opTypes.getRest();

            if (opTypes.size() >= 3) {
                Object[] prefix = opTypes.toArray(3);

                if (prefix[0] instanceof TightInfix &&
                    prefix[1] instanceof RealType &&
                    prefix[2] instanceof RealType) {
                    Span span = spanTwo(((RealType)prefix[1]).getType(),
                                        ((RealType)prefix[2]).getType());
                    throw new ReadError(span,
                                        "Precedence mismatch: " +
                                        ((TightInfix)prefix[0]).getOp().toString() +
                                        " and juxtaposition.");
                    }
            }
            if (opTypes.size() >= 2) {
                InfixOpExpr second = ((Cons<InfixOpExpr>)rest).getFirst();
                PureList<InfixOpExpr> _rest = ((Cons<InfixOpExpr>)rest).getRest();
                if (first  instanceof RealType &&
                    second instanceof RealType) {
                    return buildJuxt(_rest,
                                     PureList.make(((RealType)second).getType(),
                                                   ((RealType)first).getType()));
                }
            }
            return (resolveJuxt(rest)).cons(first);
        }
    }

    private static PureList<InfixOpExpr>
        resolvePrefix(PureList<PrefixOpExpr> opTypes) throws ReadError {
        if (isVerbose) System.out.println("resolvePrefix...");
        if (opTypes.isEmpty()) { return PureList.<InfixOpExpr>make(); }

        else { // !opTypes.isEmpty()
            PrefixOpExpr first = ((Cons<PrefixOpExpr>)opTypes).getFirst();
            PureList<PrefixOpExpr> rest = ((Cons<PrefixOpExpr>)opTypes).getRest();

            if (first instanceof Prefix) {
                PureList<InfixOpExpr> _opTypes = resolvePrefix(rest);

                if (!_opTypes.isEmpty() &&
                    ((Cons<InfixOpExpr>)_opTypes).getFirst() instanceof RealType) {
                    Cons<InfixOpExpr> __opTypes = (Cons<InfixOpExpr>)_opTypes;
                    Op op = ((Prefix)first).getOp();
                    PureList<InfixOpExpr> _rest = __opTypes.getRest();
                    try {
                        DimExpr e =
                            typeToDim(((RealType)__opTypes.getFirst()).getType());
                        return _rest.cons(new RealType(new OpDim(e.getSpan(), true,
                                                                 e, op)));
                    } catch (TypeConvertFailure x) {
                        throw new ReadError(op.getSpan(),
                                            "Prefix operator " + op.toString() +
                                            " without argument.");
                    }
                } else {
                    Op op = ((Prefix)first).getOp();
                    throw new ReadError(op.getSpan(), "Prefix operator " +
                                        op.toString() + " without argument.");
                }
            } else { // first isinstanceof InfixOpExpr
                return (resolvePrefix(rest)).cons((InfixOpExpr)first);
            }
        }
    }

    private static boolean isDiv(Op op) {
        return (op.getText().equals("/") || op.getText().equals("per"));
    }

    private static PureList<PrefixOpExpr>
        resolveTightDiv(PureList<PrefixOpExpr> opTypes) throws ReadError {
        if (isVerbose) System.out.println("resolveTightDiv...");
        if (opTypes.isEmpty()) { return PureList.<PrefixOpExpr>make(); }

        else { // !opTypes.isEmpty()
            Cons<PrefixOpExpr> _opTypes = (Cons<PrefixOpExpr>)opTypes;
            PrefixOpExpr first  = _opTypes.getFirst();
            PureList<PrefixOpExpr> rest  = _opTypes.getRest();

            if (opTypes.size() >= 4) {
                Object[] prefix = opTypes.toArray(4);
                PureList<PrefixOpExpr> _rest =
                    ((Cons<PrefixOpExpr>)rest).getRest();
                PureList<PrefixOpExpr> __rest =
                    ((Cons<PrefixOpExpr>)_rest).getRest();

                if (prefix[0] instanceof RealType &&
                    prefix[1] instanceof JuxtInfix &&
                    prefix[2] instanceof RealType &&
                    prefix[3] instanceof JuxtInfix) {
                    Op op1 = ((JuxtInfix)prefix[1]).getOp();
                    Op op3 = ((JuxtInfix)prefix[3]).getOp();

                    if (isDiv(op1) && isDiv(op3)) {
                        throw new ReadError(spanTwo(op1,op3), "The operator " +
                                            "/ and per do not associate.");
                    }
                }
            }
            if (opTypes.size() >= 3) {
                Object[] prefix = opTypes.toArray(3);
                PureList<PrefixOpExpr> _rest =
                    ((Cons<PrefixOpExpr>)rest).getRest();
                PureList<PrefixOpExpr> __rest =
                    ((Cons<PrefixOpExpr>)_rest).getRest();

                if (prefix[0] instanceof RealType &&
                    prefix[1] instanceof TightInfix &&
                    prefix[2] instanceof RealType) {
                    Op op1     = ((TightInfix)prefix[1]).getOp();
                    if(isDiv(op1)) {
                        try {
                            DimExpr expr2 =
                                typeToDim(((RealType)prefix[2]).getType());
                            TypeOrDomain expr0 = ((RealType)prefix[0]).getType();
                            Span span = spanTwo(expr0, expr2);
                            Type e;
                            try {
                                DimExpr _expr0 = typeToDim(expr0);
                                e = new QuotientDim(span, true,
                                                    _expr0, expr2);
                            } catch (TypeConvertFailure x) {
                                if (expr0 instanceof TaggedDimType) {
                                    e = makeQuotientDim(span,
                                                        (TaggedDimType)expr0,
                                                        expr2);
                                } else {
                                    throw new ReadError(span, "Misuse of tight" +
                                                        "division.");
                                }
                            }
                            return resolveTightDiv(__rest.cons(new RealType(e)));
                        } catch (TypeConvertFailure x) {}
                    }
                }
            }
            if (first instanceof TightInfix &&
                isDiv(((TightInfix)first).getOp())) {
                throw new ReadError(((TightInfix)first).getOp().getSpan(),
                                    "Misuse of tight division.");
            } else {
                return (resolveTightDiv(rest)).cons(first);
            }
        }
    }

    private static PureList<PrefixOpExpr>
        resolvePostfix(PureList<PostfixOpExpr> opTypes) throws ReadError {
        if (isVerbose) System.out.println("resolvePostfix...");
        if (opTypes.isEmpty()) { return PureList.<PrefixOpExpr>make(); }

        else {
            Cons<PostfixOpExpr> _opTypes = (Cons<PostfixOpExpr>) opTypes;
            PostfixOpExpr first = _opTypes.getFirst();
            PureList<PostfixOpExpr> rest = _opTypes.getRest();

            if (first instanceof RealType && !(rest.isEmpty()) &&
                ((Cons<PostfixOpExpr>)rest).getFirst() instanceof Postfix) {
                try {
                    DimExpr _first = typeToDim(((RealType)first).getType());
                    Cons<PostfixOpExpr> _rest = (Cons<PostfixOpExpr>)rest;
                    Op op = ((Postfix)(_rest.getFirst())).getOp();
                    PureList<PostfixOpExpr> restRest = _rest.getRest();
                    DimExpr dim = new OpDim(_first.getSpan(),
                                            _first.isParenthesized(),
                                            _first, op);
                    return resolvePostfix(restRest.cons(new RealType(dim)));
                } catch (TypeConvertFailure x) {
                    throw new ReadError(((Postfix)first).getOp().getSpan(),
                                        "Postfix operator %s without argument.");
                }
            } else if (first instanceof Postfix) {
                throw new ReadError(((Postfix)first).getOp().getSpan(),
                                    "Postfix operator %s without argument.");
            } else { // first instanceof PrefixOpExpr
                return (resolvePostfix(rest)).cons((PrefixOpExpr)first);
            }
        }
    }

    private static TypeOrDomain buildLayer(PureList<PostfixOpExpr> opTypes)
        throws ReadError {
        return resolveInfix
                   (resolveJuxt
                      (resolvePrefix
                          (resolveTightDiv
                              (resolvePostfix (opTypes)))));
    }

    private static Type typeToType(TypeOrDomain type) {
        return (Type) type.accept(new NodeUpdateVisitor() {
            public Type forExponentType(ExponentType t) {
                return makeMatrixType(t.getSpan(), typeToType(t.getBase()),
                                      t.getPower());
            }
            public Type forDimExpr(DimExpr t) {
                try {
                    return dimToType(t);
                } catch (TypeConvertFailure x) {
                    return (Type)t;
                }
            }
            public Type forTaggedDimType(TaggedDimType t) {
                return new TaggedDimType(t.getSpan(), true,
                                         typeToType(t.getType()),
                                         makeInParentheses(dimToDim(t.getDim())),
                                         t.getUnit());
            }
        });
    }

    private static Domain typeToDomain(TypeOrDomain type) {
        if (type instanceof Domain) { return (Domain) type; }
        else { // type instanceof Type
            List<Type> args;
            if (type instanceof VoidType) {
                args = Collections.emptyList();
            }
            else if (type instanceof TupleType) {
                TupleType tup = (TupleType) type;
                args = new ArrayList<Type>(tup.getElements().size());
                for (Type t : tup.getElements()) {
                    args.add(typeToType(t));
                }
            }
            else {
                args = Collections.singletonList(typeToType(type));
            }
            return new Domain(type.getSpan(), args);
        }
    }

    public static DimExpr typeToDim(TypeOrDomain type) throws TypeConvertFailure {
        try {
            return type.accept(new NodeAbstractVisitor<DimExpr>() {
                public DimExpr forDimExpr(DimExpr t) {
                    return t;
                }
                public DimExpr forExponentType(ExponentType t) {
                    try {
                        return new ExponentDim(t.getSpan(),
                                               t.isParenthesized(),
                                               typeToDim(t.getBase()),
                                               t.getPower());
                    } catch (TypeConvertFailure e) {
                        return error(t, "A dimension is expected but " +
                                     "a type is found.");
                    }
                }
                public DimExpr forMatrixType(MatrixType t) {
                    try {
                        List<ExtentRange> dimensions = t.getDimensions();
                        if ( dimensions.size() != 1)
                            return error(t, "A dimension is expected but " +
                                         "a type is found.");
                        ExtentRange dimension = dimensions.get(0);
                        IntArg power = (IntArg)dimension.getSize().unwrap();
                        return new ExponentDim(t.getSpan(),
                                               t.isParenthesized(),
                                               typeToDim(t.getType()),
                                               power.getVal());
                    } catch (Throwable e) {
                        return error(t, "A dimension is expected but " +
                                     "a type is found.");
                    }
                }
                public DimExpr forTaggedDimType(TaggedDimType t) {
                    try {
                        if (t.getUnit().isNone()) {
                            return new ProductDim(t.getSpan(),
                                                  t.isParenthesized(),
                                                  typeToDim(t.getType()),
                                                  t.getDim());
                        } else
                            return error(t, "A dimension is expected " +
                                         "but a type is found.");
                    } catch (TypeConvertFailure e) {
                        return error(t, "A dimension is expected but " +
                                     "a type is found.");
                    }
                }
                public DimExpr forVarType(VarType t) {
                    return new DimRef(t.getSpan(),
                                      t.isParenthesized(),
                                      t.getName());
                }
                public DimExpr defaultCase(Node x) {
                    return error(x, "A dimension is expected but a " +
                                 "type is found.");
                }
                });
        } catch (ProgramError e) {
            throw new TypeConvertFailure(e.getMessage());
        }
    }

    private static DimExpr dimToDim(DimExpr dim) {
        return dim.accept(new NodeAbstractVisitor<DimExpr>() {
            public DimExpr forExponentType(ExponentType d) {
                try {
                    return new ExponentDim(d.getSpan(),
                                           d.isParenthesized(),
                                           typeToDim(d.getBase()),
                                           d.getPower());
                } catch (TypeConvertFailure x) {
                    return (DimExpr)d;
                }
            }
            public DimExpr forProductDim(ProductDim d) {
                return new ProductDim(d.getSpan(),
                                      d.isParenthesized(),
                                      dimToDim(d.getMultiplier()),
                                      dimToDim(d.getMultiplicand()));
            }
            public DimExpr forQuotientDim(QuotientDim d) {
                return new QuotientDim(d.getSpan(),
                                       d.isParenthesized(),
                                       dimToDim(d.getNumerator()),
                                       dimToDim(d.getDenominator()));
            }
            public DimExpr forOpDim(OpDim d) {
                return new OpDim(d.getSpan(),
                                 d.isParenthesized(),
                                 dimToDim(d.getVal()),
                                 d.getOp());
            }
            public DimExpr defaultCase(Node x) {
                return (DimExpr)x;
            }
        });
    }

    private static Type dimToType(DimExpr dim) throws TypeConvertFailure {
        try {
            return dim.accept(new NodeAbstractVisitor<Type>() {
                public Type forDimRef(DimRef d) {
                    return new VarType(d.getSpan(),
                                       d.isParenthesized(),
                                       d.getName());
                }
                public Type forProductDim(ProductDim d) {
                    try {
                        return new TaggedDimType(d.getSpan(),
                                                 d.isParenthesized(),
                                                 makeInParentheses(dimToType(d.getMultiplier())),
                                                 dimToDim(d.getMultiplicand()));
                    } catch (TypeConvertFailure e) {
                        return error(e.getMessage());
                    }
                }
                public Type forExponentDim(ExponentDim d) {
                    try {
                        return makeMatrixType(d.getSpan(),
                                              dimToType(d.getBase()),
                                              d.getPower());
                    } catch (TypeConvertFailure e) {
                        return error(e.getMessage());
                    }
                }
                public Type forExponentType(ExponentType d) {
                    return makeMatrixType(d.getSpan(), typeToType(d.getBase()),
                                          d.getPower());
                }
                public Type defaultCase(Node x) {
                    return error(x, "A type is expected but a " +
                                 "dimension is found:\n  " + x);
                }
                });
        } catch (ProgramError e) {
            throw new TypeConvertFailure(e.getMessage());
        }
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
            TypeOrDomain type = buildLayer(opTypes);
            if (isVerbose) System.out.println("after resolveOps: " + type);
            try {
                if (type instanceof DimExpr) return dimToType((DimExpr)type);
                else return typeToType(type);
            } catch (TypeConvertFailure x) {
                return (Type)type;
            }
        } catch (Throwable e) {
            String msg = e.getMessage();
            for (PrecedenceOpExpr type : opTypes.toJavaList()) {
                msg += "\n  " + type.toString();
            }
            return error("Resolution of operator property failed for:\n" + msg);
        }
    }

    public static DimExpr resolveOpsDim(PureList<PostfixOpExpr> opTypes) {
        try {
            TypeOrDomain type = buildLayer(opTypes);
            try {
                if (type instanceof DimExpr) return typeToDim((DimExpr)type);
                else return typeToDim(type);
            } catch (TypeConvertFailure x) {
                return (DimExpr)type;
            }
        } catch (Throwable e) {
            String msg = e.getMessage();
            for (PrecedenceOpExpr type : opTypes.toJavaList()) {
                msg += "\n  " + type.toString();
            }
            return error("Resolution of operator property failed for:\n" + msg);
        }
    }
}
