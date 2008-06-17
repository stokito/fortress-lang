package com.sun.fortress.compiler.environments;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;

import com.sun.fortress.interpreter.evaluator.BaseEnv;
import com.sun.fortress.interpreter.evaluator.values.FInt;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.env.WorseEnv;

public class TopLevelEnvGenTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			SimpleClassLoader classLoader = new SimpleClassLoader();
			File classfile = new File("MyTests\\classes\\TestCompiledEnvironmentsEnv.class");
			byte[] bytecode = new byte[(int) classfile.length()];
			FileInputStream classStream = new FileInputStream(classfile);
			int read = classStream.read(bytecode);
			if (read != classfile.length()) {
				System.err.println("Only read " + read + " bytes");
			}
			Class generatedClass = classLoader.defineClass("TestCompiledEnvironmentsEnv", bytecode);
			Object environment = generatedClass.newInstance();
			BaseEnv baseEnv = (BaseEnv) environment;

			baseEnv.putValueUnconditionally("run", FInt.make(3));

			// Now test hash collisions
			baseEnv.putValueUnconditionally("Aa", FInt.make(7));			
			baseEnv.putValueUnconditionally("BB", FInt.make(13));						

			System.out.println(baseEnv.getValueRaw("run"));
			System.out.println(baseEnv.getValueRaw("Aa"));
			System.out.println(baseEnv.getValueRaw("BB"));			
			System.out.println(baseEnv.getValueRaw("michael"));			
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassFormatError e) {
			e.printStackTrace();			
		}
	}

}
