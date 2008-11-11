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

/**
 * A type bounds environment that contains a mapping of type variables to their
 * declared nonvariable type bounds.
 */
abstract sealed class BoundsEnv {
  
  /**
   * Gets the nonvariable type bounds on the given type variable.
   * @param tp The type variable to look up.
   * @return {@code Some(T)}, where {@code T} is the
   *     nonvariable type bound on the given type variable. {@code None} if
   *     the given variable is not in the domain.
   */
  def lookup(tp:TpVar):Option[Type]
  
  /**
   * Checks if the given type variable is in this bound env's domain.
   * @param name The type variable to check.
   * @return {@code true} if this env has a bound for the given type variable.
   *     {@code false} otherwise.
   */
  def inDom(tp:TpVar):Boolean = lookup(tp) != None
}

/**
 * A single empty environment with no mappings.
 */
object EmptyBoundsEnv extends BoundsEnv {
  override def lookup(tp:TpVar):Option[Type] = None
  override def toString = "[]"
}

/**
 * A nested environment structure that maps type variables to their declared
 * type bounds. A bounds environment is extended with a new type variable/type
 * pair from an existing bounds environment. Thus, we can represent a bounds
 * environment has a single type variable/type pair combined with a reference
 * to its parent environment for all the previous bindings.
 */
class NestedBoundsEnv(parent:BoundsEnv, typ:TpVar, bound:Type) extends BoundsEnv {
  
  // Lookup the given name and return its bound type. First check the type
  // variable in this environment. If it's not there, check the parent
  // environment.
  override def lookup(typ:TpVar):Option[Type] =
    if (typ == this.typ) Some(typ)
    else parent.lookup(typ)
   
  override def toString = 
    "[" + typ + " <: " + bound + "]" +
        (if (EmptyBoundsEnv != parent) " + " + parent else "")
}


