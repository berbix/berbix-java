package com.berbix;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UploadImagesResponse {
    public static final String NextStepUploadDocumentFront = "upload_document_front";
    public static final String NextStepUploadDocumentBack = "upload_document_back";
    public static final String NextStepUploadSelfieBasic = "upload_selfie_basic";
    public static final String NextStepUploadSelfieLiveness = "upload_selfie_liveness";
    // NextStepDone indicates that no more uploads are expected.
    public static final String NextStepDone = "done";

    public static final String IssueBadUpload = "bad_upload";
    public static final String IssueTextUnreadable = "text_unreadable";
    public static final String IssueNoFaceOnIDDetected = "no_face_on_id_detected";
    public static final String IssueIncompleteBarcodeDetected = "incomplete_barcode_detected";
    public static final String IssueUnsupportedIDType = "unsupported_id_type";
    public static final String IssueBadSelfie = "bad_selfie";

    public String[] issues;
    public IssueDetails issueDetails;
    public String nextStep;

    public static class IssueDetails {
        @JsonProperty("unsupported_id_type")
        public static UnsupportedIDTypeDetails unsupportedIDType;

        public static class UnsupportedIDTypeDetails {
            public boolean visaPageOfPassport;
        }
    }
}
