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
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import java.lang.reflect.Method;

import xtc.parser.ParseError;
import xtc.parser.SemanticValue;

import com.sun.fortress.compiler.Parser;
import com.sun.fortress.compiler.StaticPhaseResult;
import com.sun.fortress.exceptions.ParserError;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.interpreter.drivers.ASTIO;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.AbstractNode;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes.Decl;
import com.sun.fortress.nodes.Export;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.Import;
import com.sun.fortress.nodes.LValueBind;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.NonterminalDef;
import com.sun.fortress.nodes.NonterminalExtensionDef;
import com.sun.fortress.nodes.NonterminalHeader;
import com.sun.fortress.nodes.PrefixedSymbol;
import com.sun.fortress.nodes.SyntaxDef;
import com.sun.fortress.nodes.TransformationPreTemplateDef;
import com.sun.fortress.nodes.TransformationTemplateDef;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.VarDecl;
import com.sun.fortress.nodes.VarType;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.parser.Fortress;
import com.sun.fortress.syntax_abstractions.environments.GrammarEnv;
import com.sun.fortress.syntax_abstractions.environments.MemberEnv;
import com.sun.fortress.syntax_abstractions.environments.SyntaxDeclEnv;
import com.sun.fortress.syntax_abstractions.environments.SyntaxDeclEnv.PrefixSymbolSymbolGetter;
import com.sun.fortress.syntax_abstractions.util.InterpreterWrapper;
import com.sun.fortress.useful.Pair;
import com.sun.fortress.useful.Useful;

import edu.rice.cs.plt.tuple.Option;


/*
 * Parse pretemplates and replace with real templates
 */
public class TemplateParser extends NodeUpdateVisitor {

	public static class Result extends StaticPhaseResult {
		private Api api;

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

		public Api api() { return api; }
	}
	
	private Collection<ParserError> errors;
	private Map<Id, BaseType> vars;
	private Map<Id, BaseType> varsToNonterminalType;

	
	public TemplateParser() {
		this.errors = new LinkedList<ParserError>();
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
    public Node forNonterminalHeader(NonterminalHeader that) {
        this.vars = new HashMap<Id, BaseType>();
        for (Pair<Id, Id> p: that.getParams()) {           
            this.vars.put(p.getA(), getType(p.getB()));
        }
        return super.forNonterminalHeader(that);
    }

	private BaseType getType(Id id) {
	    MemberEnv mEnv = GrammarEnv.getMemberEnv(id);
        return mEnv.getAstType();
    }

    @Override
	public Node forSyntaxDef(SyntaxDef that) {
		this.varsToNonterminalType = new HashMap<Id, BaseType>();
		return super.forSyntaxDef(that);
	}

	@Override
	public Node forPrefixedSymbol(PrefixedSymbol that) {
		// We assume that all prefixed symbols have an identifier.
		// If that is not the case then it is an error in the disambiguation and not here.
	    Id id = that.getId().unwrap();
	    PrefixSymbolSymbolGetter psg = new SyntaxDeclEnv.PrefixSymbolSymbolGetter(id);
        that.getSymbol().accept(psg);
        for (Entry<Id, Id> e: psg.getVarToNonterminalName().entrySet()) {
            this.varsToNonterminalType.put(e.getKey(), getType(e.getValue()));
        }
        for (Id i: psg.getAnyChars()) {
            this.varsToNonterminalType.put(i, new VarType(i.getSpan(), new Id("CharLiteralExpr")));
        }
		return super.forPrefixedSymbol(that);
	}

	@Override
	public Node forTransformationPreTemplateDefOnly(TransformationPreTemplateDef that) {
		TemplateVarRewriter tvs = new TemplateVarRewriter();
		Map<Id, BaseType> vs = new HashMap<Id, BaseType>();
		vs.putAll(this.vars);
		vs.putAll(this.varsToNonterminalType);
		String p = tvs.rewriteVars(vs, that.getTransformation());
		Option<Node> res = parseTemplate(that.getSpan(), p, that.getProductionName());
		return res.unwrap(that);
	}

	/** 
	 * Find the method that would parse a given production, such as
	 * pExpression$Expr when given "Expr".
	 */ 
	private Option<Method> lookupExpression(Class parser, String production){
		try{
			/* This is a Rats! specific naming convention. Move it
			 * elsewhere?
			 */
			String fullName = "pExpression$" + production;
		    // String fullName = "pExprOnly";
			Method found = parser.getDeclaredMethod(fullName, int.class);

			/* method is private by default so we have to make
			 * it accessible
			 */
			if ( found != null ){
				found.setAccessible(true);
				return Option.wrap(found);
			}
			return Option.none();
		} catch (NoSuchMethodException e){
			throw new RuntimeException(e);
		} catch (SecurityException e){
			throw new RuntimeException(e);
		}
	}

	/**
	 * Wraps the invoked method to return a xtc.parser.Result and also throws
	 * IOException.
	 */
	private xtc.parser.Result invokeParseMethod(com.sun.fortress.parser.templateparser.TemplateParser parser, Method method, int num) throws IOException {
		try{
			return (xtc.parser.Result) method.invoke(parser, num);
		} catch (IllegalAccessException e){
			throw new RuntimeException(e);
		} catch (java.lang.reflect.InvocationTargetException e){
			throw new RuntimeException(e);
		}
	}

	private Option<Node> parseTemplate(Span span, String transformation, String productionName) {
		BufferedReader in = Useful.bufferedStringReader(transformation.trim());
		com.sun.fortress.parser.templateparser.TemplateParser parser =
                    new com.sun.fortress.parser.templateparser.TemplateParser(in, span.getBegin().getFileName());
                parser.setExpectedName(Option.<APIName>none());
		Option<Method> parse = lookupExpression(parser.getClass(), productionName);
		if ( ! parse.isSome() ){
			throw new RuntimeException("Did not find method " + productionName);
		}

		try {
//		    System.err.println("PARSING: "+transformation);
			xtc.parser.Result parseResult = invokeParseMethod(parser,parse.unwrap(),0);
			if (parseResult.hasValue()) {
				Object cu = ((SemanticValue) parseResult).value;
				if (cu instanceof AbstractNode) {
//				    System.err.println("RESULT: "+writeJavaAST(makeComponent((Expr) cu)));
					return Option.<Node>some(new TransformationTemplateDef(span, (AbstractNode) cu));
				} 
				throw new RuntimeException("Unexpected parse result: " + cu);
			} 
//			System.err.println("Error: "+((ParseError) parseResult).msg);
			this.errors.add(new ParserError((ParseError) parseResult, parser));
			return Option.none();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
	}
	
    private Component makeComponent(Expr expression) {
        APIName name = NodeFactory.makeAPIName("TransformationComponent");
        Span span = new Span();

        // Decls:
        List<Decl> decls = new LinkedList<Decl>();
        // entry point
        decls.add(NodeFactory.makeFnDecl(InterpreterWrapper.FUNCTIONNAME, new Id("Type"), expression));
        return new Component(span, name, new LinkedList<Import>(), new LinkedList<Export>(), decls);
    }

    public String writeJavaAST(Component component) {
        try {
            StringWriter sw = new StringWriter();
            BufferedWriter bw = new BufferedWriter(sw);
            ASTIO.writeJavaAst(component, bw);
            bw.flush();
            bw.close();
            //   System.err.println(sw.getBuffer().toString());
            return sw.getBuffer().toString();
        } catch (IOException e) {
            throw new RuntimeException("Unexpected error: "+e.getMessage());
        }
    }
}
