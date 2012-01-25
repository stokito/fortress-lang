/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.parser_util.precedence_resolver;

import com.sun.fortress.parser_util.precedence_opexpr.PostfixOpExpr;
import com.sun.fortress.useful.PureList;


public class Bottom extends EnclosingStack {

    public Bottom() {
        super();
    }

    public Bottom(PureList<PostfixOpExpr> _list) {
        super(_list);
    }
}
