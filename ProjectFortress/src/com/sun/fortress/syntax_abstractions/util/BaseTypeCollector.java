package com.sun.fortress.syntax_abstractions.util;

import com.sun.fortress.nodes.*;

public class BaseTypeCollector extends NodeDepthFirstVisitor<String> {

    @Override
    public String forVarType(VarType that) {
        return that.getName().getText();   
    }
    
}
