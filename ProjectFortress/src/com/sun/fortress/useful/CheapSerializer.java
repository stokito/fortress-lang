/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

abstract public class CheapSerializer<T> {
    
    /* convert a byte[] containing uft8 representation to a string */
    public static String fromUtf8(byte[] buf)
            throws java.io.UTFDataFormatException {
        return fromUtf8(buf, 0, buf.length);
    }
    public static String fromUtf8(byte[] buf, int off, int len)
            throws java.io.UTFDataFormatException {
        int t = (off + len);
        if ((off < 0) || (len < 0) || (t > buf.length) || (t < 0))
            throw new IndexOutOfBoundsException();
        if (len == 0)
            return "";
        StringBuilder result = new StringBuilder(len); // make an optimistic
                                                     // guess
        synchronized (result) {
            int i = off;
            try {
                while (i < t) {
                    int b = buf[i];
                    if (b == 0)
                        throw new java.io.UTFDataFormatException(
                                "null character in utf8 sequence");
                    if ((b & 0x80) == 0) {
                        result = result.append((char) b);
                        i++;
                    } else if ((b & 0xe0) == 0xc0) {
                        i += 2;
                        if (i > t)
                            throw new IndexOutOfBoundsException();
                        int b1 = buf[i - 1];
                        if ((b1 & 0xc0) != 0x80)
                            throw new java.io.UTFDataFormatException(
                                    "ill formed utf8 escape sequence in '"
                                            + new String(buf) + "'");
                        result = result
                                .append((char) (((b & 0x1f) << 6) + (b1 & 0x3f)));
                    } else if ((b & 0xf0) == 0xe0) {
                        i += 3;
                        if (i > t)
                            throw new IndexOutOfBoundsException();
                        int b1 = buf[i - 2];
                        int b2 = buf[i - 1];
                        if (((b1 & 0xc0) != 0x80) || ((b2 & 0xc0) != 0x80))
                            throw new java.io.UTFDataFormatException(
                                    "ill formed utf8 escape sequence in '"
                                            + new String(buf) + "'");
                        result = result
                                .append((char) ((((b & 0xf) << 12) + ((b1 & 0x3f) << 6)) + (b2 & 0x3f)));
                    } else {
                        throw new java.io.UTFDataFormatException(
                                "ill formed unicode escape sequence in '"
                                        + new String(buf) + "'");
                    }
                }
            } catch (IndexOutOfBoundsException v) {
                throw new java.io.UTFDataFormatException(
                        "incomplete utf8 escape sequence in '"
                                + new String(buf) + "'");
            }
        }
        return result.toString();
    }
    /* convert a string to a byte[] containing utf8 representation */
    public static byte[] toUtf8(String s) {
        int len = s.length();
        byte[] result;
        int newLen = 0;
        /* compute utf8 length */
        for (int i = 0; i < len; i++) {
            int c = (int) s.charAt(i);
            if ((c > 0) && (c < 0x80))
                newLen++;
            else if (c < 0x800) /* includes 0x0 */
                newLen += 2;
            else
                /* c must be < 0xffff */
                newLen += 3;
        }
        /* build correctly sized array and convert */
        if (newLen == len) {
            result = new byte[len];
            // Special case, all easy.
            for (int i = 0; i < len; i++) {
                result[i] = (byte) s.charAt(i);
            }
        } else {
            result = new byte[newLen];
            int newI = 0;
            for (int i = 0; i < len; i++) {
                int c = (int) s.charAt(i);
                if ((c > 0) && (c < 0x80))
                    result[newI++] = (byte) c;
                else if (c < 0x800) { /* includes 0x0 */
                    result[newI++] = (byte) (0xc0 | (0x1f & (c >> 6)));
                    result[newI++] = (byte) (0x80 | (0x3f & c));
                } else /* c must be < 0xffff */{
                    result[newI++] = (byte) (0xe0 | (0x0f & (c >> 12)));
                    result[newI++] = (byte) (0x80 | (0x3f & (c >> 6)));
                    result[newI++] = (byte) (0x80 | (0x3f & c));
                }
            }
        }
        return result;
    }


