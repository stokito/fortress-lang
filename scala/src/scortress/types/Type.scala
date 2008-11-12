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

package scortress.types

/** A type in our language. */
abstract sealed class Type

/** The supertype of all types. */
case object TpAny extends Type

/** The subtype of all types. */
case object TpBottom extends Type

/** A variable that represents a type. */
case class TpVar(name:String) extends Type

/** A tuple of types. */
case class TpTuple(elts:List[Type]) extends Type

/** An intersection of types. */
case class TpAnd(elts:List[Type]) extends Type

/** A union of types. */
case class TpOr(elts:List[Type]) extends Type

/** A fixed point type, \mu U.T. */
case class TpMu(param:TpVar, typ:Type) extends Type

/** A trait or object type, possibly generic. */
case class TpTrait(name:String, sargs:Option[List[Type]]) extends Type

/** A non-generic arrow type. */
case class TpArrow(domain:Type, range:Type) extends Type

/**
 * A generic functional type. This is the type of any function or method defined
 * in our language. {@code sargs} is a list of (type parameter, bound) pairs.
 */
case class TpFunctional(sargs:List[Pair[Type, Type]],
                        args:List[Type],
                        range:Type) extends Type
