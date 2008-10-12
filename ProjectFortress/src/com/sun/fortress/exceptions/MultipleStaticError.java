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

import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import com.sun.fortress.nodes_util.ErrorMsgMaker;
import com.sun.fortress.useful.HasAt;
import edu.rice.cs.plt.iter.IterUtil;

public class MultipleStaticError extends StaticError implements Iterable<StaticError> {

    /**
     * Make Eclipse happy
     */
    private static final long serialVersionUID = -4970962399662866759L;

    Set<StaticError> errors;
    Set<String> messages;

    public MultipleStaticError(Iterable<? extends StaticError> errors){
        this.errors = new HashSet<StaticError>();
        this.messages = new HashSet<String>();
        for ( StaticError error : errors ){
            if ( ! messages.contains(error.toString()) ) {
                this.errors.add( error );
                this.messages.add( error.toString() );
            }
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
