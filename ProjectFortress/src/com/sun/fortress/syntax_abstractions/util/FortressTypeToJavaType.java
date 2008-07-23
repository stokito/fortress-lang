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

package com.sun.fortress.syntax_abstractions.util;

import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.VarType;
import com.sun.fortress.nodes.TraitType;
import com.sun.fortress.nodes.NodeDepthFirstVisitor;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.TypeArg;

/**
 * Translate a Fortress type to a corresponding Java type
 * under the assumption that the Fortress type is either a
 * FortressBuiltin.String or defined in the FortressAst API,
 * or a Maybe or List of these.
 */
public class FortressTypeToJavaType {

    public static String analyze(Type t) {
        return t.accept(new NodeDepthFirstVisitor<String>() {

            @Override
            public String forVarType(VarType that) {
                return that.getName().getText();
            }

            @Override
            public String forTraitType(TraitType that) {
                if (that.getArgs().size() == 0) {
                    return that.getName().getText();                    
                }
                if (that.getArgs().size() != 1) {
                    throw new RuntimeException("One type argument was expected for type: "+that.getName());
                }
                String arg = that.getArgs().get(0).accept(this);
                if (that.getName().getText().equals("List")) {
                    return "List<"+arg+ ">";
                }
                if (that.getName().getText().equals("Maybe")) {
                    return "Option<"+arg+">";
                }
                if (that.getName().getText().equals("Just")) {
                    return "Option<"+arg+">";
                }
                if (that.getName().getText().equals("Nothing")) {
                    return "Option<"+arg+">";
                }
                throw new RuntimeException("Unexpected traittype: "+that.getName()+that.getArgs());
            }

            @Override
            public String forTypeArg(TypeArg that) {
                return that.getType().accept(this);
            }

        });
    }
}
