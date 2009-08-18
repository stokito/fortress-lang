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

import _root_.java.lang.{Iterable => JavaIterable}
import _root_.java.util.ArrayList
import _root_.java.util.{HashMap => JavaHashMap}
import _root_.java.util.{HashSet => JavaHashSet}
import _root_.java.util.LinkedList
import _root_.java.util.{List => JavaList}
import _root_.java.util.{Map => JavaMap}
import _root_.java.util.{Set => JavaSet}
import edu.rice.cs.plt.tuple.{Option => JavaOption}
import edu.rice.cs.plt.collect.IndexedRelation
import edu.rice.cs.plt.collect.Relation
import scala.collection.mutable.HashSet
import scala.collection.mutable.Set

import com.sun.fortress.compiler.GlobalEnvironment
import com.sun.fortress.compiler.NamingCzar
import com.sun.fortress.compiler.StaticPhaseResult
import com.sun.fortress.compiler.index.ApiIndex
import com.sun.fortress.compiler.index.CompilationUnitIndex
import com.sun.fortress.compiler.index.ComponentIndex
import com.sun.fortress.compiler.index.Coercion
import com.sun.fortress.compiler.index.{Constructor => JavaConstructor}
import com.sun.fortress.compiler.index.{DeclaredFunction => JavaDeclaredFunction}
import com.sun.fortress.compiler.index.{DeclaredMethod => JavaDeclaredMethod}
import com.sun.fortress.compiler.index.{FieldGetterMethod => JavaFieldGetterMethod}
import com.sun.fortress.compiler.index.{FieldSetterMethod => JavaFieldSetterMethod}
import com.sun.fortress.compiler.index.{DeclaredVariable => JavaDeclaredVariable}
import com.sun.fortress.compiler.index.{Dimension => JavaDimension}
import com.sun.fortress.compiler.index.{Function => JavaFunction}
import com.sun.fortress.compiler.index.{FunctionalMethod => JavaFunctionalMethod}
import com.sun.fortress.compiler.index.{ParamVariable => JavaParamVariable}
import com.sun.fortress.compiler.index.{SingletonVariable => JavaSingletonVariable}
import com.sun.fortress.compiler.index.{Unit => JavaUnit}
import com.sun.fortress.compiler.index.GrammarIndex
import com.sun.fortress.compiler.index.Method
import com.sun.fortress.compiler.index.NonterminalDefIndex
import com.sun.fortress.compiler.index.NonterminalExtendIndex
import com.sun.fortress.compiler.index.NonterminalIndex
import com.sun.fortress.compiler.index.ObjectTraitIndex
import com.sun.fortress.compiler.index.ParametricOperator
import com.sun.fortress.compiler.index.ProperTraitIndex
import com.sun.fortress.compiler.index.TraitIndex
import com.sun.fortress.compiler.index.TypeConsIndex
import com.sun.fortress.compiler.index.Variable
import com.sun.fortress.compiler.typechecker.TypeEnv
import com.sun.fortress.exceptions.InterpreterBug.bug
import com.sun.fortress.exceptions.StaticError
import com.sun.fortress.nodes_util.Modifiers
import com.sun.fortress.nodes_util.{NodeFactory => NF}
import com.sun.fortress.nodes_util.{NodeUtil => NU}
import com.sun.fortress.nodes_util.Span
import com.sun.fortress.nodes._
import com.sun.fortress.scala_src.linker.ApiLinker
import com.sun.fortress.scala_src.linker.CompoundApiChecker
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.useful.Iterators._
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.useful.HasAt
import com.sun.fortress.useful.NI

object IndexBuilder {
  private def error(errors: JavaList[StaticError], message: String, loc: HasAt) =
    errors.add(StaticError.make(message, loc))

  /** Result of {@link #buildApis}. */
  class ApiResult(_apis: JavaMap[APIName, ApiIndex],
                  errors: JavaList[StaticError]) extends StaticPhaseResult {
    super.setErrors(errors)
    def apis() = _apis
  }

