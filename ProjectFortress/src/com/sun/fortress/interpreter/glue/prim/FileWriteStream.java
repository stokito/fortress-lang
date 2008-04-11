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

import java.lang.String;
import java.util.List;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.sun.fortress.useful.Useful;
import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeObject;
import com.sun.fortress.interpreter.evaluator.values.NativeConstructor;
import com.sun.fortress.interpreter.evaluator.values.FBool;
import com.sun.fortress.interpreter.evaluator.values.FObject;
import com.sun.fortress.interpreter.evaluator.values.FOrdinaryObject;
import com.sun.fortress.interpreter.evaluator.values.FString;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.FVoid;
import com.sun.fortress.interpreter.glue.NativeMeth0;
import com.sun.fortress.interpreter.glue.NativeMeth1;
import com.sun.fortress.interpreter.glue.NativeMeth2;
import com.sun.fortress.nodes.GenericWithParams;

import static com.sun.fortress.interpreter.evaluator.ProgramError.errorMsg;
import static com.sun.fortress.interpreter.evaluator.ProgramError.error;

public class FileWriteStream extends NativeConstructor {
    private static NativeConstructor con = null;

    public FileWriteStream(BetterEnv env, FTypeObject selfType, GenericWithParams def) {
        super(env, selfType, def);
    }

    protected FNativeObject makeNativeObject(List<FValue> args,
                                             NativeConstructor con) {
        this.con = con;
        String name = args.get(0).getString();
        try {
            BufferedWriter r = Useful.utf8BufferedFileWriter(name);
            return new PrimWriter(name, r);
        } catch (FileNotFoundException ex) {
            return error("FileNotFound: "+name);
        }
    }

    private static final class PrimWriter extends FNativeObject {
        protected final BufferedWriter writer;
        protected final String name;

        public PrimWriter(String name, BufferedWriter writer) {
            super(FileWriteStream.con);
            this.writer = writer;
            this.name = name;
        }

        public NativeConstructor getConstructor() {
            return FileWriteStream.con;
        }

        public String getString() {
            return "<Write handle to \""+name+"\">";
        }

        public boolean seqv(FValue v) {
            return (v==this);
        }
    }

    private static abstract class w2S extends NativeMeth0 {
        protected abstract String f(PrimWriter r) throws IOException;
        protected final FValue act(FObject self) {
            try {
                return FString.make(f((PrimWriter)self));
            } catch (IOException e) {
                return error("Write IOException on "+self.getString());
            }
        }
    }

    private static abstract class wS2V extends NativeMeth1 {
        protected abstract void f(BufferedWriter r, String s) throws IOException;
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
        protected abstract void f(BufferedWriter r) throws IOException;
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
        protected final String f(PrimWriter r) {
            return r.name;
        }
    }

    public static final class toString extends w2S {
        protected final String f(PrimWriter r) {
            return r.getString();
        }
    }

    public static final class write extends wS2V {
        protected final void f(BufferedWriter w, String s) throws IOException {
            w.append(s);
        }
    }

    public static final class flush extends w2V {
        protected void f(BufferedWriter w) throws IOException {
            w.flush();
        }
    }

    public static final class close extends w2V {
        protected void f(BufferedWriter w) throws IOException {
            w.close();
        }
    }
}
