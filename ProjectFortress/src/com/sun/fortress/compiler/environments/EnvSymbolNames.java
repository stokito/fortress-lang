package com.sun.fortress.compiler.environments;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.rice.cs.plt.collect.IndexedRelation;
import edu.rice.cs.plt.collect.Relation;

/**
 * This data structure maps a Fortress namespace onto a set of top-level declarations.
 */
public class EnvSymbolNames {
	
	private Map<EnvironmentClass, Set<String>> _names;
	
	public EnvSymbolNames() {
		_names = new HashMap<EnvironmentClass, Set<String>>();
		for(EnvironmentClass eClass : EnvironmentClass.values()) {
			_names.put(eClass, new HashSet<String>());
		}
	}
	
	public void add(EnvironmentClass eClass, String name) {
		Set<String> eClassNames = _names.get(eClass);
		eClassNames.add(name);
	}

	public Set<String> getSymbolNames(EnvironmentClass eClass) {
		return _names.get(eClass);
	}
	
	public Relation<String, Integer> makeHashCodeRelation(EnvironmentClass eClass) {
		Set<String> eClassNames = _names.get(eClass);
		Relation<String, Integer> retval = new IndexedRelation<String,Integer>();
		for(String name : eClassNames) {
			retval.add(name, new Integer(name.hashCode()));			
		}
		return retval;
	}

}
