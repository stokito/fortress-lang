/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.scala_src.typechecker.impls

import _root_.java.math.BigInteger
import _root_.java.util.{HashSet => JavaHashSet}
import _root_.java.util.{List => JavaList}
import _root_.java.util.{Map => JavaMap}
import _root_.java.util.{Set => JavaSet}
import edu.rice.cs.plt.collect.Relation
import edu.rice.cs.plt.collect.UnionRelation
import com.sun.fortress.compiler.index.CompilationUnitIndex
import com.sun.fortress.compiler.index.Method
import com.sun.fortress.compiler.index.ObjectTraitIndex
import com.sun.fortress.compiler.index.TraitIndex
import com.sun.fortress.compiler.Types
import com.sun.fortress.exceptions.StaticError.errorMsg
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.{ExprFactory => EF}
import com.sun.fortress.nodes_util.{NodeFactory => NF}
import com.sun.fortress.nodes_util.{NodeUtil => NU}
import com.sun.fortress.nodes_util.Modifiers
import com.sun.fortress.scala_src.typechecker._
import com.sun.fortress.scala_src.typechecker.staticenv.STypeEnv
import com.sun.fortress.scala_src.types.TypeAnalyzer
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.useful.ASTGenHelper._
import com.sun.fortress.scala_src.useful.ErrorLog
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.Sets._
import com.sun.fortress.scala_src.useful.SExprUtil._
import com.sun.fortress.scala_src.useful.STypesUtil._
import com.sun.fortress.useful.Useful

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

  val typeConses = current.typeConses

  // ---------------------------------------------------------------------------
  // HELPER METHODS ------------------------------------------------------------

  /**
   * The Java type checker had a separate postinference pass "closing bindings".
   * @TODO: Look over this method.
   */
  protected def generatorClauseGetBindings(clause: GeneratorClause,
                                           mustBeCondition: Boolean) = {
    val SGeneratorClause(info, binds, init) = clause
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
              NF.make_InferenceVarType(NU.getSpan(id))
            val (lhstype, bindings) = binds.length match {
              case 1 => // Just one binding
                val lhstype = mkInferenceVarType(hd)
                (lhstype, List[LValue](NF.makeLValue(hd, lhstype)))
              case n =>
                // Because generator_type is almost certainly an _InferenceVar,
                // we have to declare a new tuple that is the size of the bindings
                // and declare one to be a subtype of the other.
                val inference_vars = binds.map(mkInferenceVarType)
                (Types.makeTuple(toJavaList(inference_vars)),
                 binds.zip(inference_vars).map((p:(Id,Type)) =>
                                               NF.makeLValue(p._1,p._2)))
            }
            // Get the type of the Generator
            val infer_type = NF.make_InferenceVarType(NU.getSpan(init))
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
  protected def handleIfClause(c: IfClause, expected: Option[Type]) = c match {
    case SIfClause(info, testClause, body) =>
      // For generalized 'if' we must introduce new bindings.
      val (newTestClause, bindings) = generatorClauseGetBindings(testClause, true)
      // Check body with new bindings
      val newBody = this.extend(bindings).checkExpr(body, expected).asInstanceOf[Block]
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

    // The requires clause consists of a sequence of expressions of type Boolean.
    // Expression in each subclause of ensures clause has type Boolean.
    // The expression in the provided subclause of an ensures clause subclause
    // is of type Boolean.
    case SContract(info, requiresC, ensures, invariants) => {
      val msg1 = "A requires clause"
      val msg2 = "An ensures clause"
      val msg3 = "A provided clause"
      val newRequires =
        requiresC.map(_.map(checkExpr(_, Types.BOOLEAN, errorString(msg1))))
      val newEnsures =
        ensures.map(_.map(ec => ec match {
                          case SEnsuresClause(info, post, pre) =>
                            SEnsuresClause(info, checkExpr(post, Types.BOOLEAN,
                                                           errorString(msg2)),
                                           pre.map(checkExpr(_, Types.BOOLEAN,
                                                             errorString(msg3))))}))
      SContract(info, newRequires, newEnsures, invariants.map(_.map(checkExpr)))
    }

    case _ => throw new Error(errorMsg("Not yet implemented: ", node.getClass))
  }

  // ---------------------------------------------------------------------------
  // CHECKEXPR IMPLEMENTATION --------------------------------------------------

  def declToIdType(decl : Decl) = decl match {
    case SVarDecl(_, lhs, _) if(lhs.size >= 1)=>
      lhs.map(p => (p.getName.toString, toOption(p.getIdType)))
    case SFnDecl(_, SFnHeader(_, mods, name, _, _, _, _, returnType), _, _, _) if mods.isGetter =>
      List((name.toString, toOption(returnType)))
    case _ => List(("", None))
  }

  def getOptionTy(field: Id, pair_list: List[(String, Option[TypeOrPattern])]) =
    pair_list.find(pair => pair._1 == field.toString) match {
      case Some(pa) => pa._2
      case _ =>
        signal(field, errorMsg(field, " : Not defined"))
        None
    }

  def pbToPbTy(pbi: (PatternBinding, Int), ty: Type): (PatternBinding, Type) = {
    val (pb, i) = pbi
    val optionTy = ty match {
      case t:TraitType if typeConses.keySet.contains(t.getName) =>
        val header = typeConses.get(t.getName).ast.asInstanceOf[TraitObjectDecl].getHeader
        val empty = Nil.asInstanceOf[List[(String, Option[TypeOrPattern])]]
        if (pb.getField.isDefined) {
          val pair_list =
            (toOption(header.getParams) match {
               case Some(ps) => toList(ps).map(p=>(p.getName.toString, toOption(p.getIdType)))
               case _ => List((Nil.asInstanceOf[String], Nil.asInstanceOf[Option[TypeOrPattern]]))
            }) ::: toList(header.getDecls).foldRight(empty)((d, r) => declToIdType(d) ::: r)
          getOptionTy(pb.getField.get, pair_list)
        } else {
          toOption(header.getParams) match {
            case Some(ps) => toOption(toList(ps).apply(i).getIdType)
            case _ =>
              signal(t, "A trait is expected to have value parameters for patterns.")
              None
          }
        }
      case _ =>
        signal(ty, "A trait type is expected in a pattern.")
        None
    }
    optionTy match {
      case Some(tty) if tty.isInstanceOf[Type] => (pb, tty.asInstanceOf[Type])
      case _ =>
        signal(pb, "type unmatched")
        (pb, NF.makeVoidType(NU.getSpan(pb)))
    }
  }

  def getBoundIdWithType(p : (PatternBinding, Type)): (Type, List[(Id, Type)]) = {
    val (pb, typ) = p
    pb match {
      case SPlainPattern(_, _, name, _, Some(idType)) =>
        val (t, idty_list) = getTypeAndIdTyList(idType, typ)
        (t, (name, t) :: idty_list)
      case SPlainPattern(_, _, name, _, None) => (typ, List((name, typ)))
      case STypePattern(_, _, tt) => (normalize(NF.makeIntersectionType(tt, typ)), List())
      case SNestedPattern(_, _, pat) => getTypeAndIdTyList(pat, typ)
    }
  }

  def getTypeAndIdTyList(tp : TypeOrPattern, expr_type : Type): (Type, List[(Id,Type)]) = {
    val (isTupleType, expr_size) = expr_type match {
      case t:TupleType if !NU.isVoidType(expr_type) => (true, t.getElements.size)
      case _ => (false, 1)
    }
    if(tp.isInstanceOf[Pattern]){
      val pattern = tp.asInstanceOf[Pattern]
      val ps = toList(pattern.getPatterns.getPatterns)
      toOption(pattern.getName) match {
        case Some(ty) =>
          if (isTupleType) { // error
            signal(pattern, "A typecase clause is unreachable.")
            ty
          }
          /* A structure of a pattern shoud be checked in comparison with the structure of 'ty'.*/
          /* error handling in case that a pattern has an incorrect structure. */
          ty match {
            case t:TraitType if typeConses.keySet.contains(t.getName) =>
              val params = typeConses.get(t.getName).ast.asInstanceOf[TraitObjectDecl].getHeader.getParams
              val (numParams, paramIdlist) =
                  toOption(params) match {
                    case Some(ps) => (ps.size, toList(ps).map(_.getName))
                    case _ => (0, List())
                  }
              /* check whether a given pattern is a keyword pattern or not */
              def isKeywordPattern(pattern : PatternBinding) : Boolean =
                toOption(pattern.getField) match {
                  case Some(kw) => !(paramIdlist.contains(kw))
                  case _ => false
                }
              if(ps.filter(! isKeywordPattern(_)).size != numParams) {  // error 
                signal(ty, "The number of patterns to bind should be greater than or equal to " + numParams)
                return (ty, List())
              }
            case _ => // error
              signal(ty, "Type " + ty + " not found.")
              return (ty, List())
          }
          /* get types of all fields of the type "ty" corresponding to each pattern."*/
          val collected = ps.zipWithIndex.map(pbToPbTy(_, ty)).map(getBoundIdWithType)
          (normalize(NF.makeIntersectionType(ty, expr_type)),
           collected.foldRight(List[(Id,Type)]()){ (pair, idty_list) => pair._2 ::: idty_list})

        case None => // tuple pattern
          if ((ps.length != expr_size) || !isTupleType) {
            signal(tp, "A typecase clause is unreachable.")
            (expr_type, List())
          } else {
            val eltTypes = toList(expr_type.asInstanceOf[TupleType].getElements)
            val collected = (ps zip eltTypes).map(getBoundIdWithType)
            (NF.makeMaybeTupleType(NU.getSpan(tp), toJavaList(collected.map(_._1))),
             collected.foldRight(List[(Id,Type)]()){ (pair, idty_list) => pair._2 ::: idty_list})
          }
      }
    } else {
      val ty = tp.asInstanceOf[Type]
      if (( isTupleType && !NU.isTupleType(ty)) ||
          (!isTupleType &&  NU.isTupleType(ty)) ||
          ( isTupleType &&  NU.isTupleType(ty) &&
            ty.asInstanceOf[TupleType].getElements.size !=
            expr_type.asInstanceOf[TupleType].getElements.size)) {
        signal(tp, "A typecase clause is unreachable.")
        (ty, List())
      } else (normalize(NF.makeIntersectionType(ty, expr_type)), List())
      }
  }

  // Check each typecase clause:
  //   Collect pattern variables and their types using checkedType
  //   Extend the typechecker with the new id/type information
  //   Type-check the body of the typecase clause
  def checkClause(c: TypecaseClause, checkedType: Type): TypecaseClause = {
    val STypecaseClause(info, nameOpt, matchType, body) = c
    // Construct the types corresponding to ids
    val checkLeft = getTypeAndIdTyList(matchType, checkedType)
    var idty_list : List[(Id, Type)] = checkLeft._2
    idty_list = if (nameOpt.isDefined) (nameOpt.get, checkLeft._1) :: idty_list
                else idty_list
    //idty_list.foreach(pair => System.out.println(pair._1 +", " + pair._2))
    val newChecker = this.extend(idty_list)
    val checkedBody = newChecker.checkExpr(body).asInstanceOf[Block]
    STypecaseClause(info, nameOpt, matchType, checkedBody)
  }

  def checkExprMisc(expr: Expr, expected: Option[Type]): Expr = expr match {

    case fr@SFieldRef(SExprInfo(span, parens, _),obj,field) => {
      val checkedObj = checkExpr(obj)
      val recvrType = getType(checkedObj).getOrElse(return expr)
      val fieldType = getGetterType(field, recvrType)
      fieldType match {
        case Some(_) => SFieldRef(SExprInfo(span, parens, fieldType), checkedObj, field)
        case None =>
          signal(expr,"%s has no getter called %s".format(recvrType, field))
          expr
      }
    }

    //ToDo: Why isn't this a Decl?
    case o@SObjectExpr(SExprInfo(span,parenthesized,_),
                       STraitTypeHeader(sparams, mods, name, where,
                                        throwsC, contract, extendsC, params, decls),
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
      val omethods = toSet(oi.asInstanceOf[ObjectTraitIndex].dottedMethods.secondSet)
      val imethods = commonInheritedMethods(extendsC, analyzer.traits)
      val methods = (omethods.toList ++ imethods)
      method_checker = method_checker.extendWithListOfFunctions(methods)
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
                                       throwsC, newContract, extendsC, params, newDecls),
                      selfType)
        case _ => signal(o, errorMsg("Self type is not inferred for ", o)); o
      }
    }

    /* Matches if block is an atomic block. */
    case SBlock(SExprInfo(span,parenthesized,resultType),
                loc, true, withinDo, exprs) =>
      /* Strip the "atomic" to avoid infinite recursion;
       * then set it back again */
      NF.remakeAtomic(
      forAtomic(SBlock(SExprInfo(span,parenthesized,resultType),
                       loc, false, withinDo, exprs),
                "an 'atomic' do block").asInstanceOf[Block])

    /* Matches if block is not an atomic block. */
    case SBlock(SExprInfo(span,parenthesized,resultType),
                loc, false, withinDo, exprs) => {
      val newLoc = loc match {
        case Some(l) =>
          Some(checkExpr(l, Types.REGION, errorString("Location of the block")))
        case None => loc
      }
      exprs.reverse match {
        case Nil =>
          SBlock(SExprInfo(span,parenthesized,Some(Types.VOID)),
                 newLoc, false, withinDo, exprs)
        case last::rest =>
          val allButLast = rest.map((e: Expr) => checkExpr(e, Types.VOID,
                                                           errorString("Non-last expression in a block")))
          val lastExpr = checkExpr(last, expected)
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
      SCharLiteralExpr(SExprInfo(span,parenthesized,Some(Types.CHARACTER)),
                       text, charVal)

    case SIntLiteralExpr(SExprInfo(span,parenthesized,_), text, intVal) =>
      SIntLiteralExpr(SExprInfo(span,parenthesized,Some(Types.INT_LITERAL)),
                      text, intVal)

    case SFloatLiteralExpr(SExprInfo(span,parenthesized,_), text, i, n, b, p) =>
      SFloatLiteralExpr(SExprInfo(span,parenthesized,Some(Types.FLOAT_LITERAL)),
                        text, i, n, b, p)

    case SVoidLiteralExpr(SExprInfo(span,parenthesized,_), text) =>
      SVoidLiteralExpr(SExprInfo(span,parenthesized,Some(Types.VOID)), text)

    case SBooleanLiteralExpr(SExprInfo(span,parenthesized,_), text, b) =>
      SBooleanLiteralExpr(SExprInfo(span,parenthesized,Some(Types.BOOLEAN)), text, b)      
      
    // Type checking of varargs and keyword arguments are not yet implemented.
    case STupleExpr(SExprInfo(span,parenthesized,_), es, vs, ks, inApp) => {
      if ( vs.isDefined || ks.size > 0 ) { // ArgExpr
        signal(expr, errorMsg("Type checking of varargs and keyword arguments are ",
                              "not yet implemented."))
        expr
      } else {

        // If there is an expected tuple type, check the elements with their
        // corresponding expected type.
        val newEs = expected match {
          case Some(typ) =>
            val eltsTypes = zipWithDomain(es, typ)
            if (eltsTypes.length != es.length)
              es.map(checkExpr)
            else
              // Don't perform coercion.
              eltsTypes.map(et => checkExpr(et._1, Some(et._2)))
          case None => es.map(checkExpr)
        }
        val types = newEs.map(getType(_).getOrElse(Types.VOID))
        val newType = NF.makeTupleType(span, toJavaList(types))
        STupleExpr(SExprInfo(span,parenthesized,Some(newType)), newEs, vs, ks, inApp)
      }
    }

    case SDo(SExprInfo(span,parenthesized,_), fronts) => {
      val fs = fronts.map(checkExpr(_, expected)).asInstanceOf[List[Block]]
      if ( haveTypes(fs) ) {
          // In a do-also expression (i.e., one in which also appears),
          // each block expression must have type ().
          if (fs.size > 1)
              fs.map(checkExpr(_, Types.VOID,
                               errorString("do-also expression")))

          // Get union of all clauses' types
          val frontTypes =
            fs.take(fs.size-1).foldRight(getType(fs.last).get)
              { (e:Expr, t:Type) => self.analyzer.join(getType(e).get, t) }
          SDo(SExprInfo(span,parenthesized,Some(normalize(frontTypes))), fs)
      } else { expr }
    }

    // An if expression with an else.
    case SIf(SExprInfo(span,parenthesized,_), clauses, Some(elseClause)) => {
      // Check all the clauses.
      val checkedClauses = clauses.map(c => handleIfClause(c, expected) )
      val clauseTypes = checkedClauses.flatMap(c => getType(c.getBody))
      val checkedElse = checkExpr(elseClause, expected).asInstanceOf[Block]
      val elseType = getType(checkedElse).getOrElse(return expr)

      val allClauses = checkedElse :: checkedClauses.map(_.getBody)
      val allTypes = elseType :: clauseTypes

      // Find the clause with type T s.t. T_i is substitutable for T,
      // for all clause types T_i.
      def findWinningClause(clauseTypes: List[Type]): Option[Type] = {
        for (candidate <- clauseTypes)
          if (clauseTypes.forall(coercions.substitutableFor(_, candidate)))
            return Some(candidate)
        None
      }
      findWinningClause(allTypes) match {

        // If no winning type, get union of all clauses' types.
        case None =>
          val resultType = normalize(analyzer.join(allTypes))
          SIf(SExprInfo(span,parenthesized, Some(resultType)),
              checkedClauses,
              Some(checkedElse))

        // If a winning type, coerce all the clauses to that type.
        case Some(winner) =>

          // Get the coercions on all the clauses.
          val maybeCoercions = (allClauses, allTypes).zipped.map { (e, t) =>
            // .get works because we know each clause is indeed subst.
            // for the winner
            coercions.checkSubstitutable(t, winner, Some(e)).get
          }

          // Merge the coerced clauses and the original clauses.
          val (newElseBlock :: newClauseBlocks) =
            (allClauses, maybeCoercions).zipped.map {
              case (e1, None) => e1
              case (e1, Some(e2)) => EF.makeBlock(e2)
            }

          // Reconstruct IfClauses with their new blocks.
          val newIfClauses = (checkedClauses, newClauseBlocks).zipped.map {
            case (SIfClause(a, b, _), newBlock) => SIfClause(a, b, newBlock)
          }
          SIf(SExprInfo(span,parenthesized, Some(winner)),
              newIfClauses,
              Some(newElseBlock))
      }

    }

    // An if expression without an else.
    case SIf(SExprInfo(span,parenthesized,_), clauses, None) => {
      val checkedClauses = clauses.map(c => handleIfClause(c, None))
      val clauseTypes = checkedClauses.flatMap(c => getType(c.getBody))

      // Check that each if/elif clause has void type
      clauseTypes.foreach { ty =>
        isSubtype(ty,
                  Types.VOID,
                  expr,
                  errorMsg("An 'if' clause without corresponding 'else' has type ",
                           normalize(ty), " instead of type ()."))
      }
      SIf(SExprInfo(span, parenthesized, Some(Types.VOID)),
          checkedClauses,
          None)
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
      if ( !sargs.isEmpty )
        // TODO: handle generic higher-order function passing here.
        ty match {
          case typ@STraitType(STypeInfo(sp,pr,_,_), name, args, params)
            if ( staticArgsMatchStaticParams(sargs, params) ) =>
              // make a trait type that is GenericType instantiated
              val newType = NF.makeTraitType(sp, pr, name, toJavaList(sargs))
              SVarRef(SExprInfo(span,paren,Some(newType)), checkedId, sargs, depth)
          case _ =>
            signal(v, "Unexpected type for a singleton object reference.")
            v
        }
      // TODO: handle missing static args below (either generic higher-order function
      // or generic singleton being used without explicit type instantiation).
      else SVarRef(SExprInfo(span,paren,Some(normalize(ty))), checkedId, sargs, depth)
    }

    case STypecase(SExprInfo(span, paren, _),
                   bindExpr, clauses, elseClause) => {
      val checkedExpr = checkExpr(bindExpr)
      val checkedType = getType(checkedExpr).getOrElse(return expr)
      val checkedClauses = clauses.map(checkClause(_,checkedType))
      val clauseTypes =
        checkedClauses.map(c => getType(c.getBody).getOrElse(return expr))
      val checkedElse = elseClause.map(checkExpr(_).asInstanceOf[Block])

      // Build a union type of all clauses and else.
      val allTypes = checkedElse.map(getType(_).getOrElse(return expr)) match {
        case Some(t) => Set(clauseTypes:_*) + t
        case _ => Set(clauseTypes:_*)
      }
      val unionType = NF.makeUnionType(toJavaSet(allTypes))

      // TODO: A nonexhaustive typecase is an error.

      STypecase(SExprInfo(span, paren, Some(unionType)),
                checkedExpr,
                checkedClauses,
                checkedElse)
    }

    case SAsExpr(SExprInfo(span, paren, _), sub, typ) => {
      val checkedSub = checkExpr(sub, typ, errorString("Expression", "ascripted"))
      SAsExpr(SExprInfo(span, paren, Some(typ)), checkedSub, typ)
    }

    case SAsIfExpr(SExprInfo(span, paren, _), sub, typ) => {
      val checkedSub = checkExpr(sub, typ, errorString("Expression", "assumed"))
      SAsIfExpr(SExprInfo(span, paren, Some(typ)), checkedSub, typ)
    }

    case label@SLabel(SExprInfo(span, paren, _), name, body) =>
      // Make sure this label isn't already bound
      env.lookup(name) match {
        case Some(_) =>
          signal(label, "Cannot use an existing identifier " + name +
                        " for a 'label' expression.")
          label
        case None =>
          // Initialize the set of exit types
          labelExitTypes.put(name, Some(new JavaHashSet()))
          // Extend the checker with this label name in the type env
          val newChecker = this.extend(List(NF.makeLValue(name, Types.LABEL)))
          val newBody = newChecker.checkExpr(body).asInstanceOf[Block]
          // If the body was typed, union all the exit types with it.
          // If any exit type is none, then don't type this label.
          var labelType: Option[Type] = None
          (getType(newBody), labelExitTypes.get(name)) match {
            case (Some(ty), Some(exitTypes)) =>
              exitTypes.add(ty)
              labelType = Some(normalize(analyzer.join(toSet(exitTypes))))
            case _ =>
          }
          // Destroy the mappings for this label
          labelExitTypes.remove(name)
          SLabel(SExprInfo(span, paren, labelType), name, newBody)
      }

    case exit@SExit(SExprInfo(span, paren, _), targetOpt, returnOpt) => {
      (targetOpt, returnOpt) match {
        case (Some(target), Some(returnExpr)) => env.lookup(target) match {
          case Some(b) => b.typeThunk.apply match {
            case Some(targetType) =>
              if (! targetType.isInstanceOf[LabelType])
                signal(exit, "Target of 'exit' is not a label name: " + target)
              else {
                // Append the 'with' type to the list for this label
                val newReturn = checkExpr(returnExpr)
                getType(newReturn) match {
                  case Some(ty) =>
                    val types = labelExitTypes.get(target)
                    if (types != null && types.isDefined) types.get.add(ty)
                    else signal(exit, "Target of 'exit' is missing.")
                  case None => labelExitTypes.put(target, None)
                }
                return SExit(SExprInfo(span, paren, Some(Types.BOTTOM)),
                             Some(target), Some(newReturn))
              }
            case None => signal(exit, "Target of 'exit' is missing.")
          }
          case None => signal(exit, "Could not find 'label' with name: " + target)
        }
        case (None, _) => signal(exit, "Target of 'exit' is missing.")
        case (_, None) => signal(exit, "Return expression of 'exit' is missing.")
      }
      return exit
    }

    case SThrow(SExprInfo(span, paren, _), exn) => {
      // A throw expression has type bottom, pretty much regardless
      // but expr must have type Exception
      val newExn = checkExpr(exn)
      getType(newExn) match {
        case Some(ty) =>
          isSubtype(ty, Types.EXCEPTION, exn,
                    errorMsg("'throw' can only throw objects of Exception type.  ",
                             "This expression is of type " + normalize(ty), "."))
        case None => return expr
      }
      SThrow(SExprInfo(span, paren, Some(Types.BOTTOM)), newExn)
    }

    case STry(SExprInfo(span, paren, _), body, catchC, forbidC, finallyC) => {
      // Check that all forbids are subtypes of exception
      for (ty <- forbidC)
          isSubtype(ty, Types.EXCEPTION, ty,
                    "All types in 'forbids' clause must be subtypes of Exception " +
                    "type, but "+ ty + " is not.")
      // the resulting type is the join of try, catches, and finally
      var allTypes = List[Type]()
      val newBody = checkExpr(body).asInstanceOf[Block]
              allTypes ::= getType(newBody).getOrElse(return expr)
      val newCatch = catchC match {
        case Some(SCatch(info, name, clauses)) =>
          val newClauses =
              clauses.map(_ match {
                          case SCatchClause(i, t, b) =>
                            // We have to pass the name down
                            // so it can be bound to each exn type in turn
                            /**
                             * Given the CatchClause and an Id, the Id will be bound
                             * to the exception type that the catch clause declares
                             * to catch, and then its body will be type-checked.
                             */
                            isSubtype(t, Types.EXCEPTION, t,
                                      "Catch clauses must catch subtypes of Exception," +
                                      " but " + t + " is not.")
                            // bind id and check the body
                            val newChecker = this.extend(List(NF.makeLValue(name, t)))
                            val newB = newChecker.checkExpr(b).asInstanceOf[Block]
                            SCatchClause(i, t, newB)
                          })
          val newC = SCatch(info, name, newClauses)
          // Gather all the types of the catch clauses
          val clauseTypes =
              newClauses.map(_ match {
                             case SCatchClause(_,_,b) =>
                               getType(b).getOrElse(return expr)
                            })
          // resulting type is the join of those types
          allTypes ::= self.analyzer.join(clauseTypes)
          Some(newC)
        case None => None
      }
      val newFinally = finallyC match {
        case Some(b) =>
          val newB = checkExpr(b).asInstanceOf[Block]
          allTypes ::= getType(newB).getOrElse(return expr)
          Some(newB)
        case None => None
      }
      val newTy = Some(self.analyzer.join(allTypes))
      STry(SExprInfo(span, paren, newTy), newBody, newCatch, forbidC, newFinally)
    }

    // This case is only called for single element arrays ( e.g., [5] )
    // and not for pieces of ArrayElements
    case SArrayElement(SExprInfo(span, paren, _), sargs, element) => {
      val newElement = checkExpr(element)
      val ind = toOption(traits.typeCons(Types.getArrayKName(1))) match {
        case None => toOption(traits.typeCons(Types.ARRAY_NAME)) match {
          case None =>
            signal(expr, "Type " + Types.ARRAY_NAME + " is not available.")
            return expr
          case Some(i) => i.asInstanceOf[TraitIndex]
        }
        case Some(index) => index.asInstanceOf[TraitIndex]
      }
      val elemType = getType(newElement).getOrElse(return expr)
      val newTy = sargs match {
        case Nil =>
          val lower = NF.makeIntArgVal(NU.getSpan(expr), "0")
          val size = NF.makeIntArgVal(NU.getSpan(expr), "1")
          Some(Types.makeArrayKType(1, Useful.list(NF.makeTypeArg(elemType),
                                                   lower, size)))
        case _ =>
          if (staticArgsMatchStaticParams(sargs, toListFromImmutable(ind.staticParameters))) {
            val typ = sargs.head.asInstanceOf[TypeArg].getTypeArg
            isSubtype(elemType, typ, elemType,
                      elemType + " must be a subtype of " + typ + ".")
            Some(Types.makeArrayKType(1, toJavaList(sargs)))
          } else {
            signal(expr, "Explicit static arguments do not match the required " +
                   "arguments for Array1 (" + ind.staticParameters + ".)")
            None
          }
      }
      SArrayElement(SExprInfo(span, paren, newTy), sargs, newElement)
    }

    case SArrayElements(SExprInfo(span, paren, _),
                        sargs, dimension, elements, outermost) => {
      val newElements = elements.map(checkExpr).asInstanceOf[List[ArrayExpr]]
      // We have to create a new visitor that visits ArrayElements and ArrayElement
      // knowing that we are inside of another ArrayElement.
      def getTypeAndBoundsFromArray(typ: Type): (Type, List[BigInteger]) = {
        typ match {
          case ty:TraitType =>
            if (ty.getName.toString.startsWith(NU.nameString(Types.ARRAY_NAME))) {
              var dims = List[BigInteger]()
              for {i <- 2 until ty.getArgs.size if i % 2 == 0} {
                ty.getArgs.get(i) match {
                  case ia:IntArg => ia.getIntVal match {
                    case ib:IntBase => dims ++= List(ib.getIntVal.getIntVal)
                    case _ => signal(expr, "Not yet implemented.")
                  }
                  case _ => signal(expr, "Array type changed.")
                }
              }
              ty.getArgs.get(0) match {
                case ta:TypeArg => return (ta.getTypeArg, dims)
                case _ => signal(expr, "Array type changed.")
              }
            } else signal(expr, "Not an Array.")
          case _ => signal(expr, "Not an Array.")
        }
        (typ, List[BigInteger]())
      }
      var failed = false
      val temp =
        newElements.map(e => getType(checkExpr(e)).map(getTypeAndBoundsFromArray))
      val types = temp.foldLeft(List[Type]())((res, elem) => elem match {
                                              case None => res
                                              case Some((t,_)) => res ++ List(t)})
      val dims  = temp.foldLeft(List[List[BigInteger]]())((res, elem) => elem match {
                                                          case None => res
                                                          case Some((_,is)) => res ++ List(is)})
      // one of your subarrays already failed
      if (temp.size != types.size) failed = true
      val first = dims.head
      var same_size = true
      dims.foreach(d => if (!d.equals(first)) same_size = false)
      val arrayType = self.analyzer.join(types)
      if (!same_size) signal(expr, "Not all subarrays are the same size.")
      // Now try to get array type for the dimension we have
      val ind = toOption(traits.typeCons(Types.getArrayKName(dimension))) match {
        case None => toOption(traits.typeCons(Types.ARRAY_NAME)) match {
          case None =>
            signal(expr, "Type " + Types.ARRAY_NAME + " is not available.")
            return expr
          case Some(i) => i.asInstanceOf[TraitIndex]
        }
        case Some(index) => index.asInstanceOf[TraitIndex]
      }
      if (failed || !same_size) return expr
      val span = NU.getSpan(expr)
      val newTy = sargs match {
        case Nil =>
          // then we just use what we determine to be true
          var inferredArgs = List[StaticArg](NF.makeTypeArg(arrayType))
          inferredArgs ++= List(NF.makeIntArgVal(span, "0"),
                                NF.makeIntArgVal(span, newElements.size.toString))
          for {i <- 0 until dimension-1} {
            val s = first.apply(i)
            inferredArgs ++= List(NF.makeIntArgVal(span, "0"),
                                  NF.makeIntArgVal(span, s.toString))
          }
          // then instantiate and return
          Some(Types.makeArrayKType(dimension, toJavaList(inferredArgs)))
        case _ =>
          if (staticArgsMatchStaticParams(sargs, toListFromImmutable(ind.staticParameters))) {
            // First arg MUST BE a TypeArg, and it must be a supertype of the elements
            val typ = sargs.head.asInstanceOf[TypeArg].getTypeArg
            isSubtype(arrayType, typ, arrayType,
                      "Array elements must be a subtype of explicitly declared " +
                      "type " + typ + ".")
            // Check infered dims against explicit dims
            Some(Types.makeArrayKType(dimension, toJavaList(sargs)))

          } else {
            signal(expr, "Explicit static arguments do not match the required " +
                   "arguments for Array1 (" + ind.staticParameters + ".)")
            None
          }
          None
      }
      SArrayElements(SExprInfo(span, paren, newTy),
                     sargs, dimension, newElements, outermost)
    }

    case expr:DummyExpr => expr

    // Coercions will always be typed.
    case c:CoercionInvocation => c

    case _ => throw new Error(errorMsg("Not yet implemented: ", expr.getClass))
  }

  /** A type checker that signals an error if a spawn expr occurs inside it. */
  class AtomicChecker(current: CompilationUnitIndex,
                      traits: TraitTable,
                      env: STypeEnv,
                      errors: ErrorLog,
                      enclosingExpr: String)
                     (implicit analyzer: TypeAnalyzer,
                               envCache: MMap[APIName, STypeEnv],
                               cycleChecker: CyclicReferenceChecker)
    extends STypeCheckerImpl(current,traits,env,labelExitTypes,errors) {

    override def constructor(current: CompilationUnitIndex,
                             traits: TraitTable,
                             env: STypeEnv,
                             labelExitTypes: JavaMap[Id, Option[JavaSet[Type]]],
                             errors: ErrorLog)
                             (implicit analyzer: TypeAnalyzer,
                                       envCache: MMap[APIName, STypeEnv],
                                       cycleChecker: CyclicReferenceChecker) =
      new AtomicChecker(current, traits, env, errors, enclosingExpr)

    val message = errorMsg("A 'spawn' expression must not occur inside ",
                           enclosingExpr, ".")
    override def checkExpr(e: Expr): Expr = e match {
      case SSpawn(_, _) => syntaxError(e, message); e
      case _ => super.checkExpr(e)
    }
  }
}
