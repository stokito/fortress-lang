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

package com.sun.fortress.interpreter.evaluator.values;
import java.util.ArrayList;
import java.util.List;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import com.sun.fortress.interpreter.evaluator.InterpreterError;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.useful.EquivalenceClass;
import com.sun.fortress.interpreter.useful.Fn;
import com.sun.fortress.interpreter.useful.Useful;


public abstract class FValue {
    //   public static final FValue ZERO = new FInt(0);
    //  public static final FValue ONE = new FInt(1);

    public String toString() {
        return getClass().getSimpleName() + " " + getString();
    }
    public String getString() { return "No String Representation Implemented for " + getClass().getSimpleName();}
    public abstract FType type();
    public BufferedReader getBufferedReader() { throw new InterpreterError("getBufferedReader not implemented for " + getClass().getSimpleName()); }
    public BufferedWriter getBufferedWriter() { throw new InterpreterError("getBufferedWriter not implemented for " + getClass().getSimpleName()); }
    public int getInt() { throw new InterpreterError("getInt not implemented for "  + getClass().getSimpleName());}
    public long getLong() { throw new InterpreterError("getLong not implemented for "  + getClass().getSimpleName());}
    public double getFloat() { throw new InterpreterError("getFloat not implemented for "  + getClass().getSimpleName());}
    /**
     * @param ftype The ftype to set.
     */
    public void setFtype(FType ftype) {
        throw new IllegalStateException("Cannot set ftype of "+getClass().getSimpleName());
    }

    public void setFtypeUnconditionally(FType ftype) {
    }

    // map "select type"
    static protected List<FType> typeListFromParameters(List<Parameter> params) {
        ArrayList<FType> al = new ArrayList<FType>(params.size());
        for (Parameter p : params) al.add(p.param_type);
        return al;
    }

    // map "select type"
    static protected List<FType> typeListFromValues(List<FValue> params) {
        ArrayList<FType> al = new ArrayList<FType>(params.size());
        for (FValue p : params) al.add(p.type());
        return al;
    }

  static final public class AsTypes implements EquivalenceClass<FValue, FType> {

            public int compare(FValue x, FType yt) {
                FType xt = x.type();
                return xt.compareTo(yt);
            }

            public int compareLeftKeys(FValue x, FValue y) {
                FType xt = x.type();
                FType yt = y.type();
                return xt.compareTo(yt);
            }

            public int compareRightKeys(FType xt, FType yt) {
               return xt.compareTo(yt);
            }

            public FType translate(FValue x) {
                return x.type();
            }

        }

        static final public AsTypes asTypes = new AsTypes();

        static final public class AsTypesList implements EquivalenceClass<List<FValue>, List<FType>> {

            public int compare(List<FValue> x, List<FType> y) {
                int l0 = x.size();
                int l1 = y.size();
                if (l0 < l1) return -1;
                if (l0 > l1) return 1;
                for (int i = 0; i < l0; i++) {
                    int c = x.get(i).type().compareTo(y.get(i));
                    if (c != 0)
                        return c;
                }
                return 0;
            }

            public int compareLeftKeys(List<FValue> x, List<FValue> y) {
                int l0 = x.size();
                int l1 = y.size();
                if (l0 < l1) return -1;
                if (l0 > l1) return 1;
                for (int i = 0; i < l0; i++) {
                    int c = x.get(i).type().compareTo(y.get(i).type());
                    if (c != 0)
                        return c;
                }
                return 0;
            }

            public int compareRightKeys(List<FType> x, List<FType> y) {
                int l0 = x.size();
                int l1 = y.size();
                if (l0 < l1) return -1;
                if (l0 > l1) return 1;
                for (int i = 0; i < l0; i++) {
                    int c = x.get(i).compareTo(y.get(i));
                    if (c != 0)
                        return c;
                }
                return 0;
            }

            public List<FType> translate(List<FValue> x) {
                return Useful.applyToAll(x, valToType);
            }
        }

        static final public Fn<FValue, FType> valToType = new Fn<FValue, FType>() {

            @Override
            public FType apply(FValue x) {
                return x.type();
            }

        };

        static final public AsTypesList asTypesList = new AsTypesList();


}
