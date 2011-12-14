/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.desugarer

import com.sun.fortress.compiler.Types
import com.sun.fortress.exceptions.InterpreterBug.bug
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.{ExprFactory => EF}
import com.sun.fortress.nodes_util.{NodeFactory => NF}
import com.sun.fortress.nodes_util.{NodeUtil => NU}
import com.sun.fortress.nodes_util.Span
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.Pairs._
import com.sun.fortress.scala_src.useful.SExprUtil._
import com.sun.fortress.scala_src.useful.STypesUtil._


/**
 * Desugaring of assignments and subscript expressions.
 *
 * A simple subscript expression is rewritten to a method call.
 *
 * An assignment to a tuple is rewritten to a block that binds temporary
 * variables to subexpressions that should be evaluated just once,
 * binds more temporary variables to the result of the RHS,
 * and then performs a sequence of individual assignment operations.
 * (There is probably not much to be gained by trying to parallelize
 * the latter; each individual assignment has a local variable on its
 * right and either a simple variable, a fieldref of a simple variable,
 * or a subscripting of a simple variable by simple variables.)
 *
 * A compound assignment is similarly rewritten.
 */
class AssignmentAndSubscriptDesugarer extends Walker {

  /** Oracle used for generating names. */
  private val naming = new NameOracle(this)

  /** Walk the AST, recursively desugaring any SubscriptExpr and Assignment nodes. */
  override def walk(node: Any) = node match {
    case SSubscriptExpr(info, obj, subs, op, staticArgs) =>
      // Recursively desugar the constituent expressions.
      val recurredObj = this.walk(obj).asInstanceOf[Expr]
      val recurredSubs = subs.map(this.walk(_).asInstanceOf[Expr])
      // Now desugar this SubscriptExpr node.
      desugarSubscriptExpr(EF.makeSubscriptExpr(info.getSpan, recurredObj, recurredSubs, op, staticArgs))
    case assn:Assignment =>
      // Desugar this Assignment node *first*.  (It is important that
      // any SubscriptExpr on the left-hand side *not* be desugared first!)
      desugarAssignment(assn) match {
        case SAssignment(info, lhses, op, rhs, assnInfos) =>
	  // Recursively desugar the new constituent expressions.
	  // (In particular, desugaring a tuple or compound assignment might
	  // produce simple subscripting assignments that must then be desugared.)
	  val recurredLhses = lhses.map(this.walk(_).asInstanceOf[Lhs])
	  val recurredRhs = this.walk(rhs).asInstanceOf[Expr]
	  // Now return the new Assignment node.
	  EF.makeAssignment(info.getSpan, recurredLhses, op, recurredRhs, assnInfos)
        case x => walk(x)
      }
    case _ => super.walk(node)
  }

  def desugarSubscriptExpr(se: SubscriptExpr): Expr = {
    // A subscript expression such as `a[b,c,d]` becomes a method call `a._[_](b,c,d)`.
    val SSubscriptExpr(info, obj, subs, maybeOp, staticArgs) = se
    val op = maybeOp.get
    EF.makeMethodInvocation(se, obj, op, staticArgs, EF.makeMaybeTupleExpr(NU.getSpan(op), subs))
  }

