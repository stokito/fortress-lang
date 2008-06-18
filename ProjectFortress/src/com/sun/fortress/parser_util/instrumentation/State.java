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

/*
 * Class for managing global state in instrumented parser
 */

package com.sun.fortress.parser_util.instrumentation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class State implements xtc.util.State {

    private List<Info.SequenceInfo> current = null;
    private LinkedList<List<Info.SequenceInfo>> saved = new LinkedList<List<Info.SequenceInfo>>();
    private int depth = 0;

    public State() {}

    public void reset(String file) {
        // No action required
        // Instrumenter doesn't insert "resettable", so never called (right?)
    }

    public void start() {
        saved.addFirst(current);
        current = new ArrayList<Info.SequenceInfo>();
        depth++;
    }

    public void abort() {
        for (Info.SequenceInfo i : current) {
            i.startedCount++;
        }

        current = saved.removeFirst();
        depth--;
    }

    public void commit() {
        depth--;
        boolean parseDone = (depth == 0);

        for (Info.SequenceInfo i : current) {
            i.startedCount++;
            i.endedCount++;
            if (parseDone) { i.committedCount++; }
        }

        List<Info.SequenceInfo> top = null;
        if (!saved.isEmpty()) {
            top = saved.removeFirst();
            top.addAll(current);
        }
        current = top;
    }

    public void add(Info.SequenceInfo info) {
        current.add(info);
    }
}
