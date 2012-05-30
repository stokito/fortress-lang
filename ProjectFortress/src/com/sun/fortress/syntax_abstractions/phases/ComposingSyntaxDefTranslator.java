/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.syntax_abstractions.phases;

import com.sun.fortress.exceptions.MacroError;
import com.sun.fortress.nodes.*;
import static com.sun.fortress.syntax_abstractions.ParserMaker.Mangler;
import com.sun.fortress.syntax_abstractions.environments.Depth;
import com.sun.fortress.syntax_abstractions.environments.Depth.BaseDepth;
import com.sun.fortress.syntax_abstractions.environments.Depth.ListDepth;
import com.sun.fortress.syntax_abstractions.environments.Depth.OptionDepth;
import com.sun.fortress.syntax_abstractions.environments.EnvFactory;
import com.sun.fortress.syntax_abstractions.environments.GapEnv;
import com.sun.fortress.syntax_abstractions.environments.NTEnv;
import com.sun.fortress.syntax_abstractions.rats.RatsUtil;
import com.sun.fortress.useful.Debug;
import xtc.parser.*;
import xtc.parser.Binding;

import java.util.*;

/* converts fortress syntax definitions into Rats! definitions
 * using Rats! objects
 */
public class ComposingSyntaxDefTranslator {

    private static final String BOUND_VARIABLES = "boundVariables";

    @SuppressWarnings ("unused")
    //private Id nt;
    private Mangler mangler;
    private String type;
    private NTEnv ntEnv;

    public ComposingSyntaxDefTranslator(Mangler mangler, Id nt, String type, NTEnv ntEnv) {
        this.mangler = mangler;
        //this.nt = nt;
        this.type = type;
        this.ntEnv = ntEnv;
    }

    public List<Sequence> visitSyntaxDefs(Iterable<SyntaxDef> syntaxDefs) {
        List<Sequence> sequence = new LinkedList<Sequence>();
        for (SyntaxDef syntaxDef : syntaxDefs) {
            sequence.add(visitSyntaxDef(syntaxDef));
        }
        return sequence;
    }

    private Sequence visitSyntaxDef(SyntaxDef syntaxDef) {
        List<Element> elms = new ArrayList<Element>();
        GapEnv gapEnv = EnvFactory.makeGapEnv(syntaxDef, ntEnv);
        SymbolTranslator symbolTranslator = new SymbolTranslator(gapEnv, elms);
        for (SyntaxSymbol sym : syntaxDef.getSyntaxSymbols()) {
            sym.accept(symbolTranslator);
        }
        Element action = createAction(syntaxDef.getTransformer(), type, gapEnv);
        elms.add(action);
        return new Sequence(elms);
    }

    private static Element createAction(TransformerDecl transformation, String type, GapEnv gapEnv) {
        List<String> code = new LinkedList<String>();
        List<Integer> indents = new LinkedList<Integer>();

        if (transformation instanceof NamedTransformerDef) {
            createVariableBinding(code, indents, gapEnv);
            NamedTransformerDef def = (NamedTransformerDef) transformation;
            String parameters = collectParameters(def, code, indents);
            String name = def.getName();
            /* rafkind 2/15/09 horrible hack just to get XmlUse.fss to work */
            if (type.equals("StringLiteralExpr")) {
                code.add(String.format(
                        "yyValue = new _SyntaxTransformation%s(\"bogus\", NodeFactory.makeExprInfo(createSpan(yyStart,yyCount)), %s, %s, \"%s\");",
                        type,
                        BOUND_VARIABLES,
                        parameters,
                        name));
            } else {
                code.add(String.format(
                        "yyValue = new _SyntaxTransformation%s(NodeFactory.makeExprInfo(createSpan(yyStart,yyCount)), %s, %s, \"%s\");",
                        type,
                        BOUND_VARIABLES,
                        parameters,
                        name));
            }
            indents.add(4);
        } else {
            throw new MacroError(
                    "Don't know what to do with " + transformation + " " + transformation.getClass().getName());
        }
        return new Action(code, indents);
    }

