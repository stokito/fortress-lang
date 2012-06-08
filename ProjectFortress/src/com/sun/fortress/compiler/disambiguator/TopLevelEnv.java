/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.disambiguator;

import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.WellKnownNames;
import com.sun.fortress.compiler.index.*;
import static com.sun.fortress.exceptions.InterpreterBug.bug;
import static com.sun.fortress.exceptions.ProgramError.errorMsg;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.useful.Useful;
import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.collect.FilteredRelation;
import edu.rice.cs.plt.collect.IndexedRelation;
import edu.rice.cs.plt.collect.Relation;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.lambda.*;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.Pair;
import edu.rice.cs.plt.tuple.Triple;

import java.util.*;

/**
 * This class is used by the disambiguator to represent the top-level environment of a component or API.
 * The environment it represents includes all imported identifiers and all identifiers declared at top-level.
 */
public class TopLevelEnv extends NameEnv {
    private static final Set<String> WELL_KNOWN_APIS = Useful.set(WellKnownNames.fortressLibrary(),
                                                                  WellKnownNames.fortressBuiltin(),
                                                                  WellKnownNames.anyTypeLibrary());

    private final GlobalEnvironment _originalGlobalEnv; // environment as it is created by the compiler
    private final GlobalEnvironment _filteredGlobalEnv; // environment that only includes "imported" names
    private final CompilationUnitIndex _current;
    private List<StaticError> _errors;
    private Map<IdOrOpOrAnonymousName, IdOrOpOrAnonymousName> _aliases;

    // As far as I can tell, these 'On Demand' data structures really hold all imported APIs.
    // Why are they so-named, or when did their behavior change? NEB
    private final Map<APIName, ApiIndex> _onDemandImportedApis;
    private final Map<Id, Set<Id>> _onDemandTypeConsNames;
    private final Map<Id, Set<Id>> _onDemandVariableNames;
    private final Map<Id, Set<Id>> _onDemandFunctionIdNames;
    private final Map<Op, Set<Op>> _onDemandFunctionOps;
    private final Set<Pair<ApiIndex, ParametricOperator>> _onDemandParametricOps;
    private final Map<String, Set<Id>> _onDemandGrammarNames;

    public TopLevelEnv(GlobalEnvironment globalEnv, CompilationUnitIndex current, List<StaticError> errors) {
        //           System.err.println("TopLevelEnv globalEnv:");
        //           globalEnv.print();
        //           System.err.println("End TopLevelEnv globalEnv");

        _originalGlobalEnv = globalEnv;
        _current = current;
        _errors = errors;
        _aliases = new HashMap<IdOrOpOrAnonymousName, IdOrOpOrAnonymousName>();

        GlobalEnvironment filtered_global_env;
        if (current instanceof ApiIndex) {
            // Filter env based on what this api imports
            Map<APIName, ApiIndex> filtered = filterApis(globalEnv.apis(), ((Api) current.ast()));
            filtered_global_env = new GlobalEnvironment.FromMap(filtered);
        } else if (current instanceof ComponentIndex) {
            //  Filter env based on what this component imports
            Map<APIName, ApiIndex> filtered = filterApis(globalEnv.apis(), ((Component) current.ast()));
            filtered_global_env = new GlobalEnvironment.FromMap(filtered);
        } else {
            throw new IllegalArgumentException("Unanticipated subtype of CompilationUnitIndex.");
        }
        _filteredGlobalEnv = filtered_global_env;

        //           System.err.println("TopLevelEnv _filteredGlobalEnv:");
        //           _filteredGlobalEnv.print();
        //           System.err.println("End TopLevelEnv _filteredGlobalEnv");

        _onDemandImportedApis = Collections.unmodifiableMap(initializeOnDemandImportedApis(filtered_global_env,
                                                                                           current));
        _onDemandTypeConsNames = Collections.unmodifiableMap(initializeOnDemandTypeConsNames(_onDemandImportedApis));
        _onDemandVariableNames = Collections.unmodifiableMap(initializeOnDemandVariableNames(_onDemandImportedApis));
        Triple<Map<Id, Set<Id>>, Map<Op, Set<Op>>, Set<Pair<ApiIndex, ParametricOperator>>> functions_and_ops =
                initializeOnDemandFunctionNames(_onDemandImportedApis);
        _onDemandFunctionIdNames = functions_and_ops.first();
        _onDemandFunctionOps = functions_and_ops.second();
        _onDemandParametricOps = functions_and_ops.third();
        _onDemandGrammarNames = Collections.unmodifiableMap(initializeOnDemandGrammarNames(_onDemandImportedApis));

    }

