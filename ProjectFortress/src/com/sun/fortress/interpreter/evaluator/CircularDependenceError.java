/*
 * Created on Jan 31, 2007
 *
 */
package com.sun.fortress.interpreter.evaluator;

import java.util.Set;

import com.sun.fortress.interpreter.useful.BASet;
import com.sun.fortress.interpreter.useful.HasAt;
import com.sun.fortress.interpreter.useful.StringComparer;
import com.sun.fortress.interpreter.useful.Useful;

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
