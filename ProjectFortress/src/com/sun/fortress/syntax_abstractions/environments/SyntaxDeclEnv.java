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
import java.util.HashSet;
import java.util.Set;

import com.sun.fortress.nodes.AnyCharacterSymbol;
import com.sun.fortress.nodes.BackspaceSymbol;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.CarriageReturnSymbol;
import com.sun.fortress.nodes.CharacterClassSymbol;
import com.sun.fortress.nodes.FormfeedSymbol;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.NewlineSymbol;
import com.sun.fortress.nodes.NodeDepthFirstVisitor_void;
import com.sun.fortress.nodes.NonterminalSymbol;
import com.sun.fortress.nodes.OptionalSymbol;
import com.sun.fortress.nodes.PrefixedSymbol;
import com.sun.fortress.nodes.RepeatOneOrMoreSymbol;
import com.sun.fortress.nodes.RepeatSymbol;
import com.sun.fortress.nodes.SyntaxDef;
import com.sun.fortress.nodes.SyntaxSymbol;
import com.sun.fortress.nodes.TabSymbol;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.WhitespaceSymbol;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.syntax_abstractions.intermediate.SyntaxSymbolPrinter;
import com.sun.fortress.syntax_abstractions.util.TypeCollector;


public class SyntaxDeclEnv {

    private SyntaxDef sd;
    private final Set<Id> anyChars;
    private final Set<Id> characterClasses;
    private final Set<Id> options;
    private final Set<Id> repeats;
    private final Set<Id> specialSymbols;
    private final Map<Id, Id> varToNonterminalName;
    private final Map<Id, BaseType> varToType;
    private boolean init;
    private MemberEnv memberEnv;

    public SyntaxDeclEnv(SyntaxDef sd, MemberEnv memberEnv) {
        this.anyChars = new HashSet<Id>();
        this.characterClasses = new HashSet<Id>();
        this.options = new HashSet<Id>();
        this.repeats = new HashSet<Id>();
        this.specialSymbols = new HashSet<Id>();
        this.varToNonterminalName = new HashMap<Id, Id>();
        this.varToType = new HashMap<Id, BaseType>();
        this.init = false;
        this.sd = sd;
        this.memberEnv = memberEnv;
    }

    private void init() {
        for (SyntaxSymbol ss: this.sd.getSyntaxSymbols()) {
            ss.accept(new NodeDepthFirstVisitor_void() {
                @Override
                public void forPrefixedSymbolOnly(PrefixedSymbol that) {
                    assert(that.getId().isSome());
                    final Id id = that.getId().unwrap();
                    PrefixSymbolSymbolGetter psg = new PrefixSymbolSymbolGetter(id);
                    that.getSymbol().accept(psg);
                    anyChars.addAll(psg.getAnyChars());
                    characterClasses.addAll(psg.getCharacterClasses());
                    options.addAll(psg.getOptions());
                    repeats.addAll(psg.getRepeats());
                    specialSymbols.addAll(psg.getSpecialSymbols());
                    varToNonterminalName.putAll(psg.getVarToNonterminalName());
                    varToType.put(id, TypeCollector.getType(that));
                    super.forPrefixedSymbolOnly(that);
                }
            });
        }
        this.init = true;
    }

    public boolean contains(Id var) {
        if (!init)
            init();
        return this.varToNonterminalName.containsKey(var) ||
        this.characterClasses.contains(var) ||
        this.anyChars.contains(var);
    }

    public Collection<Id> getVariables() {
        if (!init)
            init();
        Collection<Id> s = new HashSet<Id>();
        s.addAll(this.varToNonterminalName.keySet());
        s.addAll(this.characterClasses);
        s.addAll(this.anyChars);
        return s;
    }

    public boolean isNonterminal(Id id) {
        if (!init)
            init();
        return this.varToNonterminalName.containsKey(id);
    }

    /**
     * Returns the name of the nonterminal the given variable is bound to
     * @param var
     * @return
     */
    public Id getNonterminalName(Id var) {
        if (!init)
            init();
        if (this.getMemberEnv().isParameter(var)) {
            throw new RuntimeException("Parameter changed: "+var);
//            Id s = this.getMemberEnv().getParameter(var);
//            return s;
        }
        return this.varToNonterminalName.get(var);
    }

    /**
     * Returns the type of the given variable
     * @param id
     * @return
     */
    public BaseType getType(Id id) {
        if (!init)
            init();
        if (this.isNonterminal(id)) {
            return this.varToType.get(id);
        } else if (this.isOption(id)) {
            throw new RuntimeException("NYI - Syntax declaration environment getType option: "+id);
        } else if (this.isRepeat(id)) {
            throw new RuntimeException("NYI - Syntax declaration environment getType repeat: "+id);
        } else if (this.isCharacterClass(id)) {
            return NodeFactory.makeTraitType("FortressAst", "CharLiteralExpr");
        } else if (this.isAnyChar(id)) {
            return NodeFactory.makeTraitType("FortressAst", "CharLiteralExpr");
        } else if (this.isSpecialSymbol(id)) {
            return NodeFactory.makeTraitType("FortressAst", "CharLiteralExpr");
        }        
        throw new RuntimeException("NYI - Syntax declaration environment getType: "+id);
    }

