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

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.sun.fortress.nodes.Id;
import com.sun.fortress.useful.Useful;

public class EllipsesEnvironment{
    private Map<Id,Storage> vars;
    public EllipsesEnvironment(){
        vars = new HashMap<Id,Storage>();
    }

    private class Storage{
        public int level;
        public Object value;
        public Storage( int l, Object o ){
            level = l;
            value = o;
        }
    }

    public Object getValue( Id id ){
        return vars.get( id ).value;
    }

    public List<Id> getVars(){
        return Useful.list( vars.keySet() );
    }

    public int getLevel( Id var ){
        return vars.get( var ).level;
    }

    public boolean contains( Id var ){
        return vars.containsKey( var );
    }

    public void add( Id var, int level, Object value ){
        vars.put( var, new Storage(level,value) );
    }

    public String toString(){
        StringBuilder s = new StringBuilder();

        for ( Map.Entry<Id,Storage> e : vars.entrySet() ){
            s.append( e.getKey().getText() + " : level " + e.getValue().level + " value " + e.getValue().value );
        }

        return s.toString();
    }
}
