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
import com.sun.fortress.interpreter.evaluator.types.FTypeArrow;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.glue.NativeFn2;
import com.sun.fortress.nodes.ObjectConstructor;

public class ReflectArrow extends Reflect {
    public ReflectArrow(Environment env, FTypeObject selfType, ObjectConstructor def) {
        super(env, selfType, def);
    }

    @Override
    protected void checkType(FType ty) {
        if (!(ty instanceof FTypeArrow)) error(ty + " is not an arrow type.");
    }

    public static final class Make extends NativeFn2 {
        public final FValue applyToArgs(FValue domain0, FValue range0) {
            FType domain = ((ReflectedType) domain0).getTy();
            FType range = ((ReflectedType) range0).getTy();
            return Reflect.make(FTypeArrow.make(domain, range));
        }
    }

    public static final class Domain extends T2T {
        public final FType f(FType x) {
            return ((FTypeArrow) x).getDomain();
        }
    }

    public static final class Range extends T2T {
        public final FType f(FType x) {
            return ((FTypeArrow) x).getRange();
        }
    }
}
