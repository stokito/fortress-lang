/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.scala_src.typechecker.impls

import edu.rice.cs.plt.collect.Relation
import edu.rice.cs.plt.collect.UnionRelation
import com.sun.fortress.compiler.index.DeclaredFunction
import com.sun.fortress.compiler.index.Method
import com.sun.fortress.compiler.index.TraitIndex
import com.sun.fortress.compiler.index.TypeConsIndex
import com.sun.fortress.compiler.Types
import com.sun.fortress.exceptions.InterpreterBug.bug
import com.sun.fortress.exceptions.StaticError.errorMsg
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.{ExprFactory => EF}
import com.sun.fortress.nodes_util.{NodeFactory => NF}
import com.sun.fortress.nodes_util.{NodeUtil => NU}
import com.sun.fortress.scala_src.typechecker._
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.Sets._
import com.sun.fortress.scala_src.useful.SExprUtil._
import com.sun.fortress.scala_src.useful.STypesUtil._

/**
 * Provides the implementation of cases relating to declarations.
 *
 * This trait must be mixed in with an `STypeChecker with Common` instance
 * in order to provide the full type checker implementation.
 *
 * (The self-type annotation at the beginning declares that this trait must be
 * mixed into STypeChecker along with the Common helpers. This is what
 * allows this trait to implement abstract members of STypeChecker and to
 * access its protected members.)
 */
trait Decls { self: STypeChecker with Common =>

  // ---------------------------------------------------------------------------
  // HELPER METHODS ------------------------------------------------------------

  /** Check the body exprs of a LetExpr. */
  protected def checkLetBody(bodyChecker: STypeChecker,
                             body: Block)
                             : Option[(Block, Option[Type])] = {

    // Check the body exprs and make sure all but the last have type ().
    val newBody = bodyChecker.checkExpr(body).asInstanceOf[Block].getExprs
    val newBlock = toListFromImmutable(newBody)
    if (!haveTypes(newBlock)) return None
    for (e <- newBlock.dropRight(1)) {
      isSubtype(getType(e).get,
                Types.VOID,
                e,
                errorString("Non-last expression in a block"))
    }

    // The type of the body is either () or the type of the last element.
    val newType = if (newBody.isEmpty) Some(Types.VOID) else getType(newBlock.last)
    Some((EF.makeBlock(NU.getSpan(body), newBody), newType))
  }

  // ---------------------------------------------------------------------------
  // CHECK IMPLEMENTATION ------------------------------------------------------

