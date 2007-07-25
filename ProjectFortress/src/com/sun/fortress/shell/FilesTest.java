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

import junit.framework.TestCase;
import java.io.*;

/**
 * A JUnit test case class.
 * Every method starting with the word "test" will be called when running
 * the test with JUnit.
 */
public class FilesTest extends TestCase {
   public static final String TEMP_DIR = System.getProperty("java.io.tmpdir") + File.separator;

   public void testRm() throws IOException {
      File testFile = File.createTempFile("testRm",".tmp");
      Files.rm(testFile.getPath());
      assert(! testFile.exists());
   }

   public void testMkdir() {
      String fileName = TEMP_DIR + "testMkdir";
      Files.mkdir(fileName);
      File dir = new File(fileName);

      try {
         assert(dir.exists());
         assert(dir.isDirectory());
      }
      finally {
         Files.rm(dir.getPath());
      }
   }

   public void testMv() throws IOException {
      String testDirName = TEMP_DIR + "testCp";
      Files.mkdir(testDirName);

      File testFile = File.createTempFile("testRm", ".tmp");
      String movedFilePath = testDirName + testFile.getName();
      Files.mv(testFile.getPath(), movedFilePath);
      File movedFile = new File(movedFilePath);

      try {
         assert(movedFile.exists());
         assert(movedFile.isFile());
      }
      finally {
         Files.rm(testDirName);
         Files.rm(testFile.getPath());
      }
   }

   public void testCp() throws IOException {
      String testDirName = TEMP_DIR + "testCp";
      Files.mkdir(testDirName);

      File testFile = File.createTempFile("testRm", ".tmp");
      Files.cp(testFile.getPath(), testDirName + testFile.getName());
      File copy = new File(testDirName + testFile.getName());

      try {
         assert(copy.exists());
         assert(copy.isFile());
      }
      finally {
         Files.rm(testDirName);
         Files.rm(testFile.getPath());
      }
   }
}
