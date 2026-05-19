package com.idigicloud.accountopening.dto.response;

import com.idigicloud.accountopening.enums.AccountOpeningStatus;
import com.idigicloud.accountopening.enums.ProductClass;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountOpeningResponse {

    private Long id;
    private ProductClass productClass;
    private String customerType;
    private String branchCode;
    private String bankCode;
    private String currencyCode;

    // Step 2
    private String offerCode;
    private String offerName;
    private String productCode;
    private String accountType;
    private BigDecimal totalFees;

    // Step 3
    private String cbsAccountNumber;
    private String modeOfOperation;
    private List<CoApplicantResponse> coApplicants;

    // Step 5
    private String accountName;
    private String debitCardVariant;

    // Step 7
    private List<NomineeResponse> nominees;

    // Step 8
    private BigDecimal initialFundingAmount;
    private String fundingMode;

    // CBS
    private String cbsCustomerId;
    private String welcomeKitReference;

    // Status
    private AccountOpeningStatus status;
    private Integer currentStep;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CoApplicantResponse {
        private Long id;
        private String cbsCustomerId;
        private String customerName;
        private String customerRole;
        private boolean existingCustomer;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NomineeResponse {
        private Long id;
        private String firstName;
        private String middleName;
        private String lastName;
        private String gender;
        private String dateOfBirth;
        private String relationship;
        private Integer sharePercentage;
        private String mobileNumber;
        private String email;
    }
}
