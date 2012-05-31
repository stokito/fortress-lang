/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.env;

import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.useful.HasAt;

public class BetterEnvLevelZero extends BetterEnv {
    public BetterEnvLevelZero(HasAt s) {
        super(s);
    }

    public Environment extend() {
        return tlExtend();
    }

    public Environment extend(Environment additions) {
        return tlExtend(additions);
    }

    public Environment extendAt(HasAt x) {
        return tlExtendAt(x);
    }

    public Environment getTopLevel() {
        return this;
    }
}
