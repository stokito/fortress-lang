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

package com.sun.fortress.interpreter.useful;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
 * Created on Feb 3, 2006
 *
 */

public class Useful {

    static String localMilliIso8601Format = "yyyy-MM-dd'T'HH:mm:ss.SSSzzz";

    static public DateFormat localMilliDateFormat = new SimpleDateFormat(
            localMilliIso8601Format);

    public static String localNow(java.util.Date d) {
        return localMilliDateFormat.format(d);
    }

    public static String timeStamp() {
        return localNow(new java.util.Date());
    }

    public static <T> String listInParens(Collection<T> l) {
        return listInDelimiters("(", l, ")");
    }

    public static <T> String listInCurlies(Collection<T> l) {
        return listInDelimiters("{", l, "}");
    }

    public static <T> String listInDelimiters(String left, Collection<T> l,
            String right) {
        StringBuffer sb = new StringBuffer();
        sb.append(left);
        boolean first = true;
        for (T x : l) {
            if (first)
                first = false;
            else
                sb.append(",");
            sb.append(String.valueOf(x));
        }
        sb.append(right);
        return sb.toString();
    }

    public static String coordInDelimiters(String left, int[] l, int hi,
            String right) {
        return coordInDelimiters(left, l, 0, hi, right);
    }

    public static String coordInDelimiters(String left, int[] l, String right) {
        return coordInDelimiters(left, l, 0, l.length, right);
    }

    public static String coordInDelimiters(String left, int[] l, int lo,
            int hi, String right) {
        StringBuffer sb = new StringBuffer();
        sb.append(left);
        boolean first = true;
        for (int i = lo; i < hi; i++) {
            if (first)
                first = false;
            else
                sb.append(",");
            sb.append(String.valueOf(l[i]));
        }
        sb.append(right);
        return sb.toString();
    }

    public static <T, U> String listsInParens(List<T> l1, List<U> l2) {
        StringBuffer sb = new StringBuffer();
        sb.append("(");
        boolean first = true;
        for (T x : l1) {
            if (first)
                first = false;
            else
                sb.append(",");
            sb.append(String.valueOf(x));
        }
        for (U x : l2) {
            if (first)
                first = false;
            else
                sb.append(",");
            sb.append(String.valueOf(x));
        }
        sb.append(")");
        return sb.toString();
    }

    public static <T> String dottedList(List<T> l) {
        if (l.size() == 1) /* Itty-bitty performance hack */
            return String.valueOf(l.get(0));
        StringBuffer sb = new StringBuffer();
        boolean first = true;
        for (T x : l) {
            if (first)
                first = false;
            else
                sb.append(".");
            sb.append(String.valueOf(x));
        }
        return sb.toString();
    }

    public static String backtrace(int start, int count) {
        StackTraceElement[] trace = (new Throwable()).getStackTrace();
        StringBuffer sb = new StringBuffer();
        for (int i = start; i < start + count && i < trace.length; i++) {
            if (i > start)
                sb.append("\n");
            sb.append(String.valueOf(trace[i]));
        }
        return sb.toString();
    }

    public static <T> String listInOxfords(List<T> l) {
        return listInDelimiters("[\\", l, "\\]");
    }

    public static String inOxfords(String string1, String string2,
            String string3) {

        return "[\\" + string1 + ", " + string2 + ", " + string3 + "\\]";
    }

    public static String inOxfords(String string1, String string2) {

        return "[\\" + string1 + ", " + string2 + "\\]";
    }

    public static String inOxfords(String string1) {

        return "[\\" + string1 + "\\]";
    }

    public static <T> Set<List<T>> setProduct(Set<List<T>> s1, Set<T> s2) {
        HashSet<List<T>> result = new HashSet<List<T>>();

        for (List<T> list : s1) {
            for (T elt : s2) {
                ArrayList<T> java_is_not_a_functional_language = new ArrayList<T>(
                        list);
                java_is_not_a_functional_language.add(elt);
                result.add(java_is_not_a_functional_language);
            }
        }
        return result;
    }