    private static String collectParameters(NamedTransformerDef def, List<String> code, List<Integer> indents) {
        String variable = RatsUtil.getFreshName("parameterList");
        code.add(String.format("java.util.List %s = new java.util.LinkedList<String>();", variable));
        indents.add(4);
        for (NonterminalParameter parameter : def.getParameters()) {
            code.add(String.format("%s.add( \"%s\" );", variable, parameter.getName().getText()));
            indents.add(4);
        }
        return variable;
    }

    private class SymbolTranslator extends NodeDepthFirstVisitor_void {
        GapEnv gapEnv;
        List<Element> elements;

        SymbolTranslator(GapEnv gapEnv, List<Element> elements) {
            this.gapEnv = gapEnv;
            this.elements = elements;
        }

        @Override
        public void forAnyCharacterSymbol(AnyCharacterSymbol that) {
            elements.add(new NonTerminal("_"));
        }

        @Override
        public void forNonterminalSymbol(NonterminalSymbol that) {
            Debug.debug(Debug.Type.SYNTAX, 3, "NonterminalSymbol contains: " + that.getNonterminal());
            String renamed = mangler.forReference(that.getNonterminal());
            elements.add(new NonTerminal(renamed));
        }

        @Override
        public void forKeywordSymbol(KeywordSymbol that) {
            elements.add(new xtc.parser.StringLiteral(that.getToken()));
        }

        @Override
        public void forTokenSymbol(TokenSymbol that) {
            elements.add(new xtc.parser.StringLiteral(that.getToken()));
        }

        @Override
        public void forWhitespaceSymbol(WhitespaceSymbol that) {
            elements.add(new NonTerminal("w"));
        }

        @Override
        public void forBreaklineSymbol(BreaklineSymbol that) {
            elements.add(new NonTerminal("br"));
        }

        @Override
        public void forBackspaceSymbol(BackspaceSymbol that) {
            elements.add(new xtc.parser.StringLiteral("\b"));
        }

        @Override
        public void forNewlineSymbol(NewlineSymbol that) {
            elements.add(new xtc.parser.StringLiteral("\n"));
        }

        @Override
        public void forCarriageReturnSymbol(CarriageReturnSymbol that) {
            elements.add(new xtc.parser.StringLiteral("\r"));
        }

        @Override
        public void forFormfeedSymbol(FormfeedSymbol that) {
            elements.add(new xtc.parser.StringLiteral("\f"));
        }

        @Override
        public void forTabSymbol(TabSymbol that) {
            elements.add(new xtc.parser.StringLiteral("\t"));
        }

        @Override
        public void forCharacterClassSymbol(CharacterClassSymbol thatCharacterClassSymbol) {
            List<CharRange> crs = new LinkedList<CharRange>();
            final String mess = "Incorrect escape rewrite: ";
            for (CharacterSymbol c : thatCharacterClassSymbol.getCharacters()) {
                // TODO: Error when begin < end
                CharRange cr = c.accept(new NodeDepthFirstVisitor<CharRange>() {
                    @Override
                    public CharRange forCharacterInterval(CharacterInterval that) {
                        if (that.getBeginSymbol().length() != 1) {
                            throw new MacroError(mess + that.getBeginSymbol());
                        }
                        if (that.getEndSymbol().length() != 1) {
                            throw new MacroError(mess + that.getEndSymbol());
                        }
                        return new CharRange(that.getBeginSymbol().charAt(0), that.getEndSymbol().charAt(0));
                    }

                    @Override
                    public CharRange forCharSymbol(CharSymbol that) {
                        if (that.getString().length() != 1) {
                            throw new MacroError(mess + that.getString());
                        }
                        return new CharRange(that.getString().charAt(0));
                    }
                });
                crs.add(cr);
            }
            elements.add(new CharClass(crs));
        }

