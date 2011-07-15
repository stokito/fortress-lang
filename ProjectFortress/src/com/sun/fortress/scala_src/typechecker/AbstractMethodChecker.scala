/*******************************************************************************
    Copyright 2009,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.scala_src.typechecker

import _root_.java.util.ArrayList
import scala.collection.Map
import scala.collection.Set
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.Iterator._
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
import com.sun.fortress.nodes_util.{NodeFactory => NF}
import com.sun.fortress.nodes_util.Span
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.typechecker.Formula._
import com.sun.fortress.scala_src.types.TypeAnalyzer
import com.sun.fortress.scala_src.types.TypeAnalyzerUtil
import com.sun.fortress.scala_src.useful.Iterators._
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
  implicit var typeAnalyzer = TypeAnalyzer.make(new TraitTable(component, globalEnv))
  var errors = List[StaticError]()
  val componentName = component.ast.getName
  private def error(loc: Span, msg: String) =
    errors = errors ::: List(TypeError.make(msg, loc))
  def check() = { walk(component.ast); errors }

  override def walk(node:Any):Any = {
    node match {
      case o@SObjectDecl(SSpanInfo(span),
                         STraitTypeHeader(sparams, _, name, _, _, _, extendsC, ps, decls),
                         _) => ps match {
        case Some(params) if ! params.isEmpty =>
          checkObject(span, sparams, name, extendsC,
                      params.map(p => NF.makeVarDecl(toJavaList(List(NF.makeLValue(p))))) ::: walk(decls).asInstanceOf[List[Decl]])
        case _ =>
          checkObject(span, sparams, name, extendsC, walk(decls).asInstanceOf[List[Decl]])
      }

      case o@SObjectExpr(SExprInfo(span, _, _),
                         STraitTypeHeader(_, _, name, _, _, _, extendsC, _, decls),
                         _) =>
        checkObject(span, List(), name, extendsC,
                    walk(decls).asInstanceOf[List[Decl]])
        val methods = inheritedMethods(extendsC, typeAnalyzer)
        val inherited = toSet(methods.firstSet)
                        .map(_.asInstanceOf[IdOrOpOrAnonymousName])
        for {
          d <- decls;
          if d.isInstanceOf[FnDecl];
          val f = d.asInstanceOf[FnDecl];
          if NU.isFunctionalMethod(NU.getParams(f));
          if !inherited.contains(NU.getName(f))
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
    for ((owner, d) <- toCheck; if !implement(d, decls, owner)) {
        error(span,
              "The inherited abstract method " + d + " from the trait " + owner +
              "\n    in the object " + name +
              " is not defined in the component " + componentName + ".")
    }
    typeAnalyzer = oldTypeAnalyzer
  }

  private def inheritedAbstractMethods(extended_traits: List[TraitTypeWhere]):
      Iterator[(TraitType, FnDecl)] = {
      val supers = extended_traits.filter(t => !t.getBaseType().isInstanceOf[NamedType] ||
                                          !t.getBaseType().asInstanceOf[NamedType].getName().getApiName().isSome())

    val inherited = inheritedMethods(supers, typeAnalyzer)
    for {
      (meth : HasSelfType, _, tt) <- inherited.secondSet
      decl <- meth match {
        case f : JavaFunctionalMethod => single(f.ast())
        case m : JavaDeclaredMethod => single(m.ast())
        case gs : FieldGetterOrSetterMethod =>
            if (gs.fnDecl.isSome)
                single(gs.fnDecl.unwrap)
            else
                single(NF.mkFnDecl(gs.getSpan, gs.ast.getMods, gs.name, toJavaList(List[Param]()),
                                   NU.optTypeOrPatternToType(gs.ast.getIdType).unwrap))
        case o => System.err.println("inheritedAbstractMethods: skipped "+o)
            empty
      }
      SFnDecl(_,SFnHeader(_,mods,name,_,_,_,_,_),_,body,_) <- single(decl)
      /* BUGFIX: http://java.net/jira/browse/PROJECTFORTRESS-3 -- it's the trait
       * that should have no associated API, NOT the method name. */
      if (mods.isAbstract || (! body.isDefined && tt.getName.getApiName.isNone) ) 
    } yield (tt,decl)
  }

  /* Returns true if any of the concrete method declarations in "decls"
   * implements the abstract method declaration "d".
   */
  private def implement(d: FnDecl, decls: List[Decl], ast: TraitType): Boolean =
    decls.exists( (decl: Decl) => decl match {
                  case fd:FnDecl => implement(d, fd, ast)
                  case vd:VarDecl => implement(d, vd, ast)
                  case _ => false
                } )

  /* Returns true if the concrete method declaration "decl"
   * implements the abstract method declaration "d".
   */
  private def implement(d: FnDecl, decl: Decl, t: TraitType): Boolean = {
    // Quickly reject unmatched decls.  Note that we're still doing an O(n^2) search here.
    decl match {
      case fd:FnDecl =>
        if (!NU.getName(d).asInstanceOf[IdOrOp].getText.equals(
             NU.getName(fd).asInstanceOf[IdOrOp].getText))
          return false
      case vd:VarDecl if vd.getLhs.size == 1 =>
        if (!NU.getName(d).asInstanceOf[IdOrOp].getText.equals(
             vd.getLhs.get(0).getName.getText))
          return false
      case _ =>
    }

    val tci = typeAnalyzer.traits.typeCons(t.getName)
    val sparams = toOption(tci) match {
      case Some(ti:TraitIndex) =>
        toListFromImmutable(NU.getStaticParams(ti.ast.asInstanceOf[TraitObjectDecl]))
      case _ =>
        Nil
    }
    val sargs = toListFromImmutable(t.getArgs)

    // This function will replace the abstract decl's static params with those
    // static args applied to the trait in the concrete instantiation.
    def subst(ty: Type) = TypeAnalyzerUtil.substitute(sargs, sparams, ty)

    decl match {
      case fd:FnDecl =>
        //Todo: This is not quite right as we might need to alpha rename
        //Todo: The bounds for the static params may mention the parameters of the trait so we may need to substitute
        val extended = typeAnalyzer.extend(toList(fd.getHeader.getStaticParams),
          fd.getHeader.getWhereClause)
        (isTrue(extended.equivalent(subst(NU.getParamType(d)),
                                  NU.getParamType(fd))) ||
          implement(toListFromImmutable(NU.getParams(d)),
                    toListFromImmutable(NU.getParams(fd)),
                    subst _) ) &&
        isTrue(extended.subtype(NU.getReturnType(fd).unwrap,
                             subst(NU.getReturnType(d).unwrap)))
      case vd:VarDecl if vd.getLhs.size == 1 =>
        NU.isVoidType(NU.getParamType(d)) &&
        isTrue(typeAnalyzer.subtype(NU.optTypeOrPatternToType(vd.getLhs.get(0).getIdType).unwrap,
                                    subst(NU.getReturnType(d).unwrap)))
      case _ => false
    }
  }

  /**
   * Returns true if the parameters of the concrete method declaration `params`
   * `implements' the parameters of the abstract method declaration `ps`. The
   * `subst` function is used to instantiate the params of `ps`.
   */
  private def implement(ps: List[Param],
                        params: List[Param],
                        subst: Type => Type): Boolean =
    !(ps, params).zipped.exists { (p1, p2) =>
      isFalse(typeAnalyzer.equivalent(subst(NU.getParamType(p1)),
                                            NU.getParamType(p2))) &&
      !p1.getName.getText.equals("self")
    }
}
