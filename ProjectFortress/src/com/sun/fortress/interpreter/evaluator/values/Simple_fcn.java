/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.values;

import static com.sun.fortress.exceptions.InterpreterBug.bug;
import static com.sun.fortress.exceptions.ProgramError.errorMsg;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.types.BottomType;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeArrow;
import com.sun.fortress.useful.Hasher;
import com.sun.fortress.useful.MagicNumbers;
import com.sun.fortress.useful.Useful;

import java.util.List;


abstract public class Simple_fcn extends SingleFcn {
    Simple_fcn(Environment within) {
        super(within);
    }

    FType ret_type;

    public void setFtypeUnconditionally(FType ftype) {
        super.setFtypeUnconditionally(ftype);
        FType t = ((FTypeArrow) ftype).getRange();
        // Hack around lack of type inference
        if (t == BottomType.ONLY) t = com.sun.fortress.interpreter.evaluator.types.FTypeTop.ONLY;
        ret_type = t;
    }

    protected FValue check(FValue x) {
        FType t = ret_type;
        // Jam on, for now.
        if (true || t.typeMatch(x)) return x;
        return bug(errorMsg("Function ",
                            this,
                            " returned ",
                            x,
                            " but signature required ",
                            t,
                            ", supers are ",
                            x.type().getTransitiveExtends()));
    }

    public String getString() {
        return at() + ": " + getFnName().toString() + Useful.listInParens(getDomain()) + ":" + getRange();
    }

    public FType getRange() {
        return ret_type;
    }

    /**
     * Returns can-this-function-be-called-now.
     * That is, does the function have all its types assigned?
     *
     * @return
     */
    abstract boolean getFinished();

    /**
     * @return A string suitable for identifying the source of the function.
     */
    abstract public String at();

    // NOTE: I believe it is ok for functions to use object identity for
    // equals and hashCode().

    static class SignatureEquivalence extends Hasher<Simple_fcn> {
        @Override
        public long hash(Simple_fcn x) {
            long a = (long) x.getFnName().hashCode() * MagicNumbers.s;
            long b = (long) x.getDomain().hashCode() * MagicNumbers.l;
            // System.err.println("Hash of " + x + " yields " + a + " and " + b);

            return a + b;
        }

        @Override
        public boolean equiv(Simple_fcn x, Simple_fcn y) {
            List<FType> dx = x.getDomain();
            List<FType> dy = y.getDomain();
            if (dx.size() != dy.size()) return false;
            if (!x.getFnName().equals(y.getFnName())) return false;
            for (int i = 0; i < dx.size(); i++) {
                if (!dx.get(i).equals(dy.get(i))) return false;
            }
            return true;
        }

    }

    public static final Hasher<Simple_fcn> signatureEquivalence = new SignatureEquivalence();

    static class NameEquivalence extends Hasher<Simple_fcn> {
        @Override
        public long hash(Simple_fcn x) {
            long a = (long) x.getFnName().hashCode() * MagicNumbers.N;
            return a;
        }

        @Override
        public boolean equiv(Simple_fcn x, Simple_fcn y) {
            return x.getFnName().equals(y.getFnName());
        }

    }

    public static final Hasher<Simple_fcn> nameEquivalence = new NameEquivalence();


}
