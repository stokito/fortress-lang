/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.scala_src.typechecker

import _root_.java.util.ArrayList
import _root_.java.util.{List => JavaList}
import _root_.java.util.{Set => JavaSet}
import edu.rice.cs.plt.collect.CollectUtil
import edu.rice.cs.plt.collect.Relation
import edu.rice.cs.plt.tuple.{Option => JavaOption}
import com.sun.fortress.compiler.GlobalEnvironment
import com.sun.fortress.compiler.index.ApiIndex
import com.sun.fortress.compiler.index.ComponentIndex
import com.sun.fortress.compiler.index.ParametricOperator
import com.sun.fortress.compiler.index.{Constructor => JavaConstructor}
import com.sun.fortress.compiler.index.{DeclaredFunction => JavaDeclaredFunction}
import com.sun.fortress.compiler.index.{DeclaredMethod => JavaDeclaredMethod}
import com.sun.fortress.compiler.index.{FieldGetterMethod => JavaFieldGetterMethod}
import com.sun.fortress.compiler.index.{FieldSetterMethod => JavaFieldSetterMethod}
import com.sun.fortress.compiler.index.{DeclaredVariable => JavaDeclaredVariable}
import com.sun.fortress.compiler.index.{Dimension => JavaDimension}
import com.sun.fortress.compiler.index.{Function => JavaFunction}
import com.sun.fortress.compiler.index.{FunctionalMethod => JavaFunctionalMethod}
import com.sun.fortress.compiler.index.{ParamVariable => JavaParamVariable}
import com.sun.fortress.compiler.index.{SingletonVariable => JavaSingletonVariable}
import com.sun.fortress.compiler.index.{TypeAliasIndex => JavaTypeAliasIndex}
import com.sun.fortress.compiler.index.{Unit => JavaUnit}
import com.sun.fortress.exceptions.InterpreterBug
import com.sun.fortress.exceptions.StaticError
import com.sun.fortress.exceptions.TypeError
import com.sun.fortress.exceptions.InterpreterBug.bug
import com.sun.fortress.repository.FortressRepository
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.overloading.OverloadingOracle
import com.sun.fortress.scala_src.types.TypeAnalyzer
import com.sun.fortress.scala_src.useful._
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.Sets._
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.Modifiers
import com.sun.fortress.nodes_util.NodeFactory
import com.sun.fortress.nodes_util.NodeUtil
import com.sun.fortress.nodes_util.Span
import com.sun.fortress.parser_util.IdentifierUtil
import com.sun.fortress.useful.HasAt

/* Check the set of exported APIs in this component.
 * Implements the semantics of export statements described
 * in Section 20.2.2 "Export Statements"
 * in the Fortress language specification Version 1.0.
 *
 * The following declarations are not checked yet:
 *     type aliases
 *     dimensions
 *     units
 *     tests
 *     properties
 *
 * The following types are ignored for now:
 *     TaggedDimType
 *     TaggedUnitType
 *
 * The following static arguments are ignored for now:
 *     IntArg
 *     BoolArg
 *     OpArg
 *     DimArg
 *     UnitArg
 *
 * Where clauses, contracts, and default expressions are ignored for now.
 */

/* From the spec:
 * "Export statements specify the APIs that a component exports.
 *  One important restriction on components is that
 *  no API may be both imported and exported by the same component.
 *  This restriction helps to avoid some (but not all)
 *  accidental cyclic dependencies."
 *
 * -- checked by com.sun.fortress.compiler.Disambiguator.checkExports
 */
object ExportChecker {