  /** Result of {@link #buildComponents}. */
  class ComponentResult(_components: JavaMap[APIName, ComponentIndex],
                        errors: JavaList[StaticError]) extends StaticPhaseResult {
    super.setErrors(errors)
    def components() = _components
  }

  /** Convert the given ASTs to ApiIndices. */
  def buildApis(asts: JavaIterable[Api], env: GlobalEnvironment,
                modifiedDate: Long): ApiResult =
    buildApis(asts, new JavaHashMap[APIName, ApiIndex](), env,
              modifiedDate, new LinkedList[StaticError]())

  private def buildApis(asts: JavaIterable[Api], apis: JavaMap[APIName, ApiIndex],
                        env: GlobalEnvironment, modifiedDate: Long,
                        errors: JavaList[StaticError]): ApiResult = {
    var apisAdded = false
    for (ast <- asts) {
        apisAdded = apisAdded || buildApi(ast, apis, env, modifiedDate, errors)
    }
    if (apisAdded)
      buildApis(asts, apis, env, modifiedDate, new LinkedList[StaticError]())
    else new ApiResult(apis, errors)
  }

  /** Convert the given ASTs to ComponentIndices. */
  def buildComponents(asts: JavaIterable[Component], modifiedDate: Long) = {
    val errors: JavaList[StaticError] = new LinkedList[StaticError]()
    val components: JavaMap[APIName, ComponentIndex] =
      new JavaHashMap[APIName, ComponentIndex]()
    for (ast <- asts) { buildComponent(ast, components, modifiedDate, errors) }
    new ComponentResult(components, errors)
  }

  /** Create an ApiIndex and add it to the given map. */
  private def buildApi(ast: Api, apis: JavaMap[APIName, ApiIndex],
                       env: GlobalEnvironment, modifiedDate: Long,
                       errors: JavaList[StaticError]) = {
    if (apis.containsKey(ast.getName)) false
    else {
      val _errors = new CompoundApiChecker(apis, env).check(ast)
      if (! _errors.isEmpty) {
        errors.addAll(_errors); false
      } else {
        // If <code>ast</code> is a compound API, link it into a single API.
        val new_ast = new ApiLinker(apis, env).link(ast)
        apis.put(new_ast.getName,
                 buildCompilationUnitIndex(new_ast, modifiedDate,
                                           errors, true).asInstanceOf[ApiIndex])
        true
      }
    }
  }

  /** Create a ComponentIndex and add it to the given map. */
  private def buildComponent(ast: Component, components: JavaMap[APIName, ComponentIndex],
                             modifiedDate: Long, errors: JavaList[StaticError]) =
    components.put(ast.getName, buildCompilationUnitIndex(ast, modifiedDate, errors,
                                                          false).asInstanceOf[ComponentIndex])

  def buildCompilationUnitIndex(ast: CompilationUnit, modifiedDate: Long,
                                isApi: Boolean): CompilationUnitIndex =
    buildCompilationUnitIndex(ast, modifiedDate, new LinkedList[StaticError](), isApi)

