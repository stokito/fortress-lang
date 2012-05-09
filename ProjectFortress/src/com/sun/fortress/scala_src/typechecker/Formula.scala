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

package com.sun.fortress.scala_src.typechecker

import com.sun.fortress.nodes._

import com.sun.fortress.nodes_util.NodeFactory
import com.sun.fortress.compiler.Types.ANY
import com.sun.fortress.compiler.Types.BOTTOM
import com.sun.fortress.compiler.Types.OBJECT
import com.sun.fortress.exceptions.InterpreterBug.bug
import com.sun.fortress.scala_src.types.TypeAnalyzer
import com.sun.fortress.scala_src.types.{TypeAnalyzerUtil => TAU}
import com.sun.fortress.scala_src.useful.{STypesUtil => TU}

/* CFormulas express inequalities and negations of inequalities involving 
 * inference variables (so far we have type and arg variables)
 */
sealed trait CFormula{}
/* EFormulas express equalities involving inference variables
 */
sealed trait EFormula{}

// The vacuously true formula
case object True extends CFormula with EFormula {
  override def toString = "True"
}

// A contradiction
case object False extends CFormula with EFormula {
  override def toString = "False"
}

// A conjunction of primitive type and operator constraints (one for each inference variables).
case class And(ts: Map[_InferenceVarType, TPrimitive], os: Map[_InferenceVarOp, OPrimitive]) extends CFormula {}

/* A primitive type constraint.
 * So far we have: lower bound, not lower bound, upper bound, not upper bound, excludes, does not exclude
 */
case class TPrimitive(pl: Set[Type], nl: Set[Type], pu: Set[Type], nu: Set[Type], pe: Set[Type], ne: Set[Type]){}

/* A primitive op constraint
 * So far we have equals and not equals
 */
case class OPrimitive(pe: Set[Op], ne: Set[Op])

// A disjunction of conjunctions
case class Or(conjuncts: Set[And]) extends CFormula {}

// A conjunction of equality constraints (represented as cliques)
case class Conjuncts(eq: Set[Set[Type]], ops: Set[Set[Op]]) extends EFormula {}

// A disjunction of conjunctions
case class Disjuncts(es: Set[Conjuncts]) extends EFormula {}

object Formula{

  private val tUnit = TPrimitive(Set(), Set(), Set(), Set(), Set(), Set())
  private val oUnit = OPrimitive(Set(),Set())
  val oEmptySub = oSubstitution(Map()) 
  val tEmptySub = tSubstitution(Map())
  private def oEq(a: Op, b: Op) = a==b
  
  def tSubstitution(tmap: Map[_InferenceVarType, Type]): Type => Type = TU.liftSubstitution(tmap)
  def oSubstitution(omap: Map[_InferenceVarOp, Op]): Op => Op = TU.liftSubstitution(omap)
  def insertOps(omap: Op => Op): Type => Type = TU.liftSubstitution[Op, Op, Type]{case x:Op => omap(x)}
  
  private def merge[S,T](a: Map[S, T], b: Map[S,T], bin: (T,T) => T, unit: T): Map[S, T] =
    Map((a.keySet ++ b.keySet).map(k => 
      (k, bin(a.getOrElse(k, unit), b.getOrElse(k, unit)))).toSeq:_*)
  
  private def tMerge(p: TPrimitive, q:TPrimitive): TPrimitive = {
    val TPrimitive(pl1, nl1, pu1, nu1, pe1, ne1) = p
    val TPrimitive(pl2, nl2, pu2, nu2, pe2, ne2) = q
    TPrimitive(pl1 ++ pl2, nl1 ++ nl2, pu1 ++ pu2, nu1 ++ nu2, pe1 ++ pe2, ne1 ++ ne2)
  }
  private def oMerge(p: OPrimitive, q: OPrimitive): OPrimitive = {
    val OPrimitive(a1, b1) = p
    val OPrimitive(a2, b2) = q
    OPrimitive(a1 ++ a2, b1 ++ b2)
  }
  
  
  def and(cs: Iterable[CFormula])(implicit ta: TypeAnalyzer): CFormula =
    cs.foldLeft(True.asInstanceOf[CFormula])(and)
  
  def and(cs: Iterable[EFormula])(implicit ta: TypeAnalyzer): EFormula =
    cs.foldLeft(True.asInstanceOf[EFormula])(and)
  
