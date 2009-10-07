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
import com.sun.fortress.scala_src.typechecker.staticenv.KindEnv
import com.sun.fortress.scala_src.typechecker.TraitTable
import com.sun.fortress.scala_src.useful.ErrorLog
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Maps._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.STypesUtil._
import com.sun.fortress.useful.NI


class TypeLattice(traits: TraitTable, env: KindEnv) extends BoundedLattice[Type]{

  def top = ANY
  def bottom = BOTTOM

  def lteq(x: Type, y: Type): Boolean =  subtype(x, y).isTrue
  def meet(x: Type, y: Type): Type = x
  def join(x: Type, y: Type): Type = y
  
  def subtype(x: Type, y: Type): ScalaConstraint = sub(normalize(x), normalize(y))

  def equivalent(x: Type, y: Type): ScalaConstraint = {
    val s = normalize(x)
    val t = normalize(y)
    eq(s,t)
  }
  
  def excludes(x: Type, y: Type) = false
  
  def normalize(x: Type): Type = x
  
  private def eq(x: Type, y:Type): ScalaConstraint  = {
    and(sub(x, y), sub(x, y))
  }
  
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
      val sParam = staticParam(id).getOrElse(bug(s, s + " is not in the kind environment."))
      val supers = toList(sParam.getExtendsClause)
      supers.map(sub(_, t)).foldLeft(FALSE)(or)
    //Trait types
    case (STraitType(_, n1, a1,_), STraitType(_, n2, a2, _)) if (n1==n2) =>
      List.map2(a1, a2)((a, b) => eq(a, b)).foldLeft(TRUE)(and)
    case (s@STraitType(_, n, a, _) , t: TraitType) =>
      val index = typeCons(n).getOrElse(bug(s, s + "is not in the trait table.")).asInstanceOf[TraitIndex]
      val supers = toList(index.extendsTypes).
        map(tw => substitute(a, toList(index.staticParameters), tw.getBaseType))
      val base = if (s!=OBJECT) sub(OBJECT, t) else FALSE
      supers.map(sub(_, t)).foldLeft(base)(or)
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
    case (STupleType(_, e1, None, k1), STupleType(_, e2, None, k2)) if (e1.size == e2.size) =>
      List.map2(e1, e2)((a, b) => sub(a, b)).foldLeft(sub(k1, k2))(and)
    case (STupleType(_, e1, Some(v1), k1), STupleType(_, e2, Some(v2), k2)) if (e1.size == e2.size) =>
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
  
  private def eq(x: StaticArg, y: StaticArg): ScalaConstraint = (x,y) match {
    case (STypeArg(_, _, s), STypeArg(_, _, t)) => eq(s, t)
    case (SIntArg(_, _, a), SIntArg(_, _, b)) => fromBoolean(a==b)
    case (SBoolArg(_, _, a), SBoolArg(_, _, b)) => fromBoolean(a==b)
    case (SOpArg(_, _, a), SOpArg(_, _, b)) => fromBoolean(a==b)
    case (SDimArg(_, _, a), SDimArg(_, _, b)) => fromBoolean(a==b)
    case (SUnitArg(_, _, a), SUnitArg(_, _, b)) => fromBoolean(a==b)
    case _ => FALSE
  }
  
  private def exc(x: Type, y: Type): Boolean = (x,y) match {
    case (s: BottomType, _) => true
    case (_, t: BottomType) => true
    case (s: AnyType, _) => false
    case (_, t: AnyType) => false
    case (s@SVarType(_, id, _), t) =>
      val sParam = staticParam(id).getOrElse(bug(s,s + "is not in the kind env"))
      val supers = toList(sParam.getExtendsClause)
      supers.exists(exc(_, t))
    case (s, t:VarType) => exc(t, s)
    case (s: TraitType, t: TraitType) => true
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
  
  // Accessor method for trait table
  private def typeCons(x: Id): Option[TypeConsIndex] = toOption(traits.typeCons(x))
  
  //Accessor method for kind env
  private def staticParam(x: Id): Option[StaticParam] = env.staticParam(x)
  
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
      def walk(node: Node) = node match {
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
  
  //Constraint Utilities
  private val TRUE: ScalaConstraint = CnTrue
  private val FALSE: ScalaConstraint = CnFalse
  private def and(x: ScalaConstraint, y: ScalaConstraint): ScalaConstraint = TRUE
  private def or(x: ScalaConstraint, y: ScalaConstraint): ScalaConstraint = TRUE
  private def upperBound(i: _InferenceVarType, t: Type): ScalaConstraint = TRUE
  private def lowerBound(i: _InferenceVarType, t: Type): ScalaConstraint = TRUE
  private def fromBoolean(x: Boolean) = if (x) TRUE else FALSE
}