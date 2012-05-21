/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.scala_src.typechecker

import _root_.java.util.{Map => JavaMap}
import _root_.java.util.{HashMap => JavaHashMap}
import _root_.java.util.{Set => JavaSet}

import edu.rice.cs.plt.collect.Relation
import com.sun.fortress.compiler.index.CompilationUnitIndex
import com.sun.fortress.compiler.index.Functional
import com.sun.fortress.compiler.index.Method
import com.sun.fortress.compiler.index.ProperTraitIndex
import com.sun.fortress.compiler.typechecker.TypeNormalizer
import com.sun.fortress.exceptions.InterpreterBug.bug
import com.sun.fortress.exceptions.StaticError
import com.sun.fortress.exceptions.StaticError.errorMsg
import com.sun.fortress.exceptions.TypeError
import com.sun.fortress.exceptions.ProgramError
import com.sun.fortress.exceptions.ProgramError.error
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.NodeFactory
import com.sun.fortress.nodes_util.Modifiers
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.typechecker.Formula._
import com.sun.fortress.scala_src.typechecker.impls._
import com.sun.fortress.scala_src.typechecker.staticenv.STypeEnv
import com.sun.fortress.scala_src.types.TypeAnalyzer
import com.sun.fortress.scala_src.useful.DummyErrorLog
import com.sun.fortress.scala_src.useful.ErrorLog
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.Sets._
import com.sun.fortress.scala_src.useful.SExprUtil._
import com.sun.fortress.scala_src.useful.SNodeUtil._
import com.sun.fortress.scala_src.useful.TryErrorLog
import com.sun.fortress.useful.HasAt

import scala.collection.mutable.{Map => MMap}

/* Questions
 */
/* Invariants
 * 1. If a subexpression does not have any inferred type,
 *    type checking the subexpression failed.
 */
object STypeCheckerFactory {

  def make(current: CompilationUnitIndex,
           traits: TraitTable,
           env: STypeEnv)
           (implicit analyzer: TypeAnalyzer,
                     cycleChecker: CyclicReferenceChecker): STypeCheckerImpl =
    new STypeCheckerImpl(current, traits, env,
                         new JavaHashMap[Id, Option[JavaSet[Type]]](),
                         new ErrorLog)(analyzer, MMap.empty, cycleChecker)

  def make(current: CompilationUnitIndex,
           traits: TraitTable,
           env: STypeEnv,
           errors: ErrorLog)
          (implicit analyzer: TypeAnalyzer,
                    cycleChecker: CyclicReferenceChecker): STypeCheckerImpl =
    new STypeCheckerImpl(current, traits, env,
                         new JavaHashMap[Id, Option[JavaSet[Type]]](),
                         errors)(analyzer, MMap.empty, cycleChecker)

  def makeTryChecker(current: CompilationUnitIndex,
                     traits: TraitTable,
                     env: STypeEnv)
                    (implicit analyzer: TypeAnalyzer,
                     cycleChecker: CyclicReferenceChecker): TryChecker =
    new TryChecker(current, traits, env,
                   new JavaHashMap[Id, Option[JavaSet[Type]]](), new TryErrorLog)(analyzer, MMap.empty, cycleChecker)

  def makeTryChecker(checker: STypeChecker): TryChecker =
    new TryChecker(
      checker.current,
      checker.traits,
      checker.env,
      checker.labelExitTypes,
      new TryErrorLog)(checker.analyzer, checker.envCache, checker.cycleChecker)

  /**
   * Creates a type checker that does not throw or store errors. This is a bit
   * different from a TryChecker in that it silently doesn't report anything.
   */
  def makeDummyChecker(checker: STypeChecker): STypeCheckerImpl =
    new STypeCheckerImpl(
      checker.current,
      checker.traits,
      checker.env,
      checker.labelExitTypes,
      DummyErrorLog)(checker.analyzer, checker.envCache, checker.cycleChecker)

}

/**
 * A convenient type for an actual type checker instance for use in Java
 * code. This class mixes all the implementation into the abstract base class.
 */
