/*******************************************************************************
    Copyright 2010,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

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
import com.sun.fortress.repository.ProjectProperties
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.typechecker.Formula._
import com.sun.fortress.scala_src.typechecker.staticenv.KindEnv
import com.sun.fortress.scala_src.typechecker.TraitTable
import com.sun.fortress.scala_src.types.TypeAnalyzerUtil._
import com.sun.fortress.scala_src.useful.ErrorLog
import com.sun.fortress.scala_src.useful.Lists.toJavaList
import com.sun.fortress.scala_src.useful.Maps._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.Sets._
import com.sun.fortress.scala_src.useful.SNodeUtil._
import com.sun.fortress.scala_src.useful.STypesUtil._
import com.sun.fortress.syntax_abstractions.rats.RatsUtil.getFreshName
import com.sun.fortress.useful.NI

/* This class implements methods for dealing with type schema such as
 * subtyping, alpha renaming, checking for equality up to renaming and 
 * so forth.
 * 
 * The biggest weakness of this implementation is that we do
 * not syntactically distinguish types and type schema.
 * Therefore we cannot tell the difference betwen parameters that are
 * supposed to be universally and existentially quantified. If we were to
 * rewrite the front end fixing this would be a high priority. 
 */

class TypeSchemaAnalyzer(implicit val ta: TypeAnalyzer) {
  private final val cacheSchemaSubtypes = ProjectProperties.getBoolean("fortress.schema.subtype.cache", true)

  def makeDomainFromArrow(a: ArrowType): Type = {
    insertStaticParams(a.getDomain, getStaticParams(a))
  }
  
  def makeRangeFromArrow(a: ArrowType): Type = {
    insertStaticParams(a.getRange, getStaticParams(a))
  }
  
  def makeParamFromDomain(t: Type, i:Int): Type = t match {
      case (u:TupleType) =>
          insertStaticParams(u.getElements.get(i), getStaticParams(t));
      case _ =>
          t
  }

  private val subtypeUAMemo = new scala.collection.mutable.HashMap[(ArrowType, ArrowType), Boolean]()
    
  def subtypeUA(x: ArrowType, y: ArrowType): Boolean = {
     val rval = if (x == y)
           true
         else if (cacheSchemaSubtypes)
           subtypeUAMemo.get((x, y)) match {
            case Some(v) => v
            case _ => 
              val result = subtypeUAInner(x,y)
              subtypeUAMemo += ((x, y) -> result)
              result
           }
         else subtypeUAInner(x,y)
     rval
  }

  // Subtyping on universal arrows
  private def subtypeUAInner(s: ArrowType, t: ArrowType): Boolean =
    subUA(normalizeUA(alphaRenameTypeSchema(s).asInstanceOf[ArrowType]),
          normalizeUA(alphaRenameTypeSchema(t).asInstanceOf[ArrowType]))
  
  // Subtyping on universal arrows with distinct parameters  
  protected def subUA(s: ArrowType, t: ArrowType): Boolean = (s, t) match {
    // t has static parameters
    case (s, t) if !t.getInfo.getStaticParams.isEmpty =>
      /* Extend the type analyzer with the static parameters
       * from t. Then strip t of it's type parameters to get t'
       * and call lteq(s,t') */
      val tsa = extend(getStaticParams(t), getWhere(t))
      val tt = clearStaticParams(t).asInstanceOf[ArrowType]
      tsa.subUA(s, tt)
    // s has static parameters and t does not
    case (s, t) if !s.getInfo.getStaticParams.isEmpty =>
      /* Try and infer an instantiation sigma of s such that 
       * sigma(s) <: t */
      val sparams = getStaticParams(s)
      def constraintMaker(ss: Type, m: Map[Op, Op]) = ta.subtype(ss, t)
      !inferStaticParamsHelper(s, constraintMaker, true, true).isEmpty
    // neither has static parameters; use normal subtyping
    case (s, t) => ta.lteq(s, t)
  }
  
  private val subtypeEDMemo = new scala.collection.mutable.HashMap[(Type, Type), Boolean]()
    
