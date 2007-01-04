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
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import com.sun.fortress.interpreter.nodes.HasSomeExtraState;
import com.sun.fortress.interpreter.nodes.Node;
import com.sun.fortress.interpreter.nodes.NodeReflection;
import com.sun.fortress.interpreter.nodes.None;
import com.sun.fortress.interpreter.nodes.Some;
import com.sun.fortress.interpreter.nodes.Span;
import com.sun.fortress.interpreter.evaluator.ProgramError;
import com.sun.fortress.interpreter.useful.Pair;


/**
 * Used to com.sun.fortress.interpreter.rewrite the internals of an object definition.
 *
 * Rewrites:
 *   field -> obfuscated field + getter/setter
 *   field reference -> self.getter()/setter()
 *   method reference -> self.method()
 *   wrapped method reference -> self.wrappingField().method()
 */

public abstract class Rewrite extends NodeReflection {
    static Class[] oneSpanArg = { Span.class };
    @Override
    protected Constructor defaultConstructorFor(Class cl) throws NoSuchMethodException {
        return cl.getDeclaredConstructor(oneSpanArg);
    }

    /**
     * Called by VisitObject for each Node; expected to perform
     * any customized rewriting operations needed.
     *
     * @param node
     * @return
     */
    abstract protected Node visit(Node node);

    /**
     * Based on the type of o, recursively visits its pieces.
     *
     * @param o
     * @return
     */
    protected <T> T visitObject(T o) {
        Object result = null;
        if (o==null) {
            throw new NullPointerException();
        } else if (o instanceof List) {
            result = visitList((List<?>) o);
        } else if (o instanceof Pair<?,?>) {
            result = visitPair((Pair<?,?>) o);
        } else if (o instanceof Some<?>) {
            result = visitSome((Some<?>) o);
        } else if (o instanceof None<?>) {
            result = o;
        } else if (o instanceof Number) {
            result = o;
        } else if (o instanceof Boolean) {
            result = o;
        } else if (o instanceof String) {
            result = o;
        } else { // (o instanceof Node)
            result = visit((Node) o);
        }
        // Although the following cast is unchecked,
        // it holds because every visit method returns an object
        // with the same runtime type as its argument.
        // eric.allen@sun.com 9/21/2006
        return (T) result;
    }


   /**
     * Visits the pieces of a node using reflection,
     * returning either the original if nothing has changed,
     * or a new node if something has changed.
     *
     * @param n
     * @return
     */
    protected Node visitNode(Node n) {
        Node replacement = null;
        Field[] fields = getCachedPrintableFields(n.getClass());
        for (int i = 0; i < fields.length; i++) {
            Field f = fields[i];
            try {
                Object o = f.get(n);
                Object p = visitObject(o);
                if (p != o) {
                    if (replacement == null) {
                        Constructor con = constructorFor(n.getClass());
                        Object[] args = new Object[1];
                        args[0] = n.getSpan();
                        if (con == null)
                            System.err.println(n.getClass());
                        replacement = (Node) con.newInstance(args);
                        replacement.setOriginal(n);
                        // Copy over earlier fields
                        for (int j = 0; j < i; j++) {
                            Field g = fields[j];
                            g.set(replacement, g.get(n));
                        }
                    }
                    f.set(replacement, p);
                } else if (replacement != null) {
                    f.set(replacement, o);
                }
            // Should be impossible, we set them to be accessible.
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (NullPointerException x) {
                // This gives some hope of tracing an exception to a
                // point in the source code, and thus diagnosing why
                // the AST might have gone wrong. -Jan
                throw new ProgramError(n,"Has a null component.",x);
            }
        }
        if (replacement != null && n instanceof HasSomeExtraState) {
            ((HasSomeExtraState)n).finishClone(replacement);
        }
        return replacement != null ? replacement : n;
    }

     /**
     * VisitObject each element of the list, returning a different
     * list if there has been a change, otherwise returning the same
     * list.
     *
     * @param list
     * @return
     */
    protected <T> List<T> visitList(List<T> list) {
        ArrayList<T> replacement = null;
        for (int i = 0; i < list.size(); i++) {
            T  o = list.get(i);
            T p = visitObject(o);
            if (replacement == null) {
                if (o != p) {
                    replacement = new ArrayList<T>(list.size());
                    for (int j = 0; j < i; j++)
                        replacement.add(list.get(j));
                    replacement.add(p);
                }
            } else {
                replacement.add(p);
            }
        }
        return replacement == null ? list : replacement;
    }

    /**
     * VisitObject the value of the Some, returning a different
     * Some if there has been a change, otherwise returning the same
     * Some.
     *
     * @param list
     * @return
     */
    protected <T> Some<T> visitSome(Some<T> some) {
        T o = some.getVal();
        T p = visitObject(o);
        if (p != o) {
            return new Some<T>(p);
        }
        return some;
    }

    /**
     * VisitObject the two elements of the pair, returning a different
     * Pair if either one changed, otherwise returning the original.
     *
     * @param pair
     * @return
     */
    protected <T,U> Pair<T,U> visitPair(Pair<T,U> pair) {
        T a = pair.getA();
        U b = pair.getB();
        T aa = visitObject(a);
        U bb = visitObject(b);
        if (aa != a || bb != b) {
            return new Pair<T,U>(aa, bb);
        }
        return pair;
    }


    protected Rewrite() {
        super();
    }


}
