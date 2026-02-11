package com.capstone.bwlovers.insurance.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class InsuranceSelectionSaveRequest {

    @NotBlank
    @JsonProperty("resultId")
    private String resultId;

    @NotBlank
    @JsonProperty("itemId")
    private String itemId;

    @NotEmpty
    @JsonProperty("selectedContractNames")
    private List<String> selectedContractNames;

    private String memo;
}
