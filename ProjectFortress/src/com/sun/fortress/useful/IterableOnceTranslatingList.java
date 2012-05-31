/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

import java.util.Iterator;
import java.util.List;

public class IterableOnceTranslatingList<From, To> implements IterableOnce<To> {
    int i = -1;
    private List<? extends From> list;
    private F<? super From, ? extends To> translator;

    public IterableOnceTranslatingList(List<? extends From> list, F<? super From, ? extends To> translator) {
        this.list = list;
        this.translator = translator;
    }

    public Iterator<To> iterator() {
        if (i >= 0) {
            throw new IllegalStateException("One-shot iterator");
        }
        i = 0;
        return this;
    }

    public boolean hasNext() {
        return i < list.size();
    }

    public To next() {
        if (i >= list.size()) return null;
        From id = list.get(i);
        i++;
        return translator.apply(id);
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

}
