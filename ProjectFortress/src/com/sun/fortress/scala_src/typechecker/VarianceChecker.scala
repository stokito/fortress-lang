/*******************************************************************************
   Copyright 2012, Oracle and/or its affiliates.
   All rights reserved.


   Use is subject to license terms.

   This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.scala_src.typechecker

import _root_.java.util.ArrayList
import com.sun.fortress.nodes.Node
import com.sun.fortress.nodes.Component
import com.sun.fortress.nodes.TraitDecl
import com.sun.fortress.nodes.Decl
import com.sun.fortress.nodes.FnDecl
import com.sun.fortress.nodes.StaticParam
import com.sun.fortress.nodes.Param
import com.sun.fortress.nodes.StaticArg
import com.sun.fortress.nodes.Type
import com.sun.fortress.nodes.TraitType
import com.sun.fortress.nodes.TupleType
import com.sun.fortress.nodes.ArrowType
import com.sun.fortress.nodes.BottomType
import com.sun.fortress.nodes.IntersectionType
import com.sun.fortress.nodes.UnionType
import com.sun.fortress.nodes.VarType
import com.sun.fortress.nodes.KindType
import com.sun.fortress.nodes.TraitSelfType
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.useful.ASTGenHelper._
import com.sun.fortress.exceptions.StaticError
import com.sun.fortress.exceptions.TypeError

object VarianceChecker {

 val errors = new ArrayList[StaticError]()
 private def error(s:String, n:Node) = errors.add(TypeError.make(s,n))

 def run(ast: Component) = {

   val decls = scalaify(ast.getDecls()).asInstanceOf[List[Decl]]
   val b = (true /: decls) (_ && dispatch(_))
   if (!b) error("Variance not correct",ast)
   errors

 }

 private def dispatch(n: Decl): Boolean = {

   n match {
     case t: TraitDecl =>
       val decls = scalaify(t.getHeader().getDecls()).asInstanceOf[List[Decl]]
       val params =
scalaify(t.getHeader().getStaticParams()).asInstanceOf[List[StaticParam]]
       (true /: decls) (_ && check(_,params))
     case _ => true
   }

 }

 private def check(decl: Decl, params: List[StaticParam]): Boolean = {

       decl match {
         case m: FnDecl =>
           val rangeType = m.getHeader().getReturnType().unwrap()
           val domainParams =
scalaify(m.getHeader().getParams()).asInstanceOf[List[Param]]
           val domainTypes = domainParams map
(_.getIdType().unwrap().asInstanceOf[Type])
           crawl(rangeType,1)(params) &&  (true /: domainTypes) (_ &&
crawl(_,-1)(params))
         case _ => true
       }

 }

 private def flip(polarity: Int) = polarity * -1

 private def crawl(ty: Type, polarity: Int)(implicit params:
List[StaticParam]): Boolean = {

   ty match {

     case t: VarType =>
       def checkSingleParam(param: StaticParam) = {
               if (t.getName().getText().equals(param.getName().getText()))
                       param.getVariance() * polarity >= 0
               else true
       }
       (true /: params) (_ && checkSingleParam(_))

     case t:TraitType =>
       val traitParams =
scalaify(t.getTraitStaticParams()).asInstanceOf[List[StaticParam]]
       val traitArgs = scalaify(t.getArgs()).asInstanceOf[List[StaticArg]]
       val traitInfo = traitParams.zip(traitArgs)
       val filteredTraitInfo = traitInfo filter
(_._1.getKind().equals(new KindType()))
       (true /: filteredTraitInfo) ((b, x) => b &&
crawl(x._2.asInstanceOf[Type],x._1.getVariance() * polarity))

     case t: TraitSelfType => true // Unsound, need more information to know what to do exactly
     // Fix before plugin the Variance checker to the STypeChecker !!!
     /*
       val t: TraitType =
t.asInstanceOf[TraitSelfType].getNamed().asInstanceOf[TraitType]
       val traitParams =
scalaify(t.getTraitStaticParams()).asInstanceOf[List[StaticParam]]
       val traitArgs = scalaify(t.getArgs()).asInstanceOf[List[StaticArg]]
       val traitInfo = traitParams.zip(traitArgs)
       val filteredTraitInfo = traitInfo filter
(_._1.getKind().equals(new KindType()))
       (true /: filteredTraitInfo) ((b, x) => b &&
crawl(x._2.asInstanceOf[Type],x._1.getVariance() * polarity))
       */
     case t: TupleType =>
       val types = scalaify(t.getElements()).asInstanceOf[List[Type]]
       (true /: types) (_ && crawl(_,polarity))

     case t: ArrowType =>
       crawl(t.getDomain(),flip(polarity)) && crawl(t.getRange(),polarity)

     case t: IntersectionType =>
       val types = scalaify(t.getElements()).asInstanceOf[List[Type]]
       (true /: types) (_ && crawl(_,polarity))

     case t: UnionType =>
        val types = scalaify(t.getElements()).asInstanceOf[List[Type]]
       (true /: types) (_ && crawl(_,polarity))

     case t =>
       error("Variance checker: unknow form of type: " + t,t)
       false

   }


 }


}