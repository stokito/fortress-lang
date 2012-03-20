/*******************************************************************************
    Copyright 2009,2012, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/
package com.sun.fortress.compiler.codegen;

import java.util.List;

import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.FnDecl;
import com.sun.fortress.nodes.FnHeader;
import com.sun.fortress.nodes.IdOrOp;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;

public class FnNameInfo {
    List<StaticParam> static_params;
    Type returnType;
    Type paramType;
    APIName ifNone;
    IdOrOp name;
    Span span;
    public FnNameInfo(FnDecl x, APIName ifNone) {
        FnHeader header = x.getHeader();

        static_params = x.getHeader().getStaticParams();
        returnType = header.getReturnType().unwrap();
        paramType = NodeUtil.getParamType(x);
        this.ifNone = ifNone;
        span = NodeUtil.getSpan(x);
        name = (IdOrOp) (x.getHeader().getName());
    }
}
