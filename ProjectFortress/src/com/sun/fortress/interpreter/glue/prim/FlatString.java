/*******************************************************************************
 Copyright 2008,2010, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.glue.prim;

import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.types.FTypeObject;
import com.sun.fortress.interpreter.evaluator.values.*;
import com.sun.fortress.interpreter.glue.NativeMeth0;
import com.sun.fortress.interpreter.glue.NativeMeth1;
import com.sun.fortress.interpreter.glue.NativeMeth2;
import com.sun.fortress.nodes.ObjectConstructor;

import java.util.List;
import java.util.regex.Pattern;

public class FlatString extends NativeConstructor {

    public FlatString(Environment env, FTypeObject selfType, ObjectConstructor def) {
        super(env, selfType, def);
    }

    @Override
    protected FNativeObject makeNativeObject(List<FValue> args, NativeConstructor con) {
        FString.setConstructor(this);
        return FString.EMPTY;
    }

    @Override
    protected void unregister() {
        FString.resetConstructor();
    }

    private static abstract class ss2S extends NativeMeth1 {
        protected abstract java.lang.String f(java.lang.String s, java.lang.String o);

        @Override
        public final FString applyMethod(FObject self, FValue other) {
            return FString.make(f(self.getString(), other.getString()));
        }
    }

    private static abstract class ss2B extends NativeMeth1 {
        protected abstract boolean f(java.lang.String s, java.lang.String o);

        @Override
        public final FBool applyMethod(FObject self, FValue other) {
            return FBool.make(f(((FString) self).getString(), ((FString) other).getString()));
        }
    }

    private static abstract class ss2I extends NativeMeth1 {
        protected abstract int f(java.lang.String s, java.lang.String o);

        @Override
        public final FInt applyMethod(FObject self, FValue other) {
            return FInt.make(f(((FString) self).getString(), ((FString) other).getString()));
        }
    }

    private static abstract class s2I extends NativeMeth0 {
        protected abstract int f(java.lang.String s);

        @Override
        public final FInt applyMethod(FObject self) {
            return FInt.make(f(((FString) self).getString()));
        }
    }

    private static abstract class sII2s extends NativeMeth2 {
        protected abstract java.lang.String f(java.lang.String s, int lo, int hi);

        @Override
        public final FString applyMethod(FObject self, FValue lo, FValue hi) {
            return FString.make(f(((FString) self).getString(), ((FInt) lo).getInt(), ((FInt) hi).getInt()));
        }
    }

    protected static abstract class s2s extends NativeMeth0 {
        protected abstract java.lang.String f(FString s);

        @Override
        public final FString applyMethod(FObject self) {
            return FString.make(f((FString) self));
        }
    }

    protected static abstract class sI2C extends NativeMeth1 {
        protected abstract char f(java.lang.String s, int i);

        @Override
        public final FChar applyMethod(FObject self, FValue i) {
            return FChar.make(f(((FString) self).getString(), ((FInt) i).getInt()));
        }
    }

    protected static abstract class sC2I extends NativeMeth1 {
        protected abstract int f(java.lang.String s, int c);

        @Override
        public final FInt applyMethod(FObject self, FValue c) {
            return FInt.make(f(((FString) self).getString(), ((FChar) c).getChar()));
        }
    }

    private static abstract class ssI2s extends NativeMeth2 {
        protected abstract java.lang.String f(java.lang.String s1, java.lang.String s2, int i);

        @Override
            public final FString applyMethod(FObject self, FValue s, FValue i) {
            return FString.make(f(((FString) self).getString(), ((FString) s).getString(), ((FInt) i).getInt()));
        }
    }


    public static final class Size extends s2I {
        @Override
        protected int f(java.lang.String s) {
            return s.length();
        }
    }

    public static final class Eq extends ss2B {
        @Override
        protected boolean f(java.lang.String self, java.lang.String other) {
            return self.equals(other);
        }
    }

    public static final class Cmp extends ss2I {
        @Override
        protected int f(java.lang.String self, java.lang.String other) {
            return self.compareTo(other);
        }
    }

    public static final class CICmp extends ss2I {
        @Override
        protected int f(java.lang.String self, java.lang.String other) {
            return self.compareToIgnoreCase(other);
        }
    }

    public static final class Substr extends sII2s {
        @Override
        protected java.lang.String f(java.lang.String self, int x, int y) {
            return self.substring(x, y);
        }
    }

    public static final class ToString extends s2s {
        @Override
        protected java.lang.String f(FString self) {
            return self.toString();
        }
    }

    public static final class Index extends sI2C {
        @Override
        protected char f(java.lang.String self, int i) {
            return self.charAt(i);
        }
    }

    public static final class Concat extends ss2S {
        @Override
        protected java.lang.String f(java.lang.String x, java.lang.String y) {
            return x + y;
        }
    }

    public static final class IndexOf extends sC2I {
        @Override
        protected int f(java.lang.String s, int c) {
            return s.indexOf((char) c);
        }
    }

    public static final class javaRegExpMatches extends ss2B {
        @Override
        protected boolean f(java.lang.String self, java.lang.String match) {
            return Pattern.matches(match, self);
        }
    }

    public static final class javaRegExpSplit extends ssI2s {
        protected String f(java.lang.String self, java.lang.String match, int index) {
            Pattern p = Pattern.compile(match);
            java.lang.String[] temp = p.split(self);
            if (index < temp.length)
                return temp[index];
            else return "";
        }
    }
}
