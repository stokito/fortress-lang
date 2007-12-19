/*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
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

package com.sun.fortress.interpreter.evaluator.values;
import java.util.List;

import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.Evaluator;
import com.sun.fortress.interpreter.evaluator.FortressError;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeGenerator;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.AbstractNode;
import com.sun.fortress.nodes_util.NodeUtil;


public class FGenerator extends FValue {
    // For now assume that we only have a single identifier.
    //  List<Id> id;
    //  List<Range> range;
    private final Id id;
    private final FRange range;
    private final FRangeIterator iterator;

    public FGenerator(List<Id> b, FRange r) {
        id = b.get(0);
        range = r;
        iterator = new FRangeIterator(range);
    }

    public FGenerator(Id b, FRange r) {
        id = b;
        range = r;
        iterator = new FRangeIterator(range);
    }

    public FType type() {
        return new FTypeGenerator(range.getBase(), range.getSize());
    }

    public String toString() {
        return "Generator: base = " + range.getBase() + " size = " + range.getSize();
    }

    public String getString() {
        return "Generator: base = " + range.getBase() + " size = " + range.getSize();
    }

    public boolean hasNext() { return iterator.hasNext();}

    public boolean hasAtMostOne() { return iterator.hasAtMostOne();}

    public FGenerator firstHalf() {
        return new FGenerator(id, range.firstHalf());
    }


    public FGenerator secondHalf() {
        return new FGenerator(id, range.secondHalf());
    }

    public boolean update(Expr body, Evaluator ev) {
        if (iterator.hasNext()) {
            FValue val = (FValue) iterator.next();
            try {
                ev.e.putValue(NodeUtil.nameString(id), val);
            } catch (FortressError pe) {
                throw pe.setContext(body,ev.e);
            }
            ev.eval(body);
            return true;
        } else return false;
    }

    public FValue debugGetIter(Environment e) {
        return e.getValue(NodeUtil.nameString(id));
    }

    public boolean isSequential() {
        return range.isSequential();
    }

}
