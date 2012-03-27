/*******************************************************************************
    Copyright 2008,2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.exceptions;

import xtc.parser.ParseError;
import xtc.parser.ParserBase;


public class ParserError extends StaticError {

    /**
     * Make Eclipse happy
     */
    private static final long serialVersionUID = 2055048497086874145L;

    private final ParseError _parseError;
    private final String _location;

    public ParserError(ParseError parseError, ParserBase parser) {
        super(parseError.msg);
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
        else {
            if ( ! result.equals("") )
                result = "Syntax Error: " + result;
            else
                result = "Syntax Error";
        }
        // TODO: I don't know for sure whether this is allowed to be null
        if (result == null || result.equals("")) { result = "Unspecified cause"; }
        return result;
    }

    @Override
	public String at() {
        return _location;
    }
}
