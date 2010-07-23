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
import com.sun.fortress.compiler.typechecker.InferenceVarReplacer
import com.sun.fortress.exceptions.InterpreterBug.bug
import com.sun.fortress.scala_src.types.TypeAnalyzer
import com.sun.fortress.scala_src.useful.{STypesUtil => TU}

sealed trait CFormula{}
sealed trait EFormula{}

case object True extends CFormula with EFormula {
  override def toString = "True"
}

case object False extends CFormula with EFormula {
  override def toString = "False"
}

case class And(lowers: Map[_InferenceVarType, Set[Type]],
               uppers: Map[_InferenceVarType, Set[Type]]) extends CFormula {
  
  /** Format as conjunction of triples. */
  override def toString =
    Formula.toTriples(this).get map { case (lbs, x, ubs) =>
      val lbsStr = lbs.map(_.toString).mkString("{", ", ", "}")
      val ubsStr = ubs.map(_.toString).mkString("{", ", ", "}")
      "(%s <: %s <: %s)".format(lbsStr, x, ubsStr)
    } mkString " /\\ "
}

case class Or(conjuncts: Set[And]) extends CFormula {
  override def toString =
    conjuncts.map(s => "(%s)".format(s)).mkString(" \\/ ")
}

case class Conjuncts(eq: Set[Set[Type]]) extends EFormula {}

case class Disjuncts(es: Set[Conjuncts]) extends EFormula {}

case class Substitution(m: Map[_InferenceVarType, Type]) extends (Type => Type) {
  // Cache the lifted type substitution.
  protected val liftedSubstitution: Type => Type = TU.liftTypeSubstitution(m)
  override def apply(t: Type): Type = liftedSubstitution(t)
}

object Formula{

  private def merge[S, T](a: Map[S, Set[T]], b: Map[S, Set[T]]): Map[S, Set[T]] =
    Map((a.keySet ++ b.keySet).map(k => (k, get(k, a) ++ get(k, b))).toSeq:_*)
  
  private def get[S, T](k: S, m: Map[S, Set[T]]) = 
    m.getOrElse(k, Set())
   
  def and(cs: Iterable[CFormula])(implicit ta: TypeAnalyzer): CFormula =
    cs.foldLeft(True.asInstanceOf[CFormula])(and)
  
  def and(cs: Iterable[EFormula])(implicit ta: TypeAnalyzer): EFormula =
    cs.foldLeft(True.asInstanceOf[EFormula])(and)
  
