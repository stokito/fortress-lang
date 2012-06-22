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
import com.sun.fortress.interpreter.glue.NativeMeth0;
import com.sun.fortress.interpreter.glue.NativeMeth1;
import com.sun.fortress.nodes.ObjectConstructor;
import com.sun.fortress.useful.Useful;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

public class FileWriteStream extends NativeConstructor {
    private static NativeConstructor _con = null;

    public FileWriteStream(Environment env, FTypeObject selfType, ObjectConstructor def) {
        super(env, selfType, def);
    }

    @Override
    protected FNativeObject makeNativeObject(List<FValue> args, NativeConstructor con) {
        FileWriteStream._con = con;
        String name = args.get(0).getString();
        try {
            BufferedWriter r = Useful.utf8BufferedFileWriter(name);
            return new PrimWriter(name, r);
        }
        catch (FileNotFoundException ex) {
            return error("FileNotFound: " + name);
        }
    }

    private static final class PrimWriter extends FNativeObject {
        protected final BufferedWriter writer;
        protected final String name;

        public PrimWriter(String name, BufferedWriter writer) {
            super(FileWriteStream._con);
            this.writer = writer;
            this.name = name;
        }

        @Override
        public NativeConstructor getConstructor() {
            return FileWriteStream._con;
        }

        @Override
        public String getString() {
            return "<Write handle to \"" + name + "\">";
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
        protected abstract void f(BufferedWriter r, String s) throws IOException;

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
        protected abstract void f(BufferedWriter r) throws IOException;

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

    public static final class toString extends w2S {
        @Override
        protected final String f(PrimWriter r) {
            return r.getString();
        }
    }

    public static final class write extends wS2V {
        @Override
        protected final void f(BufferedWriter w, String s) throws IOException {
            w.append(s);
        }
    }

    public static final class flush extends w2V {
        @Override
        protected void f(BufferedWriter w) throws IOException {
            w.flush();
        }
    }

    public static final class close extends w2V {
        @Override
        protected void f(BufferedWriter w) throws IOException {
            w.close();
        }
    }

    @Override
    protected void unregister() {
        _con = null;
    }
}
