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
import com.sun.fortress.compiler.typechecker.SubtypeChecker
import com.sun.fortress.compiler.typechecker.TraitTable
import com.sun.fortress.compiler.typechecker.TypeEnv
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options
import com.sun.fortress.scala_src.useful.Options._
import edu.rice.cs.plt.tuple.Option
import scala.collection.mutable.LinkedList

class TypeChecker(current: CompilationUnitIndex, traits: TraitTable) {

  var errors = List[StaticError]()

  private def signal(msg:String,node:Node) = { errors = errors ::: List(TypeError.make(msg,node)) }

  private def checkSubtype(subtype:Type,supertype:Type,senv:SubtypeChecker,node:Node,error:String) = {
    val judgement = senv.subtype(subtype,supertype).booleanValue
    if (! judgement) signal(error,node)
    judgement
  }

  def check(node:Node,env:TypeEnv,senv:SubtypeChecker):Node = node match {
    case Component(info,name,imports,decls,is_native,exports)  => 
      Component(info,name,imports,map(decls,(n:Decl)=>check(n,env,senv).asInstanceOf),is_native,exports)

    case f@FnDecl(info,header,unambiguousName,body,implementsUnambiguousName) if body.isNone => f

    case f@FnDecl(info,FnHeader(statics,mods,name,wheres,throws,contract,params,returnType),unambiguousName,body,implementsUnambiguousName) => {
      val newEnv = env.extendWithStaticParams(statics).extendWithParams(params)
      val newSenv = senv.extend(statics,wheres)

      val newContract = toOption(contract) match {
        case Some(c) => Options.some(check(c,newEnv,newSenv))
        case None => Options.none()
      }
      val newBody = some(checkExpr(body.unwrap,newEnv,newSenv,returnType))

      val newReturnType = toOption(newBody.unwrap.getInfo.getExprType) match {
        case Some(typ) => toOption(returnType) match {
          case None => some(typ)
          case _ => returnType
        }
        case _ => returnType
      }
      NodeFactory.makeFnDecl(NodeUtil.getSpan(f),NodeUtil.getMods(f),NodeUtil.getName(f),statics,params,
                             newReturnType,NodeUtil.getThrowsClause(f),wheres,newContract.asInstanceOf,newBody)
    }

    
      
    case _ => node
  }

  def checkExpr(expr: Expr,env: TypeEnv,senv:SubtypeChecker,expected:Option[Type]):Expr = expr match {
    case _ => expr
  }



}
