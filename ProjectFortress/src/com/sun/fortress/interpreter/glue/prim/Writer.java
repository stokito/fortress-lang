/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.glue.prim;

import static com.sun.fortress.exceptions.ProgramError.error;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.types.FTypeObject;
import com.sun.fortress.interpreter.evaluator.values.*;
import com.sun.fortress.interpreter.glue.NativeFn0;
import com.sun.fortress.interpreter.glue.NativeMeth0;
import com.sun.fortress.interpreter.glue.NativeMeth1;
import com.sun.fortress.nodes.ObjectConstructor;

import java.io.*;
import java.nio.charset.Charset;
import java.util.List;

public class Writer extends NativeConstructor {
    private static NativeConstructor con = null;

    public Writer(Environment env, FTypeObject selfType, ObjectConstructor def) {
        super(env, selfType, def);
        con = this;
    }

    @Override
    protected FNativeObject makeNativeObject(List<FValue> args, NativeConstructor con) {
        String name = args.get(0).getString();
        try {
            OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(name), Charset.forName("UTF-8"));
            return new PrimWriter(name, w);
        }
        catch (FileNotFoundException ex) {
            return error("FileNotFound: " + name);
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
            return (v == this);
        }
    }

    private static abstract class w2S extends NativeMeth0 {
        protected abstract String f(PrimWriter r) throws IOException;

        @Override
        public final FValue applyMethod(FObject self) {
            try {
                return FString.make(f((PrimWriter) self));
            }
            catch (IOException e) {
                return error("Write IOException on " + self.getString());
            }
        }
    }

    private static abstract class wS2V extends NativeMeth1 {
        protected abstract void f(java.io.Writer r, String s) throws IOException;

        @Override
        public final FValue applyMethod(FObject self, FValue s) {
            try {
                f(((PrimWriter) self).writer, s.getString());
                return FVoid.V;
            }
            catch (IOException e) {
                return error("Write IOException on " + self.getString());
            }
        }
    }

    private static abstract class w2V extends NativeMeth0 {
        protected abstract void f(java.io.Writer r) throws IOException;

        @Override
        public final FValue applyMethod(FObject self) {
            try {
                f(((PrimWriter) self).writer);
                return FVoid.V;
            }
            catch (IOException e) {
                return error("IOException on " + self.getString());
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

    /**
     * This code used to use FileDescriptor.{out,err}, but now uses
     * System.{out.err}.  We only *write* to the underlying
     * PrintStream, which according to the Java 1.6 docs will bypass
     * the system encoding.  This means we can force UTF-8 here and it
     * will actually mean something (rather than resulting in bogus
     * double-encoded nonsense).
     */
    private static abstract class v2w extends NativeFn0 {
        protected abstract OutputStream outputStream();

        protected abstract String dummyName();

        @Override
        public final FValue applyToArgs() {
            OutputStreamWriter out = new OutputStreamWriter(this.outputStream(), Charset.forName("UTF-8"));
            return new PrimWriter(this.dummyName(), out);
        }
    }

    public static final class outputWriter extends v2w {
        @Override
        protected OutputStream outputStream() {
            return System.out;
        }

        @Override
        protected String dummyName() {
            return "<stdout>";
        }
    }

    public static final class errorWriter extends v2w {
        @Override
        protected OutputStream outputStream() {
            return System.err;
        }

        @Override
        protected String dummyName() {
            return "<stderr>";
        }
    }

    public static final class LineSeparator extends NativeFn0 {
        @Override
        protected final FValue applyToArgs() {
            return FString.make(System.getProperty("line.separator"));
        }
    }

    @Override
    protected void unregister() {
        con = null;
    }

}
