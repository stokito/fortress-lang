package com.sun.fortress.interpreter.nodes;

/**
* An extension of PrintWriter to support indenting levels.
*/
public class TabPrintWriter extends java.io.PrintWriter {
  private final int _tabSize;
  private final java.lang.String _tabString;
  private int _numIndents = 0;
  
  public TabPrintWriter(java.io.Writer writer, int tabSize) {
    super(writer);
    char[] c = new char[tabSize];
    for(int i = 0; i < tabSize; i++) {
      c[i] = ' ';
    }
    _tabString = new java.lang.String(c);
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
      print(_tabString);
    }
  }
}
