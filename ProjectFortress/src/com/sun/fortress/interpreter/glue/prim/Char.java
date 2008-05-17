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

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.values.NativeConstructor;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.FObject;
import com.sun.fortress.interpreter.evaluator.values.FChar;
import com.sun.fortress.interpreter.evaluator.values.FBool;
import com.sun.fortress.interpreter.evaluator.values.FInt;
import com.sun.fortress.interpreter.evaluator.values.FString;
import com.sun.fortress.interpreter.evaluator.types.FTypeObject;
import com.sun.fortress.nodes.GenericWithParams;
import com.sun.fortress.interpreter.glue.NativeFn1;
import com.sun.fortress.interpreter.glue.NativeMeth0;
import com.sun.fortress.interpreter.glue.NativeMeth1;

import static com.sun.fortress.interpreter.evaluator.ProgramError.error;

/**
 * The Char type.
 */
public class Char extends NativeConstructor {

    public Char(BetterEnv env, FTypeObject selfType, GenericWithParams def) {
        super(env, selfType, def);
    }

    protected FNativeObject makeNativeObject(List<FValue> args,
                                             NativeConstructor con) {
        FChar.setConstructor(this);
        return FChar.ZERO;
    }

    private static abstract class sC2B extends NativeMeth1 {
        protected abstract boolean f(char self, char other);
        protected final FValue act(FObject self, FValue other) {
            return FBool.make(f(self.getChar(), other.getChar()));
        }
    }

    private static abstract class s2I extends NativeMeth0 {
        protected abstract int f(char self);
        protected final FValue act(FObject self) {
            return FInt.make(f(self.getChar()));
        }
    }

    private static abstract class I2C extends NativeFn1 {
        abstract protected char f(int i);
        protected final FValue act(FValue i) {
            return FChar.make(f(i.getInt()));
        }
    }

    public static final class Eq extends sC2B {
        protected boolean f(char x, char y) {
            return (int) x == (int) y;
        }
    }

    public static final class LessThan extends sC2B {
        protected boolean f(char x, char y) {
            return (int) x < (int) y;
        }
    }

    public static final class Ord extends s2I {
        protected int f(char self) { return (int)self; }
    }

    public static final class Chr extends I2C {
        protected char f(int i) { return (char)i; }
    }

    public static final class ToString extends NativeMeth0 {
        protected FValue act(FObject self) {
            return FString.make(((FChar)self).toString());
        }
    }

}
