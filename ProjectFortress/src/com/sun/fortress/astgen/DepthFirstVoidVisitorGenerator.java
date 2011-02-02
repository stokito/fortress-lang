/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.astgen;

import edu.rice.cs.astgen.ASTModel;
import edu.rice.cs.astgen.NodeType;
import edu.rice.cs.astgen.TabPrintWriter;

public class DepthFirstVoidVisitorGenerator extends edu.rice.cs.astgen.DepthFirstVoidVisitorGenerator {

    public DepthFirstVoidVisitorGenerator(ASTModel ast) {
        super(ast);
    }

    protected void outputDelegatingForCase(NodeType t, TabPrintWriter writer, NodeType root, String retType, String suff, String defaultMethod) {
        if (!(t instanceof TemplateGapClass)) {
            super.outputDelegatingForCase(t, writer, root, retType, suff, defaultMethod);
        }
    }

    protected void outputDefaultCaseVoidMethod(TabPrintWriter writer, NodeType root) {
        super.outputDefaultCaseVoidMethod(writer, root);
        writer.println();
        outputDefaultTemplateMethod(writer, root);
    }

    protected void outputDefaultTemplateMethod(TabPrintWriter writer, NodeType root) {
        writer.startLine("public void defaultTemplateGap(TemplateGap t){");
        writer.indent();
        writer.startLine("throw new RuntimeException(\"Please use TemplateDepthFirstVoidVisitor if you intend to visit template gaps, if not then a template gap survived longer than its expected life time.\");");
        writer.unindent();
        writer.startLine("}");
    }


    protected void outputVisitMethod(NodeType t, TabPrintWriter writer, NodeType root) {
        if (t instanceof TemplateGapClass) {
            outputForCaseHeader(t, writer, "void", "");
            writer.indent();
            writer.startLine("defaultTemplateGap(that);");
            writer.unindent();
            writer.startLine("}");
            writer.println();
        } else {
            super.outputVisitMethod(t, writer, root);
        }
    }
}
