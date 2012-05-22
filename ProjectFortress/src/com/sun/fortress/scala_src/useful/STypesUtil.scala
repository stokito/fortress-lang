/*******************************************************************************
    Copyright 2009,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/

package com.sun.fortress.scala_src.useful

import _root_.java.util.ArrayList
import _root_.java.util.{ List => JList }
import _root_.java.util.{ Map => JMap }
import scala.collection.{ Set => CSet }
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.MultiMap
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.collection.mutable.{ Set => MSet }
import edu.rice.cs.plt.tuple.Pair
import edu.rice.cs.plt.collect.Relation
import edu.rice.cs.plt.collect.IndexedRelation
import com.sun.fortress.compiler.disambiguator.NameEnv
import com.sun.fortress.compiler.GlobalEnvironment
import com.sun.fortress.compiler.Types
import com.sun.fortress.compiler.Types.ANY
import com.sun.fortress.compiler.Types.BOTTOM
import com.sun.fortress.compiler.Types.OBJECT
import com.sun.fortress.compiler.index.{Unit => UnitI , _}
import com.sun.fortress.compiler.typechecker.StaticTypeReplacer
import com.sun.fortress.exceptions.InterpreterBug.bug
import com.sun.fortress.exceptions.TypeError
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.{ ExprFactory => EF }
import com.sun.fortress.nodes_util.{ NodeFactory => NF }
import com.sun.fortress.nodes_util.{ NodeUtil => NU }
import com.sun.fortress.nodes_util.Span
import com.sun.fortress.scala_src.overloading.OverloadingOracle
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.typechecker.CoercionOracle
import com.sun.fortress.scala_src.typechecker.Formula._
import com.sun.fortress.scala_src.types.TypeAnalyzer
import com.sun.fortress.scala_src.types.TypeSchemaAnalyzer
import com.sun.fortress.scala_src.useful.Iterators._
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.Sets._
import com.sun.fortress.scala_src.useful.SExprUtil._
import com.sun.fortress.useful.HasAt
import com.sun.fortress.useful.NI
import com.sun.fortress.scala_src.typechecker._
import com.sun.fortress.useful.Useful
import java.util.Collections;

object STypesUtil {

  final val debugInheritedMethods = false
  
  // Make sure we don't infinitely explore supertraits that are cyclic
  class HierarchyHistory {
    var explored = Set[Type]()
    def explore(t: Type): Boolean =
      if (explored(t))
        false
      else {
        explored += t
        true
      }
    def hasExplored(t: Type): Boolean = explored(t)
    def copy = {
      val h = new HierarchyHistory
      h.explored = this.explored
      h
    }
  }

  /** A function that when applied yields an option type. */
  type TypeThunk = Function0[Option[Type]]

  /** A function type that takes two types and returns a boolean. */
  type Subtype = (Type, Type) => Boolean

  /**
   * A function application candidate before any inference.
   *
   * @param arrow The arrow type of this candidate. Has been instantiated with
   *     explicit static args if any.
   * @param overloading Optional Overloading node for this candidate. Should
   *    exist for all candidates of overloaded functions.
   */
  case class PreAppCandidate(arrow: ArrowType, overloading: Option[Overloading], fnl:Option[Functional])

  /**
   * A function application candidate.
   *
   * @param arrow The arrow type of this candidate. Has been instantiated with
   *     explicit static args if any.
   * @param sargs The inferred static args for this candidate.
   * @param args The argument expression or expressions for this candidate.
   * @param overloading Optional Overloading node for this candidate. Should
   *     exist for all candidates of overloaded functions.
   */
  case class AppCandidate(arrow: ArrowType,
    sargs: List[StaticArg],
    args: List[Expr],
    overloading: Option[Overloading],
    fnl:Option[Functional]) {

    /**
     * Combine all the args in this candidate into a single, perhaps tuple arg
     * and insert this into the duplicated AppCandidate. Uses the given span for
     * the new arg.
     */
    def mergeArgs(loc: Span): AppCandidate = {
      val newArg = EF.makeArgumentExpr(loc, toJavaList(args))
      AppCandidate(arrow, sargs, List(newArg), overloading, fnl)
    }
  }

  def getStaticParameters(f: Functional, lifted: Boolean): JList[StaticParam] = f match {
      case f: HasSelfType if (lifted) => 
        toJavaList(toList(f.traitStaticParameters()) ++ toList(f.staticParameters()))
      case _ => f.staticParameters()
  }
  
  def makeArrowFromFunctional(f: Functional): Option[ArrowType] = f match {
    case _: FunctionalMethod => makeArrowFromFunctional(f, true)
    case _ => makeArrowFromFunctional(f, false)
  }
  
  def makeArrowWithoutSelfFromFunctional(f: Functional): Option[ArrowType] = f match {
    case _: FunctionalMethod => makeArrowFromFunctional(f, true, true)
    case _ => makeArrowFromFunctional(f, false, true)
  }
  
  def makeLiftedArrowWithoutSelfFromFunctional(f: Functional): Option[ArrowType] = f match {
    case _: FunctionalMethod => makeArrowFromFunctional(f, true, true)
    case _ => makeArrowFromFunctional(f, true, true)
  }
  
  /**
   *  Return the arrow type of the given Functional index.
   */
  def makeArrowFromFunctional(f: Functional, lifted: Boolean): Option[ArrowType] =
    makeArrowFromFunctional(f, lifted, false)

  def makeArrowFromFunctional(f: Functional, lifted: Boolean, omitSelf: Boolean): Option[ArrowType] = f match {
    case f: DummyVariableFunction => Some(f.getArrowType())
    case _ =>
      val returnType = toOption(f.getReturnType).getOrElse(return None)
      val originalParams = toListFromImmutable(f.parameters).map(NU.getParamType)
      val params = if (omitSelf)
                     f match { case fhst: HasSelfType => 
                                 if (fhst.selfPosition >= 0)
                                   (originalParams.take(fhst.selfPosition) ++
				    originalParams.drop(fhst.selfPosition + 1))
				 else originalParams }
                   else originalParams
      val argType = makeArgumentType(params)
      val effect = NF.makeEffect(f.thrownTypes)
      val where = f match {
        case f: Constructor => f.where
        case _ => none[WhereClause]
      }
      val info = f match {
        case fhst: HasSelfType if fhst.selfType.isNone =>
          bug("No selfType on functional %s".format(f))
        case fhst: HasSelfType => 
          Some(SMethodInfo(fhst.selfType.get, fhst.selfPosition))
        case _ => None
      }
      val sparamsJava = getStaticParameters(f, lifted)
      Some(NF.makeArrowType(NF.typeSpan,
                            false,
                            argType,
                            returnType,
                            effect,
                            sparamsJava,
                            where,
                            info))
  }

  /**
   * Make a single argument type from a list of types.
   */
  def makeArgumentType(ts: List[Type]): Type = ts match {
    case Nil => Types.VOID
    case t :: Nil => t
    case _ =>
      val span1 = NU.getSpan(ts.head)
      val span2 = NU.getSpan(ts.last)
      NF.makeTupleType(NU.spanTwo(span1, span2), toJavaList(ts))
  }

  /**
   * Make a domain type from a list of parameters, including varargs and
   * keyword types. Ported from `TypeEnv.domainFromParams`. Returns None if
   * not all parameters had types.
   */
  def makeDomainType(ps: List[Param]): Option[Type] = {
    val paramTypes = new ArrayList[Type](ps.length)
    val keywordTypes = new ArrayList[KeywordType](ps.length)
    var varargsType: Option[Type] = None
    val span = ps match {
      case Nil => NF.typeSpan
      case _ => NU.spanTwo(NU.getSpan(ps.head), NU.getSpan(ps.last))
    }

    // Extract out the appropriate parameter types.
    ps.foreach(p => p match {
      case SParam(_, _, _, _, _, Some(vaType)) => // Vararg
        varargsType = Some(vaType)
      case SParam(_, name, _, Some(idType), Some(expr), _) => // Keyword
        idType match {
          case p@SPattern(_, _, _) => bug("Pattern should be desugared away: " + p)
          case t@SType(_) =>
            keywordTypes.add(NF.makeKeywordType(name, t))
        }
      case SParam(_, _, _, Some(idType), _, _) => // Normal
        idType match {
          case p@SPattern(_, _, _) => bug("Pattern should be desugared away: " + p)
          case t@SType(_) => paramTypes.add(t)
        }
      case _ => return None
    })
    Some(NF.makeDomain(span,
      paramTypes,
      toJavaOption(varargsType),
      keywordTypes))
  }

  /**
   * Given a list of params missing some declared types, use the expected
   * domain to insert the appropriate types into those params.
   */
  def addParamTypes(expectedDomain: Type,
    oldParams: List[Param])(implicit analyzer: TypeAnalyzer): Option[List[Param]] = {

    // Get all the params with inference vars filled in.
    val params = oldParams.map {
      case SParam(info, name, mods, None, defaultExpr, None) =>
        SParam(info,
          name,
          mods,
          Some(NF.make_InferenceVarType(info.getSpan)),
          defaultExpr,
          None)
      case p => p
    }

    // Get the substitution resulting from params :> expectedDomain
    val paramsDomain = makeDomainType(params).get
    solve(analyzer.subtype(expectedDomain, paramsDomain)).map{ case (tSub, oSub) =>
      params.map{
        case SParam(info, name, mods, Some(idType), defaultExpr, None) =>
          idType match {
            case p@SPattern(_, _, _) => bug("Pattern should be desugared away: " + p)
            case t@SType(_) =>
              SParam(info,
                name,
                mods,
                Some(tSub(t)),
                defaultExpr,
                None)
          }
        case p => p
      }
    }
  }

  /**
   * Make a type for the given list of bindings. Returns None if not all
   * bindings had types.
   */
  def makeLhsType(ls: List[LValue]): Option[Type] = ls match {
    case Nil => Some(NF.makeMaybeTupleType(NF.typeSpan, toJavaList(Nil)))
    case _ =>
      Some(NF.makeMaybeTupleType(NU.spanTwo(NU.getSpan(ls.head), NU.getSpan(ls.last)),
        toJavaList(ls.map(lv => lv match {
          case SLValue(_, _, _, Some(typ), _) =>
            typ match {
              case p@SPattern(_, _, _) => bug("Pattern should be desugared away: " + p)
              case t@SType(_) => t
            }
          case _ => return None
        }))))
  }

  /**
   * Given a list of LValues and some RHS type, add in the appropriate types
   * for each LValue. Returns None if the RHS type does not correspond to the
   * LValues.
   */
  def addLhsTypes(ls: List[LValue], typ: Type): Option[List[LValue]] =
    typ match {
      case STupleType(_, elts, _, _) if elts.length == ls.length =>
        // Put each tuple element into corresponding LValue.
        Some((ls, elts).zipped.map((lv, typ) => {
          val SLValue(info, name, mods, _, mutable) = lv
          SLValue(info, name, mods, Some(typ), mutable)
        }))

      case _: TupleType => None

      case _: Type if ls.length == 1 =>
        Some(ls.map(lv => {
          val SLValue(info, name, mods, _, mutable) = lv
          SLValue(info, name, mods, Some(typ), mutable)
        }))

      case _ => None
    }

   def getOps(t: (StaticParam, StaticArg)): Option[(Op, Op)] = t match {
     case (SStaticParam(_, 0, o: Op, _, _, _, _,_: KindOp, _), SOpArg(_,_,io)) => Some((o,io))       // TODO: variance needs to be addressed
     case _ => None
   }
  
  /**
   * Convert a static parameter to the corresponding static arg. Ported from
   * `TypeEnv.staticParamsToArgs`.
   */
  def staticParamToArg(p: StaticParam): StaticArg = {
    val span = NU.getSpan(p)
    (p.getName, p.getKind) match {
      case (id: Id, _: KindBool) => NF.makeBoolArg(span, NF.makeBoolRef(span, id), p.isLifted)
      case (id: Id, _: KindDim) => NF.makeDimArg(span, NF.makeDimRef(span, id), p.isLifted)
      case (id: Id, _: KindInt) => NF.makeIntArg(span, NF.makeIntRef(span, id), p.isLifted)
      case (id: Id, _: KindNat) => NF.makeIntArg(span, NF.makeIntRef(span, id), p.isLifted)
      case (id: Id, _: KindType) => NF.makeTypeArg(span, NF.makeVarType(span, id), p.isLifted)
      case (id: Id, _: KindUnit) => NF.makeUnitArg(span, NF.makeUnitRef(span, false, id), p.isLifted)
      case (op: Op, _: KindOp) => NF.makeOpArg(span, op)
      case _ => bug("Unexpected static parameter kind")
    }
  }
  
  def staticParamToArgEnhanced(p: StaticParam, lsp: JList[StaticParam]): StaticArg = {
    val span = NU.getSpan(p)
    (p.getName, p.getKind) match {
      case (id: Id, _: KindBool) => NF.makeBoolArg(span, NF.makeBoolRef(span, id), p.isLifted)
      case (id: Id, _: KindDim) => NF.makeDimArg(span, NF.makeDimRef(span, id), p.isLifted)
      case (id: Id, _: KindInt) => NF.makeIntArg(span, NF.makeIntRef(span, id), p.isLifted)
      case (id: Id, _: KindNat) => NF.makeIntArg(span, NF.makeIntRef(span, id), p.isLifted)
      case (id: Id, _: KindType) =>
        NF.makeTypeArg(span, NF.makeVarType(span, id, lsp), p.isLifted)
      case (id: Id, _: KindUnit) => NF.makeUnitArg(span, NF.makeUnitRef(span, false, id), p.isLifted)
      case (op: Op, _: KindOp) => NF.makeOpArg(span, op)
      case _ => bug("Unexpected static parameter kind")
    }
  }

  /**
   * Convert  list of static parameters to corresponding list of static args.
   */
  def staticParamsToArgs(p: JList[StaticParam]): JList[StaticArg] =
    toJavaList(toListFromImmutable(p).map(staticParamToArg))

  def staticParamsToArgsEnhanced(p: JList[StaticParam]): JList[StaticArg] = {
    val l =  new java.util.ArrayList[StaticArg]
    for (sp <- p) {
        val ar = staticParamToArgEnhanced(sp,p)
        l.add(ar)
    }
    l
  }

  def declToTraitType(d: TraitObjectDecl): TraitType = {
    val tid = d.getHeader.getName.asInstanceOf[Id]
    val tparam = d.getHeader.getStaticParams
    NF.makeTraitType(tid, staticParamsToArgs(tparam))
  }

  def declToTraitTypeEnhanced(d: TraitObjectDecl): TraitType = {
    val tid = d.getHeader.getName.asInstanceOf[Id]
    val tparam = d.getHeader.getStaticParams
    NF.makeTraitType(tid, staticParamsToArgsEnhanced(tparam), tparam)
  }

  /**
   * Does the given type have any static parameters declared?
   */
  def hasStaticParams(typ: Type): Boolean = !typ.getInfo.getStaticParams.isEmpty

  /**
   * Does the given type have any static parameters declared? If `ignoreLifted`
   * is true, then ignore lifted static parameters.
   */
  def hasStaticParams(typ: Type, ignoreLifted: Boolean): Boolean = {
    val params = getStaticParams(typ)
    if (ignoreLifted) params.exists(!_.isLifted) else !params.isEmpty
  }

  /**
   *  Get all the static parameters out of the given type.
   */
  def getStaticParams(typ: Type): List[StaticParam] =
    toListFromImmutable(typ.getInfo.getStaticParams)

  def getWhere(typ: Type): Option[WhereClause] =
    toOption(typ.getInfo.getWhereClause)

  /**
   * Return an identical type but with the given static params removed from it.
   * TodDo: Handle where clauses
   */
  def clearStaticParams(typ: Type, sparams: List[StaticParam]): Type = {

    // A walker that clears static params out of TypeInfos.
    object paramWalker extends Walker {
      override def walk(node: Any): Any = node match {
        case STypeInfo(a, b, existingSparams, _) =>
          STypeInfo(a, b, existingSparams filterNot (sparams contains), None)
        case _ => super.walk(node)
      }
    }
    paramWalker(typ).asInstanceOf[Type]
  }

  /** Returns an identical type but with no static params. */
  def clearStaticParams(typ: Type): Type =
    clearStaticParams(typ, getStaticParams(typ))

  /**
   * Insert static parameters into a type. If the type already has static
   * parameters, a bug is thrown.
   * 
   * ToDo: Handle where clauses
   */
  def insertStaticParams(typ: Type, sparams: List[StaticParam]): Type = {
    var inserted = false
    // A walker that adds static params to the outermost type info
    object paramWalker extends Walker {
      override def walk(node: Any): Any = node match {
        case STypeInfo(a, b, Nil, c) =>
          inserted = true
          STypeInfo(a, b, sparams, c)
        case STypeInfo(_, _, _, _) =>
          bug("cannot overwrite static parameters")
        case _ => if (!inserted) super.walk(node) else node
      }
    }
    paramWalker(typ).asInstanceOf[Type]
  }

  def insertStaticParams(typ: Type, sparams_jl: java.util.List[StaticParam]): Type = {
    var inserted = false
    val sparams : List[StaticParam] = toListFromImmutable(sparams_jl);
    // A walker that adds static params to the outermost type info
    object paramWalker extends Walker {
      override def walk(node: Any): Any = node match {
        case STypeInfo(a, b, Nil, c) =>
          inserted = true
          STypeInfo(a, b, sparams, c)
        case STypeInfo(_, _, _, _) =>
          bug("cannot overwrite static parameters")
        case _ => if (!inserted) super.walk(node) else node
      }
    }
    paramWalker(typ).asInstanceOf[Type]
  }

  /**
   * Determine if the given type is an arrow or intersection of arrow types.
   */
  def isArrows(ty: Type): Boolean = {
    var valid = true
    object checkArrows extends Walker {
      override def walk(node: Any): Any = node match {
        case n: ArrowType => true
        case _ => false
      }
    }
    for (t <- conjuncts(ty)) {
      valid &= checkArrows(t).asInstanceOf[Boolean]
    }
    valid
  }

  /**
   * Determine if the type previously inferred by the type checker from the
   * given expression is an arrow or intersection of arrow types.
   */
  def isArrows(expr: Expr): Boolean =
    isFnExpr(expr) || isArrows(SExprUtil.getType(expr).get)

  /**
   * Determine if the given type could possibly be an arrow or multiple arrows.
   * It could possibly be an arrow if it is a type variable whose bound is Any.
   * Otherwise, it is multiple arrows if it is the intersection of arrows.
   */
  def possiblyArrows(ty: Type, sparams: List[StaticParam]): Boolean =
    ty match {
      case SVarType(_, typ, _) =>
        sparams.exists {
          case SStaticParam(_, _, sp, List(_: AnyType), _, _, _, _, _) => typ == sp
          case _ => false
        }
      case _ => isArrows(ty)
    }

  /**
   * Returns the type of the static parameter's upper bound if it is a type parameter.
   */
  def staticParamBoundType(sparam: StaticParam): Option[Type] =
    sparam.getKind match {
      case _: KindType if sparam.getExtendsClause.isEmpty =>
        Some(Types.ANY)
      case _: KindType =>
        Some(NF.makeIntersectionType(sparam.getExtendsClause))
      case _ => None
    }
  
  /**
   * Returns the type of the static parameter's lower bound if it is a type parameter.
   */
  def staticParamLowerBoundType(sparam: StaticParam): Option[Type] =
    sparam.getKind match {
      case _: KindType if sparam.getDominatesClause.isEmpty =>
        Some(Types.ANY)
      case _: KindType =>
        Some(NF.makeIntersectionType(sparam.getDominatesClause))
      case _ => None
    }
    
  
    // Get an inference variable type out of a static arg.
    def staticArgType(sarg: StaticArg): Option[_InferenceVarType] = sarg match {
      case sarg: TypeArg => Some(sarg.getTypeArg.asInstanceOf[_InferenceVarType])
      case _ => None
    }

  /**
   * Given a static parameters, returns a static arg containing a fresh
   * inference variable.
   */
  def makeInferenceArg(sparam: StaticParam): StaticArg = sparam.getKind match {
    case _: KindType => 
      NF.makeTypeArg(NU.getSpan(sparam), NF.make_InferenceVarType(NU.getSpan(sparam)),
                     sparam.isLifted)
    case _: KindInt => NI.nyi()
    case _: KindBool => NI.nyi()
    case _: KindDim => NI.nyi()
    case _: KindOp => NF.makeOpArg(NU.getSpan(sparam),
                      NF.make_InferenceVarOp(NU.getSpan(sparam)),
                      sparam.isLifted) // DRC- opArgs also lifted?
    case _: KindUnit => NI.nyi()
    case _: KindNat => NI.nyi()
    case _ => bug("unexpected kind of static parameter")
  }

  /**
   * Returns a list of conjuncts of the given type. If given an intersection
   * type, this is the set of the constituents. If ANY, this is empty. If some
   * other type, this is the singleton set of that type.
   */
  def conjuncts(ty: Type): Set[Type] = ty match {
    case _: AnyType => Set.empty[Type]
    case SIntersectionType(_, elts) => Set(elts: _*).flatMap(conjuncts)
    case _ => Set(ty)
  }

  /**
   * Returns a list of disjuncts of the given type. If given a union
   * type, this is the set of the constituents. If BOTTOM, this is empty. If some
   * other type, this is the singleton set of that type.
   */
  def disjuncts(ty: Type): Set[Type] = ty match {
    case _: BottomType => Set.empty[Type]
    case SUnionType(_, elts) => Set(elts: _*).flatMap(disjuncts)
    case _ => Set(ty)
  }

  /**
   * Returns TypeConsIndex of "typ".
   */
  def getTypes(typ: Id, globalEnv: GlobalEnvironment,
    compilation_unit: CompilationUnitIndex): TypeConsIndex = typ match {
    case SId(info, Some(name), text) =>
      globalEnv.api(name).typeConses.get(SId(info, None, text))
    case _ => compilation_unit.typeConses.get(typ)
  }

  /** Return the [Scala-based] conditions for subtype <: supertype to hold. */
  def checkSubtype(subtype: Type, supertype: Type)(implicit analyzer: TypeAnalyzer): CFormula = {

    val constraint = analyzer.subtype(subtype, supertype)

    if (!constraint.isInstanceOf[CFormula]) {
      bug("Not a CFormula.")
    }
    constraint.asInstanceOf[CFormula]
  }

  /** Determine if subtype <: supertype. */
  def isSubtype(subtype: Type, supertype: Type)(implicit analyzer: TypeAnalyzer): Boolean =
    isTrue(checkSubtype(subtype, supertype))

  /**
   * Replaces occurrences of static parameters with corresponding static
   * arguments in the given body type. By default, only the unlifted static
   * params will be instantiated. Use the `applyLifted` and `applyUnlifted`
   * named args to switch behavior at call site.
   *
   * @param args A list of static arguments to apply to the generic type body.
   * @param sparams A list of static parameters
   * @param body The generic type whose static parameters are to be replaced.
   * @param applyLifted If true, the static args should instantiate lifted
   *     static params. Should always be given as a named arg. Default is false.
   * @param applyUnlifted If true, the static args should instantiate unlifted
   *     static params. Should always be given as a named arg. Default is true.
   * @return An option of a type identical to body but with every occurrence of
   *         one of its declared static parameters replaced by corresponding
   *         static args. If None, then the instantiation failed.
   *         
   * ToDo: Handle where clauses
   */
  def staticInstantiation(sargs: List[StaticArg],
    body: Type,
    applyLifted: Boolean = false,
    applyUnlifted: Boolean = true)(implicit analyzer: TypeAnalyzer): Option[Type] = {

    val sparams = getStaticParams(body)
    val sparamsAndSargs = sparams.filter { s =>
      (s.isLifted && applyLifted) || (!s.isLifted && applyUnlifted)
    } zip sargs
    
    // Need to add a check to ensure if aliased is true all the args have names
    if (sparamsAndSargs.size != sargs.size) return None
    if (!staticArgsMatchStaticParams(sparamsAndSargs)) return None

    // Create mapping from parameter names to static args.
    val paramMap = Map(sparamsAndSargs.map(pa => (pa._1.getName, pa._2)): _*)

    // Gets the actual value out of a static arg.
    def sargToVal(sarg: StaticArg): Node = sarg match {
      case sarg: TypeArg => sarg.getTypeArg
      case sarg: IntArg => sarg.getIntVal
      case sarg: BoolArg => sarg.getBoolArg
      case sarg: OpArg => sarg.getId
      case sarg: DimArg => sarg.getDimArg
      case sarg: UnitArg => sarg.getUnitArg
      case _ => bug("unexpected kind of static arg")
    }

    // Clear the static params.

    val cleared = clearStaticParams(body,sparams.filter { s => (s.isLifted && applyLifted) || (!s.isLifted && applyUnlifted) })
    
    //    val cleared = 
//    		if (applyUnlifted) 
//    			clearStaticParams(body)
//    		else clearStaticParams(body,sparams.filter { s =>
//    			(s.isLifted && applyLifted) || (!s.isLifted && applyUnlifted) })
    
    // Replaces all the occurrences of static params as variables with args.
    object staticReplacer extends Walker {
      override def walk(node: Any): Any = node match {
        case n: VarType => paramMap.get(n.getName).map(sargToVal).getOrElse(n)
        case n: OpArg => paramMap.get(n.getId).getOrElse(n)
        case n: IntRef => paramMap.get(n.getName).map(sargToVal).getOrElse(n)
        case n: BoolRef => paramMap.get(n.getName).map(sargToVal).getOrElse(n)
        case n: DimRef => paramMap.get(n.getName).map(sargToVal).getOrElse(n)
        case n: UnitRef => paramMap.get(n.getName).map(sargToVal).getOrElse(n)
        case _ => super.walk(node)
      }
    }

    // Get the replaced type
    Some(staticReplacer(cleared).asInstanceOf[Type])
  }

    def staticInstantiationForApp(sargs: List[StaticArg],
    body: Type,
    applyLifted: Boolean = false,
    applyUnlifted: Boolean = true)(implicit analyzer: TypeAnalyzer): Option[Type] = {

    val sparams = getStaticParams(body)
    val sparamsAndSargs = sparams.filter { s =>
      (s.isLifted && applyLifted) || (!s.isLifted && applyUnlifted)
    } zip sargs

    // Need to add a check to ensure if aliased is true all the args have names
    if (sparamsAndSargs.size != sargs.size) return None
    if (!staticArgsMatchStaticParamsForApp(sparamsAndSargs)) return None

    // Create mapping from parameter names to static args.
    val paramMap = Map(sparamsAndSargs.map(pa => (pa._1.getName, pa._2)): _*)

    // Gets the actual value out of a static arg.
    def sargToVal(sarg: StaticArg): Node = sarg match {
      case sarg: TypeArg => sarg.getTypeArg
      case sarg: IntArg => sarg.getIntVal
      case sarg: BoolArg => sarg.getBoolArg
      case sarg: OpArg => sarg.getId
      case sarg: DimArg => sarg.getDimArg
      case sarg: UnitArg => sarg.getUnitArg
      case _ => bug("unexpected kind of static arg")
    }

    // Clear the static params.
    val cleared = clearStaticParams(body)

    // Replaces all the occurrences of static params as variables with args.
    object staticReplacer extends Walker {
      override def walk(node: Any): Any = node match {
        case n: VarType => paramMap.get(n.getName).map(sargToVal).getOrElse(n)
        case n: OpArg => paramMap.get(n.getId).getOrElse(n)
        case n: IntRef => paramMap.get(n.getName).map(sargToVal).getOrElse(n)
        case n: BoolRef => paramMap.get(n.getName).map(sargToVal).getOrElse(n)
        case n: DimRef => paramMap.get(n.getName).map(sargToVal).getOrElse(n)
        case n: UnitRef => paramMap.get(n.getName).map(sargToVal).getOrElse(n)
        case _ => super.walk(node)
      }
    }

    // Get the replaced type
    Some(staticReplacer(cleared).asInstanceOf[Type])
  }
  
  /**
   * Determines if the kinds of the given static args match those of the static
   * parameters. In the case of type arguments, the type is checked to be a
   * subtype of the corresponding type parameter's bounds.
   */
  def staticArgsMatchStaticParams(sparamsAndSargs: List[(StaticParam, StaticArg)])(implicit analyzer: TypeAnalyzer): Boolean = {

    // Match a single pair.
    def argMatchesParam(paramAndArg: (StaticParam, StaticArg)): Boolean = {
      val (param, arg) = paramAndArg
      (arg, param.getKind) match {
        case (_: TypeArg, _: KindType) => true
        case (_: IntArg, _: KindInt) => true
        case (_: BoolArg, _: KindBool) => true
        case (_: DimArg, _: KindDim) => true
        case (_: OpArg, _: KindOp) => true
        case (_: UnitArg, _: KindUnit) => true
        case (_: IntArg, _: KindNat) => true
        case (_, _) => false
      }
    }

    // Match every pair.
    sparamsAndSargs.forall(argMatchesParam)
  }

    def staticArgsMatchStaticParamsForApp(sparamsAndSargs: List[(StaticParam, StaticArg)])(implicit analyzer: TypeAnalyzer): Boolean = {

    // Match a single pair.
    def argMatchesParam(paramAndArg: (StaticParam, StaticArg)): Boolean = {
      val (param, arg) = paramAndArg
      (arg, param.getKind) match {
        case (_: TypeArg, _: KindType) => {
          val extendsOK = (true /: param.getExtendsClause()) (_ && isSubtype(arg.asInstanceOf[TypeArg].getTypeArg(),_))
          val dominatesOK = (true /: param.getDominatesClause()) (_ && isSubtype(_,arg.asInstanceOf[TypeArg].getTypeArg()))
          extendsOK && dominatesOK          
        }
        case (_: IntArg, _: KindInt) => true
        case (_: BoolArg, _: KindBool) => true
        case (_: DimArg, _: KindDim) => true
        case (_: OpArg, _: KindOp) => true
        case (_: UnitArg, _: KindUnit) => true
        case (_: IntArg, _: KindNat) => true
        case (_, _) => false
      }
    }

    // Match every pair.
    sparamsAndSargs.forall(argMatchesParam)
  }
  
  /** Same as the other overloading but checks two explicit lists. */
  def staticArgsMatchStaticParams(sargs: List[StaticArg],
    sparams: List[StaticParam])(implicit analyzer: TypeAnalyzer): Boolean =
    (sargs.size == sparams.size) && staticArgsMatchStaticParams(sparams zip sargs)

  /** Does the type contain any nested inference variables? */
  def hasInferenceVars(typ: Type): Boolean = {
    // Walker that looks for inf vars.
    object infChecker extends Walker {
      var found = false
      override def walk(node: Any): Any = node match {
        case _: _InferenceVarType => found = true; node
        case _ => super.walk(node)
      }
    }
    infChecker(typ); infChecker.found
  }

  /** Does the type contain any nested occurrences of UnknownType? */
  def hasUnknownType(typ: Type): Boolean = {
    // Walker that looks for an unknown type.
    object unknownChecker extends Walker {
      var found = false
      override def walk(node: Any): Any = node match {
        case _: UnknownType => found = true; node
        case _ => super.walk(node)
      }
    }
    unknownChecker(typ); unknownChecker.found
  }

  /** Returns true iff the two static params have the same kind. */
  def equalKinds(sp1: StaticParam, sp2: StaticParam): Boolean =
    (sp1.getKind, sp2.getKind) match {
      case (_: KindType, _: KindType) => true
      case (_: KindInt, _: KindInt) => true
      case (_: KindBool, _: KindBool) => true
      case (_: KindDim, _: KindDim) => true
      case (_: KindOp, _: KindOp) => true
      case (_: KindUnit, _: KindUnit) => true
      case (_: KindNat, _: KindNat) => true
      case (_, _) => false
    }

  /**
   * Returns true iff the two lists of static params have the same length and
   * each corresponding pair has the same kind.
   */
  def equalKinds(sp1: List[StaticParam], sp2: List[StaticParam]): Boolean =
    sp1.length == sp2.length && (sp1, sp2).zipped.forall((x, y) => equalKinds(x, y))

  /**
   * Creates an iterator over the given domain type. If this is a tuple, it
   * first iterates over the plain types; if varargs are present, it then
   * iterates over them indefinitely. If this is any other type, it is the
   * singleton iterator.
   */
  def typeIterator(dom: Type): Iterator[Type] = dom match {
    case STupleType(_, elts, None, _) => elts.iterator
    case STupleType(_, elts, Some(varargs), _) =>
      elts.iterator ++ new Iterator[Type] {
        def hasNext = true
        def next() = varargs
      }
    case _ => Iterator.single(dom)
  }

  /**
   * Creates an iterator over the given domain type. If this is a tuple, it
   * first iterates over the plain types; if varargs are present, it then
   * iterates over them indefinitely. If this is any other type, including a
   * void type, it is the singleton iterator.
   */
  def typeIteratorVoid(dom: Type): Iterator[Type] = dom match {
    case STupleType(_, Nil, None, _) => Iterator.single(dom)
    case _ => typeIterator(dom)
  }

  /**
   * Zip the given iterator of elements with the type iterator for the given
   * domain type.
   */
  def zipWithDomain[T](elts: Iterator[T], dom: Type): Iterator[(T, Type)] = {
    val first = elts.next
    if (!elts.hasNext)
      Iterator.single((first, dom))
    else
      (Iterator.single(first) ++ elts) zip typeIterator(dom)
  }

  /** Same as the other zipWithDomain but uses lists. */
  def zipWithDomain[T](elts: List[T], dom: Type): List[(T, Type)] = elts match {
    case List(elt) => List((elt, dom))
    case _ => (elts.iterator zip typeIterator(dom)).toList
  }

  /**
   * Zip the given iterator of elements with the type iterator for the given
   * domain type, considering void as a single type.
   */
  def zipWithRhsType[T](elts: Iterator[T], dom: Type): Iterator[(T, Type)] =
    elts zip typeIteratorVoid(dom)

  /** Same as the other zipWithRhsType but uses lists. */
  def zipWithRhsType[T](elts: List[T], dom: Type): List[(T, Type)] =
    (zipWithRhsType(elts.iterator, dom)).toList

  /**
   * Determine if there are enough of the given elements to cover all
   * constituent types of the given type.
   */
  def enoughElementsForType[T](elts: List[T], typ: Type): Boolean =
    elts.size == 1 || (typ match {
      case STupleType(_, typs, None, _) => typs.size == elts.size   // no varargs, keywords ignored for now
      case STupleType(_, typs, Some(_), _) => false //typs.size <= elts.size    // varargs, but ignored for now (as are keywords)
      case _ => false
    })

  /** Get the nth constituent type of the given type. */
  def getTypeAt(typ: Type, index: Int): Option[Type] = {
    if (index < 0) return None
    val itr = typeIterator(typ).drop(index)
    if (itr.hasNext) Some(itr.next) else None
  }

  
  /**
   * Checks whether an arrow type is applicable to the given args. If so, then
   * the [possiblly instantiated] arrow type along with any inferred static
   * args are returned.
   */
  def inferStaticParams(fnType: ArrowType,
    argType: Type,
    context: Option[Type])(implicit analyzer: TypeAnalyzer): Option[(ArrowType, List[StaticArg])] = {

    // Builds a constraint given the arrow with inference variables.
    def makeConstraint(infArrow: ArrowType, map: Map[Op, Op]): CFormula = {

      // argType <:? dom(infArrow) yields a constraint, C1
      val domainConstraint = checkSubtype(argType, infArrow.getDomain)

      // if context given, C := C1 AND range(infArrow) <:? context
      val rangeConstraint = context.map(t =>
        checkSubtype(infArrow.getRange, t)).getOrElse(True)
        
      and(domainConstraint, rangeConstraint)
    }

    // Do the inference.
    inferStaticParamsHelper(fnType, makeConstraint, false, true)
  }

  /**
   * Helper that performs the inference of static params in `typ`. Infers all
   * static params, lifted and unlifted, by default; behavior can be changed at
   * the call site by setting the `inferLifted` and `inferUnlifted` flags.
   * 
   * @param typ The type schema for which we want to infer an instantiation.
   * @param constraintMaker A function that, given a type with inference
   *     variables, returns the constraints to be satisfied for the
   *     instantiation.
   * @param inferLifted If true, lifted static parameters will be inferred. The
   *     default value is true.
   * @param inferUnlifted If true, unlifted static parameters will be inferred.
   *     The default value is false.
   * @return If successful, a pair of the instantiated type and the static args
   *     that instantiated it.
   */
  def inferStaticParamsHelper[T <: Type](typ: T,
      				   	 constraintMaker: (T, Map[Op, Op]) => CFormula,
					 inferLifted: Boolean = true,
					 inferUnlifted: Boolean = true)(implicit analyzer: TypeAnalyzer):
        Option[(T, List[StaticArg])] = {

    // Substitute inference variables for static parameters in typ.

    // 1. build substitution S = [T_i -> $T_i]
    // 2. instantiate fnType with S to get an arrow type with inf vars, infArrow
    val sparamsIni = getStaticParams(typ).filter { s =>
      (s.isLifted && inferLifted) || (!s.isLifted && inferUnlifted)
    }
    //Fix to avoid getting dominating variables
    val sparams: List[StaticParam] = sparamsIni.filter(_.getDominatesClause().isEmpty())
    val domparams: List[StaticParam] = sparamsIni.filterNot(_.getDominatesClause().isEmpty())
    
    val sargs = sparams.map(makeInferenceArg) ++ domparams.map(makeInferenceArg)
    
    val ops = Map((sparams.zip(sargs).flatMap(getOps)):_*)
    val infTyp = staticInstantiation(sargs,
      typ,
      applyLifted = inferLifted,
      applyUnlifted = inferUnlifted).
      getOrElse(return None).asInstanceOf[T]
//   println("Static params helper has infTyp " + infTyp)

    val constraint = constraintMaker(infTyp, ops)
//   println("Static params helper has constraint " + constraint)

    // 5. build bounds map B = [$T_i -> S(UB(T_i))]
    val infVars = sargs.flatMap(staticArgType)
    val sparamBounds = sparams.flatMap(staticParamBoundType).flatMap { t =>
      staticInstantiation(sargs,
        insertStaticParams(t, sparams),
        applyLifted = inferLifted,
        applyUnlifted = inferUnlifted)
    }.map(t => TPrimitive(Set(), Set(), Set(t), Set(  ), Set(), Set()))     // GLS 2/6/2012 Don't allow BottomType to be an upper bound (this could be loosened for some vars but not others)
    // GLS 2/6/2012 The real fix would be to prohibit BottomType as an upper bound only for vars that are in contravariant position in the signature.
    val upperBounds = And(Map(infVars.zip(sparamBounds): _*), Map())
//   println("Static params helper has bounds " + bounds)

   val sparamLowerBounds = domparams.flatMap(staticParamLowerBoundType).flatMap { t =>
      staticInstantiation(sargs,
        insertStaticParams(t, sparams),
        applyLifted = inferLifted,
        applyUnlifted = inferUnlifted)
    }.map(t => TPrimitive(Set(t), Set(), Set(), Set(), Set(), Set()))
    
    val lowerBounds = And(Map(infVars.zip(sparamLowerBounds): _*), Map())
    
    // 6. solve C to yield a substitution S' = [$T_i -> U_i]
    val (tSub, oSub) = solve(and(constraint, and(upperBounds,lowerBounds))).getOrElse(return None)

    // 7. instantiate infArrow with [U_i] to get resultArrow
    val prenorm = tSub(infTyp)
    val extendedAnalyzer = analyzer.extendJ(prenorm.getInfo().getStaticParams(),None)
    val resultTyp = extendedAnalyzer.normalize(prenorm).asInstanceOf[T]
//   println("Static params helper normalized " + prenorm + " to " + resultTyp);

    // 8. return (resultArrow,StaticArgs([U_i]))
    val resultArgs = sargs.map {
      case STypeArg(info, lifted, typ) =>  STypeArg(info, lifted, tSub(typ))
      case SOpArg(info, lifted, op) => SOpArg(info, lifted, oSub(op))
      case sarg => sarg
    }

    Some((resultTyp, resultArgs))
  }

  
  //TODO: Make op constraint
  def inferLiftedStaticParams(fnType: ArrowType, argType: Type,
      mOps: Option[(Op,Op)])(implicit analyzer: TypeAnalyzer): Option[(ArrowType, List[StaticArg])] = {

    val sparams = getStaticParams(fnType).filter(_.isLifted)
    if (sparams.isEmpty || fnType.getMethodInfo.isNone) return Some((fnType, Nil))

    // Builds a constraint given the arrow with inference variables.
    def makeConstraint(infArrow: ArrowType, map: Map[Op, Op]): CFormula = {
      // Get the type of the `self` arg and form selfArg <:? selfType
      val SMethodInfo(selfType, selfPosition) = infArrow.getMethodInfo.unwrap
      val tConstraint = getTypeAt(argType, selfPosition) match {
        case Some(selfArgType) => checkSubtype(selfArgType, selfType)
        case None => False
      }
      val oConstraint = mOps match {
        case Some((op, ov_op)) => 
          analyzer.equivalent(op, map.getOrElse(analyzer.normalize(ov_op), ov_op))
        case None => True
      }
      and(tConstraint, oConstraint)
    }

    // Do the inference.
    inferStaticParamsHelper(fnType, makeConstraint, true, false)
  }

  /**
   * Define an ordering relation on arrows with their instantiations. That is,
   * is candidate1 more specific than candidate2?
   */
  def moreSpecificCandidate(candidate1: AppCandidate,
    candidate2: AppCandidate)(implicit coercions: CoercionOracle): Boolean = {

    val AppCandidate(SArrowType(_, domain1, range1, _, _, mi1), _, args1, _, _) = candidate1
    val AppCandidate(SArrowType(_, domain2, range2, _, _, mi2), _, args2, _, _) = candidate2

    // If these are dotted methods, add in the self type as an implicit first
    // parameter. The new domains will be a tuple of the form
    // `(selfType, domainType)`.
    val (newDomain1, newDomain2) = (mi1, mi2) match {
      case (Some(SMethodInfo(selfType1, -1)),
        Some(SMethodInfo(selfType2, -1))) =>
        (getDomainAndSelfType(candidate1.arrow),
          getDomainAndSelfType(candidate2.arrow))
      case _ => (domain1, domain2)
    }

    // Determine if a coercion occurred.
    val coercion1 = args1.exists(_.isInstanceOf[CoercionInvocation])
    val coercion2 = args2.exists(_.isInstanceOf[CoercionInvocation])

    // If one did not use coercions and the other did, the one without coercions
    // is more specific.
    (coercion1, coercion2) match {
      case (true, false) => false
      case (false, true) => true
      case _ => coercions.moreSpecific(newDomain1, newDomain2)
    }
  }

  /**
   * Determines if the given overloading is dynamically applicable.
   */
  def isDynamicallyApplicable(overloading: Overloading,
    bestArrow: ArrowType,
    unliftedSargs: List[StaticArg],
    liftedSargs: List[StaticArg])(implicit analyzer: TypeAnalyzer): Option[Overloading] = {

    val SOverloading(ovInfo, ovName, origName, Some(ovType), schema) = overloading
    var newOvType: ArrowType = ovType
    val bestArrowDomainAndSelfType = getDomainAndSelfType(bestArrow)

    // If unlifted static args, then instantiate the unlifted static params.
    if (!unliftedSargs.isEmpty)
      newOvType = staticInstantiation(unliftedSargs, newOvType)
        .getOrElse(return None).asInstanceOf[ArrowType]

    // If there were lifted, inferred static args, then instantiate those.
    if (!liftedSargs.isEmpty)
      newOvType = staticInstantiation(liftedSargs,
        newOvType,
        applyLifted = true,
        applyUnlifted = false)
        .getOrElse(return None).asInstanceOf[ArrowType]

    // If there are still some static params in it, then we can't infer them
    // so it's not applicable. If this is not a subtype of the best arrow, then
    // it cannot be picked at runtime.
    if (hasStaticParams(newOvType) ||
      !isSubtype(getDomainAndSelfType(newOvType),
        bestArrowDomainAndSelfType))
      return None

    Some(SOverloading(ovInfo, ovName, origName, Some(newOvType), schema))
  }

  /**
   * Given an applicand and all the application candidates, return the applicand
   * updated with the dynamically applicable overloadings, arrow type, and
   * static args.
   */
  def rewriteApplicand(fn: Expr,
            candidates: List[AppCandidate],
            skipOverloads: Boolean)(implicit analyzer: TypeAnalyzer): Expr = {

    // Pull out the info for the winning candidate.
    val sma@AppCandidate(bestArrow, bestSargs, _, bestOverloading, bestFnl) = candidates.head

    fn match {
      case fn: FunctionalRef =>

        // Get the unlifted static args.
        val (liftedSargs, unliftedSargs) = bestSargs.partition(_.isLifted)

        
        // Get the dynamically applicable overloadings. Any time this method is
        // called, the candidates would have been created with corresponding
        // Overloading nodes; so use those instead of the ones on fn.
        val pruned = pruneMethodCandidates(candidates, sma)
        val overloadings = if (skipOverloads)  Nil else pruned.flatMap { c =>
          c.overloading.flatMap { o =>
            isDynamicallyApplicable(o, bestArrow, unliftedSargs, liftedSargs)
          }
        }

        // Add in the filtered overloadings, the inferred static args,
        // and the statically most applicable arrow to the fn.

        addType(
          addStaticArgs(
            addOverloadings(fn, overloadings, skipOverloads),
            unliftedSargs),
          bestArrow)

      case _ if !bestSargs.isEmpty =>
        NI.nyi("No place to put inferred static args in application.")

      // Just add the arrow type if the applicand is not a FunctionalRef.
      case _ => addType(fn, bestArrow)
    }
  }


  /**
   * Given a TraitIndex or TraitObjectDecl, return a HashMap with one entry for every
   * distinct supertrait of the declared trait or object.  The entry maps the name (stem)
   * of the supertrait to a TraitIndex for that name (stem).
   */

  def allSupertraits(ti: TraitIndex, analyzer: TypeAnalyzer): java.util.HashMap[Id, TraitIndex] =
    allSupertraits(ti.ast, analyzer)

  def allSupertraits(tod: TraitObjectDecl, analyzer: TypeAnalyzer): java.util.HashMap[Id, TraitIndex] =
    mapOverAllSupertraits(tod, analyzer, (x, y) => x)

  /**
   * Given a TraitIndex or TraitObjectDecl, return a HashMap with one entry for every
   * distinct supertrait of the declared trait or object.  The entry maps the name (stem)
   * of the supertrait to a pair of a TraitIndex for that name (stem) and a list of specific
   * static arguments for that stem.
   */

  def allSupertraitsAndStaticArgs(ti: TraitIndex, analyzer: TypeAnalyzer): java.util.HashMap[Id, (TraitIndex, java.util.List[StaticArg])] =
    allSupertraitsAndStaticArgs(ti.ast, analyzer)

  def allSupertraitsAndStaticArgs(tod: TraitObjectDecl, analyzer: TypeAnalyzer): java.util.HashMap[Id, (TraitIndex, java.util.List[StaticArg])] =
    mapOverAllSupertraits(tod, analyzer, (x, y) => (x, y))

  /**
   * Given a TraitIndex or TraitObjectDecl, return a HashMap with one entry for every
   * distinct supertrait of the declared trait or object.  The entry maps the name (stem)
   * of the supertrait to the result of applying a supplied function mapfn to the TraitIndex
   * for that name (stem) and a list of specific static arguments for that stem.  This requires
   * taking a transitive closure of the immediate-supertrait relation and eliminating duplicates.
   */

  def mapOverAllSupertraits[T](tod: TraitObjectDecl,
                        analyzer: TypeAnalyzer,
                        mapfn: (TraitIndex, java.util.List[StaticArg]) => T):
        java.util.HashMap[Id, T] = {
    val history: HierarchyHistory = new HierarchyHistory()
    val allTraits = new java.util.HashMap[Id, T]
    var traitsToDo: List[TraitTypeWhere] = toListFromImmutable(tod.getHeader.getExtendsClause)
    while (!traitsToDo.isEmpty) {
      val doNow = traitsToDo
      traitsToDo = List()
      for (
        STraitTypeWhere(_, ty: TraitType, _) <- doNow;
        if history.explore(ty)
      ) {
        val STraitType(_, name, trait_args, _) = ty
        toOption(analyzer.traits.typeCons(name)) match {
          case Some(ti: TraitIndex) =>
            allTraits.put(name, mapfn(ti, trait_args))
            val paramsToArgs = new StaticTypeReplacer(ti.staticParameters(), trait_args)
            val instantiated_extends_types =
              toListFromImmutable(ti.extendsTypes).map(_.accept(paramsToArgs).asInstanceOf[TraitTypeWhere])
            traitsToDo ++= instantiated_extends_types
          case _ =>  // nothing to do
        }
      }
    }
    allTraits
  }

  /**
   * Given a TraitIndex or TraitObjectDecl, return a HashMap with one entry for every distinct
   * immediate supertrait of the declared trait or object.  The entry maps the name (stem)
   * of the supertrait to a TraitIndex for that name (stem).
   */

  def immediateSupertraits(tod: TraitObjectDecl, analyzer: TypeAnalyzer): java.util.HashMap[Id, TraitIndex] =
    mapOverAllImmediateSupertraits(tod, analyzer, (x, y) => x)

  /**
   * Given a TraitIndex or TraitObjectDecl, return a HashMap with one entry for every
   * distinct supertrait of the declared trait or object.  The entry maps the name (stem)
   * of the supertrait to a list of specific static arguments for that stem.
   */

  def immediateSupertraitsStaticArgs(tod: TraitObjectDecl, analyzer: TypeAnalyzer): java.util.HashMap[Id, java.util.List[StaticArg]] =
    mapOverAllImmediateSupertraits(tod, analyzer, (x, y) => y)

  /**
   * Given a TraitIndex or TraitObjectDecl, return a HashMap with one entry for every
   * distinct supertrait of the declared trait or object.  The entry maps the name (stem)
   * of the supertrait to pair containing a TraitIndex for that name (stem) and
   *  a list of specific static arguments for that stem.
   */

  def immediateSupertraitsAndStaticArgs(tod: TraitObjectDecl, analyzer: TypeAnalyzer): java.util.HashMap[Id, (TraitIndex, java.util.List[StaticArg])] =
    mapOverAllImmediateSupertraits(tod, analyzer, (x, y) => (x, y))
    
  /**
   * Given a TraitIndex or TraitObjectDecl, return a HashMap with one entry for every distinct
   * immediate supertrait of the declared trait or object.  The entry maps the name (stem)
   * of the supertrait to the result of applying a supplied function mapfn to the TraitIndex
   * for that name (stem) and a list of specific static arguments for that stem.  This requires
   * eliminating duplicates.
   */

  def mapOverAllImmediateSupertraits[T](tod: TraitObjectDecl,
                                        analyzer: TypeAnalyzer,
                                        mapfn: (TraitIndex, java.util.List[StaticArg]) => T):
        java.util.HashMap[Id, T] = {
    val history: HierarchyHistory = new HierarchyHistory()
    val immediateTraits = new java.util.HashMap[Id, T]
    var traitsToDo: List[TraitTypeWhere] = toListFromImmutable(tod.getHeader.getExtendsClause)
    for (
      STraitTypeWhere(_, ty: TraitType, _) <- traitsToDo;
      if history.explore(ty)
    ) {
      val STraitType(_, name, trait_args, _) = ty
      toOption(analyzer.traits.typeCons(name)) match {
	case Some(ti: TraitIndex) =>
	  immediateTraits.put(name, mapfn(ti, trait_args))
	case _ =>  // nothing to do
      }
    }
    immediateTraits
  }


  // Invariant: Parameter types of all the methods should exist,
  //            either given or inferred.
  // The methods relation is updated to include all methods
  // found in the inherited traits.  Why imperative update?
  // Because we want to ensure that traversals of multiple
  // traits don't re-traverse their common parents.  This is
  // the simplest way of accomplishing that goal.  Otherwise we
  // need to pass in a "previousMethods" Relation, and return
  // a "newMethods" relation, plumbing it through everywhere:
  // essentially a state monad, which is after all what imperative
  // update gives us for free.
  // It'd be nice to abbreviate the type of the Relation somehow.
  def inheritedMethods(extendedTraits: List[TraitTypeWhere],
                       methods: Relation[IdOrOpOrAnonymousName, (Functional, StaticTypeReplacer, TraitType)],
                       analyzer: TypeAnalyzer):
        Relation[IdOrOpOrAnonymousName, (Functional, StaticTypeReplacer, TraitType)] = 
    inheritedMethods(extendedTraits, methods, analyzer, false)
      
  def inheritedMethods(extendedTraits: List[TraitTypeWhere],
                       methods: Relation[IdOrOpOrAnonymousName, (Functional, StaticTypeReplacer, TraitType)],
                       analyzer: TypeAnalyzer,
                       skipFirstTraitMethods : Boolean):
        Relation[IdOrOpOrAnonymousName, (Functional, StaticTypeReplacer, TraitType)] = {
    if (debugInheritedMethods)
      System.err.println("inheritedMethods " + extendedTraits.map(_.getBaseType))
    val history: HierarchyHistory = new HierarchyHistory()
    val allMethods =
      ( new HashMap[IdOrOpOrAnonymousName, MSet[(Type, Int, List[StaticParam], Functional, StaticTypeReplacer, TraitType)]]
        with MultiMap[IdOrOpOrAnonymousName, (Type, Int, List[StaticParam], Functional, StaticTypeReplacer, TraitType)] )
    // Method name -> parameter types (not incl self), actual decl, type info, decl site
    for (pltPair <- toSet(methods)) {
      val methodName = pltPair.first
      val (f, r, tt) = pltPair.second
      val (paramTy, selfIndex, sparams) = paramTySchemaWithoutSelf(methodName, f, r)
      allMethods.addBinding(methodName, (paramTy, selfIndex, sparams, f, r, tt))
    }
    var traitsToDo: List[TraitTypeWhere] = extendedTraits
    var skip:Boolean = skipFirstTraitMethods
    while (!traitsToDo.isEmpty) {
      val doNow = traitsToDo
      traitsToDo = List()
      for (
        STraitTypeWhere(_, ty: TraitType, _) <- doNow;
        if history.explore(ty)
      ) {
        val STraitType(_, name, trait_args, _) = ty
        toOption(analyzer.traits.typeCons(name)) match {
          case Some(ti: TraitIndex) =>
            val tindex = ti.asInstanceOf[TraitIndex]
            // Instantiate methods with static args
            val paramsToArgs = new StaticTypeReplacer(ti.staticParameters,
              toJavaList(trait_args))
            if (!skip) {
              
            def oneMethod(methodName: IdOrOp, methodFunc: Functional) = {
              val methodName2 = methodName.accept(paramsToArgs);
              val (paramTy, selfIndex, sparams) =
                paramTySchemaWithoutSelf(methodName, methodFunc, paramsToArgs)
              val first_analyzer = analyzer.extend(sparams, None)
              var new_analyzer = first_analyzer
              if (!methodFunc.name().equals(methodName)) {
                // TODO: work around the fact that TraitIndex includes
                // two copies of the same Functional for exported functional
                // methods, one under the local methodName and the other
                // under the unambiguous methodName.  Really the latter ought to
                // have a methodFunc with a different name() and no body.
                // System.err.println("   oneMethod: "+ methodFunc+" named "+methodName);
              } else {
                if (debugInheritedMethods)
                    System.err.println("   oneMethod: "+ methodFunc+" named "+methodName);
                var isOverridden = false
                val newOverloadings =
                  new HashSet[(Type, Int, List[StaticParam], Functional, StaticTypeReplacer, TraitType)]()
                for (
                  overloadings <- allMethods.get(methodName);
                  tup@(paramTyX, selfIndexX, sps, f, s, tyX) <- overloadings
                ) {
                  // ty.methodName(paramTy) vs tyX.methodName(paramTyX)
                  // ty > tyX  paramTy <= paramTyX    existing tyX overrides new ty
                  // ty < tyX  paramTy >= paramTyX    ty overrides extant tyX
                  // otherwise no relation.
                  if (debugInheritedMethods)
                      System.err.println("    "+methodFunc+" comparing "+f)

                  new_analyzer = new_analyzer.extend(sps, None)
                  
                  val tyX_le_ty = new_analyzer.lteq(tyX, ty)
                  val ty_le_tyX = new_analyzer.lteq(ty, tyX)
                  
                  if (tyX_le_ty) {
                    if (! ty_le_tyX) {
                        /*
                         * old receiver is strictly more specific
                         */
                        if (!isOverridden) {
                            isOverridden = selfIndex == selfIndexX &&
                                           new_analyzer.lteq(paramTy, paramTyX)
                            if (debugInheritedMethods && isOverridden)
                                System.err.println("    "+methodFunc+" overridden by "+f)
                        }
                    }
                    if (debugInheritedMethods)
                         System.err.println("      overload " + f)
                    newOverloadings += tup // keep old overloading
                    
                  } else if (ty_le_tyX &&
                             selfIndex == selfIndexX &&
                             new_analyzer.lteq(paramTy, paramTyX)) {
                    /*
                     * New receiver is more specific
                     */
                    // new overrides old, so do not include old.
                    if (debugInheritedMethods)
                         System.err.println("      dropped " + f)
                  } else {
                    // no relation to old, keep old.
                    if (debugInheritedMethods)
                         System.err.println("      overload " + f)
                    newOverloadings += tup
                  }
                  new_analyzer = first_analyzer
                }
                if (!isOverridden) {
                  if (debugInheritedMethods)
                         System.err.println("      added.")
                  newOverloadings += ((paramTy, selfIndex, sparams, methodFunc, paramsToArgs, ty))
                }
                
                allMethods += ((methodName, newOverloadings))
                if (debugInheritedMethods)
                         System.err.println("     allMethods=" +allMethods)
               }
            }
            def onePair[T <: Functional](t: Pair[IdOrOpOrAnonymousName, T]) =
              t.first match {
                case id: IdOrOp => oneMethod(id, t.second)
                case _ => ()
              }
            ti.dottedMethods.foreach(onePair)
            ti.functionalMethods.foreach(onePair)
            ti.getters.entrySet.foreach((e: JMap.Entry[Id, FieldGetterMethod]) => oneMethod(e.getKey, e.getValue))
            ti.setters.entrySet.foreach((e: JMap.Entry[Id, FieldSetterMethod]) => oneMethod(e.getKey, e.getValue))
            }
            val instantiated_extends_types =
              toListFromImmutable(ti.extendsTypes).map(_.accept(paramsToArgs)
                .asInstanceOf[TraitTypeWhere])
            traitsToDo ++= instantiated_extends_types
          case _ =>
        }
        skip = false
      }
    }
    for (
      (methodName, overloadings) <- allMethods;
      (_, _, _, f, s, tt) <- overloadings
    ) {
      methods.add(methodName, (f, s, tt))
    }
    methods
  }

  def inheritedMethods(extendedTraits: List[TraitTypeWhere],
                       analyzer: TypeAnalyzer):
        Relation[IdOrOpOrAnonymousName, (Functional, StaticTypeReplacer, TraitType)] = {
    val methods =
      new IndexedRelation[IdOrOpOrAnonymousName, (Functional, StaticTypeReplacer, TraitType)](false)
    inheritedMethods(extendedTraits, methods, analyzer)
  }

  def inheritedMethods(extendedTraits: JList[TraitTypeWhere],
                       analyzer: TypeAnalyzer):
        Relation[IdOrOpOrAnonymousName, (Functional, StaticTypeReplacer, TraitType)] =
    inheritedMethods(toListFromImmutable(extendedTraits),
      analyzer)

  /**
   * Returns a relation for the methods that are inherited 
   */
  def properlyInheritedMethods(tt: TraitType,
                               analyzer: TypeAnalyzer):
        Relation[IdOrOpOrAnonymousName, (Functional, StaticTypeReplacer, TraitType)] =
    gatherMethods(tt, analyzer, false)
    
  def properlyInheritedMethods(tt_id: Id,
                               analyzer: TypeAnalyzer):
        Relation[IdOrOpOrAnonymousName, (Functional, StaticTypeReplacer, TraitType)] =
    gatherMethods(tt_id, analyzer, false)
