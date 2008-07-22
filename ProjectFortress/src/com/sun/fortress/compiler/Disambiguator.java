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

package com.sun.fortress.compiler;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collection;

import com.sun.fortress.compiler.disambiguator.ExprDisambiguator;
import com.sun.fortress.compiler.disambiguator.NameEnv;
import com.sun.fortress.compiler.disambiguator.NonterminalDisambiguator;
import com.sun.fortress.compiler.disambiguator.SelfParamDisambiguator;
import com.sun.fortress.compiler.disambiguator.TopLevelEnv;
import com.sun.fortress.compiler.disambiguator.TypeDisambiguator;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.compiler.index.Function;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.AliasedAPIName;
import com.sun.fortress.nodes.AliasedSimpleName;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.GrammarDef;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.Import;
import com.sun.fortress.nodes.ImportApi;
import com.sun.fortress.nodes.ImportNames;
import com.sun.fortress.nodes.ImportStar;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeDepthFirstVisitor;
import com.sun.fortress.useful.Useful;
import com.sun.fortress.useful.Debug;

import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.collect.FilteredRelation;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.lambda.Lambda;
import edu.rice.cs.plt.lambda.Predicate;
import edu.rice.cs.plt.lambda.Predicate2;
import edu.rice.cs.plt.tuple.Option;

/**
 * Eliminates ambiguities in an AST that can be resolved solely by knowing what
 * kind of entity a name refers to.  This class specifically handles
 * the following:  
 * <ul>
 * <li>All names referring to APIs are made fully qualified (FnRefs
 *     and OpExprs may then contain lists of qualified names referring to
 *     multiple APIs).</li>
 * <li>VarRefs referring to functions become FnRefs with placeholders
 *     for implicit static arguments filled in (to be replaced later
 *     during type inference).</li> 
 * <li>VarRefs referring to getters, setters, or methods become FieldRefs.</li>
 * <li>VarRefs referring to methods, and that are juxtaposed with Exprs, become 
 *     MethodInvocations.</li>
 * <li>FieldRefs referring to methods, and that are juxtaposed with
 *     Exprs, become MethodInvocations.</li>
 * <li>FnRefs referring to methods, and that are juxtaposed with Exprs, become 
 *     MethodInvocations.</li>
 * <li>VarTypes referring to traits become TraitTypes (with 0
 *     arguments)</li> 
 * </ul>
 * 
 * Additionally, all name references that are undefined or used incorrectly are
 * treated as static errors.
 */
public class Disambiguator {
    private static final String FORTRESS_LIBRARY_NAME = "FortressLibrary";
	private static final String ANY_TYPE_API_NAME = "AnyType";
	private static final String FORTRESS_BUILTIN_NAME = "FortressBuiltin";

	/**
     * Disambiguate the names of nonterminals.
     */
    private static List<Api> disambiguateGrammarMembers(Collection<ApiIndex> apis,
            List<StaticError> errors,
            GlobalEnvironment globalEnv) {
        List<Api> results = new ArrayList<Api>();
        for (ApiIndex index : apis) {
            // ApiIndex index = globalEnv.api(api.getName());
            NameEnv env = new TopLevelEnv(globalEnv, index, errors);
            List<StaticError> newErrs = new ArrayList<StaticError>();
            NonterminalDisambiguator pd = new NonterminalDisambiguator(env, globalEnv, newErrs);
            Api pdResult = (Api) index.ast().accept(pd);
            results.add(pdResult);
            if (!newErrs.isEmpty()) { 
                errors.addAll(newErrs); 
            }
        }
        return results;
    }

    /** Result of {@link #disambiguateApis}. */
    public static class ApiResult extends StaticPhaseResult {
        private final Iterable<Api> _apis;

        public ApiResult(Iterable<Api> apis, 
                Iterable<? extends StaticError> errors) {
            super(errors);
            _apis = apis;
        }

        public Iterable<Api> apis() { return _apis; }
    }

    private static  Map<APIName, ApiIndex> filterApis(Map<APIName,ApiIndex> apis, Component comp) {
    	return filterApis(Collections.unmodifiableMap(apis), comp.getImports());
    }
    
    private static Map<APIName, ApiIndex> filterApis(Map<APIName, ApiIndex> apis, Api api) {
    	// Insert 'this' api as an implicit import. This kind of strange, but the grammars
    	// need them at a minimum.
    	Import this_api_import = new ImportStar(api.getName(), Collections.<IdOrOpOrAnonymousName>emptyList());
    	return filterApis(Collections.unmodifiableMap(apis), 
    			Useful.concat(Collections.singletonList(this_api_import), api.getImports()));
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
     * imported implicitly if it is not imported explicitly.
     */
    private static Map<APIName,ApiIndex> filterApis(Map<APIName, ApiIndex> apis, List<Import> imports) { 
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
					if(name.getText().equals(FORTRESS_LIBRARY_NAME))
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
				return !name.getText().equals(FORTRESS_LIBRARY_NAME);
			}

			@Override
			public Boolean forImportStar(ImportStar that) {
				APIName name = that.getApi();
				if( exceptions.containsKey(name) )
					exceptions.get(name).addAll(that.getExcept());
				else
					exceptions.put(name, new HashSet<IdOrOpOrAnonymousName>(that.getExcept()));
				return !name.getText().equals(FORTRESS_LIBRARY_NAME);
			}
    	};
    	
