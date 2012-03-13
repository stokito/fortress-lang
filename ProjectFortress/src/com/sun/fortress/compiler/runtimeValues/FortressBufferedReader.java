/*******************************************************************************
 Copyright 2008,2009,2011 Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.runtimeValues;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.lang.reflect.Constructor;

import com.sun.fortress.compiler.runtimeValues.FException;
import com.sun.fortress.compiler.runtimeValues.Utility;

public final class FortressBufferedReader {
    protected final BufferedReader reader;
    protected final String name;
    protected boolean eof = false;
    protected boolean consumed = false;
    
    public FortressBufferedReader(String name, BufferedReader reader) {
	this.reader = reader;
	this.name = name;
    }
    
    public String toString() {
	return "[Buffered reader " + name + "]";
    }

    public String readLine() throws FException {
	if (eof) return "";
	else {
	    try {
		String line = reader.readLine();
		if (line != null) return line;
		else {
		    eof = true;
		    return "";
		}
	    } catch (IOException e) {
		eof = true;
		throw Utility.makeFortressException("fortress.CompilerBuiltin$IOFailure", e);
	    }
	}
    }
    
    public int read() throws FException {
	if (eof) return 0;
	else {
	    try {
		int c = reader.read();
		if (c >= 0) return c;
		else {
		    eof = true;
		    return 0;
		}
	    } catch (IOException e) {
		eof = true;
		throw Utility.makeFortressException("fortress.CompilerBuiltin$IOFailure", e);
	    }
	}
    }
    
    public String readk(int k) throws FException {
	if (eof) return "";
	else {
	    k = (k <= 0) ? 65536 : k;
	    char c[] = new char[k];
	    try {
		k = reader.read(c, 0, k);
		if (k == -1) {
		    eof = true;
		    return "";
		}
		return new String(c, 0, k);
	    }
	    catch (IOException e) {
		eof = true;
		throw Utility.makeFortressException("fortress.CompilerBuiltin$IOFailure", e);
	    }
	}
    }
    
    public boolean eof() {
	return eof;
    }
    
    public boolean ready() throws FException {
	try {
	    return reader.ready();
	}
	catch (IOException e) {
	    eof = true;
	    throw Utility.makeFortressException("fortress.CompilerBuiltin$IOFailure", e);
	}
    }

    public void close() throws FException {
	// Do NOT test eof first!  Go ahead and close the reader.
	try {
	    reader.close();
	    eof = true;
	}
	catch (IOException e) {
	    eof = true;
	    throw Utility.makeFortressException("fortress.CompilerBuiltin$IOFailure", e);
	}
    }

    public void whenUnconsumed() throws FException {
	if (consumed) {
	    throw Utility.makeFortressException("fortress.CompilerBuiltin$IOFailure",
						"Performed operation on consumed reader for " + name);
	}
    }
    
    public void consume() throws FException {
	synchronized (reader) {
	    whenUnconsumed();
	    consumed = true;
	}
    }
}
