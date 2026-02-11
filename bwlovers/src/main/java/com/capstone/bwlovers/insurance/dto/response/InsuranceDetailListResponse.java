package com.capstone.bwlovers.insurance.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class InsuranceDetailListResponse {
    private Long insuranceId;
    private String insuranceCompany;
    private String productName;
    private boolean isLongTerm;
    private Long monthlyCost;
    private String memo;
    private LocalDateTime createdAt;
    private List<String> specialContractNames;
}