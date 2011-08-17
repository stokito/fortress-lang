/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/
package com.sun.fortress.repository;

import java.io.IOException;

public interface IO<Name, Data> {
    public Data read(Name name) throws IOException;

    public void write(Name name, Data data) throws IOException;

    public long lastModified(Name name);
}