//   {
//     val methods =
//       new IndexedRelation[IdOrOpOrAnonymousName, (Functional, StaticTypeReplacer, TraitType)](false)
//     inheritedMethods(List(NF.makeTraitTypeWhere(tt)), methods, analyzer, true)
//   }


  def properlyInheritedMethodsNotIdenticallyCovered(tt_id: Id, analyzer: TypeAnalyzer):
         Relation[IdOrOpOrAnonymousName, (Functional, StaticTypeReplacer, TraitType)] = {
    removeIdenticallyCoveredMethods(properlyInheritedMethods(tt_id, analyzer), analyzer)
  }

  def allMethodsNotIdenticallyCovered(tt: TraitType, analyzer: TypeAnalyzer):
         Relation[IdOrOpOrAnonymousName, (Functional, StaticTypeReplacer, TraitType)] = {
    removeIdenticallyCoveredMethods(allMethods(tt, analyzer), analyzer)
  }

  private def removeIdenticallyCoveredMethods(methods: Relation[IdOrOpOrAnonymousName, (Functional, StaticTypeReplacer, TraitType)],
  	      				      analyzer: TypeAnalyzer):
        Relation[IdOrOpOrAnonymousName, (Functional, StaticTypeReplacer, TraitType)] = {
//    println("Removing identically covered methods: " + methods)
    val tsa = new TypeSchemaAnalyzer()(analyzer)
    val result = new IndexedRelation[IdOrOpOrAnonymousName, (Functional, StaticTypeReplacer, TraitType)](false)
    // For each method, check to see whether it is covered by another definition with identical domain.
    for ((meth, str, tt) <- methods.secondSet) {
      val thisDomain = fullyReplacedMethodDomain(meth, str, tsa)
      var include = true
      for ((meth2, str2, tt2) <- methods.secondSet) {
        if ((meth2.name == meth.name) &&
            (meth != meth2) &&
            (meth.asInstanceOf[HasSelfType].selfPosition == meth2.asInstanceOf[HasSelfType].selfPosition) &&
	    analyzer.lteq(tt2, tt) && !analyzer.lteq(tt, tt2)) {
          val thatDomain = fullyReplacedMethodDomain(meth2, str2, tsa)
          if (tsa.equivalentED(thisDomain, thatDomain)) include = false
        }
      }
      if (include) result.add(meth.name, (meth, str, tt))
    }
//    println("   with the result: " + result)
    result
  }

  def fullyReplacedMethodDomain(meth: Functional, str: StaticTypeReplacer, tsa: TypeSchemaAnalyzer): Type = {
    val dom = tsa.makeDomainFromArrow(makeLiftedArrowWithoutSelfFromFunctional(meth).get)
    insertStaticParams(clearStaticParams(dom), getStaticParameters(meth, true).map(sp => str.replaceStaticParam(sp)).toList)
  }


