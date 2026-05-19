package com.idigicloud.accountopening.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InitialFundingRequest {

    @NotNull(message = "Account opening request ID is required")
    private Long accountOpeningRequestId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "500.00", message = "Minimum initial deposit is ₹500")
    private BigDecimal amount;

    @NotBlank(message = "Funding mode is required")
    private String fundingMode;   // CASH, CHEQUE, FUND_TRANSFER
}
