package com.idigicloud.accountopening.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductSelectionRequest {

    @NotNull(message = "Account opening request ID is required")
    private Long accountOpeningRequestId;

    @NotBlank(message = "Offer code is required")
    private String offerCode;          // e.g. OFFERSAVIUTAOO1

    @NotBlank(message = "Product code is required")
    private String productCode;        // e.g. CPSAVIUTAOO1

    private String offerName;          // SAVINGS OFFER
    private String accountType;        // Savings, Current
    private String productGroup;       // PGSAVIUTAOO1
    private BigDecimal totalFees;
    private String operatingInstructionTemplateId; // OPINSTSAVAOOO1
}