//   def allMethods(tt: TraitType, analyzer: TypeAnalyzer):
//         Relation[IdOrOpOrAnonymousName, (Functional, StaticTypeReplacer, TraitType)] =
//     inheritedMethods(List(NF.makeTraitTypeWhere(tt)), analyzer)

  def allMethods(tt: TraitType, analyzer: TypeAnalyzer):
         Relation[IdOrOpOrAnonymousName, (Functional, StaticTypeReplacer, TraitType)] =
    gatherMethods(tt, analyzer, true)

  def gatherMethods(tt: TraitType, analyzer: TypeAnalyzer, includeSelf: Boolean):
        Relation[IdOrOpOrAnonymousName, (Functional, StaticTypeReplacer, TraitType)] = {
          val tmp = gatherMethods(tt.getName, analyzer, includeSelf)
          val tt_args = tt.getArgs
          if ( tt_args.isEmpty ) {
            tmp
          } else {
            val ti = analyzer.traits.typeCons(tt.getName).unwrap
            val tt_params = ti.staticParameters
            val outer_str = new StaticTypeReplacer(tt_params, tt_args)
            // leave this for now to keep compilable.
            def relativizer(x:Pair[IdOrOpOrAnonymousName, (Functional, StaticTypeReplacer, TraitType)]):
               Pair[IdOrOpOrAnonymousName, (Functional, StaticTypeReplacer, TraitType)] = {
                val iooan = x.first()
                val (fl, str, tt) = x.second()
                new Pair(iooan, (fl, new StaticTypeReplacer(str, outer_str), outer_str.replaceIn(tt).asInstanceOf[TraitType]))
            }
            // It would be so nice if the Rice PLT tools came with bundled documentation, so I could figure out how to use them
            // tmp.applyToAll(relativizer)
            val result = new IndexedRelation[IdOrOpOrAnonymousName, (Functional, StaticTypeReplacer, TraitType)](false)
            for (bogus <- tmp) {
              result.add(relativizer(bogus))
            }
            result
          }
          
  }

  private def gatherMethods(tt_name: Id, analyzer: TypeAnalyzer, includeSelf: Boolean):
        Relation[IdOrOpOrAnonymousName, (Functional, StaticTypeReplacer, TraitType)] = {
    val result = new IndexedRelation[IdOrOpOrAnonymousName, (Functional, StaticTypeReplacer, TraitType)](false)
    toOption(analyzer.traits.typeCons(tt_name)) match {
      case Some(ti: TraitIndex) =>
        val traitsAndArgs = allSupertraitsAndStaticArgs(ti, analyzer)
        for (tname <- traitsAndArgs.keySet;
             (name, set) <- { val (ti, trait_args) = traitsAndArgs.get(tname); declaredMethods(tname, ti, Some(trait_args)) };
             data <- set)
          result.add(name, data)
        if (includeSelf) {
	  for ((name, set) <- declaredMethods(ti.ast.getHeader.getName.asInstanceOf[Id], ti, None);
               data <- set)
            result.add(name, data)
        }
      case _ =>
    }
    result
  }

   private val nullStaticTypeReplacer = new StaticTypeReplacer(new java.util.LinkedList[StaticParam](), new java.util.LinkedList[StaticArg]())

  /**
   * Returns a Scala HashMap that maps method names to sets of tuples
   * (Functional, StaticTypeReplacer, TraitType).
   */

  def declaredMethods(tname: Id, ti: TraitIndex, trait_args: Option[java.util.List[StaticArg]]):
        HashMap[IdOrOpOrAnonymousName, MSet[(Functional, StaticTypeReplacer, TraitType)]] = {
    val result = (new HashMap[IdOrOpOrAnonymousName, MSet[(Functional, StaticTypeReplacer, TraitType)]]
                  with MultiMap[IdOrOpOrAnonymousName, (Functional, StaticTypeReplacer, TraitType)])
    val (paramsToArgs, ty) = trait_args match {
       case Some(ta) => (new StaticTypeReplacer(ti.staticParameters, ta),
       	                 NF.makeTraitTypeForScala(tname, ta))
       case None => (nullStaticTypeReplacer,
                     NF.makeTraitType(NU.getSpan(tname), true, tname,
                                      staticParamsToArgs(ti.staticParameters), Collections.emptyList[StaticParam]))
    }

    def oneMethod(methodName: IdOrOp, methodFunc: Functional) = {
      if (!methodFunc.name().equals(methodName)) {
	// TODO: work around the fact that TraitIndex includes
	// two copies of the same Functional for exported functional
	// methods, one under the local methodName and the other
	// under the unambiguous methodName.  Really the latter ought to
	// have a methodFunc with a different name() and no body.
	// System.err.println("   oneMethod: "+ methodFunc+" named "+methodName);
      } else {
	result.addBinding(methodName, (methodFunc, paramsToArgs, ty))
      }
    }

    def onePair[T <: Functional](t: Pair[IdOrOpOrAnonymousName, T]) =
      t.first match {
	case id: IdOrOp => oneMethod(id, t.second)
	case _ => ()
      }

    def oneMapping(t: JMap.Entry[Id, Method]) = oneMethod(t.getKey, t.getValue)

    ti.dottedMethods.foreach(onePair)
    ti.functionalMethods.foreach(onePair)
    ti.getters.entrySet.foreach((e: JMap.Entry[Id, FieldGetterMethod]) => oneMethod(e.getKey, e.getValue))
    ti.setters.entrySet.foreach((e: JMap.Entry[Id, FieldSetterMethod]) => oneMethod(e.getKey, e.getValue))

    result
  }


  // TODO: This does not work properly because typechecker does not know a name for the type of an object expression.
  def allMethodsOfObjectExpr(oe: ObjectExpr, analyzer: TypeAnalyzer):
        Relation[IdOrOpOrAnonymousName, (Functional, StaticTypeReplacer, TraitType)] = {
    val methods =
      new IndexedRelation[IdOrOpOrAnonymousName, (Functional, StaticTypeReplacer, TraitType)](false)
    inheritedMethods(toList(oe.getHeader.getExtendsClause), methods, analyzer, false)
  }
 
  
  type FnAndSArgs = (Functional, Option[JList[StaticArg]])
  type GSDF = (Boolean, Boolean, Boolean, Boolean)
  type GetIndex = Id => Option[TypeConsIndex]
  type ErrorSignal = (String, HasAt) => Unit
  
  def uberInheritedMethods(b: BaseType)
        (implicit gi: GetIndex, h: HierarchyHistory, e: ErrorSignal, bs: GSDF): List[FnAndSArgs] = b match {
    case t@STraitType(_, id, _, _) => 
      gi(id) match {
        case Some(i: TraitIndex) =>
          uberInheritedMethods(i, Some(t.getArgs()))
        case None =>
          e("The name" + id + " does not appear in the trait table.", id)
          List()
    }
    case a: AnyType => List()
    case _ =>
      // Only other possibility is a VarType.
      e("Type variable " + b + " must not appear " +
          "in the extends clause of a trait or object declaration.", b)
      List()
  }
  
  def uberInheritedMethods(tw: TraitTypeWhere)
        (implicit gi: GetIndex, h: HierarchyHistory, e: ErrorSignal, bs: GSDF): List[FnAndSArgs] = tw match {
    case STraitTypeWhere(_, t, _) if h.explore(t) => uberInheritedMethods(t)
    case _ => List()
  }
  
  def uberInheritedMethods(t: TraitIndex, a: Option[JList[StaticArg]])
        (implicit gi: GetIndex, h: HierarchyHistory, e: ErrorSignal, bs: GSDF): List[FnAndSArgs] = {
    def sub(x: TraitTypeWhere) = a match {
      case Some(as) => 
        val inner = new StaticTypeReplacer(t.staticParameters, as)
        x.accept(inner).asInstanceOf[TraitTypeWhere]
      case None => x
    }
    val eMethods = t.extendsTypes.flatMap{x => uberInheritedMethods(sub(x))}.toList
    val (g, s, d, f) = bs
    val get = if (g) t.getters().values().toList else List()
    val set = if (s) t.setters().values().toList else List()
    val dot = if (d) t.dottedMethods().secondSet().toList else List()
    val fun = if (f) t.functionalMethods().secondSet().toList else List()
    eMethods ++ (get ++ set ++ dot ++ fun).map((_, a))
  }

  def commonInheritedMethods(ws: Iterable[TraitTypeWhere], tt: TraitTable): List[Method] = {
    implicit val gi = (i: Id) => toOption(tt.typeCons(i))
    implicit val h = new HierarchyHistory
    implicit val e = (a: String, b: HasAt) => ()
    implicit val bs = (true, true, true, false)
    val fnsAndArgs = ws.toList.flatMap{uberInheritedMethods(_)}
    fnsAndArgs.map{
      case (m: Method, Some(as)) => m.instantiateTraitStaticParameters(toJavaList(List[StaticParam]()) , as)
      case (m, None)  => bug(m + "is not a method or did not have static args.")
    }
  }
  
  def exprInheritedMethods(ws: Iterable[TraitTypeWhere], env: NameEnv)(implicit error: ErrorSignal): Set[FnDecl] = {
    implicit val gi = (i: Id) => Some(env.typeConsIndex(i))
    implicit val h = new HierarchyHistory
    implicit val bs = (false, false, true, false)
    val fnsAndArgs = ws.toList.flatMap{uberInheritedMethods(_)}
    fnsAndArgs.map{case (m: DeclaredMethod, a) => m.ast}.toSet
  }
  
  def oldInheritedMethods(ws: Iterable[TraitTypeWhere])(implicit ta: TypeAnalyzer): Map[IdOrOpOrAnonymousName, FnAndSArgs] = {
    implicit val gi = (i: Id) => Some(ta.typeCons(i))
    implicit val h = new HierarchyHistory
    implicit val e = (a: String, b: HasAt) => ()
    implicit val bs = (true, true, true, true)
    val fnsAndArgs = ws.toList.flatMap{uberInheritedMethods(_)}
    val idToFns = fnsAndArgs.groupBy{case (a, b) => a.name}
    val oracle = new OverloadingOracle()
    
    null
  }
  
  // TODO: Remove this
  private def paramTySchemaWithoutSelf(name: IdOrOpOrAnonymousName, func: Functional,
    paramsToArgs: StaticTypeReplacer) = {
    val span = NU.getSpan(name)
    val params = toListFromImmutable(func.parameters)
    val sparams = toListFromImmutable(func.staticParameters)
    val (paramsSansSelf, sp) =
      func match {
        case st: HasSelfType if st.selfPosition >= 0 =>
          val stp = st.selfPosition
          (params.take(stp) ++ params.drop(stp + 1), stp)
        case _ => (params, -1)
      }
    paramsToType(paramsSansSelf, span) match {
      case Some(t) => (paramsToArgs.replaceIn(t), sp, sparams)
      case _ => (NF.makeVoidType(span), sp, sparams)
    }
  }
  
  def paramTypeWithoutSelf(func: Functional with HasSelfType) = {
    val stp = func.selfPosition
    val params = toListFromImmutable(func.parameters)
    val span = NU.getSpan(func.name)
    val paramsSansSelf =
      if (stp >= 0)
        params.take(stp) ++ params.drop(stp + 1)
      else
        params
    val paramTypeSansSelf = paramsToType(paramsSansSelf, span).get
    (paramTypeSansSelf, func.selfType.unwrap, stp)
  }

  /* Returns the type of the given list of parameters. */
  def paramsToType(params: List[Param], span: Span): Option[Type] =
    params.size match {
      case 0 => Some(NF.makeVoidType(span))
      case 1 => paramToType(params.head)
      case _ =>
        val elems = params.map(paramToType)
        if (elems.forall(_.isDefined))
          Some(NF.makeTupleType(NU.spanAll(toJavaList(params)),
            toJavaList(elems.map(_.get))))
        else None
    }

  /* Returns the type of the given parameter. */
  def paramToType(param: Param): Option[Type] =
    toOption(param.getIdType) match {
      case Some(ty) =>
        ty match {
          case p@SPattern(_, _, _) => bug("Pattern should be desugared away: " + p)
          case t@SType(_) => Some(t)
        }
      case _ =>
        toOption(param.getVarargsType) match {
          case Some(ty) => Some(ty)
          case _ => None
        }
    }

  /**
   * Given an ObjectExpr, returns the Type of the expression.
   * @return
   */
  def getObjectExprType(obj: ObjectExpr): SelfType = {
    var extends_types =
      toListFromImmutable(NU.getExtendsClause(obj)).map(_.getBaseType)
    if (extends_types.isEmpty) extends_types = List(Types.OBJECT)
    NF.makeObjectExprType(toJavaList(extends_types))
  }

  /**
   * Simply provides a convenient extractor pattern for getting the self type
   * and self position out of an AppCandidate.
   */
  object AppCandidateMethodInfo {
    def unapply(c: AppCandidate): Option[(Type, Int)] = c match {
      case AppCandidate(SArrowType(_, _, _, _, _, Some(SMethodInfo(t, p))), _, _, _, _) =>
        Some((t, p))
      case _ => None
    }
  }

  /**
   * Prunes any candidates of overlapping functional and dotted methods. We
   * prune a candidate method if there is an applicable candidate whose self
   * parameter / receiver is a supertype, and the remaining parameters are
   * non-disjoint. The statically most applicable candidate should never be
   * pruned.
   */
  def pruneMethodCandidates(candidates: List[AppCandidate],
    sma: AppCandidate)(implicit analyzer: TypeAnalyzer): List[AppCandidate] = {
    // Partition the candidates by self position.
    val candidateBuckets = new HashMap[Int, ArrayBuffer[AppCandidate]]
    for (c@AppCandidateMethodInfo(_, pos) <- candidates) {
      candidateBuckets.getOrElseUpdate(pos, new ArrayBuffer) += c
    }

    // Now prune each bucket.
    val pruned = new HashSet[AppCandidate]
    for (bucket <- candidateBuckets.valuesIterator) {

      // Loop over distinct, unpruned candidate pairs.
      for {
        c1@AppCandidateMethodInfo(st1, _) <- bucket
        c2@AppCandidateMethodInfo(st2, _) <- bucket
        if (c1 ne c2) && !pruned(c1) && !pruned(c2)
      } {

        // If c2 is not the statically most applicable candidate, and c2's self
        // type is a strict subtype of c1's self type, and if c1
        // and c2 domains aren't disjoint, then they overlap -- prune c2.
        if ((c2 ne sma)
          && !isTrue(analyzer.equivalent(st1, st2))
          && isTrue(analyzer.subtype(st2, st1))
          && !analyzer.definitelyExcludes(c1.arrow.getDomain, c2.arrow.getDomain)) {
          pruned += c2
        }
      }
    }

    // Remove the pruned candidates.
    candidates.filterNot(c => pruned.contains(c))
  }

  /**
   * If this arrow type has a self type in the dotted method position, then
   * return the pair `(st, dom)`, where `st` is the self type and `dom` is the
   * domain. Otherwise, just return the domain.
   */
  def getDomainAndSelfType(t: ArrowType): Type =
    toOption(t.getMethodInfo) match {
      case Some(SMethodInfo(st, -1)) =>
        STupleType(t.getDomain.getInfo, List(st, t.getDomain), None, Nil)
      case _ => t.getDomain
    }

  /**
   * Return a set of all the VarTypes found within the given type.
   */
  def getVarTypes(t: Type): Set[VarType] = {
    val varTypes = new HashSet[VarType]

    // Add all VarTypes to the set.
    object varTypeFinder extends Walker {
      override def walk(node: Any) = node match {
        case x: VarType => varTypes += x; x
        case _ => super.walk(node)
      }
    }
    varTypeFinder(t)

    // Return the VarTypes as an immutable set.
    varTypes.toSet
  }

  /**
   * Return a set of all the inference vars found within the given type.
   */
  def getInferenceVars(t: Type): Set[_InferenceVarType] = {
    val infVars = new HashSet[_InferenceVarType]

    // Add all inference vars to the set.
    object infVarFinder extends Walker {
      override def walk(node: Any) = node match {
        case x: _InferenceVarType => infVars += x; x
        case _ => super.walk(node)
      }
    }
    infVarFinder(t)

    // Return the inference vars as an immutable set.
    infVars.toSet
  }

  /**
   * Given a partial function from type variables to types, return a new
   * function that will recursively apply the former to each VarType or
   * _InferenceVarType in the domain. Effectively, a substitution of the form
   * {T -> U} will be lifted to recursively replace any occurrence of T with U
   * inside another type, where T and U can be either VarTypes or
   * _InferenceVarTypes.
   *
   * The implicit Manifest object allows us to know the runtime representation
   * of T, the type of the type variable on which `subst` is defined. From it
   * we can get the Class object for T and check if a given type is an
   * instance of that, and if so, we then check whether `subst` is defined at
   * that type. Note that for any statically known class type, its Manifest
   * exists and will be implicitly passed at the call site.
   */
  def liftSubstitution[T, U, V](subst: PartialFunction[U, T])(implicit m: scala.reflect.Manifest[U]): V => V = {

    // Get the runtime Class object for T so that we can be sure whether subst
    // is defined on it or not.
    val tClass = m.erasure

    // Create a walker that replace any occurrence of the parameter found
    // within an AST.
    object replacer extends Walker {
      override def walk(node: Any): Any = node match {
        case x: U if tClass.isInstance(x) && subst.isDefinedAt(x) => subst(x)
        case _ => super.walk(node)
      }
    }

    // Create the resulting closure that simply applies the walker.
    (t: V) => replacer(t).asInstanceOf[V]
  }

  def killIvars: Type => Type = liftSubstitution(new PartialFunction[_InferenceVarType, Type] {
    def apply(y: _InferenceVarType) = BOTTOM
    def isDefinedAt(y: _InferenceVarType) = true
  })

  /**
   * Does the given ast contain any AST nodes
   * that should be removed after type checking?
   *
   * After type checking, the following nodes should be removed:
   *     AmbiguousMultifixOpExpr
   *     ArrayType
   *     Juxt
   *     MathItem
   *     MathPrimary
   *     MatrixType
   *     _InferenceVarType
   */
  def assertAfterTypeChecking(ast: Node): Boolean = {
    var result = false
    object outFinder extends Walker {
      override def walk(node: Any) =
        if (node.isInstanceOf[OutAfterTypeChecking])
          result = true
        else super.walk(node)
    }
    outFinder(ast)
    result
  }

  val leftOxfordBracket = "[\\"
  val rightOxfordBracket = "\\]"

  def typeToString(t: Type): String = {
    if (t.getInfo.getStaticParams.isEmpty)
      return t.toString
    val sparams = getStaticParams(t)
    sparams.mkString(leftOxfordBracket, ", ", rightOxfordBracket) + t.toString
  }

  def typeAndSpanToString(t: Type): String = typeToString(t) + " @ " + NU.getSpan(t) 
}
