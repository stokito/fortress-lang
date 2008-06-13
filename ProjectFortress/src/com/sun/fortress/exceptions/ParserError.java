package com.sun.fortress.exceptions;

import xtc.parser.ParseError;
import xtc.parser.ParserBase;


public class ParserError extends StaticError {
	private final ParseError _parseError;
	private final ParserBase _parser;

	public ParserError(ParseError parseError, ParserBase parser) {
		_parseError = parseError;
		_parser = parser;
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
		if (_parseError.index == -1) { return "Unspecified location"; }
		else { return _parser.location(_parseError.index).toString(); }
	}
}

