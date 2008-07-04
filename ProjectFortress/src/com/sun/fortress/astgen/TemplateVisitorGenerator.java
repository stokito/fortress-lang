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
import edu.rice.cs.astgen.NodeClass;
import edu.rice.cs.astgen.Field;
import edu.rice.cs.astgen.NodeInterface;
import edu.rice.cs.astgen.NodeType;
import edu.rice.cs.astgen.TabPrintWriter;
import edu.rice.cs.astgen.UpdateVisitorGenerator;
import edu.rice.cs.astgen.Types.TypeName;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.Pair;

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

        NodeType templateGapInterface = null;
        for (NodeInterface ni: ast.interfaces()) {
            if (ni.name().equals("TemplateGap")) {
                templateGapInterface = ni;
            }
        }
        outputForCaseOnly(templateGapInterface, writer, root);        

        writer.unindent();
        writer.startLine("}");
        writer.println();
        writer.close();
    }

    @Override
    protected void outputVisitMethod(NodeType t, TabPrintWriter writer, NodeType root) {
        outputForCaseHeader(t, writer, root.name(), "");
        writer.indent();

        boolean isTemplateGap = false;
        for (TypeName typeName: t.interfaces()) {
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
            for (String recurVal : recurVals) { writer.print(", " + recurVal); }
            writer.print(");");
        }
        else {
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
        }
        writer.unindent();
        writer.startLine("}");
        writer.println();
    }
    
    protected void outputForCaseOnly(NodeType t, TabPrintWriter writer, NodeType root) {
        // only called for concrete cases; must not delegate
        List<String> params = new LinkedList<String>(); // "type name" strings
        List<String> getters = new LinkedList<String>(); // expressions
        List<String> paramRefs = new LinkedList<String>(); // variable names or null
        for (Field f : t.allFields(ast)) {
          getters.add("that." + f.getGetterName() + "()");
          if (canRecurOn(f.type(), root)) {
            String paramName = f.name() + "_result";
            params.add(f.type().name() + " " + paramName);
            paramRefs.add(paramName);
          }
          else { paramRefs.add(null); }
        }
        outputForCaseHeader(t, writer, root.name(), "Only", params);
        writer.indent();
        if (params.isEmpty()) { writer.startLine("return that;"); }
        else {
          writer.startLine("if (");
          boolean first = true;
          for (Pair<String, String> getterAndRef : IterUtil.zip(getters, paramRefs)) {
            if (getterAndRef.second() != null) {
              if (first) { first = false; }
              else { writer.print(" && "); }
              writer.print(getterAndRef.first() + " == " + getterAndRef.second());
            }
          }
          writer.print(") return that;");
          
          writer.startLine("else return that;"); // new " + t.name() + "(");
//          first = true;
//          for (Pair<String, String> getterAndRef : IterUtil.zip(getters, paramRefs)) {
//            if (first) { first = false; }
//            else { writer.print(", "); }
//            if (getterAndRef.second() == null) { writer.print(getterAndRef.first()); }
//            else { writer.print(getterAndRef.second()); }
//          }
//          writer.print(");");
        }
        writer.unindent();
        writer.startLine("}");
      }

}
