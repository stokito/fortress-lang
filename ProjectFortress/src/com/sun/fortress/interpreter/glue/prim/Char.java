/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.glue.prim;

import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.types.FTypeObject;
import com.sun.fortress.interpreter.evaluator.values.*;
import com.sun.fortress.interpreter.glue.NativeFn1;
import com.sun.fortress.interpreter.glue.NativeMeth0;
import com.sun.fortress.interpreter.glue.NativeMeth1;
import com.sun.fortress.nodes.ObjectConstructor;

import java.util.List;

/**
 * The Char type.
 */
public class Char extends NativeConstructor {

    public Char(Environment env, FTypeObject selfType, ObjectConstructor def) {
        super(env, selfType, def);
    }

    protected FNativeObject makeNativeObject(List<FValue> args, NativeConstructor con) {
        FChar.setConstructor(this);
        return FChar.ZERO;
    }

    @Override
    protected void unregister() {
        FChar.resetConstructor();
    }

    private static abstract class s2B extends NativeMeth0 {
        protected abstract boolean f(int self);

        public final FValue applyMethod(FObject self) {
            return FBool.make(f(self.getChar()));
        }
    }

    private static abstract class s2C extends NativeMeth0 {
        protected abstract int f(int self);

        public final FValue applyMethod(FObject self) {
            return FChar.make(f(self.getChar()));
        }
    }

    private static abstract class s2I extends NativeMeth0 {
        protected abstract int f(int self);

        public final FValue applyMethod(FObject self) {
            return FInt.make(f(self.getChar()));
        }
    }

    private static abstract class sC2B extends NativeMeth1 {
        protected abstract boolean f(int self, int other);

        public final FValue applyMethod(FObject self, FValue other) {
            return FBool.make(f(self.getChar(), other.getChar()));
        }
    }

    private static abstract class sI2I extends NativeMeth1 {
        protected abstract int f(int self, int other);

        public final FValue applyMethod(FObject self, FValue other) {
            return FInt.make(f(self.getChar(), other.getInt()));
        }
    }

    private static abstract class I2C extends NativeFn1 {
        abstract protected int f(int i);

        public final FValue applyToArgs(FValue i) {
            return FChar.make(f(i.getInt()));
        }
    }

    /* Operators for object Char */
    public static final class Eq extends sC2B {
        protected boolean f(int x, int y) {
            return x == y;
        }
    }

    public static final class LessThan extends sC2B {
        protected boolean f(int x, int y) {
            return x < y;
        }
    }

    public static final class SimEq extends sC2B {
        protected boolean f(int x, int y) {
            int lowerX = Character.toLowerCase(x);
            int lowerY = Character.toLowerCase(y);
            if (lowerX == lowerY) return true;
            return false;
        }
    }

    public static final class NotSimEq extends sC2B {
        protected boolean f(int x, int y) {
            int lowerX = Character.toLowerCase(x);
            int lowerY = Character.toLowerCase(y);
            if (lowerX != lowerY) return true;
            return false;
        }
    }

    public static final class LessNotSim extends sC2B {
        protected boolean f(int x, int y) {
            int lowerX = Character.toLowerCase(x);
            int lowerY = Character.toLowerCase(y);
            if (lowerX < lowerY) return true;
            return false;
        }
    }

    public static final class LessSim extends sC2B {
        protected boolean f(int x, int y) {
            int lowerX = Character.toLowerCase(x);
            int lowerY = Character.toLowerCase(y);
            if (lowerX <= lowerY) return true;
            return false;
        }
    }

    public static final class GreaterNotSim extends sC2B {
        protected boolean f(int x, int y) {
            int lowerX = Character.toLowerCase(x);
            int lowerY = Character.toLowerCase(y);
            if (lowerX > lowerY) return true;
            return false;
        }
    }

    public static final class GreaterSim extends sC2B {
        protected boolean f(int x, int y) {
            int lowerX = Character.toLowerCase(x);
            int lowerY = Character.toLowerCase(y);
            if (lowerX >= lowerY) return true;
            return false;
        }
    }


    public static final class CodePoint extends s2I {
        protected int f(int self) {
            return self;
        }
    }

    public static final class Chr extends I2C {
        protected int f(int i) {
            return i;
        }
    }

    public static final class ToString extends NativeMeth0 {
        public FValue applyMethod(FObject self) {
            return FString.make(((FChar) self).getString());
        }
    }

    public static final class ToExprString extends NativeMeth0 {
        public FValue applyMethod(FObject self) {
            return FString.make(((FChar) self).toString());
        }
    }

    /* Returns the Unicode directionality property for the given character (Unicode code point). */
    public static final class GetDirectionality extends s2I {
        protected int f(int x) {
            return Character.getDirectionality(x);
        }
    }

    /* Returns the integer value that the specified character (Unicode code point) represents. */
    public static final class GetNumericValue extends s2I {
        protected int f(int x) {
            return Character.getNumericValue(x);
        }
    }

    /* Returns a value indicating a character's general category. */
    public static final class GetType extends s2I {
        protected int f(int x) {
            return Character.getType(x);
        }
    }

    /* Determines if a character (Unicode code point) is defined in Unicode. */
    public static final class IsDefined extends s2B {
        protected boolean f(int x) {
            return Character.isDefined(x);
        }
    }

    /* Determines if the specified character (Unicode code point) is a digit. */
    public static final class IsDigit extends s2B {
        protected boolean f(int x) {
            return Character.isDigit(x);
        }
    }

    /* Determines if the character (Unicode code point) may be part of a Fortress identifier as other than the first character. */
    public static final class IsFortressIdentifierPart extends s2B {
        protected boolean f(int x) {
            return Character.isLetterOrDigit(x) || (x == (int) '_') || (x == (int) '\'');
        }
    }

