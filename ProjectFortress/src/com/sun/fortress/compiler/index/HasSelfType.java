/*******************************************************************************
 Copyright 2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.index;

import com.sun.fortress.nodes.*;
import com.sun.fortress.compiler.typechecker.StaticTypeReplacer;

import edu.rice.cs.plt.tuple.Option;
import java.util.List;

/**
 * A Functional index that occurs in a trait. We need to be able to get the
 * static parameters on the declaring trait and get the type of `self`.
 */
public interface HasSelfType {
    public Node ast();
    public Id declaringTrait();
    public Option<SelfType> selfType();
    public int selfPosition();
    public List<StaticParam> traitStaticParameters();
    public HasSelfType instantiateTraitStaticParameters(List<StaticParam> params, List<StaticArg> args);
    public HasSelfType instantiateTraitStaticParameters(List<StaticParam> params, StaticTypeReplacer str);
    public boolean declarerIsObject();
}