  private def buildCompilationUnitIndex(ast: CompilationUnit, modifiedDate: Long,
                                        errors: JavaList[StaticError],
                                        isApi: Boolean): CompilationUnitIndex = {
    val variables: JavaMap[Id, Variable] = new JavaHashMap[Id, Variable]()
    val initializers: JavaSet[VarDecl] = new JavaHashSet[VarDecl]()
    val functions: Relation[IdOrOpOrAnonymousName, JavaFunction] =
      new IndexedRelation[IdOrOpOrAnonymousName, JavaFunction](false)
    val parametricOperators: JavaSet[ParametricOperator] =
      new JavaHashSet[ParametricOperator]()
    val typeConses: JavaMap[Id, TypeConsIndex] = new JavaHashMap[Id, TypeConsIndex]()
    val dimensions: JavaMap[Id, JavaDimension] = new JavaHashMap[Id, JavaDimension]()
    val units: JavaMap[Id, JavaUnit] = new JavaHashMap[Id, JavaUnit]()
    val grammars: JavaMap[String, GrammarIndex] = new JavaHashMap[String, GrammarIndex]()
    for (decl <- ast.getDecls) {
      decl match {
        case d@STraitDecl(_, _, self, excludes, comprises, _) =>
          buildTrait(d, typeConses, functions, parametricOperators, errors)
          checkTraitClauses(typeConses, NU.getName(d),
                            NU.getStaticParams(d),
                            excludes, comprises, self)
        case d@SObjectDecl(_,_,_,_) =>
          buildObject(d, typeConses, functions, parametricOperators, variables, errors)
        case d@SVarDecl(_,_,_) =>
          buildVariables(d, variables)
          if (! isApi) initializers.add(d)
        case d@SFnDecl(_,_,_,_,_) => buildFunction(d, functions)
        case d@SDimDecl(_,_,_,_) => buildDimension(d, dimensions)
        case d@SUnitDecl(_,_,_,_,_) => buildUnit(d, units)
        case d@SGrammarDecl(_,_,_,_,_,_) =>
          if (isApi) buildGrammar(d, grammars, errors)
        case d@STypeAlias(_,_,_,_) => bug("Not yet implemented: " + d)
        case d@STestDecl(_,_,_,_) => bug("Not yet implemented: " + d)
        case d@SPropertyDecl(_,_,_,_) => bug("Not yet implemented: " + d)
        case _ =>
      }
    }
    if (isApi)
      new ApiIndex(ast.asInstanceOf[Api], variables, functions, parametricOperators,
                   typeConses, dimensions, units, grammars, modifiedDate)
    else
      new ComponentIndex(ast.asInstanceOf[Component], variables, initializers,
                         functions, parametricOperators,
                         typeConses, dimensions, units, modifiedDate)
  }

  /**
   * One doesn't generally store ObjectIndices for Object expressions because they cannot
   * really be referred to on their own as types. However, there are some circumstances where
   * having an index for one can be helpful.
   */
  def buildObjectExprIndex(obj: ObjectExpr): ObjectTraitIndex = {
    val fake_span = NF.makeSpan("FAKE_SPAN")
    val fake_object_name = NF.makeId(fake_span, "FAKE_NAME")
    // Make fake object
    val decl = NF.makeObjectDecl(fake_span, fake_object_name,
                                 NU.getExtendsClause(obj),
                                 NU.getDecls(obj), obj.getSelfType)
    val index_holder: JavaMap[Id,TypeConsIndex] = new JavaHashMap[Id,TypeConsIndex]()
    // TODO: Fix this so that the global function map and parametricOperator set are
    // threaded through to here.
    buildObject(decl, index_holder,
                new IndexedRelation[IdOrOpOrAnonymousName,JavaFunction](),
                new JavaHashSet[ParametricOperator](), new JavaHashMap[Id,Variable](),
                new LinkedList[StaticError]())
    index_holder.get(fake_object_name).asInstanceOf[ObjectTraitIndex]
  }

