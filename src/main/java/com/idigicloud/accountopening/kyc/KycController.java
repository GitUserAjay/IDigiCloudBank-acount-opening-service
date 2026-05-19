package com.idigicloud.accountopening.kyc;

import com.idigicloud.accountopening.dto.response.ApiResponse;
import com.idigicloud.accountopening.exception.InvalidOperationException;
import com.idigicloud.accountopening.repository.AccountOpeningRequestRepository;
import com.idigicloud.accountopening.entity.AccountOpeningRequest;
import com.idigicloud.accountopening.enums.AccountOpeningStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/kyc")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "KYC Verification", description = "Interfaces with Python KYC service for document verification and AML screening")
public class KycController {

    private final KycClient kycClient;
    private final AccountOpeningRequestRepository requestRepository;

    // ─────────────────────────────────────────────────────────
    //  POST /api/kyc/verify  — Step 5
    // ─────────────────────────────────────────────────────────
    @PostMapping("/verify")
    @Operation(
        summary     = "Step 5 — KYC Verification",
        description = "Sends Aadhaar + PAN details to the Python KYC service for verification, " +
                      "AML screening, and negative list check. Updates account opening status accordingly."
    )
    public ResponseEntity<ApiResponse<KycResultResponse>> verifyKyc(
            @Valid @RequestBody KycVerifyRequestBody body) {

        AccountOpeningRequest opening = requestRepository.findById(body.getAccountOpeningRequestId())
                .orElseThrow(() -> new InvalidOperationException(
                        "Account opening request not found: " + body.getAccountOpeningRequestId()));

        // Build KYC request for Python
        KycVerifyRequest kycRequest = KycVerifyRequest.builder()
                .aadhaarNumber(body.getAadhaarNumber())
                .panNumber(body.getPanNumber())
                .firstName(body.getFirstName())
                .lastName(body.getLastName())
                .dateOfBirth(body.getDateOfBirth())
                .accountOpeningId(String.valueOf(body.getAccountOpeningRequestId()))
                .build();

        // Call Python KYC service
        KycVerifyResponse kycResponse = kycClient.verifyKyc(kycRequest);

        // Update account opening status based on result
        if (kycResponse.isApproved()) {
            opening.setStatus(AccountOpeningStatus.KYC_VERIFIED);
            log.info("KYC APPROVED for accountOpeningId={}", opening.getId());
        } else {
            opening.setStatus(AccountOpeningStatus.KYC_PENDING);
            log.warn("KYC FAILED/PENDING for accountOpeningId={}, status={}",
                    opening.getId(), kycResponse.getStatus());
        }
        requestRepository.save(opening);

        KycResultResponse result = KycResultResponse.builder()
                .accountOpeningRequestId(opening.getId())
                .verified(kycResponse.isVerified())
                .amlClean(kycResponse.isAmlClean())
                .riskScore(kycResponse.getRiskScore())
                .kycStatus(kycResponse.getStatus())
                .remarks(kycResponse.getRemarks())
                .accountOpeningStatus(opening.getStatus().name())
                .approved(kycResponse.isApproved())
                .build();

        String message = kycResponse.isApproved()
                ? "KYC verification passed — account opening can proceed"
                : "KYC verification failed or requires manual review";

        return ResponseEntity.ok(ApiResponse.success(message, result));
    }

    // ─────────────────────────────────────────────────────────
    //  POST /api/kyc/ocr  — Step 4a
    // ─────────────────────────────────────────────────────────
    @PostMapping("/ocr")
    @Operation(
        summary     = "Step 4a — OCR Document Extraction",
        description = "Sends uploaded document to Python for OCR. " +
                      "Python extracts name, DOB, address, and ID number automatically."
    )
    public ResponseEntity<ApiResponse<KycVerifyResponse>> extractOcr(
            @Valid @RequestBody OcrRequestBody body) {

        KycVerifyResponse ocrResult = kycClient.extractDocumentOcr(
                body.getDocumentPath(),
                body.getDocumentType(),
                String.valueOf(body.getAccountOpeningRequestId())
        );
        return ResponseEntity.ok(ApiResponse.success("OCR extraction completed", ocrResult));
    }

    // ─────────────────────────────────────────────────────────
    //  GET /api/kyc/status/{accountOpeningRequestId}
    // ─────────────────────────────────────────────────────────
    @GetMapping("/status/{accountOpeningRequestId}")
    @Operation(summary = "Get KYC status for an account opening request")
    public ResponseEntity<ApiResponse<KycStatusResponse>> getKycStatus(
            @PathVariable Long accountOpeningRequestId) {

        AccountOpeningRequest opening = requestRepository.findById(accountOpeningRequestId)
                .orElseThrow(() -> new InvalidOperationException(
                        "Account opening request not found: " + accountOpeningRequestId));

        boolean kycDone = opening.getStatus() == AccountOpeningStatus.KYC_VERIFIED
                || opening.getStatus() == AccountOpeningStatus.ACTIVE;

        KycStatusResponse status = KycStatusResponse.builder()
                .accountOpeningRequestId(accountOpeningRequestId)
                .currentStatus(opening.getStatus().name())
                .kycVerified(kycDone)
                .canProceed(kycDone)
                .build();

        return ResponseEntity.ok(ApiResponse.success("KYC status fetched", status));
    }

    // ─── Inner DTOs (kept here for clarity, no extra files needed) ────────

    @Data
    public static class KycVerifyRequestBody {
        @NotNull private Long accountOpeningRequestId;
        @NotBlank private String aadhaarNumber;
        @NotBlank private String panNumber;
        private String firstName;
        private String lastName;
        private String dateOfBirth;
    }

    @Data
    public static class OcrRequestBody {
        @NotNull  private Long accountOpeningRequestId;
        @NotBlank private String documentPath;
        @NotBlank private String documentType;
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class KycResultResponse {
        private Long accountOpeningRequestId;
        private boolean verified;
        private boolean amlClean;
        private Double riskScore;
        private String kycStatus;
        private String remarks;
        private String accountOpeningStatus;
        private boolean approved;
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class KycStatusResponse {
        private Long accountOpeningRequestId;
        private String currentStatus;
        private boolean kycVerified;
        private boolean canProceed;
    }
}
