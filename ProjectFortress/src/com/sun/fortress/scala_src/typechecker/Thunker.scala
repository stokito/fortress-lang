/*******************************************************************************
    Copyright 2009 Sun Microsystems, Inc.,
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

import _root_.java.util.{Set => JavaSet}
import com.sun.fortress.compiler.index.{Unit=>JUnit,_}
import com.sun.fortress.exceptions.InterpreterBug.bug
import com.sun.fortress.nodes._
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.ErrorLog
import com.sun.fortress.scala_src.useful.Sets._
import edu.rice.cs.plt.collect.Relation
import edu.rice.cs.plt.lambda.Thunk
import edu.rice.cs.plt.collect.UnionRelation
import edu.rice.cs.plt.tuple.{Option => JOption}
import scala.collection.mutable.Stack


class Thunker(var typeChecker: STypeChecker, val errors: ErrorLog) {
  
  protected val cycleChecker = new CyclicReferenceChecker(errors)
  
  type TypeThunk = Thunk[JOption[Type]]
  
  def makeThunk(ast: FnDecl, tryChecker: TryChecker): TypeThunk = {
    val name = ast.getHeader.getName
    return new TypeThunk() {
      def value() = {
        if (cycleChecker.push(name)) {
          tryChecker.tryCheck(ast) match {
            case Some(ast) => cycleChecker.pop()
                              //Any function without a body must have a return type
                              val body = toOption(ast.asInstanceOf[FnDecl].getBody).get
                              body.getInfo.getExprType
            case None =>  cycleChecker.pop
                          none[Type]
          }
        }
        else
          none[Type]
      }
    }
   }
    
  def primeFunctional[T <: Functional](fn: T, tryChecker: TryChecker): Unit = {
    if(fn.hasThunk) return
    fn match{
      case m:Method => m.putThunk(makeThunk(m.ast.asInstanceOf[FnDecl], tryChecker))
      case d:DeclaredFunction => d.putThunk(makeThunk(d.ast.asInstanceOf[FnDecl], tryChecker))
      case f:FunctionalMethod => f.putThunk(makeThunk(f.ast.asInstanceOf[FnDecl], tryChecker))
      case _ => 
    }
  }
  
  def primeFunctionals[T<:Functional](fns: JavaSet[T], tryChecker: TryChecker):Unit = {
    toSet(fns).foreach(f => primeFunctional(f,tryChecker))
  }
  
  def walk(node: Node):Unit = node match {
    
    case SComponent(info, name, imports, decls, comprises, isNative, exports) =>
      decls.map(walk(_))
   
    case t@STraitDecl(info,
                      STraitTypeHeader(sparams, mods, name, where, throwsC, contract, extendsC, decls),
                      excludes, comprises, hasEllipses, selfType) => {
      typeChecker = typeChecker.extend(sparams, where)
      // Add field declarations (getters/setters?) to method_checker
      typeChecker = decls.foldRight(typeChecker)
                                      {(d:Decl, c:STypeChecker) => d match {
                                        case SVarDecl(_,lhs,_) => c.extend(lhs)
                                        case _ => c }}
      toOption(typeChecker.traits.typeCons(name.asInstanceOf[Id])) match {
        case None => ()
        case Some(ti) =>
          // Extend method checker with methods and functions
          // that will now be in scope
          val inheritedMethods = typeChecker.inheritedMethods(extendsC)
          val functionalMethods = ti.asInstanceOf[TraitIndex].functionalMethods
          val dottedMethods = ti.asInstanceOf[TraitIndex]
                                .dottedMethods.asInstanceOf[Relation[IdOrOpOrAnonymousName, Method]]
          val methods = new UnionRelation(inheritedMethods,
                                          dottedMethods)
          typeChecker = typeChecker.extendWithFunctions(methods)
          typeChecker = typeChecker.extendWithFunctions(ti.asInstanceOf[TraitIndex].functionalMethods)
          // Extend method checker with self
          selfType match {
            case Some(ty) =>
              typeChecker = typeChecker.addSelf(ty)
            case _ =>
          }
          //Create a tryChecker
          val tryChecker = makeTryChecker()
          //Prime all the dotted and functional method indices)
          primeFunctionals(dottedMethods.secondSet,tryChecker)
          primeFunctionals(functionalMethods.secondSet,tryChecker)
      }
    }
    
    case o@SObjectDecl(info,
                       STraitTypeHeader(sparams, mods, name, where, throwsC, contract, extendsC, decls),
                       params, selfType) => {
      typeChecker = typeChecker.extend(sparams, where)
      // Add field declarations (getters/setters?) to method_checker
      typeChecker = decls.foldRight(typeChecker)
                                      {(d:Decl, c:STypeChecker) => d match {
                                        case SVarDecl(_,lhs,_) => c.extend(lhs)
                                        case _ => c }}
      toOption(typeChecker.traits.typeCons(name.asInstanceOf[Id])) match {
        case None => ()
        case Some(to) =>
          // Extend method checker with methods and functions
          // that will now be in scope
          val inheritedMethods = typeChecker.inheritedMethods(extendsC)
          val functionalMethods = to.asInstanceOf[TraitIndex].functionalMethods
          val dottedMethods = to.asInstanceOf[TraitIndex]
                                .dottedMethods.asInstanceOf[Relation[IdOrOpOrAnonymousName, Method]]
          val methods = new UnionRelation(inheritedMethods,
                                          dottedMethods)
          typeChecker = typeChecker.extendWithFunctions(methods)
          typeChecker = typeChecker.extendWithFunctions(to.asInstanceOf[TraitIndex].functionalMethods)
          // Extend method checker with self
          selfType match {
            case Some(ty) =>
              typeChecker = typeChecker.addSelf(ty)
            case _ =>
          }
          //Create a TryChecker
          val tryChecker = makeTryChecker()
          //Prime all the dotted and functional method indices
          primeFunctionals(dottedMethods.secondSet,tryChecker)
          primeFunctionals(functionalMethods.secondSet,tryChecker)
    }
  }
    
    case _ =>  
  }
  
  def makeTryChecker(): TryChecker = new 
    TryChecker(typeChecker.current,typeChecker.traits,typeChecker.env)(typeChecker.analyzer)
  
}

class CyclicReferenceChecker(val errors: ErrorLog) {
  
  protected val stack = new Stack[Name]
  
  def push(name: Name): Boolean = {
    // check if in the stack; if so, error. else push
    if (stack.contains(name)) {
      val cycle = stack.takeWhile(_ != name) ++ List(name)
      val cycleStr = cycle.mkString(", ")
      errors.signal("Cannot infer return type for functional %s because it has reference cycle %s".format(name, cycleStr), name)
      false
    } else {
      stack.push(name)
      true
    }
  }
  
  def pop(): Name = stack.pop
  
}