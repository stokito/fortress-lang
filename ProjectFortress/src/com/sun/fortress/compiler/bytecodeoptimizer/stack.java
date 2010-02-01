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

class stack {
  int si;
  TypeState state[];

  void push(TypeState ts) {
    state[si++] = ts;
  }

  boolean Merge(stack s, int pc) {
    boolean changed = false;
    debug.println(1,"stack height = " + si + " or " + s.si);
    if (s.si != si) 
      throw new VerifyError("Stack Height Mismatch Error");

    for (int i = 0; i < si; i++) {
      TypeState result = TypeState.Merge(state[i], s.state[i], pc);
      if (!result.asString().equals(s.state[i].asString())) {
	debug.println(1,"Stack Nontrivial Merge: " + i + " : "+ state[i].asString() +
		      " and " + s.state[i].asString() + 
		      " at pc " + pc +
		      " yields " + result.asString());
	if (result == TypeState.typeTop)
	  throw new VerifyError("Type State MisMatch");
	s.state[i] = result;
	changed = true;
      }
    }
    return changed;
  }

  TypeState pop() {
    return state[--si];
  }

  stack(int depth) {
    state = new TypeState[depth + 1];
    for (int i = 0; i < depth + 1; i++)
      state[i] = TypeState.typeBottom;
    si = 0;
  }

  stack(stack s) {
    int len = s.state.length;
    state = new TypeState[len];
    System.arraycopy(s.state, 0, state, 0, len);
    si = s.si;
  }

  void clear() { si = 0;}
  
  void print() {
    debug.print(1,"     Stack ");

    for (int i = 0; i < si; i++) 
      debug.print(1, " : " + state[i].asString());
    debug.print(1,"");
  }
}

