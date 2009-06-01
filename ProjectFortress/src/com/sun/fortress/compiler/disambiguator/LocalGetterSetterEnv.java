package com.sun.fortress.compiler.disambiguator;

import java.util.Collections;
import java.util.Set;

import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdOrOp;

import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.tuple.Option;

public class LocalGetterSetterEnv extends DelegatingNameEnv {
	
	private Set<Id> _gettersAndSetters;

    protected LocalGetterSetterEnv(NameEnv parent, Set<Id> gettersAndSetters) {
		super(parent);
		_gettersAndSetters = gettersAndSetters;
	}

	@Override
	public Set<IdOrOp> explicitFunctionNames(IdOrOp name) {
		if(_gettersAndSetters.contains(name)){
			return CollectUtil.singleton(name);
		}
		else{
		    return super.explicitFunctionNames(name);
	    }
	}

	@Override
	public Set<IdOrOp> unambiguousFunctionNames(IdOrOp name) {
		if(_gettersAndSetters.contains(name)){
			return CollectUtil.singleton(name);
		}
		else{
		    return super.unambiguousFunctionNames(name);
	    }
	}
    
}
