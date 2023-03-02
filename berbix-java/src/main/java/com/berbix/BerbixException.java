package com.berbix;

public class BerbixException extends RuntimeException {

  public BerbixException(String message) {
    super(message);
  }

  public BerbixException(String message, Throwable e) {
    super(message, e);
  }
}
