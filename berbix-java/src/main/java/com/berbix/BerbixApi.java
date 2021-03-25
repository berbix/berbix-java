package com.berbix;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.concurrent.*;

public class BerbixApi {

  private static final int REQUEST_TIMEOUT_MILLIS = 30000;
  private static final int MIN_API_THREADS = 4;
  private final String apiSecret;
  private final String apiHost;
  private final HttpClient jettyClient;

  private final ObjectMapper objectMapper;

  private final ExecutorService executorService;

  public BerbixApi(String apiSecret, String apiHost) {
    this.apiSecret = apiSecret;
    this.apiHost = apiHost;
    this.objectMapper = new ObjectMapper();
    objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    objectMapper.registerModule(new JavaTimeModule());
    SimpleModule enumModule = new SimpleModule();
    enumModule.addSerializer(OverrideTransactionRequest.ResponsePayload.class, new StdSerializer<OverrideTransactionRequest.ResponsePayload>(OverrideTransactionRequest.ResponsePayload.class) {
      @Override
      public void serialize(OverrideTransactionRequest.ResponsePayload value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeString(value.value);
      }
    });
    objectMapper.registerModule(enumModule);

    SslContextFactory.Client sslContextFactory = new SslContextFactory.Client();
    jettyClient = new HttpClient(sslContextFactory);

    executorService = new ThreadPoolExecutor(
        0,
        Math.max(MIN_API_THREADS, Runtime.getRuntime().availableProcessors() / 2),
        60L,
        TimeUnit.SECONDS,
        new SynchronousQueue<>());

    try {
      jettyClient.start();
      Thread shutdown = new Thread(() -> {
        try {
          jettyClient.stop();
          executorService.shutdown();
        } catch(Exception e){
          // ignore
        }
      });
      Runtime.getRuntime().addShutdownHook(shutdown);
    } catch(Exception e) {
      // ignore
    }
  }

  void shutdown() {
    try {
      jettyClient.stop();
      if (executorService != null) {
        executorService.shutdown();
      }
    } catch(Exception e){
      // ignore
    }
  }

  public CompletableFuture<CreateTransactionResponse> createTransactionAsync(CreateTransactionRequest createTransactionRequest) {
    return CompletableFuture.supplyAsync(() -> createTransaction(createTransactionRequest), executorService);
  }

  private CreateTransactionResponse createTransaction(CreateTransactionRequest createTransactionRequest) {
    try {
      FetchTokensResponse fetchTokensResponse = fetchTokens("/v0/transactions", createTransactionRequest);

      Tokens tokens = createTokens(fetchTokensResponse);

      CreateTransactionResponse response = new CreateTransactionResponse();
      response.tokens = tokens;
      return response;
    } catch (Exception e) {
      e.printStackTrace();
      throw new BerbixException("Unable to create transaction", e);
    }
  }

  public CompletableFuture<CreateHostedTransactionResponse> createHostedTransactionAsync(CreateHostedTransactionRequest createHostedTransactionRequest) {
    return CompletableFuture.supplyAsync(() -> createHostedTransaction(createHostedTransactionRequest), executorService);
  }

  public CreateHostedTransactionResponse createHostedTransaction(CreateHostedTransactionRequest createHostedTransactionRequest) {
    try {
      FetchTokensResponse fetchTokensResponse = fetchTokens("/v0/transactions", createHostedTransactionRequest);

      Tokens tokens = createTokens(fetchTokensResponse);

      CreateHostedTransactionResponse response = new CreateHostedTransactionResponse();
      response.tokens = tokens;
      response.hostedUrl = fetchTokensResponse.hostedUrl;
      return response;
    } catch (Exception e) {
      // e.printStackTrace();
      throw new BerbixException("Unable to create hosted transaction", e);
    }
  }

  private Tokens createTokens(FetchTokensResponse fetchTokensResponse) {
    Tokens tokens = new Tokens();
    tokens.accessToken = fetchTokensResponse.accessToken;
    tokens.clientToken = fetchTokensResponse.clientToken;
    tokens.refreshToken = fetchTokensResponse.refreshToken;
    tokens.transactionId = fetchTokensResponse.transactionId;
    tokens.expiresAt = ZonedDateTime.now(ZoneId.of("UTC")).plus(fetchTokensResponse.expiresIn, ChronoUnit.SECONDS);
    tokens.responseJsonString = fetchTokensResponse.responseJsonString;
    return tokens;
  }

  private FetchTokensResponse fetchTokens(String path, Object payload) throws IOException {
    Request request = jettyClient.newRequest(apiHost + path);
    request.method(HttpMethod.POST);
    request.header("Authorization", "Basic " + Base64.getEncoder().encodeToString((apiSecret + ":").getBytes(StandardCharsets.UTF_8)));
    request.header("Content-Type", "application/json");
    request.header("Accept", "application/json");
    request.header("User-Agent", "BerbixJava/" + Berbix.BERBIX_SDK_VERSION);

    BytesContentProvider bytesRequestContent = new BytesContentProvider("application/json",
        objectMapper.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8));
    request.content(bytesRequestContent, "application/json");

