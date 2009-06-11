/*******************************************************************************
    Copyright 2009 Sun Microsystems, Inc.,
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

package com.sun.fortress.compiler;

import static com.sun.fortress.exceptions.InterpreterBug.bug;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.sun.fortress.Shell;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.compiler.typechecker.InferenceVarInserter;
import com.sun.fortress.compiler.typechecker.InferenceVarReplacer;
import com.sun.fortress.compiler.typechecker.TypeChecker;
import com.sun.fortress.compiler.typechecker.TypeCheckerOutput;
import com.sun.fortress.compiler.typechecker.TypeCheckerResult;
import com.sun.fortress.compiler.typechecker.TypeEnv;
import com.sun.fortress.compiler.typechecker.TypeNormalizer;
import com.sun.fortress.compiler.typechecker.TypesUtil;
import com.sun.fortress.compiler.typechecker.constraints.ConstraintUtil;
import com.sun.fortress.compiler.typechecker.TypeAnalyzer;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.repository.FortressRepository;
import com.sun.fortress.scala_src.typechecker.CoercionTest;
import com.sun.fortress.scala_src.typechecker.ExportChecker;
import com.sun.fortress.scala_src.typechecker.TraitTable;
import com.sun.fortress.scala_src.typechecker.TypeHierarchyChecker;
import com.sun.fortress.scala_src.typechecker.TypeWellFormedChecker;
import com.sun.fortress.scala_src.typechecker.OverloadingChecker;
import com.sun.fortress.scala_src.typechecker.STypeChecker;
import com.sun.fortress.scala_src.typechecker.STypeCheckerFactory;
import com.sun.fortress.scala_src.useful.Lists;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.Pair;

/**
 * Verifies all static properties of a valid Fortress program that require
 * interpreting types.  Assumes all names referring to APIs are fully-qualified,
 * and that the other transformations handled by the {@link Disambiguator} have
 * been performed.  In addition to checking the program, performs the following
 * transformations:
 * <ul>
 * <li>All unknown placeholder types are provided explicit (inferred) values.</li>
 * <li>Explicit coercions are added where needed. (Not yet!) </li>
 * <li>Juxtapositions are given a binary structure.</li>
 * <li>FieldRefs that refer to methods and that are followed by an argument expression
 *     become MethodInvocations.</li>
 * </li>
 */
public class StaticChecker {

    public static class ApiResult extends StaticPhaseResult {
        private final Map<APIName, ApiIndex> _apis;
        private final List<APIName> _failedApis;
        private final TypeCheckerOutput _typeCheckerOutput;

        public ApiResult(Map<APIName, ApiIndex> apis,
                               List<APIName> failedApis,
                               Iterable<? extends StaticError> errors,
                               TypeCheckerOutput typeCheckerOutput) {
            super(errors);
            _apis = apis;
            _failedApis = failedApis;
            _typeCheckerOutput = typeCheckerOutput;
        }
        public Map<APIName, ApiIndex> apis() { return _apis; }
        public List<APIName> failed() { return _failedApis; }

        public TypeCheckerOutput typeCheckerOutput() {
            return this._typeCheckerOutput;
        }
    }

    /**
     * Check the given apis. To support circular references, the apis should appear
     * in the given environment.
     */
    public static ApiResult checkApis(Map<APIName, ApiIndex> apis,
                                      GlobalEnvironment env,
                                      FortressRepository repository) {
        List<APIName> failedApis = new ArrayList<APIName>();
        Iterable<? extends StaticError> errors = new HashSet<StaticError>();
        if ( Shell.getTypeChecking() == true &&
             WellKnownNames.areCompilerLibraries() ) {
            HashSet<Api> checkedApis = new HashSet<Api>();
            TypeCheckerOutput type_checker_output = TypeCheckerOutput.emptyOutput();

            for (APIName apiName : apis.keySet()) {
                TypeCheckerResult checked = checkApi(apis.get(apiName), env, repository);
                checkedApis.add((Api)checked.ast());
                if (!checked.isSuccessful()) failedApis.add(apiName);
                errors = IterUtil.compose(checked.errors(), errors);
                type_checker_output = new TypeCheckerOutput( type_checker_output,
                                                             checked.getTypeCheckerOutput() );
            }
            return new ApiResult
                (IndexBuilder.buildApis(checkedApis,
                                        System.currentTimeMillis()).apis(),
                 failedApis,
                 errors,
                 type_checker_output);
        } else
            return new ApiResult(apis, failedApis, errors,
                                 TypeCheckerOutput.emptyOutput());
    }

