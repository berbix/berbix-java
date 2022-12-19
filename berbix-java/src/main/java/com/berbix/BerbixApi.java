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

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Request;

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
  private final OkHttpClient okHttpClient;

  private final ObjectMapper objectMapper;

  public static final MediaType MEDIA_TYPE_JSON
      = MediaType.parse("application/json; charset=utf-8");

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

    okHttpClient = new OkHttpClient();
  }

  void shutdown() {

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
    RequestBuilder requestBuilder = new Request.Builder()
      .url(apiHost + path)
      .header("Authorization", "Basic " + Base64.getEncoder().encodeToString((apiSecret + ":").getBytes(StandardCharsets.UTF_8)))
      .addHeader("Content-Type", "application/json")
      .addHeader("Accept", "application/json")
      .addHeader("User-Agent", "BerbixJava/" + Berbix.BERBIX_SDK_VERSION);

	try {
	  Request request = requestBuilder
      .post(RequestBody.create(MEDIA_TYPE_JSON, objectMapper.writeValueAsString(payload)))
	  .build();
	} catch (JsonProcessingException e) {
      throw new BerbixException("Unable to create transaction", e);
    }

    CompletableFuture<ContentResponse> completableFuture = new CompletableFuture<>();
	okHttpClient.newCall(request).enqueue(new CompletableFutureResponseListener(completableFuture));

    return completableFuture.thenApply(response -> {
      String apiResponseData = response.body().string();

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
      RequestBuilder requestBuilder = new Request.Builder()
        .url(apiHost + path)
        .header("Authorization", "Bearer " + newTokens.accessToken)
        .addHeader("Content-Type", "application/json")
        .addHeader("Accept", "application/json")
        .addHeader("User-Agent", "BerbixJava/" + Berbix.BERBIX_SDK_VERSION);

	  try {
	    Request request = requestBuilder
        .post(RequestBody.create(MEDIA_TYPE_JSON, objectMapper.writeValueAsString(payload)))
	    .build();
	  } catch (JsonProcessingException e) {
        throw new BerbixException("Unable to create transaction", e);
      }

      CompletableFuture<ContentResponse> completableFuture = new CompletableFuture<>();
	  okHttpClient.newCall(request).enqueue(new CompletableFutureResponseListener(completableFuture));

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
        // 4xx and 5xx should result in failure exceptions
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
