package com.sun.fortress.compiler.environments;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import junit.framework.TestCase;

import com.sun.fortress.compiler.Fortress;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.interpreter.drivers.ProjectProperties;
import com.sun.fortress.interpreter.evaluator.BaseEnv;
import com.sun.fortress.interpreter.evaluator.values.FInt;
import com.sun.fortress.shell.CacheBasedRepository;
import com.sun.fortress.useful.Path;

public class TopLevelEnvGenJUTest extends TestCase {

	   BaseEnv environment;

	    /* (non-Javadoc)
	     * @see junit.framework.TestCase#setUp()
	     */
	    @Override
	    protected void setUp() throws Exception {
            compileTestProgram();  
			loadEnvironment();            
	    }	    

	    public void testNameMangling() {
	    	String input = "/.;$<>[]:\\";
	    	String mangledInput = "\\|\\,\\?\\%\\^\\_\\{\\}\\!\\-";
	    	assertEquals(TopLevelEnvGen.mangleIdentifier(input), mangledInput);
	    	input = "hello" + input;
	    	mangledInput = "\\=" + "hello" + mangledInput;
	    	assertEquals(TopLevelEnvGen.mangleIdentifier(input), mangledInput);	    	
	    }
	    
	    public void testNullGetters() {
			assertNull(environment.getBoolNull("run"));
			assertNull(environment.getIntNull("run"));			
			assertNull(environment.getNatNull("run"));			
	    }

	    public void testNullSetters() {
	    	environment.putBoolRaw("run", true);
	    	environment.putIntRaw("run", 0);
	    	environment.putNatRaw("run", 0);	    	
	    }

	    
	    
	    public void testGetPutValueRaw() {

			FInt three = FInt.make(3);
			FInt seven = FInt.make(7);
			FInt thirteen = FInt.make(13);
			FInt a = FInt.make((int) '$');
			FInt b = FInt.make((int) 'K');
			FInt c = FInt.make((int) 'l');
			
			environment.putValueRaw("run", three);
			environment.putValueRaw("$", a);
			environment.putValueRaw("K", b);			
			environment.putValueRaw("l", c);

			// Now test hash collisions
			environment.putValueRaw("Aa", seven);
			environment.putValueRaw("BB", thirteen);
			
			assertEquals(environment.getValueRaw("run"), three);
			assertEquals(environment.getValueRaw("$"), a);
			assertEquals(environment.getValueRaw("K"), b);
			assertEquals(environment.getValueRaw("l"), c);
			
			assertEquals(environment.getValueRaw("Aa"), seven);			
			assertEquals(environment.getValueRaw("BB"), thirteen);
			assertNull(environment.getValueRaw("Chupacabra"));			
			
	    }

		private void loadEnvironment() throws IOException, 
		InstantiationException, IllegalAccessException {
			SimpleClassLoader classLoader = new SimpleClassLoader();
			File classfile = new File(ProjectProperties.BYTECODE_CACHE_DIR + 
					File.separator + "TestCompiledEnvironmentsEnv.class");
			byte[] bytecode = new byte[(int) classfile.length()];
			FileInputStream classStream = new FileInputStream(classfile);
			int read = classStream.read(bytecode);
			if (read != classfile.length()) {
				fail("Expected to read " + classfile.length() + " bytes but only read " + read + " bytes.");
			}
			
			Class generatedClass = classLoader.defineClass("TestCompiledEnvironmentsEnv", bytecode);
			Object envObject = generatedClass.newInstance();
			environment = (BaseEnv) envObject;
		}

		private void compileTestProgram() {
			Fortress fortress = new Fortress(new CacheBasedRepository(ProjectProperties.ANALYZED_CACHE_DIR));

            Path path = ProjectProperties.SOURCE_PATH;
            String s = ProjectProperties.BASEDIR + "tests" + 
            	File.separator + "TestCompiledEnvironments.fss";
            
            File file = new File(s);
            s = file.getPath();

            if (s.contains(File.separator)) {
                String head = s.substring(0, s.lastIndexOf(File.separator));
                s = s.substring(s.lastIndexOf(File.separator)+1, s.length());
                path = path.prepend(head);
            }                      
            
            Iterable<? extends StaticError> errors = fortress.compile(path, s);
            
            for (StaticError error: errors) {
                fail(error.toString());
            }
		}	    
}
