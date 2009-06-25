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

package com.sun.fortress.scala_src.typechecker.impls

import edu.rice.cs.plt.collect.Relation
import edu.rice.cs.plt.collect.UnionRelation
import com.sun.fortress.compiler.index.Method
import com.sun.fortress.compiler.index.TraitIndex
import com.sun.fortress.compiler.index.TypeConsIndex
import com.sun.fortress.compiler.Types
import com.sun.fortress.exceptions.StaticError.errorMsg
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.NodeFactory
import com.sun.fortress.nodes_util.NodeUtil
import com.sun.fortress.scala_src.typechecker._
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.Sets._
import com.sun.fortress.scala_src.useful.SExprUtil._

/**
 * Provides the implementation of cases relating to declarations.
 * 
 * This trait must be mixed in with an `STypeCheckerBase with Common` instance
 * in order to provide the full type checker implementation.
 * 
 * (The self-type annotation at the beginning declares that this trait must be
 * mixed into STypeCheckerBase along with the Common helpers. This is what
 * allows this trait to implement abstract members of STypeCheckerBase and to
 * access its protected members.)
 */
trait Decls { self: STypeCheckerBase with Common =>

  // ---------------------------------------------------------------------------
  // CHECK IMPLEMENTATION ------------------------------------------------------
  
