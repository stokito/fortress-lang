/*******************************************************************************
    Copyright 2010 Sun Microsystems, Inc.,
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
package com.sun.fortress.compiler.bytecodeoptimizer;

class locals {
  TypeState state[];
  
  void store(TypeState ts, int i) {
    state[i] = ts;
  }
  
  TypeState read(int i) {
    return state[i];
  }

  boolean Merge(locals l, int pc) {
    boolean changed = false;
    if (l.state.length != state.length)
      throw new VerifyError("Local Merge Error");
    for (int i = 0; i < state.length; i++) {
      TypeState result = TypeState.Merge(state[i],l.state[i], pc);
      if (!result.asString().equals(l.state[i].asString())) {
	debug.println(1,"Locals Nontrivial Merge: " + i + ":" + state[i].asString() +
		      " and " + l.state[i].asString() + 
		      " at pc " + pc +
		      " yields " + result.asString());
	l.state[i] = result;
	changed = true;
      }
    }
    return changed;
  }

  locals(int max) {
    state = new TypeState[max + 1];
    for (int i = 0; i < max + 1; i++)
      state[i] = TypeState.typeBottom;
  }

  locals(locals l) {
    int len = l.state.length;
    state = new TypeState[len];
    System.arraycopy(l.state, 0, state, 0, len);
  }
  
  void print() {
    debug.print(1,"     locals ");
    for (int i = 0; i < state.length; i++) 
      debug.print(1," : " + state[i].asString());
    debug.println(1,"");
  }
}



