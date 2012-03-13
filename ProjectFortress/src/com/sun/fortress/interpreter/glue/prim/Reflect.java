/*******************************************************************************
 Copyright 2008,2010, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.glue.prim;

import static com.sun.fortress.exceptions.InterpreterBug.bug;
import static com.sun.fortress.exceptions.ProgramError.error;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.EvalType;
import com.sun.fortress.interpreter.evaluator.types.*;
import com.sun.fortress.interpreter.evaluator.values.*;
import com.sun.fortress.interpreter.glue.NativeFn0;
import com.sun.fortress.interpreter.glue.NativeFn1;
import com.sun.fortress.interpreter.glue.NativeMeth0;
import com.sun.fortress.interpreter.glue.NativeMeth1;
import com.sun.fortress.nodes.ObjectConstructor;
import com.sun.fortress.nodes.Decl;
import com.sun.fortress.nodes.FnDecl;
import com.sun.fortress.nodes.FnHeader;
import com.sun.fortress.nodes.VarDecl;
import com.sun.fortress.nodes.LValue;
import com.sun.fortress.nodes.Binding;
import com.sun.fortress.nodes.IdOrOp;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.ArrowType;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes_util.Modifiers;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.Useful;

import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Reflect extends NativeConstructor {
    ReflectedType it;

    static GenericConstructor gcongeneric = null;
    static GenericConstructor gconobject = null;
    static GenericConstructor gcontrait = null;
    static GenericConstructor gconarrow = null;
    static GenericConstructor gcongenarrow = null;
    static GenericConstructor gcontuple = null;
    static GenericConstructor gconrest = null;
    static GenericConstructor gconbottom = null;

    public Reflect(Environment env, FTypeObject selfType, ObjectConstructor def) {
        super(env, selfType, def);

        if (gconobject == null) {
            synchronized (this) {
                if (gconobject == null) {
                    Environment toplevel = env.getTopLevel();
                    gcongeneric = (GenericConstructor) toplevel.getRootValue("ReflectGeneric");
                    gconobject = (GenericConstructor) toplevel.getRootValue("ReflectObject");
                    gcontrait = (GenericConstructor) toplevel.getRootValue("ReflectTrait");
                    gconarrow = (GenericConstructor) toplevel.getRootValue("ReflectArrow");
                    gcongenarrow = (GenericConstructor) toplevel.getRootValue("ReflectGenericArrow");
                    gcontuple = (GenericConstructor) toplevel.getRootValue("ReflectTuple");
                    gconrest = (GenericConstructor) toplevel.getRootValue("ReflectRest");
                    gconbottom = (GenericConstructor) toplevel.getRootValue("ReflectBottom");
                }
            }
        }
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
        private final NativeConstructor con;
        private final FType ty;

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
        if (t instanceof FTypeGeneric) {
            gcon = gcongeneric;
        } else if (t instanceof FTypeObject) {
            gcon = gconobject;
        } else if (t instanceof FTypeTrait) {
            gcon = gcontrait;
        } else if (t instanceof FTypeArrow) {
            gcon = gconarrow;
        } else if (t instanceof ReflectGenericArrow.GenericArrowType) {
            gcon = gcongenarrow;
        } else if (t instanceof FTypeTuple) {
            gcon = gcontuple;
        } else if (t instanceof FTypeRest) {
            gcon = gconrest;
        } else if (t instanceof BottomType) {
            gcon = gconbottom;
        } else {
            return error("Not supported type " + t + ".");
        }
        Simple_fcn con = gcon.typeApply(Useful.list(t));
        return (ReflectedType) con.applyToArgs();
    }

    protected static final class ReflectAdapter implements ReflectCollection.CollectionAdapter<FType> {
        public static final ReflectAdapter SINGLETON = new ReflectAdapter();

        public FValue adapt(FType ty) {
            return Reflect.make(ty);
        }
    }

    protected static abstract class T2S extends NativeMeth0 {
        protected abstract String f(FType x);

        public FString applyMethod(FObject self) {
            FType x = ((ReflectedType) self).getTy();
            return FString.make(f(x));
        }
    }

    protected static abstract class T2T extends NativeMeth0 {
        protected abstract FType f(FType x);

        public ReflectedType applyMethod(FObject self) {
            FType x = ((ReflectedType) self).getTy();
            return make(f(x));
        }
    }

    protected static abstract class T2Tc extends NativeMeth0 {
        protected abstract Collection<FType> f(FType x);

        public ReflectCollection.CollectionObject<FType> applyMethod(FObject self) {
            FType x = ((ReflectedType) self).getTy();
            return ReflectCollection.<FType>make(f(x), ReflectAdapter.SINGLETON);
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

    protected static abstract class TT2Tc extends NativeMeth1 {
        protected abstract Collection<FType> f(FType x, FType y);

        public ReflectCollection.CollectionObject<FType> applyMethod(FObject self, FValue other) {
            FType x = ((ReflectedType) self).getTy();
            FType y = ((ReflectedType) other).getTy();
            return ReflectCollection.<FType>make(f(x, y), ReflectAdapter.SINGLETON);
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
        public final FValue applyToArgs(FValue arg) {
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

    public static final class Join extends TT2Tc {
        public final Collection<FType> f(FType x, FType y) {
            Set<FType> join = x.join(y);
            if (join.isEmpty()) {
                /* Empty join means top. */
                return Collections.<FType>singleton(FTypeTop.ONLY);
            } else {
                return join;
            }
        }
    }

    /* XXX Not working correctly; see a comment in FType.meet() */
    public static final class Meet extends TT2Tc {
        public final Collection<FType> f(FType x, FType y) {
            Set<FType> meet = x.meet(y);
            if (meet.isEmpty()) {
                /* Empty meet means bottom. */
                return Collections.<FType>singleton(BottomType.ONLY);
            } else {
                return meet;
            }
        }
    }

    public static final class Extends extends T2Tc {
        public final List<FType> f(FType x) {
            return x.getExtends();
        }
    }

    public static final class Excludes extends T2Tc {
        public final Set<FType> f(FType x) {
            return x.getExcludes();
        }
    }

    public static final class Comprises extends T2Tc {
        public final Set<FType> f(FType x) {
            Set<FType> comprises = x.getComprises();
            if (comprises == null) {
                return Collections.<FType>emptySet();
            } else {
                return comprises;
            }
        }
    }

    public static final class Eq extends TT2B {
        public final boolean f(FType x, FType y) {
            return x == y;
        }
    }

    public static final class Less extends TT2B {
        public final boolean f(FType x, FType y) {
            return x.compareTo(y) < 0;
        }
    }

    public static final class SubtypeOf extends TT2B {
        public final boolean f(FType x, FType y) {
            return x.subtypeOf(y);
        }
    }

    protected static final class DeclAdapter implements ReflectCollection.CollectionAdapter<Decl> {
        private final FType ty;
        private final Environment env;

        public DeclAdapter(FType ty) {
            this.ty = ty;
            if (ty instanceof FTypeTrait) {
                this.env = ((FTypeTrait) ty).getMethodExecutionEnv();
            } else if (ty instanceof FTypeObject) {
                this.env = ((FTypeObject) ty).getMethodExecutionEnv();
            } else {
                this.env = null;
            }
        }

        public FValue adapt(Decl decl) {
            if (decl instanceof FnDecl) {
                FnDecl fndecl = (FnDecl) decl;
                FnHeader header = fndecl.getHeader();
                ArrowType asttype = NodeUtil.genericArrowFromDecl(fndecl);

                ReflectMethod.MethodWrapper method;
                Reflect.ReflectedType mtype;
                List<StaticParam> params = header.getStaticParams();
                if (params.isEmpty()) {
                    if (fndecl.getBody().isSome()) {
                        MethodClosure closure;
                        if (ty instanceof FTypeTrait) {
                            closure = new TraitMethod(env, env, fndecl, ty);
                        } else {
                            closure = new MethodClosure(env, fndecl, ty);
                        }
                        closure.finishInitializing();
                        method = ReflectMethod.make(ty, closure);
                    } else {
                        method = ReflectMethod.NO_BODY;
                    }
                    mtype = Reflect.make(EvalType.getFType(asttype, env));
                } else {
                    GenericMethod closure = new GenericMethod(env, env, fndecl, ty, ty instanceof FTypeTrait);
                    //closure.finishInitializing(); should be done later
                    method = ReflectMethod.make(ty, closure);
                    // rationale: we cannot evaluate generic function type directly as it would need
                    // static arguments. instead we wrap AST type into the temporary FType instance
                    // and unwrap when the method is called with relevant static arguments.
                    mtype = Reflect.make(new ReflectGenericArrow.GenericArrowType(asttype, params, env));
                }

                FString mname = FString.make(((IdOrOp) header.getName()).getText());
                //Modifiers mods = header.getMods();
                return FTuple.make(Useful.<FValue>list(mname, mtype, method));
            } else if (decl instanceof VarDecl) {
                VarDecl vardecl = (VarDecl) decl;
                List<LValue> lhs = vardecl.getLhs();
                if (lhs.size() != 1) {
                    return bug("multiple lvalues (" + lhs.size() + ") in VarDecl node");
                }
                Binding lvalue = lhs.get(0);
                if (lvalue.getIdType().isNone() || !(lvalue.getIdType().unwrap() instanceof Type)) {
                    return bug("type information in VarDecl node is missing");
                }

                FString vname = FString.make(lvalue.getName().getText());
                Reflect.ReflectedType vtype = Reflect.make(
                            EvalType.getFType((Type) lvalue.getIdType().unwrap(), env));
                return FTuple.make(Useful.<FValue>list(vname, vtype, ReflectMethod.NO_BODY));
            } else {
                return error("Not supported Decl node " + decl.getClass());
            }
        }
    }

    public static final class Members extends NativeMeth0 {
        public final ReflectCollection.CollectionObject<Decl> applyMethod(FObject self0) {
            FTraitOrObjectOrGeneric self = (FTraitOrObjectOrGeneric) ((ReflectedType) self0).getTy();
            DeclAdapter adapter = new DeclAdapter(self);
            return ReflectCollection.<Decl>make(self.getASTmembers(), adapter);
        }
    }

    public static final class StaticArgs extends T2Tc {
        public final List<FType> f(FType x) {
            if (x instanceof GenericTypeInstance) {
                return ((GenericTypeInstance) x).getTypeParams();
            } else {
                return Collections.<FType>emptyList();
            }
        }
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
            FType self = ((ReflectedType) self0).getTy();
            List<StaticParam> params;
            if (self instanceof FTypeGeneric) {
                params = ((FTypeGeneric) self).getParams();
            } else if (self instanceof ReflectGenericArrow.GenericArrowType) {
                params = ((ReflectGenericArrow.GenericArrowType) self).getStaticParams();
            } else {
                return bug("Unexpected receiver " + self + " to StaticParams");
            }
            StaticParamAdapter adapter = new StaticParamAdapter(self.getWithin());
            return ReflectCollection.<StaticParam>make(params, adapter);
        }
    }

    public static final class Generic extends T2T {
        public final FType f(FType x) {
            if (x instanceof GenericTypeInstance) {
                return ((GenericTypeInstance) x).getGeneric();
            } else {
                return x;
            }
        }
    }

    public static final class ToString extends T2S {
        public final String f(FType ty) {
            return ty.toString();
        }
    }

    @Override
    protected void unregister() {
        synchronized (this) {
            gcongeneric = gconobject = gcontrait = gconarrow = gcongenarrow =
                gcontuple = gconrest = gconbottom = null;
        }
    }
}
