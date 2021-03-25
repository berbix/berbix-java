package com.berbix;

import java.util.List;
import java.util.Map;

public class OverrideTransactionRequest {
  public ResponsePayload responsePayload;
  public List<String> flags;
  public Map<String, String> overrideFields;

  public enum ResponsePayload {
    US_DL("us-dl"),
    US_ID("us-id"),
    PASSPORT("us-passport");

    final String value;

    ResponsePayload(String value) {
      this.value = value;
    }
  }
}
