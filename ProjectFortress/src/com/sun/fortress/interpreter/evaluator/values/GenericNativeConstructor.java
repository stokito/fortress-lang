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

package com.sun.fortress.interpreter.evaluator.values;

import java.util.List;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.BuildNativeEnvironment;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.types.FTypeGeneric;
import com.sun.fortress.interpreter.evaluator.types.FTypeObject;
import com.sun.fortress.nodes.GenericWithParams;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes_util.NodeFactory;

import edu.rice.cs.plt.tuple.Option;

public class GenericNativeConstructor extends GenericConstructor {

    private String name;

    public GenericNativeConstructor(BetterEnv env,
            GenericWithParams odefOrDecl, String name) {
        super(env, odefOrDecl, NodeFactory.makeId(name));
        this.name = name;
    }

    @Override
    protected Constructor constructAConstructor(BetterEnv clenv,
            FTypeObject objectType,
            Option<List<Param>> params) {
        return BuildNativeEnvironment.nativeConstructor(clenv, objectType, odefOrDecl, name);
      }

}
