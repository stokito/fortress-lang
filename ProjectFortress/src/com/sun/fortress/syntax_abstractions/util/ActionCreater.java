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

import com.sun.fortress.syntax_abstractions.phases.VariableCollector;
import com.sun.fortress.syntax_abstractions.rats.util.FreshName;

import com.sun.fortress.compiler.StaticPhaseResult;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.nodes_util.ASTIO;
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
import com.sun.fortress.nodes.TransformerDecl;
import com.sun.fortress.nodes.TransformerDef;
import com.sun.fortress.nodes.TransformerExpressionDef;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.TraitType;
import com.sun.fortress.nodes.TypeArg;
import com.sun.fortress.nodes.VarDecl;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.syntax_abstractions.environments.SyntaxDeclEnv;
import com.sun.fortress.useful.Debug;

import edu.rice.cs.plt.tuple.Option;

public class ActionCreater {

    protected static final String BOUND_VARIABLES = "boundVariables";
    protected static final String PACKAGE = "com.sun.fortress.syntax_abstractions.util";
    private static final String FORTRESS_AST = "FortressAst";
    private static final String FORTRESS_AST_UTIL = "FortressAstUtil";
    private static int indent = 3;

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
            TransformerDecl transformation,
            BaseType type,
            SyntaxDeclEnv syntaxDeclEnv,
            Map<PrefixedSymbol,VariableCollector.Depth> variables
            ) {
        ActionCreater ac = new ActionCreater();
        Collection<StaticError> errors = new LinkedList<StaticError>();

        String transformer = FreshName.getFreshName(String.format("%sTransformer", alternativeName));
        String returnType = new FortressTypeToJavaType().analyze(type);

        List<Integer> indents = new LinkedList<Integer>();
        List<String> code = new LinkedList<String>();
        if (transformation instanceof TransformerExpressionDef) {
            code = ActionCreaterUtil.createVariableBinding(indents, syntaxDeclEnv, BOUND_VARIABLES, false, variables );
            Expr e = ((TransformerExpressionDef) transformation).getTransformer();
            Component component = ac.makeComponent(e, syntaxDeclEnv, variables);
            String serializedComponent = ac.writeJavaAST(component);
            code.addAll(ActionCreaterUtil.createRatsAction(serializedComponent, indents));

            if (Debug.isOnMaxFor(Debug.Type.SYNTAX)) {
                addCodeLine("System.err.println(\"Parsing... production: "+alternativeName+"\");", code, indents);
            }
            addCodeLine("yyValue = (new "+PACKAGE+".FortressObjectASTVisitor<"+returnType+">(createSpan(yyStart,yyCount))).dispatch((new "+PACKAGE+".InterpreterWrapper()).evalComponent(createSpan(yyStart,yyCount), \""+alternativeName+"\", code, "+BOUND_VARIABLES+").value());", code, indents);
        }
        else if (transformation instanceof TransformerDef) {
            code = ActionCreaterUtil.createVariableBinding(indents, syntaxDeclEnv, BOUND_VARIABLES, true, variables);
            AbstractNode n = ((TransformerDef) transformation).getTransformer();
            JavaAstPrettyPrinter jpp = new JavaAstPrettyPrinter(syntaxDeclEnv);
            String yyValue = n.accept(jpp);
            
            addCodeLine( String.format("class %s implements com.sun.fortress.syntax_abstractions.phases.SyntaxTransformer<%s> {", transformer, returnType), code, indents );
            moreIndent();
            addCodeLine( String.format("public %s invoke(){", returnType), code, indents );
            moreIndent();

            for (String s: jpp.getCode()) {
                addCodeLine(s, code, indents);
            }

            if (Debug.isOnMaxFor(Debug.Type.SYNTAX)) {
                addCodeLine("System.err.println(\"Parsing... production: "+alternativeName+" with template\");", code, indents);
            }
            // addCodeLine("yyValue = "+yyValue+";", code, indents);
            addCodeLine("return "+yyValue+";", code, indents);
            lessIndent();
            addCodeLine("}", code, indents );
            lessIndent();
            addCodeLine("}", code, indents );

            addCodeLine( String.format( "yyValue = new _SyntaxTransformation%s(createSpan(yyStart,yyCount), new %s());", returnType, transformer ), code, indents );
        }
        Action a = new Action(code, indents);
        return ac.new Result(a, errors);
    }

    private static void moreIndent(){
        indent += 1;
    }

    private static void lessIndent(){
        indent -= 1;
        if ( indent < 1 ){
            indent = 1;
        }
    }

    private static void addCodeLine(String s, List<String> code,
            List<Integer> indents) {
        indents.add(indent);
        code.add(s);
    }

    private Component makeComponent(Expr expression, final SyntaxDeclEnv syntaxDeclEnv, Map<PrefixedSymbol,VariableCollector.Depth> variables ){
        APIName name = NodeFactory.makeAPIName("TransformationComponent");
        Span span = new Span();
        List<Import> imports = new LinkedList<Import>();
        /* TODO: these imports should be the same imports that the api with the grammar had */
        imports.add(NodeFactory.makeImportStar(FORTRESS_AST_UTIL));
        imports.add(NodeFactory.makeImportStar(FORTRESS_AST));
        imports.add(NodeFactory.makeImportStar("List"));
        // Exports:
        List<Export> exports = new LinkedList<Export>();
        List<APIName> exportApis = new LinkedList<APIName>();
        exportApis.add(NodeFactory.makeAPIName("Executable"));
        exports.add(new Export(exportApis));

        // Decls:
        List<Decl> decls = new LinkedList<Decl>();
        for (Map.Entry<PrefixedSymbol, VariableCollector.Depth> var: variables.entrySet()) {
            final PrefixedSymbol sym = var.getKey();
            final VariableCollector.Depth depth = var.getValue();
            List<LValueBind> valueBindings = new LinkedList<LValueBind>();
            // Type type = syntaxDeclEnv.getType(var);
            Type type = depth.accept(new VariableCollector.DepthVisitor<Type>(){
                    public Type forBaseDepth(VariableCollector.Depth d) {
                        Id id = sym.getId().unwrap();
                        return syntaxDeclEnv.getType(id);
                    }

                    public Type forListDepth(VariableCollector.Depth d) {
                        List<StaticArg> args = new LinkedList<StaticArg>();
                        args.add( new TypeArg( d.getParent().accept(this) ) );
                        return new TraitType(NodeFactory.makeId(NodeFactory.makeAPIName("List"),NodeFactory.makeId("List")), args);
                    }

                    public Type forOptionDepth(VariableCollector.Depth d) {
                        List<StaticArg> args = new LinkedList<StaticArg>();
                        args.add( new TypeArg( d.getParent().accept( this ) ) );
                        return new TraitType(NodeFactory.makeId(NodeFactory.makeAPIName("FortressLibrary"), NodeFactory.makeId("Maybe")), args);
                    }
                });
            valueBindings.add(new LValueBind(sym.getId().unwrap(), Option.some(type), false));
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
