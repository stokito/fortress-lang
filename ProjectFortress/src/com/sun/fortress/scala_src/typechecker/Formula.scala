package com.sun.fortress.scala_src.typechecker

import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.NodeFactory
import com.sun.fortress.compiler.Types.ANY
import com.sun.fortress.compiler.Types.BOTTOM
import com.sun.fortress.compiler.Types.OBJECT
import com.sun.fortress.compiler.typechecker.SubtypeHistory
import com.sun.fortress.compiler.typechecker.InferenceVarReplacer
import com.sun.fortress.exceptions.InterpreterBug.bug
import com.sun.fortress.scala_src.types.TypeAnalyzer
import com.sun.fortress.scala_src.useful.{STypesUtil => TU}

sealed trait CFormula{}
sealed trait EFormula{}

case object True extends CFormula with EFormula {
  override def toString(): String = "True"
}

case object False extends CFormula with EFormula {
  override def toString(): String = "False"
}

case class And(lowers: Map[_InferenceVarType, Set[Type]], uppers: Map[_InferenceVarType, Set[Type]]) extends CFormula{
  override def toString(): String = "And"
}

case class Or(conjuncts: Set[And]) extends CFormula {
  override def toString(): String = "Or"
}

case class Equality(eq: Set[Set[Type]]) extends EFormula {}

case class Substitution(m: Map[_InferenceVarType, Type]) extends (Type => Type) {
  override def apply(t: Type): Type = t
  def apply(c: CFormula)(implicit ta: TypeAnalyzer): Option[CFormula] = None
  def apply(e: EFormula)(implicit ta: TypeAnalyzer): Option[EFormula] = None
}

object Formula{

  private def merge[S, T](a: Map[S, Set[T]], b: Map[S, Set[T]]): Map[S, Set[T]] =
    Map((a.keySet ++ b.keySet).map(k => (k, get(k, a) ++ get(k, b))).toSeq:_*)
  
  private def get[S, T](k: S, m: Map[S, Set[T]]) = 
    m.getOrElse(k, Set())
    
  def and(c1: CFormula, c2: CFormula)(implicit ta: TypeAnalyzer): CFormula = (c1, c2) match {
    case (True, _) => reduce(c2)
    case (False, _) => False
    case (And(l1, u1), And(l2, u2)) => reduce(And(merge(l1, l2), merge(u1, u2)))
    case (Or(cs), Or(ds)) =>
      val ncs = for(c <- cs; d <- ds) yield and(c,d)
      if(ncs.exists(isTrue(_)))
        True
      else
        reduce(Or(ncs.filter(_.isInstanceOf[And]).asInstanceOf[Set[And]]))
    case (c1: Or, c2: And) => and(c1, Or(Set(c2)))
    case _ => and(c2, c1)
  }

