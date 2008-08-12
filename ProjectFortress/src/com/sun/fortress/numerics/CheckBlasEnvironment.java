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

package com.sun.fortress.numerics;

public class CheckBlasEnvironment {

    public static void checkLinux() {
        String javaHome = System.getenv("JAVA_HOME");
        String blasLib = System.getenv("BLAS_LIB");
        String blasInclude = System.getenv("BLAS_INCLUDE");
        boolean problem = false;
        if (javaHome == null) {
            System.err.println("You must specify a $JAVA_HOME environment variable"); 
            problem = true;
        }
        if (blasLib == null) {
            System.err.println("You must specify $BLAS_LIB (try /usr/lib/atlas)");
            problem = true;            
        }
        if (blasInclude == null) {
            System.err.println("You must specify $BLAS_INCLUDE (try /usr/include/atlas)");
            problem = true;            
        }        
        if (problem) System.exit(-1);
    }
    
    public static void checkOSX() {
        String javaLib = System.getenv("JAVA_LIB");        
        boolean problem = false;
        if (javaLib == null) {
            System.err.println("You must specify a $JAVA_LIB environment variable");
	    System.err.println("(try /System/Library/Frameworks/JavaVM.framework/Versions/Current)"); 
            problem = true;
        }        
        if (problem) System.exit(-1);        
    }
    
    public static void main(String[] args) {
        String osName = System.getProperty("os.name");
        if (osName.equals("Linux")) {
            checkLinux();
        } else if (osName.equals("Mac OS X")) {
	    checkOSX();            
        } else {
            System.err.println("The Fortress build system currently does not support");
            System.err.println("blas bindings for the the " + osName + " platform.");
            System.err.println("Please submit a bug report at http://projectfortress.sun.com.");
            System.exit(-1);
        }
    }

}
