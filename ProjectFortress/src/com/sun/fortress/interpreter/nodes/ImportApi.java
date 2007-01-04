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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// / and import_api = import_api_rec node
// / and import_api_rec =
// / {
// / import_api_source : dotted_name;
// / import_api_as : dotted_name option;
// / }
// /
public class ImportApi extends Import {

    List<AliasedDottedId> apis;

    public ImportApi(Span span, List<AliasedDottedId> apis) {
        super(span);
        this.apis = apis;
    }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forImportApi(this);
    }

    ImportApi(Span span) {
        super(span);
    }

    /**
     * @return Returns the as.
     */
    public List<AliasedDottedId> getApis() {
        return apis;
    }
}