  private def checkTraitClauses(typeConses: JavaMap[Id, TypeConsIndex],
                                dName: IdOrOpOrAnonymousName,
                                staticParams: JavaList[StaticParam],
                                excludes: List[BaseType],
                                comprises: Option[List[BaseType]],
                                self: Option[Type]) = {
    val dIndex = typeConses.get(dName).asInstanceOf[ProperTraitIndex]
    if ( comprises.isDefined ) {
      for ( t <- comprises.get )
        if ( t.isInstanceOf[NamedType] ) {
          val typ = t match {
            case STraitType(_,_,_,_) => t.asInstanceOf[TraitType]
            case _ => NF.makeTraitType(t.asInstanceOf[NamedType].getName)
          }
          dIndex.addComprisesType(typ)
        } else bug("TraitType is expected in the comprises clause of " + dName +
                   " but found " + t + " " + t.getClass + ".")
    }
    for (t <- excludes) {
      if ( t.isInstanceOf[NamedType] ) {
        // add t to d's excludes clause
        val tName = t.asInstanceOf[NamedType].getName
        var typ = t match {
          case STraitType(_,_,_,_) => t.asInstanceOf[TraitType]
          case _ => NF.makeTraitType(tName)
        }
        dIndex.addExcludesType(typ)
        // add d to t's excludes clause
        typ = toOption(self) match {
          case Some(ty) => ty.asInstanceOf[TraitType]
          case _ =>
            NF.makeTraitType(dName.asInstanceOf[Id],
                             TypeEnv.staticParamsToArgs(staticParams))
        }
        // If t is a parameterized type instantiated with ground types,
        // then do not add d to t's excludes clause.
        if ( ( t.isInstanceOf[VarType] ||
               t.asInstanceOf[TraitType].getArgs.isEmpty ) &&
             typeConses.get(tName).isInstanceOf[ProperTraitIndex] &&
             typeConses.get(tName).asInstanceOf[ProperTraitIndex] != null )
          typeConses.get(tName).asInstanceOf[ProperTraitIndex].addExcludesType(typ)
      } else bug("TraitType is expected in the excludes clause of " + dName +
                 " but found " + t + ".")
    }
  }

  /**
   * Create a ProperTraitIndex and put it in the given map; add functional methods
   * to the given relation.
   */
  private def buildTrait(ast: TraitDecl, typeConses: JavaMap[Id, TypeConsIndex],
                         functions: Relation[IdOrOpOrAnonymousName, JavaFunction],
                         parametricOperators: JavaSet[ParametricOperator],
                         errors: JavaList[StaticError]) = {
    val name = NU.getName(ast)
    val getters: JavaMap[Id, Method] = new JavaHashMap[Id, Method]()
    val setters: JavaMap[Id, Method] = new JavaHashMap[Id, Method]()
    val coercions: JavaSet[Coercion] = new JavaHashSet[Coercion]()
    val dottedMethods: Relation[IdOrOpOrAnonymousName, JavaDeclaredMethod] =
      new IndexedRelation[IdOrOpOrAnonymousName, JavaDeclaredMethod](false)
    val functionalMethods: Relation[IdOrOpOrAnonymousName, JavaFunctionalMethod] =
      new IndexedRelation[IdOrOpOrAnonymousName, JavaFunctionalMethod](false)

    // FnDecls should be handled before VarDecls
    for (decl <- NU.getDecls(ast)) {
      decl match {
        case d@SFnDecl(_,_,_,_,_) =>
          buildMethod(d, ast, NU.getStaticParams(ast),
                      getters, setters, coercions, dottedMethods,
                      functionalMethods, functions, parametricOperators,
                      errors)
        case _ =>
      }
    }

    for (decl <- NU.getDecls(ast)) {
      decl match {
        case d@SVarDecl(_,_,_) => buildTraitFields(d, ast, getters, setters)
        case d@SPropertyDecl(_,_,_,_) => NI.nyi; ()
        case _ =>
      }
    }

    typeConses.put(name, new ProperTraitIndex(ast, getters, setters, coercions,
                                              dottedMethods, functionalMethods))
  }

