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