        @Override
        public void forPrefixedSymbol(PrefixedSymbol that) {
            List<Element> sublist = new LinkedList<Element>();
            SymbolTranslator subTranslator = new SymbolTranslator(this.gapEnv, sublist);
            that.getSymbol().accept(subTranslator);
            if (sublist.size() == 1) {
                Element e = sublist.get(0);
                elements.add(new Binding(prefixJavaVariable(that.getId().getText()), e));
                return;
            } else if (sublist.isEmpty()) {
                throw new MacroError(
                        "Malformed variable binding, bound to nonsensible symbol: " + that.getId().getText() + " " +
                        that.getSymbol());
            } else {
                throw new MacroError("Malformed variable binding, bound to multiple symbols: " + that.getSymbol());
            }
        }

        @Override
        public void forGroupSymbol(GroupSymbol that) {
            super.forGroupSymbol(that);
        }

        @Override
        public void forOptionalSymbol(OptionalSymbol that) {
            that.getSymbol().accept(new ModifierTranslator(this, new OptionalModifier()));
        }

        @Override
        public void forRepeatOneOrMoreSymbol(RepeatOneOrMoreSymbol that) {
            that.getSymbol().accept(new ModifierTranslator(this, new RepeatModifier(true)));
        }

        @Override
        public void forRepeatSymbol(RepeatSymbol that) {
            that.getSymbol().accept(new ModifierTranslator(this, new RepeatModifier(false)));
        }

        @Override
        public void forAndPredicateSymbol(AndPredicateSymbol that) {
            List<Element> sublist = new LinkedList<Element>();
            SymbolTranslator subTranslator = new SymbolTranslator(this.gapEnv, sublist);
            that.getSymbol().accept(subTranslator);
            if (sublist.isEmpty()) {
                throw new MacroError("Malformed AND predicate symbol, not bound to any symbol: ");
            } else if (sublist.size() == 1) {
                elements.add(new FollowedBy(new Sequence(sublist.get(0))));
                return;
            } else if (sublist.size() == 2 && sublist.get(1) instanceof Action) {
                /* FIXME: Hack! When the element was a group we know the second thing
                 * in the sequence was an action. Is there a better way to know?
                 */
                elements.add(new FollowedBy(new Sequence(sublist.get(0))));
                return;
            } else {
                throw new MacroError("Malformed AND predicate symbol, bound to multiple symbols: " + that.getSymbol());
            }
        }

        @Override
        public void forNotPredicateSymbol(NotPredicateSymbol that) {
            List<Element> sublist = new LinkedList<Element>();
            SymbolTranslator subTranslator = new SymbolTranslator(this.gapEnv, sublist);
            that.getSymbol().accept(subTranslator);
            if (sublist.isEmpty()) {
                throw new MacroError("Malformed NOT predicate symbol, not bound to any symbol: ");
            } else if (sublist.size() == 1) {
                elements.add(new NotFollowedBy(new Sequence(sublist.get(0))));
                return;
            } else if (sublist.size() == 2 && sublist.get(1) instanceof Action) {
                elements.add(new NotFollowedBy(new Sequence(sublist.get(0))));
                return;
            } else {
                throw new MacroError("Malformed NOT predicate symbol, bound to multiple symbols: " + that.getSymbol());
            }
        }

        @Override
        public void defaultCase(com.sun.fortress.nodes.Node that) {
            return;
        }
    }

    /**
     * Translate an atom with a modifier( +, ?, + ) to a Rats! production.
     */
    private class ModifierTranslator extends NodeDepthFirstVisitor_void {
        SymbolTranslator inner;
        Modifier modifier;

        ModifierTranslator(SymbolTranslator inner, Modifier modifier) {
            this.inner = inner;
            this.modifier = modifier;
        }

