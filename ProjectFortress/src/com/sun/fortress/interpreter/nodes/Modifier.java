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

// / type universal_mod = [ `Static | `Test | `Shared ]
// / type trait_mod = [ `Private | `Value | universal_mod ]
// / type object_mod = trait_mod
// / type fn_mod = [ `Atomic | `IO | `Private | `Pure | universal_mod ]
// / type method_mod = [ `Getter | `Setter | `Abstract | fn_mod ]
// / type var_mod = [ `Unit | `Var | universal_mod ]
// / type field_mod = [ `Hidden | `Settable | `Var | `Wrapped |
// universal_mod ]
// / type param_mod = [ `In | `Out | `InOut | `Transient | universal_mod ]
// / type modifier =
// / [ trait_mod | object_mod | fn_mod | method_mod | var_mod
// / | field_mod | param_mod ] node
// /
public abstract class Modifier extends Node {

    /*
     * A bunch of inner classes and interfaces. These did not seem substantial
     * enough to justify individual source files for each.
     */

    Modifier(Span span) {
        super(span);
    }

    @Override
    public boolean equals(Object o) {
        return getClass().equals(o.getClass());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public static interface UniversalMod {
    }

    public static interface TraitMod extends UniversalMod {
    }

    public static interface ObjectMod extends TraitMod {
    }

    public static interface FnMod extends UniversalMod {
    }

    public static interface MethodMod extends FnMod {
    }

    public static interface VarMod extends UniversalMod {
    }

    public static interface FieldMod extends UniversalMod {
    }

    public static interface ParamMod extends UniversalMod {
    }

    final static public class Shared extends Modifier implements UniversalMod {
        public Shared(Span span) {
            super(span);
        }

        @Override
        public <T> T acceptInner(NodeVisitor<T> v) {
            return v.forModifierShared(this);
        }
    }

    final static public class Static extends Modifier implements UniversalMod {
        public Static(Span span) {
            super(span);
        }

        @Override
        public <T> T acceptInner(NodeVisitor<T> v) {
            return v.forModifierStatic(this);
        }
    }

    final static public class Test extends Modifier implements UniversalMod {
        public Test(Span span) {
            super(span);
        }

        @Override
        public <T> T acceptInner(NodeVisitor<T> v) {
            return v.forModifierTest(this);
        }
    }

    final static public class Private extends Modifier implements TraitMod,
            FnMod {
        public Private(Span span) {
            super(span);
        }

        @Override
        public <T> T acceptInner(NodeVisitor<T> v) {
            return v.forModifierPrivate(this);
        }
    }

    final static public class Value extends Modifier implements TraitMod {
        public Value(Span span) {
            super(span);
        }

        @Override
        public <T> T acceptInner(NodeVisitor<T> v) {
            return v.forModifierValue(this);
        }
    }

    final static public class Atomic extends Modifier implements FnMod {
        public Atomic(Span span) {
            super(span);
        }

        @Override
        public <T> T acceptInner(NodeVisitor<T> v) {
            return v.forModifierAtomic(this);
        }
    }

    final static public class IO extends Modifier implements FnMod {
        public IO(Span span) {
            super(span);
        }

        @Override
        public <T> T acceptInner(NodeVisitor<T> v) {
            return v.forModifierIO(this);
        }
    }

    final static public class Pure extends Modifier implements FnMod {
        public Pure(Span span) {
            super(span);
        }

        @Override
        public <T> T acceptInner(NodeVisitor<T> v) {
            return v.forModifierPure(this);
        }
    }

    final static public class Widens extends Modifier implements MethodMod {
        public Widens(Span span) {
            super(span);
        }

        @Override
        public <T> T acceptInner(NodeVisitor<T> v) {
            return v.forModifierWidens(this);
        }
    }

    final static public class Getter extends Modifier implements MethodMod {
        public Getter(Span span) {
            super(span);
        }

        @Override
        public <T> T acceptInner(NodeVisitor<T> v) {
            return v.forModifierGetter(this);
        }
    }

    final static public class Setter extends Modifier implements MethodMod {
        public Setter(Span span) {
            super(span);
        }

        @Override
        public <T> T acceptInner(NodeVisitor<T> v) {
            return v.forModifierSetter(this);
        }
    }

    final static public class Abstract extends Modifier implements MethodMod {
        public Abstract(Span span) {
            super(span);
        }

        @Override
        public <T> T acceptInner(NodeVisitor<T> v) {
            return v.forModifierAbstract(this);
        }
    }

    final static public class Unit extends Modifier implements VarMod {
        public Unit(Span span) {
            super(span);
        }

        @Override
        public <T> T acceptInner(NodeVisitor<T> v) {
            return v.forModifierUnit(this);
        }
    }

    final static public class Var extends Modifier implements VarMod, FieldMod {
        public Var(Span span) {
            super(span);
        }

        @Override
        public <T> T acceptInner(NodeVisitor<T> v) {
            return v.forModifierVar(this);
        }
    }

    final static public class Hidden extends Modifier implements FieldMod {
        public Hidden(Span span) {
            super(span);
        }

        @Override
        public <T> T acceptInner(NodeVisitor<T> v) {
            return v.forModifierHidden(this);
        }
    }

    final static public class Settable extends Modifier implements FieldMod {
        public Settable(Span span) {
            super(span);
        }

        @Override
        public <T> T acceptInner(NodeVisitor<T> v) {
            return v.forModifierSettable(this);
        }
    }

    final static public class Wrapped extends Modifier implements FieldMod {
        public Wrapped(Span span) {
            super(span);
        }

        @Override
        public <T> T acceptInner(NodeVisitor<T> v) {
            return v.forModifierWrapped(this);
        }
    }

    final static public class In extends Modifier implements ParamMod {
        public In(Span span) {
            super(span);
        }

        @Override
        public <T> T acceptInner(NodeVisitor<T> v) {
            return v.forModifierIn(this);
        }
    }

    final static public class Out extends Modifier implements ParamMod {
        public Out(Span span) {
            super(span);
        }

        @Override
        public <T> T acceptInner(NodeVisitor<T> v) {
            return v.forModifierOut(this);
        }
    }

    final static public class InOut extends Modifier implements ParamMod {
        public InOut(Span span) {
            super(span);
        }

        @Override
        public <T> T acceptInner(NodeVisitor<T> v) {
            return v.forModifierInOut(this);
        }
    }

    final static public class Transient extends Modifier implements ParamMod {
        public Transient(Span span) {
            super(span);
        }

        @Override
        public <T> T acceptInner(NodeVisitor<T> v) {
            return v.forModifierTransient(this);
        }
    }

}
