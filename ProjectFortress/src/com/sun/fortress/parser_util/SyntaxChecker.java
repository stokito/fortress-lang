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
import java.util.List;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.Modifiers;
import com.sun.fortress.nodes_util.NodeUtil;
import static com.sun.fortress.exceptions.ProgramError.error;
import static com.sun.fortress.exceptions.InterpreterBug.bug;

/**
 * A visitor that checks syntactic restrictions:
 * 1) A declaration in an API should not have any missing types.
 * 2) An operator method that is not a subscripting operator method
 *    nor a subscripted assignment operator method
 *    should have the self parameter.
 * 3) Valid modifiers for traits, objects, functionals, variables
 *    in components and APIs.
 */
public final class SyntaxChecker extends NodeDepthFirstVisitor_void {
    private boolean inComponent = false;
    private boolean inApi = false;
    private boolean inTrait = false;
    private boolean inObject = false;
    private boolean inBlock = false;
    private BufferedWriter writer;

    public SyntaxChecker( BufferedWriter in_writer ) {
        writer = in_writer;
    }

    private void log(Node that, String message) {
        try {
            if ( ! ( that instanceof ASTNode ) )
                bug(that, "Only ASTNodes are supported.");
            writer.write( ((ASTNode)that).getSpan() + " : " + message + "\n" );
        } catch (IOException error) {
            error("Writing to a log file for the syntax checker failed!");
        }
    }

    public void forComponent(Component that) {
        inComponent = true;
        super.forComponent( that );
        inComponent = false;
    }

    public void forComponentOnly(Component that) {
        try {
            writer.close();
        } catch (IOException error) {
            error("Closing a log file for the syntax checker failed!");
        }
    }

    public void forApi(Api that) {
        inApi = true;
        super.forApi( that );
        inApi = false;
    }

    public void forApiOnly(Api that) {
        try {
            writer.close();
        } catch (IOException error) {
            error("Closing a log file for the syntax checker failed!");
        }
    }

    public void forTraitDecl(TraitDecl that) {
        inTrait = true;
        Modifiers mods = NodeUtil.getMods(that);
        if (!Modifiers.TraitMod.containsAll(mods)) {
            log(that, mods.remove(Modifiers.TraitMod) + " cannot modify a trait, " +
                NodeUtil.getName(that));
        }
        if ( inApi && mods.isPrivate() ) {
            log(that, "private trait " + NodeUtil.getName(that) +
                " most not appear in an API.");
        }
        super.forTraitDecl( that );
        inTrait = false;
    }

    public void forObjectDecl(ObjectDecl that) {
        inObject = true;
        Modifiers mods = NodeUtil.getMods(that);
        if (!Modifiers.ObjectMod.containsAll(mods)) {
            log(that, mods.remove(Modifiers.ObjectMod) + " cannot modify an object, " +
                NodeUtil.getName(that));
        }
        if ( inApi && mods.isPrivate() ) {
            log(that, "private object " + NodeUtil.getName(that) +
                " most not appear in an API.");
        }
        super.forObjectDecl( that );
        inObject = false;
    }

/* ParamFldMod ::= var | hidden | settable |        wrapped */
/* FldMod      ::= var | hidden | settable | test | wrapped | private */
/* FldImmutableMod   ::= hidden |            test | wrapped | private */
/* AbsFldMod         ::= hidden | settable | test | wrapped | private */
/* ApiFldMod         ::= hidden | settable | test */

    public void forVarDeclOnly(VarDecl that) {
        if ( that.getInit().isNone() ) { // variable declaration without a body expression
            for (LValue lvb : that.getLhs()) {
                if ( lvb.getIdType().isNone() )
                    log(lvb, "The type of " + lvb.getName() + " is required.");
            }
        }
    }

    public void forFnDeclOnly(FnDecl that) {
        boolean hasBody = that.getBody().isSome();
        Modifiers mods = that.getMods();

        if ( mods.isGetter() ) {
            if ( ! that.getParams().isEmpty() )
                log(that, "Getter declaration should not have a parameter.");
        } else if ( mods.isSetter() ) {
            // Is this really true?  What if we have a tuple-typed setter?
            if ( ! (that.getParams().size() == 1) )
                log(that, "Setter declaration should have a single parameter.");
        }

        if ( inBlock ) { // local function declaration
            if (!Modifiers.LocalFnMod.containsAll(mods)) {
                log(that, mods.remove(Modifiers.LocalFnMod) + " cannot modify a local function, " +
                    that.getName());
            }
        } else if ( inTrait || inObject ) {
            if (!Modifiers.MethodMod.containsAll(mods)) {
                log(that, mods.remove(Modifiers.MethodMod) + " cannot modify a method, " +
                    that.getName());
            }
            if ( inComponent ) {
                if ( inObject && !hasBody ) {
                    log(that, "Object method " + that.getName() + " lacks a body.");
                }
                if ( mods.isAbstract() && hasBody) {
                    log(that, "Method " + that.getName() + " is concrete, but declared abstract.");
                }
            } else {
                if ( mods.isPrivate()) {
                    log(that, "private cannot modify a method " +
                        that.getName() + " in an API.");
                }
            }
        } else { // top-level function declaration
            if (!Modifiers.FnMod.containsAll(mods)) {
                log(that, mods.remove(Modifiers.FnMod) +
                    " cannot modify a function, " + that.getName());
            }
            if ( !inComponent ) {
                if (mods.isPrivate()) {
                    log(that, "private cannot modify a function " +
                        that.getName() + " in an API.");
                }
            }
        }

        boolean isOprMethod = false;
        IdOrOpOrAnonymousName name = that.getName();
        if ( (inTrait || inObject) &&
             (name instanceof Op) ) {
            isOprMethod = (! (((Op)name).isEnclosing()) ) ||
                           ((Op)name).getText().equals("| |");
        }
        boolean hasSelf = false;
        if ( (! hasBody) && that.getReturnType().isNone() )
            log(that, "The return type of " + name + " is required.");

        for ( Param p : that.getParams() ) {
            if ( p.getName().getText().equals("self") )
                hasSelf = true;
            if ( (! hasBody) &&
                 (! NodeUtil.isVarargsParam(p)) &&
                 p.getIdType().isNone() &&
                 ! p.getName().getText().equals("self") )
                log(p, "The type of " + p.getName() + " is required.");
        }
        if ( isOprMethod && ! hasSelf )
            log(that, "An operator method " + name +
                      " should have the self parameter.");
    }

    public void forBlock(Block that) {
        inBlock = true;
        super.forBlock( that );
        inBlock = false;
    }
}