  def checkDecls(node: Node): Node = node match {
    
    case SComponent(info, name, imports, decls, comprises, isNative, exports) =>
      SComponent(info, name, imports,
                 decls.map((n:Decl) => check(n).asInstanceOf[Decl]), comprises,
                 isNative, exports)

    case t@STraitDecl(info,
                      STraitTypeHeader(sparams, mods, name, where,
                                       throwsC, contract, extendsC, decls),
                      excludes, comprises, hasEllipses, selfType) => {
      val checkerWSparams = this.extend(sparams, where)
      var method_checker = checkerWSparams
      var field_checker = checkerWSparams
      // Add field declarations (getters/setters?) to method_checker
      method_checker = decls.foldRight(method_checker)
                                      { (d:Decl, c:STypeChecker) => d match {
                                        case SVarDecl(_,lhs,_) => c.extend(lhs)
                                        case _ => c } }
      toOption(traits.typeCons(name.asInstanceOf[Id])).asInstanceOf[Option[TypeConsIndex]] match {
        case None => signal(name, errorMsg(name, " is not found.")); t
        case Some(ti) =>
          // Extend method checker with methods and functions
          // that will now be in scope
          val methods = new UnionRelation(inheritedMethods(extendsC),
                                          ti.asInstanceOf[TraitIndex].dottedMethods.asInstanceOf[Relation[IdOrOpOrAnonymousName, Method]])
          method_checker = method_checker.extendWithMethods(methods)
          method_checker = method_checker.extendWithFunctions(ti.asInstanceOf[TraitIndex].functionalMethods)
          // Extend method checker with self
          selfType match {
            case Some(ty) =>
              method_checker = method_checker.addSelf(ty)
              // Check declarations
              val newDecls = decls.map( (d:Decl) => d match {
                                        case SFnDecl(_,_,_,_,_) =>
                                          // methods see extra variables in scope
                                          method_checker.check(d).asInstanceOf[Decl]
                                        case SVarDecl(_,lhs,_) =>
                                          // fields see other fields
                                          val newD = field_checker.check(d).asInstanceOf[Decl]
                                          field_checker = field_checker.extend(lhs)
                                          newD
                                        case _ => checkerWSparams.check(d).asInstanceOf[Decl] } )
              STraitDecl(info,
                         STraitTypeHeader(sparams, mods, name, where,
                                          throwsC, contract, extendsC, newDecls),
                         excludes, comprises, hasEllipses, selfType)
            case _ => signal(t, errorMsg("Self type is not inferred for ", t)); t
          }
      }
    }

    case o@SObjectDecl(info,
                       STraitTypeHeader(sparams, mods, name, where,
                                        throwsC, contract, extendsC, decls),
                       params, selfType) => {
      val checkerWSparams = this.extend(sparams, params, where)
      var method_checker = checkerWSparams
      var field_checker = checkerWSparams
      val newContract = contract match {
        case Some(e) => Some(method_checker.check(e).asInstanceOf[Contract])
        case _ => contract
      }
      // Extend method checker with fields
      method_checker = decls.foldRight(method_checker)
                                      { (d:Decl, c:STypeChecker) => d match {
                                        case SVarDecl(_,lhs,_) => c.extend(lhs)
                                        case _ => c } }
      // Check method declarations.
      toOption(traits.typeCons(name.asInstanceOf[Id])).asInstanceOf[Option[TypeConsIndex]] match {
        case None => signal(name, errorMsg(name, " is not found.")); o
        case Some(oi) =>
          // Extend type checker with methods and functions
          // that will now be in scope as regular functions
          val methods = new UnionRelation(inheritedMethods(extendsC),
                                          oi.asInstanceOf[TraitIndex].dottedMethods.asInstanceOf[Relation[IdOrOpOrAnonymousName,Method]])
          method_checker = method_checker.extendWithMethods(methods)
          method_checker = method_checker.extendWithFunctions(oi.asInstanceOf[TraitIndex].functionalMethods)
          // Extend method checker with self
          selfType match {
            case Some(ty) =>
              method_checker = method_checker.addSelf(ty)
              // Check declarations, storing them in the same order
              val newDecls = decls.map( (d:Decl) => d match {
                                        case SFnDecl(_,_,_,_,_) =>
                                          // Methods get some extra vars in their declarations
                                          method_checker.check(d).asInstanceOf[Decl]
                                        case SVarDecl(_,lhs,_) =>
                                          // Fields get to see earlier fields
                                          val newD = field_checker.check(d).asInstanceOf[Decl]
                                          field_checker = field_checker.extend(lhs)
                                          newD
                                        case _ => checkerWSparams.check(d).asInstanceOf[Decl] } )
              SObjectDecl(info,
                          STraitTypeHeader(sparams, mods, name, where,
                                           throwsC, newContract, extendsC, newDecls),
                          params, selfType)
            case _ => signal(o, errorMsg("Self type is not inferred for ", o)); o
          }
      }
    }

    /* Matches if a function declaration does not have a body expression. */
    case f@SFnDecl(info,
                   SFnHeader(statics,mods,name,wheres,throws,contract,params,returnType),
                   unambiguousName, None, implementsUnambiguousName) => {
      returnType match {
        case Some(ty) =>
          if ( NodeUtil.isSetter(f) )
            isSubtype(ty, Types.VOID, f, "Setter declarations must return void.")
        case _ =>
      }
      f
    }

    /* Matches if a function declaration has a body expression. */
    // @TODO: Only change return type of FnHeader if it was an inf var.
    case f@SFnDecl(info,
                   SFnHeader(statics,mods,name,wheres,throws,contract,params,rType),
                   unambiguousName, Some(body), implementsUnambiguousName) => {
      val newChecker = this.extend(env.extendWithStaticParams(statics).extendWithParams(params),
                                   analyzer.extend(statics, wheres))
      val newContract = contract.map(c => newChecker.check(c))

      // If setter decl and no return type given, make it void.
      val returnType =
        if (rType.isEmpty && NodeUtil.isSetter(f))
          Some(Types.VOID)
        else
          rType

      // Get the new return type and body.
      val (newReturnType, newBody) = returnType match {

        // If there is a declared return type, check the body, expecting this
        // type. If this is a setter, check that the return type is a void too.
        case Some(rt) =>
          if (NodeUtil.isSetter(f))
            isSubtype(rt, Types.VOID, f, "Setter declarations must return void.")
          (Some(rt), newChecker.checkExpr(body, rt, errorString("Function body",
                                                                "declared return")))

        case None =>
          val newBody = newChecker.checkExpr(body)
          (getType(newBody), newBody)
      }

      SFnDecl(info,
              SFnHeader(statics, mods, name, wheres, throws,
                        newContract.asInstanceOf[Option[Contract]],
                        params, newReturnType),
              unambiguousName, Some(newBody), implementsUnambiguousName)
    }

    case v@SVarDecl(info, lhs, body) => body match {
      case Some(init) =>
        val newInit = checkExpr(init)
        val ty = lhs match {
          case l::Nil => // We have a single variable binding, not a tuple binding
            toOption(l.getIdType).asInstanceOf[Option[Type]] match {
              case Some(typ) => typ
              case _ => // Eventually, this case will involve type inference
                signal(v, errorMsg("All inferrred types should at least be inference ",
                                   "variables by typechecking: ", v))
                NodeFactory.makeVoidType(NodeUtil.getSpan(l))
            }
          case _ =>
            def handleBinding(binding: LValue) =
              toOption(binding.getIdType).asInstanceOf[Option[Type]] match {
                case Some(typ) => typ
                case _ =>
                  signal(binding, errorMsg("Missing type for ", binding, "."))
                  NodeFactory.makeVoidType(NodeUtil.getSpan(binding))
              }
            NodeFactory.makeTupleType(NodeUtil.getSpan(v),
                                      toJavaList(lhs.map(handleBinding)))
        }
        getType(newInit) match {
          case Some(typ) =>
            val left = lhs match {
              case hd::Nil => hd
              case _ => lhs
            }
            isSubtype(typ, ty, v,
                         errorMsg("Attempt to define variable ", left,
                                  " with an expression of type ", normalize(typ)))
          case _ =>
            signal(v, errorMsg("The right-hand side of ", v, " could not be typed."))
        }
        SVarDecl(info, lhs, Some(newInit))
      case _ => v
    }

    case _ => throw new Error(errorMsg("not yet implemented: ", node.getClass))
  }
  
