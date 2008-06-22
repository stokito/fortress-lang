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

import java.io.*;
import java.util.*;
import org.apache.tools.ant.*;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.PumpStreamHandler;
import org.apache.tools.ant.types.Environment;

public abstract class BatchTask extends Task {
    protected final String execName;
    // protected final StringBuffer execOptions = new StringBuffer();
    private final Vector<String> options = new Vector<String>();
    protected final Vector<FileSet> filesets = new Vector<FileSet>();
    private boolean newEnvironment = false;
    private Environment env = new Environment();

    protected BatchTask(String _execName) {
        execName = _execName;
    }

    protected void addExecOption(String arg){
	    options.add(arg);
    }

    public void addEnv(Environment.Variable var){
        env.addVariable(var);
        newEnvironment = true;
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
		    /*
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
        */
		    System.err.println( "Processing " + nextFile );
		    ByteArrayOutputStream errors = new ByteArrayOutputStream();
                    Execute execute = new Execute( new PumpStreamHandler( new ByteArrayOutputStream(), new BufferedOutputStream(errors), null ) );
                    if ( newEnvironment ){
                            execute.setNewenvironment(newEnvironment);
                            String[] environment = env.getVariables();
                            execute.setEnvironment(environment);
                    }
                    // execute.setCommandline( new String[]{execName, execOptions.toString(), nextFile } );
		    Vector<String> command = new Vector<String>();
		    command.add(execName);
		    command.addAll(options);
		    command.add(nextFile);
                    // execute.setCommandline( new String[]{execName, nextFile } );
                    execute.setCommandline( command.toArray(new String[0]) );
                    execute.setAntRun(getProject());
                    try{
                        int exitValue = execute.execute();
			if ( exitValue != 0 ){
				System.err.print(errors.toString(0));
			}
                    } catch ( IOException e ){
                        failures = true;
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
