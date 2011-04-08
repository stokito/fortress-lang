/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator;

import com.sun.fortress.compiler.WellKnownNames;
import static com.sun.fortress.exceptions.InterpreterBug.bug;
import static com.sun.fortress.exceptions.ProgramError.errorMsg;
import com.sun.fortress.interpreter.env.LazilyEvaluatedCell;
import com.sun.fortress.interpreter.env.NonApiWrapper;
import com.sun.fortress.interpreter.evaluator.types.FTraitOrObjectOrGeneric;
import com.sun.fortress.interpreter.evaluator.types.FTypeGeneric;
import com.sun.fortress.interpreter.evaluator.types.FTypeObject;
import com.sun.fortress.interpreter.evaluator.values.Constructor;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.GenericConstructor;
import com.sun.fortress.interpreter.evaluator.values.GenericNativeConstructor;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.ExprFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import edu.rice.cs.plt.tuple.Option;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

public class BuildNativeEnvironment extends BuildTopLevelEnvironments {

    public BuildNativeEnvironment(Environment within, Map<String, NonApiWrapper> linker) {
        super(within, linker);
        // TODO Auto-generated constructor stub
    }

    public static Constructor nativeConstructor(Environment containing,
                                                FTypeObject ft,
                                                ObjectConstructor x,
                                                String fname) {
        String pack = containing.getTopLevel().getRootValue("package").getString();
        String classname = pack + "." + fname;
        try {
            Class cl = Class.forName(classname);
            // cl must extend Constructor,
            // cl must have a constructor BetterEnv env, FTypeObject selfType,
            // ObjectConstructor def
            java.lang.reflect.Constructor ccl = cl.getDeclaredConstructor(Environment.class,
                                                                          FTypeObject.class,
                                                                          ObjectConstructor.class);
            return (Constructor) ccl.newInstance(containing, ft, x);
        }
        catch (ClassCastException e) {
            return bug(x, containing, errorMsg("Native class ", classname, " must extend Constructor"), e);
        }
        catch (InstantiationException e) {
            return bug(x, containing, errorMsg("Native class must have constructor ",
                                               classname,
                                               "(Environment, FTypeObject, GenericWithParams)"), e);
        }
        catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new Error(e);
        }
        catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new Error(e);
        }
        catch (NoSuchMethodException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new Error(e);
        }
        catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new Error(e);
        }
        catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new Error(e);
        }
        catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new Error(e);
        }

    }

    protected void forObjectDecl1(ObjectDecl x) {
        // List<Modifier> mods;

        Id name = NodeUtil.getName(x);

        List<StaticParam> staticParams = NodeUtil.getStaticParams(x);
        Option<List<Param>> params = NodeUtil.getParams(x);

        // List<Type> throws_;
        // List<WhereClause> where;
        // Contract contract;
        // List<Decl> defs = NodeUtil.getDecls(x);
        String fname = NodeUtil.nameString(name);
        FTraitOrObjectOrGeneric ft;
        ft = staticParams.isEmpty() ?
             new FTypeObject(fname, containing, x, params, NodeUtil.getDecls(x), x) :
             new FTypeGeneric(containing, x, NodeUtil.getDecls(x), x);

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
                Constructor cl = nativeConstructor(containing, (FTypeObject) ft, x, fname);
                guardedPutValue(containing, fname, cl, x);
                // doDefs(interior, defs);
            }

        } else {
            if (!staticParams.isEmpty()) {
                // A parameterized singleton is a sort of generic value.
                bug(x, "Native generic singleton objects not yet implemented");
                GenericConstructor gen = new GenericConstructor(containing, x, name);
                guardedPutValue(containing, WellKnownNames.obfuscatedSingletonConstructorName(fname, x), gen, x);

            } else {
                // It is a singleton; do not expose the constructor, do
                // visit the interior environment.
                // BetterEnv interior = new SpineEnv(e, x);

                // TODO - binding into "containing", or "bindInto"?

                Constructor cl = nativeConstructor(containing, (FTypeObject) ft, x, fname);
                guardedPutValue(containing, WellKnownNames.obfuscatedSingletonConstructorName(fname, x), cl, x);

                // Create a little expression to run the constructor.
                Expr init = ExprFactory.makeTightJuxt(NodeUtil.getSpan(x),
                                                      ExprFactory.makeVarRef(NodeUtil.getSpan(x),
                                                                             WellKnownNames.obfuscatedSingletonConstructorName(
                                                                                     fname,
                                                                                     x),
                                                                             0),
                                                      ExprFactory.makeVoidLiteralExpr(NodeUtil.getSpan(x)));
                FValue init_value = new LazilyEvaluatedCell(init, containing);
                putValue(bindInto, fname, init_value);

                // doDefs(interior, defs);
            }
        }

        scanForFunctionalMethodNames(ft, NodeUtil.getDecls(x));

    }
}
