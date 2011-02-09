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

sealed trait CFormula{}
sealed trait EFormula{}

case object True extends CFormula with EFormula {
  override def toString = "True"
}

case object False extends CFormula with EFormula {
  override def toString = "False"
}

case class And(cnjcts: Map[_InferenceVarType, Primitive]) extends CFormula {}


case class Primitive(pl: Set[Type], nl: Set[Type], pu: Set[Type], nu: Set[Type], pe: Set[Type], ne: Set[Type]){}
                    
case class Or(conjuncts: Set[And]) extends CFormula {}

case class Conjuncts(eq: Set[Set[Type]]) extends EFormula {}

case class Disjuncts(es: Set[Conjuncts]) extends EFormula {}

case class Substitution(m: Map[_InferenceVarType, Type]) extends (Type => Type) {
  // Cache the lifted type substitution.
  protected val liftedSubstitution: Type => Type = TU.liftTypeSubstitution(m)
  override def apply(t: Type): Type = liftedSubstitution(t)
}

object Formula{

  private def merge(a: Map[_InferenceVarType, Primitive],
                    b: Map[_InferenceVarType, Primitive]): Map[_InferenceVarType, Primitive] =
    Map((a.keySet ++ b.keySet).map(k => (k, merge(get(k, a), get(k, b)))).toSeq:_*)
  
  private def merge(p: Primitive, q: Primitive): Primitive = {
    val Primitive(pl1, nl1, pu1, nu1, pe1, ne1) = p
    val Primitive(pl2, nl2, pu2, nu2, pe2, ne2) = q
    Primitive(pl1 ++ pl2, nl1 ++ nl2, pu1 ++ pu2, nu1 ++ nu2, pe1 ++ pe2, ne1 ++ ne2)
  }
  
  private def get(k: _InferenceVarType, m: Map[_InferenceVarType, Primitive]) = 
    m.getOrElse(k, Primitive(Set(), Set(), Set(), Set(), Set(), Set()))
   
  def and(cs: Iterable[CFormula])(implicit ta: TypeAnalyzer): CFormula =
    cs.foldLeft(True.asInstanceOf[CFormula])(and)
  
  def and(cs: Iterable[EFormula])(implicit ta: TypeAnalyzer): EFormula =
    cs.foldLeft(True.asInstanceOf[EFormula])(and)
  
  def and(c1: CFormula, c2: CFormula)(implicit ta: TypeAnalyzer): CFormula = (c1, c2) match {
    case (True, _) => reduce(c2)
    case (False, _) => False
    case (And(ps), And(qs)) => reduce(And(merge(ps, qs)))
    case (Or(cs), Or(ds)) =>
      dis(for(c <- cs; d <- ds) yield and(c,d))
    case (c1: Or, c2: And) => and(c1, Or(Set(c2)))
    case _ => and(c2, c1)
  }
  
