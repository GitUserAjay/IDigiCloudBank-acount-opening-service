package com.idigicloud.accountopening.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DocumentUploadRequest {

    @NotNull(message = "Account opening request ID is required")
    private Long accountOpeningRequestId;

    @NotBlank(message = "Document type is required")
    private String documentType;      // AADHAAR, PAN, DRIVING_LICENSE, RATION_CARD

    @NotBlank(message = "Document category is required")
    private String documentCategory;  // ID_PROOF, ADDRESS_PROOF

    @NotBlank(message = "File name is required")
    private String fileName;

    private String indexCategory;     // CUSTOMER, ACCOUNT
    private String documentId;        // CBS document reference ID
    private Integer versionNo;
    private String filePath;
}
