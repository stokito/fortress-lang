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

package com.sun.fortress.astgen;

import java.util.LinkedList;
import java.util.List;

import edu.rice.cs.astgen.ASTModel;
import edu.rice.cs.astgen.CodeGenerator;
import edu.rice.cs.astgen.Field;
import edu.rice.cs.astgen.NodeClass;
import edu.rice.cs.astgen.NodeInterface;
import edu.rice.cs.astgen.NodeType;
import edu.rice.cs.astgen.TabPrintWriter;
import edu.rice.cs.astgen.Types;
import edu.rice.cs.astgen.Types.TypeName;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.Pair;

public class TemplateGapNodeCreator extends CodeGenerator implements Runnable {

    private TypeName idType = Types.parse("Id", ast);
    private TypeName listIdType = Types.parse("List<Id>", ast);

    public static List<Field> TEMPLATEGAPFIELDS; {
        TEMPLATEGAPFIELDS = new LinkedList<Field>();
        TypeName infoType = Types.parse("ASTNodeInfo", ast);
        TEMPLATEGAPFIELDS.add(new Field(infoType , "info", Option.<String>some("NodeFactory.makeASTNodeInfo()"), false, true, true));
        TEMPLATEGAPFIELDS.add(new Field(idType , "gapId", Option.<String>none(), false, false, true));
        TEMPLATEGAPFIELDS.add(new Field(listIdType , "templateParams", Option.<String>none(), false, false, true));
    }

    public static List<Field> TEMPLATEGAPEXPRFIELDS; {
        TEMPLATEGAPEXPRFIELDS = new LinkedList<Field>();
        TypeName infoType = Types.parse("ExprInfo", ast);
        TEMPLATEGAPEXPRFIELDS.add(new Field(infoType , "info", Option.<String>some("NodeFactory.makeExprInfo()"), false, true, true));
        TEMPLATEGAPEXPRFIELDS.add(new Field(idType , "gapId", Option.<String>none(), false, false, true));
        TEMPLATEGAPEXPRFIELDS.add(new Field(listIdType , "templateParams", Option.<String>none(), false, false, true));
    }

    public static List<Field> TEMPLATEGAPTYPEFIELDS; {
        TEMPLATEGAPTYPEFIELDS = new LinkedList<Field>();
        TypeName infoType = Types.parse("TypeInfo", ast);
        TEMPLATEGAPTYPEFIELDS.add(new Field(infoType , "info", Option.<String>some("NodeFactory.makeTypeInfo()"), false, true, true));
        TEMPLATEGAPTYPEFIELDS.add(new Field(idType , "gapId", Option.<String>none(), false, false, true));
        TEMPLATEGAPTYPEFIELDS.add(new Field(listIdType , "templateParams", Option.<String>none(), false, false, true));
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
        if ( ast.typeForName("AbstractNode").isSome() &&
             ast.typeForName("Expr").isSome() &&
             ast.typeForName("Type").isSome() ) {
            abstractNode = ast.typeForName("AbstractNode").unwrap();
            exprNode     = ast.typeForName("Expr").unwrap();
            typeNode     = ast.typeForName("Type").unwrap();
        } else
            throw new RuntimeException("Fortress.ast does not define AbstractNode/Expr/Type!");
        for (NodeClass nodeClass: ast.classes()) {
            if ( nodeClass.getClass() == NodeClass.class &&
                 ast.isDescendent(abstractNode, nodeClass) ){
                List<TypeName> interfaces = new LinkedList<TypeName>();
                interfaces.add(templateGapName);
                String infoType;
                List<Field> fields;
                if ( ast.isDescendent(exprNode, nodeClass) ) {
                    infoType = "ExprInfo";
                    fields = TemplateGapNodeCreator.TEMPLATEGAPEXPRFIELDS;
                } else if ( ast.isDescendent(typeNode, nodeClass) ) {
                    infoType = "TypeInfo";
                    fields = TemplateGapNodeCreator.TEMPLATEGAPTYPEFIELDS;
                } else {
                    infoType = "ASTNodeInfo";
                    fields = TemplateGapNodeCreator.TEMPLATEGAPFIELDS;
                }
                TypeName superName = Types.parse(nodeClass.name(), ast);
                NodeType templateGap = new TemplateGapClass("TemplateGap"+nodeClass.name(),
                                                            fields, superName, interfaces,
                                                            infoType);
                templateGaps.add(new Pair<NodeType, NodeType>(templateGap, nodeClass));
	    }
        }
        for (Pair<NodeType, NodeType> p: templateGaps) {
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
