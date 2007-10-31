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

package com.sun.fortress.interpreter.glue.prim;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import com.sun.fortress.useful.Useful;
import com.sun.fortress.interpreter.evaluator.ProgramError;

import static com.sun.fortress.interpreter.evaluator.ProgramError.error;

/**
 * Functions from File.
 */
public class File {

public static final class OutFileOpen extends Util.S2Fw {
    protected BufferedWriter f(String fileName) {
        try {
            BufferedWriter fout = Useful.utf8BufferedFileWriter(fileName);
            return fout;
        } catch (FileNotFoundException ex) {
            return error("FileNotFound: " + fileName);
        }
    }
}

public static final class OutFileWrite extends Util.FwS2V {
    protected void f(BufferedWriter fout, String str) {
        try {
            fout.append(str);
        } catch (IOException ex) {
            error("FileWriteException!");
        }
    }
}

public static final class OutFileClose extends Util.Fw2V {
    protected void f(BufferedWriter fout) {
        try {
            if (fout != null) fout.close();
        } catch (IOException ex) {
            error("FileCloseException!");
        }
    }
}

}
