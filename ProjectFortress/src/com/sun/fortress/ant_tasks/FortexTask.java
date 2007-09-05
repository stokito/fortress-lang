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

public class FortexTask extends Task {
    private Vector<FileSet> filesets = new Vector<FileSet>();

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
                    System.err.println("Processing " + dirScanner.getBasedir() + File.separator + fileName); 
                    Process fortexProcess = Runtime.getRuntime().exec (
                        "fortex " + 
                        dirScanner.getBasedir() + File.separator + fileName
                    );
                    int exitValue = fortexProcess.waitFor();
                    if (exitValue != 0) {
                        failures = true;
                        InputStream errors = fortexProcess.getErrorStream();
                        Writer out = new BufferedWriter(new OutputStreamWriter(System.err));
                        while (errors.available() != 0) {
                            out.write(errors.read());
                        }
                        out.flush();
                    }
                }
            }
            if (failures) { throw new RuntimeException("FORTEX FAILED ON SOME FILES. SEE ABOVE ERROR MESSAGES FOR DETAILS."); }
            
        } catch (Throwable t) {
            if (t instanceof RuntimeException) { throw (RuntimeException)t; }
            else { throw new RuntimeException(t); }
        }
    }
}
