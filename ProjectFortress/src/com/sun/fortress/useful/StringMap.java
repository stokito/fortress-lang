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

package com.sun.fortress.useful;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Properties;

public interface StringMap {
   String get(String s);
   boolean isEmpty();
   
   static public class FromEnv implements StringMap {
       public String get(String s) {
           s = asEnvOrReflect(s);
           String t = System.getenv(s);
           return t;
       }
       public boolean isEmpty() { return false; }
       
    public static String asEnvOrReflect(String s) {
        s = s.toUpperCase();
        s = s.replace('.', '_');
        return s;
    }
   }
   static public class FromProps implements StringMap {
       Properties p;
       FromProps(Properties p) {
           this.p = p;
       }
       public String get(String s) {
           return p.getProperty(s);
       }
       public boolean isEmpty() { return p == null; }
      
   }
   
   static public class FromSysProps implements StringMap {
       public String get(String s) {
    	   return System.getProperty(s);
       }
       public boolean isEmpty() { return false; }
   }
   
   static public class FromFileProps extends FromProps implements StringMap {
       static Properties fromFile(String filename) {
           Properties p = null;
           try {
               BufferedInputStream bis = new BufferedInputStream(new FileInputStream(filename));
               Properties tmp_p = new Properties();
               tmp_p.load(bis);
               p = tmp_p; // Assign if no exception.
           } catch (IOException ex) {
               
           }
           return p;
       }
       public FromFileProps(String filename) {
           super(fromFile(filename));
       }
        
   }
   static public class FromReflection implements StringMap {

       private final Class mapClass;
       
    public FromReflection(Class cl) {
        mapClass = cl;
    }
       
    public String get(String s) {
        try {
            s = FromEnv.asEnvOrReflect(s);
            Field f = mapClass.getDeclaredField(s);
            return f.get(null).toString();
        } catch (SecurityException e) {
            return null;
        } catch (NoSuchFieldException e) {
            return null;
        } catch (IllegalArgumentException e) {
            return null;
        } catch (IllegalAccessException e) {
            return null;
        } catch (NullPointerException e) {
            return null;
        }
    
    }

    public boolean isEmpty() {
        // TODO Auto-generated method stub
        return false;
    }
       
   }
   
   static public class ComposedMaps implements StringMap {
       private final StringMap[] ma;
       public String get(String s) {
           String a = null;
           for (StringMap m : ma) {
               a = m.get(s);
               if (a != null) return a;
           }
           return a;
       }
       public boolean isEmpty() { return ma.length == 0; }
       public ComposedMaps(StringMap... maps) {
           int ne = 0;
           for (StringMap m : maps) {
               if (!m.isEmpty())
                   ne++;
           }
           ma = new StringMap[ne];
           ne = 0;
           for (StringMap m : maps) {
               if (!m.isEmpty()) {
                   ma[ne] = m;
                   ne++;
               }
           }
          
       }
   }
}
