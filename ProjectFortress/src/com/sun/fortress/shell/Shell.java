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

package com.sun.fortress.shell;

import java.io.*;
import java.util.regex.Pattern;
import java.util.Arrays;
import edu.rice.cs.plt.tuple.Option;

import com.sun.fortress.interpreter.drivers.*;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes.Api;

import static com.sun.fortress.shell.ConvenientStrings.*;

public final class Shell {
    /* Patterns for parsing shell messages.*/
    private static final String NAME =                   "[\\S]+";
    private static final String COMPILE_PATTERN =        "compile " + NAME + "((.fss)|(.fsi))";
    private static final String SELF_UPGRADE_PATTERN =   "selfupgrade " + NAME + ".tar";
    private static final String SCRIPT_PATTERN =         "script " + NAME + ".fsx";
    private static final String RUN_PATTERN =            "run " + NAME;
    private static final String API_PATTERN =            "api " + NAME + ".fss";
    private static final String LINK_PATTERN =           "link " + NAME + " from " + NAME + " with " + NAME;
    private static final String UPGRADE_PATTERN =        "upgrade " + NAME + " from " + NAME + " with " + NAME;
    private static final String EXISTS_PATTERN =         "exists " + NAME;

    private String pwd;
    /* Relative location of the resident fortress to the jar file this class is packaged into. */
    private String components;

    private CommandInterpreter interpreter;

    public Shell(String _pwd) {
        pwd = _pwd;
        // For now, assume compiled components and APIs are in pwd.
        components = pwd;
        interpreter = new CommandInterpreter(this);
    }

    String getPwd() { return pwd; }
    String getComponents() { return components; }
    CommandInterpreter getInterpreter() { return interpreter; }

    public static String fortressLocation() {
        return ProjectProperties.ANALYZED_CACHE_DIR;
    }

    /* Helper method to print usage message.*/
    private void printUsageMessage() {
        System.err.println("Usage:");
        System.err.println("  " + COMPILE_PATTERN);
        //System.err.println("  " + SELF_UPGRADE_PATTERN);
        //System.err.println("  " + SCRIPT_PATTERN);
        System.err.println("  " + RUN_PATTERN);
        //System.err.println("  " + API_PATTERN);
        //System.err.println("  " + LINK_PATTERN);
        //System.err.println("  " + UPGRADE_PATTERN);
        System.err.println("  " + EXISTS_PATTERN);
    }

    /* Main entry point for the fortress shell.*/
    public void execute(String[] tokens) throws InterruptedException, Throwable {
      
        if (tokens.length == 0) {
            printUsageMessage();
            System.exit(-1);
        }

        // Otherwise, tokens.length > 1.
        // First assemble tokens into a single string we can match against.
        StringBuilder msgBuffer = new StringBuilder(tokens[0]);
        for (String token : Arrays.asList(tokens).subList(1, tokens.length)) {
            msgBuffer.append(" " + token);
        }
        String msg = msgBuffer.toString();

        // Now match the assembled string.
        try {
            if      (msg.matches(COMPILE_PATTERN)) { interpreter.compile(false, tokens[1]); }
            //else if (msg.matches(SELF_UPGRADE_PATTERN)) { interpreter.selfUpgrade(tokens[2]); }
            //else if (msg.matches(SCRIPT_PATTERN)) { interpreter.script(tokens[2]); }
            else if (msg.matches(RUN_PATTERN)) {
                interpreter.run(tokens[1], Arrays.asList(tokens).subList(2, tokens.length));
            }
            //else if (msg.matches(API_PATTERN)) { interpreter.api(tokens[2]); }
            //else if (msg.matches(LINK_PATTERN)) { interpreter.link(tokens[2], tokens[4], tokens[6]); }
            //else if (msg.matches(UPGRADE_PATTERN)) { interpreter.upgrade(tokens[2], tokens[4], tokens[6]); }
            else if (msg.matches(EXISTS_PATTERN)) { interpreter.exists(tokens[1]); }
            else { printUsageMessage(); }
        }
        catch (UserError error) {
            System.err.println(error.getMessage());
        }
        catch (IOException error) {
            System.err.println(error.getMessage());
        }
    }

    public static void main(String[] args) throws InterruptedException, Throwable {
        // First argument is supplied by the fss script and is always present; it's simply $PWD.
        new Shell(args[0]).execute(args);
    }

}
