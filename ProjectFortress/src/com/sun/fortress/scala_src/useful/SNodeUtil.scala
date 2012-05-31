/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.scala_src.useful

import _root_.java.util.{List => JList}
import _root_.java.util.{Set => JSet}
import edu.rice.cs.plt.tuple.{Option => JavaOption}
import com.sun.fortress.exceptions.InterpreterBug.bug
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.Span
import com.sun.fortress.nodes_util.{ExprFactory => EF}
import com.sun.fortress.nodes_util.{NodeFactory => NF}
import com.sun.fortress.nodes_util.{NodeUtil => NU}
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.typechecker.staticenv.EmptyKindEnv
import com.sun.fortress.scala_src.typechecker.staticenv.KindEnv
import com.sun.fortress.scala_src.typechecker.staticenv.StaticEnv
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
    case SNamedOp(info, _, text, fix, enc) => SNamedOp(info, None, text, fix, enc)
    case SAnonymousFnName(info, _) => SAnonymousFnName(info, None)
    case SConstructorFnName(info, _, ctor) => SConstructorFnName(info, None, ctor)
    case _ => name
  }

  /** Given a name return the same name qualified with the given API. */
  def qualifiedName(name: Name, api: APIName): Name = name match {
    case SId(info, _, text) => SId(info, Some(api), text)
    case SNamedOp(info, _, text, fix, enc) => SNamedOp(info, Some(api), text, fix, enc)
    case SAnonymousFnName(info, _) => SAnonymousFnName(info, Some(api))
    case SConstructorFnName(info, _, ctor) => SConstructorFnName(info, Some(api), ctor)
    case _ => name
  }

  /** Return a copy of the given static parameter but lifted. */
  def liftStaticParam(sp: StaticParam): StaticParam = {
    val SStaticParam(v1, v2, v3, v4, v5, v6, v7, v8, _) = sp
    SStaticParam(v1, v2, v3, v4, v5, v6, v7, v8, true)
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
              case SStaticParam(info, variance, name, extendsC, dominatesC, dim, absorbs, k:KindType, lifted) =>
		sparams ++= List(SStaticParam(info, variance, name,
					      (self_type :: extendsC).distinct, dominatesC,
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

  /**
   * Returns a type with all bound static parameters replaced with unique
   * identifiers for a specified static environment. Each call should generate
   * entirely different names.
   */
  def alphaRenameTypeSchema(t: Type, env: KindEnv): Type = {
    if (t.getInfo.getStaticParams.isEmpty) return t
    
    // Make a substitution of [Ti -> Xi] for each static parameter Ti where
    // Xi is fresh in the static environment.
    val subst = getStaticParams(t).map(sp => (sp.getName, makeFreshName(sp.getName, env).asInstanceOf[Id]))
    val renamedType = alphaRename(subst, t).asInstanceOf[Type]
    val renamedStaticParams = getStaticParams(t).zip(subst.map(_._2)).map(x => x match { case (a, b) =>
        val renamedExtendsClause = toList(a.getExtendsClause).map(ty => alphaRename(subst, ty).asInstanceOf[BaseType])
        NF.makeStaticParam(a, b, toJavaList(renamedExtendsClause))
    })
    insertStaticParams(clearStaticParams(renamedType), toJavaList(renamedStaticParams))
  }

  /**
   * Returns a type function/method header with all bound static parameters replaced
   * with unique identifiers for this static environment. Each call should generate
   * entirely different names.
   */
  def alphaRenameHeader(h: FnHeader, env: KindEnv): FnHeader = {
    if (h.getStaticParams.isEmpty) return h
    
    // Make a substitution of [Ti -> Xi] for each static parameter Ti where
    // Xi is fresh in the static environment.
    val subst = toListFromImmutable(h.getStaticParams).map { sp =>
      val srcName = sp.getName
      val dstName = makeFreshName(srcName, env)
      (srcName, dstName)
    }
    alphaRename(subst, h).asInstanceOf[FnHeader]
  }

  /**
   * Replace every occurrence of a the name `orig` with the name `repl` in the
   * node `body`. Every occurrence of a name with the same API and text as
   * `orig` is replaced with the name `repl`. The span of the original name is
   * preserved on the new occurrence of `repl`.
   */
  def alphaRename(orig: IdOrOp, repl: IdOrOp, body: Node): Node =
    alphaRename(List((orig, repl)), body)
  
  /**
   * Replace every occurrence of a the name `orig` with the name `repl` in the
   * node `body`. Every occurrence of a name with the same API and text as
   * `orig` is replaced with the name `repl`. The span of the original name is
   * preserved on the new occurrence of `repl`.
   */
  def alphaRename(repls: Iterable[(IdOrOp, IdOrOp)], body: Node): Node = {
    
    // Predicate that says the replacement matches the name x.
    def matches(x: IdOrOp)(repl: (IdOrOp, IdOrOp)): Boolean = {
      val SIdOrOp(_, xApi, xText) = x
      val SIdOrOp(_, oApi, oText) = repl._1
      xApi == oApi && xText == oText
    }
    
    // Walker that replaces the names.
    object renamer extends Walker {
      override def walk(n: Any): Any = n match {
        
        // If this name matches one of the replacements in API and text, replace
        // it while preserving the span. Otherwise we don't need to recur since
        // no nested names.
        case x:IdOrOp => repls.find(matches(x)) match {
          case Some((_, repl)) => setSpan(repl, x.getInfo.getSpan)
          case _ => x
        }
        
        case _ => super.walk(n)
      }
    }
    
    // Walk
    renamer(body).asInstanceOf[Node]
  }
  
  /**
   * Check for any occurrence of `name` in `body`. Returns true iff there is any
   * occurrence of an IdOrOp node with the same API and text as `name` somewhere
   * within the node `body`.
   */
  def nameInBody(name: IdOrOp, body: Node): Boolean = {
    val SIdOrOp(_, api, text) = name
    
    // The walker sets found = true when it finds the given name.
    object checker extends Walker {
      var found = false 
      override def walk(n: Any): Any = n match {
        case SIdOrOp(_, napi, ntext)
               if !found && napi == api && ntext == text => found = true
        case _ if !found => super.walk(n)
        case _ => n
      }
    }
    
    // Walk and return whether or not it was found.
    checker.walk(body)
    checker.found
  }
  
  /**
   * Creates a duplicate of `x` with a fresh name within `node`. Repeatedly
   * creates a duplicate of `x` with a fresh name, checking if it exists within
   * `node`. If so, it continues (stopping with an error eventually); otherwise
   * it returns the successful, fresh duplicate.
   */
  def makeFreshName(x: IdOrOp, node: Node): IdOrOp =
    makeFreshNameHelper(x, y => !nameInBody(y, node))
  
  /**
   * Creates a fresh name within `node`. Repeatedly creates a fresh name,
   * checking if it exists within `node`. If so, it continues (stopping with an
   * error eventually); otherwise it returns the successful, fresh duplicate.
   */
  def makeFreshName(node: Node): IdOrOp = {
    // Ok to use a dummy span because if this name is used for alpha renaming,
    // the span of the original name it replaces remains.
    val tempId = NF.bogusId(NF.typeSpan)
    makeFreshNameHelper(tempId, y => !nameInBody(y, node))
  }
  
  /**
   * Creates a duplicate of `x` with a fresh name within `env`. Repeatedly
   * creates a duplicate of `x` with a fresh name, checking if it exists either
   * in the domain of `env`. If so, it continues (stopping with an error
   * eventually); otherwise it returns the successful, fresh duplicate.
   */
  def makeFreshName[T](x: IdOrOp, env: StaticEnv[T]): IdOrOp =
    makeFreshNameHelper(x, y => !env.contains(y))
  
  /**
   * Creates a fresh name within `env`. Repeatedly creates a fresh name,
   * checking if it exists either in the domain of `env`. If so, it continues
   * (stopping with an error eventually); otherwise it returns the successful,
   * fresh duplicate.
   */
  def makeFreshName[T](env: StaticEnv[T]): IdOrOp = {
    // Ok to use a dummy span because if this name is used for alpha renaming,
    // the span of the original name it replaces remains.
    val tempId = NF.bogusId(NF.typeSpan)
    makeFreshNameHelper(tempId, y => !env.contains(y))
  }
  
  /** Counter used for generating fresh names. */
  private var freshNameCounter = 1
  
  /**
   * Helper for the companion method that takes in an arbitrary predicate
   * for determining if a name is fresh. Fresh names are generated by appending
   * a dollar sign ($) and a fresh integer (according to `freshNameCounter`) to
   * the name `x`.
   */
  private def makeFreshNameHelper(x: IdOrOp,
                                  isFresh: IdOrOp => Boolean)
                                  : IdOrOp = {
    
    // Creates a duplicate of `x` with a mangled name.
    def mkname(i: Int): IdOrOp = {
      
      // Match any token, followed by a dollar sign, followed by an integer.
      val FreshName = """([^$]*)$(\d+)""".r
      val s = x.getText match {
        case FreshName(name, counter) => "%s$%d".format(name, freshNameCounter)
        case _ => "%s$%d".format(x.getText, freshNameCounter) 
      }
      
      // Create an Id or Op with this name, depending on what x is.
      x match {
        case SId(info, _, _) => SId(info, None, s)
        case SNamedOp(info, _, _, f, e) => SNamedOp(info, None, s, f, e)
        case _ => null // never matches
      }
    }
    
    // Keep trying to generate a name not already in `env`.
    val MAX_I = freshNameCounter + 10000
    for (i <- freshNameCounter.to(MAX_I)) {
      val fresh = mkname(i)
      if (isFresh(fresh)) {
        freshNameCounter += 1
        return fresh
      }
    }
    bug("Failed to create a fresh name for %s".format(x))
  }
}
