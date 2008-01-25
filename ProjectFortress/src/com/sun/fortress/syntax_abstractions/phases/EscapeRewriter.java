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

import java.util.LinkedList;
import java.util.List;

import com.sun.fortress.nodes.CharSymbol;
import com.sun.fortress.nodes.CharacterClassSymbol;
import com.sun.fortress.nodes.CharacterInterval;
import com.sun.fortress.nodes.CharacterSymbol;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.KeywordSymbol;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeDepthFirstVisitor;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.PrefixedSymbol;
import com.sun.fortress.nodes.SyntaxSymbol;
import com.sun.fortress.nodes.TokenSymbol;
import com.sun.fortress.parser.Fortress;

import edu.rice.cs.plt.tuple.Option;

public class EscapeRewriter extends NodeUpdateVisitor {

	private static final String ESCAPECHAR = "`";

	private String removeLeadingEscape(String s, List<CharacterSymbol> ls) {
		if (s.startsWith(ESCAPECHAR)) {
			int inx = 1;
			while(inx<s.length()-1) {
				ls.add(new CharSymbol(""+s.charAt(inx)));
				inx++;							
			}
			s = ""+s.charAt(s.length()-1);
		}
		return s;
	}
	
	private String removeTrailingEscape(String s, List<CharacterSymbol> ls) {
		String end = "";
		if (s.startsWith(ESCAPECHAR)) {
			int inx = 1;
			end = ""+s.charAt(inx);
			inx++;
			while(inx<s.length()) {
				ls.add(new CharSymbol(""+s.charAt(inx)));
				inx++;							
			}
			return end;
		}
		return s;
	}
	
	@Override
	public Node forCharacterClassSymbol(CharacterClassSymbol that) {
		List<CharacterSymbol> ls = new LinkedList<CharacterSymbol>(); 
		for (CharacterSymbol cs: that.getCharacters()) {
			List<CharacterSymbol> ncs = cs.accept(new NodeDepthFirstVisitor<List<CharacterSymbol>>() {

				@Override
				public List<CharacterSymbol> forCharacterInterval(CharacterInterval that) {
					List<CharacterSymbol> head = new LinkedList<CharacterSymbol>();
					String begin = removeLeadingEscape(that.getBegin(), head);
					List<CharacterSymbol> tail = new LinkedList<CharacterSymbol>();
					String end = removeTrailingEscape(that.getEnd(), tail);
					head.add(new CharacterInterval(begin, end));
					head.addAll(tail);
					return head;
				}

				@Override
				public List<CharacterSymbol> forCharSymbol(CharSymbol that) {
					List<CharacterSymbol> head = new LinkedList<CharacterSymbol>();
					String s = removeLeadingEscape(that.getString(), head);
					head.add(new CharSymbol(s));
					return head;
				}
				
			});
			ls.addAll(ncs);
		}
		return new CharacterClassSymbol(that.getSpan(), ls);
	}

	@Override
	public Node forKeywordSymbol(KeywordSymbol that) {
		String s = removeEscape(that.getToken());
		return new KeywordSymbol(that.getSpan(), s);
	}

	@Override
	public Node forTokenSymbol(TokenSymbol that) {
		String s = removeEscape(that.getToken());
		return new TokenSymbol(that.getSpan(), s);
	}
	
	@Override
	public Node forPrefixedSymbolOnly(PrefixedSymbol that,
									  Option<Id> result_id,
									  SyntaxSymbol result_symbol) {
		String s = removeEscape(Option.unwrap(result_id).getText());
		return new PrefixedSymbol(that.getSpan(), Option.some(new Id(s)), result_symbol);
	}
	
	private String removeEscape(String s) {
		for (String symbol: Fortress.FORTRESS_SYNTAX_SPECIAL_SYMBOLS) { 
			s = s.replaceAll(ESCAPECHAR+symbol, symbol);
		}
		for (String symbol: Fortress.FORTRESS_SYNTAX_SPECIAL_CHARS) { 
			s = s.replaceAll(ESCAPECHAR+symbol, symbol);
		}

		return s;
	}

	
}
