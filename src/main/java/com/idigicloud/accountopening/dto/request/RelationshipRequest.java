package com.idigicloud.accountopening.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class RelationshipRequest {

    @NotNull(message = "Account opening request ID is required")
    private Long accountOpeningRequestId;

    @NotBlank(message = "Mode of operation is required")
    private String modeOfOperation;  // SINGLE, ANYONE_OR_SURVIVOR, JOINTLY

    // Optional co-applicants
    private List<CoApplicantRequest> coApplicants;

    @Data
    public static class CoApplicantRequest {
        private String cbsCustomerId;
        private String customerName;
        private String customerRole;     // JOF, JOO, GUARANTOR
        private boolean existingCustomer;
    }
}
