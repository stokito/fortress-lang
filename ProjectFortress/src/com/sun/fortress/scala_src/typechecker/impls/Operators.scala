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

import com.sun.fortress.compiler.typechecker.TypeEnv
import com.sun.fortress.compiler.Types
import com.sun.fortress.exceptions.StaticError.errorMsg
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.ExprFactory
import com.sun.fortress.nodes_util.NodeUtil
import com.sun.fortress.nodes_util.OprUtil
import com.sun.fortress.scala_src.typechecker._
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
 * This trait must be mixed in with an `STypeCheckerBase with Common` instance
 * in order to provide the full type checker implementation.
 * 
 * (The self-type annotation at the beginning declares that this trait must be
 * mixed into STypeCheckerBase along with the Common helpers. This is what
 * allows this trait to implement abstract members of STypeCheckerBase and to
 * access its protected members.)
 */
trait Operators { self: STypeCheckerBase with Common =>

  // ---------------------------------------------------------------------------
  // CHECK IMPLEMENTATION ------------------------------------------------------
  
  def checkOperators(node: Node): Node = node match {

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

  def checkExprOperators(expr: Expr, expected: Option[Type]): Expr = expr match {

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

    case _ => throw new Error(errorMsg("Not yet implemented: ", expr.getClass))
  }
  
}
