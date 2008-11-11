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

package scortress.meta

import scortress.types._

import scala.collection.immutable.HashMap

/**
 * A single class table to manage lookups for traits and objects.
 */
object ClassTable {

  /** The inner table that stores a mapping from type names to definitions. */
  private var table:HashMap[String, DfType] = new HashMap

  /**  */
  def initialize(prog:Program):Unit =
    table = new HashMap ++ prog.defns.filter(defn =>
        defn.isInstanceOf[DfType]).map(defn => (defn.name, defn))

  /**
   * Get all matching method types from the class table. This will return a list
   * of functional types that have the given name in the given class. All
   * occurrences of the class' static parameters will be replaced with the
   * static arguments given in {@code cls}.
   * @return {@code Some(lst)} where {@code lst} is a possibly empty list of
   *     functional types, if the type {@code typ} exists and is valid in the
   *     class table. {@code None} if no such type exists.
   */
  def mtype(name:String, typ:TpTrait):Option[List[TpFunctional]] =
    table.get(typ.name).map(defn => defn.fns.filter(fn => fn.name == name))

  /**/
  def mbody(name:String, sargs:List[Type], cls:TpTrait):Term

}