    public byte[] toBytes(T data) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
           version(bos);
           write(bos, data);
        } catch (IOException ex){
            throw new Error("Cannot happen!");
        }
        byte[] b = bos.toByteArray();
        return b;
    }
    
    public T fromBytes(byte[] b) throws VersionMismatch {
        ByteArrayInputStream bis = new ByteArrayInputStream(b);

        try {
            version(bis);

            T data = read(bis);
            return data;
        } catch (IOException ex) {
            throw new Error("Cannot happen!");
        }
    }
    
    public abstract void write(OutputStream o, T data) throws IOException;

    public abstract T read(InputStream i) throws IOException;

    public abstract void version(OutputStream o) throws IOException;

    public abstract void version(InputStream o) throws IOException, VersionMismatch;

    protected static void check(InputStream o, byte[] v) throws IOException, VersionMismatch {
        byte[] b = new byte[v.length];
        int n = o.read(b);
        if (n < v.length) throw new VersionMismatch("Expected " + new String(v) + ", got (short) " + new String(b,
                                                                                                                0,
                                                                                                                n));
        for (int i = 0; i < n; i++) {
            if (b[i] != v[i]) throw new VersionMismatch("Expected " + new String(v) + ", got " + new String(b, 0, n));
        }

    }

    public void writeInt(OutputStream o, int data) throws IOException {
        java.lang.String s = java.lang.String.valueOf(data);
        byte[] b1 = s.getBytes();
        o.write(b1);
        o.write(' ');
    }

    public int readInt(InputStream i) throws IOException {
        int x = i.read();
        int sum = 0;
        boolean doneg = false;
        if (x == '-') {
            doneg = true;
            x = i.read();
        }
        while (x != -1 && x != ' ') {
            if ('0' > x || x > '9') {
                throw new NumberFormatException(
                        "Read '" + (char) x + "' expecting a digit, preceding number is " + sum);
            }
            sum = sum * 10 + (x - '0');
            x = i.read();
        }

        return doneg ? -sum : sum;
    }

    public void writeLong(OutputStream o, long data) throws IOException {
        java.lang.String s = java.lang.String.valueOf(data);
        byte[] b1 = s.getBytes();
        o.write(b1);
        o.write(' ');
    }

    public long readLong(InputStream i) throws IOException {
        int x = i.read();
        long sum = 0;
        boolean doneg = false;
        if (x == '-') {
            doneg = true;
            x = i.read();
        }
        while (x != -1 && x != ' ') {
            if ('0' > x || x > '9') {
                throw new NumberFormatException(
                        "Read '" + (char) x + "' expecting a digit, preceding number is " + sum);
            }
            sum = sum * 10 + (x - '0');
            x = i.read();
        }
        return doneg ? -sum : sum;
    }

 
    static public final CheapSerializer<java.lang.String> STRING = new CheapSerializer<java.lang.String>() {

        @Override
        public java.lang.String read(InputStream i) throws IOException {
            int n = readInt(i);
            byte[] b = new byte[n];
            int m = i.read(b);
            if (m != n) throw new EOFException("input ended early");
            m = i.read();
            if (m == -1) throw new EOFException("input ended early");
            if (m != ' ') throw new RuntimeException("Expected ' ', got '" + (char) m + "'");
            return fromUtf8(b);
        }

        @Override
        public void write(OutputStream o, java.lang.String data) throws IOException {
            byte[] b2 = toUtf8(data);
            writeInt(o, b2.length);
            o.write(b2);
            o.write(' ');
        }

        byte[] V = {'S', 'T', 'R', 'I', 'N', 'G', '_', '1', '.', '0', ' '};

        @Override
        public void version(OutputStream o) throws IOException {
            o.write(V);
        }

        @Override
        public void version(InputStream o) throws IOException, VersionMismatch {
            // TODO Auto-generated method stub
            check(o, V);
        }

    };

    static public CheapSerializer<Integer> INTEGER = new CheapSerializer<Integer>() {

        byte[] V = {'I', 'N', 'T', 'E', 'G', 'E', 'R', '_', '1', '.', '0', ' '};

        @Override
        public void version(OutputStream o) throws IOException {
            o.write(V);
        }

        @Override
        public void version(InputStream o) throws IOException, VersionMismatch {
            // TODO Auto-generated method stub
            check(o, V);
        }

        @Override
        public Integer read(InputStream i) throws IOException {
            return Integer.valueOf(readInt(i));
        }

        @Override
        public void write(OutputStream o, Integer data) throws IOException {
            writeInt(o, data.intValue());
        }

    };

    static public final CheapSerializer<Long> LONG = new CheapSerializer<Long>() {

        byte[] V = {'L', 'O', 'N', 'G', '_', '1', '.', '0', ' '};

        @Override
        public void version(OutputStream o) throws IOException {
            o.write(V);
        }

        @Override
        public void version(InputStream o) throws IOException, VersionMismatch {
            // TODO Auto-generated method stub
            check(o, V);
        }

        @Override
        public Long read(InputStream i) throws IOException {
            return Long.valueOf(readLong(i));
        }

        @Override
        public void write(OutputStream o, Long data) throws IOException {
            writeLong(o, data.longValue());
        }

    };

    static public class LIST<T> extends CheapSerializer<List<T>> {
        private CheapSerializer<T> elements;
        
        public LIST(CheapSerializer<T> elements) {
           this.elements = elements;
        }

        @Override
        public List<T> read(InputStream i) throws IOException {
            int n = readInt(i);
            ArrayList<T> m = new ArrayList<T>(n);
            for (int j = 0; j < n; j++) {
                T k = elements.read(i);
                m.add(k);
            }
            return m;
        }

        byte[] V = {'L', 'I', 'S', 'T', '_', '1', '.', '0', ' '};

        @Override
        public void version(OutputStream o) throws IOException {
            o.write(V);
            elements.version(o);
        }


        @Override
        public void version(InputStream o) throws IOException, VersionMismatch {
            check(o, V);
            elements.version(o);            
        }

        @Override
        public void write(OutputStream o, List<T> data) throws IOException {
            int n = data.size();
            writeInt(o, n);
            for (T e : data) {
                elements.write(o, e);
            }   
        }
    }

    static public class PAIR<A, B> extends CheapSerializer<Pair<A, B>> {
        private CheapSerializer<A> a;
        private CheapSerializer<B> b;

        public PAIR(CheapSerializer<A> a, CheapSerializer<B> b) {
            this.a = a;
            this.b = b;
        }
        
        @Override
        public Pair<A, B> read(InputStream i) throws IOException {
            A aa = a.read(i);
            B bb = b.read(i);
            return new Pair<A,B>(aa, bb);
        }

        public void write(OutputStream o, Pair<A, B> data) throws IOException {
            a.write(o, data.getA());
            b.write(o, data.getB());
        }
        
        byte[] V = {'P', 'A', 'I', 'R', '_', '1', '.', '0', ' '};

        @Override
        public void version(OutputStream o) throws IOException {
            o.write(V);
            a.version(o);
            b.version(o);

        }

        @Override
        public void version(InputStream o) throws IOException, VersionMismatch {
            check(o, V);
            a.version(o);
            b.version(o);

        }
        
    }
    
    static public class TRIPLE<A, B, C> extends CheapSerializer<Triple<A, B, C>> {
        private CheapSerializer<A> a;
        private CheapSerializer<B> b;
        private CheapSerializer<C> c;

        public TRIPLE(CheapSerializer<A> a, CheapSerializer<B> b, CheapSerializer<C> c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }
        
        @Override
        public Triple<A, B, C> read(InputStream i) throws IOException {
            A aa = a.read(i);
            B bb = b.read(i);
            C cc = c.read(i);
            return new Triple<A,B,C>(aa, bb, cc);
        }

        public void write(OutputStream o, Triple<A, B, C> data) throws IOException {
            a.write(o, data.getA());
            b.write(o, data.getB());
            c.write(o, data.getC());
        }
        
        byte[] V = {'T', 'R', 'I', 'P', 'L', 'E', '_', '1', '.', '0', ' '};

        @Override
        public void version(OutputStream o) throws IOException {
            o.write(V);
            a.version(o);
            b.version(o);
            c.version(o);

        }

        @Override
        public void version(InputStream o) throws IOException, VersionMismatch {
            check(o, V);
            a.version(o);
            b.version(o);
            c.version(o);

        }
        
    }
    
    static public class MAP<K, V> extends CheapSerializer<Map<K, V>> {

        private CheapSerializer<K> keys;
        private CheapSerializer<V> values;

        public MAP(CheapSerializer<K> keys, CheapSerializer<V> values) {
            this.keys = keys;
            this.values = values;
        }

        protected Map<K,V> newMap(int n) {
            Map<K, V> m = new HashMap<K, V>();
            return m;
        }

        @Override
        public Map<K, V> read(InputStream i) throws IOException {
            int n = readInt(i);
            Map<K, V> m = newMap(n);
            for (int j = 0; j < n; j++) {
                K k = keys.read(i);
                V v = values.read(i);
                m.put(k, v);
            }
            return m;
        }

        @Override
        public void write(OutputStream o, Map<K, V> data) throws IOException {
            Set<Entry<K, V>> s = data.entrySet();
            int n = s.size();
            writeInt(o, n);
            for (Entry<K, V> e : s) {
                keys.write(o, e.getKey());
                values.write(o, e.getValue());
            }
        }

        byte[] V = {'M', 'A', 'P', '_', '1', '.', '0', ' '};

        @Override
        public void version(OutputStream o) throws IOException {
            o.write(V);
            keys.version(o);
            values.version(o);

        }


        @Override
        public void version(InputStream o) throws IOException, VersionMismatch {
            check(o, V);
            keys.version(o);
            values.version(o);

        }

    }


}
