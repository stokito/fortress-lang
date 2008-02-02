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
import java.util.Map;
import java.util.Set;

import com.sun.fortress.compiler.Fortress;
import com.sun.fortress.compiler.FortressRepository;
import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.StaticError;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.interpreter.evaluator.ProgramError;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.Component;
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
        
        Set<Api> staleA = newStaleApis();
        Set<Component> staleC = newStaleComponents();
        
        // The typechecker is insufficienbtly lazy, and works too hard in the zero case.
        if (staleA.size() == 0 && staleC.size() == 0)
            return;
        
        Map<APIName, ApiIndex> apimap = this.apis();
        
        System.err.println("Refresh analyzing\nstale apis       = " + newStaleApiNames() +
                                            "\nstale components = " + newStaleComponentNames() +
                                            "\nknown apis       = " + apimap.keySet()
                                            );
        
        
        
        Iterable<? extends StaticError> errors =
            fort.analyze(new GlobalEnvironment.FromMap(apimap),
                    staleA,
                    staleC,
                    System.currentTimeMillis());
        
        if (errors.iterator().hasNext()) {
            resetStale();
            throw new ProgramError(errors);
        }
    }

}
