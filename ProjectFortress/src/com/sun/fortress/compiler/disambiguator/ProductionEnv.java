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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.sun.fortress.compiler.StaticError;
import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.compiler.index.ProductionIndex;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.GrammarDecl;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.NonterminalDecl;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.nodes_util.NodeFactory;

import edu.rice.cs.plt.tuple.Option;

/**
 * 	This production environment is used during disambiguation
 *  The production environment has separate access to the production names 
 *  declared in the current grammar (using explicitProductionNames) and 
 *  to those inherited from extended grammars (using inheritedProductionsNames()).
 *  The name should not be qualified. 
 */
public class ProductionEnv {

	private TypeNameEnv _typeEnv;
	private GrammarIndex _current;
	private Map<Id, Set<QualifiedIdName>> _productions = new HashMap<Id, Set<QualifiedIdName>>();
	
	public ProductionEnv(TypeNameEnv typeEnv, GrammarIndex currentGrammar, List<StaticError> errors) {
		_typeEnv = typeEnv;
		this._current = currentGrammar;
		initializeProductions();
	}
	
	public ProductionEnv(GrammarIndex currentGrammar) {
		this._current = currentGrammar;
		initializeProductions();
	}

	public GrammarIndex getGrammarIndex() {
		return this._current;
	}
	
	private void initializeProductions() {
		for (Map.Entry<QualifiedIdName,ProductionIndex<? extends NonterminalDecl>> e: this.getGrammarIndex().productions().entrySet()) {
			Id key = e.getKey().getName();
			GrammarDecl currentGrammar = Option.unwrap(this.getGrammarIndex().ast());
			Id name = NodeFactory.makeId(currentGrammar.getName().stringName()+"."+key.stringName());
			if (_productions.containsKey(key)) {
				_productions.get(key).add(new QualifiedIdName(key.getSpan(),
						currentGrammar.getName().getApi(),
						name));

			} else {
				Set<QualifiedIdName> matches = new HashSet<QualifiedIdName>();
				matches.add(new QualifiedIdName(key.getSpan(),
						currentGrammar.getName().getApi(),
						name));
				_productions.put(key, matches);
			}
		}
	}

