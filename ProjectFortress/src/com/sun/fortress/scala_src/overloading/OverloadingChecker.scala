/*******************************************************************************
    Copyright 2010,2011, Oracle and/or its affiliates.
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
import com.sun.fortress.compiler.index._
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
import com.sun.fortress.scala_src.useful.Maps._
import com.sun.fortress.scala_src.useful.Iterators._


/* Checks the set of overloadings in a compilation unit. Must be run after typechecking
 * since return types must be inferred.
 * TODO : Rename this FunctionalChecker
 */

class OverloadingChecker(current: CompilationUnitIndex,
                         global: GlobalEnvironment,
                         errors: List[StaticError] = List())
                         (analyzer: TypeAnalyzer = TypeAnalyzer.make(new TraitTable(current, global))) {
  
  def extend(params: List[StaticParam], where: Option[WhereClause]) = 
    new OverloadingChecker(current, global, errors)(analyzer.extend(params, where))
    
  def check(): JavaList[StaticError] = toJavaList(checkFunctions ++ checkMethods)
  
  private def checkFunctions(): List[StaticError] = {
    val compFnRel = current.functions
    // Create a map from function names to sets of function indices
    val compFns = toSet(compFnRel.firstSet).flatMap(isDeclaredName).map{
      f => (f, toSet(compFnRel.matchFirst(f).asInstanceOf[JavaSet[Functional]]))
    }
    val imports = toListFromImmutable(current.ast.getImports)
    val explicitImports = imports.flatMap{
      case SImportNames(_, _, api, aliases) =>
        aliases.flatMap{
          case SAliasedSimpleName(_,name: IdOrOp, Some(alias: IdOrOp)) =>
            Some((alias, getFunctions(api, name)))
          case SAliasedSimpleName(_, name: IdOrOp, None) => Some(name)
            Some((name, getFunctions(api, name)))
          case _ => None
        }
      case _ => List()
    }
    val explicitFns = (compFns ++ explicitImports).groupBy(_._1.getText).mapValues{x => 
      val (ids, fns) = x.unzip
      (ids, fns.flatMap(y => y))
    }
    
    val importStar = imports.flatMap{isImportStar}
    
    // TODO: Crawl component to get all of the function references so I can check implicit imports properly
    
    val allFns = explicitFns.map{
      case (name, (ids, fns)) =>
        val implicitFns = importStar.flatMap{
          case SImportStar(_, _, api, excepts) if !excepts.exists(_.asInstanceOf[IdOrOp].getText == name) =>
            ids.flatMap(getFunctions(api, _))
          case _ =>
            Set[Functional]()
        }
        (ids, fns ++ implicitFns)
    }
    implicit val checkingMethods = false
    allFns.flatMap(checkOverloadingRules).toList
  }

  private def checkMethods(): List[StaticError] = {
    // overloading rules
    // abstract methods
    // getters/setters
    // no new functional methods
    val typesInCompilationUnit = current.typeConses.values
    typesInCompilationUnit.flatMap{
      case t: ObjectTraitIndex => List()
      case t: ProperTraitIndex => List()
      case _ => List()
    }.toList
  }
  
  private def checkOverloadingRules(idsAndFns: (Set[IdOrOp], Set[Functional]))(implicit checkingMethods: Boolean): List[StaticError] = {
    val (ids, fns) = idsAndFns
    noDuplicatesRule(fns) ++ meetRule(fns) ++ returnTypeRule(fns)
  }
  
  private def noDuplicatesRule(fns: Set[Functional])(implicit checkingMethods: Boolean): List[StaticError] = List()
  private def meetRule(fns: Set[Functional])(implicit checkingMethods: Boolean): List[StaticError] = List()
  private def returnTypeRule(fns: Set[Functional])(implicit checkingMethods: Boolean): List[StaticError] = List()

  
  private def checkGetterSetters(): List[StaticError] = List()
  private def checkAbstractMethods(): List[StaticError] = List()
  private def noNewFunctionalMethods(): List[StaticError] = List()
  
  private def isDeclaredName(f: IdOrOpOrAnonymousName): Option[IdOrOp] = f match {
    case i@SId(_,_,str) if IdentifierUtil.validId(str) => Some(i)
    case o@SOp(_,_,str,_,_) if NodeUtil.validOp(str) => Some(o) 
    case _ => None
  }
  
  private def isImportStar(i: Import): Option[ImportStar] = i match {
    case s: ImportStar => Some(s)
    case _ => None
  }
  
  private def isArrow(v: Variable): Set[Functional] = v match {
    case d@SDeclaredVariable(lvalue) =>
      toOption(lvalue.getIdType) match {
        case Some(a: ArrowType) => Set(new DummyVariableFunction(d))
        case _ => Set()
      }
    case _ => Set()
  }
  
  private def getFunctions(api: APIName, id: IdOrOp): Set[Functional] = 
    getFunctions(global.lookup(api), id, false)
  
  private def getFunctions(cUnit: CompilationUnitIndex, id: IdOrOp, onlyConcrete: Boolean): Set[Functional] = {
    val fns = toSet(cUnit.functions.matchFirst(id)).filter(!onlyConcrete || _.body.isSome)
    val vars = cUnit.variables
    val mArrowVar = if (vars.containsKey(id)) isArrow(vars.get(id)) else Set()
    fns ++ mArrowVar
  }

}
