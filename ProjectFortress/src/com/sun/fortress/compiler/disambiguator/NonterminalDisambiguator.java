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

package com.sun.fortress.compiler.disambiguator;

import java.util.LinkedList;
import java.util.List;

import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.disambiguator.NameEnv;
import com.sun.fortress.compiler.disambiguator.NonterminalNameDisambiguator;
import com.sun.fortress.compiler.disambiguator.NonterminalEnv;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.nodes.AbsExternalSyntax;
import com.sun.fortress.nodes.GrammarDef;
import com.sun.fortress.nodes.GrammarMemberDecl;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.nodes.Modifier;
import com.sun.fortress.nodes.ModifierPrivate;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.NonterminalDef;
import com.sun.fortress.nodes.NonterminalExtensionDef;
import com.sun.fortress.nodes.NonterminalHeader;
import com.sun.fortress.nodes.NonterminalParameter;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.SyntaxDecl;
import com.sun.fortress.nodes.SyntaxDef;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.TransformerDecl;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.WhereClause;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.Pair;

import edu.rice.cs.plt.tuple.Option;

import static com.sun.fortress.exceptions.InterpreterBug.bug;

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
    public Node forGrammarDef(GrammarDef that) {
        if (this._env.grammarIndex(that.getName()).isSome()) {
            this._currentEnv = new NonterminalEnv(this._env.grammarIndex(that.getName()).unwrap());
        }
        else {
            error("Undefined grammar: " + NodeUtil.nameString(that.getName()), that.getName());
        }
        return super.forGrammarDef(that);
    }

    /*
    // Isn't this the default implementation?
    @Override
    public Node forGrammarDefOnly(GrammarDef that, Id name_result, List<Id> extends_result, List<GrammarMemberDecl> nonterminal_result, List<TransformerDecl> transformers) {
        return new GrammarDef(that.getSpan(), name_result, extends_result, nonterminal_result, transformers, that.isNative());
    }
    */

    @Override
    public Node forNonterminalDefOnly(NonterminalDef that,
            NonterminalHeader header_result, Option<BaseType> astType_result,
            List<SyntaxDecl> syntaxDefs_result) {
        if (astType_result.isNone()) {
            throw new RuntimeException("Type inference is not supported yet!");
        }
        return super.forNonterminalDefOnly(that, header_result, astType_result, syntaxDefs_result);
    }

    @Override
    public Node forNonterminalHeaderOnly(NonterminalHeader that,
            Option<ModifierPrivate> modifier_result, Id name_result,
            List<NonterminalParameter> params_result,
            List<StaticParam> staticParams_result, Option<Type> type_result,
            Option<WhereClause> whereClause_result) {
        NonterminalNameDisambiguator pnd = new NonterminalNameDisambiguator(this._globalEnv);

        // Disambiguate the name
        Option<Id> oname = pnd.handleNonterminalName(_currentEnv, that.getName());
        Id name = that.getName();
        if (oname.isSome()) {
            name = oname.unwrap();
        }

        this._errors.addAll(pnd.errors());
        return new NonterminalHeader(that.getSpan(), modifier_result, name, params_result, staticParams_result, type_result, whereClause_result);
    }
}