  /* Called by com.sun.fortress.compiler.StaticChecker.checkComponent */
  def checkExports(component: ComponentIndex,
                   globalEnv: GlobalEnvironment): JavaList[StaticError] = {
    val overloadingChecker = new OverloadingChecker(component, globalEnv)
    val errors = new ArrayList[StaticError]()
    val componentName = component.ast.getName
    var missingDecls  = List[ASTNode]()
    var multipleDecls = List[(String,String)]()
    var wrongDecls  = List[(ASTNode,String)]()
    var declaredVariables           = Map[IdOrOpOrAnonymousName,APIName]()
    var declaredFunctions           = Map[IdOrOpOrAnonymousName,APIName]()
    var declaredParametricOperators = Map[IdOrOpOrAnonymousName,APIName]()
    var declaredTypeConses          = Map[IdOrOpOrAnonymousName,APIName]()
    var declaredDimensions          = Map[IdOrOpOrAnonymousName,APIName]()
    var declaredUnits               = Map[IdOrOpOrAnonymousName,APIName]()
    /* From the spec:
     * "A component must provide a declaration, or a set of declarations,
     *  that satisfies every top-level declaration in any API
     *  that it exports, as described below.
     *  A component may include declarations that do not participate in
     *  satisfying any exported declaration (i.e., a declaration of
     *  any exported API)."
     */
    var exports = List[APIName]()
    for ( e <- toSet(component.exports) ) exports = e :: exports
    for ( e <- exports.sortWith((a1,a2) => (a1.getText compareTo a2.getText) < 0) ) {
      // We are assured at this point that all exported API names refer to an
      // API in the globalEnv
      val api = globalEnv.api(e)
      val apiName = api.ast.getName

      /* Multiple APIs exported by a single component cannot include
       * declarations with the same name and kind.
       */
      val apiVariables = toSet(api.variables.keySet)
      for ( v <- apiVariables ) {
        if ( declaredVariables.keySet.contains(v) )
          // If the two matched APIs are related via containment,
          // there is no error.
          if (!((globalEnv.contains(declaredVariables.get(v).get, apiName)) ||
                (globalEnv.contains(apiName, declaredVariables.get(v).get))))
            multipleDecls = (declaredVariables.get(v).get + "." + v,
                             apiName + "." + v) :: multipleDecls
          else {
            val kv = (v, apiName)
            declaredVariables = declaredVariables + kv
          }
      }
      for ( f <- toSet(api.functions.firstSet) ) {
        if ( declaredFunctions.keySet.contains(f) )
          // If the two matched APIs are related via containment,
          // there is no error.
          if (!((globalEnv.contains(declaredFunctions.get(f).get, apiName)) ||
                (globalEnv.contains(apiName, declaredFunctions.get(f).get))))
            multipleDecls = (declaredFunctions.get(f).get + "." + f,
                             apiName + "." + f) :: multipleDecls
          else {
            val kv = (f, apiName)
            declaredFunctions = declaredFunctions + kv
          }
      }
      for ( o <- toSet(api.parametricOperators) ) {
        val name = o.name
        if ( declaredParametricOperators.keySet.contains(name) )
          // If the two matched APIs are related via containment,
          // there is no error.
          if (!((globalEnv.contains(declaredParametricOperators.get(name).get, apiName)) ||
                (globalEnv.contains(apiName, declaredParametricOperators.get(name).get))))
            multipleDecls = (declaredParametricOperators.get(name).get + "." +
                             name, apiName + "." + name) :: multipleDecls
          else {
            val kv = (name, apiName)
            declaredParametricOperators = declaredParametricOperators + kv
          }
      }
      for ( t <- toSet(api.typeConses.keySet) ) {
        if ( declaredTypeConses.keySet.contains(t) ) {
          // If the two matched APIs are related via containment,
          // there is no error.
          if (!((globalEnv.contains(declaredTypeConses.get(t).get, apiName)) ||
                (globalEnv.contains(apiName, declaredTypeConses.get(t).get))))
            multipleDecls = (declaredTypeConses.get(t).get + "." + t,
                             apiName + "." + t) :: multipleDecls
          }
        else {
          val kv = (t, apiName)
          declaredTypeConses = declaredTypeConses + kv
        }
      }
      for ( d <- toSet(api.dimensions.keySet) ) {
        if ( declaredDimensions.keySet.contains(d) )
          // If the two matched APIs are related via containment,
          // there is no error.
          if (!((globalEnv.contains(declaredDimensions.get(d).get, apiName)) ||
                (globalEnv.contains(apiName, declaredDimensions.get(d).get))))
            multipleDecls = (declaredDimensions.get(d).get + "." + d,
                             apiName + "." + d) :: multipleDecls
          else {
            val kv = (d, apiName)
            declaredDimensions = declaredDimensions + kv
          }
      }
      for ( u <- toSet(api.units.keySet) ) {
        if ( declaredUnits.keySet.contains(u) )
          // If the two matched APIs are related via containment,
          // there is no error.
          if (!((globalEnv.contains(declaredUnits.get(u).get, apiName)) ||
                (globalEnv.contains(apiName, declaredUnits.get(u).get))))
            multipleDecls = (declaredUnits.get(u).get + "." + u,
                             apiName + "." + u) :: multipleDecls
          else {
            val kv = (u, apiName)
            declaredUnits = declaredUnits + kv
          }
      }

      /* From the spec:
       * "A top-level variable declaration declaring a single variable
       *  is satisfied by any top-level variable declaration that declares
       *  the name with the same type (in the component, the type may be
       *  inferred).  A top-level variable declaration declaring
       *  multiple variables is satisfied by a set of declarations (possibly
       *  just one) that declare all the names with their respective types
       *  (which again, may be inferred).  In either case, the modifiers
       *  including the mutability of a variable must be the same in
       *  the exported and satisfying declarations."
       */
      val vsInComp = component.variables.keySet
      for ( v <- apiVariables ) {
        // v should be in this component
        if ( vsInComp.contains(v) ) {
          (api.variables.get(v), component.variables.get(v)) match {
            case (SDeclaredVariable(lvalueInAPI),
                  SDeclaredVariable(lvalueInComp)) =>
              val ltopt = NodeUtil.optTypeOrPatternToType(lvalueInAPI.getIdType)
              val rtopt = NodeUtil.optTypeOrPatternToType(lvalueInComp.getIdType)
              // should be with the same type and the same mutability
              val diffType = ! equalOptTypes(ltopt, rtopt)
              val diffMods = ! lvalueInAPI.getMods.equals(lvalueInComp.getMods)
              val diffMuts = ! lvalueInAPI.isMutable == lvalueInComp.isMutable
              var cause = ""
              if ( diffType ) cause = addMessage(cause, "different types")
              if ( diffMods ) cause = addMessage(cause, "different modifiers")
              if ( diffMuts ) cause = addMessage(cause, "different mutabilities")
              if ( diffType || diffMods || diffMuts )
                wrongDecls = (lvalueInAPI, cause) :: wrongDecls
            case _ => // non-DeclaredVariable:
                      //   ParamVariable or SingletonVariable
          }
        } else {
          api.variables.get(v) match {
            case SDeclaredVariable(lvalue) =>
              // possibly abstract function declarations
              if ( lvalue.getIdType.unwrap.isInstanceOf[ArrowType] &&
                   component.functions.firstSet.contains(v) ) {
                // no overloading for abstract function declarations for now
                val overloadingInComp =
                  overloadingChecker.coverOverloading(toSet(component.functions.matchFirst(v)))
                if ( ! existsMatching(lvalue, overloadingInComp) ) {
                  TypeError.b2(overloadingInComp, v);
                  missingDecls = lvalue :: missingDecls
                }
              } else {
                TypeError.b2(vsInComp, v);
                missingDecls = lvalue :: missingDecls
              }
            case _ => // non-DeclaredVariable
                      //   ParamVariable or SingletonVariable
          }
        }
      }

      /* From the spec:
       * "For functional declarations, recall that several functional
       *  declarations may define the same entity (i.e., they may be
       *  overloaded).  Given a set of overloaded declarations,
       *  it is not permitted to export some of them and not others."
       */
      val fnsInAPI  = api.functions
      val fnsInComp = component.functions
      for ( f <- toSet(fnsInAPI.firstSet) ;
            if overloadingChecker.isDeclaredName(f) ) {
        // f should be in this component
        if ( fnsInComp.firstSet.contains(f) ) {
          val overloadingInAPI =
            overloadingChecker.coverOverloading(toSet(fnsInAPI.matchFirst(f)))
          val compFns = toSet(fnsInComp.matchFirst(f))
          val overloadingInComp =
            overloadingChecker.coverOverloading(compFns)
// 	  if (f.isInstanceOf[IdOrOp] && f.asInstanceOf[IdOrOp].getText.contains("println")) {
// 	        System.out.println("API overloadingInAPI:\n" + overloadingInAPI)
// 		System.out.println("Component overloading:\n" + overloadingInComp)
// 		System.out.println(fnsInComp.firstSet)
// 		System.out.println(fnsInComp)
//           }     
          for ( g <- overloadingInAPI ) {
            if ( ! existsMatching(g, overloadingInComp) ) {
              // This junk is all here because Scala does not play nice with
              // the Eclipse debugger.  Since the bug is a Heisenbug, the
              // offending code is repeated several times to give multiple
              // shots at figuring it out when it finally goes wrong.
              val oicTest =
                   overloadingChecker.coverOverloading(compFns)
              TypeError.b5(overloadingInComp, oicTest, g, f, compFns);
              val oicTest2 =
                   overloadingChecker.coverOverloading(compFns)
              TypeError.b0();
              val oicTest3 =
                   overloadingChecker.coverOverloading(compFns)
              TypeError.b0();
              val oicTest4 =
                   overloadingChecker.coverOverloading(compFns)
              TypeError.b0();
              val oicTest5 =
                   overloadingChecker.coverOverloading(compFns)
              TypeError.b0();
              missingDecls = g :: missingDecls
            }
          }
          
        } else {
          for ( d <- toSet(fnsInAPI.matchFirst(f)) ) {
            d match {
              case SDeclaredFunction(fd) => {
                TypeError.b2(fnsInAPI, fd);
                missingDecls = fd :: missingDecls
              }
              case _ =>
            }
          }
        }
      }

      /* From the spec:
       * "A trait or object declaration is satisfied by a declaration that
       *  has the same header, and contains, for each field declaration
       *  and non-abstract method declaration in the exported declaration,
       *  a satisfying declaration (or a set of declarations).
       *  When a trait has an abstract method declared, a satisfying trait
       *  declaration is allowed to provide a concrete declaration.
       *
       *  A satisfying trait or object declaration may contain method and
       *  field declarations not exported by the API but these
       *  might not be overloaded with method or field declarations provided
       *  by (contained in or inherited by) any declarations
       *  exported by the API."
       */
      val typesInAPI  = api.typeConses
      val typesInComp = component.typeConses
      for ( t <- toSet(typesInAPI.keySet) ;
            if NodeUtil.isTraitOrObject(typesInAPI.get(t)) ) {
        // t should be in this component
        val declInAPI = NodeUtil.getDecl(typesInAPI.get(t))
        if ( typesInComp.keySet.contains(t) ) {
          val traitOrObject = typesInComp.get(t)
          val declInComp = NodeUtil.getDecl(traitOrObject)
          val equalHeaders = equalTraitTypeHeaders(declInAPI.getHeader,
                                                   declInComp.getHeader)
          var cause = ""
          if ( ! equalHeaders._1 )
              cause = addMessage(cause, equalHeaders._2)
          if (NodeUtil.isTrait(traitOrObject)) {
            if (!declInAPI.isInstanceOf[TraitDecl]) {
              cause = addMessage(cause, "non-trait in api, but trait in component")
            } else {
              if (! equalListTypes(toListFromImmutable(NodeUtil.getExcludesClause(declInAPI)),
                                   toListFromImmutable(NodeUtil.getExcludesClause(declInComp)))) {
                cause = addMessage(cause, "different excludes clauses for traits")
              }
              if (! equalComprises(declInAPI, declInComp)) {
                cause = addMessage(cause, "different comprises clauses for traits")
              }
            }
          } else if (NodeUtil.isObject(traitOrObject)) {
            if (declInAPI.isInstanceOf[TraitDecl]) {
              if ( ! isEmptyComprisesEllipses(declInAPI)) {
                cause = addMessage(cause, "trait in api must comprise {...} to match object in component")
              }
            } else if (! equalOptListParams(toOptList(NodeUtil.getParams(declInAPI)),
                                            toOptList(NodeUtil.getParams(declInComp)))) {
              cause = addMessage(cause, "different parameters for object constructors")
            }
          }
          if ( ! cause.equals("") )
            wrongDecls = (declInAPI, cause) :: wrongDecls
        } else {
          TypeError.b2(typesInComp.keySet, t);
          missingDecls = declInAPI :: missingDecls
        }
      }

      def toString(x:ASTNode) = x.getInfo.getSpan.toString
      // Collect the error messages for the missing declarations.
      if ( ! missingDecls.isEmpty ) {
        def comp(x:ASTNode,y:ASTNode) = toString(x) < toString(y)
        missingDecls = missingDecls.sortWith(comp)
        var message = "" + getMessage(missingDecls.head)
        for ( f <- missingDecls.tail )
          message += ",\n                           " + getMessage(f)
        error(errors, componentName,
              "Component " + componentName + " exports API " + apiName +
              "\n    but does not define all declarations in " + apiName +
              ".\n    Missing declarations: {" + message + "}")
      }

      // Collect the error messages for the multiple declarations.
      if ( ! multipleDecls.isEmpty ) {
        def comp(x:(String,String),y:(String,String)) = x._1 < y._1
        multipleDecls = multipleDecls.sortWith(comp)
        var message = "" + multipleDecls.head
        for ( f <- multipleDecls.tail )
          message += ",\n                           " + f
        error(errors, componentName,
              "Multiple exported API declarations must not be satisfied by a " +
              "single definition.\n" +
              "    Multiple declarations: {" + message + "}")
      }

      // Collect the error messages for the wrong declarations.
      if ( ! wrongDecls.isEmpty ) {
        def comp(x:(ASTNode,String), y:(ASTNode,String)) =
          toString(x._1) < toString(y._1)
        wrongDecls = wrongDecls.sortWith(comp)
        var message = "" + wrongDecls.head
        for ( f <- wrongDecls.tail )
          message += ",\n        " + f
        error(errors, componentName,
              "The following declarations in API " + apiName +
              " are not matched\n    by the declarations in component " +
              componentName + ".\n    Unmatched declarations: {\n        " +
              message + "\n    }")
      }
    }
    errors
  }
  // End of checkExports
  
