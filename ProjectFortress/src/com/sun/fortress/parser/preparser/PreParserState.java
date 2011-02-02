/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/
package com.sun.fortress.parser.preparser;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import xtc.util.State;
import edu.rice.cs.plt.tuple.Option;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdOrOp;
import com.sun.fortress.nodes.Op;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.parser_util.precedence_resolver.PrecedenceMap;
import com.sun.fortress.useful.Debug;

/**
 * PreParser state for checking delimiter matching.  Note that
 * this class supports only a single state-modifying transaction.
 * In other words, calls to {@link #start()}, {@link #commit()},
 * and {@link #abort()} cannot be nested within each other.
 */
public class PreParserState implements State {

    /** The list of left delimiters. */
    private List<IdOrOp> lefts;

    /** The beginning keywords matching with "end". */
    private List<String> keywords;

    /** The file to report mismatched delimiters. */
    private BufferedWriter writer;

    /** Nested component/API definitions are not allowed. */
    private boolean sawCompilation = false;

    /** Operator declarations are not allowed in block expressions. */
    private boolean inBlock = false;
    private boolean sawFn = false;

    /** Region annotation by "at" should be followed by "do". */
    private Option<Span> sawAt = Option.<Span>none();

    /** Create a new preparser state object. */
    public PreParserState() {
        reset("No file yet!");
    }

    /** Initialize the map of matching keywords. */
    public void init(BufferedWriter wr) {
        writer = wr;
        String[] ends = new String[]{"api", "component", "trait", "object", "grammar",
                                     "do", "case", "if", "try", "typecase"};
        keywords = new ArrayList<String>(java.util.Arrays.asList(ends));
    }

    public void reset(String file) {
        lefts = new ArrayList<IdOrOp>();
    }

    public void start() { }
    public void commit() { }
    public void abort() { }

    /** Record a left delimiter. */
    public void left(IdOrOp open) {
        Debug.debug( Debug.Type.PARSER, 1, "Left delimiter ", open );
        String token = open.getText();
        if ( token.equals("component") || token.equals("api") ) {
            if ( sawCompilation )
                log(NodeUtil.getSpan(open),
                    "Nested " + token + " definitions are not allowed.");
            sawCompilation = true;
        }
        if ( lefts.isEmpty() || !isQuote(lefts.get(0).getText()) )
            lefts.add(0, open);
    }

    public void left(Span span, String open) {
        left( NodeFactory.makeId(span, open) );
    }

    private void emptyLefts(IdOrOp close) {
        Debug.debug( Debug.Type.PARSER, 1,
                     "right: Unmatched delimiter \"" + close + "\"." );
        log(NodeUtil.getSpan(close),
            "Unmatched delimiter \"" + close + "\".");
    }

    /** Check a right delimiter. */
    public void right(IdOrOp close) {
        Debug.debug( Debug.Type.PARSER, 1, "Right delimiter ", close );
        if ( lefts.isEmpty() ) emptyLefts(close);
        else {
            IdOrOp open = lefts.remove(0);
            /* label Id /end Id -- covered by handleEnd
             * (if ... )        -- covered by matches
             * (if ... end)     -- covered by matches
             * (if ... end ...) -- here
             */
            if ( open.getText().equals("(if") && close.getText().equals("end") ) {
                lefts.add(0, NodeFactory.makeId(NodeUtil.getSpan(open), "("));
            } else if ( open.getText().startsWith("label$") ) {
                log(NodeUtil.spanTwo(open, close),
                    "Missing label at the end of a label expression.");
            } else if ( ! matches(open, close) ) {
                Debug.debug( Debug.Type.PARSER, 1,
                             "right: Unmatched delimiter \"" + close + "\"." );
                log(NodeUtil.spanTwo(NodeUtil.getSpan(open),
                                     NodeUtil.getSpan(close)),
                    "Unmatched delimiters \"" + open + "\" and \"" + close + "\".");
            }
            if ( open.getText().equals("(if") && close.getText().equals(")") )
                inBlock = false;
        }
    }

    public void right(Span span, String close) {
        right( NodeFactory.makeId(span, close) );
    }

