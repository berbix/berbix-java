package com.berbix;

import com.fasterxml.jackson.annotation.JsonProperty;

class FetchTokensResponse {
  @JsonProperty("access_token")
  public String accessToken;

  @JsonProperty("client_token")
  public String clientToken;

  @JsonProperty("refresh_token")
  public String refreshToken;

  @JsonProperty("transaction_id")
  public Long transactionId;

  @JsonProperty("expires_in")
  public Long expiresIn;

  @JsonProperty("hosted_url")
  public String hostedUrl;

  public String responseJsonString;
}