  def subtypeED(x: Type, y: Type): Boolean = {
     val rval = if (x == y)
           true
         else if (cacheSchemaSubtypes)
           subtypeEDMemo.get((x, y)) match {
            case Some(v) => v
            case _ => 
              val result = subtypeEDInner(x,y)
              subtypeEDMemo += ((x, y) -> result)
              result
           }
         else subtypeEDInner(x,y)
     rval
  }

  // Subtyping for existential domains
  private def subtypeEDInner(s: Type, t: Type) = 
    subED(normalizeED(alphaRenameTypeSchema(s)), normalizeED(alphaRenameTypeSchema(t)))
  
  private def subED(s: Type, t: Type): Boolean = (s,t) match {
    // t has static parameters
    case (s,t) if !s.getInfo.getStaticParams.isEmpty =>
      /* Extend the type analyzer with the static parameters
       * from s. Then strip s of it's type parameters to get s'
       * and call lteq(s',t) */
      val tsa = extend(getStaticParams(s), getWhere(s))
      val ss = clearStaticParams(s)
      tsa.subED(ss, t)
    // s has static parameters and t does not
    case (s, t) if !t.getInfo.getStaticParams.isEmpty =>
      /* Try and infer an instantiation sigma of t such that 
       * s <: sigma(t) */
      val sparams = getStaticParams(t)
      def constraintMaker(tt: Type, m: Map[Op, Op]) = ta.subtype(s, tt)
      !inferStaticParamsHelper(t, constraintMaker, true, true).isEmpty
    // neither has static parameters; use normal subtyping
    case (s,t) => ta.lteq(s, t)
  }
  
  def equivalentED(s: Type, t: Type) =
    eqED(alphaRenameTypeSchema(s), alphaRenameTypeSchema(t))
    
  private def eqED(s: Type, t: Type) = 
    subED(s, t) && subED(t, s)
    
  /**
   * Returns a type with all bound static parameters replaced with unique
   * identifiers for this static environment. Each call should generate
   * entirely different names.
   */
  def alphaRenameTypeSchema(t: Type): Type = {
    if (t.getInfo.getStaticParams.isEmpty) return t
    
    // Make a substitution of [Ti -> Xi] for each static parameter Ti where
    // Xi is fresh in the static environment.
    val subst = getStaticParams(t).map { sp =>
      val srcName = sp.getName
      val dstName = makeFreshName(srcName, ta.env)
      (srcName, dstName)
    }
    alphaRename(subst, t).asInstanceOf[Type]
  }
  
  // Normalizes existential domains using exclusion as in the paper
  def normalizeED(e: Type): Type = {
    reduceED(e).getOrElse(return e)._1
  }
  
  // Normalizes universal arrows using exclusion as in the paper
  def normalizeUA(u: ArrowType): ArrowType = {
    val e = makeDomainFromArrow(u)
    val (ne, s) = reduceED(e).getOrElse(return u)
    val sp = getStaticParams(ne)
    insertStaticParams(ta.extend(sp, None).normalize(s(clearStaticParams(u))), sp).asInstanceOf[ArrowType]
  }
  
  // The meet of two existential types
  def meetED(x: Type, y: Type): Type = {
    val ax = alphaRenameTypeSchema(x)
    val ay = alphaRenameTypeSchema(y)
    val xp = getStaticParams(ax)
    val yp = getStaticParams(ay)
    assert((xp intersect yp).isEmpty)
    
    // Create the ugly meet.
    val meet = insertStaticParams(makeIntersectionType(Set(clearStaticParams(ax), clearStaticParams(ay))),
                                  xp ++ yp)
    
    // Try to reduce this existential type.
    normalizeED(meet)
  }
  
    // The meet of two existential types
  def joinED(x: Type, y: Type): Type = {
    val ax = alphaRenameTypeSchema(x)
    val ay = alphaRenameTypeSchema(y)
    val xp = getStaticParams(ax)
    val yp = getStaticParams(ay)
    assert((xp intersect yp).isEmpty)
    
    // Create the ugly meet.
    val join = insertStaticParams(makeUnionType(Set(clearStaticParams(ax), clearStaticParams(ay))),
                                  xp ++ yp)
    
    // Try to reduce this existential type.
    normalizeED(join)
  }