  private def existsMatching(g: FnDecl, set: Set[FnDecl]): Boolean = {
    for ( f <- set ) {
      if ( equalFnHeaders(g.getHeader, f.getHeader, false) )
        return true
    }
    false
  }

  // Invariant: The first argument g has an arrow type.
  private def existsMatching(g: LValue, set: Set[FnDecl]): Boolean = {
    val arrow = g.getIdType.unwrap.asInstanceOf[ArrowType]
    for ( f <- set ) {
      if ( equalTypes(arrow.getDomain, NodeUtil.getParamType(f)) &&
           equalTypes(arrow.getRange, f.getHeader.getReturnType.unwrap) )
        return true
    }
    false
  }

  private def getMessage(n: ASTNode) =
    if ( NodeUtil.isTraitObjectDecl(n) ) n.toString
    else n.toString + " at " + NodeUtil.getSpan(n)

  private def addMessage(original: String, added: String) =
    if ( added.equals("") ) original
    else if ( original.equals("") &&
              ! added.startsWith("\n         due to ") )
           "\n         due to " + added
         else if ( original.equals("") ) added
         else original + ", " + added

  private def error(errors: JavaList[StaticError], loc: HasAt, msg: String) =
    errors.add(TypeError.make(msg, loc))

  /* Returns true if two types are same.
   * If any of the following types are compared, returns false:
   *
   *     ArrayType        : removed after type checking
   *     MatrixType       : removed after type checking
   *     _InferenceVarType: removed after type checking
   *     TaggedDimType    : not supported yet
   *     TaggedUnitType   : not supported yet
   *     IntersectionType : not in APIs
   *     UnionType        : not in APIs
   *     FixedPointType   : not in APIs
   *     LabelType        : not in APIs
   *     TraitType with non-type static arguments : not supported yet
   */
  def equalTypes(left: Type, right: Type): Boolean =
    (left, right) match {
      case (SAnyType(_), SAnyType(_)) => true
      case (SBottomType(_), SBottomType(_)) => true
      case (SVarType(_, nameL, _), SVarType(_, nameR, _)) =>
        equalIds(nameL, nameR)
      case (STraitSelfType(_, namedL, tysL),
            STraitSelfType(_, namedR, tysR)) =>
        equalTypes(namedL, namedR) &&
        (tysL.isEmpty || equalListTypes(tysL, tysR))
        /*
         * Bug above with tysL.isEmpty -- comprises {...} must match ordinary
         * self type, however both are encoded as an empty comprises list.
         * This seems wrong.
         */
      case (SObjectExprType(_, tysL), SObjectExprType(_, tysR)) =>
        equalListTypes(tysL, tysR)
      case (STraitType(_, nameL, argsL, paramsL),
            STraitType(_, nameR, argsR, paramsR)) =>
        equalIds(nameL, nameR) &&
        equalListStaticArgs(argsL, argsR) &&
        equalListStaticParams(paramsL, paramsR)
      case (STupleType(_, elmsL, varargsL, kwdL),
            STupleType(_, elmsR, varargsR, kwdR)) =>
        equalListTypes(elmsL, elmsR) &&
        equalOptTypes(varargsL, varargsR) &&
        equalListKeywordTypes(kwdL, kwdR)
      case (SArrowType(_, domL, ranL, effL, ioL, miL),
            SArrowType(_, domR, ranR, effR, ioR, miR)) =>
        equalTypes(domL, domR) &&
        equalTypes(ranL, ranR) &&
        equalEffects(effL, effR) &&
        ioL == ioR &&
        equalOptMethodInfos(miL, miR)
      case _ => false
  }

