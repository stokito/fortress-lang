/*******************************************************************************
    Copyright 2009 Sun Microsystems, Inc.,
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
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

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

import static java.util.Arrays.asList;

public class SyntaxAbstractionCreator extends CodeGenerator implements Runnable {

    public SyntaxAbstractionCreator(ASTModel ast) {
        super(ast);
    }

    @Override
    public Iterable<Class<? extends CodeGenerator>> dependencies() {
        return new LinkedList<Class<? extends CodeGenerator>>();
    }

    public void run() {
        List<NodeType> all = new LinkedList<NodeType>();
        NodeType abstractNode;
        if (ast.typeForName("AbstractNode").isSome()){
            abstractNode = ast.typeForName("AbstractNode").unwrap();
        } else {
            throw new RuntimeException("Fortress.ast does not define AbstractNode/Expr/Type!");
        }

        abstractNode = ast.typeForName("AbstractNode").unwrap();

        for ( NodeType n : ast.classes() ){
            if ( n.getClass() == NodeClass.class &&
                 ast.isDescendent(abstractNode, n)){

                all.add(new SyntaxEllipsesNode((NodeClass) n, ast));
                all.add(new SyntaxTemplateGapNode((NodeClass) n, ast));
                all.add(new SyntaxTransformationNode((NodeClass) n, ast));
                /*
                NodeType child = new EllipsesNode((NodeClass) n,ast,infoType);
                all.add( new Pair<NodeType,NodeType>( child, n ) );
                */
            }
        }
        for (NodeType p : all) {
            // ast.addType( p.first(), false, p.second() );
            ast.addTopType(p, false);
        }
    }

    private static class SyntaxTransformationNode extends NodeClass {
        public SyntaxTransformationNode(NodeClass parent, ASTModel ast){
        super( "_SyntaxTransformation" + parent.name(), false, fields(ast), Types.parse(parent.name(), ast), Collections.singletonList(Types.parse("_SyntaxTransformation",ast)) );
        }

        public Iterable<Field> declaredFields(ASTModel ast) {
            return fields(ast);
        }

        private static List<Field> fields( ASTModel ast ){
            return asList(new Field(Types.parse("String", ast), "syntaxTransformer", Option.<String>none(), false, false, true)
                    );
        }
    }

    private static class SyntaxTemplateGapNode extends NodeClass {

        private static List<TypeName> implementing(NodeClass parent, ASTModel ast){
            List<TypeName> foo = new ArrayList<TypeName>();
            foo.add(Types.parse("TemplateGap",ast));
            foo.add(Types.parse(parent.name(), ast));
            return foo;
        }

        public SyntaxTemplateGapNode(NodeClass parent, ASTModel ast){
            super( "TemplateGap" + parent.name(), false, fields(ast), null, implementing(parent, ast));

        }

        private static List<Field> fields(ASTModel ast){
            return asList(
                    new Field(Types.parse("Id", ast), "gapId", Option.<String>none(), false, false, true),
                    new Field(Types.parse("List<Id>", ast), "templateParams", Option.<String>none(), false, false, true)
                    );
        }
    }

    private static class SyntaxEllipsesNode extends NodeClass {
        private String nodeField = "repeatedNode";
        private String infoType;

        private static List<TypeName> implementing(NodeClass parent, ASTModel ast){
            List<TypeName> foo = new ArrayList<TypeName>();
            foo.add(Types.parse("_Ellipses",ast));
            foo.add(Types.parse(parent.name(), ast));
            return foo;
        }

        public SyntaxEllipsesNode( NodeClass parent, ASTModel ast){
            super( "_Ellipses" + parent.name(), false, fields(ast), null, implementing(parent, ast));
        }

        @Override
        public Iterable<Field> declaredFields(ASTModel ast) {
            return fields(ast);
        }

        private static List<Field> fields( ASTModel ast ){
            // return new ArrayList<Field>();
            return Collections.<Field>singletonList(new Field(Types.parse("AbstractNode", ast), "repeatedNode", Option.<String>none(), false, false, true));
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
