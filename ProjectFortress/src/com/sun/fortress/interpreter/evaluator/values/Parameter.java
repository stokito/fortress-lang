/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.values;

import com.sun.fortress.interpreter.evaluator.types.FType;

public class Parameter {
    String param_name;
    FType param_type;
    boolean is_mutable;

    public Parameter(String pname, FType ptype, boolean mutable) {
        param_name = pname;
        param_type = ptype;
        is_mutable = mutable;
    }

    public String getName() {
        return param_name;
    }

    public FType getType() {
        return param_type;
    }

    public boolean getMutable() {
        return is_mutable;
    }

    public String toString() {
        return param_name + ":" + param_type;
    }

    public int hashCode() {
        return super.hashCode();
    }

    public boolean equals(Object o) {
        if (o instanceof Parameter) {
            Parameter p = (Parameter) o;
            if (!param_type.equals(p.param_type)) return false;
            return is_mutable == p.is_mutable;
        }
        return false;
    }
}
