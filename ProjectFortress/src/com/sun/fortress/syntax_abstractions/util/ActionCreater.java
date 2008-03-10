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
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import xtc.parser.Action;
import xtc.util.Utilities;

import com.sun.fortress.compiler.StaticError;
import com.sun.fortress.compiler.StaticPhaseResult;
import com.sun.fortress.interpreter.drivers.ASTIO;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes.Decl;
import com.sun.fortress.nodes.Export;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.FnDef;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdType;
import com.sun.fortress.nodes.Import;
import com.sun.fortress.nodes.InstantiatedType;
import com.sun.fortress.nodes.KeywordSymbol;
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
import com.sun.fortress.nodes.SyntaxSymbol;
import com.sun.fortress.nodes.TokenSymbol;
import com.sun.fortress.nodes.TraitType;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.TypeArg;
import com.sun.fortress.nodes.VarDecl;
import com.sun.fortress.nodes.VarargsParam;
import com.sun.fortress.nodes.VarargsType;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.syntax_abstractions.phases.NonterminalTypeDictionary;

import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.Pair;

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

	public static Result create(String alternativeName, 
			Expr e,
			TraitType type,
			Collection<PrefixedSymbol> boundVariables) {
		ActionCreater ac = new ActionCreater();
		Collection<StaticError> errors = new LinkedList<StaticError>();

		Component component = ac.makeComponent(e, boundVariables);
		String serializedComponent = "";
		serializedComponent = ac.writeJavaAST(component);

		String returnType = new FortressTypeToJavaType().analyze(type);

		List<Integer> indents = new LinkedList<Integer>();		
		List<String> code = createVariabelBinding(indents, boundVariables);
		code.addAll(createRatsAction(serializedComponent, indents));
		addCodeLine("System.err.println(\"Parsing... production: "+alternativeName+"\");", code, indents);
		addCodeLine("yyValue = (new "+PACKAGE+".FortressObjectASTVisitor<"+returnType+">(createSpan(yyStart,yyCount))).dispatch((new "+PACKAGE+".InterpreterWrapper()).evalComponent(createSpan(yyStart,yyCount), \""+alternativeName+"\", code, "+BOUND_VARIABLES+").value());", code, indents);

		// System.err.println(code);
		Action a = new Action(code, indents);
		return ac.new Result(a, errors);
	}

	private static void addCodeLine(String s, List<String> code,
			List<Integer> indents) {
		indents.add(3);
		code.add(s);		
	}

	/**
	 * 
	 * @param ps
	 * @param nonterminalNameToDeclaredReturnType
	 * @return
	 */
	private Option<Type> getType(PrefixedSymbol ps) {
		return ps.getSymbol().accept(new NodeDepthFirstVisitor<Option<Type>>() {
			@Override
			public Option<Type> defaultCase(Node that) {
				throw new RuntimeException("Unexpected case: "+that.getClass());
			}		

			@Override
			public Option<Type> forOptionalSymbol(OptionalSymbol that) {
				return handle(that.getSymbol(), SyntaxAbstractionUtil.FORTRESSLIBRARY, SyntaxAbstractionUtil.MAYBE);
			}

			@Override
			public Option<Type> forRepeatOneOrMoreSymbol(RepeatOneOrMoreSymbol that) {
				return handle(that.getSymbol(), SyntaxAbstractionUtil.ARRAYLIST, SyntaxAbstractionUtil.LIST);
			}

			@Override
			public Option<Type> forRepeatSymbol(RepeatSymbol that) {
				return handle(that.getSymbol(), SyntaxAbstractionUtil.ARRAYLIST, SyntaxAbstractionUtil.LIST);
			}

			@Override 
			public Option<Type> forNonterminalSymbol(NonterminalSymbol that) {
				return NonterminalTypeDictionary.getType(that.getNonterminal().getName().getText());
			}
			
			@Override
			public Option<Type> forKeywordSymbol(KeywordSymbol that) {
				QualifiedIdName string = NodeFactory.makeQualifiedIdName(SyntaxAbstractionUtil.FORTRESSBUILTIN, SyntaxAbstractionUtil.STRING);
				return Option.<Type>some(new IdType(string));
			}
			
			@Override
			public Option<Type> forTokenSymbol(TokenSymbol that) {
				QualifiedIdName string = NodeFactory.makeQualifiedIdName(SyntaxAbstractionUtil.FORTRESSBUILTIN, SyntaxAbstractionUtil.STRING);
				return Option.<Type>some(new IdType(string));
			}

			private Option<Type> handle(SyntaxSymbol symbol, String api, String id) {
				Option<Type> t = symbol.accept(this);
				if (t.isNone()) {
					return t;
				}
				Type type = Option.unwrap(t);
				QualifiedIdName list = NodeFactory.makeQualifiedIdName(api, id);
				List<StaticArg> args = new LinkedList<StaticArg>();
				args.add(new TypeArg(type));
				return Option.<Type>some(new InstantiatedType(list, args));
			}
		});
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

	private Component makeComponent(Expr expression, Collection<PrefixedSymbol> boundVariables) {
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
			Option<Type> type = getType(ps);
			valueBindings.add(new LValueBind(new Id(var), type, false));
			decls.add(new VarDecl(valueBindings, NodeFactory.makeIntLiteralExpr(7)));
		}
		// entry point
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

	private APIName makeAPIName(String name) {
		List<Id> ids = new LinkedList<Id>();
		ids.add(new Id(name));
		return new APIName(ids);
	}

	public String writeJavaAST(Component component) {
		try {
			StringWriter sw = new StringWriter();
			BufferedWriter bw = new BufferedWriter(sw);
			ASTIO.writeJavaAst(component, bw);
			bw.flush();
			bw.close();
//			System.err.println(sw.getBuffer().toString());
			return sw.getBuffer().toString();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Unexpected error: "+e.getMessage());
		}
	}
}