    	// Visit each import, populating the exceptions and allowed maps
    	Iterable<Boolean> temp=IterUtil.map(imports, import_visitor);
    	boolean importlibrary = true;
    	for(Boolean t : temp){
    		importlibrary&=t;
    	}
    	
    	// Created filters for ApiIndex in apis
    	Map<APIName, ApiIndex> result = new HashMap<APIName, ApiIndex>();
    	for( Map.Entry<APIName, ApiIndex> api : apis.entrySet() ) {
    		// TODO: Report an error on conflicting import statements
    		APIName name = api.getKey();
    		ApiIndex index = api.getValue();
    		
    		
    		if( exceptions.containsKey(name) ) {
    			Set<IdOrOpOrAnonymousName> exceptions_ = exceptions.get(name);
    			result.put(name, remove(index, exceptions_));
    		}
    		else if( allowed.containsKey(name) ) {
    			Set<IdOrOpOrAnonymousName> allowed_ = allowed.get(name);
    			result.put(name, keep(index, allowed_));
    		}
    		else if( name.getText().equals(FORTRESS_BUILTIN_NAME) ) {
    			// Fortress builtin is always implicitly imported
    			result.put(name, index);
    		}
    		else if( name.getText().equals(ANY_TYPE_API_NAME) ) {
    			// For now, AnyType is always implicitly imported.
    			result.put(name, index);
    		}
    		else if( name.getText().equals(FORTRESS_LIBRARY_NAME) && 
    				 importlibrary ) {
    			// Fortress Library is import implicitly if nothing else is imported
    			result.put(name, index);
    		}
    	}
    	return result;
    }
    
    /**
     * Disambiguate the given apis. To support circular references,
     * the apis should appear in the given environment.
     * @param apis_to_disambiguate Apis currently being disambiguated.
     * @param globalEnv The current global environment.
     * @param repository_apis Apis that already exist in the repository.
     */
    public static ApiResult disambiguateApis(Iterable<Api> apis_to_disambiguate,
            GlobalEnvironment globalEnv, Map<APIName, ApiIndex> repository_apis) {
          	
    	repository_apis = Collections.unmodifiableMap(repository_apis);
    	
        List<StaticError> errors = new ArrayList<StaticError>();
        
        // First, loop through apis disambiguating types.
        List<Api> new_apis = new ArrayList<Api>();
        for( Api api : apis_to_disambiguate ) {
        	 ApiIndex index = globalEnv.api(api.getName());
        	 
        	 Map<APIName,ApiIndex> filtered = filterApis(globalEnv.apis(), api);
             GlobalEnvironment filtered_global_env = new GlobalEnvironment.FromMap(filtered);
        	 NameEnv env = new TopLevelEnv(filtered_global_env, index, errors);
        	 
        	 Set<IdOrOpOrAnonymousName> onDemandImports = new HashSet<IdOrOpOrAnonymousName>();
             
             SelfParamDisambiguator self_disambig = new SelfParamDisambiguator();
             Api spd_result = (Api)api.accept(self_disambig);
             
             List<StaticError> newErrs = new ArrayList<StaticError>();
             TypeDisambiguator td = 
                 new TypeDisambiguator(env, onDemandImports, newErrs);
             Api tdResult = (Api) spd_result.accept(td);
             
             if( newErrs.isEmpty() )
            	 new_apis.add(tdResult);
             else 
             	errors.addAll(newErrs);
        }
        
        // Go no further if we couldn't disambiguate the types.
        if( errors.size() > 0 ) {
        	return new ApiResult(new_apis, errors);
        }
        
        // then, rebuild the indices
        IndexBuilder.ApiResult rebuilt_indx = IndexBuilder.buildApis(new_apis, System.currentTimeMillis());
        GlobalEnvironment new_global_env = new GlobalEnvironment.FromMap(CollectUtil.union(repository_apis,
        		rebuilt_indx.apis()));

        // Finally, disambiguate the expressions using the rebuild indices.
        List<Api> results = new ArrayList<Api>();
        for( Api api : new_apis ) {
        	ApiIndex index = new_global_env.api(api.getName());
        	
       	 	Map<APIName,ApiIndex> filtered = filterApis(new_global_env.apis(), api);
       	 	GlobalEnvironment filtered_global_env = new GlobalEnvironment.FromMap(filtered);
        	NameEnv env = new TopLevelEnv(filtered_global_env, index, errors);
        	
        	List<StaticError> newErrs = new ArrayList<StaticError>();
        	ExprDisambiguator ed = 
                new ExprDisambiguator(env, newErrs);
            Api edResult = (Api) api.accept(ed);
            if (newErrs.isEmpty())
            	results.add(edResult);
            else 
            	errors.addAll(newErrs);
        }
        
        IndexBuilder.ApiResult rebuilt_indx2 = IndexBuilder.buildApis(results, System.currentTimeMillis());
        GlobalEnvironment new_global_env2 = new GlobalEnvironment.FromMap(CollectUtil.union(repository_apis,
        		rebuilt_indx2.apis()));

        initializeGrammarIndexExtensions(rebuilt_indx2.apis().values());
        results = disambiguateGrammarMembers(rebuilt_indx2.apis().values(), errors, new_global_env2);
        return new ApiResult(results, errors);
    }

    private static Collection<? extends StaticError> initializeGrammarIndexExtensions(Collection<ApiIndex> apis) {
        List<StaticError> errors = new LinkedList<StaticError>();
        Map<String, GrammarIndex> grammars = new HashMap<String, GrammarIndex>();
        for (ApiIndex a2: apis) {
            for (Map.Entry<String, GrammarIndex> e: a2.grammars().entrySet()) {
                grammars.put(e.getKey(), e.getValue());
            }
        }
        for (ApiIndex a1: apis) {
            for (Map.Entry<String,GrammarIndex> e: a1.grammars().entrySet()) {
                Option<GrammarDef> og = e.getValue().ast();
                if (og.isSome()) {
                    List<GrammarIndex> ls = new LinkedList<GrammarIndex>();
                    for (Id n: og.unwrap().getExtends()) {
                        ls.add(grammars.get(n.getText()));
                    }
                    Debug.debug( Debug.Type.SYNTAX, 3, "Grammar " + e.getKey() + " extends " + ls );
                    e.getValue().setExtended(ls);
                } else {
                    Debug.debug( Debug.Type.SYNTAX, 3, "Grammar " + e.getKey() + " has no ast" );
                }
            }
        }
        return errors;
    }

    /** Result of {@link #disambiguateComponents}. */
    public static class ComponentResult extends StaticPhaseResult {
        private final Iterable<Component> _components;
        public ComponentResult(Iterable<Component> components,
                Iterable<? extends StaticError> errors) {
            super(errors);
            _components = components;
        }
        public Iterable<Component> components() { return _components; }
    }

    /** Disambiguate the given components. */
    public static ComponentResult disambiguateComponents(Iterable<Component> components,
                                                         GlobalEnvironment globalEnv,
                                                         Map<APIName, ComponentIndex> indices) {
    	
    	
        List<Component> results = new ArrayList<Component>();
        List<StaticError> errors = new ArrayList<StaticError>();
        
        // First, disambiguate the types
        List<Component> new_comps = new ArrayList<Component>();
        for (Component comp : components) {
            ComponentIndex index = indices.get(comp.getName());
            if (index == null) {
                throw new IllegalArgumentException("Missing component index");
            }
            
            // Filter env based on what this component imports
       	 	Map<APIName,ApiIndex> filtered = filterApis(globalEnv.apis(), comp);
       	 	GlobalEnvironment filtered_global_env = new GlobalEnvironment.FromMap(filtered);
            NameEnv env = new TopLevelEnv(filtered_global_env, index, errors);
            Set<IdOrOpOrAnonymousName> onDemandImports = new HashSet<IdOrOpOrAnonymousName>();

            SelfParamDisambiguator self_disambig = new SelfParamDisambiguator();
            Component spdResult = (Component) comp.accept(self_disambig);
            
            List<StaticError> newErrs = new ArrayList<StaticError>();
            TypeDisambiguator td = 
                new TypeDisambiguator(env, onDemandImports, newErrs);
            Component tdResult = (Component) spdResult.accept(td);
            if (newErrs.isEmpty())
            	new_comps.add(tdResult);
            else
            	errors.addAll(newErrs);
        }
        
        // Then, rebuild the component indices based on disambiguated types
        IndexBuilder.ComponentResult new_comp_ir =
        	IndexBuilder.buildComponents(new_comps, System.currentTimeMillis());
        
        // Finall, disambiguate the expressions
        for( Component comp : new_comps ) {
        	ComponentIndex index = new_comp_ir.components().get(comp.getName());
        	if (index == null) {
                throw new IllegalArgumentException("Missing component index");
            }
        	
        	// Filter evn based on what this component imports
       	 	Map<APIName,ApiIndex> filtered = filterApis(globalEnv.apis(), comp);
       	 	GlobalEnvironment filtered_global_env = new GlobalEnvironment.FromMap(filtered);
        	NameEnv env = new TopLevelEnv(filtered_global_env, index, errors);
            
            List<StaticError> newErrs = new ArrayList<StaticError>();
            ExprDisambiguator ed = 
                new ExprDisambiguator(env, //onDemandImports, 
                		newErrs);
            Component edResult = (Component) comp.accept(ed);
            if (newErrs.isEmpty())
            	results.add(edResult);
            else 
            	errors.addAll(newErrs);
        }
        return new ComponentResult(results, errors);
    }
}
