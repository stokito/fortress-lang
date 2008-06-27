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

package com.sun.fortress.repository.graph;

public abstract class GraphNode{

    long age;
    public GraphNode(){
        this.age = 0;
    }

    public long getAge(){
        return age;
    }

    public void setAge(long age){
        this.age = age;
    }

    public boolean older(GraphNode node){
        return getAge() < node.getAge();
    }

    public abstract <T,F extends Throwable> T accept( GraphVisitor<T,F> g ) throws F;
}
