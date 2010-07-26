/*******************************************************************************
    Copyright 2010 Sun Microsystems, Inc.,
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

package com.sun.fortress.compiler.phases;

import com.sun.fortress.compiler.AnalyzeResult;
import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.exceptions.MultipleStaticError;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.scala_src.typechecker.IndexBuilder;
import com.sun.fortress.useful.Debug;
import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.tuple.Option;

public class EmptyPhase extends Phase {

    private final Iterable<Api> apis;
    private final Iterable<Component> components;

    public EmptyPhase(GlobalEnvironment env, Iterable<Api> apis, Iterable<Component> components, long lastModified) {
        super(null);
        this.env = env;
        this.apis = apis;
        this.components = components;
        this.lastModified = lastModified;
    }

    @Override
    public AnalyzeResult execute() throws StaticError {
        Debug.debug(Debug.Type.FORTRESS, 1, "Start phase Empty");
        IndexBuilder.ApiResult apiIndex = IndexBuilder.buildApis(apis, env, lastModified);
        // Index building might fail if imports or comprises clauses contain unresolvable types.
        if (!apiIndex.isSuccessful()) {
            throw new MultipleStaticError(apiIndex.errors());
        }

        IndexBuilder.ComponentResult componentIndex = IndexBuilder.buildComponents(components, lastModified);

        // Index building might fail if imports, exports, or comprises
        // clauses contain unresolvable types.
        if (!componentIndex.isSuccessful()) {
            throw new MultipleStaticError(componentIndex.errors());
        }

        return new AnalyzeResult(apiIndex.apis(),
                                 componentIndex.components(),
                                 CollectUtil.union(CollectUtil.asSet(apiIndex.errors()),
                                                   CollectUtil.asSet(componentIndex.errors())));
    }

}
