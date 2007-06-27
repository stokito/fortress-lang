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

package com.sun.fortress.interpreter.nodes;

import com.sun.fortress.interpreter.useful.Option;
import java.util.List;
import java.util.Map;

import com.sun.fortress.interpreter.rewrite.Disambiguate.Thing;
import com.sun.fortress.interpreter.useful.IterableOnce;
import com.sun.fortress.interpreter.useful.UnitIterable;


public abstract class TraitDefOrDecl extends Node implements Generic, HasWhere {

    List<Modifier> mods;

    Id name;

    Option<List<StaticParam>> staticParams;

    Option<List<TypeRef>> extends_;

    List<TypeRef> excludes;

    Option<List<TypeRef>> bounds;

    List<WhereClause> wheres;

    transient private Map<String, Thing> disambiguatingEnvironment;

    TraitDefOrDecl(Span span) {
        super(span);
    }

    /**
     * @return Returns the bounds.
     */
    public Option<List<TypeRef>> getBounds() {
        return bounds;
    }

    /**
     * @return Returns the excludes.
     */
    public List<TypeRef> getExcludes() {
        return excludes;
    }

    /**
     * @return Returns the extends_.
     */
    public Option<List<TypeRef>> getExtends_() {
        return extends_;
    }

    /**
     * @return Returns the mods.
     */
    public List<Modifier> getMods() {
        return mods;
    }

    /**
     * @return Returns the name.
     */
    public Id getName() {
        return name;
    }

    @Override
    public String stringName() {
        return name.getName();
    }

    /**
     * @return Returns the staticParams.
     */
    public Option<List<StaticParam>> getStaticParams() {
        return staticParams;
    }

    /**
     * @return Returns the where.
     */
    public List<WhereClause> getWhere() {
        return wheres;
    }

    /**
     * In order to correctly rewrite object/trait member references, it is
     * necessary to figure out what traits are extended by what traits (in
     * general, a trait might be defined in an environment different from its
     * use, therefore it must capture that environment).
     *
     * @param e
     */
    public void setDisEnv(Map<String, Thing> e) {
        disambiguatingEnvironment = e;
    }

    public Map<String, Thing> getDisEnv() {
        return disambiguatingEnvironment;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.DefOrDecl#stringNames()
     */
    public IterableOnce<String> stringNames() {
        return new UnitIterable<String>(stringName());
    }

    /**
     * Same result as subtype getFn, but the type is generic to remove the need
     * for picky casting in some clients.
     */
    abstract public List<? extends DefOrDecl> getFns();


}
