/*******************************************************************************
    Copyright 2009 Sun Microsystems, Inc.,
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
package com.sun.fortress.compiler.codegen.stubs;

public class Compiled2Stub {
    
    private class Object { 
        int x;
    }
 
   private interface String {
       public void setX(int val);
       public int getX();
    }

    private interface SillyString extends String {
        public void printX();
    }

    private class MySillyString extends Object implements SillyString {
        public void setX(int val) {x = val;}
        public int getX() {return x;}
        public void printX() {System.out.println("X=" + x);}
    }

    public static void run() {
        Compiled2Stub foo = new Compiled2Stub();
        MySillyString bar = foo.new MySillyString();
        bar.setX(7);
        bar.printX();
    }
    public static void main(String args[]) {
        run();
    }
}