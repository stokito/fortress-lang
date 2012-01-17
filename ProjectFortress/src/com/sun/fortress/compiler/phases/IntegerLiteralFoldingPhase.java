/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.phases;

import com.sun.fortress.compiler.AnalyzeResult;
import com.sun.fortress.compiler.IntegerLiteralfolder;
import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.exceptions.MultipleStaticError;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.useful.Debug;
import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;

public class IntegerLiteralFoldingPhase extends Phase {

    public IntegerLiteralFoldingPhase(Phase parentPhase) {
        super(parentPhase);
    }

    @Override
    public AnalyzeResult execute() throws StaticError {
        Debug.debug(Debug.Type.FORTRESS, 1, "Start phase Integer Literal Folding");
        AnalyzeResult previous = parentPhase.getResult();

       IntegerLiteralfolder.ComponentResult componentsFolded = IntegerLiteralfolder.foldComponents(previous.components()); 
       
        return new AnalyzeResult(previous.apis(),
                componentsFolded.components(),
                IterUtil.<StaticError>empty());
    }

}