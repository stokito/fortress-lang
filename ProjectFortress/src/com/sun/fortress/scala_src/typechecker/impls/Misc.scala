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
import edu.rice.cs.plt.collect.Relation
import edu.rice.cs.plt.collect.UnionRelation
import com.sun.fortress.compiler.index.CompilationUnitIndex
import com.sun.fortress.compiler.index.Method
import com.sun.fortress.compiler.index.ObjectTraitIndex
import com.sun.fortress.compiler.typechecker.TypeAnalyzer
import com.sun.fortress.compiler.Types
import com.sun.fortress.exceptions.StaticError.errorMsg
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.NodeFactory
import com.sun.fortress.nodes_util.Modifiers
import com.sun.fortress.nodes_util.NodeUtil
import com.sun.fortress.scala_src.typechecker._
import com.sun.fortress.scala_src.typechecker.staticenv.STypeEnv
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.useful.ASTGenHelper._
import com.sun.fortress.scala_src.useful.ErrorLog
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.Sets._
import com.sun.fortress.scala_src.useful.SExprUtil._
import com.sun.fortress.scala_src.useful.STypesUtil._

import scala.collection.mutable.{Map => MMap}

/**
 * Provides the implementation of miscellaneous cases that aren't found in any
 * of the other implementation groups.
 *
 * This trait must be mixed in with an `STypeChecker with Common` instance
 * in order to provide the full type checker implementation.
 *
 * (The self-type annotation at the beginning declares that this trait must be
 * mixed into STypeChecker along with the Common helpers. This is what
 * allows this trait to implement abstract members of STypeChecker and to
 * access its protected members.)
 */
trait Misc { self: STypeChecker with Common =>

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
    new AtomicChecker(current,traits,env,errors,enclosingExpr).checkExpr(expr)

  // ---------------------------------------------------------------------------
  // CHECK IMPLEMENTATION ------------------------------------------------------

  def checkMisc(node: Node): Node = node match {

    case id@SId(info,api,name) => {
      // Don't try to get the type if there isn't one.
      if (!nameHasBinding(id)) {
        signal(id, errorMsg("Variable '", id, "' not found."))
        return id
      }
      getTypeFromName(id) match {
        case Some(ty) => ty match {
          case SLabelType(_) => // then, newName must be an Id
            signal(id, errorMsg("Cannot use label name ", id, " as an identifier."))
          case _ =>
        }
        case None =>
          // This means that `id` is a function whose return type could not be
          // inferred. An error should have been signaled by the
          // CyclicReferenceChecker.
      }
      id
    }

    case _ => throw new Error(errorMsg("not yet implemented: ", node.getClass))
  }

  // ---------------------------------------------------------------------------
  // CHECKEXPR IMPLEMENTATION --------------------------------------------------

  def checkExprMisc(expr: Expr, expected: Option[Type]): Expr = expr match {

    case fr@SFieldRef(SExprInfo(span, parens, _),obj,field) => {
      val checkedObj = checkExpr(obj)
      val recvrType = getType(checkedObj).getOrElse(return expr)
      val fieldType = findFieldsInTraitHierarchy(field, recvrType)
      fieldType match {
        case Some(_) => SFieldRef(SExprInfo(span, parens, fieldType), checkedObj, field)
        case None =>
          signal(expr,"%s has no field called %s".format(obj,field))
          expr
      }
    }

    //ToDo: Why isn't this a Decl?
    case o@SObjectExpr(SExprInfo(span,parenthesized,_),
                     STraitTypeHeader(sparams, mods, name, where,
                                      throwsC, contract, extendsC, decls),
                     selfType) => {
      // Verify that no extends clauses try to extend an object.
      extendsC.foreach( (t:TraitTypeWhere) =>
                        assertTrait(t.getBaseType,
                                    "Objects can only extend traits.", t.getBaseType) )
      var method_checker: STypeChecker = self
      var field_checker: STypeChecker = self
      val newContract = contract match {
        case Some(e) => Some(method_checker.check(e).asInstanceOf[Contract])
        case _ => contract
      }
      // Extend the type checker with all of the field decls
      method_checker = decls.foldRight(method_checker)
                                      { (d:Decl, c:STypeChecker) => d match {
                                        case SVarDecl(_,lhs,_) => c.extend(lhs)
                                        case _ => c } }
      // Extend type checker with methods and functions
      // that will now be in scope as regular functions
      val oi = IndexBuilder.buildObjectExprIndex(o)
      val methods = new UnionRelation(inheritedMethods(extendsC),
                                      oi.asInstanceOf[ObjectTraitIndex].dottedMethods.asInstanceOf[Relation[IdOrOpOrAnonymousName, Method]])
      method_checker = method_checker.extendWithFunctions(methods)
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

    case s@SSpawn(SExprInfo(span,paren,optType), body) => {
      val newExpr = this.extendWithout(s, toSet(labelExitTypes.keySet)).checkExpr(body)
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

    case v@SVarRef(SExprInfo(span,paren,_), id, sargs, depth) => {
      val checkedId = check(id).asInstanceOf[Id]
      val ty = getTypeFromName(checkedId).getOrElse(return expr)
      if ( NodeUtil.isSingletonObject(v) )
        ty match {
          case typ@STraitType(STypeInfo(sp,pr,_,_), name, args, params) =>
            if ( NodeUtil.isGenericSingletonType(typ) &&
                 staticArgsMatchStaticParams(sargs, params)) {
              // make a trait type that is GenericType instantiated
              val newType = NodeFactory.makeTraitType(sp, pr, name,
                                                      toJavaList(sargs))
              SVarRef(SExprInfo(span,paren,Some(newType)), checkedId, sargs, depth)
            } else {
              signal(v, "Unexpected type for a singleton object reference.")
              v
            }
          case _ =>
            signal(v, "Unexpected type for a singleton object reference.")
            v
        }
      else SVarRef(SExprInfo(span,paren,Some(normalize(ty))), checkedId, sargs, depth)
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
      val unionType = NodeFactory.makeUnionType(toJavaSet(allTypes))
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
                      env: STypeEnv,
                      errors: ErrorLog,
                      enclosingExpr: String)
                     (implicit analyzer: TypeAnalyzer,
                               envCache: MMap[APIName, STypeEnv])
      extends STypeCheckerImpl(current,traits,env,errors) {

    override def constructor(current: CompilationUnitIndex,
                              traits: TraitTable,
                              env: STypeEnv,
                              errors: ErrorLog)
                             (implicit analyzer: TypeAnalyzer,
                                       envCache: MMap[APIName, STypeEnv]) =
      new AtomicChecker(current, traits, env, errors, enclosingExpr)

    val message = errorMsg("A 'spawn' expression must not occur inside ",
                           enclosingExpr, ".")
    override def checkExpr(e: Expr): Expr = e match {
      case SSpawn(_, _) => syntaxError(e, message); e
      case _ => super.checkExpr(e)
    }
  }

}
