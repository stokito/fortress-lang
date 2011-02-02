/*******************************************************************************
    Copyright 2008,2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.nodes_util;

import java.lang.Math;
import java.io.IOException;
import java.io.Serializable;

import static com.sun.fortress.exceptions.InterpreterBug.bug;

/**
 * Modifiers represents a set of modifiers applied to some Fortress
 * declaration.  For simplicity we use one Modifiers type everywhere,
 * but provide methods to check whether a set of modifiers can be
 * applied to a given syntactic construct.  Modifiers can be combined
 * using the combine method (which returns fresh Modifiers).
 *
 * modifier
 * TraitMod      ::= AbsTraitMod | private
 * AbsTraitMod   ::= value | test
 * ObjectMods    ::= TraitMods
 * AbsObjectMods ::= AbsTraitMods
 * MdMod         ::= FnMod | getter | override | setter
 * AbsMdMod      ::= AbsFnMod | abstract | getter | override | setter
 * FnMod         ::= AbsFnMod | private
 * AbsFnMod      ::= LocalFnMod | test
 * LocalFnMod    ::= atomic | io
 * ParamFldMod   ::= var | hidden | settable | wrapped
 * VarMod        ::= AbsVarMod | private
 * AbsVarMod     ::= var | test
 * FldMod        ::= var | AbsFldMod
 * AbsFldMod     ::= ApiFldMod | wrapped | private
 * ApiFldMod     ::= hidden | settable
 */
public final class Modifiers implements Serializable {
    /*
     * Local constants
     */
    private static final int ABSTRACT = 0;
    private static final int ATOMIC = 1;
    private static final int GETTER = 2;
    private static final int HIDDEN = 3;
    private static final int IO_M = 4;
    private static final int OVERRIDE = 5;
    private static final int PRIVATE = 6;
    private static final int SETTABLE = 7;
    private static final int SETTER = 8;
    private static final int TEST = 9;
    private static final int VALUE = 10;
    private static final int VAR = 11;
    private static final int WIDENS = 12;
    private static final int WRAPPED = 13;
    private static final int NEXT_AVAIL_MODIFIER = 14;

    protected static final Modifiers[] modifiersByBit = {
        modifierForBit(ABSTRACT),
        modifierForBit(ATOMIC),
        modifierForBit(GETTER),
        modifierForBit(HIDDEN),
        modifierForBit(IO_M),
        modifierForBit(OVERRIDE),
        modifierForBit(PRIVATE),
        modifierForBit(SETTABLE),
        modifierForBit(SETTER),
        modifierForBit(TEST),
        modifierForBit(VALUE),
        modifierForBit(VAR),
        modifierForBit(WIDENS),
        modifierForBit(WRAPPED)
    };

    /**
     * Singletons that define individual modifiers.
     */
    public static final Modifiers None = new Modifiers(0);

    /**
     * e.g.) abstract m(x: ZZ32): ()
     */
    public static final Modifiers Abstract = modifiersByBit[ABSTRACT];
    /**
     * e.g.) atomic f(x: ZZ32): ()
     */
    public static final Modifiers Atomic = modifiersByBit[ATOMIC];
    /**
     * e.g.) getter velocity(): (RR Velocity)^3
     */
    public static final Modifiers Getter = modifiersByBit[GETTER];
    /**
     * e.g.) hidden x: ZZ32
     */
    public static final Modifiers Hidden = modifiersByBit[HIDDEN];
    /**
     * e.g.) io print(s: String): ()
     */
    public static final Modifiers IO = modifiersByBit[IO_M];
    /**
     * e.g.) override m(x: ZZ32): ()
     */
    public static final Modifiers Override = modifiersByBit[OVERRIDE];
    /**
     * e.g.) private f(): ()
     */
    public static final Modifiers Private = modifiersByBit[PRIVATE];
    /**
     * e.g.) settable message: Maybe[\String\]
     */
    public static final Modifiers Settable = modifiersByBit[SETTABLE];
    /**
     * e.g.) setter velocity(v: (RR Velocity)^3): ()
     */
    public static final Modifiers Setter = modifiersByBit[SETTER];
    /**
     * e.g.) test object TestSuite(testFunctions = modifierForBit({}) end
     */
    public static final Modifiers Test = modifiersByBit[TEST];
    /**
     * e.g.) value object IntEmpty extends List[\ZZ32\] end
     */
    public static final Modifiers Value = modifiersByBit[VALUE];
    /**
     * e.g.) var x: ZZ32
     */
    public static final Modifiers Var = modifiersByBit[VAR];
    /**
     * e.g.) coerce (x: RR32) widens = modifierForBit(...
     */
    public static final Modifiers Widens = modifiersByBit[WIDENS];
    /**
     * e.g.) wrapped val: Dictionary[\T\]
     */
    public static final Modifiers Wrapped = modifiersByBit[WRAPPED];

