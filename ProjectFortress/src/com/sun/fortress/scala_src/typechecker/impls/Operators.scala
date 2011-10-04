/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.scala_src.typechecker.impls

import com.sun.fortress.compiler.Types
import com.sun.fortress.exceptions.InterpreterBug.bug
import com.sun.fortress.exceptions.StaticError.errorMsg
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.{ExprFactory => EF}
import com.sun.fortress.nodes_util.{NodeFactory => NF}
import com.sun.fortress.nodes_util.{NodeUtil => NU}
import com.sun.fortress.nodes_util.OprUtil
import com.sun.fortress.scala_src.typechecker._
import com.sun.fortress.scala_src.typechecker.Formula._
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.useful.ASTGenHelper._
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.Sets._
import com.sun.fortress.scala_src.useful.SExprUtil._
import com.sun.fortress.scala_src.useful.STypesUtil._

/**
 * Provides the implementation of cases representing some sort of operator
 * expression.
 *
 * This trait must be mixed in with an `STypeChecker with Common` instance
 * in order to provide the full type checker implementation.
 *
 * (The self-type annotation at the beginning declares that this trait must be
 * mixed into STypeChecker along with the Common helpers. This is what
 * allows this trait to implement abstract members of STypeChecker and to
 * access its protected members.)
 */
trait Operators { self: STypeChecker with Common =>

  // ---------------------------------------------------------------------------
  // CHECK IMPLEMENTATION ------------------------------------------------------

