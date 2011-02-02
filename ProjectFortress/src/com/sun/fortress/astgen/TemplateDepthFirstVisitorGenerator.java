/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.astgen;

import edu.rice.cs.astgen.*;
import edu.rice.cs.astgen.DepthFirstVisitorGenerator;
import edu.rice.cs.astgen.Types.TypeName;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TemplateDepthFirstVisitorGenerator extends DepthFirstVisitorGenerator {

    public TemplateDepthFirstVisitorGenerator(ASTModel ast) {
        super(ast);
    }

    protected void generateVisitor(NodeType root) {
        String visitorName = "Template" + root.name() + "DepthFirstVisitor";
        TabPrintWriter writer = options.createJavaSourceInOutDir(visitorName);
        Map<Class<?>, TemplateGapClass> nodeClasses = new HashMap<Class<?>, TemplateGapClass>();
        Map<Class<?>, TransformationNode> nodeClassesTransformation = new HashMap<Class<?>, TransformationNode>();
        Map<Class<?>, EllipsesNode> nodeClassesEllipses = new HashMap<Class<?>, EllipsesNode>();

        // Class header
        writer.startLine("/** A parametric abstract implementation of a visitor over " + root.name());
        writer.print(" that returns a value.");
        writer.startLine(" ** This visitor implements the visitor interface with methods that ");
        writer.startLine(" ** first visit children, and then call forCASEOnly(), passing in ");
        writer.startLine(" ** the values of the visits of the children. (CASE is replaced by the case name.)");
        writer.startLine(" ** By default, each of forCASEOnly delegates to a more general case; at the");
        writer.startLine(" ** top of this delegation tree is defaultCase(), which (unless overridden)");
        writer.startLine(" ** throws an exception.");
        writer.startLine(" **/");
        writer.startLine("@SuppressWarnings(value={\"unused\"})");
        writer.startLine("public abstract class " + visitorName + "<RetType>");
        if (options.usePLT) {
            writer.print(" extends " + root.name() + "VisitorLambda<RetType>");
        } else {
            writer.print(" implements " + root.name() + "Visitor<RetType>");
        }
        writer.print(" {");
        writer.indent();

        outputDefaultCaseMethod(writer, root);
        writer.println();

        // Write out forCASEOnly methods
        writer.startLine("/* Methods to handle a node after recursion. */");
        for (NodeType t : ast.descendents(root)) {
            if (t instanceof TemplateGapClass) {
                if (!nodeClasses.containsKey(t.getClass())) {
                    nodeClasses.put(t.getClass(), (TemplateGapClass) t);
                }
                outputNonstandardClassesForCaseOnly((TemplateGapClass) t, writer, root);
            } else if (t instanceof TransformationNode) {
                if (!nodeClassesTransformation.containsKey(t.getClass())) {
                    nodeClassesTransformation.put(t.getClass(), (TransformationNode) t);
                }
                outputNonstandardClassesForCaseOnly((TransformationNode) t, writer, root);
            } else if (t instanceof EllipsesNode) {
                if (!nodeClassesEllipses.containsKey(t.getClass())) {
                    nodeClassesEllipses.put(t.getClass(), (EllipsesNode) t);
                }
                outputNonstandardClassesForCaseOnly((EllipsesNode) t, writer, root);
            } else {
                outputForCaseOnly(t, writer, root);
            }
        }
        writer.println();

        // Write implementation of visit methods
        writer.startLine("/** Methods to recur on each child. */");
        for (NodeType t : ast.descendents(root)) {
            if (!t.isAbstract()) {
                outputVisitMethod(t, writer, root);
            }
        }

        // Write implementation of default cases for nonstandard classes
        writer.startLine("/** Defaultcases for nonstandard classes. */");
        for (TemplateGapClass t : nodeClasses.values()) {
            outputNonstandardDefaultCaseMethod(writer, t);
        }
        for (TransformationNode t : nodeClassesTransformation.values()) {
            outputNonstandardDefaultCaseMethod(writer, t);
        }
        for (EllipsesNode t : nodeClassesEllipses.values()) {
            outputNonstandardDefaultCaseMethod(writer, t);
        }

        writer.println();
        outputRecurMethod(writer, root, "RetType");

        // Output helpers, if necessary
        for (TypeName t : helpers()) {
            writer.println();
            generateHelper(t, writer, root);
        }
        clearHelpers();

        // output array seeds, if necessary
        for (TypeName t : this.arraySeeds()) {
            writer.println();
            generateArraySeed(t, writer);
        }
        clearArraySeeds();

        writer.unindent();
        writer.startLine("}");
        writer.println();
        writer.close();
    }

    private void outputNonstandardDefaultCaseMethod(TabPrintWriter writer, TemplateGapClass t) {
        writer.startLine("/**");
        writer.startLine(" * This method is run for all cases that are not handled elsewhere.");
        writer.startLine(" * By default, an exception is thrown; subclasses may override this behavior.");
        writer.startLine(" * @throws IllegalArgumentException");
        writer.startLine("**/");
        writer.startLine("public RetType defaultTemplateGapCase(TemplateGap that) {");
        writer.indent();
        writer.startLine("throw new IllegalArgumentException(\"Visitor \" + getClass().getName()");
        writer.print(" + \" does not support visiting values of type \" + that.getClass().getName());");
        writer.unindent();
        writer.startLine("}");
    }

    private void outputNonstandardDefaultCaseMethod(TabPrintWriter writer, TransformationNode t) {
        writer.startLine("/**");
        writer.startLine(" * This method is run for all cases that are not handled elsewhere.");
        writer.startLine(" * By default, an exception is thrown; subclasses may override this behavior.");
        writer.startLine(" * @throws IllegalArgumentException");
        writer.startLine("**/");
        writer.startLine("public RetType defaultTransformationNodeCase(_SyntaxTransformation that) {");
        writer.indent();
        writer.startLine("throw new IllegalArgumentException(\"Visitor \" + getClass().getName()");
        writer.print(" + \" does not support visiting values of type \" + that.getClass().getName());");
        writer.unindent();
        writer.startLine("}");
    }

    private void outputNonstandardDefaultCaseMethod(TabPrintWriter writer, EllipsesNode t) {
        writer.startLine("/**");
        writer.startLine(" * This method is run for all cases that are not handled elsewhere.");
        writer.startLine(" * By default, an exception is thrown; subclasses may override this behavior.");
        writer.startLine(" * @throws IllegalArgumentException");
        writer.startLine("**/");
        writer.startLine("public RetType defaultEllipsesNodeCase(_Ellipses that) {");
        writer.indent();
        writer.startLine("throw new IllegalArgumentException(\"Visitor \" + getClass().getName()");
        writer.print(" + \" does not support visiting values of type \" + that.getClass().getName());");
        writer.unindent();
        writer.startLine("}");
    }

    /**
     * Direct all calls to the default case for this non standard type of classes
     *
     * @param t
     * @param writer
     * @param root
     */
    protected void outputNonstandardClassesForCaseOnly(TemplateGapClass t, TabPrintWriter writer, NodeType root) {
        List<String> recurDecls = new LinkedList<String>();
        for (Field f : t.allFields(ast)) {
            if (canRecurOn(f.type(), root)) {
                recurDecls.add(resultType(f.type()).name() + " " + f.name() + "_result");
            }
        }
        outputForCaseHeader(t, writer, "RetType", "Only", recurDecls);
        writer.indent();
        writer.startLine("return defaultTemplateGapCase(that);");
        writer.unindent();
        writer.startLine("}");
        writer.println();
    }

    /**
     * Direct all calls to the default case for this non standard type of classes
     *
     * @param t
     * @param writer
     * @param root
     */
    protected void outputNonstandardClassesForCaseOnly(TransformationNode t, TabPrintWriter writer, NodeType root) {
        List<String> recurDecls = new LinkedList<String>();
        for (Field f : t.allFields(ast)) {
            if (canRecurOn(f.type(), root)) {
                recurDecls.add(resultType(f.type()).name() + " " + f.name() + "_result");
            }
        }
        outputForCaseHeader(t, writer, "RetType", "Only", recurDecls);
        writer.indent();
        writer.startLine("return defaultTransformationNodeCase(that);");
        writer.unindent();
        writer.startLine("}");
        writer.println();
    }

    /**
     * Direct all calls to the default case for this non standard type of classes
     *
     * @param t
     * @param writer
     * @param root
     */
    protected void outputNonstandardClassesForCaseOnly(EllipsesNode t, TabPrintWriter writer, NodeType root) {
        List<String> recurDecls = new LinkedList<String>();
        for (Field f : t.allFields(ast)) {
            if (canRecurOn(f.type(), root)) {
                recurDecls.add(resultType(f.type()).name() + " " + f.name() + "_result");
            }
        }
        outputForCaseHeader(t, writer, "RetType", "Only", recurDecls);
        writer.indent();
        writer.startLine("return defaultEllipsesNodeCase(that);");
        writer.unindent();
        writer.startLine("}");
        writer.println();
    }

}
