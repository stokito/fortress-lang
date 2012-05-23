/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.disambiguator;

import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.useful.Debug;
import com.sun.fortress.useful.HasAt;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;

import java.util.*;

public class NonterminalNameDisambiguator {

    private List<StaticError> _errors;
    private GlobalEnvironment _globalEnv;

    public NonterminalNameDisambiguator(GlobalEnvironment env) {
        this._errors = new LinkedList<StaticError>();
        this._globalEnv = env;
    }

    private void error(String msg, HasAt loc) {
        this._errors.add(StaticError.make(msg, loc));
    }

    public List<StaticError> errors() {
        return this._errors;
    }

    /**
     * Given a name Foo.Bar.Baz, iterate though the set of {Foo, Bar, Baz} and
     * construct each of the prefixes: Foo, Foo.Bar, Foo.Bar.Baz.
     * For each of these prefixes, check if it is an API, if not proceed with
     * the next.  If it is, the next element must be a grammar.
     * E.g. if Foo.Bar is an API, then return the API name Foo.Bar.Baz.
     * Return none if no API is found and some if an API is found.
     *
     * @param name
     * @return TODO: we don't check for the case if an API exists which name is a prefix of
     *         the intended name.
     */
    private Option<APIName> grammarName(APIName name) {
        List<Id> ids = new LinkedList<Id>();
        Iterator<Id> it = name.getIds().iterator();
        boolean foundApi = false;
        Span span = NodeFactory.macroSpan;
        while (it.hasNext() && !foundApi) {
            ids.add(it.next());
            foundApi = _globalEnv.definesApi(NodeFactory.makeAPIName(span, ids));
        }
        if (!it.hasNext()) {
            return Option.none();
        }
        Id grammarName = it.next();
        Collection<Id> aids = new LinkedList<Id>();
        aids.addAll(ids);
        aids.add(grammarName);
        return Option.some(NodeFactory.makeAPIName(span, aids));
    }

    /**
     * Disambiguate the given nonterminal name against
     * the given nonterminal environment.
     *
     * @param currentEnv
     * @param name
     * @return
     */
    public Option<Id> handleNonterminalName(NonterminalEnv currentEnv, Id name) {
        // If it is already fully qualified
        if (name.getApiName().isSome()) {
            APIName originalApiGrammar = name.getApiName().unwrap();
            Option<APIName> realApiGrammarOpt = this.grammarName(originalApiGrammar);
            // Check that the qualifying part is a real grammar
            if (realApiGrammarOpt.isNone()) {
                error("Undefined grammar: " + NodeUtil.nameString(originalApiGrammar) + " obtained from " + name,
                      originalApiGrammar);
                return Option.none();
            }
            APIName realApiGrammar = realApiGrammarOpt.unwrap();
            Id newN;
            if (originalApiGrammar == realApiGrammar) {
                newN = name;
            } else {
                newN = NodeFactory.makeId(realApiGrammar, name);
            }
            if (!currentEnv.hasQualifiedNonterminal(newN)) {
                error("Undefined qualified nonterminal: " + NodeUtil.nameString(newN), newN);
                return Option.none();
            }
            return Option.some(newN);
        } else { // Unqualified name
            String uqname = name.getText();
            Set<Id> names = currentEnv.declaredNonterminalNames(uqname);
            // Is it defined in the current grammar?
            if (1 == names.size()) {
                Id qname = IterUtil.first(names);
                return Option.some(qname);
            }
            // If the nonterminal is not defined in the current grammar
            // then look among the inherited nonterminal names
            if (names.isEmpty()) {
                names = currentEnv.inheritedNonterminalNames(uqname);
                // if not there it is undefined
                if (names.isEmpty()) {
                    error("Undefined non-qualified nonterminal: " + uqname, name);
                    return Option.none();
                }
                // If too many are found we are not sure which one is the right...
                if (names.size() > 1) {
                    error("Nonterminal name may refer to: " + NodeUtil.namesString(names), name);
                    return Option.none();
                }
                Debug.debug(Debug.Type.SYNTAX, 4, uqname, " is qualified as ", IterUtil.first(names));
                return Option.some(IterUtil.first(names));
            }
            // names.size() > 1
            error("Nonterminal name may refer to: " + NodeUtil.namesString(names), name);
            return Option.none();
        }
    }
}
