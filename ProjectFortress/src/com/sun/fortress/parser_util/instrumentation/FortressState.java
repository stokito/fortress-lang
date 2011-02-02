/*******************************************************************************
    Copyright 2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

/*
 * Class for managing global state in instrumented parser
 */

package com.sun.fortress.parser_util.instrumentation;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class FortressState implements xtc.util.State {

    private List<Info.SequenceInfo> current = null;
    private LinkedList<List<Info.SequenceInfo>> saved = new LinkedList<List<Info.SequenceInfo>>();
    private int depth = 0;

    public FortressState() {
    }

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
            if (parseDone) {
                i.committedCount++;
            }
        }

        List<Info.SequenceInfo> top = null;
        if (!saved.isEmpty()) {
            top = saved.removeFirst();
        }
        if (top != null) {
            top.addAll(current);
        }
        current = top;
    }

    public void add(Info.SequenceInfo info) {
        current.add(info);
    }
}
