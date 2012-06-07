/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.env;

import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.useful.HasAt;

public class BetterEnvWithTopLevel extends BetterEnv {

    Environment topLevel;

    private BetterEnvWithTopLevel(BetterEnvWithTopLevel betterEnvWithTopLevel, BetterEnv additions) {
        super(betterEnvWithTopLevel, additions);
        topLevel = betterEnvWithTopLevel.topLevel;
    }

    private BetterEnvWithTopLevel(BetterEnvWithTopLevel betterEnvWithTopLevel, Environment additions) {
        super(betterEnvWithTopLevel, additions);
        topLevel = betterEnvWithTopLevel.topLevel;

    }

    private BetterEnvWithTopLevel(BetterEnvWithTopLevel betterEnvWithTopLevel, HasAt x) {
        super(betterEnvWithTopLevel, x);
        topLevel = betterEnvWithTopLevel.topLevel;
    }

    public BetterEnvWithTopLevel(Environment topLevel, HasAt x) {
        super(x);
        parent = topLevel;
        this.topLevel = topLevel;
    }

    public Environment extend(BetterEnv additions) {
        return new BetterEnvWithTopLevel(this, additions);
    }

    public Environment extend(Environment additions) {
        if (additions instanceof BetterEnv) {
            return new BetterEnvWithTopLevel(this, (BetterEnv) additions);
        }
        return new BetterEnvWithTopLevel(this, additions);
    }

    public Environment extendAt(HasAt x) {
        return new BetterEnvWithTopLevel(this, x);
    }

    public Environment extend() {
        return new BetterEnvWithTopLevel(this, this.getAt());
    }

    public Environment getTopLevel() {
        return topLevel;
    }

}
