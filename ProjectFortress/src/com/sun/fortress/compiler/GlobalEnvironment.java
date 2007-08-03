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

package com.sun.fortress.compiler;

import java.util.Map;
import com.sun.fortress.compiler.index.ApiIndex;

public class GlobalEnvironment {
    private Map<String, ApiIndex> _apis;
    
    public GlobalEnvironment(Map<String, ApiIndex> apis) { _apis = apis; }
    
    public boolean definesApi(String name) { return _apis.containsKey(name); }
    
    public ApiIndex api(String name) { return _apis.get(name); }
    
}
