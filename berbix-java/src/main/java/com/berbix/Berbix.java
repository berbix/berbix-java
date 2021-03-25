package com.berbix;

public class Berbix {

  static final String BERBIX_SDK_VERSION = "1.0-SNAPSHOT";
  private static final String DEFAULT_API_HOST = "https://api.berbix.com";

  public static BerbixClient create(BerbixOptions options) {
    return new BerbixClient(options);
  }

  public static class BerbixOptions {
    String apiSecret;
    String apiHost;

    public BerbixOptions(String apiSecret, String apiHost) {
      this.apiSecret = apiSecret;
      this.apiHost = apiHost;
    }

    public static class Builder {

      private String apiHost = DEFAULT_API_HOST;
      private String apiSecret;

      public BerbixOptions build() {
        return new BerbixOptions(apiSecret, apiHost);
      }

      public Builder apiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
        return this;
      }

      public Builder apiHost(String apiHost) {
        this.apiHost = apiHost;
        return this;
      }
    }
  }
}
