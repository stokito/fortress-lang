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
import com.sun.fortress.compiler.index.ComponentIndex
import com.sun.fortress.compiler.index.{DeclaredMethod => JavaDeclaredMethod}
import com.sun.fortress.compiler.index.FieldGetterOrSetterMethod
import com.sun.fortress.compiler.index.{Functional => JavaFunctional}
import com.sun.fortress.compiler.index.{FunctionalMethod => JavaFunctionalMethod}
import com.sun.fortress.compiler.index.HasSelfType
import com.sun.fortress.compiler.index.TraitIndex
import com.sun.fortress.compiler.typechecker.StaticTypeReplacer
import com.sun.fortress.exceptions.StaticError
import com.sun.fortress.exceptions.TypeError
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.{NodeUtil => NU}
import com.sun.fortress.nodes_util.Span
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.types.TypeAnalyzer
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
  implicit var typeAnalyzer = TypeAnalyzer.make(traits)
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
        val inherited = toSet(inheritedMethods(traits, extendsC, Set(), typeAnalyzer).firstSet)
                        .map(t => t.asInstanceOf[IdOrOp].getText)
        for {
          d <- decls;
          if d.isInstanceOf[FnDecl];
          if NU.isFunctionalMethod(NU.getParams(d.asInstanceOf[FnDecl]));
          if !inherited.exists(NU.getName(d.asInstanceOf[FnDecl]).asInstanceOf[IdOrOp]
                               .getText.equals(_))
        } error(NU.getSpan(d),
                "Object expressions should not define any new functional methods.")

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
    // Add static parameters of the enclosing trait or object
    typeAnalyzer = typeAnalyzer.extend(sparams, None)
    val toCheck = inheritedAbstractMethods(extendsC)
    for ( (owner, d) <- toCheck; if (!implement(d, decls, owner)) ) {
        error(span,
              "The inherited abstract method " + d + " from the trait " + owner +
              "\n    in the object " + name +
              " is not defined in the component " + componentName + ".")
    }
    typeAnalyzer = oldTypeAnalyzer
  }

  private def inheritedAbstractMethods(extended_traits: List[TraitTypeWhere]):
      List[(TraitType, FnDecl)] = {
    val inherited = inheritedMethods(typeAnalyzer.traits, extended_traits,
                                     Set(), typeAnalyzer)
    var res = List[(TraitType, FnDecl)]()
    for {
      (meth : HasSelfType, _, tt) <- toSet(inherited.secondSet)
      decl <- meth match {
        case f : JavaFunctionalMethod => Some(f.ast())
        case m : JavaDeclaredMethod => Some(m.ast())
        case gs : FieldGetterOrSetterMethod if gs.fnDecl.isSome =>
            Some(gs.fnDecl.unwrap)
        case o => System.err.println("inheritedAbstractMethods: skipped "+o)
            None
      }
      SFnDecl(_,SFnHeader(_,mods,_,_,_,_,_,_),_,body,_) <- Some(decl)
      if (mods.isAbstract || ! body.isDefined)
    } res = ((tt,decl)) :: res
    res
  }

  private def collectAbstractMethods(name: IdOrOp, decls: List[Decl]) = {
    val set = new HashSet[FnDecl]
    for ( fd@SFnDecl(_,SFnHeader(_,mods,_,_,_,_,_,_),_,body,_) <- decls ) {
      if ( component.typeConses.containsKey(name) ) {
        if ( ! body.isDefined ) set += fd
      } else if ( mods.isAbstract ) set += fd
    }
    set
  }

  /* Returns true if any of the concrete method declarations in "decls"
   * implements the abstract method declaration "d".
   */
  private def implement(d: FnDecl, decls: List[Decl], ast: TraitType): Boolean =
    decls.exists( (decl: Decl) => decl match {
                  case fd:FnDecl => implement(d, fd, ast)
                  case _ => false
                } )

  /* Returns true if the concrete method declaration "decl"
   * implements the abstract method declaration "d".
   */
  private def implement(d: FnDecl, decl: FnDecl, t: TraitType): Boolean = {
    // Quickly reject unmatched decls.  Note that we're still doing an O(n^2) search here.
    if (!NU.getName(d).asInstanceOf[IdOrOp].getText.equals(
         NU.getName(decl).asInstanceOf[IdOrOp].getText))
      return false;
    // Quickly reject unmatched modifiers.
    if (!NU.getMods(d).containsAll(NU.getMods(decl)))
      return false;

    val tci = typeAnalyzer.traits.typeCons(t.getName)
    var sparams = List[StaticParam]()
    if ( tci.isSome && tci.unwrap.isInstanceOf[TraitIndex] )
      sparams = toList(NU.getStaticParams(tci.unwrap.asInstanceOf[TraitIndex].ast.asInstanceOf[TraitObjectDecl])).asInstanceOf[List[StaticParam]]
    val sargs = toList(t.getArgs)

    // Extend the type analyzer with the collected static parameters
    def subst(ty: Type) =
      staticInstantiation(sparams zip sargs, ty).getOrElse(ty)
    val result =
      ( typeAnalyzer.equivalent(subst(NU.getParamType(d).asInstanceOf[Type]),
                                subst(NU.getParamType(decl))).isTrue ||
        implement(toList(NU.getParams(d)), toList(NU.getParams(decl))) ) &&
      typeAnalyzer.subtype(subst(NU.getReturnType(decl).unwrap),
                           subst(NU.getReturnType(d).unwrap)).isTrue
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
