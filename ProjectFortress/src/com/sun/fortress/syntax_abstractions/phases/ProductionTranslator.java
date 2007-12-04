/*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
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
import java.util.LinkedList;
import java.util.List;

import xtc.parser.AlternativeAddition;
import xtc.parser.Element;
import xtc.parser.FullProduction;
import xtc.parser.NonTerminal;
import xtc.parser.OrderedChoice;
import xtc.parser.Production;
import xtc.parser.Sequence;
import xtc.parser.SequenceName;
import xtc.parser.Terminal;
import xtc.tree.Attribute;
import xtc.tree.Node;
import xtc.type.Type;

import com.sun.fortress.compiler.StaticError;
import com.sun.fortress.compiler.StaticPhaseResult;
import com.sun.fortress.nodes.NodeDepthFirstVisitor;
import com.sun.fortress.nodes.NonterminalSymbol;
import com.sun.fortress.nodes.OptionalSymbol;
import com.sun.fortress.nodes.ProductionDef;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.nodes.RepeatOneOrMoreSymbol;
import com.sun.fortress.nodes.RepeatSymbol;
import com.sun.fortress.nodes.SyntaxDef;
import com.sun.fortress.nodes.SyntaxSymbol;
import com.sun.fortress.nodes.TokenSymbol;
import com.sun.fortress.nodes.WhitespaceSymbol;
import com.sun.fortress.syntax_abstractions.rats.util.FreshName;
import com.sun.fortress.syntax_abstractions.rats.util.ModuleInfo;
import com.sun.fortress.syntax_abstractions.rats.util.ProductionEnum;
import com.sun.fortress.syntax_abstractions.util.ActionCreater;

import edu.rice.cs.plt.tuple.Option;

public class ProductionTranslator {

	public class Result extends StaticPhaseResult {
		private List<Production> productions;

		public Result(List<Production> productions, 
				Iterable<? extends StaticError> errors) {
			super(errors);
			this.productions = productions;
		}

		public List<Production> productions() { return productions; }

	}

	/**
	 * Translate a collection of Fortress productions to Rats! productions
	 * @param productions
	 * @return
	 */
	public static Result translate(Collection<ProductionDef> productions) {
		ProductionTranslator pt = new ProductionTranslator();
		List<Production> ratsProductions = new LinkedList<Production>();
		Collection<StaticError> errors = new LinkedList<StaticError>();
		
		for (ProductionDef production: productions) {
			Result result = pt.translate(production);
			for (StaticError se: result.errors()) {
				errors.add(se);
			}
			ratsProductions.addAll(result.productions());
		}
		
		return pt.new Result(ratsProductions, errors);
	}

	/**
	 * Translate a Fortress production to a Rats! production 
	 * @param production
	 * @return
	 */
	private Result translate(ProductionDef production) {
		Collection<StaticError> errors = new LinkedList<StaticError>();
		
		List<Sequence> sequence = new LinkedList<Sequence>();
		
		for (SyntaxDef syntaxDef: production.getSyntaxDefs()) {
			List<Element> elms = new LinkedList<Element>();
			for (SyntaxSymbol sym: syntaxDef.getSyntaxSymbols()) {
				elms.addAll(sym.accept(new SymbolTranslator()));
				elms.add(new NonTerminal("w")); // Todo: implement all the symbols
			}
			String productionName = FreshName.getFreshName(production.getName().toString()).toUpperCase();
			ActionCreater.Result acr = ActionCreater.create(productionName ,syntaxDef.getTransformationExpression(), production.getType().toString());
			if (!acr.isSuccessful()) { for (StaticError e: acr.errors()) { errors.add(e); } }
			elms.add(acr.action());
			sequence.add(new Sequence(new SequenceName(productionName), elms));		
		}
		
		Production ratsProduction = null;
		if (production.getExtends().isSome()) {
			ProductionEnum productionEnum = ModuleInfo.getProductionEnum(Option.unwrap(production.getExtends()).getName().stringName());
			ratsProduction = new AlternativeAddition(ModuleInfo.getProductionReturnType(productionEnum),
							 new NonTerminal(ModuleInfo.getProductionName(productionEnum)),
							 new OrderedChoice(sequence), 
							 new SequenceName(ModuleInfo.getExtensionPoint(productionEnum)),false);
			ratsProduction.name = new NonTerminal(production.getName().toString());
		}
		else {
			List<Attribute> attr = new LinkedList<Attribute>();
			String type = production.getType().toString();
			ratsProduction = new FullProduction(attr, type,
							 new NonTerminal(production.getName().toString()),
							 new OrderedChoice(sequence));
			ratsProduction.name = new NonTerminal(production.getName().toString());
		}
		
		List<Production> productions = new LinkedList<Production>();
		productions.add(ratsProduction);
		return new Result(productions, errors);
	}
	
	private static class SymbolTranslator extends NodeDepthFirstVisitor<List<Element>> {

		private List<Element> mkList(Element e) {
			List<Element> els = new LinkedList<Element>();
			els.add(e);
			return els;
		}
		
		@Override
		public List<Element> forNonterminalSymbol(NonterminalSymbol that) {
			return mkList(new NonTerminal(that.getNonterminal().getName().stringName()));
		}

		@Override
		public List<Element> forOptionalSymbol(OptionalSymbol that) {
			// TODO Auto-generated method stub
			return super.forOptionalSymbol(that);
		}

		@Override
		public List<Element> forOptionalSymbolOnly(OptionalSymbol that,
				List<Element> symbol_result) {
			// TODO Auto-generated method stub
			return super.forOptionalSymbolOnly(that, symbol_result);
		}

		@Override
		public List<Element> forRepeatOneOrMoreSymbol(RepeatOneOrMoreSymbol that) {
			// TODO Auto-generated method stub
			return super.forRepeatOneOrMoreSymbol(that);
		}

		@Override
		public List<Element> forRepeatOneOrMoreSymbolOnly(
				RepeatOneOrMoreSymbol that, List<Element> symbol_result) {
			// TODO Auto-generated method stub
			return super.forRepeatOneOrMoreSymbolOnly(that, symbol_result);
		}

		@Override
		public List<Element> forRepeatSymbol(RepeatSymbol that) {
			// TODO Auto-generated method stub
			return super.forRepeatSymbol(that);
		}

		@Override
		public List<Element> forRepeatSymbolOnly(RepeatSymbol that,
				List<Element> symbol_result) {
			// TODO Auto-generated method stub
			return super.forRepeatSymbolOnly(that, symbol_result);
		}

		@Override
		public List<Element> forTokenSymbol(TokenSymbol that) {
			return mkList(new NonTerminal(that.getToken()));
		}

		@Override
		public List<Element> forWhitespaceSymbol(WhitespaceSymbol that) {
			// TODO Auto-generated method stub
			return super.forWhitespaceSymbol(that);
		}

		@Override
		public List<Element> forWhitespaceSymbolOnly(WhitespaceSymbol that,
				List<Element> symbol_result) {
			// TODO Auto-generated method stub
			return super.forWhitespaceSymbolOnly(that, symbol_result);
		}
		
	}

}
