/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.index;

import com.sun.fortress.nodes.LValue;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.TypeOrPattern;
import com.sun.fortress.nodes.VarDecl;
import com.sun.fortress.nodes_util.Modifiers;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;
import edu.rice.cs.plt.lambda.SimpleBox;
import edu.rice.cs.plt.lambda.Thunk;
import edu.rice.cs.plt.tuple.Option;

public class DeclaredVariable extends Variable {

    protected final LValue _lvalue;
    protected int _position;
    protected boolean _isEntireLHS; // necessary when assigning a tuple expr to a singleton
    
    public DeclaredVariable(LValue lvalue, VarDecl decl) {
        _lvalue = lvalue;

        // Figure out my position in the decl.
        int i = 0;
        for (LValue lv : decl.getLhs()) {
            // lvalue came from decl in the first place, so referential equality
            // works just fine.
            if (lv == lvalue) {
                _position = i;
                break;
            }
            ++i;
        }
        
        _isEntireLHS = (decl.getLhs().size() == 1);

        Option<TypeOrPattern> tp = _lvalue.getIdType();
        if (tp.isSome() && tp.unwrap() instanceof Type)
            _thunk = Option.<Thunk<Option<Type>>>some(SimpleBox.make(NodeUtil.optTypeOrPatternToType(tp)));
    }

    public LValue ast() {
        return _lvalue;
    }

    public Modifiers modifiers() {
        return _lvalue.getMods();
    }

    public boolean mutable() {
        return _lvalue.isMutable();
    }
    
    public boolean isEntireLHS() {
        return _isEntireLHS;
    }

    public int position() {
        return _position;
    }

    @Override
    public boolean hasExplicitType() {
        return _lvalue.getIdType().isSome();
    }

    @Override
    public String toString() {
        return _lvalue.toString();
    }

    @Override
    public Span getSpan() {
        return NodeUtil.getSpan(_lvalue);
    }
}
