/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/
package com.sun.fortress.scala_src.linker

import collection.mutable.HashMap
import com.sun.fortress.compiler.GlobalEnvironment
import com.sun.fortress.compiler.index.ComponentIndex
import com.sun.fortress.nodes.{IdOrOpOrAnonymousName, APIName}
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.useful.Sets._
import com.sun.fortress.repository.FortressRepository


/**
 * This class is responsible for taking an AST corresponding to the constituent of a compound
 * component and hygienically renaming all names in that constituent. The renamed contents can
 * then be merged with the contents of other constituents to create a single, flattened representation
 * of the enclosing compound component.
 */
class HygienicRenamer(constituent: APIName,
                      enclosing: APIName,
                      repository: FortressRepository) extends Walker {

  val SEP = "?" // U+2609

  val constituentImported = findImportedNames(repository.getComponent(constituent))
  val constituentExported = findExportedNames(repository.getComponent(constituent))
  val enclosingImported = findImportedNames(repository.getComponent(enclosing))
  val enclosingExported = findExportedNames(repository.getComponent(enclosing))

  def constituentImports(name: IdOrOpOrAnonymousName) = { constituentImported contains name }
  def constituentExports(name: IdOrOpOrAnonymousName) = { constituentExported contains name }
  def enclosingImports(name: IdOrOpOrAnonymousName) = { enclosingImported contains name }
  def enclosingExports(name: IdOrOpOrAnonymousName) = { enclosingExported contains name }

  def findImportedNames(comp: ComponentIndex) = findNames(comp, false)
  def findExportedNames(comp: ComponentIndex) = findNames(comp, true)

  def findNames(comp: ComponentIndex, exported: Boolean) = {
    val result = new HashMap[IdOrOpOrAnonymousName, APIName]()
    var apis = List[APIName]()

    if (exported) { for (a <- toSet(comp.exports)) apis = a :: apis }
    else { for (a <- toSet(comp.imports)) apis = a :: apis }

    for (e <- apis.sortWith((a1,a2) => (a1.getText compareTo a2.getText) < 0)) {
      // We are assured at this point that all exported API names refer to an
      // API in the globalEnv
      val api = repository.getApi(e)
      val apiName = api.ast.getName

      /* Multiple APIs exported by a single component cannot include
       * declarations with the same name and kind.
       */
      val apiVariables = toSet(api.variables.keySet)
      for (v <- apiVariables) { result += v -> apiName }
      for (f <- toSet(api.functions.firstSet)) { result += f -> apiName  }
      for (t <- toSet(api.typeConses.keySet)) { result += t -> apiName }
      for (d <- toSet(api.dimensions.keySet)) { result += d -> apiName }
      for (u <- toSet(api.units.keySet)) { result += u -> apiName }
      // TODO: What should we do with top-level parametricOperators?
      // for (o <- toSet(api.parametricOperators)) { result += o -> apiName }
    }
    result
  }

  def replaceAtDecl(name: IdOrOpOrAnonymousName) = {
    name match {
      case SId(info, Some(apiName), text) => // name must be imported by this constituent
         val enclosingAndApiQualified = SId(info, None, enclosing + SEP + apiName + SEP + text)
         val enclosingAndConstituentQualified = SId(info, None, enclosing + SEP + constituent + SEP + text)

         if (constituentExports(name) && enclosingExports(name)) {
            name
         } else if (constituentExports(name) && ! enclosingExports(name)) {
            enclosingAndApiQualified
         } else { // name is not exported by constituent or enclosing
           enclosingAndConstituentQualified
         }
       case _ => name // TODO: Implement Op case
     }
  }

  def replaceAtUse(name: IdOrOpOrAnonymousName) = {
     name match {
       case SId(info, api, text) => // name must be imported by this constituent
          val enclosingAndConstituentQualified = SId(info, None, enclosing + SEP + constituent + SEP + text)

          api match {
            case Some(apiName) =>
              val enclosingAndApiQualified = SId(info, None, enclosing + SEP + apiName + SEP + text)

              if (enclosingImports(name) && constituentImports(name)) {
                name
              } else if (constituentImports(name) && enclosingExports(name)) {
                // name must be exported by some other constituent
                SId(info, None, text)
              } else { // name must be exported by some other constituent but not by enclosing
                enclosingAndApiQualified
              }
            case None =>
              if (constituentExports(name) && enclosingExports(name)) {
                name
              } else if (constituentExports(name) && ! enclosingExports(name)) {
                SId(info, None, enclosing + SEP + constituentExported(name) + SEP + text)
              } else { // name is not exported by constituent or enclosing
                enclosingAndConstituentQualified
              }
          }
       case _ => name // TODO: Implement Op case
     }
  }

    override def walk(node: Any): Any = node match {
         case SFnDecl(getInfo, getHeader, getUnambiguousName, getBody, getImplementsUnambiguousName) =>
             SFnDecl(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ASTNodeInfo], walk(getHeader).asInstanceOf[com.sun.fortress.nodes.FnHeader], walk(getUnambiguousName).asInstanceOf[com.sun.fortress.nodes.IdOrOp], walk(getBody).asInstanceOf[Option[com.sun.fortress.nodes.Expr]], walk(getImplementsUnambiguousName).asInstanceOf[Option[com.sun.fortress.nodes.IdOrOp]])
         case SFnHeader(getStaticParams, getMods, getName, getWhereClause, getThrowsClause, getContract, getParams, getReturnType) =>
             SFnHeader(walk(getStaticParams).asInstanceOf[List[com.sun.fortress.nodes.StaticParam]], walk(getMods).asInstanceOf[com.sun.fortress.nodes_util.Modifiers], walk(getName).asInstanceOf[com.sun.fortress.nodes.IdOrOpOrAnonymousName], walk(getWhereClause).asInstanceOf[Option[com.sun.fortress.nodes.WhereClause]], walk(getThrowsClause).asInstanceOf[Option[List[com.sun.fortress.nodes.BaseType]]], walk(getContract).asInstanceOf[Option[com.sun.fortress.nodes.Contract]], walk(getParams).asInstanceOf[List[com.sun.fortress.nodes.Param]], walk(getReturnType).asInstanceOf[Option[com.sun.fortress.nodes.Type]])
         case SFnRef(getInfo, getStaticArgs, getLexicalDepth, getOriginalName, getNames, getInterpOverloadings, getNewOverloadings, getOverloadingType, getOverloadingSchema) =>
             SFnRef(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ExprInfo], walk(getStaticArgs).asInstanceOf[List[com.sun.fortress.nodes.StaticArg]], walk(getLexicalDepth).asInstanceOf[Int], walk(getOriginalName).asInstanceOf[com.sun.fortress.nodes.IdOrOp], walk(getNames).asInstanceOf[List[com.sun.fortress.nodes.IdOrOp]], walk(getInterpOverloadings).asInstanceOf[List[com.sun.fortress.nodes.Overloading]], walk(getNewOverloadings).asInstanceOf[List[com.sun.fortress.nodes.Overloading]], walk(getOverloadingType).asInstanceOf[Option[com.sun.fortress.nodes.Type]], walk(getOverloadingSchema).asInstanceOf[Option[com.sun.fortress.nodes.Type]])
         case SId(getInfo, getApiName, getText) =>
             SId(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ASTNodeInfo], walk(getApiName).asInstanceOf[Option[com.sun.fortress.nodes.APIName]], walk(getText).asInstanceOf[String])
         case SKeywordExpr(getInfo, getName, getInit) =>
             SKeywordExpr(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ASTNodeInfo], walk(getName).asInstanceOf[com.sun.fortress.nodes.Id], walk(getInit).asInstanceOf[com.sun.fortress.nodes.Expr])
         case SLValue(getInfo, getName, getMods, getIdType, isMutable) =>
             SLValue(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ASTNodeInfo], walk(getName).asInstanceOf[com.sun.fortress.nodes.Id], walk(getMods).asInstanceOf[com.sun.fortress.nodes_util.Modifiers], walk(getIdType).asInstanceOf[Option[com.sun.fortress.nodes.Type]], walk(isMutable).asInstanceOf[Boolean])
         case SObjectDecl(getInfo, getHeader, getSelfType) =>
             SObjectDecl(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ASTNodeInfo], walk(getHeader).asInstanceOf[com.sun.fortress.nodes.TraitTypeHeader], walk(getSelfType).asInstanceOf[Option[com.sun.fortress.nodes.SelfType]])
         case SObjectExpr(getInfo, getHeader, getSelfType) =>
             SObjectExpr(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ExprInfo], walk(getHeader).asInstanceOf[com.sun.fortress.nodes.TraitTypeHeader], walk(getSelfType).asInstanceOf[Option[com.sun.fortress.nodes.SelfType]])
         case SNamedOp(getInfo, getApiName, getText, getFixity, isEnclosing) =>
             SNamedOp(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ASTNodeInfo], walk(getApiName).asInstanceOf[Option[com.sun.fortress.nodes.APIName]], walk(getText).asInstanceOf[String], walk(getFixity).asInstanceOf[com.sun.fortress.nodes.Fixity], walk(isEnclosing).asInstanceOf[Boolean])
         case SOpArg(getInfo, isLifted, getId) =>
             SOpArg(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ASTNodeInfo], walk(isLifted).asInstanceOf[Boolean], walk(getId).asInstanceOf[com.sun.fortress.nodes.Op])
         case SOpExpr(getInfo, getOp, getArgs) =>
             SOpExpr(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ExprInfo], walk(getOp).asInstanceOf[com.sun.fortress.nodes.FunctionalRef], walk(getArgs).asInstanceOf[List[com.sun.fortress.nodes.Expr]])
         case SOpRef(getInfo, getStaticArgs, getLexicalDepth, getOriginalName, getNames, getInterpOverloadings, getNewOverloadings, getOverloadingType, getOverloadingSchema) =>
             SOpRef(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ExprInfo], walk(getStaticArgs).asInstanceOf[List[com.sun.fortress.nodes.StaticArg]], walk(getLexicalDepth).asInstanceOf[Int], walk(getOriginalName).asInstanceOf[com.sun.fortress.nodes.IdOrOp], walk(getNames).asInstanceOf[List[com.sun.fortress.nodes.IdOrOp]], walk(getInterpOverloadings).asInstanceOf[List[com.sun.fortress.nodes.Overloading]], walk(getNewOverloadings).asInstanceOf[List[com.sun.fortress.nodes.Overloading]], walk(getOverloadingType).asInstanceOf[Option[com.sun.fortress.nodes.Type]], walk(getOverloadingSchema).asInstanceOf[Option[com.sun.fortress.nodes.Type]])
         case SOptionalSymbol(getInfo, getSymbol) =>
             SOptionalSymbol(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ASTNodeInfo], walk(getSymbol).asInstanceOf[com.sun.fortress.nodes.SyntaxSymbol])
         case SOverloading(getInfo, getUnambiguousName, getOriginalName, getType, getSchema) =>
             SOverloading(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ASTNodeInfo], walk(getUnambiguousName).asInstanceOf[com.sun.fortress.nodes.IdOrOp], walk(getOriginalName).asInstanceOf[com.sun.fortress.nodes.IdOrOp], walk(getType).asInstanceOf[Option[com.sun.fortress.nodes.ArrowType]], walk(getSchema).asInstanceOf[Option[com.sun.fortress.nodes.ArrowType]])
         case SParam(getInfo, getName, getMods, getIdType, getDefaultExpr, getVarargsType) =>
             SParam(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ASTNodeInfo], walk(getName).asInstanceOf[com.sun.fortress.nodes.Id], walk(getMods).asInstanceOf[com.sun.fortress.nodes_util.Modifiers], walk(getIdType).asInstanceOf[Option[com.sun.fortress.nodes.Type]], walk(getDefaultExpr).asInstanceOf[Option[com.sun.fortress.nodes.Expr]], walk(getVarargsType).asInstanceOf[Option[com.sun.fortress.nodes.Type]])
         case SParenthesisDelimitedMI(getInfo, getExpr) =>
             SParenthesisDelimitedMI(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ASTNodeInfo], walk(getExpr).asInstanceOf[com.sun.fortress.nodes.Expr])
         case SPostFixity =>
             SPostFixity
         case SPreFixity =>
             SPreFixity
         case SPreTransformerDef(getInfo, getTransformer) =>
             SPreTransformerDef(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ASTNodeInfo], walk(getTransformer).asInstanceOf[com.sun.fortress.nodes.Transformer])
         case SPrefixedSymbol(getInfo, getId, getSymbol) =>
             SPrefixedSymbol(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ASTNodeInfo], walk(getId).asInstanceOf[com.sun.fortress.nodes.Id], walk(getSymbol).asInstanceOf[com.sun.fortress.nodes.SyntaxSymbol])
         case SPropertyDecl(getInfo, getName, getParams, getExpr) =>
             SPropertyDecl(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ASTNodeInfo], walk(getName).asInstanceOf[Option[com.sun.fortress.nodes.Id]], walk(getParams).asInstanceOf[List[com.sun.fortress.nodes.Param]], walk(getExpr).asInstanceOf[com.sun.fortress.nodes.Expr])
         case SRepeatOneOrMoreSymbol(getInfo, getSymbol) =>
             SRepeatOneOrMoreSymbol(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ASTNodeInfo], walk(getSymbol).asInstanceOf[com.sun.fortress.nodes.SyntaxSymbol])
         case SRepeatSymbol(getInfo, getSymbol) =>
             SRepeatSymbol(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ASTNodeInfo], walk(getSymbol).asInstanceOf[com.sun.fortress.nodes.SyntaxSymbol])
         case SSpanInfo(getSpan) =>
             SSpanInfo(walk(getSpan).asInstanceOf[com.sun.fortress.nodes_util.Span])
         case SSpawn(getInfo, getBody) =>
             SSpawn(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ExprInfo], walk(getBody).asInstanceOf[com.sun.fortress.nodes.Expr])
         case SStaticParam(getInfo, getVariance, getName, getExtendsClause, getDominatesClause, getDimParam, isAbsorbsParam, getKind, isLifted) =>
             SStaticParam(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ASTNodeInfo], getVariance, walk(getName).asInstanceOf[com.sun.fortress.nodes.IdOrOp], walk(getExtendsClause).asInstanceOf[List[com.sun.fortress.nodes.BaseType]], walk(getDominatesClause).asInstanceOf[List[com.sun.fortress.nodes.BaseType]], walk(getDimParam).asInstanceOf[Option[com.sun.fortress.nodes.Type]], walk(isAbsorbsParam).asInstanceOf[Boolean], walk(getKind).asInstanceOf[com.sun.fortress.nodes.StaticParamKind], walk(isLifted).asInstanceOf[Boolean])
         case SStringLiteralExpr(getInfo, getText) =>
             SStringLiteralExpr(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ExprInfo], walk(getText).asInstanceOf[String])
         case SSubscriptExpr(getInfo, getObj, getSubs, getOp, getStaticArgs) =>
             SSubscriptExpr(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ExprInfo], walk(getObj).asInstanceOf[com.sun.fortress.nodes.Expr], walk(getSubs).asInstanceOf[List[com.sun.fortress.nodes.Expr]], walk(getOp).asInstanceOf[Option[com.sun.fortress.nodes.Op]], walk(getStaticArgs).asInstanceOf[List[com.sun.fortress.nodes.StaticArg]])
         case SSubscriptingMI(getInfo, getOp, getExprs, getStaticArgs) =>
             SSubscriptingMI(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ASTNodeInfo], walk(getOp).asInstanceOf[com.sun.fortress.nodes.Op], walk(getExprs).asInstanceOf[List[com.sun.fortress.nodes.Expr]], walk(getStaticArgs).asInstanceOf[List[com.sun.fortress.nodes.StaticArg]])
         case SSuperSyntaxDef(getInfo, getModifier, getNonterminal, getGrammarId) =>
             SSuperSyntaxDef(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ASTNodeInfo], walk(getModifier).asInstanceOf[Option[String]], walk(getNonterminal).asInstanceOf[com.sun.fortress.nodes.Id], walk(getGrammarId).asInstanceOf[com.sun.fortress.nodes.Id])
         case SSyntaxDef(getInfo, getModifier, getSyntaxSymbols, getTransformer) =>
             SSyntaxDef(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ASTNodeInfo], walk(getModifier).asInstanceOf[Option[String]], walk(getSyntaxSymbols).asInstanceOf[List[com.sun.fortress.nodes.SyntaxSymbol]], walk(getTransformer).asInstanceOf[com.sun.fortress.nodes.TransformerDecl])
         case STabSymbol(getInfo) =>
             STabSymbol(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ASTNodeInfo])
         case STaggedDimType(getInfo, getElemType, getDimExpr, getUnitExpr) =>
             STaggedDimType(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.TypeInfo], walk(getElemType).asInstanceOf[com.sun.fortress.nodes.Type], walk(getDimExpr).asInstanceOf[com.sun.fortress.nodes.DimExpr], walk(getUnitExpr).asInstanceOf[Option[com.sun.fortress.nodes.Expr]])
         case STaggedUnitType(getInfo, getElemType, getUnitExpr) =>
             STaggedUnitType(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.TypeInfo], walk(getElemType).asInstanceOf[com.sun.fortress.nodes.Type], walk(getUnitExpr).asInstanceOf[com.sun.fortress.nodes.Expr])
         case STestDecl(getInfo, getName, getGens, getExpr) =>
             STestDecl(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ASTNodeInfo], walk(getName).asInstanceOf[com.sun.fortress.nodes.Id], walk(getGens).asInstanceOf[List[com.sun.fortress.nodes.GeneratorClause]], walk(getExpr).asInstanceOf[com.sun.fortress.nodes.Expr])
         case SThrow(getInfo, getExpr) =>
             SThrow(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ExprInfo], walk(getExpr).asInstanceOf[com.sun.fortress.nodes.Expr])
         case STokenSymbol(getInfo, getToken) =>
             STokenSymbol(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ASTNodeInfo], walk(getToken).asInstanceOf[String])
         case STraitDecl(getInfo, getHeader, getExcludesClause, getComprisesClause, isComprisesEllipses, getSelfType) =>
             STraitDecl(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ASTNodeInfo], walk(getHeader).asInstanceOf[com.sun.fortress.nodes.TraitTypeHeader], walk(getSelfType).asInstanceOf[Option[com.sun.fortress.nodes.SelfType]], walk(getExcludesClause).asInstanceOf[List[com.sun.fortress.nodes.BaseType]], walk(getComprisesClause).asInstanceOf[Option[List[com.sun.fortress.nodes.NamedType]]], walk(isComprisesEllipses).asInstanceOf[Boolean])
         case STraitSelfType(getInfo, getNamed, getComprised) =>
             STraitSelfType(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.TypeInfo],
                            walk(getNamed).asInstanceOf[com.sun.fortress.nodes.BaseType],
                            walk(getComprised).asInstanceOf[List[com.sun.fortress.nodes.NamedType]])
         case SObjectExprType(getInfo, getExtended) =>
             SObjectExprType(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.TypeInfo],
                             walk(getExtended).asInstanceOf[List[com.sun.fortress.nodes.BaseType]])
         case STraitType(getInfo, getName, getArgs, getStaticParams) =>
             STraitType(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.TypeInfo], walk(getName).asInstanceOf[com.sun.fortress.nodes.Id], walk(getArgs).asInstanceOf[List[com.sun.fortress.nodes.StaticArg]], walk(getStaticParams).asInstanceOf[List[com.sun.fortress.nodes.StaticParam]])
         case STraitTypeHeader(getStaticParams, getMods, getName, getWhereClause, getThrowsClause, getContract, getExtendsClause, getParams, getDecls) =>
             STraitTypeHeader(walk(getStaticParams).asInstanceOf[List[com.sun.fortress.nodes.StaticParam]], walk(getMods).asInstanceOf[com.sun.fortress.nodes_util.Modifiers], walk(getName).asInstanceOf[com.sun.fortress.nodes.IdOrOpOrAnonymousName], walk(getWhereClause).asInstanceOf[Option[com.sun.fortress.nodes.WhereClause]], walk(getThrowsClause).asInstanceOf[Option[List[com.sun.fortress.nodes.BaseType]]], walk(getContract).asInstanceOf[Option[com.sun.fortress.nodes.Contract]], walk(getExtendsClause).asInstanceOf[List[com.sun.fortress.nodes.TraitTypeWhere]], walk(getParams).asInstanceOf[Option[List[com.sun.fortress.nodes.Param]]], walk(getDecls).asInstanceOf[List[com.sun.fortress.nodes.Decl]])
         case STraitTypeWhere(getInfo, getBaseType, getWhereClause) =>
             STraitTypeWhere(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ASTNodeInfo], walk(getBaseType).asInstanceOf[com.sun.fortress.nodes.BaseType], walk(getWhereClause).asInstanceOf[Option[com.sun.fortress.nodes.WhereClause]])
         case STry(getInfo, getBody, getCatchClause, getForbidClause, getFinallyClause) =>
             STry(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ExprInfo], walk(getBody).asInstanceOf[com.sun.fortress.nodes.Block], walk(getCatchClause).asInstanceOf[Option[com.sun.fortress.nodes.Catch]], walk(getForbidClause).asInstanceOf[List[com.sun.fortress.nodes.BaseType]], walk(getFinallyClause).asInstanceOf[Option[com.sun.fortress.nodes.Block]])
         case STryAtomicExpr(getInfo, getExpr) =>
             STryAtomicExpr(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ExprInfo], walk(getExpr).asInstanceOf[com.sun.fortress.nodes.Expr])
         case STupleExpr(getInfo, getExprs, getVarargs, getKeywords, isInApp) =>
             STupleExpr(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ExprInfo], walk(getExprs).asInstanceOf[List[com.sun.fortress.nodes.Expr]], walk(getVarargs).asInstanceOf[Option[com.sun.fortress.nodes.Expr]], walk(getKeywords).asInstanceOf[List[com.sun.fortress.nodes.KeywordExpr]], walk(isInApp).asInstanceOf[Boolean])
         case STupleType(getInfo, getElements, getVarargs, getKeywords) =>
             STupleType(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.TypeInfo], walk(getElements).asInstanceOf[List[com.sun.fortress.nodes.Type]], walk(getVarargs).asInstanceOf[Option[com.sun.fortress.nodes.Type]], walk(getKeywords).asInstanceOf[List[com.sun.fortress.nodes.KeywordType]])
         case STypeAlias(getInfo, getName, getStaticParams, getTypeDef) =>
             STypeAlias(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ASTNodeInfo], walk(getName).asInstanceOf[com.sun.fortress.nodes.Id], walk(getStaticParams).asInstanceOf[List[com.sun.fortress.nodes.StaticParam]], walk(getTypeDef).asInstanceOf[com.sun.fortress.nodes.Type])
         case STypeArg(getInfo, isLifted, getTypeArg) =>
             STypeArg(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ASTNodeInfo], walk(isLifted).asInstanceOf[Boolean], walk(getTypeArg).asInstanceOf[com.sun.fortress.nodes.Type])
         case STypeInfo(getSpan, isParenthesized, getStaticParams, getWhereClause) =>
             STypeInfo(walk(getSpan).asInstanceOf[com.sun.fortress.nodes_util.Span], walk(isParenthesized).asInstanceOf[Boolean], walk(getStaticParams).asInstanceOf[List[com.sun.fortress.nodes.StaticParam]], walk(getWhereClause).asInstanceOf[Option[com.sun.fortress.nodes.WhereClause]])
         case STypecase(getInfo, getBindExpr, getClauses, getElseClause) =>
             STypecase(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ExprInfo], walk(getBindExpr).asInstanceOf[com.sun.fortress.nodes.Expr], walk(getClauses).asInstanceOf[List[com.sun.fortress.nodes.TypecaseClause]], walk(getElseClause).asInstanceOf[Option[com.sun.fortress.nodes.Block]])
         case STypecaseClause(getInfo, getName, getMatchType, getBody) =>
             STypecaseClause(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ASTNodeInfo], walk(getName).asInstanceOf[Option[com.sun.fortress.nodes.Id]], walk(getMatchType).asInstanceOf[com.sun.fortress.nodes.TypeOrPattern], walk(getBody).asInstanceOf[com.sun.fortress.nodes.Block])
         case SUnionType(getInfo, getElements) =>
             SUnionType(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.TypeInfo], walk(getElements).asInstanceOf[List[com.sun.fortress.nodes.Type]])
         case SUnitArg(getInfo, isLifted, getUnitArg) =>
             SUnitArg(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ASTNodeInfo], walk(isLifted).asInstanceOf[Boolean], walk(getUnitArg).asInstanceOf[com.sun.fortress.nodes.UnitExpr])
         case SUnitBinaryOp(getInfo, isParenthesized, getLeft, getRight, getOp) =>
             SUnitBinaryOp(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ASTNodeInfo], walk(isParenthesized).asInstanceOf[Boolean], walk(getLeft).asInstanceOf[com.sun.fortress.nodes.UnitExpr], walk(getRight).asInstanceOf[com.sun.fortress.nodes.UnitExpr], walk(getOp).asInstanceOf[com.sun.fortress.nodes.Op])
         case SUnitConstraint(getInfo, getName) =>
             SUnitConstraint(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ASTNodeInfo], walk(getName).asInstanceOf[com.sun.fortress.nodes.Id])
         case SUnitDecl(getInfo, isSi_unit, getUnits, getDimType, getDefExpr) =>
             SUnitDecl(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ASTNodeInfo], walk(isSi_unit).asInstanceOf[Boolean], walk(getUnits).asInstanceOf[List[com.sun.fortress.nodes.Id]], walk(getDimType).asInstanceOf[Option[com.sun.fortress.nodes.Type]], walk(getDefExpr).asInstanceOf[Option[com.sun.fortress.nodes.Expr]])
         case SUnitRef(getInfo, isParenthesized, getName) =>
             SUnitRef(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ASTNodeInfo], walk(isParenthesized).asInstanceOf[Boolean], walk(getName).asInstanceOf[com.sun.fortress.nodes.Id])
         case SUnknownFixity =>
             SUnknownFixity
         case SUnknownType(getInfo) =>
             SUnknownType(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.TypeInfo])
         case SUnparsedTransformer(getInfo, getTransformer, getNonterminal) =>
             SUnparsedTransformer(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ASTNodeInfo], walk(getTransformer).asInstanceOf[String], walk(getNonterminal).asInstanceOf[com.sun.fortress.nodes.Id])
         case SVarDecl(getInfo, getLhs, getInit) =>
             SVarDecl(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ASTNodeInfo], walk(getLhs).asInstanceOf[List[com.sun.fortress.nodes.LValue]], walk(getInit).asInstanceOf[Option[com.sun.fortress.nodes.Expr]])
         case SVarRef(getInfo, getVarId, getStaticArgs, getLexicalDepth) =>
             SVarRef(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ExprInfo], walk(getVarId).asInstanceOf[com.sun.fortress.nodes.Id], walk(getStaticArgs).asInstanceOf[List[com.sun.fortress.nodes.StaticArg]], walk(getLexicalDepth).asInstanceOf[Int])
         case SVarType(getInfo, getName, getLexicalDepth) =>
             SVarType(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.TypeInfo], walk(getName).asInstanceOf[com.sun.fortress.nodes.Id], walk(getLexicalDepth).asInstanceOf[Int])
         case SVoidLiteralExpr(getInfo, getText) =>
             SVoidLiteralExpr(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ExprInfo], walk(getText).asInstanceOf[String])
         case SWhereBinding(getInfo, getName, getSupers, getKind) =>
             SWhereBinding(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ASTNodeInfo], walk(getName).asInstanceOf[com.sun.fortress.nodes.Id], walk(getSupers).asInstanceOf[List[com.sun.fortress.nodes.BaseType]], walk(getKind).asInstanceOf[com.sun.fortress.nodes.StaticParamKind])
         case SWhereClause(getInfo, getBindings, getConstraints) =>
             SWhereClause(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ASTNodeInfo], walk(getBindings).asInstanceOf[List[com.sun.fortress.nodes.WhereBinding]], walk(getConstraints).asInstanceOf[List[com.sun.fortress.nodes.WhereConstraint]])
         case SWhereCoerces(getInfo, getLeft, getRight, isCoerces, isWidens) =>
             SWhereCoerces(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ASTNodeInfo], walk(getLeft).asInstanceOf[com.sun.fortress.nodes.Type], walk(getRight).asInstanceOf[com.sun.fortress.nodes.Type], walk(isCoerces).asInstanceOf[Boolean], walk(isWidens).asInstanceOf[Boolean])
         case SWhereEquals(getInfo, getLeft, getRight) =>
             SWhereEquals(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ASTNodeInfo], walk(getLeft).asInstanceOf[com.sun.fortress.nodes.Id], walk(getRight).asInstanceOf[com.sun.fortress.nodes.Id])
         case SWhereExtends(getInfo, getName, getSupers) =>
             SWhereExtends(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ASTNodeInfo], walk(getName).asInstanceOf[com.sun.fortress.nodes.Id], walk(getSupers).asInstanceOf[List[com.sun.fortress.nodes.BaseType]])
         case SWhereTypeAlias(getInfo, getAlias) =>
             SWhereTypeAlias(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ASTNodeInfo], walk(getAlias).asInstanceOf[com.sun.fortress.nodes.TypeAlias])
         case SWhile(getInfo, getTestExpr, getBody) =>
             SWhile(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ExprInfo], walk(getTestExpr).asInstanceOf[com.sun.fortress.nodes.GeneratorClause], walk(getBody).asInstanceOf[com.sun.fortress.nodes.Do])
         case SWhitespaceSymbol(getInfo, getS) =>
             SWhitespaceSymbol(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ASTNodeInfo], walk(getS).asInstanceOf[String])
         case S_InferenceVarType(getInfo, getName, getId) =>
             S_InferenceVarType(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.TypeInfo], walk(getName).asInstanceOf[com.sun.fortress.nodes.Id], walk(getId).asInstanceOf[_root_.java.lang.Object])
         case S_RewriteFnApp(getInfo, getFunction, getArgument) =>
             S_RewriteFnApp(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ExprInfo], walk(getFunction).asInstanceOf[com.sun.fortress.nodes.Expr], walk(getArgument).asInstanceOf[com.sun.fortress.nodes.Expr])
         case S_RewriteFnOverloadDecl(getInfo, getName, getFns, getType) =>
             S_RewriteFnOverloadDecl(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ASTNodeInfo], walk(getName).asInstanceOf[com.sun.fortress.nodes.IdOrOp], walk(getFns).asInstanceOf[List[com.sun.fortress.nodes.IdOrOp]], walk(getType).asInstanceOf[Option[com.sun.fortress.nodes.Type]])
         case S_RewriteFnRef(getInfo, getFnExpr, getStaticArgs) =>
             S_RewriteFnRef(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ExprInfo], walk(getFnExpr).asInstanceOf[com.sun.fortress.nodes.Expr], walk(getStaticArgs).asInstanceOf[List[com.sun.fortress.nodes.StaticArg]])
         case S_RewriteFunctionalMethodDecl(getInfo, getFunctionalMethodNames) =>
             S_RewriteFunctionalMethodDecl(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ASTNodeInfo], walk(getFunctionalMethodNames).asInstanceOf[List[String]])
         case S_RewriteObjectExpr(getInfo, getHeader, getImplicitTypeParameters, getGenSymName, getStaticArgs) =>
             S_RewriteObjectExpr(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ExprInfo], walk(getHeader).asInstanceOf[com.sun.fortress.nodes.TraitTypeHeader], walk(getImplicitTypeParameters).asInstanceOf[Map[String, com.sun.fortress.nodes.StaticParam]], walk(getGenSymName).asInstanceOf[String], walk(getStaticArgs).asInstanceOf[List[com.sun.fortress.nodes.StaticArg]])
         case S_RewriteObjectExprDecl(getInfo, getObjectExprs) =>
             S_RewriteObjectExprDecl(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ASTNodeInfo], walk(getObjectExprs).asInstanceOf[List[com.sun.fortress.nodes._RewriteObjectExpr]])
         case S_RewriteObjectExprRef(getInfo, getGenSymName, getStaticArgs) =>
             S_RewriteObjectExprRef(walk(getInfo).asInstanceOf[com.sun.fortress.nodes.ExprInfo], walk(getGenSymName).asInstanceOf[String], walk(getStaticArgs).asInstanceOf[List[com.sun.fortress.nodes.StaticArg]])
         case xs:List[_] => xs.map(walk _)
         case xs:Option[_] => xs.map(walk _)
         case _ => node

    }
}