  private def checkObject(getters: JavaMap[Id, Method],
                          setters: JavaMap[Id, Method],
                          dottedMethods: Relation[IdOrOpOrAnonymousName, JavaDeclaredMethod],
                          optParams: JavaOption[JavaList[Param]],
                          errors: JavaList[StaticError]) = {
    for (id <- getters.keySet) {
      if ( dottedMethods.firstSet().contains(id) )
        error(errors, "Getter declarations should not be overloaded with method declarations.", id)
    }
    for (id <- setters.keySet) {
      if ( dottedMethods.firstSet().contains(id) )
        error(errors, "Setter declarations should not be overloaded with method declarations.", id)
    }
    toOption(optParams) match {
      case Some(params) =>
        for (p <- toList(params)) {
          val mods = p.getMods
          if (!mods.isHidden && NU.isVarargsParam(p) )
              error(errors, "Varargs object parameters should not define getters.", p)
          if ( (mods.isSettable || mods.isVar) && NU.isVarargsParam(p) )
              error(errors, "Varargs object parameters should not define setters.", p)
        }
      case _ =>
    }
  }

  /**
   * Create an ObjectTraitIndex and put it in the given map; add functional methods
   * to the given relation; create a constructor function or singleton variable and
   * put it in the appropriate map.
   */
  private def buildObject(ast: ObjectDecl, typeConses: JavaMap[Id, TypeConsIndex],
                          functions: Relation[IdOrOpOrAnonymousName, JavaFunction],
                          parametricOperators: JavaSet[ParametricOperator],
                          variables: JavaMap[Id, Variable],
                          errors: JavaList[StaticError]) = {
    val name = NU.getName(ast)
    val fields: JavaMap[Id, Variable] = new JavaHashMap[Id, Variable]()
    val initializers: JavaSet[VarDecl] = new JavaHashSet[VarDecl]()
    val getters: JavaMap[Id, Method] = new JavaHashMap[Id, Method]()
    val setters: JavaMap[Id, Method] = new JavaHashMap[Id, Method]()
    val coercions: JavaSet[Coercion] = new JavaHashSet[Coercion]()
    val dottedMethods: Relation[IdOrOpOrAnonymousName, JavaDeclaredMethod] =
      new IndexedRelation[IdOrOpOrAnonymousName, JavaDeclaredMethod](false)
    val functionalMethods: Relation[IdOrOpOrAnonymousName, JavaFunctionalMethod] =
      new IndexedRelation[IdOrOpOrAnonymousName, JavaFunctionalMethod](false)

    for (decl <- NU.getDecls(ast)) {
      decl match {
        case d@SFnDecl(_,_,_,_,_) =>
          buildMethod(d, ast, NU.getStaticParams(ast),
                      getters, setters, coercions, dottedMethods,
                      functionalMethods, functions, parametricOperators,
                      errors)
        case _ =>
      }
    }

    for (decl <- NU.getDecls(ast)) {
      decl match {
        case d@SVarDecl(_,_,_) =>
          buildFields(d, ast, fields, getters, setters)
          if (d.getInit.isSome) initializers.add(d)
        case d@SPropertyDecl(_,_,_,_) => NI.nyi; ()
        case _ =>
      }
    }
    val constructor = toOption(ast.getParams) match {
      case Some(params) =>
        for (p <- params) {
          val mods = p.getMods
          val paramName = p.getName
          fields.put(paramName, new JavaParamVariable(p))
          if ( !mods.isHidden && ! NU.isVarargsParam(p) )
              getters.put(paramName, new JavaFieldGetterMethod(p, ast))
          if ( (mods.isSettable || mods.isVar) && ! NU.isVarargsParam(p) )
              setters.put(paramName, new JavaFieldSetterMethod(p, ast))
        }
        val c = new JavaConstructor(name, NU.getStaticParams(ast),
                                    ast.getParams, NU.getThrowsClause(ast),
                                    NU.getWhereClause(ast))
        functions.add(name, c)
        some[JavaConstructor](c)
      case _ =>
        variables.put(name, new JavaSingletonVariable(name))
        none[JavaConstructor]
    }

    checkObject(getters, setters, dottedMethods, ast.getParams, errors)

    typeConses.put(name, new ObjectTraitIndex(ast, constructor, fields, initializers,
                                              getters, setters, coercions,
                                              dottedMethods, functionalMethods))
  }

