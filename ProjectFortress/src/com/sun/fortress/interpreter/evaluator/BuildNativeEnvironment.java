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

package com.sun.fortress.interpreter.evaluator;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.env.LazilyEvaluatedCell;
import com.sun.fortress.interpreter.evaluator.types.FTraitOrObjectOrGeneric;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeGeneric;
import com.sun.fortress.interpreter.evaluator.types.FTypeObject;
import com.sun.fortress.interpreter.evaluator.values.Constructor;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.GenericConstructor;
import com.sun.fortress.interpreter.evaluator.values.GenericNativeConstructor;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.GenericWithParams;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.ObjectDecl;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes_util.ExprFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.NI;
import edu.rice.cs.plt.tuple.Option;

import static com.sun.fortress.interpreter.evaluator.ProgramError.error;
import static com.sun.fortress.interpreter.evaluator.InterpreterBug.bug;

public class BuildNativeEnvironment extends BuildEnvironments {

    public BuildNativeEnvironment(BetterEnv within) {
        super(within);
        // TODO Auto-generated constructor stub
    }

    public static Constructor nativeConstructor(BetterEnv containing,
            FTypeObject ft, GenericWithParams x, String fname) {
        String pack = containing.getValue("package").getString();
        String classname = pack + "." + fname;
        try {
            Class cl = Class.forName(classname);
            // cl must extend Constructor,
            // cl must have a constructor BetterEnv env, FTypeObject selfType,
            // GenericWithParams def
            if (Constructor.class.isAssignableFrom(cl)) {
                java.lang.reflect.Constructor ccl = cl.getDeclaredConstructor(
                        BetterEnv.class, FTypeObject.class,
                        GenericWithParams.class);
                return (Constructor) ccl.newInstance(containing, ft, x);
            } else {
                return error("Native class " + classname + " must extend Constructor");
            }

        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new Error(e);
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new Error(e);
        } catch (NoSuchMethodException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new Error(e);
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new Error(e);
        } catch (InstantiationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new Error(e);
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new Error(e);
        } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new Error(e);
        }

    }

    protected void forObjectDecl1(ObjectDecl x) {
        // List<Modifier> mods;

        Id name = x.getName();

        List<StaticParam> staticParams = x.getStaticParams();
        Option<List<Param>> params = x.getParams();

        // List<Type> throws_;
        // List<WhereClause> where;
        // Contract contract;
        // List<Decl> defs = x.getDecls();
        String fname = NodeUtil.nameString(name);
        FTraitOrObjectOrGeneric ft;
        ft = staticParams.isEmpty()
                 ? new FTypeObject(fname, containing, x, params, x.getDecls(), x)
                 : new FTypeGeneric(containing, x, x.getDecls(), x);

        // Need to check for overloaded constructor.

        guardedPutType(fname, ft, x);

        if (params.isSome()) {
            if (!staticParams.isEmpty()) {
                GenericConstructor gen = new GenericNativeConstructor(containing, x, fname);
                guardedPutValue(containing, fname, gen, x);

            } else {
                // TODO need to deal with constructor overloading.

                // If parameters are present, it is really a constructor
                // BetterEnv interior = new SpineEnv(e, x);
                Constructor cl = nativeConstructor(containing,
                        (FTypeObject) ft, x, fname);
                guardedPutValue(containing, fname, cl, x);
                // doDefs(interior, defs);
            }

        } else {
            if (!staticParams.isEmpty()) {
                // A parameterized singleton is a sort of generic value.
                bug(x,"Native generic singleton objects not yet implemented");
                GenericConstructor gen = new GenericConstructor(containing, x, name);
                guardedPutValue(containing, obfuscatedSingletonConstructorName(fname, x), gen, x);

            } else {
                // It is a singleton; do not expose the constructor, do
                // visit the interior environment.
                // BetterEnv interior = new SpineEnv(e, x);

                // TODO - binding into "containing", or "bindInto"?

                Constructor cl = nativeConstructor(containing,
                        (FTypeObject) ft, x, fname);
                guardedPutValue(containing, obfuscatedSingletonConstructorName(fname, x), cl, x);

                // Create a little expression to run the constructor.
                Expr init = ExprFactory.makeTightJuxt(x.getSpan(), ExprFactory
                        .makeVarRef(x.getSpan(), obfuscatedSingletonConstructorName(fname, x)),
                        ExprFactory.makeVoidLiteralExpr(x.getSpan()));
                FValue init_value = new LazilyEvaluatedCell(init, containing);
                putValue(bindInto, fname, init_value);

                // doDefs(interior, defs);
            }
        }

        scanForFunctionalMethodNames(ft, x.getDecls());

    }
}
