/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

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
import scala.collection.mutable.MultiMap
import scala.collection.mutable.HashMap
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
import com.sun.fortress.scala_src.useful.Sets._
import com.sun.fortress.scala_src.useful.STypesUtil._
import com.sun.fortress.useful.HasAt
import com.sun.fortress.useful.NI
import com.sun.fortress.exceptions.TypeError

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
    val apiName = if (isApi) Some(ast.getName) else None

    // Make a mutable multimap to hold the coercions to each trait.
    val liftedCoercions = new HashMap[String, Set[Coercion]] with MultiMap[String, Coercion]

    for (decl <- ast.getDecls) {
      decl match {
        case d@STraitDecl(_, _, self, excludes, comprises, _) =>
          buildTrait(d, apiName, typeConses, functions, parametricOperators, errors)
          checkTraitClauses(typeConses, NU.getName(d),
                            NU.getStaticParams(d),
                            excludes, comprises, self)
        case d:ObjectDecl =>
          buildObject(d, apiName, typeConses, functions, parametricOperators, variables, errors)
        case d:VarDecl =>
          buildVariables(d, variables)
          if (! isApi) initializers.add(d)
        case d:FnDecl => buildFunction(d, apiName, functions, liftedCoercions)
        case d:DimDecl => buildDimension(d, dimensions)
        case d:UnitDecl => buildUnit(d, units)
        case d:GrammarDecl => if (isApi) buildGrammar(d, grammars, errors)
        case d:TypeAlias => bug("Not yet implemented: " + d)
        case d:TestDecl => bug("Not yet implemented: " + d)
        case d:PropertyDecl => bug("Not yet implemented: " + d)
        case _ =>
      }
    }

    // Add lifted coercions that were found.
    addLiftedCoercions(typeConses, liftedCoercions)
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
    val span = NU.getSpan(obj)
    val fake_object_name = NF.makeId(span, "<object expression>")
    // Make fake object
    val decl = NF.makeObjectDecl(span, fake_object_name,
                                 NU.getExtendsClause(obj),
                                 NU.getDecls(obj), obj.getSelfType)
    val index_holder: JavaMap[Id,TypeConsIndex] = new JavaHashMap[Id,TypeConsIndex]()
    // TODO: Fix this so that the global function map and parametricOperator set are
    // threaded through to here.
    buildObject(decl, None, index_holder,
                new IndexedRelation[IdOrOpOrAnonymousName,JavaFunction](),
                new JavaHashSet[ParametricOperator](), new JavaHashMap[Id,Variable](),
                new LinkedList[StaticError]())
    index_holder.get(fake_object_name).asInstanceOf[ObjectTraitIndex]
  }

  private def checkTraitClauses(typeConses: JavaMap[Id, TypeConsIndex],
                                dName: IdOrOpOrAnonymousName,
                                staticParams: JavaList[StaticParam],
                                excludes: List[BaseType],
                                comprises: Option[List[NamedType]],
                                self: Option[SelfType]) = {
    val dIndex = typeConses.get(dName).asInstanceOf[ProperTraitIndex]
    if ( comprises.isDefined ) {
      for ( t <- comprises.get ) dIndex.addComprisesType(t)
    }
    for (t <- excludes) {
      if ( t.isInstanceOf[NamedType] ) {
        // add t to d's excludes clause
        val tName = t.asInstanceOf[NamedType].getName
        var typ = t match {
          case _:TraitType => t.asInstanceOf[TraitType]
          case _ => NF.makeTraitType(tName)
        }
        dIndex.addExcludesType(typ)
        // add d to t's excludes clause
        typ = toOption(self) match {
          case Some(ty) if (ty.isInstanceOf[TraitSelfType] &&
                            ty.asInstanceOf[TraitSelfType].getNamed.isInstanceOf[TraitType]) =>
            ty.asInstanceOf[TraitSelfType].getNamed.asInstanceOf[TraitType]
          case _ =>
            NF.makeTraitType(dName.asInstanceOf[Id],
                             staticParamsToArgs(staticParams))
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
  private def buildTrait(ast: TraitDecl,
                         apiName: Option[APIName],
                         typeConses: JavaMap[Id, TypeConsIndex],
                         functions: Relation[IdOrOpOrAnonymousName, JavaFunction],
                         parametricOperators: JavaSet[ParametricOperator],
                         errors: JavaList[StaticError]) = {
    val name = NU.getName(ast)
    val getters: JavaMap[Id, JavaFieldGetterMethod] = new JavaHashMap[Id, JavaFieldGetterMethod]()
    val setters: JavaMap[Id, JavaFieldSetterMethod] = new JavaHashMap[Id, JavaFieldSetterMethod]()
    val coercions: JavaSet[Coercion] = new JavaHashSet[Coercion]()
    val dottedMethods: Relation[IdOrOpOrAnonymousName, JavaDeclaredMethod] =
      new IndexedRelation[IdOrOpOrAnonymousName, JavaDeclaredMethod](false)
    val functionalMethods: Relation[IdOrOpOrAnonymousName, JavaFunctionalMethod] =
      new IndexedRelation[IdOrOpOrAnonymousName, JavaFunctionalMethod](false)

    // FnDecls should be handled before VarDecls
    for (decl <- NU.getDecls(ast)) {
      decl match {
        case d:FnDecl =>
          buildMethod(d, apiName, ast, NU.getStaticParams(ast),
                      getters, setters, coercions, dottedMethods,
                      functionalMethods, functions, parametricOperators,
                      errors)
        case _ =>
      }
    }

    for (decl <- NU.getDecls(ast)) {
      decl match {
        case d:VarDecl => buildTraitFields(d, ast, getters, setters)
        case d:PropertyDecl => NI.nyi; ()
        case _ =>
      }
    }

    typeConses.put(name, new ProperTraitIndex(ast, getters, setters, coercions,
                                              dottedMethods, functionalMethods))
  }

  /**
   * Create an ObjectTraitIndex and put it in the given map; add functional methods
   * to the given relation; create a constructor function or singleton variable and
   * put it in the appropriate map.
   */
  private def buildObject(ast: ObjectDecl,
                          apiName: Option[APIName],
                          typeConses: JavaMap[Id, TypeConsIndex],
                          functions: Relation[IdOrOpOrAnonymousName, JavaFunction],
                          parametricOperators: JavaSet[ParametricOperator],
                          variables: JavaMap[Id, Variable],
                          errors: JavaList[StaticError]) = {
    val name = NU.getName(ast)
    val fields: JavaMap[Id, Variable] = new JavaHashMap[Id, Variable]()
    val initializers: JavaSet[VarDecl] = new JavaHashSet[VarDecl]()
    val getters: JavaMap[Id, JavaFieldGetterMethod] = new JavaHashMap[Id, JavaFieldGetterMethod]()
    val setters: JavaMap[Id, JavaFieldSetterMethod] = new JavaHashMap[Id, JavaFieldSetterMethod]()
    val coercions: JavaSet[Coercion] = new JavaHashSet[Coercion]()
    val dottedMethods: Relation[IdOrOpOrAnonymousName, JavaDeclaredMethod] =
      new IndexedRelation[IdOrOpOrAnonymousName, JavaDeclaredMethod](false)
    val functionalMethods: Relation[IdOrOpOrAnonymousName, JavaFunctionalMethod] =
      new IndexedRelation[IdOrOpOrAnonymousName, JavaFunctionalMethod](false)

    for (decl <- NU.getDecls(ast)) {
      decl match {
        case d:FnDecl =>
          buildMethod(d, apiName, ast, NU.getStaticParams(ast),
                      getters, setters, coercions, dottedMethods,
                      functionalMethods, functions, parametricOperators,
                      errors)
        case _ =>
      }
    }

    for (decl <- NU.getDecls(ast)) {
      decl match {
        case d:VarDecl =>
          buildFields(d, ast, fields, getters, setters)
          if (d.getInit.isSome) initializers.add(d)
        case d:PropertyDecl => NI.nyi; ()
        case _ =>
      }
    }
    val constructor = toOption(NU.getParams(ast)) match {
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
                                    NU.getParams(ast), NU.getThrowsClause(ast),
                                    NU.getWhereClause(ast))
        functions.add(name, c)
        some[JavaConstructor](c)
      case _ =>
        variables.put(name, new JavaSingletonVariable(name))
        none[JavaConstructor]
    }

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
                               getters: JavaMap[Id, JavaFieldGetterMethod],
                               setters: JavaMap[Id, JavaFieldSetterMethod]) =
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
                          getters: JavaMap[Id, JavaFieldGetterMethod],
                          setters: JavaMap[Id, JavaFieldSetterMethod]) =
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
                            apiName: Option[APIName],
                            functions: Relation[IdOrOpOrAnonymousName, JavaFunction],
                            liftedCoercions: MultiMap[String, Coercion]) = {
    val df = new JavaDeclaredFunction(ast)
    functions.add(NU.getName(ast), df)
    functions.add(ast.getUnambiguousName, df)
    // TODO: Don't double-add the indices?

    // Add as coercion too?
    if (NU.isLiftedCoercion(ast)) {
      val c = new Coercion(ast, apiName)
      liftedCoercions.addBinding(c.declaringTrait.getText, c)
    }
  }

  /**
   * Determine whether the given declaration is a getter, setter, coercion, dotted
   * method, or functional method, and add it to the appropriate map; also store
   * functional methods with top-level functions. Note that parametric operators
   * are also propagated to top-level, with their parametric names. These names
   * must be substituted with particular instantiations during lookup.
   */
  private def buildMethod(ast: FnDecl,
                          apiName: Option[APIName],
                          traitDecl: TraitObjectDecl,
                          enclosingParams: JavaList[StaticParam],
                          getters: JavaMap[Id, JavaFieldGetterMethod],
                          setters: JavaMap[Id, JavaFieldSetterMethod],
                          coercions: JavaSet[Coercion],
                          dottedMethods: Relation[IdOrOpOrAnonymousName, JavaDeclaredMethod],
                          functionalMethods: Relation[IdOrOpOrAnonymousName, JavaFunctionalMethod],
                          topLevelFunctions: Relation[IdOrOpOrAnonymousName, JavaFunction],
                          parametricOperators: JavaSet[ParametricOperator],
                          errors: JavaList[StaticError]): Unit = {
    val mods = NU.getMods(ast)
    val name = NU.getName(ast)
    val uaname = NU.getUnambiguousName(ast);
    if (NU.isCoercion(ast)) {
      coercions.add(new Coercion(ast, traitDecl, apiName, enclosingParams))
    } else if (mods.isGetter) {
      name match {
        case id:Id =>
          if ( !getters.keySet.contains(id) )
            getters.put(id, new JavaFieldGetterMethod(ast, traitDecl))
        case _ => // Checked by ExprDisambiguator.
      }
    } else if (mods.isSetter) {
      name match {
        case id:Id =>
          if ( !setters.keySet.contains(id) )
            setters.put(id, new JavaFieldSetterMethod(ast, traitDecl))
        case _ => // Checked by ExprDisambiguator.
      }
    } else {
      var functional = false
      for (p <- toListFromImmutable(NU.getParams(ast))) {
        if (p.getName.equals(NamingCzar.SELF_NAME))
          functional = true
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
        functionalMethods.add(uaname, m)
        topLevelFunctions.add(uaname, m)

      } else if (functional && operator) {
        var parametric = false
        for (p <- enclosingParams) {
          // this seems very brittle
          if (NU.getName(ast).toString == NU.getName(p))
            parametric = true
        }
        if (parametric){
            TypeError.b3(ast, traitDecl, apiName);
            val po = new ParametricOperator(ast, traitDecl, enclosingParams)
            parametricOperators.add(po)
            topLevelFunctions.add(name, po)
            topLevelFunctions.add(uaname, po)
            functionalMethods.add(name, po)
        }
        else {
            TypeError.b3(ast, traitDecl, apiName);          
          val m = new JavaFunctionalMethod(ast, traitDecl, enclosingParams)
          functionalMethods.add(name, m)
          topLevelFunctions.add(name, m)

          functionalMethods.add(uaname, m)
          topLevelFunctions.add(uaname, m)
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
        case d:NonterminalDef =>
          result.add(new NonterminalDefIndex(d))
        case d:NonterminalExtensionDef =>
          result.add(new NonterminalExtendIndex(d))
        case _ =>
      }
    }
    result
  }

  /**
   * For each TraitIndex in `typeConses` add in any lifted coercions that were
   * found. Because each TraitIndex has a publicly accessible, mutable set of
   * Coercion indices, we can merely get that set and add into it.
   */
  private def addLiftedCoercions(typeConses: JavaMap[Id, TypeConsIndex],
                                 liftedCoercions: MultiMap[String, Coercion]) = {
    // Loop over the typeCons Ids.
    for (id <- typeConses.keySet) {

      // Get all the coercions with the same trait name.
      liftedCoercions.get(id.getText) match {
        case None =>
        case Some(csLifted) =>

          // Get the current index, which must be a trait or object. Get its
          // mutable set of Coercion indices and add the lifted ones!
          val ti = typeConses.get(id)
          if (ti.isInstanceOf[TraitIndex]) {
            // Add in the lifted coercions by mutating the current index.
            ti.asInstanceOf[TraitIndex].coercions.addAll(toJavaSet(csLifted))
          }
      }
    }
  }
}
