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
import java.util.ArrayList;
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

public class TransformationNodeCreator extends CodeGenerator implements Runnable {

    public TransformationNodeCreator(ASTModel ast) {
        super(ast);
    }
    
    @Override
    public Iterable<Class<? extends CodeGenerator>> dependencies() {
        return new LinkedList<Class<? extends CodeGenerator>>();
    }

    public void run() {
        List<Pair<NodeType, NodeType>> all = new LinkedList<Pair<NodeType, NodeType>>();
        for ( NodeType n : ast.classes() ){
            NodeType child = createNode(n);
            all.add( new Pair<NodeType,NodeType>( child, n ) );
        }
        for (Pair<NodeType, NodeType> p: all) {
            ast.addType( p.first(), false, p.second() );
        }
    }

    private NodeType createNode( NodeType parent ){
        List<Field> fields = new ArrayList<Field>();
        TypeName transformType = Types.parse( "com.sun.fortress.syntax_abstractions.phases.SyntaxTransformer", ast );
        fields.add( new Field( transformType, "transformation", Option.<String>none(), false, true, true ) );
        List<TypeName> interfaces = new ArrayList<TypeName>();
        interfaces.add( Types.parse("_SyntaxTransformation", ast ) );
        return new NodeClass( "_SyntaxTransformation" + parent.name(), false, fields, Types.parse( parent.name(), ast ), interfaces );
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
