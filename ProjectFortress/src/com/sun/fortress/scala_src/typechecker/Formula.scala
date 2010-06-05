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

case class Or(conjuncts: List[And]) extends CFormula {
  override def toString(): String = "Or"
}

case class Equality(eq: Map[_InferenceVarType, Set[Type]]) extends EFormula {}

object Formula{

  def and(c1: CFormula, c2: CFormula)(implicit ta: TypeAnalyzer): CFormula = (c1, c2) match {
    case (True, _) => reduce(c2)
    case (False, _) => False
    case (And(l1, u1), And(l2, u2)) =>
      val nl = (l1.keySet ++ l2.keySet).map(i =>(i, l1.getOrElse(i, Set()) ++ l2.getOrElse(i, Set())))
      val nu = (u1.keySet ++ u2.keySet).map(i =>(i, u1.getOrElse(i, Set()) ++ u2.getOrElse(i, Set())))
      reduce(And(Map(nl.toSeq:_*), Map(nu.toSeq:_*)))
    case (Or(cs), Or(ds)) =>
      val ncs = (for(c <- cs; d <- ds) yield and(c,d)).flatMap{
        case a:And => Some(a)
        case True => return True
        case _ => None
      }
      reduce(Or(ncs))
    case (c1: Or, c2: And) => and(c1, Or(List(c2)))
    case _ => and(c2, c1)
  }

  def or(c1: CFormula, c2: CFormula)(implicit ta: TypeAnalyzer): CFormula = (c1, c2) match {
    case (True, _) => True
    case (False, _) => reduce(c2)
    case (Or(cs), Or(ds)) => reduce(Or(cs ++ ds))
    case (a: And, b: And) => reduce(Or(List(a, b)))
    case (a: And, Or(cs)) => reduce(Or(cs ++ List(a)))
    case _ => or(c2, c1)
  }
  
  def implies(c1: CFormula, c2: CFormula)(implicit ta: TypeAnalyzer): Boolean = (c1, c2) match {
    case (False, _) => true
    case (_, True) => true
    case (True, False) => false
    // Checks if c2 is vacuous
    case (True, And(l, u)) => 
      l.forall{case (i,b) => ta.eq(ta.join(b.filter(t => i!=t)), BOTTOM)} &&
      u.forall{case (i,b) => ta.eq(ta.meet(b.filter(t => i!=t)), ANY)}
    // Checks if c2 is contradictory
    case (And(l, u), False) => 
      (l.keySet ++ u.keySet).exists(i => 
        ta.subtype(ta.join(l.getOrElse(i, Set()).filter(t => i!=t)),
                   ta.meet(u.getOrElse(i, Set()).filter(t => i!=t))).isFalse)
    // Checks whether every constraint in c2 is in c1
    case (And(l1, u1), And(l2, u2)) =>
      val lowers = l2.keySet.forall(i =>
        ta.lteq(ta.join(l2.getOrElse(i,Set()).filter(t => t!=i)),
                ta.join(l1.getOrElse(i,Set()).filter(t => t!=i))))
      val uppers = u2.keySet.forall(i =>
        ta.lteq(ta.meet(u1.getOrElse(i, Set()).filter(t => t!=i)),
                ta.meet(u2.getOrElse(i, Set()).filter(t => t!=i))))
      uppers && lowers
    case (c1, Or(cs)) => cs.exists(implies(c1, _))
    case (Or(cs), c2) => cs.forall(implies(_, c2))
  }
  
  def implies(e1: EFormula, e2: EFormula)(implicit ta: TypeAnalyzer): Boolean = (e1,e2) match {
    case (False, _) => true
    case (_, True) => false
    case (True, False) => false
    // Checks if c2 is vacuous
    case (True, Equality(eq)) => eq.forall{case (i,e) => e.filter(t => t!=i).isEmpty}
    // Checks if c1 is contradictory
    case (Equality(eq), False) => eq.forall{case (i,e) => 
      var contradiction = false
      // Do I trust transitivity enough to do the order n algorithm instead of the order n^2
      for(s <- e; t <-e)
        contradiction |= ta.eq(s, t)
      contradiction
    }
    case (Equality(eq1), Equality(eq2)) =>
      eq2.keySet.forall{i =>
        eq2.getOrElse(i, Set()).forall(s => eq1.getOrElse(i,Set()).exists(t => ta.eq(s,t)))}
  }
  
  def equivalent(c1: CFormula, c2: CFormula)(implicit ta: TypeAnalyzer): Boolean =
    implies(c1, c2) && implies(c2, c1)
  
  def equivalent(e1: EFormula, e2: EFormula)(implicit ta: TypeAnalyzer): Boolean =
    implies(e1, e2) && implies(e2, e1)
    
  def isFalse(c: CFormula)(implicit ta: TypeAnalyzer): Boolean = implies(c, False)
  
  def isFalse(e: EFormula)(implicit ta: TypeAnalyzer): Boolean = implies(e, False)
  
  def isTrue(c: CFormula)(implicit ta: TypeAnalyzer): Boolean = implies(True, c)
  
  def isTrue(e: EFormula)(implicit ta: TypeAnalyzer): Boolean = implies(True, e)
  
  protected def reduce(c: CFormula)(implicit ta: TypeAnalyzer): CFormula =  c match {
    case _ if isFalse(c) => False
    case _ if isTrue(c) => True
    case And(l, u) => 
      val nl = l.mapValues(x => TU.disjuncts(ta.join(x))).filter{case (i,b) => !b.isEmpty}
      val nu = u.mapValues(x => TU.conjuncts(ta.meet(x))).filter{case (i,b) => !b.isEmpty}
      And(nl, nu)
    case Or(cs) =>
      val dcs = cs.distinct
      val ncs = dcs.filterNot(c => dcs.exists(x => implies(c, x) && x!=c)).map(reduce)
      if (ncs.size == 1)
        ncs.head
      else
        Or(ncs.map(_.asInstanceOf[And]))
    case _ => c
  }
  
  protected def reduce(e: EFormula)(implicit ta: TypeAnalyzer): EFormula = e match {
    case _ if isTrue(e) => True
    case _ if isFalse(e) => False
    case Equality(eq) => e
    case _ => e
  }
  
  protected def getMinimalElements(typs: Set[Type]) = {
    null
  }
  
  protected def getMaximalElements(typs: Set[Type]) = {
    null
  }
  
  protected def getExtremalElements(typs: Set[Type], comp: Type => Boolean) = {
    null
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
     * 4) Solve the inequality constraint
     */
    case c@And(l, u) => 
      val (equality, inequality) = factorConstraint(c)
      null
  }
  
  def factorConstraint(c: And)(implicit ta: TypeAnalyzer): (EFormula, CFormula) = {
    val And(l, u) = c
    val ivars = l.keySet ++ u.keySet
    val nbounds = ivars.map{i =>
      val il = l.getOrElse(i, Set())
      val iu = u.getOrElse(i, Set())
      val ieq = il intersect iu
      val nl = il diff ieq
      val nu = iu diff ieq
      ((i, ieq), ((i, nl),(i, nu)))
    }
    val (eq, temp) = nbounds.unzip
    val (nl, nu) = temp.unzip
    (reduce(Equality(Map(eq.toSeq:_*))), reduce(And(Map(nl.toSeq:_*), Map(nu.toSeq:_*))))
  }
  
  def unify(e: EFormula)(implicit ta: TypeAnalyzer) = null 

}
