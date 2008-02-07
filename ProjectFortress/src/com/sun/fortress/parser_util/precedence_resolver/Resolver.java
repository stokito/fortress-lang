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
 * Resolving operator precedence during parsing.
 */
package com.sun.fortress.parser_util.precedence_resolver;

import java.util.ArrayList;
import java.util.List;

import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.Op;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.parser_util.FortressUtil;
import com.sun.fortress.parser_util.precedence_opexpr.Chain;
import com.sun.fortress.parser_util.precedence_opexpr.Equal;
import com.sun.fortress.parser_util.precedence_opexpr.Higher;
import com.sun.fortress.parser_util.precedence_opexpr.InfixFrame;
import com.sun.fortress.parser_util.precedence_opexpr.InfixOpExpr;
import com.sun.fortress.parser_util.precedence_opexpr.JuxtInfix;
import com.sun.fortress.parser_util.precedence_opexpr.Left;
import com.sun.fortress.parser_util.precedence_opexpr.Loose;
import com.sun.fortress.parser_util.precedence_opexpr.LooseChain;
import com.sun.fortress.parser_util.precedence_opexpr.LooseInfix;
import com.sun.fortress.parser_util.precedence_opexpr.LoosePrefix;
import com.sun.fortress.parser_util.precedence_opexpr.Lower;
import com.sun.fortress.parser_util.precedence_opexpr.NonChain;
import com.sun.fortress.parser_util.precedence_opexpr.PrecedenceOpExpr;
import com.sun.fortress.parser_util.precedence_opexpr.Postfix;
import com.sun.fortress.parser_util.precedence_opexpr.PostfixOpExpr;
import com.sun.fortress.parser_util.precedence_opexpr.Precedence;
import com.sun.fortress.parser_util.precedence_opexpr.Prefix;
import com.sun.fortress.parser_util.precedence_opexpr.PrefixOpExpr;
import com.sun.fortress.parser_util.precedence_opexpr.RealExpr;
import com.sun.fortress.parser_util.precedence_opexpr.Right;
import com.sun.fortress.parser_util.precedence_opexpr.Tight;
import com.sun.fortress.parser_util.precedence_opexpr.TightChain;
import com.sun.fortress.parser_util.precedence_opexpr.TightInfix;
import com.sun.fortress.parser_util.precedence_opexpr.TightPrefix;
import com.sun.fortress.useful.Cons;
import com.sun.fortress.useful.Fn;
import com.sun.fortress.useful.Pair;
import com.sun.fortress.useful.PureList;

import static com.sun.fortress.interpreter.evaluator.ProgramError.error;
import static com.sun.fortress.nodes_util.OprUtil.noColonText;

/*
 * This class implements the functionality for resolving operator precedence during parsing.
 * Resolution is performed via pattern matching along with an auxillary stack.
 * Above each method is a description of the method's functionality in O'Caml.
 */
public class Resolver {

  private static boolean noSpace = false;

  // let is_div op = op.node_data = "/"
  private static boolean isDiv(Op op) {
      return op.getText().equals("/");
  }

  private static boolean isNonAssociative(Op op) {
      return PrecedenceMap.ONLY.isNonAssociative(op.getText());
  }

  // (* A predicate for whether an operator may participate in chaining *)
  // let chains (op : op) : bool =
  //   List.exists (fun set -> OpSet.mem op.node_data set)
  //     [ equivalence_operators; less_than_operators; greater_than_operators;
  //       subset_comparison_operators; superset_comparison_operators;
  //       square_original_of; square_image_of;
  //       curly_precedes; curly_succeeds;
  //       chickenfoot_greater; chickenfoot_smaller;
  //       tri_subgroup; tri_contains;
  //       ]
  private static boolean chains(Op op) {
      return PrecedenceMap.ONLY.isChain(op.getText());
  }

  // let precedence (one : op) (two : op) : precedence =
  //   let one = one.node_data
  //   and two = two.node_data in
  //     if one = two then `Equal else
  //       match OpPairMap.find_opt (one,two) precedences with
  //         | Some (`Higher | `Lower | `Equal as prec) -> prec
  //         | None -> `None
  private static Precedence precedence(Op op1, Op op2) {
      String op1name = noColonText(op1);
      String op2name = noColonText(op2);
      return PrecedenceMap.ONLY.get(op1name, op2name);
  }

  //(* Given a list of exprs and ops, ensure that the ops are a valid chaining *)
  // let ensure_valid_chaining (links : (expr * op) list) : unit =
  // let node_list = List.map (fun (e,op) -> op) links in
  // let op_list = List.map (fun op -> op.node_data) node_list in
  // let op_set = set op_list in
  // if List.exists (fun chain_set -> OpSet.subset op_set chain_set) chain_sets
  //   then ()
  //   else
  //      Errors.read_error (span_all node_list)
  //       "Incompatible chaining operators:\n%s" (ops_to_string op_list)
    private static void ensureValidChaining(PureList<ExprOpPair> links)
        throws ReadError {
        PureList<String> names = links.map(new Fn<ExprOpPair,String>() {
            public String apply(ExprOpPair p) { return p.getB().getText(); }
        });

        if (!PrecedenceMap.ONLY.isValidChaining(names.toJavaList())) {
            PureList<Op> ops = links.map(new Fn<ExprOpPair,Op>() {
                public Op apply(ExprOpPair pair) { return pair.getB(); }
            });

            throw new ReadError(FortressUtil.spanAll(ops.toArray(), ops.size()),
                                "Incompatible chaining operators.");
        }
    }

