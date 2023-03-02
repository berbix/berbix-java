package com.berbix;

public class CreateHostedTransactionRequest {
  public String customerUid;
  public String templateKey;
  public String phone;
  public String email;
  public boolean consentsToAutomatedFacialRecognition;
  public HostedOptions hostedOptions = new HostedOptions();

  public static class HostedOptions {
    public String completionEmail;
    public String redirectUrl;
  }
}
