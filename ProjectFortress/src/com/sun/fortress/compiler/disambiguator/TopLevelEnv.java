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

import java.util.Set;
import java.util.Collections;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.OptionVisitor;

import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.index.TypeConsIndex;
import com.sun.fortress.compiler.index.CompilationUnitIndex;
import com.sun.fortress.nodes.IdName;
import com.sun.fortress.nodes.OpName;
import com.sun.fortress.nodes.DottedName;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.nodes.QualifiedOpName;
import com.sun.fortress.nodes_util.NodeFactory;

import com.sun.fortress.useful.NI;

public class TopLevelEnv extends NameEnv {
    private GlobalEnvironment _globalEnv;
    private CompilationUnitIndex _current;
    
    public TopLevelEnv(GlobalEnvironment globalEnv, CompilationUnitIndex current) {
        _globalEnv = globalEnv;
        _current = current;
    }
    
    public Option<DottedName> apiName(DottedName name) {
        return NI.nyi();
    }
    
    public boolean hasTypeParam(IdName name) {
        return false;
    }

    public Set<QualifiedIdName> explicitTypeConsNames(IdName name) {
        // TODO: imports
        if (_current.typeConses().containsKey(name)) {
            return Collections.singleton(NodeFactory.makeQualifiedIdName(name));
        }
        else { return Collections.emptySet(); }
    }
    
    public Set<QualifiedIdName> explicitVariableNames(IdName name) {
        // TODO: imports
        if (_current.variables().containsKey(name)) {
            return Collections.singleton(NodeFactory.makeQualifiedIdName(name));
        }
        else { return Collections.emptySet(); }
    }
    
    public Set<QualifiedIdName> explicitFunctionNames(IdName name) {
        // TODO: imports
        if (_current.functions().containsFirst(name)) {
            return Collections.singleton(NodeFactory.makeQualifiedIdName(name));
        }
        else { return Collections.emptySet(); }
    }
    
    public Set<QualifiedOpName> explicitFunctionNames(OpName name) {
        // TODO: imports
        if (_current.functions().containsFirst(name)) {
            return Collections.singleton(NodeFactory.makeQualifiedOpName(name));
        }
        else { return Collections.emptySet(); }
    }

    public Set<QualifiedIdName> onDemandTypeConsNames(IdName name) {
        // TODO: imports
        return Collections.emptySet();
    }
    
    public Set<QualifiedIdName> onDemandVariableNames(IdName name) {
        // TODO: imports
        return Collections.emptySet();
    }
    
    public Set<QualifiedIdName> onDemandFunctionNames(IdName name) {
        // TODO: imports
        return Collections.emptySet();
    }
    
    public Set<QualifiedOpName> onDemandFunctionNames(OpName name) {
        // TODO: imports
        return Collections.emptySet();
    }
    
    
    public boolean hasQualifiedTypeCons(QualifiedIdName name) {
        DottedName api = Option.unwrap(name.getApi());
        if (_globalEnv.definesApi(api)) {
            return _globalEnv.api(api).typeConses().containsKey(name);
        }
        else { return false; }
    }
    
    public boolean hasQualifiedVariable(QualifiedIdName name) {
        DottedName api = Option.unwrap(name.getApi());
        if (_globalEnv.definesApi(api)) {
            return _globalEnv.api(api).variables().containsKey(name.getName());
        }
        else { return false; }
    }
    
    public boolean hasQualifiedFunction(QualifiedIdName name) {
        DottedName api = Option.unwrap(name.getApi());
        if (_globalEnv.definesApi(api)) {
            return _globalEnv.api(api).functions().containsFirst(name.getName());
        }
        else { return false; }
    }
    
    public TypeConsIndex typeConsIndex(final QualifiedIdName name) {
        return name.getApi().apply(new OptionVisitor<DottedName, TypeConsIndex>() {
            public TypeConsIndex forSome(DottedName api) {
                return _globalEnv.api(api).typeConses().get(name.getName());
            }
            public TypeConsIndex forNone() {
                return _current.typeConses().get(name.getName());
            }
        });
    }
    
}
