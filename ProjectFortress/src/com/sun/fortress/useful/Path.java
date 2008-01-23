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

package com.sun.fortress.useful;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Path {

    static String pathSep = File.pathSeparator;

    List<File> dirs;

    public Path(String path) {
       this(stringToFiles(path));
    }

    private static List<File> stringToFiles(String path) {
        List<File> dirs = new ArrayList<File>();
        
        path = Useful.substituteVars(path);
        StringTokenizer st = new StringTokenizer(path, pathSep);
        while (st.hasMoreTokens()) {
            String e = st.nextToken();           
            File f = new File(e);
            if (f.isDirectory()) {
                dirs.add(f);
            }
        }
        return dirs;
    }
    
    public Path(List<File> dirs) {
        this.dirs = dirs;
    }
    
    public String toString() {
        return Useful.listInDelimiters("", dirs, "", pathSep);
    }
    
    public Path prepend(Path other) {
        return new Path(Useful.concat(other.dirs, this.dirs));
     }
    public Path append(Path other) {
        return new Path(Useful.concat(this.dirs, other.dirs));
     }
     
    public Path prepend (String s) {
        return prepend(new File(s));
    }
    
    public Path prepend(File f) {
        return new Path(Useful.prepend(f, dirs));
    }
 
    public File findFile(String s) throws FileNotFoundException {
        File inappropriateFile = null;
        if (s.startsWith("/") || s.startsWith(File.separator)) {
            File f = new File(s);
            if (f.isFile()) {
                return f;
            } else if (f.exists()) {
                inappropriateFile = f;
            }
        } else {
        for (File d : dirs) {
            File f = new File(d, s);
            if (f.isFile()) {
                return f;
            } else if (f.exists()) {
                inappropriateFile = f;
            }
        }
        }
        if (inappropriateFile != null) {
            throw new FileNotFoundException("Normal file " + s
                    + " not found in directories " + dirs
                    + "; the last abnormal match was " + inappropriateFile);
        }
        throw new FileNotFoundException("File " + s
                + " not found in directories " + dirs);
    }

    public File findDir(String s) throws FileNotFoundException {
        File inappropriateFile = null;
        if (s.startsWith("/") || s.startsWith(File.separator)) {
            File f = new File(s);
            if (f.isDirectory()) {
                return f;
            } else if (f.exists()) {
                inappropriateFile = f;
            }
        } else {
        for (File d : dirs) {
            File f = new File(d, s);
            if (f.isDirectory()) {
                return f;
            } else if (f.exists()) {
                inappropriateFile = f;
            }
        }
        }
        if (inappropriateFile != null) {
            throw new FileNotFoundException("Directory " + s
                    + " not found in directories " + dirs
                    + "; the last non-directory name match was " + inappropriateFile);
        }
        throw new FileNotFoundException("File " + s
                + " not found in directories " + dirs);
    }

}
