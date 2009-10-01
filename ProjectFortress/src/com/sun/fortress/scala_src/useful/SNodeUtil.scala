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

package com.sun.fortress.scala_src.useful

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
            }.firstOption            
            
          case _ => None
        }
  
        // Get the first name that matched within any import, or return name.
        imports.flatMap(getAlias).firstOption
  
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
}