package com.idigicloud.accountopening.kyc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO sent to the Python KYC service.
 * Python endpoint: POST http://localhost:5000/kyc/verify
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycVerifyRequest {

    private String aadhaarNumber;
    private String panNumber;
    private String firstName;
    private String lastName;
    private String dateOfBirth;      // DD-MMM-YYYY
    private String documentType;     // AADHAAR, PAN, DRIVING_LICENSE
    private String documentPath;     // file path / URL after upload
    private String accountOpeningId; // for traceability
}
