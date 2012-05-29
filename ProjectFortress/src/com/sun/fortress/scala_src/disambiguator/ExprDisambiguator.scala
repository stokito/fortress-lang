/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.scala_src.disambiguator;

import _root_.java.util.{ArrayList => JArrayList}
import _root_.java.util.{List => JList}
import _root_.java.util.{Set => JSet}
import edu.rice.cs.plt.collect.CollectUtil
import edu.rice.cs.plt.iter.IterUtil
import edu.rice.cs.plt.tuple.{Option => JOption}
import edu.rice.cs.plt.tuple.{Pair => JPair}
import com.sun.fortress.compiler.NamingCzar
import com.sun.fortress.compiler.disambiguator.LocalFnEnv
import com.sun.fortress.compiler.disambiguator.LocalVarEnv
import com.sun.fortress.compiler.disambiguator.NameEnv
import com.sun.fortress.compiler.index.DeclaredMethod
import com.sun.fortress.compiler.index.TraitIndex
import com.sun.fortress.compiler.index.TypeConsIndex
import com.sun.fortress.exceptions.StaticError
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.{ExprFactory => EF}
import com.sun.fortress.nodes_util.{NodeFactory => NF}
import com.sun.fortress.nodes_util.{NodeUtil => NU}
import com.sun.fortress.nodes_util.Span
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.Pairs._
import com.sun.fortress.scala_src.useful.Sets._
import com.sun.fortress.scala_src.useful.STypesUtil._
import com.sun.fortress.useful.HasAt
import com.sun.fortress.useful.Useful

/**
 * <p>Eliminates ambiguities in an AST that can be resolved solely by knowing what kind
 * of entity a name refers to.  This class assumes all types in declarations have been
 * resolved, and specifically handles the following:
 * <ul>
 * <li>All names referring to APIs are made fully qualified (FnRefs and OpRefs may then
 * contain lists of qualified names referring to multiple APIs).
 * <li>VarRefs referring to functions become FnRefs with placeholders for implicit static
 * arguments filled in (to be replaced later during type inference).</li>
 * <li>VarRefs referring to trait members, and that are juxtaposed with Exprs, become
 * MethodInvocations.  (Maybe?  Depends on parsing rules for getters.)</li>
 * <li>VarRefs referring to trait members become FieldRefs.</li>
 * <li>FieldRefs referring to trait members, and that are juxtaposed with Exprs, become
 * MethodInvocations.  (Maybe?  Depends on parsing rules for getters.)</li>
 * <li>FnRefs referring to trait members, and that are juxtaposed with Exprs, become
 * MethodInvocations.</li>
 * <li>TODO: StaticArgs of FnRefs, and types nested within them, are disambiguated.</li>
 * </ul>
 * <p/>
 * Additionally, all name references that are undefined or used incorrectly are
 * treated as static errors.  (TODO: check names in non-type static args)</p>
 */
