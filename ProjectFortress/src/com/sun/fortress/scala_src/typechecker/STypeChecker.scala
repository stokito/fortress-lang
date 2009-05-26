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

import _root_.java.util.{Map => JavaMap}
import _root_.java.util.{HashMap => JavaHashMap}
import _root_.java.util.{Set => JavaSet}
import edu.rice.cs.plt.tuple.{Option => JavaOption}
import edu.rice.cs.plt.collect.EmptyRelation
import edu.rice.cs.plt.collect.IndexedRelation
import edu.rice.cs.plt.collect.Relation
import edu.rice.cs.plt.collect.UnionRelation
import com.sun.fortress.nodes._
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.nodes_util.NodeFactory
import com.sun.fortress.nodes_util.ExprFactory
import com.sun.fortress.nodes_util.NodeUtil
import com.sun.fortress.nodes_util.OprUtil
import com.sun.fortress.exceptions.StaticError
import com.sun.fortress.exceptions.StaticError.errorMsg
import com.sun.fortress.exceptions.TypeError
import com.sun.fortress.compiler.IndexBuilder
import com.sun.fortress.compiler.disambiguator.ExprDisambiguator.HierarchyHistory
import com.sun.fortress.compiler.index.CompilationUnitIndex
import com.sun.fortress.compiler.index.{FunctionalMethod => JavaFunctionalMethod}
import com.sun.fortress.compiler.index.Method
import com.sun.fortress.compiler.index.ObjectTraitIndex
import com.sun.fortress.compiler.index.ProperTraitIndex
import com.sun.fortress.compiler.index.TraitIndex
import com.sun.fortress.compiler.index.TypeConsIndex
import com.sun.fortress.compiler.typechecker.StaticTypeReplacer
import com.sun.fortress.compiler.typechecker.TypeAnalyzer
import com.sun.fortress.compiler.typechecker.TypeEnv
import com.sun.fortress.compiler.typechecker.TypesUtil
import com.sun.fortress.compiler.Types
import com.sun.fortress.scala_src.useful.ASTGenHelper._
import com.sun.fortress.scala_src.useful.ErrorLog
import com.sun.fortress.scala_src.useful.ExprUtil
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.exceptions.InterpreterBug.bug
import com.sun.fortress.useful.HasAt

/* Quesitons
 * 1. Do we infer types of function parameters?
 */
/* Invariants
 * 1. If a subexpression does not have any inferred type,
 *    type checking the subexpression failed.
 */
object STypeCheckerFactory {
  def make(current: CompilationUnitIndex, traits: TraitTable, env: TypeEnv,
           analyzer: TypeAnalyzer) = {
    val errors = new ErrorLog()
    new STypeChecker(current, traits, env, analyzer, errors,
                     new CoercionOracleFactory(traits, analyzer, errors))
  }
  def make(current: CompilationUnitIndex, traits: TraitTable, env: TypeEnv,
           analyzer: TypeAnalyzer, errors: ErrorLog,
           factory: CoercionOracleFactory) = {
    new STypeChecker(current, traits, env, analyzer, errors, factory)
  }
}

