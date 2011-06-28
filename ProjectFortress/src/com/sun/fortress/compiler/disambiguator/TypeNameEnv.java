/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.disambiguator;

import com.sun.fortress.compiler.WellKnownNames;
import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.compiler.index.TypeConsIndex;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdOrOp;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes_util.NodeFactory;
import edu.rice.cs.plt.tuple.Option;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class TypeNameEnv {
    /**
     * Produce the unaliased API name referenced by the given name, or a
     * "none" if the name is undefined.  For optimization, should return
     * the {@code name} object (wrapped as a "some") where possible, rather
     * than a duplication of it.
     */
    public abstract Option<APIName> apiName(APIName name);

    /**
     * Determine whether a type parameter with the given name is defined.
     */
    public abstract Option<StaticParam> hasTypeParam(IdOrOp name);

    /**
     * Produce the set of unaliased qualified names corresponding to the given
     * type constructor name; on-demand imports are ignored.  An undefined reference
     * produces an empty set, and an ambiguous reference produces a set of size greater
     * than 1.
     */
    public abstract Set<Id> explicitTypeConsNames(Id name);

    /**
     * Produce the set of unaliased qualified names available via on-demand imports
     * that correspond to the given type constructor name.  An undefined reference
     * produces an empty set, and an ambiguous reference produces a set of size
     * greater than 1.
     */
    public abstract Set<Id> onDemandTypeConsNames(Id name);

    /**
     * Given a disambiguated name (aliases and imports have been resolved),
     * determine whether a type constructor exists.  Assumes
     * {@code name.getApi().isSome()}.
     */
    public abstract boolean hasQualifiedTypeCons(Id name);

    /**
     * Given a disambiguated name (aliases and imports have been resolved),
     * provide the corresponding TypeConsIndex (assumed to exist).
     */
    public abstract TypeConsIndex typeConsIndex(Id name);

    /**
     * Returns a list of implicitly imported APIS.
     */
    public List<APIName> implicitlyImportedApis() {
        List<APIName> result = new ArrayList<APIName>();
        for (String defaultLib : WellKnownNames.defaultLibrary()) {
            result.add(NodeFactory.makeAPIName(NodeFactory.typeSpan, defaultLib));
        }
        return result;
    }

    /**
     * Returns true iff the given api is implicitly imported.
     */
    public boolean isImplicitlyImportedApi(APIName api) {
        for (APIName imported : implicitlyImportedApis()) {
            if (api.equals(imported)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Produce the set of unaliased qualified names corresponding to the given
     * grammar name; on-demand imports are ignored.  An undefined reference
     * produces an empty set, and an ambiguous reference produces a set of size greater
     * than 1.
     */
    public abstract Set<Id> explicitGrammarNames(String uqname);

    /**
     * Given a disambiguated name (aliases and imports have been resolved),
     * determine whether a grammar exists.  Assumes {@code name.getApi().isSome()}.
     */
    public abstract boolean hasQualifiedGrammar(Id name);

    /**
     * Determine whether a grammar with the given name is defined.
     */
    public abstract boolean hasGrammar(String name);

    /**
     * Produce the set of unaliased qualified names available via on-demand imports
     * that correspond to the given grammar name.  An undefined reference
     * produces an empty set, and an ambiguous reference produces a set of size
     * greater than 1.
     */
    public abstract Set<Id> onDemandGrammarNames(String name);

    /**
     * Given a disambiguated name (aliases and imports have been resolved),
     * return the corresponding grammar.  Assumes {@code name.getApi().isSome()}.
     */
    public abstract Option<GrammarIndex> grammarIndex(final Id name);
}
