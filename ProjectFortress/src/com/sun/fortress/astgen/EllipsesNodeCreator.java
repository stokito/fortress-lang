/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.astgen;

import edu.rice.cs.astgen.*;
import edu.rice.cs.plt.tuple.Pair;

import java.util.LinkedList;
import java.util.List;

public class EllipsesNodeCreator extends CodeGenerator implements Runnable {

    public EllipsesNodeCreator(ASTModel ast) {
        super(ast);
    }

    @Override
    public Iterable<Class<? extends CodeGenerator>> dependencies() {
        return new LinkedList<Class<? extends CodeGenerator>>();
    }

    public void run() {
        List<Pair<NodeType, NodeType>> all = new LinkedList<Pair<NodeType, NodeType>>();
        NodeType abstractNode;
        NodeType exprNode;
        NodeType typeNode;
        if (ast.typeForName("AbstractNode").isSome() && ast.typeForName("Expr").isSome() && ast.typeForName("Type").isSome()) {
            abstractNode = ast.typeForName("AbstractNode").unwrap();
            exprNode = ast.typeForName("Expr").unwrap();
            typeNode = ast.typeForName("Type").unwrap();
        } else throw new RuntimeException("Fortress.ast does not define AbstractNode/Expr/Type!");

        abstractNode = ast.typeForName("AbstractNode").unwrap();
        for (NodeType n : ast.classes()) {
            if (n.getClass() == NodeClass.class && ast.isDescendent(abstractNode, n) && !n.name().startsWith("_SyntaxTransformation") && !n.name().startsWith("TemplateGap")) {

                String infoType;
                if (ast.isDescendent(exprNode, n)) infoType = "ExprInfo";
                else if (ast.isDescendent(typeNode, n)) infoType = "TypeInfo";
                else infoType = "ASTNodeInfo";

                NodeType child = new EllipsesNode((NodeClass) n, ast, infoType);
                all.add(new Pair<NodeType, NodeType>(child, n));
            }
        }
        for (Pair<NodeType, NodeType> p : all) {
            ast.addType(p.first(), false, p.second());
        }
    }

    @Override
    public void generateAdditionalCode() {
    }

    @Override
    public void generateClassMembers(TabPrintWriter arg0, NodeClass arg1) {
    }

    @Override
    public void generateInterfaceMembers(TabPrintWriter arg0, NodeInterface arg1) {
    }
}
