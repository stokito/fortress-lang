/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.disambiguator;

import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.HasAt;
import edu.rice.cs.plt.tuple.Option;

import java.util.List;

public class NonterminalDisambiguator extends NodeUpdateVisitor {

    private NameEnv _env;
    private List<StaticError> _errors;
    private NonterminalEnv _currentEnv;
    private GlobalEnvironment _globalEnv;

    public NonterminalDisambiguator(NameEnv env, GlobalEnvironment globalEnv, List<StaticError> newErrs) {
        this._env = env;
        this._errors = newErrs;
        this._globalEnv = globalEnv;
    }

    private void error(String msg, HasAt loc) {
        this._errors.add(StaticError.make(msg, loc));
    }

    @Override
    public Node forGrammarDecl(GrammarDecl that) {
        if (this._env.grammarIndex(that.getName()).isSome()) {
            this._currentEnv = new NonterminalEnv(this._env.grammarIndex(that.getName()).unwrap());
        } else {
            error("Undefined grammar: " + NodeUtil.nameString(that.getName()), that.getName());
        }
        return super.forGrammarDecl(that);
    }

    @Override
    public Node forNonterminalDefOnly(NonterminalDef that,
                                      ASTNodeInfo info,
                                      Id _name_result,
                                      List<SyntaxDecl> syntaxDefs_result,
                                      NonterminalHeader header_result,
                                      Option<BaseType> astType_result) {
        if (astType_result.isNone()) {
            error("Type inference is not supported yet!", that);
        }
        return super.forNonterminalDefOnly(that,
                                           that.getInfo(),
                                           header_result.getName(),
                                           syntaxDefs_result,
                                           header_result,
                                           astType_result);
    }

    @Override
    public Node forNonterminalExtensionDefOnly(NonterminalExtensionDef that,
                                               ASTNodeInfo info,
                                               Id name_result,
                                               List<SyntaxDecl> syntaxDefs_result) {
        Id name = disambiguateNonterminalName(name_result);
        return super.forNonterminalExtensionDefOnly(that, that.getInfo(), name, syntaxDefs_result);
    }

    @Override
    public Node forNonterminalHeaderOnly(NonterminalHeader that,
                                         ASTNodeInfo info,
                                         Id name_result,
                                         List<NonterminalParameter> params_result,
                                         List<StaticParam> staticParams_result,
                                         Option<Type> type_result,
                                         Option<WhereClause> whereClause_result) {
        Id name = disambiguateNonterminalName(name_result);
        return super.forNonterminalHeaderOnly(that,
                                              that.getInfo(),
                                              name,
                                              params_result,
                                              staticParams_result,
                                              type_result,
                                              whereClause_result);
    }

    private Id disambiguateNonterminalName(Id name) {
        NonterminalNameDisambiguator pnd = new NonterminalNameDisambiguator(this._globalEnv);

        // Disambiguate the name
        Option<Id> oname = pnd.handleNonterminalName(_currentEnv, name);
        this._errors.addAll(pnd.errors());
        if (oname.isSome()) {
            return oname.unwrap();
        } else {
            return name;
        }
    }

}
