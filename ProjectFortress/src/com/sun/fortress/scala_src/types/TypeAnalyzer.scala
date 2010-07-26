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
import com.sun.fortress.nodes_util.{NodeUtil => NU}
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.typechecker._
import com.sun.fortress.scala_src.typechecker.Formula._
import com.sun.fortress.scala_src.typechecker.staticenv.KindEnv
import com.sun.fortress.scala_src.typechecker.TraitTable
import com.sun.fortress.scala_src.types.TypeAnalyzerUtil._
import com.sun.fortress.scala_src.useful.ErrorLog
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Maps._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.Pairs
import com.sun.fortress.scala_src.useful.Sets._
import com.sun.fortress.scala_src.useful.STypesUtil._
import com.sun.fortress.useful.NI

class TypeAnalyzer(val traits: TraitTable, val env: KindEnv) extends BoundedLattice[Type]{
  
  implicit val ta: TypeAnalyzer = this
  
  def top = ANY
  def bottom = BOTTOM

  def lteq(x: Type, y: Type): Boolean =  isTrue(subtype(x, y))
  def meet(x: Type, y: Type): Type = meet(List(x, y))
  def meet(x: Iterable[Type]): Type = normalize(makeIntersectionType(x))
  def join(x: Type, y: Type): Type = meet(List(x, y))
  def join(x: Iterable[Type]): Type = normalize(makeUnionType(x))

  def subtype(x: Type, y: Type): CFormula =
    sub(normalize(x), normalize(y))
  
  def notSubtype(x: Type, y: Type): CFormula = 
    nsub(normalize(x), normalize(y))
    
  protected def sub(x: Type, y: Type): CFormula = (x, y) match {
    case (s,t) if (s==t) => True
    case (s: BottomType, _) => True
    case (s, t: AnyType) => True
    // Intersection types
    case (s, SIntersectionType(_,ts)) =>
      mapAnd(ts)(sub(s, _))
    case (SIntersectionType(_,ss), t) =>
      or(anyExclude(ss), mapOr(ss)(sub(_, t)))
    // Union types
    case (SUnionType(_,ss), t) =>
     mapAnd(ss)(sub(_, t))
    case (s, SUnionType(_, ts)) =>
      mapOr(ts)(sub(s, _))
    // Inference variables
    case (s: _InferenceVarType, t: _InferenceVarType) =>
      and(upperBound(s, t), lowerBound(t,s))
    case (s: _InferenceVarType, t) => upperBound(s,t)
    case (s, t: _InferenceVarType) => lowerBound(t,s)
    // Type variables
    case (s@SVarType(_, id, _), t) =>
      val sParam = staticParam(id)
      val supers = toListFromImmutable(sParam.getExtendsClause)
      or(supers.map(sub(_, t)))
    // Trait types
    case (s: TraitType, t: TraitType) if (t==OBJECT) => True
    case (STraitType(_, n1, a1,_), STraitType(_, n2, a2, _)) if (n1==n2) =>
      and((a1, a2).zipped.map((a, b) => eqv(a, b)))
    case (s:TraitType , t: TraitType) =>
      val par = parents(s)
      or(par.map(sub(_, t)))
    case (s: TraitSelfType, t) => sub(removeSelf(s), t)
    case (t, STraitSelfType(_, named, _)) => sub(t,removeSelf(named))
    case (s: ObjectExprType, t) => sub(removeSelf(s), t)
    // Arrow types
    case (SArrowType(_, d1, r1, e1, i1, _), SArrowType(_, d2, r2, e2, i2, _)) =>
      and(and(sub(d2, d1), sub(r1, r2)), sub(e1, e2))
    // Tuple types
    // TODO: Handle keywords
    case (s@STupleType(_, e1, None, _), t@STupleType(_, e2, Some(mv2), _)) =>
      sub(s, disjunctFromTuple(t, e1.size))
    case (s@STupleType(_, e1, Some(v1), _), t@STupleType(_, e2, _, _)) =>
      and(sub(disjunctFromTuple(s, e1.size), disjunctFromTuple(t, e1.size)),
          sub(disjunctFromTuple(s, e2.size + 1), disjunctFromTuple(t, e2.size + 1)))
    case (s@STupleType(_, e1, None, _), t@STupleType(_, e2, None, _)) if e1.size == e2.size =>
      or(mapOr(e1)(eqv(_, BOTTOM)), mapAnd((e1,e2).zipped.map((a, b) => sub(a, b)))(x => x))
    case (s@STupleType(_, e1, _, _), t) =>
      mapOr(e1)(eqv(_, BOTTOM))
    case (s, t:TupleType) => 
      sub(s, disjunctFromTuple(t,1))
    case _ => False
  }
  
