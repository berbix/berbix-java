package com.berbix;

import java.util.ArrayList;
import java.util.List;

public class UploadImagesRequest {
    public static final String ImageSubjectDocumentFront = "document_front";
    public static final String ImageSubjectDocumentBack = "document_back";
    public static final String ImageSubjectDocumentBarcode = "document_barcode";
    public static final String ImageSubjectSelfieFront = "selfie_front";
    public static final String ImageSubjectSelfieLeft = "selfie_left";
    public static final String ImageSubjectSelfieRight = "selfie_right";

    public static final String ImageFormatPNG = "image/png";
    public static final String ImageFormatJPEG = "image/jpeg";

    public List<ImageData> images = new ArrayList<>();

    public static class ImageData {
        // base 64 encoded string of image
        public String data;
        public String imageSubject;
        public String format;
        public SupplementaryData supplementaryData;

        public static class SupplementaryData {
            public ExtractedBarcode extractedBarcode;

            public static class ExtractedBarcode {
                public String barcodeType;
                public String extractedData;
            }
        }
    }
}


