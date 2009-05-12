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
import com.sun.fortress.nodes_util.ExprFactory
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

    case id@SId(info,api,name) => api match {
      case Some(api) => {
        val newName = handleAliases(id, api, toList(current.ast.getImports))
        getTypeFromName( newName ) match {
          case Some(ty) =>
            if ( ty.isInstanceOf[NamedType] ) {
              /*
              // Type was declared in that API, so it's not qualified;
              // prepend it with the API.
              if ( ty.instanceOf[NamedType].getName.getApiName.isNone )
                ty = NodeFactory.makeNamedType(api, ty)
              */
              id
            } else id
          case _ =>
            // Operators are never qualified in source code,
            // so if 'name' is qualified and not found,
            // it must be an Id, not an Op.
            errors.signal("Attempt to reference unbound variable: " + id, id)
            id
        }
      }
      case _ => {
        getTypeFromName( id ) match {
          case Some(ty) => ty match {
            case SLabelType(_) => // then, newName must be an Id
              errors.signal("Cannot use label name " + id + " as an identifier.",
                            id)
              id
            case _ => id
          }
          case _ =>
            errors.signal("Variable '" + id + "' not found.", id)
            id
        }
      }
    }

    case _ => throw new Error("not yet implemented: " + node.getClass)
  }

  def checkExpr(expr: Expr, expected: Option[Type],
                first: String, second: String): Expr = {
    val newExpr = checkExpr(expr)
    val newType = inferredType(newExpr) match {
      case Some(typ) => expected match {
        case Some(t) =>
          checkSubtype(typ, t, expr,
                       first + " has type " + typ + ", but " + second +
                       " type is " + t + ".")
          typ
        case _ => typ
      }
      case _ => throw new Error("Type is not inferred for: " + expr)
    }
    ExprUtil.addType(newExpr, newType)
  }

  def checkExpr(expr: Expr, expected: Option[Type], message: String): Expr = {
    val newExpr = checkExpr(expr)
    val newType = inferredType(newExpr) match {
      case Some(typ) => expected match {
        case Some(t) =>
          checkSubtype(typ, t, expr,
                       message + " has type " + typ + ", but it must have " +
                       t + " type.")
          typ
        case _ => typ
      }
      case _ => throw new Error("Type is not inferred for: " + expr)
    }
    ExprUtil.addType(newExpr, newType)
  }

  class AtomicChecker(current: CompilationUnitIndex, traits: TraitTable,
                      env: TypeEnv, analyzer: TypeAnalyzer, errors: ErrorLog,
                      factory: CoercionOracleFactory, enclosingExpr: String)
      extends STypeChecker(current,traits,env,analyzer,errors,factory) {
    val message = "A 'spawn' expression must not occur inside " +
                  enclosingExpr + "."
    override def checkExpr(e: Expr): Expr = e match {
      case SSpawn(_, _) => errors.signal(message, e); e
      case _ => super.checkExpr(e)
    }
  }

  private def forAtomic(expr: Expr, enclosingExpr: String) =
    new AtomicChecker(current,traits,env,analyzer,errors,factory,enclosingExpr).checkExpr(expr)

  def checkExpr(expr: Expr): Expr = expr match {
    case s@SSpawn(SExprInfo(span,paren,optType), body) => {
      val newExpr = this.extendWithout(s, labelExitTypes.keySet).checkExpr(body)
      val newType = inferredType(newExpr) match {
        case Some(typ) => typ
        case _ => throw new Error("Type is not inferred for: " + body)
      }
      SSpawn(SExprInfo(span,paren,Some(Types.makeThreadType(newType))), newExpr)
    }

    case SAtomicExpr(SExprInfo(span,paren,optType), body) => {
      val newExpr = forAtomic(body, "an 'atomic' expression")
      SAtomicExpr(SExprInfo(span,paren,inferredType(newExpr)), newExpr)
    }

      /* ToDo for Compiled0
    case SFnRef(SExprInfo(span,paren,optType),
                sargs, depth, name, names, overloadings, types) => {
        expr
    }

    case SStringLiteralExpr(info, text) => {
        expr
    }
      */

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
        bug(expr, "TightJuxt denoted as function application but has " +
            n + "(!= 2) expressions.")
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
            if ( ! (isStringType(e) || isStringType(f)) )
              throw new Error("Neither element is of type String in " +
                              "a juxtaposition of String elements.")
            else e
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
      } else throw new Error("Type is not inferred for: " + expr)
    }

    /* Temporary code for Tight Juxtapositions
     */
    case SJuxt(SExprInfo(span,paren,optType), multi, infix, exprs, false, true) => {
      val checkedExprs = exprs.map((e:Expr)=>checkExpr(e))
      if(haveInferredTypes(checkedExprs)){
        //check if there are any functions
        if(checkedExprs.exists((e:Expr)=>isArrows(e))){
          //ToDo: some static checks
          //Left associate
          val leftAssociated = checkedExprs.tail.foldLeft(checkedExprs.head){(e1: Expr, e2: Expr) => ExprFactory.makeOpExpr(infix,e1,e2)}
          checkExpr(leftAssociated)
        }
        else{
          //find the left most function
          val prefix = checkedExprs.takeWhile((e:Expr)=> !isArrows(e))
          //ToDo: check fn is not the last element of the list
          val fn::arg::suffix = checkedExprs.dropWhile((e:Expr)=> !isArrows(e))
          //ToDo: check that its argument is parenthesized
          //Replace fn and arg with a _ReWriteFnApp and recurse
          val fnApp = ExprFactory.make_RewriteFnApp(fn,arg)
          val newExprs = prefix++(fnApp::suffix)
          checkExpr(SJuxt(SExprInfo(span,paren,optType),
                          multi,infix,newExprs,false,true))
        }
      }
      else{
        SJuxt(SExprInfo(span,paren,optType),
              multi,infix,checkedExprs,false,true)
      }
    }

    case _ => throw new Error("Not yet implemented: " + expr.getClass)
  }

}
