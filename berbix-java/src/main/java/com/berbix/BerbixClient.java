package com.berbix;


import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class BerbixClient implements AutoCloseable {

  private static final long CLOCK_DRIFT = 30000;
  private final BerbixApi berbixAPI;

  BerbixClient(Berbix.BerbixOptions options) {
    String apiSecret = options.apiSecret;
    String apiHost = options.apiHost;

    this.berbixAPI = new BerbixApi(apiSecret, apiHost);
  }

  public CreateTransactionResponse createTransaction(CreateTransactionRequest createTransactionRequest) throws ExecutionException, InterruptedException {
    return createTransactionAsync(createTransactionRequest).get();
  }

  public CompletableFuture<CreateTransactionResponse> createTransactionAsync(CreateTransactionRequest createTransactionRequest) {
    return berbixAPI.createTransactionAsync(createTransactionRequest);
  }

  public CreateHostedTransactionResponse createHostedTransaction(CreateHostedTransactionRequest createHostedTransactionRequest) throws ExecutionException, InterruptedException {
    return createHostedTransactionAsync(createHostedTransactionRequest).get();
  }

  public CompletableFuture<CreateHostedTransactionResponse> createHostedTransactionAsync(CreateHostedTransactionRequest createHostedTransactionRequest) {
    return berbixAPI.createHostedTransactionAsync(createHostedTransactionRequest);
  }

  public Transaction fetchTransaction(Tokens tokens) throws ExecutionException, InterruptedException {
    return fetchTransactionAsync(tokens).get();
  }

  public CompletableFuture<Transaction> fetchTransactionAsync(Tokens tokens) {
    return berbixAPI.fetchTransactionAsync(tokens);
  }

  public Tokens refreshTokens(Tokens tokens) throws ExecutionException, InterruptedException {
    return refreshTokensAsync(tokens).get();
  }

  public CompletableFuture<Tokens> refreshTokensAsync(Tokens tokens) {
    return berbixAPI.refreshTokensAsync(tokens);
  }

  public Boolean overrideTransaction(Tokens tokens, OverrideTransactionRequest request) throws ExecutionException, InterruptedException {
    return overrideTransactionAsync(tokens, request).get();
  }

  public CompletableFuture<Boolean> overrideTransactionAsync(Tokens tokens, OverrideTransactionRequest request) {
    return berbixAPI.overrideTransactionAsync(tokens, request);
  }

  public Transaction updateTransaction(Tokens tokens, UpdateTransactionRequest request) throws ExecutionException, InterruptedException {
    return updateTransactionAsync(tokens, request).get();
  }

  public CompletableFuture<Transaction> updateTransactionAsync(Tokens tokens, UpdateTransactionRequest request) {
    return berbixAPI.updateTransactionAsync(tokens, request);
  }

  public Boolean deleteTransaction(Tokens tokens) throws ExecutionException, InterruptedException {
    return deleteTransactionAsync(tokens).get();
  }

  public CompletableFuture<Boolean> deleteTransactionAsync(Tokens tokens) {
    return berbixAPI.deleteTransactionAsync(tokens);
  }

  public boolean validateSignature(String secret, String body, String header) {
    String[] parts = header.split(",");

    if (parts.length != 3) {
      return false;
    }

    // Version (parts[0]) is currently unused
    String timestamp = parts[1];
    String signature = parts[2];

    if (Long.parseLong(timestamp) < System.currentTimeMillis() / 1000 - CLOCK_DRIFT) {
      return false;
    }
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.US_ASCII), "HmacSHA256");
      mac.init(secretKeySpec);
      byte[] hexDigest = mac.doFinal((timestamp + "," + secret + "," + body).getBytes(StandardCharsets.US_ASCII));
      return DatatypeConverter.printHexBinary(hexDigest).toLowerCase(Locale.ROOT).equals(signature);
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      return false;
    }
  }

  @Override
  public void close() {
    berbixAPI.shutdown();
  }
}