  /* Returns true if two optional types are same. */
  private def equalOptTypes(left: Option[Type], right: Option[Type]): Boolean =
    (left, right) match {
      case (None, None) => true
      case (Some(tyL), Some(tyR)) => equalTypes(tyL, tyR)
      case _ => false
    }

  /* Returns true if two lists of types are same. */
  private def equalListTypes(left: List[Type], right: List[Type]): Boolean =
    left.length == right.length &&
    (left, right).zipped.forall((l,r) => equalTypes(l,r))

  /* Returns true if two optional lists of types are same. */
  private def equalOptListTypes(left: Option[List[Type]],
                                right: Option[List[Type]]): Boolean =
    (left, right) match {
      case (None, None) => true
      case (Some(tyL), Some(tyR)) => equalListTypes(tyL, tyR)
      case _ => false
    }

  /* Returns true if two lists of keyword types are same. */
  private def equalListKeywordTypes(left: List[KeywordType],
                                    right: List[KeywordType]): Boolean =
    left.length == right.length &&
    (left, right).zipped.forall((l,r) => equalKeywordTypes(l,r))

  /* Returns true if two keyword types are same. */
  private def equalKeywordTypes(left: KeywordType, right: KeywordType): Boolean =
    (left, right) match {
      case (SKeywordType(_, nameL, typeL), SKeywordType(_, nameR, typeR)) =>
        equalIds(nameL, nameR) && equalTypes(typeL, typeR)
    }

