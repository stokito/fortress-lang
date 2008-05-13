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
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.parser_util.FortressUtil;

import static com.sun.fortress.interpreter.evaluator.InterpreterBug.bug;

public class TopLevelEnv extends NameEnv {
    private GlobalEnvironment _globalEnv;
    private CompilationUnitIndex _current;
    private List<StaticError> _errors;

    private Map<APIName, ApiIndex> _onDemandImportedApis = new HashMap<APIName, ApiIndex>();
    private Map<Id, Set<Id>> _onDemandTypeConsNames = new HashMap<Id, Set<Id>>();
    private Map<Id, Set<Id>> _onDemandVariableNames = new HashMap<Id, Set<Id>>();
    private Map<Id, Set<Id>> _onDemandFunctionIdNames = new HashMap<Id, Set<Id>>();
    private Map<OpName, Set<OpName>> _onDemandFunctionOpNames = new HashMap<OpName, Set<OpName>>();
    private Map<String, Set<Id>> _onDemandGrammarNames = new HashMap<String, Set<Id>>();

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
                                     Map<Id, Set<Id>> table) {
        Id key = entry.getKey();
        if (table.containsKey(key)) {
            table.get(key).add(new Id(key.getSpan(),
                                      Option.some(apiEntry.getKey()),
                                      key.getText()));

        } else {
            Set<Id> matches = new HashSet<Id>();
            matches.add(new Id(key.getSpan(),
                               Option.some(apiEntry.getKey()),
                               key.getText()));
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
            for (IdOrOpOrAnonymousName fnName: apiEntry.getValue().functions().firstSet()) {
                if (fnName instanceof Id) {
                    Id _fnName = (Id)fnName;
                    Id name = new Id(_fnName.getSpan(),
                                     Option.some(apiEntry.getKey()),
                                     _fnName.getText());
                    if (_onDemandFunctionIdNames.containsKey(_fnName)) {
                        _onDemandFunctionIdNames.get(_fnName).add(name);
                    } else {
                        Set<Id> matches = new HashSet<Id>();
                        matches.add(name);
                        _onDemandFunctionIdNames.put(_fnName, matches);
                    }
                } else { // fnName instanceof OpName
                    OpName _fnName = (OpName)fnName;
                    if (_onDemandFunctionOpNames.containsKey(_fnName)) {
                        _onDemandFunctionOpNames.get(_fnName).add(_fnName);
                    } else {
                        Set<OpName> matches = new HashSet<OpName>();
                        matches.add(_fnName);
                        _onDemandFunctionOpNames.put(_fnName, matches);
                    }
                }
            }
        }
    }

    private void initializeOnDemandGrammarNames() {
        for (Map.Entry<APIName, ApiIndex> apiEntry: _onDemandImportedApis.entrySet()) {
            for (Map.Entry<String, GrammarIndex> grammarEntry: apiEntry.getValue().grammars().entrySet()) {
                Span span = grammarEntry.getValue().getName().getSpan();
            	String key = grammarEntry.getKey();
            	Id id = NodeFactory.makeId(span, apiEntry.getKey(), key);
                if (_onDemandGrammarNames.containsKey(key)) {
					_onDemandGrammarNames.get(key).add(id);
                } else {
                    Set<Id> matches = new HashSet<Id>();
                    matches.add(id);
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
    public boolean hasGrammar(String name) {
        if (_current instanceof ApiIndex) {
            if (((ApiIndex) _current).grammars().containsKey(name)) {
                return true;
            }
        }
        return false;
    }

    public Set<Id> explicitTypeConsNames(Id name) {
        // TODO: imports
        if (_current.typeConses().containsKey(name) ||
            _current.dimensions().containsKey(name) ||
            _current.units().containsKey(name)) {
            return Collections.singleton(NodeFactory.makeId(_current.ast().getName(), name));
        }
        else { return Collections.emptySet(); }
    }

    public Set<Id> explicitVariableNames(Id name) {
        // TODO: imports
        if (_current.variables().containsKey(name) ||
            _current.units().containsKey(name)) {
            return Collections.singleton(NodeFactory.makeId(_current.ast().getName(), name));
        }
        else { return Collections.emptySet(); }
    }

    public Set<Id> explicitFunctionNames(Id name) {
        // TODO: imports
        if (_current.functions().containsFirst(name)) {
            return Collections.singleton(NodeFactory.makeId(_current.ast().getName(), name));
        }
        else { return Collections.emptySet(); }
    }

    public Set<OpName> explicitFunctionNames(OpName name) {
        // TODO: imports
        if (_current.functions().containsFirst(name)) {
            return Collections.singleton(name);
        }
        else { return Collections.emptySet(); }
    }

    @Override
    public Set<Id> explicitGrammarNames(String name) {
        // TODO: imports
        if (_current instanceof ApiIndex) {
            ApiIndex apiIndex = (ApiIndex) _current;
			if (apiIndex.grammars().containsKey(name)) {
                GrammarIndex g = apiIndex.grammars().get(name);
                Span span = g.getName().getSpan();
				APIName api = apiIndex.ast().getName();
				Id qname = NodeFactory.makeId(span , api , g.getName().getText());
                return Collections.singleton(qname);
            }
        }
        return Collections.emptySet();
    }

    private Set<Id> onDemandNames(Id name, Map<Id, Set<Id>> table)
    {
        if (table.containsKey(name)) {
            return table.get(name);
        } else {
            return new HashSet<Id>();
        }
    }

    public Set<Id> onDemandTypeConsNames(Id name) {
        return onDemandNames(name, _onDemandTypeConsNames);
    }

    public Set<Id> onDemandVariableNames(Id name) {
        return onDemandNames(name, _onDemandVariableNames);
    }

    public Set<Id> onDemandFunctionNames(Id name) {
        if (_onDemandFunctionIdNames.containsKey(name)) {
            return _onDemandFunctionIdNames.get(name);
        } else {
            return new HashSet<Id>();
        }
    }

    public Set<OpName> onDemandFunctionNames(OpName name) {
        if (_onDemandFunctionOpNames.containsKey(name)) {
            return _onDemandFunctionOpNames.get(name);
        } else {
            return new HashSet<OpName>();
        }
    }

    public Set<Id> onDemandGrammarNames(String name) {
        if (_onDemandGrammarNames.containsKey(name)) {
            return _onDemandGrammarNames.get(name);
        } else {
            return new HashSet<Id>();
        }
    }

    public boolean hasQualifiedTypeCons(Id name) {
        Option<APIName> optApi= name.getApi();
        if (optApi.isNone()) {
            bug(name, "Expected to have an API name.");
            return false;
        }
        APIName api = optApi.unwrap();
        if (_globalEnv.definesApi(api)) {
            return _globalEnv.api(api).typeConses().containsKey(name);
        }
        else { return false; }
    }

    public boolean hasQualifiedVariable(Id name) {
        Option<APIName> optApi= name.getApi();
        if (optApi.isNone())
            bug(name, "Expected to have an API name.");
        APIName api = optApi.unwrap();
        if (_globalEnv.definesApi(api)) {
            return _globalEnv.api(api).variables().containsKey(name);
        }
        else { return false; }
    }

    public boolean hasQualifiedFunction(Id name) {
        Option<APIName> optApi= name.getApi();
        if (optApi.isNone())
            bug(name, "Expected to have an API name.");
        APIName api = optApi.unwrap();
        if (_globalEnv.definesApi(api)) {
            return _globalEnv.api(api).functions().containsFirst(name);
        }
        else { return false; }
    }

    public boolean hasQualifiedGrammar(Id name) {
        Option<APIName> optApi= name.getApi();
        if (optApi.isNone())
            bug(name, "Expected to have an API name.");
        APIName api = optApi.unwrap();
        if (_globalEnv.definesApi(api)) {
            return _globalEnv.api(api).grammars().containsKey(name.getText());
        }
        else { return false; }
    }

    public TypeConsIndex typeConsIndex(final Id name) {
        Option<APIName> api = name.getApi();
        APIName actualApi;
        // If no API in name or it's the current API, use its own typeCons.
        // Otherwise, try to find the API in the global env and use its typeCons.
        if (api.isNone()) {
            actualApi = _current.ast().getName();
        } else {
            actualApi = api.unwrap();
        }
        if (api.isNone() || _current.ast().getName().equals(actualApi)) {
            TypeConsIndex res = _current.typeConses().get(ignoreApi(name));
            if (res != null) return res;
            System.err.println("Lookup of "+name+" in current api was null!\n  Trying qualified lookup, api = "+api);
        }

        TypeConsIndex res = _globalEnv.api(actualApi).typeConses().get(ignoreApi(name));
        if (res != null) return res;
        System.err.println("Still couldn't find "+name);
        return null;
    }

    private Id ignoreApi(Id id) {
        return new Id(id.getSpan(), id.getText());
    }

    public Option<GrammarIndex> grammarIndex(final Id name) {
    	String uqname = name.getText();
    	if (name.getApi().isSome()) {
            APIName n = name.getApi().unwrap();
            if (_globalEnv.definesApi(n)) {
            	return Option.some(_globalEnv.api(n).grammars().get(uqname));
            }
            else {
                return Option.none();
            }
        }
        if (_current instanceof ApiIndex) {
            return Option.some(((ApiIndex) _current).grammars().get(uqname));
        }
        else {
            _errors.add(StaticError.make("Attempt to get grammar definition from a component: " + name, name));
            return Option.none();
        }
    }
}
