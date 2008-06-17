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

package com.sun.fortress.parser_util.instrumentation;

/*
 * Based on com.sun.fortress.syntax_abstraction.rats.RatsUtil
 */
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import xtc.parser.Module;
import xtc.parser.ModuleName;
import xtc.parser.PParser;
import xtc.parser.ParseError;
import xtc.parser.PrettyPrinter;
import xtc.parser.Production;
import xtc.parser.Result;
import xtc.parser.SemanticValue;
import xtc.tree.Comment;
import xtc.tree.Printer;
import xtc.type.JavaAST;

import com.sun.fortress.useful.Useful;

import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.io.IOUtil;

/*
 * Utility class. Should have no Fortress-specific knowledge.
 */

public class Util {

    private static final char SEP = '/'; // File.separatorChar

    public static File getTempDir() {
        try {
            return IOUtil.createAndMarkTempDirectory("instrumented","rats");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Module getRatsModule(File file) {
        // System.err.println("Reading in " + file);
        try {
            BufferedReader in = Useful.utf8BufferedFileReader(file.getCanonicalPath());
            xtc.parser.PParser parser = 
                new PParser(in, file.getCanonicalPath());
            Result r = parser.pModule(0);
            if (r.hasValue()) {
                SemanticValue v = (SemanticValue) r;
                return (Module) v.value;
            } else {
                ParseError err = (ParseError) r;
                String errMsg;
                throw new RuntimeException("Parse error: " + err.toString());
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeRatsModule(Module module, File tempDir) {
        try {
            String basename = getModulePath(module.name.name);
            File file = new File(tempDir, basename + ".rats");
            makeSureDirectoryExists(file.getParentFile());
            // System.err.println("Writing out " + file);
            FileOutputStream fo = new FileOutputStream(file);  
            PrettyPrinter pp = new PrettyPrinter(new Printer(fo), new JavaAST(), true);
            pp.visit(module);
            pp.flush();
            fo.flush();
            fo.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void makeSureDirectoryExists(File dir) {
        if (!dir.isDirectory()) {
            if (!dir.mkdirs()) {
                throw new RuntimeException("Could not create directories: "+dir.getAbsolutePath());
            }
        }
    }

    public static String getModulePath(String dottedName) {
        return dottedName.replace('.', File.separatorChar);
    }
}
