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
import java.io.BufferedReader;
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

public class FileReadStream extends NativeConstructor {
    private static NativeConstructor con = null;

    public FileReadStream(BetterEnv env, FTypeObject selfType, GenericWithParams def) {
        super(env, selfType, def);
    }

    protected FNativeObject makeNativeObject(List<FValue> args,
                                             NativeConstructor con) {
        this.con = con;
        String name = args.get(0).getString();
        try {
            BufferedReader r = Useful.utf8BufferedFileReader(name);
            return new PrimReader(name, r);
        } catch (FileNotFoundException ex) {
            return error("FileNotFound: "+name);
        }
    }

    private static final class PrimReader extends FNativeObject {
        protected final BufferedReader reader;
        protected final String name;
        protected boolean eof = false;
        protected boolean consumed = false;

        public PrimReader(String name, BufferedReader reader) {
            super(FileReadStream.con);
            this.reader = reader;
            this.name = name;
        }

        public NativeConstructor getConstructor() {
            return FileReadStream.con;
        }

        public String getString() {
            return "<Handle to \""+name+"\">";
        }

        public void whenUnconsumed() {
            if (consumed) {
                error(errorMsg("Performed operation on consumed FileReadStream ",name));
            }
        }

        public boolean seqv(FValue v) {
            return (v==this);
        }
    }

    private static abstract class r2S extends NativeMeth0 {
        protected abstract String f(PrimReader r) throws IOException;
        protected final FValue act(FObject self) {
            try {
                return FString.make(f((PrimReader)self));
            } catch (IOException e) {
                return error("Read IOException on "+self.getString());
            }
        }
    }

    private static abstract class r2B extends NativeMeth0 {
        protected abstract boolean f(PrimReader r) throws IOException;
        protected final FValue act(FObject self) {
            try {
                return FBool.make(f((PrimReader)self));
            } catch (IOException e) {
                return error("Ready IOException on "+self.getString());
            }
        }
    }

    public static final class fileName extends r2S {
        protected final String f(PrimReader r) {
            return r.name;
        }
    }

    public static final class toString extends r2S {
        protected final String f(PrimReader r) {
            return r.getString();
        }
    }

    public static final class readLine extends r2S {
        protected final String f(PrimReader r) throws IOException {
            String line = r.reader.readLine();
            if (line==null) {
                r.eof = true;
                line = "";
            }
            return line;
        }
    }

    public static final class readChar extends r2S {
        protected final String f(PrimReader r) throws IOException {
            int c = r.reader.read();
            if (c==-1) {
                r.eof = true;
                return "";
            }
            return (String.valueOf((char)c));
        }
    }

    public static final class readk extends NativeMeth1 {
        protected final FValue act(FObject self, FValue size) {
            PrimReader r = (PrimReader)self;
            int k = size.getInt();
            k = (k <= 0)? 65536 : k;
            char c[] = new char[k];
            try {
                k = r.reader.read(c,0,k);
                if (k==-1) {
                    r.eof = true;
                    return FString.make("");
                }
                return FString.make(new String(c,0,k));
            } catch (IOException e) {
                return error(r.getString()+".read("+k+") IO error.");
            }
        }
    }

    public static final class eof extends r2B {
        protected final boolean f(PrimReader r) {
            return r.eof;
        }
    }

    public static final class ready extends r2B {
        protected final boolean f(PrimReader r) throws IOException {
            return r.reader.ready();
        }
    }

    public static final class close extends NativeMeth0 {
        protected final FValue act(FObject x) {
            PrimReader r = (PrimReader) x;
            try {
                r.reader.close();
                r.eof = true;
                return FVoid.V;
            } catch (IOException e) {
                return error("Close IOException on "+x.getString());
            }
        }
    }

    public static final class whenUnconsumed extends NativeMeth0 {
        synchronized protected final FValue act(FObject self) {
            PrimReader r = (PrimReader) self;
            r.whenUnconsumed();
            return FVoid.V;
        }
    }

    public static final class consume extends NativeMeth0 {
        protected final FValue act(FObject self) {
            PrimReader r = (PrimReader) self;
            synchronized (r) {
                r.whenUnconsumed();
                r.consumed = true;
            }
            return FVoid.V;
        }
    }
}
