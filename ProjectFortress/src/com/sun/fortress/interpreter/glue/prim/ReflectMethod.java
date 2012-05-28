/*******************************************************************************
 Copyright 2010, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.glue.prim;

import static com.sun.fortress.exceptions.ProgramError.error;
import static com.sun.fortress.exceptions.ProgramError.errorMsg;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeObject;
import com.sun.fortress.interpreter.evaluator.types.BottomType;
import com.sun.fortress.interpreter.evaluator.values.*;
import com.sun.fortress.interpreter.glue.NativeMeth3;
import com.sun.fortress.nodes.ObjectConstructor;

import java.util.ArrayList;
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

    public static final class Apply extends NativeMeth3 {
        public final FValue applyMethod(FObject method0, FValue self, FValue sargs, FValue args) {
            MethodWrapper method = (MethodWrapper) method0;
            if (!self.type().subtypeOf(method.getType())) {
                return error(errorMsg(self.type(), " is not a subtype of the expected receiver type ", method.getType(), "."));
            }

            MethodClosure closure = method.getClosure();
            if (closure instanceof GenericMethod) {
                List<FType> typeargs = new ArrayList<FType>();
                if (sargs instanceof FTuple) {
                    for (FValue v : ((FTuple) sargs).getVals()) {
                        typeargs.add(((Reflect.ReflectedType) v).getTy());
                    }
                } else {
                    typeargs.add(((Reflect.ReflectedType) sargs).getTy());
                }
                closure = ((GenericMethod) closure).make(typeargs, closure.getDef());
            } else {
                if (!(sargs instanceof FVoid)) {
                    return error("Static arguments for non-generic method wrapper should be void.");
                }
            }

            // applyMethod will unwrap the tuple argument automatically, so we don't have to.
            return closure.applyMethod((FObject) self, args);
        }
    }

    @Override
    protected void unregister() {
        MethodWrapper.resetConstructor();
    }
}
