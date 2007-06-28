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

package com.sun.fortress.interpreter.nodes;

import com.sun.fortress.interpreter.nodes_util.Span;
import com.sun.fortress.interpreter.nodes_util.Unicode;
import java.math.BigInteger;

import com.sun.fortress.interpreter.useful.MagicNumbers;


public class IntLiteral extends NumberLiteral {

    BigInteger val;

    @Override
    BigInteger intPart() {
        return val;
    }

    @Override
    BigInteger numerator() {
        // TODO Auto-generated method stub
        return BigInteger.ZERO;
    }

    @Override
    int denominatorBase() {
        // TODO Auto-generated method stub
        return 1;
    }

    @Override
    int denominatorPower() {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * @param s
     */
    public static BigInteger decode(String s) {
        int underLoc = s.indexOf('_');
        if (underLoc == -1) {
            return new BigInteger(s);
        } else {
            String digits = s.substring(0, underLoc);
            String base_digits = s.substring(underLoc + 1);
            int base;
            if (!Unicode.charactersOverlap(base_digits, "0123456789")) {
                base = Unicode.numberToValue(base_digits);
            } else {
                base = Integer.parseInt(base_digits);
            }
            digits = NumberLiteral.dozenalHack(digits, base);
            return new BigInteger(digits, base);
        }
    }

    public IntLiteral(Span span) {
        super(span);
    }

    public IntLiteral(Span span, BigInteger val) {
        super(span, val.toString());
        this.val = val;
    }

    public IntLiteral(Span span, String s) {
        super(span, s);
        this.val = decode(s);
    }

    public String toString() {
        return val.toString();
    }

    /*
     * Note that this ignores the text of the literal.
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof IntLiteral) {
            IntLiteral il = (IntLiteral) o;
            return val.equals(il.getVal());
        }
        return false;
    }

    /*
     * Note that this ignores the text of the literal.
     */
    @Override
    public int hashCode() {
        return MagicNumbers.i * val.hashCode();
    }

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forIntLiteral(this);
    }

    public BigInteger getVal() {
        return val;
    }

}
