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

package com.sun.fortress.scala_src.useful

import _root_.java.util.ArrayList
import com.sun.fortress.compiler.GlobalEnvironment
import com.sun.fortress.compiler.Types
import com.sun.fortress.compiler.index._
import com.sun.fortress.compiler.typechecker.StaticTypeReplacer
import com.sun.fortress.compiler.typechecker.TypeAnalyzer
import com.sun.fortress.compiler.typechecker.TypesUtil
import com.sun.fortress.exceptions.InterpreterBug.bug
import com.sun.fortress.exceptions.TypeError
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.ExprFactory
import com.sun.fortress.nodes_util.{NodeFactory => NF}
import com.sun.fortress.nodes_util.{NodeUtil => NU}
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.useful.HasAt
import com.sun.fortress.useful.NI

object STypesUtil {
  
  /** A function that when applied yields an option type. */
  type TypeThunk = Function0[Option[Type]]

  /** A function type that takes two types and returns a boolean. */
  type Subtype = (Type, Type) => Boolean
  
  /**
   * Return the arrow type of the given Functional index.
   */  
  def makeArrowFromFunctional(f: Functional): Option[ArrowType] = {
    val returnType = toOption(f.getReturnType).getOrElse(return None)
    val params = toList(f.parameters).map(NU.getParamType)
    val argType = makeArgumentType(params)
    val sparams = f.staticParameters
    val effect = NF.makeEffect(f.thrownTypes)
    val where = f match {
      case f:Constructor => f.where
      case _ => none[WhereClause]
    }
    Some(NF.makeArrowType(NF.typeSpan,
                          false,
                          argType,
                          returnType,
                          effect,
                          sparams,
                          where))
  }
  
  /**
   * Make a single argument type from a list of types.
   */
  def makeArgumentType(ts: List[Type]): Type = ts match {
    case Nil => Types.VOID
    case t :: Nil => t
    case _ =>
      val span1 = NU.getSpan(ts.head)
      val span2 = NU.getSpan(ts.last)
      NF.makeTupleType(NU.spanTwo(span1, span2), toJavaList(ts))
  }
  
  /**
   * Make a domain type from a list of parameters, including varargs and
   * keyword types. Ported from `TypeEnv.domainFromParams`.
   */
  def makeDomainType(ps: List[Param]): Type = {
    val paramTypes = new ArrayList[Type](ps.length)
    val keywordTypes = new ArrayList[KeywordType](ps.length)
    var varargsType: Option[Type] = None
    val span = ps match {
      case Nil => NF.typeSpan
      case _ => NU.spanTwo(NU.getSpan(ps.first), NU.getSpan(ps.last))
    }
    
    // Extract out the appropriate parameter types.
    ps.foreach(p => p match {
      case SParam(_, _, _, _, _, Some(vaType)) => // Vararg
        varargsType = Some(vaType)
      case SParam(_, name, _, Some(idType), Some(expr), _) => // Keyword
        keywordTypes.add(NF.makeKeywordType(name, idType))
      case SParam(_, _, _, Some(idType), _, _) => // Normal
        paramTypes.add(idType)
      case _ => bug("Parameter missing type") 
    })
    NF.makeDomain(span, paramTypes, toJavaOption(varargsType), keywordTypes)
  }
  
  /**
   * Convert a static parameter to the corresponding static arg. Ported from
   * `TypeEnv.staticParamsToArgs`.
   */
  def staticParamToArg(p: StaticParam): StaticArg = {
    val span = NU.getSpan(p)
    (p.getName, p.getKind) match {
      case (id:Id, _:KindBool) => NF.makeBoolArg(span, NF.makeBoolRef(span, id))
      case (id:Id, _:KindDim) => NF.makeDimArg(span, NF.makeDimRef(span, id))
      case (id:Id, _:KindInt) => NF.makeIntArg(span, NF.makeIntRef(span, id))
      case (id:Id, _:KindNat) => NF.makeIntArg(span, NF.makeIntRef(span, id))
      case (id:Id, _:KindType) => NF.makeTypeArg(span, NF.makeVarType(span, id))
      case (id:Id, _:KindUnit) =>
        NF.makeUnitArg(span, NF.makeUnitRef(span, false, id))
      case (op:Op, _:KindOp) => NF.makeOpArg(span, ExprFactory.makeOpRef(op))
      case _ => bug("Unexpected static parameter kind")
    }
  }

  /**
   * Get all the static parameters out of the given type.
   */
  def getStaticParams(typ: Type): List[StaticParam] =
    toList(typ.getInfo.getStaticParams)

