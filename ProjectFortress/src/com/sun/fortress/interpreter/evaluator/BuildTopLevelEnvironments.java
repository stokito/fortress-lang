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

package com.sun.fortress.interpreter.evaluator;

import java.util.HashMap;
import java.util.List;

import com.sun.fortress.interpreter.env.ComponentWrapper;
import com.sun.fortress.interpreter.evaluator.types.FTypeTrait;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.AbsDeclOrDecl;
import com.sun.fortress.nodes.AbsTraitDecl;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeAbstractVisitor;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.TraitDecl;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.Voidoid;

public class BuildTopLevelEnvironments extends BuildEnvironments {
    /**
     * Creates an environment builder that will inject bindings into 'within'.
     * The visit is suspended at generics (com.sun.fortress.interpreter.nodes
     * with type parameters) until they can be instantiated.
     */
    public BuildTopLevelEnvironments(Environment within, HashMap<String, ComponentWrapper> linker) {
        super(within);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forApi(com.sun.fortress.interpreter.nodes.Api)
     */
    @Override
    public Voidoid forApi(Api x) {
        List<? extends AbsDeclOrDecl> decls = x.getDecls();

        switch (getPass()) {
        case 1:
        case 2:
        case 3:
        case 4: doDefs(this, decls);break;
        }
        return null;

    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forComponent(com.sun.fortress.interpreter.nodes.Component)
     */
    @Override
    public Voidoid forComponent(Component x) {
        List<? extends AbsDeclOrDecl> defs = x.getDecls();
        switch (getPass()) {
        case 1: forComponent1(x); break;

        case 2: doDefs(this, defs); {
            ForceTraitFinish v = new ForceTraitFinish() ;
            for (AbsDeclOrDecl def : defs) {
                v.visit(def);
            }
        }
        break;
        case 3: doDefs(this, defs);break;
        case 4: doDefs(this, defs);break;
        }
        return null;
    }

    public Voidoid forComponentDefs(Component x) {
        List<? extends AbsDeclOrDecl> defs = x.getDecls();
        doDefs(this, defs);
        return null;
    }

    public Voidoid forComponent1(Component x) {
        APIName name = x.getName();
        // List<Import> imports = x.getImports();
        // List<Export> exports = x.getExports();
        List<? extends AbsDeclOrDecl> defs = x.getDecls();

        // SComponent comp = new SComponent(BetterEnv.primitive(x), x);
        //containing.putComponent(name, comp);

        forComponentDefs(x);

        return null;
    }

    class ForceTraitFinish extends NodeAbstractVisitor<Voidoid> {

        /**
         * Make the default behavior return null, no throw an exception.
         */
        public Voidoid defaultCase(Node x) {
            return null;
        }

        /* (non-Javadoc)
         * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forAbsTraitDecl(com.sun.fortress.interpreter.nodes.AbsTraitDecl)
         */
        @Override
        public Voidoid forAbsTraitDecl(AbsTraitDecl x) {
            List<StaticParam> staticParams = x.getStaticParams();
            Id name = x.getName();

            if (staticParams.isEmpty()) {
                    FTypeTrait ftt =
                        (FTypeTrait) containing.getType(NodeUtil.nameString(name));
                    Environment interior = ftt.getWithin();
                    ftt.getMembers();
            }
            return null;
        }

        /* (non-Javadoc)
         * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forTraitDecl(com.sun.fortress.interpreter.nodes.TraitDecl)
         */
        @Override
        public Voidoid forTraitDecl(TraitDecl x) {
            List<StaticParam> staticParams = x.getStaticParams();
            Id name = x.getName();

            if (staticParams.isEmpty()) {
                    FTypeTrait ftt = (FTypeTrait) containing
                            .getType(NodeUtil.nameString(name));
                    Environment interior = ftt.getWithin();
                    ftt.getMembers();
            }
            return null;
        }

        void visit(AbsDeclOrDecl def) {
            def.accept(this);
        }
    }

}