  /* Returns true if two effects are same. */
  private def equalEffects(left: Effect, right: Effect): Boolean =
    (left, right) match {
      case (SEffect(_, throwsL, ioL), SEffect(_, throwsR, ioR)) =>
        equalOptListTypes(throwsL, throwsR) &&
        ioL == ioR
    }

  /* Returns true if two IdOrOps denote the same type. */
  private def equalIdOrOps(left: IdOrOp, right: IdOrOp): Boolean =
    (left, right) match {
      case (idl@SId(_,_,_), idr@SId(_,_,_)) => equalIds(idl, idr)
      case (SOp(_, apiL, textL, fixityL, enclosingL),
            SOp(_, apiR, textR, fixityR, enclosingR)) =>
        equalOptAPINames(apiL, apiR) && textL == textR &&
        fixityL == fixityR && enclosingL == enclosingR
      case _ => false
    }

  /* Returns true if two Ids denote the same type. */
  private def equalIds(left: Id, right: Id): Boolean =
    (left, right) match {
      case (SId(_, apiL, textL), SId(_, apiR, textR)) =>
        equalOptAPINames(apiL, apiR) && textL == textR
    }

  /* Returns true if two optional method infos are the same. */
  private def equalOptMethodInfos(left: Option[MethodInfo],
                                  right: Option[MethodInfo]): Boolean =
    (left, right) match {
      case (None, None) => true
      case (Some(SMethodInfo(typL, posL)), Some(SMethodInfo(typR, posR))) =>
        equalTypes(typL, typR) && (posL == posR)
      case _ => false
    }

