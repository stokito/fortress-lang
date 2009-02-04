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
import _root_.java.util.Set
import edu.rice.cs.plt.collect.CollectUtil
import edu.rice.cs.plt.collect.Relation
import edu.rice.cs.plt.tuple.{Option => JavaOption}
import scala.collection.jcl.Conversions

import com.sun.fortress.compiler.GlobalEnvironment
import com.sun.fortress.compiler.index.ApiIndex
import com.sun.fortress.compiler.index.ComponentIndex
import com.sun.fortress.compiler.index.{Constructor => JavaConstructor}
import com.sun.fortress.compiler.index.{DeclaredFunction => JavaDeclaredFunction}
import com.sun.fortress.compiler.index.{DeclaredVariable => JavaDeclaredVariable}
import com.sun.fortress.compiler.index.{FunctionalMethod => JavaFunctionalMethod}
import com.sun.fortress.compiler.index.{ParamVariable => JavaParamVariable}
import com.sun.fortress.compiler.index.{SingletonVariable => JavaSingletonVariable}
import com.sun.fortress.exceptions.StaticError
import com.sun.fortress.exceptions.TypeError
import com.sun.fortress.scala_src.useful._
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.nodes._
import com.sun.fortress.parser_util.IdentifierUtil
import com.sun.fortress.useful.HasAt

object ExportChecker {

    /* Check the set of exported APIs in this component.
     * Implements the semantics of export statements
     * described in Section 20.2.2 in the Fortress language
     * specification Version 1.0.
     */
    def checkExports(component: ComponentIndex,
                     globalEnv: GlobalEnvironment): JavaList[StaticError] = {
        val errors = new ArrayList[StaticError]()
        val componentName = component.ast.getName
        var missingDecls = List[Node]()
        for ( e <- Conversions.convertSet(component.exports) ) {
            /* A component must provide a declaration, or a set of declarations,
             * that satisfies every top-level declaration in any API
             * that it exports, as described below.
             */
            val api = globalEnv.api(e)
            val apiName = api.ast.getName
            /* A top-level variable declaration declaring a single variable
             * is satisfied by any top-level variable declaration that declares
             * the name with the same type (in the component, the type may be
             * inferred).  A top-level variable declaration declaring
             * multiple variables is satisfied by a set of declarations (possibly
             * just one) that declare all the names with their respective types
             * (which again, may be inferred).  In either case, the mutability
             * of a variable must be the same in the exported and satisfying
             * declarations.
             */
            val vsInComp = component.variables.keySet
            for ( v <- Conversions.convertSet(api.variables.keySet) ) {
                // v should be in this component
                if ( vsInComp.contains(v) ) {
                    (api.variables.get(v), component.variables.get(v)) match {
                        case (DeclaredVariable(lvalueInAPI),
                              DeclaredVariable(lvalueInComp)) =>
                            // should be with the same type and the same mutability
                            if ( ! equalOptTypes(toOption(lvalueInAPI.getIdType),
                                                 toOption(lvalueInComp.getIdType)) ||
                                 lvalueInAPI.isMutable != lvalueInComp.isMutable )
                                missingDecls = lvalueInAPI :: missingDecls
                        case _ => // non-DeclaredVariable
                    }
                } else {
                    api.variables.get(v) match {
                        case DeclaredVariable(lvalue) =>
                            missingDecls = lvalue :: missingDecls
                        case _ => // non-DeclaredVariable
                    }
                }
            }

            /* For functional declarations, recall that several functional
             * declarations may define the same entity (i.e., they may be
             * overloaded).  Given a set of overloaded declarations,
             * it is not permitted to export some of them and not others.
             */
            val fnsInAPI  = api.functions
            val fnsInComp = component.functions
            val idsInAPI  = fnsInAPI.firstSet
            val idsInComp = fnsInComp.firstSet
            for ( f <- Conversions.convertSet(idsInAPI) ;
                  if isDeclaredFunction(f) ) {
                // f should be in this component
                if ( idsInComp.contains(f) ) {
                    val declsInAPI  = fnsInAPI.matchFirst(f)
                    val declsInComp = fnsInComp.matchFirst(f)
                    // No overloading for now.
                    (declsInAPI.size, declsInComp.size) match {
                        case (1, 1) =>
                            (declsInAPI.iterator.next,
                             declsInComp.iterator.next) match {
                                case (DeclaredFunction(FnDecl(a,l,b,c,d)),
                                      DeclaredFunction(FnDecl(_,r,_,_,_))) =>
                                    if ( ! equalFnHeaders(l, r) )
                                        missingDecls = FnDecl(a,l,b,c,d) :: missingDecls
                                // If they are not DeclaredFunction, skip.
                                case _ =>
                            }
                        // If there is a set of overloaded declarations, skip.
                        case _ =>
                    }
                } else {
                    // No overloading for now.
                    fnsInAPI.matchFirst(f).iterator.next match {
                        case DeclaredFunction(fd) =>
                            missingDecls = fd :: missingDecls
                        case _ =>
                    }
                }
            }
            // Collect the error messages for the missing declarations.
            if ( ! missingDecls.isEmpty ) {
                var message = "" + missingDecls.head
                for ( f <- missingDecls.tail )
                    message += ",\n                           " + f
                error(errors, componentName,
                      "Component " + componentName + " exports API " + apiName +
                      "\n    but does not define all declarations in " + apiName +
                      ".\n    Missing declarations: {" + message + "}")
            }

            /* for the other kinds of top-level declarations
             *
            Set<ParametricOperator> parametricOperators = api.parametricOperators;
            Map<Id, TypeConsIndex> typeConses = api.typeConses;
            Map<Id, Dimension> dimensions = api.dimensions;
            Map<Id, Unit> units = api.units;
            */

            /* from the spec
             *
             * A trait or object declaration is satisfied by a declaration that
             * has the same header, and contains, for each field declaration
             * and non-abstract method declaration in the exported declaration,
             * a satisfying declaration (or a set of declarations).
             * When a trait has an abstract method declared, a satisfying trait
             * declaration is allowed to provide a concrete declaration.
             *
             * A satisfying trait or object declaration may contain method and
             * field declarations not exported by the API but these
             * might not be overloaded with method or field declarations provided
             * by (contained in or inherited by) any declarations
             * exported by the API.
             */
        }
        errors
    }

