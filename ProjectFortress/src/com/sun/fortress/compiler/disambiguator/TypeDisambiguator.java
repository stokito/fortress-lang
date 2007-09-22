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

package com.sun.fortress.compiler.disambiguator;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.Collections;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.Pair;
import edu.rice.cs.plt.collect.ConsList;
import edu.rice.cs.plt.lambda.Lambda;
import edu.rice.cs.plt.lambda.Thunk;
import edu.rice.cs.plt.lambda.LambdaUtil;

import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.ExprFactory;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.compiler.StaticError;
import com.sun.fortress.compiler.index.TypeConsIndex;
import com.sun.fortress.useful.HasAt;

/**
 * <p>Eliminates ambiguities in types:
 * <ul>
 * <li>Type names referring to APIs are made fully qualified.
 * <li>IdTypes referring to type constructors become InstantiatedTypes
 *     (with 0 arguments).</li>
 * <li>Disambiguate static arguments of these types.</li>
 * TODO: Do not allow this to recur on types in function static arguments, as these
 * arguments may not actually be types.  Resolving the ambiguity in function static
 * arguments cannot occur in this phase.
 * </ul>
 * </p>
 * <p>All name references in resolved types that are undefined or used incorrectly are
 * treated as static errors.  Similarly, incorrect arity or kinds of static arguments
 * are treated as errors.</p>
 */
public class TypeDisambiguator extends NodeUpdateVisitor {
    
    private TypeNameEnv _env;
    private Set<SimpleName> _onDemandImports;
    private List<StaticError> _errors;
    
    public TypeDisambiguator(TypeNameEnv env, Set<SimpleName> onDemandImports,
                             List<StaticError> errors) {
        _env = env;
        _onDemandImports = onDemandImports;
        _errors = errors;
    }
    
    private void error(String msg, HasAt loc) {
        _errors.add(StaticError.make(msg, loc));
    }
    
    @Override public Node forIdType(final IdType that) {
        Thunk<Type> varHandler = LambdaUtil.<Type>valueLambda(that);
        Lambda<QualifiedIdName, Type> typeConsHandler =
            new Lambda<QualifiedIdName, Type>() {
            public Type value(QualifiedIdName n) {
                TypeConsIndex typeCons = _env.typeConsIndex(n);
                if (!typeCons.staticParameters().isEmpty()) {
                    error("Type requires static arguments: " + NodeUtil.nameString(n),
                          n);
                    return that;
                }
                return new InstantiatedType(n, Collections.<StaticArg>emptyList());
            }
        };
        return handleTypeName(that, that.getName(), varHandler, typeConsHandler);
    }
    
    @Override public Node forInstantiatedType(final InstantiatedType that) {
        Thunk<Type> varHandler = new Thunk<Type>() {
            public Type value() {
                error("Type parameter cannot be parameterized: " +
                      NodeUtil.nameString(that.getName()), that);
                return that;
            }
        };
        Lambda<QualifiedIdName, Type> typeConsHandler =
            new Lambda<QualifiedIdName, Type>() {
            public InstantiatedType value(QualifiedIdName n) {
                TypeConsIndex typeCons = _env.typeConsIndex(n);
                Map<IdName, StaticParam> params = typeCons.staticParameters();
                List<StaticArg> args = that.getArgs();
                if (params.size() != args.size()) {
                    error("Incorrect number of static arguments for type '" +
                          NodeUtil.nameString(n) + "': provided " + args.size() +
                          ", expected " + params.size(), n);
                    return that;
                }
                boolean changed = false;
                List<StaticArg> newArgs = new ArrayList<StaticArg>(args.size());
                for (Pair<StaticParam, StaticArg> pair :
                         IterUtil.zip(params.values(), args)) {
                    StaticArg updated = updateStaticArg(pair.second(), pair.first());
                    if (updated != pair.second()) { changed = true; }
                    newArgs.add(updated);
                }
                return changed ?
                    new InstantiatedType(that.getSpan(), n, newArgs) : that;
            }
        };
        return handleTypeName(that, that.getName(), varHandler, typeConsHandler);
    }
    
    /**
     * Generalization of name handling in {@code forIdType} and
     * {@code forInstantiatedType}.  Disambiguate the given type name,
     * determine whether it is a variable or a type constructor, and delegate to the
     * appropriate handler.
     * @param that  The type that is being translated (used as a result when errors
     *              occur).
     * @param n  The type name to be resolved (provided by {@code that}).
     * @param variableHandler  Produces the appropriate result where {@code n} represents
     *                         a type variable.  May assume that {@code n} has no
     *                         API and was not changed.
     * @param typeConsHandler  Produce the appropriate result where {@code n} represents
     *                         a type constructor (trait, object, or type alias) name.
     *                         An updated, potentially-qualified version of {@code n} is
     *                         provided.  May assume that the named type constructor
     *                         exists.
     */
    private Type handleTypeName(Type that, QualifiedIdName n,
                                Thunk<Type> variableHandler,
                                Lambda<QualifiedIdName, Type> typeConsHandler) {
        if (n.getApi().isSome()) {
            DottedName originalApi = Option.unwrap(n.getApi());
            Option<DottedName> realApiOpt = _env.apiName(originalApi);
            if (realApiOpt.isNone()) {
                error("Undefined API: " + NodeUtil.nameString(originalApi), originalApi);
                return that;
            }
            DottedName realApi = Option.unwrap(realApiOpt);
            QualifiedIdName newN;
            if (originalApi == realApi) { newN = n; }
            else { newN = NodeFactory.makeQualifiedIdName(realApi, n.getName()); }
            
            if (!_env.hasQualifiedTypeCons(newN)) {
                error("Undefined type: " + NodeUtil.nameString(newN), newN);
                return that;
            }
            return typeConsHandler.value(newN);
        }
        
        else {
            IdName id = n.getName();
            if (_env.hasTypeParam(id)) { return variableHandler.value(); }
            else {
                Set<QualifiedIdName> typeConses = _env.explicitTypeConsNames(id);
                if (typeConses.isEmpty()) {
                    typeConses = _env.onDemandTypeConsNames(id);
                    _onDemandImports.add(id);
                }
                
                if (typeConses.isEmpty()) {
                    error("Undefined type: " + NodeUtil.nameString(n), n);
                    return that;
                }
                if (typeConses.size() > 1) {
                    error("Type name may refer to: " + NodeUtil.namesString(typeConses),
                          n);
                    return that;
                }
                return typeConsHandler.value(IterUtil.first(typeConses));
            }
        }
    }
    
    private StaticArg updateStaticArg(StaticArg a, StaticParam p) {
        // TODO: implement
        return a;
    }
    
}
