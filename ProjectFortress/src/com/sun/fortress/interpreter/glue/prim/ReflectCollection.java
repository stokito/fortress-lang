/*******************************************************************************
 Copyright 2010, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.glue.prim;

import static com.sun.fortress.exceptions.InterpreterBug.bug;
import static com.sun.fortress.exceptions.ProgramError.error;
import static com.sun.fortress.exceptions.ProgramError.errorMsg;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.types.*;
import com.sun.fortress.interpreter.evaluator.values.*;
import com.sun.fortress.interpreter.glue.NativeMeth0;
import com.sun.fortress.interpreter.glue.NativeMeth1;
import com.sun.fortress.interpreter.glue.NativeMeth3;
import com.sun.fortress.nodes.ObjectConstructor;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class ReflectCollection extends NativeConstructor {

    public ReflectCollection(Environment env, FTypeObject selfType, ObjectConstructor def) {
        super(env, selfType, def);
    }

    protected FNativeObject makeNativeObject(List<FValue> args, NativeConstructor con) {
        CollectionObject.setConstructor(this);
        return new CollectionObject(Collections.<FValue>emptyList(), new CollectionAdapter<FValue>() {
            public FValue adapt(FValue obj) { return obj; }
        });
    }

    public static interface CollectionAdapter<T> {
        public abstract FValue adapt(T obj);
    }

    public static class CollectionObject<T> extends FNativeObject {
        private static volatile NativeConstructor con;
        private final Collection<T> collection;
        private final CollectionAdapter<T> adapter;

        private CollectionObject(Collection<T> collection, CollectionAdapter<T> adapter) {
            super(null);
            this.collection = collection;
            this.adapter = adapter;
        }

        public NativeConstructor getConstructor() {
            return con;
        }

        public Collection<T> getCollection() {
            return collection;
        }

        public FValue adapt(T obj) {
            return adapter.adapt(obj);
        }

        public boolean seqv(FValue other) {
            if (!(other instanceof CollectionObject)) return false;
            return getCollection().equals(((CollectionObject) other).getCollection());
        }

        public static void setConstructor(NativeConstructor con) {
            // WARNING!  In order to run the tests we must reset con for
            // each new test, so it's not OK to ignore setConstructor
            // attempts after the first one.
            if (con == null) return;
            CollectionObject.con = con;
        }

        public static void resetConstructor() {
            CollectionObject.con = null;
        }
    }

    public static final <T> CollectionObject<T> make(Collection<T> collection, CollectionAdapter<T> adapter) {
        return new CollectionObject<T>(collection, adapter);
    }

    public static final class Size extends NativeMeth0 {
        public final FInt applyMethod(FObject self) {
            Collection<Object> collection = ((CollectionObject<Object>) self).getCollection();
            return FInt.make(collection.size());
        }
    }

    public static final class Get extends NativeMeth1 {
        public final FValue applyMethod(FObject self0, FValue i0) {
            CollectionObject<Object> self = (CollectionObject<Object>) self0;
            Collection<Object> collection = self.getCollection();
            if (!(collection instanceof List)) {
                return error(errorMsg("This collection doesn't support a direct indexing."));
            }

            int i = ((FInt) i0).getInt();
            try {
                return self.adapt(((List<Object>) collection).get(i));
            }
            catch (IndexOutOfBoundsException e) {
                return error(errorMsg("Collection element index ", i, " out of bounds, length=", collection.size()), e);
            }
        }
    }

    public static final class Generate extends NativeMeth3 {
        public final FValue applyMethod(FObject self0, FValue empty, FValue join, FValue body) {
            CollectionObject<Object> self = (CollectionObject<Object>) self0;
            Collection<Object> collection = self.getCollection();
            if (collection.isEmpty()) {
                return ((Fcn) empty).applyToArgs();
            } else {
                Fcn joinfn = (Fcn) join, bodyfn = (Fcn) body;
                Iterator<Object> it = collection.iterator();
                FValue reduced = bodyfn.applyToArgs(self.adapt(it.next()));
                while (it.hasNext()) {
                    FValue current = bodyfn.applyToArgs(self.adapt(it.next()));
                    reduced = joinfn.applyToArgs(reduced, current);
                }
                return reduced;
            }
        }
    }

    @Override
    protected void unregister() {
        CollectionObject.resetConstructor();
    }
}
