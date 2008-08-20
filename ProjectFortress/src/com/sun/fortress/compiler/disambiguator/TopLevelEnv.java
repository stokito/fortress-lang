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

import static com.sun.fortress.exceptions.InterpreterBug.bug;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.CompilationUnitIndex;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.compiler.index.Dimension;
import com.sun.fortress.compiler.index.Function;
import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.compiler.index.TypeConsIndex;
import com.sun.fortress.compiler.index.Unit;
import com.sun.fortress.compiler.index.Variable;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.interpreter.glue.WellKnownNames;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.AliasedAPIName;
import com.sun.fortress.nodes.AliasedSimpleName;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes.Enclosing;
import com.sun.fortress.nodes.Export;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdOrOpName;
import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.nodes.Import;
import com.sun.fortress.nodes.ImportApi;
import com.sun.fortress.nodes.ImportNames;
import com.sun.fortress.nodes.ImportStar;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeDepthFirstVisitor;
import com.sun.fortress.nodes.Op;
import com.sun.fortress.nodes.OpName;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.useful.NI;
import com.sun.fortress.useful.Useful;

import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.collect.FilteredRelation;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.lambda.Lambda;
import edu.rice.cs.plt.lambda.Lambda2;
import edu.rice.cs.plt.lambda.Predicate;
import edu.rice.cs.plt.lambda.Predicate2;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.Pair;

public class TopLevelEnv extends NameEnv {
    private static final Set<String> WELL_KNOWN_APIS = Useful.set(WellKnownNames.fortressLibrary, WellKnownNames.fortressBuiltin, WellKnownNames.anyTypeName);
    
    private final GlobalEnvironment _originalGlobalEnv; // environment as it is created by the compiler
    private final GlobalEnvironment _filteredGlobalEnv; // environment that only includes "imported" names
    private final CompilationUnitIndex _current;
    private List<StaticError> _errors;

    // As far as I can tell, these 'On Demand' data structures really hold all imported APIs.
    // Why are they so-named, or when did their behavior change? NEB
    private final Map<APIName, ApiIndex> _onDemandImportedApis;
    private final Map<Id, Set<Id>> _onDemandTypeConsNames;
    private final Map<Id, Set<Id>> _onDemandVariableNames;
    private final Map<Id, Set<Id>> _onDemandFunctionIdNames;
    private final Map<OpName, Set<OpName>> _onDemandFunctionOpNames;
    private final Map<String, Set<Id>> _onDemandGrammarNames;

    public TopLevelEnv(GlobalEnvironment globalEnv, CompilationUnitIndex current, List<StaticError> errors) {
        _originalGlobalEnv = globalEnv;
        _current = current;
        _errors = errors;
        
        GlobalEnvironment filtered_global_env;
        if( current instanceof ApiIndex ) {
         // Filter env based on what this api imports
          Map<APIName,ApiIndex> filtered = filterApis(globalEnv.apis(), ((Api)current.ast()));
          filtered_global_env = new GlobalEnvironment.FromMap(filtered);
        }
        else if( current instanceof ComponentIndex ) {
            //  Filter env based on what this component imports
            Map<APIName,ApiIndex> filtered = filterApis(globalEnv.apis(), ((Component)current.ast()));
            filtered_global_env = new GlobalEnvironment.FromMap(filtered);            
        }
        else {
            throw new IllegalArgumentException("Unanticipated subtype of CompilationUnitIndex.");
        }
        _filteredGlobalEnv = filtered_global_env;
        
        _onDemandImportedApis = Collections.unmodifiableMap(initializeOnDemandImportedApis(filtered_global_env, current));
        _onDemandTypeConsNames = Collections.unmodifiableMap(initializeOnDemandTypeConsNames(_onDemandImportedApis));
        _onDemandVariableNames = Collections.unmodifiableMap(initializeOnDemandVariableNames(_onDemandImportedApis));
        Pair<Map<Id, Set<Id>>, Map<OpName, Set<OpName>>> functions_and_ops =
            initializeOnDemandFunctionNames(_onDemandImportedApis);
        _onDemandFunctionIdNames = functions_and_ops.first();
        _onDemandFunctionOpNames = functions_and_ops.second();
        _onDemandGrammarNames = Collections.unmodifiableMap(initializeOnDemandGrammarNames(_onDemandImportedApis));
    }