  def checkOperators(node: Node): Node = node match {

    case op@SOp(info,api,name,fixity,enclosing) => {
      val tyEnv = api match {
        case Some(api) => getEnvFromApi(api)
        case _ => env
      }
      if (!tyEnv.contains(op)) {
        if ( enclosing ) signal(op, errorMsg("Enclosing operator not found: ", op))
        else signal(op, errorMsg("Operator not found: ", OprUtil.decorateOperator(op)))
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

  def checkExprOperators(expr: Expr, expected: Option[Type]): Expr = expr match {

    // For a tight juxt, create a MathPrimary
    case SJuxt(info, multi, infix, front::rest, false, true) => {
      def toMathItem(exp: Expr): MathItem = {
        val span = NU.getSpan(exp)
        if ( exp.isInstanceOf[TupleExpr] ||
             exp.isInstanceOf[VoidLiteralExpr] ||
             NU.isParenthesized(exp) )
          EF.makeParenthesisDelimitedMI(span, exp)
        else EF.makeNonParenthesisDelimitedMI(span, exp)
      }
      checkExpr(SMathPrimary(info, multi, infix, front, rest.map(toMathItem)))
    }

    // If this juxt is actually a fn app, then rewrite to a fn app.
    case SJuxt(info, multi, infix, front::rest, true, true) => rest.length match {
      case 1 => checkExpr(S_RewriteFnApp(info, front, rest.head))
      case n => // Make sure it is just two exprs.
        signal(expr, errorMsg("TightJuxt denoted as function application but has ",
                              int2Integer(n), "(!= 2) expressions."))
        expr
    }

    /* Loose Juxts are handled using the algorithm in 16.8 of Fortress Spec 1.0
     * ToDo: Revisit
     */
    case SJuxt(SExprInfo(span,paren,optType),
               multi, infix, exprs, isApp, false) => {
      // Check subexpressions
      //val checkedExprs = exprs.map(checkExprIfCheckable).map(_.fold(x=>x,x=>x))
      //if (!haveTypesOrUncheckable(checkedExprs)) { return expr }
      val checkedExprs = exprs.map(checkExpr)
      if(!haveTypes(checkedExprs)) {return expr}
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
            val (nonArrows,remainingChunks) = temp.span((e:Expr) => !isArrows(e))
            chunker(remainingChunks, (arrows,nonArrows)::results)
          } else {
            val (nonArrows,remainingChunks) = (first::rest).span((e:Expr) => !isArrows(e))
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
                                      EF.makeOpExpr(infix,e1,e2) })
        }
      // Right associate everything in a chunk as a _RewriteFnApp
      def associateArrows(fs: List[Expr], oe: Option[Expr]) = oe match {
        case None => fs match {
          case Nil =>
            errors.signal("Empty chunk", expr)
            expr
          case _ =>
            fs.take(fs.size-1).foldRight(fs.last){ (f: Expr, e: Expr) =>
                                                   EF.make_RewriteFnApp(f,e) }
        }
        case Some(e) =>
          fs.foldRight(e){ (f: Expr, e: Expr) => EF.make_RewriteFnApp(f,e) }
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
      def isStringType(t: Type) = isTrue(analyzer.subtype(t, Types.STRING))
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
        STypeCheckerFactory.makeTryChecker(this).
          tryCheckExpr(EF.makeOpExpr(span,
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
                                           EF.makeOpExpr(infix,e1,e2) })
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
                                              EF.make_RewriteFnApp(fn.asInstanceOf[ExprMI].getExpr,
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
                EF.makeOpExpr(op, head, toJavaOption(expr))
              case SSubscriptingMI(_,op,exprs,sargs) =>
                EF.makeSubscriptExpr(span, head,
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
            val fn = EF.make_RewriteFnApp(front,
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
                                      EF.makeVoidLiteralExpr(span)
                                    } else e.asInstanceOf[ExprMI].getExpr )
            // Treat the sequence that remains as a multifix application of
            // the juxtaposition operator.
            // The rules for multifix operators then apply.
            val multi_op_expr =
              STypeCheckerFactory.makeTryChecker(this).
                tryCheckExpr(EF.makeOpExpr(span,
                                           multi,
                                           toJavaList(head::newTail)))
            multi_op_expr.getOrElse {
              newTail.foldLeft(head) { (r:Expr, e:Expr) =>
                checkExpr(EF.makeOpExpr(NU.spanTwo(r, e), infix, r, e))
              }
            }
          }
      }
    }

    // First try to type check this expression as a multifix operator expression.
    // If that fails, type check it as some number of applications of the infix
    // operator, left associatively.
    case SAmbiguousMultifixOpExpr(info, infixOp, multifixOp, args) => {
      def infixAssociate(e1: Expr, e2: Expr) = SOpExpr(info, infixOp, List(e1, e2))
      STypeCheckerFactory.makeTryChecker(this).
        tryCheckExpr(SOpExpr(info, multifixOp, args)).
        getOrElse(checkExpr(args.reduceLeft(infixAssociate)))
    }

    case SChainExpr(SExprInfo(span,parenthesized,_), first, links, andOp) => {

      // Build up a list of OpExprs from the Links (in reverse).
      def makeOpExpr(prevAndResult: (Expr, List[OpExpr]),
                     nextLink: Link)
                     : (Expr, List[OpExpr]) = {

        val (prev, result) = prevAndResult
        val next = nextLink.getExpr()
        val op = nextLink.getOp()
        val newExpr = EF.makeOpExpr(NU.spanTwo(prev, next),
                                    op,
                                    prev,
                                    next)
        (next, newExpr :: result)
      }
      val (_, conjuncts0) = links.foldLeft((first, List[OpExpr]()))(makeOpExpr)
      val conjuncts = conjuncts0.reverse


      // Check that every OpExpr formed from Links is a Boolean, returning the
      // checked OpExpr.
      def checkBoolean(expr: OpExpr): Option[OpExpr] = {
        val checked =
          checkExpr(expr,
                    Types.BOOLEAN,
                    "The chained expression had type %s, but should have type %s.",
                    expr).asInstanceOf[OpExpr]
        getType(checked).map(_ => checked)
      }
      val checkedConjuncts = conjuncts.flatMap(checkBoolean)
      if (conjuncts.size != checkedConjuncts.size) return expr

      // For each link, insert the checked FnRef and RHS.
      val newLinks = (links, checkedConjuncts).zipped.map {
        case (SLink(info, _, _), SOpExpr(_, op, List(_, rhs))) =>
          SLink(info, op, rhs).asInstanceOf[Link]
      }

      // Get the checked first expr out of the first conjunct.
      val checkedFirst = checkedConjuncts.head.getArgs().get(0)

      // Check a dummy OpExpr for the AND operation.
      val andExpr = EF.makeOpExpr(NF.typeSpan,
                                  andOp,
                                  makeDummyFor(Types.BOOLEAN),
                                  makeDummyFor(Types.BOOLEAN))

      // Check it and pull out the checked AND op.
      val checkedAndOp = checkExpr(andExpr).asInstanceOf[OpExpr].getOp

      SChainExpr(SExprInfo(span, parenthesized, Some(Types.BOOLEAN)),
                 checkedFirst,
                 newLinks,
                 checkedAndOp)
    }

    // A singleton ordinary assignment. e.g. x := e
    case SAssignment(SExprInfo(span, paren, _), lhs :: Nil, None, rhs, _) => {
      // Cast is safe since grammar dictates the LHS can only be an expression.

      val (checkedLhs: Lhs, checkedRhs: Expr) = lhs match {
        case lhs @ SVarRef(info, id, _, _) =>
          // Check the var ref and make sure it is mutable.
          val checkedLhs = checkExpr(lhs).asInstanceOf[VarRef]
          val lhsType = getType(checkedLhs).getOrElse(return expr)

          if (!env.isMutable(id)) {
            signal(expr, "Cannot assign to immutable variable: %s".format(id))
            return expr
          }

          // Check the RHS with the expected type of the var ref's type.
          val checkedRhs = checkExpr(rhs, lhsType, "Could not assign an expression of type %s to variable "+id+" of type %s.")
          if (getType(checkedRhs).isNone) return expr
          (checkedLhs, checkedRhs)

        case SFieldRef(SExprInfo(span, paren, _), obj, field) =>
          // Check the receiver and lookup the setter with this name.
          val checkedObj = checkExpr(obj)
          val receiverType = getType(checkedObj).getOrElse(return expr)
          val fieldType = getSetterType(field, receiverType)
          val checkedLhs = fieldType match {
            case Some(_) => SFieldRef(SExprInfo(span, paren, fieldType), checkedObj, field)
            case None =>
              signal(expr, "%s has no setter called %s".format(receiverType, field))
              return expr
          }

          // Check the RHS with the expected type of the field ref's type.
          val checkedRhs = checkExpr(rhs, fieldType.get, "Could not assign an expression of type %s to field "+receiverType+"."+field+" of type %s.")
          if (getType(checkedRhs).isNone) return expr
          (checkedLhs, checkedRhs)

        case SSubscriptExpr(info @ SExprInfo(span, paren, _), obj, subs, Some(op), sargs) =>
          // Check obj and subs ahead of time to make sure they all get typed.
          val checkedObj = checkExpr(obj)
          val checkedSubs = subs.map(checkExpr)
          val checkedRhs = checkExpr(rhs)
          if (!haveTypes(checkedObj :: checkedRhs :: checkedSubs)) return expr
          val rhsType = getType(checkedRhs).get

          // Check this as a normal subscript expr but with the additional RHS arg.
          // NOTE! Subscript assignment operators have the RHS as the first param
          //   and the indexing subs as the remaining parameters.
          val newExpr = SSubscriptExpr(info,
                                       makeDummyFor(checkedObj),
                                       (checkedRhs :: checkedSubs).map(makeDummyFor),
                                       Some(NF.makeOp(op, op.getText() + ":=")),
                                       sargs)
          val maybeCheckedExpr =
            STypeCheckerFactory.makeTryChecker(this).tryCheckExpr(newExpr)

          // Signal the error if this did not apply.
          if (maybeCheckedExpr.isNone) {
            val subsStr = checkedSubs.map(getType(_).get).mkString(", ")
            val subscriptStr = op.getText.replace(" ", subsStr)
            val exprStr = "%s.%s".format(getType(checkedObj).get, subscriptStr)
            signal(expr, "Could not assign an expression of type %s with subscript operator %s.".format(rhsType, exprStr))
            return expr
          }

          // Rebuild the subscript expression.
          // TODO: Take care of coercions!
          val SSubscriptExpr(_, _, _, checkedOp, infSargs) = maybeCheckedExpr.get
          val checkedSubscriptExpr =
            SSubscriptExpr(SExprInfo(span, paren, Some(Types.VOID)),
                           checkedObj,
                           checkedSubs,
                           checkedOp,
                           infSargs)
          (checkedSubscriptExpr, checkedRhs)

        case _ => bug("unexpected LHS for assignment")
      }

      // Rebuild the assignment with the checked info.
      SAssignment(SExprInfo(span, paren, Some(Types.VOID)),
                  List(checkedLhs),
                  None,
                  checkedRhs,
                  Nil)
    }

    // A singleton compound assignment. e.g. x += e
    case SAssignment(info @ SExprInfo(span, paren, _), lhs :: Nil, Some(op), rhs, _) => {
      // 1. Desugar x += y into x := x + y
      // 2. Check that desguaring using the other case
      // 3. Get out the op ref and lhs
      // 4. Stuff them together into an Assignment
      val opExpr = EF.makeOpExpr(span, op, lhs.asInstanceOf[Expr], rhs)

      val checkedAssn = checkExpr(SAssignment(info, List(lhs), None, opExpr, Nil))
      if (getType(checkedAssn).isNone) return expr

      // Pull out the checked LHS and checked OpExpr.
      val SAssignment(_, checkedLhs :: Nil, _, checkedOpExpr, _) = checkedAssn

      // Check for coercions. There can be one on the OpExpr itself, and there
      // can be one on each of the two args to the OpExpr.

      // See if there was a coercion on the entire operator expression.
      val (realCheckedOpExpr, maybeOuterCoercion) = checkedOpExpr match {
        case c:CoercionInvocation => (c.getArg, Some(c))
        case _ => (checkedOpExpr, None)
      }

      // The checked OpExpr will have two args: the checked LHS and the checked RHS.
      val SOpExpr(_, checkedOp, List(argL, checkedRhs)) = realCheckedOpExpr

      // See if there was a coercion on the LHS arg.
      val maybeInnerCoercion = argL match {
        case c:CoercionInvocation => Some(c)
        case _ => None
      }

      // Create the info for the compound assignment's op and coercions.
      val assnInfo =
        SCompoundAssignmentInfo(checkedOp, maybeOuterCoercion, maybeInnerCoercion)

      SAssignment(SExprInfo(span, paren, Some(Types.VOID)),
                  List(checkedLhs),
                  Some(op),
                  checkedRhs,
                  List(assnInfo))
    }

    // Any arbitrary assignment.
    case SAssignment(info @ SExprInfo(span, paren, _), lhses, maybeOp, rhs, _) => {
      val checkedRhs = checkExpr(rhs)
      val rhsType = getType(checkedRhs).getOrElse(return expr)
      if (!enoughElementsForType(lhses, rhsType)) return expr

      // Create dummy expressions for each constituent type on the RHS.
      val rhsTypes = typeIterator(rhsType).toList

      // Create a list of singleton assignments.
      val checkedAssignments = (lhses, rhsTypes).zipped.map { (lhs, rhsType) => {
        val span = NU.getSpan(lhs.asInstanceOf[Expr])
        checkExpr(SAssignment(SExprInfo(span, false, None),
                              List(lhs),
                              maybeOp,
                              makeDummyFor(rhsType, span),
                              Nil))
      }}
      if (!haveTypes(checkedAssignments)) return expr

      // Pull out the checked LHS and checked OpRef for each assignment.
      val (checkedLhses, infos) = (checkedAssignments.map {
        case SAssignment(_, lhs :: Nil, _, _, info) => (lhs, info)
        case _ => bug("impossible result of checking")
      }).unzip

      // Put the assignment back together.
      SAssignment(SExprInfo(span, paren, Some(Types.VOID)),
                  checkedLhses,
                  maybeOp,
                  checkedRhs,
                  infos.flatMap(x => x))
    }

    case _ => throw new Error(errorMsg("Not yet implemented: ", expr.getClass))
  }

}
