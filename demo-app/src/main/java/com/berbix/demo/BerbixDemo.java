package com.berbix.demo;

import com.berbix.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class BerbixDemo {

  public static void main(String[] args) throws JsonProcessingException, ExecutionException, InterruptedException {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    objectMapper.registerModule(new JavaTimeModule());

    BerbixClient berbixClient = Berbix.create(
        new Berbix.BerbixOptions.Builder()
            .apiSecret("YOUR_API_SECRET_HERE_DO_NOT_PUT_IN_SOURCE_CODE")
            .build());
    try {
      CreateHostedTransactionRequest request = new CreateHostedTransactionRequest();
      request.customerUid = "YOUR_CUSTOMER_UID_HERE";
      request.templateKey = "YOUR_TEMPLATE_KEY_HERE";

      System.out.println("creating hosted transaction");
      CreateHostedTransactionResponse response = berbixClient.createHostedTransaction(request);
      System.out.println(response.hostedUrl);

      Tokens tokens = berbixClient.refreshTokens(response.tokens);

      System.out.println("fetch transaction");
      Transaction transaction = berbixClient.fetchTransaction(tokens);
      System.out.println(objectMapper.writeValueAsString(transaction));

      OverrideTransactionRequest overrideTransactionRequest = new OverrideTransactionRequest();
      overrideTransactionRequest.responsePayload = OverrideTransactionRequest.ResponsePayload.US_DL;
      List<String> flags = new ArrayList<>();
      flags.add("id_under_18");
      flags.add("id_under_21");
      overrideTransactionRequest.flags = flags;
      Map<String, String> overrideFields = new HashMap<>();
      overrideFields.put("date_of_birth", "2000-12-09");
      overrideTransactionRequest.overrideFields = overrideFields;

      System.out.println("override transaction");
      berbixClient.overrideTransaction(tokens, overrideTransactionRequest);

      System.out.println("fetch transaction");
      transaction = berbixClient.fetchTransaction(tokens);
      System.out.println(objectMapper.writeValueAsString(transaction));

      UpdateTransactionRequest updateTransactionRequest = new UpdateTransactionRequest();
      updateTransactionRequest.action = "accept";
      updateTransactionRequest.note = "custom note here for update";

      System.out.println("update transaction");
      transaction = berbixClient.updateTransaction(tokens, updateTransactionRequest);
      System.out.println(objectMapper.writeValueAsString(transaction));

      // System.out.println("delete transaction");
      //boolean deleted = berbixClient.deleteTransaction(tokens);
      //System.out.println("deleted transaction: " + deleted);

      System.out.println("verify signature");
      boolean signatureValidation = berbixClient.validateSignature("YOUR_WEBHOOK_SECRET_HERE_DO_NOT_PUT_IN_SOURCE_CODE",
          "WEBHOOK_BODY",
          "x-berbix-signature header");
      System.out.println("signatureValidation: " + signatureValidation);
    } catch (Exception e) {
      System.out.println(e);
    } finally {
      berbixClient.close();
    }
  }
}
