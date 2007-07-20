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
/*
 * Created on Jan 31, 2007
 *
 */
package com.sun.fortress.interpreter.evaluator;

import java.util.Set;

import com.sun.fortress.useful.BASet;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.StringComparer;
import com.sun.fortress.useful.Useful;

public class CircularDependenceError extends ProgramError {

    public CircularDependenceError() {
        // TODO Auto-generated constructor stub
    }

    public CircularDependenceError(HasAt loc, Environment env, String arg0) {
        super(loc, env, arg0);
        // TODO Auto-generated constructor stub
    }

    public CircularDependenceError(HasAt loc, String arg0) {
        super(loc, arg0);
        // TODO Auto-generated constructor stub
    }

    public CircularDependenceError(HasAt loc1, HasAt loc2, Environment env,
            String arg0) {
        super(loc1, loc2, env, arg0);
        // TODO Auto-generated constructor stub
    }

    public CircularDependenceError(String arg0) {
        super(arg0);
        // TODO Auto-generated constructor stub
    }

    public CircularDependenceError(String arg0, Throwable arg1) {
        super(arg0, arg1);
        // TODO Auto-generated constructor stub
    }

    public CircularDependenceError(HasAt loc, Environment env, String arg0,
            Throwable arg1) {
        super(loc, env, arg0, arg1);
        // TODO Auto-generated constructor stub
    }

    public CircularDependenceError(Throwable arg0) {
        super(arg0);
        // TODO Auto-generated constructor stub
    }

    public CircularDependenceError(HasAt loc, String string, Throwable ex) {
        super(loc, string, ex);
        // TODO Auto-generated constructor stub
    }
    
    public void addParticipant(String s) {
        participants.add(s);
    }
    
    Set<String> participants = new BASet<String>(StringComparer.V);
    
    @Override
    public String getMessage() {
        return super.getMessage() + " Cycle participants are " + Useful.listInCurlies(participants) + ".";
    }

}
