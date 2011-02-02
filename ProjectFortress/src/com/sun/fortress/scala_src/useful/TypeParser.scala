/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.scala_src.useful
import _root_.java.util.{List => JList}
import com.sun.fortress.compiler.GlobalEnvironment
import com.sun.fortress.compiler.WellKnownNames._
import com.sun.fortress.compiler.index._
import com.sun.fortress.compiler.Types.ANY
import com.sun.fortress.compiler.Types.BOTTOM
import com.sun.fortress.compiler.Types.OBJECT
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.Modifiers
import com.sun.fortress.nodes_util.NodeFactory._
import com.sun.fortress.nodes_util.NodeUtil._
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.typechecker.TraitTable
import com.sun.fortress.scala_src.types.TypeAnalyzer
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Maps._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.Sets._
import com.sun.fortress.scala_src.useful.STypesUtil._

import edu.rice.cs.plt.collect.CollectUtil;

import scala.util.parsing.combinator._

object TypeParser extends RegexParsers {
  val TRAIT = """[A-Z][a-zA-Z0-9]+"""r
  val VAR = """[A-Z]"""r
  val IVAR = """[a-z]"""r

  def typeSchema: Parser[Type] = opt(staticParams) ~ typ ^^
    {case ps~t => insertStaticParams(t, ps.getOrElse(Nil))}

  def arrowTypeSchema: Parser[ArrowType] = opt(staticParams) ~ arrowType ^^
    {case ps~t => insertStaticParams(t, ps.getOrElse(Nil)).asInstanceOf[ArrowType]}

  def traitSchema: Parser[TraitType] = opt(staticParams) ~ traitType ^^
    {case ps~t => insertStaticParams(t, ps.getOrElse(Nil)).asInstanceOf[TraitType]}

  def staticParams: Parser[List[StaticParam]] = "[" ~> repsep(staticParam, ",") <~ "]"
  
  def staticParam: Parser[StaticParam] = regex(VAR) ~ opt("extends {" ~> repsep(baseType, ",") <~ "}") ^^
    {case id~bounds => makeTypeParam(typeSpan, makeId(typeSpan, id), toJavaList(bounds.getOrElse(List(OBJECT))), none[Type], false)}

  def typ: Parser[Type] = arrowType | nonArrowType

  def arrowType: Parser[ArrowType] = nonArrowType ~ "->" ~ typ ^^
    {case dom~_~ran => makeArrowType(typeSpan,dom, ran)}

  def nonArrowType: Parser[Type] = baseType | tupleType | intersectionType | unionType | ivarType

  def tupleType: Parser[Type] = "(" ~> repsep(typ, ",") <~ ")" ^^
    {typs => makeMaybeTupleType(typeSpan, toJavaList(typs))}

  def baseType: Parser[BaseType] = anyType | bottomType | namedType
  
  def objectType: Parser[TraitType] = literal("Object") ^^ {x => OBJECT}

  def namedType: Parser[NamedType] = objectType | traitType | varType

  def anyType: Parser[AnyType] = literal("ANY") ^^ {x => ANY}

  def bottomType: Parser[BottomType] = literal("BOTTOM") ^^ {x => BOTTOM}

  def intersectionType: Parser[Type] = "&&" ~> "{" ~> repsep(typ, ",") <~ "}" ^^
    {x => makeMaybeIntersectionType(toJavaList(x))}

  def unionType: Parser[Type] = "||" ~> "{" ~> repsep(typ, ",") <~ "}" ^^
    { x => makeMaybeUnionType(toJavaList(x))}

  def varType: Parser[VarType] = regex(VAR) ^^
    {id => makeVarType(typeSpan, id)}

  def ivarType: Parser[_InferenceVarType] = regex(IVAR) ^^
    {id => make_InferenceVarType(typeSpan, false, id)}
  
  def traitType: Parser[TraitType] = regex(TRAIT) ~ opt(staticArgs) ^^
    {case id~args => makeTraitType(typeSpan, id, toJavaList(args.getOrElse(Nil)))}
  def staticArgs: Parser[List[StaticArg]] = "[" ~> repsep(staticArg, ",") <~ "]"
  def staticArg: Parser[StaticArg] = typ ^^
    {t => makeTypeArg(typeSpan, t)}

