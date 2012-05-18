/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.astgen;

import edu.rice.cs.astgen.*;
import edu.rice.cs.astgen.Types.TypeName;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.Pair;

import java.util.ArrayList;
import static java.util.Arrays.asList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class TransformationNodeCreator extends CodeGenerator implements Runnable {

    public TransformationNodeCreator(ASTModel ast) {
        super(ast);
    }

    @Override
    public Iterable<Class<? extends CodeGenerator>> dependencies() {
        return new LinkedList<Class<? extends CodeGenerator>>();
    }

    private <T> List<T> combine(List<T>... a) {
        List<T> ok = new ArrayList<T>();
        for (List<T> in : a) {
            ok.addAll(in);
        }
        return ok;
    }

    public void run() {
        List<Pair<NodeType, NodeType>> all = new LinkedList<Pair<NodeType, NodeType>>();

        List<Field> normalFields = asList(new Field(Types.parse("java.util.Map<String,Level>", ast), "variables", Option.<String>none(), false, false, true), new Field(Types.parse("java.util.List<String>", ast), "syntaxParameters", Option.<String>none(), false, false, true), new Field(Types.parse("String", ast), "syntaxTransformer", Option.<String>none(), false, false, true));

        List<Field> astFields = combine(asList(new Field(Types.parse("ASTNodeInfo", ast), "info", Option.<String>some("NodeFactory.makeASTNodeInfo(NodeFactory.macroSpan)"), false, true, true)), normalFields);

        List<Field> exprFields = combine(asList(new Field(Types.parse("ExprInfo", ast), "info", Option.<String>some("NodeFactory.makeExprInfo(NodeFactory.macroSpan)"), false, true, true)), normalFields);

        List<Field> typeFields = combine(asList(new Field(Types.parse("TypeInfo", ast), "info", Option.<String>some("NodeFactory.makeTypeInfo(NodeFactory.macroSpan)"), false, true, true)), normalFields);

        NodeType abstractNode;
        NodeType exprNode;
        NodeType typeNode;
        if (ast.typeForName("AbstractNode").isSome() && ast.typeForName("Expr").isSome() && ast.typeForName("Type").isSome()) {
            abstractNode = ast.typeForName("AbstractNode").unwrap();
            exprNode = ast.typeForName("Expr").unwrap();
            typeNode = ast.typeForName("Type").unwrap();
        } else throw new RuntimeException("Fortress.ast does not define AbstractNode/Expr/Type!");
        for (NodeType n : ast.classes()) {
            if (n.getClass() == NodeClass.class && ast.isDescendent(abstractNode, n) && !n.name().startsWith("TemplateGap") && !n.name().startsWith("_Ellipses")) {

                String infoType;
                List<Field> fields;
                if (ast.isDescendent(exprNode, n)) {
                    infoType = "ExprInfo";
                    fields = exprFields;
                } else if (ast.isDescendent(typeNode, n)) {
                    infoType = "TypeInfo";
                    fields = typeFields;
                } else {
                    infoType = "ASTNodeInfo";
                    fields = astFields;
                }

                // NodeType child = new TransformationNode((NodeClass) n,ast,infoType);
                NodeType child = new NodeClass("_SyntaxTransformation" + ((NodeClass) n).name(), false, fields, Types.parse((n).name(), ast), Collections.singletonList(Types.parse("_SyntaxTransformation", ast)));
                all.add(new Pair<NodeType, NodeType>(child, n));
            }
        }
        for (Pair<NodeType, NodeType> p : all) {
            ast.addType(p.first(), false, p.second());
        }
    }

    /*
    private NodeType createNode( NodeType parent ){
        List<TypeName> interfaces = new ArrayList<TypeName>();
        interfaces.add( Types.parse("_SyntaxTransformation", ast ) );
        return new NodeClass( "_SyntaxTransformation" + parent.name(), false, FIELDS, Types.parse( parent.name(), ast ), interfaces );
    }
    */

    @Override
    public void generateAdditionalCode() {
    }

    @Override
    public void generateClassMembers(TabPrintWriter writer, NodeClass arg1) {
        System.out.println("Transformation creator: " + arg1.name());
        if (arg1.name().startsWith("_SyntaxTransformation")) {
            writer.startLine("public Node accept(TemplateUpdateVisitor visitor) {");
            writer.indent();
            writer.startLine("return visitor.for" + arg1.name() + "(this);");
            writer.unindent();
            writer.startLine("}");
        }
    }

    @Override
    public void generateInterfaceMembers(TabPrintWriter arg0, NodeInterface arg1) {
    }
}
