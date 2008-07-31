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

package com.sun.fortress.syntax_abstractions.phases;

import java.lang.reflect.Constructor;

import java.io.File;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.StringWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileInputStream;

import java.util.Collection;

import edu.rice.cs.astgen.TabPrintWriter;

import com.sun.fortress.exceptions.MacroError;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.syntax_abstractions.environments.SyntaxDeclEnv;
import com.sun.fortress.syntax_abstractions.rats.util.FreshName;
import com.sun.fortress.syntax_abstractions.rats.RatsUtil;
import com.sun.fortress.syntax_abstractions.rats.JavaC;
import com.sun.fortress.syntax_abstractions.util.JavaAstPrettyPrinter;

import com.sun.fortress.syntax_abstractions.util.ActionCreater;

import com.sun.fortress.useful.Debug;

/* creates java classes that represent syntax transformers.
 * this is not used anymore, it is superceded by the ComposingSyntaxDefTranslater
 */
public class SyntaxTransformerCreater {

    private static final String PACKAGE = "com.sun.fortress.syntax_abstractions.transformer";
    private static final String NODES = "com.sun.fortress.nodes.*";
    private static final String NODES_UTIL = "com.sun.fortress.nodes_util.*";
    private static final String JAVA_UTIL = "java.util.*";
    private static final String INTERFACE = "com.sun.fortress.syntax_abstractions.phases.SyntaxTransformer";
    private String dir;

    public SyntaxTransformerCreater(){
        dir = RatsUtil.getTempDir();
    }

    public SyntaxTransformer create( Node node, SyntaxDeclEnv env ){
        String className = FreshName.getFreshName("Transformer");
        String code = makeCode( node, env, className );
        return createTransformer( code, className );
    }

    private String tempDir(){
        return dir;
    }

    private SyntaxTransformer createTransformer( String code, String className ){
        String dir = tempDir();

        try{
            makeSureDirectoryExists( dir + '/' + PACKAGE.replace('.','/') );
            File file = new File( dir + '/' + PACKAGE.replace('.','/') + '/' + className +".java" );
            BufferedWriter writer = new BufferedWriter( new FileWriter( file ) );
            writer.write( code );
            writer.close();
            int compileResult = JavaC.compile(dir, dir, file.getCanonicalPath() );
            if (compileResult != 0) {
                throw new MacroError("A compiler error occured while compiling a temporary parser: " + compileResult );
            }

            Debug.debug( Debug.Type.SYNTAX, 2, "Created transformer " + className + " in " + dir );

            return (SyntaxTransformer) instantiate( dir, PACKAGE + "." + className );
        } catch ( IOException e ){
            throw new MacroError( "Could not create transformer for " + className, e );
        }
    }

    private Object instantiate( String dir, String full ){

        Loader loader = new Loader(dir);
        try {
            Class c = loader.findClass(full);

            Constructor<?> constructor = c.getConstructor();
            return c.newInstance();
        } catch (ClassNotFoundException e) {
            throw new MacroError( "Could not find class " + full, e );
        } catch ( Exception e ){
            throw new MacroError( "Could not create class of type " + full, e );
        }
    }

    private class Loader extends ClassLoader {

        private String basedir;

        public Loader(String basedir) {
            super(ClassLoader.getSystemClassLoader());
            this.basedir = basedir;
        }

        @Override
        public Class<?> findClass(String name) throws ClassNotFoundException {
            byte[] b = loadClassData(name);
            if (b != null)
                return defineClass(null, b, 0, b.length);
            throw new ClassNotFoundException(name);
        }

        private byte[] loadClassData(String classname) throws ClassNotFoundException {

            byte[] res = null;
            classname = classname.replaceAll("\\.", ""+File.separatorChar);

            File classfile = new File(basedir + classname+".class");
            if (classfile.exists()) {
                res = new byte[(int) classfile.length()];
                try {
                    new FileInputStream(classfile).read(res);
                } catch (FileNotFoundException e) {
                    throw new ClassNotFoundException(e.getMessage(), e);
                } catch (IOException e) {
                    throw new ClassNotFoundException(e.getMessage(), e);
                }
            }
            return res;
        }
    }

    private void makeSureDirectoryExists(String tempDir) {
        File dir = new File(tempDir);
        if (!dir.isDirectory()) {
            if (!dir.mkdirs()) {
                throw new MacroError("Could not create directories: "+dir.getAbsolutePath());
            }
        }
    }

    private String makeCode( Node node, SyntaxDeclEnv env, String className ){
        StringWriter string = new StringWriter();
        TabPrintWriter writer = new TabPrintWriter(string, 4);
        writer.startLine(String.format("package %s;", PACKAGE));
        writeImports(writer);
        writer.startLine(String.format("public class %s implements %s<Node>{", className, INTERFACE));
        writer.indent();
        /* ugly way of getting BOUND_VARIABLES */
        writer.startLine(String.format("public Node invoke( Map<String,Object> %s){", ActionCreater.BOUND_VARIABLES));
        writer.indent();

        JavaAstPrettyPrinter jpp = new JavaAstPrettyPrinter(env);
        String value = node.accept(jpp);

        for (String s: jpp.getCode()) {
            writer.startLine(s);
        }
        writer.startLine( String.format("return %s;", value) );

        writer.unindent();
        writer.startLine("}"); // invoke function

        writer.unindent();
        writer.startLine("}"); // class

        return string.toString();
    }

    private void writeImports( TabPrintWriter writer ){
        writer.startLine(String.format("import %s;", NODES));
        writer.startLine(String.format("import %s;", NODES_UTIL));
        writer.startLine(String.format("import %s;", JAVA_UTIL));
        writer.startLine(String.format("import edu.rice.cs.plt.tuple.Option;"));
    }
}
