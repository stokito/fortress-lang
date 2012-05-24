/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.astgen;

import edu.rice.cs.astgen.ASTModel;
import edu.rice.cs.astgen.DepthFirstVoidVisitorGenerator;
import edu.rice.cs.astgen.NodeType;
import edu.rice.cs.astgen.TabPrintWriter;
import edu.rice.cs.astgen.Types.TypeName;

public class TemplateDepthFirstVoidVisitorGenerator extends DepthFirstVoidVisitorGenerator {

    public TemplateDepthFirstVoidVisitorGenerator(ASTModel ast) {
        super(ast);
    }

    protected void generateVisitor(NodeType root) {
        String visitorName = "Template" + root.name() + "DepthFirstVisitor_void";
        TabPrintWriter writer = options.createJavaSourceInOutDir(visitorName);

        // Class header
        writer.startLine("/** An abstract implementation of a visitor over " + root.name());
        writer.print(" including template gaps that does not return a value.");
        writer.startLine(" ** This visitor implements the visitor interface with methods that ");
        writer.startLine(" ** first call forCASEDoFirst(), second visit the children, and finally ");
        writer.startLine(" ** call forCASEOnly().  (CASE is replaced by the case name.)");
        writer.startLine(" ** By default, each of forCASEDoFirst and forCASEOnly delegates");
        writer.startLine(" ** to a more general case.  At the top of this delegation tree are");
        writer.startLine(" ** defaultDoFirst() and defaultCase(), respectively, which (unless");
        writer.startLine(" ** overridden) are no-ops.");
        writer.startLine(" **/");
        writer.startLine("@SuppressWarnings(value={\"unused\"})");
        writer.startLine("public class " + visitorName);
        if (options.usePLT) {
            writer.print(" extends " + root.name() + "VisitorRunnable1");
        } else {
            writer.print(" implements " + root.name() + "Visitor_void");
        }
        writer.print(" {");
        writer.indent();

        outputDefaultCaseVoidMethod(writer, root);
        writer.println();

        writer.startLine("/**");
        writer.startLine(" * This method is run for all DoFirst cases that are not handled elsewhere.");
        writer.startLine(" * By default, it is a no-op; subclasses may override this behavior.");
        writer.startLine("**/");
        writer.startLine("public void defaultDoFirst(" + root.name() + " that) {");
        writer.print("}");
        writer.println();

        writer.startLine("/* Methods to handle a node before recursion. */");
        for (NodeType t : ast.descendents(root)) {
            outputDelegatingForCase(t, writer, root, "void", "DoFirst", "defaultDoFirst");
        }

        writer.startLine("/* Methods to handle a node after recursion. */");
        for (NodeType t : ast.descendents(root)) {
            outputDelegatingForCase(t, writer, root, "void", "Only", "defaultCase");
        }

        writer.startLine("/* Methods to recur on each child. */");
        for (NodeType t : ast.descendents(root)) {
            if (!t.isAbstract()) {
                outputVisitMethod(t, writer, root);
            }
        }

        writer.println();
        outputRecurMethod(writer, root, "void");

        // Output helpers
        for (TypeName t : helpers()) {
            writer.println();
            generateHelper(t, writer, root);
        }
        clearHelpers();

        writer.unindent();
        writer.startLine("}");
        writer.println();
        writer.close();
    }
}
