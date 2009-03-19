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

import _root_.java.util.ArrayList
import _root_.java.util.{List => JavaList}

import com.sun.fortress.compiler.GlobalEnvironment
import com.sun.fortress.compiler.index.ComponentIndex
import com.sun.fortress.compiler.index.TypeConsIndex
import com.sun.fortress.compiler.index.TraitIndex
import com.sun.fortress.compiler.Types
import com.sun.fortress.repository.FortressRepository
import com.sun.fortress.exceptions.StaticError
import com.sun.fortress.exceptions.TypeError
import com.sun.fortress.nodes.APIName
import com.sun.fortress.nodes.BaseType
import com.sun.fortress.nodes.Id
import com.sun.fortress.nodes.NamedType
import com.sun.fortress.nodes.Node
import com.sun.fortress.nodes.TraitTypeWhere
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.useful.ASTGenHelper.javaify
import com.sun.fortress.scala_src.useful.ASTGenHelper.scalaify

class TypeHierarchyChecker(component: ComponentIndex,
                           globalEnv: GlobalEnvironment,
                           repository: FortressRepository) {
  def checkHierarchy(): JavaList[StaticError] = {
    val errors = new ArrayList[StaticError]()

    for (typ <- scalaify(component.typeConses.keySet).asInstanceOf[Set[Id]]) {
      errors.addAll(checkDecl(typ, List()))
    }
    javaify(removeDuplicates(scalaify(errors).asInstanceOf[List[StaticError]])).asInstanceOf[JavaList[StaticError]]
  }

  private def removeDuplicates(errors:List[StaticError]):List[StaticError] = { 
    errors match {
      case Nil => errors
      case fst::rst => 
        if (rst contains fst) { removeDuplicates(rst) }
        else { fst :: removeDuplicates(rst) }
    }
  }

  def checkDecl(decl:Id,children:List[Id]): JavaList[StaticError] = {
    val errors = new ArrayList[StaticError]()
    def error(s:String,n:Node) = errors.add(TypeError.make(s,n))

    val types = decl match {
      case Id(info,Some(name),text) => {
        globalEnv.api(name).typeConses.get(Id(info,None,text))
      }
      case _ => component.typeConses.get(decl) 
    }

    if (types == null) { 
      error("Unknown type: " + decl, decl) 
    }
    else if (children contains decl) {
      error("Cyclic type hierarchy: Type " + decl + " transitively extends itself.", types.ast)
    }
    else {
      types match {
        case ti:TraitIndex => 
          for (extension <- scalaify(ti.extendsTypes).asInstanceOf[List[TraitTypeWhere]]) {
            extension match {
              // TODO: Extend to handle non-empty where clauses.
              case TraitTypeWhere(_,AnyType(_),_) => {}
              case TraitTypeWhere(_,TraitType(_,name,_,_),_) => errors.addAll(checkDecl(name,decl::children))
              case _ => error("Invalid type in extends clause: " + extension.getBaseType, extension)
            }
          }
      }
    }
    errors
  }
}
                              
                              
