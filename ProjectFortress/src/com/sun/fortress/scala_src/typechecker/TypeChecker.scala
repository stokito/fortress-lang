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
import com.sun.fortress.nodes_util.NodeUtil
import com.sun.fortress.exceptions.StaticError
import com.sun.fortress.exceptions.TypeError
import com.sun.fortress.compiler.index.CompilationUnitIndex
import com.sun.fortress.compiler.typechecker.TypeAnalyzer
import com.sun.fortress.compiler.typechecker.TraitTable
import com.sun.fortress.compiler.typechecker.TypeEnv
import com.sun.fortress.compiler.Types
import scala.collection.mutable.LinkedList
import com.sun.fortress.scala_src.useful.ASTGenHelper._
import com.sun.fortress.nodes_util.ExprFactory

class TypeChecker(current: CompilationUnitIndex, traits: TraitTable) {

  var errors = List[StaticError]()

  private def signal(msg:String,node:Node) = { errors = errors ::: List(TypeError.make(msg,node)) }

  private def inferredType(expr:Expr): Option[Type] = scalaify(expr.getInfo.getExprType).asInstanceOf

  private def checkSubtype(subtype:Type,supertype:Type,senv:TypeAnalyzer,node:Node,error:String) = {
    val judgement = senv.subtype(subtype,supertype).isTrue
    if (! judgement) signal(error,node)
    judgement
  }

  def check(node:Node,env:TypeEnv,senv:TypeAnalyzer):Node = node match {
    case Component(info,name,imports,decls,isNative,exports)  => 
      Component(info,name,imports,decls.map((n:Decl)=>check(n,env,senv).asInstanceOf),isNative,exports)

    case f@FnDecl(info,header,unambiguousName,None,implementsUnambiguousName) => f

    case f@FnDecl(info,FnHeader(statics,mods,name,wheres,throws,contract,params,returnType),unambiguousName,Some(body),implementsUnambiguousName) => {
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
      FnDecl(info, FnHeader(statics,mods,name,wheres,throws,newContract.asInstanceOf,params,newReturnType), unambiguousName, Some(newBody), implementsUnambiguousName)
    }
    
    /* Matches if block is not an atomic block. */
    case Block(ExprInfo(span,parenthesized,resultType),loc,false,withinDo,exprs) => {
      var newExprs = List[Expr]()
      val newResultType:Option[Type] = 
        if (newExprs.isEmpty)
          Some(Types.VOID)
        else {
          for (e <- exprs.take(exprs.size-1)){
            newExprs=checkExpr(e,env,senv,Some(Types.VOID))::newExprs
          }
          val lastExpr = checkExpr(exprs.last,env,senv)
          newExprs=(lastExpr::newExprs).reverse
          inferredType(lastExpr)
        }
      Block(ExprInfo(span,parenthesized,newResultType),loc,false,withinDo,newExprs)
    }
      

    // tight juxt, known function application
    case j@Juxt(ExprInfo(span,parenthesized,typ), multi, infix, front::arg::Nil, true, true) => {
      val freshArrow = ArrowType(TypeInfo(span,false,List(),None),
                                 _InferenceVarType(TypeInfo(span,false,List(),None), new _root_.java.lang.Object()),
                                 _InferenceVarType(TypeInfo(span,false,List(),None),new _root_.java.lang.Object()),
                                 Effect(SpanInfo(span),None,false))
      _RewriteFnApp(ExprInfo(span,parenthesized,Some(freshArrow)),checkExpr(front,env,senv),checkExpr(arg,env,senv))
    }

    // tight juxt, known not a function application
    case Juxt(info, multi, infix, front::arg::Nil, false, true) => {
      def converter(e:Expr) = {
        if (NodeUtil.isParenthesized(e) || (e.isInstanceOf[TupleExpr]) || (e.isInstanceOf[VoidLiteralExpr]))
          ParenthesisDelimitedMI(SpanInfo(NodeUtil.getSpan(e)),e)
        else
          ParenthesisDelimitedMI(SpanInfo(NodeUtil.getSpan(e)),e)
      }
      MathPrimary(info,multi,infix,checkExpr(front,env,senv),List(converter(arg)))
    }
    case Juxt(info, multi, infix, exprs, isApp, false) => {
      Juxt(info,multi,infix,exprs.map((e:Expr)=>checkExpr(e,env,senv)),isApp,false)
    }

    case _ => node
  }

  def checkExpr(expr: Expr,env: TypeEnv,senv:TypeAnalyzer):Expr = checkExpr(expr,env,senv,None)

  def checkExpr(expr: Expr,env: TypeEnv,senv:TypeAnalyzer,expected:Option[Type]):Expr = expr match {
    case _ => expr
  }
  

}
