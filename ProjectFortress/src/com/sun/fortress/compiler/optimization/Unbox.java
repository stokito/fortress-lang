/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.optimization;

import java.util.HashMap;
import java.util.List;

import com.sun.fortress.useful.Bits;

abstract public class Unbox {
    /*
     * Cases for unboxing:
     * 
     * 1) primitive.  ZZ{8,16,32,64}, RR{32,64}, Boolean
     * 2) singleton (non-generic) object.
     * 3) unboxed fields
     * 4) comprises unboxed
     * 
     */
    
    /**
     * How wide is this unboxed type, in bits?
     */
    abstract public int bitWidth();
    
    /**
     * What is the Java representation of this unboxed type?
     * By default, integer types are used.
     * Implementation types are
     * V,B,S,I,J,JJ,JJJ, etc.
     * 
     * Note that zero-bit representations are possible; if a type has only one
     * instance, ever, then it takes no bits to distinguish its members.
     * 
     * @return
     */
    public String javaRep() {
        int b = bitWidth();
        if (b == 0)
            return "V";
        if (b <= 8)
            return "B";
        if (b <= 16)
            return "S";
        if (b <= 32)
            return "I";

        StringBuilder buf = new StringBuilder();
        buf.append("J");
        while (b > 64) {
            b = b - 64;
            buf.append("J");
        }
        return buf.toString();
    }
    
    /**
     * An unbox, that has been tagged with some identifier (a field name,
     * a variant name).
     * 
     * @author dr2chase
     */
    public static class TaggedUnbox extends Unbox {
        final Unbox item;
        final Object tag;
        public TaggedUnbox(Object tag, Unbox item) {
            this.item = item;
            this.tag = tag;
        }
        @Override
        public int bitWidth() {
            return item.bitWidth();
        }
        @Override
        public String javaRep() {
            return item.javaRep();
        }
        public Object tag() {
            return tag;
        }
    }
    
    /**
     * Unboxing of the various integers.
     * @author dr2chase
     */
    public static class ZZ extends Unbox {
        final int width;
        
        public ZZ(int n) {
            width = n;
            if (n == 8) {
            } else if (n == 16) {
            } else if (n == 32) {
            } else if (n == 64) {
            } else throw new IllegalArgumentException("Expected n in {8,16,32,64}, not " + n);
        }

        @Override
        public int bitWidth() {
            return width;
        }

    }

    /**
     * Unboxing of the various reals.
     * @author dr2chase
     */
    public static class RR extends Unbox {
        final int width;
        
        public RR(int n) {
            width = n;
            if (n == 32) {
            } else if (n == 64) {
            } else throw new IllegalArgumentException("Expected n in {8,16,32,64}, not " + n);
        }

        @Override
        public int bitWidth() {
            return width;
        }
        public String javaRep() {
            return width == 32 ? "F" : "D";
        }

    }

    /**
     * Unboxing a singleton type (e.g., an object-no-constructor type).
     * @author dr2chase
     */
    public static class Singleton extends Unbox {
        
        public Singleton() {
        }
        
        @Override
        public int bitWidth() {
            return 0;
        }

        @Override
        public String javaRep() {
            return "V";
        }
    }
    
    /**
     * Unboxing comprises -- one choice, of several, provided that each of the
     * comprised items is also unboxable.  This uses a tag+data encoding;
     * an alternate encoding is to use the size of the range, and encode
     * the choice by biasing the stored values.
     * 
     * @author dr2chase
     */
    public static class Comprises extends Unbox {
        final int tagWidth;
        final int maxChoicesWidth;
        final TaggedUnbox[] tagToItem;
        final HashMap<Object, Integer> itemToTag =
            new HashMap<Object, Integer>();
       
        public Comprises(List<TaggedUnbox> choices) {
            int l = choices.size();
            tagToItem = new TaggedUnbox[l];
            
            tagWidth = Bits.ceilLogTwo(l);
            int max = 0;
            int i = 0;
            for (TaggedUnbox u : choices) {
                tagToItem[i] = u;
                itemToTag.put(u.tag(), i);
                i++;
                
                int s = u.bitWidth();
                if (s > max)
                    max = s;
            }
            maxChoicesWidth = max;
        }

        @Override
        public int bitWidth() {
            // TODO Auto-generated method stub
            return tagWidth + maxChoicesWidth;
        }
        
        public int tagWidth() {
            return tagWidth;
        }
        public int tagOffset() {
            return maxChoicesWidth;
        }
        public int dataWidth() {
            return maxChoicesWidth;
        }
        public int dataOffset() {
            return 0;
        }
        public TaggedUnbox itemForTag(int tag) {
            return tagToItem[tag];
        }
        public int tagForItem(Object item) {
            return itemToTag.get(item);
        }
        
        /* Need container type, container index, shift, mask */
        /* Need tag type, tag index, shift, mask */
        
    }
    
    /**
     * Unboxing "structs" -- a concatenation of unboxed values.
     * @author dr2chase
     */
    public static class Contains extends Unbox {
        final int bitWidth;
        final HashMap<Object, Integer> itemToOffset =
            new HashMap<Object, Integer>();
        
        public Contains(List<TaggedUnbox> members) {
            int sum = 0;
            for (TaggedUnbox u : members) {
                itemToOffset.put(u.tag(), sum);
                sum += u.bitWidth();
            }
            bitWidth = sum;
        }

        @Override
        public int bitWidth() {
            return bitWidth;
        }
        public int offsetForItem(Object item) {
            return itemToOffset.get(item);
        }
    }
    
}
