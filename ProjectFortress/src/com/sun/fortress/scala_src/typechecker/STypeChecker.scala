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

import _root_.java.util.{Map => JavaMap}
import _root_.java.util.{HashMap => JavaHashMap}
import _root_.java.util.{Set => JavaSet}
import edu.rice.cs.plt.tuple.{Option => JavaOption}
import com.sun.fortress.nodes._
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.nodes_util.NodeFactory
import com.sun.fortress.nodes_util.ExprFactory
import com.sun.fortress.nodes_util.NodeUtil
import com.sun.fortress.nodes_util.OprUtil
import com.sun.fortress.exceptions.StaticError
import com.sun.fortress.exceptions.TypeError
import com.sun.fortress.compiler.index.CompilationUnitIndex
import com.sun.fortress.compiler.typechecker.TypeAnalyzer
import com.sun.fortress.compiler.typechecker.TypeEnv
import com.sun.fortress.compiler.typechecker.TypesUtil
import com.sun.fortress.compiler.Types
import com.sun.fortress.scala_src.useful.ASTGenHelper._
import com.sun.fortress.scala_src.useful.ErrorLog
import com.sun.fortress.scala_src.useful.ExprUtil
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.exceptions.InterpreterBug.bug
import com.sun.fortress.useful.HasAt

object STypeCheckerFactory {
  def make(current: CompilationUnitIndex, traits: TraitTable, env: TypeEnv,
           analyzer: TypeAnalyzer) = {
    val errors = new ErrorLog()
    new STypeChecker(current, traits, env, analyzer, errors,
                     new CoercionOracleFactory(traits, analyzer, errors))
  }
  def make(current: CompilationUnitIndex, traits: TraitTable, env: TypeEnv,
           analyzer: TypeAnalyzer, errors: ErrorLog,
           factory: CoercionOracleFactory) = {
    new STypeChecker(current, traits, env, analyzer, errors, factory)
  }
}

