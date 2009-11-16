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

package com.sun.fortress.scala_src.useful

import _root_.java.util.ArrayList
import _root_.java.util.{List => JList}
import _root_.java.util.{Map => JMap}
import scala.collection.{Set => CSet}
import edu.rice.cs.plt.tuple.Pair
import edu.rice.cs.plt.collect.Relation
import edu.rice.cs.plt.collect.IndexedRelation
import com.sun.fortress.compiler.GlobalEnvironment
import com.sun.fortress.compiler.Types
import com.sun.fortress.compiler.Types.ANY
import com.sun.fortress.compiler.Types.BOTTOM
import com.sun.fortress.compiler.Types.OBJECT
import com.sun.fortress.compiler.index._
import com.sun.fortress.compiler.typechecker.StaticTypeReplacer
import com.sun.fortress.compiler.typechecker.TypesUtil
import com.sun.fortress.exceptions.InterpreterBug.bug
import com.sun.fortress.exceptions.TypeError
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.ExprFactory
import com.sun.fortress.nodes_util.{NodeFactory => NF}
import com.sun.fortress.nodes_util.{NodeUtil => NU}
import com.sun.fortress.nodes_util.Span
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.typechecker.ConstraintFormula
import com.sun.fortress.scala_src.typechecker.CnFalse
import com.sun.fortress.scala_src.typechecker.CnTrue
import com.sun.fortress.scala_src.typechecker.TraitTable
import com.sun.fortress.scala_src.types.TypeAnalyzer
import com.sun.fortress.scala_src.useful.Iterators._
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.Sets._
import com.sun.fortress.scala_src.useful.SExprUtil._
import com.sun.fortress.useful.HasAt
import com.sun.fortress.useful.MultiMap
import com.sun.fortress.useful.NI


object STypesUtil {

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

  /** A function application candidate. */
  type AppCandidate = (ArrowType, List[StaticArg], List[Expr])