class STypeCheckerImpl(current: CompilationUnitIndex,
                       traits: TraitTable,
                       env: STypeEnv,
                       labelExitTypes: JavaMap[Id, Option[JavaSet[Type]]],
                       errors: ErrorLog)
                      (implicit analyzer: TypeAnalyzer,
                                envCache: MMap[APIName, STypeEnv],
                                cycleChecker: CyclicReferenceChecker)
    extends STypeChecker(current, traits, env, labelExitTypes, errors)
    with Dispatch with Common
    with Decls with Functionals with Operators
    with Misc {

  override def constructor(current: CompilationUnitIndex,
                           traits: TraitTable,
                           env: STypeEnv,
                           labelExitTypes: JavaMap[Id, Option[JavaSet[Type]]],
                           errors: ErrorLog)
                          (implicit analyzer: TypeAnalyzer,
                                    envCache: MMap[APIName, STypeEnv],
                                    cycleChecker: CyclicReferenceChecker) =
  new STypeCheckerImpl(current, traits, env, labelExitTypes, errors)

}

/**
 * The abstract base class for a type checker. This class contains all the
 * basic functionality for use in type checking, including helper methods that
 * are widely used for many different cases. Use the `STypeCheckerImpl` class
 * instead for a fully implemented type checker.
 *
 * The actual type checking is defined abstractly; concrete subclasses or mixed-
 * in traits should provide the actual implementation of type checking each
 * case.
 *
 * Constructor parameters are marked `protected val` so that the implementing,
 * mixed-in traits can access the fields.
 */
