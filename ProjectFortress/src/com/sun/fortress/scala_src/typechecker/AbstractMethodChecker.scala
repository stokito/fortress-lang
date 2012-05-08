/*******************************************************************************
    Copyright 2009,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.scala_src.typechecker

import _root_.java.util.ArrayList
import scala.collection.Map
import scala.collection.Set
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.Iterator._
import edu.rice.cs.plt.collect.Relation
import com.sun.fortress.compiler.GlobalEnvironment
import com.sun.fortress.compiler.Types.BOTTOM
import com.sun.fortress.compiler.index.ComponentIndex
import com.sun.fortress.compiler.index.{DeclaredMethod => JavaDeclaredMethod}
import com.sun.fortress.compiler.index.FieldGetterOrSetterMethod
import com.sun.fortress.compiler.index.{Functional => JavaFunctional}
import com.sun.fortress.compiler.index.{FunctionalMethod => JavaFunctionalMethod}
import com.sun.fortress.compiler.index.HasSelfType
import com.sun.fortress.compiler.index.TraitIndex
import com.sun.fortress.compiler.index.ObjectTraitIndex
import com.sun.fortress.compiler.typechecker.StaticTypeReplacer
import com.sun.fortress.exceptions.StaticError
import com.sun.fortress.exceptions.TypeError
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.{NodeUtil => NU}
import com.sun.fortress.nodes_util.{NodeFactory => NF}
import com.sun.fortress.nodes_util.Span
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.overloading.OverloadingOracle
import com.sun.fortress.scala_src.typechecker.Formula._
import com.sun.fortress.scala_src.types.TypeAnalyzer
import com.sun.fortress.scala_src.types.TypeSchemaAnalyzer
import com.sun.fortress.scala_src.types.TypeAnalyzerUtil
import com.sun.fortress.scala_src.useful.Iterators._
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.Sets._
import com.sun.fortress.scala_src.useful.SNodeUtil._
import com.sun.fortress.scala_src.useful.STypesUtil._

/* All inherited abstract methods in object declarations and
 * object expressions should be defined,
 * with compatible signatures and modifiers.
 */
