package com.berbix;

import java.io.IOException;

public class BerbixException extends RuntimeException {

  public BerbixException(String message, Exception e) {
    super(message, e);
  }
}
