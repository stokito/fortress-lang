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
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeObject;
import com.sun.fortress.interpreter.evaluator.types.FTypeTop;
import com.sun.fortress.interpreter.evaluator.types.BottomType;
import com.sun.fortress.interpreter.evaluator.values.*;
import com.sun.fortress.interpreter.glue.NativeApp;
import com.sun.fortress.interpreter.glue.NativeMeth0;
import com.sun.fortress.interpreter.glue.NativeMeth1;
import com.sun.fortress.nodes.ObjectConstructor;
import com.sun.fortress.useful.Useful;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Reflect extends NativeConstructor {
    static GenericConstructor gcon = null;

    volatile ReflectedType it;

    public Reflect(Environment env, FTypeObject selfType, ObjectConstructor def) {
        super(env, selfType, def);
        gcon = (GenericConstructor) env.getTopLevel().getRootValue("Reflect");
    }

    protected FNativeObject makeNativeObject(List<FValue> args, NativeConstructor con) {
        if (it == null) {
            synchronized (this) {
                if (it == null) {
                    it = new ReflectedType(con);
                }
            }
        }
        return it;
    }

    private static final class ReflectedType extends FNativeObject {
        final NativeConstructor con;

        private ReflectedType(NativeConstructor con) {
            super(con);
            this.con = con;
        }

        FType getTy() {
            return getSelfEnv().getLeafType("T"); // leaf
        }

        public NativeConstructor getConstructor() {
            return con;
        }

        public boolean seqv(FValue other) {
            if (!(other instanceof ReflectedType)) return false;
            return getTy() == ((ReflectedType) other).getTy();
        }
    }

    public static final ReflectedType makeReflectedType(FType t) {
        if (gcon == null) {
            return error("Cannot make Reflect[\\" + t + "\\](); constructor not invoked from Fortress yet.");
        }
        Simple_fcn con = gcon.typeApply(Useful.list(t));
        return (ReflectedType) con.applyToArgs();
    }

    public static final class Join extends NativeMeth1 {
        public final FValue applyMethod(FObject x, FValue y) {
            FType xty = ((ReflectedType) x).getTy();
            FType yty = ((ReflectedType) y).getTy();
            Set<FType> join = xty.join(yty);
            /* For now, just choose a type at random. */
            for (FType ty : join) {
                return makeReflectedType(ty);
            }
            /* Empty join means top. */
            return makeReflectedType(FTypeTop.ONLY);
        }
    }

    /* XXX Not working correctly; see a comment in FType.meet() */
    public static final class Meet extends NativeMeth1 {
        public final FValue applyMethod(FObject x, FValue y) {
            FType xty = ((ReflectedType) x).getTy();
            FType yty = ((ReflectedType) y).getTy();
            Set<FType> meet = xty.meet(yty);
            /* For now, just choose a type at random. */
            for (FType ty : meet) {
                return makeReflectedType(ty);
            }
            /* Empty meet means bottom. */
            return makeReflectedType(BottomType.ONLY);
        }
    }

    public static final class ToString extends NativeMeth0 {
        public final FValue applyMethod(FObject selfValue) {
            FType ty = ((ReflectedType) selfValue).getTy();
            return FString.make("Reflect[\\" + ty.toString() + "\\]()");
        }
    }

    @Override
    protected void unregister() {
        gcon = null;
    }
}
