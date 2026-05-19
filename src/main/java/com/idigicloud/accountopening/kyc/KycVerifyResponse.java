package com.idigicloud.accountopening.kyc;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO received from the Python KYC service.
 * Python returns this after Aadhaar/PAN verification + AML/negative list screening.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KycVerifyResponse {

    /** true = KYC passed, false = KYC failed */
    private boolean verified;

    /** true = customer cleared AML / negative list screening */
    private boolean amlClean;

    /** Risk score 0.0 (low risk) to 1.0 (high risk) */
    private Double riskScore;

    /** Human-readable result: VERIFIED, REJECTED, MANUAL_REVIEW */
    private String status;

    /** Rejection or review reason (if any) */
    private String remarks;

    /** Extracted name from OCR (Python can return this) */
    private String extractedName;

    /** Extracted DOB from OCR */
    private String extractedDob;

    /** Extracted address from OCR */
    private String extractedAddress;

    public boolean isApproved() {
        return verified && amlClean && (riskScore == null || riskScore < 0.7);
    }
}
