/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.disambiguator;

import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.compiler.index.NonterminalIndex;
import static com.sun.fortress.exceptions.InterpreterBug.bug;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.useful.Debug;
import edu.rice.cs.plt.tuple.Option;

import java.util.*;

/**
 * This nonterminal environment is used during disambiguation of nonterminal names
 * The name of the nonterminal should not be qualified, unless the method comments
 * states otherwise. The environment returns qualified names.
 * We assume that the name of the given grammar has been disambiguated.
 * The nonterminal environment has access to the nonterminal names
 * declared in the current grammar (using explicitNonterminalNames) and
 * to those inherited from extended grammars (using inheritedNonterminalNames()).
 */
public class NonterminalEnv {

    private GrammarIndex current;
    private Map<String, Set<Id>> nonterminals = new HashMap<String, Set<Id>>();

    public NonterminalEnv(GrammarIndex currentGrammar) {
        if (currentGrammar == null) {
            throw new RuntimeException("Current grammar is null");
        }
        this.current = currentGrammar;
        Debug.debug(Debug.Type.COMPILER, 2, "Current grammar is ", current);
        initializeNonterminals();
    }

    public GrammarIndex getGrammarIndex() {
        return this.current;
    }

    /**
     * Initialize the mapping from nonterminal names to sets of qualified nonterminal names
     */
    private void initializeNonterminals() {
        for (final NonterminalIndex e : this.getGrammarIndex().getDeclaredNonterminals()) {

            e.ast().accept(new NodeDepthFirstVisitor_void() {
                @Override
                public void forNonterminalDefOnly(NonterminalDef that) {
                    GrammarDecl currentGrammar = NonterminalEnv.this.getGrammarIndex().ast();

                    Span span = NodeUtil.getSpan(e.getName());
                    String key = e.getName().getText();
                    APIName api = constructNonterminalApi(currentGrammar.getName());
                    Id qname = NodeFactory.makeId(span, api, key);

                    Debug.debug(Debug.Type.SYNTAX, 2, "Qualify ", key, " with name ", qname);
                    if (nonterminals.containsKey(key)) {
                        nonterminals.get(key).add(qname);
                    } else {
                        Set<Id> matches = new HashSet<Id>();
                        matches.add(qname);
                        nonterminals.put(key, matches);
                    }
                }

                @Override
                public void defaultTemplateGap(TemplateGap t) {
                    // This override is necessary because we are disambiguating all APIs
                    // in the repository, on every pass, to ensure that all APis referred to
                    // have unambiguous names during type checking. 
                }
            });
        }
    }

    /*
    private void initializeNonterminals() {
        initializeForGrammar(this.getGrammarIndex());
        for ( GrammarIndex grammar : this.getGrammarIndex().getExtended() ){
            initializeForGrammar(grammar);
        }
    }
    */

    private void initializeForGrammar(GrammarIndex grammar) {
        for (NonterminalIndex e : grammar.getDeclaredNonterminals()) {
            GrammarDecl currentGrammar = grammar.ast();

            Span span = NodeUtil.getSpan(e.getName());
            String key = e.getName().getText();
            APIName api = constructNonterminalApi(currentGrammar.getName());
            Id qname = NodeFactory.makeId(span, api, key);

            Debug.debug(Debug.Type.SYNTAX, 2, "Ambigious key ", key, " disambiguates to ", qname);
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
     *
     * @param grammarName
     * @return
     */
    private APIName constructNonterminalApi(Id grammarName) {
        Option<APIName> optApi = grammarName.getApiName();
        APIName api;
        if (optApi.isSome()) api = optApi.unwrap();
        else api = bug("NonterminalEnv.constructNonterminalApi failed! " + grammarName + " was unqualified.");
        List<Id> ls = new LinkedList<Id>();
        ls.addAll(api.getIds());
        ls.add(NodeFactory.makeId(NodeUtil.getSpan(grammarName), grammarName.getText()));
        return NodeFactory.makeAPIName(NodeUtil.getSpan(grammarName), ls);
    }

    /**
     * Given a disambiguated name (aliases and imports have been resolved),
     * determine whether a nonterminal exists.  Assumes {@code name.getApiName().isSome()}.
     */
    public boolean hasQualifiedNonterminal(Id name) {
        Debug.debug(Debug.Type.SYNTAX, 3, "Find a qualified id for non-terminal ", name);
        Option<APIName> optApi = name.getApiName();
        if (optApi.isNone()) bug(name,
                                 "A qualified identifier is supposed to have an API name, but the api is not present");
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

    /**
     * Determine whether a nonterminal with the given name is defined.
     * We assume that the given name is unqualified
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
     *
     * @param an unqualified nonterminal name
     */
    public Set<Id> declaredNonterminalNames(String name) {
        //GrammarIndex grammar = this.getGrammarIndex();
        Set<Id> results = new HashSet<Id>();
        if (this.nonterminals.containsKey(name)) {
            return this.nonterminals.get(name);
        } else {
            return results;
        }
    }

    /**
     * Produce the set of inherited qualified names corresponding to the given
     * nonterminal name. If the name is not declared in any extended grammar
     * an empty set is produced, and an ambiguous reference produces a set
     * of size greater than 1.
     *
     * @param an unqualified nonterminal name
     */
    public Set<Id> inheritedNonterminalNames(String name) {
        Debug.debug(Debug.Type.SYNTAX, 3, "Is ", name, " inherited?");
        Set<Id> results = new HashSet<Id>();
        for (GrammarIndex extend : this.getGrammarIndex().getExtended()) {
            results.addAll(new NonterminalEnv(extend).declaredNonterminalNames(name));
        }
        return results;
    }

    private Id getGrammarNameFromLastIdInAPI(APIName name) {
        return name.getIds().get(name.getIds().size() - 1);
    }

    private APIName getApi(APIName name) {
        Span span = NodeUtil.getSpan(name);
        if (name.getIds().size() <= 1) {
            return NodeFactory.makeAPIName(span, new LinkedList<Id>());
        }
        List<Id> ids = new LinkedList<Id>();
        ids.addAll(name.getIds());
        ids.remove(ids.size() - 1);
        return NodeFactory.makeAPIName(span, ids);
    }

}
