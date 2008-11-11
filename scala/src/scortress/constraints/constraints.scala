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

package scortress.constraints

import scortress.types._

/** A static constraint for the validity of the program. */
abstract sealed class Constraint

/** A constraint of the form {@code C1 AND C2}. */
case class CnConj(c1:Constraint, c2:Constraint) extends Constraint

/** A constraint of the form {@code C1 OR C2}. */
case class CnDisj(c1:Constraint, c2:Constraint) extends Constraint

/** A constraint of the form {@code T1 == T2}. */
case class CnEquation(t1:Type, t2:Type) extends Constraint

/** A constraint of the form {@code T1 <: T2}. */
case class CnSubtype(t1:Type, t2:Type) extends Constraint

