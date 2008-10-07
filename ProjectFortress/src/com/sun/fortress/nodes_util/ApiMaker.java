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

import java.util.List;
import java.util.ArrayList;
import com.sun.fortress.nodes.*;

/**
 * A visitor that makes an api from a component.
 */
public final class ApiMaker extends NodeUpdateVisitor {
    private boolean inTrait = false;
    private boolean inObject = false;

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
        inTrait = true;
        List<AbsDecl> absDecls = declsToAbsDecls(that.getDecls());
        inTrait = false;
        return new AbsTraitDecl(that.getSpan(),
                                that.getMods(),
                                that.getName(),
                                that.getStaticParams(),
                                that.getExtendsClause(),
                                that.getWhere(),
                                that.getExcludes(),
                                that.getComprises(),
                                absDecls);
    }

    public AbsObjectDecl forObjectDecl(ObjectDecl that) {
        inObject = true;
        List<AbsDecl> absDecls = declsToAbsDecls(that.getDecls());
        inObject = false;
        return new AbsObjectDecl(that.getSpan(),
                                 that.getMods(),
                                 that.getName(),
                                 that.getStaticParams(),
                                 that.getExtendsClause(),
                                 that.getWhere(),
                                 that.getParams(),
                                 that.getThrowsClause(),
                                 that.getContract(),
                                 absDecls);
    }

    /* For a field declaration with the "var" modifier in a component,
       the APIMaker leaves the "var" modifier off in the generated API.
     */
    public AbsVarDecl forVarDecl(VarDecl that) {
        List<LValueBind> lhs = new ArrayList<LValueBind>();
        for (LValueBind lvb : that.getLhs()) {
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
        return new AbsVarDecl(that.getSpan(), lhs);
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

    /* For an abstract method declaration in a component,
       the APIMaker puts the "abstract" modifier in the generated API.
     */
    public AbsFnDecl forAbsFnDecl(AbsFnDecl that) {
        List<Modifier> mods = that.getMods();
        if ( inTrait ) {
            mods.add(0, new ModifierAbstract(that.getSpan()));
        }
        return new AbsFnDecl(that.getSpan(),
                             mods,
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