  def and(e1: EFormula, e2: EFormula)(implicit ta: TypeAnalyzer): EFormula = (e1, e2) match {
    case (True, _) => reduce(e2)
    case (False, _) => False
    case (Conjuncts(eq1), Conjuncts(eq2)) => reduce(Conjuncts(eq1 ++ eq2))
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
  
  def implies(c1: CFormula, c2: CFormula)(implicit ta: TypeAnalyzer) =
    imp(reduce(c1), reduce(c2))
    
  private def imp(c1: CFormula, c2: CFormula)(implicit ta: TypeAnalyzer): Boolean = (c1, c2) match {
    case (False, _) => true
    case (_, False) => false
    case (_, True) => true
    case (True, _) => false
    // Checks whether every constraint in c2 is in c1
    case (And(ps), And(qs)) =>
      def prim(p: Primitive, q: Primitive): Boolean = {
        val Primitive(pl1, nl1, pu1, nu1, pe1, ne1) = p
        val Primitive(pl2, nl2, pu2, nu2, pe2, ne2) = q
        ta.lteq(ta.join(pl2), ta.join(pl1)) &&
        nl2.forall(n2 => nl1.exists(ta.lteq(n2,_)) || pe1.exists(p1 => ta.definitelyExcludes(p1, n2)) || pu1.exists(p1 => isFalse(ta.subtype(n2, p1)))) &&
        ta.lteq(ta.meet(pu1), ta.meet(pu2)) &&
        nu2.forall(n2 => nu1.exists(ta.lteq(_,n2)) || pe1.exists(p1 => ta.definitelyExcludes(p1, n2)) || pl1.exists(p1 => isFalse(ta.subtype(p1, n2)))) &&
        // ToDo: Figure out whether using a recursive implies here will terminate
        pe2.forall(e2 => pe1.exists(ta.lteq(e2, _)) || pu1.exists(t1 => imp(c1, ta.excludes(e2, t1)))) &&
        ne2.forall(n2 => pu1.exists(ta.lteq(_, n2)) || pl1.exists(p1 => isFalse(ta.excludes(p1, n2))))
     }
     (ps.keySet ++ qs.keySet).forall(k => prim(get(k, ps), get(k, qs)))
    case (c1, Or(cs)) => cs.exists(imp(c1, _))
    case (Or(cs), c2) => cs.forall(imp(_, c2))
  }
  
  def implies(e1: EFormula, e2: EFormula)(implicit ta: TypeAnalyzer) =
    imp(reduce(e1), reduce(e2))
    
  private def imp(e1: EFormula, e2: EFormula)(implicit ta: TypeAnalyzer): Boolean = (e1, e2) match {
    case (False, _) => true
    case (_, False) => false
    case (_, True) => true
    case (True, _) => false
    case (Conjuncts(eq1), Conjuncts(eq2)) =>
      eq2.forall{e2 => eq1.exists(e1 => e2.forall(e => e1.exists(ta.equiv(e, _))))}
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
    case And(ps) =>
      def simplify(ip: (_InferenceVarType,Primitive)): Option[(_InferenceVarType, Primitive)] = ip match {
        case (i, Primitive(pl,nl,pu,nu,pe,ne)) 
          if (nl.contains(i) || nu.contains(i) || pe.contains(i) || 
              nl.contains(BOTTOM)|| ne.contains(BOTTOM) || nu.contains(ANY)) =>
            None
        case (i, Primitive(pl,nl,pu,nu,pe,ne))
          if (pe.contains(ANY)) =>
            simplify(i, Primitive(pl, nl, Set(BOTTOM), nu, Set(), ne))
        case (i, Primitive(pl,nl,pu,nu,pe,ne)) =>
          Some((i, Primitive(maxTypes(pl.filter(_!=i)), minTypes(nl) ++ nl.filter(_==ANY), 
                                      minTypes(pu.filter(_!=i)) , maxTypes(nu) ++ nu.filter(_==BOTTOM), 
                                      maxTypes(pe), minTypes(ne) ++ ne.filter(_==ANY))))
      }
      // Comprises means there are more of these
      def contradiction(ip: (_InferenceVarType, Primitive)) = {
        val Primitive(pl,nl,pu,nu,pe,ne) = ip._2
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
      def redundant(ip: (_InferenceVarType, Primitive)) = {
        val Primitive(pl,nl,pu,nu,pe,ne) = ip._2
        Some((ip._1, Primitive(pl,
                      nl.filterNot(n => pu.exists(p => isFalse(ta.subtype(n, p)))).
                         filterNot(n => pe.exists(ta.definitelyExcludes(n, _))),
                      pu,
                      nu.filterNot(n => pl.exists(p => isFalse(ta.subtype(p, n)))).
                         filterNot(n => pe.exists(ta.definitelyExcludes(n, _))),
                      pe.filterNot(p => pu.exists(ta.definitelyExcludes(p, _))),
                      ne.filterNot(n => pu.exists(ta.lteq(_, n))).
                         filterNot(n => pl.exists(p => isFalse(ta.excludes(p, n)))))))
      }
      def trivial(ip: (_InferenceVarType, Primitive)) = {
        val Primitive(pl,nl,pu,nu,pe,ne) = ip._2
        pl.isEmpty && nl.isEmpty && pu.isEmpty && nu.isEmpty && pe.isEmpty && ne.isEmpty
      }
      val sps = ps.map(c => simplify(c))
      val cps = sps.map(_.flatMap(contradiction))
      val rps = cps.map(_.flatMap(redundant))
      val nps = rps.map(_.getOrElse(return False)).filterNot(trivial)
      if (nps.isEmpty)
        return True
      else
        And(Map(nps.toSeq: _*)) 
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
    case Conjuncts(eq) => 
      def isContradictory(eq: Set[Set[Type]])(implicit ta: TypeAnalyzer) =
        eq.forall{e =>  !e.isEmpty && e.tail.foldLeft((e.head, false)){ case ((s, b), t) =>
                          (t, isFalse(ta.equivalent(s,t)) || b)}._2
      }
      val req = merge(eq).map{e => removeDups(e, (a: Type, b: Type) => ta.equiv(a, b))}.
        filter{_.size > 1}
      if(req.isEmpty)
        True
      else if(isContradictory(req))
        False
      else
        Conjuncts(req)  
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

  /*
   * Given a set of sets of types that are supposed to represent
   * equivalence classes, this method merges together any classes
   * that share a type to give a reduced set of sets of types.
   */
  
  private def merge(es: Set[Set[Type]])(implicit ta: TypeAnalyzer): Set[Set[Type]] = {
    def binMerge(e1: Set[Type], es: Set[Set[Type]])(implicit ta: TypeAnalyzer): Set[Set[Type]] =
      if(es.isEmpty)
        Set(e1)
      else {
        // Since es is reduced there will be 0 or 1 matches
        val e2 = es.filter(e => e.exists(a => e1.exists(ta.equiv(_, a)))).headOption
        val nes = es.filterNot(e => e.exists(a => e1.exists(ta.equiv(_, a))))
        Set(e2.getOrElse(Set()) ++ e1) ++ nes
      }
    es.foldLeft(Set[Set[Type]]()){(a, b) => binMerge(b, a)}
  }
  
  /*
   * Solves a constraint formula by first unifying away all the equality constraints
   * and then using Dan's simple algorithm from "Java Type Inference is Broken." Dan's
   * algorithm does not do very well when inference variables have bounds that contain
   * other inference variables such as those that arise from the self type idiom. At some
   * point it would be a good idea to replace this with a smarter algorithm.
   */
  
  def solve(c: CFormula)(implicit ta: TypeAnalyzer): Option[Type => Type] = 
    slv(reduce(c))
  
  private def slv(c: CFormula)(implicit ta: TypeAnalyzer): Option[Type => Type] = c match {
    // False cannot be solved
    case False => None
    // True has the trivial solution
    case True => Some(Substitution(Map()))
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
      val (nc, unifier) = unify(c).getOrElse(return None)
      // println("nc: " + nc)
      nc match {
        case True => Some(unifier)
        case False => None
        case nc@And(ps) =>
          val sub = TU.killIvars compose 
          	Substitution(ps.map{
          		case (k, p@Primitive(pl,nl,pu,nu,pe,ne)) => {
          			var uni = ta.join(pl.filterNot(TU.hasInferenceVars))
          			// Heuristic extension to Dan Smith's algorithm:
          			// If there is a single lower bound and it is a trait type, 
          			// try all of its ancestors, searching for one that satisfies
          			// all upper bounds.
          			if (pl.size == 1) {
          				pl.head match {
          					case tt:TraitType => {
						     	// println("tt: " + tt)
          						for (a <- (ta.ancestors(tt) ++ List(uni)).toList.sortWith((a,b) => isTrue(ta.subtype(b,a)))) {
							       // println("  a: " + a)
							       if (isTrue(map(nc, TU.killIvars compose Substitution(Map((k,p)).map(_=>(k,a)))))) { 
							       	  // println ("uni set to " + a)
							       	  uni = a 
								}
							       // if (isTrue(map((k,p), TU.killIvars compose Substitution(Map((k,p)).map(_=>(k,a)))))) { yield uni = a }
          						/*	if (and(pu.map
          										(b => isTrue(ta.subtype
          														(a,TAU.substitute
          																(List(TypeArg(a.getInfo, false, a)), k, b)))))) 
          							{
          								uni = a; break
          							}
							*/
          						}
          					}
          					case _ => 0
          				}
          			}
          			(k, uni)
          		}
          	}
          )
          if(isTrue(map(nc, sub)))
            Some(sub compose unifier)
          else {
               println("sub:" + sub)
               None
          }
        // Should never occur
        case _:Or => bug("Applied a substitution to an And and got an Or")
      }      
  }
  
  def unify(c: CFormula)(implicit ta: TypeAnalyzer): Option[(CFormula, Type => Type)] = {
      val eq = getEquality(c)
      val uni = un(eq).getOrElse(return None)
      Some((map(c, uni), uni))
  }
  
  /*
   * This method factors all of the type equalities out of an inequality constraint
   * since they are better solved through unification than the algorithm in Dan Smith's
   * "Java Type Inference is Broken" paper.
   */
  
  def getEquality(c: CFormula)(implicit ta: TypeAnalyzer): EFormula = reduce(c) match {
    case False => False
    case True => True
    case And(ps) =>
      val teq = ps.map{case (k, p@Primitive(pl,nl,pu,nu,pe,ne))  =>
        pl.filter(a => pu.exists(b => ta.equiv(a, b))) ++ Set(k) ++ pu.filter(a => pl.exists(b => ta.equiv(a, b)))
      }.toSet
      reduce(Conjuncts(teq))
    case Or(cs) => 
      val eq = cs.map(getEquality)
      dis(eq)
  }
  
  private def dis(eq: Set[EFormula])(implicit ta: TypeAnalyzer): EFormula = {
    if (eq.contains(True))
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
  
  
  def unify(e: EFormula)(implicit ta: TypeAnalyzer): Option[Type => Type] =
    un(reduce(e))
  
  private def un(e: EFormula)(implicit ta: TypeAnalyzer): Option[Type => Type] = e match {
    case False => None
    case True => Some(Substitution(Map()))
    case e@Conjuncts(eq) =>
      val split = eq.map{e => e.partition(_.isInstanceOf[_InferenceVarType])}
      if(split.forall{case (a, b) => b.flatMap(TU.getInferenceVars).exists(i => a.contains(i))})
        return None
      val (ivars, nivars) = split.unzip.asInstanceOf[(Set[Set[_InferenceVarType]], Set[Set[Type]])]
      /* Gets all equivalence classes with more than two inference variables and 
       * creates substitutions to unify them.
       */
      val subs = ivars.filter{_.size > 1}.map{x => Substitution(Map(x.tail.map((x.head, _)).toSeq:_*))}
      // If there were any inference variables to be unified, then recurse
      if(!subs.isEmpty) {
        val sub = subs.tail.foldRight(subs.head.asInstanceOf[Type => Type])((a, b) => a compose b)
        return un(map(e, sub)).map(_ compose sub)
      }
      /* Gets all equivalence classes with more than two non inference variables and computes the 
       * constraints under which they are equivalent.
       */
      val neqs = nivars.filter{_.size > 1}.map{e => e.tail.foldLeft((e.head, True.asInstanceOf[EFormula]))
        {case ((s, c), t) => (t, and(c, getEquality(ta.equivalent(s, t))))}._2}
      // If there were any non inference variables to be unified, then recurse
      if(!neqs.isEmpty) {
        val neq = and(neqs)
        val oeq = split.map{case (iv, niv) => iv ++ Set(niv.head)}
        return un(and(neq, Conjuncts(oeq)))
      }
      /* Now each equivalence class must consist of one inference variable 
       * and one non inference variable. Unify them.
       */
      Some(Substitution(Map(split.map{case (iv, niv) => 
        (iv.head.asInstanceOf[_InferenceVarType], niv.head)}.toSeq:_*)))
    case Disjuncts(es) => None
  }
  
  def map(c: CFormula, s: Type => Type)(implicit ta: TypeAnalyzer): CFormula = c match {
    case And(ps) =>
      and(ps.map{
        case (k, Primitive(pl,nl,pu,nu,pe,ne)) =>
          val sk = s(k)
          and(ta.subtype(s(ta.join(pl)), sk), and(
              and(nl.map(t => ta.notSubtype(s(t),sk))), and(
              ta.subtype(sk, s(ta.meet(pu))), and(
              and(nu.map(t => ta.notSubtype(sk, s(t)))), and(
              and(pe.map(t => ta.excludes(sk,s(t)))),
              and(ne.map(t => ta.notExcludes(sk, s(t)))))))))})
    case Or(cs) => dis(cs.map(map(_, s)))
    case _ => c
  }
  
  def map(e: EFormula, s: Type => Type)(implicit ta: TypeAnalyzer): EFormula = e match {
    case Conjuncts(eq) => reduce(Conjuncts(eq.map(ts => ts.map(s))))
    case Disjuncts(es) => dis(es.map(map(_, s)))
    case _ => e
  }
  
  def negate(c: CFormula)(implicit ta: TypeAnalyzer): CFormula = neg(reduce(c))
  
  def neg(c: CFormula)(implicit ta: TypeAnalyzer): CFormula = c match {
    case True => False
    case False => True
    case Or(cs) => and(cs.map(negate))
    case And(ps) =>
      def neg(ip: (_InferenceVarType, Primitive))(implicit ta: TypeAnalyzer): CFormula = ip match {
        case (i, Primitive(pl, nl, pu, nu, pe, ne)) =>
          or(or(pl.map(t => notLowerBound(i, t))), or(
             or(nl.map(t => lowerBound(i, t))), or(
             or(pu.map(t => notUpperBound(i, t))), or(
             or(nu.map(t => upperBound(i, t))), or(
             or(pe.map(t => notExclusion(i, t))),
             or(ne.map(t => exclusion(i, t))))))))
      }
      or(ps.map(neg))
  }
    
  def upperBound(i: _InferenceVarType, t: Type): CFormula =
    And(Map(i -> Primitive(Set(), Set(), Set(t), Set(), Set(), Set())))
  
  def notUpperBound(i: _InferenceVarType, t: Type): CFormula = 
    And(Map(i -> Primitive(Set(), Set(), Set(), Set(t), Set(), Set())))
  
  def lowerBound(i: _InferenceVarType, t: Type): CFormula =
    And(Map(i -> Primitive(Set(t), Set(), Set(), Set(), Set(), Set())))
    
  def notLowerBound(i: _InferenceVarType, t: Type): CFormula =
    And(Map(i -> Primitive(Set(), Set(t), Set(), Set(), Set(), Set())))
    
  def exclusion(i: _InferenceVarType, t: Type): CFormula = 
    And(Map(i -> Primitive(Set(), Set(), Set(), Set(), Set(t), Set())))
  
  def notExclusion(i: _InferenceVarType, t: Type): CFormula = 
    And(Map(i -> Primitive(Set(), Set(), Set(), Set(), Set(), Set(t))))
    
  def fromBoolean(x: Boolean) = if (x) True else False
  
}
