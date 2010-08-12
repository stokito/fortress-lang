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

import static com.sun.fortress.exceptions.ProgramError.error;
import static com.sun.fortress.exceptions.ProgramError.errorMsg;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeObject;
import com.sun.fortress.interpreter.evaluator.types.BottomType;
import com.sun.fortress.interpreter.evaluator.values.*;
import com.sun.fortress.interpreter.glue.NativeMeth2;
import com.sun.fortress.nodes.ObjectConstructor;

import java.util.List;

public class ReflectMethod extends NativeConstructor {
    // As this object is not intended to be called, we insert an unoccupied type
    // to cause an error early.
    public final static MethodWrapper NO_BODY = new MethodWrapper(BottomType.ONLY, null);

    public ReflectMethod(Environment env, FTypeObject selfType, ObjectConstructor def) {
        super(env, selfType, def);
    }

    protected FNativeObject makeNativeObject(List<FValue> args, NativeConstructor con) {
        MethodWrapper.setConstructor(this);
        return NO_BODY;
    }

    public static class MethodWrapper extends FNativeObject {
        private static volatile NativeConstructor con;
        private final FType type;
        private final MethodClosure closure;

        private MethodWrapper(FType type, MethodClosure closure) {
            super(null);
            this.type = type;
            this.closure = closure;
        }

        public NativeConstructor getConstructor() {
            return con;
        }

        public FType getType() {
            return type;
        }

        public MethodClosure getClosure() {
            return closure;
        }

        public boolean seqv(FValue other0) {
            if (!(other0 instanceof MethodWrapper)) return false;
            MethodWrapper other = (MethodWrapper) other0;
            if (!getType().equals(other.getType())) return false;
            if (getClosure() == null) return (other.getClosure() == null);
            return getClosure().equals(other.getClosure());
        }

        public static void setConstructor(NativeConstructor con) {
            // WARNING!  In order to run the tests we must reset con for
            // each new test, so it's not OK to ignore setConstructor
            // attempts after the first one.
            if (con == null) return;
            MethodWrapper.con = con;
        }

        public static void resetConstructor() {
            MethodWrapper.con = null;
        }
    }

    public static final MethodWrapper make(FType type, MethodClosure closure) {
        return new MethodWrapper(type, closure);
    }

    public static final class Apply extends NativeMeth2 {
        public final FValue applyMethod(FObject method0, FValue self, FValue args) {
            MethodWrapper method = (MethodWrapper) method0;
            if (!self.type().subtypeOf(method.getType())) {
                return error(errorMsg(self.type(), " is not a subtype of the expected receiver type ", method.getType(), "."));
            }
            return method.getClosure().applyMethod((FObject) self, args);
        }
    }

    @Override
    protected void unregister() {
        MethodWrapper.resetConstructor();
    }
}
