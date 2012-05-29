/*******************************************************************************
    Copyright 2008, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler;

import java.util.List;

import com.sun.fortress.interpreter.evaluator.values.FObject;
import com.sun.fortress.interpreter.evaluator.values.FValue;

/**
 * A FortressClosure is the standard interface for invoking
 * Fortress-level code.  There might need to be some "fetch type
 * information" functionality here, once we have a consistent story on
 * how run-time type information is being represented.
 *
 * Self parameters are always *first*, even for functional method
 * implementations that have a non-first self parameter (the self
 * parameter is squeezed out of the remaining parameters).
 */
public interface FortressClosure {

    /**
     * Entry point for interpreter, and for varargs / high arg counts.
     * Note that this method is responsible for all arity checking that
     * might need to occur.
     */
    public FValue applyToArgs(List<FValue> args);

    /**
     * 0-arg entry point
     */
    public FValue applyToArgs();

    /**
     * Single-arg entry point, with tuple unwrapping if needed.
     */
    public FValue applyToArgs(FValue a);

    /**
     * 2-arg entry point, with tuple unwrapping for methods if needed.
     */
    public FValue applyToArgs(FValue a, FValue b);

    /**
     * 3-arg entry point
     */
    public FValue applyToArgs(FValue a, FValue b, FValue c);

    /**
     * 4-arg entry point
     */
    public FValue applyToArgs(FValue a, FValue b, FValue c, FValue d);

}
