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

import java.math.BigInteger;

import com.sun.fortress.interpreter.useful.MagicNumbers;


public class FloatLiteral extends NumberLiteral {

    /*
     * The value of the literal is intPart + numerator / (denomBase^denomPower).
     * Literals with zero fraction part are canonically num=0, base=1, power=0
     */

    BigInteger intPart;

    BigInteger numerator;

    int denomBase;

    int denomPower;

    public FloatLiteral(Span span, String s) {
        super(span, s);
        decode(s);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof FloatLiteral) {
            FloatLiteral fl = (FloatLiteral) o;
            return denomBase == fl.denominatorBase()
                    && denomPower == fl.denominatorPower()
                    && numerator.equals(fl.numerator())
                    && intPart.equals(fl.intPart());

        }
        return false;
    }

    @Override
    public int hashCode() {
        return MagicNumbers.n * numerator().hashCode() + MagicNumbers.i
                * intPart().hashCode() + MagicNumbers.B * denominatorBase()
                + MagicNumbers.p * denominatorPower();
    }

    /**
     * @param s
     */
    public void decode(String s) {

        // Trim leading zeroes
        while (s.length() > 1 && s.charAt(0) == '0') {
            s = s.substring(1);
        }

        int dotLoc = s.indexOf('.');
        int underLoc = s.indexOf('_');

        if (dotLoc == -1) {
            // No fraction part.
            numerator = BigInteger.ZERO;
            denomBase = 1;
            denomPower = 0;
            int base;
            String digits;

            if (underLoc == -1) {
                digits = s;
                base = 10;
            } else {
                digits = s.substring(0, underLoc);
                // Base other, no ".", parse as BigInteger and convert.
                String base_digits = s.substring(underLoc + 1);

                if (!Unicode.charactersOverlap(base_digits, "0123456789")) {
                    base = Unicode.numberToValue(base_digits);
                } else {
                    base = Integer.parseInt(base_digits);
                }
            }
            digits = dozenalHack(digits, base);
            intPart = new BigInteger(digits, base);

        } else {
            // There is a fraction part.

            int base;

            if (underLoc == -1) {
                base = 10;
                underLoc = s.length();
            } else {
                String base_digits = s.substring(underLoc + 1);
                if (!Unicode.charactersOverlap(base_digits, "0123456789")) {
                    base = Unicode.numberToValue(base_digits);
                } else {
                    base = Integer.parseInt(base_digits);
                }
            }
            {
                String digits = s.substring(0, dotLoc);
                if (digits.length() > 0) {
                    digits = dozenalHack(digits, base);
                    intPart = new BigInteger(digits, base);
                } else {
                    intPart = BigInteger.ZERO;
                }

                digits = s.substring(dotLoc + 1, underLoc);

                // TODO Getting the rounding and overflow dead right is hard.
                while (digits.length() > 1 && digits.endsWith("0")) {
                    digits = digits.substring(0, digits.length() - 1);
                }

                if (digits.length() == 0 || "0".equals(digits)) {
                    numerator = BigInteger.ZERO;
                    denomBase = 1;
                    denomPower = 0;

                } else {
                    digits = dozenalHack(digits, base);
                    numerator = new BigInteger(digits, base);
                    denomBase = base;
                    denomPower = digits.length();
                }
            }
        }
    }

    public FloatLiteral(Span span) {
        super(span);
    }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forFloatLiteral(this);
    }

    @Override
    BigInteger intPart() {
        return intPart;
    }

    @Override
    BigInteger numerator() {
        // TODO Auto-generated method stub
        return numerator;
    }

    @Override
    int denominatorBase() {
        // TODO Auto-generated method stub
        return denomBase;
    }

    @Override
    int denominatorPower() {
        // TODO Auto-generated method stub
        return denomPower;
    }

}
