/*******************************************************************************
 Copyright now,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.environments;

import com.sun.fortress.compiler.NamingCzar;
import com.sun.fortress.interpreter.evaluator.BaseEnv;
import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.useful.Useful;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class SimpleClassLoader extends ClassLoader {

    static SimpleClassLoader aLoader = new SimpleClassLoader();

    static Set<String> reloadEnvs = new HashSet<String>();

    public Class<?> defineClass(String name, byte[] b) {
        return defineClass(name, b, 0, b.length);
    }

    public static void reloadEnvironment(String name) {
        reloadEnvs.add(name);
    }

    /**
     * @param className
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     * @throws Error
     */
    private static byte[] getTheBytes(String className) throws FileNotFoundException, IOException, Error {

        FileInputStream classStream = new FileInputStream(ProjectProperties.ENVIRONMENT_CACHE_DIR + File.separator + className + ".class");
        int expected_length = classStream.available();
        byte[] bytecode = new byte[expected_length];
        int read = classStream.read(bytecode);
        if (read != expected_length) {
            throw new Error("Expected to read " + expected_length + " bytes but read " + read + " bytes instead.");
        }
        return bytecode;
    }


    public static BaseEnv loadEnvironment(String fortressFileName, boolean isApi) throws IOException, InstantiationException, IllegalAccessException {
        String className = fortressFileName;
        if (isApi) {
            className = NamingCzar.classNameForApiEnvironment(className);
        } else {
            className = NamingCzar.classNameForComponentEnvironment(className);
        }


        byte[] bytecode = getTheBytes(className);

        SimpleClassLoader classLoader = aLoader; // new SimpleClassLoader();

        if (reloadEnvs.contains(fortressFileName)) {
            classLoader = new SimpleClassLoader();
            aLoader = classLoader;
            reloadEnvs.clear();
        }

        BaseEnv envObject = (BaseEnv) defineAsNecessaryAndAllocate(className, bytecode);

        return (envObject);
    }

    /**
     * @param className
     * @param bytecode
     * @param classLoader
     * @return
     */
    public static Class<?> defineAsNecessary(String className, byte[] bytecode) {
        className = Useful.replace(className, "/", ".");
        Class<?> generatedClass = aLoader.findLoadedClass(className);
        if (generatedClass == null) {
            generatedClass = aLoader.defineClass(className, bytecode);
        }
        return generatedClass;
    }

    public static Object defineAsNecessaryAndAllocate(String className, byte[] bytecode) throws InstantiationException, IllegalAccessException {
        return defineAsNecessary(className, bytecode).newInstance();
    }


}
