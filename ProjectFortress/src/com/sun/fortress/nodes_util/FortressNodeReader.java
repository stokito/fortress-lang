/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/
package com.sun.fortress.nodes_util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.math.BigInteger;
import java.util.ArrayList;

import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeReader;

import static com.sun.fortress.exceptions.InterpreterBug.bug;

public class FortressNodeReader extends NodeReader {

    public FortressNodeReader(BufferedReader r) {
        super(r);
    }

    public static Node read(Reader r) throws IOException {
        return new FortressNodeReader(new BufferedReader(r)).readNode();
    }

    // read chunk starting at curr().
    // return chunk, leave curr at following delimiter.
    private String readSpanChunk() throws IOException {
        StringBuilder sb = new StringBuilder();
        char c = curr();
        while (c != ':' && c != '~' && !Character.isWhitespace(c)) {
            sb.append(c);
            c = next();
        }
        return sb.toString();
    }

    // Most of readSpan has been cribbed from Unprinter.
    private Span readSpan() throws IOException {
        StringBuilder full = new StringBuilder();
        full.append(" @");
        String fname = ""; // lastSpan.begin.getFileName();
        char c = next();
        if (c == '"') {
            full.append(c);
            fname = readString();
            full.append(fname);
            if (curr() != ':') throw error();
            c = next();
            full.append("\":");
        }

        String s = readSpanChunk();
        full.append(s);
        c = curr();
        full.append(c);
        int line = Integer.parseInt(s,10);

        if (c != ':') throw error();
        next();

        s = readSpanChunk();
        full.append(s);
        c = curr();
        full.append(c);
        int column = Integer.parseInt(s,10);

        SourceLoc beginning = new SourceLocRats(fname, line, column, 0);
        SourceLoc ending = beginning;

        if (c == '~') {
            boolean sawFile = false;
            c = next();
            if (c == '"') {
                full.append(c);
                fname = readString();
                full.append(fname);
                if (curr() != ':') throw error();
                c = next();
                full.append("\":");
                sawFile = true;
            }
            s = readSpanChunk();
            full.append(s);
            c = curr();
            full.append(c);
            int lineOrCol;
            try {
                lineOrCol = Integer.parseInt(s,10);
            } catch (NumberFormatException nfe) {
                System.out.println("s = \""+s+"\" c = '"+c+"' full = \""+full.toString()+"\"");
                throw error();
            }
            if (c==':') {
                c = next();
                line = lineOrCol;
                s = readSpanChunk();
                full.append(s);
                c = curr();
                full.append(c);
                column = Integer.parseInt(s,10);
            } else if (sawFile) {
                throw error();
            } else {
                // line unchanged
                column = lineOrCol;
            }
            ending = new SourceLocRats(fname, line, column, 0);
        }
        Span lastSpan = new Span(beginning, ending);
        if (!Character.isWhitespace(c)) {
            return bug("Unexpected non-whitespace "+c);
        }
        StringBuilder sb = new StringBuilder();
        lastSpan.appendTo(sb,true,true);
        sb.append('\n');
        if (!sb.toString().equals(full.toString())) {
            System.out.println("Read '"+full.toString()+"' as '"+sb.toString()+"'");
            throw error();
        }
        return lastSpan;
    }

    protected Object wordToUnknownObject(String word) throws IOException {
        Object o = super.wordToUnknownObject(word);
        System.out.println("Read unknown object of class "+o.getClass());
        return o;
    }

    @Override
    protected Object readUnknownObject() throws IOException {
        char c = readCharWord();
        // System.out.println("readUnknownObject("+c+")");
        switch (c) {
            case '@':
                return readSpan();
            case 'M':
                next();
                return Modifiers.decode(readWord());
            case 'I':
                next();
                return new BigInteger(readNum());
            default:
                // Fall through
        }
        String w = readWord();
        readToNewline();
        return wordToUnknownObject(w);
    }

    @Override
    protected void readFieldDelim(String s) throws IOException {
        if (ASTIO.useFieldLabels) {
            super.readFieldDelim(s);
        }
    }

    @Override
    protected IOException error() {
        StringBuilder sb = new StringBuilder();
        sb.append("Can't parse serialization; next few lines:\n*****\n");
        sb.append(currentChar);
        try {
            for (int i=0; i < 10; i++) {
                sb.append(in.readLine());
                sb.append("\n");
            }
        } catch (IOException e) {
            // Ignore while fast forwarding
        }
        sb.append("*****\n");
        return new IOException(sb.toString());
    }

}
