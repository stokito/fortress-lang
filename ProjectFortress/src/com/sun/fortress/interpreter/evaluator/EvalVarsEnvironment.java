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

import com.sun.fortress.nodes.AbsFnDecl;
import com.sun.fortress.nodes.AbsObjectDecl;
import com.sun.fortress.nodes.AbsTraitDecl;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes.DimUnitDecl;
import com.sun.fortress.nodes.FnDef;
import com.sun.fortress.nodes.ImportApi;
import com.sun.fortress.nodes.ImportNames;
import com.sun.fortress.nodes.ImportStar;
import com.sun.fortress.nodes.ObjectDecl;
import com.sun.fortress.nodes.TraitDecl;
import com.sun.fortress.nodes.TypeAlias;
import com.sun.fortress.nodes.DimArg;


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
        return null;

    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.BuildEnvironments#forComponent(com.sun.fortress.interpreter.nodes.Component)
     */
    @Override
    public Boolean forComponent(Component x) {
        return null;

    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.BuildEnvironments#forDimUnitDecl(com.sun.fortress.interpreter.nodes.DimUnitDecl)
     */
    @Override
    public Boolean forDimUnitDecl(DimUnitDecl x) {
        return null;

    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.BuildEnvironments#forFnDef(com.sun.fortress.interpreter.nodes.AbsFnDecl)
     */
    @Override
    public Boolean forAbsFnDecl(AbsFnDecl x) {
        return null;

    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.BuildEnvironments#forFnDef(com.sun.fortress.interpreter.nodes.FnDef)
     */
    @Override
    public Boolean forFnDef(FnDef x) {
        return null;

    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.BuildEnvironments#forImportApi(com.sun.fortress.interpreter.nodes.ImportApi)
     */
    @Override
    public Boolean forImportApi(ImportApi x) {
        return null;

    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.BuildEnvironments#forImportNames(com.sun.fortress.interpreter.nodes.ImportNames)
     */
    @Override
    public Boolean forImportNames(ImportNames x) {
        return null;

    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.BuildEnvironments#forImportStar(com.sun.fortress.interpreter.nodes.ImportStar)
     */
    @Override
    public Boolean forImportStar(ImportStar x) {
        return null;

    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.BuildEnvironments#forObjectDecl(com.sun.fortress.interpreter.nodes.AbsObjectDecl)
     */
    @Override
    public Boolean forAbsObjectDecl(AbsObjectDecl x) {
        return null;

    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.BuildEnvironments#forObjectDef(com.sun.fortress.interpreter.nodes.ObjectDecl)
     */
    @Override
    public Boolean forObjectDecl(ObjectDecl x) {
        return null;

    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.BuildEnvironments#forTraitDecl(com.sun.fortress.interpreter.nodes.AbsTraitDecl)
     */
    @Override
    public Boolean forAbsTraitDecl(AbsTraitDecl x) {
        return null;

    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.BuildEnvironments#forTraitDef(com.sun.fortress.interpreter.nodes.TraitDecl)
     */
    @Override
    public Boolean forTraitDecl(TraitDecl x) {
        return null;

    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.BuildEnvironments#forTypeAlias(com.sun.fortress.interpreter.nodes.TypeAlias)
     */
    @Override
    public Boolean forTypeAlias(TypeAlias x) {
        return null;

    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.BuildEnvironments#forDimArg(com.sun.fortress.interpreter.nodes.DimArg)
     */
    @Override
    public Boolean forDimArg(DimArg x) {
        return null;

    }

    public EvalVarsEnvironment(Environment within, Environment bind_into) {
        super(within, bind_into);
        // TODO Auto-generated constructor stub
    }

}
