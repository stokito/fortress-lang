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

package com.sun.fortress.scala_src.types

import com.sun.fortress.compiler.index._
import com.sun.fortress.compiler.Types.ANY
import com.sun.fortress.compiler.Types.BOTTOM
import com.sun.fortress.compiler.Types.OBJECT
import com.sun.fortress.exceptions.InterpreterBug.bug
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.NodeFactory._
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.typechecker.ScalaConstraint
import com.sun.fortress.scala_src.typechecker.CnFalse
import com.sun.fortress.scala_src.typechecker.CnTrue
import com.sun.fortress.scala_src.typechecker.CnAnd
import com.sun.fortress.scala_src.typechecker.CnOr
import com.sun.fortress.scala_src.typechecker.staticenv.KindEnv
import com.sun.fortress.scala_src.typechecker.TraitTable
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
  def meet(x: Iterable[Type]): Type = normalize(makeIntersectionType(x.toList))
  def join(x: Type, y: Type): Type = meet(List(x, y))
  def join(x: Iterable[Type]): Type = normalize(makeUnionType(x.toList))

  def subtype(x: Type, y: Type): ScalaConstraint = sub(normalize(x), normalize(y))

  private def sub(x: Type, y: Type): ScalaConstraint = (x,y) match {
    case (s: BottomType, _) => TRUE
    case (s, t: AnyType) => TRUE
    //Inference variables
    case (s: _InferenceVarType, t: _InferenceVarType) =>
      and(upperBound(s, t), lowerBound(t,s))
    case (s: _InferenceVarType, t) => upperBound(s,t)
    case (s, t: _InferenceVarType) => lowerBound(t,s)
    //Type variables
    case (s: VarType, t: VarType) if (s==t) => TRUE
    case (s@SVarType(_, id, _), t) =>
      val sParam = staticParam(id)
      val supers = toList(sParam.getExtendsClause)
      supers.map(sub(_, t)).foldLeft(FALSE)(or)
    //Trait types
    case (s: TraitType, t: TraitType) if (t==OBJECT) => TRUE
    case (STraitType(_, n1, a1,_), STraitType(_, n2, a2, _)) if (n1==n2) =>
      List.map2(a1, a2)((a, b) => eq(a, b)).foldLeft(TRUE)(and)
    case (s@STraitType(_, n, a, _) , t: TraitType) =>
      val index = typeCons(n).asInstanceOf[TraitIndex]
      val supers = toList(index.extendsTypes).
	map(tw => substitute(a, toList(index.staticParameters), tw.getBaseType))
      supers.map(sub(_, t)).foldLeft(FALSE)(or)
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
	List.map2(e1, e2)((a, b) => sub(a, b)).foldLeft(sub(k1, k2))(and)
    case (STupleType(_, e1, Some(v1), k1), STupleType(_, e2, Some(v2), k2))
      if (e1.size == e2.size) =>
	List.map2(e1, e2)((a, b) => sub(a, b)).foldLeft(and(sub(v1, v2), sub(k1, k2)))(and)
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

  private def sub(x: List[KeywordType], y: List[KeywordType]): ScalaConstraint = {
    def toPair(k: KeywordType) = (k.getName, k.getKeywordType)
    val xmap = Map(x.map(toPair):_*)
    val ymap = Map(y.map(toPair):_*)
    def compare(id: Id) = (xmap.get(id), ymap.get(id)) match {
      case (None, _) => TRUE
      case (Some(s), Some(t)) => sub(s, t)
      case _ => FALSE
    }
    xmap.keys.map(compare).foldLeft(TRUE)(and)
  }

  private def sub(x: Effect, y: Effect): ScalaConstraint = {
    val (SEffect(_, tc1, io1), SEffect(_, tc2, io2)) = (x,y)
    if (!io1 || io2)
      sub(makeUnionType(tc1.getOrElse(Nil)), makeUnionType(tc2.getOrElse(Nil)))
    else
      FALSE
  }


  def equivalent(x: Type, y: Type): ScalaConstraint = {
    val s = normalize(x)
    val t = normalize(y)
    eq(s,t)
  }

  private def eq(x: Type, y:Type): ScalaConstraint  = {
    and(sub(x, y), sub(x, y))
  }

  private def eq(x: StaticArg, y: StaticArg): ScalaConstraint = (x,y) match {
    case (STypeArg(_, _, s), STypeArg(_, _, t)) => eq(s, t)
    case (SIntArg(_, _, a), SIntArg(_, _, b)) => fromBoolean(a==b)
    case (SBoolArg(_, _, a), SBoolArg(_, _, b)) => fromBoolean(a==b)
    case (SOpArg(_, _, a), SOpArg(_, _, b)) => fromBoolean(a==b)
    case (SDimArg(_, _, a), SDimArg(_, _, b)) => fromBoolean(a==b)
    case (SUnitArg(_, _, a), SUnitArg(_, _, b)) => fromBoolean(a==b)
    case _ => FALSE
  }


  def excludes(x: Type, y: Type) = exc(normalize(x), normalize(y))

  private def exc(x: Type, y: Type): Boolean = (x,y) match {
    case (s: BottomType, _) => true
    case (_, t: BottomType) => true
    case (s: AnyType, _) => false
    case (_, t: AnyType) => false
    case (s@SVarType(_, id, _), t) =>
      val sParam = staticParam(id)
      val supers = toList(sParam.getExtendsClause)
      supers.exists(exc(_, t))
    case (s, t:VarType) => exc(t, s)
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
    //Arrow types
    case (s: ArrowType, t: ArrowType) => false
    case (s: ArrowType, _) => true
    case (_, t: ArrowType) => true
    // Tuple types
    case (STupleType(_, e1, mv1, _), STupleType(_, e2, mv2, _)) =>
      val excludes = List.exists2(e1, e2)((a, b) => exc(a, b))
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
    // Intersection types
    case (s@SIntersectionType(_, elts), t) =>
      elts.exists(exc(_, t))
    case (s, t: IntersectionType) => exc(t, s)
    // Union types
    case (s@SUnionType(_, elts), t) =>
      elts.forall(exc(_, t))
    case (s, t: UnionType) => exc(t, s)
    case _ => false
  }

  private def comprisesClause(t: TraitType): Set[TraitType] = {
    val ti = typeCons(t.getName).asInstanceOf[ProperTraitIndex]
    val args = toList(t.getArgs)
    val params = toList(ti.staticParameters)
    toSet(ti.comprisesTypes).map(substitute(args, params, _).asInstanceOf[TraitType])
  }

  private def excludesClause(t: TraitType): Set[TraitType] = {
    val ti = typeCons(t.getName).asInstanceOf[TraitIndex]
    val args = toList(t.getArgs)
    val params = toList(ti.staticParameters)
    val excludes = ti match{
      case ti : ProperTraitIndex =>
	toSet(ti.excludesTypes).map(substitute(args, params, _).asInstanceOf[TraitType])
      case _ => Set[TraitType]()
    }
    val supers = toList(ti.extendsTypes).map(tw => substitute(args, params, tw.getBaseType))
    val transitively = supers.flatMap{
      case s: TraitType => excludesClause(s)
      case _ => Set[TraitType]()
    }
    excludes ++ transitively
  }

  def normalize(x: Type): Type = {
    object normalizer extends Walker {
      override def walk(y: Any): Any = y match {
	case t@STraitType(_, n, a, _) =>
	  val index = typeCons(n)
	  index match {
	    case ti: TypeAliasIndex =>
	      val params = toList(ti.staticParameters)
	      walk(substitute(a, params, ti.ast.getTypeDef))
	    case _ => super.walk(t)
	  }
	// ToDo: Keywords
	case t@STupleType(_, e, None, _) => t
	case a: ArrowType => a
	case e: Effect => e
	case u@SUnionType(_, e) =>
	  val ps = e.flatMap(y => disjuncts(normalize(y)))
	  makeUnionType(reduceSum(ps))
	case i@SIntersectionType(_, e) =>
	  val sop = cross(e.map(y => disjuncts(normalize(y))))
	  val ps = sop.map(y => makeIntersectionType(reduceProduct(y.flatMap(disjuncts))))
	  makeUnionType(reduceSum(ps))
	case _ => super.walk(y)
      }
    }
    normalizer(x).asInstanceOf[Type]
  }

  private def reduceProduct(x: Iterable[Type]): List[Type] = {
    if(x.exists(y => x.exists(z => exc(y, z))))
      List(BOTTOM)
    else {
      x.foldLeft(List[Type]())((l, a) => {
	val l2 = l.filter(!sub(a,_).isTrue)
	l2 ++ (if (l2.exists(sub(_, a).isTrue)) Nil else List(a))
      })
    }
  }
  private def reduceSum(x: Iterable[Type]): List[Type] = {
      x.foldLeft(List[Type]())((l, a) => {
	val l2 = l.filter(!sub(_,a).isTrue)
	l2 ++ (if (l2.exists(sub(a,_).isTrue)) Nil else List(a))
      })
  }

  private def cross[T](x: Iterable[Iterable[T]]): Iterable[Iterable[T]] = {
    x.foldLeft(List(List[T]()))((l, a) => l.flatMap(b => a.map(b ++ List(_))))
  }

  // Accessor method for trait table
  private def typeCons(x: Id): TypeConsIndex =
    toOption(traits.typeCons(x)).getOrElse(bug(x, x + " is not in the trait table"))

  // Accessor method for kind env
  private def staticParam(x: Id): StaticParam =
    env.staticParam(x).getOrElse(bug(x, x + " is not in the kind env."))

  //Type utilites
  private def substitute(args: List[StaticArg], params: List[StaticParam], typ: Type): Type = {

    def getVal(x: StaticArg): Node = x match {
      case STypeArg(_, _, v) => v
      case SIntArg(_, _, v) => v
      case SBoolArg(_, _, v) => v
      case SOpArg(_, _, v) => v
      case SDimArg(_, _, v) => v
      case SUnitArg(_, _, v) => v
    }
    val subst = Map(List.map2(params, args)
		    ((p, a) => (p.getName, a)):_*)

    object replacer extends Walker {
      override def walk(node: Any) = node match {
	case n:VarType => subst.get(n.getName).map(getVal).getOrElse(n)
	case n:OpArg => subst.get(n.getName.getOriginalName).getOrElse(n)
	case n:IntRef => subst.get(n.getName).map(getVal).getOrElse(n)
	case n:BoolRef => subst.get(n.getName).map(getVal).getOrElse(n)
	case n:DimRef => subst.get(n.getName).map(getVal).getOrElse(n)
	case n:UnitRef => subst.get(n.getName).map(getVal).getOrElse(n)
	case _ => super.walk(node)
      }
    }

    replacer(typ).asInstanceOf[Type]
  }

  /**
   * A tuple with var args is equivalent to an infinite union of tuples.
   * (A,B ...) = BOTTOM UNION A UNION (A,B) UNION (A,B,B) ...
   * This method gets the ith disjunct
   */
  private def disjunctFromTuple(t: TupleType, i: Int): Type = t match {
    case STupleType(_, elts, Some(varg), keys) if (i >= elts.size) =>
      makeTupleType(elts ++ List.make(i-elts.size, varg), keys)
    case STupleType(_, elts, _, _) if (i == elts.size)=> t
    case _ => BOTTOM
  }

  private def makeTupleType(types: List[Type]): Type = makeTupleType(types, Nil)

  private def makeTupleType(types: List[Type], keys: List[KeywordType]): Type = types match {
    case t::Nil if (keys.isEmpty) => t
    case _ => STupleType(makeInfo(types), types, None, keys)
  }

  private def makeIntersectionType(types: List[Type]) = types match {
    case Nil => ANY
    case t::Nil => t
    case _ => SIntersectionType(makeInfo(types), types)
  }

  private def makeUnionType(types: List[Type]) = types match {
    case Nil => BOTTOM
    case t::Nil => t
    case _ => SUnionType(makeInfo(types), types)
  }

  //ToDo: Make a better span
  private def makeInfo(types: List[Type]): TypeInfo = {
    STypeInfo(typeSpan, false ,Nil, None)
  }

  //Todo: Constraint Utilities
  private val TRUE: ScalaConstraint = CnTrue
  private val FALSE: ScalaConstraint = CnFalse
  private def and(x: ScalaConstraint, y: ScalaConstraint): ScalaConstraint =
    x.scalaAnd(y, this.lteq)
  private def or(x: ScalaConstraint, y: ScalaConstraint): ScalaConstraint =
    x.scalaOr(y, this.lteq)
  private def upperBound(i: _InferenceVarType, t: Type): ScalaConstraint =
    CnAnd(Map((i,t)), Map(), this.lteq)
  private def lowerBound(i: _InferenceVarType, t: Type): ScalaConstraint =
    CnAnd(Map(), Map((i,t)), this.lteq)
  private def fromBoolean(x: Boolean) = if (x) TRUE else FALSE

  def extend(params: List[StaticParam], where: Option[WhereClause])
    = new TypeAnalyzer(traits, env.extend(params, where))

}

object TypeAnalyzer {
  def make(traits: TraitTable) = new TypeAnalyzer(traits, KindEnv.makeFresh)

}
