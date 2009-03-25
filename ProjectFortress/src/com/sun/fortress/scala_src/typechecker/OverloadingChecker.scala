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
import edu.rice.cs.plt.tuple.{Option => JavaOption}
import scala.collection.Set
import com.sun.fortress.compiler.GlobalEnvironment
import com.sun.fortress.compiler.index.ComponentIndex
import com.sun.fortress.compiler.index.{Function => JavaFunction}
import com.sun.fortress.compiler.typechecker.TraitTable
import com.sun.fortress.compiler.typechecker.TypeAnalyzer
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
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.Sets._
import com.sun.fortress.scala_src.nodes._

/* Check the set of overloadings in this component.
 *
 * The following functionals are not checked yet:
 *     functional methods
 *     dotted methods
 *     object constructors
 *
 * The following features are not (fully) checked yet:
 *     static parameters
 *     exclusion relationships
 *     varargs parameters
 *     keyword parameters
 */
class OverloadingChecker(component: ComponentIndex,
                         globalEnv: GlobalEnvironment,
                         repository: FortressRepository) {

    val typeAnalyzer = TypeAnalyzer.make(new TraitTable(component, globalEnv))
    var errors = List[StaticError]()

    /* Called by com.sun.fortress.compiler.StaticChecker.checkComponent */
    def checkOverloading(): JavaList[StaticError] = {
        val fnsInComp = component.functions
        for ( f <- toSet(fnsInComp.firstSet) ; if isDeclaredName(f) ) {
            checkOverloading(f, fnsInComp.matchFirst(f))
        }
        toJavaList(errors)
    }

    /* Checks the validity of the overloaded function declarations. */
    private def checkOverloading(name: IdOrOpOrAnonymousName,
                                 set: JavaSet[JavaFunction]) = {
        var signatures = List[((Type,Type),Span)]()
        for ( f <- toSet(set) ; if isDeclaredFunction(f) ) {
            val result = f.getReturnType
            val param = paramsToType(f.parameters, f.getSpan)
            signatures.find(p => p._1 == (param,result)) match {
                case Some((_,span)) =>
                    error(mergeSpan(span, f.getSpan),
                          "There are multiple declarations of " +
                          name + " with the same type: " +
                          param + " -> " + result)
                case _ =>
                    signatures = ((param, result), f.getSpan) :: signatures
            }
        }
        var index = 1
        for ( first <- signatures ) {
            signatures.slice(index, signatures.length)
            .foreach(second => if (! validOverloading(first, second, signatures) ) {
                                   val firstO = toString(first)
                                   val secondO = toString(second)
                                   val mismatch = if (firstO < secondO)
                                                      firstO + "\n and " + secondO
                                                  else
                                                      secondO + "\n and " + firstO
                                   error(mergeSpan(first, second),
                                         "Invalid overloading of " + name +
                                         ":\n     " + mismatch)
                               })
            index += 1
        }
    }

    /* Checks the overloading rules: subtype / exclusion / meet */
    private def validOverloading(first: ((Type,Type),Span),
                                 second: ((Type,Type),Span),
                                 set: List[((Type,Type),Span)]) =
        subtype(first, second) || subtype(second, first) ||
        exclusion(first, second) || meet(first, second, set)

    /* Checks the overloading rule: subtype */
    private def subtype(newTypeAnalyzer: TypeAnalyzer, f: JavaFunction,
                        g: JavaFunction): Boolean =
        subtype(newTypeAnalyzer, paramsToType(g.parameters, g.getSpan),
                paramsToType(f.parameters, f.getSpan)) &&
        subtype(newTypeAnalyzer, f.getReturnType, g.getReturnType)

    private def subtype(newTypeAnalyzer: TypeAnalyzer, sub_type: Type,
                        super_type: Type): Boolean =
        newTypeAnalyzer.subtype(sub_type, super_type).isTrue

    private def subtype(sub_type: Type, super_type: Type): Boolean =
        subtype(typeAnalyzer, sub_type, super_type)

    private def subtype(sub_type: ((Type,Type),Span),
                        super_type: ((Type,Type),Span)): Boolean =
        subtype(super_type._1._1, sub_type._1._1) &&
        subtype(sub_type._1._2, super_type._1._2)

    /* Checks the overloading rule: exclusion */
    /* Not yet fully implemented... */
    private def exclusion(first: ((Type,Type),Span),
                          second: ((Type,Type),Span)): Boolean =
        NodeUtil.differentArity(first._1._1, second._1._1)

    /* Checks the overloading rule: meet */
    /* Not yet fully implemented... */
    private def meet(first: ((Type,Type),Span), second: ((Type,Type),Span),
                     set: List[((Type,Type),Span)]) = {
        var result = false
        val meet = (reduce(typeAnalyzer.meet(first._1._1, second._1._1)),
                    reduce(typeAnalyzer.meet(first._1._2, second._1._2)))
        for ( f <- set ; if ! result )
            if ( subtype(f._1._1, meet._1) &&
                 subtype(meet._1, f._1._1) &&
                 subtype(meet._2, f._1._2) &&
                 subtype(f._1._2, meet._2))
                result = true
        result
    }

    private def reduce(t: Type): Type = t match {
        case IntersectionType(info, elements) =>
            val (tuples, nots) = elements.partition(ty => NodeUtil.isTupleType(ty))
            if ( ! tuples.isEmpty && ! nots.isEmpty ) NodeFactory.makeBottomType(info)
            else if ( tuples.isEmpty ) t
            else {
                val size = NodeUtil.getTupleTypeSize(tuples.head)
                if ( tuples.forall(ty => NodeUtil.getTupleTypeSize(ty) == size &&
                                         ! NodeUtil.hasVarargs(ty) &&
                                         ! NodeUtil.hasKeywords(ty)) ) {
                    var elems = List[Type]()
                    var i = 0
                    while ( i < size ) {
                        elems = elems ::: List(typeAnalyzer.meet(toJavaList(tuples.map(ty => NodeUtil.getTupleTypeElem(ty, i)))))
                        i += 1
                    }
                    val mt = NodeFactory.makeTupleType(NodeUtil.getSpan(t), toJavaList(elems))
                    NodeFactory.makeIntersectionType(info, toJavaList(mt :: tuples))
                } else t
            }
        case _ => t
    }

    /* Returns the set of overloaded function declarations
     * covering the given set of the overloaded declarations.
     * Invariant: set.size > 1
     */
    def coverOverloading(set: Set[JavaFunction]) = {
        var result = Set[JavaFunction]()
        for ( f <- set ) { if ( ! coveredBy(f, result) ) result = result + f }
        result = result.filter(f => f match { case DeclaredFunction(_) => true
                                              case _ => false } )
        result.map(f => f match { case DeclaredFunction(fd) => fd })
    }

    private def coveredBy(f: JavaFunction, set: Set[JavaFunction]): Boolean = {
        var result = false
        for ( g <- set ; if ! result ) { if ( coveredBy(f, g) ) result = true }
        result
    }

    /* Whether the signature of f is covered by the signature of g */
    private def coveredBy(f: JavaFunction, g: JavaFunction): Boolean = {
        val staticParameters = new ArrayList[StaticParam]()
        // Add static parameters of "f"
        staticParameters.addAll(f.staticParameters)
        // If "f" is a functional method,
        // add static parameters of "f"'s enclosing trait or object
        if ( NodeUtil.isFunctionalMethod(f) ) {
            val ind = typeAnalyzer.traitTable.typeCons(NodeUtil.getDeclaringTrait(f))
            if ( ind.isSome && NodeUtil.isTraitOrObject(ind.unwrap) ) {
                staticParameters.addAll( NodeUtil.getStaticParameters(ind.unwrap) )
            }
        }
        // If "g" is a functional method,
        // add static parameters of "g"'s enclosing trait or object
        if ( NodeUtil.isFunctionalMethod(g) ) {
            val ind = typeAnalyzer.traitTable.typeCons(NodeUtil.getDeclaringTrait(g))
            if ( ind.isSome && NodeUtil.isTraitOrObject(ind.unwrap) ) {
                staticParameters.addAll( NodeUtil.getStaticParameters(ind.unwrap) )
            }
        }
        // Add static parameters of "g"
        for ( s <- toList(g.staticParameters) ) staticParameters.add(s)
        // Extend the type analyzer with the collected static parameters
        val newTypeAnalyzer = typeAnalyzer.extend(staticParameters,
                                                  none[WhereClause])
        // Whether "g"'s parameter type is a subtype of "f"'s parameter type
        // and "f"'s return type is a subtype of "g"'s return type
        subtype(newTypeAnalyzer, f, g)
    }

    def isDeclaredName(f: IdOrOpOrAnonymousName) = f match {
        case Id(_,_,str) => IdentifierUtil.validId(str)
        case Op(_,_,str,_,_) => NodeUtil.validOp(str)
        case _ => false
    }

    def isDeclaredFunction(f: JavaFunction) = f match {
        case DeclaredFunction(fd) => true
        case _ => false
    }

    private def mergeSpan(sub_type: ((Type,Type),Span),
                          super_type: ((Type,Type),Span)): String =
        mergeSpan(sub_type._2, super_type._2)

    private def mergeSpan(first: Span, second: Span): String =
        if (first.toString < second.toString)
            first.toString + "\n" + second.toString
        else
            second.toString + "\n" + first.toString

    private def toString(ty: ((Type,Type), Span)): String =
        ty._1._1 + " -> " + ty._1._2 + " @ " + ty._2

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
        toOption(param.getIdType) match {
            case Some(ty) => ty
            case _ => InterpreterBug.bug(param,
                                         "Type checking couldn't infer the type of " + param)
        }

    private def error(loc: String, msg: String) =
        errors = errors ::: List(TypeError.make(msg, loc.toString()))
}