  // let rec resolve_postfix (oes : postfix_opexpr list) : prefix_opexpr list =
  //   match oes with
  //     | [] -> []
  //     | `Expr e :: `Postfix op :: rest ->
  //         resolve_postfix (`Expr (Util.postfix e.node_span e op) :: rest)
  //     | `Postfix op :: rest ->
  //         Errors.internal_error None
  //           "Postfix operator %s without argument." op.node_data
  //     | ((`Expr _ | `Prefix _ | `TightInfix _ | `LooseInfix _) as oe) :: rest ->
  //         oe :: resolve_postfix rest
    private static PureList<PrefixOpExpr> resolvePostfix(PureList<PostfixOpExpr> opExprs)
        throws ReadError
    {
        if (opExprs.isEmpty()) { return PureList.<PrefixOpExpr>make(); }

        else {
            Cons<PostfixOpExpr> _opExprs = (Cons<PostfixOpExpr>) opExprs;
            PostfixOpExpr first = _opExprs.getFirst();
            PureList<PostfixOpExpr> rest = _opExprs.getRest();

            if (first instanceof RealExpr && !(rest.isEmpty()) &&
                ((Cons<PostfixOpExpr>)rest).getFirst() instanceof Postfix)
                {
                    Expr _first = ((RealExpr)first).getExpr();
                    Cons<PostfixOpExpr> _rest = (Cons<PostfixOpExpr>)rest;
                    Op op = ((Postfix)(_rest.getFirst())).getOp();
                    PureList<PostfixOpExpr> restRest = _rest.getRest();
                    Expr expr = ASTUtil.postfix(_first.getSpan(), _first, op);

                    return resolvePostfix(restRest.cons(new RealExpr(expr)));
                }
            else if (first instanceof Postfix) {
                throw new ReadError(((Postfix)first).getOp().getSpan(),
                                    "Postfix operator %s without argument.");
            }
            else { // first instanceof PrefixOpExpr
                return (resolvePostfix(rest)).cons((PrefixOpExpr)first);
            }
        }
    }

  // let rec resolve_tight_div (oes : prefix_opexpr list) : prefix_opexpr list =
  //   match oes with
  //  | [] -> []
  //  | `Expr _ :: `TightInfix op1 :: `Expr _ :: `TightInfix op2 :: _
  //         when is_div op1 && is_div op2 ->
  //         Errors.read_error (span_two op1 op2)
  //           "Tight division (/) does not associate."
  //  | `Expr e1 :: `TightInfix op :: `Expr e2 :: rest when is_div op ->
  //         resolve_tight_div (`Expr (Util.infix (span_two e1 e2) e1 op e2) :: rest\
  //       )
  //  | `TightInfix op :: _ when is_div op ->
  //         Errors.read_error op.node_span "Misuse of tight division."
  //  | opexpr :: rest -> opexpr :: resolve_tight_div rest
  private static PureList<PrefixOpExpr>
    resolveNonAssociative(PureList<PrefixOpExpr> opExprs) throws ReadError
  {
    if (opExprs.isEmpty()) { return PureList.<PrefixOpExpr>make(); }

    else { // !opExprs.isEmpty()
      Cons<PrefixOpExpr> _opExprs = (Cons<PrefixOpExpr>)opExprs;
      PrefixOpExpr first  = _opExprs.getFirst();
      PureList<PrefixOpExpr> rest  = _opExprs.getRest();

      if (opExprs.size() >= 4) {
        Object[] prefix = opExprs.toArray(4);
        PureList<PrefixOpExpr> _rest  = ((Cons<PrefixOpExpr>)rest).getRest();
        PureList<PrefixOpExpr> __rest  = ((Cons<PrefixOpExpr>)_rest).getRest();

        if (prefix[0] instanceof RealExpr &&
            prefix[1] instanceof JuxtInfix &&
            prefix[2] instanceof RealExpr &&
            prefix[3] instanceof JuxtInfix)
        {
          Op op1 = ((JuxtInfix)prefix[1]).getOp();
          Op op3 = ((JuxtInfix)prefix[3]).getOp();

          if (isNonAssociative(op1) && isNonAssociative(op3)) {
              throw new ReadError(FortressUtil.spanTwo(op1,op3), op1.getText() +
                                  " does not associate.");
          }
        }
      }
      if (opExprs.size() >= 3) {
        Object[] prefix = opExprs.toArray(3);
        PureList<PrefixOpExpr> _rest  = ((Cons<PrefixOpExpr>)rest).getRest();
        PureList<PrefixOpExpr> __rest  = ((Cons<PrefixOpExpr>)_rest).getRest();

        if (prefix[0] instanceof RealExpr &&
            prefix[1] instanceof TightInfix &&
            prefix[2] instanceof RealExpr)
        {
          Expr expr0 = ((RealExpr)prefix[0]).getExpr();
          Op op1     = ((TightInfix)prefix[1]).getOp();
          Expr expr2 = ((RealExpr)prefix[2]).getExpr();

          if(isDiv(op1)) {
            Span span = FortressUtil.spanTwo(expr0, expr2);
            RealExpr e = new RealExpr(ASTUtil.infix(span, expr0, op1, expr2));

            return resolveNonAssociative(__rest.cons(e));
          }
        }
      }
      if (first instanceof TightInfix &&
          isDiv(((TightInfix)first).getOp())) {
        throw new ReadError(((TightInfix)first).getOp().getSpan(),
                            "Misuse of " + ((TightInfix)first).getOp().getText()
                            + ".");
      }
      else {
        return (resolveNonAssociative(rest)).cons(first);
      }
    }
  }



  // let rec resolve_prefix (oes : prefix_opexpr list) : infix_opexpr list =
  //   match oes with
  //     | [] -> []
  //  | `Prefix op :: rest ->
  //         (match resolve_prefix rest with
  //   | `Expr e :: rest' ->
  //                `Expr (Util.prefix e.node_span op e) :: rest'
  //            | _ ->
  //                Errors.internal_error None
  //   "Prefix operator %s without argument." op.node_data)
  //  | ((`Expr _ | `TightInfix _ | `LooseInfix _) as oe) :: rest ->
  //         oe :: resolve_prefix rest
  private static PureList<InfixOpExpr> resolvePrefix(PureList<PrefixOpExpr> opExprs)
    throws ReadError
  {
    if (opExprs.isEmpty()) { return PureList.<InfixOpExpr>make(); }

    else { // !opExprs.isEmpty()
      PrefixOpExpr first = ((Cons<PrefixOpExpr>)opExprs).getFirst();
      PureList<PrefixOpExpr> rest = ((Cons<PrefixOpExpr>)opExprs).getRest();

      if (first instanceof Prefix) {
        PureList<InfixOpExpr> _opExprs = resolvePrefix(rest);
        if (!_opExprs.isEmpty() &&
            ((Cons<InfixOpExpr>)_opExprs).getFirst() instanceof RealExpr) {
          Cons<InfixOpExpr> __opExprs = (Cons<InfixOpExpr>)_opExprs;
          Expr e = ((RealExpr)__opExprs.getFirst()).getExpr();
          PureList<InfixOpExpr> _rest = __opExprs.getRest();
          if (!_rest.isEmpty()) {
              InfixOpExpr third = ((Cons<InfixOpExpr>)_rest).getFirst();
              Op op = ((Prefix)first).getOp();
              if (first instanceof LoosePrefix &&
                  third instanceof TightInfix) {
                  Op _op = ((TightInfix)third).getOp();
                  throw new ReadError(op.getSpan(), "Loose prefix operator " +
                                      op.toString() + " near tight operator " +
                                      _op.toString() + ".");
              } else if (first instanceof TightPrefix &&
                         third instanceof RealExpr && !noSpace) {
                  throw new ReadError(op.getSpan(),
                                      "Prefix operator " + op.toString() +
                                      " near loose juxtaposition.");
              }
          }
          return _rest.cons(new RealExpr
                              (ASTUtil.prefix(((Prefix)first).getOp(),
                                              e)));
        } else {
          Op op = ((Prefix)first).getOp();
          throw new ReadError(op.getSpan(),
                              "Prefix operator " + op.toString() +
                              " without argument.");
        }
      } else { // first isinstanceof InfixOpExpr
        return (resolvePrefix(rest)).cons((InfixOpExpr)first);
      }
    }
  }

