/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.values;

import com.sun.fortress.exceptions.InterpreterBug;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.nodes.AbstractNode;
import com.sun.fortress.nodes_util.ErrorMsgMaker;
import com.sun.fortress.useful.EquivalenceClass;
import com.sun.fortress.useful.Fn;
import com.sun.fortress.useful.Useful;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public abstract class FValue {
    //   public static final FValue ZERO = new FInt(0);
    //  public static final FValue ONE = new FInt(1);

    protected static String s(Object node) {
        return node == null ?
               "NULL" :
               (node instanceof AbstractNode) ? ErrorMsgMaker.makeErrorMsg((AbstractNode) node) : node.toString();
    }

    public String toString() {
        // We want this toString to look as close as possible to Fortress concrete syntax.
        // EricAllen 3/13/2008
        return getString() + ":" + getClass().getSimpleName();
        //getClass().getSimpleName() + " " + getString();
    }

    public String getString() {
        return "No String Representation Implemented for " + getClass().getSimpleName();
    }

    public abstract FType type();

    public FValue getValue() {
        return this;
    }

    public int getInt() {
        throw new InterpreterBug("getInt not implemented for " + getClass().getSimpleName());
    }

    public BigInteger getBigInteger() {
        throw new InterpreterBug("getBigInteger not implemented for " + getClass().getSimpleName());
    }

    public long getLong() {
        throw new InterpreterBug("getLong not implemented for " + getClass().getSimpleName());
    }

    public int getNN32() {
        throw new InterpreterBug("getNN32 not implemented for " + getClass().getSimpleName());
    }

    public long getNN64() {
        throw new InterpreterBug("getNN64 not implemented for " + getClass().getSimpleName());
    }

    public double getFloat() {
        throw new InterpreterBug("getFloat not implemented for " + getClass().getSimpleName());
    }

    public float getRR32() {
        throw new InterpreterBug("getRR32 not implemented for " + getClass().getSimpleName());
    }

    public int getChar() {
        throw new InterpreterBug("getChar not implemented for " + getClass().getSimpleName());
    }

    /**
     * seqv can ignore pointer equivalence; that must be checked before calling.
     */
    public abstract boolean seqv(FValue other);

    // map "select type"
    static protected List<FType> typeListFromParameters(List<Parameter> params) {
        ArrayList<FType> al = new ArrayList<FType>(params.size());
        for (Parameter p : params) {
            al.add(p.param_type);
        }
        return al;
    }

    // map "select type"
    static protected List<FType> typeListFromValues(List<FValue> params) {
        ArrayList<FType> al = new ArrayList<FType>(params.size());
        for (FValue p : params) {
            al.add(p.type());
        }
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
                if (c != 0) return c;
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
                if (c != 0) return c;
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
                if (c != 0) return c;
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
