/*******************************************************************************
 Copyright 2010, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.glue.prim;

import static com.sun.fortress.exceptions.ProgramError.error;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeObject;
import com.sun.fortress.interpreter.evaluator.types.FTypeRest;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.glue.NativeFn1;
import com.sun.fortress.nodes.ObjectConstructor;

public class ReflectRest extends Reflect {
    public ReflectRest(Environment env, FTypeObject selfType, ObjectConstructor def) {
        super(env, selfType, def);
    }

    @Override
    protected void checkType(FType ty) {
        if (!(ty instanceof FTypeRest)) error(ty + " is not a rest type.");
    }

    public static final class Make extends NativeFn1 {
        public final FValue applyToArgs(FValue type0) {
            FType type = ((ReflectedType) type0).getTy();
            return Reflect.make(FTypeRest.make(type));
        }
    }

    public static final class Base extends T2T {
        public final FType f(FType x) {
            return ((FTypeRest) x).getType();
        }
    }
}
