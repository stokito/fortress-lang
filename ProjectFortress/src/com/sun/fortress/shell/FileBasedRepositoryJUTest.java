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

package com.sun.fortress.shell;

import java.io.IOException;
import java.io.File;
import junit.framework.TestCase;

import com.sun.fortress.compiler.*;
import com.sun.fortress.compiler.index.*;
import com.sun.fortress.interpreter.drivers.*;
import com.sun.fortress.nodes.*;

import edu.rice.cs.plt.tuple.Option;

import java.io.*;
import java.util.*;

import static com.sun.fortress.shell.ConvenientStrings.*;

public class FileBasedRepositoryJUTest extends TestCase {

    private static FileBasedRepository repository;

    public void testNothing() {}

//    public void setUp() throws IOException {
//        repository = new FileBasedRepository(".");
//    }
//
//    public void testStandardLibrary() throws IOException {
//        ArrayList<Id> ids = new ArrayList<Id>();
//        ids.add(new Id("FortressLibrary"));
//        assertNotNull(repository.getComponent(new APIName(ids)));
//    }
//
//    public void testAddApi() throws IOException {
//        Fortress fortress = new Fortress(repository);
//        Iterable<? extends StaticError> errors = fortress.compile(new File("tests/TupleBinding.fss"));
//        for (StaticError error: errors) { System.err.println(error); }
//
//        ArrayList<Id> ids = new ArrayList<Id>();
//        ids.add(new Id("TupleBinding"));
//        assertNotNull(repository.getApi(new APIName(ids)));
//    }
//
//    public void testAddComponent() throws IOException {
//        Fortress fortress = new Fortress(repository);
//        Iterable<? extends StaticError> errors = fortress.compile(new File("tests/TupleBinding.fss"));
//        for (StaticError error: errors) { System.err.println(error); }
//
//        ArrayList<Id> ids = new ArrayList<Id>();
//        ids.add(new Id("TupleBinding"));
//        assertNotNull(repository.getApi(new APIName(ids)));
//    }
}
