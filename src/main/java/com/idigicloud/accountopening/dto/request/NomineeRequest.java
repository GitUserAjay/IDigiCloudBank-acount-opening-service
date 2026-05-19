package com.idigicloud.accountopening.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class NomineeRequest {

    @NotNull(message = "Account opening request ID is required")
    private Long accountOpeningRequestId;

    @NotEmpty(message = "At least one nominee is required")
    private List<NomineeDetail> nominees;

    @Data
    public static class NomineeDetail {

        @NotBlank(message = "First name is required")
        private String firstName;

        private String middleName;
        private String lastName;

        @NotBlank(message = "Gender is required")
        private String gender;

        @NotBlank(message = "Date of birth is required")
        private String dateOfBirth;  // DD-MMM-YYYY e.g. 01-Jan-1990

        @NotBlank(message = "Relationship is required")
        private String relationship;

        @NotNull(message = "Share percentage is required")
        @Min(value = 1, message = "Share percentage must be at least 1")
        @Max(value = 100, message = "Share percentage cannot exceed 100")
        private Integer sharePercentage;

        private String mobileNumber;
        private String email;
        private String addressLine1;
        private String addressLine2;
        private String country;
        private String postalCode;
    }
}