  // let rec resolve_juxt (oes : infix_opexpr list) : infix_opexpr list =
  //   let rec build_juxt oes rev_exprs =
  //     match oes with
  //        | `TightInfix op :: rest ->
  //        Errors.read_error (span_all (List.rev rev_exprs))
  //             "Precedence mismatch: juxtaposition and %s." op.node_data
  //        | `Expr e :: rest -> build_juxt rest (e :: rev_exprs)
  //       | _ ->
  //        `Expr (Util.loose (List.rev rev_exprs)) :: find_juxt oes
  //   and find_juxt oes =
  //     match oes with
  //       | [] -> []
  //        | `TightInfix op :: `Expr e :: `Expr e' :: rest ->
  //           Errors.read_error (span_two e e')
  //             "Precedence mismatch: %s and juxtaposition." op.node_data
  //   | `Expr e :: `Expr e' :: rest -> build_juxt rest [e';e]
  //     | oe :: rest -> oe :: find_juxt rest
  //   in
  //     find_juxt oes
  private static PureList<PrefixOpExpr> resolveJuxt(PureList<PrefixOpExpr> opExprs)
    throws ReadError
  {
    return findJuxt(opExprs);
  }

  private static PureList<PrefixOpExpr> findJuxt(PureList<PrefixOpExpr> opExprs)
    throws ReadError
  {
    if (opExprs.isEmpty()) { return PureList.<PrefixOpExpr>make(); }

    else { // opExprs instanceof Cons
      Cons<PrefixOpExpr> _opExprs = (Cons<PrefixOpExpr>) opExprs;
      PrefixOpExpr first = _opExprs.getFirst();
      PureList<PrefixOpExpr> rest = _opExprs.getRest();

      if (opExprs.size() >= 3) {
        Object[] prefix = opExprs.toArray(3);

        if (prefix[0] instanceof TightInfix &&
            prefix[1] instanceof RealExpr &&
            prefix[2] instanceof RealExpr)
        {
          Span span = FortressUtil.spanTwo
            (((RealExpr)prefix[1]).getExpr(),
             ((RealExpr)prefix[2]).getExpr());
          throw new ReadError(span,
                              "Precedence mismatch: " +
                              ((TightInfix)prefix[0]).getOp().toString() +
                              " and juxtaposition.");
        }
      }
      if (opExprs.size() >= 2) {
        PrefixOpExpr second = ((Cons<PrefixOpExpr>)rest).getFirst();
        PureList<PrefixOpExpr> _rest = ((Cons<PrefixOpExpr>)rest).getRest();
        if (first  instanceof RealExpr &&
            second instanceof RealExpr) {
          return buildJuxt(_rest, PureList.make((RealExpr)second,
                                            (RealExpr)first));
        }
      }
      return (findJuxt(rest)).cons(first);
    }
  }


  private static PureList<PrefixOpExpr> buildJuxt(PureList<PrefixOpExpr> opExprs,
                         PureList<RealExpr> revExprs)
    throws ReadError
  {
    if (!opExprs.isEmpty() &&
        ((Cons<PrefixOpExpr>)opExprs).getFirst() instanceof TightInfix)
    {
      TightInfix first =
        (TightInfix)((Cons<PrefixOpExpr>)opExprs).getFirst();
      Span span = ASTUtil.spanAll(revExprs.reverse());
      throw new ReadError(span,
                          "Precedence mismatch: juxtaposition and " +
                          first.getOp().toString() + ".");
    }
    else if (!opExprs.isEmpty() &&
               ((Cons<PrefixOpExpr>)opExprs).getFirst() instanceof RealExpr) {
      RealExpr first =
        (RealExpr)((Cons<PrefixOpExpr>)opExprs).getFirst();
      PureList<PrefixOpExpr> rest = ((Cons<PrefixOpExpr>)opExprs).getRest();

      return buildJuxt(rest, revExprs.cons(first));
    }
    else {
      RealExpr e = new RealExpr(ASTUtil.loose(revExprs.reverse()));

      return (findJuxt(opExprs)).cons(e);
    }
  }

  // let rec resolve_infix (oes : infix_opexpr list) : expr =
  //   resolve_infix_stack oes []
  private static Expr resolveInfix(PureList<InfixOpExpr> opExprs)
    throws ReadError
  {
    return resolveInfixStack(opExprs, PureList.<InfixFrame>make());
  }