    public static <T, U, V> Set<V> setProduct(Set<T> t, Set<U> u,
            Fn2<T, U, V> product) {
        HashSet<V> result = new HashSet<V>();
        for (T i : t)
            for (U j : u)
                result.add(product.apply(i, j));
        return result;
    }

    public static <T, U> Set<U> applyToAll(Set<T> s, Fn<T, U> verb) {
        HashSet<U> result = new HashSet<U>();
        for (T i : s)
            result.add(verb.apply(i));
        return result;

    }

    public static <T, U> List<U> applyToAll(List<T> s, Fn<T, U> verb) {
        ArrayList<U> result = new ArrayList<U>();
        for (T i : s)
            result.add(verb.apply(i));
        return result;

    }

    public static <T> Set<T> set(Iterable<T> xs) {
      HashSet<T> result = new HashSet<T>();
      
      for (T x : xs) {
          if (x != null) result.add(x);
      }
      return result;
    }

    public static <T> Set<T> set() {
        return Collections.emptySet();
      }

    public static <T> Set<T> set(T x1) {
        HashSet<T> result = new HashSet<T>();
        result.add(x1);
        return result;
      }

    public static <T> Set<T> set(T x1, T x2) {
        HashSet<T> result = new HashSet<T>();
        result.add(x1);
        result.add(x2);
         return result;
      }

    public static <T> Set<T> set(T x1, T x2, T x3) {
        HashSet<T> result = new HashSet<T>();
        result.add(x1);
        result.add(x2);
        result.add(x3);
        return result;
      }


    
    /* Union, treating null as empty. */
    public static <T> Set<T> union(Iterable<? extends Collection<T>>  xs) {
        HashSet<T> result = new HashSet<T>();
        for (Collection<T> x : xs) {
            if (x != null) result.addAll(x);
        }
        return result;
    }

    public static <T> Set<T> union(Collection<T> x1, Collection<T> x2, Collection<T> x3, Collection<T> x4) {
        HashSet<T> result = new HashSet<T>();
        result.addAll(x1);
        result.addAll(x2);
        result.addAll(x3);
        result.addAll(x4);
        return result;
    }

    public static <T> Set<T> union(Collection<T> x1, Collection<T> x2, Collection<T> x3) {
        HashSet<T> result = new HashSet<T>();
        result.addAll(x1);
        result.addAll(x2);
        result.addAll(x3);
        return result;
    }

    public static <T> Set<T> union(Collection<T> x1, Collection<T> x2) {
        HashSet<T> result = new HashSet<T>();
        result.addAll(x1);
        result.addAll(x2);
        return result;
    }

    // Don't support singleton or empty unions -- singletons lead to type
    // signature clashes with the varargs case.
    
    public static <T> T clampedGet(List<T> l, int j) {
        int s = l.size();
        return j < s ? l.get(j) : l.get(s - 1);
    }

    public static <T> List<T> list(Iterable<T> xs) {
        ArrayList<T> result;
        if (xs instanceof Collection<?>) {
            result = new ArrayList<T>(((Collection<?>)xs).size());
        } else {
            result = new ArrayList<T>();
        }
        for (T x : xs) {
            result.add(x);
        }
        return result;
      }

    public static <T> List<T> list(T x1, T x2, T x3, T x4) {
        ArrayList<T> result = new ArrayList<T>(4);
        result.add(x1);
        result.add(x2);
        result.add(x3);
        result.add(x4);
        return result;
      }

    public static <T> List<T> list(T x1, T x2, T x3) {
        ArrayList<T> result = new ArrayList<T>(3);
        result.add(x1);
        result.add(x2);
        result.add(x3);
        return result;
      }

    public static <T> List<T> list(T x1, T x2) {
        ArrayList<T> result = new ArrayList<T>(2);
        result.add(x1);
        result.add(x2);
        return result;
      }