  // The special arrow that we use when checking the return type rule
  def returnUA(x: ArrowType, y: ArrowType) = (alphaRenameTypeSchema(x), alphaRenameTypeSchema(y)) match {
    case (SArrowType(STypeInfo(s1, p1, sp1, w1), d1, r1, e1, i1, m1), 
          SArrowType(STypeInfo(s2, p2, sp2, w2), d2, r2, e2, i2, m2)) =>
       normalizeUA(SArrowType(STypeInfo(s1, p1,sp1 ++ sp2, None), ta.meet(d1,d2), r2, ta.mergeEffect(e1,e2), i1 && i2, None))
  }
  
  /**
   * Checks if two types `s` and `t` are syntactically equivalent. If either
   * type has static parameters, then the `syntacticEqGeneric` method is called.
   * Otherwise, a simply equality check is performed on the types.
   */
  def syntacticEq(s: Type, t: Type): Boolean =
    if (!s.getInfo.getStaticParams.isEmpty() || !t.getInfo.getStaticParams.isEmpty())
      syntacticEqGeneric(s, t)
    else
      s == t
  
  /**
   * Checks if two type schema `s` and `t` are syntactically equivalent under
   * alpha renaming. The static params of `s` are used to instantiate `t` and
   * `t`'s bounds; if the instantiated type and bounds are syntactically
   * equivalent to those of `s`, then the two type schema `s` and `t` are
   * syntactically equivalent.
   */
  protected def syntacticEqGeneric(s: Type, t: Type): Boolean = {
    
    // Extract out the explicit static params of each.
    // TODO: What to do about lifted static params?
    val s_sp = getStaticParams(s).filterNot(_.isLifted)
    val t_sp = getStaticParams(t).filterNot(_.isLifted)
    
    // If they don't match in kind, return false.
    if (!equalKinds(s_sp, t_sp)) return false
    
    // Get the list of bounds on the static params. Each elt is Some(b) iff that
    // static param is a type parameter.
    val s_bds = s_sp.map(staticParamBoundType)
    val t_bds = t_sp.map(staticParamBoundType)
    
    // Create the StaticParam -> StaticArg replacement list. t's static params
    // will map to s's static params' corresponding static args.
    val s_sp_args = s_sp.map(staticParamToArg)
    
    // Instantiate t and its bounds with s's static params.
    val t_inst = staticInstantiation(s_sp_args, t).getOrElse{return false}
    val t_bds_inst = t_bds.map { bd_opt =>
      bd_opt.map { bd =>
        staticInstantiation(s_sp_args, bd).getOrElse{return false}
      }
    } // List of Option[Type], where Some(bd) is an instantiated type param bd
    
    // Clear the static params of s. (t has already been instantiated.)
    val s_typ = clearStaticParams(s)
    
    // Check that the instantiated body of t is equal to s, and that each
    // instantiated bound of t is equal to the corresponding bound of s.
    s_typ == t_inst && (s_bds, t_bds_inst).zipped.forall {
      case (Some(sbd), Some(tbd)) => sbd == tbd
      case (None, None) => true
      case _ => false
    }
  }
  
  /**
   * Remove any duplicates so that every type is syntactically different from
   * every other type.
   */
  def removeDuplicates(ts: List[Type]): List[Type] = ts match {
    case Nil => Nil
    case t :: rest =>
      val restMinusTs = rest.filterNot(s => syntacticEq(t, s))
      t :: removeDuplicates(restMinusTs)
  }
  
  /** Remove syntactically equivalent duplicates and take the intersection. */
  def duplicateFreeIntersection(ts: List[Type]): Type =
    NF.makeMaybeIntersectionType(toJavaList(removeDuplicates(ts)))
    
  
  def extend(params: List[StaticParam], where: Option[WhereClause]) =
    new TypeSchemaAnalyzer()(ta.extend(params, where))
  