  // and resolve_infix_stack (oes : infix_opexpr list) (stack : infix_stack) : expr =
  //   match oes with
  //     | [`Expr e] -> finish_infix_stack e stack
  //     (* Chaining *)
  //     | `Expr e :: `LooseInfix op :: (_ :: _ as rest) when chains op ->
  //         resolve_infix_stack rest (loose_chain_stack e op stack)
  //     | `Expr e :: `TightInfix op :: (_ :: _ as rest) when chains op ->
  //         resolve_infix_stack rest (tight_chain_stack e op stack)
  //     (* Regular operators *)
  //     | `Expr e :: `LooseInfix op :: (_ :: _ as rest) ->
  //         resolve_infix_stack rest (loose_infix_stack e op stack)
  //     | `Expr e :: `TightInfix op :: (_ :: _ as rest) ->
  //         resolve_infix_stack rest (tight_infix_stack e op stack)
  //     (* Errors *)
  //     | [] -> Errors.read_error None "Empty juxtaposition/operation expression."
  //     | (`TightInfix op | `LooseInfix op) :: _ ->
  //         Errors.internal_error op.node_span
  //           "Interpreted %s with no left operand as infix." op.node_data
  //     | `Expr e1 :: `Expr e2 :: _ ->
  //         Errors.internal_error (span_two e1 e2)
  //           "Failed to process juxtaposition."
  //     | [`Expr _; (`TightInfix op | `LooseInfix op)] ->
  //         Errors.internal_error op.node_span
  //           "Interpreted %s with no right operand as infix." op.node_data
  private static Expr resolveInfixStack(PureList<InfixOpExpr> opExprs,
                    PureList<InfixFrame> stack)
    throws ReadError {
    if (opExprs.size() == 1 &&
        ((Cons<InfixOpExpr>)opExprs).getFirst() instanceof RealExpr)
    {
      return finishInfixStack
        (((RealExpr)((Cons<InfixOpExpr>)opExprs).getFirst()).getExpr(),
         stack);
    }
    // Chaining
    if (opExprs.size() >= 3) {
      InfixOpExpr first = ((Cons<InfixOpExpr>)opExprs).getFirst();
      Cons<InfixOpExpr> rest =
        (Cons<InfixOpExpr>)
           ((Cons<InfixOpExpr>)opExprs)
        .getRest();
      InfixOpExpr second = ((Cons<InfixOpExpr>)rest).getFirst();
      PureList<InfixOpExpr> _rest = ((Cons<InfixOpExpr>)rest).getRest();

      if (first  instanceof RealExpr &&
          second instanceof LooseInfix &&
          chains(((LooseInfix)second).getOp())) {
        return resolveInfixStack
          (_rest, looseChainStack(((RealExpr)first).getExpr(),
                                  ((LooseInfix)second).getOp(),
                                  stack));
      }
      else if (first  instanceof RealExpr &&
               second instanceof TightInfix &&
               chains(((TightInfix)second).getOp()))
      {
        return resolveInfixStack
          (_rest, tightChainStack(((RealExpr)first).getExpr(),
                                  ((TightInfix)second).getOp(),
                                  stack));

      }
      // Regular operators
      else if (first  instanceof RealExpr &&
               second instanceof LooseInfix)
      {
        return resolveInfixStack
          (_rest, looseInfixStack(((RealExpr)first).getExpr(),
                                  ((LooseInfix)second).getOp(),
                                  stack));
      }
      else if (first  instanceof RealExpr &&
               second instanceof TightInfix) {
        return resolveInfixStack
          (_rest, tightInfixStack(((RealExpr)first).getExpr(),
                                  ((TightInfix)second).getOp(),
                                  stack));
      }
    }
    // Errors
    if (opExprs.isEmpty()) {
      throw new ReadError(new Span(),
                          "Empty juxtaposition/operation expression.");
    }
    else { // !opExprs.isEmpty()
      InfixOpExpr first = ((Cons<InfixOpExpr>)opExprs).getFirst();
      if (first instanceof JuxtInfix) {
        Op op = ((JuxtInfix)first).getOp();
        throw new ReadError(op.getSpan(),
                            "Interpreted " + op.getText() +
                            " with no left operand as infix.");
      }
      else { // first instanceof RealExpr
        PureList<InfixOpExpr> rest = ((Cons<InfixOpExpr>)opExprs).getRest();
        if (rest.isEmpty()) {
            throw new ReadError(((RealExpr)first).getExpr().getSpan(),
                                "Nonexhaustive pattern matching.");
        }
        else { // !rest.isEmpty()
          Cons<InfixOpExpr> _rest = (Cons<InfixOpExpr>)rest;
          InfixOpExpr second = _rest.getFirst();

          if (second instanceof RealExpr) {
            Span span = FortressUtil.spanTwo
              (((RealExpr)first).getExpr(),
               ((RealExpr)second).getExpr());

            throw new ReadError(span, "Failed to process juxtaposition.");
          }
          else { // second instanceof JuxtInfix
            Op op = ((JuxtInfix)second).getOp();
            throw new ReadError(op.getSpan(),
                                "Interpreted " + op.getText() +
                                " with no right operand as infix.");
          }
        }
      }
    }
  }

  // and finish_infix_stack (last : expr) (stack : infix_stack) : expr =
  //   match stack with
  //     | [] -> last
  //     | frame :: rest ->
  //         finish_infix_stack (finish_infix_frame last frame) rest
  private static Expr finishInfixStack(Expr last, PureList<InfixFrame> stack)
    throws ReadError
  {
    if (stack.isEmpty()) { return last; }

    else { // !stack.isEmpty()
      InfixFrame frame = ((Cons<InfixFrame>)stack).getFirst();
      PureList<InfixFrame> rest = ((Cons<InfixFrame>)stack).getRest();

      return finishInfixStack(finishInfixFrame(last, frame), rest);
    }
  }

  // and finish_infix_frame (last : expr) (frame : infix_frame) : expr =
  //   match frame with
  //     | `Loose (op,es) | `Tight (op,es) ->
  //         let args = List.rev (last :: es) in
  //           Util.multifix (span_all args) op args
  //     | `TightChain links | `LooseChain links ->
  //         ensure_valid_chaining links;
  //         build_chain_expr (List.rev links) last
  private static Expr finishInfixFrame(Expr last, InfixFrame frame)
    throws ReadError
  {
    if (frame instanceof NonChain) {
      // frame instanceof Loose || frame instanceof Tight
      Op op = ((NonChain)frame).getOp();
      PureList<Expr> exprs = ((NonChain)frame).getExprs();
      PureList<Expr> args = exprs.cons(last).reverse();

      return ASTUtil.multifix(FortressUtil.spanAll(exprs.toArray(),exprs.size()),
                              op, args.toJavaList());
    }
    else { // frame instanceof TightChain || frame instanceof LooseChain
      PureList<ExprOpPair> links = ((Chain)frame).getLinks();
      ensureValidChaining(links);

      return buildChainExpr(links.reverse(), last);
    }
  }


  // and build_chain_expr (links : (expr * op) list) (last : expr) : expr =
  //   match links with
  //  | (first,op) :: rest ->
  //         Util.chain (span_two first last)
  //  first (build_links op rest last)
  //     | [] -> Errors.internal_error last.node_span "Empty chain expression."
  private static Expr buildChainExpr(PureList<ExprOpPair> links,
                     Expr last) throws ReadError
  {
    if (!links.isEmpty()) {
      ExprOpPair link = ((Cons<ExprOpPair>)links).getFirst();
      PureList<ExprOpPair> rest = ((Cons<ExprOpPair>)links).getRest();
      Expr first = link.getA();
      Op op = link.getB();
      Span span = FortressUtil.spanTwo(first, last);

      return ASTUtil.chain(span, first, buildLinks(op, rest, last).toJavaList());
    }
    else { // links.isEmpty()
      throw new ReadError(last.getSpan(), "Empty chain expression.");
    }
  }

