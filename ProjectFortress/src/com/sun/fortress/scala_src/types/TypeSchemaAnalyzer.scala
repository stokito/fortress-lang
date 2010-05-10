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

package com.sun.fortress.scala_src.types

import com.sun.fortress.compiler.index._
import com.sun.fortress.compiler.Types.ANY
import com.sun.fortress.compiler.Types.BOTTOM
import com.sun.fortress.compiler.Types.OBJECT
import com.sun.fortress.exceptions.InterpreterBug.bug
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.NodeFactory.typeSpan
import com.sun.fortress.nodes_util.{NodeFactory => NF}
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.typechecker.ConstraintFormula
import com.sun.fortress.scala_src.typechecker.CnFalse
import com.sun.fortress.scala_src.typechecker.CnTrue
import com.sun.fortress.scala_src.typechecker.CnAnd
import com.sun.fortress.scala_src.typechecker.CnOr
import com.sun.fortress.scala_src.typechecker.staticenv.KindEnv
import com.sun.fortress.scala_src.typechecker.TraitTable
import com.sun.fortress.scala_src.types.TypeAnalyzerUtil._
import com.sun.fortress.scala_src.useful.ErrorLog
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Maps._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.Sets._
import com.sun.fortress.scala_src.useful.STypesUtil._
import com.sun.fortress.syntax_abstractions.rats.RatsUtil.getFreshName
import com.sun.fortress.useful.NI

class TypeSchemaAnalyzer(implicit val ta: TypeAnalyzer) extends BoundedLattice[Type] {
  def top = ANY
  def bottom = BOTTOM
  
  def meet(s: Type, t: Type): Type = s
  def join(s: Type, t: Type): Type = s
  
  // I am going to need to alpha rename bound params to make this work
  def lteq(s: Type, t:Type): Boolean = (s,t) match {
    case (s,t) if !s.getInfo.getStaticParams.isEmpty =>
      /* Extend the type analyzer with the static parameters
       * from s. Then strip s of it's type parameters to get s'
       * and call lteq(s',t) */
      // Q: What about lifted/unlifted
      val sparams = getStaticParams(s)
      val where = getWhere(s)
      val tsa = extend(sparams, where)
      val ss = clearStaticParams(s)
      ta.lteq(ss,t)
    case (s,t) if !t.getInfo.getStaticParams.isEmpty =>
      /* Try and infer an instantiation sigma of t such that 
       * s <: sigma(t) */
      val sparams = getStaticParams(t)
      def constraintMaker(tt: Type) = ta.subtype(s, tt)
      !inferStaticParamsHelper(t, sparams, constraintMaker).isEmpty
    case (s,t) => ta.lteq(s, t)
  }
  
  /***
   * Returns a type with all bound static parameters replaced
   * with unique identifiers.
   */
  def alphaRename(t: Type): Type = {
    if(getStaticParams(t).isEmpty)
      return t
    return t
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
    val repl_pairs: List[(StaticParam, StaticArg)] = (t_sp, s_sp_args).zip
    
    // Instantiate t and its bounds with s's static params.
    val t_inst = staticInstantiation(repl_pairs, t)(ta).getOrElse{return false}
    val t_bds_inst = t_bds.map { bd_opt =>
      bd_opt.map { bd =>
        staticInstantiation(repl_pairs, bd)(ta).getOrElse{return false}
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
}