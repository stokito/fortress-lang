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
import scala.collection.Map
import scala.collection.Set
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import com.sun.fortress.compiler.GlobalEnvironment
import com.sun.fortress.compiler.disambiguator.ExprDisambiguator.HierarchyHistory
import com.sun.fortress.compiler.index.ComponentIndex
import com.sun.fortress.compiler.index.TraitIndex
import com.sun.fortress.compiler.typechecker.StaticTypeReplacer
import com.sun.fortress.compiler.typechecker.TypeAnalyzer
import com.sun.fortress.exceptions.StaticError
import com.sun.fortress.exceptions.TypeError
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.{NodeUtil => NU}
import com.sun.fortress.nodes_util.Span
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.Sets._
import com.sun.fortress.scala_src.useful.STypesUtil._

/* All inherited abstract methods in object declarations and
 * object expressions should be defined,
 * with compatible signatures and modifiers.
 */
class AbstractMethodChecker(component: ComponentIndex,
                            globalEnv: GlobalEnvironment)
    extends Walker {
  val traits = new TraitTable(component, globalEnv)
  var typeAnalyzer = TypeAnalyzer.make(traits)
  var errors = List[StaticError]()
  val componentName = component.ast.getName
  private def error(loc: Span, msg: String) =
    errors = errors ::: List(TypeError.make(msg, loc))
  def check() = { walk(component.ast); errors }

  override def walk(node:Any):Any = {
    node match {
      case o@SObjectDecl(SSpanInfo(span),
                         STraitTypeHeader(sparams, _, name, _, _, _, extendsC, decls),
                         _, _) =>
        checkObject(span, sparams, name, extendsC,
                    walk(decls).asInstanceOf[List[Decl]])

      case o@SObjectExpr(SExprInfo(span, _, _),
                         STraitTypeHeader(_, _, name, _, _, _, extendsC, decls),
                         _) =>
        checkObject(span, List(), name, extendsC,
                    walk(decls).asInstanceOf[List[Decl]])
      /*
        val inherited = toSet(inheritedMethods(extendsC).firstSet).map(_.getText)
      */
        val inherited = inheritedMethods(extendsC)
        for ( d <- decls ; if d.isInstanceOf[FnDecl] ) {
          if ( NU.isFunctionalMethod(NU.getParams(d.asInstanceOf[FnDecl])) &&
               ! inherited.exists(NU.getName(d.asInstanceOf[FnDecl]).asInstanceOf[IdOrOp].getText.equals(_)) )
            error(span, "Object expressions should not define any new " +
                  "functional methods.")
        }

      case _ => super.walk(node)
    }
    node
  }

  private def checkObject(span: Span, sparams: List[StaticParam],
                          name: IdOrOpOrAnonymousName,
                          extendsC: List[TraitTypeWhere],
                          decls: List[Decl]) = {
    // Extend the type analyzer with the collected static parameters
    val oldTypeAnalyzer = typeAnalyzer
    val staticParameters = new ArrayList[StaticParam]()
    // Add static parameters of the enclosing trait or object
    staticParameters.addAll( toJavaList(sparams) )
    typeAnalyzer = typeAnalyzer.extend(staticParameters, none[WhereClause])
    val toCheck = inheritedAbstractMethods(extendsC)
    for ( t <- toCheck.keySet ) {
      for ( (owner, ds) <- toCheck.get(t) ) {
        for ( d <- ds ) {
          if ( ! implement(d, decls, owner) )
            error(span,
                  "The inherited abstract method " + d + " from the trait " + t +
                  "\n    in the object " + name +
                  " is not defined in the component " + componentName + ".")
         }
       }
    }
    typeAnalyzer = oldTypeAnalyzer
  }

  def inheritedMethods(extendedTraits: List[TraitTypeWhere]) = {
    // Return all of the methods from super-traits
    def inheritedMethodsHelper(history: HierarchyHistory,
                               extended_traits: List[TraitTypeWhere])
                               : Set[String] = {
      var methods = new HashSet[String]()
      var done = false
      var h = history
      for ( trait_ <- extended_traits ; if (! done) ) {
        val type_ = trait_.getBaseType
        if ( ! h.hasExplored(type_) ) {
          h = h.explore(type_)
          type_ match {
            case ty@STraitType(_, name, _, params) =>
              toOption(traits.typeCons(name)) match {
                case Some(ti) =>
                  if ( ti.isInstanceOf[TraitIndex] ) {
                    val trait_params = ti.staticParameters
                    val trait_args = ty.getArgs
                    // Instantiate methods with static args
                    val dotted = toSet(ti.asInstanceOf[TraitIndex].dottedMethods).map(t => t.first)
                    for ( pair <- dotted ; if pair.isInstanceOf[IdOrOp] ) {
                      methods += pair.asInstanceOf[IdOrOp].getText
                    }
                    val paramsToArgs = new StaticTypeReplacer(trait_params, trait_args)
                    val instantiated_extends_types =
                      toList(ti.asInstanceOf[TraitIndex].extendsTypes).map( (t:TraitTypeWhere) =>
                            t.accept(paramsToArgs).asInstanceOf[TraitTypeWhere] )
                    methods ++= inheritedMethodsHelper(h, instantiated_extends_types)
                  } else done = true
                case _ => done = true
              }
            case _ => done = true
          }
        }
      }
      methods
    }
    inheritedMethodsHelper(new HierarchyHistory(), extendedTraits)
  }

  private def inheritedAbstractMethods(extended_traits: List[TraitTypeWhere]) =
    inheritedAbstractMethodsHelper(new HierarchyHistory(), extended_traits)

  private def inheritedAbstractMethodsHelper(hist: HierarchyHistory,
                                             extended_traits: List[TraitTypeWhere]):
                                            Map[IdOrOp, (TraitType, Set[FnDecl])] = {
    var h = hist
    var map = new HashMap[IdOrOp, (TraitType, Set[FnDecl])]()
    for ( trait_ <- extended_traits ; if ! h.hasExplored(trait_.getBaseType) ) {
      trait_.getBaseType match {
        case ty@STraitType(info, name, args, params) =>
          h = h.explore(ty)
          val tci = typeAnalyzer.traitTable.typeCons(name)
          if ( tci.isSome && tci.unwrap.isInstanceOf[TraitIndex] ) {
            val ti = tci.unwrap.asInstanceOf[TraitIndex]
            map.put(name, (ty, collectAbstractMethods(name, toList(NU.getDecls(ti.ast)))))
            map ++= inheritedAbstractMethodsHelper(h, toList(ti.extendsTypes))
          } else error(NU.getSpan(trait_),
                       "Trait types are expected in an extends clause but found "
                       + ty.toStringVerbose + "\n" + tci.getClass)
        case SAnyType(_) =>
        case ty => error(NU.getSpan(trait_),
                         "Trait types are expected in an extends clause but found "
                         + ty.toStringVerbose)
      }
    }
    map
  }

  private def collectAbstractMethods(name: IdOrOp, decls: List[Decl]) = {
    val set = new HashSet[FnDecl]
    decls.foreach( (d: Decl) => d match {
                   case fd@SFnDecl(_,SFnHeader(_,mods,_,_,_,_,_,_),_,body,_) =>
                     if ( component.typeConses.keySet.contains(name) ) {
                       if ( ! body.isDefined ) set += fd
                     } else if ( mods.isAbstract ) set += fd
                   case _ => })
      set
  }

  /* Returns true if any of the concrete method declarations in "decls"
   * implements the abstract method declaration "d".
   */
  private def implement(d: FnDecl, decls: List[Decl], ast: TraitType): Boolean =
    decls.exists( (decl: Decl) => decl match {
                  case fd@SFnDecl(_,_,_,_,_) => implement(d, fd, ast)
                  case _ => false
                } )

  /* Returns true if the concrete method declaration "decl"
   * implements the abstract method declaration "d".
   */
  private def implement(d: FnDecl, decl: FnDecl, t: TraitType): Boolean = {
    val tci = typeAnalyzer.traitTable.typeCons(t.getName)
    var sparams = List[StaticParam]()
    if ( tci.isSome && tci.unwrap.isInstanceOf[TraitIndex] )
      sparams = toList(NU.getStaticParams(tci.unwrap.asInstanceOf[TraitIndex].ast.asInstanceOf[TraitObjectDecl])).asInstanceOf[List[StaticParam]]
    val sargs = toList(t.getArgs)

    // Extend the type analyzer with the collected static parameters
    def subst(ty: Type)(implicit analyzer: TypeAnalyzer) =
      staticInstantiation(sargs, sparams, ty, true) match {
        case Some(t) => t
        case _ => ty
      }
    val result =
      NU.getName(d).asInstanceOf[IdOrOp].getText.equals(NU.getName(decl).asInstanceOf[IdOrOp].getText) &&
      NU.getMods(d).containsAll(NU.getMods(decl)) &&
      ( typeAnalyzer.equivalent(subst(NU.getParamType(d).asInstanceOf[Type])(typeAnalyzer),
                                subst(NU.getParamType(decl))(typeAnalyzer)).isTrue ||
        implement(toList(NU.getParams(d)), toList(NU.getParams(decl))) ) &&
      typeAnalyzer.equivalent(subst(NU.getReturnType(d).unwrap)(typeAnalyzer),
                              subst(NU.getReturnType(decl).unwrap)(typeAnalyzer)).isTrue
    result
  }

  /* Returns true if the parameters of the concrete method declaration "params"
   * `implements' the parameters of the abstract method declaration "ps".
   */
  private def implement(ps: List[Param], params: List[Param]): Boolean =
    ! ps.zip(params).exists( (p:(Param,Param)) =>
                             typeAnalyzer.equivalent(NU.getParamType(p._1),
                                                     NU.getParamType(p._2)).isFalse &&
                             ! p._1.getName.getText.equals("self") )
}
