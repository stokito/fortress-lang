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
import edu.rice.cs.plt.collect.CollectUtil
import edu.rice.cs.plt.collect.Relation
import edu.rice.cs.plt.tuple.{Option => JavaOption}

import com.sun.fortress.compiler.GlobalEnvironment
import com.sun.fortress.compiler.index.ApiIndex
import com.sun.fortress.compiler.index.ComponentIndex
import com.sun.fortress.compiler.index.ParametricOperator
import com.sun.fortress.compiler.index.{Constructor => JavaConstructor}
import com.sun.fortress.compiler.index.{DeclaredFunction => JavaDeclaredFunction}
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
import com.sun.fortress.repository.FortressRepository
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.useful._
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.Sets._
import com.sun.fortress.scala_src.typechecker.OverloadingChecker._
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
                     globalEnv: GlobalEnvironment,
                     repository: FortressRepository): JavaList[StaticError] = {
        val errors = new ArrayList[StaticError]()
        val componentName = component.ast.getName
        var missingDecls  = List[ASTNode]()
        var multipleDecls = List[(String,String)]()
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
        for ( e <- toSet(component.exports) ) {
            val api = try { globalEnv.api(e) }
                      catch { case _ => repository.getApi(e) }
            val apiName = api.ast.getName

            /* Multiple APIs exported by a single component cannot include
             * declarations with the same name and kind.
             */
            val apiVariables = toSet(api.variables.keySet)
            for ( v <- apiVariables ) {
                if ( declaredVariables.keySet.contains(v) )
                    multipleDecls = (declaredVariables.get(v).get + "." + v,
                                     apiName + "." + v) :: multipleDecls
                else {
                    val kv = (v, apiName)
                    declaredVariables = declaredVariables + kv
                }
            }
            for ( f <- toSet(api.functions.firstSet) ) {
                if ( declaredFunctions.keySet.contains(f) )
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
                    multipleDecls = (declaredParametricOperators.get(name).get + "." +
                                     name, apiName + "." + name) :: multipleDecls
                else {
                    val kv = (name, apiName)
                    declaredParametricOperators = declaredParametricOperators + kv
                }
            }
            for ( t <- toSet(api.typeConses.keySet) ) {
                if ( declaredTypeConses.keySet.contains(t) )
                    multipleDecls = (declaredTypeConses.get(t).get + "." + t,
                                     apiName + "." + t) :: multipleDecls
                else {
                    val kv = (t, apiName)
                    declaredTypeConses = declaredTypeConses + kv
                }
            }
            for ( d <- toSet(api.dimensions.keySet) ) {
                if ( declaredDimensions.keySet.contains(d) )
                    multipleDecls = (declaredDimensions.get(d).get + "." + d,
                                     apiName + "." + d) :: multipleDecls
                else {
                    val kv = (d, apiName)
                    declaredDimensions = declaredDimensions + kv
                }
            }
            for ( u <- toSet(api.units.keySet) ) {
                if ( declaredUnits.keySet.contains(u) )
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
                        case (DeclaredVariable(lvalueInAPI),
                              DeclaredVariable(lvalueInComp)) =>
                            // should be with the same type and the same mutability
                            if ( ! equalOptTypes(toOption(lvalueInAPI.getIdType),
                                                 toOption(lvalueInComp.getIdType)) ||
                                 ! lvalueInAPI.getMods.equals(lvalueInComp.getMods) ||
                                 lvalueInAPI.isMutable != lvalueInComp.isMutable )
                                missingDecls = lvalueInAPI :: missingDecls
                        case _ => // non-DeclaredVariable:
                                  //   ParamVariable or SingletonVariable
                    }
                } else {
                    api.variables.get(v) match {
                        case DeclaredVariable(lvalue) =>
                            missingDecls = lvalue :: missingDecls
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
                  if isDeclaredName(f) ) {
                // f should be in this component
                if ( fnsInComp.firstSet.contains(f) ) {
                    // Assumes that coverOverloading returns
                    // a function declaration that covers
                    // the given set of overloaded function declarations.
                    (coverOverloading(fnsInAPI.matchFirst(f)),
                     coverOverloading(fnsInComp.matchFirst(f))) match {
                        case (DeclaredFunction(fd@FnDecl(_,l,_,_,_)),
                              DeclaredFunction(FnDecl(_,r,_,_,_))) =>
                            if ( ! equalFnHeaders(l, r, false) )
                                missingDecls = fd :: missingDecls
                        // If they are not DeclaredFunction, skip.
                        case _ =>
                    }
                } else {
                    for ( d <- toSet(fnsInAPI.matchFirst(f)) ) {
                        d match {
                            case DeclaredFunction(fd) =>
                                missingDecls = fd :: missingDecls
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
                    if ( ! equalTraitTypeHeaders(declInAPI.getHeader,
                                                 declInComp.getHeader) ||
                         ( NodeUtil.isTrait(traitOrObject) &&
                           ( ! equalListTypes(toList(NodeUtil.getExcludesClause(declInAPI)),
                                              toList(NodeUtil.getExcludesClause(declInComp))) ||
                             ! equalOptListTypes(toOptList(NodeUtil.getComprisesClause(declInAPI)),
                                                 toOptList(NodeUtil.getComprisesClause(declInComp))) ||
                             NodeUtil.isComprisesEllipses(declInAPI) !=
                             NodeUtil.isComprisesEllipses(declInComp) ) ) ||
                         ( NodeUtil.isObject(traitOrObject) &&
                           ! equalOptListParams(toOptList(NodeUtil.getParams(declInAPI)),
                                                toOptList(NodeUtil.getParams(declInComp))) ) )
                        missingDecls = declInAPI :: missingDecls
                } else missingDecls = declInAPI :: missingDecls
            }

            // Collect the error messages for the missing declarations.
            if ( ! missingDecls.isEmpty ) {
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
                var message = "" + multipleDecls.head
                for ( f <- multipleDecls.tail )
                    message += ",\n                           " + f
                error(errors, componentName, "Component " + componentName +
                      " defines multiple declarations.\n" +
                      "    Multiple declarations: {" + message + "}")
            }
        }
        errors
    }

    private def getMessage(n: ASTNode) =
        if ( NodeUtil.isTraitObjectDecl(n) ) n.toString
        else n.toString + " at " + NodeUtil.getSpan(n)

    private def error(errors: JavaList[StaticError], loc: HasAt, msg: String) =
        errors.add(TypeError.make(msg, loc))

    private def error(errors: JavaList[StaticError], loc: String, msg: String) =
        errors.add(TypeError.make(msg, loc.toString()))

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
    private def equalTypes(left: Type, right: Type): Boolean =
        (left, right) match {
            case (AnyType(_), AnyType(_)) => true
            case (BottomType(_), BottomType(_)) => true
            case (VarType(_, nameL, _), VarType(_, nameR, _)) =>
                equalIds(nameL, nameR)
            case (TraitType(_, nameL, argsL, paramsL),
                  TraitType(_, nameR, argsR, paramsR)) =>
                equalIds(nameL, nameR) &&
                equalListStaticArgs(argsL, argsR) &&
                equalListStaticParams(paramsL, paramsR)
            case (TupleType(_, elmsL, varargsL, kwdL),
                  TupleType(_, elmsR, varargsR, kwdR)) =>
                equalListTypes(elmsL, elmsR) &&
                equalOptTypes(varargsL, varargsR) &&
                equalListKeywordTypes(kwdL, kwdR)
            case (ArrowType(_, domL, ranL, effL),
                  ArrowType(_, domR, ranR, effR)) =>
                  equalTypes(domL, domR) &&
                  equalTypes(ranL, ranR) &&
                  equalEffects(effL, effR)
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
        List.forall2(left, right)((l,r) => equalTypes(l,r))

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
        List.forall2(left, right)((l,r) => equalKeywordTypes(l,r))

    /* Returns true if two keyword types are same. */
    private def equalKeywordTypes(left: KeywordType, right: KeywordType): Boolean =
        (left, right) match {
            case (KeywordType(_, nameL, typeL), KeywordType(_, nameR, typeR)) =>
                equalIds(nameL, nameR) && equalTypes(typeL, typeR)
        }

    /* Returns true if two effects are same. */
    private def equalEffects(left: Effect, right: Effect): Boolean =
        (left, right) match {
            case (Effect(_, throwsL, ioL), Effect(_, throwsR, ioR)) =>
                equalOptListTypes(throwsL, throwsR) &&
                ioL == ioR
        }

    /* Returns true if two IdOrOps denote the same type. */
    private def equalIdOrOps(left: IdOrOp, right: IdOrOp): Boolean =
        (left, right) match {
            case (idl@Id(_,_,_), idr@Id(_,_,_)) => equalIds(idl, idr)
            case (Op(_, apiL, textL, fixityL, enclosingL),
                  Op(_, apiR, textR, fixityR, enclosingR)) =>
                equalOptAPINames(apiL, apiR) && textL == textR &&
                fixityL == fixityR && enclosingL == enclosingR
            case _ => false
        }

    /* Returns true if two Ids denote the same type. */
    private def equalIds(left: Id, right: Id): Boolean =
        (left, right) match {
            case (Id(_, apiL, textL), Id(_, apiR, textR)) =>
                equalOptAPINames(apiL, apiR) && textL == textR
        }

    /* Returns true if two optional APINames are same. */
    private def equalOptAPINames(left: Option[APIName],
                                 right: Option[APIName]): Boolean =
        (left, right) match {
            case (None, None) => true
            case (Some(APIName(_, idsL, _)), Some(APIName(_, idsR, _))) =>
                List.forall2(idsL, idsR)((l,r) => equalIds(l,r))
            case _ => false
        }

    /* Returns true if two FnHeaders are same. */
    private def equalFnHeaders(left: FnHeader, right: FnHeader,
                               ignoreAbstract: boolean): Boolean =
        (left, right) match {
            case (FnHeader(sparamsL, modsL, _, whereL, throwsL, contractL,
                           paramsL, retTyL),
                  FnHeader(sparamsR, modsR, _, whereR, throwsR, contractR,
                           paramsR, retTyR)) =>
                equalListStaticParams(sparamsL, sparamsR) &&
                ( if (ignoreAbstract)
                      modsL.remove(Modifiers.Abstract).equals(modsR.remove(Modifiers.Abstract))
                  else modsL.equals(modsR) ) &&
                equalOptListTypes(throwsL, throwsR) &&
                equalListParams(paramsL, paramsR) && equalOptTypes(retTyL, retTyR)
    }

    /* Returns true if two lists of static parameters are same. */
    private def equalListStaticParams(left: List[StaticParam],
                                      right: List[StaticParam]): Boolean =
        left.length == right.length &&
        List.forall2(left, right)((l,r) => equalStaticParams(l,r))

    /* Returns true if two static parameters are same. */
    private def equalStaticParams(left: StaticParam, right: StaticParam): Boolean =
        (left, right) match {
            case (StaticParam(_, nameL, extendsL, dimL, absorbsL, kindL),
                  StaticParam(_, nameR, extendsR, dimR, absorbsR, kindR)) =>
                equalIdOrOps(nameL, nameR) && equalListTypes(extendsL, extendsR) &&
                equalOptTypes(dimL, dimR) &&
                absorbsL == absorbsR && kindL == kindR
        }

    /* Returns true if two lists of static arguments are same. */
    private def equalListStaticArgs(left: List[StaticArg],
                                    right: List[StaticArg]): Boolean =
        left.length == right.length &&
        List.forall2(left, right)((l,r) => equalStaticArgs(l,r))

    /* Returns true if two static arguments are same. */
    private def equalStaticArgs(left: StaticArg, right: StaticArg): Boolean =
        (left, right) match {
            case (TypeArg(_, typeL), TypeArg(_, typeR)) => equalTypes(typeL, typeR)
            case _ => false
        }

    /* Returns true if two parameters are same. */
    private def equalParams(left: Param, right: Param): Boolean =
        (left, right) match {
            case (Param(_, nameL, modsL, typeL, initL, varargsL),
                  Param(_, nameR, modsR, typeR, initR, varargsR)) =>
                modsL.equals(modsR) &&
                equalOptTypes(typeL, typeR) && equalOptTypes(varargsL, varargsR)
        }

    /* Returns true if two lists of parameters are same. */
    private def equalListParams(left: List[Param], right: List[Param]): Boolean =
        left.length == right.length &&
        List.forall2(left, right)((l,r) => equalParams(l,r))

    /* Returns true if two optional lists of parameters are same. */
    private def equalOptListParams(left: Option[List[Param]],
                                   right: Option[List[Param]]): Boolean =
        (left, right) match {
            case (None, None) => true
            case (Some(paramL), Some(paramR)) => equalListParams(paramL, paramR)
            case _ => false
        }

    /* Returns true if two TraitTypeHeaders are same. */
    private def equalTraitTypeHeaders(left: TraitTypeHeader,
                                      right: TraitTypeHeader): Boolean =
        (left, right) match {
            case (TraitTypeHeader(sparamsL, modsL, _, whereL, throwsL, contractL,
                                  extendsL, declsL),
                  TraitTypeHeader(sparamsR, modsR, _, whereR, throwsR, contractR,
                                  extendsR, declsR)) =>
                equalListStaticParams(sparamsL, sparamsR) && modsL.equals(modsR) &&
                equalOptListTypes(throwsL, throwsR) &&
                equalListTraitTypeWheres(extendsL, extendsR) &&
                equalListMembers(declsL, declsR)
    }

    /* Returns true if two lists of TraitTypeWheres are same. */
    private def equalListTraitTypeWheres(left: List[TraitTypeWhere],
                                         right: List[TraitTypeWhere]): Boolean =
        left.length == right.length &&
        List.forall2(left, right)((l,r) => equalTraitTypeWheres(l,r))

    /* Returns true if two TraitTypeWheres are same. */
    private def equalTraitTypeWheres(left: TraitTypeWhere,
                                     right: TraitTypeWhere): Boolean =
        (left, right) match {
            case (TraitTypeWhere(_, typeL, whereL),
                  TraitTypeWhere(_, typeR, whereR)) =>
                equalTypes(typeL, typeR)
    }

    /* Returns true if two lists of members in traits and objects are same. */
    private def equalListMembers(left: List[Decl], right: List[Decl]): Boolean =
        left.length == right.length &&
        List.forall2(left, right)((l,r) => equalMember(l,r))

    /* Returns true if two members in traits and objects are same. */
    private def equalMember(left: Decl, right: Decl): Boolean =
        (left, right) match {
            case (VarDecl(_, lhsL, _), VarDecl(_, lhsR, _)) =>
                equalListLValues(lhsL, lhsR)
            case (FnDecl(_,headerL,_,_,_), FnDecl(_,headerR,_,_,_)) =>
                equalFnHeaders(headerL, headerR, true)
    }

    /* Returns true if two lists of LValues are same. */
    private def equalListLValues(left: List[LValue], right: List[LValue]): Boolean =
        left.length == right.length &&
        List.forall2(left, right)((l,r) => equalLValue(l,r))

    /* Returns true if two LValues are same. */
    private def equalLValue(left: LValue, right: LValue): Boolean =
        (left, right) match {
            case (LValue(_, nameL, modsL, typeL, _),
                  LValue(_, nameR, modsR, typeR, _)) =>
                equalIds(nameL, nameR) && modsL.equals(modsR)
                equalOptTypes(typeL, typeR)
    }

    private def toOptList[T](ol: JavaOption[JavaList[T]]): Option[List[T]] =
        if (ol.isNone) None
        else Some(toList(ol.unwrap))
}

/* Extractor Objects
 * In order to use pattern matching over Java classes,
 * the following extractor objects are defined.
 */
/* com.sun.fortress.compiler.index.Variable
 *     comprises { DeclaredVariable, ParamVariable, SingletonVariable }
 */
object DeclaredVariable {
    def unapply(variable:JavaDeclaredVariable) =
        Some(variable.ast)
    def apply(lvalue:LValue) =
        new JavaDeclaredVariable(lvalue)
}

object ParamVariable {
    def unapply(variable:JavaParamVariable) =
        Some(variable.ast)
    def apply(param:Param) =
        new JavaParamVariable(param)
}

object SingletonVariable {
    def unapply(variable:JavaSingletonVariable) =
        Some(variable.declaringTrait)
    def apply(id:Id) =
        new JavaSingletonVariable(id)
}

/* com.sun.fortress.compiler.index.Functional
 *     comprises { Function, Method }
 * Function
 *     comprises { Constructor, DeclaredFunction, FunctionalMethod }
 * Method
 *     comprises { DeclaredMethod, FieldGetterMethod, FieldSetterMethod }
 * FunctionalMethod
 *     comprises { ParametricOperator }
 */
object Constructor {
    def unapply(function:JavaConstructor) =
        Some((function.declaringTrait, function.staticParameters,
              JavaOption.wrap(function.parameters),
              JavaOption.wrap(function.thrownTypes),
              function.where))
    def apply(id:Id, staticParams: JavaList[StaticParam],
              params: JavaOption[JavaList[Param]],
              throwsClause: JavaOption[JavaList[BaseType]],
              where: JavaOption[WhereClause]) =
        new JavaConstructor(id, staticParams, params, throwsClause, where)
}

object DeclaredFunction {
    def unapply(function:JavaDeclaredFunction) =
        Some(function.ast)
    def apply(fndecl:FnDecl) =
        new JavaDeclaredFunction(fndecl)
}

object FunctionalMethod {
    def unapply(function:JavaFunctionalMethod) =
        Some(function.ast, function.declaringTrait)
    def apply(fndecl:FnDecl, id:Id) =
        new JavaFunctionalMethod(fndecl, id)
}
