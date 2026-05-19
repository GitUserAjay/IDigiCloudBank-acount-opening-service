package com.idigicloud.accountopening.kyc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP client that communicates with the Python KYC microservice.
 *
 * Python service responsibilities (from internship spec):
 *   - Step 4a: OCR on uploaded Aadhaar / PAN / Driving License / Ration Card
 *   - Step 5 : Aadhaar + PAN verification
 *   - Step 5a: AML screening, negative list screening, risk scoring
 *   - Step 9a: Welcome kit PDF generation (separate endpoint)
 *
 * Base URL configured in application.properties:
 *   kyc.service.base-url=http://localhost:5000
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KycClient {

    @Value("${kyc.service.base-url}")
    private String kycBaseUrl;

    private final RestTemplate restTemplate;

    // ─────────────────────────────────────────
    //  MAIN KYC VERIFICATION (Step 5)
    // ─────────────────────────────────────────

    /**
     * Sends Aadhaar + PAN details to Python for KYC + AML verification.
     * Python runs: Aadhaar check, PAN check, AML screening, negative list check.
     *
     * @param request KYC request payload
     * @return KycVerifyResponse with verified/amlClean/riskScore
     */
    public KycVerifyResponse verifyKyc(KycVerifyRequest request) {
        log.info("KYC: Sending verification request for accountOpeningId={}",
                request.getAccountOpeningId());
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<KycVerifyRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<KycVerifyResponse> response = restTemplate.postForEntity(
                    kycBaseUrl + "/kyc/verify",
                    entity,
                    KycVerifyResponse.class
            );

            KycVerifyResponse result = response.getBody();
            log.info("KYC: Result for accountOpeningId={} => verified={}, amlClean={}, riskScore={}",
                    request.getAccountOpeningId(),
                    result != null && result.isVerified(),
                    result != null && result.isAmlClean(),
                    result != null ? result.getRiskScore() : "N/A");
            return result;

        } catch (ResourceAccessException e) {
            log.warn("KYC service unreachable at {}. Returning mock APPROVED for dev.", kycBaseUrl);
            return buildMockApprovedResponse();

        } catch (HttpClientErrorException e) {
            log.error("KYC service error [{}]: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return KycVerifyResponse.builder()
                    .verified(false)
                    .amlClean(false)
                    .status("REJECTED")
                    .remarks("KYC service returned error: " + e.getStatusCode())
                    .build();

        } catch (Exception e) {
            log.error("KYC unexpected error: {}", e.getMessage());
            // Graceful fallback — don't block account opening if KYC service is down in dev
            log.warn("KYC service down. Using mock approval for development.");
            return buildMockApprovedResponse();
        }
    }

    // ─────────────────────────────────────────
    //  OCR EXTRACTION (Step 4a)
    // ─────────────────────────────────────────

    /**
     * Sends a document file path to Python for OCR text extraction.
     * Python extracts: name, DOB, address, ID number from the document image/PDF.
     *
     * @param documentPath path/URL of uploaded document
     * @param documentType AADHAAR, PAN, DRIVING_LICENSE, RATION_CARD
     * @param accountOpeningId for traceability
     * @return KycVerifyResponse containing extracted fields
     */
    public KycVerifyResponse extractDocumentOcr(String documentPath,
                                                 String documentType,
                                                 String accountOpeningId) {
        log.info("KYC OCR: Extracting from documentType={}, accountOpeningId={}",
                documentType, accountOpeningId);
        try {
            KycVerifyRequest request = KycVerifyRequest.builder()
                    .documentPath(documentPath)
                    .documentType(documentType)
                    .accountOpeningId(accountOpeningId)
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<KycVerifyRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<KycVerifyResponse> response = restTemplate.postForEntity(
                    kycBaseUrl + "/kyc/ocr",
                    entity,
                    KycVerifyResponse.class
            );
            return response.getBody();

        } catch (Exception e) {
            log.warn("KYC OCR service unavailable: {}. Skipping OCR step.", e.getMessage());
            return KycVerifyResponse.builder()
                    .status("MANUAL_REVIEW")
                    .remarks("OCR service unavailable — manual data entry required")
                    .build();
        }
    }

    // ─────────────────────────────────────────
    //  WELCOME KIT PDF (Step 9a)
    // ─────────────────────────────────────────

    /**
     * Triggers Python to generate the personalised Welcome Kit PDF.
     * Python formats customer details into a secure PDF document.
     *
     * @param accountNumber CBS account number
     * @param customerName  full name for the PDF
     * @return download URL/path of the generated PDF
     */
    public String generateWelcomeKitPdf(String accountNumber, String customerName) {
        log.info("KYC: Generating welcome kit PDF for accountNumber={}", accountNumber);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            java.util.Map<String, String> payload = new java.util.HashMap<>();
            payload.put("accountNumber", accountNumber);
            payload.put("customerName", customerName);

            HttpEntity<java.util.Map<String, String>> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<java.util.Map> response = restTemplate.postForEntity(
                    kycBaseUrl + "/kyc/welcome-kit-pdf",
                    entity,
                    java.util.Map.class
            );

            if (response.getBody() != null && response.getBody().containsKey("pdfUrl")) {
                return (String) response.getBody().get("pdfUrl");
            }
        } catch (Exception e) {
            log.warn("Welcome kit PDF generation failed: {}. PDF will be generated manually.", e.getMessage());
        }
        return null;
    }

    // ─────────────────────────────────────────
    //  PRIVATE HELPERS
    // ─────────────────────────────────────────

    /**
     * Mock approved response used when Python KYC service is not running locally.
     * This allows Java backend development to continue without requiring Python service.
     */
    private KycVerifyResponse buildMockApprovedResponse() {
        return KycVerifyResponse.builder()
                .verified(true)
                .amlClean(true)
                .riskScore(0.05)
                .status("VERIFIED")
                .remarks("Mock approval — Python KYC service not running")
                .build();
    }
}
