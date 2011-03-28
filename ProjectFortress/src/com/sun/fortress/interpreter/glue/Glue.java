/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.glue;

import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.types.FTraitOrObject;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.IntNat;
import com.sun.fortress.interpreter.evaluator.types.TypeRange;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.GenericFunctionOrConstructor;
import com.sun.fortress.interpreter.evaluator.values.Simple_fcn;
import com.sun.fortress.useful.Factory1P;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.Useful;

import java.util.ArrayList;
import java.util.List;


public class Glue {

    static public int natFromGeneric(FType vt, String trait, String nat) {
        if (vt instanceof FTraitOrObject) {
            FTraitOrObject fto = (FTraitOrObject) vt;
            List<FType> traits = fto.getTransitiveExtends();
            for (FType t : traits) {

                String fttn = t.getName();
                if (fttn.equals(trait)) {
                    Number n = t.getWithin().getNat(nat);
                    return n.intValue();
                }


            }
        }
        return -1;
    }

    static public FType typeFromGeneric(FType vt, String trait, String param) {
        if (vt instanceof FTraitOrObject) {
            FTraitOrObject fto = (FTraitOrObject) vt;
            List<FType> traits = fto.getTransitiveExtends();
            for (FType t : traits) {

                String fttn = t.getName();
                if (fttn.equals(trait)) {
                    return t.getWithin().getLeafType(param); // leaf
                }

            }
        }
        return null;
    }

    /**
     * Returns true if vt is called name or if any trait
     * that (transitively) extends vt is called name.
     */
    static public boolean extendsGenericTrait(FType vt, String name) {
        if (vt instanceof FTraitOrObject) {
            FTraitOrObject fto = (FTraitOrObject) vt;

            List<FType> traits = fto.getTransitiveExtends();
            for (FType t : traits) {

                String fttn = t.getName();
                if (fttn.equals(name)) return true;

            }
        }
        return false;
    }

    static public int arrayRank(FValue v) {
        FType vt = v.type();
        return natFromGeneric(vt, "Rank", "n");
    }

    /**
     * This method depends on naming conventions for encoding
     * array traits.  The input axis is assumed to be numbered
     * starting at zero, but the library convention is numbering
     * starting at one.
     * <p/>
     * If the supplied value is NOT an array, then the length
     * returned is -1.  If it is an array but the axis exceeds
     * the dimensionality of the array, then the length returned
     * is -2.  Otherwise, the length returned is the extent of
     * the array along that axis.
     */
    static public int lengthAlongArrayAxis(FValue v, int axis) {
        // axis+1 converts zero to one.
        String goalTraitName = "Indexed" + (axis + 1);
        FType vt = v.type();
        int x = natFromGeneric(vt, goalTraitName, "n");
        if (x >= 0) return x;
        x = natFromGeneric(vt, "Rank", "n");
        if (x > 0) return -2;
        return -1;
    }

    public static List<FType> parametersForGenericIndexed(FType bestGuess, int[] is) {
        List<FType> l = new ArrayList<FType>(1 + 2 * is.length);
        l.add(bestGuess);
        FType zero = IntNat.make(0);
        for (int i = 0; i < is.length; i++) {
            l.add(zero);
            l.add(IntNat.make(is[i]));
        }
        return l;
    }

    public static List<FType> parametersForGenericIndexed(FType bestGuess, List<TypeRange> ltr) {
        List<FType> l = new ArrayList<FType>(1 + 2 * ltr.size());
        l.add(bestGuess);
        for (TypeRange i : ltr) {
            l.add(i.getBase());
            l.add(i.getSize());
        }
        return l;
    }

    public static Simple_fcn instantiateGenericConstructor(Environment e, String genericName, FType T, int[] nats) {
        FValue thingMakerValue = e.getRootValue(genericName);
        GenericFunctionOrConstructor thingMaker = (GenericFunctionOrConstructor) thingMakerValue;
        List<FType> l = parametersForGenericIndexed(T, nats);
        return thingMaker.typeApply(l);
    }

    public static Simple_fcn instantiateGenericConstructor(Environment e, String genericName, FType T) {
        FValue thingMakerValue = e.getRootValue(genericName);
        GenericFunctionOrConstructor thingMaker = (GenericFunctionOrConstructor) thingMakerValue;
        List<FType> l = Useful.list(T);
        return thingMaker.typeApply(l);
    }

    public static FTraitOrObject instantiateGenericType(Environment e,
                                                        String genericName,
                                                        FType T,
                                                        List<TypeRange> nats,
                                                        HasAt x) {
        List<FType> l = parametersForGenericIndexed(T, nats);
        FTraitOrObject f = instantiateGenericType(e, genericName, l, x);
        return f;
    }

    private static FTraitOrObject instantiateGenericType(Environment e, String genericName, List<FType> l, HasAt x) {
        FType thingMakerValue = e.getRootType(genericName); // top level
        Factory1P<List<FType>, FTraitOrObject, HasAt> thingMaker =
                (Factory1P<List<FType>, FTraitOrObject, HasAt>) thingMakerValue;
        FTraitOrObject f = thingMaker.make(l, x);
        return f;
    }

}
