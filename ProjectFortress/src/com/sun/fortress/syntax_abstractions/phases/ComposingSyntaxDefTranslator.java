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

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;
import java.util.ArrayList;

import xtc.tree.Comment;
import xtc.parser.Action;
import xtc.parser.Binding;
import xtc.parser.CharClass;
import xtc.parser.CharRange;
import xtc.parser.Element;
import xtc.parser.FollowedBy;
import xtc.parser.Module;
import xtc.parser.NonTerminal;
import xtc.parser.NotFollowedBy;
import xtc.parser.Sequence;
import xtc.parser.SequenceName;
import xtc.parser.SequenceName;
import xtc.parser.ParserAction;

import com.sun.fortress.compiler.StaticPhaseResult;
import com.sun.fortress.compiler.index.NonterminalIndex;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.nodes.AbstractNode;
import com.sun.fortress.nodes.AndPredicateSymbol;
import com.sun.fortress.nodes.AnyCharacterSymbol;
import com.sun.fortress.nodes.BackspaceSymbol;
import com.sun.fortress.nodes.BreaklineSymbol;
import com.sun.fortress.nodes.CarriageReturnSymbol;
import com.sun.fortress.nodes.CharSymbol;
import com.sun.fortress.nodes.CharacterClassSymbol;
import com.sun.fortress.nodes.CharacterInterval;
import com.sun.fortress.nodes.CharacterSymbol;
import com.sun.fortress.nodes.FormfeedSymbol;
import com.sun.fortress.nodes.GrammarMemberDecl;
import com.sun.fortress.nodes.GroupSymbol;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.ItemSymbol;
import com.sun.fortress.nodes.KeywordSymbol;
import com.sun.fortress.nodes.NewlineSymbol;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeDepthFirstVisitor;
import com.sun.fortress.nodes.NonterminalDef;
import com.sun.fortress.nodes.NonterminalExtensionDef;
import com.sun.fortress.nodes.NonterminalSymbol;
import com.sun.fortress.nodes.NotPredicateSymbol;
import com.sun.fortress.nodes.OptionalSymbol;
import com.sun.fortress.nodes.PrefixedSymbol;
import com.sun.fortress.nodes.RepeatOneOrMoreSymbol;
import com.sun.fortress.nodes.RepeatSymbol;
import com.sun.fortress.nodes.SimpleTransformerDef;
import com.sun.fortress.nodes.SyntaxDef;
import com.sun.fortress.nodes.SyntaxSymbol;
import com.sun.fortress.nodes.TabSymbol;
import com.sun.fortress.nodes.TokenSymbol;
import com.sun.fortress.nodes.TransformerDecl;
import com.sun.fortress.nodes.TransformerDef;
import com.sun.fortress.nodes.TransformerNode;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.WhitespaceSymbol;
import com.sun.fortress.nodes._TerminalDef;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.parser_util.FortressUtil;
import com.sun.fortress.syntax_abstractions.environments.ComposingSyntaxDeclEnv;
import com.sun.fortress.syntax_abstractions.rats.util.FreshName;
import com.sun.fortress.syntax_abstractions.util.ActionCreater;
import com.sun.fortress.syntax_abstractions.util.FortressTypeToJavaType;
import com.sun.fortress.syntax_abstractions.util.JavaAstPrettyPrinter;
import com.sun.fortress.syntax_abstractions.util.SyntaxAbstractionUtil;
import com.sun.fortress.useful.Debug;

import static com.sun.fortress.syntax_abstractions.ComposingMacroCompiler.Mangler;

import edu.rice.cs.plt.tuple.Option;

public class ComposingSyntaxDefTranslator {

    private static final String BOUND_VARIABLES = "boundVariables";

    private Mangler mangler;
    private String name;
    private String type;
    private Map<String, BaseType> ntTypes;

    public ComposingSyntaxDefTranslator(Mangler mangler, String name, String type,
                                        Map<String, BaseType> ntTypes) {
        this.mangler = mangler;
        this.name = name;
        this.type = type;
        this.ntTypes = ntTypes;
    }

    public List<Sequence> visitSyntaxDefs(Iterable<SyntaxDef> syntaxDefs) {
        List<Sequence> sequence = new LinkedList<Sequence>();
        for (SyntaxDef syntaxDef: syntaxDefs) {
	    sequence.add(visitSyntaxDef(syntaxDef));
        }
        return sequence;
    }

