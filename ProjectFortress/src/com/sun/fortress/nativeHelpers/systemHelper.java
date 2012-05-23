/*******************************************************************************
 Copyright 2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.nativeHelpers;
import com.sun.fortress.compiler.runtimeValues.FStringVector;
import java.lang.reflect.*;


// This is probably bad juju using reflection to set a final field, but
// it seems like the right thing in this case.

public class systemHelper {

    static void setFinalStatic(Field field, Object newValue) throws Exception {
      field.setAccessible(true);

      Field modifiersField = Field.class.getDeclaredField("modifiers");
      modifiersField.setAccessible(true);
      modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

      field.set(null, newValue);
    }

    public static void registerArgs(String[] args) {
        try {

        Class cl = Class.forName("CompilerSystem$args");
        Field f = cl.getDeclaredField("ONLY");

        setFinalStatic(f, new FStringVector(args));

        } catch (Exception e) {
            //            throw new RuntimeException("Error setting CompilerSystem args" + e);
            // If CompilerSystem isn't found then we don't need it ?
        }
    }

}
