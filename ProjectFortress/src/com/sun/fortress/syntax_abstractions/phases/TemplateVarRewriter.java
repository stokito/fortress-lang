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

package com.sun.fortress.syntax_abstractions.phases;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.sun.fortress.syntax_abstractions.rats.util.FreshName;



import com.sun.fortress.useful.Pair;

/*
 * Rewrite all occurrences of variables in a given template to 
 * occurrences of gaps 
 */
public class TemplateVarRewriter {

	public static final String GAPSYNTAXPREFIX = "<!@#$%^&*< ";
	public static final String GAPSYNTAXSUFFIX = " >*&^%$#@!>";

	public String rewriteVars(Collection<String> vars, String t) {
		Map<String, String> varToGapName = new HashMap<String, String>();
		String result = "";

		L: for (int inx=0; inx<t.length(); inx++) {
			for (String var: vars) {
				// System.err.println("Testing: "+var);
				int end = inx+var.length();

				if (match(inx, end, var, t)) {
					// System.err.println("Match...");
					if (isVar(inx, end, t)) {
						// System.err.println("isVar...");
						inx = end-1;
						String tmp = "";
						if (isTemplateApplication(end, t)) {
							Pair<Integer,String> p = parseTemplateApplication(varToGapName, end, t, var);
							inx = p.getA();
							tmp = p.getB();
						}
						result += getVar(var+tmp);						
						continue L;
					}
				}
			}
			result += t.charAt(inx);
		}	
		return result;
	}

	private boolean isVar(int inx, int end, String t) {
		if (startOfString(inx)) {
			return isTemplateApplication(end, t) || toEndOfString(end, t) || t.charAt(end) == ' ';
		} else if (endOfString(end, t)) {
			return t.charAt(inx-1) == ' ';
		} else {
			return (t.charAt(inx-1) == ' ' && isTemplateApplication(end, t)) || (t.charAt(inx-1) == ' ' && t.charAt(end) == ' ');
		}
	}

	private boolean endOfString(int end, String t) {
		return end == t.length();
	}

	private boolean toEndOfString(int end, String t) {
		return endOfString(end, t) || isTemplateApplication(end, t);
	}

	private boolean match(int s, int e, String var, String t) {
		if (e-1<t.length()) {
			return t.substring(s, e).equals(var);
		}
		return false;
	}

	private Pair<Integer,String> parseTemplateApplication(Map<String, String> varToGapName, int end, String t, String v) {
		String result = "";
		if (isTemplateApplication(end, t)) {
			int jnx = getEndOfTemplateApplication(end,t);
			List<String> params = parseArgs(varToGapName, t.substring(end+1, jnx));
			if (!params.isEmpty()) {
				result = "(";
				Iterator<String> it = params.iterator();
				while (it.hasNext()) {
					result += it.next();
					if (it.hasNext()) {
						result += ", ";
					}
				}
				result += ")";
			}			
			return new Pair<Integer,String>(jnx, result);
		}
		// return an error code
		return new Pair<Integer,String>(-1, result);
	}

	private String getVar(String v) {
		return TemplateVarRewriter.getGapString(v);
	}

	private int getEndOfTemplateApplication(int end, String t) {
		return t.indexOf(')', end);
	}
	
	private boolean isTemplateApplication(int end, String t) {
		int jnx = getEndOfTemplateApplication(end, t);		
		if (jnx > -1) {
			return t.length() > end && t.charAt(end) == '(';	
		}
		return false;
	}

	private boolean startOfString(int inx) {
		return inx == 0;
	}

//	private String getGapName(Map<String, String> varToGapName, String var) {
//		if (varToGapName.containsKey(var)) {
//			return varToGapName.get(var);
//		}
//		String gapName = FreshName.getFreshName(var);
//		varToGapName.put(var, gapName);
//		return gapName;
//	}

	private List<String> parseArgs(Map<String, String> varToGapName , String s) {
		// System.err.println("S: "+s);
		String[] tokens = s.split(",");
		List<String> ls = new LinkedList<String>();
		for (String token: tokens) {
			// System.err.println("T: "+token);
			// ls.add(getGapName(varToGapName, token.trim()));
			ls.add(token.trim());
		}
		return ls;
	}

	public static String getGapString(String var) {
		return TemplateVarRewriter.GAPSYNTAXPREFIX+var+TemplateVarRewriter.GAPSYNTAXSUFFIX;
	}

}