    /* Determines if the character (Unicode code point) is permissible as the first character in a Fortress identifier. */
    public static final class IsFortressIdentifierStart extends s2B {
        protected boolean f(int x) {
            return Character.isLetter(x) || (x == (int) '_');
        }
    }

    /* Determines if the given Char value is a high-surrogate code unit (also known as leading-surrogate code unit). */
    public static final class IsHighSurrogate extends s2B {
        protected boolean f(int x) {
            return (x <= 0xFFFF) && Character.isHighSurrogate((char) x);
        }
    }

    /* Determines if the specified character (Unicode code point) should be regarded as an ignorable character in a Java identifier or a Unicode identifier. */
    public static final class IsIdentifierIgnorable extends s2B {
        protected boolean f(int x) {
            return Character.isIdentifierIgnorable(x);
        }
    }

    /* Determines if the referenced character (Unicode code point) is an ISO control character. */
    public static final class IsISOControl extends s2B {
        protected boolean f(int x) {
            return Character.isISOControl(x);
        }
    }

    /* Determines if the character (Unicode code point) may be part of a Java identifier as other than the first character. */
    public static final class IsJavaIdentifierPart extends s2B {
        protected boolean f(int x) {
            return Character.isJavaIdentifierPart(x);
        }
    }

    /* Determines if the character (Unicode code point) is permissible as the first character in a Java identifier. */
    public static final class IsJavaIdentifierStart extends s2B {
        protected boolean f(int x) {
            return Character.isJavaIdentifierStart(x);
        }
    }

    /* Determines if the specified character (Unicode code point) is a letter. */
    public static final class IsLetter extends s2B {
        protected boolean f(int x) {
            return Character.isLetter(x);
        }
    }

    /* Determines if the specified character (Unicode code point) is a letter or digit. */
    public static final class IsLetterOrDigit extends s2B {
        protected boolean f(int x) {
            return Character.isLetterOrDigit(x);
        }
    }

    /* Determines if the specified character (Unicode code point) is a lowercase character. */
    public static final class IsLowerCase extends s2B {
        protected boolean f(int x) {
            return Character.isLowerCase(x);
        }
    }

    /* Determines if the given Char value is a low-surrogate code unit (also known as trailing-surrogate code unit). */
    public static final class IsLowSurrogate extends s2B {
        protected boolean f(int x) {
            return (x <= 0xFFFF) && Character.isLowSurrogate((char) x);
        }
    }

    /* Determines whether the specified character (Unicode code point) is mirrored according to the Unicode specification. */
    public static final class IsMirrored extends s2B {
        protected boolean f(int x) {
            return Character.isMirrored(x);
        }
    }

    /* Determines if the specified character (Unicode code point) is a Unicode space character. */
    public static final class IsSpaceChar extends s2B {
        protected boolean f(int x) {
            return Character.isSpaceChar(x);
        }
    }

    /* Determines whether the specified character (Unicode code point) is in the supplementary character range. */
    public static final class IsSupplementaryCodePoint extends s2B {
        protected boolean f(int x) {
            return Character.isSupplementaryCodePoint(x);
        }
    }

    /* Determines whether the specified pair of Char values is a valid surrogate pair. */
    public static final class IsSurrogatePair extends sC2B {
        protected boolean f(int x, int y) {
            return (x <= 0xFFFF) && (y <= 0xFFFF) && Character.isSurrogatePair((char) x, (char) y);
        }
    }

    /* Determines if the specified character (Unicode code point) is a titlecase character. */
    public static final class IsTitleCase extends s2B {
        protected boolean f(int x) {
            return Character.isTitleCase(x);
        }
    }

    /* Determines if the specified character (Unicode code point) may be part of a Unicode identifier as other than the first character. */
    public static final class IsUnicodeIdentifierPart extends s2B {
        protected boolean f(int x) {
            return Character.isUnicodeIdentifierPart(x);
        }
    }

    /* Determines if the specified character (Unicode code point) is permissible as the first character in a Unicode identifier. */
    public static final class IsUnicodeIdentifierStart extends s2B {
        protected boolean f(int x) {
            return Character.isUnicodeIdentifierStart(x);
        }
    }

    /* Determines if the specified character (Unicode code point) is an uppercase character. */
    public static final class IsUpperCase extends s2B {
        protected boolean f(int x) {
            return Character.isUpperCase(x);
        }
    }

    /* Determines whether the specified code point is a valid Unicode code point value in the range of 0x0000 to 0x10FFFF inclusive. */
    public static final class IsValidCodePoint extends s2B {
        protected boolean f(int x) {
            return Character.isValidCodePoint(x);
        }
    }

    /* Determines if the specified character (Unicode code point) is white space according to Java. */
    public static final class IsWhitespace extends s2B {
        protected boolean f(int x) {
            return Character.isWhitespace(x);
        }
    }

    /* Return the numeric value of the character (Unicode code point) in the specified radix */
    public static final class JavaDigit extends sI2I {
        protected int f(int x, int radix) {
            return Character.digit(x, radix);
        }
    }

    /* Converts the character (Unicode code point) argument to lowercase using case mapping information from the UnicodeData file. */
    public static final class ToLowerCase extends s2C {
        protected int f(int x) {
            return Character.toLowerCase(x);
        }
    }

    /* Converts the character (Unicode code point) argument to titlecase using case mapping information from the UnicodeData file. */
    public static final class ToTitleCase extends s2C {
        protected int f(int x) {
            return Character.toTitleCase(x);
        }
    }

    /* Converts the character (Unicode code point) argument to uppercase using case mapping information from the UnicodeData file. */
    public static final class ToUpperCase extends s2C {
        protected int f(int x) {
            return Character.toUpperCase(x);
        }
    }
}
