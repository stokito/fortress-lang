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

package com.sun.fortress.compiler.typechecker;

import com.sun.fortress.nodes.*;
import edu.rice.cs.plt.tuple.Option;
import java.util.List;

import static edu.rice.cs.plt.tuple.Option.*;

public class WhereClauseEnv extends StaticParamEnv {
    private List<StaticParam> params;
    private WhereClause whereClause;
    private StaticParamEnv parent;
    
    public WhereClauseEnv(List<StaticParam> _params, WhereClause _whereClause, StaticParamEnv _parent) {
        params = _params;
        whereClause = _whereClause;
        parent = _parent;
    }
    
    private SimpleName paramName(StaticParam param) {
        // Both OperatorParams and IdStaticParams have name fields, but they
        // differ in the types of the fields. 
        if (param instanceof OperatorParam) { 
            return ((OperatorParam)param).getName(); 
        } else { // param instanceof IdStaticParam
            return ((IdStaticParam)param).getName(); 
        }
    }

    public Option<StaticParam> binding(SimpleName name) {
        for (StaticParam param : params) {
            if (name.equals(paramName(param))) { return wrap(param); }
        }
        for (WhereBinding entry : whereClause.getBindings()) {
            if (entry instanceof WhereType && name.equals(((WhereType)entry).getName())) { 
                return Option.<StaticParam>wrap(new SimpleTypeParam(entry.getSpan(), 
                                                                    entry.getName(), 
                                                                    ((WhereType)entry).getSupers()));
            }
        }
        return parent.binding(name);
    }
}
