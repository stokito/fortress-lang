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

import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.List;
import java.util.LinkedList;

import com.sun.fortress.nodes.NodeDepthFirstVisitor;
import com.sun.fortress.nodes.PrefixedSymbol;
import com.sun.fortress.nodes.GroupSymbol;
import com.sun.fortress.nodes.SyntaxSymbol;
import com.sun.fortress.nodes.SyntaxDef;

import com.sun.fortress.nodes.RepeatSymbol;
import com.sun.fortress.nodes.RepeatOneOrMoreSymbol;
import com.sun.fortress.nodes.OptionalSymbol;

public class VariableCollector extends NodeDepthFirstVisitor<Map<PrefixedSymbol, VariableCollector.Depth>> {
    
    private Depth depth;

    public interface Depth {
        public Depth getParent();
        public String getType(String baseType);
        public <T> T accept(DepthVisitor<T> visitor);
        // public String createCode(String id, List<String> code, List<Integer> indents);
    }

    public interface DepthVisitor<T> {
        T forBaseDepth(Depth d);
        T forListDepth(Depth d);
        T forOptionDepth(Depth d);
    }

    static class BaseDepth implements Depth {
        public Depth getParent() {
            throw new UnsupportedOperationException("cannot get parent of BaseDepth");
        }
        public String getType(String baseType) {
            return baseType;
        }
        public <T> T accept(DepthVisitor<T> visitor) {
            return visitor.forBaseDepth(this);
        }
    }

    static class ListDepth implements Depth {
        private Depth d;
        ListDepth(Depth d) { this.d = d; }
        public Depth getParent(){
            return d;
        }
        public String getType(String baseType){
            return "List<" + d.getType(baseType) + ">";
        }
        public <T> T accept(DepthVisitor<T> visitor) {
            return visitor.forListDepth(this);
        }
    }

    static class OptionDepth implements Depth {
        private Depth d;
        OptionDepth(Depth d) { this.d = d; }
        public Depth getParent(){
            return d;
        }
        public String getType(String baseType) {
            return "Option<" + d.getType(baseType) + ">";
        }
        public <T> T accept(DepthVisitor<T> visitor) {
            return visitor.forOptionDepth(this);
        }
    }

    public VariableCollector() {
        this.depth = new BaseDepth();
    }

    private VariableCollector(Depth depth) {
        this.depth = depth;
    }

    @Override
    public Map<PrefixedSymbol,Depth> defaultCase(com.sun.fortress.nodes.Node that) {
        return new HashMap<PrefixedSymbol,Depth>();
    }	

    @Override
    public Map<PrefixedSymbol,Depth> forPrefixedSymbol(PrefixedSymbol that) {
        Map<PrefixedSymbol,Depth> c = super.forPrefixedSymbol(that);
        if (that.getId().isSome()) {
            c.put(that, depth);
        }
        return c;
    }
    
    @Override
    public Map<PrefixedSymbol,Depth> forSyntaxDef(SyntaxDef that) {
        Map<PrefixedSymbol,Depth> all = new HashMap<PrefixedSymbol,Depth>();
        for ( SyntaxSymbol s : that.getSyntaxSymbols() ){
            all.putAll( s.accept(this) );
        }
        return all;
    }

    @Override
    public Map<PrefixedSymbol,Depth> forGroupSymbol(GroupSymbol that) {
        Map<PrefixedSymbol,Depth> c = super.forGroupSymbol(that);
        for ( SyntaxSymbol symbol : that.getSymbols() ){
            c.putAll( symbol.accept(this) );
        }
        System.out.println( "Bound symbols for group: " + c );
        return c;
    }

    @Override
    public Map<PrefixedSymbol,Depth> forRepeatSymbol(RepeatSymbol that) {
        return that.getSymbol().accept(new VariableCollector(addStar(depth)));
    }

    @Override
    public Map<PrefixedSymbol,Depth> forRepeatOneOrMoreSymbol(RepeatOneOrMoreSymbol that) {
        return that.getSymbol().accept(new VariableCollector(addPlus(depth)));
    }

    @Override
    public Map<PrefixedSymbol,Depth> forOptionalSymbol(OptionalSymbol that) {
        return that.getSymbol().accept(new VariableCollector(addOptional(depth)));
    }

    private Depth addStar(Depth d) {
        return new ListDepth(d);
    }
    private Depth addPlus(Depth d) {
        return new ListDepth(d);
    }
    private Depth addOptional(Depth d) {
        return new OptionDepth(d);
    }

}
