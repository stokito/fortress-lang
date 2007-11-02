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
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.TypeConsIndex;
import com.sun.fortress.compiler.index.CompilationUnitIndex;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeFactory;

import com.sun.fortress.useful.NI;

public class TopLevelEnv extends NameEnv {
    private GlobalEnvironment _globalEnv;
    private CompilationUnitIndex _current;
    
    private Map<IdName, Set<QualifiedIdName>> _onDemandTypeConsNames = new HashMap<IdName, Set<QualifiedIdName>>();
    
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
    
    public TopLevelEnv(GlobalEnvironment globalEnv, CompilationUnitIndex current) {
        _globalEnv = globalEnv;
        _current = current;
        initializeOnDemandTypeConsNames();
    }
    
    private void initializeOnDemandTypeConsNames() {
        // For now, we support only on demand imports.
        // TODO: Fix to support explicit imports and api imports.
        
        //initializeAny();
        
        for (Map.Entry<DottedName, ApiIndex> apiEntry: _globalEnv.apis().entrySet()) {
            for (Map.Entry<IdName, TypeConsIndex> typeEntry: apiEntry.getValue().typeConses().entrySet()) {
                IdName key = typeEntry.getKey();
                if (_onDemandTypeConsNames.containsKey(key)) {
                    _onDemandTypeConsNames.get(key).add(new QualifiedIdName(key.getSpan(),
                                                                            Option.some(apiEntry.getKey()),
                                                                            key));
                                                                            
                } else {
                    Set<QualifiedIdName> matches = new HashSet<QualifiedIdName>();
                    matches.add(new QualifiedIdName(key.getSpan(),
                                                    Option.some(apiEntry.getKey()),
                                                    key));
                    _onDemandTypeConsNames.put(key, matches);
                }
            }
        } 
    }
    
    private void initializeAny() {
        // Type Any exists only as a type tag in the table.
        Set<QualifiedIdName> homeOfAny = new HashSet<QualifiedIdName>();
        List<Id> fortressBuiltin = new ArrayList<Id>();
        fortressBuiltin.add(new Id("FortressBuiltin"));
        
        homeOfAny.add
            (new QualifiedIdName
                 (Option.some
                      (new DottedName(fortressBuiltin)), 
                  new IdName(new Id("Any"))));
        _onDemandTypeConsNames.put(new IdName(new Id("Any")), homeOfAny);
    }
    
    public Option<DottedName> apiName(DottedName name) {
        return NI.nyi();
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

    public Set<QualifiedIdName> onDemandTypeConsNames(IdName name) {
        if (_onDemandTypeConsNames.containsKey(name)) {
            return _onDemandTypeConsNames.get(name);
        } else {
            return new HashSet<QualifiedIdName>();
        }
    }
    
    public Set<QualifiedIdName> onDemandVariableNames(IdName name) {
        // TODO: imports
        return Collections.emptySet();
    }
    
    public Set<QualifiedIdName> onDemandFunctionNames(IdName name) {
        // TODO: imports
        return Collections.emptySet();
    }
    
    public Set<QualifiedOpName> onDemandFunctionNames(OpName name) {
        // TODO: imports
        return Collections.emptySet();
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
