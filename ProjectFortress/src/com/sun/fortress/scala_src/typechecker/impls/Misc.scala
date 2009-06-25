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

import _root_.java.util.{List => JavaList}
import edu.rice.cs.plt.collect.EmptyRelation
import edu.rice.cs.plt.collect.IndexedRelation
import edu.rice.cs.plt.collect.Relation
import edu.rice.cs.plt.collect.UnionRelation
import com.sun.fortress.compiler.IndexBuilder
//import com.sun.fortress.compiler.disambiguator.ExprDisambiguator.HierarchyHistory
import com.sun.fortress.compiler.index.CompilationUnitIndex
import com.sun.fortress.compiler.index.{FunctionalMethod => JavaFunctionalMethod}
import com.sun.fortress.compiler.index.Method
import com.sun.fortress.compiler.index.ObjectTraitIndex
import com.sun.fortress.compiler.index.ProperTraitIndex
import com.sun.fortress.compiler.index.TraitIndex
import com.sun.fortress.compiler.index.TypeConsIndex
import com.sun.fortress.compiler.typechecker.TypeNormalizer
//import com.sun.fortress.compiler.typechecker.StaticTypeReplacer
import com.sun.fortress.compiler.typechecker.TypeAnalyzer
import com.sun.fortress.compiler.typechecker.TypeEnv
import com.sun.fortress.compiler.Types
import com.sun.fortress.exceptions.InterpreterBug.bug
import com.sun.fortress.exceptions.StaticError
import com.sun.fortress.exceptions.StaticError.errorMsg
import com.sun.fortress.exceptions.TypeError
import com.sun.fortress.exceptions.ProgramError
import com.sun.fortress.exceptions.ProgramError.error
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.NodeFactory
import com.sun.fortress.nodes_util.ExprFactory
import com.sun.fortress.nodes_util.Modifiers
import com.sun.fortress.nodes_util.NodeUtil
import com.sun.fortress.nodes_util.OprUtil
import com.sun.fortress.scala_src.typechecker._
import com.sun.fortress.scala_src.typechecker.ScalaConstraintUtil._
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.useful.ASTGenHelper._
import com.sun.fortress.scala_src.useful.ErrorLog
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.Sets._
import com.sun.fortress.scala_src.useful.SExprUtil._
import com.sun.fortress.scala_src.useful.STypesUtil._
import com.sun.fortress.useful.HasAt
import com.sun.fortress.useful.NI

/**
 * Provides the implementation of miscellaneous cases that aren't found in any
 * of the other implementation groups.
 * 
 * This trait must be mixed in with an STypeCheckerBase instance to provide the
 * full type checker implementation.
 * 
 * (The self-type annotation at the beginning declares that this trait must be
 * mixed into STypeCheckerBase. This is what allows it to implement abstract
 * members of STypeCheckerBase and access its protected members.)
 */
trait Misc { self: STypeCheckerBase with Common =>

  // ---------------------------------------------------------------------------
  // HELPER METHODS ------------------------------------------------------------

  /**
   * The Java type checker had a separate postinference pass "closing bindings".
   * @TODO: Look over this method.
   */
  protected def generatorClauseGetBindings(clause: GeneratorClause,
                                           mustBeCondition: Boolean) = clause match {
    case SGeneratorClause(info, binds, init) =>
      val newInit = checkExpr(init)
      val err = errorMsg("Filter expressions in generator clauses must have type Boolean, ",
                         "but ", init)
      getType(newInit) match {
        case None =>
          signal(init, errorMsg(err, " was not well typed."))
          (SGeneratorClause(info, Nil, newInit), Nil)
        case Some(ty) =>
          isSubtype(ty, Types.BOOLEAN, init, errorMsg(err, " had type ", normalize(ty), "."))
          binds match {
            case Nil =>
              // If bindings are empty, then init must be of type Boolean, a filter, 13.14
              (SGeneratorClause(info, Nil, newInit), Nil)
            case hd::tl =>
              def mkInferenceVarType(id: Id) =
                NodeFactory.make_InferenceVarType(NodeUtil.getSpan(id))
              val (lhstype, bindings) = binds.length match {
                case 1 => // Just one binding
                  val lhstype = mkInferenceVarType(hd)
                  (lhstype, List[LValue](NodeFactory.makeLValue(hd, lhstype)))
                case n =>
                  // Because generator_type is almost certainly an _InferenceVar,
                  // we have to declare a new tuple that is the size of the bindings
                  // and declare one to be a subtype of the other.
                  val inference_vars = binds.map(mkInferenceVarType)
                  (Types.makeTuple(toJavaList(inference_vars)),
                   binds.zip(inference_vars).map((p:(Id,Type)) =>
                                                 NodeFactory.makeLValue(p._1,p._2)))
              }
              // Get the type of the Generator
              val infer_type = NodeFactory.make_InferenceVarType(NodeUtil.getSpan(init))
              val generator_type = if (mustBeCondition)
                                     Types.makeConditionType(infer_type)
                                   else Types.makeGeneratorType(infer_type)
              isSubtype(ty, generator_type, init,
                        errorMsg("Init expression of generator must be a subtype of ",
                                 (if (mustBeCondition) "Condition" else "Generator"),
                                 " but is type ", normalize(ty), "."))
              val err = errorMsg("If more than one variable is bound in a generator, ",
                                 "generator must have tuple type but ", init,
                                 " does not or has different number of arguments.")
              isSubtype(lhstype, generator_type, init, err)
              isSubtype(generator_type, lhstype, init, err)
              (SGeneratorClause(info, binds, newInit), bindings)
          }
      }
  }