  /**
   * Create a variable wrapper for each declared variable and add it to the given
   * map.
   */
  private def buildVariables(ast: VarDecl, variables: JavaMap[Id, Variable]) =
    for (b <- ast.getLhs)
      variables.put(b.getName, new JavaDeclaredVariable(b, ast))

  /**
   * Create and add to the given maps implicit getters and setters for a trait's
   * abstract fields.
   */
  private def buildTraitFields(ast: VarDecl, traitDecl: TraitObjectDecl,
                               getters: JavaMap[Id, Method],
                               setters: JavaMap[Id, Method]) =
    for (b <- ast.getLhs) {
      val mods = b.getMods
      val name = b.getName
      if (!mods.isHidden)
        getters.put(name, new JavaFieldGetterMethod(b, traitDecl))
      if (mods.isMutable)
        setters.put(name, new JavaFieldSetterMethod(b, traitDecl))
    }

  /**
   * Create field variables and add them to the given map; also create implicit
   * getters and setters.
   */
  private def buildFields(ast: VarDecl, traitDecl: TraitObjectDecl,
                          fields: JavaMap[Id, Variable],
                          getters: JavaMap[Id, Method],
                          setters: JavaMap[Id, Method]) =
    for (b <- ast.getLhs) {
      val mods = b.getMods
      val name = b.getName
      fields.put(name, new JavaDeclaredVariable(b, ast))
      if (!mods.isHidden)
        getters.put(name, new JavaFieldGetterMethod(b, traitDecl))
      if (mods.isSettable || mods.isVar)
        setters.put(name, new JavaFieldSetterMethod(b, traitDecl))
    }

  /**
   * Create a dimension wrapper for the declaration and put it in the given map.
   */
  private def buildDimension(ast: DimDecl,
                             dimensions: JavaMap[Id, JavaDimension]) =
    dimensions.put(ast.getDimId, new JavaDimension(ast))

  /**
   * Create a unit wrapper for the declaration and put it in the given map.
   */
  private def buildUnit(ast: UnitDecl, units: JavaMap[Id, JavaUnit]) =
    for (unit <- ast.getUnits)
      units.put(unit, new JavaUnit(ast))

  /**
   * Create a function wrapper for the declaration and put it in the given
   * relation.
   */
  private def buildFunction(ast: FnDecl,
                            functions: Relation[IdOrOpOrAnonymousName, JavaFunction]) = {
    val df = new JavaDeclaredFunction(ast)
    functions.add(NU.getName(ast), df)
    functions.add(ast.getUnambiguousName, df)
  }

  private def fnDeclToBinding(ast: FnDecl) = {
    val mods = NU.getMods(ast)
    new LValue(ast.getInfo, NU.getName(ast).asInstanceOf[Id], mods,
               NU.getReturnType(ast), mods.isMutable)
  }

