/*******************************************************************************
    Copyright 2009 Sun Microsystems, Inc.,
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
package com.sun.fortress.runtimeSystem;

import java.lang.reflect.InvocationTargetException;

import com.sun.fortress.useful.Useful;

public class MainWrapper {

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
            // ensure that FortressExecutable is loaded and statically initialized
            // so that there is a task pool before any user code is run.
            ClassLoader icl = InstantiatingClassloader.ONLY;
            Class cl = Class.forName(whatToRun, true, icl);
            Class arr = Class.forName("\\=Arrow\u27e6\u26a0CompilerLibrary\\%ReductionString\\?\u26a0CompilerBuiltin\\%String\u27e7", true, icl);
            Class red = Class.forName("fortress.CompilerLibrary$ReductionString", true, icl);
            java.lang.reflect.Method ap = arr.getDeclaredMethod("apply", red);
            java.lang.reflect.Method m = cl.getDeclaredMethod("main", String[].class);
            m.invoke(null, (Object) subargs);
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
