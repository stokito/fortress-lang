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

package scortress.definitions

import scortress.terms._
import scortress.types._

/** Definitions for a program in our language. */
abstract sealed class Definition

/** Either a top-level function definition or a method definition. */
case class DfFunction(name:String,
                      sparams:List[Pair[String, Type]],
                      params:List[Pair[String, Option[Type]]],
                      range:Option[Type],
                      body:Term) extends Definition

abstract class DfType(val name:String,
                      val sparams:List[Pair[String, Type]],
                      val exts:List[TpTrait],
                      val fns:List[DfFunction]) extends Definition


/** A top-level trait definition. */
case class DfTrait(override val name:String,
                   override val sparams:List[Pair[String, Type]],
                   override val exts:List[TpTrait],
                   override val fns:List[DfFunction])
    extends DfType(name, sparams, exts, fns)

/** A top-level object definition. */
case class DfObject(override val name:String,
                    override val sparams:List[Pair[String, Type]],
                    params:List[Pair[String, Option[Type]]],
                    override val exts:List[TpTrait],
                    override val fns:List[DfFunction])
    extends DfType(name, sparams, exts, fns)

/**
 * A full program in our language. Evaluate the body with the given definitions.
 */
case class Program(defns:List[Definition], body:Term)
