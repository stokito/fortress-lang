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

import static com.sun.fortress.interpreter.evaluator.InterpreterBug.bug;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.compiler.index.NonterminalIndex;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.GrammarDecl;
import com.sun.fortress.nodes.GrammarMemberDecl;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.syntax_abstractions.phases.GrammarAnalyzer;

import edu.rice.cs.plt.tuple.Option;

/**
 *  This nonterminal environment is used during disambiguation of nonterminal names
 *  The name of the nonterminal should not be qualified, unless the method comments
 *  states otherwise. The environment returns qualified names.
 *  We assume that the name of the given grammar has been disambiguated.
 *  The nonterminal environment has access to the nonterminal names
 *  declared in the current grammar (using explicitNonterminalNames) and
 *  to those inherited from extended grammars (using inheritedNonterminalNames()).
 */
public class NonterminalEnv {

    private GrammarIndex current;
    private Map<String, Set<Id>> nonterminals = new HashMap<String, Set<Id>>();

    public NonterminalEnv(GrammarIndex currentGrammar) {
        this.current = currentGrammar;
        initializeNonterminals();
    }

    public GrammarIndex getGrammarIndex() {
        return this.current;
    }

    /**
     * Initialize the mapping from nonterminal names to sets of qualified nonterminal names
     */
    private void initializeNonterminals() {
        for (NonterminalIndex<? extends GrammarMemberDecl> e: this.getGrammarIndex().getDeclaredNonterminals()) {
            Option<? extends GrammarDecl> optGd = this.getGrammarIndex().ast();
            GrammarDecl currentGrammar;
            if (optGd.isSome()) {
                currentGrammar = optGd.unwrap();
            } else {
                currentGrammar = bug("NonterminalEnv.initializeNonterminals has failed!");
            }

            Span span = e.getName().getSpan();
            String key = e.getName().getText();
            APIName api = constructNonterminalApi(currentGrammar.getName());
            Id qname = NodeFactory.makeId(span, api, key);

            if (nonterminals.containsKey(key)) {
                nonterminals.get(key).add(qname);
            } else {
                Set<Id> matches = new HashSet<Id>();
                matches.add(qname);
                nonterminals.put(key, matches);
            }
        }
    }

    /**
     * Given a grammar name, construct an API for a nonterminal
     * An API for a nonterminal is the API of the grammar
     * concatenated with the name of the grammar.
     * @param grammarName
     * @return
     */
    private APIName constructNonterminalApi(Id grammarName) {
        Option<APIName> optApi = grammarName.getApi();
        APIName api;
        if (optApi.isSome())
            api = optApi.unwrap();
        else
            api = bug("NonterminalEnv.constructNonterminalApi is failed!");
        List<Id> ls = new LinkedList<Id>();
        ls.addAll(api.getIds());
        ls.add(NodeFactory.makeId(grammarName.getSpan(), grammarName.getText()));
        return NodeFactory.makeAPIName(grammarName.getSpan(), ls);
    }

    /**
     * Given a disambiguated name (aliases and imports have been resolved),
     * determine whether a nonterminal exists.  Assumes {@code name.getApi().isSome()}.
     */
    public boolean hasQualifiedNonterminal(Id name) {
        Option<APIName> optApi = name.getApi();
        if (optApi.isNone())
            bug(name, "A qualified identifier is supposed to have an API name, but the api is not present");
        APIName api = getApi(optApi.unwrap());
        Id gname = getGrammarNameFromLastIdInAPI(optApi.unwrap());
        Id grammarName = NodeFactory.makeId(api, gname);

        Set<Id> names = null;
        if (grammarName.equals(this.current.getName())) {
            names = this.declaredNonterminalNames(name.getText());
        }
        // If the nonterminal is not defined in the current grammar then look
        // among the inherited nonterminal names
        else {
            names = this.inheritedNonterminalNames(name.getText());
        }
        return !names.isEmpty();
    }

    /** Determine whether a nonterminal with the given name is defined.
     *  We assume that the given name is unqualified
     */
    public boolean hasNonterminal(String name) {
        if (this.nonterminals.containsKey(name)) {
            return true;
        }
        return false;
    }

    /**
     * Produce the set of qualified names corresponding to the given
     * nonterminal name.  If the name is not declared in the current grammar
     * an empty set is produced, and an ambiguous reference produces a set
     * of size greater than 1.
     * @param an unqualified nonterminal name
     */
    public Set<Id> declaredNonterminalNames(String name) {
        GrammarIndex grammar = this.getGrammarIndex();
        Set<Id> results = new HashSet<Id>();
        if (this.nonterminals.containsKey(name)) {
            if (grammar.ast().isSome()) {
                return this.nonterminals.get(name);
            }
        }
        return results;
    }

    /**
     * Produce the set of inherited qualified names corresponding to the given
     * nonterminal name. If the name is not declared in any extended grammar
     * an empty set is produced, and an ambiguous reference produces a set
     * of size greater than 1.
     * @param an unqualified nonterminal name
     */
    public Set<Id> inheritedNonterminalNames(String name) {
        GrammarAnalyzer<GrammarIndex> ga = new GrammarAnalyzer<GrammarIndex>();
        Set<Id> results = ga.getInherited(name, this.current);
        return results;
    }

    private Id getGrammarNameFromLastIdInAPI(APIName name) {
        return name.getIds().get(name.getIds().size()-1);
    }

    private APIName getApi(APIName name) {
        if (name.getIds().size() <= 1) {
            return NodeFactory.makeAPIName(new LinkedList<Id>());
        }
        List<Id> ids = new LinkedList<Id>();
        ids.addAll(name.getIds());
        ids.remove(ids.size()-1);
        return NodeFactory.makeAPIName(ids);
    }

}
