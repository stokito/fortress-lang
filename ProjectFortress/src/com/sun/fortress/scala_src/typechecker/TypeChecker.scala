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

import com.sun.fortress.nodes._
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.nodes_util.NodeFactory
import com.sun.fortress.nodes_util.ExprFactory
import com.sun.fortress.nodes_util.NodeUtil
import com.sun.fortress.exceptions.StaticError
import com.sun.fortress.exceptions.TypeError
import com.sun.fortress.compiler.index.CompilationUnitIndex
import com.sun.fortress.compiler.typechecker.TypeAnalyzer
import com.sun.fortress.compiler.typechecker.TraitTable
import com.sun.fortress.compiler.typechecker.TypeEnv
import com.sun.fortress.compiler.typechecker.TypesUtil
import com.sun.fortress.compiler.Types
import com.sun.fortress.scala_src.useful.ASTGenHelper._
import com.sun.fortress.nodes_util.ExprFactory
import com.sun.fortress.useful.NI
import com.sun.fortress.exceptions.InterpreterBug.bug;

class TypeChecker(current: CompilationUnitIndex, traits: TraitTable) {

  var errors = List[StaticError]()

  private def signal(msg:String,node:Node) = { errors = errors ::: List(TypeError.make(msg,node)) }

  private def inferredType(expr:Expr): Option[Type] = scalaify(expr.getInfo.getExprType).asInstanceOf

  private def haveInferredTypes(exprs: List[Expr]): Boolean = exprs.forall((e:Expr)=>inferredType(e).isDefined)

  private def isArrows(expr: Expr): Boolean = TypesUtil.isArrows(inferredType(expr).get).asInstanceOf
  
  private def checkSubtype(subtype:Type,supertype:Type,senv:TypeAnalyzer,node:Node,error:String) = {
    val judgement = senv.subtype(subtype,supertype).isTrue
    if (! judgement) signal(error,node)
    judgement
  }

  def check(node:Node,env:TypeEnv,senv:TypeAnalyzer):Node = node match {
    case Component(info,name,imports,decls,isNative,exports)  => 
      Component(info,name,imports,decls.map((n:Decl)=>check(n,env,senv).asInstanceOf),isNative,exports)

    case f@FnDecl(info,header,unambiguousName,None,implementsUnambiguousName) => f

    case f@FnDecl(info,FnHeader(statics,mods,name,wheres,throws,contract,params,returnType),
                  unambiguousName,Some(body),implementsUnambiguousName) => {
      val newEnv = env.extendWithStaticParams(statics).extendWithParams(params)
      val newSenv = senv.extend(statics,wheres)

      val newContract = contract match {
        case Some(c) => Some(check(c,newEnv,newSenv))
        case None => contract
      }
      val newBody = checkExpr(body,newEnv,newSenv,returnType)

      val newReturnType = inferredType(newBody) match {
        case Some(typ) => returnType match {
          case None => Some(typ)
          case _ => returnType
        }
        case _ => returnType
      }
      FnDecl(info, FnHeader(statics,mods,name,wheres,throws,newContract.asInstanceOf,params,newReturnType), 
             unambiguousName, Some(newBody), implementsUnambiguousName)
    }
    
    /* Matches if block is not an atomic block. */
    case Block(ExprInfo(span,parenthesized,resultType),loc,false,withinDo,exprs) => exprs.reverse match {
      case Nil =>
        Block(ExprInfo(span,parenthesized,Some(Types.VOID)),loc,false,withinDo,exprs)   
      case last::rest =>
        val allButLast = rest.map((e: Expr) => checkExpr(e,env,senv,Some(Types.VOID)))
        val lastExpr = checkExpr(last,env,senv)
        val newExprs = (lastExpr::allButLast).reverse
        Block(ExprInfo(span,parenthesized,inferredType(lastExpr)),loc,false,withinDo,newExprs)  
    }

    case _ => node
  }

  def checkExpr(expr: Expr,env: TypeEnv,senv:TypeAnalyzer):Expr = checkExpr(expr,env,senv,None)

