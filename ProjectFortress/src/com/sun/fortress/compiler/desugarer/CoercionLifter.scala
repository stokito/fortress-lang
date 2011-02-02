/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.desugarer

import com.sun.fortress.compiler.GlobalEnvironment
import com.sun.fortress.compiler.index.ApiIndex
import com.sun.fortress.compiler.index.TraitIndex
import com.sun.fortress.exceptions.InterpreterBug.bug
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.{ExprFactory => EF}
import com.sun.fortress.nodes_util.{NodeFactory => NF}
import com.sun.fortress.nodes_util.{NodeUtil => NU}
import com.sun.fortress.nodes_util.Span
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.SExprUtil._
import com.sun.fortress.scala_src.useful.SNodeUtil._
import com.sun.fortress.scala_src.useful.STypesUtil._

/**
 * Lifts coercions to top level. Any coercion declaration appearing in a trait
 * Foo will be moved to top level with a new, inexpressible name (e.g.
 * `coerce$Foo`). This lifting should be performed both in APIs and in
 * components to be consistent.
 */
class CoercionLifter(env: GlobalEnvironment) extends Walker {

  /** A list of lifted coercion declarations to insert at top level. */
  protected var liftedCoercions: List[FnDecl] = Nil

  /**
   * Public method that does the actual lifting for the given program. Returns
   * a new program with the coercions lifted.
   */
  def liftCoercions(program: CompilationUnit): CompilationUnit = {
    liftedCoercions = Nil
    walk(program) match {
      case a:Api => putCoercionsInto(a)
      case c:Component => putCoercionsInto(c)
      case _ => bug("Unexpected program in CoercionLifter!")
    }
  }

  /** Walk the hierarchy to gather coercions. */
  override def walk(node: Any) = node match {

    // Lift out this trait's coercions.
    case t @ STraitDecl(t1, STraitTypeHeader(h1, h2, h3, h4, h5, h6, h7, h8, _), t3, t4, t5, t6) =>
      val decls = liftCoercionsFromTrait(t)
      STraitDecl(t1, STraitTypeHeader(h1, h2, h3, h4, h5, h6, h7, h8, decls), t3, t4, t5, t6)

    // Lift out this object's coercions.
    case t @ SObjectDecl(o1, STraitTypeHeader(h1, h2, h3, h4, h5, h6, h7, h8, _), o3) =>
      val decls = liftCoercionsFromTrait(t)
      SObjectDecl(o1, STraitTypeHeader(h1, h2, h3, h4, h5, h6, h7, h8, decls), o3)

    case _ => super.walk(node)
  }

  /** Lift out all the coercions to `t` and return `t`'s non-coercion decls. */
  protected def liftCoercionsFromTrait(t: TraitObjectDecl): List[Decl] = {

    // Get all the trait's decls.
    val decls = toListFromImmutable(t.getHeader.getDecls)

    // Partition into coercion decls and others.
    val (coercionDecls, otherDecls) = decls.partition(NU.isCoercion)

    // Make lifted coercions from this trait's coercions.
    liftedCoercions ++=
      coercionDecls.map { d: Decl =>
        CoercionLifter.makeLiftedCoercion(d.asInstanceOf[FnDecl], t)
      }

    // Return the other decls.
    otherDecls
  }

  /** Put the lifted coercions into this API. Just add them to the decls. */
  protected def putCoercionsInto(api: Api): Api = {
    val SApi(a1, a2, a3, decls, a5) = api

    // Make the new API with the lifted coercions.
    val newApi = SApi(a1, a2, a3, decls ++ liftedCoercions, a5)

    // Clear the lifted coercions list and return the new API.
    liftedCoercions = Nil; newApi
  }

  /**
   * Put the lifted coercions into this component. This adds them to the
   * component's decls and then adds any additional imports for coercions in
   * other APIs.
   */
  protected def putCoercionsInto(component: Component): Component = {
    val SComponent(c1, c2, imports, decls, c5, c6, c7) = component

    // Create the new list of decls and clear the lifted coercions list.
    val newDecls = liftedCoercions
    liftedCoercions = Nil

    // For any traits that are explicitly imported and that declare coercions,
    // add a new import for its lifted coercion function.
    val newImports = imports.flatMap {

      // Explicit import statement:
      case imp @ SImportNames(_, _, apiName, aliases) =>

        // Get the API index for this imported API.
        val api = env.lookup(apiName)

        // Get the imported trait names that have coercions and create a new
        // import for their lifted coercions.
        val traitsWithCoercions = aliases.flatMap { a: AliasedSimpleName =>
          CoercionLifter.isTraitWithCoercions(api, a.getName)
        }
        if (traitsWithCoercions.isEmpty)
          None
        else
          Some(CoercionLifter.makeCoercionImport(apiName, traitsWithCoercions, imp))

      // Any other kind of import statement won't explicitly import traits.
      case _ => None
    }

    // Make the new component using the new decls and imports.
    SComponent(c1, c2, imports ++ newImports, decls ++ newDecls, c5, c6, c7)
  }
}

object CoercionLifter {

  /** Make the new, lifted coercion function's FnDecl. */
  protected def makeLiftedCoercion(f: FnDecl, t: TraitObjectDecl): FnDecl = {
    val SFnDecl(f1, header, f3, f4, f5) = f
    val SFnHeader(fnSparams, h2, _, h4, h5, h6, h7, _) = header

    // Get the lifted name.
    val newName = NF.makeLiftedCoercionId(NU.getSpan(NU.getName(f)), NU.getName(t))

    // Concatenate the trait's and the function's static params.
    val liftedSparams = toListFromImmutable(NU.getStaticParams(t)).map(liftStaticParam)
    val sparams = liftedSparams ++ fnSparams

    // Create the return type.
    val returnType = t.getSelfType.getOrElse(bug("No self type on trait "+t))

    // Create a new FnHeader with the new info.
    // TODO: Worry about `implementsUnambiguousName` field?
    val newHeader = SFnHeader(sparams, h2, newName, h4, h5, h6, h7, Some(returnType))
    
    // Create a new FnDecl with the new header.
    SFnDecl(f1, newHeader, f3, f4, f5)
  }

  /**
   * Is `name` in API `api` a trait with declared coercions? If so, return the
   * name as an Id.
   */
  protected def isTraitWithCoercions(api: ApiIndex, name: Name): Option[Id] = {
    
    // Since this is called for any name and not just traits, it may not be an Id.
    val id = name match {
      case id:Id => id
      case _ => return None
    }

    // Look for a trait with this name and check for coercions.
    api.typeConses.get(id) match {
      case t:TraitIndex if !t.coercions.isEmpty => Some(id)
      case _ => None
    }
  }

  /**
   * Creates an import statement that imports the lifted coercion functions of
   * each of the given traits from the given API. Uses the original import of
   * these traits for the node info and foreign language.
   */
  protected def makeCoercionImport(apiName: APIName,
                                   traits: List[Id],
                                   fromImport: ImportNames): ImportNames = {
    // Make the simple aliases for the lifted coercion functions.
    val aliases = traits.map { t =>
      val span = NU.getSpan(t)
      val liftedName = NF.makeLiftedCoercionId(span, t)
      SAliasedSimpleName(SSpanInfo(span), liftedName, None)
    }

    SImportNames(fromImport.getInfo, fromImport.getForeignLanguage, apiName, aliases)
  }
}