  def checkDecls(node: Node): Node = node match {

    case SComponent(info, name, imports, decls, comprises, isNative, exports) =>
      SComponent(info, name, imports,
                 decls.map((n:Decl) => check(n).asInstanceOf[Decl]), comprises,
                 isNative, exports)

    case t@STraitDecl(info,
                      STraitTypeHeader(sparams, mods, name, where,
                                       throwsC, contract, extendsC, params, decls),
                      selfType, excludes, comprises, hasEllipses) => {
      val checkerWSparams: STypeChecker = this.extend(sparams, params, where)
      var method_checker = checkerWSparams
      var field_checker = checkerWSparams
      // Add field declarations (getters/setters?) to method_checker
      method_checker = decls.foldRight(method_checker)
                                      { (d:Decl, c:STypeChecker) => d match {
                                        case SVarDecl(_,lhs,_) => c.extend(lhs)
                                        case _ => c } }
      toOption(traits.typeCons(name.asInstanceOf[Id])) match {
        case None => signal(name, errorMsg(name, " is not found.")); t
        case Some(ti) =>
          // Extend method checker with methods and functions
          // that will now be in scope
          val inheritedMethods = commonInheritedMethods(extendsC, analyzer.traits)
          val dottedMethods = toSet(ti.asInstanceOf[TraitIndex].dottedMethods.secondSet)
          val methods = inheritedMethods ++ dottedMethods
          method_checker = method_checker.extendWithListOfFunctions(methods)
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
                                          throwsC, contract, extendsC, params, newDecls),
                         selfType, excludes, comprises, hasEllipses)
            case _ => signal(t, errorMsg("Self type is not inferred for ", t)); t
          }
      }
    }

    case o@SObjectDecl(info,
                       STraitTypeHeader(sparams, mods, name, where,
                                        throwsC, contract, extendsC, params, decls),
                       selfType) => {
      val checkerWSparams: STypeChecker = this.extend(sparams, params, where)
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
      toOption(traits.typeCons(name.asInstanceOf[Id])) match {
        case None => signal(name, errorMsg(name, " is not found.")); o
        case Some(oi) =>
          // Extend type checker with methods and functions
          // that will now be in scope as regular functions
          val dottedMethods = toSet(oi.asInstanceOf[TraitIndex].dottedMethods.secondSet)
          val inheritedMethods = commonInheritedMethods(extendsC, analyzer.traits)
          val methods = inheritedMethods ++ dottedMethods
          method_checker = method_checker.extendWithListOfFunctions(methods)
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
                                           throwsC, newContract, extendsC, params, newDecls),
                          selfType)
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
          if ( NU.isSetter(f) )
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
      val newChecker = this.extend(statics, Some(params), wheres)
      val newContract = contract.map(c => newChecker.check(c))

      // If setter decl and no return type given, make it void.
      val returnType =
        if (rType.isEmpty && NU.isSetter(f))
          Some(Types.VOID)
        else
          rType

      // Get the new return type and body.
      val (newReturnType, newBody) = returnType match {

        // If there is a declared return type, check the body, expecting this
        // type. If this is a setter, check that the return type is a void too.
        case Some(rt) =>
          if (NU.isSetter(f))
            isSubtype(rt, Types.VOID, f, "Setter declarations must return void.")
          (Some(rt), newChecker.checkExpr(body, rt, errorString("Function body",
                                                                "declared return")))

        case None =>
          val newBody = newChecker.checkExpr(body)
          val rt = if (NU.isCoercion(f)) None else getType(newBody)
          (rt, newBody)
      }

      SFnDecl(info,
              SFnHeader(statics, mods, name, wheres, throws,
                        newContract.asInstanceOf[Option[Contract]],
                        params, newReturnType),
              unambiguousName, Some(newBody), implementsUnambiguousName)
    }

    case v@SVarDecl(info, lhses, None) => v

    case d@SVarDecl(info, lhses, Some(rhs))
      if (lhses.length > 1 && rhs.isInstanceOf[TupleExpr] &&
          rhs.asInstanceOf[TupleExpr].getExprs.size == lhses.length) => {
        val pairs =
          (lhses, toListFromImmutable(rhs.asInstanceOf[TupleExpr].getExprs)).zipped.map((lv, r) => toOption(lv.getIdType) match {
            case Some(pt) => pt match {
              case p@SPattern(_,_,_) =>
                bug("Pattern should be desugared away: " + p)
              case t@SType(_) =>
          //System.out.println("Losing rhs: " + r)
                (lv, checkExpr(r, t, errorString("Right-hand side", "declared")))
            }
            case None => lv match {
              case SLValue(info, name, mods, None, mut) =>
                val newR = checkExpr(r, None)
                (SLValue(info, name, mods,
                         Some(getType(newR).getOrElse(return node)), mut),
                 newR)
              case _ => return node
            }
          })
        val (newLhses, newRhses) = (pairs.map(_._1), pairs.map(_._2))
        SVarDecl(info, newLhses,
                 Some(EF.makeTupleExpr(NU.getSpan(rhs), toJavaList(newRhses))))
    }

    case d@SVarDecl(info, lhses, Some(rhs)) => {
      // Gather declared types of LHS as a big tuple type.
      val declaredTypes = lhses.flatMap(lv => toOption(lv.getIdType) match {
                                        case Some(pt) => pt match {
                                          case p@SPattern(_,_,_) =>
                                            bug("Pattern should be desugared away: " + p)
                                          case t@SType(_) => Some(t)
                                        }
                                        case None => None
                                        })
      val declaredType =
        if (declaredTypes.length == lhses.length)
          Some(NF.makeMaybeTupleType(NU.getSpan(d), toJavaList(declaredTypes)))
        else
          None
      // Type check the RHS, expecting the declared type.
      val (newLhses, newRhs) = declaredType match {
        // If there is a declared type, just check the RHS expecting that.
        case Some(typ) =>
          //System.out.println("Losing rhs: " + rhs)
          (lhses, checkExpr(rhs, typ, errorString("Right-hand side", "declared")))
        // If there is not a declared type, check the RHS and assign LHS types
        // from that.
        case None =>
          // Check the RHS.
          val newRhs = checkExpr(rhs, None)
          val rhsType = getType(newRhs).getOrElse(return node)
          // Get all the LHSes and their corresponding RHS type.
          val lhsAndRhsTypes = lhses match {
            case List(lhs) => List((lhs, rhsType))
            case _ =>
              if (!enoughElementsForType(lhses, rhsType)) {
                signal(node, "Right-hand side has type %s, but left-hand side is a tuple of %d variables.".format(rhsType, lhses.size))
                return node
              }
              zipWithRhsType(lhses, rhsType)
          }
          // Map over the LHS/RHS pairs to create the new list of LHSes.
          val newLhses = lhsAndRhsTypes.map {
            // No type on LHS -- just insert it.
            case (SLValue(info, name, mods, None, mut), rhsType) =>
              SLValue(info, name, mods, Some(rhsType), mut)
            case (lhs, _) => lhs
          }
          (newLhses, newRhs)
        }
      SVarDecl(info, newLhses, Some(newRhs))
    }

    case _ => throw new Error(errorMsg("not yet implemented: ", node.getClass))
  }

  // ---------------------------------------------------------------------------
  // CHECKEXPR IMPLEMENTATION --------------------------------------------------

  def checkExprDecls(expr: Expr, expected: Option[Type]): Expr = expr match {

    case d@SLocalVarDecl(SExprInfo(span, paren,_), body, lhses, None) => {
      // Extend typechecker with new bindings from the RHS types
      val newChecker = this.extend(lhses)
      // Check the LetExpr body.
      val (newBody, newType) = checkLetBody(newChecker, body).getOrElse(return expr)
      SLocalVarDecl(SExprInfo(span, paren, newType), newBody, lhses, None)
    }

    case d@SLocalVarDecl(SExprInfo(span, paren,_), body, lhses, Some(rhs))
      if (lhses.length > 1 && rhs.isInstanceOf[TupleExpr] &&
          rhs.asInstanceOf[TupleExpr].getExprs.size == lhses.length) => {
        val pairs =
          (lhses, toListFromImmutable(rhs.asInstanceOf[TupleExpr].getExprs)).zipped.map((lv, r) => toOption(lv.getIdType) match {
            case Some(pt) => pt match {
              case p@SPattern(_,_,_) =>
                bug("Pattern should be desugared away: " + p)
              case t@SType(_) =>
          //System.out.println("Losing rhs: " + rhs)
                (lv, checkExpr(r, t, errorString("Right-hand side", "declared")))
            }
            case None => lv match {
              case SLValue(info, name, mods, None, mut) =>
                val newR = checkExpr(r, None)
                (SLValue(info, name, mods,
                         Some(getType(newR).getOrElse(return expr)), mut),
                 newR)
              case _ => return expr
            }
          })
        val (newLhses, newRhses) = (pairs.map(_._1), pairs.map(_._2))
        val newRhs = EF.makeTupleExpr(span, toJavaList(newRhses))
        // Extend typechecker with new bindings from the RHS types
        val newChecker = this.extend(newLhses)
        // Check the LetExpr body
        val (newBody, newType) = checkLetBody(newChecker, body).getOrElse(return expr)
      SLocalVarDecl(SExprInfo(span, paren, newType), newBody, newLhses, Some(newRhs))
    }

    case d@SLocalVarDecl(SExprInfo(span, paren, _), body, lhses, Some(rhs)) => {
      // Gather declared types of LHS as a big tuple type.
      val declaredTypes = lhses.flatMap(lv => toOption(lv.getIdType) match {
                                        case Some(pt) => pt match {
                                          case p@SPattern(_,_,_) =>
                                            bug("Pattern should be desugared away: " + p)
                                          case t@SType(_) => Some(t)
                                        }
                                        case None => None
                                        })
      val declaredType =
        if (declaredTypes.length == lhses.length)
          Some(NF.makeMaybeTupleType(NU.getSpan(d), toJavaList(declaredTypes)))
        else
          None
      // Type check the RHS, expecting the declared type.
      val (newLhses, newRhs) = declaredType match {
        // If there is a declared type, just check the RHS expecting that.
        case Some(typ) =>
          //System.out.println("Losing rhs: " + rhs)
          (lhses, checkExpr(rhs, typ, errorString("Right-hand side", "declared")))
        // If there is not a declared type, check the RHS and assign LHS types
        // from that.
        case None =>
          // Check the RHS.
          val newRhs = checkExpr(rhs, None)
          val rhsType = getType(newRhs).getOrElse(return expr)
          // Get all the LHSes and their corresponding RHS type.
          val lhsAndRhsTypes = lhses match {
            case List(lhs) => List((lhs, rhsType))
            case _ =>
              if (!enoughElementsForType(lhses, rhsType)) {
                signal(expr, "Right-hand side has type %s, but left-hand side is a tuple of %d variables.".format(rhsType, lhses.size))
                return expr
              }
              zipWithRhsType(lhses, rhsType)
          }
          // Map over the LHS/RHS pairs to create the new list of LHSes.
          val newLhses = lhsAndRhsTypes.map {
            // No type on LHS -- just insert it.
            case (SLValue(info, name, mods, None, mut), rhsType) =>
              SLValue(info, name, mods, Some(rhsType), mut)
            case (lhs, _) => lhs
          }
          (newLhses, newRhs)
        }
      // Extend typechecker with new bindings from the RHS types
      val newChecker = this.extend(newLhses)
      // Check the LetExpr body.
      val (newBody, newType) = checkLetBody(newChecker, body).getOrElse(return expr)
      SLocalVarDecl(SExprInfo(span, paren, newType), newBody, newLhses, Some(newRhs))
    }

    case SLetFn(SExprInfo(span, paren, _), body, fns) => {
      // Extend with functions
      val fnIndices = fns.map(new DeclaredFunction(_))
      val newChecker = this.extendWithListOfFunctions(fnIndices)

      // Prime functionals to infer return types.
      Thunker.primeFunctionals(fnIndices, STypeCheckerFactory.makeTryChecker(this))

      // Check the contained body and FnDecls.
      val (newBody, newType) = checkLetBody(newChecker, body).getOrElse(return expr)
      val newFns = fns.map(newChecker.check(_).asInstanceOf[FnDecl])

      SLetFn(SExprInfo(span, paren, newType), newBody, newFns)
    }

    case _ => throw new Error(errorMsg("Not yet implemented: ", expr.getClass))
  }
}
