/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.astgen;

import edu.rice.cs.astgen.*;
import edu.rice.cs.astgen.Types.PrimitiveName;
import edu.rice.cs.plt.iter.IterUtil;

/**
 * Produce a single argument constructor which takes a SpanInfo, and instantiates
 * each field to null, leaving clients to manually instantiate each field correctly.
 */
public class SingleSpanConstructorGenerator extends CodeGenerator {

    private NodeType abstractNode;
    private NodeType exprNode;
    private NodeType typeNode;

    public SingleSpanConstructorGenerator(ASTModel ast) {
        super(ast);
        if (ast.typeForName("AbstractNode").isSome() && ast.typeForName("Expr").isSome() && ast.typeForName("Type").isSome()) {
            abstractNode = ast.typeForName("AbstractNode").unwrap();
            exprNode = ast.typeForName("Expr").unwrap();
            typeNode = ast.typeForName("Type").unwrap();
        } else throw new RuntimeException("Fortress.ast does not define AbstractNode/Expr!");
    }

    public Iterable<Class<? extends CodeGenerator>> dependencies() {
        return IterUtil.empty();
    }

    public void generateInterfaceMembers(TabPrintWriter writer, NodeInterface i) {
    }

    public void generateClassMembers(TabPrintWriter writer, NodeClass c) {
        // boolean hasSingleSpanConstructor = true;
        // boolean allDefaults = true;
        int nonDefault = 0;
        // Guaranteed to iterate at least once, all fields have SpanInfo.
        for (Field f : c.allFields(ast)) {
            //hasSingleSpanConstructor = false;
            //allDefaults &= f.defaultValue().isSome();
            if (f.defaultValue().isNone()) nonDefault++;
        }

        // hasSingleSpanConstructor |= allDefaults;

        // if (!hasSingleSpanConstructor) {
        // 1 is the Span field, which is known not to be defaulted
        if (nonDefault > 1 && ast.isDescendent(abstractNode, c)) {
            writer.startLine("/**");
            writer.startLine(" * Single Span constructor, for template gap access.  Clients are ");
            writer.startLine(" * responsible for never accessing other fields than the gapId and ");
            writer.startLine(" * templateParams.");
            writer.startLine(" */");
            String infoType = "ASTNodeInfo";
            if (ast.isDescendent(exprNode, c)) infoType = "ExprInfo";
            else if (ast.isDescendent(typeNode, c)) infoType = "TypeInfo";
            writer.startLine("protected " + c.name() + "(" + infoType + " info) {");
            writer.indent();
            writer.startLine("super(info);");
            for (Field f : c.declaredFields(ast)) {
                String init;
                if (f.type() instanceof PrimitiveName) {
                    if (f.type().name().equals("boolean")) {
                        init = "false";
                    } else {
                        init = "0";
                    }
                } else {
                    init = "null";
                }
                writer.startLine("_" + f.name() + " = " + init + ";");
            }
            writer.unindent();
            writer.startLine("}");
            writer.println();
        }
    }

    public void generateAdditionalCode() {
    }

}
