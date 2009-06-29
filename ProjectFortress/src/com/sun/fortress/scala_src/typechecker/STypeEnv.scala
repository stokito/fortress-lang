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

import com.sun.fortress.exceptions.InterpreterBug.bug
import com.sun.fortress.nodes._
import com.sun.fortress.scala_src.useful.SExprUtil
import scala.collection.immutable.EmptyMap

import StaticEnv.EnvBinding

/**
 * Represents a list of variable name to type bindings for some context. All
 * instances of this class should be created with `STypeEnv.make` or with the
 * `extendWith` method.
 */
abstract sealed class STypeEnv extends StaticEnv[Type] {
  
  /** My type. */
  type Env = STypeEnv
  
  /** Extend me with the immediate bindings of the given node. */
  def extendWith(node: Any): STypeEnv =
    new NestedSTypeEnv(this, STypeEnv.extractEnvBindings(node))
  
  /** Same as `lookup`. */
  def getType(x: Name): Option[Type] = lookup(x)
}

/** The single empty type environment. */
object EmptySTypeEnv extends STypeEnv with EmptyStaticEnv[Type]

/**
 * A type environment with a parent and some explicit bindings.
 * 
 * @param parent A type environment that this one extends.
 * @param _bindings A collection of all the bindings in this environment.
 */
class NestedSTypeEnv protected (protected val parent: STypeEnv,
                                _bindings: Collection[EnvBinding[Type]])
    extends STypeEnv with NestedStaticEnv[Type] {
    
  /** Internal representation of `bindings` is a map. */
  protected val bindings: Map[Name, Type] = new EmptyMap ++ _bindings
}

/** Companion module for STypeEnv. */
object STypeEnv extends StaticEnvCompanion[Type] {
  
  /** My type. */
  type Env = STypeEnv
  
  /** New type environment with empty parent and the node's bindings. */
  def make(node: Any): STypeEnv =
    new NestedSTypeEnv(EmptySTypeEnv, extractEnvBindings(node))
  
  /** Extract out the bindings in node. */
  protected def extractEnvBindings(node: Any): Collection[EnvBinding[Type]] =
    null
  
//  protected def extractFnDecls(node: Any): List[FnDecl] = null
//  
//  /**
//   * A type environment that stores types of named functionals. Return types
//   * might not always be given on functional declarations, so this type
//   * environment will lazily evaluate those types as needed.
//   * 
//   * This class should only ever be instantiated by `STypeEnv`.
//   * 
//   * @param parent A static environment that this one extends.
//   * @param fnDecls An immutable list of all the functional declarations in
//   *                scope for this type environment.
//   */
//  protected class LazyFnTypeEnv(protected val parent: STypeEnv,
//                                protected val fnDecls: List[FnDecl],
//                                protected val initialChecker: STypeChecker)
//      extends STypeEnv(parent, Nil) {
//    
//    import scala.collection.mutable.Map
//    import scala.collection.mutable.Stack
//    
//    type TypeThunk = Function0[Option[Type]]
//    type UnambiguousName = Name
//    
//    /**
//     * Contains the function names that have been called thus far during the
//     * type checking of function bodies in this environment.
//     */
//    protected var callStack: Stack[Name] = new Stack
//    
//    /** Maps unsolved overloadings to their thunks. */
//    protected var unsolvedOverloadings: Map[UnambiguousName, TypeThunk] = Map.empty
//    
//    /** Maps solved overloadings to their thunks. */
//    protected var solvedOverloadings: Map[UnambiguousName, Type] = Map.empty
//    
//    /**  */
//    protected var solved: Map[Name, Type] = Map.empty
//    
//    protected def makeThunk(fnDecl: FnDecl): TypeThunk = {
//      
//      // If return type given, just thunk it.
//      fnDecl.getHeader.getReturnType match {
//        case Some(t) => return () => t
//        case _ =>
//      }
//      
//      // Get relevant parts out of the decl.
//      val name = fnDecl.getHeader.getName
//      val unambiguousName = fnDecl.getUnambiguousName
//      val body = fnDecl.getBody.getOrElse(bug("No body on function."))
//      
//      // Capture the type checker at this point.
//      val checker = this.initialChecker
//      def thunk: Option[Type] = {
//        
//        // If we have already seen this name, then there is a cycle!
//        if (callStack.contains(name)) {
//          checker.signal(body, "Cyclical reference to function %s while checking body of function %s"
//                                   .format(name, this.callStack.top))
//        }
//        
//        // Add this name to the call stack.
//        this.callStack += name
//        
//        // Extend the type checker with the params of this decl and check.
//        val newChecker = checker.extend(fnDecl)
//        SExprUtil.getType(newChecker.checkExpr(body))
//      }
//      thunk _
//    }
//  }
}
