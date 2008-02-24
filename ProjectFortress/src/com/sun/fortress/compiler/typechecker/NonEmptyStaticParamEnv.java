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

public class NonEmptyStaticParamEnv extends StaticParamEnv {
    private List<StaticParam> entries;
    private StaticParamEnv parent;

    public NonEmptyStaticParamEnv(List<StaticParam> _entries, StaticParamEnv _parent) {
        entries = _entries;
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
        for (StaticParam entry : entries) {
            if (name.equals(paramName(entry))) { return wrap(entry); }
        }
        return parent.binding(name);
    }
}
