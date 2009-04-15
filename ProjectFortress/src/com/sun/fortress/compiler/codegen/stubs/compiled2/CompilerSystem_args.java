package com.sun.fortress.compiler.codegen.stubs.compiled2;
import com.sun.fortress.nativeHelpers.*;
import com.sun.fortress.compiler.runtimeValues.*;

public final class CompilerSystem_args {
        public FZZ32 length() {return FZZ32.make(simpleSystem.nativeArgcount());}
        public FString elem(FZZ32 n) {return FString.make(simpleSystem.nativeArg(n.getValue()));}
}