  def and(c1: CFormula, c2: CFormula)(implicit ta: TypeAnalyzer): CFormula = (c1, c2) match {
    case (True, _) => reduce(c2)
    case (False, _) => False
    case (And(as, os), And(bs, ps)) =>
      reduce(And(merge(as, bs, tMerge, tUnit), merge(os, ps, oMerge, oUnit)))
    case (Or(cs), Or(ds)) =>
      dis(for(c <- cs; d <- ds) yield and(c,d))
    case (c1: Or, c2: And) => and(c1, Or(Set(c2)))
    case _ => and(c2, c1)
  }
  
  def and(e1: EFormula, e2: EFormula)(implicit ta: TypeAnalyzer): EFormula = (e1, e2) match {
    case (True, _) => reduce(e2)
    case (False, _) => False
    case (Conjuncts(as, os), Conjuncts(bs, ps)) => reduce(Conjuncts(as ++ bs, os ++ ps))
    case (Disjuncts(e1), Disjuncts(e2)) => 
      dis(for(c <- e1; d <- e2) yield and(c,d))
    case (e1: Disjuncts, e2: Conjuncts) => and(e1, Disjuncts(Set(e2)))
    case _ => and(e2, e1)
  }
  
  def or(cs: Iterable[CFormula])(implicit ta: TypeAnalyzer): CFormula =
    cs.foldLeft(False.asInstanceOf[CFormula])(or)
    
  def or(cs: Iterable[EFormula])(implicit ta: TypeAnalyzer): EFormula =
    cs.foldLeft(False.asInstanceOf[EFormula])(or)
    
  def or(c1: CFormula, c2: CFormula)(implicit ta: TypeAnalyzer): CFormula = (c1, c2) match {
    case (True, _) => True
    case (False, _) => reduce(c2)
    case (Or(cs), Or(ds)) => reduce(Or(cs ++ ds))
    case (a: And, b: And) => reduce(Or(Set(a, b)))
    case (a: And, Or(cs)) => reduce(Or(cs ++ Set(a)))
    case _ => or(c2, c1)
  }
  
  def or(e1: EFormula, e2: EFormula)(implicit ta: TypeAnalyzer): EFormula = (e1, e2) match {
    case (True, _) => True
    case (False, _) => reduce(e2)
    case (Disjuncts(e1s), Disjuncts(e2s)) => reduce(Disjuncts(e1s ++ e2s))
    case (a: Conjuncts, b: Conjuncts) => reduce(Disjuncts(Set(a, b)))
    case (a: Conjuncts, Disjuncts(es)) => reduce(Disjuncts(es ++ Set(a)))
    case _ => or(e2, e1)
  }
  
  def implies[T](as: Set[Set[T]], bs: Set[Set[T]], eq: (T, T)=>Boolean): Boolean = {
    bs.forall{b => as.exists(a => b.forall(x => a.exists(eq(x, _))))}
  }
  
  def impliesWithDebug(c1: CFormula, c2: CFormula, debug:Boolean=false)(implicit ta: TypeAnalyzer) =
    imp(reduce(c1), reduce(c2), debug)

  def implies(c1: CFormula, c2: CFormula)(implicit ta: TypeAnalyzer) =
    imp(reduce(c1), reduce(c2))

  private def imp(c1: CFormula, c2: CFormula, debug:Boolean=false)(implicit ta: TypeAnalyzer): Boolean = {
    if (debug)
      println("imp: c1 = " + c1 + ", c2 = " + c2)
    val result = (c1, c2) match {
  
    case (False, _) => true
    case (_, False) => false
    case (_, True) => true
    case (True, _) => false
    // Checks whether every constraint in c2 is in c1
    case (And(as, os), And(bs, ps)) => {
       val a1 = forall(as, bs, tImplies, tUnit)
       val a2 = forall(ps, os, oImplies, oUnit)
       if (debug)
         println("a1, a2 = " + a1 + ", " + a2)
       a1 && a2
    }
    case (c1, Or(cs)) => cs.exists(imp(c1, _))
    case (Or(cs), c2) => cs.forall(imp(_, c2))
    }
    if (debug)
      println("imp: result = " + result)
    result
  }

