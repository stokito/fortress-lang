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

import java.util.Iterator;
import java.util.Map;

import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Api;

public final class ApiIterable implements Iterable<Api>  {
	
    private final Map<APIName, ApiIndex> _apis;	
    
    ApiIterable(Map<APIName, ApiIndex> apis) {
    	_apis = apis;
    }

	public Iterator<Api> iterator() {
		
		final Iterator<ApiIndex> apiIndexIterator = _apis.values().iterator();
		
		return new Iterator<Api>() {

			public boolean hasNext() {
				return apiIndexIterator.hasNext();
			}

			public Api next() {
				return (Api) apiIndexIterator.next().ast();
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
			
		};
	}
	
}
