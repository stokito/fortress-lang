/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator;

import static com.sun.fortress.exceptions.InterpreterBug.bug;
import com.sun.fortress.interpreter.env.CUWrapper;
import com.sun.fortress.interpreter.env.NonApiWrapper;
import com.sun.fortress.interpreter.evaluator.types.FTypeTrait;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.OverloadedFunction;
import com.sun.fortress.interpreter.evaluator.values.SingleFcn;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeUtil;
import edu.rice.cs.plt.tuple.Option;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BuildTopLevelEnvironments extends BuildEnvironments {

    /**
     * Empty by default
     */
    public Set<String> valNames = new HashSet<String>();
    /**
     * Empty by default
     */
    public Set<String> overloadNames = new HashSet<String>();
    /**
     * Empty by default
     */
    public Set<String> typeNames = new HashSet<String>();

    /**
     * Used for mapping API Names to their environments
     */
    Map<String, NonApiWrapper> linker;

    /**
     * Creates an environment builder that will inject bindings into 'within'.
     * The visit is suspended at generics (com.sun.fortress.interpreter.nodes
     * with type parameters) until they can be instantiated.
     */
    public BuildTopLevelEnvironments(Environment within, Map<String, NonApiWrapper> linker) {
        super(within);
        this.linker = linker;
    }

    @Override
    public Boolean forImportApi(ImportApi x) {
        List<AliasedAPIName> apis = x.getApis();
        for (AliasedAPIName aliased_api : apis) {
            APIName imported = aliased_api.getApiName();
            importAPIName(imported);
        }
        return Boolean.valueOf(false);
    }

    @Override
    public Boolean forImportNames(ImportNames x) {
        APIName imported = x.getApiName();
        importAPIName(imported);
        return Boolean.valueOf(false);
    }

    @Override
    public Boolean forImportStar(ImportStar x) {
        APIName imported = x.getApiName();
        importAPIName(imported);
        return Boolean.valueOf(false);
    }

    private void importAPIName(APIName imported) {
        String s = NodeUtil.nameString(imported);
        importAPIName(s);
    }

    public void importAPIName(String s) {
        CUWrapper c = linker.get(s);
        if (c != null) bindInto.putApi(s, c.getEnvironment());
        else {
            System.err.println("Reference to missing api/component " + s);
        }
        // Null Pointer Exception here means that static checking was inadequate.
    }


    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forApi(com.sun.fortress.interpreter.nodes.Api)
     */
    @Override
    public Boolean forApi(Api x) {
        List<Decl> decls = x.getDecls();

        switch (getPass()) {
            case 1:
            case 2:
            case 3:
            case 4:
                doDefs(this, decls);
                break;
        }
        return Boolean.valueOf(false);

    }

    public Boolean for_RewriteObjectExprDecl(_RewriteObjectExprDecl x) {
        return Boolean.valueOf(false);
    }

    public Boolean for_RewriteFunctionalMethodDecl(_RewriteFunctionalMethodDecl x) {
        return Boolean.valueOf(false);
    }

    public Boolean for_RewriteFnOverloadDecl(_RewriteFnOverloadDecl x) {
        switch (getPass()) {
            case 1: {
                // Define the overloaded function.
                String s = x.getName().stringName();
                bindInto.putValue(s, new OverloadedFunction(x.getName(), bindInto));
            }
            break;

            case 3: {
                String s = x.getName().stringName();
                OverloadedFunction of = (OverloadedFunction) bindInto.getRootValue(s);
                for (IdOrOp fn : x.getFns()) {
                    Option<APIName> oapi = fn.getApiName();
                    FValue oapi_val = null;

                    if (fn instanceof Id) {
                        oapi_val = bindInto.getValueNull((Id) fn, Environment.TOP_LEVEL); // top-level reference
                    } else if (fn instanceof Op) {
                        oapi_val = bindInto.getValueNull((Op) fn, Environment.TOP_LEVEL); // top-level reference
                    } else {
                        bug("Unexpected change to AST node hierarchy");
                    }

                    if (oapi_val == null) {
                        
                        if (fn instanceof Id) {
                            oapi_val = bindInto.getValueNull((Id) fn, Environment.TOP_LEVEL); // top-level reference
                        } else if (fn instanceof Op) {
                            oapi_val = bindInto.getValueNull((Op) fn, Environment.TOP_LEVEL); // top-level reference
                        } else {
                            bug("Unexpected change to AST node hierarchy");
                        }
                        
                        bug("Failed to find overload member " + fn + " for " + x);
                    }

                    if (oapi_val instanceof SingleFcn) {
                        of.addOverload((SingleFcn) oapi_val, true);
                    } else if (oapi_val instanceof OverloadedFunction) {
                        of.addOverloads((OverloadedFunction) oapi_val);
                    } else {
                        bug("Unexpected function binding for " + fn + " , value is " + oapi_val);
                    }

                }

                of.finishInitializing();

            }

        }
        return Boolean.valueOf(false);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forComponent(com.sun.fortress.interpreter.nodes.Component)
     */
    @Override
    public Boolean forComponent(Component x) {
        List<Decl> defs = x.getDecls();
        switch (getPass()) {
            case 1:
                forComponent1(x);
                break;

            case 2:
                doDefs(this, defs);
                break;
            case 3: {
                ForceTraitFinish v = new ForceTraitFinish();
                for (Decl def : defs) {
                    v.visit(def);
                }
            }
            doDefs(this, defs);
            break;
            case 4:
                doDefs(this, defs);
                break;
        }
        return Boolean.valueOf(false);
    }

    public Boolean forComponentDefs(Component x) {
        List<Decl> defs = x.getDecls();
        doDefs(this, defs);
        return Boolean.valueOf(false);
    }

    public Boolean forComponent1(Component x) {
        //APIName name = x.getName();
        List<Import> imports = x.getImports();
        // List<Export> exports = x.getExports();
        //List<Decl> defs = x.getDecls();

        // SComponent comp = new SComponent(BetterEnv.primitive(x), x);
        //containing.putComponent(name, comp);

        forComponentDefs(x);

        for (Import imp : imports) {
            imp.accept(this);
        }

        return Boolean.valueOf(false);
    }

    class ForceTraitFinish extends NodeAbstractVisitor<Boolean> {

        /**
         * Make the default behavior return null, no throw an exception.
         */
        public Boolean defaultCase(Node x) {
            return Boolean.valueOf(false);
        }

        /* (non-Javadoc)
         * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forTraitDecl(com.sun.fortress.interpreter.nodes.TraitDecl)
         */
        @Override
        public Boolean forTraitDecl(TraitDecl x) {
            List<StaticParam> staticParams = NodeUtil.getStaticParams(x);
            Id name = NodeUtil.getName(x);

            if (staticParams.isEmpty()) {
                FTypeTrait ftt = (FTypeTrait) containing.getRootType(NodeUtil.nameString(name)); // top level
                //Environment interior = ftt.getWithin();
                ftt.getMembers();
            }
            return Boolean.valueOf(false);
        }

        void visit(Decl def) {
            def.accept(this);
        }
    }

    public void setExporterAndApi(CUWrapper exporter, CUWrapper api) {
        bug("Can only set exporter of API environment builder.");
    }


}
