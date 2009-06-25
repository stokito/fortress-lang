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

package com.sun.fortress.scala_src.typechecker.impls

import edu.rice.cs.plt.collect.IndexedRelation
import edu.rice.cs.plt.collect.Relation
import com.sun.fortress.compiler.disambiguator.ExprDisambiguator.HierarchyHistory
import com.sun.fortress.compiler.index.Method
import com.sun.fortress.compiler.index.ProperTraitIndex
import com.sun.fortress.compiler.index.TraitIndex
import com.sun.fortress.compiler.typechecker.StaticTypeReplacer
import com.sun.fortress.exceptions.InterpreterBug.bug
import com.sun.fortress.exceptions.StaticError.errorMsg
import com.sun.fortress.nodes._
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.typechecker._
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.Sets._
import com.sun.fortress.scala_src.useful.STypesUtil._

/**
 * Provides some helper methods that are common to cases among multiple groups.
 * Note that helpers that are generally used in _every_ case/group should go in
 * the STypeCheckerBase class; the helpers herein each generally apply to cases
 * among only a few groups.
 * 
 * This trait must be mixed in with an STypeCheckerBase instance to provide the
 * full type checker implementation.
 * 
 * (The self-type annotation at the beginning declares that this trait must be
 * mixed into STypeCheckerBase. This is what allows this trait to access its
 * protected members.)
 */
trait Common { self: STypeCheckerBase =>
  
  // TODO: Rewrite this method!
  protected def inheritedMethods(extendedTraits: List[TraitTypeWhere]) = {
    
    // Return all of the methods from super-traits
    def inheritedMethodsHelper(history: HierarchyHistory,
                               extended_traits: List[TraitTypeWhere])
                               : Relation[IdOrOpOrAnonymousName, Method] = {
      var methods = new IndexedRelation[IdOrOpOrAnonymousName, Method](false)
      var done = false
      var h = history
      for ( trait_ <- extended_traits ; if (! done) ) {
        val type_ = trait_.getBaseType
        if ( ! h.hasExplored(type_) ) {
          h = h.explore(type_)
          type_ match {
            case ty@STraitType(_, name, _, params) =>
              toOption(traits.typeCons(name)) match {
                case Some(ti) =>
                  if ( ti.isInstanceOf[TraitIndex] ) {
                    val trait_params = ti.staticParameters
                    val trait_args = ty.getArgs
                    // Instantiate methods with static args
                    val dotted = toSet(ti.asInstanceOf[TraitIndex].dottedMethods).map(t => (t.first, t.second))
                    for ( pair <- dotted ) {
                        methods.add(pair._1,
                                    pair._2.instantiate(trait_params,trait_args).asInstanceOf[Method])
                    }
                    val getters = ti.asInstanceOf[TraitIndex].getters
                    for ( getter <- toSet(getters.keySet) ) {
                        methods.add(getter,
                                    getters.get(getter).instantiate(trait_params,trait_args).asInstanceOf[Method])
                    }
                    val setters = ti.asInstanceOf[TraitIndex].setters
                    for ( setter <- toSet(setters.keySet) ) {
                        methods.add(setter,
                                    setters.get(setter).instantiate(trait_params,trait_args).asInstanceOf[Method])
                    }
                    val paramsToArgs = new StaticTypeReplacer(trait_params, trait_args)
                    val instantiated_extends_types =
                      toList(ti.asInstanceOf[TraitIndex].extendsTypes).map( (t:TraitTypeWhere) =>
                            t.accept(paramsToArgs).asInstanceOf[TraitTypeWhere] )
                    methods.addAll(inheritedMethodsHelper(h, instantiated_extends_types))
                  } else done = true
                case _ => done = true
              }
            case _ => done = true
          }
        }
      }
      methods
    }
    inheritedMethodsHelper(new HierarchyHistory(), extendedTraits)
  }

  /**
   * Identical to the overloading but with an explicitly given list of static
   * parameters.
   */
  protected def staticInstantiation(sargs: List[StaticArg],
                                    sparams: List[StaticParam],
                                    body: Type): Option[Type] = {

    // Check that the args match.
    if (!staticArgsMatchStaticParams(sargs, sparams)) return None

    // Create mapping from parameter names to static args.
    val paramMap = Map(sparams.map(_.getName).zip(sargs): _*)

    // Gets the actual value out of a static arg.
    def sargToVal(sarg: StaticArg): Node = sarg match {
      case sarg:TypeArg => sarg.getTypeArg
      case sarg:IntArg => sarg.getIntVal
      case sarg:BoolArg => sarg.getBoolArg
      case sarg:OpArg => sarg.getName
      case sarg:DimArg => sarg.getDimArg
      case sarg:UnitArg => sarg.getUnitArg
      case _ => bug("unexpected kind of static arg")
    }

    // Replaces all the params with args in a node.
    object staticReplacer extends Walker {
      override def walk(node: Any): Any = node match {
        case n:VarType => paramMap.get(n.getName).map(sargToVal).getOrElse(n)
        // TODO: Check proper name for OpArgs.
        case n:OpArg => paramMap.get(n.getName.getOriginalName).getOrElse(n)
        case n:IntRef => paramMap.get(n.getName).map(sargToVal).getOrElse(n)
        case n:BoolRef => paramMap.get(n.getName).map(sargToVal).getOrElse(n)
        case n:DimRef => paramMap.get(n.getName).map(sargToVal).getOrElse(n)
        case n:UnitRef => paramMap.get(n.getName).map(sargToVal).getOrElse(n)
        case _ => super.walk(node)
      }
    }

    // Get the replaced type and clear out its static params, if any.
    Some(clearStaticParams(staticReplacer(body).asInstanceOf[Type]))
  }

  /**
   * Instantiates a generic type with some static arguments. The static
   * parameters are retrieved from the body type and replaced inside body with
   * their corresponding static arguments. In the end, any static parameters
   * in the replaced type will be cleared.
   *
   * @param args A list of static arguments to apply to the generic type body.
   * @param body The generic type whose static parameters are to be replaced.
   * @return An option of a type identical to body but with every occurrence of
   *         one of its declared static parameters replaced by corresponding
   *         static args. If None, then the instantiation failed.
   */
  protected def staticInstantiation(sargs: List[StaticArg],
                                    body: Type): Option[Type] =
    staticInstantiation(sargs, getStaticParams(body), body)

  /**
   * Determines if the kinds of the given static args match those of the static
   * parameters. In the case of type arguments, the type is checked to be a
   * subtype of the corresponding type parameter's bounds.
   */
  protected def staticArgsMatchStaticParams(args: List[StaticArg],
                                            params: List[StaticParam]): Boolean = {
    
    if (args.length != params.length) return false

    // Match a single pair.
    def argMatchesParam(argAndParam: (StaticArg, StaticParam)): Boolean = {
      val (arg, param) = argAndParam
      (arg, param.getKind) match {
        case (STypeArg(_, argType), SKindType(_)) =>
            toList(param.getExtendsClause).forall((bound:Type) =>
              isSubtype(argType, bound, arg,
                        errorMsg(normalize(argType), " not a subtype of ", normalize(bound))))
        case (SIntArg(_, _), SKindInt(_)) => true
        case (SBoolArg(_, _), SKindBool(_)) => true
        case (SDimArg(_, _), SKindDim(_)) => true
        case (SOpArg(_, _), SKindOp(_)) => true
        case (SUnitArg(_, _), SKindUnit(_)) => true
        case (SIntArg(_, _), SKindNat(_)) => true
        case (_, _) => false
      }
    }

    // Match every pair.
    args.zip(params).forall(argMatchesParam)
  }

}
