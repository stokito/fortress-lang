package com.sun.fortress.shell;

public class ShellException extends RuntimeException {
   public ShellException(Exception e) {
      super(e.getMessage());
   }
}