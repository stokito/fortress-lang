/*******************************************************************************
    Copyright 2010 Sun Microsystems, Inc.,
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