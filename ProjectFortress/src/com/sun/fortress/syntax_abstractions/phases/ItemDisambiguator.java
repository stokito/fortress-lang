/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.syntax_abstractions.phases;

import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.disambiguator.NonterminalEnv;
import com.sun.fortress.compiler.disambiguator.NonterminalNameDisambiguator;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.exceptions.MacroError;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.parser_util.IdentifierUtil;
import com.sun.fortress.useful.Debug;
import com.sun.fortress.useful.HasAt;
import edu.rice.cs.plt.tuple.Option;

import java.util.Collection;
import java.util.List;

/* ItemDisambiguator
 * - Disambiguates "items" to nonterminals or keywords/tokens
 * - Disambiguates occurrences of nonterminal names within patterns
 *    eg, Expr -> FortressSyntax.Expression.Expr
 */
public class ItemDisambiguator extends NodeUpdateVisitor {

    private Collection<StaticError> _errors;
    private GlobalEnvironment _globalEnv;
    private GrammarIndex _currentGrammarIndex;
    private ApiIndex _currentApi;

    public ItemDisambiguator(GlobalEnvironment env, List<StaticError> errors) {
        this._errors = errors;
        this._globalEnv = env;
    }

    private void error(String msg, HasAt loc) {
        this._errors.add(StaticError.make(msg, loc));
    }

    public Collection<StaticError> errors() {
        return this._errors;
    }

    @Override
    public Node forApi(Api that) {
        if (this._globalEnv.definesApi(that.getName())) {
            this._currentApi = this._globalEnv.api(that.getName());
        } else error("Undefined API ", that);
        return super.forApi(that);
    }

    @Override
    public Node forGrammarDecl(GrammarDecl that) {
        Option<GrammarIndex> index = this.grammarIndex(that.getName());
        if (index.isSome()) {
            this._currentGrammarIndex = index.unwrap();
        } else {
            error("Grammar " + that.getName() + " not found", that);
        }
        return super.forGrammarDecl(that);
    }

    private Option<GrammarIndex> grammarIndex(Id name) {
        if (name.getApiName().isSome()) {
            APIName api = name.getApiName().unwrap();
            if (this._globalEnv.definesApi(api)) {
                return Option.some(_globalEnv.api(api).grammars().get(name.getText()));
            } else {
                return Option.none();
            }
        } else return Option.some(((ApiIndex) _currentApi).grammars().get(name));
    }

    @Override
    public Node forUnparsedTransformer(UnparsedTransformer that) {
        NonterminalNameDisambiguator nnd = new NonterminalNameDisambiguator(this._globalEnv);
        Option<Id> oname = nnd.handleNonterminalName(new NonterminalEnv(this._currentGrammarIndex),
                                                     that.getNonterminal());
        if (oname.isSome()) {
            return new UnparsedTransformer(NodeFactory.makeSpanInfo(NodeFactory.makeSpan(that, oname.unwrap())),
                                           that.getTransformer(),
                                           oname.unwrap());
        } else {
            throw new MacroError(that, "Cannot find nonterminal " + that.getNonterminal());
        }
    }

    @Override
    public Node forItemSymbol(ItemSymbol that) {
        SyntaxSymbol n = nameResolution(that);
        Debug.debug(Debug.Type.SYNTAX, 4, "Resolve item symbol ", that.getItem(), " to ", n);
        return n;
    }

    private SyntaxSymbol nameResolution(ItemSymbol item) {
        if (IdentifierUtil.validId(item.getItem())) {
            Id name = makeId(NodeUtil.getSpan(item), item.getItem());
            NonterminalNameDisambiguator nnd = new NonterminalNameDisambiguator(this._globalEnv);
            Option<Id> oname = nnd.handleNonterminalName(new NonterminalEnv(this._currentGrammarIndex), name);
            if (oname.isSome()) {
                name = oname.unwrap();
                return makeNonterminal(item, name);
            } else {
                return makeKeywordSymbol(item);
            }
        } else {
            return makeTokenSymbol(item);
        }
    }

    private static Id makeId(Span span, String item) {
        int lastIndexOf = item.lastIndexOf('.');
        if (lastIndexOf != -1) {
            APIName apiName = NodeFactory.makeAPIName(span, item.substring(0, lastIndexOf));
            return NodeFactory.makeId(span, apiName, NodeFactory.makeId(span, item.substring(lastIndexOf + 1)));
        } else {
            return NodeFactory.makeId(span, item);
        }
    }

    private NonterminalSymbol makeNonterminal(ItemSymbol that, Id name) {
        return new NonterminalSymbol(that.getInfo(), name);
    }

    private KeywordSymbol makeKeywordSymbol(ItemSymbol that) {
        return new KeywordSymbol(that.getInfo(), that.getItem());
    }

    private TokenSymbol makeTokenSymbol(ItemSymbol that) {
        return new TokenSymbol(that.getInfo(), that.getItem());
    }
}
