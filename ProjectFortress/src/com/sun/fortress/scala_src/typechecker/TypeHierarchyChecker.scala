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
import com.sun.fortress.exceptions.StaticError
import com.sun.fortress.exceptions.TypeError
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.NodeUtil
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.types.TypeAnalyzer
import com.sun.fortress.scala_src.useful.Errors._
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.SNodeUtil
import com.sun.fortress.scala_src.useful.STypesUtil
import com.sun.fortress.scala_src.useful.Sets._

/* Check type hierarchy to ensure the following:
 *  - acyclicity
 *  - comprises clauses
 */
class TypeHierarchyChecker(compilation_unit: CompilationUnitIndex,
                           globalEnv: GlobalEnvironment,
                           isApi: Boolean) {
  def checkHierarchy(): JavaList[StaticError] = {
    val errors = new ArrayList[StaticError]()
    for (typ <- toSet(compilation_unit.typeConses.keySet)) {
      checkDeclAcyclicity(typ, List(), errors)
    }
    toJavaList(removeDuplicates(toList(errors)))
  }

  def checkAcyclicHierarchy(analyzer: TypeAnalyzer): JavaList[StaticError] = {
    val errors = new ArrayList[StaticError]()
    for (typ <- toSet(compilation_unit.typeConses.keySet)) {
      checkAcyclicity(typ, List(), errors)
      checkDeclComprises(typ, errors, analyzer)
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
  def checkDeclAcyclicity(decl:Id, children:List[Id],
                          errors:JavaList[StaticError]): Unit = {
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
                checkDeclAcyclicity(name, decl::children, errors)
              case _ => error(errors, "Invalid type in extends clause: " +
                              extension.getBaseType, extension)
            }
          }
        case _ =>
      }
    }
  }

  /* Check the given declaration to ensure acyclicity including coercions */
  def checkAcyclicity(decl:Id, children:List[Id],
                      errors:JavaList[StaticError]): Unit = {
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
                checkAcyclicity(name, decl::kids, errors)
              case _ => error(errors, "Invalid type in extends clause: " +
                              extension.getBaseType, extension)
            }
          }
        }
      case _ =>
    }
  }

  /* Check the given declaration to check its comprises clause
   *   - for each trait/object T
   *       for each trait S in T's extends clause
   *         = There should be no exclusive types in T's extends clause.
   *         = S should not exclude T
   *         = T should be eligible to extend S.
   *          (either 1) S does not have any comprises clause, or
   *           2) S's comprises clause contains T's supertype, or
   *           3) T has a comprises clause and every type in the comprises
   *              clause is eligible to extend S)
   *           if S has comprises ... and T is not in S's comprises clause,
   *           T should not be exposed at all!)
   *   - for each trait T
   *       for each naked type variable V in T's comprises clause
   *         V should be one of the static parameters of T
   */
  def checkDeclComprises(decl: Id,
                         errors: JavaList[StaticError],
                         analyzer: TypeAnalyzer): Unit = {
    if (decl.getText.equals(Types.ANY_NAME.getText)) return
    getTypes(decl, errors) match {
      case ti:TraitIndex =>
        // println("checkDeclComprises" + ti.ast)
        val self_type = ti.typeOfSelf.unwrap
        val tt = SNodeUtil.getTraitType(self_type) match {
          case Some(traitTy) => traitTy
          case _ =>
            error(errors, "Invalid trait type: " + self_type, decl)
            Types.OBJECT // error recovery
        }
        val new_analyzer = analyzer.extend(toList(ti.staticParameters), None)
        val extended = toList(ti.extendsTypes)
        for (extension <- extended) {
          extension match {
            // TODO: Extend to handle non-empty where clauses.
            case STraitTypeWhere(_,SAnyType(_),_) => {}
            case STraitTypeWhere(_,st@STraitType(_,name,_,_),_) =>
              for (second <- extended) {
                second match {
                  case STraitTypeWhere(_,other@STraitType(_,_,_,_),_) =>
                    if ( new_analyzer.excludes(st, other) )
                      error(errors, "Types " + st + " and " + other +
                            " exclude each other.  " + decl +
                            " must not extend them.", st)
                  case _ =>
                }
              }
              getTypes(name, errors) match {
                case si:ProperTraitIndex =>
                  if ( new_analyzer.excludes(tt, st) ) {
                      error(errors, "Type " + tt + " excludes " + name +
                            " but it extends " + name + ".", extension)
                  }
                  val comprises = toSet(si.comprisesTypes)
                  SNodeUtil.validComprises(comprises, si.staticParameters) match {
                    case Some(tv) =>
                      error(errors, tv + " is a naked type variable in the " +
                            "comprises clause of a trait " + name + "\n    but " +
                            "it is not a static parameter of " + name + ".", extension)
                    case _ =>
                  }
                  val subst_comprises =
                    comprises.map(new_analyzer.substitute(toList(st.getArgs),
                                                          toList(si.staticParameters),
                                                          _).asInstanceOf[NamedType])
                  if (! comprises.isEmpty &&
                      ! isEligibleToExtend(tt, subst_comprises, new_analyzer, errors) &&
                      ! NodeUtil.isComprisesEllipses(si.ast) )
                    error(errors, "Invalid comprises clause: " + name +
                          " has a comprises clause\n    but its immediate subtype " + decl +
                          " is not eligible to extend it.", ti.ast)
                  if ( isApi && NodeUtil.isComprisesEllipses(si.ast) &&
                       ! comprisesContains(subst_comprises, tt, new_analyzer) )
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
                case tty@STraitType(_,name,_,_) =>
                  getTypes(name, errors) match {
                    case tti:TraitIndex =>
                      if ( ! extendsContains(tty, toList(tti.extendsTypes), decl,
                                             new_analyzer, errors) )
                        error(errors, "Invalid comprises clause: " + ty +
                              " is included in the comprises clause of " + decl +
                              "\n    but " + name + " does not extend " + decl + ".", tti.ast)
                    }
                case _ =>
              }
            }
          case _ =>
        }
       case _ =>
    }
  }

  /**
   *  Either:
   *  1) S does not have any comprises clause, or (already checked)
   *  2) S's comprises clause contains T's supertype, or
   *  3) T has a comprises clause and every type in the comprises
   *     clause is eligible to extend S
   */
  private def isEligibleToExtend(tt: TraitType, comprises: Set[NamedType],
                                 analyzer: TypeAnalyzer,
                                 errors:JavaList[StaticError]): Boolean = {
    comprisesContains(comprises, tt, analyzer) ||
    (getTypes(tt.getName, errors) match {
      case ti:ProperTraitIndex =>
        val t_comprises = ti.comprisesTypes
        ! t_comprises.isEmpty &&
        toSet(t_comprises).filter(_.isInstanceOf[TraitType]).forall(t => isEligibleToExtend(t.asInstanceOf[TraitType],
                                                                                            comprises, analyzer, errors))
      case _ => false
     })
  }

  private def comprisesContains(comprises: Set[NamedType],
                                decl: Type,
                                analyzer: TypeAnalyzer): Boolean = {
    for (ty <- comprises) {
      ty match {
        case _:TraitType => if (analyzer.subtype(decl, ty).isTrue) return true
        case _ =>
      }
    }
    false
  }

  /** Whether 'comprises' is a supertype of any type in 'extendsC'
   *  If any type in 'extendsC' is a subtype of 'comprises', then OK
   *  Otherwise, for each type T in 'extendsC',
   *  check extendsContains(comprises, substituted_extendsC_of_T, analyzer)
   */

  private def extendsContains(comprised: TraitType, extendsC: List[TraitTypeWhere],
                              decl:Id, analyzer: TypeAnalyzer,
                              errors:JavaList[StaticError]): Boolean = {
    for (ty <- extendsC) {
      ty match {
        case STraitTypeWhere(_,SAnyType(_),_) =>
          if ( decl.getText.equals("Any") ) return true
        case STraitTypeWhere(_,STraitType(_,name,sargs,_),_) =>
          if ( name.getText.equals(decl.getText) ) {
            val sparams = compilation_unit.typeConses.get(decl).staticParameters
            STypesUtil.staticInstantiation(toList(sparams) zip sargs, comprised)(analyzer) match {
              case Some(STraitType(_,n1,s1@hd::_,_)) =>
                val s2 = toList(compilation_unit.typeConses.get(n1).staticParameters)
                return (s1 zip s2).forall(pair =>
                                          (pair._1, STypesUtil.staticParamToArg(pair._2)) match {
                                            case (STypeArg(_,_,t1), STypeArg(_,_,t2)) =>
                                              t1 == t2
                                            case (SOpArg(_,_,t1), SOpArg(_,_,t2)) =>
                                              t1 == t2
                                            case _ => false
                                          })
              case _ => return true
            }
          }
      }
    }
    extendsC.map(ty => ty.getBaseType match {
        case STraitType(_, name, _, _) =>
                 getTypes(name, errors) match {
                   case ti:TraitIndex =>
                     extendsContains(comprised, toList(ti.extendsTypes),
                                     decl, analyzer, errors)}
        case _ => false}).foldLeft(false)((a,b) => a || b)
  }
}