    private Sequence visitSyntaxDef(SyntaxDef syntaxDef) {
        List<Element> elms = new LinkedList<Element>();
        ComposingSyntaxDeclEnv syntaxDeclEnv = new ComposingSyntaxDeclEnv(syntaxDef);
        for (SyntaxSymbol sym: syntaxDef.getSyntaxSymbols()) {
            elms.addAll(sym.accept(new SymbolTranslator(syntaxDeclEnv)));
        }
        Element action = 
            createAction(syntaxDef.getTransformer(), type, syntaxDeclEnv,
                         syntaxDef.accept(new VariableCollector()));
        elms.add(action);
        return new Sequence(elms);
    }

    public static Element createAction(TransformerDecl transformation,
                                       String type,
                                       ComposingSyntaxDeclEnv syntaxDeclEnv,
                                       Map<PrefixedSymbol,VariableCollector.Depth> variables) {
        List<String> code = new LinkedList<String>();
        List<Integer> indents = new LinkedList<Integer>();

        if (transformation instanceof TransformerDef) {
            createVariableBinding(code, indents, syntaxDeclEnv, variables);
            code.add(String.format("yyValue = new _SyntaxTransformation%s(createSpan(yyStart,yyCount), \"%s\", %s);", 
                                   type,
                                   ((TransformerDef) transformation).getTransformer(), 
                                   BOUND_VARIABLES ));
            indents.add(3);
        } else if ( transformation instanceof SimpleTransformerDef ){
            createVariableBinding(code, indents, syntaxDeclEnv, variables);
            AbstractNode n = ((SimpleTransformerDef) transformation).getNode();
            // FIXME: hack: We know it's a StringLiteralExpr, so the env isn't needed
            // should convert to new env
            JavaAstPrettyPrinter jpp = new JavaAstPrettyPrinter(null);
            String yyValue = n.accept(jpp);
            for (String s: jpp.getCode()) {
                code.add(s);
                indents.add(3);
            }
            code.add(String.format( "yyValue = %s;", yyValue ));
            indents.add(3);
        } else if ( transformation instanceof TransformerNode ){
            createVariableBinding(code, indents, syntaxDeclEnv, variables);
            code.add(String.format("yyValue = new _SyntaxTransformation%s(createSpan(yyStart,yyCount), \"%s\", %s);", 
                                   type,
                                   ((TransformerNode) transformation).getTransformer(), 
                                   BOUND_VARIABLES ));
            indents.add(3);
        } else {
            throw new RuntimeException( "Don't know what to do with " + transformation + " " + transformation.getClass().getName() );
        }
        return new Action(code, indents);
    }


    private static List<Element> mkList(Element... e) {
        return Arrays.asList(e);
    }

    private class SymbolTranslator extends NodeDepthFirstVisitor<List<Element>> {
        ComposingSyntaxDeclEnv syntaxDeclEnv;

        SymbolTranslator(ComposingSyntaxDeclEnv syntaxDeclEnv) {
            this.syntaxDeclEnv = syntaxDeclEnv;
        }

        @Override
        public List<Element> forAnyCharacterSymbol(AnyCharacterSymbol that) {
            return mkList(new NonTerminal("_"));
        }

        @Override
        public List<Element> forNonterminalSymbol(NonterminalSymbol that) {
            Debug.debug( Debug.Type.SYNTAX, 3,
                         "NonterminalSymbol contains: " + that.getNonterminal());
            String renamed = mangler.forReference(NodeUtil.nameString(that.getNonterminal()));
            return mkList(new NonTerminal(renamed));
        }

        @Override
        public List<Element> forKeywordSymbol(KeywordSymbol that) {
            return mkList(new xtc.parser.StringLiteral(that.getToken()));
        }

        @Override
        public List<Element> forTokenSymbol(TokenSymbol that) {
            return mkList(new xtc.parser.StringLiteral(that.getToken()));
        }

        @Override
        public List<Element> forWhitespaceSymbol(WhitespaceSymbol that) {
            return mkList(new NonTerminal("w"));
        }

        @Override
        public List<Element> forBreaklineSymbol(BreaklineSymbol that) {
            return mkList(new NonTerminal("br"));
        }