    /**
     * Initializes the map of imported API names to ApiIndices.
     * Adds all imported and implicitly imported
     * Apis to a map that it returns.
     * For now, all imports are assumed to be on-demand imports.
     */
    private Map<APIName, ApiIndex> initializeOnDemandImportedApis(GlobalEnvironment globalEnv,
                                                                  CompilationUnitIndex current) {
        // TODO: Fix to support other kinds of imports.
        Set<APIName> imports = current.imports();
        Map<APIName, ApiIndex> result = new HashMap<APIName, ApiIndex>();

        // The following APIs are always imported, provided they exist.
        for (APIName api : implicitlyImportedApis()) {
            addIfAvailableApi(globalEnv, result, api, false);
        }

        for (APIName name : imports) {
            addIfAvailableApi(globalEnv, result, name, true);
        }
        return result;
    }

    /**
     * Adds an API to {@code _onDemandImportedApis}, giving an error if
     * {@code errorIfUnavailable} is true and the given API cannot be
     * found in the {@code _globalEnv}'s list of apis.
     */
    private void addIfAvailableApi(GlobalEnvironment globalEnv,
                                   Map<APIName, ApiIndex> map,
                                   APIName name,
                                   boolean errorIfUnavailable) {
        Map<APIName, ApiIndex> availableApis = globalEnv.apis();
        // System.err.println("addIfAvailableApi");
        // globalEnv.print();

        if (availableApis.containsKey(name)) {
            map.put(name, availableApis.get(name));
        } else if (errorIfUnavailable) {
            _errors.add(StaticError.make("Attempt to import an API not in the repository: " + name,
                                         NodeUtil.getSpan(name)));
        }

    }

    private static <T> void initializeEntry(Map.Entry<APIName, ApiIndex> apiEntry,
                                            Map.Entry<Id, T> entry,
                                            Map<Id, Set<Id>> table) {
        Id key = entry.getKey();
        if (table.containsKey(key)) {
            table.get(key).add(NodeFactory.makeId(NodeUtil.getSpan(key),
                                                  Option.some(apiEntry.getKey()),
                                                  key.getText()));

        } else {
            Set<Id> matches = new HashSet<Id>();
            matches.add(NodeFactory.makeId(NodeUtil.getSpan(key), Option.some(apiEntry.getKey()), key.getText()));
            table.put(key, matches);
        }
    }

    private Map<Id, Set<Id>> initializeOnDemandTypeConsNames(Map<APIName, ApiIndex> imported_apis) {
        // For now, we support only on demand imports.
        // TODO: Fix to support explicit imports and api imports. I don't think this is still a 'to-do'
        Map<Id, Set<Id>> result = new HashMap<Id, Set<Id>>();

        for (Map.Entry<APIName, ApiIndex> apiEntry : imported_apis.entrySet()) {
            for (Map.Entry<Id, TypeConsIndex> typeEntry : apiEntry.getValue().typeConses().entrySet()) {
                // System.err.println("TopLevelEnv for " + _current.ast().getName() + ": initializing entry " + apiEntry);
                initializeEntry(apiEntry, typeEntry, result);
            }
            // Inject the names of physical units into the set of type cons names.
            for (Map.Entry<Id, Unit> unitEntry : apiEntry.getValue().units().entrySet()) {
                initializeEntry(apiEntry, unitEntry, result);
            }
            // Inject the names of physical dimensions into the set of type cons names.
            for (Map.Entry<Id, Dimension> dimEntry : apiEntry.getValue().dimensions().entrySet()) {
                initializeEntry(apiEntry, dimEntry, result);
            }
        }
        return result;
    }

    private static Map<Id, Set<Id>> initializeOnDemandVariableNames(Map<APIName, ApiIndex> imported_apis) {
        Map<Id, Set<Id>> result = new HashMap<Id, Set<Id>>();
        for (Map.Entry<APIName, ApiIndex> apiEntry : imported_apis.entrySet()) {
            for (Map.Entry<Id, Variable> varEntry : apiEntry.getValue().variables().entrySet()) {
                initializeEntry(apiEntry, varEntry, result);
            }
            // Inject the names of physical units into the set of bound variables.
            for (Map.Entry<Id, Unit> unitEntry : apiEntry.getValue().units().entrySet()) {
                initializeEntry(apiEntry, unitEntry, result);
            }
        }
        return result;
    }

    private static Op copyOpWithNewAPIName(Op op, final APIName api) {
        Op result = op.accept(new NodeDepthFirstVisitor<Op>() {
            @Override
            public Op defaultCase(Node that) {
                return bug("Unexpected sub-type of Op.");
            }

            @Override
            public Op forNamedOp(NamedOp that) {
                return NodeFactory.makeOp(NodeUtil.getSpan(that),
                                          Option.some(api),
                                          that.getText(),
                                          that.getFixity(),
                                          that.isEnclosing());
            }
        });
        return result;
    }

