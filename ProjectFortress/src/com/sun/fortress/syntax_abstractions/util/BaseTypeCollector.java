/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.syntax_abstractions.util;

import com.sun.fortress.nodes.*;

import java.util.List;

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
    public String forTraitSelfType(TraitSelfType that) {
        return that.getNamed().accept(this);
    }

    @Override
    public String forTraitTypeOnly(TraitType that,
                                   String info,
                                   String name_result,
                                   List<String> args_result,
                                   List<String> params_result) {
        if (args_result.size() == 0) return that.getName().getText();
        if (args_result.size() != 1) return super.forTraitTypeOnly(that, info, name_result, args_result, params_result);
        return args_result.get(0);
    }

    @Override
    public String forTypeArgOnly(TypeArg that, String info, String type_result) {
        return type_result;
    }

    @Override
    public String forVarType(VarType that) {
        return that.getName().getText();
    }

}
