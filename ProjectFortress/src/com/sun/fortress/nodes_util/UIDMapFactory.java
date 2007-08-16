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

package com.sun.fortress.nodes_util;

import com.sun.fortress.useful.BATree;
import com.sun.fortress.useful.BATree2;

import static com.sun.fortress.interpreter.evaluator.InterpreterBug.bug;

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
