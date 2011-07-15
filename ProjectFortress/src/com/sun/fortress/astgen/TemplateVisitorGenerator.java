/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.astgen;


import edu.rice.cs.astgen.*;
import edu.rice.cs.astgen.Types.TypeName;
import edu.rice.cs.astgen.UpdateVisitorGenerator;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class TemplateVisitorGenerator extends UpdateVisitorGenerator {

    private NodeType exprNode;
    private NodeType typeNode;

    public TemplateVisitorGenerator(ASTModel ast) {
        super(ast);
        if (ast.typeForName("Expr").isSome() && ast.typeForName("Type").isSome()) {
            exprNode = ast.typeForName("Expr").unwrap();
            typeNode = ast.typeForName("Type").unwrap();
        } else throw new RuntimeException("Fortress.ast does not define AbstractNode/Expr/Type!");
    }

    @Override
    protected void generateVisitor(NodeType root) {
        // only defined for the "Node" class
        if (!root.name().equals("Node")) {
            return;
        }

        String visitorName = "TemplateUpdateVisitor";
        TabPrintWriter writer = options.createJavaSourceInOutDir(visitorName);

        // Class header
        writer.startLine("/** ");
        writer.startLine(" * If a template node is visited then the handleTemplate method is called as the first thing of a visit.");
        writer.startLine(" * ");
        writer.startLine(" * A depth-first visitor that makes an updated copy as it visits (by default).");
        writer.startLine(" * The type of the result is generally the same as that of the argument; where");
        writer.startLine(" * automatic recursion on a field of type T occurs, this must be true for T.");
        writer.startLine(" * Where no changes are made to a node, a new copy is not allocated.");
        writer.startLine(" * This visitor implements the visitor interface with methods that ");
        writer.startLine(" * first update the children, and then call forCASEOnly(), passing in ");
        writer.startLine(" * the values of the updated children. (CASE is replaced by the case name.)");
        writer.startLine(" * Override forCASE or forCASEOnly if you want to transform an AST subtree.");
        writer.startLine(" * There is no automatic delegation to more general cases, because each concrete");
        writer.startLine(" * case has a default implementation.");
        writer.startLine(" */");
        writer.startLine("@SuppressWarnings(value={\"unused\"})");
        writer.startLine("public abstract class " + visitorName);

        if (options.usePLT) {
            writer.print(" extends " + root.name() + "VisitorLambda<" + root.name() + ">");
        } else {
            writer.print(" implements " + root.name() + "Visitor<" + root.name() + ">");
        }

        writer.print(" {");
        writer.indent();
        writer.println();


        writer.startLine("/* Methods to handle a node after recursion. */");
        for (NodeType t : ast.descendents(root)) {
            if (!t.isAbstract()) {
                writer.println();
                if (t.name().startsWith("_SyntaxTransformation")) {
                    outputForTransformationCaseOnly(t, writer, root);
                } else if (t instanceof EllipsesNode) {
                    outputForEllipsesCaseOnly((EllipsesNode) t, writer, root);
                } else {
                    outputForCaseOnly(t, writer, root);
                }
            }
            //          if (t instanceof NodeClass) {
            //              outputTemplateForCaseOnly(t, writer, root);
            //          }
        }
        writer.println();

        writer.startLine("/** Methods to recur on each child. */");
        for (NodeType t : ast.descendents(root)) {
            if (!t.isAbstract()) {
                writer.println();
                outputVisitMethod(t, writer, root);
            }
            //          if (t instanceof NodeClass) {
            //              outputTemplateVisitMethod(t, writer, root);
            //          }
        }

        NodeType templateGapInterface = null;
        for (NodeInterface ni : ast.interfaces()) {
            if (ni.name().equals("TemplateGap")) {
                templateGapInterface = ni;
            }
        }
        outputForTemplateGapOnly(templateGapInterface, writer, root);

        writer.println();
        outputRecurMethod(writer, root, root.name());
        writer.println();

        outputTransformationDefaultCase(writer, root);
        writer.println();
        outputEllipsesDefaultCase(writer, root);

        // Output helpers
        for (TypeName t : helpers()) {
            writer.println();
            generateHelper(t, writer, root);
        }

        writer.unindent();
        writer.startLine("}");
        writer.println();
        writer.close();
    }

    protected void outputForTransformationCaseOnly(NodeType node, TabPrintWriter writer, NodeType root) {
        List<String> params = new ArrayList<String>();
        /*
        for (Field f : node.allFields(ast)) {
            params.add(f.name());
        }
        */
        outputForCaseHeader(node, writer, root.name(), "Only", params);
        writer.indent();
        writer.startLine("return defaultTransformationNodeCase(that);");
        writer.unindent();
        writer.startLine("}");
    }

    protected void outputForEllipsesCaseOnly(EllipsesNode node, TabPrintWriter writer, NodeType root) {
        List<String> params = new ArrayList<String>();
        outputForCaseHeader(node, writer, root.name(), "Only", params);
        writer.indent();
        writer.startLine("return defaultEllipsesNodeOnly(that);");
        writer.unindent();
        writer.startLine("}");
    }

    protected void outputTransformationDefaultCase(TabPrintWriter writer, NodeType root) {
        writer.startLine(String.format("public %s defaultTransformationNodeCase( _SyntaxTransformation s ){", root.name()));
        writer.indent();
        writer.startLine("return s;");
        writer.unindent();
        writer.startLine("}");
    }

    protected void outputEllipsesDefaultCase(TabPrintWriter writer, NodeType root) {
        writer.startLine(String.format("public %s defaultEllipsesNodeOnly( _Ellipses s ){", root.name()));
        writer.indent();
        writer.startLine("return s;");
        writer.unindent();
        writer.startLine("}");
    }

    @Override
    protected void outputVisitMethod(NodeType t, TabPrintWriter writer, NodeType root) {
        outputForCaseHeader(t, writer, root.name(), "");
        writer.indent();

        boolean isTemplateGap = false;
        for (TypeName typeName : t.interfaces()) {
            if (typeName.name().equals("TemplateGap")) {
                isTemplateGap = true;
            }
        }

        if (isTemplateGap) {
            List<String> recurVals = new LinkedList<String>();
            for (Field f : t.fields()) {
                Option<String> recur = recurExpression(f.type(), "that." + f.getGetterName() + "()", root, true);
                if (recur.isSome()) {
                    String recurName = f.name() + "_result";
                    writer.startLine(f.type().name() + " " + recurName + " = " + recur.unwrap() + ";");
                    recurVals.add(recurName);
                }
            }
            writer.startLine("return forTemplateGapOnly((TemplateGap) that");
            for (String recurVal : recurVals) {
                writer.print(", " + recurVal);
            }
            writer.print(");");
        } else {
            List<String> recurVals = new LinkedList<String>();
            for (Field f : t.allFields(ast)) {
                Option<String> recur = recurExpression(f.type(), "that." + f.getGetterName() + "()", root, true);
                if (recur.isSome()) {
                    String recurName = f.name() + "_result";
                    writer.startLine(f.type().name() + " " + recurName + " = " + recur.unwrap() + ";");
                    recurVals.add(recurName);
                }
            }
            writer.startLine("return " + visitorMethodName(t) + "Only(that");
            if (!t.name().startsWith("_SyntaxTransformation")) {
                for (String recurVal : recurVals) {
                    writer.print(", " + recurVal);
                }
            }
            writer.print(");");
        }
        writer.unindent();
        writer.startLine("}");
        writer.println();
    }

    protected void outputTemplateForCaseHeader(NodeType t, TabPrintWriter writer, String retType) {
        outputTemplateForCaseHeader(t.name(), writer, retType, "", IterUtil.<String>empty());
    }

    protected void outputTemplateForCaseHeader(String name, TabPrintWriter writer, String retType, String suff, Iterable<String> extraParams) {
        writer.startLine("public ");
        writer.print(retType);
        writer.print(" ");
        writer.print(visitorTemplateMethodName(name));
        writer.print(suff + "("); // Only(
        writer.print("TemplateGap" + name + " that");
        for (String p : extraParams) {
            writer.print(", " + p);
        }
        writer.print(") {");
    }

    protected String visitorTemplateMethodName(String name) {
        if (name.length() == 0) {
            return options.visitorMethodPrefix + "TemplateGap";
        }
        return options.visitorMethodPrefix + "TemplateGap" + upperCaseFirst(name);
    }

    protected void outputForTemplateGapOnly(NodeType t, TabPrintWriter writer, NodeType root) {
        // only called for concrete cases; must not delegate
        List<String> params = new LinkedList<String>(); // "type name" strings
        List<String> getters = new LinkedList<String>(); // expressions
        List<String> paramRefs = new LinkedList<String>(); // variable names or null
        List<Field> fields;
        if (ast.isDescendent(exprNode, root)) fields = TemplateGapNodeCreator.TEMPLATEGAPEXPRFIELDS;
        else if (ast.isDescendent(typeNode, root)) fields = TemplateGapNodeCreator.TEMPLATEGAPTYPEFIELDS;
        else fields = TemplateGapNodeCreator.TEMPLATEGAPFIELDS;
        for (Field f : fields) {
            getters.add("that." + f.getGetterName() + "()");
            if (canRecurOn(f.type(), root)) {
                String paramName = f.name() + "_result";
                params.add(f.type().name() + " " + paramName);
                paramRefs.add(paramName);
            } else {
                paramRefs.add(null);
            }
        }
        outputTemplateForCaseHeader("", writer, root.name(), "Only", params);
        writer.indent();
        writer.startLine("return that;");
        writer.unindent();
        writer.startLine("}");
    }

    @Override
    public Iterable<Class<? extends CodeGenerator>> dependencies() {
        List<Class<? extends CodeGenerator>> deps = new LinkedList<Class<? extends CodeGenerator>>();
        deps.add(TemplateGapNodeCreator.class);
        return deps;
    }

    @Override
    public void generateClassMembers(TabPrintWriter writer, NodeClass arg1) {
        if (arg1.name().startsWith("_SyntaxTransformation")) {
            writer.startLine("public Node accept(TemplateUpdateVisitor visitor) {");
            writer.indent();
            writer.startLine("return visitor.for" + arg1.name() + "(this);");
            writer.unindent();
            writer.startLine("}");
        }
    }
}
