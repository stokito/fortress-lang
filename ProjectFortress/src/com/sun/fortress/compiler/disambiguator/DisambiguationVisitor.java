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
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.collect.ConsList;

import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.ExprFactory;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.compiler.StaticError;
import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.index.ApiIndex;

/**
 * <p>Eliminates ambiguities in an AST that can be resolved solely by knowing what kind
 * of entity a name refers to.  This class specifically handles the following:
 * <ul>
 * <li>All names referring to APIs are made fully qualified (FnRefs and OprExprs may then
 *     contain lists of qualified names referring to multiple APIs).
 * <li>VarRefs referring to functions become FnRefs with placeholders for implicit static
 *     arguments filled in (to be replaced later during type inference).</li>
 * <li>VarRefs referring to getters, setters, or methods become FieldRefs.</li>
 * <li>VarRefs referring to methods, and that are juxtaposed with Exprs, become 
 *     MethodInvocations.</li>
 * <li>FieldRefs referring to methods, and that are juxtaposed with Exprs, become 
 *     MethodInvocations.</li>
 * <li>FnRefs referring to methods, and that are juxtaposed with Exprs, become 
 *     MethodInvocations.</li>
 * <li>IdTypes referring to traits become InstantiatedTypes (with 0 arguments)</li>
 * </ul>
 * 
 * Additionally, all name references that are undefined or used incorrectly are
 * treated as static errors.</p>
 * 
 * <p>Makes the following assumptions about the input:
 * <ul>
 * <li>DottedIds have a length of at least 1.</li>
 * </ul>
 * </p>
 */
public class DisambiguationVisitor extends NodeUpdateVisitor {
    
    private Environment _env;
    private GlobalEnvironment _globalEnv;
    private List<StaticError> _errors;
    
    public DisambiguationVisitor(Environment env, GlobalEnvironment globalEnv,
                                 List<StaticError> errors) {
        _env = env;
        _globalEnv = globalEnv;
        _errors = errors;
    }
    
    @Override public Node forVarRef(VarRef that) {
        Expr result = null;
        ConsList<? extends Id> fields = IterUtil.asConsList(that.getVar().getNames());
        Id entity = ConsList.first(fields);
        String entityName = entity.getName();
        fields = ConsList.rest(fields);
        
        // Declared variable reference
        if (result == null && _env.hasVar(entityName)) {
            if (ConsList.isEmpty(fields)) { return that; }
            else { result = ExprFactory.makeVarRef(entity); }
        }
        
        // Declared function reference
        if (result == null && _env.hasFn(entityName)) {
            List<DottedId> ids = new ArrayList<DottedId>(1);
            ids.add(NodeFactory.makeDottedId(entity));
            ids.addAll(makeApiIds(_env.apisForFn(entityName), entity));
            result = new FnRef(entity.getSpan(), ids);
        }
        
        // Imported variable reference
        if (result == null) {
            Option<String> varApi = _env.apiForVar(entityName);
            if (varApi.isSome()) {
                String api = Option.unwrap(varApi);
                result = new VarRef(entity.getSpan(), makeApiId(api, entity));
            }
        }
        
        // Imported function reference
        if (result == null) {
            Set<String> fnApis = _env.apisForFn(entityName);
            if (!fnApis.isEmpty()) {
                result = new FnRef(entity.getSpan(), makeApiIds(fnApis, entity));
            }
        }
                
        // Qualified name
        if (result == null) {
            List<Id> api = new ArrayList<Id>();
            while (result == null && !ConsList.isEmpty(fields)) {
                api.add(entity);
                entity = ConsList.first(fields);
                entityName = entity.getName();
                fields = ConsList.rest(fields);

                String apiName = asDottedString(api);
                if (_globalEnv.definesApi(apiName)) {
                    ApiIndex index = _globalEnv.api(apiName);
                    if (index.variables().containsKey(entityName)) {
                        if (ConsList.isEmpty(fields)) { return that; }
                        else {
                            result =
                                ExprFactory.makeVarRef(IterUtil.compose(api, entity));
                        }
                    }
                    else if (index.functions().containsFirst(entityName)) {
                        result = ExprFactory.makeFnRef(IterUtil.compose(api, entity));
                    }
                }
            }
        }
        
        if (result != null) {
            for (Id field : fields) {
                result = ExprFactory.makeFieldRef(result, field);
            }
            if (that.isParenthesized()) {
                result = ExprFactory.makeInParentheses(result);
            }
            return result;
        }
        else {
            String err = "Unrecognized name '" + NodeUtil.getName(that.getVar()) + "'";
            _errors.add(StaticError.make(err, that));
            return that;
        }
    }
    
    
    private static String asDottedString(Iterable<Id> ids) {
        Iterable<String> names = IterUtil.map(ids, NodeUtil.IdToStringFn);
        return IterUtil.toString(names, "", ".", "");
    }

    private static List<DottedId> makeApiIds(Set<String> apis, Id entity) {
        List<DottedId> result = new ArrayList<DottedId>(apis.size());
        for (String api : apis) {
            result.add(makeApiId(api, entity));
        }
        return result;
    }
    
    private static DottedId makeApiId(String api, Id entity) {
        List<Id> ids = new ArrayList<Id>(2);
        for (String piece : api.split("\\.")) {
            ids.add(new Id(entity.getSpan(), piece));
        }
        ids.add(entity);
        return new DottedId(entity.getSpan(), ids);
    }
    
}
