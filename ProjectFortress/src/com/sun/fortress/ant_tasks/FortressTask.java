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

package com.sun.fortress.ant_tasks;

import com.sun.fortress.interpreter.drivers.fs;

import java.io.File;
import java.io.*;
import java.util.*;
import org.apache.tools.ant.*;
import org.apache.tools.ant.types.FileSet;

public class FortressTask extends BatchTask {
    private boolean ast = false;
    private boolean keep = false;
    private boolean pause = false;
    private boolean parseOnly = false;
    private boolean nolib = false;
    private boolean verbose = false;
    private boolean test = false;

    public FortressTask() { super("fortress"); }

    public void setAst(boolean val) { execOptions.append(" -ast "); }
    public void setKeep(boolean val) { execOptions.append(" -keep "); }
    public void setPause(boolean val) { execOptions.append(" -pause "); }
    public void setParseOnly(boolean val) { execOptions.append(" -parseOnly "); }
    public void setNolib(boolean val) { execOptions.append(" -nolib "); }
    public void setVerbose(boolean val) { execOptions.append("-v"); }
    public void setTest(boolean val) { execOptions.append("-t"); }

}
