/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.syntax_abstractions.util;

import com.sun.fortress.exceptions.MacroError;
import com.sun.fortress.nodes.*;

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
            public String forTraitSelfType(TraitSelfType that) {
                return that.getNamed().accept(this);
            }

            @Override
            public String forTraitType(TraitType that) {
                if (that.getArgs().size() == 0) {
                    return that.getName().getText();
                }
                if (that.getArgs().size() != 1) {
                    throw new MacroError(that, "One type argument was expected for type " + that.getName());
                }
                String arg = that.getArgs().get(0).accept(this);
                if (that.getName().getText().equals("List")) {
                    return "List<" + arg + ">";
                }
                if (that.getName().getText().equals("Maybe")) {
                    return "Option<" + arg + ">";
                }
                if (that.getName().getText().equals("Just")) {
                    return "Option<" + arg + ">";
                }
                if (that.getName().getText().equals("Nothing")) {
                    return "Option<" + arg + ">";
                }
                throw new MacroError(that, "Unexpected trait type " + that.getName() + "[\\" + that.getArgs() + "\\]");
            }

            @Override
            public String forTypeArg(TypeArg that) {
                return that.getTypeArg().accept(this);
            }

        });
    }
}
