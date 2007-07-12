/*
 * Created on Jul 12, 2007
 *
 */
package com.sun.fortress.interpreter.nodes_util;

import com.sun.fortress.interpreter.useful.BATree;
import com.sun.fortress.interpreter.useful.BATree2;

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
        throw new Error("Really, don't call this");
    }
}