    public static class ComponentResult extends StaticPhaseResult {
        private final Map<APIName, ComponentIndex> _components;
        private final List<APIName> _failedComponents;
        private final TypeCheckerOutput _typeCheckerOutput;

        public ComponentResult(Map<APIName, ComponentIndex> components,
                               List<APIName> failedComponents,
                               Iterable<? extends StaticError> errors,
                               TypeCheckerOutput typeCheckerOutput) {
            super(errors);
            _components = components;
            _failedComponents = failedComponents;
            _typeCheckerOutput = typeCheckerOutput;
        }
        public Map<APIName, ComponentIndex> components() { return _components; }
        public List<APIName> failed() { return _failedComponents; }

        public TypeCheckerOutput typeCheckerOutput() {
            return this._typeCheckerOutput;
        }
    }

    /** Statically check the given components. */
    public static ComponentResult
        checkComponents(Map<APIName, ComponentIndex> components,
                        GlobalEnvironment env,
                        FortressRepository repository)
    {
        HashSet<Component> checkedComponents = new HashSet<Component>();
        Iterable<? extends StaticError> errors = new HashSet<StaticError>();
        List<APIName> failedComponents = new ArrayList<APIName>();

        TypeCheckerOutput type_checker_output = TypeCheckerOutput.emptyOutput();

        for (APIName componentName : components.keySet()) {
            TypeCheckerResult checked = checkComponent(components.get(componentName), env,
                                                       repository);
            checkedComponents.add((Component)checked.ast());
            if (!checked.isSuccessful())
                failedComponents.add(componentName);

            errors = IterUtil.compose(checked.errors(), errors);
            type_checker_output = new TypeCheckerOutput( type_checker_output, checked.getTypeCheckerOutput() );
        }
        return new ComponentResult
            (IndexBuilder.buildComponents(checkedComponents,
                                          System.currentTimeMillis()).
             components(),
             failedComponents,
             errors,
             type_checker_output);
    }

