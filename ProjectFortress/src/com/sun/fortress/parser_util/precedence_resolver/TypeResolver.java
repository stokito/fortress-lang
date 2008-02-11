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
import com.sun.fortress.interpreter.evaluator.ProgramError;

import static com.sun.fortress.interpreter.evaluator.ProgramError.error;
import static com.sun.fortress.nodes_util.OprUtil.noColonText;
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
        DimExpr dim = new ProductDim(span, dimToDim(expr0.getDim()),
                                     dimToDim(expr2));
        return new TaggedDimType(span, typeToType(expr0.getType()), dim,
                                 expr0.getUnit());
    }

    private static Type makeQuotientDim(Span span, TaggedDimType expr0,
                                        DimExpr expr2)
        throws TypeConvertFailure {
        DimExpr dim = new QuotientDim(span, dimToDim(expr0.getDim()),
                                      dimToDim(expr2));
        return new TaggedDimType(span, typeToType(expr0.getType()), dim,
                                 expr0.getUnit());
    }

    private static Type makeJuxt(Type first, Type second) throws ReadError {
        Span span = spanTwo(first, second);
        try {
            DimExpr dim = typeToDim(second);
            if (first instanceof TaggedDimType) {
                return makeProductDim(span, (TaggedDimType)first, dim);
            } else {
                return new TaggedDimType(span, typeToType(first), dim);
            }
        } catch (TypeConvertFailure x) {
            throw new ReadError(span, "Misuse of type juxtaposition.");
        }
    }

    public static Type makeMatrixType(Span span, Type type, IntExpr power) {
        StaticArg arg = new IntArg(power.getSpan(), power);
        ExtentRange er = new ExtentRange(Option.<StaticArg>none(),
                                         Option.some(arg));
        return NodeFactory.makeMatrixType(span, typeToType(type), er);
    }

    private static PureList<TypeInfixFrame>
        looseInfixStack(Type e, Op op, Option<List<TraitType>> _throws,
                        PureList<TypeInfixFrame> stack) throws ReadError {
        if (stack.isEmpty()) {
            return PureList.<TypeInfixFrame>make(new TypeLoose(op, _throws, e));
        } else { // !stack.isEmpty()
            Cons<TypeInfixFrame> _stack = (Cons<TypeInfixFrame>)stack;
            TypeInfixFrame frame = _stack.getFirst();
            PureList<TypeInfixFrame> rest = _stack.getRest();

            if (frame instanceof TypeTight) {
                Op _op = ((TypeTight)frame).getOp();
                if (precedence(_op, op) instanceof Higher) {
                    return looseInfixStack(finishInfixFrame(e,frame),op,
                                           _throws, rest);
                } else {
                    throw new ReadError(spanTwo(_op,op),
                                        "Tight operator " + _op.getText() +
                                        " near loose operator " + op.getText()
                                        + " of incompatible precedence.");
                }
            } else { // (frame instanceof TypeLoose)
                Op _op = ((TypeLoose)frame).getOp();
                Type first = ((TypeLoose)frame).getArg();
                Span span = spanTwo(first, e);
                if (op.getText().equals(_op.getText())) {
                    if (isDOT(op)) {
                        try {
                            if (first instanceof TaggedDimType) {
                                Type _new = makeProductDim(span,
                                                           (TaggedDimType)first,
                                                           typeToDim(e));
                                return rest.cons(new TypeLoose(_op,_throws,_new));
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
                        return stack.cons(new TypeLoose(op, _throws, e));
                    } else if (prec instanceof Lower ||
                               prec instanceof Equal) {
                        return looseInfixStack(finishInfixFrame(e,frame), op,
                                               _throws, rest);
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
      tightInfixStack(Type e, Op op, Option<List<TraitType>> _throws,
                      PureList<TypeInfixFrame> stack) throws ReadError {
      if (stack.isEmpty()) {
          return PureList.<TypeInfixFrame>make(new TypeTight(op, _throws, e));
      } else { // !stack.isEmpty()
          Cons<TypeInfixFrame> _stack = (Cons<TypeInfixFrame>)stack;
          TypeInfixFrame frame = _stack.getFirst();
          PureList<TypeInfixFrame> rest = _stack.getRest();

          if (frame instanceof TypeLoose) {
              Op _op = ((TypeLoose)frame).getOp();
              if (precedence(op, _op) instanceof Higher) {
                  return stack.cons(new TypeTight(op, _throws, e));
              } else {
                  throw new ReadError(spanTwo(_op,op),
                                      "Loose operator " + _op.getText() +
                                      " near tight operator " + op.getText()
                                      + " of incompatible precedence.");
              }
          } else { // (frame instanceof TypeTight)
              Op _op = ((TypeTight)frame).getOp();
              Type first = ((TypeTight)frame).getArg();
              Span span = spanTwo(first, e);
              if (op.getText().equals(_op.getText())) {
                  if (isDOT(op)) {
                        try {
                            if (first instanceof TaggedDimType) {
                                Type _new = makeProductDim(span,
                                                           (TaggedDimType)first,
                                                           typeToDim(e));
                                return rest.cons(new TypeTight(_op,_throws,_new));
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
                      return stack.cons(new TypeTight(op, _throws, e));
                  }
                  else if (prec instanceof Lower ||
                           prec instanceof Equal) {
                      return tightInfixStack(finishInfixFrame(e,frame), op,
                                             _throws, rest);
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

    private static Type finishInfixFrame(Type last, TypeInfixFrame frame)
        throws ReadError {
        Op op = frame.getOp();
        Type first = frame.getArg();
        if (isTypeOp(op)) {
            Type domain = NodeFactory.inArrowType(typeToType(first));
            return NodeFactory.makeArrowType(spanTwo(first,last), domain,
                                             typeToType(last),
                                             frame.getThrows());
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
                        return new ProductDim(span, _first, _second);
                    else // op.getText().equals("/") ||
                         // op.getText().equals("per")
                        return new QuotientDim(span, _first, _second);
                    //                    throw new ReadError(op.getSpan(), "DimExpr is expected.");
                }
            } catch (TypeConvertFailure x) {
                throw new ReadError(op.getSpan(), "DimExpr is expected.");
            }

        }
    }

    private static Type finishInfixStack(Type last,
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

    private static Type resolveInfixStack(PureList<InfixOpExpr> opTypes,
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
            Type type = ((RealType)first).getType();
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
                                                ((LooseInfix)second).getThrows(),
                                                stack));
                else if (second instanceof TightInfix)
                    return resolveInfixStack
                        (_rest, tightInfixStack(type,
                                                ((TightInfix)second).getOp(),
                                                ((TightInfix)second).getThrows(),
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

    private static Type resolveInfix(PureList<InfixOpExpr> opTypes)
        throws ReadError {
        if (isVerbose) System.out.println("resolveInfix...");
        return resolveInfixStack(opTypes, PureList.<TypeInfixFrame>make());
    }

    private static PureList<InfixOpExpr> buildJuxt(PureList<InfixOpExpr> opTypes,
                                                   PureList<Type> revTypes)
        throws ReadError {
        if (revTypes.size() < 2) {
            throw new ReadError(new Span(),
                                "Misuse of type/dimension juxtaposition.");
        }
        Object[] prefix = revTypes.toArray(2);
        Type first = (Type)prefix[1];
        Type second = (Type)prefix[0];
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
            Type _first =
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
                        return _rest.cons(new RealType(new OpDim(e.getSpan(), e,
                                                                 op)));
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
                            Type expr0 = ((RealType)prefix[0]).getType();
                            Span span = spanTwo(expr0, expr2);
                            Type e;
                            try {
                                DimExpr _expr0 = typeToDim(expr0);
                                e = new QuotientDim(span,_expr0,expr2);
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
                    DimExpr dim = new OpDim(_first.getSpan(), _first, op);
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

    private static Type buildLayer(PureList<PostfixOpExpr> opTypes)
        throws ReadError {
        return resolveInfix
                   (resolveJuxt
                      (resolvePrefix
                          (resolveTightDiv
                              (resolvePostfix (opTypes)))));
    }

    private static Type typeToType(Type type) {
        return type.accept(new NodeAbstractVisitor<Type>() {
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
            public Type forArrowType(ArrowType t) {
                Type domain = NodeFactory.inArrowType(typeToType(t.getDomain()));
                return new ArrowType(t.getSpan(), domain,
                                     typeToType(t.getRange()),
                                     t.getThrowsClause(), t.isIo());
            }
            public Type forArrayType(ArrayType t) {
                return new ArrayType(t.getSpan(), typeToType(t.getElement()),
                                     t.getIndices());
            }
            public Type forMatrixType(MatrixType t) {
                return new MatrixType(t.getSpan(), typeToType(t.getElement()),
                                      t.getDimensions());
            }
            public Type forArgType(ArgType t) {
                List<Type> elements = new ArrayList<Type>();
                for (Type ty : t.getElements()) {
                    elements.add(typeToType(ty));
                }
                Option<VarargsType> varargs = t.getVarargs();
                if (varargs.isSome()) {
                    VarargsType ty = Option.unwrap(varargs);
                    varargs = Option.some(new VarargsType(ty.getSpan(),
                                                          typeToType(ty.getType())));
                }
                List<KeywordType> keywords = new ArrayList<KeywordType>();
                for (KeywordType ty : t.getKeywords()) {
                    keywords.add(new KeywordType(ty.getSpan(), ty.getName(),
                                                 typeToType(ty.getType())));
                }
                return new ArgType(t.getSpan(), elements, varargs, keywords,
                                   t.isInArrow());
            }
            public Type forTupleType(TupleType t) {
                List<Type> elements = new ArrayList<Type>();
                for (Type ty : t.getElements()) {
                    elements.add(typeToType(ty));
                }
                return new TupleType(t.getSpan(), elements);
            }
            public Type forTaggedDimType(TaggedDimType t) {
                return new TaggedDimType(t.getSpan(), typeToType(t.getType()),
                                         dimToDim(t.getDim()), t.getUnit());
            }
            public Type defaultCase(Node x) {
                return (Type)x;
            }
        });
    }

    public static DimExpr typeToDim(Type type) throws TypeConvertFailure {
        try {
            return type.accept(new NodeAbstractVisitor<DimExpr>() {
                public DimExpr forDimExpr(DimExpr t) {
                    return t;
                }
                public DimExpr forExponentType(ExponentType t) {
                    try {
                        return new ExponentDim(t.getSpan(),
                                               typeToDim(t.getBase()),
                                               t.getPower());
                    } catch (TypeConvertFailure e) {
                        return error(t, "A dimension is expected but " +
                                     "a type is found.");
                    }
                }
                public DimExpr forTaggedDimType(TaggedDimType t) {
                    try {
                        if (t.getUnit().isNone()) {
                            return new ProductDim(t.getSpan(),
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
                public DimExpr forIdType(IdType t) {
                    return new DimRef(t.getSpan(), t.getName());
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
                    return new ExponentDim(d.getSpan(), typeToDim(d.getBase()),
                                           d.getPower());
                } catch (TypeConvertFailure x) {
                    return (DimExpr)d;
                }
            }
            public DimExpr forProductDim(ProductDim d) {
                return new ProductDim(d.getSpan(), dimToDim(d.getMultiplier()),
                                      dimToDim(d.getMultiplicand()));
            }
            public DimExpr forQuotientDim(QuotientDim d) {
                return new QuotientDim(d.getSpan(), dimToDim(d.getNumerator()),
                                       dimToDim(d.getDenominator()));
            }
            public DimExpr forOpDim(OpDim d) {
                return new OpDim(d.getSpan(), dimToDim(d.getVal()),
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
                    return new IdType(d.getSpan(), d.getName());
                }
                public Type forProductDim(ProductDim d) {
                    try {
                        return new TaggedDimType(d.getSpan(),
                                                 dimToType(d.getMultiplier()),
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
            Type type = buildLayer(opTypes);
            if (isVerbose) System.out.println("after resolveOps: " + type);
            try {
                if (type instanceof DimExpr) return dimToType((DimExpr)type);
                else if (type instanceof ArgType) {
                    ArgType ty = (ArgType)type;
                    if (ty.getVarargs().isNone() && ty.getKeywords().isEmpty())
                        return typeToType(new TupleType(ty.getSpan(),
                                                        ty.isParenthesized(),
                                                        ty.getElements()));
                    else return typeToType(type);
                    /*
                    else return error(type, "Tuple types are not allowed to " +
                                      "have varargs or keyword types.");
                    */
                } else
                    return typeToType(type);
            } catch (TypeConvertFailure x) {
                return type;
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
            Type type = buildLayer(opTypes);
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
