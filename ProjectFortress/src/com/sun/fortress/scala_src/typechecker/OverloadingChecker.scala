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
import _root_.java.util.{Set => JavaSet}
import scala.collection.jcl.Conversions

import com.sun.fortress.compiler.GlobalEnvironment
import com.sun.fortress.compiler.index.ComponentIndex
import com.sun.fortress.compiler.index.{Function => JavaFunction}
import com.sun.fortress.exceptions.InterpreterBug
import com.sun.fortress.exceptions.StaticError
import com.sun.fortress.exceptions.TypeError
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.NodeFactory
import com.sun.fortress.nodes_util.NodeUtil
import com.sun.fortress.nodes_util.Span
import com.sun.fortress.parser_util.IdentifierUtil
import com.sun.fortress.repository.FortressRepository
import com.sun.fortress.scala_src.useful._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.nodes._

/* Check the set of overloadings in this component. */
object OverloadingChecker {

    /* Called by com.sun.fortress.compiler.StaticChecker.checkComponent */
    def checkOverloading(component: ComponentIndex,
                         globalEnv: GlobalEnvironment,
                         repository: FortressRepository): JavaList[StaticError] = {
        val errors = new ArrayList[StaticError]()
        val fnsInComp = component.functions
        for ( f <- Conversions.convertSet(fnsInComp.firstSet) ;
              if isDeclaredName(f) ) {
            checkOverloading(f, fnsInComp.matchFirst(f), errors)
        }
        errors
    }

    /* Returns the function declaration covering the given set of
     * the overloaded declarations.
     * Invariant: set.size > 1
     * Nothing fancy here yet.  Returns the first element for now.
     */
    def coverOverloading(set: JavaSet[JavaFunction]) = set.iterator.next

    def isDeclaredName(f: IdOrOpOrAnonymousName) = f match {
        case Id(_,_,str) => IdentifierUtil.validId(str)
        case Op(_,_,str,_,_) => NodeUtil.validOp(str)
        case _ => false
    }

    def isDeclaredFunction(f: JavaFunction) = f match {
        case DeclaredFunction(fd) => true
        case _ => false
    }

    /* Checks the validity of the overloaded function declarations.
     * Nothing fancy here yet.
     * Signals errors only for the declarations with the same types for now.
     */
    private def checkOverloading(name: IdOrOpOrAnonymousName,
                                 set: JavaSet[JavaFunction],
                                 errors: JavaList[StaticError]) = {
        var signatures = List[((Type,Type),Span)]()
        for ( f <- Conversions.convertSet(set) ;
              if isDeclaredFunction(f) ) {
            val result = f.getReturnType
            val param = paramsToType(f.parameters, f.getSpan)
            signatures.find(p => p._1 == (param,result)) match {
                case Some((_,span)) =>
                    error(errors, mergeSpan(span, f.getSpan),
                          "There are multiple declarations of " +
                          name + " with the same signature: " +
                          param + " -> " + result)
                case _ =>
                    signatures = ((param, result), f.getSpan) :: signatures
            }
        }
    }

    private def mergeSpan(first: Span, second: Span): String = {
        if (first.toString < second.toString)
            first.toString + "\n" + second.toString
        else
            second.toString + "\n" + first.toString
    }

    /* Returns the type of the given list of parameters. */
    private def paramsToType(params: JavaList[Param], span: Span): Type =
        params.size match {
            case 0 => NodeFactory.makeVoidType(span)
            case 1 => paramToType(params.get(0))
            case _ =>
            NodeFactory.makeTupleType(NodeUtil.spanAll(params),
                                      Lists.toJavaList(Lists.toList(params).map(p => paramToType(p))))
        }

    /* Returns the type of the given parameter. */
    private def paramToType(param: Param): Type =
        (toOption(param.getIdType), toOption(param.getVarargsType)) match {
            case (Some(ty), _) => ty
            case (_, Some(ty)) => ty
            case _ => InterpreterBug.bug(param,
                                         "Type checking couldn't infer the type of " + param)
        }

    private def error(errors: JavaList[StaticError], loc: String, msg: String) =
        errors.add(TypeError.make(msg, loc.toString()))
}
