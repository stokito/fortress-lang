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

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.List;

import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.types.FTypeObject;
import com.sun.fortress.interpreter.evaluator.values.FObject;
import com.sun.fortress.interpreter.evaluator.values.FString;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.FVoid;
import com.sun.fortress.interpreter.evaluator.values.NativeConstructor;
import com.sun.fortress.interpreter.glue.NativeFn0;
import com.sun.fortress.interpreter.glue.NativeMeth0;
import com.sun.fortress.interpreter.glue.NativeMeth1;
import com.sun.fortress.nodes.GenericWithParams;

public class Writer extends NativeConstructor {
    private static NativeConstructor con = null;

    public Writer(Environment env, FTypeObject selfType, GenericWithParams def) {
        super(env, selfType, def);
        con = this;
    }

    @Override
	protected FNativeObject makeNativeObject(List<FValue> args,
                                             NativeConstructor con) {
        String name = args.get(0).getString();
        try {
            OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(
					name), Charset.forName("UTF-8"));
			return new PrimWriter(name, w);
        } catch (FileNotFoundException ex) {
            return error("FileNotFound: "+name);
        }
    }

    static final class PrimWriter extends FNativeObject {
        protected final OutputStreamWriter writer;
        protected final String name;

        public PrimWriter(String name, OutputStreamWriter writer) {
			super(Writer.con);
            this.writer = writer;
            this.name = name;
        }

        @Override
		public NativeConstructor getConstructor() {
            return Writer.con;
        }

        @Override
		public String getString() {
            return "<Writer on \"" + name + "\">";
        }

        public OutputStreamWriter getWriter() {
			return writer;
		}
        
        @Override
		public boolean seqv(FValue v) {
            return (v==this);
        }
    }

    private static abstract class w2S extends NativeMeth0 {
        protected abstract String f(PrimWriter r) throws IOException;
        @Override
		protected final FValue act(FObject self) {
            try {
                return FString.make(f((PrimWriter)self));
            } catch (IOException e) {
                return error("Write IOException on "+self.getString());
            }
        }
    }

    private static abstract class wS2V extends NativeMeth1 {
        protected abstract void f(java.io.Writer r, String s)
				throws IOException;
        @Override
		protected final FValue act(FObject self, FValue s) {
            try {
                f(((PrimWriter)self).writer, s.getString());
                return FVoid.V;
            } catch (IOException e) {
                return error("Write IOException on "+self.getString());
            }
        }
    }

    private static abstract class w2V extends NativeMeth0 {
        protected abstract void f(java.io.Writer r) throws IOException;
        @Override
		protected final FValue act(FObject self) {
            try {
                f(((PrimWriter)self).writer);
                return FVoid.V;
            } catch (IOException e) {
                return error("IOException on "+self.getString());
            }
        }
    }

    public static final class fileName extends w2S {
        @Override
		protected final String f(PrimWriter r) {
            return r.name;
        }
    }

    public static final class write extends wS2V {
        @Override
		protected final void f(java.io.Writer w, String s) throws IOException {
            w.append(s);
        }
    }

    public static final class flush extends w2V {
        @Override
		protected void f(java.io.Writer w) throws IOException {
            w.flush();
        }
    }

    public static final class close extends w2V {
        @Override
		protected void f(java.io.Writer w) throws IOException {
            w.close();
        }
    }

    private static abstract class v2w extends NativeFn0 {
		protected abstract FileDescriptor fileDescriptor();

		@Override
		protected final PrimWriter act() {
			FileOutputStream fd = new FileOutputStream(fileDescriptor());
			OutputStreamWriter stdout = new OutputStreamWriter(fd, Charset
					.forName("UTF-8"));
			return new PrimWriter("<stdout>", stdout);
		}
	}

	public static final class outputWriter extends v2w {
		@Override
		protected FileDescriptor fileDescriptor() {
			return FileDescriptor.out;
		}
	}

	public static final class errorWriter extends v2w {
		@Override
		protected FileDescriptor fileDescriptor() {
			return FileDescriptor.err;
		}
	}
    
    @Override
    protected void unregister() {
        con = null;
    }
}
