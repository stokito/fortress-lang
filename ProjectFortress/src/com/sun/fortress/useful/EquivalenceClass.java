/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

public interface EquivalenceClass<Element, Canonicalized> {
    public abstract int compare(Element x, Canonicalized y);

    public abstract int compareRightKeys(Canonicalized x, Canonicalized y);

    public abstract int compareLeftKeys(Element x, Element y);

    public abstract Canonicalized translate(Element x);
}
