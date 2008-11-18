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

package com.sun.fortress.interpreter.glue.prim;

import static com.sun.fortress.exceptions.ProgramError.error;

import java.io.IOException;
import java.util.List;

import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.types.FTypeObject;
import com.sun.fortress.interpreter.evaluator.values.FObject;
import com.sun.fortress.interpreter.evaluator.values.FString;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.FVoid;
import com.sun.fortress.interpreter.evaluator.values.NativeConstructor;
import com.sun.fortress.interpreter.glue.NativeMeth0;
import com.sun.fortress.interpreter.glue.NativeMeth1;
import com.sun.fortress.nodes.GenericWithParams;

public class BufferedWriter extends NativeConstructor {
    private static NativeConstructor con = null;

    public BufferedWriter(Environment env, FTypeObject selfType,
			GenericWithParams def) {
        super(env, selfType, def);
        con = this;
    }

    @Override
    protected FNativeObject makeNativeObject(List<FValue> args,
			NativeConstructor con) {
		Writer.PrimWriter w = (Writer.PrimWriter) args.get(0);
		int size = args.get(1).getInt();
		return new BuffWriter(new java.io.BufferedWriter(w.getWriter(), size));
    }


    private static final class BuffWriter extends FNativeObject {
		protected final java.io.BufferedWriter writer;

		public BuffWriter(java.io.BufferedWriter writer) {
			super(BufferedWriter.con);
            this.writer = writer;
        }

        @Override
		public NativeConstructor getConstructor() {
            return BufferedWriter.con;
        }

        @Override
		public String getString() {
            return "<BufferedWriter on " + writer.toString() + ">";
        }

        @Override
		public boolean seqv(FValue v) {
            return (v==this);
        }
    }

    private static abstract class w2S extends NativeMeth0 {
        protected abstract String f(BuffWriter r) throws IOException;
        @Override
		protected final FValue act(FObject self) {
            try {
                return FString.make(f((BuffWriter) self));
            } catch (IOException e) {
                return error("Write IOException on "+self.getString());
            }
        }
    }

    private static abstract class wS2V extends NativeMeth1 {
        protected abstract void f(java.io.BufferedWriter r, String s)
				throws IOException;
        @Override
		protected final FValue act(FObject self, FValue s) {
            try {
                f(((BuffWriter) self).writer, s.getString());
                return FVoid.V;
            } catch (IOException e) {
                return error("Write IOException on "+self.getString());
            }
        }
    }

    private static abstract class w2V extends NativeMeth0 {
        protected abstract void f(java.io.BufferedWriter r) throws IOException;
        @Override
		protected final FValue act(FObject self) {
            try {
                f(((BuffWriter) self).writer);
                return FVoid.V;
            } catch (IOException e) {
                return error("IOException on "+self.getString());
            }
        }
    }

    public static final class toString extends w2S {
        @Override
		protected final String f(BuffWriter r) {
            return r.getString();
        }
    }

    public static final class write extends wS2V {
        @Override
		protected final void f(java.io.BufferedWriter w, String s)
				throws IOException {
			w.write(s);
        }
    }

    public static final class flush extends w2V {
        @Override
		protected void f(java.io.BufferedWriter w) throws IOException {
            w.flush();
        }
    }

    public static final class close extends w2V {
        @Override
		protected void f(java.io.BufferedWriter w) throws IOException {
            w.close();
        }
    }

    @Override
    protected void unregister() {
        con = null;
    }
}
