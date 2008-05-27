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

package com.sun.fortress.syntax_abstractions.phases;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.GrammarDef;
import com.sun.fortress.nodes.GrammarMemberDecl;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.ModifierPrivate;
import com.sun.fortress.nodes.NonterminalHeader;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.VarType;
import com.sun.fortress.nodes.KeywordSymbol;
import com.sun.fortress.nodes.Modifier;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.NonterminalSymbol;
import com.sun.fortress.nodes.NotPredicateSymbol;
import com.sun.fortress.nodes.PrefixedSymbol;
import com.sun.fortress.nodes.SyntaxDef;
import com.sun.fortress.nodes.SyntaxSymbol;
import com.sun.fortress.nodes.TokenSymbol;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.TransformationTemplateDef;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.WhereClause;
import com.sun.fortress.nodes._TerminalDef;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.syntax_abstractions.rats.util.FreshName;
import com.sun.fortress.syntax_abstractions.util.SyntaxAbstractionUtil;
import com.sun.fortress.useful.Pair;

import edu.rice.cs.plt.tuple.Option;

/*
 * Rewrite occurrences of Keyword symbols and token symbols to
 * nonterminal references to terminal definitions
 */
public class TerminalRewriter extends NodeUpdateVisitor {

    private static final String STRINGLITERALEXPR = "StringLiteralExpr";
    private Collection<_TerminalDef> _terminalDefs;
    private List<Id> apiName;
    private String var;

    @Override
    public Node forGrammarDef(GrammarDef that) {
        this._terminalDefs = new LinkedList<_TerminalDef>();
        this.apiName = new LinkedList<Id>();

        Id name = that.getName();
        assert(name.getApi().isSome());
        this.apiName.addAll(name.getApi().unwrap().getIds());
        this.apiName.add(NodeFactory.makeId(name.getSpan(), name.getText()));
        return super.forGrammarDef(that);
    }

    @Override
    public Node forGrammarDefOnly(GrammarDef that, Id name_result,
            List<Id> extends_result,
            List<GrammarMemberDecl> members_result) {
        members_result.addAll(this._terminalDefs);
        return super.forGrammarDefOnly(that, name_result, extends_result,
                members_result);
    }

    @Override
    public Node forPrefixedSymbol(PrefixedSymbol that) {
        assert(that.getId().isSome());
        this.var = that.getId().unwrap().getText();
        return super.forPrefixedSymbol(that);
    }

    @Override
    public Node forNonterminalSymbol(NonterminalSymbol that) {
     return super.forNonterminalSymbol(that);
    }


    @Override
    public Node forKeywordSymbol(KeywordSymbol that) {
        return handleTerminal(that, that.getToken());
    }

    @Override
    public Node forTokenSymbol(TokenSymbol that) {
        return handleTerminal(that, that.getToken());
    }

    /**
     * TODO: Check to see if we already have created terminal definition
     * with the same token and reuse it.
     * @param that
     * @return
     */
    private Node handleTerminal(SyntaxSymbol that, String token){
        // Create a new name for the terminal definition
        String var = "";
        if (null != this.var) {
            var = FreshName.getFreshName("T"+this.var.toUpperCase());
        }
        else {
            var = FreshName.getFreshName("T");
        }
        APIName apiName = NodeFactory.makeAPIName(that.getSpan(), this.apiName);
        Id id = NodeFactory.makeId(that.getSpan(), var);
        Id name = NodeFactory.makeId(that.getSpan(), apiName, id);

        // Create a the return type - A StringLiteralExpr
        Option<BaseType> type = Option.<BaseType>some(new VarType(that.getSpan(), NodeFactory.makeId("FortressBuiltin", STRINGLITERALEXPR)));

        // Create the syntax symbol inside the terminal definition
        List<SyntaxSymbol> syntaxSymbols = new LinkedList<SyntaxSymbol>();
        syntaxSymbols.add(that);

        // Create the transformation expression
        Expr transformationExpression = NodeFactory.makeStringLiteralExpr(that.getSpan(), token);

        // Add the terminal definition to the collection of new terminal definitions
        SyntaxDef syntaxDef = new SyntaxDef(syntaxSymbols, new TransformationTemplateDef(transformationExpression));
        Option<ModifierPrivate> mods = Option.none();
        List<StaticParam> st = new LinkedList<StaticParam>();
        Type t = NodeFactory.makeTraitType(NodeFactory.makeId("FortressLibrary", "String"));
        WhereClause whereClauses = new WhereClause(that.getSpan());
        NonterminalHeader header = new NonterminalHeader(that.getSpan(), mods, name, new LinkedList<Pair<Id, Type>>(), st, Option.some(t), whereClauses);
        this._terminalDefs.add(new _TerminalDef(that.getSpan(), header, type, syntaxDef));

        // Return a new nonterminal reference to the new terminal definition
        return new NonterminalSymbol(that.getSpan(), NodeFactory.makeId(that.getSpan(), apiName, id));
    }

}
