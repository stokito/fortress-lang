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

package com.sun.fortress.interpreter.drivers;

import java.io.File;
import java.net.URI;

public class ProjectProperties {
    
    private static String baseDir() {
        String s = null;
        
        try {
         s = System.getProperty("BASEDIR");
        } catch (Throwable th) {
            
        }
            
        if (s == null) {
            // This gave the wrong value under Eclipse with the old copy
            // of the repository.  As a transitional crutch, specify the
            // BASEDIR explicitly with a property instead.
            
            s = new File(URI.create(ProjectProperties.class.getProtectionDomain().
                getCodeSource().getLocation().toExternalForm())).
                    getParent() ;
        }
        
        s = s + File.separator;
        
        // It is a myth that Windows requires backslashes.  Only the DOS shell
        // requires backslashes.
        s = backslashToSlash(s);
        return s;
    }


    /**
     * No-op right now.
     * 
     * Intended to replace backslashes in a string with slashes.
     * It is a myth that Windows needs backslashes in path names;
     * in fact, it is only the DOS shell.  This fix was backed out
     * because a different fix for the same problem was added; however,
     * once tested this may be re-enabled.
     * 
     * @param s
     * @return
     */
    public static String backslashToSlash(String s) {
        //return s.replaceAll("\\\\", "/");
        return s;
    }
    
    /* This static field holds the absolute path of the project location, as
     * computed by reflectively finding the file location of the unnamed
     * package, and grabbing the parent directory.
     */
    public static final String BASEDIR = baseDir();
        

    /** Creates a new instance of ProjectProperties */
    private ProjectProperties() {
    }
}
