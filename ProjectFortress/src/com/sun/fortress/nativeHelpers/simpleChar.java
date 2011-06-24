/*******************************************************************************
 Copyright 2009,2010, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.nativeHelpers;

public class simpleChar {

    public static String charToString(char x) {
        return Character.toString(x);
    }

    public static boolean charLT(char a, char b) {
        return a < b;
    }

    public static boolean charLE(char a, char b) {
        return a <= b;
    }

    public static boolean charGT(char a, char b) {
        return a > b;
    }

    public static boolean charGE(char a, char b) {
        return a >= b;
    }

    public static boolean charEQ(char a, char b) {
        return a == b;
    }

    public static boolean charNE(char a, char b) {
        return a != b;
    }

    public static boolean charLessNotSim(char x, char y) {
        return (Character.toLowerCase(x) < Character.toLowerCase(y));
    }

    public static boolean charLessSim(char x, char y) {
        return (Character.toLowerCase(x) <= Character.toLowerCase(y));
    }

    public static boolean charGreaterNotSim(char x, char y) {
        return (Character.toLowerCase(x) > Character.toLowerCase(y));
    }

    public static boolean charGreaterSim(char x, char y) {
        return (Character.toLowerCase(x) >= Character.toLowerCase(y));
    }

    public static boolean charSimEq(char x, char y) {
        return (Character.toLowerCase(x) == Character.toLowerCase(y));
    }

    public static boolean charNotSimEq(char x, char y) {
        return (Character.toLowerCase(x) != Character.toLowerCase(y));
    }

    /* Returns the Unicode directionality property for the given character (Unicode code point). */
    public static int charGetDirectionality(char x) {
	return Character.getDirectionality(x);
    }

    /* Returns the integer value that the specified character (Unicode code point) represents. */
    public static int charGetNumericValue(char x) {
	return Character.getNumericValue(x);
    }

    /* Returns a value indicating a character's general category. */
    public static int charGetType(char x) {
	return Character.getType(x);
    }

    /* Determines if a character (Unicode code point) is defined in Unicode. */
    public static boolean charIsDefined(char x) {
	return Character.isDefined(x);
    }

    /* Determines if the specified character (Unicode code point) is a digit. */
    public static boolean charIsDigit(char x) {
	return Character.isDigit(x);
    }

    /* Determines if the character (Unicode code point) may be part of a Fortress identifier as other than the first character. */
    public static boolean charIsFortressIdentifierPart(char x) {
	return Character.isLetterOrDigit(x) || (x == (char) '_') || (x == (char) '\'');
    }

    /* Determines if the character (Unicode code point) is permissible as the first character in a Fortress identifier. */
    public static boolean charIsFortressIdentifierStart(char x) {
	return Character.isLetter(x) || (x == (char) '_');
    }

    /* Determines if the given Char value is a high-surrogate code unit (also known as leading-surrogate code unit). */
    public static boolean charIsHighSurrogate(char x) {
	return (x <= 0xFFFF) && Character.isHighSurrogate((char) x);
    }

    /* Determines if the specified character (Unicode code point) should be regarded as an ignorable character in a Java identifier or a Unicode identifier. */
    public static boolean charIsIdentifierIgnorable(char x) {
	return Character.isIdentifierIgnorable(x);
    }

    /* Determines if the referenced character (Unicode code point) is an ISO control character. */
    public static boolean charIsISOControl(char x) {
	return Character.isISOControl(x);
    }

    /* Determines if the character (Unicode code point) may be part of a Java identifier as other than the first character. */
    public static boolean charIsJavaIdentifierPart(char x) {
	return Character.isJavaIdentifierPart(x);
    }

    /* Determines if the character (Unicode code point) is permissible as the first character in a Java identifier. */
    public static boolean charIsJavaIdentifierStart(char x) {
	return Character.isJavaIdentifierStart(x);
    }

    /* Determines if the specified character (Unicode code point) is a letter. */
    public static boolean charIsLetter(char x) {
	return Character.isLetter(x);
    }

    /* Determines if the specified character (Unicode code point) is a letter or digit. */
    public static boolean charIsLetterOrDigit(char x) {
	return Character.isLetterOrDigit(x);
    }

    /* Determines if the specified character (Unicode code point) is a lowercase character. */
    public static boolean charIsLowerCase(char x) {
	return Character.isLowerCase(x);
    }

    /* Determines if the given Char value is a low-surrogate code unit (also known as trailing-surrogate code unit). */
    public static boolean charIsLowSurrogate(char x) {
	return (x <= 0xFFFF) && Character.isLowSurrogate((char) x);
    }

    /* Determines whether the specified character (Unicode code point) is mirrored according to the Unicode specification. */
    public static boolean charIsMirrored(char x) {
	return Character.isMirrored(x);
    }

    /* Determines if the specified character (Unicode code point) is a Unicode space character. */
    public static boolean charIsSpaceChar(char x) {
	return Character.isSpaceChar(x);
    }

    /* Determines whether the specified character (Unicode code point) is in the supplementary character range. */
    public static boolean charIsSupplementaryCodePoint(char x) {
	return Character.isSupplementaryCodePoint(x);
    }

    /* Determines whether the specified pair of Char values is a valid surrogate pair. */
    public static boolean charIsSurrogatePair(char x, char y) {
	return (x <= 0xFFFF) && (y <= 0xFFFF) && Character.isSurrogatePair((char) x, (char) y);
    }

    /* Determines if the specified character (Unicode code point) is a titlecase character. */
    public static boolean charIsTitleCase(char x) {
	return Character.isTitleCase(x);
    }

    /* Determines if the specified character (Unicode code point) may be part of a Unicode identifier as other than the first character. */
    public static boolean charIsUnicodeIdentifierPart(char x) {
	return Character.isUnicodeIdentifierPart(x);
    }

    /* Determines if the specified character (Unicode code point) is permissible as the first character in a Unicode identifier. */
    public static boolean charIsUnicodeIdentifierStart(char x) {
	return Character.isUnicodeIdentifierStart(x);
    }

    /* Determines if the specified character (Unicode code point) is an uppercase character. */
    public static boolean charIsUpperCase(char x) {
	return Character.isUpperCase(x);
    }

    /* Determines whether the specified code point is a valid Unicode code point value in the range of 0x0000 to 0x10FFFF inclusive. */
    public static boolean charIsValidCodePoint(char x) {
	return Character.isValidCodePoint(x);
    }

    /* Determines if the specified character (Unicode code point) is white space according to Java. */
    public static boolean charIsWhitespace(char x) {
	return Character.isWhitespace(x);
    }

    /* Return the numeric value of the character (Unicode code point) in the specified radix */
    public static int charJavaDigit(char x, int radix) {
	return Character.digit(x, radix);
    }

    /* Converts the character (Unicode code point) argument to lowercase using case mapping information from the UnicodeData file. */
    public static char charToLowerCase(char x) {
	return Character.toLowerCase(x);
    }

    /* Converts the character (Unicode code point) argument to titlecase using case mapping information from the UnicodeData file. */
    public static char charToTitleCase(char x) {
	return Character.toTitleCase(x);
    }

    /* Converts the character (Unicode code point) argument to uppercase using case mapping information from the UnicodeData file. */
    public static char charToUpperCase(char x) {
	return Character.toUpperCase(x);
    }

}