  private def tImplies(p: TPrimitive, q: TPrimitive)(implicit ta: TypeAnalyzer): Boolean = {
    val TPrimitive(pl1, nl1, pu1, nu1, pe1, ne1) = p
    val TPrimitive(pl2, nl2, pu2, nu2, pe2, ne2) = q
    val pls = ta.lteq(ta.join(pl2), ta.join(pl1))
    val nls = nl2.forall(n2 => nl1.exists(ta.lteq(n2,_)) || pe1.exists(p1 => ta.definitelyExcludes(p1, n2)) || pu1.exists(p1 => isFalse(ta.subtype(n2, p1))))
    val pus = ta.lteq(ta.meet(pu1), ta.meet(pu2))
    val nus = nu2.forall(n2 => nu1.exists(ta.lteq(_,n2)) || pe1.exists(p1 => ta.definitelyExcludes(p1, n2)) || pl1.exists(p1 => isFalse(ta.subtype(p1, n2))))
//         // ToDo: Figure out whether using a recursive implies here will terminate
//         val pes = pe2.forall(e2 => pe1.exists(ta.lteq(e2, _)) || pu1.exists(t1 => imp(c1, ta.excludes(e2, t1))))
    // Actually, it won't.  So we need to make sure that c1 is also decomposed.  So we use a specialized imp called primImp.
    val pes = pe2.forall(e2 => pe1.exists(ta.lteq(e2, _)) || pu1.exists(t1 => primImp(p, ta.excludes(e2, t1))))
    val nes = ne2.forall(n2 => pu1.exists(ta.lteq(_, n2)) || pl1.exists(p1 => isFalse(ta.excludes(p1, n2))))
    pls && nls && pus && nus && pes && nes
 }

  private def primImp(p: TPrimitive, c2: CFormula)(implicit ta: TypeAnalyzer): Boolean = c2 match {
    case (False) => false
    case (True) => true
    case (And(bs, ps)) => bs.values.forall(tImplies(p, _))
    case (Or(cs)) => cs.exists(primImp(p, _))
  }

 private def oImplies(p: OPrimitive, q: OPrimitive): Boolean = {
   val OPrimitive(p1s, n1s) = p
   val OPrimitive(p2s, n2s) = q
   p2s.forall(p2 => p1s.exists(_==p2)) && n2s.forall(n2 => n1s.exists(_==n2))
 }

  private var stackLevel = 0

  private def forall[S, T](as: Map[S, T], bs: Map[S,T], pred: (T, T) => Boolean, unit: T): Boolean = {
    stackLevel += 1
    if (stackLevel == 10) bug("forall stack overflow")
    val result = (as.keySet ++ bs.keySet).forall(k => pred(as.getOrElse(k, unit), bs.getOrElse(k, unit)))
    stackLevel -= 1
    result
  }
  
  def implies(e1: EFormula, e2: EFormula)(implicit ta: TypeAnalyzer) =
    imp(reduce(e1), reduce(e2))
    
  private def imp(e1: EFormula, e2: EFormula)(implicit ta: TypeAnalyzer): Boolean = (e1, e2) match {
    case (False, _) => true
    case (_, False) => false
    case (_, True) => true
    case (True, _) => false
    case (Conjuncts(as, os), Conjuncts(bs, ps)) =>
      implies(as, bs, ta.equiv) && implies(os, ps, oEq)
    case (_, Disjuncts(es)) => es.exists(imp(e1, _))
    case (Disjuncts(es), _) => es.forall(imp(_, e2))
  }
  
  def equivalent(c1: CFormula, c2: CFormula)(implicit ta: TypeAnalyzer): Boolean = {
    val rc1 = reduce(c1)
    val rc2 = reduce(c2)
    imp(rc1, rc2) && imp(rc2, rc1)
  }
  
  def equivalent(e1: EFormula, e2: EFormula)(implicit ta: TypeAnalyzer): Boolean = {
    val re1 = reduce(e1)
    val re2 = reduce(e2)
    imp(re1, re2) && imp(re2, re1)
  }
  
  def isFalse(c: CFormula)(implicit ta: TypeAnalyzer): Boolean = implies(c, False)
  
  def isFalse(e: EFormula)(implicit ta: TypeAnalyzer): Boolean = implies(e, False)
  
  def isTrue(c: CFormula)(implicit ta: TypeAnalyzer): Boolean = implies(True, c)
  
  def isTrue(e: EFormula)(implicit ta: TypeAnalyzer): Boolean = implies(True, e)
  
  private def removeDups[T](s: Set[T], eq: (T, T)=> Boolean) = 
    s.foldLeft(Set[T]()){(ns, i) =>
      if(ns.exists(eq(i, _)))
        ns
      else
        ns ++ Set(i)
    }
  
  private def maximalCFormulas(s: Set[CFormula])(implicit ta: TypeAnalyzer) = {
    val nds = removeDups(s, (a: CFormula, b: CFormula) => equivalent(a, b))
    nds.filterNot{c => nds.exists(d => implies(c, d) && !equivalent(c,d))}
  }

