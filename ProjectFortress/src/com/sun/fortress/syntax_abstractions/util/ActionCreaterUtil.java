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

package com.sun.fortress.syntax_abstractions.util;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import xtc.util.Utilities;

import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.PrefixedSymbol;
import com.sun.fortress.syntax_abstractions.environments.SyntaxDeclEnv;

import edu.rice.cs.plt.tuple.Option;

public class ActionCreaterUtil {

    public static List<String> createVariabelBinding(List<Integer> indents,
            SyntaxDeclEnv syntaxDeclEnv, String BOUND_VARIABLES) {
        List<String> code = new LinkedList<String>();
        indents.add(3);
        code.add("Map<String, AbstractNode> "+BOUND_VARIABLES+" = new HashMap<String, AbstractNode>();");
        for(Id id: syntaxDeclEnv.getVariables()) {
            indents.add(3);
            code.add(BOUND_VARIABLES+".put(\""+id.getText()+"\""+", "+id.getText()+");");
        }
        return code;
    }

    public static List<String> createRatsAction(String serializedComponent, List<Integer> indents) {
        List<String> code = new LinkedList<String>();
        String[] sc = Utilities.SPACE_NEWLINE_SPACE.split(serializedComponent);
        indents.add(3);
        code.add("String code = "+"\""+sc[0].replaceAll("\"", "\\\\\"") + " \"+");
        for (int inx = 1; inx < sc.length; inx++) {
            String s = "\""+sc[inx].replaceAll("\"", "\\\\\"") + " \"";
            if (inx < sc.length-1) {
                s += "+";
            }
            else {
                s += ";";
            }
            indents.add(3);
            code.add(s);
        }
        return code;
    }
    
}
