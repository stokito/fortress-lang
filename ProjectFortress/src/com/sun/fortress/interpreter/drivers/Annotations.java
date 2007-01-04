/*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
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

package com.sun.fortress.interpreter.drivers;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Annotations  {

    public boolean compile;

    public Annotations(String fname) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(fname));
        String str = br.readLine();
        if (str.startsWith("(* COMPILE *)"))
            compile = true;
        else
            compile = false;
    }

}
