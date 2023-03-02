package com.berbix;

public class CreateAPIOnlyTransactionRequest {
    public String customerUid;
    public String templateKey;
    public String phone;
    public String email;
    public boolean consentsToAutomatedFacialRecognition;
    public CreateAPIOnlyTransactionRequest.APIOnlyOptions apiOnlyOptions = new CreateAPIOnlyTransactionRequest.APIOnlyOptions();

    public static class APIOnlyOptions {
        public String idCountry;
        public String idType;
    }
}
