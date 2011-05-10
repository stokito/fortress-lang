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
 * Compound assignment desugaring. Desugars a compound assignment with N LHSes
 * into a tuple of N ordinary single assignments. Preserves any coercions that
 * were attached to the compound assignment nodes.
 */
class AssignmentDesugarer extends Walker {

  /** Counter for the number of assignments desugared. Used in gensym. */
  private val naming = new NameOracle(this)

  /** Does the walking. */
  override def walk(node: Any) = node match {
    // Make sure to recursively desugar assignments in any subexpressions.
    case assn@SAssignment(v1, lhses, v3, rhs, v5) =>
      val recurredLhses = lhses.map(this.walk(_).asInstanceOf[Lhs])
      val recurredRhs = this.walk(rhs).asInstanceOf[Expr]
      val recurredAssn = SAssignment(v1, recurredLhses, v3, recurredRhs, v5)
      desugarAssignment(assn)
    case _ => super.walk(node)
  }

  def desugarAssignment(assn: Assignment): Expr = {
    implicit val impAssn @ SAssignment(SExprInfo(assnSpan, _, _), lhses, _, rhs, assnInfos) = assn

    // Phase 1 -----------------------------------------------------------------

    // Create the variable Ids for RHS_i vars and the type decl for all of them
    // (without initializing expressions).
    val (rhsVars, rhsTypeDecl) = makeRhsVarsAndDecl(rhs)

    // Same thing but for the LHS_i vars. Only done if compound.
    val (lhsVars, lhsTypeDecl) =
      if (isCompound)
        mapSome(makeLhsVarsAndDecl(lhses))
      else
        (None, None)

    // Collect up the type decls to be done for this phase.
    val phaseOneDecls =
      if (isCompound)
        List(rhsTypeDecl, lhsTypeDecl.get)
      else
        List(rhsTypeDecl)

    // Evaluate the subexpressions of each of the LHSes and get any necessary
    // decls for doing so.
    val evaledLhsesAndDecls = makeEvaledLhses(lhses)
    val (evaledLhses, declsForLhs) = (evaledLhsesAndDecls).unzip

    // A list of list of decls. For each component, this will be the list of all
    // decls that need to be performed. If compound, add in the decl setting
    // LHS_i to evaledLhs_i. Otherwise just use the declsForLhs decls.
    val parallelDeclsForLhs =
      if (isCompound)
        appendLhsDecls(evaledLhsesAndDecls, lhsVars.get)
      else
        declsForLhs

    // Now append the decl setting the RHS_i vars to the original RHS expr.
    // We also want to filter out any of the LHS decls that were empty (i.e.
    // for VarRef LHSes).
    val parallelDecls =
      parallelDeclsForLhs.filter(!_.isEmpty) ++
        List(List(TempVarDecl(rhsVars, rhs)))

    // Collapse each parallel group of decls into nested LocalVarDecls. The
    // result is a list of LocalVarDecls that must be done in parallel.
    val actualParallelDecls = parallelDecls.map(decls =>
      TempVarDecl.makeLocalVarDeclFromList(decls,
                                           assnSpan,
                                           EF.makeTupleExpr(assnSpan)))

    // Create the parallel do expression for the Phase 1 decls.
    val phaseOneDoFronts = actualParallelDecls.map(e => EF.makeBlock(e))
    val phaseOneDo =
      EF.makeDo(assnSpan, false, Some(Types.VOID), toJavaList(phaseOneDoFronts))

    // Compound Phase ----------------------------------------------------------

    // Create N fresh names and set them equal to the operator expressions.
    val (compoundDecl, opExprNames) =
      if (isCompound)
        mapSome(makeCompoundDecl(lhsVars.get, rhsVars))
      else
        (None, None)

    // Phase 2 -----------------------------------------------------------------

    val phaseTwoRhses: List[Expr] = if (isCompound) opExprNames.get else rhsVars
    val phaseTwoExpr = makeAssignments(evaledLhses, phaseTwoRhses)

    // Collapse all the expressions after Phase 1.
    val exprAfterPhaseOne =
      if (isCompound)
        compoundDecl.get.makeLocalVarDecl(assnSpan, phaseTwoExpr)
      else
        phaseTwoExpr

    // Create the block of expressions after the Phase 1 decls.
    val afterPhaseOneDecls = List(phaseOneDo, exprAfterPhaseOne)

    // Collapse the block after Phase 1 with the Phase 1 decls to create one
    // massive LocalVarDecl.
    val phasesOneAndTwo =
      TempVarDecl.makeLocalVarDeclFromList(phaseOneDecls, assnSpan, afterPhaseOneDecls)

    // Create the do block that wraps around the whole mess (and a void).
    EF.makeDo(assnSpan,
              Some(Types.VOID),
              toJavaList(List(phasesOneAndTwo,
                              EF.makeVoidLiteralExpr(assnSpan))))
  }

  /**
   * For the given list of LHSes, make the block of all necessary
   * sub-evaluations and a list of the recombined LHSes. For example, for the
   * LHSes of `(a[i], b.x, c) += (0, 1, 2)`, return the list
   * [`a'[i']`, `b'.x`, `c`] and the list of decls
   * [[`(a', i') = (a, i)`], [`b' = b`], Nil] zipped together.
   */
  private def makeEvaledLhses(lhses: List[Lhs])
                             (implicit assn: Assignment)
                              : List[(Lhs, List[TempVarDecl])] =
    lhses.map {
      case e:VarRef => (e, Nil)

      // Eval the receiver and recompose.
      case SFieldRef(v1, obj, v3) =>
        val evaledObj: VarRef = naming.makeVarRef(obj)
        val objDecl = TempVarDecl(evaledObj, obj)
        (SFieldRef(v1, evaledObj, v3), List(objDecl))

      // Eval the receiver and args and recompose.
      case SSubscriptExpr(v1 @ SExprInfo(span, _, _), obj, subs, v4, v5) =>
        val evaledObj: VarRef = naming.makeVarRef(obj)

        // Eval subs with the tuple containing them as the RHS
        val evaledSubs: List[VarRef] = subs.map(naming.makeVarRef)
        val expr = EF.makeMaybeTupleExpr(span, toJavaList(obj :: subs))
        val decls = TempVarDecl(evaledObj :: evaledSubs, expr)
        (SSubscriptExpr(v1, evaledObj, evaledSubs, v4, v5), List(decls))

      case e => bug("unexpected LHS for assignment: %s".format(e))
    }


