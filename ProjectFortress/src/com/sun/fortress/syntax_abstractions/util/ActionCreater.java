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

package com.sun.fortress.syntax_abstractions.util;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.sun.fortress.compiler.StaticError;
import com.sun.fortress.compiler.StaticPhaseResult;
import com.sun.fortress.interpreter.drivers.ASTIO;
import com.sun.fortress.interpreter.evaluator.values.FObject;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes.Decl;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Export;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.FnDef;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.Import;
import com.sun.fortress.nodes.InstantiatedType;
import com.sun.fortress.nodes.LValueBind;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeDepthFirstVisitor;
import com.sun.fortress.nodes.NonterminalSymbol;
import com.sun.fortress.nodes.OptionalSymbol;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.PrefixedSymbol;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.nodes.RepeatOneOrMoreSymbol;
import com.sun.fortress.nodes.RepeatSymbol;
import com.sun.fortress.nodes.SimpleName;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.StringLiteralExpr;
import com.sun.fortress.nodes.TraitDecl;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.TypeArg;
import com.sun.fortress.nodes.VarDecl;
import com.sun.fortress.nodes.VarargsParam;
import com.sun.fortress.nodes.VarargsType;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.Printer;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.syntax_abstractions.phases.ProductionTranslator.NonterminalNameToDeclaredReturnType;

import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.Pair;

import xtc.parser.Action;
import xtc.parser.Production;
import xtc.util.Utilities;

public class ActionCreater {

	private static final String BOUND_VARIABLES = "boundVariables";
	private static final String PACKAGE = "com.sun.fortress.syntax_abstractions.util";
	private static final String FORTRESS_AST = "FortressAst";

	public class Result extends StaticPhaseResult {
		private Action action;

		public Result(Action action,
				Iterable<? extends StaticError> errors) {
			super(errors);
			this.action = action;
		}

		public Action action() { return action; }

	}

	private static final Id ANY = new Id("Any");
	private static final Id STRING = new Id("String");

	public static Result create(String productionName, 
								Expr e,
								String returnType,
								Collection<PrefixedSymbol> boundVariables,
								NonterminalNameToDeclaredReturnType nonterminalNameToDeclaredReturnType) {
		ActionCreater ac = new ActionCreater();
		Collection<StaticError> errors = new LinkedList<StaticError>();

		Component component = ac.makeComponent(e, boundVariables, nonterminalNameToDeclaredReturnType);
		String serializedComponent = "";
		try {
			serializedComponent = ac.writeJavaAST(component);
//			System.err.println(serializedComponent);
		} catch (IOException e1) {
			errors.add(StaticError.make(e1.getMessage(), e.getSpan().toString()));
		}
		List<Integer> indents = new LinkedList<Integer>();		
		List<String> code = createVariabelBinding(indents, boundVariables);
		code.addAll(createRatsAction(serializedComponent, indents));
		indents.add(3);
		code.add("System.err.println(\"Parsing... production: "+productionName+"\");");
		indents.add(3);
		code.add("yyValue = (new "+PACKAGE+".FortressObjectASTVisitor<"+returnType+">()).dispatch((new "+PACKAGE+".InterpreterWrapper()).evalComponent(createSpan(yyStart,yyCount), \""+productionName+"\", code, "+BOUND_VARIABLES+").value());");
//		System.err.println(code);
		Action a = new Action(code, indents);

		return ac.new Result(a, errors);
	}

	enum Kinds {OPTIONAL, REPETITION}
	
	/**
	 * We siliently assume, that Terminal definitions does not 
	 * @param ps
	 * @param nonterminalNameToDeclaredReturnType
	 * @return
	 */
	private Option<Type> getType(PrefixedSymbol ps,
				NonterminalNameToDeclaredReturnType nonterminalNameToDeclaredReturnType) {

		Pair<String, Option<Kinds>> result = ps.getSymbol().accept(new NodeDepthFirstVisitor<Pair<String, Option<Kinds>>>() {

			@Override
			public Pair<String, Option<Kinds>> defaultCase(Node that) {
				return new Pair<String, Option<Kinds>>("", Option.<Kinds>none());
			}		
			
			@Override
			public Pair<String, Option<Kinds>> forOptionalSymbolOnly(
					OptionalSymbol that,
					Pair<String, Option<Kinds>> symbol_result) {
				return handle(symbol_result, Kinds.OPTIONAL);
			}

			@Override
			public Pair<String, Option<Kinds>> forRepeatOneOrMoreSymbolOnly(
					RepeatOneOrMoreSymbol that,
					Pair<String, Option<Kinds>> symbol_result) {
				return handle(symbol_result, Kinds.REPETITION);
			}

			@Override
			public Pair<String, Option<Kinds>> forRepeatSymbolOnly(
					RepeatSymbol that, Pair<String, Option<Kinds>> symbol_result) {
				return handle(symbol_result, Kinds.REPETITION);
			}
					
			@Override 
			public Pair<String, Option<Kinds>> forNonterminalSymbol(NonterminalSymbol that) {
				return new Pair<String, Option<Kinds>>(that.getNonterminal().getName().toString(), Option.<Kinds>none());
			}

			private Pair<String, Option<Kinds>> handle(
					Pair<String, Option<Kinds>> p, Kinds kind) {
				if (p.second().isNone()) {
					return new Pair<String, Option<Kinds>>(p.first(), Option.some(kind));
				}
				return new Pair<String, Option<Kinds>>(p.first(), Option.some(kind));
			}
		});
		
		String var = result.first();
	
		Option<Type> type = nonterminalNameToDeclaredReturnType.getType(result.first());

		if (type.isNone()) {
			return Option.none();
		}
		
		List<StaticArg> staticArgs = new LinkedList<StaticArg>();
		staticArgs.add(new TypeArg(Option.unwrap(type)));
		
		if (result.second().isNone()) {
			return type;
		}
		
		if (Option.unwrap(result.second()).equals(Kinds.OPTIONAL)) {
			QualifiedIdName name = NodeFactory.makeQualifiedIdName("FortressLibrary", "Maybe");
			return Option.<Type>some(NodeFactory.makeInstantiatedType(name, staticArgs));
		}
		else if (Option.unwrap(result.second()).equals(Kinds.REPETITION)) {
			QualifiedIdName name = NodeFactory.makeQualifiedIdName("ArrayList", "List");
			return Option.<Type>some(NodeFactory.makeInstantiatedType(name, staticArgs));
		}
		return Option.none();
	}
	
