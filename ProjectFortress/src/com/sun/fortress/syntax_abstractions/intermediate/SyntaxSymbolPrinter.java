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

package com.sun.fortress.syntax_abstractions.intermediate;

import java.util.List;

import com.sun.fortress.nodes.AndPredicateSymbol;
import com.sun.fortress.nodes.BreaklineSymbol;
import com.sun.fortress.nodes.CarriageReturnSymbol;
import com.sun.fortress.nodes.CharSymbol;
import com.sun.fortress.nodes.CharacterClassSymbol;
import com.sun.fortress.nodes.CharacterInterval;
import com.sun.fortress.nodes.CharacterSymbol;
import com.sun.fortress.nodes.FormfeedSymbol;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.ItemSymbol;
import com.sun.fortress.nodes.KeywordSymbol;
import com.sun.fortress.nodes.NewlineSymbol;
import com.sun.fortress.nodes.NoWhitespaceSymbol;
import com.sun.fortress.nodes.NodeDepthFirstVisitor;
import com.sun.fortress.nodes.NonterminalSymbol;
import com.sun.fortress.nodes.NotPredicateSymbol;
import com.sun.fortress.nodes.OptionalSymbol;
import com.sun.fortress.nodes.PrefixedSymbol;
import com.sun.fortress.nodes.RepeatOneOrMoreSymbol;
import com.sun.fortress.nodes.RepeatSymbol;
import com.sun.fortress.nodes.TabSymbol;
import com.sun.fortress.nodes.TokenSymbol;
import com.sun.fortress.nodes.WhitespaceSymbol;

import edu.rice.cs.plt.tuple.Option;

public class SyntaxSymbolPrinter extends NodeDepthFirstVisitor<String> {

	@Override
	public String forBreaklineSymbol(BreaklineSymbol that) {
		return "BreaklineSymbol()";
	}

	@Override
	public String forCarriageReturnSymbol(CarriageReturnSymbol that) {
		return "CarriageReturnSymbol()";
	}


	@Override
	public String forCharacterClassSymbolOnly(CharacterClassSymbol that,
			List<String> characters_result) {
		return "CharacterClassSymbol("+characters_result+")";
	}

	@Override
	public String forCharacterInterval(CharacterInterval that) {
		return "CharacterInterval("+that.getBegin()+":"+that.getEnd()+")";
	}

	@Override
	public String forCharSymbol(CharSymbol that) {
		return "CharSymbol("+that.getString()+")";
	}

	@Override
	public String forFormfeedSymbol(FormfeedSymbol that) {
		return "FormfeedSymbol()";
	}

	@Override
	public String forId(Id that) {
		return that.getText();
	}

	@Override
	public String forItemSymbolOnly(ItemSymbol that) {
		return "ItemSymbol("+that.getItem()+")";
	}

	@Override
	public String forKeywordSymbol(KeywordSymbol that) {
		return "KeywordSymbol("+that.getToken()+")";
	}

	@Override
	public String forNewlineSymbol(NewlineSymbol that) {
		return "NewlineSymbol()";
	}

	@Override
	public String forNonterminalSymbol(NonterminalSymbol that) {
		return "NonterminalSymbol("+that.getNonterminal()+")";
	}
	
	@Override
	public String forAndPredicateSymbolOnly(AndPredicateSymbol that,
			String symbol_result) {
		return "AndPredicateSymbol("+symbol_result+")";
	}

	@Override
	public String forNotPredicateSymbolOnly(NotPredicateSymbol that,
			String symbol_result) {
		return "NotPredicateSymbol("+symbol_result+")";
	}

	@Override
	public String forNoWhitespaceSymbol(NoWhitespaceSymbol that) {
		return "NoWhitespaceSymbol()";
	}

	@Override
	public String forOptionalSymbolOnly(OptionalSymbol that,
			String symbol_result) {
		return "OptionalSymbol("+symbol_result+")";
	}

	
	@Override
	public String forPrefixedSymbolOnly(PrefixedSymbol that,
			Option<String> id_result, String symbol_result) {
		if (id_result.isSome()) {
			return "PrefixedSymbol("+Option.unwrap(id_result)+":"+symbol_result+")";
		}
		return "PrefixedSymbol("+symbol_result+")"; 
	}

	@Override
	public String forRepeatOneOrMoreSymbolOnly(RepeatOneOrMoreSymbol that,
			String symbol_result) {
		return "RepeatOneOrMoreSymbol("+symbol_result+")";
	}

	@Override
	public String forRepeatSymbolOnly(RepeatSymbol that, String symbol_result) {
		return "RepeatSymbol("+symbol_result+")";
	}

	@Override
	public String forTabSymbol(TabSymbol that) {
		return "TabSymbol()";
	}

	@Override
	public String forTokenSymbol(TokenSymbol that) {
		return "TokenSymbol("+that.getToken()+")";
	}
	
	@Override
	public String forWhitespaceSymbol(WhitespaceSymbol that) {
		return "WhitespaceSymbol()";
	}

}
