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
  
  def implies(c1: CFormula, c2: CFormula)(implicit ta: TypeAnalyzer): Boolean = (c1, c2) match {
    case (False, _) => true
    case (_, True) => true
    case (True, False) => false
    // Checks if c2 is vacuous
    case (True, And(l, u)) => 
      l.forall{case (i,b) => ta.eq(ta.join(b.filter(!ta.lteq(_, i))), BOTTOM)} &&
      u.forall{case (i,b) => ta.eq(ta.meet(b.filter(!ta.lteq(i, _))), ANY)}
    // Checks if c2 is contradictory
    case (And(l, u), False) => 
      (l.keySet ++ u.keySet).exists(i => 
        ta.subtype(ta.join(get(i, l).filter(!ta.lteq(_, i))),
                   ta.meet(get(i, u).filter(!ta.lteq(i, _)))).isFalse)
    // Checks whether every constraint in c2 is in c1
    case (And(l1, u1), And(l2, u2)) =>
      val lowers = l2.keySet.forall(i =>
        ta.lteq(ta.join(get(i, l2).filter(!ta.lteq(_, i))),
                ta.join(get(i, l1).filter(!ta.lteq(_, i)))))
      val uppers = u2.keySet.forall(i =>
        ta.lteq(ta.meet(get(i, u1).filter(!ta.lteq(i, _))),
                ta.meet(get(i, u1).filter(!ta.lteq(i, _)))))
      uppers && lowers
    case (c1, Or(cs)) => cs.exists(implies(c1, _))
    case (Or(cs), c2) => cs.forall(implies(_, c2))
  }
  
  def implies(e1: EFormula, e2: EFormula)(implicit ta: TypeAnalyzer): Boolean = (e1,e2) match {
    case (False, _) => true
    case (_, True) => false
    case (True, False) => false
    // Checks if c2 is vacuous (relies on eq being transitive)
    case (True, Equality(eq)) => eq.forall{e =>
      e.isEmpty || e.tail.foldLeft((e.head, true)){ case ((s, b), t) =>
       (t, ta.eq(s,t) && b)
      }._2 
    }    
    // Checks if c1 is contradictory
    case (Equality(eq), False) => eq.forall{e => 
      !e.isEmpty && e.tail.foldLeft((e.head, false)){ case ((s, b), t) =>
        (t, ta.equivalent(s,t).isFalse || b)
      }._2
    }
    case (Equality(eq1), Equality(eq2)) =>
      eq2.forall{e2 => eq1.exists(e1 => e2.subsetOf(e1))}
  }
  
  def equivalent(c1: CFormula, c2: CFormula)(implicit ta: TypeAnalyzer): Boolean =
    implies(c1, c2) && implies(c2, c1)
  
  def equivalent(e1: EFormula, e2: EFormula)(implicit ta: TypeAnalyzer): Boolean =
    implies(e1, e2) && implies(e2, e1)
    
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
    case _ if isFalse(c) => False
    case And(l, u) => 
      val nl = l.map{case (i, ls) => (i, maximalTypes(ls.filter(!ta.lteq(_, i))))}.
        filter{case (i,b) => !b.isEmpty}
      val nu = u.map{case (i, us) => (i, minimalTypes(us.filter(!ta.lteq(i, _))))}.
        filter{case (i,b) => !b.isEmpty}
      if(nu.isEmpty && nl.isEmpty)
        True
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
    case _ if isFalse(e) => False
    case Equality(eq) => Equality(eq.map{e => removeDuplicates(e, (a: Type, b: Type) => ta.eq(a, b))})
    case _ => e
  }
  
  def solve(c: CFormula, b: Map[_InferenceVarType, Set[Type]])(implicit ta: TypeAnalyzer) = c match {
    // False cannot be solved
    case False => null
    // True has the trivial solution
    case True => null
    // We solve an Or by solving one of its branches
    case Or(cs) => null
    /* To solve an And:
     * 1) Factor the constraint formula into the conjunction of an equality constraint
     * and an inequality constraint
     * 2) Find the principal unifier of the equality constraints
     * 3) Use the solution of the equality constraint to eliminate degrees
     * of freedom in the inequality constraint
     * 4) Solve the inequality constraint if possible
     */
    case c@And(l, u) => 
      val (equality, inequality) = factorConstraint(c)
      null
  }
  
  /*
   * T
   */
  
  def factorConstraint(c: And)(implicit ta: TypeAnalyzer): (EFormula, CFormula) = {
    val And(l, u) = c
    val (eq, temp) = (l.keySet ++ u.keySet).map{k =>
      val ls = get(k, l)
      val us = get(k, u)
      val eq = ls.filter(a => us.exists(b => ta.eq(a, b)))
      (eq ++ Set(k), ((k, ls diff eq), (k, us.filterNot(u => ls.exists(ta.eq(u, _))))))
    }.unzip
    val (ls, us) = temp.unzip
    (Equality(eq.asInstanceOf[Set[Set[Type]]]), And(Map(ls.toSeq:_*), Map(us.toSeq:_*)))
  }
  
  /*
   * This method should take an equality formula and produce
   * the principal unifier.
   */
  
  def unify(e: EFormula)(implicit ta: TypeAnalyzer): Map[_InferenceVarType, Type] = {
    null
  }

}