  def reduce(c: CFormula)(implicit ta: TypeAnalyzer): CFormula =  c match {
    case And(ts, os) =>
      def tSimplify(ip: (_InferenceVarType, TPrimitive)): Option[(_InferenceVarType, TPrimitive)] = ip match {
        // catch trivial contradictions 
        case (i, TPrimitive(pl,nl,pu,nu,pe,ne)) 
          if (nl.contains(i) || nu.contains(i) || pe.contains(i) || 
              nl.contains(BOTTOM)|| ne.contains(BOTTOM) || nu.contains(ANY)) =>
            None
        case (i, TPrimitive(pl,nl,pu,nu,pe,ne))
          if (pe.contains(ANY)) =>
            tSimplify(i, TPrimitive(pl, nl, Set(BOTTOM), nu, Set(), ne))
        case (i, TPrimitive(pl,nl,pu,nu,pe,ne)) =>
          Some((i, TPrimitive(maxTypes(pl.filter(_!=i)), minTypes(nl) ++ nl.filter(_==ANY), 
                                      minTypes(pu.filter(_!=i)) , maxTypes(nu) ++ nu.filter(_==BOTTOM), 
                                      maxTypes(pe), minTypes(ne) ++ ne.filter(_==ANY))))
      }
      // Comprises means there are more of these
      def tContradiction(ip: (_InferenceVarType, TPrimitive)) = {
        val TPrimitive(pl,nl,pu,nu,pe,ne) = ip._2
        if(isFalse(ta.subtype(ta.join(pl), ta.meet(pu))) ||
           nl.exists(n => pl.exists(ta.lteq(n, _))) ||
           nu.exists(n => pu.exists(ta.lteq(_, n))) ||
           pe.exists(e => pu.exists(ta.lteq(_,e))) ||
           pl.exists(l => pe.exists(e => isFalse(ta.excludes(l,e)))) ||
           ne.exists(n => pe.exists(ta.lteq(n, _))) ||
           ne.exists(n => pu.exists(ta.definitelyExcludes(n, _))))
          None
        else
          Some(ip)
      }
      def tRedundant(ip: (_InferenceVarType, TPrimitive)) = {
        val TPrimitive(pl,nl,pu,nu,pe,ne) = ip._2
        Some((ip._1, TPrimitive(pl,
                       nl.filterNot(n => pu.exists(p => isFalse(ta.subtype(n, p)))).
                          filterNot(n => pe.exists(ta.definitelyExcludes(n, _))),
                       pu,
                       nu.filterNot(n => pl.exists(p => isFalse(ta.subtype(p, n)))).
                          filterNot(n => pe.exists(ta.definitelyExcludes(n, _))),
                       pe.filterNot(p => pu.exists(ta.definitelyExcludes(p, _))),
                       ne.filterNot(n => pu.exists(ta.lteq(_, n))).
                          filterNot(n => pl.exists(p => isFalse(ta.excludes(p, n)))))))
      }
      def tTrivial(ip: (_InferenceVarType, TPrimitive)) = {
        val TPrimitive(pl,nl,pu,nu,pe,ne) = ip._2
        pl.isEmpty && nl.isEmpty && pu.isEmpty && nu.isEmpty && pe.isEmpty && ne.isEmpty
      }
      def oSimplify(ip: (_InferenceVarOp, OPrimitive)) = {
        val (i, OPrimitive(ps, ns)) = ip
        val sps = removeDups(ps - i, oEq)
        val sns = removeDups(ns, oEq)
        (i, OPrimitive(sps, sns))
      }
      def oContradiction(ip: (_InferenceVarOp, OPrimitive)) = {
        val (i, OPrimitive(ps, ns)) = ip
        if(ns.contains(i) || ps.exists(p => ns.contains(p)) || ps.filterNot(_.isInstanceOf[_InferenceVarOp]).size > 1)
          None
        else
          Some(ip)
      }
      def oTrivial(ip: (_InferenceVarOp, OPrimitive)) = {
        val (i, OPrimitive(ps, ns)) = ip
        ps.isEmpty && ns.isEmpty
      }
      val nts = ts.map(tSimplify).map(_.flatMap(tContradiction)).map(_.flatMap(tRedundant)).map(_.getOrElse(return False)).filterNot(tTrivial)
      val nos = os.map(oSimplify).map(oContradiction).map(_.getOrElse(return False)).filterNot(oTrivial)
      if (nts.isEmpty && nos.isEmpty)
        return True
      else
        And(Map(nts.toSeq: _*), Map(nos.toSeq: _*)) 
    case Or(cs) =>
      val rcs = cs.map(reduce(_))
      if(rcs.contains(True))
        True
      else {
        val ncs = maximalCFormulas(rcs)
        ncs.size match {
          case 0 => False
          case 1 => ncs.head
          case _ => Or(ncs.asInstanceOf[Set[And]])
        }
      }
    case _ => c
  }
  
