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

package com.sun.fortress.scala_src.typechecker

import _root_.java.util.ArrayList
import _root_.java.util.{List => JavaList}
import _root_.java.util.{Set => JavaSet}
import edu.rice.cs.plt.tuple.{Option => JavaOption}
import scala.collection.Set
import com.sun.fortress.compiler.GlobalEnvironment
import com.sun.fortress.compiler.index.ComponentIndex
import com.sun.fortress.compiler.index.{Function => JavaFunction}
import com.sun.fortress.compiler.typechecker.TraitTable
import com.sun.fortress.compiler.typechecker.TypeAnalyzer
import com.sun.fortress.exceptions.InterpreterBug
import com.sun.fortress.exceptions.StaticError
import com.sun.fortress.exceptions.TypeError
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.NodeFactory
import com.sun.fortress.nodes_util.NodeUtil
import com.sun.fortress.nodes_util.Span
import com.sun.fortress.parser_util.IdentifierUtil
import com.sun.fortress.repository.FortressRepository
import com.sun.fortress.scala_src.useful._
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.Sets._
import com.sun.fortress.scala_src.nodes._

class ExclusionOracle(typeAnalyzer: TypeAnalyzer) {
    /* Checks the overloading rule: exclusion
     * Invariant: firstParam is not equal to secondParam
     * The following types are not yet supported:
     *     Types tagged with dimensions or units
     *     Effects on arrow types
     *     Keyword parameters and varargs parameters
     *     Intersection types
     *     Union types
     *     Fixed-point types
     */
    def excludes(first: Type, second: Type): Boolean =
        (first, second) match {
            case (SBottomType(_), _) => true
            case (_, SBottomType(_)) => true
            case (SAnyType(_), _) => false
            case (_, SAnyType(_)) => false
            case (SVarType(_,_,_), _) =>
                typeAnalyzer.exclusion(first, second)
            case (_, SVarType(_,_,_)) =>
                typeAnalyzer.exclusion(first, second)
            case (SArrowType(_,_,_,_), SArrowType(_,_,_,_)) => false
            case (SArrowType(_,_,_,_), _) => true
            case (_, SArrowType(_,_,_,_)) => true
            case (f@STupleType(_,_,_,_), s@STupleType(_,_,_,_)) =>
                NodeUtil.differentArity(f, s) || typeAnalyzer.exclusion(f, s)
            case (STupleType(_,_,_,_) ,_) => true
            case (_, STupleType(_,_,_,_)) => true
            case _ => typeAnalyzer.exclusion(first, second)
    }

}
