package com.berbix;

import java.time.ZonedDateTime;

public class Tokens {
  public String accessToken;

  public String clientToken;

  public String refreshToken;

  public Long transactionId;

  public ZonedDateTime expiresAt;

  public String responseJsonString;

  public boolean needsRefresh() {
    return expiresAt == null || expiresAt.isBefore(ZonedDateTime.now());
  }

  public void refresh(Tokens newTokens) {
    accessToken = newTokens.accessToken;
    clientToken = newTokens.clientToken;
    transactionId = newTokens.transactionId;
    expiresAt = newTokens.expiresAt;
  }

  public static Tokens fromRefresh(String refreshToken) {
    Tokens tokens = new Tokens();
    tokens.refreshToken = refreshToken;

    return tokens;
  }
}
