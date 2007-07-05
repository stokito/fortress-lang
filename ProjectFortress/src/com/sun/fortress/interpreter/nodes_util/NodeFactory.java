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

package com.sun.fortress.interpreter.nodes_util;

import java.util.Collections;
import java.util.List;
import java.math.BigInteger;
import com.sun.fortress.interpreter.nodes.*;
import com.sun.fortress.interpreter.useful.Fn;
import com.sun.fortress.interpreter.useful.Some;
import com.sun.fortress.interpreter.useful.None;
import com.sun.fortress.interpreter.useful.Useful;
import com.sun.fortress.interpreter.useful.HasAt;
import com.sun.fortress.interpreter.parser.precedence.resolver.PrecedenceMap;

public class NodeFactory {
    /******************************************************
    private static int anonymousFnNameSequence;
    static synchronized int anonymousFnNameSerial() {
        return ++anonymousFnNameSequence;
    }

    public static AnonymousFnName makeAnonymousFnName(Span span) {
        return new AnonymousFnName(span, anonymousFnNameSerial(), new None<HasAt>());
    }

    public static AnonymousFnName makeAnonymousFnName(HasAt at) {
        return new AnonymousFnName(new Span(), anonymousFnNameSerial(),
                                   new Some<HasAt>(at));
    }
    ******************************************************/

    public static CharLiteral makeCharLiteral(Span span, String s) {
        return new CharLiteral(span, s, s.charAt(0));
    }

    /******************************************************
    private static int constructorFnNameSequence;
    private static int constructorFnNameSerial() {
        return ++constructorFnNameSequence;
    }
    public static ConstructorFnName makeConstructorFnName(DefOrDecl def) {
        return new ConstructorFnName(def.getSpan(), constructorFnNameSerial(), def);
    }
    ******************************************************/

    public static Contract makeContract() {
        return new Contract(new Span(), Collections.<Expr> emptyList(),
                            Collections.<EnsuresClause> emptyList(),
                            Collections.<Expr> emptyList());
    }

    public static DottedId makeDottedId(Span span, String s) {
        return new DottedId(span, Useful.list(s));
    }

    public static DottedId makeDottedId(Span span, Id s) {
        return new DottedId(span, Useful.list(s.getName()));
    }

    public static DottedId makeDottedId(Span span, Id s, List<Id> ls) {
        return new DottedId(span, Useful.prependMapped(s, ls,
                                                // fn(x) => x.getName()
                                                new Fn<Id, String>() {
                                                    @Override
                                                    public String apply(Id x) {
                                                        return x.getName();
                                                    }
        }));
    }

    public static FloatLiteral makeFloatLiteral(Span span, String s) {
        BigInteger intPart;
        BigInteger numerator;
        int denomBase;
        int denomPower;

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
        return new FloatLiteral(span, s,
                                intPart, numerator, denomBase, denomPower);
    }

    public static Fun makeFun(Span span, String string) {
        return new Fun(span, new Id(span, string));
    }

    /**
     * Call this only for names that have no location. (When/if this constructor
     * disappears, it will be because we have a better plan for those names, and
     * its disappearance will identify all those places that need updating).
     */
    public static Fun makeFun(String string) {
        Span span = new Span();
        return new Fun(span, new Id(span, string));
    }

    public static Id makeId(String string) {
        return new Id(new Span(), string);
    }

    public static IntLiteral makeIntLiteral(Span span, BigInteger val) {
        return new IntLiteral(span, val.toString(), val);
    }

    public static IntLiteral makeIntLiteral(Span span, String s) {
        BigInteger val;
        int underLoc = s.indexOf('_');
        if (underLoc == -1) {
            val = new BigInteger(s);
        } else {
            String digits = s.substring(0, underLoc);
            String base_digits = s.substring(underLoc + 1);
            int base;
            if (!Unicode.charactersOverlap(base_digits, "0123456789")) {
                base = Unicode.numberToValue(base_digits);
            } else {
                base = Integer.parseInt(base_digits);
            }
            digits = dozenalHack(digits, base);
            val = new BigInteger(digits, base);
        }
        return new IntLiteral(span, s, val);
    }

   static String dozenalHack(String digits, int base) {
        if (base == 12 && Unicode.charactersOverlap(digits, "xXeE")) {
            digits = digits.replace('x', 'A');
            digits = digits.replace('X', 'A');
            digits = digits.replace('e', 'B');
            digits = digits.replace('E', 'B');
        }
        return digits;
   }

    public static Name makeName(Span span, Id id) {
        return new Name(span, new Some<Id>(id), new None<Op>());
    }

    public static Name makeName(Span span, Op op) {
        return new Name(span, new None<Id>(), new Some<Op>(op));
    }

    public static NatParam makeNatParam(String name) {
        return new NatParam(new Span(), new Id(new Span(), name));
    }

    public static Op makeOp(Span span, String name) {
        return new Op(span, PrecedenceMap.ONLY.canon(name));
    }

    public static SimpleTypeParam makeSimpleTypeParam(String name) {
        return new SimpleTypeParam(new Span(), new Id(new Span(), name),
                                   new None<List<TypeRef>>(), false);
    }

    public static VarRefExpr makeVarRefExpr(Span span, String s) {
        return new VarRefExpr(span, new Id(span, s));
    }

    public static VoidLiteral makeVoidLiteral(Span span) {
        return new VoidLiteral(span, "");
    }
}
