/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.astgen;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import edu.rice.cs.astgen.ASTModel;
import edu.rice.cs.astgen.Field;
import edu.rice.cs.astgen.NodeType;
import edu.rice.cs.astgen.TabPrintWriter;
import edu.rice.cs.astgen.Types.ClassName;
import edu.rice.cs.astgen.Types.TypeName;
import edu.rice.cs.astgen.Types.TypeArgumentName;

public class CollectingVisitorGenerator extends DepthFirstVisitorGenerator {

    private HashMap<TypeName, String> encounteredResultTypes;

    public CollectingVisitorGenerator(ASTModel ast) {
        super(ast);
        encounteredResultTypes = new HashMap<TypeName, String>();
    }

    protected void generateVisitor(NodeType root) {
        String visitorName = root.name() + "CollectingVisitor";
        TabPrintWriter writer = options.createJavaSourceInOutDir(visitorName);

        // Class header
        writer.startLine("/** A visitor over " + root.name());
        writer.print(" that combined recursive results.");
        writer.startLine(" ** This visitor implements the visitor interface with methods that");
        writer.startLine(" ** first visit children, and then call forCASEOnly(), passing in");
        writer.startLine(" ** the values of the visits of the children. (CASE is replaced by the case name.)");
        writer.startLine(" ** By default, each forCASEOnly combines its results together using the method");
        writer.startLine(" **    combine(node, values...)");
        writer.startLine(" ** All recursion should go via recur(...); this allows overrides to insert");
        writer.startLine(" ** hooks into the recursion path (eg to record incremental results)");
        writer.startLine(" **/");
        writer.startLine("@SuppressWarnings({\"unused\", \"unchecked\"})");
        writer.startLine("public abstract class " + visitorName + "<RetType>");
        writer.print(" extends " + root.name() + "DepthFirstVisitor<RetType>");
        writer.print(" {");
        writer.indent();

        // Write out forCASEOnly methods
        writer.startLine("/* Methods to handle a node after recursion. */");
        for (NodeType t : ast.descendents(root)) {
            if (!(t instanceof TemplateGapClass) && !(t instanceof TransformationNode) && !(t instanceof EllipsesNode)) {
                outputForCaseOnly(t, writer, root);
            }
        }

        outputCombineDecls(writer, root);

        writer.unindent();
        writer.startLine("}");
        writer.println();
        writer.close();
    }

    protected void outputForCaseOnly(NodeType t, TabPrintWriter writer, NodeType root) {
        List<String> recurDecls = new ArrayList<String>();
        String combArgs = "";
        StringBuilder buf = new StringBuilder();
        for (Field f : t.allFields(ast)) {
            if (canRecurOn(f.type(), root)) {
                TypeName result = resultType(f.type());
                String resultTy = result.name();
                String combiner = combine(result, resultTy);
                String resultId = f.name() + "_result";
                recurDecls.add(resultTy + " " + resultId);
                if (combiner==null) {
                    buf.append(", " + resultId);
                } else {
                    encounteredResultTypes.put(result, combiner);
                    buf.append(", " + combiner + resultId + ")");
                }
            }
        }
        combArgs = buf.toString();
        outputForCaseHeader(t, writer, "RetType", "Only", recurDecls);
        writer.indent();
        writer.startLine("return combine(that");
        writer.print(combArgs);
        writer.print(");");
        writer.unindent();
        writer.startLine("}");
        writer.println();
    }

    protected void outputCombineDecls(TabPrintWriter writer, NodeType root) {
        closeOverTypeArgs();
        writer.startLine("public abstract RetType combine(List<RetType> l);");
        writer.println();
        writer.startLine("public RetType combine(");
        writer.print(root.name());
        writer.print(" that, RetType... vals) {");
        writer.indent();
        writer.startLine("return combine(vals);");
        writer.unindent();
        writer.startLine("}");
        writer.println();
        writer.startLine("public RetType combine(RetType... vals) {");
        writer.indent();
        writer.startLine("List<RetType> l = new java.util.ArrayList(vals.length);");
        writer.startLine("for (int i=0; i < vals.length; i++) l.add(vals[i]);");
        writer.startLine("return combine(l);");
        writer.unindent();
        writer.startLine("}");
        writer.println();
        for (Map.Entry<TypeName, String> e : encounteredResultTypes.entrySet()) {
            outputCombineDecl(writer, e.getValue(), e.getKey());
        }
    }

