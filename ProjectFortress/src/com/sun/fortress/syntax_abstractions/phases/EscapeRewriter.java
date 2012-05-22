/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.syntax_abstractions.phases;

import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.parser.Fortress;

import java.util.LinkedList;
import java.util.List;

/* EscapeRewriter
 * Replaces escape sequences in strings
 * (see Syntax.rats : EscapedSpecialChars, EscapedSpecialSymbol)
 * - in KeywordSymbol, TokenSymbol, and prefixes
 */
public class EscapeRewriter extends NodeUpdateVisitor {

    private static final String ESCAPECHAR = "`";

    private String removeLeadingEscape(String s, List<CharacterSymbol> ls) {
        if (s.startsWith(ESCAPECHAR)) {
            int inx = 1;
            while (inx < s.length() - 1) {
                ls.add(new CharSymbol(NodeFactory.makeSpanInfo(NodeFactory.makeSpan("impossible", ls)), "" + s.charAt(
                        inx)));
                inx++;
            }
            s = "" + s.charAt(s.length() - 1);
        }
        return s;
    }

    private String removeTrailingEscape(Span span, String s, List<CharacterSymbol> ls) {
        String end = "";
        if (s.startsWith(ESCAPECHAR)) {
            int inx = 1;
            end = "" + s.charAt(inx);
            inx++;
            while (inx < s.length()) {
                ls.add(new CharSymbol(NodeFactory.makeSpanInfo(span), "" + s.charAt(inx)));
                inx++;
            }
            return end;
        }
        return s;
    }

    @Override
    public Node forAnyCharacterSymbol(AnyCharacterSymbol that) {
        return that;
    }

    @Override
    public Node forCharacterClassSymbol(CharacterClassSymbol thatCharacterClassSymbol) {
        List<CharacterSymbol> ls = new LinkedList<CharacterSymbol>();
        for (CharacterSymbol cs : thatCharacterClassSymbol.getCharacters()) {
            List<CharacterSymbol> ncs = cs.accept(new NodeDepthFirstVisitor<List<CharacterSymbol>>() {

                @Override
                public List<CharacterSymbol> forCharacterInterval(CharacterInterval thatCharacterInterval) {
                    List<CharacterSymbol> head = new LinkedList<CharacterSymbol>();
                    String begin = removeLeadingEscape(thatCharacterInterval.getBeginSymbol(), head);
                    List<CharacterSymbol> tail = new LinkedList<CharacterSymbol>();
                    String end = removeTrailingEscape(NodeUtil.getSpan(thatCharacterInterval),
                                                      thatCharacterInterval.getEndSymbol(),
                                                      tail);
                    head.add(new CharacterInterval(thatCharacterInterval.getInfo(), begin, end));
                    head.addAll(tail);
                    return head;
                }

                @Override
                public List<CharacterSymbol> forCharSymbol(CharSymbol that) {
                    List<CharacterSymbol> head = new LinkedList<CharacterSymbol>();
                    String s = removeLeadingEscape(that.getString(), head);
                    head.add(new CharSymbol(that.getInfo(), s));
                    return head;
                }

            });
            ls.addAll(ncs);
        }
        return new CharacterClassSymbol(thatCharacterClassSymbol.getInfo(), ls);
    }

    @Override
    public Node forKeywordSymbol(KeywordSymbol that) {
        return new KeywordSymbol(that.getInfo(), removeEscape(that.getToken()));
    }

    @Override
    public Node forTokenSymbol(TokenSymbol that) {
        return new TokenSymbol(that.getInfo(), removeEscape(that.getToken()));
    }

    @Override
    public Node forPrefixedSymbolOnly(PrefixedSymbol that, ASTNodeInfo info, Id result_id, SyntaxSymbol result_symbol) {
        String s = removeEscape(result_id.getText());
        // TODO is span correct below?
        return new PrefixedSymbol(that.getInfo(), NodeFactory.makeId(NodeUtil.getSpan(result_id), s), result_symbol);
    }

    private String removeEscape(String s) {
        for (String symbol : Fortress.FORTRESS_SYNTAX_SPECIAL_SYMBOLS) {
            s = s.replaceAll(ESCAPECHAR + symbol, symbol);
        }
        for (String symbol : Fortress.FORTRESS_SYNTAX_SPECIAL_CHARS) {
            s = s.replaceAll(ESCAPECHAR + symbol, symbol);
        }
        return s;
    }

}
