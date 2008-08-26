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

package com.sun.fortress.interpreter.env;

import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.useful.HasAt;

public class BetterEnvWithTopLevel extends BetterEnv {

    Environment topLevel;
    
    private BetterEnvWithTopLevel(BetterEnvWithTopLevel betterEnvWithTopLevel,
            BetterEnv additions) {
        super(betterEnvWithTopLevel, additions);
        topLevel = betterEnvWithTopLevel.topLevel;
    }

    private BetterEnvWithTopLevel(BetterEnvWithTopLevel betterEnvWithTopLevel,
            Environment additions) {
        super(betterEnvWithTopLevel, additions);
        topLevel = betterEnvWithTopLevel.topLevel;

    }

    private BetterEnvWithTopLevel(BetterEnvWithTopLevel betterEnvWithTopLevel,
            HasAt x) {
        super(betterEnvWithTopLevel, x);
        topLevel = betterEnvWithTopLevel.topLevel;
    }
    
    public BetterEnvWithTopLevel(Environment topLevel, HasAt x) {
        super(x);
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