class AbstractMethodChecker(component: ComponentIndex,
                            globalEnv: GlobalEnvironment)
    extends Walker {
  implicit var typeAnalyzer = TypeAnalyzer.make(new TraitTable(component, globalEnv))
  var errors = List[StaticError]()
  val componentName = component.ast.getName
  private def error(loc: Span, msg: String) =
    errors = errors ::: List(TypeError.make(msg, loc))

  def check() = {
    // check object declarations
    for (tc <- component.typeConses.values) {
      tc match { case oi: ObjectTraitIndex => checkObjectDeclaration(oi.ast.asInstanceOf[ObjectDecl])
                 case _ =>
               }
    }
    // check object expressions
    walk(component.ast)
    errors
  }

  private def traceObjectDeclMethods(o: ObjectDecl, ms: Relation[IdOrOpOrAnonymousName, (JavaFunctional, StaticTypeReplacer, TraitType)]) = {
    println(ms.size + " method" + (if (ms.size == 1) "" else "s") + " for object " + o + ":" + o.getHeader.getStaticParams)
    for { (meth, _, tt) <- ms.secondSet } println(tt + ": " + meth)
  }

  private def checkObjectDeclaration(od: ObjectDecl) = {
    val tth = od.getHeader
    val fakeArgs = tth.getStaticParams.map(p => NF.makeTypeArg(NU.getSpan(p), p.asInstanceOf[StaticParam].getName.getText).asInstanceOf[StaticArg])
    val methods = allMethods(NF.makeTraitTypeForScala(tth.getName.asInstanceOf[Id], toJavaList(fakeArgs.toIterable)), typeAnalyzer)
    // traceObjectDeclMethods(od, methods)
    val oldTypeAnalyzer = typeAnalyzer
    // Add static parameters of the enclosing trait or object
    typeAnalyzer = typeAnalyzer.extend(toList(tth.getStaticParams), None)
    val tsa = new TypeSchemaAnalyzer()(typeAnalyzer)
    val concreteMethods = methods.secondSet.filter(x => x match { case (meth, _, tt) => 
      !isAbstractMethod(meth, tt)}).toList
    // For each abstract method, check to see whether it is covered by one or more concrete definitions.
    for ((meth, str, tt) <- methods.secondSet) {
      if (isAbstractMethod(meth, tt)) {
        val thisDomain = str.replaceIn(tsa.makeDomainFromArrow(makeArrowWithoutSelfFromFunctional(meth).get))
	val relevantConcreteMethods = concreteMethods.filter(x => x match { case (meth2, _, _) =>
            (meth2.name == meth.name) && (meth.asInstanceOf[HasSelfType].selfPosition == meth2.asInstanceOf[HasSelfType].selfPosition)})
        val concreteMethodsAndDomains = relevantConcreteMethods.map(x => x match { case (meth2, str2, _) =>
            (meth2, str2.replaceIn(tsa.makeDomainFromArrow(makeArrowWithoutSelfFromFunctional(meth2).get)))}).toList
        val moreSpecificConcreteMethodsAndDomains = concreteMethodsAndDomains.filter(x => x match { case (meth2, dom2) => tsa.subtypeED(dom2, thisDomain)})
        val domainList = moreSpecificConcreteMethodsAndDomains.map(x => x match { case (concMeth, domain) => domain }).toList
        val domainUnion = domainList.fold(BOTTOM)(tsa.joinED)
//    	println("\nThis domain: " + thisDomain)
//    	println("Concrete methods and domains: " + concreteMethodsAndDomains)
//    	println("More specific concrete methods and domains: " + moreSpecificConcreteMethodsAndDomains)
//    	println("Domain union of " + domainList + " is " + domainUnion)
        if (!tsa.subtypeED(thisDomain, domainUnion)) {
  	  println("Domain " + thisDomain + " is not a subtype of " + domainUnion)
	  error(NU.getSpan(od),
		"The inherited abstract method " + meth.asInstanceOf[HasSelfType].ast + " from the trait " + tt +
		"\n    has no concrete implementation in the object " + tth.getName +
		" in component " + componentName + ".")
        }
      }
    }
    typeAnalyzer = oldTypeAnalyzer
  }

  private def traceObjectExprMethods(o: ObjectExpr, ms: Relation[IdOrOpOrAnonymousName, (JavaFunctional, StaticTypeReplacer, TraitType)]) = {
    println(ms.size + " method" + (if (ms.size == 1) "" else "s") + " for object expression " + o)
    for { (meth, _, tt) <- ms.secondSet } println(tt + ": " + meth)
  }

  // This doesn't work, but the eventual plan is to lift all object expressions to become top-level objects with generated names.
  private def checkObjectExpression(oe: ObjectExpr) = {
    // An object expression does not have static parameters, so no need to make a new TypeAnalyzer.
    val tth = oe.getHeader
    val methods = allMethodsOfObjectExpr(oe, typeAnalyzer)
    // traceObjectExprMethods(oe, methods)
    // If any method inherited and not overridden is abstract,
    // then there was a failure to provide a concrete definition.
    for ((meth, _, tt) <- methods.secondSet) {
      if (isAbstractMethod(meth, tt)) {
// TODO: Unfortunately, this code does not work properly.  The typechecker needs for object expression types to have names.
//         error(NU.getSpan(oe),
//               "The inherited abstract method " + meth.asInstanceOf[HasSelfType].ast + " from the trait " + tt +
//               "\n    has no concrete implementation in the object expression in component " + componentName + ".")
      }
    }
  }

  override def walk(node:Any):Any = {
    node match {
      case o@SObjectExpr(SExprInfo(span, _, _),
                         STraitTypeHeader(_, _, name, _, _, _, extendsC, _, decls),
                         _) => {
        checkObjectExpression(o)
        val methods = inheritedMethods(extendsC, typeAnalyzer)
        val inheritedNames = toSet(methods.firstSet)
                        .map(_.asInstanceOf[IdOrOpOrAnonymousName])
        for {
          d <- decls;
          if d.isInstanceOf[FnDecl];
          val f = d.asInstanceOf[FnDecl];
          if NU.isFunctionalMethod(NU.getParams(f));
          if !inheritedNames.contains(NU.getName(f))
        } error(NU.getSpan(d),
                "Object expressions should not define any new functional methods.")
      }
      case _ => super.walk(node)
    }
    node
  }

  private def isAbstractMethod(meth: JavaFunctional, tt: TraitType): Boolean = {
    meth match {
      case f: JavaFunctionalMethod => isAbstractFnDecl(f.ast, tt)
      case m: JavaDeclaredMethod => isAbstractFnDecl(m.ast, tt)
      case gs: FieldGetterOrSetterMethod =>
	  if (gs.fnDecl.isSome)
	      isAbstractFnDecl(gs.fnDecl.unwrap, tt)
	  else
	      gs.ast.getMods.isAbstract
    }
  }

  private def isAbstractFnDecl(f: FnDecl, tt: TraitType): Boolean = {
    val SFnDecl(_,SFnHeader(_,mods,name,_,_,_,_,_),_,body,_) = f
    /* BUGFIX: http://java.net/jira/browse/PROJECTFORTRESS-3 -- it's the trait
     * that should have no associated API, NOT the method name. */
    mods.isAbstract || ((!body.isDefined) && tt.getName.getApiName.isNone)
  }

}