  def or(c1: CFormula, c2: CFormula)(implicit ta: TypeAnalyzer): CFormula = (c1, c2) match {
    case (True, _) => True
    case (False, _) => reduce(c2)
    case (Or(cs), Or(ds)) => reduce(Or(cs ++ ds))
    case (a: And, b: And) => reduce(Or(Set(a, b)))
    case (a: And, Or(cs)) => reduce(Or(cs ++ Set(a)))
    case _ => or(c2, c1)
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
        ta.lteq(ta.join(get(i, l2).filter(!ta.lteq(_, i))),
                ta.join(get(i, l1).filter(!ta.lteq(_, i)))))
      val uppers = u2.keySet.forall(i =>
        ta.lteq(ta.meet(get(i, u1).filter(!ta.lteq(i, _))),
                ta.meet(get(i, u1).filter(!ta.lteq(i, _)))))
      uppers && lowers
    case (c1, Or(cs)) => cs.exists(imp(c1, _))
    case (Or(cs), c2) => cs.forall(imp(_, c2))
  }
  
  private def isContradictory(l: Map[_InferenceVarType, Set[Type]], u: Map[_InferenceVarType, Set[Type]])(implicit ta: TypeAnalyzer) =
    (l.keySet ++ u.keySet).exists(i => 
        ta.subtype(ta.join(get(i, l).filter(!ta.lteq(_, i))),
                   ta.meet(get(i, u).filter(!ta.lteq(i, _)))).isFalse)
  
                   
  def implies(e1: EFormula, e2: EFormula)(implicit ta: TypeAnalyzer) =
    imp(reduce(e1), reduce(e2))
    
  private def imp(e1: EFormula, e2: EFormula)(implicit ta: TypeAnalyzer): Boolean = (e1, e2) match {
    case (False, _) => true
    case (_, False) => false
    case (_, True) => true
    case (True, _) => false
    case (Equality(eq1), Equality(eq2)) =>
      eq2.forall{e2 => eq1.exists(e1 => e2.forall(e => e1.exists(ta.eq(e, _))))}
  }
  
  private def isContradictory(eq: Set[Set[Type]])(implicit ta: TypeAnalyzer) =
    eq.forall{e => 
      !e.isEmpty && e.tail.foldLeft((e.head, false)){ case ((s, b), t) =>
        (t, ta.equivalent(s,t).isFalse || b)
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
  
  private def removeDuplicates[T](s: Set[T], eq: (T, T)=> Boolean) = 
    s.foldLeft(Set[T]()){(ns, i) =>
      if(ns.exists(eq(i, _)))
        ns
      else
        ns ++ Set(i)
    }
  
  private def maximalCFormulas(s: Set[CFormula])(implicit ta: TypeAnalyzer) = {
    val nds = removeDuplicates(s, (a: CFormula, b: CFormula) => equivalent(a, b))
    nds.filterNot{c => nds.exists(d => implies(c, d) && !equivalent(c,d))}
  }

  def reduce(c: CFormula)(implicit ta: TypeAnalyzer): CFormula =  c match {
    case And(l, u) => 
      val nl = l.map{case (i, ls) => (i, maximalTypes(ls.filter(!ta.lteq(_, i))))}.
        filter{case (i,b) => !b.isEmpty}
      val nu = u.map{case (i, us) => (i, minimalTypes(us.filter(!ta.lteq(i, _))))}.
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
    case Equality(eq) => 
      val req = merge(eq).map{e => removeDuplicates(e, (a: Type, b: Type) => ta.eq(a, b))}.
        filterNot{_.isEmpty}
      if(req.forall(_.size==1))
        True
      else if(isContradictory(req))
        False
      else
        Equality(req)  
    case _ => e
  }
  
  /*
   * Given a set of sets of types that are supposed to represent
   * equivalence classes, this method merges together any classes
   * that share a type to give a reduced set of sets of types.
   */
  
  private def merge(es: Set[Set[Type]])(implicit ta: TypeAnalyzer): Set[Set[Type]] =
    if(es.isEmpty)
      es
    else {
      val h = es.first
      val t = es diff Set(h)
      merge(h, merge(t))
    }
  
  /*
   * Given a set of types and a reduced set of sets of types
   * return a reduced set of set of types.
   */
  
  private def merge(e1: Set[Type], es: Set[Set[Type]])(implicit ta: TypeAnalyzer): Set[Set[Type]] =
    if(es.isEmpty)
      Set(e1)
    else {
      // Since es is reduced there will be 0 or 1 matches
      val e2 = es.filter(e => e.exists(a => e1.exists(ta.eq(_, a)))).firstOption
      val nes = es.filterNot(e => e.exists(a => e1.exists(ta.eq(_, a))))
      Set(e2.getOrElse(Set()) ++ e1) ++ nes
    }
  
  def solve(c: CFormula)(implicit ta: TypeAnalyzer): Option[Type => Type] = c match {
    // False cannot be solved
    case False => None
    // True has the trivial solution
    case True => Some(Substitution(Map()))
    // We solve an Or by solving one of its branches
    case Or(cs) => null
    /* To solve an And:
     * 1) Factor the constraint formula into the conjunction of an equality constraint E
     * and an inequality constraint I
     * 2) Find the principal unifier U of E
     * 3) Apply U to I to get I'
     * 4) Solve I' to get a substitution S
     * 5) Return S composed with U
     */
    case c@And(l, u) => 
      val (eq, ineq) = factorEquality(c)
      val uni = unify(eq).getOrElse(return None)
      null
  }
  
  /*
   * This method factors all of the type equalities out of an inequality constraint
   * since they are better solved through unification than the algorithm in Dan Smith's
   * "Java Type Inference is Broken" paper.
   */
  
  def factorEquality(c: And)(implicit ta: TypeAnalyzer): (EFormula, CFormula) = {
    val And(l, u) = c
    val (eq, temp) = (l.keySet ++ u.keySet).map{k =>
      val ls = get(k, l)
      val us = get(k, u)
      val eq = ls.filter(a => us.exists(b => ta.eq(a, b)))
      (eq ++ Set(k), ((k, ls diff eq), (k, us.filterNot(u => ls.exists(ta.eq(u, _))))))
    }.unzip
    val (ls, us) = temp.unzip
    (reduce(Equality(eq.asInstanceOf[Set[Set[Type]]])), reduce(And(Map(ls.toSeq:_*), Map(us.toSeq:_*))))
  }
  
  /*
   * This method should take an equality formula and produce
   * the principal unifier.
   */
  
  def unify(e: EFormula)(implicit ta: TypeAnalyzer): Option[Type => Type] = {
    None
  }
  
}