  // and build_links (first : op) (links : (expr * op) list) (last : expr)
  //     : (op * expr) list =
  //   match links with
  //     | [] -> [(first,last)]
  //     | (e,op) :: rest -> (first,e) :: build_links op rest last
  private static PureList<Pair<Op, Expr>>
      buildLinks(Op first, PureList<ExprOpPair> links, Expr last)
  {
    if (links.isEmpty()) {
        return PureList.<Pair<Op,Expr>>make(new Pair<Op,Expr>(first,last));
    }
    else { // !links.isEmpty()
      Cons<ExprOpPair> _links = (Cons<ExprOpPair>)links;
      ExprOpPair link = _links.getFirst();
      Pair<Op,Expr> l = new Pair<Op,Expr>(first, link.getA());

      return (buildLinks(link.getB(), _links.getRest(), last)).cons(l);
    }
  }


  // and loose_infix_stack (e : expr) (op : op) (stack : infix_stack) : infix_stack =
  //   match stack with
  //     | [] -> [`Loose (op,[e])]
  //     | (`Tight (op',es) as frame) :: rest ->
  //         (match precedence op' op with
  //            | `Higher ->
  //                loose_infix_stack (finish_infix_frame e frame) op rest
  //            | _ ->
  //                Errors.read_error (span_two op' op)
  //                  "Tight operator %s near loose operator %s of incompatible precedence."
  //                  op'.node_data op.node_data)
  //     | (`Loose (op',es) as frame) :: rest ->
  //         if op.node_data = op'.node_data
  //         then `Loose (op',e::es) :: rest
  //         else
  //           (match precedence op op' with
  //              | `Higher -> `Loose (op,[e]) :: stack
  //              | `Lower | `Equal ->
  //                  loose_infix_stack (finish_infix_frame e frame) op rest
  //              | `None ->
  //                  Errors.read_error (span_two op' op)
  //                    "Loose operators %s and %s have incomparable precedence."
  //                    op'.node_data op.node_data)
  //     | (`LooseChain ((_,op') :: _) as frame) :: rest ->
  //         (match precedence op op' with
  //            | `Higher -> `Loose (op,[e]) :: stack
  //            | `Lower -> loose_infix_stack (finish_infix_frame e frame) op rest
  //            | `Equal ->
  //                Errors.internal_error op.node_span
  //                  "Chaining operator %s not parsed as such." op.node_data
  //            | `None ->
  //                Errors.read_error (span_two op' op)
  //                  "Loose operators %s and %s have incomparable precedence."
  //                  op'.node_data op.node_data)
  //     | (`TightChain ((_,op') :: _) as frame) :: rest ->
  //         (match precedence op' op with
  //            | `Higher -> loose_infix_stack (finish_infix_frame e frame) op rest
  //            | _ ->
  //                Errors.read_error (span_two op' op)
  //                  "Tight operator %s near loose operator %s of incompatible precedence."
  //                  op'.node_data op.node_data)
  //     | (`LooseChain [] | `TightChain []) :: rest ->
  //         Errors.internal_error op.node_span "Empty chain expression."
  private static PureList<InfixFrame> looseInfixStack(Expr e, Op op,
                          PureList<InfixFrame> stack)
    throws ReadError
  {
    if (stack.isEmpty()) {
      return PureList.<InfixFrame>make(new Loose(op,PureList.make(e)));
    }
    else { // !stack.isEmpty()
      Cons<InfixFrame> _stack = (Cons<InfixFrame>)stack;
      InfixFrame frame = _stack.getFirst();
      PureList<InfixFrame> rest = _stack.getRest();

      if (frame instanceof Tight) {
        Op _op = ((Tight)frame).getOp();

        if (precedence(_op, op) instanceof Higher) {
          return looseInfixStack(finishInfixFrame(e,frame),op,rest);
        }
        else {
          throw new ReadError(FortressUtil.spanTwo(_op,op),
                              "Tight operator " + _op.getText() +
                              " near loose operator " + op.getText() + ".");
        }
      }
      else if (frame instanceof Loose) {
        Op _op = ((Loose)frame).getOp();
        PureList<Expr> exprs = ((Loose)frame).getExprs();

        if (op.getText().equals(_op.getText())) {
          return rest.cons(new Loose(_op, exprs.cons(e)));
        }
        else {
          Precedence prec = precedence(op, _op);

          if (prec instanceof Higher) {
            return stack.cons(new Loose(op, PureList.make(e)));
          }
          else if (prec instanceof Lower ||
                   prec instanceof Equal)
          {
            return looseInfixStack(finishInfixFrame(e,frame),
                                   op, rest);
          }
          else { // prec instanceof None
            throw new ReadError(FortressUtil.spanTwo(_op, op),
                                "Loose operators " + _op.getText()
                                  + " and " + op.getText() +
                                " have incomparable precedence.");
          }
        }
      }
      else if (frame instanceof LooseChain) {
        PureList<ExprOpPair> links = ((LooseChain)frame).getLinks();
        Op _op;

        if (links.isEmpty()) {
          throw new ReadError(op.getSpan(), "Empty chain expression.");
        }
        else {
          _op = ((Cons<ExprOpPair>)links).getFirst().getB();
        }
        Precedence prec = precedence(op, _op);
        if (prec instanceof Higher) {
          return stack.cons(new Loose(op, PureList.make(e)));
        }
        else if (prec instanceof Lower) {
          return looseInfixStack(finishInfixFrame(e,frame),op,rest);
        }
        else if (prec instanceof Equal) {
          throw new ReadError(op.getSpan(),
                              "Chaining operator " + op.getText() +
                              " not parsed as such.");
        }
        else { // prec instanceof None
          throw new ReadError(FortressUtil.spanTwo(_op, op),
                              "Loose operators " + _op.getText()
                                + " and " + op.getText() +
                              " have incomparable precedence.");
        }
      }
      else { // frame instanceof TightChain
        PureList<ExprOpPair> links = ((TightChain)frame).getLinks();
        if (links.isEmpty()) {
          throw new ReadError(op.getSpan(), "Empty chain expression.");
        }
        else {
          Op _op = ((Cons<ExprOpPair>)links).getFirst().getB();
          Precedence prec = precedence(_op, op);

          if (prec instanceof Higher) {
            return looseInfixStack(finishInfixFrame(e,frame),
                                   op, rest);
          }
          else {
            throw new ReadError(FortressUtil.spanTwo(_op,op),
                                "Tight operator " + _op.getText() +
                                " near loose operator " +
                                op.getText() + ".");
          }
        }
      }
    }
  }

