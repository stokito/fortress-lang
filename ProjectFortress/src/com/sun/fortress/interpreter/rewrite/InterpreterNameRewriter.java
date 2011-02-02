/*******************************************************************************
 Copyright 2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/
package com.sun.fortress.interpreter.rewrite;

import com.sun.fortress.nodes.*;

public interface InterpreterNameRewriter {
    Expr replacement(VarRef original);

    BoolRef replacement(BoolRef original);

    IntRef replacement(IntRef original);

    Expr replacement(FnRef original);

    Expr replacement(OpRef original);

    VarType replacement(VarType original);
}
