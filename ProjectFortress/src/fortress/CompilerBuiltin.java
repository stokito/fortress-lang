/*******************************************************************************
 Copyright 2009,2010, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package fortress;

import com.sun.fortress.compiler.runtimeValues.FValue;

public class CompilerBuiltin {
    public static interface IntLiteral extends fortress.CompilerBuiltin.Object {
        public static abstract class DefaultTraitMethods extends FValue implements IntLiteral {}
    }

    public static interface FloatLiteral extends fortress.CompilerBuiltin.Object {
        public static abstract class DefaultTraitMethods extends FValue implements FloatLiteral {}
    }

    public static interface ZZ32 extends fortress.CompilerBuiltin.Number {
        public static abstract class DefaultTraitMethods extends FValue implements ZZ32 {}
    }

    public static interface ZZ64 extends fortress.CompilerBuiltin.Number {
        public static abstract class DefaultTraitMethods extends FValue implements ZZ64 {}
    }

    public static interface RR32 extends fortress.CompilerBuiltin.Number {
        public static abstract class DefaultTraitMethods extends FValue implements RR32 {}
    }

    public static interface RR64 extends fortress.CompilerBuiltin.Number {
        public static abstract class DefaultTraitMethods extends FValue implements RR64 {}
    }

    public static interface Number extends fortress.CompilerBuiltin.Object {
        public static abstract class DefaultTraitMethods extends FValue implements Number {}
    }

    public static interface Boolean extends fortress.CompilerBuiltin.Object {
        public static abstract class DefaultTraitMethods extends FValue implements Boolean {}
    }

    public static interface String extends fortress.CompilerBuiltin.Object {
        public static abstract class DefaultTraitMethods extends FValue implements String {}
    }

    public static interface Character extends fortress.CompilerBuiltin.Object {
        public static abstract class DefaultTraitMethods extends FValue implements Character {}
    }

    public static interface JavaBufferedReader extends fortress.CompilerBuiltin.Object {
        public static abstract class DefaultTraitMethods extends FValue implements JavaBufferedReader {}
    }

    public static interface JavaBufferedWriter extends fortress.CompilerBuiltin.Object {
        public static abstract class DefaultTraitMethods extends FValue implements JavaBufferedWriter {}
    }

    public static interface Vector extends fortress.CompilerBuiltin.Object {
        public static abstract class DefaultTraitMethods extends FValue implements Vector {}
    }

    public static interface Object extends fortress.AnyType.Any {
        public static abstract class DefaultTraitMethods extends FValue implements Object {}
    }
}
