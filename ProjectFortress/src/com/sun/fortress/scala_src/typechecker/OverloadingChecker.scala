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
import scala.collection.Map
import scala.collection.Set
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import com.sun.fortress.compiler.GlobalEnvironment
import com.sun.fortress.compiler.index.CompilationUnitIndex
import com.sun.fortress.compiler.index.ObjectTraitIndex
import com.sun.fortress.compiler.index.TraitIndex
import com.sun.fortress.compiler.index.{Function => JavaFunction}
import com.sun.fortress.compiler.index.{Functional => JavaFunctional}
import com.sun.fortress.compiler.index.{Method => JavaMethod}
import com.sun.fortress.compiler.disambiguator.ExprDisambiguator.HierarchyHistory
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

/* Check the set of overloadings in this compilation unit.
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
class OverloadingChecker(compilation_unit: CompilationUnitIndex,
                         globalEnv: GlobalEnvironment,
                         repository: FortressRepository) {
    var typeAnalyzer = TypeAnalyzer.make(new TraitTable(compilation_unit, globalEnv))
    var errors = List[StaticError]()

    /* Called by com.sun.fortress.compiler.StaticChecker.checkComponent
     *       and com.sun.fortress.compiler.StaticChecker.checkApi
     */
    def checkOverloading(): JavaList[StaticError] = {
        val fnsInComp = compilation_unit.functions
        for ( f <- toSet(fnsInComp.firstSet) ; if isDeclaredName(f) ) {
            checkOverloading(f, toSet(fnsInComp.matchFirst(f)).asInstanceOf[Set[JavaFunctional]])
        }
        val typesInComp = compilation_unit.typeConses
        for ( t <- toSet(typesInComp.keySet) ;
              if NodeUtil.isTraitOrObject(typesInComp.get(t)) ) {
            val traitOrObject = typesInComp.get(t).asInstanceOf[TraitIndex]
            /* All inherited abstract methods in object definitions and
             * object expressions should be defined,
             * with compatible signatures and modifiers.
             */
            if ( compilation_unit.ast.isInstanceOf[Component] &&
                 traitOrObject.isInstanceOf[ObjectTraitIndex] ) {
                val ast = traitOrObject.ast
                val toCheck = inheritedAbstractMethods(toList(NodeUtil.getExtendsClause(ast)))
                for ( t <- toCheck.keySet ) {
                  for ( ds <- toCheck.get(t) ) {
                    for ( d <- ds ) {
                        if ( ! implement(d, toList(NodeUtil.getDecls(ast))) )
                            error(NodeUtil.getSpan(d).toString,
                                  "The inherited abstract method " + d +
                                  " from the trait " + t +
                                  "\n    in the object " + NodeUtil.getName(ast) +
                                  " is not defined in the component " +
                                  compilation_unit.ast.getName + ".")
                     }
                  }
                }
            }

            /* The parameter type of a setter must be the same as the return type
             * of a getter with the same name, if any.
             */
            for ( f <- toSet(traitOrObject.setters.keySet) ) {
                if ( traitOrObject.getters.keySet.contains(f) ) {
                    val getter = traitOrObject.getters.get(f)
                    // Setter declarations are guaranteed to have a single parameter.
                    val param = traitOrObject.setters.get(f).parameters.get(0)
                    val span = getter.getSpan.toString
                    if ( param.getIdType.isSome &&
                         ! typeAnalyzer.equivalent(param.getIdType.unwrap,
                                                   getter.getReturnType).isTrue )
                        error(span,
                              "The parameter type of a setter must be " +
                              "the same as\n    the return type of a getter " +
                              "with the same name, if any.")
                }
            }
            val methods = traitOrObject.dottedMethods
            val staticParameters = new ArrayList[StaticParam]()
            // Add static parameters of the enclosing trait or object
            staticParameters.addAll( traitOrObject.staticParameters )
            // Extend the type analyzer with the collected static parameters
            val oldTypeAnalyzer = typeAnalyzer
            for ( f <- toSet(methods.firstSet) ; if isDeclaredName(f) ) {
                typeAnalyzer = typeAnalyzer.extend(staticParameters, none[WhereClause])
                checkOverloading(f, toSet(methods.matchFirst(f)).asInstanceOf[Set[JavaFunctional]])
            }
            typeAnalyzer = oldTypeAnalyzer
        }
        toJavaList(errors)
    }

    /* Checks the validity of the overloaded function declarations. */
    private def checkOverloading(name: IdOrOpOrAnonymousName,
                                 set: Set[JavaFunctional]) = {
        var signatures = List[((JavaList[StaticParam],Type,Type),Span)]()
        for ( f <- set ; if isDeclaredFunctional(f) ) {
            val result = f.getReturnType
            val param = paramsToType(f.parameters, f.getSpan)
            val sparams = f.staticParameters
            signatures.find(p => p._1 == (sparams,param,result)) match {
                case Some((_,span)) =>
                    error(mergeSpan(span, f.getSpan),
                          "There are multiple declarations of " +
                          name + " with the same type: " +
                          param + " -> " + result)
                case _ =>
                    signatures = ((sparams, param, result), f.getSpan) :: signatures
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
    private def validOverloading(first: ((JavaList[StaticParam],Type,Type),Span),
                                 second: ((JavaList[StaticParam],Type,Type),Span),
                                 set: List[((JavaList[StaticParam],Type,Type),Span)]) =
        subtype(first, second) || subtype(second, first) ||
        exclusion(first, second) || meet(first, second, set)

    /* Checks the overloading rule: subtype */
    private def subtype(f: JavaFunction, g: JavaFunction): Boolean =
        subtype(paramsToType(g.parameters, g.getSpan),
                paramsToType(f.parameters, f.getSpan)) &&
        subtype(f.getReturnType, g.getReturnType)

    private def subtype(sub_type: Type, super_type: Type): Boolean =
        typeAnalyzer.subtype(sub_type, super_type).isTrue

    private def subtype(sub_type: ((JavaList[StaticParam],Type,Type),Span),
                        super_type: ((JavaList[StaticParam],Type,Type),Span)): Boolean = {
        val oldTypeAnalyzer = typeAnalyzer
        // Add static parameters of the method
        val staticParameters = new ArrayList[StaticParam]()
        staticParameters.addAll(sub_type._1._1)
        staticParameters.addAll(super_type._1._1)
        typeAnalyzer = typeAnalyzer.extend(staticParameters, none[WhereClause])
        val result = subtype(super_type._1._2, sub_type._1._2) &&
                     subtype(sub_type._1._3, super_type._1._3)
        typeAnalyzer = oldTypeAnalyzer
        result
    }

    /* Checks the overloading rule: exclusion
     * Invariant: firstParam is not equal to secondParam
     * The following types are not yet supported:
     *     Types tagged with dimensions or units
     *     Effects on arrow types
     *     Keyword parameters and varargs parameters
     *     Intersection types
     *     Union types
     *     Fixed-point types
     */
    private def exclusion(first: ((JavaList[StaticParam],Type,Type),Span),
                          second: ((JavaList[StaticParam],Type,Type),Span)): Boolean = {
        val oldTypeAnalyzer = typeAnalyzer
        // Add static parameters of the method
        val staticParameters = new ArrayList[StaticParam]()
        staticParameters.addAll(first._1._1)
        staticParameters.addAll(second._1._1)
        typeAnalyzer = typeAnalyzer.extend(staticParameters, none[WhereClause])
        val result = new ExclusionOracle(typeAnalyzer,
                                         new ErrorLog()).excludes(first._1._2,
                                                                  second._1._2)
        typeAnalyzer = oldTypeAnalyzer
        result
    }

    /* Checks the overloading rule: meet */
    /* Not yet fully implemented... */
    private def meet(first: ((JavaList[StaticParam],Type,Type),Span),
                     second: ((JavaList[StaticParam],Type,Type),Span),
                     set: List[((JavaList[StaticParam],Type,Type),Span)]) = {
        val oldTypeAnalyzer = typeAnalyzer
        // Add static parameters of the method
        val staticParameters = new ArrayList[StaticParam]()
        staticParameters.addAll(first._1._1)
        staticParameters.addAll(second._1._1)
        typeAnalyzer = typeAnalyzer.extend(staticParameters, none[WhereClause])
        var result = false
        val meet = (reduce(typeAnalyzer.meet(first._1._2, second._1._2)),
                    reduce(typeAnalyzer.meet(first._1._3, second._1._3)))
        for ( f <- set ; if ! result )
            if ( subtype(f._1._2, meet._1) &&
                 subtype(meet._1, f._1._2) &&
                 subtype(meet._2, f._1._3) &&
                 subtype(f._1._3, meet._2))
                result = true
        typeAnalyzer = oldTypeAnalyzer
        result
    }

    private def reduce(t: Type): Type = t match {
        case SIntersectionType(info, elements) =>
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
        val oldTypeAnalyzer = typeAnalyzer
        // Whether "g"'s parameter type is a subtype of "f"'s parameter type
        // and "f"'s return type is a subtype of "g"'s return type
        typeAnalyzer = typeAnalyzer.extend(staticParameters, none[WhereClause])
        val result = subtype(f, g)
        typeAnalyzer = oldTypeAnalyzer
        result
    }

    def isDeclaredName(f: IdOrOpOrAnonymousName) = f match {
        case SId(_,_,str) => IdentifierUtil.validId(str)
        case SOp(_,_,str,_,_) => NodeUtil.validOp(str)
        case _ => false
    }

    def isDeclaredFunctional(f: JavaFunctional) = f match {
        case DeclaredFunction(fd) => true
        case DeclaredMethod(_,_) => true
        case _ => false
    }

    private def mergeSpan(sub_type: ((JavaList[StaticParam],Type,Type),Span),
                          super_type: ((JavaList[StaticParam],Type,Type),Span)): String =
        mergeSpan(sub_type._2, super_type._2)

    private def mergeSpan(first: Span, second: Span): String =
        if (first.toString < second.toString)
            first.toString + ":\n" + second.toString
        else
            second.toString + ":\n" + first.toString

    private def toString(ty: ((JavaList[StaticParam],Type,Type), Span)): String =
        ty._1._2 + " -> " + ty._1._3 + " @ " + ty._2

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
            case _ =>
                toOption(param.getVarargsType) match {
                    case Some(ty) => ty
                    case _ =>
                        val span = NodeUtil.getSpan(param)
                        error(span.toString,
                              "Type checking couldn't infer the type of " + param)
                        NodeFactory.makeVoidType(span)
                }
        }

    private def error(loc: String, msg: String) =
        errors = errors ::: List(TypeError.make(msg, loc.toString))

    /* Returns true if any of the concrete method declarations in "decls"
       implements the abstract method declaration "d".
     */
    private def implement(d: FnDecl, decls: List[Decl]): Boolean =
        decls.exists( (decl: Decl) => decl match {
                      case fd@SFnDecl(_,_,_,_,_) => implement(d, fd)
                      case _ => false
                    } )

    /* Returns true if the concrete method declaration "decl"
       implements the abstract method declaration "d".
     */
    private def implement(d: FnDecl, decl: FnDecl): Boolean =
        NodeUtil.getName(d).asInstanceOf[Id].getText.equals(NodeUtil.getName(decl).asInstanceOf[Id].getText) &&
        NodeUtil.getMods(d).containsAll(NodeUtil.getMods(decl)) &&
        typeAnalyzer.equivalent(NodeUtil.getParamType(d),
                                NodeUtil.getParamType(decl)).isTrue &&
        typeAnalyzer.equivalent(NodeUtil.getReturnType(d).unwrap,
                                NodeUtil.getReturnType(decl).unwrap).isTrue

    private def inheritedAbstractMethods(extended_traits: List[TraitTypeWhere]) =
        inheritedAbstractMethodsHelper(new HierarchyHistory(), extended_traits)

    private def inheritedAbstractMethodsHelper(hist: HierarchyHistory,
                                               extended_traits: List[TraitTypeWhere]):
                                              Map[Id, Set[FnDecl]] = {
        var h = hist
        var map = new HashMap[Id, Set[FnDecl]]()
        for ( trait_ <- extended_traits ; if ! h.hasExplored(trait_.getBaseType) ) {
            trait_.getBaseType match {
                case ty@STraitType(info, name, args, params) =>
                    h = h.explore(ty)
                    val tci = typeAnalyzer.traitTable.typeCons(name)
                    if ( tci.isSome && tci.unwrap.isInstanceOf[TraitIndex] ) {
                        val ti = tci.unwrap.asInstanceOf[TraitIndex]
                        map.put(name, collectAbstractMethods(name, toList(NodeUtil.getDecls(ti.ast))))
                        map ++= inheritedAbstractMethodsHelper(h, toList(ti.extendsTypes))
                    } else error(NodeUtil.getSpan(trait_).toString,
                                 "Trait types are expected in an extends clause but found "
                                 + ty.toStringVerbose + "\n" + tci.getClass)
                case SAnyType(_) =>
                case ty => error(NodeUtil.getSpan(trait_).toString,
                                 "Trait types are expected in an extends clause but found "
                                 + ty.toStringVerbose)
            }
        }
        map
    }

    private def collectAbstractMethods(name: Id, decls: List[Decl]) = {
        val set = new HashSet[FnDecl]
        decls.foreach( (d: Decl) => d match {
                       case fd@SFnDecl(_,SFnHeader(_,mods,_,_,_,_,_,_),_,body,_) =>
                           if ( compilation_unit.typeConses.keySet.contains(name) ) {
                               if ( ! body.isDefined ) set += fd
                           } else if ( mods.isAbstract ) set += fd
                       case _ => })
        set
    }
}
