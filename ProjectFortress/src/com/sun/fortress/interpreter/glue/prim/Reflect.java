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
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.types.*;
import com.sun.fortress.interpreter.evaluator.values.*;
import com.sun.fortress.interpreter.glue.NativeFn0;
import com.sun.fortress.interpreter.glue.NativeFn1;
import com.sun.fortress.interpreter.glue.NativeMeth0;
import com.sun.fortress.interpreter.glue.NativeMeth1;
import com.sun.fortress.nodes.ObjectConstructor;
import com.sun.fortress.useful.Useful;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Reflect extends NativeConstructor {
    ReflectedType it;

    static GenericConstructor gconobject = null;
    static GenericConstructor gcontrait = null;
    static GenericConstructor gconarrow = null;
    static GenericConstructor gcontuple = null;
    static GenericConstructor gconbottom = null;

    public Reflect(Environment env, FTypeObject selfType, ObjectConstructor def) {
        super(env, selfType, def);

        Environment toplevel = env.getTopLevel();
        gconobject = (GenericConstructor) toplevel.getRootValue("ReflectObject");
        gcontrait = (GenericConstructor) toplevel.getRootValue("ReflectTrait");
        gconarrow = (GenericConstructor) toplevel.getRootValue("ReflectArrow");
        gcontuple = (GenericConstructor) toplevel.getRootValue("ReflectTuple");
        gconbottom = (GenericConstructor) toplevel.getRootValue("ReflectBottom");
    }

    protected void checkType(FType ty) {
    }

    protected FNativeObject makeNativeObject(List<FValue> args, NativeConstructor con) {
        return it;
    }

    @Override
    protected void oneTimeInit(Environment self_env) {
        FType ty = self_env.getLeafType("T"); // leaf
        checkType(ty);
        it = new ReflectedType(this, ty);
    }

    protected static class ReflectedType extends FNativeObject {
        private NativeConstructor con;
        private FType ty;

        private ReflectedType(NativeConstructor con, FType ty) {
            super(con);
            this.con = con;
            this.ty = ty;
        }

        public NativeConstructor getConstructor() {
            return con;
        }

        FType getTy() {
            return ty;
        }

        public boolean seqv(FValue other) {
            if (!(other instanceof ReflectedType)) return false;
            return getTy() == ((ReflectedType) other).getTy();
        }
    }

    public static final ReflectedType make(FType t) {
        if (gconobject == null) {
            return error("Constructor not invoked from Fortress yet.");
        }

        GenericConstructor gcon = null;
        if (t instanceof FTypeObject) {
            gcon = gconobject;
        } else if (t instanceof FTypeTrait) {
            gcon = gcontrait;
        } else if (t instanceof FTypeArrow) {
            gcon = gconarrow;
        } else if (t instanceof FTypeTuple) {
            gcon = gcontuple;
        } else if (t instanceof BottomType) {
            gcon = gconbottom;
        } else {
            return error("Not supported type " + t + ".");
        }
        Simple_fcn con = gcon.typeApply(Useful.list(t));
        return (ReflectedType) con.applyToArgs();
    }

    protected static abstract class T2S extends NativeMeth0 {
        protected abstract String f(FType x);

        public FString applyMethod(FObject self) {
            FType x = ((ReflectedType) self).getTy();
            return FString.make(f(x));
        }
    }

    protected static abstract class T2I extends NativeMeth0 {
        protected abstract int f(FType x);

        public FInt applyMethod(FObject self) {
            FType x = ((ReflectedType) self).getTy();
            return FInt.make(f(x));
        }
    }

    protected static abstract class T2T extends NativeMeth0 {
        protected abstract FType f(FType x);

        public ReflectedType applyMethod(FObject self) {
            FType x = ((ReflectedType) self).getTy();
            return make(f(x));
        }
    }

    protected static abstract class TI2T extends NativeMeth1 {
        protected abstract FType f(FType x, int y);

        public ReflectedType applyMethod(FObject self, FValue y0) {
            FType x = ((ReflectedType) self).getTy();
            int y = ((FInt) y0).getInt();
            return make(f(x, y));
        }
    }

    protected static abstract class TT2T extends NativeMeth1 {
        protected abstract FType f(FType x, FType y);

        public ReflectedType applyMethod(FObject self, FValue other) {
            FType x = ((ReflectedType) self).getTy();
            FType y = ((ReflectedType) other).getTy();
            return make(f(x, y));
        }
    }

    protected static abstract class TT2B extends NativeMeth1 {
        protected abstract boolean f(FType x, FType y);

        public FBool applyMethod(FObject self, FValue other) {
            FType x = ((ReflectedType) self).getTy();
            FType y = ((ReflectedType) other).getTy();
            return FBool.make(f(x, y));
        }
    }

    public static final class TypeOf extends NativeFn1 {
        public FValue applyToArgs(FValue arg) {
            return make(arg.type());
        }
    }

    public static final class Copy extends T2T {
        public final FType f(FType x) {
            /* Only appears in the internal Reflect[\T\] object; it converts
             * a generic Reflect[\T\] object into ReflectXxx[\T\] objects,
             * therefore placing it into the correct Type hierarchy. */
            return x;
        }
    }

    public static final class Join extends TT2T {
        public final FType f(FType x, FType y) {
            Set<FType> join = x.join(y);
            /* For now, just choose a type at random. */
            for (FType ty : join) {
                return ty;
            }
            /* Empty join means top. */
            return FTypeTop.ONLY;
        }
    }

    /* XXX Not working correctly; see a comment in FType.meet() */
    public static final class Meet extends TT2T {
        public final FType f(FType x, FType y) {
            Set<FType> meet = x.meet(y);
            /* For now, just choose a type at random. */
            for (FType ty : meet) {
                return ty;
            }
            /* Empty meet means bottom. */
            return BottomType.ONLY;
        }
    }

    public static final class SubtypeOf extends TT2B {
        public final boolean f(FType x, FType y) {
            return x.subtypeOf(y);
        }
    }

    public static final class ToString extends T2S {
        public final String f(FType ty) {
            return ty.toString();
        }
    }

    @Override
    protected void unregister() {
        gconobject = gcontrait = gconarrow = gcontuple = gconbottom = null;
    }
}