class ExprDisambiguator(compilation_unit: CompilationUnit,
                        _env: NameEnv) extends Walker {
  var env = _env
  val errors = new JArrayList[StaticError]()
  private var types   = Set[String]()
  private var topVars = Set[String]()
  private var topFns  = Set[String]()
  private var labels = List[Id]()
  var uninitializedNames = Set[Id]()
  var inComponent = false
  var inTraitOrObject = false
  var inBlock = false

  def check() = walk(compilation_unit)
  def getErrors() = errors

  override def walk(node:Any):Any = {
    node match {
      case SComponent(_, name, _, _, comprises, _, _) =>
        if (comprises.isEmpty) {
          inComponent = true
          super.walk(node)
        } else node

      case SApi(_, _, _, _, comprises) =>
        if (comprises.isEmpty) super.walk(node)
        else node

      /* Declarations **********************************************************/

      /**
       * When recurring on a TraitDecl, we first need to extend the
       * environment with all the newly bound static parameters that
       * can be used in an expression context.
       * TODO: Handle variables bound in where clauses.
       */
      case STraitDecl(info@SASTNodeInfo(span),
                      STraitTypeHeader(sparams, mods, old_name, where,
                                       throwsC, contract, extendsC,
                                       old_params, decls),
                      self, excludes, comprises, ellipses) =>
        inTraitOrObject = true
        val name = old_name.asInstanceOf[Id]
        checkForShadowingType(name)
        types += name.getText
        // Check that wrapped fields must not have naked type variables.
        for (decl <- decls) decl match {
          case SVarDecl(_,lhs,_) =>
            for (lv <- lhs) lv match {
              case SLValue(_, lv_name, mods, Some(ty), mutable) => ty match {
                case SVarType(_, tyVar, _) =>
                  if (mods.isWrapped && env.typeConsIndex(tyVar) == null)
                    error("A wrapped field " + lv_name + " must not have " +
                          "a naked type variable " + tyVar + ".", tyVar)
                case _ =>
              }
              case _ =>
            }
          case _ =>
        }
        old_params match {
          case Some(ps) =>
/* Not yet checking for modifiers of the trait parameters
            for (param <- ps) {
              val mods = param.getMods
              if (!mods.isHidden && NU.isVarargsParam(param) )
                error("Varargs object parameters should not define getters.", param)
              if ( (mods.isSettable || mods.isVar) && NU.isVarargsParam(param) )
                error("Varargs object parameters should not define setters.", param)
              if (param.getMods.isWrapped) {
                toOption(param.getIdType) match {
                  case Some(ty) if ty.isInstanceOf[VarType] =>
                    val tyVar = ty.asInstanceOf[VarType].getName
                    if (env.typeConsIndex(tyVar) == null)
                      error("A wrapped field " + param.getName + " must not have " +
                            "a naked type variable " + tyVar + ".", tyVar)
                  case _ =>
                }
                toOption(param.getVarargsType) match {
                  case Some(ty) if ty.isInstanceOf[VarType] =>
                    val tyVar = ty.asInstanceOf[VarType].getName
                    if (env.typeConsIndex(tyVar) == null)
                      error("A wrapped field " + param.getName + " must not have " +
                            "a naked type variable " + tyVar + ".", tyVar)
                  case _ =>
                }
              }
            }
*/
          case _ =>
        }
        val old_env = env
        extendWithVars(extractStaticExprVars(sparams))
        val extendsClause = walk(extendsC).asInstanceOf[List[TraitTypeWhere]]
        val params = extractParamNames(old_params)
        // Include trait declarations and inherited methods
        val (vars, gettersAndSetters, fns) = partitionDecls(decls, params)
        val inheritedMs = inheritedMethods(extendsClause)
        // Do not extend the environment with "fields", getters, or setters in a trait.
        // References to all three must have an explicit receiver.
        extendWithVars(Set(NF.makeId(span, "self")))
        extendWithFields(joinFields(params, vars))
        extendWithMethods(inheritedMs ++ fns)
        val result = STraitDecl(info,
                                STraitTypeHeader(sparams, mods, name, where, throwsC,
                                                 contract, extendsClause,
                                                 walk(old_params).asInstanceOf[Option[List[Param]]],
                                                 walk(decls).asInstanceOf[List[Decl]]),
                                self, excludes, comprises, ellipses)
        env = old_env
        inTraitOrObject = false
        result

      /**
       * When recurring on an ObjectDecl, we first need to extend the
       * environment with all the newly bound static parameters that can
       * be used in an expression context, along with all the object parameters.
       * TODO: Handle variables bound in where clauses.
       * TODO: Insert inherited method names into the environment.
       */
      case SObjectDecl(info@SASTNodeInfo(span),
                       STraitTypeHeader(sparams, mods, old_name, where,
                                        throwsC, contract, extendsC,
                                        old_params, decls),
                       self) =>
        inTraitOrObject = true
        val name = old_name.asInstanceOf[Id]
        checkForShadowingType(name)
        types += name.getText
        // Check that wrapped fields must not have naked type variables.
        for (decl <- decls) decl match {
          case SVarDecl(_,lhs,_) =>
            for (lv <- lhs) lv match {
              case SLValue(_, lv_name, mods, Some(ty), mutable) => ty match {
                case SVarType(_, tyVar, _) =>
                  if (mods.isWrapped && env.typeConsIndex(tyVar) == null)
                    error("A wrapped field " + lv_name + " must not have " +
                          "a naked type variable " + tyVar + ".", tyVar)
                case _ =>
              }
              case _ =>
            }
          case _ =>
        }
        old_params match {
          case Some(ps) =>
            checkForShadowingTopFunction(name)
            topFns += name.getText
            checkForValidParams(extractParamNames(ps))
            for (param <- ps) {
              val mods = param.getMods
              if (!mods.isHidden && NU.isVarargsParam(param) )
                error("Varargs object parameters should not define getters.", param)
              if ( (mods.isSettable || mods.isVar) && NU.isVarargsParam(param) )
                error("Varargs object parameters should not define setters.", param)
              if (param.getMods.isWrapped) {
                toOption(param.getIdType) match {
                  case Some(ty) if ty.isInstanceOf[VarType] =>
                    val tyVar = ty.asInstanceOf[VarType].getName
                    if (env.typeConsIndex(tyVar) == null)
                      error("A wrapped field " + param.getName + " must not have " +
                            "a naked type variable " + tyVar + ".", tyVar)
                  case _ =>
                }
                toOption(param.getVarargsType) match {
                  case Some(ty) if ty.isInstanceOf[VarType] =>
                    val tyVar = ty.asInstanceOf[VarType].getName
                    if (env.typeConsIndex(tyVar) == null)
                      error("A wrapped field " + param.getName + " must not have " +
                            "a naked type variable " + tyVar + ".", tyVar)
                  case _ =>
                }
              }
            }
          case _ =>
            checkForShadowingTopVariable(name)
            topVars += name.getText
        }
        val old_env = env
        extendWithVars(extractStaticExprVars(sparams))
        val extendsClause = walk(extendsC).asInstanceOf[List[TraitTypeWhere]]
        val params = extractParamNames(old_params)
        // Include trait declarations and inherited methods
        val (vars, gettersAndSetters, fns) = partitionDecls(decls, params)
        val inheritedMs = inheritedMethods(extendsClause)
        val fields = params ++ vars
        // Do not extend the environment with "fields", getters, or setters in a trait.
        // References to all three must have an explicit receiver.
        extendWithVars(Set(NF.makeId(span, "self")))
        extendWithFields(joinFields(params, vars))
        extendWithMethods(inheritedMs ++ fns)
        val result = SObjectDecl(info,
                                 STraitTypeHeader(sparams, mods, name, where, throwsC,
                                                  walk(contract).asInstanceOf[Option[Contract]],
                                                  extendsClause,
                                                  walk(old_params).asInstanceOf[Option[List[Param]]],
                                                  walk(decls).asInstanceOf[List[Decl]]),
                                 self)
        env = old_env
        inTraitOrObject = false
        result

      /**
       * When recurring on a FnDecl, we first need to extend the
       * environment with all the newly bound static parameters that
       * can be used in an expression context, along with all function
       * parameters and 'self' if it is a method.
       * TODO: Handle variables bound in where clauses.
       */
      case fd@SFnDecl(info,
                      SFnHeader(sparams, mods, old_name, where, throwsC, contract,
                                params, returnType),
                      uname, body, imp_name) =>
        val name = old_name.asInstanceOf[IdOrOp]
        var functional = false
        for (p <- params) {
          if (p.getName.equals(NamingCzar.SELF_NAME)) {
            if (functional)
              error("Parameter 'self' appears twice in a method declaration.", fd)
            functional = true
          }
        }
        if (!inBlock && (!inTraitOrObject || functional)) {
          checkForShadowingTopFunction(name)
          topFns += name.getText
        }
        val old_env = env
        val param_names = extractParamNames(params)
        checkForValidParams(param_names)
        extendWithVars(extractStaticExprVars(sparams) ++ param_names)
        // No need to recur on the name, as we will not modify it and we have already checked
        // for shadowing in forObjectDecl. Also, if this FnDecl is a getter, we allow it
        // to share its name with a field, so blindly checking for shadowing at this point
        // doesn't work.
        val result = SFnDecl(info,
                             SFnHeader(sparams, mods, name, where, throwsC,
                                       walk(contract).asInstanceOf[Option[Contract]],
                                       walk(params).asInstanceOf[List[Param]],
                                       returnType),
                             uname, walk(body).asInstanceOf[Option[Expr]], imp_name)
        env = old_env
        result

      case SVarDecl(info, lhs, init) if (!inTraitOrObject) =>
        lhs.foreach(l => l match {
                    case SLValue(_,name,_,_,_) =>
                      checkForShadowingTopVariable(name); topVars += name.getText
                   })
        super.walk(node)

      /**
       * Currently we don't do any disambiguation of dimension, unit, and grammar
       * declarations.
       */
      case _:DimDecl => node
      case _:UnitDecl => node
      case _:GrammarDecl => node

      /* Expressions ***********************************************************/
      /**
       * When recurring on an ObjExpr, we first need to extend the
       * environment with all the newly bound variables and methods
       * TODO: Insert inherited method names into the environment.
       */
      case SObjectExpr(info@SASTNodeInfo(span),
                       STraitTypeHeader(sparams, mods, name, where,
                                        throwsC, contract, extendsC,
                                        params, decls),
                       self) =>
        val extendsClause = walk(extendsC).asInstanceOf[List[TraitTypeWhere]]
        // Include trait declarations and inherited methods
        val (vars, gettersAndSetters, fns) = partitionDecls(decls, Set[Id]())
        val inheritedMs = inheritedMethods(extendsClause)
        val old_env = env
        extendWithVars(Set(NF.makeId(span, "self")))
        extendWithFields(vars)
        extendWithMethods(inheritedMs ++ fns)
        val result = SObjectExpr(info,
                                 STraitTypeHeader(sparams, mods, name, where, throwsC,
                                                  contract, extendsClause, params,
                                                  walk(decls).asInstanceOf[List[Decl]]),
                                 self)
        env = old_env
        result

      /**
       * When recurring on a FnExpr, we first need to extend the environment
       * with any parameters.
       */
      case SFnExpr(info,
                   SFnHeader(sparams, mods, name, where, throwsC, contract,
                             params, returnType), body) =>
        val old_env = env
        val param_names = extractParamNames(params)
        checkForValidParams(param_names)
        extendWithVars(param_names)
        val result = SFnExpr(info, SFnHeader(sparams, mods, name, where, throwsC,
                                             walk(contract).asInstanceOf[Option[Contract]],
                                             walk(params).asInstanceOf[List[Param]],
                                             returnType), walk(body).asInstanceOf[Expr])
        env = old_env
        result

      /**
       * VarRefs can be made qualified or translated into FnRefs.
       */
      case vref@SVarRef(info, id, sargs, depth) =>
        val span = NU.getSpan(vref)
        var name = id
        if (NU.isSingletonObject(vref)) { // singleton object
          var objs = env.explicitTypeConsNames(name)
          if (objs.isEmpty) objs = env.onDemandTypeConsNames(name)
          if (objs.isEmpty) vref
          else if (objs.size == 1)
            SVarRef(info, objs.iterator.next, sargs, depth)
          else {
            error("Name may refer to " + NU.namesString(objs) + ".", vref); vref
          }
        } else {
          var api = toOption(name.getApiName)
          var fields = List[Id]()
          var result: Option[Expr] = None
          // First, try to interpret it as a qualified name
          while (!result.isDefined && api.isDefined) {
            val givenApiName = api.get
            toOption(env.apiName(givenApiName)) match {
              case Some(realApiName) =>
                val newId = NF.makeId(realApiName, name)
                if (env.hasQualifiedVariable(newId)) {
                  if (fields.isEmpty && givenApiName == realApiName)
                    // no change -- no need to recreate the VarRef
                    return vref
                  else result = Some(SVarRef(info, newId, sargs, depth))
                } else if (env.hasQualifiedFunction(newId)) {
                  result = Some(SFnRef(info, sargs, depth, name, List(name),
                                       List(), List(), None, None))
                  // TODO: insert correct number of to-infer arguments?
                } else {
                  error("Unrecognized name: " + NU.nameString(name), vref)
                  return vref
                }
              case None =>
                // shift all names to the right, and try a smaller api name
                fields = name :: fields
                val ids = toListFromImmutable(givenApiName.getIds)
                name = ids.last
                api = ids.dropRight(1) match {
                  case Nil => None
                  case prefix => Some(NF.makeAPIName(span, toJavaList(prefix)))
                }
            }
          }
          // Second, try to interpret it as an unqualified name.
          if (!result.isDefined) { // api.isDefined must be false
            (env.explicitVariableNames(name),
             env.explicitFunctionNames(name),
             env.explicitTypeConsNames(name)) match {
              case (vars, fns, objs) => (vars.size, fns.size, objs.size) match {
                case (1, 0, 0) =>
                  val newName = vars.iterator.next
                  if (newName.getApiName.isNone && newName == name && fields.isEmpty) {
                    // no change -- no need to recreate the VarRef
                    return vref
                  } else result = Some(SVarRef(info, newName, sargs, depth))
                case (0, f, _) if f > 0 =>
                  val new_fns = toSet(fns).toList
                  val unambiguous_fns = toSet(env.unambiguousFunctionNames(name)).toList
                  // Create a list of overloadings for this FnRef from the
                  // matching function names.
                  // TODO: insert correct number of to-infer arguments?
                  if (false && (new_fns.size > 1 || unambiguous_fns.size > 1))
                      System.err.println(info.getSpan+":\n  new_fns "+new_fns+"\n  unamb_fns "+unambiguous_fns);
                  result = Some(SFnRef(info, sargs, depth, name, new_fns,
                                       new_fns.map(new Overloading(info, _, name, None)),
                                       unambiguous_fns.map(new Overloading(info, _, name, None)),
                                       None, None))
                case (1, 0, 1) =>
                  result = Some(SVarRef(info, objs.iterator.next, sargs, depth))
                case (v, f, o) if v == 0 && f == 0 && o == 0 =>
                  // Turn off error message on this branch until we can ensure
                  // that the VarRef doesn't resolve to an inherited method.
                  // For now, assume it does refer to an inherited method.
                  if (fields.isEmpty && !topVars.contains(name.getText)
                      && !topFns.contains(name.getText)) {
                    // no change -- no need to recreate the VarRef
                    error("Variable " + name + " is not defined.", name)
                    return vref
                  } else result = Some(SVarRef(info, name, sargs, depth))
                case _ =>
                  return vref
              }
            }
          }
          // result.isDefined is now true
          var new_node = result.get
          for (field <- fields) { new_node = SFieldRef(info, new_node, field) }
          new_node
        }

      case fnref@SFnRef(info, sargs, depth, name, names, ovl, newOvl, _, _) =>
        // Many FnRefs will be covered by the VarRef case, since many functions
        // are parsed as variables.  FnRefs can be parsed if, for example,
        // explicit static arguments are provided.  These function references
        // must still be disambiguated.
        val fn_name = names.head
        val fns = env.explicitFunctionNames(fn_name)
        if (fns.isEmpty)
          fn_name match {
            case id:Id => // Could be a singleton object with static arguments.
            if (env.explicitTypeConsNames(id).isEmpty && !topFns.contains(id.getText)) {
                error("Function " + id + " is not defined.", fnref); fnref
              } else // create _RewriteObjectRef -- as var ref, or as expr?
                walk(SVarRef(info, id, sargs, depth)).asInstanceOf[VarRef]
            case _ => node
          }
        else {
          val new_fns = toSet(fns).toList
          val unambiguous_fns = toSet(env.unambiguousFunctionNames(fn_name)).toList
          SFnRef(info, sargs, depth, fn_name.asInstanceOf[Id], new_fns,
                 // Create a list of overloadings for this FnRef from the matching
                 // function names.
                 new_fns.map(new Overloading(info, _, fn_name, None)),
                 unambiguous_fns.map(new Overloading(info, _, fn_name, None)), None, None)
        }

      case opref@SOpRef(info, sargs, depth, name, names, ovl, newOvl, _, _) =>
        opRefHelper(opref).getOrElse {
          // Make sure to populate the 'originalName' field.
          SOpRef(info, sargs, depth, names.head, names, ovl, newOvl, None, None)
        }

      // OpExpr checks to make sure its OpRef can be disambiguated, since
      // forOpRef will not automatically report an error.
      case SOpExpr(info, op, args) =>
        val new_op = opRefHelper(op).getOrElse {
          error("Operator " + op.getNames.get(0).stringName + " is not defined.", op)
          walk(op).asInstanceOf[FunctionalRef]
        }
        SOpExpr(info, new_op, walk(args).asInstanceOf[List[Expr]])

      case SLabel(info, name, body) =>
        checkForShadowingLabel(name)
        val old_labels = labels
        labels = name :: labels
        val new_block = walk(body).asInstanceOf[Block]
        labels = old_labels
        SLabel(info, name, new_block)

      case exit@SExit(info, target, returnExpr) =>
        val new_target = target match {
          case Some(_) => target
          case None =>
            if (labels.isEmpty) {
              error("Exit occurs outside of a label.", exit); None
            } else Some(labels.head)
        }
        val new_return = returnExpr match {
          case Some(expr) => Some(walk(expr).asInstanceOf[Expr])
          case None => Some(EF.makeVoidLiteralExpr(NU.getSpan(exit)))
        }
        SExit(info, new_target, new_return)

      case SCatch(info, name, clauses) =>
        val old_env = env
        extendWithVars(Set(name))
        val result = SCatch(info, name,
                            walk(clauses).asInstanceOf[List[CatchClause]])
        env = old_env
        result

      /**
       * Contracts are implicitly allowed to refer to a variable, "outcome."
       */
      case SContract(info@SASTNodeInfo(span), require, ensures, invariants) =>
        val old_env = env
        extendWithVars(Set(NF.makeId(span, "outcome")))
        val result = SContract(info, walk(require).asInstanceOf[Option[List[Expr]]],
                               walk(ensures).asInstanceOf[Option[List[EnsuresClause]]],
                               walk(invariants).asInstanceOf[Option[List[Expr]]])
        env = old_env
        result

      // Accumulator can bind variables
      case SAccumulator(info, sargs, accOp, gens, body) =>
        val old_env = env
        val (new_gens, bindings) = bindInListGenClauses(gens)
        extendWithVars(bindings)
        val result = SAccumulator(info, sargs, accOp, new_gens,
                                  walk(body).asInstanceOf[Expr])
        env = old_env
        result

      /**
       * An if clause has a generator that can potentially create a new binding.
       * Here we must extend the context.
       */
      case SIfClause(info, gen, body) =>
        val old_env = env
        val (new_gens, bindings) = bindInListGenClauses(List(gen))
        extendWithVars(bindings)
        val result = SIfClause(info, new_gens.head, walk(body).asInstanceOf[Block])
        env = old_env
        result

      /**
       * for loops have generator clauses that bind in later generator clauses.
       */
      case SFor(info, gens, body) =>
        val old_env = env
        val (new_gens, bindings) = bindInListGenClauses(gens)
        extendWithVars(bindings)
        val result = SFor(info, new_gens, walk(body).asInstanceOf[Block])
        env = old_env
        result

      /**
       * While loops have generator clauses that can bind variables in the body.
       */
      case SWhile(info, gen, body) =>
        val old_env = env
        val (new_gens, bindings) = bindInListGenClauses(List(gen))
        extendWithVars(bindings)
        val result = SWhile(info, new_gens.head, walk(body).asInstanceOf[Do])
        env = old_env
        result

      /**
       * Typecase can bind new variables in the clauses.
       */
      case STypecase(info, bindExpr, clauses, elseClause) =>
        val old_env = env
        val new_expr = walk(bindExpr).asInstanceOf[Expr]
        val new_else = elseClause match {
          case Some(block) => Some(walk(block).asInstanceOf[Block])
          case None => None
        }
        val result = STypecase(info, new_expr,
                               walk(clauses).asInstanceOf[List[TypecaseClause]],
                               new_else)
        env = old_env
        result

      case STypecaseClause(info, nameOpt, matchType, body) =>
        val old_env = env
        if (nameOpt.isDefined) extendWithVars(Set(nameOpt.get))

        def extendWithPattern(tp : TypeOrPattern) : Unit = {
          def extendWithId(pb : PatternBinding) = pb match {
            case SPlainPattern(_, _, name, _, Some(idType)) =>
              extendWithVars(Set(name))
              extendWithPattern(idType)
            case SPlainPattern(_, _, name, _, None) =>
              extendWithVars(Set(name))
            case t:TypePattern => ()
            case SNestedPattern(_, _, pat) =>
              extendWithPattern(pat)
          }
          if(tp.isInstanceOf[Pattern]){
            val pattern = tp.asInstanceOf[Pattern]
            val ps = toList(pattern.getPatterns.getPatterns)
            ps.map(extendWithId)
          }
          else ()
        }

        extendWithPattern(matchType)
        val new_body = walk(body).asInstanceOf[Block]
        val result = STypecaseClause(info, nameOpt, matchType, new_body)
        env = old_env
        result

      /**
       * LocalVarDecls introduce local variables while visiting the body.
       */
      case SLocalVarDecl(info, body, lhs, rhs) =>
        val old_env = env
        val old_uninitialized = uninitializedNames
        val new_lhs = walk(lhs).asInstanceOf[List[LValue]]
        val new_rhs = walk(rhs).asInstanceOf[Option[Expr]]
        val definedNames = extractDefinedVarNames(new_lhs)
        // Record uninitialized local variables so that:
        //   1. We can check that these variables are initialized before use.
        //   2. We don't signal shadowing errors when they are initialized.
        if (!new_rhs.isDefined) uninitializedNames ++= definedNames
        extendWithVars(definedNames, uninitializedNames)
        val result = SLocalVarDecl(info, walk(body).asInstanceOf[Block],
                                   new_lhs, new_rhs)
        env = old_env
        uninitializedNames = old_uninitialized
        result

      /**
       * LetFns introduce local functions in scope within the body.
       */
      case SLetFn(info, body, fns) =>
        inBlock = true
        val old_env = env
        val definedDecls = fns.toSet
        for (fn <- fns) {
          NU.getName(fn) match {
            case name:Id =>
              if (!env.explicitVariableNames(name).isEmpty ||
                  !env.explicitFunctionNames(name).isEmpty)
                error("Local function " + name + " is already declared.", name)
            case _ =>
          }
        }
        extendWithFnsNoCheck(definedDecls)
        val result = SLetFn(info, walk(body).asInstanceOf[Block],
                            walk(fns).asInstanceOf[List[FnDecl]])
        env = old_env
        inBlock = false
        result

      case _:Type => node

      case _ => super.walk(node)
    }
  }

  private def error(s:String, n:HasAt) = errors.add(StaticError.make(s,n))

  private def extendWithMethods(definedDecls: Set[FnDecl]): Unit =
    extendWithFns(definedDecls,
                  extractDefinedFnNames(definedDecls.filter(!NU.isFunctionalMethod(_)))
                  .map(_.asInstanceOf[IdOrOp].getText))

  private def extendWithFns(definedDecls: Set[FnDecl],
                            allowedShadowings: Set[String]): Unit = {
    checkForShadowingFunctions(extractDefinedFnNames(definedDecls), allowedShadowings)
    extendWithFnsNoCheck(definedDecls)
  }

  private def extendWithFnsNoCheck(definedFunctions: Set[FnDecl]) =
    env = new LocalFnEnv(env, toJavaSet(definedFunctions))

  private def extendWithFields(vars: Set[Id]) =
    extendWithVarsNoCheck(vars)

  private def extendWithVars(vars: Set[Id]) = {
    checkForShadowingVars(vars); extendWithVarsNoCheck(vars)
  }

  private def extendWithVarsNoCheck(vars: Set[Id]) =
    env = new LocalVarEnv(env, toJavaSet(vars))

  private def extendWithVars(vars: Set[Id], uninitialized: Set[Id]) = {
    checkForShadowingVars(vars); extendWithVarsNoCheck(vars, uninitialized)
  }

  private def extendWithVarsNoCheck(vars: Set[Id], uninitialized: Set[Id]) = {
    env = new LocalVarEnv(env, toJavaSet(vars))
    uninitializedNames = uninitializedNames ++ uninitialized
  }

  /**
   * Check that the function corresponding to the given Id does not shadow any variables
   * in scope.
   */
  private def checkForShadowingFunction(v: Id, allowedShadowings: Set[String]) =
    if (!env.explicitVariableNames(v).isEmpty &&
        !allowedShadowings.contains(v.getText) &&
        !NU.isUnderscore(v))
      error("Function " + v + " is already declared.", v)

  private def checkForShadowingFunctions(definedNames: Set[IdOrOpOrAnonymousName],
                                         allowedShadowings: Set[String]) =
    for (name <- definedNames) name match {
      case id:Id => checkForShadowingFunction(id, allowedShadowings)
      case _ =>
    }

  private def checkForValidParams(params: Set[Id]) =
    for (param <- params; if param.getText.equals("outcome"))
      error("Parameters must not be named `outcome'", param)

  /**
   * Check that the variable corresponding to the give Id does not shadow any variables or
   * functions in scope.
   */
  private def checkForShadowingVar(v: Id) = {
    val text = v.getText
    if (!text.equals("self") && !text.equals("_") && !text.equals("outcome") &&
        !uninitializedNames.contains(v)) {
      if ((!env.explicitVariableNames(v).isEmpty ||
           !env.explicitFunctionNames(v).isEmpty) &&
          !NU.isUnderscore(v))
          {
              /* for debugging
              for (id <- toSet(env.explicitVariableNames(v)) ) {
                  System.err.println("explicitVariableNames " + id.toStringVerbose)
              }
              for (id <- toSet(env.explicitFunctionNames(v)) ) {
                  System.err.println("explicitFunctionNames " + id.toStringVerbose)
              }
              */
        error("Variable " + v + " is already declared.", v)
          }
    }
  }

  private def checkForShadowingVars(vars: Set[Id]) = {
    // First check that vars do not shadow other declarations in scope
    for (v <- vars) { checkForShadowingVar(v) }
    // Now check that these vars do not conflict.
    // We could speed up asymptotic complexity by sorting first.
    // But vars is expected to be relatively small,
    // so the overhead of sorting probably isn't worth it.
    // A single var has nothing to conflict with.
    if (vars.size > 1) {
      val _vars = vars.toArray
      for {i <- 0 until vars.size - 2
           j <- i + 1 until vars.size -1} {
        if (_vars(i).equals(_vars(j)) && !NU.isUnderscore(_vars(i)))
          error("Variable " + _vars(i) + " is already declared at " +
                NU.getSpan(_vars(j).asInstanceOf[Id]),
                _vars(i).asInstanceOf[Id])
      }
    }
  }

  /**
   * Check that the type corresponding to the give Id does not shadow any types
   * in scope.
   */
  private def checkForShadowingType(ty: Id) =
    if (types.contains(ty.getText) && !NU.isUnderscore(ty))
      error("Type " + ty + " is already declared.", ty)

  private def checkForShadowingTopVariable(v: Id) =
    if ((topVars.contains(v.getText) ||
         topFns.contains(v.getText)) &&
        !NU.isUnderscore(v))
      error("Top-level variable " + v + " is already declared.", v)

  private def checkForShadowingTopFunction(f: IdOrOp) =
    if (topVars.contains(f.getText) && !NU.isUnderscore(f.asInstanceOf[Id]))
      error("Top-level variable " + f + " is already declared.", f)

  /**
   * Check that the label corresponding to the give Id does not shadow any labels
   * in scope.
   */
  private def checkForShadowingLabel(label: Id) =
    if (labels.exists(_.getText.equals(label.getText)) &&
        !NU.isUnderscore(label))
      error("Label " + label + " is already declared.", label)

  /**
   * Pull out all static variables that can be used in expression contexts,
   * and return them as a Set<Id>.
   * TODO: Collect OpParams as well.
   */
  private def extractStaticExprVars(sparams: List[StaticParam]) = {
    var result = Set[Id]()
    for (sp <- sparams;
         if !NU.isDimParam(sp) && !NU.isOpParam(sp)) {
      result += sp.getName.asInstanceOf[Id]
    }
    result
  }

  /**
   * Convenience method that unwraps its argument and passes it
   * to the overloaded definition of extractParamNames on lists.
   */
  private def extractParamNames(params: Option[List[Param]]): Set[Id] =
    if (params.isDefined) extractParamNames(params.unwrap) else Set[Id]()

  /**
   * Returns a list of Ids of the given list of Params.
   */
  private def extractParamNames(params: List[Param]): Set[Id] = {
    var result = Set[Id]()
    for (param <- params) { result = result + param.getName }
    result
  }

  private def extractDefinedVarNames(lvalues: List[LValue]): Set[Id] =
    extractDefinedVarNames(lvalues, Set[Id]())

  private def extractDefinedVarNames(lvalues: List[LValue],
                                     set: Set[Id]): Set[Id] = {
    var result = set
    for (lv <- lvalues) {
      var valid = true
      val id = lv.getName
      valid = (!result.contains(id) || id.getText.equals("_"))
      result += id
      if (!valid) error("Duplicate local variable name: " + id, lv)
    }
    result
  }

  /**
   * Disambiguates an OpRef, but instead of reporting an error if it cannot be
   * disambiguated, it returns NONE, which other methods can then use to decide
   * if they want to report an error.
   */
  private def opRefHelper(op: FunctionalRef) = op match {
    case SOpRef(info, sargs, depth, _, names, oldOvl, overloadings, _, _) =>
      val op_name = names.head.asInstanceOf[Op]
      val parametric_ops: Set[(IdOrOp, IdOrOp)] = 
        toSet(env.getParametricOperators).map(toPair(_))
      val explicit_ops: List[(IdOrOp, IdOrOp)] = 
        (toSet(env.explicitFunctionNames(op_name)).map((op_name, _)) ++
          parametric_ops.map{case (a, b) => (a, a)}).toList
      val unambiguous_ops: List[(IdOrOp, IdOrOp)] =
        (toSet(env.unambiguousFunctionNames(op_name)).map((op_name, _)) ++
          parametric_ops).toList
      if (explicit_ops.isEmpty && unambiguous_ops.isEmpty)
        None
      else {
        Some(SOpRef(info, sargs, depth, op_name,
                    explicit_ops.map(_._1),
                    explicit_ops.map{case (a,b) => SOverloading(info, b, a, None, None)},
                    unambiguous_ops.map{case (a,b) => SOverloading(info, b, a, None, None)},
                    None, None))
      }
      case _ => None
  }

  private def bindInListGenClauses(gens: List[GeneratorClause]) =
    gens.foldLeft((List[GeneratorClause](), Set[Id]()))
                 {(pair, gen) => (pair, gen) match {
                   case ((old_gens, bindings),
                         SGeneratorClause(info, bind, init)) =>
                     val old_env = env
                     // given the bindings thus far, rebuild the current
                     // GeneratorClause with the new bindings
                     // pass along that generator's bindings.
                     extendWithVars(bindings)
                     val new_gen = walk(gen).asInstanceOf[GeneratorClause]
                     env = old_env
                     (old_gens ::: List(new_gen), bindings ++ bind.toSet)
                 }}

  // multiple instances of the same name are allowed
  private def extractDefinedFnNames(fns: Set[FnDecl]) = fns.map(NU.getName(_))

  /**
   * Partition decls into three sets.  Partition all the given decls into three
   * sets: a set of variable Ids, a set of Ids for accessors, and a set of
   * FnDecls for other functions.
   */
  private def partitionDecls(decls: List[Decl], paramFields: Set[Id]) = {
    val (vars, gettersAndSetters, getterAndSetterIds, fns) =
      decls.foldLeft((Set[Id](), Set[FnDecl](), Set[Id](), Set[FnDecl]()))
                    {(res, decl) => (res, decl) match {
                       case ((vars, accessors, accessorIds, fns),
                             fd@SFnDecl(_,
                                        SFnHeader(_,mods,name,_,_,_,params,_),_,_,_)) =>
                         if (mods.isGetterSetter) {
                           name match {
                             case id:Id =>
                               if (accessorIds.contains(id) &&
                                   accessors.exists(d => NU.getMods(d).equals(mods) &&
                                                         NU.getName(d).equals(id))) {
                                 error("Getter/setter declarations should not be overloaded.", fd)
                               }
                               (vars, accessors + fd, accessorIds + id, fns)
                             case _ =>
                               error("Getter/setter declared with an operator name, '" +
                                     NU.nameString(name) + "'", fd)
                               (vars, accessors, accessorIds, fns)
                           }
                         // Don't add functional methods!  They go at the top level.
                         } else if (!NU.isFunctionalMethod(toJavaList(params)))
                           (vars, accessors, accessorIds, fns + fd)
                         else (vars, accessors, accessorIds, fns)
                       case ((vars, accessors, accessorIds, fns), vd@SVarDecl(_, lhs, _)) =>
                         (joinFields(vars, extractDefinedVarNames(lhs)),
                          accessors, accessorIds, fns)
                       case _ => res
                     }}
    val fnNames = fns.flatMap((fd: FnDecl) => NU.getName(fd) match {
                                              case fname: Id => Set(fname.getText)
                                              case _ => Set[String]() })
    for (id <- paramFields ++ vars ++ getterAndSetterIds if (fnNames.contains(id.getText)))
        error("Getter/setter declarations should not be overloaded with method declarations.", id)
    (vars, getterAndSetterIds, fns)
  }

  private def joinFields(vars: Set[Id], new_vars: Set[Id]) =
    vars.intersect(new_vars).toList match {
      case Nil => vars ++ new_vars
      case list =>
        val elem = list.head
        if (!NU.isUnderscore(elem))
          error("Field " + elem + " is already declared.", elem)
        vars
    }

  /**
   * Given a list of TraitTypeWhere that some trait or object extends,
   * this method returns a pair of sets of getters/setter ids and method declarations that
   * the trait receives through inheritance. The implementation of this method is somewhat
   * involved, since at this stage of compilation, not all types are
   * fully formed. (In particular, types in extends clauses of types that
   * are found in the GlobalEnvironment.)
   *
   * @param extended_traits
   * @return
   */
  private def inheritedMethods(extended_traits: List[TraitTypeWhere]): Set[FnDecl] = {
    exprInheritedMethods(extended_traits, env)(error)
  }

}
