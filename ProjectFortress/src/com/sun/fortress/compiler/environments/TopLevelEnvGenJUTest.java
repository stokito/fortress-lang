/*******************************************************************************
    Copyright now,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/

package com.sun.fortress.compiler.environments;

import com.sun.fortress.Shell;
import com.sun.fortress.compiler.WellKnownNames;
import com.sun.fortress.compiler.phases.PhaseOrder;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.exceptions.shell.UserError;
import com.sun.fortress.interpreter.env.WorseEnv;
import com.sun.fortress.interpreter.evaluator.BaseEnv;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.types.IntNat;
import com.sun.fortress.interpreter.evaluator.values.FInt;
import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.runtimeSystem.Naming;
import com.sun.fortress.useful.Path;
import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;

public class TopLevelEnvGenJUTest extends TestCase {

    private BaseEnv testCompiledEnv;
    private BaseEnv testCompiledImportEnv;
    private BaseEnv testCompiledNestedImportEnv;
    private BaseEnv testLibraryEnv;

    private String fsiFiles[];
    private String fssFiles[];

    /* (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        Shell.setPhaseOrder(PhaseOrder.interpreterPhaseOrder);
        Shell.setScala(false);
        fssFiles = new String[4];
        fsiFiles = new String[3];

        fssFiles[0] = "TestCompiledEnvironments";
        fssFiles[1] = "TestCompiledImports";
        fssFiles[2] = "TestCompiledNestedImports";
        fssFiles[3] = WellKnownNames.fortressLibrary();

        for (String fssFile : fssFiles) {
            compileTestProgram(fssFile + ".fss");
        }
        testCompiledEnv = SimpleClassLoader.loadEnvironment(fssFiles[0], false);
        testCompiledImportEnv = SimpleClassLoader.loadEnvironment(fssFiles[1], false);
        testCompiledNestedImportEnv = SimpleClassLoader.loadEnvironment(fssFiles[2], false);
        testLibraryEnv = SimpleClassLoader.loadEnvironment(fssFiles[3], false);

        fsiFiles[0] = "AsciiVal";
        fsiFiles[1] = "a.b.NestedOne";
        fsiFiles[2] = "a.b.c.d.NestedTwo";
    }

    public void testConstructor() {
        WorseEnv worseEnv = new WorseEnv();
        assertFalse(worseEnv.isTopLevel());
        assertTrue(testCompiledEnv.isTopLevel());
    }

    public void testNameMangling() {
        String input = "/.;$<>[]:\\%\\";
        String mangledInput = "\\|\\,\\?\\%\\^\\_\\{\\}\\!\\-%\\";
        String s = Naming.mangleIdentifier(input);
        assertEquals(s, mangledInput);
        input = "hello" + input;
        mangledInput = "\\=" + "hello" + mangledInput;
        s = Naming.mangleIdentifier(input);
        assertEquals(s, mangledInput);
    }

    public void testRemoveMethods() {
        IntNat three = IntNat.make(3);
        testCompiledEnv.putTypeRaw("Empty", three);
        assertEquals(testCompiledEnv.getLeafTypeNull("Empty"), three); // leaf
        testCompiledEnv.removeType("Empty");
        assertNull(testCompiledEnv.getLeafTypeNull("Empty")); // leaf

        FInt seven = FInt.make(7);

        testCompiledEnv.putValueRaw("run", seven);
        assertEquals(testCompiledEnv.getValueRaw("run"), seven);
        testCompiledEnv.removeVar("run");
        assertNull(testCompiledEnv.getValueRaw("run"));

    }

    public void testNullGetters() {
        assertNull(testCompiledEnv.getBoolNull("run"));
        assertNull(testCompiledEnv.getIntNull("run"));
        assertNull(testCompiledEnv.getNatNull("run"));
    }

    public void testNullSetters() {
        testCompiledEnv.putBoolRaw("run", true);
        testCompiledEnv.putIntRaw("run", 0);
        testCompiledEnv.putNatRaw("run", 0);
    }

    public void testGetPutApi() throws IOException, InstantiationException, IllegalAccessException {
        FInt val = FInt.make(65);
        String apiName = fsiFiles[0];

        assertNull(testCompiledImportEnv.getApiNull(apiName));
        Environment loadedEnv = SimpleClassLoader.loadEnvironment(fsiFiles[0], true);
        testCompiledImportEnv.putApi(apiName, loadedEnv);
        Environment env = testCompiledImportEnv.getApiNull(apiName);
        assertEquals(loadedEnv, env);

        testLibraryEnv.putValue("false", val);
        env.putValueRaw("A", val);
        assertEquals(val, loadedEnv.getRootValueNull("A"));
    }

    public void testGetPutApiInNestedDir() throws IOException, InstantiationException, IllegalAccessException {
        FInt level = FInt.make(1);

        assertNull(testCompiledNestedImportEnv.getApiNull(fsiFiles[1]));
        assertNull(testCompiledNestedImportEnv.getApiNull(fsiFiles[2]));
        assertNull(testCompiledNestedImportEnv.getApiNull("NonExistentApi"));

        Environment loadedLevel1Api = SimpleClassLoader.loadEnvironment(fsiFiles[1], true);
        Environment loadedLevel2Api = SimpleClassLoader.loadEnvironment(fsiFiles[2], true);
        testCompiledNestedImportEnv.putApi(fsiFiles[1], loadedLevel1Api);
        testCompiledNestedImportEnv.putApi(fsiFiles[2], loadedLevel2Api);

        Environment env1 = testCompiledNestedImportEnv.getApiNull(fsiFiles[1]);
        Environment env2 = testCompiledNestedImportEnv.getApiNull(fsiFiles[2]);

        assertEquals(loadedLevel1Api, env1);
        assertEquals(loadedLevel2Api, env2);

        env1.putValueRaw("level1", level);
        env2.putValueRaw("level2", level);

        assertEquals(level, loadedLevel1Api.getRootValueNull("level1"));
        assertEquals(level, loadedLevel2Api.getRootValueNull("level2"));
    }

    public void testGetPutTypeRaw() {
        IntNat three = IntNat.make(3);
        IntNat a = IntNat.make((int) '$');
        IntNat b = IntNat.make((int) 'K');
        IntNat c = IntNat.make((int) 'l');

        testCompiledEnv.putTypeRaw("Empty", three);
        testCompiledEnv.putTypeRaw("Empty" + '\u05D0', a);
        testCompiledEnv.putTypeRaw("Empty" + '\u05D1', b);
        testCompiledEnv.putTypeRaw("Empty" + '\u05D2', c);

        assertEquals(testCompiledEnv.getLeafTypeNull("Empty"), three); // leaf
        assertEquals(testCompiledEnv.getLeafTypeNull("Empty" + '\u05D0'), a); // leaf
        assertEquals(testCompiledEnv.getLeafTypeNull("Empty" + '\u05D1'), b); // leaf
        assertEquals(testCompiledEnv.getLeafTypeNull("Empty" + '\u05D2'), c); // leaf

        assertNull(testCompiledEnv.getLeafTypeNull("Chupacabra")); // leaf
    }

    public void testGetPutValueRaw() {

        FInt three = FInt.make(3);
        FInt seven = FInt.make(7);
        FInt thirteen = FInt.make(13);
        FInt a = FInt.make((int) '$');
        FInt b = FInt.make((int) 'K');
        FInt c = FInt.make((int) 'l');

        testCompiledEnv.putValueRaw("run", three);
        testCompiledEnv.putValueRaw("$", a);
        testCompiledEnv.putValueRaw("K", b);
        testCompiledEnv.putValueRaw("l", c);

        // Now test hash collisions
        testCompiledEnv.putValueRaw("Aa", seven);
        testCompiledEnv.putValueRaw("BB", thirteen);

        assertEquals(testCompiledEnv.getValueRaw("run"), three);
        assertEquals(testCompiledEnv.getValueRaw("$"), a);
        assertEquals(testCompiledEnv.getValueRaw("K"), b);
        assertEquals(testCompiledEnv.getValueRaw("l"), c);

        assertEquals(testCompiledEnv.getValueRaw("Aa"), seven);
        assertEquals(testCompiledEnv.getValueRaw("BB"), thirteen);
        assertNull(testCompiledEnv.getValueRaw("Chupacabra"));

    }

    public void testDump() throws IOException {
        FInt three = FInt.make(3);
        FInt a = FInt.make((int) '$');
        FInt b = FInt.make((int) 'K');
        FInt c = FInt.make((int) 'l');

        testCompiledEnv.putValueRaw("run", three);
        testCompiledEnv.putValueRaw("$", a);
        testCompiledEnv.putValueRaw("K", b);
        testCompiledEnv.putValueRaw("l", c);

        StringBuilder buffer = new StringBuilder();
        testCompiledEnv.verboseDump = true;
        testCompiledEnv.dump(buffer);
    }

    private void compileTestProgram(String testFileName) throws UserError {
        Path path = ProjectProperties.SOURCE_PATH;
        String s = ProjectProperties.BASEDIR + "tests" + File.separator + testFileName;

        //        System.err.println("compileTestProgram(" + s + ")");

        File file = new File(s);
        s = file.getPath();

        if (s.contains(File.separator)) {
            String head = s.substring(0, s.lastIndexOf(File.separator));
            s = s.substring(s.lastIndexOf(File.separator) + 1, s.length());
            path = path.prepend(head);
        }

        // HACK: We need to compile these test programs using the old Fortress
        // libraries instead of the new compiler libraries.
        // Shell.useFortressLibraries();

        Iterable<? extends StaticError> errors = Shell.compilerPhases(path, s);

        for (StaticError error : errors) {
            fail(error.toString());
        }
    }
}