  // and tight_infix_stack (e : expr) (op : op) (stack : infix_stack) : infix_stack =
  //   match stack with
  //     | [] -> [`Tight (op,[e])]
  //     | `Loose (op',_) :: _ ->
  //         (match precedence op op' with
  //            | `Higher -> `Tight (op,[e]) :: stack
  //            | _ ->
  //                Errors.read_error (span_two op' op)
  //                  "Loose operator %s near tight operator %s of incompatible precedence."
  //                  op'.node_data op.node_data)
  //     | (`Tight (op',es) as frame) :: rest ->
  //         if op.node_data = op'.node_data
  //         then `Tight (op',e::es) :: rest
  //         else
  //           (match precedence op op' with
  //              | `Higher -> `Tight (op,[e]) :: stack
  //              | `Lower | `Equal ->
  //                  tight_infix_stack (finish_infix_frame e frame) op rest
  //              | `None ->
  //                  Errors.read_error (span_two op' op)
  //                    "Tight operators %s and %s have incomparable precedence."
  //                    op'.node_data op.node_data)
  //     | (`TightChain ((_,op') :: _) as frame) :: rest ->
  //         (match precedence op op' with
  //            | `Higher -> `Tight (op,[e]) :: stack
  //            | `Lower -> tight_infix_stack (finish_infix_frame e frame) op rest
  //            | `Equal ->
  //                Errors.internal_error op.node_span
  //                  "Chaining operator %s not parsed as such." op.node_data
  //            | `None ->
  //                Errors.read_error (span_two op' op)
  //                  "Tight operators %s and %s have incomparable precedence."
  //                  op'.node_data op.node_data)
  //     | (`LooseChain ((_,op') :: _) as frame) :: rest ->
  //         (match precedence op op' with
  //            | `Higher -> `Tight (op,[e]) :: stack
  //            | _ ->
  //                Errors.read_error (span_two op' op)
  //                  "Loose operator %s near tight operator %s of incompatible precedence."
  //                  op'.node_data op.node_data)
  //     | (`LooseChain [] | `TightChain []) :: rest ->
  //         Errors.internal_error op.node_span "Empty chain expression."
  private static PureList<InfixFrame> tightInfixStack(Expr e, Op op,
                          PureList<InfixFrame> stack)
    throws ReadError
  {
    if (stack.isEmpty()) {
      return PureList.<InfixFrame>make(new Tight(op, PureList.make(e)));
    }
    else { // !stack.isEmpty()
      Cons<InfixFrame> _stack = (Cons<InfixFrame>)stack;
      InfixFrame frame = _stack.getFirst();
      PureList<InfixFrame> rest = _stack.getRest();

      if (frame instanceof Loose) {
        Op _op = ((Loose)frame).getOp();

        if (precedence(op, _op) instanceof Higher) {
          return stack.cons(new Tight(op, PureList.make(e)));
        }
        else {
          throw new ReadError(FortressUtil.spanTwo(_op,op),
                              "Loose operator " + _op.getText() +
                              " near tight operator " + op.getText() + ".");
        }
      }
      else if (frame instanceof Tight) {
        Op _op = ((Tight)frame).getOp();
        PureList<Expr> exprs = ((Tight)frame).getExprs();

        if (op.getText().equals(_op.getText())) {
          return rest.cons(new Tight(_op, exprs.cons(e)));
        }
        else {
          Precedence prec = precedence(op, _op);

          if (prec instanceof Higher) {
            return stack.cons(new Tight(op, PureList.make(e)));
          }
          else if (prec instanceof Lower ||
                   prec instanceof Equal) {
            return tightInfixStack(finishInfixFrame(e,frame),
                                   op, rest);
          }
          else { // prec instanceof None
            throw new ReadError(FortressUtil.spanTwo(_op, op),
                                "Tight operators " + _op.getText()
                                  + " and " + op.getText() +
                                " have incomparable precedence.");
          }
        }
      }
      else if (frame instanceof TightChain) {
        PureList<ExprOpPair> links = ((TightChain)frame).getLinks();
        Op _op;

        if (links.isEmpty()) {
          throw new ReadError(op.getSpan(), "Empty chain expression.");
        }
        else {
          _op = ((Cons<ExprOpPair>)links).getFirst().getB();
        }
        Precedence prec = precedence(op, _op);

        if (prec instanceof Higher) {
          return stack.cons(new Tight(op, PureList.make(e)));
        }
        else if (prec instanceof Lower) {
          return tightInfixStack(finishInfixFrame(e,frame),op,rest);
        }
        else if (prec instanceof Equal) {
          throw new ReadError(op.getSpan(),
                              "Chaining operator " + op.getText() +
                              " not parsed as such.");
        }
        else { // prec instanceof None
          throw new ReadError(FortressUtil.spanTwo(_op, op),
                              "Tight operators " + _op.getText()
                                + " and " + op.getText() +
                              " have incomparable precedence.");
        }
      }
      else { // frame instanceof LooseChain
        PureList<ExprOpPair> links = ((LooseChain)frame).getLinks();
        if (links.isEmpty()) {
          throw new ReadError(op.getSpan(), "Empty chain expression.");
        }
        else {
          Op _op = ((Cons<ExprOpPair>)links).getFirst().getB();
          Precedence prec = precedence(op, _op);

          if (prec instanceof Higher) {
            return stack.cons(new Tight(op, PureList.make(e)));
          }
          else {
            throw new ReadError(FortressUtil.spanTwo(_op,op),
                                "Loose operator " + _op.getText() +
                                " near tight operator " +
                                op.getText() + ".");
          }
        }
      }
    }
  }

