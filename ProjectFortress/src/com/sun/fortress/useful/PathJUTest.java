/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

import junit.framework.TestCase;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class PathJUTest extends TestCase {
    public static void main(String[] args) {
        junit.swingui.TestRunner.run(PathJUTest.class);
    }

    public void testCreate() {
        new Path(".:..");
    }

    public void testInappropriate() {
        Path p = new Path(".:..");
        try {
            p.findFile(".");

        }
        catch (FileNotFoundException e) {
            System.err.println(e);
        }
    }

    public void testMissing() {
        Path p = new Path(".:..");
        try {
            p.findFile("thisfileismissing");

        }
        catch (FileNotFoundException e) {
            System.err.println(e);
        }
    }

    public void testFound() {
        Path p = new Path(".:..");
        try {
            p.findFile("README.txt");
        }
        catch (FileNotFoundException e) {
            System.err.println(e);
        }
        try {
            File f = p.findFile("tests/ProjectFortress/a/b/c/d/e.f");
            System.err.println("Found " + f.getCanonicalPath());
        }
        catch (FileNotFoundException e) {
            System.err.println(e);
        }
        catch (IOException e) {
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
