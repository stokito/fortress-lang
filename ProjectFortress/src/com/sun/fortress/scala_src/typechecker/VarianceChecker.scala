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
import com.sun.fortress.nodes.VarDecl
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
import com.sun.fortress.nodes.LValue
import com.sun.fortress.nodes.Binding
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.useful.ASTGenHelper._
import com.sun.fortress.exceptions.StaticError
import com.sun.fortress.exceptions.TypeError

/**
 *
 */
object VarianceChecker {

 val errors = new ArrayList[StaticError]()
 private def error(s:String, n:Node) = errors.add(TypeError.make(s,n))

 private def scan[T](l: List[T],P: T => Boolean): Boolean = (true /: l) (_ && P(_))
 
 /**
 * @param ast
 * @return
 */
def run(ast: Component) = {

   val decls = scalaify(ast.getDecls()).asInstanceOf[List[Decl]]
   val b = scan(decls,verifyTrait)
   errors

 }
   
/**
 * @param s: A trait type parameter for which we are checking variance
 * @param name: A type name we found while analyzing a type
 * @param polarity: the polarity of the position we were in when we found the type name
 * @return
 */
private def verifyParam(s: StaticParam, name: String, polarity: Int, n: Node): Boolean = {
  
  // If the type name we found while analyzing a type match the trait static parameter name
  if (s.getName().getText().equals(name)) {
    // Verify that the static parameter's variance declaration correspond to the current polarity
	  val b = s.getVariance() == polarity
	  val vary = if (s.getVariance() == 1) "covariant" else "contravariant"
	  val othervary = polarity match {
	    case 1 => "covariant"
	    case 0 => "invariant"
	    case -1 => "contravariant"
	  }  
	  if (!b) error(
	      "Type error: static parameter " + s.getName().getText() + 
	      " is declared to be " + vary +
	      " but appears in a " + othervary + " position",n)
	  b
  }
  // Otherwise, nothing to do
  else true
  
}  
 
 /**
 * @param n A top-level declaration (trait, function, object, variable)
 * @return true if the declaration is a function, obejct, or variable, as these cannot have variance clauses
 *         true if the declaration is a trait and all occurrences of the trait's type parameters appear in a position
 *         that match their declared polarity
 *         false otherwise
 */
private def verifyTrait(n: Decl): Boolean = {

   n match {
     
     case t: TraitDecl =>
       val decls = scalaify(t.getHeader().getDecls()).asInstanceOf[List[Decl]]
       val params = scalaify(t.getHeader().getStaticParams()).asInstanceOf[List[StaticParam]]
       // We don't need to verify anything for type parameters that are declared invariant
       val filteredParams = params filter (_.getVariance() != 0)
       scan(decls,{ verifyTraitDeclaration(_:Decl,filteredParams) })
       
       
     case _ => 
       true

   }

 }

 /**
 * @param decl A declaration appearing within a trait declaration
 * @param traitStaticParameters
 * @return
 * For each functional declaration (getters, setters, methods, functional methods) we verify 
 * the correct polarity of the trait's type parameters for 
 * 1. The return type
 * 2. The domain type
 * 3. The static parameters of the functional declaration
 * 
 * For now, we do not handle abstract variable declaration. Need to check whether they are incolved
 * in variance clauses.
 */
private def verifyTraitDeclaration(decl: Decl, traitStaticParameters: List[StaticParam]): Boolean = {

       decl match {
         
         case m: FnDecl =>
           val rangeType = m.getHeader().getReturnType().unwrap()
           val domainParams = scalaify(m.getHeader().getParams()).asInstanceOf[List[Param]]
           val domainTypes = domainParams map (_.getIdType().unwrap().asInstanceOf[Type])
           val staticParams = scalaify(m.getHeader().getStaticParams()).asInstanceOf[List[StaticParam]]
           // First, analyze the arrow type range
           verifyType(rangeType,1)(traitStaticParameters) &&  
           // Second, analyze the arrow type domain
           scan(domainTypes, { verifyType(_:Type,-1)(traitStaticParameters) }) &&
           // Third, analyze the arrow type parameters
           scan(staticParams, { verifyFunDeclStaticParam(_:StaticParam,-1)(traitStaticParameters) })
         
         case v: VarDecl => 
           val lvalues = scalaify(v.getLhs()).asInstanceOf[List[LValue]]
           scan(lvalues, { verifyVarDecl(_:LValue)(traitStaticParameters) } )

         case _ => 
           error("Variance checker: unknow form of declaration: " + decl,decl)
           false
       
       }

 }

