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

package com.sun.fortress.useful;

import java.io.File;
import java.io.FileNotFoundException;

import junit.framework.TestCase;

public class PathJUTest extends TestCase {
    public static void main(String[] args) {
        junit.swingui.TestRunner.run(PathJUTest.class);
    }
    
    public void testCreate() {
        Path p = new Path(".:..");
    }

    public void testInappropriate() {
        Path p = new Path(".:..");
        try {
            File f = p.findFile(".");
        
        } catch (FileNotFoundException e) {
            System.err.println(e);
        }
    }

    public void testMissing() {
        Path p = new Path(".:..");
        try {
            File f = p.findFile("thisfileismissing");
        
        } catch (FileNotFoundException e) {
            System.err.println(e);
        }
    }
    
    public void testFound() {
        Path p = new Path(".:..");
        try {
            File f = p.findFile("README.txt");
        } catch (FileNotFoundException e) {
            System.err.println(e);
        }
    }
    
    public void testSubstitute() {
        String s1 = "nosub";
        String s2 = "${java.version}";
        String s3 = "Path is ${PATH}";
        String s4 = "${HOME} is home";
        String s5 = "${java.version}${java.version} ${java.version}";
        System.err.println(Useful.substituteVars(s1));
        System.err.println(Useful.substituteVars(s2));
        System.err.println(Useful.substituteVars(s3));
        System.err.println(Useful.substituteVars(s4));
        System.err.println(Useful.substituteVars(s5));
    
    }
    

}