  private def minTypes(s: Set[Type])(implicit ta: TypeAnalyzer) =
      TU.conjuncts(ta.meet(s))
      
  private def maxTypes(s: Set[Type])(implicit ta: TypeAnalyzer) =
      TU.disjuncts(ta.join(s))
  
  def reduce(e: EFormula)(implicit ta: TypeAnalyzer): EFormula = e match {
    case Conjuncts(as, os) => 
      val ras = reduce(as, ta.equiv).map{x => removeDups(x, (a: Type, b: Type) => ta.equiv(a, b))}.
        filter{_.size > 1}
      val ros = reduce(os, (a: Op, b: Op) => a==b).map{
        x => removeDups(x, (a: Op, b: Op) => a==b)}.
        filter{_.size > 1}
      if(ras.isEmpty && ros.isEmpty)
        True
      else if(isContradictory(ras, (s: Type, t: Type) => isFalse(ta.equivalent(s,t)))
              || isContradictory(ros, definitelyNotEqual))
        False
      else
        Conjuncts(ras, ros)  
    case True => True
    case False => False
    case Disjuncts(es) => 
      val nes = es.map(reduce).filter{_.isInstanceOf[Conjuncts]}
      nes.size match {
        case 0 => False
        case 1 => nes.head
        case _ => Disjuncts(nes.asInstanceOf[Set[Conjuncts]])
      }
  }
  
  private def definitelyNotEqual(a: Op, b: Op): Boolean = (a, b) match {
    case (_: _InferenceVarOp, _) => false
    case (_, _:_InferenceVarOp) => false
    case _ => a!=b
  }
  
  private def isContradictory[T](as: Set[Set[T]], neq: (T,T) => Boolean): Boolean =
    !as.isEmpty && as.forall{a =>  !a.isEmpty && a.tail.foldLeft((a.head, false)){case ((s, b), t) => (t, neq(s,t) || b)}._2}
  /*
   * Given a set of sets of Ts that are supposed to represent
   * equivalence classes, this method merges together any classes
   * that share a T to give a reduced set of sets of Ts.
   */
  
  private def reduce[T](es: Set[Set[T]], eq: (T,T)=>Boolean): Set[Set[T]] = {
    def binReduce(e1: Set[T], es: Set[Set[T]]): Set[Set[T]] = 
      if(es.isEmpty)
        Set(e1)
      else {
        // Since es is reduced there will be 0 or 1 matches
        val e2 = es.filter(e => e.exists(a => e1.exists(eq(_, a)))).headOption
        val nes = es.filterNot(e => e.exists(a => e1.exists(eq(_, a))))
        Set(e2.getOrElse(Set()) ++ e1) ++ nes
      }
    es.foldLeft(Set[Set[T]]()){(a, b) => binReduce(b, a)}
  }
  
  /*
   * Solves a constraint formula by first unifying away all the equality constraints
   * and then using Dan's simple algorithm from "Java Type Inference is Broken." Dan's
   * algorithm does not do very well when inference variables have bounds that contain
   * other inference variables such as those that arise from the self type idiom. At some
   * point it would be a good idea to replace this with a smarter algorithm.
   */
  
  def solve(c: CFormula)(implicit ta: TypeAnalyzer): Option[(Type => Type, Op => Op)] = 
    slv(reduce(c))
  
