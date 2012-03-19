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

import static java.util.Arrays.asList;
import java.util.LinkedList;
import java.util.List;

public class TemplateGapNodeCreator extends CodeGenerator implements Runnable {

    private TypeName idType = Types.parse("Id", ast);
    private TypeName listIdType = Types.parse("List<Id>", ast);

    protected static List<Field> TEMPLATEGAPFIELDS; {
        TEMPLATEGAPFIELDS = asList(new Field(Types.parse("ASTNodeInfo", ast), "info", Option.<String>some("NodeFactory.makeASTNodeInfo(NodeFactory.macroSpan)"), false, true, true), new Field(idType, "gapId", Option.<String>none(), false, false, true), new Field(listIdType, "templateParams", Option.<String>none(), false, false, true));
    }

    protected static List<Field> TEMPLATEGAPEXPRFIELDS; {
        TEMPLATEGAPEXPRFIELDS = asList(new Field(Types.parse("ExprInfo", ast), "info", Option.<String>some("NodeFactory.makeExprInfo(NodeFactory.macroSpan)"), false, true, true), new Field(idType, "gapId", Option.<String>none(), false, false, true), new Field(listIdType, "templateParams", Option.<String>none(), false, false, true));
    }

    protected static List<Field> TEMPLATEGAPTYPEFIELDS; {
        TEMPLATEGAPTYPEFIELDS = asList(new Field(Types.parse("TypeInfo", ast), "info", Option.<String>some("NodeFactory.makeTypeInfo(NodeFactory.macroSpan)"), false, true, true), new Field(idType, "gapId", Option.<String>none(), false, false, true), new Field(listIdType, "templateParams", Option.<String>none(), false, false, true));
    }

    public TemplateGapNodeCreator(ASTModel ast) {
        super(ast);
    }

    @Override
    public Iterable<Class<? extends CodeGenerator>> dependencies() {
        return new LinkedList<Class<? extends CodeGenerator>>();
    }

    public void run() {
        TypeName templateGapName = Types.parse("TemplateGap", ast);
        List<Pair<NodeType, NodeType>> templateGaps = new LinkedList<Pair<NodeType, NodeType>>();
        NodeType abstractNode;
        NodeType exprNode;
        NodeType typeNode;
        if (ast.typeForName("AbstractNode").isSome() && ast.typeForName("Expr").isSome() && ast.typeForName("Type").isSome()) {
            abstractNode = ast.typeForName("AbstractNode").unwrap();
            exprNode = ast.typeForName("Expr").unwrap();
            typeNode = ast.typeForName("Type").unwrap();
        } else throw new RuntimeException("Fortress.ast does not define AbstractNode/Expr/Type!");
        for (NodeClass nodeClass : ast.classes()) {
            if (nodeClass.getClass() == NodeClass.class && ast.isDescendent(abstractNode, nodeClass) && !nodeClass.name().startsWith("_Ellipses") && !nodeClass.name().startsWith("_SyntaxTransformation")) {
                List<TypeName> interfaces = new LinkedList<TypeName>();
                interfaces.add(templateGapName);
                String infoType;
                List<Field> fields;
                if (ast.isDescendent(exprNode, nodeClass)) {
                    infoType = "ExprInfo";
                    fields = TemplateGapNodeCreator.TEMPLATEGAPEXPRFIELDS;
                } else if (ast.isDescendent(typeNode, nodeClass)) {
                    infoType = "TypeInfo";
                    fields = TemplateGapNodeCreator.TEMPLATEGAPTYPEFIELDS;
                } else {
                    infoType = "ASTNodeInfo";
                    fields = TemplateGapNodeCreator.TEMPLATEGAPFIELDS;
                }
                TypeName superName = Types.parse(nodeClass.name(), ast);
                NodeType templateGap = new TemplateGapClass("TemplateGap" + nodeClass.name(), fields, superName, interfaces, infoType);
                templateGaps.add(new Pair<NodeType, NodeType>(templateGap, nodeClass));
            }
        }
        for (Pair<NodeType, NodeType> p : templateGaps) {
            ast.addType(p.first(), false, p.second());
        }
    }

    @Override
    public void generateAdditionalCode() {
        // TODO Auto-generated method stub
    }

    @Override
    public void generateClassMembers(TabPrintWriter arg0, NodeClass arg1) {
        // TODO Auto-generated method stub
    }

    @Override
    public void generateInterfaceMembers(TabPrintWriter arg0, NodeInterface arg1) {
        // TODO Auto-generated method stub
    }
}
