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

package com.sun.fortress.shell;

import java.io.*;
import java.util.regex.Pattern;
import java.util.Arrays;
import edu.rice.cs.plt.tuple.Option;

import com.sun.fortress.interpreter.drivers.*;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes.Api;

public final class Shell extends ShellObject {
    /* Patterns for parsing shell messages.*/
    private static final String NAME =                   "[\\S]+";
    private static final String COMPILE_PATTERN =        "compile " + NAME + "(.fss)|(.fsi)";
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
    
    public Shell(String _pwd) { 
        pwd = _pwd; 
        
        // For now, assume compiled components and APIs are in pwd.
        components = pwd;
    }
    
    private static String fortressLocation() {
        try {
            return new File(System.getenv("FORTRESS_PATH")).getCanonicalPath();
        }
        catch (Exception e) {
            return "";
        }
    }
    
    /* Helper method to print usage message.*/
    private void printUsageMessage() {
        System.err.println("Usage:");
        System.err.println("  " + COMPILE_PATTERN);
        System.err.println("  " + SELF_UPGRADE_PATTERN);
        System.err.println("  " + SCRIPT_PATTERN);
        System.err.println("  " + RUN_PATTERN);
        System.err.println("  " + API_PATTERN);
        System.err.println("  " + LINK_PATTERN);
        System.err.println("  " + UPGRADE_PATTERN);
        System.err.println("  " + EXISTS_PATTERN);
    }
    
    /* Helper method that creates a CompilationUnit from a Fortress source file name.*/
    private Option<CompilationUnit> makeCompilationUnit(String fileName) throws UserError {
        File sourceFile = new File(fileName);
        
        if (! sourceFile.exists()) {
            throw new UserError("Error: File " + fileName + " does not exist.");
        }   
        try {
            return Driver.parseToJavaAst(fileName);
        }
        catch (IOException e) {
            throw new ShellException(e);
        }
    }
    
    /* Helper method that returns true if a component of the given name is installed.*/
    private boolean isInstalled(String componentName) {
        return new File(components + SEP + "components" + SEP + componentName + SEP + ".jst").exists();
    }
    
    /* Main entry point for the fortress shell.*/
    public void execute(String[] tokens) throws InterruptedException {
        // First argument is supplied by the fss script and always present; it's simple $PWD.
        
        if (tokens.length == 1) {
            printUsageMessage();
            System.exit(-1);
        }
        
        // Otherwise, tokens.length > 1.
        // First assemble tokens into a single string we can match against.
        StringBuilder msgBuffer = new StringBuilder(tokens[1]);
        for (String token : Arrays.asList(tokens).subList(2, tokens.length)) {
            msgBuffer.append(" " + token);
        }
        String msg = msgBuffer.toString();
        
        // Now match the assembled string.
        try {
            if      (msg.matches(COMPILE_PATTERN)) { compile(tokens[2]); }
            else if (msg.matches(SELF_UPGRADE_PATTERN)) { selfUpgrade(tokens[2]); }
            else if (msg.matches(SCRIPT_PATTERN)) { script(tokens[2]); }
            else if (msg.matches(RUN_PATTERN)) { run(tokens[2]); }
            else if (msg.matches(API_PATTERN)) { api(tokens[2]); }
            else if (msg.matches(LINK_PATTERN)) { link(tokens[2], tokens[4], tokens[6]); }
            else if (msg.matches(UPGRADE_PATTERN)) { upgrade(tokens[2], tokens[4], tokens[6]); }
            else if (msg.matches(EXISTS_PATTERN)) { exists(tokens[2]); }
            else { printUsageMessage(); }
        }
        catch (UserError error) {
            System.err.println(error.getMessage());
        }
        catch (IOException error) {
            System.err.println(error.getMessage());
        }
    }
    
    /* Call the Ant compile target, passing in the fileName relative to the user's directory,
     * along with the name of the component to store the result into.
     */
    void compile(String fileName) throws UserError, InterruptedException {
        Option<CompilationUnit> prog = makeCompilationUnit(fileName);
        
        if (prog.isNone()) { 
            throw new UserError("Error: File " + fileName + " is not a well-formed Fortress file."); 
        }
        try {
            CompilationUnit _prog = Option.unwrap(prog);
            Driver.writeJavaAst(_prog, pwd + SEP + _prog.getName() + fs.JAVA_AST_SUFFIX);
            
        } catch (IOException e) {
            throw new ShellException(e);
        }
    }
    
    /* Upgrade the internal files of the resident fortress with the contents of the given tar file.*/
    void selfUpgrade(String fileName) throws UserError, InterruptedException {
        //Ant.run("selfupgrade", "-Dtarfile=" + fileName);
    }
    
    /* Convenience method for calling selfUpgrade directly with a file.*/
    void selfUpgrade(File file) throws UserError, InterruptedException { selfUpgrade(file.getPath()); }
    
    /* Checks whether a component or API has been installed in the resident fortress.*/
    void exists(String componentName) throws UserError {
        if (isInstalled(componentName)) {
            System.out.println("Yes, component " + componentName + " is installed in this fortress.");
        }
        else if (new File(components + SEP + "apis" + SEP + componentName + SEP + ".jst").exists()) {
            System.out.println("Yes, API " + componentName + " is installed in this fortress.");
        }
        else {
            System.out.println("No, there is no component or API with name " + componentName + " installed in this fortress.");
        }
    }
    
    /* Runs a fortress source file directly.*/
    void script(String fileName) throws UserError, IOException { Driver.evalComponent(Option.unwrap(makeCompilationUnit(fileName))); }
    
    void run(String componentName) throws UserError {
        try {
            if (isInstalled(componentName)) {
                Driver.evalComponent(Option.unwrap(Driver.readJavaAst(components + SEP + "components" + SEP + componentName + SEP + ".jst")));
            }
            else {
                throw new UserError("Error: There is no component with name " + componentName + " installed in this fortress.");
            }
        }
        catch (IOException e) {
            throw new ShellException(e);
        }
    }
    
    void link(String result, String left, String right) throws UserError { throw new UserError("Error: Link not yet implemented!"); }
    void upgrade(String result, String left, String right) throws UserError { throw new UserError("Error: Upgrade not yet implemented!"); }
    
    void api(String fileName) throws UserError { throw new UserError("Error: Automatic API generation not yet implemented.");  }
    
    public static void main(String[] args) throws InterruptedException { 
        // First argument is supplied by the fss script and always present; it's simple $PWD.
        new Shell(args[0]).execute(args); 
    }
    
}