  /* Returns true if two optional APINames are same. */
  private def equalOptAPINames(left: Option[APIName],
                               right: Option[APIName]): Boolean =
    (left, right) match {
      case (None, None) => true
      case (Some(SAPIName(_, idsL, _)), Some(SAPIName(_, idsR, _))) =>
        (idsL, idsR).zipped.forall((l,r) => equalIds(l,r))
      case _ => false
    }

  /* Returns true if two FnHeaders are same. */
  private def equalFnHeaders(left: FnHeader, right: FnHeader,
                             ignoreAbstract: Boolean): Boolean = (left, right) match {
      case (SFnHeader(sparamsL, modsL, _, whereL, throwsL, contractL,
                      paramsL, retTyL),
            SFnHeader(sparamsR, modsR, _, whereR, throwsR, contractR,
                      paramsR, retTyR)) =>
        (equalListStaticParams(sparamsL, sparamsR) &&
          ( if (ignoreAbstract)
              modsL.remove(Modifiers.Abstract).equals(modsR.remove(Modifiers.Abstract))
            else modsL.equals(modsR) ) &&
        equalOptListTypes(throwsL, throwsR) &&
        equalListParams(paramsL, paramsR) && equalOptTypes(retTyL, retTyR))
  }

  /* Returns true if two lists of static parameters are same. */
  private def equalListStaticParams(left: List[StaticParam],
                                    right: List[StaticParam]): Boolean =
    left.length == right.length &&
    (left, right).zipped.forall((l,r) => equalStaticParams(l,r))

  /* Returns true if two static parameters are same. */
  private def equalStaticParams(left: StaticParam, right: StaticParam): Boolean =
    (left, right) match {
      case (SStaticParam(_, varianceL, nameL, extendsL, dominatesL, dimL, absorbsL, kindL, _),
            SStaticParam(_, varianceR, nameR, extendsR, dominatesR, dimR, absorbsR, kindR, _)) =>
        equalIdOrOps(nameL, nameR) && varianceL == varianceR && equalListTypes(extendsL, extendsR) &&
        equalListTypes(dominatesL, dominatesR) &&
        equalOptTypes(dimL, dimR) && 
        absorbsL == absorbsR && kindL == kindR
    }

