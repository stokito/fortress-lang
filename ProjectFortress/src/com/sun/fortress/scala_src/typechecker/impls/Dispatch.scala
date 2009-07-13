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
    case e:LocalVarDecl => checkExprDecls(e, expected)

    case e:_RewriteFnApp => checkExprFunctionals(e, expected)
    case e:FunctionalRef => checkExprFunctionals(e, expected)
    case e:MethodInvocation => checkExprFunctionals(e,expected)
    case e:OpExpr => checkExprFunctionals(e, expected)
    case e:SubscriptExpr => checkExprFunctionals(e, expected)
    
    case e:Juxt => checkExprOperators(e, expected)
    case e:MathPrimary => checkExprOperators(e, expected)
    case e:AmbiguousMultifixOpExpr => checkExprOperators(e, expected)
    case e:ChainExpr => checkExprOperators(e, expected)
    
    case _ => checkExprMisc(expr, expected)
  }
}
