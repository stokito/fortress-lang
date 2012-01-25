/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.scala_src.overloading

import _root_.java.util.{List => JavaList}
import com.sun.fortress.compiler.GlobalEnvironment
import com.sun.fortress.compiler.index._
import com.sun.fortress.compiler.typechecker.StaticTypeReplacer
import com.sun.fortress.compiler.Types.ANY
import com.sun.fortress.compiler.Types.BOTTOM
import com.sun.fortress.compiler.Types.OBJECT
import com.sun.fortress.exceptions.InterpreterBug.bug
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.NodeFactory._
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.typechecker.staticenv.KindEnv
import com.sun.fortress.scala_src.typechecker.Formula._
import com.sun.fortress.scala_src.typechecker.TraitTable
import com.sun.fortress.scala_src.types.TypeAnalyzer
import com.sun.fortress.scala_src.types.TypeSchemaAnalyzer
import com.sun.fortress.scala_src.useful.ErrorLog
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Maps._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.Sets._
import com.sun.fortress.scala_src.useful.STypesUtil._
import com.sun.fortress.useful.NI

class OverloadingOracle(implicit ta: TypeAnalyzer) extends PartialOrdering[Functional] {
  
  val sa = new TypeSchemaAnalyzer()
  
  def extend(params: List[StaticParam], where: Option[WhereClause]) = new OverloadingOracle()(ta.extend(params, where))
  
  override def tryCompare(x: Functional, y: Functional): Option[Int] = {
    val xLEy = lteq(x,y)
    val yLEx = lteq(y,x)
    if (xLEy) Some(if (yLEx) 0 else -1)
    else if (yLEx) Some(1) else None
  }
  
  // Checks when f is more specific than g
  def lteq(f: Functional, g: Functional): Boolean = {
    val fa = makeArrowFromFunctional(f, true).get
    val ga = makeArrowFromFunctional(g, true).get
    val fd = sa.makeDomainWithSelfFromArrow(fa)
    val gd = sa.makeDomainWithSelfFromArrow(ga)
    sa.subtypeED(fd, gd)
  }
  
  // Checks the return type rule
  def typeSafe(f: Functional, g: Functional): Boolean = {
    if(!lteq(f, g))
      true
    else {
      val fa = makeArrowFromFunctional(f, true).get
      val ga = makeArrowFromFunctional(g, true).get
      val ra = sa.returnUA(fa, ga)
      sa.subtypeUA(fa, ra)
    }
  }
  
  //Checks whether f is the meet of g and h
  def meet(f: Functional, g: Functional, h: Functional)(implicit checkingMethods: Boolean): Boolean = {
    val fa = makeArrowFromFunctional(f, true).get
    val ga = makeArrowFromFunctional(g, true).get
    val ha = makeArrowFromFunctional(h, true).get
    val fd = sa.makeDomainFromArrow(fa)
    val gd = sa.makeDomainFromArrow(ga)
    val hd = sa.makeDomainFromArrow(ha)
    val md = sa.meetED(gd, hd)
    sa.equivalentED(fd, md)
  }
  
  sealed trait COMPARISON_RESULT {}
  case object NO_RELATION extends COMPARISON_RESULT {}
  case object OVERLOADS extends COMPARISON_RESULT {}
  case object NARROWS extends COMPARISON_RESULT {}
  case object JUST_SHADOWS extends COMPARISON_RESULT {}
  
