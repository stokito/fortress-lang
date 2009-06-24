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
import com.sun.fortress.compiler.index.ApiIndex
import com.sun.fortress.compiler.index.CompilationUnitIndex
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
import com.sun.fortress.scala_src.useful.Errors._
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.STypesUtil
import com.sun.fortress.scala_src.useful.Sets._

/* Check type hierarchy to ensure the following:
 *  - acyclicity
 *  - comprises clauses
 *   = for each trait T with a comprises clause "comprises { S... }"
 *     every S_i \in { S... } should include T in its extends clause.
 *   = no other S' \not\in { S... } should include T in its extends clause.
 */
class TypeHierarchyChecker(compilation_unit: CompilationUnitIndex,
                           globalEnv: GlobalEnvironment,
                           repository: FortressRepository,
                           exclusionOracle: ExclusionOracle) {
  val isApi = compilation_unit.isInstanceOf[ApiIndex]

  def checkHierarchy(): JavaList[StaticError] = {
    val errors = new ArrayList[StaticError]()
    for (typ <- toSet(compilation_unit.typeConses.keySet)) {
      errors.addAll(checkDeclAcyclicity(typ, List()))
    }
    toJavaList(removeDuplicates(toList(errors)))
  }

  def checkAcyclicHierarchy(): JavaList[StaticError] = {
    val errors = new ArrayList[StaticError]()
    for (typ <- toSet(compilation_unit.typeConses.keySet)) {
      errors.addAll(checkAcyclicity(typ, List()))
      errors.addAll(checkDeclComprises(typ))
    }
    toJavaList(removeDuplicates(toList(errors)))
  }

  private def getTypes(typ:Id, errors:JavaList[StaticError]) = {
    val types = STypesUtil.getTypes(typ, globalEnv, compilation_unit)
    if (types == null) {
      error(errors, "Unknown type: " + typ, typ)
    }
    types
  }

  private def error(errors:JavaList[StaticError], s:String, n:Node) =
    errors.add(TypeError.make(s,n))

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
          for (extension <- toList(ti.extendsTypes)) {
            extension match {
              // TODO: Extend to handle non-empty where clauses.
              case STraitTypeWhere(_,SAnyType(_),_) => {}
              case STraitTypeWhere(_,STraitType(_,name,_,_),_) =>
                  errors.addAll(checkDeclAcyclicity(name,decl::children))
              case _ => error(errors, "Invalid type in extends clause: " +
                              extension.getBaseType, extension)
            }
          }
      }
    }
    errors
  }

  /* Check the given declaration to ensure acyclicity including coercions */
  def checkAcyclicity(decl:Id, children:List[Id]): JavaList[StaticError] = {
    val errors = new ArrayList[StaticError]()
    getTypes(decl, errors) match {
      case ti:TraitIndex =>
        var kids = children
        for ( c <- toSet(ti.coercions) ) {
          // The parser checks that:
          // 1) a coercion declaration should have exactly one parameter,
          // 2) it should not have an explicitly declared return type, and
          // 3) it should explicitly declare its parameter type.
          c.parameters.get(0).getIdType.unwrap match {
            case STraitType(_,name,_,_) => kids = name::kids
            case _ =>
          }
        }
        if (kids contains decl) {
          error(errors, "Cyclic type hierarchy: Type " + decl +
                " transitively extends/coerces to itself.", ti.ast)
        } else {
          for (extension <- toList(ti.extendsTypes)) {
            extension match {
              // TODO: Extend to handle non-empty where clauses.
              case STraitTypeWhere(_,SAnyType(_),_) => {}
              case STraitTypeWhere(_,STraitType(_,name,_,_),_) =>
                  errors.addAll(checkAcyclicity(name,decl::kids))
              case _ => error(errors, "Invalid type in extends clause: " +
                              extension.getBaseType, extension)
            }
          }
        }
    }
    errors
  }

  /* Check the given declaration to check its comprises clause
   *   - for each trait/object T
   *       for each trait S in T's extends clause
   *         = There should be no exclusive types in T's extends clause.
   *         = S should not exclude T
   *         = either S does not have any comprises clause
   *           or T should be in S's comprises clause;
   *           if S has comprises ... and T is not in S's comprises clause,
   *           T should not be exposed at all!
   *   - for each trait T
   *       for each trait/object S in T's comprises clause
   *         T should be in S's extends clause
   */
  def checkDeclComprises(decl:Id): JavaList[StaticError] = {
    val errors = new ArrayList[StaticError]()
    getTypes(decl, errors) match {
      case ti:TraitIndex =>
        val tt = ti.typeOfSelf.unwrap
        val extended = toList(ti.extendsTypes)
        for (extension <- extended) {
          extension match {
            // TODO: Extend to handle non-empty where clauses.
            case STraitTypeWhere(_,SAnyType(_),_) => {}
            case STraitTypeWhere(_,st@STraitType(_,name,_,_),_) =>
              for (second <- extended) {
                second match {
                  case STraitTypeWhere(_,other@STraitType(_,_,_,_),_) =>
                    if ( exclusionOracle.excludes(st, other) )
                      error(errors, "Types " + st + " and " + other +
                            " exclude each other.  " + decl +
                            " must not extend them.", st)
                  case _ =>
                }
              }
              getTypes(name, errors) match {
                case si:ProperTraitIndex =>
                  if ( exclusionOracle.excludes(tt, st) ) {
                      error(errors, "Type " + decl + " excludes " + name +
                            " but it extends " + name + ".", extension)
                  }
                  val comprises = si.comprisesTypes
                  if ( ! comprises.isEmpty && // extension has a comprises clause
                       // decl is not in the comprises clause
                       ! comprisesContains(comprises, decl) &&
                       ! NodeUtil.isComprisesEllipses(si.ast) )
                      error(errors, "Invalid comprises clause: " + name +
                            " has a comprises clause\n    but its immediate subtype " + decl +
                            " is not included in the comprises clause.", ti.ast)
                  if ( isApi && NodeUtil.isComprisesEllipses(si.ast) &&
                       ! comprisesContains(comprises, decl) )
                      error(errors, "Invalid comprises clause: " + name +
                            " has a comprises ...\n    but its immediate subtype " + decl +
                            " is exposed in the API.", ti.ast)
                case _ => error(errors, "Invalid type in extends clause: " +
                                extension.getBaseType, extension)
              }
            case _ => error(errors, "Invalid type in extends clause: " +
                            extension.getBaseType, extension)
          }
        }
        ti match {
          case si:ProperTraitIndex =>
            for (ty <- toSet(si.comprisesTypes)) {
              ty match {
                case STraitType(_,name,_,_) =>
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
    for (ty <- toSet(comprises)) {
      ty match {
        case STraitType(_,name,_,_) =>
          if ( name.getText.equals(decl.getText) ) result = true
      }
    }
    result
  }

  private def extendsContains(extendsC: JavaList[TraitTypeWhere], decl:Id): Boolean = {
    var result = false
    for (ty <- toList(extendsC)) {
      ty match {
        case STraitTypeWhere(_,SAnyType(_),_) =>
          if ( decl.getText.equals("Any") ) result = true
        case STraitTypeWhere(_,STraitType(_,name,_,_),_) =>
          if ( name.getText.equals(decl.getText) ) result = true
      }
    }
    result
  }
}
