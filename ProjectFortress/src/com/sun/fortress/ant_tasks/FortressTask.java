/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.ant_tasks;

public class FortressTask extends BatchTask {

    public FortressTask() {
        super("fortress");
    }

    public void setCompile(boolean val) {
        addExecOption("compile");
    }

    public void setAst(boolean val) {
        addExecOption("-ast");
    }

    public void setKeep(boolean val) {
        addExecOption("-keep");
    }

    public void setPause(boolean val) {
        addExecOption("-pause");
    }

    public void setParse(boolean val) {
        addExecOption("parse");
    }

    public void setNolib(boolean val) {
        addExecOption("-nolib");
    }

    public void setVerbose(boolean val) {
        addExecOption("-v");
    }

    public void setTest(boolean val) {
        addExecOption("-test");
    }

}
