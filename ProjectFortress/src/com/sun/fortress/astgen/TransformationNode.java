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

import java.util.Date;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

import edu.rice.cs.astgen.ASTModel;
import edu.rice.cs.astgen.CodeGenerator;
import edu.rice.cs.astgen.Field;
import edu.rice.cs.astgen.NodeClass;
import edu.rice.cs.astgen.NodeType;
import edu.rice.cs.astgen.TabPrintWriter;
import edu.rice.cs.astgen.Types;
import edu.rice.cs.astgen.Types.TypeName;
import edu.rice.cs.plt.tuple.Option;

public class TransformationNode extends NodeClass {

    /* the field that corresponds to the name of the transformer function. */
    private final String fieldTransformer = "syntaxTransformer";

    public TransformationNode( NodeClass parent, ASTModel ast ){
        super( "_SyntaxTransformation" + parent.name(), false, fields(ast), Types.parse(parent.name(), ast), Collections.singletonList(Types.parse("_SyntaxTransformation",ast)) );
    }

    /**
     * The fields that are declared by this class (rather than being inherited).  These include
     * all fields that are not declared by a parent NodeClass, plus any fields inherited from an
     * interface parent.
     */
    public Iterable<Field> declaredFields(ASTModel ast) {
        return fields(ast);
    }
        
    private static List<Field> fields( ASTModel ast ){
        /*
        List<Field> fields = new ArrayList<Field>();
        TypeName transformType = Types.parse( "com.sun.fortress.syntax_abstractions.phases.SyntaxTransformer", ast );
        fields.add( new Field( transformType, "transformation", Option.<String>none(), false, true, true ) );
        return fields;
        */
        return new ArrayList<Field>();
    }

    public void output(ASTModel ast, Iterable<CodeGenerator> gens) {
        TabPrintWriter writer = ast.options().createJavaSourceInOutDir(this.name());

        // Class header
        writer.startLine("/**");
        writer.startLine(" * Transformation class" + _name + ", a transformation of the ");
        writer.print("ASTGen-generated composite hierarchy.");

        if (!ast.options().allowNulls) {
            writer.startLine(" * Note: null is not allowed as a value for any field.");
        }

        writer.startLine(" * @version  Generated automatically by ASTGen at ");
        writer.print(new Date());
        writer.startLine(" */");        
        writer.startLine("@SuppressWarnings(value={\"all\"})");
        // Class header
        writer.startLine("public class " + this.name());
        writer.print(" extends " + this.superClass().name());
        writer.print(" implements _SyntaxTransformation");
        writer.print(" {");
        writer.indent();

        writer.println("");
        writeFields(writer);
        writer.println("");
        writeEmptyConstructor(writer, this.name());
        writer.println("");
        writeConstructor(writer, this.name());
        writer.println("");
        writeGetters(writer, this.name());
        writer.println("");
        writeAcceptors(ast, writer, this.name());
        writer.println("");
        writeEquals(writer);
        writer.println("");
        writeToString(writer);
        writer.println("");
        writeGenerateHashCode(writer);
        writer.println("");
        writeSerialize(writer);
        writer.println("");
        writeOutputHelp(writer, this.name());      

        //   for (CodeGenerator g : gens) { g.generateClassMembers(writer, this); }

        writer.unindent();
        writer.startLine("}");
        writer.println();
        writer.close();
    }

    private String getter( String s ){
        return "get" + s.substring(0,1).toUpperCase() + s.substring(1);
    }

    private void writeGetters(TabPrintWriter writer, String name) {
        writer.startLine(String.format("final public String %s(){ return _%s; }", getter(fieldTransformer), fieldTransformer ) );
        writer.startLine("final public java.util.Map<String,Object> getVariables(){ return _variables; }" );
        writer.startLine("final public java.util.List<String> getSyntaxParameters(){ return _syntaxParameters; }" );
        writer.println();
    }

    private void writeAcceptors(ASTModel ast, TabPrintWriter writer, String name) {

        for (NodeType t: ast.ancestorRoots(this)) {

            writer.startLine("public <RetType> RetType accept("+t.name()+"Visitor<RetType> visitor) {");
            writer.indent();
            writer.startLine("return visitor.for"+name+"(this);");
            writer.unindent();
            writer.startLine("}");

            writer.startLine("public void accept("+t.name()+"Visitor_void visitor) {");
            writer.indent();
            writer.startLine("visitor.for"+name+"(this);");
            writer.unindent();
            writer.startLine("}");            
        }

        writer.startLine("public Node accept(TemplateUpdateVisitor visitor) {");
        writer.indent();
        writer.startLine("return visitor.for"+name+"(this);");
        writer.unindent();
        writer.startLine("}");
    }

    private void writeFields(TabPrintWriter writer) {
        // writer.startLine("private final Span _span;");
        writer.startLine(String.format("private final String _%s;", fieldTransformer));
        writer.startLine("private final java.util.Map<String,Object> _variables;");
        writer.startLine("private final java.util.List<String> _syntaxParameters;");
    }

    private void writeConstructor(TabPrintWriter writer, String className) {
        writer.startLine(String.format("public %s(Span span, String %s, java.util.Map<String,Object> variables, java.util.List<String> syntaxParameters ) {", className, fieldTransformer));
        writer.indent();
        writer.startLine("super(span);");
        writer.startLine(String.format("this._%s = %s;", fieldTransformer, fieldTransformer));
        writer.startLine("this._variables = variables;");
        writer.startLine("this._syntaxParameters = syntaxParameters;");
        writer.unindent();
        writer.startLine("}");
    }

    private void writeEmptyConstructor(TabPrintWriter writer, String className) {
        writer.startLine("public " + className+"() {");
        writer.indent();
        writer.startLine("super(new Span());");
        writer.startLine(String.format("this._%s = null;", fieldTransformer));
        writer.startLine("this._variables = null;");
        writer.startLine("this._syntaxParameters = null;");
        writer.unindent();
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
        // writer.startLine("code ^= getTransformation().hashCode();");
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
        writer.startLine("writer.unindent();");
        writer.unindent();
        writer.startLine("}");
    }

    @Override
    public Iterable<Field> allFields(ASTModel ast) {
        return fields(ast);
    }

    public String getKindName(ASTModel ast) {
        return "Transformation";
    }
}
