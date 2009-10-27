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
import edu.rice.cs.plt.collect.Relation
import edu.rice.cs.plt.tuple.{Option => JavaOption}
import edu.rice.cs.plt.tuple.{Pair => JavaPair}
import com.sun.fortress.compiler.GlobalEnvironment
import com.sun.fortress.compiler.index.CompilationUnitIndex
import com.sun.fortress.compiler.index.ComponentIndex
import com.sun.fortress.compiler.index.ObjectTraitIndex
import com.sun.fortress.compiler.index.ProperTraitIndex
import com.sun.fortress.compiler.index.TraitIndex
import com.sun.fortress.compiler.index.{Function => JavaFunction}
import com.sun.fortress.compiler.index.{Functional => JavaFunctional}
import com.sun.fortress.compiler.index.{Method => JavaMethod}
import com.sun.fortress.compiler.index.{Variable => JavaVariable}
import com.sun.fortress.compiler.typechecker.StaticTypeReplacer
import com.sun.fortress.exceptions.InterpreterBug
import com.sun.fortress.exceptions.StaticError
import com.sun.fortress.exceptions.TypeError
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.MultiSpan
import com.sun.fortress.nodes_util.NodeFactory
import com.sun.fortress.nodes_util.NodeUtil
import com.sun.fortress.nodes_util.Span
import com.sun.fortress.parser_util.IdentifierUtil
import com.sun.fortress.scala_src.types.TypeAnalyzer
import com.sun.fortress.scala_src.useful._
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.STypesUtil._
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
                         globalEnv: GlobalEnvironment) {
    var typeAnalyzer = TypeAnalyzer.make(new TraitTable(compilation_unit, globalEnv))
    var errors = List[StaticError]()

    private def getFunctions(index: CompilationUnitIndex,
                             f: IdOrOpOrAnonymousName)
                            : Set[((JavaList[StaticParam],Type,Type), Span)] = {
      val fns = toFunctionSig(toSet(index.functions.matchFirst(f)).asInstanceOf[Set[JavaFunctional]])
      if ( index.variables.keySet.contains(f) )
        index.variables.get(f) match {
          case DeclaredVariable(lvalue)
               if lvalue.getIdType.unwrap.isInstanceOf[ArrowType] =>
            val ty = lvalue.getIdType.unwrap.asInstanceOf[ArrowType]
            fns.+(((new ArrayList[StaticParam](), ty.getDomain, ty.getRange),
                   NodeUtil.getSpan(lvalue)))
          case _ => fns
        }
      else fns
    }

    // for functions
    private def toFunctionSig(set: Set[JavaFunctional])
                             : Set[((JavaList[StaticParam],Type,Type), Span)] =
      set.filter(isDeclaredFunction(_))
         .map(f => ((f.staticParameters,
                     paramsToType(f.parameters, f.getSpan),
                     f.getReturnType.unwrap),
                    f.getSpan))

    // for functional methods
    private def toFunctionalMethodSig(set: Set[(JavaFunction, StaticTypeReplacer)])
                                     : Set[((JavaList[StaticParam],Type,Type), Span)] =
      set.filter(p => isFunctionalMethod(p._1))
         .map(p => {
              val (f, replacer) = p
              ((f.staticParameters,
                replacer.replaceIn(paramsToType(f.parameters, f.getSpan)),
                replacer.replaceIn(f.getReturnType.unwrap)),
               f.getSpan)})

    // for dotted methods
    private def toMethodSig(set: Set[(JavaMethod, StaticTypeReplacer)])
                           : Set[((JavaList[StaticParam],Type,Type), Span)] =
      set.filter(p => isDeclaredMethod(p._1))
         .map(p => {
              val (f, replacer) = p
              ((f.staticParameters,
                replacer.replaceIn(paramsToType(f.selfType.unwrap, f.parameters, f.getSpan)),
                replacer.replaceIn(f.getReturnType.unwrap)),
               f.getSpan)})

    /* Called by com.sun.fortress.compiler.StaticChecker.checkComponent
     *       and com.sun.fortress.compiler.StaticChecker.checkApi
     */
    def checkOverloading(): JavaList[StaticError] = {
        val fnsInComp = compilation_unit.functions
        val ast = compilation_unit.ast
        val importStars = toList(ast.getImports).filter(_.isInstanceOf[ImportStar]).map(_.asInstanceOf[ImportStar])
        val importNames = toList(ast.getImports).filter(_.isInstanceOf[ImportNames]).map(_.asInstanceOf[ImportNames])
        for ( f <- toSet(fnsInComp.firstSet) ; if isDeclaredName(f) ) {
          val name = f.asInstanceOf[IdOrOp].getText
          var set = getFunctions(compilation_unit, f)
          for ( i <- importNames ) {
            for ( n <- toList(i.getAliasedNames) ) {
              if ( n.getAlias.isSome &&
                   n.getAlias.unwrap.asInstanceOf[IdOrOp].getText.equals(name) ) {
                // get the JavaFunctionals from the api and add them to set
                set ++= getFunctions(globalEnv.lookup(i.getApiName), n.getName)
              } else if ( n.getAlias.isNone &&
                          n.getName.asInstanceOf[IdOrOp].getText.equals(name) ) {
                // get the JavaFunctionals from the api and add them to set
                set ++= getFunctions(globalEnv.lookup(i.getApiName), f)
              }
            }
          }
          for ( i <- importStars ) {
            val index = globalEnv.lookup(i.getApiName)
            val excepts = toList(i.getExceptNames).asInstanceOf[List[IdOrOp]]
            for ( n <- toSet(index.functions.firstSet).++(toSet(index.variables.keySet)) ) {
              val text = n.asInstanceOf[IdOrOp].getText
              if ( text.equals(name) &&
                   ! excepts.contains((m:IdOrOp) => m.getText.equals(text)) ) {
                // get the JavaFunctionals from the api and add them to set
                set ++= getFunctions(index, f)
              }
            }
          }
          checkFunctionOverloading(f, set)
        }
        /* All inherited abstract methods in object expressions should be defined,
         * with compatible signatures and modifiers.
         */
        if ( compilation_unit.isInstanceOf[ComponentIndex] )
          errors = errors ::: new AbstractMethodChecker(compilation_unit.asInstanceOf[ComponentIndex],
                                                        globalEnv).check()

        val typesInComp = compilation_unit.typeConses
        for ( t <- toSet(typesInComp.keySet) ;
              if NodeUtil.isTraitOrObject(typesInComp.get(t)) ) {
            val traitOrObject = typesInComp.get(t).asInstanceOf[TraitIndex]
            // Extend the type analyzer with the collected static parameters
            val oldTypeAnalyzer = typeAnalyzer
            // Add static parameters of the enclosing trait or object
            typeAnalyzer = typeAnalyzer.extend(toList(traitOrObject.staticParameters),
                                               None)
            /* The parameter type of a setter must be the same as the return type
             * of a getter with the same name, if any.
             */
            for ( f <- toSet(traitOrObject.setters.keySet) ) {
                if ( traitOrObject.getters.keySet.contains(f) ) {
                    val getter = traitOrObject.getters.get(f)
                    // Setter declarations are guaranteed to have a single parameter.
                    val param = traitOrObject.setters.get(f).parameters.get(0)
                    val span = getter.getSpan
                    if ( param.getIdType.isSome &&
                         ! typeAnalyzer.equivalent(param.getIdType.unwrap,
                                                   getter.getReturnType.unwrap).isTrue )
                        error(span,
                              "The parameter type of a setter must be " +
                              "the same as\n    the return type of a getter " +
                              "with the same name, if any.")
                }
            }
            val identity = new StaticTypeReplacer(new ArrayList[StaticParam](),
                                                  new ArrayList[StaticArg]())
            var methods = toSet(traitOrObject.dottedMethods)
                          .asInstanceOf[Set[JavaPair[IdOrOpOrAnonymousName, JavaFunctional]]]
                          .map(p => new JavaPair(p.first, (p.second, identity)))
            methods ++=
              STypesUtil.inheritedMethods(typeAnalyzer.traits,
                                          toList(traitOrObject.extendsTypes),
                                          methods, typeAnalyzer)
              .asInstanceOf[Set[JavaPair[IdOrOpOrAnonymousName, (JavaFunctional, StaticTypeReplacer)]]]

            for ( f <- methods.map(x => x.first) ; if isDeclaredName(f) ) {
              var ss = Set[(JavaMethod, StaticTypeReplacer)]()
              methods.filter(p => (p.first == f && p.second._1.isInstanceOf[JavaMethod]) &&
                             p.second._1.asInstanceOf[JavaMethod].selfType.isSome)
                     .foreach(ss += _.second.asInstanceOf[(JavaMethod, StaticTypeReplacer)])
              checkMethodOverloading(f, toMethodSig(ss))
            }
            for ( f <- methods.map(x => x.first) ; if isDeclaredName(f) ) {
              var ss = Set[(JavaFunction, StaticTypeReplacer)]()
              methods.filter(p => p.first == f && p.second._1.isInstanceOf[JavaFunction])
                     .foreach(ss += _.second.asInstanceOf[(JavaFunction, StaticTypeReplacer)])
              checkFunctionOverloading(f, toFunctionalMethodSig(ss))
            }
            typeAnalyzer = oldTypeAnalyzer
        }
        toJavaList(errors)
    }

    /* Checks the validity of the overloaded method declarations. */
    private def checkMethodOverloading(name: IdOrOpOrAnonymousName,
                                       set: Set[((JavaList[StaticParam],Type,Type), Span)])
                                      : Unit =
      checkOverloading(name, set, true)

    /* Checks the validity of the overloaded function declarations. */
    private def checkFunctionOverloading(name: IdOrOpOrAnonymousName,
                                         set: Set[((JavaList[StaticParam],Type,Type), Span)])
                                        : Unit =
      checkOverloading(name, set, false)

    /* Checks the validity of the overloaded function declarations. */
    private def checkOverloading(name: IdOrOpOrAnonymousName,
                                 set: Set[((JavaList[StaticParam],Type,Type), Span)],
                                 isMethod: Boolean)
                                : Unit = set.size match {
      case 0 =>
      case 1 =>
      case _ =>
        var signatures = List[((JavaList[StaticParam],Type,Type),Span)]()
        for ( sig@((sparams,param,result), span) <- set ) {
          signatures.find(p => p._1 == sig._1) match {
            case Some((_,sp)) =>
              error(mergeSpan(sp, span),
                    "There are multiple declarations of " +
                    name + " with the same type: " +
                    (if (isMethod) dropReceiver(param) else param) +
                    " -> " + result)
            case _ =>
              // A functional which takes a single parameter of a parametric type
              // bound by Any cannot be overloaded.
              if ( checkBoundAny(param, toList(sparams)) ) {
                error(span, "A functional which takes a single parameter " +
                      "of a parametric type bound by Any\n    cannot be overloaded.")
                return
              }
              signatures = sig :: signatures
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

    /* A functional which takes a single parameter of a parametric type
     * bound by Any cannot be overloaded.
     */
    private def checkBoundAny(param: Type, sparams: List[StaticParam]) =
      if (param.isInstanceOf[VarType]) {
        val tyName = param.asInstanceOf[VarType].getName.getText
        sparams.exists(sp =>
                       sp.getName.getText.equals(tyName) &&
                       sp.getExtendsClause.size == 1 &&
                       sp.getExtendsClause.get(0).isInstanceOf[AnyType])
      } else false

    /* Checks the overloading rules: subtype / exclusion / meet */
    private def validOverloading(first: ((JavaList[StaticParam],Type,Type),Span),
                                 second: ((JavaList[StaticParam],Type,Type),Span),
                                 set: List[((JavaList[StaticParam],Type,Type),Span)]) =
        subtype(first, second) || subtype(second, first) ||
        exclusion(first, second) || meet(first, second, set)

    /* Checks the overloading rule: subtype */
    private def subtype(sub_type: Type, super_type: Type): Boolean =
        typeAnalyzer.subtype(sub_type, super_type).isTrue

    private def subtype(sub_type: ((JavaList[StaticParam],Type,Type),Span),
                        super_type: ((JavaList[StaticParam],Type,Type),Span)): Boolean = {
        val oldTypeAnalyzer = typeAnalyzer
        // Add static parameters of the method
        val staticParameters = new ArrayList[StaticParam]()
        staticParameters.addAll(sub_type._1._1)
        staticParameters.addAll(super_type._1._1)
        typeAnalyzer = typeAnalyzer.extend(toList(staticParameters), None)
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
        typeAnalyzer = typeAnalyzer.extend(toList(staticParameters), None)
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
        typeAnalyzer = typeAnalyzer.extend(toList(staticParameters), None)
        var result = false
        val exclusionOracle = new ExclusionOracle(typeAnalyzer, new ErrorLog())
        val meet = (reduce(typeAnalyzer.meet(first._1._2, second._1._2), exclusionOracle),
                    reduce(typeAnalyzer.meet(first._1._3, second._1._3), exclusionOracle))
        for ( f <- set ; if ! result )
            if ( subtype(f._1._2, meet._1) &&
                 subtype(meet._1, f._1._2) &&
                 subtype(meet._2, f._1._3) &&
                 subtype(f._1._3, meet._2))
                result = true
        typeAnalyzer = oldTypeAnalyzer
        result
    }

    /* If "t" is an intersection type of "elements",
     *  - if there are tuple types and non-tuple types in "elements"
     *    return BottomType
     *  - if "elements" are all non-tuple types
     *    check comprises clauses
     *  - if "elements" are all tuple types
     *    simplify "t"
     */
    private def reduce(t: Type, exclusionOracle: ExclusionOracle): Type = t match {
        case SIntersectionType(info, elements) =>
            val (tuples, nots) = elements.partition(NodeUtil.isTupleType)
            if ( ! tuples.isEmpty && ! nots.isEmpty ) NodeFactory.makeBottomType(info)
            else if ( tuples.isEmpty ) {
                // If all the "elements" have comprises clauses
                // and they all include type "M", then "M" is the meet.
                existComprisedMeet(elements, exclusionOracle) match {
                    case Some(m) => m
                    case _ => t
                }
            } else {
                val size = NodeUtil.getTupleTypeSize(tuples.head)
                if ( tuples.forall(ty => NodeUtil.getTupleTypeSize(ty) == size &&
                                         ! NodeUtil.hasVarargs(ty) &&
                                         ! NodeUtil.hasKeywords(ty)) ) {
                    var elems = List[Type]()
                    var i = 0
                    while ( i < size ) {
                        elems = elems ::: List(typeAnalyzer.meet(tuples.map(ty => NodeUtil.getTupleTypeElem(ty, i))))
                        i += 1
                    }
                    val mt = NodeFactory.makeTupleType(NodeUtil.getSpan(t), toJavaList(elems))
                    typeAnalyzer.normalize(mt)
                } else t
            }
        case _ => t
    }

    /* If all the "types" have comprises clauses
     * and they all include types "comprised",
     * then if "comprised" = {"M"} and all the others are exclusive
     *      then "M" is the meet
     */
    private def existComprisedMeet(types: List[Type],
                                   exclusionOracle: ExclusionOracle): Option[TraitType] = {
      var allComprises = List[(Id, List[TraitType])]()
      var meets: List[TraitType] = null
      for ( t <- types ) {
        if ( t.isInstanceOf[TraitType] ) {
          val name = t.asInstanceOf[TraitType].getName
          STypesUtil.getTypes(name, globalEnv, compilation_unit) match {
            case ti:ProperTraitIndex =>
              var comprises = List[TraitType]()
              for ( s <- toSet(ti.comprisesTypes) ) {
                if (!s.isInstanceOf[TraitType]) return None
                else comprises = s.asInstanceOf[TraitType]::comprises
              }
              allComprises = (name, comprises)::allComprises
              if ( ! comprises.isEmpty ) {
                if ( meets == null ) meets = comprises
                else meets = meets.intersect(comprises)
              } else return None // no comprises clause
            case _ => return None // not a trait
          }
        } else return None
      }
      if ( meets.length == 1 ) {
        val meet = meets.head
        def nonExclusive(pair: (List[TraitType], List[TraitType])): Boolean = {
          for ( first <- pair._1 - meet ) {
            for ( second <- pair._2 - meet ) {
              if ( typeAnalyzer.equivalent(first, second).isFalse &&
                   ! exclusionOracle.excludes(first, second) )
                return true
            }
          }
          false
        }
        for ( first <- allComprises ) {
          for ( second <- allComprises ;
                if ! first._1.getText.equals(second._1.getText) ) {
            if ( nonExclusive(first._2, second._2) ) return None
          }
        }
        Some(meet)
      } else  None
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
            val ind = typeAnalyzer.traits.typeCons(NodeUtil.getDeclaringTrait(f))
            if ( ind.isSome && NodeUtil.isTraitOrObject(ind.unwrap) ) {
                staticParameters.addAll( NodeUtil.getStaticParameters(ind.unwrap) )
            }
        }
        // If "g" is a functional method,
        // add static parameters of "g"'s enclosing trait or object
        if ( NodeUtil.isFunctionalMethod(g) ) {
            val ind = typeAnalyzer.traits.typeCons(NodeUtil.getDeclaringTrait(g))
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
        typeAnalyzer = typeAnalyzer.extend(toList(staticParameters), None)
        val result = subtype(paramsToType(g.parameters, g.getSpan),
                             paramsToType(f.parameters, f.getSpan)) &&
                     subtype(f.getReturnType.unwrap, g.getReturnType.unwrap)
        typeAnalyzer = oldTypeAnalyzer
        result
    }

    /* Drop the first element denoting the receiver type of the given type. */
    private def dropReceiver(param: Type) = param match {
      case STupleType(info, _::ty::Nil, None, Nil) => ty
      case STupleType(info, elements, varargs, keywords) =>
        STupleType(info, elements.takeRight(elements.size-1), varargs, keywords)
      case _ => NodeFactory.makeVoidType(NodeUtil.getSpan(param))
    }

    def isDeclaredName(f: IdOrOpOrAnonymousName) = f match {
        case SId(_,_,str) => IdentifierUtil.validId(str)
        case SOp(_,_,str,_,_) => NodeUtil.validOp(str)
        case _ => false
    }

    def isDeclaredMethod(f: JavaFunctional) = f match {
        case DeclaredMethod(_,_) => true
        case _ => false
    }

    def isDeclaredFunction(f: JavaFunctional) = f match {
        case DeclaredFunction(_) => true
        case _ => false
    }

    def isFunctionalMethod(f: JavaFunctional) = f match {
        case FunctionalMethod(_,_) => true
        case _ => false
    }

    def isDeclaredVariable(v: JavaVariable) = v match {
        case DeclaredVariable(_) => true
        case _ => false
    }

    private def mergeSpan(sub_type: ((JavaList[StaticParam],Type,Type),Span),
                          super_type: ((JavaList[StaticParam],Type,Type),Span)): Span =
        mergeSpan(sub_type._2, super_type._2)

    private def mergeSpan(first: Span, second: Span): Span = new MultiSpan(first, second)

    private def toString(ty: ((JavaList[StaticParam],Type,Type), Span)): String =
        ty._1._2 + " -> " + ty._1._3 + " @ " + ty._2

    /* Returns the type of the given list of parameters. */
    private def paramsToType(params: JavaList[Param], span: Span): Type =
      STypesUtil.paramsToType(toList(params), span) match {
        case Some(ty) => ty
        case _ =>
          val span = NodeUtil.spanAll(params)
          error(span,
                "Type checking couldn't infer the type of " + params)
          NodeFactory.makeVoidType(span)
      }

    /* Returns the type of the given self type and a list of parameters. */
    private def paramsToType(self: Type, params: JavaList[Param], span: Span): Type = {
      val span = params.size match {
        case 0 => NodeUtil.getSpan(self)
        case _ => NodeUtil.spanAll(params)
      }
      val elems = toList(params).map(paramToType)
      if (elems.forall(_.isDefined))
        NodeFactory.makeTupleType(span, toJavaList(List(self) ++ elems.map(_.get)))
      else {
        error(span,
              "Type checking couldn't infer the type of " + params)
        NodeFactory.makeVoidType(span)
      }
    }

    private def error(loc: Span, msg: String) =
        errors = errors ::: List(TypeError.make(msg, loc))

}
