/*******************************************************************************
 Copyright 2008 Sun Microsystems, Inc.,
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

package com.sun.fortress.compiler.desugarer;


import com.sun.fortress.compiler.StaticError;
import com.sun.fortress.compiler.typechecker.*;
import static com.sun.fortress.compiler.StaticError.errorMsg;
import com.sun.fortress.compiler.index.FunctionalMethod;
import com.sun.fortress.compiler.index.Method;
import com.sun.fortress.compiler.index.TraitIndex;
import com.sun.fortress.compiler.typechecker.Types;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.*;
import com.sun.fortress.useful.*;
import edu.rice.cs.plt.collect.HashRelation;
import edu.rice.cs.plt.collect.Relation;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;
import static edu.rice.cs.plt.tuple.Option.unwrap;

import java.util.ArrayList;
import java.util.List;

public class DesugaringVisitor extends NodeUpdateVisitor {
    private boolean inTrait = false;
    private List<Id> fieldsInScope;

    public DesugaringVisitor() {
        fieldsInScope = new ArrayList<Id>();
    }

    public DesugaringVisitor(List<Id> _fieldsInScope) {
        fieldsInScope = _fieldsInScope;
    }

    public DesugaringVisitor(boolean _inTrait, List<Id> _fieldsInScope) {
        inTrait = _inTrait;
        fieldsInScope = _fieldsInScope;
    }

    private boolean hidden(Param param) {
        for (Modifier mod : param.getMods()) {
            if (mod instanceof ModifierHidden) {
                return true;
            }
        }
        return false;
    }
    
    private boolean hidden(LValueBind binding) {
        for (Modifier mod : binding.getMods()) {
            if (mod instanceof ModifierHidden) {
                return true;
            }
        }
        return false;
    }
    
    private static final Id mangleName(Id fieldName) {
        return NodeFactory.makeId(fieldName.getSpan(), "$" + fieldName.getText()); 
    }

    private static Option<List<Param>> mangleParams(Option<List<Param>> params) {
        return new NodeUpdateVisitor() {
            public Node forNormalParam(NormalParam that) {
                return new NormalParam(that.getSpan(), that.getMods(), mangleName(that.getName()), that.getType());
            }
            public Node forVarargsParam(VarargsParam that) {
                return new VarargsParam(that.getSpan(), that.getMods(), mangleName(that.getName()), 
                                        that.getVarargsType());
            }
        }.recurOnOptionOfListOfParam(params);
    }

    private DesugaringVisitor extend(Option<List<Param>> params, List<Decl> decls) {
        List<Id> newScope = new ArrayList<Id>();

        if (params.isSome()) {
            for (Param param: Option.unwrap(params)) {
                newScope.add(param.getName());
            }
        }
        for (Decl decl: decls) {
            if (decl instanceof VarDecl) {
                for (LValueBind binding : (((VarDecl)decl).getLhs())) {
                    newScope.add(binding.getName());
                }
            }
        }
        return new DesugaringVisitor(newScope);
    }

    private List<Decl> removeVarDecls(List<Decl> decls) {
        final List<Decl> result = new ArrayList<Decl>();
        
        for (Decl decl : decls) {            
            decl.accept(new NodeAbstractVisitor_void() {
                public void forDecl(Decl that) {
                    result.add(that);
                }
                public void forVarDecl(VarDecl that) {
                    // skip it
                }
            });
        }
        return result;
    }
                    
    private List<Decl> makeGetters(Option<List<Param>> params, List<Decl> decls) {     
        final List<Decl> result = new ArrayList<Decl>(); 
        if (params.isSome()) {
            for (Param param : Option.unwrap(params)) {
                param.accept(new NodeAbstractVisitor_void() {
                    public void forNormalParam(NormalParam param) {
                        if (! hidden(param)) {
                            result.add(new FnDef(param.getSpan(), param.getMods(), param.getName(), 
                                                 new ArrayList<StaticParam>(), 
                                                 new ArrayList<Param>(), param.getType(), 
                                                 new WhereClause(param.getSpan()), new Contract(new Span()), 
                                                 new VarRef(param.getSpan(), false,
                                                            new QualifiedIdName(param.getSpan(),
                                                                                mangleName(param.getName())))));
                        }
                    }
                    public void forVarargsParam(VarargsParam param) { 
                        if (! hidden(param)) {
                            result.add(new FnDef(param.getSpan(), param.getMods(), param.getName(), 
                                                 new ArrayList<StaticParam>(), 
                                                 new ArrayList<Param>(), 
                                                 Option.wrap(Types.fromVarargsType(param.getVarargsType())), 
                                                 new WhereClause(param.getSpan()), new Contract(new Span()),
                                                 new VarRef(param.getSpan(), false,
                                                            new QualifiedIdName(param.getSpan(),
                                                                                mangleName(param.getName())))));  
                        }
                    }
                });
            }
        }
        for (Decl decl : decls) {
            decl.accept(new NodeAbstractVisitor_void() {
                public void forVarDecl(VarDecl decl) {
                    for (LValueBind binding : decl.getLhs()) {
                        if (! hidden(binding)) {
                            result.add(new FnDef(binding.getSpan(), binding.getMods(), binding.getName(), 
                                                 new ArrayList<StaticParam>(), 
                                                 new ArrayList<Param>(), 
                                                 binding.getType(), 
                                                 new WhereClause(new Span()), new Contract(new Span()),
                                                 new VarRef(binding.getSpan(), false,
                                                            new QualifiedIdName(binding.getSpan(),
                                                                                mangleName(binding.getName())))));
                        }
                    }
                }
            });
        }
        return result;
    }

    /* TODO Implement */
    private List<Decl> mangleDecls(List<Decl> decls) {
        return new NodeUpdateVisitor() {
            public Node forVarDecl(VarDecl that) {
                List<LValueBind> newLVals = new ArrayList<LValueBind>();

                for (LValueBind lval : that.getLhs()) {
                    newLVals.add(NodeFactory.makeLValue(lval, mangleName(lval.getName())));
                }
                return new VarDecl(that.getSpan(), newLVals, that.getInit());
            }
            public Node forAbsVarDecl(AbsVarDecl that) {
                List<LValueBind> newLVals = new ArrayList<LValueBind>();

                for (LValueBind lval : that.getLhs()) {
                    newLVals.add(NodeFactory.makeLValue(lval, mangleName(lval.getName())));
                }
                return new AbsVarDecl(that.getSpan(), newLVals);
            }
        }.recurOnListOfDecl(decls);
    
    }
    
    //    public Node forAbsVarDeclOnly(AbsVarDecl that, List<LValueBind> lhs_result) {
//        if (that.getLhs() == lhs_result) return that;
//        else return new AbsVarDecl(that.getSpan(), lhs_result);
//    }
//
//    public Node forVarDeclOnly(VarDecl that, List<LValueBind> lhs_result, Expr init_result) {
//        if (that.getLhs() == lhs_result && that.getInit() == init_result) return that;
//        else return new VarDecl(that.getSpan(), lhs_result, init_result);
//    }

    /* 
     * Recur on VarRef to change to a mangled name if it's a field ref.
     */
    public Node forVarRefOnly(VarRef that, QualifiedIdName varResult) {
        // After disambiguation, the QualifiedIdName in a VarRef should have an empty API.
        assert(varResult.getApi().isNone());

        Id varName = varResult.getName();
        
        if (fieldsInScope.contains(varResult.getName())) { varName = mangleName(varName); }

        return new VarRef(that.getSpan(), that.isParenthesized(),
                          new QualifiedIdName(varResult.getSpan(), varResult.getApi(),
                                              mangleName(varResult.getName())));
    }

    public Node forFieldRefOnly(FieldRef that, Expr obj_result, Id field_result) {
        return new MethodInvocation(that.getSpan(), that.isParenthesized(), obj_result, field_result,
                                    new ArrayList<StaticArg>(), NodeFactory.makeVoidLiteralExpr());
    }

    public Node forObjectDecl(ObjectDecl that) {
        DesugaringVisitor newVisitor = extend(that.getParams(), that.getDecls());

        List<Modifier> mods_result = newVisitor.recurOnListOfModifier(that.getMods());
        Id name_result = (Id) that.getName().accept(newVisitor);
        List<StaticParam> staticParams_result = newVisitor.recurOnListOfStaticParam(that.getStaticParams());
        List<TraitTypeWhere> extendsClause_result = newVisitor.recurOnListOfTraitTypeWhere(that.getExtendsClause());
        WhereClause where_result = (WhereClause) that.getWhere().accept(newVisitor);
        Option<List<Param>> params_result = mangleParams(newVisitor.recurOnOptionOfListOfParam(that.getParams()));
        Option<List<TraitType>> throwsClause_result = newVisitor.recurOnOptionOfListOfTraitType(that.getThrowsClause());
        Contract contract_result = (Contract) that.getContract().accept(newVisitor);
        List<Decl> decls_result = mangleDecls(newVisitor.recurOnListOfDecl(that.getDecls()));
        
        decls_result.addAll(makeGetters(that.getParams(), that.getDecls()));
        return forObjectDeclOnly(that, mods_result, name_result, staticParams_result, extendsClause_result,
                                 where_result, params_result, throwsClause_result, contract_result, decls_result);
    }
    
    public Node forTraitDecl(TraitDecl that) {
        DesugaringVisitor newVisitor = extend(Option.<List<Param>>none(), that.getDecls());
        
        List<Modifier> mods_result = newVisitor.recurOnListOfModifier(that.getMods());
        Id name_result = (Id) that.getName().accept(newVisitor);
        List<StaticParam> staticParams_result = newVisitor.recurOnListOfStaticParam(that.getStaticParams());
        List<TraitTypeWhere> extendsClause_result = newVisitor.recurOnListOfTraitTypeWhere(that.getExtendsClause());
        WhereClause where_result = (WhereClause) that.getWhere().accept(newVisitor);
        List<TraitType> excludes_result = newVisitor.recurOnListOfTraitType(that.getExcludes());
        Option<List<TraitType>> comprises_result = newVisitor.recurOnOptionOfListOfTraitType(that.getComprises());
        List<Decl> decls_result = removeVarDecls(newVisitor.recurOnListOfDecl(that.getDecls()));
        
        decls_result.addAll(makeGetters(Option.<List<Param>>none(), that.getDecls()));       
        return forTraitDeclOnly(that, mods_result, name_result, staticParams_result, extendsClause_result, 
                                where_result, excludes_result, comprises_result, decls_result);
    }

    
    // Inherited methods copied from superclass as comments to help implement overrides.