  def checkExpr(expr: Expr,env: TypeEnv,senv:TypeAnalyzer,expected:Option[Type]):Expr = expr match {
    
    /* Temporary code for Tight Juxtapositions
     */
    case Juxt(info, multi, infix, exprs, false, true) => {
      val checkedExprs = exprs.map((e:Expr)=>checkExpr(e,env,senv))
      if(haveInferredTypes(checkedExprs)){
        //check if there are any functions
        if(checkedExprs.exists((e:Expr)=>isArrows(e))){ 
          //ToDo: some static checks
          //Left associate
          val leftAssociated = checkedExprs.tail.foldLeft(checkedExprs.head){(e1: Expr, e2: Expr) => ExprFactory.makeOpExpr(infix,e1,e2)}
          checkExpr(leftAssociated,env,senv,expected)
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
          checkExpr(Juxt(info,multi,infix,newExprs,false,true),env,senv,expected)
        }
      }
      else{
        Juxt(info,multi,infix,checkedExprs,false,true)
      }  
    }
    
    
    /* Loose Juxts are handled using the algorithm in 16.8 of Fortress Spec 1.0
     */
    case Juxt(info, multi, infix, exprs, isApp, false) => {
      //Check subexpressions
      val checkedExprs = exprs.map((e:Expr)=>checkExpr(e,env,senv))
      if(haveInferredTypes(checkedExprs)){
        //Breaks the list of expressions into chunks
        def chunker(exprs: List[Expr], results: List[(List[Expr],List[Expr])]): List[(List[Expr],List[Expr])] = exprs match {
        case Nil => results.reverse
        case first::rest =>
          if(isArrows(first)){
            val arrows = first::rest.takeWhile((e:Expr)=> isArrows(e))
            val dropArrows = rest.dropWhile((e:Expr)=> isArrows(e))
            val nonArrows = dropArrows.takeWhile((e:Expr)=> !isArrows(e))
            val dropNonArrows = dropArrows.dropWhile((e:Expr)=> !isArrows(e))
            chunker(dropNonArrows,(arrows,nonArrows)::results)
          }
          else{
            val nonArrows = first::rest.takeWhile((e:Expr)=> !isArrows(e))
            val dropNonArrows = rest.dropWhile((e:Expr)=> !isArrows(e))
            chunker(dropNonArrows, (List(),dropNonArrows)::results)
          }
        }
        val chunks = chunker(checkedExprs,List())
        //Left associate nonarrows as as a single OpExprs
        def associateNonArrows(nonArrows :List[Expr]):Option[Expr] = nonArrows match {
          case Nil => None
          case head::tail =>
            Some(tail.foldLeft(head){(e1: Expr, e2: Expr) => ExprFactory.makeOpExpr(infix,e1,e2)})
        }
        //Right associate everthing in a chunk as a _RewriteFnApp
        def associateArrows(fs :List[Expr], oe : Option[Expr]) = oe match {
          case None =>
            fs match {
              case Nil => bug("Empty Chunk")
              case _ =>
                val last = fs.last
                val allButLast = fs.take(fs.size-1)
                allButLast.foldRight(last){(f: Expr, e: Expr) => ExprFactory.make_RewriteFnApp(f,e)}
            }
          case Some(e) =>
            fs.foldRight(e){(f: Expr, e: Expr) => ExprFactory.make_RewriteFnApp(f,e)}
        }
        //Associate a chunk
        def associateChunk(chunk: (List[Expr],List[Expr])): Expr = {
          val (arrows,nonArrows) = chunk
          val associatedNonArrows = associateNonArrows(nonArrows)
          associateArrows(arrows,associatedNonArrows)
        }
        val associatedChunks = chunks.map(associateChunk)
        //TODO: String Check
        //TODO: See if you can make a MultiJuxt
        //If not left associate as InfixJuxts
        associatedChunks match{
          case Nil => bug("Empty Juxt")
          case head::tail =>
            checkExpr(tail.foldLeft(head){(e1: Expr, e2: Expr) => ExprFactory.makeOpExpr(infix,e1,e2)},env,senv,expected)
        }  
      }
      else{
        Juxt(info,multi,infix,checkedExprs,isApp,false)  
      }
    }
    
    /* Tight Juxts will be rewritten as MathPrimaries
    case Juxt(info, multi, infix, front::rest, false, true) => {
      def converter(e:Expr): MathItem = {
        if (NodeUtil.isParenthesized(e) || (e.isInstanceOf[TupleExpr]) || (e.isInstanceOf[VoidLiteralExpr]))
          ParenthesisDelimitedMI(SpanInfo(NodeUtil.getSpan(e)),e)
        else
          NonParenthesisDelimitedMI(SpanInfo(NodeUtil.getSpan(e)),e)
      }
      checkExpr(MathPrimary(info,multi,infix,checkExpr(front,env,senv),rest.map(converter)),env,senv,expected)
    }
     */
    
    //TODO: Handle math expressions
    case MathPrimary(info,multi,infix,front,rest) => expr
    
    case _ => expr
  }
  
  def checkMathItem(item: MathItem,env: TypeEnv,senv:TypeAnalyzer): MathItem = checkMathItem(item,env,senv,None)

  def checkMathItem(item: MathItem, env: TypeEnv,senv:TypeAnalyzer,expected:Option[Type]): MathItem = item match{
    case _ => item
  } 

}