    public static <T> List<T> list(T x1) {
        ArrayList<T> result = new ArrayList<T>(1);
        result.add(x1);
        return result;
      }

    public static <T> List<T> list() {
        return Collections.emptyList();
      }

      public static <T> List<T> prepend(T x, List<T> y) {
        ArrayList<T> result = new ArrayList<T>(1 + y.size());
        result.add(x);
        result.addAll(y);
        return result;
    }

    public static <T> List<T> removeIndex(int i, List<T> y) {
        int l = y.size();
        if (i == 0) return y.subList(1,l);
        if (i == l-1) return y.subList(0,l-1);
        ArrayList<T> result = new ArrayList<T>(y.size()-1);
        result.addAll(y.subList(0, i));
        result.addAll(y.subList(i+1, l));
        return result;
    }

    public static <T,U> List<U> prependMapped(T x, List<T> y, Fn<T,U> f) {
        ArrayList<U> result = new ArrayList<U>(1 + y.size());
        result.add(f.apply(x));
        for (T t : y) result.add(f.apply(t));
        return result;
    }

    public static <T> List<T> concat(Iterable<Collection<T>> xs) {
        ArrayList<T> result = new ArrayList<T>();
        
        for (Collection<T> x : xs ) {
            result.addAll(x);
        }
        result.trimToSize();
        return result;
    }

    public static <T> List<T> concat(Collection<T> x1, Collection<T>x2) {
        ArrayList<T> result = new ArrayList<T>();
        result.addAll(x1);
        result.addAll(x2);
        result.trimToSize();
        return result;
    }

    public static <T> List<T> concat(Collection<T> x1) {
        ArrayList<T> result = new ArrayList<T>(x1);
       return result;
    }

    public static <T> List<T> concat() {
        ArrayList<T> result = new ArrayList<T>();
        return result;
    }

    public static <T> T singleValue(Set<T> s) {
        int si = s.size();
        if (si == 0)
            throw new Error("Empty set where singleton expected");
        if (si > 1)
            throw new Error("Multiple-element set where singleton expected");
        for (T e : s)
            return e;

        return NI.<T> np();
    }

    public static <T extends Comparable<T>, U extends Comparable<U> > int compare(T a,
            T b, U c, U d) {
        int x = a.compareTo(b);
        if (x != 0)
            return x;
        return c.compareTo(d);
    }

    public static <T extends Comparable<T>, U extends Comparable<U>, V extends Comparable<V> > int compare(
            T a, T b, U c, U d, V e, V f) {
        int x = a.compareTo(b);
        if (x != 0)
            return x;
        x = c.compareTo(d);
        if (x != 0)
            return x;
        return e.compareTo(f);
    }

    public static <T extends Comparable<T>, U extends Comparable<U>, V extends Comparable<V>, W extends Comparable<W>> int compare(
            T a, T b, U c, U d, V e, V f, W g, W h) {
        int x = a.compareTo(b);
        if (x != 0)
            return x;
        x = c.compareTo(d);
        if (x != 0)
            return x;
        x = e.compareTo(f);
        if (x != 0)
            return x;
        return g.compareTo(h);
    }

    public static <T> int compare(T a, T b, Comparator<T> c1, Comparator<T> c2) {
        int x = c1.compare(a, b);
        if (x != 0)
            return x;
        return c2.compare(a, b);
    }

    public static <T> int compare(T a, T b, Comparator<T> c1, Comparator<T> c2,
            Comparator<T> c3) {
        int x = c1.compare(a, b);
        if (x != 0)
            return x;
        x = c2.compare(a, b);
        if (x != 0)
            return x;
        return c3.compare(a, b);
    }

    public static String extractAfterMatch(String input, String to_match)
            throws NotFound {
        int i = input.indexOf(to_match);
        if (i == -1)
            throw new NotFound();
        return input.substring(i + to_match.length());
    }

