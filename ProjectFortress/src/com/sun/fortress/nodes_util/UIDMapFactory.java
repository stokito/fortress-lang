/*******************************************************************************
    Copyright 2008, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.nodes_util;

import com.sun.fortress.useful.BATree;
import com.sun.fortress.useful.BATree2;

import static com.sun.fortress.exceptions.InterpreterBug.bug;

public class UIDMapFactory {
    /**
     * Returns a BATree mapping UID things to T's
     * @param <T>
     * @return
     */
    public static <T> BATree<UIDObject, T> make() {
        return new BATree<UIDObject, T>(UIDComparator.V);
    }
    /**
     * Returns a BATree mapping UID things to T's and U's
     * (available singly, or in pairs).
     * @param <T>
     * @return
     */
    public static <T, U> BATree2<UIDObject, T, U> make2() {
        return new BATree2<UIDObject, T, U>(UIDComparator.V);
    }

    private UIDMapFactory() {
        bug("Really, don't call this");
    }
}
