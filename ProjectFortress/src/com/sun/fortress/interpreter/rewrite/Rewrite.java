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
import java.util.Collection;
import java.util.List;
import edu.rice.cs.plt.tuple.Option;

import com.sun.fortress.nodes_util.HasSomeExtraState;
import com.sun.fortress.nodes.AbstractNode;
import com.sun.fortress.nodes_util.NodeReflection;
import com.sun.fortress.nodes_util.RewriteHackList;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.useful.Pair;

import static com.sun.fortress.interpreter.evaluator.ProgramError.error;

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
    protected Constructor defaultConstructorFor(Class cl) {
        try {
            return cl.getDeclaredConstructor(oneSpanArg);
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchMethodException e) {

        }
        return null;
    }

    /**
     * Called by VisitObject for each Node; expected to perform
     * any customized rewriting operations needed.
     */
    abstract protected AbstractNode visit(AbstractNode node);

    /**
     * Based on the type of o, recursively visits its pieces.
     */
    @SuppressWarnings("unchecked")
    protected <T> T visitObject(T o) {
        Object result = null;
        if (o==null) {
            throw new NullPointerException();
        } else if (o instanceof List<?>) {
            result = visitList((List<?>) o);
        } else if (o instanceof Pair<?,?>) {
            result = visitPair((Pair<?,?>) o);
        } else if (o instanceof Option<?>) {
            result = visitOption((Option<?>) o);
        } else if (o instanceof Number) {
            result = o;
        } else if (o instanceof Boolean) {
            result = o;
        } else if (o instanceof String) {
            result = o;
        } else { // (o instanceof Node)
            result = visit((AbstractNode) o);
        }
        // Although the following cast is unchecked,
        // it holds because every visit method returns an object
        // with the same runtime type as its argument.
        // eric.allen@sun.com 9/21/2006
        // *Unless* this assumption is violated by subclasses --
        // which is quite likely.  For some T, such as a specific
        // Expr type, the translation could generate a different
        // return Expr type.  We rely on the assumption that, where
        // this might happen, the calling context will never choose
        // a T that will cause problems (it will only use, say, Expr).
        return (T) result;
    }


   /**
     * Visits the pieces of a node using reflection,
     * returning either the original if nothing has changed,
     * or a new node if something has changed.
     */
    protected AbstractNode visitNode(AbstractNode n) {
        AbstractNode replacement = null;
        Field[] fields = getCachedPrintableFields(n.getClass());
        for (int i = 0; i < fields.length; i++) {
            Field f = fields[i];
            try {
                Object o = f.get(n);
                Object p = visitObject(o);
                if (p != o) {
                    if (replacement == null) {
                        replacement = makeNodeFromSpan(null, n.getClass(), n.getSpan());
//                        Constructor con = constructorFor(n.getClass());
//                        Object[] args = new Object[1];
//                        args[0] = n.getSpan();
//                        if (con == null)
//                            System.err.println(n.getClass());
//                        replacement = (AbstractNode) con.newInstance(args);
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
                error(n, "Has a null component.", x);
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
                }
            }
            if (replacement != null) {
                if (p instanceof RewriteHackList) {
                    // Ugh.  We can only take this reflection/generics thing so far.
                    // Assumes the elements of the RewriteHackList are instances of T.
                    replacement.addAll( (Collection<? extends T>) ((RewriteHackList)p).getNodes());
                } else {
                    replacement.add(p);
                }
            }
        }
        return replacement == null ? list : replacement;
    }

    /**
     * VisitObject the value of the Option, returning a 
     * "some" wrapper if the value exists and VisitObject does not return null.
     * Otherwise returns the untouched argument.
     */
    protected <T> Option<T> visitOption(Option<T> opt) {
        if (opt.isSome()) {
            T updated = visitObject(Option.unwrap(opt));
            if (updated != null) {
                return Option.some(updated);
            }
            // else fall through
        }
        return opt;
    }

    /**
     * VisitObject the two elements of the pair, returning a different
     * Pair if either one changed, otherwise returning the original.
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