  def traitIndex: Parser[TraitIndex] = "trait" ~> regex(TRAIT) ~ opt(staticParams) ~
    opt("extends {" ~> repsep(baseType, ",") <~ "}") ~
    opt("excludes {" ~> repsep(baseType, ",") <~ "}") ~
    opt("comprises {" ~> repsep(namedType, ",") <~ "}") ^^
    {case tName~mSparams~mSupers~mExcludes~mComprises =>
      val tId = makeId(typeSpan, tName)
      val supers = mSupers.getOrElse(Nil)
      val excludes = mExcludes.getOrElse(Nil)
      val superWheres = supers.map(makeTraitTypeWhere(_, none[WhereClause]))
      val comprises = mComprises.map(toJavaList(_))
      val sparams = mSparams.getOrElse(Nil)
      val sargs = sparams.map(staticParamToArg)
      val tType = makeTraitType(typeSpan, tName, toJavaList(sargs))
      val selfType = comprises match {
        case Some(cs) => makeSelfType(tType, cs)
        case None => makeSelfType(tType)
      }
       
      // Construct the AST trait declaration node.
      val ast = makeTraitDecl(typeSpan,
                              Modifiers.None,
                              tId,
                              toJavaList(sparams),
                              toJavaOption(None),
                              toJavaList(superWheres),
                              none[WhereClause],
                              toJavaList[Decl](Nil),
                              toJavaList(excludes),
                              toJavaOption(comprises),
                              false,
                              some(selfType))
      val ti = new ProperTraitIndex(ast,
                                    toJavaMap(Map()),
                                    toJavaMap(Map()),
                                    toJavaSet(Set()),
                                    CollectUtil.emptyRelation[IdOrOpOrAnonymousName,DeclaredMethod],
                                    CollectUtil.emptyRelation[IdOrOpOrAnonymousName,FunctionalMethod])
     excludes.map(x => ti.addExcludesType(x.asInstanceOf[TraitType]))
     mComprises.map(x => x.map(y => ti.addComprisesType(y.asInstanceOf[NamedType])))
     ti
  }

  def objectIndex: Parser[TraitIndex] = "object" ~> regex(TRAIT) ~ opt(staticParams) ~
    opt("extends {" ~> repsep(baseType, ",") <~ "}") ^^
    {case tName~mSparams~mSupers =>
      val tId = makeId(typeSpan, tName)
      val supers = mSupers.getOrElse(Nil)
      val superWheres = supers.map(makeTraitTypeWhere(_, none[WhereClause]))
      val sparams = mSparams.getOrElse(Nil)
      val sargs = sparams.map(staticParamToArg)
      val tType = makeTraitType(typeSpan, tName, toJavaList(sargs))
      val selfType = makeSelfType(tType)
      
      // Make object declaration AST node.
      val ast = makeObjectDecl(typeSpan,
                               Modifiers.None,
                               tId,
                               toJavaList(sparams),
                               toJavaList(superWheres),
                               none[WhereClause],
                               toJavaList[Decl](Nil),
                               none[JList[Param]],
                               none[JList[Type]],
                               none[Contract],
                               some(selfType))
      new ObjectTraitIndex(ast,
                           toJavaOption(None),
                           toJavaMap(Map()),
                           toJavaSet(Set()),
                           toJavaMap(Map()),
                           toJavaMap(Map()),
                           toJavaSet(Set()),
                           CollectUtil.emptyRelation[IdOrOpOrAnonymousName,DeclaredMethod],
                           CollectUtil.emptyRelation[IdOrOpOrAnonymousName,FunctionalMethod])
  }

  def typeAnalyzer: Parser[TypeAnalyzer] = "{" ~> repsep(traitIndex | objectIndex, ",") <~ "}" ^^
    {traits =>
      val component = makeComponentIndex("OverloadingTest", traits)
      TypeAnalyzer.make(new TraitTable(component, GLOBAL_ENV))
    }

  def overloadingSet: Parser[List[ArrowType]] = "{" ~> repsep(arrowTypeSchema, ",") <~ "}"

  def makeComponentIndex(name: String, traits: List[TraitIndex]): ComponentIndex = {
    val traitDecls = traits.map(_.ast.asInstanceOf[Decl])
    val traitMap = Map(traits.map(t=> (getName(t.ast),t)):_*)
    val ast = makeComponent(typeSpan, makeAPIName(typeSpan, name),
                              toJavaList(Nil), toJavaList(traitDecls), toJavaList(Nil))
    new ComponentIndex(ast,
                       toJavaMap(Map()),
                       toJavaSet(Set()),
                       CollectUtil.emptyRelation[IdOrOpOrAnonymousName, Function],
                       toJavaSet(Set()),
                       toJavaMap(traitMap),
                       toJavaMap(Map()),
                       toJavaMap(Map()),
                       0)
  }

  def makeApiIndex(name: String, traits: List[TraitIndex]): ApiIndex = {
    val traitDecls = traits.map(_.ast.asInstanceOf[Decl])
    val traitMap = Map(traits.map(t=> (getName(t.ast),t)):_*)
    val ast = makeApi(typeSpan, makeAPIName(typeSpan, name),
                      toJavaList(Nil), toJavaList(traitDecls));
    return new ApiIndex(ast,
                        toJavaMap(Map()),
                        CollectUtil.emptyRelation[IdOrOpOrAnonymousName, Function],
                        toJavaSet(Set()),
                        toJavaMap(traitMap),
                        toJavaMap(Map()),
                        toJavaMap(Map()),
                        toJavaMap(Map()),
                        0);
  }

  val GLOBAL_ENV = {
    val any = makeApiIndex(anyTypeLibrary, List(parse(traitIndex, "trait Any").get))
    val obj = makeApiIndex(fortressBuiltin, List(parse(traitIndex, "trait Object").get))
    new GlobalEnvironment.FromMap(toJavaMap(Map((any.ast.getName, any), (obj.ast.getName, obj))))
  }

}