	public Option<APIName> grammarName(APIName name) {
		List<Id> ids = new LinkedList<Id>();
		Iterator<Id> it = name.getIds().iterator();
		boolean foundApi = false;
		while (it.hasNext() && !foundApi) {
			ids.add(it.next());
			Option<APIName> realApi = _typeEnv.apiName(NodeFactory.makeAPIName(ids));
			if (realApi.isSome()) {
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

	private Id getGrammar(APIName name) {
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

    /**
     * Given a disambiguated name (aliases and imports have been resolved),
     * determine whether a production exists.  Assumes {@code name.getApi().isSome()}.
     */
	public boolean hasQualifiedProduction(QualifiedIdName name) {
		APIName api = getApi(Option.unwrap(name.getApi()));
        Id gname = getGrammar(Option.unwrap(name.getApi()));
        QualifiedIdName grammarName = NodeFactory.makeQualifiedIdName(api, gname);
        if (this._typeEnv.hasQualifiedGrammar(grammarName)) {
        	Set<QualifiedIdName> names = this.declaredProductionNames(name);
        	if (names.isEmpty()) {
        		names = this.inheritedProductionNames(name);
        	}
        	return !names.isEmpty();
        }
        else { return false; }

	}

	/** Determine whether a production with the given name is defined. */
	public boolean hasProduction(QualifiedIdName name) {
		if (this.getGrammarIndex().productions().containsKey(name)) {
        	return true;
        }
        return false;
	}

	/**
     * Produce the set of qualified names corresponding to the given
     * production name.  If the name is not declared in the current grammar
     * an empty set is produced, and an ambiguous reference produces a set 
     * of size greater than 1.
     */
	public Set<QualifiedIdName> declaredProductionNames(QualifiedIdName name) {
        return declaredProductionNames(this.getGrammarIndex(), name);
	}

    // TODO are more than one ever returned?
	/**
	 * Name May be qualified or unqualified. If qualified it must be in the grammar specified. 
	 */
	private Set<QualifiedIdName> declaredProductionNames(GrammarIndex grammar, QualifiedIdName name) {
		Set<QualifiedIdName> results = new HashSet<QualifiedIdName>();
		QualifiedIdName unQualifiedName = NodeFactory.makeQualifiedIdName(name.getSpan(), name.getName());
		if (grammar.productions().containsKey(unQualifiedName)) {
			if (grammar.ast().isSome()) {
				QualifiedIdName gname = Option.unwrap(grammar.ast()).getName();
				APIName gApi = Option.unwrap(gname.getApi());
				Set<QualifiedIdName> collection = Collections.singleton(qualifyProductionName(gApi , gname.getName(), name.getName()));
				if (name.getApi().isSome()) {
					APIName nApi = Option.unwrap(name.getApi());
					if (!nApi.getIds().isEmpty()) {
						if (gApi.equals(nApi)) {
							results.addAll(collection);
						}
					}
				}
				else {
					results.addAll(collection);
				}
			}
		}
		
		QualifiedIdName qualifiedName = qualifyProductionName(Option.unwrap(Option.unwrap(grammar.ast()).getName().getApi()), Option.unwrap(grammar.ast()).getName().getName(), name.getName());
		if (grammar.productions().containsKey(qualifiedName)) {
			if (grammar.ast().isSome()) {
				QualifiedIdName gname = Option.unwrap(grammar.ast()).getName();
				APIName gApi = Option.unwrap(gname.getApi());
				results.addAll(Collections.singleton(qualifyProductionName(gApi , gname.getName(), name.getName())));
			}
		}
	    return results;
	}

	/**
	 * Returns a qualified id name where the grammar name is added to the api.
	 * E.g. api: Foo.Bar, grammar name Baz, and production Gnu gives 
	 * APIName: Foo.Bar.Baz and id: Gnu.
	 */
	private QualifiedIdName qualifyProductionName(APIName api, Id grammarName, Id productionName) {
		Collection<Id> names = new LinkedList<Id>();
		names.addAll(api.getIds());
		names.add(grammarName);
		APIName apiGrammar = NodeFactory.makeAPIName(names);
		return NodeFactory.makeQualifiedIdName(apiGrammar, productionName);
	}

    /**
     * Produce the set of inherited qualified names corresponding to the given
     * production name.  An undefined reference produces an empty set, and
     * an ambiguous reference produces a set of size greater
     * than 1.
     */
	public Set<QualifiedIdName> inheritedProductionNames(QualifiedIdName name) {
//		if (name.getName().getText().equals("Literal")) {
//			System.err.println("Looking for: "+name);
//			System.err.println(" current is: "+Option.unwrap(_current.ast()).getName());
//		}
		Set<QualifiedIdName> rs = new HashSet<QualifiedIdName>();
		for (GrammarIndex g: this.getGrammarIndex().getExtendedGrammars()) {
			if (g.ast().isSome()) {
//				if (name.getName().getText().equals("Literal")) {
//					System.err.println("- in: "+Option.unwrap(g.ast()).getName());
//				}
//				QualifiedIdName gname = Option.unwrap(g.ast()).getName();
//				QualifiedIdName n = null;
//				if (gname.getApi().isSome()) {
//					n = qualifyProductionName(Option.unwrap(gname.getApi()), gname.getName(), name.getName());
//				}
//				else {
//					n = NodeFactory.makeQualifiedIdName(NodeFactory.makeAPIName(gname.getName()), name.getName());
//				}
				rs.addAll(declaredProductionNames(g, name));
			}
		}
//		if (name.getName().getText().equals("Literal")) {
//			System.err.println("found it: "+!rs.isEmpty());
//		}
        return rs;
	}
	
	private Set<QualifiedIdName> allInheritedProductionNames() {
		Set<QualifiedIdName> rs = new HashSet<QualifiedIdName>();
		for (GrammarIndex g: this.getGrammarIndex().getExtendedGrammars()) {
			rs.addAll(g.productions().keySet());
		}
        return rs;
	}
	
	public String toString() {
		Set<QualifiedIdName> productions = new HashSet<QualifiedIdName>();
		productions.addAll(allInheritedProductionNames());
		productions.addAll(allDeclaredProductionNames());
		return productions.toString();
	}

	private Set<QualifiedIdName> allDeclaredProductionNames() {
		Set<QualifiedIdName> rs = new HashSet<QualifiedIdName>();
		rs.addAll(this.getGrammarIndex().productions().keySet());
        return rs;
	}
	
	/**
     * Produce the set of inherited production index's with the given name.
     * An undefined name produces an empty set, and an ambiguous name 
     * produces a set of size greater than 1.
     */
	public Set<ProductionIndex<? extends NonterminalDecl>> getExtendedNonterminal(Id name) {
		Set<ProductionIndex<? extends NonterminalDecl>> rs = new HashSet<ProductionIndex<? extends NonterminalDecl>>();
		for (GrammarIndex g: this.getGrammarIndex().getExtendedGrammars()) {
			for (Entry<QualifiedIdName, ProductionIndex<? extends NonterminalDecl>> entry: g.productions().entrySet()) {
				if (entry.getKey().getName().equals(name)) {
					rs.add(entry.getValue());
				}
			}
		}
        return rs;
	}
	
}
