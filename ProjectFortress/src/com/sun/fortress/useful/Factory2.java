/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

public interface Factory2<Part1, Part2, Value> {
    abstract public Value make(Part1 part1, Part2 part2);
}
