/*******************************************************************************
 Copyright 2008,2009,2011 Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.runtimeValues;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.lang.reflect.Constructor;

import com.sun.fortress.compiler.runtimeValues.FException;
import com.sun.fortress.compiler.runtimeValues.Utility;

public final class FortressBufferedWriter {
    protected final BufferedWriter writer;
    protected final String name;
    protected boolean eof = false;
    protected boolean consumed = false;
    
    public FortressBufferedWriter(String name, BufferedWriter writer) {
	this.writer = writer;
	this.name = name;
    }
    
    public String toString() {
	return "[Buffered writer " + name + "]";
    }

    public void write(int c) throws FException {
	try {
	    writer.write(c);
	} catch (IOException e) {
	    eof = true;
	    throw Utility.makeFortressException("fortress.CompilerBuiltin$IOFailure", e);
	}
    }

    public void write(String s) throws FException {
	try {
	    writer.write(s, 0, s.length());
	} catch (IOException e) {
	    eof = true;
	    throw Utility.makeFortressException("fortress.CompilerBuiltin$IOFailure", e);
	}
    }

    public void newLine() throws FException {
	try {
	    writer.newLine();
	}
	catch (IOException e) {
	    eof = true;
	    throw Utility.makeFortressException("fortress.CompilerBuiltin$IOFailure", e);
	}
    }

    public void flush() throws FException {
	try {
	    writer.flush();
	}
	catch (IOException e) {
	    eof = true;
	    throw Utility.makeFortressException("fortress.CompilerBuiltin$IOFailure", e);
	}
    }

    public void close() throws FException {
	try {
	    writer.close();
	}
	catch (IOException e) {
	    eof = true;
	    throw Utility.makeFortressException("fortress.CompilerBuiltin$IOFailure", e);
	}
    }
}
