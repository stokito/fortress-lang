/*******************************************************************************
    Copyright 2010 Sun Microsystems, Inc.,
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

package com.sun.fortress.scala_src.useful

import _root_.java.util.{List => JList}
import _root_.java.util.{Set => JSet}
import edu.rice.cs.plt.tuple.{Option => JavaOption}
import com.sun.fortress.compiler.typechecker.TypeEnv
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.Span
import com.sun.fortress.nodes_util.{ExprFactory => EF}
import com.sun.fortress.nodes_util.{NodeFactory => NF}
import com.sun.fortress.nodes_util.{NodeUtil => NU}
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.Sets._
import com.sun.fortress.scala_src.useful.SExprUtil._
import com.sun.fortress.scala_src.useful.STypesUtil._
import com.sun.fortress.useful.HasAt

/**
 * Contains miscellaneous utility code for the Node hierarchy.
 */
object SNodeUtil {

  /**
   * If the given name is an alias for another name, return the qualified,
   * aliased name. Otherwise return None.
   */
  def getAliasedName(name: Name, imports: List[Import]): Option[IdOrOp] =
    name match {
      case name @ SIdOrOp(_, Some(api), _) =>
	val unqualified = unqualifiedName(name)

	// Get the alias for `name` from this import, if it exists.
	def getAlias(imp: Import): Option[IdOrOp] = imp match {
	  case SImportNames(_, _, aliasApi, aliases) if api.equals(aliasApi) =>

	    // Get the first name that matched.
	    aliases.flatMap {
	      case SAliasedSimpleName(_, newName, Some(alias))
		if alias.equals(unqualified) =>
		  Some(newName.asInstanceOf[IdOrOp])
	      case _ => None
	    }.headOption

	  case _ => None
	}

	// Get the first name that matched within any import, or return name.
	imports.flatMap(getAlias).headOption

      case _ => None
    }

  /**
   * If the given name is an alias for another name, return the qualified,
   * asliased name. Otherwise return the given name.
   */
  def getRealName(name: Name, imports: List[Import]): Name =
    getAliasedName(name, imports).getOrElse(name)

  /** Given a name return the same name without an API. */
  def unqualifiedName(name: Name): Name = name match {
    case SId(info, _, text) => SId(info, None, text)
    case SOp(info, _, text, fix, enc) => SOp(info, None, text, fix, enc)
    case SAnonymousFnName(info, _) => SAnonymousFnName(info, None)
    case SConstructorFnName(info, _, ctor) => SConstructorFnName(info, None, ctor)
    case _ => name
  }

  /** Given a name return the same name qualified with the given API. */
  def qualifiedName(name: Name, api: APIName): Name = name match {
    case SId(info, _, text) => SId(info, Some(api), text)
    case SOp(info, _, text, fix, enc) => SOp(info, Some(api), text, fix, enc)
    case SAnonymousFnName(info, _) => SAnonymousFnName(info, Some(api))
    case SConstructorFnName(info, _, ctor) => SConstructorFnName(info, Some(api), ctor)
    case _ => name
  }

  /** Return a copy of the given static parameter but lifted. */
  def liftStaticParam(sp: StaticParam): StaticParam = {
    val SStaticParam(v1, v2, v3, v4, v5, v6, _) = sp
    SStaticParam(v1, v2, v3, v4, v5, v6, true)
  }

  /** Given a node with a Span, return the same node but with the given span. */
  def setSpan(node: HasAt, span: Span): HasAt = {
    object adder extends Walker {
      var swap = false
      override def walk(node: Any): Any = node match {
	case SExprInfo(_, a, b) if !swap =>
	  swap = true
	  SExprInfo(span, a, b)
	case STypeInfo(_, a, b, c) if !swap =>
	  swap = true
	  STypeInfo(span, a, b, c)
	case SSpanInfo(_) if !swap =>
	  swap = true
	  SSpanInfo(span)
	case _ if !swap => super.walk(node)
	case _ => node
      }
    }
    adder(node).asInstanceOf[HasAt]
  }

  /**
   * Whether `ty` in a comprises clause of a trait declaration
   * is the static parameter `sp` of the trait declaration.
   */
  private def equalSparam(ty: NamedType, sp: StaticParam) = sp.getName match {
    case id:Id => ty match {
      case SVarType(_, name, _) => id.getText().equals(name.getText())
      case _ => false
    }
    case _ => false
  }

  /**
   * For each naked type variable V in a trait `trait_name`'s comprises clause
   * V should be one of the static parameters of T
   *         the instance of T by using all its static parameter names as
   *           corresponding static arguments to the trait is implicitly
   *           regarded as one of the bounds on that static parameter V
   *           (in addition to any other bounds it might have).
   */
  def checkSparams(trait_name: Id,
		   jSparams: JList[StaticParam],
		   jComprises: JavaOption[JList[NamedType]]) =
    toOption(jComprises) match {
      case Some(jComprisesTypes) =>
	val self_type = NF.makeTraitType(trait_name,
					 STypesUtil.staticParamsToArgs(jSparams))
	var sparams = List[StaticParam]()
	for (sp <- toListFromImmutable(jSparams)) {
	  if (toListFromImmutable(jComprisesTypes).exists(equalSparam(_, sp))) {
	    sp match {
              case SStaticParam(info, name, extendsC, dim, absorbs, k:KindType, lifted) =>
		sparams ++= List(SStaticParam(info, name,
					      self_type :: extendsC,
					      dim, absorbs, k, lifted))
              case _ => sparams ++= List(sp)
	    }
	  } else sparams ++= List(sp)
	}
	toJavaList(sparams)
      case _ => jSparams
    }

  private def equalSparam(sp: StaticParam, id: Id) = sp.getName match {
    case name:Id => id.getText().equals(name.getText())
    case _ => false
  }

  def validComprises(comprises: Set[NamedType],
		     jSparams: JList[StaticParam]): Option[Id] = {
    for (ty <- comprises) {
      ty match {
	case SVarType(_, name, _) =>
	  if (!toListFromImmutable(jSparams).exists(equalSparam(_, name))) return Some(name)
	case _ =>
      }
    }
    None
  }

  def getTraitType(ty: Type): Option[TraitType] = ty match {
    case tt:TraitType => Some(tt)
    case tt:TraitSelfType => getTraitType(tt.getNamed)
    case _ => None
  }
}