    public static TypeCheckerResult checkComponent(ComponentIndex component,
                                                   GlobalEnvironment env,
                                                   FortressRepository repository) {
        if (Shell.getTypeChecking() == true) {
            // Check type hierarchy to ensure acyclicity.
            List<StaticError> errors = new TypeHierarchyChecker(component, env, repository).checkHierarchy();
            if (! errors.isEmpty()) {
                return new TypeCheckerResult(component.ast(), errors);
            }

            Node component_ast = component.ast();
            // Replace implicit types with explicit ones.
            if (!Shell.getScala()) {
                component_ast = component_ast.accept(new InferenceVarInserter());
            } else {
                errors.addAll(new ReturnTypeChecker().getErrors(component_ast));
                if(!errors.isEmpty())
                    return new TypeCheckerResult(component_ast,errors);
            }
            component_ast = component_ast.accept(new TypeNormalizer());
            component = IndexBuilder.builder.buildComponentIndex((Component)component_ast,
                                                                 System.currentTimeMillis());
            TraitTable traitTable = new TraitTable(component, env);
            TypeAnalyzer typeAnalyzer = TypeAnalyzer.make(traitTable);

            if (Shell.testCoercion())
                errors.addAll(new CoercionTest(typeAnalyzer).run());

            errors.addAll(new TypeHierarchyChecker(component, env, repository).checkAcyclicHierarchy());
            errors.addAll(new TypeWellFormedChecker(component, env, typeAnalyzer).check());

            TypeCheckerResult result;
            if ( ! Shell.getScala() ) {
                // typecheck...
                result = typeCheck(component, env, traitTable, component_ast, false);
                // then replace inference variables...
                InferenceVarReplacer rep = new InferenceVarReplacer(result.getIVarResults());
                component_ast = (Component)result.ast().accept(rep);
                // then typecheck again!!!
                component = IndexBuilder.builder.buildComponentIndex((Component)component_ast,
                                                                     System.currentTimeMillis());
                result = typeCheck(component, env, traitTable, component_ast, true);
            } else {
                TypeEnv typeEnv = typeCheckEnv(component, env);
                ConstraintUtil.useScalaFormulas();
                STypeChecker typeChecker = STypeCheckerFactory.make(component, traitTable, typeEnv, typeAnalyzer);
                component_ast = typeChecker.typecheck(component_ast);
                component = IndexBuilder.builder.buildComponentIndex((Component)component_ast,
                    System.currentTimeMillis());
                errors.addAll(Lists.toJavaList(typeChecker.getErrors()));
                result = new TypeCheckerResult(component_ast, errors);
            }
            result.setAst(result.ast().accept(new TypeNormalizer()));
            // There should be no Inference vars left at this point
            if( TypesUtil.assertAfterTypeChecking(result.ast()) )
                bug("Result of typechecking still contains ArrayType/MatrixType/_InferenceVarType.\n" +
                    result.ast());

            errors.addAll(new TypeWellFormedChecker(component, env, typeAnalyzer).check());
            if ( ! errors.isEmpty() ) {
                result = addErrors(errors, result);
                return result;
            }

            // Check overloadings in this component.
            errors.addAll(new OverloadingChecker(component, env, repository).checkOverloading());
            if ( ! errors.isEmpty() ) {
                result = addErrors(errors, result);
                return result;
            }

            // Check the set of exported APIs in this component.
            errors.addAll(ExportChecker.checkExports(component, env, repository));
            result = addErrors(errors, result);
            return result;
         } else {
             return new TypeCheckerResult(component.ast(), IterUtil.<StaticError>empty());
         }
    }

    private static TypeCheckerResult typeCheck(ComponentIndex component,
                                               GlobalEnvironment env,
                                               TraitTable traitTable,
                                               Node component_ast,
                                               boolean postInference) {
        TypeEnv typeEnv = typeCheckEnv(component, env);
        ConstraintUtil.useJavaFormulas();
        TypeChecker typeChecker = new TypeChecker(traitTable, typeEnv,
                                                  component, postInference);
        return component_ast.accept(typeChecker);
    }

    private static TypeEnv typeCheckEnv(ComponentIndex component,
                                        GlobalEnvironment env) {
        TypeEnv typeEnv = TypeEnv.make(component);
        // Add all top-level function names to the component-level environment.
        typeEnv = typeEnv.extendWithFunctions(component.functions());
        // Iterate over top-level variables,
        // adding each to the component-level environment.
        typeEnv = typeEnv.extend(component.variables());
        // Add all top-level object names to the component-level environment.
        typeEnv = typeEnv.extendWithTypeConses(component.typeConses());
        return typeEnv;
    }

    public static TypeCheckerResult checkApi(ApiIndex api,
                                             GlobalEnvironment env,
                                             FortressRepository repository) {
        // Check type hierarchy to ensure acyclicity.
        List<StaticError> errors = new TypeHierarchyChecker(api, env, repository).checkHierarchy();
        if (! errors.isEmpty()) {
            return new TypeCheckerResult(api.ast(), errors);
        }
        Node api_ast = api.ast();
        TypeCheckerResult result = new TypeCheckerResult(api_ast, errors);
        result.setAst(result.ast().accept(new TypeNormalizer()));
        // There should be no Inference vars left at this point
        if( TypesUtil.assertAfterTypeChecking(result.ast()) )
            bug("Result of typechecking still contains ArrayType/MatrixType/_InferenceVarType.\n" +
                result.ast());
        // Check overloadings in this API.
        errors = new OverloadingChecker(api, env, repository).checkOverloading();
        result = addErrors(errors, result);
        return result;
    }

    private static TypeCheckerResult addErrors(List<StaticError> errors,
                                               TypeCheckerResult result) {
        if ( ! errors.isEmpty() ) {
            for ( StaticError error : errors ) {
                result = TypeCheckerResult.addError(result, error);
            }
        }
        return result;
    }

}
