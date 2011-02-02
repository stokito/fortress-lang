/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.astgen;

import edu.rice.cs.astgen.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class EllipsesNode extends NodeClass {

    private String nodeField = "repeatedNode";
    private String infoType;

    public EllipsesNode(NodeClass parent, ASTModel ast, String in_infoType) {
        super("_Ellipses" + parent.name(), false, fields(ast), Types.parse(parent.name(), ast), Collections.singletonList(Types.parse("_Ellipses", ast)));
        infoType = in_infoType;
    }

    /**
     * The fields that are declared by this class (rather than being inherited).  These include
     * all fields that are not declared by a parent NodeClass, plus any fields inherited from an
     * interface parent.
     */
    public Iterable<Field> declaredFields(ASTModel ast) {
        return fields(ast);
    }

    private static List<Field> fields(ASTModel ast) {
        return new ArrayList<Field>();
    }

    public void output(ASTModel ast, Iterable<CodeGenerator> gens) {
        TabPrintWriter writer = ast.options().createJavaSourceInOutDir(this.name());

        // Class header
        writer.startLine("/**");
        writer.startLine(" * Ellipses class " + _name + ".");
        writer.print("ASTGen-generated composite hierarchy.");

        if (!ast.options().allowNulls) {
            writer.startLine(" * Note: null is not allowed as a value for any field.");
        }

        writer.startLine(" * @version  Generated automatically by ASTGen at ");
        writer.print(new Date());
        writer.startLine(" */");
        writer.startLine("@SuppressWarnings(value={\"unused\"})");
        // Class header
        writer.startLine("public class " + this.name());
        writer.print(" extends " + this.superClass().name());
        writer.print(" implements _Ellipses");
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
        writeWalk(writer, this.name());

        //   for (CodeGenerator g : gens) { g.generateClassMembers(writer, this); }

        writer.unindent();
        writer.startLine("}");
        writer.println();
        writer.close();
    }

    private String getter(String s) {
        return "get" + s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private void writeGetters(TabPrintWriter writer, String name) {
        writer.startLine(String.format("final public AbstractNode %s(){ return _%s; }", getter(nodeField), nodeField));
        /*
        writer.startLine(String.format("final public String %s(){ return _%s; }", getter(fieldTransformer), fieldTransformer ) );
        writer.startLine("final public java.util.Map<String,Object> getVariables(){ return _variables; }" );
        writer.startLine("final public java.util.List<String> getSyntaxParameters(){ return _syntaxParameters; }" );
        writer.println();
        */
    }

    private void writeAcceptors(ASTModel ast, TabPrintWriter writer, String name) {

        for (NodeType t : ast.ancestorRoots(this)) {

            writer.startLine("public <RetType> RetType accept(" + t.name() + "Visitor<RetType> visitor) {");
            writer.indent();
            writer.startLine("return visitor.for" + name + "(this);");
            writer.unindent();
            writer.startLine("}");

            writer.startLine("public void accept(" + t.name() + "Visitor_void visitor) {");
            writer.indent();
            writer.startLine("visitor.for" + name + "(this);");
            writer.unindent();
            writer.startLine("}");
        }

        writer.startLine("public Node accept(TemplateUpdateVisitor visitor) {");
        writer.indent();
        writer.startLine("return visitor.for" + name + "(this);");
        writer.unindent();
        writer.startLine("}");
    }

    private void writeFields(TabPrintWriter writer) {
        writer.startLine(String.format("private final AbstractNode _%s;", nodeField));
        /*
        writer.startLine(String.format("private final String _%s;", fieldTransformer));
        writer.startLine("private final java.util.Map<String,Object> _variables;");
        writer.startLine("private final java.util.List<String> _syntaxParameters;");
        */
    }

    private void writeConstructor(TabPrintWriter writer, String className) {
        writer.startLine(String.format("public %s(" + infoType + " info, AbstractNode %s){", className, nodeField));
        writer.indent();
        writer.startLine("super(info);");
        writer.startLine(String.format("this._%s = %s;", nodeField, nodeField));
        writer.unindent();
        writer.startLine("}");
    }

    private void writeEmptyConstructor(TabPrintWriter writer, String className) {
        writer.startLine("public " + className + "() {");
        writer.indent();
        writer.startLine("super(NodeFactory.make" + infoType + "(NodeFactory.macroSpan));");
        writer.startLine(String.format("this._%s = null;", nodeField));
        /*
        writer.startLine(String.format("this._%s = null;", fieldTransformer));
        writer.startLine("this._variables = null;");
        writer.startLine("this._syntaxParameters = null;");
        */
        writer.unindent();
        writer.startLine("}");
    }

    private void writeEquals(TabPrintWriter writer) {
        writer.startLine("public boolean equals(Object obj) {");
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
        writer.startLine("walk(new LosslessStringWalker(writer, 2));");
        writer.unindent();
        writer.startLine("}");
    }

    private void writeWalk(TabPrintWriter writer, String name) {
        writer.startLine("public void walk(TreeWalker w) {");
        writer.indent();
        writer.startLine("if (w.visitNode(this, \"" + name + "\", 1)) {");
        writer.indent();

        writer.startLine(infoType + " temp_info = getInfo();");
        writer.startLine("if (w.visitNodeField(\"info\", temp_info)) {");
        writer.indent();
        writer.startLine("w.visitUnknownObject(temp_info);");
        writer.startLine("w.endNodeField(\"info\", temp_info);");
        writer.unindent();
        writer.startLine("}");

        writer.startLine("w.endNode(this, \"" + name + "\", 1);");
        writer.unindent();
        writer.startLine("}");
        writer.unindent();
        writer.startLine("}");
    }

    @Override
    public Iterable<Field> allFields(ASTModel ast) {
        return fields(ast);
    }

    public String getKindName(ASTModel ast) {
        return "Ellipses";
    }
}
