/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.scala_src.disambiguator

import _root_.java.util.List
import edu.rice.cs.plt.tuple.Option
import com.sun.fortress.compiler.NamingCzar
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.{NodeFactory => NF}
import com.sun.fortress.nodes_util.{NodeUtil => NU}
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.STypesUtil._

/**
 * Replaces implicitly-typed 'self' parameters of methods with explicitly-typed
 * ones. This is now a separate visitor because it needs to occur during
 * disambiguation but before the TypeDisambiguator pass.
 * At parse-time, methods that take the 'self' parameter may not have a
 * type for that parameter. However, at disambiguation time, we can give
 * it one.<br>
 * {@code trait Foo f(self) : () end}
 * becomes
 * {@code trait Foo f(self:Foo) : () end}
 * This method will only replace parameters "down" to the next
 * object or trait declaration. When the new trait or object is declared,
 * this method will be called again. This method is guaranteed to return
 * the type of node given.
 */
class SelfParamDisambiguator extends Walker {
  /* The type of 'self' is the type of the trait or object being declared
   * by the innermost enclosing trait or object declaration or object expression.
   */
  override def walk(node: Any): Any = node match {
    case SObjectDecl(_, STraitTypeHeader(sparams,_,name,_,_,_,_,_,_), _) =>
      // Add a type to self parameters of methods
      val self_type = NF.makeSelfType(NF.makeTraitType(name.asInstanceOf[Id],
                                                       staticParamsToArgs(toJavaList(sparams))))
      replaceSelfParamsWithType(node, self_type).asInstanceOf[ObjectDecl] match {
        case SObjectDecl(info, header, _) =>
          super.walk(SObjectDecl(info, header, some(self_type)))
      }

    /* The type of 'self' is the type of the trait or object being declared
     * by the innermost enclosing trait or object declaration or object expression.
     * If the innermost enclosing such construct is a trait declaration
     * (for a trait called, say, 'T'), and that trait declaration has
     * a 'comprises' clause, and 'U_1', 'U_2', ..., 'U_n' are the traits mentioned
     * in the 'comprises' clause, then the type of 'self' is instead
     * '(T & (U_1 | U_2 | ... | U_n))', where '&' indicates type intersection and
     * '|' indicates type union.
     */
    case STraitDecl(_, STraitTypeHeader(sparams,_,name,_,_,_,_,_,_),
                    _, _, comprisesC, _) =>
      // Add a type to self parameters of methods
      val type_name = NF.makeTraitType(name.asInstanceOf[Id],
                                       staticParamsToArgs(toJavaList(sparams)))
      val self_type = comprisesC match {
        case Some(comprises@_::_) => NF.makeSelfType(type_name, toJavaList(comprises))
        case _ => NF.makeSelfType(type_name)
       
      }
      replaceSelfParamsWithType(node, self_type).asInstanceOf[TraitDecl] match {
        case STraitDecl(info, header, _, excludes, comprises, ellipses) =>
          super.walk(STraitDecl(info, header, some(self_type),
                                excludes, comprises, ellipses))
      }

    case oe@SObjectExpr(_, STraitTypeHeader(sparams,_,name,_,_,_,_,_,_), _) =>
      // Add a type to self parameters of methods
      val self_type = getObjectExprType(oe)
      replaceSelfParamsWithType(oe, self_type) match {
        case SObjectExpr(info, header, _) =>
          super.walk(SObjectExpr(info, header, some(self_type)))
      }

    case _ => super.walk(node)
  }

  /**
   * Replaces Parameters whose name is 'self' with a parameter with
   * the explicit type given.
   *
   * @param thatNode
   * @param self_type
   */
  def replaceSelfParamsWithType(that_node: Any, self_type: SelfType): Node = {
    object replacer extends Walker {
      var traitNestingDepth = 0

      override def walk(node: Any): Any = node match {
        case SParam(info, name, mods, idType, defaultExpr, varargs) => varargs match {
          case Some(_) => super.walk(node)
          case None =>
            // my type is broken I need to qualify the type name
            val new_type: Option[TypeOrPattern] =
              if (name.equals(NamingCzar.SELF_NAME)) some(self_type)
              else idType
            SParam(info, name, mods, new_type, defaultExpr, None)
        }

        case od:ObjectDecl =>
          traitNestingDepth += 1
          if (traitNestingDepth > 1) od else super.walk(od)

        case td:TraitDecl =>
          traitNestingDepth += 1
          if (traitNestingDepth > 1) td else super.walk(td)

        case oe:ObjectExpr =>
          traitNestingDepth += 1
          if (traitNestingDepth > 1) oe else super.walk(oe)

        case _ => super.walk(node)
      }
    }
    replacer(that_node).asInstanceOf[Node]
  }
}
