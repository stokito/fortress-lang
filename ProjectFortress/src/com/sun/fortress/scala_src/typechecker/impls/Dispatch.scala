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

package com.sun.fortress.scala_src.typechecker.impls

import com.sun.fortress.nodes._
import com.sun.fortress.scala_src.typechecker.STypeCheckerBase

/**
 * This trait declares abstract methods for checking various groups of nodes
 * and implements the STypeCheckerBase.check method by dispatching to the
 * appropriate abstract method. The main purpose of this trait is to dispatch
 * to a specific implementation of type checking each group of nodes. When
 * mixed into STypeCheckerBase, the resulting type has abstract methods for
 * checking each group, so each group's implementation needs to be mixed in as
 * well to provide full type checking.
 * 
 * (The self-type annotation at the beginning declares that this trait must be
 * mixed into STypeCheckerBase. This is what allows it to implement abstract
 * members of STypeCheckerBase and access its protected members.)
 */
trait Dispatch { self: STypeCheckerBase =>

  // ---------------------------------------------------------------------------
  // ABSTRACT IMPL DECLARATIONS ------------------------------------------------
  
  // Functionals group
  def checkFunctionals(node: Node): Node
  def checkExprFunctionals(expr: Expr, expected: Option[Type]): Expr
  
  // Misc group
  def checkMisc(node: Node): Node
  def checkExprMisc(expr: Expr, expected: Option[Type]): Expr

  // ---------------------------------------------------------------------------
  // DISPATCH IMPLEMENTATION ---------------------------------------------------
  
  /**
   * Implements STypeCheckerBase.check by dispatching to some other abstract
   * method defined elsewhere.
   */
  def check(node: Node): Node = node match {
    case expr:Expr => checkExpr(expr)
    case ov:Overloading => checkFunctionals(ov)
    case _ => checkMisc(node)
  }
  
  /**
   * Implements STypeCheckerBase.checkExpr by dispatching to some other abstract
   * method defined elsewhere.
   */
  def checkExpr(expr: Expr, expected: Option[Type]): Expr = expr match {
    case e:SubscriptExpr => checkExprFunctionals(e, expected)
    case e:FunctionalRef => checkExprFunctionals(e, expected)
    case e:_RewriteFnApp => checkExprFunctionals(e, expected)
    case e:OpExpr => checkExprFunctionals(e, expected)
    case _ => checkExprMisc(expr, expected)
  }
}
