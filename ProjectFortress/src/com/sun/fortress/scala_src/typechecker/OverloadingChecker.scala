/*******************************************************************************
    Copyright 2009,2012, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.scala_src.typechecker

import _root_.java.util.ArrayList
import _root_.java.util.{List => JavaList}
import _root_.java.util.{Set => JavaSet}
import edu.rice.cs.plt.collect.Relation
import edu.rice.cs.plt.tuple.{Option => JavaOption}
import edu.rice.cs.plt.tuple.{Pair => JavaPair}
import edu.rice.cs.plt.collect.Relation
import edu.rice.cs.plt.collect.IndexedRelation
import com.sun.fortress.compiler.GlobalEnvironment
import com.sun.fortress.compiler.index.ApiIndex
import com.sun.fortress.compiler.index.CompilationUnitIndex
import com.sun.fortress.compiler.index.ComponentIndex
import com.sun.fortress.compiler.index.ObjectTraitIndex
import com.sun.fortress.compiler.index.ProperTraitIndex
import com.sun.fortress.compiler.index.TraitIndex
import com.sun.fortress.compiler.index.{Function => JavaFunction}
import com.sun.fortress.compiler.index.{Functional => JavaFunctional}
import com.sun.fortress.compiler.index.{FunctionalMethod => JavaFunctionalMethod}
import com.sun.fortress.compiler.index.{Method => JavaMethod}
import com.sun.fortress.compiler.index.{Variable => JavaVariable}
import com.sun.fortress.compiler.index.{HasSelfType => JavaHasSelfType}
import com.sun.fortress.compiler.index.DeclaredFunction
import com.sun.fortress.compiler.typechecker.StaticTypeReplacer
import com.sun.fortress.exceptions.InterpreterBug
import com.sun.fortress.exceptions.InterpreterBug.bug
import com.sun.fortress.exceptions.StaticError
import com.sun.fortress.exceptions.TypeError
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.MultiSpan
import com.sun.fortress.nodes_util.NodeFactory
import com.sun.fortress.nodes_util.{NodeFactory => NF}
import com.sun.fortress.nodes_util.{NodeUtil => NU}
import com.sun.fortress.nodes_util.Span
import com.sun.fortress.parser_util.IdentifierUtil
import com.sun.fortress.scala_src.overloading.OverloadingOracle
import com.sun.fortress.scala_src.typechecker.Formula._
import com.sun.fortress.scala_src.types.TypeAnalyzer
import com.sun.fortress.scala_src.useful._
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.STypesUtil._
import com.sun.fortress.scala_src.useful.Sets._
import com.sun.fortress.scala_src.nodes._

/* Check the set of overloadings in this compilation unit:
 *
 * For every trait and object, check dotted methods and functional methods, taking inheritance into account
 * Check top-level functions, taking functional methods and top-level variables into account
 * Check local functions, taking local variables into account
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
    val globalOracle = new OverloadingOracle()(typeAnalyzer)
    var errors = List[StaticError]()

    private def getFunctionsFromCompilationUnit(index: CompilationUnitIndex,
                                                f: IdOrOpOrAnonymousName)
        : List[(ArrowType,Option[Int])] =
      getFunctions(index, f, compilation_unit.isInstanceOf[ComponentIndex])

    private def getFunctions(index: CompilationUnitIndex,
                             f: IdOrOpOrAnonymousName)
        : List[(ArrowType,Option[Int])] =
      getFunctions(index, f, false)

    private def getFunctions(index: CompilationUnitIndex,
                             f: IdOrOpOrAnonymousName,
                             onlyConcrete: Boolean)
        : List[(ArrowType,Option[Int])] = {
      val fndecls = toSet(index.functions.matchFirst(f)).asInstanceOf[Set[JavaFunctional]]
      val fns = toFunctionArrows(fndecls, onlyConcrete)
      if ( index.variables.keySet.contains(f) )
        index.variables.get(f) match {
          case SDeclaredVariable(lvalue)
               if lvalue.getIdType.unwrap.isInstanceOf[ArrowType] =>
            fns :+ ((lvalue.getIdType.unwrap.asInstanceOf[ArrowType], None))
          case _ => fns
        }
      else fns
    }

    private def getFunctionalMethods(index: CompilationUnitIndex, f: IdOrOpOrAnonymousName): List[(ArrowType,Option[Int])] = {
      
        val fndecls = toSet(index.functions.matchFirst(f)).asInstanceOf[Set[JavaFunctional]]
        val fndeclsfiltered = fndecls.filter(f => f.isInstanceOf[JavaFunctionalMethod])
        toFunctionArrows(fndeclsfiltered, false)

    }
    
    private def getDeclaredFunctions(index: CompilationUnitIndex, f: IdOrOpOrAnonymousName): List[(ArrowType,Option[Int])] = {
      
        val fndecls = toSet(index.functions.matchFirst(f)).asInstanceOf[Set[JavaFunctional]]
        val fndeclsfiltered = fndecls.filter(f => f.isInstanceOf[DeclaredFunction])
        toFunctionArrows(fndeclsfiltered, false)

    }    
    
    private def toFunctionArrows(set: Set[JavaFunctional], onlyConcrete: Boolean):
        List[(ArrowType,Option[Int])] =
      set.filter(s => isFunction(s) &&
                      (!onlyConcrete || s.asInstanceOf[JavaFunctional].body.isSome))
         .toList.map(f => (NF.makeArrowType(f.getSpan,
				     paramsToType(f.parameters, f.getSpan),
				     f.getReturnType.unwrap,
				     getStaticParameters(f, true)),
	            if (isFunctionalMethod(f))
                      Some(f.asInstanceOf[JavaFunctionalMethod].selfPosition)
                    else None))

    // for functional methods
    private def toFunctionalMethodArrows(set: Set[(JavaFunctional, StaticTypeReplacer, TraitType)]):
        List[(ArrowType,Option[Int])] =
      set.filter(p => p match { case (f, _, _) => isFunctionalMethod(f) && f.asInstanceOf[JavaFunction].body.isSome})
         .toList.map(p => p match { case (f, replacer, _) =>
              //TODO: Figure out whether I should get the lifted parameters or not. The original code DID.  I think the answer is NO. 2/21/2012
              (NF.makeArrowType(f.getSpan,
                                replacer.replaceIn(paramsToType(f.parameters, f.getSpan)),
                                replacer.replaceIn(f.getReturnType.unwrap),
				getStaticParameters(f, false)),
               Some(f.asInstanceOf[JavaFunctionalMethod].selfPosition))})

    // for dotted methods
    private def toMethodArrows(set: Set[(JavaFunctional, StaticTypeReplacer, TraitType)])
                           : List[(ArrowType,Option[Int])] =
      set.filter(p => p match { case (f, _, _) => isDeclaredMethod(f)})
         .toList.map(p => p match { case (f, replacer, _) =>
              (NF.makeArrowType(f.getSpan,
	                        replacer.replaceIn(paramsToType(f.asInstanceOf[JavaMethod].selfType.unwrap, f.parameters, f.getSpan)),
                		replacer.replaceIn(f.getReturnType.unwrap),
				getStaticParameters(f, false)),
               None)})


    /* Called by com.sun.fortress.compiler.StaticChecker.checkComponent
     *       and com.sun.fortress.compiler.StaticChecker.checkApi
     */
    def checkOverloading(): JavaList[StaticError] = {
        val fnsInComp = compilation_unit.functions
        val functions = fnsInComp.firstSet
        val ast = compilation_unit.ast
        val kindAndName = (compilation_unit match { case _:ComponentIndex => "component "; case _:ApiIndex => "API " }) + compilation_unit.name
        val importItems = toListFromImmutable(ast.getImports)
        val importStars = importItems.filter(_.isInstanceOf[ImportStar]).map(_.asInstanceOf[ImportStar])
        val importNames = importItems.filter(_.isInstanceOf[ImportNames]).map(_.asInstanceOf[ImportNames])

        // First check names of functions declared in this component.
        for ( f <- toSet(functions) ; if isDeclaredName(f) ) {
          val name = f.asInstanceOf[IdOrOp].getText
          var set = getFunctionsFromCompilationUnit(compilation_unit, f)

          for ( i <- importNames ) {
            for ( n <- toListFromImmutable(i.getAliasedNames) ) {
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
            val excepts = toListFromImmutable(i.getExceptNames).asInstanceOf[List[IdOrOp]]
            for ( n <- toSet(index.functions.firstSet).++(toSet(index.variables.keySet)) ) {
              val text = n.asInstanceOf[IdOrOp].getText
              if ( text.equals(name) &&
                   ! excepts.contains((m:IdOrOp) => m.getText.equals(text)) ) {
                // get the JavaFunctionals from the api and add them to set
                set ++= getFunctions(index, f)
              }
            }
          }
          
          // debugging probe: TypeError.b3(name, f, set)
          checkFunctionOverloading(kindAndName, f, set, globalOracle)
          
          val dFunctions = getDeclaredFunctions(compilation_unit, f)

          var fMethods = getFunctionalMethods(compilation_unit, f)

          for ( i <- importNames ) {
            for ( n <- toListFromImmutable(i.getAliasedNames) ) {
              if ( n.getAlias.isSome &&
                   n.getAlias.unwrap.asInstanceOf[IdOrOp].getText.equals(name) ) {
                // get the JavaFunctionals from the api and add them to set
                fMethods ++= getFunctionalMethods(globalEnv.lookup(i.getApiName), n.getName)
              } else if ( n.getAlias.isNone &&
                          n.getName.asInstanceOf[IdOrOp].getText.equals(name) ) {
                // get the JavaFunctionals from the api and add them to set
                fMethods ++= getFunctionalMethods(globalEnv.lookup(i.getApiName), f)
              }
            }
          }
          
          for ( i <- importStars ) {
            val index = globalEnv.lookup(i.getApiName)
            val excepts = toListFromImmutable(i.getExceptNames).asInstanceOf[List[IdOrOp]]
            for ( n <- toSet(index.functions.firstSet).++(toSet(index.variables.keySet)) ) {
              val text = n.asInstanceOf[IdOrOp].getText
              if ( text.equals(name) &&
                   ! excepts.contains((m:IdOrOp) => m.getText.equals(text)) ) {
                // get the JavaFunctionals from the api and add them to set
                fMethods ++= getFunctionalMethods(index, f)
              }
            }
          }          
          
          checkFancyRule(f, dFunctions, fMethods, globalOracle)
          
        }
        
        // Overloading checking for imported functionals that are not declared in this component
        for ( f <- importNames ) {
          for ( g <- toListFromImmutable(f.getAliasedNames);
                if (g.getAlias.isSome && ! toSet(functions).contains(g.getAlias.unwrap)) ||
                   ! toSet(functions).contains(g.getName) ) {
            val name = if (g.getAlias.isSome) g.getAlias.unwrap.asInstanceOf[IdOrOp].getText
                       else g.getName.asInstanceOf[IdOrOp].getText
            var set = getFunctions(globalEnv.lookup(f.getApiName), g.getName)
            for ( i <- importNames ; if ! f.getApiName.getText.equals(i.getApiName.getText)) {
              for ( n <- toListFromImmutable(i.getAliasedNames) ) {
                if ( n.getAlias.isSome &&
                     n.getAlias.unwrap.asInstanceOf[IdOrOp].getText.equals(name) ) {
                  // get the JavaFunctionals from the api and add them to set
                  set ++= getFunctions(globalEnv.lookup(i.getApiName), n.getName)
                } else if ( n.getAlias.isNone &&
                            n.getName.asInstanceOf[IdOrOp].getText.equals(name) ) {
                  // get the JavaFunctionals from the api and add them to set
                  set ++= getFunctions(globalEnv.lookup(i.getApiName), g.getName)
                }
              }
            }
            for ( i <- importStars ) {
              val index = globalEnv.lookup(i.getApiName)
              val excepts = toListFromImmutable(i.getExceptNames).asInstanceOf[List[IdOrOp]]
              for ( n <- toSet(index.functions.firstSet).++(toSet(index.variables.keySet)) ) {
                val text = n.asInstanceOf[IdOrOp].getText
                if ( text.equals(name) &&
                     ! excepts.contains((m:IdOrOp) => m.getText.equals(text)) ) {
                  // get the JavaFunctionals from the api and add them to set
                  set ++= getFunctions(index, g.getName)
                }
              }
            }
            checkFunctionOverloading(kindAndName, g.getName, set, globalOracle)
          }
        }

        // All inherited abstract methods in objects should have implementations.
        if ( compilation_unit.isInstanceOf[ComponentIndex] )
          errors = errors ::: new AbstractMethodChecker(compilation_unit.asInstanceOf[ComponentIndex],
                                                        globalEnv).check()

        // Now check trait and object declarations.
        val typesInComp = compilation_unit.typeConses
        for ( t <- toSet(typesInComp.keySet) ; if NU.isTraitOrObject(typesInComp.get(t)) ) {
            val traitOrObject = typesInComp.get(t).asInstanceOf[TraitIndex]
            val traitKindAndName = (compilation_unit match { case _:ObjectTraitIndex => "object "; case _ => "trait " }) + NU.getName(traitOrObject.ast)
            // Extend the type analyzer with the collected static parameters
            val oldTypeAnalyzer = typeAnalyzer
            // Add static parameters of the enclosing trait or object
            typeAnalyzer = typeAnalyzer.extend(toListFromImmutable(traitOrObject.staticParameters),
                                               None)
            val oracle = new OverloadingOracle()(typeAnalyzer)
            /* The parameter type of a setter must be the same as the return type
             * of a getter with the same name, if any.
             */
            for ( f <- toSet(traitOrObject.setters.keySet) ) {
                if ( traitOrObject.getters.keySet.contains(f) ) {
                    val getter = traitOrObject.getters.get(f)
                    // Setter declarations are guaranteed to have a single parameter.
                    val param = traitOrObject.setters.get(f).parameters.get(0)
                    val span = getter.getSpan
                    if ( param.getIdType.isSome ) {
                        if (! isTrue(typeAnalyzer.equivalent(NU.optTypeOrPatternToType(param.getIdType).unwrap,
                                                      getter.getReturnType.unwrap))(typeAnalyzer) )
                            error(span,
                                  "The parameter type of a setter must be " +
                                  "the same as\n    the return type of a getter " +
                                  "with the same name, if any.")
                    }
                }
            }

            val tt = STypesUtil.declToTraitType(traitOrObject.ast)
	        val methodsR = allMethods(tt, typeAnalyzer)

	        for (f <- toSet(methodsR.firstSet) ; if isDeclaredName(f) ) {
                  val mset = toSet(methodsR.matchFirst(f))
	              val dottedMethods = mset.filter(x => x match { case (m, str, tt) => m.isInstanceOf[JavaMethod] &&
	      	      		  		     	     	      m.asInstanceOf[JavaMethod].selfType.isSome })  // unsure why this second condition
	              checkMethodOverloading(traitKindAndName, f, toMethodArrows(dottedMethods), oracle)
	              val functionalMethods = mset.filter(x => x match { case (m, str, tt) => m.isInstanceOf[JavaFunction]})
	              checkMethodOverloading(traitKindAndName, f, toFunctionalMethodArrows(functionalMethods), oracle)
	        }
            typeAnalyzer = oldTypeAnalyzer
        }
        toJavaList(errors)
    }


    private def checkFancyRule(name: IdOrOpOrAnonymousName, dFunctions: List[(ArrowType,Option[Int])], fMethods: List[(ArrowType,Option[Int])], oracle: OverloadingOracle) = {
      
      // These variables are used to improve the error message
      var x: ArrowType = null 
      var y: ArrowType = null  
      if (!dFunctions.forall(f => fMethods.forall( m => if (!(oracle.lteq(f._1,m._1))) true else { x = f._1; y = m._1; false } )))
        error(x.getInfo().getSpan(),"Overload violation: the declaration of top-level function " + name + " on line " + 
             x.getInfo().getSpan() + " is more specific than the declaration of the functional method " + name + " on line " + 
             y.getInfo().getSpan())
      
    }
    
    /* Checks the validity of the overloaded method declarations. */
    private def checkMethodOverloading(containingKindAndName: String,
    	    			       name: IdOrOpOrAnonymousName,
                                       pairs: List[(ArrowType,Option[Int])],
				       oracle: OverloadingOracle)
                                      : Unit =
      checkOverloading(containingKindAndName, name, pairs, true, oracle)

    /* Checks the validity of the overloaded function declarations. */
    private def checkFunctionOverloading(containingKindAndName: String,
    	    			         name: IdOrOpOrAnonymousName,
                                         pairs: List[(ArrowType,Option[Int])],
					 oracle: OverloadingOracle)
                                        : Unit =
      checkOverloading(containingKindAndName, name, pairs, false, oracle)

    /* Checks the validity of the overloaded function declarations. */
    private def checkOverloading(containingKindAndName: String,
    	    			 name: IdOrOpOrAnonymousName,
                                 pairs: List[(ArrowType,Option[Int])],
                                 isMethod: Boolean,
				 oracle: OverloadingOracle)
                                : Unit = 
        pairs.size match {
                case 0 =>
                case 1 =>
                case _ => var signatures = List[(ArrowType,Option[Int])]()
                          for ( sig@(at, sp) <- pairs ) {
                        	  signatures.find(p => p match { case (at2, sp2) => oracle.equiv(at, at2) && sp == sp2 }) match {
                                	case Some((at2: ArrowType,sp2)) =>
                                	error(mergeSpan(at, at2),
                                			"There are multiple declarations of " +
                                					name + " with the same parameter type: " + globalOracle.sa.makeDomainFromArrow(at, isMethod))
                                	case None =>	
                              }	
                              if ( checkBoundAny(at.getDomain, toListFromImmutable(at.getInfo.getStaticParams)) ) {
                                		error(NU.getSpan(at), "A functional which takes a single parameter " +
                                				"of a parametric type bound by Any\n    cannot be overloaded.")
                              }
                                	signatures = sig :: signatures
                           }
                           var index = 1
                           for ( first <- signatures ) {
                               signatures.slice(index, signatures.length).foreach(second => {
                                       val a1 = validOverloading(first, second, signatures, isMethod, oracle)
                                       if (false) {
                                           val a2 = validOverloading(first, second, signatures, isMethod, oracle)
                                           val a3 = validOverloading(first, second, signatures, isMethod, oracle)
                                           val a4 = validOverloading(first, second, signatures, isMethod, oracle)
                                           val a5 = validOverloading(first, second, signatures, isMethod, oracle)
                                           val a6 = validOverloading(first, second, signatures, isMethod, oracle)
                                           val a7 = validOverloading(first, second, signatures, isMethod, oracle)
                                           val a8 = validOverloading(first, second, signatures, isMethod, oracle)
                                           val a9 = validOverloading(first, second, signatures, isMethod, oracle)
                                           val a10 = validOverloading(first, second, signatures, isMethod, oracle)
                                           val a11 = validOverloading(first, second, signatures, isMethod, oracle)
                                           if (a1 != a2 || a2 != a3 || a3 != a4 ||
                                                   a4 != a5 || a5 != a6 || a6 != a7 ||
                                                   a7 != a8 || a8 != a9 || a9 != a10 ||
                                                   a10 != a11) {
                                               println("Inconsistent overloading validity results")
                                               for (i <- 1 to 100) {
                                                   val for_debugging = validOverloading(first, second, signatures, isMethod, oracle, true)
                                                   if (! for_debugging)
                                                       println("Subsequent failure at " + i)
                                                       else
                                                           println("----------")
                                               } 
                                           }
                                       }
                                       if (! a1 ) {
                                	      val (fa, fsp) = first
                                	      val (ga, gsp) = second
                                	      val firstO = typeAndSpanToString(fa)
                                	      val secondO = typeAndSpanToString(ga)
                                	      val mismatch = if (firstO < secondO) firstO + "\n and " + secondO
                                				         else secondO + "\n and " + firstO
                                					      	error(mergeSpan(fa, ga),
                                								"Invalid overloading of " + name + " in " + containingKindAndName +
                                								":\n     " + mismatch)
                                       } else {
                                	      returnTypeCheck(name, first, second, oracle)
                                	      returnTypeCheck(name, second, first, oracle)
                                       } 
                               }
                                 )
                                 index += 1
                           }
    }

    /* A functional which takes a single parameter of a parametric type
     * bound by Any cannot be overloaded.
     */
    private def checkBoundAny(param: Type, sparams: List[StaticParam]): Boolean =
      if (param.isInstanceOf[VarType]) {
        val tyName = param.asInstanceOf[VarType].getName.getText
        sparams.exists(sp =>
                       sp.getName.getText.equals(tyName) &&
                       toList(sp.getExtendsClause).exists(_.isInstanceOf[AnyType]))
      } else false

    /* Checks the overloading rules: subtype / exclusion / meet */
    private def validOverloading(first: (ArrowType, Option[Int]),
                                 second: (ArrowType, Option[Int]),
                                 signatures: List[(ArrowType, Option[Int])],
				 isMethod: Boolean,
				 oa: OverloadingOracle,
				 debug:Boolean=false): Boolean = {
      val (fa, fsp) = first
      val (ga, gsp) = second
      val b1 = oa.lteq(fa, ga)
      val b2 = oa.lteq(ga, fa) 
      val b3 = oa.excludes(fa, ga) 
      val b4 = meetRule(first, second, signatures, isMethod, oa, debug)
      //if (b1) !b2 else ( b2 || b3 || b4 )  //JT: It looks like the duplicate rule IS enforced somewhere else
      (b1 || b2 || b3 || b4)
    }

    private def meetRule(first: (ArrowType, Option[Int]),
    	                 second: (ArrowType, Option[Int]),
			 signatures: List[(ArrowType, Option[Int])],
			 isMethod: Boolean,
			 oa: OverloadingOracle,
			 debug : Boolean = false): Boolean = {
      val (fa, fsp) = first
      val (ga, gsp) = second
      val b1 = fsp == gsp
      val b2 = (fsp == gsp) &&
      signatures.exists(third => third match {
           case (ha, hsp) => {
              val a1 = (hsp == fsp) 
              val a2 = a1 && oa.lteq(ha, fa) 
              val a3 = a2 && oa.lteq(ha, ga) 
              val a4 = a3 && oa.isMeet(ha, fa, ga, isMethod, debug)
     //         if (debug)
     //             println("" + ha + " " + a1 + a2 + a3 + a4)
                   
              a4}})
      b2
    }

    private def returnTypeCheck(name: IdOrOpOrAnonymousName,
    	    			first: (ArrowType, Option[Int]),
                                second: (ArrowType, Option[Int]),
			        oa: OverloadingOracle) = {
      val (fa, fsp) = first
      val (ga, gsp) = second
//          println("Satisfies return type rule?\n   " + typeAndSpanToString(fa) + "\n   " + typeAndSpanToString(ga))
      if (!oa.satisfiesReturnTypeRule(fa, ga)) {
//        println("For " + name + ",\nthe return type of " + typeAndSpanToString(fa) + " should be a subtype of the\n    return type of " + typeAndSpanToString(ga))
        error(mergeSpan(fa, ga),
	      "For " + name + ",\nthe return type of " + typeAndSpanToString(fa) + " should be a subtype of the\n    return type of " + typeAndSpanToString(ga))
      }
    }


    /* Drop the first element denoting the receiver type of the given type. */
    private def dropReceiver(param: Type) = param match {
      case STupleType(info, _::ty::Nil, None, Nil) => ty
      case STupleType(info, elements, varargs, keywords) =>
        STupleType(info, elements.takeRight(elements.size-1), varargs, keywords)
      case _ => NodeFactory.makeVoidType(NU.getSpan(param))
    }

    def isDeclaredName(f: IdOrOpOrAnonymousName) = f match {
        case SId(_,_,str) => IdentifierUtil.validId(str)
        case SOp(_,_,str,_,_) => NU.validOp(str)
        case _ => false
    }

    def isDeclaredMethod(f: JavaFunctional) = f match {
        case SDeclaredMethod(_,_) => true
        case _ => false
    }

    def isFunction(f: JavaFunctional) = f match {
        case SDeclaredFunction(_) => true
        case SFunctionalMethod(_,_) => true
        case SConstructor(_,_,_,_,_) => true
        case _ => false
    }

    def isFunctionalMethod(f: JavaFunctional) = f match {
        case SFunctionalMethod(_,_) => true
        case _ => false
    }

    def isDeclaredVariable(v: JavaVariable) = v match {
        case SDeclaredVariable(_) => true
        case _ => false
    }

    private def mergeSpan(first: ArrowType, second: ArrowType): Span = mergeSpan(NU.getSpan(first), NU.getSpan(second))

    private def mergeSpan(first: Span, second: Span): Span = new MultiSpan(first, second)

    /* Returns the type of the given list of parameters. */
    private def paramsToType(params: JavaList[Param], span: Span): Type =
      STypesUtil.paramsToType(toListFromImmutable(params), span) match {
        case Some(ty) => ty
        case _ =>
          val span = NU.spanAll(params)
          error(span,
                "Type checking couldn't infer the type of " + params)
          NodeFactory.makeVoidType(span)
      }

    /* Returns the type of the given self type and a list of parameters. */
    private def paramsToType(self: Type, params: JavaList[Param], span: Span): Type = {
      val span = params.size match {
        case 0 => NU.getSpan(self)
        case _ => NU.spanAll(params)
      }
      val elems = toListFromImmutable(params).map(paramToType)
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


    // The following is used by the ExportChecker.

    def coverOverloading(set: Set[JavaFunction]) = {
      var result = Set[JavaFunction]()
      for (f <- set ; if !coveredBy(f, set)) {
        result = result + f
      }
      result = result.filter {
        case SDeclaredFunction(_) => true
        case _ => false
      }
      result.map { case SDeclaredFunction(fd) => fd }
    }

    /*
     * Set may contain the object f; if so, ignore it.
     */
    private def coveredBy(f: JavaFunction, set: Set[JavaFunction]): Boolean = {
      var result = false
      for (g <- set ; if !result && f != g && coveredBy(f, g)) { result = true }
      result
    }

    /* Whether the signature of f is covered by the signature of g */
    /* (If signatures are identical, then NOT covered; this is necessary for error reporting of duplicate definitions. */
    private def coveredBy(f: JavaFunction, g: JavaFunction): Boolean =
      globalOracle.lteq(f, g) && !globalOracle.lteq(g, f) && globalOracle.satisfiesReturnTypeRule(f, g)

}
