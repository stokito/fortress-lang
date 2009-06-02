/*******************************************************************************
    Copyright 2009 Sun Microsystems, Inc.,
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

package com.sun.fortress.scala_src.useful

import com.sun.fortress.nodes._
import com.sun.fortress.scala_src.nodes._

object ExprUtil {

  def addType(expr: Expr, inferred: Type): Expr = expr match {
    case SAsExpr(SExprInfo(span, paren, ty), exp, annType) =>
      SAsExpr(SExprInfo(span, paren, Some(inferred)), exp, annType)
    case SAsIfExpr(SExprInfo(span, paren, ty), exp, annType) =>
      SAsIfExpr(SExprInfo(span, paren, Some(inferred)), exp, annType)
    case SAssignment(SExprInfo(span, paren, ty), lhs, assignOp, rhs, opsForLhs) =>
      SAssignment(SExprInfo(span, paren, Some(inferred)), lhs, assignOp, rhs,
                  opsForLhs)
    case SBlock(SExprInfo(span, paren, ty), loc, atomicBlock, withinDo, exprs) =>
      SBlock(SExprInfo(span, paren, Some(inferred)), loc, atomicBlock, withinDo,
             exprs)
    case SDo(SExprInfo(span, paren, ty), fronts) =>
      SDo(SExprInfo(span, paren, Some(inferred)), fronts)
    case SCaseExpr(SExprInfo(span, paren, ty), param, compare, equalsOp, inOp,
                   clauses, elseClause) =>
      SCaseExpr(SExprInfo(span, paren, Some(inferred)), param, compare, equalsOp,
                inOp, clauses, elseClause)
    case SIf(SExprInfo(span, paren, ty), clauses, elseClause) =>
      SIf(SExprInfo(span, paren, Some(inferred)), clauses, elseClause)
    case SLabel(SExprInfo(span, paren, ty), name, body) =>
      SLabel(SExprInfo(span, paren, Some(inferred)), name, body)
    case SObjectExpr(SExprInfo(span, paren, ty), header, selfType) =>
      SObjectExpr(SExprInfo(span, paren, Some(inferred)), header, selfType)
    case S_RewriteObjectExpr(SExprInfo(span, paren, ty), header,
                             implicitTypeParameters, genSymName,
                             staticArgs, params) =>
      S_RewriteObjectExpr(SExprInfo(span, paren, Some(inferred)), header,
                          implicitTypeParameters, genSymName, staticArgs,
                          params)
    case STry(SExprInfo(span, paren, ty), body, catchClause, forbidClause,
              finallyClause) =>
      STry(SExprInfo(span, paren, Some(inferred)),
           body, catchClause, forbidClause, finallyClause)
    case STupleExpr(SExprInfo(span, paren, ty), exprs, varargs, keywords, inApp) =>
      STupleExpr(SExprInfo(span, paren, Some(inferred)), exprs, varargs, keywords,
                 inApp)
    case STypecase(SExprInfo(span, paren, ty), bindIds, bindExpr, clauses,
                   elseClause) =>
      STypecase(SExprInfo(span, paren, Some(inferred)), bindIds, bindExpr, clauses,
                elseClause)
    case SWhile(SExprInfo(span, paren, ty), testExpr, body) =>
      SWhile(SExprInfo(span, paren, Some(inferred)), testExpr, body)
    case SFor(SExprInfo(span, paren, ty), gens, body) =>
      SFor(SExprInfo(span, paren, Some(inferred)), gens, body)
    case SAccumulator(SExprInfo(span, paren, ty), staticArgs, accOp, gens, body) =>
      SAccumulator(SExprInfo(span, paren, Some(inferred)), staticArgs, accOp, gens,
                   body)
    case SArrayComprehension(SExprInfo(span, paren, ty), staticArgs, clauses) =>
      SArrayComprehension(SExprInfo(span, paren, Some(inferred)), staticArgs,
                          clauses)
    case SAtomicExpr(SExprInfo(span, paren, ty), expr) =>
      SAtomicExpr(SExprInfo(span, paren, Some(inferred)), expr)
    case SExit(SExprInfo(span, paren, ty), target, returnExpr) =>
      SExit(SExprInfo(span, paren, Some(inferred)), target, returnExpr)
    case SSpawn(SExprInfo(span, paren, ty), body) =>
      SSpawn(SExprInfo(span, paren, Some(inferred)), body)
    case SThrow(SExprInfo(span, paren, ty), expr) =>
      SThrow(SExprInfo(span, paren, Some(inferred)), expr)
    case STryAtomicExpr(SExprInfo(span, paren, ty), expr) =>
      STryAtomicExpr(SExprInfo(span, paren, Some(inferred)), expr)
    case SFnExpr(SExprInfo(span, paren, ty), header, body) =>
      SFnExpr(SExprInfo(span, paren, Some(inferred)), header, body)
    case SLetFn(SExprInfo(span, paren, ty), body, fns) =>
      SLetFn(SExprInfo(span, paren, Some(inferred)), body, fns)
    case SLocalVarDecl(SExprInfo(span, paren, ty), body, lhs, rhs) =>
      SLocalVarDecl(SExprInfo(span, paren, Some(inferred)), body, lhs, rhs)
    case SSubscriptExpr(SExprInfo(span, paren, ty), obj, subs, op, staticArgs) =>
      SSubscriptExpr(SExprInfo(span, paren, Some(inferred)), obj, subs, op,
                     staticArgs)
    case SFloatLiteralExpr(SExprInfo(span, paren, ty), text,
                           intPart, numerator, denomBase, denomPower) =>
      SFloatLiteralExpr(SExprInfo(span, paren, Some(inferred)), text,
                        intPart, numerator, denomBase, denomPower)
    case SIntLiteralExpr(SExprInfo(span, paren, ty), text, intVal) =>
      SIntLiteralExpr(SExprInfo(span, paren, Some(inferred)), text, intVal)
    case SCharLiteralExpr(SExprInfo(span, paren, ty), text, charVal) =>
      SCharLiteralExpr(SExprInfo(span, paren, Some(inferred)), text, charVal)
    case SStringLiteralExpr(SExprInfo(span, paren, ty), text) =>
      SStringLiteralExpr(SExprInfo(span, paren, Some(inferred)), text)
    case SVoidLiteralExpr(SExprInfo(span, paren, ty), text) =>
      SVoidLiteralExpr(SExprInfo(span, paren, Some(inferred)), text)
    case SVarRef(SExprInfo(span, paren, ty), varId, staticArgs, lexicalDepth) =>
      SVarRef(SExprInfo(span, paren, Some(inferred)), varId, staticArgs,
              lexicalDepth)
    case SFieldRef(SExprInfo(span, paren, ty), obj, field) =>
      SFieldRef(SExprInfo(span, paren, Some(inferred)), obj, field)
    case SFnRef(SExprInfo(span, paren, ty), staticArgs, lexicalDepth, originalName,
               names, overloadings, newOverloadings, overloadingType) =>
      SFnRef(SExprInfo(span, paren, Some(inferred)), staticArgs, lexicalDepth,
             originalName, names, overloadings, newOverloadings, overloadingType)
    case SOpRef(SExprInfo(span, paren, ty), staticArgs, lexicalDepth, originalName,
               names, overloadings, newOverloadings, overloadingType) =>
      SOpRef(SExprInfo(span, paren, Some(inferred)), staticArgs, lexicalDepth,
             originalName, names, overloadings, newOverloadings, overloadingType)
    case S_RewriteFnRef(SExprInfo(span, paren, ty), fnExpr, staticArgs) =>
      S_RewriteFnRef(SExprInfo(span, paren, Some(inferred)), fnExpr, staticArgs)
    case S_RewriteObjectExprRef(SExprInfo(span, paren, ty), name, sargs) =>
      S_RewriteObjectExprRef(SExprInfo(span, paren, Some(inferred)), name, sargs)
    case SJuxt(SExprInfo(span, paren, ty), multiJuxt, infixJuxt, exprs,
               fnApp, tight) =>
      SJuxt(SExprInfo(span, paren, Some(inferred)), multiJuxt, infixJuxt, exprs,
            fnApp, tight)
    case S_RewriteFnApp(SExprInfo(span, paren, ty), function, argument) =>
      S_RewriteFnApp(SExprInfo(span, paren, Some(inferred)), function, argument)
    case SOpExpr(SExprInfo(span, paren, ty), op, args) =>
      SOpExpr(SExprInfo(span, paren, Some(inferred)), op, args)
    case SAmbiguousMultifixOpExpr(SExprInfo(span, paren, ty), infix_op,
                                  multifix_op, args) =>
      SAmbiguousMultifixOpExpr(SExprInfo(span, paren, Some(inferred)), infix_op,
                               multifix_op, args)
    case SChainExpr(SExprInfo(span, paren, ty), first, links) =>
      SChainExpr(SExprInfo(span, paren, Some(inferred)), first, links)
    case SCoercionInvocation(SExprInfo(span, paren, ty), toType,
                             staticArgs, arg) =>
      SCoercionInvocation(SExprInfo(span, paren, Some(inferred)), toType,
                          staticArgs, arg)
    case SMethodInvocation(SExprInfo(span, paren, ty), obj, method,
                           staticArgs, arg) =>
      SMethodInvocation(SExprInfo(span, paren, Some(inferred)), obj, method,
                        staticArgs, arg)
    case SMathPrimary(SExprInfo(span, paren, ty), multiJuxt,
                      infixJuxt, front, rest) =>
      SMathPrimary(SExprInfo(span, paren, Some(inferred)), multiJuxt,
                   infixJuxt, front, rest)
    case SArrayElement(SExprInfo(span, paren, ty), staticArgs, element) =>
      SArrayElement(SExprInfo(span, paren, Some(inferred)), staticArgs, element)
    case SArrayElements(SExprInfo(span, paren, ty), staticArgs, dimension,
                        elements, outermost) =>
      SArrayElements(SExprInfo(span, paren, Some(inferred)), staticArgs, dimension,
                     elements, outermost)
  }
}
