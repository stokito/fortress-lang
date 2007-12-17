/*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.StaticError;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.compiler.index.ProductionIndex;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.GrammarDecl;
import com.sun.fortress.nodes.GrammarDef;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdName;
import com.sun.fortress.nodes.ProductionDef;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;

import edu.rice.cs.plt.tuple.Option;

public class ProductionEnv {

	private TypeNameEnv _typeEnv;
	private GrammarIndex _current;
	private List<StaticError> _errors;

	private Map<IdName, Set<QualifiedIdName>> _productions = new HashMap<IdName, Set<QualifiedIdName>>();

	public ProductionEnv(TypeNameEnv typeEnv, GrammarIndex currentGrammar, List<StaticError> errors) {
		_typeEnv = typeEnv;
		_current = currentGrammar;
		_errors = errors;
		initializeProductions();
	}

	private void initializeProductions() {
		for (Map.Entry<QualifiedIdName,ProductionIndex> e: _current.productions().entrySet()) {
			IdName key = e.getKey().getName();
			GrammarDecl currentGrammar = Option.unwrap(_current.ast());
			IdName name = NodeFactory.makeIdName(currentGrammar.getName().stringName()+"."+key.getId().stringName());
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
		APIName api = getApi(name);
		Option<APIName> realApi = _typeEnv.apiName(api);
		Id grammarName = getGrammar(name);
		GrammarDecl currentGrammar = Option.unwrap(_current.ast());
		if (currentGrammar.getName().getName().equals(grammarName) &&
			realApi.isSome()) {
			Collection<Id> ids = new LinkedList<Id>();
			ids.addAll(Option.unwrap(realApi).getIds());
			ids.add(grammarName);
			return Option.some(NodeFactory.makeAPIName(ids));
		}
		return Option.none();
	}

	private Id getGrammar(APIName name) {
		return name.getIds().get(name.getIds().size()-1);
	}

	private APIName getApi(APIName name) {
		return NodeFactory.makeAPIName(name.getIds().remove(name.getIds().size()));
	}

    /**
     * Given a disambiguated name (aliases and imports have been resolved),
     * determine whether a production exists.  Assumes {@code name.getApi().isSome()}.
     */
	public boolean hasQualifiedProduction(QualifiedIdName name) {
        APIName api = getApi(Option.unwrap(name.getApi()));
        IdName gname = NodeFactory.makeIdName(getGrammar(Option.unwrap(name.getApi())));
        QualifiedIdName grammarName = NodeFactory.makeQualifiedIdName(api, gname);
        if (this._typeEnv.hasQualifiedGrammar(grammarName)) {
            return this._current.productions().containsKey(name.getName());
        }
        else { return false; }
		
	}

	/** Determine whether a production with the given name is defined. */
	public boolean hasProduction(QualifiedIdName name) {
		if (_current.productions().containsKey(name)) {
        	return true;            
        }
        return false;
	}

    /**
     * Produce the set of qualified names corresponding to the given
     * production name.  An undefined reference produces an empty set, and 
     * an ambiguous reference produces a set of size greater
     * than 1.
     */
	public Set<QualifiedIdName> explicitProductionNames(QualifiedIdName name) {
        return explicitProductionNames(_current, name);
	}
	
	private Set<QualifiedIdName> explicitProductionNames(GrammarIndex grammar, QualifiedIdName name) {
		if (grammar.productions().containsKey(name)) {
			if (grammar.ast().isSome()) {
				QualifiedIdName gname = Option.unwrap(grammar.ast()).getName();
				return Collections.singleton(qualifyProductionName(Option.unwrap(gname.getApi()), gname.getName().getId(), name.getName()));
			}
		}
        return Collections.emptySet();
	}
	
	private QualifiedIdName qualifyProductionName(APIName api, Id grammarName, IdName productionName) {
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
		Set<QualifiedIdName> rs = new HashSet<QualifiedIdName>();
		for (GrammarIndex g: _current.getExtendedGrammars()) {
			if (g.ast().isSome()) {
				QualifiedIdName gname = Option.unwrap(g.ast()).getName();
				QualifiedIdName n = qualifyProductionName(Option.unwrap(gname.getApi()), gname.getName().getId(), name.getName());
				rs.addAll(explicitProductionNames(g, n));
			}
		}
        return rs;
	}
}