        /**
         * Just a plain element with a modifier attached.
         * Return the same element wrapped with the modifier.
         */
        @Override
        public void defaultCase(Node that) {
            List<Element> result = new ArrayList<Element>();
            that.accept(new SymbolTranslator(inner.gapEnv, result));
            if (result.size() == 1) {
                inner.elements.add(modifier.makePack(result.get(0)));
                return;
            } else {
                throw new MacroError(String.format("Malformed %s symbol while scanning %s, %s",
                                                   modifier.getName(),
                                                   that.getClass().getName(),
                                                   (result.isEmpty() ?
                                                    "not bound to any symbol" :
                                                    ("bound to multiple symbols: " + result))));
            }
        }

        @Override
        public void forNonterminalSymbol(NonterminalSymbol that) {
            defaultCase(that);
        }

        @Override
        public void forPrefixedSymbol(PrefixedSymbol that) {
            defaultCase(that);
        }

        /**
         * Extract the pattern variables from the group and bind them.
         */
        @Override
        public void forGroupSymbol(GroupSymbol that) {
            Map<Id, Depth> varMap = new HashMap<Id, Depth>();
            that.accept(new VariableCollector(varMap));

            List<Id> varIds = new ArrayList<Id>(varMap.keySet());
            String freshName = RatsUtil.getFreshName("g");
            List<Element> all = new ArrayList<Element>();
            for (SyntaxSymbol syms : that.getSymbols()) {
                syms.accept(new SymbolTranslator(inner.gapEnv, all));
            }

            List<Integer> indents = new LinkedList<Integer>();
            List<String> code = new LinkedList<String>();
            StringBuilder variables = new StringBuilder();
            for (Id varId : varIds) {
                String name = varId.getText();
                variables.append(prefixJavaVariable(name)).append(",");
            }
            code.add(String.format("yyValue = new Object[] { %s };", variables.toString()));
            indents.add(1);

            all.add(new Action(code, indents));
            Element pack = new Binding(freshName, modifier.makePack(new Sequence(all)));

            List<Integer> indents2 = new LinkedList<Integer>();
            List<String> code2 = new LinkedList<String>();
            String packedName = RatsUtil.getFreshName("packed");
            indents2.add(1);
            code2.add(modifier.preUnpack(packedName, freshName));
            int varCount = varIds.size();
            for (int index = 0; index < varCount; index++) {
                Id varId = varIds.get(index);
                String varName = prefixJavaVariable(varId.getText());
                String baseFortressType = inner.gapEnv.getJavaType(varId);
                String fullType = varMap.get(varId).getType(baseFortressType);
                indents2.add(1);
                code2.add(modifier.unpackDecl(fullType, varName, packedName, index));
            }
            Element unpack = new Action(code2, indents2);
            inner.elements.add(pack);
            inner.elements.add(unpack);
        }
    }

    private static interface Modifier {
        public String getName();

        public Element makePack(Element e);

        public String preUnpack(String packedName, String rawName);

        public String unpackDecl(String fullType, String varName, String packedName, int index);
    }

    private static class RepeatModifier implements Modifier {
        boolean isPlus;

        RepeatModifier(boolean isPlus) {
            this.isPlus = isPlus;
        }

        public String getName() {
            return isPlus ? "repeat" : "repeat-one-or-more";
        }

        public Element makePack(Element e) {
            return new xtc.parser.Repetition(isPlus, e);
        }

        public String preUnpack(String packedName, String rawName) {
            return String.format(
                    "List<Object[]> %s = com.sun.fortress.syntax_abstractions.util.ArrayUnpacker.convertPackedList(%s);",
                    packedName,
                    rawName);
        }

        public String unpackDecl(String fullType, String varName, String packedName, int index) {
            return String.format(
                    "List<%s> %s = com.sun.fortress.syntax_abstractions.util.ArrayUnpacker.<%s>unpack(%s, %d);",
                    fullType,
                    varName,
                    fullType,
                    packedName,
                    index);
        }
    }