  protected def sub(x: List[KeywordType], y: List[KeywordType]): CFormula = {
    def toPair(k: KeywordType) = (k.getName, k.getKeywordType)
    val xmap = Map(x.map(toPair):_*)
    val ymap = Map(y.map(toPair):_*)
    def compare(id: Id) = (xmap.get(id), ymap.get(id)) match {
      case (None, _) => True
      case (Some(s), Some(t)) => sub(s, t)
      case _ => False
    }
    and(xmap.keys.map(compare))
  }

  protected def sub(x: Effect, y: Effect): CFormula = {
    val (SEffect(_, tc1, io1), SEffect(_, tc2, io2)) = (x,y)
    if (!io1 || io2)
      sub(makeUnionType(tc1.getOrElse(Nil)), makeUnionType(tc2.getOrElse(Nil)))
    else
      False
  }
  
  protected def nsub(s: Type, t: Type): CFormula = 
    fromBoolean(isFalse(sub(s, t)))
  
  def equivalent(x: Type, y: Type): CFormula = {
    val s = normalize(x)
    val t = normalize(y)
    eqv(s,t)
  }
  
  def notEquivalent(x: Type, y: Type): CFormula = {
    fromBoolean(isFalse(equivalent(x, y)))
  }

  protected def eqv(x: Type, y:Type): CFormula  = {
    and(sub(x, y), sub(y, x))
  }
  
  protected def eqv(x: StaticArg, y: StaticArg): CFormula = (x,y) match {
    case (STypeArg(_, _, s), STypeArg(_, _, t)) => eqv(s, t)
    case (SIntArg(_, _, a), SIntArg(_, _, b)) => fromBoolean(a==b)
    case (SBoolArg(_, _, a), SBoolArg(_, _, b)) => fromBoolean(a==b)
    case (SOpArg(_, _, a), SOpArg(_, _, b)) => fromBoolean(a==b)
    case (SDimArg(_, _, a), SDimArg(_, _, b)) => fromBoolean(a==b)
    case (SUnitArg(_, _, a), SUnitArg(_, _, b)) => fromBoolean(a==b)
    case _ => False
  }
  
  def excludes(x: Type, y: Type): CFormula =
    exc(normalize(removeSelf(x)), normalize(removeSelf(y)))
    
  def definitelyExcludes(x: Type, y: Type): Boolean =
    dexc(normalize(removeSelf(x)), normalize(removeSelf(y)))

  /** Determine if a collection of types all exclude each other. */
  def allExclude(ts: Iterable[Type]): CFormula =
    mapOr(Pairs.distinctPairsFrom(ts))(tt => excludes(tt._1, tt._2))
  
  def anyExclude(ts: Iterable[Type]):CFormula =
    mapAnd(Pairs.distinctPairsFrom(ts))(tt => excludes(tt._1, tt._2))
  
