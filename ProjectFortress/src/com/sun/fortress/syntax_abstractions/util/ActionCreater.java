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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

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
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.nodes.SimpleName;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.TraitDecl;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.VarargsParam;
import com.sun.fortress.nodes.VarargsType;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.Printer;
import com.sun.fortress.nodes_util.Span;

import edu.rice.cs.plt.tuple.Option;

import xtc.parser.Action;
import xtc.parser.Production;
import xtc.util.Utilities;

public class ActionCreater {

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

	public static Result create(String productionName, Expr e, String returnType) {
		ActionCreater ac = new ActionCreater();
		Collection<StaticError> errors = new LinkedList<StaticError>();

		Component component = ac.makeComponent(e);
		String serializedComponent = "";
		try {
			serializedComponent = ac.writeJavaAST(component);
		} catch (IOException e1) {
			System.err.println(e1.getMessage());
			errors.add(StaticError.make(e1.getMessage(), e.getSpan().toString()));
		}

		List<Integer> indents = new LinkedList<Integer>();
//		FortressObjectASTToJavaFortressAST fortressToJava = new FortressObjectASTToJavaFortressAST();
//		InterpreterWrapper eval = new InterpreterWrapper();
//		List<String> ls = fortressToJava.dispatch(eval.evalExpression(e));
//		Iterator<String> it = ls.iterator();
//		String stms = "";
//		while (it.hasNext()) {
//		String s = it.next();
//		if (!it.hasNext()) {
//		stms += "yyValue = ";
//		}
//		stms += s+";\n";
//		}
		String[] sc = Utilities.SPACE_NEWLINE_SPACE.split(serializedComponent);
		List<String> code = new LinkedList<String>();
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
		indents.add(3);
		code.add("System.err.println(\"Parsing... production: "+productionName+"\");");
		indents.add(3);
		code.add("yyValue = (new "+PACKAGE+".FortressObjectASTVisitor<"+returnType+">()).dispatch((new "+PACKAGE+".InterpreterWrapper()).evalComponent(\""+productionName+"\", code).value());");
		System.err.println(code);
		Action a = new Action(code, indents);

		return ac.new Result(a, errors);
	}

	private Component makeComponent(Expr expression) {
		APIName name = makeAPIName("TransformationComponent");
		Span span = new Span();
		List<Import> imports = new LinkedList<Import>();
		imports.add(NodeFactory.makeImportStar(NodeFactory.makeAPIName(FORTRESS_AST), new LinkedList<SimpleName>()));
		// Exports:
		List<Export> exports = new LinkedList<Export>();
		List<APIName> exportApis = new LinkedList<APIName>();
		exportApis.add(makeAPIName("Executable"));
		exports.add(new Export(exportApis));

		// Decls:
		List<Decl> decls = new LinkedList<Decl>();
		TraitDecl objectTrait = new TraitDecl(ANY, new LinkedList<Decl>());
		decls.add(objectTrait);
		// entry point
		List<Param> params = new LinkedList<Param>();
		decls.add(makeFunction(InterpreterWrapper.FUNCTIONNAME, ANY, expression));
		//decls.add(makeFunction("run", STRING, expression));
		return new Component(span, name, imports, exports, decls);
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
