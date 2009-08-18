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
import com.sun.fortress.compiler.GlobalEnvironment
import com.sun.fortress.compiler.Types
import com.sun.fortress.compiler.index._
import com.sun.fortress.compiler.typechecker.StaticTypeReplacer
import com.sun.fortress.compiler.typechecker.TypeAnalyzer
import com.sun.fortress.compiler.typechecker.TypesUtil
import com.sun.fortress.exceptions.InterpreterBug.bug
import com.sun.fortress.exceptions.TypeError
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.ExprFactory
import com.sun.fortress.nodes_util.{NodeFactory => NF}
import com.sun.fortress.nodes_util.{NodeUtil => NU}
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.typechecker.ScalaConstraint
import com.sun.fortress.scala_src.typechecker.ScalaConstraintUtil.TRUE_FORMULA
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.Sets._
import com.sun.fortress.scala_src.useful.SExprUtil._
import com.sun.fortress.useful.HasAt
import com.sun.fortress.useful.NI


object STypesUtil {
  
  /** A function that when applied yields an option type. */
  type TypeThunk = Function0[Option[Type]]

  /** A function type that takes two types and returns a boolean. */
  type Subtype = (Type, Type) => Boolean
  
  /**
   * Return the arrow type of the given Functional index.
   */  
  def makeArrowFromFunctional(f: Functional): Option[ArrowType] = {
    val returnType = toOption(f.getReturnType).getOrElse(return None)
    val params = toList(f.parameters).map(NU.getParamType)
    val argType = makeArgumentType(params)
    val sparams = f.staticParameters
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
                          argType,
                          returnType,
                          effect,
                          sparams,
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
    val subst = analyzer.subtype(expectedDomain, paramsDomain).
                asInstanceOf[ScalaConstraint].scalaSolve(Map())
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
    case SKindType(_) => {
      // Create a new inference var type.
      val t = NF.make_InferenceVarType(NU.getSpan(sparam))
      NF.makeTypeArg(NF.makeSpan(t), t, sparam.isLifted)
    }
    case SKindInt(_) => NI.nyi()
    case SKindBool(_) => NI.nyi()
    case SKindDim(_) => NI.nyi()
    case SKindOp(_) => NI.nyi()
    case SKindUnit(_) => NI.nyi()
    case SKindNat(_) => NI.nyi()
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
                  (implicit analyzer: TypeAnalyzer): ScalaConstraint = {
    val constraint = analyzer.subtype(subtype, supertype)
    if (!constraint.isInstanceOf[ScalaConstraint]) {
      bug("Not a ScalaConstraint.")
    }
    constraint.asInstanceOf[ScalaConstraint]
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
        case (STypeArg(_, _, argType), _:KindType) =>
          toList(param.getExtendsClause).
            forall(!analyzer.subtype(argType, _).isFalse)
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
   * Zip the given iterator of elements with the type iterator for the given
   * domain type.
   */
  def zipWithDomain[T](elts: Iterator[T], dom: Type): Iterator[(T, Type)] =
    elts zip typeIterator(dom)

  /** Same as the other zipWithDomain but uses lists. */
  def zipWithDomain[T](elts: List[T], dom: Type): List[(T, Type)] =
    List.fromIterator(zipWithDomain(elts.elements, dom))

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
    def makeConstraint(infArrow: ArrowType): ScalaConstraint = {
      // argType <:? dom(infArrow) yields a constraint, C1
      val domainConstraint = checkSubtype(argType, infArrow.getDomain)

      // if context given, C := C1 AND range(infArrow) <:? context
      val rangeConstraint = context.map(t =>
        checkSubtype(infArrow.getRange, t)).getOrElse(TRUE_FORMULA)
      domainConstraint.scalaAnd(rangeConstraint, isSubtype)
    }

    // Do the inference.
    inferStaticParamsHelper(fnType, sparams, makeConstraint)
  }

  /** Helper that performs the inference. */
  def inferStaticParamsHelper(fnType: ArrowType,
                              sparams: List[StaticParam],
                              constraintMaker: ArrowType => ScalaConstraint)
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
                               map(t => insertStaticParams(t, sparams))
    val boundsMap = Map(infVars.zip(sparamBounds): _*)

    // 6. solve C to yield a substitution S' = [$T_i -> U_i]
    val subst = constraint.scalaSolve(boundsMap).getOrElse(return None)

    // 7. instantiate infArrow with [U_i] to get resultArrow
    val resultArrow =
      analyzer.normalize(substituteTypesForInferenceVars(subst, infArrow)).
               asInstanceOf[ArrowType]

    // 8. return (resultArrow,StaticArgs([U_i]))
    val resultArgs = sargs.map {
      case STypeArg(info, lifted, typ) =>
        NF.makeTypeArg(info.getSpan, substituteTypesForInferenceVars(subst, typ))
      case sarg => sarg
    }

    Some((resultArrow, resultArgs))
  }

  def inferLiftedStaticParams(fnType: ArrowType,
                              argType: Type)
                             (implicit analyzer: TypeAnalyzer)
                              : Option[ArrowType] = {

    val sparams = getStaticParams(fnType).filter(_.isLifted)
    if (sparams.isEmpty || fnType.getMethodInfo.isNone) return Some(fnType)

    // Get the type of the `self` arg and form selfArg <:? selfType
    val SMethodInfo(selfType, selfPosition) = fnType.getMethodInfo.unwrap
    val selfArgType = getTypeAt(argType, selfPosition).getOrElse(return None)
    val constraint = checkSubtype(selfArgType, selfType)

    // Do the inference.
    inferStaticParamsHelper(fnType, sparams, _ => constraint).map(_._1)
  }

  def typeInference(fnType: ArrowType,
                        argType: Type,
                        context: Option[Type])
                       (implicit analyzer: TypeAnalyzer)
                        : Option[(ArrowType, List[StaticArg])] = {

    inferLiftedStaticParams(fnType, argType).flatMap(arrow =>
      inferStaticParams(arrow, argType, context))
  }
}
