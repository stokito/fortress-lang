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

import java.util.LinkedList;
import java.util.List;

import com.sun.fortress.nodes.AnyCharacterSymbol;
import com.sun.fortress.nodes.CharSymbol;
import com.sun.fortress.nodes.CharacterClassSymbol;
import com.sun.fortress.nodes.CharacterInterval;
import com.sun.fortress.nodes.CharacterSymbol;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.KeywordSymbol;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeDepthFirstVisitor;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.PrefixedSymbol;
import com.sun.fortress.nodes.SyntaxSymbol;
import com.sun.fortress.nodes.ASTNodeInfo;
import com.sun.fortress.nodes.TokenSymbol;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.parser_util.SyntaxUtil;

/* EscapeRewriter
 * Replaces escape sequences in strings (see Syntax.rats : EscapedSpecialChars, EscapedSpecialSymbol)
 * - in KeywordSymbol, TokenSymbol, and prefixes
 */
public class EscapeRewriter extends NodeUpdateVisitor {

    private static final String ESCAPECHAR = "`";

    private String removeLeadingEscape(String s, List<CharacterSymbol> ls) {
        if (s.startsWith(ESCAPECHAR)) {
            int inx = 1;
            while(inx<s.length()-1) {
                ls.add(new CharSymbol(NodeFactory.makeSpanInfo(NodeFactory.makeSpan("impossible", ls)), ""+s.charAt(inx)));
                inx++;
            }
            s = ""+s.charAt(s.length()-1);
        }
        return s;
    }

    private String removeTrailingEscape(Span span, String s, List<CharacterSymbol> ls) {
        String end = "";
        if (s.startsWith(ESCAPECHAR)) {
            int inx = 1;
            end = ""+s.charAt(inx);
            inx++;
            while(inx<s.length()) {
                ls.add(new CharSymbol(NodeFactory.makeSpanInfo(span), ""+s.charAt(inx)));
                inx++;
            }
            return end;
        }
        return s;
    }

    @Override
    public Node forAnyCharacterSymbol(AnyCharacterSymbol that) {
        return new AnyCharacterSymbol(NodeFactory.makeSpanInfo(NodeUtil.getSpan(that)));
    }

    @Override
    public Node forCharacterClassSymbol(CharacterClassSymbol that) {
        List<CharacterSymbol> ls = new LinkedList<CharacterSymbol>();
        for (CharacterSymbol cs: that.getCharacters()) {
            List<CharacterSymbol> ncs = cs.accept(new NodeDepthFirstVisitor<List<CharacterSymbol>>() {

                @Override
                public List<CharacterSymbol> forCharacterInterval(CharacterInterval that) {
                    List<CharacterSymbol> head = new LinkedList<CharacterSymbol>();
                    String begin = removeLeadingEscape(that.getBeginSymbol(), head);
                    List<CharacterSymbol> tail = new LinkedList<CharacterSymbol>();
                    String end = removeTrailingEscape(NodeUtil.getSpan(that), that.getEndSymbol(), tail);
                    head.add(new CharacterInterval(NodeFactory.makeSpanInfo(NodeUtil.getSpan(that)), begin, end));
                    head.addAll(tail);
                    return head;
                }

                @Override
                public List<CharacterSymbol> forCharSymbol(CharSymbol that) {
                    List<CharacterSymbol> head = new LinkedList<CharacterSymbol>();
                    String s = removeLeadingEscape(that.getString(), head);
                    head.add(new CharSymbol(NodeFactory.makeSpanInfo(NodeUtil.getSpan(that)), s));
                    return head;
                }

            });
            ls.addAll(ncs);
        }
        return new CharacterClassSymbol(NodeFactory.makeSpanInfo(NodeUtil.getSpan(that)), ls);
    }

    @Override
    public Node forKeywordSymbol(KeywordSymbol that) {
        String s = removeEscape(that.getToken());
        return new KeywordSymbol(NodeFactory.makeSpanInfo(NodeUtil.getSpan(that)), s);
    }

    @Override
    public Node forTokenSymbol(TokenSymbol that) {
        String s = removeEscape(that.getToken());
        return new TokenSymbol(NodeFactory.makeSpanInfo(NodeUtil.getSpan(that)), s);
    }

    @Override
        public Node forPrefixedSymbolOnly(PrefixedSymbol that, ASTNodeInfo info,
                                          Id result_id,
                                          SyntaxSymbol result_symbol) {
        String s = removeEscape(result_id.getText());
        // TODO is span correct below?
        return new PrefixedSymbol(NodeFactory.makeSpanInfo(NodeUtil.getSpan(that)), NodeFactory.makeId(NodeUtil.getSpan(result_id), s), result_symbol);
    }

    private String removeEscape(String s) {
        for (String symbol: SyntaxUtil.specialSymbols()) {
            s = s.replaceAll(ESCAPECHAR+symbol, symbol);
        }
        for (String symbol: SyntaxUtil.specialChars()) {
            s = s.replaceAll(ESCAPECHAR+symbol, symbol);
        }

        return s;
    }


}
