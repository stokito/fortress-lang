/*******************************************************************************
    Copyright 2009 Sun Microsystems, Inc.,
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
            writer.write( NodeUtil.getSpan((ASTNode)that) + "\n    " + message + "\n" );
        } catch (IOException error) {
            error("Writing to a log file for the syntax checker failed!");
        }
    }

    public void forImportStar(ImportStar that) {
        if ( that.getForeignLanguage().isSome() )
            log(that, "Foreign language imports are not allowed to have {...}.");
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
            log(that, "Private trait " + NodeUtil.getName(that) +
                " must not appear in an API.");
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
            log(that, "Private object " + NodeUtil.getName(that) +
                " must not appear in an API.");
        }
        if ( NodeUtil.getParams(that).isSome() ) {
            for ( Param p : NodeUtil.getParams(that).unwrap() ) {
                Modifiers m = p.getMods();
                if ( NodeUtil.isMutable(p) &&
                     p.getIdType().isNone() )
                    log(p, "The type of " + p.getName() + " is required.");
                if ( inComponent ) {
                    if (! Modifiers.ParamFldMod.containsAll(m) )
                        log(p, m.remove(Modifiers.ParamFldMod) +
                            " cannot modify an object parameter, " + p.getName());
                } else {
                    if (! Modifiers.ApiFldMod.containsAll(m) )
                        log(p, m.remove(Modifiers.ApiFldMod) +
                            " cannot modify an object parameter, " + p.getName());
                }
            }
        }

        super.forObjectDecl( that );
        inObject = false;
    }

    public void forLocalVarDeclOnly(LocalVarDecl that) {
        for (LValue lvb : that.getLhs()) {
            // variable declaration without a body expression or
            if ( that.getRhs().isNone() ||
                 lvb.isMutable() ) { // a mutable variable
                if ( lvb.getIdType().isNone() ) // type is required
                    log(lvb, "The type of " + lvb.getName() + " is required.");
            }
        }

        Modifiers mods = NodeUtil.getMods(writer, that);
        if (! Modifiers.LocalVarMod.containsAll(mods) )
            log(that, mods.remove(Modifiers.LocalVarMod) +
                " cannot modify local variables.");
    }

    public void forVarDeclOnly(VarDecl that) {
        for (LValue lvb : that.getLhs()) {
            // variable declaration without a body expression or
            if ( that.getInit().isNone() ||
                 lvb.isMutable() ) { // a mutable variable
                if ( lvb.getIdType().isNone() ) // type is required
                    log(lvb, "The type of " + lvb.getName() + " is required.");
            }
            // Top-level _ declarations are allowed only in components
            // not in objects/traits/APIs.
            if ( lvb.getName().getText().equals("_") &&
                 (inApi || inTrait || inObject) )
                log(lvb, "Fields or top-level declarations in APIs " +
                    "cannot be named '_'.");
        }

        Modifiers mods = NodeUtil.getMods(writer, that);
        if ( inComponent ) {
            if ( inTrait ) {
                if (! Modifiers.AbsFldMod.containsAll(mods) )
                    log(that, mods.remove(Modifiers.AbsFldMod) +
                        " cannot modify fields in a trait in a component.");
            } else if ( inObject ) {
                if (! Modifiers.FldMod.containsAll(mods) )
                    log(that, mods.remove(Modifiers.FldMod) +
                        " cannot modify fields in an object in a component.");
            } else { // top-level variable declaration
                if (! Modifiers.VarMod.containsAll(mods) )
                    log(that, mods.remove(Modifiers.VarMod) +
                        " cannot modify top-level variables in a component.");
            }
        } else { // in API
            if ( inTrait || inObject ) {
                if (! Modifiers.ApiFldMod.containsAll(mods) )
                    log(that, mods.remove(Modifiers.ApiFldMod) +
                        " cannot modify fields in an API.");
            } else { // top-level variable declaration
                if (! Modifiers.AbsVarMod.containsAll(mods) )
                    log(that, mods.remove(Modifiers.AbsVarMod) +
                        " cannot modify top-level variables in an API.");
            }
        }
    }

    public void forFnDeclOnly(FnDecl that) {
        boolean hasBody = that.getBody().isSome();
        Modifiers mods = NodeUtil.getMods(that);

        if ( mods.isGetter() ) {
            if ( ! NodeUtil.getParams(that).isEmpty() )
                log(that, "Getter declaration should not have a parameter.");
        } else if ( mods.isSetter() ) {
            // Is this really true?  What if we have a tuple-typed setter?
            if ( ! (NodeUtil.getParams(that).size() == 1) )
                log(that, "Setter declaration should have a single parameter.");
        }

        if ( inBlock ) { // local function declaration
            if (!Modifiers.LocalFnMod.containsAll(mods)) {
                log(that, mods.remove(Modifiers.LocalFnMod) + " cannot modify a local function, " +
                    NodeUtil.getName(that));
            }
        } else if ( inTrait || inObject ) {
            if (!Modifiers.MethodMod.containsAll(mods)) {
                log(that, mods.remove(Modifiers.MethodMod) + " cannot modify a method, " +
                    NodeUtil.getName(that));
            }
            if ( inComponent ) {
                if ( inObject && !hasBody ) {
                    log(that, "Object method " + NodeUtil.getName(that) + " lacks a body.");
                }
                if ( mods.isAbstract() && hasBody) {
                    log(that, "Method " + NodeUtil.getName(that) + " is concrete, but declared abstract.");
                }
            } else {
                if ( mods.isPrivate()) {
                    log(that, "private cannot modify a method " +
                        NodeUtil.getName(that) + " in an API.");
                }
            }
        } else { // top-level function declaration
            if (!Modifiers.FnMod.containsAll(mods)) {
                log(that, mods.remove(Modifiers.FnMod) +
                    " cannot modify a function, " + NodeUtil.getName(that));
            }
            if ( !inComponent ) {
                if (mods.isPrivate()) {
                    log(that, "private cannot modify a function " +
                        NodeUtil.getName(that) + " in an API.");
                }
            }
        }

        boolean isOprMethod = false;
        IdOrOpOrAnonymousName name = NodeUtil.getName(that);
        if ( (inTrait || inObject) &&
             (name instanceof Op) ) {
            isOprMethod = (! (((Op)name).isEnclosing()) ) ||
                           ((Op)name).getText().equals("| |");
        }
        boolean hasSelf = false;
        if ( (! hasBody) && NodeUtil.getReturnType(that).isNone() )
            log(that, "The return type of " + name + " is required.");

        for ( Param p : NodeUtil.getParams(that) ) {
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
