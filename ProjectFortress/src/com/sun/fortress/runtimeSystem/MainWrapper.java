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
            //Class cl = Class.forName(whatToRun, true, InstantiatingClassloader.ONLY);
            Class cl = Class.forName(whatToRun);
            Class[] arg_classes = new Class[1];
            arg_classes[0] = String[].class;
            java.lang.reflect.Method m = cl.getDeclaredMethod("main", arg_classes);
            m.invoke(null, (Object) subargs);
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            System.err.println("Could not load " + whatToRun);
            e.printStackTrace();
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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
