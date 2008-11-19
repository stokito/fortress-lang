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
import com.sun.fortress.useful.Useful;
import edu.rice.cs.plt.tuple.Option;
import static com.sun.fortress.exceptions.ProgramError.error;

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
            writer.write( that.getSpan() + " : " + message + "\n" );
        } catch (IOException error) {
            error("Writing to a log file for the api tool failed!");
        }
        return Option.<Node>none();
    }

    private boolean containsPrivate(List<Modifier> mods) {
        boolean result = false;
        for (Modifier mod : mods) {
            if ( mod instanceof ModifierPrivate )
                result = true;
        }
        return result;
    }

    private Boolean isPrivate(Decl decl) {
        return decl.accept( new NodeDepthFirstVisitor<Boolean>() {
                @Override public Boolean forTraitDecl(TraitDecl that) {
                    return new Boolean(containsPrivate(that.getMods()));
                }

                @Override public Boolean forObjectDecl(ObjectDecl that) {
                    return new Boolean(containsPrivate(that.getMods()));
                }

                @Override public Boolean forVarDecl(VarDecl that) {
                    List<LValue> lhs = that.getLhs();
                    boolean result = false;
                    for (LValue lv : lhs) {
                        if ( containsPrivate(lv.getMods()) )
                            result = true;
                    }
                    return new Boolean(result);
                }

                @Override public Boolean forFnDecl(FnDecl that) {
                    return new Boolean(containsPrivate(that.getMods()));
                }

                @Override public Boolean defaultCase(Node that) {
                    return new Boolean(false);
                }
            } ).booleanValue();
    }

    private List<AbsDecl> declsToAbsDecls(final List<Decl> that) {
        boolean changed = false;
        List<AbsDecl> result = new java.util.ArrayList<AbsDecl>(0);
        for (Decl elt : that) {
            Option<Node> elt_result = elt.accept(this);
            if ( elt_result.isSome() )
                result.add((AbsDecl)elt_result.unwrap());
        }
        return result;
    }

    public Option<Node> forComponent(Component that) {
        Node result = new Api(that.getSpan(),
                              that.getName(),
                              that.getImports(),
                              declsToAbsDecls(that.getDecls()));
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
            List<AbsDecl> absDecls = declsToAbsDecls(that.getDecls());
            inTrait = false;
            return Option.<Node>some(new AbsTraitDecl(that.getSpan(),
                                                      that.getMods(),
                                                      that.getName(),
                                                      that.getStaticParams(),
                                                      that.getExtendsClause(),
                                                      that.getWhere(),
                                                      that.getExcludes(),
                                                      that.getComprises(),
                                                      absDecls));
        } else return Option.<Node>none();
    }

    public Option<Node> forObjectDecl(ObjectDecl that) {
        if ( ! isPrivate(that) ) {
            inObject = true;
            List<AbsDecl> absDecls = declsToAbsDecls(that.getDecls());
            inObject = false;
            return Option.<Node>some(new AbsObjectDecl(that.getSpan(),
                                                       that.getMods(),
                                                       that.getName(),
                                                       that.getStaticParams(),
                                                       that.getExtendsClause(),
                                                       that.getWhere(),
                                                       that.getParams(),
                                                       that.getThrowsClause(),
                                                       that.getContract(),
                                                       absDecls));
        } else return Option.<Node>none();
    }

    /* For a field declaration with the "var" modifier in a component,
       the APIMaker leaves the "var" modifier off in the generated API.
     */
    public Option<Node> forVarDecl(VarDecl that) {
        if ( ! isPrivate(that) ) {
            List<LValue> lhs = new ArrayList<LValue>();
            for (LValue lvb : that.getLhs()) {
                if ( lvb.getType().isNone() )
                    log(lvb, "The type of " + lvb.getName() + " is required.");
                if ( inObject && NodeUtil.isVar(lvb.getMods()) ) {
                    List<Modifier> mods = new ArrayList<Modifier>();
                    for (Modifier mod : lvb.getMods()) {
                        if ( ! (mod instanceof ModifierVar) ) {
                            mods.add( mod );
                        }
                    }
                    lhs.add( NodeFactory.makeLValue(lvb, mods, false) );
                } else
                    lhs.add( lvb );
            }
            return Option.<Node>some(new AbsVarDecl(that.getSpan(), lhs));
        } else return Option.<Node>none();
    }

    public Option<Node> forFnDecl(FnDecl that) {
        if ( ! isPrivate(that) ) {
            if ( that.getReturnType().isNone() )
                log(that, "The return type of " + that.getName() + " is required.");
            for ( Param p : that.getParams() ) {
                if ( p instanceof NormalParam &&
                     ((NormalParam)p).getType().isNone() &&
                     ! p.getName().getText().equals("self") )
                    log(p, "The type of " + p.getName() + " is required.");
            }
            return Option.<Node>some(new AbsFnDecl(that.getSpan(),
                                                   that.getMods(),
                                                   that.getName(),
                                                   that.getStaticParams(),
                                                   that.getParams(),
                                                   that.getReturnType(),
                                                   that.getThrowsClause(),
                                                   that.getWhere(),
                                                   that.getContract()));
        } else return Option.<Node>none();
    }

    /* For an abstract method declaration in a component,
       the APIMaker puts the "abstract" modifier in the generated API.
     */
    public Option<Node> forAbsFnDecl(AbsFnDecl that) {
        if ( ! isPrivate(that) ) {
            if ( that.getReturnType().isNone() )
                log(that, "The return type of " + that.getName() + " is required.");
            for ( Param p : that.getParams() ) {
                if ( p instanceof NormalParam &&
                     ((NormalParam)p).getType().isNone() &&
                     ! p.getName().getText().equals("self") )
                    log(p, "The type of " + p.getName() + " is required.");
            }
            List<Modifier> mods = that.getMods();
            if ( inTrait ) {
                mods.add(0, new ModifierAbstract(that.getSpan()));
            }
            return Option.<Node>some(new AbsFnDecl(that.getSpan(),
                                                   mods,
                                                   that.getName(),
                                                   that.getStaticParams(),
                                                   that.getParams(),
                                                   that.getReturnType(),
                                                   that.getThrowsClause(),
                                                   that.getWhere(),
                                                   that.getContract()));
        } else return Option.<Node>none();
    }
}
