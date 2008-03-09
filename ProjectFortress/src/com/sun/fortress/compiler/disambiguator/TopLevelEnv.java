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

import java.util.*;

import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.OptionVisitor;

import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.StaticError;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.Dimension;
import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.compiler.index.TypeConsIndex;
import com.sun.fortress.compiler.index.CompilationUnitIndex;
import com.sun.fortress.compiler.index.Unit;
import com.sun.fortress.compiler.index.Variable;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeFactory;

public class TopLevelEnv extends NameEnv {
    private GlobalEnvironment _globalEnv;
    private CompilationUnitIndex _current;
    private List<StaticError> _errors;

    private Map<APIName, ApiIndex> _onDemandImportedApis = new HashMap<APIName, ApiIndex>();
    private Map<Id, Set<QualifiedIdName>> _onDemandTypeConsNames = new HashMap<Id, Set<QualifiedIdName>>();
    private Map<Id, Set<QualifiedIdName>> _onDemandVariableNames = new HashMap<Id, Set<QualifiedIdName>>();
    private Map<Id, Set<QualifiedIdName>> _onDemandFunctionIdNames = new HashMap<Id, Set<QualifiedIdName>>();
    private Map<OpName, Set<QualifiedOpName>> _onDemandFunctionOpNames = new HashMap<OpName, Set<QualifiedOpName>>();
    private Map<Id, Set<QualifiedIdName>> _onDemandGrammarNames = new HashMap<Id, Set<QualifiedIdName>>();

    private static class TypeIndex {
        private APIName _api;
        private TypeConsIndex _typeCons;

        TypeIndex(APIName api, TypeConsIndex typeCons) {
            _api = api;
            typeCons = _typeCons;
        }
        public APIName api() { return _api; }
        public TypeConsIndex typeCons() { return _typeCons; }
    }

    public TopLevelEnv(GlobalEnvironment globalEnv, CompilationUnitIndex current, List<StaticError> errors) {
        _globalEnv = globalEnv;
        _current = current;
        _errors = errors;
        initializeOnDemandImportedApis();
        initializeOnDemandTypeConsNames();
        initializeOnDemandVariableNames();
        initializeOnDemandFunctionNames();
        initializeOnDemandGrammarNames();
    }

    /**
     * Initializes the map of imported API names to ApiIndices.
     * For now, all imports are assumed to be on-demand imports.
     */
    private void initializeOnDemandImportedApis() {
        // TODO: Fix to support other kinds of imports.
        Set<APIName> imports = _current.imports();


        // The following APIs are always imported, provided they exist.
        for (APIName api : implicitlyImportedApis()) {
            addIfAvailableApi(api, false);
        }

        for (APIName name: imports) {
            addIfAvailableApi(name, true);
        }
    }

    private void addIfAvailableApi(APIName name, boolean errorIfUnavailable) {
        Map<APIName, ApiIndex> availableApis = _globalEnv.apis();

        if (availableApis.containsKey(name)) {
            _onDemandImportedApis.put(name, availableApis.get(name));
        }
        else if (errorIfUnavailable) {
            _errors.add(StaticError.make("Attempt to import an API not in the repository: " + name.getIds(),
                                        name.getSpan().toString()));
        }

    }

    private <T> void initializeEntry(Map.Entry<APIName, ApiIndex> apiEntry,
                                     Map.Entry<Id, T> entry,
                                     Map<Id, Set<QualifiedIdName>> table) {
        Id key = entry.getKey();
        if (table.containsKey(key)) {
            table.get(key).add(new QualifiedIdName(key.getSpan(),
                                                   Option.some(apiEntry.getKey()),
                                                   key));

        } else {
            Set<QualifiedIdName> matches = new HashSet<QualifiedIdName>();
            matches.add(new QualifiedIdName(key.getSpan(),
                                            Option.some(apiEntry.getKey()),
                                            key));
            table.put(key, matches);
        }
    }

    private void initializeOnDemandTypeConsNames() {
        // For now, we support only on demand imports.
        // TODO: Fix to support explicit imports and api imports.

        for (Map.Entry<APIName, ApiIndex> apiEntry: _onDemandImportedApis.entrySet()) {
            for (Map.Entry<Id, TypeConsIndex> typeEntry: apiEntry.getValue().typeConses().entrySet()) {
                initializeEntry(apiEntry, typeEntry, _onDemandTypeConsNames);
            }
            // Inject the names of physical units into the set of type cons names.
            for (Map.Entry<Id, Unit> unitEntry: apiEntry.getValue().units().entrySet()) {
                initializeEntry(apiEntry, unitEntry, _onDemandTypeConsNames);
            }
            // Inject the names of physical dimensions into the set of type cons names.
            for (Map.Entry<Id, Dimension> dimEntry: apiEntry.getValue().dimensions().entrySet()) {
                initializeEntry(apiEntry, dimEntry, _onDemandTypeConsNames);
            }
        }
    }

