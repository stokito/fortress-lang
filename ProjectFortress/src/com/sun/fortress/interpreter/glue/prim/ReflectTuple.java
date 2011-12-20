/*******************************************************************************
 Copyright 2010, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

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
import com.sun.fortress.interpreter.glue.NativeApp;
import com.sun.fortress.nodes.ObjectConstructor;
import com.sun.fortress.nodes.Applicable;
import com.sun.fortress.nodes_util.NodeUtil;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

public class ReflectTuple extends Reflect {
    public ReflectTuple(Environment env, FTypeObject selfType, ObjectConstructor def) {
        super(env, selfType, def);
    }

    @Override
    protected void checkType(FType ty) {
        if (!(ty instanceof FTypeTuple)) error(ty + " is not a tuple type.");
    }

    public static final class Make extends NativeApp {
        public int getArity() {
            return -1; // This value is not going to be used anyway.
        }

        // See also NativeApp.init.
        protected void init(Applicable app, boolean isFunctionalMethod) {
            if (this.a != null) {
                bug("Duplicate NativeApp.init call.");
            }
            this.a = app;
            if (NodeUtil.getReturnType(app) == null || NodeUtil.getReturnType(app).isNone()) {
                error(app, "Please specify a Fortress return type.");
            }
        }

        public final FValue applyToArgs(List<FValue> types0) {
            List<FType> types = new ArrayList<FType>();
            for (FValue v : types0) {
                types.add(((ReflectedType) v).getTy());
            }
            return Reflect.make(FTypeTuple.make(types));
        }
    }

    public static final class Types extends T2Tc {
        public final List<FType> f(FType x) {
            return ((FTypeTuple) x).getTypes();
        }
    }
}
