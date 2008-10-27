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

package com.sun.fortress.scala.scortress.exprs

import com.sun.fortress.scala.scortress.types._

/** An expression in our language. */
abstract sealed class Expr

/** A variable reference. */
case class ExVariable(name:String) extends Expr

/** A tuple of expressions of size two or greater. */
case class ExTuple(elts:List[Expr]) extends Expr

/** An instantiation of an object. */
case class ExObjInst(obj:String,
                     sargs:Option[List[Type]],
                     args:List[Expr]) extends Expr

/** An anonymous function expression. */
case class ExAnonFn(params:List[Pair[String, Option[Type]]],
                    range:Option[Type],
                    body:Expr) extends Expr

/** A dotted method invocation. */
case class ExInvok(obj:Expr,
                   name:String,
                   sargs:Option[List[Type]],
                   args:List[Expr]) extends Expr

/**
 * A juxtaposition of two other expressions as created by the parser. This could
 * be rewritten into either {@code Ex_OpApp} or {@code Ex_FnApp}, depending on
 * the type of its subexpressions.
 */
case class ExJuxt(left:Expr, right:Expr) extends Expr

/**
 * An application of the juxtaposition operator to two argument expressions.
 * After parsing, there is only a {@code ExJuxt} node. The type checker will
 * create this rewritten node if it is judged to be a juxtaposition and not a
 * function application.
 */
case class Ex_OpApp(left:Expr, right:Expr) extends Expr

/**
 * An application of some arrow-typed term to an operand. After
 * parsing, there is only a {@code ExJuxt} node. The type checker will create
 * this rewritten node if it is judged to be a juxtaposition and not a function
 * application.
 */
case class Ex_FnApp(app:Expr, opr:Expr) extends Expr
