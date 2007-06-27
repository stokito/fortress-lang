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

import com.sun.fortress.interpreter.nodes_util.Span;
import java.util.List;

// / and component = component_rec node
// / and component_rec =
// / {
// / component_name : dotted_name;
// / component_imports : import list;
// / component_exports : export list;
// / component_defs : def list;
// / }
// /
public class Component extends CompilationUnit {
    
    List<Export> exports;

    List<? extends DefOrDecl> defs;

    // Constructor for general, especially Rats!, use
    public Component(Span span, DottedId name, List<Import> imports,
            List<Export> exports, List<? extends DefOrDecl> defs) {
        super(span);
        this.name = name;
        this.imports = imports;
        this.exports = exports;
        this.defs = defs;
    }

    /**
     * @return Returns the defs.
     */
    public List<? extends DefOrDecl> getDefs() {
        return defs;
    }

    /**
     * @return Returns the exports.
     */
    public List<Export> getExports() {
        return exports;
    }

   

    // Necessary for vistor pattern
    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forComponent(this);
    }

    // Necessary for reflective creation.
    public Component(Span span) {
        super(span);
    }
}