    /**
     * Sets of interesting modifiers
     */
    public static final Modifiers AbsTraitMod   = Value.combine(Test);
    public static final Modifiers TraitMod      = AbsTraitMod.combine(Private);
    public static final Modifiers ObjectMod     = TraitMod;
    public static final Modifiers AbsObjectMod  = AbsTraitMod;
    public static final Modifiers LocalFnMod    = Atomic.combine(IO);
    public static final Modifiers AbsFnMod      = LocalFnMod.combine(Test);
    public static final Modifiers AbsMdMod      = AbsFnMod.combine(Abstract.combine(Getter.combine(Override.combine(Setter))));
    public static final Modifiers FnMod         = AbsFnMod.combine(Private);
    public static final Modifiers MdMod         = FnMod.combine(Getter.combine(Override.combine(Setter)));
    public static final Modifiers ParamFldMod   = Var.combine(Hidden.combine(Settable.combine(Wrapped)));
    public static final Modifiers AbsVarMod     = Var.combine(Test);
    public static final Modifiers VarMod        = AbsVarMod.combine(Private);
    public static final Modifiers LocalVarMod   = Var;
    public static final Modifiers ApiFldMod     = Hidden.combine(Settable);
    public static final Modifiers AbsFldMod     = ApiFldMod.combine(Wrapped.combine(Private));
    public static final Modifiers FldMod        = Var.combine(AbsFldMod);

    public static final Modifiers Mutable       = Var.combine(Settable);
    public static final Modifiers GetterSetter  = Getter.combine(Setter);
    public static final Modifiers MethodMod     = MdMod.combine(GetterSetter).combine(Abstract);

    private static final String[] modifierNames = {
        "abstract",
        "atomic",
        "getter",
        "hidden",
        "io",
        "override",
        "private",
        "settable",
        "setter",
        "test",
        "value",
        "var",
        "widens",
        "wrapped"
    };

    private static final String modifierEncodings = "baghioplstfvwr";

    private static Modifiers[] modifierDecodings = new Modifiers[128];

    static {
        for (int i = 0; i < NEXT_AVAIL_MODIFIER; i++) {
            char c = modifierEncodings.charAt(i);
            modifierDecodings[(int)c] = modifiersByBit[i];
        }
    }

    /**
     * We represent Modifiers as an immutable bit set.
     */
    private final int bits;

    private Modifiers(int bits) {
        this.bits = bits;
    }

    private static Modifiers modifierForBit(int bit) {
        return new Modifiers(1 << bit);
    }

    /**
     * Returns fresh Modifiers containing those in m and in this.
     */
    public Modifiers combine(Modifiers m) {
        return new Modifiers(bits | m.bits);
    }

    /**
     * like m1.combine(m2), except nulls are treated as Modifiers.None.
     */
    public static Modifiers combine(Modifiers m1, Modifiers m2) {
        m1 = nonNull(m1);
        m2 = nonNull(m2);
        return m1.combine(m2);
    }

    /**
     * like m1.combine(m2).combine(m3), except nulls are treated as Modifiers.None.
     */
    public static Modifiers combine(Modifiers m1, Modifiers m2, Modifiers m3) {
        m1 = nonNull(m1);
        m2 = nonNull(m2);
        m3 = nonNull(m3);
        return m1.combine(m2).combine(m3);
    }

    /**
     * Possibly abstract; if null, None, otherwise Abstract.
     */
    public static Modifiers possiblyAbstract(Object o) {
        return (o==null)?None:Abstract;
    }

    /**
     * Possibly some other modifier, or if null, None.
     */
    public static Modifiers possible(Object o, Modifiers m) {
        return (o==null)?None:m;
    }

