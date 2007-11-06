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
import com.sun.fortress.nodes.DottedName;
import com.sun.fortress.nodes_util.NodeUtil;

public class GlobalEnvironment {
    private Map<DottedName, ApiIndex> _apis;
    
    public GlobalEnvironment(Map<DottedName, ApiIndex> apis) { _apis = apis; }
    
    public Map<DottedName, ApiIndex> apis() { return _apis; }
    
    public boolean definesApi(DottedName name) { return _apis.containsKey(name); }
    
    public ApiIndex api(DottedName name) {
        ApiIndex result = _apis.get(name);
        if (result == null) {
            throw new IllegalArgumentException("Undefined API: " +
                                               NodeUtil.nameString(name));
        }
        else { return result; }
    }
    
    public void print() {
        for (DottedName name : apis().keySet()) {
            System.out.println(name);
        }
    }
}
