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

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import xtc.parser.ParseError;
import xtc.parser.SemanticValue;

import com.sun.fortress.compiler.Parser;
import com.sun.fortress.compiler.StaticError;
import com.sun.fortress.compiler.StaticPhaseResult;
import com.sun.fortress.nodes.AbstractNode;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.NonterminalDef;
import com.sun.fortress.nodes.NonterminalExtensionDef;
import com.sun.fortress.nodes.PrefixedSymbol;
import com.sun.fortress.nodes.SyntaxDef;
import com.sun.fortress.nodes.TransformationPreTemplateDef;
import com.sun.fortress.nodes.TransformationTemplateDef;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.useful.Pair;
import com.sun.fortress.useful.Useful;

import edu.rice.cs.plt.tuple.Option;


/*
 * Parse pretemplates and replace with real templates
 */
public class TemplateParser extends NodeUpdateVisitor {

	/**
	 * Result of the module translation
	 */
	public static class Result extends StaticPhaseResult {
		Api api;

		public Result(Api api, 
				Collection<StaticError> errors) {
			super(errors);
			this.api = api;
		}

		public Result(Api api,
				Iterable<? extends StaticError> errors) {
			super(errors);
			this.api = api;
		}

		public Api modules() { return api; }
	}

	private Collection<Parser.Error> errors;
	private Collection<String> vars;
	private LinkedList<String> syntaxDefVars;

	public TemplateParser() {
		this.errors = new LinkedList<Parser.Error>();
	}

	private Collection<? extends StaticError> getErrors() {
		return this.errors;
	}

	private boolean isSuccessfull() {
		return this.errors.isEmpty();
	}

	public static Result parseTemplates(Api api) {
		TemplateParser templateParser = new TemplateParser();
		Api a = (Api) api.accept(templateParser);
		if (!templateParser.isSuccessfull()) {
			return new Result(a, templateParser.getErrors());
		}
		return new Result(a, Collections.<StaticError>emptyList());
	}

	@Override
	public Node forNonterminalDef(NonterminalDef that) {
		this.vars = new LinkedList<String>();
		for (Pair<Id, Type> p: that.getParams()) {
			this.vars.add(p.getA().toString());
		}
		return super.forNonterminalDef(that);
	}

	@Override
	public Node forNonterminalExtensionDef(NonterminalExtensionDef that) {
		this.vars = new LinkedList<String>();
		for (Pair<Id, Type> p: that.getParams()) {
			this.vars.add(p.getA().toString());
		}
		return super.forNonterminalExtensionDef(that);
	}

	@Override
	public Node forSyntaxDef(SyntaxDef that) {
		this.syntaxDefVars = new LinkedList<String>();
		return super.forSyntaxDef(that);
	}

	@Override
	public Node forPrefixedSymbol(PrefixedSymbol that) {
		// We assume that all prefixed symbols have an identifier.
		// If that is not the case then it is an error in the disambiguation and not here.
		this.syntaxDefVars.add(that.getId().unwrap().toString());
		return super.forPrefixedSymbol(that);
	}

	@Override
	public Node forTransformationPreTemplateDefOnly(TransformationPreTemplateDef that) {
		TemplateVarRewriter tvs = new TemplateVarRewriter();
		List<String> vs = new LinkedList<String>();
		vs.addAll(this.vars);
		vs.addAll(this.syntaxDefVars);
		String p = tvs.rewriteVars(vs, that.getTransformation());
		Option<Node> res = parseTemplate(that.getSpan(), p, that.getProductionName());
		return res.unwrap(that);
	}

	private Option<Node> parseTemplate(Span span, String transformation, String productionName) {
		BufferedReader in = Useful.bufferedStringReader(transformation);
		com.sun.fortress.parser.Fortress p =
			new com.sun.fortress.parser.Fortress(in, "FooBar");
		try {
			xtc.parser.Result parseResult = p.pExpression$Expr(0);
			if (parseResult.hasValue()) {
				Object cu = ((SemanticValue) parseResult).value;
				if (cu instanceof AbstractNode) {
					return Option.<Node>some(new TransformationTemplateDef(span, (AbstractNode) cu));
				} 
				throw new RuntimeException("Unexpected parse result: " + cu);
			} 
			this.errors.add(new Parser.Error((ParseError) parseResult, p));
			return Option.none();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
	}

}
