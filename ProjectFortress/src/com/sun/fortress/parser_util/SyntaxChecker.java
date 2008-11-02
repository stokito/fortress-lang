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
package com.sun.fortress.parser_util;

import java.io.BufferedWriter;
import java.io.IOException;
/*
import java.util.List;
import java.util.ArrayList;
*/
import com.sun.fortress.nodes.*;
import com.sun.fortress.useful.Useful;
/*
import edu.rice.cs.plt.tuple.Option;
*/
import static com.sun.fortress.exceptions.ProgramError.error;

/**
 * A visitor that checks syntactic restrictions:
 * 1) Declarations in APIs should not have any missing types.
 */
public final class SyntaxChecker extends NodeDepthFirstVisitor_void {
    private BufferedWriter writer;

    public SyntaxChecker( String file ) {
        try {
            writer = Useful.filenameToBufferedWriter( file );
        } catch (IOException error) {
            error("Creating a log file for the syntax checker failed!");
        }
    }

    private void log(Node that, String message) {
        try {
            writer.write( that.getSpan() + " : " + message + "\n" );
        } catch (IOException error) {
            error("Writing to a log file for the syntax checker failed!");
        }
    }

    public void forComponentOnly(Component that) {
        try {
            writer.close();
        } catch (IOException error) {
            error("Creating a log file for the syntax checker failed!");
        }
    }

    public void forApiOnly(Api that) {
        try {
            writer.close();
        } catch (IOException error) {
            error("Creating a log file for the syntax checker failed!");
        }
    }

    public void forAbsVarDeclOnly(AbsVarDecl that) {
        for (LValueBind lvb : that.getLhs()) {
            if ( lvb.getType().isNone() )
                log(lvb, "The type of " + lvb.getName() + " is required.");
        }
    }

    public void forAbsFnDeclOnly(AbsFnDecl that) {
        if ( that.getReturnType().isNone() )
            log(that, "The return type of " + that.getName() + " is required.");
        for ( Param p : that.getParams() ) {
            if ( p instanceof NormalParam &&
                 ((NormalParam)p).getType().isNone() &&
                 ! p.getName().getText().equals("self") )
                log(p, "The type of " + p.getName() + " is required.");
        }
    }
}
