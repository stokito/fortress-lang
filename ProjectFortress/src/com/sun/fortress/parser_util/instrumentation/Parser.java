/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.parser_util.instrumentation;

import com.sun.fortress.useful.Useful;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collection;

/*
 * Utility class for using parsers and collecting information from them.
 *
 * Also a command line tool:
 *
 * usage:
 *   java com.sun.fortress.parser_util.instrumentation.Parser file ...
 * Parses the listed files with the default (non-instrumented) parser,
 * reporting total parse time after parsing all files.
 *
 * (Especially useful with the java -verbose:gc option.)
 */

class Parser {

    /* Use reflection for two reasons:
     * 1) To allow this collection to be compiled in "compile" target,
     *    when FortressInstrumented may not yet exist.
     * 2) To allow for eventual separation into separate utility.
     */
    static final String PARSER_CLASS = "com.sun.fortress.parser.Fortress";
    static final String PARSER_METHOD = "pFile";
    static final String PARSER_INFO_METHOD = "moduleInfos";

    Class<?> parserClass;
    Constructor<?> constructor;
    Method parserMethod;
    public long parseTime = 0;

    Parser(String parserClass) {
        this(parserClass, PARSER_METHOD);
    }

    Parser(String parserClass, String parserMethod) {
        try {
            this.parserClass = Class.forName(parserClass);
            this.constructor = this.parserClass.getConstructor(Reader.class, String.class);
            this.parserMethod = this.parserClass.getDeclaredMethod(parserMethod, Integer.TYPE);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    void parse(File file) {
        String filename = file.getAbsolutePath();
        Reader in = null;
        try {
            in = Useful.utf8BufferedFileReader(filename);
            parse(in, filename);
        }
        catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        finally {
            if (in != null) try {
                in.close();
            }
            catch (IOException ioe) {
            }
        }
    }

    void parse(Reader in, String filename) {
        try {
            Object p = this.constructor.newInstance(in, filename);
            long startTime = System.currentTimeMillis();
            this.parserMethod.invoke(p, 0);
            parseTime = parseTime + (System.currentTimeMillis() - startTime);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    Collection<Info.ModuleInfo> info() {
        try {
            Method infoMethod = this.parserClass.getDeclaredMethod(PARSER_INFO_METHOD);
            return (Collection<Info.ModuleInfo>) infoMethod.invoke(null);
        }
        catch (NoSuchMethodException nsme) {
            return null;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        Parser p = new Parser(PARSER_CLASS);
        long then = System.currentTimeMillis();
        for (String file : args) {
            try {
                p.parse(new File(file));
                System.out.println("-- done with " + file);
            }
            catch (RuntimeException re) {
                System.out.println("-- parse error in " + file);
            }
        }
        long now = System.currentTimeMillis();
        System.out.println("Time: " + (now - then) + " ms");
    }
}
