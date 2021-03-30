package com.berbix;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpContentResponse;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.BufferingResponseListener;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class BerbixApi {

  private static final int REQUEST_TIMEOUT_MILLIS = 30000;
  private final String apiSecret;
  private final String apiHost;
  private final HttpClient jettyClient;

  private final ObjectMapper objectMapper;

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

    try {
      jettyClient.start();
      Thread shutdown = new Thread(() -> {
        try {
          jettyClient.stop();
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
    } catch(Exception e){
      // ignore
    }
  }

  public CompletableFuture<CreateTransactionResponse> createTransactionAsync(CreateTransactionRequest createTransactionRequest) {
    return fetchTokens("/v0/transactions", createTransactionRequest)
        .thenApply(fetchTokensResponse -> {
          Tokens tokens = createTokens(fetchTokensResponse);

          CreateTransactionResponse response = new CreateTransactionResponse();
          response.tokens = tokens;
          return response;
        })
        .handle((result, ex) -> {
          if (ex != null) {
            throw new BerbixException("Unable to create transaction", ex);
          }

          return result;
        });
  }

  public CompletableFuture<CreateHostedTransactionResponse> createHostedTransactionAsync(CreateHostedTransactionRequest createHostedTransactionRequest) {
    return fetchTokens("/v0/transactions", createHostedTransactionRequest)
        .thenApply(fetchTokensResponse -> {
          Tokens tokens = createTokens(fetchTokensResponse);

          CreateHostedTransactionResponse response = new CreateHostedTransactionResponse();
          response.tokens = tokens;
          response.hostedUrl = fetchTokensResponse.hostedUrl;
          return response;
        })
        .handle((result, ex) -> {
          if (ex != null) {
            throw new BerbixException("Unable to create hosted transaction", ex);
          }

          return result;
        });
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

  private CompletableFuture<FetchTokensResponse> fetchTokens(String path, Object payload) {
    Request request = jettyClient.newRequest(apiHost + path);
    request.method(HttpMethod.POST);
    request.header("Authorization", "Basic " + Base64.getEncoder().encodeToString((apiSecret + ":").getBytes(StandardCharsets.UTF_8)));
    request.header("Content-Type", "application/json");
    request.header("Accept", "application/json");
    request.header("User-Agent", "BerbixJava/" + Berbix.BERBIX_SDK_VERSION);

    BytesContentProvider bytesRequestContent;
    try {
      bytesRequestContent = new BytesContentProvider("application/json",
          objectMapper.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8));
    } catch (JsonProcessingException e) {
      throw new BerbixException("Unable to create transaction", e);
    }
    request.content(bytesRequestContent, "application/json");

    CompletableFuture<ContentResponse> completableFuture = new CompletableFuture<>();
    request.timeout(REQUEST_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    request.send(new CompletableFutureResponseListener(completableFuture));

    return completableFuture.thenApply(response -> {
      String apiResponseData = response.getContentAsString();

      // uncomment to see raw response
      // System.out.println("response:" + apiResponseData);

      FetchTokensResponse fetchTokensResponse = null;
      try {
        fetchTokensResponse = objectMapper.readValue(apiResponseData, FetchTokensResponse.class);
        fetchTokensResponse.responseJsonString = apiResponseData;
      } catch (JsonProcessingException e) {
        completableFuture.completeExceptionally(e);
      }

      return fetchTokensResponse;
    });
  }

  public CompletableFuture<Transaction> fetchTransactionAsync(Tokens tokens) {
    try {
      return tokenAuthRequest("GET", tokens, "/v0/transactions", null, Transaction.class)
          .handle((result, ex) -> {
            if (ex != null) {
              throw new BerbixException("Unable to fetch transaction", ex);
            }

            return result;
          });
    } catch (IOException e) {
      CompletableFuture<Transaction> completableFuture = new CompletableFuture<>();
      completableFuture.completeExceptionally(new BerbixException("Unable to fetch transaction", e));
      return completableFuture;
    }
  }

  private <T> CompletableFuture<T> tokenAuthRequest(String method, Tokens tokens, String path, Object payload, Class<T> responseClass) throws IOException {
    return refreshIfNecessaryAsync(tokens).thenCompose(newTokens -> {
      Request request = jettyClient.newRequest(apiHost + path);
      request.method(method);
      request.header("Authorization", "Bearer " + newTokens.accessToken);
      request.header("Content-Type", "application/json");
      request.header("Accept", "application/json");
      request.header("User-Agent", "BerbixJava/" + Berbix.BERBIX_SDK_VERSION);

      if (payload != null) {
        BytesContentProvider bytesRequestContent;
        try {
          bytesRequestContent = new BytesContentProvider("application/json",
              objectMapper.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8));
        } catch (JsonProcessingException ex) {
          throw new BerbixException("Unable to serialize API request", ex);
        }
        request.content(bytesRequestContent, "application/json");
      }

      CompletableFuture<ContentResponse> completableFuture = new CompletableFuture<>();
      request.timeout(REQUEST_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
      request.send(new CompletableFutureResponseListener(completableFuture));

      return completableFuture.thenApply(response -> {
        if (response.getStatus() == 204 && responseClass == String.class) {
          // cast string as String so T compiles.
          return responseClass.cast("finished");
        } else {
          String responseData = response.getContentAsString();
          try {
            return objectMapper.readValue(responseData, responseClass);
          } catch (JsonProcessingException ex) {
            throw new BerbixException("Unable to read API response", ex);
          }
        }
      });
    });
  }

  private CompletableFuture<Tokens> refreshIfNecessaryAsync(Tokens tokens) {
    if (tokens.needsRefresh()) {
      return refreshTokensAsync(tokens)
          .thenApply(newTokens -> {
            tokens.refresh(newTokens);
            return tokens;
          });
    } else {
      return CompletableFuture.completedFuture(tokens);
    }
  }

  public CompletableFuture<Tokens> refreshTokensAsync(Tokens tokens) {
    RefreshTokenRequest request = new RefreshTokenRequest();
    request.refreshToken = tokens.refreshToken;
    request.grantType = "refresh_token";
    return fetchTokens("/v0/tokens", request)
        .thenApply(this::createTokens)
        .handle((result, ex) -> {
          if (ex != null) {
            throw new BerbixException("Unable to refresh tokens", ex);
          }

          return result;
        });
  }

  public CompletableFuture<Boolean> overrideTransactionAsync(Tokens tokens, OverrideTransactionRequest request) {
    try {
      return tokenAuthRequest("PATCH", tokens, "/v0/transactions/override", request, String.class)
          .handle((result, ex) -> {
            if (ex != null) {
              throw new BerbixException("Unable to override transaction", ex);
            }

            return true;
          });
    } catch (IOException e) {
      CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
      completableFuture.completeExceptionally(new BerbixException("Unable to override transaction", e));
      return completableFuture;
    }
  }

  public CompletableFuture<Transaction> updateTransactionAsync(Tokens tokens, UpdateTransactionRequest request) {
    try {
      return tokenAuthRequest("PATCH", tokens, "/v0/transactions", request, Transaction.class)
          .handle((result, ex) -> {
            if (ex != null) {
              throw new BerbixException("Unable to update transaction", ex);
            }

            return result;
          });
    } catch (IOException e) {
      CompletableFuture<Transaction> completableFuture = new CompletableFuture<>();
      completableFuture.completeExceptionally(new BerbixException("Unable to update transaction", e));
      return completableFuture;
    }
  }

  public CompletableFuture<Boolean> deleteTransactionAsync(Tokens tokens) {
    try {
      return tokenAuthRequest("DELETE", tokens, "/v0/transactions", null, String.class)
          .handle((result, ex) -> {
            if (ex != null) {
              throw new BerbixException("Unable to override transaction", ex);
            }

            return true;
          });
    } catch (IOException e) {
      CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
      completableFuture.completeExceptionally(new BerbixException("Unable to override transaction", e));
      return completableFuture;
    }
  }

  public static class CompletableFutureResponseListener extends BufferingResponseListener {
    private final CompletableFuture<ContentResponse> completable;

    public CompletableFutureResponseListener(
        CompletableFuture<ContentResponse> completable) {
      this.completable = completable;
    }

    @Override
    public void onComplete(Result result) {
      if (result.isFailed()) {
        completable.completeExceptionally(result.getFailure());
      } else {
        HttpContentResponse response =
            new HttpContentResponse(
                result.getResponse(),
                getContent(),
                getMediaType(),
                getEncoding());
        completable.complete(response);
      }
    }
  }
}
