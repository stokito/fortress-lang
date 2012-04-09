/*******************************************************************************
    Copyright 2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/
package com.sun.fortress.compiler.asmbytecodeoptimizer;

import java.util.HashSet;
import java.util.Set;

public class AbstractInterpretationValue {
    private static int count = 0;
    private int valueNumber;
    private Set<Insn> defs;
    private Set<Insn> uses;
    private boolean needed;
    private Insn definition;
    private String type;

    AbstractInterpretationValue(Insn definition, String type) {
        this.valueNumber = count++;
        this.defs = new HashSet<Insn>();
        this.defs.add(definition);
        this.uses = new HashSet<Insn>();
        this.needed = true;
        this.definition = definition;
        this.type = type;
        if (type.equals("J") || type.equals("D"))
            count++;
    }

    public static void initializeCount() { count = 0;}
    public int getValueNumber() { return valueNumber;}
    public boolean isNeeded() { return needed;}
    public boolean notNeeded() { return !needed;}
    public void setNeeded() { needed = true;}
    public void setUnNeeded() {needed = false;}
    public Insn getDefinition() { return definition;}
    public String getType() { return type;}
    

    public void addUse(Insn i) {
        uses.add(i);
        i.addUse(this);
    }

    public void setUses(Set<Insn> s) { uses = s;}
    public Set<Insn> getUses() { return uses;}

    public void addDefinition(Insn i) {
        defs.add(i);
        i.addDef(this);
    }

    public Set<Insn> getDefs() { return defs;}

    public boolean isBoxed() {
        if (this instanceof AbstractInterpretationBoxedValue) return true; else return false;
    }

    public String usesString() {
        String result = " ";
        for (Insn i : uses) 
            result = result + "\n         " + i;
        return result;
    }

    public String defsString() {
        String result = " ";
        for (Insn i : defs)
            result = result + "\n         " + i;
        return result;
    }

    public String idString() {
        return "AIV:";
    }

    public String toString() { 
        return idString() + valueNumber;

        //        return idString() + valueNumber + " needed = " + needed + 
        //            " with type " + type + "\n     defs = " + defsString() + 
        //            "\n     used in " + usesString() + "\n";
    }
}