  /**
   * Determine whether the given declaration is a getter, setter, coercion, dotted
   * method, or functional method, and add it to the appropriate map; also store
   * functional methods with top-level functions. Note that parametric operators
   * are also propagated to top-level, with their parametric names. These names
   * must be substituted with particular instantiations during lookup.
   */
  private def buildMethod(ast: FnDecl, traitDecl: TraitObjectDecl,
                          enclosingParams: JavaList[StaticParam],
                          getters: JavaMap[Id, Method],
                          setters: JavaMap[Id, Method],
                          coercions: JavaSet[Coercion],
                          dottedMethods: Relation[IdOrOpOrAnonymousName, JavaDeclaredMethod],
                          functionalMethods: Relation[IdOrOpOrAnonymousName, JavaFunctionalMethod],
                          topLevelFunctions: Relation[IdOrOpOrAnonymousName, JavaFunction],
                          parametricOperators: JavaSet[ParametricOperator],
                          errors: JavaList[StaticError]): Unit = {
    val mods = NU.getMods(ast)
    val name = NU.getName(ast)
    if (mods.isGetter) {
      name match {
        case id@SId(_,_,_) =>
          if ( getters.keySet.contains(id) )
              error(errors, "Getter declarations should not be overloaded.", ast)
          else getters.put(id, new JavaFieldGetterMethod(fnDeclToBinding(ast), traitDecl))
        case _ =>
          error(errors, "Getter declared with an operator name, '" +
                NU.nameString(name) + "'", ast)
      }
    } else if (mods.isSetter) {
      name match {
        case id@SId(_,_,_) =>
          if ( setters.keySet.contains(id) )
            error(errors, "Setter declarations should not be overloaded.", ast)
          else setters.put(id, new JavaFieldSetterMethod(fnDeclToBinding(ast), NU.getParams(ast).get(0), traitDecl))
        case _ =>
          error(errors, "Getter declared with an operator name, '" +
                NU.nameString(name) + "'", ast)
      }
    } else if (name.isInstanceOf[Id] &&
               name.asInstanceOf[Id].getText.equals(NamingCzar.COERCION_NAME)) {
        coercions.add(new Coercion(ast, traitDecl, enclosingParams))
    } else {
      var functional = false
      for (p <- toList(NU.getParams(ast))) {
        if (p.getName.equals(NamingCzar.SELF_NAME)) {
          if (functional) {
              error(errors, "Parameter 'self' appears twice in a method declaration.", ast)
              return
          }
          functional = true
        }
      }
      val operator = NU.isOp(ast)
      // Determine whether:
      //   (1) this declaration has a self parameter
      //   (2) this declaration is for an operator
      // Place the declaration in the appropriate bins according to the answer.
      if (functional && ! operator) {
        val m = new JavaFunctionalMethod(ast, traitDecl, enclosingParams)
        functionalMethods.add(name, m)
        topLevelFunctions.add(name, m)
      } else if (functional && operator) {
        var parametric = false
        for (p <- enclosingParams) {
          if (NU.getName(ast).equals(NU.getName(p)))
            parametric = true
        }
        if (parametric)
            parametricOperators.add(new ParametricOperator(ast, traitDecl, enclosingParams))
        else {
          val m = new JavaFunctionalMethod(ast, traitDecl, enclosingParams)
          functionalMethods.add(name, m)
          topLevelFunctions.add(name, m)
        }
      } else if ((! functional) && operator) {
        // In this case, we must have a subscripting operator method declaration
        // or a subscripted assignment operator method declaration. See F 1.0 beta Section 34.
        // TODO: Check that we are handling this case correctly!
        dottedMethods.add(name, new JavaDeclaredMethod(ast, traitDecl))
      } else { // ! functional && ! operator
        dottedMethods.add(name, new JavaDeclaredMethod(ast, traitDecl))
      }
    }
  }

  /**
   * Create a Grammar and put it in the given map.
   */
  private def buildGrammar(ast: GrammarDecl,
                           grammars: JavaMap[String, GrammarIndex],
                           errors: JavaList[StaticError]) = {
    val name = ast.getName.getText
    val grammar = new GrammarIndex(ast, buildMembers(ast.getMembers, errors))
    if (grammars.containsKey(name))
      error(errors, "Multiple grammars declared with the same name: "+name, ast)
    grammars.put(name, grammar)
  }

  private def buildMembers(members: JavaList[GrammarMemberDecl],
                           errors: JavaList[StaticError]) = {
    val result: JavaList[NonterminalIndex] = new ArrayList[NonterminalIndex]()
    val names: JavaSet[Id] = new JavaHashSet[Id]()
    for (m <- members) {
      if (names.contains(m.getName))
        error(errors, "Nonterminal declared twice: "+m.getName, m)
      names.add(m.getName)
      m match {
        case d@SNonterminalDef(_,_,_,_,_) =>
          result.add(new NonterminalDefIndex(d))
        case d@SNonterminalExtensionDef(_,_,_) =>
          result.add(new NonterminalExtendIndex(d))
        case _ =>
      }
    }
    result
  }
}
