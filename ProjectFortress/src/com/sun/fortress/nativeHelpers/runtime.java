/*******************************************************************************
 Copyright 2009,2010, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.nativeHelpers;
import com.sun.fortress.runtimeSystem.FortressExecutable;

public class runtime {
    public static int loopChunk()  { return FortressExecutable.loopChunk;}
    public static int numThreads() { return FortressExecutable.numThreads;}
}
