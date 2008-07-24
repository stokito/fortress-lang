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

import org.objectweb.asm.Type;

import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.values.FValue;

/**
 * From the Fortress Language Specification Version 1.0, Section 7.2:
 *
 *     "Fortress supports three namespaces, one for types, one for values,
 *  and one for labels. (If we consider the Fortress component system,
 *  there is another namespace for APIs.) These namespaces are logically
 *  disjoint: names in one namespace do not conflict with names in another."
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

    public String namespace() { return namespace; }
    public String internalName() { return internalName; }
    public String descriptor() { return 'L' + internalName + ';' ; }
}
