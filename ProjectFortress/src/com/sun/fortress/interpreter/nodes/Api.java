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

package com.sun.fortress.interpreter.nodes;

import java.util.List;

// / and api = api_rec node
// / and api_rec =
// / {
// / api_name : dotted_name;
// / api_imports : import list;
// / api_decls : decl list;
// / }
// /
public class Api extends CompilationUnit {
    DottedId name;

    List<Import> imports;

    List<? extends DefOrDecl> decls;

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forApi(this);
    }

    Api(Span span) {
        super(span);
    }

    public Api(Span span, DottedId id, List<Import> is,
            List<? extends DefOrDecl> ds) {
        super(span);
        this.name = id;
        this.imports = is;
        this.decls = ds;
    }

    /**
     * @return Returns the decls.
     */
    public List<? extends DefOrDecl> getDecls() {
        return decls;
    }

    /**
     * @return Returns the imports.
     */
    public List<Import> getImports() {
        return imports;
    }

    /**
     * @return Returns the name.
     */
    public DottedId getName() {
        return name;
    }
}