        @Override
        public List<Element> forBackspaceSymbol(BackspaceSymbol that) {
            return mkList(new xtc.parser.StringLiteral("\b"));
        }

        @Override
        public List<Element> forNewlineSymbol(NewlineSymbol that) {
            return mkList(new xtc.parser.StringLiteral("\n"));
        }

        @Override
        public List<Element> forCarriageReturnSymbol(CarriageReturnSymbol that) {
            return mkList(new xtc.parser.StringLiteral("\r"));
        }

        @Override
        public List<Element> forFormfeedSymbol(FormfeedSymbol that) {
            return mkList(new xtc.parser.StringLiteral("\f"));
        }

        @Override
        public List<Element> forTabSymbol(TabSymbol that) {
            return mkList(new xtc.parser.StringLiteral("\t"));
        }

        @Override
        public List<Element> forCharacterClassSymbol(CharacterClassSymbol that) {
            List<CharRange> crs = new LinkedList<CharRange>();
            final String mess = "Incorrect escape rewrite: ";
            for (CharacterSymbol c: that.getCharacters()) {
                // TODO: Error when begin < end
                CharRange cr = c.accept(new NodeDepthFirstVisitor<CharRange>() {
                        @Override
                        public CharRange forCharacterInterval(CharacterInterval that) {
                            if (that.getBegin().length() != 1) {
                                new RuntimeException(mess +that.getBegin());
                            }
                            if (that.getEnd().length() != 1) {
                                new RuntimeException(mess+that.getEnd());
                            }
                            return new CharRange(that.getBegin().charAt(0), that.getEnd().charAt(0));
                        }

                        @Override
                        public CharRange forCharSymbol(CharSymbol that) {
                            if (that.getString().length() != 1) {
                                new RuntimeException(mess+that.getString());
                            }
                            return new CharRange(that.getString().charAt(0));
                        }
                    });
                crs.add(cr);
            }
            return mkList(new CharClass(crs));
        }

        @Override
        public List<Element> forPrefixedSymbolOnly(PrefixedSymbol that, 
                                                   Option<List<Element>> id_result, 
                                                   Option<List<Element>> type_result,
                                                   List<Element> symbol_result) {
            Debug.debug( Debug.Type.SYNTAX, 3, "Prefixed symbol has ", symbol_result.size() );
            if (symbol_result.size() == 1) {
                Element e = symbol_result.get(0);
                assert(that.getId().isSome());
                return mkList(new Binding(that.getId().unwrap().getText(), e));
            }
            if (symbol_result.isEmpty()) {
                if (that.getId().isSome()) {
                    throw new RuntimeException("Malformed variable binding, bound to nonsensible symbol: "+that.getId().unwrap().getText() + " "+that.getSymbol());
                }
                throw new RuntimeException("Malformed variable binding, bound to nonsensible symbol, no identifier: "+that.getSymbol());
            }
            throw new RuntimeException("Malformed variable binding, bound to multiple symbols: "+symbol_result);
        }

        @Override
        public List<Element> forGroupSymbol(GroupSymbol that){
            List<Element> all = new LinkedList<Element>();
            for ( SyntaxSymbol syms : that.getSymbols() ){
                all.addAll( syms.accept(this) );
            }
            return all;
        }

        @Override
        public List<Element> forOptionalSymbol(OptionalSymbol that) {
            return that.getSymbol().accept(new ModifierTranslator(this, new OptionalModifier()));
        }

        @Override
        public List<Element> forRepeatOneOrMoreSymbol(RepeatOneOrMoreSymbol that) {
            return that.getSymbol().accept(new ModifierTranslator(this, new RepeatModifier(true)));
        }

        @Override
        public List<Element> forRepeatSymbol(RepeatSymbol that) {
            return that.getSymbol().accept(new ModifierTranslator(this, new RepeatModifier(false)));
        }

