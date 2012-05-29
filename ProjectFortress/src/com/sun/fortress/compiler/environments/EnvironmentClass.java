/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.environments;

import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import org.objectweb.asm.Type;

/**
 * From the Fortress Language Specification Version 1.0, Section 7.2:
 * <p/>
 * "Fortress supports three namespaces, one for types, one for values,
 * and one for labels. (If we consider the Fortress component system,
 * there is another namespace for APIs.) These namespaces are logically
 * disjoint: names in one namespace do not conflict with names in another."
 */
public enum EnvironmentClass {
    FTYPE("$FType", Type.getType(FType.class).getInternalName()),
    FVALUE("$FValue", Type.getType(FValue.class).getInternalName()),
    ENVIRONMENT("$Api", Type.getType(Environment.class).getInternalName());

    private final String namespace;
    private final String internalName;

    EnvironmentClass(String namespace, String internalName) {
        this.namespace = namespace;
        this.internalName = internalName;
    }

    public String namespace() {
        return namespace;
    }

    public String internalName() {
        return internalName;
    }

    public String descriptor() {
        return 'L' + internalName + ';';
    }
}
