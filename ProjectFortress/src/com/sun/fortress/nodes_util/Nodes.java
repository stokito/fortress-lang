/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/
package com.sun.fortress.nodes_util;

import com.sun.fortress.nodes.CompilationUnit;

public final class Nodes { 

    public static void printNode(CompilationUnit node, String filePrefix) { 
        try { 
            ASTIO.writeJavaAst(node, filePrefix + node.getName().getText());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
