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
 * 1) A declaration in an API should not have any missing types.
 * 2) An operator method that is not a subscripting operator method
 *    nor a subscripted assignment operator method
 *    should have the self parameter.
 */
public final class SyntaxChecker extends NodeDepthFirstVisitor_void {
    private boolean inTrait = false;
    private boolean inObject = false;
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

    public void forTraitDecl(TraitDecl that) {
        inTrait = true;
        super.forTraitDecl( that );
        inTrait = false;
    }

    public void forObjectDecl(ObjectDecl that) {
        inObject = true;
        super.forObjectDecl( that );
        inObject = false;
    }

    public void forAbsVarDeclOnly(AbsVarDecl that) {
        for (LValueBind lvb : that.getLhs()) {
            if ( lvb.getType().isNone() )
                log(lvb, "The type of " + lvb.getName() + " is required.");
        }
    }

    public void forAbsFnDeclOnly(AbsFnDecl that) {
        boolean isOprMethod = false;
        IdOrOpOrAnonymousName name = that.getName();
        if ( (inTrait || inObject) &&
             (that.getName() instanceof OpName) ) {
            isOprMethod = (! (name instanceof Enclosing) ) ||
                          ((Enclosing)name).getOpen().getText().equals("|");
        }
        boolean hasSelf = false;
        if ( that.getReturnType().isNone() )
            log(that, "The return type of " + name + " is required.");
        for ( Param p : that.getParams() ) {
            if ( p.getName().getText().equals("self") )
                hasSelf = true;
            if ( p instanceof NormalParam &&
                 ((NormalParam)p).getType().isNone() &&
                 ! p.getName().getText().equals("self") )
                log(p, "The type of " + p.getName() + " is required.");
        }
        if ( isOprMethod && ! hasSelf )
            log(that, "An operator method " + name +
                      " should have the self parameter.");
    }

    public void forFnDefOnly(FnDef that) {
        boolean isOprMethod = false;
        IdOrOpOrAnonymousName name = that.getName();
        if ( (inTrait || inObject) &&
             (name instanceof OpName) ) {
            isOprMethod = (! (name instanceof Enclosing) ) ||
                          ((Enclosing)name).getOpen().getText().equals("|");
        }
        boolean hasSelf = false;
        for ( Param p : that.getParams() ) {
            if ( p.getName().getText().equals("self") )
                hasSelf = true;
        }
        if ( isOprMethod && ! hasSelf )
            log(that, "An operator method " + name +
                      " should have the self parameter.");
    }
}
