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
            case (BottomType(_), _) => true
            case (_, BottomType(_)) => true
            case (AnyType(_), _) => false
            case (_, AnyType(_)) => false
            case (VarType(_,_,_), _) =>
                typeAnalyzer.exclusion(first, second)
            case (_, VarType(_,_,_)) =>
                typeAnalyzer.exclusion(first, second)
            case (ArrowType(_,_,_,_), ArrowType(_,_,_,_)) => false
            case (ArrowType(_,_,_,_), _) => true
            case (_, ArrowType(_,_,_,_)) => true
            case (f@TupleType(_,_,_,_), s@TupleType(_,_,_,_)) =>
                NodeUtil.differentArity(f, s) || typeAnalyzer.exclusion(f, s)
            case (TupleType(_,_,_,_) ,_) => true
            case (_, TupleType(_,_,_,_)) => true
            case _ => typeAnalyzer.exclusion(first, second)
    }

}
