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

package com.sun.fortress.interpreter.evaluator.types;

import java.util.List;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.nodes.Type;

public interface GenericTypeInstance {
    /**
     * The generic type instantiated by this thing.
     * @return
     */
    public FTypeGeneric getGeneric();

    /**
     * The type parameters of this generic instance, but without opr parameters.
     * Instantiation of opr parameters is implemented with cloning and rewriting
     * instead.
     *  @return
     */
    public List<FType> getTypeParams();
    /**
     * This type parameters of this generic instance, including opr parameters.
     * These are necessary to correctly format the name of the generic type.
     * @return
     */
    public List<FType> getTypeParamsForName();

    /**
     * The environment where the instance was (in theory) instantiated.
     * (getEnv may be the wrong name, for now it is handy).
     * For purposes of inferring functional methods in overloadings.
     * @return
     */
    public BetterEnv getEnv();

    public List<FType> getTransitiveExtends();

}
