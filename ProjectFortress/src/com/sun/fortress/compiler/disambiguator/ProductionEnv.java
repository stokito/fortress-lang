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

import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.StaticError;
import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.compiler.index.ProductionIndex;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.GrammarDecl;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.NonterminalDecl;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.syntax_abstractions.phases.GrammarAnalyzer;
import com.sun.fortress.syntax_abstractions.util.SyntaxAbstractionUtil;

import edu.rice.cs.plt.tuple.Option;

/**
 * 	This production environment is used during disambiguation
 *  The production environment has separate access to the production names 
 *  declared in the current grammar (using explicitProductionNames) and 
 *  to those inherited from extended grammars (using inheritedProductionsNames()).
 *  The name should not be qualified. 
 */
public class ProductionEnv {

	private GlobalEnvironment _globalEnv;
	private GrammarIndex _current;
	private Map<Id, Set<QualifiedIdName>> _productions = new HashMap<Id, Set<QualifiedIdName>>();
	
	public ProductionEnv(GlobalEnvironment env, GrammarIndex currentGrammar, List<StaticError> errors) {
		_globalEnv = env;
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
	
	/**
	 * Initialize the mapping from production names to sets of qualified production names
	 */
	private void initializeProductions() {
		for (ProductionIndex<? extends NonterminalDecl> e: this.getGrammarIndex().productions()) {
			Id key = e.getName().getName();
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

    /**
     * Given a disambiguated name (aliases and imports have been resolved),
     * determine whether a production exists.  Assumes {@code name.getApi().isSome()}.
     */
	public boolean hasQualifiedProduction(QualifiedIdName name) {
		APIName api = getApi(Option.unwrap(name.getApi()));
        Id gname = getGrammar(Option.unwrap(name.getApi()));
        QualifiedIdName grammarName = NodeFactory.makeQualifiedIdName(api, gname);
        
        if (grammarName.equals(this._current.getName())) {
            	Set<QualifiedIdName> names = this.declaredProductionNames(name.getName());
            	return !names.isEmpty();
        }
        return false;
	}

	/** Determine whether a production with the given name is defined. */
	public boolean hasProduction(Id name) {
		if (this._productions.containsKey(name)) {
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
	public Set<QualifiedIdName> declaredProductionNames(Id name) {
        return declaredProductionNames(this.getGrammarIndex(), name);
	}

    // TODO are more than one ever returned?
	/**
	 * Name May be qualified or unqualified. If qualified it must be in the grammar specified. 
	 */
	private Set<QualifiedIdName> declaredProductionNames(GrammarIndex grammar, Id name) {
		Set<QualifiedIdName> results = new HashSet<QualifiedIdName>();
		if (this._productions.containsKey(name)) {
			if (grammar.ast().isSome()) {
				QualifiedIdName gname = Option.unwrap(grammar.ast()).getName();
				APIName gApi = Option.unwrap(gname.getApi());
				results.addAll(Collections.singleton(SyntaxAbstractionUtil.qualifyProductionName(gApi , gname.getName(), name)));				
			}
		}
		
//		QualifiedIdName qualifiedName = qualifyProductionName(Option.unwrap(Option.unwrap(grammar.ast()).getName().getApi()), Option.unwrap(grammar.ast()).getName().getName(), name);
//		if (this._productions.containsKey(qualifiedName)) {
//			if (grammar.ast().isSome()) {
//				QualifiedIdName gname = Option.unwrap(grammar.ast()).getName();
//				APIName gApi = Option.unwrap(gname.getApi());
//				results.addAll(Collections.singleton(qualifyProductionName(gApi , gname.getName(), name)));
//			}
//		}
	    return results;
	}

	public Set<QualifiedIdName> inheritedProductionNames(Id name) {
		GrammarAnalyzer ga = new GrammarAnalyzer();
		Set<QualifiedIdName> results = ga.getInherited(name, this._current);
		return results;
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

}
