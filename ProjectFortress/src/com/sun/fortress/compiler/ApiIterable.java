/*******************************************************************************
    Copyright 2008,2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

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
