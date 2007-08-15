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

package com.sun.fortress.interpreter.rewrite;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;
import edu.rice.cs.plt.tuple.Option;

import com.sun.fortress.nodes.AbstractNode;
import com.sun.fortress.nodes_util.NodeReflection;
import com.sun.fortress.useful.NI;
import com.sun.fortress.useful.Pair;


abstract public class Visit extends NodeReflection {

    @Override
    protected Constructor defaultConstructorFor(Class cl)
            throws NoSuchMethodException {
        return NI.na("Visitors cannot modify tree structure");
    }

    /**
     * Called by VisitObject for each Node; expected to perform
     * any customized rewriting operations needed.
     */
    abstract protected void visit(AbstractNode node);

    /**
     * Based on the type of o, recursively visits its pieces.
     */
    protected void visitObject(Object o) {
        if (o instanceof List) {
             visitList((List) o);
        } else if (o instanceof Pair<?, ?>) {
             visitPair((Pair<?, ?>) o);
        } else if (o instanceof Option<?>) {
             visitOption((Option<?>) o);
        } else if (o instanceof Number) {

        } else if (o instanceof Boolean) {

        } else if (o instanceof AbstractNode) {
             visit((AbstractNode) o);
        } else {
        }
            return;
    }


   /**
     * Visits the pieces of a node using reflection,
     * returning either the original if nothing has changed,
     * or a new node if something has changed.
     */
    protected void visitNode(AbstractNode n) {
        Field[] fields = getCachedPrintableFields(n.getClass());
        for (int i = 0; i < fields.length; i++) {
            Field f = fields[i];
            try {
                Object o = f.get(n);
                visitObject(o);

            // Should be impossible, we set them to be accessible.
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return;
    }

     /**
     * VisitObject each element of the list
     */
    protected  void visitList(List list) {
        for (int i = 0; i < list.size(); i++) {
            Object o = list.get(i);
            visitObject(o);

        }
        return;
    }

    /**
     * VisitObject the value of the Option (if it exists).
     */
    protected void visitOption(Option<?> opt) {
        if (opt.isSome()) { visitObject(Option.unwrap(opt)); }
    }

    /**
     * VisitObject the two elements of the pair, returning a different
     * Pair if either one changed, otherwise returning the original.
     */
    protected void visitPair(Pair pair) {
        Object a = pair.getA();
        Object b = pair.getB();
        visitObject(a);
        visitObject(b);

     }


    protected Visit() {
        super();
    }



}
