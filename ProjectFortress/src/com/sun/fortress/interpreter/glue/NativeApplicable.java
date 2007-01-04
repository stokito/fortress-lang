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

import java.util.Collections;
import java.util.List;

import com.sun.fortress.interpreter.nodes.Applicable;
import com.sun.fortress.interpreter.nodes.Expr;
import com.sun.fortress.interpreter.nodes.FnName;
import com.sun.fortress.interpreter.nodes.Fun;
import com.sun.fortress.interpreter.nodes.None;
import com.sun.fortress.interpreter.nodes.Option;
import com.sun.fortress.interpreter.nodes.Param;
import com.sun.fortress.interpreter.nodes.StaticParam;
import com.sun.fortress.interpreter.nodes.TypeRef;
import com.sun.fortress.interpreter.nodes.WhereClause;
import com.sun.fortress.interpreter.useful.NI;


class NativeApplicable implements Applicable {
    String name;
    FnName fnName;

    NativeApplicable(String name) {
        this.name = name;
        this.fnName = new Fun(name);
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

    public Option<TypeRef> getReturnType() {
        // TODO Auto-generated method stub
        return NI.ni();
    }

    public FnName getFnName() {
        return fnName;
    }

    public String at() {
        return "Native stub for " + name;
    }

    public String stringName() {
        return name;
    }

    public Option<List<StaticParam>> getStaticParams() {
        // TODO Auto-generated method stub
        return new None<List<StaticParam>>();
    }

    public List<WhereClause> getWhere() {
        return Collections.<WhereClause>emptyList();
    }

}
