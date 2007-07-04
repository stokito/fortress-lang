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


/**
 * An extension of PrintWriter to support indenting levels.
 */
public class TabPrintWriter extends java.io.PrintWriter {
  private int _tabSize;
  private int _numIndents = 0;

  public TabPrintWriter(java.io.Writer writer, int tabSize) {
    super(writer);
    _tabSize = tabSize;
  }

  /** ups indent for any future new lines. */
  public void indent() { _numIndents++; }
  public void unindent() { _numIndents--; }

  public void startLine(java.lang.Object s) {
    startLine();
    print(s);
  }

  public void startLine() {
    println();
    for (int i = 0; i < _numIndents; i++) {
      for (int j = 0; j < _tabSize; j++) {
        print(" ");
      }
    }
  }

  public void printEscaped(java.lang.Object o) { printEscaped(o.toString()); }

  /** Print a string in Java source-compatible escaped form.  All control characters
    * (including line breaks) and quoting punctuation are escaped with a backslash.
    */
  public void printEscaped(java.lang.String s) {
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      switch (c) {
        case '\b': print("\\b"); break;
        case '\t': print("\\t"); break;
        case '\n': print("\\n"); break;
        case '\f': print("\\f"); break;
        case '\r': print("\\r"); break;
        case '\"': print("\\\""); break;
        case '\'': print("\\\'"); break;
        case '\\': print("\\\\"); break;
        default:
          if (c < ' ' || c == '\u007f') {
            print('\\');
            // must use 3 digits so that unescaping doesn't consume too many chars ("\12" vs. "\0012")
            java.lang.String num = java.lang.Integer.toOctalString(c);
            while (num.length() < 3) num = "0" + num;
            print(num);
          }
          else { print(c); }
          break;
      }
    }
  }

  /** Print the serialized form of the given object as a hexadecimal number.
    * @throws RuntimeException  If the object is not serializable.
    */
  public void printSerialized(java.lang.Object o) {
    java.io.ByteArrayOutputStream bs = new java.io.ByteArrayOutputStream();
    try {
      java.io.ObjectOutputStream objOut = new java.io.ObjectOutputStream(bs);
      try { objOut.writeObject(o); }
      finally { objOut.close(); }
    }
    catch (java.io.IOException e) { throw new java.lang.RuntimeException(e); }
    printBytes(bs.toByteArray());
  }

  /** Print the serialized form of the given float as a hexadecimal number. */
  public void printSerialized(float f) {
    java.io.ByteArrayOutputStream bs = new java.io.ByteArrayOutputStream();
    try {
      java.io.ObjectOutputStream objOut = new java.io.ObjectOutputStream(bs);
      try { objOut.writeFloat(f); }
      finally { objOut.close(); }
    }
    catch (java.io.IOException e) { throw new java.lang.RuntimeException(e); }
    printBytes(bs.toByteArray());
  }

  /** Print the serialized form of the given float as a hexadecimal number. */
  public void printSerialized(double d) {
    java.io.ByteArrayOutputStream bs = new java.io.ByteArrayOutputStream();
    try {
      java.io.ObjectOutputStream objOut = new java.io.ObjectOutputStream(bs);
      try { objOut.writeDouble(d); }
      finally { objOut.close(); }
    }
    catch (java.io.IOException e) { throw new java.lang.RuntimeException(e); }
    printBytes(bs.toByteArray());
  }

  private void printBytes(byte[] bs) {
    for (byte b : bs) {
      int unsigned = b;
      if (unsigned < 0) unsigned += 256; // we need to map -1 to 255, -128 to 128
      java.lang.String num = java.lang.Integer.toHexString(unsigned);
      if (num.length() == 1) num = "0" + num;
      print(num);
    }
  }

}