  /**
   * Create the LHS_i variable for each component in the assignment.
   * Return a tuple of the LHS Ids and the decl binding them to their respective
   * types. This is only necessary for a compound assignment.
   */
  private def makeLhsVarsAndDecl(lhses: List[Lhs])
                                (implicit assn: Assignment)
                                 : (List[VarRef], TempVarDecl) = {
    // Get the N types out of the LHSes.
    val lhsTypes = lhses.map(e => getType(e.asInstanceOf[Expr]).get)

    // Create the corresponding Ids.
    val lhsVars = lhsTypes.map(naming.makeVarRef)

    // Create the big decl of all LHS vars.
    (lhsVars, TempVarDecl(lhsVars))
  }


  /**
   * Create the RHS_i variable for each component in the assignment.
   * Return a tuple of the RHS Ids and the decl binding them to their respective
   * types.
   */
  private def makeRhsVarsAndDecl(rhs: Expr)
                                (implicit assn: Assignment)
                                 : (List[VarRef], TempVarDecl) = {
    // Get the N types out of the RHS.
    val n = assn.getLhs.size
    val rhsTypes =
      if (n == 1)
        List(getType(rhs).get)
      else
        typeIterator(getType(rhs).get).take(n).toList

    // Create the corresponding Ids.
    val rhsVars = rhsTypes.map(naming.makeVarRef)

    // Create the big decl of all RHS vars and their types.
    (rhsVars, TempVarDecl(rhsVars))
  }

  /**
   * Append the LHS_i decls onto the existing LHS decls. This is only done for
   * compound assignments.
   */
  private def appendLhsDecls(evaledLhsesAndDecls: List[(Lhs, List[TempVarDecl])],
                             lhsVars: List[VarRef])
                            (implicit assn: Assignment)
                             : List[List[TempVarDecl]] =

    // Zip over the evaluated LHSes (with their decls) and the LHS var names.
    (evaledLhsesAndDecls, lhsVars).zipped.map {
      case ((evaledLhs, decls), lhsVar) =>
        // Add the `LHS_i = evaledLhses_i` decl to the existing one.
        decls ++ List(TempVarDecl(lhsVar, evaledLhs.asInstanceOf[Expr]))
    }

  /**
   * For each component in the compound assignment, create the OpExpr
   * `LHS_i OP RHS_i`. There might also be coercions to apply, as given in
   * the assignment infos. Then make the decl for setting some fresh names
   * equal to the op exprs. Returns the decl and the list of new VarRefs.
   */
  private def makeCompoundDecl(lhsVars: List[VarRef],
                               rhsVars: List[VarRef])
                              (implicit assn: Assignment)
                               : (TempVarDecl, List[VarRef]) = {

    // Map over (LHS_i, RHS_i, info_i), creating a new OpExpr (or coercion).
    val opExprs = ((lhsVars zip rhsVars) zip toListFromImmutable(assn.getAssignmentInfos)) map {
      case ((lhsVar, rhsVar), SCompoundAssignmentInfo(op, coercionOuter, coercionInner)) =>

        // If there was a coercion on the inner argument, copy it onto LHS_i.
        val argL: Expr = coercionInner.map(copyCoercion(_, lhsVar)).getOrElse(lhsVar)

        // Get the return type from OP.
        val range = getType(op).get.asInstanceOf[ArrowType].getRange
        val info = SExprInfo(NU.getSpan(op), false, Some(range))

        // Create the OpExpr for `argL OP rhsVar`.
        val opExpr = SOpExpr(info, op, List(argL, rhsVar))

        // If there was a coercion on the outer argument, copy it onto opExpr.
        // Otherwise just return the OpExpr itself.
        coercionOuter.map(copyCoercion(_, opExpr)).getOrElse(opExpr)
    }

    // Make the fresh variables and return the decl.
    val freshVars = opExprs.map(naming.makeVarRef)
    val opExprsTuple = EF.makeMaybeTupleExpr(NU.getSpan(assn), toJavaList(opExprs))
    (TempVarDecl(freshVars, opExprsTuple), freshVars)
  }

  /**
   * Make an assignment or tuple of assignments using the given LHSes and RHSes.
   */
  private def makeAssignments(evaledLhses: List[Lhs],
                              phaseTwoRhses: List[Expr])
                             (implicit assn: Assignment): Expr = {

    // Create the individual assignments.
    val assns = (evaledLhses, phaseTwoRhses).zipped.map { (lhs, rhs) =>
      val info = SExprInfo(NU.getSpan(assn),
                           NU.isParenthesized(assn),
                           Some(Types.VOID))
      SAssignment(info, List(lhs), None, rhs, Nil)
    }

    // Combine them into a tuple, if necessary.
    EF.makeMaybeTupleExpr(NU.getSpan(assn), toJavaList(assns))
  }



  /** Is the given assignment compound? */
  private def isCompound(implicit assn: Assignment): Boolean =
    assn.getAssignOp.isSome




}
