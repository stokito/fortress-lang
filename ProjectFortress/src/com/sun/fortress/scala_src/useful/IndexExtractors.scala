package com.sun.fortress.scala_src.useful
import com.sun.fortress.compiler.index._
import com.sun.fortress.nodes._
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.Sets._

import _root_.java.util.ArrayList
import _root_.java.util.{List => JList}
import _root_.java.util.{Set => JSet}
import edu.rice.cs.plt.tuple.{Option => JOption}

/* Extractor Objects
 * In order to use pattern matching over Java classes,
 * the following extractor objects are defined.
 */
/* com.sun.fortress.compiler.index.Variable
 *     comprises { DeclaredVariable, ParamVariable, SingletonVariable }
 */
object SDeclaredVariable {
  def unapply(variable:DeclaredVariable) = Some(variable.ast)
  def apply(lvalue:LValue, decl:VarDecl) = new DeclaredVariable(lvalue, decl)
}

object SParamVariable {
  def unapply(variable:ParamVariable) = Some(variable.ast)
  def apply(param:Param) = new ParamVariable(param)
}

object SSingletonVariable {
  def unapply(variable:SingletonVariable) = Some(variable.declaringTrait)
  def apply(id:Id) = new SingletonVariable(id)
}

/* com.sun.fortress.compiler.index.Functional
 *     comprises { Function, Method }
 * Function
 *     comprises { Constructor, DeclaredFunction, FunctionalMethod }
 * Method
 *     comprises { DeclaredMethod, FieldGetterMethod, FieldSetterMethod }
 * FunctionalMethod
 *     comprises { ParametricOperator }
 */
object SConstructor {
  def unapply(function:Constructor) =
    Some((function.declaringTrait, function.staticParameters,
          JOption.wrap(function.parameters),
          JOption.wrap(function.thrownTypes),
          function.where))
  def apply(id:Id, staticParams: JList[StaticParam],
            params: JOption[JList[Param]],
            throwsClause: JOption[JList[Type]],
            where: JOption[WhereClause]) =
    new Constructor(id, staticParams, params, throwsClause, where)
}

object SDeclaredFunction {
  def unapply(function:DeclaredFunction) = Some(function.ast)
  def apply(fndecl:FnDecl) = new DeclaredFunction(fndecl)
}

object SFunctionalMethod {
  def unapply(function:FunctionalMethod) =
    Some((function.ast, function.declaringTrait))
  def apply(fndecl:FnDecl, tdecl:TraitObjectDecl, traitParams:JList[StaticParam]) =
    new FunctionalMethod(fndecl, tdecl, traitParams)
}

object SDeclaredMethod {
  def unapply(method:DeclaredMethod) =
    Some((method.ast, method.declaringTrait))
  def apply(fndecl:FnDecl, tdecl:TraitObjectDecl) =
    new DeclaredMethod(fndecl, tdecl)
}

object SFieldGetterMethod {
  def unapply(method:FieldGetterMethod) =
    Some((method.ast, method.declaringTrait))
  def apply(binding:Binding, tdecl:TraitObjectDecl) =
    new FieldGetterMethod(binding, tdecl)
}

object SFieldSetterMethod {
  def unapply(method:FieldSetterMethod) =
    Some((method.ast, method.declaringTrait))
  def apply(binding:Binding, tdecl:TraitObjectDecl) =
    new FieldSetterMethod(binding, tdecl)
}