  /* Returns true if two lists of static arguments are same. */
  private def equalListStaticArgs(left: List[StaticArg],
                                  right: List[StaticArg]): Boolean =
    left.length == right.length &&
    (left, right).zipped.forall((l,r) => equalStaticArgs(l,r))

  /* Returns true if two static arguments are same. */
  private def equalStaticArgs(left: StaticArg, right: StaticArg): Boolean =
    (left, right) match {
      case (STypeArg(_, liftedL, typeL), STypeArg(_, liftedR, typeR)) =>
        equalTypes(typeL, typeR) && liftedL == liftedR
      case (SIntArg(_, liftedL, intL), SIntArg(_, liftedR, intR)) =>
        equalIntExprs(intL,intR) && liftedL == liftedR
      case _ => false
    }

  /* Returns true if two IntExprs are same.
   * Not implemented!
   */
  private def equalIntExprs(left: IntExpr, right: IntExpr): Boolean = false

  /* Returns true if two parameters are same. */
  private def equalParams(left: Param, right: Param): Boolean =
    (left, right) match {
      case (SParam(_, nameL, modsL, typeL, initL, varargsL),
            SParam(_, nameR, modsR, typeR, initR, varargsR)) =>
        modsL.equals(modsR) &&
        ((typeL, typeR) match {
           case (Some(tl@SType(_)), Some(tr@SType(_))) => equalTypes(tl, tr)
           case _ => false
         }) &&
        equalOptTypes(varargsL, varargsR)
    }

  /* Returns true if two lists of parameters are same. */
  private def equalListParams(left: List[Param], right: List[Param]): Boolean =
    left.length == right.length &&
    (left, right).zipped.forall((l,r) => equalParams(l,r))

  /* Returns true if two optional lists of parameters are same. */
  private def equalOptListParams(left: Option[List[Param]],
                                 right: Option[List[Param]]): Boolean =
    (left, right) match {
      case (None, None) => true
      case (Some(paramL), Some(paramR)) => equalListParams(paramL, paramR)
      case _ => false
    }

  /* Returns true if two TraitTypeHeaders are same. */
  private def equalTraitTypeHeaders(inAPI:  TraitTypeHeader,
                                    inComp: TraitTypeHeader): (Boolean, String) =
    (inAPI, inComp) match {
      case (STraitTypeHeader(sparamsL, modsL, _, whereL, throwsL, contractL,
                             extendsL, paramsL, declsL),
            STraitTypeHeader(sparamsR, modsR, _, whereR, throwsR, contractR,
                             extendsR, paramsR, declsR)) =>
        var cause = ""
        if ( ! equalListStaticParams(sparamsL, sparamsR) )
          cause = addMessage(cause, "different static parameters")
        if ( ! modsL.equals(modsR) )
          cause = addMessage(cause, "different modifiers")
        if ( ! equalOptListTypes(throwsL, throwsR) )
          cause = addMessage(cause, "different throws clauses")
        if ( ! equalListTraitTypeWheres(extendsL, extendsR) )
          cause = addMessage(cause, "different extends clauses")
        if ( ! equalOptListParams(paramsL, paramsR) )
          cause = addMessage(cause, "different parameters")
        val equalDecls = equalListMembers(declsL, declsR, cause)
        if ( ! equalDecls._1 )
          cause = addMessage(cause, equalDecls._2)
        val abstractOK = allAbstractsMadePublic(declsL, declsR, cause)
        if ( ! abstractOK._1 )
          cause = addMessage(cause, abstractOK._2)  
        (cause.equals(""), cause)
    }

   /* Returns true if two lists of TraitTypeWheres are same. */
  private def equalListTraitTypeWheres(inAPI:  List[TraitTypeWhere],
                                       inComp: List[TraitTypeWhere]): Boolean =
    inAPI.length == inComp.length &&
    (inAPI, inComp).zipped.forall((l,r) => equalTraitTypeWheres(l,r))

  /* Returns true if two TraitTypeWheres are same. */
  private def equalTraitTypeWheres(inAPI:  TraitTypeWhere,
                                   inComp: TraitTypeWhere): Boolean =
    (inAPI, inComp) match {
      case (STraitTypeWhere(_, typeL, whereL),
            STraitTypeWhere(_, typeR, whereR)) =>
        equalTypes(typeL, typeR)
    }