//    /* Methods to handle a node after recursion. */
//    public Node forComponentOnly(Component that, APIName name_result, List<Import> imports_result, List<Export> exports_result, List<Decl> decls_result) {
//        if (that.getName() == name_result && that.getImports() == imports_result && that.getExports() == exports_result && that.getDecls() == decls_result)
//            return that;
//        else
//            return new Component(that.getSpan(), that.is_native(), name_result, imports_result, exports_result, decls_result);
//    }
//
//    public Node forApiOnly(Api that, APIName name_result, List<Import> imports_result, List<AbsDecl> decls_result) {
//        if (that.getName() == name_result && that.getImports() == imports_result && that.getDecls() == decls_result)
//            return that;
//        else return new Api(that.getSpan(), that.is_native(), name_result, imports_result, decls_result);
//    }
//
//    public Node forImportStarOnly(ImportStar that, APIName api_result, List<SimpleName> except_result) {
//        if (that.getApi() == api_result && that.getExcept() == except_result) return that;
//        else return new ImportStar(that.getSpan(), api_result, except_result);
//    }
//
//    public Node forImportNamesOnly(ImportNames that, APIName api_result, List<AliasedSimpleName> aliasedNames_result) {
//        if (that.getApi() == api_result && that.getAliasedNames() == aliasedNames_result) return that;
//        else return new ImportNames(that.getSpan(), api_result, aliasedNames_result);
//    }
//
//    public Node forImportApiOnly(ImportApi that, List<AliasedAPIName> apis_result) {
//        if (that.getApis() == apis_result) return that;
//        else return new ImportApi(that.getSpan(), apis_result);
//    }
//
//    public Node forAliasedSimpleNameOnly(AliasedSimpleName that, SimpleName name_result, Option<SimpleName> alias_result) {
//        if (that.getName() == name_result && that.getAlias() == alias_result) return that;
//        else return new AliasedSimpleName(that.getSpan(), name_result, alias_result);
//    }
//
//    public Node forAliasedAPINameOnly(AliasedAPIName that, APIName api_result, Option<Id> alias_result) {
//        if (that.getApi() == api_result && that.getAlias() == alias_result) return that;
//        else return new AliasedAPIName(that.getSpan(), api_result, alias_result);
//    }
//
//    public Node forExportOnly(Export that, List<APIName> apis_result) {
//        if (that.getApis() == apis_result) return that;
//        else return new Export(that.getSpan(), apis_result);
//    }
//
//    public Node forAbsTraitDeclOnly(AbsTraitDecl that, List<Modifier> mods_result, Id name_result, List<StaticParam> staticParams_result, List<TraitTypeWhere> extendsClause_result, WhereClause where_result, List<TraitType> excludes_result, Option<List<TraitType>> comprises_result, List<AbsDecl> decls_result) {
//        if (that.getMods() == mods_result && that.getName() == name_result && that.getStaticParams() == staticParams_result && that.getExtendsClause() == extendsClause_result && that.getWhere() == where_result && that.getExcludes() == excludes_result && that.getComprises() == comprises_result && that.getDecls() == decls_result)
//            return that;
//        else
//            return new AbsTraitDecl(that.getSpan(), mods_result, name_result, staticParams_result, extendsClause_result, where_result, excludes_result, comprises_result, decls_result);
//    }
//
//    public Node forTraitDeclOnly(TraitDecl that, List<Modifier> mods_result, Id name_result, List<StaticParam> staticParams_result, List<TraitTypeWhere> extendsClause_result, WhereClause where_result, List<TraitType> excludes_result, Option<List<TraitType>> comprises_result, List<Decl> decls_result) {
//        if (that.getMods() == mods_result && that.getName() == name_result && that.getStaticParams() == staticParams_result && that.getExtendsClause() == extendsClause_result && that.getWhere() == where_result && that.getExcludes() == excludes_result && that.getComprises() == comprises_result && that.getDecls() == decls_result)
//            return that;
//        else
//            return new TraitDecl(that.getSpan(), mods_result, name_result, staticParams_result, extendsClause_result, where_result, excludes_result, comprises_result, decls_result);
//    }
//
//    public Node forAbsObjectDeclOnly(AbsObjectDecl that, List<Modifier> mods_result, Id name_result, List<StaticParam> staticParams_result, List<TraitTypeWhere> extendsClause_result, WhereClause where_result, Option<List<Param>> params_result, Option<List<TraitType>> throwsClause_result, Contract contract_result, List<AbsDecl> decls_result) {
//        if (that.getMods() == mods_result && that.getName() == name_result && that.getStaticParams() == staticParams_result && that.getExtendsClause() == extendsClause_result && that.getWhere() == where_result && that.getParams() == params_result && that.getThrowsClause() == throwsClause_result && that.getContract() == contract_result && that.getDecls() == decls_result)
//            return that;
//        else
//            return new AbsObjectDecl(that.getSpan(), mods_result, name_result, staticParams_result, extendsClause_result, where_result, params_result, throwsClause_result, contract_result, decls_result);
//    }
//
//    public Node forObjectDeclOnly(ObjectDecl that, List<Modifier> mods_result, Id name_result, List<StaticParam> staticParams_result, List<TraitTypeWhere> extendsClause_result, WhereClause where_result, Option<List<Param>> params_result, Option<List<TraitType>> throwsClause_result, Contract contract_result, List<Decl> decls_result) {
//        if (that.getMods() == mods_result && that.getName() == name_result && that.getStaticParams() == staticParams_result && that.getExtendsClause() == extendsClause_result && that.getWhere() == where_result && that.getParams() == params_result && that.getThrowsClause() == throwsClause_result && that.getContract() == contract_result && that.getDecls() == decls_result)
//            return that;
//        else
//            return new ObjectDecl(that.getSpan(), mods_result, name_result, staticParams_result, extendsClause_result, where_result, params_result, throwsClause_result, contract_result, decls_result);
//    }
//
//    public Node forAbsVarDeclOnly(AbsVarDecl that, List<LValueBind> lhs_result) {
//        if (that.getLhs() == lhs_result) return that;
//        else return new AbsVarDecl(that.getSpan(), lhs_result);
//    }
//
//    public Node forVarDeclOnly(VarDecl that, List<LValueBind> lhs_result, Expr init_result) {
//        if (that.getLhs() == lhs_result && that.getInit() == init_result) return that;
//        else return new VarDecl(that.getSpan(), lhs_result, init_result);
//    }
//
//    public Node forLValueBindOnly(LValueBind that, Id name_result, Option<Type> type_result, List<Modifier> mods_result) {
//        if (that.getName() == name_result && that.getType() == type_result && that.getMods() == mods_result)
//            return that;
//        else return new LValueBind(that.getSpan(), name_result, type_result, mods_result, that.isMutable());
//    }
//
//    public Node forUnpastingBindOnly(UnpastingBind that, Id name_result, List<ExtentRange> dim_result) {
//        if (that.getName() == name_result && that.getDim() == dim_result) return that;
//        else return new UnpastingBind(that.getSpan(), name_result, dim_result);
//    }
//
//    public Node forUnpastingSplitOnly(UnpastingSplit that, List<Unpasting> elems_result) {
//        if (that.getElems() == elems_result) return that;
//        else return new UnpastingSplit(that.getSpan(), elems_result, that.getDim());
//    }
//
//    public Node forAbsFnDeclOnly(AbsFnDecl that, List<Modifier> mods_result, SimpleName name_result, List<StaticParam> staticParams_result, List<Param> params_result, Option<Type> returnType_result, Option<List<TraitType>> throwsClause_result, WhereClause where_result, Contract contract_result) {
//        if (that.getMods() == mods_result && that.getName() == name_result && that.getStaticParams() == staticParams_result && that.getParams() == params_result && that.getReturnType() == returnType_result && that.getThrowsClause() == throwsClause_result && that.getWhere() == where_result && that.getContract() == contract_result)
//            return that;
//        else
//            return new AbsFnDecl(that.getSpan(), mods_result, name_result, staticParams_result, params_result, returnType_result, throwsClause_result, where_result, contract_result, that.getSelfName());
//    }
//
//    public Node forFnDefOnly(FnDef that, List<Modifier> mods_result, SimpleName name_result, List<StaticParam> staticParams_result, List<Param> params_result, Option<Type> returnType_result, Option<List<TraitType>> throwsClause_result, WhereClause where_result, Contract contract_result, Expr body_result) {
//        if (that.getMods() == mods_result && that.getName() == name_result && that.getStaticParams() == staticParams_result && that.getParams() == params_result && that.getReturnType() == returnType_result && that.getThrowsClause() == throwsClause_result && that.getWhere() == where_result && that.getContract() == contract_result && that.getBody() == body_result)
//            return that;
//        else
//            return new FnDef(that.getSpan(), mods_result, name_result, staticParams_result, params_result, returnType_result, throwsClause_result, where_result, contract_result, that.getSelfName(), body_result);
//    }
//
//    public Node forNormalParamOnly(NormalParam that, List<Modifier> mods_result, Id name_result, Option<Type> type_result, Option<Expr> defaultExpr_result) {
//        if (that.getMods() == mods_result && that.getName() == name_result && that.getType() == type_result && that.getDefaultExpr() == defaultExpr_result)
//            return that;
//        else return new NormalParam(that.getSpan(), mods_result, name_result, type_result, defaultExpr_result);
//    }
//
//    public Node forVarargsParamOnly(VarargsParam that, List<Modifier> mods_result, Id name_result, VarargsType varargsType_result) {
//        if (that.getMods() == mods_result && that.getName() == name_result && that.getVarargsType() == varargsType_result)
//            return that;
//        else return new VarargsParam(that.getSpan(), mods_result, name_result, varargsType_result);
//    }
//
//    public Node forDimDeclOnly(DimDecl that, Id dim_result, Option<Type> derived_result, Option<Id> default_result) {
//        if (that.getDim() == dim_result && that.getDerived() == derived_result && that.getDefault() == default_result)
//            return that;
//        else return new DimDecl(that.getSpan(), dim_result, derived_result, default_result);
//    }
//
//    public Node forUnitDeclOnly(UnitDecl that, List<Id> units_result, Option<Type> dim_result, Option<Expr> def_result) {
//        if (that.getUnits() == units_result && that.getDim() == dim_result && that.getDef() == def_result) return that;
//        else return new UnitDecl(that.getSpan(), that.isSi_unit(), units_result, dim_result, def_result);
//    }
//
//    public Node forTestDeclOnly(TestDecl that, Id name_result, List<GeneratorClause> gens_result, Expr expr_result) {
//        if (that.getName() == name_result && that.getGens() == gens_result && that.getExpr() == expr_result)
//            return that;
//        else return new TestDecl(that.getSpan(), name_result, gens_result, expr_result);
//    }
//
//    public Node forPropertyDeclOnly(PropertyDecl that, Option<Id> name_result, List<Param> params_result, Expr expr_result) {
//        if (that.getName() == name_result && that.getParams() == params_result && that.getExpr() == expr_result)
//            return that;
//        else return new PropertyDecl(that.getSpan(), name_result, params_result, expr_result);
//    }
//
//    public Node forAbsExternalSyntaxOnly(AbsExternalSyntax that, SimpleName openExpander_result, Id name_result, SimpleName closeExpander_result) {
//        if (that.getOpenExpander() == openExpander_result && that.getName() == name_result && that.getCloseExpander() == closeExpander_result)
//            return that;
//        else return new AbsExternalSyntax(that.getSpan(), openExpander_result, name_result, closeExpander_result);
//    }
//
//    public Node forExternalSyntaxOnly(ExternalSyntax that, SimpleName openExpander_result, Id name_result, SimpleName closeExpander_result, Expr expr_result) {
//        if (that.getOpenExpander() == openExpander_result && that.getName() == name_result && that.getCloseExpander() == closeExpander_result && that.getExpr() == expr_result)
//            return that;
//        else
//            return new ExternalSyntax(that.getSpan(), openExpander_result, name_result, closeExpander_result, expr_result);
//    }
//
//    public Node forGrammarDefOnly(GrammarDef that, QualifiedIdName name_result, List<QualifiedIdName> extends_result, List<GrammarMemberDecl> members_result) {
//        if (that.getName() == name_result && that.getExtends() == extends_result && that.getMembers() == members_result)
//            return that;
//        else return new GrammarDef(that.getSpan(), name_result, extends_result, members_result);
//    }
//
//    public Node forNonterminalDefOnly(NonterminalDef that, QualifiedIdName name_result, Option<TraitType> type_result, Option<? extends Modifier> modifier_result, List<SyntaxDef> syntaxDefs_result) {
//        if (that.getName() == name_result && that.getType() == type_result && that.getModifier() == modifier_result && that.getSyntaxDefs() == syntaxDefs_result)
//            return that;
//        else return new NonterminalDef(that.getSpan(), name_result, type_result, modifier_result, syntaxDefs_result);
//    }
//
//    public Node forNonterminalExtensionDefOnly(NonterminalExtensionDef that, QualifiedIdName name_result, Option<TraitType> type_result, Option<? extends Modifier> modifier_result, List<SyntaxDef> syntaxDefs_result) {
//        if (that.getName() == name_result && that.getType() == type_result && that.getModifier() == modifier_result && that.getSyntaxDefs() == syntaxDefs_result)
//            return that;
//        else
//            return new NonterminalExtensionDef(that.getSpan(), name_result, type_result, modifier_result, syntaxDefs_result);
//    }
//
//    public Node for_TerminalDefOnly(_TerminalDef that, QualifiedIdName name_result, Option<TraitType> type_result, Option<? extends Modifier> modifier_result, SyntaxDef syntaxDef_result) {
//        if (that.getName() == name_result && that.getType() == type_result && that.getModifier() == modifier_result && that.getSyntaxDef() == syntaxDef_result)
//            return that;
//        else return new _TerminalDef(that.getSpan(), name_result, type_result, modifier_result, syntaxDef_result);
//    }
//
//    public Node forSyntaxDefOnly(SyntaxDef that, List<SyntaxSymbol> syntaxSymbols_result, Expr transformationExpression_result) {
//        if (that.getSyntaxSymbols() == syntaxSymbols_result && that.getTransformationExpression() == transformationExpression_result)
//            return that;
//        else return new SyntaxDef(that.getSpan(), syntaxSymbols_result, transformationExpression_result);
//    }
//
//    public Node forPrefixedSymbolOnly(PrefixedSymbol that, Option<Id> id_result, SyntaxSymbol symbol_result) {
//        if (that.getId() == id_result && that.getSymbol() == symbol_result) return that;
//        else return new PrefixedSymbol(that.getSpan(), id_result, symbol_result);
//    }
//
//    public Node forOptionalSymbolOnly(OptionalSymbol that, SyntaxSymbol symbol_result) {
//        if (that.getSymbol() == symbol_result) return that;
//        else return new OptionalSymbol(that.getSpan(), symbol_result);
//    }
//
//    public Node forRepeatSymbolOnly(RepeatSymbol that, SyntaxSymbol symbol_result) {
//        if (that.getSymbol() == symbol_result) return that;
//        else return new RepeatSymbol(that.getSpan(), symbol_result);
//    }
//
//    public Node forRepeatOneOrMoreSymbolOnly(RepeatOneOrMoreSymbol that, SyntaxSymbol symbol_result) {
//        if (that.getSymbol() == symbol_result) return that;
//        else return new RepeatOneOrMoreSymbol(that.getSpan(), symbol_result);
//    }
//
//    public Node forNoWhitespaceSymbolOnly(NoWhitespaceSymbol that, SyntaxSymbol symbol_result) {
//        if (that.getSymbol() == symbol_result) return that;
//        else return new NoWhitespaceSymbol(that.getSpan(), symbol_result);
//    }
//
//    public Node forWhitespaceSymbolOnly(WhitespaceSymbol that) {
//        return that;
//    }
//
//    public Node forTabSymbolOnly(TabSymbol that) {
//        return that;
//    }
//
//    public Node forFormfeedSymbolOnly(FormfeedSymbol that) {
//        return that;
//    }
//
//    public Node forCarriageReturnSymbolOnly(CarriageReturnSymbol that) {
//        return that;
//    }
//
//    public Node forBackspaceSymbolOnly(BackspaceSymbol that) {
//        return that;
//    }
//
//    public Node forNewlineSymbolOnly(NewlineSymbol that) {
//        return that;
//    }
//
//    public Node forBreaklineSymbolOnly(BreaklineSymbol that) {
//        return that;
//    }
//
//    public Node forItemSymbolOnly(ItemSymbol that) {
//        return that;
//    }
//
//    public Node forNonterminalSymbolOnly(NonterminalSymbol that, QualifiedName nonterminal_result) {
//        if (that.getNonterminal() == nonterminal_result) return that;
//        else return new NonterminalSymbol(that.getSpan(), nonterminal_result);
//    }
//
//    public Node forKeywordSymbolOnly(KeywordSymbol that) {
//        return that;
//    }
//
//    public Node forTokenSymbolOnly(TokenSymbol that) {
//        return that;
//    }
//
//    public Node forNotPredicateSymbolOnly(NotPredicateSymbol that, SyntaxSymbol symbol_result) {
//        if (that.getSymbol() == symbol_result) return that;
//        else return new NotPredicateSymbol(that.getSpan(), symbol_result);
//    }
//
//    public Node forAndPredicateSymbolOnly(AndPredicateSymbol that, SyntaxSymbol symbol_result) {
//        if (that.getSymbol() == symbol_result) return that;
//        else return new AndPredicateSymbol(that.getSpan(), symbol_result);
//    }
//
//    public Node forCharacterClassSymbolOnly(CharacterClassSymbol that, List<CharacterSymbol> characters_result) {
//        if (that.getCharacters() == characters_result) return that;
//        else return new CharacterClassSymbol(that.getSpan(), characters_result);
//    }
//
//    public Node forCharSymbolOnly(CharSymbol that) {
//        return that;
//    }
//
//    public Node forCharacterIntervalOnly(CharacterInterval that) {
//        return that;
//    }
//
//    public Node forAsExprOnly(AsExpr that, Expr expr_result, Type type_result) {
//        if (that.getExpr() == expr_result && that.getType() == type_result) return that;
//        else return new AsExpr(that.getSpan(), that.isParenthesized(), expr_result, type_result);
//    }
//
//    public Node forAsIfExprOnly(AsIfExpr that, Expr expr_result, Type type_result) {
//        if (that.getExpr() == expr_result && that.getType() == type_result) return that;
//        else return new AsIfExpr(that.getSpan(), that.isParenthesized(), expr_result, type_result);
//    }
//
//    public Node forAssignmentOnly(Assignment that, List<LHS> lhs_result, Option<Op> opr_result, Expr rhs_result) {
//        if (that.getLhs() == lhs_result && that.getOpr() == opr_result && that.getRhs() == rhs_result) return that;
//        else return new Assignment(that.getSpan(), that.isParenthesized(), lhs_result, opr_result, rhs_result);
//    }
//
//    public Node forBlockOnly(Block that, List<Expr> exprs_result) {
//        if (that.getExprs() == exprs_result) return that;
//        else return new Block(that.getSpan(), that.isParenthesized(), exprs_result);
//    }
//
//    public Node forCaseExprOnly(CaseExpr that, Option<Expr> param_result, Option<Op> compare_result, List<CaseClause> clauses_result, Option<Block> elseClause_result) {
//        if (that.getParam() == param_result && that.getCompare() == compare_result && that.getClauses() == clauses_result && that.getElseClause() == elseClause_result)
//            return that;
//        else
//            return new CaseExpr(that.getSpan(), that.isParenthesized(), param_result, compare_result, clauses_result, elseClause_result);
//    }
//
//    public Node forDoOnly(Do that, List<DoFront> fronts_result) {
//        if (that.getFronts() == fronts_result) return that;
//        else return new Do(that.getSpan(), that.isParenthesized(), fronts_result);
//    }
//
//    public Node forForOnly(For that, List<GeneratorClause> gens_result, DoFront body_result) {
//        if (that.getGens() == gens_result && that.getBody() == body_result) return that;
//        else return new For(that.getSpan(), that.isParenthesized(), gens_result, body_result);
//    }
//
//    public Node forIfOnly(If that, List<IfClause> clauses_result, Option<Block> elseClause_result) {
//        if (that.getClauses() == clauses_result && that.getElseClause() == elseClause_result) return that;
//        else return new If(that.getSpan(), that.isParenthesized(), clauses_result, elseClause_result);
//    }
//
//    public Node forLabelOnly(Label that, Id name_result, Block body_result) {
//        if (that.getName() == name_result && that.getBody() == body_result) return that;
//        else return new Label(that.getSpan(), that.isParenthesized(), name_result, body_result);
//    }
//
//    public Node forObjectExprOnly(ObjectExpr that, List<TraitTypeWhere> extendsClause_result, List<Decl> decls_result) {
//        if (that.getExtendsClause() == extendsClause_result && that.getDecls() == decls_result) return that;
//        else return new ObjectExpr(that.getSpan(), that.isParenthesized(), extendsClause_result, decls_result);
//    }
//
//    public Node for_RewriteObjectExprOnly(_RewriteObjectExpr that, List<TraitTypeWhere> extendsClause_result, List<Decl> decls_result, List<StaticParam> staticParams_result, List<StaticArg> staticArgs_result, Option<List<Param>> params_result) {
//        if (that.getExtendsClause() == extendsClause_result && that.getDecls() == decls_result && that.getStaticParams() == staticParams_result && that.getStaticArgs() == staticArgs_result && that.getParams() == params_result)
//            return that;
//        else
//            return new _RewriteObjectExpr(that.getSpan(), that.isParenthesized(), extendsClause_result, decls_result, that.getImplicitTypeParameters(), that.getGenSymName(), staticParams_result, staticArgs_result, params_result);
//    }
//
//    public Node forTryOnly(Try that, Block body_result, Option<Catch> catchClause_result, List<TraitType> forbid_result, Option<Block> finallyClause_result) {
//        if (that.getBody() == body_result && that.getCatchClause() == catchClause_result && that.getForbid() == forbid_result && that.getFinallyClause() == finallyClause_result)
//            return that;
//        else
//            return new Try(that.getSpan(), that.isParenthesized(), body_result, catchClause_result, forbid_result, finallyClause_result);
//    }
//
//    public Node forTupleExprOnly(TupleExpr that, List<Expr> exprs_result) {
//        if (that.getExprs() == exprs_result) return that;
//        else return new TupleExpr(that.getSpan(), that.isParenthesized(), exprs_result);
//    }
//
//    public Node forArgExprOnly(ArgExpr that, List<Expr> exprs_result, Option<VarargsExpr> varargs_result, List<KeywordExpr> keywords_result) {
//        if (that.getExprs() == exprs_result && that.getVarargs() == varargs_result && that.getKeywords() == keywords_result)
//            return that;
//        else
//            return new ArgExpr(that.getSpan(), that.isParenthesized(), exprs_result, varargs_result, keywords_result, that.isInApp());
//    }
//
//    public Node forTypecaseOnly(Typecase that, List<TypecaseClause> clauses_result, Option<Block> elseClause_result) {
//        if (that.getClauses() == clauses_result && that.getElseClause() == elseClause_result) return that;
//        else
//            return new Typecase(that.getSpan(), that.isParenthesized(), that.getBind(), clauses_result, elseClause_result);
//    }
//
//    public Node forWhileOnly(While that, Expr test_result, Do body_result) {
//        if (that.getTest() == test_result && that.getBody() == body_result) return that;
//        else return new While(that.getSpan(), that.isParenthesized(), test_result, body_result);
//    }
//
//    public Node forAccumulatorOnly(Accumulator that, List<StaticArg> staticArgs_result, OpName opr_result, List<GeneratorClause> gens_result, Expr body_result) {
//        if (that.getStaticArgs() == staticArgs_result && that.getOpr() == opr_result && that.getGens() == gens_result && that.getBody() == body_result)
//            return that;
//        else
//            return new Accumulator(that.getSpan(), that.isParenthesized(), staticArgs_result, opr_result, gens_result, body_result);
//    }
//
//    public Node forArrayComprehensionOnly(ArrayComprehension that, List<StaticArg> staticArgs_result, List<ArrayComprehensionClause> clauses_result) {
//        if (that.getStaticArgs() == staticArgs_result && that.getClauses() == clauses_result) return that;
//        else return new ArrayComprehension(that.getSpan(), that.isParenthesized(), staticArgs_result, clauses_result);
//    }
//
//    public Node forAtomicExprOnly(AtomicExpr that, Expr expr_result) {
//        if (that.getExpr() == expr_result) return that;
//        else return new AtomicExpr(that.getSpan(), that.isParenthesized(), expr_result);
//    }
//
//    public Node forExitOnly(Exit that, Option<Id> target_result, Option<Expr> returnExpr_result) {
//        if (that.getTarget() == target_result && that.getReturnExpr() == returnExpr_result) return that;
//        else return new Exit(that.getSpan(), that.isParenthesized(), target_result, returnExpr_result);
//    }
//
//    public Node forSpawnOnly(Spawn that, Expr body_result) {
//        if (that.getBody() == body_result) return that;
//        else return new Spawn(that.getSpan(), that.isParenthesized(), body_result);
//    }
//
//    public Node forThrowOnly(Throw that, Expr expr_result) {
//        if (that.getExpr() == expr_result) return that;
//        else return new Throw(that.getSpan(), that.isParenthesized(), expr_result);
//    }
//
//    public Node forTryAtomicExprOnly(TryAtomicExpr that, Expr expr_result) {
//        if (that.getExpr() == expr_result) return that;
//        else return new TryAtomicExpr(that.getSpan(), that.isParenthesized(), expr_result);
//    }
//
//    public Node forFnExprOnly(FnExpr that, SimpleName name_result, List<StaticParam> staticParams_result, List<Param> params_result, Option<Type> returnType_result, WhereClause where_result, Option<List<TraitType>> throwsClause_result, Expr body_result) {
//        if (that.getName() == name_result && that.getStaticParams() == staticParams_result && that.getParams() == params_result && that.getReturnType() == returnType_result && that.getWhere() == where_result && that.getThrowsClause() == throwsClause_result && that.getBody() == body_result)
//            return that;
//        else
//            return new FnExpr(that.getSpan(), that.isParenthesized(), name_result, staticParams_result, params_result, returnType_result, where_result, throwsClause_result, body_result);
//    }
//
//    public Node forLetFnOnly(LetFn that, List<Expr> body_result, List<FnDef> fns_result) {
//        if (that.getBody() == body_result && that.getFns() == fns_result) return that;
//        else return new LetFn(that.getSpan(), that.isParenthesized(), body_result, fns_result);
//    }
//
//    public Node forLocalVarDeclOnly(LocalVarDecl that, List<Expr> body_result, List<LValue> lhs_result, Option<Expr> rhs_result) {
//        if (that.getBody() == body_result && that.getLhs() == lhs_result && that.getRhs() == rhs_result) return that;
//        else return new LocalVarDecl(that.getSpan(), that.isParenthesized(), body_result, lhs_result, rhs_result);
//    }
//
//    public Node forGeneratedExprOnly(GeneratedExpr that, Expr expr_result, List<GeneratorClause> gens_result) {
//        if (that.getExpr() == expr_result && that.getGens() == gens_result) return that;
//        else return new GeneratedExpr(that.getSpan(), that.isParenthesized(), expr_result, gens_result);
//    }
//
//    public Node forSubscriptExprOnly(SubscriptExpr that, Expr obj_result, List<Expr> subs_result, Option<Enclosing> op_result) {
//        if (that.getObj() == obj_result && that.getSubs() == subs_result && that.getOp() == op_result) return that;
//        else return new SubscriptExpr(that.getSpan(), that.isParenthesized(), obj_result, subs_result, op_result);
//    }
//
//    public Node forFloatLiteralExprOnly(FloatLiteralExpr that) {
//        return that;
//    }
//
//    public Node forIntLiteralExprOnly(IntLiteralExpr that) {
//        return that;
//    }
//
//    public Node forCharLiteralExprOnly(CharLiteralExpr that) {
//        return that;
//    }
//
//    public Node forStringLiteralExprOnly(StringLiteralExpr that) {
//        return that;
//    }
//
//    public Node forVoidLiteralExprOnly(VoidLiteralExpr that) {
//        return that;
//    }
//
//    public Node forVarRefOnly(VarRef that, QualifiedIdName var_result) {
//        if (that.getVar() == var_result) return that;
//        else return new VarRef(that.getSpan(), that.isParenthesized(), var_result);
//    }
//
//    public Node forFieldRefOnly(FieldRef that, Expr obj_result, Id field_result) {
//        if (that.getObj() == obj_result && that.getField() == field_result) return that;
//        else return new FieldRef(that.getSpan(), that.isParenthesized(), obj_result, field_result);
//    }
//
//    public Node forFieldRefForSureOnly(FieldRefForSure that, Expr obj_result, Id field_result) {
//        if (that.getObj() == obj_result && that.getField() == field_result) return that;
//        else return new FieldRefForSure(that.getSpan(), that.isParenthesized(), obj_result, field_result);
//    }
//
//    public Node for_RewriteFieldRefOnly(_RewriteFieldRef that, Expr obj_result, Name field_result) {
//        if (that.getObj() == obj_result && that.getField() == field_result) return that;
//        else return new _RewriteFieldRef(that.getSpan(), that.isParenthesized(), obj_result, field_result);
//    }
//
//    public Node forFnRefOnly(FnRef that, List<QualifiedIdName> fns_result, List<StaticArg> staticArgs_result) {
//        if (that.getFns() == fns_result && that.getStaticArgs() == staticArgs_result) return that;
//        else return new FnRef(that.getSpan(), that.isParenthesized(), fns_result, staticArgs_result);
//    }
//
//    public Node for_RewriteFnRefOnly(_RewriteFnRef that, Expr fn_result, List<StaticArg> staticArgs_result) {
//        if (that.getFn() == fn_result && that.getStaticArgs() == staticArgs_result) return that;
//        else return new _RewriteFnRef(that.getSpan(), that.isParenthesized(), fn_result, staticArgs_result);
//    }
//
//    public Node forOpRefOnly(OpRef that, List<QualifiedOpName> ops_result, List<StaticArg> staticArgs_result) {
//        if (that.getOps() == ops_result && that.getStaticArgs() == staticArgs_result) return that;
//        else return new OpRef(that.getSpan(), that.isParenthesized(), ops_result, staticArgs_result);
//    }
//
//    public Node forLooseJuxtOnly(LooseJuxt that, List<Expr> exprs_result) {
//        if (that.getExprs() == exprs_result) return that;
//        else return new LooseJuxt(that.getSpan(), that.isParenthesized(), exprs_result);
//    }
//
//    public Node forTightJuxtOnly(TightJuxt that, List<Expr> exprs_result) {
//        if (that.getExprs() == exprs_result) return that;
//        else return new TightJuxt(that.getSpan(), that.isParenthesized(), exprs_result);
//    }
//
//    public Node forOprExprOnly(OprExpr that, OpRef op_result, List<Expr> args_result) {
//        if (that.getOp() == op_result && that.getArgs() == args_result) return that;
//        else return new OprExpr(that.getSpan(), that.isParenthesized(), op_result, args_result);
//    }
//
//    public Node forChainExprOnly(ChainExpr that, Expr first_result) {
//        if (that.getFirst() == first_result) return that;
//        else return new ChainExpr(that.getSpan(), that.isParenthesized(), first_result, that.getLinks());
//    }
//
//    public Node forCoercionInvocationOnly(CoercionInvocation that, TraitType type_result, List<StaticArg> staticArgs_result, Expr arg_result) {
//        if (that.getType() == type_result && that.getStaticArgs() == staticArgs_result && that.getArg() == arg_result)
//            return that;
//        else
//            return new CoercionInvocation(that.getSpan(), that.isParenthesized(), type_result, staticArgs_result, arg_result);
//    }
//
//    public Node forMethodInvocationOnly(MethodInvocation that, Expr obj_result, Id method_result, List<StaticArg> staticArgs_result, Expr arg_result) {
//        if (that.getObj() == obj_result && that.getMethod() == method_result && that.getStaticArgs() == staticArgs_result && that.getArg() == arg_result)
//            return that;
//        else
//            return new MethodInvocation(that.getSpan(), that.isParenthesized(), obj_result, method_result, staticArgs_result, arg_result);
//    }
//
//    public Node forMathPrimaryOnly(MathPrimary that, Expr front_result, List<MathItem> rest_result) {
//        if (that.getFront() == front_result && that.getRest() == rest_result) return that;
//        else return new MathPrimary(that.getSpan(), that.isParenthesized(), front_result, rest_result);
//    }
//
//    public Node forArrayElementOnly(ArrayElement that, Expr element_result) {
//        if (that.getElement() == element_result) return that;
//        else return new ArrayElement(that.getSpan(), that.isParenthesized(), element_result);
//    }
//
//    public Node forArrayElementsOnly(ArrayElements that, List<ArrayExpr> elements_result) {
//        if (that.getElements() == elements_result) return that;
//        else return new ArrayElements(that.getSpan(), that.isParenthesized(), that.getDimension(), elements_result);
//    }
//
//    public Node forExponentTypeOnly(ExponentType that, Type base_result, IntExpr power_result) {
//        if (that.getBase() == base_result && that.getPower() == power_result) return that;
//        else return new ExponentType(that.getSpan(), that.isParenthesized(), base_result, power_result);
//    }
//
//    public Node forBaseDimOnly(BaseDim that) {
//        return that;
//    }
//
//    public Node forDimRefOnly(DimRef that, QualifiedIdName name_result) {
//        if (that.getName() == name_result) return that;
//        else return new DimRef(that.getSpan(), that.isParenthesized(), name_result);
//    }
//
//    public Node forProductDimOnly(ProductDim that, DimExpr multiplier_result, DimExpr multiplicand_result) {
//        if (that.getMultiplier() == multiplier_result && that.getMultiplicand() == multiplicand_result) return that;
//        else return new ProductDim(that.getSpan(), that.isParenthesized(), multiplier_result, multiplicand_result);
//    }
//
//    public Node forQuotientDimOnly(QuotientDim that, DimExpr numerator_result, DimExpr denominator_result) {
//        if (that.getNumerator() == numerator_result && that.getDenominator() == denominator_result) return that;
//        else return new QuotientDim(that.getSpan(), that.isParenthesized(), numerator_result, denominator_result);
//    }
//
//    public Node forExponentDimOnly(ExponentDim that, DimExpr base_result, IntExpr power_result) {
//        if (that.getBase() == base_result && that.getPower() == power_result) return that;
//        else return new ExponentDim(that.getSpan(), that.isParenthesized(), base_result, power_result);
//    }
//
//    public Node forOpDimOnly(OpDim that, DimExpr val_result, Op op_result) {
//        if (that.getVal() == val_result && that.getOp() == op_result) return that;
//        else return new OpDim(that.getSpan(), that.isParenthesized(), val_result, op_result);
//    }
//
//    public Node forArrowTypeOnly(ArrowType that, Type domain_result, Type range_result, Option<List<Type>> throwsClause_result) {
//        if (that.getDomain() == domain_result && that.getRange() == range_result && that.getThrowsClause() == throwsClause_result)
//            return that;
//        else
//            return new ArrowType(that.getSpan(), that.isParenthesized(), domain_result, range_result, throwsClause_result, that.isIo());
//    }
//
//    public Node for_RewriteGenericArrowTypeOnly(_RewriteGenericArrowType that, Type domain_result, Type range_result, Option<List<Type>> throwsClause_result, List<StaticParam> staticParams_result, WhereClause where_result) {
//        if (that.getDomain() == domain_result && that.getRange() == range_result && that.getThrowsClause() == throwsClause_result && that.getStaticParams() == staticParams_result && that.getWhere() == where_result)
//            return that;
//        else
//            return new _RewriteGenericArrowType(that.getSpan(), that.isParenthesized(), domain_result, range_result, throwsClause_result, that.isIo(), staticParams_result, where_result);
//    }
//
//    public Node forBottomTypeOnly(BottomType that) {
//        return that;
//    }
//
//    public Node forArrayTypeOnly(ArrayType that, Type element_result, Indices indices_result) {
//        if (that.getElement() == element_result && that.getIndices() == indices_result) return that;
//        else return new ArrayType(that.getSpan(), that.isParenthesized(), element_result, indices_result);
//    }
//
//    public Node forIdTypeOnly(IdType that, QualifiedIdName name_result) {
//        if (that.getName() == name_result) return that;
//        else return new IdType(that.getSpan(), that.isParenthesized(), name_result);
//    }
//
//    public Node forInferenceVarTypeOnly(InferenceVarType that) {
//        return that;
//    }
//
//    public Node forMatrixTypeOnly(MatrixType that, Type element_result, List<ExtentRange> dimensions_result) {
//        if (that.getElement() == element_result && that.getDimensions() == dimensions_result) return that;
//        else return new MatrixType(that.getSpan(), that.isParenthesized(), element_result, dimensions_result);
//    }
//
//    public Node forInstantiatedTypeOnly(InstantiatedType that, QualifiedIdName name_result, List<StaticArg> args_result) {
//        if (that.getName() == name_result && that.getArgs() == args_result) return that;
//        else return new InstantiatedType(that.getSpan(), that.isParenthesized(), name_result, args_result);
//    }
//
//    public Node forTupleTypeOnly(TupleType that, List<Type> elements_result) {
//        if (that.getElements() == elements_result) return that;
//        else return new TupleType(that.getSpan(), that.isParenthesized(), elements_result);
//    }
//
//    public Node forArgTypeOnly(ArgType that, List<Type> elements_result, Option<VarargsType> varargs_result, List<KeywordType> keywords_result) {
//        if (that.getElements() == elements_result && that.getVarargs() == varargs_result && that.getKeywords() == keywords_result)
//            return that;
//        else
//            return new ArgType(that.getSpan(), that.isParenthesized(), elements_result, varargs_result, keywords_result, that.isInArrow());
//    }
//
//    public Node forVoidTypeOnly(VoidType that) {
//        return that;
//    }
//
//    public Node forIntersectionTypeOnly(IntersectionType that, Set<Type> elements_result) {
//        if (that.getElements() == elements_result) return that;
//        else return new IntersectionType(that.getSpan(), that.isParenthesized(), elements_result);
//    }
//
//    public Node forUnionTypeOnly(UnionType that, Set<Type> elements_result) {
//        if (that.getElements() == elements_result) return that;
//        else return new UnionType(that.getSpan(), that.isParenthesized(), elements_result);
//    }
//
//    public Node forAndTypeOnly(AndType that, Type first_result, Type second_result) {
//        if (that.getFirst() == first_result && that.getSecond() == second_result) return that;
//        else return new AndType(that.getSpan(), that.isParenthesized(), first_result, second_result);
//    }
//
//    public Node forOrTypeOnly(OrType that, Type first_result, Type second_result) {
//        if (that.getFirst() == first_result && that.getSecond() == second_result) return that;
//        else return new OrType(that.getSpan(), that.isParenthesized(), first_result, second_result);
//    }
//
//    public Node forTaggedDimTypeOnly(TaggedDimType that, Type type_result, DimExpr dim_result, Option<Expr> unit_result) {
//        if (that.getType() == type_result && that.getDim() == dim_result && that.getUnit() == unit_result) return that;
//        else return new TaggedDimType(that.getSpan(), that.isParenthesized(), type_result, dim_result, unit_result);
//    }
//
//    public Node forTaggedUnitTypeOnly(TaggedUnitType that, Type type_result, Expr unit_result) {
//        if (that.getType() == type_result && that.getUnit() == unit_result) return that;
//        else return new TaggedUnitType(that.getSpan(), that.isParenthesized(), type_result, unit_result);
//    }
//
//    public Node forIdArgOnly(IdArg that, QualifiedIdName name_result) {
//        if (that.getName() == name_result) return that;
//        else return new IdArg(that.getSpan(), that.isParenthesized(), name_result);
//    }
//
//    public Node forTypeArgOnly(TypeArg that, Type type_result) {
//        if (that.getType() == type_result) return that;
//        else return new TypeArg(that.getSpan(), that.isParenthesized(), type_result);
//    }
//
//    public Node forIntArgOnly(IntArg that, IntExpr val_result) {
//        if (that.getVal() == val_result) return that;
//        else return new IntArg(that.getSpan(), that.isParenthesized(), val_result);
//    }
//
//    public Node forBoolArgOnly(BoolArg that, BoolExpr bool_result) {
//        if (that.getBool() == bool_result) return that;
//        else return new BoolArg(that.getSpan(), that.isParenthesized(), bool_result);
//    }
//
//    public Node forOprArgOnly(OprArg that, Op name_result) {
//        if (that.getName() == name_result) return that;
//        else return new OprArg(that.getSpan(), that.isParenthesized(), name_result);
//    }
//
//    public Node forDimArgOnly(DimArg that, DimExpr dim_result) {
//        if (that.getDim() == dim_result) return that;
//        else return new DimArg(that.getSpan(), that.isParenthesized(), dim_result);
//    }
//
//    public Node forUnitArgOnly(UnitArg that, Expr unit_result) {
//        if (that.getUnit() == unit_result) return that;
//        else return new UnitArg(that.getSpan(), that.isParenthesized(), unit_result);
//    }
//
//    public Node for_RewriteImplicitTypeOnly(_RewriteImplicitType that) {
//        return that;
//    }
//
//    public Node for_RewriteIntersectionTypeOnly(_RewriteIntersectionType that, List<Type> elements_result) {
//        if (that.getElements() == elements_result) return that;
//        else return new _RewriteIntersectionType(that.getSpan(), that.isParenthesized(), elements_result);
//    }
//
//    public Node for_RewriteUnionTypeOnly(_RewriteUnionType that, List<Type> elements_result) {
//        if (that.getElements() == elements_result) return that;
//        else return new _RewriteUnionType(that.getSpan(), that.isParenthesized(), elements_result);
//    }
//
//    public Node for_RewriteFixedPointTypeOnly(_RewriteFixedPointType that, _RewriteImplicitType var_result, Type body_result) {
//        if (that.getVar() == var_result && that.getBody() == body_result) return that;
//        else return new _RewriteFixedPointType(that.getSpan(), that.isParenthesized(), var_result, body_result);
//    }
//
//    public Node forNumberConstraintOnly(NumberConstraint that, IntLiteralExpr val_result) {
//        if (that.getVal() == val_result) return that;
//        else return new NumberConstraint(that.getSpan(), that.isParenthesized(), val_result);
//    }
//
//    public Node forIntRefOnly(IntRef that, QualifiedIdName name_result) {
//        if (that.getName() == name_result) return that;
//        else return new IntRef(that.getSpan(), that.isParenthesized(), name_result);
//    }
//
//    public Node forSumConstraintOnly(SumConstraint that, IntExpr left_result, IntExpr right_result) {
//        if (that.getLeft() == left_result && that.getRight() == right_result) return that;
//        else return new SumConstraint(that.getSpan(), that.isParenthesized(), left_result, right_result);
//    }
//
//    public Node forMinusConstraintOnly(MinusConstraint that, IntExpr left_result, IntExpr right_result) {
//        if (that.getLeft() == left_result && that.getRight() == right_result) return that;
//        else return new MinusConstraint(that.getSpan(), that.isParenthesized(), left_result, right_result);
//    }
//
//    public Node forProductConstraintOnly(ProductConstraint that, IntExpr left_result, IntExpr right_result) {
//        if (that.getLeft() == left_result && that.getRight() == right_result) return that;
//        else return new ProductConstraint(that.getSpan(), that.isParenthesized(), left_result, right_result);
//    }
//
//    public Node forExponentConstraintOnly(ExponentConstraint that, IntExpr left_result, IntExpr right_result) {
//        if (that.getLeft() == left_result && that.getRight() == right_result) return that;
//        else return new ExponentConstraint(that.getSpan(), that.isParenthesized(), left_result, right_result);
//    }
//
//    public Node forBoolConstantOnly(BoolConstant that) {
//        return that;
//    }
//
//    public Node forBoolRefOnly(BoolRef that, QualifiedIdName name_result) {
//        if (that.getName() == name_result) return that;
//        else return new BoolRef(that.getSpan(), that.isParenthesized(), name_result);
//    }
//
//    public Node forNotConstraintOnly(NotConstraint that, BoolExpr bool_result) {
//        if (that.getBool() == bool_result) return that;
//        else return new NotConstraint(that.getSpan(), that.isParenthesized(), bool_result);
//    }
//
//    public Node forOrConstraintOnly(OrConstraint that, BoolExpr left_result, BoolExpr right_result) {
//        if (that.getLeft() == left_result && that.getRight() == right_result) return that;
//        else return new OrConstraint(that.getSpan(), that.isParenthesized(), left_result, right_result);
//    }
//
//    public Node forAndConstraintOnly(AndConstraint that, BoolExpr left_result, BoolExpr right_result) {
//        if (that.getLeft() == left_result && that.getRight() == right_result) return that;
//        else return new AndConstraint(that.getSpan(), that.isParenthesized(), left_result, right_result);
//    }
//
//    public Node forImpliesConstraintOnly(ImpliesConstraint that, BoolExpr left_result, BoolExpr right_result) {
//        if (that.getLeft() == left_result && that.getRight() == right_result) return that;
//        else return new ImpliesConstraint(that.getSpan(), that.isParenthesized(), left_result, right_result);
//    }
//
//    public Node forBEConstraintOnly(BEConstraint that, BoolExpr left_result, BoolExpr right_result) {
//        if (that.getLeft() == left_result && that.getRight() == right_result) return that;
//        else return new BEConstraint(that.getSpan(), that.isParenthesized(), left_result, right_result);
//    }
//
//    public Node forWhereClauseOnly(WhereClause that, List<WhereBinding> bindings_result, List<WhereConstraint> constraints_result) {
//        if (that.getBindings() == bindings_result && that.getConstraints() == constraints_result) return that;
//        else return new WhereClause(that.getSpan(), bindings_result, constraints_result);
//    }
//
//    public Node forWhereTypeOnly(WhereType that, Id name_result, List<TraitType> supers_result) {
//        if (that.getName() == name_result && that.getSupers() == supers_result) return that;
//        else return new WhereType(that.getSpan(), name_result, supers_result);
//    }
//
//    public Node forWhereNatOnly(WhereNat that, Id name_result) {
//        if (that.getName() == name_result) return that;
//        else return new WhereNat(that.getSpan(), name_result);
//    }
//
//    public Node forWhereIntOnly(WhereInt that, Id name_result) {
//        if (that.getName() == name_result) return that;
//        else return new WhereInt(that.getSpan(), name_result);
//    }
//
//    public Node forWhereBoolOnly(WhereBool that, Id name_result) {
//        if (that.getName() == name_result) return that;
//        else return new WhereBool(that.getSpan(), name_result);
//    }
//
//    public Node forWhereUnitOnly(WhereUnit that, Id name_result) {
//        if (that.getName() == name_result) return that;
//        else return new WhereUnit(that.getSpan(), name_result);
//    }
//
//    public Node forWhereExtendsOnly(WhereExtends that, Id name_result, List<TraitType> supers_result) {
//        if (that.getName() == name_result && that.getSupers() == supers_result) return that;
//        else return new WhereExtends(that.getSpan(), name_result, supers_result);
//    }
//
//    public Node forTypeAliasOnly(TypeAlias that, Id name_result, List<StaticParam> staticParams_result, Type type_result) {
//        if (that.getName() == name_result && that.getStaticParams() == staticParams_result && that.getType() == type_result)
//            return that;
//        else return new TypeAlias(that.getSpan(), name_result, staticParams_result, type_result);
//    }
//
//    public Node forWhereCoercesOnly(WhereCoerces that, Type left_result, Type right_result) {
//        if (that.getLeft() == left_result && that.getRight() == right_result) return that;
//        else return new WhereCoerces(that.getSpan(), left_result, right_result);
//    }
//
//    public Node forWhereWidensOnly(WhereWidens that, Type left_result, Type right_result) {
//        if (that.getLeft() == left_result && that.getRight() == right_result) return that;
//        else return new WhereWidens(that.getSpan(), left_result, right_result);
//    }
//
//    public Node forWhereWidensCoercesOnly(WhereWidensCoerces that, Type left_result, Type right_result) {
//        if (that.getLeft() == left_result && that.getRight() == right_result) return that;
//        else return new WhereWidensCoerces(that.getSpan(), left_result, right_result);
//    }
//
//    public Node forWhereEqualsOnly(WhereEquals that, QualifiedIdName left_result, QualifiedIdName right_result) {
//        if (that.getLeft() == left_result && that.getRight() == right_result) return that;
//        else return new WhereEquals(that.getSpan(), left_result, right_result);
//    }
//
//    public Node forUnitConstraintOnly(UnitConstraint that, Id name_result) {
//        if (that.getName() == name_result) return that;
//        else return new UnitConstraint(that.getSpan(), name_result);
//    }
//
//    public Node forLEConstraintOnly(LEConstraint that, IntExpr left_result, IntExpr right_result) {
//        if (that.getLeft() == left_result && that.getRight() == right_result) return that;
//        else return new LEConstraint(that.getSpan(), left_result, right_result);
//    }
//
//    public Node forLTConstraintOnly(LTConstraint that, IntExpr left_result, IntExpr right_result) {
//        if (that.getLeft() == left_result && that.getRight() == right_result) return that;
//        else return new LTConstraint(that.getSpan(), left_result, right_result);
//    }
//
//    public Node forGEConstraintOnly(GEConstraint that, IntExpr left_result, IntExpr right_result) {
//        if (that.getLeft() == left_result && that.getRight() == right_result) return that;
//        else return new GEConstraint(that.getSpan(), left_result, right_result);
//    }
//
//    public Node forGTConstraintOnly(GTConstraint that, IntExpr left_result, IntExpr right_result) {
//        if (that.getLeft() == left_result && that.getRight() == right_result) return that;
//        else return new GTConstraint(that.getSpan(), left_result, right_result);
//    }
//
//    public Node forIEConstraintOnly(IEConstraint that, IntExpr left_result, IntExpr right_result) {
//        if (that.getLeft() == left_result && that.getRight() == right_result) return that;
//        else return new IEConstraint(that.getSpan(), left_result, right_result);
//    }
//
//    public Node forBoolConstraintExprOnly(BoolConstraintExpr that, BoolConstraint constraint_result) {
//        if (that.getConstraint() == constraint_result) return that;
//        else return new BoolConstraintExpr(that.getSpan(), constraint_result);
//    }
//
//    public Node forContractOnly(Contract that, Option<List<Expr>> requires_result, Option<List<EnsuresClause>> ensures_result, Option<List<Expr>> invariants_result) {
//        if (that.getRequires() == requires_result && that.getEnsures() == ensures_result && that.getInvariants() == invariants_result)
//            return that;
//        else return new Contract(that.getSpan(), requires_result, ensures_result, invariants_result);
//    }
//
//    public Node forEnsuresClauseOnly(EnsuresClause that, Expr post_result, Option<Expr> pre_result) {
//        if (that.getPost() == post_result && that.getPre() == pre_result) return that;
//        else return new EnsuresClause(that.getSpan(), post_result, pre_result);
//    }
//
//    public Node forModifierAbstractOnly(ModifierAbstract that) {
//        return that;
//    }
//
//    public Node forModifierAtomicOnly(ModifierAtomic that) {
//        return that;
//    }
//
//    public Node forModifierGetterOnly(ModifierGetter that) {
//        return that;
//    }
//
//    public Node forModifierHiddenOnly(ModifierHidden that) {
//        return that;
//    }
//
//    public Node forModifierIOOnly(ModifierIO that) {
//        return that;
//    }
//
//    public Node forModifierOverrideOnly(ModifierOverride that) {
//        return that;
//    }
//
//    public Node forModifierPrivateOnly(ModifierPrivate that) {
//        return that;
//    }
//
//    public Node forModifierSettableOnly(ModifierSettable that) {
//        return that;
//    }
//
//    public Node forModifierSetterOnly(ModifierSetter that) {
//        return that;
//    }
//
//    public Node forModifierTestOnly(ModifierTest that) {
//        return that;
//    }
//
//    public Node forModifierTransientOnly(ModifierTransient that) {
//        return that;
//    }
//
//    public Node forModifierValueOnly(ModifierValue that) {
//        return that;
//    }
//
//    public Node forModifierVarOnly(ModifierVar that) {
//        return that;
//    }
//
//    public Node forModifierWidensOnly(ModifierWidens that) {
//        return that;
//    }
//
//    public Node forModifierWrappedOnly(ModifierWrapped that) {
//        return that;
//    }
//
//    public Node forOperatorParamOnly(OperatorParam that, Op name_result) {
//        if (that.getName() == name_result) return that;
//        else return new OperatorParam(that.getSpan(), name_result);
//    }
//
//    public Node forBoolParamOnly(BoolParam that, Id name_result) {
//        if (that.getName() == name_result) return that;
//        else return new BoolParam(that.getSpan(), name_result);
//    }
//
//    public Node forDimensionParamOnly(DimensionParam that, Id name_result) {
//        if (that.getName() == name_result) return that;
//        else return new DimensionParam(that.getSpan(), name_result);
//    }
//
//    public Node forIntParamOnly(IntParam that, Id name_result) {
//        if (that.getName() == name_result) return that;
//        else return new IntParam(that.getSpan(), name_result);
//    }
//
//    public Node forNatParamOnly(NatParam that, Id name_result) {
//        if (that.getName() == name_result) return that;
//        else return new NatParam(that.getSpan(), name_result);
//    }
//
//    public Node forSimpleTypeParamOnly(SimpleTypeParam that, Id name_result, List<TraitType> extendsClause_result) {
//        if (that.getName() == name_result && that.getExtendsClause() == extendsClause_result) return that;
//        else return new SimpleTypeParam(that.getSpan(), name_result, extendsClause_result, that.isAbsorbs());
//    }
//
//    public Node forUnitParamOnly(UnitParam that, Id name_result, Option<Type> dim_result) {
//        if (that.getName() == name_result && that.getDim() == dim_result) return that;
//        else return new UnitParam(that.getSpan(), name_result, dim_result, that.isAbsorbs());
//    }
//
//    public Node forAPINameOnly(APIName that, List<Id> ids_result) {
//        if (that.getIds() == ids_result) return that;
//        else return new APIName(that.getSpan(), ids_result);
//    }
//
//    public Node forQualifiedIdNameOnly(QualifiedIdName that, Option<APIName> api_result, Id name_result) {
//        if (that.getApi() == api_result && that.getName() == name_result) return that;
//        else return new QualifiedIdName(that.getSpan(), api_result, name_result);
//    }
//
//    public Node forQualifiedOpNameOnly(QualifiedOpName that, Option<APIName> api_result, OpName name_result) {
//        if (that.getApi() == api_result && that.getName() == name_result) return that;
//        else return new QualifiedOpName(that.getSpan(), api_result, name_result);
//    }
//
//    public Node forIdOnly(Id that) {
//        return that;
//    }
//
//    public Node forOpOnly(Op that, Option<Fixity> fixity_result) {
//        if (that.getFixity() == fixity_result) return that;
//        else return new Op(that.getSpan(), that.getText(), fixity_result);
//    }
//
//    public Node forEnclosingOnly(Enclosing that, Op open_result, Op close_result) {
//        if (that.getOpen() == open_result && that.getClose() == close_result) return that;
//        else return new Enclosing(that.getSpan(), open_result, close_result);
//    }
//
//    public Node forAnonymousFnNameOnly(AnonymousFnName that) {
//        return that;
//    }
//
//    public Node forConstructorFnNameOnly(ConstructorFnName that, GenericWithParams def_result) {
//        if (that.getDef() == def_result) return that;
//        else return new ConstructorFnName(that.getSpan(), def_result);
//    }
//
//    public Node forArrayComprehensionClauseOnly(ArrayComprehensionClause that, List<Expr> bind_result, Expr init_result, List<GeneratorClause> gens_result) {
//        if (that.getBind() == bind_result && that.getInit() == init_result && that.getGens() == gens_result)
//            return that;
//        else return new ArrayComprehensionClause(that.getSpan(), bind_result, init_result, gens_result);
//    }
//
//    public Node forKeywordExprOnly(KeywordExpr that, Id name_result, Expr init_result) {
//        if (that.getName() == name_result && that.getInit() == init_result) return that;
//        else return new KeywordExpr(that.getSpan(), name_result, init_result);
//    }
//
//    public Node forCaseClauseOnly(CaseClause that, Expr match_result, Block body_result) {
//        if (that.getMatch() == match_result && that.getBody() == body_result) return that;
//        else return new CaseClause(that.getSpan(), match_result, body_result);
//    }
//
//    public Node forCatchOnly(Catch that, Id name_result, List<CatchClause> clauses_result) {
//        if (that.getName() == name_result && that.getClauses() == clauses_result) return that;
//        else return new Catch(that.getSpan(), name_result, clauses_result);
//    }
//
//    public Node forCatchClauseOnly(CatchClause that, TraitType match_result, Block body_result) {
//        if (that.getMatch() == match_result && that.getBody() == body_result) return that;
//        else return new CatchClause(that.getSpan(), match_result, body_result);
//    }
//
//    public Node forDoFrontOnly(DoFront that, Option<Expr> loc_result, Block expr_result) {
//        if (that.getLoc() == loc_result && that.getExpr() == expr_result) return that;
//        else return new DoFront(that.getSpan(), loc_result, that.isAtomic(), expr_result);
//    }
//
//    public Node forIfClauseOnly(IfClause that, Expr test_result, Block body_result) {
//        if (that.getTest() == test_result && that.getBody() == body_result) return that;
//        else return new IfClause(that.getSpan(), test_result, body_result);
//    }
//
//    public Node forTypecaseClauseOnly(TypecaseClause that, List<Type> match_result, Block body_result) {
//        if (that.getMatch() == match_result && that.getBody() == body_result) return that;
//        else return new TypecaseClause(that.getSpan(), match_result, body_result);
//    }
//
//    public Node forExtentRangeOnly(ExtentRange that, Option<StaticArg> base_result, Option<StaticArg> size_result) {
//        if (that.getBase() == base_result && that.getSize() == size_result) return that;
//        else return new ExtentRange(that.getSpan(), base_result, size_result);
//    }
//
//    public Node forGeneratorClauseOnly(GeneratorClause that, List<Id> bind_result, Expr init_result) {
//        if (that.getBind() == bind_result && that.getInit() == init_result) return that;
//        else return new GeneratorClause(that.getSpan(), bind_result, init_result);
//    }
//
//    public Node forVarargsExprOnly(VarargsExpr that, Expr varargs_result) {
//        if (that.getVarargs() == varargs_result) return that;
//        else return new VarargsExpr(that.getSpan(), varargs_result);
//    }
//
//    public Node forVarargsTypeOnly(VarargsType that, Type type_result) {
//        if (that.getType() == type_result) return that;
//        else return new VarargsType(that.getSpan(), type_result);
//    }
//
//    public Node forKeywordTypeOnly(KeywordType that, Id name_result, Type type_result) {
//        if (that.getName() == name_result && that.getType() == type_result) return that;
//        else return new KeywordType(that.getSpan(), name_result, type_result);
//    }
//
//    public Node forTraitTypeWhereOnly(TraitTypeWhere that, TraitType type_result, WhereClause where_result) {
//        if (that.getType() == type_result && that.getWhere() == where_result) return that;
//        else return new TraitTypeWhere(that.getSpan(), type_result, where_result);
//    }
//
//    public Node forIndicesOnly(Indices that, List<ExtentRange> extents_result) {
//        if (that.getExtents() == extents_result) return that;
//        else return new Indices(that.getSpan(), extents_result);
//    }
//
//    public Node forParenthesisDelimitedMIOnly(ParenthesisDelimitedMI that, Expr expr_result) {
//        if (that.getExpr() == expr_result) return that;
//        else return new ParenthesisDelimitedMI(that.getSpan(), expr_result);
//    }
//
//    public Node forNonParenthesisDelimitedMIOnly(NonParenthesisDelimitedMI that, Expr expr_result) {
//        if (that.getExpr() == expr_result) return that;
//        else return new NonParenthesisDelimitedMI(that.getSpan(), expr_result);
//    }
//
//    public Node forExponentiationMIOnly(ExponentiationMI that, Op op_result, Option<Expr> expr_result) {
//        if (that.getOp() == op_result && that.getExpr() == expr_result) return that;
//        else return new ExponentiationMI(that.getSpan(), op_result, expr_result);
//    }
//
//    public Node forSubscriptingMIOnly(SubscriptingMI that, Enclosing op_result, List<Expr> exprs_result) {
//        if (that.getOp() == op_result && that.getExprs() == exprs_result) return that;
//        else return new SubscriptingMI(that.getSpan(), op_result, exprs_result);
//    }
//
//    public Node forInFixityOnly(InFixity that) {
//        return that;
//    }
//
//    public Node forPreFixityOnly(PreFixity that) {
//        return that;
//    }
//
//    public Node forPostFixityOnly(PostFixity that) {
//        return that;
//    }
//
//    public Node forNoFixityOnly(NoFixity that) {
//        return that;
//    }
//
//    public Node forMultiFixityOnly(MultiFixity that) {
//        return that;
//    }
//
//    public Node forEnclosingFixityOnly(EnclosingFixity that) {
//        return that;
//    }
//
//    public Node forBigFixityOnly(BigFixity that) {
//        return that;
//    }
//
//
//    /**
//     * Methods to recur on each child.
//     */
//    public Node forComponent(Component that) {
//        APIName name_result = (APIName) that.getName().accept(this);
//        List<Import> imports_result = recurOnListOfImport(that.getImports());
//        List<Export> exports_result = recurOnListOfExport(that.getExports());
//        List<Decl> decls_result = recurOnListOfDecl(that.getDecls());
//        return forComponentOnly(that, name_result, imports_result, exports_result, decls_result);
//    }
//
//    public Node forApi(Api that) {
//        APIName name_result = (APIName) that.getName().accept(this);
//        List<Import> imports_result = recurOnListOfImport(that.getImports());
//        List<AbsDecl> decls_result = recurOnListOfAbsDecl(that.getDecls());
//        return forApiOnly(that, name_result, imports_result, decls_result);
//    }
//
//    public Node forImportStar(ImportStar that) {
//        APIName api_result = (APIName) that.getApi().accept(this);
//        List<SimpleName> except_result = recurOnListOfSimpleName(that.getExcept());
//        return forImportStarOnly(that, api_result, except_result);
//    }
//
//    public Node forImportNames(ImportNames that) {
//        APIName api_result = (APIName) that.getApi().accept(this);
//        List<AliasedSimpleName> aliasedNames_result = recurOnListOfAliasedSimpleName(that.getAliasedNames());
//        return forImportNamesOnly(that, api_result, aliasedNames_result);
//    }
//
//    public Node forImportApi(ImportApi that) {
//        List<AliasedAPIName> apis_result = recurOnListOfAliasedAPIName(that.getApis());
//        return forImportApiOnly(that, apis_result);
//    }
//
//    public Node forAliasedSimpleName(AliasedSimpleName that) {
//        SimpleName name_result = (SimpleName) that.getName().accept(this);
//        Option<SimpleName> alias_result = recurOnOptionOfSimpleName(that.getAlias());
//        return forAliasedSimpleNameOnly(that, name_result, alias_result);
//    }
//
//    public Node forAliasedAPIName(AliasedAPIName that) {
//        APIName api_result = (APIName) that.getApi().accept(this);
//        Option<Id> alias_result = recurOnOptionOfId(that.getAlias());
//        return forAliasedAPINameOnly(that, api_result, alias_result);
//    }
//
//    public Node forExport(Export that) {
//        List<APIName> apis_result = recurOnListOfAPIName(that.getApis());
//        return forExportOnly(that, apis_result);
//    }
//
//    public Node forAbsTraitDecl(AbsTraitDecl that) {
//        List<Modifier> mods_result = recurOnListOfModifier(that.getMods());
//        Id name_result = (Id) that.getName().accept(this);
//        List<StaticParam> staticParams_result = recurOnListOfStaticParam(that.getStaticParams());
//        List<TraitTypeWhere> extendsClause_result = recurOnListOfTraitTypeWhere(that.getExtendsClause());
//        WhereClause where_result = (WhereClause) that.getWhere().accept(this);
//        List<TraitType> excludes_result = recurOnListOfTraitType(that.getExcludes());
//        Option<List<TraitType>> comprises_result = recurOnOptionOfListOfTraitType(that.getComprises());
//        List<AbsDecl> decls_result = recurOnListOfAbsDecl(that.getDecls());
//        return forAbsTraitDeclOnly(that, mods_result, name_result, staticParams_result, extendsClause_result, where_result, excludes_result, comprises_result, decls_result);
//    }
//
//    public Node forTraitDecl(TraitDecl that) {
//        List<Modifier> mods_result = recurOnListOfModifier(that.getMods());
//        Id name_result = (Id) that.getName().accept(this);
//        List<StaticParam> staticParams_result = recurOnListOfStaticParam(that.getStaticParams());
//        List<TraitTypeWhere> extendsClause_result = recurOnListOfTraitTypeWhere(that.getExtendsClause());
//        WhereClause where_result = (WhereClause) that.getWhere().accept(this);
//        List<TraitType> excludes_result = recurOnListOfTraitType(that.getExcludes());
//        Option<List<TraitType>> comprises_result = recurOnOptionOfListOfTraitType(that.getComprises());
//        List<Decl> decls_result = recurOnListOfDecl(that.getDecls());
//        return forTraitDeclOnly(that, mods_result, name_result, staticParams_result, extendsClause_result, where_result, excludes_result, comprises_result, decls_result);
//    }
//
//    public Node forAbsObjectDecl(AbsObjectDecl that) {
//        List<Modifier> mods_result = recurOnListOfModifier(that.getMods());
//        Id name_result = (Id) that.getName().accept(this);
//        List<StaticParam> staticParams_result = recurOnListOfStaticParam(that.getStaticParams());
//        List<TraitTypeWhere> extendsClause_result = recurOnListOfTraitTypeWhere(that.getExtendsClause());
//        WhereClause where_result = (WhereClause) that.getWhere().accept(this);
//        Option<List<Param>> params_result = recurOnOptionOfListOfParam(that.getParams());
//        Option<List<TraitType>> throwsClause_result = recurOnOptionOfListOfTraitType(that.getThrowsClause());
//        Contract contract_result = (Contract) that.getContract().accept(this);
//        List<AbsDecl> decls_result = recurOnListOfAbsDecl(that.getDecls());
//        return forAbsObjectDeclOnly(that, mods_result, name_result, staticParams_result, extendsClause_result, where_result, params_result, throwsClause_result, contract_result, decls_result);
//    }
//
//    public Node forObjectDecl(ObjectDecl that) {
//        List<Modifier> mods_result = recurOnListOfModifier(that.getMods());
//        Id name_result = (Id) that.getName().accept(this);
//        List<StaticParam> staticParams_result = recurOnListOfStaticParam(that.getStaticParams());
//        List<TraitTypeWhere> extendsClause_result = recurOnListOfTraitTypeWhere(that.getExtendsClause());
//        WhereClause where_result = (WhereClause) that.getWhere().accept(this);
//        Option<List<Param>> params_result = recurOnOptionOfListOfParam(that.getParams());
//        Option<List<TraitType>> throwsClause_result = recurOnOptionOfListOfTraitType(that.getThrowsClause());
//        Contract contract_result = (Contract) that.getContract().accept(this);
//        List<Decl> decls_result = recurOnListOfDecl(that.getDecls());
//        return forObjectDeclOnly(that, mods_result, name_result, staticParams_result, extendsClause_result, where_result, params_result, throwsClause_result, contract_result, decls_result);
//    }
//
//    public Node forAbsVarDecl(AbsVarDecl that) {
//        List<LValueBind> lhs_result = recurOnListOfLValueBind(that.getLhs());
//        return forAbsVarDeclOnly(that, lhs_result);
//    }
//
//    public Node forVarDecl(VarDecl that) {
//        List<LValueBind> lhs_result = recurOnListOfLValueBind(that.getLhs());
//        Expr init_result = (Expr) that.getInit().accept(this);
//        return forVarDeclOnly(that, lhs_result, init_result);
//    }
//
//    public Node forLValueBind(LValueBind that) {
//        Id name_result = (Id) that.getName().accept(this);
//        Option<Type> type_result = recurOnOptionOfType(that.getType());
//        List<Modifier> mods_result = recurOnListOfModifier(that.getMods());
//        return forLValueBindOnly(that, name_result, type_result, mods_result);
//    }
//
//    public Node forUnpastingBind(UnpastingBind that) {
//        Id name_result = (Id) that.getName().accept(this);
//        List<ExtentRange> dim_result = recurOnListOfExtentRange(that.getDim());
//        return forUnpastingBindOnly(that, name_result, dim_result);
//    }
//
//    public Node forUnpastingSplit(UnpastingSplit that) {
//        List<Unpasting> elems_result = recurOnListOfUnpasting(that.getElems());
//        return forUnpastingSplitOnly(that, elems_result);
//    }
//
//    public Node forAbsFnDecl(AbsFnDecl that) {
//        List<Modifier> mods_result = recurOnListOfModifier(that.getMods());
//        SimpleName name_result = (SimpleName) that.getName().accept(this);
//        List<StaticParam> staticParams_result = recurOnListOfStaticParam(that.getStaticParams());
//        List<Param> params_result = recurOnListOfParam(that.getParams());
//        Option<Type> returnType_result = recurOnOptionOfType(that.getReturnType());
//        Option<List<TraitType>> throwsClause_result = recurOnOptionOfListOfTraitType(that.getThrowsClause());
//        WhereClause where_result = (WhereClause) that.getWhere().accept(this);
//        Contract contract_result = (Contract) that.getContract().accept(this);
//        return forAbsFnDeclOnly(that, mods_result, name_result, staticParams_result, params_result, returnType_result, throwsClause_result, where_result, contract_result);
//    }
//
//    public Node forFnDef(FnDef that) {
//        List<Modifier> mods_result = recurOnListOfModifier(that.getMods());
//        SimpleName name_result = (SimpleName) that.getName().accept(this);
//        List<StaticParam> staticParams_result = recurOnListOfStaticParam(that.getStaticParams());
//        List<Param> params_result = recurOnListOfParam(that.getParams());
//        Option<Type> returnType_result = recurOnOptionOfType(that.getReturnType());
//        Option<List<TraitType>> throwsClause_result = recurOnOptionOfListOfTraitType(that.getThrowsClause());
//        WhereClause where_result = (WhereClause) that.getWhere().accept(this);
//        Contract contract_result = (Contract) that.getContract().accept(this);
//        Expr body_result = (Expr) that.getBody().accept(this);
//        return forFnDefOnly(that, mods_result, name_result, staticParams_result, params_result, returnType_result, throwsClause_result, where_result, contract_result, body_result);
//    }
//
//    public Node forNormalParam(NormalParam that) {
//        List<Modifier> mods_result = recurOnListOfModifier(that.getMods());
//        Id name_result = (Id) that.getName().accept(this);
//        Option<Type> type_result = recurOnOptionOfType(that.getType());
//        Option<Expr> defaultExpr_result = recurOnOptionOfExpr(that.getDefaultExpr());
//        return forNormalParamOnly(that, mods_result, name_result, type_result, defaultExpr_result);
//    }
//
//    public Node forVarargsParam(VarargsParam that) {
//        List<Modifier> mods_result = recurOnListOfModifier(that.getMods());
//        Id name_result = (Id) that.getName().accept(this);
//        VarargsType varargsType_result = (VarargsType) that.getVarargsType().accept(this);
//        return forVarargsParamOnly(that, mods_result, name_result, varargsType_result);
//    }
//
//    public Node forDimDecl(DimDecl that) {
//        Id dim_result = (Id) that.getDim().accept(this);
//        Option<Type> derived_result = recurOnOptionOfType(that.getDerived());
//        Option<Id> default_result = recurOnOptionOfId(that.getDefault());
//        return forDimDeclOnly(that, dim_result, derived_result, default_result);
//    }
//
//    public Node forUnitDecl(UnitDecl that) {
//        List<Id> units_result = recurOnListOfId(that.getUnits());
//        Option<Type> dim_result = recurOnOptionOfType(that.getDim());
//        Option<Expr> def_result = recurOnOptionOfExpr(that.getDef());
//        return forUnitDeclOnly(that, units_result, dim_result, def_result);
//    }
//
//    public Node forTestDecl(TestDecl that) {
//        Id name_result = (Id) that.getName().accept(this);
//        List<GeneratorClause> gens_result = recurOnListOfGeneratorClause(that.getGens());
//        Expr expr_result = (Expr) that.getExpr().accept(this);
//        return forTestDeclOnly(that, name_result, gens_result, expr_result);
//    }
//
//    public Node forPropertyDecl(PropertyDecl that) {
//        Option<Id> name_result = recurOnOptionOfId(that.getName());
//        List<Param> params_result = recurOnListOfParam(that.getParams());
//        Expr expr_result = (Expr) that.getExpr().accept(this);
//        return forPropertyDeclOnly(that, name_result, params_result, expr_result);
//    }
//
//    public Node forAbsExternalSyntax(AbsExternalSyntax that) {
//        SimpleName openExpander_result = (SimpleName) that.getOpenExpander().accept(this);
//        Id name_result = (Id) that.getName().accept(this);
//        SimpleName closeExpander_result = (SimpleName) that.getCloseExpander().accept(this);
//        return forAbsExternalSyntaxOnly(that, openExpander_result, name_result, closeExpander_result);
//    }
//
//    public Node forExternalSyntax(ExternalSyntax that) {
//        SimpleName openExpander_result = (SimpleName) that.getOpenExpander().accept(this);
//        Id name_result = (Id) that.getName().accept(this);
//        SimpleName closeExpander_result = (SimpleName) that.getCloseExpander().accept(this);
//        Expr expr_result = (Expr) that.getExpr().accept(this);
//        return forExternalSyntaxOnly(that, openExpander_result, name_result, closeExpander_result, expr_result);
//    }
//
//    public Node forGrammarDef(GrammarDef that) {
//        QualifiedIdName name_result = (QualifiedIdName) that.getName().accept(this);
//        List<QualifiedIdName> extends_result = recurOnListOfQualifiedIdName(that.getExtends());
//        List<GrammarMemberDecl> members_result = recurOnListOfGrammarMemberDecl(that.getMembers());
//        return forGrammarDefOnly(that, name_result, extends_result, members_result);
//    }
//
//    public Node forNonterminalDef(NonterminalDef that) {
//        QualifiedIdName name_result = (QualifiedIdName) that.getName().accept(this);
//        Option<TraitType> type_result = recurOnOptionOfTraitType(that.getType());
//        Option<? extends Modifier> modifier_result = recurOnOptionOfModifier(that.getModifier());
//        List<SyntaxDef> syntaxDefs_result = recurOnListOfSyntaxDef(that.getSyntaxDefs());
//        return forNonterminalDefOnly(that, name_result, type_result, modifier_result, syntaxDefs_result);
//    }
//
//    public Node forNonterminalExtensionDef(NonterminalExtensionDef that) {
//        QualifiedIdName name_result = (QualifiedIdName) that.getName().accept(this);
//        Option<TraitType> type_result = recurOnOptionOfTraitType(that.getType());
//        Option<? extends Modifier> modifier_result = recurOnOptionOfModifier(that.getModifier());
//        List<SyntaxDef> syntaxDefs_result = recurOnListOfSyntaxDef(that.getSyntaxDefs());
//        return forNonterminalExtensionDefOnly(that, name_result, type_result, modifier_result, syntaxDefs_result);
//    }
//
//    public Node for_TerminalDef(_TerminalDef that) {
//        QualifiedIdName name_result = (QualifiedIdName) that.getName().accept(this);
//        Option<TraitType> type_result = recurOnOptionOfTraitType(that.getType());
//        Option<? extends Modifier> modifier_result = recurOnOptionOfModifier(that.getModifier());
//        SyntaxDef syntaxDef_result = (SyntaxDef) that.getSyntaxDef().accept(this);
//        return for_TerminalDefOnly(that, name_result, type_result, modifier_result, syntaxDef_result);
//    }
//
//    public Node forSyntaxDef(SyntaxDef that) {
//        List<SyntaxSymbol> syntaxSymbols_result = recurOnListOfSyntaxSymbol(that.getSyntaxSymbols());
//        Expr transformationExpression_result = (Expr) that.getTransformationExpression().accept(this);
//        return forSyntaxDefOnly(that, syntaxSymbols_result, transformationExpression_result);
//    }
//
//    public Node forPrefixedSymbol(PrefixedSymbol that) {
//        Option<Id> id_result = recurOnOptionOfId(that.getId());
//        SyntaxSymbol symbol_result = (SyntaxSymbol) that.getSymbol().accept(this);
//        return forPrefixedSymbolOnly(that, id_result, symbol_result);
//    }
//
//    public Node forOptionalSymbol(OptionalSymbol that) {
//        SyntaxSymbol symbol_result = (SyntaxSymbol) that.getSymbol().accept(this);
//        return forOptionalSymbolOnly(that, symbol_result);
//    }
//
//    public Node forRepeatSymbol(RepeatSymbol that) {
//        SyntaxSymbol symbol_result = (SyntaxSymbol) that.getSymbol().accept(this);
//        return forRepeatSymbolOnly(that, symbol_result);
//    }
//
//    public Node forRepeatOneOrMoreSymbol(RepeatOneOrMoreSymbol that) {
//        SyntaxSymbol symbol_result = (SyntaxSymbol) that.getSymbol().accept(this);
//        return forRepeatOneOrMoreSymbolOnly(that, symbol_result);
//    }
//
//    public Node forNoWhitespaceSymbol(NoWhitespaceSymbol that) {
//        SyntaxSymbol symbol_result = (SyntaxSymbol) that.getSymbol().accept(this);
//        return forNoWhitespaceSymbolOnly(that, symbol_result);
//    }
//
//    public Node forWhitespaceSymbol(WhitespaceSymbol that) {
//        return forWhitespaceSymbolOnly(that);
//    }
//
//    public Node forTabSymbol(TabSymbol that) {
//        return forTabSymbolOnly(that);
//    }
//
//    public Node forFormfeedSymbol(FormfeedSymbol that) {
//        return forFormfeedSymbolOnly(that);
//    }
//
//    public Node forCarriageReturnSymbol(CarriageReturnSymbol that) {
//        return forCarriageReturnSymbolOnly(that);
//    }
//
//    public Node forBackspaceSymbol(BackspaceSymbol that) {
//        return forBackspaceSymbolOnly(that);
//    }
//
//    public Node forNewlineSymbol(NewlineSymbol that) {
//        return forNewlineSymbolOnly(that);
//    }
//
//    public Node forBreaklineSymbol(BreaklineSymbol that) {
//        return forBreaklineSymbolOnly(that);
//    }
//
//    public Node forItemSymbol(ItemSymbol that) {
//        return forItemSymbolOnly(that);
//    }
//
//    public Node forNonterminalSymbol(NonterminalSymbol that) {
//        QualifiedName nonterminal_result = (QualifiedName) that.getNonterminal().accept(this);
//        return forNonterminalSymbolOnly(that, nonterminal_result);
//    }
//
//    public Node forKeywordSymbol(KeywordSymbol that) {
//        return forKeywordSymbolOnly(that);
//    }
//
//    public Node forTokenSymbol(TokenSymbol that) {
//        return forTokenSymbolOnly(that);
//    }
//
//    public Node forNotPredicateSymbol(NotPredicateSymbol that) {
//        SyntaxSymbol symbol_result = (SyntaxSymbol) that.getSymbol().accept(this);
//        return forNotPredicateSymbolOnly(that, symbol_result);
//    }
//
//    public Node forAndPredicateSymbol(AndPredicateSymbol that) {
//        SyntaxSymbol symbol_result = (SyntaxSymbol) that.getSymbol().accept(this);
//        return forAndPredicateSymbolOnly(that, symbol_result);
//    }
//
//    public Node forCharacterClassSymbol(CharacterClassSymbol that) {
//        List<CharacterSymbol> characters_result = recurOnListOfCharacterSymbol(that.getCharacters());
//        return forCharacterClassSymbolOnly(that, characters_result);
//    }
//
//    public Node forCharSymbol(CharSymbol that) {
//        return forCharSymbolOnly(that);
//    }
//
//    public Node forCharacterInterval(CharacterInterval that) {
//        return forCharacterIntervalOnly(that);
//    }
//
//    public Node forAsExpr(AsExpr that) {
//        Expr expr_result = (Expr) that.getExpr().accept(this);
//        Type type_result = (Type) that.getType().accept(this);
//        return forAsExprOnly(that, expr_result, type_result);
//    }
//
//    public Node forAsIfExpr(AsIfExpr that) {
//        Expr expr_result = (Expr) that.getExpr().accept(this);
//        Type type_result = (Type) that.getType().accept(this);
//        return forAsIfExprOnly(that, expr_result, type_result);
//    }
//
//    public Node forAssignment(Assignment that) {
//        List<LHS> lhs_result = recurOnListOfLHS(that.getLhs());
//        Option<Op> opr_result = recurOnOptionOfOp(that.getOpr());
//        Expr rhs_result = (Expr) that.getRhs().accept(this);
//        return forAssignmentOnly(that, lhs_result, opr_result, rhs_result);
//    }
//
//    public Node forBlock(Block that) {
//        List<Expr> exprs_result = recurOnListOfExpr(that.getExprs());
//        return forBlockOnly(that, exprs_result);
//    }
//
//    public Node forCaseExpr(CaseExpr that) {
//        Option<Expr> param_result = recurOnOptionOfExpr(that.getParam());
//        Option<Op> compare_result = recurOnOptionOfOp(that.getCompare());
//        List<CaseClause> clauses_result = recurOnListOfCaseClause(that.getClauses());
//        Option<Block> elseClause_result = recurOnOptionOfBlock(that.getElseClause());
//        return forCaseExprOnly(that, param_result, compare_result, clauses_result, elseClause_result);
//    }
//
//    public Node forDo(Do that) {
//        List<DoFront> fronts_result = recurOnListOfDoFront(that.getFronts());
//        return forDoOnly(that, fronts_result);
//    }
//
//    public Node forFor(For that) {
//        List<GeneratorClause> gens_result = recurOnListOfGeneratorClause(that.getGens());
//        DoFront body_result = (DoFront) that.getBody().accept(this);
//        return forForOnly(that, gens_result, body_result);
//    }
//
//    public Node forIf(If that) {
//        List<IfClause> clauses_result = recurOnListOfIfClause(that.getClauses());
//        Option<Block> elseClause_result = recurOnOptionOfBlock(that.getElseClause());
//        return forIfOnly(that, clauses_result, elseClause_result);
//    }
//
//    public Node forLabel(Label that) {
//        Id name_result = (Id) that.getName().accept(this);
//        Block body_result = (Block) that.getBody().accept(this);
//        return forLabelOnly(that, name_result, body_result);
//    }
//
//    public Node forObjectExpr(ObjectExpr that) {
//        List<TraitTypeWhere> extendsClause_result = recurOnListOfTraitTypeWhere(that.getExtendsClause());
//        List<Decl> decls_result = recurOnListOfDecl(that.getDecls());
//        return forObjectExprOnly(that, extendsClause_result, decls_result);
//    }
//
//    public Node for_RewriteObjectExpr(_RewriteObjectExpr that) {
//        List<TraitTypeWhere> extendsClause_result = recurOnListOfTraitTypeWhere(that.getExtendsClause());
//        List<Decl> decls_result = recurOnListOfDecl(that.getDecls());
//        List<StaticParam> staticParams_result = recurOnListOfStaticParam(that.getStaticParams());
//        List<StaticArg> staticArgs_result = recurOnListOfStaticArg(that.getStaticArgs());
//        Option<List<Param>> params_result = recurOnOptionOfListOfParam(that.getParams());
//        return for_RewriteObjectExprOnly(that, extendsClause_result, decls_result, staticParams_result, staticArgs_result, params_result);
//    }
//
//    public Node forTry(Try that) {
//        Block body_result = (Block) that.getBody().accept(this);
//        Option<Catch> catchClause_result = recurOnOptionOfCatch(that.getCatchClause());
//        List<TraitType> forbid_result = recurOnListOfTraitType(that.getForbid());
//        Option<Block> finallyClause_result = recurOnOptionOfBlock(that.getFinallyClause());
//        return forTryOnly(that, body_result, catchClause_result, forbid_result, finallyClause_result);
//    }
//
//    public Node forTupleExpr(TupleExpr that) {
//        List<Expr> exprs_result = recurOnListOfExpr(that.getExprs());
//        return forTupleExprOnly(that, exprs_result);
//    }
//
//    public Node forArgExpr(ArgExpr that) {
//        List<Expr> exprs_result = recurOnListOfExpr(that.getExprs());
//        Option<VarargsExpr> varargs_result = recurOnOptionOfVarargsExpr(that.getVarargs());
//        List<KeywordExpr> keywords_result = recurOnListOfKeywordExpr(that.getKeywords());
//        return forArgExprOnly(that, exprs_result, varargs_result, keywords_result);
//    }
//
//    public Node forTypecase(Typecase that) {
//        List<TypecaseClause> clauses_result = recurOnListOfTypecaseClause(that.getClauses());
//        Option<Block> elseClause_result = recurOnOptionOfBlock(that.getElseClause());
//        return forTypecaseOnly(that, clauses_result, elseClause_result);
//    }
//
//    public Node forWhile(While that) {
//        Expr test_result = (Expr) that.getTest().accept(this);
//        Do body_result = (Do) that.getBody().accept(this);
//        return forWhileOnly(that, test_result, body_result);
//    }
//
//    public Node forAccumulator(Accumulator that) {
//        List<StaticArg> staticArgs_result = recurOnListOfStaticArg(that.getStaticArgs());
//        OpName opr_result = (OpName) that.getOpr().accept(this);
//        List<GeneratorClause> gens_result = recurOnListOfGeneratorClause(that.getGens());
//        Expr body_result = (Expr) that.getBody().accept(this);
//        return forAccumulatorOnly(that, staticArgs_result, opr_result, gens_result, body_result);
//    }
//
//    public Node forArrayComprehension(ArrayComprehension that) {
//        List<StaticArg> staticArgs_result = recurOnListOfStaticArg(that.getStaticArgs());
//        List<ArrayComprehensionClause> clauses_result = recurOnListOfArrayComprehensionClause(that.getClauses());
//        return forArrayComprehensionOnly(that, staticArgs_result, clauses_result);
//    }
//
//    public Node forAtomicExpr(AtomicExpr that) {
//        Expr expr_result = (Expr) that.getExpr().accept(this);
//        return forAtomicExprOnly(that, expr_result);
//    }
//
//    public Node forExit(Exit that) {
//        Option<Id> target_result = recurOnOptionOfId(that.getTarget());
//        Option<Expr> returnExpr_result = recurOnOptionOfExpr(that.getReturnExpr());
//        return forExitOnly(that, target_result, returnExpr_result);
//    }
//
//    public Node forSpawn(Spawn that) {
//        Expr body_result = (Expr) that.getBody().accept(this);
//        return forSpawnOnly(that, body_result);
//    }
//
//    public Node forThrow(Throw that) {
//        Expr expr_result = (Expr) that.getExpr().accept(this);
//        return forThrowOnly(that, expr_result);
//    }
//
//    public Node forTryAtomicExpr(TryAtomicExpr that) {
//        Expr expr_result = (Expr) that.getExpr().accept(this);
//        return forTryAtomicExprOnly(that, expr_result);
//    }
//
//    public Node forFnExpr(FnExpr that) {
//        SimpleName name_result = (SimpleName) that.getName().accept(this);
//        List<StaticParam> staticParams_result = recurOnListOfStaticParam(that.getStaticParams());
//        List<Param> params_result = recurOnListOfParam(that.getParams());
//        Option<Type> returnType_result = recurOnOptionOfType(that.getReturnType());
//        WhereClause where_result = (WhereClause) that.getWhere().accept(this);
//        Option<List<TraitType>> throwsClause_result = recurOnOptionOfListOfTraitType(that.getThrowsClause());
//        Expr body_result = (Expr) that.getBody().accept(this);
//        return forFnExprOnly(that, name_result, staticParams_result, params_result, returnType_result, where_result, throwsClause_result, body_result);
//    }
//
//    public Node forLetFn(LetFn that) {
//        List<Expr> body_result = recurOnListOfExpr(that.getBody());
//        List<FnDef> fns_result = recurOnListOfFnDef(that.getFns());
//        return forLetFnOnly(that, body_result, fns_result);
//    }
//
//    public Node forLocalVarDecl(LocalVarDecl that) {
//        List<Expr> body_result = recurOnListOfExpr(that.getBody());
//        List<LValue> lhs_result = recurOnListOfLValue(that.getLhs());
//        Option<Expr> rhs_result = recurOnOptionOfExpr(that.getRhs());
//        return forLocalVarDeclOnly(that, body_result, lhs_result, rhs_result);
//    }
//
//    public Node forGeneratedExpr(GeneratedExpr that) {
//        Expr expr_result = (Expr) that.getExpr().accept(this);
//        List<GeneratorClause> gens_result = recurOnListOfGeneratorClause(that.getGens());
//        return forGeneratedExprOnly(that, expr_result, gens_result);
//    }
//
//    public Node forSubscriptExpr(SubscriptExpr that) {
//        Expr obj_result = (Expr) that.getObj().accept(this);
//        List<Expr> subs_result = recurOnListOfExpr(that.getSubs());
//        Option<Enclosing> op_result = recurOnOptionOfEnclosing(that.getOp());
//        return forSubscriptExprOnly(that, obj_result, subs_result, op_result);
//    }
//
//    public Node forFloatLiteralExpr(FloatLiteralExpr that) {
//        return forFloatLiteralExprOnly(that);
//    }
//
//    public Node forIntLiteralExpr(IntLiteralExpr that) {
//        return forIntLiteralExprOnly(that);
//    }
//
//    public Node forCharLiteralExpr(CharLiteralExpr that) {
//        return forCharLiteralExprOnly(that);
//    }
//
//    public Node forStringLiteralExpr(StringLiteralExpr that) {
//        return forStringLiteralExprOnly(that);
//    }
//
//    public Node forVoidLiteralExpr(VoidLiteralExpr that) {
//        return forVoidLiteralExprOnly(that);
//    }
//
//    public Node forVarRef(VarRef that) {
//        QualifiedIdName var_result = (QualifiedIdName) that.getVar().accept(this);
//        return forVarRefOnly(that, var_result);
//    }
//
//    public Node forFieldRef(FieldRef that) {
//        Expr obj_result = (Expr) that.getObj().accept(this);
//        Id field_result = (Id) that.getField().accept(this);
//        return forFieldRefOnly(that, obj_result, field_result);
//    }
//
//    public Node forFieldRefForSure(FieldRefForSure that) {
//        Expr obj_result = (Expr) that.getObj().accept(this);
//        Id field_result = (Id) that.getField().accept(this);
//        return forFieldRefForSureOnly(that, obj_result, field_result);
//    }
//
//    public Node for_RewriteFieldRef(_RewriteFieldRef that) {
//        Expr obj_result = (Expr) that.getObj().accept(this);
//        Name field_result = (Name) that.getField().accept(this);
//        return for_RewriteFieldRefOnly(that, obj_result, field_result);
//    }
//
//    public Node forFnRef(FnRef that) {
//        List<QualifiedIdName> fns_result = recurOnListOfQualifiedIdName(that.getFns());
//        List<StaticArg> staticArgs_result = recurOnListOfStaticArg(that.getStaticArgs());
//        return forFnRefOnly(that, fns_result, staticArgs_result);
//    }
//
//    public Node for_RewriteFnRef(_RewriteFnRef that) {
//        Expr fn_result = (Expr) that.getFn().accept(this);
//        List<StaticArg> staticArgs_result = recurOnListOfStaticArg(that.getStaticArgs());
//        return for_RewriteFnRefOnly(that, fn_result, staticArgs_result);
//    }
//
//    public Node forOpRef(OpRef that) {
//        List<QualifiedOpName> ops_result = recurOnListOfQualifiedOpName(that.getOps());
//        List<StaticArg> staticArgs_result = recurOnListOfStaticArg(that.getStaticArgs());
//        return forOpRefOnly(that, ops_result, staticArgs_result);
//    }
//
//    public Node forLooseJuxt(LooseJuxt that) {
//        List<Expr> exprs_result = recurOnListOfExpr(that.getExprs());
//        return forLooseJuxtOnly(that, exprs_result);
//    }
//
//    public Node forTightJuxt(TightJuxt that) {
//        List<Expr> exprs_result = recurOnListOfExpr(that.getExprs());
//        return forTightJuxtOnly(that, exprs_result);
//    }
//
//    public Node forOprExpr(OprExpr that) {
//        OpRef op_result = (OpRef) that.getOp().accept(this);
//        List<Expr> args_result = recurOnListOfExpr(that.getArgs());
//        return forOprExprOnly(that, op_result, args_result);
//    }
//
//    public Node forChainExpr(ChainExpr that) {
//        Expr first_result = (Expr) that.getFirst().accept(this);
//        return forChainExprOnly(that, first_result);
//    }
//
//    public Node forCoercionInvocation(CoercionInvocation that) {
//        TraitType type_result = (TraitType) that.getType().accept(this);
//        List<StaticArg> staticArgs_result = recurOnListOfStaticArg(that.getStaticArgs());
//        Expr arg_result = (Expr) that.getArg().accept(this);
//        return forCoercionInvocationOnly(that, type_result, staticArgs_result, arg_result);
//    }
//
//    public Node forMethodInvocation(MethodInvocation that) {
//        Expr obj_result = (Expr) that.getObj().accept(this);
//        Id method_result = (Id) that.getMethod().accept(this);
//        List<StaticArg> staticArgs_result = recurOnListOfStaticArg(that.getStaticArgs());
//        Expr arg_result = (Expr) that.getArg().accept(this);
//        return forMethodInvocationOnly(that, obj_result, method_result, staticArgs_result, arg_result);
//    }
//
//    public Node forMathPrimary(MathPrimary that) {
//        Expr front_result = (Expr) that.getFront().accept(this);
//        List<MathItem> rest_result = recurOnListOfMathItem(that.getRest());
//        return forMathPrimaryOnly(that, front_result, rest_result);
//    }
//
//    public Node forArrayElement(ArrayElement that) {
//        Expr element_result = (Expr) that.getElement().accept(this);
//        return forArrayElementOnly(that, element_result);
//    }
//
//    public Node forArrayElements(ArrayElements that) {
//        List<ArrayExpr> elements_result = recurOnListOfArrayExpr(that.getElements());
//        return forArrayElementsOnly(that, elements_result);
//    }
//
//    public Node forExponentType(ExponentType that) {
//        Type base_result = (Type) that.getBase().accept(this);
//        IntExpr power_result = (IntExpr) that.getPower().accept(this);
//        return forExponentTypeOnly(that, base_result, power_result);
//    }
//
//    public Node forBaseDim(BaseDim that) {
//        return forBaseDimOnly(that);
//    }
//
//    public Node forDimRef(DimRef that) {
//        QualifiedIdName name_result = (QualifiedIdName) that.getName().accept(this);
//        return forDimRefOnly(that, name_result);
//    }
//
//    public Node forProductDim(ProductDim that) {
//        DimExpr multiplier_result = (DimExpr) that.getMultiplier().accept(this);
//        DimExpr multiplicand_result = (DimExpr) that.getMultiplicand().accept(this);
//        return forProductDimOnly(that, multiplier_result, multiplicand_result);
//    }
//
//    public Node forQuotientDim(QuotientDim that) {
//        DimExpr numerator_result = (DimExpr) that.getNumerator().accept(this);
//        DimExpr denominator_result = (DimExpr) that.getDenominator().accept(this);
//        return forQuotientDimOnly(that, numerator_result, denominator_result);
//    }
//
//    public Node forExponentDim(ExponentDim that) {
//        DimExpr base_result = (DimExpr) that.getBase().accept(this);
//        IntExpr power_result = (IntExpr) that.getPower().accept(this);
//        return forExponentDimOnly(that, base_result, power_result);
//    }
//
//    public Node forOpDim(OpDim that) {
//        DimExpr val_result = (DimExpr) that.getVal().accept(this);
//        Op op_result = (Op) that.getOp().accept(this);
//        return forOpDimOnly(that, val_result, op_result);
//    }
//
//    public Node forArrowType(ArrowType that) {
//        Type domain_result = (Type) that.getDomain().accept(this);
//        Type range_result = (Type) that.getRange().accept(this);
//        Option<List<Type>> throwsClause_result = recurOnOptionOfListOfType(that.getThrowsClause());
//        return forArrowTypeOnly(that, domain_result, range_result, throwsClause_result);
//    }
//
//    public Node for_RewriteGenericArrowType(_RewriteGenericArrowType that) {
//        Type domain_result = (Type) that.getDomain().accept(this);
//        Type range_result = (Type) that.getRange().accept(this);
//        Option<List<Type>> throwsClause_result = recurOnOptionOfListOfType(that.getThrowsClause());
//        List<StaticParam> staticParams_result = recurOnListOfStaticParam(that.getStaticParams());
//        WhereClause where_result = (WhereClause) that.getWhere().accept(this);
//        return for_RewriteGenericArrowTypeOnly(that, domain_result, range_result, throwsClause_result, staticParams_result, where_result);
//    }
//
//    public Node forBottomType(BottomType that) {
//        return forBottomTypeOnly(that);
//    }
//
//    public Node forArrayType(ArrayType that) {
//        Type element_result = (Type) that.getElement().accept(this);
//        Indices indices_result = (Indices) that.getIndices().accept(this);
//        return forArrayTypeOnly(that, element_result, indices_result);
//    }
//
//    public Node forIdType(IdType that) {
//        QualifiedIdName name_result = (QualifiedIdName) that.getName().accept(this);
//        return forIdTypeOnly(that, name_result);
//    }
//
//    public Node forInferenceVarType(InferenceVarType that) {
//        return forInferenceVarTypeOnly(that);
//    }
//
//    public Node forMatrixType(MatrixType that) {
//        Type element_result = (Type) that.getElement().accept(this);
//        List<ExtentRange> dimensions_result = recurOnListOfExtentRange(that.getDimensions());
//        return forMatrixTypeOnly(that, element_result, dimensions_result);
//    }
//
//    public Node forInstantiatedType(InstantiatedType that) {
//        QualifiedIdName name_result = (QualifiedIdName) that.getName().accept(this);
//        List<StaticArg> args_result = recurOnListOfStaticArg(that.getArgs());
//        return forInstantiatedTypeOnly(that, name_result, args_result);
//    }
//
//    public Node forTupleType(TupleType that) {
//        List<Type> elements_result = recurOnListOfType(that.getElements());
//        return forTupleTypeOnly(that, elements_result);
//    }
//
//    public Node forArgType(ArgType that) {
//        List<Type> elements_result = recurOnListOfType(that.getElements());
//        Option<VarargsType> varargs_result = recurOnOptionOfVarargsType(that.getVarargs());
//        List<KeywordType> keywords_result = recurOnListOfKeywordType(that.getKeywords());
//        return forArgTypeOnly(that, elements_result, varargs_result, keywords_result);
//    }
//
//    public Node forVoidType(VoidType that) {
//        return forVoidTypeOnly(that);
//    }
//
//    public Node forIntersectionType(IntersectionType that) {
//        Set<Type> elements_result = recurOnSetOfType(that.getElements());
//        return forIntersectionTypeOnly(that, elements_result);
//    }
//
//    public Node forUnionType(UnionType that) {
//        Set<Type> elements_result = recurOnSetOfType(that.getElements());
//        return forUnionTypeOnly(that, elements_result);
//    }
//
//    public Node forAndType(AndType that) {
//        Type first_result = (Type) that.getFirst().accept(this);
//        Type second_result = (Type) that.getSecond().accept(this);
//        return forAndTypeOnly(that, first_result, second_result);
//    }
//
//    public Node forOrType(OrType that) {
//        Type first_result = (Type) that.getFirst().accept(this);
//        Type second_result = (Type) that.getSecond().accept(this);
//        return forOrTypeOnly(that, first_result, second_result);
//    }
//
//    public Node forTaggedDimType(TaggedDimType that) {
//        Type type_result = (Type) that.getType().accept(this);
//        DimExpr dim_result = (DimExpr) that.getDim().accept(this);
//        Option<Expr> unit_result = recurOnOptionOfExpr(that.getUnit());
//        return forTaggedDimTypeOnly(that, type_result, dim_result, unit_result);
//    }
//
//    public Node forTaggedUnitType(TaggedUnitType that) {
//        Type type_result = (Type) that.getType().accept(this);
//        Expr unit_result = (Expr) that.getUnit().accept(this);
//        return forTaggedUnitTypeOnly(that, type_result, unit_result);
//    }
//
//    public Node forIdArg(IdArg that) {
//        QualifiedIdName name_result = (QualifiedIdName) that.getName().accept(this);
//        return forIdArgOnly(that, name_result);
//    }
//
//    public Node forTypeArg(TypeArg that) {
//        Type type_result = (Type) that.getType().accept(this);
//        return forTypeArgOnly(that, type_result);
//    }
//
//    public Node forIntArg(IntArg that) {
//        IntExpr val_result = (IntExpr) that.getVal().accept(this);
//        return forIntArgOnly(that, val_result);
//    }
//
//    public Node forBoolArg(BoolArg that) {
//        BoolExpr bool_result = (BoolExpr) that.getBool().accept(this);
//        return forBoolArgOnly(that, bool_result);
//    }
//
//    public Node forOprArg(OprArg that) {
//        Op name_result = (Op) that.getName().accept(this);
//        return forOprArgOnly(that, name_result);
//    }
//
//    public Node forDimArg(DimArg that) {
//        DimExpr dim_result = (DimExpr) that.getDim().accept(this);
//        return forDimArgOnly(that, dim_result);
//    }
//
//    public Node forUnitArg(UnitArg that) {
//        Expr unit_result = (Expr) that.getUnit().accept(this);
//        return forUnitArgOnly(that, unit_result);
//    }
//
//    public Node for_RewriteImplicitType(_RewriteImplicitType that) {
//        return for_RewriteImplicitTypeOnly(that);
//    }
//
//    public Node for_RewriteIntersectionType(_RewriteIntersectionType that) {
//        List<Type> elements_result = recurOnListOfType(that.getElements());
//        return for_RewriteIntersectionTypeOnly(that, elements_result);
//    }
//
//    public Node for_RewriteUnionType(_RewriteUnionType that) {
//        List<Type> elements_result = recurOnListOfType(that.getElements());
//        return for_RewriteUnionTypeOnly(that, elements_result);
//    }
//
//    public Node for_RewriteFixedPointType(_RewriteFixedPointType that) {
//        _RewriteImplicitType var_result = (_RewriteImplicitType) that.getVar().accept(this);
//        Type body_result = (Type) that.getBody().accept(this);
//        return for_RewriteFixedPointTypeOnly(that, var_result, body_result);
//    }
//
//    public Node forNumberConstraint(NumberConstraint that) {
//        IntLiteralExpr val_result = (IntLiteralExpr) that.getVal().accept(this);
//        return forNumberConstraintOnly(that, val_result);
//    }
//
//    public Node forIntRef(IntRef that) {
//        QualifiedIdName name_result = (QualifiedIdName) that.getName().accept(this);
//        return forIntRefOnly(that, name_result);
//    }
//
//    public Node forSumConstraint(SumConstraint that) {
//        IntExpr left_result = (IntExpr) that.getLeft().accept(this);
//        IntExpr right_result = (IntExpr) that.getRight().accept(this);
//        return forSumConstraintOnly(that, left_result, right_result);
//    }
//
//    public Node forMinusConstraint(MinusConstraint that) {
//        IntExpr left_result = (IntExpr) that.getLeft().accept(this);
//        IntExpr right_result = (IntExpr) that.getRight().accept(this);
//        return forMinusConstraintOnly(that, left_result, right_result);
//    }
//
//    public Node forProductConstraint(ProductConstraint that) {
//        IntExpr left_result = (IntExpr) that.getLeft().accept(this);
//        IntExpr right_result = (IntExpr) that.getRight().accept(this);
//        return forProductConstraintOnly(that, left_result, right_result);
//    }
//
//    public Node forExponentConstraint(ExponentConstraint that) {
//        IntExpr left_result = (IntExpr) that.getLeft().accept(this);
//        IntExpr right_result = (IntExpr) that.getRight().accept(this);
//        return forExponentConstraintOnly(that, left_result, right_result);
//    }
//
//    public Node forBoolConstant(BoolConstant that) {
//        return forBoolConstantOnly(that);
//    }
//
//    public Node forBoolRef(BoolRef that) {
//        QualifiedIdName name_result = (QualifiedIdName) that.getName().accept(this);
//        return forBoolRefOnly(that, name_result);
//    }
//
//    public Node forNotConstraint(NotConstraint that) {
//        BoolExpr bool_result = (BoolExpr) that.getBool().accept(this);
//        return forNotConstraintOnly(that, bool_result);
//    }
//
//    public Node forOrConstraint(OrConstraint that) {
//        BoolExpr left_result = (BoolExpr) that.getLeft().accept(this);
//        BoolExpr right_result = (BoolExpr) that.getRight().accept(this);
//        return forOrConstraintOnly(that, left_result, right_result);
//    }
//
//    public Node forAndConstraint(AndConstraint that) {
//        BoolExpr left_result = (BoolExpr) that.getLeft().accept(this);
//        BoolExpr right_result = (BoolExpr) that.getRight().accept(this);
//        return forAndConstraintOnly(that, left_result, right_result);
//    }
//
//    public Node forImpliesConstraint(ImpliesConstraint that) {
//        BoolExpr left_result = (BoolExpr) that.getLeft().accept(this);
//        BoolExpr right_result = (BoolExpr) that.getRight().accept(this);
//        return forImpliesConstraintOnly(that, left_result, right_result);
//    }
//
//    public Node forBEConstraint(BEConstraint that) {
//        BoolExpr left_result = (BoolExpr) that.getLeft().accept(this);
//        BoolExpr right_result = (BoolExpr) that.getRight().accept(this);
//        return forBEConstraintOnly(that, left_result, right_result);
//    }
//
//    public Node forWhereClause(WhereClause that) {
//        List<WhereBinding> bindings_result = recurOnListOfWhereBinding(that.getBindings());
//        List<WhereConstraint> constraints_result = recurOnListOfWhereConstraint(that.getConstraints());
//        return forWhereClauseOnly(that, bindings_result, constraints_result);
//    }
//
//    public Node forWhereType(WhereType that) {
//        Id name_result = (Id) that.getName().accept(this);
//        List<TraitType> supers_result = recurOnListOfTraitType(that.getSupers());
//        return forWhereTypeOnly(that, name_result, supers_result);
//    }
//
//    public Node forWhereNat(WhereNat that) {
//        Id name_result = (Id) that.getName().accept(this);
//        return forWhereNatOnly(that, name_result);
//    }
//
//    public Node forWhereInt(WhereInt that) {
//        Id name_result = (Id) that.getName().accept(this);
//        return forWhereIntOnly(that, name_result);
//    }
//
//    public Node forWhereBool(WhereBool that) {
//        Id name_result = (Id) that.getName().accept(this);
//        return forWhereBoolOnly(that, name_result);
//    }
//
//    public Node forWhereUnit(WhereUnit that) {
//        Id name_result = (Id) that.getName().accept(this);
//        return forWhereUnitOnly(that, name_result);
//    }
//
//    public Node forWhereExtends(WhereExtends that) {
//        Id name_result = (Id) that.getName().accept(this);
//        List<TraitType> supers_result = recurOnListOfTraitType(that.getSupers());
//        return forWhereExtendsOnly(that, name_result, supers_result);
//    }
//
//    public Node forTypeAlias(TypeAlias that) {
//        Id name_result = (Id) that.getName().accept(this);
//        List<StaticParam> staticParams_result = recurOnListOfStaticParam(that.getStaticParams());
//        Type type_result = (Type) that.getType().accept(this);
//        return forTypeAliasOnly(that, name_result, staticParams_result, type_result);
//    }
//
//    public Node forWhereCoerces(WhereCoerces that) {
//        Type left_result = (Type) that.getLeft().accept(this);
//        Type right_result = (Type) that.getRight().accept(this);
//        return forWhereCoercesOnly(that, left_result, right_result);
//    }
//
//    public Node forWhereWidens(WhereWidens that) {
//        Type left_result = (Type) that.getLeft().accept(this);
//        Type right_result = (Type) that.getRight().accept(this);
//        return forWhereWidensOnly(that, left_result, right_result);
//    }
//
//    public Node forWhereWidensCoerces(WhereWidensCoerces that) {
//        Type left_result = (Type) that.getLeft().accept(this);
//        Type right_result = (Type) that.getRight().accept(this);
//        return forWhereWidensCoercesOnly(that, left_result, right_result);
//    }
//
//    public Node forWhereEquals(WhereEquals that) {
//        QualifiedIdName left_result = (QualifiedIdName) that.getLeft().accept(this);
//        QualifiedIdName right_result = (QualifiedIdName) that.getRight().accept(this);
//        return forWhereEqualsOnly(that, left_result, right_result);
//    }
//
//    public Node forUnitConstraint(UnitConstraint that) {
//        Id name_result = (Id) that.getName().accept(this);
//        return forUnitConstraintOnly(that, name_result);
//    }
//
//    public Node forLEConstraint(LEConstraint that) {
//        IntExpr left_result = (IntExpr) that.getLeft().accept(this);
//        IntExpr right_result = (IntExpr) that.getRight().accept(this);
//        return forLEConstraintOnly(that, left_result, right_result);
//    }
//
//    public Node forLTConstraint(LTConstraint that) {
//        IntExpr left_result = (IntExpr) that.getLeft().accept(this);
//        IntExpr right_result = (IntExpr) that.getRight().accept(this);
//        return forLTConstraintOnly(that, left_result, right_result);
//    }
//
//    public Node forGEConstraint(GEConstraint that) {
//        IntExpr left_result = (IntExpr) that.getLeft().accept(this);
//        IntExpr right_result = (IntExpr) that.getRight().accept(this);
//        return forGEConstraintOnly(that, left_result, right_result);
//    }
//
//    public Node forGTConstraint(GTConstraint that) {
//        IntExpr left_result = (IntExpr) that.getLeft().accept(this);
//        IntExpr right_result = (IntExpr) that.getRight().accept(this);
//        return forGTConstraintOnly(that, left_result, right_result);
//    }
//
//    public Node forIEConstraint(IEConstraint that) {
//        IntExpr left_result = (IntExpr) that.getLeft().accept(this);
//        IntExpr right_result = (IntExpr) that.getRight().accept(this);
//        return forIEConstraintOnly(that, left_result, right_result);
//    }
//
//    public Node forBoolConstraintExpr(BoolConstraintExpr that) {
//        BoolConstraint constraint_result = (BoolConstraint) that.getConstraint().accept(this);
//        return forBoolConstraintExprOnly(that, constraint_result);
//    }
//
//    public Node forContract(Contract that) {
//        Option<List<Expr>> requires_result = recurOnOptionOfListOfExpr(that.getRequires());
//        Option<List<EnsuresClause>> ensures_result = recurOnOptionOfListOfEnsuresClause(that.getEnsures());
//        Option<List<Expr>> invariants_result = recurOnOptionOfListOfExpr(that.getInvariants());
//        return forContractOnly(that, requires_result, ensures_result, invariants_result);
//    }
//
//    public Node forEnsuresClause(EnsuresClause that) {
//        Expr post_result = (Expr) that.getPost().accept(this);
//        Option<Expr> pre_result = recurOnOptionOfExpr(that.getPre());
//        return forEnsuresClauseOnly(that, post_result, pre_result);
//    }
//
//    public Node forModifierAbstract(ModifierAbstract that) {
//        return forModifierAbstractOnly(that);
//    }
//
//    public Node forModifierAtomic(ModifierAtomic that) {
//        return forModifierAtomicOnly(that);
//    }
//
//    public Node forModifierGetter(ModifierGetter that) {
//        return forModifierGetterOnly(that);
//    }
//
//    public Node forModifierHidden(ModifierHidden that) {
//        return forModifierHiddenOnly(that);
//    }
//
//    public Node forModifierIO(ModifierIO that) {
//        return forModifierIOOnly(that);
//    }
//
//    public Node forModifierOverride(ModifierOverride that) {
//        return forModifierOverrideOnly(that);
//    }
//
//    public Node forModifierPrivate(ModifierPrivate that) {
//        return forModifierPrivateOnly(that);
//    }
//
//    public Node forModifierSettable(ModifierSettable that) {
//        return forModifierSettableOnly(that);
//    }
//
//    public Node forModifierSetter(ModifierSetter that) {
//        return forModifierSetterOnly(that);
//    }
//
//    public Node forModifierTest(ModifierTest that) {
//        return forModifierTestOnly(that);
//    }
//
//    public Node forModifierTransient(ModifierTransient that) {
//        return forModifierTransientOnly(that);
//    }
//
//    public Node forModifierValue(ModifierValue that) {
//        return forModifierValueOnly(that);
//    }
//
//    public Node forModifierVar(ModifierVar that) {
//        return forModifierVarOnly(that);
//    }
//
//    public Node forModifierWidens(ModifierWidens that) {
//        return forModifierWidensOnly(that);
//    }
//
//    public Node forModifierWrapped(ModifierWrapped that) {
//        return forModifierWrappedOnly(that);
//    }
//
//    public Node forOperatorParam(OperatorParam that) {
//        Op name_result = (Op) that.getName().accept(this);
//        return forOperatorParamOnly(that, name_result);
//    }
//
//    public Node forBoolParam(BoolParam that) {
//        Id name_result = (Id) that.getName().accept(this);
//        return forBoolParamOnly(that, name_result);
//    }
//
//    public Node forDimensionParam(DimensionParam that) {
//        Id name_result = (Id) that.getName().accept(this);
//        return forDimensionParamOnly(that, name_result);
//    }
//
//    public Node forIntParam(IntParam that) {
//        Id name_result = (Id) that.getName().accept(this);
//        return forIntParamOnly(that, name_result);
//    }
//
//    public Node forNatParam(NatParam that) {
//        Id name_result = (Id) that.getName().accept(this);
//        return forNatParamOnly(that, name_result);
//    }
//
//    public Node forSimpleTypeParam(SimpleTypeParam that) {
//        Id name_result = (Id) that.getName().accept(this);
//        List<TraitType> extendsClause_result = recurOnListOfTraitType(that.getExtendsClause());
//        return forSimpleTypeParamOnly(that, name_result, extendsClause_result);
//    }
//
//    public Node forUnitParam(UnitParam that) {
//        Id name_result = (Id) that.getName().accept(this);
//        Option<Type> dim_result = recurOnOptionOfType(that.getDim());
//        return forUnitParamOnly(that, name_result, dim_result);
//    }
//
//    public Node forAPIName(APIName that) {
//        List<Id> ids_result = recurOnListOfId(that.getIds());
//        return forAPINameOnly(that, ids_result);
//    }
//
//    public Node forQualifiedIdName(QualifiedIdName that) {
//        Option<APIName> api_result = recurOnOptionOfAPIName(that.getApi());
//        Id name_result = (Id) that.getName().accept(this);
//        return forQualifiedIdNameOnly(that, api_result, name_result);
//    }
//
//    public Node forQualifiedOpName(QualifiedOpName that) {
//        Option<APIName> api_result = recurOnOptionOfAPIName(that.getApi());
//        OpName name_result = (OpName) that.getName().accept(this);
//        return forQualifiedOpNameOnly(that, api_result, name_result);
//    }
//
//    public Node forId(Id that) {
//        return forIdOnly(that);
//    }
//
//    public Node forOp(Op that) {
//        Option<Fixity> fixity_result = recurOnOptionOfFixity(that.getFixity());
//        return forOpOnly(that, fixity_result);
//    }
//
//    public Node forEnclosing(Enclosing that) {
//        Op open_result = (Op) that.getOpen().accept(this);
//        Op close_result = (Op) that.getClose().accept(this);
//        return forEnclosingOnly(that, open_result, close_result);
//    }
//
//    public Node forAnonymousFnName(AnonymousFnName that) {
//        return forAnonymousFnNameOnly(that);
//    }
//
//    public Node forConstructorFnName(ConstructorFnName that) {
//        GenericWithParams def_result = (GenericWithParams) that.getDef().accept(this);
//        return forConstructorFnNameOnly(that, def_result);
//    }
//
//    public Node forArrayComprehensionClause(ArrayComprehensionClause that) {
//        List<Expr> bind_result = recurOnListOfExpr(that.getBind());
//        Expr init_result = (Expr) that.getInit().accept(this);
//        List<GeneratorClause> gens_result = recurOnListOfGeneratorClause(that.getGens());
//        return forArrayComprehensionClauseOnly(that, bind_result, init_result, gens_result);
//    }
//
//    public Node forKeywordExpr(KeywordExpr that) {
//        Id name_result = (Id) that.getName().accept(this);
//        Expr init_result = (Expr) that.getInit().accept(this);
//        return forKeywordExprOnly(that, name_result, init_result);
//    }
//
//    public Node forCaseClause(CaseClause that) {
//        Expr match_result = (Expr) that.getMatch().accept(this);
//        Block body_result = (Block) that.getBody().accept(this);
//        return forCaseClauseOnly(that, match_result, body_result);
//    }
//
//    public Node forCatch(Catch that) {
//        Id name_result = (Id) that.getName().accept(this);
//        List<CatchClause> clauses_result = recurOnListOfCatchClause(that.getClauses());
//        return forCatchOnly(that, name_result, clauses_result);
//    }
//
//    public Node forCatchClause(CatchClause that) {
//        TraitType match_result = (TraitType) that.getMatch().accept(this);
//        Block body_result = (Block) that.getBody().accept(this);
//        return forCatchClauseOnly(that, match_result, body_result);
//    }
//
//    public Node forDoFront(DoFront that) {
//        Option<Expr> loc_result = recurOnOptionOfExpr(that.getLoc());
//        Block expr_result = (Block) that.getExpr().accept(this);
//        return forDoFrontOnly(that, loc_result, expr_result);
//    }
//
//    public Node forIfClause(IfClause that) {
//        Expr test_result = (Expr) that.getTest().accept(this);
//        Block body_result = (Block) that.getBody().accept(this);
//        return forIfClauseOnly(that, test_result, body_result);
//    }
//
//    public Node forTypecaseClause(TypecaseClause that) {
//        List<Type> match_result = recurOnListOfType(that.getMatch());
//        Block body_result = (Block) that.getBody().accept(this);
//        return forTypecaseClauseOnly(that, match_result, body_result);
//    }
//
//    public Node forExtentRange(ExtentRange that) {
//        Option<StaticArg> base_result = recurOnOptionOfStaticArg(that.getBase());
//        Option<StaticArg> size_result = recurOnOptionOfStaticArg(that.getSize());
//        return forExtentRangeOnly(that, base_result, size_result);
//    }
//
//    public Node forGeneratorClause(GeneratorClause that) {
//        List<Id> bind_result = recurOnListOfId(that.getBind());
//        Expr init_result = (Expr) that.getInit().accept(this);
//        return forGeneratorClauseOnly(that, bind_result, init_result);
//    }
//
//    public Node forVarargsExpr(VarargsExpr that) {
//        Expr varargs_result = (Expr) that.getVarargs().accept(this);
//        return forVarargsExprOnly(that, varargs_result);
//    }
//
//    public Node forVarargsType(VarargsType that) {
//        Type type_result = (Type) that.getType().accept(this);
//        return forVarargsTypeOnly(that, type_result);
//    }
//
//    public Node forKeywordType(KeywordType that) {
//        Id name_result = (Id) that.getName().accept(this);
//        Type type_result = (Type) that.getType().accept(this);
//        return forKeywordTypeOnly(that, name_result, type_result);
//    }
//
//    public Node forTraitTypeWhere(TraitTypeWhere that) {
//        TraitType type_result = (TraitType) that.getType().accept(this);
//        WhereClause where_result = (WhereClause) that.getWhere().accept(this);
//        return forTraitTypeWhereOnly(that, type_result, where_result);
//    }
//
//    public Node forIndices(Indices that) {
//        List<ExtentRange> extents_result = recurOnListOfExtentRange(that.getExtents());
//        return forIndicesOnly(that, extents_result);
//    }
//
//    public Node forParenthesisDelimitedMI(ParenthesisDelimitedMI that) {
//        Expr expr_result = (Expr) that.getExpr().accept(this);
//        return forParenthesisDelimitedMIOnly(that, expr_result);
//    }
//
//    public Node forNonParenthesisDelimitedMI(NonParenthesisDelimitedMI that) {
//        Expr expr_result = (Expr) that.getExpr().accept(this);
//        return forNonParenthesisDelimitedMIOnly(that, expr_result);
//    }
//
//    public Node forExponentiationMI(ExponentiationMI that) {
//        Op op_result = (Op) that.getOp().accept(this);
//        Option<Expr> expr_result = recurOnOptionOfExpr(that.getExpr());
//        return forExponentiationMIOnly(that, op_result, expr_result);
//    }
//
//    public Node forSubscriptingMI(SubscriptingMI that) {
//        Enclosing op_result = (Enclosing) that.getOp().accept(this);
//        List<Expr> exprs_result = recurOnListOfExpr(that.getExprs());
//        return forSubscriptingMIOnly(that, op_result, exprs_result);
//    }
//
//    public Node forInFixity(InFixity that) {
//        return forInFixityOnly(that);
//    }
//
//    public Node forPreFixity(PreFixity that) {
//        return forPreFixityOnly(that);
//    }
//
//    public Node forPostFixity(PostFixity that) {
//        return forPostFixityOnly(that);
//    }
//
//    public Node forNoFixity(NoFixity that) {
//        return forNoFixityOnly(that);
//    }
//
//    public Node forMultiFixity(MultiFixity that) {
//        return forMultiFixityOnly(that);
//    }
//
//    public Node forEnclosingFixity(EnclosingFixity that) {
//        return forEnclosingFixityOnly(that);
//    }
//
//    public Node forBigFixity(BigFixity that) {
//        return forBigFixityOnly(that);
//    }
//
//
//    public List<Import> recurOnListOfImport(final List<Import> that) {
//        boolean changed = false;
//        List<Import> result = new java.util.ArrayList<Import>(0);
//        for (Import elt : that) {
//            Import elt_result = (Import) elt.accept(this);
//            result.add(elt_result);
//            if (elt != elt_result) changed = true;
//        }
//        return changed ? (result) : that;
//    }
//
//    public List<Export> recurOnListOfExport(final List<Export> that) {
//        boolean changed = false;
//        List<Export> result = new java.util.ArrayList<Export>(0);
//        for (Export elt : that) {
//            Export elt_result = (Export) elt.accept(this);
//            result.add(elt_result);
//            if (elt != elt_result) changed = true;
//        }
//        return changed ? (result) : that;
//    }
//
//    public List<Decl> recurOnListOfDecl(final List<Decl> that) {
//        boolean changed = false;
//        List<Decl> result = new java.util.ArrayList<Decl>(0);
//        for (Decl elt : that) {
//            Decl elt_result = (Decl) elt.accept(this);
//            result.add(elt_result);
//            if (elt != elt_result) changed = true;
//        }
//        return changed ? (result) : that;
//    }
//
//    public List<AbsDecl> recurOnListOfAbsDecl(final List<AbsDecl> that) {
//        boolean changed = false;
//        List<AbsDecl> result = new java.util.ArrayList<AbsDecl>(0);
//        for (AbsDecl elt : that) {
//            AbsDecl elt_result = (AbsDecl) elt.accept(this);
//            result.add(elt_result);
//            if (elt != elt_result) changed = true;
//        }
//        return changed ? (result) : that;
//    }
//
//    public List<SimpleName> recurOnListOfSimpleName(final List<SimpleName> that) {
//        boolean changed = false;
//        List<SimpleName> result = new java.util.ArrayList<SimpleName>(0);
//        for (SimpleName elt : that) {
//            SimpleName elt_result = (SimpleName) elt.accept(this);
//            result.add(elt_result);
//            if (elt != elt_result) changed = true;
//        }
//        return changed ? (result) : that;
//    }
//
//    public List<AliasedSimpleName> recurOnListOfAliasedSimpleName(final List<AliasedSimpleName> that) {
//        boolean changed = false;
//        List<AliasedSimpleName> result = new java.util.ArrayList<AliasedSimpleName>(0);
//        for (AliasedSimpleName elt : that) {
//            AliasedSimpleName elt_result = (AliasedSimpleName) elt.accept(this);
//            result.add(elt_result);
//            if (elt != elt_result) changed = true;
//        }
//        return changed ? (result) : that;
//    }
//
//    public List<AliasedAPIName> recurOnListOfAliasedAPIName(final List<AliasedAPIName> that) {
//        boolean changed = false;
//        List<AliasedAPIName> result = new java.util.ArrayList<AliasedAPIName>(0);
//        for (AliasedAPIName elt : that) {
//            AliasedAPIName elt_result = (AliasedAPIName) elt.accept(this);
//            result.add(elt_result);
//            if (elt != elt_result) changed = true;
//        }
//        return changed ? (result) : that;
//    }
//
//    public Option<SimpleName> recurOnOptionOfSimpleName(final Option<SimpleName> that) {
//        return that.apply(new edu.rice.cs.plt.tuple.OptionVisitor<SimpleName, Option<SimpleName>>() {
//            public Option<SimpleName> forSome(SimpleName elt) {
//                SimpleName elt_result = (SimpleName) elt.accept(NodeUpdateVisitor.this);
//                return (elt == elt_result) ? that : edu.rice.cs.plt.tuple.Option.some(elt_result);
//            }
//
//            public Option<SimpleName> forNone() {
//                return that;
//            }
//        });
//    }
//
//    public Option<Id> recurOnOptionOfId(final Option<Id> that) {
//        return that.apply(new edu.rice.cs.plt.tuple.OptionVisitor<Id, Option<Id>>() {
//            public Option<Id> forSome(Id elt) {
//                Id elt_result = (Id) elt.accept(NodeUpdateVisitor.this);
//                return (elt == elt_result) ? that : edu.rice.cs.plt.tuple.Option.some(elt_result);
//            }
//
//            public Option<Id> forNone() {
//                return that;
//            }
//        });
//    }
//
//    public List<APIName> recurOnListOfAPIName(final List<APIName> that) {
//        boolean changed = false;
//        List<APIName> result = new java.util.ArrayList<APIName>(0);
//        for (APIName elt : that) {
//            APIName elt_result = (APIName) elt.accept(this);
//            result.add(elt_result);
//            if (elt != elt_result) changed = true;
//        }
//        return changed ? (result) : that;
//    }
//
//    public List<Modifier> recurOnListOfModifier(final List<Modifier> that) {
//        boolean changed = false;
//        List<Modifier> result = new java.util.ArrayList<Modifier>(0);
//        for (Modifier elt : that) {
//            Modifier elt_result = (Modifier) elt.accept(this);
//            result.add(elt_result);
//            if (elt != elt_result) changed = true;
//        }
//        return changed ? (result) : that;
//    }
//
//    public List<StaticParam> recurOnListOfStaticParam(final List<StaticParam> that) {
//        boolean changed = false;
//        List<StaticParam> result = new java.util.ArrayList<StaticParam>(0);
//        for (StaticParam elt : that) {
//            StaticParam elt_result = (StaticParam) elt.accept(this);
//            result.add(elt_result);
//            if (elt != elt_result) changed = true;
//        }
//        return changed ? (result) : that;
//    }
//
//    public List<TraitTypeWhere> recurOnListOfTraitTypeWhere(final List<TraitTypeWhere> that) {
//        boolean changed = false;
//        List<TraitTypeWhere> result = new java.util.ArrayList<TraitTypeWhere>(0);
//        for (TraitTypeWhere elt : that) {
//            TraitTypeWhere elt_result = (TraitTypeWhere) elt.accept(this);
//            result.add(elt_result);
//            if (elt != elt_result) changed = true;
//        }
//        return changed ? (result) : that;
//    }
//
//    public List<TraitType> recurOnListOfTraitType(final List<TraitType> that) {
//        boolean changed = false;
//        List<TraitType> result = new java.util.ArrayList<TraitType>(0);
//        for (TraitType elt : that) {
//            TraitType elt_result = (TraitType) elt.accept(this);
//            result.add(elt_result);
//            if (elt != elt_result) changed = true;
//        }
//        return changed ? (result) : that;
//    }
//
//    public Option<List<TraitType>> recurOnOptionOfListOfTraitType(final Option<List<TraitType>> that) {
//        return that.apply(new edu.rice.cs.plt.tuple.OptionVisitor<List<TraitType>, Option<List<TraitType>>>() {
//            public Option<List<TraitType>> forSome(List<TraitType> elt) {
//                List<TraitType> elt_result = recurOnListOfTraitType(elt);
//                return (elt == elt_result) ? that : edu.rice.cs.plt.tuple.Option.some(elt_result);
//            }
//
//            public Option<List<TraitType>> forNone() {
//                return that;
//            }
//        });
//    }
//
//    public Option<List<Param>> recurOnOptionOfListOfParam(final Option<List<Param>> that) {
//        return that.apply(new edu.rice.cs.plt.tuple.OptionVisitor<List<Param>, Option<List<Param>>>() {
//            public Option<List<Param>> forSome(List<Param> elt) {
//                List<Param> elt_result = recurOnListOfParam(elt);
//                return (elt == elt_result) ? that : edu.rice.cs.plt.tuple.Option.some(elt_result);
//            }
//
//            public Option<List<Param>> forNone() {
//                return that;
//            }
//        });
//    }
//
//    public List<Param> recurOnListOfParam(final List<Param> that) {
//        boolean changed = false;
//        List<Param> result = new java.util.ArrayList<Param>(0);
//        for (Param elt : that) {
//            Param elt_result = (Param) elt.accept(this);
//            result.add(elt_result);
//            if (elt != elt_result) changed = true;
//        }
//        return changed ? (result) : that;
//    }
//
//    public List<LValueBind> recurOnListOfLValueBind(final List<LValueBind> that) {
//        boolean changed = false;
//        List<LValueBind> result = new java.util.ArrayList<LValueBind>(0);
//        for (LValueBind elt : that) {
//            LValueBind elt_result = (LValueBind) elt.accept(this);
//            result.add(elt_result);
//            if (elt != elt_result) changed = true;
//        }
//        return changed ? (result) : that;
//    }
//
//    public Option<Type> recurOnOptionOfType(final Option<Type> that) {
//        return that.apply(new edu.rice.cs.plt.tuple.OptionVisitor<Type, Option<Type>>() {
//            public Option<Type> forSome(Type elt) {
//                Type elt_result = (Type) elt.accept(NodeUpdateVisitor.this);
//                return (elt == elt_result) ? that : edu.rice.cs.plt.tuple.Option.some(elt_result);
//            }
//
//            public Option<Type> forNone() {
//                return that;
//            }
//        });
//    }
//
//    public List<ExtentRange> recurOnListOfExtentRange(final List<ExtentRange> that) {
//        boolean changed = false;
//        List<ExtentRange> result = new java.util.ArrayList<ExtentRange>(0);
//        for (ExtentRange elt : that) {
//            ExtentRange elt_result = (ExtentRange) elt.accept(this);
//            result.add(elt_result);
//            if (elt != elt_result) changed = true;
//        }
//        return changed ? (result) : that;
//    }
//
//    public List<Unpasting> recurOnListOfUnpasting(final List<Unpasting> that) {
//        boolean changed = false;
//        List<Unpasting> result = new java.util.ArrayList<Unpasting>(0);
//        for (Unpasting elt : that) {
//            Unpasting elt_result = (Unpasting) elt.accept(this);
//            result.add(elt_result);
//            if (elt != elt_result) changed = true;
//        }
//        return changed ? (result) : that;
//    }
//
//    public Option<Expr> recurOnOptionOfExpr(final Option<Expr> that) {
//        return that.apply(new edu.rice.cs.plt.tuple.OptionVisitor<Expr, Option<Expr>>() {
//            public Option<Expr> forSome(Expr elt) {
//                Expr elt_result = (Expr) elt.accept(NodeUpdateVisitor.this);
//                return (elt == elt_result) ? that : edu.rice.cs.plt.tuple.Option.some(elt_result);
//            }
//
//            public Option<Expr> forNone() {
//                return that;
//            }
//        });
//    }
//
//    public List<Id> recurOnListOfId(final List<Id> that) {
//        boolean changed = false;
//        List<Id> result = new java.util.ArrayList<Id>(0);
//        for (Id elt : that) {
//            Id elt_result = (Id) elt.accept(this);
//            result.add(elt_result);
//            if (elt != elt_result) changed = true;
//        }
//        return changed ? (result) : that;
//    }
//
//    public List<GeneratorClause> recurOnListOfGeneratorClause(final List<GeneratorClause> that) {
//        boolean changed = false;
//        List<GeneratorClause> result = new java.util.ArrayList<GeneratorClause>(0);
//        for (GeneratorClause elt : that) {
//            GeneratorClause elt_result = (GeneratorClause) elt.accept(this);
//            result.add(elt_result);
//            if (elt != elt_result) changed = true;
//        }
//        return changed ? (result) : that;
//    }
//
//    public List<QualifiedIdName> recurOnListOfQualifiedIdName(final List<QualifiedIdName> that) {
//        boolean changed = false;
//        List<QualifiedIdName> result = new java.util.ArrayList<QualifiedIdName>(0);
//        for (QualifiedIdName elt : that) {
//            QualifiedIdName elt_result = (QualifiedIdName) elt.accept(this);
//            result.add(elt_result);
//            if (elt != elt_result) changed = true;
//        }
//        return changed ? (result) : that;
//    }
//
//    public List<GrammarMemberDecl> recurOnListOfGrammarMemberDecl(final List<GrammarMemberDecl> that) {
//        boolean changed = false;
//        List<GrammarMemberDecl> result = new java.util.ArrayList<GrammarMemberDecl>(0);
//        for (GrammarMemberDecl elt : that) {
//            GrammarMemberDecl elt_result = (GrammarMemberDecl) elt.accept(this);
//            result.add(elt_result);
//            if (elt != elt_result) changed = true;
//        }
//        return changed ? (result) : that;
//    }
//
//    public Option<TraitType> recurOnOptionOfTraitType(final Option<TraitType> that) {
//        return that.apply(new edu.rice.cs.plt.tuple.OptionVisitor<TraitType, Option<TraitType>>() {
//            public Option<TraitType> forSome(TraitType elt) {
//                TraitType elt_result = (TraitType) elt.accept(NodeUpdateVisitor.this);
//                return (elt == elt_result) ? that : edu.rice.cs.plt.tuple.Option.some(elt_result);
//            }
//
//            public Option<TraitType> forNone() {
//                return that;
//            }
//        });
//    }
//
//    public Option<? extends Modifier> recurOnOptionOfModifier(final Option<? extends Modifier> that) {
//        return that.apply(new edu.rice.cs.plt.tuple.OptionVisitor<Modifier, Option<? extends Modifier>>() {
//            public Option<? extends Modifier> forSome(Modifier elt) {
//                Modifier elt_result = (Modifier) elt.accept(NodeUpdateVisitor.this);
//                return (elt == elt_result) ? that : edu.rice.cs.plt.tuple.Option.some(elt_result);
//            }
//
//            public Option<? extends Modifier> forNone() {
//                return that;
//            }
//        });
//    }
//
//    public List<SyntaxDef> recurOnListOfSyntaxDef(final List<SyntaxDef> that) {
//        boolean changed = false;
//        List<SyntaxDef> result = new java.util.ArrayList<SyntaxDef>(0);
//        for (SyntaxDef elt : that) {
//            SyntaxDef elt_result = (SyntaxDef) elt.accept(this);
//            result.add(elt_result);
//            if (elt != elt_result) changed = true;
//        }
//        return changed ? (result) : that;
//    }
//
//    public List<SyntaxSymbol> recurOnListOfSyntaxSymbol(final List<SyntaxSymbol> that) {
//        boolean changed = false;
//        List<SyntaxSymbol> result = new java.util.ArrayList<SyntaxSymbol>(0);
//        for (SyntaxSymbol elt : that) {
//            SyntaxSymbol elt_result = (SyntaxSymbol) elt.accept(this);
//            result.add(elt_result);
//            if (elt != elt_result) changed = true;
//        }
//        return changed ? (result) : that;
//    }
//
//    public List<CharacterSymbol> recurOnListOfCharacterSymbol(final List<CharacterSymbol> that) {
//        boolean changed = false;
//        List<CharacterSymbol> result = new java.util.ArrayList<CharacterSymbol>(0);
//        for (CharacterSymbol elt : that) {
//            CharacterSymbol elt_result = (CharacterSymbol) elt.accept(this);
//            result.add(elt_result);
//            if (elt != elt_result) changed = true;
//        }
//        return changed ? (result) : that;
//    }
//
//    public List<LHS> recurOnListOfLHS(final List<LHS> that) {
//        boolean changed = false;
//        List<LHS> result = new java.util.ArrayList<LHS>(0);
//        for (LHS elt : that) {
//            LHS elt_result = (LHS) elt.accept(this);
//            result.add(elt_result);
//            if (elt != elt_result) changed = true;
//        }
//        return changed ? (result) : that;
//    }
//
//    public Option<Op> recurOnOptionOfOp(final Option<Op> that) {
//        return that.apply(new edu.rice.cs.plt.tuple.OptionVisitor<Op, Option<Op>>() {
//            public Option<Op> forSome(Op elt) {
//                Op elt_result = (Op) elt.accept(NodeUpdateVisitor.this);
//                return (elt == elt_result) ? that : edu.rice.cs.plt.tuple.Option.some(elt_result);
//            }
//
//            public Option<Op> forNone() {
//                return that;
//            }
//        });
//    }
//
//    public List<Expr> recurOnListOfExpr(final List<Expr> that) {
//        boolean changed = false;
//        List<Expr> result = new java.util.ArrayList<Expr>(0);
//        for (Expr elt : that) {
//            Expr elt_result = (Expr) elt.accept(this);
//            result.add(elt_result);
//            if (elt != elt_result) changed = true;
//        }
//        return changed ? (result) : that;
//    }
//
//    public List<CaseClause> recurOnListOfCaseClause(final List<CaseClause> that) {
//        boolean changed = false;
//        List<CaseClause> result = new java.util.ArrayList<CaseClause>(0);
//        for (CaseClause elt : that) {
//            CaseClause elt_result = (CaseClause) elt.accept(this);
//            result.add(elt_result);
//            if (elt != elt_result) changed = true;
//        }
//        return changed ? (result) : that;
//    }
//
//    public Option<Block> recurOnOptionOfBlock(final Option<Block> that) {
//        return that.apply(new edu.rice.cs.plt.tuple.OptionVisitor<Block, Option<Block>>() {
//            public Option<Block> forSome(Block elt) {
//                Block elt_result = (Block) elt.accept(NodeUpdateVisitor.this);
//                return (elt == elt_result) ? that : edu.rice.cs.plt.tuple.Option.some(elt_result);
//            }
//
//            public Option<Block> forNone() {
//                return that;
//            }
//        });
//    }
//
//    public List<DoFront> recurOnListOfDoFront(final List<DoFront> that) {
//        boolean changed = false;
//        List<DoFront> result = new java.util.ArrayList<DoFront>(0);
//        for (DoFront elt : that) {
//            DoFront elt_result = (DoFront) elt.accept(this);
//            result.add(elt_result);
//            if (elt != elt_result) changed = true;
//        }
//        return changed ? (result) : that;
//    }
//
//    public List<IfClause> recurOnListOfIfClause(final List<IfClause> that) {
//        boolean changed = false;
//        List<IfClause> result = new java.util.ArrayList<IfClause>(0);
//        for (IfClause elt : that) {
//            IfClause elt_result = (IfClause) elt.accept(this);
//            result.add(elt_result);
//            if (elt != elt_result) changed = true;
//        }
//        return changed ? (result) : that;
//    }
//
//    public List<StaticArg> recurOnListOfStaticArg(final List<StaticArg> that) {
//        boolean changed = false;
//        List<StaticArg> result = new java.util.ArrayList<StaticArg>(0);
//        for (StaticArg elt : that) {
//            StaticArg elt_result = (StaticArg) elt.accept(this);
//            result.add(elt_result);
//            if (elt != elt_result) changed = true;
//        }
//        return changed ? (result) : that;
//    }
//
//    public Option<Catch> recurOnOptionOfCatch(final Option<Catch> that) {
//        return that.apply(new edu.rice.cs.plt.tuple.OptionVisitor<Catch, Option<Catch>>() {
//            public Option<Catch> forSome(Catch elt) {
//                Catch elt_result = (Catch) elt.accept(NodeUpdateVisitor.this);
//                return (elt == elt_result) ? that : edu.rice.cs.plt.tuple.Option.some(elt_result);
//            }
//
//            public Option<Catch> forNone() {
//                return that;
//            }
//        });
//    }
//
//    public Option<VarargsExpr> recurOnOptionOfVarargsExpr(final Option<VarargsExpr> that) {
//        return that.apply(new edu.rice.cs.plt.tuple.OptionVisitor<VarargsExpr, Option<VarargsExpr>>() {
//            public Option<VarargsExpr> forSome(VarargsExpr elt) {
//                VarargsExpr elt_result = (VarargsExpr) elt.accept(NodeUpdateVisitor.this);
//                return (elt == elt_result) ? that : edu.rice.cs.plt.tuple.Option.some(elt_result);
//            }
//
//            public Option<VarargsExpr> forNone() {
//                return that;
//            }
//        });
//    }
//
//    public List<KeywordExpr> recurOnListOfKeywordExpr(final List<KeywordExpr> that) {
//        boolean changed = false;
//        List<KeywordExpr> result = new java.util.ArrayList<KeywordExpr>(0);
//        for (KeywordExpr elt : that) {
//            KeywordExpr elt_result = (KeywordExpr) elt.accept(this);
//            result.add(elt_result);
//            if (elt != elt_result) changed = true;
//        }
//        return changed ? (result) : that;
//    }
//
//    public List<TypecaseClause> recurOnListOfTypecaseClause(final List<TypecaseClause> that) {
//        boolean changed = false;
//        List<TypecaseClause> result = new java.util.ArrayList<TypecaseClause>(0);
//        for (TypecaseClause elt : that) {
//            TypecaseClause elt_result = (TypecaseClause) elt.accept(this);
//            result.add(elt_result);
//            if (elt != elt_result) changed = true;
//        }
//        return changed ? (result) : that;
//    }
//
//    public List<ArrayComprehensionClause> recurOnListOfArrayComprehensionClause(final List<ArrayComprehensionClause> that) {
//        boolean changed = false;
//        List<ArrayComprehensionClause> result = new java.util.ArrayList<ArrayComprehensionClause>(0);
//        for (ArrayComprehensionClause elt : that) {
//            ArrayComprehensionClause elt_result = (ArrayComprehensionClause) elt.accept(this);
//            result.add(elt_result);
//            if (elt != elt_result) changed = true;
//        }
//        return changed ? (result) : that;
//    }
//
//    public List<FnDef> recurOnListOfFnDef(final List<FnDef> that) {
//        boolean changed = false;
//        List<FnDef> result = new java.util.ArrayList<FnDef>(0);
//        for (FnDef elt : that) {
//            FnDef elt_result = (FnDef) elt.accept(this);
//            result.add(elt_result);
//            if (elt != elt_result) changed = true;
//        }
//        return changed ? (result) : that;
//    }
//
//    public List<LValue> recurOnListOfLValue(final List<LValue> that) {
//        boolean changed = false;
//        List<LValue> result = new java.util.ArrayList<LValue>(0);
//        for (LValue elt : that) {
//            LValue elt_result = (LValue) elt.accept(this);
//            result.add(elt_result);
//            if (elt != elt_result) changed = true;
//        }
//        return changed ? (result) : that;
//    }
//
//    public Option<Enclosing> recurOnOptionOfEnclosing(final Option<Enclosing> that) {
//        return that.apply(new edu.rice.cs.plt.tuple.OptionVisitor<Enclosing, Option<Enclosing>>() {
//            public Option<Enclosing> forSome(Enclosing elt) {
//                Enclosing elt_result = (Enclosing) elt.accept(NodeUpdateVisitor.this);
//                return (elt == elt_result) ? that : edu.rice.cs.plt.tuple.Option.some(elt_result);
//            }
//
//            public Option<Enclosing> forNone() {
//                return that;
//            }
//        });
//    }
//
//    public List<QualifiedOpName> recurOnListOfQualifiedOpName(final List<QualifiedOpName> that) {
//        boolean changed = false;
//        List<QualifiedOpName> result = new java.util.ArrayList<QualifiedOpName>(0);
//        for (QualifiedOpName elt : that) {
//            QualifiedOpName elt_result = (QualifiedOpName) elt.accept(this);
//            result.add(elt_result);
//            if (elt != elt_result) changed = true;
//        }
//        return changed ? (result) : that;
//    }
//
//    public List<MathItem> recurOnListOfMathItem(final List<MathItem> that) {
//        boolean changed = false;
//        List<MathItem> result = new java.util.ArrayList<MathItem>(0);
//        for (MathItem elt : that) {
//            MathItem elt_result = (MathItem) elt.accept(this);
//            result.add(elt_result);
//            if (elt != elt_result) changed = true;
//        }
//        return changed ? (result) : that;
//    }
//
//    public List<ArrayExpr> recurOnListOfArrayExpr(final List<ArrayExpr> that) {
//        boolean changed = false;
//        List<ArrayExpr> result = new java.util.ArrayList<ArrayExpr>(0);
//        for (ArrayExpr elt : that) {
//            ArrayExpr elt_result = (ArrayExpr) elt.accept(this);
//            result.add(elt_result);
//            if (elt != elt_result) changed = true;
//        }
//        return changed ? (result) : that;
//    }
//
//    public Option<List<Type>> recurOnOptionOfListOfType(final Option<List<Type>> that) {
//        return that.apply(new edu.rice.cs.plt.tuple.OptionVisitor<List<Type>, Option<List<Type>>>() {
//            public Option<List<Type>> forSome(List<Type> elt) {
//                List<Type> elt_result = recurOnListOfType(elt);
//                return (elt == elt_result) ? that : edu.rice.cs.plt.tuple.Option.some(elt_result);
//            }
//
//            public Option<List<Type>> forNone() {
//                return that;
//            }
//        });
//    }
//
//    public List<Type> recurOnListOfType(final List<Type> that) {
//        boolean changed = false;
//        List<Type> result = new java.util.ArrayList<Type>(0);
//        for (Type elt : that) {
//            Type elt_result = (Type) elt.accept(this);
//            result.add(elt_result);
//            if (elt != elt_result) changed = true;
//        }
//        return changed ? (result) : that;
//    }
//
//    public Option<VarargsType> recurOnOptionOfVarargsType(final Option<VarargsType> that) {
//        return that.apply(new edu.rice.cs.plt.tuple.OptionVisitor<VarargsType, Option<VarargsType>>() {
//            public Option<VarargsType> forSome(VarargsType elt) {
//                VarargsType elt_result = (VarargsType) elt.accept(NodeUpdateVisitor.this);
//                return (elt == elt_result) ? that : edu.rice.cs.plt.tuple.Option.some(elt_result);
//            }
//
//            public Option<VarargsType> forNone() {
//                return that;
//            }
//        });
//    }
//
//    public List<KeywordType> recurOnListOfKeywordType(final List<KeywordType> that) {
//        boolean changed = false;
//        List<KeywordType> result = new java.util.ArrayList<KeywordType>(0);
//        for (KeywordType elt : that) {
//            KeywordType elt_result = (KeywordType) elt.accept(this);
//            result.add(elt_result);
//            if (elt != elt_result) changed = true;
//        }
//        return changed ? (result) : that;
//    }
//
//    public Set<Type> recurOnSetOfType(final Set<Type> that) {
//        boolean changed = false;
//        Set<Type> result = new java.util.HashSet<Type>(0);
//        for (Type elt : that) {
//            Type elt_result = (Type) elt.accept(this);
//            result.add(elt_result);
//            if (elt != elt_result) changed = true;
//        }
//        return changed ? (result) : that;
//    }
//
//    public List<WhereBinding> recurOnListOfWhereBinding(final List<WhereBinding> that) {
//        boolean changed = false;
//        List<WhereBinding> result = new java.util.ArrayList<WhereBinding>(0);
//        for (WhereBinding elt : that) {
//            WhereBinding elt_result = (WhereBinding) elt.accept(this);
//            result.add(elt_result);
//            if (elt != elt_result) changed = true;
//        }
//        return changed ? (result) : that;
//    }
//
//    public List<WhereConstraint> recurOnListOfWhereConstraint(final List<WhereConstraint> that) {
//        boolean changed = false;
//        List<WhereConstraint> result = new java.util.ArrayList<WhereConstraint>(0);
//        for (WhereConstraint elt : that) {
//            WhereConstraint elt_result = (WhereConstraint) elt.accept(this);
//            result.add(elt_result);
//            if (elt != elt_result) changed = true;
//        }
//        return changed ? (result) : that;
//    }
//
//    public Option<List<Expr>> recurOnOptionOfListOfExpr(final Option<List<Expr>> that) {
//        return that.apply(new edu.rice.cs.plt.tuple.OptionVisitor<List<Expr>, Option<List<Expr>>>() {
//            public Option<List<Expr>> forSome(List<Expr> elt) {
//                List<Expr> elt_result = recurOnListOfExpr(elt);
//                return (elt == elt_result) ? that : edu.rice.cs.plt.tuple.Option.some(elt_result);
//            }
//
//            public Option<List<Expr>> forNone() {
//                return that;
//            }
//        });
//    }
//
//    public Option<List<EnsuresClause>> recurOnOptionOfListOfEnsuresClause(final Option<List<EnsuresClause>> that) {
//        return that.apply(new edu.rice.cs.plt.tuple.OptionVisitor<List<EnsuresClause>, Option<List<EnsuresClause>>>() {
//            public Option<List<EnsuresClause>> forSome(List<EnsuresClause> elt) {
//                List<EnsuresClause> elt_result = recurOnListOfEnsuresClause(elt);
//                return (elt == elt_result) ? that : edu.rice.cs.plt.tuple.Option.some(elt_result);
//            }
//
//            public Option<List<EnsuresClause>> forNone() {
//                return that;
//            }
//        });
//    }
//
//    public List<EnsuresClause> recurOnListOfEnsuresClause(final List<EnsuresClause> that) {
//        boolean changed = false;
//        List<EnsuresClause> result = new java.util.ArrayList<EnsuresClause>(0);
//        for (EnsuresClause elt : that) {
//            EnsuresClause elt_result = (EnsuresClause) elt.accept(this);
//            result.add(elt_result);
//            if (elt != elt_result) changed = true;
//        }
//        return changed ? (result) : that;
//    }
//
//    public Option<APIName> recurOnOptionOfAPIName(final Option<APIName> that) {
//        return that.apply(new edu.rice.cs.plt.tuple.OptionVisitor<APIName, Option<APIName>>() {
//            public Option<APIName> forSome(APIName elt) {
//                APIName elt_result = (APIName) elt.accept(NodeUpdateVisitor.this);
//                return (elt == elt_result) ? that : edu.rice.cs.plt.tuple.Option.some(elt_result);
//            }
//
//            public Option<APIName> forNone() {
//                return that;
//            }
//        });
//    }
//
//    public Option<Fixity> recurOnOptionOfFixity(final Option<Fixity> that) {
//        return that.apply(new edu.rice.cs.plt.tuple.OptionVisitor<Fixity, Option<Fixity>>() {
//            public Option<Fixity> forSome(Fixity elt) {
//                Fixity elt_result = (Fixity) elt.accept(NodeUpdateVisitor.this);
//                return (elt == elt_result) ? that : edu.rice.cs.plt.tuple.Option.some(elt_result);
//            }
//
//            public Option<Fixity> forNone() {
//                return that;
//            }
//        });
//    }
//
//    public List<CatchClause> recurOnListOfCatchClause(final List<CatchClause> that) {
//        boolean changed = false;
//        List<CatchClause> result = new java.util.ArrayList<CatchClause>(0);
//        for (CatchClause elt : that) {
//            CatchClause elt_result = (CatchClause) elt.accept(this);
//            result.add(elt_result);
//            if (elt != elt_result) changed = true;
//        }
//        return changed ? (result) : that;
//    }
//
//    public Option<StaticArg> recurOnOptionOfStaticArg(final Option<StaticArg> that) {
//        return that.apply(new edu.rice.cs.plt.tuple.OptionVisitor<StaticArg, Option<StaticArg>>() {
//            public Option<StaticArg> forSome(StaticArg elt) {
//                StaticArg elt_result = (StaticArg) elt.accept(NodeUpdateVisitor.this);
//                return (elt == elt_result) ? that : edu.rice.cs.plt.tuple.Option.some(elt_result);
//            }
//
//            public Option<StaticArg> forNone() {
//                return that;
//            }
//        });
//    }
}
