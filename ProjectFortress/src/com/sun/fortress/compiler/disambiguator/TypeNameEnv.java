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

package com.sun.fortress.compiler.disambiguator;

import java.util.*;

import edu.rice.cs.plt.tuple.Option;

import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.compiler.index.TypeConsIndex;

public abstract class TypeNameEnv {
    /**
     * Produce the unaliased API name referenced by the given name, or a
     * "none" if the name is undefined.  For optimization, should return
     * the {@code name} object (wrapped as a "some") where possible, rather
     * than a duplication of it.
     */
    public abstract Option<APIName> apiName(APIName name);

    /** Determine whether a type parameter with the given name is defined. */
    public abstract boolean hasTypeParam(Id name);

    /**
     * Produce the set of unaliased qualified names corresponding to the given
     * type constructor name; on-demand imports are ignored.  An undefined reference
     * produces an empty set, and an ambiguous reference produces a set of size greater
     * than 1.
     */
    public abstract Set<QualifiedIdName> explicitTypeConsNames(Id name);
    /**
     * Produce the set of unaliased qualified names available via on-demand imports
     * that correspond to the given type constructor name.  An undefined reference
     * produces an empty set, and an ambiguous reference produces a set of size
     * greater than 1.
     */
    public abstract Set<QualifiedIdName> onDemandTypeConsNames(Id name);

    /**
     * Given a disambiguated name (aliases and imports have been resolved),
     * determine whether a type constructor exists.  Assumes
     * {@code name.getApi().isSome()}.
     */
    public abstract boolean hasQualifiedTypeCons(QualifiedIdName name);
    /**
     * Given a disambiguated name (aliases and imports have been resolved),
     * provide the corresponding TypeConsIndex (assumed to exist).
     */
    public abstract TypeConsIndex typeConsIndex(QualifiedIdName name);
    /**
     * Returns a list of implicitly imported APIS.
     */
    public List<APIName> implicitlyImportedApis() {
        List<APIName> result = new ArrayList<APIName>();
        result.add(NodeFactory.makeAPIName("FortressBuiltin"));
        result.add(NodeFactory.makeAPIName("FortressLibrary"));
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
    public abstract Set<QualifiedIdName> explicitGrammarNames(QualifiedIdName name);
    /**
     * Given a disambiguated name (aliases and imports have been resolved),
     * determine whether a grammar exists.  Assumes {@code name.getApi().isSome()}.
     */
    public abstract boolean hasQualifiedGrammar(QualifiedIdName name);
    /** Determine whether a grammar with the given name is defined. */
    public abstract boolean hasGrammar(QualifiedIdName name);
    /**
     * Produce the set of unaliased qualified names available via on-demand imports
     * that correspond to the given grammar name.  An undefined reference
     * produces an empty set, and an ambiguous reference produces a set of size
     * greater than 1.
     */
    public abstract Set<QualifiedIdName> onDemandGrammarNames(Id name);

    /**
     * Given a disambiguated name (aliases and imports have been resolved),
     * return the corresponding grammar.  Assumes {@code name.getApi().isSome()}.
     */
    public abstract Option<GrammarIndex> grammarIndex(final QualifiedIdName name);
}