    private static Triple<Map<Id, Set<Id>>, Map<Op, Set<Op>>, Set<Pair<ApiIndex, ParametricOperator>>>
                   initializeOnDemandFunctionNames(Map<APIName, ApiIndex> imported_apis) {
        Map<Id, Set<Id>> fun_result = new HashMap<Id, Set<Id>>();
        Map<Op, Set<Op>> ops_result = new HashMap<Op, Set<Op>>();
        Set<Pair<ApiIndex, ParametricOperator>> paramOpsResult = new HashSet<Pair<ApiIndex, ParametricOperator>>();

        for (Map.Entry<APIName, ApiIndex> apiEntry : imported_apis.entrySet()) {
            for (IdOrOpOrAnonymousName fnName : apiEntry.getValue().functions().firstSet()) {
                if (fnName instanceof Id) {
                    Id _fnName = (Id) fnName;
                    Id name = NodeFactory.makeId(NodeUtil.getSpan(_fnName),
                                                 Option.some(apiEntry.getKey()),
                                                 _fnName.getText());
                    if (fun_result.containsKey(_fnName)) {
                        fun_result.get(_fnName).add(name);
                    } else {
                        Set<Id> matches = new HashSet<Id>();
                        matches.add(name);
                        fun_result.put(_fnName, matches);
                    }
                } else { // fnName instanceof Op
                    Op _opName = (Op) fnName;
                    Op name = copyOpWithNewAPIName(_opName, apiEntry.getKey());
                    // NEB: I put this code here because I don't see why we shouldn't qualify Ops as well...
                    boolean found = false;
                    for (Map.Entry<Op, Set<Op>> f : ops_result.entrySet()) {
                        if (f.getKey().getText().equals(_opName.getText())) {
                            f.getValue().add(name);
                            found = true;
                        }
                    }
                    if (!found) {
                        Set<Op> matches = new HashSet<Op>();
                        matches.add(name);
                        ops_result.put(_opName, matches);
                    }
                    /* r4187
                    if (ops_result.containsKey(_opName)) {
                        ops_result.get(_opName).add(name);
                    } else {
                        Set<Op> matches = new HashSet<Op>();
                        matches.add(name);
                        ops_result.put(_opName, matches);
                    }
                    */
                }
            }
            // Accumulate parametrically named operators from imported APIs.
            ApiIndex api = apiEntry.getValue();
            paramOpsResult = CollectUtil.union(paramOpsResult, qualifyParametricOps(api));
        }
        return Triple.make(fun_result, ops_result, paramOpsResult);
    }

    private static Set<Pair<ApiIndex, ParametricOperator>> qualifyParametricOps(ApiIndex api) {
        Set<Pair<ApiIndex, ParametricOperator>> result = new HashSet<Pair<ApiIndex, ParametricOperator>>();
        for (ParametricOperator op : api.parametricOperators()) {
            result.add(Pair.make(api, op));
        }
        return result;
    }

