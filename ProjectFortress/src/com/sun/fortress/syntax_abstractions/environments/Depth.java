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

package com.sun.fortress.syntax_abstractions.environments;

public abstract class Depth {

    public abstract Depth getParent();
    public abstract String getType(String baseType);
    public abstract <T> T accept(Visitor<T> visitor);

    public Depth addStar() {
        return new ListDepth(this);
    }
    public Depth addPlus() {
        return new ListDepth(this);
    }
    public Depth addOptional() {
        return new OptionDepth(this);
    }

    public interface Visitor<T> {
        T forBaseDepth(BaseDepth d);
        T forListDepth(ListDepth d);
        T forOptionDepth(OptionDepth d);
    }

    public static class BaseDepth extends Depth {
        public Depth getParent() {
            throw new UnsupportedOperationException("cannot get parent of BaseDepth");
        }
        public String getType(String baseType) {
            return baseType;
        }
        public <T> T accept(Visitor<T> visitor) {
            return visitor.forBaseDepth(this);
        }
        public String toString() {
            return "B";
        }
    }

    public static class ListDepth extends Depth {
        private Depth d;
        ListDepth(Depth d) { this.d = d; }
        public Depth getParent(){
            return d;
        }
        public String getType(String baseType){
            return "List<" + d.getType(baseType) + ">";
        }
        public <T> T accept(Visitor<T> visitor) {
            return visitor.forListDepth(this);
        }
        public String toString() {
            return "L" + d.toString();
        }
    }

    public static class OptionDepth extends Depth {
        private Depth d;
        OptionDepth(Depth d) { this.d = d; }
        public Depth getParent(){
            return d;
        }
        public String getType(String baseType) {
            return "Option<" + d.getType(baseType) + ">";
        }
        public <T> T accept(Visitor<T> visitor) {
            return visitor.forOptionDepth(this);
        }
        public String toString() {
            return "O" + d.toString();
        }
    }
}
