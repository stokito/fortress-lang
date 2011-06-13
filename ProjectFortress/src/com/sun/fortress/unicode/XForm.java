/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.unicode;


abstract class XForm extends NamedXForm {
    XForm(String s) {
        super(s);
    }

    abstract String translate(String x);

}
