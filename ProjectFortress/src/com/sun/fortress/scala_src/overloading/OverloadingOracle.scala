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
import com.sun.fortress.compiler.Types.ANY
import com.sun.fortress.compiler.Types.BOTTOM
import com.sun.fortress.compiler.Types.OBJECT
import com.sun.fortress.exceptions.InterpreterBug.bug
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.NodeFactory._
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.typechecker.ConstraintFormula
import com.sun.fortress.scala_src.typechecker.CnFalse
import com.sun.fortress.scala_src.typechecker.CnTrue
import com.sun.fortress.scala_src.typechecker.CnAnd
import com.sun.fortress.scala_src.typechecker.CnOr
import com.sun.fortress.scala_src.typechecker.staticenv.KindEnv
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
    val fd = sa.makeDomainFromArrow(fa)
    val gd = sa.makeDomainFromArrow(ga)
    sa.subtypeED(fd, gd)
  }
  
  // Convenience function when arrows have been extracted from functionals
  // already.
  def lteq(fa: ArrowType, ga: ArrowType): Boolean = {
    val fd = sa.makeDomainFromArrow(fa)
    val gd = sa.makeDomainFromArrow(ga)
    sa.subtypeED(fd, gd)
  }
  
  // Convenience function when domains have been extracted from arrows
  // already.
  def lteq(fd: Type, gd: Type) = {
	  sa.subtypeED(fd, gd)
  }
  
  // Checks when f is more specific than g in a particular parameter.
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
  
  def getRangeType(f: Functional): Type = {
    val fa = makeArrowFromFunctional(f, true).get
    val fd = sa.makeRangeFromArrow(fa)
    // Watch out, do we need to strip self type?
    fd
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
  def meet(f: Functional, g: Functional, h: Functional): Boolean = {
    val fa = makeArrowFromFunctional(f, true).get
    val ga = makeArrowFromFunctional(g, true).get
    val ha = makeArrowFromFunctional(h, true).get
    val fd = sa.makeDomainFromArrow(fa)
    val gd = sa.makeDomainFromArrow(ga)
    val hd = sa.makeDomainFromArrow(ha)
    val md = sa.meetED(gd, hd)
    sa.equivalentED(fd, md)
  }
  
  def meet(f: ((JavaList[StaticParam],Type,Type,Option[Int]),_),
           g: ((JavaList[StaticParam],Type,Type,Option[Int]),_),
           h: ((JavaList[StaticParam],Type,Type,Option[Int]),_)): Boolean = {
    val fd = insertStaticParams(f._1._2, toList(f._1._1))
    val gd = insertStaticParams(g._1._2, toList(g._1._1))
    val hd = insertStaticParams(h._1._2, toList(h._1._1))
    val fr = insertStaticParams(f._1._3, toList(f._1._1))
    val gr = insertStaticParams(g._1._3, toList(g._1._1))
    val hr = insertStaticParams(h._1._3, toList(h._1._1))

    sa.equivalentED(fd, sa.meetED(gd, hd)) && sa.subtypeED(fr, sa.meetED(gr, hr))
  }
}