    /**
     * Returns fresh Modifiers containing any modifiers in this not in m.
     */
    public Modifiers remove(Modifiers m) {
        return new Modifiers(bits & (~ m.bits));
    }

    /**
     * true iff this contains all modifiers in m
     */
    public boolean containsAll(Modifiers m) {
        return ((bits & m.bits) == m.bits);
    }

    /**
     * true iff this and m contain any modifiers in common
     */
    public boolean containsAny(Modifiers m) {
        return ((bits & m.bits) != 0);
    }

    public boolean equals(Object m) {
        if (!(m instanceof Modifiers)) return false;
        return (bits == ((Modifiers)m).bits);
    }

    public int hashCode() {
        return bits;
    }

    public String toString() {
        if (bits == 0) return "";
        int i = 0;
        int MOD = 1;
        for (; true; i++) {
            if ((bits & MOD) != 0) break;
            MOD <<= 1;
        }
        String r0 = modifierNames[i];
        MOD <<= 1;
        i++;
        if (MOD > bits) return r0;
        StringBuilder r = new StringBuilder(r0);
        for (; MOD <= bits; i++) {
            if ((bits & MOD) != 0) {
                r.append(' ');
                r.append(modifierNames[i]);
            }
            MOD <<= 1;
        }
        return r.toString();
    }

    public static Modifiers nonNull(Modifiers m) {
        if (m==null) return None;
        return m;
    }

    public boolean isAbstract() {
        return containsAny(Abstract);
    }

    public boolean isAtomic() {
        return containsAny(Atomic);
    }

    public boolean isGetter() {
        return containsAny(Getter);
    }

    public boolean isHidden() {
        return containsAny(Hidden);
    }

    public boolean isIo() {
        return containsAny(IO);
    }

    public boolean isOverride() {
        return containsAny(Override);
    }

    public boolean isPrivate() {
        return containsAny(Private);
    }

    public boolean isSettable() {
        return containsAny(Settable);
    }

    public boolean isSetter() {
        return containsAny(Setter);
    }

    public boolean isTest() {
        return containsAny(Test);
    }

    public boolean isValue() {
        return containsAny(Value);
    }

    public boolean isVar() {
        return containsAny(Var);
    }

    public boolean isWidens() {
        return containsAny(Widens);
    }

    public boolean isWrapped() {
        return containsAny(Wrapped);
    }

    public boolean isMutable() {
        return containsAny(Mutable);
    }

    public boolean isGetterSetter() {
        return containsAny(GetterSetter);
    }

    public boolean isEmpty() {
        return ( this == None || bits == 0 );
    }

    /**
     * Encodes modifiers as a quoted but otherwise rather succint string designed for machine consumption.
     */
    public String encode() {
        StringBuilder r = new StringBuilder();
        try {
            encodeTo(r);
        } catch (IOException e) {
            bug("Should not happen!");
        }
        return r.toString();
    }

    /**
     * Append quoted encoding of modifiers to provided Appendable.
     */
    public void encodeTo(Appendable a) throws IOException {
        a.append('"');
        if (bits==0) {
            a.append('"');
            return;
        }
        int i = 0;
        int MODS = 1;
        for (; MODS <= bits; i++) {
            if ((bits & MODS) != 0) a.append(modifierEncodings.charAt(i));
            MODS <<= 1;
        }
        a.append('"');
        return;
    }

    /**
     * Decode string produced by encode.  Tries to be robust to
     * presence/absence of quotes inserted by encode.
     *
     * Ignores bad characters; is this a good idea?  If not there
     * needs to be special-casing for quotes.
     */
    public static Modifiers decode(String s) {
        Modifiers m = None;
        if (s==null || s.equals("")) return m;
        for (int i = 0; i < s.length(); i++) {
            int c = (int)(s.charAt(i));
            if (c >= 128) continue;
            Modifiers e = modifierDecodings[c];
            if (e != null) m = m.combine(e);
        }
        return m;
    }

    static final double factor = Math.pow(2.0,(double)NEXT_AVAIL_MODIFIER);

    /**
     * Generates a random but valid set of modifiers for testing.
     */
    public static Modifiers randomForTest() {
        int i = (int)(factor * Math.random());
        return new Modifiers(i);
    }

}
