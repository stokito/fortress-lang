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

import java.util.*;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.OptionVisitor;

import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.StaticError;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.TypeConsIndex;
import com.sun.fortress.compiler.index.CompilationUnitIndex;
import com.sun.fortress.compiler.index.Variable;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeFactory;

import com.sun.fortress.useful.NI;

public class TopLevelEnv extends NameEnv {
    private GlobalEnvironment _globalEnv;
    private CompilationUnitIndex _current;
    private List<StaticError> _errors;
    
    private Map<DottedName, ApiIndex> _onDemandImportedApis = new HashMap<DottedName, ApiIndex>();
    private Map<IdName, Set<QualifiedIdName>> _onDemandTypeConsNames = new HashMap<IdName, Set<QualifiedIdName>>();
    private Map<IdName, Set<QualifiedIdName>> _onDemandVariableNames = new HashMap<IdName, Set<QualifiedIdName>>();
    private Map<IdName, Set<QualifiedIdName>> _onDemandFunctionIdNames = new HashMap<IdName, Set<QualifiedIdName>>();
    private Map<OpName, Set<QualifiedOpName>> _onDemandFunctionOpNames = new HashMap<OpName, Set<QualifiedOpName>>();
    
    private static class TypeIndex {
        private DottedName _api;
        private TypeConsIndex _typeCons;
        
        TypeIndex(DottedName api, TypeConsIndex typeCons) {
            _api = api;
            typeCons = _typeCons;
        }
        public DottedName api() { return _api; }
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
    }
    
    /**
     * Initializes the map of imported API names to ApiIndices.
     * For now, all imports are assumed to be on-demand imports.
     */
    private void initializeOnDemandImportedApis() {
        // TODO: Fix to support other kinds of imports.        
        Set<DottedName> imports = _current.imports();


        // The following APIs are always imported, provided they exist. 
        DottedName FortressBuiltin = NodeFactory.makeDottedName("FortressBuiltin");
        DottedName FortressLibrary = NodeFactory.makeDottedName("FortressLibrary");
        
        addIfAvailableApi(FortressBuiltin, false);
        addIfAvailableApi(FortressLibrary, false);
        
        for (DottedName name: imports) {
            addIfAvailableApi(name, true);
        }
    }
    
    private void addIfAvailableApi(DottedName name, boolean errorIfUnavailable) {
        Map<DottedName, ApiIndex> availableApis = _globalEnv.apis();
        
        if (availableApis.containsKey(name)) {
            _onDemandImportedApis.put(name, availableApis.get(name));   
        }
        else if (errorIfUnavailable) {
            _errors.add(StaticError.make("Attempt to import an API not in the repository: " + name.getIds(),
                                        name.getSpan().toString()));
        }
            
    }
    
    private <T> void initializeEntry(Map.Entry<DottedName, ApiIndex> apiEntry,
                                     Map.Entry<IdName, T> entry, 
                                     Map<IdName, Set<QualifiedIdName>> table) {
        IdName key = entry.getKey();
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
        
        for (Map.Entry<DottedName, ApiIndex> apiEntry: _onDemandImportedApis.entrySet()) {
            for (Map.Entry<IdName, TypeConsIndex> typeEntry: apiEntry.getValue().typeConses().entrySet()) {
                initializeEntry(apiEntry, typeEntry, _onDemandTypeConsNames);
            } 
        }
    }
    
    private void initializeOnDemandVariableNames() {
        for (Map.Entry<DottedName, ApiIndex> apiEntry: _onDemandImportedApis.entrySet()) {
            for (Map.Entry<IdName, Variable> varEntry: apiEntry.getValue().variables().entrySet()) {
                initializeEntry(apiEntry, varEntry, _onDemandVariableNames);
            }
        } 
    }
    
    private void initializeOnDemandFunctionNames() {
        for (Map.Entry<DottedName, ApiIndex> apiEntry: _onDemandImportedApis.entrySet()) {
            for (SimpleName fnName: apiEntry.getValue().functions().firstSet()) {
                if (fnName instanceof IdName) {
                    IdName _fnName = (IdName)fnName;
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
    
    public Option<DottedName> apiName(DottedName name) {
        // TODO: Handle aliases.
        if (_onDemandImportedApis.containsKey(name)) {
            return Option.some(name);
        } else {
            return Option.none();
        }
    }
    
    public boolean hasTypeParam(IdName name) {
        return false;
    }

    public Set<QualifiedIdName> explicitTypeConsNames(IdName name) {
        // TODO: imports
        if (_current.typeConses().containsKey(name)) {
            return Collections.singleton(NodeFactory.makeQualifiedIdName(name));
        }
        else { return Collections.emptySet(); }
    }
    
    public Set<QualifiedIdName> explicitVariableNames(IdName name) {
        // TODO: imports
        if (_current.variables().containsKey(name)) {
            return Collections.singleton(NodeFactory.makeQualifiedIdName(name));
        }
        else { return Collections.emptySet(); }
    }
    
    public Set<QualifiedIdName> explicitFunctionNames(IdName name) {
        // TODO: imports
        if (_current.functions().containsFirst(name)) {
            return Collections.singleton(NodeFactory.makeQualifiedIdName(name));
        }
        else { return Collections.emptySet(); }
    }
    
    public Set<QualifiedOpName> explicitFunctionNames(OpName name) {
        // TODO: imports
        if (_current.functions().containsFirst(name)) {
            return Collections.singleton(NodeFactory.makeQualifiedOpName(name));
        }
        else { return Collections.emptySet(); }
    }

    private Set<QualifiedIdName> onDemandNames(IdName name, Map<IdName, Set<QualifiedIdName>> table) 
    {
        if (table.containsKey(name)) {
            return table.get(name);
        } else {
            return new HashSet<QualifiedIdName>();
        }
    }
        
    public Set<QualifiedIdName> onDemandTypeConsNames(IdName name) {
        return onDemandNames(name, _onDemandTypeConsNames);
    }
    
    public Set<QualifiedIdName> onDemandVariableNames(IdName name) {
        return onDemandNames(name, _onDemandVariableNames);
    }
    
    public Set<QualifiedIdName> onDemandFunctionNames(IdName name) {
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
    
    public boolean hasQualifiedTypeCons(QualifiedIdName name) {
        DottedName api = Option.unwrap(name.getApi());
        if (_globalEnv.definesApi(api)) {
            return _globalEnv.api(api).typeConses().containsKey(name);
        }
        else { return false; }
    }
    
    public boolean hasQualifiedVariable(QualifiedIdName name) {
        DottedName api = Option.unwrap(name.getApi());
        if (_globalEnv.definesApi(api)) {
            return _globalEnv.api(api).variables().containsKey(name.getName());
        }
        else { return false; }
    }
    
    public boolean hasQualifiedFunction(QualifiedIdName name) {
        DottedName api = Option.unwrap(name.getApi());
        if (_globalEnv.definesApi(api)) {
            return _globalEnv.api(api).functions().containsFirst(name.getName());
        }
        else { return false; }
    }
    
    public TypeConsIndex typeConsIndex(final QualifiedIdName name) {
        return name.getApi().apply(new OptionVisitor<DottedName, TypeConsIndex>() {
            public TypeConsIndex forSome(DottedName api) {
                return _globalEnv.api(api).typeConses().get(name.getName());
            }
            public TypeConsIndex forNone() {
                return _current.typeConses().get(name.getName());
            }
        });
    }
    
}
