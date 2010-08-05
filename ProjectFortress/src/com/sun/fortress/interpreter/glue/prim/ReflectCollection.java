/*******************************************************************************
 Copyright 2010 Sun Microsystems, Inc.,
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

package com.sun.fortress.interpreter.glue.prim;

import static com.sun.fortress.exceptions.InterpreterBug.bug;
import static com.sun.fortress.exceptions.ProgramError.error;
import static com.sun.fortress.exceptions.ProgramError.errorMsg;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.types.*;
import com.sun.fortress.interpreter.evaluator.values.*;
import com.sun.fortress.interpreter.glue.NativeMeth0;
import com.sun.fortress.interpreter.glue.NativeMeth1;
import com.sun.fortress.interpreter.glue.NativeMeth;
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
        ReflectedTypeCollection.setConstructor(this);
        return new ReflectedTypeCollection(Collections.<FType>emptyList());
    }

    protected static class ReflectedTypeCollection extends FNativeObject {
        private static volatile NativeConstructor con;
        private final Collection<FType> types;

        private ReflectedTypeCollection(Collection<FType> types) {
            super(null);
            this.types = types;
        }

        public NativeConstructor getConstructor() {
            return con;
        }

        Collection<FType> getTypes() {
            return types;
        }

        public boolean seqv(FValue other) {
            if (!(other instanceof ReflectedTypeCollection)) return false;
            return getTypes().equals(((ReflectedTypeCollection) other).getTypes());
        }

        public static void setConstructor(NativeConstructor con) {
            // WARNING!  In order to run the tests we must reset con for
            // each new test, so it's not OK to ignore setConstructor
            // attempts after the first one.
            if (con == null) return;
            ReflectedTypeCollection.con = con;
        }

        public static void resetConstructor() {
            ReflectedTypeCollection.con = null;
        }
    }

    public static final ReflectedTypeCollection make(Collection<FType> types) {
        return new ReflectedTypeCollection(types);
    }

    public static final class Size extends NativeMeth0 {
        public final FInt applyMethod(FObject self) {
            Collection<FType> types = ((ReflectedTypeCollection) self).getTypes();
            return FInt.make(types.size());
        }
    }

    public static final class Get extends NativeMeth1 {
        public final FValue applyMethod(FObject self, FValue i0) {
            Collection<FType> types = ((ReflectedTypeCollection) self).getTypes();
            if (!(types instanceof List)) {
                return error(errorMsg("This collection doesn't support a direct indexing."));
            }

            int i = ((FInt) i0).getInt();
            try {
                return Reflect.make(((List<FType>) types).get(i));
            }
            catch (IndexOutOfBoundsException e) {
                return error(errorMsg("Tuple element index ", i, " out of bounds, length=", types.size()), e);
            }
        }
    }

    public static final class Generate extends NativeMeth {
        public final int getArity() {
            return 3;
        }

        public final FValue applyMethod(FObject self, List<FValue> args) {
            Collection<FType> types = ((ReflectedTypeCollection) self).getTypes();
            if (types.isEmpty()) {
                Fcn empty = (Fcn) args.get(0);
                return empty.applyToArgs();
            } else {
                Fcn join = (Fcn) args.get(1), body = (Fcn) args.get(2);
                Iterator<FType> it = types.iterator();
                FValue reduced = body.applyToArgs(Reflect.make(it.next()));
                while (it.hasNext()) {
                    FValue current = body.applyToArgs(Reflect.make(it.next()));
                    reduced = join.applyToArgs(reduced, current);
                }
                return reduced;
            }
        }
    }

    @Override
    protected void unregister() {
        ReflectedTypeCollection.resetConstructor();
    }
}
