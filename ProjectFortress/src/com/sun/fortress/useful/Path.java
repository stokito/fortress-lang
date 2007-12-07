/*
 * Created on Nov 20, 2007
 *
 */
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

    List<File> dirs = new ArrayList<File>();

    public Path(String path) {
        path = Useful.substituteVars(path);
        StringTokenizer st = new StringTokenizer(path, pathSep);
        while (st.hasMoreTokens()) {
            String e = st.nextToken();           
            File f = new File(e);
            if (f.isDirectory()) {
                dirs.add(f);
            }
        }
    }
 
    public File findFile(String s) throws FileNotFoundException {
        File inappropriateFile = null;
        for (File d : dirs) {
            File f = new File(d, s);
            if (f.isFile()) {
                return f;
            } else if (f.exists()) {
                inappropriateFile = f;
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
        for (File d : dirs) {
            File f = new File(d, s);
            if (f.isDirectory()) {
                return f;
            } else if (f.exists()) {
                inappropriateFile = f;
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
