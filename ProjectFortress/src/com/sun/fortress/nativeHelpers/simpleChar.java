/*******************************************************************************
 Copyright 2009,2010, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.nativeHelpers;

public class simpleChar {

    public static String charToString(int x) {
	// This implementation is incorrect for supplementary characters!
        return Character.toString((char)x);
    }

    public static String charToExprString(int x) {
        if (x <= 0xFFFF) {
	    if (Character.isValidCodePoint(x)) {
		if (0x20 <= x && x < 0x7F) return "'" + ((char) x) + "'";
		if (Character.isISOControl(x)) {
		    switch ((char) x) {
		    case '\b': return "'\\b'";
		    case '\t': return "'\\t'";
		    case '\n': return "'\\n'";
		    case '\f': return "'\\f'";
		    case '\r': return "'\\r'";
		    case '\"': return "'\\\"'";
		    case '\\': return "'\\\\'";
		    default: return "'" + charToHexString(x) + "'";
		    }
		}
		return "'" + ((char) x) + " " + charToHexString(x) + "'";
	    }
	    else return "'" + charToHexString(x) + "'";
	    
	} else {
	    return "'" + charToHexString(x) + "'";
	}
    }

    private static String charToHexString(int x) {
        if (x <= 0xF) return "000" + Integer.toHexString(x);
        else if (x <= 0xFF) return "00" + Integer.toHexString(x);
        else if (x <= 0xFFF) return "0" + Integer.toHexString(x);
        else return Integer.toHexString(x);
    }

    // There is deep magic associated with making the compiler
    // believe that the argument type of this method is really
    // the Fortress Character type.  See the method
    // NamingCzar.fortressTypeForForeignJavaType  for details.
    public static int charCodePointWithSpecialCompilerHackForCharacterArgumentType(int x) { return x; }

    // There is deep magic associated with making the compiler
    // believe that the return type of this method is really
    // the Fortress Character type.  See the method
    // NamingCzar.fortressTypeForForeignJavaType  for details.
    public static int charMakeCharacterWithSpecialCompilerHackForCharacterResultType(int x) { return x; }
    
    public static boolean charLT(int a, int b) {
        return a < b;
    }

    public static boolean charLE(int a, int b) {
        return a <= b;
    }

    public static boolean charGT(int a, int b) {
        return a > b;
    }

    public static boolean charGE(int a, int b) {
        return a >= b;
    }

    public static boolean charEQ(int a, int b) {
        return a == b;
    }

    public static boolean charNE(int a, int b) {
        return a != b;
    }

    public static boolean charLessNotSim(int x, int y) {
        return (Character.toLowerCase(x) < Character.toLowerCase(y));
    }

    public static boolean charLessSim(int x, int y) {
        return (Character.toLowerCase(x) <= Character.toLowerCase(y));
    }

    public static boolean charGreaterNotSim(int x, int y) {
        return (Character.toLowerCase(x) > Character.toLowerCase(y));
    }

    public static boolean charGreaterSim(int x, int y) {
        return (Character.toLowerCase(x) >= Character.toLowerCase(y));
    }

    public static boolean charSimEq(int x, int y) {
        return (Character.toLowerCase(x) == Character.toLowerCase(y));
    }

    public static boolean charNotSimEq(int x, int y) {
        return (Character.toLowerCase(x) != Character.toLowerCase(y));
    }

    /* Returns the Unicode directionality property for the given character (Unicode code point). */
    public static int charGetDirectionality(int x) {
	return Character.getDirectionality(x);
    }

    /* Returns the integer value that the specified character (Unicode code point) represents. */
    public static int charGetNumericValue(int x) {
	return Character.getNumericValue(x);
    }

    /* Returns a value indicating a character's general category. */
    public static int charGetType(int x) {
	return Character.getType(x);
    }

    /* Determines if a character (Unicode code point) is defined in Unicode. */
    public static boolean charIsDefined(int x) {
	return Character.isDefined(x);
    }

    /* Determines if the specified character (Unicode code point) is a digit. */
    public static boolean charIsDigit(int x) {
	return Character.isDigit(x);
    }

    /* Determines if the character (Unicode code point) may be part of a Fortress identifier as other than the first character. */
    public static boolean charIsFortressIdentifierPart(int x) {
	return Character.isLetterOrDigit(x) || (x == (char) '_') || (x == (char) '\'');
    }

    /* Determines if the character (Unicode code point) is permissible as the first character in a Fortress identifier. */
    public static boolean charIsFortressIdentifierStart(int x) {
	return Character.isLetter(x) || (x == (char) '_');
    }

    /* Determines if the given Char value is a high-surrogate code unit (also known as leading-surrogate code unit). */
    public static boolean charIsHighSurrogate(int x) {
	return (x <= 0xFFFF) && Character.isHighSurrogate((char) x);
    }

    /* Determines if the specified character (Unicode code point) should be regarded as an ignorable character in a Java identifier or a Unicode identifier. */
    public static boolean charIsIdentifierIgnorable(int x) {
	return Character.isIdentifierIgnorable(x);
    }

    /* Determines if the referenced character (Unicode code point) is an ISO control character. */
    public static boolean charIsISOControl(int x) {
	return Character.isISOControl(x);
    }

    /* Determines if the character (Unicode code point) may be part of a Java identifier as other than the first character. */
    public static boolean charIsJavaIdentifierPart(int x) {
	return Character.isJavaIdentifierPart(x);
    }

    /* Determines if the character (Unicode code point) is permissible as the first character in a Java identifier. */
    public static boolean charIsJavaIdentifierStart(int x) {
	return Character.isJavaIdentifierStart(x);
    }

    /* Determines if the specified character (Unicode code point) is a letter. */
    public static boolean charIsLetter(int x) {
	return Character.isLetter(x);
    }

    /* Determines if the specified character (Unicode code point) is a letter or digit. */
    public static boolean charIsLetterOrDigit(int x) {
	return Character.isLetterOrDigit(x);
    }

    /* Determines if the specified character (Unicode code point) is a lowercase character. */
    public static boolean charIsLowerCase(int x) {
	return Character.isLowerCase(x);
    }

    /* Determines if the given Char value is a low-surrogate code unit (also known as trailing-surrogate code unit). */
    public static boolean charIsLowSurrogate(int x) {
	return (x <= 0xFFFF) && Character.isLowSurrogate((char) x);
    }

    /* Determines whether the specified character (Unicode code point) is mirrored according to the Unicode specification. */
    public static boolean charIsMirrored(int x) {
	return Character.isMirrored(x);
    }

    /* Determines if the specified character (Unicode code point) is a Unicode space character. */
    public static boolean charIsSpaceChar(int x) {
	return Character.isSpaceChar(x);
    }

    /* Determines whether the specified character (Unicode code point) is in the supplementary character range. */
    public static boolean charIsSupplementaryCodePoint(int x) {
	return Character.isSupplementaryCodePoint(x);
    }

    /* Determines whether the specified pair of Char values is a valid surrogate pair. */
    public static boolean charIsSurrogatePair(int x, int y) {
	return (x <= 0xFFFF) && (y <= 0xFFFF) && Character.isSurrogatePair((char) x, (char) y);
    }

    /* Determines if the specified character (Unicode code point) is a titlecase character. */
    public static boolean charIsTitleCase(int x) {
	return Character.isTitleCase(x);
    }

    /* Determines if the specified character (Unicode code point) may be part of a Unicode identifier as other than the first character. */
    public static boolean charIsUnicodeIdentifierPart(int x) {
	return Character.isUnicodeIdentifierPart(x);
    }

    /* Determines if the specified character (Unicode code point) is permissible as the first character in a Unicode identifier. */
    public static boolean charIsUnicodeIdentifierStart(int x) {
	return Character.isUnicodeIdentifierStart(x);
    }

    /* Determines if the specified character (Unicode code point) is an uppercase character. */
    public static boolean charIsUpperCase(int x) {
	return Character.isUpperCase(x);
    }

    /* Determines whether the specified code point is a valid Unicode code point value in the range of 0x0000 to 0x10FFFF inclusive. */
    public static boolean charIsValidCodePoint(int x) {
	return Character.isValidCodePoint(x);
    }

    /* Determines if the specified character (Unicode code point) is white space according to Java. */
    public static boolean charIsWhitespace(int x) {
	return Character.isWhitespace(x);
    }

    /* Return the numeric value of the character (Unicode code point) in the specified radix */
    public static int charJavaDigit(int x, int radix) {
	return Character.digit(x, radix);
    }

    /* Converts the character (Unicode code point) argument to lowercase using case mapping information from the UnicodeData file. */
    public static int charToLowerCase(int x) {
	return Character.toLowerCase(x);
    }

    /* Converts the character (Unicode code point) argument to titlecase using case mapping information from the UnicodeData file. */
    public static int charToTitleCase(int x) {
	return Character.toTitleCase(x);
    }

    /* Converts the character (Unicode code point) argument to uppercase using case mapping information from the UnicodeData file. */
    public static int charToUpperCase(int x) {
	return Character.toUpperCase(x);
    }

}
