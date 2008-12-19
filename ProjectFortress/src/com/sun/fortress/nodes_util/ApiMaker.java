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
package com.sun.fortress.nodes_util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.useful.Useful;
import edu.rice.cs.plt.tuple.Option;
import static com.sun.fortress.exceptions.ProgramError.error;
import static com.sun.fortress.exceptions.InterpreterBug.bug;

/**
 * A visitor that makes an api from a component.
 */
public final class ApiMaker extends NodeDepthFirstVisitor<Option<Node>> {
    private boolean inTrait = false;
    private boolean inObject = false;
    private BufferedWriter writer;

    public ApiMaker( String file ) {
        try {
            writer = Useful.filenameToBufferedWriter( file );
        } catch (IOException error) {
            error("Creating a log file for the api tool failed!");
        }
    }

    private Option<Node> log(Node that, String message) {
        try {
            if ( ! ( that instanceof ASTNode ) )
                bug(that, "Only ASTNodes are supported.");
            writer.write( NodeUtil.getSpan((ASTNode)that) + " : " + message + "\n" );
        } catch (IOException error) {
            error("Writing to a log file for the api tool failed!");
        }
        return Option.<Node>none();
    }

    private Boolean isPrivate(Decl decl) {
        if (decl instanceof TraitObjectDecl) {
            return ((TraitObjectDecl)decl).getHeader().getMods().isPrivate();
        } else if (decl instanceof VarDecl) {
            List<LValue> lhs = ((VarDecl)decl).getLhs();
            for (LValue lv : lhs) {
                if (lv.getMods().isPrivate()) return true;
            }
        } else if (decl instanceof FnDecl) {
            return ((FnDecl)decl).getHeader().getMods().isPrivate();
        }
        return false;
    }

    private List<Decl> declsToDecls(final List<Decl> that) {
        boolean changed = false;
        List<Decl> result = new java.util.ArrayList<Decl>(0);
        for (Decl elt : that) {
            Option<Node> elt_result = elt.accept(this);
            if ( elt_result.isSome() )
                result.add((Decl)elt_result.unwrap());
        }
        return result;
    }

    public Option<Node> forComponent(Component that) {
        Node result = NodeFactory.makeApi(NodeUtil.getSpan(that),
                              that.getName(),
                              that.getImports(),
                              declsToDecls(that.getDecls()));
        try {
            writer.close();
        } catch (IOException error) {
            error("Creating a log file for the api tool failed!");
        }
        return Option.<Node>some(result);
    }

    public Option<Node> forTraitDecl(TraitDecl that) {
        if ( ! isPrivate(that) ) {
            inTrait = true;
            List<Decl> absDecls = declsToDecls(NodeUtil.getDecls(that));
            inTrait = false;
            return Option.<Node>some(
                          NodeFactory.makeTraitDecl(NodeUtil.getSpan(that),
                                                    NodeUtil.getMods(that),
                                                    NodeUtil.getName(that),
                                                    NodeUtil.getStaticParams(that),
                                                    NodeUtil.getExtendsClause(that),
                                                    NodeUtil.getWhereClause(that),
                                                    absDecls,
                                                    NodeUtil.getExcludesClause(that),
                                                    NodeUtil.getComprisesClause(that)));
        } else return Option.<Node>none();
    }

    public Option<Node> forObjectDecl(ObjectDecl that) {
        if ( ! isPrivate(that) ) {
            inObject = true;
            List<Decl> absDecls = declsToDecls(NodeUtil.getDecls(that));
            inObject = false;
            return Option.<Node>some(
                          NodeFactory.makeObjectDecl(NodeUtil.getSpan(that),
                                                     NodeUtil.getMods(that),
                                                     NodeUtil.getName(that),
                                                     NodeUtil.getStaticParams(that),
                                                     NodeUtil.getExtendsClause(that),
                                                     NodeUtil.getWhereClause(that),
                                                     absDecls,
                                                     NodeUtil.getParams(that),
                                                     NodeUtil.getThrowsClause(that),
                                                     NodeUtil.getContract(that)));
        } else return Option.<Node>none();
    }

    /* For a field declaration with the "var" modifier in a component,
       the APIMaker leaves the "var" modifier off in the generated API.
     */
    public Option<Node> forVarDecl(VarDecl that) {
        if ( ! isPrivate(that) ) {
            List<LValue> lhs = new ArrayList<LValue>();
            for (LValue lvb : that.getLhs()) {
                if ( lvb.getIdType().isNone() )
                    log(lvb, "The type of " + lvb.getName() + " is required.");
                if ( inObject && lvb.getMods().isVar()) {
                    Modifiers mods = lvb.getMods().remove(Modifiers.Var);
                    lhs.add( NodeFactory.makeLValue(lvb, mods, false) );
                } else
                    lhs.add( lvb );
            }
            return Option.<Node>some(NodeFactory.makeVarDecl(NodeUtil.getSpan(that), lhs, Option.<Expr>none()));
        } else return Option.<Node>none();
    }

    public Option<Node> forFnDecl(FnDecl that) {
        if ( ! isPrivate(that) ) {
            if ( NodeUtil.getReturnType(that).isNone() )
                log(that, "The return type of " + NodeUtil.getName(that) + " is required.");
            for ( Param p : NodeUtil.getParams(that) ) {
                if ( p.getIdType().isNone() &&
                     p.getVarargsType().isNone() &&
                     ! p.getName().getText().equals("self") )
                    log(p, "The type of " + p.getName() + " is required.");
            }
            /* For an abstract method declaration in a component,
               the APIMaker puts the "abstract" modifier in the generated API.
            */
            Modifiers mods = NodeUtil.getMods(that);
            if ( inTrait && NodeUtil.getBody(that).isNone() ) {
                mods = mods.combine(Modifiers.Abstract);
            }
            return Option.<Node>some(
                          NodeFactory.makeFnDecl(NodeUtil.getSpan(that),
                                                 mods,
                                                 NodeUtil.getName(that),
                                                 NodeUtil.getStaticParams(that),
                                                 NodeUtil.getParams(that),
                                                 NodeUtil.getReturnType(that),
                                                 NodeUtil.getThrowsClause(that),
                                                 NodeUtil.getWhereClause(that),
                                                 NodeUtil.getContract(that),
                                                 Option.<Expr>none()));
        } else return Option.<Node>none();
    }
}
