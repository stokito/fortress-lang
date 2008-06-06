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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import xtc.parser.Action;
import xtc.parser.ParseError;

import com.sun.fortress.compiler.StaticError;
import com.sun.fortress.compiler.StaticPhaseResult;
import com.sun.fortress.interpreter.drivers.ASTIO;
import com.sun.fortress.interpreter.drivers.ProjectProperties;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.AbstractNode;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes.Decl;
import com.sun.fortress.nodes.Export;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.Import;
import com.sun.fortress.nodes.LValueBind;
import com.sun.fortress.nodes.PrefixedSymbol;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.TransformationDecl;
import com.sun.fortress.nodes.TransformationExpressionDef;
import com.sun.fortress.nodes.TransformationTemplateDef;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.VarDecl;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.shell.CommandInterpreter;
import com.sun.fortress.syntax_abstractions.environments.GrammarEnv;
import com.sun.fortress.syntax_abstractions.environments.MemberEnv;
import com.sun.fortress.syntax_abstractions.environments.SyntaxDeclEnv;

import edu.rice.cs.plt.tuple.Option;

public class ActionCreater {

    protected static final String BOUND_VARIABLES = "boundVariables";
    protected static final String PACKAGE = "com.sun.fortress.syntax_abstractions.util";
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
            TransformationDecl transformation,
            BaseType type,
            SyntaxDeclEnv syntaxDeclEnv,
            Map<PrefixedSymbol,com.sun.fortress.syntax_abstractions.phases.VariableCollector.Depth> variables
            ) {
        ActionCreater ac = new ActionCreater();
        Collection<StaticError> errors = new LinkedList<StaticError>();

        String returnType = new FortressTypeToJavaType().analyze(type);

        List<Integer> indents = new LinkedList<Integer>();
        List<String> code = new LinkedList<String>();
        if (transformation instanceof TransformationExpressionDef) {
            code = ActionCreaterUtil.createVariableBinding(indents, syntaxDeclEnv, BOUND_VARIABLES, false, variables );
            Expr e = ((TransformationExpressionDef) transformation).getTransformation();
            Component component = ac.makeComponent(e, syntaxDeclEnv);
            String serializedComponent = ac.writeJavaAST(component);
            code.addAll(ActionCreaterUtil.createRatsAction(serializedComponent, indents));

            if (ProjectProperties.debug) {
                addCodeLine("System.err.println(\"Parsing... production: "+alternativeName+"\");", code, indents);
            }
            addCodeLine("yyValue = (new "+PACKAGE+".FortressObjectASTVisitor<"+returnType+">(createSpan(yyStart,yyCount))).dispatch((new "+PACKAGE+".InterpreterWrapper()).evalComponent(createSpan(yyStart,yyCount), \""+alternativeName+"\", code, "+BOUND_VARIABLES+").value());", code, indents);
        }
        else if (transformation instanceof TransformationTemplateDef) {
            code = ActionCreaterUtil.createVariableBinding(indents, syntaxDeclEnv, BOUND_VARIABLES, true, variables);
            AbstractNode n = ((TransformationTemplateDef) transformation).getTransformation();
            JavaAstPrettyPrinter jpp = new JavaAstPrettyPrinter(syntaxDeclEnv);
            String yyValue = n.accept(jpp);
            
            for (String s: jpp.getCode()) {
                addCodeLine(s, code, indents);
            }

            if (ProjectProperties.debug) {
                addCodeLine("System.err.println(\"Parsing... production: "+alternativeName+" with template\");", code, indents);
            }
            addCodeLine("yyValue = "+yyValue+";", code, indents);
        }
        Action a = new Action(code, indents);
        return ac.new Result(a, errors);
    }

    private static void addCodeLine(String s, List<String> code,
            List<Integer> indents) {
        indents.add(3);
        code.add(s);
    }

    private Component makeComponent(Expr expression, SyntaxDeclEnv syntaxDeclEnv) {
        APIName name = NodeFactory.makeAPIName("TransformationComponent");
        Span span = new Span();
        List<Import> imports = new LinkedList<Import>();
        imports.add(NodeFactory.makeImportStar(FORTRESS_AST));
        imports.add(NodeFactory.makeImportStar("List"));
        // Exports:
        List<Export> exports = new LinkedList<Export>();
        List<APIName> exportApis = new LinkedList<APIName>();
        exportApis.add(NodeFactory.makeAPIName("Executable"));
        exports.add(new Export(exportApis));

        // Decls:
        List<Decl> decls = new LinkedList<Decl>();
        for (Id var: syntaxDeclEnv.getVariables()) {
            List<LValueBind> valueBindings = new LinkedList<LValueBind>();
            Type type = syntaxDeclEnv.getType(var);
            valueBindings.add(new LValueBind(var, Option.some(type), false));
            decls.add(new VarDecl(valueBindings, NodeFactory.makeIntLiteralExpr(7)));
        }
        // entry point
        decls.add(NodeFactory.makeFnDecl(InterpreterWrapper.FUNCTIONNAME, ANY, expression));
        return new Component(span, name, imports, exports, decls);
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
