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

package com.sun.fortress.syntax_abstractions.environments;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.NodeDepthFirstVisitor;
import com.sun.fortress.nodes.NodeDepthFirstVisitor_void;
import com.sun.fortress.nodes.NonterminalSymbol;
import com.sun.fortress.nodes.PrefixedSymbol;
import com.sun.fortress.nodes.SyntaxDef;
import com.sun.fortress.nodes.SyntaxSymbol;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.syntax_abstractions.util.TypeCollector;

import edu.rice.cs.plt.tuple.Option;

public class SyntaxDeclEnv {

 private Map<Id, Type> varToTypeMap;
 private Map<Id, Id> varToNonterminalName;

 private SyntaxDeclEnv() {
  this.varToTypeMap = new HashMap<Id, Type>();
  this.varToNonterminalName = new HashMap<Id, Id>();
 }

 public static SyntaxDeclEnv getEnv(SyntaxDef sd) {
  final SyntaxDeclEnv sdEnv = new SyntaxDeclEnv();
  for (SyntaxSymbol ss: sd.getSyntaxSymbols()) {

   ss.accept(new NodeDepthFirstVisitor_void() {

    @Override
    public void forPrefixedSymbolOnly(PrefixedSymbol that) {
     Option<Type> type = TypeCollector.getType(that);
     if (type.isSome()) {
      Id id = that.getId().unwrap();
      sdEnv.add(id, type.unwrap());
      if (that.getSymbol() instanceof NonterminalSymbol) {
       sdEnv.addVarToNonterminal(id, ((NonterminalSymbol) that.getSymbol()).getNonterminal());
      }
      else {
       throw new RuntimeException("Only nonterminals may be bound "+that.getSymbol().getClass());
      }
     }
     super.forPrefixedSymbolOnly(that);
    }
   });
  }
  return sdEnv;
 }

 protected void addVarToNonterminal(Id id, Id name) {
  this.varToNonterminalName.put(id, name);
 }

 private void add(Id var, Type type) {
  this.varToTypeMap.put(var, type);
 }

 public boolean contains(Id var) {
  return this.varToTypeMap.containsKey(var);
 }

 public Type getType(Id var) {
  return this.varToTypeMap.get(var);
 }

 public Collection<Id> getVariables() {
  return this.varToTypeMap.keySet();
 }

 public Id getNonterminalName(Id var) {
  return this.varToNonterminalName.get(var);
 }

}
