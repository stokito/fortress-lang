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

package com.sun.fortress.syntax_abstractions.rats.util;

import java.io.BufferedReader;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import xtc.parser.ParserBase;
import xtc.parser.Result;

public class ParserMediator {

	private static ParserBase parser;

	public ParserMediator() {
	}

	/**
	 * Instantiate a new instance of the given parserClass which must be a subtype of xtc.parser.ParserBase.
	 * The instantiated parser object is stored in a field and returned; 
	 * If anything goes wrong an exception is thrown. 
	 * @param parserClass
	 * @param reader
	 * @param filename
	 * @return The new instantiated parser object
	 * @throws IllegalArgumentException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 */
	public static ParserBase getParser(Class<?> parserClass, BufferedReader reader, String filename)
		throws IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException, SecurityException, NoSuchMethodException {

		Constructor<?> constructor = parserClass.getConstructor(Reader.class, String.class);
		parser = (ParserBase) constructor.newInstance(reader, filename);
		return parser;
	}

	/**
	 * Call the method pFile(0) on the current parser object created using instantiate. 
	 * @return The xtc.parser.Result object returned by the call to pFile(0).
	 * @throws IllegalArgumentException
	 * @throws SecurityException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 */
	public static Result parse() throws IllegalArgumentException, SecurityException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		String methodName = "pFile";
		Class[] types = new Class[] {int.class};
		Object args = 0;
		return (Result) invokeMethod(methodName, types, args);
	}

	public static String getLocation(int index) throws IllegalArgumentException, SecurityException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		String methodName = "location";
		Class[] types = new Class[] {int.class};
		Object args = index;
		return invokeMethod(methodName, types, args).toString();
	}

	/**
	 * Looks up the given method in the current parser object using reflection using the supplied argument types.
	 * The method is invoked with the supplied arguments.
	 * If no method exists with the given name and argument types, an exception is thrown.
	 * If the arguments are not consistent with the declared types then an exception is thrown. 
	 * @param methodName
	 * @param types
	 * @param args
	 * @throws InvocationTargetException 
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 * @throws NoSuchMethodException 
	 * @throws SecurityException 
	 */
	private static Object invokeMethod(String methodName, Class[] types, Object args) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, SecurityException, NoSuchMethodException {
		Method pFile = parser.getClass().getMethod(methodName, types);
		Object o = pFile.invoke(parser, args);
		return o;
	}


}
