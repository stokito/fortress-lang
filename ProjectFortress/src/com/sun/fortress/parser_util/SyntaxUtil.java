/*******************************************************************************
    Copyright 2008 Sun Microsystems, Inc.,
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

package com.sun.fortress.parser_util;

import com.sun.fortress.parser.Fortress;

/*
 * Re-provides functionality and definitions from the parser.
 */
public class SyntaxUtil {

    /*
     * Todo: Add a check for operators
     */
    public static boolean notIdOrOpOrKeyword(char c) {
        return !(IdentifierUtil.validId(""+c) || 
                 isKeyword(""+c) ||
                 isSpecialSyntaxChar(c));
    }

    public static boolean isKeyword(String word) {
        return Fortress.FORTRESS_KEYWORDS.contains(word);
    }

    public static boolean isSpecialSyntaxChar(char c) {
        return Fortress.FORTRESS_SYNTAX_SPECIAL_CHARS.contains(c);
    }

    public static Iterable<String> specialSymbols() {
        return Fortress.FORTRESS_SYNTAX_SPECIAL_SYMBOLS;
    }

    public static Iterable<String> specialChars() {
        return Fortress.FORTRESS_SYNTAX_SPECIAL_CHARS;
    }
}
