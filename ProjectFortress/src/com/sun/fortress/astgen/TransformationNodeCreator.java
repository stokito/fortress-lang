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
        /*
        System.out.println( "Created " + this.getClass().getName() );
        try{
            throw new RuntimeException( "blah" );
        } catch ( Exception e ){
            e.printStackTrace();
        }
        */
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
        return new NodeClass( "_SyntaxTransformation" + parent.name(), false, new ArrayList<Field>(), Types.parse( parent.name(), ast ), new ArrayList<TypeName>() );
    }

/*
      TypeName templateGapName = Types.parse("TemplateGap", ast);
      List<Pair<NodeType, NodeType>> templateGaps = new LinkedList<Pair<NodeType, NodeType>>();
      for (NodeClass nodeClass: ast.classes()) {
          List<TypeName> interfaces = new LinkedList<TypeName>();
          interfaces.add(templateGapName);
          List<Field> fields = new LinkedList<Field>();
          for (Field f: nodeClass.allFields(ast)) {
              fields.add(new Field(Types.parse("int", ast), f.name(), Option.some("42"), false, true, false));
          }
          TypeName idType = Types.parse("Id", ast);
          fields.add(new Field(idType , "id", Option.<String>none(), false, false, true));
          TypeName listIdType = Types.parse("List<Id>", ast);;
          fields.add(new Field(listIdType , "templateParams", Option.<String>none(), false, false, true));
          TypeName name = Types.parse(nodeClass.name(), ast);;
          NodeType templateGap = new TemplateGapClass("TemplateGap"+nodeClass.name(),fields , name , interfaces);
          templateGaps.add(new Pair<NodeType, NodeType>(templateGap, nodeClass));
      }
      for (Pair<NodeType, NodeType> p: templateGaps) {
          ast.addType(p.first(), false, p.second());
      }        
      */
    
    @Override
    public void generateAdditionalCode() {
        /*
      for (NodeClass nodeClass: ast.classes()) {
          generateTransformationNode(nodeClass);
      }      
      */
    }

    private void generateTransformationNode(NodeClass node) {
        String className = "_SyntaxTransformation"+node.name();
        TabPrintWriter writer = options.createJavaSourceInOutDir(className);

        // Class header
        writer.startLine("/** A TransformationNode node " + node.name());
        writer.startLine(" **/");
        writer.startLine("public class " + className);
        writer.print(" extends " + node.name());
        writer.print(" implements _SyntaxTransformation");
        writer.print(" {");
        writer.indent();
        
        writeFields(writer);
        writeConstructor(writer, className);
        writeGetters(writer, className);
        writeEquals(writer);
        writeToString(writer);
        writeGenerateHashCode(writer);
        writeSerialize(writer);
        writeOutputHelp(writer, className);      
        
        writer.unindent();
        writer.startLine("}");
        writer.println();
        writer.close();
    }

    private void writeGetters(TabPrintWriter writer, String name) {
        writer.startLine("public com.sun.fortress.syntax_abstractions.phases.SyntaxTransformer getTransformer() { return _transformer; }");

        /*
        writer.startLine("public <RetType> RetType accept(NodeVisitor<RetType> visitor) {");
        writer.indent();
        writer.startLine( "return this;" );
        writer.unindent();
        writer.startLine("}");

        writer.startLine("public <RetType> RetType accept(ExprVisitor<RetType> visitor) {");
        writer.indent();
        writer.startLine( "return this;" );
        writer.unindent();
        writer.startLine("}");
        
        writer.startLine("public <RetType> RetType accept(StaticArgVisitor<RetType> visitor) {");
        writer.indent();
        writer.startLine( "return this;" );
        writer.unindent();
        writer.startLine("}");
        
        writer.startLine("public <RetType> RetType accept(TypeVisitor<RetType> visitor) {");
        writer.indent();
        writer.startLine( exception );
        writer.unindent();
        writer.startLine("}");
        
        writer.startLine("public void accept(TypeVisitor_void visitor) {");
        writer.indent();
        writer.startLine( exception );
        writer.unindent();
        writer.startLine("}");
        
        writer.startLine("public void accept(ExprVisitor_void visitor) {");
        writer.indent();
        writer.startLine( exception );
        writer.unindent();
        writer.startLine("}");
        
        writer.startLine("public void accept(StaticArgVisitor_void visitor) {");
        writer.indent();
        writer.startLine( exception );
        writer.unindent();
        writer.startLine("}");

        writer.startLine("public void accept(NodeVisitor_void visitor) {");
        writer.indent();
        writer.startLine( exception );
        writer.unindent();
        writer.indent();
        // writer.startLine("visitor.for_SyntaxTransformationOnly(this);");
        writer.unindent();
        */
        writer.startLine("}");
    }

    private void writeFields(TabPrintWriter writer) {
        writer.startLine("private final com.sun.fortress.syntax_abstractions.phases.SyntaxTransformer _transformer;");
    }

    private void writeConstructor(TabPrintWriter writer, String className) {
        /*
        writer.startLine("public " + className+"(Id id, List<Id> templateParams) {");
        writer.indent();
        writer.startLine("this._id = id;");
        writer.startLine("this._templateParams = templateParams;");
        writer.unindent();
        writer.startLine("}");
        */
        writer.startLine("public " + className+"(com.sun.fortress.syntax_abstractions.phases.SyntaxTransformer transformer) {");
        writer.startLine("this._transformer = transformer;");
        writer.startLine("}");
    }

    private void writeEquals(TabPrintWriter writer) {
        writer.startLine("public boolean equals(java.lang.Object obj) {");
        writer.startLine("if (obj == null) return false;");
        writer.indent();
        writer.startLine("if ((obj.getClass() != this.getClass()) || (obj.hashCode() != this.hashCode())) {");
        writer.indent();
        writer.startLine("return false;");
        writer.unindent();
        writer.startLine("}");
        writer.startLine(" else {");
        writer.indent();
        /*
        writer.startLine("TemplateGapLink casted = (TemplateGapLink) obj;");
        writer.startLine("Id temp_id = getId();");
        writer.startLine("Id casted_id = casted.getId();");
        writer.startLine("if (!(temp_id == casted_id || temp_id.equals(casted_id))) return false;");
        writer.startLine("List<Id> temp_templateParams = getTemplateParams();");
        writer.startLine("List<Id> casted_templateParams = casted.getTemplateParams();");
        writer.startLine("if (!(temp_templateParams == casted_templateParams || temp_templateParams.equals(casted_templateParams))) return false;");
        writer.startLine("return true;");
        */
        writer.startLine("return false;");
        writer.unindent();
        writer.startLine("}");
        writer.unindent();
        writer.startLine("}");        
    }

    private void writeToString(TabPrintWriter writer) {
        // TODO Auto-generated method stub
        
    }

    private void writeGenerateHashCode(TabPrintWriter writer) {
        writer.startLine("/**");
        writer.startLine(" * Implementation of hashCode that is consistent with equals.  The value of");
        writer.startLine(" * the hashCode is formed by XORing the hashcode of the class object with");
        writer.startLine(" * the hashcodes of all the fields of the object.");
        writer.startLine(" */");
        writer.startLine("public int generateHashCode() {");
        writer.indent();
        writer.startLine("int code = getClass().hashCode();");
        writer.startLine("code ^= getTransformer().hashCode();");
        /*
        writer.startLine("List<Id> temp_templateParams = getTemplateParams();");
        writer.startLine("code ^= temp_templateParams.hashCode();");
        */
        writer.startLine("return code;");
        writer.unindent();
        writer.startLine("}");
    }

    private void writeSerialize(TabPrintWriter writer) {
        writer.startLine("/** Generate a human-readable representation that can be deserialized. */");
        writer.startLine("public java.lang.String serialize() {");
        writer.indent();
        writer.startLine("java.io.StringWriter w = new java.io.StringWriter();");
        writer.startLine("serialize(w);");
        writer.startLine("return w.toString();");
        writer.unindent();
        writer.startLine("}");
        writer.startLine("/** Generate a human-readable representation that can be deserialized. */");
        writer.startLine("public void serialize(java.io.Writer writer) {");
        writer.indent();
        writer.startLine("outputHelp(new TabPrintWriter(writer, 2), true);");
        writer.unindent();
        writer.startLine("}");
    }

    private void writeOutputHelp(TabPrintWriter writer, String name) {
        writer.startLine("public void outputHelp(TabPrintWriter writer, boolean lossless) {");
        /*
        writer.indent();
        writer.startLine("writer.print(\""+name+":\");");
        writer.startLine("writer.indent();");

        writer.startLine("Span temp_span = getSpan();");
        writer.startLine("writer.startLine();");
        writer.startLine("writer.print(\"span = \");");
        writer.startLine("if (lossless) {");
        writer.indent();
        writer.startLine("writer.printSerialized(temp_span);");
        writer.startLine("writer.print(\" \");");
        writer.startLine("writer.printEscaped(temp_span);");
        writer.unindent();
        writer.startLine("} else { writer.print(temp_span); }");

        writer.startLine("Id temp_id = getId();");
        writer.startLine("writer.startLine();");
        writer.startLine("writer.print(\"id = \");");
        writer.startLine("temp_id.outputHelp(writer, lossless);");

        writer.startLine("List<Id> temp_templateParams = getTemplateParams();");
        writer.startLine("writer.startLine();");
        writer.startLine("writer.print(\"templateParams = \");");
        writer.startLine("writer.print(\"{\");");
        writer.startLine("writer.indent();");
        writer.startLine("boolean isempty_temp_templateParams = true;");
        writer.startLine("for (Id elt_temp_templateParams : temp_templateParams) {");
        writer.indent();
        writer.startLine("isempty_temp_templateParams = false;");
        writer.startLine("writer.startLine(\"* \");");
        writer.startLine("if (elt_temp_templateParams == null) {");
        writer.indent();
        writer.startLine("writer.print(\"null\");");
        writer.unindent();
        writer.startLine("} else {");
        writer.indent();
        writer.startLine("elt_temp_templateParams.outputHelp(writer, lossless);");
        writer.unindent();
        writer.startLine("}");
        writer.unindent();
        writer.startLine("}");
        writer.startLine("writer.unindent();");
        writer.startLine("if (isempty_temp_templateParams) writer.print(\" }\");");
        writer.startLine("else writer.startLine(\"}\");");
        writer.startLine("writer.unindent();");
        writer.unindent();
        */
        writer.startLine("}");
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
