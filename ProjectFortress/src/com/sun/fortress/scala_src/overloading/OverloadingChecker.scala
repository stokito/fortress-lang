/*******************************************************************************
    Copyright 2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.scala_src.overloading
import _root_.java.util.ArrayList
import _root_.java.util.{List => JavaList}
import _root_.java.util.{Set => JavaSet}
import edu.rice.cs.plt.collect.Relation
import edu.rice.cs.plt.tuple.{Option => JavaOption}
import edu.rice.cs.plt.tuple.{Pair => JavaPair}
import edu.rice.cs.plt.collect.Relation
import edu.rice.cs.plt.collect.IndexedRelation
import com.sun.fortress.compiler.GlobalEnvironment
import com.sun.fortress.compiler.index.CompilationUnitIndex
import com.sun.fortress.compiler.index.ComponentIndex
import com.sun.fortress.compiler.index.ObjectTraitIndex
import com.sun.fortress.compiler.index.ProperTraitIndex
import com.sun.fortress.compiler.index.TraitIndex
import com.sun.fortress.compiler.index.{Function => JavaFunction}
import com.sun.fortress.compiler.index.{Functional => JavaFunctional}
import com.sun.fortress.compiler.index.{Method => JavaMethod}
import com.sun.fortress.compiler.index.{Variable => JavaVariable}
import com.sun.fortress.compiler.typechecker.StaticTypeReplacer
import com.sun.fortress.exceptions.InterpreterBug
import com.sun.fortress.exceptions.InterpreterBug.bug
import com.sun.fortress.exceptions.StaticError
import com.sun.fortress.exceptions.TypeError
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.MultiSpan
import com.sun.fortress.nodes_util.NodeFactory
import com.sun.fortress.nodes_util.NodeUtil
import com.sun.fortress.nodes_util.Span
import com.sun.fortress.parser_util.IdentifierUtil
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.typechecker.TraitTable
import com.sun.fortress.scala_src.types.TypeAnalyzer
import com.sun.fortress.scala_src.useful._
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.STypesUtil._
import com.sun.fortress.scala_src.useful.Sets._


/* Checks the set of overloadings in a compilation unit. Must be run after typechecking
 * since return types must be inferred.
 * Verifies:
 * 1) Meet Rule, Exclusion Rule, Subtype Rule
 * 2)
 */

class OverloadingChecker(compilation_unit: CompilationUnitIndex,
                         globalEnv: GlobalEnvironment,
                         errors: List[StaticError])
                        (implicit analyzer: TypeAnalyzer = TypeAnalyzer.make(new TraitTable(compilation_unit, globalEnv))) {
  
  def extend(params: List[StaticParam], where: Option[WhereClause]) = 
    new OverloadingChecker(compilation_unit, globalEnv, errors)(analyzer.extend(params, where))
    
  def checkOverloadings(): JavaList[StaticError] = {
    val compFns = compilation_unit.functions
    // Create a map from function names to sets of function indices
    val names= toSet(compFns.firstSet)
    val imports = toListFromImmutable(compilation_unit.ast.getImports)
    val fnMap = for(f <- names) yield {
      val fComponent = toSet(compilation_unit.functions.matchFirst(f))
      val fImport = imports.flatMap{
        case SImportStar(_,_,api,except) => None
        case SImportNames(_,_,api,alias) => None
        case _ => None
      }
      (f,fComponent ++ fImport)
    }
    null
  }
  
  def isValid():Boolean = false
  
}