  def and(c1: CFormula, c2: CFormula)(implicit ta: TypeAnalyzer): CFormula = (c1, c2) match {
    case (True, _) => reduce(c2)
    case (False, _) => False
    case (And(l1, u1), And(l2, u2)) => reduce(And(merge(l1, l2), merge(u1, u2)))
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
  
  private def minimalTypes(s: Set[Type])(implicit ta: TypeAnalyzer) =
      TU.conjuncts(ta.meet(s))
      
  private def maximalTypes(s: Set[Type])(implicit ta: TypeAnalyzer) =
      TU.disjuncts(ta.join(s))
  
  def implies(c1: CFormula, c2: CFormula)(implicit ta: TypeAnalyzer) =
    imp(reduce(c1), reduce(c2))
    
  private def imp(c1: CFormula, c2: CFormula)(implicit ta: TypeAnalyzer): Boolean = (c1, c2) match {
    case (False, _) => true
    case (_, False) => false
    case (_, True) => true
    case (True, _) => false
    // Checks whether every constraint in c2 is in c1
    case (And(l1, u1), And(l2, u2)) =>
      val lowers = l2.keySet.forall(i =>
        ta.lteq(ta.join(get(i, l2).filter(_!=i)),
                ta.join(get(i, l1).filter(_!=i))))
      val uppers = u2.keySet.forall(i =>
        ta.lteq(ta.meet(get(i, u1).filter(_!=i)),
                ta.meet(get(i, u1).filter(_!=i))))
      uppers && lowers
    case (c1, Or(cs)) => cs.exists(imp(c1, _))
    case (Or(cs), c2) => cs.forall(imp(_, c2))
  }
  
  private def isContradictory(l: Map[_InferenceVarType, Set[Type]], u: Map[_InferenceVarType, Set[Type]])(implicit ta: TypeAnalyzer) =
    (l.keySet ++ u.keySet).exists{i => 
        val lb = ta.join(get(i, l).filter(_!=i))
        val ub = ta.meet(get(i, u).filter(_!=i))
        val c = ta.subtype(lb, ub)
        isFalse(c)}
  
                   
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
  
  private def isContradictory(eq: Set[Set[Type]])(implicit ta: TypeAnalyzer) =
    eq.forall{e => 
      !e.isEmpty && e.tail.foldLeft((e.head, false)){ case ((s, b), t) =>
        (t, isFalse(ta.equivalent(s,t)) || b)
      }._2
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
    case And(l, u) => 
      val nl = l.map{case (i, ls) => (i, maximalTypes(ls.filter(_!=i)))}.
        filter{case (i,b) => !b.isEmpty}
      val nu = u.map{case (i, us) => (i, minimalTypes(us.filter(_!=i)))}.
        filter{case (i,b) => !b.isEmpty}
      if(nu.isEmpty && nl.isEmpty)
        True
      else if(isContradictory(nl, nu))
        False
      else
        And(nl, nu)
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
  
  def reduce(e: EFormula)(implicit ta: TypeAnalyzer): EFormula = e match {
    // Should also merge connected components
    case Conjuncts(eq) => 
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
  
  private def merge(es: Set[Set[Type]])(implicit ta: TypeAnalyzer): Set[Set[Type]] =
    es.foldLeft(Set[Set[Type]]()){(a, b) => merge(b, a)}
  
  /*
   * Given a set of types and a reduced set of sets of types
   * return a reduced set of set of types.
   */
  
  private def merge(e1: Set[Type], es: Set[Set[Type]])(implicit ta: TypeAnalyzer): Set[Set[Type]] =
    if(es.isEmpty)
      Set(e1)
    else {
      // Since es is reduced there will be 0 or 1 matches
      val e2 = es.filter(e => e.exists(a => e1.exists(ta.equiv(_, a)))).headOption
      val nes = es.filterNot(e => e.exists(a => e1.exists(ta.equiv(_, a))))
      Set(e2.getOrElse(Set()) ++ e1) ++ nes
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
     * and an inequality constraint I
     * 2) Find the principal unifier U of E
     * 3) Apply U to I to get I'
     * 4) Solve I' to get a substitution S
     * 5) Return S composed with U
     */
    case _:And => 
      val (nc, unifier) = unify(c).getOrElse(return None)
      nc match {
        case True => Some(unifier)
        case False => None
        case nc@And(l, u) =>
          val sub = TU.killIvars compose Substitution(l.map{case (k, v) => 
            (k, ta.join(v.filterNot(TU.hasInferenceVars)))})
          val remainder = reduce(And(l.mapValues{_.filter(TU.hasInferenceVars)}, 
                                     u.mapValues{_.filter(TU.hasInferenceVars)}))
          if(isTrue(map(remainder, sub)))
            Some(sub compose unifier)
          else
            None
        // Should never occur
        case _:Or => bug("Applied a subsitution to an And and got an Or")
      }      
  }
  
  def unify(c: CFormula)(implicit ta: TypeAnalyzer): Option[(CFormula, Type => Type)] = {
      val (eq, ineq) = factorEquality(c)
      val uni = un(eq).getOrElse(return None)
      Some((map(ineq, uni), uni))
  }
  
  /*
   * This method factors all of the type equalities out of an inequality constraint
   * since they are better solved through unification than the algorithm in Dan Smith's
   * "Java Type Inference is Broken" paper.
   */
  
  def factorEquality(c: CFormula)(implicit ta: TypeAnalyzer): (EFormula, CFormula) = reduce(c) match {
    case False => (False, False)
    case True => (True, True)
    case And(l, u) =>
      val teq = (l.keySet ++ u.keySet).map{k =>
        val ls = get(k, l)
        val us = get(k, u)
        ls.filter(a => us.exists(b => ta.equiv(a, b))) ++ Set(k) ++ us.filter(a => ls.exists(b => ta.equiv(a, b)))
      }.toSet
      reduce(Conjuncts(teq)) match {
        case e@Conjuncts(eq) =>
          val nls = l.map{case (k, v) => (k, v.filterNot(t => eq.find(_.contains(k)).getOrElse(Set()).exists(ta.equiv(_,t))))}
          val nus = u.map{case (k, v) => (k, v.filterNot(t => eq.find(_.contains(k)).getOrElse(Set()).exists(ta.equiv(_,t))))}
          (e, reduce(And(nls, nus)))
        case True => (True, c)
        case False => (False, False)
        case Disjuncts(es) => bug("Reduced a conjunct and got a disjunct")
      }
    case Or(cs) => 
      val (eq, ncs) = cs.map(factorEquality).unzip
      (dis(eq), dis(ncs))
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
        val sub = subs.tail.foldRight((x: Type) => x)((a, b) => a compose b)
        return un(map(e, sub)).map(_ compose sub)
      }
      /* Gets all equivalence classes with more than two non inference variables and computes the 
       * constraints under which they are equivalent.
       */
      val neqs = nivars.filter{_.size > 1}.map{e => e.tail.foldLeft((e.head, True.asInstanceOf[EFormula]))
        {case ((s, c), t) => (t, and(c, factorEquality(ta.equivalent(s, t))._1))}._2}
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
    case And(l, u) =>
      val lcs = l.map{case (i, ls) => ta.subtype(s(ta.join(ls)), s(i))}
      val ncs = u.map{case (i, us) => ta.subtype(s(i), s(ta.meet(us)))}
      and(and(lcs), and(ncs))
    case Or(cs) => dis(cs.map(map(_, s)))
    case _ => c
  }
  
  def map(e: EFormula, s: Type => Type)(implicit ta: TypeAnalyzer): EFormula = e match {
    case Conjuncts(eq) => reduce(Conjuncts(eq.map(ts => ts.map(s))))
    case Disjuncts(es) => dis(es.map(map(_, s)))
    case _ => e
  }
  
  /**
   * Given a constraint formula, return the triples corresponding to each
   * conjunct. Each triple is of the form `(lbs, x, ubs)`, where lbs is a set
   * of disjuncts forming the lower bound of inference variable x and ubs is
   * likewise a set of conjuncts forming the upper bound.
   */
  def toTriples(cf: CFormula): Option[Set[(Set[Type],
                                           _InferenceVarType,
                                           Set[Type])]] =
    cf match {
      case True => Some(Set.empty)
      
      // Flatten out the lowers and uppers maps.
      case And(lowersMap, uppersMap) =>
      
        // Gather all inference vars and group with lowers and uppers.
        val allVars = (lowersMap.keySet union uppersMap.keySet).toSet
        val triples = allVars map { iv =>
          (lowersMap.getOrElse(iv, Set.empty),
           iv,
           uppersMap.getOrElse(iv, Set.empty))
        }
        Some(triples)
        
      case _ => None
    }
    
  def upperBound(i: _InferenceVarType, t: Type): CFormula =
    And(Map(), Map((i,Set(t))))
  
  def lowerBound(i: _InferenceVarType, t: Type): CFormula =
    And(Map((i,Set(t))), Map())
  
  def fromBoolean(x: Boolean) = if (x) True else False

  
}