  /**
   * @TODO: Look over this method.
   */
  protected def handleIfClause(c: IfClause) = c match {
    case SIfClause(info, testClause, body) =>
      // For generalized 'if' we must introduce new bindings.
      val (newTestClause, bindings) = generatorClauseGetBindings(testClause, true)
      // Check body with new bindings
      val newBody = this.extend(bindings).checkExpr(body).asInstanceOf[Block]
      SIfClause(info, newTestClause, newBody)
  }

  // For each generator clause, check its body,
  // then put its variables in scope for the next generator clause.
  // Finally, return all of the bindings so that they can be put in scope
  // in some larger expression, like the body of a for loop, for example.
  // @TODO: Look over this method.
  def handleGens(generators: List[GeneratorClause])
                 : (List[GeneratorClause], List[LValue]) = generators match {
                   
    case Nil => (Nil, Nil)
    case hd::Nil =>
      val (clause, binds) = generatorClauseGetBindings(hd, false)
      (List[GeneratorClause](clause), binds)
    case hd::tl =>
      val (clause, binds) = generatorClauseGetBindings(hd, false)
      val (newTl, tlBinds) = this.extend(binds).handleGens(tl)
      (clause::newTl, binds++tlBinds)
  }

  protected def forAtomic(expr: Expr, enclosingExpr: String) =
    new AtomicChecker(current,traits,env,analyzer,errors,enclosingExpr).checkExpr(expr)

  // ---------------------------------------------------------------------------
  // CHECK IMPLEMENTATION ------------------------------------------------------
  
  def checkMisc(node:Node):Node = node match {
    
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

    case id@SId(info,api,name) => {
      api match {
        case Some(_) => {
          val newName = handleAlias(id, toList(current.ast.getImports))
          getTypeFromName( newName ) match {
            case None =>
              // Operators are never qualified in source code,
              // so if 'name' is qualified and not found,
              // it must be an Id, not an Op.
              signal(id, errorMsg("Attempt to reference unbound variable: ", id))
            case _ => id
          }
        }
        case _ => {
          getTypeFromName( id ) match {
            case Some(ty) => ty match {
              case SLabelType(_) => // then, newName must be an Id
                signal(id, errorMsg("Cannot use label name ", id, " as an identifier."))
              case _ =>
            }
            case _ => signal(id, errorMsg("Variable '", id, "' not found."))
          }
        }
      }
      id
    }

    case op@SOp(info,api,name,fixity,enclosing) => {
      val tyEnv = api match {
        case Some(api) => getEnvFromApi(api)
        case _ => env
      }
      scalaify(tyEnv.binding(op)).asInstanceOf[Option[TypeEnv.BindingLookup]] match {
        case None =>
          if ( enclosing ) signal(op, errorMsg("Enclosing operator not found: ", op))
          else signal(op, errorMsg("Operator not found: ", OprUtil.decorateOperator(op)))
        case _ =>
      }
      op
    }

    case SLink(info,op,expr) => {
      SLink(info,checkExpr(op).asInstanceOf[FunctionalRef],checkExpr(expr))
    }

    case _ => throw new Error(errorMsg("not yet implemented: ", node.getClass))
  }
  
  // ---------------------------------------------------------------------------
  // CHECKEXPR IMPLEMENTATION --------------------------------------------------

