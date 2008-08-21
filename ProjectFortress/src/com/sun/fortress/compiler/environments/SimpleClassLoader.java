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

package com.sun.fortress.compiler.environments;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import com.sun.fortress.interpreter.evaluator.BaseEnv;
import com.sun.fortress.repository.ProjectProperties;

public class SimpleClassLoader extends ClassLoader {

    static SimpleClassLoader aLoader = new SimpleClassLoader();
    
    public Class<?> defineClass(String name, byte[] b) {
        return defineClass(name, b, 0, b.length);
    }

    public static BaseEnv loadEnvironment(String fortressFileName, boolean isApi) 
                                    throws IOException, InstantiationException, IllegalAccessException {
    	String className = "";
    	if(isApi) {
    		className = fortressFileName + TopLevelEnvGen.API_ENV_SUFFIX;
    	}
    	else {
    		className = fortressFileName + TopLevelEnvGen.COMPONENT_ENV_SUFFIX;
    	}
    	className = TopLevelEnvGen.mangleClassIdentifier(className);
    	
        SimpleClassLoader classLoader = aLoader; // new SimpleClassLoader();
        File classfile = new File(ProjectProperties.BYTECODE_CACHE_DIR +
                                  File.separator + className + ".class");
        byte[] bytecode = new byte[(int) classfile.length()];
        FileInputStream classStream = new FileInputStream(classfile);
        int read = classStream.read(bytecode);
        if (read != classfile.length()) {
            throw new Error("Expected to read " + classfile.length() + " bytes but read " + read + " bytes instead.");
        }
        Class<?> generatedClass = classLoader.findLoadedClass(className);
        if (generatedClass == null)
            generatedClass = classLoader.defineClass(className, bytecode);
        BaseEnv envObject = (BaseEnv) generatedClass.newInstance();
        
        return(envObject);
    }

}
