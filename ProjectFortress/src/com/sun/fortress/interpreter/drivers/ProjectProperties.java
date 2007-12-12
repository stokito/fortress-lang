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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.Properties;

import com.sun.fortress.useful.Path;
import com.sun.fortress.useful.StringMap;

public class ProjectProperties {
    
    private static String fortressHome() {
        String s = null;
        try {
            s = System.getProperty("fortress.home");
           } catch (Throwable th) {
               
        }
        if (s == null) {
            s = System.getenv("FORTRESS_HOME");
        }
        if (s == null) {
            Path p = new Path(System.getProperty("java.class.path"));
            File f = null;
            try {
                f = p.findDir("../ProjectFortress");
                try {
                   s = (new File(f, "..")).getCanonicalPath();
                } catch (IOException ex) {
                    throw new Error("Failure to evaluate relative path .. from " + f);
                }
            } catch (FileNotFoundException ex1) {
                try {
                    f = p.findDir("../../ProjectFortress/build");
                    try {
                        s = (new File(f, "../..")).getCanonicalPath();
                     } catch (IOException ex) {
                         throw new Error("Failure to evaluate relative path ../.. from " + f);
                     }
                } catch (FileNotFoundException ex2) {
                    throw new Error("Could not find fortress home using fortress.home, FORTRESS_HOME, or probing classpath.");
                }
            }
        }
        return s;
    }
    
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

   private static String someImplDir(String property_name, String default_name) {
        String s = null;
        
        try {
         s = System.getProperty(property_name);
        } catch (Throwable th) {
            
        }
            
        if (s == null) {
            s = BASEDIR + default_name;
        }
        
        s = s + File.separator;
        
        // It is a myth that Windows requires backslashes.  Only the DOS shell
        // requires backslashes.
        s = backslashToSlash(s);
        return s;
    }

   private static Properties sysProps = System.getProperties();
    
   
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
     */
    public static String backslashToSlash(String s) {
        //return s.replaceAll("\\\\", "/");
        return s;
    }
    
    public static final String FORTRESS_HOME = fortressHome();
    
    static final String home = System.getenv("HOME");
    
    static final StringMap searchTail = new StringMap.ComposedMaps(
            new StringMap.FromFileProps(".fortress.properties"),
            new StringMap.FromFileProps(home+"/.fortress.properties"),
            new StringMap.FromFileProps(FORTRESS_HOME+"/fortress.properties")
             );
 
    /**
     * System.getProperty("fortress.cache")
     * System.getenv("FORTRESS_CACHE")
     * ./.fortress.properties .getProperty("fortress.cache")
     * ~/.fortress.properties .getProperty("fortress.cache")
     * ${FORTRESS_HOME}/fortress.properties .getProperty("fortress.cache") .
     */
    
    private static String searchDef(String asProp, String asEnv, String defaultValue) {
        String result = null;
        result = System.getProperty(asProp);
        if (result != null) return result;
        result = System.getenv(asEnv);
        if (result != null) return result;
        result = searchTail.get(asProp);
        return result == null ? defaultValue : result;
    }
    
    public static final String CACHE_DIR = searchDef("fortress.cache", "FORTRESS_CACHE", ".") + "/.fortress_cache";
    
    /* This static field holds the absolute path of the project location, as
     * computed by reflectively finding the file location of the unnamed
     * package, and grabbing the parent directory.
     */
    public static final String BASEDIR = baseDir();
    
    public static final String TEST_LIB_DIR = someImplDir("TEST_LIB_DIR", "test_library");
    public static final String TEST_LIB_NATIVE_DIR = someImplDir("TEST_LIB_NATIVE_DIR", "test_library_native");
    
    

    /** Creates a new instance of ProjectProperties */
    private ProjectProperties() {
    }
}
