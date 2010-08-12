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
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.EvalType;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeObject;
import com.sun.fortress.interpreter.evaluator.types.FTypeGeneric;
import com.sun.fortress.interpreter.evaluator.values.*;
import com.sun.fortress.interpreter.glue.NativeMeth;
import com.sun.fortress.interpreter.glue.NativeMeth0;
import com.sun.fortress.nodes.Applicable;
import com.sun.fortress.nodes.ObjectConstructor;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.Useful;

import java.util.List;
import java.util.ArrayList;

public class ReflectGeneric extends Reflect {
    public ReflectGeneric(Environment env, FTypeObject selfType, ObjectConstructor def) {
        super(env, selfType, def);
    }

    @Override
    protected void checkType(FType ty) {
        if (!(ty instanceof FTypeGeneric)) error(ty + " is not a generic type.");
    }

    protected static final class BaseTypeAdapter implements ReflectCollection.CollectionAdapter<BaseType> {
        private Environment env;

        public BaseTypeAdapter(Environment env) {
            this.env = env;
        }

        public FValue adapt(BaseType ty) {
            return Reflect.make(EvalType.getFType(ty, env));
        }
    }

    protected static final class StaticParamAdapter implements ReflectCollection.CollectionAdapter<StaticParam> {
        private BaseTypeAdapter extendsAdapter;

        public StaticParamAdapter(Environment env) {
            extendsAdapter = new BaseTypeAdapter(env);
        }

        public FValue adapt(StaticParam param) {
            FString id = FString.make(param.getName().getText());
            ReflectCollection.CollectionObject<BaseType> extendsClause =
                ReflectCollection.<BaseType>make(param.getExtendsClause(), extendsAdapter);
            return FTuple.make(Useful.<FValue>list(id, extendsClause));
        }
    }

    public static final class StaticParams extends NativeMeth0 {
        public ReflectCollection.CollectionObject<StaticParam> applyMethod(FObject self0) {
            FTypeGeneric self = (FTypeGeneric) ((ReflectedType) self0).getTy();
            StaticParamAdapter adapter = new StaticParamAdapter(self.getWithin());
            return ReflectCollection.<StaticParam>make(self.getParams(), adapter);
        }
    }

    public static final class TypeApply extends NativeMeth {
        public int getArity() {
            return -1; // This will be ignored due to the new definition of init.
        }

        @Override
        protected void init(Applicable app, boolean isFunctionalMethod) {
            if (this.a != null) {
                bug("Duplicate NativeApp.init call.");
            }
            this.a = app;
            int aty = NodeUtil.getParams(app).size();
            if (NodeUtil.getReturnType(app) == null || NodeUtil.getReturnType(app).isNone()) {
                error(app, "Please specify a Fortress return type.");
            }
        }

        public ReflectedType applyMethod(FObject self, List<FValue> args) {
            FTypeGeneric generic = (FTypeGeneric) ((ReflectedType) self).getTy();
            List<FType> typeargs = new ArrayList<FType>();
            for (FValue v : args) {
                typeargs.add(((ReflectedType) v).getTy());
            }
            return Reflect.make(generic.make(typeargs, generic.getDecl()));
        }
    }
}
