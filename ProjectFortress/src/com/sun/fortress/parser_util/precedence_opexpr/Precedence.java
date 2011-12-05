/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.parser_util.precedence_opexpr;


public interface Precedence {

    public <RetType> RetType accept(PrecedenceVisitor<RetType> visitor);

    public void accept(PrecedenceVisitor_void visitor);

    public void outputHelp(TabPrintWriter writer);
}
