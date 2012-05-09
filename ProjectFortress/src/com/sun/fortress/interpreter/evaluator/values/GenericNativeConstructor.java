/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.values;

import com.sun.fortress.interpreter.evaluator.BuildNativeEnvironment;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.types.FTypeObject;
import com.sun.fortress.nodes.ObjectConstructor;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes_util.NodeFactory;
import edu.rice.cs.plt.tuple.Option;

import java.util.List;

public class GenericNativeConstructor extends GenericConstructor {

    private String name;

    public GenericNativeConstructor(Environment env, ObjectConstructor odefOrDecl, String name) {
        super(env, odefOrDecl, NodeFactory.makeId(NodeFactory.interpreterSpan, name));
        this.name = name;
    }

    @Override
    protected Constructor constructAConstructor(Environment clenv, FTypeObject objectType, Option<List<Param>> params) {
        return BuildNativeEnvironment.nativeConstructor(clenv, objectType, odefOrDecl, name);
    }

}