 private def verifyVarDecl(v: LValue)(implicit traitStaticParameters:List[StaticParam]): Boolean = {
   
   val hidden = v.getMods().isHidden()
   val settable = v.getMods().isSettable()
   val typ = v.getIdType().unwrap()    // John: for now, I'm assuming that abstract field declarations always have a type  

   val pol = (hidden,settable) match {
     case (true,true) => -1
     case (true,false) => 1 // This one is actually ruled out by the parser
     case (false,false) => 1
     case (false,true) => 0 
   }
   
   verifyType(typ.asInstanceOf[Type],pol)  
   
 }

 private def flip(polarity: Int) = polarity * -1

/**
 * @param s
 * @return
 */
private def verifyFunDeclStaticParam(s: StaticParam, polarity: Int)(implicit traitStaticParameters:List[StaticParam]): Boolean = {
   
		 scan(traitStaticParameters, { verifyParam(_:StaticParam,s.getName().getText(),polarity,s) })	&&
		 scan(scalaify(s.getExtendsClause()).asInstanceOf[List[Type]], { verifyType(_: Type,polarity) } ) &&
		 scan(scalaify(s.getDominatesClause()).asInstanceOf[List[Type]], { verifyType(_: Type,flip(polarity)) })
   
 } 	
 
 /**
 * @param ty The type to analyze
 * @param polarity The current polarity  
 * @param traitStaticParameters The type parameters from the trait being declared
 * @return
 */
private def verifyType(ty: Type, polarity: Int)(implicit traitStaticParameters:List[StaticParam]): Boolean = {

   ty match {

     case t: VarType =>
       scan(traitStaticParameters, { verifyParam(_: StaticParam,t.getName().getText(),polarity,ty) })

     case t:TraitType =>
       val traitParams = scalaify(t.getTraitStaticParams()).asInstanceOf[List[StaticParam]]
       val traitArgs = scalaify(t.getArgs()).asInstanceOf[List[StaticArg]]
       val traitInfo = traitParams.zip(traitArgs)
       val filteredTraitInfo = traitInfo filter (_._1.getKind().equals(new KindType()))
       (true /: filteredTraitInfo) ((b, x) => b && verifyType(x._2.asInstanceOf[Type],x._1.getVariance() * polarity))

     case ts: TraitSelfType => 
       val t: TraitType = ts.asInstanceOf[TraitSelfType].getNamed().asInstanceOf[TraitType]
       val traitParams = scalaify(t.getTraitStaticParams()).asInstanceOf[List[StaticParam]]
       val traitArgs = scalaify(t.getArgs()).asInstanceOf[List[StaticArg]]
       val traitInfo = traitParams.zip(traitArgs)
       val filteredTraitInfo = traitInfo filter (_._1.getKind().equals(new KindType()))
       (true /: filteredTraitInfo) ((b, x) => b && verifyType(x._2.asInstanceOf[Type],x._1.getVariance() * polarity))
       
     case t: ArrowType =>
       verifyType(t.getDomain(),flip(polarity)) && verifyType(t.getRange(),polarity)

     /*
      * The following compound types (tuple, intersection, union) are simple:
      * recursively verify their constituents, with same polarity. 
      */
       
     case t: TupleType =>
       val types = scalaify(t.getElements()).asInstanceOf[List[Type]]
       scan(types, { verifyType(_: Type,polarity) })
       
     case t: IntersectionType =>
       val types = scalaify(t.getElements()).asInstanceOf[List[Type]]
       scan(types, { verifyType(_: Type,polarity) })

     case t: UnionType =>
        val types = scalaify(t.getElements()).asInstanceOf[List[Type]]
        scan(types, { verifyType(_: Type,polarity) })

     case t =>
       error("Variance checker: unknow form of type: " + t,t)
       false

   }

 }


 

}