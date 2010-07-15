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
package com.sun.fortress.compiler.asmbytecodeoptimizer;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.*;
import org.objectweb.asm.util.*;

public class Substitution {

    List<Insn> match;
    List<Insn> replacements;

    Substitution(List<Insn> match, List<Insn> replacements) {
        this.match = match;
        this.replacements = replacements;
    }

    boolean insnMatch(Insn m1, Insn m2) {
        if (m1 instanceof MethodInsn & m2 instanceof MethodInsn) {
            MethodInsn mi1 = (MethodInsn) m1;
            MethodInsn mi2 = (MethodInsn) m2;
            return mi1.matches(mi2);
        } else if (m1 instanceof LabelInsn & m2 instanceof LabelInsn) {
            LabelInsn li1 = (LabelInsn) m1;
            LabelInsn li2 = (LabelInsn) m2;
            return li1.matches(li2);
        } else if (m1 instanceof VisitLineNumberInsn & m2 instanceof VisitLineNumberInsn) {
            VisitLineNumberInsn vlni1 = (VisitLineNumberInsn) m1;
            VisitLineNumberInsn vlni2 = (VisitLineNumberInsn) m2;
            return vlni1.matches(vlni2);
        } else return false;
    }

    boolean isAMatch(ByteCodeMethodVisitor bcmv, int i) {
        boolean result = true;

        for (int j = 0; j < match.size(); j++) {
            if (!insnMatch(bcmv.insns.get(i+j), match.get(j))) {
                result = false;
                break;
            }
        }

        return result;
    }

    void makeReplacements(ByteCodeMethodVisitor bcmv, int i) {
        // Every instruction has a label instruction with it.
        for (int j = 0; j < match.size(); j++)
            bcmv.insns.remove(i);
        for (int j = replacements.size()-1; j >= 0; j--) 
            bcmv.insns.add(i, replacements.get(j));
    }

    public void makeSubstitution(ByteCodeMethodVisitor bcmv) {
        for (int i = 0; i < bcmv.insns.size() - match.size(); i++) {
            if (isAMatch(bcmv, i)) {
                makeReplacements(bcmv, i);
                //                System.out.println("Optimization starting at " + bcmv.insns.get(i));
            }
        }
    }
}