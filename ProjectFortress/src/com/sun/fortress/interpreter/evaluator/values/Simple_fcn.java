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

import java.util.List;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.useful.Hasher;
import com.sun.fortress.useful.MagicNumbers;
import com.sun.fortress.useful.Useful;


abstract public class Simple_fcn extends SingleFcn {
    Simple_fcn(BetterEnv within) {
        super(within);
    }

    public String getString() {
        return getFnName().toString() + Useful.listInParens(getDomain()) + "@" + at();
    }

    abstract public List<FType> getDomain();

    /**
     * Returns can-this-function-be-called-now.
     * That is, does the function have all its types assigned?
     * @return
     */
    abstract boolean getFinished();

    /**
     *
     * @return A string suitable for identifying the source of the function.
     */
    abstract public String at();

    // NOTE: I believe it is ok for functions to use object identity for
    // equals and hashCode().

    static class SignatureEquivalence extends Hasher<Simple_fcn> {
        @Override
        public long hash(Simple_fcn x) {
            long a = (long) x.getFnName().hashCode() * MagicNumbers.s;
            long b =  (long) x.getDomain().hashCode() * MagicNumbers.l;
            // System.err.println("Hash of " + x + " yields " + a + " and " + b);

            return a + b;
        }

        @Override
        public boolean equiv(Simple_fcn x, Simple_fcn y) {
            List<FType> dx = x.getDomain();
            List<FType> dy = y.getDomain();
            if (dx.size() != dy.size())
                return false;
            if (! x.getFnName().equals(y.getFnName()))
                return false;
            for (int i = 0; i < dx.size(); i++) {
                if (! dx.get(i).equals(dy.get(i)))
                    return false;
            }
            return true;
        }

    }

    public static Hasher<Simple_fcn> signatureEquivalence = new SignatureEquivalence();

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

    public static Hasher<Simple_fcn> nameEquivalence = new NameEquivalence();


}