  // ---------------------------------------------------------------------------
  // CHECKEXPR IMPLEMENTATION --------------------------------------------------

  def checkExprDecls(expr: Expr, expected: Option[Type]): Expr = expr match {

    case d@SLocalVarDecl(SExprInfo(span,paren,_), body, lhs, rhs) => {
      val newRhs = rhs.map(checkExpr)
      val newLhs = newRhs match {
        case Some(e) => getType(e) match {
          case Some(typ@STupleType(_,elts,_,_)) =>
            if ( lhs.size != elts.size ) {
              signal(expr, errorMsg("The size of right-hand side, ", typ,
                                    ", does not match with the size of left-hand side."))
              return expr
            }
            lhs.zip(elts).map( (p:(LValue,Type)) => p._1 match {
                               case SLValue(i,n,m,None,mt) =>
                                 SLValue(i,n,m,Some(p._2),mt)
                               case SLValue(i,n,m,Some(t),mt) =>
                                 isSubtype(p._2, t, p._1,
                                           errorMsg("Right-hand side, ", p._2,
                                                    ", must be a subtype of left-hand side, ",
                                                    t, "."))
                                 p._1 } )
          case Some(typ) => lhs match {
            case List(SLValue(i,name,mods,Some(idType),mutable)) =>
              isSubtype(typ, idType, expr,
                        errorMsg("Right-hand side, ", typ,
                                 ", must be a subtype of left-hand side, ", idType, "."))
              lhs
            case List(SLValue(i,name,mods,None,mutable)) =>
              List(SLValue(i,name,mods,Some(typ),mutable))
            case _ =>
              signal(expr, errorMsg("Right-hand side, ", typ,
                                    ", is not a tuple type but left-hand side ",
                                    "declares multiple variables."))
              return expr
          }
          case _ => return expr
        }
        case _ => lhs
      }

      // Extend typechecker with new bindings from the RHS types
      val newChecker = this.extend(d)
      // A LocalVarDecl is like a let. It has a body, and its type is the type of the body
      val newBody = body.map(newChecker.checkExpr)
      if (!haveTypes(newBody)) { return expr }
      newBody.dropRight(1).foreach(e => isSubtype(getType(e).get, Types.VOID, e,
                                                  errorString("Non-last expression in a block")))
      val newType = body.size match {
        case 0 => Some(Types.VOID)
        case _ => getType(newBody.last)
      }
      SLocalVarDecl(SExprInfo(span,paren,newType), newBody, newLhs, newRhs)
    }

    case _ => throw new Error(errorMsg("Not yet implemented: ", expr.getClass))
  } 
}
