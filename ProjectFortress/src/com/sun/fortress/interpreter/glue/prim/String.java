/*******************************************************************************
    Copyright 2008 Sun Microsystems, Inc.,
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

import java.util.List;
import java.util.Collections;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.values.NativeConstructor;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.FObject;
import com.sun.fortress.interpreter.evaluator.values.FBool;
import com.sun.fortress.interpreter.evaluator.values.FInt;
import com.sun.fortress.interpreter.evaluator.values.FString;
import com.sun.fortress.interpreter.evaluator.types.FTypeObject;
import com.sun.fortress.interpreter.glue.NativeFn0;
import com.sun.fortress.interpreter.glue.NativeMeth0;
import com.sun.fortress.interpreter.glue.NativeMeth1;
import com.sun.fortress.interpreter.glue.NativeMeth2;
import com.sun.fortress.nodes.GenericWithParams;

import static com.sun.fortress.interpreter.evaluator.ProgramError.errorMsg;
import static com.sun.fortress.interpreter.evaluator.ProgramError.error;


public class String extends NativeConstructor {

    public String(BetterEnv env,
                  FTypeObject selfType,
                  GenericWithParams def) {
        super(env,selfType,def);
    }

    protected FNativeObject makeNativeObject(List<FValue> args,
                                             NativeConstructor con) {
        FString.setConstructor(this);
        return FString.EMPTY;
    }

    private static abstract class ss2B extends NativeMeth1 {
        protected abstract boolean f(java.lang.String s, java.lang.String o);
        protected final FBool act(FObject self, FValue other) {
            return FBool.make(f(((FString)self).getString(),
                                ((FString)other).getString()));
        }
    }

    private static abstract class ss2I extends NativeMeth1 {
        protected abstract int f(java.lang.String s, java.lang.String o);
        protected final FInt act(FObject self, FValue other) {
            return FInt.make(f(((FString)self).getString(),
                               ((FString)other).getString()));
        }
    }

    private static abstract class s2I extends NativeMeth0 {
        protected abstract int f(java.lang.String s);
        protected final FInt act(FObject self) {
            return FInt.make(f(((FString)self).getString()));
        }
    }

    private static abstract class sII2s extends NativeMeth2 {
        protected abstract java.lang.String f(java.lang.String s, int lo, int hi);
        protected final FString act(FObject self, FValue lo, FValue hi) {
            return FString.make(f(((FString)self).getString(),
                                  ((FInt)lo).getInt(),
                                  ((FInt)hi).getInt()));
        }
    }

    protected static abstract class s2s extends NativeMeth0 {
        protected abstract java.lang.String f(FString s);
        protected final FString act(FObject self) {
            return FString.make(f((FString) self));
        }
    }

    public static final class Size extends s2I {
        protected int f(java.lang.String s) {
            return s.length();
        }
    }

    public static final class Eq extends ss2B {
        protected boolean f(java.lang.String self, java.lang.String other) {
            return self.equals(other);
        }
    }

    public static final class Cmp extends ss2I {
        protected int f(java.lang.String self, java.lang.String other) {
            return self.compareTo(other);
        }
    }

    public static final class Substr extends sII2s {
        protected java.lang.String f(java.lang.String self, int x, int y) {
            return self.substring(x,y);
        }
    }

    public static final class ToString extends s2s {
        protected java.lang.String f(FString self) {
            return self.toString();
        }
    }
}