    private void initializeOnDemandVariableNames() {
        for (Map.Entry<APIName, ApiIndex> apiEntry: _onDemandImportedApis.entrySet()) {
            for (Map.Entry<Id, Variable> varEntry: apiEntry.getValue().variables().entrySet()) {
                initializeEntry(apiEntry, varEntry, _onDemandVariableNames);
            }
            // Inject the names of physical units into the set of bound variables.
            for (Map.Entry<Id, Unit> unitEntry: apiEntry.getValue().units().entrySet()) {
                initializeEntry(apiEntry, unitEntry, _onDemandVariableNames);
            }
        }
    }

    private void initializeOnDemandFunctionNames() {
        for (Map.Entry<APIName, ApiIndex> apiEntry: _onDemandImportedApis.entrySet()) {
            for (SimpleName fnName: apiEntry.getValue().functions().firstSet()) {
                if (fnName instanceof Id) {
                    Id _fnName = (Id)fnName;
                    QualifiedIdName name = new QualifiedIdName(_fnName.getSpan(),
                                                               Option.some(apiEntry.getKey()),
                                                               _fnName);
                    if (_onDemandFunctionIdNames.containsKey(_fnName)) {
                        _onDemandFunctionIdNames.get(_fnName).add(name);
                    } else {
                        Set<QualifiedIdName> matches = new HashSet<QualifiedIdName>();
                        matches.add(name);
                        _onDemandFunctionIdNames.put(_fnName, matches);
                    }
                } else { // fnName instanceof OpName
                    OpName _fnName = (OpName)fnName;
                    QualifiedOpName name = new QualifiedOpName(_fnName.getSpan(),
                                                             Option.some(apiEntry.getKey()),
                                                             _fnName);
                    if (_onDemandFunctionOpNames.containsKey(_fnName)) {
                        _onDemandFunctionOpNames.get(_fnName).add(name);
                    } else {
                        Set<QualifiedOpName> matches = new HashSet<QualifiedOpName>();
                        matches.add(name);
                        _onDemandFunctionOpNames.put(_fnName, matches);
                    }
                }
            }
        }
    }

    private void initializeOnDemandGrammarNames() {
        for (Map.Entry<APIName, ApiIndex> apiEntry: _onDemandImportedApis.entrySet()) {
        	for (Map.Entry<QualifiedIdName, GrammarIndex> grammarEntry: apiEntry.getValue().grammars().entrySet()) {
        		Id key = grammarEntry.getKey().getName();
                if (_onDemandGrammarNames.containsKey(key)) {
                	_onDemandGrammarNames.get(key).add(new QualifiedIdName(key.getSpan(),
                                                           Option.some(apiEntry.getKey()),
                                                           key));

                } else {
                    Set<QualifiedIdName> matches = new HashSet<QualifiedIdName>();
                    matches.add(new QualifiedIdName(key.getSpan(),
                                                    Option.some(apiEntry.getKey()),
                                                    key));
                    _onDemandGrammarNames.put(key, matches);
                }
            }
        }
    }

    public Option<APIName> apiName(APIName name) {
        // TODO: Handle aliases.
        if (_onDemandImportedApis.containsKey(name)) {
            return Option.some(name);
        } else {
            return Option.none();
        }
    }

    public boolean hasTypeParam(Id name) {
        return false;
    }

	@Override
	public boolean hasGrammar(QualifiedIdName name) {
        if (_current instanceof ApiIndex) {
        	if (((ApiIndex) _current).grammars().containsKey(name)) {
        		return true;
        	}
        }
        return false;
	}


    public Set<QualifiedIdName> explicitTypeConsNames(Id name) {
        // TODO: imports
        if (_current.typeConses().containsKey(name) ||
            _current.dimensions().containsKey(name) ||
            _current.units().containsKey(name)) {
            return Collections.singleton(NodeFactory.makeQualifiedIdName(_current.ast().getName(), name));
        }
        else { return Collections.emptySet(); }
    }

    public Set<QualifiedIdName> explicitVariableNames(Id name) {
        // TODO: imports
        if (_current.variables().containsKey(name) ||
            _current.units().containsKey(name)) {
            return Collections.singleton(NodeFactory.makeQualifiedIdName(_current.ast().getName(), name));
        }
        else { return Collections.emptySet(); }
    }

    public Set<QualifiedIdName> explicitFunctionNames(Id name) {
        // TODO: imports
        if (_current.functions().containsFirst(name)) {
            return Collections.singleton(NodeFactory.makeQualifiedIdName(_current.ast().getName(), name));
        }
        else { return Collections.emptySet(); }
    }