class STypeChecker(current: CompilationUnitIndex, traits: TraitTable,
                   env: TypeEnv, analyzer: TypeAnalyzer, errors: ErrorLog,
                   factory: CoercionOracleFactory) {

  val coercionOracle = factory.makeOracle(env)

  private var labelExitTypes: JavaMap[Id, JavaOption[JavaSet[Type]]] =
    new JavaHashMap[Id, JavaOption[JavaSet[Type]]]()

  private def addSelf(self_type: Type) =
    extend(List[LValue](NodeFactory.makeLValue("self", self_type)))

  private def extend(newEnv: TypeEnv, newAnalyzer: TypeAnalyzer) =
    STypeCheckerFactory.make(current, traits, newEnv, newAnalyzer, errors, factory)

  private def extend(bindings: List[LValue]) =
    STypeCheckerFactory.make(current, traits,
                             env.extendWithLValues(toJavaList(bindings)),
                             analyzer, errors, factory)

  private def extend(sparams: List[StaticParam], where: Option[WhereClause]) =
    STypeCheckerFactory.make(current, traits,
                             env.extendWithStaticParams(sparams),
                             analyzer.extend(sparams, where), errors, factory)

  private def extend(sparams: List[StaticParam], params: Option[List[Param]],
                     where: Option[WhereClause]) = params match {
    case Some(ps) =>
      STypeCheckerFactory.make(current, traits,
                               env.extendWithParams(ps).extendWithStaticParams(sparams),
                               analyzer.extend(sparams, where), errors, factory)
    case None =>
      STypeCheckerFactory.make(current, traits,
                               env.extendWithStaticParams(sparams),
                               analyzer.extend(sparams, where), errors, factory)
  }

  private def extendWithFunctions(methods: Relation[IdOrOpOrAnonymousName, JavaFunctionalMethod]) =
    STypeCheckerFactory.make(current, traits, env.extendWithFunctions(methods),
                             analyzer, errors, factory)

  private def extendWithMethods(methods: Relation[IdOrOpOrAnonymousName, Method]) =
    STypeCheckerFactory.make(current, traits, env.extendWithMethods(methods),
                             analyzer, errors, factory)

  private def extendWithout(declSite: Node, names: JavaSet[Id]) =
    STypeCheckerFactory.make(current, traits, env.extendWithout(declSite, names),
                             analyzer, errors, factory)

  private def noType(hasAt:HasAt) =
    signal(hasAt, "Type is not inferred for: " + hasAt)

  private def signal(msg:String, hasAt:HasAt) =
    errors.signal(msg, hasAt)

  private def signal(hasAt:HasAt, msg:String) =
    errors.signal(msg, hasAt)

  private def inferredType(expr:Expr): Option[Type] =
    scalaify(expr.getInfo.getExprType).asInstanceOf[Option[Type]]

  private def haveInferredTypes(exprs: List[Expr]): Boolean =
    exprs.forall((e:Expr) => inferredType(e).isDefined)

  private def isArrows(expr: Expr): Boolean =
    TypesUtil.isArrows(inferredType(expr).get).asInstanceOf[Boolean]

  private def checkSubtype(subtype:Type, supertype:Type, node:Node, error:String) = {
    val judgement = analyzer.subtype(subtype, supertype).isTrue
    if (! judgement) signal(error, node)
    judgement
  }

  private def handleAliases(name: Id, api: APIName, imports: List[Import]): Id = {
    def getAliases(imp: Import): Option[Id] = imp match {
      case SImportNames(info, foreignLanguage, aliasApi, aliases) =>
        if ( api.equals(aliasApi) ) {
          def getName(aliasedName: AliasedSimpleName): Option[Id] = aliasedName match {
            case SAliasedSimpleName(_, newName, Some(alias)) =>
              if ( alias.equals(name) ) Some(newName.asInstanceOf)
              else None
            case _ => None
          }
          aliases.flatMap(getName).find((x:Id) => true)
        } else None
      case _ => None
    }
    imports.flatMap(getAliases).find((x:Id) => true).getOrElse(name)
  }

  private def getEnvFromApi(api: APIName): TypeEnv =
    TypeEnv.make( traits.compilationUnit(api) )

  private def getTypeFromName(name: Name): Option[Type] = name match {
    case id@SId(info, api, name) => api match {
      case Some(api) => toOption(getEnvFromApi(api).getType(id))
      case _ => toOption(env.getType(id))
    }
    case _ => None
  }

  def getErrors(): List[StaticError] = errors.errors

  private def assertTrait(t: BaseType, ast: Node, msg: String,
                          error_loc: Node) = t match {
    case tt@STraitType(info, name, args, params) =>
      traits.typeCons(tt.getName).asInstanceOf[Option[TypeConsIndex]] match {
        case Some(ti) =>
          if ( ! ti.isInstanceOf[ProperTraitIndex] ) signal(error_loc, msg)
        case _ => signal(error_loc, msg)
      }
    case SAnyType(info) =>
    case _ => signal(error_loc, msg)
  }

  private def inheritedMethods(extendedTraits: List[TraitTypeWhere]) =
    inheritedMethodsHelper(new HierarchyHistory(), extendedTraits)

  // Return all of the methods from super-traits
  private def inheritedMethodsHelper(history: HierarchyHistory,
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
          case ty@STraitType(info, name, args, params) =>
            traits.typeCons(name).asInstanceOf[Option[TypeConsIndex]] match {
              case Some(ti) =>
                if ( ti.isInstanceOf[TraitIndex] ) {
                  val trait_params = ti.staticParameters
                  val trait_args = ty.getArgs
                  // Instantiate methods with static args
                  val dotted = ti.asInstanceOf[TraitIndex].dottedMethods.asInstanceOf[Set[(IdOrOpOrAnonymousName,Method)]]
                  for ( pair <- dotted ) {
                      methods.add(pair._1,
                                  pair._2.instantiate(trait_params,trait_args).asInstanceOf[Method])
                  }
                  val getters = ti.asInstanceOf[TraitIndex].getters
                  for ( getter <- getters.keySet.asInstanceOf[Set[IdOrOpOrAnonymousName]] ) {
                      methods.add(getter,
                                  getters.get(getter).instantiate(trait_params,trait_args).asInstanceOf[Method])
                  }
                  val setters = ti.asInstanceOf[TraitIndex].setters
                  for ( setter <- setters.keySet.asInstanceOf[Set[IdOrOpOrAnonymousName]] ) {
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

  def check(node:Node):Node = node match {
    case SComponent(info, name, imports, decls, isNative, exports)  =>
      SComponent(info, name, imports,
                 decls.map((n:Decl) => check(n).asInstanceOf[Decl]),
                 isNative, exports)

    case t@STraitDecl(info,
                      STraitTypeHeader(sparams, mods, name, where,
                                       throwsC, contract, extendsC, decls),
                      excludes, comprises, hasEllipses, selfType) => {
      // Verify that this trait only extends other traits
      extendsC.foreach( (t:TraitTypeWhere) =>
                        assertTrait(t.getBaseType, t,
                                    "Traits can only extend traits.", t) )
      val checkerWSparams = this.extend(sparams, where)
      var method_checker = checkerWSparams
      var field_checker = checkerWSparams
      // Add field declarations (getters/setters?) to method_checker
      method_checker = decls.foldRight(method_checker)
                                      { (d:Decl, c:STypeChecker) => d match {
                                        case SVarDecl(_,lhs,_) => c.extend(lhs)
                                        case _ => c } }
      traits.typeCons(name.asInstanceOf[Id]).asInstanceOf[Option[TypeConsIndex]] match {
        case None => signal(name, name + " is not found."); t
        case Some(ti) =>
          // Extend method checker with methods and functions
          // that will now be in scope
          val methods = new UnionRelation(inheritedMethods(extendsC),
                                          ti.asInstanceOf[TraitIndex].dottedMethods)
          method_checker = method_checker.extendWithMethods(methods)
          method_checker = method_checker.extendWithFunctions(ti.asInstanceOf[TraitIndex].functionalMethods)
          // Extend method checker with self
          selfType match {
            case Some(ty) =>
              method_checker = method_checker.addSelf(ty)
              // Check declarations
              val newDecls = decls.map( (d:Decl) => d match {
                                        case SFnDecl(_,_,_,_,_) =>
                                          // methods see extra variables in scope
                                          method_checker.check(d).asInstanceOf[Decl]
                                        case SVarDecl(_,lhs,_) =>
                                          // fields see other fields
                                          val newD = field_checker.check(d).asInstanceOf[Decl]
                                          field_checker = field_checker.extend(lhs)
                                          newD
                                        case _ => checkerWSparams.check(d).asInstanceOf[Decl] } )
              STraitDecl(info,
                         STraitTypeHeader(sparams, mods, name, where,
                                          throwsC, contract, extendsC, newDecls),
                         excludes, comprises, hasEllipses, selfType)
            case _ => signal(t, "Self type is not inferred for " + t); t
          }
      }
    }

    case o@SObjectDecl(info,
                       STraitTypeHeader(sparams, mods, name, where,
                                        throwsC, contract, extendsC, decls),
                       params, selfType) => {
      // Verify that no extends clauses try to extend an object.
      extendsC.foreach( (t:TraitTypeWhere) =>
                        assertTrait(t.getBaseType, o,
                                    "Objects can only extend traits.", o) )
      val checkerWSparams = this.extend(sparams, params, where)
      var method_checker = checkerWSparams
      var field_checker = checkerWSparams
      val newContract = contract match {
        case Some(e) => Some(method_checker.check(e).asInstanceOf[Contract])
        case _ => contract
      }
      // Extend method checker with fields
      method_checker = decls.foldRight(method_checker)
                                      { (d:Decl, c:STypeChecker) => d match {
                                        case SVarDecl(_,lhs,_) => c.extend(lhs)
                                        case _ => c } }
      // Check method declarations.
      traits.typeCons(name.asInstanceOf[Id]).asInstanceOf[Option[TypeConsIndex]] match {
        case None => signal(name, name + " is not found."); o
        case Some(oi) =>
          // Extend type checker with methods and functions
          // that will now be in scope as regular functions
          val methods = new UnionRelation(inheritedMethods(extendsC),
                                          oi.asInstanceOf[TraitIndex].dottedMethods)
          method_checker = method_checker.extendWithMethods(methods)
          method_checker = method_checker.extendWithFunctions(oi.asInstanceOf[TraitIndex].functionalMethods)
          // Extend method checker with self
          selfType match {
            case Some(ty) =>
              method_checker = method_checker.addSelf(ty)
              // Check declarations, storing them in the same order
              val newDecls = decls.map( (d:Decl) => d match {
                                        case SFnDecl(_,_,_,_,_) =>
                                          // Methods get some extra vars in their declarations
                                          method_checker.check(d).asInstanceOf[Decl]
                                        case SVarDecl(_,lhs,_) =>
                                          // Fields get to see earlier fields
                                          val newD = field_checker.check(d).asInstanceOf[Decl]
                                          field_checker = field_checker.extend(lhs)
                                          newD
                                        case _ => checkerWSparams.check(d).asInstanceOf[Decl] } )
              SObjectDecl(info,
                          STraitTypeHeader(sparams, mods, name, where,
                                           throwsC, newContract, extendsC, newDecls),
                          params, selfType)
            case _ => signal(o, "Self type is not inferred for " + o); o
          }
      }
    }

    /* Matches if a function declaration does not have a body expression. */
    case f@SFnDecl(info,
                   SFnHeader(statics,mods,name,wheres,throws,contract,params,returnType),
                   unambiguousName, None, implementsUnambiguousName) => {
      returnType match {
        case Some(ty) =>
          if ( NodeUtil.isSetter(f) )
            checkSubtype(ty, Types.VOID, f, "Setter declarations must return void.")
        case _ =>
      }
      f
    }

    /* Matches if a function declaration has a body expression. */
    case f@SFnDecl(info,
                   SFnHeader(statics,mods,name,wheres,throws,contract,params,returnType),
                   unambiguousName, Some(body), implementsUnambiguousName) => {
      val newChecker = this.extend(env.extendWithStaticParams(statics).extendWithParams(params),
                                   analyzer.extend(statics, wheres))
      val newContract = contract match {
        case Some(c) => Some(newChecker.check(c))
        case None => None
      }
      val newBody = newChecker.checkExpr(body, returnType, "Function body",
                                         "declared return")
      val newType = inferredType(newBody) match {
        case Some(ty) =>
          if ( NodeUtil.isSetter(f) )
            checkSubtype(ty, Types.VOID, f, "Setter declarations must return void.")
          Some(ty)
        case _ => noType(body); None
      }
      SFnDecl(info,
              SFnHeader(statics, mods, name, wheres, throws,
                        newContract.asInstanceOf[Option[Contract]],
                        params, newType),
              unambiguousName, Some(newBody), implementsUnambiguousName)
    }

    case v@SVarDecl(info, lhs, body) => body match {
      case Some(init) =>
        val newInit = checkExpr(init)
        val ty = lhs match {
          case l::Nil => // We have a single variable binding, not a tuple binding
            l.getIdType.asInstanceOf[Option[Type]] match {
              case Some(typ) => typ
              case _ => // Eventually, this case will involve type inference
                signal(v, "All inferrred types should at least be inference " +
                       "variables by typechecking: " + v)
                NodeFactory.makeVoidType(NodeUtil.getSpan(l))
            }
          case _ =>
            def handleBinding(binding: LValue) =
              binding.getIdType.asInstanceOf[Option[Type]] match {
                case Some(typ) => typ
                case _ =>
                  signal(binding, "Missing type for " + binding + ".")
                  NodeFactory.makeVoidType(NodeUtil.getSpan(binding))
              }
            NodeFactory.makeTupleType(NodeUtil.getSpan(v),
                                      toJavaList(lhs.map(handleBinding)))
        }
        inferredType(newInit) match {
          case Some(typ) =>
            checkSubtype(typ, ty, v,
                         errorMsg("Attempt to define variable ", v,
                                  " with an expression of type ", typ))
          case _ =>
            signal(v, "The right-hand side of " + v + " could not be typed.")
        }
        SVarDecl(info, lhs, Some(newInit))
      case _ => v
    }

    case id@SId(info,api,name) => {
      api match {
        case Some(apiName) => {
          val newName = handleAliases(id, apiName, toList(current.ast.getImports))
          getTypeFromName( newName ) match {
            case Some(ty) =>
              if ( ty.isInstanceOf[NamedType] ) {
                // Type was declared in that API, so it's not qualified;
                // prepend it with the API.
                /*
                if ( ty.asInstanceOf[NamedType].getName.getApiName.isNone )
                  _type = NodeFactory.makeNamedType(apiName, ty.asInstanceOf[NamedType])
                */
              }
            case _ =>
              // Operators are never qualified in source code,
              // so if 'name' is qualified and not found,
              // it must be an Id, not an Op.
              signal(id, "Attempt to reference unbound variable: " + id)
          }
        }
        case _ => {
          getTypeFromName( id ) match {
            case Some(ty) => ty match {
              case SLabelType(_) => // then, newName must be an Id
                signal(id, "Cannot use label name " + id + " as an identifier.")
              case _ =>
            }
            case _ => signal(id, "Variable '" + id + "' not found.")
          }
        }
      }
      id
    }

    case op@SOp(info,api,name,fixity,enclosing) => {
      val tyEnv = api match {
        case Some(api) => getEnvFromApi(api)
        case _ => env
      }
      scalaify(tyEnv.binding(op)).asInstanceOf[Option[TypeEnv.BindingLookup]] match {
        case None =>
          if ( enclosing ) signal(op, "Enclosing operator not found: " + op)
          else signal(op, "Operator not found: " + OprUtil.decorateOperator(op))
        case _ =>
      }
      op
    }

    case _ => throw new Error("not yet implemented: " + node.getClass)
  }

  def checkExpr(expr: Expr, expected: Option[Type],
                first: String, second: String): Expr = {
    val newExpr = checkExpr(expr)
    inferredType(newExpr) match {
      case Some(typ) => expected match {
        case Some(t) =>
          checkSubtype(typ, t, expr,
                       first + " has type " + typ + ", but " + second +
                       " type is " + t + ".")
          ExprUtil.addType(newExpr, typ)
        case _ => ExprUtil.addType(newExpr, typ)
      }
      case _ => noType(expr); expr
    }
  }

  def checkExpr(expr: Expr, expected: Option[Type], message: String): Expr = {
    val newExpr = checkExpr(expr)
    inferredType(newExpr) match {
      case Some(typ) => expected match {
        case Some(t) =>
          checkSubtype(typ, t, expr,
                       message + " has type " + typ + ", but it must have " +
                       t + " type.")
          ExprUtil.addType(newExpr, typ)
        case _ => ExprUtil.addType(newExpr, typ)
      }
      case _ => noType(expr); expr
    }
  }

  class AtomicChecker(current: CompilationUnitIndex, traits: TraitTable,
                      env: TypeEnv, analyzer: TypeAnalyzer, errors: ErrorLog,
                      factory: CoercionOracleFactory, enclosingExpr: String)
      extends STypeChecker(current,traits,env,analyzer,errors,factory) {
    val message = "A 'spawn' expression must not occur inside " +
                  enclosingExpr + "."
    override def checkExpr(e: Expr): Expr = e match {
      case SSpawn(_, _) => signal(e, message); e
      case _ => super.checkExpr(e)
    }
  }

  private def forAtomic(expr: Expr, enclosingExpr: String) =
    new AtomicChecker(current,traits,env,analyzer,errors,factory,enclosingExpr).checkExpr(expr)

  /**
   * Given a type, which could be a VarType, Intersection or Union, return the TraitTypes
   * that this type could be used as for the purposes of calling methods and fields.
   */
  private def traitTypesCallable(typ: Type): List[TraitType] = typ match {
    case SIntersectionType(info, ts) =>
      ts.foldRight(List[TraitType]()){ (t: Type, l: List[TraitType]) =>
                                       if ( NodeUtil.isTraitType(t) )
                                         traitTypesCallable(t):::l
                                       else l }
    case t@STraitType(info, name, args, params) => List[TraitType](t)
    case SVarType(info, name, depth) =>
      env.staticParam(name).asInstanceOf[Option[StaticParam]] match {
        case Some(s@SStaticParam(info,_,ts,_,_,kind)) =>
          if ( NodeUtil.isTypeParam(s) )
            ts.foldRight(List[TraitType]()){ (t: Type, l: List[TraitType]) =>
                                             if ( NodeUtil.isTraitType(t) )
                                               traitTypesCallable(t):::l
                                             else l }
          else List[TraitType]()
        case _ => List[TraitType]()
    }
    case SUnionType(info, ts) =>
      signal(typ, "You should be able to call methods on this type," +
             "but this is not yet implemented.")
      List[TraitType]()
    case _ => List[TraitType]()
  }

  /* Not yet implemented.
   * Waiting for _RewriteFnApp to be implemented.
   */
  private def findMethodsInTraitHierarchy(method_name: IdOrOpOrAnonymousName,
                                          supers: List[TraitType], arg_type: Type,
                                          in_static_args: List[StaticArg],
                                          that: Node): (List[Method], List[Method]) =
    (Nil, Nil)

  /* Invariant: newObj and the elements of newSubs all have type information.
   */
  private def subscriptHelper(expr: SubscriptExpr, newObj: Expr,
                              newSubs: List[Expr]) = expr match {
    case SSubscriptExpr(SExprInfo(span,parenthesized,_), _, _, op, sargs) => {
      val obj_type = inferredType(newObj).get
      val traits = traitTypesCallable(obj_type)
      val subs_types = newSubs.map((e:Expr) => inferredType(e).get)
      traits match {
        case Nil => {
          // We need to have a trait otherwise we can't see its methods.
          signal(expr, "Only traits can have subscripting methods and " + obj_type +
                 " is not one.")
          expr
        }
        case head::tail => {
          // Make a tuple type out of given argument types.
          val arg_type = subs_types.length match {
            case 1 => subs_types.head
            case _ => Types.MAKE_TUPLE.value(toJavaList(subs_types))
          }
          op match {
            case Some(opr) => {
              val (candidates,_) = findMethodsInTraitHierarchy(opr, traits, arg_type,
                                                               sargs, expr)
              candidates match {
                case Nil =>
                  signal("No candidate methods found for '" + opr + "' on type " +
                         obj_type + " with argument types (" + arg_type + ").", expr)
                  expr
                case _ => {
                  val newType = analyzer.meet(toJavaList(candidates.map((m:Method) => m.getReturnType)))
                  SSubscriptExpr(SExprInfo(span,parenthesized,Some(newType)),
                                 newObj, newSubs, op, sargs)
                }
              }
            }
            case _ =>
              signal(expr,
                     "A subscript expression requires the subscripting operator.")
              expr
          }
        }
      }
    }
  }

  /* The Java type checker had a separate postinference pass "closing bindings". */
  private def generatorClauseGetBindings(clause: GeneratorClause,
                                         mustBeCondition: Boolean) = clause match {
    case SGeneratorClause(info, binds, init) =>
      val newInit = checkExpr(init)
      val err = "Filter expressions in generator clauses must have type Boolean, " +
                "but " + init
      inferredType(newInit) match {
        case None =>
          signal(init, err + " was not well typed.")
          (SGeneratorClause(info, Nil, newInit), Nil)
        case Some(ty) =>
          checkSubtype(ty, Types.BOOLEAN, init, err + " had type " + ty + ".")
          binds match {
            case Nil =>
              // If bindings are empty, then init must be of type Boolean, a filter, 13.14
              (SGeneratorClause(info, Nil, newInit), Nil)
            case hd::tl =>
              def mkInferenceVarType(id: Id) =
                NodeFactory.make_InferenceVarType(NodeUtil.getSpan(id))
              val (lhstype, bindings) = binds.length match {
                case 1 => // Just one binding
                  val lhstype = mkInferenceVarType(hd)
                  (lhstype, List[LValue](NodeFactory.makeLValue(hd, lhstype)))
                case n =>
                  // Because generator_type is almost certainly an _InferenceVar,
                  // we have to declare a new tuple that is the size of the bindings
                  // and declare one to be a subtype of the other.
                  val inference_vars = binds.map(mkInferenceVarType)
                  (Types.makeTuple(toJavaList(inference_vars)),
                   binds.zip(inference_vars).map((p:(Id,Type)) =>
                                                 NodeFactory.makeLValue(p._1,p._2)))
              }
              // Get the type of the Generator
              val infer_type = NodeFactory.make_InferenceVarType(NodeUtil.getSpan(init))
              val generator_type = if (mustBeCondition)
                                     Types.makeConditionType(infer_type)
                                   else Types.makeGeneratorType(infer_type)
              checkSubtype(ty, generator_type, init,
                           "Init expression of generator must be a subtype of " +
                           (if (mustBeCondition) "Condition" else "Generator") +
                           " but is type " + ty + ".")
              val err = "If more than one variable is bound in a generator, " +
                        "generator must have tuple type but " + init +
                        " does not or has different number of arguments."
              checkSubtype(lhstype, generator_type, init, err);
              checkSubtype(generator_type, lhstype, init, err);
              (SGeneratorClause(info, binds, newInit), bindings)
          }
      }
  }

  private def handleIfClause(c: IfClause) = c match {
    case SIfClause(info, testClause, body) =>
      // For generalized 'if' we must introduce new bindings.
      val (newTestClause, bindings) = generatorClauseGetBindings(testClause, true)
      // Check body with new bindings
      val newBody = this.extend(bindings).checkExpr(body).asInstanceOf[Block]
      inferredType(newBody) match {
        case None => noType(body)
        case _ =>
      }
      SIfClause(info, newTestClause, newBody)
  }

  // For each generator clause, check its body,
  // then put its variables in scope for the next generator clause.
  // Finally, return all of the bindings so that they can be put in scope
  // in some larger expression, like the body of a for loop, for example.
  def handleGens(generators: List[GeneratorClause]): (List[GeneratorClause], List[LValue]) =
    generators match {
      case Nil => (Nil, Nil)
      case hd::Nil =>
        val (clause, binds) = generatorClauseGetBindings(hd, false)
        (List[GeneratorClause](clause), binds)
      case hd::tl =>
        val (clause, binds) = generatorClauseGetBindings(hd, false)
        val (newTl, tlBinds) = this.extend(binds).handleGens(tl)
        (clause::newTl, binds++tlBinds)
    }

  def checkExpr(expr: Expr): Expr = expr match {
    case o@SObjectExpr(SExprInfo(span,parenthesized,_),
                     STraitTypeHeader(sparams, mods, name, where,
                                      throwsC, contract, extendsC, decls),
                     selfType) => {
      // Verify that no extends clauses try to extend an object.
      extendsC.foreach( (t:TraitTypeWhere) =>
                        assertTrait(t.getBaseType, o,
                                    "Objects can only extend traits.", o) )
      var method_checker = this
      var field_checker = this
      val newContract = contract match {
        case Some(e) => Some(method_checker.check(e).asInstanceOf[Contract])
        case _ => contract
      }
      // Extend the type checker with all of the field decls
      method_checker = decls.foldRight(method_checker)
                                      { (d:Decl, c:STypeChecker) => d match {
                                        case SVarDecl(_,lhs,_) => c.extend(lhs)
                                        case _ => c } }
      // Extend type checker with methods and functions
      // that will now be in scope as regular functions
      val oi = IndexBuilder.buildObjectExprIndex(o)
      val methods = new UnionRelation(inheritedMethods(extendsC),
                                      oi.asInstanceOf[ObjectTraitIndex].dottedMethods)
      method_checker = method_checker.extendWithMethods(methods)
      method_checker = method_checker.extendWithFunctions(oi.asInstanceOf[ObjectTraitIndex].functionalMethods)
      // Extend method checker with self
      selfType match {
        case Some(ty) =>
          method_checker = method_checker.addSelf(ty)
          // Typecheck each declaration
          val newDecls = decls.map( (d:Decl) => d match {
                                    case SFnDecl(_,_,_,_,_) =>
                                      // Methods get a few more things in scope than everything else
                                      method_checker.check(d).asInstanceOf[Decl]
                                    case SVarDecl(_,lhs,_) =>
                                      // fields get to see earlier fields
                                      val newD = field_checker.check(d).asInstanceOf[Decl]
                                      field_checker = field_checker.extend(lhs)
                                      newD
                                    case _ => check(d).asInstanceOf[Decl] } )
          SObjectExpr(SExprInfo(span,parenthesized,Some(ty)),
                      STraitTypeHeader(sparams, mods, name, where,
                                       throwsC, newContract, extendsC, newDecls),
                      selfType)
        case _ => signal(o, "Self type is not inferred for " + o); o
      }
    }

    /* Matches if block is an atomic block. */
    case SBlock(SExprInfo(span,parenthesized,resultType),
                loc, true, withinDo, exprs) =>
      forAtomic(SBlock(SExprInfo(span,parenthesized,resultType),
                       loc, false, withinDo, exprs),
                "an 'atomic'do block")

    /* Matches if block is not an atomic block. */
    case SBlock(SExprInfo(span,parenthesized,resultType),
                loc, false, withinDo, exprs) => {
      val newLoc = loc match {
        case Some(l) =>
          Some(checkExpr(l, Some(Types.REGION), "Location of the block"))
        case None => loc
      }
      exprs.reverse match {
        case Nil =>
          SBlock(SExprInfo(span,parenthesized,Some(Types.VOID)),
                 newLoc, false, withinDo, exprs)
        case last::rest =>
        val allButLast = rest.map((e: Expr) => checkExpr(e, Some(Types.VOID),
                                                         "Non-last expression in a block"))
          val lastExpr = checkExpr(last)
          val newExprs = (lastExpr::allButLast).reverse
          SBlock(SExprInfo(span,parenthesized,inferredType(lastExpr)),
                 newLoc, false, withinDo, newExprs)
      }
    }

    case s@SSpawn(SExprInfo(span,paren,optType), body) => {
      val newExpr = this.extendWithout(s, labelExitTypes.keySet).checkExpr(body)
      inferredType(newExpr) match {
        case Some(typ) =>
          SSpawn(SExprInfo(span,paren,Some(Types.makeThreadType(typ))), newExpr)
        case _ => noType(body); expr
      }
    }

    case SAtomicExpr(SExprInfo(span,paren,optType), body) => {
      val newExpr = forAtomic(body, "an 'atomic' expression")
      SAtomicExpr(SExprInfo(span,paren,inferredType(newExpr)), newExpr)
    }

    case STryAtomicExpr(SExprInfo(span,paren,optType), body) => {
      val newExpr = forAtomic(body, "an 'tryatomic' expression")
      STryAtomicExpr(SExprInfo(span,paren,inferredType(newExpr)), newExpr)
    }

    // For a tight juxt, create a MathPrimary
    case SJuxt(info, multi, infix, front::rest, false, true) => {
      def toMathItem(exp: Expr): MathItem = {
        val span = NodeUtil.getSpan(exp)
        if ( exp.isInstanceOf[TupleExpr] ||
             exp.isInstanceOf[VoidLiteralExpr] ||
             NodeUtil.isParenthesized(exp) )
          ExprFactory.makeParenthesisDelimitedMI(span, exp)
        else ExprFactory.makeNonParenthesisDelimitedMI(span, exp)
      }
      checkExpr(SMathPrimary(info, multi, infix, front, rest.map(toMathItem)))
    }

    // If this juxt is actually a fn app, then rewrite to a fn app.
    case SJuxt(info, multi, infix, front::rest, true, true) => rest.length match {
      case 1 => checkExpr(S_RewriteFnApp(info, front, rest.head))
      case n => // Make sure it is just two exprs.
        signal(expr, "TightJuxt denoted as function application but has " +
               n + "(!= 2) expressions.")
        expr
    }

    /* Loose Juxts are handled using the algorithm in 16.8 of Fortress Spec 1.0
     */
    case SJuxt(SExprInfo(span,paren,optType),
               multi, infix, exprs, isApp, false) => {
      // Check subexpressions
      val checkedExprs = exprs.map(checkExpr)
      if ( haveInferredTypes(checkedExprs) ) {
        // Break the list of expressions into chunks.
        // First the loose juxtaposition is broken into nonempty chunks;
        // wherever there is a non-function element followed
        // by a function element, the latter begins a new chunk.
        // Thus a chunk consists of some number (possibly zero) of
        // functions followed by some number (possibly zero) of non-functions.
        def chunker(exprs: List[Expr], results: List[(List[Expr],List[Expr])]): List[(List[Expr],List[Expr])] = exprs match {
          case Nil => results.reverse
          case first::rest =>
            if ( isArrows(first) ) {
              val arrows = first::rest.takeWhile(isArrows)
              val dropArrows = rest.dropWhile(isArrows)
              val nonArrows = dropArrows.takeWhile((e:Expr) => ! isArrows(e))
              val dropNonArrows = dropArrows.dropWhile((e:Expr) => ! isArrows(e))
              chunker(dropNonArrows, (arrows,nonArrows)::results)
            } else {
              val nonArrows = first::rest.takeWhile((e:Expr) => ! isArrows(e))
              val dropNonArrows = rest.dropWhile((e:Expr) => ! isArrows(e))
              chunker(dropNonArrows, (List(),nonArrows)::results)
            }
        }
        val chunks = chunker(checkedExprs, List())
        // Left associate nonarrows as a single OpExpr
        def associateNonArrows(nonArrows: List[Expr]): Option[Expr] =
          nonArrows match {
            case Nil => None
            case head::tail =>
              Some(tail.foldLeft(head){ (e1: Expr, e2: Expr) =>
                                        ExprFactory.makeOpExpr(infix,e1,e2) })
          }
        // Right associate everything in a chunk as a _RewriteFnApp
        def associateArrows(fs: List[Expr], oe: Option[Expr]) = oe match {
          case None => fs match {
            case Nil => bug(expr, "Empty chunk")
            case _ =>
              fs.take(fs.size-1).foldRight(fs.last){ (f: Expr, e: Expr) =>
                                                     ExprFactory.make_RewriteFnApp(f,e) }
          }
          case Some(e) =>
            fs.foldRight(e){ (f: Expr, e: Expr) => ExprFactory.make_RewriteFnApp(f,e) }
        }
        // Associate a chunk
        def associateChunk(chunk: (List[Expr],List[Expr])): Expr = {
          val (arrows, nonArrows) = chunk
          associateArrows(arrows, associateNonArrows(nonArrows))
        }
        val associatedChunks = chunks.map(associateChunk)
        // (1) If any element that remains has type String,
        //     then it is a static error
        //     if there is any pair of adjacent elements within the juxtaposition
        //     such that neither element is of type String.
        val types = associatedChunks.map((e: Expr) => inferredType(e).get)
        def isStringType(t: Type) = analyzer.subtype(t, Types.STRING).isTrue
        if ( types.exists(isStringType) ) {
          def stringCheck(e: Type, f: Type) =
            if ( ! (isStringType(e) || isStringType(f)) ) {
              signal(expr, "Neither element is of type String in " +
                     "a juxtaposition of String elements.")
              e
            } else e
          types.take(types.size-1).foldRight(types.last)(stringCheck)
        }
        // (2) Treat the sequence that remains as a multifix application
        //     of the juxtaposition operator.
        //     The rules for multifix operators then apply.
        val multiOpExpr = checkExpr(ExprFactory.makeOpExpr(span, paren, toJavaOption(optType),
                                                           multi, toJavaList(associatedChunks)))
        if ( inferredType(multiOpExpr).isDefined ) multiOpExpr
        else {
          // If not, left associate as InfixJuxts
          associatedChunks match {
            case Nil => bug(expr, "Empty juxt")
            case head::tail =>
              checkExpr(tail.foldLeft(head){ (e1: Expr, e2: Expr) =>
                                             ExprFactory.makeOpExpr(infix,e1,e2) })
          }
        }
      } else noType(expr); expr
    }

    // Math primary, which is the more general case,
    // is going to be called for both tight Juxt and MathPrimary

    // Base case of recursion: If there is no 'rest', return the Expr
    case SMathPrimary(info, multi, infix, front, Nil) => checkExpr(front)

    case mp@SMathPrimary(info@SExprInfo(span,paren,optType),
                         multi, infix, front, rest@second::remained) => {
      /** Check for ^ followed by ^ or ^ followed by [], both static errors. */
      def exponentiationStaticCheck(items: List[MathItem]) = {
        var exponent: Option[MathItem] = None
        def checkMathItem(item: MathItem) = item match {
          case SExponentiationMI(_,_,_) => exponent match {
            case None => exponent = Some(item)
            case Some(e) => signal(item, "Two consecutive ^s.")
          }
          case SSubscriptingMI(_,_,_,_) => exponent match {
            case Some(e) =>
              signal(item, "Exponentiation followed by subscripting is illegal.")
            case None =>
          }
          case _ => exponent = None
        }
        // Check for two exponentiations or an exponentiation and a subscript in a row
        items.foreach(checkMathItem)
      }
      exponentiationStaticCheck(rest) // See if simple static errors exist

      def isExprMI(expr: MathItem): Boolean = expr match {
        case SParenthesisDelimitedMI(_, _) => true
        case SNonParenthesisDelimitedMI(_, _) => true
        case _ => false
      }
      def isParenedExprItem(item: MathItem) = item match {
        case SParenthesisDelimitedMI(_,_) => true
        case _ => false
      }
      def isFunctionItem(item: MathItem) = item match {
        case SParenthesisDelimitedMI(_,e) => isArrows(checkExpr(e))
        case SNonParenthesisDelimitedMI(_,e) => isArrows(checkExpr(e))
        case _ =>
      }
      def expectParenedExprItem(item: MathItem) =
        if ( ! isParenedExprItem(item) )
          signal(item, "Argument to function must be parenthesized.")
      def expectExprMI(item: MathItem) =
        if ( ! isExprMI(item) )
          signal(item, "Item at this location must be an expression, not an operator.")
      // items is not an empty list.
      def associateMathItems(first: Expr,
                             items: List[MathItem]): (Expr, List[MathItem]) = {
        /* For each expression element (i.e., not a subscripting, exponentiation
         * or postfix operator), determine whether it is a function.
         * If some function element is immediately followed by an expression
         * element then, find the first such function element, and call the
         * next element the argument.
         */
        // find the left-most function
        val (prefix, others) = items.span((e:MathItem) =>
                                          !isFunctionItem(e).asInstanceOf[Boolean])
        others match {
          case fn::arg::suffix => arg match {
            // It is a static error if either the argument is not parenthesized,
            case SNonParenthesisDelimitedMI(_,e) =>
              signal(e, "Tightly juxtaposed expression should be parenthesized.")
              (first, Nil)
            case SParenthesisDelimitedMI(i,e) => {
              // or the argument is immediately followed by a non-expression element.
              suffix match {
                case third::more =>
                  if ( ! isExprMI(third) )
                    signal(third, "An expression is expected.")
                case _ =>
              }
              // Otherwise, replace the function and argument with a single element
              // that is the application of the function to the argument.  This new
              // element is an expression.  Reassociate the resulting sequence
              // (which is one element shorter)
              val fnApp =
                new NonParenthesisDelimitedMI(i,
                                              ExprFactory.make_RewriteFnApp(fn.asInstanceOf[ExprMI].getExpr,
                                                                            arg.asInstanceOf[ExprMI].getExpr))
              associateMathItems( first, prefix++(fnApp::suffix) )
            }
            case _ => reassociateMathItems( first, items )
          }
          case _ => reassociateMathItems( first, items )
        }
      }
      // items is not an empty list.
      def reassociateMathItems(first: Expr, items: List[MathItem]) = {
        val (left, right) = items.span( isExprMI )
        val head = left match {
          case Nil => first
          case _ =>
            if ( isExprMI(left.last) )
              left.last.asInstanceOf[ExprMI].getExpr
            else {
              signal(left.last, "An expression is expected.")
              first
            }
        }
        right match {
        /* If there is any non-expression element (it cannot be the first element)
         * then replace the first such element and the element
         * immediately preceeding it (which must be an expression) with
         * a single element that does the appropriate operator application.
         * This new element is an expression.  Reassociate the resulting
         * sequence (which is one element shorter.)
         */
          case item::suffix => {
            val newExpr = item match {
              case SExponentiationMI(_,op,expr) =>
                ExprFactory.makeOpExpr(op, head, toJavaOption(expr))
              case SSubscriptingMI(_,op,exprs,sargs) =>
                ExprFactory.makeSubscriptExpr(span, head,
                                              toJavaList(exprs), some(op),
                                              toJavaList(sargs))
              case _ =>
                signal(item, "Non-expression element is expected.")
                head
            }
            left match {
              case Nil => associateMathItems(newExpr, suffix)
              case _ =>
                val exp = new NonParenthesisDelimitedMI(newExpr.getInfo, newExpr)
                associateMathItems(first, left.dropRight(1)++(exp::suffix))
            }
          }
          case _ => (first, items)
        }
      }

      // HANDLE THE FRONT ITEM
      val newFront = checkExpr(front)
      inferredType( newFront ) match {
        case None => noType(front); front
        case Some(t) =>
          // If front is a fn followed by an expr, we reassociate
          if ( TypesUtil.isArrows(t).asInstanceOf[Boolean] && isExprMI(second) ) {
            // It is a static error if either the argument is not parenthesized,
            expectParenedExprItem(second)
            // static error if the argument is immediately followed by
            // a non-expression element.
            remained match {
              case hd::tl => expectExprMI(hd)
              case Nil =>
            }
            // Otherwise, make a new MathPrimary that is one element shorter,
            // and recur.
            val fn = ExprFactory.make_RewriteFnApp(front,
                                                   second.asInstanceOf[ExprMI].getExpr)
            checkExpr(SMathPrimary(info, multi, infix, fn, remained))
          // THE FRONT ITEM WAS NOT A FN FOLLOWED BY AN EXPR, REASSOCIATE REST
          } else {
            val (head, tail) = associateMathItems( newFront, rest )
            // Otherwise, left-associate the sequence, which has only expression
            // elements, only the last of which may be a function.
            val newTail = tail.map( (e:MathItem) =>
                                    if ( ! isExprMI(e) ) {
                                      signal(e, "An expression is expected.")
                                      ExprFactory.makeVoidLiteralExpr(span)
                                    } else e.asInstanceOf[ExprMI].getExpr )
            // Treat the sequence that remains as a multifix application of
            // the juxtaposition operator.
            // The rules for multifix operators then apply.
            val multi_op_expr = checkExpr( ExprFactory.makeOpExpr(span, multi,
                                                                  toJavaList(head::newTail)) )
            inferredType(multi_op_expr) match {
              case Some(_) => multi_op_expr
              case None =>
                newTail.foldLeft(head){ (r:Expr, e:Expr) =>
                                        ExprFactory.makeOpExpr(NodeUtil.spanTwo(r, e),
                                                               infix, r, e) }
            }
          }
      }
    }

    case s@SSubscriptExpr(info, obj, subs, _, _) => {
      val newObj = checkExpr(obj)
      val newSubs = subs.map(checkExpr)
      // Ignore the op.  A subscript op behaves like a dotted method.
      // Make sure all sub-exprs are well-typed.
      if ( haveInferredTypes(newSubs) )
        inferredType(newObj) match {
          case Some(ty) => subscriptHelper(s, newObj, newSubs)
          case _ => noType(expr); expr
        }
      else {
        noType(expr); expr
      }
    }

    case SStringLiteralExpr(SExprInfo(span,parenthesized,_), text) =>
      SStringLiteralExpr(SExprInfo(span,parenthesized,Some(Types.STRING)), text)

    case SCharLiteralExpr(SExprInfo(span,parenthesized,_), text, charVal) =>
      SCharLiteralExpr(SExprInfo(span,parenthesized,Some(Types.CHAR)),
                       text, charVal)

    case SIntLiteralExpr(SExprInfo(span,parenthesized,_), text, intVal) =>
      SIntLiteralExpr(SExprInfo(span,parenthesized,Some(Types.INT_LITERAL)),
                      text, intVal)

    case SFloatLiteralExpr(SExprInfo(span,parenthesized,_), text, i, n, b, p) =>
      SFloatLiteralExpr(SExprInfo(span,parenthesized,Some(Types.FLOAT_LITERAL)),
                        text, i, n, b, p)

    case SVoidLiteralExpr(SExprInfo(span,parenthesized,_), text) =>
      SVoidLiteralExpr(SExprInfo(span,parenthesized,Some(Types.VOID)), text)

    /* ToDo for Compiled0
    case SFnRef(SExprInfo(span,paren,optType),
                sargs, depth, name, names, overloadings, types) => {
        expr
    }
    */

    case SDo(SExprInfo(span,parenthesized,_), fronts) => {
      val fs = fronts.map(checkExpr).asInstanceOf[List[Block]]
      if ( haveInferredTypes(fs) ) {
          // Get union of all clauses' types
          val frontTypes =
            fs.take(fs.size-1).foldRight(inferredType(fs.last).get)
              { (e:Expr, t:Type) => analyzer.join(inferredType(e).get, t) }
          SDo(SExprInfo(span,parenthesized,Some(frontTypes)), fs)
      } else noType(expr); expr
    }

    case SIf(SExprInfo(span,parenthesized,_), clauses, elseC) => {
      val newClauses = clauses.map( handleIfClause )
      val types = newClauses.map( (c: IfClause) => inferredType(c.getBody) match {
                                    case Some(ty) => ty
                                    case None => noType(c.getBody); Types.VOID
                                  } )
      val (newElse, newType) = elseC match {
        case None => {
          // Check that each if/elif clause has void type
          types.foreach( (ty: Type) =>
                         checkSubtype(ty, Types.VOID, expr,
                                      errorMsg("An 'if' clause without corresponding 'else' has type ",
                                               ty, " instead of type ().")) )
          (None, Types.VOID)
        }
        case Some(b) => {
          val newBlock = checkExpr(b).asInstanceOf[Block]
          inferredType(newBlock) match {
            case None => { noType(b) ; (None, Types.VOID) }
            case Some(ty) =>
              // Get union of all clauses' types
              (Some(newBlock), analyzer.join(toJavaList(ty::types)))
          }
        }
      }
      SIf(SExprInfo(span,parenthesized,Some(newType)), newClauses, newElse)
    }

    case SWhile(SExprInfo(span,parenthesized,_), testExpr, body) => {
      val (newTestExpr, bindings) = generatorClauseGetBindings(testExpr, true)
      val newBody = this.extend(bindings).checkExpr(body).asInstanceOf[Do]
      inferredType(newBody) match {
        case None => noType(body)
        case Some(ty) =>
          checkSubtype(ty, Types.VOID, body,
                       "Body of while loop must have type (), but had type " +
                       ty + ".")
      }
      SWhile(SExprInfo(span,parenthesized,Some(Types.VOID)), newTestExpr, newBody)
    }

    case SFor(SExprInfo(span,parenthesized,_), gens, body) => {
      val (newGens, bindings) = handleGens(gens)
      val newBody = this.extend(bindings).checkExpr(body).asInstanceOf[Block]
      inferredType(newBody) match {
        case None => noType(body)
        case Some(ty) =>
          checkSubtype(ty, Types.VOID, body,
                       "Body type of a for loop must have type () but has type " +
                       ty + ".")
      }
      SFor(SExprInfo(span,parenthesized,Some(Types.VOID)), newGens, newBody)
    }

    case v@SVarRef(SExprInfo(span,paren,_), id, sargs, depth) =>
      getTypeFromName(id) match {
        case Some(ty) =>
          if ( NodeUtil.isSingletonObject(v) )
            ty match {
              case typ@STraitType(STypeInfo(sp,pr,_,_), name, args, params) =>
                if ( NodeUtil.isGenericSingletonType(typ) &&
                     StaticTypeReplacer.argsMatchParams(toJavaList(sargs),
                                                        toJavaList(params),
                                                        analyzer).isSome ) {
                  // make a trait type that is GenericType instantiated
                  val newType = NodeFactory.makeTraitType(sp, pr, name,
                                                          toJavaList(sargs))
                  SVarRef(SExprInfo(span,paren,Some(newType)), id, sargs, depth)
                } else {
                  signal(v, "Unexpected type for a singleton object reference.")
                  v
                }
              case _ =>
                signal(v, "Unexpected type for a singleton object reference.")
                v
            }
          else SVarRef(SExprInfo(span,paren,Some(ty)), id, sargs, depth)
        case None => signal(id, "Type of the variable '" + id + "' not found."); v
      }

    case _ => throw new Error("Not yet implemented: " + expr.getClass)
    // "\n" + expr.toStringVerbose())
  }

}
