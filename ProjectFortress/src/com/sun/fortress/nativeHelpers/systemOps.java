/*******************************************************************************
 Copyright 2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.nativeHelpers;

import com.sun.fortress.repository.ProjectProperties;

public class systemOps {
    public static String getProperty(String what, String ifMissing) {
        return ProjectProperties.get(what, ifMissing);
    }

}