  private def slv(c: CFormula)(implicit ta: TypeAnalyzer): Option[(Type => Type, Op => Op)] = c match {
    // False cannot be solved
    case False => None
    // True has the trivial solution
    case True => Some((tEmptySub, oEmptySub))
    // We solve an Or by solving one of its branches
    case Or(cs) => 
      for (e <- cs) {
        val se = slv(e)
        if(se.isDefined)
          return se
      }
      None
    /* To solve an And:
     * 1) Factor the constraint formula into the conjunction of an equality constraint E
     * and additional constraints I
     * 2) Find the principal unifier U of E
     * 3) Apply U to I to get I'
     * TODO: We can solve more constraints if we iterate 1), 2) 3) until we hit a fixed point but we must be careful to ensure termination
     * 4) Solve I' to get a substitution S
     * 5) Return S composed with U
     */
    case _:And => 
      val (newCon, tUnifier, oUnifier) = unify(c).getOrElse(return None)
      newCon match {
        case True => Some((tUnifier, oUnifier))
        case False => None
        case nc@And(ts, os) =>
//	  println("nc: " + nc)
          // Operators only have equality constraints so they should always be solved by unification
          assert(os.isEmpty)
          val sub = TU.killIvars compose 
            tSubstitution(ts.map{
              case (k, p@TPrimitive(pl,nl,pu,nu,pe,ne)) => {
                var uni = ta.join(pl.filterNot(TU.hasInferenceVars))
                // Heuristic extension to Dan Smith's algorithm:
                // If there is a single lower bound and it is a trait type, 
                // try all of its ancestors, searching for one that satisfies
                // all upper bounds.
//		println("uni: " + uni)
//		println("pl: " + pl)
                if (pl.size == 1) {
                  pl.head match {
                    case tt:TraitType =>
                      for (a <- (ta.ancestors(tt) ++ List(uni)).toList.sortWith((a,b) => isTrue(ta.subtype(b,a)))) {
		        val proposedMap = cMap(nc, TU.killIvars compose tSubstitution(Map((k,p)).map(_=>(k,a))))
//			println("For ancestor " + a + " proposed map is " + proposedMap)
                        if (isTrue(proposedMap)) {
                          uni = a 
                        }
                      }
                    case _ => 0
                  }
                }
//                println("slv: new (k->uni) = " + k + "->" + uni)
                (k, uni)
              }
            })
	  val newMap = cMap(newCon, sub)
//	  println("newMap: " + newMap)
          if(isTrue(newMap))
            Some((sub compose tUnifier, oUnifier))
          else {
//               println("sub:" + sub + " on formula " + c)
               None
          }
        // Should never occur
        case _:Or => bug("Applied a substitution to an And and got an Or")
      }      
  }
  
  def unify(c: CFormula)(implicit ta: TypeAnalyzer): Option[(CFormula, Type => Type, Op => Op)] = {
      val eq = getEquality(c)
//      println("Unify " + c + " produces " + eq)
      un(eq).map{case (ts, os) => (cMap(c, ts, os), ts, os)}
  }
  
  def unifyWithDebug(c: CFormula, debug:Boolean=false)(implicit ta: TypeAnalyzer): Option[(CFormula, Type => Type, Op => Op)] = {
      val eq = getEquality(c)
      if (debug)
          println("Unify " + c + " produces " + eq)
      un(eq).map{case (ts, os) => (cMapWithDebug(c, ts, os, debug), ts, os)}
  }
  
  /*
   * This method factors all of the type equalities out of an inequality constraint
   * since they are better solved through unification than the algorithm in Dan Smith's
   * "Java Type Inference is Broken" paper.
   */
  
  private def getEquality(c: CFormula)(implicit ta: TypeAnalyzer): EFormula = reduce(c) match {
    case False => False
    case True => True
    case And(ts, os) =>
      val teq = getCliques[Type, TPrimitive, _InferenceVarType](ts, 
          {case TPrimitive(pl,nl,pu,nu,pe,ne) => pl.filter(a => pu.exists(b => ta.equiv(a, b))) ++ pu.filter(a => pl.exists(b => ta.equiv(a, b)))})
      val oeq = getCliques[Op, OPrimitive, _InferenceVarOp](os,
          {case OPrimitive(ps, ns) => ps})
      reduce(Conjuncts(teq, oeq))
    case Or(cs) => 
      val eq = cs.map(getEquality)
      dis(eq)
  }
  
  private def getCliques[S,T, U <: S](xs: Map[U, T], makeClique: T => Set[S]): Set[Set[S]] = {
    xs.map{case (a, b) => makeClique(b) ++ Set(a)}.toSet
  }
  
  private def dis(eq: Set[EFormula])(implicit ta: TypeAnalyzer): EFormula = {
    if (eq.exists(isTrue(_)))
      True
    else
      reduce(Disjuncts(eq.filter(_.isInstanceOf[Conjuncts]).asInstanceOf[Set[Conjuncts]]))
  }
  
  private def dis(cs: Set[CFormula])(implicit ta: TypeAnalyzer): CFormula = {
    if(cs.exists(isTrue(_)))
      True
    else
      reduce(Or(cs.filter(_.isInstanceOf[And]).asInstanceOf[Set[And]]))
  }
  
  /*
   * This method should take an equality formula and produce
   * the principal unifier.
   */
  
  
  def unify(e: EFormula)(implicit ta: TypeAnalyzer): Option[(Type => Type, Op => Op)] =
    un(reduce(e))
  