abstract class STypeChecker(val current: CompilationUnitIndex,
                            val traits: TraitTable,
                            val env: STypeEnv,
                            val labelExitTypes: JavaMap[Id, Option[JavaSet[Type]]],
                            val errors: ErrorLog)
                           (implicit val analyzer: TypeAnalyzer,
                                     val envCache: MMap[APIName, STypeEnv],
                                     val cycleChecker: CyclicReferenceChecker) {

  /** Oracle for determining coercions between types. */
  protected implicit val coercions = new CoercionOracle(traits, current)

  /**
   * This method simply creates a new instance of the class in which it is defined. We need this
   * so that subclasses will extend themselves properly. This MUST be overriden in every subclass of
   * STypeChecker.
   */
  def constructor(current: CompilationUnitIndex,
                  traits: TraitTable,
                  env: STypeEnv,
                  labelExitTypes: JavaMap[Id, Option[JavaSet[Type]]],                  errors: ErrorLog)
                 (implicit analyzer: TypeAnalyzer,
                           envCache: MMap[APIName, STypeEnv],
                           cycleChecker: CyclicReferenceChecker): STypeCheckerImpl

  def extend(newEnv: STypeEnv, newAnalyzer: TypeAnalyzer) =
    constructor(current, traits, newEnv, labelExitTypes, errors)(newAnalyzer, envCache, cycleChecker)

  def extend(bindings: List[Binding]) =
    constructor(current,
                traits,
                env.extend(bindings),
                labelExitTypes,
                errors)

  def extend(sparams: List[StaticParam], where: Option[WhereClause]) =
    constructor(current,
                traits,
                env,
                labelExitTypes,
                errors)(analyzer.extend(sparams, where), envCache, cycleChecker)

  def extend(sparams: List[StaticParam],
             params: Option[List[Param]],
             where: Option[WhereClause]) = {
    val newEnv = params match {
      case Some(ps) => env.extend(ps)
      case None => env
    }
    constructor(current,
                traits,
                newEnv,
                labelExitTypes,
                errors)(analyzer.extend(sparams, where), envCache, cycleChecker)
  }

  def extend(id: Id, typ: Type): STypeChecker =
    extend(List[LValue](NodeFactory.makeLValue(id, typ)))

  def extend(ids: List[Id], types: List[Type]): STypeChecker =
    extend(ids.zip(types).map((p:(Id, Type)) =>
        NodeFactory.makeLValue(p._1,p._2)))

  def extend(id_types: List[(Id, Type)]): STypeChecker =
    extend(id_types.map((p:(Id, Type)) =>
        NodeFactory.makeLValue(p._1,p._2)))

  def extend(decl: LocalVarDecl): STypeChecker =
    constructor(current,
                traits,
                env.extend(decl),
                labelExitTypes,
                errors)

  def extendWithFunctions[T <: Functional](methods: Relation[IdOrOpOrAnonymousName, T]) =
    constructor(current,
                traits,
                env.extendWithFunctions(methods),
                labelExitTypes,
                errors)

  def extendWithListOfFunctions[T <: Functional](fns:List[T]) =
    constructor(current,
                traits,
                env.extendWithListOfFunctions(fns),
                labelExitTypes,
                errors)

  def extendWithout(declSite: Node, names: Set[Id]) =
    constructor(current,
                traits,
                env.extendWithout(names),
                labelExitTypes,
                errors)

  def addSelf(self_type: Type) =
    extend(List[LValue](NodeFactory.makeLValue("self", self_type)))

  protected def signal(msg:String, hasAt:HasAt) =
    errors.signal(msg, hasAt)

  protected def signal(hasAt:HasAt, msg:String) =
    errors.signal(msg, hasAt)

  protected def signal(error: StaticError) =
    errors.signal(error)

  protected def syntaxError(hasAt:HasAt, msg:String) =
    error(hasAt, msg)

  protected def isValidErrorMessage(msg: String) = !msg.containsSlice("%s")

  /**
   *  Determine if subtype <: supertype. If false, then the given error message
   * is signaled for the given location.
   */
  protected def isSubtype(subtype: Type,
                          supertype: Type,
                          location: HasAt,
                          error: String): Boolean = {
    val judgement = isSubtype(subtype, supertype)
    if (! judgement) {
      if (isValidErrorMessage(error))
        signal(error, location)
      else
        signal(error.format(normalize(subtype), normalize(supertype)), location)
    }
    judgement
  }

  /**
   * Determine if subtype <: supertype.
   */
  protected def isSubtype(subtype: Type, supertype: Type): Boolean =
    isTrue(analyzer.subtype(subtype, supertype))

  /**
   * Return the conditions for subtype <: supertype to hold.
   */
  protected def checkSubtype(subtype: Type, supertype: Type): CFormula = {
    analyzer.subtype(subtype, supertype)
  }

  protected def equivalentTypes(t1: Type, t2: Type): Boolean =
    isTrue(analyzer.equivalent(t1, t2))

  protected def normalize(ty: Type): Type =
    TypeNormalizer.normalize(ty)

  /**
   * Get the TypeEnv that corresponds to this API.
   */
  protected def getEnvFromApi(api: APIName): STypeEnv =
    envCache.getOrElseUpdate(api, STypeEnv.make(traits.compilationUnit(api)))

  /**
   * Lookup the type of the given name in the proper type environment.
   */
  protected def getTypeFromName(name: Name): Option[Type] =
    getRealName(name, toListFromImmutable(current.ast.getImports)) match {
      case id@SIdOrOp(_, Some(api), _) =>
        getEnvFromApi(api).getType(id)
      case id:IdOrOp =>
        env.getType(id) match {
        case Some(ty) =>
          Some(ty)
        case None =>
          analyzer.env.getType(id)
      }
      case _ => None
    }

  /**
   * Is there a binding for the given name?
   */
  protected def nameHasBinding(name: Name): Boolean =
    getRealName(name, toListFromImmutable(current.ast.getImports)) match {
      case id@SIdOrOp(_, Some(api), _) => getEnvFromApi(api).contains(id)
      case id:IdOrOp => env.contains(id) || analyzer.env.contains(id)
      case _ => false
    }

  /**
   * Lookup the modifiers of the given name in the proper type environment.
   */
  protected def getModsFromName(name: Name): Option[Modifiers] =
    getRealName(name, toListFromImmutable(current.ast.getImports)) match {
      case id@SIdOrOp(_, Some(api), _) => getEnvFromApi(api).getMods(id)
      case id:IdOrOp => env.getMods(id)
      case _ => None
    }

  /**
   * Lookup the functional indices for the given name in the proper type
   * environment.
   */
  protected def getFnIndicesFromName(name: Name): Option[List[Functional]] =
    getRealName(name, toListFromImmutable(current.ast.getImports)) match {
      case id@SIdOrOp(_, Some(api), _) => getEnvFromApi(api).getFnIndices(id)
      case id:IdOrOp => env.getFnIndices(id)
      case _ => None
    }

  def getErrors(): List[StaticError] = errors.errors

  /**
   * Signal an error if the given type is not a trait.
   */
  protected def assertTrait(t: BaseType,
                            msg: String,
                            error_loc: Node): Unit = t match {
    case tt:TraitSelfType => assertTrait(tt.getNamed, msg, error_loc)
    case tt:TraitType => toOption(traits.typeCons(tt.getName)) match {
      case Some(ti) if ti.isInstanceOf[ProperTraitIndex] =>
      case _ => signal(error_loc, msg)
    }
    case SAnyType(info) =>
    case _ => signal(error_loc, msg)
  }

  /**
   * Create an error message that will have type and expected type inserted.
   * There should be no string format operators in the message.
   */
  protected def errorString(message: String): String =
    message + " has type %s, but it must have %s type."

  /**
   * Create an error message that will have type and expected type inserted.
   * There should be no string format operators in either supplied message.
   */
  protected def errorString(first: String, second: String): String =
    first + " has type %s, but " + second + " type is %s."


  // ---------------------------------------------------------------------------
  // ABSTRACT DEFINITIONS ------------------------------------------------------

  /**
   * Type check the given node, returning the rewritten node. Either a subclass
   * or mixed-in trait should provide the implementation of type checking an
   * arbitrary node.
   */
  def check(node: Node): Node

  /**
   * Type check an expression, returning the rewritten node. This overloading
   * should only ever be called by the other two overloadings. That is, no cases
   * in the implementation should call this method itself. Either a subclass or
   * mixed-in trait should provide the implementation of type checking an
   * expression.
   *
   * @param expr The expression node to type check.
   * @param expected The expected type of this expression, if there is one.
   *                 This should only be explicitly used when doing type
   *                 inference.
   * @return The rewritten expression node.
   */
  def checkExpr(expr: Expr, expected: Option[Type]): Expr

  // ---------------------------------------------------------------------------
  // CONCRETE DEFINITIONS ------------------------------------------------------

  /**
   * Type check the given node. External code must use this method for all type
   * checking.
   *
   * @param node The node to type check.
   * @return The resulting node, possibly containing new type information.
   */
  def typeCheck(node: Node): Node =
    try {
      check(node)
    }
    catch {
      case e:ProgramError =>
        errors.errors = List[StaticError]()
        errors.signal(e.getOriginalMessage, e.getLoc.unwrap)
        node
    }

  /**
   * Type check an expression and guarantee that its type is substitutable for
   * the expected type. That is, the resulting type should be a subtype of or
   * coerced to the expected type. If this is not the case, signal an error
   * with the given message. This message should have two "%s" string format
   * operators in it; the first is replaced with the actual type and the second
   * with the expected type.
   *
   * @param expr The expression node to type check.
   * @param expected The expected type of this expression.
   * @param message The message for the error if the expression is well-typed
   *                but fails the expected type check. Must contain two %s
   *                format specifiers.
   * @return The rewritten node if the check succeeded. Otherwise, the original
   *         node.
   */
  def checkExpr(expr: Expr,
                expected: Type,
                message: String,
                location: HasAt): Expr =
    checkExpr(expr, expected, Some((message, location)))

  /**
   * Check the expression with the given expected type. Do not signal an error
   * on failure.
   */
  def checkExpr(expr: Expr,
                expected: Type): Expr =
    checkExpr(expr, expected, None)

  /**
   * Check the expression with the given expected type. If the error message and
   * location are present, then the appropriate error will be signaled.
   */
  protected def checkExpr(expr: Expr,
                          expected: Type,
                          msgAndLoc: Option[(String, HasAt)]): Expr = {
    val checkedExpr = checkExpr(expr, Some(expected))
    getType(checkedExpr) match {
      case Some(typ) if isSubtype(typ, expected) => checkedExpr
      case Some(typ) =>
        // Try to build a coercion of checkedExpr to the expected type.
        coercions.buildCoercion(checkedExpr, expected).getOrElse {
          msgAndLoc match {
            case Some((message, location)) =>
              val someE = Some(expected)
              val checkedExpr2 = checkExpr(expr, someE) // Repeat calculation for debugging
              val checkedExpr3 = checkExpr(expr, someE) // Repeat calculation for debugging
              signal(location, message.format(normalize(typ), normalize(expected)))
            case None =>
          }
          expr
        }
      case None => expr
    }
  }

  /**
   * This overloading uses the expression as the location in the error message.
   */
  def checkExpr(expr: Expr, expected: Type, message: String): Expr =
    checkExpr(expr, expected, message, expr)

  /**
   * Type check an expression, returning the rewritten node. This overloading
   * should be called whenever there is no expected type.
   *
   * @param expr The expression node to type check.
   * @return The rewritten expression node.
   */
  def checkExpr(expr: Expr): Expr = checkExpr(expr, None)

  /**
   * Check the given expr if it is checkable, yielding Left for the checked expr and Right for the
   * unchecked original expr.
   */
  def checkExprIfCheckable(expr: Expr): Either[Expr, FnExpr] = {
    if (isCheckable(expr)) {
      val checked = checkExpr(expr)
      Left(checked)
    } else {
      Right(expr.asInstanceOf[FnExpr])
    }
  }

}