  def compare(f: Functional, g: Functional): COMPARISON_RESULT = (f, g) match {
    case (f: HasSelfType, g: HasSelfType) =>
      val (fPTSS, tFST, fSI) = paramTypeWithoutSelf(f)
      val fST = removeSelf(tFST)
      val (uGPTSS, uGST, gSI) = paramTypeWithoutSelf(g)
      if(fSI != gSI)
        return NO_RELATION
      val (fSPJ, gSPJ) = (f.staticParameters, g.staticParameters)
      val (fSPS, gSPS) = (toList(fSPJ), toList(gSPJ))
      val (fSA, gSA) = (staticParamsToArgs(fSPJ), staticParamsToArgs(gSPJ))
      val (fTA, gTA) = (ta.extend(gSPS, None), ta.extend(fSPS, None))
      if(!staticArgsMatchStaticParams(toList(fSA), gSPS)(fTA) || 
         !staticArgsMatchStaticParams(toList(gSA), fSPS)(gTA))
        return NO_RELATION
      val fSA_for_gSP = new StaticTypeReplacer(fSPJ, gSA)
      val (gPTSS, gST) = (fSA_for_gSP.replaceIn(uGPTSS), fSA_for_gSP.replaceIn(removeSelf(uGST)))
      val (fRT, gRT) = (f.getReturnType.unwrap, fSA_for_gSP.replaceIn(g.getReturnType.unwrap))
      val fST_strictlySub_gST = isTrue(fTA.subtype(fST, gST)) && !isTrue(fTA.subtype(gST,fST))
      val gDTSS_sub_fDTSS = isTrue(fTA.subtype(gPTSS, fPTSS))
      val fDTSS_sub_gDTSS = isTrue(fTA.subtype(fPTSS, gPTSS))
      val fRT_eq_gRT = isTrue(fTA.equivalent(fRT, gRT))
      (fST_strictlySub_gST, gDTSS_sub_fDTSS, fDTSS_sub_gDTSS, fRT_eq_gRT) match {
        case (false, _, _, _) => NO_RELATION
        case (true, true, false, _) => JUST_SHADOWS
        case (true, true, true, false) => NARROWS
        case (true, true, true, true) => OVERLOADS
      }
    case _ =>
      bug("Should only be used on methods and functional methods.")
  }
  
  def narrows(f: Functional, g: Functional) = compare(f, g) match {
    case NARROWS => true
    case _ => false
  }
  
  def shadows(f: Functional, g: Functional) = compare(f, g) match {
    case NO_RELATION => false
    case _ => true
  }
  
  def overloads(f: Functional, g: Functional) = compare(f, g) match {
    case OVERLOADS => true
    case _ => false
  }
  
  private def removeSelf(x: SelfType) = x match {
    case STraitSelfType(_, tt, _) => tt
    case _ => x
  }
  
  /* TODO: REMOVE THE FOLLOWING FUNCTIONS
   * They are currently being used by the code generator in a very undisciplined way
   */
  
  
  // drc, trying to figure out Scala
  def getParamType(f: Functional, i:Int): Type = {
    val fa = makeArrowFromFunctional(f, true).get
    val fd = sa.makeDomainFromArrow(fa)
    val fp = sa.makeParamFromDomain(fd, i)
    // Watch out, do we need to strip self type?
    fp
  }
  
  // Checks when f is more specific than g in a particular parameter.
  // drc, trying to figure out Scala
  def getDomainType(f: Functional): Type = {
    val fa = makeArrowFromFunctional(f, true).get
    val fd = sa.makeDomainFromArrow(fa)
    // Watch out, do we need to strip self type?
    fd
  }
  
  def getNoSelfDomainType(f: Functional): Type = {
    // might be better calling makeArrowWithoutSelfFromFunctional
    val fa = makeArrowFromFunctional(f, true, true).get
    val fd = sa.makeDomainFromArrow(fa)
    fd
  }
  
  def getRangeType(f: Functional): Type = {
    val fa = makeArrowFromFunctional(f, true).get
    val fd = sa.makeRangeFromArrow(fa)
    // Watch out, do we need to strip self type?
    fd
  }
  
  
  // Convenience function when arrows have been extracted from functionals
  // already.
  def lteq(fa: ArrowType, ga: ArrowType): Boolean = {
    val fd = sa.makeDomainWithSelfFromArrow(fa)
    val gd = sa.makeDomainWithSelfFromArrow(ga)
    sa.subtypeED(fd, gd)
  }
  
  // Convenience function when domains have been extracted from arrows
  // already.
  def lteq(fd: Type, gd: Type) = {
      sa.subtypeED(fd, gd)
  }
  
}

