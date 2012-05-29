/*******************************************************************************
    Copyright 2009,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/
package com.sun.fortress.runtimeSystem;

import java.lang.reflect.InvocationTargetException;

import com.sun.fortress.useful.Useful;

public class MainWrapper {

    public static ClassLoader icl;
    
    /**
     * @param args
     * @throws ClassNotFoundException
     */
    public static void main(String[] args) {
        int l = args.length;
        if (l == 0) {
            System.err.println("The wrapper class needs the name of a class to run.");
            return;
        }

        String[] subargs = new String[l-1];
        System.arraycopy(args, 1, subargs, 0, l-1);

        String whatToRun = args[0];

        if (whatToRun.indexOf('\\') >= 0 ||
            whatToRun.indexOf('/') >= 0) {
            if (whatToRun.endsWith(".class")) {
                /* If . is on the classpath, which is common, this allows
                 * use of filename completion to locate the file, if it is
                 * not cached.
                 */
                whatToRun = Useful.substring(whatToRun, 0, -6);
            }
            /*
             * Graceful behavior on windows REQUIRES handling both / and \
             * as pathname separators; both work at the OS level.
             * This does inhibit use of backslashes in the names of classes
             * on Unix machines, but such classfile names are not portable to
             * windows, and are also very rare.
             */
            whatToRun = sepToDot(whatToRun);
        }


        try {
            String cp = System.getProperty("java.class.path");
            
            // ensure that FortressExecutable is loaded and statically initialized
            // so that there is a task pool before any user code is run.
            icl = InstantiatingClassloader.ONLY;
            Class cl = Class.forName(whatToRun, true, icl);
            java.lang.reflect.Method m = cl.getDeclaredMethod("main", String[].class);
            try {
                m.invoke(null, (Object) subargs);
            } finally {
                InstantiatingClassloader.exitProgram();
            }
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            System.err.println("Could not load " + whatToRun);
            e.printStackTrace();
            System.exit(1);
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.exit(1);
        } catch (NoSuchMethodException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.exit(1);
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.exit(1);
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.exit(1);
        } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.getCause().printStackTrace();
            System.exit(1);
//        } catch (NoSuchFieldException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//            System.exit(1);
        }

    }

    /**
     * @param filepath
     * @return
     */
    public static String sepToDot(String filepath) {
        filepath = filepath.replace('/', '.');
        filepath = filepath.replace('\\', '.');
        return filepath;
    }

}
