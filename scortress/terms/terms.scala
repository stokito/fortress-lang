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

package com.sun.fortress.scala.scortress.terms

import com.sun.fortress.scala.scortress.types._

/** An expression in our language. */
abstract sealed class Term

/** A variable reference. */
case class TmVariable(name:String) extends Term

/** A tuple of expressions of size two or greater. */
case class TmTuple(elts:List[Term]) extends Term

/** An instantiation of an object. */
case class TmObjInst(obj:String,
                     sargs:Option[List[Type]],
                     args:List[Term]) extends Term

/** An anonymous function expression. */
case class TmAnonFn(params:List[Pair[String, Option[Type]]],
                    range:Option[Type],
                    body:Term) extends Term

/** A dotted method invocation. */
case class TmInvok(obj:Term,
                   name:String,
                   sargs:Option[List[Type]],
                   args:List[Term]) extends Term

/**
 * A juxtaposition of two other expressions as created by the parser. This could
 * be rewritten into either {@code Tm_OpApp} or {@code Tm_FnApp}, depending on
 * the type of its subexpressions.
 */
case class TmJuxt(left:Term, right:Term) extends Term

/**
 * An application of the juxtaposition operator to two argument expressions.
 * After parsing, there is only a {@code TmJuxt} node. The type checker will
 * create this rewritten node if it is judged to be a juxtaposition and not a
 * function application.
 */
case class Tm_OpApp(left:Term, right:Term) extends Term

/**
 * An application of some arrow-typed term to an operand. After
 * parsing, there is only a {@code TmJuxt} node. The type checker will create
 * this rewritten node if it is judged to be a juxtaposition and not a function
 * application.
 */
case class Tm_FnApp(app:Term, opr:Term) extends Term
