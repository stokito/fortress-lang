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
import java.util.Map;

import xtc.util.Utilities;

import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Enclosing;
import com.sun.fortress.nodes.EnclosingFixity;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.Fixity;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.Op;
import com.sun.fortress.nodes.OpExpr;
import com.sun.fortress.nodes.OpName;
import com.sun.fortress.nodes.OpRef;
import com.sun.fortress.nodes.PrefixedSymbol;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.TraitType;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.TypeArg;
import com.sun.fortress.nodes.VarType;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.syntax_abstractions.environments.SyntaxDeclEnv;
import com.sun.fortress.syntax_abstractions.phases.VariableCollector;
import com.sun.fortress.syntax_abstractions.rats.util.FreshName;
import com.sun.fortress.useful.NI;
import com.sun.fortress.useful.Debug;

import edu.rice.cs.plt.tuple.Option;

public class ActionCreaterUtil {

    public static List<String> createVariableBinding(final List<Integer> indents,
            final SyntaxDeclEnv syntaxDeclEnv, String BOUND_VARIABLES,
            boolean isTemplate, Map<PrefixedSymbol,VariableCollector.Depth> variables) {
        final List<String> code = new LinkedList<String>();
        indents.add(3);
        code.add("Map<String, Object> "+BOUND_VARIABLES+" = new HashMap<String, Object>();");
        final List<String> listCode = new LinkedList<String>();
        final List<Integer> listIndents = new LinkedList<Integer>();

        for ( Map.Entry<PrefixedSymbol,VariableCollector.Depth> pair : variables.entrySet() ){
            final PrefixedSymbol sym = pair.getKey();
            VariableCollector.Depth depth = pair.getValue();
            String var = sym.getId().unwrap().getText();
            if ( isTemplate ){
                Debug.debug( Debug.Type.SYNTAX, 1, "Depth for ", sym, " is ", depth );
                final String astNode = SyntaxAbstractionUtil.getJavaType(syntaxDeclEnv, sym.getId().unwrap());
                class DepthConvertVisitor implements VariableCollector.DepthVisitor<String> {
                    String source;
                    int indent;
                    DepthConvertVisitor(String source, int indent) {
                        this.source = source;
                        this.indent = indent;
                    }

                    public String forBaseDepth(VariableCollector.Depth d) {
                        Id id = sym.getId().unwrap();
                        if (syntaxDeclEnv.isCharacterClass(id)){
                            return getFortressCharacterClass(source, code, indents);  
                        }

                        if (syntaxDeclEnv.isAnyChar(id)) {
                            return getFortressAnyChar(source, code, indents);  
                        }

                        return source;
                    }

                    private void addCodeLine(String line){
                        listIndents.add(indent);
                        listCode.add(line);
                    }

                    public String forListDepth(VariableCollector.Depth d) {
                        String fresh = FreshName.getFreshName("list");
                        // String innerType = d.getParent().getType("Object"); // FIXME
                        String innerType = d.getParent().getType(astNode);
                        String iterator = FreshName.getFreshName("iter");
                        addCodeLine(String.format("List<Expr> %s = new LinkedList<Expr>();", fresh));

                        addCodeLine(String.format("for (%s %s : %s) {", innerType, iterator, source));

                        String parentVar = d.getParent().accept
                            (new DepthConvertVisitor(iterator, indent + 2));

                        indent += 2;
                        addCodeLine(String.format("%s.add(%s);", fresh, parentVar));
                        indent -= 2;

                        addCodeLine( "}" );
                        return getFortressList(fresh, listCode, listIndents);
                    }

                    public String forOptionDepth(VariableCollector.Depth d) {
                        String fresh = FreshName.getFreshName("option");
                        String innerType = d.getParent().getType("Object"); // FIXME
                        addCodeLine(String.format("Expr %s = null;", fresh));

                        addCodeLine(String.format("if (%s != null) {", source));

                        String parentVar = d.getParent().accept
                            (new DepthConvertVisitor(source, indent + 2));
                        indent += 2;
                        addCodeLine(String.format("%s = %s;", fresh, parentVar));
                        indent -= 2;

                        addCodeLine("}");

                        // return getFortressList(fresh, listCode, listIndents);
                        return fresh;
                    }
                };
                /*
                String var = depth.createCode(sym.getId().getText(), listCode, listIndents);
                indents.add(3);
                */
                var = depth.accept(new DepthConvertVisitor(sym.getId().unwrap().getText(), 3));
            }
            indents.add(3);
            // code.add(BOUND_VARIABLES+".put(\""+sym.getId().unwrap().getText()+"\""+", "+var+");");
            code.add(String.format("%s.put(\"%s\",%s);", BOUND_VARIABLES, sym.getId().unwrap().getText(), var));
        }
        listCode.addAll(code);
        indents.addAll(0,listIndents);
        return listCode;
    }

    private static String getFortressList(String id, List<String> code, List<Integer> indents) {
        String converter = "com.sun.fortress.syntax_abstractions.util.ActionRuntime.makeListAST";
        String astName = FreshName.getFreshName("ast");
        indents.add(3);
        code.add(String.format("Expr %s = %s(%s);",astName, converter, id));
        return astName;

    }

    public static Option<String> FortressWrapper(Id id, SyntaxDeclEnv syntaxDeclEnv, List<String> listCode, List<String> code, List<Integer> indents) {
        if (syntaxDeclEnv.contains(id)) {
            if (syntaxDeclEnv.isRepeat(id)) {
                return Option.some(getFortressList(id.getText(), listCode, indents));  
            }
            if (syntaxDeclEnv.isOption(id)) {
                // return Option.some(getFortressMaybe(id.getText(), code, indents, syntaxDeclEnv));  
                return Option.some(getFortressMaybe(id.getText(), code, indents));  
            }
            if (syntaxDeclEnv.isCharacterClass(id)) {
                return Option.some(getFortressCharacterClass(id.getText(), code, indents));  
            }
            if (syntaxDeclEnv.isAnyChar(id)) {
                return Option.some(getFortressAnyChar(id.getText(), code, indents));  
            }
        }
        return Option.none();
    }


    private static String getFortressMaybe(String id, List<String> code, List<Integer> indents ) {
        String converter = "com.sun.fortress.syntax_abstractions.util.ActionRuntime.makeMaybeAST";
        String astName = FreshName.getFreshName("ast");
        indents.add(4);
        code.add("Expr "+astName+" = "+converter+"("+id+")");
        return astName;

    }

    private static String getFortressCharacterClass(String id, List<String> code, List<Integer> indents) {
        String name = FreshName.getFreshName("characterClass");
        indents.add(3);
        code.add("StringLiteralExpr "+name+" = new StringLiteralExpr(\"\"+"+id+");");
        return name;
    }

    private static String getFortressAnyChar(String id, List<String> code,
            List<Integer> indents) {
        String name = FreshName.getFreshName("anyCharacter");
        indents.add(3);
        code.add("StringLiteralExpr "+name+" = new StringLiteralExpr(\"\"+"+id+");");
        return name;
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
