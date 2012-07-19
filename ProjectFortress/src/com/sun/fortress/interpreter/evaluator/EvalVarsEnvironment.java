/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/

package com.sun.fortress.interpreter.evaluator;

import com.sun.fortress.nodes.*;


/**
 * Like a BuildEnvironments, but all the object/method/trait methods
 * have been stubbed out so it is only good for declaring and
 * evaluating variables.
 */
public class EvalVarsEnvironment extends BuildEnvironments {

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.BuildEnvironments#forApi(com.sun.fortress.interpreter.nodes.Api)
     */
    @Override
    public Boolean forApi(Api x) {
        return Boolean.valueOf(false);

    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.BuildEnvironments#forComponent(com.sun.fortress.interpreter.nodes.Component)
     */
    @Override
    public Boolean forComponent(Component x) {
        return Boolean.valueOf(false);

    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.BuildEnvironments#forDimUnitDecl(com.sun.fortress.interpreter.nodes.DimUnitDecl)
     */
    @Override
    public Boolean forDimUnitDecl(DimUnitDecl x) {
        return Boolean.valueOf(false);

    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.BuildEnvironments#forFnDecl(com.sun.fortress.interpreter.nodes.FnDecl)
     */
    @Override
    public Boolean forFnDecl(FnDecl x) {
        return Boolean.valueOf(false);

    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.BuildEnvironments#forImportApi(com.sun.fortress.interpreter.nodes.ImportApi)
     */
    @Override
    public Boolean forImportApi(ImportApi x) {
        return Boolean.valueOf(false);

    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.BuildEnvironments#forImportNames(com.sun.fortress.interpreter.nodes.ImportNames)
     */
    @Override
    public Boolean forImportNames(ImportNames x) {
        return Boolean.valueOf(false);

    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.BuildEnvironments#forImportStar(com.sun.fortress.interpreter.nodes.ImportStar)
     */
    @Override
    public Boolean forImportStar(ImportStar x) {
        return Boolean.valueOf(false);

    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.BuildEnvironments#forObjectDef(com.sun.fortress.interpreter.nodes.ObjectDecl)
     */
    @Override
    public Boolean forObjectDecl(ObjectDecl x) {
        return Boolean.valueOf(false);

    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.BuildEnvironments#forTraitDef(com.sun.fortress.interpreter.nodes.TraitDecl)
     */
    @Override
    public Boolean forTraitDecl(TraitDecl x) {
        return Boolean.valueOf(false);

    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.BuildEnvironments#forTypeAlias(com.sun.fortress.interpreter.nodes.TypeAlias)
     */
    @Override
    public Boolean forTypeAlias(TypeAlias x) {
        return Boolean.valueOf(false);

    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.BuildEnvironments#forDimArg(com.sun.fortress.interpreter.nodes.DimArg)
     */
    @Override
    public Boolean forDimArg(DimArg x) {
        return Boolean.valueOf(false);

    }

    public EvalVarsEnvironment(Environment within, Environment bind_into) {
        super(within, bind_into);
        // TODO Auto-generated constructor stub
    }

}
