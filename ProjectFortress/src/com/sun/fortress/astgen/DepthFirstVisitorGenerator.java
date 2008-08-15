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
import java.util.List;

import edu.rice.cs.astgen.ASTModel;
import edu.rice.cs.astgen.Field;
import edu.rice.cs.astgen.NodeType;
import edu.rice.cs.astgen.TabPrintWriter;
import edu.rice.cs.astgen.Types.TypeName;
import edu.rice.cs.plt.tuple.Option;

public class DepthFirstVisitorGenerator extends edu.rice.cs.astgen.DepthFirstVisitorGenerator {

    public DepthFirstVisitorGenerator(ASTModel ast) {
        super(ast);
    }

    protected void generateVisitor(NodeType root) {
        String visitorName = root.name() + "DepthFirstVisitor";
        TabPrintWriter writer = options.createJavaSourceInOutDir(visitorName);

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
        writer.startLine("@SuppressWarnings(value={\"all\"})");        
        writer.startLine("public abstract class " + visitorName + "<RetType>");
        if (options.usePLT) { writer.print(" extends " + root.name() + "VisitorLambda<RetType>"); }
        else { writer.print(" implements " + root.name() + "Visitor<RetType>"); }
        writer.print(" {");
        writer.indent();

        writer.startLine("private String templateErrorMessage = \"Please use TemplateDepthFirstVisitor if you intend to visit template gaps, if not then a template gap survived longer than its expected life time.\";");

        outputDefaultCaseMethod(writer, root);
        writer.println();

        // Write out forCASEOnly methods
        writer.startLine("/* Methods to handle a node after recursion. */");
        for (NodeType t : ast.descendents(root)) {
            if (!(t instanceof TemplateGapClass) &&
                !(t instanceof TransformationNode) &&
                !(t instanceof EllipsesNode)) {
                outputForCaseOnly(t, writer, root);
            }
        }
        writer.println();


        // Write implementation of visit methods
        writer.startLine("/** Methods to recur on each child. */");
        for (NodeType t : ast.descendents(root)) {
            if (!t.isAbstract()) {
                if (t instanceof TemplateGapClass) {
                    outputVisitTemplateMethod(t, writer, root);
                } else if ( t instanceof TransformationNode ){
                    outputVisitTransformationMethod(t, writer, root );
                } else if ( t instanceof EllipsesNode ){
                    outputVisitEllipsesMethod(t, writer, root);
                } else {
                    outputVisitMethod(t, writer, root);
                }
            }
        }

        writer.println();
        outputRecurMethod(writer, root, "RetType");
        writer.println();

        // Output helpers, if necessary
        for (TypeName t : helpers()) { writer.println(); generateHelper(t, writer, root); }
        clearHelpers();

        // output array seeds, if necessary
        for (TypeName t : this.arraySeeds()) { writer.println(); generateArraySeed(t, writer); }
        //        this._arraySeeds.clear(); // TODO why did Dan clear this

        writer.unindent();
        writer.startLine("}");
        writer.println();
        writer.close();
    }

    @Override
    protected void outputDefaultCaseMethod(TabPrintWriter writer, NodeType root) {
        super.outputDefaultCaseMethod(writer, root);
        writer.println();
        outputDefaultTemplateMethod(writer, root);
        writer.println();
        outputDefaultTransformationMethod(writer, root);
        writer.println();
        outputDefaultEllipsesMethod(writer, root);
    }

    protected void outputDefaultTemplateMethod(TabPrintWriter writer, NodeType root ){
        writer.startLine("public RetType defaultTemplateGap(TemplateGap t){");
        writer.indent();
        writer.startLine("throw new RuntimeException(this.templateErrorMessage);");
        writer.unindent();
        writer.startLine("}");
    }

    protected void outputDefaultTransformationMethod(TabPrintWriter writer, NodeType root ){
        writer.startLine("public RetType defaultTransformationNode(_SyntaxTransformation that){");
        writer.indent();
        writer.startLine("throw new RuntimeException(\"Override defaultTransformationNode to support syntax transformations\");" );
        writer.unindent();
        writer.startLine("}");
    }

    protected void outputDefaultEllipsesMethod(TabPrintWriter writer, NodeType root ){
        writer.startLine("public RetType defaultEllipsesNode(_Ellipses that){");
        writer.indent();
        writer.startLine("throw new RuntimeException(\"Override defaultEllipsesNode to support ellipses\");" );
        writer.unindent();
        writer.startLine("}");
    }

    protected void outputVisitTemplateMethod(NodeType t, TabPrintWriter writer, NodeType root) {
        outputForCaseHeader(t, writer, "RetType", "");
        writer.indent();
        // writer.startLine("throw new RuntimeException(this.templateErrorMessage);");
        writer.startLine("return defaultTemplateGap(that);");
        writer.unindent();
        writer.startLine("}");
        writer.println();
    }

    protected void outputVisitTransformationMethod(NodeType t, TabPrintWriter writer, NodeType root) {
        outputForCaseHeader(t, writer, "RetType", "");
        writer.indent();
        writer.startLine("return defaultTransformationNode(that);");
        writer.unindent();
        writer.startLine("}");
        writer.println();
    }
    
    protected void outputVisitEllipsesMethod(NodeType t, TabPrintWriter writer, NodeType root) {
        outputForCaseHeader(t, writer, "RetType", "");
        writer.indent();
        writer.startLine("return defaultEllipsesNode(that);");
        writer.unindent();
        writer.startLine("}");
        writer.println();
    }
}