    private def isDeclaredFunction(f: IdOrOpOrAnonymousName) = f match {
        case Id(_,_,str) => IdentifierUtil.validId(str)
        case _ => false
    }

    private def error(errors: JavaList[StaticError], loc: HasAt, msg: String) =
        errors.add(TypeError.make(msg, loc))

    /* Transforms a Java option to a Scala option */
    private def toOption[T](opt: JavaOption[T]): Option[T] =
        if ( opt.isNone ) None
        else Some( opt.unwrap )

    /* Returns true if two optional types are the same.
     * Should be able to handle arbitrary type equality testing.
     * Only works for NamedType for now.
     */
    private def equalOptTypes(left: Option[Type], right: Option[Type]): boolean =
        (left, right) match {
            case (None, None) => true
            case (Some(tyLeft), Some(tyRight)) => equalTypes(tyLeft, tyRight)
            case _ => false
        }

    /* Returns true if two types are the same.
     * Should be able to handle arbitrary type equality testing.
     * Only works for NamedType for now.
     */
    private def equalTypes(left: Type, right: Type): boolean =
        (left, right) match {
            case (VarType(_,nameLeft,_), VarType(_,nameRight,_)) =>
                equalIds(nameLeft, nameRight)
            /* Should be able to handle TraitTypes with static arguments.
             * Only works for TraitTypes without any static arguments for now.
             */
            case (TraitType(_, nameLeft,  argsLeft,  paramsLeft),
                  TraitType(_, nameRight, argsRight, paramsRight)) =>
                equalIds( nameLeft, nameRight ) &&
                argsLeft.isEmpty && argsRight.isEmpty &&
                paramsLeft.isEmpty && paramsRight.isEmpty
            /* Should be able to handle any TupleTypes.
             * Only works for void types.
             */
            case (TupleType(_, elmsL, varargsL, kwdL),
                  TupleType(_, elmsR, varargsR, kwdR)) =>
                elmsL.isEmpty && varargsL.isNone && kwdL.isEmpty &&
                elmsR.isEmpty && varargsR.isNone && kwdR.isEmpty
            case _ => false
        }

    /* Returns true if two Ids denote the same type. */
    private def equalIds(left: Id, right: Id): boolean =
        (left, right) match {
            case (Id(_, apiLeft,  textLeft),
                  Id(_, apiRight, textRight)) =>
                equalOptAPINames(toOption(apiLeft), toOption(apiRight)) &&
                textLeft == textRight
        }

    /* Returns true if two optional APINames are the same. */
    private def equalOptAPINames(left: Option[APIName],
                                 right: Option[APIName]): boolean =
        (left, right) match {
            case (None, None) => true
            case (Some(APIName(_, idsLeft, _)), Some(APIName(_, idsRight, _))) =>
                List.forall2(Lists.fromJavaList(idsLeft),
                             Lists.fromJavaList(idsRight))((l,r) => equalIds(l,r))
            case _ => false
        }

    /* Returns true if two Params are the same.
     * Should check the modifiers and default expressions
     * Only checks the types for now.
     */
    private def equalParams(left: Param, right: Param): boolean =
        equalOptTypes(toOption(left.getIdType), toOption(right.getIdType)) &&
        equalOptTypes(toOption(left.getVarargsType), toOption(right.getVarargsType))

    /* Returns true if two lists of Params are the same. */
    private def equalListParams(left: List[Param], right: List[Param]): boolean =
        left.length == right.length &&
        List.forall2(left, right)((l,r) => equalParams(l,r))

    /* Returns true if two FnHeaders are the same.
     * Should check the static parameters, modifiers, where clauses,
     * throws clauses, and contracts.
     * Only checks the parameter types and the return types for now.
     */
    private def equalFnHeaders(left: FnHeader, right: FnHeader): boolean =
        equalListParams(Lists.fromJavaList(left.getParams),
                        Lists.fromJavaList(right.getParams)) &&
        equalOptTypes(toOption(left.getReturnType),
                      toOption(right.getReturnType))
}

/* Extractor Objects
 * In order to use pattern matching over Java classes,
 * the following extractor objects are defined.
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