    public boolean isPatternVariable(Id id) {
        return isNonterminal(id) || isOption(id) || 
               isRepeat(id) || isAnyChar(id) || 
               isCharacterClass(id) || isSpecialSymbol(id);
    }
    
    public boolean isAnyChar(Id id) {
        if (!init)
            init();
        return this.anyChars.contains(id);
    }

    public boolean isCharacterClass(Id id) {
        if (!init)
            init();
        return this.characterClasses.contains(id);
    }

    public boolean isOption(Id id) {
        if (!init)
            init();
        return this.options.contains(id);
    }

    public boolean isRepeat(Id id) {
        if (!init)
            init();
        return this.repeats.contains(id);
    }

    private boolean isSpecialSymbol(Id id) {
        if (!init)
            init();
        return this.specialSymbols.contains(id);
    }
    
    /**
     * At this point the only symbols which should be children of a 
     * prefix symbol are nonterminal, optional, repeat one or more times, 
     * repeat, character classes or any chars, thus we only handle these cases. 
     * In the case of optional, repeat one or more times, and repeat, we are interested 
     * in the nonterminals they refer to.
     */
    public final static class PrefixSymbolSymbolGetter extends NodeDepthFirstVisitor_void {
        private final Id id;

        private Set<Id> anyChars;
        private Set<Id> characterClasses;
        private Set<Id> options;
        private Set<Id> repeats;
        private Set<Id> specialSymbols;
        private Map<Id, Id> varToNonterminalName;
        
        public PrefixSymbolSymbolGetter(Id id) {
            this.anyChars = new HashSet<Id>();
            this.characterClasses = new HashSet<Id>();
            this.options = new HashSet<Id>();
            this.repeats = new HashSet<Id>();
            this.specialSymbols = new HashSet<Id>();
            this.varToNonterminalName = new HashMap<Id, Id>();
            this.id = id;
        }

        public Map<Id, Id> getVarToNonterminalName() {
            return this.varToNonterminalName;
        }

        public Set<Id> getAnyChars() {
            return this.anyChars;
        }

        public Set<Id> getCharacterClasses() {
            return this.characterClasses;
        }

        public Set<Id> getOptions() {
            return this.options;
        }

        public Set<Id> getRepeats() {
            return this.repeats;
        }
        
        public Set<Id> getSpecialSymbols() {
            return this.specialSymbols;
        }
        @Override
        public void forNonterminalSymbol(NonterminalSymbol that) {
            this.varToNonterminalName.put(this.id, that.getNonterminal());
        }

        @Override
        public void forAnyCharacterSymbol(
                AnyCharacterSymbol that) {
            this.anyChars.add(this.id);
        }

        @Override
        public void forCharacterClassSymbol(CharacterClassSymbol that) {
            this.characterClasses.add(id);
        }

        @Override
        public void forOptionalSymbol(OptionalSymbol that) {
            this.options.add(id);
            super.forOptionalSymbol(that);
        }

        @Override
        public void forRepeatOneOrMoreSymbol(
                RepeatOneOrMoreSymbol that) {
            this.repeats.add(id);
            super.forRepeatOneOrMoreSymbol(that);
        }

        @Override
        public void forRepeatSymbol(RepeatSymbol that) {
            this.repeats.add(id);
            super.forRepeatSymbol(that);
        }
        
        @Override
        public void forTabSymbol(TabSymbol that) {
            this.specialSymbols.add(id);
            super.forTabSymbol(that);
        }      
        
        @Override
        public void forFormfeedSymbol(FormfeedSymbol that) {
            this.specialSymbols.add(id);
            super.forFormfeedSymbol(that);
        }      
        

        @Override
        public void forCarriageReturnSymbol(CarriageReturnSymbol that) {
            this.specialSymbols.add(id);
            super.forCarriageReturnSymbol(that);
        }      

        @Override
        public void forNewlineSymbol(NewlineSymbol that) {
            this.specialSymbols.add(id);
            super.forNewlineSymbol(that);
        }      

        @Override
        public void forWhitespaceSymbol(WhitespaceSymbol that) {
            this.specialSymbols.add(id);
            super.forWhitespaceSymbol(that);
        }      

        @Override
        public void forBackspaceSymbol(BackspaceSymbol that) {
            this.specialSymbols.add(id);
            super.forBackspaceSymbol(that);
        }      

    }

    public MemberEnv getMemberEnv() {
        return this.memberEnv;
    }
    
    @Override
    public String toString() {
        String st = "";
        for (SyntaxSymbol s: sd.getSyntaxSymbols()) {
            st += " "+s.accept(new SyntaxSymbolPrinter());
        }
        st += "\n AnyChars: "+this.anyChars;
        st += "\n Characterclasses: "+this.characterClasses;
        st += "\n Options: "+this.options;
        st += "\n Repeats: "+this.repeats;
        st += "\n Nonterminals: "+this.varToNonterminalName;
        return st;
    }

}
