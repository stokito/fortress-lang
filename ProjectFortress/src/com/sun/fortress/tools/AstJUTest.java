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

package com.sun.fortress.tools;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.Test;

import java.util.Arrays;

import java.io.FilenameFilter;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.IOException;

import com.sun.fortress.nodes.Node;

import com.sun.fortress.nodes_util.NodeFactory;

import com.sun.fortress.compiler.Parser;

import com.sun.fortress.repository.ProjectProperties;

public class AstJUTest extends TestCase {

    private static final char SEP = File.separatorChar;

    private String file;

    public AstJUTest( String file ){
        super(file);
        this.file = file;
    }

    @Override public void runTest() throws FileNotFoundException, IOException {
        File f = new File(file);
        assertEquals(parse(f), parse(unparse(parse(f))));
    }

    private Node parse(String buffer) throws IOException {
        return Parser.parseString(NodeFactory.makeAPIName(file), buffer);
    }

    private Node parse(File file) throws FileNotFoundException, IOException {
        return Parser.parseFile(NodeFactory.makeAPIName(file.getName()), file);
    }

    private String unparse(Node node){
        return node.accept( new FortressAstToConcrete() );
    }

    private static Iterable<String> allTests( String dir ){
        return Arrays.asList(new File(dir).list( new FilenameFilter(){
            public boolean accept( File dir, String name ){
                return name.endsWith( ".fss" ) ||
                       name.endsWith( ".fsi" );
            }
        }));
    }

    public static Test suite() throws IOException {
       String tests = ProjectProperties.FORTRESS_AUTOHOME + "/ProjectFortress/tests";
       String library = ProjectProperties.FORTRESS_AUTOHOME + "/Library";
       String demos = ProjectProperties.FORTRESS_AUTOHOME + "/ProjectFortress/demos";
       String builtin = ProjectProperties.FORTRESS_AUTOHOME + "/ProjectFortress/LibraryBuiltin";
       TestSuite suite = new TestSuite("Parses all .fss and .fsi files in ProjectFortress/tests" );
       String[] dirs = new String[]{ tests, library, demos, builtin };
       for ( String dir : dirs ){
           for ( String file : allTests(dir) ){
               suite.addTest( new AstJUTest(dir + SEP + file) );
           }
       }
       return suite;
    }
}
