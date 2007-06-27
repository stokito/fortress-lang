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

package com.sun.fortress.interpreter.nodes;

import com.sun.fortress.interpreter.nodes_util.Span;
import com.sun.fortress.interpreter.useful.Option;
import java.util.List;

import com.sun.fortress.interpreter.evaluator.ProgramError;
import com.sun.fortress.interpreter.useful.IterableOnce;
import com.sun.fortress.interpreter.useful.MagicNumbers;
import com.sun.fortress.interpreter.useful.Useful;


public abstract class VarDefOrDecl extends Node implements DefOrDecl {

   
    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.DefOrDecl#stringNames()
     */
    public IterableOnce<String> stringNames() {
        return new IterableOnceForLValueList(lhs);
    }

    List<LValue> lhs;

    VarDefOrDecl(Span s) {
        super(s);
    }

    protected boolean superEquals(VarDefOrDecl v) {
        return lhs.equals(v.getLhs());
    }

    public List<LValue> getLhs() {
        return lhs;
    }

    protected int superHashCode() {
        return MagicNumbers.hashList(lhs, MagicNumbers.O);
    }

    // /**
    // * @return Returns the mods.
    // */
    // public List<Modifier> getMods() {
    // return mods;
    // }
    /**
     * @deprecated Only works for old-style single name vardecl.
     * @return Returns the name.
     */
    @Deprecated
    public Id getName() {
        if (lhs.size() != 1) {
            throw new ProgramError(this,
                    "Only works for old-style single name vardecl.");
        }
        LValue lv = lhs.get(0);
        if (!(lv instanceof LValueBind)) {
            throw new ProgramError(this,
                    "Only works for old-style single name vardecl.");
        }
        LValueBind lvb = (LValueBind) lv;
        return lvb.getName();

    }

    /**
     * @deprecated Only works for old-style single name vardecl.
     * @return Returns the name.
     */
    @Deprecated
    public Option<TypeRef> getType() {
        if (lhs.size() != 1) {
            throw new ProgramError(this,
                    "Only works for old-style single name vardecl.");
        }
        LValue lv = lhs.get(0);
        if (!(lv instanceof LValueBind)) {
            throw new ProgramError(this,
                    "Only works for old-style single name vardecl.");
        }
        LValueBind lvb = (LValueBind) lv;
        return lvb.getType();

    }

    //
    // public String stringName() {
    // return name.getName();
    // }

    public void init(Id name, Option<TypeRef> type, List<Modifier> mods,
            boolean mutable) {
        lhs = Useful.<LValue> list(new LValueBind(span, name, type, mods,
                mutable));

    }

}