  def desugarAssignment(assn: Assignment): Expr = {
    val SAssignment(_, lhses, op, rhs, assnInfos) = assn
    if (op.isSome || lhses.size > 1) {
      // Compound and/or tuple assignment
      // The basic idea is to transform `(a, b.field, c[sub1,sub2]) := e` into
      // `do (ta, tb, tc, tsub1, tsub2, (t1, t2, t3)) = (a, b, c, sub1, sub2, e)
      //     a := t1; tb.field := t2; tc[tsub1, tsub2] := t3 end`
      // (TODO) Unfortunately, currently we don't handle nested binding tuples.
      // For now, we'll just transform it into
      // `do (ta, tb, tc, tsub1, tsub2) = (a, b, c, sub1, sub2)
      //     (t1, t2, t3) = e
      //     a := t1; tb.field := t2; tc[tsub1, tsub2] := t3 end`
      // which merely loses a bit of potential parallelism.
      // We omit the first tuple binding if the tuple is empty.
      // If it is a compound assignment `(a, b.field, c[sub1,sub2]) OP= e`, it becomes
      // `do (ta, tb, tc, tsub1, tsub2) = (a, b, c, sub1, sub2)
      //     (t1, t2, t3) = (ta, tb, tc[tsub1, tsub2]) OP e
      //     a := t1; tb.field := t2; tc[tsub1, tsub2] := t3 end`
      val isCompound = op.isDefined
      val assnSpan = NU.getSpan(assn)
      // For each lhs, we compute a nested tuple ((lvalues, exprs), (exprlvalue, access, assignment)).
      // We then unzip the list of such tuples into five distinct lists.
      val (temp1, temp2) = lhses.map(lhs => analyzeLhs(lhs, assnSpan)).unzip
      val (lvaluess, exprss) = temp1.unzip
      val (lvalues, exprs) = (lvaluess.flatten, exprss.flatten)
      val (exprlvalues, accesses, assignments) = temp2.unzip3
      val newRhs = (if (isCompound)
	  EF.makeOpExpr(assnSpan, op.get, EF.makeMaybeTupleExpr(assnSpan, accesses), rhs)
	  else rhs)
      val body = EF.makeLocalVarDecl(assnSpan, exprlvalues, newRhs, EF.makeBlock(assnSpan, assignments))
      if (exprs.size == 0) body else {
	  EF.makeLocalVarDecl(assnSpan, lvalues, EF.makeMaybeTupleExpr(assnSpan, exprs), body)
      }
    } else {
      lhses.head match {
	case SSubscriptExpr(info, obj, subs, maybeOp, staticArgs) =>
	  // Simple subscripted assignment `a[b,c,d] := e` becomes a method call `a._[_]:=(e,b,c,d)`.
	  val op = maybeOp.get
	  EF.makeMethodInvocation(assn, obj, NF.makeOp(op, op.getText() + ":="), staticArgs,
			          EF.makeMaybeTupleExpr(NU.spanTwo(op, rhs), rhs :: subs))
	case _ => assn
      }
    }
  }

  // Analyze one LHS of a (possibly tuple) assignment statement.
  // Returns a tuple of tuples of the form ((lvalues, exprs), (lvalue, expr, assigment)).
  // The first subtuple is a pair of lists:
  // the second list is a list of subexpressions of the LHS that need to be evaluated just once,
  // and the first list is a list of LValues (temporary variables to hold the values of those subexpressions).
  // The second subtuple has three items: an LValue that is used to temporarily bind one result from the
  // original RHS before putting it into its final destination; optionally an access to the LHS as an Expr
  // for purposes of compound assignment; and an assignment that will transfer the bound result to the LHS.
  def analyzeLhs(lhs: Lhs, assnSpan: Span): ((List[LValue], List[Expr]), (LValue, Expr, Expr)) = {
    val lhSpan = NU.getSpan(lhs.asInstanceOf[Expr]);
    val (rhsLValue, rhsVar) = naming.makeLValueAndVarRef(lhSpan)
    lhs match {
      case lhvar@SVarRef(_, _, _, _) => {
	val (varLValue, varVar) = naming.makeLValueAndVarRef(lhSpan)
	((List(varLValue), List(lhvar)),
	 (rhsLValue, varVar, EF.makeAssignment(assnSpan, lhvar, rhsVar)))
      }
      case SFieldRef(info, obj, field) => {
	val objSpan = NU.getSpan(obj);
	val (objLValue, objVar) = naming.makeLValueAndVarRef(objSpan)
	val newLhs = EF.makeFieldRef(lhSpan, objVar, field);
	((List(objLValue), List(obj)),
	 (rhsLValue, newLhs, EF.makeAssignment(assnSpan, newLhs, rhsVar)))
      }
      case SSubscriptExpr(info, obj, subs, op, staticArgs) => {
	val objSpan = NU.getSpan(obj);
	val (objLValue, objVar) = naming.makeLValueAndVarRef(objSpan)
	val (subLValues, subVars) = subs.map(sub => naming.makeLValueAndVarRef(NU.getSpan(sub)).asInstanceOf[(LValue,Expr)]).unzip
	val newLhs = EF.makeSubscriptExpr(lhSpan, objVar, subVars, op, staticArgs)
	((objLValue :: subLValues, obj :: subs),
	 (rhsLValue, newLhs, EF.makeAssignment(assnSpan, newLhs, rhsVar)))
      }
    }
  }

}