  private def un(e: EFormula)(implicit ta: TypeAnalyzer): Option[(Type => Type, Op => Op)] = { // println("un of " + e)
   e match {
    case False => None
    case True => Some((tEmptySub, oEmptySub))
    case e@Conjuncts(teq, oeq) =>
      // Makes sure we don't have to use fixed point types
      def tError(x: (Set[_InferenceVarType], Set[Type])) = x match {
        case (a, b) => b.flatMap(TU.getInferenceVars).exists(a.contains(_))
      }
      // Takes a set of more than one inference variable and creates a substitution to unify them
      def tMake(x: Set[_InferenceVarType]) = tSubstitution({ val result = Map(x.tail.map((x.head, _)).toSeq:_*); // println("tMake subst = " + result);
                                                             result })
      val (mTSub, tSplit, tVars, tReg) = makeSub(teq, tError, tMake).getOrElse(return None)
      // No errors possible
      def oError(x: (Set[_InferenceVarOp], Set[Op])) = false
      def oMake(x: Set[_InferenceVarOp]) = oSubstitution(Map(x.tail.map((x.head, _)).toSeq:_*)) 
      val (mOSub, oSplit, oVars, oReg) = makeSub(oeq, oError, oMake).getOrElse(return None)
      // Combine op and type substitutions
      val mTOSub = (mTSub, mOSub) match {
        case (Some(tSub), Some(oSub)) => Some((insertOps(oSub) compose tSub, oSub))
        case (Some(tSub), None) => Some((tSub, oEmptySub))
        case (None, Some(oSub)) => Some((insertOps(oSub), oSub))
        case (None, None) => None
      }
      mTOSub match{
        case Some((tSub, oSub)) =>
	  val eResult = eMap(e, tSub, oSub)
//	  println("un recursion 1: eMap produces " + eResult)
          // If there were any inference variables to be unified, then recurse
          un(eResult).map{case (t, o) => (t compose tSub, o compose oSub)}
        case None=>
	  /* Gets all equivalence classes with more than two non inference variable types and computes the 
	   * constraints under which they are equivalent.
	   */
	  val tNewCons = tReg.filter{_.size > 1}.map{e => e.tail.foldLeft((e.head, True.asInstanceOf[EFormula]))
	    {case ((s, c), t) => (t, and(c, getEquality(ta.equivalent(s, t))))}._2}
	  if(!tNewCons.isEmpty) {
	    // If there were any new constraints, then recurse
	    val tNewCon = and(tNewCons)
	    val tOrig = Conjuncts(tSplit.map{case (iv, niv) => iv ++ Set(niv.head)}, oeq)
	    un(and(tNewCon, tOrig))
	  } else {
	    /* Now each equivalence class must consist of one inference variable 
	     * and one non inference variable. Unify them.
	     */
	    val tSub = tSubstitution(Map(tSplit.map{case (iv, ts) => (iv.head, ts.head)}.toSeq:_*))
	    val oSub = oSubstitution(Map(oSplit.map{case (iv, ts) => (iv.head, ts.head)}.toSeq:_*))
	    Some((insertOps(oSub) compose tSub, oSub))
          }
      }
    case Disjuncts(es) =>
      for(e <- es) {
        val solved = un(e)
        if(solved.isDefined) return solved
      }
      None
  } }
  
  // Have to use a manifest due to erasure
  private def makeSub[T, U <: T](xss: Set[Set[T]], error: ((Set[U], Set[T])) => Boolean , makeS: Set[U] => (T => T))(implicit m: scala.reflect.Manifest[U]): Option[(Option[T => T], Set[(Set[U], Set[T])], Set[Set[U]], Set[Set[T]])] = {
    val split = xss.map{_.partition(m.erasure.isInstance(_))}.asInstanceOf[Set[(Set[U], Set[T])]]
    if(split.exists(error))
      None
    else {
      val (us, ts) = split.unzip
      Some(us.filter{_.size > 1}.map(makeS).reduceRightOption((a, b) => a compose b), split, us, ts)
    }
  }
  
  def cMap(form: CFormula, tSub: Type => Type = tEmptySub, oSub: Op => Op = oEmptySub)(implicit ta: TypeAnalyzer): CFormula = 
    cMapWithDebug(form, tSub, oSub, false)
    