    public static String extractBeforeMatch(String input, String to_match)
            throws NotFound {
        int i = input.indexOf(to_match);
        if (i == -1)
            throw new NotFound();
        return input.substring(0, i);
    }

    public static String extractBetweenMatch(String input, String before,
            String after) {
        int b = input.indexOf(before);
        if (b == -1)
            return null;
        input = input.substring(b + before.length());
        if (after == null)
            return input;
        int a = input.indexOf(after);
        if (a == -1)
            return null;
        return input.substring(0, a);
    }

    public static String replace(String original, String search, String replace) {
        int searchLength = search.length();
        // One way of dealing with the empty string
        if (searchLength == 0)
            throw new IllegalArgumentException("Cannot replace empty string");
        // arbitrary guess at the new size. Assume zero or one replacements
        // ignore the pathological search == "" case.
        StringBuffer sb = new StringBuffer(original.length()
                + Math.max(0, replace.length() - search.length()));
        int start = 0;
        int at = original.indexOf(search);
        while (at != -1) {
            sb.append(original.substring(start, at)); // copy up to 'search'
            sb.append(replace); // insert 'replacement'
            start = at + searchLength; // skip 'search' in original
            at = original.indexOf(search, start);
        }
        if (start == 0)
            return original;
        sb.append(original.substring(start));
        return sb.toString();
    }

    /**
     * The substring function Sun should have defined. Negative numbers are
     * end-relative, longer-than-the-end is equivalent to the end, crossed
     * indices result in an empty string.
     */
    public static String substring(String s, int start, int end) {
        int l = s.length();
        if (start > l)
            start = l;
        if (end > l)
            end = l;
        if (start < 0)
            start = l + start;
        if (end < 0)
            end = l + end;
        if (start < 0)
            start = 0;
        if (end < 0)
            end = 0;
        if (start > end)
            start = end;
        return s.substring(start, end);
    }

    public static int commonPrefixLength(String s1, String s2) {
        int i = 0;
        while (i < s1.length() && i < s2.length()
                && s1.charAt(i) == s2.charAt(i))
            i++;
        return i;
    }

    public static int commonPrefixLengthCI(String s1, String s2) {
        int i = 0;
        while (i < s1.length()
                && i < s2.length()
                && Character.toUpperCase(s1.charAt(i)) == Character
                        .toUpperCase(s2.charAt(i)))
            i++;
        return i;
    }

    /**
     * @throws FileNotFoundException
     */
    public static BufferedReader filenameToBufferedReader(String filename) throws FileNotFoundException {
        return new BufferedReader(
                new FileReader(filename));
    }

    public static BufferedWriter filenameToBufferedWriter(String filename) throws IOException {
        return new BufferedWriter(
                new FileWriter(filename));
    }

    /**
     * Returns a BufferedReader for the file named s, with encoding assumed to be UTF-8.
     * @throws FileNotFoundException
     */
    static public BufferedReader utf8BufferedFileReader(String s) throws FileNotFoundException {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(s), Charset.forName("UTF-8")));
        return br;
    }

    /**
     * Returns a BufferedWriter for the file named s, with encoding assumed to be UTF-8.
     * @throws FileNotFoundException
     */
    static public BufferedWriter utf8BufferedFileWriter(String s) throws FileNotFoundException {
        BufferedWriter br = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(s), Charset.forName("UTF-8")));
        return br;
    }

    static public boolean olderThanOrMissing(String resultFile, String inputFile) {
        File res = new File(resultFile);
        File inp = new File(inputFile);
        // res does not exist, OR res has a smaller birthday.
        return !res.exists() || (res.lastModified() < inp.lastModified());
    }

    public static int compareClasses(Object x, Object y) {
        Class<? extends Object> a = x.getClass();
        Class<? extends Object> b = y.getClass();
        if (a == b) return 0;
        if (a.isAssignableFrom(b)) return -1;
        if (b.isAssignableFrom(a)) return 1;
        return a.getName().compareTo(b.getName());
    }

}