    ContentResponse contentResponse;
    try {
      request.timeout(REQUEST_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
      contentResponse = request.send();
      if (contentResponse.getStatus() == HttpStatus.OK_200) {
        String response = contentResponse.getContentAsString();

        // uncomment to see raw response
        // System.out.println("response:" + response);

        FetchTokensResponse fetchTokensResponse = objectMapper.readValue(response, FetchTokensResponse.class);
        fetchTokensResponse.responseJsonString = response;

        return fetchTokensResponse;
      } else {
        String response = contentResponse.getContentAsString();
        throw new IOException(response);
      }
    } catch (InterruptedException | TimeoutException | ExecutionException e) {
      // e.printStackTrace();
      throw new IOException(e.getMessage());
    }
  }

  public CompletableFuture<Transaction> fetchTransactionAsync(Tokens tokens) {
    return CompletableFuture.supplyAsync(() -> fetchTransaction(tokens), executorService);
  }

  private Transaction fetchTransaction(Tokens tokens) {
    try {
      String transactionResponse = tokenAuthRequest("GET", tokens, "/v0/transactions", null);
      System.out.println(transactionResponse);
      return objectMapper.readValue(transactionResponse, Transaction.class);
    } catch (IOException e) {
      //e.printStackTrace();
      throw new BerbixException("Unable to fetch transaction", e);
    }
  }

  private String tokenAuthRequest(String method, Tokens tokens, String path, Object payload) throws IOException {
    refreshIfNecessary(tokens);
    Request request = jettyClient.newRequest(apiHost + path);
    request.method(method);
    request.header("Authorization", "Bearer " + tokens.accessToken);
    request.header("Content-Type", "application/json");
    request.header("Accept", "application/json");
    request.header("User-Agent", "BerbixJava/" + Berbix.BERBIX_SDK_VERSION);

    if (payload != null) {
      BytesContentProvider bytesRequestContent = new BytesContentProvider("application/json",
          objectMapper.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8));
      request.content(bytesRequestContent, "application/json");
    }

    ContentResponse contentResponse;
    try {
      request.timeout(REQUEST_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
      contentResponse = request.send();
      if (contentResponse.getStatus() == HttpStatus.OK_200) {

        // uncomment to see raw response
        // System.out.println("response:" + response);

        return contentResponse.getContentAsString();
      } else if (contentResponse.getStatus() == 204) {
        return "finished";
      } else {
        String response = contentResponse.getContentAsString();
        throw new IOException(response);
      }
    } catch (InterruptedException | TimeoutException | ExecutionException e) {
      // e.printStackTrace();
      throw new IOException(e.getMessage());
    }
  }

  private void refreshIfNecessary(Tokens tokens) throws IOException {
    if (tokens.needsRefresh()) {
      Tokens newTokens = refreshTokens(tokens);
      tokens.refresh(newTokens);
    }

  }

  private Tokens refreshTokens(Tokens tokens) throws IOException {
    RefreshTokenRequest request = new RefreshTokenRequest();
    request.refreshToken = tokens.refreshToken;
    request.grantType = "refresh_token";
    return createTokens(fetchTokens("/v0/tokens", request));
  }

  public CompletableFuture<Tokens> refreshTokensAsync(Tokens tokens) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        return refreshTokens(tokens);
      } catch (Exception e) {
        // e.printStackTrace();
        throw new BerbixException("Unable to refresh tokens", e);
      }
    }, executorService);
  }

  public CompletableFuture<Boolean> overrideTransactionAsync(Tokens tokens, OverrideTransactionRequest request) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        tokenAuthRequest("PATCH", tokens, "/v0/transactions/override", request);
        return true;
      } catch (Exception e) {
        // e.printStackTrace();
        throw new BerbixException("Unable to override transaction", e);
      }
    }, executorService);
  }

  public CompletableFuture<Transaction> updateTransactionAsync(Tokens tokens, UpdateTransactionRequest request) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        String transactionResponse = tokenAuthRequest("PATCH", tokens, "/v0/transactions", request);
        return objectMapper.readValue(transactionResponse, Transaction.class);
      } catch (Exception e) {
        // e.printStackTrace();
        throw new BerbixException("Unable to update transaction", e);
      }
    }, executorService);
  }

  public CompletableFuture<Boolean> deleteTransactionAsync(Tokens tokens) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        tokenAuthRequest("DELETE", tokens, "/v0/transactions", null);
        return true;
      } catch (Exception e) {
        // e.printStackTrace();
        throw new BerbixException("Unable to delete transaction", e);
      }
    }, executorService);
  }
}
