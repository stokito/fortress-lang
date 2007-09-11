/*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
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
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.Null;
import edu.rice.cs.plt.tuple.Wrapper;
import java.io.*;
import java.util.*;
import org.apache.tools.ant.*;
import org.apache.tools.ant.types.FileSet;

public abstract class BatchTask extends Task {
    protected final String execName;
    protected final StringBuffer execOptions = new StringBuffer();
    protected final Vector<FileSet> filesets = new Vector<FileSet>();


    protected BatchTask(String _execName) {
        execName = _execName;
    }
    
    public void addFileset(FileSet fileset) {
        filesets.add(fileset);
    }
    
    public void execute() {
        try {
            boolean failures = false;
            
            for (FileSet fileSet : filesets) {
                DirectoryScanner dirScanner = fileSet.getDirectoryScanner(getProject());
                String[] includedFiles = dirScanner.getIncludedFiles();
                for (String fileName : includedFiles) {
                    String nextFile = dirScanner.getBasedir() + File.separator + fileName;
                    System.err.println("Processing " + nextFile); 
                    Process process = Runtime.getRuntime().exec (
                        execName + " " + execOptions + nextFile
                    );
                    int exitValue = process.waitFor();
                    if (exitValue != 0) {
                        failures = true;
                        InputStream errors = process.getErrorStream();
                        Writer out = new BufferedWriter(new OutputStreamWriter(System.err));
                        while (errors.available() != 0) {
                            out.write(errors.read());
                        }
                        out.flush();
                    }
                }
            }
            if (failures) { 
                throw new RuntimeException(
                    execName + " " +
                    "FAILED ON SOME FILES. " +
                    "SEE ABOVE ERROR MESSAGES FOR DETAILS."
                ); 
            }         
        } catch (RuntimeException e) {
            // Catch RuntimeExceptions here to avoid catching them in the next 
            // clause and wrapping them.
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
