/*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.types.Bool;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeBool;
import com.sun.fortress.interpreter.evaluator.types.FTypeFloat;
import com.sun.fortress.interpreter.evaluator.types.FTypeFloatLiteral;
import com.sun.fortress.interpreter.evaluator.types.FTypeInt;
import com.sun.fortress.interpreter.evaluator.types.FTypeIntLiteral;
import com.sun.fortress.interpreter.evaluator.types.FTypeIntegral;
import com.sun.fortress.interpreter.evaluator.types.FTypeLong;
import com.sun.fortress.interpreter.evaluator.types.FTypeNumber;
import com.sun.fortress.interpreter.evaluator.types.FTypeRange;
import com.sun.fortress.interpreter.evaluator.types.FTypeString;
import com.sun.fortress.interpreter.evaluator.types.FTypeTop;
import com.sun.fortress.interpreter.evaluator.values.FBool;
import com.sun.fortress.interpreter.evaluator.values.FFloat;
import com.sun.fortress.interpreter.evaluator.values.FFloatLiteral;
import com.sun.fortress.interpreter.evaluator.values.FInt;
import com.sun.fortress.interpreter.evaluator.values.FIntLiteral;
import com.sun.fortress.interpreter.evaluator.values.FLong;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.glue.NativeFunction;
import com.sun.fortress.interpreter.useful.HasAt;


public class Primitives {
    static boolean debug = false;

    public static void debugPrint(String debugString) {
        if (debug)
            System.out.print(debugString);
    }

    public static void debugPrintln(String debugString) {
        if (debug)
            System.out.println(debugString);
    }

    public static void install_type(BetterEnv env, String name, FType t) {
        env.putType(name, t);
    }

    public static void install_value(BetterEnv env, String name, FValue v) {
        env.putValue(name, v);
    }

    public static void installPrimitives(BetterEnv env) {
        install_type(env, "ZZ32", FTypeInt.T);
        install_type(env, "ZZ64", FTypeLong.T);
        install_type(env, "Integral", FTypeIntegral.T);
        install_type(env, "String", FTypeString.T);
        install_type(env, "Boolean", FTypeBool.T);
        install_type(env, "RR64", FTypeFloat.T);
        install_type(env, "Number", FTypeNumber.T);
        install_type(env, "ZZ32Range", FTypeRange.T);

        install_type(env, "IntLiteral", FTypeIntLiteral.T);
        install_type(env, "FloatLiteral", FTypeFloatLiteral.T);

        install_value(env, "true", FBool.TRUE);
        install_value(env, "false", FBool.FALSE);

        // Dual identity of true/false
        install_type(env, "true", new Bool("true", FBool.TRUE));
        install_type(env, "false", new Bool("false", FBool.FALSE));

        install_type(env, "Any", FTypeTop.T);

        FTypeNumber.T.addExclude(FTypeString.T);
        FTypeNumber.T.addExclude(FTypeBool.T);
        FTypeInt.T.addExclude(FTypeString.T);
        FTypeInt.T.addExclude(FTypeFloat.T);
        FTypeInt.T.addExclude(FTypeFloatLiteral.T);
        FTypeInt.T.addExclude(FTypeBool.T);
        FTypeInt.T.addExclude(FTypeLong.T);
        FTypeLong.T.addExclude(FTypeString.T);
        FTypeLong.T.addExclude(FTypeFloat.T);
        FTypeLong.T.addExclude(FTypeFloatLiteral.T);
        FTypeLong.T.addExclude(FTypeBool.T);
        FTypeIntegral.T.addExclude(FTypeString.T);
        FTypeIntegral.T.addExclude(FTypeFloat.T);
        FTypeIntegral.T.addExclude(FTypeFloatLiteral.T);
        FTypeIntegral.T.addExclude(FTypeBool.T);
        FTypeFloat.T.addExclude(FTypeString.T);
        FTypeFloat.T.addExclude(FTypeBool.T);
        FTypeString.T.addExclude(FTypeBool.T);
        FTypeString.T.addExclude(FTypeIntLiteral.T);
        FTypeString.T.addExclude(FTypeFloatLiteral.T);

            NativeFunction.registerPrimitives(env);


    }

    static boolean typeMatch(List<FValue> args, List<FType> domain) {
        // debugPrintln("typeMatch " + args + " " + args.size() + " " + domain +
        // " " + domain.size());
        if (args.size() != domain.size())
            return false;
        Iterator<FValue> argsIter = args.iterator();
        Iterator<FType> dIter = domain.iterator();
        try {
            while (argsIter.hasNext()) {
                FValue v = argsIter.next();
                FType t = dIter.next();
                if (!t.typeMatch(v))
                    return false;
            }
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            System.err.println("Failed type match for args=" + args
                    + ", domain=" + domain);
        }
        return true;
    }

//    static String typeMatchError(Primitive prim, List<FValue> args,
//            List<FType> domain) {
//        if (args.size() != domain.size())
//            return "Primitive " + prim + " expected " + domain.size()
//                    + " args but got " + args.size();
//        Iterator<FValue> argsIter = args.iterator();
//        Iterator<FType> dIter = domain.iterator();
//        int i = 0;
//        while (argsIter.hasNext()) {
//            FValue v = argsIter.next();
//            FType t = dIter.next();
//            i++;
//            if (!t.typeMatch(v))
//                return "Primitive " + prim + " parameter " + i
//                        + " expected type " + t + " got type " + v.type();
//        }
//        return "Unknown error, arg types appeared to match";
//    }

    // We want a value that corresponds to domain_type.
    // We have a value that corresponds to arg_type.
    // We know that domain_type isAssignable from arg_type
    static Class getArgClassFromDomainType(FType domain_type, FValue arg) {
        Class argClass = arg.getClass();
        Class argTypeClass = arg.type().getClass();
        Class domainClass = domain_type.getClass();
        while (argTypeClass != domainClass) {
            // debugPrintln("argClass = " + argClass + " argTypeClass = " +
            // argTypeClass + " domainClass = " + domainClass);
            argClass = argClass.getSuperclass();
            argTypeClass = argTypeClass.getSuperclass();
        }
        return argClass;
    }

//    public static FValue applyPrimitive(Primitive prim, List<FValue> args,
//            HasAt loc) {
//        // debugPrintln("applyPrimitive: " + prim.getName());
//        // for (FValue arg: args)
//        // debugPrintln(" arg = " + arg.getString());
//        FValue res = applyPrimitiveHelper(prim, args, loc);
//        // debugPrintln(" res = " + res.getString());
//        return res;
//    }

    static FValue ensure_type(FType t, FValue v, HasAt loc) {
        FType vt = v.type();
        if (vt.subtypeOf(t))
            return v;
        else if (t instanceof FTypeInt && v instanceof FIntLiteral)
            return FInt.make(v.getInt());
        else if (t instanceof FTypeLong && v instanceof FIntLiteral)
            return FLong.make(v.getLong());
        else if (t instanceof FTypeFloat && v instanceof FIntLiteral)
            return FFloat.make(v.getFloat());
        else if (t instanceof FTypeFloat && v instanceof FFloatLiteral)
            return FFloat.make(v.getFloat());
        else if (t instanceof FTypeFloat && v instanceof FInt)
            return FFloat.make(v.getFloat());
        else
            throw new InterpreterError(loc,
                       "NYI: ensure_type for t=" + t + ", v=" + v + " at "
                       + loc.at());
    }

    static List<FValue> ensure_types(List<FType> ts, List<FValue> vs, HasAt loc) {
        ArrayList<FValue> results = new ArrayList<FValue>();
        Iterator<FValue> vi = vs.iterator();
        if (ts.size() != vs.size())
            throw new ProgramError(loc,
                    "Incorrect number of arguments, expected " + ts.size()
                            + ",  got " + vs.size());
        int pos = 0;
        for (FType t : ts) {
            FValue v = vi.next();
            results.add(pos, ensure_type(t, v, loc));
            pos++;
        }
        return results;
    }

//    // Fix Result type later
//    static FValue applyPrimitiveHelper(Primitive prim, List<FValue> args,
//            HasAt loc) {
//        // The following code is disgusting, and will go away when we get
//        // overloading done right
//        // debugPrintln("applyPrimitiveHelper: " + prim.getName());
////        if (prim.getName().equals("op_minus") && (args.size() == 1)) {
////            return op_unary_minus(args.get(0));
////        }
//
//        List<FValue> new_args = ensure_types(prim.getDomain(), args, loc);
//
//        try {
//            Class fvClass = Class.forName("com.sun.fortress.interpreter.evaluator.values.FValue");
//            Class cl = Class.forName(prim.getClassName());
//
//            switch (args.size()) {
//            case 0:
//                // debugPrintln("calling 0 arg version of " + prim.getName());
//                return (FValue) cl.getMethod(prim.getName()).invoke(null);
//            case 1:
//                //debugPrintln("calling 1 arg version of " + prim.getName());
//                return (FValue) cl.getMethod(prim.getName(), fvClass).invoke(
//                        null, new_args.get(0));
//            case 2:
//                //debugPrintln("calling 2 arg version of " + prim.getName());
//                Method m = cl.getMethod(prim.getName(), fvClass, fvClass);
//                //debugPrintln("method takes "+ m.getParameterTypes().length +" arguments");
//                return (FValue) m
//                        .invoke(null, new_args.get(0), new_args.get(1));
//            default:
//                throw new Error("NYI: primitives with more than 2 arguments "
//                        + prim);
//            }
//
//        } catch (java.lang.ClassNotFoundException x) {
//            throw new ProgramError(loc.at() + ": Class Not Found:"
//                    + prim.getClassName());
//        } catch (java.lang.NoSuchMethodException x) {
//            throw new ProgramError(loc.at() + ": Primitive method "
//                    + prim.getName() + " not found");
//        } catch (java.lang.IllegalAccessException x) {
//            throw new ProgramError(loc.at() + ": Primitve method not invoked");
//        } catch (java.lang.reflect.InvocationTargetException x) {
//            Throwable y = x.getTargetException();
//            System.out.println("Message was: " + y.getMessage());
//            System.out.println(y.toString());
//            throw new ProgramError(loc.at() + ": Primitive method not invoked "
//                    + x);
//        }
//    }
}