  protected def exc(x: Type, y: Type): CFormula = (x, y) match {
    case (s: BottomType, _) => True
    case (_, t: BottomType) => True
    case (s: AnyType, t) => sub(t, BOTTOM)
    case (s, t: AnyType) => sub(s, BOTTOM)
    case (s@SVarType(_, id, _), t) =>
      val sParam = staticParam(id)
      val supers = toListFromImmutable(sParam.getExtendsClause)
      mapOr(supers)(exc(_, t))
    case (s, t:VarType) => exc(t, s)
    case (s: TraitType, t: TraitType) =>
      def checkEC(s: TraitType, t: TraitType): CFormula = {
        def cEC(s: TraitType, t: TraitType) = mapOr(excludesClause(s))(sub(t, _))
        or(cEC(s, t), cEC(t, s))
      }
      def checkCC(s: TraitType, t: TraitType): CFormula = {
        def cCC(s: TraitType, t: TraitType): CFormula = {
          val scc = comprisesClause(s)
          if(scc.isEmpty) False else mapAnd(scc)(exc(t,_))
        }
        or(cCC(s, t), cCC(t, s))
      }
      def checkO(s: TraitType, t: TraitType): CFormula = {
        def cO(s: TraitType, t: TraitType): CFormula = typeCons(s.getName) match {
          case _:ObjectTraitIndex => nsub(s, t)
          case _ => False
        }
        or(cO(s,t), cO(t,s))
      }
      def checkP(s: TraitType, t: TraitType): CFormula = {
        val sas = ancestors(s) ++ Set(s)
        val tas = ancestors(t) ++ Set(t)
        def cP(s: BaseType, t: BaseType): CFormula = (s, t) match {
          case (s@STraitType(_, n1, a1, _), t@STraitType(_, n2, a2, _)) if (n1 == n2) =>
            //Todo: Handle int, nat, bool args
            mapOr((a1, a2).zipped.map{
              case (STypeArg(_, _, t1), STypeArg(_, _, t2)) => notEquivalent(t1, t2)
              case _ => False
            })(x => x)
          case _ => False
        }
        mapOr(for(sa <- sas; ta <- tas) yield (sa, ta))(tt => cP(tt._1, tt._2))
      }
      mapOr(List(checkEC(s,t), checkCC(s,t), checkO(s,t), checkP(s,t)))(x => x)
    case (s@SIntersectionType(_, elts), t) =>
      or(sub(s, BOTTOM), mapOr(elts)(exc(_, t)))
    case (s, t: IntersectionType) => exc(t, s)
    case (s@SUnionType(_, elts), t) =>
      or(sub(s, BOTTOM), mapAnd(elts)(exc(_, t)))
    case (s, t: UnionType) => exc(t, s)
    case (s: ArrowType, t: ArrowType) => False
    case (s: ArrowType, t) => True
    case (s, t: ArrowType) => True
    // ToDo: Handle keywords
    case (STupleType(_, e1, mv1, _), STupleType(_, e2, mv2, _)) =>
      val excludes = mapOr((e1, e2).zipped.map((a, b) => exc(a, b)))(x => x)
      val different = (mv1, mv2) match {
        case (Some(v1), _) if (e1.size < e2.size) =>
          mapOr(e2.drop(e1.size))(exc(_, v1))
        case (_, Some(v2)) if (e1.size > e2.size) =>
          mapOr(e1.drop(e2.size))(exc(_, v2))
        case _ if (e1.size!=e2.size) => True
        case _ => False
      }
      or(different, excludes)
    case (s: TupleType, _) => True
    case (_, t: TupleType) => True
  }
  
  def dexc(s: Type, t: Type) = isTrue(exc(s, t))
  
  /*
   * Given two types x and y this method computes the constraints
   * under which x and y do not exclude on another. For example if
   * we have x=List[\$i\] and y=List[\$k\] then x and y exclude one another
   * unless $i=$j. Note that this method only generates equality constraints.
   */
  def notExcludes(x: Type , y: Type): CFormula = False 
  