  // and loose_chain_stack (e : expr) (op : op) (stack : infix_stack) : infix_stack =
  //   match stack with
  //     | [] -> [`LooseChain [(e,op)]]
  //     | (`Tight (op',es) as frame) :: rest ->
  //         (match precedence op op' with
  //            | `Lower -> loose_chain_stack (finish_infix_frame e frame) op rest
  //            | _ -> Errors.read_error (span_two op' op)
  //                "Tight operator %s near loose operator %s of incompatible precedence."
  //                  op'.node_data op.node_data)
  //     | (`Loose (op',es) as frame) :: rest ->
  //         (match precedence op op' with
  //            | `Lower -> loose_chain_stack (finish_infix_frame e frame) op rest
  //            | `Higher -> `LooseChain [(e,op)] :: stack
  //            | `Equal ->
  //                Errors.internal_error op'.node_span
  //                  "Chaining operator %s not parsed as such." op'.node_data
  //            | `None ->
  //                Errors.read_error (span_two op' op)
  //                  "Loose operators %s and %s have incomparable precedence."
  //                  op'.node_data op.node_data)
  //     | (`TightChain links as frame) :: rest ->
  //         Errors.read_error op.node_span "Chaining with inconsistent spacing."
  //     | (`LooseChain links as frame) :: rest ->
  //         `LooseChain ((e,op)::links) :: rest
  private static PureList<InfixFrame> looseChainStack(Expr e, Op op,
                          PureList<InfixFrame> stack)
    throws ReadError
  {
    if (stack.isEmpty()) {
      InfixFrame frame = new LooseChain(PureList.make(new ExprOpPair(e,op)));

      return PureList.make(frame);
    }
    else { // !stack.isEmpty()
      Cons<InfixFrame> _stack = (Cons<InfixFrame>)stack;
      InfixFrame frame = _stack.getFirst();
      PureList<InfixFrame> rest = _stack.getRest();

      if (frame instanceof Tight) {
        Op _op = ((Tight)frame).getOp();

        if (precedence(op, _op) instanceof Lower) {
          return looseChainStack(finishInfixFrame(e,frame),op,rest);
        }
        else {
          throw new ReadError(FortressUtil.spanTwo(_op, op),
                              "Tight operator " + _op.getText() +
                              " near loose operator " + op.getText() + ".");
        }
      }
      else if (frame instanceof Loose) {
        Op _op = ((Loose)frame).getOp();
        Precedence prec = precedence(op, _op);

        if (prec instanceof Lower) {
          return looseChainStack(finishInfixFrame(e,frame),op,rest);
        }
        else if (prec instanceof Higher) {
          return stack.cons(new LooseChain
                              (PureList.make(new ExprOpPair(e,op))));
        }
        else if (prec instanceof Equal) {
          throw new ReadError(_op.getSpan(),
                              "Chaining operator " + _op.getText() +
                              " not parsed as such.");
        }
        else { // prec instanceof None
          throw new ReadError(FortressUtil.spanTwo(_op, op),
                              "Loose operators " + _op.getText() +
                              " and " + op.getText() + ".");
        }
      } else if (frame instanceof TightChain) {
        throw new ReadError(op.getSpan(),
                            "Chaining with inconsistent spacing: TightChain in LooseChainStack");
      }
      else { // frame instanceof LooseChain
        PureList<ExprOpPair> links = ((LooseChain)frame).getLinks();

        return rest.cons
          (new LooseChain(links.cons(new ExprOpPair(e,op))));
      }
    }
  }

  // and tight_chain_stack (e : expr) (op : op) (stack : infix_stack) : infix_stack =
  //   match stack with
  //     | [] -> [`TightChain [(e,op)]]
  //     | (`Loose (op',es) as frame) :: rest ->
  //         (match precedence op op' with
  //            | `Higher -> `TightChain [(e,op)] :: stack
  //            | _ -> Errors.read_error (span_two op' op)
  //                "Loose operator %s near tight operator %s of incompatible precedence."
  //                  op'.node_data op.node_data)
  //     | (`Tight (op',es) as frame) :: rest ->
  //         (match precedence op op' with
  //            | `Lower -> tight_chain_stack (finish_infix_frame e frame) op rest
  //            | `Higher -> `TightChain [(e,op)] :: stack
  //            | `Equal ->
  //                Errors.internal_error op'.node_span
  //                  "Chaining operator %s not parsed as such." op'.node_data
  //            | `None ->
  //                Errors.read_error (span_two op' op)
  //                  "Tight operators %s and %s have incomparable precedence."
  //                  op'.node_data op.node_data)
  //     | (`LooseChain links as frame) :: rest ->
  //         Errors.read_error op.node_span "Chaining with inconsistent spacing."
  //     | (`TightChain links as frame) :: rest ->
  //         `TightChain ((e,op)::links) :: rest
  private static PureList<InfixFrame> tightChainStack(Expr e, Op op,
                          PureList<InfixFrame> stack)
    throws ReadError {
    if (stack.isEmpty()) {
      InfixFrame frame = new TightChain(PureList.make(new ExprOpPair(e,op)));

      return PureList.make(frame);
    }
    else { // !stack.isEmpty()
      Cons<InfixFrame> _stack = (Cons<InfixFrame>)stack;
      InfixFrame frame = _stack.getFirst();
      PureList<InfixFrame> rest = _stack.getRest();

      if (frame instanceof Loose) {
        Op _op = ((Loose)frame).getOp();

        if (precedence(op, _op) instanceof Higher) {
          return stack.cons
            (new TightChain(PureList.make(new ExprOpPair(e,op))));
        }
        else {
          throw new ReadError(FortressUtil.spanTwo(_op, op),
                              "Loose operator " + _op.getText() +
                              " near tight operator " + op.getText() + ".");
        }
      }
      else if (frame instanceof Tight) {
        Op _op = ((Tight)frame).getOp();
        Precedence prec = precedence(op, _op);

        if (prec instanceof Lower) {
          return tightChainStack(finishInfixFrame(e,frame),op,rest);
        }
        else if (prec instanceof Higher) {
          return stack.cons(new TightChain
                              (PureList.make(new ExprOpPair(e,op))));
        }
        else if (prec instanceof Equal) {
          throw new ReadError(_op.getSpan(),
                              "Chaining operator " + _op.getText() +
                              " not parsed as such.");
        }
        else { // prec instanceof None
          throw new ReadError(FortressUtil.spanTwo(_op, op),
                              "Tight operators " + _op.getText() +
                              " and " + op.getText() + ".");
        }
      }
      else if (frame instanceof LooseChain) {
        throw new ReadError(op.getSpan(),
                            "Chaining with inconsistent spacing:LooseChain in TightChainStack");
      }
      else { // frame instanceof TightChain
        PureList<ExprOpPair> links = ((TightChain)frame).getLinks();

        return rest.cons
          (new TightChain(links.cons(new ExprOpPair(e,op))));
      }
    }
  }