    private static Map<String, Set<Id>> initializeOnDemandGrammarNames(Map<APIName, ApiIndex> imported_apis) {
        Map<String, Set<Id>> result = new HashMap<String, Set<Id>>();
        for (Map.Entry<APIName, ApiIndex> apiEntry : imported_apis.entrySet()) {
            for (Map.Entry<String, GrammarIndex> grammarEntry : apiEntry.getValue().grammars().entrySet()) {
                Span span = NodeUtil.getSpan(grammarEntry.getValue().getName());
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
        if (_onDemandImportedApis.containsKey(name) || _current.name().equals(name)) {
            return Option.some(name);
        } else {
            return Option.none();
        }
    }

    @Override
    public Option<StaticParam> hasTypeParam(IdOrOp name) {
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
        if (_current.typeConses().containsKey(name) || _current.dimensions().containsKey(name) ||
            _current.units().containsKey(name)) {

            // A name defined in this CU should only be qualified if this is an API
            Id result_id;
            if (_current instanceof ApiIndex) result_id = NodeFactory.makeId(_current.ast().getName(),
                                                                             name,
                                                                             NodeUtil.getSpan(name));
            else if (_current instanceof ComponentIndex)
                result_id = apiQualifyIfComponentExports(((ComponentIndex) _current), name);
            else result_id = name;


            result = Collections.singleton(result_id);
        }

        result = CollectUtil.union(result, this.onDemandTypeConsNames(name));

        if (_aliases.containsKey(name)) result = CollectUtil.union(result,
                                                                   explicitTypeConsNames((Id) _aliases.get(name)));

        return result;
    }

    /**
     * If the given {@code ComponentIndex} exports an API that contains this type name,
     * we should qualify this type name with the API that is being exported. This is done
     * so that this and other APIs can agree on which particular type is being referred to.
     */
    private Id apiQualifyIfComponentExports(ComponentIndex comp, Id name) {
        Option<Id> result_ = Option.none();

        for (APIName api_name : comp.exports()) {

            // TODO: We don't really need this, but for now since there is no Executable...
            if (!_originalGlobalEnv.definesApi(api_name)) continue;

            ApiIndex api = _originalGlobalEnv.api(api_name);

            if (api.typeConses().containsKey(name)) {

                /* if( result_.isSome() )
                 *   Will be caught by export checker
                 *   return NI.nyi("Disambiguator cannot yet handle the same Component providing the implementation for multiple APIs: " + name);
                 */
                result_ = Option.some(NodeFactory.makeId(api_name, name, NodeUtil.getSpan(name)));
            }
        }

        if (result_.isNone()) return name;
        else return result_.unwrap();
    }

    public Set<Id> explicitVariableNames(Id name) {
        Set<Id> result = Collections.emptySet();
        if (_current.variables().containsKey(name) || _current.units().containsKey(name)) {

            // A name defined in this CU should only be qualified if this is an API
            Id result_id;
            if (_current instanceof ApiIndex) result_id = NodeFactory.makeId(_current.ast().getName(),
                                                                             name,
                                                                             NodeUtil.getSpan(name));
            else result_id = name;

            result = Collections.singleton(result_id);
        }

        result = CollectUtil.union(result, this.onDemandVariableNames(name));

        if (_aliases.containsKey(name)) result = CollectUtil.union(result,
                                                                   explicitVariableNames((Id) _aliases.get(name)));

        return result;
    }

    public List<Id> explicitVariableNames() {
        List<Id> result = new LinkedList<Id>();
        result.addAll(_current.variables().keySet());
        return result;
    }

    
    // Function that gets an unambiguous name out of a Function.
    private Lambda<Function,IdOrOp> unambiguousNameFromFunction = new Lambda<Function, IdOrOp>() {
        @Override public IdOrOp value(Function fn) {
            return fn.unambiguousName();
        }
    };
    
    // Qualifies the given name with the given API. Create normal Lambdas
    // by binding an API.
    private Lambda2<APIName, IdOrOp, IdOrOp> addApi = new Lambda2<APIName, IdOrOp, IdOrOp>() {
        @Override
        public IdOrOp value(APIName api, IdOrOp name) {
            if (name instanceof Id) {
                return NodeFactory.makeId(Option.some(api), (Id) name);
            } else {
                return NodeFactory.makeOp(Option.some(api), (Op) name);
            }
        }
    };
    
    Lambda<Pair<ApiIndex,ParametricOperator>, ParametricOperator> second = new Lambda<Pair<ApiIndex,ParametricOperator>, ParametricOperator>(){
        @Override
        public ParametricOperator value(Pair<ApiIndex, ParametricOperator> p) {
            return p.second();
        }
    };

    
    public Set<IdOrOp> explicitFunctionNames(IdOrOp name) {
        // Add fns/ops from this component
        Set<IdOrOp> current = (_current.functions().containsFirst(name)) ?
                                   Collections.<IdOrOp>singleton((_current instanceof ApiIndex) ? addApi.value(_current.ast().getName(), name) : name) :
                                   CollectUtil.<IdOrOp>emptySet();
        // Also add imports
        Set<IdOrOp> imports = (Set<IdOrOp>) this.onDemandFunctionNames(name);
        // Add aliases
        Set<IdOrOp> aliases = (_aliases.containsKey(name)) ? explicitFunctionNames((IdOrOp) _aliases.get(name)) : CollectUtil.<IdOrOp>emptySet();
        
        // Union and return
        return CollectUtil.union(CollectUtil.union(current, imports), aliases);
    }

    public Set<IdOrOp> unambiguousFunctionNames(final IdOrOp name) {
        // If this name is qualified, then lookup names only in that API.
        if (name.getApiName().isSome()) {
            APIName api = name.getApiName().unwrap();
            // Get the functions from this api with this name.
            Set<? extends Function> functions =
                _onDemandImportedApis.get(api).functions().matchFirst(name);
            // Return their unambiguous names, qualified.
            Lambda<IdOrOp, IdOrOp> addThisApi = LambdaUtil.bindFirst(addApi, api);
            return CollectUtil.asSet(IterUtil.map(functions,
                                                  LambdaUtil.compose(unambiguousNameFromFunction,
                                                                     addThisApi)));
        }

        // First get all the declarations from this compilation unit.
        Set<? extends Function> functions = _current.functions().matchFirst(name);
        Iterable<IdOrOp> results = IterUtil.map(functions, unambiguousNameFromFunction);

        // Loop over all the imported APIs.
        for (final ApiIndex api : _onDemandImportedApis.values()) {
            Lambda<IdOrOp, IdOrOp> addThisApi = LambdaUtil.bindFirst(addApi, api.ast().getName());
            // Get all the declarations from this API and qualify the names.
            functions = api.functions().matchFirst(name);
            results = IterUtil.compose(results,
                                       IterUtil.map(functions,
                                                    LambdaUtil.compose(unambiguousNameFromFunction,
                                                                       addThisApi)));
        }

        // Add in the unambiguous names from the alias.
        // TODO: Correct behavior?
        if (_aliases.containsKey(name)) {
            results = IterUtil.compose(results,
                                       unambiguousFunctionNames((IdOrOp) _aliases.get(name)));
        }

        return CollectUtil.asSet(results);
    }

    @Override
    public Set<Pair<Op, Op>> getParametricOperators() {
        Iterable<ParametricOperator> pOps = IterUtil.compose(_current.parametricOperators(), IterUtil.map(onDemandParametricOps(), second));
        Iterable<Pair<Op,Op>> pairs = IterUtil.map(pOps, new Lambda<ParametricOperator, Pair<Op, Op>>(){
            @Override
            public Pair<Op, Op> value(ParametricOperator p) {
                return Pair.make(p.name(), (Op) p.unambiguousName());
            }
        });
        return CollectUtil.asSet(pairs);
    }
    
    @Override
    public Set<Id> explicitGrammarNames(String name) {
        // TODO: imports
        if (_current instanceof ApiIndex) {
            ApiIndex apiIndex = (ApiIndex) _current;
            if (apiIndex.grammars().containsKey(name)) {
                GrammarIndex g = apiIndex.grammars().get(name);
                Span span = NodeUtil.getSpan(g.getName());
                APIName api = apiIndex.ast().getName();
                Id qname = NodeFactory.makeId(span, api, g.getName().getText());
                return Collections.singleton(qname);
            }
        }
        return Collections.emptySet();
    }

    private Set<Id> onDemandNames(Id name, Map<Id, Set<Id>> table) {
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

    public Set<? extends IdOrOp> onDemandFunctionNames(IdOrOp name) {
        if (name instanceof Id) return onDemandFunctionNames((Id) name);
        else return onDemandFunctionNames((Op) name);
    }

    public Set<Id> onDemandFunctionNames(Id name) {
        if (_onDemandFunctionIdNames.containsKey(name)) {
            return _onDemandFunctionIdNames.get(name);
        } else {
            return new HashSet<Id>();
        }
    }

    public Set<Op> onDemandFunctionNames(Op name) {
        for (Op op : _onDemandFunctionOps.keySet()) {
            if (op.getText().equals(name.getText()))
                return _onDemandFunctionOps.get(op);
        }
        return new HashSet<Op>();
    }

    public Set<Pair<ApiIndex, ParametricOperator>> onDemandParametricOps() {
        return _onDemandParametricOps;
    }

    public Set<Id> onDemandGrammarNames(String name) {
        if (_onDemandGrammarNames.containsKey(name)) {
            return _onDemandGrammarNames.get(name);
        } else {
            return new HashSet<Id>();
        }
    }

    public boolean hasQualifiedTypeCons(Id name) {
        Option<APIName> optApi = name.getApiName();
        if (optApi.isNone()) {
            bug(name, "Expected to have an API name.");
            return false;
        }
        APIName api = optApi.unwrap();
        if (_filteredGlobalEnv.definesApi(api)) {
            name = NodeFactory.makeLocalId(name);
            return _filteredGlobalEnv.api(api).typeConses().containsKey(name);
        } else {
            return false;
        }
    }

    public boolean hasQualifiedVariable(Id name) {
        Option<APIName> optApi = name.getApiName();
        if (optApi.isNone()) bug(name, "Expected to have an API name.");
        APIName api = optApi.unwrap();
        if (_filteredGlobalEnv.definesApi(api)) {
            name = NodeFactory.makeLocalId(name);
            return _filteredGlobalEnv.api(api).variables().containsKey(name);
        } else {
            return false;
        }
    }

    public boolean hasQualifiedFunction(Id name) {
        Option<APIName> optApi = name.getApiName();
        if (optApi.isNone()) bug(name, "Expected to have an API name.");
        APIName api = optApi.unwrap();
        if (_filteredGlobalEnv.definesApi(api)) {
            name = NodeFactory.makeLocalId(name);
            return _filteredGlobalEnv.api(api).functions().containsFirst(name);
        } else {
            return false;
        }
    }

    public boolean hasQualifiedGrammar(Id name) {
        Option<APIName> optApi = name.getApiName();
        if (optApi.isNone()) bug(name, "Expected to have an API name.");
        APIName api = optApi.unwrap();
        if (_filteredGlobalEnv.definesApi(api)) {
            name = NodeFactory.makeLocalId(name);
            return _filteredGlobalEnv.api(api).grammars().containsKey(name.getText());
        } else {
            return false;
        }
    }

    public TypeConsIndex typeConsIndex(final Id name) {
        Option<APIName> api = name.getApiName();
        APIName actualApi;
        Id actualName = ignoreApi(name);
        // If no API in name or it's the current API, use its own typeCons.
        // Otherwise, try to find the API in the global env and use its typeCons.
        CompilationUnit ast = _current.ast();
        if (api.isNone()) {
            actualApi = ast.getName();
        } else {
            actualApi = api.unwrap();
        }
        if (api.isNone() || (ast instanceof Api && ast.getName().equals(actualApi)) ||
            (ast instanceof Component && ((Component) ast).getExports().contains(actualApi))) {
            Map<Id, TypeConsIndex> typeConses = _current.typeConses();
            if (typeConses.keySet().contains(actualName)) return typeConses.get(actualName);
                //             System.err.println("Lookup of "+name+" in current api was null!\n  Trying qualified lookup, api = "+api);
            else return null;
        }

        // By this point it should be okay to use the unfiltered environment because this
        // method should only be called after the name is disambiguated.
        Map<Id, TypeConsIndex> typeConses = _originalGlobalEnv.api(actualApi).typeConses();
        if (typeConses.keySet().contains(actualName)) {
            return typeConses.get(actualName);
        }
        //         System.err.println("Still couldn't find "+name);
        else return null;
    }

    private Id ignoreApi(Id id) {
        return NodeFactory.makeId(NodeUtil.getSpan(id), id.getText());
    }

    public Option<GrammarIndex> grammarIndex(final Id name) {
        String uqname = name.getText();
        if (name.getApiName().isSome()) {
            APIName n = name.getApiName().unwrap();
            if (_filteredGlobalEnv.definesApi(n)) {
                return Option.some(_filteredGlobalEnv.api(n).grammars().get(uqname));
            } else {
                return Option.none();
            }
        }
        if (_current instanceof ApiIndex) {
            return Option.some(((ApiIndex) _current).grammars().get(uqname));
        } else {
            _errors.add(StaticError.make("Attempt to get grammar definition from a component: " + name, name));
            return Option.none();
        }
    }

    /**
     * Returns API names in the given list of exports that can be imported implicitly.
     * This helps, for example, when compiling FortressLibrary, since that is one API that
     * is normally imported implicitly.
     */
    private static Set<APIName> findWellKnownExports(List<APIName> exports) {
        return IterUtil.fold(exports,
                             new HashSet<APIName>(),
                             new Lambda2<HashSet<APIName>, APIName, HashSet<APIName>>() {
                                 public HashSet<APIName> value(HashSet<APIName> arg0, APIName api) {
                                     if (WELL_KNOWN_APIS.contains(api.getText())) {
                                         arg0.add(api);
                                     }
                                     return arg0;
                                 }
                             });
    }

    private Map<APIName, ApiIndex> filterApis(Map<APIName, ApiIndex> apis, Component comp) {
        Set<APIName> dont_import = findWellKnownExports(comp.getExports());
        return filterApis(Collections.unmodifiableMap(apis), comp.getImports(), dont_import);
    }

    private Map<APIName, ApiIndex> filterApis(Map<APIName, ApiIndex> apis, Api api) {
        // Insert 'this' api as an implicit import. This kind of strange, but the grammars
        // need them at a minimum.
        Import this_api_import = NodeFactory.makeImportStar(NodeFactory.makeSpan(api),
                                                            Option.<String>none(),
                                                            api.getName(),
                                                            Collections.<IdOrOpOrAnonymousName>emptyList());
        return filterApis(Collections.unmodifiableMap(apis),
                          Useful.concat(Collections.singletonList(this_api_import),
                                        api.getImports()),
                          Collections.<APIName>emptySet());
    }

    private static <K, T> Map<K, T> filterMap(Map<K, T> map, Set<? super K> set, Predicate<K> pred) {

        Map<K, T> result = new HashMap<K, T>();
        for (Map.Entry<K, T> entry : map.entrySet()) {
            if (pred.contains(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    private static <K, T> Map<K, T> removeHelper(Map<K, T> map, final Set<? super K> set) {
        Predicate<K> pred = new Predicate<K>() {
            public boolean contains(K arg1) {
                return !set.contains(arg1);
            }
        };

        return filterMap(map, set, pred);
    }

    private static ApiIndex remove(ApiIndex index, final Set<IdOrOpOrAnonymousName> exceptions_) {

        Predicate2<IdOrOpOrAnonymousName, Function> pred = new Predicate2<IdOrOpOrAnonymousName, Function>() {

            public boolean contains(IdOrOpOrAnonymousName arg0, Function arg1) {
                return !exceptions_.contains(arg0);
            }

        };

        return new ApiIndex((Api) index.ast(),
                            removeHelper(index.variables(), exceptions_),
                            new FilteredRelation<IdOrOpOrAnonymousName, Function>(index.functions(), pred),
                            // Parametric operators are parameterized by their names; they can't be filtered
                            // in any straightforward way.
                            // TODO: Do we need to attach lists of filtered operators to suppress
                            // matching of certain parametric operators? EricAllen 12/18/2008
                            index.parametricOperators(),
                            removeHelper(index.typeConses(), exceptions_),
                            removeHelper(index.dimensions(), exceptions_),
                            removeHelper(index.units(), exceptions_),
                            index.grammars(),
                            index.modifiedDate());
    }

    private static <K, T> Map<K, T> keepHelper(Map<K, T> map, final Set<? super K> set) {
        Predicate<K> pred = new Predicate<K>() {
            public boolean contains(K arg1) {
                return set.contains(arg1);
            }
        };

        return filterMap(map, set, pred);
    }

    /** Add into the aliases map any aliased indices from the relation. */
    private <V> void addAliasesFrom(Relation<IdOrOpOrAnonymousName, V> relation,
                                    Set<AliasedSimpleName> aliases) {
        for (AliasedSimpleName alias : aliases) {
            if (alias.getAlias().isSome()) {
                IdOrOpOrAnonymousName oldFirst = alias.getName();
                IdOrOpOrAnonymousName newFirst = alias.getAlias().unwrap();
                if (relation.containsFirst(oldFirst)) {
                    _aliases.put(newFirst, oldFirst);
                }
            }
        }
    }

    /** Add into the aliases map any aliased indices from the given map. */
    private <V> void addAliasesFrom(Map<? extends Id, V> map,
                                    Set<AliasedSimpleName> aliases) {
        for (AliasedSimpleName alias : aliases) {
            if (alias.getName() instanceof Id && alias.getAlias().isSome()) {
                Id oldFirst = (Id) alias.getName();
                Id newFirst = (Id) alias.getAlias().unwrap();
                if (map.containsKey(oldFirst)) {
                    _aliases.put(newFirst, oldFirst);
                }
            }
        }
    }

    private <V> Map<Id, V> aliasIds(Map<Id, V> allowed, Set<AliasedSimpleName> aliased) {
        Map<Id, V> result = new HashMap<Id, V>();
        result.putAll(allowed);

        for (AliasedSimpleName alias : aliased) {
            if (alias.getAlias().isSome()) {
                IdOrOpOrAnonymousName oldFirst = alias.getName();
                IdOrOpOrAnonymousName newFirst = alias.getAlias().unwrap();
                if (result.containsValue(oldFirst)) {
                    if (!(newFirst instanceof Id)) {
                        _errors.add(StaticError.make(
                                "Attempt to alias variable " + oldFirst + " with invalid variable name:" + newFirst,
                                NodeUtil.getSpan(alias)));
                    }
                }
            }
        }
        return result;
    }

    private ApiIndex keep(ApiIndex index,
                          final Set<IdOrOpOrAnonymousName> allowed_,
                          final Set<AliasedSimpleName> aliased_) {

        // Build up relation of Function indices based on names that are allowable.
        Relation<IdOrOpOrAnonymousName, Function> allowedFunctions =
                new IndexedRelation<IdOrOpOrAnonymousName, Function>(false);
        for (Pair<IdOrOpOrAnonymousName, Function> entry : index.functions()) {
            IdOrOpOrAnonymousName name = entry.first();
            Function function = entry.second();
            if (name instanceof Op) {
                String op = ((Op)name).getText();
                for (IdOrOpOrAnonymousName f : allowed_) {
                    if (f instanceof Op && ((Op)f).getText().equals(op))
                        allowedFunctions.add(name, function);
                }
            } else if (allowed_.contains(name)) {
                allowedFunctions.add(name, function);
            }
        }
        
        // Build up map of TypeConsIndex based on names that are allowable.
        Map<Id, TypeConsIndex> allowedTypeConses =
                new HashMap<Id, TypeConsIndex>(index.typeConses().size());
        for (Map.Entry<Id, TypeConsIndex> entry : index.typeConses().entrySet()) {
            if (allowed_.contains(entry.getKey())) {
                allowedTypeConses.put(entry.getKey(), entry.getValue());
            }
        }
        
        // Add into the aliases map any aliases for functions and type conses.
        addAliasesFrom(allowedFunctions, aliased_);
        addAliasesFrom(allowedTypeConses, aliased_);

        return new ApiIndex((Api) index.ast(),
                            aliasIds(keepHelper(index.variables(), allowed_), aliased_),
                            allowedFunctions,

                            // Parametric operators are parameterized by their names; they can't be filtered
                            // in any straightforward way.
                            // TODO: Do we need to attach lists of filtered operators to suppress
                            // matching of certain parametric operators? EricAllen 12/18/2008
                            index.parametricOperators(),
                            allowedTypeConses,
                            aliasIds(keepHelper(index.dimensions(), allowed_), aliased_),
                            aliasIds(keepHelper(index.units(), allowed_), aliased_),
                            index.grammars(),
                            index.modifiedDate());
    }

    /**
     * Filter out whole apis or parts of apis based on the imports of a component or
     * api. FortressBuiltin and AnyType are always imported, and FortressLibrary is only
     * imported implicitly if it is not imported explicitly. If {@code do_not_import} contains
     * api names, those apis will not be imported no matter what.
     */
    private Map<APIName, ApiIndex> filterApis(final Map<APIName, ApiIndex> apis,
                                              List<Import> imports,
                                              Set<APIName> do_not_import) {
        final Map<APIName, Set<IdOrOpOrAnonymousName>> exceptions = new HashMap<APIName, Set<IdOrOpOrAnonymousName>>();
        final Map<APIName, Set<IdOrOpOrAnonymousName>> allowed = new HashMap<APIName, Set<IdOrOpOrAnonymousName>>();
        final Map<APIName, Set<AliasedSimpleName>> aliases = new HashMap<APIName, Set<AliasedSimpleName>>();

        NodeDepthFirstVisitor<Boolean> import_visitor = new NodeDepthFirstVisitor<Boolean>() {
            // Do nothing for template-related imports
            @Override
            public Boolean defaultCase(Node that) {
                return false;
            }

            @Override
            public Boolean forImportApi(ImportApi that) {
                Boolean implib = true;
                for (AliasedAPIName api : that.getApis()) {
                    APIName name = api.getApiName();
                    if (name.getText().equals(WellKnownNames.fortressLibrary())) implib = false;
                    if (!exceptions.containsKey(name)) exceptions.put(name, new HashSet<IdOrOpOrAnonymousName>());
                }
                return implib;
            }

            @Override
            public Boolean forImportNames(final ImportNames that) {
                final APIName name = that.getApiName();

                // Handle aliased names
                Lambda<AliasedSimpleName, IdOrOpOrAnonymousName> lambda =
                new Lambda<AliasedSimpleName, IdOrOpOrAnonymousName>() {
                    public IdOrOpOrAnonymousName value(AliasedSimpleName arg0) {
                        //                                  System.err.println("aliased name: " + arg0.getName());
                        if (aliases.containsKey(name)) {
                            aliases.get(name).add(arg0);
                        } else {
                            aliases.put(name, Useful.set(arg0));
                        }
                        // Check whether the imported name is declared in the API.
                        IdOrOpOrAnonymousName imported_name = arg0.getName();
                        //    System.err.println("Looking up API name " + name);
                        if (!apis.containsKey(name)) {
                            _errors.add(StaticError.make(errorMsg("Reference to API ", name,
                                                                  " cannot be resolved."),
                                                         that));
                        } else if (!apis.get(name).declared(imported_name)) {
                            _errors.add(StaticError.make(errorMsg("Attempt to import ", imported_name,
                                                                  " from the API ", name,
                                                                  "\n    which does not declare ",
                                                                  imported_name + "."),
                                                         that));
                        }
                        return imported_name;
                    }
                };
                final List<IdOrOpOrAnonymousName> names =
                    CollectUtil.makeList(IterUtil.map(that.getAliasedNames(), lambda));
                //    System.out.println("names: " + names);

                if (allowed.containsKey(name)) allowed.get(name).addAll(names);
                else allowed.put(name, new HashSet<IdOrOpOrAnonymousName>(names));
                return !name.getText().equals(WellKnownNames.fortressLibrary());
            }

            @Override
            public Boolean forImportStar(ImportStar that) {
                APIName name = that.getApiName();
                if (exceptions.containsKey(name)) exceptions.get(name).addAll(that.getExceptNames());
                else exceptions.put(name, new HashSet<IdOrOpOrAnonymousName>(that.getExceptNames()));
                return !name.getText().equals(WellKnownNames.fortressLibrary());
            }
        };

        // Visit each import, populating the exceptions, allowed, and aliases maps
        Iterable<Boolean> temp = IterUtil.map(imports, import_visitor);
        boolean import_library = true;
        for (Boolean t : temp) {
            import_library &= t;
        }

        // Created filters for ApiIndex in apis
        Map<APIName, ApiIndex> result = new HashMap<APIName, ApiIndex>();
        for (Map.Entry<APIName, ApiIndex> api : apis.entrySet()) {
            // TODO: Report an error on conflicting import statements
            APIName name = api.getKey();
            ApiIndex index = api.getValue();


            if (do_not_import.contains(name)) {
                // Do nothing! this is a set of things we must not import no matter what.
            } else if (exceptions.containsKey(name)) {
                Set<IdOrOpOrAnonymousName> exceptions_ = exceptions.get(name);
                result.put(name, remove(index, exceptions_));
            } else if (allowed.containsKey(name)) {
                Set<IdOrOpOrAnonymousName> allowed_ = allowed.get(name);
                Set<AliasedSimpleName> aliased_ = aliases.get(name);
                //    System.err.println("allowed API: " + name);
                //    System.err.println("allowed index: " + index);
                //    System.err.println("allowed names: " + allowed_);
                //    System.err.println("aliased names: " + aliased_);
                result.put(name, keep(index, allowed_, aliased_));
            } else if (name.getText().equals(WellKnownNames.fortressBuiltin())) {
                // Fortress builtin is always implicitly imported
                result.put(name, index);
            } else if (name.getText().equals(WellKnownNames.anyTypeLibrary())) {
                // For now AnyType needs to be implicitly imported
                result.put(name, index);
            } else if (name.getText().equals(WellKnownNames.fortressLibrary()) && import_library) {
                // FortressLibrary is only imported implicitly if nothing from it is imported explicitly
                result.put(name, index);
            }
        }

        // Now handle aliases

//                         System.err.println("result");
//                         System.err.println(result);
//                         System.err.println("end result");
        return result;
    }
}
