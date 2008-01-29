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

package com.sun.fortress.shell;

import java.io.IOException;

import com.sun.fortress.compiler.Fortress;
import com.sun.fortress.compiler.FortressRepository;
import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.StaticError;
import com.sun.fortress.interpreter.evaluator.ProgramError;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.useful.Path;
import com.sun.fortress.useful.ReversedList;

public class BatchCachingAnalyzingRepository extends BatchCachingRepository {

    public BatchCachingAnalyzingRepository(FortressRepository source,
            FortressRepository cache) {
        super(source, cache);
        // TODO Auto-generated constructor stub
    }

    public BatchCachingAnalyzingRepository(boolean doLink, Path p,
            FortressRepository cache) {
        super(doLink, p, cache);
        // TODO Auto-generated constructor stub
    }

    public BatchCachingAnalyzingRepository(Path p, FortressRepository cache) {
        super(p, cache);
        // TODO Auto-generated constructor stub
    }

    public BatchCachingAnalyzingRepository(boolean doLink,
            FortressRepository source, FortressRepository cache) {
        super(doLink, source, cache);
        // TODO Auto-generated constructor stub
    }
    
    protected void refreshCache() {
        
        Fortress fort = new Fortress(this);
        
        Iterable<? extends StaticError> errors = fort.analyze(new GlobalEnvironment.FromRepository(this), newStaleApis(), newStaleComponents(), System.currentTimeMillis());
        
        if (errors.iterator().hasNext()) {
            throw new ProgramError(errors);
        }
    }

}
