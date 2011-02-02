/*******************************************************************************
 Copyright 2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.index;

import com.sun.fortress.nodes.SelfType;
import com.sun.fortress.nodes.Id;
import edu.rice.cs.plt.tuple.Option;

/**
 * A Functional index that occurs in a trait. We need to be able to get the
 * static parameters on the declaring trait and get the type of `self`.
 */
public interface HasSelfType {
    public Id declaringTrait();
    public Option<SelfType> selfType();
    public int selfPosition();
}
