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

package com.sun.fortress.exceptions;

import xtc.parser.ParseError;
import xtc.parser.ParserBase;


public class ParserError extends StaticError {
	private final ParseError _parseError;
	private final String _location;

	public ParserError(ParseError parseError, ParserBase parser) {
		_parseError = parseError;
		if (_parseError.index == -1) {
		    _location = "Unspecified location";
		} else {
		    _location = parser.location(_parseError.index).toString();
		}
	}

	public String typeDescription() { return "Parse Error"; }

	@Override
	public String description() {
		String result = _parseError.msg;
		int size = result.length();
		if (size > 8 && result.substring(size-8,size).equals("expected"))
			result = "Syntax Error";
		else result = "Syntax Error: " + result;
		// TODO: I don't know for sure whether this is allowed to be null
		if (result == null || result.equals("")) { result = "Unspecified cause"; }
		return result;
	}

	@Override
	public String at() {
		return _location;
	}
}