class STypeChecker(current: CompilationUnitIndex, traits: TraitTable,
                   env: TypeEnv, analyzer: TypeAnalyzer, errors: ErrorLog,
                   factory: CoercionOracleFactory) {

  val coercionOracle = factory.makeOracle(env)

  private var labelExitTypes: JavaMap[Id, JavaOption[JavaSet[Type]]] =
    new JavaHashMap[Id, JavaOption[JavaSet[Type]]]()

  private def extend(newEnv: TypeEnv, newAnalyzer: TypeAnalyzer) =
    STypeCheckerFactory.make(current, traits, newEnv, newAnalyzer, errors, factory)

  private def extendWithout(declSite: Node, names: JavaSet[Id]) =
    STypeCheckerFactory.make(current, traits, env.extendWithout(declSite, names),
                     analyzer, errors, factory)

  private def signal(msg:String, hasAt:HasAt) =
    errors.signal(msg, hasAt)

  private def signal(hasAt:HasAt, msg:String) =
    errors.signal(msg, hasAt)

  private def inferredType(expr:Expr): Option[Type] =
    scalaify(expr.getInfo.getExprType).asInstanceOf[Option[Type]]

  private def haveInferredTypes(exprs: List[Expr]): Boolean =
    exprs.forall((e:Expr) => inferredType(e).isDefined)

  private def isArrows(expr: Expr): Boolean =
    TypesUtil.isArrows(inferredType(expr).get).asInstanceOf[Boolean]

  private def checkSubtype(subtype:Type, supertype:Type, node:Node, error:String) = {
    val judgement = analyzer.subtype(subtype, supertype).isTrue
    if (! judgement) signal(error, node)
    judgement
  }

  private def handleAliases(name: Id, api: APIName, imports: List[Import]): Id = {
    def getAliases(imp: Import): Option[Id] = imp match {
      case SImportNames(info, foreignLanguage, aliasApi, aliases) =>
        if ( api.equals(aliasApi) ) {
          def getName(aliasedName: AliasedSimpleName): Option[Id] = aliasedName match {
            case SAliasedSimpleName(_, newName, Some(alias)) =>
              if ( alias.equals(name) ) Some(newName.asInstanceOf)
              else None
            case _ => None
          }
          aliases.flatMap(getName).find((x:Id) => true)
        } else None
      case _ => None
    }
    imports.flatMap(getAliases).find((x:Id) => true).getOrElse(name)
  }

  private def getEnvFromApi(api: APIName): TypeEnv =
    TypeEnv.make( traits.compilationUnit(api) )

  private def getTypeFromName(name: Name): Option[Type] = name match {
    case id@SId(info, api, name) => api match {
      case Some(api) => toOption(getEnvFromApi(api).getType(id))
      case _ => toOption(env.getType(id))
    }
    case _ => None
  }

  def getErrors(): List[StaticError] = errors.errors

  def check(node:Node):Node = node match {
    case SComponent(info, name, imports, decls, isNative, exports)  =>
      SComponent(info, name, imports,
                 decls.map((n:Decl) => check(n).asInstanceOf[Decl]),
                 isNative, exports)

    /* Matches if a function declaration does not have a body expression. */
    case f@SFnDecl(info,header,unambiguousName,None,implementsUnambiguousName) => f

    /* Matches if a function declaration has a body expression. */
    case f@SFnDecl(info,
                   SFnHeader(statics,mods,name,wheres,throws,contract,params,returnType),
                   unambiguousName, Some(body), implementsUnambiguousName) => {
      val newEnv = env.extendWithStaticParams(statics).extendWithParams(params)
      val newAnalyzer = analyzer.extend(statics, wheres)
      val newChecker = this.extend(newEnv, newAnalyzer)

      val newContract = contract match {
        case Some(c) => Some(newChecker.check(c))
        case None => contract
      }
      val newBody = newChecker.checkExpr(body, returnType, "Function body", "declared return")
      SFnDecl(info,
              SFnHeader(statics, mods, name, wheres, throws,
                        newContract.asInstanceOf[Option[Contract]],
                        params, inferredType(newBody)),
              unambiguousName, Some(newBody), implementsUnambiguousName)
    }

    /* ToDo for Compiled6
    case t@STraitDecl(info,header,excludes,comprises,hasEllises,self) => t
    case o@SObjectDecl(info,header,params,self) => o
    */

    case id@SId(info,api,name) => {
      api match {
        case Some(apiName) => {
          val newName = handleAliases(id, apiName, toList(current.ast.getImports))
          getTypeFromName( newName ) match {
            case Some(ty) =>
              if ( ty.isInstanceOf[NamedType] ) {
                // Type was declared in that API, so it's not qualified;
                // prepend it with the API.
                /*
                if ( ty.asInstanceOf[NamedType].getName.getApiName.isNone )
                  _type = NodeFactory.makeNamedType(apiName, ty.asInstanceOf[NamedType])
                */
              }
            case _ =>
              // Operators are never qualified in source code,
              // so if 'name' is qualified and not found,
              // it must be an Id, not an Op.
              signal(id, "Attempt to reference unbound variable: " + id)
          }
        }
        case _ => {
          getTypeFromName( id ) match {
            case Some(ty) => ty match {
              case SLabelType(_) => // then, newName must be an Id
                signal(id, "Cannot use label name " + id + " as an identifier.")
              case _ =>
            }
            case _ => signal(id, "Variable '" + id + "' not found.")
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
          if ( enclosing ) signal(op, "Enclosing operator not found: " + op)
          else signal(op, "Operator not found: " + OprUtil.decorateOperator(op))
        case _ =>
      }
      op
    }

    case _ => throw new Error("not yet implemented: " + node.getClass)
  }

  def checkExpr(expr: Expr, expected: Option[Type],
                first: String, second: String): Expr = {
    val newExpr = checkExpr(expr)
    inferredType(newExpr) match {
      case Some(typ) => expected match {
        case Some(t) =>
          checkSubtype(typ, t, expr,
                       first + " has type " + typ + ", but " + second +
                       " type is " + t + ".")
          ExprUtil.addType(newExpr, typ)
        case _ => ExprUtil.addType(newExpr, typ)
      }
      case _ => signal(expr, "Type is not inferred for: " + expr); expr
    }
  }

  def checkExpr(expr: Expr, expected: Option[Type], message: String): Expr = {
    val newExpr = checkExpr(expr)
    inferredType(newExpr) match {
      case Some(typ) => expected match {
        case Some(t) =>
          checkSubtype(typ, t, expr,
                       message + " has type " + typ + ", but it must have " +
                       t + " type.")
          ExprUtil.addType(newExpr, typ)
        case _ => ExprUtil.addType(newExpr, typ)
      }
      case _ => signal(expr, "Type is not inferred for: " + expr); expr
    }
  }

  class AtomicChecker(current: CompilationUnitIndex, traits: TraitTable,
                      env: TypeEnv, analyzer: TypeAnalyzer, errors: ErrorLog,
                      factory: CoercionOracleFactory, enclosingExpr: String)
      extends STypeChecker(current,traits,env,analyzer,errors,factory) {
    val message = "A 'spawn' expression must not occur inside " +
                  enclosingExpr + "."
    override def checkExpr(e: Expr): Expr = e match {
      case SSpawn(_, _) => signal(e, message); e
      case _ => super.checkExpr(e)
    }
  }

  private def forAtomic(expr: Expr, enclosingExpr: String) =
    new AtomicChecker(current,traits,env,analyzer,errors,factory,enclosingExpr).checkExpr(expr)

  def checkExpr(expr: Expr): Expr = expr match {
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
          Some(checkExpr(l, Some(Types.REGION), "Location of the block"))
        case None => loc
      }
      exprs.reverse match {
        case Nil =>
          SBlock(SExprInfo(span,parenthesized,Some(Types.VOID)),
                 newLoc, false, withinDo, exprs)
        case last::rest =>
        val allButLast = rest.map((e: Expr) => checkExpr(e, Some(Types.VOID),
                                                         "Non-last expression in a block"))
          val lastExpr = checkExpr(last)
          val newExprs = (lastExpr::allButLast).reverse
          SBlock(SExprInfo(span,parenthesized,inferredType(lastExpr)),
                 newLoc, false, withinDo, newExprs)
      }
    }

    case s@SSpawn(SExprInfo(span,paren,optType), body) => {
      val newExpr = this.extendWithout(s, labelExitTypes.keySet).checkExpr(body)
      inferredType(newExpr) match {
        case Some(typ) =>
          SSpawn(SExprInfo(span,paren,Some(Types.makeThreadType(typ))), newExpr)
        case _ => signal(body, "Type is not inferred for: " + body); expr
      }
    }

    case SAtomicExpr(SExprInfo(span,paren,optType), body) => {
      val newExpr = forAtomic(body, "an 'atomic' expression")
      SAtomicExpr(SExprInfo(span,paren,inferredType(newExpr)), newExpr)
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
        signal(expr, "TightJuxt denoted as function application but has " +
               n + "(!= 2) expressions.")
        expr
    }

    /* Loose Juxts are handled using the algorithm in 16.8 of Fortress Spec 1.0
     */
    case SJuxt(SExprInfo(span,paren,optType),
               multi, infix, exprs, isApp, false) => {
      // Check subexpressions
      val checkedExprs = exprs.map(checkExpr)
      if ( haveInferredTypes(checkedExprs) ) {
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
              val arrows = first::rest.takeWhile(isArrows)
              val dropArrows = rest.dropWhile(isArrows)
              val nonArrows = dropArrows.takeWhile((e:Expr) => ! isArrows(e))
              val dropNonArrows = dropArrows.dropWhile((e:Expr) => ! isArrows(e))
              chunker(dropNonArrows, (arrows,nonArrows)::results)
            } else {
              val nonArrows = first::rest.takeWhile((e:Expr) => ! isArrows(e))
              val dropNonArrows = rest.dropWhile((e:Expr) => ! isArrows(e))
              chunker(dropNonArrows, (List(),nonArrows)::results)
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
            case Nil => bug(expr, "Empty chunk")
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
        val types = associatedChunks.map((e: Expr) => inferredType(e).get)
        def isStringType(t: Type) = analyzer.subtype(t, Types.STRING).isTrue
        if ( types.exists(isStringType) ) {
          def stringCheck(e: Type, f: Type) =
            if ( ! (isStringType(e) || isStringType(f)) ) {
              signal(expr, "Neither element is of type String in " +
                     "a juxtaposition of String elements.")
              e
            } else e
          types.take(types.size-1).foldRight(types.last)(stringCheck)
        }
        // (2) Treat the sequence that remains as a multifix application
        //     of the juxtaposition operator.
        //     The rules for multifix operators then apply.
        val multiOpExpr = checkExpr(ExprFactory.makeOpExpr(span, paren, toJavaOption(optType),
                                                           multi, toJavaList(associatedChunks)))
        if ( inferredType(multiOpExpr).isDefined ) multiOpExpr
        else {
          // If not, left associate as InfixJuxts
          associatedChunks match {
            case Nil => bug(expr, "Empty juxt")
            case head::tail =>
              checkExpr(tail.foldLeft(head){ (e1: Expr, e2: Expr) =>
                                             ExprFactory.makeOpExpr(infix,e1,e2) })
          }
        }
      } else signal(expr, "Type is not inferred for: " + expr); expr
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
            case Some(e) => signal(item, "Two consecutive ^s.")
          }
          case SSubscriptingMI(_,_,_,_) => exponent match {
            case Some(e) =>
              signal(item, "Exponentiation followed by subscripting is illegal.")
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
        case _ =>
      }
      def expectParenedExprItem(item: MathItem) =
        if ( ! isParenedExprItem(item) )
          signal(item, "Argument to function must be parenthesized.")
      def expectExprMI(item: MathItem) =
        if ( ! isExprMI(item) )
          signal(item, "Item at this location must be an expression, not an operator.")
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
                                          !isFunctionItem(e).asInstanceOf[Boolean])
        others match {
          case fn::arg::suffix => arg match {
            // It is a static error if either the argument is not parenthesized,
            case SNonParenthesisDelimitedMI(_,e) =>
              signal(e, "Tightly juxtaposed expression should be parenthesized.")
              (first, Nil)
            case SParenthesisDelimitedMI(i,e) => {
              // or the argument is immediately followed by a non-expression element.
              suffix match {
                case third::more =>
                  if ( ! isExprMI(third) )
                    signal(third, "An expression is expected.")
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
              signal(left.last, "An expression is expected.")
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
                signal(item, "Non-expression element is expected.")
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
      inferredType( newFront ) match {
        case None => signal(front, "Type is not inferred for: " + front); front
        case Some(t) =>
          // If front is a fn followed by an expr, we reassociate
          if ( TypesUtil.isArrows(t).asInstanceOf[Boolean] && isExprMI(second) ) {
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
                                      signal(e, "An expression is expected.")
                                      ExprFactory.makeVoidLiteralExpr(span)
                                    } else e.asInstanceOf[ExprMI].getExpr )
            // Treat the sequence that remains as a multifix application of
            // the juxtaposition operator.
            // The rules for multifix operators then apply.
            val multi_op_expr = checkExpr( ExprFactory.makeOpExpr(span, multi,
                                                                  toJavaList(head::newTail)) )
            inferredType(multi_op_expr) match {
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

    /* ToDo for Compiled0
    case SFnRef(SExprInfo(span,paren,optType),
                sargs, depth, name, names, overloadings, types) => {
        expr
    }

                        abstract FunctionalRef(List<StaticArg> staticArgs,
                                               int lexicalDepth,
                                               IdOrOp originalName,
                                               List<IdOrOp> names,
                                               Option<List<FunctionalRef>> overloadings,
                                               Option<Type> overloadingType);

    */

    case SDo(SExprInfo(span,parenthesized,_), fronts) => {
      val fs = fronts.map(checkExpr).asInstanceOf[List[Block]]
      if ( haveInferredTypes(fs) ) {
          // Get union of all clauses' types
          val frontTypes =
            fs.take(fs.size-1).foldRight(inferredType(fs.last).get)
              { (e:Expr, t:Type) => analyzer.join(inferredType(e).get, t) }
          SDo(SExprInfo(span,parenthesized,Some(frontTypes)), fs)
      } else signal(expr, "Type is not inferred for: " + expr); expr
    }

    /* ToDo for Compiled3
    case SIf(info, clauses, elseC) => {
        expr
    }
    */

    /* ToDo for Compiled6
    case SVarRef(SExprInfo(span,paren,optType), id, sargs, depth) => {
        expr
    }
    */

    case _ => throw new Error("Not yet implemented: " + expr.getClass)
    // "\n" + expr.toStringVerbose())
  }

}
