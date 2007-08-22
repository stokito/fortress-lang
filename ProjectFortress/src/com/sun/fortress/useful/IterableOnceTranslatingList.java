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

package com.sun.fortress.useful;

import java.util.Iterator;
import java.util.List;

public class IterableOnceTranslatingList<From, To> implements IterableOnce<To> {
    int i = -1;
    private List<? extends From> list;
    private Fn<? super From, ? extends To> translator;

    public IterableOnceTranslatingList(List<? extends From> list,
                                       Fn<? super From, ? extends To> translator) {
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
        if (i >= list.size())
            return null;
        From id = list.get(i);
        i++;
        return translator.apply(id);
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

};
