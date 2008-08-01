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

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import com.sun.fortress.nodes.*;
import com.sun.fortress.useful.*;
import edu.rice.cs.plt.tuple.Option;

/**
 * A visitor that makes an api from a component.
 */
public final class ApiMaker extends NodeUpdateVisitor {
    public static final ApiMaker ONLY = new ApiMaker();

    private ApiMaker() {}

    private boolean containsPrivate(List<Modifier> mods) {
        boolean result = false;
        for (Modifier mod : mods) {
            if ( mod instanceof ModifierPrivate )
                result = true;
        }
        return result;
    }

    private Boolean isPrivate(AbsDecl decl) {
        return decl.accept( new NodeDepthFirstVisitor<Boolean>() {
                @Override public Boolean forAbsTraitDecl(AbsTraitDecl that) {
                    return new Boolean(containsPrivate(that.getMods()));
                }

                @Override public Boolean forAbsObjectDecl(AbsObjectDecl that) {
                    return new Boolean(containsPrivate(that.getMods()));
                }

                @Override public Boolean forAbsVarDecl(AbsVarDecl that) {
                    List<LValueBind> lhs = that.getLhs();
                    boolean result = false;
                    for (LValueBind lv : lhs) {
                        if ( containsPrivate(lv.getMods()) )
                            result = true;
                    }
                    return new Boolean(result);
                }

                @Override public Boolean forAbsFnDecl(AbsFnDecl that) {
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
            AbsDecl elt_result = (AbsDecl) elt.accept(this);
            if ( ! isPrivate(elt_result) ) {
                result.add(elt_result);
            }
        }
        return result;
    }

    public Api forComponent(Component that) {
        return new Api(that.getSpan(),
                       that.getName(),
                       that.getImports(),
                       declsToAbsDecls(that.getDecls()));
    }

    public AbsTraitDecl forTraitDecl(TraitDecl that) {
        return new AbsTraitDecl(that.getSpan(),
                                that.getMods(),
                                that.getName(),
                                that.getStaticParams(),
                                that.getExtendsClause(),
                                that.getWhere(),
                                that.getExcludes(),
                                that.getComprises(),
                                declsToAbsDecls(that.getDecls()));
    }

    public AbsObjectDecl forObjectDecl(ObjectDecl that) {
        return new AbsObjectDecl(that.getSpan(),
                                 that.getMods(),
                                 that.getName(),
                                 that.getStaticParams(),
                                 that.getExtendsClause(),
                                 that.getWhere(),
                                 that.getParams(),
                                 that.getThrowsClause(),
                                 that.getContract(),
                                 declsToAbsDecls(that.getDecls()));
    }

    public AbsVarDecl forVarDecl(VarDecl that) {
        return new AbsVarDecl(that.getSpan(),
                              that.getLhs());
    }

    public AbsFnDecl forFnDef(FnDef that) {
        return new AbsFnDecl(that.getSpan(),
                             that.getMods(),
                             that.getName(),
                             that.getStaticParams(),
                             that.getParams(),
                             that.getReturnType(),
                             that.getThrowsClause(),
                             that.getWhere(),
                             that.getContract(),
                             that.getSelfName());
    }
}
