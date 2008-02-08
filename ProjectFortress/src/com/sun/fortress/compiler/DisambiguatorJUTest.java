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

package com.sun.fortress.compiler;

import java.util.Arrays;
import java.util.List;


public class DisambiguatorJUTest extends StaticTest {

    private final List<String> NOT_PASSING = Arrays.asList(
        staticTests + "XXXMaltypedTopLevelVar.fss",                                                          
        staticTests + "XXXMultipleRefErrors.fss",
        staticTests + "XXXUndefinedArrayRef.fss",
        staticTests + "XXXUndefinedInitializer.fss",
        staticTests + "XXXUndefinedNestedRef.fss",
        staticTests + "XXXUndefinedRefInLoop.fss",
        staticTests + "XXXUndefinedVar.fss",
        staticTests + "XXXUndefinedTopLevelVar.fss",
        staticTests + "stub to eliminate comma trouble"
    );
    
    public List<String> getNotPassing() {
        return NOT_PASSING;
    }
}