  def cMapWithDebug(form: CFormula, tSub: Type => Type = tEmptySub, oSub: Op => Op = oEmptySub, debug:Boolean)(implicit ta: TypeAnalyzer): CFormula =
  {
    if (debug)
      println("cMap("+form+", "+tSub+", "+oSub+")")
    form match {
    case And(ts, os) =>
      val tForm = and(ts.map{
        case (k, TPrimitive(pl,nl,pu,nu,pe,ne)) =>
          val sk = tSub(k)
  	  if (debug)
  	    println("cMap substitutes " + sk + " for " + k)
	  val plf = ta.subtype(tSub(ta.join(pl)), sk)
	  val nlf = and(nl.map(t => ta.notSubtype(tSub(t),sk)))
	  val puf = ta.subtype(sk, tSub(ta.meet(pu)))
	  val nuf = and(nu.map(t => ta.notSubtype(sk, tSub(t))))
	  val pef = and(pe.map(t => ta.excludes(sk,tSub(t))))
	  val nef = and(ne.map(t => ta.notExcludes(sk, tSub(t))))
	  if (debug)
	    println("cMap produces AND(" + plf + "," + nlf + "," + puf + "," + nuf + "," + pef + "," + nef + ")")
          and(plf, and(nlf, and(puf, and(nuf, and(pef, nef)))))})
      val oForm = and(os.map{
        case (k, OPrimitive(po, no)) =>
          val sk = oSub(k)
          and(and(po.map(ta.equivalent(sk, _))), and(no.map(ta.notEquivalent(sk, _))))
      })
      and(tForm, oForm)
    case Or(cs) => dis(cs.map(cMap(_, tSub, oSub)))
    case _ => form
  }
  }
  def eMap(form: EFormula, tSub: Type => Type = tEmptySub, oSub: Op => Op = oEmptySub)(implicit ta: TypeAnalyzer): EFormula = form match {
    case Conjuncts(ts, os) => reduce(Conjuncts(ts.map(_.map(tSub)), os.map(_.map(oSub))))
    case Disjuncts(es) => dis(es.map(eMap(_, tSub, oSub)))
    case _ => form
  }
  
  def negate(c: CFormula)(implicit ta: TypeAnalyzer): CFormula = neg(reduce(c))
  
  def neg(c: CFormula)(implicit ta: TypeAnalyzer): CFormula = c match {
    case True => False
    case False => True
    case Or(cs) => and(cs.map(negate))
    case And(ts, os) =>
      def tNeg(ip: (_InferenceVarType, TPrimitive)): CFormula = ip match {
        case (i, TPrimitive(pl, nl, pu, nu, pe, ne)) =>
          or(or(pl.map(notLowerBound(i, _))), or(
             or(nl.map(lowerBound(i, _))), or(
             or(pu.map(notUpperBound(i, _))), or(
             or(nu.map(upperBound(i, _))), or(
             or(pe.map(notExclusion(i, _))),
             or(ne.map(exclusion(i, _))))))))
      }
      def oNeg(ip: (_InferenceVarOp, OPrimitive)): CFormula = ip match {
        case (i, OPrimitive(po, no)) =>
          or(or(po.map(oNotEquivalent(i, _))), or(no.map(oEquivalent(i, _))))
      }
      or(or(ts.map(tNeg)), or(os.map(oNeg)))
  }
    
  def upperBound(i: _InferenceVarType, t: Type): CFormula =
    And(Map(i -> TPrimitive(Set(), Set(), Set(t), Set(), Set(), Set())), Map())
  
  def notUpperBound(i: _InferenceVarType, t: Type): CFormula = 
    And(Map(i -> TPrimitive(Set(), Set(), Set(), Set(t), Set(), Set())), Map())
  
  def lowerBound(i: _InferenceVarType, t: Type): CFormula =
    And(Map(i -> TPrimitive(Set(t), Set(), Set(), Set(), Set(), Set())), Map())
    
  def notLowerBound(i: _InferenceVarType, t: Type): CFormula =
    And(Map(i -> TPrimitive(Set(), Set(t), Set(), Set(), Set(), Set())), Map())
    
  def exclusion(i: _InferenceVarType, t: Type): CFormula = 
    And(Map(i -> TPrimitive(Set(), Set(), Set(), Set(), Set(t), Set())), Map())
  
  def notExclusion(i: _InferenceVarType, t: Type): CFormula = 
    And(Map(i -> TPrimitive(Set(), Set(), Set(), Set(), Set(), Set(t))), Map())
  
  def oNotEquivalent(i: _InferenceVarOp, o: Op): CFormula = 
    And(Map(), Map(i -> OPrimitive(Set(), Set(o))))
  
  def oEquivalent(i: _InferenceVarOp, o: Op): CFormula = 
    And(Map(), Map(i -> OPrimitive(Set(o), Set())))

  def fromBoolean(x: Boolean) = if (x) True else False
  
}