  /**
   * Return an identical type that has no static params. If `ignoreLifted` is
   * true, then the lifted static parameters will remain in the type.
   * TODO: How to handle where clauses in TypeInfos?
   */
  def clearStaticParams(typ: Type, ignoreLifted: Boolean): Type = {

    // A walker that clears static params out of TypeInfos.
    object paramWalker extends Walker {
      override def walk(node: Any): Any = node match {
        case STypeInfo(_, _, Nil, _) => node
        case STypeInfo(a, b, sparams, _) =>
          // Leave in the lifted params if nec.
          val sparams_ = if (ignoreLifted) sparams.filter(_.isLifted) else Nil
          STypeInfo(a, b, sparams_, None)
        case _ => super.walk(node)
      }
    }
    paramWalker(typ).asInstanceOf[Type]
  }

  /**
   * Identical to other overloading but `ignoreLifted` is false.
   */
  def clearStaticParams(typ: Type): Type = clearStaticParams(typ, false)

  /**
   * Insert static parameters into a type. If the type already has static
   * parameters, a bug is thrown.
   */
  def insertStaticParams(typ: Type, sparams: List[StaticParam]): Type = {

    // A walker that clears static params out of TypeInfos.
    object paramWalker extends Walker {
      override def walk(node: Any): Any = node match {
        case STypeInfo(a, b, Nil, c) => STypeInfo(a, b, sparams, c)
        case STypeInfo(_, _, _, _) =>
          bug("cannot overwrite static parameters")
        case _ => super.walk(node)
      }
    }
    paramWalker(typ).asInstanceOf[Type]
  }

  /**
   * Determine if the type previously inferred by the type checker from the
   * given expression is an arrow or intersection of arrow types.
   */
  def isArrows(expr: Expr): Boolean = isArrows(SExprUtil.getType(expr).get)

  /**
   * Determine if the given type is an arrow or intersection of arrow types.
   */
  def isArrows(ty: Type): Boolean =
    TypesUtil.isArrows(ty).asInstanceOf[Boolean]

  /**
   * Performs the given substitution on the body type.
   */
  def substituteTypesForInferenceVars(substitution: Map[_InferenceVarType, Type],
                                      body: Type): Type = {

    object substitutionWalker extends Walker {
      override def walk(node: Any): Any = node match {
        case ty:_InferenceVarType => substitution.get(ty).getOrElse(super.walk(ty))
        case _ => super.walk(node)
      }
    }

    // Perform the substitution on the body type.
    substitutionWalker(body).asInstanceOf[Type]
  }

  /**
   * Returns the type of the static parameter's bound if it is a type parameter.
   */
  def staticParamBoundType(sparam: StaticParam): Option[Type] =
    sparam.getKind match {
      case SKindType(_) => Some(NF.makeIntersectionType(sparam.getExtendsClause))
      case _ => None
    }

  /**
   * Given a static parameters, returns a static arg containing a fresh
   * inference variable.
   */
  def makeInferenceArg(sparam: StaticParam): StaticArg = sparam.getKind match {
    case SKindType(_) => {
      // Create a new inference var type.
      val t = NF.make_InferenceVarType(NU.getSpan(sparam))
      NF.makeTypeArg(NF.makeSpan(t), t)
    }
    case SKindInt(_) => NI.nyi()
    case SKindBool(_) => NI.nyi()
    case SKindDim(_) => NI.nyi()
    case SKindOp(_) => NI.nyi()
    case SKindUnit(_) => NI.nyi()
    case SKindNat(_) => NI.nyi()
    case _ => bug("unexpected kind of static parameter")
  }

  /**
   * Returns a list of conjuncts of the given type. If given an intersection
   * type, this is the set of the constituents. If ANY, this is empty. If some
   * other type, this is the singleton set of that type.
   */
  def conjuncts(ty: Type): Set[Type] = ty match {
    case _:AnyType => Set.empty[Type]
    case SIntersectionType(_, elts) => Set(elts:_*).flatMap(conjuncts)
    case _ => Set(ty)
  }

  /**
   * Returns TypeConsIndex of "typ".
   */
  def getTypes(typ:Id, globalEnv: GlobalEnvironment,
               compilation_unit: CompilationUnitIndex): TypeConsIndex = typ match {
    case SId(info,Some(name),text) =>
      globalEnv.api(name).typeConses.get(SId(info,None,text))
    case _ => compilation_unit.typeConses.get(typ)
  }
}
