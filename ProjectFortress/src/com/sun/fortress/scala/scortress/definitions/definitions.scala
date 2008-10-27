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

package com.sun.fortress.scala.scortress.definitions

import com.sun.fortress.scala.scortress.exprs._
import com.sun.fortress.scala.scortress.types._

/** Definitions for a program in our language. */
abstract sealed class Definition

/** Either a top-level function definition or a method definition. */
case class DefFunction(name:String,
                       sparams:List[Pair[String, Type]],
                       params:List[Pair[String, Option[Type]]],
                       range:Option[Type],
                       body:Term) extends Definition

/** A top-level trait definition. */
case class DefTrait(name:String,
                    sparams:List[Pair[String, Type]],
                    exts:List[Type],
                    fns:List[DefFunction]) extends Definition

/** A top-level object definition. */
case class DefObject(name:String,
                     sparams:List[Pair[String, Type]],
                     params:List[Pair[String, Option[Type]]],
                     exts:List[Type],
                     fns:List[DefFunction]) extends Definition

/**
 * A full program in our language. Evaluate the body with the given definitions.
 */
case class Program(defns:List[Definition], body:Term)

