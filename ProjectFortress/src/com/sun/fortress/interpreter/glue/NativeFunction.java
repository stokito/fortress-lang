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

/*
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.sun.fortress.interpreter.glue;

import java.util.Collections;
import java.util.List;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.BuildEnvironments;
import com.sun.fortress.interpreter.evaluator.InterpreterError;
import com.sun.fortress.interpreter.evaluator.ProgramError;
import com.sun.fortress.interpreter.evaluator.tasks.BaseTask;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeDynamic;
import com.sun.fortress.interpreter.evaluator.types.FTypeFloat;
import com.sun.fortress.interpreter.evaluator.types.FTypeInt;
import com.sun.fortress.interpreter.evaluator.types.FTypeIntegral;
import com.sun.fortress.interpreter.evaluator.types.FTypeNumber;
import com.sun.fortress.interpreter.evaluator.types.FTypeVoid;
import com.sun.fortress.interpreter.evaluator.values.Closure;
import com.sun.fortress.interpreter.evaluator.values.FFloat;
import com.sun.fortress.interpreter.evaluator.values.FInt;
import com.sun.fortress.interpreter.evaluator.values.FIntLiteral;
import com.sun.fortress.interpreter.evaluator.values.FLong;
import com.sun.fortress.interpreter.evaluator.values.FString;
import com.sun.fortress.interpreter.evaluator.values.FStringLiteral;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.FVoid;
import com.sun.fortress.interpreter.evaluator.values.OverloadedFunction;
import com.sun.fortress.interpreter.evaluator.values.Parameter;
import com.sun.fortress.interpreter.useful.HasAt;
import com.sun.fortress.interpreter.useful.Useful;


public class NativeFunction extends Closure {

    BetterEnv nativeEnvironment;
    static long startTime = 0;

    public void register(BetterEnv e) {
        nativeEnvironment = e;
        BuildEnvironments be = new BuildEnvironments(e);
        be.putOrOverloadOrShadow(getAt(), e, getFnName(), this);
    }

    static public void registerAll(BetterEnv e, NativeFunction[] funcs) {
        for (NativeFunction f : funcs)
            f.register(e);
    }

    static public void registerPrimitives(BetterEnv e) {
        NativeFunction[] nfs = primitives(e);
        registerAll(e, nfs);
        // GHACK follows!
        // TODO  Environments really need to be able to do this for us.
        for (NativeFunction nf : nfs) {
            FValue f = e.getValue(nf.getFnName().name());
            if (f instanceof OverloadedFunction) {
                ((OverloadedFunction) f).finishInitializing();
            }
        }
    }

    /** Creates a new instance of NativeFunction */
    protected NativeFunction(BetterEnv e, String name, List<Parameter> params,
            FType returnType) {
        super(e, new NativeApplicable(name));
        setParamsAndReturnType(params, returnType);
    }

    public Closure finishInitializing() {
        return this;
    }

    public BetterEnv getWithin() {
        return nativeEnvironment;
    }

    public abstract static class NF1 extends NativeFunction {
        public NF1(BetterEnv e, String name, String pname, FType ptype, FType returnType) {
            super(e, name, Useful.list(new Parameter(pname, ptype)), returnType);
        }

        public FValue applyInner(List<FValue> args, HasAt loc, BetterEnv envForInference) {
            int l = args.size();
            if (l == 1) {
                try {
                    return a(args.get(0), loc);
                } catch (ProgramError ex) {
                    throw ex;
                } catch (RuntimeException ex) {
                    throw new ProgramError(loc, "Wrapped exception", ex);
                } catch (Error ex) {
                    throw new ProgramError(loc, "Wrapped error", ex);
                }
            } else {
                throw new InterpreterError(loc, "No native method " + toString()
                        + " with " + args.size() + " args");
            }
        }

        abstract FValue a(FValue x, HasAt loc);
    }

    public abstract static class NF0 extends NativeFunction {
        public NF0(BetterEnv e, String name, FType returnType) {
            super(e, name, Collections.<Parameter> emptyList(), returnType);
        }

        public FValue applyInner(List<FValue> args, HasAt loc, BetterEnv envForInference) {
            int l = args.size();
            if (l == 0) {
                try {
                    return a(loc);
                } catch (ProgramError ex) {
                    throw ex;
                } catch (RuntimeException ex) {
                    throw new ProgramError(loc, "Wrapped exception", ex);
                } catch (Error ex) {
                    throw new ProgramError(loc, "Wrapped error", ex);
                }
            } else {
                throw new InterpreterError(loc, "No native method " + toString()
                        + " with " + args.size() + " args");
            }
        }

        abstract FValue a(HasAt loc);
    }

    public abstract static class NF2 extends NativeFunction {
        public NF2(BetterEnv e, String name, String pname1, FType ptype1, String pname2,
                FType ptype2, FType returnType) {
            super(e, name, Useful.list(new Parameter(pname1, ptype1),
                    new Parameter(pname2, ptype2)), returnType);
        }

        public FValue applyInner(List<FValue> args, HasAt loc, BetterEnv envForInference) {
            int l = args.size();
            if (l == 2) {
                try {
                    return a(args.get(0), args.get(1), loc);
                } catch (ProgramError ex) {
                    throw ex;
                } catch (RuntimeException ex) {
                    throw new ProgramError(loc, "Wrapped exception", ex);
                } catch (Error ex) {
                    throw new ProgramError(loc, "Wrapped error", ex);
                }
            } else {
                throw new InterpreterError(loc, "No native method " + toString()
                        + " with " + args.size() + " args");
            }
        }

        abstract FValue a(FValue x, FValue y, HasAt loc);
    }

    private static boolean intable(FValue v) {
        Class cl = v.getClass();
        return (cl == FInt.class) || (cl == FIntLiteral.class);
    }

     // THIS IS WRONG.
    private final static int ERROR = 0;
    private final static int FINT = 1;
    private final static int FLONG = 2;
    private final static int FFLOAT = 7;
    private final static int FSTRING = 8;
    private final static int FINT_LITERAL = 3;
    private final static int FFLOAT_LITERAL = 5;

    /*
     *  IL IL -> IL 1
     *  I IL  -> I  3
     *  I I   -> I  3
     *  I F   -> F  7
     *  F I   -> F  7
     *  F F   -> F  7
     *  FL FL -> F  7
     *  FL IL -> FL 5
     *  FL I  -> F  7 = 5|3
     *  F FL  -> F  7
     *  FL F  -> F  7
     *  S *   -> S  8
     * !S S   -> E  > 8
     *
     */

    private static int binaryResultType(FValue x, FValue y) {
        Class xcl = x.getClass();
        Class ycl = y.getClass();
        // FInt, FLong, FString, FFloat, FIntLiteral, FFloatLiteral
        return ERROR;
    }



    private static boolean stringy(FValue v) {
        Class cl = v.getClass();
        return (cl == FString.class || cl == FStringLiteral.class);
    }

    static NativeFunction[] primitives(final BetterEnv e) {
        NativeFunction[] p = {
            new NF2(e, "^", "x", FTypeInt.T, "y", FTypeIntegral.T,
                    FTypeNumber.T) {
                FValue a(FValue x, FValue y, HasAt loc) {
                    long pow = y.getLong();
                    if (pow >= 0) {
                        int b = x.getInt();
                        int r = 1;
                        for (; pow > 0; pow--)
                            r *= b;
                        return FInt.make(r);
                    }
                    return FFloat.make(Math.pow(x.getFloat(), y.getFloat()));
                }
            },
            new NF2(e, "^", "x", FTypeIntegral.T, "y", FTypeIntegral.T,
                    FTypeNumber.T) {
                FValue a(FValue x, FValue y, HasAt loc) {
                    long pow = y.getLong();
                    if (pow >= 0) {
                        long b = x.getLong();
                        long r = 1;
                        for (; pow > 0; pow--)
                            r *= b;
                        return FLong.make(r);
                    }
                    return FFloat.make(Math.pow(x.getFloat(), y.getFloat()));
                }
            },
            new NF2(e, "^", "x", FTypeNumber.T, "y", FTypeNumber.T,
                    FTypeFloat.T) {
                FValue a(FValue x, FValue y, HasAt loc) {
                    return FFloat.make(Math.pow(x.getFloat(), y.getFloat()));
                }
	    },  new NF1(e, "recordTime","x", FTypeDynamic.T, FTypeVoid.T) {
		FValue a(FValue x, HasAt loc) {
                    startTime = System.currentTimeMillis();
                    return FVoid.V;
                }
            },	new NF1(e, "printTime", "x", FTypeDynamic.T, FTypeVoid.T) {
		FValue a(FValue x, HasAt loc) {
                System.err.println("Operation took " + (System.currentTimeMillis() - startTime) +" milliseconds");
                return FVoid.V;
		}
            },	new NF1(e, "printTaskTrace", "x", FTypeDynamic.T, FTypeVoid.T) {
		FValue a(FValue x, HasAt loc) {
                BaseTask.getCurrentTask().printTaskTrace();
                return FVoid.V;
		}

            }

    };
        return p;
    }

   /**
    * GCD, for positive 64-bit integers.
    * @param u
    * @param v
    * @return
    */

   protected static long gcd(long u, long v) {
       /* Thank you, Wikipedia. */
        long k = 0;
        if (u == 0)
            return v;
        if (v == 0)
            return u;
        while ((u & 1) == 0 && (v & 1) == 0) {
        /*
         * while both u and v
         * are even
         */
            u >>>= 1; /* shift u right, dividing it by 2 */
            v >>>= 1; /* shift v right, dividing it by 2 */
            k++; /* add a power of 2 to the final result */
        }
        /* At this point either u or v (or both) is odd */
        do {
            if ((u & 1) == 0) /* if u is even */
                u >>>= 1; /* divide u by 2 */
            else if ((v & 1) == 0) /* else if v is even */
                v >>>= 1; /* divide v by 2 */
            else if (u >= v) /* u and v are both odd */
                u = (u - v) >>> 1;
            else
                /* u and v both odd, v > u */
                v = (v - u) >>> 1;
        } while (u > 0);
        return v << k; /* returns v * 2^k */
    }

protected static long choose(long n, long k) {
    if (k > n/2) k = n - k;
    if (k == 0 || k == n) return 1;
    if (k == 1) return n;
    // k <= n/2
    // will multiply (k) terms, n-k+1 through n-k+k
    // will divide by k terms, 1 through k
    // Note that if we divide after multiplying, it will
    // always be even.
    // Proof left as a tricky exercise to the com.sun.fortress.interpreter.reader.
    long accum = 1;
    for (long j= 1; j <= k; j++) {
        long m = n-k+j;
        accum = accum * m;
        accum = accum / j;
    }
    return (long) accum;
}

protected static long mod(long xi, long yi) {
    long ri = xi % yi;
    if (ri == 0)
        return 0;

    if (yi > 0) {
        if (xi >= 0)
            return ri;
        return ri + yi;
    } else {
        if (xi >= 0)
            return ri + yi;
        return ri;
    }
}

}
