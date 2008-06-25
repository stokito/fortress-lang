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

package com.sun.fortress.nodes_astgen_generators;

import java.util.LinkedList;
import java.util.List;

import edu.rice.cs.astgen.ASTModel;
import edu.rice.cs.astgen.NodeClass;
import edu.rice.cs.astgen.Field;
import edu.rice.cs.astgen.NodeType;
import edu.rice.cs.astgen.TabPrintWriter;
import edu.rice.cs.astgen.UpdateVisitorGenerator;
import edu.rice.cs.plt.tuple.Option;

public class TemplateVisitorGenerator extends UpdateVisitorGenerator {

    public TemplateVisitorGenerator(ASTModel ast) {
        super(ast);
    }

    @Override
    protected void generateVisitor(NodeType root) {
        String visitorName = "TemplateUpdateVisitor";
        String extendedVisitorName = root.name() + "UpdateVisitor";
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
        writer.startLine("public abstract class " + visitorName);
        
        writer.print(" extends " + extendedVisitorName);
        
        writer.print(" {");
        writer.indent();
        writer.println();
        
        writer.startLine("/** Methods to recur on each child. */");
        for (NodeType t : ast.descendents(root)) {
          if (!t.isAbstract()) {
            writer.println();
            outputVisitMethod(t, writer, root);
          }
        }
       
        outputHandleTemplateMethod(writer);
        
        writer.unindent();
        writer.startLine("}");
        writer.println();
        writer.close();
      }
    
    @Override
    protected void outputVisitMethod(NodeType t, TabPrintWriter writer, NodeType root) {
        outputForCaseHeader(t, writer, root.name(), "");
        writer.indent();
        writer.startLine("if (that instanceof TemplateGap) {");
        writer.indent();
        writer.startLine("handleTemplateGap((TemplateGap) that);");
        writer.unindent();
        writer.startLine("}");
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
        for (String recurVal : recurVals) { writer.print(", " + recurVal); }
        writer.print(");");
        writer.unindent();
        writer.startLine("}");
        writer.println();
      }
    
    private void outputHandleTemplateMethod(TabPrintWriter writer) {
        writer.startLine("public void handleTemplateGap(TemplateGap that) {");
        writer.indent();
        writer.startLine("return;");
        writer.unindent();
        writer.startLine("}");        
    }
}
