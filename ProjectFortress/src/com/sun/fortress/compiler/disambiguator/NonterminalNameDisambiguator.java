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

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.StaticError;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.HasAt;

import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;

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
	 * Given a name Foo.Bar.Baz iterate though the set of {Foo, Bar, Baz} and
	 * construct each of the prefixes: Foo, Foo.Bar, Foo.Bar.Baz.
	 * For each of these prefix check if it is an API, if not proceed with the
	 * next. If it is the next element must be a grammar.
	 * E.g. if Foo.Bar is an API, then return the API name Foo.Bar.Baz.
	 * Return none if no API is found and some if an API is found.
	 * @param name
	 * @return
	 * TODO: we don't check for the case if an API exists which name is a prefix of 
	 * the intended name.
	 */
	public Option<APIName> grammarName(APIName name) {
		List<Id> ids = new LinkedList<Id>();
		Iterator<Id> it = name.getIds().iterator();
		boolean foundApi = false;
		while (it.hasNext() && !foundApi) {
			ids.add(it.next());
			boolean realApi = _globalEnv.definesApi(NodeFactory.makeAPIName(ids));
			if (realApi) {
				foundApi = true;
			}
		}
		if (!foundApi || !it.hasNext()) {
			return Option.none();
		}
		Id grammarName = it.next();
		Collection<Id> aids = new LinkedList<Id>();
		aids.addAll(ids);
		aids.add(grammarName);
		return Option.some(NodeFactory.makeAPIName(aids));
	}
	
	/**
	 * Disambiguate the given nonterminal name against the given nonterminal environment.
	 * If reportNonterminalErrors is false, don't report errors relating to nonterminals
	 * @param currentEnv
	 * @param name
	 * @return
	 */
	public Option<QualifiedIdName> handleNonterminalName(NonterminalEnv currentEnv, QualifiedIdName name) {
		// If it is already fully qualified
		if (name.getApi().isSome()) {
			APIName originalApiGrammar = Option.unwrap(name.getApi());
			Option<APIName> realApiGrammarOpt = this.grammarName(originalApiGrammar);
			// Check that the qualifying part is a real grammar 
			if (realApiGrammarOpt.isNone()) {
				error("Undefined grammar: " + NodeUtil.nameString(originalApiGrammar) +" obtained from "+name, originalApiGrammar);
				return Option.none();
			}
			APIName realApiGrammar = Option.unwrap(realApiGrammarOpt);
			QualifiedIdName newN;
			if (originalApiGrammar == realApiGrammar) { newN = name; }
			else { newN = NodeFactory.makeQualifiedIdName(realApiGrammar, name.getName()); }

			if (!currentEnv.hasQualifiedNonterminal(newN)) {
				error("Undefined nonterminal: " + NodeUtil.nameString(newN), newN);
				return Option.none();
			}
			return Option.some(newN);
		}
		else { // Unqualified name
			// Is it defined in the current grammar?
			if (currentEnv.hasNonterminal(name.getName())) {
				Set<QualifiedIdName> names = currentEnv.declaredNonterminalNames(name.getName());
				if (names.size() > 1) {
					error("Nonterminal name may refer to: " + NodeUtil.namesString(names), name);
					return Option.none();
				}
				if (names.isEmpty()) {
					error("Internal error we know the nonterminal is there but can't see it: " + name, name);
					return Option.none();
				}
				QualifiedIdName qname = IterUtil.first(names);
				return Option.some(qname);
			}
			else {
				Set<QualifiedIdName> names = currentEnv.declaredNonterminalNames(name.getName());
				// If the nonterminal is not defined in the current grammar then look
				// among the inherited nonterminal names
				if (names.isEmpty()) {
					names = currentEnv.inheritedNonterminalNames(name.getName());
				}

				// if not there it is undefined
				if (names.isEmpty()) {
					error("Undefined nonterminal: " + NodeUtil.nameString(name), name);
					return Option.none();
				}

				// If too many are found we are not sure which one is the right...
				if (names.size() > 1) {
					error("Nonterminal name may refer to: " + NodeUtil.namesString(names), name);
					return Option.none();
				}
				QualifiedIdName qname = IterUtil.first(names); 
				return Option.some(qname);
			}
		}
	}	

}

