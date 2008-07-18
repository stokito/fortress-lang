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

package com.sun.fortress.syntax_abstractions.phases;

import java.util.Map;
import java.util.HashMap;

public class SyntaxTransformerManager {

    private static SyntaxTransformerManager manager = new SyntaxTransformerManager();

    private Map<String,SyntaxTransformer> transformers;
    private SyntaxTransformerManager(){
        this.transformers = new HashMap<String,SyntaxTransformer>();
    }

    public static void addTransformer( String name, SyntaxTransformer transformer ){
        manager.transformers.put( name, transformer );
    }

    public static SyntaxTransformer getTransformer( String name ){
        return manager.transformers.get( name );
    }
}