    /**
     * Initializes the map of imported API names to ApiIndices. 
     * Adds all imported and implicitly imported
     * Apis to a map that it returns.
     * For now, all imports are assumed to be on-demand imports.
     */
    private Map<APIName, ApiIndex> initializeOnDemandImportedApis(GlobalEnvironment globalEnv, CompilationUnitIndex current) {
        // TODO: Fix to support other kinds of imports.
        Set<APIName> imports = current.imports();
        Map<APIName, ApiIndex> result = new HashMap<APIName, ApiIndex>(); 

        // The following APIs are always imported, provided they exist.
        for (APIName api : implicitlyImportedApis()) {
            addIfAvailableApi(globalEnv, result, api, false);
        }

        for (APIName name: imports) {
            addIfAvailableApi(globalEnv, result, name, true);
        }
        return result;
    }

    /**
     * Adds an API to {@code _onDemandImportedApis}, giving an error if
     * {@code errorIfUnavailable} is true and the given API cannot be
     * found in the {@code _globalEnv}'s list of apis.
     */
    private void addIfAvailableApi(GlobalEnvironment globalEnv, Map<APIName, ApiIndex> map, APIName name, boolean errorIfUnavailable) {
        Map<APIName, ApiIndex> availableApis = globalEnv.apis();

        if (availableApis.containsKey(name)) {
            map.put(name, availableApis.get(name));
        }
        else if (errorIfUnavailable) {
            _errors.add(StaticError.make("Attempt to import an API not in the repository: " + name.getIds(),
                                        name.getSpan().toString()));
        }

    }