	private static List<String> createVariabelBinding(List<Integer> indents,
													  Collection<PrefixedSymbol> boundVariables) {
		List<String> code = new LinkedList<String>();
		indents.add(3);
		code.add("Map<String, Object> "+BOUND_VARIABLES+" = new HashMap<String, Object>();");
		for(PrefixedSymbol ps: boundVariables) {
			String s = Option.unwrap(ps.getId()).getText();
			indents.add(3);
			code.add(BOUND_VARIABLES+".put(\""+s+"\""+", "+s+");");
		}
		return code;
	}

	private static List<String> createRatsAction(String serializedComponent, List<Integer> indents) {
		List<String> code = new LinkedList<String>();
		String[] sc = Utilities.SPACE_NEWLINE_SPACE.split(serializedComponent);
		indents.add(3);
		code.add("String code = "+"\""+sc[0].replaceAll("\"", "\\\\\"") + " \"+");
		for (int inx = 1; inx < sc.length; inx++) {
			String s = "\""+sc[inx].replaceAll("\"", "\\\\\"") + " \"";
			if (inx < sc.length-1) {
				s += "+";
			}
			else {
				s += ";";
			}
			indents.add(3);
			code.add(s);
		}
		return code;
	}

	private Component makeComponent(Expr expression, Collection<PrefixedSymbol> boundVariables, NonterminalNameToDeclaredReturnType nonterminalNameToDeclaredReturnType) {
		APIName name = makeAPIName("TransformationComponent");
		Span span = new Span();
		List<Import> imports = new LinkedList<Import>();
		imports.add(makeImportStar(FORTRESS_AST));
		imports.add(makeImportStar("ArrayList"));
		// Exports:
		List<Export> exports = new LinkedList<Export>();
		List<APIName> exportApis = new LinkedList<APIName>();
		exportApis.add(makeAPIName("Executable"));
		exports.add(new Export(exportApis));

		// Decls:
		List<Decl> decls = new LinkedList<Decl>();
    	for (PrefixedSymbol ps: boundVariables) {
    		String var = Option.unwrap(ps.getId()).getText();
			List<LValueBind> valueBindings = new LinkedList<LValueBind>();
			Option<Type> type = getType(ps, nonterminalNameToDeclaredReturnType);
			valueBindings.add(new LValueBind(new Id(var), type, false));
			decls.add(new VarDecl(valueBindings, NodeFactory.makeIntLiteralExpr(7)));
    	}
		// entry point
		List<Param> params = new LinkedList<Param>();
		decls.add(makeFunction(InterpreterWrapper.FUNCTIONNAME, ANY, expression));
		return new Component(span, name, imports, exports, decls);
	}

	private Import makeImportStar(String apiName) {
		return NodeFactory.makeImportStar(NodeFactory.makeAPIName(apiName), new LinkedList<SimpleName>());
	}

	private Decl makeFunction(String functionName, Id typeString, Expr expression) {
		Id fnName = new Id(functionName);
		List<Param> params = new LinkedList<Param>();
		QualifiedIdName typeName = new QualifiedIdName(typeString);
		List<StaticArg> staticArgs = new LinkedList<StaticArg>();
		Type type = new InstantiatedType(typeName, staticArgs);
		params.add(new VarargsParam(new Id("args"), new VarargsType(type)));
		QualifiedIdName objectTypeName = new QualifiedIdName(typeString);
		Type returnType = new InstantiatedType(objectTypeName , staticArgs);
		return new FnDef(fnName, params, Option.some(returnType), expression);
	}

	/**
	 * @return
	 */
	private APIName makeAPIName(String name) {
		List<Id> ids = new LinkedList<Id>();
		ids.add(new Id(name));
		return new APIName(ids);
	}

	private String writeJavaAST(Component component) throws IOException {
		StringWriter sw = new StringWriter();
		BufferedWriter bw = new BufferedWriter(sw);
		ASTIO.writeJavaAst(component, bw);
		bw.flush();
		bw.close();
		return sw.getBuffer().toString();
	}

}
