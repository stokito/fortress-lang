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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sun.fortress.interpreter.useful.BATree;
import com.sun.fortress.interpreter.useful.IterableOnce;
import com.sun.fortress.interpreter.useful.UnitIterable;


// / and object_expr = object_expr_rec node
// / and object_expr_rec =
// / {
// / object_expr_traits : type_ref list option;
// / object_expr_defs : def list;
// / }
// /
public class ObjectExpr extends ValueExpr implements GenericDefWithParams {

    Option<List<TypeRef>> traits;

    List<? extends DefOrDecl> defs;

    public ObjectExpr(Span span, Option<List<TypeRef>> traits,
            List<? extends DefOrDecl> defs) {
        super(span);
        this.traits = traits;
        this.defs = defs;
    }

    // Needs to be private to prevent inclusion in JST serialized form.
    private BATree<String, StaticParam> implicitTypeParameters;

    private String genSymName;

    private Option<List<StaticParam>> staticParams;

    private List<StaticArg> staticArgs;

    static final private Option<List<Param>> optParams = new Some<List<Param>>(
            Collections.<Param> emptyList());

    public Option<List<Param>> getParams() {
        return optParams;
    }

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forObjectExpr(this);
    }

    ObjectExpr(Span span) {
        super(span);
    }

    /**
     * @return Returns the defs.
     */
    public List<? extends DefOrDecl> getDefs() {
        return defs;
    }

    /**
     * @return Returns the traits.
     */
    public Option<List<TypeRef>> getTraits() {
        return traits;
    }

    /**
     * @param implicitTypeParameters
     *            the implicitTypeParameters to set
     */
    public void setImplicitTypeParameters(
            BATree<String, StaticParam> implicit_type_arameters) {
        if (this.staticParams != null) {
            throw new Error("Second set");
        }
        this.implicitTypeParameters = implicit_type_arameters;
        staticArgs = new ArrayList<StaticArg>(implicitTypeParameters.size());

        if (implicitTypeParameters.size() == 0) {
            staticParams = new None<List<StaticParam>>();
        } else {// FIXME
            List<StaticParam> tparams = new ArrayList<StaticParam>(
                    implicitTypeParameters.values());
            staticParams = Some.makeSomeList(tparams);
            for (String s : implicitTypeParameters.keySet()) {
                staticArgs.add(TypeArg.make(getSpan(), s));

            }
        }
    }

    public Option<List<StaticParam>> getStaticParams() {
        return staticParams;
    }

    /**
     * If there are implicit type parameters, these are the type arguments to
     * use in the "instantiation".
     *
     * @return
     */
    public List<StaticArg> getStaticArgs() {
        return staticArgs;
    }

    /**
     * @param genSymName
     *            the genSymName to set
     */
    public void setGenSymName(String genSymName) {
        if (this.genSymName != null) {
            throw new Error("Second set");
        }
        this.genSymName = genSymName;
    }

    /**
     * @return the genSymName
     */
    public String getGenSymName() {
        if (this.genSymName == null) {
            throw new Error("Not yet set");
        }
        return genSymName;
    }

    @Override
    public String stringName() {
        return getGenSymName();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.DefOrDecl#stringNames()
     */
    public IterableOnce<String> stringNames() {
        return new UnitIterable<String>(stringName());
    }

}
