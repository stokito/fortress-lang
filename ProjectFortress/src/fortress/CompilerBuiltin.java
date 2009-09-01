/*******************************************************************************
 Copyright 2009 Sun Microsystems, Inc.,
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

package fortress;

import com.sun.fortress.compiler.runtimeValues.FValue;

public class CompilerBuiltin {
    public static interface ZZ32 extends fortress.CompilerBuiltin.ZZ64 {
        public static abstract class DefaultTraitMethods extends FValue implements ZZ32 {}
    }

    public static interface ZZ64 extends fortress.CompilerBuiltin.Number {
        public static abstract class DefaultTraitMethods extends FValue implements ZZ64 {}
    }

    public static interface RR32 extends fortress.CompilerBuiltin.RR64 {
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

    public static interface Char extends fortress.CompilerBuiltin.Object {
        public static abstract class DefaultTraitMethods extends FValue implements Char {}
    }

    public static interface Object extends fortress.AnyType.Any {
        public static abstract class DefaultTraitMethods extends FValue implements Object {}
    }
}
