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

package com.sun.fortress.exceptions;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import com.sun.fortress.nodes_util.ErrorMsgMaker;
import com.sun.fortress.useful.HasAt;
import edu.rice.cs.plt.iter.IterUtil;

public class MultipleStaticError extends StaticError implements Iterable<StaticError> {
    List<StaticError> errors;

    public MultipleStaticError(Iterable<? extends StaticError> errors){
        this.errors = new ArrayList<StaticError>();
        for ( StaticError error : errors ){
            this.errors.add( error );
        }
    }

    public Iterator<StaticError> iterator(){
        return errors.iterator();
    }

    public String at(){
        return "";
    }

    public String description(){
        return "";
    }

    public String toString() {
        StringBuilder buffer = new StringBuilder();
        for ( StaticError error : errors ){
            buffer.append(error).append("\n");
        }
        return buffer.toString();
    }

    @Override
    public void printStackTrace(){
        for ( StaticError error : errors ){
            error.printStackTrace();
        }
    }
}