    private static class OptionalModifier implements Modifier {
        public String getName() {
            return "optional";
        }

        public Element makePack(Element e) {
            return new xtc.parser.Option(e);
        }

        public String preUnpack(String packedName, String rawName) {
            return String.format("Object[] %s = (Object[]) %s;", packedName, rawName);
        }

        /**
         * If the group did not occur then the packedName variable
         * will be null.  Set the pattern variable to null if the
         * packedName is null otherwise set it to the index'th object.
         */
        public String unpackDecl(String fullType, String varName, String packedName, int index) {
            return String.format("%s %s = (%s)(%s == null ? null : %s[%d]);",
                                 fullType,
                                 varName,
                                 fullType,
                                 packedName,
                                 packedName,
                                 index);
        }
    }

    private static void createVariableBinding(final List<String> code,
                                              final List<Integer> indents,
                                              final GapEnv gapEnv) {
        indents.add(4);
        code.add("Map<String, Level> " + BOUND_VARIABLES + " = new HashMap<String, Level>();");

        final List<String> listCode = new LinkedList<String>();
        final List<Integer> listIndents = new LinkedList<Integer>();

        for (final Id id : gapEnv.gaps()) {
            Depth depth = gapEnv.getDepth(id);
            // Debug.debug(Debug.Type.SYNTAX, 3, "Depth for ", id, " is ", depth);
            String var = id.getText();

            class DepthConvertVisitor implements Depth.Visitor<String> {
                String source;
                int indent;

                DepthConvertVisitor(String source, int indent) {
                    this.source = source;
                    this.indent = indent;
                }

                public String forBaseDepth(BaseDepth d) {
                    if (gapEnv.hasJavaStringType(id)) {
                        return convertToStringLiteralExpr(source, code, indents);
                    } else {
                        return source;
                    }
                }

                public String forListDepth(ListDepth d) {
                    return source;
                    //throw new MacroError("not supported now");
                    /*
                    String fresh = FreshName.getFreshName("list");
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
                    */
                }

                public String forOptionDepth(OptionDepth d) {
                    throw new MacroError("not supported now");
                    /*
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
                    return fresh;
                    */
                }

                @SuppressWarnings ("unused")
                private void addCodeLine(String line) {
                    listIndents.add(indent);
                    listCode.add(line);
                }
            }
            ;
            String resultVar = depth.accept(new DepthConvertVisitor(var, 4));
            int levelDepth = depth.accept(new Depth.Visitor<Integer>() {
                public Integer forOptionDepth(OptionDepth d) {
                    return 1 + d.getParent().accept(this);
                }

                public Integer forListDepth(ListDepth d) {
                    return 1 + d.getParent().accept(this);
                }

                public Integer forBaseDepth(BaseDepth d) {
                    return 0;
                }
            });

            indents.add(4);
            code.add(String.format("%s.put(\"%s\", new Level(%d, %s));",
                                   BOUND_VARIABLES,
                                   var,
                                   levelDepth,
                                   prefixJavaVariable(resultVar)));
        }
    }

    /* Prefix a variable with some identifier so that names chosen by the user
     * don't clash with generated bindings nor do they conflict with java keywords
     * such as 'do' and 'for'.
     * One restriction is the prefix cannot start with _ because that is the "any char"
     * character in Rats!.
     */
    private static String prefixJavaVariable(String s) {
        return "fortress_" + s;
    }

    private static String convertToStringLiteralExpr(String id, List<String> code, List<Integer> indents) {
        String name = RatsUtil.getFreshName("stringLiteral");
        indents.add(4);
        code.add("StringLiteralExpr " + prefixJavaVariable(name) +
                 " = ExprFactory.makeStringLiteralExpr(NodeFactory.makeSpan(\"blame ComposingSyntaxDefTranslater\"), \"\"+" +
                 prefixJavaVariable(id) + ");");
        return name;
    }
}
