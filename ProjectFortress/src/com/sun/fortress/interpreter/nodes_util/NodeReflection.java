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

package com.sun.fortress.interpreter.nodes_util;

import com.sun.fortress.interpreter.nodes.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

abstract public class NodeReflection {
    // For minor efficiency, cache the fields, classes, and constructors.
    // Note that reflection is filtered to exclude certain fields, and to
    // sort them into a particular order.

    public static final String NODES_PACKAGE_PREFIX = "com.sun.fortress.interpreter.nodes.";

    private HashMap<String, HashMap<String, Field>> shortClassNameToFieldNameToField = new HashMap<String, HashMap<String, Field>>();

    private HashMap<String, Class> classMap = new HashMap<String, Class>();

    private HashMap<String, Constructor> constructorMap = new HashMap<String, Constructor>();

    // What happened here is that various forms of node reflection got merged
    // into a
    // common superclass, and the needs of printing and unprintig were not
    // exactly aligned.
    // private HashMap<String, Field[]> fullClassNameToFieldArray = new
    // HashMap<String, Field[]>();
    private HashMap<String, Field[]> shortClassNameToFieldArray = new HashMap<String, Field[]>();

    protected Field fieldFor(String class_name, String field_name) {
        return shortClassNameToFieldNameToField.get(class_name).get(field_name);
    }

    protected Constructor constructorFor(String class_name) {
        return constructorMap.get(class_name);
    }

    protected Constructor constructorFor(Class cls) {
        String sn = modifiedSimpleName(cls);
        Constructor c = constructorMap.get(sn);
        if (c == null) {
            classFor(sn);
            c = constructorMap.get(sn);
        }
        return c;
    }

    protected Field[] fieldArrayFor(String class_name) {
        return shortClassNameToFieldArray.get(class_name);
    }

    abstract protected Constructor defaultConstructorFor(Class cl)
            throws NoSuchMethodException;

    protected Class classFor(String class_name) {
        String full_class_name = NODES_PACKAGE_PREFIX + class_name;
        Class cl = classMap.get(class_name);
        if (cl == null) {
            try {
                cl = Class.forName(full_class_name);
                // TODO Ought to make all leaf classes final, and check for
                // that.
                Field[] fields = NodeReflection.getPrintableFields(cl);
                HashMap<String, Field> h = new HashMap<String, Field>();
                for (int i = 0; i < fields.length; i++) {
                    Field f = fields[i];
                    h.put(f.getName(), f);
                }
                // fullClassNameToFieldArray.put(full_class_name, fields);
                shortClassNameToFieldArray.put(class_name, fields);
                shortClassNameToFieldNameToField.put(class_name, h);
                classMap.put(class_name, cl);
                Constructor c = defaultConstructorFor(cl);
                c.setAccessible(true);
                constructorMap.put(class_name, c);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                throw new Error("Error reading node type " + class_name);
            } catch (SecurityException e) {
                e.printStackTrace();
                throw new Error("Error reading node type " + class_name);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
                throw new Error("Error reading node type " + class_name
                        + ", missing constructor (Span)");
            } catch (NoSuchFieldException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return cl;
    }

    protected static Field[] getPrintableFields(Class cl)
            throws SecurityException, NoSuchFieldException {
        Field[] fields;
        ArrayList<Field> fal = new ArrayList<Field>();
        Class icl = cl;
        while (icl != Node.class && icl != Object.class) {
            fields = icl.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                if ((fields[i].getModifiers() & java.lang.reflect.Modifier.STATIC) == 0
                        // && (fields[i].getModifiers() & java.lang.reflect.Modifier.PRIVATE) == 0
                        && (fields[i].getModifiers() & java.lang.reflect.Modifier.TRANSIENT) == 0) {
                    Field f = fields[i];
                    f.setAccessible(true);
                    fal.add(f);
                }
            }
            icl = icl.getSuperclass();
        }
        if (icl == Node.class) {
            Field f = Node.class.getDeclaredField("props");
            f.setAccessible(true);
            fal.add(f);
        }
        Field[] ifields = new Field[fal.size()];
        ifields = fal.toArray(ifields);
        Arrays.sort(ifields, fieldComparator);
        return ifields;
    }

    protected static Comparator<Field> fieldComparator = new Comparator<Field>() {

        public int compare(Field arg0, Field arg1) {
            Class c0 = arg0.getType();
            Class c1 = arg1.getType();
            if (c0 == List.class && c1 != List.class) {
                return 1;
            }
            if (c0 != List.class && c1 == List.class) {
                return -1;
            }
            String s0 = arg0.getName();
            String s1 = arg1.getName();
            return s0.compareTo(s1);
        }

    };

    protected Field[] getCachedPrintableFields(Class cl) {

        return getCachedPrintableFields(cl, modifiedSimpleName(cl));
    }

    private String modifiedSimpleName(Class cl) {
        return (Modifier.class.isAssignableFrom(cl)) ? "Modifier$"
                + cl.getSimpleName() : cl.getSimpleName();
    }

    protected Field[] getCachedPrintableFields(Class cl, String clname) {
        Field[] fields = shortClassNameToFieldArray.get(clname);
        if (fields == null) {
            try {
                fields = getPrintableFields(cl);
            } catch (SecurityException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            synchronized (shortClassNameToFieldNameToField) {
                shortClassNameToFieldArray.put(clname, fields);
            }
        }
        return fields;
    }

}