        @Override
        public List<Element> forAndPredicateSymbolOnly(AndPredicateSymbol that,
                                                       List<Element> symbol_result) {
            if (symbol_result.isEmpty()) {
                throw new RuntimeException("Malformed AND predicate symbol, not bound to any symbol: ");
            }
            if (symbol_result.size() == 1) {
                Element e = symbol_result.get(0);
                return mkList(new FollowedBy(new Sequence(e)));
            }
            /* FIXME: Hack! When the element was a group we know the second thing
             * in the sequence was an action. Is there a better way to know?
             */
            if (symbol_result.size() == 2 && symbol_result.get(1) instanceof Action ){
                Element e = symbol_result.get(0);
                return mkList(new FollowedBy(new Sequence(e)));
            }
            throw new RuntimeException("Malformed AND predicate symbol, bound to multiple symbols: "+symbol_result);
        }

        @Override
        public List<Element> forNotPredicateSymbolOnly(NotPredicateSymbol that,
                                                       List<Element> symbol_result) {
            if (symbol_result.isEmpty()) {
                throw new RuntimeException("Malformed NOT predicate symbol, not bound to any symbol: ");
            }
            if (symbol_result.size() == 1) {
                Element e = symbol_result.get(0);
                return mkList(new NotFollowedBy(new Sequence(e)));
            }
            if (symbol_result.size() == 2 && symbol_result.get(1) instanceof Action ){
                Element e = symbol_result.get(0);
                return mkList(new NotFollowedBy(new Sequence(e)));
            }
            throw new RuntimeException("Malformed NOT predicate symbol, bound to multiple symbols: "+symbol_result);
        }

        @Override
        public List<Element> defaultCase(com.sun.fortress.nodes.Node that) {
            return new LinkedList<Element>();
        }

    }

    /**
     * Translate an atom with a modifier( +, ?, + ) to a rats! production.
     */
    private static class ModifierTranslator extends NodeDepthFirstVisitor<List<Element>> {

        private SymbolTranslator inner;
        private Modifier modifier;

        ModifierTranslator(SymbolTranslator inner, Modifier modifier) {
            this.inner = inner;
            this.modifier = modifier;
        }

        /**
         * Just a plain element with a modifier attached. Return the same element wrapped with
         * the modifier.
         */
        @Override
        public List<Element> defaultCase(Node that) {
            List<Element> result = that.accept(inner);
            if (result.size() == 1) {
                Element e = result.get(0);
                return mkList(modifier.makePack(e));
            } else {
                throw new RuntimeException
                    (String.format("Malformed %s symbol while scanning %s, %s",
                                   modifier.getName(),
                                   that.getClass().getName(),
                                   (result.isEmpty()
                                    ? "not bound to any symbol"
                                    : ("bound to multiple symbols: " + result))));
            }
        }

        @Override
        public List<Element> forNonterminalSymbol(NonterminalSymbol that){
            return defaultCase(that);
        }

        @Override
        public List<Element> forPrefixedSymbol(PrefixedSymbol that){
            return defaultCase(that);
        }

        private String lookupAstType( Id variable ){
            // FIXME!!!
            return "Object";

            /*
            // return SyntaxAbstractionUtil.getJavaTypeOld(nonterminal);
            Debug.debug( Debug.Type.SYNTAX, 4, "Looking up ast type for ", variable );
            return SyntaxAbstractionUtil.getJavaType(inner.getEnv(), variable);
            */
        }

