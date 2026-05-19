package com.idigicloud.accountopening.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BasicDetailsRequest {

    @NotNull(message = "Account opening request ID is required")
    private Long accountOpeningRequestId;

    private String accountName;
    private String debitCardVariant;      // VISA CLASSIC, VISA PLATINUM, MASTERCARD
    private boolean netBankingRequested;
    private boolean mobileBankingRequested;
    private boolean chequeBookRequested;
    private boolean passbookRequested;
    private String preferredContactNumber;
    private String preferredEmail;
}
