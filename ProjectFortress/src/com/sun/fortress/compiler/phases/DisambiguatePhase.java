/*******************************************************************************
 Copyright 2009 Sun Microsystems, Inc.,
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
import com.sun.fortress.compiler.Disambiguator;
import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.IndexBuilder;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.exceptions.MultipleStaticError;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Api;
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

        // Build a new GlobalEnvironment consisting of all APIs in a global
        // repository combined with all APIs that have been processed in the
        // previous step. 
        //        GlobalEnvironment rawApiEnv = new GlobalEnvironment.FromMap(CollectUtil
        //                                                            .union(repository.apis(),
        //                                                                    CollectUtil.union(env.apis(),
        //                                                                                      previous.apis())));

        GlobalEnvironment rawApiEnv = new GlobalEnvironment.FromMap(CollectUtil.union(env.apis(),
                previous.apis()));

        // GlobalEnvironment rawApiEnv = new GlobalEnvironment.FromMap(previous.apis());

//          System.out.println("rawApiEnv:");
//          rawApiEnv.print();
//          System.out.println("end rawApiEnv");


        // Rewrite all API ASTs so they include only fully qualified names,
        // relying on the rawApiEnv constructed in the previous step. Note that,
        // after this step, the rawApiEnv is stale and needs to be rebuilt with
        // the new API ASTs.
        // 
        // HACK: Disambiguate *all* APIs. It should be possible to disambiguate
        // only those APIs relevant to the current compilation, and pass along the 
        // disambiguated ApiIndices to subsequent phases. However, it appears
        // that a single compilation is making multiple calls to all phases, passing
        // only some CompilationUnits on each call, and not passing along results
        // of disambiguation from call to call. This needs to be fixed. 
        // EricAllen 7/8/2009
        Disambiguator.ApiResult apiDR =
                Disambiguator.disambiguateApis(apiAsts(previous.apis()),
                        rawApiEnv);

//         for (Api api : apiDR.apis()) { 
//             Nodes.printNode(api, "apiDR.");
//         }


        if (!apiDR.isSuccessful()) {
            throw new MultipleStaticError(apiDR.errors());
        }

        // Rebuild ApiIndices.
        IndexBuilder.ApiResult apiIR =
                IndexBuilder.buildApis(apiDR.apis(), rawApiEnv, lastModified);

//         for (ApiIndex api : apiIR.apis().values()) { 
//             Nodes.printNode(api.ast(), "apiIR.");
//         }

        if (!apiIR.isSuccessful()) {
            throw new MultipleStaticError(apiIR.errors());
        }

        // Rebuild GlobalEnvironment.
        GlobalEnvironment apiEnv =
                new GlobalEnvironment.FromMap(CollectUtil.union(rawApiEnv.apis(), apiIR.apis()));

//         System.err.println("DisambiguatePhase apiEnv");
//         apiEnv.print();
//         System.err.println("end DisambiguatePhase apiEnv");

        Disambiguator.ComponentResult componentDR =
                Disambiguator.disambiguateComponents(previous.componentIterator(),
                        apiEnv,
                        previous.components());
        if (!componentDR.isSuccessful()) {
            throw new MultipleStaticError(componentDR.errors());
        }

        // Rebuild ComponentIndices.
        IndexBuilder.ComponentResult componentsDone =
                IndexBuilder.buildComponents(componentDR.components(), lastModified);
        if (!componentsDone.isSuccessful()) {
            throw new MultipleStaticError(componentsDone.errors());
        }

        return new AnalyzeResult(apiIR.apis(),
                componentsDone.components(),
                IterUtil.<StaticError>empty(),
                previous.typeCheckerOutput());

    }

}
