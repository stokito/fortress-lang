/*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
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
package com.sun.fortress.shell;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.*;
import com.sun.fortress.useful.*;
import edu.rice.cs.plt.tuple.Option;

/**
 * A visitor that makes an api from a component.
 */
public final class ApiMaker extends NodeUpdateVisitor {
    public static final ApiMaker ONLY = new ApiMaker();

    private ApiMaker() {}

    private List<AbsDecl> declsToAbsDecls(final List<Decl> that) {
        boolean changed = false;
        List<AbsDecl> result = new java.util.ArrayList<AbsDecl>(0);
        for (Decl elt : that) {
            AbsDecl elt_result = (AbsDecl) elt.accept(this);
            result.add(elt_result);
        }
        return result;
    }

    public Node forComponent(Component that) {
        APIName name_result = (APIName) that.getName().accept(this);
        List<Import> imports_result = recurOnListOfImport(that.getImports());
        List<Export> exports_result = recurOnListOfExport(that.getExports());
        List<AbsDecl> decls_result = declsToAbsDecls(that.getDecls());
        return new Api(that.getSpan(), name_result, imports_result, decls_result);
    }

    public Node forTraitDecl(TraitDecl that) {
        List<Modifier> mods_result = recurOnListOfModifier(that.getMods());
        Id name_result = (Id) that.getName().accept(this);
        List<StaticParam> staticParams_result = recurOnListOfStaticParam(that.getStaticParams());
        List<TraitTypeWhere> extendsClause_result = recurOnListOfTraitTypeWhere(that.getExtendsClause());
        WhereClause where_result = (WhereClause) that.getWhere().accept(this);
        List<TraitType> excludes_result = recurOnListOfTraitType(that.getExcludes());
        Option<List<TraitType>> comprises_result = recurOnOptionOfListOfTraitType(that.getComprises());
        List<AbsDecl> decls_result = declsToAbsDecls(that.getDecls());

        return new AbsTraitDecl(that.getSpan(),
                                mods_result,
                                name_result,
                                staticParams_result,
                                extendsClause_result,
                                where_result,
                                excludes_result,
                                comprises_result,
                                decls_result);
    }

    public Node forObjectDecl(ObjectDecl that) {
        List<Modifier> mods_result = recurOnListOfModifier(that.getMods());
        Id name_result = (Id) that.getName().accept(this);
        List<StaticParam> staticParams_result = recurOnListOfStaticParam(that.getStaticParams());
        List<TraitTypeWhere> extendsClause_result = recurOnListOfTraitTypeWhere(that.getExtendsClause());
        WhereClause where_result = (WhereClause) that.getWhere().accept(this);
        Option<List<Param>> params_result = recurOnOptionOfListOfParam(that.getParams());
        Option<List<TraitType>> throwsClause_result = recurOnOptionOfListOfTraitType(that.getThrowsClause());
        Contract contract_result = (Contract) that.getContract().accept(this);
        List<AbsDecl> decls_result = declsToAbsDecls(that.getDecls());

                return new AbsObjectDecl(that.getSpan(),
                                 mods_result,
                                 name_result,
                                 staticParams_result,
                                 extendsClause_result,
                                 where_result,
                                 params_result,
                                 throwsClause_result,
                                 contract_result,
                                 decls_result);
    }

    public Node forVarDeclOnly(VarDecl that, List<LValueBind> lhs_result, Expr init_result) {
        return new AbsVarDecl(that.getSpan(), lhs_result);
    }

    public Node forFnDefOnly(FnDef that,
                             List<Modifier> mods_result,
                             SimpleName name_result,
                             List<StaticParam> staticParams_result,
                             List<Param> params_result,
                             Option<Type> returnType_result,
                             Option<List<TraitType>> throwsClause_result,
                             WhereClause where_result,
                             Contract contract_result,
                             Expr body_result) {
        return new AbsFnDecl(that.getSpan(),
                             mods_result,
                             name_result,
                             staticParams_result,
                             params_result,
                             returnType_result,
                             throwsClause_result,
                             where_result,
                             contract_result,
                             that.getSelfName());
    }

    public Node forExternalSyntaxOnly(ExternalSyntax that,
                                      SimpleName openExpander_result,
                                      Id name_result,
                                      SimpleName closeExpander_result,
                                      Expr expr_result) {
        return new AbsExternalSyntax(that.getSpan(),
                                     openExpander_result,
                                     name_result,
                                     closeExpander_result);
    }



    // The following for- methods are overridden solely as an optimization;
    // we can short-circuit processing of AST fragments that are simply
    // thrown away by the above forOnly- methods.

    public Node forVarDecl(VarDecl that) {
        List<LValueBind> lhs_result = recurOnListOfLValueBind(that.getLhs());
        // No need to recur on the body, since we throw it away.
        Expr init_result = null;
        return forVarDeclOnly(that, lhs_result, init_result);
    }

    public Node forFnDef(FnDef that) {
        List<Modifier> mods_result = recurOnListOfModifier(that.getMods());
        SimpleName name_result = (SimpleName) that.getName().accept(this);
        List<StaticParam> staticParams_result = recurOnListOfStaticParam(that.getStaticParams());
        List<Param> params_result = recurOnListOfParam(that.getParams());
        Option<Type> returnType_result = recurOnOptionOfType(that.getReturnType());
        Option<List<TraitType>> throwsClause_result = recurOnOptionOfListOfTraitType(that.getThrowsClause());
        WhereClause where_result = (WhereClause) that.getWhere().accept(this);
        Contract contract_result = (Contract) that.getContract().accept(this);
        // No need to recur on the body, since we throw it away.
        Expr body_result = null;
        return forFnDefOnly(that,
                            mods_result,
                            name_result,
                            staticParams_result,
                            params_result,
                            returnType_result,
                            throwsClause_result,
                            where_result,
                            contract_result,
                            body_result);
    }

    public Node forExternalSyntax(ExternalSyntax that) {
        SimpleName openExpander_result = (SimpleName) that.getOpenExpander().accept(this);
        Id name_result = (Id) that.getName().accept(this);
        SimpleName closeExpander_result = (SimpleName) that.getCloseExpander().accept(this);
        // No need to recur on the body, since we throw it away.
        Expr expr_result = null;
        return forExternalSyntaxOnly(that, openExpander_result, name_result, closeExpander_result, expr_result);
    }
}
