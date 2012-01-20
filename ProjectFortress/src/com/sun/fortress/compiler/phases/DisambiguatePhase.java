/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.phases;

import com.sun.fortress.compiler.AnalyzeResult;
import com.sun.fortress.compiler.Disambiguator;
import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.exceptions.MultipleStaticError;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.scala_src.typechecker.IndexBuilder;
import com.sun.fortress.useful.Debug;
import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.iter.IterUtil;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DisambiguatePhase extends Phase {

    public DisambiguatePhase(Phase parentPhase) {
        super(parentPhase);
    }

    private Iterable<Api> apiAsts(Map<APIName, ApiIndex> apis) {
        Set<Api> result = new HashSet<Api>();
        for (ApiIndex api : apis.values()) {
            result.add((Api) api.ast());
        }
        return result;
    }

    @Override
    public AnalyzeResult execute() throws StaticError {
        Debug.debug(Debug.Type.FORTRESS, 1, "Start phase Disambiguate");
        AnalyzeResult previous = parentPhase.getResult();

        GlobalEnvironment rawApiEnv = new GlobalEnvironment.FromMap(CollectUtil.union(env.apis(), previous.apis()));

        // Rewrite all API ASTs so they include only fully qualified names,
        // relying on the rawApiEnv constructed in the previous step. Note that,
        // after this step, the rawApiEnv is stale and needs to be rebuilt with
        // the new API ASTs.
        Disambiguator.ApiResult apiDR = Disambiguator.disambiguateApis(apiAsts(previous.apis()), rawApiEnv);

        if (!apiDR.isSuccessful()) {
            throw new MultipleStaticError(apiDR.errors());
        }

        // Rebuild ApiIndices.
        IndexBuilder.ApiResult apiIR = IndexBuilder.buildApis(apiDR.apis(), rawApiEnv, lastModified);

        if (!apiIR.isSuccessful()) {
            throw new MultipleStaticError(apiIR.errors());
        }

        // Rebuild GlobalEnvironment.
        GlobalEnvironment apiEnv = new GlobalEnvironment.FromMap(CollectUtil.union(rawApiEnv.apis(), apiIR.apis()));

        Disambiguator.ComponentResult componentDR = Disambiguator.disambiguateComponents(previous.componentIterator(),
                                                                                         apiEnv,
                                                                                         previous.components());
        if (!componentDR.isSuccessful()) {
            throw new MultipleStaticError(componentDR.errors());
        }

        // Rebuild ComponentIndices.
        IndexBuilder.ComponentResult componentsDone = IndexBuilder.buildComponents(componentDR.components(),
                                                                                   lastModified);
        if (!componentsDone.isSuccessful()) {
            throw new MultipleStaticError(componentsDone.errors());
        }

        return new AnalyzeResult(apiIR.apis(),
                                 componentsDone.components(),
                                 IterUtil.<StaticError>empty());

    }

}
