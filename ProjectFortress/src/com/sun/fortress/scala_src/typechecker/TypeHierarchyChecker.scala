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
import _root_.java.util.{Set  => JavaSet}

import com.sun.fortress.compiler.GlobalEnvironment
import com.sun.fortress.compiler.index.ComponentIndex
import com.sun.fortress.compiler.index.ProperTraitIndex
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
import com.sun.fortress.nodes.TraitType
import com.sun.fortress.nodes.TraitTypeWhere
import com.sun.fortress.nodes_util.NodeUtil
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.useful.ASTGenHelper.javaify
import com.sun.fortress.scala_src.useful.ASTGenHelper.scalaify

/* Check type hierarchy to ensure the followings:
 *  - acyclicity
 *  - comprises clauses
 *   = for each trait T with a comprises clause "comprises { S... }"
 *     every S_i \in { S... } should include T in its extends clause.
 *   = no other S' \not\in { S... } should include T in its extends clause.
 */
class TypeHierarchyChecker(component: ComponentIndex,
                           globalEnv: GlobalEnvironment,
                           repository: FortressRepository) {
  def checkHierarchy(): JavaList[StaticError] = {
    val errors = new ArrayList[StaticError]()

    for (typ <- scalaify(component.typeConses.keySet).asInstanceOf[Set[Id]]) {
      errors.addAll(checkDeclAcyclicity(typ, List()))
      errors.addAll(checkDeclComprises(typ))
    }
    javaify(removeDuplicates(scalaify(errors).asInstanceOf[List[StaticError]])).asInstanceOf[JavaList[StaticError]]
  }

  private def getTypes(typ:Id, errors:JavaList[StaticError]) = {
    val types = typ match {
      case Id(info,Some(name),text) =>
        globalEnv.api(name).typeConses.get(Id(info,None,text))
      case _ => component.typeConses.get(typ)
    }
    if (types == null) {
      error(errors, "Unknown type: " + typ, typ)
    }
    types
  }

  private def error(errors:JavaList[StaticError], s:String, n:Node) =
    errors.add(TypeError.make(s,n))

  private def removeDuplicates(errors:List[StaticError]):List[StaticError] = {
    errors match {
      case Nil => errors
      case fst::rst =>
        if (rst contains fst) { removeDuplicates(rst) }
        else { fst :: removeDuplicates(rst) }
    }
  }

  /* Check the given declaration to ensure acyclicity */
  def checkDeclAcyclicity(decl:Id, children:List[Id]): JavaList[StaticError] = {
    val errors = new ArrayList[StaticError]()
    val types = getTypes(decl, errors)

    if (children contains decl) {
      error(errors, "Cyclic type hierarchy: Type " + decl +
            " transitively extends itself.", types.ast)
    }
    else {
      types match {
        case ti:TraitIndex =>
          for (extension <- scalaify(ti.extendsTypes).asInstanceOf[List[TraitTypeWhere]]) {
            extension match {
              // TODO: Extend to handle non-empty where clauses.
              case TraitTypeWhere(_,AnyType(_),_) => {}
              case TraitTypeWhere(_,TraitType(_,name,_,_),_) =>
                  errors.addAll(checkDeclAcyclicity(name,decl::children))
              case _ => error(errors, "Invalid type in extends clause: " +
                              extension.getBaseType, extension)
            }
          }
      }
    }
    errors
  }

  /* Check the given declaration to check its comprises clause
   *   - for each trait T
   *       for each trait/object S in T's comprises clause
   *         T should be in S's extends clause
   *   - for each trait/object T
   *       for each trait S in T's extends clause
   *         either S does not have any comprises clause
   *         or T should be in S's comprises clause
   */
  def checkDeclComprises(decl:Id): JavaList[StaticError] = {
    val errors = new ArrayList[StaticError]()
    getTypes(decl, errors) match {
      case ti:TraitIndex =>
        for (extension <- scalaify(ti.extendsTypes).asInstanceOf[List[TraitTypeWhere]]) {
          extension match {
            // TODO: Extend to handle non-empty where clauses.
            case TraitTypeWhere(_,AnyType(_),_) => {}
            case TraitTypeWhere(_,TraitType(_,name,_,_),_) =>
              getTypes(name, errors) match {
                case si:ProperTraitIndex =>
                  val comprises = si.comprisesTypes
                  if ( ! NodeUtil.isComprisesEllipses(si.ast) &&
                       ! comprises.isEmpty && // extension has a comprises clause
                       // decl is not in the comprises clause
                       ! comprisesContains(comprises, decl) )
                      error(errors, "Invalid comprises clause: " + name +
                            " has a comprises clause\n    but its immediate subtype " + decl +
                            " is not included in the comprises clause.", ti.ast)
                case _ => error(errors, "Invalid type in extends clause: " +
                                extension.getBaseType, extension)
              }
            case _ => error(errors, "Invalid type in extends clause: " +
                            extension.getBaseType, extension)
          }
        }
        ti match {
          case si:ProperTraitIndex =>
            for (ty <- scalaify(si.comprisesTypes).asInstanceOf[Set[TraitType]]) {
              ty match {
                case TraitType(_,name,_,_) =>
                  getTypes(name, errors) match {
                    case tt:TraitIndex =>
                    if ( ! extendsContains(tt.extendsTypes, decl) )
                        error(errors, "Invalid comprises clause: " + name +
                              " is included in the comprises clause of " + decl +
                              "\n    but " + name + " does not extend " + decl + ".", tt.ast)
                  }
              }
            }
          case _ =>
        }
    }
    errors
  }

  private def comprisesContains(comprises: JavaSet[TraitType], decl:Id): Boolean = {
    var result = false
    for (ty <- scalaify(comprises).asInstanceOf[Set[TraitType]]) {
      ty match {
        case TraitType(_,name,_,_) =>
          if ( name.getText.equals(decl.getText) ) result = true
      }
    }
    result
  }

  private def extendsContains(extendsC: JavaList[TraitTypeWhere], decl:Id): Boolean = {
    var result = false
    for (ty <- scalaify(extendsC).asInstanceOf[List[TraitTypeWhere]]) {
      ty match {
        case TraitTypeWhere(_,AnyType(_),_) =>
          if ( decl.getText.equals("Any") ) result = true
        case TraitTypeWhere(_,TraitType(_,name,_,_),_) =>
          if ( name.getText.equals(decl.getText) ) result = true
      }
    }
    result
  }
}
