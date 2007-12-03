package com.sun.fortress.syntaxabstractions.rats.util;

public class FreshName {

	private static int freshid = 0;
	
	public static String getFreshName(String s) {
		return s+(++freshid);
	}
}