  /* Returns true if two comprises clauses are "same" in the presence of "..." */
  private def equalComprises(declInAPI:  TraitObjectDecl,
                             declInComp: TraitObjectDecl): Boolean = {
    val comprisesInAPI  = toOptList(NodeUtil.getComprisesClause(declInAPI))
    val comprisesInComp = toOptList(NodeUtil.getComprisesClause(declInComp))
    if ( NodeUtil.isComprisesEllipses(declInAPI) )
      (comprisesInAPI, comprisesInComp) match {
        case (Some(tysInAPI), Some(tysInComp)) =>
          for ( t <- tysInAPI )
            if ( ! tysInComp.contains(t) ) return false
          true
        case (Some(tysInAPI), None) => false
        case _ => true
      }
    else equalOptListTypes(comprisesInAPI, comprisesInComp)
  }

  /* Returns true if declInAPI has simply {...} as a comprises clause. */
  private def isEmptyComprisesEllipses(declInAPI: TraitObjectDecl): Boolean =
    NodeUtil.isComprisesEllipses(declInAPI) &&
    toOptList(NodeUtil.getComprisesClause(declInAPI)).map(_.isEmpty).getOrElse(true)

  /*
   * Returns true if abstract members of a component are declared in the corresponding api  
   */
    
  private def allAbstractsMadePublic(inAPI: List[Decl], inComp: List[Decl], original: String): (Boolean, String) = {
    var cause = original
    (inComp.forall( 
    	d => d match {
    	  case SFnDecl(_,h,_,_,_) => if (h.getMods().isAbstract()) {
    	    val b = inAPI.exists(r => equalMember(d,r))
    	    if (b) true else { cause = addMessage(cause, "Asbtract method " + h.getName() + " @ " + NodeUtil.getSpan(d) + " is not declared in the API") ; false }
    	  }  else true
    	  case _ => true
    	}
        
    ),cause)
  }                  	
                                 
    
    
  /* Returns true if members in traits and objects in an API have
   * corresponding members in the component.
   */
  private def equalListMembers(inAPI: List[Decl], inComp: List[Decl],
                               original: String): (Boolean, String) =
    if ( inAPI.length > inComp.length )
      (false, "missing members of the trait/object in the component")
    else {
      var cause = original
      def handleOne(l: Decl) = {
        val eq = inComp.exists(r => equalMember(l, r))
        if ( ! eq )
          l match {
            case SVarDecl(_,_,_) =>
              cause = addMessage(cause,
                                 "different field @ " + NodeUtil.getSpan(l))
            case SFnDecl(_,h,_,_,_) =>
              val eq2 = inComp.exists(r => equalMember(l, r))
              cause = addMessage(cause,
                                 "different method " + h.getName +
                                 " @ " + NodeUtil.getSpan(l))
          }
        eq
      }
      (inAPI.forall(handleOne), cause)
    }

  /* Returns true if two members in traits and objects are same. */
  private def equalMember(inAPI: Decl, inComp: Decl): Boolean =
    (inAPI, inComp) match {
      case (SVarDecl(_, lhsL, _), SVarDecl(_, lhsR, _)) =>
        equalListLValues(lhsL, lhsR)
      case (SFnDecl(_,headerL,_,_,_), SFnDecl(_,headerR,_,e,_)) =>
/*        if (e.isEmpty) { 
        	val mods = headerR.getMods().combine(Modifiers.Abstract) 
        	headerR match {
        	  case SFnHeader(a,_,c,d,e,f,g,h) => equalFnHeaders(headerL, SFnHeader(a,mods,c,d,e,f,g,h), false) 
        	}         	
        }
        else */ equalFnHeaders(headerL, headerR, false)
      case _ => false
    }

  /* Returns true if two lists of LValues are same. */
  private def equalListLValues(left: List[LValue], right: List[LValue]): Boolean =
    left.length == right.length &&
    (left, right).zipped.forall((l,r) => equalLValue(l,r))

  /* Returns true if two LValues are same. */
  private def equalLValue(left: LValue, right: LValue): Boolean =
    (left, right) match {
      case (SLValue(_, nameL, modsL, typeL, _),
            SLValue(_, nameR, modsR, typeR, _)) =>
        equalIds(nameL, nameR) && modsL.equals(modsR)
        (typeL, typeR) match {
          case (Some(tl@SType(_)), Some(tr@SType(_))) => equalTypes(tl, tr)
          case _ => false
        }
  }

  private def toOptList[T](ol: JavaOption[JavaList[T]]): Option[List[T]] =
    if (ol.isNone) None else Some(toListFromImmutable(ol.unwrap))

    /* Returns the set of overloaded function declarations
     * covering the given set of the overloaded declarations.
     * Invariant: set.size > 1
     */

}
