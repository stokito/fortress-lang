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

import java.util.List;

import com.sun.fortress.nodes.*;

public class BaseTypeCollector extends NodeDepthFirstVisitor<String> {

    @Override
    public String defaultCase(Node that) {
        return "";
    }

    @Override
    public String forTraitType(TraitType that) {
        // TODO Auto-generated method stub
        return super.forTraitType(that);
    }

    @Override
    public String forTraitTypeOnly(TraitType that, String name_result,
            List<String> args_result) {
        if (args_result.size() != 1)
            return super.forTraitTypeOnly(that, name_result, args_result);       
        return args_result.get(0);
    }

    @Override
    public String forTypeArgOnly(TypeArg that, String type_result) {
        return type_result;
    }

    @Override
    public String forVarType(VarType that) {
        return that.getName().getText();
    }

}