    private static <T> void initializeEntry(Map.Entry<APIName, ApiIndex> apiEntry,
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

    private static Map<Id, Set<Id>> initializeOnDemandTypeConsNames(Map<APIName, ApiIndex> imported_apis) {
        // For now, we support only on demand imports.
        // TODO: Fix to support explicit imports and api imports. I don't think this is still a 'to-do'
        Map<Id, Set<Id>> result = new HashMap<Id, Set<Id>>();
        
        for (Map.Entry<APIName, ApiIndex> apiEntry: imported_apis.entrySet()) {
            for (Map.Entry<Id, TypeConsIndex> typeEntry: apiEntry.getValue().typeConses().entrySet()) {
                initializeEntry(apiEntry, typeEntry, result);
            }
            // Inject the names of physical units into the set of type cons names.
            for (Map.Entry<Id, Unit> unitEntry: apiEntry.getValue().units().entrySet()) {
                initializeEntry(apiEntry, unitEntry, result);
            }
            // Inject the names of physical dimensions into the set of type cons names.
            for (Map.Entry<Id, Dimension> dimEntry: apiEntry.getValue().dimensions().entrySet()) {
                initializeEntry(apiEntry, dimEntry, result);
            }
        }
        return result;
    }

    private static Map<Id, Set<Id>> initializeOnDemandVariableNames(Map<APIName, ApiIndex> imported_apis) {
        Map<Id, Set<Id>> result = new HashMap<Id, Set<Id>>();
        for (Map.Entry<APIName, ApiIndex> apiEntry: imported_apis.entrySet()) {
            for (Map.Entry<Id, Variable> varEntry: apiEntry.getValue().variables().entrySet()) {
                initializeEntry(apiEntry, varEntry, result);
            }
            // Inject the names of physical units into the set of bound variables.
            for (Map.Entry<Id, Unit> unitEntry: apiEntry.getValue().units().entrySet()) {
                initializeEntry(apiEntry, unitEntry, result);
            }
        }
        return result;
    }

    private static OpName copyOpNameWithNewAPIName(OpName op, final APIName api) {
    	OpName result = 
    		op.accept(new NodeDepthFirstVisitor<OpName>(){
    			@Override
    			public OpName defaultCase(Node that) {
    				return bug("Unexpected sub-type of OpName.");
    			}
    			@Override
    			public OpName forEnclosing(Enclosing that) {
    				return new Enclosing(that.getSpan(),Option.some(api),that.getOpen(),that.getClose());
    			}
    			@Override
    			public OpName forOp(Op that) {
    				return new Op(that.getSpan(),Option.some(api),that.getText(),that.getFixity());
    			}});
    	return result;
    }
    
    private static Pair<Map<Id, Set<Id>>, Map<OpName, Set<OpName>>> initializeOnDemandFunctionNames(Map<APIName, ApiIndex> imported_apis) {
        Map<Id, Set<Id>> fun_result = new HashMap<Id, Set<Id>>();
        Map<OpName, Set<OpName>> ops_result =  new HashMap<OpName, Set<OpName>>();
        
        for (Map.Entry<APIName, ApiIndex> apiEntry: imported_apis.entrySet()) {
            for (IdOrOpOrAnonymousName fnName: apiEntry.getValue().functions().firstSet()) {

                if (fnName instanceof Id ) {
                    Id _fnName = (Id)fnName;
                    Id name = new Id(_fnName.getSpan(),
                            Option.some(apiEntry.getKey()),
                            _fnName.getText());
                    if (fun_result.containsKey(_fnName)) {
                        fun_result.get(_fnName).add(name);
                    }
                    else {
                        Set<Id> matches = new HashSet<Id>();
                        matches.add(name);
                        fun_result.put(_fnName, matches);
                    }
                } 
                else { // fnName instanceof OpName
                    OpName _opName = (OpName)fnName;
                    OpName name = copyOpNameWithNewAPIName(_opName, apiEntry.getKey());
                    // NEB: I put this code here because I don't see why we shouldn't qualify OpNames as well...

                    if (ops_result.containsKey(_opName)) {
                        ops_result.get(_opName).add(name);
                    } else {
                        Set<OpName> matches = new HashSet<OpName>();
                        matches.add(name);
                        ops_result.put(_opName, matches);
                    }
                }
            }
        }
        return Pair.make(fun_result, ops_result);
    }

    private static Map<String, Set<Id>> initializeOnDemandGrammarNames(Map<APIName, ApiIndex> imported_apis) {
        Map<String, Set<Id>> result = new HashMap<String, Set<Id>>();
        for (Map.Entry<APIName, ApiIndex> apiEntry: imported_apis.entrySet()) {
            for (Map.Entry<String, GrammarIndex> grammarEntry: apiEntry.getValue().grammars().entrySet()) {
                Span span = grammarEntry.getValue().getName().getSpan();
                String key = grammarEntry.getKey();
                Id id = NodeFactory.makeId(span, apiEntry.getKey(), key);
                if (result.containsKey(key)) {
                    result.get(key).add(id);
                } else {
                    Set<Id> matches = new HashSet<Id>();
                    matches.add(id);
                    result.put(key, matches);
                }
            }
        }
        return result;
    }

    public Option<APIName> apiName(APIName name) {
        // TODO: Handle aliases.
        if (_onDemandImportedApis.containsKey(name)) {
            return Option.some(name);
        } else {
            return Option.none();
        }
    }
    
    @Override
    public Option<StaticParam> hasTypeParam(IdOrOpName name) {
        return Option.none();
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
        Set<Id> result = Collections.emptySet();
        if (_current.typeConses().containsKey(name) ||
                _current.dimensions().containsKey(name) ||
                _current.units().containsKey(name)) {

            // A name defined in this CU should only be qualified if this is an API 
            Id result_id;
            if( _current instanceof ApiIndex )
                result_id = NodeFactory.makeId(_current.ast().getName(), name, name.getSpan());
            else if( _current instanceof ComponentIndex )
                result_id = apiQualifyIfComponentExports(((ComponentIndex)_current), name);
            else 
                result_id = name;


            result  = Collections.singleton(result_id);
        }

        result = CollectUtil.union(result, this.onDemandTypeConsNames(name));
        return result;
    }

    /**
     * If the given {@code ComponentIndex} exports an API that contains this type name,
     * we should qualify this type name with the API that is being exported. This is done
     * so that this and other APIs can agree on which particular type is being referred to.
     */
    private Id apiQualifyIfComponentExports(ComponentIndex comp, Id name) {
        Option<Id> result_ = Option.none();
        
        for( APIName api_name : comp.exports() ) {
            
            // TODO: We don't really need this, but for now since there is no Executable...
            if( !_originalGlobalEnv.definesApi(api_name) ) continue;
            
            ApiIndex api = _originalGlobalEnv.api(api_name);
            
            if( api.typeConses().containsKey(name) ) {
                if( result_.isSome() ) 
                    return NI.nyi("Disambiguator cannot yet handle the same Component providing the implementation for multiple APIs: " + name);
                
                result_ = Option.some(NodeFactory.makeId(api_name, name, name.getSpan()));
            }
        }
        
        if( result_.isNone() )
            return name;
        else
            return result_.unwrap();
    }

    public Set<Id> explicitVariableNames(Id name) {  	
        Set<Id> result = Collections.emptySet();
        if (_current.variables().containsKey(name) ||
                _current.units().containsKey(name)) {
            
            // A name defined in this CU should only be qualified if this is an API
            Id result_id;
            if( _current instanceof ApiIndex )
                result_id = NodeFactory.makeId(_current.ast().getName(), name, name.getSpan());
            else
                result_id = name;

            result = Collections.singleton(result_id);
        }

        result = CollectUtil.union(result, this.onDemandVariableNames(name));
        return result;
    }

    public List<Id> explicitVariableNames() {
        List<Id> result = new LinkedList<Id>();
        result.addAll(_current.variables().keySet());
        return result;
    }

    public Set<Id> explicitFunctionNames(Id name) {
    	Set<Id> result = Collections.emptySet();
    	
    	// Add fns from this component
        if (_current.functions().containsFirst(name)) {
            // Only qualify name with an API if we are indeed inside of an API
            Id result_id;
            if( _current instanceof ApiIndex )
                result_id = NodeFactory.makeId(_current.ast().getName(), name, name.getSpan());
            else
                result_id = name;
            
            result = CollectUtil.union(result, Collections.singleton(result_id));
        }
        
        // Also add imports
        result = CollectUtil.union(result, this.onDemandFunctionNames(name));

        return result;
    }

    public Set<OpName> explicitFunctionNames(OpName name) {
        Set<OpName> result = Collections.emptySet();

        // Add ops in this component
        if( _current.functions().containsFirst(name)) {
            // Only qualify name with an API if we are inside of an API
            OpName result_id;
            if( _current instanceof ApiIndex )
                result_id = NodeFactory.makeOpName(_current.ast().getName(), name);
            else
                result_id = name;
                
            result = CollectUtil.union(result, Collections.singleton(result_id));
        }

        // Also add imports
        result = CollectUtil.union(result, this.onDemandFunctionNames(name));

        return result;
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
        if (_filteredGlobalEnv.definesApi(api)) {
            return _filteredGlobalEnv.api(api).typeConses().containsKey(name);
        }
        else { return false; }
    }

    public boolean hasQualifiedVariable(Id name) {
        Option<APIName> optApi= name.getApi();
        if (optApi.isNone())
            bug(name, "Expected to have an API name.");
        APIName api = optApi.unwrap();
        if (_filteredGlobalEnv.definesApi(api)) {
            return _filteredGlobalEnv.api(api).variables().containsKey(name);
        }
        else { return false; }
    }

    public boolean hasQualifiedFunction(Id name) {
        Option<APIName> optApi= name.getApi();
        if (optApi.isNone())
            bug(name, "Expected to have an API name.");
        APIName api = optApi.unwrap();
        if (_filteredGlobalEnv.definesApi(api)) {
            return _filteredGlobalEnv.api(api).functions().containsFirst(name);
        }
        else { return false; }
    }

    public boolean hasQualifiedGrammar(Id name) {
        Option<APIName> optApi= name.getApi();
        if (optApi.isNone())
            bug(name, "Expected to have an API name.");
        APIName api = optApi.unwrap();
        if (_filteredGlobalEnv.definesApi(api)) {
            return _filteredGlobalEnv.api(api).grammars().containsKey(name.getText());
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

        // By this point it should be okay to use the unfiltered environment because this
        // method should only be called after the name is disambiguated.
        TypeConsIndex res = _originalGlobalEnv.api(actualApi).typeConses().get(ignoreApi(name));
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
            if (_filteredGlobalEnv.definesApi(n)) {
            	return Option.some(_filteredGlobalEnv.api(n).grammars().get(uqname));
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
    
    /**
     * Returns API names in the given list of exports that can be imported implicitly.
     * This helps, for example, when compiling FortressLibrary, since that is one API that
     * is normally imported implicitly. 
     */
    private static Set<APIName> findWellKnownExports(List<Export> exports) {
        return IterUtil.fold(exports, new HashSet<APIName>(),new Lambda2<HashSet<APIName>,Export, HashSet<APIName>>(){
            public HashSet<APIName> value(HashSet<APIName> arg0, Export arg1) {
                for( APIName api : arg1.getApis() ) {
                    if( WELL_KNOWN_APIS.contains(api.getText()) ) {
                        arg0.add(api);
                    }
                }
                return arg0;
            }});
    }
    
    private static  Map<APIName, ApiIndex> filterApis(Map<APIName,ApiIndex> apis, Component comp) {
        Set<APIName> dont_import = findWellKnownExports(comp.getExports());
        return filterApis(Collections.unmodifiableMap(apis), comp.getImports(), dont_import);
    }

    private static Map<APIName, ApiIndex> filterApis(Map<APIName, ApiIndex> apis, Api api) {
        // Insert 'this' api as an implicit import. This kind of strange, but the grammars
        // need them at a minimum.
        Import this_api_import = new ImportStar(api.getName(), Collections.<IdOrOpOrAnonymousName>emptyList());
        return filterApis(Collections.unmodifiableMap(apis), 
                Useful.concat(Collections.singletonList(this_api_import), 
                        api.getImports()
                        )
                        , Collections.<APIName>emptySet());
    }

    private static <K, T> Map<K,T> filterMap(Map<K,T> map, Set<? super K> set, 
            Predicate<K> pred) {
        
        Map<K,T> result = new HashMap<K,T>();
        for( Map.Entry<K, T> entry : map.entrySet() ) {
            if( pred.contains(entry.getKey()) ) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }
    
    private static <K, T> Map<K,T> removeHelper(Map<K,T> map, final Set<? super K> set) {
        Predicate<K> pred = new Predicate<K>() {
            public boolean contains(K arg1) {
                return !set.contains(arg1);
            }};
        
        return filterMap(map, set, pred);
    }
    
    private static ApiIndex remove(ApiIndex index,
            final Set<IdOrOpOrAnonymousName> exceptions_) {
        
        Predicate2<IdOrOpOrAnonymousName,Function> pred = new Predicate2<IdOrOpOrAnonymousName,Function>(){

            public boolean contains(IdOrOpOrAnonymousName arg0, Function arg1) {
                return !exceptions_.contains(arg0);
            }
            
        };
        
        return new ApiIndex((Api)index.ast(),
                            removeHelper(index.variables(), exceptions_),
                            new FilteredRelation<IdOrOpOrAnonymousName,Function>(index.functions(), pred),
                            removeHelper(index.typeConses(), exceptions_),
                            removeHelper(index.dimensions(), exceptions_),
                            removeHelper(index.units(), exceptions_),
                            index.grammars(),
                            index.modifiedDate());
    }
    
    private static <K, T> Map<K,T> keepHelper(Map<K,T> map, final Set<? super K> set) {
        Predicate<K> pred = new Predicate<K>() {
            public boolean contains(K arg1) {
                return set.contains(arg1);
            }};
        
        return filterMap(map, set, pred);
    }
    
    private static ApiIndex keep(ApiIndex index,
            final Set<IdOrOpOrAnonymousName> allowed_) {
        
        Predicate2<IdOrOpOrAnonymousName,Function> pred = new Predicate2<IdOrOpOrAnonymousName,Function>(){

            public boolean contains(IdOrOpOrAnonymousName arg0, Function arg1) {
                return allowed_.contains(arg0);
            }
            
        };
        
        return new ApiIndex((Api)index.ast(),
                keepHelper(index.variables(), allowed_),
                new FilteredRelation<IdOrOpOrAnonymousName,Function>(index.functions(), pred),
                keepHelper(index.typeConses(), allowed_),
                keepHelper(index.dimensions(), allowed_),
                keepHelper(index.units(), allowed_),
                index.grammars(),
                index.modifiedDate());
    }
    
    /**
     * Filter out whole apis or parts of apis based on the imports of a component or
     * api. FortressBuiltin and AnyType are always imported, and FortressLibrary is only 
     * imported implicitly if it is not imported explicitly. If {@code do_not_import} contains
     * api names, those apis will not beimported no matter what.
     */
    private static Map<APIName,ApiIndex> filterApis(Map<APIName, ApiIndex> apis, List<Import> imports, Set<APIName> do_not_import) { 
        final Map<APIName, Set<IdOrOpOrAnonymousName>> exceptions = new HashMap<APIName, Set<IdOrOpOrAnonymousName>>();
        final Map<APIName, Set<IdOrOpOrAnonymousName>> allowed = new HashMap<APIName, Set<IdOrOpOrAnonymousName>>();

        NodeDepthFirstVisitor<Boolean> import_visitor = new NodeDepthFirstVisitor<Boolean>(){
            // Do nothing for template-related imports
            @Override public Boolean defaultCase(Node that) {return false;}

            @Override
            public Boolean forImportApi(ImportApi that) {
                Boolean implib = true;
                for( AliasedAPIName api : that.getApis() ) {
                    APIName name = api.getApi();
                    if(name.getText().equals(WellKnownNames.fortressLibrary))
                        implib=false;
                    if( !exceptions.containsKey(name) )
                        exceptions.put(name, new HashSet<IdOrOpOrAnonymousName>());
                }
                return implib;
            }

            @Override
            public Boolean forImportNames(ImportNames that) {
                APIName name = that.getApi();

                // TODO Handle these aliased names more thoroughly
                List<IdOrOpOrAnonymousName> names = CollectUtil.makeList(IterUtil.map(that.getAliasedNames(), 
                        new Lambda<AliasedSimpleName,IdOrOpOrAnonymousName>(){
                    public IdOrOpOrAnonymousName value(
                            AliasedSimpleName arg0) {
                        return arg0.getName();
                    }}));

                if( allowed.containsKey(name) )
                    allowed.get(name).addAll(names);
                else
                    allowed.put(name, new HashSet<IdOrOpOrAnonymousName>(names));
                return !name.getText().equals(WellKnownNames.fortressLibrary);
            }

            @Override
            public Boolean forImportStar(ImportStar that) {
                APIName name = that.getApi();
                if( exceptions.containsKey(name) )
                    exceptions.get(name).addAll(that.getExcept());
                else
                    exceptions.put(name, new HashSet<IdOrOpOrAnonymousName>(that.getExcept()));
                return !name.getText().equals(WellKnownNames.fortressLibrary);
            }
        };

        // Visit each import, populating the exceptions and allowed maps
        Iterable<Boolean> temp=IterUtil.map(imports, import_visitor);
        boolean import_library = true;
        for(Boolean t : temp){
            import_library&=t;
        }

        // Created filters for ApiIndex in apis
        Map<APIName, ApiIndex> result = new HashMap<APIName, ApiIndex>();
        for( Map.Entry<APIName, ApiIndex> api : apis.entrySet() ) {
            // TODO: Report an error on conflicting import statements
            APIName name = api.getKey();
            ApiIndex index = api.getValue();


            if( do_not_import.contains(name) ) {
                // Do nothing! this is a set of things we must not import no matter what.
            }
            else if( exceptions.containsKey(name) ) {
                Set<IdOrOpOrAnonymousName> exceptions_ = exceptions.get(name);
                result.put(name, remove(index, exceptions_));
            }
            else if( allowed.containsKey(name) ) {
                Set<IdOrOpOrAnonymousName> allowed_ = allowed.get(name);
                result.put(name, keep(index, allowed_));
            } 
            else if( name.getText().equals(WellKnownNames.fortressBuiltin) ) {
                // Fortress builtin is always implicitly imported
                result.put(name, index);
            }
            else if( name.getText().equals(WellKnownNames.anyTypeLibrary) ) {
                // For now AnyType needs to be implicitly imported
                result.put(name, index);
            }
            else if( name.getText().equals(WellKnownNames.fortressLibrary) && import_library ) {
                // FortressLibrary is only imported implicitly if nothing from it is imported explicitly
                result.put(name, index);
            }
        }
        return result;
    }
}
