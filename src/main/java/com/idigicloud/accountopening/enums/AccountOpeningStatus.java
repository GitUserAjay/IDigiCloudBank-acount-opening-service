package com.idigicloud.accountopening.enums;

public enum AccountOpeningStatus {
    DRAFT,               // Step 1 - started
    PRODUCT_SELECTED,    // Step 2
    RELATIONSHIP_SET,    // Step 3
    DOCUMENTS_UPLOADED,  // Step 4
    BASIC_DETAILS_DONE,  // Step 5
    LIMITS_CONFIGURED,   // Step 6
    NOMINEE_ADDED,       // Step 7
    SUBMITTED,           // Submitted to CBS
    KYC_PENDING,
    KYC_VERIFIED,
    ACTIVE,
    REJECTED
}