  protected def removeSelf(x: Type) = {
    object remover extends Walker {
      override def walk(y: Any): Any = y match {
        case t:TraitSelfType =>
          if (t.getComprised.isEmpty) t.getNamed
          else NF.makeIntersectionType(t.getNamed,
                                       NF.makeMaybeUnionType(t.getComprised))
        case t:ObjectExprType => NF.makeMaybeIntersectionType(t.getExtended)
        case _ => super.walk(y)
      }
    }
    remover(x).asInstanceOf[Type]
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
        case t:TupleType => super.walk(t) match {
          case STupleType(i, e, Some(v: BottomType), k) => STupleType(i, e, None, k)
          case STupleType(i, e, vt, k) if e.contains(bottom) => bottom
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

  protected def normConjunct(x: Iterable[Type]): List[Type] = {
    if(x.exists(y => x.exists(z => dexc(y, z))))
      List(BOTTOM)
    else {
      val ds = x.foldLeft(List[Type]())((l, a) => {
        val l2 = l.filter(x => !isTrue(sub(a, x)))
        l2 ++ (if (l2.exists(x => isTrue(sub(x, a)))) Nil else List(a))
      })
      val es = ds.map(_ match {
        case t:TupleType => Left(t)
        case s => Right(s)
      })
      val (ts, ss) = (for (Left(x) <- es) yield x, for (Right(x) <- es) yield x)
      ss ++ normTuples(ts)
    }
  }

  protected def normTuples(ts: List[TupleType]): List[TupleType] = ts match {
    case Nil => Nil
    case _ => List(ts.reduceLeft(normTuples))
  }

  //ToDo: Keywords
  protected def normTuples(x: TupleType, y: TupleType): TupleType = (x,y) match {
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

  protected def normDisjunct(x: Iterable[Type]): List[Type] = {
      x.foldLeft(List[Type]())((l, a) => {
        val l2 = l.filter(x => !isTrue(sub(x,a)))
        l2 ++ (if (l2.exists(x => isTrue(sub(a,x)))) Nil else List(a))
      })
  }

  protected def cross[T](x: Iterable[Iterable[T]]): Iterable[Iterable[T]] = {
    x.foldLeft(List(List[T]()))((l, a) => l.flatMap(b => a.map(b ++ List(_))))
  }
  
  def mergeEffect(x: Effect, y: Effect) = {
    val SEffect(i1, t1, io1) = x
    val SEffect(i2, t2, io2) = y
    val tc = meet(join(t1.getOrElse(Nil)), join(t2.getOrElse(Nil))) match {
      case t:BottomType => None
      case SUnionType(_, elts) => Some(elts)
      case t => Some(List(t))
    }
    //merge ASTNodeInfo?
    SEffect(i1, tc, io1 && io2)
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
  
  // For the purposes of this method Any is not the Parent of Object as it is not a TraitType
  def parents(t: TraitType): Set[BaseType] = {
    val STraitType(_, n, a, _) = t
    val index = typeCons(n).asInstanceOf[TraitIndex]
    toListFromImmutable(index.extendsTypes).
      map(tw =>
        substitute(a, toListFromImmutable(index.staticParameters), tw.getBaseType).asInstanceOf[BaseType]).
          toSet
  }
  
  def ancestors(t: TraitType): Set[BaseType] =
    parents(t).flatMap{
      case s:TraitType => Set(s) ++ parents(s)
      case x => Set(x)
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
  
  protected def mapAnd[T](x: Iterable[T])(f: T=>CFormula): CFormula = 
    x.map(f).foldLeft(True.asInstanceOf[CFormula])(and)
  protected def mapOr[T](x: Iterable[T])(f: T=>CFormula): CFormula = 
    x.map(f).foldLeft(False.asInstanceOf[CFormula])(or)
    
}

object TypeAnalyzer {
  def make(traits: TraitTable) = new TypeAnalyzer(traits, KindEnv.makeFresh)
}
