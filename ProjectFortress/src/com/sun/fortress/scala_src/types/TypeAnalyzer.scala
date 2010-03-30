/*******************************************************************************
    Copyright 2010 Sun Microsystems, Inc.,
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

package com.sun.fortress.scala_src.types

import com.sun.fortress.compiler.index._
import com.sun.fortress.compiler.Types.ANY
import com.sun.fortress.compiler.Types.BOTTOM
import com.sun.fortress.compiler.Types.OBJECT
import com.sun.fortress.exceptions.InterpreterBug.bug
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.NodeFactory.typeSpan
import com.sun.fortress.nodes_util.{NodeFactory => NF}
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.typechecker.ConstraintFormula
import com.sun.fortress.scala_src.typechecker.CnFalse
import com.sun.fortress.scala_src.typechecker.CnTrue
import com.sun.fortress.scala_src.typechecker.CnAnd
import com.sun.fortress.scala_src.typechecker.CnOr
import com.sun.fortress.scala_src.typechecker.staticenv.KindEnv
import com.sun.fortress.scala_src.typechecker.TraitTable
import com.sun.fortress.scala_src.types.TypeAnalyzerUtil._
import com.sun.fortress.scala_src.useful.ErrorLog
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Maps._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.Sets._
import com.sun.fortress.scala_src.useful.STypesUtil._
import com.sun.fortress.useful.NI


class TypeAnalyzer(val traits: TraitTable, val env: KindEnv) extends BoundedLattice[Type]{

  def top = ANY
  def bottom = BOTTOM

  def lteq(x: Type, y: Type): Boolean =  subtype(x, y).isTrue
  def meet(x: Type, y: Type): Type = meet(List(x, y))
  def meet(x: Iterable[Type]): Type = normalize(makeIntersectionType(x))
  def join(x: Type, y: Type): Type = meet(List(x, y))
  def join(x: Iterable[Type]): Type = normalize(makeUnionType(x))

  private def removeSelf(x: Type) = {
    object remover extends Walker {
      override def walk(y: Any): Any = y match {
        case t:TraitSelfType =>
          if (t.getComprised.isEmpty) t.getNamed
          else NF.makeIntersectionType(t.getNamed,
                                       NF.makeMaybeUnionType(t.getComprised))
        case t:ObjectExprType => NF.makeIntersectionType(t.getExtended)
        case _ => super.walk(y)
      }
    }
    normalize(remover(x).asInstanceOf[Type])
  }

  def subtype(x: Type, y: Type): ConstraintFormula =
    sub(normalize(x), normalize(y))

  private def sub(x: Type, y: Type): ConstraintFormula = (x, y) match {
    case (s,t) if (s==t) => TRUE
    case (s: BottomType, _) => TRUE
    case (s, t: AnyType) => TRUE
    //Inference variables
    case (s: _InferenceVarType, t: _InferenceVarType) =>
      and(upperBound(s, t), lowerBound(t,s))
    case (s: _InferenceVarType, t) => upperBound(s,t)
    case (s, t: _InferenceVarType) => lowerBound(t,s)
    //Type variables
    case (s@SVarType(_, id, _), t) =>
      val sParam = staticParam(id)
      val supers = toListFromImmutable(sParam.getExtendsClause)
      supers.map(sub(_, t)).foldLeft(FALSE)(or)
    //Trait types
    case (s: TraitType, t: TraitType) if (t==OBJECT) => TRUE
    case (STraitType(_, n1, a1,_), STraitType(_, n2, a2, _)) if (n1==n2) =>
      (a1, a2).zipped.map((a, b) => eq(a, b)).foldLeft(TRUE)(and)
    case (s@STraitType(_, n, a, _) , t: TraitType) =>
      val index = typeCons(n).asInstanceOf[TraitIndex]
      val supers = toListFromImmutable(index.extendsTypes).
      map(tw => substitute(a, toListFromImmutable(index.staticParameters), tw.getBaseType))
      supers.map(sub(_, t)).foldLeft(FALSE)(or)
    case (s: TraitSelfType, t) => sub(removeSelf(s), t)
    case (t, STraitSelfType(_, named, _)) => sub(t,named)
    case (s: ObjectExprType, t) => sub(removeSelf(s), t)
    //Arrow types
    case (SArrowType(_, d1, r1, e1, i1, _), SArrowType(_, d2, r2, e2, i2, _)) =>
      and(and(sub(d2, d1), sub(r1, r2)), sub(e1, e2))
    //Tuple types
    case (s: AnyType, t@STupleType(_, e, Some(v), _)) =>
      sub(s, disjunctFromTuple(t,1))
    case (s: TraitType, t@STupleType(_, e, Some(v), _)) =>
      sub(s, disjunctFromTuple(t,1))
    case (s@STupleType(_, e1, None, _), t@STupleType(_, e2, Some(v), _)) =>
      sub(s, disjunctFromTuple(t, e1.size))
    case (STupleType(_, e1, None, k1), STupleType(_, e2, None, k2))
      if (e1.size == e2.size) =>
        (e1, e2).zipped.map((a, b) => sub(a, b)).foldLeft(sub(k1, k2))(and)
    case (STupleType(_, e1, Some(v1), k1), STupleType(_, e2, Some(v2), k2))
      if (e1.size == e2.size) =>
        (e1, e2).zipped.map((a, b) => sub(a, b)).foldLeft(and(sub(v1, v2), sub(k1, k2)))(and)
    //Intersection types
    case (s, SIntersectionType(_,ts)) =>
      ts.map(sub(s, _)).foldLeft(TRUE)(and)
    case (SIntersectionType(_,ss), t) =>
      ss.map(sub(_, t)).foldLeft(FALSE)(or)
    //Union types
    case (SUnionType(_,ss), t) =>
      ss.map(sub(_, t)).foldLeft(TRUE)(and)
    case (s, SUnionType(_, ts)) =>
      ts.map(sub(s, _)).foldLeft(FALSE)(or)
    //Otherwise
    case _ => FALSE
  }

  private def sub(x: List[KeywordType], y: List[KeywordType]): ConstraintFormula = {
    def toPair(k: KeywordType) = (k.getName, k.getKeywordType)
    val xmap = Map(x.map(toPair):_*)
    val ymap = Map(y.map(toPair):_*)
    def compare(id: Id) = (xmap.get(id), ymap.get(id)) match {
      case (None, _) => TRUE
      case (Some(s), Some(t)) => sub(s, t)
      case _ => FALSE
    }
    xmap.keysIterator.map(compare).foldLeft(TRUE)(and)
  }

  private def sub(x: Effect, y: Effect): ConstraintFormula = {
    val (SEffect(_, tc1, io1), SEffect(_, tc2, io2)) = (x,y)
    if (!io1 || io2)
      sub(makeUnionType(tc1.getOrElse(Nil)), makeUnionType(tc2.getOrElse(Nil)))
    else
      FALSE
  }


  def equivalent(x: Type, y: Type): ConstraintFormula = {
    val s = normalize(x)
    val t = normalize(y)
    eq(s,t)
  }

  private def eq(x: Type, y:Type): ConstraintFormula  = {
    and(sub(x, y), sub(x, y))
  }

  private def eq(x: StaticArg, y: StaticArg): ConstraintFormula = (x,y) match {
    case (STypeArg(_, _, s), STypeArg(_, _, t)) => eq(s, t)
    case (SIntArg(_, _, a), SIntArg(_, _, b)) => fromBoolean(a==b)
    case (SBoolArg(_, _, a), SBoolArg(_, _, b)) => fromBoolean(a==b)
    case (SOpArg(_, _, a), SOpArg(_, _, b)) => fromBoolean(a==b)
    case (SDimArg(_, _, a), SDimArg(_, _, b)) => fromBoolean(a==b)
    case (SUnitArg(_, _, a), SUnitArg(_, _, b)) => fromBoolean(a==b)
    case _ => FALSE
  }


  def excludes(x: Type, y: Type) =
    exc(normalize(removeSelf(x)), normalize(removeSelf(y)))

  /** Determine if a collection of types all exclude each other. */
  def excludes(tsCollection: Iterable[Type]): Boolean = {

    // Cache as array for faster lookups.
    val ts = tsCollection.toArray

    // Check that each symmetric, irreflexive pair excludes.
    for (i <- ts.indices; j <- ts.indices if i < j)
      if (!excludes(ts(i), ts(j)))
        return false
    true
  }

  private def exc(x: Type, y: Type): Boolean = (x, y) match {
    case (s: BottomType, _) => true
    case (_, t: BottomType) => true
    case (s: AnyType, _) => false
    case (_, t: AnyType) => false
    case (s@SVarType(_, id, _), t) =>
      val sParam = staticParam(id)
      val supers = toListFromImmutable(sParam.getExtendsClause)
      supers.exists(exc(_, t))
    case (s, t:VarType) => exc(t, s)
    // ToDo: Make sure that two traits with the same exclude each other
    // if their parameters are definitely different
    case (s@STraitType(_, n1, a1, _), t@STraitType(_, n2, a2, _)) =>
      val sExcludes = excludesClause(s)
      val tExcludes = excludesClause(t)
      if (sExcludes.exists(sub(t, _).isTrue))
        return true
      if (tExcludes.exists(sub(s, _).isTrue))
        return true
      val sIndex = typeCons(n1).asInstanceOf[TraitIndex]
      val tIndex = typeCons(n2).asInstanceOf[TraitIndex]
      (sIndex, tIndex) match {
        case (si: ProperTraitIndex, ti: ProperTraitIndex) =>
          val sComprises = comprisesClause(s)
          val tComprises = comprisesClause(t)
          if (!sComprises.isEmpty && sComprises.forall(exc(t, _)))
            return true
          if (!tComprises.isEmpty && tComprises.forall(exc(s, _)))
            return true
          false
        case _ =>
          or(sub(s, t), sub(t, s)).isFalse
      }
    case (s: ArrowType, t: ArrowType) => false
    case (s: ArrowType, _) => true
    case (_, t: ArrowType) => true
    // ToDo: Handle keywords
    case (STupleType(_, e1, mv1, _), STupleType(_, e2, mv2, _)) =>
      val excludes = (e1, e2).zipped.exists((a, b) => exc(a, b))
      val different = (mv1, mv2) match {
        case (Some(v1), _) if (e1.size < e2.size) =>
          e2.drop(e1.size).exists(exc(_, v1))
        case (_, Some(v2)) if (e1.size > e2.size) =>
          e1.drop(e2.size).exists(exc(_, v2))
        case _ if (e1.size!=e2.size) => true
        case _ => false
      }
      different || excludes
    case (s: TupleType, _) => true
    case (_, t: TupleType) => true
    case (s@SIntersectionType(_, elts), t) =>
      elts.exists(exc(_, t))
    case (s, t: IntersectionType) => exc(t, s)
    case (s@SUnionType(_, elts), t) =>
      elts.forall(exc(_, t))
    case (s, t: UnionType) => exc(t, s)
    case _ => false
  }

  def normalize(x: Type): Type = {
    object normalizer extends Walker {
      override def walk(y: Any): Any = y match {
        case t@STraitType(_, n, a, _) =>
          val index = typeCons(n)
          index match {
            case ti: TypeAliasIndex =>
              val params = toListFromImmutable(ti.staticParameters)
              walk(substitute(a, params, ti.ast.getTypeDef))
            case _ => super.walk(t)
          }
        //ToDo: Handle keywords
        //ToDo: Make sure that a tuple of bottom is bottom
        case t:TupleType => super.walk(t) match {
          case STupleType(i, e, Some(v: BottomType), k) => STupleType(i, e, None, k)
          case _ => t
        }
        case u@SUnionType(_, e) =>
          val ps = e.flatMap(y => disjuncts(walk(y).asInstanceOf[Type]))
          makeUnionType(normDisjunct(ps))
        case i@SIntersectionType(info, e) =>
          val sop = cross(e.map(y => disjuncts(walk(y).asInstanceOf[Type])))
          val ps = sop.map(y => makeIntersectionType(normConjunct(y.flatMap(disjuncts))))
          makeUnionType(normDisjunct(ps))
        case _ => super.walk(y)
      }
    }
    normalizer(x).asInstanceOf[Type]
  }

  private def normConjunct(x: Iterable[Type]): List[Type] = {
    if(x.exists(y => x.exists(z => exc(y, z))))
      List(BOTTOM)
    else {
      val ds = x.foldLeft(List[Type]())((l, a) => {
        val l2 = l.filter(!sub(a,_).isTrue)
        l2 ++ (if (l2.exists(sub(_, a).isTrue)) Nil else List(a))
      })
      val (ts, ss) = List.separate(ds.map(_ match {
        case t:TupleType => Left(t)
        case s => Right(s)
      }))
      ss ++ normTuples(ts)
    }
  }

  private def normTuples(ts: List[TupleType]): List[TupleType] = ts match {
    case Nil => Nil
    case _ => List(ts.reduceLeft(normTuples))
  }

  //ToDo: Keywords
  private def normTuples(x: TupleType, y: TupleType): TupleType = (x,y) match {
    case (STupleType(_, e1, None, _), STupleType(_, e2, None, _)) =>
      STupleType(makeInfo(e1), (e1, e2).zipped.map(meet), None, Nil)
    case (STupleType(_, e1, None, _), STupleType(_, e2, Some(_), _)) =>
      normTuples(x, disjunctFromTuple(y, e1.size).asInstanceOf[TupleType])
    case (STupleType(_, e1, Some(_), _), STupleType(_, e2, None, _)) =>
      normTuples(disjunctFromTuple(x, e2.size).asInstanceOf[TupleType], y)
    case (STupleType(_, e1, Some(v1), _), STupleType(_, e2, Some(v2), _)) => {
      val ee1 = e1 ++ List.fill(e2.size - e1.size){v1}
      val ee2 = e2 ++ List.fill(e1.size - e2.size){v2}
      STupleType(makeInfo(e1), (ee1, ee2).zipped.map(meet), Some(meet(v1, v2)), Nil)
    }
  }

  private def normDisjunct(x: Iterable[Type]): List[Type] = {
      x.foldLeft(List[Type]())((l, a) => {
        val l2 = l.filter(!sub(_,a).isTrue)
        l2 ++ (if (l2.exists(sub(a,_).isTrue)) Nil else List(a))
      })
  }

  private def cross[T](x: Iterable[Iterable[T]]): Iterable[Iterable[T]] = {
    x.foldLeft(List(List[T]()))((l, a) => l.flatMap(b => a.map(b ++ List(_))))
  }

  def coveringEquivalent(x: Type, y: Type) = (lteq(minimalCovering(y), x) && lteq(minimalCovering(x), y))

  def minimalCovering(x: Type): Type = normalize(x) match {
    case SIntersectionType(_, e) =>
      val (as, ts) = List.separate(e.map(_ match {
        case a:ArrowType => Left(a)
        case t => Right(minimalCovering(t))
      }))
      meet(minimalArrows(as) ++ ts)
    case SUnionType(_, e) => join(e.map(minimalCovering))
    case t:TraitType => join(comprisesLeaves(t))
    //ToDo: Handle keywords
    case STupleType(i, e, mv, _) => STupleType(i, e.map(minimalCovering), mv.map(minimalCovering), Nil)
    case SArrowType(i, d, r, e, io, m) =>
      // ToDo: Go into domain but flip to only decrease contravariant positions
      SArrowType(i, d, minimalCovering(r), e, io, m)
    case _ => x
  }

  private def minimalArrows(x: ArrowType, y: ArrowType): ArrowType = {
    val SArrowType(i1, d1, r1, e1, io1, mi1) = x
    val SArrowType(i2, d2, r2, e2, io2, mi2) = y
    //merge methodInfo?
    SArrowType(minimalTypeInfo(i1, i2),
               join(d1, d2),
               minimalCovering(meet(r1, r2)),
               minimalEffect(e1, e2),
               io1 || io2,
               mi1)
  }

  private def minimalArrows(x: List[ArrowType]): List[ArrowType] = x match {
    case Nil => Nil
    case _ => List(x.reduceLeft(minimalArrows))
  }

  private def minimalTypeInfo(x: TypeInfo, y: TypeInfo) = x

  private def minimalEffect(x: Effect, y: Effect) = {
    val SEffect(i1, t1, io1) = x
    val SEffect(i2, t2, io2) = y
    val tc = minimalCovering(meet(join(t1.getOrElse(Nil)), join(t2.getOrElse(Nil)))) match {
      case t:BottomType => None
      case SUnionType(_, elts) => Some(elts)
      case t => Some(List(t))
    }
    //merge ASTNodeInfo?
    SEffect(i1, tc, io1 || io2)
  }

  private def comprisesLeaves(x: TraitType): Set[TraitType] = comprisesClause(x) match {
    case ts if ts.isEmpty => Set(x)
    case ts => ts.flatMap(comprisesLeaves)
  }

  // Accessor methods for trait table
  def typeCons(x: Id): TypeConsIndex =
    toOption(traits.typeCons(x)).getOrElse(bug(x, x + " is not in the trait table"))

    def comprisesClause(t: TraitType): Set[TraitType] = typeCons(t.getName) match {
    case ti: ProperTraitIndex =>
      val args = toListFromImmutable(t.getArgs)
      val params = toListFromImmutable(ti.staticParameters)
      toSet(ti.comprisesTypes).map(nt => if (!nt.isInstanceOf[TraitType]) return Set()
                                         else substitute(args, params, nt).asInstanceOf[TraitType])
    case _ => Set()
  }

  def excludesClause(t: TraitType): Set[TraitType] = {
    val ti = typeCons(t.getName).asInstanceOf[TraitIndex]
    val args = toListFromImmutable(t.getArgs)
    val params = toListFromImmutable(ti.staticParameters)
    val excludes = ti match{
      case ti : ProperTraitIndex =>
      toSet(ti.excludesTypes).map(substitute(args, params, _).asInstanceOf[TraitType])
      case _ => Set[TraitType]()
    }
    val supers = toListFromImmutable(ti.extendsTypes).map(tw => substitute(args, params, tw.getBaseType))
    val transitively = supers.flatMap{
      case s: TraitType => excludesClause(s)
      case _ => Set[TraitType]()
    }
    excludes ++ transitively
  }

  // Accessor method for kind env
  def staticParam(x: Id): StaticParam =
    env.staticParam(x).getOrElse(bug(x, x + " is not in the kind env."))

  def extend(params: List[StaticParam], where: Option[WhereClause]) =
    new TypeAnalyzer(traits, env.extend(params, where))

  private val TRUE: ConstraintFormula = CnTrue
  private val FALSE: ConstraintFormula = CnFalse
  private def and(x: ConstraintFormula, y: ConstraintFormula): ConstraintFormula =
    x.and(y, this)
  private def or(x: ConstraintFormula, y: ConstraintFormula): ConstraintFormula =
    x.or(y, this)
  private def upperBound(i: _InferenceVarType, t: Type): ConstraintFormula =
    CnAnd(Map((i,t)), Map(), this)
  private def lowerBound(i: _InferenceVarType, t: Type): ConstraintFormula =
    CnAnd(Map(), Map((i,t)), this)
  private def fromBoolean(x: Boolean) = if (x) TRUE else FALSE

}

object TypeAnalyzer {
  def make(traits: TraitTable) = new TypeAnalyzer(traits, KindEnv.makeFresh)
}
