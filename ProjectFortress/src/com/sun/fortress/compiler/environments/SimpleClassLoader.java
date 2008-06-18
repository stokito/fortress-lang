package com.sun.fortress.compiler.environments;

public class SimpleClassLoader extends ClassLoader {
			
	public Class defineClass(String name, byte[] b) {
		return defineClass(name, b, 0, b.length);
	}

}