        /**
         * Extract the pattern variables from the group and bind them.
         */
        @Override
        public List<Element> forGroupSymbol(GroupSymbol that){
            Map<PrefixedSymbol,VariableCollector.Depth> varMap = that.accept(new VariableCollector());
            List<PrefixedSymbol> varSyms = new ArrayList<PrefixedSymbol>(varMap.keySet());
            String freshName = FreshName.getFreshName("g");
            List<Element> all = new LinkedList<Element>();
            for (SyntaxSymbol syms : that.getSymbols()) {
                all.addAll( syms.accept(inner) );
            }

            List<Integer> indents = new LinkedList<Integer>();
            List<String> code = new LinkedList<String>();
            StringBuilder variables = new StringBuilder();
            for (PrefixedSymbol sym : varSyms) {
                variables.append( sym.getId().unwrap().toString() ).append( "," );
            }
            code.add(String.format("yyValue = new Object[] { %s };", variables.toString()));
            indents.add(1);
                
            all.add(new Action(code, indents));
            Element pack = new Binding(freshName, modifier.makePack(new Sequence(all)));

            List<Integer> indents2 = new LinkedList<Integer>();
            List<String> code2 = new LinkedList<String>();
            String packedName = FreshName.getFreshName("packed");
            indents2.add(1);
            code2.add(modifier.preUnpack(packedName, freshName));
            int varCount = varSyms.size();
            for (int index = 0; index < varCount ; index++) {
                PrefixedSymbol sym = varSyms.get(index);
                Id varId = sym.getId().unwrap();
                String varName = varId.toString();
                Id ntName = inner.syntaxDeclEnv.getNonterminalOfVar(varId);
                String baseFortressType = lookupAstType(varId);
                String fullType = varMap.get(sym).getType(baseFortressType); 
                indents2.add(1);
                code2.add(modifier.unpackDecl(fullType, varName, packedName, index));
            }
            Element unpack = new Action(code2, indents2);

            return mkList(pack, unpack);
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
            return isPlus? "repeat" : "repeat-one-or-more";
        }
        public Element makePack(Element e) {
            return new xtc.parser.Repetition(isPlus, e);
        }
        public String preUnpack(String packedName, String rawName) {
            return String.format("List<Object[]> %s = com.sun.fortress.syntax_abstractions.util.ArrayUnpacker.convertPackedList(%s);",
                                 packedName, rawName);
        }
        public String unpackDecl(String fullType, String varName, String packedName, int index) {
            return String.format("List<%s> %s = com.sun.fortress.syntax_abstractions.util.ArrayUnpacker.<%s>unpack(%s, %d);", 
                                 fullType, varName, fullType, packedName, index);
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
         * will be null. Set the pattern variable to null if the
         * packedName is null otherwise set it to the index'th object.
         */
        public String unpackDecl(String fullType, String varName, String packedName, int index) {
            return String.format("%s %s = (%s)(%s == null ? null : %s[%d]);", 
                                 fullType, varName, fullType, 
                                 packedName, packedName, index);
        }
    }





    public static void createVariableBinding(final List<String> code,
                                             final List<Integer> indents,
                                             final ComposingSyntaxDeclEnv syntaxDeclEnv, 
                                             Map<PrefixedSymbol,VariableCollector.Depth> variables) {
        indents.add(3);
        code.add("Map<String, Object> "+BOUND_VARIABLES+" = new HashMap<String, Object>();");

        final List<String> listCode = new LinkedList<String>();
        final List<Integer> listIndents = new LinkedList<Integer>();

        for ( Map.Entry<PrefixedSymbol,VariableCollector.Depth> pair : variables.entrySet() ){

            final PrefixedSymbol sym = pair.getKey();
            VariableCollector.Depth depth = pair.getValue();
            Debug.debug( Debug.Type.SYNTAX, 3, "Depth for ", sym, " is ", depth );

            String var = sym.getId().unwrap().getText();
            // String ntOfVar = syntaxDeclEnv.getNonterminalOfVar(sym.getId().unwrap());
            // String typeOfVar = ntTypes.get(ntOfVar);

            class DepthConvertVisitor implements VariableCollector.DepthVisitor<String> {
                String source;
                int indent;
                DepthConvertVisitor(String source, int indent) {
                    this.source = source;
                    this.indent = indent;
                }

                public String forBaseDepth(VariableCollector.Depth d) {
                    Id id = sym.getId().unwrap();
                    if (syntaxDeclEnv.hasJavaStringType(id)) {
                        return convertToStringLiteralExpr(source, code, indents);
                    } else {
                        return source;
                    }
                }
                public String forListDepth(VariableCollector.Depth d) {
                    throw new RuntimeException("not supported now");
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
                public String forOptionDepth(VariableCollector.Depth d) {
                    throw new RuntimeException("not supported now");
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
                private void addCodeLine(String line){
                    listIndents.add(indent);
                    listCode.add(line);
                }
            };
            String resultVar = depth.accept(new DepthConvertVisitor(var, 3));

            indents.add(3);
            code.add(String.format("%s.put(\"%s\",%s);", BOUND_VARIABLES, var, resultVar));
        }
    }

    private static String convertToStringLiteralExpr(String id, List<String> code, List<Integer> indents) {
        String name = FreshName.getFreshName("stringLiteral");
        indents.add(3);
        code.add("StringLiteralExpr "+name+" = new StringLiteralExpr(\"\"+"+id+");");
        return name;
    }
}