  def checkExprMisc(expr: Expr, expected: Option[Type]): Expr = expr match {

    case o@SObjectExpr(SExprInfo(span,parenthesized,_),
                     STraitTypeHeader(sparams, mods, name, where,
                                      throwsC, contract, extendsC, decls),
                     selfType) => {
      // Verify that no extends clauses try to extend an object.
      extendsC.foreach( (t:TraitTypeWhere) =>
                        assertTrait(t.getBaseType,
                                    "Objects can only extend traits.", t.getBaseType) )
      var method_checker: STypeCheckerBase = self
      var field_checker: STypeCheckerBase = self
      val newContract = contract match {
        case Some(e) => Some(method_checker.check(e).asInstanceOf[Contract])
        case _ => contract
      }
      // Extend the type checker with all of the field decls
      method_checker = decls.foldRight(method_checker)
                                      { (d:Decl, c:STypeCheckerBase) => d match {
                                        case SVarDecl(_,lhs,_) => c.extend(lhs)
                                        case _ => c } }
      // Extend type checker with methods and functions
      // that will now be in scope as regular functions
      val oi = IndexBuilder.buildObjectExprIndex(o)
      val methods = new UnionRelation(inheritedMethods(extendsC),
                                      oi.asInstanceOf[ObjectTraitIndex].dottedMethods.asInstanceOf[Relation[IdOrOpOrAnonymousName, Method]])
      method_checker = method_checker.extendWithMethods(methods)
      method_checker = method_checker.extendWithFunctions(oi.asInstanceOf[ObjectTraitIndex].functionalMethods)
      // Extend method checker with self
      selfType match {
        case Some(ty) =>
          method_checker = method_checker.addSelf(ty)
          // Typecheck each declaration
          val newDecls = decls.map( (d:Decl) => d match {
                                    case SFnDecl(_,_,_,_,_) =>
                                      // Methods get a few more things in scope than everything else
                                      method_checker.check(d).asInstanceOf[Decl]
                                    case SVarDecl(_,lhs,_) =>
                                      // fields get to see earlier fields
                                      val newD = field_checker.check(d).asInstanceOf[Decl]
                                      field_checker = field_checker.extend(lhs)
                                      newD
                                    case _ => check(d).asInstanceOf[Decl] } )
          SObjectExpr(SExprInfo(span,parenthesized,Some(normalize(ty))),
                      STraitTypeHeader(sparams, mods, name, where,
                                       throwsC, newContract, extendsC, newDecls),
                      selfType)
        case _ => signal(o, errorMsg("Self type is not inferred for ", o)); o
      }
    }

    /* Matches if block is an atomic block. */
    case SBlock(SExprInfo(span,parenthesized,resultType),
                loc, true, withinDo, exprs) =>
      forAtomic(SBlock(SExprInfo(span,parenthesized,resultType),
                       loc, false, withinDo, exprs),
                "an 'atomic'do block")

    /* Matches if block is not an atomic block. */
    case SBlock(SExprInfo(span,parenthesized,resultType),
                loc, false, withinDo, exprs) => {
      val newLoc = loc match {
        case Some(l) =>
          Some(checkExpr(l, Some(Types.REGION), errorString("Location of the block")))
        case None => loc
      }
      exprs.reverse match {
        case Nil =>
          SBlock(SExprInfo(span,parenthesized,Some(Types.VOID)),
                 newLoc, false, withinDo, exprs)
        case last::rest =>
          val allButLast = rest.map((e: Expr) => checkExpr(e, Some(Types.VOID),
                                                           errorString("Non-last expression in a block")))
          val lastExpr = checkExpr(last)
          val newExprs = (lastExpr::allButLast).reverse
          SBlock(SExprInfo(span,parenthesized,getType(lastExpr)),
                 newLoc, false, withinDo, newExprs)
      }
    }

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

    case s@SSpawn(SExprInfo(span,paren,optType), body) => {
      val newExpr = this.extendWithout(s, labelExitTypes.keySet).checkExpr(body)
      getType(newExpr) match {
        case Some(typ) =>
          SSpawn(SExprInfo(span,paren,Some(Types.makeThreadType(typ))), newExpr)
        case _ => expr
      }
    }

    case SAtomicExpr(SExprInfo(span,paren,optType), body) => {
      val newExpr = forAtomic(body, "an 'atomic' expression")
      SAtomicExpr(SExprInfo(span,paren,getType(newExpr)), newExpr)
    }

    case STryAtomicExpr(SExprInfo(span,paren,optType), body) => {
      val newExpr = forAtomic(body, "a 'tryatomic' expression")
      STryAtomicExpr(SExprInfo(span,paren,getType(newExpr)), newExpr)
    }

    // For a tight juxt, create a MathPrimary
    case SJuxt(info, multi, infix, front::rest, false, true) => {
      def toMathItem(exp: Expr): MathItem = {
        val span = NodeUtil.getSpan(exp)
        if ( exp.isInstanceOf[TupleExpr] ||
             exp.isInstanceOf[VoidLiteralExpr] ||
             NodeUtil.isParenthesized(exp) )
          ExprFactory.makeParenthesisDelimitedMI(span, exp)
        else ExprFactory.makeNonParenthesisDelimitedMI(span, exp)
      }
      checkExpr(SMathPrimary(info, multi, infix, front, rest.map(toMathItem)))
    }

    // If this juxt is actually a fn app, then rewrite to a fn app.
    case SJuxt(info, multi, infix, front::rest, true, true) => rest.length match {
      case 1 => checkExpr(S_RewriteFnApp(info, front, rest.head))
      case n => // Make sure it is just two exprs.
        signal(expr, errorMsg("TightJuxt denoted as function application but has ",
                              n + "(!= 2) expressions."))
        expr
    }

    /* Loose Juxts are handled using the algorithm in 16.8 of Fortress Spec 1.0
     * ToDo: Revisit
     */
    case SJuxt(SExprInfo(span,paren,optType),
               multi, infix, exprs, isApp, false) => {
      // Check subexpressions
      val checkedExprs = exprs.map(checkExpr)
      if (!haveTypes(checkedExprs)) { return expr }

      // Break the list of expressions into chunks.
      // First the loose juxtaposition is broken into nonempty chunks;
      // wherever there is a non-function element followed
      // by a function element, the latter begins a new chunk.
      // Thus a chunk consists of some number (possibly zero) of
      // functions followed by some number (possibly zero) of non-functions.
      def chunker(exprs: List[Expr], results: List[(List[Expr],List[Expr])]): List[(List[Expr],List[Expr])] = exprs match {
        case Nil => results.reverse
        case first::rest =>
          if ( isArrows(first) ) {
            val (arrows,temp) = (first::rest).span(isArrows)
            val (nonArrows,remainingChunks) = temp.span((e:Expr) => ! isArrows(e))
            chunker(remainingChunks, (arrows,nonArrows)::results)
          } else {
            val (nonArrows,remainingChunks) = (first::rest).span((e:Expr) => ! isArrows(e))
            chunker(remainingChunks, (List(),nonArrows)::results)
          }
      }
      val chunks = chunker(checkedExprs, List())
      // Left associate nonarrows as a single OpExpr
      def associateNonArrows(nonArrows: List[Expr]): Option[Expr] =
        nonArrows match {
          case Nil => None
          case head::tail =>
            Some(tail.foldLeft(head){ (e1: Expr, e2: Expr) =>
                                      ExprFactory.makeOpExpr(infix,e1,e2) })
        }
      // Right associate everything in a chunk as a _RewriteFnApp
      def associateArrows(fs: List[Expr], oe: Option[Expr]) = oe match {
        case None => fs match {
          case Nil =>
            errors.signal("Empty chunk", expr)
            expr
          case _ =>
            fs.take(fs.size-1).foldRight(fs.last){ (f: Expr, e: Expr) =>
                                                   ExprFactory.make_RewriteFnApp(f,e) }
        }
        case Some(e) =>
          fs.foldRight(e){ (f: Expr, e: Expr) => ExprFactory.make_RewriteFnApp(f,e) }
      }
      // Associate a chunk
      def associateChunk(chunk: (List[Expr],List[Expr])): Expr = {
        val (arrows, nonArrows) = chunk
        associateArrows(arrows, associateNonArrows(nonArrows))
      }
      val associatedChunks = chunks.map(associateChunk)
      // (1) If any element that remains has type String,
      //     then it is a static error
      //     if there is any pair of adjacent elements within the juxtaposition
      //     such that neither element is of type String.
      val types = if ( haveTypes(associatedChunks) )
                    associatedChunks.map((e: Expr) => getType(e).get)
                  else List()
      def isStringType(t: Type) = analyzer.subtype(t, Types.STRING).isTrue
      if ( types.exists(isStringType) ) {
        def stringCheck(e: Type, f: Type) =
          if ( ! (isStringType(e) || isStringType(f)) ) {
            signal(expr, errorMsg("Neither element is of type String in ",
                                  "a juxtaposition of String elements."))
            e
          } else e
        types.take(types.size-1).foldRight(types.last)(stringCheck)
      }
      // (2) Treat the sequence that remains as a multifix application
      //     of the juxtaposition operator.
      //     The rules for multifix operators then apply.
      val multiOpExpr =
        new TryChecker(current, traits, env, analyzer).
          tryCheckExpr(ExprFactory.makeOpExpr(span,
                                              paren,
                                              toJavaOption(optType),
                                              multi,
                                              toJavaList(associatedChunks)))
      multiOpExpr.getOrElse {
        // If not, left associate as InfixJuxts
        associatedChunks match {
          case Nil =>
            errors.signal("Empty juxt", expr)
            expr
          case head::tail =>
            checkExpr(tail.foldLeft(head){ (e1: Expr, e2: Expr) =>
                                           ExprFactory.makeOpExpr(infix,e1,e2) })
        }
      }
    }

    // Math primary, which is the more general case,
    // is going to be called for both tight Juxt and MathPrimary

    // Base case of recursion: If there is no 'rest', return the Expr
    case SMathPrimary(info, multi, infix, front, Nil) => checkExpr(front)

    case mp@SMathPrimary(info@SExprInfo(span,paren,optType),
                         multi, infix, front, rest@second::remained) => {
      /** Check for ^ followed by ^ or ^ followed by [], both static errors. */
      def exponentiationStaticCheck(items: List[MathItem]) = {
        var exponent: Option[MathItem] = None
        def checkMathItem(item: MathItem) = item match {
          case SExponentiationMI(_,_,_) => exponent match {
            case None => exponent = Some(item)
            case Some(e) => syntaxError(item, "Two consecutive ^s.")
          }
          case SSubscriptingMI(_,_,_,_) => exponent match {
            case Some(e) =>
              syntaxError(item, "Exponentiation followed by subscripting is illegal.")
            case None =>
          }
          case _ => exponent = None
        }
        // Check for two exponentiations or an exponentiation and a subscript in a row
        items.foreach(checkMathItem)
      }
      exponentiationStaticCheck(rest) // See if simple static errors exist

      def isExprMI(expr: MathItem): Boolean = expr match {
        case SParenthesisDelimitedMI(_, _) => true
        case SNonParenthesisDelimitedMI(_, _) => true
        case _ => false
      }
      def isParenedExprItem(item: MathItem) = item match {
        case SParenthesisDelimitedMI(_,_) => true
        case _ => false
      }
      def isFunctionItem(item: MathItem) = item match {
        case SParenthesisDelimitedMI(_,e) => isArrows(checkExpr(e))
        case SNonParenthesisDelimitedMI(_,e) => isArrows(checkExpr(e))
        case _ => false
      }
      def expectParenedExprItem(item: MathItem) =
        if ( ! isParenedExprItem(item) )
          syntaxError(item, "Argument to function must be parenthesized.")
      def expectExprMI(item: MathItem) =
        if ( ! isExprMI(item) )
          syntaxError(item, "Item at this location must be an expression, not an operator.")
      // items is not an empty list.
      def associateMathItems(first: Expr,
                             items: List[MathItem]): (Expr, List[MathItem]) = {
        /* For each expression element (i.e., not a subscripting, exponentiation
         * or postfix operator), determine whether it is a function.
         * If some function element is immediately followed by an expression
         * element then, find the first such function element, and call the
         * next element the argument.
         */
        // find the left-most function
        val (prefix, others) = items.span((e:MathItem) =>
                                          !isFunctionItem(e))
        others match {
          case fn::arg::suffix => arg match {
            // It is a static error if either the argument is not parenthesized,
            case SNonParenthesisDelimitedMI(_,e) =>
              syntaxError(e, "Tightly juxtaposed expression should be parenthesized.")
              (first, Nil)
            case SParenthesisDelimitedMI(i,e) => {
              // or the argument is immediately followed by a non-expression element.
              suffix match {
                case third::more =>
                  if ( ! isExprMI(third) )
                    syntaxError(third, "An expression is expected.")
                case _ =>
              }
              // Otherwise, replace the function and argument with a single element
              // that is the application of the function to the argument.  This new
              // element is an expression.  Reassociate the resulting sequence
              // (which is one element shorter)
              val fnApp =
                new NonParenthesisDelimitedMI(i,
                                              ExprFactory.make_RewriteFnApp(fn.asInstanceOf[ExprMI].getExpr,
                                                                            arg.asInstanceOf[ExprMI].getExpr))
              associateMathItems( first, prefix++(fnApp::suffix) )
            }
            case _ => reassociateMathItems( first, items )
          }
          case _ => reassociateMathItems( first, items )
        }
      }
      // items is not an empty list.
      def reassociateMathItems(first: Expr, items: List[MathItem]) = {
        val (left, right) = items.span( isExprMI )
        val head = left match {
          case Nil => first
          case _ =>
            if ( isExprMI(left.last) )
              left.last.asInstanceOf[ExprMI].getExpr
            else {
              syntaxError(left.last, "An expression is expected.")
              first
            }
        }
        right match {
        /* If there is any non-expression element (it cannot be the first element)
         * then replace the first such element and the element
         * immediately preceeding it (which must be an expression) with
         * a single element that does the appropriate operator application.
         * This new element is an expression.  Reassociate the resulting
         * sequence (which is one element shorter.)
         */
          case item::suffix => {
            val newExpr = item match {
              case SExponentiationMI(_,op,expr) =>
                ExprFactory.makeOpExpr(op, head, toJavaOption(expr))
              case SSubscriptingMI(_,op,exprs,sargs) =>
                ExprFactory.makeSubscriptExpr(span, head,
                                              toJavaList(exprs), some(op),
                                              toJavaList(sargs))
              case _ =>
                syntaxError(item, "Non-expression element is expected.")
                head
            }
            left match {
              case Nil => associateMathItems(newExpr, suffix)
              case _ =>
                val exp = new NonParenthesisDelimitedMI(newExpr.getInfo, newExpr)
                associateMathItems(first, left.dropRight(1)++(exp::suffix))
            }
          }
          case _ => (first, items)
        }
      }

      // HANDLE THE FRONT ITEM
      val newFront = checkExpr(front)
      getType( newFront ) match {
        case None => front
        case Some(t) =>
          // If front is a fn followed by an expr, we reassociate
          if ( isArrows(t) && isExprMI(second) ) {
            // It is a static error if either the argument is not parenthesized,
            expectParenedExprItem(second)
            // static error if the argument is immediately followed by
            // a non-expression element.
            remained match {
              case hd::tl => expectExprMI(hd)
              case Nil =>
            }
            // Otherwise, make a new MathPrimary that is one element shorter,
            // and recur.
            val fn = ExprFactory.make_RewriteFnApp(front,
                                                   second.asInstanceOf[ExprMI].getExpr)
            checkExpr(SMathPrimary(info, multi, infix, fn, remained))
          // THE FRONT ITEM WAS NOT A FN FOLLOWED BY AN EXPR, REASSOCIATE REST
          } else {
            val (head, tail) = associateMathItems( newFront, rest )
            // Otherwise, left-associate the sequence, which has only expression
            // elements, only the last of which may be a function.
            val newTail = tail.map( (e:MathItem) =>
                                    if ( ! isExprMI(e) ) {
                                      syntaxError(e, "An expression is expected.")
                                      ExprFactory.makeVoidLiteralExpr(span)
                                    } else e.asInstanceOf[ExprMI].getExpr )
            // Treat the sequence that remains as a multifix application of
            // the juxtaposition operator.
            // The rules for multifix operators then apply.
            val multi_op_expr = checkExpr( ExprFactory.makeOpExpr(span, multi,
                                                                  toJavaList(head::newTail)) )
            getType(multi_op_expr) match {
              case Some(_) => multi_op_expr
              case None =>
                newTail.foldLeft(head){ (r:Expr, e:Expr) =>
                                        ExprFactory.makeOpExpr(NodeUtil.spanTwo(r, e),
                                                               infix, r, e) }
            }
          }
      }
    }

    case SStringLiteralExpr(SExprInfo(span,parenthesized,_), text) =>
      SStringLiteralExpr(SExprInfo(span,parenthesized,Some(Types.STRING)), text)

    case SCharLiteralExpr(SExprInfo(span,parenthesized,_), text, charVal) =>
      SCharLiteralExpr(SExprInfo(span,parenthesized,Some(Types.CHAR)),
                       text, charVal)

    case SIntLiteralExpr(SExprInfo(span,parenthesized,_), text, intVal) =>
      SIntLiteralExpr(SExprInfo(span,parenthesized,Some(Types.INT_LITERAL)),
                      text, intVal)

    case SFloatLiteralExpr(SExprInfo(span,parenthesized,_), text, i, n, b, p) =>
      SFloatLiteralExpr(SExprInfo(span,parenthesized,Some(Types.FLOAT_LITERAL)),
                        text, i, n, b, p)

    case SVoidLiteralExpr(SExprInfo(span,parenthesized,_), text) =>
      SVoidLiteralExpr(SExprInfo(span,parenthesized,Some(Types.VOID)), text)

    // Type checking of varargs and keyword arguments are not yet implemented.
    case STupleExpr(SExprInfo(span,parenthesized,_), es, vs, ks, inApp) => {
      if ( vs.isDefined || ks.size > 0 ) { // ArgExpr
        signal(expr, errorMsg("Type checking of varargs and keyword arguments are ",
                              "not yet implemented."))
        expr
      } else {
        val newEs = es.map(checkExpr)
        val types = newEs.map((e:Expr) =>
                              if (getType(e).isDefined) getType(e).get
                              else { Types.VOID })
        val newType = NodeFactory.makeTupleType(span, toJavaList(types).asInstanceOf[JavaList[Type]])
        STupleExpr(SExprInfo(span,parenthesized,Some(newType)), newEs, vs, ks, inApp)
      }
    }

    // First try to type check this expression as a multifix operator expression.
    // If that fails, type check it as some number of applications of the infix
    // operator, left associatively.
    case SAmbiguousMultifixOpExpr(info, infixOp, multifixOp, args) => {
      def infixAssociate(e1: Expr, e2: Expr) = SOpExpr(info, infixOp, List(e1, e2))
      new TryChecker(current, traits, env, analyzer).
        tryCheckExpr(SOpExpr(info, multifixOp, args)).
        getOrElse(checkExpr(args.reduceLeft(infixAssociate)))
    }

    case SChainExpr(SExprInfo(span,parenthesized,_), first, links) => {
      // Build up a list of OpExprs from the Links (in reverse).
      def makeOpExpr(prevAndResult: (Expr, List[Expr]),
                       nextLink: Link): (Expr, List[Expr]) = {
        val (prev, result) = prevAndResult
        val next = nextLink.getExpr()
        val op = nextLink.getOp()
        val newExpr = ExprFactory.makeOpExpr(NodeUtil.spanTwo(prev, next),
                                             op,
                                             prev,
                                             next)
        (next, newExpr :: result)
      }
      val (_, conjuncts) = links.foldLeft((first, List[Expr]()))(makeOpExpr)


      // Check that an expr is a Boolean.
      def checkBoolean(expr: Expr): Boolean = {
        getType(checkExpr(expr)) match {
          case Some(ty) =>
            isSubtype(ty, Types.BOOLEAN, expr,
                      errorMsg("The chained expression ",
                               " should have type Boolean, but had type ", normalize(ty), "."))
          case _ => false
        }
      }
      if (!conjuncts.forall(checkBoolean)) return expr

      // Reduce the OpExprs with an AND operation.


      SChainExpr(SExprInfo(span,parenthesized,Some(Types.BOOLEAN)), checkExpr(first),
                 links.map(t => check(t).asInstanceOf[Link]))
    }


    case SDo(SExprInfo(span,parenthesized,_), fronts) => {
      val fs = fronts.map(checkExpr).asInstanceOf[List[Block]]
      if ( haveTypes(fs) ) {
          // Get union of all clauses' types
          val frontTypes =
            fs.take(fs.size-1).foldRight(getType(fs.last).get)
              { (e:Expr, t:Type) => self.analyzer.join(getType(e).get, t) }
          SDo(SExprInfo(span,parenthesized,Some(normalize(frontTypes))), fs)
      } else { expr }
    }

    case SIf(SExprInfo(span,parenthesized,_), clauses, elseC) => {
      val newClauses = clauses.map( handleIfClause )
      val types = newClauses.flatMap(c => getType(c.getBody))
      val (newElse, newType) = elseC match {
        case None => {
          // Check that each if/elif clause has void type
          types.foreach( (ty: Type) =>
                         isSubtype(ty, Types.VOID, expr,
                                   errorMsg("An 'if' clause without corresponding 'else' has type ",
                                            normalize(ty), " instead of type ().")) )
          (None, Some(Types.VOID))
        }
        case Some(b) => {
          val newBlock = checkExpr(b).asInstanceOf[Block]
          getType(newBlock) match {
            case None => { (None, None) }
            case Some(ty) =>
              // Get union of all clauses' types
              (Some(newBlock), Some(normalize(analyzer.join(toJavaList(ty::types)))))
          }
        }
      }
      SIf(SExprInfo(span,parenthesized, newType), newClauses, newElse)
    }

    case SWhile(SExprInfo(span,parenthesized,_), testExpr, body) => {
      val (newTestExpr, bindings) = generatorClauseGetBindings(testExpr, true)
      val newBody = this.extend(bindings).checkExpr(body).asInstanceOf[Do]
      getType(newBody) match {
        case None => return expr
        case Some(ty) =>
          isSubtype(ty, Types.VOID, body,
                    errorMsg("Body of while loop must have type (), but had type ",
                             normalize(ty), "."))
      }
      SWhile(SExprInfo(span,parenthesized,Some(Types.VOID)), newTestExpr, newBody)
    }

    case SFor(SExprInfo(span,parenthesized,_), gens, body) => {
      val (newGens, bindings) = handleGens(gens)
      val newBody = this.extend(bindings).checkExpr(body).asInstanceOf[Block]
      getType(newBody) match {
        case None => return expr
        case Some(ty) =>
          isSubtype(ty, Types.VOID, body,
                    errorMsg("Body type of a for loop must have type () but has type ",
                             normalize(ty), "."))
      }
      SFor(SExprInfo(span,parenthesized,Some(Types.VOID)), newGens, newBody)
    }

    case v@SVarRef(SExprInfo(span,paren,_), id, sargs, depth) =>
      getTypeFromName(id) match {
        case Some(ty) =>
          if ( NodeUtil.isSingletonObject(v) )
            ty match {
              case typ@STraitType(STypeInfo(sp,pr,_,_), name, args, params) =>
                if ( NodeUtil.isGenericSingletonType(typ) &&
                     staticArgsMatchStaticParams(sargs, params)) {
                  // make a trait type that is GenericType instantiated
                  val newType = NodeFactory.makeTraitType(sp, pr, name,
                                                          toJavaList(sargs))
                  SVarRef(SExprInfo(span,paren,Some(newType)), id, sargs, depth)
                } else {
                  signal(v, "Unexpected type for a singleton object reference.")
                  v
                }
              case _ =>
                signal(v, "Unexpected type for a singleton object reference.")
                v
            }
          else SVarRef(SExprInfo(span,paren,Some(normalize(ty))), id, sargs, depth)
        case None => signal(id, errorMsg("Type of the variable '", id, "' not found.")); v
      }

    case STypecase(SExprInfo(span, paren, _),
                   bindIds, bindExpr, clauses, elseClause) => {
      val (checkedExpr, checkedType) = bindExpr.map(checkExpr) match {

        // If expr exists and was checked properly, make sure the bindIds are
        // not shadowing.
        case Some(checkedE) =>
          bindIds.foreach(i =>
            if (getTypeFromName(i).isDefined) {
              signal(i, "Cannot shadow name: %s".format(i))
              return expr
            })
          (Some(checkedE), getType(checkedE).getOrElse(return expr))

        // If expr does not exist, make sure thr bindIds are not mutable.
        case _ =>
          bindIds.foreach(id =>
            if (getModsFromName(id).getOrElse(Modifiers.None).isMutable)
              signal(id, ("Identifier for a typecase expression without a " +
                         "binding expression cannot be a mutable variable: %s").
                           format(id)))

          val idTypes = bindIds.map(getTypeFromName(_).getOrElse(return expr))
          (None, NodeFactory.makeMaybeTupleType(NodeUtil.getSpan(expr),
                                                toJavaList(idTypes)))
      }

      // Check that the number of bindIds matches the size of the bindExpr.
      val isMultipleIds = bindIds.size > 1
      if (isMultipleIds && bindExpr.isDefined) {
        checkedType match {
          case STupleType(_, elts, _, _) =>
            if (elts.size != bindIds.size) {
              signal(bindExpr.get,
                     errorMsg("A typecase expression has multiple identifiers\n    but ",
                              "the sizes of the identifiers and the binding ",
                              "expression do not match."))
              return expr
            }
          case _ =>
            signal(bindExpr.get,
                   errorMsg("A typecase expression has multiple identifiers\n    but ",
                            "the binding expression does not have a tuple type."))
            return expr
        }
      }
      // Check each clause with the bound ids having types of
      // intersection of the static types of the guard and the checkedType
      def checkClause(c: TypecaseClause): TypecaseClause = {
        val STypecaseClause(info, matchType, body) = c
        if (matchType.size != bindIds.size) {
          signal(c, "A typecase expression has a different number of cases in a clause.")
          return c
        }

        // Construct the types that correspond to each id.
        val newType =
          if (isMultipleIds) {
            val STupleType(_, eltTypes, _, _) = checkedType
            eltTypes.zip(matchType).map((p:(Type, Type)) =>
                normalize(NodeFactory.makeIntersectionType(p._1, p._2)))
          } else {
            List[Type](normalize(NodeFactory.makeIntersectionType(checkedType, matchType.first)))
          }

        val checkedBody =
          this.extend(bindIds, newType).
          checkExpr(body).asInstanceOf[Block]

        STypecaseClause(info, matchType, checkedBody)
      }
      val checkedClauses = clauses.map(checkClause)
      val clauseTypes =
        checkedClauses.map(c => getType(c.getBody).getOrElse(return expr))

      // Check the else clause with the new binding.
      val newType =
        if (isMultipleIds)
          toList(checkedType.asInstanceOf[TupleType].getElements)
        else
          List[Type](checkedType)
      val checkedElse =
        elseClause.map(e =>
          this.extend(bindIds, newType).
            checkExpr(e).asInstanceOf[Block])
      val elseType = checkedElse.map(getType(_).getOrElse(return expr))

      // Build union type of all clauses and else.
      val allTypes = elseType match {
        case Some(t) => Set(clauseTypes:_*) + t
        case _ => Set(clauseTypes:_*)
      }
      val unionType = NodeFactory.makeUnionType(allTypes)
      // TODO: A nonexhaustive typecase is an error.
      STypecase(SExprInfo(span, paren, Some(unionType)),
                bindIds,
                checkedExpr,
                checkedClauses,
                checkedElse)
    }

    case SAsExpr(SExprInfo(span, paren, _), sub, typ) => {
      val checkedSub = checkExpr(sub, Some(typ), errorString("Expression", "ascripted"))
      SAsExpr(SExprInfo(span, paren, Some(typ)), checkedSub, typ)
    }

    case SAsIfExpr(SExprInfo(span, paren, _), sub, typ) => {
      val checkedSub = checkExpr(sub, Some(typ), errorString("Expression", "assumed"))
      SAsIfExpr(SExprInfo(span, paren, Some(typ)), checkedSub, typ)
    }

    case _ => throw new Error(errorMsg("Not yet implemented: ", expr.getClass))
  }

  /** A type checker that signals an error if a spawn expr occurs inside it. */
  class AtomicChecker(current: CompilationUnitIndex,
                      traits: TraitTable,
                      env: TypeEnv,
                      analyzer: TypeAnalyzer,
                      errors: ErrorLog,
                      enclosingExpr: String)
      extends STypeChecker(current,traits,env,analyzer,errors) {
    
    val message = errorMsg("A 'spawn' expression must not occur inside ",
                           enclosingExpr, ".")
    override def checkExpr(e: Expr): Expr = e match {
      case SSpawn(_, _) => syntaxError(e, message); e
      case _ => super.checkExpr(e)
    }
  }
  
}
