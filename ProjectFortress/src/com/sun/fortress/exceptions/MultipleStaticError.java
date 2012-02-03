/*******************************************************************************
    Copyright 2008,2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

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
        return IterUtil.toString(IterUtil.sort(errors), "", "\n", "");
    }

    @Override
    public void printStackTrace(){
        for ( StaticError error : errors ){
            error.printStackTrace();
        }
    }
}
