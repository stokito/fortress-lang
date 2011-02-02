/*******************************************************************************
    Copyright 2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.scala_src.typechecker.impls

import com.sun.fortress.nodes._
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.typechecker.STypeChecker

/**
 * This trait declares abstract methods for checking various groups of nodes
 * and implements the STypeChecker.check method by dispatching to the
 * appropriate abstract method. The main purpose of this trait is to dispatch
 * to a specific implementation of type checking each group of nodes. When
 * mixed into STypeChecker, the resulting type has abstract methods for
 * checking each group, so each group's implementation needs to be mixed in as
 * well to provide full type checking.
 *
 * (The self-type annotation at the beginning declares that this trait must be
 * mixed into STypeChecker. This is what allows this trait to implement
 * abstract members of STypeChecker and to access its protected members.)
 */
trait Dispatch { self: STypeChecker =>

  // ---------------------------------------------------------------------------
  // ABSTRACT IMPL DECLARATIONS ------------------------------------------------

  // Decls group
  def checkDecls(node: Node): Node
  def checkExprDecls(expr: Expr, expected: Option[Type]): Expr

  // Functionals group
  def checkFunctionals(node: Node): Node
  def checkExprFunctionals(expr: Expr, expected: Option[Type]): Expr

  // Operators group
  def checkOperators(node: Node): Node
  def checkExprOperators(expr: Expr, expected: Option[Type]): Expr

  // Misc group
  def checkMisc(node: Node): Node
  def checkExprMisc(expr: Expr, expected: Option[Type]): Expr

  // ---------------------------------------------------------------------------
  // DISPATCH IMPLEMENTATION ---------------------------------------------------

  /**
   * Implements STypeChecker.check by dispatching to some other abstract
   * method defined elsewhere.
   */
  def check(node: Node): Node = node match {
    case n:Expr => checkExpr(n)

    case n:Component => checkDecls(n)
    case n:TraitDecl => checkDecls(n)
    case n:ObjectDecl => checkDecls(n)
    case n:FnDecl => checkDecls(n)
    case n:VarDecl => checkDecls(n)

    case n:Overloading => checkFunctionals(n)

    case n:Op => checkOperators(n)
    case n:Link => checkOperators(n)

    case _ => checkMisc(node)
  }

  /**
   * Implements STypeChecker.checkExpr by dispatching to some other abstract
   * method defined elsewhere.
   */
  def checkExpr(expr: Expr, expected: Option[Type]): Expr = expr match {
    case e:LetFn => checkExprDecls(e, expected)
    case e:LocalVarDecl => checkExprDecls(e, expected)

    case e:_RewriteFnApp => checkExprFunctionals(e, expected)
    case e:CaseExpr => checkExprFunctionals(e, expected)
    case e:FnExpr => checkExprFunctionals(e, expected)
    case e:FunctionalRef => checkExprFunctionals(e, expected)
    case e:MethodInvocation => checkExprFunctionals(e,expected)
    case e:OpExpr => checkExprFunctionals(e, expected)
    case e:SubscriptExpr => checkExprFunctionals(e, expected)

    case e:AmbiguousMultifixOpExpr => checkExprOperators(e, expected)
    case e:Assignment => checkExprOperators(e, expected)
    case e:ChainExpr => checkExprOperators(e, expected)
    case e:Juxt => checkExprOperators(e, expected)
    case e:MathPrimary => checkExprOperators(e, expected)

    case _ => checkExprMisc(expr, expected)
  }
}
