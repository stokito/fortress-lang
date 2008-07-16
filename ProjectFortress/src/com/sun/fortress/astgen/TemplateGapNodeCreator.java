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

    public static List<Field> TEMPLATEGAPFIELDS; {
        TEMPLATEGAPFIELDS = new LinkedList<Field>();
        TypeName spanType = Types.parse("Span", ast);
        TEMPLATEGAPFIELDS.add(new Field(spanType , "span", Option.<String>some("new Span()"), false, true, true));
        TypeName idType = Types.parse("Id", ast);
        TEMPLATEGAPFIELDS.add(new Field(idType , "gapId", Option.<String>none(), false, false, true));
        TypeName listIdType = Types.parse("List<Id>", ast);
        TEMPLATEGAPFIELDS.add(new Field(listIdType , "templateParams", Option.<String>none(), false, false, true));
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
        for (NodeClass nodeClass: ast.classes()) {
            List<TypeName> interfaces = new LinkedList<TypeName>();
            interfaces.add(templateGapName);
            List<Field> fields = TemplateGapNodeCreator.TEMPLATEGAPFIELDS;
            TypeName superName = Types.parse(nodeClass.name(), ast);
            NodeType templateGap = new TemplateGapClass("TemplateGap"+nodeClass.name(), fields, superName, interfaces);
            templateGaps.add(new Pair<NodeType, NodeType>(templateGap, nodeClass));
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
