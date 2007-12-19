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

package com.sun.fortress.interpreter.glue;

import com.sun.fortress.nodes_util.NodeFactory;
import java.util.Collections;
import java.util.List;
import edu.rice.cs.plt.tuple.Option;

import com.sun.fortress.nodes.Applicable;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.SimpleName;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.WhereClause;
import com.sun.fortress.useful.NI;
import com.sun.fortress.useful.Useful;


public class NativeApplicable implements Applicable {
    String name;
    SimpleName fnName;

    NativeApplicable(String name) {
        this.name = name;
        this.fnName = NodeFactory.makeId(name);
    }

    public String toString() {
        return "native " + name;
    }

    public Expr getBody() {
        // TODO Auto-generated method stub
        return null;
    }

    public List<Param> getParams() {
        // TODO Auto-generated method stub
        return NI.ni();
    }

    public Option<Type> getReturnType() {
        // TODO Auto-generated method stub
        return NI.ni();
    }

    public SimpleName getName() {
        return fnName;
    }

    public String at() {
        return "Native stub for " + name;
    }

    public String stringName() {
        return name;
    }

    public List<StaticParam> getStaticParams() {
        // TODO Auto-generated method stub
        return Collections.<StaticParam>emptyList();
    }

    public List<WhereClause> getWhere() {
        return Collections.<WhereClause>emptyList();
    }
}