  def toString(t: Type): String = {
    if (t.getInfo.getStaticParams.isEmpty)
      return t.toString
    val sparams = getStaticParams(t)
    "[" + sparams.mkString(", ") + "]" + t.toString
  }
  
  
  //TODO: FIX OP Params
  /**
   * Reduce the existential type `exType` to another existential type. For
   * more details, see Section 5.3 of our paper and the "Existential
   * reduction" definition.
   */
  private def reduceED(ed: Type): Option[(Type, Type => Type)] = {
    // Insert inference variables for type parameters
    val spd = getStaticParams(ed)
    val e = insertStaticParams(ta.extend(spd, None).normalize(clearStaticParams(ed)), spd)
    val sp = getStaticParams(e)
    val ia = sp.map(s => NF.make_InferenceVarType(NU.getSpan(s)))
    val temp = (ia, sp).zipped.flatMap{
      case (i, SStaticParam(info, n, _, _, _, _, _)) =>
        Some((i, NF.makeVarType(info.getSpan, n.asInstanceOf[Id])))
      case _ => None
    }
    val iv: Type => Type = liftSubstitution(Map[_InferenceVarType, Type](temp.toSeq:_*))
    val vi: Type => Type = liftSubstitution(Map[VarType, Type](temp.map(x =>(x._2, x._1)).toSeq:_*))
    val ie = vi(clearStaticParams(e))
    // Check under what conditions ie is (possibly) not equivalent to Bottom
    // Note that the notEquivalent method in ta is neccessarily not equivalent and is not the same
    // Add bounds (even if we can't use them very well yet)
    val ub = and((ia, sp).zipped.flatMap{
      case (i, SStaticParam(_, _, p, _, _, _:KindType, _)) =>
        Some(upperBound(i, ta.meet(p.map(vi))))
      case _ => None
    })
    val c = and(negate(ta.equivalent(ie, BOTTOM)), ub)
    val (nc, ts, os) = unify(c).getOrElse(return None)
    val nub = cMap(ub, ts, os)
    if(implies(nub, nc)){
      // Need conjugate s by the map that sends static args to inference variables
      val sub = iv compose ts compose vi
      val nsp = boundsSubstitution(sub, sp).getOrElse{return None}
      // Make a new existential type.
      Some((insertStaticParams(ta.extend(nsp, None).normalize(sub(clearStaticParams(e))), nsp), sub))
    }
    else
      None
  }
  
  /**
   * Map phi onto the given static params to produce a new set of static
   * params. For more details, see the "Bounds substitution" definition
   * on pg. 10 of our paper.
   */
  
  //TODO: FIX THIS FOR OPS
  protected def boundsSubstitution(phi: Type => Type,
                                   sparams: List[StaticParam])
                                   : Option[List[StaticParam]] = {
    import scala.collection.mutable.HashMap
    
    // Create a mapping from type variables in the static params to their
    // corresponding bounds.
    val varsMap = new HashMap[VarType, List[Type]]
    for (SStaticParam(info, x:Id, exts, _, _, _:KindType, _) <- sparams)
      varsMap(NF.makeVarType(info.getSpan, x)) = exts
    
    // Get the unique vars in the image under phi.
    val imageVars = varsMap.keys.map(phi).filter(_.isInstanceOf[VarType])
                           .toList.distinct.map(_.asInstanceOf[VarType])
    
    // Transfer bounds from the image vars onto the preimage vars to produce
    // a new set of static params.
    val imageSparams = imageVars map { y =>
      
      // For each x that maps to y under phi, collect the image of its bound.
      val ybds = (for ((x, xbds) <- varsMap ; if phi(x) == y) yield xbds)
                   .toList.flatten.map(_.asInstanceOf[BaseType])
      
      // Create the static param for Y <: YBDS.
      NF.makeTypeParam(NU.getSpan(y), y.getName, toJavaList(ybds), none[Type], false)
    }
    
    // Create a type analyzer with only the image variables and their bounds.
    val imageTa = ta.extend(imageSparams, None)
    val rimageSparams = imageSparams.map{
      case SStaticParam(i, x, e, d, a, k:KindType, l) =>
        SStaticParam(i, x, conjuncts(imageTa.meet(e)).
                             toList.map(_.asInstanceOf[BaseType]), d, a, k, l)
      case x => x
    }
    val rimageTa = ta.extend(rimageSparams, None)
    // Verify that the image environment can prove that each variable's image
    // is a subtype of all its bounds' images.
    if (varsMap.forall { case (x, xbds) =>
      rimageTa.lteq(phi(x), phi(imageTa.meet(xbds)))
    }) Some(rimageSparams) // Success -- return the image's static params.
    else None             // Failure
  }
}
