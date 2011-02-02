/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.astgen;

import edu.rice.cs.astgen.*;
import edu.rice.cs.astgen.Types.TypeName;
import edu.rice.cs.plt.tuple.Option;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class TemplateGapClass extends NodeClass {
    //    private TypeName _superClass;
    private String infoType;

    public TemplateGapClass(String name, List<Field> fields, TypeName superClass, List<TypeName> interfaces, String in_infoType) {
        super(name, false, fields, superClass, interfaces);
        infoType = in_infoType;
        //        _superClass = superClass;
    }

    //    public boolean isAbstract() { return false; }
    //    public TypeName superClass() { return _superClass; }

    /**
     * The fields that are declared by this class (rather than being inherited).  These include
     * all fields that are not declared by a parent NodeClass, plus any fields inherited from an
     * interface parent.
     */
    public Iterable<Field> declaredFields(ASTModel ast) {
        Option<NodeType> parent = ast.parent(this);
        if (parent.isSome() && parent.unwrap() instanceof NodeClass) {
            NodeType parentType = parent.unwrap();
            List<Field> result = new LinkedList<Field>();
            for (Field f : _fields) {
                Option<Field> supF = parentType.fieldForName(f.name(), ast);
                if (supF.isNone()) {
                    result.add(f);
                }
            }
            return result;
        }
        // may include fields in a parent interface
        if (infoType.equals("ExprInfo")) return TemplateGapNodeCreator.TEMPLATEGAPEXPRFIELDS;
        else if (infoType.equals("TypeInfo")) return TemplateGapNodeCreator.TEMPLATEGAPTYPEFIELDS;
        else return TemplateGapNodeCreator.TEMPLATEGAPFIELDS;
    }

    public void output(ASTModel ast, Iterable<CodeGenerator> gens) {
        TabPrintWriter writer = ast.options().createJavaSourceInOutDir(this.name());

        // Class header
        writer.startLine("/**");
        writer.startLine(" * Template gap class" + _name + ", a template gap of the ");
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
        writer.print(" implements TemplateGap");
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


    private void writeGetters(TabPrintWriter writer, String name) {
        writer.startLine("final public Id getGapId() { return _id; }");
        writer.startLine("final public List<Id> getTemplateParams() { return _templateParams; }");
        writer.println();
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

        /* this should probably be 'return visitor.for...' */
        writer.startLine("public Node accept(TemplateUpdateVisitor visitor) {");
        writer.indent();
        writer.startLine("return visitor.for" + name + "(this);");
        writer.unindent();
        writer.startLine("}");
    }

    private void writeFields(TabPrintWriter writer) {
        writer.startLine("private final Id _id;");
        writer.startLine("private final List<Id> _templateParams;");
    }

    private void writeConstructor(TabPrintWriter writer, String className) {
        writer.startLine("public " + className + "(" + infoType + " info, Id id, List<Id> templateParams) {");
        writer.indent();
        writer.startLine("super(info);");
        writer.startLine("this._id = id;");
        writer.startLine("this._templateParams = templateParams;");
        writer.unindent();
        writer.startLine("}");
    }

    private void writeEmptyConstructor(TabPrintWriter writer, String className) {
        writer.startLine("public " + className + "() {");
        writer.indent();
        writer.startLine("super(NodeFactory.make" + infoType + "(NodeFactory.macroSpan));");
        writer.startLine("this._id = null;");
        writer.startLine("this._templateParams = null;");
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
        writer.startLine("TemplateGap casted = (TemplateGap) obj;");
        writer.startLine("Id temp_id = getGapId();");
        writer.startLine("Id casted_id = casted.getGapId();");
        writer.startLine("if (!(temp_id == casted_id || temp_id.equals(casted_id))) return false;");
        writer.startLine("List<Id> temp_templateParams = getTemplateParams();");
        writer.startLine("List<Id> casted_templateParams = casted.getTemplateParams();");
        writer.startLine("if (!(temp_templateParams == casted_templateParams || temp_templateParams.equals(casted_templateParams))) return false;");
        writer.startLine("return true;");
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
        writer.startLine("Id temp_id = getGapId();");
        writer.startLine("code ^= temp_id.hashCode();");
        writer.startLine("List<Id> temp_templateParams = getTemplateParams();");
        writer.startLine("code ^= temp_templateParams.hashCode();");
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
        writer.startLine("if (w.visitNode(this, \"" + name + "\", 3)) {");
        writer.indent();

        writer.startLine(infoType + " temp_info = getInfo();");
        writer.startLine("if (w.visitNodeField(\"info\", temp_info)) {");
        writer.indent();
        writer.startLine("temp_info.walk(w);");
        writer.startLine("w.endNodeField(\"info\", temp_info);");
        writer.unindent();
        writer.startLine("}");

        writer.startLine("Id temp_id = getGapId();");
        writer.startLine("if (w.visitNodeField(\"gapId\", temp_id)) {");
        writer.indent();
        writer.startLine("temp_id.walk(w);");
        writer.startLine("w.endNodeField(\"gapId\", temp_id);");
        writer.unindent();
        writer.startLine("}");

        writer.startLine("List<Id> temp_templateParams = getTemplateParams();");
        writer.startLine("if (w.visitNodeField(\"templateParams\", temp_templateParams)) {");
        writer.indent();
        writer.startLine("if (w.visitIterated(temp_templateParams)) {");
        writer.indent();
        writer.startLine("int i_temp_templateParams = 0;");
        writer.startLine("for (Id elt_temp_templateParams : temp_templateParams) {");
        writer.indent();
        writer.startLine("if (w.visitIteratedElement(i_temp_templateParams, elt_temp_templateParams)) {");
        writer.indent();
        writer.startLine("if (elt_temp_templateParams == null) w.visitNull();");
        writer.startLine("else elt_temp_templateParams.walk(w);");
        writer.startLine("w.endIteratedElement(i_temp_templateParams, elt_temp_templateParams);");
        writer.unindent();
        writer.startLine("}");
        writer.startLine("i_temp_templateParams++;");
        writer.unindent();
        writer.startLine("}");
        writer.startLine("w.endIterated(temp_templateParams, i_temp_templateParams);");
        writer.unindent();
        writer.startLine("}");
        writer.startLine("w.endNodeField(\"templateParams\", temp_templateParams);");
        writer.unindent();
        writer.startLine("}");

        writer.startLine("w.endNode(this, \"" + name + "\", 3);");
        writer.unindent();
        writer.startLine("}");
        writer.unindent();
        writer.startLine("}");
    }

    @Override
    public Iterable<Field> allFields(ASTModel ast) {
        if (infoType.equals("ExprInfo")) return TemplateGapNodeCreator.TEMPLATEGAPEXPRFIELDS;
        else if (infoType.equals("TypeInfo")) return TemplateGapNodeCreator.TEMPLATEGAPTYPEFIELDS;
        else return TemplateGapNodeCreator.TEMPLATEGAPFIELDS;
    }

    public String getKindName(ASTModel ast) {
        return "TemplateGap";
    }
}