  //  let rec resolve_ops_enclosing (oes : opexpr list) (stack : enclosing_stack)
  //    : expr =
  //  match oes with
  //   | [] ->
  //       finish_enclosing stack
  //   | `Left op :: rest ->
  //       resolve_ops_enclosing rest ( `Layer (op,[],stack) )
  //   | `Right op :: rest ->
  //       pop_enclosing op rest stack
  //   | (`Expr _ | `TightInfix _ | `LooseInfix _ | `Postfix _ | `Prefix _ as oe)
  //        :: rest ->
  //        push_enclosing oe rest stack
  private static Expr resolveOpsEnclosing(PureList<PrecedenceOpExpr> opExprs,
                      EnclosingStack stack)
    throws ReadError {
    if (opExprs.isEmpty()) { return finishEnclosing(stack); }

    else { // opExprs instanceof Cons
      Cons<PrecedenceOpExpr> _opExprs = (Cons<PrecedenceOpExpr>) opExprs;
      PrecedenceOpExpr first = _opExprs.getFirst();
      PureList<PrecedenceOpExpr> rest = _opExprs.getRest();

      if (first instanceof Left) {
        Left _first = (Left) first;

        return resolveOpsEnclosing(rest, stack.layer(_first.getOp()));
      }
      else if (first instanceof Right) {
        Right _first = (Right) first;

        return popEnclosing(_first.getOp(), rest, stack);
      }
      else { // first instanceof PostfixOpExpr
        return pushEnclosing((PostfixOpExpr)first, rest, stack);
      }
    }
  }

  //and build_layer (oes : postfix_opexpr list) : expr =
  //  resolve_infix
  //    (resolve_juxt
  //       (resolve_prefix
  //          (resolve_tight_div
  //             (resolve_postfix (List.rev oes)))))
  private static Expr buildLayer(PureList<PostfixOpExpr> opExprs)
    throws ReadError {
    return resolveInfix
              (resolvePrefix
                 (resolveJuxt
                    (resolveNonAssociative
                       (resolvePostfix (opExprs.reverse())))));
  }

  //and finish_enclosing (stack : enclosing_stack) : expr =
  //  match stack with
  //    | `Bottom oes -> build_layer oes
  //    | `Layer (op,oes,stack') ->
  //        Errors.read_error op.node_span
  //          "Left encloser %s without right encloser." op.node_data
  private static Expr finishEnclosing(EnclosingStack stack) throws ReadError {
    if (stack instanceof Bottom) { return buildLayer(stack.getList()); }

    else { // stack instanceof Layer
      Op op = ((Layer) stack).getOp();
      throw new ReadError(op.getSpan(), "Left encloser " + op.getText() +
                                        " without right encloser.");
    }
  }

  //and pop_enclosing (op : op) (oes : opexpr list) (stack : enclosing_stack)
  //    : expr =
  //  match stack with
  //    | `Layer (op',oes',stack') when op'.node_data = op.node_data ->
  //        let e = build_layer oes' in
  //          resolve_ops_enclosing
  //            (`Expr (Util.enclosing (span_two op' op) op' [e] op) :: oes)
  //            stack'
  //    | _ ->
  //        Errors.read_error op.node_span
  //          "Right encloser %s without left encloser." op.node_data
  private static Expr popEnclosing(Op op, PureList<PrecedenceOpExpr> opExprs,
                   EnclosingStack stack) throws ReadError {

    if (stack instanceof Layer &&
    ((Layer) stack).getOp().getText().equals(op.getText())) {
      Layer layer = (Layer) stack;
      Op layerOp = layer.getOp();

      List<Expr> es = new ArrayList<Expr>();
      es.add(buildLayer(layer.getList()));
      RealExpr expr = new RealExpr
        (ASTUtil.enclosing(FortressUtil.spanTwo(layerOp, op), layerOp, es, op));

      return resolveOpsEnclosing(opExprs.cons(expr), layer.getNext());
    }
    else {
      throw new ReadError(op.getSpan(),
                          "Right encloser without left encloser.");
    }
  }

  //and push_enclosing (oe : postfix_opexpr)
  //                   (oes : opexpr list)
  //                   (stack : enclosing_stack) : expr =
  //  match stack with
  //    | `Bottom oes' -> resolve_ops_enclosing oes (`Bottom (oe::oes'))
  //    | `Layer (op',oes',stack') ->
  //        resolve_ops_enclosing oes (`Layer (op',oe::oes',stack'))
  private static Expr pushEnclosing(PostfixOpExpr opExpr, PureList<PrecedenceOpExpr> opExprs,
                    EnclosingStack stack) throws ReadError {
    if (stack instanceof Bottom) {
      Bottom _stack = (Bottom)stack;
      PureList<PostfixOpExpr> stackList = _stack.getList();

      return resolveOpsEnclosing(opExprs,
                                 new Bottom(stackList.cons(opExpr)));
    }
    else { // stack instanceof Layer
      Layer layer = (Layer) stack;

      return resolveOpsEnclosing(opExprs,
                                 new Layer(layer.getOp(),
                                           layer.getList().cons(opExpr),
                                           layer.getNext()));
    }
  }

  //let resolve_ops (oes : opexpr list) : expr =
  //  try
  //    (* Trace.trace 0 "Resolve_ops %s\n" (opexprs_to_string oes); *)
  //    resolve_ops_enclosing oes (`Bottom [])
  //  with
  //    | Errors.ReadError _ as exn ->
  //        begin
  //          Trace.trace 3 "Precedence.resolve_ops failed for:\n%s"
  //            (opexprs_to_string oes);
  //          raise exn
  //        end
  public static Expr resolveOps(PureList<PrecedenceOpExpr> opExprs) {
    try {
      return resolveOpsEnclosing(opExprs, new Bottom());
    }
    catch (ReadError e) {
      String msg = e.getMessage();
      for (PrecedenceOpExpr expr : opExprs.toJavaList()) {
      msg += "\n  " + expr.toString();
      }
      return error("Resolution of operator property failed for:\n" + msg);
    }
  }

  public static Expr resolveOpsNoSpace(PureList<PrecedenceOpExpr> opExprs) {
      noSpace = true;
      return resolveOps(opExprs);
  }
}