/**
 * A type checker that doesn't report its errors. Use the tryCheck() and
 * tryCheckExpr() methods instead of check() and checkExpr() to determine if
 * the check failed or not. As soon as any static error is generated, these
 * methods will return None. If they succeed, they return the node wrapped
 * in Some.
 */
class TryChecker(current: CompilationUnitIndex,
                 traits: TraitTable,
                 env: STypeEnv,
                 labelExitTypes: JavaMap[Id, Option[JavaSet[Type]]],
                 errors: ErrorLog)
                (implicit analyzer: TypeAnalyzer,
                          envCache: MMap[APIName, STypeEnv],
                          cycleChecker: CyclicReferenceChecker)
  extends STypeCheckerImpl(current, traits, env, labelExitTypes, errors) {

  override def constructor(current: CompilationUnitIndex,
                           traits: TraitTable,
                           env: STypeEnv,
                           labelExitTypes: JavaMap[Id, Option[JavaSet[Type]]],
                           errors: ErrorLog)
                          (implicit analyzer: TypeAnalyzer,
                                    envCache: MMap[APIName, STypeEnv],
                                    cycleChecker: CyclicReferenceChecker) =
    new TryChecker(current, traits, env, labelExitTypes, errors)

  /** Check the given node; return it if successful, None otherwise. */
  def tryCheck(node: Node): Option[Node] =
    try {
      Some(super.check(node))
    }
    catch {
      case e:StaticError => None
      case e => throw e
    }

  /** Check the given expression; return it if successful, None otherwise. */
  def tryCheckExpr(expr: Expr): Option[Expr] =
    try {
      val checkedExpr = super.checkExpr(expr)
      if (getType(checkedExpr).isNone)
        bug("TryChecker returned an untyped expr!")
      Some(checkedExpr)
    }
    catch {
      case e:StaticError => None
      case e => throw e
    }

  /** Check the given expression; return it if successful, None otherwise. */
  def tryCheckExpr(expr: Expr, expected: Option[Type]): Option[Expr] =
    try {
      val checkedExpr = super.checkExpr(expr, expected)
      if (getType(checkedExpr).isNone)
        bug("TryChecker returned an untyped expr!")
      Some(checkedExpr)
    }
    catch {
      case e:StaticError => None
      case e => throw e
    }

  /**
   * Check the given expression with the given expected type; return it if
   * successful, None otherwise.
   */
  def tryCheckExpr(expr: Expr, typ: Type): Option[Expr] =
    try {
      val checkedExpr = super.checkExpr(expr, typ)
      if (getType(checkedExpr).isNone)
        bug("TryChecker returned an untyped expr!")
      Some(checkedExpr)
    }
    catch {
      case e:StaticError => None
      case e => throw e
    }

  /** Return the error that made the TryChecker fail. */
  def getError: Option[StaticError] = errors.errors.headOption
}
