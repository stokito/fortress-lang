/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.phases;

import com.sun.fortress.compiler.AnalyzeResult;
import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.exceptions.MultipleStaticError;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.scala_src.typechecker.IndexBuilder;
import com.sun.fortress.syntax_abstractions.phases.GrammarRewriter;
import com.sun.fortress.useful.Debug;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.collect.CollectUtil;
import java.util.Collection;

public class GrammarPhase extends Phase {

    public GrammarPhase(Phase parentPhase) {
        super(parentPhase);
    }

    @Override
    public AnalyzeResult execute() throws StaticError {
        Debug.debug(Debug.Type.FORTRESS, 1, "Start phase GrammarPhase");
        AnalyzeResult previous = parentPhase.getResult();

        GlobalEnvironment apiEnv = new GlobalEnvironment.FromMap(CollectUtil.union(env.apis(), previous.apis()));

        Collection<Api> apis = GrammarRewriter.rewriteApis(previous.apis(), apiEnv);

        IndexBuilder.ApiResult apiDone = IndexBuilder.buildApis(apis, apiEnv, lastModified);
        if (!apiDone.isSuccessful()) {
            throw new MultipleStaticError(apiDone.errors());
        }

        return new AnalyzeResult(apiDone.apis(),
                                 previous.components(),
                                 IterUtil.<StaticError>empty());
    }
}
