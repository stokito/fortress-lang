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
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeObject;
import com.sun.fortress.interpreter.evaluator.types.FTypeTuple;
import com.sun.fortress.interpreter.evaluator.values.*;
import com.sun.fortress.nodes.ObjectConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ReflectTuple extends Reflect {
    public ReflectTuple(Environment env, FTypeObject selfType, ObjectConstructor def) {
        super(env, selfType, def);
    }

    @Override
    protected void checkType(FType ty) {
        if (!(ty instanceof FTypeTuple)) error(ty + " is not a tuple type.");
    }

    public static final class Size extends T2I {
        public final int f(FType x) {
            return ((FTypeTuple) x).getTypes().size();
        }
    }

    public static final class Get extends TI2T {
        public FType f(FType x, int i) {
            List<FType> types = ((FTypeTuple) x).getTypes();
            try {
                return types.get(i);
            }
            catch (IndexOutOfBoundsException e) {
                return bug(errorMsg("Tuple element index ", i, " out of bounds, length=", types.size()), e);
            }
        }
    }
}
