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
 * declared nonvariable type bounds. This environment also includes both
 * explicit and implicit bounds, such as {@code TpAny} and {@code TpObject}.
 */
abstract sealed class BoundsEnv {
  
  /**
   * Gets the nonvariable type bounds on the given type variable.
   * @param tp The type variable to look up.
   * @return {@code Some(List(T, ...))}, where the {@code T}s are the
   *     nonvariable type bounds on the given type variable. {@code None} if
   *     the given variable is not in the domain.
   */
  def lookup(tp:TpVar):Option[List[Type]]
  
  /**
   * Checks if the given type variable is in this bound env's domain.
   * @param name The type variable to check.
   * @return {@code true} if this env has bounds for the given type variable.
   *     {@code false} otherwise.
   */
  def inDom(tp:TpVar):Boolean = lookup(tp) != None
}

/**
 * A single empty environment with no mappings.
 */
object EmptyBoundsEnv extends BoundsEnv {
  override def lookup(name:String):Option[Type] = None
  override def toString = "[]"
}

/**
 * A nested environment structure that maps names to types. A type environment
 * is extended with a new name/type pair from an existing type environment.
 * Thus, we can represent a type environment has a single name/type pair
 * combined with a reference to its parent environment for all the previous
 * bindings.
 */
class NestedBoundsEnv(parent:TypeEnv, name:String, typ:Type) extends BoundsEnv {
  
  // Lookup the given name and return its bound type. First check the name in
  // this environment. If it's not there, check the parent environment.
  override def lookup(name:String):Option[Type] =
    if (name == this.name) Some(typ)
    else parent.lookup(name)
   
  override def toString = 
    "[" + t + " <: " + typ + "]" +
        (if (EmptyTypeEnv != parent) " + " + parent else "")
}