    public void handleLabelEnd(Span span, Id id) {
        Debug.debug( Debug.Type.PARSER, 1, "HandleLabelEnd");
        Id close = NodeFactory.makeId(span, "end");
        if ( lefts.isEmpty() ) emptyLefts(close);
        else {
            IdOrOp open = lefts.remove(0);
            String openText = open.getText();
            if ( openText.startsWith("label$") ) {
                String openLabel = openText.substring(6);
                String closeLabel = id.getText();
                if ( ! openLabel.equals(closeLabel) )
                    log(NodeUtil.spanTwo(NodeUtil.getSpan(open), span),
                        "Opening label must match closing label.");
            } else {
                lefts.add(0, open);
                handleEnd(span);
            }
        }
    }

    public void handleDo(Span span) {
        Debug.debug( Debug.Type.PARSER, 1, "HandleDo");
        sawAt = Option.<Span>none();
        if ( ! lefts.isEmpty() && lefts.get(0).getText().equals("also") )
            right( span, "do" );
        else left( span, "do" );
    }

    public void handleAt(Span span) {
        Debug.debug( Debug.Type.PARSER, 1, "HandleAt");
        sawAt = Option.<Span>some(span);
    }

    public void handleOpr(Span span) {
        Debug.debug( Debug.Type.PARSER, 1, "HandleOpr");
        for ( IdOrOp left : lefts ) {
            if ( left.getText().equals("object") ) return;
            if ( left.getText().equals("do") ||
                 left.getText().startsWith("label$") ||
                 inBlock )
                log(span, "Operator declarations are not allowed in block expressions.");
        }
    }

    public void beginBlock() {
        inBlock = true;
    }

    public void handleFn() {
        sawFn = true;
    }

    // "=>" does not introduce a block for import aliases and fn expressions.
    public void handleDoubleArrow() {
        if ( sawFn ) sawFn = false;
        else if ( ! lefts.isEmpty() && ! lefts.get(0).getText().equals("{") )
            inBlock = true;
    }

    public void handleEnd(Span span) {
        if ( ! lefts.isEmpty() &&
             ! lefts.get(0).getText().equals("object") )
            inBlock = false;
        right(span, "end");
    }

    public void quote(Span span, String token) {
        Debug.debug( Debug.Type.PARSER, 1, "Quote ", token);
        if ( lefts.isEmpty() )
            left( NodeFactory.makeId(span, token) );
        else {
            if ( matches(lefts.get(0).getText(), token) )
                lefts.remove(0);
            else
                left( NodeFactory.makeId(span, token) );
        }
    }

    public void report() {
        if ( ! lefts.isEmpty() ) {
            for ( IdOrOp left : lefts ) {
                Debug.debug( Debug.Type.PARSER, 1,
                             "report: Unmatched delimiter \"" + left + "\"." );
                log(NodeUtil.getSpan(left),
                    "Unmatched delimiter \"" + left + "\".");
            }
        }
        if ( sawAt.isSome() )
            log(sawAt.unwrap(), "Unmatched delimiter \"at\".");
    }

    private void log(Span span, String message) {
        NodeUtil.log(writer, span, message);
    }

    private boolean isQuote(String token) {
        String[] all = new String[]{"\\\"", "\u201c"};
        List<String> quotes = new ArrayList<String>(java.util.Arrays.asList(all));
        return quotes.contains( token );
    }

    /** Check whether the given delimiters are matching delimiters. */
    private boolean matches(IdOrOp open, IdOrOp close) {
        String left = open.getText();
        String right = close.getText();
        if ( open  instanceof Id && close instanceof Id )
            return matches(left, right);
        else if ( open  instanceof Op && close instanceof Op )
            return PrecedenceMap.ONLY.matchedBrackets(left, right);
        else return false;
    }

    private boolean matches(String left, String right) {
        if ( keywords.contains(left) ) return right.equals("end");
        else if ( left.equals("also") ) return right.equals("do");
        else if ( left.equals("(if") ) return ( right.equals(")") || right.equals("end)") );
        else if ( left.equals("(") ) return right.equals(")");
        else if ( left.equals("[") ) return right.equals("]");
        else if ( left.equals("{") ) return right.equals("}");
        else if ( left.equals("[\\") || left.equals("\u27e6") )
            return ( right.equals("\\]") || right.equals("\u27e7") );
        else if ( left.equals("\\\"") || left.equals("\u201c") )
            return ( right.equals("\\\"") || right.equals("\u201d") );
        else if ( left.equals("`") || left.equals("'") || left.equals("\u2018") )
            return ( right.equals("'") || right.equals("\u2019") );
        else return false;
    }
}
