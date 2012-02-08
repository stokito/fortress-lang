/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.phases;

import com.sun.fortress.compiler.AnalyzeResult;
import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.StaticChecker;
import com.sun.fortress.exceptions.MultipleStaticError;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.scala_src.typechecker.IndexBuilder;
import com.sun.fortress.useful.Debug;
import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;

public class TypeCheckPhase extends Phase {

    public TypeCheckPhase(Phase parentPhase) {
        super(parentPhase);
    }

    @Override
    public AnalyzeResult execute() throws StaticError {
        Debug.debug(Debug.Type.FORTRESS, 1, "Start phase TypeCheck");
        AnalyzeResult previous = parentPhase.getResult();

        IndexBuilder.ApiResult apiIndex = IndexBuilder.buildApis(previous.apiIterator(), this.env, lastModified);

        IndexBuilder.ComponentResult componentIndex = IndexBuilder.buildComponents(previous.componentIterator(),
                                                                                   lastModified);

        GlobalEnvironment apiEnv = new GlobalEnvironment.FromMap(CollectUtil.union(env.apis(), apiIndex.apis()));

        StaticChecker.ApiResult apiSR = StaticChecker.checkApis(apiIndex.apis(), apiEnv);

        if (!apiSR.isSuccessful()) {
            throw new MultipleStaticError(apiSR.errors());
        }

        StaticChecker.ComponentResult componentSR = StaticChecker.checkComponents(componentIndex.components(), env);

        if (!componentSR.isSuccessful()) {
            throw new MultipleStaticError(componentSR.errors());
        }

        return new AnalyzeResult(apiSR.apis(),
                                 componentSR.components(),
                                 IterUtil.<StaticError>empty(),componentSR.typeCheckers());
    }

}
