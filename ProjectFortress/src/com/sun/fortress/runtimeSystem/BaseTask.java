/********************************************************************************
 Copyright 2009 Sun Microsystems, Inc.,
 4150 Network Circle, Santa Clara, California 95054, U.S.A.
 All rights reserved.

 U.S. Government Rights - Commercial software.
 Government users are subject to the Sun Microsystems, Inc. standard
 license agreement and applicable provisions of the FAR and its supplements.

 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 Sun, Sun Microsystems, the Sun logo and Java are trademarks or registered
 trademarks of Sun Microsystems, Inc. in the U.S. and other countries.
 ********************************************************************************/

package com.sun.fortress.runtimeSystem;

import jsr166y.RecursiveAction;

public abstract class BaseTask extends RecursiveAction {
    // Could get this by hacking ForkJoinTask's status field, but
    // not touching that for now as it's too changeable
    boolean actuallyForked = false;

    static boolean worthSpawning() {
        return (getSurplusQueuedTaskCount() < 3);
    }

    // The constructor used by all compiled instances (right now)
    public BaseTask() {
        super();
    }

    public void forkAlways() {
        actuallyForked = true;
        this.fork();
    }

    public void forkIfProfitable() {
        if (worthSpawning()) {
            this.forkAlways();
        }
    }

    public void joinOrRun() {
        if (actuallyForked) {
            this.join();
        } else {
            this.compute();
        }
    }
}
