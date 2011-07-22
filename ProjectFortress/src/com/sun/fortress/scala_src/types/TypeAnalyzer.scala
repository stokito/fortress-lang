/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.scala_src.types

import _root_.java.util.{List => JavaList}
import _root_.edu.rice.cs.plt.tuple.{Option => JavaOption}
// import com.sun.fortress.compiler.index._
import com.sun.fortress.compiler.index.ApiIndex
import com.sun.fortress.compiler.index.ComponentIndex
import com.sun.fortress.compiler.index.Function
import com.sun.fortress.compiler.index.Functional
import com.sun.fortress.compiler.index.FunctionalMethod
import com.sun.fortress.compiler.index.HasSelfType
import com.sun.fortress.compiler.index.Method
import com.sun.fortress.compiler.index.TraitIndex
import com.sun.fortress.compiler.index.TypeConsIndex
import com.sun.fortress.compiler.index.ObjectTraitIndex
import com.sun.fortress.compiler.index.ProperTraitIndex
import com.sun.fortress.compiler.index.TypeAliasIndex

import com.sun.fortress.compiler.Types.ANY
import com.sun.fortress.compiler.Types.BOTTOM
import com.sun.fortress.compiler.Types.OBJECT
import com.sun.fortress.exceptions.InterpreterBug.bug
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.NodeFactory.typeSpan
import com.sun.fortress.nodes_util.{NodeFactory => NF}
import com.sun.fortress.nodes_util.{NodeUtil => NU}
import com.sun.fortress.repository.ProjectProperties

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
  
  private final val debugSubtype = ProjectProperties.getBoolean("fortress.debug.analyzer.subtype", false)
  private final val cacheSubtypes = ProjectProperties.getBoolean("fortress.analyzer.subtype.cache", true)
  private final val cacheExcludes = ProjectProperties.getBoolean("fortress.analyzer.excludes.cache", true)
  private final val cacheNormalizeTrait = ProjectProperties.getBoolean("fortress.analyzer.normalize.trait.cache", true)
  private final val cacheNormalizeVar = ProjectProperties.getBoolean("fortress.analyzer.normalize.var.cache", true)
  private final val cacheNormalizeTuple = ProjectProperties.getBoolean("fortress.analyzer.normalize.tuple.cache", true)
  private final val cacheNormalizeArrow = ProjectProperties.getBoolean("fortress.analyzer.normalize.arrow.cache", true)
  private final val cacheNormalizeOther = ProjectProperties.getBoolean("fortress.analyzer.normalize.Other.cache", true)
  
  type hType = (Boolean, Boolean, Type, Type)
  implicit val ta: TypeAnalyzer = this
  
  def top = ANY
  def bottom = BOTTOM

  def lteq(x: Type, y: Type): Boolean =  isTrue(subtype(x, y))
  def meet(x: Type, y: Type): Type = meet(List(x, y))
  def meet(x: Iterable[Type]): Type = normalize(makeIntersectionType(x))
  def join(x: Type, y: Type): Type = join(List(x, y))
  def join(x: Iterable[Type]): Type = normalize(makeUnionType(x))

  def subtype(x: Type, y: Type): CFormula =
    sub(normalize(x), normalize(y))(Set())
  
  protected def sub(x: Type, y: Type)(implicit history: Set[hType]): CFormula = 
    pSub(x, y)(false, history)
    
  def notSubtype(x: Type, y: Type): CFormula = 
    nsub(normalize(x), normalize(y))(Set())
  
  protected def nsub(x: Type, y: Type)(implicit history: Set[hType]): CFormula = 
    pSub(x, y)(true, history)

  private val pSubMemo = new scala.collection.mutable.HashMap[(Type, Type, Boolean, Set[hType]), CFormula]()
    
  protected def pSub(x: Type, y: Type)(implicit negate: Boolean, history: Set[hType]): CFormula = {
     if (debugSubtype)
         System.err.println("psub > (" + x + ", " + y + ", " + negate + ")" )
     val rval = if (x == y)
           pTrue()
         else if (cacheSubtypes)
           pSubMemo.get((x, y, negate, history)) match {
            case Some(v) => v
            case _ => 
              val result = pSubInner(x,y)
              pSubMemo += ((x, y, negate, history) -> result)
              result
           }
         else pSubInner(x,y)
     if (debugSubtype)
         System.err.println("psub < (" + x + ", " + y + ", " + negate + ") RETURNS " + rval)
     rval
  }

  protected def pSubInner(x: Type, y: Type)(implicit negate: Boolean, history: Set[hType]): CFormula = {
    (x, y) match {
     // case (s,t) if (s==t) => pTrue() // moved up before cache for speed
    case (s: BottomType, _) => pTrue()
    case (s, t: AnyType) => pTrue()
    // Intersection types
    case (s, SIntersectionType(_,ts)) =>
      pAnd(ts.map(pSub(s, _)))
    case (SIntersectionType(_,ss), t) => // Note that t is not an SIntersectionType
      pOr(pOr(Pairs.distinctPairsFrom(ss).map(tt => pExc(tt._1, tt._2))),
          pOr(ss.map(pSub(_, t))))
    // Union types
    case (SUnionType(_,ss), t) =>
      pAnd(ss.map(pSub(_, t)))
    case (s, SUnionType(_, ts)) =>
      pOr(ts.map(pSub(s, _)))
    // Inference variables
    case (s: _InferenceVarType, t: _InferenceVarType) =>
      pAnd(pUpperBound(s, t), pLowerBound(t,s))
    case (s: _InferenceVarType, t) => pUpperBound(s,t)
    case (s, t: _InferenceVarType) => pLowerBound(t,s)
    // Type variables
    case (s@SVarType(_, id, _), t) =>
      val hEntry = (negate, true, s, t)
      if (history.contains(hEntry))
        False
      else {
        val nHistory = history + hEntry
        val sParam = staticParam(id)
        val supers = meet(toListFromImmutable(sParam.getExtendsClause))
        if (negate)
          pExc(supers, t)(!negate, nHistory)
        else pSub(supers, t)(negate, nHistory)
      }
    // Trait types
    case (s: TraitType, t: TraitType) if (t==OBJECT) => pTrue()
    case (STraitType(_, n1, a1,_), STraitType(_, n2, a2, _)) if (typeCons(n1)==typeCons(n2)) =>
      // println("checking " + a1 + " vs " + a2)
      pAnd((a1, a2).zipped.map((a, b) => pEqv(a, b)))
    case (s:TraitType , t: TraitType) =>
      // println("checking " + s + " <: " + t)
      val par = parents(s)
      // println(s + " has parents " + par)
      pOr(par.map(pSub(_, t)))
    case (s: TraitSelfType, t) => pSub(removeSelf(s), t)
    case (t, STraitSelfType(_, named, _)) => pSub(t,removeSelf(named))
    case (s: ObjectExprType, t) => pSub(removeSelf(s), t)
    // Arrow types
    case (SArrowType(_, d1, r1, e1, i1, _), SArrowType(_, d2, r2, e2, i2, _)) =>
      pAnd(pAnd(pSub(d2, d1), pSub(r1, r2)), pSub(e1, e2))
    // Tuple types
    // TODO: Handle keywords
    case (s@STupleType(_, e1, None, _), t@STupleType(_, e2, Some(mv2), _)) =>
      pSub(s, disjunctFromTuple(t, e1.size))
    case (s@STupleType(_, e1, Some(v1), _), t@STupleType(_, e2, _, _)) =>
      pAnd(pSub(disjunctFromTuple(s, e1.size), disjunctFromTuple(t, e1.size)),
           pSub(disjunctFromTuple(s, e2.size + 1), disjunctFromTuple(t, e2.size + 1)))
    case (s@STupleType(_, e1, None, _), t@STupleType(_, e2, None, _)) if e1.size == e2.size =>
      pOr(pOr(e1.map(pEqv(_, BOTTOM))), pAnd((e1,e2).zipped.map((a, b) => pSub(a, b))))
    case (s@STupleType(_, e1, _, _), t) =>
      pOr(e1.map(pEqv(_, BOTTOM)))
    case (s, t:TupleType) => 
      pSub(s, disjunctFromTuple(t,1))
    case _ => pFalse()
  }}
  
  /* This is not yet hooked into pSub
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
 */

  protected def pSub(x: Effect, y: Effect)(implicit negate: Boolean, history: Set[hType]): CFormula = {
    val (SEffect(_, tc1, io1), SEffect(_, tc2, io2)) = (x,y)
    pAnd(pFromBoolean(!io1 || io2),
         pSub(makeUnionType(tc1.getOrElse(Nil)), makeUnionType(tc2.getOrElse(Nil))))
  }
  
  def equivalent(x: Type, y: Type): CFormula = eqv(normalize(x), normalize(y))(Set())
  
  protected def eqv(x: Type, y: Type)(implicit history: Set[hType]): CFormula = 
    pEqv(x, y)(false, history)
  
  protected def pEqv(x: Type, y: Type)(implicit negate: Boolean, history: Set[hType]) = 
    pAnd(pSub(x,y), pSub(y,x))
  
  def notEquivalent(x: Type, y: Type): CFormula = nEqv(normalize(x), normalize(y))(Set())
  
  protected def nEqv(x: Type, y: Type)(implicit history: Set[hType]): CFormula = 
    pEqv(x, y)(true, history)
  
  protected def pEqv(x: StaticArg, y: StaticArg)(implicit negate: Boolean, history: Set[hType]): CFormula = (x,y) match {
    case (a,b) if (a==b) => pTrue()
    case (STypeArg(_, _, s), STypeArg(_, _, t)) => pEqv(s, t)
    // Not handling all static args properly yet
    case (SOpArg(_, _, a:_InferenceVarOp, _), b: OpArg) => pTrue()
    case (a: OpArg, SOpArg(_, _, b: _InferenceVarOp, _)) => pTrue()
    case (_: IntArg, _: IntArg) => pTrue()
    case (_: BoolArg, _: BoolArg) => pTrue()
    case (_: DimArg, _: DimArg) => pTrue()
    case (_: UnitArg, _: UnitArg) => pTrue()
    case _ => pFalse()
  }
  
  def excludes(x: Type, y: Type): CFormula =
    exc(normalize(removeSelf(x)), normalize(removeSelf(y)))(Set())
    
  protected def exc(x: Type, y: Type)(implicit history: Set[hType]): CFormula = 
    pExc(x,y)(false, history)
    
  def definitelyExcludes(x: Type, y: Type): Boolean =
    isTrue(excludes(x,y))

  def dExc(x: Type, y: Type)(implicit history: Set[hType]): Boolean = isTrue(exc(x,y))

  /*
   * Given two types x and y this method computes the constraints
   * under which x and y do not exclude one another. For example if
   * we have x=List[\$i\] and y=List[\$k\] then x and y exclude one another
   * unless $i=$j. Note that this method only generates equality constraints.
   */
  def notExcludes(x: Type , y: Type): CFormula = nExc(normalize(x), normalize(y))(Set())
    
  protected def nExc(x: Type, y: Type)(implicit history: Set[hType]): CFormula = 
    pExc(x,y)(true, history)
  
  /** Determine if a collection of types all exclude each other. */
  def allExclude(ts: Iterable[Type]): CFormula =
    and(Pairs.distinctPairsFrom(ts).map(tt => excludes(tt._1, tt._2)))
  
  def anyExclude(ts: Iterable[Type]):CFormula =
    or(Pairs.distinctPairsFrom(ts).map(tt => excludes(tt._1, tt._2)))


  private val pExcMemo = new scala.collection.mutable.HashMap[(Type, Type, Boolean, Set[hType]), CFormula]()
    
  protected def pExc(x: Type, y: Type)(implicit negate: Boolean, history: Set[hType]): CFormula = {
     val rval = if (x == y)
           pFalse()
         else if (cacheExcludes)
           pExcMemo.get((x, y, negate, history)) match {
            case Some(v) => v
            case _ => 
              val result = pExcInner(x,y)
              pExcMemo += ((x, y, negate, history) -> result)
              result
           }
         else pExcInner(x,y)
     rval
  }

  protected def pExcInner(x: Type, y: Type)(implicit negate: Boolean, history: Set[hType]): CFormula = {
        (removeSelf(x), removeSelf(y)) match {
    case (s, t) if (s==t) => pFalse()
    case (s: BottomType, _) => pTrue()
    case (_, t: BottomType) => pTrue()
    case (s: AnyType, t) => pSub(t, BOTTOM)
    case (s, t: AnyType) => pSub(s, BOTTOM)
    case (s@SIntersectionType(_, elts), t) =>
      pOr(pSub(s, BOTTOM), pOr(elts.map(pExc(_, t))))
    case (s, t: IntersectionType) => exc(t, s)
    case (s@SUnionType(_, elts), t) =>
      pOr(pSub(s, BOTTOM), pAnd(elts.map(pExc(_, t))))
    case (i: _InferenceVarType, j: _InferenceVarType) => pAnd(pExclusion(i,j), pExclusion(j,i))
    case (i: _InferenceVarType, t) => pExclusion(i, t)
    case (s, j: _InferenceVarType) => pExc(j, s)
    case (s, t: UnionType) => exc(t, s)
    case (s@SVarType(_, id, _), t) =>
      val hEntry = (negate, false, s, t)
      if (history.contains(hEntry))
        False
      else {
        val nHistory = history + hEntry
        val sParam = staticParam(id)
        val supers = meet(toListFromImmutable(sParam.getExtendsClause))
        if (negate) pFalse()(!negate) else pExc(supers, t)(negate, nHistory)
      }
    case (s, t:VarType) => exc(t, s)
    case (s: TraitType, t: TraitType) =>
      def checkEC(s: TraitType, t: TraitType): CFormula = {
        def cEC(s: TraitType, t: TraitType) = pOr(excludesClause(s).map(pSub(t, _)))
        pOr(cEC(s, t), cEC(t, s))
      }
      def checkCC(s: TraitType, t: TraitType): CFormula = {
        def cCC(s: TraitType, t: TraitType): CFormula = {
          val scc = comprisesClause(s)
          pAnd(pFromBoolean(!scc.isEmpty), pAnd(scc.map(pExc(t,_))))
        }
        pOr(cCC(s, t), cCC(t, s))
      }
      def checkO(s: TraitType, t: TraitType): CFormula = {
        def cO(s: TraitType, t: TraitType): CFormula = typeCons(s.getName) match {
          case _:ObjectTraitIndex => pSub(s, t)(!negate, history)
          case _ => pFalse()
        }
        pOr(cO(s,t), cO(t,s))
      }
      def checkP(s: TraitType, t: TraitType): CFormula = {
        val sas = ancestors(s) ++ Set(s)
        val tas = ancestors(t) ++ Set(t)
        def cP(s: BaseType, t: BaseType): CFormula = (s, t) match {
          case (s@STraitType(_, n1, a1, _), t@STraitType(_, n2, a2, _)) if (n1 == n2) =>
            //Todo: Handle int, nat, bool args
            pOr((a1, a2).zipped.map{
              case (STypeArg(_, _, t1), STypeArg(_, _, t2)) => pEqv(t1, t2)(!negate, history)
              case _ => pFalse()
            })
          case _ => pFalse()
        }
        pOr(for(sa <- sas; ta <- tas) yield cP(sa, ta))
      }
      pOr(List(checkEC(s,t), checkCC(s,t), checkO(s,t), checkP(s,t)))
    case (s: ArrowType, t: ArrowType) => pFalse()
    case (s: ArrowType, t) => pTrue()
    case (s, t: ArrowType) => pTrue()
    // ToDo: Handle keywords
    case (STupleType(_, e1, mv1, _), STupleType(_, e2, mv2, _)) =>
      val excludes = pOr((e1, e2).zipped.map((a, b) => pExc(a, b)))
      val different = (mv1, mv2) match {
        case (Some(v1), _) if (e1.size < e2.size) =>
          pOr(e2.drop(e1.size).map(pExc(_, v1)))
        case (_, Some(v2)) if (e1.size > e2.size) =>
          pOr(e1.drop(e2.size).map(pExc(_, v2)))
        case _ if (e1.size!=e2.size) => pTrue()
        case _ => pFalse()
      }
      pOr(different, excludes)
    case (s: TupleType, _) => pTrue()
    case (_, t: TupleType) => pTrue()
  }
  }
  
  protected def removeSelf(x: Type) = {
    object remover extends Walker {
      override def walk(y: Any): Any = y match {
        case t:TraitSelfType =>
          if (t.getComprised.isEmpty)
            t.getNamed
          else
            NF.makeIntersectionType(t.getNamed, NF.makeMaybeUnionType(t.getComprised))
        case t:ObjectExprType => NF.makeMaybeIntersectionType(t.getExtended)
        case _ => super.walk(y)
      }
    }
    remover(x).asInstanceOf[Type]
  }
  /*
   * For is-subtype queries, it makes no sense to stir in the comprised types;
   * if this is a subtype, then so are they
   * (unless they are declared to exclude -- but does that matter?) 
   */
  protected def removeSelfAsSubtype(x: Type) = {
    object remover extends Walker {
      override def walk(y: Any): Any = y match {
        case t:TraitSelfType => t.getNamed
        case t:ObjectExprType => NF.makeMaybeIntersectionType(t.getExtended)
        case _ => super.walk(y)
      }
    }
    remover(x).asInstanceOf[Type]
  }
  

  private val normalizeVarTypeMemo = new scala.collection.mutable.HashMap[Id, Any]()
  private val normalizeTraitTypeMemo = new scala.collection.mutable.HashMap[(Id, List[Type]), Any]()
  private val normalizeTupleTypeMemo = new scala.collection.mutable.HashMap[(List[Type], Option[Type]), Any]()
  private val normalizeArrowTypeMemo = new scala.collection.mutable.HashMap[(Type, Type, Effect, Boolean), Any]()
  private val normalizeOtherTypeMemo = new scala.collection.mutable.HashMap[Any, Any]()

  def normalize(x: Type): Type = {
    object normalizer extends Walker {
      override def walk(y: Any): Any = y match {
        case t: VarType => walkVarType(t)
        case t: TraitType => walkTraitType(t)
        case t: TupleType => walkTupleType(t)
        case t: ArrowType => walkArrowType(t)
        case _ => walkOtherType(y)
      }
      def walkVarType(t: VarType) = {
        t match { case SVarType(_, n, _) =>
            if (cacheNormalizeVar)
              normalizeVarTypeMemo.get(n) match {
                case Some(v) => v
                case _ =>
                  val result = walkVarTypeInner(t, n)
                  normalizeVarTypeMemo += (n -> result)
                  result
              }
            else walkVarTypeInner(t, n)
        }
      }
      def walkVarTypeInner(t: VarType, n: Id) = super.walk(t)
      def walkTraitType(t: TraitType) = {
        t match { case STraitType(_, n, a, _) =>
            if (cacheNormalizeTrait && a.forall(s => s.isInstanceOf[TypeArg])) {
              // For now, no attempt to handle static args other than type args
              val args = a.map(s => s.asInstanceOf[TypeArg].getTypeArg)
              normalizeTraitTypeMemo.get((n, args)) match {
                case Some(v) => v
                case _ =>
                  walkTraitTypeInner(t, n, a) match {
                    case result@STraitType(_, _, aa, _) =>
                       normalizeTraitTypeMemo += ((n, args) -> result)
                       normalizeTraitTypeMemo += ((n, aa.map(s => s.asInstanceOf[TypeArg].getTypeArg)) -> result)
                       result
                  }
              }
            }
            else walkTraitTypeInner(t, n, a)
        }
      }
      def walkTraitTypeInner(t: TraitType, n: Id, a: List[StaticArg]) = {
        typeCons(n) match {
          case ti: TypeAliasIndex =>
            val params = toListFromImmutable(ti.staticParameters)
            walk(substitute(a, params, ti.ast.getTypeDef))
          case _ => super.walk(t)
        }
      }
      def walkTupleType(t: TupleType) = {
        if (cacheNormalizeTuple)
          t match { case STupleType(_, e, vt, _) =>
              normalizeTupleTypeMemo.get((e, vt)) match {
                case Some(v) => v
                case _ =>
                  walkTupleTypeInner(t) match {
                    case result@STupleType(_, ee, vtx, _) =>
                      normalizeTupleTypeMemo += ((e, vt) -> result)
                      normalizeTupleTypeMemo += ((ee, vtx) -> result)
                      result
                    case result: BottomType =>
                      normalizeTupleTypeMemo += ((e, vt) -> result)
                      result
                  }
              }
          }
        else walkTupleTypeInner(t)
      }
      def walkTupleTypeInner(t: TupleType) = {
        //ToDo: Handle keywords
        super.walk(t) match {
          case STupleType(i, e, Some(v: BottomType), k) => STupleType(i, e, None, k)
          case STupleType(i, e, vt, k) if e.contains(bottom) => bottom
          case _ => t
        }
      }
      def walkArrowType(t: ArrowType) = {
        if (cacheNormalizeArrow)
          t match { case SArrowType(_, d, r, e, i, _) =>
              normalizeArrowTypeMemo.get((d, r, e, i)) match {
                case Some(v) => v
                case _ =>
                  walkArrowTypeInner(t) match {
                    case result@SArrowType(_, dd, rr, ee, ii, _) =>
                      normalizeArrowTypeMemo += ((d, r, e, i) -> result)
                      normalizeArrowTypeMemo += ((dd, rr, ee, ii) -> result)
                      result
                  }
              }
          }
        else walkArrowTypeInner(t)
      }
      def walkArrowTypeInner(t: ArrowType) = super.walk(t)
      def walkOtherType(t: Any) = {
        if (cacheNormalizeOther)
          normalizeOtherTypeMemo.get(t) match {
            case Some(v) => v
            case _ =>
              val result = walkOtherTypeInner(t)
              normalizeOtherTypeMemo += (t -> result)
              result
          }
        else walkOtherTypeInner(t)
      }
      def walkOtherTypeInner(t: Any) = t match {
        case u@SUnionType(_, e) =>
          val ps = e.flatMap(t => disjuncts(walk(t).asInstanceOf[Type]))
          makeUnionType(normDisjunct(ps))
        case i@SIntersectionType(_, e) =>
          val sop = cross(e.map(t => disjuncts(walk(t).asInstanceOf[Type])))
          val ps = sop.map(t => makeIntersectionType(normConjunct(t.flatMap(disjuncts))))
          makeUnionType(normDisjunct(ps))
        case _ => super.walk(t)
      }
    }
    // Here's the body: walk the type, and make sure it's a type when finished
    normalizer(x).asInstanceOf[Type]
  }

  protected def normConjunct(x: Iterable[Type]): List[Type] = {
    implicit val h = Set[hType]()
    if(x.exists(y => x.exists(z => dExc(y, z))))
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
      implicit val h = Set[hType]()
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
      case s:TraitType => Set(s) ++ ancestors(s)
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
  
  def extendJ(params: JavaList[StaticParam], where: JavaOption[WhereClause]) =
      new TypeAnalyzer(traits, env.extend(toList(params), toOption(where)))
  
  def pTrue()(implicit negate: Boolean) = 
    if (negate) False else True
  def pFalse()(implicit negate: Boolean) = 
    if (negate) True else False
  def pAnd(c1: CFormula, c2: CFormula)(implicit ta: TypeAnalyzer, negate: Boolean) = 
    if (negate) or(c1, c2) else and(c1, c2)
  def pAnd(cs: Iterable[CFormula])(implicit ta: TypeAnalyzer, negate: Boolean): CFormula = 
    if (negate) or(cs) else and(cs)
  def pOr(c1: CFormula, c2: CFormula)(implicit ta: TypeAnalyzer, negate: Boolean) = 
    if (negate) and(c1, c2) else or(c1, c2)
  def pOr(cs: Iterable[CFormula])(implicit ta: TypeAnalyzer, negate: Boolean) = 
    if (negate) and(cs) else or(cs)
  def pUpperBound(i: _InferenceVarType, t: Type)(implicit negate: Boolean) = 
    if (negate) notUpperBound(i, t) else upperBound(i, t)
  def pLowerBound(i: _InferenceVarType, t: Type)(implicit negate: Boolean) = 
    if (negate) notLowerBound(i, t) else lowerBound(i, t)
  def pExclusion(i: _InferenceVarType, t: Type)(implicit negate: Boolean) = 
    if (negate) notExclusion(i, t) else exclusion(i, t)
  def pFromBoolean(b: Boolean)(implicit negate: Boolean) = fromBoolean(negate != b)
}

object TypeAnalyzer {
  def make(traits: TraitTable) = new TypeAnalyzer(traits, KindEnv.makeFresh)
}