    public Set<QualifiedOpName> explicitFunctionNames(OpName name) {
        // TODO: imports
        if (_current.functions().containsFirst(name)) {
            return Collections.singleton(NodeFactory.makeQualifiedOpName(_current.ast().getName(), name));
        }
        else { return Collections.emptySet(); }
    }

	@Override
	public Set<QualifiedIdName> explicitGrammarNames(QualifiedIdName name) {
        // TODO: imports
		if (_current instanceof ApiIndex) {
			QualifiedIdName lookupName = NodeFactory.makeQualifiedIdName(name.getName());
			if (((ApiIndex)_current).grammars().containsKey(lookupName)) {
				APIName api = ((ApiIndex)_current).ast().getName();
				return Collections.singleton(NodeFactory.makeQualifiedIdName(api, lookupName.getName()));
			}
		}
        return Collections.emptySet();
	}

    private Set<QualifiedIdName> onDemandNames(Id name, Map<Id, Set<QualifiedIdName>> table)
    {
        if (table.containsKey(name)) {
            return table.get(name);
        } else {
            return new HashSet<QualifiedIdName>();
        }
    }

    public Set<QualifiedIdName> onDemandTypeConsNames(Id name) {
        return onDemandNames(name, _onDemandTypeConsNames);
    }

    public Set<QualifiedIdName> onDemandVariableNames(Id name) {
        return onDemandNames(name, _onDemandVariableNames);
    }

    public Set<QualifiedIdName> onDemandFunctionNames(Id name) {
        if (_onDemandFunctionIdNames.containsKey(name)) {
            return _onDemandFunctionIdNames.get(name);
        } else {
            return new HashSet<QualifiedIdName>();
        }
    }

    public Set<QualifiedOpName> onDemandFunctionNames(OpName name) {
        if (_onDemandFunctionOpNames.containsKey(name)) {
            return _onDemandFunctionOpNames.get(name);
        } else {
            return new HashSet<QualifiedOpName>();
        }
    }

    public Set<QualifiedIdName> onDemandGrammarNames(Id name) {
        if (_onDemandGrammarNames.containsKey(name)) {
            return _onDemandGrammarNames.get(name);
        } else {
            return new HashSet<QualifiedIdName>();
        }
    }

    public boolean hasQualifiedTypeCons(QualifiedIdName name) {
        APIName api = Option.unwrap(name.getApi());
        if (_globalEnv.definesApi(api)) {
            return _globalEnv.api(api).typeConses().containsKey(name);
        }
        else { return false; }
    }

    public boolean hasQualifiedVariable(QualifiedIdName name) {
        APIName api = Option.unwrap(name.getApi());
        if (_globalEnv.definesApi(api)) {
            return _globalEnv.api(api).variables().containsKey(name.getName());
        }
        else { return false; }
    }

    public boolean hasQualifiedFunction(QualifiedIdName name) {
        APIName api = Option.unwrap(name.getApi());
        if (_globalEnv.definesApi(api)) {
            return _globalEnv.api(api).functions().containsFirst(name.getName());
        }
        else { return false; }
    }

    public boolean hasQualifiedGrammar(QualifiedIdName name) {
        APIName api = Option.unwrap(name.getApi());
        if (_globalEnv.definesApi(api)) {
            return _globalEnv.api(api).grammars().containsKey(name.getName());
        }
        else { return false; }
    }

    public TypeConsIndex typeConsIndex(final QualifiedIdName name) {
        Option<APIName> api = name.getApi();
        // If no API in name or it's the current API, use its own typeCons.
        // Otherwise, try to find the API in the global env and use its typeCons.
        if (api.isNone() || _current.ast().getName().equals(Option.unwrap(api))) {
            return _current.typeConses().get(name.getName());
        } else {
            return _globalEnv.api(Option.unwrap(api)).typeConses().get(name.getName());
        }
    }

    public Option<GrammarIndex> grammarIndex(final QualifiedIdName name) {
        QualifiedIdName lookupName = NodeFactory.makeQualifiedIdName(name.getName());
		if (name.getApi().isSome()) {
        	APIName n = Option.unwrap(name.getApi());
        	if (_globalEnv.definesApi(n)) {
        		return Option.some(_globalEnv.api(n).grammars().get(lookupName));
        	}
        	else {
        		return Option.none();
        	}
        }
        if (_current instanceof ApiIndex) {
        	return Option.some(((ApiIndex) _current).grammars().get(lookupName));
        }
        else {
        	_errors.add(StaticError.make("Attempt to get grammar definition from a component: " + name,
                name.getSpan().toString()));
        	return Option.none();
        }
    }
}
