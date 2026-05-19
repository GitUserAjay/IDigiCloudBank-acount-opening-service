package com.idigicloud.accountopening.dto.request;

import com.idigicloud.accountopening.enums.ProductClass;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

// ── Step 1: New Account ──────────────────────────────
@Data
public class NewAccountRequest {

    @NotNull(message = "Product class is required")
    private ProductClass productClass;   // CASA, TD, RD, LOAN

    @NotBlank(message = "Customer type is required")
    private String customerType;         // INDIVIDUAL, CORPORATE

    @NotBlank(message = "Branch code is required")
    private String branchCode;

    private String currencyCode = "INR";
}
