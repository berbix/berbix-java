package com.berbix.demo;

import com.berbix.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class APIOnlyDemo {

    public static void main(String[] args) throws JsonProcessingException, ExecutionException, InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.registerModule(new JavaTimeModule());

        BerbixClient berbixClient = Berbix.create(
                new Berbix.BerbixOptions.Builder()
                        .apiSecret("YOUR_API_SECRET_HERE_DO_NOT_PUT_IN_SOURCE_CODE")
                        .build());
        try {
            CreateAPIOnlyTransactionRequest request = new CreateAPIOnlyTransactionRequest();
            request.customerUid = UUID.randomUUID().toString();
            request.templateKey = "YOUR_TEMPLATE_KEY_HERE";
            request.consentsToAutomatedFacialRecognition = true;
            request.apiOnlyOptions = new CreateAPIOnlyTransactionRequest.APIOnlyOptions();

            System.out.println("creating API transaction");
            CreateAPIOnlyTransactionResponse response = berbixClient.createAPIOnlyTransaction(request);
            System.out.println(objectMapper.writeValueAsString(response));

            UploadImagesResponse imagesResponse;

            UploadImagesRequest.ImageData imageData;
            imageData = createImageData("PATH_TO_FRONT_IMAGE",
                    UploadImagesRequest.ImageSubjectDocumentFront);

            System.out.println("uploading front");
            imagesResponse = uploadImages(berbixClient,
                    response.tokens.clientToken,
                    Collections.singletonList(imageData));

            System.out.println(objectMapper.writeValueAsString(imagesResponse));


            imageData = createImageData("PATH_TO_BACK_IMAGE",
                    UploadImagesRequest.ImageSubjectDocumentBack);

            System.out.println("uploading back");
            imagesResponse = uploadImages(berbixClient,
                    response.tokens.clientToken,
                    Collections.singletonList(imageData));

            System.out.println(objectMapper.writeValueAsString(imagesResponse));


            imageData = createImageData("PATH_TO_SELFIE_IMAGE",
                    UploadImagesRequest.ImageSubjectSelfieFront);
            System.out.println("uploading selfie");
            imagesResponse = uploadImages(berbixClient,
                    response.tokens.clientToken,
                    Collections.singletonList(imageData));

            System.out.println(objectMapper.writeValueAsString(imagesResponse));

            System.out.println("fetch transaction");
            Transaction transactionResponse = berbixClient.fetchTransaction(response.tokens);
            System.out.println(objectMapper.writeValueAsString(transactionResponse));

        } catch (Exception e) {
            System.out.println(e);
            System.out.println(e.getCause().getCause());
        } finally {
            berbixClient.close();
        }
    }

    private static UploadImagesRequest.ImageData createImageData(String imagePath, String subject) throws IOException {
        File imageFile = new File(imagePath);
        FileInputStream fileInputStreamReader = new FileInputStream(imageFile);
        byte[] bytes = new byte[(int) imageFile.length()];
        fileInputStreamReader.read(bytes);
        String encodedImage = Base64.getEncoder().encodeToString(bytes);

        UploadImagesRequest.ImageData imageData = new UploadImagesRequest.ImageData();
        imageData.data = encodedImage;
        imageData.imageSubject = subject;
        imageData.format = UploadImagesRequest.ImageFormatJPEG;

        return imageData;
    }

    private static UploadImagesResponse uploadImages(BerbixClient berbixClient, String clientToken, List<UploadImagesRequest.ImageData> imageData) throws IOException, ExecutionException, InterruptedException {
        UploadImagesRequest uploadImagesRequest = new UploadImagesRequest();
        uploadImagesRequest.images = imageData;

        return berbixClient.uploadImages(clientToken, uploadImagesRequest);
    }
}
