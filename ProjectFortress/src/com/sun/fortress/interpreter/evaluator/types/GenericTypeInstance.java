/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.types;

import com.sun.fortress.interpreter.evaluator.Environment;

import java.util.List;

public interface GenericTypeInstance {
    /**
     * The generic type instantiated by this thing.
     *
     * @return
     */
    public FTypeGeneric getGeneric();

    /**
     * The type parameters of this generic instance, but without opr parameters.
     * Instantiation of opr parameters is implemented with cloning and rewriting
     * instead.
     *
     * @return
     */
    public List<FType> getTypeParams();

    /**
     * This type parameters of this generic instance, including opr parameters.
     * These are necessary to correctly format the name of the generic type.
     *
     * @return
     */
    public List<FType> getTypeParamsForName();

    /**
     * The environment where the instance was (in theory) instantiated.
     * (getEnv may be the wrong name, for now it is handy).
     * For purposes of inferring functional methods in overloadings.
     *
     * @return
     */
    public Environment getWithin();

    public List<FType> getTransitiveExtends();

}