    protected Error confused(String name, String tName) {
        return new Error("Don't know how to generate "+name+tName+")");
    }

    protected void outputCombineDecl(TabPrintWriter writer, String name, TypeName t) {
        String tName = t.name();
        if (tName.equals("List<RetType>")) return;
        boolean isList = false;
        boolean isOption = false;
        ClassName c = null;
        if (t instanceof ClassName) {
            c = (ClassName)t;
            String cn = c.className();
            isList = cn.equals("List");
            isList = isList || cn.equals("java.util.List");
            if (!isList) {
                isOption = cn.equals("Option");
                isOption = isOption || cn.equals("edu.rice.cs.plt.tuple.Option");
            }
        }
        writer.startLine("public ");
        if (!isList && !isOption) writer.print("abstract ");
        writer.print("RetType " + name + tName + " v)");
        if (!isList && !isOption) {
            writer.print(";");
            writer.startLine(
                "// CollectingVisitorGenerator can't generate code for this yet");
            return;
        }
        writer.print(" {");
        writer.indent();
        TypeArgumentName arg = c.typeArguments().get(0);
        String argName = arg.name();
        String combiner = combine(arg,argName);
        if (isList) {
            writer.startLine("ArrayList<RetType> t = new ArrayList<RetType>();");
            writer.startLine("for (" + argName + " e : v) t.add(" + combiner + "e));");
            writer.startLine("return combine(t);");
        } else if (isOption) {
            writer.startLine("if (v.isSome()) return ");
            if (combiner == null) {
                writer.print("v.unwrap();");
            } else {
                writer.print(combiner + "v.unwrap());");
            }
            writer.startLine("return combine();");
        }
        writer.unindent();
        writer.startLine("}");
        writer.println();
    }

    protected void closeOverTypeArgs() {
        List<ClassName> working;
        List<ClassName> todo = new ArrayList(encounteredResultTypes.size());
        // Avoid concurrent modification problems on encounteredResultTypes
        // by copying current key set into todo.
        for (TypeName t : encounteredResultTypes.keySet()) {
            if (t instanceof ClassName) todo.add((ClassName)t);
        }
        do {
            working = todo;
            todo = new ArrayList<ClassName>();
            for (ClassName t : working) {
                for (TypeArgumentName a : ((ClassName)t).typeArguments()) {
                    if (!(a instanceof ClassName)) continue;
                    ClassName tn = (ClassName)a;
                    String combiner = combine(tn, tn.name());
                    if (combiner==null) continue;
                    if (encounteredResultTypes.put(tn,combiner)==null)
                        todo.add(tn);
                }
            }
        } while (todo.size() > 0);
    }

    protected String combineInner(TypeArgumentName t) {
        if (!(t instanceof ClassName)) return null;
        ClassName c = (ClassName) t;
        List<TypeArgumentName> tArgs = c.typeArguments();
        if (tArgs.size() == 0) return null;
        String res = c.className();
        StringBuilder buf = new StringBuilder();
        buf.append(res);
        for (TypeArgumentName ta : tArgs) {
            String in = combineInner(ta);
            if (in != null) buf.append(in);
        }
        res = buf.toString();
        return res;
    }

    protected String combine(TypeArgumentName t, String name) {
        if (!(t instanceof ClassName)) return "combine(";
        ClassName c = (ClassName) t;
        String res = c.className();
        if (res.equals("RetType")) return null;
        List<TypeArgumentName> tArgs = c.typeArguments();
        if (tArgs.size() == 0) return "combine(";
        boolean useRes = false;
        StringBuilder buf = new StringBuilder();
        buf.append(res);
        for (TypeArgumentName ta : tArgs) {
            String in = combineInner(ta);
            if (in==null) {
                buf.append(ta.name());
            } else {
                useRes = true;
                buf.append(in);
            }
        }
        res = buf.toString();
        if (!useRes) return "combine(";
        return "combine" + res + "(";
    }
}