  /**
   * Return the arrow type of the given Functional index.
   */
  def makeArrowFromFunctional(f: Functional): Option[ArrowType] = {
    val returnType = toOption(f.getReturnType).getOrElse(return None)
    val params = toList(f.parameters).map(NU.getParamType)
    val argType = makeArgumentType(params)
    val sparamsJava = f.staticParameters
    val sparams = toList(sparamsJava)
    val effect = NF.makeEffect(f.thrownTypes)
    val where = f match {
      case f:Constructor => f.where
      case _ => none[WhereClause]
    }
    val info = f match {
      case m:HasSelfType if m.selfType.isNone =>
          bug("No selfType on functional %s".format(f))
      case m:HasSelfType =>
          Some(SMethodInfo(m.selfType.get, m.selfPosition))
      case _ => None
    }
    Some(NF.makeArrowType(NF.typeSpan,
                          false,
                          insertStaticParams(argType, sparams),
                          insertStaticParams(returnType, sparams),
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
      case _ => NU.spanTwo(NU.getSpan(ps.first), NU.getSpan(ps.last))
    }

    // Extract out the appropriate parameter types.
    ps.foreach(p => p match {
      case SParam(_, _, _, _, _, Some(vaType)) => // Vararg
        varargsType = Some(vaType)
      case SParam(_, name, _, Some(idType), Some(expr), _) => // Keyword
        keywordTypes.add(NF.makeKeywordType(name, idType))
      case SParam(_, _, _, Some(idType), _, _) => // Normal
        paramTypes.add(idType)
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
                    oldParams: List[Param])
                   (implicit analyzer: TypeAnalyzer): Option[List[Param]] = {

    // Get all the params with inference vars filled in.
    val params = oldParams.map(p => p match {
      case SParam(info, name, mods, None, defaultExpr, None) =>
        SParam(info,
               name,
               mods,
               Some(NF.make_InferenceVarType(info.getSpan)),
               defaultExpr,
               None)
      case _ => p
    })

    // Get the substitution resulting from params :> expectedDomain
    val paramsDomain = makeDomainType(params).get
    val subst = analyzer.subtype(expectedDomain, paramsDomain).solve(Map())
    subst.map(s =>
      params.map(p => p match {
        case SParam(info, name, mods, Some(idType), defaultExpr, None) =>
          SParam(info,
                 name,
                 mods,
                 Some(substituteTypesForInferenceVars(s, idType)),
                 defaultExpr,
                 None)
        case _ => p
      }))
  }

  /**
   * Make a type for the given list of bindings. Returns None if not all
   * bindings had types.
   */
  def makeLhsType(ls: List[LValue]): Option[Type] = {
    val span = ls match {
      case Nil => NF.typeSpan
      case _ => NU.spanTwo(NU.getSpan(ls.first), NU.getSpan(ls.last))
    }
    val types = ls.map(lv => lv match {
      case SLValue(_, _, _, Some(typ), _) => typ
      case _ => return None
    })
    Some(NF.makeMaybeTupleType(span, toJavaList(types)))
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
        Some(List.map2(ls, elts)((lv, typ) => {
          val SLValue(info, name, mods, _, mutable) = lv
          SLValue(info, name, mods, Some(typ), mutable)
        }))

      case _:TupleType => None

      case _:Type if ls.length == 1 =>
        Some(ls.map(lv => {
          val SLValue(info, name, mods, _, mutable) = lv
          SLValue(info, name, mods, Some(typ), mutable)
        }))

      case _ => None
    }

  /**
   * Convert a static parameter to the corresponding static arg. Ported from
   * `TypeEnv.staticParamsToArgs`.
   */
  def staticParamToArg(p: StaticParam): StaticArg = {
    val span = NU.getSpan(p)
    (p.getName, p.getKind) match {
      case (id:Id, _:KindBool) => NF.makeBoolArg(span, NF.makeBoolRef(span, id), p.isLifted)
      case (id:Id, _:KindDim) => NF.makeDimArg(span, NF.makeDimRef(span, id), p.isLifted)
      case (id:Id, _:KindInt) => NF.makeIntArg(span, NF.makeIntRef(span, id), p.isLifted)
      case (id:Id, _:KindNat) => NF.makeIntArg(span, NF.makeIntRef(span, id), p.isLifted)
      case (id:Id, _:KindType) => NF.makeTypeArg(span, NF.makeVarType(span, id), p.isLifted)
      case (id:Id, _:KindUnit) => NF.makeUnitArg(span, NF.makeUnitRef(span, false, id), p.isLifted)
      case (op:Op, _:KindOp) => NF.makeOpArg(span, ExprFactory.makeOpRef(op), p.isLifted)
      case _ => bug("Unexpected static parameter kind")
    }
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
    toList(typ.getInfo.getStaticParams)

  /**
   * Return an identical type but with the given static params removed from it.
   */
  def clearStaticParams(typ: Type, sparams: List[StaticParam]): Type = {

    // A walker that clears static params out of TypeInfos.
    object paramWalker extends Walker {
      override def walk(node: Any): Any = node match {
        case STypeInfo(a, b, existingSparams, _) =>
          STypeInfo(a, b, existingSparams -- sparams, None)
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
   */
  def insertStaticParams(typ: Type, sparams: List[StaticParam]): Type = {

    // A walker that clears static params out of TypeInfos.
    object paramWalker extends Walker {
      override def walk(node: Any): Any = node match {
        case STypeInfo(a, b, Nil, c) => STypeInfo(a, b, sparams, c)
        case STypeInfo(_, _, _, _) =>
          bug("cannot overwrite static parameters")
        case _ => super.walk(node)
      }
    }
    paramWalker(typ).asInstanceOf[Type]
  }

  /**
   * Determine if the given type is an arrow or intersection of arrow types.
   */
  def isArrows(ty: Type): Boolean =
    TypesUtil.isArrows(ty).asInstanceOf[Boolean]

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
      case SVarType(_, typ, _) => sparams.exists {
        case SStaticParam(_, sp, List(_:AnyType), _, _, _, _) => typ == sp
        case _ => false
      }
      case _ => isArrows(ty)
    }

  /**
   * Performs the given substitution on the body type. Does not replace any
   * inference variables that appear in body but not in the substitution.
   */
  def substituteTypesForInferenceVars(substitution: Map[_InferenceVarType, Type],
                                      body: Type): Type = {

    object substitutionWalker extends Walker {
      override def walk(node: Any): Any = node match {
        case ty:_InferenceVarType => substitution.get(ty).getOrElse(ty)
        case _ => super.walk(node)
      }
    }

    // Perform the substitution on the body type.
    substitutionWalker(body).asInstanceOf[Type]
  }

  /**
   * Returns the type of the static parameter's bound if it is a type parameter.
   */
  def staticParamBoundType(sparam: StaticParam): Option[Type] =
    sparam.getKind match {
      case _:KindType if sparam.getExtendsClause.isEmpty =>
        Some(Types.ANY)
      case _:KindType =>
        Some(NF.makeIntersectionType(sparam.getExtendsClause))
      case _ => None
    }

  /**
   * Given a static parameters, returns a static arg containing a fresh
   * inference variable.
   */
  def makeInferenceArg(sparam: StaticParam): StaticArg = sparam.getKind match {
    case _:KindType => {
      // Create a new inference var type.
      val t = NF.make_InferenceVarType(NU.getSpan(sparam))
      NF.makeTypeArg(NF.makeSpan(t), t, sparam.isLifted)
    }
    case _:KindInt => NI.nyi()
    case _:KindBool => NI.nyi()
    case _:KindDim => NI.nyi()
    case _:KindOp => NI.nyi()
    case _:KindUnit => NI.nyi()
    case _:KindNat => NI.nyi()
    case _ => bug("unexpected kind of static parameter")
  }

  /**
   * Returns a list of conjuncts of the given type. If given an intersection
   * type, this is the set of the constituents. If ANY, this is empty. If some
   * other type, this is the singleton set of that type.
   */
  def conjuncts(ty: Type): Set[Type] = ty match {
    case _:AnyType => Set.empty[Type]
    case SIntersectionType(_, elts) => Set(elts:_*).flatMap(conjuncts)
    case _ => Set(ty)
  }

  /**
   * Returns a list of disjuncts of the given type. If given a union
   * type, this is the set of the constituents. If BOTTOM, this is empty. If some
   * other type, this is the singleton set of that type.
   */
  def disjuncts(ty: Type): Set[Type] = ty match {
    case _:BottomType => Set.empty[Type]
    case SUnionType(_, elts) => Set(elts:_*).flatMap(disjuncts)
    case _ => Set(ty)
  }

  /**
   * Returns TypeConsIndex of "typ".
   */
  def getTypes(typ:Id, globalEnv: GlobalEnvironment,
               compilation_unit: CompilationUnitIndex): TypeConsIndex = typ match {
    case SId(info,Some(name),text) =>
      globalEnv.api(name).typeConses.get(SId(info,None,text))
    case _ => compilation_unit.typeConses.get(typ)
  }

  /** Return the [Scala-based] conditions for subtype <: supertype to hold. */
  def checkSubtype(subtype: Type, supertype: Type)
                  (implicit analyzer: TypeAnalyzer): ConstraintFormula = {

    val constraint = analyzer.subtype(subtype, supertype)

    if (!constraint.isInstanceOf[ConstraintFormula]) {
      bug("Not a ConstraintFormula.")
    }
    constraint.asInstanceOf[ConstraintFormula]
  }

  /** Determine if subtype <: supertype. */
  def isSubtype(subtype: Type, supertype: Type)
               (implicit analyzer: TypeAnalyzer): Boolean =
    checkSubtype(subtype, supertype).isTrue

  /**
   * Replaces occurrences of static parameters with corresponding static
   * arguments in the given body type. In the end, any static parameters
   * in the replaced type will be cleared. If `ignoreLifted` is true, then
   * don't consider lifted static parameters at all.
   *
   * @param args A list of static arguments to apply to the generic type body.
   * @param sparams A list of static parameters
   * @param body The generic type whose static parameters are to be replaced.
   * @return An option of a type identical to body but with every occurrence of
   *         one of its declared static parameters replaced by corresponding
   *         static args. If None, then the instantiation failed.
   */
  def staticInstantiation(sparamsAndSargs: List[(StaticParam, StaticArg)],
                          body: Type)
                         (implicit analyzer: TypeAnalyzer): Option[Type] = body match {

    case t:IntersectionType =>
      val cs = conjuncts(t)
      val ts = cs.flatMap(staticInstantiation(sparamsAndSargs, _))
      if (ts.isEmpty && !cs.isEmpty)
        None
      else
        Some(NF.makeMaybeIntersectionType(toJavaSet(ts)))
    case t:UnionType =>
      val ds = disjuncts(t)
      val ts = ds.flatMap(staticInstantiation(sparamsAndSargs, _))
      if (ts.isEmpty && !ds.isEmpty)
        None
      else
        Some(NF.makeMaybeUnionType(toJavaSet(ts)))
    case _ =>

      // Check that the args match.
      if (!staticArgsMatchStaticParams(sparamsAndSargs)) return None

      // Create mapping from parameter names to static args.
      val paramMap = Map(sparamsAndSargs.map(pa => (pa._1.getName, pa._2)):_*)

      // Gets the actual value out of a static arg.
      def sargToVal(sarg: StaticArg): Node = sarg match {
        case sarg:TypeArg => sarg.getTypeArg
        case sarg:IntArg => sarg.getIntVal
        case sarg:BoolArg => sarg.getBoolArg
        case sarg:OpArg => sarg.getName
        case sarg:DimArg => sarg.getDimArg
        case sarg:UnitArg => sarg.getUnitArg
        case _ => bug("unexpected kind of static arg")
      }

      // Replaces all the params with args in a node.
      object staticReplacer extends Walker {
        override def walk(node: Any): Any = node match {
          case n:VarType => paramMap.get(n.getName).map(sargToVal).getOrElse(n)
          // TODO: Check proper name for OpArgs.
          case n:OpArg => paramMap.get(n.getName.getOriginalName).getOrElse(n)
          case n:IntRef => paramMap.get(n.getName).map(sargToVal).getOrElse(n)
          case n:BoolRef => paramMap.get(n.getName).map(sargToVal).getOrElse(n)
          case n:DimRef => paramMap.get(n.getName).map(sargToVal).getOrElse(n)
          case n:UnitRef => paramMap.get(n.getName).map(sargToVal).getOrElse(n)
          case _ => super.walk(node)
        }
      }

      // Get the replaced type and clear out its static params, if any.
      Some(clearStaticParams(staticReplacer(body).asInstanceOf[Type],
                             sparamsAndSargs.map(_._1)))
  }


  /**
   * Instantiate only the unlifted static parameters with the given static args
   * in the given body type.
   */
  def instantiateStaticParams(sargs: List[StaticArg],
                              body: Type)
                             (implicit analyzer: TypeAnalyzer): Option[Type] = {
    val sparams = getStaticParams(body).filter(!_.isLifted)
    if (sargs.length != sparams.length) return None
    staticInstantiation(sparams zip sargs, body)
  }

  /**
   * Instantiate only the lifted static parameters with the given static args
   * in the given body type.
   */
  def instantiateLiftedStaticParams(sargs: List[StaticArg],
                                    body: Type)
                                   (implicit analyzer: TypeAnalyzer): Option[Type] = {
    val sparams = getStaticParams(body).filter(_.isLifted)
    if (sargs.length != sparams.length) return None
    staticInstantiation(sparams zip sargs, body)
  }

  /**
   * Determines if the kinds of the given static args match those of the static
   * parameters. In the case of type arguments, the type is checked to be a
   * subtype of the corresponding type parameter's bounds.
   */
  def staticArgsMatchStaticParams(sparamsAndSargs: List[(StaticParam, StaticArg)])
                                 (implicit analyzer: TypeAnalyzer): Boolean = {

    // Match a single pair.
    def argMatchesParam(paramAndArg: (StaticParam, StaticArg)): Boolean = {
      val (param, arg) = paramAndArg
      (arg, param.getKind) match {
        case (_:TypeArg, _:KindType) => true
        case (_:IntArg, _:KindInt) => true
        case (_:BoolArg, _:KindBool) => true
        case (_:DimArg, _:KindDim) => true
        case (_:OpArg, _:KindOp) => true
        case (_:UnitArg, _:KindUnit) => true
        case (_:IntArg, _:KindNat) => true
        case (_, _) => false
      }
    }

    // Match every pair.
    sparamsAndSargs.forall(argMatchesParam)
  }

  /** Same as the other overloading but checks two explicit lists. */
  def staticArgsMatchStaticParams(sargs: List[StaticArg],
                                  sparams: List[StaticParam])
                                 (implicit analyzer: TypeAnalyzer): Boolean =
    (sargs.size == sparams.size) && staticArgsMatchStaticParams(sparams zip sargs)

  /** Does the type contain any nested inference variables? */
  def hasInferenceVars(typ: Type): Boolean = {
    // Walker that looks for inf vars.
    object infChecker extends Walker {
      var found = false
      override def walk(node: Any): Any = node match {
        case _:_InferenceVarType => found = true; node
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
        case _:UnknownType => found = true; node
        case _ => super.walk(node)
      }
    }
    unknownChecker(typ); unknownChecker.found
  }

  /**
   * Creates an iterator over the given domain type. If this is a tuple, it
   * first iterates over the plain types; if varargs are present, it then
   * iterates over them indefinitely. If this is any other type, it is the
   * singleton iterator.
   */
  def typeIterator(dom: Type): Iterator[Type] = dom match {
    case STupleType(_, elts, None, _) => elts.elements
    case STupleType(_, elts, Some(varargs), _) =>
      elts.elements ++ new Iterator[Type] {
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
    case _ => List.fromIterator(elts.elements zip typeIterator(dom))
  }

  /**
   * Zip the given iterator of elements with the type iterator for the given
   * domain type, considering void as a single type.
   */
  def zipWithRhsType[T](elts: Iterator[T], dom: Type): Iterator[(T, Type)] =
    elts zip typeIteratorVoid(dom)

  /** Same as the other zipWithRhsType but uses lists. */
  def zipWithRhsType[T](elts: List[T], dom: Type): List[(T, Type)] =
    List.fromIterator(zipWithRhsType(elts.elements, dom))

  /**
   * Determine if there are enough of the given elements to cover all
   * constituent types of the given type.
   */
  def enoughElementsForType[T](elts: List[T], typ: Type): Boolean =
    elts.size == 1 || (typ match {
      case STupleType(_, typs, None, _) => typs.size == elts.size
      case STupleType(_, typs, Some(_), _) => false //typs.size <= elts.size
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
                        context: Option[Type])
                       (implicit analyzer: TypeAnalyzer)
                        : Option[(ArrowType, List[StaticArg])] = {

    // Pull out the unlifted static params.
    val sparams = getStaticParams(fnType).filter(!_.isLifted)

    // Builds a constraint given the arrow with inference variables.
    def makeConstraint(infArrow: ArrowType): ConstraintFormula = {

      // argType <:? dom(infArrow) yields a constraint, C1
      val domainConstraint = checkSubtype(argType, infArrow.getDomain)

      // if context given, C := C1 AND range(infArrow) <:? context
      val rangeConstraint = context.map(t =>
        checkSubtype(infArrow.getRange, t)).getOrElse(CnTrue)
      domainConstraint.and(rangeConstraint, analyzer)
    }

    // Do the inference.
    inferStaticParamsHelper(fnType, sparams, makeConstraint)
  }

  /** Helper that performs the inference. */
  def inferStaticParamsHelper(fnType: ArrowType,
                              sparams: List[StaticParam],
                              constraintMaker: ArrowType => ConstraintFormula)
                             (implicit analyzer: TypeAnalyzer)
                              : Option[(ArrowType, List[StaticArg])] = {

    // Substitute inference variables for static parameters in fnType.

    // 1. build substitution S = [T_i -> $T_i]
    // 2. instantiate fnType with S to get an arrow type with inf vars, infArrow
    val sargs = sparams.map(makeInferenceArg)
    val infArrow = staticInstantiation(sparams zip sargs, fnType).
      getOrElse(return None).asInstanceOf[ArrowType]
    val constraint = constraintMaker(infArrow)

    // Get an inference variable type out of a static arg.
    def staticArgType(sarg: StaticArg): Option[_InferenceVarType] = sarg match {
      case sarg:TypeArg => Some(sarg.getTypeArg.asInstanceOf[_InferenceVarType])
      case _ => None
    }

    // 5. build bounds map B = [$T_i -> S(UB(T_i))]
    val infVars = sargs.flatMap(staticArgType)
    val sparamBounds = sparams.flatMap(staticParamBoundType).
      map(t => staticInstantiation(sparams zip sargs, insertStaticParams(t, sparams)).get)
    val boundsMap = Map(infVars.zip(sparamBounds): _*)

    // 6. solve C to yield a substitution S' = [$T_i -> U_i]
    val subst = constraint.solve(boundsMap).getOrElse(return None)

    // 7. instantiate infArrow with [U_i] to get resultArrow
    val resultArrow =
      analyzer.normalize(substituteTypesForInferenceVars(subst, infArrow)).
               asInstanceOf[ArrowType]

    // 8. return (resultArrow,StaticArgs([U_i]))
    val resultArgs = sargs.map {
      case STypeArg(info, lifted, typ) =>
        NF.makeTypeArg(info.getSpan,
                       substituteTypesForInferenceVars(subst, typ),
                       lifted)
      case sarg => sarg
    }

    Some((resultArrow, resultArgs))
  }

  def inferLiftedStaticParams(fnType: ArrowType,
                              argType: Type)
                             (implicit analyzer: TypeAnalyzer)
                              : Option[(ArrowType, List[StaticArg])] = {

    val sparams = getStaticParams(fnType).filter(_.isLifted)
    if (sparams.isEmpty || fnType.getMethodInfo.isNone) return Some((fnType, Nil))

    // Builds a constraint given the arrow with inference variables.
    def makeConstraint(infArrow: ArrowType): ConstraintFormula = {

      // Get the type of the `self` arg and form selfArg <:? selfType
      val SMethodInfo(selfType, selfPosition) = infArrow.getMethodInfo.unwrap
      getTypeAt(argType, selfPosition) match {
        case Some(selfArgType) => checkSubtype(selfArgType, selfType)
        case None => CnFalse
      }
    }

    // Do the inference.
    inferStaticParamsHelper(fnType, sparams, makeConstraint)
  }


  /**
   * Define an ordering relation on arrows with their instantiations. That is,
   * is candidate1 more specific than candidate2?
   */
  def moreSpecificCandidate(candidate1: AppCandidate,
                            candidate2: AppCandidate)
                           (implicit analyzer: TypeAnalyzer): Boolean = {

    val (SArrowType(_, domain1, range1, _, _, mi1), _, args1) = candidate1
    val (SArrowType(_, domain2, range2, _, _, mi2), _, args2) = candidate2

    // If these are dotted methods, add in the self type as an implicit first
    // parameter. The new domains will be a tuple of the form
    // `(selfType, domainType)`.
    val (newDomain1, newDomain2) = (mi1, mi2) match {
      case (Some(SMethodInfo(selfType1, -1)),
            Some(SMethodInfo(selfType2, -1))) =>
        (STupleType(domain1.getInfo, List(selfType1, domain1), None, Nil),
         STupleType(domain2.getInfo, List(selfType2, domain2), None, Nil))
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
      case _ if analyzer.equivalent(newDomain1, newDomain2).isTrue => false
      case _ => isSubtype(newDomain1, newDomain2)
    }
  }

  /**
   * Determines if the given overloading is dynamically applicable.
   */
  def isDynamicallyApplicable(overloading: Overloading,
                              smaArrow: ArrowType,
                              sargs: List[StaticArg],
                              liftedInfSargs: List[StaticArg])
                             (implicit analyzer: TypeAnalyzer)
                              : Option[Overloading] = {
    val SOverloading(ovInfo, ovName, origName, Some(ovType), schema) = overloading

    // If static args, then instantiate the unlifted static params.
    val typ1 =
      if (!sargs.isEmpty)
        instantiateStaticParams(sargs, ovType).getOrElse(return None)
      else
        ovType

    // If there were lifted, inferred static args, then instantiate those.
    val typ2 =
      if (!liftedInfSargs.isEmpty)
        instantiateLiftedStaticParams(liftedInfSargs, typ1).getOrElse(return None)
      else
        typ1

    // If there are still some static params in it, then we can't infer them
    // so it's not applicable.
    val typ = typ2.asInstanceOf[ArrowType]
    val newOvType =
      if (!hasStaticParams(typ) && isSubtype(typ.getDomain, smaArrow.getDomain))
        typ
      else
        return None

    Some(SOverloading(ovInfo, ovName, origName, Some(newOvType), schema))
  }

  /**
   * Given an applicand, the statically most applicable arrow type for it,
   * and the static args from the application, return the applicand updated
   * with the dynamically applicable overloadings, arrow type, and static args.
   */
  def rewriteApplicand(fn: Expr,
                       arrow: ArrowType,
                       sargs: List[StaticArg])
                      (implicit analyzer: TypeAnalyzer): Expr = fn match {
    case fn: FunctionalRef =>
      // Get the unlifted static args.
      val (liftedSargs, unliftedSargs) = sargs.partition(_.isLifted)

      // Get the dynamically applicable overloadings.
      val overloadings =
        toList(fn.getNewOverloadings).
        flatMap(o => isDynamicallyApplicable(o, arrow, unliftedSargs, liftedSargs))

      // Add in the filtered overloadings, the inferred static args,
      // and the statically most applicable arrow to the fn.
      addType(
        addStaticArgs(
          addOverloadings(fn, overloadings), unliftedSargs), arrow)

    case _ if !sargs.isEmpty =>
      NI.nyi("No place to put inferred static args in application.")

    // Just add the arrow type if the applicand is not a FunctionalRef.
    case _ => addType(fn, arrow)
  }

  // Invariant: Parameter types of all the methods should exist,
  //            either given or inferred.
  def inheritedMethods(extendedTraits: List[TraitTypeWhere],
                       initial: Set[(IdOrOpOrAnonymousName,
                                         (Functional, StaticTypeReplacer))],
                       analyzer: TypeAnalyzer)
                       : Relation[IdOrOpOrAnonymousName,
                                  (Functional, StaticTypeReplacer, TraitType)] = {
    val methods =
        new IndexedRelation[IdOrOpOrAnonymousName,
                            (Functional, StaticTypeReplacer, TraitType)](false)
    val emptySet = Set[Type]()
    // Return all of the methods from super-traits
    def inheritedMethodsHelper(history: HierarchyHistory,
                               extended_traits: List[TraitTypeWhere],
                               given: Map[String, Set[Type]]) : scala.Unit = {
      var allMethods = given
      def addToAllMethods(fname: String, paramTy: Type) = {
          val newSet = allMethods.getOrElse(fname, emptySet) + paramTy
          allMethods = allMethods.update(fname, newSet)
      }

      // a set of inherited methods:
      // a set of pairs of method names and
      //                   triples of Functionals, static parameters substitutions, and declaring trait
      for ( STraitTypeWhere(_, ty: TraitType, _) <- extended_traits;
            if history.explore(ty) ) {
        val STraitType(_, name, trait_args, _) = ty
        toOption(analyzer.traits.typeCons(name)) match {
          case Some(ti : TraitIndex) =>
            val tindex = ti.asInstanceOf[TraitIndex]
            // Instantiate methods with static args
            val paramsToArgs = new StaticTypeReplacer(ti.staticParameters,
                                                      toJavaList(trait_args))
            def oneMethod(methodName: IdOrOp, methodFunc: Functional) = {
              val (fname, paramTy0) = toNameParamTy(methodName, methodFunc)
              val paramTy = paramsToArgs.replaceIn(paramTy0)
              if (!isOverride(fname, paramTy, allMethods, analyzer)) {
                methods.add(methodName, (methodFunc, paramsToArgs, ty))
                addToAllMethods(fname, paramTy)
              }
            }
            def onePair[T <: Functional](t: Pair[IdOrOpOrAnonymousName, T]) =
              t.first match {
                case id : IdOrOp => oneMethod(id, t.second)
                case _ => ()
              }
            def oneMapping(t: JMap.Entry[Id, Method]) = oneMethod(t.getKey, t.getValue)
            ti.dottedMethods.foreach(onePair)
            ti.functionalMethods.foreach(onePair)
            ti.getters.entrySet.foreach(oneMapping)
            ti.setters.entrySet.foreach(oneMapping)
            val instantiated_extends_types =
              toList(ti.extendsTypes).map(_.accept(paramsToArgs)
                                               .asInstanceOf[TraitTypeWhere])
            inheritedMethodsHelper(history, instantiated_extends_types, allMethods)
          case _ =>
        }
      }
      ()
    }
    var initialMap = Map[String, Set[Type]]()
    for ( (id, (fnl, _)) <- initial ) {
        val (fname, ty) = toNameParamTy(id,fnl)
        val newSet = initialMap.getOrElse(fname, emptySet) + ty
        initialMap = initialMap.update(fname, newSet)
    }
    inheritedMethodsHelper(new HierarchyHistory(), extendedTraits, initialMap)
    methods
  }

  def inheritedMethods(extendedTraits: List[TraitTypeWhere],
                       analyzer: TypeAnalyzer)
                       : Relation[IdOrOpOrAnonymousName,
                                  (Functional, StaticTypeReplacer, TraitType)] =
    inheritedMethods(extendedTraits, Set(), analyzer)

  def inheritedMethods(extendedTraits: JList[TraitTypeWhere],
                       analyzer: TypeAnalyzer)
                       : Relation[IdOrOpOrAnonymousName,
                                  (Functional, StaticTypeReplacer, TraitType)] =
    inheritedMethods(toList(extendedTraits), Set(), analyzer)


  private def toNameParamTy(name: IdOrOpOrAnonymousName, func: Functional) = {
    val span = NU.getSpan(name)
    val ty = paramsToType(toList(func.parameters), span) match {
      case Some(t) => t
      case _ => NF.makeVoidType(span)
    }
    (name.asInstanceOf[IdOrOp].getText, ty)
  }

  private def isOverride(fname: String, paramTy: Type,
                         allMethods: Map[String, Set[Type]],
                         analyzer: TypeAnalyzer): Boolean = {
    for (tys <- allMethods.get(fname); t <- tys) {
      if (analyzer.equivalent(paramTy, t).isTrue) return true
    }
    false
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
      case Some(ty) => Some(ty)
      case _ => toOption(param.getVarargsType) match {
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
      toList(NU.getExtendsClause(obj)).map(_.getBaseType)
    if (extends_types.isEmpty) extends_types = List(Types.OBJECT)
    NF.makeObjectExprType(toJavaList(extends_types))
  }

}
